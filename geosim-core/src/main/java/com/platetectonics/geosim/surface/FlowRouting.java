package com.platetectonics.geosim.surface;

import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.parallel.Parallel;

/**
 * Single-direction (D8) flow routing over the land with depressions filled (Barnes et al. 2014,
 * Priority-Flood + FIFO). Starting from the coast, cells are taken lowest first. Each land cell drains to
 * the neighbour that reached it first, which is its lowest neighbour on the filled surface. Cells inside
 * a depression get the depression's spill level as their water surface and form a lake.
 * <p>
 * The ocean is the water below sea level that belongs to a sea of at least {@link #MIN_SEA_KM2}. A
 * smaller hollow below sea level with land all round is a depression like any other: a lake that rivers
 * fill with sediment up to its spill level, rather than a one-cell sea that swallows a river's whole load.
 * <p>
 * {@link #order} lists land cells so that every cell comes after its receiver (downstream first);
 * walking it backwards visits every cell before its receiver (upstream first).
 */
public final class FlowRouting {
    /** Smallest enclosed sea, km2 (the Caspian is 371,000); smaller hollows below sea level are lakes. */
    public static final double MIN_SEA_KM2 = 100_000.0;
    private static final int[] DI = {1, -1, 0, 0, 1, -1, 1, -1};
    private static final int[] DJ = {0, 0, 1, -1, 1, 1, -1, -1};

    private final CylinderGrid grid;
    /** Receiver of each cell; ocean cells receive themselves. */
    public final int[] receiver;
    /** Land cells, downstream first. */
    public final int[] order;
    /** Water surface: the lake level in depressions, else the ground (m on the datum). */
    public final float[] filled;
    /** Distance to the receiver, m. */
    public final float[] lengthM;
    private final boolean[] ocean;
    private final boolean[] visited;
    /** Ocean cells next to land: the starting coast. */
    private final boolean[] coast;
    private final int[] pit;
    /** Heap entries: an order-preserving int of the float key in the high half, the cell in the low half. */
    private final long[] heap;
    /** Index offsets of the 8 neighbours (same order as {@link #DI}, {@link #DJ}) away from the grid edges. */
    private final int[] offset = new int[8];
    private int heapSize;
    private int landCount;
    private float[] elevation;
    private float seaLevel;
    private Parallel parallel = Parallel.SERIAL;

    public FlowRouting(CylinderGrid grid) {
        this.grid = grid;
        int n = grid.size();
        receiver = new int[n];
        order = new int[n];
        filled = new float[n];
        lengthM = new float[n];
        ocean = new boolean[n];
        visited = new boolean[n];
        coast = new boolean[n];
        pit = new int[n];
        heap = new long[n];
        for (int k = 0; k < 8; k++) {
            offset[k] = DJ[k] * grid.width() + DI[k];
        }
    }

    /**
     * Worker threads for the per-cell setup. The flood itself takes cells in priority order and stays on
     * one thread.
     */
    public void setParallel(Parallel parallel) {
        this.parallel = parallel;
    }

    /** Routes flow over {@code elevationM} (datum); cells at or below {@code seaLevelM} are ocean. */
    public void route(float[] elevationM, double seaLevelM) {
        this.elevation = elevationM;
        this.seaLevel = (float) seaLevelM;
        int n = grid.size();
        int w = grid.width();
        int h = grid.height();
        double cellM = grid.cellKm() * 1000.0;
        float sea = seaLevel;
        float straightM = (float) cellM;
        float diagonalM = (float) (cellM * Math.sqrt(2.0));
        heapSize = 0;
        landCount = 0;
        parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                ocean[idx] = elevationM[idx] <= sea;
                visited[idx] = ocean[idx];
                receiver[idx] = idx;
                filled[idx] = elevationM[idx];
                lengthM[idx] = straightM;
            }
        });
        dropSmallSeas(n, w, h);
        int firstOcean = 0;
        while (firstOcean < n && !ocean[firstOcean]) {
            firstOcean++;
        }
        if (firstOcean == n) {
            // an all-land planet drains to its lowest point
            int lowest = 0;
            for (int idx = 1; idx < n; idx++) {
                if (elevationM[idx] < elevationM[lowest]) {
                    lowest = idx;
                }
            }
            ocean[lowest] = true;
            visited[lowest] = true;
        }
        parallel.rows(h, (from, to) -> {
            for (int idx = from * w; idx < to * w; idx++) {
                coast[idx] = ocean[idx] && touchesLand(idx, w, h);
            }
        });
        for (int idx = 0; idx < n; idx++) {
            if (coast[idx]) {
                push(idx, Math.max(sea, elevationM[idx]));
            }
        }
        int[] offset = this.offset;
        int pitHead = 0;
        int pitTail = 0;
        while (pitHead < pitTail || heapSize > 0) {
            int c;
            if (pitHead < pitTail) {
                c = pit[pitHead++];
            } else {
                c = pop();
                pitHead = 0;
                pitTail = 0;
            }
            float level;
            if (ocean[c]) {
                level = Math.max(sea, elevationM[c]);
            } else {
                order[landCount++] = c;
                level = filled[c];
            }
            int cj = c / w;
            int ci = c - cj * w;
            boolean inner = ci > 0 && ci < w - 1 && cj > 0 && cj < h - 1;
            for (int k = 0; k < 8; k++) {
                int nb;
                if (inner) {
                    nb = c + offset[k];
                } else {
                    int nj = cj + DJ[k];
                    if (nj < 0 || nj >= h) {
                        continue;
                    }
                    int ni = ci + DI[k];
                    ni = ni < 0 ? ni + w : ni >= w ? ni - w : ni;
                    nb = nj * w + ni;
                }
                if (visited[nb]) {
                    continue;
                }
                visited[nb] = true;
                receiver[nb] = c;
                lengthM[nb] = k < 4 ? straightM : diagonalM;
                float e = elevationM[nb];
                if (e <= level) {
                    filled[nb] = level;
                    pit[pitTail++] = nb;
                } else {
                    filled[nb] = e;
                    push(nb, e);
                }
            }
        }
    }

    /**
     * Turns water bodies below sea level smaller than {@link #MIN_SEA_KM2} into land. Water connects
     * through cell edges only (as sediment spreads at sea): a hollow touching the sea at a corner is a lake.
     * If no water body is that large, the largest one stays ocean.
     */
    private void dropSmallSeas(int n, int w, int h) {
        int minCells = (int) Math.ceil(MIN_SEA_KM2 / grid.cellAreaKm2());
        boolean[] seen = coast; // scratch: the coast is worked out afterwards
        java.util.Arrays.fill(seen, false);
        int[] queue = pit; // scratch: the flood below starts with an empty pit queue
        int largest = -1;
        int largestSize = 0;
        boolean kept = false;
        for (int start = 0; start < n; start++) {
            if (!ocean[start] || seen[start]) {
                continue;
            }
            int tail = floodWater(start, queue, seen, w, h);
            if (tail >= minCells) {
                kept = true;
                continue;
            }
            if (tail > largestSize) {
                largestSize = tail;
                largest = start;
            }
            for (int k = 0; k < tail; k++) {
                ocean[queue[k]] = false;
                visited[queue[k]] = false;
            }
        }
        if (!kept && largest >= 0) {
            // flood the largest body again from its first cell; it alone stays ocean
            for (int idx = 0; idx < n; idx++) {
                ocean[idx] = elevation[idx] <= seaLevel;
                seen[idx] = false;
            }
            floodWater(largest, queue, seen, w, h);
            for (int idx = 0; idx < n; idx++) {
                ocean[idx] = seen[idx];
                visited[idx] = seen[idx];
            }
        }
    }

    /** Collects the water cells edge-connected to {@code start} into {@code queue}; returns how many. */
    private int floodWater(int start, int[] queue, boolean[] seen, int w, int h) {
        int head = 0;
        int tail = 0;
        queue[tail++] = start;
        seen[start] = true;
        while (head < tail) {
            int c = queue[head++];
            int cj = c / w;
            int ci = c - cj * w;
            for (int k = 0; k < 4; k++) {
                int nj = cj + DJ[k];
                if (nj < 0 || nj >= h) {
                    continue;
                }
                int ni = ci + DI[k];
                ni = ni < 0 ? ni + w : ni >= w ? ni - w : ni;
                int nb = nj * w + ni;
                if (ocean[nb] && !seen[nb]) {
                    seen[nb] = true;
                    queue[tail++] = nb;
                }
            }
        }
        return tail;
    }

    /** Number of land cells in {@link #order}. */
    public int landCount() {
        return landCount;
    }

    public boolean isOcean(int idx) {
        return ocean[idx];
    }

    /** Ocean cells of the last routing (read only; the same array every step). */
    boolean[] oceanMask() {
        return ocean;
    }

    /** True inside a filled depression (the water stands at least half a metre deep). */
    public boolean isLake(int idx) {
        return !ocean[idx] && filled[idx] > elevation[idx] + 0.5f;
    }

    public float seaLevel() {
        return seaLevel;
    }

    private boolean touchesLand(int idx, int w, int h) {
        int ci = idx % w;
        int cj = idx / w;
        for (int k = 0; k < 8; k++) {
            int nj = cj + DJ[k];
            if (nj < 0 || nj >= h) {
                continue;
            }
            int ni = ci + DI[k];
            ni = ni < 0 ? ni + w : ni >= w ? ni - w : ni;
            if (!ocean[nj * w + ni]) {
                return true;
            }
        }
        return false;
    }

    // A binary min-heap on float keys; ties keep a fixed (deterministic) order. Each entry packs the key,
    // mapped to an int with the same order, above the cell, so one load reads both. Keys are compared
    // through the high half only: tie order stays that of the heap, never the cell index.

    private static long entry(int cell, float key) {
        int bits = Float.floatToRawIntBits(key + 0.0f); // -0 becomes +0, which compares equal to it
        return (long) (bits ^ ((bits >> 31) & 0x7FFFFFFF)) << 32 | cell;
    }

    private void push(int cell, float key) {
        long entry = entry(cell, key);
        long rank = entry >> 32;
        int k = heapSize++;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            long up = heap[parent];
            if (up >> 32 <= rank) {
                break;
            }
            heap[k] = up;
            k = parent;
        }
        heap[k] = entry;
    }

    private int pop() {
        long top = heap[0];
        int size = --heapSize;
        long last = heap[size];
        long rank = last >> 32;
        int k = 0;
        while (true) {
            int child = 2 * k + 1;
            if (child >= size) {
                break;
            }
            // Take the right child only if its key is smaller (branch-free). A right child at index size is
            // the old copy of last, which can only end the loop here, as it would have without it.
            child += (int) (((heap[child + 1] >> 32) - (heap[child] >> 32)) >>> 63);
            long down = heap[child];
            if (down >> 32 >= rank) {
                break;
            }
            heap[k] = down;
            k = child;
        }
        heap[k] = last;
        return (int) top;
    }
}
