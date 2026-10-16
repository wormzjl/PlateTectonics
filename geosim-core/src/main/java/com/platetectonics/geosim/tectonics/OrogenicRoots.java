package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

/**
 * Dense roots under thickened crust, and their removal (docs/design/03-tectonics.md#plateaus-and-roots;
 * evidence in docs/research_notes/Tibetan plateau support).
 * <p>
 * Thick crust floats high, but what lies beneath it can pull it down. Two roots are tracked:
 * <ul>
 *   <li>keel: collision thickens the cold mantle lithosphere along with the crust. It is slightly denser
 *       than the asthenosphere and warms up (loses that excess) over {@code keelWarmingMyr};</li>
 *   <li>eclogite: mafic lower crust deeper than {@code eclogiteDepthKm} turns into eclogite, denser than
 *       the mantle. The reaction needs water: a dry lower crust (like India's under Tibet, ~30%
 *       converted) reacts over tens of Myr, a wet one within a few Myr. Wetness is a property of each
 *       crustal terrane, keyed by its formation time.</li>
 * </ul>
 * An attached root keeps a plateau 0.5-2 km lower than its crust alone would hold it. It does not
 * last: thick roots peel off and sink (delamination), a random event. The eclogite returns to the
 * mantle (a sink of continental crust), hot asthenosphere rises into the gap, the surface rebounds
 * 1-2.5 km, and post-collisional (potassic, adakitic) magmas erupt. The hot lithosphere then cools back
 * over {@code lithosphereRegrowthMyr}. This is why Tibet stands at ~5 km rather than sinking.
 */
final class OrogenicRoots {
    private OrogenicRoots() {
    }

    static void update(PlateSimulation sim, double dt) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        int n = sim.grid.size();
        double depth = cfg.eclogiteDepthKm();
        double kDry = 1.0 - Math.exp(-dt / cfg.eclogiteDryTimescaleMyr());
        double kWet = 1.0 - Math.exp(-dt / cfg.eclogiteWetTimescaleMyr());
        float warm = (float) Math.exp(-dt / cfg.keelWarmingMyr());
        float regrow = (float) Math.exp(-dt / cfg.lithosphereRegrowthMyr());
        float keelMax = (float) cfg.keelMaxKm();
        float crustMin = (float) cfg.delaminationCrustKm();
        double loadMin = cfg.delaminationLoadM();

        // each cell's roots change from its own column only
        sim.parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                if (L.type[idx] == Lithosphere.CONTINENTAL && L.thick[idx] > depth) {
                    double target = cfg.eclogiteMaxFraction() * (L.thick[idx] - depth);
                    if (L.eclogite[idx] < target) {
                        double k = isWet(sim, idx) ? kWet : kDry;
                        L.eclogite[idx] += (float) ((target - L.eclogite[idx]) * k);
                    }
                }
                float keel = L.keel[idx];
                if (keel > 0f) {
                    L.keel[idx] = Math.min(keel * warm, keelMax);
                } else if (keel < 0f) {
                    L.keel[idx] = keel * regrow;
                }
            }
        });
        // the weights add up in cell order
        long eligible = 0;
        double weight = 0.0;
        for (int idx = 0; idx < n; idx++) {
            double load = load(sim, idx);
            if (L.thick[idx] >= crustMin && load > loadMin) {
                eligible++;
                weight += load - loadMin;
            }
        }
        if (eligible == 0) {
            return;
        }

        double radiusCells = cfg.delaminationRadiusKm() / sim.grid.cellKm();
        double patchCells = Math.PI * radiusCells * radiusCells;
        double expected = cfg.delaminationRatePerMyr() * dt * eligible / Math.max(1.0, patchCells);
        int events = PlateReorganizer.poisson(expected, Rng.unit(sim.seed, Streams.DELAMINATION, sim.stepIndex, -1));
        for (int k = 0; k < events && weight > 0.0; k++) {
            int cell = pick(sim, weight * Rng.unit(sim.seed, Streams.DELAMINATION, sim.stepIndex, k));
            if (cell < 0) {
                break;
            }
            double removed = delaminate(sim, cell, radiusCells);
            weight -= removed;
            sim.events.add(GeoEvent.Type.DELAMINATION, sim.time, cell, L.owner[cell], -1, removed);
        }
    }

    /** How far the roots hold the surface down, m (cold keel plus eclogite; a hot keel counts as zero). */
    static double load(PlateSimulation sim, int idx) {
        Lithosphere L = sim.lith;
        return 1000.0 * (sim.cfg.keelDensityFactor() * Math.max(0f, L.keel[idx])
                + sim.cfg.eclogiteDensityFactor() * L.eclogite[idx]);
    }

    private static boolean isWet(PlateSimulation sim, int idx) {
        long terrane = Float.floatToIntBits(sim.lith.formedAt[idx]);
        return Rng.unit(sim.seed, Streams.ECLOGITE, terrane, 0) < sim.cfg.eclogiteWetChance();
    }

    /** Cell whose load crosses {@code target} on the running sum of (load - threshold) over eligible cells. */
    private static int pick(PlateSimulation sim, double target) {
        Lithosphere L = sim.lith;
        float crustMin = (float) sim.cfg.delaminationCrustKm();
        double loadMin = sim.cfg.delaminationLoadM();
        double sum = 0.0;
        int last = -1;
        for (int idx = 0; idx < sim.grid.size(); idx++) {
            if (L.thick[idx] < crustMin) {
                continue;
            }
            double load = load(sim, idx);
            if (load > loadMin) {
                sum += load - loadMin;
                last = idx;
                if (sum >= target) {
                    return idx;
                }
            }
        }
        return last;
    }

    /**
     * Drops the roots under the plate within {@code radiusCells}: eclogite leaves the crust, the keel is
     * replaced by hot asthenosphere. Returns the removed load above the eligibility threshold, m x cells.
     */
    private static double delaminate(PlateSimulation sim, int cell, double radiusCells) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int ci = cell % w;
        int cj = cell / w;
        int owner = L.owner[cell];
        double loadMin = sim.cfg.delaminationLoadM();
        float hot = (float) sim.cfg.hotKeelKm();
        int r = Math.max(1, (int) Math.round(radiusCells));
        double removed = 0.0;
        for (int dj = -r; dj <= r; dj++) {
            int j = cj + dj;
            if (j < 0 || j >= h) {
                continue;
            }
            for (int di = -r; di <= r; di++) {
                if (di * di + dj * dj > radiusCells * radiusCells) {
                    continue;
                }
                int idx = j * w + Math.floorMod(ci + di, w);
                if (L.owner[idx] != owner || (L.keel[idx] <= 0f && L.eclogite[idx] <= 0f)) {
                    continue;
                }
                removed += Math.max(0.0, load(sim, idx) - loadMin);
                L.thick[idx] -= L.eclogite[idx];
                L.sediment[idx] = Math.min(L.sediment[idx], L.thick[idx]);
                sim.budgetEclogite += L.eclogite[idx];
                L.eclogite[idx] = 0f;
                L.keel[idx] = Math.min(L.keel[idx], hot);
                L.marks[idx] |= Lithosphere.MARK_PLUME;
            }
        }
        return removed;
    }
}
