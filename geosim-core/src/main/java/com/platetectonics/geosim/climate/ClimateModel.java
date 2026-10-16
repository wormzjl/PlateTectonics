package com.platetectonics.geosim.climate;

import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.GlobalState;
import com.platetectonics.geosim.history.Milestone;
import com.platetectonics.geosim.parallel.Parallel;

/**
 * Climate from latitude, the global temperature, elevation and the land-sea layout
 * (docs/design/04-surface-processes.md#climate).
 * <ul>
 *   <li>Temperature: a zonal profile around the global mean (the pole-to-equator contrast shrinks in warm
 *       worlds), minus 6.5 C per km of elevation; continental interiors are colder at high latitude and
 *       have a larger seasonal range.</li>
 *   <li>Wind: trade winds blow west below 30 deg, westerlies east to 60 deg, polar easterlies west.</li>
 *   <li>Moisture is swept along each row with the wind, then poleward along each column (monsoons in the
 *       tropics, storm tracks in mid-latitudes). Air over the sea is saturated; over land it rains out
 *       with distance, more slowly where plants return rain to the air (forests most, judged from the
 *       previous step's climate; bare land before land plants not at all). Rising air rains more and
 *       dries: windward slopes are wet and the lee lies in a rain shadow. Cold high ground holds little
 *       moisture. The zonal base pattern has the wet equator, dry subtropics, wet storm tracks and dry
 *       poles.</li>
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

    private final CylinderGrid grid;
    private final ClimateConfig cfg;
    private final int[] queue;
    private final int[] dist;
    private final float[] mixScratch;
    /** Share of the rain that plants return to the air, per cell, from the previous step's climate. */
    private final float[] recycling;
    /** Forest cover 0..1 times tropical warmth, per cell: where forests drive their own convection. */
    private final float[] convection;
    private final double[] weatheringScratch;
    private Parallel parallel = Parallel.SERIAL;

    public ClimateModel(CylinderGrid grid) {
        this(grid, ClimateConfig.defaults());
    }

    public ClimateModel(CylinderGrid grid, ClimateConfig cfg) {
        this.grid = grid;
        this.cfg = cfg;
        this.recycling = new float[grid.size()];
        this.convection = new float[grid.size()];
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
        double equator = tg + cfg.equatorOffsetC() - 0.3 * (tg - 15.0);
        double pole = tg + cfg.poleOffsetC() + 0.5 * (tg - 15.0);
        parallel.rows(grid.height(), (from, to) -> {
            for (int j = from; j < to; j++) {
                double lat = Math.toRadians(grid.latitudeDeg(j));
                double s = Math.sin(lat);
                double s2 = s * s;
                double sea = pole + (equator - pole) * Math.pow(Math.cos(lat), cfg.profileExponent());
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
        vegetation(g, c);
        // rows are independent: each sweep reads and writes its own row only
        parallel.rows(h, (from, to) -> {
            for (int j = from; j < to; j++) {
                double lat = Math.abs(grid.latitudeDeg(j));
                double zonal = zonal(lat) * wetter;
                int dir = lat < 30.0 ? -1 : lat < 60.0 ? 1 : -1;
                double decayKm = decayKm(lat);
                // prevailing wind, then the weaker flow from the other side (monsoons, cyclones on lee coasts)
                sweep(c, e, j, dir, zonal, decayKm, 1.0, p, false);
                sweep(c, e, j, -dir, zonal, decayKm * 0.6, cfg.reverseFlow(), p, true);
            }
        });
        if (cfg.monsoonFlow() > 0.0 || cfg.stormTrackFlow() > 0.0) {
            // columns are independent: each poleward sweep reads and writes its own column only
            parallel.range(w, 8, (from, to) -> {
                for (int i = from; i < to; i++) {
                    poleward(c, e, i, -1, wetter, p);
                    poleward(c, e, i, 1, wetter, p);
                }
            });
        }
        // storms and monsoons spread rain beyond the sweep lines: three box passes in each direction,
        // together close to a Gaussian of spread mixingKm whatever the cell size
        double sigma = cfg.mixingKm() / cellKm;
        int r = (int) Math.round((Math.sqrt(4.0 * sigma * sigma + 1.0) - 1.0) / 2.0);
        for (int pass = 0; pass < 3 && r > 0; pass++) {
            boxRows(p, mixScratch, r);
            boxColumns(mixScratch, p, r);
        }
        parallel.cells(p.length, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                double pet = Math.max(0.0, 60.0 * (c.temperatureC[idx] + 3.0));
                c.aridity[idx] = (float) (pet <= 1.0 ? 10.0 : Math.min(10.0, p[idx] / pet));
            }
        });
    }

    /** Mean over {@code 2r + 1} cells along each row (rows wrap around the cylinder). */
    private void boxRows(float[] src, float[] dst, int r) {
        int w = grid.width();
        int span = Math.min(r, (w - 1) / 2);
        parallel.rows(grid.height(), (from, to) -> {
            for (int j = from; j < to; j++) {
                int row = j * w;
                double sum = 0.0;
                for (int k = -span; k <= span; k++) {
                    sum += src[row + Math.floorMod(k, w)];
                }
                for (int i = 0; i < w; i++) {
                    dst[row + i] = (float) (sum / (2 * span + 1));
                    sum += src[row + Math.floorMod(i + span + 1, w)] - src[row + Math.floorMod(i - span, w)];
                }
            }
        });
    }

    /** Mean over up to {@code 2r + 1} cells along each column (the window shrinks at the poles). */
    private void boxColumns(float[] src, float[] dst, int r) {
        int w = grid.width();
        int h = grid.height();
        parallel.range(w, 8, (from, to) -> {
            for (int i = from; i < to; i++) {
                double sum = 0.0;
                int count = 0;
                for (int j = 0; j <= Math.min(r, h - 1); j++) {
                    sum += src[j * w + i];
                    count++;
                }
                for (int j = 0; j < h; j++) {
                    dst[j * w + i] = (float) (sum / count);
                    int add = j + r + 1;
                    int drop = j - r;
                    if (add < h) {
                        sum += src[add * w + i];
                        count++;
                    }
                    if (drop >= 0) {
                        sum -= src[drop * w + i];
                        count--;
                    }
                }
            }
        });
    }

    /**
     * Moisture e-folding distance over bare land. Near the equator the converging trade winds of the
     * intertropical convergence zone keep resupplying the air, so moisture lasts {@code 1 + itczDecayFactor}
     * times longer there (fading out from 5 to 15 deg); rising air still dries, so rain shadows remain.
     */
    private double decayKm(double absLat) {
        double base = absLat < 25.0 ? cfg.decayTropicsKm() : absLat < 60.0 ? cfg.decayMidKm() : cfg.decayPolarKm();
        double itcz = Math.max(0.0, Math.min(1.0, (15.0 - absLat) / 10.0));
        return base * (1.0 + cfg.itczDecayFactor() * itcz);
    }

    /**
     * How much rain the plants of each land cell return to the air, judged from the previous step's
     * aridity and this step's temperature: forests where it was moist and warm enough once forests exist,
     * open vegetation once land plants exist, nothing on bare land.
     */
    private void vegetation(GlobalState g, Climate c) {
        boolean plants = g.has(Milestone.LAND_PLANTS);
        boolean forests = g.has(Milestone.FORESTS);
        parallel.cells(recycling.length, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                double r = 0.0;
                double convect = 0.0;
                if (plants && c.iceM[idx] <= 0f) {
                    double warm = clamp((c.temperatureC[idx] + 5.0) / 15.0);
                    double moist = clamp(c.aridity[idx] / 0.5);
                    r = cfg.openRecycling() * moist * warm;
                    if (forests) {
                        double canopy = clamp((c.aridity[idx] - 0.4) / 0.4) * warm;
                        r = Math.max(r, cfg.forestRecycling() * canopy);
                        convect = canopy * clamp((c.temperatureC[idx] - 18.0) / 6.0);
                    }
                }
                recycling[idx] = (float) r;
                convection[idx] = (float) convect;
            }
        });
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    /**
     * Carries moisture poleward along column {@code i} from the equator ({@code dir} -1: north, +1: south),
     * starting from what the row sweeps left at the equator, and keeps the wetter result: summer monsoons
     * in the tropics, storm tracks in mid-latitudes.
     */
    private void poleward(Climate c, float[] e, int i, int dir, double wetter, float[] p) {
        int w = grid.width();
        int h = grid.height();
        double cellKm = grid.cellKm();
        int j = dir < 0 ? h / 2 - 1 : h / 2;
        int idx0 = j * w + i;
        double zonal0 = zonal(Math.abs(grid.latitudeDeg(j))) * wetter;
        double q = e[idx0] <= 0f ? 1.0 : Math.min(1.0, p[idx0] / Math.max(1e-9, zonal0));
        double seaTemp = c.temperatureC[idx0];
        double prevE = Math.max(0.0, e[idx0]);
        double crest = prevE;
        double relax = Math.exp(-cellKm / cfg.crestRelaxKm());
        for (; j >= 0 && j < h; j += dir) {
            int idx = j * w + i;
            double lat = Math.abs(grid.latitudeDeg(j));
            double weight = lat < 25.0 ? cfg.monsoonFlow() : lat < 60.0 ? cfg.stormTrackFlow() : 0.0;
            double elev = e[idx];
            if (elev <= 0.0) {
                q = 1.0;
                seaTemp = c.temperatureC[idx];
                prevE = 0.0;
                crest = 0.0;
                continue;
            }
            double rise = Math.max(0.0, elev - prevE);
            crest *= relax;
            double above = Math.max(0.0, elev - crest);
            crest = Math.max(crest, elev);
            double hold = Math.exp(0.065 * Math.min(0.0, c.temperatureC[idx] - seaTemp));
            double rain = weight * zonal(lat) * wetter * Math.min(q, hold) * (1.0 + 0.02 * rise / cellKm)
                    * (1.0 + cfg.landConvection() * convection[idx]);
            p[idx] = Math.max(p[idx], (float) rain);
            q *= Math.exp(-cellKm * (1.0 - recycling[idx]) / decayKm(lat))
                    * Math.exp(-cfg.riseDryingPerKm() * above / 1_000.0);
            prevE = elev;
        }
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
        double crest = 0.0;
        double relax = Math.exp(-cellKm / cfg.crestRelaxKm());
        for (int step = 0; step < w; step++, i = Math.floorMod(i + dir, w)) {
            int idx = j * w + i;
            double elev = e[idx];
            double rain;
            if (elev <= 0.0) {
                q = 1.0;
                rain = zonal;
                prevE = 0.0;
                crest = 0.0;
            } else {
                double rise = Math.max(0.0, elev - prevE);
                // air dries when lifted above the highest ground it has crossed (which it slowly forgets
                // downwind); climbing back over lower bumps wrings out little, whatever the cell size
                crest *= relax;
                double above = Math.max(0.0, elev - crest);
                crest = Math.max(crest, elev);
                // cold air holds less water (Clausius-Clapeyron, ~6.5% per degree): it limits the rain here;
                // what the air loses crossing high ground is the drying above the crest
                double hold = Math.exp(0.065 * Math.min(0.0, c.temperatureC[idx] - seaTemp));
                double lift = rise / cellKm; // m per km
                rain = zonal * Math.min(q, hold) * (1.0 + 0.02 * lift) * (1.0 + cfg.landConvection() * convection[idx]);
                q *= Math.exp(-cellKm * (1.0 - recycling[idx]) / decayKm)
                        * Math.exp(-cfg.riseDryingPerKm() * above / 1_000.0);
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
        c.landWeathering = sum / (0.29 * grid.size() * cfg.earthLandWeight());
    }
}
