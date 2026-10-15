package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.terrain.SeaLevel;

/**
 * Elevation from crust and lithosphere properties (docs/design/03-tectonics.md#isostasy-and-elevation).
 * Elevations are on a fixed datum; sea level is then found by filling the oceans to their water volume.
 * <p>
 * Continents: {@code e = 0.1515 (H - 33) - 0.013 keel - 0.13 eclogite} (km), i.e. Airy buoyancy of the
 * crust, minus the pull of a cold excess mantle keel (a hot, thinned keel with negative excess pushes
 * up), minus the pull of eclogitized lower crust. This fits both southern Tibet (~75 km crust with an
 * eclogite root) and northern Tibet (~60 km crust over hot mantle) at ~5 km.
 */
public final class Isostasy {
    /** Airy factor for continental crust: (1 - rho_crust / rho_mantle) with 2.8 / 3.3 g/cm3. */
    public static final double AIRY_FACTOR = 1.0 - 2.8 / 3.3;
    /** Continental crust of this thickness stands at the datum. */
    public static final double DATUM_CRUST_KM = 33.0;
    public static final double MAX_OCEAN_DEPTH_M = 6_000.0;

    private Isostasy() {
    }

    /** Ocean floor deepens as it cools: 2.5 km at the ridge, plus 0.35 km x sqrt(age in Myr), down to ~6 km. */
    public static double oceanDepthM(double ageMyr) {
        return Math.min(MAX_OCEAN_DEPTH_M, 2_500.0 + 350.0 * Math.sqrt(Math.max(0.0, ageMyr)));
    }

    public static double elevationM(boolean continental, double thicknessKm, double oceanAgeMyr) {
        return elevationM(continental, thicknessKm, oceanAgeMyr, 0.0);
    }

    /** @param rootPullM how far keel and eclogite hold the surface down (negative: a hot keel pushes up) */
    public static double elevationM(boolean continental, double thicknessKm, double oceanAgeMyr, double rootPullM) {
        if (continental) {
            return AIRY_FACTOR * (thicknessKm - DATUM_CRUST_KM) * 1000.0 - rootPullM;
        }
        // oceanic plateaus and island arcs: thicker ocean crust floats higher
        double plateau = Math.max(0.0, thicknessKm - 7.0) * AIRY_FACTOR * 1000.0;
        return -oceanDepthM(oceanAgeMyr) + plateau - rootPullM;
    }

    /** Keel and eclogite pull of one cell, m. */
    static double rootPullM(PlateSimulation sim, int idx) {
        Lithosphere L = sim.lith;
        return 1000.0 * (sim.cfg.keelDensityFactor() * L.keel[idx] + sim.cfg.eclogiteDensityFactor() * L.eclogite[idx]);
    }

    /** Elevation on the datum for one cell, capped at {@code maxElevationM} above the current sea level. */
    static double elevationM(PlateSimulation sim, int idx) {
        Lithosphere L = sim.lith;
        double e = elevationM(L.type[idx] == Lithosphere.CONTINENTAL, L.thick[idx], L.oceanAge[idx], rootPullM(sim, idx));
        return Math.min(e, sim.seaLevelM + sim.cfg.maxElevationM());
    }

    /** Crust thickness at which a continental cell would stand at {@code elevationM}, given its roots. */
    static double thicknessForElevationKm(PlateSimulation sim, int idx, double elevationM) {
        return DATUM_CRUST_KM + (elevationM + rootPullM(sim, idx)) / (AIRY_FACTOR * 1000.0);
    }

    /** Elevation on the datum for every cell. */
    public static FloatField elevation(PlateSimulation sim) {
        Lithosphere L = sim.lith;
        FloatField out = new FloatField(L.grid);
        for (int idx = 0; idx < L.n; idx++) {
            out.set(idx, (float) elevationM(sim, idx));
        }
        return out;
    }

    /** Elevation relative to sea level, with the ocean filled to {@code meanOceanDepthM} of water. */
    public static FloatField elevationAboveSea(PlateSimulation sim, double meanOceanDepthM) {
        Lithosphere L = sim.lith;
        FloatField e = elevation(sim);
        double sea = SeaLevel.fillToVolume(e, meanOceanDepthM);
        for (int idx = 0; idx < L.n; idx++) {
            e.set(idx, (float) (e.get(idx) - sea));
        }
        return e;
    }
}
