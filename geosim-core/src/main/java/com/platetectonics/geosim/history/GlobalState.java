package com.platetectonics.geosim.history;

/**
 * The planet's global state at one time (docs/design/07-life-atmosphere-sun.md); every geologic rule
 * reads it instead of Earth's calendar (D-6).
 *
 * @param timeMyr          Myr since formation
 * @param luminosity       solar luminosity relative to today's Sun
 * @param mantleTpC        mantle potential temperature, deg C
 * @param degassing        volcanic CO2 output relative to Earth today
 * @param globalTempC      global mean surface temperature, deg C
 * @param pCO2ppm          CO2 needed for that temperature under this Sun and the methane present
 *                         (diagnostic; capped at 1e6 ppm)
 * @param oxygenPal        atmospheric O2 relative to today's level
 * @param organicBurial    organic carbon burial relative to Earth today (the oxygen source)
 * @param methaneGreenhouseC warming from biogenic methane (large before the Great Oxidation Event)
 * @param snowball         a global glaciation is under way
 * @param milestoneTimes   when each {@link Milestone} happened (NaN = not yet), indexed by ordinal
 */
public record GlobalState(double timeMyr, double luminosity, double mantleTpC, double degassing, double globalTempC,
                          double pCO2ppm, double oxygenPal, double organicBurial, double methaneGreenhouseC,
                          boolean snowball, double[] milestoneTimes) {

    public boolean has(Milestone m) {
        double when = milestoneTimes[m.ordinal()];
        return !Double.isNaN(when) && when <= timeMyr;
    }

    public double timeOf(Milestone m) {
        return milestoneTimes[m.ordinal()];
    }

    public OxygenState oxygen() {
        return OxygenState.of(oxygenPal);
    }

    /** Atmospheric oxygen states; deposits gate on them (BIF, red beds, sulfate, uranium). */
    public enum OxygenState {
        /** Below 1e-4 of today's level: anoxic, methane-rich (BIF, detrital uraninite and pyrite). */
        PRE_GOE,
        /** 1e-4 to 0.2 of today's level: after the Great Oxidation Event (the "boring billion" sat near 0.01-0.1). */
        INTERMEDIATE,
        /** Above 20% of today's level: animals, an ozone shield, oxygenated deep ocean. */
        POST_NOE;

        public static OxygenState of(double pal) {
            return pal < 1e-4 ? PRE_GOE : pal < 0.2 ? INTERMEDIATE : POST_NOE;
        }
    }
}
