package com.platetectonics.geosim.surface;

/**
 * Constants of erosion, deposition and weathering (docs/design/04-surface-processes.md). Rates are in m and
 * Myr; discharge in km3/yr.
 *
 * @param streamPowerK          fluvial erodibility: rock erosion in m/Myr = K Q^m S^n (Q in km3/yr), for
 *                              hard rock under forests
 * @param dischargeExponent     m
 * @param slopeExponent         n (1 or 2 are solved in closed form, other values by Newton steps)
 * @param reliefErosionK        sub-grid hillslope erosion: rock m/Myr = k h^2 (h = km above sea level) x
 *                              wetness; the grid cannot resolve the steep slopes that do most of the work in
 *                              mountains (Ahnert 1970; Montgomery &amp; Brandon 2002)
 * @param transportK            river transport capacity, km3/Myr per (km3/yr x slope)
 * @param softSedimentFactor    sediment and soft cover erode this much faster than crystalline rock
 * @param bareLandFactor        erodibility before land plants (braided rivers, no roots)
 * @param glacialFactor         extra erodibility under thick ice (fjords, overdeepened valleys)
 * @param shelfDepthM           river sediment builds shelves up to this depth below sea level
 * @param shelfRadiusKm         shelf building reaches this far from a river mouth
 * @param apronRadiusKm         the rest spreads as turbidite aprons this far out
 * @param pelagicMPerMyr        clay and dust settling everywhere at sea
 * @param oozeMPerMyr           carbonate ooze above the carbonate compensation depth once calcareous plankton exist
 * @param ccdDepthM             carbonate compensation depth
 * @param carbonateMPerMyr      reef and platform carbonate in warm shallow seas once animals exist (less before)
 * @param carbonateDepthM       carbonate factories work above this depth
 * @param carbonateMinTempC     ... and in water at least this warm
 * @param soilProductionMPerMyr bare-rock weathering-front advance in a warm wet climate under forests
 * @param soilDecayDepthM       production falls by e for each this many metres of regolith
 */
public record SurfaceConfig(
        double streamPowerK,
        double dischargeExponent,
        double slopeExponent,
        double reliefErosionK,
        double transportK,
        double softSedimentFactor,
        double bareLandFactor,
        double glacialFactor,
        double shelfDepthM,
        double shelfRadiusKm,
        double apronRadiusKm,
        double pelagicMPerMyr,
        double oozeMPerMyr,
        double ccdDepthM,
        double carbonateMPerMyr,
        double carbonateDepthM,
        double carbonateMinTempC,
        double soilProductionMPerMyr,
        double soilDecayDepthM) {

    public static SurfaceConfig defaults() {
        return new SurfaceConfig(
                5_000.0,  // streamPowerK
                0.5,      // dischargeExponent
                1.0,      // slopeExponent
                20.0,     // reliefErosionK
                3.0e5,    // transportK
                2.0,      // softSedimentFactor
                1.5,      // bareLandFactor
                2.0,      // glacialFactor
                120.0,    // shelfDepthM
                300.0,    // shelfRadiusKm
                900.0,    // apronRadiusKm
                2.0,      // pelagicMPerMyr
                6.0,      // oozeMPerMyr
                4_500.0,  // ccdDepthM
                40.0,     // carbonateMPerMyr
                150.0,    // carbonateDepthM
                20.0,     // carbonateMinTempC
                60.0,     // soilProductionMPerMyr
                5.0);     // soilDecayDepthM
    }
}
