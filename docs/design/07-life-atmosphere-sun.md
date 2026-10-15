# 07 · Life, atmosphere, oceans, Sun and mantle cooling

> **Level 1** · Read when: you touch global state, time gates, greenhouse or oxygen, life milestones, extinctions or the Sun.
> Decisions: A-6, A-13, D-6 · Go deeper: report § "About a dozen global switches gate deposits by planetary age".

**Summary.** A handful of cheap **0-D box models** (one number each, updated every step) evolve the mantle temperature, the Sun's brightness, CO₂ and temperature, oxygen and ocean chemistry. **Life milestones** happen at random times once their prerequisites are met (hazard model). Together they form `WorldHistory.globalState(t)`. Every geologic rule queries it, so a world's deposits follow *its own* history, not Earth's dates (D-6). An "Earth-like" preset pins the random draws to Earth's median timings.

## Box models

| Model | State | Rule (per step) | Drives |
|---|---|---|---|
| Mantle cooling | Potential temperature Tp | Exponential decay from ≈1,600–1,650 °C toward ≈1,300 °C, rate randomized ±20% | Ocean-crust thickness, plate speed, degassing; **komatiites** while Tp is high; **kimberlites dominate** once Tp < ~1,400 °C |
| Sun | Luminosity L | L(t) = L_ref / (1 + 0.4·(1 − t / 4,570 Myr)) (Gough 1981): ≈71% at t = 0, 100% at 4.57 Gyr, ≈114% at 6 Gyr; L_ref randomized ±5% | Insolation for climate ([04](04-surface-processes.md)) |
| Carbon cycle | pCO₂, global mean temperature | Find pCO₂ such that silicate weathering = degassing. Degassing = ridges × spreading rate + arcs + LIP pulses. Weathering grows with temperature, runoff, uplift (fresh rock) and land plants. Temperature from energy balance with log-CO₂ forcing and ice–albedo feedback | Climate, glaciations, **snowball** episodes, weathering-profile rates |
| Oxygen | O₂ level (states below) | Sources: organic-carbon and pyrite burial (∝ sediment flux × productivity). Sinks: reduced volcanic gases (declining as the mantle cools) and oxidative weathering. Bistable: when sources exceed reductant sinks, O₂ **jumps** (the GOE) | Atmosphere state |
| Ocean redox | Deep-ocean state | Ferruginous while O₂ is low and hydrothermal Fe is high → euxinic margins after GOE as sulfate rises → oxygenated after the second rise (NOE) | BIF, SEDEX, MVT, uranium and phosphorite gates ([06](06-resources.md)) |
| Seawater chemistry | Aragonite vs calcite seas (toggle) | Follows ridge production (contested; low weight) | Potash mineralogy |

**O₂ states:** `PRE_GOE` (< 10⁻⁵ of today's level) → `INTERMEDIATE` (~1–10%) → `POST_NOE` (> ~20%).

## Life milestones (stochastic)

Each milestone has prerequisites and a random waiting time once they are met (exponential with the median shown; Earth's value in brackets as a reference).

| Milestone | Prerequisites | Median wait after prerequisites (Earth) | Switches on |
|---|---|---|---|
| Life | Liquid ocean, t > 100 Myr | ~300 Myr (≈0.5–0.8 Gyr after formation) | Biological carbon burial |
| Oxygenic photosynthesis | Life | ~1 Gyr (≈1.6–1.9 Gyr after formation) | O₂ source; enables the GOE |
| Sulfate-reducing microbes | O₂ ≥ INTERMEDIATE | ~50 Myr | MVT, SEDEX and sediment-hosted Cu sulfides |
| Complex life (eukaryotes) | O₂ ≥ INTERMEDIATE | ~500 Myr | Higher productivity |
| Animals with shells, reefs | O₂ = POST_NOE | ~50 Myr (Cambrian) | Reef limestone, shelly carbonates, phosphorites |
| Land plants | O₂ = POST_NOE (ozone), animals | ~70 Myr (≈4.15 Gyr after formation) | Soils, slower erosion, enhanced weathering |
| Forests and peat swamps | Land plants | ~50 Myr | **Coal**, plant-debris reductant (roll-front U, red-bed Cu) |
| Calcareous plankton | Animals with shells | ~300 Myr (Jurassic) | **Chalk** |
| Diatoms | Calcareous plankton | ~50 Myr (Cretaceous) | Diatomite |

## Extinctions and crises

- Triggers: large LIPs (probability grows with eruption volume), snowball deglaciations, random impacts.
- Effects: reef collapse; a **coal gap** of about 10 Myr after a major terrestrial extinction; temporarily lower organic burial; recovery delays.
- All are written to the event log so the timeline preview can show them.

## "Solar activity"

Only the **long-term brightening** of the Sun matters at million-year steps. Sunspot and flare cycles average out. Orbital (Milankovitch-like) cycles are modelled only in the last 10 Myr, to pace glacial advances ([04](04-surface-processes.md#glaciers)).

## Presentation (A-13)

The life timeline drives **geology** (coal, reefs, chalk, soils, erosion rates, oxygen-gated ores). In-game vegetation and animals are always present (Minecraft gameplay). The map preview shows the realized timeline, for example "land plants appeared 380 Ma; no coal older than that". A young world (say T = 1 Gyr) will have little continental crust, no coal, no reef limestone and few diamonds. That follows from the simulation and is intended.

## Randomness and presets

- Draws use the world seed (D-5), so the same seed gives the same history.
- Presets: **Earth-like** (medians, Earth-tuned constants) · **Random** (full distributions) · **Custom** (expose the main medians in an advanced tab).
