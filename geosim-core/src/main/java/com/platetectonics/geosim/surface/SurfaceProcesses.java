package com.platetectonics.geosim.surface;

import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.Milestone;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.tectonics.Isostasy;
import com.platetectonics.geosim.tectonics.Lithosphere;
import com.platetectonics.geosim.tectonics.PlateSimulation;

import java.util.Arrays;

/**
 * Rivers, erosion, deposition and weathering on the master grid
 * (docs/design/04-surface-processes.md#rivers-erosion-transport-and-deposition), once per step after the
 * plates have moved:
 * <ol>
 *   <li><b>Flow</b>: D8 routing over the filled surface ({@link FlowRouting}); runoff from rain minus
 *       evaporation is summed downstream into discharge. A lake whose evaporation exceeds its inflow
 *       does not spill: an endorheic basin.</li>
 *   <li><b>Erosion</b> (outlets first): stream power, rock erosion = K Q^m S^n, solved implicitly so any
 *       step length is stable (Braun &amp; Willett 2013). Removing rock lets the crust rebound, so the
 *       surface only drops by the isostatic factor (15% for crystalline crust): old ranges wear down
 *       over hundreds of Myr while young ones stay sharp. Soft sediment, bare land before plants and
 *       glaciers erode faster.</li>
 *   <li><b>Transport</b> (headwaters first): rivers carry up to K_t Q S. Where they carry more they
 *       deposit, just enough that the steepened channel carries the rest (alluvial fans at mountain
 *       fronts, floodplains). Lakes trap sediment until they are full.</li>
 *   <li><b>Sea</b>: sediment reaching a river mouth builds the shelf out to the shelf depth, then spreads
 *       as turbidite aprons, then builds a delta top. Clay and dust settle everywhere; carbonate ooze above
 *       the compensation depth once calcareous plankton exist; reefs and platforms in warm shallow seas.</li>
 *   <li><b>Weathering</b>: a regolith front advances (fast when hot and wet, slower under thick soil)
 *       and is stripped by erosion and ice: deep laterite profiles grow only on old, flat, tropical land.</li>
 * </ol>
 * Every change goes through {@link Lithosphere#changeSurface}, so crust thickness and elevation follow.
 */
public final class SurfaceProcesses {
    private final CylinderGrid grid;
    private final SurfaceConfig cfg;
    private final FlowRouting flow;
    private final float[] elevation;
    private final float[] hNew;
    private final float[] runoffM;
    private final float[] discharge;
    private final float[] eroded;
    private final float[] sedimentFlux;
    private final float[] minDonorM;
    private final float[] marineInput;
    private final float[] lakeInflow;
    private final float[] lakeEvaporation;
    private final int[] spill;
    private final boolean[] closedSpill;
    /** Marine spreading: sea cells in the order they were taken from one mouth. */
    private final int[] queue;
    /**
     * Marine spreading: queued sea cells in rings of whole cells of distance from the mouth, first come
     * first served within a ring. A neighbour lies at most one ring further out, so two buffers hold the
     * queue: this ring and the next (round fronts at constant cost per cell).
     */
    private int[] ringNow;
    private int[] ringNext;
    private int ringHead;
    private int ringTail;
    private int nextTail;
    private int ring;
    /** (ring + 1)²: cells closer than this (squared, in cells) belong to the current ring. */
    private long ringLimit;
    /** Marine spreading: the mouth that last queued each ocean cell; land cells hold {@code Integer.MAX_VALUE}. */
    private final int[] stampOf;
    private final int[] apron;
    private final double[] pelagicScratch;
    private final double[] carbonateScratch;
    private int stamp;
    private Parallel parallel = Parallel.SERIAL;

    /** Rock erosion rate of the last step, m/Myr (0 at sea and where sediment was deposited). */
    public final float[] erosionMPerMyr;
    /** Net deposition of the last step, m of sediment. */
    public final float[] depositM;
    /** 0 = no lake, 1 = lake that spills, 2 = endorheic lake (evaporation exceeds inflow). */
    public final byte[] lakes;

    private double meanLandErosionMPerMyr;
    private double erodedKm3;
    private double landDepositKm3;
    private double marineDepositKm3;
    private double pelagicKm3;
    private double carbonateKm3;
    private double deltaPlainKm3;

    public SurfaceProcesses(CylinderGrid grid, SurfaceConfig cfg) {
        this.grid = grid;
        this.cfg = cfg;
        int n = grid.size();
        flow = new FlowRouting(grid);
        elevation = new float[n];
        hNew = new float[n];
        runoffM = new float[n];
        discharge = new float[n];
        eroded = new float[n];
        sedimentFlux = new float[n];
        minDonorM = new float[n];
        marineInput = new float[n];
        lakeInflow = new float[n];
        lakeEvaporation = new float[n];
        spill = new int[n];
        closedSpill = new boolean[n];
        queue = new int[n];
        ringNow = new int[n];
        ringNext = new int[n];
        stampOf = new int[n];
        apron = new int[n];
        pelagicScratch = new double[n];
        carbonateScratch = new double[n];
        erosionMPerMyr = new float[n];
        depositM = new float[n];
        lakes = new byte[n];
    }

    /**
     * Worker threads for the per-cell loops. Routing, erosion, transport and marine spreading follow the
     * drainage order or share sediment between cells, so they stay on one thread.
     */
    public void setParallel(Parallel parallel) {
        this.parallel = parallel;
        flow.setParallel(parallel);
    }

    /** One step of {@code dtMyr} on the current plates, climate and global state. */
    public void step(PlateSimulation sim, Climate climate, GlobalState g, double dtMyr) {
        Lithosphere lith = sim.lithosphere();
        int n = grid.size();
        double sea = sim.seaLevelM();
        parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                elevation[idx] = (float) sim.elevationM(idx);
            }
        });
        flow.route(elevation, sea);
        Arrays.fill(erosionMPerMyr, 0f);
        Arrays.fill(depositM, 0f);
        Arrays.fill(lakes, (byte) 0);
        routeWater(climate);
        double erodedNow = erode(lith, climate, g, dtMyr);
        double landNow = transport(lith, dtMyr);
        double[] marineNow = {0.0, 0.0};
        stamp = 0;
        parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                stampOf[idx] = flow.isOcean(idx) ? 0 : Integer.MAX_VALUE;
            }
        });
        for (int idx = 0; idx < n; idx++) {
            if (marineInput[idx] > 0f) {
                spreadMarine(lith, idx, marineInput[idx], sea, marineNow);
            }
        }
        seaFloor(lith, climate, g, sea, dtMyr);
        weather(lith, climate, g, dtMyr);

        erodedKm3 += erodedNow;
        landDepositKm3 += landNow;
        marineDepositKm3 += marineNow[0];
        deltaPlainKm3 += marineNow[1];
        double sum = 0.0;
        int land = flow.landCount();
        for (int k = 0; k < land; k++) {
            sum += erosionMPerMyr[flow.order[k]];
        }
        meanLandErosionMPerMyr = land == 0 ? 0.0 : sum / land;
    }

    // ------------------------------------------------------------------ water

    private void routeWater(Climate climate) {
        int land = flow.landCount();
        int[] order = flow.order;
        int[] rec = flow.receiver;
        double area = grid.cellAreaKm2();
        Arrays.fill(discharge, 0f);
        Arrays.fill(lakeInflow, 0f);
        Arrays.fill(lakeEvaporation, 0f);
        Arrays.fill(closedSpill, false);
        Arrays.fill(runoffM, 0f);
        for (int k = 0; k < land; k++) {
            int idx = order[k];
            double p = climate.precipitationMm[idx] / 1000.0;
            double pet = Math.max(0.0, 0.06 * (climate.temperatureC[idx] + 3.0));
            double runoff = climate.iceM[idx] > 0f ? p : p + pet <= 0.0 ? 0.0 : p * p / (p + pet);
            runoffM[idx] = (float) runoff;
            discharge[idx] = (float) (runoff * area / 1000.0);
            if (flow.isLake(idx)) {
                int r = rec[idx];
                spill[idx] = flow.isLake(r) ? spill[r] : r;
                lakeEvaporation[spill[idx]] += (float) (area * Math.max(0.0, pet - p) / 1000.0);
            } else {
                spill[idx] = -1;
            }
        }
        for (int k = land - 1; k >= 0; k--) {
            int idx = order[k];
            if (lakeInflow[idx] > 0f || lakeEvaporation[idx] > 0f) {
                // idx is the spill point of a lake: all its inflow has arrived
                double net = lakeInflow[idx] - lakeEvaporation[idx];
                if (net > 0.0) {
                    discharge[idx] += (float) net;
                } else {
                    closedSpill[idx] = true;
                }
            }
            int r = rec[idx];
            if (flow.isLake(idx) && !flow.isLake(r)) {
                lakeInflow[r] += discharge[idx];
            } else {
                discharge[r] += discharge[idx];
            }
        }
        for (int k = 0; k < land; k++) {
            int idx = order[k];
            if (flow.isLake(idx)) {
                lakes[idx] = (byte) (closedSpill[spill[idx]] ? 2 : 1);
            }
        }
    }

    // ------------------------------------------------------------------ erosion

    /** Returns the volume eroded, km3. */
    private double erode(Lithosphere lith, Climate climate, GlobalState g, double dt) {
        int land = flow.landCount();
        int[] order = flow.order;
        double area = grid.cellAreaKm2();
        double veg = vegetationErodibility(g);
        double total = 0.0;
        for (int k = 0; k < land; k++) {
            int idx = order[k];
            float h0 = elevation[idx];
            hNew[idx] = h0;
            eroded[idx] = 0f;
            if (flow.isLake(idx)) {
                continue;
            }
            double base = baseLevel(flow.receiver[idx]);
            double q = discharge[idx];
            if (h0 <= base || q <= 0.0) {
                continue;
            }
            boolean continental = lith.isContinental(idx);
            double sed = lith.sedimentKm(idx);
            double fSed = continental ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
            double fRock = continental || lith.thicknessKm(idx) - sed > 7.0 ? Isostasy.AIRY_FACTOR : 0.0;
            boolean soft = sed > 0.01;
            double f = soft ? fSed : fRock;
            if (f <= 0.0) {
                continue;
            }
            double ice = climate.iceM[idx];
            double erodibility = veg * (soft ? cfg.softSedimentFactor() : 1.0)
                    * (1.0 + cfg.glacialFactor() * Math.min(1.0, ice / 300.0));
            double c = f * dt * cfg.streamPowerK() * erodibility * Math.pow(q, cfg.dischargeExponent())
                    / Math.pow(flow.lengthM[idx], cfg.slopeExponent());
            double drop = (h0 - base) - solveDrop(h0 - base, c, cfg.slopeExponent());
            // hillslopes and small valleys inside the cell: their relief, and erosion, grow with elevation
            double reliefKm = (h0 - flow.seaLevel()) / 1000.0;
            double wet = Math.max(0.2, Math.min(2.0, runoffM[idx] / 0.5));
            double hillRockM = cfg.reliefErosionK() * reliefKm * reliefKm * wet * erodibility * dt;
            drop = Math.min(h0 - base, drop + f * hillRockM);
            // the surface drops by f per unit of rock removed: through sediment first, then basement
            double sedDrop = soft ? Math.min(drop, fSed * 1000.0 * sed) : 0.0;
            double removedKm = sedDrop / (fSed * 1000.0);
            double rockDrop = fRock > 0.0 ? drop - sedDrop : 0.0;
            removedKm += rockDrop / (fRock > 0.0 ? fRock * 1000.0 : 1.0);
            removedKm = Math.min(removedKm, lith.thicknessKm(idx) - 0.1);
            if (removedKm <= 0.0) {
                continue;
            }
            lith.changeSurface(idx, (float) -removedKm, false);
            hNew[idx] = (float) (h0 - sedDrop - rockDrop);
            eroded[idx] = (float) (removedKm * area);
            erosionMPerMyr[idx] = (float) (removedKm * 1000.0 / dt);
            total += removedKm * area;
        }
        return total;
    }

    /**
     * Implicit stream power for one cell: height {@code d} above the (already updated) receiver, solve
     * y + c y^n = d for the new height y.
     */
    static double solveDrop(double d, double c, double n) {
        if (c <= 0.0 || d <= 0.0) {
            return Math.max(0.0, d);
        }
        if (n == 1.0) {
            return d / (1.0 + c);
        }
        if (n == 2.0) {
            return (Math.sqrt(1.0 + 4.0 * c * d) - 1.0) / (2.0 * c);
        }
        double y = d;
        for (int it = 0; it < 30; it++) {
            double fy = y + c * Math.pow(y, n) - d;
            double dfy = 1.0 + n * c * Math.pow(y, n - 1.0);
            double next = Math.max(0.0, Math.min(d, y - fy / dfy));
            if (Math.abs(next - y) < 1e-6) {
                return next;
            }
            y = next;
        }
        return y;
    }

    private double baseLevel(int r) {
        if (flow.isOcean(r)) {
            return flow.seaLevel();
        }
        return flow.isLake(r) ? flow.filled[r] : hNew[r];
    }

    /** Bare land erodes faster than land held by roots. */
    private double vegetationErodibility(GlobalState g) {
        if (g.has(Milestone.FORESTS)) {
            return 1.0;
        }
        return g.has(Milestone.LAND_PLANTS) ? 1.0 + 0.3 * (cfg.bareLandFactor() - 1.0) : cfg.bareLandFactor();
    }

    // ------------------------------------------------------------------ transport and deposition on land

    /** Returns the volume deposited on land and in lakes, km3. */
    private double transport(Lithosphere lith, double dt) {
        int land = flow.landCount();
        int[] order = flow.order;
        double area = grid.cellAreaKm2();
        Arrays.fill(sedimentFlux, 0f);
        Arrays.fill(marineInput, 0f);
        Arrays.fill(minDonorM, Float.MAX_VALUE);
        double total = 0.0;
        for (int k = land - 1; k >= 0; k--) {
            int idx = order[k];
            int r = flow.receiver[idx];
            double qs = sedimentFlux[idx] + eroded[idx];
            double h = hNew[idx];
            double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
            double d = 0.0;
            if (qs > 0.0) {
                if (flow.isLake(idx)) {
                    d = Math.min(qs, Math.max(0.0, flow.filled[idx] - h) / (fs * 1000.0) * area);
                } else {
                    double len = flow.lengthM[idx];
                    double s0 = Math.max(0.0, (h - baseLevel(r)) / len);
                    double kappa = cfg.transportK() * discharge[idx] * dt;
                    // deposit just enough that the steepened channel carries the rest
                    d = (qs - kappa * s0) / (1.0 + kappa * fs * 1000.0 / (area * len));
                    d = Math.max(0.0, Math.min(qs, d));
                    if (minDonorM[idx] < Float.MAX_VALUE) {
                        // never bury the cells that feed it
                        d = Math.min(d, Math.max(0.0, minDonorM[idx] - h) / (fs * 1000.0) * area);
                    }
                }
                if (d > 0.0) {
                    double km = d / area;
                    lith.changeSurface(idx, (float) km, true);
                    h += fs * 1000.0 * km;
                    depositM[idx] += (float) (km * 1000.0);
                    total += d;
                }
            }
            hNew[idx] = (float) h;
            if (h < minDonorM[r]) {
                minDonorM[r] = (float) h;
            }
            double out = qs - d;
            if (flow.isOcean(r)) {
                marineInput[r] += (float) out;
            } else {
                sedimentFlux[r] += (float) out;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ sea

    /**
     * Builds shelf, apron and delta top from one river mouth; adds to {@code totals} (deposited, of it
     * above sea level). Sea cells are taken nearest first by straight-line distance from the mouth (reached
     * through the sea, edge to edge), so shelves and deltas grow as round fans.
     */
    private void spreadMarine(Lithosphere lith, int mouth, double volume, double sea, double[] totals) {
        int w = grid.width();
        double shelfLevel = sea - cfg.shelfDepthM();
        double shelfCells = Math.max(1.0, cfg.shelfRadiusKm() / grid.cellKm());
        double apronCells = Math.max(shelfCells + 1.0, cfg.apronRadiusKm() / grid.cellKm());
        long shelf2 = (long) (shelfCells * shelfCells);
        int apronRing = (int) apronCells;
        // Land cells hold Integer.MAX_VALUE in stampOf (set in step), so one test skips land and queued cells.
        int s = ++stamp;
        int mi = mouth % w;
        int mj = mouth / w;
        ring = 0;
        ringLimit = 1;
        ringHead = 0;
        ringTail = 0;
        nextTail = 0;
        stampOf[mouth] = s;
        ringNow[ringTail++] = mouth;
        int visited = 0;
        int apronCount = 0;
        // with no deep water in reach yet, keep looking further out before building delta top
        int reach = 3 * apronRing;
        double left = volume;
        while (left > 0.0 && queued() && nextRing() <= reach) {
            int c = ringNow[ringHead++];
            queue[visited++] = c;
            if (distance2(c % w, c / w, mi, mj) <= shelf2) {
                left -= fillTo(lith, c, shelfLevel, left);
            } else if (elevation[c] < shelfLevel) {
                apron[apronCount++] = c;
                reach = apronRing;
            }
            pushSeaNeighbours(c, s, mi, mj);
        }
        if (left > 0.0 && apronCount > 0) {
            left = fillApron(lith, apronCount, shelfLevel, left);
        }
        // a sea already filled to shelf depth: the delta top builds to sea level, prograding across the
        // sea as far as it takes, then the delta plain rises above it
        double topLevel = sea - 1.0;
        for (int q = 0; q < visited && left > 0.0; q++) {
            left -= fillTo(lith, queue[q], topLevel, left);
        }
        while (left > 0.0 && queued()) {
            nextRing();
            int c = ringNow[ringHead++];
            queue[visited++] = c;
            left -= fillTo(lith, c, topLevel, left);
            pushSeaNeighbours(c, s, mi, mj);
        }
        if (left > 0.0) {
            double km = left / visited / grid.cellAreaKm2();
            for (int q = 0; q < visited; q++) {
                depositKm(lith, queue[q], km);
            }
            totals[1] += left;
        }
        totals[0] += volume;
    }

    /** Queues the sea cells beside {@code c} not yet queued from this mouth, by their distance from it. */
    private void pushSeaNeighbours(int c, int s, int mi, int mj) {
        int w = grid.width();
        int cj = c / w;
        int rowStart = cj * w;
        int ci = c - rowStart;
        int east = ci + 1 == w ? 0 : ci + 1;
        int west = ci == 0 ? w - 1 : ci - 1;
        pushSea(rowStart + east, east, cj, s, mi, mj);
        pushSea(rowStart + west, west, cj, s, mi, mj);
        if (cj > 0) {
            pushSea(c - w, ci, cj - 1, s, mi, mj);
        }
        if (cj + 1 < grid.height()) {
            pushSea(c + w, ci, cj + 1, s, mi, mj);
        }
    }

    private void pushSea(int idx, int i, int j, int s, int mi, int mj) {
        if (stampOf[idx] >= s) {
            return;
        }
        stampOf[idx] = s;
        // a cell inside the current ring (a way round a cape) joins it
        if (distance2(i, j, mi, mj) < ringLimit) {
            ringNow[ringTail++] = idx;
        } else {
            ringNext[nextTail++] = idx;
        }
    }

    /** Squared distance in cells between (i, j) and (mi, mj), across the date line where that is shorter. */
    private long distance2(int i, int j, int mi, int mj) {
        int w = grid.width();
        int di = Math.abs(i - mi);
        di = Math.min(di, w - di);
        long dj = j - mj;
        return (long) di * di + dj * dj;
    }

    private boolean queued() {
        return ringHead < ringTail || nextTail > 0;
    }

    /** Ring of the next cell to come out, moving on to the next ring when this one is used up. */
    private int nextRing() {
        if (ringHead == ringTail) {
            int[] t = ringNow;
            ringNow = ringNext;
            ringNext = t;
            ringHead = 0;
            ringTail = nextTail;
            nextTail = 0;
            ring++;
            ringLimit = (long) (ring + 1) * (ring + 1);
        }
        return ring;
    }

    /**
     * Turbidite aprons: equal shares over the deep cells in reach, none built above {@code level} (a full
     * cell passes its share on to the others). Returns the volume that did not fit, km3.
     */
    private double fillApron(Lithosphere lith, int count, double level, double left) {
        int active = count;
        while (left > 0.0 && active > 0) {
            double share = left / active;
            int stillDeep = 0;
            for (int a = 0; a < active; a++) {
                int c = apron[a];
                double d = fillTo(lith, c, level, share);
                left -= d;
                if (d >= share) {
                    apron[stillDeep++] = c;
                }
            }
            if (stillDeep == active) {
                return 0.0; // every cell took its full share
            }
            active = stillDeep;
        }
        return Math.max(0.0, left);
    }

    private double fillTo(Lithosphere lith, int idx, double level, double available) {
        if (elevation[idx] >= level) {
            return 0.0;
        }
        double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
        double room = (level - elevation[idx]) / (fs * 1000.0) * grid.cellAreaKm2();
        double d = Math.min(available, room);
        deposit(lith, idx, d);
        return d;
    }

    /**
     * Sediment on ocean floor stays oceanic (a passive-margin wedge on transitional crust): it rides to the
     * trench, where part is scraped into the overriding plate and the rest subducts. Turning thick piles
     * into continental crust would make unsinkable crust out of sediment alone.
     */
    private void deposit(Lithosphere lith, int idx, double km3) {
        depositKm(lith, idx, km3 / grid.cellAreaKm2());
    }

    private void depositKm(Lithosphere lith, int idx, double km) {
        double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
        lith.changeSurface(idx, (float) km, true);
        elevation[idx] += (float) (fs * 1000.0 * km);
        depositM[idx] += (float) (km * 1000.0);
    }

    /** Pelagic clay, carbonate ooze and shallow-water carbonate. */
    private void seaFloor(Lithosphere lith, Climate climate, GlobalState g, double sea, double dt) {
        double area = grid.cellAreaKm2();
        boolean ooze = g.has(Milestone.CALCAREOUS_PLANKTON);
        double factory = g.has(Milestone.ANIMALS) ? 1.0 : g.has(Milestone.LIFE) ? 0.3 : 0.1;
        // each cell only receives its own sediment; the totals are summed afterwards in cell order
        parallel.cells(grid.size(), (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                pelagicScratch[idx] = 0.0;
                carbonateScratch[idx] = 0.0;
                if (!flow.isOcean(idx)) {
                    continue;
                }
                double depth = sea - elevation[idx];
                if (depth <= 5.0) {
                    continue;
                }
                double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
                double clay = cfg.pelagicMPerMyr() + (ooze && depth < cfg.ccdDepthM() ? cfg.oozeMPerMyr() : 0.0);
                // settling mud never builds land: it stops a few metres below the surface
                double km = Math.min(clay * dt / 1000.0, (depth - 5.0) / (fs * 1000.0));
                pelagicScratch[idx] = km * area;
                deposit(lith, idx, km * area);
                depth = sea - elevation[idx];
                if (depth > 5.0 && depth < cfg.carbonateDepthM() && climate.temperatureC[idx] >= cfg.carbonateMinTempC()) {
                    double grow = Math.min(cfg.carbonateMPerMyr() * factory * dt / 1000.0, (depth - 5.0) / (fs * 1000.0));
                    carbonateScratch[idx] = grow * area;
                    deposit(lith, idx, grow * area);
                }
            }
        });
        for (int idx = 0; idx < grid.size(); idx++) {
            pelagicKm3 += pelagicScratch[idx];
            carbonateKm3 += carbonateScratch[idx];
        }
    }

    // ------------------------------------------------------------------ weathering

    private void weather(Lithosphere lith, Climate climate, GlobalState g, double dt) {
        double plants = g.has(Milestone.FORESTS) ? 1.0 : g.has(Milestone.LAND_PLANTS) ? 0.8
                : g.has(Milestone.PHOTOSYNTHESIS) ? 0.35 : 0.25;
        double h0 = cfg.soilDecayDepthM();
        int land = flow.landCount();
        parallel.cells(land, (from, to) -> {
            for (int k = from; k < to; k++) {
                int idx = flow.order[k];
                double ice = climate.iceM[idx];
                double regolith = lith.regolithM(idx);
                if (ice > 0f) {
                    lith.setLastGlaciated(idx, (float) g.timeMyr());
                }
                if (ice > 100f) {
                    // ice sheets scrape shields down to fresh rock
                    lith.setRegolithM(idx, (float) (regolith * Math.exp(-dt / 0.5)));
                    continue;
                }
                double production = cfg.soilProductionMPerMyr() * plants * Math.min(3.0, runoffM[idx] / 0.5)
                        * Math.exp((climate.temperatureC[idx] - 15.0) / 13.7);
                double e = erosionMPerMyr[idx];
                // dH/dt = P exp(-H / h0) - E, solved exactly over the step with u = exp(H / h0)
                double u0 = Math.exp(Math.min(40.0, regolith / h0));
                double u;
                if (e > 1e-6) {
                    double eq = production / e;
                    u = eq + (u0 - eq) * Math.exp(-e * dt / h0);
                } else {
                    u = u0 + production * dt / h0;
                }
                lith.setRegolithM(idx, (float) (h0 * Math.log(Math.max(1.0, u))));
            }
        });
    }

    // ------------------------------------------------------------------ read access

    public FlowRouting flow() {
        return flow;
    }

    /** River discharge of the last step, km3/yr (at river mouths: the river's total). */
    public float[] dischargeKm3PerYr() {
        return discharge;
    }

    /** Mean rock erosion rate over the land in the last step, m/Myr (Earth today: ~50). */
    public double meanLandErosionMPerMyr() {
        return meanLandErosionMPerMyr;
    }

    /** Cumulative sediment budget, km3. */
    public Budget budget() {
        return new Budget(erodedKm3, landDepositKm3, marineDepositKm3, deltaPlainKm3, pelagicKm3, carbonateKm3);
    }

    /**
     * Cumulative volumes, km3. Eroded rock ends up on land (alluvium, lakes) or at sea; {@code deltaPlain} is
     * the part of the sea share that filled its sea and built land at the river mouth. Pelagic and
     * carbonate sediment come from the dissolved load and settling dust and are counted on their own.
     */
    public record Budget(double eroded, double landDeposit, double marineDeposit, double deltaPlain, double pelagic,
                         double carbonate) {
    }
}
