package com.platetectonics.geosim.planet;

import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.surface.SurfaceConfig;
import com.platetectonics.geosim.tectonics.Lithosphere;
import com.platetectonics.geosim.tectonics.TectonicsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetSimulationTest {

    /** D-5: the thread count must not change the planet, not even in the last bit. */
    @Test
    void threadCountDoesNotChangeThePlanet() {
        // 256 x 128 cells: enough for several chunks per loop, so the threads really split the work
        CylinderGrid grid = CylinderGrid.forPlanet(40_000.0, 256);
        try (Parallel five = Parallel.of(5)) {
            PlanetSimulation a = planet(grid);
            PlanetSimulation b = planet(grid);
            b.setParallel(five);
            for (int step = 0; step < 30; step++) {
                a.step(10.0);
                b.step(10.0);
            }
            assertArrayEquals(a.elevationAboveSeaM(), b.elevationAboveSeaM());
            assertArrayEquals(a.climate().temperatureC, b.climate().temperatureC);
            assertArrayEquals(a.climate().precipitationMm, b.climate().precipitationMm);
            assertArrayEquals(a.climate().iceM, b.climate().iceM);
            assertArrayEquals(a.surface().erosionMPerMyr, b.surface().erosionMPerMyr);
            assertArrayEquals(a.surface().depositM, b.surface().depositM);
            assertArrayEquals(column(a, Kind.SEDIMENT), column(b, Kind.SEDIMENT));
            assertArrayEquals(column(a, Kind.REGOLITH), column(b, Kind.REGOLITH));
            assertArrayEquals(column(a, Kind.OCEAN_AGE), column(b, Kind.OCEAN_AGE));
            assertEquals(a.surface().budget(), b.surface().budget());
            assertEquals(a.plates().crustBudget(), b.plates().crustBudget());
            assertEquals(a.climate().landWeathering, b.climate().landWeathering);
            assertEquals(a.state().globalTempC(), b.state().globalTempC());
            assertEquals(a.state().pCO2ppm(), b.state().pCO2ppm());
            assertEquals(a.state().oxygenPal(), b.state().oxygenPal());
            assertArrayEquals(a.state().milestoneTimes(), b.state().milestoneTimes());
        }
    }

    /**
     * Sediment is part of a column's crust, so no process may leave more sediment than crust (isostasy would
     * read the difference as negative crystalline crust and raise a lone pillar), and no basin may pile up
     * more than a few tens of km of it.
     */
    @Test
    void sedimentStaysPartOfAPhysicalColumn() {
        CylinderGrid grid = CylinderGrid.forPlanet(40_000.0, 256);
        PlanetSimulation sim = planet(grid);
        for (int step = 0; step < 150; step++) {
            sim.step(10.0);
        }
        Lithosphere lith = sim.plates().lithosphere();
        float maxSediment = 0f;
        for (int idx = 0; idx < grid.size(); idx++) {
            float sediment = lith.sedimentKm(idx);
            assertTrue(sediment <= lith.thicknessKm(idx) + 1e-3f,
                    "cell " + idx + ": " + sediment + " km sediment in " + lith.thicknessKm(idx) + " km of crust");
            maxSediment = Math.max(maxSediment, sediment);
        }
        assertTrue(maxSediment < 60f, "thickest sediment " + maxSediment + " km");
    }

    private static PlanetSimulation planet(CylinderGrid grid) {
        return new PlanetSimulation(grid, 7L, TectonicsConfig.defaults(), SurfaceConfig.defaults(),
                WorldHistory.Preset.RANDOM, 2_620.0, false);
    }

    private enum Kind { SEDIMENT, REGOLITH, OCEAN_AGE }

    private static float[] column(PlanetSimulation sim, Kind kind) {
        Lithosphere lith = sim.plates().lithosphere();
        float[] out = new float[sim.grid().size()];
        for (int idx = 0; idx < out.length; idx++) {
            out[idx] = switch (kind) {
                case SEDIMENT -> lith.sedimentKm(idx);
                case REGOLITH -> lith.regolithM(idx);
                case OCEAN_AGE -> lith.oceanAgeMyr(idx);
            };
        }
        return out;
    }
}
