package com.platetectonics.geosim.surface;

import com.platetectonics.geosim.grid.CylinderGrid;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowRoutingTest {
    private static final int W = 40;
    private static final int H = 20;

    /** Ocean in columns 0-4, land rising 10 m per column to the east, with a pit at (20, 10). */
    private static float[] slopeWithPit(CylinderGrid g) {
        float[] e = new float[g.size()];
        for (int idx = 0; idx < e.length; idx++) {
            int i = g.iOf(idx);
            int j = g.jOf(idx);
            e[idx] = i < 5 ? -1_000f : 10f * (i - 4) + 0.5f * Math.abs(j - H / 2);
        }
        e[g.index(20, 10)] = 1f;
        return e;
    }

    @Test
    void everyLandCellComesAfterItsReceiverAndReachesTheSea() {
        CylinderGrid g = new CylinderGrid(W, H, 10.0);
        float[] e = slopeWithPit(g);
        FlowRouting flow = new FlowRouting(g);
        flow.route(e, 0.0);

        int land = (int) Arrays.stream(boxed(e)).filter(v -> v > 0f).count();
        assertEquals(land, flow.landCount(), "every land cell is routed once");
        int[] pos = new int[g.size()];
        Arrays.fill(pos, -1);
        for (int k = 0; k < flow.landCount(); k++) {
            pos[flow.order[k]] = k;
        }
        for (int k = 0; k < flow.landCount(); k++) {
            int idx = flow.order[k];
            int r = flow.receiver[idx];
            assertTrue(flow.isOcean(r) || pos[r] < pos[idx], "receiver first (downstream-first order)");
            assertTrue(flow.filled[idx] >= e[idx], "water surface never below the ground");
            int c = idx;
            for (int steps = 0; !flow.isOcean(c); steps++) {
                assertTrue(steps < g.size(), "no cycles");
                c = flow.receiver[c];
            }
        }
    }

    @Test
    void depressionFillsToItsSpillLevelAndBecomesALake() {
        CylinderGrid g = new CylinderGrid(W, H, 10.0);
        float[] e = slopeWithPit(g);
        FlowRouting flow = new FlowRouting(g);
        flow.route(e, 0.0);

        int pit = g.index(20, 10);
        assertTrue(flow.isLake(pit));
        float lowestNeighbour = Float.MAX_VALUE;
        for (int dj = -1; dj <= 1; dj++) {
            for (int di = -1; di <= 1; di++) {
                if (di != 0 || dj != 0) {
                    lowestNeighbour = Math.min(lowestNeighbour, e[g.index(20 + di, 10 + dj)]);
                }
            }
        }
        assertEquals(lowestNeighbour, flow.filled[pit], 1e-3, "the lake stands at its spill height");
        assertFalse(flow.isLake(g.index(30, 5)), "an ordinary slope is no lake");
    }

    private static Float[] boxed(float[] a) {
        Float[] out = new Float[a.length];
        for (int k = 0; k < a.length; k++) {
            out[k] = a[k];
        }
        return out;
    }
}
