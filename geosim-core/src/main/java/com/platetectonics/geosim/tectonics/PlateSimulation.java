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
    final float[] ceilingScratch;
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

    public PlateSimulation(CylinderGrid grid, long seed, TectonicsConfig cfg) {
        this.grid = grid;
        this.seed = seed;
        this.cfg = cfg;
        this.lith = new Lithosphere(grid);
        this.queue = new int[grid.size()];
        this.dist = new int[grid.size()];
        this.ceilingScratch = new float[grid.size()];
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
        updateSeaLevel();
        arcMultiplier = arcMultiplier();
        PlateDynamics.updateVelocities(this, dtMyr);

        double maxCells = 0.0;
        for (Plate p : plates) {
            if (p.alive) {
                maxCells = Math.max(maxCells, Math.max(Math.abs(p.vx), Math.abs(p.vy)) * dtMyr / grid.cellKm());
            }
        }
        if (underthrustCrust.length < plates.size()) {
            underthrustCrust = new double[plates.size()];
            underthrustKeel = new double[plates.size()];
            underthrustEclogite = new double[plates.size()];
        }
        int substeps = Math.max(1, (int) Math.ceil(maxCells / cfg.maxCellsPerSubstep()));
        double subDt = dtMyr / substeps;
        for (int s = 0; s < substeps; s++) {
            for (Plate p : plates) {
                if (!p.alive) {
                    continue;
                }
                p.accX += p.vx * subDt / grid.cellKm();
                p.accY += p.vy * subDt / grid.cellKm();
                p.shiftX = (int) Math.floor(p.accX);
                p.shiftY = (int) Math.floor(p.accY);
                p.accX -= p.shiftX;
                p.accY -= p.shiftY;
            }
            PlateMotion.substep(this, subDt, time + (s + 1) * subDt);
        }
        lastSubsteps = substeps;

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
        double[] sumJ = new double[count];
        for (Plate p : plates) {
            p.area = 0;
            p.continentalArea = 0;
            p.maxOceanAge = 0f;
        }
        int w = grid.width();
        for (int idx = 0; idx < grid.size(); idx++) {
            int o = lith.owner[idx];
            Plate p = plates.get(o);
            p.area++;
            int i = idx % w;
            sumCos[o] += columnCos[i];
            sumSin[o] += columnSin[i];
            sumJ[o] += idx / w;
            if (lith.type[idx] == Lithosphere.CONTINENTAL) {
                p.continentalArea++;
            } else if (lith.oceanAge[idx] > p.maxOceanAge) {
                p.maxOceanAge = lith.oceanAge[idx];
            }
        }
        for (Plate p : plates) {
            if (p.area == 0) {
                p.alive = false;
                continue;
            }
            double ci = Math.atan2(sumSin[p.id], sumCos[p.id]) * w / (2.0 * Math.PI);
            p.centroidI = ci < 0 ? ci + w : ci;
            p.centroidJ = sumJ[p.id] / p.area;
        }
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
                budgetEclogite * s, budgetSubducted * s, budgetForearc * s);
    }

    /**
     * @param arcs      crust added by arc magmatism (ocean and continent)
     * @param shelves   eroded crust deposited on shelves (part of {@code erosion})
     * @param erosion   crust removed from thick continents
     * @param collision crust lost when continents stack (1 - collisionTransfer)
     * @param eclogite  lower crust turned into eclogite keel
     * @param subducted thickened ocean crust (young arcs, plateaus) lost down trenches, beyond normal 7 km crust
     * @param forearc   crust scraped off overriding plates by subduction erosion
     */
    public record CrustBudget(double arcs, double shelves, double erosion, double collision, double eclogite,
                              double subducted, double forearc) {
    }

    public StepStats stats() {
        double area = 0;
        double sq = 0;
        long cont = 0;
        for (Plate p : plates) {
            if (p.alive) {
                area += p.area;
                sq += p.area * (p.vx * p.vx + p.vy * p.vy);
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
     * @param rmsSpeedCmPerYr       area-weighted RMS plate speed
     * @param continentalVolumeGkm3 continental crust volume in 10^9 km3 (Earth today: about 7)
     * @param maxKeelKm             thickest dense keel under any crust
     */
    public record StepStats(double timeMyr, int plates, double continentalFraction, double rmsSpeedCmPerYr,
                            long subductedCells, long collisionCells, long newOceanCells, int substeps,
                            double continentalVolumeGkm3, double maxKeelKm) {
    }

}
