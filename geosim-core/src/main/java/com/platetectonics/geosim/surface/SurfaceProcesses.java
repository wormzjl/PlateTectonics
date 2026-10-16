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
 *   <li><b>Sea</b> (D-41): sediment reaching a river mouth builds a sloping shelf as wide as its supply
 *       (one cell on an active margin), sends a share past it to a turbidite fan under a continental slope,
 *       builds a delta top, then drapes the fan. Clay and dust settle everywhere; carbonate ooze above the
 *       compensation depth once calcareous plankton exist; reefs and platforms in clear, warm, shallow
 *       seas, fastest near the surface.</li>
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
    /** Marine spreading: river mouths of this step, in this step's scan order (see {@link #collectMouths}). */
    private final int[] mouths;
    /** Marine spreading, per mouth of the step: shelf radius and fan length (km), last ring of its walk. */
    private final double[] shelfKm;
    private final double[] fanKm;
    private final int[] reach;
    /** Marine spreading: cells within {@code activeMarginKm} of a trench or arc, this step. */
    private final boolean[] activeMargin;
    private final int[] marginScratch;
    private final float[] supplyScratch;
    /** Steps taken so far (the scan order of the mouths rotates with it). */
    private int steps;
    /** Marine spreading: walks the sea from a mouth on the calling thread, over the whole grid. */
    private final SeaWalk seaWalk;
    /** Marine spreading for real, on the calling thread. */
    private final Replay replay = new Replay();
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
    /**
     * Marine spreading so far by part (D-41), km3: shelf, fan, delta top, drape, shelf built on beyond the fan,
     * enclosed seas filled towards sea level, delta plain.
     */
    private final double[] marinePartsKm3 = new double[7];
    private int landFromSeaCells;

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
        mouths = new int[n];
        shelfKm = new double[n];
        fanKm = new double[n];
        reach = new int[n];
        activeMargin = new boolean[n];
        marginScratch = new int[n];
        supplyScratch = new float[n];
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
        // walks made ahead cover the furthest a mouth reaches (the widest shelf, or 3 of the longest fan
        // lengths) and one ring more; the cells they queue lie one ring further out
        double cellKm = grid.cellKm();
        int radius = (int) (Math.max(Math.max(cfg.shelfRadiusMaxKm(), cellKm), 3.0 * cfg.fanLengthMaxKm()) / cellKm) + 3;
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
        markActiveMargins(lith);
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
        steps++;
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
    /** e^(-x) for x = 0 .. 3 in steps of 1/1024: fan weights, the same bits on every machine. */
    private static final double[] DECAY = new double[3 * 1024 + 2];

    static {
        for (int k = 0; k < DECAY.length; k++) {
            DECAY[k] = StrictMath.exp(-k / 1024.0);
        }
    }

    /**
     * Marks the sea cells within {@code activeMarginKm} of a trench or arc this step (a square around each):
     * mouths there sit on an active margin.
     */
    private void markActiveMargins(Lithosphere lith) {
        int w = grid.width();
        int h = grid.height();
        int r = (int) Math.round(cfg.activeMarginKm() / grid.cellKm());
        int[] near = marginScratch;
        int mask = Lithosphere.MARK_TRENCH | Lithosphere.MARK_ARC;
        // along each row (across the date line): marked cells within r columns
        parallel.rows(h, (from, to) -> {
            for (int j = from; j < to; j++) {
                int row = j * w;
                if (2 * r + 1 >= w) {
                    int any = 0;
                    for (int i = 0; i < w; i++) {
                        any += (lith.marks(row + i) & mask) != 0 ? 1 : 0;
                    }
                    for (int i = 0; i < w; i++) {
                        near[row + i] = any;
                    }
                    continue;
                }
                int count = 0;
                for (int d = -r; d <= r; d++) {
                    count += (lith.marks(row + Math.floorMod(d, w)) & mask) != 0 ? 1 : 0;
                }
                for (int i = 0; i < w; i++) {
                    near[row + i] = count;
                    count -= (lith.marks(row + Math.floorMod(i - r, w)) & mask) != 0 ? 1 : 0;
                    count += (lith.marks(row + Math.floorMod(i + r + 1, w)) & mask) != 0 ? 1 : 0;
                }
            }
        });
        // down each column: rows within r of a row that has one
        parallel.range(w, 16, (from, to) -> {
            for (int i = from; i < to; i++) {
                int count = 0;
                for (int j = 0; j <= Math.min(r, h - 1); j++) {
                    count += near[j * w + i] > 0 ? 1 : 0;
                }
                for (int j = 0; j < h; j++) {
                    activeMargin[j * w + i] = count > 0;
                    if (j - r >= 0) {
                        count -= near[(j - r) * w + i] > 0 ? 1 : 0;
                    }
                    if (j + r + 1 < h) {
                        count += near[(j + r + 1) * w + i] > 0 ? 1 : 0;
                    }
                }
            }
        });
    }

    /**
     * River mouths of this step. The scan order rotates through rows down or up and columns east or west, one
     * per step, so no direction always comes first to a shared sea (neighbours stay together, which keeps the
     * walks made ahead in step).
     */
    private int collectMouths() {
        int w = grid.width();
        int h = grid.height();
        boolean up = (steps & 2) != 0;
        boolean west = (steps & 1) != 0;
        int count = 0;
        for (int jj = 0; jj < h; jj++) {
            int row = (up ? h - 1 - jj : jj) * w;
            for (int ii = 0; ii < w; ii++) {
                int idx = row + (west ? w - 1 - ii : ii);
                if (marineInput[idx] > 0f) {
                    mouths[count++] = idx;
                }
            }
        }
        return count;
    }

    /**
     * Shelf radius, fan length and walk reach of every mouth, from its supply relative to the step's median
     * and from its margin: a mouth near a trench or arc gets a shelf one cell wide.
     */
    private void mouthShapes(int count) {
        for (int m = 0; m < count; m++) {
            supplyScratch[m] = marineInput[mouths[m]];
        }
        Arrays.sort(supplyScratch, 0, count);
        double median = supplyScratch[count / 2];
        double cellKm = grid.cellKm();
        for (int m = 0; m < count; m++) {
            int mouth = mouths[m];
            double q = marineInput[mouth] / median;
            double r = activeMargin[mouth] ? cellKm
                    : Math.max(cfg.shelfRadiusMinKm(), Math.min(cfg.shelfRadiusMaxKm(),
                    cfg.shelfRadiusKm() * StrictMath.pow(q, cfg.shelfSupplyExponent())));
            double l = Math.max(cfg.fanLengthMinKm(), Math.min(cfg.fanLengthMaxKm(),
                    cfg.fanLengthKm() * StrictMath.pow(q, cfg.fanSupplyExponent())));
            shelfKm[m] = r;
            fanKm[m] = l;
            reach[m] = (int) (Math.max(r, 3.0 * l) / cellKm);
        }
    }

    /**
     * Spreads the sediment of every river mouth (D-41), one after another. Which sea cells a mouth takes, and
     * in what order, depends only on the sea, never on what earlier mouths deposited; only where the walk
     * stops does. So worker threads spread a batch of mouths ahead of time, each worker the mouths of a band
     * of columns on its own copy of the sea floor, and record the walks they took; the mouths are then spread
     * for real one after another along those walks. A walk that turns out too short (a mouth in another band
     * changed the sea it counted on) is carried on by the calling thread from where it ended: the planet is
     * the same on any number of threads.
     */
    private void spreadAllMarine(Lithosphere lith, double sea, double[] totals) {
        int count = collectMouths();
        if (count == 0) {
            return;
        }
        mouthShapes(count);
        if (slots.length == 0) {
            for (int m = 0; m < count; m++) {
                replay.startLive(mouths[m]);
                totals[1] += replay.spread(lith, m, sea);
                totals[0] += marineInput[mouths[m]];
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
                WalkSlot slot = slots[aheadSlot[k]];
                replay.startRecorded(mouths[m], slot.rec, aheadFrom[k], aheadEnd[k], aheadRing[k], slot.queue,
                        aheadQueueFrom[k], aheadQueueEnd[k]);
                totals[1] += replay.spread(lith, m, sea);
                totals[0] += marineInput[mouths[m]];
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
            slot.rec.len = 0;
            slot.queueLen = 0;
            slot.rec.shrink(AHEAD_KEEP);
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
                        slot.spreadAhead(lith, sea, s, m, m - from);
                    }
                }
            }
        });
    }

    /**
     * One mouth's marine spreading (D-41) along its walk from the mouth ({@link SeaWalk}): nearest cells first
     * by straight-line distance through the sea, so shelves, fans and deltas grow round. The calling thread
     * spreads for real ({@link Replay}); worker threads run the same code on their own copy of the sea floor
     * to learn how far each walk goes ({@link WalkSlot}).
     */
    private abstract class Spreader {
        /** The walk under the cursor: cells {@code pos..end} of these arrays. */
        int[] cells;
        int[] rings;
        int[] dist2;
        int pos;
        int end;
        /** This mouth's cells in walk order: all it visited, its delta top, its fan (with weight and cap). */
        int[] visit = new int[1 << 12];
        int[] delta = new int[1 << 8];
        int[] fan = new int[1 << 10];
        double[] fanWeight = new double[1 << 10];
        double[] fanCap = new double[1 << 10];
        /** Fan filling: each fan cell's room (km3) and surface rise per unit, and the cells not yet full. */
        double[] room = new double[1 << 10];
        double[] roomFs = new double[1 << 10];
        int[] active = new int[1 << 10];

        /** Puts more of the walk under the cursor, rings up to {@code maxRing}; false if there are none. */
        abstract boolean more(int maxRing);

        /** Sea floor elevation of cell {@code c}, m on the datum. */
        abstract float elevation(int c);

        /** Deposits {@code km} of sediment on {@code c}; {@code fs}: surface rise per unit on its crust. */
        abstract void deposit(Lithosphere lith, int c, double km, double fs);

        /** What one mouth's spreading put where, km3 (in the order of {@link #marinePartsKm3}). */
        void account(double shelf, double fan, double top, double drape, double prograde, double brim, double plain) {
        }

        /** What the two passes beyond the fan took. */
        private final double[] taken = new double[2];

        /**
         * Spreads mouth {@code m}'s sediment; returns the part that built a delta plain over a full sea (km3).
         * <ol>
         *   <li>Shelf: walking out to the mouth's reach, cells within the shelf radius R fill nearest first to a
         *       ramp, {@code rampTopM} deep at the mouth and the shelf break at R, with the shelf's share.
         *       Deeper cells beyond R, within 3 fan lengths L, are noted for the fan.</li>
         *   <li>Fan: the bypass share and what the shelf could not hold, shared by weight e^(-d/L), none built
         *       above a continental slope from the shelf break at R ({@code slopeMPerKm} down to
         *       {@code slopeFootM}) and a gentler rise beyond ({@code riseMPerKm}).</li>
         *   <li>Delta top: the shelf within {@code deltaRadiusKm} of the mouth (the mouth cell alone by default)
         *       builds to {@code deltaTopM} above sea level: land.</li>
         *   <li>Drape: the rest raises the fan's slope and rise by one height everywhere, never above the shelf
         *       break: the margin builds up and out as a whole rather than as a plateau at the shelf break.</li>
         *   <li>Beyond the fan: the shelf builds on at the shelf break, nearest first, as far as it takes. A sea
         *       that is full to the shelf break all over (an enclosed sea) fills on to 1 m below sea level, then a
         *       delta plain over all of it.</li>
         * </ol>
         */
        final double spread(Lithosphere lith, int m, double sea) {
            double volume = marineInput[mouths[m]];
            double cellKm = grid.cellKm();
            double rKm = shelfKm[m];
            double lKm = fanKm[m];
            int last = reach[m];
            double shelfCells = rKm / cellKm;
            double shelf2 = shelfCells * shelfCells;
            double deltaCells = cfg.deltaRadiusKm() / cellKm;
            double delta2 = deltaCells * deltaCells;
            double fanCells = 3.0 * lKm / cellKm;
            double fan2 = fanCells * fanCells;
            double shelfBreak = sea - cfg.shelfDepthM();
            double rampTop = sea - cfg.rampTopM();
            double rampPerKm = (cfg.shelfDepthM() - cfg.rampTopM()) / rKm;
            double slope = cfg.slopeMPerKm();
            double foot = cfg.slopeFootM();
            double rise = cfg.riseMPerKm();
            double footKm = foot / slope;
            double shelfLeft = volume * (1.0 - cfg.bypassFraction());
            int visited = 0;
            int deltaCount = 0;
            int fanCount = 0;
            int[] visit = this.visit;
            int[] cells = this.cells;
            int[] rings = this.rings;
            int[] dist2 = this.dist2;
            int pos = this.pos;
            int end = this.end;
            while (true) {
                if (pos == end) {
                    this.pos = pos;
                    if (!more(last)) {
                        break;
                    }
                    cells = this.cells;
                    rings = this.rings;
                    dist2 = this.dist2;
                    pos = this.pos;
                    end = this.end;
                }
                if (rings[pos] > last) {
                    break;
                }
                int c = cells[pos];
                int d2 = dist2[pos++];
                if (visited == visit.length) {
                    visit = this.visit = Arrays.copyOf(visit, 2 * visited);
                }
                visit[visited++] = c;
                if (d2 <= shelf2) {
                    if (d2 <= delta2) {
                        if (deltaCount == delta.length) {
                            delta = Arrays.copyOf(delta, 2 * deltaCount);
                        }
                        delta[deltaCount++] = c;
                    }
                    if (shelfLeft > 0.0) {
                        shelfLeft -= fillTo(lith, c, rampTop - rampPerKm * Math.sqrt(d2) * cellKm, shelfLeft);
                    }
                } else if (d2 <= fan2 && elevation(c) < shelfBreak) {
                    if (fanCount == fan.length) {
                        fan = Arrays.copyOf(fan, 2 * fanCount);
                        fanWeight = Arrays.copyOf(fanWeight, 2 * fanCount);
                        fanCap = Arrays.copyOf(fanCap, 2 * fanCount);
                    }
                    double dKm = Math.sqrt(d2) * cellKm;
                    fan[fanCount] = c;
                    fanWeight[fanCount] = DECAY[(int) (dKm / lKm * 1024.0 + 0.5)];
                    double x = dKm - rKm;
                    fanCap[fanCount] = shelfBreak - (x <= footKm ? slope * x : foot + rise * (x - footKm));
                    fanCount++;
                }
            }
            this.pos = pos;
            double shelfTaken = volume * (1.0 - cfg.bypassFraction()) - shelfLeft;
            double left = shelfLeft + volume * cfg.bypassFraction();
            double before = left;
            if (fanCount > 0) {
                left = fillFan(lith, fanCount, left);
            }
            double fanTaken = before - left;
            before = left;
            double top = sea + cfg.deltaTopM();
            for (int s = 0; s < deltaCount && left > 0.0; s++) {
                left -= fillTo(lith, delta[s], top, left);
            }
            double topTaken = before - left;
            before = left;
            if (left > 0.0 && fanCount > 0) {
                left = raiseFan(lith, fanCount, left, shelfBreak);
            }
            double drapeTaken = before - left;
            // beyond the fan: the shelf progrades at the shelf break, nearest first, as far as it takes; only a sea
            // that is that full all over (an enclosed sea) fills on to 1 m below sea level
            double[] taken = this.taken;
            for (int pass = 0; pass < 2; pass++) {
                before = left;
                double level = pass == 0 ? shelfBreak : sea - 1.0;
                for (int q = 0; q < visited && left > 0.0; q++) {
                    left -= fillTo(lith, visit[q], level, left);
                }
                cells = this.cells;
                pos = this.pos;
                end = this.end;
                while (left > 0.0) {
                    if (pos == end) {
                        this.pos = pos;
                        if (!more(Integer.MAX_VALUE)) {
                            break;
                        }
                        cells = this.cells;
                        pos = this.pos;
                        end = this.end;
                    }
                    int c = cells[pos++];
                    if (visited == visit.length) {
                        visit = this.visit = Arrays.copyOf(visit, 2 * visited);
                    }
                    visit[visited++] = c;
                    left -= fillTo(lith, c, level, left);
                }
                this.pos = pos;
                taken[pass] = before - left;
            }
            if (left <= 0.0) {
                account(shelfTaken, fanTaken, topTaken, drapeTaken, taken[0], taken[1], 0.0);
                return 0.0;
            }
            account(shelfTaken, fanTaken, topTaken, drapeTaken, taken[0], taken[1], left);
            double km = left / visited / grid.cellAreaKm2();
            for (int q = 0; q < visited; q++) {
                int c = visit[q];
                deposit(lith, c, km, fs(lith, c));
            }
            return left;
        }

        /**
         * Shares {@code left} among the fan cells by weight, none built above its cap on the slope and rise: a full
         * cell passes the rest of its share on to the others, until none is left over. Each cell then takes
         * min(w x, room) with one x for all (water filling). Returns the volume that did not fit, km3.
         */
        private double fillFan(Lithosphere lith, int count, double left) {
            if (room.length < count) {
                room = new double[fan.length];
                roomFs = new double[fan.length];
                active = new int[fan.length];
            }
            double area = grid.cellAreaKm2();
            double roomSum = 0.0;
            double weightSum = 0.0;
            for (int k = 0; k < count; k++) {
                int c = fan[k];
                double level = fanCap[k];
                float e = elevation(c);
                double fs = fs(lith, c);
                double r = e >= level ? 0.0 : (level - e) / (fs * 1000.0) * area;
                room[k] = r;
                roomFs[k] = fs;
                roomSum += r;
                weightSum += fanWeight[k];
                active[k] = k;
            }
            double x = Double.POSITIVE_INFINITY;
            if (roomSum > left) {
                // the cells too full for their share at this x take their room; the rest is shared again
                int n = count;
                double rest = left;
                double weight = weightSum;
                while (true) {
                    double share = rest / weight;
                    int still = 0;
                    double kept = 0.0;
                    for (int a = 0; a < n; a++) {
                        int k = active[a];
                        if (room[k] < fanWeight[k] * share) {
                            rest -= room[k];
                        } else {
                            active[still++] = k;
                            kept += fanWeight[k];
                        }
                    }
                    if (still == n || still == 0) {
                        x = still == n ? share : Double.POSITIVE_INFINITY;
                        break;
                    }
                    n = still;
                    weight = kept;
                }
            }
            for (int k = 0; k < count && left > 0.0; k++) {
                double d = Math.min(left, Math.min(room[k], fanWeight[k] * x));
                if (d > 0.0) {
                    deposit(lith, fan[k], d / area, roomFs[k]);
                    left -= d;
                }
            }
            return Math.max(0.0, left);
        }

        /**
         * Raises the fan's slope and rise by the height h that holds {@code left}: each fan cell fills to its cap
         * plus h, but never above the shelf break. The volume held grows with h piecewise linearly; h is found by
         * regula falsi (Illinois) to within a millimetre, and the last cells take what is left. Returns the volume
         * that did not fit (the fan is full to the shelf break), km3.
         */
        private double raiseFan(Lithosphere lith, int count, double left, double shelfBreak) {
            double area = grid.cellAreaKm2();
            double full = 0.0;
            double hi = 0.0;
            for (int k = 0; k < count; k++) {
                int c = fan[k];
                double fs = fs(lith, c);
                double e = elevation(c);
                room[k] = e;
                roomFs[k] = fs;
                full += Math.max(0.0, shelfBreak - e) / (fs * 1000.0) * area;
                hi = Math.max(hi, shelfBreak - fanCap[k]);
            }
            if (full > left) {
                double lo = 0.0;
                double vLo = raised(count, 0.0, shelfBreak);
                double vHi = full;
                int kept = 0;
                for (int it = 0; it < 100 && hi - lo > 1e-3; it++) {
                    double h = lo + (left - vLo) / (vHi - vLo) * (hi - lo);
                    double v = raised(count, h, shelfBreak);
                    if (v < left) {
                        lo = h;
                        vLo = v;
                        if (kept < 0) {
                            vHi = left + (vHi - left) / 2.0;
                        }
                        kept = -1;
                    } else {
                        hi = h;
                        vHi = v;
                        if (kept > 0) {
                            vLo = left - (left - vLo) / 2.0;
                        }
                        kept = 1;
                    }
                }
            }
            for (int k = 0; k < count && left > 0.0; k++) {
                double level = Math.min(shelfBreak, fanCap[k] + hi);
                double d = Math.min(left, Math.max(0.0, level - room[k]) / (roomFs[k] * 1000.0) * area);
                if (d > 0.0) {
                    deposit(lith, fan[k], d / area, roomFs[k]);
                    left -= d;
                }
            }
            return Math.max(0.0, left);
        }

        /** Volume the fan holds with its slope and rise raised by {@code h} (see {@link #raiseFan}), km3. */
        private double raised(int count, double h, double shelfBreak) {
            double sum = 0.0;
            for (int k = 0; k < count; k++) {
                double level = Math.min(shelfBreak, fanCap[k] + h);
                if (level > room[k]) {
                    sum += (level - room[k]) / (roomFs[k] * 1000.0);
                }
            }
            return sum * grid.cellAreaKm2();
        }

        /** Fills cell {@code c} up to {@code level} with at most {@code available} km3; returns what it took. */
        final double fillTo(Lithosphere lith, int c, double level, double available) {
            float e = elevation(c);
            if (e >= level) {
                return 0.0;
            }
            double fs = fs(lith, c);
            double room = (level - e) / (fs * 1000.0) * grid.cellAreaKm2();
            double d = Math.min(available, room);
            deposit(lith, c, d / grid.cellAreaKm2(), fs);
            return d;
        }
    }

    private static double fs(Lithosphere lith, int c) {
        return lith.isContinental(c) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
    }

    /** Marine spreading for real on the calling thread, along a walk made ahead or one it walks itself. */
    private final class Replay extends Spreader {
        /** The walk made ahead: first cell in the arrays, last whole ring, and the cells it had queued. */
        private int start;
        private int walkedTo;
        private int[] queued;
        private int queuedFrom;
        private int queuedTo;
        private int mouth;
        /** Walking itself ({@link #seaWalk}) rather than reading the walk made ahead. */
        private boolean live;

        void startLive(int mouth) {
            this.mouth = mouth;
            seaWalk.start(mouth);
            live = true;
            pos = 0;
            end = 0;
        }

        void startRecorded(int mouth, SeaWalk.Cells rec, int from, int to, int walkedTo, int[] queued, int queuedFrom,
                           int queuedTo) {
            this.mouth = mouth;
            cells = rec.cells;
            rings = rec.rings;
            dist2 = rec.dist2;
            start = from;
            pos = from;
            end = to;
            this.walkedTo = walkedTo;
            this.queued = queued;
            this.queuedFrom = queuedFrom;
            this.queuedTo = queuedTo;
            live = false;
        }

        @Override
        boolean more(int maxRing) {
            if (!live) {
                // the next cell lies beyond the last whole ring walked ahead
                if (walkedTo == Integer.MAX_VALUE || maxRing <= walkedTo) {
                    return false;
                }
                // that walk was too short: carry it on here
                seaWalk.resume(mouth, walkedTo, cells, start, end, queued, queuedFrom, queuedTo);
                live = true;
                resumedWalks++;
            }
            if (!seaWalk.hasNext() || seaWalk.upcomingRing() > maxRing) {
                return false;
            }
            seaCells.len = 0;
            seaWalk.takeRing(seaCells);
            cells = seaCells.cells;
            rings = seaCells.rings;
            dist2 = seaCells.dist2;
            pos = 0;
            end = seaCells.len;
            return true;
        }

        @Override
        float elevation(int c) {
            return elevation[c];
        }

        @Override
        void account(double shelf, double fan, double top, double drape, double prograde, double brim, double plain) {
            double[] parts = marinePartsKm3;
            parts[0] += shelf;
            parts[1] += fan;
            parts[2] += top;
            parts[3] += drape;
            parts[4] += prograde;
            parts[5] += brim;
            parts[6] += plain;
        }

        @Override
        void deposit(Lithosphere lith, int c, double km, double fs) {
            depositKm(lith, c, km, fs);
        }
    }

    /**
     * One worker of marine spreading ahead: its sea walker, the walks it recorded this batch, and the sea
     * floor as its spreading ahead left it in the band it is working on.
     */
    private final class WalkSlot extends Spreader {
        final SeaWalk walk;
        final SeaWalk.Cells rec = new SeaWalk.Cells();
        /** For each walk recorded, the cells it had queued for the next ring (to carry it on). */
        int[] queue = new int[1 << 12];
        int queueLen;
        /**
         * Elevation of the cells the spreading ahead changed in this band, computed exactly as the spreading
         * for real computes it; NaN elsewhere (as {@link SurfaceProcesses#elevation}).
         */
        final float[] floor = new float[grid.size()];
        int[] touched = new int[1 << 12];
        int touchedCount;

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

        /**
         * Records the walk from mouth {@code m} (number {@code k} of the batch), spreading its sediment on the
         * sea floor as this worker sees it: every ring the spreading took, plus one more.
         */
        void spreadAhead(Lithosphere lith, double sea, int s, int m, int k) {
            walk.start(mouths[m]);
            int first = rec.len;
            aheadSlot[k] = s;
            aheadFrom[k] = first;
            cells = rec.cells;
            rings = rec.rings;
            dist2 = rec.dist2;
            pos = first;
            end = first;
            if (aheadRecorded.get() < aheadCells) {
                spread(lith, m, sea);
                take(Integer.MAX_VALUE);
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

        @Override
        boolean more(int maxRing) {
            if (!take(maxRing)) {
                return false;
            }
            cells = rec.cells;
            rings = rec.rings;
            dist2 = rec.dist2;
            end = rec.len;
            return true;
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
            walk.takeRing(rec);
            return true;
        }

        @Override
        float elevation(int c) {
            float e = floor[c];
            return e != e ? elevation[c] : e;
        }

        @Override
        void deposit(Lithosphere lith, int c, double km, double fs) {
            float e = floor[c];
            if (e != e) {
                e = elevation[c];
                if (touchedCount == touched.length) {
                    touched = Arrays.copyOf(touched, 2 * touchedCount);
                }
                touched[touchedCount++] = c;
            }
            floor[c] = e + (float) (fs * 1000.0 * km);
        }
    }

    /**
     * Sediment on ocean floor stays oceanic (a passive-margin wedge on transitional crust): it rides to the
     * trench, where part is scraped into the overriding plate and the rest subducts. Turning thick piles
     * into continental crust would make unsinkable crust out of sediment alone.
     */
    private void deposit(Lithosphere lith, int idx, double km3) {
        depositKm(lith, idx, km3 / grid.cellAreaKm2(), fs(lith, idx));
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
        double factory = g.has(Milestone.ANIMALS) ? 1.0
                : g.has(Milestone.LIFE) ? cfg.carbonateMicrobialFactor() : cfg.carbonateAbioticFactor();
        double z0 = cfg.carbonateDecayM();
        double surfaceRate = cfg.carbonateMPerMyr() * factory;
        double muddy = cfg.carbonateClasticLimitMPerMyr() * dt;
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
                // river sediment this cell received this step (marine spreading only, so far)
                double clastic = depositM[idx];
                double fs = lith.isContinental(idx) ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
                double clay = cfg.pelagicMPerMyr() + (ooze && depth < cfg.ccdDepthM() ? cfg.oozeMPerMyr() : 0.0);
                // settling mud never builds land: it stops a few metres below the surface
                double km = Math.min(clay * dt / 1000.0, (depth - 5.0) / (fs * 1000.0));
                pelagicScratch[idx] = km * area;
                deposit(lith, idx, km * area);
                depth = sea - elevation[idx];
                if (depth > 5.0 && depth < cfg.carbonateDepthM() && climate.temperatureC[idx] >= cfg.carbonateMinTempC()
                        && clastic <= muddy) {
                    // growth falls off with depth as light does, and the surface rises as it grows:
                    // dz/dt = -fs r0 e^(-z/z0), so u = e^(z/z0) falls by fs r0 dt / z0; it stops 5 m below sea level
                    double u = Math.exp(depth / z0) - fs * surfaceRate * dt / z0;
                    double end = u <= Math.exp(5.0 / z0) ? 5.0 : z0 * Math.log(u);
                    double grow = (depth - end) / (fs * 1000.0);
                    if (grow > 0.0) {
                        carbonateScratch[idx] = grow * area;
                        deposit(lith, idx, grow * area);
                    }
                }
            }
        });
        int built = 0;
        for (int idx = 0; idx < grid.size(); idx++) {
            pelagicKm3 += pelagicScratch[idx];
            carbonateKm3 += carbonateScratch[idx];
            // settling stops below sea level: sea cells now above it were built by deltas
            if (flow.isOcean(idx) && elevation[idx] > sea) {
                built++;
            }
        }
        landFromSeaCells = built;
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

    /** Sea cells that the last step's marine sediment built above sea level. */
    public int landFromSeaCells() {
        return landFromSeaCells;
    }

    /** Mean rock erosion rate over the land in the last step, m/Myr (Earth today: ~50). */
    public double meanLandErosionMPerMyr() {
        return meanLandErosionMPerMyr;
    }

    /** Cumulative sediment budget, km3. */
    /** Marine spreading so far by part (D-41), km3, in the order of {@link #marinePartsKm3}. */
    public double[] marinePartsKm3() {
        return marinePartsKm3.clone();
    }

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
