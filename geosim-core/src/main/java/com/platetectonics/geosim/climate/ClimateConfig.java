package com.platetectonics.geosim.climate;

/**
 * Tunable constants of the climate model (docs/design/04-surface-processes.md#climate). Lengths in km.
 *
 * @param equatorOffsetC   sea-level temperature at the equator minus the global mean, for an Earth-like mean
 * @param poleOffsetC      the same at the poles
 * @param profileExponent  between them temperature follows pole + (equator - pole) cos(latitude)^exponent
 * @param decayTropicsKm   moisture e-folding distance inland over bare land, |latitude| below 25 deg
 * @param decayMidKm       ... between 25 and 60 deg
 * @param decayPolarKm     ... above 60 deg
 * @param forestRecycling  share of the rain that a closed forest returns to the air (transpiration); moisture
 *                         then decays over decay / (1 - share), so forests carry rain far inland
 * @param openRecycling    the same for grass, shrubs and early land plants
 * @param reverseFlow      strength of the flow against the prevailing wind (monsoons, cyclones), relative to it
 * @param monsoonFlow      strength of the poleward inflow from the equator side in the tropics (summer
 *                         monsoons), relative to the prevailing wind
 * @param stormTrackFlow   the same in mid-latitudes (storm tracks run poleward)
 * @param itczDecayFactor  over equatorial land moisture lasts 1 + this times longer: the converging trade
 *                         winds of the intertropical convergence zone keep resupplying it
 * @param landConvection   warm forests rain up to (1 + this) times the zonal rate: transpiration feeds their
 *                         own thunderstorms, which keeps rainforests wet
 * @param mixingKm         spread of rain by storms and monsoons beyond the sweep lines (one sigma)
 * @param riseDryingPerKm  rising air loses this share of its moisture (as e-folds) per km climbed above the
 *                         highest ground it has already crossed
 * @param crestRelaxKm     ... which it forgets over this distance (e-folding) downwind
 * @param earthLandWeight  mean weathering weight of a land cell for an Earth-like layout in this model (rain
 *                         relative to 750 mm times warmth): normalizes land weathering to 1 for Earth today
 */
public record ClimateConfig(
        double equatorOffsetC,
        double poleOffsetC,
        double profileExponent,
        double decayTropicsKm,
        double decayMidKm,
        double decayPolarKm,
        double forestRecycling,
        double openRecycling,
        double reverseFlow,
        double monsoonFlow,
        double stormTrackFlow,
        double itczDecayFactor,
        double landConvection,
        double mixingKm,
        double riseDryingPerKm,
        double crestRelaxKm,
        double earthLandWeight) {

    public static ClimateConfig defaults() {
        return new ClimateConfig(
                12.0,     // equatorOffsetC: 27 C at the equator for a 15 C Earth
                -40.0,    // poleOffsetC: -25 C at the poles
                1.1,      // profileExponent: fits Earth's zonal means (0.86, 0.75, 0.60, 0.47 of the range at 30-60 deg)
                4_000.0,  // decayTropicsKm
                3_000.0,  // decayMidKm
                1_500.0,  // decayPolarKm
                0.6,      // forestRecycling: Amazon-like transpiration feedback
                0.25,     // openRecycling
                0.6,      // reverseFlow
                1.0,      // monsoonFlow
                0.7,      // stormTrackFlow
                6.0,      // itczDecayFactor
                0.4,      // landConvection: forested tropics reach ~2,000 mm, like the Amazon and Congo
                250.0,    // mixingKm
                0.25,     // riseDryingPerKm
                1_000.0,  // crestRelaxKm
                0.6);     // earthLandWeight: calibrated at 31.25 km to keep the previous weathering of bare and of vegetated land
    }
}
