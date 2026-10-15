package com.platetectonics.geosim.noise;

import com.platetectonics.geosim.rng.Rng;

/**
 * Fractal value noise that repeats exactly in x, so maps join seamlessly across the
 * east-west wrap. Coordinates are in base-lattice units; the base period is {@code periodX}
 * lattice cells, and every octave doubles both frequency and period so all octaves stay periodic.
 */
public final class PeriodicNoise {
    private final long seed;
    private final long stream;
    private final int periodX;
    private final int octaves;
    private final double persistence;

    public PeriodicNoise(long seed, long stream, int periodX, int octaves, double persistence) {
        if (periodX < 1 || octaves < 1) {
            throw new IllegalArgumentException("periodX and octaves must be >= 1");
        }
        this.seed = seed;
        this.stream = stream;
        this.periodX = periodX;
        this.octaves = octaves;
        this.persistence = persistence;
    }

    /** Value in [-1, 1]. */
    public double sample(double x, double y) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        for (int o = 0; o < octaves; o++) {
            int f = 1 << o;
            sum += amp * value(o, x * f, y * f, periodX * f);
            norm += amp;
            amp *= persistence;
        }
        return sum / norm;
    }

    private double value(int octave, double x, double y, int period) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double sx = fade(x - x0);
        double sy = fade(y - y0);
        double v00 = lattice(octave, x0, y0, period);
        double v10 = lattice(octave, x0 + 1, y0, period);
        double v01 = lattice(octave, x0, y0 + 1, period);
        double v11 = lattice(octave, x0 + 1, y0 + 1, period);
        double a = v00 + (v10 - v00) * sx;
        double b = v01 + (v11 - v01) * sx;
        return a + (b - a) * sy;
    }

    private double lattice(int octave, int x, int y, int period) {
        int ix = Math.floorMod(x, period);
        long key = ((long) y << 32) ^ (ix & 0xFFFFFFFFL);
        return Rng.signed(seed, stream, octave, key);
    }

    /** Quintic smoothstep (C2-continuous). */
    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }
}
