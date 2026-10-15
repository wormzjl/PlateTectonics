package com.platetectonics.geosim.terrain;

import com.platetectonics.geosim.field.FloatField;

/**
 * Sea level from a fixed water volume ("fill the basins with water"): finds the level L at which
 * the ocean holds the given volume, expressed as a mean depth over the whole planet
 * (Earth: about 1.335e9 km3 over 5.1e8 km2, roughly 2,620 m). All cells have equal area on the cylinder.
 */
public final class SeaLevel {
    public static final double EARTH_MEAN_OCEAN_DEPTH_M = 2_620.0;
    /** Histogram bin for the fast fill, m. */
    private static final double BIN_M = 1.0;

    private SeaLevel() {
    }

    public static double fillToVolume(FloatField elevation, double meanOceanDepthM) {
        int n = elevation.grid().size();
        float[] e = new float[n];
        for (int k = 0; k < n; k++) {
            e[k] = elevation.get(k);
        }
        return fillToVolume(e, meanOceanDepthM);
    }

    /**
     * Histogram fill in O(cells + range / 1 m): accumulate count and sum per 1 m bin, walk up until the
     * water held reaches the target, then solve the last bin exactly as if its cells were all flooded.
     */
    public static double fillToVolume(float[] elevation, double meanOceanDepthM) {
        int n = elevation.length;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float v : elevation) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (meanOceanDepthM <= 0) {
            return min;
        }
        int bins = (int) Math.ceil((max - min) / BIN_M) + 1;
        long[] count = new long[bins];
        double[] sum = new double[bins];
        for (float v : elevation) {
            int b = (int) ((v - min) / BIN_M);
            count[b]++;
            sum[b] += v;
        }
        double target = meanOceanDepthM * n;
        long c = 0;
        double s = 0.0;
        for (int b = 0; b < bins; b++) {
            c += count[b];
            s += sum[b];
            double top = min + (b + 1) * BIN_M;
            if (b == bins - 1 || c * top - s >= target) {
                // every cell up to this bin is under water: c * L - s = target
                return (target + s) / c;
            }
        }
        return max + meanOceanDepthM;
    }
}
