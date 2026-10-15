package com.platetectonics.geosim;

/** Version stamp written into every simulation output (see docs/design/02-simulation-pipeline.md). */
public final class GeoSim {
    /** Bump whenever a change alters simulation results for the same seed and settings. */
    public static final String SIM_VERSION = "0.1.0";

    private GeoSim() {
    }
}
