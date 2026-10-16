package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.grid.CylinderGrid;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plate rotation (D-42): the exact integer rotation, turning plates in the motion step, and spin from torque. */
class PlateRotationTest {

    // ------------------------------------------------------------------ the integer rotation

    @Test
    void rotationIsOneToOneWithAnExactInverse() {
        PlateFrame f = new PlateFrame();
        for (int a = -180; a < 180; a += 7) {
            double theta = Math.toRadians(a + 0.37);
            f.set(theta);
            Set<Long> images = new HashSet<>();
            double worst = 0.0;
            for (int u = -60; u <= 60; u++) {
                for (int v = -60; v <= 60; v++) {
                    long at = f.forward(u, v);
                    assertTrue(images.add(at), "two cells land on one place at " + a + " deg");
                    assertEquals(PlateFrame.pack(u, v), f.inverse(PlateFrame.x(at), PlateFrame.y(at)));
                    long back = f.forward(PlateFrame.x(f.inverse(u, v)), PlateFrame.y(f.inverse(u, v)));
                    assertEquals(PlateFrame.pack(u, v), back);
                    double ex = Math.cos(theta) * u - Math.sin(theta) * v;
                    double ey = Math.sin(theta) * u + Math.cos(theta) * v;
                    worst = Math.max(worst, Math.hypot(PlateFrame.x(at) - ex, PlateFrame.y(at) - ey));
                }
            }
            assertTrue(worst <= 2.0, "a cell lands " + worst + " cells from the exact rotation at " + a + " deg");
        }
    }

    @Test
    void quarterTurnsAndFullTurnsAreExact() {
        PlateFrame f = new PlateFrame();
        f.set(Math.PI / 2.0);
        for (int u = -20; u <= 20; u++) {
            for (int v = -20; v <= 20; v++) {
                assertEquals(PlateFrame.pack(-v, u), f.forward(u, v), "a quarter turn moves x toward y");
            }
        }
        f.set(-Math.PI);
        assertEquals(PlateFrame.pack(-3, -5), f.forward(3, 5));
        f.set(2.0 * Math.PI);
        assertTrue(f.identity(), "a full turn is no turn");
        f.set(0.3);
        long[] a = new long[100];
        for (int k = 0; k < a.length; k++) {
            a[k] = f.forward(k - 50, 17 - k);
        }
        f.set(0.3 + 4.0 * Math.PI);
        for (int k = 0; k < a.length; k++) {
            assertEquals(a[k], f.forward(k - 50, 17 - k), "the map depends on the angle only modulo a full turn");
        }
    }

    @Test
    void aTurnedSolidShapeHasNoHoles() {
        PlateFrame f = new PlateFrame();
        for (int a = 1; a < 360; a += 11) {
            f.set(Math.toRadians(a));
            Set<Long> image = new HashSet<>();
            for (int u = -30; u <= 30; u++) {
                for (int v = -30; v <= 30; v++) {
                    if (u * u + v * v <= 900) {
                        image.add(f.forward(u, v));
                    }
                }
            }
            for (long p : image) {
                int x = PlateFrame.x(p);
                int y = PlateFrame.y(p);
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    long q = PlateFrame.pack(x + d[0], y + d[1]);
                    if (!image.contains(q)) {
                        boolean enclosed = image.contains(PlateFrame.pack(x + d[0] + 1, y + d[1]))
                                && image.contains(PlateFrame.pack(x + d[0] - 1, y + d[1]))
                                && image.contains(PlateFrame.pack(x + d[0], y + d[1] + 1))
                                && image.contains(PlateFrame.pack(x + d[0], y + d[1] - 1));
                        assertTrue(!enclosed, "a hole inside the disk at " + a + " deg");
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ turning plates

    private static final int W = 64;
    private static final int H = 32;

    /**
     * A 64 x 32 world of 100 km cells: plate 1 is a continental disk of radius 9 cells around (32, 16), each
     * cell with its own thickness; plate 0 is old ocean floor around it. Every other plate is emptied.
     */
    private static PlateSimulation disk() {
        PlateSimulation sim = new PlateSimulation(new CylinderGrid(W, H, 100.0), 1L, TectonicsConfig.defaults());
        Lithosphere L = sim.lith;
        for (int idx = 0; idx < L.n; idx++) {
            int di = idx % W - 32;
            int dj = idx / W - 16;
            boolean in = di * di + dj * dj <= 81;
            L.owner[idx] = in ? 1 : 0;
            L.type[idx] = in ? Lithosphere.CONTINENTAL : Lithosphere.OCEANIC;
            L.thick[idx] = in ? 30f + (idx % 13) : 7f;
            L.oceanAge[idx] = in ? 0f : 100f;
            L.formedAt[idx] = 0f;
            L.marks[idx] = 0;
            L.keel[idx] = 0f;
            L.eclogite[idx] = 0f;
            L.sediment[idx] = 0f;
        }
        for (Plate p : sim.plates) {
            p.vx = 0;
            p.vy = 0;
        }
        sim.refreshStats();
        boolean[] all = new boolean[sim.plates.size()];
        Arrays.fill(all, true);
        sim.reframe(all);
        sim.underthrustCrust = new double[sim.plates.size()];
        sim.underthrustKeel = new double[sim.plates.size()];
        sim.underthrustEclogite = new double[sim.plates.size()];
        sim.seaLevelM = 0.0;
        return sim;
    }

    /** Continental thickness by cell, 0 where there is none. */
    private static float[] continent(Lithosphere L) {
        float[] c = new float[L.n];
        for (int idx = 0; idx < L.n; idx++) {
            c[idx] = L.type[idx] == Lithosphere.CONTINENTAL ? L.thick[idx] : 0f;
        }
        return c;
    }

    /** Turns plate 1 to an absolute angle and moves its crust there (no time passes, so nothing grows or ages). */
    private static void turnTo(PlateSimulation sim, double theta) {
        sim.plates.get(1).frame.set(theta);
        PlateMotion.substep(sim, 0.0, 0.0);
    }

    @Test
    void turningAPlateCopiesItsCrustExactly() {
        float[] before = continent(disk().lith);
        for (double deg : new double[] {3, 17, 44, 46, 90, 133, -71}) {
            PlateSimulation sim = disk();
            turnTo(sim, Math.toRadians(deg));
            assertArrayEquals(nonZeroSorted(before), nonZeroSorted(continent(sim.lith)),
                    "every continental cell arrives once, unchanged, at " + deg + " deg");
            assertEquals(0L, sim.roundingHoleCells, "a solid plate turns without holes");
            turnTo(sim, 0.0);
            assertArrayEquals(before, continent(sim.lith), "turning back puts every cell where it was");
        }
    }

    private static float[] nonZeroSorted(float[] a) {
        int n = 0;
        for (float x : a) {
            n += x != 0f ? 1 : 0;
        }
        float[] b = new float[n];
        int k = 0;
        for (float x : a) {
            if (x != 0f) {
                b[k++] = x;
            }
        }
        Arrays.sort(b);
        return b;
    }

    @Test
    void everyCellSitsWhereItsPlatesPosePutsIt() {
        PlateSimulation sim = new PlateSimulation(CylinderGrid.forPlanet(40_000.0, 128), 11L, TectonicsConfig.defaults());
        int turned = 0;
        for (int s = 0; s < 80; s++) {
            sim.step(10.0);
            Lithosphere L = sim.lith;
            int w = sim.grid.width();
            Map<Integer, Set<Long>> places = new HashMap<>();
            for (int idx = 0; idx < L.n; idx++) {
                Plate p = sim.plates.get(L.owner[idx]);
                long at = p.frame.forward(L.u[idx], L.v[idx]);
                assertEquals(idx % w, Math.floorMod(p.originI + PlateFrame.x(at), w), "column of cell " + idx);
                assertEquals(idx / w, p.originJ + PlateFrame.y(at), "row of cell " + idx);
                assertTrue(places.computeIfAbsent(p.id, k -> new HashSet<>()).add(PlateFrame.pack(L.u[idx], L.v[idx])),
                        "two cells of one plate share a place");
            }
        }
        for (Plate p : sim.plates) {
            if (p.alive && !p.frame.identity()) {
                turned++;
            }
        }
        assertTrue(turned > 0, "some plates have turned");
    }

    // ------------------------------------------------------------------ spin from torque

    @Test
    void slabPullOnOneCornerSpinsThePlateTheRightWay() {
        TectonicsConfig cfg = with(TectonicsConfig.defaults(), "driftKmPerMyr", 0.0);
        for (int sign : new int[] {1, -1}) {
            PlateSimulation sim = new PlateSimulation(new CylinderGrid(W, H, 100.0), 1L, cfg);
            Lithosphere L = sim.lith;
            // plate 1 is ocean floor in columns 16-39, plate 0 the rest; plate 1 moves east into plate 0. On the
            // northern (sign 1) or southern (sign -1) half of the contact plate 1's floor is older, so it sinks
            // there and is pulled east at that end only
            for (int idx = 0; idx < L.n; idx++) {
                int i = idx % W;
                int j = idx / W;
                boolean one = i >= 16 && i < 40;
                boolean pulledHalf = sign > 0 ? j < H / 2 : j >= H / 2;
                L.owner[idx] = one ? 1 : 0;
                L.type[idx] = Lithosphere.OCEANIC;
                L.thick[idx] = 7f;
                L.oceanAge[idx] = one ? (pulledHalf ? 150f : 10f) : 60f;
                L.sediment[idx] = 0f;
            }
            for (Plate p : sim.plates) {
                p.vx = p.id == 1 ? 50.0 : 0.0;
                p.vy = 0.0;
            }
            sim.refreshStats();
            boolean[] all = new boolean[sim.plates.size()];
            Arrays.fill(all, true);
            sim.reframe(all);
            sim.refreshSpin();
            PlateDynamics.updateVelocities(sim, 10.0);
            double omega = sim.plates.get(1).omega;
            // pulled east at the north end, the plate turns east toward south: clockwise on a north-up map
            assertTrue(omega * sign > 0.0, "spin " + omega + " for pull on the " + (sign > 0 ? "north" : "south"));
        }
    }

    @Test
    void infiniteSpinDragTurnsSpinOff() {
        TectonicsConfig cfg = with(TectonicsConfig.defaults(), "spinDragFactor", Double.POSITIVE_INFINITY);
        PlateSimulation sim = new PlateSimulation(CylinderGrid.forPlanet(40_000.0, 128), 11L, cfg);
        for (int s = 0; s < 40; s++) {
            sim.step(10.0);
        }
        for (Plate p : sim.plates) {
            assertEquals(0.0, p.omega);
            assertTrue(p.frame.identity());
        }
        assertEquals(0L, sim.roundingHoleCells);
    }

    /** A copy of the tectonics constants with one replaced. */
    static TectonicsConfig with(TectonicsConfig cfg, String name, double value) {
        try {
            RecordComponent[] parts = TectonicsConfig.class.getRecordComponents();
            Object[] values = new Object[parts.length];
            Class<?>[] types = new Class<?>[parts.length];
            for (int k = 0; k < parts.length; k++) {
                types[k] = parts[k].getType();
                values[k] = parts[k].getAccessor().invoke(cfg);
                if (parts[k].getName().equals(name)) {
                    values[k] = types[k] == int.class ? (Object) (int) value : (Object) value;
                }
            }
            return TectonicsConfig.class.getDeclaredConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
