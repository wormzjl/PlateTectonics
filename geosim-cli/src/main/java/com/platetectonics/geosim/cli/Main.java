package com.platetectonics.geosim.cli;

import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.Milestone;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.io.GifWriter;
import com.platetectonics.geosim.io.MapImage;
import com.platetectonics.geosim.io.SurfaceMaps;
import com.platetectonics.geosim.io.TectonicMaps;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.planet.PlanetSimulation;
import com.platetectonics.geosim.surface.SurfaceConfig;
import com.platetectonics.geosim.surface.SurfaceProcesses;
import com.platetectonics.geosim.tectonics.PlateSimulation;
import com.platetectonics.geosim.tectonics.TectonicsConfig;
import com.platetectonics.geosim.terrain.SeaLevel;
import com.platetectonics.geosim.world.PlanetSpec;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 3 CLI: runs the planet from formation to the chosen age (plates, global history, climate, rivers,
 * erosion and deposition), then writes the final maps, a timeline GIF, the event log and the global
 * history. {@code --plates-only} runs the Phase 2 plate engine alone.
 * Usage: {@code --seed N --width W | --cell-km K --age MYR --history random|earth --frame-every MYR
 * --gif-width PX --out DIR}
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        long seed = 42;
        int width = 1024;
        double scale = PlanetSpec.DEFAULT_SCALE;
        double oceanDepth = SeaLevel.EARTH_MEAN_OCEAN_DEPTH_M;
        double age = 4500;
        double frameEvery = 25;
        int gifWidth = 720;
        double reportEvery = 250;
        boolean platesOnly = false;
        int threads = Runtime.getRuntime().availableProcessors();
        WorldHistory.Preset preset = WorldHistory.Preset.RANDOM;
        java.util.List<String> overrides = new java.util.ArrayList<>();
        Path out = Path.of("maps");
        for (int k = 0; k < args.length; k++) {
            switch (args[k]) {
                case "--seed" -> seed = Long.parseLong(args[++k]);
                case "--width" -> width = Integer.parseInt(args[++k]);
                case "--cell-km" -> width = (int) Math.round(PlanetSpec.EARTH_CIRCUMFERENCE_KM / Double.parseDouble(args[++k]));
                case "--scale" -> scale = Double.parseDouble(args[++k]);
                case "--ocean-depth" -> oceanDepth = Double.parseDouble(args[++k]);
                case "--age" -> age = Double.parseDouble(args[++k]);
                case "--frame-every" -> frameEvery = Double.parseDouble(args[++k]);
                case "--gif-width" -> gifWidth = Integer.parseInt(args[++k]);
                case "--report-every" -> reportEvery = Double.parseDouble(args[++k]);
                case "--plates-only" -> platesOnly = true;
                case "--threads" -> threads = Integer.parseInt(args[++k]);
                case "--history" -> preset = switch (args[++k]) {
                    case "random" -> WorldHistory.Preset.RANDOM;
                    case "earth" -> WorldHistory.Preset.EARTH_LIKE;
                    default -> throw new IllegalArgumentException("--history random|earth");
                };
                case "--set" -> overrides.add(args[++k]);
                case "--out" -> out = Path.of(args[++k]);
                case "--help", "-h" -> {
                    System.out.println("geosim-cli --seed N [--width W | --cell-km K] --age MYR [--history random|earth] "
                            + "[--plates-only] [--set CONSTANT=VALUE ...] --frame-every MYR --gif-width PX --report-every MYR --scale S "
                            + "--ocean-depth M --threads N --out DIR");
                    System.out.println("  --frame-every 0 skips the timeline GIF");
                    System.out.println("  --threads defaults to all cores; every count gives the same planet");
                    return;
                }
                default -> throw new IllegalArgumentException("unknown argument: " + args[k]);
            }
        }

        PlanetSpec planet = PlanetSpec.earth(scale);
        CylinderGrid grid = CylinderGrid.forPlanet(planet.circumferenceKm(), width);
        Path dir = out.resolve("seed_" + seed);
        Files.createDirectories(dir);

        System.out.printf(Locale.ROOT, "geosim %s | seed %d | %.0f Myr | %s%n", GeoSim.SIM_VERSION, seed, age,
                platesOnly ? "plates only" : "history " + preset.name().toLowerCase(Locale.ROOT));
        System.out.printf(Locale.ROOT, "grid: %d x %d cells of %.3f km (%.0f blocks)%n",
                grid.width(), grid.height(), grid.cellKm(), grid.cellKm() * 1000.0 / planet.metersPerBlock());

        long t0 = System.nanoTime();
        TectonicsConfig tectonics = TectonicsConfig.defaults();
        SurfaceConfig surfaceCfg = SurfaceConfig.defaults();
        for (String o : overrides) {
            String[] kv = o.split("=", 2);
            if (hasComponent(tectonics, kv[0])) {
                tectonics = withValue(tectonics, kv[0], Double.parseDouble(kv[1]));
            } else if (hasComponent(surfaceCfg, kv[0])) {
                surfaceCfg = withValue(surfaceCfg, kv[0], Double.parseDouble(kv[1]));
            } else {
                throw new IllegalArgumentException("--set: no constant named " + kv[0]);
            }
            System.out.println("set " + kv[0] + " = " + kv[1]);
        }
        PlanetSimulation sim = new PlanetSimulation(grid, seed, tectonics, surfaceCfg, preset, oceanDepth, platesOnly);
        Parallel parallel = Parallel.of(threads);
        sim.setParallel(parallel);
        long tInit = System.nanoTime();

        GifWriter gif = frameEvery > 0 ? new GifWriter(dir.resolve("timeline.gif"), 80) : null;
        long renderNanos = 0;
        double nextFrame = 0;
        double nextReport = 0;
        int steps = 0;
        try (PrintWriter history = new PrintWriter(Files.newBufferedWriter(dir.resolve("history.csv")))) {
            history.println("time_myr,luminosity,mantle_tp_c,degassing,temp_c,pco2_ppm,o2_pal,organic_burial,"
                    + "methane_c,snowball,land_fraction,ice_fraction,land_weathering,erosion_m_per_myr,sea_level_m");
            System.out.println("    Myr plates cont%  land%  cm/yr  Gkm3  max m |  T(C)  O2(PAL)    CO2  ice%  E(m/Myr)");
            while (true) {
                double now = sim.timeMyr();
                if (gif != null && now >= nextFrame - 1e-6) {
                    long r0 = System.nanoTime();
                    gif.write(TectonicMaps.frame(frame(sim), gifWidth, caption(sim)));
                    renderNanos += System.nanoTime() - r0;
                    nextFrame += frameEvery;
                }
                if (now >= nextReport - 1e-6 || now >= age - 1e-6) {
                    report(sim);
                    nextReport += reportEvery;
                }
                if (now >= age - 1e-6) {
                    break;
                }
                sim.step(Math.min(eraStep(now, age), age - now));
                steps++;
                logHistory(sim, history);
            }
        }
        if (gif != null) {
            gif.close();
        }
        long tSim = System.nanoTime();

        PlateSimulation plates = sim.plates();
        FloatField elevation = new FloatField(grid);
        System.arraycopy(sim.elevationAboveSeaM(), 0, elevation.data(), 0, grid.size());
        MapImage.write(TectonicMaps.elevation(plates, elevation, false), dir.resolve("elevation.png"));
        MapImage.write(TectonicMaps.elevation(plates, elevation, true), dir.resolve("elevation_plates.png"));
        MapImage.write(TectonicMaps.plates(plates), dir.resolve("plates.png"));
        MapImage.write(TectonicMaps.crustAge(plates), dir.resolve("crust_age.png"));
        MapImage.write(TectonicMaps.crustType(plates), dir.resolve("crust_type.png"));
        if (!platesOnly) {
            Climate c = sim.climate();
            float[] e = sim.elevationAboveSeaM();
            SurfaceProcesses s = sim.surface();
            MapImage.write(SurfaceMaps.temperature(c, e), dir.resolve("temperature.png"));
            MapImage.write(SurfaceMaps.precipitation(c, e), dir.resolve("precipitation.png"));
            MapImage.write(SurfaceMaps.biome(c, e), dir.resolve("biome.png"));
            MapImage.write(SurfaceMaps.rivers(c, s, e), dir.resolve("rivers.png"));
            MapImage.write(SurfaceMaps.ice(c, e), dir.resolve("ice.png"));
            MapImage.write(SurfaceMaps.erosion(s, grid, e), dir.resolve("erosion.png"));
            MapImage.write(SurfaceMaps.sediment(plates.lithosphere(), e), dir.resolve("sediment.png"));
            MapImage.write(SurfaceMaps.regolith(plates.lithosphere(), e), dir.resolve("regolith.png"));
        }
        writeEvents(plates, dir.resolve("events.csv"));
        long tEnd = System.nanoTime();

        PlateSimulation.CrustBudget b = plates.crustBudget();
        System.out.printf(Locale.ROOT,
                "crust budget (10^9 km3): arcs +%.2f, shelves +%.2f, erosion -%.2f, collision -%.2f, eclogite -%.2f, "
                        + "subducted arc/plateau -%.2f, subduction erosion -%.2f%n",
                b.arcs(), b.shelves(), b.erosion(), b.collision(), b.eclogite(), b.subducted(), b.forearc());
        if (!platesOnly) {
            SurfaceProcesses.Budget sb = sim.surface().budget();
            System.out.printf(Locale.ROOT,
                    "sediment budget (10^9 km3): eroded %.2f -> land %.2f, sea %.2f (delta plains %.2f) | pelagic %.2f, carbonate %.2f%n",
                    sb.eroded() / 1e9, sb.landDeposit() / 1e9, sb.marineDeposit() / 1e9, sb.deltaPlain() / 1e9,
                    sb.pelagic() / 1e9, sb.carbonate() / 1e9);
            printTimeline(sim);
        }
        printHypsometry(sim);
        if (plates.flowBudgetExhausted()) {
            System.out.println("warning: crustal flow hit its work budget in at least one step");
        }
        System.out.print("events:");
        for (Map.Entry<GeoEvent.Type, Integer> e : plates.events().counts().entrySet()) {
            System.out.printf(Locale.ROOT, " %s=%d", e.getKey(), e.getValue());
        }
        System.out.println();
        double simMs = (tSim - tInit - renderNanos) / 1e6;
        System.out.printf(Locale.ROOT, "init %.0f ms | %d steps in %.1f s (%.1f ms/step, %d threads) | frames %.1f s | maps %.1f s%n",
                (tInit - t0) / 1e6, steps, simMs / 1000.0, simMs / steps, parallel.threads(), renderNanos / 1e9,
                (tEnd - tSim) / 1e9);
        parallel.close();
        double[] phases = sim.phaseSeconds();
        System.out.printf(Locale.ROOT, "time split: plates %.1f s | climate %.1f s | surface %.1f s%n", phases[0], phases[1], phases[2]);
        System.out.println("-> " + dir.toAbsolutePath());
    }

    private static boolean hasComponent(Record r, String name) {
        for (var c : r.getClass().getRecordComponents()) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** A copy of config record {@code r} with one numeric constant replaced (for tuning runs). */
    @SuppressWarnings("unchecked")
    private static <R extends Record> R withValue(R r, String name, double value) throws Exception {
        var components = r.getClass().getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        for (int k = 0; k < components.length; k++) {
            types[k] = components[k].getType();
            values[k] = components[k].getAccessor().invoke(r);
            if (components[k].getName().equals(name)) {
                values[k] = types[k] == int.class ? (Object) (int) value : (Object) value;
            }
        }
        return (R) r.getClass().getDeclaredConstructor(types).newInstance(values);
    }

    /** Era time step (docs/design/02-simulation-pipeline.md): coarse in deep time, fine near the present. */
    static double eraStep(double now, double age) {
        double remaining = age - now;
        if (remaining > 1000) {
            return 10;
        }
        if (remaining > 300) {
            return 5;
        }
        return 2;
    }

    private static BufferedImage frame(PlanetSimulation sim) {
        if (sim.platesOnly() || sim.timeMyr() <= 0.0) {
            FloatField e = new FloatField(sim.grid());
            System.arraycopy(sim.elevationAboveSeaM(), 0, e.data(), 0, e.data().length);
            return TectonicMaps.elevation(sim.plates(), e, true);
        }
        return SurfaceMaps.reliefWithIce(sim.climate(), sim.elevationAboveSeaM());
    }

    private static String caption(PlanetSimulation sim) {
        GlobalState g = sim.state();
        if (sim.platesOnly()) {
            return String.format(Locale.ROOT, "%.0f Myr", sim.timeMyr());
        }
        return String.format(Locale.ROOT, "%.0f Myr  %.0f C  O2 %s%s", sim.timeMyr(), g.globalTempC(), oxygen(g.oxygenPal()),
                g.snowball() ? "  SNOWBALL" : "");
    }

    private static String oxygen(double pal) {
        return pal < 1e-4 ? "none" : pal < 0.01 ? String.format(Locale.ROOT, "%.1e", pal) : String.format(Locale.ROOT, "%.2f", pal);
    }

    private static void report(PlanetSimulation sim) {
        PlateSimulation.StepStats s = sim.plates().stats();
        float[] e = sim.elevationAboveSeaM();
        int land = 0;
        float max = -1e9f;
        for (float v : e) {
            if (v > 0f) {
                land++;
            }
            max = Math.max(max, v);
        }
        System.out.printf(Locale.ROOT, "%7.0f %6d %5.1f %6.1f %6.2f %5.2f %6.0f", s.timeMyr(), s.plates(),
                100 * s.continentalFraction(), 100.0 * land / e.length, s.rmsSpeedCmPerYr(), s.continentalVolumeGkm3(), max);
        if (sim.platesOnly() || sim.timeMyr() <= 0.0) {
            System.out.println();
            return;
        }
        GlobalState g = sim.state();
        System.out.printf(Locale.ROOT, " | %5.1f %8s %6.0f %5.1f %9.1f%s%n", g.globalTempC(), oxygen(g.oxygenPal()),
                g.pCO2ppm(), 100 * sim.climate().iceFraction, sim.surface().meanLandErosionMPerMyr(),
                g.snowball() ? "  snowball" : "");
    }

    private static void logHistory(PlanetSimulation sim, PrintWriter w) {
        GlobalState g = sim.state();
        Climate c = sim.climate();
        boolean full = !sim.platesOnly();
        w.printf(Locale.ROOT, "%.1f,%.4f,%.1f,%.3f,%.2f,%.0f,%.3e,%.3f,%.1f,%d,%.4f,%.4f,%.3f,%.1f,%.1f%n",
                g.timeMyr(), g.luminosity(), g.mantleTpC(), g.degassing(), g.globalTempC(), g.pCO2ppm(), g.oxygenPal(),
                g.organicBurial(), g.methaneGreenhouseC(), g.snowball() ? 1 : 0,
                full ? c.landFraction : Double.NaN, full ? c.iceFraction : Double.NaN, full ? c.landWeathering : Double.NaN,
                full ? sim.surface().meanLandErosionMPerMyr() : Double.NaN, sim.plates().seaLevelM());
    }

    private static void printTimeline(PlanetSimulation sim) {
        StringBuilder sb = new StringBuilder("history:");
        for (GeoEvent e : sim.plates().events().events()) {
            switch (e.type()) {
                case MILESTONE -> sb.append(String.format(Locale.ROOT, " %s@%.0f",
                        Milestone.values()[(int) e.value()].name().toLowerCase(Locale.ROOT), e.timeMyr()));
                case OXYGENATION -> sb.append(String.format(Locale.ROOT, " O2->%s@%.0f",
                        GlobalState.OxygenState.values()[(int) e.value()], e.timeMyr()));
                case SNOWBALL_START -> sb.append(String.format(Locale.ROOT, " SNOWBALL(%.0f Myr)@%.0f", e.value(), e.timeMyr()));
                default -> {
                }
            }
        }
        System.out.println(sb);
    }

    /** Elevation percentiles of continental crust (Earth: ~30% under water as shelves and slopes, land median ~400 m). */
    private static void printHypsometry(PlanetSimulation sim) {
        float[] e = sim.elevationAboveSeaM();
        float[] cont = new float[e.length];
        int n = 0;
        for (int idx = 0; idx < e.length; idx++) {
            if (sim.plates().lithosphere().isContinental(idx)) {
                cont[n++] = e[idx];
            }
        }
        float[] sorted = java.util.Arrays.copyOf(cont, n);
        java.util.Arrays.sort(sorted);
        int flooded = 0;
        while (flooded < n && sorted[flooded] <= 0f) {
            flooded++;
        }
        if (n == 0) {
            return;
        }
        System.out.printf(Locale.ROOT, "continental hypsometry: %.0f%% flooded | p10 %.0f  p25 %.0f  p50 %.0f  p75 %.0f  p90 %.0f m%n",
                100.0 * flooded / n, sorted[n / 10], sorted[n / 4], sorted[n / 2], sorted[3 * n / 4], sorted[9 * n / 10]);
        var lith = sim.plates().lithosphere();
        double contThick = 0;
        double contSed = 0;
        double oceanSed = 0;
        int arcLand = 0;
        int sedimentLand = 0;
        for (int idx = 0; idx < e.length; idx++) {
            if (lith.isContinental(idx)) {
                contThick += lith.thicknessKm(idx);
                contSed += lith.sedimentKm(idx);
            } else {
                oceanSed += lith.sedimentKm(idx);
                if (e[idx] > 0f) {
                    if (lith.sedimentKm(idx) > 0.5 * (lith.thicknessKm(idx) - 7.0)) {
                        sedimentLand++;
                    } else {
                        arcLand++;
                    }
                }
            }
        }
        double a = sim.grid().cellAreaKm2() / 1e9;
        System.out.printf(Locale.ROOT, "continental crust: mean %.1f km thick, %.2f Gkm3 of which sediment %.2f | ocean-floor sediment %.2f Gkm3%n",
                contThick / n, contThick * a, contSed * a, oceanSed * a);
        System.out.printf(Locale.ROOT, "land on oceanic crust: arcs and plateaus %.1f%%, sediment piles %.1f%% of the surface%n",
                100.0 * arcLand / e.length, 100.0 * sedimentLand / e.length);
    }

    private static void writeEvents(PlateSimulation sim, Path file) throws Exception {
        CylinderGrid g = sim.grid();
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
            w.println("type,time_myr,i,j,plate_a,plate_b,value");
            for (GeoEvent e : sim.events().events()) {
                boolean located = e.cell() >= 0;
                w.printf(Locale.ROOT, "%s,%.1f,%d,%d,%d,%d,%.3f%n", e.type(), e.timeMyr(),
                        located ? g.iOf(e.cell()) : -1, located ? g.jOf(e.cell()) : -1,
                        e.plateA(), e.plateB(), e.value());
            }
        }
    }
}
