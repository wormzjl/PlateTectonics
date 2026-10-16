# 09 · Roadmap

> **Level 1** · Read when: you plan work or need to know what "done" means for a phase.
> Rule: get to a **walkable world early** (Phase 4), then deepen.

| Phase | Deliverable | Exit criteria |
|---|---|---|
| **0 Design** ✅ | This folder; research report | Owner reviewed decisions; open items listed |
| **1 Setup** ✅ | Git repo; Gradle multi-project (`geosim-core`, `geosim-cli`, `mod`); NeoForge 21.1.x mod skeleton that loads; Java 21; unit-test harness (the golden-image test helper moved to Phase 2, when there are real maps to pin) | `gradlew build` passes (12 unit tests); the mod loads in NeoForge 21.1.259 (verified headless via `runData`; `runClient` not yet tried); CLI writes a proto-terrain map |
| **2 Plates** ✅ (core) | Proto-crust; rigid plates on one world raster (whole-cell translation; rotation deferred, D-21); force balance; subduction with remembered polarity, accretion, collision with underthrusting; plateaus, roots and delamination (D-23); rifting, ridges, subduction initiation; crust growth curve; isostasy; sea level by volume; event log v0; CLI PNG maps (elevation, plates, crust age, crust type), timeline GIF, events CSV | Met: a 4.5 Gyr run grows continents from nuclei and arcs, collides, rifts and accretes them (37–40% continental, 34–35% land, ~4 cm/yr); same seed gives the same history (test); 22 unit tests pass; benchmark done (O-6). **Not met:** a clear supercontinent assembly and breakup is not visible yet, so it moves to Phase 8 tuning. The golden-image test is replaced by a bit-exact determinism test |
| **3 Climate and erosion** ✅ (core) | Box models (Sun, mantle, CO₂, O₂, life milestones), coupled through life (D-27, D-28); climate field; FastScape-style erosion and deposition with a sub-grid relief term (D-30); glaciers v0; weathering depth; biome classification; maps for each | Met: rain shadows on the lee of ranges, interior deserts and polar ice caps; river networks with lakes, endorheic basins, shelves and deltas; young ranges stay sharp while old ones wear to lowlands; the Earth-like preset replays Earth's order and roughly its timing; sediment mass is conserved (test); 34 unit tests pass. **Partly met:** land runs 35–39% (Earth 29%) and standing ocean-floor sediment is ~2–3× an Earth-scaled estimate (see Phase 3 results) |
| **4 First walkable world** 🚧 | Vanilla `PlanetChunkGenerator`; `TfcPlanetChunkGenerator` + `ChunkDataGenerator`; MasterWorld save and load; preset + Customize screen with Simulate and map preview; TFC default veins (D-9 step 1) | Create a world in-game with and without TFC; terrain matches the preview; TFC seasons, crops and prospecting work; no crashes from TFC features |
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

## Phase 3 results

- **Code:** `geosim-core` packages `history` (`WorldHistory`, `GlobalState`, `Milestone`), `climate` (`ClimateModel`, `Climate`, `Biome`), `surface` (`FlowRouting`, `SurfaceProcesses`, `SurfaceConfig`), `planet/PlanetSimulation` (step order and couplings, [02](02-simulation-pipeline.md)), `io/SurfaceMaps`. The plate engine gained subduction erosion (D-32); its erosion stand-in now runs only with `--plates-only`.
- **CLI:** `--history random|earth`, `--plates-only`, `--set CONSTANT=VALUE` (any `TectonicsConfig` or `SurfaceConfig` constant, for tuning). New outputs: temperature, precipitation, biome, rivers, ice, erosion, sediment and regolith maps; `history.csv` (global state per step); sediment budget, life timeline, hypsometry and a time split in the summary.
- **Results at 31.25 km, 4.5 Gyr** (defaults; Earth-like preset for seed 42, random histories for the others):

  | Seed | Continental crust | Land | T today | O₂ today | Ice on land | Land erosion | Continental elevation p50 / p90 |
  |---|---|---|---|---|---|---|---|
  | 42 (Earth-like) | 36% | 39% | 15.5 °C | 0.99 PAL | 17% | 39 m/Myr | 321 / 2,299 m |
  | 1 | 36% | 39% | 15.0 °C | 1.40 PAL | 21% | 54 m/Myr | 432 / 2,187 m |
  | 2 | 36% | 37% | 19.5 °C | 0.70 PAL | 10% | 42 m/Myr | 142 / 1,199 m |
  | 5 | 35% | 39% | 12.0 °C | 1.39 PAL | 29% | 68 m/Myr | 539 / 2,323 m |
  | 99 | 36% | 35% | 15.5 °C | 1.52 PAL | 19% | 65 m/Myr | 279 / 1,992 m |

  Seed 42's history: GOE with a 35 Myr snowball at 2,010 Myr (2.5 Ga), eukaryotes 2,510, algae and NOE 3,610, animals 3,860, land plants 3,960, forests 4,020, calcareous plankton 4,210, diatoms 4,310 Myr; CO₂ today 409 ppm.
- **Benchmark** (Ryzen 7 9700X, 8 cores / 16 threads; seed 42 Earth-like, 4.5 Gyr, 640 steps; `--threads N`, default all cores):

  | Cell | Threads | ms/step | World | Plates / climate / surface |
  |---|---|---|---|---|
  | 31.25 km | 1 (before multithreading) | 334 | 3.6 min | 117 / 19 / 78 s |
  | 31.25 km | 8 (before faster spreading and routing) | 177 | 1.9 min | 39 / 6 / 67 s |
  | 31.25 km | 16 (before faster spreading and routing) | 177–189 | 1.9–2.0 min | 39–41 / 6 / 68–74 s |
  | 31.25 km | 16 | **138** | **1.5 min** | 39 / 6 / 43 s |
  | 78 km | 1 | 24.4 (before: 26.9) | 16 s | 6.4 / 2.2 / 6.8 s |
  | 78 km | 8 | 16.7 (before: 20.5) | 11 s | 4.2 / 1.1 / 5.2 s |
  | 78 km | 16 | 15.9 (before: 19.7) | 10 s | 4.1 / 1.1 / 4.9 s |

  Every thread count writes byte-identical maps, history and events (D-5; `PlanetSimulationTest` checks 1 vs 5 threads). Per-cell loops run in parallel: the plate move copy (the largest cost, memory-bound), ocean ageing, elevations and sea level, temperature, the moisture sweeps (by row) and mixing, sea-floor sediment, weathering and the routing setup. What stays on one thread follows drainage order or shares material between cells.
- **Faster spreading and routing (same results):** marine spreading (~27% of the main thread) and Priority-Flood routing (~23%) were the sequential bottleneck. Both still visit cells in exactly the same order, so every output stays byte-identical, but each cell costs less:
  - Marine spreading runs a breadth-first search from each river mouth, ~25,000 mouths per step at 31.25 km. 14% of mouths pass the shelf with sediment left and search the whole apron radius (~900 cells each); they cause 80% of the search work. Each queue entry now carries its distance and row, and one stamp test skips both land and visited cells.
  - The routing heap packs key and cell into one `long` and picks the smaller child without a branch. Ties keep the old heap's order ([02 § Determinism](02-simulation-pipeline.md#determinism-d-5)).
  - Result at 16 threads: routing 25 → 13 s and spreading 28 → 14–16 s per world; surface 68–74 → 43 s. Plates and surface now take about the same time.
- **Tuning method:** sweeps of one JVM per variant, 6–8 at a time, at 78 km to find directions, then confirmation at 31.25 km. Erosion constants were tuned against present-day land erosion (Earth ~50 m/Myr) and continental hypsometry.
- **Lessons:**
  - Coarse cells cannot resolve the steep slopes that do most mountain erosion. Stream power alone planed lowlands but left 4 km plateaus, so a sub-grid relief term was needed (D-30).
  - Mass-conserving erosion removed the stand-in's hidden crust sink. Subduction erosion (D-32) and keeping sea-floor sediment oceanic (D-31) brought continental area back from ~58%.
  - Weathering has to depend on CO₂ itself, or the faint young Sun drops out of the climate (D-28).
  - Oxygen needs smoothing and hysteresis; under a snowball the oxygen system has to freeze. Otherwise it flickers between states and triggers repeated snowballs.
- **Known gaps:**
  - Land is 35–39%. More ocean water lowers it only weakly and noisily (seed 99: 40% → 25% from 2,620 to 3,500 m; seed 42: ~33% throughout). `--ocean-depth` stays a world setting.
  - Standing ocean-floor sediment is 1.1–1.6 Gkm³, ~2–3× an Earth-scaled estimate. The marine flux is Earth-like but residence is ~120 Myr.
  - 4–6% of the surface is sediment-filled ocean slivers between continental blocks (land on oceanic crust).
  - Land erosion at 31.25 km runs about half its value at 78 km.
  - Glaciers v0, the row-based wind and the missing climate statistics are listed in [04](04-surface-processes.md).

## Phase 4 progress

Parameter tuning is deferred (owner, 2026-10-16: "do not waste time on fine tuning for now"). Phase 3's known gaps stay open.

- **Done** (details in [08 § As built](08-minecraft-integration.md#as-built-phase-4-first-slice)):
  - MasterWorld save and load (format 1).
  - `TerrainSampler` (block-resolution terrain and climate).
  - Vanilla `PlanetChunkGenerator` and biome source.
  - World types `planet`, `planet_quick` and `planet_tfc`.
  - Simulate-on-first-start, then load on later starts; spawn on land.
  - TFC `ChunkGeneratorExtension` + `ChunkDataGenerator` (climate corners, placeholder rocks, TFC soils, salt water, TFC landform biomes). TFC's default veins can run on our TFC raw rocks (D-9 step 1).
  - GameTests for both modes (pass with and without TFC 4.2.12); 37 unit tests.
- **Not yet:**
  - A real game session: a dedicated server needs the EULA accepted in `mod/run`, or the owner runs the client. Checks still open: in-game walkability, TFC seasons, crops and prospecting, the TFC preset in the world-type list next to TFC's (A-11), and TFC's climate model at `temperature_scale` 333,333.
  - The Customize screen with Simulate, a progress bar and the map preview. Until it exists, the world simulates on its first start and the client waits.
  - Caves, rivers and lakes in chunks (Phase 5); strata (Phase 6).
- O-6 is still open. The save format stores the grid size, so 15.6 km and 31.25 km planets both load.
