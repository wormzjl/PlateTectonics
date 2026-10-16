package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

/**
 * Closes ocean basins trapped inside one plate (D-36). Rigid plates weld at first contact, so the ocean
 * left between colliding continents, fore-arc strips and back-arc basins end up enclosed by one plate's
 * continental crust, where no trench can ever reach them. On Earth the blocks keep converging until such
 * basins are gone. Here an ocean region of a plate that touches its continental crust but no other plate
 * closes from that rim at {@code enclosedOceanClosureKmPerMyr}: closed cells become continental crust as
 * thick as the continent beside them (at most the reference thickness) and keep their sediment.
 * <p>
 * A young rift ocean always touches the plate boundary at its ridge, so it is never closed.
 */
final class BasinClosure {
    private BasinClosure() {
    }

    /** Closes the rims of enclosed basins for one step of {@code dt} ending at {@code now}. */
    static void close(PlateSimulation sim, double dt, double now) {
        double front = sim.cfg.enclosedOceanClosureKmPerMyr() * dt / sim.grid.cellKm();
        if (front > 0.0) {
            visit(sim, front, now);
        }
    }

    /** Oceanic cells in enclosed basins right now. */
    static int enclosedCells(PlateSimulation sim) {
        return visit(sim, 0.0, sim.time);
    }

    /** Floods every ocean region of every plate; closes enclosed ones by {@code front} cells. */
    private static int visit(PlateSimulation sim, double front, double now) {
        Lithosphere L = sim.lith;
        int n = L.n;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int[] region = sim.queue;
        int[] order = sim.closureOrder;
        int[] dist = sim.dist;
        boolean[] seen = sim.closureSeen;
        float[] fill = sim.closureFill;
        java.util.Arrays.fill(seen, false);
        int full = (int) Math.floor(front);
        double partial = front - full;
        float cap = (float) sim.cfg.referenceContinentKm();
        int enclosedTotal = 0;
        for (int start = 0; start < n; start++) {
            if (seen[start] || L.type[start] != Lithosphere.OCEANIC) {
                continue;
            }
            int owner = L.owner[start];
            int size = 0;
            region[size++] = start;
            seen[start] = true;
            boolean enclosed = true;
            boolean rimmed = false;
            for (int head = 0; head < size; head++) {
                int c = region[head];
                int i = c % w;
                int j = c / w;
                for (int k = 0; k < 4; k++) {
                    int nb = neighbour(i, j, k, w, h);
                    if (nb < 0) {
                        continue;
                    }
                    if (L.owner[nb] != owner) {
                        enclosed = false;
                    } else if (L.type[nb] != Lithosphere.OCEANIC) {
                        rimmed = true;
                    } else if (!seen[nb]) {
                        seen[nb] = true;
                        region[size++] = nb;
                    }
                }
            }
            if (!enclosed || !rimmed) {
                continue;
            }
            enclosedTotal += size;
            if (front <= 0.0) {
                continue;
            }
            // distance from the continental rim (rim = 1); each cell takes the crust of whatever reached it first
            int tail = 0;
            for (int r = 0; r < size; r++) {
                int c = region[r];
                int i = c % w;
                int j = c / w;
                double sum = 0.0;
                int count = 0;
                for (int k = 0; k < 4; k++) {
                    int nb = neighbour(i, j, k, w, h);
                    if (nb >= 0 && L.type[nb] != Lithosphere.OCEANIC) {
                        sum += L.thick[nb];
                        count++;
                    }
                }
                if (count > 0) {
                    dist[c] = 1;
                    fill[c] = Math.min(cap, (float) (sum / count));
                    order[tail++] = c;
                }
            }
            for (int head = 0; head < tail; head++) {
                int c = order[head];
                if (dist[c] > full) {
                    continue;
                }
                int i = c % w;
                int j = c / w;
                for (int k = 0; k < 4; k++) {
                    int nb = neighbour(i, j, k, w, h);
                    if (nb >= 0 && dist[nb] < 0 && L.type[nb] == Lithosphere.OCEANIC) {
                        dist[nb] = dist[c] + 1;
                        fill[nb] = fill[c];
                        order[tail++] = nb;
                    }
                }
            }
            for (int q = 0; q < tail; q++) {
                int c = order[q];
                if (dist[c] <= full
                        || (dist[c] == full + 1 && Rng.unit(sim.seed, Streams.BASIN_CLOSURE, sim.stepIndex, c) < partial)) {
                    float before = L.thick[c];
                    L.thick[c] = Math.max(before, fill[c]);
                    sim.budgetClosure += L.thick[c] - before;
                    L.type[c] = Lithosphere.CONTINENTAL;
                    L.oceanAge[c] = 0f;
                    L.formedAt[c] = (float) now;
                }
            }
            for (int q = 0; q < tail; q++) {
                dist[order[q]] = -1;
            }
        }
        return enclosedTotal;
    }

    /** 4-neighbour {@code k} (east, west, row - 1, row + 1) on the cylinder, or -1 past a pole. */
    private static int neighbour(int i, int j, int k, int w, int h) {
        return switch (k) {
            case 0 -> j * w + (i + 1 == w ? 0 : i + 1);
            case 1 -> j * w + (i == 0 ? w - 1 : i - 1);
            case 2 -> j > 0 ? (j - 1) * w + i : -1;
            default -> j < h - 1 ? (j + 1) * w + i : -1;
        };
    }
}
