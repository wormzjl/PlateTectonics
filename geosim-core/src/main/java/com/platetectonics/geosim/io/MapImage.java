package com.platetectonics.geosim.io;

import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes fields as PNG maps (north at the top, x wrapping left-right). */
public final class MapImage {
    // Elevation ramp relative to sea level: {metres, r, g, b}
    private static final double[][] ELEVATION_RAMP = {
            {-6000, 8, 24, 58},
            {-3000, 18, 52, 104},
            {-200, 52, 112, 170},
            {0, 96, 160, 205},
            {1, 84, 138, 72},
            {500, 132, 164, 92},
            {1500, 190, 170, 110},
            {3000, 150, 122, 100},
            {4500, 235, 235, 235},
    };

    private MapImage() {
    }

    /** Elevation map with simple hillshading. {@code seaLevelM} is the water surface on the same datum. */
    public static void writeElevation(FloatField elevation, double seaLevelM, Path file) throws IOException {
        write(renderElevation(elevation, seaLevelM), file);
    }

    public static BufferedImage renderElevation(FloatField elevation, double seaLevelM) {
        CylinderGrid g = elevation.grid();
        BufferedImage img = new BufferedImage(g.width(), g.height(), BufferedImage.TYPE_INT_RGB);
        double shadeScale = 1.0 / (g.cellKm() * 1000.0);
        for (int j = 0; j < g.height(); j++) {
            for (int i = 0; i < g.width(); i++) {
                double e = elevation.get(i, j) - seaLevelM;
                int[] rgb = ramp(e);
                if (e > 0) {
                    // light from the north-west; slopes in metres per metre
                    double dx = (elevation.get(i + 1, j) - elevation.get(i - 1, j)) * 0.5 * shadeScale;
                    double dy = (elevation.get(i, j + 1) - elevation.get(i, j - 1)) * 0.5 * shadeScale;
                    double shade = 1.0 - 40.0 * (dx + dy);
                    shade = Math.max(0.6, Math.min(1.3, shade));
                    for (int c = 0; c < 3; c++) {
                        rgb[c] = (int) Math.max(0, Math.min(255, rgb[c] * shade));
                    }
                }
                img.setRGB(i, j, (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
            }
        }
        return img;
    }

    public static void write(BufferedImage img, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(img, "png", file.toFile());
    }

    public static int[] ramp(double e) {
        double[][] r = ELEVATION_RAMP;
        if (e <= r[0][0]) {
            return new int[]{(int) r[0][1], (int) r[0][2], (int) r[0][3]};
        }
        for (int k = 1; k < r.length; k++) {
            if (e <= r[k][0]) {
                double t = (e - r[k - 1][0]) / (r[k][0] - r[k - 1][0]);
                int[] out = new int[3];
                for (int c = 0; c < 3; c++) {
                    out[c] = (int) Math.round(r[k - 1][c + 1] + t * (r[k][c + 1] - r[k - 1][c + 1]));
                }
                return out;
            }
        }
        double[] last = r[r.length - 1];
        return new int[]{(int) last[1], (int) last[2], (int) last[3]};
    }
}
