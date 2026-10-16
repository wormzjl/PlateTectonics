package com.platetectonics.geosim.cli;

import com.platetectonics.geosim.climate.Climate;
import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.io.SurfaceMaps;
import com.platetectonics.geosim.io.TectonicMaps;
import com.platetectonics.geosim.planet.PlanetSimulation;
import com.platetectonics.geosim.surface.SurfaceProcesses;
import com.platetectonics.geosim.tectonics.PlateSimulation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Timeline frames that show every map at once (CLI {@code --gif-panels}): relief, plates, crust, climate, surface
 * and soil maps in a grid, a header with the global numbers, and a chart of the global history up to the frame.
 */
final class PanelFrames {
    private static final int COLUMNS = 4;
    private static final int GAP = 4;
    private static final Color BACKGROUND = new Color(18, 20, 26);

    /** One chart series: name, colour, value range, and how a history row maps to it. */
    private record Series(String name, Color colour, double min, double max, int column) {
    }

    // history rows: time, temperature, log10 CO2, log10 O2, land %, ice %, sea level, snowball (0/1)
    private static final Series[] SERIES = {
            new Series("temperature -10..40 C", new Color(255, 110, 90), -10, 40, 1),
            new Series("CO2 100..10^7 ppm (log)", new Color(255, 190, 70), 2, 7, 2),
            new Series("O2 1e-6..2 PAL (log)", new Color(90, 170, 255), -6, Math.log10(2.0), 3),
            new Series("land 0..50%", new Color(120, 210, 110), 0, 50, 4),
            new Series("ice 0..60%", new Color(225, 250, 255), 0, 60, 5),
            new Series("sea level -1,500..1,000 m (datum)", new Color(90, 120, 255), -1500, 1000, 6),
    };

    private final int panelWidth;
    private final double ageMyr;
    private final long seed;
    private final List<double[]> history = new ArrayList<>();

    PanelFrames(int panelWidth, double ageMyr, long seed) {
        this.panelWidth = panelWidth;
        this.ageMyr = ageMyr;
        this.seed = seed;
    }

    /** Remembers the global numbers after a step. */
    void record(PlanetSimulation sim) {
        GlobalState g = sim.state();
        Climate c = sim.climate();
        history.add(new double[] {sim.timeMyr(), g.globalTempC(), Math.log10(Math.max(1e-3, g.pCO2ppm())),
                Math.log10(Math.max(1e-9, g.oxygenPal())), 100 * c.landFraction, 100 * c.iceFraction,
                sim.plates().seaLevelM(), g.snowball() ? 1 : 0});
    }

    BufferedImage render(PlanetSimulation sim) {
        PlateSimulation plates = sim.plates();
        Climate c = sim.climate();
        SurfaceProcesses s = sim.surface();
        float[] e = sim.elevationAboveSeaM();
        FloatField field = new FloatField(sim.grid());
        System.arraycopy(e, 0, field.data(), 0, e.length);

        List<BufferedImage> panels = new ArrayList<>();
        panels.add(panel(SurfaceMaps.reliefWithIce(c, e), "Relief, sea and ice"));
        panels.add(panel(TectonicMaps.elevation(plates, field, true), "Elevation, plate boundaries"));
        BufferedImage platesPanel = panel(TectonicMaps.plates(plates, false), "Plates (bright: continent; arrows: spin)");
        TectonicMaps.drawSpins(plates, platesPanel, Math.max(8.0, platesPanel.getWidth() / 24.0));
        panels.add(platesPanel);
        panels.add(panel(TectonicMaps.crustAge(plates), "Crust age"));
        panels.add(panel(TectonicMaps.crustType(plates), "Crust type"));
        panels.add(panel(SurfaceMaps.temperature(c, e), "Temperature"));
        panels.add(panel(SurfaceMaps.precipitation(c, e), "Precipitation"));
        panels.add(panel(SurfaceMaps.biome(c, e), "Biomes"));
        panels.add(panel(SurfaceMaps.rivers(c, s, e), "Rivers"));
        panels.add(panel(SurfaceMaps.ice(c, e), "Ice"));
        panels.add(panel(SurfaceMaps.erosion(s, sim.grid(), e), "Erosion"));
        panels.add(panel(SurfaceMaps.sediment(plates.lithosphere(), e), "Sediment"));
        panels.add(panel(SurfaceMaps.regolith(plates.lithosphere(), e), "Regolith"));

        int ph = panels.get(0).getHeight();
        int header = Math.max(22, panelWidth / 16);
        int rows = (panels.size() + 3 + COLUMNS - 1) / COLUMNS;
        int width = COLUMNS * panelWidth + (COLUMNS + 1) * GAP;
        int height = header + rows * ph + (rows + 1) * GAP;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(13, header * 3 / 5)));
        g.setColor(Color.WHITE);
        g.drawString(headline(sim), GAP + 4, header - header / 4);
        for (int k = 0; k < panels.size(); k++) {
            int col = k % COLUMNS;
            int row = k / COLUMNS;
            g.drawImage(panels.get(k), GAP + col * (panelWidth + GAP), header + GAP + row * (ph + GAP), null);
        }
        // the history chart fills the rest of the last row
        int k = panels.size();
        int cx = GAP + (k % COLUMNS) * (panelWidth + GAP);
        int cy = header + GAP + (k / COLUMNS) * (ph + GAP);
        int cw = width - GAP - cx;
        chart(g, cx, cy, cw, ph);
        g.dispose();
        return out;
    }

    /**
     * A map shrunk to the panel width by averaging the source pixels under each panel pixel, with its title. (AWT's
     * scaled instances are freed only lazily; thirteen per frame filled the heap.)
     */
    private BufferedImage panel(BufferedImage full, String title) {
        int sw = full.getWidth();
        int sh = full.getHeight();
        int w = Math.min(panelWidth, sw);
        int h = Math.max(1, sh * w / sw);
        int[] src = full.getRGB(0, 0, sw, sh, null, 0, sw);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int ty = 0; ty < h; ty++) {
            int y0 = (int) ((long) ty * sh / h);
            int y1 = Math.max(y0 + 1, (int) ((long) (ty + 1) * sh / h));
            for (int tx = 0; tx < w; tx++) {
                int x0 = (int) ((long) tx * sw / w);
                int x1 = Math.max(x0 + 1, (int) ((long) (tx + 1) * sw / w));
                int r = 0;
                int g = 0;
                int b = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int p = src[y * sw + x];
                        r += (p >> 16) & 0xFF;
                        g += (p >> 8) & 0xFF;
                        b += p & 0xFF;
                    }
                }
                int c = (y1 - y0) * (x1 - x0);
                out.setRGB(tx, ty, ((r / c) << 16) | ((g / c) << 8) | (b / c));
            }
        }
        Graphics2D gfx = out.createGraphics();
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gfx.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, w / 32)));
        gfx.setColor(new Color(0, 0, 0, 160));
        gfx.drawString(title, 7, gfx.getFont().getSize() + 5);
        gfx.setColor(Color.WHITE);
        gfx.drawString(title, 6, gfx.getFont().getSize() + 4);
        gfx.dispose();
        return out;
    }

    private String headline(PlanetSimulation sim) {
        GlobalState g = sim.state();
        PlateSimulation.StepStats st = sim.plates().stats();
        Climate c = sim.climate();
        String o2 = g.oxygenPal() < 1e-4 ? "trace" : String.format(Locale.ROOT, "%.3g PAL", g.oxygenPal());
        return String.format(Locale.ROOT,
                "seed %d   %,.0f Myr   %.1f C   CO2 %,.0f ppm   O2 %s   land %.0f%%   ice %.0f%%   sea level %+.0f m   "
                        + "continental crust %.0f%%   %d plates   %.1f cm/yr (spin %.1f)%s",
                seed, sim.timeMyr(), g.globalTempC(), g.pCO2ppm(), o2, 100 * c.landFraction, 100 * c.iceFraction,
                sim.plates().seaLevelM(), 100 * st.continentalFraction(), st.plates(), st.rmsSpeedCmPerYr(),
                sim.plates().spinRmsCmPerYr(), g.snowball() ? "   SNOWBALL" : "");
    }

    /** Global history so far, each series scaled to its own range; grey bands mark snowball episodes. */
    private void chart(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(30, 33, 42));
        g.fillRect(x, y, w, h);
        int left = x + 8;
        int right = x + w - 8;
        int top = y + 8;
        int bottom = y + h - 18;
        double span = Math.max(1.0, ageMyr);
        // snowball bands
        g.setColor(new Color(80, 90, 110));
        for (int k = 1; k < history.size(); k++) {
            if (history.get(k)[7] > 0) {
                int x0 = left + (int) ((right - left) * history.get(k - 1)[0] / span);
                int x1 = left + (int) ((right - left) * history.get(k)[0] / span);
                g.fillRect(x0, top, Math.max(1, x1 - x0), bottom - top);
            }
        }
        // time axis
        g.setColor(new Color(120, 125, 140));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(left, bottom, right, bottom);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        double tick = span > 3000 ? 1000 : span > 1200 ? 500 : 250;
        for (double t = 0; t <= span + 1e-6; t += tick) {
            int tx = left + (int) ((right - left) * t / span);
            g.drawLine(tx, bottom, tx, bottom + 3);
            // the last label carries the unit and ends at the axis end
            boolean last = t + tick > span + 1e-6;
            String label = String.format(Locale.ROOT, last ? "%,.0f Myr" : "%,.0f", t);
            int lw = g.getFontMetrics().stringWidth(label);
            g.drawString(label, last ? Math.min(tx - 10, right - lw) : tx - 10, bottom + 14);
        }
        // series
        g.setStroke(new BasicStroke(1.6f));
        for (Series s : SERIES) {
            g.setColor(s.colour());
            int px = -1;
            int py = -1;
            for (double[] row : history) {
                double v = (row[s.column()] - s.min()) / (s.max() - s.min());
                v = Math.max(0.0, Math.min(1.0, v));
                int qx = left + (int) ((right - left) * row[0] / span);
                int qy = bottom - (int) ((bottom - top) * v);
                if (px >= 0) {
                    g.drawLine(px, py, qx, qy);
                }
                px = qx;
                py = qy;
            }
        }
        // legend
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        int lx = left + 4;
        int ly = top + 12;
        for (Series s : SERIES) {
            g.setColor(s.colour());
            g.fillRect(lx, ly - 8, 10, 3);
            g.drawString(s.name(), lx + 14, ly - 3);
            lx += 14 + g.getFontMetrics().stringWidth(s.name()) + 14;
            if (lx > right - 170) {
                lx = left + 4;
                ly += 14;
            }
        }
    }
}
