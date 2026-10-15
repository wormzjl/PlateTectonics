package com.platetectonics.geosim.climate;

import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.parallel.Parallel;

/**
 * Climate from latitude, the global temperature, elevation and the land-sea layout
 * (docs/design/04-surface-processes.md#climate).
 * <ul>
 *   <li>Temperature: a zonal profile around the global mean (the pole-to-equator contrast shrinks in warm
 *       worlds), minus 6.5 C per km of elevation; continental interiors are colder at high latitude and
 *       have a larger seasonal range.</li>
 *   <li>Wind: trade winds blow west below 30 deg, westerlies east to 60 deg, polar easterlies west.</li>
 *   <li>Moisture is swept along each row with the wind. Air over the sea is saturated; over land it rains
 *       out with distance (slowly in the tropics, where forests recycle rain), and rising air rains more
 *       and dries: windward slopes are wet and the lee lies in a rain shadow. Cold high ground holds
 *       little moisture. The zonal base pattern has the wet equator, dry subtropics, wet storm tracks
 *       and dry poles.</li>
 *   <li>Ice sheets grow on land with no summer melt. Their thickness follows a perfectly plastic ice
 *       profile, H = sqrt(2 tau d / (rho g)) with d the distance from the ice margin.</li>
 * </ul>
 */
public final class ClimateModel {
    /** |latitude| (deg) -> precipitation over the sea, mm/yr: Earth's zonal pattern. */
    private static final double[][] ZONAL_PRECIP = {
            {0, 2_100}, {8, 1_900}, {15, 1_200}, {22, 500}, {30, 450}, {38, 750},
            {45, 1_000}, {55, 1_050}, {65, 650}, {75, 300}, {90, 150}};
    private static final double LAPSE_C_PER_M = 6.5 / 1000.0;
    private static final double ICE_RHO_G = 917.0 * 9.81;
    private static final double ICE_YIELD_PA = 1.0e5;
    /**
     * Mean weathering weight of a land cell in this climate model for an Earth-like layout (rain inland is
     * below the coastal value, most land erodes slowly): normalizes land weathering to 1 for Earth today.
     */
    private static final double EARTH_LAND_WEIGHT = 0.45;
    /** Strength of the flow against the prevailing wind (monsoons, cyclones), relative to it. */
    private static final double REVERSE_FLOW = 0.6;
    /** North-south spread of rain by storms and monsoons (one sigma), km. */
    private static final double MIXING_KM = 100.0;
    /** Rising air loses this share of its moisture (as e-folds) per km climbed. */
    private static final double RISE_DRYING_PER_KM = 0.45;

    private final CylinderGrid grid;
    private final int[] queue;
    private final int[] dist;
    private final float[] mixScratch;
    private final double[] weatheringScratch;
    private Parallel parallel = Parallel.SERIAL;

    public ClimateModel(CylinderGrid grid) {
        this.grid = grid;
        this.queue = new int[grid.size()];
        this.dist = new int[grid.size()];
        this.mixScratch = new float[grid.size()];
        this.weatheringScratch = new double[grid.size()];
    }

    /** Worker threads for the row and cell loops (results do not depend on the count). */
    public void setParallel(Parallel parallel) {
        this.parallel = parallel;
    }

    /**
     * @param elevationAboveSeaM elevation relative to sea level, m
     * @param erosionMPerMyr     recent erosion rate (fresh rock weathers faster), or null
     */
    public Climate compute(GlobalState g, float[] elevationAboveSeaM, float[] erosionMPerMyr, Climate out) {
        Climate c = out != null ? out : new Climate(grid);
        coastDistance(elevationAboveSeaM, c.coastDistanceKm);
        temperature(g, elevationAboveSeaM, c);
        precipitation(g, elevationAboveSeaM, c);
        ice(g, elevationAboveSeaM, c);
        weathering(g, erosionMPerMyr, elevationAboveSeaM, c);
        return c;
    }

    // ------------------------------------------------------------------ temperature

    private void temperature(GlobalState g, float[] e, Climate c) {
        int w = grid.width();
        double tg = g.globalTempC();
        double equator = tg + 13.0 - 0.3 * (tg - 15.0);
        double pole = tg - 32.0 + 0.5 * (tg - 15.0);
        parallel.rows(grid.height(), (from, to) -> {
            for (int j = from; j < to; j++) {
                double s = Math.sin(Math.toRadians(grid.latitudeDeg(j)));
                double s2 = s * s;
                double sea = equator + (pole - equator) * s2;
                for (int i = 0; i < w; i++) {
                    int idx = j * w + i;
                    if (e[idx] <= 0f) {
                        c.temperatureC[idx] = (float) Math.max(-2.0, sea);
                        c.seasonalRangeC[idx] = (float) (2.0 + 8.0 * Math.abs(s));
                        continue;
                    }
                    double inland = Math.min(1.0, c.coastDistanceKm[idx] / 1_500.0);
                    c.temperatureC[idx] = (float) (sea - LAPSE_C_PER_M * e[idx] - 6.0 * s2 * inland + 2.0 * (1.0 - s2) * inland);
                    c.seasonalRangeC[idx] = (float) (3.0 + 22.0 * Math.abs(s) * (0.3 + 0.7 * inland));
                }
            }
        });
    }

    // ------------------------------------------------------------------ precipitation

    private void precipitation(GlobalState g, float[] e, Climate c) {
        int w = grid.width();
        int h = grid.height();
        double cellKm = grid.cellKm();
        double wetter = Math.exp(0.03 * (g.globalTempC() - 15.0)) * (g.snowball() ? 0.1 : 1.0);
        float[] p = c.precipitationMm;
        // rows are independent: each sweep reads and writes its own row only
        parallel.rows(h, (from, to) -> {
            for (int j = from; j < to; j++) {
                double lat = Math.abs(grid.latitudeDeg(j));
                double zonal = zonal(lat) * wetter;
                int dir = lat < 30.0 ? -1 : lat < 60.0 ? 1 : -1;
                double decayKm = lat < 25.0 ? 2_500.0 : lat < 60.0 ? 1_500.0 : 1_000.0;
                // prevailing wind, then the weaker flow from the other side (monsoons, cyclones on lee coasts)
                sweep(c, e, j, dir, zonal, decayKm, 1.0, p, false);
                sweep(c, e, j, -dir, zonal, decayKm * 0.6, REVERSE_FLOW, p, true);
            }
        });
        // north-south mixing (storms and monsoons are not zonal): repeated 1-2-1 passes, each adding half a
        // cell squared of variance, to a spread of MIXING_KM whatever the cell size; ping-pong buffers
        int passes = Math.max(1, (int) Math.round(2.0 * Math.pow(MIXING_KM / cellKm, 2.0)));
        float[] src = p;
        float[] dst = mixScratch;
        for (int pass = 0; pass < passes; pass++) {
            float[] a = src;
            float[] b = dst;
            parallel.rows(h, (from, to) -> {
                for (int j = from; j < to; j++) {
                    int jn = Math.max(0, j - 1);
                    int js = Math.min(h - 1, j + 1);
                    for (int i = 0; i < w; i++) {
                        b[j * w + i] = 0.5f * a[j * w + i] + 0.25f * (a[jn * w + i] + a[js * w + i]);
                    }
                }
            });
            src = b;
            dst = a;
        }
        if (src != p) {
            System.arraycopy(src, 0, p, 0, p.length);
        }
        parallel.cells(p.length, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                double pet = Math.max(0.0, 60.0 * (c.temperatureC[idx] + 3.0));
                c.aridity[idx] = (float) (pet <= 1.0 ? 10.0 : Math.min(10.0, p[idx] / pet));
            }
        });
    }

    /**
     * Carries moisture along row {@code j} in direction {@code dir}, raining it out over land; with
     * {@code keepWetter} the row keeps the wetter of this sweep (scaled by {@code weight}) and what is there.
     */
    private void sweep(Climate c, float[] e, int j, int dir, double zonal, double decayKm, double weight, float[] p,
                       boolean keepWetter) {
        int w = grid.width();
        double cellKm = grid.cellKm();
        double seaTemp = c.temperatureC[j * w]; // any sea cell of the row would do; recomputed below
        int start = -1;
        for (int i = 0; i < w; i++) {
            if (e[j * w + i] <= 0f) {
                start = i;
                seaTemp = c.temperatureC[j * w + i];
                break;
            }
        }
        double q = start < 0 ? 0.5 : 1.0;
        int i = start < 0 ? 0 : start;
        double prevE = 0.0;
        for (int step = 0; step < w; step++, i = Math.floorMod(i + dir, w)) {
            int idx = j * w + i;
            double elev = e[idx];
            double rain;
            if (elev <= 0.0) {
                q = 1.0;
                rain = zonal;
                prevE = 0.0;
            } else {
                double rise = Math.max(0.0, elev - prevE);
                // cold air holds less water (Clausius-Clapeyron, ~6.5% per degree)
                double hold = Math.exp(0.065 * Math.min(0.0, c.temperatureC[idx] - seaTemp));
                q = Math.min(q, hold);
                double lift = rise / cellKm; // m per km
                rain = zonal * q * (1.0 + 0.02 * lift);
                q *= Math.exp(-cellKm / decayKm) * Math.exp(-RISE_DRYING_PER_KM * rise / 1_000.0);
                prevE = elev;
            }
            p[idx] = keepWetter ? Math.max(p[idx], (float) (weight * rain)) : (float) rain;
        }
    }

    private static double zonal(double absLat) {
        for (int k = 1; k < ZONAL_PRECIP.length; k++) {
            if (absLat <= ZONAL_PRECIP[k][0]) {
                double t = (absLat - ZONAL_PRECIP[k - 1][0]) / (ZONAL_PRECIP[k][0] - ZONAL_PRECIP[k - 1][0]);
                return ZONAL_PRECIP[k - 1][1] + t * (ZONAL_PRECIP[k][1] - ZONAL_PRECIP[k - 1][1]);
            }
        }
        return ZONAL_PRECIP[ZONAL_PRECIP.length - 1][1];
    }

    // ------------------------------------------------------------------ ice

    private void ice(GlobalState g, float[] e, Climate c) {
        int n = grid.size();
        int w = grid.width();
        int h = grid.height();
        float[] ice = c.iceM;
        int tail = 0;
        int land = 0;
        int iced = 0;
        for (int idx = 0; idx < n; idx++) {
            ice[idx] = 0f;
            dist[idx] = -1;
            if (e[idx] <= 0f) {
                continue;
            }
            land++;
            double summer = c.temperatureC[idx] + 0.5 * c.seasonalRangeC[idx];
            if (g.snowball() || (summer < 0.0 && c.precipitationMm[idx] >= 30.0)) {
                ice[idx] = 1f; // marked; thickness below
                iced++;
            }
        }
        // distance from the ice margin, breadth-first from ice cells that touch ice-free ground
        for (int idx = 0; idx < n; idx++) {
            if (ice[idx] > 0f && touchesIceFree(ice, idx, w, h)) {
                dist[idx] = 0;
                queue[tail++] = idx;
            }
        }
        for (int head = 0; head < tail; head++) {
            int idx = queue[head];
            int i = idx % w;
            int j = idx / w;
            tail = visitIce(ice, idx, j * w + (i + 1 == w ? 0 : i + 1), tail);
            tail = visitIce(ice, idx, j * w + (i == 0 ? w - 1 : i - 1), tail);
            if (j > 0) {
                tail = visitIce(ice, idx, idx - w, tail);
            }
            if (j < h - 1) {
                tail = visitIce(ice, idx, idx + w, tail);
            }
        }
        double volume = 0.0;
        for (int idx = 0; idx < n; idx++) {
            if (ice[idx] > 0f) {
                double d = (Math.max(0, dist[idx]) + 0.5) * grid.cellKm() * 1000.0;
                ice[idx] = (float) Math.min(4_000.0, Math.sqrt(2.0 * ICE_YIELD_PA * d / ICE_RHO_G));
                volume += ice[idx];
            }
        }
        c.iceWaterEquivalentM = volume * 0.917 / n;
        c.landFraction = (double) land / n;
        c.iceFraction = land == 0 ? 0.0 : (double) iced / land;
    }

    private static boolean touchesIceFree(float[] ice, int idx, int w, int h) {
        int i = idx % w;
        int j = idx / w;
        return ice[j * w + (i + 1 == w ? 0 : i + 1)] <= 0f || ice[j * w + (i == 0 ? w - 1 : i - 1)] <= 0f
                || (j > 0 && ice[idx - w] <= 0f) || (j < h - 1 && ice[idx + w] <= 0f);
    }

    private int visitIce(float[] ice, int from, int to, int tail) {
        if (ice[to] > 0f && dist[to] < 0) {
            dist[to] = dist[from] + 1;
            queue[tail++] = to;
        }
        return tail;
    }

    // ------------------------------------------------------------------ coast distance and weathering

    private void coastDistance(float[] e, float[] out) {
        int n = grid.size();
        int w = grid.width();
        int h = grid.height();
        int tail = 0;
        for (int idx = 0; idx < n; idx++) {
            if (e[idx] <= 0f) {
                dist[idx] = 0;
                queue[tail++] = idx;
            } else {
                dist[idx] = -1;
            }
        }
        for (int head = 0; head < tail; head++) {
            int idx = queue[head];
            int i = idx % w;
            int j = idx / w;
            tail = visit(idx, j * w + (i + 1 == w ? 0 : i + 1), tail);
            tail = visit(idx, j * w + (i == 0 ? w - 1 : i - 1), tail);
            if (j > 0) {
                tail = visit(idx, idx - w, tail);
            }
            if (j < h - 1) {
                tail = visit(idx, idx + w, tail);
            }
        }
        for (int idx = 0; idx < n; idx++) {
            out[idx] = (float) ((dist[idx] < 0 ? 10_000 : dist[idx]) * grid.cellKm());
        }
    }

    private int visit(int from, int to, int tail) {
        if (dist[to] < 0) {
            dist[to] = dist[from] + 1;
            queue[tail++] = to;
        }
        return tail;
    }

    /**
     * Land weathering strength for the carbon thermostat, relative to Earth today: rain on ice-free land,
     * warmer than the global mean weathers faster, fresh (eroding) rock faster than old soil-covered shields.
     */
    private void weathering(GlobalState g, float[] erosion, float[] e, Climate c) {
        double tg = g.globalTempC();
        double[] term = weatheringScratch;
        parallel.cells(grid.size(), (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                if (e[idx] <= 0f || c.iceM[idx] > 0f) {
                    continue;
                }
                double fresh = erosion == null ? 1.0 : 0.5 + 0.5 * Math.min(1.0, erosion[idx] / 50.0);
                term[idx] = c.precipitationMm[idx] / 750.0 * Math.exp((c.temperatureC[idx] - tg) / 13.7) * fresh;
            }
        });
        // summed in cell order, as single-threaded code would, so the total is the same bit for bit
        double sum = 0.0;
        for (int idx = 0; idx < grid.size(); idx++) {
            if (e[idx] > 0f && c.iceM[idx] <= 0f) {
                sum += term[idx];
            }
        }
        c.landWeathering = sum / (0.29 * grid.size() * EARTH_LAND_WEIGHT);
    }
}
