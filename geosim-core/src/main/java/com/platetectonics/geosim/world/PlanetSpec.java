package com.platetectonics.geosim.world;

/**
 * Planet size and the mapping between planet coordinates and Minecraft blocks.
 * See docs/design/01-world-model.md: the planet is a cylinder that repeats east-west,
 * poles at the north and south edges, and one block = {@code metersPerBlock} in every direction.
 *
 * @param metersPerBlock  scale factor S (real metres per block, horizontal and vertical)
 * @param circumferenceKm planet circumference C
 */
public record PlanetSpec(double metersPerBlock, double circumferenceKm) {
    public static final double DEFAULT_SCALE = 30.0;
    public static final double EARTH_CIRCUMFERENCE_KM = 40_000.0;
    public static final int SEA_LEVEL_Y = 63;

    // Vertical mapping knees, in block y (see "Vertical mapping" in 01-world-model.md).
    static final double PEAK_KNEE_Y = 296.0;
    static final double PEAK_SOFT_RANGE = 20.0;
    static final double OCEAN_KNEE_Y = -37.0;
    static final double DEEPEST_Y = -60.0;
    static final double DEEPEST_ELEVATION_M = -11_000.0;

    public PlanetSpec {
        if (!(metersPerBlock > 0) || !(circumferenceKm > 0)) {
            throw new IllegalArgumentException("scale and circumference must be positive");
        }
    }

    public static PlanetSpec earth(double metersPerBlock) {
        return new PlanetSpec(metersPerBlock, EARTH_CIRCUMFERENCE_KM);
    }

    /** W: east-west period in blocks. */
    public double blocksAround() {
        return circumferenceKm * 1000.0 / metersPerBlock;
    }

    /** H: pole-to-pole extent in blocks (half the circumference, like a meridian). */
    public double poleToPoleBlocks() {
        return blocksAround() / 2.0;
    }

    /** s: pole-to-equator distance in blocks; also TFC's {@code temperature_scale}. */
    public double poleToEquatorBlocks() {
        return poleToPoleBlocks() / 2.0;
    }

    public double northPoleZ() {
        return -poleToEquatorBlocks() / 2.0;
    }

    public double equatorZ() {
        return poleToEquatorBlocks() / 2.0;
    }

    public double southPoleZ() {
        return 1.5 * poleToEquatorBlocks();
    }

    /** Latitude in degrees (+north), linear in z and clamped to the poles. Matches TFC's convention. */
    public double latitudeDeg(double z) {
        double s = poleToEquatorBlocks();
        double lat = 90.0 * (s / 2.0 - z) / s;
        return Math.max(-90.0, Math.min(90.0, lat));
    }

    /**
     * Present-day elevation (metres relative to sea level) to block y. Linear at {@code metersPerBlock},
     * with a soft cap for the highest peaks and a linear squeeze for the deep ocean.
     */
    public double elevationToY(double elevationM) {
        double yLin = SEA_LEVEL_Y + elevationM / metersPerBlock;
        if (yLin > PEAK_KNEE_Y) {
            return PEAK_KNEE_Y + PEAK_SOFT_RANGE * Math.tanh((yLin - PEAK_KNEE_Y) / PEAK_SOFT_RANGE);
        }
        if (yLin >= OCEAN_KNEE_Y) {
            return yLin;
        }
        double kneeElevation = (OCEAN_KNEE_Y - SEA_LEVEL_Y) * metersPerBlock;
        if (kneeElevation <= DEEPEST_ELEVATION_M) {
            return Math.max(DEEPEST_Y, yLin);
        }
        double t = (kneeElevation - elevationM) / (kneeElevation - DEEPEST_ELEVATION_M);
        t = Math.min(1.0, t);
        return OCEAN_KNEE_Y + (DEEPEST_Y - OCEAN_KNEE_Y) * t;
    }
}
