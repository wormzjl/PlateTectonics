package com.platetectonics.geosim.cli;

import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.io.GifWriter;
import com.platetectonics.geosim.io.MapImage;
import com.platetectonics.geosim.io.TectonicMaps;
import com.platetectonics.geosim.tectonics.Isostasy;
import com.platetectonics.geosim.tectonics.PlateSimulation;
import com.platetectonics.geosim.tectonics.TectonicsConfig;
import com.platetectonics.geosim.terrain.SeaLevel;
import com.platetectonics.geosim.world.PlanetSpec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 2 CLI: runs the plate simulation from formation to the chosen planet age, then writes the final
 * maps (elevation, plates, crust age, crust type), a timeline GIF and the event log.
 * Usage: {@code --seed N --width W | --cell-km K --age MYR --frame-every MYR --gif-width PX --out DIR}
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
                case "--out" -> out = Path.of(args[++k]);
                case "--help", "-h" -> {
                    System.out.println("geosim-cli --seed N [--width W | --cell-km K] --age MYR --frame-every MYR "
                            + "--gif-width PX --report-every MYR --scale S --ocean-depth M --out DIR");
                    System.out.println("  --frame-every 0 skips the timeline GIF");
                    return;
                }
                default -> throw new IllegalArgumentException("unknown argument: " + args[k]);
            }
        }

        PlanetSpec planet = PlanetSpec.earth(scale);
        CylinderGrid grid = CylinderGrid.forPlanet(planet.circumferenceKm(), width);
        Path dir = out.resolve("seed_" + seed);
        Files.createDirectories(dir);

        System.out.printf(Locale.ROOT, "geosim %s | seed %d | %.0f Myr%n", GeoSim.SIM_VERSION, seed, age);
        System.out.printf(Locale.ROOT, "grid: %d x %d cells of %.3f km (%.0f blocks)%n",
                grid.width(), grid.height(), grid.cellKm(), grid.cellKm() * 1000.0 / planet.metersPerBlock());

        long t0 = System.nanoTime();
        PlateSimulation sim = new PlateSimulation(grid, seed, TectonicsConfig.defaults());
        sim.setMeanOceanDepthM(oceanDepth);
        long tInit = System.nanoTime();

        GifWriter gif = frameEvery > 0 ? new GifWriter(dir.resolve("timeline.gif"), 80) : null;
        long renderNanos = 0;
        double nextFrame = 0;
        double nextReport = 0;
        int steps = 0;
        System.out.println("    Myr plates cont%  land%  cm/yr subduct collide newOcean sub  Gkm3  keel  max m");
        while (true) {
            double now = sim.timeMyr();
            if (gif != null && now >= nextFrame - 1e-6) {
                long r0 = System.nanoTime();
                FloatField e = Isostasy.elevationAboveSea(sim, oceanDepth);
                gif.write(TectonicMaps.frame(TectonicMaps.elevation(sim, e, true), gifWidth,
                        String.format(Locale.ROOT, "%.0f Myr", now)));
                renderNanos += System.nanoTime() - r0;
                nextFrame += frameEvery;
            }
            if (now >= nextReport - 1e-6 || now >= age - 1e-6) {
                report(sim, oceanDepth);
                nextReport += reportEvery;
            }
            if (now >= age - 1e-6) {
                break;
            }
            sim.step(Math.min(eraStep(now, age), age - now));
            steps++;
        }
        if (gif != null) {
            gif.close();
        }
        long tSim = System.nanoTime();

        FloatField elevation = Isostasy.elevationAboveSea(sim, oceanDepth);
        MapImage.write(TectonicMaps.elevation(sim, elevation, false), dir.resolve("elevation.png"));
        MapImage.write(TectonicMaps.elevation(sim, elevation, true), dir.resolve("elevation_plates.png"));
        MapImage.write(TectonicMaps.plates(sim), dir.resolve("plates.png"));
        MapImage.write(TectonicMaps.crustAge(sim), dir.resolve("crust_age.png"));
        MapImage.write(TectonicMaps.crustType(sim), dir.resolve("crust_type.png"));
        writeEvents(sim, dir.resolve("events.csv"));
        long tEnd = System.nanoTime();

        PlateSimulation.CrustBudget b = sim.crustBudget();
        System.out.printf(Locale.ROOT,
                "crust budget (10^9 km3): arcs +%.2f, shelves +%.2f, erosion -%.2f, collision -%.2f, eclogite -%.2f, "
                        + "subducted arc/plateau -%.2f%n",
                b.arcs(), b.shelves(), b.erosion(), b.collision(), b.eclogite(), b.subducted());
        if (sim.flowBudgetExhausted()) {
            System.out.println("warning: crustal flow hit its work budget in at least one step");
        }
        System.out.print("events:");
        for (Map.Entry<GeoEvent.Type, Integer> e : sim.events().counts().entrySet()) {
            System.out.printf(Locale.ROOT, " %s=%d", e.getKey(), e.getValue());
        }
        System.out.println();
        double simMs = (tSim - tInit - renderNanos) / 1e6;
        System.out.printf(Locale.ROOT, "init %.0f ms | %d steps in %.1f s (%.1f ms/step) | frames %.1f s | maps %.1f s%n",
                (tInit - t0) / 1e6, steps, simMs / 1000.0, simMs / steps, renderNanos / 1e9, (tEnd - tSim) / 1e9);
        System.out.println("-> " + dir.toAbsolutePath());
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

    private static void report(PlateSimulation sim, double oceanDepth) {
        PlateSimulation.StepStats s = sim.stats();
        FloatField e = Isostasy.elevationAboveSea(sim, oceanDepth);
        double land = 1.0 - e.fractionBelow(0.0);
        System.out.printf(Locale.ROOT, "%7.0f %6d %5.1f %6.1f %6.2f %7d %7d %8d %3d %5.2f %5.1f %6.0f%n",
                s.timeMyr(), s.plates(), 100 * s.continentalFraction(), 100 * land, s.rmsSpeedCmPerYr(),
                s.subductedCells(), s.collisionCells(), s.newOceanCells(), s.substeps(),
                s.continentalVolumeGkm3(), s.maxKeelKm(), e.max());
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
