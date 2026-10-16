package com.platetectonics.geosim.planet;

import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.climate.ClimateModel;
import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.events.EventLog;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.surface.SurfaceConfig;
import com.platetectonics.geosim.surface.SurfaceProcesses;
import com.platetectonics.geosim.tectonics.PlateSimulation;
import com.platetectonics.geosim.tectonics.TectonicsConfig;

import java.util.List;

/**
 * The whole planet, one step at a time (docs/design/02-simulation-pipeline.md): global box models, plates,
 * sea level, climate, then rivers, erosion and deposition. Each part reads what the others produced in
 * the step before, so the coupling loops close: weathering sets the temperature, ice lowers the sea,
 * erosion feeds nutrients to life, life sets oxygen and how fast land erodes.
 */
public final class PlanetSimulation {
    /** Mean rock erosion over land on Earth today, m/Myr (~0.05 mm/yr). */
    private static final double EARTH_DENUDATION_M_PER_MYR = 50.0;

    private final CylinderGrid grid;
    private final PlateSimulation plates;
    private final WorldHistory history;
    private final ClimateModel climateModel;
    private final Climate climate;
    private final SurfaceProcesses surface;
    private final boolean platesOnly;
    private final float[] aboveSea;
    private GlobalState state;
    private int lastPlumes;
    private boolean climateReady;
    private long platesNanos;
    private long climateNanos;
    private long surfaceNanos;

    /**
     * @param platesOnly run the Phase 2 plate engine alone, with its erosion stand-in and no climate
     */
    public PlanetSimulation(CylinderGrid grid, long seed, TectonicsConfig tectonics, SurfaceConfig surfaceCfg,
                            WorldHistory.Preset preset, double meanOceanDepthM, boolean platesOnly) {
        this.grid = grid;
        this.platesOnly = platesOnly;
        plates = new PlateSimulation(grid, seed, tectonics);
        plates.setMeanOceanDepthM(meanOceanDepthM);
        plates.setErosionStandIn(platesOnly);
        history = new WorldHistory(seed, preset, grid.size() * grid.cellAreaKm2(), plates.events());
        climateModel = new ClimateModel(grid);
        climate = new Climate(grid);
        surface = platesOnly ? null : new SurfaceProcesses(grid, surfaceCfg);
        aboveSea = new float[grid.size()];
        state = history.state();
        updateAboveSea();
    }

    /** Advances everything by {@code dtMyr}. */
    public void step(double dtMyr) {
        double t = plates.timeMyr();
        PlateSimulation.StepStats last = plates.stats();
        double newOcean = last.newOceanCells() * grid.cellAreaKm2() / dtMyr;
        state = history.update(t + dtMyr, dtMyr, new WorldHistory.Inputs(newOcean, lastPlumes,
                climateReady ? climate.landWeathering : Double.NaN,
                climateReady ? climate.landFraction : landFraction(),
                surface != null && climateReady ? surface.meanLandErosionMPerMyr() / EARTH_DENUDATION_M_PER_MYR : Double.NaN));

        EventLog events = plates.events();
        int before = events.events().size();
        long t0 = System.nanoTime();
        plates.step(dtMyr);
        lastPlumes = countPlumes(events.events(), before);
        if (platesOnly) {
            updateAboveSea();
            platesNanos += System.nanoTime() - t0;
            return;
        }
        plates.refreshSeaLevel();
        updateAboveSea();
        long t1 = System.nanoTime();
        climateModel.compute(state, aboveSea, climateReady ? surface.erosionMPerMyr : null, climate);
        long t2 = System.nanoTime();
        surface.step(plates, climate, state, dtMyr);
        plates.setIceWaterEquivalentM(climate.iceWaterEquivalentM);
        plates.refreshSeaLevel();
        updateAboveSea();
        long t3 = System.nanoTime();
        platesNanos += t1 - t0;
        climateNanos += t2 - t1;
        surfaceNanos += t3 - t2;
        climateReady = true;
    }

    /**
     * Step length for a run to {@code ageMyr}: 10 Myr until 1 Gyr before the end, 5 Myr until 300 Myr
     * before, then 2 Myr, so the recent past that shapes today's landscape is resolved best.
     */
    public static double eraStepMyr(double nowMyr, double ageMyr) {
        double remaining = ageMyr - nowMyr;
        if (remaining > 1000) {
            return 10;
        }
        if (remaining > 300) {
            return 5;
        }
        return 2;
    }

    /** Wall time spent so far in plates, climate and surface processes, s. */
    public double[] phaseSeconds() {
        return new double[]{platesNanos / 1e9, climateNanos / 1e9, surfaceNanos / 1e9};
    }

    private static int countPlumes(List<GeoEvent> events, int from) {
        int plumes = 0;
        for (int k = from; k < events.size(); k++) {
            if (events.get(k).type() == GeoEvent.Type.PLUME) {
                plumes++;
            }
        }
        return plumes;
    }

    private void updateAboveSea() {
        double sea = plates.seaLevelM();
        plates.parallel().cells(aboveSea.length, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                aboveSea[idx] = (float) (plates.elevationM(idx) - sea);
            }
        });
    }

    /**
     * Worker threads for every per-cell loop of plates, climate and surface. Loops that depend on order
     * stay on one thread, so the planet is the same bit for bit for any count (tested).
     */
    public void setParallel(Parallel parallel) {
        plates.setParallel(parallel);
        climateModel.setParallel(parallel);
        if (surface != null) {
            surface.setParallel(parallel);
        }
    }

    private double landFraction() {
        int land = 0;
        for (float e : aboveSea) {
            if (e > 0f) {
                land++;
            }
        }
        return (double) land / aboveSea.length;
    }

    // ------------------------------------------------------------------ read access

    public CylinderGrid grid() {
        return grid;
    }

    public PlateSimulation plates() {
        return plates;
    }

    public WorldHistory history() {
        return history;
    }

    public GlobalState state() {
        return state;
    }

    /** Climate of the last step (empty in plates-only runs). */
    public Climate climate() {
        return climate;
    }

    /** Rivers, erosion and deposition (null in plates-only runs). */
    public SurfaceProcesses surface() {
        return surface;
    }

    /** Elevation relative to today's sea level, m, after the last step. */
    public float[] elevationAboveSeaM() {
        return aboveSea;
    }

    public double timeMyr() {
        return plates.timeMyr();
    }

    public boolean platesOnly() {
        return platesOnly;
    }
}
