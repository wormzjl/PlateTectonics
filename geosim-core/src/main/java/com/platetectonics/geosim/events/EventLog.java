package com.platetectonics.geosim.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Append-only log of geologic events; read later by resource generators and the timeline preview. */
public final class EventLog {
    private final List<GeoEvent> events = new ArrayList<>();

    public void add(GeoEvent event) {
        events.add(event);
    }

    public void add(GeoEvent.Type type, double timeMyr, int cell, int plateA, int plateB, double value) {
        events.add(new GeoEvent(type, timeMyr, cell, plateA, plateB, value));
    }

    public List<GeoEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public Map<GeoEvent.Type, Integer> counts() {
        Map<GeoEvent.Type, Integer> counts = new EnumMap<>(GeoEvent.Type.class);
        for (GeoEvent e : events) {
            counts.merge(e.type(), 1, Integer::sum);
        }
        return counts;
    }
}
