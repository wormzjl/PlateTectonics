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

```
 1. Global state      box models: mantle temperature, Sun, CO2, O2, life milestones   → 07
 2. Mantle            plume and upwelling field; plume-head arrivals (LIPs)           → 03
 3. Plate velocities  force/torque balance per plate                                  → 03
 4. Plate motion      move plates rigidly; compose the world raster                   → 03
 5. Boundaries        subduction, collision, rifting, ridges, transforms; reorganize  → 03
 6. Magmatism         arcs, hotspots, LIPs, rift volcanics, intrusions (event log)    → 03, 05
 7. Isostasy          elevation from crust thickness, density and ocean age           → 03
 8. Sea level         fill the ocean to its water volume (hypsometry)                 → 04
 9. Climate           temperature, wind, precipitation, ice (every N steps)           → 04
10. Surface processes rivers, erosion, transport, deposition, glaciers, weathering    → 04
11. Columns           append deposits, remove eroded tops, update burial and Tmax     → 05
12. Resources         score triggered families; instantiate deposits; preservation    → 06
13. Snapshot          downsampled frame for the preview timeline (every ~10–50 Myr)
```

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

**Global** (`WorldHistory.globalState(t)`): mantle potential temperature, solar luminosity, pCO₂, O₂ state, ocean redox, sea level, global mean temperature, seawater-chemistry toggle, biological flags. Owned by [07](07-life-atmosphere-sun.md); read by every deposit rule ([06](06-resources.md)).

**Event log:** append-only records `(type, t, location or segment, parameters)` for subduction segments (rate, obliquity, slab age), collisions, rifts, ridge segments, plume arrivals, LIPs, intrusions, glaciations, oxygenation steps and extinctions. Deposit generators trigger on these events.

## Determinism (D-5)

- RNG: a counter-based hash (SplitMix64 or a PCG mix) of `(worldSeed, streamId, step, cellIndex)`. No shared `Random` objects.
- Parallel loops split by fixed row bands. Reductions (sums, maxima) combine in band order. Never depend on `HashMap` iteration order.
- Every output carries `(modVersion, simVersion, settingsHash, seed)`. A mismatch on load means regenerate, or refuse with a clear message.
- Golden tests: fixed seeds produce byte-identical outputs across runs and thread counts.

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
