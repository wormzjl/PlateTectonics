package com.platetectonics.geosim.rng;

/**
 * Counter-based randomness (design decision D-5): every random value is a pure function of
 * (world seed, stream, step, cell). There is no mutable generator state, so results never depend on
 * thread count or evaluation order.
 */
public final class Rng {
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private Rng() {
    }

    /** SplitMix64 finalizer: a strong 64-bit mixing function. */
    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long hash(long seed, long stream, long step, long cell) {
        long h = mix64(seed + GOLDEN);
        h = mix64(h ^ (stream + GOLDEN * 2));
        h = mix64(h ^ (step + GOLDEN * 3));
        return mix64(h ^ (cell + GOLDEN * 4));
    }

    /** Uniform double in [0, 1) from the top 53 bits of a hash. */
    public static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    public static double unit(long seed, long stream, long step, long cell) {
        return unit(hash(seed, stream, step, cell));
    }

    /** Uniform double in [-1, 1). */
    public static double signed(long seed, long stream, long step, long cell) {
        return unit(seed, stream, step, cell) * 2.0 - 1.0;
    }
}
