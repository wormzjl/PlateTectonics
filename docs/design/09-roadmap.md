# 09 · Roadmap

> **Level 1** · Read when: you plan work or need to know what "done" means for a phase.
> Rule: get to a **walkable world early** (Phase 4), then deepen.

| Phase | Deliverable | Exit criteria |
|---|---|---|
| **0 Design** ✅ | This folder; research report | Owner reviewed decisions; open items listed |
| **1 Setup** ✅ | Git repo; Gradle multi-project (`geosim-core`, `geosim-cli`, `mod`); NeoForge 21.1.x mod skeleton that loads; Java 21; unit-test harness (the golden-image test helper moved to Phase 2, when there are real maps to pin) | `gradlew build` passes (12 unit tests); the mod loads in NeoForge 21.1.259 (verified headless via `runData`; `runClient` not yet tried); CLI writes a proto-terrain map |
| **2 Plates** | Proto-terrain; plates with per-plate rasters (translation first, then rotation); force balance; subduction, collision, rifting and ridges; crust growth; isostasy; sea level by volume; event log v0; CLI PNG maps (elevation, plates, crust age, crust type) and timeline GIF | A default 4.5 Gyr run shows continents forming, colliding and rifting (supercontinent cycle visible); deterministic across thread counts; **benchmark decides the master cell size** (15.6 vs 31 km) |
| **3 Climate and erosion** | Box models (Sun, mantle, CO₂, O₂, life milestones); climate field; FastScape erosion and deposition; glaciers v0; weathering depth; biome classification; maps for each | Rain shadows, deserts and ice caps in plausible places; river networks and deltas; eroded old ranges vs sharp young ranges |
| **4 First walkable world** | Vanilla `PlanetChunkGenerator`; `TfcPlanetChunkGenerator` + `ChunkDataGenerator`; MasterWorld save and load; preset + Customize screen with Simulate and map preview; TFC default veins (D-9 step 1) | Create a world in-game with and without TFC; terrain matches the preview; TFC seasons, crops and prospecting work; no crashes from TFC features |
| **5 Region refinement** | Tier 1.5a province tiles (local landscape evolution, disk cache) and Tier 1.5b local tiles: river channels and lakes, coastlines, glacial landforms (U-valleys, fjords, moraines), detail synthesis; height expansion option | Rivers continuous across chunks; no seams at tile borders; chunk-generation overhead within budget |
| **6 Rocks and volcanoes** | Rock columns rendered (folds, tilts, unconformities) to TFC rocks; metamorphism; volcano structures from simulated volcanics; soils | Geologic map at the surface matches the column data; ranges expose old cores and basins young cover |
| **7 Resources** | Deposit pipeline G1–G8; preservation; `DepositFeature` (D-9 step 2); indicators and gossans; coal ranks; petroleum simulation with tar seeps and oil shale; progression floor; richness multipliers | Validation targets roughly met ([06](06-resources.md#validation-targets-for-tuning-not-hard-rules)); TFC progression (bronze, iron, redstone) reachable on many random seeds |
| **8 Deep time and polish** | Supercontinent-cycle tuning; extinctions; timeline scrubber UI; dedicated-server map; teleport-wrap option (A-12); lat/long readout and in-game map; oil and gas fluids; optional addon palettes (Firmalife, Metallum) | Owner sign-off |

## Phase 2 detail (next up)

- **Repository:** `PlateTectonics/` as a git repository; `docs/` (this design) stays at the root.
- **Core packages:** `grid` (wrapping cylinder grid, neighbours), `rng` (counter-based hash), `plates` (plate, raster, transform, composition), `tectonics` (forces, boundaries, reorganization), `isostasy`, `ocean` (sea level), `events` (log), `io` (PNG and GIF writers), `bench`.
- **CLI:** `geosim-cli --seed 42 --age 4500 --cell-km 15.625 --out maps/ --frames 100`.
- **Tests:** grid wrap and neighbour indexing; RNG reproducibility; plate composition preserves area; isostasy against analytic cases; golden PNG hash for fixed small seeds.
