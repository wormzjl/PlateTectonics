package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateTectonicsTest {
    private static final int W = 32;
    private static final int H = 16;

    /**
     * A 32 x 16 world of 100 km cells: plate 0 holds columns 0-15, plate 1 columns 16-31; every other plate
     * the constructor made is emptied (and so dead). Nothing moves until the test sets shifts or velocities.
     */
    private static PlateSimulation twoPlates(byte typeA, float thickA, float ageA, byte typeB, float thickB, float ageB) {
        PlateSimulation sim = new PlateSimulation(new CylinderGrid(W, H, 100.0), 1L, TectonicsConfig.defaults());
        Lithosphere L = sim.lith;
        for (int idx = 0; idx < L.n; idx++) {
            boolean a = idx % W < W / 2;
            L.owner[idx] = a ? 0 : 1;
            L.type[idx] = a ? typeA : typeB;
            L.thick[idx] = a ? thickA : thickB;
            L.oceanAge[idx] = a ? ageA : ageB;
            L.formedAt[idx] = 0f;
            L.marks[idx] = 0;
            L.keel[idx] = 0f;
            L.eclogite[idx] = 0f;
        }
        for (Plate p : sim.plates) {
            p.vx = 0;
            p.vy = 0;
            p.shiftX = 0;
            p.shiftY = 0;
        }
        sim.refreshStats();
        sim.underthrustCrust = new double[sim.plates.size()];
        sim.underthrustKeel = new double[sim.plates.size()];
        sim.underthrustEclogite = new double[sim.plates.size()];
        sim.seaLevelM = 0.0;
        return sim;
    }

    private static double totalCrust(Lithosphere L) {
        double sum = 0.0;
        for (int idx = 0; idx < L.n; idx++) {
            sum += L.thick[idx];
        }
        return sum;
    }

    private static int continentalCells(Lithosphere L) {
        int c = 0;
        for (int idx = 0; idx < L.n; idx++) {
            if (L.type[idx] == Lithosphere.CONTINENTAL) {
                c++;
            }
        }
        return c;
    }

    // ------------------------------------------------------------------ boundaries

    @Test
    void oceanSubductsUnderContinentAndBuildsAnArc() {
        PlateSimulation sim = twoPlates(Lithosphere.CONTINENTAL, 35f, 0f, Lithosphere.OCEANIC, 7f, 100f);
        sim.plates.get(1).shiftX = -1;
        PlateMotion.substep(sim, 1.0, 1.0);
        Lithosphere L = sim.lith;

        assertEquals(H, sim.subductedCells, "one column of ocean floor goes down the trench");
        assertEquals(H, sim.newOceanCells, "the opening on the far side fills with new ocean floor");
        assertEquals(W / 2 * H, continentalCells(L), "no continental crust is lost");
        for (int j = 0; j < H; j++) {
            int trench = j * W + 15;
            assertEquals(0, L.owner[trench]);
            assertTrue((L.marks[trench] & Lithosphere.MARK_TRENCH) != 0);
            // arc band 80-250 km behind the trench grows; the cell at the trench does not
            assertEquals(35.25f, L.thick[j * W + 13], 1e-4f);
            assertEquals(35f, L.thick[j * W + 8], 1e-4f);
            assertEquals(1f, L.oceanAge[j * W + 31], 1e-6f, "new ridge crust is as old as the substep");
        }
    }

    @Test
    void continentalCollisionUnderthrustsAndConservesCrust() {
        PlateSimulation sim = twoPlates(Lithosphere.CONTINENTAL, 35f, 0f, Lithosphere.CONTINENTAL, 35f, 0f);
        Lithosphere L = sim.lith;
        double before = totalCrust(L);
        sim.plates.get(1).shiftX = -1;
        PlateMotion.substep(sim, 1.0, 1.0);
        PlateMotion.distributeUnderthrust(sim);

        TectonicsConfig cfg = sim.cfg;
        assertEquals(H, sim.collisionCells);
        assertEquals(0, sim.subductedCells, "continents never subduct");
        double lost = (1.0 - cfg.collisionTransfer()) * 35.0 * H;
        assertEquals(lost, sim.budgetCollision, 1e-6);
        // new ocean floor (7 km) opens behind plate 1
        assertEquals(before - lost + 7.0 * H, totalCrust(L), 1e-2);
        for (int j = 0; j < H; j++) {
            // frontal range at the suture, plateau crust 150-300 km behind it, nothing in between
            assertEquals(35.0 + cfg.underthrustFrontFraction() * 35.0, L.thick[j * W + 15], 1e-3);
            assertEquals(35.0, L.thick[j * W + 14], 1e-3);
            assertTrue(L.thick[j * W + 13] > 40f && L.thick[j * W + 12] > 40f);
            assertTrue(L.keel[j * W + 15] > 0f, "the mantle keel thickens with the crust");
        }
    }

    @Test
    void subductionPolarityIsKeptOnceDecided() {
        SubductionPolarity polarity = new SubductionPolarity();
        assertEquals(3, polarity.sinking(3, 5, 100f, 50f, 0), "the older floor starts sinking");
        assertEquals(3, polarity.sinking(3, 5, 10f, 90f, 1), "and keeps sinking when younger floor arrives");
        assertEquals(3, polarity.known(5, 3));
        polarity.forgetStale(10);
        assertEquals(-1, polarity.known(3, 5));
        assertEquals(5, polarity.sinking(3, 5, 10f, 90f, 10));
    }

    // ------------------------------------------------------------------ isostasy and plateaus

    @Test
    void isostasyMatchesReferenceValues() {
        assertEquals(0.0, Isostasy.elevationM(true, 33.0, 0.0), 1e-9);
        assertEquals(Isostasy.AIRY_FACTOR * 37_000.0, Isostasy.elevationM(true, 70.0, 0.0), 1e-6);
        assertEquals(-2_500.0, Isostasy.elevationM(false, 7.0, 0.0), 1e-9);
        assertEquals(-6_000.0, Isostasy.elevationM(false, 7.0, 100.0), 1e-9);
    }

    @Test
    void bothHalvesOfTibetStandNearFiveKilometres() {
        TectonicsConfig cfg = TectonicsConfig.defaults();
        // south: ~75 km crust, ~30% of the layer below 50 km eclogitized, cold thickened keel
        double southPull = 1000.0 * (cfg.keelDensityFactor() * 60.0 + cfg.eclogiteDensityFactor() * 0.3 * 25.0);
        double south = Isostasy.elevationM(true, 75.0, 0.0, southPull);
        // north: ~60 km crust over hot, thinned lithosphere
        double northPull = 1000.0 * cfg.keelDensityFactor() * cfg.hotKeelKm();
        double north = Isostasy.elevationM(true, 60.0, 0.0, northPull);
        assertTrue(south > 4_000 && south < 5_600, "south " + south);
        assertTrue(north > 4_000 && north < 5_600, "north " + north);
        // crust alone would put the south far too high and the north too low
        assertTrue(Isostasy.elevationM(true, 75.0, 0.0) > 6_000);
        assertTrue(Isostasy.elevationM(true, 60.0, 0.0) < 4_200);
    }

    @Test
    void crustalFlowConservesVolumeAndRespectsCeilingAndSlope() {
        PlateSimulation sim = twoPlates(Lithosphere.CONTINENTAL, 35f, 0f, Lithosphere.CONTINENTAL, 35f, 0f);
        Lithosphere L = sim.lith;
        Arrays.fill(L.owner, 0);
        sim.refreshStats();
        L.thick[8 * W + 8] = 400f;
        L.thick[8 * W + 9] = 200f;
        double before = totalCrust(L);
        CrustalFlow.relax(sim);

        assertEquals(before, totalCrust(L), 1e-2, "lateral flow moves crust, it does not make or destroy it");
        TectonicsConfig cfg = sim.cfg;
        double ceiling = Isostasy.DATUM_CRUST_KM + cfg.plateauElevationM() / (Isostasy.AIRY_FACTOR * 1000.0);
        double maxStep = cfg.crustSlopeKmPerKm() * 100.0;
        int[] nb = new int[4];
        for (int idx = 0; idx < L.n; idx++) {
            assertTrue(L.thick[idx] <= ceiling + 0.05, "cell above the plateau ceiling: " + L.thick[idx]);
            sim.grid.neighbors4(idx, nb);
            for (int k = 0; k < 4; k++) {
                if (L.thick[idx] > cfg.flowOnsetKm() + 0.05) {
                    assertTrue(L.thick[idx] - L.thick[nb[k]] <= maxStep + 0.05, "over-steep plateau margin");
                }
            }
        }
        assertTrue(L.thick[8 * W + 8] > 60f, "the collision zone stays a high plateau");
    }

    @Test
    void lowerCrustEclogitizesPartiallyAndRootsDelaminate() {
        PlateSimulation sim = twoPlates(Lithosphere.CONTINENTAL, 80f, 0f, Lithosphere.CONTINENTAL, 80f, 0f);
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        Arrays.fill(L.keel, 90f);
        double maxEclogite = cfg.eclogiteMaxFraction() * (80.0 - cfg.eclogiteDepthKm());

        boolean delaminated = false;
        for (int step = 0; step < 50 && !delaminated; step++) {
            sim.stepIndex = step;
            OrogenicRoots.update(sim, 10.0);
            delaminated = sim.events.counts().getOrDefault(GeoEvent.Type.DELAMINATION, 0) > 0;
            for (int idx = 0; idx < L.n; idx++) {
                assertTrue(L.eclogite[idx] <= maxEclogite + 1e-3, "at most the configured share eclogitizes");
            }
        }
        assertTrue(delaminated, "a thick, heavy root eventually drops off");
        GeoEvent event = sim.events.events().get(0);
        int cell = event.cell();
        assertEquals(0f, L.eclogite[cell], 1e-6f);
        assertTrue(L.keel[cell] <= cfg.hotKeelKm() + 1e-3, "hot asthenosphere replaces the keel");
        assertTrue(L.thick[cell] < 80f, "the eclogite left with the root");
        assertTrue((L.marks[cell] & Lithosphere.MARK_PLUME) != 0, "post-delamination magmatism");
        double pullAfter = Isostasy.rootPullM(sim, cell);
        assertTrue(pullAfter < 0.0, "the surface rebounds above what the crust alone holds");
    }

    // ------------------------------------------------------------------ whole runs

    @Test
    void sameSeedSameHistory() {
        PlateSimulation a = new PlateSimulation(CylinderGrid.forPlanet(40_000.0, 128), 77L, TectonicsConfig.defaults());
        PlateSimulation b = new PlateSimulation(CylinderGrid.forPlanet(40_000.0, 128), 77L, TectonicsConfig.defaults());
        PlateSimulation c = new PlateSimulation(CylinderGrid.forPlanet(40_000.0, 128), 78L, TectonicsConfig.defaults());
        for (int s = 0; s < 40; s++) {
            a.step(10.0);
            b.step(10.0);
            c.step(10.0);
        }
        assertArrayEquals(a.lith.owner, b.lith.owner);
        assertArrayEquals(a.lith.thick, b.lith.thick);
        assertArrayEquals(a.lith.keel, b.lith.keel);
        assertEquals(a.events.events(), b.events.events());
        assertFalse(Arrays.equals(a.lith.thick, c.lith.thick), "another seed gives another planet");
    }

    @Test
    void fullHistoryStaysPhysicallyPlausible() {
        PlateSimulation sim = new PlateSimulation(CylinderGrid.forPlanet(40_000.0, 256), 5L, TectonicsConfig.defaults());
        while (sim.timeMyr() < 4_500.0 - 1e-6) {
            sim.step(Math.min(10.0, 4_500.0 - sim.timeMyr()));
            Lithosphere L = sim.lith;
            int[] counted = new int[sim.plates.size()];
            for (int idx = 0; idx < L.n; idx++) {
                Plate p = sim.plates.get(L.owner[idx]);
                assertTrue(p.alive, "every cell belongs to a live plate");
                counted[p.id]++;
                assertTrue(L.thick[idx] > 0f && Float.isFinite(L.thick[idx]));
            }
            for (Plate p : sim.plates) {
                if (p.alive) {
                    assertEquals(counted[p.id], p.area, "plate area matches its cells");
                }
            }
        }
        PlateSimulation.StepStats s = sim.stats();
        FloatField e = Isostasy.elevationAboveSea(sim, sim.meanOceanDepthM);
        assertTrue(s.continentalFraction() > 0.15 && s.continentalFraction() < 0.6, "continents " + s.continentalFraction());
        assertTrue(s.rmsSpeedCmPerYr() > 1.0 && s.rmsSpeedCmPerYr() < 12.0, "speed " + s.rmsSpeedCmPerYr());
        assertTrue(e.max() <= sim.cfg.maxElevationM() + 1.0, "highest cell " + e.max());
        assertTrue(1.0 - e.fractionBelow(0.0) > 0.1, "some land above the sea");
    }

    @Test
    void growthCurveRunsFromZeroToToday() {
        PlateSimulation sim = new PlateSimulation(new CylinderGrid(16, 8, 2500.0), 1L, TectonicsConfig.defaults());
        assertEquals(0.0, sim.growthCurve(0.0), 1e-12);
        assertEquals(1.0, sim.growthCurve(4_500.0), 1e-12);
        double prev = -1.0;
        for (double t = 0; t <= 4_500; t += 100) {
            assertTrue(sim.growthCurve(t) > prev);
            prev = sim.growthCurve(t);
        }
        assertTrue(sim.growthCurve(1_500.0) > 0.6 && sim.growthCurve(1_500.0) < 0.8, "~70% by 3 Ga");
    }
}
