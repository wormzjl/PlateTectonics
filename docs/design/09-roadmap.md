# 09 · Roadmap

> **Level 1** · Read when: you plan work or need to know what "done" means for a phase.
> Rule: get to a **walkable world early** (Phase 4), then deepen.

| Phase | Deliverable | Exit criteria |
|---|---|---|
| **0 Design** ✅ | This folder; research report | Owner reviewed decisions; open items listed |
| **1 Setup** ✅ | Git repo; Gradle multi-project (`geosim-core`, `geosim-cli`, `mod`); NeoForge 21.1.x mod skeleton that loads; Java 21; unit-test harness (the golden-image test helper moved to Phase 2, when there are real maps to pin) | `gradlew build` passes (12 unit tests); the mod loads in NeoForge 21.1.259 (verified headless via `runData`; `runClient` not yet tried); CLI writes a proto-terrain map |
| **2 Plates** ✅ (core) | Proto-crust; rigid plates on one world raster (whole-cell translation; rotation deferred, D-21); force balance; subduction with remembered polarity, accretion, collision with underthrusting; plateaus, roots and delamination (D-23); rifting, ridges, subduction initiation; crust growth curve; isostasy; sea level by volume; event log v0; CLI PNG maps (elevation, plates, crust age, crust type), timeline GIF, events CSV | Met: a 4.5 Gyr run grows continents from nuclei and arcs, collides, rifts and accretes them (37–40% continental, 34–35% land, ~4 cm/yr); same seed gives the same history (test); 22 unit tests pass; benchmark done (O-6). **Not met:** a clear supercontinent assembly and breakup is not visible yet, so it moves to Phase 8 tuning. The golden-image test is replaced by a bit-exact determinism test |
| **3 Climate and erosion** | Box models (Sun, mantle, CO₂, O₂, life milestones); climate field; FastScape erosion and deposition; glaciers v0; weathering depth; biome classification; maps for each | Rain shadows, deserts and ice caps in plausible places; river networks and deltas; eroded old ranges vs sharp young ranges |
| **4 First walkable world** | Vanilla `PlanetChunkGenerator`; `TfcPlanetChunkGenerator` + `ChunkDataGenerator`; MasterWorld save and load; preset + Customize screen with Simulate and map preview; TFC default veins (D-9 step 1) | Create a world in-game with and without TFC; terrain matches the preview; TFC seasons, crops and prospecting work; no crashes from TFC features |
| **5 Region refinement** | Tier 1.5a province tiles (local landscape evolution, disk cache) and Tier 1.5b local tiles: river channels and lakes, coastlines, glacial landforms (U-valleys, fjords, moraines), detail synthesis; height expansion option | Rivers continuous across chunks; no seams at tile borders; chunk-generation overhead within budget |
| **6 Rocks and volcanoes** | Rock columns rendered (folds, tilts, unconformities) to TFC rocks; metamorphism; volcano structures from simulated volcanics; soils | Geologic map at the surface matches the column data; ranges expose old cores and basins young cover |
| **7 Resources** | Deposit pipeline G1–G8; preservation; `DepositFeature` (D-9 step 2); indicators and gossans; coal ranks; petroleum simulation with tar seeps and oil shale; progression floor; richness multipliers | Validation targets roughly met ([06](06-resources.md#validation-targets-for-tuning-not-hard-rules)); TFC progression (bronze, iron, redstone) reachable on many random seeds |
| **8 Deep time and polish** | Supercontinent-cycle tuning (add a largest-continent-share metric; Phase 2 runs show no clear cycle yet); plate rotation if maps need it; extinctions; timeline scrubber UI; dedicated-server map; teleport-wrap option (A-12); lat/long readout and in-game map; oil and gas fluids; optional addon palettes (Firmalife, Metallum) | Owner sign-off |

## Phase 2 results

- **Code:** `geosim-core` packages `tectonics` (`PlateSimulation`, `PlateMotion`, `PlateDynamics`, `PlateReorganizer`, `SubductionPolarity`, `CrustalFlow`, `OrogenicRoots`, `Isostasy`, `TectonicsConfig`), `events`, `io` (`TectonicMaps`, `GifWriter`), `terrain/SeaLevel` (1 m histogram fill).
- **CLI:** `geosim-cli --seed 42 --cell-km 31.25 --age 4500 --frame-every 50 --gif-width 640 --out maps`. It writes `maps/seed_<seed>/` (elevation, elevation with plates, plates, crust age, crust type, timeline GIF, events CSV) and prints a crust budget (arcs, shelves, erosion, collision, eclogite, subducted arc crust).
- **Era steps:** 10 Myr until 1 Gyr before the chosen age, 5 Myr until 300 Myr before, then 2 Myr.
- **Benchmark** (one thread, plates only, 4.5 Gyr):

  | Cell | Grid | ms/step | Total |
  |---|---|---|---|
  | 78 km | 512 × 256 | ~11 | ~7 s |
  | 31.25 km | 1,280 × 640 | ~115 | ~75 s |
  | 15.625 km | 2,560 × 1,280 | ~635 | ~7 min |

- **Lessons** (now decisions D-22, D-25): arc crust must be buoyant and trenches must keep their polarity, or continents never grow. Every rule must be in km and Myr, not cells, or behaviour changes with resolution.

## Phase 3 detail (next up)

See [04 · Surface processes](04-surface-processes.md) and [07 · Life, atmosphere, Sun](07-life-atmosphere-sun.md). The erosion stand-in in `PlateSimulation.erodeThickCrust` is replaced by real erosion and deposition. Plate crust thickness becomes the tectonic uplift input.
