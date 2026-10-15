package com.platetectonics.geosim.rng;

/** Stream ids for {@link Rng}. Each process draws from its own stream so adding one never shifts another. */
public final class Streams {
    public static final long PROTO_TERRAIN = 1;
    public static final long PROTO_NUCLEI = 2;

    private Streams() {
    }
}
