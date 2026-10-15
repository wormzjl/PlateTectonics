package com.platetectonics.geosim.terrain;

import com.platetectonics.geosim.field.FloatField;

/**
 * Sea level from a fixed water volume ("fill the basins with water"): finds the level L at which
 * the ocean holds the given volume, expressed as a mean depth over the whole planet
 * (Earth: about 1.335e9 km3 over 5.1e8 km2, roughly 2,620 m). All cells have equal area on the cylinder.
 */
public final class SeaLevel {
    public static final double EARTH_MEAN_OCEAN_DEPTH_M = 2_620.0;

    private SeaLevel() {
    }

    public static double fillToVolume(FloatField elevation, double meanOceanDepthM) {
        if (meanOceanDepthM <= 0) {
            return elevation.min();
        }
        double lo = elevation.min();
        double hi = elevation.max() + meanOceanDepthM;
        for (int iter = 0; iter < 60; iter++) {
            double mid = 0.5 * (lo + hi);
            if (meanDepthAt(elevation, mid) < meanOceanDepthM) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return 0.5 * (lo + hi);
    }

    static double meanDepthAt(FloatField elevation, double level) {
        int n = elevation.grid().size();
        double sum = 0.0;
        for (int k = 0; k < n; k++) {
            double d = level - elevation.get(k);
            if (d > 0) {
                sum += d;
            }
        }
        return sum / n;
    }
}
