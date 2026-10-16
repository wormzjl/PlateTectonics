# 08 · Minecraft integration: NeoForge, TFC, chunk pipeline, UI, save data

> **Level 1** · Read when: you write code in the `mod` project, touch TFC interfaces, chunk generation, biomes, the world-creation screen or save data.
> Decisions: A-4, A-5, D-8, D-9, D-15, D-17 … D-20, A-11, A-14 · Go deeper: report § "TFC 4.2.12 accepts any generator that implements two interfaces", § "Blocked or hard-coupled", § "Lossy".

**Summary.**
- The mod ships **two chunk-generator types** over one shared core:
  - a **vanilla** generator;
  - a **TFC** generator that also implements TFC's `ChunkGeneratorExtension` and provides a `ChunkDataGenerator`. That class is loaded only when TFC is present.
- TFC then supplies seasons, crops, soils logic, prospecting and ore blocks, all fed by our data.
- The simulation runs at world creation from a preset-editor screen and is saved in the world folder.
- Chunks are filled from cached region data. Rivers, volcanoes, strata and deposits come from the simulation, not from TFC's own features.

## Projects

```
PlateTectonics/
├─ geosim-core/   pure Java 21, no Minecraft. Simulation, MasterWorld I/O, region refiner
├─ geosim-cli/    command-line runner: seeds → PNG maps, timelines, benchmarks
└─ mod/           NeoForge 21.1.x (ModDevGradle). Generators, UI, networking, TFC adapter
     └─ compat/tfc/   classes that touch TFC types; loaded only if TFC is present
```

TFC (v4.2.x for MC 1.21.1, NeoForge ≥ 21.1.234, requires Patchouli) is not on a public Maven repository. Compile against it via CurseMaven or the GitHub release jar, as `compileOnly` plus a runtime dev dependency.

## Chunk generators

| Codec id | Class | When |
|---|---|---|
| `platetectonics:planet` | `PlanetChunkGenerator extends ChunkGenerator` | Always available (vanilla blocks) |
| `platetectonics:planet_tfc` | `TfcPlanetChunkGenerator extends PlanetChunkGenerator implements ChunkGeneratorExtension` | Registered only when TFC is loaded; offered as its own world type next to TFC's |

**TFC contract** for `TfcPlanetChunkGenerator` (precedent: KubeJS-TFC `WrappedChunkGenerator`, MIT):
- `settings()` with a valid `rock_layer_settings`. Every rock block we place must be listed in it.
- `rockLayerSettings()`, `applySettings`, `chunkDataGenerator()`, `getOrCreateAquifer`, `findSpawnBiome`, `initRandomState(ChunkMap, ServerLevel)`. TFC's own `ChunkMapMixin` calls `initRandomState` for any implementer.
- Mirror TFC's copy-on-reinit fix.
- Sea level 63; base noise settings `minecraft:overworld`, y −64…320 by default.

**`PlanetChunkDataGenerator implements ChunkDataGenerator`:**
- `generate()` to **PARTIAL**: four corner climate values per chunk (sea-level temperature, rainfall 0–500, rain variance −1…1, groundwater 0–500) remapped from our present-day climate, plus a `ForestType` from vegetation density.
- **FULL**: `surfaceHeight[256]` and aquifer heights from Tier 1.5/2.
- `generateRock(x, y, z, surfaceY, cache)`: answered from the saved rock column. It must be **fast, thread-safe and deterministic forever**, because TFC calls it after reloads and for arbitrary chunks (structure placement).

## Chunk pipeline (Tier 2)

```
1. Tile lookup      Tier 1.5b local tile (memory LRU, built from the disk-cached Tier 1.5a province tile): 4-block heights, river channels, lake levels,
                    glacial forms, volcano list, layer-boundary surfaces
2. Heights          bicubic upsample + detail noise scaled by slope, rock hardness and climate;
                    river and lake carving; coastal shaping
3. Fill             per column: strata from the rock column (folded and tilted) → TFC raw rocks or vanilla stones;
                    fluids: TFC salt water in seas, fresh water in lakes and rivers
4. Caves/aquifers   vanilla or TFC carvers and aquifers (unchanged)
5. Surface          soil and regolith from the weathering model (TFC soil blocks; D-18); sand, gravel, till, peat
6. Features         DepositFeature (ores, coal seams, evaporites, tar), volcano structures,
                    then TFC or vanilla vegetation features by biome
```

## Biomes (D-17)

Classify each position from simulated terrain class (plain, plateau, rift valley, collisional range, volcanic arc, shield volcano, ice sheet, delta…) plus present climate into **TFC biome ids**. TFC has 125, each paired with a `BiomeExtension`. Add our own biomes (with `BiomeExtension` via TFC's public `BiomeBuilder`) only where TFC has no match. In vanilla mode, map to vanilla biomes with a Whittaker-style table.

## Climate alignment (D-7)

TFC assigns `OverworldClimateModel` automatically. It reads our ChunkData averages and adds z-based seasons from its client-side `SolarCalculator` (equator at z = s/2). We set `temperature_scale` s = pole-to-equator distance and place our equator at z = s/2 ([01](01-world-model.md#coordinates)), so no mixin is needed.

## Ores (D-9)

- **Step 1:** place only TFC raw rocks and let TFC's 52 default veins run. Biomes carry `#tfc:in_biome/veins`; veins follow host rock automatically.
- **Step 2:** override `#tfc:in_biome/veins*` tags with `"replace": true` (keeping gravel and geodes if wanted) and place everything from `DepositFeature` using the deposit index ([06](06-resources.md)). Firmalife chromite is supported via its palette entry when installed.

## World-creation UI

- A `WorldPreset` "PlateTectonics" plus NeoForge `RegisterPresetEditorsEvent` gives the **Customize** screen. Fields: seed, elapsed time T, scale factor S (metres per block; the planet is Earth-sized), presets (Earth-like / Random / Custom), richness and progression-floor options.
- **Simulate** runs `geosim-core` on a background thread with a progress bar. The **map preview** has layer toggles (elevation, plates, crust age, climate, rock at surface, resources, ice) and a **timeline scrubber** over the snapshots. Buttons: Accept / Reroll / Expand height (A-3).
- The result is written to `<world>/data/platetectonics/` before the first chunk loads.
- **Dedicated servers:** settings come from a config file; the simulation runs on first boot. Players open an in-game map screen; the server sends downsampled map tiles.
- **Coexisting with TFC (A-11):** our preset is an *additional* world type. We never touch TFC's worldgen, presets or `defaultWorldPreset` config; `tfc:overworld` stays the default and players choose "PlateTectonics" in the world-type list. Phase 4 verifies that TFC's `WorldPresetsMixin` leaves our preset selectable.

## As built (Phase 4, first slice)

**World types.** `data/platetectonics/worldgen/world_preset/`, listed in `#minecraft:normal`:

| Preset | Generator | Notes |
|---|---|---|
| `planet` | `platetectonics:planet` + biome source `platetectonics:planet` | 31.25 km master grid |
| `planet_quick` | same, `grid_width` 512 | 78 km, a world in ~15 s |
| `planet_tfc` | `platetectonics:planet_tfc` + `platetectonics:planet_tfc` | Loaded only with TFC (`neoforge:conditions` mod_loaded; the tag entry is `required: false`). Carries TFC's own `tfc_settings` (rock layers); `temperature_scale` is reset from S at load (333,333 at 30 m/block, D-7) |

**Planet settings** (`planet` object; all optional): `meters_per_block` (30), `grid_width` (1,280), `age_myr` (4,500), `history` (random / earth), `ocean_depth_m` (2,640; D-41, D-42). The seed is the world seed.

**Load or simulate** (`PlanetLoader`, on `ServerAboutToStartEvent`, before any level exists):
- If `<world>/data/platetectonics/master.ptw` exists, it is read. A settings mismatch logs a warning and keeps the saved planet.
- Otherwise the planet is simulated on all cores but one (~4 min at 31.25 km since D-41), saved, and then attached to the generator and its biome source.
- The first spawn is the mild, dry-land cell nearest the origin. Vanilla uses `LevelEvent.CreateSpawnPosition`; TFC uses `findSpawnBiome`.

**Terrain** (`geosim-core` `TerrainSampler`; the stand-in for Tier 1.5 until Phase 5, D-34):
- Elevation is a Catmull-Rom bicubic of the master cells, clamped to the four surrounding cells.
- Fractal detail is added, with amplitude 12 m + 0.18 × local relief on land (capped at 900 m), calmer near the coast.
- Climate is bilinear; temperature follows the detail with 6.5 °C/km.
- Ice sheets are packed ice of their simulated thickness, with snow on top.

**Columns (vanilla):**
- Bedrock floor, deepslate below y = 0, stone above.
- The cover comes from climate class, slope and water depth: grass and dirt, podzol and coarse dirt patches, desert sand over sandstone, beach sand, gravel and clay on the sea floor, bare stone on cliffs, snow above the snow line.
- Water fills to sea level, with ice on frozen seas.
- Biomes come from the climate class: oceans split by water temperature, beaches, and peaks above 2,600 m. Vanilla vegetation features place the plants.
- Not yet: caves and carvers, rivers and lakes, strata. Phase 5 and Phase 6 add these.

**TFC adapter** (`compat/tfc`, compiled against TFC 4.2.12 from Modrinth's Maven; registered only if TFC is loaded):
- `TfcPlanetChunkGenerator` implements `ChunkGeneratorExtension`:
  - `initRandomState` registers itself on the `RandomState`, as TFC's generator does, so climate-dependent structure placement finds it.
  - `ChunkData` goes to PARTIAL in `createStructures` and in fill, then to FULL with the 16 × 16 surface heights.
  - The aquifer is disabled (no caves yet).
- `PlanetChunkDataGenerator`:
  - Corner climate: rainfall = precipitation × 0.2 (0–500); temperature reduced to sea level (TFC applies its own lapse rate); rain variance 0; groundwater = rainfall.
  - Forest type from the climate class.
  - `generateRock` is a deterministic placeholder until Phase 6: sedimentary beds to the cell's sediment depth; basalt over gabbro on oceanic crust; continental suites chosen by exhumation, in irregular patches of about 300 blocks.
- The surface uses TFC soil orders (aridisol, oxisol, podzol, mollisol, alfisol, entisol on slopes), each rock's own sand, sandstone and gravel, salt water and sea ice.
- `TfcPlanetBiomeSource` picks TFC landform biomes (deep ocean, ocean, shore, lowlands, plains, rolling hills, hills, highlands, plateau, mountains, old mountains, ice sheet) from depth, height, relief and ice. TFC takes climate from ChunkData, not from the biome.

**Tests:**
- `gradlew :mod:runGameTestServer` (add `-PwithTfc` for TFC 4.2.12 and Patchouli) decodes the presets, simulates a small planet, fills chunks and checks:
  - heights match `getBaseHeight`, and seas are filled;
  - with TFC: ChunkData reaches FULL, rock at depth matches `generateRock`, the sea is salt water, and biomes are TFC biomes.
- The game-test server needs no EULA. A dedicated-server or client run is still to do.
- `geosim-cli --from-master FILE --block-view X,Z,PIXELS,STEP` renders the terrain as chunk generation builds it, without starting Minecraft.

## Gaps

What is missing, blocked or lossy with TFC (details and sources in the report):

| Kind | Item | Handling |
|---|---|---|
| Missing content | No block for Pb, Cu-sulfides, Mo, W, Sb, Cr (except via Firmalife), PGE, U, Mn, Al, Ti, Li, REE, P, fluorite, barite, **petroleum**, anthracite | Stand-ins or omit (D-12); own blocks for oil, tar, oil shale and anthracite (D-13, D-14) |
| Missing rocks | No ultramafic, kimberlite, anorthosite, amphibolite, ore-bearing sandstone, evaporite rock | **Sandstone: our own TFC-compatible rock** (A-14); the rest collapse onto the 21 rocks ([05](05-lithology.md)) |
| Blocked | TFC's `RegionGenerator` (final, hash velocities), river carving, centred volcano features, biome-shaped height noises | Not used: our generator owns terrain, rivers and volcanoes (D-20) |
| Hard-coded | Sea level 63 (~168 references) | Kept (D-19) |
| Crash risk | `ErosionFeature`, `SpringFeature` and `CaveSpikesFeature` hard-cast the generator to `ChunkGeneratorExtension` | Satisfied by `TfcPlanetChunkGenerator`; keep them out of vanilla-mode biomes |
| Lossy | Rainfall and variance ranges; ForestType enum; grade quantized to 3 levels; TFC lapse rate stops at y ≈ 173 (matters with height expansion); small nuggets exist only for 12 graded ores | Remap; add our own surface indicators; optionally subclass the climate model |
| Licensing | TFC is EUPL-1.2 | Depend on its interfaces only; copy no TFC source (MIT addons already do this; not legal advice) |

## Performance notes

- TFC's base fill calls `generateRock` once per solid block (with `ChunkRockDataCache`). Our generator fills whole columns directly, but TFC features still query point-wise, so column lookups must be O(1) (precomputed per-chunk layer tables).
- Region tiles are computed off-thread and cached (LRU, size-bounded). Chunk generation never runs the master simulation.
