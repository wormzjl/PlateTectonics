package com.platetectonics.geosim.history;

import com.platetectonics.geosim.events.EventLog;
import com.platetectonics.geosim.events.GeoEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldHistoryTest {
    private static final double EARTH_AREA_KM2 = 5.1e8;

    /** Earth-like boundary conditions held constant: today's spreading and land fraction. */
    private static GlobalState run(WorldHistory history) {
        double t = 0.0;
        GlobalState g = history.state();
        while (t < 4_570.0) {
            double dt = t < 3_000.0 ? 10.0 : t < 4_000.0 ? 5.0 : 2.0;
            t += dt;
            g = history.update(t, dt, new WorldHistory.Inputs(3.0e6, 0, Double.NaN, 0.29, Double.NaN));
        }
        return g;
    }

    private static double firstOxygenation(EventLog log, GlobalState.OxygenState state) {
        return log.events().stream()
                .filter(e -> e.type() == GeoEvent.Type.OXYGENATION && e.value() == state.ordinal())
                .mapToDouble(GeoEvent::timeMyr).min().orElse(Double.NaN);
    }

    @Test
    void earthLikePresetFollowsEarthsHistory() {
        EventLog log = new EventLog();
        GlobalState g = run(new WorldHistory(1L, WorldHistory.Preset.EARTH_LIKE, EARTH_AREA_KM2, log));

        double goe = firstOxygenation(log, GlobalState.OxygenState.INTERMEDIATE);
        double noe = firstOxygenation(log, GlobalState.OxygenState.POST_NOE);
        assertTrue(goe > 2_000.0 && goe < 2_400.0, "GOE near 2.4 Ga: " + goe);
        assertTrue(noe > 3_600.0 && noe < 3_950.0, "NOE near 0.8 Ga: " + noe);
        assertTrue(g.timeOf(Milestone.ANIMALS) > noe, "animals wait for oxygen");
        assertTrue(g.has(Milestone.FORESTS) && g.timeOf(Milestone.FORESTS) > 4_100.0
                && g.timeOf(Milestone.FORESTS) < 4_300.0, "forests in the Devonian: " + g.timeOf(Milestone.FORESTS));
        assertTrue(g.oxygenPal() > 0.7 && g.oxygenPal() < 1.4, "today's oxygen: " + g.oxygenPal());
        assertTrue(g.globalTempC() > 12.0 && g.globalTempC() < 18.0, "today's temperature: " + g.globalTempC());
        assertTrue(g.pCO2ppm() > 200.0 && g.pCO2ppm() < 400.0, "today's CO2: " + g.pCO2ppm());
        // the methane collapse at the GOE tips this world into a Huronian-style snowball
        assertTrue(log.events().stream().anyMatch(e -> e.type() == GeoEvent.Type.SNOWBALL_START
                && Math.abs(e.timeMyr() - goe) < 1e-9), "snowball at the GOE");
    }

    @Test
    void lifeAndOxygenGateEachOther() {
        for (long seed = 1; seed <= 60; seed++) {
            EventLog log = new EventLog();
            GlobalState g = run(new WorldHistory(seed, WorldHistory.Preset.RANDOM, EARTH_AREA_KM2, log));
            double goe = firstOxygenation(log, GlobalState.OxygenState.INTERMEDIATE);
            double noe = firstOxygenation(log, GlobalState.OxygenState.POST_NOE);
            if (!Double.isNaN(goe)) {
                assertTrue(g.has(Milestone.PHOTOSYNTHESIS) && g.timeOf(Milestone.PHOTOSYNTHESIS) <= goe,
                        "seed " + seed + ": oxygen needs photosynthesis");
            }
            for (Milestone m : Milestone.values()) {
                if (!g.has(m)) {
                    continue;
                }
                if (m.prerequisite() != null) {
                    assertTrue(g.timeOf(m.prerequisite()) <= g.timeOf(m), "seed " + seed + ": " + m + " after its prerequisite");
                }
                if (m.oxygenNeeded() == GlobalState.OxygenState.INTERMEDIATE) {
                    assertTrue(goe <= g.timeOf(m), "seed " + seed + ": " + m + " after the GOE");
                }
                if (m.oxygenNeeded() == GlobalState.OxygenState.POST_NOE) {
                    assertTrue(noe <= g.timeOf(m), "seed " + seed + ": " + m + " after the NOE");
                }
            }
        }
    }

    @Test
    void mostRandomWorldsGrowForests() {
        int forests = 0;
        for (long seed = 1; seed <= 100; seed++) {
            if (run(new WorldHistory(seed, WorldHistory.Preset.RANDOM, EARTH_AREA_KM2, new EventLog())).has(Milestone.FORESTS)) {
                forests++;
            }
        }
        assertTrue(forests >= 60 && forests <= 90, "forests (and coal) in most, not all, worlds: " + forests);
    }

    @Test
    void sameSeedGivesSameHistory() {
        EventLog logA = new EventLog();
        EventLog logB = new EventLog();
        GlobalState a = run(new WorldHistory(77L, WorldHistory.Preset.RANDOM, EARTH_AREA_KM2, logA));
        GlobalState b = run(new WorldHistory(77L, WorldHistory.Preset.RANDOM, EARTH_AREA_KM2, logB));
        assertEquals(a.globalTempC(), b.globalTempC());
        assertEquals(a.oxygenPal(), b.oxygenPal());
        assertArrayEquals(a.milestoneTimes(), b.milestoneTimes());
        List<GeoEvent> ea = logA.events();
        assertEquals(ea, logB.events());
    }

    @Test
    void thermostatRespondsToWeatheringAndTheSun() {
        assertEquals(1.0, WorldHistory.weathering(15.0, 1.0, 1.0, 0.0), 1e-12, "calibrated to today");
        assertEquals(15.0, WorldHistory.thermostat(1.0, 1.0, 1.0, 0.0), 1e-6);
        double today = WorldHistory.thermostat(1.0, 1.0, 1.0, 0.0);
        assertTrue(WorldHistory.thermostat(2.0, 1.0, 1.0, 0.0) < today - 3.0, "more weatherable land cools the planet");
        assertTrue(WorldHistory.thermostat(1.0, 1.0, 2.0, 0.0) > today + 3.0, "more volcanic CO2 warms it");
        double faintSun = WorldHistory.greenhouseDeficitC(0.8, 0.0);
        assertTrue(WorldHistory.thermostat(1.0, 1.0, 1.0, faintSun) < today - 3.0,
                "a faint Sun leaves the planet cooler: its extra CO2 also weathers rock faster");
        assertTrue(WorldHistory.thermostat(1.0, 1.0, 1.0, WorldHistory.greenhouseDeficitC(0.8, 12.0))
                > WorldHistory.thermostat(1.0, 1.0, 1.0, faintSun), "methane makes up part of the gap");
    }
}
