package com.platetectonics.geosim.tectonics;

import java.util.Map;

/**
 * Lateral flow of thickened continental crust (docs/design/03-tectonics.md#plateaus-and-roots; evidence
 * in docs/research_notes/Tibetan plateau support).
 * <p>
 * Hot, weak middle and lower crust under a thick orogen flows sideways instead of piling up forever, and
 * gravity limits how high a plateau can stand: Tibet's crust spreads east and its interior extends once
 * it is ~5 km high. Two rules:
 * <ul>
 *   <li>ceiling: a cell may not stand higher than {@code plateauElevationM} above sea level (by
 *       {@link Isostasy}, so a
 *       cell with an eclogite root holds more crust, and one over hot mantle less). Crust above that is
 *       cut and fills the nearest cells of the same plate up to their own ceiling, so a collision grows
 *       the plateau outward instead of upward. Continental cells are filled first; only a plate with no
 *       room left on its continents spills into its own ocean floor (which then becomes continental).
 *       Once a plate is no longer in a collision, its hot crust (over a thinned keel) collapses toward
 *       {@code collapseElevationM}; cold crust keeps its height and only erodes;</li>
 *   <li>slope: crust thicker than {@code flowOnsetKm} may not be more than {@code crustSlopeKmPerKm} x cell
 *       size thicker than a continental neighbour, which gives plateaus tapering margins a few hundred
 *       km wide.</li>
 * </ul>
 * Only buoyant crust moves; the mantle keel and eclogite stay where they are. Moving crust carries its
 * cell's share of sediment (the column thins as a whole), so sediment never outgrows the crust it is part
 * of. Crust and sediment volumes are conserved exactly (all cells have the same area).
 */
final class CrustalFlow {
    private static final float EPS = 0.01f;

    private CrustalFlow() {
    }

    static void relax(PlateSimulation sim) {
        spreadAboveCeiling(sim);
        relaxSlopes(sim);
    }

    // ------------------------------------------------------------------ ceiling

    private static void spreadAboveCeiling(PlateSimulation sim) {
        Lithosphere L = sim.lith;
        int n = sim.grid.size();
        int plates = sim.plates.size();
        float[] ceiling = ceilings(sim);
        double[] excess = new double[plates];
        double[] excessSediment = new double[plates];
        int[] firstSource = new int[plates];
        java.util.Arrays.fill(firstSource, -1);
        int sources = 0;
        for (int idx = 0; idx < n; idx++) {
            float cut = L.thick[idx] - ceiling[idx];
            if (cut > EPS && L.type[idx] == Lithosphere.CONTINENTAL) {
                int o = L.owner[idx];
                float sediment = cut * sedimentShare(L, idx);
                excess[o] += cut;
                excessSediment[o] += sediment;
                L.sediment[idx] -= sediment;
                L.thick[idx] = ceiling[idx];
                if (firstSource[o] < 0) {
                    firstSource[o] = idx;
                }
                sources++;
            }
        }
        if (sources == 0) {
            return;
        }
        fill(sim, ceiling, excess, excessSediment, true, false);
        fill(sim, ceiling, excess, excessSediment, false, false);
        // a small plate that is full to the ceiling everywhere pushes into its neighbours' crust
        fill(sim, ceiling, excess, excessSediment, true, true);
        fill(sim, ceiling, excess, excessSediment, false, true);
        // nothing left anywhere within reach: the excess stays at the first source cell
        for (int o = 0; o < plates; o++) {
            if (excess[o] > EPS && firstSource[o] >= 0) {
                L.thick[firstSource[o]] += (float) excess[o];
                L.sediment[firstSource[o]] += (float) excessSediment[o];
            }
        }
    }

    /** Sediment's share of a cell's crust (at most 1). */
    private static float sedimentShare(Lithosphere L, int idx) {
        return L.thick[idx] > 0f ? Math.min(1f, L.sediment[idx] / L.thick[idx]) : 0f;
    }

    /** Crust thickness each cell may hold: up to the plateau (or collapse) elevation, at most maxCrustKm. */
    private static float[] ceilings(PlateSimulation sim) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        boolean[] compressional = new boolean[sim.plates.size()];
        markCompressional(sim.stepCollisions, compressional, 0.0);
        markCompressional(sim.collisionMemory, compressional, 1.0);
        float[] ceiling = sim.ceilingScratch;
        float[] ceilingE = sim.ceilingElevationScratch;
        // each cell from its own column only
        sim.parallel.cells(L.n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                boolean collapse = L.keel[idx] < 0f && !compressional[L.owner[idx]];
                double e = sim.seaLevelM + (collapse ? cfg.collapseElevationM() : cfg.plateauElevationM());
                ceilingE[idx] = (float) e;
                ceiling[idx] = (float) Math.min(cfg.maxCrustKm(), Isostasy.thicknessForElevationKm(sim, idx, e));
            }
        });
        return ceiling;
    }

    private static void markCompressional(PairCounter pairs, boolean[] out, double min) {
        for (Map.Entry<Long, Double> e : pairs.sorted().entrySet()) {
            if (e.getValue() > min) {
                int a = PairCounter.first(e.getKey());
                int b = PairCounter.second(e.getKey());
                if (a < out.length) {
                    out[a] = true;
                }
                if (b < out.length) {
                    out[b] = true;
                }
            }
        }
    }

    /**
     * Breadth-first fill from every at-ceiling cell of a plate holding excess, nearest cells first. Each
     * visited cell remembers which plate's excess reached it, so with {@code anyPlate} the fill can cross
     * into neighbouring plates.
     */
    private static void fill(PlateSimulation sim, float[] ceiling, double[] excess, double[] excessSediment,
                             boolean continentalOnly, boolean anyPlate) {
        Lithosphere L = sim.lith;
        int n = sim.grid.size();
        int w = sim.grid.width();
        int h = sim.grid.height();
        float threshold = (float) sim.cfg.continentThresholdKm();
        int[] queue = sim.queue;
        int[] source = sim.dist; // plate whose excess reached the cell; shared scratch, restored to -1 on exit
        int tail = 0;
        for (int idx = 0; idx < n; idx++) {
            if (excess[L.owner[idx]] > EPS && L.thick[idx] >= ceiling[idx] - EPS) {
                queue[tail++] = idx;
                source[idx] = L.owner[idx];
            }
        }
        int head = 0;
        while (head < tail) {
            int idx = queue[head++];
            int o = source[idx];
            if (excess[o] <= EPS) {
                continue;
            }
            int i = idx % w;
            int j = idx / w;
            for (int m = 0; m < 4; m++) {
                int nb;
                if (m == 0) {
                    nb = j * w + (i + 1 == w ? 0 : i + 1);
                } else if (m == 1) {
                    nb = j * w + (i == 0 ? w - 1 : i - 1);
                } else if (m == 2) {
                    if (j == 0) {
                        continue;
                    }
                    nb = idx - w;
                } else {
                    if (j == h - 1) {
                        continue;
                    }
                    nb = idx + w;
                }
                if (source[nb] >= 0 || (!anyPlate && L.owner[nb] != o)
                        || (continentalOnly && L.type[nb] != Lithosphere.CONTINENTAL)) {
                    continue;
                }
                source[nb] = o;
                queue[tail++] = nb;
                float room = room(sim, nb, ceiling, excess[o] > EPS ? excessSediment[o] / excess[o] : 0.0);
                if (room > 0f && excess[o] > EPS) {
                    double give = Math.min(room, excess[o]);
                    double sediment = excessSediment[o] * give / excess[o];
                    L.thick[nb] += (float) give;
                    L.sediment[nb] += (float) sediment;
                    excess[o] -= give;
                    excessSediment[o] -= sediment;
                    if (L.type[nb] == Lithosphere.OCEANIC && L.thick[nb] >= threshold) {
                        L.type[nb] = Lithosphere.CONTINENTAL;
                        L.oceanAge[nb] = 0f;
                        L.formedAt[nb] = (float) sim.time;
                    }
                }
            }
        }
        for (int k = 0; k < tail; k++) {
            source[queue[k]] = -1;
        }
    }

    /**
     * Crust cell {@code nb} can take before it reaches its ceiling. A continental cell is filled up to its
     * ceiling elevation, allowing for the sediment share of the incoming crust (lighter, so less fits).
     */
    private static float room(PlateSimulation sim, int nb, float[] ceiling, double incomingShare) {
        Lithosphere L = sim.lith;
        if (L.type[nb] != Lithosphere.CONTINENTAL) {
            return ceiling[nb] - L.thick[nb];
        }
        double perKm = (Isostasy.AIRY_FACTOR + (Isostasy.SEDIMENT_LAND_FACTOR - Isostasy.AIRY_FACTOR) * incomingShare) * 1000.0;
        double rise = sim.ceilingElevationScratch[nb] - Isostasy.uncappedElevationM(sim, nb);
        return (float) Math.min(rise / perKm, sim.cfg.maxCrustKm() - L.thick[nb]);
    }

    // ------------------------------------------------------------------ slope

    private static void relaxSlopes(PlateSimulation sim) {
        Lithosphere L = sim.lith;
        int n = sim.grid.size();
        float onset = (float) sim.cfg.flowOnsetKm();
        float maxStep = (float) (sim.cfg.crustSlopeKmPerKm() * sim.grid.cellKm());
        int[] queue = sim.queue;
        int[] queued = sim.dist; // shared scratch, restored to -1 on exit
        int[] nb = new int[4];
        int[] nb2 = new int[4];

        int head = 0;
        int count = 0;
        for (int idx = 0; idx < n; idx++) {
            if (oversteep(L, sim, idx, onset, maxStep, nb)) {
                queue[count++] = idx;
                queued[idx] = 0;
            }
        }
        long budget = 32L * n;
        while (count > 0 && budget-- > 0) {
            int idx = queue[head];
            head = head + 1 == n ? 0 : head + 1;
            count--;
            queued[idx] = -1;

            float h0 = L.thick[idx];
            if (h0 <= onset) {
                continue;
            }
            int k = continentalNeighbours(sim, idx, nb);
            int steep = 0;
            for (int m = 0; m < k; m++) {
                if (h0 - L.thick[nb[m]] > maxStep + EPS) {
                    steep++;
                }
            }
            if (steep == 0) {
                continue;
            }
            float share = sedimentShare(L, idx);
            float moved = 0f;
            for (int m = 0; m < k; m++) {
                float d = h0 - L.thick[nb[m]] - maxStep;
                if (d > EPS) {
                    float give = d / (steep + 1);
                    L.thick[nb[m]] += give;
                    L.sediment[nb[m]] += give * share;
                    moved += give;
                }
            }
            L.thick[idx] = h0 - moved;
            L.sediment[idx] = Math.max(0f, L.sediment[idx] - moved * share);

            for (int m = 0; m < k; m++) {
                int j = nb[m];
                if (queued[j] < 0 && oversteep(L, sim, j, onset, maxStep, nb2)) {
                    queue[(head + count) % n] = j;
                    count++;
                    queued[j] = 0;
                }
            }
            if (queued[idx] < 0 && oversteep(L, sim, idx, onset, maxStep, nb2)) {
                queue[(head + count) % n] = idx;
                count++;
                queued[idx] = 0;
            }
        }
        sim.flowBudgetExhausted |= count > 0;
        for (int c = 0; c < count; c++) {
            queued[queue[(head + c) % n]] = -1;
        }
    }

    private static boolean oversteep(Lithosphere L, PlateSimulation sim, int idx, float onset, float maxStep, int[] nb) {
        if (L.type[idx] != Lithosphere.CONTINENTAL || L.thick[idx] <= onset) {
            return false;
        }
        int k = continentalNeighbours(sim, idx, nb);
        for (int m = 0; m < k; m++) {
            if (L.thick[idx] - L.thick[nb[m]] > maxStep + EPS) {
                return true;
            }
        }
        return false;
    }

    /** Continental neighbours on the same plate, written to {@code out}; returns how many. */
    private static int continentalNeighbours(PlateSimulation sim, int idx, int[] out) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int i = idx % w;
        int j = idx / w;
        int owner = L.owner[idx];
        int k = 0;
        k = add(L, out, k, j * w + (i + 1 == w ? 0 : i + 1), owner);
        k = add(L, out, k, j * w + (i == 0 ? w - 1 : i - 1), owner);
        if (j > 0) {
            k = add(L, out, k, idx - w, owner);
        }
        if (j < h - 1) {
            k = add(L, out, k, idx + w, owner);
        }
        return k;
    }

    private static int add(Lithosphere L, int[] out, int k, int idx, int owner) {
        if (L.owner[idx] == owner && L.type[idx] == Lithosphere.CONTINENTAL) {
            out[k++] = idx;
        }
        return k;
    }
}
