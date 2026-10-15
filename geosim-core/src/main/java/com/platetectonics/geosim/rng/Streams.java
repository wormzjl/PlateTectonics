package com.platetectonics.geosim.rng;

/** Stream ids for {@link Rng}. Each process draws from its own stream so adding one never shifts another. */
public final class Streams {
    public static final long PROTO_TERRAIN = 1;
    public static final long PROTO_NUCLEI = 2;
    public static final long PROTO_AGE = 3;
    public static final long PLATE_SEEDS = 10;
    public static final long PLATE_WARP = 11;
    public static final long DRIFT = 20;
    public static final long PLUME = 30;
    public static final long RIFT = 31;
    public static final long MARGIN_SPLIT = 32;
    public static final long BREAKUP = 33;
    public static final long DELAMINATION = 34;
    /** Wet or dry lower crust per crustal terrane (keyed by formation time). */
    public static final long ECLOGITE = 35;

    private Streams() {
    }
}
