package com.platetectonics.geosim.cli;

import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.climate.ClimateConfig;
import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.Milestone;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.io.BlockPreview;
import com.platetectonics.geosim.io.GifWriter;
import com.platetectonics.geosim.io.MapImage;
import com.platetectonics.geosim.io.SurfaceMaps;
import com.platetectonics.geosim.io.TectonicMaps;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.planet.PlanetSimulation;
import com.platetectonics.geosim.surface.SurfaceConfig;
import com.platetectonics.geosim.surface.SurfaceProcesses;
import com.platetectonics.geosim.tectonics.PlateSimulation;
import com.platetectonics.geosim.tectonics.SeaLevelDrivers;
import com.platetectonics.geosim.tectonics.TectonicsConfig;
import com.platetectonics.geosim.terrain.SeaLevel;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.MasterWorldIO;
import com.platetectonics.geosim.world.PlanetSpec;
import com.platetectonics.geosim.world.TerrainSampler;

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
        double oceanDepth = SeaLevel.DEFAULT_OCEAN_DEPTH_M;
        double age = 4500;
        double frameEvery = 25;
        int gifWidth = 720;
        int gifDelayMs = 80;
        boolean gifPanels = false;
        boolean seaDrivers = false;
        double reportEvery = 250;
        boolean platesOnly = false;
        int threads = Runtime.getRuntime().availableProcessors();
        boolean saveMaster = false;
        java.util.List<String> blockViews = new java.util.ArrayList<>();
        Path fromMaster = null;
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
                case "--gif-delay" -> gifDelayMs = Integer.parseInt(args[++k]);
                case "--gif-panels" -> gifPanels = true;
                case "--sea-drivers" -> seaDrivers = true;
                case "--report-every" -> reportEvery = Double.parseDouble(args[++k]);
                case "--plates-only" -> platesOnly = true;
                case "--threads" -> threads = Integer.parseInt(args[++k]);
                case "--master" -> saveMaster = true;
                case "--block-view" -> blockViews.add(args[++k]);
                case "--from-master" -> fromMaster = Path.of(args[++k]);
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
                            + "--ocean-depth M --threads N [--master] [--block-view X,Z[,PIXELS[,BLOCKS_PER_PIXEL]] ...] --out DIR");
                    System.out.println("  --frame-every 0 skips the timeline GIF; --gif-delay MS sets its speed (80 ms per frame)");
                    System.out.println("  --gif-panels draws every map in each frame (--gif-width is then the width of one map)");
                    System.out.println("  --sea-drivers writes sea_drivers.csv: each step, the part of sea level due to ice, ocean-floor age"
                            + " and sediment, and the land share at fixed levels");
                    System.out.println("  --master writes master.ptw, the file a Minecraft world loads");
                    System.out.println("  --block-view renders the terrain as chunk generation builds it, centred on block X,Z");
                    System.out.println("  --threads defaults to all cores; every count gives the same planet");
                    return;
                }
                default -> throw new IllegalArgumentException("unknown argument: " + args[k]);
            }
        }

        PlanetSpec planet = PlanetSpec.earth(scale);
        if (fromMaster != null) {
            // block views of a saved planet, no simulation
            Files.createDirectories(out);
            renderBlockViews(new TerrainSampler(MasterWorldIO.read(fromMaster), planet), blockViews, out);
            System.out.println("-> " + out.toAbsolutePath());
            return;
        }
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
        ClimateConfig climateCfg = ClimateConfig.defaults();
        for (String o : overrides) {
            String[] kv = o.split("=", 2);
            if (hasComponent(tectonics, kv[0])) {
                tectonics = withValue(tectonics, kv[0], Double.parseDouble(kv[1]));
            } else if (hasComponent(surfaceCfg, kv[0])) {
                surfaceCfg = withValue(surfaceCfg, kv[0], Double.parseDouble(kv[1]));
            } else if (hasComponent(climateCfg, kv[0])) {
                climateCfg = withValue(climateCfg, kv[0], Double.parseDouble(kv[1]));
            } else {
                throw new IllegalArgumentException("--set: no constant named " + kv[0]);
            }
            System.out.println("set " + kv[0] + " = " + kv[1]);
        }
        PlanetSimulation sim = new PlanetSimulation(grid, seed, tectonics, surfaceCfg, climateCfg, preset, oceanDepth,
                platesOnly);
        Parallel parallel = Parallel.of(threads);
        sim.setParallel(parallel);
        long tInit = System.nanoTime();

        GifWriter gif = frameEvery > 0 ? new GifWriter(dir.resolve("timeline.gif"), gifDelayMs) : null;
        PanelFrames panels = gif != null && gifPanels && !platesOnly ? new PanelFrames(gifWidth, age, seed) : null;
        long renderNanos = 0;
        double nextFrame = 0;
        double nextReport = 0;
        int steps = 0;
        boolean turnsMarked = false;
        try (PrintWriter history = new PrintWriter(Files.newBufferedWriter(dir.resolve("history.csv")));
             PrintWriter drivers = seaDrivers && !platesOnly
                     ? new PrintWriter(Files.newBufferedWriter(dir.resolve("sea_drivers.csv"))) : null) {
            if (drivers != null) {
                StringBuilder head = new StringBuilder("time_myr,sea_level_m,ice_m,age_m,sediment_m,ice_water_m,ocean_fraction,"
                        + "continental_fraction,mean_ocean_age_myr,mean_ocean_sediment_km,land_fraction,land_pts_per_100m,"
                        + "median_continent_m,mean_land_m,map_ice_water_m,sea_level_old_depth_m,sea_level_map_ice_m,"
                        + "sea_level_old_both_m,land_from_sea_fraction");
                for (double top : BAND_TOPS_M) {
                    head.append(String.format(Locale.ROOT, ",land_share_below_%.0f,erosion_below_%.0f,sediment_km_below_%.0f,"
                            + "oceanic_share_below_%.0f", top, top, top, top));
                }
                for (double level : SeaLevelDrivers.LEVELS_M) {
                    head.append(String.format(Locale.ROOT, ",land_above_%.0f", level));
                }
                drivers.println(head);
            }
            history.println("time_myr,luminosity,mantle_tp_c,degassing,temp_c,pco2_ppm,o2_pal,organic_burial,"
                    + "methane_c,snowball,land_fraction,ice_fraction,land_weathering,erosion_m_per_myr,sea_level_m");
            System.out.println("    Myr plates cont%  land%  cm/yr  Gkm3  max m |  T(C)  O2(PAL)    CO2  ice%  E(m/Myr)");
            while (true) {
                double now = sim.timeMyr();
                if (gif != null && now >= nextFrame - 1e-6) {
                    long r0 = System.nanoTime();
                    if (panels == null) {
                        gif.write(TectonicMaps.frame(frame(sim), gifWidth, caption(sim)));
                    } else if (now > 0.0) {
                        gif.write(panels.render(sim)); // the climate and surface maps exist from the first step on
                    }
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
                if (!turnsMarked && now >= age - 500.0 - 1e-6) {
                    sim.plates().markTurns();
                    turnsMarked = true;
                }
                sim.step(Math.min(PlanetSimulation.eraStepMyr(now, age), age - now));
                steps++;
                logHistory(sim, history);
                if (drivers != null) {
                    logDrivers(sim, drivers);
                }
                if (panels != null) {
                    panels.record(sim);
                }
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
            if (saveMaster || !blockViews.isEmpty()) {
                MasterWorld master = MasterWorld.capture(sim, new MasterWorld.Settings(seed, width, age, preset, oceanDepth));
                if (saveMaster) {
                    MasterWorldIO.write(master, dir.resolve("master.ptw"));
                }
                renderBlockViews(new TerrainSampler(master, planet), blockViews, dir);
            }
        }
        writeEvents(plates, dir.resolve("events.csv"));
        long tEnd = System.nanoTime();

        PlateSimulation.CrustBudget b = plates.crustBudget();
        System.out.printf(Locale.ROOT,
                "crust budget (10^9 km3): arcs +%.2f, shelves +%.2f, basin closure +%.2f, rotation rounding +%.3f, erosion -%.2f, "
                        + "collision -%.2f, eclogite -%.2f, subducted arc/plateau -%.2f, subduction erosion -%.2f%n",
                b.arcs(), b.shelves(), b.closure(), b.rounding(), b.erosion(), b.collision(), b.eclogite(), b.subducted(),
                b.forearc());
        if (!platesOnly) {
            SurfaceProcesses.Budget sb = sim.surface().budget();
            System.out.printf(Locale.ROOT,
                    "sediment budget (10^9 km3): eroded %.2f -> land %.2f, sea %.2f (delta plains %.2f) | pelagic %.2f, carbonate %.2f%n",
                    sb.eroded() / 1e9, sb.landDeposit() / 1e9, sb.marineDeposit() / 1e9, sb.deltaPlain() / 1e9,
                    sb.pelagic() / 1e9, sb.carbonate() / 1e9);
            double[] parts = sim.surface().marinePartsKm3();
            System.out.printf(Locale.ROOT, "sea by part (10^9 km3): shelf %.2f, fan %.2f, delta top %.2f, drape %.2f, "
                            + "shelf beyond the fan %.2f, enclosed seas %.2f, delta plain %.2f%n",
                    parts[0] / 1e9, parts[1] / 1e9, parts[2] / 1e9, parts[3] / 1e9, parts[4] / 1e9, parts[5] / 1e9,
                    parts[6] / 1e9);
            printTimeline(sim);
        }
        printHypsometry(sim);
        printSpin(sim);
        if (!platesOnly) {
            printSeaFloor(sim);
            printBiomes(sim);
        }
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

    private static void renderBlockViews(TerrainSampler sampler, java.util.List<String> views, Path dir) throws java.io.IOException {
        for (String view : views) {
            String[] v = view.split(",");
            int bx = Integer.parseInt(v[0].trim());
            int bz = Integer.parseInt(v[1].trim());
            int px = v.length > 2 ? Integer.parseInt(v[2].trim()) : 1024;
            int step = v.length > 3 ? Integer.parseInt(v[3].trim()) : 4;
            MapImage.write(BlockPreview.render(sampler, bx, bz, px, step),
                    dir.resolve("blocks_" + bx + "_" + bz + "_x" + step + ".png"));
        }
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

    /** Upper edges of the land height bands in sea_drivers.csv, m above sea level (the last band is open). */
    private static final double[] BAND_TOPS_M = {50, 200, 500, 1_000, 1e9};

    /** One row of sea_drivers.csv (measurement only; see {@link SeaLevelDrivers}). */
    private static void logDrivers(PlanetSimulation sim, PrintWriter w) {
        // the ice's water counted at the map's own area (before D-43), to compare with the sphere-weighted budget
        float[] iceM = sim.climate().iceM;
        double mapIce = 0.0;
        for (float v : iceM) {
            mapIce += v * 0.917;
        }
        mapIce /= iceM.length;
        SeaLevelDrivers.Sample d = SeaLevelDrivers.measure(sim.plates(), mapIce);
        StringBuilder row = new StringBuilder(String.format(Locale.ROOT,
                "%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.4f,%.4f,%.1f,%.3f,%.4f,%.2f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f", sim.timeMyr(),
                d.seaLevelM(), d.iceM(), d.ageM(), d.sedimentM(), d.iceWaterM(), d.oceanFraction(),
                d.continentalFraction(), d.meanOceanAgeMyr(), d.meanOceanSedimentKm(), d.landFraction(), d.landPer100M(),
                d.medianContinentM(), d.meanLandM(), mapIce, d.oldDepthLevelM(), d.otherIceLevelM(), d.bothLevelM()));
        row.append(String.format(Locale.ROOT, ",%.4f", (double) sim.surface().landFromSeaCells() / iceM.length));
        // land by height above the sea: share of the land and mean erosion in each band
        float[] above = sim.elevationAboveSeaM();
        float[] erosion = sim.surface().erosionMPerMyr;
        int bands = BAND_TOPS_M.length;
        long[] count = new long[bands];
        double[] sum = new double[bands];
        double[] sediment = new double[bands];
        long[] oceanic = new long[bands];
        var lith = sim.plates().lithosphere();
        long land = 0;
        for (int idx = 0; idx < above.length; idx++) {
            if (above[idx] <= 0f) {
                continue;
            }
            land++;
            int b = 0;
            while (b < bands - 1 && above[idx] > BAND_TOPS_M[b]) {
                b++;
            }
            count[b]++;
            sum[b] += erosion[idx];
            sediment[b] += lith.sedimentKm(idx);
            if (!lith.isContinental(idx)) {
                oceanic[b]++;
            }
        }
        for (int b = 0; b < bands; b++) {
            row.append(String.format(Locale.ROOT, ",%.4f,%.2f,%.3f,%.4f", land == 0 ? 0.0 : (double) count[b] / land,
                    count[b] == 0 ? 0.0 : sum[b] / count[b], count[b] == 0 ? 0.0 : sediment[b] / count[b],
                    count[b] == 0 ? 0.0 : (double) oceanic[b] / count[b]));
        }
        for (double f : d.landAtLevel()) {
            row.append(String.format(Locale.ROOT, ",%.4f", f));
        }
        w.println(row);
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
        double ageSum = 0.0;
        int oceanCells = 0;
        int old = 0;
        for (int idx = 0; idx < e.length; idx++) {
            if (!lith.isContinental(idx)) {
                ageSum += lith.oceanAgeMyr(idx);
                oceanCells++;
                if (lith.oceanAgeMyr(idx) > 200f) {
                    old++;
                }
            }
        }
        System.out.printf(Locale.ROOT, "ocean floor: mean age %.0f Myr, %.0f%% older than 200 Myr (Earth ~64 Myr, ~0%%) | "
                        + "enclosed in one plate: %.1f%% of the surface%n",
                ageSum / Math.max(1, oceanCells), 100.0 * old / Math.max(1, oceanCells),
                100.0 * sim.plates().enclosedOceanCells() / e.length);
    }

    /**
     * The sea floor against Earth: ocean area by depth, shelf width, sediment piles, land built from the sea,
     * sea level and the water that ocean-floor sediment displaces (docs/design/04-surface-processes.md#sea-floor).
     */
    /** Plate spin and turn (D-42), boundary types and coast orientations. */
    private static void printSpin(PlanetSimulation sim) {
        PlateSimulation plates = sim.plates();
        int n = sim.grid().size();
        double[] sum = new double[2];
        double[] max = new double[2];
        int[] count = new int[2];
        java.util.List<com.platetectonics.geosim.tectonics.Plate> alive = new java.util.ArrayList<>();
        for (var p : plates.plates()) {
            if (!p.alive()) {
                continue;
            }
            alive.add(p);
            int c = p.area() >= 0.05 * n ? 0 : 1;
            double s = Math.abs(p.spinDegPerMyr());
            sum[c] += s;
            max[c] = Math.max(max[c], s);
            count[c]++;
        }
        System.out.printf(Locale.ROOT, "spin (deg/Myr): %d plates over 5%% of the surface, mean %.2f (max %.2f); %d smaller, "
                        + "mean %.2f (max %.2f) | RMS speed %.2f cm/yr, of which spin alone %.2f%n",
                count[0], count[0] > 0 ? sum[0] / count[0] : 0.0, max[0], count[1], count[1] > 0 ? sum[1] / count[1] : 0.0,
                max[1], plates.stats().rmsSpeedCmPerYr(), plates.spinRmsCmPerYr());
        alive.sort((a, b) -> Integer.compare(b.continentalArea(), a.continentalArea()));
        long cont = 0;
        for (var p : alive) {
            cont += p.continentalArea();
        }
        StringBuilder turns = new StringBuilder();
        for (int k = 0; k < Math.min(3, alive.size()); k++) {
            var p = alive.get(k);
            turns.append(String.format(Locale.ROOT, "%s%.0f%% of the continent %+.0f deg", k == 0 ? "" : ", ",
                    100.0 * p.continentalArea() / Math.max(1, cont), p.turnSinceMarkDeg()));
        }
        System.out.println("turn over the last 500 Myr (clockwise +), plates with the most continent: " + turns
                + String.format(Locale.ROOT, " | rounding holes filled: %d cells (%.1f%% of the new ocean floor) | %d substeps",
                plates.roundingHoleCells(), 100.0 * plates.roundingHoleCells() / Math.max(1L, plates.totalNewOceanCells()),
                plates.totalSubsteps()));
        double[] b = plates.boundaryShares();
        System.out.printf(Locale.ROOT, "plate boundaries: converging %.0f%%, diverging %.0f%%, transform %.0f%% (by local motion)%n",
                100 * b[0], 100 * b[1], 100 * b[2]);
        // coast orientation: Sobel gradient of the land mask at land cells next to the sea
        float[] e = sim.elevationAboveSeaM();
        int w = sim.grid().width();
        int h = sim.grid().height();
        long[] bins = new long[4];
        for (int j = 1; j < h - 1; j++) {
            for (int i = 0; i < w; i++) {
                int idx = j * w + i;
                if (e[idx] < 0f) {
                    continue;
                }
                int ie = (i + 1) % w;
                int iw = (i + w - 1) % w;
                if (e[j * w + ie] >= 0f && e[j * w + iw] >= 0f && e[idx - w] >= 0f && e[idx + w] >= 0f) {
                    continue;
                }
                double gx = land(e, j - 1, ie, w) + 2 * land(e, j, ie, w) + land(e, j + 1, ie, w)
                        - land(e, j - 1, iw, w) - 2 * land(e, j, iw, w) - land(e, j + 1, iw, w);
                double gy = land(e, j + 1, iw, w) + 2 * land(e, j + 1, i, w) + land(e, j + 1, ie, w)
                        - land(e, j - 1, iw, w) - 2 * land(e, j - 1, i, w) - land(e, j - 1, ie, w);
                if (gx == 0 && gy == 0) {
                    continue;
                }
                // the coast runs at right angles to the gradient; bins of 45 degrees centred on N-S, NE-SW, E-W, NW-SE
                double a = Math.toDegrees(Math.atan2(-gy, gx)); // y up
                int bin = (int) Math.floor(Math.floorMod((int) Math.round(a + 22.5), 180) / 45.0);
                bins[bin]++;
            }
        }
        double t = Math.max(1L, bins[0] + bins[1] + bins[2] + bins[3]);
        System.out.printf(Locale.ROOT, "coast orientation: N-S %.0f%%, NW-SE %.0f%%, E-W %.0f%%, NE-SW %.0f%% (25%% each if none is preferred)%n",
                100 * bins[0] / t, 100 * bins[1] / t, 100 * bins[2] / t, 100 * bins[3] / t);
    }

    private static int land(float[] e, int j, int i, int w) {
        return e[j * w + i] >= 0f ? 1 : 0;
    }

    private static void printSeaFloor(PlanetSimulation sim) {
        float[] e = sim.elevationAboveSeaM();
        var flow = sim.surface().flow();
        var lith = sim.plates().lithosphere();
        int n = e.length;
        int w = sim.grid().width();
        int h = sim.grid().height();
        double cellKm = sim.grid().cellKm();
        boolean[] ocean = new boolean[n];
        int oceanCells = 0;
        int[] byDepth = new int[3];
        int shallowOnContinents = 0;
        int land = 0;
        double displaced = 0.0;
        for (int idx = 0; idx < n; idx++) {
            if (e[idx] >= 0f) {
                land++;
            }
            ocean[idx] = e[idx] < 0f && flow.isOcean(idx);
            if (ocean[idx]) {
                oceanCells++;
                byDepth[-e[idx] <= 200f ? 0 : -e[idx] <= 3_000f ? 1 : 2]++;
                if (-e[idx] <= 200f && lith.isContinental(idx)) {
                    shallowOnContinents++;
                }
                if (!lith.isContinental(idx)) {
                    displaced += com.platetectonics.geosim.tectonics.Isostasy.SEDIMENT_MARINE_FACTOR * lith.sedimentKm(idx) * 1000.0;
                }
            }
        }
        // shelf width: from each coastal sea cell through the sea to the nearest water deeper than 200 m
        int[] dist = new int[n];
        int[] queue = new int[n];
        java.util.Arrays.fill(dist, -1);
        int tail = 0;
        for (int idx = 0; idx < n; idx++) {
            if (ocean[idx] && -e[idx] > 200f) {
                dist[idx] = 0;
                queue[tail++] = idx;
            }
        }
        for (int head = 0; head < tail; head++) {
            int c = queue[head];
            int i = c % w;
            int j = c / w;
            int[] nbs = {j * w + (i + 1) % w, j * w + (i + w - 1) % w, j > 0 ? c - w : -1, j < h - 1 ? c + w : -1};
            for (int nb : nbs) {
                if (nb >= 0 && ocean[nb] && dist[nb] < 0) {
                    dist[nb] = dist[c] + 1;
                    queue[tail++] = nb;
                }
            }
        }
        int[] bins = new int[6]; // < 1 cell, <= 100, <= 200, <= 400, > 400 km, no deep water
        java.util.List<Double> widths = new java.util.ArrayList<>();
        for (int idx = 0; idx < n; idx++) {
            if (!ocean[idx] || !touchesLand(ocean, e, idx, w, h)) {
                continue;
            }
            if (dist[idx] < 0) {
                bins[5]++;
                continue;
            }
            double km = dist[idx] * cellKm;
            widths.add(km);
            bins[dist[idx] == 0 ? 0 : km <= 100.0 ? 1 : km <= 200.0 ? 2 : km <= 400.0 ? 3 : 4]++;
        }
        int coast = 0;
        for (int b : bins) {
            coast += b;
        }
        java.util.Collections.sort(widths);
        double mean = widths.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double median = widths.isEmpty() ? 0.0 : widths.get(widths.size() / 2);
        float maxOcean = 0f;
        float maxCont = 0f;
        for (int idx = 0; idx < n; idx++) {
            if (lith.isContinental(idx)) {
                maxCont = Math.max(maxCont, lith.sedimentKm(idx));
            } else {
                maxOcean = Math.max(maxOcean, lith.sedimentKm(idx));
            }
        }
        double o = Math.max(1, oceanCells);
        System.out.printf(Locale.ROOT, "ocean by depth: 0-200 m %.1f%% (%.0f%% of it on continental crust), 200-3,000 m %.1f%%, "
                        + ">3,000 m %.1f%% (Earth ~7.5, 15, 77)%n",
                100.0 * byDepth[0] / o, 100.0 * shallowOnContinents / Math.max(1, byDepth[0]), 100.0 * byDepth[1] / o,
                100.0 * byDepth[2] / o);
        double c = Math.max(1, coast);
        System.out.printf(Locale.ROOT, "shelf width from the coast to 200 m: mean %.0f km, median %.0f km | <1 cell %.0f%%, "
                        + "<=100 km %.0f%%, 100-200 %.0f%%, 200-400 %.0f%%, >400 %.0f%%, no deep water %.0f%% (Earth mean ~80 km)%n",
                mean, median, 100.0 * bins[0] / c, 100.0 * bins[1] / c, 100.0 * bins[2] / c, 100.0 * bins[3] / c,
                100.0 * bins[4] / c, 100.0 * bins[5] / c);
        System.out.printf(Locale.ROOT, "thickest sediment: %.1f km on ocean floor, %.1f km on continents (Earth <= ~20 km) | "
                        + "land built from the sea last step: %.0f km2%n",
                maxOcean, maxCont, sim.surface().landFromSeaCells() * sim.grid().cellAreaKm2());
        System.out.printf(Locale.ROOT, "sea level %.0f m on the datum | land %.1f%% of the surface (Earth 29%%) | "
                        + "ocean-floor sediment displaces %.0f m of the mean ocean depth%n",
                sim.plates().seaLevelM(), 100.0 * land / n, displaced / o);
        // water for 29% land on this surface as it stands (the planet would answer by eroding and building
        // differently, so it is a first guess): fill to the level with 29% of the cells above it
        float[] sorted = e.clone();
        java.util.Arrays.sort(sorted);
        double level = sorted[(int) (0.71 * n)];
        double now = 0.0;
        double needed = 0.0;
        for (float v : e) {
            now += Math.max(0.0, -v);
            needed += Math.max(0.0, level - v);
        }
        System.out.printf(Locale.ROOT, "ocean water: mean depth %.0f m now | 29%% land would take about %.0f m "
                + "(sea level %+.0f m)%n", now / n, needed / n, level);
    }

    private static boolean touchesLand(boolean[] ocean, float[] e, int idx, int w, int h) {
        int i = idx % w;
        int j = idx / w;
        int[] nbs = {j * w + (i + 1) % w, j * w + (i + w - 1) % w, j > 0 ? idx - w : -1, j < h - 1 ? idx + w : -1};
        for (int nb : nbs) {
            if (nb >= 0 && !ocean[nb] && e[nb] >= 0f) {
                return true;
            }
        }
        return false;
    }

    /** Biome groups and aridity classes over land, for comparison with Earth. */
    private static void printBiomes(PlanetSimulation sim) {
        float[] e = sim.elevationAboveSeaM();
        Climate c = sim.climate();
        int[] counts = new int[Biome.values().length];
        int land = 0;
        int iceFree = 0;
        int[] arid = new int[5];
        for (int idx = 0; idx < e.length; idx++) {
            if (e[idx] <= 0f) {
                continue;
            }
            land++;
            Biome b = c.biome(idx, e[idx]);
            counts[b.ordinal()]++;
            if (b != Biome.ICE_SHEET) {
                iceFree++;
                double ai = c.aridity[idx];
                arid[ai < 0.05 ? 0 : ai < 0.2 ? 1 : ai < 0.5 ? 2 : ai < 0.65 ? 3 : 4]++;
            }
        }
        if (land == 0) {
            return;
        }
        double ice = counts[Biome.ICE_SHEET.ordinal()];
        double desert = counts[Biome.HOT_DESERT.ordinal()] + counts[Biome.COLD_DESERT.ordinal()]
                + counts[Biome.POLAR_DESERT.ordinal()];
        double tundra = counts[Biome.TUNDRA.ordinal()];
        double open = counts[Biome.COLD_STEPPE.ordinal()] + counts[Biome.TEMPERATE_GRASSLAND.ordinal()]
                + counts[Biome.SAVANNA.ordinal()];
        double forest = counts[Biome.BOREAL_FOREST.ordinal()] + counts[Biome.TEMPERATE_FOREST.ordinal()]
                + counts[Biome.TEMPERATE_RAINFOREST.ordinal()] + counts[Biome.TROPICAL_SEASONAL_FOREST.ordinal()]
                + counts[Biome.TROPICAL_RAINFOREST.ordinal()];
        double rain = counts[Biome.TROPICAL_RAINFOREST.ordinal()];
        System.out.printf(Locale.ROOT, "biomes (%% of land): ice %.0f, desert %.0f, tundra %.0f, steppe/grass/savanna %.0f, "
                        + "forest %.0f (tropical rainforest %.1f) | Earth ~10, ~20, ~8, ~27, ~31 (~7)%n",
                100 * ice / land, 100 * desert / land, 100 * tundra / land, 100 * open / land, 100 * forest / land,
                100 * rain / land);
        System.out.printf(Locale.ROOT, "aridity (%% of ice-free land): hyperarid %.0f, arid %.0f, semi-arid %.0f, "
                        + "dry subhumid %.0f, humid %.0f | Earth ~8, 12, 18, 10, 52%n",
                100.0 * arid[0] / Math.max(1, iceFree), 100.0 * arid[1] / Math.max(1, iceFree),
                100.0 * arid[2] / Math.max(1, iceFree), 100.0 * arid[3] / Math.max(1, iceFree),
                100.0 * arid[4] / Math.max(1, iceFree));
        double[] bandRain = new double[9];
        double[] temp = new double[9];
        int[] cells = new int[9];
        CylinderGrid g = sim.grid();
        for (int idx = 0; idx < e.length; idx++) {
            if (e[idx] > 0f) {
                int band = Math.min(8, (int) (Math.abs(g.latitudeDeg(g.jOf(idx))) / 10.0));
                bandRain[band] += c.precipitationMm[idx];
                temp[band] += c.temperatureC[idx];
                cells[band]++;
            }
        }
        StringBuilder sb = new StringBuilder("land rain mm/yr | T C by |latitude| band:");
        for (int b = 0; b < 9; b++) {
            sb.append(String.format(Locale.ROOT, " %d0s %.0f|%.0f", b, bandRain[b] / Math.max(1, cells[b]), temp[b] / Math.max(1, cells[b])));
        }
        System.out.println(sb + "  (Earth rain ~2000, 1200, 650, 550, 600, 550, 400, 250, 150)");
        System.out.printf(Locale.ROOT, "land weathering %.2f x Earth (0.29 x cells x earthLandWeight); %.1f%% of the surface is land%n",
                c.landWeathering, 100.0 * land / e.length);
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
