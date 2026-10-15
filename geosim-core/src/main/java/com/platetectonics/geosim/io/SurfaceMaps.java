package com.platetectonics.geosim.io;

import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.surface.SurfaceProcesses;
import com.platetectonics.geosim.tectonics.Lithosphere;

import java.awt.image.BufferedImage;

/** Map renderings of climate and surface processes: temperature, rain, biomes, rivers, ice, sediment, soil. */
public final class SurfaceMaps {
    private static final double[][] TEMPERATURE_RAMP = {
            {-40, 0x2A0A5E}, {-20, 0x3050C0}, {-5, 0x9CC8F0}, {0, 0xF4F4F4}, {10, 0xF6E08A}, {20, 0xF09A3C},
            {30, 0xC0281C}, {40, 0x5A0A0A}};
    private static final double[][] RAIN_RAMP = {
            {0, 0xC8A064}, {250, 0xE6D28C}, {500, 0xB4D278}, {1000, 0x50A050}, {2000, 0x1E7850}, {4000, 0x0A3C64}};
    private static final double[][] SEDIMENT_RAMP = {
            {0, 0x303030}, {0.5, 0x6E5A3C}, {2, 0xB48C50}, {5, 0xE6C878}, {10, 0xFAF0B4}, {15, 0xFFFFFF}};
    private static final double[][] REGOLITH_RAMP = {
            {0, 0x8C8C8C}, {2, 0xC8B48C}, {10, 0xC88250}, {30, 0xA0321E}, {60, 0x5A0F0F}};
    private static final double[][] EROSION_RAMP = {
            {0, 0x283C28}, {10, 0x507850}, {50, 0xC8C864}, {300, 0xE6823C}, {1000, 0xB41E1E}, {3000, 0xFFFFFF}};

    private SurfaceMaps() {
    }

    /** Annual mean surface temperature; the ocean drawn darker. */
    public static BufferedImage temperature(Climate c, float[] aboveSea) {
        return field(c.grid, idx -> c.temperatureC[idx], TEMPERATURE_RAMP, aboveSea, 0.75);
    }

    /** Annual precipitation (land), the ocean in grey. */
    public static BufferedImage precipitation(Climate c, float[] aboveSea) {
        BufferedImage img = field(c.grid, idx -> c.precipitationMm[idx], RAIN_RAMP, aboveSea, 1.0);
        return greyOcean(img, c.grid, aboveSea);
    }

    /** Biome of every cell. */
    public static BufferedImage biome(Climate c, float[] aboveSea) {
        CylinderGrid g = c.grid;
        BufferedImage img = new BufferedImage(g.width(), g.height(), BufferedImage.TYPE_INT_RGB);
        for (int idx = 0; idx < g.size(); idx++) {
            img.setRGB(g.iOf(idx), g.jOf(idx), c.biome(idx, aboveSea[idx]).rgb());
        }
        return img;
    }

    /** Shaded relief with ice sheets, lakes (salt lakes white-green) and rivers widening with discharge. */
    public static BufferedImage rivers(Climate c, SurfaceProcesses s, float[] aboveSea) {
        CylinderGrid g = c.grid;
        BufferedImage img = relief(g, aboveSea);
        float[] q = s.dischargeKm3PerYr();
        for (int idx = 0; idx < g.size(); idx++) {
            int i = g.iOf(idx);
            int j = g.jOf(idx);
            if (aboveSea[idx] <= 0f) {
                continue;
            }
            if (c.iceM[idx] > 0f) {
                img.setRGB(i, j, blend(img.getRGB(i, j), 0xF4F8FF, 0.55 + 0.4 * Math.min(1.0, c.iceM[idx] / 2_000.0)));
            }
            if (s.lakes[idx] == 2) {
                img.setRGB(i, j, 0xC8E6D2);
            } else if (s.lakes[idx] == 1) {
                img.setRGB(i, j, 0x3C82C8);
            } else if (q[idx] > 5f) {
                double t = Math.min(1.0, Math.log10(q[idx] / 5.0) / 2.5);
                img.setRGB(i, j, blend(img.getRGB(i, j), 0x1E50C8, 0.45 + 0.55 * t));
            }
        }
        return img;
    }

    /** Ice-sheet thickness over relief. */
    public static BufferedImage ice(Climate c, float[] aboveSea) {
        CylinderGrid g = c.grid;
        BufferedImage img = relief(g, aboveSea);
        for (int idx = 0; idx < g.size(); idx++) {
            if (c.iceM[idx] > 0f) {
                double t = Math.min(1.0, c.iceM[idx] / 3_000.0);
                img.setRGB(g.iOf(idx), g.jOf(idx), lerp(0xDCEBFF, 0x5A78B4, t));
            }
        }
        return img;
    }

    /** Sediment thickness, km, on land and sea floor; coastlines in black. */
    public static BufferedImage sediment(Lithosphere lith, float[] aboveSea) {
        BufferedImage img = field(lith.grid(), lith::sedimentKm, SEDIMENT_RAMP, null, 1.0);
        return coastlines(img, lith.grid(), aboveSea);
    }

    /** Regolith (weathering profile) depth, m, on land. */
    public static BufferedImage regolith(Lithosphere lith, float[] aboveSea) {
        BufferedImage img = field(lith.grid(), lith::regolithM, REGOLITH_RAMP, null, 1.0);
        return greyOcean(img, lith.grid(), aboveSea);
    }

    /** Rock erosion rate of the last step, m/Myr, on land. */
    public static BufferedImage erosion(SurfaceProcesses s, CylinderGrid g, float[] aboveSea) {
        BufferedImage img = field(g, idx -> s.erosionMPerMyr[idx], EROSION_RAMP, null, 1.0);
        return greyOcean(img, g, aboveSea);
    }

    /** Relief with ice in white: the timeline GIF frame for full planet runs. */
    public static BufferedImage reliefWithIce(Climate c, float[] aboveSea) {
        CylinderGrid g = c.grid;
        BufferedImage img = relief(g, aboveSea);
        for (int idx = 0; idx < g.size(); idx++) {
            if (c.iceM[idx] > 0f) {
                img.setRGB(g.iOf(idx), g.jOf(idx), 0xF0F4FA);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ helpers

    private interface Value {
        double at(int idx);
    }

    private static BufferedImage relief(CylinderGrid g, float[] aboveSea) {
        FloatField f = new FloatField(g);
        System.arraycopy(aboveSea, 0, f.data(), 0, aboveSea.length);
        return MapImage.renderElevation(f, 0.0);
    }

    private static BufferedImage field(CylinderGrid g, Value v, double[][] ramp, float[] aboveSea, double oceanShade) {
        BufferedImage img = new BufferedImage(g.width(), g.height(), BufferedImage.TYPE_INT_RGB);
        for (int idx = 0; idx < g.size(); idx++) {
            int rgb = ramp(ramp, v.at(idx));
            if (aboveSea != null && aboveSea[idx] <= 0f && oceanShade < 1.0) {
                rgb = lerp(0x000000, rgb, oceanShade);
            }
            img.setRGB(g.iOf(idx), g.jOf(idx), rgb);
        }
        return img;
    }

    private static BufferedImage greyOcean(BufferedImage img, CylinderGrid g, float[] aboveSea) {
        for (int idx = 0; idx < g.size(); idx++) {
            if (aboveSea[idx] <= 0f) {
                img.setRGB(g.iOf(idx), g.jOf(idx), 0x5A6470);
            }
        }
        return img;
    }

    private static BufferedImage coastlines(BufferedImage img, CylinderGrid g, float[] aboveSea) {
        for (int idx = 0; idx < g.size(); idx++) {
            int i = g.iOf(idx);
            int j = g.jOf(idx);
            boolean land = aboveSea[idx] > 0f;
            boolean east = aboveSea[g.index(i + 1, j)] > 0f;
            boolean south = j + 1 < g.height() && aboveSea[g.index(i, j + 1)] > 0f;
            if (land != east || (j + 1 < g.height() && land != south)) {
                img.setRGB(i, j, 0x000000);
            }
        }
        return img;
    }

    private static int ramp(double[][] r, double v) {
        if (v <= r[0][0]) {
            return (int) r[0][1];
        }
        for (int k = 1; k < r.length; k++) {
            if (v <= r[k][0]) {
                return lerp((int) r[k - 1][1], (int) r[k][1], (v - r[k - 1][0]) / (r[k][0] - r[k - 1][0]));
            }
        }
        return (int) r[r.length - 1][1];
    }

    private static int lerp(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(((a >> 16) & 255) + t * (((b >> 16) & 255) - ((a >> 16) & 255)));
        int gr = (int) Math.round(((a >> 8) & 255) + t * (((b >> 8) & 255) - ((a >> 8) & 255)));
        int bl = (int) Math.round((a & 255) + t * ((b & 255) - (a & 255)));
        return (r << 16) | (gr << 8) | bl;
    }

    private static int blend(int a, int b, double t) {
        return lerp(a & 0xFFFFFF, b & 0xFFFFFF, t);
    }
}
