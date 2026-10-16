package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.events.EventLog;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.noise.PeriodicNoise;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The plate-tectonics loop (docs/design/03-tectonics.md): proto-crust, rigid plates moved in whole-cell
 * substeps, subduction / collision / spreading at their boundaries, arc crust growth, erosion of thick
 * crust, and plate reorganization (rifts, merges, subduction initiation). Deterministic for a given seed.
 */
public final class PlateSimulation {
    final CylinderGrid grid;
    final long seed;
    final TectonicsConfig cfg;
    final Lithosphere lith;
    final List<Plate> plates = new ArrayList<>();
    final EventLog events = new EventLog();
    final PairCounter collisionMemory = new PairCounter();
    final PairCounter stepCollisions = new PairCounter();
    final SubductionPolarity polarity = new SubductionPolarity();

    double time;
    int stepIndex;
    /** Water volume as mean depth over the planet, m; sets sea level. */
    double meanOceanDepthM = com.platetectonics.geosim.terrain.SeaLevel.EARTH_MEAN_OCEAN_DEPTH_M;
    /** Sea level on the isostatic datum, m, updated at the start of every step. */
    double seaLevelM;
    /** Water locked in ice sheets, as metres of water over the whole planet (lowers sea level). */
    double iceWaterEquivalentM;
    /** Phase 2 erosion stand-in; turned off when real surface processes run. */
    boolean erosionStandIn = true;

    /** Worker threads for per-cell loops; results do not depend on the count. */
    Parallel parallel = Parallel.SERIAL;

    // scratch shared by the motion code
    final int[] queue;
    final int[] dist;
    // basin closure scratch (BasinClosure)
    final int[] closureOrder;
    /** All false between uses. */
    final boolean[] closureSeen;
    final float[] closureFill;
    final BasinClosure.Runs closureRuns = new BasinClosure.Runs();
    final float[] ceilingScratch;
    /** Elevation each cell may reach in crustal flow, m on the datum (scratch). */
    final float[] ceilingElevationScratch;
    /** Plate motion: where each cell moves (-1: lost at a pole), which cell claims each target first, and the rest. */
    final int[] moveTarget;
    final int[] moveFirst;
    final int[] moveLater;
    private final double[] columnCos;
    private final double[] columnSin;
    int[] trench = new int[1024];
    int trenchCount;

    // per-step counters
    long subductedCells;
    long collisionCells;
    long newOceanCells;
    /** Cells inside turned plates filled because of the rotation's rounding (D-42), since formation. */
    long roundingHoleCells;
    /** Since formation: motion substeps, and cells of new ocean floor. */
    long totalSubsteps;
    long totalNewOceanCells;
    long poleLostCells;
    int lastSubsteps;
    boolean flowBudgetExhausted;
    /** Crust (and its keel, eclogite) that underthrust each plate this step, km x cells; by plate id. */
    double[] underthrustCrust = new double[0];
    double[] underthrustKeel = new double[0];
    double[] underthrustEclogite = new double[0];
    /** Arc productivity this step relative to arcGrowthKmPerMyr (crustal growth curve feedback). */
    double arcMultiplier = 1.0;

    // cumulative continental-crust budget, km x cells (see crustBudget())
    double budgetArc;
    double budgetShelf;
    double budgetErosion;
    double budgetCollision;
    double budgetEclogite;
    double budgetSubducted;
    double budgetForearc;
    double budgetClosure;
    double budgetRounding;

    public PlateSimulation(CylinderGrid grid, long seed, TectonicsConfig cfg) {
        this.grid = grid;
        this.seed = seed;
        this.cfg = cfg;
        this.lith = new Lithosphere(grid);
        this.queue = new int[grid.size()];
        this.dist = new int[grid.size()];
        this.closureOrder = new int[grid.size()];
        this.closureSeen = new boolean[grid.size()];
        this.closureFill = new float[grid.size()];
        this.ceilingScratch = new float[grid.size()];
        this.ceilingElevationScratch = new float[grid.size()];
        this.moveTarget = new int[grid.size()];
        this.moveFirst = new int[grid.size()];
        this.moveLater = new int[grid.size()];
        Arrays.fill(dist, -1);
        columnCos = new double[grid.width()];
        columnSin = new double[grid.width()];
        for (int i = 0; i < grid.width(); i++) {
            double a = 2.0 * Math.PI * i / grid.width();
            columnCos[i] = Math.cos(a);
            columnSin[i] = Math.sin(a);
        }
        initialize();
    }

    // ------------------------------------------------------------------ initial state

    private void initialize() {
        int w = grid.width();
        int h = grid.height();
        int n = grid.size();
        double cellKm = grid.cellKm();

        // Proto-crust: thin basaltic crust with a few thicker protocontinental nuclei.
        int periodX = 8;
        double lattice = (double) periodX / w;
        PeriodicNoise thickNoise = new PeriodicNoise(seed, Streams.PROTO_TERRAIN, periodX, 6, 0.5);
        PeriodicNoise ageNoise = new PeriodicNoise(seed, Streams.PROTO_AGE, periodX, 4, 0.5);
        int nuclei = 5 + (int) (Rng.unit(seed, Streams.PROTO_NUCLEI, 0, 0) * 5);
        double[] ni = new double[nuclei];
        double[] nj = new double[nuclei];
        double[] nr = new double[nuclei];
        for (int k = 0; k < nuclei; k++) {
            ni[k] = Rng.unit(seed, Streams.PROTO_NUCLEI, 1, k) * w;
            nj[k] = (0.15 + 0.7 * Rng.unit(seed, Streams.PROTO_NUCLEI, 2, k)) * h;
            nr[k] = (600.0 + 900.0 * Rng.unit(seed, Streams.PROTO_NUCLEI, 3, k)) / cellKm;
        }
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int idx = j * w + i;
                double t = cfg.oceanCrustKm() + 2.0 + 2.0 * thickNoise.sample(i * lattice, j * lattice);
                for (int k = 0; k < nuclei; k++) {
                    double dx = wrapDelta(i - ni[k]);
                    double dy = j - nj[k];
                    t += 24.0 * Math.exp(-(dx * dx + dy * dy) / (nr[k] * nr[k]));
                }
                lith.thick[idx] = (float) t;
                lith.type[idx] = t >= cfg.continentThresholdKm() ? Lithosphere.CONTINENTAL : Lithosphere.OCEANIC;
                lith.oceanAge[idx] = lith.type[idx] == Lithosphere.OCEANIC
                        ? (float) (50.0 + 45.0 * ageNoise.sample(i * lattice, j * lattice)) : 0f;
                lith.formedAt[idx] = 0f;
            }
        }

        // Plates: noise-warped Voronoi cells around random seeds.
        int count = 8 + (int) (Rng.unit(seed, Streams.PLATE_SEEDS, 0, 0) * 7);
        double[] si = new double[count];
        double[] sj = new double[count];
        for (int k = 0; k < count; k++) {
            si[k] = Rng.unit(seed, Streams.PLATE_SEEDS, 1, k) * w;
            sj[k] = (0.08 + 0.84 * Rng.unit(seed, Streams.PLATE_SEEDS, 2, k)) * h;
            double angle = Rng.unit(seed, Streams.PLATE_SEEDS, 3, k) * 2.0 * Math.PI;
            double speed = 30.0 + 40.0 * Rng.unit(seed, Streams.PLATE_SEEDS, 4, k);
            plates.add(new Plate(k, speed * Math.cos(angle), speed * Math.sin(angle), 0.0));
        }
        PeriodicNoise warpX = new PeriodicNoise(seed, Streams.PLATE_WARP, periodX, 5, 0.55);
        PeriodicNoise warpY = new PeriodicNoise(seed, Streams.PLATE_WARP + 1000, periodX, 5, 0.55);
        double warpCells = 700.0 / cellKm;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                double x = i + warpCells * warpX.sample(i * lattice, j * lattice);
                double y = j + warpCells * warpY.sample(i * lattice, j * lattice);
                int best = 0;
                double bestD = Double.MAX_VALUE;
                for (int k = 0; k < count; k++) {
                    double dx = wrapDelta(x - si[k]);
                    double dy = y - sj[k];
                    double d = dx * dx + dy * dy;
                    if (d < bestD) {
                        bestD = d;
                        best = k;
                    }
                }
                lith.owner[j * w + i] = best;
            }
        }
        boolean[] all = new boolean[count];
        Arrays.fill(all, true);
        reframe(all);
        refreshStats();
        updateSeaLevel();
    }

    // ------------------------------------------------------------------ stepping

    /** Advances the planet by {@code dtMyr}. */
    public void step(double dtMyr) {
        Arrays.fill(lith.marks, (byte) 0);
        subductedCells = 0;
        collisionCells = 0;
        newOceanCells = 0;
        poleLostCells = 0;
        stepCollisions.clear();
        polarity.forgetStale(stepIndex);

        refreshStats();
        if (spinOn()) {
            refreshSpin();
        }
        updateSeaLevel();
        arcMultiplier = arcMultiplier();
        PlateDynamics.updateVelocities(this, dtMyr);

        // the fastest cell of any plate (drift plus spin at its farthest reach) moves at most a few cells
        double maxCells = 0.0;
        for (Plate p : plates) {
            if (p.alive) {
                double cells = Math.max(Math.abs(p.vx), Math.abs(p.vy)) * dtMyr / grid.cellKm();
                if (p.omega != 0.0) {
                    cells += Math.abs(p.omega) * p.radius * dtMyr;
                }
                maxCells = Math.max(maxCells, cells);
            }
        }
        if (underthrustCrust.length < plates.size()) {
            underthrustCrust = new double[plates.size()];
            underthrustKeel = new double[plates.size()];
            underthrustEclogite = new double[plates.size()];
        }
        int substeps = Math.max(1, (int) Math.ceil(maxCells / cfg.maxCellsPerSubstep()));
        double subDt = dtMyr / substeps;
        int w = grid.width();
        for (int s = 0; s < substeps; s++) {
            // the pose is absolute: cells land where it puts their place in the plate's raster, never relative
            // to where they were, so small turns add up instead of rounding away
            for (Plate p : plates) {
                if (!p.alive) {
                    continue;
                }
                p.accX += p.vx * subDt / grid.cellKm();
                p.accY += p.vy * subDt / grid.cellKm();
                if (p.omega != 0.0) {
                    turn(p, p.omega * subDt);
                }
                int shiftX = (int) Math.floor(p.accX);
                int shiftY = (int) Math.floor(p.accY);
                p.accX -= shiftX;
                p.accY -= shiftY;
                p.originI = Math.floorMod(p.originI + shiftX, w);
                p.originJ += shiftY;
            }
            PlateMotion.substep(this, subDt, time + (s + 1) * subDt);
        }
        lastSubsteps = substeps;
        totalSubsteps += substeps;
        totalNewOceanCells += newOceanCells;

        PlateMotion.distributeUnderthrust(this);
        CrustalFlow.relax(this);
        OrogenicRoots.update(this, dtMyr);
        if (erosionStandIn) {
            erodeThickCrust(dtMyr);
        }
        refreshStats();
        PlateReorganizer.reorganize(this, dtMyr);
        time += dtMyr;
        stepIndex++;
    }

    /**
     * Stand-in for erosion until Phase 3: crust thicker than the reference relaxes toward it, and part of
     * the removed volume is deposited on the plate's own continental shelves (ocean cells next to its
     * continent) and in its submerged continental basins (crust thinner than the datum), which slowly
     * grows continents outward and fills their inland seas.
     */
    void erodeThickCrust(double dt) {
        int n = grid.size();
        double k = 1.0 - Math.exp(-dt / cfg.erosionTimescaleMyr());
        double ref = cfg.referenceContinentKm();
        double[] removed = new double[plates.size()];
        for (int idx = 0; idx < n; idx++) {
            if (lith.type[idx] == Lithosphere.CONTINENTAL && lith.thick[idx] > ref) {
                double r = (lith.thick[idx] - ref) * k;
                lith.thick[idx] -= (float) r;
                budgetErosion += r;
                removed[lith.owner[idx]] += r * cfg.marginDepositFraction();
            }
        }
        int[] shelfCount = new int[plates.size()];
        boolean[] shelf = new boolean[n];
        int w = grid.width();
        int h = grid.height();
        float basin = (float) Isostasy.DATUM_CRUST_KM;
        int shelfCells = Math.max(1, (int) Math.round(cfg.shelfWidthKm() / grid.cellKm()));
        int tail = 0;
        for (int idx = 0; idx < n; idx++) {
            if (lith.type[idx] == Lithosphere.CONTINENTAL) {
                dist[idx] = 0;
                queue[tail++] = idx;
                if (lith.thick[idx] < basin) {
                    shelf[idx] = true;
                    shelfCount[lith.owner[idx]]++;
                }
            }
        }
        // ocean floor of the same plate within the shelf width of its continent
        for (int head = 0; head < tail; head++) {
            int idx = queue[head];
            if (dist[idx] >= shelfCells) {
                continue;
            }
            int p = lith.owner[idx];
            int i = idx % w;
            int j = idx / w;
            for (int m = 0; m < 4; m++) {
                int nb = m == 0 ? j * w + (i + 1 == w ? 0 : i + 1)
                        : m == 1 ? j * w + (i == 0 ? w - 1 : i - 1)
                        : m == 2 ? (j > 0 ? idx - w : -1)
                        : (j < h - 1 ? idx + w : -1);
                if (nb < 0 || dist[nb] >= 0 || lith.owner[nb] != p || lith.type[nb] != Lithosphere.OCEANIC) {
                    continue;
                }
                dist[nb] = dist[idx] + 1;
                queue[tail++] = nb;
                shelf[nb] = true;
                shelfCount[p]++;
            }
        }
        for (int q = 0; q < tail; q++) {
            dist[queue[q]] = -1;
        }
        for (int idx = 0; idx < n; idx++) {
            if (!shelf[idx]) {
                continue;
            }
            int p = lith.owner[idx];
            double add = Math.min(5.0, removed[p] / shelfCount[p]);
            if (lith.type[idx] == Lithosphere.CONTINENTAL) {
                add = Math.min(add, basin - lith.thick[idx]);
            }
            lith.thick[idx] += (float) add;
            budgetShelf += add;
            if (lith.thick[idx] >= cfg.continentThresholdKm()) {
                lith.type[idx] = Lithosphere.CONTINENTAL;
                lith.oceanAge[idx] = 0f;
                lith.formedAt[idx] = (float) time;
            }
        }
    }

    /** Sea level from the current elevations and water volume (scratch: ceilingScratch). */
    void updateSeaLevel() {
        float[] e = ceilingScratch;
        parallel.cells(grid.size(), (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                e[idx] = (float) Isostasy.elevationM(this, idx);
            }
        });
        seaLevelM = com.platetectonics.geosim.terrain.SeaLevel.fillToVolume(e,
                Math.max(1.0, meanOceanDepthM - iceWaterEquivalentM));
    }

    /** Ocean water volume as a mean depth over the whole planet, m (Earth: 2,620). */
    public void setMeanOceanDepthM(double meanOceanDepthM) {
        this.meanOceanDepthM = meanOceanDepthM;
        updateSeaLevel();
    }

    public double seaLevelM() {
        return seaLevelM;
    }

    public double meanOceanDepthM() {
        return meanOceanDepthM;
    }

    /** Worker threads for the per-cell loops (the planet comes out the same for every count). */
    public void setParallel(Parallel parallel) {
        this.parallel = parallel;
    }

    public Parallel parallel() {
        return parallel;
    }

    /** Turns the Phase 2 erosion stand-in off when real erosion and deposition run instead. */
    public void setErosionStandIn(boolean on) {
        this.erosionStandIn = on;
    }

    /** Oceanic cells enclosed by one plate's continental crust (basins that {@link BasinClosure} closes). */
    public int enclosedOceanCells() {
        return BasinClosure.enclosedCells(this);
    }

    /** Recomputes sea level after outside changes to the crust (erosion, deposition, ice). */
    public void refreshSeaLevel() {
        updateSeaLevel();
    }

    /** Water held in ice sheets, m over the whole planet; takes effect at the next sea-level update. */
    public void setIceWaterEquivalentM(double metres) {
        this.iceWaterEquivalentM = metres;
    }

    /** Elevation on the isostatic datum of one cell, m (sea level is {@link #seaLevelM()}). */
    public double elevationM(int idx) {
        return Isostasy.elevationM(this, idx);
    }

    /**
     * Earth-like crustal growth curve, normalised to 1 at 4.5 Gyr: fast growth early, slowing later
     * (zircon-based models put ~65-70% of today's continental crust in place by 3 Ga).
     */
    double growthCurve(double timeMyr) {
        double tau = cfg.crustGrowthTimescaleMyr();
        return (1.0 - Math.exp(-timeMyr / tau)) / (1.0 - Math.exp(-4500.0 / tau));
    }

    /**
     * Mantle productivity steers arc output toward the growth curve, so seeds differ in where continents
     * are, not in how much continent the planet has. With feedback 0 the rate is fixed.
     */
    private double arcMultiplier() {
        if (cfg.crustGrowthFeedback() <= 0.0) {
            return 1.0;
        }
        double volume = 0.0;
        for (int idx = 0; idx < grid.size(); idx++) {
            if (lith.type[idx] == Lithosphere.CONTINENTAL) {
                volume += lith.thick[idx];
            }
        }
        double areaKm2 = grid.size() * grid.cellAreaKm2();
        double targetKm3 = cfg.continentalCrustKm3PerKm2() * areaKm2 * growthCurve(time);
        double ratio = targetKm3 / Math.max(1.0, volume * grid.cellAreaKm2());
        return Math.max(0.25, Math.min(4.0, Math.pow(ratio, cfg.crustGrowthFeedback())));
    }

    /** Recomputes area, continental area, centroid and oldest ocean for every plate. */
    void refreshStats() {
        int count = plates.size();
        double[] sumCos = new double[count];
        double[] sumSin = new double[count];
        // row sums are whole numbers far below 2^53, exact in any order
        long[] sumJ = new long[count];
        int[] area = new int[count];
        int[] continental = new int[count];
        float[] maxAge = new float[count];
        int w = grid.width();
        int h = grid.height();
        int[] owner = lith.owner;
        byte[] type = lith.type;
        float[] age = lith.oceanAge;
        double[] cos = columnCos;
        double[] sin = columnSin;
        // the sines and cosines add up per plate in cell order, as they always have
        for (int j = 0, idx = 0; j < h; j++) {
            for (int i = 0; i < w; i++, idx++) {
                int o = owner[idx];
                area[o]++;
                sumCos[o] += cos[i];
                sumSin[o] += sin[i];
                sumJ[o] += j;
                if (type[idx] == Lithosphere.CONTINENTAL) {
                    continental[o]++;
                } else if (age[idx] > maxAge[o]) {
                    maxAge[o] = age[idx];
                }
            }
        }
        for (Plate p : plates) {
            p.area = area[p.id];
            p.continentalArea = continental[p.id];
            p.maxOceanAge = maxAge[p.id];
            if (p.area == 0) {
                p.alive = false;
                continue;
            }
            double ci = Math.atan2(sumSin[p.id], sumCos[p.id]) * w / (2.0 * Math.PI);
            p.centroidI = ci < 0 ? ci + w : ci;
            p.centroidJ = (double) sumJ[p.id] / p.area;
        }
    }

    /** Whether plates spin (D-42); an infinite {@code spinDragFactor} turns spin off. */
    boolean spinOn() {
        return Double.isFinite(cfg.spinDragFactor());
    }

    /**
     * Turns plate {@code p} by {@code dTheta} about its centre of area, which stays put (drift moves it
     * separately): the frame origin swings round the centre by the exact rotation, so it may lie far away.
     */
    private static void turn(Plate p, double dTheta) {
        PlateFrame f = p.frame;
        double bx = f.rotX(p.pivotU, p.pivotV);
        double by = f.rotY(p.pivotU, p.pivotV);
        f.set(f.theta() + dTheta);
        p.accX += bx - f.rotX(p.pivotU, p.pivotV);
        p.accY += by - f.rotY(p.pivotU, p.pivotV);
        p.turned += dTheta;
    }

    /** Remembers every plate's total turn, for {@link Plate#turnSinceMarkDeg()}. */
    public void markTurns() {
        for (Plate p : plates) {
            p.turnedMark = p.turned;
        }
    }

    /**
     * Centre of area, moments and reach of every plate in its own frame, and whether it can turn (D-42). A
     * plate whose reach comes near half the world's width would wrap onto itself if it turned (a band round
     * the cylinder cannot turn about a vertical axis): it gets a fresh frame at angle 0 and does not spin.
     * It may turn again once its reach falls below 90% of the limit. A wide plate's frame is refreshed every
     * eighth step, in case its places went round the cylinder while it was a band.
     */
    void refreshSpin() {
        spinStats();
        int count = plates.size();
        boolean[] wide = new boolean[count];
        boolean any = false;
        double limit = grid.width() - 6.0;
        for (Plate p : plates) {
            if (!p.alive) {
                continue;
            }
            double reach = 2.0 * p.radius;
            boolean was = p.canSpin;
            p.canSpin = was ? reach < limit : reach < 0.9 * limit;
            if (!p.canSpin) {
                p.omega = 0.0;
                p.spinDrift = 0.0;
                if (was || !p.frame.identity() || (stepIndex & 7) == 0) {
                    wide[p.id] = true;
                    any = true;
                }
            }
        }
        if (any) {
            reframe(wide);
            spinStats();
        }
    }

    /** Single cells filled inside turned plates because of the rotation's rounding (D-42), since formation. */
    public long roundingHoleCells() {
        return roundingHoleCells;
    }

    /** Cells of new ocean floor made at ridges since formation. */
    public long totalNewOceanCells() {
        return totalNewOceanCells;
    }

    /** Motion substeps since formation. */
    public long totalSubsteps() {
        return totalSubsteps;
    }

    /** Area-weighted RMS speed of the cells from spin alone, cm/yr (D-42); compare {@link StepStats#rmsSpeedCmPerYr}. */
    public double spinRmsCmPerYr() {
        double area = 0.0;
        double sq = 0.0;
        double k = grid.cellKm();
        for (Plate p : plates) {
            if (p.alive) {
                area += p.area;
                sq += p.omega * p.omega * p.inertia * k * k;
            }
        }
        return area > 0.0 ? Math.sqrt(sq / area) / 10.0 : 0.0;
    }

    /**
     * Shares of plate-boundary length that converge, diverge or slide past (transform), from each side's local
     * velocity, spin included: an edge converges or diverges when its closing speed beats its slip speed.
     */
    public double[] boundaryShares() {
        spinStats();
        return PlateDynamics.boundaryShares(this);
    }

    /** Whole-number sums over each plate's places; exact in any order, so the worker threads may add them up. */
    private static final class PlaceSums {
        final long[] su;
        final long[] sv;
        final long[] sq;
        final long[] cu;
        final long[] cv;
        final long[] cq;
        final int[] cn;
        final int[] minU;
        final int[] maxU;
        final int[] minV;
        final int[] maxV;

        PlaceSums(int count) {
            su = new long[count];
            sv = new long[count];
            sq = new long[count];
            cu = new long[count];
            cv = new long[count];
            cq = new long[count];
            cn = new int[count];
            minU = new int[count];
            maxU = new int[count];
            minV = new int[count];
            maxV = new int[count];
            Arrays.fill(minU, Integer.MAX_VALUE);
            Arrays.fill(maxU, Integer.MIN_VALUE);
            Arrays.fill(minV, Integer.MAX_VALUE);
            Arrays.fill(maxV, Integer.MIN_VALUE);
        }

        void add(Lithosphere lith, int from, int to) {
            int[] owner = lith.owner;
            int[] us = lith.u;
            int[] vs = lith.v;
            byte[] type = lith.type;
            for (int idx = from; idx < to; idx++) {
                int o = owner[idx];
                int u = us[idx];
                int v = vs[idx];
                long q = (long) u * u + (long) v * v;
                su[o] += u;
                sv[o] += v;
                sq[o] += q;
                if (type[idx] == Lithosphere.CONTINENTAL) {
                    cu[o] += u;
                    cv[o] += v;
                    cq[o] += q;
                    cn[o]++;
                }
                minU[o] = Math.min(minU[o], u);
                maxU[o] = Math.max(maxU[o], u);
                minV[o] = Math.min(minV[o], v);
                maxV[o] = Math.max(maxV[o], v);
            }
        }

        void merge(PlaceSums o) {
            for (int k = 0; k < su.length; k++) {
                su[k] += o.su[k];
                sv[k] += o.sv[k];
                sq[k] += o.sq[k];
                cu[k] += o.cu[k];
                cv[k] += o.cv[k];
                cq[k] += o.cq[k];
                cn[k] += o.cn[k];
                minU[k] = Math.min(minU[k], o.minU[k]);
                maxU[k] = Math.max(maxU[k], o.maxU[k]);
                minV[k] = Math.min(minV[k], o.minV[k]);
                maxV[k] = Math.max(maxV[k], o.maxV[k]);
            }
        }
    }

    /** Centre of area (frame and world), moments and reach of every plate, from its cells' places. */
    void spinStats() {
        int count = plates.size();
        PlaceSums sums = new PlaceSums(count);
        parallel.cells(grid.size(), (from, to) -> {
            PlaceSums part = new PlaceSums(count);
            part.add(lith, from, to);
            synchronized (sums) {
                sums.merge(part);
            }
        });
        long[] su = sums.su;
        long[] sv = sums.sv;
        long[] sq = sums.sq;
        long[] cu = sums.cu;
        long[] cv = sums.cv;
        long[] cq = sums.cq;
        int[] cn = sums.cn;
        int[] minU = sums.minU;
        int[] maxU = sums.maxU;
        int[] minV = sums.minV;
        int[] maxV = sums.maxV;
        for (Plate p : plates) {
            int a = p.area;
            if (!p.alive || a == 0) {
                continue;
            }
            int o = p.id;
            double pu = (double) su[o] / a;
            double pv = (double) sv[o] / a;
            double m2 = pu * pu + pv * pv;
            p.pivotU = pu;
            p.pivotV = pv;
            p.pivotX = p.originI + p.frame.rotX(pu, pv);
            p.pivotY = p.originJ + p.frame.rotY(pu, pv);
            p.inertia = Math.max(0.0, sq[o] - a * m2);
            double cont = Math.max(0.0, cq[o] - 2.0 * (pu * cu[o] + pv * cv[o]) + cn[o] * m2);
            p.spinDrag = p.inertia + cfg.continentalDrag() * cont;
            double du = Math.max(pu - minU[o], maxU[o] - pu);
            double dv = Math.max(pv - minV[o], maxV[o] - pv);
            p.radius = Math.hypot(du, dv);
        }
    }

    /**
     * Gives the marked plates (by id) fresh frames at angle 0 (D-42): each cell's place is its offset from the
     * plate's centre, measured from the far side of the plate's widest run of empty columns, so the plate is in
     * one piece. Nothing moves.
     */
    void reframe(boolean[] marked) {
        int w = grid.width();
        int count = plates.size();
        int[] slot = new int[count];
        int slots = 0;
        for (int k = 0; k < count; k++) {
            slot[k] = k < marked.length && marked[k] ? slots++ : -1;
        }
        if (slots == 0) {
            return;
        }
        int[] owner = lith.owner;
        int n = owner.length;
        int used = slots;
        // the passes run on the workers: marks are all `true`, and the sums are whole numbers (exact in any order)
        boolean[] usedColumn = new boolean[used * w];
        parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                int s = slot[owner[idx]];
                if (s >= 0) {
                    usedColumn[s * w + idx % w] = true;
                }
            }
        });
        int[] start = new int[slots];
        for (int s = 0; s < slots; s++) {
            start[s] = windowStart(usedColumn, s * w, w);
        }
        long[] sumX = new long[slots];
        long[] sumY = new long[slots];
        long[] cells = new long[slots];
        parallel.cells(n, (from, to) -> {
            long[] px = new long[used];
            long[] py = new long[used];
            long[] pn = new long[used];
            for (int idx = from; idx < to; idx++) {
                int s = slot[owner[idx]];
                if (s >= 0) {
                    px[s] += Math.floorMod(idx % w - start[s], w);
                    py[s] += idx / w;
                    pn[s]++;
                }
            }
            synchronized (sumX) {
                for (int s = 0; s < used; s++) {
                    sumX[s] += px[s];
                    sumY[s] += py[s];
                    cells[s] += pn[s];
                }
            }
        });
        int[] cx = new int[slots];
        int[] cy = new int[slots];
        for (int k = 0; k < count; k++) {
            int s = slot[k];
            if (s < 0 || cells[s] == 0) {
                continue;
            }
            cx[s] = (int) Math.floor((double) sumX[s] / cells[s] + 0.5);
            cy[s] = (int) Math.floor((double) sumY[s] / cells[s] + 0.5);
            Plate p = plates.get(k);
            p.originI = Math.floorMod(start[s] + cx[s], w);
            p.originJ = cy[s];
            p.frame.set(0.0);
        }
        int[] u = lith.u;
        int[] v = lith.v;
        parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                int s = slot[owner[idx]];
                if (s >= 0) {
                    u[idx] = Math.floorMod(idx % w - start[s], w) - cx[s];
                    v[idx] = idx / w - cy[s];
                }
            }
        });
    }

    /** Fresh frame for one plate (see {@link #reframe(boolean[])}). */
    void reframe(Plate p) {
        boolean[] marked = new boolean[plates.size()];
        marked[p.id] = true;
        reframe(marked);
    }

    /** First column after the longest run of columns the plate does not use (wrapping); 0 if it uses all. */
    private static int windowStart(boolean[] used, int off, int w) {
        int first = -1;
        for (int i = 0; i < w; i++) {
            if (used[off + i]) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            return 0;
        }
        int bestLen = 0;
        int bestStart = 0;
        int run = 0;
        for (int k = 1; k <= w; k++) {
            int i = (first + k) % w;
            if (!used[off + i]) {
                run++;
            } else {
                if (run > bestLen) {
                    bestLen = run;
                    bestStart = i;
                }
                run = 0;
            }
        }
        return bestLen == 0 ? 0 : bestStart;
    }

    /** Signed east-west distance in cells, wrapped to [-W/2, W/2). */
    double wrapDelta(double d) {
        int w = grid.width();
        d = d % w;
        if (d >= w / 2.0) {
            d -= w;
        } else if (d < -w / 2.0) {
            d += w;
        }
        return d;
    }

    Plate newPlate(double vx, double vy) {
        Plate p = new Plate(plates.size(), vx, vy, time);
        plates.add(p);
        return p;
    }

    int alivePlateCount() {
        int c = 0;
        for (Plate p : plates) {
            if (p.alive) {
                c++;
            }
        }
        return c;
    }

    void addTrench(int idx) {
        if (trenchCount == trench.length) {
            trench = Arrays.copyOf(trench, trench.length * 2);
        }
        trench[trenchCount++] = idx;
    }

    // ------------------------------------------------------------------ read access

    public CylinderGrid grid() {
        return grid;
    }

    public Lithosphere lithosphere() {
        return lith;
    }

    public List<Plate> plates() {
        return Collections.unmodifiableList(plates);
    }

    public EventLog events() {
        return events;
    }

    public double timeMyr() {
        return time;
    }

    public TectonicsConfig config() {
        return cfg;
    }

    /** True if any step so far ended crustal flow before the crust was fully relaxed. */
    public boolean flowBudgetExhausted() {
        return flowBudgetExhausted;
    }

    /** Cumulative sources and sinks of crust since formation, in 10^9 km3. */
    public CrustBudget crustBudget() {
        double s = grid.cellAreaKm2() / 1e9;
        return new CrustBudget(budgetArc * s, budgetShelf * s, budgetErosion * s, budgetCollision * s,
                budgetEclogite * s, budgetSubducted * s, budgetForearc * s, budgetClosure * s, budgetRounding * s);
    }

    /**
     * @param arcs      crust added by arc magmatism (ocean and continent)
     * @param shelves   eroded crust deposited on shelves (part of {@code erosion})
     * @param erosion   crust removed from thick continents
     * @param collision crust lost when continents stack (1 - collisionTransfer)
     * @param eclogite  lower crust turned into eclogite keel
     * @param subducted thickened ocean crust (young arcs, plateaus) lost down trenches, beyond normal 7 km crust
     * @param forearc   crust scraped off overriding plates by subduction erosion
     * @param closure   ocean enclosed by one plate's continent, closed into continent (D-36)
     * @param rounding  crust beyond new ocean crust copied into single-cell holes that the rotation's rounding opens
     *                  in turned plates (D-42)
     */
    public record CrustBudget(double arcs, double shelves, double erosion, double collision, double eclogite,
                              double subducted, double forearc, double closure, double rounding) {
    }

    public StepStats stats() {
        double area = 0;
        double sq = 0;
        long cont = 0;
        double k = grid.cellKm();
        for (Plate p : plates) {
            if (p.alive) {
                area += p.area;
                sq += p.area * (p.vx * p.vx + p.vy * p.vy);
                if (p.omega != 0.0) {
                    sq += p.omega * p.omega * p.inertia * k * k; // every cell's speed, spin included (D-42)
                }
                cont += p.continentalArea;
            }
        }
        double volume = 0.0;
        double maxKeel = 0.0;
        for (int idx = 0; idx < grid.size(); idx++) {
            if (lith.type[idx] == Lithosphere.CONTINENTAL) {
                volume += lith.thick[idx];
            }
            maxKeel = Math.max(maxKeel, lith.keel[idx]);
        }
        return new StepStats(time, alivePlateCount(), (double) cont / grid.size(), Math.sqrt(sq / area) / 10.0,
                subductedCells, collisionCells, newOceanCells, lastSubsteps,
                volume * grid.cellAreaKm2() / 1e9, maxKeel);
    }

    /**
     * Summary after a step.
     *
     * @param rmsSpeedCmPerYr       area-weighted RMS speed of the cells, drift and spin
     * @param continentalVolumeGkm3 continental crust volume in 10^9 km3 (Earth today: about 7)
     * @param maxKeelKm             thickest dense keel under any crust
     */
    public record StepStats(double timeMyr, int plates, double continentalFraction, double rmsSpeedCmPerYr,
                            long subductedCells, long collisionCells, long newOceanCells, int substeps,
                            double continentalVolumeGkm3, double maxKeelKm) {
    }

}
