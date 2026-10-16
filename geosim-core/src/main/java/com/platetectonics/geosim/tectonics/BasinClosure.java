package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

import java.util.Arrays;

/**
 * Closes ocean basins trapped inside one plate (D-36). Rigid plates weld at first contact, so the ocean
 * left between colliding continents, fore-arc strips and back-arc basins end up enclosed by one plate's
 * continental crust, where no trench can ever reach them. On Earth the blocks keep converging until such
 * basins are gone. Here an ocean region of a plate that touches its continental crust but no other plate
 * closes from that rim at {@code enclosedOceanClosureKmPerMyr}: closed cells become continental crust as
 * thick as the continent beside them (at most the reference thickness) and keep their sediment.
 * <p>
 * A young rift ocean always touches the plate boundary at its ridge, so it is never closed.
 * <p>
 * Ocean regions are found as runs of one plate's ocean cells along rows, joined across rows and the date
 * line by union-find; the rows are read on the worker threads. Only enclosed regions are flooded. Closing
 * one region never changes whether another is enclosed: cells of two regions only touch where their plates
 * differ, which already makes both open.
 */
final class BasinClosure {
    /** Region flags: it touches another plate; it touches its own plate's continental crust. */
    private static final byte OPEN = 1;
    private static final byte RIMMED = 2;

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

    /** Finds every ocean region of every plate; closes enclosed ones by {@code front} cells, first cell first. */
    private static int visit(PlateSimulation sim, double front, double now) {
        Runs runs = sim.closureRuns;
        runs.label(sim);
        int count = runs.count;
        int[] begin = runs.begin;
        int[] len = runs.len;
        byte[] flags = runs.flags;
        int[] size = runs.size;
        byte[] regionFlags = runs.regionFlags;
        int[] first = runs.first;
        Arrays.fill(size, 0, count, 0);
        Arrays.fill(regionFlags, 0, count, (byte) 0);
        Arrays.fill(first, 0, count, -1);
        for (int k = 0; k < count; k++) {
            int root = runs.find(k);
            size[root] += len[k];
            regionFlags[root] |= flags[k];
            if (first[root] < 0) {
                first[root] = begin[k];
            }
        }
        int full = (int) Math.floor(front);
        double partial = front - full;
        float cap = (float) sim.cfg.referenceContinentKm();
        int enclosedTotal = 0;
        // regions in the order of their first cell
        for (int k = 0; k < count; k++) {
            int root = runs.find(k);
            if (first[root] != begin[k] || regionFlags[root] != RIMMED) {
                continue;
            }
            enclosedTotal += size[root];
            if (front > 0.0) {
                closeRegion(sim, begin[k], full, partial, cap, now);
            }
        }
        return enclosedTotal;
    }

    /** Closes the enclosed region whose first cell is {@code start} by {@code full + partial} cells. */
    private static void closeRegion(PlateSimulation sim, int start, int full, double partial, float cap, double now) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int[] region = sim.queue;
        int[] order = sim.closureOrder;
        int[] dist = sim.dist;
        boolean[] seen = sim.closureSeen;
        float[] fill = sim.closureFill;
        int owner = L.owner[start];
        int size = 0;
        region[size++] = start;
        seen[start] = true;
        for (int head = 0; head < size; head++) {
            int c = region[head];
            int i = c % w;
            int j = c / w;
            for (int k = 0; k < 4; k++) {
                int nb = neighbour(i, j, k, w, h);
                if (nb >= 0 && L.owner[nb] == owner && L.type[nb] == Lithosphere.OCEANIC && !seen[nb]) {
                    seen[nb] = true;
                    region[size++] = nb;
                }
            }
        }
        for (int r = 0; r < size; r++) {
            seen[region[r]] = false;
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

    /** 4-neighbour {@code k} (east, west, row - 1, row + 1) on the cylinder, or -1 past a pole. */
    private static int neighbour(int i, int j, int k, int w, int h) {
        return switch (k) {
            case 0 -> j * w + (i + 1 == w ? 0 : i + 1);
            case 1 -> j * w + (i == 0 ? w - 1 : i - 1);
            case 2 -> j > 0 ? (j - 1) * w + i : -1;
            default -> j < h - 1 ? (j + 1) * w + i : -1;
        };
    }

    /**
     * Runs of one plate's ocean cells along a row (no run crosses the date line), each with what its cells
     * touch, joined into regions by union-find. Scratch kept between steps.
     */
    static final class Runs {
        int count;
        /** First run of each row; {@code rowStart[h]} is the number of runs. */
        private int[] rowStart = new int[0];
        /** First cell and length of each run, in cell order. */
        int[] begin = new int[0];
        int[] len = new int[0];
        byte[] flags = new byte[0];
        private int[] parent = new int[0];
        /** Per region (indexed by its root run): cells, flags, first cell. */
        int[] size = new int[0];
        byte[] regionFlags = new byte[0];
        int[] first = new int[0];

        int find(int k) {
            int[] p = parent;
            while (p[k] != k) {
                p[k] = p[p[k]];
                k = p[k];
            }
            return k;
        }

        private void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra < rb) {
                parent[rb] = ra;
            } else if (rb < ra) {
                parent[ra] = rb;
            }
        }

        /** Finds the runs of the current crust, what they touch, and joins them into regions. */
        void label(PlateSimulation sim) {
            Lithosphere L = sim.lith;
            int w = sim.grid.width();
            int h = sim.grid.height();
            int[] owner = L.owner;
            byte[] type = L.type;
            if (rowStart.length < h + 1) {
                rowStart = new int[h + 1];
            }
            int[] rows = rowStart;
            // runs per row, then where each row's runs go
            sim.parallel.rows(h, (from, to) -> {
                for (int j = from; j < to; j++) {
                    int runs = 0;
                    int row = j * w;
                    for (int i = 0; i < w; i++) {
                        int idx = row + i;
                        if (type[idx] == Lithosphere.OCEANIC
                                && (i == 0 || type[idx - 1] != Lithosphere.OCEANIC || owner[idx - 1] != owner[idx])) {
                            runs++;
                        }
                    }
                    rows[j + 1] = runs;
                }
            });
            rows[0] = 0;
            for (int j = 0; j < h; j++) {
                rows[j + 1] += rows[j];
            }
            count = rows[h];
            if (begin.length < count) {
                int cap = Math.max(count, begin.length + begin.length / 2);
                begin = new int[cap];
                len = new int[cap];
                flags = new byte[cap];
                parent = new int[cap];
                size = new int[cap];
                regionFlags = new byte[cap];
                first = new int[cap];
            }
            int[] begin = this.begin;
            int[] len = this.len;
            byte[] flags = this.flags;
            int[] parent = this.parent;
            sim.parallel.rows(h, (from, to) -> {
                for (int j = from; j < to; j++) {
                    int k = rows[j];
                    int row = j * w;
                    int i = 0;
                    while (i < w) {
                        int idx = row + i;
                        if (type[idx] != Lithosphere.OCEANIC) {
                            i++;
                            continue;
                        }
                        int o = owner[idx];
                        int b = i;
                        byte f = 0;
                        while (i < w && type[row + i] == Lithosphere.OCEANIC && owner[row + i] == o) {
                            // the rows above and below
                            if (j > 0) {
                                int nb = row + i - w;
                                f |= owner[nb] != o ? OPEN : type[nb] != Lithosphere.OCEANIC ? RIMMED : 0;
                            }
                            if (j < h - 1) {
                                int nb = row + i + w;
                                f |= owner[nb] != o ? OPEN : type[nb] != Lithosphere.OCEANIC ? RIMMED : 0;
                            }
                            i++;
                        }
                        if (b > 0 || i < w) {
                            // the cells west and east of the run (a run of the whole row has none)
                            int west = row + (b == 0 ? w - 1 : b - 1);
                            int east = row + (i == w ? 0 : i);
                            f |= owner[west] != o ? OPEN : type[west] != Lithosphere.OCEANIC ? RIMMED : 0;
                            f |= owner[east] != o ? OPEN : type[east] != Lithosphere.OCEANIC ? RIMMED : 0;
                        }
                        begin[k] = row + b;
                        len[k] = i - b;
                        flags[k] = f;
                        parent[k] = k;
                        k++;
                    }
                }
            });
            for (int j = 0; j < h; j++) {
                int a = rows[j];
                int z = rows[j + 1];
                // across the date line
                if (z - a > 1) {
                    int lastEnd = begin[z - 1] + len[z - 1];
                    if (begin[a] == j * w && lastEnd == (j + 1) * w && owner[begin[a]] == owner[begin[z - 1]]) {
                        union(a, z - 1);
                    }
                }
                if (j == 0) {
                    continue;
                }
                // with the row above: runs of one plate that share a column
                int p = rows[j - 1];
                int pz = a;
                int q = a;
                while (p < pz && q < z) {
                    int pb = begin[p] - (j - 1) * w;
                    int pe = pb + len[p];
                    int qb = begin[q] - j * w;
                    int qe = qb + len[q];
                    if (pb < qe && qb < pe && owner[begin[p]] == owner[begin[q]]) {
                        union(p, q);
                    }
                    if (pe <= qe) {
                        p++;
                    } else {
                        q++;
                    }
                }
            }
        }
    }
}
