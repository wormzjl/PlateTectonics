# PlateTectonics — Design (start here)

> **Level 0 — always read this page.** It fits on one screen. Open a Level 1 page only for the subsystem you are working on; open Level 2 material only when implementing a specific algorithm.

## What it is

A Minecraft **1.21.1 NeoForge** world-generation mod. At world creation it **simulates a planet's geologic history**: proto-terrain → mantle and plates → climate, erosion and glaciers → rock layers, ores, coal and oil. The player picks how much time has passed. The resulting **master terrain** is shown as a map before play. When the player explores, chunks are **refined** from that master data. It works with **TerraFirmaCraft (TFC)** by default and falls back to vanilla blocks without it.

## Player flow

```
Create World → preset "PlateTectonics" → Customize:
  seed · elapsed time (Myr) · scale factor (default 30 m per block)
→ Simulate (progress bar, ~1–3 min) → master map preview (elevation, plates, climate, rock, resources, timeline)
→ Accept / Reroll → play → chunks refined on demand
```

## Architecture at a glance

```
seed + settings
   │
   ▼
geosim-core  (pure Java, no Minecraft)        Tier 1: master simulation, whole planet, ≈15.6 km cells
   │  writes MasterWorld (world/data/platetectonics/)
   ▼
Province tiles (≈1 km cells, disk cache)       Tier 1.5a: local landscape evolution, glaciers, rock layers
   ▼
Local tiles (4-block cells, memory cache)      Tier 1.5b: detail, river channels, lake shores, volcano cones
   ▼
Chunk generator (NeoForge; TFC adapter)        Tier 2: block-level detail, strata, ore bodies, surface
```

## Core decisions (full log: [decisions.md](decisions.md))

| | Decision |
|---|---|
| World shape | **Earth-sized cylinder**: repeats east–west (≈1.33 M blocks around), poles at the north/south edges |
| Scale | **Scale factor S = 30 m per block in every direction** (no vertical exaggeration), tweakable |
| Height | Vanilla −64…320 at S = 30 (deep ocean and highest peaks gently compressed); taller dimension optional |
| Compatibility | **TFC by default** (soft dependency), vanilla fallback; our world type sits **next to** TFC's, which we never modify |
| Rocks | TFC's 21 rocks **plus our own sandstone rock** (TFC-compatible) |
| Redstone | TFC **cinnabar** (and cryolite) = redstone source |
| Life and atmosphere | **Simulated** (cheap 0-D model) **with randomness** |
| First build | Phases 1–2: plate engine and maps in pure Java, PNG output |

## Document map

| Doc | Level | Read when |
|---|---|---|
| [decisions.md](decisions.md) | L1 | You need to know *why* something is the way it is, or what is still open |
| [01-world-model.md](01-world-model.md) | L1 | Coordinates, scale factor, latitude, vertical mapping, grid sizes |
| [02-simulation-pipeline.md](02-simulation-pipeline.md) | L1 | Time stepping, step order, data fields, determinism, performance budgets |
| [03-tectonics.md](03-tectonics.md) | L1 | Mantle, plates, boundaries, isostasy, proto-terrain |
| [04-surface-processes.md](04-surface-processes.md) | L1 | Climate, rivers, erosion and deposition, glaciers, weathering, sea level |
| [05-lithology.md](05-lithology.md) | L1 | Rock columns, metamorphism, mapping to TFC rocks |
| [06-resources.md](06-resources.md) | L1 | Ores, gems, industrial minerals, coal, oil and gas |
| [07-life-atmosphere-sun.md](07-life-atmosphere-sun.md) | L1 | Oxygen, life milestones, the Sun's brightness, glaciation triggers |
| [08-minecraft-integration.md](08-minecraft-integration.md) | L1 | NeoForge and TFC integration, chunk pipeline, UI, save data |
| [09-roadmap.md](09-roadmap.md) | L1 | Phases, deliverables and exit criteria |
| [Research report](../reports/Ore%20deposits%20and%20TFC%20compatibility.md) | L2 | Full deposit catalog, TFC internals, sources |
| [Research notes](../research_notes/Ore%20deposits%20and%20TFC%20compatibility/) | L2 | Raw, cited notes behind the report |
| [Tibetan plateau support](../research_notes/Tibetan%20plateau%20support/tibetan_plateau_support.md) | L2 | Why thick crust stays high (roots, delamination, flow, ceiling); model rules R1–R9 behind [03 § plateaus](03-tectonics.md#plateaus-and-roots) |

## Status

**Phase 2 core done**: the plate engine builds a 4.5 Gyr planet in ~75 s at 31 km cells, including Tibet-style plateaus that are held up by buoyant crust and lose their dense roots ([03](03-tectonics.md#plateaus-and-roots)). **Phase 3 core done**: climate, a life-coupled history (oxygen and CO₂ built with life; [07](07-life-atmosphere-sun.md)) and erosion and deposition ([04](04-surface-processes.md)); the planet step runs on all cores with identical results (~1.5 min per world at 31 km). **Phase 4 in progress**: vanilla and TFC world types that build chunks from the saved planet ([08](08-minecraft-integration.md#as-built-phase-4-first-slice)); tuning is deferred. **World model refined before chunk generation** (D-36–D-40): coherent continents without lone pillars or diamond-shaped coasts, Earth-like forest and grassland shares; a 31.25 km world now takes ~2.5 min ([09](09-roadmap.md#world-model-refinement)). Open: O-6, the master cell size ([decisions](decisions.md#open)). See [09-roadmap.md](09-roadmap.md).
