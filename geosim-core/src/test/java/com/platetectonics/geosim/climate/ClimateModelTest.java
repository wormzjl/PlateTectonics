package com.platetectonics.geosim.climate;

import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.Milestone;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClimateModelTest {
    private static final int W = 180;
    private static final int H = 90;
    private static final int COAST_W = 60;
    private static final int COAST_E = 80;
    private static final int RIDGE = 69;

    /** Earth today: every innovation in place, 15 C. */
    private static GlobalState earthToday() {
        double[] times = new double[Milestone.values().length];
        Arrays.fill(times, 0.0);
        return new GlobalState(4_500.0, 1.0, 1_330.0, 1.0, 15.0, 280.0, 1.0, 1.0, 0.0, false, times);
    }

    /**
     * A pole-to-pole continent 4,000 km wide (columns 60-79 of 200 km) with a 3 km north-south range in
     * its middle (columns 69-71), so both flanks are equally far from the sea.
     */
    private static Climate run(CylinderGrid g) {
        float[] e = new float[g.size()];
        for (int idx = 0; idx < e.length; idx++) {
            int i = g.iOf(idx);
            e[idx] = i < COAST_W || i >= COAST_E ? -4_000f : i >= RIDGE && i < RIDGE + 3 ? 3_000f : 200f;
        }
        return new ClimateModel(g).compute(earthToday(), e, null, null);
    }

    private static double meanRain(Climate c, CylinderGrid g, double latMin, double latMax, int i0, int i1) {
        double sum = 0.0;
        int n = 0;
        for (int j = 0; j < g.height(); j++) {
            double lat = Math.abs(g.latitudeDeg(j));
            if (lat < latMin || lat > latMax) {
                continue;
            }
            for (int i = i0; i < i1; i++) {
                sum += c.precipitationMm[g.index(i, j)];
                n++;
            }
        }
        return sum / n;
    }

    @Test
    void rangesCastRainShadowsDownwind() {
        CylinderGrid g = new CylinderGrid(W, H, 200.0);
        Climate c = run(g);
        // westerlies (40-55 deg) blow east: the west flank of the range is wet, the east flank dry
        double westMid = meanRain(c, g, 40, 55, RIDGE - 4, RIDGE);
        double eastMid = meanRain(c, g, 40, 55, RIDGE + 3, RIDGE + 7);
        assertTrue(westMid > 1.5 * eastMid, "mid-latitude rain shadow east of the range: " + westMid + " vs " + eastMid);
        // trade winds (5-25 deg) blow west: here the range shelters its west flank
        double westTrop = meanRain(c, g, 5, 25, RIDGE - 4, RIDGE);
        double eastTrop = meanRain(c, g, 5, 25, RIDGE + 3, RIDGE + 7);
        assertTrue(eastTrop > 1.5 * westTrop, "tropical rain shadow west of the range: " + eastTrop + " vs " + westTrop);
    }

    /**
     * The same hilly continent (6,000 km wide, a 3 km range, hills of +-400 m on ~600 km wavelengths) on
     * 100, 50 and 25 km cells: finer cells resolve more relief, but land rain must stay about the same.
     */
    @Test
    void landRainBarelyDependsOnCellSize() {
        double[] mean = new double[3];
        for (int r = 0; r < 3; r++) {
            double cellKm = 100.0 / (1 << r);
            CylinderGrid g = new CylinderGrid(160 << r, 80 << r, cellKm);
            float[] e = new float[g.size()];
            for (int idx = 0; idx < e.length; idx++) {
                double x = (g.iOf(idx) + 0.5) * cellKm;
                double y = (g.jOf(idx) + 0.5) * cellKm;
                boolean land = x > 5_000.0 && x < 11_000.0;
                double hills = 400.0 * Math.sin(2.0 * Math.PI * x / 550.0) * Math.sin(2.0 * Math.PI * y / 650.0);
                double range = 3_000.0 * Math.exp(-Math.pow((x - 8_000.0) / 150.0, 2.0));
                e[idx] = land ? (float) (600.0 + hills + range) : -4_000f;
            }
            Climate c = new ClimateModel(g).compute(earthToday(), e, null, null);
            double sum = 0.0;
            int n = 0;
            for (int idx = 0; idx < e.length; idx++) {
                if (e[idx] > 0f) {
                    sum += c.precipitationMm[idx];
                    n++;
                }
            }
            mean[r] = sum / n;
        }
        for (int r = 1; r < 3; r++) {
            assertTrue(mean[r] > 0.85 * mean[0] && mean[r] < 1.15 * mean[0],
                    "mean land rain " + Arrays.toString(mean) + " mm on 100, 50, 25 km cells");
        }
    }

    @Test
    void polesAreColdAndIcyAndTheEquatorWarm() {
        CylinderGrid g = new CylinderGrid(W, H, 200.0);
        Climate c = run(g);
        double equator = c.temperatureC[g.index(COAST_E - 2, H / 2)];
        double pole = c.temperatureC[g.index(COAST_E - 2, 1)];
        assertTrue(equator > 22.0 && pole < -15.0, "equator " + equator + ", pole " + pole);
        assertTrue(c.iceM[g.index(COAST_W, 1)] > 0f, "polar coast carries an ice sheet");
        assertTrue(c.iceM[g.index(COAST_E - 2, H / 2)] == 0f, "no ice at the equator");
        assertTrue(c.temperatureC[g.index(RIDGE + 1, H / 2)] < c.temperatureC[g.index(RIDGE + 6, H / 2)] - 15.0,
                "mountains are colder (lapse rate)");
    }
}
