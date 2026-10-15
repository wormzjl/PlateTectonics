package com.platetectonics.geosim;

import com.platetectonics.geosim.noise.PeriodicNoise;
import com.platetectonics.geosim.rng.Rng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RngAndNoiseTest {
    @Test
    void hashIsAPureFunction() {
        assertEquals(Rng.hash(1, 2, 3, 4), Rng.hash(1, 2, 3, 4));
        assertNotEquals(Rng.hash(1, 2, 3, 4), Rng.hash(1, 3, 3, 4));
        assertNotEquals(Rng.hash(1, 2, 3, 4), Rng.hash(2, 2, 3, 4));
    }

    @Test
    void unitValuesAreUniformEnough() {
        int n = 200_000;
        double sum = 0;
        for (int k = 0; k < n; k++) {
            double u = Rng.unit(7, 1, 0, k);
            assertTrue(u >= 0.0 && u < 1.0);
            sum += u;
        }
        assertEquals(0.5, sum / n, 0.005);
    }

    @Test
    void noiseRepeatsExactlyInX() {
        PeriodicNoise noise = new PeriodicNoise(99, 1, 8, 6, 0.5);
        for (int k = 0; k < 500; k++) {
            double x = Rng.unit(5, 9, 0, k) * 8.0;
            double y = Rng.unit(5, 9, 1, k) * 4.0;
            assertEquals(noise.sample(x, y), noise.sample(x + 8.0, y), 1e-12);
            double v = noise.sample(x, y);
            assertTrue(v >= -1.0 && v <= 1.0);
        }
    }
}
