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
import java.util.concurrent.atomic.AtomicInteger;

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
    private final int[] apron;
    /** Marine spreading: river mouths of this step, in cell order. */
    private final int[] mouths;
    /** Marine spreading geometry, in cells: shelf radius squared, apron radius. */
    private final long shelf2;
    private final int apronRing;
    /** Marine spreading: walks the sea from a mouth on the calling thread, over the whole grid. */
    private final SeaWalk seaWalk;
    /** Marine spreading: walks made ahead on worker threads, one buffer per worker. */
    private WalkSlot[] slots = new WalkSlot[0];
    /** Furthest ring a walk made ahead may reach (its visited window). */
    private int aheadMaxRing;
    /**
     * Cells the workers may record per batch, all together (12 bytes each); past it a worker records only
     * the mouth and the calling thread walks on from there. Buffers larger than {@link #AHEAD_KEEP} cells
     * are let go after their batch.
     */
    private int aheadCells = 1 << 24;
    private static final int AHEAD_KEEP = 1 << 21;
    /** Cells recorded so far in this batch. */
    private final AtomicInteger aheadRecorded = new AtomicInteger();
    /** Walks made ahead that the calling thread carried on (for tests). */
    private long resumedWalks;
    /**
     * Shelf and delta-top levels this step. Spreading only adds sediment, so a sea cell at or above a level
     * when a batch begins stays there until the batch ends: walks made ahead flag such cells in place of
     * their distance ({@link #AT_SHELF}, {@link #AT_TOP}), and the spreading skips them without reading
     * their elevation. A flagged cell's distance is never needed: it takes nothing at shelf level.
     */
    private double shelfLevelNow;
    private double topLevelNow;
    private static final int AT_SHELF = -1;
    private static final int AT_TOP = -2;
    /** Mouths per batch: sized so a batch records about {@link #AHEAD_TARGET} cells. */
    private int aheadBatch = AHEAD_BATCH;
    private static final int AHEAD_TARGET = 1 << 23;
    private static final int AHEAD_MIN_BATCH = 256;
    /** Next band of columns for a worker to take. */
    private final AtomicInteger nextBand = new AtomicInteger();
    /** The batch's mouths by band ({@code bandOrder[bandStart[b]..bandStart[b + 1])}), in order within each. */
    private int[] bandStart = new int[0];
    private int[] bandFill = new int[0];
    private int[] bandOrder = new int[0];
    /**
     * Walk made ahead for each mouth of a batch: worker, first and end position in its buffer, last whole
     * ring, and the cells it had queued for the next ring.
     */
    private int[] aheadSlot = new int[0];
    private int[] aheadFrom = new int[0];
    private int[] aheadEnd = new int[0];
    private int[] aheadRing = new int[0];
    private int[] aheadQueueFrom = new int[0];
    private int[] aheadQueueEnd = new int[0];
    /** Marine spreading: the ring {@link #seaWalk} took last. */
    private final SeaWalk.Cells seaCells = new SeaWalk.Cells();
    /**
     * Cursor over the walk of the mouth being spread: cells {@code curPos..curEnd} of the walk made ahead
     * (all rings up to {@code curWalkedTo}, or the whole walk if that is {@code Integer.MAX_VALUE}), then,
     * once {@code live}, of the last ring {@link #seaWalk} took.
     */
    private int[] curCells;
    private int[] curRings;
    private int[] curDist2;
    private int[] curQueue;
    private int curQueueFrom;
    private int curQueueEnd;
    private int curStart;
    private int curMouth;
    private int curPos;
    private int curEnd;
    private int curWalkedTo;
    private boolean live;
    private final double[] pelagicScratch;
    private final double[] carbonateScratch;
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
        apron = new int[n];
        mouths = new int[n];
        double shelfCells = Math.max(1.0, cfg.shelfRadiusKm() / grid.cellKm());
        double apronCells = Math.max(shelfCells + 1.0, cfg.apronRadiusKm() / grid.cellKm());
        shelf2 = (long) (shelfCells * shelfCells);
        apronRing = (int) apronCells;
        seaWalk = new SeaWalk(grid.width(), grid.height(), flow.oceanMask(), -1);
        pelagicScratch = new double[n];
        carbonateScratch = new double[n];
        erosionMPerMyr = new float[n];
        depositM = new float[n];
        lakes = new byte[n];
    }

    /**
     * Worker threads for the per-cell loops. Routing, erosion, transport and marine spreading follow the
     * drainage order or share sediment between cells, so they stay on one thread; the workers walk the sea
     * from the river mouths ahead of marine spreading.
     */
    public void setParallel(Parallel parallel) {
        this.parallel = parallel;
        flow.setParallel(parallel);
        int threads = parallel.threads();
        // walks made ahead cover the apron search (3 apron radii) and one ring more; the cells they queue
        // lie one ring further out
        int radius = 3 * apronRing + 3;
        int window = SeaWalk.windowFits(grid.width(), radius) ? radius : -1;
        aheadMaxRing = window < 0 ? Integer.MAX_VALUE : radius - 2;
        slots = new WalkSlot[threads == 1 ? 0 : threads];
        for (int s = 0; s < slots.length; s++) {
            slots[s] = new WalkSlot(new SeaWalk(grid.width(), grid.height(), flow.oceanMask(), window));
        }
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
        spreadAllMarine(lith, sea, marineNow);
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
        double n = cfg.slopeExponent();
        // Math.pow(x, 1.0) is x by its specification, so skipping the call changes nothing
        boolean linear = n == 1.0;
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
            double len = flow.lengthM[idx];
            double c = f * dt * cfg.streamPowerK() * erodibility * Math.pow(q, cfg.dischargeExponent())
                    / (linear ? len : Math.pow(len, n));
            double drop = (h0 - base) - solveDrop(h0 - base, c, n);
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

    /** Most river mouths spread ahead together, then for real. */
    private static final int AHEAD_BATCH = 8_192;
    /** Bands of columns per worker thread (more bands balance the work, fewer see more neighbours). */
    private static final int AHEAD_BANDS = 4;

    /**
     * Spreads the sediment of every river mouth, in cell order. Which sea cells a mouth takes, and in what
     * order, depends only on the sea, never on what earlier mouths deposited; only where the walk stops
     * does. So worker threads spread a batch of mouths ahead of time, each worker the mouths of a band of
     * columns on its own copy of the sea floor, and record the walks they took; the mouths are then spread
     * for real one after another along those walks. A walk that turns out too short (a mouth in another
     * band filled the shelf it counted on) is carried on by the calling thread from where it ended: the
     * planet is the same on any number of threads.
     */
    private void spreadAllMarine(Lithosphere lith, double sea, double[] totals) {
        int n = grid.size();
        int count = 0;
        for (int idx = 0; idx < n; idx++) {
            if (marineInput[idx] > 0f) {
                mouths[count++] = idx;
            }
        }
        shelfLevelNow = sea - cfg.shelfDepthM();
        topLevelNow = sea - 1.0;
        if (slots.length == 0) {
            for (int m = 0; m < count; m++) {
                int mouth = mouths[m];
                curMouth = mouth;
                seaWalk.start(mouth);
                curPos = 0;
                curEnd = 0;
                live = true;
                spreadMarine(lith, mouth, marineInput[mouth], sea, totals);
            }
            return;
        }
        if (aheadSlot.length < AHEAD_BATCH) {
            bandStart = new int[grid.width() + 1];
            bandFill = new int[grid.width() + 1];
            bandOrder = new int[AHEAD_BATCH];
            aheadSlot = new int[AHEAD_BATCH];
            aheadFrom = new int[AHEAD_BATCH];
            aheadEnd = new int[AHEAD_BATCH];
            aheadRing = new int[AHEAD_BATCH];
            aheadQueueFrom = new int[AHEAD_BATCH];
            aheadQueueEnd = new int[AHEAD_BATCH];
        }
        for (int from = 0; from < count; ) {
            int to = Math.min(count, from + aheadBatch);
            spreadAhead(lith, sea, from, to);
            // size the next batch by what this one recorded (speed only: any batches give the same planet)
            long next = (long) (to - from) * AHEAD_TARGET / Math.max(1, aheadRecorded.get());
            aheadBatch = (int) Math.max(AHEAD_MIN_BATCH, Math.min(AHEAD_BATCH, next));
            for (int m = from; m < to; m++) {
                int k = m - from;
                SeaWalk.Cells cells = slots[aheadSlot[k]].cells;
                curMouth = mouths[m];
                curCells = cells.cells;
                curRings = cells.rings;
                curDist2 = cells.dist2;
                curQueue = slots[aheadSlot[k]].queue;
                curQueueFrom = aheadQueueFrom[k];
                curQueueEnd = aheadQueueEnd[k];
                curStart = aheadFrom[k];
                curPos = aheadFrom[k];
                curEnd = aheadEnd[k];
                curWalkedTo = aheadRing[k];
                live = false;
                spreadMarine(lith, curMouth, marineInput[curMouth], sea, totals);
            }
            from = to;
        }
    }

    /**
     * Spreads mouths {@code from}..{@code to} ahead on the worker threads. Mouths spread into each other's
     * sea within a few hundred km, in every direction, so the batch is cut into bands of columns: one
     * worker spreads all the mouths of a band in order, each seeing what the ones before it left.
     */
    private void spreadAhead(Lithosphere lith, double sea, int from, int to) {
        for (WalkSlot slot : slots) {
            slot.cells.len = 0;
            slot.queueLen = 0;
            slot.cells.shrink(AHEAD_KEEP);
        }
        int w = grid.width();
        int bands = Math.min(w, AHEAD_BANDS * slots.length);
        Arrays.fill(bandStart, 0, bands + 1, 0);
        for (int m = from; m < to; m++) {
            bandStart[(int) ((long) (mouths[m] % w) * bands / w) + 1]++;
        }
        for (int b = 0; b < bands; b++) {
            bandStart[b + 1] += bandStart[b];
        }
        System.arraycopy(bandStart, 0, bandFill, 0, bands);
        for (int m = from; m < to; m++) {
            bandOrder[bandFill[(int) ((long) (mouths[m] % w) * bands / w)]++] = m;
        }
        nextBand.set(0);
        aheadRecorded.set(0);
        parallel.range(slots.length, 1, (a, b) -> {
            for (int s = a; s < b; s++) {
                WalkSlot slot = slots[s];
                for (int band = nextBand.getAndIncrement(); band < bands; band = nextBand.getAndIncrement()) {
                    slot.clearFloor();
                    for (int q = bandStart[band]; q < bandStart[band + 1]; q++) {
                        int m = bandOrder[q];
                        slot.spreadAhead(lith, sea, s, mouths[m], m - from);
                    }
                }
            }
        });
    }

    /**
     * Builds shelf, apron and delta top from one river mouth; adds to {@code totals} (deposited, of it
     * above sea level). Sea cells are taken nearest first by straight-line distance from the mouth (reached
     * through the sea, edge to edge; {@link SeaWalk}), so shelves and deltas grow as round fans.
     */
    private void spreadMarine(Lithosphere lith, int mouth, double volume, double sea, double[] totals) {
        double shelfLevel = sea - cfg.shelfDepthM();
        int visited = 0;
        int apronCount = 0;
        // with no deep water in reach yet, keep looking further out before building delta top
        int reach = 3 * apronRing;
        double left = volume;
        // the cursor in locals; back in its fields only when more cells are needed
        int[] cells = curCells;
        int[] rings = curRings;
        int[] dist2 = curDist2;
        int pos = curPos;
        int end = curEnd;
        while (left > 0.0) {
            if (pos == end) {
                curPos = pos;
                if (!more(reach)) {
                    break;
                }
                cells = curCells;
                rings = curRings;
                dist2 = curDist2;
                pos = curPos;
                end = curEnd;
            }
            if (rings[pos] > reach) {
                break;
            }
            int c = cells[pos];
            int d2 = dist2[pos++];
            if (d2 < 0) {
                // flagged by the walk made ahead (see shelfLevelNow); full to the delta top: skipped there too
                queue[visited++] = d2 == AT_TOP ? ~c : c;
                continue;
            }
            queue[visited++] = c;
            if (d2 <= shelf2) {
                left -= fillTo(lith, c, shelfLevel, left);
            } else if (elevation[c] < shelfLevel) {
                apron[apronCount++] = c;
                reach = apronRing;
            }
        }
        if (left > 0.0 && apronCount > 0) {
            left = fillApron(lith, apronCount, shelfLevel, left);
        }
        // a sea already filled to shelf depth: the delta top builds to sea level, prograding across the
        // sea as far as it takes, then the delta plain rises above it
        double topLevel = sea - 1.0;
        for (int q = 0; q < visited && left > 0.0; q++) {
            int c = queue[q];
            if (c >= 0) {
                left -= fillTo(lith, c, topLevel, left);
            }
        }
        while (left > 0.0) {
            if (pos == end) {
                curPos = pos;
                if (!more(Integer.MAX_VALUE)) {
                    break;
                }
                cells = curCells;
                dist2 = curDist2;
                pos = curPos;
                end = curEnd;
            }
            int c = cells[pos];
            if (dist2[pos++] == AT_TOP) {
                queue[visited++] = ~c;
                continue;
            }
            queue[visited++] = c;
            left -= fillTo(lith, c, topLevel, left);
        }
        curPos = pos;
        if (left > 0.0) {
            double km = left / visited / grid.cellAreaKm2();
            for (int q = 0; q < visited; q++) {
                int c = queue[q];
                depositKm(lith, c < 0 ? ~c : c, km);
            }
            totals[1] += left;
        }
        totals[0] += volume;
    }

    /**
     * Puts the next ring of the walk under the cursor, if there is one and it is at most {@code maxRing};
     * the cursor must have used up the cells it had.
     */
    private boolean more(int maxRing) {
        if (!live) {
            // the next cell lies beyond the last whole ring walked ahead
            if (curWalkedTo == Integer.MAX_VALUE || maxRing <= curWalkedTo) {
                return false;
            }
            // that walk was too short: carry it on here
            seaWalk.resume(curMouth, curWalkedTo, curCells, curStart, curEnd, curQueue, curQueueFrom, curQueueEnd);
            live = true;
            resumedWalks++;
        }
        if (!seaWalk.hasNext() || seaWalk.upcomingRing() > maxRing) {
            return false;
        }
        seaCells.len = 0;
        seaWalk.takeRing(seaCells);
        curCells = seaCells.cells;
        curRings = seaCells.rings;
        curDist2 = seaCells.dist2;
        curPos = 0;
        curEnd = seaCells.len;
        return true;
    }

    /**
     * One worker of marine spreading ahead: its sea walker, the walks it recorded this batch, and the sea
     * floor as its spreading ahead left it in the band it is working on.
     */
    private final class WalkSlot {
        final SeaWalk walk;
        final SeaWalk.Cells cells = new SeaWalk.Cells();
        /** For each walk recorded, the cells it had queued for the next ring (to carry it on). */
        int[] queue = new int[1 << 12];
        int queueLen;
        /**
         * Elevation of the cells the spreading ahead changed in this band, computed exactly as
         * {@link SurfaceProcesses#spreadMarine} computes it; NaN elsewhere (as {@link SurfaceProcesses#elevation}).
         */
        final float[] floor = new float[grid.size()];
        int[] touched = new int[1 << 12];
        int touchedCount;
        int[] deep = new int[1 << 12];

        WalkSlot(SeaWalk walk) {
            this.walk = walk;
            Arrays.fill(floor, Float.NaN);
        }

        void clearFloor() {
            for (int t = 0; t < touchedCount; t++) {
                floor[touched[t]] = Float.NaN;
            }
            touchedCount = 0;
        }

        private float elevation(int c) {
            float e = floor[c];
            return e != e ? elevation[c] : e;
        }

        /**
         * Records the walk from {@code mouth} for mouth {@code k} of the batch, spreading its sediment on the
         * sea floor as this worker sees it.
         */
        void spreadAhead(Lithosphere lith, double sea, int s, int mouth, int k) {
            SeaWalk.Cells rec = cells;
            walk.start(mouth);
            int first = rec.len;
            aheadSlot[k] = s;
            aheadFrom[k] = first;
            if (aheadRecorded.get() < aheadCells) {
                spread(lith, sea, mouth, first);
                aheadRecorded.addAndGet(rec.len - first);
            } else {
                // this batch has recorded its share: the mouth alone, the calling thread walks on from there
                take(0);
            }
            aheadEnd[k] = rec.len;
            aheadQueueFrom[k] = queueLen;
            if (walk.hasNext()) {
                aheadRing[k] = rec.rings[rec.len - 1];
                int count = walk.queuedCount();
                if (queueLen + count > queue.length) {
                    queue = Arrays.copyOf(queue, Math.max(queueLen + count, 2 * queue.length));
                }
                System.arraycopy(walk.queued(), 0, queue, queueLen, count);
                queueLen += count;
            } else {
                aheadRing[k] = Integer.MAX_VALUE;
            }
            aheadQueueEnd[k] = queueLen;
        }

        /**
         * {@link SurfaceProcesses#spreadMarine} on the sea floor as this worker sees it, recording the walk
         * from cell {@code first} of {@link #cells}: every ring it took, plus one more.
         */
        private void spread(Lithosphere lith, double sea, int mouth, int first) {
            SeaWalk.Cells rec = cells;
            double shelfLevel = sea - cfg.shelfDepthM();
            int pos = first;
            int deepCount = 0;
            int reach = 3 * apronRing;
            double left = marineInput[mouth];
            while (left > 0.0 && (pos < rec.len || take(reach))) {
                if (rec.rings[pos] > reach) {
                    break;
                }
                int c = rec.cells[pos];
                int d2 = rec.dist2[pos++];
                if (d2 < 0) {
                    continue;
                }
                if (d2 <= shelf2) {
                    left -= fillTo(lith, c, shelfLevel, left);
                } else if (elevation(c) < shelfLevel) {
                    if (deepCount == deep.length) {
                        deep = Arrays.copyOf(deep, 2 * deepCount);
                    }
                    deep[deepCount++] = c;
                    reach = apronRing;
                }
            }
            if (left > 0.0 && deepCount > 0) {
                left = fillApron(lith, deepCount, shelfLevel, left);
            }
            double topLevel = sea - 1.0;
            for (int q = first; q < pos && left > 0.0; q++) {
                if (rec.dist2[q] != AT_TOP) {
                    left -= fillTo(lith, rec.cells[q], topLevel, left);
                }
            }
            while (left > 0.0 && (pos < rec.len || take(Integer.MAX_VALUE))) {
                if (rec.dist2[pos] != AT_TOP) {
                    left -= fillTo(lith, rec.cells[pos], topLevel, left);
                }
                pos++;
            }
            if (left > 0.0) {
                double km = left / (pos - first) / grid.cellAreaKm2();
                for (int q = first; q < pos; q++) {
                    depositKm(lith, rec.cells[q], km);
                }
            }
            take(Integer.MAX_VALUE);
        }

        /** Records the next ring if there is one, it is at most {@code maxRing} and stays in the window. */
        private boolean take(int maxRing) {
            if (!walk.hasNext()) {
                return false;
            }
            int r = walk.upcomingRing();
            if (r > maxRing || r > aheadMaxRing) {
                return false;
            }
            int from = cells.len;
            walk.takeRing(cells);
            // flag the cells at shelf or delta-top level when the batch began (see shelfLevelNow)
            int[] taken = cells.cells;
            int[] dist2 = cells.dist2;
            float[] e = elevation;
            double shelf = shelfLevelNow;
            double top = topLevelNow;
            for (int q = from; q < cells.len; q++) {
                float h = e[taken[q]];
                if (h >= shelf) {
                    dist2[q] = h >= top ? AT_TOP : AT_SHELF;
                }
            }
            return true;
        }

        private double fillApron(Lithosphere lith, int count, double level, double left) {
            int active = count;
            while (left > 0.0 && active > 0) {
                double share = left / active;
                int stillDeep = 0;
                for (int a = 0; a < active; a++) {
                    int c = deep[a];
                    double d = fillTo(lith, c, level, share);
                    left -= d;
                    if (d >= share) {
                        deep[stillDeep++] = c;
                    }
                }
                if (stillDeep == active) {
                    return 0.0;
                }
                active = stillDeep;
            }
            return Math.max(0.0, left);
        }

        private double fillTo(Lithosphere lith, int idx, double level, double available) {
            float e = elevation(idx);
            if (e >= level) {
                return 0.0;
            }
            double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
            double room = (level - e) / (fs * 1000.0) * grid.cellAreaKm2();
            double d = Math.min(available, room);
            depositKm(lith, idx, d / grid.cellAreaKm2());
            return d;
        }

        private void depositKm(Lithosphere lith, int idx, double km) {
            double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
            float e = floor[idx];
            if (e != e) {
                e = elevation[idx];
                if (touchedCount == touched.length) {
                    touched = Arrays.copyOf(touched, 2 * touchedCount);
                }
                touched[touchedCount++] = idx;
            }
            floor[idx] = e + (float) (fs * 1000.0 * km);
        }
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
        depositKm(lith, idx, d / grid.cellAreaKm2(), fs);
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
        depositKm(lith, idx, km, lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR);
    }

    /** {@code fs}: surface rise per unit of sediment on this crust. */
    private void depositKm(Lithosphere lith, int idx, double km, double fs) {
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

    /** Tests: cells the workers may record per batch. */
    void setAheadCells(int cells) {
        aheadCells = cells;
    }

    /** Tests: walks made ahead that the calling thread carried on, so far. */
    long resumedWalks() {
        return resumedWalks;
    }

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
