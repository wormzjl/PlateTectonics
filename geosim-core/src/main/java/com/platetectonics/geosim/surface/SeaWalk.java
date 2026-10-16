package com.platetectonics.geosim.surface;

import java.util.Arrays;

/**
 * The order in which marine spreading takes sea cells from a river mouth (D-40): nearest first by
 * straight-line distance, in rings one cell wide, first come first served within a ring, reached through
 * the sea edge to edge (east, west, row − 1, row + 1). A cell that lies inside the current ring because
 * the way to it led round a cape joins the current ring.
 * <p>
 * A neighbour lies at most one ring further out than the cell that queues it, so two buffers hold the
 * queue: this ring and the next. The order depends only on the sea mask, never on what is deposited, so a
 * walk can be made ahead of time on another thread and gives the same cells in the same order.
 * <p>
 * Cells come out a whole ring at a time, with the queue in local variables: walks on several threads then
 * share no field that changes per cell. The queue holds (row, column) pairs, so no cell index is divided.
 * Visited cells are remembered either in a window around the mouth (small and cache-friendly) or over the
 * whole grid. A cell of ring r lies less than r + 1 cells from the mouth and its neighbours at most r + 1,
 * so a window walk may take rings up to {@code radius − 1}; it throws rather than go further.
 * <p>
 * Grids up to 32,768 cells on a side: on an Earth-sized cylinder, cells down to ~1.2 km. Marine spreading
 * always builds one whole-grid walker, so this bounds the master grid on any thread count.
 */
final class SeaWalk {
    private final int w;
    private final int h;
    private final boolean[] sea;
    /** Half-width of the visited window, cells; −1: the whole grid. */
    private final int radius;
    private final int side;
    /** Visited marks: the walk's stamp, 1..255 (one byte: a window's marks fit the L1 cache). */
    private final byte[] seen;
    private int stamp;
    /** Queued cells as {@code row << 16 | column}: this ring and the next. */
    private int[] now;
    private int[] next;
    private int head;
    private int tail;
    private int nextTail;
    private int ring;
    private int mi;
    private int mj;

    /**
     * @param sea    sea cells (shared, read only while walks run)
     * @param radius visited window half-width in cells, or −1 for the whole grid
     */
    SeaWalk(int width, int height, boolean[] sea, int radius) {
        if (width > 1 << 15 || height > 1 << 15) {
            throw new IllegalArgumentException("grid too large for packed cells: " + width + " x " + height);
        }
        this.w = width;
        this.h = height;
        this.sea = sea;
        this.radius = radius;
        this.side = 2 * radius + 1;
        int cells = radius < 0 ? width * height : side * side;
        seen = new byte[cells];
        // one spare slot: every neighbour is written to both queues before the counts decide
        now = new int[cells + 1];
        next = new int[cells + 1];
    }

    /** True if a window of this half-width fits the grid without one cell appearing twice. */
    static boolean windowFits(int width, int radius) {
        return 2 * radius + 1 <= width;
    }

    void start(int mouth) {
        if (++stamp == 256) {
            Arrays.fill(seen, (byte) 0);
            stamp = 1;
        }
        mi = mouth % w;
        mj = mouth / w;
        ring = 0;
        head = 0;
        tail = 0;
        nextTail = 0;
        seen[radius < 0 ? mouth : radius * side + radius] = (byte) stamp;
        now[tail++] = mj << 16 | mi;
    }

    /**
     * Carries on a walk another walker made from {@code mouth}: {@code taken[takenFrom..takenTo)} are all
     * the cells it took, every ring up to {@code ring}, and {@code queued[queuedFrom..queuedTo)} the cells
     * it had queued for the next ring ({@link #queued()}). Between them they are every cell that walk
     * reached, so the visited marks follow. Whole-grid walkers only.
     */
    void resume(int mouth, int ring, int[] taken, int takenFrom, int takenTo, int[] queued, int queuedFrom,
                int queuedTo) {
        if (radius >= 0) {
            throw new IllegalStateException("only a whole-grid walk can carry on another walk");
        }
        if (++stamp == 256) {
            Arrays.fill(seen, (byte) 0);
            stamp = 1;
        }
        mi = mouth % w;
        mj = mouth / w;
        this.ring = ring;
        head = 0;
        tail = 0;
        byte mark = (byte) stamp;
        for (int q = takenFrom; q < takenTo; q++) {
            seen[taken[q]] = mark;
        }
        nextTail = 0;
        for (int q = queuedFrom; q < queuedTo; q++) {
            int p = queued[q];
            seen[(p >>> 16) * w + (p & 0xFFFF)] = mark;
            next[nextTail++] = p;
        }
    }

    /**
     * The cells queued for the next ring (packed as {@code row << 16 | column}), the first
     * {@link #queuedCount()} of them; valid right after {@link #takeRing}, until the next call.
     */
    int[] queued() {
        return next;
    }

    int queuedCount() {
        return nextTail;
    }

    boolean hasNext() {
        return head < tail || nextTail > 0;
    }

    /** Ring of the next cell; only while {@link #hasNext()}. */
    int upcomingRing() {
        return head < tail ? ring : ring + 1;
    }

    /**
     * Takes the cells of the next ring, in order, onto the end of {@code out} with their squared distance
     * from the mouth, queueing their sea neighbours not yet queued; only while {@link #hasNext()}.
     */
    void takeRing(Cells out) {
        if (head == tail) {
            int[] t = now;
            now = next;
            next = t;
            head = 0;
            tail = nextTail;
            nextTail = 0;
            ring++;
        }
        if (radius >= 0 && ring >= radius) {
            throw new IllegalStateException("ring " + ring + " leaves the visited window of half-width " + radius);
        }
        int[] now = this.now;
        int[] next = this.next;
        byte[] seen = this.seen;
        boolean[] sea = this.sea;
        int head = this.head;
        int tail = this.tail;
        int nextTail = this.nextTail;
        byte stamp = (byte) this.stamp;
        int w = this.w;
        int half = w / 2;
        int mi = this.mi;
        int mj = this.mj;
        int radius = this.radius;
        int side = this.side;
        boolean window = radius >= 0;
        // (ring + 1)²: cells closer than this (squared, in cells) belong to this ring
        long limit = (long) (ring + 1) * (ring + 1);
        int start = out.len;
        int len = start;
        out.reserve(len + (tail - head));
        int[] cells = out.cells;
        int[] dist2 = out.dist2;
        while (head < tail) {
            if (len == cells.length) {
                out.len = len;
                out.reserve(len + (tail - head));
                cells = out.cells;
                dist2 = out.dist2;
            }
            int p = now[head++];
            int ci = p & 0xFFFF;
            int cj = p >>> 16;
            int ci0 = ci - mi;
            ci0 = ci0 > half ? ci0 - w : ci0 < -half ? ci0 + w : ci0;
            int cj0 = cj - mj;
            int c = cj * w + ci;
            cells[len] = c;
            dist2[len++] = ci0 * ci0 + cj0 * cj0;
            for (int q = 0; q < 4; q++) {
                int ni;
                int nj;
                int di;
                if (q == 0) {
                    ni = ci + 1 == w ? 0 : ci + 1;
                    nj = cj;
                    di = ci0 + 1;
                } else if (q == 1) {
                    ni = ci == 0 ? w - 1 : ci - 1;
                    nj = cj;
                    di = ci0 - 1;
                } else if (q == 2) {
                    if (cj == 0) {
                        continue;
                    }
                    ni = ci;
                    nj = cj - 1;
                    di = ci0;
                } else {
                    if (cj + 1 == h) {
                        continue;
                    }
                    ni = ci;
                    nj = cj + 1;
                    di = ci0;
                }
                int idx = nj * w + ni;
                if (!sea[idx]) {
                    continue;
                }
                // column offset from the mouth, across the date line where that is shorter
                di = di > half ? di - w : di < -half ? di + w : di;
                int dj = nj - mj;
                int key = window ? (dj + radius) * side + di + radius : idx;
                // without branches: both queues get the cell, the counts say which keeps it
                int fresh = seen[key] != stamp ? 1 : 0;
                seen[key] = stamp;
                int inside = (long) di * di + (long) dj * dj < limit ? fresh : 0; // a way round a cape
                int packed = nj << 16 | ni;
                now[tail] = packed;
                next[nextTail] = packed;
                tail += inside;
                nextTail += fresh - inside;
            }
        }
        this.head = head;
        this.tail = tail;
        this.nextTail = nextTail;
        out.len = len;
        Arrays.fill(out.rings, start, len, ring);
    }

    /**
     * Cells taken from walks, in order, each with its ring and squared distance from the mouth (cells). The
     * distances are never negative, so a user may flag a cell by storing {@code ~dist2}.
     */
    static final class Cells {
        int[] cells = new int[1 << 12];
        int[] rings = new int[1 << 12];
        int[] dist2 = new int[1 << 12];
        int len;

        /** Lets go of a buffer grown past {@code max} cells; only while empty. */
        void shrink(int max) {
            if (cells.length > max) {
                cells = new int[1 << 12];
                rings = new int[1 << 12];
                dist2 = new int[1 << 12];
            }
        }

        /** Room for at least {@code size} cells, keeping the first {@link #len}. */
        void reserve(int size) {
            if (size > cells.length) {
                int grown = Math.max(size, 2 * cells.length);
                cells = Arrays.copyOf(cells, grown);
                rings = Arrays.copyOf(rings, grown);
                dist2 = Arrays.copyOf(dist2, grown);
            }
        }
    }
}
