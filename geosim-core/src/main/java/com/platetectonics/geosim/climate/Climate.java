package com.platetectonics.geosim.climate;

import com.platetectonics.geosim.grid.CylinderGrid;

/**
 * Climate fields for one moment (docs/design/04-surface-processes.md#climate).
 * All arrays are row-major over the master grid.
 */
public final class Climate {
    public final CylinderGrid grid;
    /** Annual mean temperature at the surface, deg C. */
    public final float[] temperatureC;
    /** Warmest minus coldest month, deg C. */
    public final float[] seasonalRangeC;
    /** Annual precipitation, mm. */
    public final float[] precipitationMm;
    /** Precipitation / potential evaporation (< 0.2 arid, > 0.65 humid). */
    public final float[] aridity;
    /** Ice-sheet thickness, m (0 = no ice). */
    public final float[] iceM;
    /** Distance from the nearest ocean cell, km (0 at sea). */
    public final float[] coastDistanceKm;

    /** Water held in ice, m over the whole planet; each cell counts at a sphere's area for its latitude (D-43). */
    public double iceWaterEquivalentM;
    /** Weathering strength of the land, 1 = Earth today (for the carbon thermostat). */
    public double landWeathering;
    /** Share of the surface above sea level. */
    public double landFraction;
    /** Share of the land under ice. */
    public double iceFraction;

    public Climate(CylinderGrid grid) {
        this.grid = grid;
        int n = grid.size();
        temperatureC = new float[n];
        seasonalRangeC = new float[n];
        precipitationMm = new float[n];
        aridity = new float[n];
        iceM = new float[n];
        coastDistanceKm = new float[n];
    }

    public Biome biome(int idx, double elevationAboveSeaM) {
        return Biome.classify(elevationAboveSeaM, temperatureC[idx], precipitationMm[idx], aridity[idx], iceM[idx]);
    }
}
