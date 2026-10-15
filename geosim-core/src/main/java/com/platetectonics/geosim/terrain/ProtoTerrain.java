package com.platetectonics.geosim.terrain;

import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.noise.PeriodicNoise;
import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

/**
 * Phase 1 placeholder proto-terrain: seeded fractal relief plus a few raised protocontinental nuclei.
 * Phase 2 replaces this with a crust-thickness field and isostasy (docs/design/03-tectonics.md).
 * Elevations are in metres relative to an arbitrary datum; sea level is found afterwards by volume.
 */
public final class ProtoTerrain {
    private static final int NOISE_PERIOD_X = 8;
    private static final int NOISE_OCTAVES = 8;
    private static final double BASE_ELEVATION_M = -3_000.0;
    private static final double NOISE_AMPLITUDE_M = 3_500.0;
    private static final double NUCLEUS_HEIGHT_M = 3_000.0;

    private ProtoTerrain() {
    }

    public static FloatField generate(CylinderGrid grid, long seed) {
        PeriodicNoise noise = new PeriodicNoise(seed, Streams.PROTO_TERRAIN, NOISE_PERIOD_X, NOISE_OCTAVES, 0.5);
        Nucleus[] nuclei = nuclei(grid, seed);
        FloatField elevation = new FloatField(grid);
        double latticePerCell = (double) NOISE_PERIOD_X / grid.width();
        for (int j = 0; j < grid.height(); j++) {
            for (int i = 0; i < grid.width(); i++) {
                double e = BASE_ELEVATION_M + NOISE_AMPLITUDE_M * noise.sample(i * latticePerCell, j * latticePerCell);
                for (Nucleus n : nuclei) {
                    double dx = wrappedDelta(i - n.i, grid.width());
                    double dy = j - n.j;
                    double r2 = (dx * dx + dy * dy) / (n.radiusCells * n.radiusCells);
                    e += NUCLEUS_HEIGHT_M * Math.exp(-r2);
                }
                elevation.set(i, j, (float) e);
            }
        }
        return elevation;
    }

    private record Nucleus(double i, double j, double radiusCells) {
    }

    private static Nucleus[] nuclei(CylinderGrid grid, long seed) {
        int count = 4 + (int) (Rng.unit(seed, Streams.PROTO_NUCLEI, 0, 0) * 5);
        Nucleus[] out = new Nucleus[count];
        for (int k = 0; k < count; k++) {
            double i = Rng.unit(seed, Streams.PROTO_NUCLEI, 1, k) * grid.width();
            // keep nuclei away from the poles: rows 15% .. 85%
            double j = (0.15 + 0.7 * Rng.unit(seed, Streams.PROTO_NUCLEI, 2, k)) * grid.height();
            double radiusKm = 800.0 + 1_700.0 * Rng.unit(seed, Streams.PROTO_NUCLEI, 3, k);
            out[k] = new Nucleus(i, j, radiusKm / grid.cellKm());
        }
        return out;
    }

    private static double wrappedDelta(double d, int period) {
        d = d % period;
        if (d > period / 2.0) {
            d -= period;
        } else if (d < -period / 2.0) {
            d += period;
        }
        return d;
    }
}
