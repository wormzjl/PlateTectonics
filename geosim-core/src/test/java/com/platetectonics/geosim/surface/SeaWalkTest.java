package com.platetectonics.geosim.surface;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaWalkTest {
    private static final int W = 96;
    private static final int H = 48;

    /** Open sea with round islands, some across the date line: plenty of capes and enclosed bays. */
    private static boolean[] islands(long seed) {
        Random r = new Random(seed);
        boolean[] sea = new boolean[W * H];
        Arrays.fill(sea, true);
        for (int b = 0; b < 70; b++) {
            int ci = r.nextInt(W);
            int cj = r.nextInt(H);
            int rad = 1 + r.nextInt(5);
            for (int dj = -rad; dj <= rad; dj++) {
                for (int di = -rad; di <= rad; di++) {
                    int j = cj + dj;
                    if (di * di + dj * dj <= rad * rad && j >= 0 && j < H) {
                        sea[j * W + Math.floorMod(ci + di, W)] = false;
                    }
                }
            }
        }
        return sea;
    }

    private static int[] seaMouths(boolean[] sea, long seed, int count) {
        Random r = new Random(seed);
        int[] mouths = new int[count];
        int k = 0;
        mouths[k++] = firstSea(sea, H / 2 * W); // near the date line
        while (k < count) {
            int idx = r.nextInt(W * H);
            if (sea[idx]) {
                mouths[k++] = idx;
            }
        }
        return mouths;
    }

    private static int firstSea(boolean[] sea, int from) {
        int idx = from;
        while (!sea[idx]) {
            idx++;
        }
        return idx;
    }

    /** The walk from {@code mouth}, every ring up to {@code maxRing}. */
    private static SeaWalk.Cells walk(SeaWalk walk, int mouth, int maxRing) {
        SeaWalk.Cells out = new SeaWalk.Cells();
        walk.start(mouth);
        while (walk.hasNext() && walk.upcomingRing() <= maxRing) {
            walk.takeRing(out);
        }
        return out;
    }

    private static long distance2(int a, int b) {
        int di = Math.abs(a % W - b % W);
        di = Math.min(di, W - di);
        int dj = a / W - b / W;
        return (long) di * di + (long) dj * dj;
    }

    /** Sea cells 4-connected to {@code mouth}, across the date line. */
    private static int component(boolean[] sea, int mouth) {
        boolean[] seen = new boolean[sea.length];
        ArrayDeque<Integer> open = new ArrayDeque<>();
        seen[mouth] = true;
        open.add(mouth);
        int count = 0;
        while (!open.isEmpty()) {
            int c = open.poll();
            count++;
            int i = c % W;
            int j = c / W;
            int[] next = {j * W + (i + 1) % W, j * W + (i + W - 1) % W, j > 0 ? c - W : -1, j + 1 < H ? c + W : -1};
            for (int nb : next) {
                if (nb >= 0 && sea[nb] && !seen[nb]) {
                    seen[nb] = true;
                    open.add(nb);
                }
            }
        }
        return count;
    }

    @Test
    void takesEveryConnectedSeaCellOnceInRingsOfDistance() {
        boolean[] sea = islands(1);
        SeaWalk walker = new SeaWalk(W, H, sea, -1);
        for (int mouth : seaMouths(sea, 2, 25)) {
            SeaWalk.Cells out = walk(walker, mouth, Integer.MAX_VALUE);
            assertEquals(component(sea, mouth), out.len, "every connected sea cell is taken");
            assertEquals(mouth, out.cells[0]);
            boolean[] taken = new boolean[W * H];
            for (int q = 0; q < out.len; q++) {
                int c = out.cells[q];
                assertTrue(sea[c] && !taken[c], "each sea cell once");
                taken[c] = true;
                assertEquals(distance2(c, mouth), out.dist2[q]);
                long ring = out.rings[q];
                assertTrue(out.dist2[q] < (ring + 1) * (ring + 1), "no cell is taken before its ring");
                if (q > 0) {
                    assertTrue(out.rings[q] >= out.rings[q - 1], "rings come in order");
                }
            }
        }
    }

    @Test
    void walkInAWindowMatchesWalkOverTheWholeGrid() {
        boolean[] sea = islands(3);
        int radius = 20;
        assertTrue(SeaWalk.windowFits(W, radius));
        SeaWalk whole = new SeaWalk(W, H, sea, -1);
        SeaWalk window = new SeaWalk(W, H, sea, radius);
        for (int mouth : seaMouths(sea, 4, 25)) {
            SeaWalk.Cells a = walk(whole, mouth, radius - 2);
            SeaWalk.Cells b = walk(window, mouth, radius - 2);
            assertEquals(a.len, b.len);
            assertArrayEquals(Arrays.copyOf(a.cells, a.len), Arrays.copyOf(b.cells, b.len));
            assertArrayEquals(Arrays.copyOf(a.rings, a.len), Arrays.copyOf(b.rings, b.len));
            assertArrayEquals(Arrays.copyOf(a.dist2, a.len), Arrays.copyOf(b.dist2, b.len));
        }
    }

    @Test
    void walkCarriedOnByAnotherWalkerEndsAsIfNeverInterrupted() {
        boolean[] sea = islands(5);
        SeaWalk straight = new SeaWalk(W, H, sea, -1);
        SeaWalk window = new SeaWalk(W, H, sea, 20);
        SeaWalk carry = new SeaWalk(W, H, sea, -1);
        int[] mouths = seaMouths(sea, 6, 25);
        for (int m = 0; m < mouths.length; m++) {
            int mouth = mouths[m];
            SeaWalk.Cells expected = walk(straight, mouth, Integer.MAX_VALUE);
            SeaWalk.Cells part = walk(window, mouth, 4 + m % 10);
            if (!window.hasNext()) {
                continue;
            }
            int[] queued = Arrays.copyOf(window.queued(), window.queuedCount());
            walk(carry, mouths[(m + 1) % mouths.length], 6); // leave marks of another walk behind
            carry.resume(mouth, part.rings[part.len - 1], part.cells, 0, part.len, queued, 0, queued.length);
            SeaWalk.Cells all = part;
            while (carry.hasNext()) {
                carry.takeRing(all);
            }
            assertEquals(expected.len, all.len);
            assertArrayEquals(Arrays.copyOf(expected.cells, expected.len), Arrays.copyOf(all.cells, all.len));
            assertArrayEquals(Arrays.copyOf(expected.rings, expected.len), Arrays.copyOf(all.rings, all.len));
        }
    }
}
