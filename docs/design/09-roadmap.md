# 09 · Roadmap

> **Level 1** · Read when: you plan work or need to know what "done" means for a phase.
> Rule: get to a **walkable world early** (Phase 4), then deepen.

| Phase | Deliverable | Exit criteria |
|---|---|---|
| **0 Design** ✅ | This folder; research report | Owner reviewed decisions; open items listed |
| **1 Setup** ✅ | Git repo; Gradle multi-project (`geosim-core`, `geosim-cli`, `mod`); NeoForge 21.1.x mod skeleton that loads; Java 21; unit-test harness (the golden-image test helper moved to Phase 2, when there are real maps to pin) | `gradlew build` passes (12 unit tests); the mod loads in NeoForge 21.1.259 (verified headless via `runData`; `runClient` not yet tried); CLI writes a proto-terrain map |
| **2 Plates** ✅ (core) | Proto-crust; rigid plates on one world raster (whole-cell translation, D-21; rotation added later, D-42); force balance; subduction with remembered polarity, accretion, collision with underthrusting; plateaus, roots and delamination (D-23); rifting, ridges, subduction initiation; crust growth curve; isostasy; sea level by volume; event log v0; CLI PNG maps (elevation, plates, crust age, crust type), timeline GIF, events CSV | Met: a 4.5 Gyr run grows continents from nuclei and arcs, collides, rifts and accretes them (37–40% continental, 34–35% land, ~4 cm/yr); same seed gives the same history (test); 22 unit tests pass; benchmark done (O-6). **Not met:** a clear supercontinent assembly and breakup is not visible yet, so it moves to Phase 8 tuning. The golden-image test is replaced by a bit-exact determinism test |
| **3 Climate and erosion** ✅ (core) | Box models (Sun, mantle, CO₂, O₂, life milestones), coupled through life (D-27, D-28); climate field; FastScape-style erosion and deposition with a sub-grid relief term (D-30); glaciers v0; weathering depth; biome classification; maps for each | Met: rain shadows on the lee of ranges, interior deserts and polar ice caps; river networks with lakes, endorheic basins, shelves and deltas; young ranges stay sharp while old ones wear to lowlands; the Earth-like preset replays Earth's order and roughly its timing; sediment mass is conserved (test); 34 unit tests pass. **Partly met:** land runs 35–39% (Earth 29%) and standing ocean-floor sediment is ~2–3× an Earth-scaled estimate (see Phase 3 results) |
| **4 First walkable world** 🚧 | Vanilla `PlanetChunkGenerator`; `TfcPlanetChunkGenerator` + `ChunkDataGenerator`; MasterWorld save and load; preset + Customize screen with Simulate and map preview; TFC default veins (D-9 step 1) | Create a world in-game with and without TFC; terrain matches the preview; TFC seasons, crops and prospecting work; no crashes from TFC features |
| **5 Region refinement** | Tier 1.5a province tiles (local landscape evolution, disk cache) and Tier 1.5b local tiles: river channels and lakes, coastlines, glacial landforms (U-valleys, fjords, moraines), detail synthesis; height expansion option | Rivers continuous across chunks; no seams at tile borders; chunk-generation overhead within budget |
| **6 Rocks and volcanoes** | Rock columns rendered (folds, tilts, unconformities) to TFC rocks; metamorphism; volcano structures from simulated volcanics; soils | Geologic map at the surface matches the column data; ranges expose old cores and basins young cover |
| **7 Resources** | Deposit pipeline G1–G8; preservation; `DepositFeature` (D-9 step 2); indicators and gossans; coal ranks; petroleum simulation with tar seeps and oil shale; progression floor; richness multipliers | Validation targets roughly met ([06](06-resources.md#validation-targets-for-tuning-not-hard-rules)); TFC progression (bronze, iron, redstone) reachable on many random seeds |
| **8 Deep time and polish** | Supercontinent-cycle tuning (add a largest-continent-share metric; Phase 2 runs show no clear cycle yet); extinctions; timeline scrubber UI; dedicated-server map; teleport-wrap option (A-12); lat/long readout and in-game map; oil and gas fluids; optional addon palettes (Firmalife, Metallum) | Owner sign-off |

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
  | 31.25 km | 16 (world-model refinement, round fans) | 258 | 2.8 min | 51 / 7 / 107 s |
  | 31.25 km | 16 (round fans, walks made ahead) | **235** | **2.5 min** | 55 / 8 / 87 s |
  | 31.25 km | 16 (exact speedups after the review; before, same day: 188–197) | **175–179** | **1.9 min** | 38 / 6 / 68–69 s |
  | 78 km | 1 | 24.4 (before: 26.9) | 16 s | 6.4 / 2.2 / 6.8 s |
  | 78 km | 8 | 16.7 (before: 20.5) | 11 s | 4.2 / 1.1 / 5.2 s |
  | 78 km | 16 | 15.9 (before: 19.7) | 10 s | 4.1 / 1.1 / 4.9 s |
  | 78 km | 16 (world-model refinement, round fans) | 23.6–23.9 | 15 s | 5.4 / 1.5 / 8.0 s |
  | 78 km | 16 (round fans, walks made ahead) | 21.2–21.4 | 14 s | 5.2 / 1.5 / 6.7 s |
  | 78 km | 1 (round fans; no walks ahead on one thread) | 39.5 (before: 39.3) | 25 s | 10.2 / 4.6 / 10.3 s |
  | 78 km | 16 (exact speedups after the review) | 16.9–17.1 (before: 19.7) | 11 s | 3.9 / 1.5 / 5.4 s |
  | 78 km | 1 (exact speedups after the review) | 35.3 (before: 36.8) | 23 s | 9.0 / 4.4 / 9.1 s |

  Every thread count writes byte-identical maps, history and events (D-5; `PlanetSimulationTest` checks 1 vs 5 threads). Per-cell loops run in parallel: the plate move copy (the largest cost, memory-bound), ocean ageing, elevations and sea level, temperature, the moisture sweeps (by row) and mixing, sea-floor sediment, weathering and the routing setup. What stays on one thread follows drainage order or shares material between cells.
- **Faster spreading and routing (same results):** marine spreading (~27% of the main thread) and Priority-Flood routing (~23%) were the sequential bottleneck. Both still visit cells in exactly the same order, so every output stays byte-identical, but each cell costs less:
  - Marine spreading runs a breadth-first search from each river mouth, ~25,000 mouths per step at 31.25 km. 14% of mouths pass the shelf with sediment left and search the whole apron radius (~900 cells each); they cause 80% of the search work. Each queue entry now carries its distance and row, and one stamp test skips both land and visited cells.
  - (Replaced by the ring queue of D-40, which draws round fans but costs more, then made faster with walks made ahead; see [World-model refinement](#world-model-refinement).)
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
  - 4–6% of the surface is sediment-filled ocean slivers between continental blocks (land on oceanic crust). *Resolved by basin closure (D-36): 0–1.5%.*
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

## World-model refinement

Owner request (2026-10-16): "Refine the world model and parameters before further moving to the chunk gen"; chosen scope: **climate and biomes** and **coherent continents**. Owner note on sea level: it must not be fixed while plates move. It is not: the water volume is fixed and sea level floats (D-26; [04 § Sea level](04-surface-processes.md#sea-level)).

- **Changes:**
  - Basin closure (D-36).
  - Climate refinements: temperature profile, vegetation recycling, poleward monsoon and storm-track sweeps, crest-based rain shadows, recalibrated weathering (D-37).
  - Sediment kept inside its crust column (D-38).
  - Small hollows below sea level are lakes, not sea (D-39).
  - Round marine spreading with capped aprons and prograding deltas (D-40).
- **What was wrong.** The maps showed lone white pillars over land and sea, and speckled, flooded continental platforms. Crustal flow moved crust but left its sediment, so columns carried more sediment than crust, and isostasy raised them to the elevation cap. Accretionary wedges, aprons in narrow straits and one-cell inland "seas" fed sediment into single cells without limit; once a cell sat at the cap, the cap hid the pile from erosion and it kept growing (up to 1,200 km of sediment). Breadth-first spreading drew diamond-shaped shelves and deltas.
- **Results at 4.5 Gyr** (Earth-like preset, final code):

  | Cell | Seed | Continental crust | Land | Flooded continent | Continent p50 | Ocean floor age | Desert / forest / ice (% of land) | GOE |
  |---|---|---|---|---|---|---|---|---|
  | 78 km | 1 | 41% | 39% | 7% | 211 m | 93 Myr | 6 / 39 / 20 | 2,060 Myr |
  | 78 km | 5 | 36% | 33% | 12% | 117 m | 77 Myr | 6 / 44 / 13 | 2,370 Myr |
  | 78 km | 42 | 38% | 35% | 11% | 237 m | 120 Myr | 11 / 36 / 20 | 2,150 Myr |
  | 78 km | 99 | 40% | 40% | 6% | 243 m | 102 Myr | 7 / 36 / 21 | 1,930 Myr |
  | 31.25 km | 42 | 43% | 42% | 6% | 177 m | 69 Myr | 9 / 42 / 21 | 2,420 Myr |
  | 31.25 km | 1 | 46% | 43% | 8% | 246 m | 61 Myr | 14 / 35 / 21 | 1,820 Myr |

  Earth: ~40% continental crust, 29% land, desert ~20 / forest ~31 / ice ~10% of land, ocean floor ~64 Myr, GOE ~2,100 Myr after formation. Cells more than 800 m above all eight neighbours fell from 463 to 81 at 78 km; those left are sediment wedges at trenches, forearc highs and thick crust at orogen edges. The thickest sediment is 27–54 km (it was 1,200+ km). Delta-plain dumping fell from 9 to 0.7–1.2 × 10⁹ km³.
- **Cost.** A 31.25 km world took ~2.5 min on a busy machine (plates 51–55 s, climate 7–8 s, surface 87 s; it was 1.5 min before this refinement), and ~1.9 min after the exact speedups below. Most of the increase is marine spreading: round fans search ~1.4× the cells of the old diamonds, and each visit costs more. Basin closure adds ~12 s to the plates.
- **Faster round spreading (same results).** Owner request: speed up round marine spreading without changing what it builds. Every thread count still writes byte-identical maps, history and events (checked at 78 km on 1, 2 and 16 threads and at 31.25 km on 16).
  - How: walks made ahead on the workers, replayed in cell order ([04 § Sea floor](04-surface-processes.md#sea-floor), D-40).
  - Marine spreading per world, 16 threads: 78 km 3.7 → 2.1 s; 31.25 km 69 → 46 s (17 s walking ahead on the workers, 29 s replay on the calling thread). Surface step: 8.0 → 6.7 s and 107 → 87 s. One thread does not walk ahead and is ~5% slower in the surface step (each ring is copied before it is used).
  - What limits it: the replay reads each cell's elevation once (memory-bound), and the 4% of walks that stop short are carried on by one thread (~0.8 × 10⁹ cells at 31.25 km). Breadth-first spreading took 1.35 s at 78 km, so round fans still cost more.
  - Tried and dropped: walking ahead while the replay runs (the replay slowed by 40% sharing cores with the workers, net slower); walks ahead from the sea floor at the batch start, without the worker's own deposits (about twice as many short walks); bands of mouth index instead of columns (neighbours one row up sit dozens of mouths earlier); remembering last step's reach per mouth (8% fewer short walks, no faster).
- **Exact speedups after the review (same results).** Owner request: carry out the exact items of the [marine spreading review](../reports/Marine%20spreading%20review.md#status-after-the-exact-speedups), plates side included. Maps, history and events stay byte-identical on 1, 2 and 16 threads at 78 km and on 16 at 31.25 km; 47 tests. A JFR profile and counts of the marine work chose what to do.
  - Plates: plate statistics without list lookups (P2); crustal-flow ceilings, eclogite and keel updates and rift sides on the worker threads; the last subduction pair remembered; basin closure finds ocean regions as runs along rows joined by union-find, and floods only the enclosed ones (P10; [03](03-tectonics.md#boundary-processes)).
  - Surface: no `pow` call for slope exponent 1 (P1). Water bodies (D-39) are found as runs too. Walk records flag cells already at shelf or delta-top level, batches are sized by what they record (at most ~200 MB), and visited marks take one byte per cell ([04 § Sea floor](04-surface-processes.md#sea-floor)).
  - Code-review fixes: a cap on the cells recorded per batch (C1), a window guard in the sea walk (C2), the grid-size limit documented (C3), and tests for carried-on walks, capped records, whole-grid walks, odd widths and one- or two-row grids (C5).
  - Time, alternating runs on 16 threads at 31.25 km: 121–126 s → 112–114 s (plates 44 → 38 s, surface 70–75 → 68–69 s). At 78 km: 12.6 → 10.9 s; one thread 23.6 → 22.6 s. The machine was shared, and a single 31.25 km run varies by up to ±8 s.
  - Measured and dropped:
    - Skipping the apron search where nothing in reach is deep (P4): 1.9% of mouths.
    - Skipping walks across full seas (P5): 0.8% of mouths, 2% of cells.
    - Spare rings recorded past each walk (P6): 20–27% fewer carried-on walks, but the long ones remain; no faster.
    - Skipping the first sea-level update of a step (P3): ~1 s, and it needs a proof about the elevation cap.
  - What limits marine spreading now (~44 s at 31.25 km: 15 s ahead on the workers, 29 s replay): the replay's serial pass over ~4.4 × 10⁹ recorded cells, and ~0.8 × 10⁹ cells of walks carried on by one thread. The flags skip 87% of the elevation reads but save only ~1.7 s. The next exact step is to replay only the cells with room (estimated −10 s), best done after the [marine science changes](../reports/Marine%20science%20plan.md).
- **Marine science (D-41).** Owner request: "Approve H2, H3 and H8 as written; leave H9 for Phase 6; retune sea level toward Earth's 29% land." Built from the [marine science plan](../reports/Marine%20science%20plan.md); measuring the rules as written changed five of them ([§ Outcome](../reports/Marine%20science%20plan.md#outcome)).
  - Shelves sized by river supply and margin, a ramp profile, 35% bypass to fans under a continental slope and rise, deltas that build land at the mouth, a drape that raises the whole margin, depth-limited and mud-limited carbonate, mouth scan order rotating per step ([04 § Sea floor](04-surface-processes.md#sea-floor)).
  - The CLI report gained the validation lines: ocean area by depth (and how much of the shallow part is flooded continent), shelf width, thickest sediment, land built from the sea, displaced water, the water that would give 29% land, and the marine sediment by part.
  - Default ocean water 2,660 m: 12 seeds at 78 km average 29.1% land (13–39%), 8 seeds at 31.25 km 27.9% (14–41%); with Earth's 2,620 m it was 33.9% at 78 km.
  - Same maps on 1 and 16 threads (seed 42 at 78 km and at 31.25 km); 47 tests.
  - Cost: a 78 km world ~19 s (was ~11.5 s), a 31.25 km world ~230 s (was ~117 s; surface ~180 s). Each mouth walks out to 3 fan lengths and deposits on every fan cell; the old rules stopped once the sediment ran out. The walk-record flags are gone (fill levels now differ per cell and mouth). ~3% of mouths at 78 km walk on past the recorded window: their fan is full to the shelf break and the shelf builds out.
- **Plate rotation (D-42).** Owner request: "Approve the plate-rotation plan as written: per-plate rasters stored as (u, v) on the world raster, spin from torque with spinDragFactor, and build it now as D-42." Built from the [plate rotation plan](../reports/Plate%20rotation%20plan.md) ([03 § Rotation](03-tectonics.md#rotation-d-42); [§ Outcome](../reports/Plate%20rotation%20plan.md#outcome)).
  - Each cell keeps its place in its plate's raster; an exact integer rotation puts the raster on the world every substep; spin comes from the torque of the forces that drive drift.
  - Spin off reproduces the D-41 planet bit for bit; with spin on, 1 and 16 threads give byte-identical maps at 78 and 31.25 km; 54 tests.
  - Large plates spin ~0.1–0.25°/Myr on average; the plates with most continent turned 6–53° in the last 500 Myr. Continents lose the straight-edged slabs of translation-only motion, and wedge-shaped basins open. Layouts are busier (more merges and breakups).
  - Turned plates build their rasters at ridges at many angles, and ~2% of the floor made there later surfaces as single-cell holes; they are filled with a copy of a neighbour's crust, not as new ocean.
  - Default ocean water 2,640 m: 12 seeds at 78 km average 28.1% land (20–39%); with spin, 2,660 m gave 25.2%.
  - A 2,000 Myr run at 31.25 km ran out of memory: crust piled against a pole edge left more eclogite than crust behind when it flowed away, and delamination took the column below zero ([03 § Plateaus and roots](03-tectonics.md#plateaus-and-roots)). Eclogite is now capped at its column's crust. The bug predates D-42 (spin off no longer matches the D-41 reference for that reason). After the fix, 12 seeds at 78 km average 31.2% land (19–40%), within seed noise of 28.1%; 1 and 16 threads still give identical maps; 55 tests.
  - The CLI can draw every map in each timeline frame (`--gif-panels`, with `--gif-delay` for the frame time): 13 maps, the global numbers and a chart of the history so far.
  - Cost at 31.25 km: plates 41 → 57 s; a world ~245–265 s (was ~241 s).
  - The CLI report gained: spin by plate size, RMS speed with and without spin, turn over the last 500 Myr, rounding holes and substeps, boundary types by local motion, coast orientation. Plate maps mark each spinning plate with a turning arrow.
- **Steady land share (D-43, D-44).** Owner: "Land % is having too much variations", then: "Write a plan to steady land share the Earth way: measure how much each sea-level driver (ice, ocean-floor age, seafloor sediment) contributes to the swings, then propose how to damp them and raise continental freeboard." and "Approve the land share plan as written: build R1 and R2, run the R3 experiments, use the proposed targets, and you may download ETOPO 2022 after telling me the exact file and size." Built from the [land share stability plan](../reports/Land%20share%20stability%20plan.md) ([§ Outcome](../reports/Land%20share%20stability%20plan.md#outcome)).
  - `geosim-cli --sea-drivers` writes `sea_drivers.csv`: per step, the part of sea level due to ice, ocean-floor age and seafloor sediment, the land share at fixed levels, and land, erosion and sediment by height band. Measurement only (byte-identical planet).
  - R1: ocean floor on GDH1 (Stein & Stein 1992). R2: ice water counted at a sphere's area for its latitude. Together: sea-level SD 104 → 73 m, land SD 8.9 → 7.2 points (7 seeds at 78 km, last 500 Myr).
  - R3: low continents were planed to a platform at sea level. Of four experiments, slope-squared stream power (n = 2, K = 250,000) fixed it; a stronger crust growth feedback, no delta top and 3× river transport changed little.
  - Final, 12 seeds at 78 km, last 500 Myr: land SD 2.3 points, at most 8.0 within 50 Myr; 4.0 points per 100 m of sea level (Earth 4.8, from ETOPO 2022); mean land 580 m (Earth 656 m without ice sheets); land 31–37% at the end (mean 34.3%).
  - Same steadiness at 31.25 km (4 seeds: SD 2.85 points, at most 8.5 within 50 Myr), with more land (36–40%).
  - Not met: ~29% land, shallow sea 5–10% of the ocean (3.3%), mean land ≥ 600 m (580 m at 78 km, 483 m at 31.25 km). Land is now about the area of continental crust and more water barely moves it (2,730 m: 33.6%); Earth keeps ~30% of its continental crust under shallow sea on thinned margins, which the model lacks. Water stays at 2,640 m.
  - Side effect: weaker weathering of slowly eroding lowlands leaves the Earth-like present ~2 °C warmer (17.1 °C, 650 ppm CO2).
  - Same maps on 1, 4 and 16 threads at 78 km and on 1 and 8 at 31.25 km (seed 42); 55 tests.
- **Known gaps** (not in this scope):
  - Land is ~34%, above Earth's 29%, and shallow seas cover ~3% of the ocean (Earth 7.5%): continental margins are not thinned, so almost all continental crust stands above the sea (D-44). Before D-44, continental interiors stood only 120–250 m above sea level and land swung 13–41% by seed.
  - The Earth-like present is ~2 °C warmer since D-44 (weathering follows erosion; `earthLandWeight` predates it).
  - No ocean currents or subtropical highs: no hyper-arid desert cores, so deserts cover 6–14% of land; ice covers 13–21%.
  - Ocean floor is still older than Earth's at 78 km (77–120 Myr); at 31.25 km it is close (61–69 Myr).
  - Sediment piles on oceanic crust at open margins (D-31 keeps them oceanic on purpose).
  - The RNG streams `MARGIN_SPLIT + 1` and `BREAKUP` share the value 33 (latent collision; unchanged so far).
  - Coasts run E–W more often than N–S or diagonally (31–37% against 25% for no preference), with or without spin; pole walls are a likely cause.
