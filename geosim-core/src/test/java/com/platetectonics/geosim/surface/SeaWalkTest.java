package com.platetectonics.geosim.surface;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaWalkTest {
    /** A sea mask on a w x h cylinder. */
    private record Sea(int w, int h, boolean[] sea) {
        int size() {
            return w * h;
        }
    }

    /** Open sea with round islands, some across the date line: plenty of capes and enclosed bays. */
    private static Sea islands(int w, int h, long seed) {
        Random r = new Random(seed);
        boolean[] sea = new boolean[w * h];
        Arrays.fill(sea, true);
        int count = Math.max(1, 70 * w * h / (96 * 48));
        for (int b = 0; b < count; b++) {
            int ci = r.nextInt(w);
            int cj = r.nextInt(h);
            int rad = 1 + r.nextInt(5);
            for (int dj = -rad; dj <= rad; dj++) {
                for (int di = -rad; di <= rad; di++) {
                    int j = cj + dj;
                    if (di * di + dj * dj <= rad * rad && j >= 0 && j < h) {
                        sea[j * w + Math.floorMod(ci + di, w)] = false;
                    }
                }
            }
        }
        return new Sea(w, h, sea);
    }

    /** Open sea with a few land cells only: long thin grids keep most of their sea. */
    private static Sea sparse(int w, int h, long seed) {
        Random r = new Random(seed);
        boolean[] sea = new boolean[w * h];
        Arrays.fill(sea, true);
        for (int b = 0; b < w * h / 12; b++) {
            sea[r.nextInt(w * h)] = false;
        }
        return new Sea(w, h, sea);
    }

    private static int[] seaMouths(Sea s, long seed, int count) {
        Random r = new Random(seed);
        int[] mouths = new int[count];
        int k = 0;
        mouths[k++] = firstSea(s, s.h / 2 * s.w); // near the date line
        while (k < count) {
            int idx = r.nextInt(s.size());
            if (s.sea[idx]) {
                mouths[k++] = idx;
            }
        }
        return mouths;
    }

    private static int firstSea(Sea s, int from) {
        int idx = from;
        while (!s.sea[idx]) {
            idx = (idx + 1) % s.size();
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

    private static long distance2(Sea s, int a, int b) {
        int di = Math.abs(a % s.w - b % s.w);
        di = Math.min(di, s.w - di);
        int dj = a / s.w - b / s.w;
        return (long) di * di + (long) dj * dj;
    }

    /** Sea cells 4-connected to {@code mouth}, across the date line. */
    private static int component(Sea s, int mouth) {
        int w = s.w;
        boolean[] seen = new boolean[s.size()];
        ArrayDeque<Integer> open = new ArrayDeque<>();
        seen[mouth] = true;
        open.add(mouth);
        int count = 0;
        while (!open.isEmpty()) {
            int c = open.poll();
            count++;
            int i = c % w;
            int j = c / w;
            int[] next = {j * w + (i + 1) % w, j * w + (i + w - 1) % w, j > 0 ? c - w : -1, j + 1 < s.h ? c + w : -1};
            for (int nb : next) {
                if (nb >= 0 && s.sea[nb] && !seen[nb]) {
                    seen[nb] = true;
                    open.add(nb);
                }
            }
        }
        return count;
    }

    private static void assertSameWalk(SeaWalk.Cells expected, SeaWalk.Cells actual) {
        assertEquals(expected.len, actual.len);
        assertArrayEquals(Arrays.copyOf(expected.cells, expected.len), Arrays.copyOf(actual.cells, actual.len));
        assertArrayEquals(Arrays.copyOf(expected.rings, expected.len), Arrays.copyOf(actual.rings, actual.len));
        assertArrayEquals(Arrays.copyOf(expected.dist2, expected.len), Arrays.copyOf(actual.dist2, actual.len));
    }

    @Test
    void takesEveryConnectedSeaCellOnceInRingsOfDistance() {
        // odd widths put the far side of the date line at an odd offset; one and two rows have no poles between
        Sea[] seas = {islands(96, 48, 1), islands(95, 47, 7), sparse(41, 1, 8), sparse(40, 2, 9)};
        for (Sea s : seas) {
            SeaWalk walker = new SeaWalk(s.w, s.h, s.sea, -1);
            for (int mouth : seaMouths(s, 2, 25)) {
                SeaWalk.Cells out = walk(walker, mouth, Integer.MAX_VALUE);
                assertEquals(component(s, mouth), out.len, "every connected sea cell is taken");
                assertEquals(mouth, out.cells[0]);
                boolean[] taken = new boolean[s.size()];
                for (int q = 0; q < out.len; q++) {
                    int c = out.cells[q];
                    assertTrue(s.sea[c] && !taken[c], "each sea cell once");
                    taken[c] = true;
                    assertEquals(distance2(s, c, mouth), out.dist2[q]);
                    long ring = out.rings[q];
                    assertTrue(out.dist2[q] < (ring + 1) * (ring + 1), "no cell is taken before its ring");
                    if (q > 0) {
                        assertTrue(out.rings[q] >= out.rings[q - 1], "rings come in order");
                    }
                }
            }
        }
    }

    @Test
    void walkInAWindowMatchesWalkOverTheWholeGrid() {
        Sea[] seas = {islands(96, 48, 3), islands(95, 47, 10), sparse(41, 1, 11), sparse(40, 2, 12)};
        for (Sea s : seas) {
            int radius = Math.min(20, (s.w - 1) / 2);
            assertTrue(SeaWalk.windowFits(s.w, radius));
            SeaWalk whole = new SeaWalk(s.w, s.h, s.sea, -1);
            SeaWalk window = new SeaWalk(s.w, s.h, s.sea, radius);
            for (int mouth : seaMouths(s, 4, 25)) {
                // as far as the window allows
                assertSameWalk(walk(whole, mouth, radius - 1), walk(window, mouth, radius - 1));
            }
        }
    }

    @Test
    void walkInAWindowRefusesToLeaveIt() {
        Sea s = sparse(96, 48, 13);
        SeaWalk window = new SeaWalk(s.w, s.h, s.sea, 6);
        int mouth = firstSea(s, s.h / 2 * s.w + 40);
        walk(window, mouth, 5);
        assertTrue(window.hasNext() && window.upcomingRing() == 6);
        assertThrows(IllegalStateException.class, () -> window.takeRing(new SeaWalk.Cells()));
    }

    @Test
    void walkCarriedOnByAnotherWalkerEndsAsIfNeverInterrupted() {
        Sea[] seas = {islands(96, 48, 5), islands(95, 47, 14), sparse(41, 1, 15), sparse(40, 2, 16)};
        for (Sea s : seas) {
            int radius = Math.min(20, (s.w - 1) / 2);
            SeaWalk straight = new SeaWalk(s.w, s.h, s.sea, -1);
            SeaWalk window = new SeaWalk(s.w, s.h, s.sea, radius);
            SeaWalk carry = new SeaWalk(s.w, s.h, s.sea, -1);
            int[] mouths = seaMouths(s, 6, 25);
            for (int m = 0; m < mouths.length; m++) {
                int mouth = mouths[m];
                SeaWalk.Cells expected = walk(straight, mouth, Integer.MAX_VALUE);
                SeaWalk.Cells part = walk(window, mouth, Math.min(radius - 1, 4 + m % 10));
                if (!window.hasNext()) {
                    continue;
                }
                // the recorded walk and queue sit behind other records, as they do in a batch
                int pad = 3 + m;
                int[] taken = new int[pad + part.len];
                System.arraycopy(part.cells, 0, taken, pad, part.len);
                int[] queued = new int[pad + window.queuedCount() + 2];
                System.arraycopy(window.queued(), 0, queued, pad, window.queuedCount());
                walk(carry, mouths[(m + 1) % mouths.length], 6); // leave marks of another walk behind
                carry.resume(mouth, part.rings[part.len - 1], taken, pad, pad + part.len, queued, pad,
                        pad + window.queuedCount());
                SeaWalk.Cells all = part;
                while (carry.hasNext()) {
                    carry.takeRing(all);
                }
                assertSameWalk(expected, all);
            }
        }
    }
}
