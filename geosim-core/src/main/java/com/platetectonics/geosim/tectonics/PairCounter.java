package com.platetectonics.geosim.tectonics;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** Symmetric counters keyed by an unordered plate pair. Iteration is in key order (deterministic). */
final class PairCounter {
    private final Map<Long, Double> values = new HashMap<>();

    static long key(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    static int first(long key) {
        return (int) (key >>> 32);
    }

    static int second(long key) {
        return (int) key;
    }

    void add(int a, int b, double amount) {
        values.merge(key(a, b), amount, Double::sum);
    }

    double get(int a, int b) {
        return values.getOrDefault(key(a, b), 0.0);
    }

    void scale(double factor) {
        values.replaceAll((k, v) -> v * factor);
        values.values().removeIf(v -> v < 1e-3);
    }

    void addAll(PairCounter other) {
        other.values.forEach((k, v) -> values.merge(k, v, Double::sum));
    }

    /** Removes every pair involving {@code plate}. */
    void forget(int plate) {
        values.keySet().removeIf(k -> first(k) == plate || second(k) == plate);
    }

    void clear() {
        values.clear();
    }

    /** Snapshot sorted by key, for deterministic iteration. */
    TreeMap<Long, Double> sorted() {
        return new TreeMap<>(values);
    }
}
