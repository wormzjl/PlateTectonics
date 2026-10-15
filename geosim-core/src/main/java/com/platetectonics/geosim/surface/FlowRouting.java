package com.platetectonics.geosim.surface;

import com.platetectonics.geosim.grid.CylinderGrid;

/**
 * Single-direction (D8) flow routing over the land with depressions filled (Barnes et al. 2014,
 * Priority-Flood + FIFO). Starting from the coast, cells are taken lowest first. Each land cell drains to
 * the neighbour that reached it first, which is its lowest neighbour on the filled surface. Cells inside
 * a depression get the depression's spill level as their water surface and form a lake.
 * <p>
 * {@link #order} lists land cells so that every cell comes after its receiver (downstream first);
 * walking it backwards visits every cell before its receiver (upstream first).
 */
public final class FlowRouting {
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
    private final int[] pit;
    private final int[] heapCell;
    private final float[] heapKey;
    private int heapSize;
    private int landCount;
    private float[] elevation;
    private float seaLevel;

    public FlowRouting(CylinderGrid grid) {
        this.grid = grid;
        int n = grid.size();
        receiver = new int[n];
        order = new int[n];
        filled = new float[n];
        lengthM = new float[n];
        ocean = new boolean[n];
        visited = new boolean[n];
        pit = new int[n];
        heapCell = new int[n];
        heapKey = new float[n];
    }

    /** Routes flow over {@code elevationM} (datum); cells at or below {@code seaLevelM} are ocean. */
    public void route(float[] elevationM, double seaLevelM) {
        this.elevation = elevationM;
        this.seaLevel = (float) seaLevelM;
        int n = grid.size();
        int w = grid.width();
        int h = grid.height();
        double cellM = grid.cellKm() * 1000.0;
        heapSize = 0;
        landCount = 0;
        int lowest = 0;
        boolean anyOcean = false;
        for (int idx = 0; idx < n; idx++) {
            ocean[idx] = elevationM[idx] <= seaLevel;
            visited[idx] = ocean[idx];
            receiver[idx] = idx;
            filled[idx] = elevationM[idx];
            lengthM[idx] = (float) cellM;
            anyOcean |= ocean[idx];
            if (elevationM[idx] < elevationM[lowest]) {
                lowest = idx;
            }
        }
        if (!anyOcean) {
            // an all-land planet drains to its lowest point
            ocean[lowest] = true;
            visited[lowest] = true;
        }
        for (int idx = 0; idx < n; idx++) {
            if (ocean[idx] && touchesLand(idx, w, h)) {
                push(idx, Math.max(seaLevel, elevationM[idx]));
            }
        }
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
                level = Math.max(seaLevel, elevationM[c]);
            } else {
                order[landCount++] = c;
                level = filled[c];
            }
            int ci = c % w;
            int cj = c / w;
            for (int k = 0; k < 8; k++) {
                int nj = cj + DJ[k];
                if (nj < 0 || nj >= h) {
                    continue;
                }
                int ni = ci + DI[k];
                ni = ni < 0 ? ni + w : ni >= w ? ni - w : ni;
                int nb = nj * w + ni;
                if (visited[nb]) {
                    continue;
                }
                visited[nb] = true;
                receiver[nb] = c;
                lengthM[nb] = (float) (k < 4 ? cellM : cellM * Math.sqrt(2.0));
                if (elevationM[nb] <= level) {
                    filled[nb] = level;
                    pit[pitTail++] = nb;
                } else {
                    filled[nb] = elevationM[nb];
                    push(nb, filled[nb]);
                }
            }
        }
    }

    /** Number of land cells in {@link #order}. */
    public int landCount() {
        return landCount;
    }

    public boolean isOcean(int idx) {
        return ocean[idx];
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

    // a binary min-heap on float keys; ties keep a fixed (deterministic) order

    private void push(int cell, float key) {
        int k = heapSize++;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            if (heapKey[parent] <= key) {
                break;
            }
            heapCell[k] = heapCell[parent];
            heapKey[k] = heapKey[parent];
            k = parent;
        }
        heapCell[k] = cell;
        heapKey[k] = key;
    }

    private int pop() {
        int top = heapCell[0];
        int lastCell = heapCell[--heapSize];
        float lastKey = heapKey[heapSize];
        int k = 0;
        while (true) {
            int child = 2 * k + 1;
            if (child >= heapSize) {
                break;
            }
            if (child + 1 < heapSize && heapKey[child + 1] < heapKey[child]) {
                child++;
            }
            if (heapKey[child] >= lastKey) {
                break;
            }
            heapCell[k] = heapCell[child];
            heapKey[k] = heapKey[child];
            k = child;
        }
        heapCell[k] = lastCell;
        heapKey[k] = lastKey;
        return top;
    }
}
