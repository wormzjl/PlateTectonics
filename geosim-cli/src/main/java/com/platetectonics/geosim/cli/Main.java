package com.platetectonics.geosim.cli;

import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.io.MapImage;
import com.platetectonics.geosim.terrain.ProtoTerrain;
import com.platetectonics.geosim.terrain.SeaLevel;
import com.platetectonics.geosim.world.PlanetSpec;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Phase 1 CLI: generates the placeholder proto-terrain, fills the oceans by volume and writes a PNG.
 * Usage: {@code --seed N --width W --scale S --ocean-depth M --out DIR}
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        long seed = 42;
        int width = 1024;
        double scale = PlanetSpec.DEFAULT_SCALE;
        double oceanDepth = SeaLevel.EARTH_MEAN_OCEAN_DEPTH_M;
        Path out = Path.of("maps");
        for (int k = 0; k < args.length; k++) {
            switch (args[k]) {
                case "--seed" -> seed = Long.parseLong(args[++k]);
                case "--width" -> width = Integer.parseInt(args[++k]);
                case "--scale" -> scale = Double.parseDouble(args[++k]);
                case "--ocean-depth" -> oceanDepth = Double.parseDouble(args[++k]);
                case "--out" -> out = Path.of(args[++k]);
                case "--help", "-h" -> {
                    System.out.println("geosim-cli --seed N --width W --scale S --ocean-depth M --out DIR");
                    return;
                }
                default -> throw new IllegalArgumentException("unknown argument: " + args[k]);
            }
        }

        PlanetSpec planet = PlanetSpec.earth(scale);
        CylinderGrid grid = CylinderGrid.forPlanet(planet.circumferenceKm(), width);
        long t0 = System.nanoTime();
        FloatField elevation = ProtoTerrain.generate(grid, seed);
        double seaLevel = SeaLevel.fillToVolume(elevation, oceanDepth);
        long t1 = System.nanoTime();
        Path file = out.resolve("proto_" + seed + ".png");
        MapImage.writeElevation(elevation, seaLevel, file);

        System.out.printf(Locale.ROOT, "geosim %s | seed %d%n", GeoSim.SIM_VERSION, seed);
        System.out.printf(Locale.ROOT, "planet: %.0f km around, %.0f m/block -> %.0f blocks around, %.0f pole to pole%n",
                planet.circumferenceKm(), planet.metersPerBlock(), planet.blocksAround(), planet.poleToPoleBlocks());
        System.out.printf(Locale.ROOT, "grid: %d x %d cells of %.2f km (%.0f blocks)%n",
                grid.width(), grid.height(), grid.cellKm(), grid.cellKm() * 1000.0 / planet.metersPerBlock());
        System.out.printf(Locale.ROOT, "sea level %.0f m on the datum; ocean covers %.1f%% of the surface%n",
                seaLevel, 100.0 * elevation.fractionBelow(seaLevel));
        System.out.printf(Locale.ROOT, "generated in %.0f ms -> %s%n", (t1 - t0) / 1e6, file.toAbsolutePath());
    }
}
