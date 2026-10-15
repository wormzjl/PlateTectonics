package com.platetectonics.geosim.surface;

import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.climate.ClimateModel;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.planet.PlanetSimulation;
import com.platetectonics.geosim.tectonics.Lithosphere;
import com.platetectonics.geosim.tectonics.PlateSimulation;
import com.platetectonics.geosim.tectonics.TectonicsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceProcessesTest {

    @Test
    void implicitStreamPowerSolvesItsEquation() {
        for (double n : new double[]{1.0, 2.0, 1.5}) {
            for (double c : new double[]{0.01, 1.0, 50.0}) {
                double d = 1_200.0;
                double y = SurfaceProcesses.solveDrop(d, c, n);
                assertTrue(y >= 0.0 && y <= d, "new height between base level and old height");
                assertEquals(d, y + c * Math.pow(y, n), 1e-3 * d, "n=" + n + " c=" + c);
            }
        }
    }

    /** Erosion and deposition only move crust; the sea adds pelagic and carbonate sediment from outside. */
    @Test
    void sedimentIsConserved() {
        CylinderGrid grid = CylinderGrid.forPlanet(40_000.0, 128);
        PlateSimulation plates = new PlateSimulation(grid, 3L, TectonicsConfig.defaults());
        plates.setMeanOceanDepthM(2_620.0);
        plates.setErosionStandIn(false);
        WorldHistory history = new WorldHistory(3L, WorldHistory.Preset.EARTH_LIKE, grid.size() * grid.cellAreaKm2(),
                plates.events());
        ClimateModel climateModel = new ClimateModel(grid);
        Climate climate = new Climate(grid);
        SurfaceProcesses surface = new SurfaceProcesses(grid, SurfaceConfig.defaults());
        float[] aboveSea = new float[grid.size()];
        double erodedTotal = 0.0;
        for (int step = 0; step < 40; step++) {
            double t = plates.timeMyr();
            GlobalState g = history.update(t + 10.0, 10.0, new WorldHistory.Inputs(3e6, 0, Double.NaN, 0.3, Double.NaN));
            plates.step(10.0);
            plates.refreshSeaLevel();
            for (int idx = 0; idx < aboveSea.length; idx++) {
                aboveSea[idx] = (float) (plates.elevationM(idx) - plates.seaLevelM());
            }
            climateModel.compute(g, aboveSea, null, climate);

            double before = crustKm3(plates);
            SurfaceProcesses.Budget b0 = surface.budget();
            surface.step(plates, climate, g, 10.0);
            SurfaceProcesses.Budget b1 = surface.budget();
            double eroded = b1.eroded() - b0.eroded();
            double deposited = b1.landDeposit() - b0.landDeposit() + b1.marineDeposit() - b0.marineDeposit();
            double added = b1.pelagic() - b0.pelagic() + b1.carbonate() - b0.carbonate();
            assertEquals(eroded, deposited, 1e-6 * Math.max(1.0, eroded), "every eroded km3 is deposited somewhere");
            assertEquals(before + added, crustKm3(plates), 2e-6 * before, "crust only changes by sea-floor sediment");
            erodedTotal += eroded;
        }
        assertTrue(erodedTotal > 0.0, "the land eroded");
    }

    @Test
    void planetRunIsDeterministic() {
        CylinderGrid grid = CylinderGrid.forPlanet(40_000.0, 128);
        PlanetSimulation a = new PlanetSimulation(grid, 11L, TectonicsConfig.defaults(), SurfaceConfig.defaults(),
                WorldHistory.Preset.RANDOM, 2_620.0, false);
        PlanetSimulation b = new PlanetSimulation(grid, 11L, TectonicsConfig.defaults(), SurfaceConfig.defaults(),
                WorldHistory.Preset.RANDOM, 2_620.0, false);
        for (int step = 0; step < 30; step++) {
            a.step(10.0);
            b.step(10.0);
        }
        assertArrayEquals(a.elevationAboveSeaM(), b.elevationAboveSeaM());
        assertArrayEquals(a.climate().precipitationMm, b.climate().precipitationMm);
        assertEquals(a.surface().budget(), b.surface().budget());
        assertEquals(a.state().globalTempC(), b.state().globalTempC());
    }

    private static double crustKm3(PlateSimulation sim) {
        Lithosphere lith = sim.lithosphere();
        double sum = 0.0;
        for (int idx = 0; idx < sim.grid().size(); idx++) {
            sum += lith.thicknessKm(idx);
        }
        return sum * sim.grid().cellAreaKm2();
    }
}
