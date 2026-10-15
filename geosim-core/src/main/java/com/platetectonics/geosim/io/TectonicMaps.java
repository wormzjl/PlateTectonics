package com.platetectonics.geosim.io;

import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.tectonics.Lithosphere;
import com.platetectonics.geosim.tectonics.PlateSimulation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Map renderings of the plate simulation: elevation, plates, crust age and crust type. */
public final class TectonicMaps {
    private TectonicMaps() {
    }

    /** Elevation above sea level with plate boundaries drawn as thin dark lines. */
    public static BufferedImage elevation(PlateSimulation sim, FloatField aboveSea, boolean boundaries) {
        BufferedImage img = MapImage.renderElevation(aboveSea, 0.0);
        if (boundaries) {
            Lithosphere L = sim.lithosphere();
            CylinderGrid g = sim.grid();
            for (int idx = 0; idx < g.size(); idx++) {
                if (isBoundary(L, g, idx)) {
                    int rgb = img.getRGB(g.iOf(idx), g.jOf(idx));
                    img.setRGB(g.iOf(idx), g.jOf(idx), blend(rgb, 0x101010, 0.55));
                }
            }
        }
        return img;
    }

    /** Each plate in its own colour; continents bright, ocean floor dark; boundaries black. */
    public static BufferedImage plates(PlateSimulation sim) {
        Lithosphere L = sim.lithosphere();
        CylinderGrid g = sim.grid();
        BufferedImage img = new BufferedImage(g.width(), g.height(), BufferedImage.TYPE_INT_RGB);
        for (int idx = 0; idx < g.size(); idx++) {
            int rgb;
            if (isBoundary(L, g, idx)) {
                rgb = 0x000000;
            } else {
                float hue = (float) ((L.owner(idx) * 0.618033988749895) % 1.0);
                float brightness = L.isContinental(idx) ? 0.92f : 0.55f;
                rgb = Color.HSBtoRGB(hue, 0.55f, brightness) & 0xFFFFFF;
            }
            img.setRGB(g.iOf(idx), g.jOf(idx), rgb);
        }
        return img;
    }

    /**
     * Ocean floor coloured by age like real sea-floor age maps (red young, blue/violet old);
     * continents in browns that darken with the age of the crust.
     */
    public static BufferedImage crustAge(PlateSimulation sim) {
        Lithosphere L = sim.lithosphere();
        CylinderGrid g = sim.grid();
        BufferedImage img = new BufferedImage(g.width(), g.height(), BufferedImage.TYPE_INT_RGB);
        double now = sim.timeMyr();
        for (int idx = 0; idx < g.size(); idx++) {
            int rgb;
            if (L.isContinental(idx)) {
                double age = Math.max(0.0, now - L.formedAtMyr(idx));
                double t = Math.min(1.0, age / 4000.0);
                rgb = lerp(0xF2D9A0, 0x4A2A12, t);
            } else {
                double t = Math.min(1.0, L.oceanAgeMyr(idx) / 200.0);
                rgb = Color.HSBtoRGB((float) (0.0 + 0.78 * t), 0.75f, 0.9f) & 0xFFFFFF;
            }
            img.setRGB(g.iOf(idx), g.jOf(idx), rgb);
        }
        return img;
    }

    /**
     * Crust type and this step's activity: ocean (blue), continent (tan, darker and then white where
     * thick), arcs (green), trenches (red), new ridges (yellow), collisions (magenta), plume heads (orange).
     */
    public static BufferedImage crustType(PlateSimulation sim) {
        Lithosphere L = sim.lithosphere();
        CylinderGrid g = sim.grid();
        BufferedImage img = new BufferedImage(g.width(), g.height(), BufferedImage.TYPE_INT_RGB);
        for (int idx = 0; idx < g.size(); idx++) {
            int rgb;
            if (L.isContinental(idx)) {
                double th = L.thicknessKm(idx);
                rgb = th < 45 ? lerp(0xD8C08A, 0x9C7A4A, (th - 20) / 25.0) : lerp(0x9C7A4A, 0xFFFFFF, (th - 45) / 30.0);
            } else {
                rgb = lerp(0x3C78B4, 0x0E2448, Math.min(1.0, L.oceanAgeMyr(idx) / 180.0));
            }
            byte m = L.marks(idx);
            if ((m & Lithosphere.MARK_COLLISION) != 0) {
                rgb = 0xE020E0;
            } else if ((m & Lithosphere.MARK_TRENCH) != 0) {
                rgb = 0xE02020;
            } else if ((m & Lithosphere.MARK_RIDGE) != 0) {
                rgb = 0xF0E040;
            } else if ((m & Lithosphere.MARK_PLUME) != 0) {
                rgb = blend(rgb, 0xFF8C00, 0.6);
            } else if ((m & Lithosphere.MARK_ARC) != 0) {
                rgb = blend(rgb, 0x30C040, 0.6);
            }
            img.setRGB(g.iOf(idx), g.jOf(idx), rgb);
        }
        return img;
    }

    /** Area-averaged downscale to {@code width} pixels wide (never upscaled), with a caption in the top-left corner. */
    public static BufferedImage frame(BufferedImage full, int width, String caption) {
        width = Math.min(width, full.getWidth());
        int height = Math.max(1, full.getHeight() * width / full.getWidth());
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D gfx = out.createGraphics();
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gfx.drawImage(full.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gfx.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, width / 45)));
        gfx.setColor(new Color(0, 0, 0, 150));
        gfx.drawString(caption, 9, gfx.getFont().getSize() + 7);
        gfx.setColor(Color.WHITE);
        gfx.drawString(caption, 8, gfx.getFont().getSize() + 6);
        gfx.dispose();
        return out;
    }

    private static boolean isBoundary(Lithosphere L, CylinderGrid g, int idx) {
        int i = g.iOf(idx);
        int j = g.jOf(idx);
        int o = L.owner(idx);
        return L.owner(g.index(i + 1, j)) != o || (j + 1 < g.height() && L.owner(g.index(i, j + 1)) != o);
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
