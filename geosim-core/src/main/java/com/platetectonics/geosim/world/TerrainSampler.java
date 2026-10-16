package com.platetectonics.geosim.world;

import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.noise.PeriodicNoise;
import com.platetectonics.geosim.rng.Streams;

/**
 * Today's planet at block resolution, for chunk generation (Phase 4 stand-in for the Tier 1.5 refinement in
 * docs/design/01-world-model.md#refinement-levels): elevation is a bicubic (Catmull-Rom) interpolation of
 * the master cells plus fractal detail whose amplitude follows the local relief, so the large shapes match
 * the map preview exactly and the detail only roughens them. Climate is bilinear, and temperature follows
 * the detail with the lapse rate. Immutable and thread-safe; chunk workers call it concurrently.
 */
public final class TerrainSampler {
    private static final double LAPSE_C_PER_M = 6.5 / 1000.0;
    /** Detail lattice: this many lattice cells per master cell for the first octave. */
    private static final int DETAIL_PER_CELL = 4;

    private final MasterWorld world;
    private final PlanetSpec spec;
    private final CylinderGrid grid;
    private final int w;
    private final int h;
    private final double blocksToCells;
    private final double northZ;
    private final PeriodicNoise detail;
    /** Elevation range of each cell's 3 x 3 neighbourhood, m. */
    private final float[] relief;

    public TerrainSampler(MasterWorld world, PlanetSpec spec) {
        this.world = world;
        this.spec = spec;
        this.grid = world.grid;
        this.w = grid.width();
        this.h = grid.height();
        this.blocksToCells = spec.metersPerBlock() / 1000.0 / grid.cellKm();
        this.northZ = spec.northPoleZ();
        this.detail = new PeriodicNoise(world.settings.seed(), Streams.TERRAIN_DETAIL, w * DETAIL_PER_CELL, 6, 0.5);
        this.relief = new float[grid.size()];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                float lo = Float.MAX_VALUE;
                float hi = -Float.MAX_VALUE;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        float e = world.elevationM[grid.index(i + di, j + dj)];
                        lo = Math.min(lo, e);
                        hi = Math.max(hi, e);
                    }
                }
                relief[j * w + i] = hi - lo;
            }
        }
    }

    public MasterWorld world() {
        return world;
    }

    public PlanetSpec spec() {
        return spec;
    }

    /** Column coordinate in cells (cell centres at k + 0.5), unwrapped. */
    public double u(double blockX) {
        return blockX * blocksToCells;
    }

    /** Row coordinate in cells from the north pole, clamped to the grid. */
    public double v(double blockZ) {
        return Math.max(0.0, Math.min(h, (blockZ - northZ) * blocksToCells));
    }

    /** Master cell under a block. */
    public int cellIndex(double blockX, double blockZ) {
        return grid.index((int) Math.floor(u(blockX)), (int) Math.floor(v(blockZ)));
    }

    /** Smooth elevation from the master cells alone (what the preview shows), m above sea level. */
    public double baseElevationM(double blockX, double blockZ) {
        return bicubic(world.elevationM, u(blockX) - 0.5, v(blockZ) - 0.5);
    }

    /** Elevation with block-scale detail, m above sea level. */
    public double elevationM(double blockX, double blockZ) {
        double u = u(blockX);
        double v = v(blockZ);
        double base = bicubic(world.elevationM, u - 0.5, v - 0.5);
        double r = bilinear(relief, u - 0.5, v - 0.5);
        // rough where the master relief is high, gentle on plains and abyssal floors; calmer near the coast
        double amplitude = base >= 0.0 ? 12.0 + 0.18 * r : 4.0 + 0.04 * r;
        amplitude = Math.min(amplitude, 900.0);
        double coast = Math.min(1.0, Math.abs(base) / 250.0);
        amplitude *= 0.35 + 0.65 * coast;
        double n = detail.sample(u * DETAIL_PER_CELL, v * DETAIL_PER_CELL);
        return base + amplitude * n;
    }

    /** Elevation range of the master cells around a point (local relief), m. */
    public double reliefM(double blockX, double blockZ) {
        return bilinear(relief, u(blockX) - 0.5, v(blockZ) - 0.5);
    }

    /** Block y of the rock surface. */
    public double surfaceY(double elevationM) {
        return spec.elevationToY(elevationM);
    }

    /** Ice-sheet thickness, m (smooth, so sheet edges are rounded rather than following cell squares). */
    public double iceM(double blockX, double blockZ) {
        return Math.max(0.0, bicubic(world.iceM, u(blockX) - 0.5, v(blockZ) - 0.5));
    }

    /** Annual mean temperature at a point of the given elevation, deg C. */
    public double temperatureC(double blockX, double blockZ, double elevationM) {
        double u = u(blockX) - 0.5;
        double v = v(blockZ) - 0.5;
        double cellMean = bilinear(world.elevationM, u, v);
        double t = bilinear(world.temperatureC, u, v);
        // the cell value is for the cell's mean height; land detail is warmer in valleys, colder on peaks
        if (elevationM > 0.0) {
            t -= LAPSE_C_PER_M * (elevationM - Math.max(0.0, cellMean));
        }
        return t;
    }

    public double precipitationMm(double blockX, double blockZ) {
        return Math.max(0.0, bilinear(world.precipitationMm, u(blockX) - 0.5, v(blockZ) - 0.5));
    }

    public double seasonalRangeC(double blockX, double blockZ) {
        return bilinear(world.seasonalRangeC, u(blockX) - 0.5, v(blockZ) - 0.5);
    }

    public double aridity(double blockX, double blockZ) {
        return Math.max(0.0, bilinear(world.aridity, u(blockX) - 0.5, v(blockZ) - 0.5));
    }

    /** Climate class at a point, from the interpolated climate and the detailed elevation. */
    public Biome biome(double blockX, double blockZ) {
        double e = elevationM(blockX, blockZ);
        return Biome.classify(e, temperatureC(blockX, blockZ, e), precipitationMm(blockX, blockZ),
                aridity(blockX, blockZ), iceM(blockX, blockZ));
    }

    /** Latitude, degrees north. */
    public double latitudeDeg(double blockZ) {
        return spec.latitudeDeg(blockZ);
    }

    // ------------------------------------------------------------------ interpolation on the cylinder

    private float at(float[] f, int i, int j) {
        return f[(j < 0 ? 0 : j >= h ? h - 1 : j) * w + Math.floorMod(i, w)];
    }

    /** Bilinear between cell centres; {@code x, y} in cell units with centres at integers. */
    double bilinear(float[] f, double x, double y) {
        int i = (int) Math.floor(x);
        int j = (int) Math.floor(y);
        double fx = x - i;
        double fy = y - j;
        double a = at(f, i, j) + (at(f, i + 1, j) - at(f, i, j)) * fx;
        double b = at(f, i, j + 1) + (at(f, i + 1, j + 1) - at(f, i, j + 1)) * fx;
        return a + (b - a) * fy;
    }

    /**
     * Catmull-Rom bicubic between cell centres, clamped to the four surrounding cells: an unclamped spline
     * overshoots at steps (a shelf next to the abyss rises above the sea as a chain of islands, a lone peak
     * sits in a moat).
     */
    double bicubic(float[] f, double x, double y) {
        int i = (int) Math.floor(x);
        int j = (int) Math.floor(y);
        double fx = x - i;
        double fy = y - j;
        double r0 = cubic(at(f, i - 1, j - 1), at(f, i, j - 1), at(f, i + 1, j - 1), at(f, i + 2, j - 1), fx);
        double r1 = cubic(at(f, i - 1, j), at(f, i, j), at(f, i + 1, j), at(f, i + 2, j), fx);
        double r2 = cubic(at(f, i - 1, j + 1), at(f, i, j + 1), at(f, i + 1, j + 1), at(f, i + 2, j + 1), fx);
        double r3 = cubic(at(f, i - 1, j + 2), at(f, i, j + 2), at(f, i + 1, j + 2), at(f, i + 2, j + 2), fx);
        double v = cubic(r0, r1, r2, r3, fy);
        double a = at(f, i, j);
        double b = at(f, i + 1, j);
        double c = at(f, i, j + 1);
        double d = at(f, i + 1, j + 1);
        double lo = Math.min(Math.min(a, b), Math.min(c, d));
        double hi = Math.max(Math.max(a, b), Math.max(c, d));
        return Math.max(lo, Math.min(hi, v));
    }

    private static double cubic(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3 + t * (3.0 * (p1 - p2) + p3 - p0)));
    }
}
