package com.platetectonics.geosim.rng;

/** Stream ids for {@link Rng}. Each process draws from its own stream so adding one never shifts another. */
public final class Streams {
    public static final long PROTO_TERRAIN = 1;
    public static final long PROTO_NUCLEI = 2;
    public static final long PROTO_AGE = 3;
    public static final long PLATE_SEEDS = 10;
    public static final long PLATE_WARP = 11;
    public static final long DRIFT = 20;
    /** Spin drift of each plate (D-42); uses this stream and the next. */
    public static final long SPIN_DRIFT = 22;
    public static final long PLUME = 30;
    public static final long RIFT = 31;
    public static final long MARGIN_SPLIT = 32;
    public static final long BREAKUP = 33;
    public static final long DELAMINATION = 34;
    /** Fractional advance of the basin-closure front. */
    public static final long BASIN_CLOSURE = 36;
    /** Wet or dry lower crust per crustal terrane (keyed by formation time). */
    public static final long ECLOGITE = 35;
    /** Box-model constants drawn once per world (Sun, mantle, thresholds). */
    public static final long HISTORY = 40;
    /** Waiting times of life and atmosphere milestones. */
    public static final long MILESTONES = 41;
    /** Snowball onset thresholds and durations. */
    public static final long SNOWBALL = 42;
    /** Block-scale terrain detail in chunk generation (not part of the simulation). */
    public static final long TERRAIN_DETAIL = 100;

    private Streams() {
    }
}
