package com.platetectonics.geosim.tectonics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Which plate sinks at each ocean-ocean convergent boundary. A slab, once sinking, keeps pulling its
 * plate down, so polarity is decided once when two plates first converge (the older ocean floor sinks)
 * and then kept for as long as they keep converging. Without this memory the trench would flip as soon
 * as the incoming plate brought younger floor than the overriding plate's edge, and every young island
 * arc would be dragged down its own trench.
 */
final class SubductionPolarity {
    /** Boundaries not seen converging for this many steps are forgotten. */
    private static final int FORGET_STEPS = 5;

    private final Map<Long, int[]> pairs = new HashMap<>(); // key -> {sinking plate, last step seen}

    /** The sinking plate for plates a and b; {@code ageA} / {@code ageB} decide a new boundary. */
    int sinking(int a, int b, float ageA, float ageB, int step) {
        long key = PairCounter.key(a, b);
        int[] entry = pairs.get(key);
        if (entry == null) {
            int sink = ageA > ageB || (ageA == ageB && a > b) ? a : b;
            entry = new int[]{sink, step};
            pairs.put(key, entry);
        }
        entry[1] = step;
        return entry[0];
    }

    /** The remembered sinking plate, or -1 if the pair has no established trench. */
    int known(int a, int b) {
        int[] entry = pairs.get(PairCounter.key(a, b));
        return entry == null ? -1 : entry[0];
    }

    void forgetStale(int step) {
        Iterator<int[]> it = pairs.values().iterator();
        while (it.hasNext()) {
            if (step - it.next()[1] > FORGET_STEPS) {
                it.remove();
            }
        }
    }
}
