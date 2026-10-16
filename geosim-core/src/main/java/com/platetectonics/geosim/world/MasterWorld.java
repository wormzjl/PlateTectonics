package com.platetectonics.geosim.world;

import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.planet.PlanetSimulation;
import com.platetectonics.geosim.surface.SurfaceConfig;
import com.platetectonics.geosim.surface.SurfaceProcesses;
import com.platetectonics.geosim.tectonics.Lithosphere;
import com.platetectonics.geosim.tectonics.TectonicsConfig;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * The present-day planet that a Minecraft world is built from (docs/design/02-simulation-pipeline.md#output-masterworld-file):
 * every field chunk generation and the map preview read, one value per master cell, row-major. Saved with
 * the world by {@link MasterWorldIO}; chunk generation never runs the simulation.
 */
public final class MasterWorld {
    /**
     * How a planet is made. Same settings and simulation version, same planet (D-5).
     *
     * @param seed            planet seed
     * @param width           master grid cells around the planet (height = width / 2)
     * @param ageMyr          elapsed time since formation
     * @param preset          random or Earth-like history
     * @param meanOceanDepthM ocean water as a mean depth over the planet
     */
    public record Settings(long seed, int width, double ageMyr, WorldHistory.Preset preset, double meanOceanDepthM) {
        public static Settings defaults(long seed) {
            return new Settings(seed, 1_280, 4_500.0, WorldHistory.Preset.RANDOM, 2_620.0);
        }

        public double cellKm() {
            return PlanetSpec.EARTH_CIRCUMFERENCE_KM / width;
        }

        /** Stable text form, stored in the file to detect a world saved with other settings. */
        public String describe() {
            return String.format(Locale.ROOT, "seed=%d width=%d age=%.1f preset=%s ocean=%.1f", seed, width, ageMyr,
                    preset.name(), meanOceanDepthM);
        }
    }

    public final Settings settings;
    public final String simVersion;
    public final CylinderGrid grid;
    /** Elevation relative to sea level, m. */
    public final float[] elevationM;
    public final float[] temperatureC;
    public final float[] seasonalRangeC;
    public final float[] precipitationMm;
    public final float[] aridity;
    /** Ice-sheet thickness, m. */
    public final float[] iceM;
    /** River discharge, km3/yr. */
    public final float[] dischargeKm3PerYr;
    /** Rock erosion rate of the last step, m/Myr. */
    public final float[] erosionMPerMyr;
    /** 0 = none, 1 = lake that spills, 2 = endorheic lake. */
    public final byte[] lakes;
    /** {@link Biome} ordinal. */
    public final byte[] biome;
    /** 0 = oceanic, 1 = continental crust. */
    public final byte[] crustType;
    public final float[] crustKm;
    public final float[] sedimentKm;
    public final float[] regolithM;
    public final float[] exhumedKm;
    /** When the crust formed, Myr since planet formation. */
    public final float[] crustFormedMyr;
    public final int[] plate;

    MasterWorld(Settings settings, String simVersion, CylinderGrid grid) {
        this.settings = settings;
        this.simVersion = simVersion;
        this.grid = grid;
        int n = grid.size();
        elevationM = new float[n];
        temperatureC = new float[n];
        seasonalRangeC = new float[n];
        precipitationMm = new float[n];
        aridity = new float[n];
        iceM = new float[n];
        dischargeKm3PerYr = new float[n];
        erosionMPerMyr = new float[n];
        lakes = new byte[n];
        biome = new byte[n];
        crustType = new byte[n];
        crustKm = new float[n];
        sedimentKm = new float[n];
        regolithM = new float[n];
        exhumedKm = new float[n];
        crustFormedMyr = new float[n];
        plate = new int[n];
    }

    /** Named fields in file order (see {@link MasterWorldIO}). */
    List<Object[]> fields() {
        return List.of(
                new Object[]{"elevation_m", elevationM},
                new Object[]{"temperature_c", temperatureC},
                new Object[]{"seasonal_range_c", seasonalRangeC},
                new Object[]{"precipitation_mm", precipitationMm},
                new Object[]{"aridity", aridity},
                new Object[]{"ice_m", iceM},
                new Object[]{"discharge_km3_yr", dischargeKm3PerYr},
                new Object[]{"erosion_m_myr", erosionMPerMyr},
                new Object[]{"lakes", lakes},
                new Object[]{"biome", biome},
                new Object[]{"crust_type", crustType},
                new Object[]{"crust_km", crustKm},
                new Object[]{"sediment_km", sedimentKm},
                new Object[]{"regolith_m", regolithM},
                new Object[]{"exhumed_km", exhumedKm},
                new Object[]{"crust_formed_myr", crustFormedMyr},
                new Object[]{"plate", plate});
    }

    public Biome biomeAt(int idx) {
        return Biome.values()[biome[idx]];
    }

    /**
     * Runs the whole history (plates, climate, rivers) and returns today's planet.
     *
     * @param progress receives the finished share 0..1 after every step (may be null)
     */
    public static MasterWorld simulate(Settings s, Parallel parallel, DoubleConsumer progress) {
        CylinderGrid grid = CylinderGrid.forPlanet(PlanetSpec.EARTH_CIRCUMFERENCE_KM, s.width());
        PlanetSimulation sim = new PlanetSimulation(grid, s.seed(), TectonicsConfig.defaults(), SurfaceConfig.defaults(),
                s.preset(), s.meanOceanDepthM(), false);
        sim.setParallel(parallel);
        while (sim.timeMyr() < s.ageMyr() - 1e-6) {
            double now = sim.timeMyr();
            sim.step(Math.min(PlanetSimulation.eraStepMyr(now, s.ageMyr()), s.ageMyr() - now));
            if (progress != null) {
                progress.accept(sim.timeMyr() / s.ageMyr());
            }
        }
        return capture(sim, s);
    }

    /** Copies today's fields out of a finished simulation. */
    public static MasterWorld capture(PlanetSimulation sim, Settings s) {
        CylinderGrid grid = sim.grid();
        MasterWorld m = new MasterWorld(s, GeoSim.SIM_VERSION, grid);
        Climate c = sim.climate();
        SurfaceProcesses surface = sim.surface();
        Lithosphere lith = sim.plates().lithosphere();
        float[] e = sim.elevationAboveSeaM();
        float[] discharge = surface.dischargeKm3PerYr();
        for (int idx = 0; idx < grid.size(); idx++) {
            m.elevationM[idx] = e[idx];
            m.temperatureC[idx] = c.temperatureC[idx];
            m.seasonalRangeC[idx] = c.seasonalRangeC[idx];
            m.precipitationMm[idx] = c.precipitationMm[idx];
            m.aridity[idx] = c.aridity[idx];
            m.iceM[idx] = c.iceM[idx];
            m.dischargeKm3PerYr[idx] = discharge[idx];
            m.erosionMPerMyr[idx] = surface.erosionMPerMyr[idx];
            m.lakes[idx] = surface.lakes[idx];
            m.biome[idx] = (byte) c.biome(idx, e[idx]).ordinal();
            m.crustType[idx] = (byte) (lith.isContinental(idx) ? 1 : 0);
            m.crustKm[idx] = lith.thicknessKm(idx);
            m.sedimentKm[idx] = lith.sedimentKm(idx);
            m.regolithM[idx] = lith.regolithM(idx);
            m.exhumedKm[idx] = lith.exhumedKm(idx);
            m.crustFormedMyr[idx] = lith.formedAtMyr(idx);
            m.plate[idx] = lith.owner(idx);
        }
        return m;
    }
}
