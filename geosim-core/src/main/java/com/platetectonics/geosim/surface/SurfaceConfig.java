package com.platetectonics.geosim.surface;

/**
 * Constants of erosion, deposition and weathering (docs/design/04-surface-processes.md). Rates are in m and
 * Myr; discharge in km3/yr.
 *
 * @param streamPowerK                 fluvial erodibility: rock erosion in m/Myr = K Q^m S^n (Q in km3/yr), for
 *                                     hard rock under forests; with n = 2 it erodes as K = 5,000 with n = 1 would at
 *                                     a 2% slope (D-44)
 * @param dischargeExponent            m
 * @param slopeExponent                n (1 or 2 are solved in closed form, other values by Newton steps); 2, so
 *                                     gentle lowlands erode slowly and keep their height above the sea (Harel et
 *                                     al. 2016; D-44)
 * @param reliefErosionK               sub-grid hillslope erosion: rock m/Myr = k h^2 (h = km above sea level) x
 *                                     wetness; the grid cannot resolve the steep slopes that do most of the work
 *                                     in mountains (Ahnert 1970; Montgomery &amp; Brandon 2002)
 * @param transportK                   river transport capacity, km3/Myr per (km3/yr x slope)
 * @param softSedimentFactor           sediment and soft cover erode this much faster than crystalline rock
 * @param bareLandFactor               erodibility before land plants (braided rivers, no roots)
 * @param glacialFactor                extra erodibility under thick ice (fjords, overdeepened valleys)
 * @param shelfDepthM                  shelf break: river shelves slope down to this depth (D-41)
 * @param shelfRadiusKm                shelf radius on a passive margin for a river of the step's median supply
 * @param shelfSupplyExponent          the radius grows as (supply / median)^this
 * @param shelfRadiusMinKm             ... but stays between this
 * @param shelfRadiusMaxKm             ... and this
 * @param activeMarginKm               a trench or arc this close to a mouth makes its margin active: a shelf one
 *                                     cell wide
 * @param rampTopM                     a shelf fills to this depth at the mouth, sloping to the shelf break at its
 *                                     radius
 * @param bypassFraction               share of a mouth's sediment that goes straight to its fan (canyons, gravity
 *                                     flows), even while the shelf has room
 * @param fanLengthKm                  turbidite fan e-folding length for a river of median supply (fan shares fall
 *                                     off as e^(-d/L) out to 3 L; Somme et al. 2009)
 * @param fanSupplyExponent            the length grows as (supply / median)^this
 * @param fanLengthMinKm               ... but stays between this
 * @param fanLengthMaxKm               ... and this
 * @param slopeMPerKm                  fans build no higher than a continental slope falling this much per km beyond
 *                                     the shelf radius ...
 * @param slopeFootM                   ... down to this far below the shelf break ...
 * @param riseMPerKm                   ... then a continental rise falling this much per km (no plateaus)
 * @param deltaTopM                    the delta top builds the shelf up to this height above sea level: land
 * @param deltaRadiusKm                ... on the shelf cells this close to the mouth (0: the mouth cell alone)
 * @param pelagicMPerMyr               clay and dust settling everywhere at sea
 * @param oozeMPerMyr                  carbonate ooze above the carbonate compensation depth once calcareous
 *                                     plankton exist
 * @param ccdDepthM                    carbonate compensation depth
 * @param carbonateMPerMyr             reef and platform carbonate at the sea surface in warm water once animals
 *                                     exist
 * @param carbonateDecayM              ... falling off with depth as e^(-z / this) (light; Bosscher &amp; Schlager
 *                                     1992)
 * @param carbonateDepthM              carbonate factories work above this depth
 * @param carbonateMinTempC            ... and in water at least this warm
 * @param carbonateClasticLimitMPerMyr ... but not where river sediment arrives faster than this (mud smothers
 *                                     them)
 * @param carbonateMicrobialFactor     carbonate rate with microbial life only (stromatolite platforms)
 * @param carbonateAbioticFactor       carbonate rate before life (inorganic precipitation)
 * @param soilProductionMPerMyr        bare-rock weathering-front advance in a warm wet climate under forests
 * @param soilDecayDepthM              production falls by e for each this many metres of regolith
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
        double shelfSupplyExponent,
        double shelfRadiusMinKm,
        double shelfRadiusMaxKm,
        double activeMarginKm,
        double rampTopM,
        double bypassFraction,
        double fanLengthKm,
        double fanSupplyExponent,
        double fanLengthMinKm,
        double fanLengthMaxKm,
        double slopeMPerKm,
        double slopeFootM,
        double riseMPerKm,
        double deltaTopM,
        double deltaRadiusKm,
        double pelagicMPerMyr,
        double oozeMPerMyr,
        double ccdDepthM,
        double carbonateMPerMyr,
        double carbonateDecayM,
        double carbonateDepthM,
        double carbonateMinTempC,
        double carbonateClasticLimitMPerMyr,
        double carbonateMicrobialFactor,
        double carbonateAbioticFactor,
        double soilProductionMPerMyr,
        double soilDecayDepthM) {

    public static SurfaceConfig defaults() {
        return new SurfaceConfig(
                250_000.0, // streamPowerK
                0.5,      // dischargeExponent
                2.0,      // slopeExponent
                20.0,     // reliefErosionK
                3.0e5,    // transportK
                2.0,      // softSedimentFactor
                1.5,      // bareLandFactor
                2.0,      // glacialFactor
                120.0,    // shelfDepthM
                100.0,    // shelfRadiusKm
                0.25,     // shelfSupplyExponent
                50.0,     // shelfRadiusMinKm
                250.0,    // shelfRadiusMaxKm
                300.0,    // activeMarginKm
                10.0,     // rampTopM
                0.35,     // bypassFraction
                100.0,    // fanLengthKm
                0.5,      // fanSupplyExponent
                50.0,     // fanLengthMinKm
                400.0,    // fanLengthMaxKm
                50.0,     // slopeMPerKm
                2_000.0,  // slopeFootM
                5.0,      // riseMPerKm
                2.0,      // deltaTopM
                0.0,      // deltaRadiusKm
                2.0,      // pelagicMPerMyr
                6.0,      // oozeMPerMyr
                4_500.0,  // ccdDepthM
                40.0,     // carbonateMPerMyr
                27.0,     // carbonateDecayM
                150.0,    // carbonateDepthM
                20.0,     // carbonateMinTempC
                10.0,     // carbonateClasticLimitMPerMyr
                0.8,      // carbonateMicrobialFactor
                0.4,      // carbonateAbioticFactor
                60.0,     // soilProductionMPerMyr
                5.0);     // soilDecayDepthM
    }
}
