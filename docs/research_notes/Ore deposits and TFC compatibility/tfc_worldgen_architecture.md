# TerraFirmaCraft (TFC) 1.21.1 NeoForge world generation architecture, and what a replacement terrain mod must provide

Research basis: a sparse clone of `TerraFirmaCraft/TerraFirmaCraft`, branch `1.21.x`, at commit `c3b4d9536b82e5a9746dd793c586720a74108057` (2026-10-14, the "4.2.12" release commit). All source links below point at branch `1.21.x`. Short names used below:
- `B/` = `https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/`
- All class paths are relative to package `net.dries007.tfc`.

Note on method: the web-search budget for this session ran out before CurseForge pages, the TFC Discord, "Tectonic" and "TFC Caves" could be checked. Version data comes from the GitHub releases API and the Modrinth API. Addon data comes from GitHub code search and direct source reads.

---

## 1. Version status, branch, and dependencies (1.21.1 NeoForge)

### Takeaway
The TFC line for Minecraft 1.21.1 / NeoForge is **v4.x**. It has been a full release since **4.1.0 (2026-04-04)**. The current version is **4.2.12 (2026-10-14)**. The code lives on branch **`1.21.x`**, which is the repository's default branch. The 4.2.0 release (2026-06-21) rebuilt the large-scale terrain to "mimic plate tectonics". TFC is not on a public Maven repository: addons pull it through CurseMaven or through the GitHub release download URLs.

### Cited Findings
- The repository default branch is `1.21.x`. The license is `EUPL-1.2`. Branches are `1.12.x, 1.16.x, 1.17.x, 1.18.x, 1.20.x, 1.21.x`. — [GitHub API: repo + branches](https://github.com/TerraFirmaCraft/TerraFirmaCraft)
- Release history for 1.21.1:
  - `v4.0.0-beta` was the first 1.21.1 build (2025-09-13). Betas continued up to `v4.0.19-beta` (2026-02-16).
  - `v4.1.0` came out 2026-04-04, followed by `4.1.1`–`4.1.4`.
  - `v4.2.0` came out 2026-06-21, followed by `4.2.1`–`4.2.12`. `v4.2.12` was published 2026-10-14.
  - The 1.20.1 line (`v3.2.x`) is still maintained: `v3.2.27` was published 2026-10-10.
  - Source: [TFC GitHub releases](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases)
- Modrinth labels every `4.0.x-beta` file as `version_type: beta`, and `4.1.0` onward as `release`. The latest 1.21.1 file there is `4.2.12` (2026-10-14). — [Modrinth API, terrafirmacraft versions](https://api.modrinth.com/v2/project/terrafirmacraft/version)
- Build toolchain:
  - `minecraftVersion = "1.21.1"`, `neoForgeVersion = "21.1.234"`, Java 21 (`modJavaVersion = "21"`).
  - ModDevGradle plugin `net.neoforged.moddev` 2.0.107.
  - Compile/runtime integrations: EMI (compileOnly api), JEI 19.25.0.321, Patchouli `1.21.1-92-NEOFORGE` (implementation), Jade, and The One Probe (compileOnly).
  - Source: [build.gradle.kts](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/build.gradle.kts)
- `neoforge.mods.toml` declares these dependencies:
  - Required: **patchouli**, **neoforge** (`[21.1.234,)`), and **minecraft** (exactly `[1.21.1]`).
  - Optional: **jei**.
  - It also uses `enumExtensions = "META-INF/enumextensions.json"` and a `tfc.mixins.json` mixin config.
  - Source: [neoforge.mods.toml template](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/templates/META-INF/neoforge.mods.toml)
- 4.2.0 changelog:
  - "Reworked large-scale terrain generation to mimic plate tectonics."
  - "Each continent area will now be bordered by convergent or divergent boundaries" (subduction zones, rift valleys, collisional ranges, ocean ridges).
  - Stratovolcanoes are limited to subduction zones. Trenches extend down to y = 0.
  - Volcanic island arcs and barrier islands were added. Large rivers now carve wide valleys.
  - Source: [v4.2.0 release notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.0)
- 4.2.8 changelog: "Add a rock settings registry, which allows datapacks to override the rock settings for a world without needing to rewrite the entire world preset file." — [TFC releases (v4.2.8)](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.8)
- 4.0.17-beta broke compatibility with several addons, including "anything else that uses the `tfc:biome` placement type". — [v4.0.17-beta notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.17-beta)
- The v4.0.0-beta notes say AlcatrazEscapee (long-time lead) "has, at long last, decided to hang up the keyboard". The README thanks her "from the Minecraft 1.12 era in 2018, all the way until 1.20 in 2025". — [v4.0.0-beta notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta); [README](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/README.md)
- The repo's AGENTS.md says: "When outside of alpha and beta versions, TFC has a policy of backwards compatibility. Do not change method signatures of methods that could reasonably be considered API." It also asks agents not to file issues and asks that large features be discussed on Discord first. — [AGENTS.md](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/AGENTS.md)
- How addons get the TFC artifact:
  - KubeJS-TFC (1.21.1) uses `curse.maven:terrafirmacraft-302973:${tfc_file}` with an access-transformed configuration (`accessTfcImplementation`). — [KubeJS-TFC build.gradle](https://github.com/Notenoughmail/KubeJS-TFC/blob/1.21.1/build.gradle)
  - TFC-Real-World (1.21.x) uses `compileOnly("net.dries007.tfc:TerraFirmaCraft-NeoForge-1.21.1:<ver>@jar")` from a repository pointed at `https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/download`. — [TFC-Real-World build.gradle.kts](https://github.com/yazloysasha/TFC-Real-World/blob/1.21.x/build.gradle.kts)
- The official addon/pack documentation has a 1.21.x section with pages for World Preset, Biomes, Features (veins, trees), Placement Modifiers, Carvers, and Tags. — [TFC Documentation](https://terrafirmacraft.github.io/Documentation/); [Documentation repo 1.21.x](https://github.com/TerraFirmaCraft/Documentation/tree/main/1.21.x)

### Inferences
- Target **TFC ≥ 4.2.x** on NeoForge 21.1.x. The 4.2.0 terrain rework moved internals around (new region tasks, stratovolcanoes, tuff). Any mixin-based integration should pin a TFC version range and be retested on each 4.2.x patch.
- The backwards-compatibility policy covers methods "that could reasonably be considered API". Internal classes are not covered: region tasks, `ChunkHeightFiller`, `ChunkNoiseFiller`. Interfaces such as `ChunkGeneratorExtension`, `ChunkDataGenerator`, `ClimateModel` and `SelectClimateModelEvent` are the safer coupling points.

### Gaps
- I did not verify CurseForge file IDs or download counts, or Discord announcements about the 1.21.1 roadmap (no web search budget left).
- The exact 4.1.0 "first stable" announcement text was not read. Stable status is inferred from the Modrinth `release` label and the dropped `-beta` suffix.

---

## 2. Chunk generator, terrain height, the region ("plate") generator, rivers, world preset and Settings

### Takeaway
`TFCChunkGenerator` (codec type `tfc:overworld`) is a fully custom `ChunkGenerator` that implements `ChunkGeneratorExtension`.

Terrain is built in three steps:
1. A stateless, per-Voronoi-cell **RegionGenerator** runs a fixed list of tasks over a 128-block grid. The tasks cover continents, plate-boundary divergence, hotspots, mountains, climate, rocks, rivers and biomes.
2. The per-point biome IDs are zoomed into a quart-resolution biome layer.
3. Height is computed by **blending per-biome 2D height noises** (each `BiomeExtension` carries its own heightmap noise). River, shore and volcano modifiers are then applied, followed by 3D cave noise and TFC's own aquifer.

The "plates" are not simulated over time. Each continent cell gets a pseudo-random velocity from a hash, and boundary type comes from the relative velocity of the neighbouring cell.

### Cited Findings
**Generator class and codec**
- `world/TFCChunkGenerator.java`: `public class TFCChunkGenerator extends ChunkGenerator implements ChunkGeneratorExtension`.
  - Codec fields: `biome_source` (must be a `BiomeSourceExtension`, else the error "Must be a BiomeSourceExtension"), `settings` (vanilla `NoiseGeneratorSettings`) and `tfc_settings` (TFC `Settings`).
  - `SEA_LEVEL_Y = 63; // Matches vanilla`.
  - Source: [TFCChunkGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/TFCChunkGenerator.java)
- `world/ChunkGeneratorExtension.java` is the "Interface for TerraFirmaCraft compatible chunk generators". Its members:
  - `Settings settings()`
  - `default RockLayerSettings rockLayerSettings()`
  - `void applySettings(UnaryOperator<Settings>)` (used by the preset editor screen)
  - `ChunkDataGenerator chunkDataGenerator()`
  - `Aquifer getOrCreateAquifer(ChunkAccess)`
  - `default BlockPos findSpawnBiome(RandomSource)`, which delegates to `BiomeSourceExtension`
  - `void initRandomState(ChunkMap, ServerLevel)`
  - `static getFromStructureState(ChunkGeneratorStructureState)`, which reads the extension cached on `RandomState`
  - Source: [ChunkGeneratorExtension.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/ChunkGeneratorExtension.java)
- `mixin/ChunkMapMixin.java` injects at the end of the `ChunkMap` constructor: `if (generator instanceof ChunkGeneratorExtension ex) ex.initRandomState(...)`. This is TFC's hook for seed-dependent initialization, and it fires for **any** generator implementing the interface. `mixin/RandomStateMixin.java` adds the `tfc$get/setChunkGeneratorExtension` field. — [ChunkMapMixin.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/mixin/ChunkMapMixin.java); [RandomStateMixin.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/mixin/RandomStateMixin.java)

**Initialization (`initRandomState`)**
- Steps, in order:
  1. `Seed.of(level.getSeed())`
  2. `new RegionGenerator(settings, seed)`
  3. `TFCLayers.createRegionBiomeLayer(regionGenerator, seed)`, wrapped in `ConcurrentArea<BiomeExtension>`
  4. Create the `NoiseSampler`
  5. `chunkDataGenerator = regionGenerator.chunkDataGenerator()`
  6. Create `SurfaceManager(seed)` and the tide noise
  7. `customBiomeSource.initRandomState(regionGenerator, biomeLayer)`
  8. Register itself on the RandomState
- If the generator was already initialized, it copies itself (a fix for Create-style duplicate worlds, issue #2591).
- Source: [TFCChunkGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/TFCChunkGenerator.java)

**Generation pipeline (same file)**
- `createBiomes`: `chunkDataGenerator.generate(chunk)` brings chunk data to PARTIAL. Biomes are then filled in **2D** (`customBiomeSource.getBiome(quartX, quartZ)`; Y is ignored).
- `createStructures`: also calls `chunkDataGenerator.generate(chunk)` first ("populate chunk data before references to enable placements").
- `fillFromNoise`:
  1. Biome weights come from `ChunkBiomeSampler.sampleBiomes(...BiomeExtension::biomeBlendType)`.
  2. A `ChunkNoiseFiller` is built from: per-biome `BiomeNoiseSampler`s, river samplers per `RiverBlendType`, shore samplers per `ShoreBlendType`, centered-feature samplers per `CenteredFeatureBlendType` (cinder cones, tuff rings, tuyas, atolls, stratovolcanoes), the 3D `NoiseSampler`, `ChunkBaseBlockSource` (rock blocks from RockData), and the `Beardifier`.
  3. It then runs `sampleAquiferSurfaceHeight` → `chunkData.generateFull(surfaceHeight, aquiferSurfaceHeights)` → `fillFromNoise()` → `chunkData.modifyBaseGroundwater(surfaceHeight)` → `surfaceManager.buildSurface(...)`.
- `buildSurface` only places bedrock: flat, or a random 0–5 layer. Soil and sand are placed inside `fillFromNoise` by the `SurfaceManager`.
- `applyCarvers` runs only the `AIR` step ("Skip water carving … since we use aquifers").
- `applyBiomeDecoration` is a custom loop that calls `TFCBiomes.getExtensionOrThrow(level, biome)` for every adjacent biome.
- `getBaseHeight` uses `ChunkHeightFiller.sampleHeight`. `getBaseColumn` returns an empty `NoiseColumn`.
- Source: [TFCChunkGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/TFCChunkGenerator.java)

**Height sources**
- `ChunkHeightFiller.sampleColumnHeightAndBiome` blends the per-biome heights by sampled biome weights. River-edge sampling uses `RegionPartition.Point`. `ChunkNoiseFiller` adds a slope map, the 3D density with "surface integrity depth", noise and noodle caves, and the aquifer. — [ChunkHeightFiller.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/ChunkHeightFiller.java); [ChunkNoiseFiller.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/ChunkNoiseFiller.java)
- Per-biome height lives in `BiomeBuilder.heightmap(LongFunction<Noise2D>)`. Examples:
  - `OCEAN`: `BiomeNoise.ocean(seed, -26, -12)`
  - `DEEP_OCEAN_TRENCH`: `BiomeNoise.oceanTrench(seed, -60, -46)`
  - `GLACIALLY_CARVED_VOLCANIC_MOUNTAINS`: `BiomeNoise.glacialCirques(seed).addConstant(39)`
  - Source: [TFCBiomes.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/TFCBiomes.java)

**RegionGenerator**
- `world/region/RegionGenerator.java` is `public final class RegionGenerator`, documented as a "single-instance, threadsafe" generator. It caches `Region`s per Voronoi cell (`FastConcurrentCache<Region>(256)`) and river partitions (`FastConcurrentCache<RegionPartition>(256)`). It owns `RegionChunkDataGenerator`. Its task list is a hardcoded enum, run in this order:
  1. `INIT`
  2. `ANNOTATE_DISTANCE_TO_CELL_EDGE`
  3. `ANNOTATE_BOUNDARY_TYPES`
  4. `ADD_CONTINENTS`
  5. `FLOOD_FILL_SMALL_OCEANS`
  6. `ADD_HOTSPOTS`
  7. `ANNOTATE_DISTANCE_TO_OCEAN`
  8. `ANNOTATE_DISTANCE_TO_DEEP_OCEAN`
  9. `ANNOTATE_BASE_LAND_HEIGHT`
  10. `ANNOTATE_DISTANCE_TO_WEST_COAST`
  11. `ADD_MOUNTAINS`
  12. `ADD_ISLANDS`
  13. `ANNOTATE_BIOME_ALTITUDE`
  14. `ANNOTATE_CLIMATE`
  15. `CHOOSE_ROCKS`
  16. `ANNOTATE_KARST_SURFACE`
  17. `ADD_RIVERS_AND_LAKES`
  18. `CHOOSE_BIOMES`
  - Source: [RegionGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java)
- Units (`world/region/Units.java`):
  - `GRID_BITS = 7`, so one grid point = **128 blocks**.
  - `PARTITION_WIDTH_IN_GRID = 3`.
  - `CELL_WIDTH_IN_PARTITION = 32`, so a cell = 96 grid = **12,288 blocks**.
  - `REGION_RADIUS_IN_GRID = CELL_WIDTH_IN_GRID + 5` (= 101).
  - Source: [Units.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/Units.java)
- "Tectonics" in `RegionGenerator.getDivergence(point)`: each continent cell's velocity is `getVX/getVY = Helpers.hashDouble(cell.noise(), const)`. Divergence compares the cell's velocity with the adjacent cell's, along x and y. It returns "converging (negative) or diverging (positive)". — [RegionGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java)
- Continent noise:
  - `continentNoise = cellNoise.then(c -> 1 - c.f1()/(0.37f + c.f2())).lazyProduct(OpenSimplex2D.spread(0.24).scaled(continentalness*10 - 2.5, 8.7).octaves(4))`.
  - `AddContinentsAndSetOceanDepths` marks land when `(continentNoise + riftSeas) * finiteContinentFactor > 4.4`.
  - Otherwise it sets `oceanDepth`: 2 = shelf (> 3.3), 3 = ridge (divergent boundary or cell edge), 5 = trench (convergent, continent > 3), 4 = abyssal plain. Reef (1) is set later.
  - Source: [AddContinentsAndSetOceanDepths.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/AddContinentsAndSetOceanDepths.java)
- `AddHotspots` places hotspot chains from `hotSpotIntensityNoise > 0.65`, away from plate-region edges. It records `hotSpotAge` and floods outward while intensity > 0.15. — [AddHotspots.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/AddHotspots.java)
- `Region.Point` fields:
  - Flags: land, island, river, lake, mountain, coastalMountain, volcanic, barrierIsland.
  - Bytes: `distanceToOcean`, `distanceToDeepOcean`, `distanceToEdge`, `distanceToWestCoast`, `distanceToLand`, `baseLandHeight`, `biomeAltitude`, `oceanDepth`.
  - Floats: `rainfall`, `rainfallVariance`, `temperature`, `divergence`.
  - Other: `int biome`, `int rock`, `boolean isSurfaceRockKarst`, `byte hotSpotAge`.
  - Source: [Region.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/Region.java)

**Rivers**
- `AddRiversAndLakes`: every **shore** point is a candidate river source.
  - It picks the best of 8 directions looking 4 grid inland, maximizing `distanceToOcean`, plus ±0.1 rad jitter.
  - It builds `River.Builder(rng, x, z, angle, RIVER_LENGTH=2.7f, RIVER_DEPTH=17, RIVER_FEATHER=0.8f)` through `RegionRiverGenerator`, and stores the result as `RiverEdge`s on the Region.
  - So rivers grow as fractal trees from the coast inward. They are not derived from simulated runoff or elevation.
  - Source: [AddRiversAndLakes.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/AddRiversAndLakes.java)
- `RiverEdge` details:
  - Width is 8..24 (`MIN_WIDTH`, `MAX_WIDTH`), `MIN_VALLEY_WIDTH = 20`.
  - Each edge has a `MidpointFractal` path.
  - It has a **public constructor** `RiverEdge(River.Edge edge, RandomSource random)`, where `River.Edge(Vertex source, Vertex drain)` and `Vertex(x, y, angle, length, distance)` are public records in grid units.
  - Source: [RiverEdge.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RiverEdge.java); [River.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/river/River.java)
- River lookup:
  - `RegionPartition` indexes the river edges that overlap each 3×3-grid partition (`record Point(List<RiverEdge> rivers)`).
  - `BiomeSourceExtension.getBiomeExtension` returns `TFCBiomes.RIVER` when the base biome `hasRivers()` and a river fractal intersects within 0.08 grid.
  - Terrain carving uses `RiverNoiseSampler` per `RiverBlendType` (e.g. `WIDE`, `CANYON`, `CAVE`).
  - Source: [RegionPartition.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionPartition.java); [BiomeSourceExtension.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/BiomeSourceExtension.java)
- `RegionChunkDataGenerator` raises `baseGroundwater` near rivers at least 12 wide, within about 40 blocks: "creating a bit of a humid area in the immediate vicinity of the river". — [RegionChunkDataGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RegionChunkDataGenerator.java)
- TFC-Real-World describes the river behaviour from an addon's point of view: rivers are given "as straight edges; TFC bends and carves them itself". It adds: "Rivers flow at sea level, and where an edge runs into the sea or a lake nothing is carved." — [TFC-Real-World README](https://github.com/yazloysasha/TFC-Real-World/blob/1.21.x/README.md)

**World preset and Settings**
- `world/settings/Settings.java` is a record with these JSON fields:
  - `flat_bedrock`, `spawn_distance`, `spawn_center_x`, `spawn_center_z`
  - `temperature_scale`, `temperature_constant` (optional, default 0)
  - `rainfall_scale`, `rainfall_constant` (optional, default 0)
  - `rock_layer_settings`
  - `continentalness`, `grass_density`, `finite_continents`
  - Javadoc: temperature and rainfall scale are the "Distance between temperature 'poles'. Default 20km".
  - Source: [Settings.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/settings/Settings.java)
- The default preset `data/tfc/worldgen/world_preset/overworld.json` defines:
  - Overworld generator: `"type": "tfc:overworld"`, biome_source `{"type": "tfc:overworld"}`, `"settings": "minecraft:overworld"`.
  - `tfc_settings`: `continentalness 0.5`, `finite_continents false`, `flat_bedrock false`, `grass_density 0.5`, `rainfall_scale 20000`, `temperature_scale 20000`, `spawn_distance 4000`, `spawn_center_x/z 0`.
  - The Nether and End use vanilla generators.
  - Source: [overworld.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/worldgen/world_preset/overworld.json)
- The official docs describe the same fields. They note `continentalness` is in [-1, 1], and `finite_continents`: "If the world should generate only a few continents, leaving a vast ocean beyond." — [Docs: World Preset](https://terrafirmacraft.github.io/Documentation/1.21.x/worldgen/world-preset/)
- `RegionGenerator.continentFactor` (finite continents) limits land to one pole-to-pole area. It uses `rainfallScale` on |x| and `temperatureScale` on z, fading from 1 to 0 between 1.0× and 1.2× scale. — [RegionGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java)
- `mixin/WorldPresetsMixin.java` replaces vanilla `WorldPresets.createNormalWorldDimensions` with the preset named by TFC's common config `defaultWorldPreset` (default `"tfc:overworld"`). — [WorldPresetsMixin.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/mixin/WorldPresetsMixin.java); [CommonConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/config/CommonConfig.java)
- The world-creation editor (`client/screen/CreateTFCWorldScreen`) is registered through NeoForge `RegisterPresetEditorsEvent` and edits `Settings` through `ChunkGeneratorExtension.applySettings`. — [ChunkGeneratorExtension.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/ChunkGeneratorExtension.java)
- Differences in 1.20.1 (TFC 3.x, branch `1.20.x`):
  - `Settings` lacked `temperature_constant`, `rainfall_constant`, `grass_density` and `finite_continents`.
  - The chunkdata package had `ChunkDataProvider` and `ChunkDataCapability`.
  - Region tasks were fewer: no boundary types, hotspots, west coast or karst; there was `ShrinkToCell`.
  - Source: [1.20.x chunkdata dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.20.x/src/main/java/net/dries007/tfc/world/chunkdata); [1.20.x region dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.20.x/src/main/java/net/dries007/tfc/world/region)

### Inferences
- TFC's "plate tectonics" is a stateless pattern generator: cell velocities come from a hash, and boundary type from relative velocity. It has no plate history, no crustal age and no stratigraphic record. It cannot be "fed" a simulated plate history without replacing most region tasks.
- Heights are owned by biomes. There is no single "height function" to swap out. Replacing the heightmap inside `TFCChunkGenerator` means either mixins into `ChunkHeightFiller` / `BiomeNoise` (the TFC-Real-World approach) or not using `TFCChunkGenerator` at all.
- Rivers are coupled to the region grid (partitions of 3×128 blocks) and to biome flags. External rivers can still be injected as `RiverEdge`s, since the constructor and records are public, but only into the partition cache, which is private and reachable only by mixin.

### Gaps
- I did not read `RegionRiverGenerator` / `River.Builder` in full, so branching rules and the exact lake creation logic are not documented here.
- I did not trace how `AnnotateBaseLandHeight` and `AnnotateBiomeAltitude` turn into actual block heights (via `ChooseBiomes` altitude classes).

---

## 3. ChunkData, ChunkDataGenerator, and the game systems that depend on them

### Takeaway
1.21 has no `ChunkDataProvider`; that was the 1.20 name. Instead, a NeoForge **data attachment** `tfc:chunk_data` holds a `ChunkData` per chunk. It stores:
- climate as **four corner values per chunk** (bilinear `LerpFloatLayer`): average sea-level temperature, average rainfall, rain variance, base groundwater;
- a `ForestType`;
- the surface-height and aquifer-height arrays;
- a `RockData` handle.

**Rock types are not stored.** They are recomputed on demand by calling the world's `ChunkDataGenerator.generateRock(x, y, z, surfaceY, cache)`, during generation and at runtime. Any generator that is TFC-compatible must therefore supply a working, deterministic, thread-safe `ChunkDataGenerator` for the life of the world.

### Cited Findings
- `common/TFCAttachments.java` registers `CHUNK_DATA` as an `AttachmentType`. On creation it does `new ChunkData(generator, chunk.getPos())`. The generator is `ex.chunkDataGenerator()` when the level's generator is a `ChunkGeneratorExtension`, else `null`. It serializes with `ChunkData.serializeNBT/deserializeNBT`. — [TFCAttachments.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/TFCAttachments.java)
- `world/chunkdata/ChunkData.java`:
  - Fields: `status` (EMPTY, CLIENT, PARTIAL, FULL, INVALID), `rockData`, `rainfallLayer`, `rainVarianceLayer`, `baseGroundwaterLayer`, `temperatureLayer` (all `LerpFloatLayer`), `aquiferSurfaceHeight int[]`, `forestType`, `lastRandomTick`, `nextSnowPosition`.
  - Unknown defaults: rainfall 250, temperature 10.
  - `generatePartial(rainfall, rainVariance, baseGroundwater, temperature, forestType)` moves EMPTY to PARTIAL. `generateFull(int[] surfaceHeight, int[] aquiferSurfaceHeight)` moves PARTIAL to FULL. Both methods are public.
  - Saved NBT: status, surfaceHeight, aquiferSurfaceHeight, the four climate layers, forestType, lastRandomTick, nextSnowPosition. No rock data is saved.
  - `getAverageGroundwater = min(baseGroundwater + rainfall, 500)`. `modifyBaseGroundwater` fades groundwater between y = 73 and y = 88 (sea level +10 to +25).
  - The client receives a `ChunkWatchPacket` with the four layers and the forest type.
  - Source: [ChunkData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ChunkData.java)
- `world/chunkdata/RockData.java`:
  - `getRock(x, y, z)` calls `generator.generateRock(x, y, z, surfaceHeight[index], cache)`.
  - If surface heights are missing, it logs "Queried rock data … before surface height cache was populated" and falls back to surfaceY = 64.
  - `getSurfaceRock(x, z)` calls `generator.generateSurfaceRock`.
  - Source: [RockData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RockData.java)
- `world/chunkdata/ChunkDataGenerator.java` is a public interface with these members:
  - `ChunkData generate(ChunkData)` (up to PARTIAL)
  - `default generate(ChunkAccess)`
  - `default createAndGenerate(ChunkPos)` ("should only be used if the underlying chunk cannot be accessed")
  - `default generateSurfaceRock(x, z)`, which equals `generateRock(x, 0, z, 0, null)`
  - `RockSettings generateRock(int x, int y, int z, int surfaceY, @Nullable ChunkRockDataCache cache)`
  - `default displayDebugInfo(...)`
  - Source: [ChunkDataGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ChunkDataGenerator.java)
- `RegionChunkDataGenerator` (TFC's implementation):
  - It samples the 2×2 surrounding region grid points (128-block spacing) for rainfall, variance and temperature, then rescales them to the chunk's corners.
  - It adds river groundwater.
  - It samples `ForestType` per chunk from a separate forest layer.
  - Its rock algorithm is described in section 4.
  - Source: [RegionChunkDataGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RegionChunkDataGenerator.java)
- The forest layer does **not** depend on climate:
  - `TFCLayers.createOverworldForestLayer` starts from `ForestInitLayer` (OpenSimplex noise: below 0 gives `GRASSLAND`, otherwise `SECONDARY_DIVERSE`), then applies randomize, zoom and edge layers.
  - `ForestType` is a fixed enum (GRASSLAND, CLEARING, SHRUBLAND, SPARSE, SAVANNA_*, PRIMARY_*, SECONDARY_*, …), each with tree/bush count parameters.
  - Source: [TFCLayers.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/layer/TFCLayers.java); [ForestInitLayer.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/layer/ForestInitLayer.java); [ForestType.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ForestType.java)
- Runtime (non-worldgen) consumers of `ChunkData`, found by grepping `ChunkData.get` / `getRockData`:
  - `util/climate/OverworldClimateModel` (all temperature, rain and groundwater queries)
  - `common/blocks/soil/FarmlandBlock` and `common/blockentities/FarmlandBlockEntity` (crop hydration from rainfall, `MAX_RAINFALL_CONTRIBUTION`)
  - `common/blocks/plant/PlantRegrowth` (regrows loose rocks using `getRockData().getRock(pos).loose()`)
  - `common/entities/Faunas` (spawn conditions) and `common/entities/prey/TFCFox`, `TFCFrog`, `TFCRabbit`
  - `util/tracker/WeatherHelpers` and `WorldTracker` (rain/snow)
  - `network/ChunkWatchPacket`
  - `client/ClientForgeEventHandler` (F3 debug calls `ex.chunkDataGenerator().displayDebugInfo`)
  - Source: [TFC 1.21.x source tree](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/java/net/dries007/tfc)
- `world/structure/ClimateStructurePlacement` gets the generator through `ChunkGeneratorExtension.getFromStructureState(state)` and calls `ex.chunkDataGenerator().createAndGenerate(chunkPos)`, so structure placement asks for climate of arbitrary chunks. — [ClimateStructurePlacement.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/structure/ClimateStructurePlacement.java)
- Client colours: `client/TFCColors`, `ClimateRenderCache` and `LeavesBlockModel` sample climate at `SEA_LEVEL_Y` for grass, foliage and sky colours. The docs say biome `fog_color`/`sky_color`/`water_color` are ignored and "queried based off a color map … based off of the actual rainfall and temperature". — [TFCColors.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/client/TFCColors.java); [Docs: Biomes](https://terrafirmacraft.github.io/Documentation/1.21.x/worldgen/biomes/)
- Grepping `common/component` for `Climate` or `ChunkData` found no references (the food-decay code lives there). Collapses and prospecting run through `CollapseEvent` / `ProspectedEvent`, and neither appears in the `ChunkData` consumer list. — [TFC 1.21.x source tree](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/java/net/dries007/tfc)

### Inferences
- The minimum contract for full compatibility:
  - (1) `ChunkData` reaches PARTIAL before the structure and biome stages, with climate corners and a forest type.
  - (2) `ChunkData` reaches FULL during fill-noise, with `surfaceHeight[256]` and `aquiferSurfaceHeight`. KubeJS-TFC uses 16 values, a 4×4 grid.
  - (3) `generateRock` answers any (x, y, z) consistently, both during generation and after world reload.
  - For a simulation-based mod, (3) means the simulated stratigraphy must be **persisted with the world**, or regenerated deterministically from the seed, before any chunk loads. TFC calls `generateRock` at runtime (loose-rock regrowth, debug overlay, cave columns, etc.).
- Climate resolution is limited to bilinear interpolation between chunk corners (16 blocks). That is more than enough for a planet-scale simulation, but sub-chunk climate detail cannot be expressed.
- Food decay, collapses and prospecting appear not to depend on `ChunkData` climate. This is inferred from grep only and should be confirmed before relying on it.

### Gaps
- I did not read `Faunas`'s exact spawn rules or `WeatherHelpers`' precipitation logic. Only the fact that they use ChunkData / the climate model is established.
- I did not check whether any TFC system reads `ChunkData.getForestType()` at runtime outside world generation.

---

## 4. Rock system: RockSettings, rock layer settings, rock types and blocks

### Takeaway
Rocks are data-driven `RockSettings`:
- `raw`, `hardened`, `gravel`, `cobble`, `sand`, `sandstone`;
- optional `spike`, `loose`, `mossy_loose`, `karst`, `mafic`.

They are registered in the datapack registry `tfc:worldgen/rock_settings`, which was added in 4.2.8. Rock *layers* are a probabilistic chain: four top-level "layer types" (`ocean_floor`, `volcanic`, `land`, `uplift`), each a list of named layers, each mapping a rock to the next layer, ending in an endlessly repeating `bottom`. Layer thickness is noise (43–63 blocks) and cannot be configured. TFC has **21 rocks**, and ores exist as per-rock blocks.

### Cited Findings
- `world/settings/RockSettings.java`: `record RockSettings(Block raw, Block hardened, Block gravel, Block cobble, Block sand, Block sandstone, Optional<Block> spike, Optional<Block> loose, Optional<Block> mossyLoose, Optional<Boolean> karst, Optional<Boolean> mafic)`. Registry key `tfc:worldgen/rock_settings`. — [RockSettings.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/settings/RockSettings.java)
- `world/settings/RockLayerSettings.java`:
  - `Data(rocks: Map<String, Holder<RockSettings>> via RegistryFileCodec, bottom: List<String>, layers: List<LayerData{id, layers: ordered map rock→nextLayer}>, ocean_floor, land, volcanic, uplift: List<String>)`.
  - `sampleAtLayer(pointRock, n)` seeds a `XoroshiroRandomSource` with `pointRock >> 2`. It picks the root list by the low 2 bits (OCEAN=0, VOLCANIC=1, LAND=2, else UPLIFT) and walks `next` n times.
  - It also offers `getRock(Block)` (a map from any rock block to its RockSettings) and `getHardened(raw)`.
  - Source: [RockLayerSettings.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/settings/RockLayerSettings.java)
- Docs on layer types: "`ocean_floor` (Oceans), `volcanic` (Volcanic islands, and volcanic mountains), `uplift` (Inland mountains), or `land` (All other non-ocean areas)". On the bottom layer: "Once the `bottom` layer is reached, any further rocks underneath it will repeat endlessly." — [Docs: World Preset → Rock Layer Settings](https://terrafirmacraft.github.io/Documentation/1.21.x/worldgen/world-preset/)
- Default `rock_layer_settings` in the 1.21.x preset:

  | Name | Contents |
  |---|---|
  | `bottom` | gneiss, schist, diorite, granite, gabbro |
  | `felsic` | granite → bottom |
  | `intermediate` | diorite → bottom |
  | `mafic` | gabbro → bottom |
  | `igneous_extrusive` | andesite → intermediate, basalt → mafic, dacite → intermediate, rhyolite → felsic |
  | `igneous_extrusive_x2` | each extrusive rock → igneous_extrusive |
  | `igneous_intrusive` | diorite → intermediate, gabbro → mafic, granite → felsic |
  | `high_grade` | gneiss, schist → bottom |
  | `low_grade` | phyllite, slate → high_grade |
  | `marble` | marble → bottom |
  | `quartzite` | quartzite → bottom |
  | `sedimentary` | chalk → marble, chert → quartzite, claystone → low_grade, conglomerate → low_grade, dolomite → marble, limestone → marble, shale → low_grade |
  | `uplift` | diorite, gabbro, granite → low_grade; gneiss, phyllite, schist, slate → high_grade; marble, quartzite → bottom |
  | top-level `ocean_floor` | [igneous_extrusive] |
  | top-level `land` | [sedimentary ×3, igneous_extrusive] |
  | top-level `volcanic` | [igneous_extrusive, igneous_extrusive_x2, igneous_intrusive] |
  | top-level `uplift` | [uplift ×3, sedimentary] |

  `tuff` is in `rocks` but appears in no layer. — [overworld.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/worldgen/world_preset/overworld.json)
- Choosing the layer type (`world/region/ChooseRocks.java`):
  - `point.rock = (uniformRockArea << 2) | type`.
  - The type is VOLCANIC if, within Manhattan distance 2, a point is an island at distance < 4, has a hotspot, or is volcanic.
  - Otherwise it is UPLIFT if a mountain or coastal mountain is at distance < 3.
  - Otherwise it is LAND or OCEAN.
  - Source: [ChooseRocks.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/ChooseRocks.java)
- Vertical layering (`RegionChunkDataGenerator.generateRock`):
  1. Surface Y above 125 is compressed: `125 + 0.3·(surfaceY − 125)`.
  2. Layer thickness = `OpenSimplex2D.octaves(3).scaled(43, 63).spread(0.014)`, with a per-layer lateral offset.
  3. Lateral skew = `±1.8 × (deltaY + 12)` blocks.
  4. The rock is `rockLayerSettings.sampleAtLayer(rockLayerArea(skewX, skewZ), layerIndex)`.
  - Source: [RegionChunkDataGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RegionChunkDataGenerator.java)
- The `common/blocks/rock/Rock.java` enum has 21 rocks:
  - Felsic intrusive: GRANITE. Intermediate intrusive: DIORITE. Mafic intrusive: GABBRO.
  - Felsic extrusive: RHYOLITE. Mafic extrusive: BASALT. Intermediate extrusive: ANDESITE, DACITE.
  - SEDIMENTARY display category: SHALE, CLAYSTONE, LIMESTONE, CONGLOMERATE, DOLOMITE, CHERT, CHALK, **TUFF**.
  - METAMORPHIC: QUARTZITE, SLATE, PHYLLITE, SCHIST, GNEISS, MARBLE.
  - `RockCategory`: IGNEOUS_EXTRUSIVE, IGNEOUS_INTRUSIVE, METAMORPHIC, SEDIMENTARY (hardness modifiers 0, +0.2, −0.2, −0.4).
  - Source: [Rock.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Rock.java); [RockCategory.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/RockCategory.java); [RockDisplayCategory.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/RockDisplayCategory.java)
- Block types per rock (`Rock.BlockType`): RAW, HARDENED, SMOOTH, COBBLE, BRICKS, GRAVEL, SPIKE, CRACKED_BRICKS, MOSSY_BRICKS, MOSSY_COBBLE, CHISELED, LOOSE, MOSSY_LOOSE, PRESSURE_PLATE, BUTTON, AQUEDUCT, ROPE_ANCHOR. Slabs, stairs and walls come from decorations, and rock anvils exist for igneous rocks. — [Rock.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Rock.java)
- Default rock_settings files (`data/tfc/tfc/worldgen/rock_settings/*.json`, 21 files):
  - Example: basalt = `raw tfc:rock/raw/basalt`, `hardened tfc:rock/hardened/basalt`, gravel, cobble, `sand tfc:sand/black`, `sandstone tfc:raw_sandstone/black`, spike, loose, mossy_loose, `karst false`, `mafic true`.
  - `karst: true`: limestone, dolomite, chalk, marble. `mafic: true`: basalt, gabbro.
  - Sand colours:
    - white: chalk, dolomite, limestone, marble, quartzite
    - red: andesite, chert, dacite, diorite
    - black: basalt, gabbro
    - brown: claystone, conglomerate, shale
    - yellow: gneiss, granite, phyllite, rhyolite, schist, slate
    - green: tuff
  - Source: [rock_settings dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/generated/resources/data/tfc/tfc/worldgen/rock_settings)
- Some surface builders hard-code rocks instead of using RockSettings:
  - `BadlandsSurfaceBuilder` uses `Rock.TUFF` (volcanic badlands layers).
  - `TuyaSurfaceBuilder` and `CinderConeSurfaceBuilder` use `Rock.BASALT`.
  - `SurfaceStates` references `Rock.GRANITE` / `Rock.DIORITE`.
  - The 4.2.0 notes say stratovolcanoes are "made of layers of igneous extrusive rocks and tuff".
  - Source: [surface/builder dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/java/net/dries007/tfc/world/surface/builder); [v4.2.0 notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.0)
- Ores are **per-rock blocks**. Vein features (`tfc:cluster_vein`, disc, pipe; 52 configured veins under `data/tfc/worldgen/configured_feature/vein/`) list `"replace": ["tfc:rock/raw/<rock>"]` and give weighted `"with"` blocks such as `tfc:ore/poor_native_copper/rhyolite`, `tfc:ore/normal_…`, `tfc:ore/rich_…`, `tfc:deposit/native_copper/rhyolite`. — [montane_native_copper.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/montane_native_copper.json)
- The `Ore` enum: NATIVE_COPPER, NATIVE_GOLD, HEMATITE, NATIVE_SILVER, CASSITERITE, BISMUTHINITE, GARNIERITE, MALACHITE, MAGNETITE, LIMONITE, SPHALERITE, TETRAHEDRITE, GYPSUM, CINNABAR, CRYOLITE, BORAX, GRAPHITE, SALTPETER, SULFUR, SYLVITE, AMETHYST, DIAMOND, EMERALD, LAPIS_LAZULI, OPAL, PYRITE, RUBY, SAPPHIRE, TOPAZ, BITUMINOUS_COAL, LIGNITE, HALITE. — [Ore.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Ore.java)
- `VeinConfig` fields: `blocks` (map Block → weighted BlockState), `indicator`, `rarity`, `density`, `min_y`, `max_y`, `project`, `project_offset`, `random_name` (seed), `near_lava`. It does not read ChunkData. — [VeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinConfig.java)

### Inferences
- TFC's layer chain cannot express real stratigraphy. It has no ages, no explicit thicknesses, no unconformities, and no lateral facies tied to tectonic history. A simulated-geology mod should **bypass `RockLayerSettings` sampling** and answer `ChunkDataGenerator.generateRock(x, y, z, surfaceY)` from its own stratigraphic column.
- The mod should still supply a valid `rock_layer_settings` object. It is a required codec field, and features use `rockLayerSettings().getRock(block)` to map placed blocks back to `RockSettings`. Every rock block the mod places must appear in `rocks`.
- TFC ores, veins and indicators only exist for the 21 TFC rocks, because ore block IDs are `tfc:ore/<grade>_<ore>/<rock>`. Simulated lithologies must map onto those 21 rocks (or onto new rocks that bring their own ore blocks and veins). Veins work with any generator that places TFC `raw` rock blocks, since they replace by block.
- Tuff has no layer slot. If the simulation produces pyroclastic deposits, it can emit `tfc:rock/raw/tuff` directly. That is allowed because tuff is in `rocks`.

### Gaps
- I did not read the `cluster/disc/pipe` vein algorithms or indicator placement in detail; another research note may cover ore deposits.
- I did not verify whether the rock_settings registry (4.2.8) lets datapacks add new rocks with full gameplay support (tools, anvils) or only remap blocks.

---

## 5. Climate: ClimateModel, OverworldClimateModel, latitude and seasons; can a third party supply its own?

### Takeaway
Climate is pluggable. NeoForge fires `SelectClimateModelEvent` per `ServerLevel`, and the chosen `ClimateModel` must have a registered, client-synced `ClimateModelType`. For any overworld whose generator implements `ChunkGeneratorExtension`, TFC itself picks `OverworldClimateModel`.

That model reads average temperature, rainfall, rain variance and groundwater **from `ChunkData`**. It then adds **z-based** seasonal and daily variation, an elevation lapse rate tied to sea level 63, and wind bands by z. Latitude (sun position, day length, hemisphere) is a **static triangle-wave function of z** with the equator fixed at `z = temperatureScale/2`. A climate model can change the scale through `hemisphereScale()`, but not the offset.

### Cited Findings
- `util/climate/ClimateModel.java` javadoc:
  - "Climate models are assigned to dimensions using the `SelectClimateModelEvent`. TFC listens to this event at highest priority and will assign an overworld climate model for the overworld… In order to override the climate model for the overworld or any other dimension, you simply must listen to the event at normal priority."
  - Methods: `type()`, `hemisphereScale()` (default 20,000), `getAverageTemperature`, `getInstantTemperature(level, pos, calendarTicks, daysInMonth)`, `getTimeAverageTemperature`, `getAverageRainfall` ("must be in the range [0, 500]"), `getRainfallVariance` ([-1, 1]; positive = wet summers), `getInstantRainfall`, `getBaseGroundwater`, `getAverageGroundwater`, `getInstantGroundwater`, `getRain(calendarTicks)`, `getThunder`, `supportsRain`, `getWind` (Vec2), `getFog`.
  - "Climate models are not ticked… must be able to be fully re-created from scratch, on an arbitrary query time."
  - Source: [ClimateModel.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/climate/ClimateModel.java)
- `util/events/SelectClimateModelEvent.java` defaults to `BiomeBasedClimateModel.INSTANCE`. `ForgeEventHandler.onSelectClimateModel` sets `new OverworldClimateModel(level, ex)` when `dimension == OVERWORLD && generator instanceof ChunkGeneratorExtension`. — [SelectClimateModelEvent.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/events/SelectClimateModelEvent.java); [ForgeEventHandler.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/ForgeEventHandler.java)
- `util/climate/ClimateModels.java` has a registry built with `.sync(true)` and a `DeferredRegister<ClimateModelType<?>> TYPES`. Registered types: `overworld` (OverworldClimateModel.STREAM_CODEC) and `biome_based`. — [ClimateModels.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/climate/ClimateModels.java)
- `OverworldClimateModel` (a public class, **protected** constructor `(long climateSeed, float temperatureScale)`, protected `calculateMonthlyTemperature` and `calculateDailyTemperature`):
  - Average temperature = `ChunkData.getAverageSeaLevelTemp(pos)` adjusted by elevation.
  - Instant temperature = sea-level temperature + monthly term + daily term.
  - The monthly term is `monthFactor * triangle(-18, 0, 1/(4·temperatureScale), z − temperatureScale/2)`.
  - The daily term is roughly ±4 °C, seeded per day.
  - Instant rainfall = `triangle(rainVariance·rainAverage, rainAverage, 1, fractionOfYear + 0.75)`.
  - Wind uses bands by `pos.getZ()`.
  - Source: [OverworldClimateModel.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java)
- `Helpers.adjustAverageTemperatureByElevation`:
  - Above y = 0: `avg − clamp((y − 63)·0.16225, 0, 17.822)`.
  - Below y = 0: lerp toward `LAVA_LEVEL_TEMPERATURE = 15` at `DEPTH_LEVEL = −64`. The 4.2.12 changelog says: "Below y=0, temperature now gets warmer as you approach lava level."
  - Source: [Helpers.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/Helpers.java); [CHANGELOG / v4.2.12](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.12)
- `client/overworld/SolarCalculator.getLatitude(z, hemisphereScale) = triangle(−π/2, 0, 1/(4·hemisphereScale), z − 0.5·hemisphereScale)`. It drives `getSunBasedDayTime` and `getInNorthernHemisphere`. — [SolarCalculator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/client/overworld/SolarCalculator.java)
- Region climate generation (`RegionGenerator` constructor):
  - `temperatureNoise = triangle wave on **z** (scale = temperatureScale), scaled −20…30 °C, + OpenSimplex ±3`.
  - `rainfallNoise = triangle wave on **x** (scale = rainfallScale), scaled 0…500 mm, + OpenSimplex −80…+40`.
  - If a scale is 0, the matching `*_constant` is used instead.
  - Source: [RegionGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java)
- `AnnotateClimate`:
  - It biases land near the ocean toward 5 °C and higher rainfall, using a noise-weighted "oceanic influence" delta.
  - It sets `rainfallVariance` from `distanceToWestCoast` plus noise, mapping 0..80 to −1..1. The variance is damped near cell edges.
  - Source: [AnnotateClimate.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/AnnotateClimate.java)
- From the 4.0.0 notes:
  - "Adjusting the 'Temperature Scale' setting … will change the locations of poles and equators."
  - "Northern and southern hemispheres alternate infinitely if you travel far enough north or south."
  - Dry-summer coastal climates appear on west coasts and monsoon climates on east coasts.
  - Köppen climates are shown in the inventory menu (class `util/climate/KoppenClimateClassification`).
  - Source: [v4.0.0-beta notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta)
- TFC-Real-World (1.21.x) overrides `SolarCalculator.getLatitude` and `getInNorthernHemisphere` with mixins, along with `WeatherHelpers` and `RegionChunkDataGenerator` (climate layers). It supplies temperature (−50…50 °C), rainfall (0…500 mm) and rain-variance maps. — [TFC-Real-World mixin list](https://github.com/yazloysasha/TFC-Real-World/tree/1.21.x/src/main/java/net/yazloysasha/tfcrealworld/mixin); [README](https://github.com/yazloysasha/TFC-Real-World/blob/1.21.x/README.md)

### Inferences
- Default latitude geometry, derived from the formulas with s = temperature_scale = 20,000:
  - North pole at z = −s/2 = −10,000 (coldest, −20 °C base).
  - Equator at z = +s/2 = +10,000 (30 °C base).
  - The next pole at z = 3s/2 = 30,000.
  - The pattern repeats every 2s = 40,000 blocks.
  - Spawn at z = 0 is mid-latitude (about 5 °C base).
  - Default rainfall varies **east–west** (along x), with a period of 2 × rainfall_scale.
- For a planet wrapping east–west with latitude along z:
  - TFC's *seasonal* model (a function of z only) fits well. Set `temperature_scale` to the pole-to-equator distance in blocks, and place the planet's equator at z = scale/2 and its poles at z = −scale/2 and z = 3·scale/2.
  - A world centred on z = 0 for the equator needs either a coordinate offset or a mixin on `SolarCalculator.getLatitude` (client) plus a subclass of `OverworldClimateModel` (server).
  - TFC's x-periodic rainfall is irrelevant if the mod supplies its own ChunkData climate.
- Registering a separate `ClimateModel` is possible but often unnecessary. `OverworldClimateModel` already reads everything time-invariant from ChunkData, so a generator that fills ChunkData from its climate simulation gets TFC seasons, crops, snow and colours for free. A custom model is only needed to change seasonality, the lapse rate, wind, or latitude placement. It needs its own `ClimateModelType` registered in the synced `tfc` climate-model registry, plus a stream codec.
- Allowed value ranges: sea-level mean temperature, typically −20…30 °C (crops and plants are tuned to TFC ranges); rainfall 0–500 mm/yr; rain variance −1…1; groundwater 0–500. Simulated values must be mapped into these ranges.

### Gaps
- I did not check the `Month` temperature modifiers or wind magnitude formulas in detail.
- I did not check whether `ClimateModels.REGISTRY` accepts third-party registrations at runtime without ordering problems. It is a standard NeoForge registry with a DeferredRegister, so registration should work; this is not verified.

---

## 6. Biomes: BiomeExtension, TFCBiomes, how biomes are chosen, surface builders

### Takeaway
Each TFC biome is a vanilla `Biome` JSON paired with a `BiomeExtension`. The extension is held in a TFC registry `tfc:biome_extension` whose IDs match the biome IDs, and it carries the heightmap noise, surface builder, blend types, river/shore/volcano flags and aquifer lookahead.

Biomes are chosen per 128-block region point by the `CHOOSE_BIOMES` task. Inputs are altitude class, flags, plate divergence, hotspot age, karst surface rock, and **temperature/rainfall**. The result is zoomed to quart resolution, and river biomes are overlaid. Biomes do not determine climate.

`SurfaceManager.buildSurface(...)` is public and could be reused by another generator if it supplies per-column BiomeExtensions and helper arrays.

### Cited Findings
- `world/biome/BiomeExtension.java` (final; constructor is package-private) has these fields: `key`, `noiseFactory`, `aquiferSurfaceHeight`, `surfaceBuilderFactory`, `biomeBlendType`, `riverBlendType`, `shoreBlendType`, `salty`, `hasCinderCones`, `hasTuffCones`, `hasTuyas`, `hasStratovolcanoes`, `hasAtolls`, `centeredFeatureFrequency/BaseHeight/ScaleHeight/Ice`, `spawnable`, `rivers`, `shore`, `shoreBaseHeight`, `sandyRiverShores`. — [BiomeExtension.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/BiomeExtension.java)
- `world/biome/BiomeBuilder.java` has a public `builder()` and public setters: `heightmap`, `surface`, `carving`, `noise`, `aquiferHeightOffset`, `type(BiomeBlendType/RiverBlendType/ShoreBlendType)`, `salty`, `spawnable`, `noRivers`, `shore`, `cinderCones`, `tuffRings`, `stratovolcanoes`, `tuyas`, `atolls`. It ends with a public `build(ResourceKey<Biome>)`. — [BiomeBuilder.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/BiomeBuilder.java)
- `TFCBiomes` details:
  - `REGISTRY = new RegistryBuilder<>(KEY "tfc:biome_extension").create()`, plus a DeferredRegister `EXTENSIONS`.
  - 125 `BiomeExtension` constants, from OCEAN to STONE_CIRCLES.
  - Lookup: `getExtension(level, biome)`, cached by `mixin/BiomeMixin`, maps the biome's ResourceKey to an extension with the same location.
  - `getExtensionOrThrow` is used by `TFCChunkGenerator.applyBiomeDecoration`.
  - Source: [TFCBiomes.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/TFCBiomes.java); [BiomeMixin.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/mixin/BiomeMixin.java)
- The docs list the biome groups:
  - aquatic, low/mid/high-altitude continental, island, shore, water, lake, dry
  - tower karst, karren (burren), shilin, doline, cenote
  - shield volcano, ice sheet, ice-sheet edge, glaciated, glacially carved, paleoglacial
  - Features attach through placed-feature tags `tfc:in_biome/erosion`, `…/veins`, `…/soil_discs/<biome>`, `…/large_features/<biome>`, `…/surface_decoration/<biome>`, `…/top_layer_modification`, etc.
  - "Using Non-TFC Biomes in TFC … the biomes must have a `BiomeExtension` registered… If you want TFC biomes to be *generated* by the TFC biome source, this will very likely require custom hooks… Please consult us on Discord."
  - Source: [Docs: Biomes](https://terrafirmacraft.github.io/Documentation/1.21.x/worldgen/biomes/)
- The v4.0.0 notes say the biome count grew "from the 30 that existed in 1.20 to **109**". The current code registers 125, after 4.2.0 added coastal terraces, karst, stratovolcano and related biomes. — [v4.0.0-beta notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta); [TFCBiomes.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/TFCBiomes.java)
- `ChooseBiomes` rules use these inputs:
  - `point.divergence`, `distanceToEdge`, `distanceToOcean`, `temperature`, `rainfall`.
  - Ice sheets when `temperature < −16 + 0.006·rainfall`.
  - Atolls when `temperature > 12`.
  - `hotSpotAge`, which selects active, dormant, extinct, ancient or sunken shield volcanoes.
  - Low-rainfall replacements (e.g. rainfall ≤ 55; < 145 turns patterned ground into stone circles), and no badlands in high rainfall.
  - Source: [ChooseBiomes.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/ChooseBiomes.java)
- `BiomeSourceExtension` (an interface) requires `getBiomeExtensionNoRiver(quartX, quartZ)`, `getBiomeFromExtension(ext)` and `getPartition(blockX, blockZ)` (which returns a `RegionPartition.Point` of river edges). It has `default initRandomState(RegionGenerator, ConcurrentArea<BiomeExtension>)` and `findSpawnBiome(Settings, RandomSource)`. `RegionBiomeSource` (codec `tfc:overworld`) implements these on top of the RegionGenerator. — [BiomeSourceExtension.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/BiomeSourceExtension.java); [RegionBiomeSource.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/RegionBiomeSource.java)
- Surface building:
  - `world/surface/SurfaceManager` is `public final` with a public constructor `(Seed)` and `public void buildSurface(LevelAccessor, ChunkAccess, RockLayerSettings, ChunkData, BiomeExtension[] accurateChunkBiomes, BiomeExtension[] …NoRivers, double[] weights, double[] slopeMap, int[] preVolcanicHeights, RandomSource, int seaLevel, int minY, BiomeExtension cinderCone, tuffRing, tuya, atoll, stratovolcano)`.
  - There are 26 surface builders in `world/surface/builder` (Normal, Badlands, Burren, Dune, Shilin, IceSheet, Stratovolcano, …).
  - `SurfaceBuilderContext` reads rainfall, rain variance, base groundwater and average temperature from ChunkData. `SoilSurfaceState` uses them to choose among the 8 soil types (the 4.0.0 notes list Entisols, Aridisols, Mollisols, Alfisols, Podzols, Andisols, Fluvisols, Oxisols).
  - Source: [SurfaceManager.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/surface/SurfaceManager.java); [SoilSurfaceState.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/surface/SoilSurfaceState.java); [v4.0.0-beta notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta)

### Inferences
- A replacement terrain mod can still **use TFC biomes** by classifying each quart column of its simulated terrain into one of TFC's 125 biome IDs. Examples: collisional mountains from collision belts, rift valleys from divergent continental crust, ocean ridges from spreading centres, shield volcanoes from hotspots, ice sheets from simulated glaciation. TFC features, decoration tags and mob spawns then follow.
- The BiomeExtension *heightmaps* would be ignored if the mod produces its own heights. The *surface builders* (soil, sand, snow, karst surfaces) can be reused through the public `SurfaceManager`, but they expect TFC-shaped helper arrays (slope map, pre-volcanic heights, centered-feature biomes). Those arrays would have to be synthesized.
- New biomes (e.g. simulated environments TFC lacks) are possible. Register a vanilla biome JSON plus a `BiomeExtension` with the same ID through the public `BiomeBuilder` and a DeferredRegister on `tfc:biome_extension`.

### Gaps
- I did not determine whether `SurfaceManager.buildSurface` tolerates "neutral" helper arrays (e.g. all-zero slope, no centered features) without visual artifacts.
- I did not check whether the `tfc:biome_extension` registry is frozen before third-party DeferredRegisters fire. Standard NeoForge registries accept other namespaces, so it should work; this is not verified.

---

## 7. Feature hooks: which TFC features and placements read ChunkData or need a TFC generator

### Takeaway
Many TFC features read `ChunkData`: climate, forest type, rock and surface rock. Three features **hard-cast** `context.chunkGenerator()` to `ChunkGeneratorExtension` and would throw `ClassCastException` under any other generator. Ore veins are pure block-replacement features and need only TFC raw-rock blocks to be present.

### Cited Findings
- Features that call `ChunkData.get(...)` / `getRockData()`:
  - `BouldersFeature` and `LooseRockFeature` (surface rock)
  - `DynamicDensityRandomPatchFeature`, `NoisyMultipleFeature`
  - `ErosionFeature` (rock + hardened)
  - `FissureFeature` (rock at minY + 1)
  - `HotSpringFeature` (rock at y = 0)
  - `SeaStacksFeature` (surface rock hardened)
  - `SoilForestAreaFeature`, `TidePoolFeature`
  - `cave/CaveColumnFeature` (hardened rock), `cave/IceCaveFeature`
  - `tree/ForestFeature` (uses `getForestType()` for per-chunk chance and tree/bush counts)
  - placement `ClimatePlacement`, and structure placement `ClimateStructurePlacement`
  - Source: [world/feature dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/java/net/dries007/tfc/world/feature); [ForestFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/tree/ForestFeature.java)
- Hard casts to `ChunkGeneratorExtension`:
  - `ErosionFeature`: `(ChunkGeneratorExtension) context.chunkGenerator()`, then `extension.getOrCreateAquifer(chunk)` and `extension.rockLayerSettings()`.
  - `SpringFeature`: `extension.rockLayerSettings()`.
  - `cave/CaveSpikesFeature`: `((ChunkGeneratorExtension) context.chunkGenerator()).rockLayerSettings().getRock(wallState.getBlock())`.
  - Source: [ErosionFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/ErosionFeature.java); [SpringFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/SpringFeature.java); [CaveSpikesFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/cave/CaveSpikesFeature.java)
- `ClimatePlacement` filters on `min/max_temperature`, `min/max_groundwater`, `min/max_rain_variance` (with `rain_variance_absolute`), `min/max_forest`, `forest_types`, `min/max_elevation` (default −64..320), `fuzzy` and `ignore_rivers`. — [ClimatePlacement.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/placement/ClimatePlacement.java)
- Other TFC placement modifiers include `AtollPlacement`, `CinderConePlacement`, `StratovolcanoPlacement`, `TuffRingPlacement`, `TuyaPlacement`, `OceanRidgePlacement`, `FlatEnoughPlacement`, `NearFluidPlacement`, `ShallowWaterPlacement`, `UndergroundPlacement`, `IntertidalPlacement` and `BoundedCarvingMaskPlacement`. The volcano placements key off TFC's `CenteredFeatureNoise`. — [world/placement dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/java/net/dries007/tfc/world/placement)
- Veins: `VeinConfig` has no ChunkData dependency and replaces listed raw-rock blocks. The veins are attached via the `tfc:in_biome/veins` tag. — [VeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinConfig.java); [Docs: Biomes](https://terrafirmacraft.github.io/Documentation/1.21.x/worldgen/biomes/)

### Inferences
- A replacement generator must implement `ChunkGeneratorExtension`. Otherwise the erosion, spring and cave-spike features crash, if they are used at all (they are in TFC biome tags).
- Volcano-centered placements (cinder cones, stratovolcanoes, tuff rings, tuyas, atolls) depend on TFC's centered-feature noise and on biome flags. In a custom generator they will either be inert or misplaced. The mod may need its own volcano features keyed to its simulated volcanoes, or it could drop those biomes or flags.

### Gaps
- I did not audit every placed feature JSON to see which use `ClimatePlacement` versus biome-only placement.

---

## 8. Integration options and existing addons that change TFC terrain

### Takeaway
There are three viable routes in 1.21.1, and each has a working precedent.

- **(a) Own generator implementing TFC's interfaces.** Write a `ChunkGenerator implements ChunkGeneratorExtension` and a `ChunkDataGenerator`. This uses TFC's documented-ish API and needs no TFC mixins. KubeJS-TFC's `WrappedChunkGenerator` makes **any** chunk generator TFC-compatible this way.
- **(b) Feed TFC's own pipeline with external data through mixins.** TFC-Real-World (1.21.x) mixes into nearly every region task, plus the height filler, biome noise, chunk-data generator and the solar latitude calculator, to drive TFC with Earth maps.
- **(c) Mixin-replace parts of `TFCChunkGenerator`.** TerraFirmaGreg Core-Modern (1.20.1) redirects TFC's biome-layer and surface creation.

For a simulation that owns heights, rocks and climate, **(a)** is the most robust. **(b)** gives the most "native TFC" look but is fragile and still constrained by TFC's per-biome height noises.

### Cited Findings
**Route (a): KubeJS-TFC (1.21.1, MIT)**
- `WrappedChunkGenerator extends ChunkGenerator implements ChunkGeneratorExtension`. Codec: `generator` (any ChunkGenerator), `event_key`, `settings` (TFC `Settings`).
- Its `initRandomState`:
  - creates a `KubeChunkDataGenerator`,
  - stores `rs.sampler()`,
  - calls `((RandomStateExtension) rs).tfc$setChunkGeneratorExtension(this)`,
  - and, on a second init, copies itself through `ChunkMapBridge.tfc$updateGenerator`.
- `fillFromNoise` generates chunk data, delegates to the wrapped generator, then calls `generateFullIfNot` and `data.getRockData().useCache(pos)`.
- `findSpawnBiome` is overridden to use the vanilla `Climate.SpawnFinder`, because the wrapped biome source is not a `BiomeSourceExtension`.
- `buildSurface` passes a `ChunkGenAwareWorldGenerationContext` so surface rules can query rock (`RockSurfaceRuleSource`).
- Source: [WrappedChunkGenerator.java](https://github.com/Notenoughmail/KubeJS-TFC/blob/1.21.1/src/main/java/io/github/notenoughmail/kubejstfc/worldgen/generator/WrappedChunkGenerator.java)
- `KubeChunkDataGenerator implements ChunkDataGenerator`:
  - The default partial step fills all four `LerpFloatLayer`s with 0 and `ForestType.GRASSLAND`.
  - The default full step builds `surfaceHeight[256]` from `Heightmap.Types.OCEAN_FLOOR_WG` and `aquifer[16]` = (4×4 average − 20).
  - The default aquifer is `Aquifer.createDisabled(...)`.
  - The default rock is an all-AIR `RockSettings`.
  - Scripts can override all of these: `generatePartial`, `generateFull`, `createAquifer`, `generateRock`.
  - Source: [KubeChunkDataGenerator.java](https://github.com/Notenoughmail/KubeJS-TFC/blob/1.21.1/src/main/java/io/github/notenoughmail/kubejstfc/worldgen/generator/KubeChunkDataGenerator.java)

**Route (b): TFC-Real-World (1.21.x branch, MIT)**
- README: "The mod works by replacing TFC's default noise generators with data sampled from customizable map images… Uses Mixins to feed TFC's own world generation with map data." Also: "It is still TFC. The mod only tells TerraFirmaCraft what kind of place every spot on Earth is; TFC builds the biomes, the rocks and the seasons itself."
- Inputs:
  - a continent map;
  - a tectonics palette map (classes like lowland, upland, mountain, rift, collision, volcanic arc, reef; hotspot ages);
  - temperature, rainfall and rain-variance maps;
  - `rivers.bin` (river edges with widths; "TFC bends and carves them itself"; keep edges about 345 blocks long because "TFC checks every edge near a column, so many short edges slow generation down");
  - `ridges.bin`.
- The default world is 80,000 × 40,000 blocks.
- Source: [TFC-Real-World README](https://github.com/yazloysasha/TFC-Real-World/blob/1.21.x/README.md)
- Its mixins target:
  - region tasks: `AddContinentsAndSetOceanDepths`, `AddHotspots`, `AddIslands`, `AddMountainsAndBarrierIslands`, `AddRiversAndLakes`, `AnnotateBaseLandHeight`, `AnnotateBiomeAltitude`, `AnnotateBoundaryTypes`, `AnnotateClimate`, `AnnotateDistanceToCellEdge`, `AnnotateDistanceToOcean`, `AnnotateDistanceToWestCoast`, `ChooseBiomes`, `FloodFillSmallOceans`, `RegionGenerator`;
  - world classes: `ChunkHeightFiller` (inject HEAD/RETURN of `sampleColumnHeightAndBiome`, `sampleRiverEdge`), `ChunkNoiseFiller`, `ChunkBaseBlockSource`, `BiomeNoise`, `BiomeExtension`, `BiomeSourceExtension`, `RegionChunkDataGenerator` (climate layers, river groundwater), `TFCLayers`;
  - client and weather: `SolarCalculator` (`getLatitude`, `getInNorthernHemisphere`), `WeatherHelpers`, and the `CreateTFCWorldScreen` UI.
  - Source: [TFC-Real-World mixins](https://github.com/yazloysasha/TFC-Real-World/tree/1.21.x/src/main/java/net/yazloysasha/tfcrealworld/mixin)

**Route (c): TerraFirmaGreg Core-Modern (1.20.1, LGPL-3.0)**
- `mixins/common/tfc/new_ow_wg/TFCChunkGeneratorMixin` ("Points the TFC chunk generator to use the new biome layers if the config option is enabled") uses `@Redirect` on `TFCLayers.createRegionBiomeLayer` inside `initRandomState` and swaps in its own surface manager.
- The repo also carries modified copies of TFC classes (`net/dries007/tfc/world/...`, e.g. `ChunkDataProvider.java`) for 1.20.1.
- `minecraft_version = 1.20.1`.
- Source: [TFCChunkGeneratorMixin.java](https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/src/main/java/su/terrafirmagreg/core/mixins/common/tfc/new_ow_wg/TFCChunkGeneratorMixin.java); [gradle.properties](https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/gradle.properties)

**Other related projects (found via GitHub code search)**
- `Notenoughmail/TFCGenViewer` (default branch 1.21.1, MIT): world preview before creation. — [TFCGenViewer](https://github.com/Notenoughmail/TFCGenViewer)
- `RyanSeacroft/tfc-atlas` (1.21.x, MIT): "TerraFirmaCraft terrain prediction and search for Xaero's World Map". It uses accessor mixins into the TFC generator and rock noise. — [tfc-atlas](https://github.com/RyanSeacroft/tfc-atlas)
- `TKFT/World-Preview-TFC` (Apache-2.0): a fork of World Preview for TFC. — [World-Preview-TFC](https://github.com/TKFT/World-Preview-TFC)
- `jc-cy/TerraFirmaEarth-1.20.1`: an Earth map for TFC on 1.20.1, using mixins on `RegionChunkDataGenerator` and `ChunkData`. — [TerraFirmaEarth-1.20.1](https://github.com/jc-cy/TerraFirmaEarth-1.20.1)
- `Notenoughmail/TFCBiomeExtender` (branch 1.20, EUPL-1.2): `DatapackableChunkGeneratorExtension`. — [TFCBiomeExtender](https://github.com/Notenoughmail/TFCBiomeExtender)
- `Fuyuaki-Vulpes/Wilderness-Reborn` has a `WildChunkGenerator implements ChunkGeneratorExtension`. However, it targets MC 26.2 and its imports are its own region classes, not `net.dries007`, so it is not a TFC addon. — [Wilderness-Reborn](https://github.com/Fuyuaki-Vulpes/Wilderness-Reborn)
- `Skapcz/TFC-WorldGen` ("Minecraft addon for TerraFirmaCraft which makes custom world generation") contains only a README. — [TFC-WorldGen](https://github.com/Skapcz/TFC-WorldGen)
- ArborFirmaCraft is a tree addon, not terrain. The 4.2.4 notes warn the update "is expected to break any addons which add trees, including ArborFirmaCraft". — [v4.2.4 notes](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.4)

### Inferences
Recommended architecture for the plate-tectonics mod, route (a) plus selective reuse:
1. `PlanetChunkGenerator extends ChunkGenerator implements ChunkGeneratorExtension`, with codec fields for our simulation settings plus a TFC `Settings`. Keep sea level at 63 and use `minecraft:overworld` noise settings, y −64..320.
2. Initialize in `initRandomState`, which TFC's own ChunkMap mixin calls. Load or compute the persisted simulation, build `PlanetChunkDataGenerator implements ChunkDataGenerator`, and register on `RandomStateExtension`. Mirror TFC's copy-on-reinit pattern.
3. `PlanetChunkDataGenerator`:
   - `generate()` → `generatePartial` with chunk-corner climate from the simulation, and a `ForestType` derived from simulated vegetation density.
   - Full status is set in our `fillFromNoise` (surface and aquifer heights).
   - `generateRock(x, y, z, surfaceY, cache)` answers from the simulated stratigraphic column, mapped to TFC `RockSettings`.
   - It must be fast, thread-safe and deterministic.
4. Biome source: a custom `BiomeSource` that assigns TFC biome IDs (or our own biomes with `BiomeExtension`s) per quart from the simulation's terrain classes. Implement `BiomeSourceExtension` only if TFC's spawn search or river lookup is wanted. Otherwise override `findSpawnBiome`, as KubeJS-TFC does.
5. Climate: let TFC choose `OverworldClimateModel`, which happens automatically. Align our latitude to TFC's z convention, or subclass the model and mixin `SolarCalculator`.
6. Ores: place TFC raw-rock blocks and let TFC vein features (in biome tags) run. Optionally add our own simulation-driven deposit features that emit `tfc:ore/<grade>_<ore>/<rock>` blocks.
7. Optionally reuse TFC's public `SurfaceManager` for soils and surface textures.

Route (b) suits a mod that only wants to "steer" TFC (continents, climate, rivers) and accepts TFC-style biome heightmaps. It requires about 25 fragile mixins into internal classes, as TFC-Real-World shows.

### Gaps
- I could not verify by web search: whether Tectonic (the vanilla terrain overhaul) has any TFC compatibility (I found no GitHub code linking them), "TFC Caves" status on 1.21.1, or TerraFirmaGreg's 1.21 plans. Search budget was exhausted.
- I did not run or test KubeJS-TFC's wrapper. Whether every TFC feature behaves correctly under it (e.g. erosion with a disabled aquifer) is unverified.

---

## 9. Gaps, blockers, hard parts, and licensing

### Takeaway
Nothing outright *blocks* a third-party generator from being TFC-compatible in 1.21.1: the interfaces needed exist and are public. The hard parts are:
1. TFC's hard-coded assumptions: sea level 63, z-based latitude and seasons with a fixed equator offset, climate value ranges, a fixed `ForestType` enum, 21 rocks with per-rock ore blocks.
2. On-demand, non-persisted rock queries, which force persisting the simulation.
3. TFC-native terrain features that are coupled to `TFCChunkGenerator` internals: river carving, volcano centered features, biome heightmaps, the surface-builder helper arrays.
4. The fragility of mixins into non-API internals.

TFC is EUPL-1.2, and existing MIT-licensed addons depend on and mix into it.

### Cited Findings
- `TFCChunkGenerator.SEA_LEVEL_Y = 63` is referenced across client colours, climate (`OverworldClimateModel.SEA_LEVEL`), biome noise, shore and aquifer code. A grep finds 168 `SEA_LEVEL_Y`/`getSeaLevel()` references in TFC sources. — [TFCChunkGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/TFCChunkGenerator.java); [OverworldClimateModel.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java)
- Latitude is fixed at `getLatitude = triangle(−π/2, 0, 1/(4s), z − s/2)` (static, client-side), and the TFC seasonal term is likewise a function of z. — [SolarCalculator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/client/overworld/SolarCalculator.java)
- `RockData.getRock` recomputes rock from the generator at query time, and no rock data is saved in chunk NBT. — [RockData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RockData.java); [ChunkData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ChunkData.java)
- `RegionGenerator` is `final` and its tasks are a fixed enum. The `TFCChunkGenerator` codec guards that the biome source is a `BiomeSourceExtension`, and its `initRandomState` always builds a TFC `RegionGenerator` and takes `chunkDataGenerator` from it. — [RegionGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java); [TFCChunkGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/TFCChunkGenerator.java)
- `BiomeSourceExtension.getPartition` must return TFC `RegionPartition.Point` (river edges). The default `initRandomState` takes a TFC `RegionGenerator`. — [BiomeSourceExtension.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/BiomeSourceExtension.java)
- `ClimateModel` ranges: rainfall "must be in the range [0, 500]"; variance [−1, 1]; groundwater [0, 500]. — [ClimateModel.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/climate/ClimateModel.java)
- `ForestType` is a Java enum, serialized by ordinal byte in ChunkData NBT. — [ForestType.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ForestType.java); [ChunkData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ChunkData.java)
- `TFCChunkGenerator.getBaseColumn` returns an empty `NoiseColumn`, and `buildSurface` only does bedrock. These are TFC's own choices; a custom generator can do better for structure compatibility. — [TFCChunkGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/TFCChunkGenerator.java)
- Spawn: `ForgeEventHandler.onCreateWorldSpawn` handles spawn placement for **any** `ChunkGeneratorExtension`, using `extension.findSpawnBiome(random)`. — [ForgeEventHandler.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/ForgeEventHandler.java)
- Default preset: TFC's `WorldPresetsMixin` forces the preset named in TFC config `defaultWorldPreset` (default `tfc:overworld`) as the vanilla "normal" world. — [WorldPresetsMixin.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/mixin/WorldPresetsMixin.java); [CommonConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/config/CommonConfig.java)
- Thread-safety: TFC documents that `RegionGenerator` must support concurrent access (FastConcurrentCache, ThreadLocal areas, stateless noise). Structure placement calls `createAndGenerate(chunkPos)` for arbitrary chunks. — [RegionGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java); [ClimateStructurePlacement.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/structure/ClimateStructurePlacement.java)
- API churn examples: 4.0.17-beta broke addons using `tfc:biome` placement, and 4.2.4 broke tree addons. — [v4.0.17-beta](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.17-beta); [v4.2.4](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.4)
- **License text** (LICENSE.txt, EUPL v1.2):
  - Definition of Derivative Works: "This Licence does not define the extent of modification or dependence on the Original Work required in order to classify a work as a Derivative Work; this extent is determined by copyright law applicable in the country mentioned in Article 15."
  - Copyleft clause: distribution of the Original or Derivative Works must be "under the terms of this Licence or of a later version".
  - Compatibility clause and appendix: Derivative Works combined with works under a Compatible Licence may be distributed under that licence. The list covers GPL v2/v3, AGPL v3, OSL 2.1/3.0, EPL 1.0, CeCILL 2.0/2.1, MPL 2, LGPL 2.1/3, CC BY-SA 3.0 (non-software), EUPL 1.1/1.2, and LiLiQ-R/R+.
  - Source: [LICENSE.txt](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/LICENSE.txt)
- The README says "Licensed under the EUPL, Version 1.2 unless otherwise noted". `FastNoiseLite.java` is MIT, and parts come from the original 1.7.10 TFC "under a different license with permission". — [README.md](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/README.md)
- Addon licensing in practice:
  - KubeJS-TFC: MIT. It implements TFC interfaces and uses TFC access transformers.
  - TFC-Real-World: MIT. About 25 mixins into TFC internals.
  - TFCGenViewer and tfc-atlas: MIT.
  - TFCBiomeExtender: EUPL-1.2.
  - TerraFirmaGreg Core-Modern: LGPL-3.0 (an EUPL-compatible licence). It ships modified copies of TFC classes.
  - Source: GitHub repo metadata for [KubeJS-TFC](https://github.com/Notenoughmail/KubeJS-TFC), [TFC-Real-World](https://github.com/yazloysasha/TFC-Real-World), [TFCGenViewer](https://github.com/Notenoughmail/TFCGenViewer), [tfc-atlas](https://github.com/RyanSeacroft/tfc-atlas), [TFCBiomeExtender](https://github.com/Notenoughmail/TFCBiomeExtender), [Core-Modern](https://github.com/TerraFirmaGreg-Team/Core-Modern)

### Inferences
Concrete gap list for "plate-tectonics mod + TFC by default":
1. **Must provide.**
   - `ChunkGeneratorExtension` (settings incl. a valid `rock_layer_settings`, `chunkDataGenerator`, `getOrCreateAquifer`, `initRandomState`, `findSpawnBiome`, `applySettings`).
   - A `ChunkDataGenerator` with climate corners, forest type, surface and aquifer heights, and `generateRock`.
   - Biomes that each have a `BiomeExtension`.
   - Sea level 63.
   - TFC raw-rock blocks as the base stone, so veins, collapse and prospecting work.
   - TFC `salt_water` in seas and fresh `water` in lakes and rivers. TFC's `ChunkBaseBlockSource` picks `TFCBlocks.SALT_WATER` versus vanilla water from the biome's `isSalty()` flag and the blending weight ([ChunkBaseBlockSource.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/ChunkBaseBlockSource.java)).
   - A persisted simulation, so `generateRock` and climate keep working after reload and for structure placement.
2. **Lacking or lossy.**
   - Simulated stratigraphy must collapse to TFC's 21 rocks (or new rocks without TFC ore variants).
   - Simulated climate must be clamped to TFC ranges and to 16-block bilinear resolution.
   - Vegetation must collapse to the `ForestType` enum.
   - TFC has no notion of ages or deposits in ChunkData: ore genesis must be our own features placing TFC ore blocks.
3. **Hard or coupled.**
   - TFC's river carving and lake/shore blending, centered volcano features (stratovolcano, cinder cone, tuff ring, tuya, atoll), and biome-specific height shapes (karst towers, shilin, cenotes, glacial cirques) live in `TFCChunkGenerator` internals. The mod must reimplement equivalents, or inject data into TFC's partitions and samplers via mixins.
   - Surface builders are reusable through the public `SurfaceManager`, but need TFC-shaped helper arrays.
4. **Latitude and wrap.**
   - TFC assumes an infinite plane with infinitely alternating hemispheres along z and an equator at z = temperature_scale/2.
   - East–west wrap is not a TFC concept, but nothing in TFC's climate depends on x once ChunkData is supplied by us. Wrapping must be implemented by our mod (chunk mirroring or teleport), and is unaffected by TFC.
   - Placing the equator elsewhere needs a mixin on the client `SolarCalculator` (as TFC-Real-World does) or a z offset.
5. **Preset defaulting.** TFC's own mixin forces `tfc:overworld` as the default world type unless the TFC config `defaultWorldPreset` is changed. "Work with TFC by default" therefore means either asking users or packs to set that config to our preset, or overriding it (e.g. by setting the config value programmatically or via a mixin with higher priority). That is a UX and design decision.
6. **Licensing.**
   - Shipping a separate mod that compiles against TFC's API and runs alongside it is common practice in the TFC ecosystem under MIT, as the examples above show.
   - Copying or modifying TFC source (as TFG does) clearly creates EUPL obligations. Compatible copyleft licences (LGPL/GPL/MPL) would be acceptable for such parts.
   - Whether interface-implementation or mixins make a "Derivative Work" is, per EUPL's own text, left to national copyright law. This is not legal advice.

### Gaps
- I found no TFC statement (wiki or Discord) on whether TFC considers `ChunkGeneratorExtension` / `ChunkDataGenerator` stable public API for third-party generators. AGENTS.md only gives the general backward-compatibility policy.
- I did not do a performance assessment. TFC caches 256 regions of about 12 km each. I found no benchmarks for custom `generateRock` call volume. That volume is large: in TFC's own generator, `ChunkBaseBlockSource.getBaseBlock(x, y, z)` returns `rockData.getRock(x, y, z).raw()` for every solid block during fill-noise, mitigated by `ChunkRockDataCache` (verified in [ChunkBaseBlockSource.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/ChunkBaseBlockSource.java)). Our own generator can avoid this by filling columns directly from the simulation, but TFC features still call `generateRock` point-wise.
- I could not check the Discord or wiki for recommended practice on replacing terrain (search budget exhausted).
