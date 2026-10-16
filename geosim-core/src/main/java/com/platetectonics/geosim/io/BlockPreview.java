package com.platetectonics.geosim.io;

import com.platetectonics.geosim.world.TerrainSampler;

import java.awt.image.BufferedImage;

/**
 * Top-down view of the terrain at block resolution, as chunk generation will build it: the same
 * {@link TerrainSampler} elevation, hillshaded, with water and ice. Checks the block-scale detail without
 * starting Minecraft.
 */
public final class BlockPreview {
    private BlockPreview() {
    }

    /**
     * @param centreX centre block x
     * @param centreZ centre block z
     * @param pixels  image width and height
     * @param step    blocks per pixel
     */
    public static BufferedImage render(TerrainSampler s, int centreX, int centreZ, int pixels, int step) {
        BufferedImage img = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_RGB);
        double[] e = new double[(pixels + 1) * (pixels + 1)];
        int x0 = centreX - pixels / 2 * step;
        int z0 = centreZ - pixels / 2 * step;
        for (int py = 0; py <= pixels; py++) {
            for (int px = 0; px <= pixels; px++) {
                e[py * (pixels + 1) + px] = s.elevationM(x0 + px * step, z0 + py * step);
            }
        }
        double metresPerPixel = step * s.spec().metersPerBlock();
        for (int py = 0; py < pixels; py++) {
            for (int px = 0; px < pixels; px++) {
                double h = e[py * (pixels + 1) + px];
                int bx = x0 + px * step;
                int bz = z0 + py * step;
                int[] rgb = MapImage.ramp(h);
                if (h > 0.0) {
                    if (s.iceM(bx, bz) > 1.0) {
                        rgb = new int[]{236, 242, 248};
                    }
                    double dx = (e[py * (pixels + 1) + px + 1] - h) / metresPerPixel;
                    double dy = (e[(py + 1) * (pixels + 1) + px] - h) / metresPerPixel;
                    double shade = Math.max(0.45, Math.min(1.4, 1.0 - 2.5 * (dx + dy)));
                    for (int c = 0; c < 3; c++) {
                        rgb[c] = (int) Math.max(0, Math.min(255, rgb[c] * shade));
                    }
                } else if (s.temperatureC(bx, bz, h) < -2.0) {
                    rgb = new int[]{200, 220, 235}; // sea ice
                }
                img.setRGB(px, py, (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
            }
        }
        return img;
    }
}
