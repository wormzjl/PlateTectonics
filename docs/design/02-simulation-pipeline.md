# 02 · Simulation pipeline: time stepping, state, determinism, budgets

> **Level 1** · Read when: you add a process, change step order, add a state field, or touch threading, RNG or the save format.
> Decisions: D-3, D-4, D-5, D-6, D-15 · Related: [03](03-tectonics.md), [04](04-surface-processes.md), [05](05-lithology.md), [06](06-resources.md), [07](07-life-atmosphere-sun.md).

**Summary.** One deterministic loop advances the planet from t = 0 to t = T. Early eons use big time steps on a coarser grid, because most of their surface record gets recycled. The last few hundred Myr run at full detail, so total cost stays roughly constant whatever age the player picks. Every process reads and writes one shared `PlanetState`, appends to an **event log**, and updates per-cell **rock columns**. Those three outputs are everything the ore, map and chunk stages need.

## Time stepping

| Window (relative to the end, T) | Step dt | Grid | What runs |
|---|---|---|---|
| t < T − 1,000 Myr ("deep time") | 10 Myr | 2× coarser | Tectonics (simplified boundary rules), crust growth, box models, bulk erosion, event log, columns |
| T − 1,000 … T − 300 Myr | 5 Myr | full | All processes; climate every step |
| T − 300 … T − 10 Myr | 1–2 Myr | full | All processes |
| Last 10 Myr | 0.1–1 Myr | full | Adds glacial cycles and final climate; tectonics continues |

A world with T < 1,000 Myr runs at full detail throughout. Total steps stay at or below about 600 for any T. The windows are configuration, not code constants.

## Order of operations in one step

As built in Phase 3 (`PlanetSimulation.step`):

```
 1. Global state      box models: Sun, mantle, degassing, O2, CO2 and temperature,
                      life milestones (from the planet of the previous step)           → 07
 2. Plates            sea level; velocities (force balance); motion; subduction,
                      collision, ridges; subduction erosion; arcs; underthrusting,
                      crustal flow, roots; plumes, rifts, reorganization, basin closure → 03
 3. Sea level         fill the ocean to its water volume (hypsometry)                  → 04
 4. Climate           temperature, wind, precipitation, ice, land weathering strength  → 04
 5. Surface processes flow, erosion, transport, deposition, sea floor, regolith        → 04
 6. Sea level         again, with the water held in ice taken out of the ocean         → 04
    -- not built yet --
 7. Columns           append deposits, remove eroded tops, update burial and Tmax      → 05
 8. Resources         score triggered families; instantiate deposits; preservation     → 06
 9. Snapshot          downsampled frame for the preview timeline (every ~10–50 Myr)
```

**Couplings.** Each part reads what the others produced in the step before, so the loops close without iterating within a step:

- the box models read the previous step's new ocean floor (spreading) and plume heads, the climate's land weathering strength and land fraction, and the mean land erosion rate;
- the climate reads this step's global temperature and the previous step's erosion map (fresh rock weathers faster);
- erosion, deposition and weathering read this step's climate and the life milestones (vegetation, carbonate producers);
- ice lowers sea level, which the next step's plates and climate see.

Elevation is never stored; isostasy gives it whenever a part asks ([03](03-tectonics.md#isostasy-and-elevation)). The CLI flag `--plates-only` runs steps 1–2 alone, with the Phase 2 erosion stand-in and no climate or surface processes.

## State

**Per cell** (master grid, ≈15.6 km; see [01](01-world-model.md#parameters)):

| Field | Type | Unit | Notes |
|---|---|---|---|
| plateId | u16 | — | Owning plate after composition |
| crustType | u8 enum | — | Oceanic, continental, arc, oceanic plateau, rift, suture… |
| crustThickness | f32 | km | Drives isostasy |
| crustAge | f32 | Myr | Age of the oldest crust component (cratons) |
| oceanAge | f32 | Myr | Time since formation at a ridge (oceanic only) |
| elevation | f32 | m | Derived (isostasy + flexure + dynamic topography) |
| heatFlow | f16 | mW/m² | For burial temperature, maturation and metamorphism |
| upliftRate, exhumationTotal | f32 | m/Myr, m | Exhumation drives ore preservation |
| temperature, tempRange, precipitation | f16 | °C, °C, mm/yr | Present values plus running climate statistics |
| iceThickness, lastGlaciation | f16 | m, Myr | Glacial landforms in Tier 1.5 |
| sedimentFlux, drainageArea | f32 | m³/yr, km² | From the flow-routing pass |
| columnRef | i32 | — | Index into the rock-column store ([05](05-lithology.md)) |
| flags | u32 bitset | — | Arc, back-arc, ophiolite, suture, rift, hotspot track… |

**Per plate:** id, rigid transform (x, z, θ), velocity (vx, vz, ω), local raster, area, continental fraction, age, parent/child ids for splits and merges.

**Global** (a `GlobalState` per step, from `WorldHistory.state()`): mantle potential temperature, solar luminosity, degassing, global mean temperature, pCO₂, O₂ level and state, organic burial, methane warming, snowball flag, milestone times. Sea level lives in the plate simulation. Ocean redox and the seawater-chemistry toggle are planned with the resource rules. Owned by [07](07-life-atmosphere-sun.md); read by every deposit rule ([06](06-resources.md)).

**Event log:** append-only records `(type, t, location or segment, parameters)` for subduction segments (rate, obliquity, slab age), collisions, rifts, ridge segments, plume arrivals, LIPs, intrusions, glaciations, oxygenation steps and extinctions. Deposit generators trigger on these events.

## Determinism (D-5)

- RNG: a counter-based hash (SplitMix64 or a PCG mix) of `(worldSeed, streamId, step, cellIndex)`. No shared `Random` objects.
- Parallel loops (`parallel/Parallel`) write only the state of their own cell or row. Sums, lists and budgets are built afterwards on one thread in cell order, so every thread count gives the single-threaded result bit for bit. Work that follows drainage order or shares material between cells (routing, erosion, transport, marine spreading, breadth-first bands, overlap resolution in plate motion) stays on one thread. Speed-ups of that work must keep its visit order. Other threads may still work ahead for it if what they compute can only change speed: marine spreading replays walks made ahead on the workers and carries on any that stop short ([04 § Sea floor](04-surface-processes.md#sea-floor), D-40). For example, every ocean cell enters the Priority-Flood heap with the same key (sea level), so the heap's tie order decides which coast cell each land cell drains to; the heap therefore cannot become a bucket or radix queue. Never depend on `HashMap` iteration order.
- Every output carries `(modVersion, simVersion, settingsHash, seed)`. A mismatch on load means regenerate, or refuse with a clear message.
- Golden tests: fixed seeds produce byte-identical outputs across runs and thread counts (`PlanetSimulationTest`: 1 vs 5 threads; `SurfaceProcessesTest`: two runs).

## Budgets (reference machine: 8-core desktop)

| Item | Budget |
|---|---|
| Full default run (T = 4.5 Gyr, 2,560 × 1,280 grid) | ≤ 3 min |
| Peak memory during the simulation | ≤ 3 GB |
| Saved master data (compressed) | ≤ 100 MB |
| Rock-column store in memory | ≤ 400 MB (see [05](05-lithology.md#storage)) |
| Province tile (256 × 256 km at ≈1 km, Tier 1.5a) | ≤ 2 s, off-thread, then disk-cached |
| Local tile (512² blocks at 4 blocks, Tier 1.5b) | ≤ 50 ms |
| Chunk generation overhead vs vanilla | ≤ 1.5× |

If the 15.6 km grid cannot meet the run budget, the fallback is a 31 km master grid, with 15.6 km surface processes for the last 300 Myr.

## Output: MasterWorld file

- Location: `<world>/data/platetectonics/master.ptw`, plus `deposits.ptd`, `preview/`, and the province-tile cache `provinces/` (filled lazily as players explore; safe to delete, it regenerates deterministically).
- Layout: header (versions, seed, settings, grid size) + tiles of 64×64 cells, each compressed independently for random access.
- Contents: present-day fields, rock columns, river network, glacial fields, deposit index, and preview snapshots (downsampled maps every 10–50 Myr for the timeline scrubber).
- **As built (format 1, Phase 4):** `world/MasterWorld` + `world/MasterWorldIO`. It is one gzip stream, not yet tiled. A header (magic `PTMW`, format, simulation version, seed, grid width, age, history preset, ocean depth, grid) is followed by 17 named per-cell fields:
  - surface: elevation, temperature, seasonal range, precipitation, aridity, ice, discharge, erosion rate, lakes, climate class;
  - crust: crust type and thickness, sediment, regolith, exhumation, formation time, plate.

  Fields are matched by name on load. The file is written to a temporary file and then moved into place. Size is ~15 MB at 31.25 km. Rock columns, rivers, deposits and preview snapshots are added with their phases.
