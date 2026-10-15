package com.platetectonics.geosim.tectonics;

/**
 * Tunable constants of the plate model (docs/design/03-tectonics.md). Lengths in km, times in Myr,
 * speeds in km/Myr (10 km/Myr = 1 cm/yr), elevations in m.
 */
public record TectonicsConfig(
        // crust
        double oceanCrustKm,
        double continentThresholdKm,
        double buoyantCrustKm,
        double accretedSedimentFraction,
        double subductionErosionKm3PerKmMyr,
        double referenceContinentKm,
        double maxCrustKm,
        double erosionTimescaleMyr,
        double marginDepositFraction,
        double shelfWidthKm,
        // arcs and crustal growth
        double arcGrowthKmPerMyr,
        double continentalCrustKm3PerKm2,
        double crustGrowthTimescaleMyr,
        double crustGrowthFeedback,
        double arcNearKm,
        double arcFarKm,
        // collision, plateaus and roots (docs/research_notes/Tibetan plateau support)
        double collisionTransfer,
        double underthrustFrontFraction,
        double underthrustNearKm,
        double underthrustFarKm,
        double plateauElevationM,
        double collapseElevationM,
        double maxElevationM,
        double flowOnsetKm,
        double crustSlopeKmPerKm,
        double keelPerCollisionCrust,
        double keelDensityFactor,
        double keelWarmingMyr,
        double lithosphereRegrowthMyr,
        double hotKeelKm,
        double keelMaxKm,
        double eclogiteDepthKm,
        double eclogiteMaxFraction,
        double eclogiteDensityFactor,
        double eclogiteDryTimescaleMyr,
        double eclogiteWetTimescaleMyr,
        double eclogiteWetChance,
        double delaminationCrustKm,
        double delaminationLoadM,
        double delaminationRatePerMyr,
        double delaminationRadiusKm,
        // forces and speeds
        double slabPull,
        double ridgePush,
        double collisionResistance,
        double continentalDrag,
        double speedPresentKmPerMyr,
        double speedEarlyBoost,
        double speedBoostTimescaleMyr,
        double maxSpeedKmPerMyr,
        double velocityRelaxMyr,
        double driftKmPerMyr,
        double driftTimescaleMyr,
        // plumes, rifts, subduction initiation, merges
        double plumeRatePerMyr,
        double plumeRadiusKm,
        double riftSeparationKmPerMyr,
        double supercontinentFraction,
        double supercontinentRiftDelayMyr,
        double marginSplitAgeMyr,
        double marginSplitRatePerMyr,
        double oceanSubductionAgeMyr,
        double oceanSubductionRatePerMyr,
        double collisionMergeTimescaleMyr,
        double collisionMergeDistanceKm,
        // plate count and motion
        int minPlates,
        int maxPlates,
        double minPlateFraction,
        double maxCellsPerSubstep) {

    public static TectonicsConfig defaults() {
        return new TectonicsConfig(
                7.0,     // oceanCrustKm: new crust at ridges
                20.0,    // continentThresholdKm: thicker crust becomes continental
                15.0,    // buoyantCrustKm: thicker crystalline crust (arcs, oceanic plateaus) jams trenches instead of sinking
                0.5,     // accretedSedimentFraction: sediment scraped off a sinking plate into the trench's wedge
                35.0,    // subductionErosionKm3PerKmMyr: forearc crust dragged down (Earth ~1.4 km3/yr over ~40,000 km)
                35.0,    // referenceContinentKm: erosion relaxes thick crust toward this
                88.0,    // maxCrustKm: safety cap (thickest crust on Earth is ~80 km)
                200.0,   // erosionTimescaleMyr
                0.75,    // marginDepositFraction: share of eroded crust added to shelves and basins
                150.0,   // shelfWidthKm: eroded sediment settles on ocean floor this close to the continent

                0.25,    // arcGrowthKmPerMyr: thickening rate in the arc band
                14.1,    // continentalCrustKm3PerKm2: Earth today, 7.2e9 km3 over 5.1e8 km2
                1300.0,  // crustGrowthTimescaleMyr: about 70% of today's crust by 1.5 Gyr (Dhuime et al. 2012)
                1.0,     // crustGrowthFeedback: how hard arc output steers toward the growth curve (0 = off)
                80.0,    // arcNearKm: arc band starts this far behind the trench
                250.0,   // arcFarKm

                0.9,     // collisionTransfer: share of the over-run crust kept (the rest sinks with the slab)
                0.35,    // underthrustFrontFraction: of the over-run crust, stacked into the frontal range
                150.0,   // underthrustNearKm: the rest spreads under the overriding plate this far
                300.0,   // underthrustFarKm: ... to this far behind the suture (India under Tibet)
                5300.0,  // plateauElevationM: collisional plateaus stop rising here; excess crust flows sideways
                1000.0,  // collapseElevationM: hot plateaus spread down to this once convergence stops
                6500.0,  // maxElevationM: cell-mean safety cap
                50.0,    // flowOnsetKm: lower crust under thicker crust is weak enough to flow
                0.15,    // crustSlopeKmPerKm: steepest crust-thickness gradient before the lower crust flows
                2.4,     // keelPerCollisionCrust: km of mantle keel per km of stacked crust (85 km keel / 35 km crust)
                0.013,   // keelDensityFactor: m of surface lowered per m of cold excess keel
                120.0,   // keelWarmingMyr: a thickened keel warms and loses its excess density
                50.0,    // lithosphereRegrowthMyr: hot, thin lithosphere after root removal cools back
                -50.0,   // hotKeelKm: keel state right after delamination (hot asthenosphere: +0.65 km)
                100.0,   // keelMaxKm: keel at most ~doubled; caps removal rebound near 2 km
                50.0,    // eclogiteDepthKm: mafic lower crust below this depth can turn to eclogite
                0.35,    // eclogiteMaxFraction: share of that layer that converts (Tibet ~30%: dry, slow)
                0.13,    // eclogiteDensityFactor: m of surface lowered per m of eclogite
                70.0,    // eclogiteDryTimescaleMyr
                2.0,     // eclogiteWetTimescaleMyr: water lets the reaction run fast
                0.15,    // eclogiteWetChance: share of crustal terranes with a wet lower crust
                55.0,    // delaminationCrustKm: only thick crust can drop its root
                750.0,   // delaminationLoadM: ... and only when the root holds the surface down this much
                1.0 / 20.0, // delaminationRatePerMyr: per patch-sized root area; mean wait ~20 Myr (research R5a)
                200.0,   // delaminationRadiusKm

                1.0,     // slabPull (per convergent boundary edge, scaled by slab age)
                0.2,     // ridgePush (per young divergent edge)
                1.5,     // collisionResistance (per continent-continent convergent edge)
                2.0,     // continentalDrag: extra basal drag for continental keels
                45.0,    // speedPresentKmPerMyr: area-weighted RMS plate speed today (~4.5 cm/yr)
                1.0,     // speedEarlyBoost: early mantle is hotter; RMS speed x (1 + boost) at t = 0
                1500.0,  // speedBoostTimescaleMyr
                150.0,   // maxSpeedKmPerMyr
                10.0,    // velocityRelaxMyr
                12.0,    // driftKmPerMyr: random mantle drag on each plate
                100.0,   // driftTimescaleMyr

                1.0 / 30.0, // plumeRatePerMyr
                500.0,   // plumeRadiusKm
                20.0,    // riftSeparationKmPerMyr
                0.2,     // supercontinentFraction: plates holding this share of all continent count as one
                150.0,   // supercontinentRiftDelayMyr: insulation time before plumes favour it
                160.0,   // marginSplitAgeMyr: old ocean at a passive margin may start subducting
                0.004,   // marginSplitRatePerMyr
                170.0,   // oceanSubductionAgeMyr: ocean floor this old may break and sink on its own
                0.01,    // oceanSubductionRatePerMyr
                60.0,    // collisionMergeTimescaleMyr
                300.0,   // collisionMergeDistanceKm: plates weld after about this much convergence across their contact

                10,      // minPlates
                24,      // maxPlates
                0.002,   // minPlateFraction of the world area
                3.0);    // maxCellsPerSubstep
    }
}
