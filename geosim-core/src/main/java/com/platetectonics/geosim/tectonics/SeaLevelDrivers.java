package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.terrain.SeaLevel;

import java.util.Arrays;

/**
 * Measurement only (CLI {@code --sea-drivers}): how much each driver moves sea level. The ocean is refilled with one
 * driver held fixed (no ice on land; every ocean floor cell at one age; no sediment on the ocean floor), and the
 * difference to the real level is that driver's part. The land share at fixed levels on the datum separates sea-level
 * swings from changes of the continents themselves. The rules before D-43 are also tried on the same surface (first
 * order: the history they would have made differs): ocean depth on the old square-root law, and an ice volume given
 * by the caller (the ice counted at the map's own area).
 */
public final class SeaLevelDrivers {
    /** Levels on the datum at which the land share is recorded, m. */
    public static final double[] LEVELS_M = {-900, -800, -700, -600, -500, -400, -300, -200, -100, 0, 100, 200, 300};
    /** Age the whole ocean floor takes in the fixed-age refill, Myr. */
    public static final double REFERENCE_AGE_MYR = 80.0;

    /**
     * @param seaLevelM      sea level on the datum, m
     * @param iceM           sea level minus the level with no ice on land, m (negative: ice holds water back)
     * @param ageM           sea level minus the level with every ocean floor cell at the reference age, m
     * @param sedimentM      sea level minus the level with no sediment on the ocean floor, m
     * @param iceWaterM      water held in ice, as a depth over the whole planet, m
     * @param landPer100M    land share between 50 m below and 50 m above sea level, percentage points
     * @param medianContinentM median height of continental crust above sea level, m (negative: mostly flooded)
     * @param meanLandM      mean height of land above sea level, m
     * @param landAtLevel    land share above each of {@link #LEVELS_M}
     * @param oldDepthLevelM sea level with the ocean floor at the old square-root depths, m
     * @param otherIceLevelM sea level with the caller's ice volume, m
     * @param bothLevelM     sea level with both, m
     */
    public record Sample(double seaLevelM, double iceM, double ageM, double sedimentM, double iceWaterM,
                         double oceanFraction, double continentalFraction, double meanOceanAgeMyr,
                         double meanOceanSedimentKm, double landFraction, double landPer100M, double medianContinentM,
                         double meanLandM, double[] landAtLevel, double oldDepthLevelM, double otherIceLevelM,
                         double bothLevelM) {
    }

    /** Ocean depth before D-43: 2.5 km plus 0.35 km x sqrt(age in Myr), down to 6 km. */
    public static double oldDepthM(double ageMyr) {
        return Math.min(6_000.0, 2_500.0 + 350.0 * Math.sqrt(Math.max(0.0, ageMyr)));
    }

    private SeaLevelDrivers() {
    }

    /** @param otherIceWaterM ice volume to try instead of the real one, as a depth over the whole planet, m */
    public static Sample measure(PlateSimulation sim, double otherIceWaterM) {
        Lithosphere L = sim.lith;
        int n = L.n;
        float[] e = new float[n];
        float[] fixedAge = new float[n];
        float[] noSediment = new float[n];
        float[] oldDepth = new float[n];
        double reference = Isostasy.oceanDepthM(REFERENCE_AGE_MYR);
        int continental = 0;
        int ocean = 0;
        double ageSum = 0.0;
        double sedimentSum = 0.0;
        for (int idx = 0; idx < n; idx++) {
            double v = Isostasy.elevationM(sim, idx);
            e[idx] = (float) v;
            if (L.type[idx] == Lithosphere.CONTINENTAL) {
                fixedAge[idx] = (float) v;
                noSediment[idx] = (float) v;
                oldDepth[idx] = (float) v;
                continental++;
            } else {
                fixedAge[idx] = (float) (v + Isostasy.oceanDepthM(L.oceanAge[idx]) - reference);
                noSediment[idx] = (float) (v - Isostasy.SEDIMENT_MARINE_FACTOR * L.sediment[idx] * 1000.0);
                oldDepth[idx] = (float) (v + Isostasy.oceanDepthM(L.oceanAge[idx]) - oldDepthM(L.oceanAge[idx]));
                ocean++;
                ageSum += L.oceanAge[idx];
                sedimentSum += L.sediment[idx];
            }
        }
        double water = Math.max(1.0, sim.meanOceanDepthM - sim.iceWaterEquivalentM);
        double level = SeaLevel.fillToVolume(e, water);
        double noIce = SeaLevel.fillToVolume(e, Math.max(1.0, sim.meanOceanDepthM));
        double sameAge = SeaLevel.fillToVolume(fixedAge, water);
        double bare = SeaLevel.fillToVolume(noSediment, water);
        double otherWater = Math.max(1.0, sim.meanOceanDepthM - otherIceWaterM);
        double oldDepthLevel = SeaLevel.fillToVolume(oldDepth, water);
        double otherIce = SeaLevel.fillToVolume(e, otherWater);
        double both = SeaLevel.fillToVolume(oldDepth, otherWater);

        int land = 0;
        int below = 0;
        int above = 0;
        int sea = 0;
        double landHeight = 0.0;
        int[] atLevel = new int[LEVELS_M.length];
        float[] continentHeight = new float[continental];
        int c = 0;
        for (int idx = 0; idx < n; idx++) {
            double v = e[idx];
            if (v > level) {
                land++;
                landHeight += v - level;
            } else if (v < level) {
                sea++;
            }
            if (v > level - 50.0) {
                below++;
            }
            if (v > level + 50.0) {
                above++;
            }
            for (int k = 0; k < LEVELS_M.length; k++) {
                if (v > LEVELS_M[k]) {
                    atLevel[k]++;
                }
            }
            if (L.type[idx] == Lithosphere.CONTINENTAL) {
                continentHeight[c++] = (float) (v - level);
            }
        }
        Arrays.sort(continentHeight);
        double[] landAtLevel = new double[LEVELS_M.length];
        for (int k = 0; k < LEVELS_M.length; k++) {
            landAtLevel[k] = (double) atLevel[k] / n;
        }
        return new Sample(level, level - noIce, level - sameAge, level - bare, sim.iceWaterEquivalentM,
                (double) sea / n, (double) continental / n, ocean == 0 ? 0.0 : ageSum / ocean,
                ocean == 0 ? 0.0 : sedimentSum / ocean, (double) land / n, 100.0 * (below - above) / n,
                continental == 0 ? Double.NaN : continentHeight[continental / 2], land == 0 ? 0.0 : landHeight / land,
                landAtLevel, oldDepthLevel, otherIce, both);
    }
}
