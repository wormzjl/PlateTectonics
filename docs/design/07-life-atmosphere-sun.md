# 07 · Life, atmosphere, oceans, Sun and mantle cooling

> **Level 1** · Read when: you touch global state, time gates, greenhouse or oxygen, life milestones, snowballs, extinctions or the Sun.
> Decisions: A-6, A-13, D-6, D-27, D-28, D-29 · Related: [02 step order](02-simulation-pipeline.md#order-of-operations-in-one-step), [04](04-surface-processes.md) (climate field, erosion) · Go deeper: report § "About a dozen global switches gate deposits by planetary age".

**Summary.** A few cheap **0-D box models** (one number each, updated every step) evolve the Sun's brightness, the mantle temperature, volcanic CO₂, oxygen and the global temperature. They are **coupled through life**. Oxygen is a balance: organic-carbon burial (life's productivity × phosphorus from weathered continents × burial efficiency from erosion) against reduced volcanic gases and oxidative weathering. The Great Oxidation Event (GOE) and the Neoproterozoic Oxygenation Event (NOE) are **outcomes** of that balance, not scheduled dates. Temperature comes from a **carbon thermostat**: the temperature at which silicate weathering removes the volcanic CO₂ that organic burial does not. **Life milestones** arrive after random waits once their prerequisites, including oxygen levels, are met. The result is `GlobalState`. Every geologic rule reads it, so a world's deposits follow *its own* history, not Earth's dates (D-6). The Earth-like preset uses Earth's own waits and lands close to Earth's history.

## As built (Phase 3) vs planned

| Built (`geosim-core`, package `history`) | Deferred |
|---|---|
| Sun, mantle cooling, volcanic degassing with plume pulses | Ocean-crust thickness from mantle temperature; komatiite and kimberlite gates (Phase 7, [06](06-resources.md)) |
| Oxygen balance with hysteresis; GOE and NOE as outcomes | Ocean redox state (ferruginous, euxinic, oxygenated); aragonite vs calcite seas |
| Carbon thermostat with CO₂-dependent land and sea-floor weathering; methane; ice–albedo feedback; snowballs | Extinctions and crises; orbital cycles |
| 10 life milestones with oxygen prerequisites; Random and Earth-like presets | Custom preset (exposed medians) |

## Inputs from the planet

Each step the box models read what the planet did in the step before ([02](02-simulation-pipeline.md#order-of-operations-in-one-step)):

| Input | From | Used for |
|---|---|---|
| New ocean floor, relative to Earth's 3 km²/yr scaled by planet area (at least 0.2) = "spreading" | Plates | Degassing; sea-floor weathering |
| Plume heads | Plates | Degassing pulses |
| Land weathering strength (1 = Earth today) | Climate field ([04](04-surface-processes.md#climate)) | Thermostat; phosphorus supply |
| Mean land erosion ÷ 50 m/Myr = "denudation" (1 = Earth today) | Surface processes | Burial efficiency; oxidative weathering |

Before the first climate field exists, land weathering is estimated as land fraction ÷ 0.29.

## Box models

| Model | Rule (per step) | Drives |
|---|---|---|
| Sun | L(t) = L_ref / (1 + 0.4 (1 − t / 4,570 Myr)) (Gough 1981): ≈71% of today at t = 0, 100% at 4.57 Gyr, ≈114% at 6 Gyr; L_ref ±5% per world | The faint-Sun greenhouse gap |
| Mantle | Tp = 1,300 °C + 325 °C (±20%) × e^(−t / 2,000 Myr (±20%)): ≈1,625 °C at t = 0, ≈1,333 °C at 4.57 Gyr | Degassing; reduced gases |
| Degassing | (0.25 + 0.75 × spreading) × e^((Tp − 1,334) / 150), plus a pulse per plume head worth 0.5 Myr of today's output; smoothed over 10 Myr; 1 = Earth today | CO₂ to remove; the reduced-gas sink |
| Oxygen | [Below](#oxygen) | O₂ state; milestone and deposit gates |
| Carbon thermostat | [Below](#carbon-thermostat) | Global temperature, pCO₂, snowballs |

Planned: komatiites while Tp is high and kimberlites once Tp < ~1,400 °C; ocean redox (ferruginous while O₂ is low and hydrothermal Fe high → euxinic margins after the GOE → oxygenated after the NOE) for BIF, SEDEX, MVT, uranium and phosphorite gates; an aragonite/calcite-sea toggle from ridge production for potash mineralogy.

## Oxygen

Relative units, 1 = Earth today. The structure follows the bistable model of Goldblatt et al. (2006).

- **Source: organic-carbon burial** B = productivity × nutrients × efficiency. B = 0 before oxygenic photosynthesis.
  - Productivity: 0.35 at first, × 1.15 with eukaryotes, × 1.5 with algae, × 1.25 with animals, × 1.33 with land plants (≈1.0 with all of them).
  - Nutrients (phosphorus) = (continental weathering flux)^0.7, clamped 0.2–3. Warm, wet, mountainous land feeds the sea. After a snowball a meltwater pulse multiplies it by 1 + 2 e^(−t / 5 Myr) for 30 Myr.
  - Efficiency = √denudation, clamped 0.3–3: more sediment buries more carbon.
  - B follows these drivers with a 10 Myr response time, not one step's weather.
- **Sinks:**
  - reduced volcanic gases (H₂, CO, H₂S): R = 0.135 × degassing × e^((Tp − 1,334) / 120). They fade as the mantle cools;
  - oxidative weathering of old organic matter and pyrite: k √O₂, with k = 0.9 × √denudation (denudation capped at 3).
- **GOE.** The atmosphere turns oxic when B first exceeds R. Methane then collapses and an ozone layer forms, so only 60% of R still consumes oxygen. Oxygen settles where burial balances the sinks: O₂ = ((B − 0.6 R) / k)² PAL, kept between 10⁻⁴ and 1.6 PAL. Just after the GOE that is a few percent of today's level.
- **Hysteresis.** The atmosphere goes anoxic again (10⁻⁷ PAL) only if B falls to 0.6 R. The `POST_NOE` state is left only below 0.15 PAL, not on a dip just under 0.2.
- **NOE.** Algae, a snowball's nutrient pulse or big orogenies (more erosion, more burial) push B high enough for ≥ 0.2 PAL.
- **Snowballs freeze the system.** Burial, oxygen and the phosphorus supply hold their values until the thaw.

**O₂ states** (milestones and deposits gate on them):

| State | O₂ | Meaning |
|---|---|---|
| `PRE_GOE` | < 10⁻⁴ PAL | Anoxic and methane-rich: BIF, detrital uraninite and pyrite |
| `INTERMEDIATE` | 10⁻⁴ to < 0.2 PAL | The "boring billion": sulfate in the sea, red beds |
| `POST_NOE` | ≥ 0.2 PAL (left only below 0.15) | Animals, an ozone shield, an oxygenated deep ocean |

## Carbon thermostat

Over a few hundred kyr, silicate weathering plus net organic burial remove CO₂ as fast as volcanoes add it. Each step the model solves (by bisection between −40 and 60 °C) for the temperature T at which they balance:

```
weathering(T) = degassing − 0.2 × net organic burial            (at least 0.1 × degassing)
weathering(T) = 0.8 × land      × e^((T − 15)/13.7) × (pCO2 / today)^0.3     land
              + 0.2 × spreading × e^((T − 15)/12)   × (pCO2 / today)^0.23    sea floor
pCO2          = 280 ppm × 2^((T − 15 + deficit) / 3)                         3 °C per doubling
deficit       = 192 °C × (1 − L) − methane warming                           faint-Sun gap
```

- Net organic burial is B minus oxidative weathering. 0.2 is organic carbon's share of the carbon leaving the surface on Earth today (the rest leaves as carbonate).
- Weathering depends on temperature **and** on CO₂ itself (β = 0.3 on land, 0.23 on the sea floor; Krissansen-Totton et al. 2018). So a faint young Sun leaves the planet **cooler**: it needs more CO₂ for the same warmth, and that CO₂ weathers rock faster.
- **Land** = the land weathering strength from the climate field ([04](04-surface-processes.md#climate)) × life on land: 0.25 bare rock, 0.35 microbial mats (after photosynthesis), 0.8 land plants, 1.0 forests.
- **Methane.** Early life's methane adds up to +12 °C (ramping in over 200 Myr after life appears) and is absent while the atmosphere is oxic. It fills part of the faint-Sun gap.
- Calibrated so Earth today (L = 1, land = 1, spreading = 1, no methane) balances at 15 °C. pCO₂ is a diagnostic, clamped to 150 ppm … 10⁶ ppm.

## Ice and snowballs

- **Ice–albedo feedback.** Below 12 °C every degree of cooling becomes 1.6 degrees.
- **Trigger.** A snowball starts when that amplified temperature falls below the world's threshold (−6 °C ± 3 °C, drawn per world), at least 50 Myr after the last thaw. In the step that oxygen appears, the test also subtracts the methane warming just lost, because methane collapses faster than CO₂ can build up. This often tips the planet into a snowball at the GOE, like the Huronian glaciation (~2.4 Ga on Earth).
- **During a snowball** the global mean is −35 °C and all land is iced ([04](04-surface-processes.md#glaciers)) for 10–60 Myr (random), until volcanic CO₂ builds up. Oxygen and phosphorus hold.
- **After the thaw:** a hothouse of +25 °C decaying with a 3 Myr e-fold (for 30 Myr), and the meltwater nutrient pulse above.
- Event log: `SNOWBALL_START` (value = duration), `SNOWBALL_END`, `OXYGENATION` (value = new state), `MILESTONE` (value = milestone).

## Life milestones (stochastic)

Each milestone waits a random time once all its prerequisites are met: the previous milestone and, for some, an oxygen state already reached. Random worlds draw around the median wait. The Earth-like preset uses Earth's wait; the last column gives the time that produces (Myr after formation).

| Milestone | Prerequisites | Median wait (Myr) | Earth wait (Myr) → time | Effect now | Gates later ([06](06-resources.md)) |
|---|---|---|---|---|---|
| Life | t ≥ 100 Myr | 300 | 400 → 500 | Methane greenhouse; shallow carbonate × 0.3 (from × 0.1) | — |
| Oxygenic photosynthesis | Life | 900 | 1,100 → 1,600 | Organic burial (the O₂ source); microbial mats on land (weathering and soil production × 0.35) | — |
| Sulfate-reducing microbes | Life; O₂ ≥ `INTERMEDIATE` | 50 | 50 → ~2,220 | — | MVT, SEDEX, sediment-hosted Cu sulfides |
| Eukaryotes | Life; O₂ ≥ `INTERMEDIATE` | 400 | 500 → ~2,670 | Productivity × 1.15 | — |
| Algae | Eukaryotes | 800 | 1,100 → ~3,770 | Productivity × 1.5 (often triggers the NOE) | — |
| Animals with shells, reefs | Algae; O₂ = `POST_NOE` | 150 | 250 → ~4,020 | Productivity × 1.25; shallow carbonate at full rate | Reef limestone, shelly carbonates, phosphorites |
| Land plants | Animals; O₂ = `POST_NOE` | 80 | 100 → ~4,120 | Productivity × 1.33; land weathering and soil production × 0.8; erodibility × 1.15 instead of × 1.5 | Soils |
| Forests and peat swamps | Land plants | 50 | 60 → ~4,180 | Land weathering, soil production and erodibility × 1.0 | **Coal**; plant-debris reductant (roll-front U, red-bed Cu) |
| Calcareous plankton | Animals | 300 | 350 → ~4,370 | Carbonate ooze above the CCD | **Chalk** |
| Diatoms | Calcareous plankton | 100 | 100 → ~4,470 | — | Diatomite |

- **Wait distribution.** Log-logistic around the median, σ = 0.6 in log space (× or ÷ ~1.8 at one sigma). The medians are shorter than Earth's waits, because Earth is a somewhat slow world (forests only ~390 Myr before the present). With exponential waits and Earth as the median, under half of random worlds would grow forests (and coal) by 4.57 Gyr; now about three quarters do (test: 60–90 of 100 seeds; D-29).
- **Oxygen gates.** Milestones that need oxygen wait for it. A world whose oxygen never rises gets no eukaryotes, animals or land plants.
- The GOE and NOE are not milestones; they come from the [oxygen balance](#oxygen). Milestone times feed back into it through productivity, weathering and erosion.

## Results (box models alone, Earth inputs)

With today's spreading and a land fraction of 0.29 held constant (`WorldHistoryTest`):

- **Earth-like preset:** GOE ~2,170 Myr with a snowball (Earth: ~2.4 Ga = ~2,170 Myr); NOE ~3,770 Myr; animals ~4,020; forests ~4,180. Today ~15 °C, ~1 PAL O₂, ~270 ppm CO₂.
- **200 random worlds:** ~99% get photosynthesis, ~82% an NOE, ~76% forests. A snowball at the GOE is common (~85%).

Full-planet runs feed the real climate and erosion back in; their results are still being tuned and will be recorded in [09](09-roadmap.md).

## Extinctions and crises (planned)

Not built yet.

- Triggers: large LIPs (probability grows with eruption volume), snowball deglaciations, random impacts.
- Effects: reef collapse; a **coal gap** of about 10 Myr after a major terrestrial extinction; temporarily lower organic burial; recovery delays.
- All are written to the event log so the timeline preview can show them.

## "Solar activity"

Only the **long-term brightening** of the Sun matters at million-year steps. Sunspot and flare cycles average out. Orbital (Milankovitch-like) cycles are planned for the last 10 Myr only, to pace glacial advances ([04](04-surface-processes.md#glaciers)).

## Presentation (A-13)

The life timeline drives **geology** (coal, reefs, chalk, soils, erosion rates, oxygen-gated ores). In-game vegetation and animals are always present (Minecraft gameplay). The map preview shows the realized timeline, for example "land plants appeared 380 Ma; no coal older than that". A young world (say T = 1 Gyr) will have little continental crust, no coal, no reef limestone and few diamonds. That follows from the simulation and is intended.

## Randomness and presets

- Draws use the world seed (D-5), so the same seed gives the same history (tested).
- Per-world draws: L_ref (±5%), the mantle's excess temperature and cooling time (±20% each), the snowball threshold (−6 ± 3 °C), each snowball's duration (10–60 Myr) and each milestone's wait.
- **Random** (default; CLI `--history random`): full distributions.
- **Earth-like** (`--history earth`): every draw at its median and Earth's own milestone waits.
- **Custom** (planned): expose the main medians in an advanced tab.

## References

- Gough 1981 (solar luminosity over time).
- Walker, Hays & Kasting 1981 (silicate-weathering thermostat).
- Goldblatt, Lenton & Watson 2006 (bistable atmospheric oxygen).
- Krissansen-Totton, Arney & Catling 2018 (CO₂ dependence of land and sea-floor weathering).
