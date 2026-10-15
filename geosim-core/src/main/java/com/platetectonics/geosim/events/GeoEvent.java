package com.platetectonics.geosim.events;

/**
 * One entry in the append-only event log (docs/design/02-simulation-pipeline.md).
 *
 * @param type   what happened
 * @param timeMyr when, in Myr since planet formation
 * @param cell   grid cell index where it happened (or -1)
 * @param plateA main plate involved (or -1)
 * @param plateB second plate involved (or -1)
 * @param value  type-specific magnitude (e.g. cells affected)
 */
public record GeoEvent(Type type, double timeMyr, int cell, int plateA, int plateB, double value) {
    public enum Type {
        /** Mantle plume head arrives (large igneous province). */
        PLUME,
        /** A plate splits along a rift. */
        RIFT,
        /** Old ocean breaks from a passive margin and starts subducting. */
        SUBDUCTION_START,
        /** Two plates weld together after a sustained continental collision. */
        COLLISION_MERGE,
        /** A tiny plate is absorbed by a neighbour. */
        ABSORB,
        /** The largest plate breaks up because too few plates remain. */
        BREAKUP,
        /** Dense lithospheric root drops off beneath thickened crust. */
        DELAMINATION
    }
}
