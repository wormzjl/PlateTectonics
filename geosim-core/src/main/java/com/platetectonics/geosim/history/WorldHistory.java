package com.platetectonics.geosim.history;

import com.platetectonics.geosim.events.EventLog;
import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

import java.util.Arrays;

/**
 * Cheap 0-D box models for the whole planet (docs/design/07-life-atmosphere-sun.md), coupled through life:
 * the Sun brightens, the mantle cools, biological innovations arrive at random, and life drives both the
 * oxygen and the carbon cycle. Updated once per simulation step.
 * <p>
 * <b>Oxygen</b> (a Goldblatt et al. 2006-style bistable balance, relative units, 1 = Earth today):
 * <ul>
 *   <li>source: organic carbon burial = productivity x nutrients x burial efficiency. Productivity steps up
 *       with each innovation (cyanobacteria, eukaryotes, algae, animals, land plants). Nutrients
 *       (phosphorus) come from weathering of continental rock, so warm, wet, mountainous land feeds the
 *       sea, and so does the meltwater after a snowball. Burial efficiency grows with sediment supply;</li>
 *   <li>sinks: reduced volcanic gases (H2, CO, H2S), which fade as the mantle cools, and oxidative
 *       weathering of old organic matter and pyrite (~ erosion x sqrt(O2));</li>
 *   <li>the Great Oxidation Event happens when burial first outpaces the reduced gases. Methane then
 *       collapses and an ozone layer forms, which makes the reduced-gas sink less efficient: oxygen jumps
 *       to a few percent of today's level and stays there even if burial dips (hysteresis). The
 *       Neoproterozoic Oxygenation Event follows when algae, a snowball's nutrient flush or big orogenies
 *       push burial high enough. Oxygen falls back if burial collapses. Under a snowball, burial and
 *       oxidative weathering both nearly stop, so oxygen holds.</li>
 * </ul>
 * <b>Carbon thermostat.</b> Over a few hundred kyr, silicate weathering plus net organic burial remove CO2
 * as fast as volcanoes add it, so the model solves for the temperature at which they balance. Weathering
 * grows with temperature (x e per 13.7 K) and with CO2 itself (~ pCO2^0.3, Krissansen-Totton et al.
 * 2018), with land area and rain from the climate, and with life on land (microbial mats, plants,
 * forests); ocean-floor weathering adds a smaller term. Through the CO2 term a faint Sun leaves the planet
 * cooler: it needs more CO2 for the same warmth, and that CO2 weathers rock faster. Early life's methane
 * makes up part of the gap. The methane collapses at the Great Oxidation Event faster than CO2 can build
 * up, a sudden cooling that can tip the planet into a snowball, as on Earth ~2.4 Ga.
 * <p>
 * <b>Ice and snowballs.</b> Below ~12 C ice-albedo feedback amplifies cooling. If the amplified temperature
 * falls past a per-world threshold, the planet freezes over for 10-60 Myr until volcanic CO2 builds up.
 * The freeze ends in a short hothouse, and its intense weathering floods the ocean with nutrients.
 */
public final class WorldHistory {
    /** Today's Earth spreads about 3 km2 of ocean floor per year; scaled by planet area. */
    private static final double EARTH_SPREADING_KM2_PER_MYR = 3.0e6;
    private static final double EARTH_AREA_KM2 = 5.1e8;
    private static final double SNOWBALL_TEMP_C = -35.0;
    /** Anoxic background oxygen, PAL. */
    private static final double ANOXIC_PAL = 1e-7;
    /** Methane warming before the Great Oxidation Event, deg C. */
    private static final double METHANE_WARMING_C = 12.0;
    /** Reduced volcanic gases relative to today's organic burial, at today's mantle temperature. */
    private static final double REDUCED_GAS_SCALE = 0.135;
    /** Share of the reduced-gas sink left once ozone shields methane and oxygen persists. */
    private static final double OXIC_SINK_SHARE = 0.6;
    /** Share of carbon leaving the surface as organic burial on Earth today (the rest as carbonate). */
    private static final double ORGANIC_CARBON_SHARE = 0.2;
    /** CO2 dependence of land weathering per deg C of greenhouse forcing: beta 0.3 x ln2 / 3 C per doubling. */
    private static final double LAND_CO2_PER_C = 0.3 * Math.log(2.0) / 3.0;
    /** The same for sea-floor weathering, beta 0.23. */
    private static final double SEA_CO2_PER_C = 0.23 * Math.log(2.0) / 3.0;
    /** Share of weathering on land today (the rest on the sea floor). */
    private static final double EARTH_LAND_SHARE = 0.8;
    /** Oxygen must fall below this to leave the post-NOE state again, PAL. */
    private static final double NOE_EXIT_PAL = 0.15;
    /** Response time of organic burial to its drivers, Myr. */
    private static final double BURIAL_SMOOTHING_MYR = 10.0;
    /** Log-space spread of milestone waits (one sigma). */
    private static final double WAIT_SIGMA = 0.6;
    /** Scales a logistic variate to unit variance (sqrt(3) / pi). */
    private static final double LOGISTIC_TO_NORMAL = Math.sqrt(3.0) / Math.PI;

    /** How random draws are made. */
    public enum Preset {
        /** Full distributions (owner decision A-6). */
        RANDOM,
        /** Earth's milestone waits and median draws for everything else: roughly Earth's history. */
        EARTH_LIKE
    }

    /**
     * What the rest of the simulation reports each step.
     *
     * @param newOceanKm2PerMyr ocean floor made at ridges
     * @param plumes            plume heads (large igneous provinces) this step
     * @param landWeathering    weathering strength of the land from the climate field, 1 = Earth today
     *                          (NaN: not computed yet; estimated from the land fraction)
     * @param landFraction      share of the surface above sea level
     * @param denudation        continental erosion rate relative to Earth today (NaN: unknown, taken as 1)
     */
    public record Inputs(double newOceanKm2PerMyr, int plumes, double landWeathering, double landFraction,
                         double denudation) {
    }

    private final long seed;
    private final Preset preset;
    private final double planetAreaKm2;
    private final EventLog events;

    private final double luminosityRef;
    private final double tpExcessC;
    private final double tpTauMyr;
    private final double snowballTriggerC;
    private final double[] milestoneTimes = new double[Milestone.values().length];
    private final double[] oxygenReached = new double[GlobalState.OxygenState.values().length];

    private double degassing = Double.NaN;
    private double burial = Double.NaN;
    /** Continental weathering flux of the last step, 1 = Earth today (the phosphorus supply). */
    private double landFlux = 1.0;
    private double oxygenPal = ANOXIC_PAL;
    private GlobalState.OxygenState oxygenState = GlobalState.OxygenState.PRE_GOE;
    private boolean oxic;
    private boolean snowball;
    private double snowballEnd;
    private double lastSnowballEnd = -1e9;
    private int snowballCount;
    private GlobalState state;

    public WorldHistory(long seed, Preset preset, double planetAreaKm2, EventLog events) {
        this.seed = seed;
        this.preset = preset;
        this.planetAreaKm2 = planetAreaKm2;
        this.events = events;
        luminosityRef = 1.0 + 0.05 * (2.0 * draw(Streams.HISTORY, 0) - 1.0);
        tpExcessC = 325.0 * (1.0 + 0.2 * (2.0 * draw(Streams.HISTORY, 1) - 1.0));
        tpTauMyr = 2_000.0 * (1.0 + 0.2 * (2.0 * draw(Streams.HISTORY, 2) - 1.0));
        snowballTriggerC = -6.0 + 3.0 * (2.0 * draw(Streams.HISTORY, 4) - 1.0);
        Arrays.fill(milestoneTimes, Double.NaN);
        Arrays.fill(oxygenReached, Double.NaN);
        oxygenReached[GlobalState.OxygenState.PRE_GOE.ordinal()] = 0.0;
        state = new GlobalState(0.0, luminosity(0.0), mantleTpC(0.0), 1.0, 25.0, Double.NaN, ANOXIC_PAL, 0.0, 0.0,
                false, milestoneTimes.clone());
    }

    private double draw(long stream, long k) {
        return preset == Preset.EARTH_LIKE ? 0.5 : Rng.unit(seed, stream, 0, k);
    }

    /** Gough (1981): ~71% of today's output at formation, 100% at 4.57 Gyr. */
    public double luminosity(double t) {
        return luminosityRef / (1.0 + 0.4 * (1.0 - t / 4_570.0));
    }

    /** Mantle potential temperature: ~1,625 C at formation cooling toward ~1,300 C. */
    public double mantleTpC(double t) {
        return 1_300.0 + tpExcessC * Math.exp(-t / tpTauMyr);
    }

    /** The snowball threshold of this world, deg C. */
    public double snowballTriggerC() {
        return snowballTriggerC;
    }

    public GlobalState state() {
        return state;
    }

    /** Advances the box models to time {@code t} (the end of a step of {@code dt}). */
    public GlobalState update(double t, double dt, Inputs in) {
        double tp = mantleTpC(t);
        double lum = luminosity(t);
        double spreading = Math.max(0.2, in.newOceanKm2PerMyr()
                / (EARTH_SPREADING_KM2_PER_MYR * planetAreaKm2 / EARTH_AREA_KM2));
        double denudation = Double.isNaN(in.denudation()) ? 1.0 : Math.max(0.05, in.denudation());

        // volcanic CO2: ridges and arcs (with mantle heat), plus plume pulses
        double raw = (0.25 + 0.75 * spreading) * Math.exp((tp - 1_334.0) / 150.0) + 0.5 * in.plumes() / Math.max(dt, 1e-9);
        degassing = Double.isNaN(degassing) ? raw : degassing + (raw - degassing) * (1.0 - Math.exp(-dt / 10.0));

        updateMilestones(t);

        // oxygen: organic burial against reduced volcanic gases and oxidative weathering
        boolean wasOxic = oxic;
        // the surface oxygen and phosphorus reservoirs respond over millions of years, not to one step's weather
        // (under a snowball the whole oxygen system stands still)
        double rawBurial = organicBurial(t, denudation);
        if (Double.isNaN(burial)) {
            burial = rawBurial;
        } else if (!snowball) {
            burial += (rawBurial - burial) * (1.0 - Math.exp(-dt / BURIAL_SMOOTHING_MYR));
        }
        double reducedGases = REDUCED_GAS_SCALE * degassing * Math.exp((tp - 1_334.0) / 120.0);
        double oxidativeK = 0.9 * Math.sqrt(Math.min(3.0, denudation));
        if (!snowball) {
            if (!oxic && burial > reducedGases) {
                oxic = true;
            } else if (oxic && burial <= OXIC_SINK_SHARE * reducedGases) {
                oxic = false;
            }
            if (oxic) {
                double surplus = (burial - OXIC_SINK_SHARE * reducedGases) / oxidativeK;
                oxygenPal = Math.min(1.6, Math.max(1e-4, surplus * surplus));
            } else {
                oxygenPal = ANOXIC_PAL;
            }
        }
        recordOxygen(t);
        double netOrganic = Math.max(0.0, burial - (oxic ? oxidativeK * Math.sqrt(oxygenPal) : 0.0));

        // carbon thermostat: silicate weathering removes what organic burial does not
        double methane = has(Milestone.LIFE, t) && !oxic
                ? METHANE_WARMING_C * Math.min(1.0, (t - milestoneTimes[Milestone.LIFE.ordinal()]) / 200.0) : 0.0;
        double deficit = greenhouseDeficitC(lum, methane);
        double toWeather = Math.max(0.1 * degassing, degassing - ORGANIC_CARBON_SHARE * netOrganic);
        double land = Double.isNaN(in.landWeathering())
                ? in.landFraction() / 0.29 * lifeOnLand(t, false)
                : in.landWeathering() * lifeOnLand(t, true);
        double equilibrium = thermostat(land, spreading, toWeather, deficit);
        if (!snowball) {
            // phosphorus supply; under the ice it waits for the thaw (then the meltwater pulse adds to it)
            double landPart = 0.8 * land * Math.exp((equilibrium - 15.0) / 13.7 + LAND_CO2_PER_C * (equilibrium - 15.0 + deficit));
            landFlux = toWeather * landPart / weathering(equilibrium, land, spreading, deficit) / EARTH_LAND_SHARE;
        }
        // the methane greenhouse collapses at the oxidation event faster than CO2 can build up
        double shock = oxic && !wasOxic ? state.methaneGreenhouseC() : 0.0;
        double temperature = climate(t, equilibrium, shock);

        double co2 = 280.0 * Math.pow(2.0, (equilibrium - 15.0 + deficit) / 3.0);
        state = new GlobalState(t, lum, tp, degassing, temperature, Math.max(150.0, Math.min(1e6, co2)),
                oxygenPal, burial, methane, snowball, milestoneTimes.clone());
        return state;
    }

    /**
     * Warming that CO2 must supply on top of today's to keep 15 C, deg C: the faint-Sun gap (~192 C per unit of
     * luminosity, i.e. ~0.7 C per W/m2 of absorbed sunlight) minus biogenic methane.
     */
    static double greenhouseDeficitC(double luminosity, double methaneC) {
        return -192.0 * (luminosity - 1.0) - methaneC;
    }

    /** Organic carbon burial relative to Earth today: productivity x nutrients x burial efficiency. */
    private double organicBurial(double t, double denudation) {
        if (!has(Milestone.PHOTOSYNTHESIS, t)) {
            return 0.0;
        }
        double productivity = 0.35;
        productivity *= has(Milestone.EUKARYOTES, t) ? 1.15 : 1.0;
        productivity *= has(Milestone.ALGAE, t) ? 1.5 : 1.0;
        productivity *= has(Milestone.ANIMALS, t) ? 1.25 : 1.0;
        productivity *= has(Milestone.LAND_PLANTS, t) ? 1.33 : 1.0;
        double sinceThaw = t - lastSnowballEnd;
        double thawPulse = sinceThaw >= 0.0 && sinceThaw < 30.0 ? 1.0 + 2.0 * Math.exp(-sinceThaw / 5.0) : 1.0;
        double nutrients = Math.max(0.2, Math.min(3.0, Math.pow(landFlux, 0.7))) * thawPulse;
        double efficiency = Math.sqrt(Math.max(0.3, Math.min(3.0, denudation)));
        return productivity * nutrients * efficiency;
    }

    /** How much life on land speeds up weathering: bare rock, microbial mats, plants, forests. */
    private double lifeOnLand(double t, boolean climateField) {
        // a climate field already rains more on vegetated land (recycling, forest convection: ~1.5x the
        // weathering of bare land), so plants add less on top of it
        if (has(Milestone.FORESTS, t)) {
            return climateField ? 0.7 : 1.0;
        }
        if (has(Milestone.LAND_PLANTS, t)) {
            return climateField ? 0.6 : 0.8;
        }
        return has(Milestone.PHOTOSYNTHESIS, t) ? 0.35 : 0.25;
    }

    /** Temperature at which land plus sea-floor weathering removes CO2 as fast as required. */
    static double thermostat(double landStrength, double spreading, double required, double deficitC) {
        double lo = -40.0;
        double hi = 60.0;
        if (weathering(hi, landStrength, spreading, deficitC) < required) {
            return hi;
        }
        if (weathering(lo, landStrength, spreading, deficitC) > required) {
            return lo;
        }
        for (int k = 0; k < 50; k++) {
            double mid = 0.5 * (lo + hi);
            if (weathering(mid, landStrength, spreading, deficitC) < required) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return 0.5 * (lo + hi);
    }

    /**
     * Weathering flux relative to Earth today (= 1 at 15 C under today's Sun with Earth's land and spreading).
     * {@code tempC - 15 + deficitC} is the greenhouse warming CO2 supplies beyond today's, so the CO2 terms
     * are pCO2 ratios raised to beta.
     */
    static double weathering(double tempC, double landStrength, double spreading, double deficitC) {
        double co2Warming = tempC - 15.0 + deficitC;
        return 0.8 * landStrength * Math.exp((tempC - 15.0) / 13.7 + LAND_CO2_PER_C * co2Warming)
                + 0.2 * spreading * Math.exp((tempC - 15.0) / 12.0 + SEA_CO2_PER_C * co2Warming);
    }

    /** Ice-albedo feedback: below 12 C each degree of forcing cools the planet 1.6 degrees. */
    static double withIceAlbedo(double tempC) {
        return tempC < 12.0 ? tempC - 0.6 * (12.0 - tempC) : tempC;
    }

    /** Snowball episodes and their hothouse aftermath; {@code shock} = sudden cooling this step. */
    private double climate(double t, double equilibrium, double shock) {
        if (snowball) {
            if (t < snowballEnd) {
                return SNOWBALL_TEMP_C;
            }
            snowball = false;
            lastSnowballEnd = snowballEnd;
            events.add(GeoEvent.Type.SNOWBALL_END, snowballEnd, -1, -1, -1, 0.0);
        }
        double sinceThaw = t - lastSnowballEnd;
        if (withIceAlbedo(equilibrium - shock) < snowballTriggerC && sinceThaw > 50.0) {
            double duration = 10.0 + 50.0 * draw(Streams.SNOWBALL, snowballCount++);
            snowball = true;
            snowballEnd = t + duration;
            events.add(GeoEvent.Type.SNOWBALL_START, t, -1, -1, -1, duration);
            return SNOWBALL_TEMP_C;
        }
        // CO2 built up under the ice leaves a short hothouse
        return withIceAlbedo(equilibrium) + (sinceThaw >= 0 && sinceThaw < 30.0 ? 25.0 * Math.exp(-sinceThaw / 3.0) : 0.0);
    }

    /** Logs oxygen-state changes; leaving POST_NOE needs a clear drop (below 0.15 PAL), not a wobble. */
    private void recordOxygen(double t) {
        GlobalState.OxygenState now = GlobalState.OxygenState.of(oxygenPal);
        if (oxygenState == GlobalState.OxygenState.POST_NOE && now == GlobalState.OxygenState.INTERMEDIATE
                && oxygenPal >= NOE_EXIT_PAL) {
            now = GlobalState.OxygenState.POST_NOE;
        }
        if (now != oxygenState) {
            events.add(GeoEvent.Type.OXYGENATION, t, -1, -1, -1, now.ordinal());
            oxygenState = now;
        }
        for (GlobalState.OxygenState s : GlobalState.OxygenState.values()) {
            if (s.ordinal() <= now.ordinal() && Double.isNaN(oxygenReached[s.ordinal()])) {
                oxygenReached[s.ordinal()] = t;
            }
        }
    }

    private void updateMilestones(double t) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Milestone m : Milestone.values()) {
                if (!Double.isNaN(milestoneTimes[m.ordinal()])) {
                    continue;
                }
                double ready = readyTime(m);
                if (Double.isNaN(ready)) {
                    continue;
                }
                double wait = waitMyr(m);
                if (ready + wait <= t) {
                    milestoneTimes[m.ordinal()] = ready + wait;
                    events.add(GeoEvent.Type.MILESTONE, ready + wait, -1, -1, -1, m.ordinal());
                    changed = true;
                }
            }
        }
    }

    /**
     * Random wait once the prerequisites are met: log-logistic around the median (u = 0.5 gives the median),
     * about x/÷ 1.8 at one sigma. Exponential waits have tails so long that most worlds would never reach
     * forests (and coal) by 4.57 Gyr; this spread keeps the order random but the total near Earth's.
     */
    private double waitMyr(Milestone m) {
        if (preset == Preset.EARTH_LIKE) {
            return m.earthWaitMyr();
        }
        double u = Math.min(1.0 - 1e-9, Math.max(1e-9, draw(Streams.MILESTONES, m.ordinal())));
        return m.medianWaitMyr() * Math.exp(WAIT_SIGMA * LOGISTIC_TO_NORMAL * Math.log(u / (1.0 - u)));
    }

    /** When the prerequisites of {@code m} were all met (NaN = not yet). */
    private double readyTime(Milestone m) {
        double ready = m.prerequisite() == null ? 100.0 : milestoneTimes[m.prerequisite().ordinal()];
        if (m.oxygenNeeded() != null) {
            ready = Math.max(ready, oxygenReached[m.oxygenNeeded().ordinal()]);
        }
        return ready;
    }

    private boolean has(Milestone m, double t) {
        double when = milestoneTimes[m.ordinal()];
        return !Double.isNaN(when) && when <= t;
    }
}
