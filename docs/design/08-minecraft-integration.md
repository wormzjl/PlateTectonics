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
