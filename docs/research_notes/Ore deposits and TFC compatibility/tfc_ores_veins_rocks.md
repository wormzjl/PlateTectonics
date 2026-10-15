# TerraFirmaCraft 1.21.1 (NeoForge) ore, rock and vein content, with real-deposit mapping

Scope note. All TFC data here was read from the `1.21.x` branch of `TerraFirmaCraft/TerraFirmaCraft` at commit `c3b4d953` (2026-10-14). `gh api .../compare/v4.2.12...1.21.x` reports the branch tip and release tag `v4.2.12` as **identical**, so these notes describe TFC **v4.2.12 for MC 1.21.1**. The build file pins `minecraftVersion = "1.21.1"` and `neoForgeVersion = "21.1.234"` ([build.gradle.kts](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/build.gradle.kts)). Comparisons with 1.20.1 use the `1.20.x` branch (TFC v3.x, `minecraftVersion = "1.20.1"`), and each one is labelled "1.20.1".

Shorthand used in the tables below (from `expand_rocks` categories in [constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)):
- **IE** (igneous extrusive) = rhyolite, basalt, andesite, dacite
- **II** (igneous intrusive) = granite, diorite, gabbro
- **SED** (sedimentary) = shale, claystone, limestone, conglomerate, dolomite, chert, chalk, tuff
- **MM** (metamorphic) = quartzite, slate, phyllite, schist, gneiss, marble

Grade weights (poor/normal/rich): **POOR = 70/25/5**, **NORMAL = 35/40/25**, **RICH = 15/25/60** ([constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)).

---

## Q1. Complete list of TFC 1.21.1 ores, minerals and gems, what each is used for, and 1.20.1 to 1.21.1 changes

### Takeaway
TFC 1.21.1 has exactly 33 ore entries, in five types:
- 12 graded metal ores
- 4 plain minerals
- 4 minerals that also have a powder
- 9 gems
- 3 "item-only" minerals (bituminous coal, lignite, halite). These now generate as single rock-independent blocks.

TFC has no lead/galena, chalcopyrite, chromite, PGE, tungsten, molybdenum, uranium, manganese, aluminium or lithium ore. Cinnabar **and** cryolite both grind into redstone.

### Cited Findings
- The `Ore` enum lists the following ([Ore.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Ore.java)):
  - **GRADED:** NATIVE_COPPER, NATIVE_GOLD, HEMATITE, NATIVE_SILVER, CASSITERITE, BISMUTHINITE, GARNIERITE, MALACHITE, MAGNETITE, LIMONITE, SPHALERITE, TETRAHEDRITE
  - **NORMAL:** GYPSUM, CINNABAR, CRYOLITE, BORAX
  - **NORMAL_WITH_POWDER:** GRAPHITE, SALTPETER, SULFUR, SYLVITE
  - **GEM:** AMETHYST, DIAMOND, EMERALD, LAPIS_LAZULI, OPAL, PYRITE, RUBY, SAPPHIRE, TOPAZ
  - **ITEM_ONLY:** BITUMINOUS_COAL, LIGNITE, HALITE. `hasBlock()` is false for ITEM_ONLY, so these have no per-rock ore block.
- `Ore.metal()` maps ores to metals as follows ([Ore.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Ore.java)):
  - native copper, malachite, tetrahedrite → COPPER
  - native gold → GOLD
  - hematite, magnetite, limonite → CAST_IRON
  - native silver → SILVER
  - cassiterite → TIN
  - bismuthinite → BISMUTH
  - garnierite → NICKEL
  - sphalerite → ZINC
- Melt values. Small/poor/normal/rich hematite melt to 10/15/25/35 mB of `tfc:metal/cast_iron` at 1535 °C. Poor/normal/rich native copper give 15/25/35 mB of `tfc:metal/copper` at 1080 °C ([heating/ore/normal_hematite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/heating/ore/normal_hematite.json); sibling files `poor_`, `rich_`, `small_hematite.json`, `*_native_copper.json`).
- The `Metal` enum contains: bismuth, bismuth_bronze, black_bronze, bronze, brass, copper, gold, nickel, rose_gold, silver, tin, zinc, sterling_silver, wrought_iron, cast_iron, pig_iron, steel, black_steel, blue_steel, red_steel, weak_steel, weak_blue_steel, weak_red_steel, high_carbon_steel, high_carbon_black_steel, high_carbon_blue_steel, high_carbon_red_steel, unknown ([Metal.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/Metal.java)).
- Alloy recipes ([constants.py ALLOYS](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py); generated as [recipe/alloy/*.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/generated/resources/data/tfc/recipe/alloy)):
  - bronze = Cu 88–92 % + Sn 8–12 %
  - bismuth bronze = Zn 20–30 %, Cu 50–65 %, Bi 10–20 %
  - black bronze = Cu 50–70 %, Ag 10–25 %, Au 10–25 %
  - brass = Cu 88–92 %, Zn 8–12 %
  - rose gold = Cu 15–30 %, Au 70–85 %
  - sterling silver = Cu 20–40 %, Ag 60–80 %
  - weak steel = steel 50–70 %, Ni 15–25 %, black bronze 15–25 %
  - weak blue steel = black steel 50–55 %, steel 20–25 %, bismuth bronze 10–15 %, sterling silver 10–15 %
  - weak red steel = black steel 50–55 %, steel 20–25 %, brass 10–15 %, rose gold 10–15 %
- Pig iron is made in the blast furnace from `tfc:metal/cast_iron` with `tfc:powder/flux` as catalyst ([blast_furnace/pig_iron.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/blast_furnace/pig_iron.json)).
- Flux sources:
  - Quern: any `#tfc:fluxstone` gives 2 flux; borax ore gives 6 flux ([quern/powder/flux.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/quern/powder/flux.json), [quern/powder/flux_from_borax.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/quern/powder/flux_from_borax.json)).
  - `#tfc:fluxstone` contains loose/mossy-loose limestone, dolomite, chalk and marble, plus shells, scutes and mussels ([tags/item/fluxstone.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/tags/item/fluxstone.json)).
- Uses of the non-metal minerals:
  - **Redstone:** cinnabar **or** cryolite → 8 `minecraft:redstone` in the quern ([quern/redstone.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/quern/redstone.json)).
  - **Graphite:** 4 graphite powder; used in fire clay (4 kaolinite powder + 4 graphite powder + 1 clay ball), gunpowder_with_graphite and blue dye ([quern/powder/graphite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/quern/powder/graphite.json), [crafting/fire_clay.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/crafting/fire_clay.json)).
  - **Saltpeter powder:** gunpowder; fertilizer N 0.1 / K 0.4 ([fertilizer/powder/saltpeter.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/tfc/fertilizer/powder/saltpeter.json)).
  - **Sylvite powder:** fertilizer K 0.5; orange dye ([fertilizer/powder/sylvite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/tfc/fertilizer/powder/sylvite.json)).
  - **Sulfur powder:** gunpowder.
  - **Halite:** 4 salt powder, used for salting food and salt water ([quern/powder/salt.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/quern/powder/salt.json)).
  - **Gypsum:** alabaster (barrel) and alabaster bricks.
  - **Pyrite:** flint-and-pyrite.
  - **Lapis:** blue dye.
  - **Kaolin clay:** heats into kaolinite powder ([recipe directory](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/generated/resources/data/tfc/recipe)).
- Fuels:
  - `tfc:ore/bituminous_coal` is fuel at 1415 °C for 2000 ticks, the same entry as `minecraft:coal` ([fuel/coal.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/tfc/fuel/coal.json)).
  - `tfc:ore/lignite` is fuel at 1350 °C for 2200 ticks ([fuel/ore/lignite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/tfc/fuel/ore/lignite.json)).
- Gems: the quern has powder recipes for amethyst, diamond, emerald, lapis, opal, pyrite, ruby, sapphire and topaz ([quern/powder/](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/generated/resources/data/tfc/recipe/quern/powder)). All 9 gems are armor-trim materials (`TRIM_MATERIALS = (*GEMS, ...)`) ([constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)).
- Data quirk: a quern recipe turns `tfc:rock/raw/limestone` into `tfc:ore/gypsum` ([quern/ore/gypsum.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/recipe/quern/ore/gypsum.json)).
- **Change, 1.21.1 vs 1.20.1 (ore blocks):** coal, lignite and halite are now plain blocks `tfc:halite`, `tfc:lignite` and `tfc:bituminous_coal` ([TFCBlocks.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/TFCBlocks.java); `simple_blocks=True` in [constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)). In 1.20.1 these were per-rock, e.g. `tfc:ore/halite/shale` ([1.20.x vein/halite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/halite.json)).
- **Change, 1.21.1 vs 1.20.1 (tool tiers):**
  - In 1.21.1 every ore in `ORES` has required tier 'copper' ([constants.py 1.21.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)). The v4.2.7 notes say "Removed tool tier requirements for mining TFC ores and minerals" ([v4.2.7 release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.7)).
  - In 1.20.1 the tiers were ([constants.py 1.20.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/resources/constants.py)):
    - bronze: garnierite, cinnabar, cryolite, halite
    - wrought iron: lapis, opal
    - steel: amethyst, emerald, topaz
    - black steel: diamond, ruby, sapphire
- No `GALENA`/lead, chromite, PGE, wolframite, molybdenite, uraninite, manganese, bauxite, lithium or REE entry exists in the `Ore` enum, and no lead metal exists in `Metal` ([Ore.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Ore.java), [Metal.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/Metal.java)).

### Inferences
- A geology-driven generator must produce 12 graded metal ores, 8 per-rock non-metal minerals, 9 per-rock gems and 3 simple blocks. It also needs 4 placer "deposit" ores (see Q2/Q6).
- Ore-to-metal coverage is redundant in places:
  - Copper has three sources: native copper, malachite, tetrahedrite.
  - Cast iron has three sources: hematite, magnetite, limonite.
  - Redstone has two sources: cinnabar, cryolite.
  - Each of tin (cassiterite), bismuth, zinc, nickel, gold and silver has exactly one ore.
- Progression gates on availability. Bronze needs cassiterite. Bismuth bronze needs sphalerite + bismuthinite. Black bronze needs Au + Ag. Black steel needs garnierite (Ni) + black bronze. Blue/red steel need sterling silver, bismuth bronze, brass and rose gold. A realistic generator that makes any of these ores very rare would break TFC progression.
- Because 1.21.1 dropped tool-tier gates on mining, deep or rare placement only affects discovery effort, not tier gating.

### Gaps
- I did not verify the heating mB for every graded ore. Only hematite and native copper were opened; the others are assumed to follow the same 10/15/25/35 pattern.
- I did not open the gunpowder, orange dye or alabaster recipes. Their ingredients were confirmed only by file references (grep).

---

## Q2. Rock types, categories, per-rock block variants, and whether ores are rock-specific blocks

### Takeaway
TFC 1.21.1 has **21** rocks. **Tuff** is new versus 1.20.1's 20. It is classed as sedimentary and is not part of the default layer stack. Ores are rock-specific blocks (`tfc:ore/<grade>_<ore>/<rock>` or `tfc:ore/<ore>/<rock>`). Sand is no longer per-rock: there are 7 sand colours, mapped from rocks.

### Cited Findings
- `Rock` enum with display categories ([Rock.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Rock.java)):

  | Display category | Rocks |
  |---|---|
  | felsic igneous intrusive | granite |
  | intermediate igneous intrusive | diorite |
  | mafic igneous intrusive | gabbro |
  | sedimentary | shale, claystone, limestone, conglomerate, dolomite, chert, chalk, **tuff** |
  | felsic igneous extrusive | rhyolite |
  | mafic igneous extrusive | basalt |
  | intermediate igneous extrusive | andesite, dacite |
  | metamorphic | quartzite, slate, phyllite, schist, gneiss, marble |

- `RockCategory` hardness modifiers are IGNEOUS_EXTRUSIVE 0, IGNEOUS_INTRUSIVE +0.2, METAMORPHIC −0.2, SEDIMENTARY −0.4. Each category also sets a stone-tool tier ([RockCategory.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/RockCategory.java), [RockDisplayCategory.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/RockDisplayCategory.java)).
- 1.20.1 had the same list without tuff (20 rocks) ([constants.py 1.20.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/resources/constants.py)).
- Per-rock block types `Rock.BlockType` are: RAW, HARDENED, SMOOTH, COBBLE, BRICKS, GRAVEL, SPIKE, CRACKED_BRICKS, MOSSY_BRICKS, MOSSY_COBBLE, CHISELED, LOOSE, MOSSY_LOOSE, PRESSURE_PLATE, BUTTON, AQUEDUCT, ROPE_ANCHOR. Registry IDs are `tfc:rock/<type>/<rock>` ([Rock.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Rock.java), [TFCBlocks.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/TFCBlocks.java)).
  - Types flagged `variants=true` also get slab/stair/wall: raw, smooth, cobble, bricks, cracked/mossy bricks, mossy cobble.
  - Igneous rocks can become `tfc:rock/anvil/<rock>`.
  - RAW hardness is 6.5 + modifier; HARDENED is 8 + modifier.
- Sand colours are brown, white, black, red, yellow, green, pink ([SandBlockType.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/soil/SandBlockType.java)). Per-rock `rock_settings` data maps each rock to a sand/sandstone colour and flags `karst`/`mafic` ([rock_settings/](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/generated/resources/data/tfc/tfc/worldgen/rock_settings)):

  | Sand colour | Rocks |
  |---|---|
  | yellow | granite, gneiss, phyllite, rhyolite, schist, slate |
  | red | diorite, andesite, chert, dacite |
  | black | gabbro, basalt (both `mafic: true`) |
  | brown | shale, claystone, conglomerate |
  | white | limestone, dolomite, chalk, marble (all `karst: true`), quartzite |
  | green | tuff |

- The v4.0.0 notes say "Beach sand is no longer tied to rock layers, but varies randomly or with climate" ([v4.0.0-beta release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta)).
- `RockSettings` codec fields are `raw`, `hardened`, `gravel`, `cobble`, `sand`, `sandstone` (required) and `spike`, `loose`, `mossy_loose`, `karst`, `mafic` (optional). The registry is `tfc:worldgen/rock_settings` ([RockSettings.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/settings/RockSettings.java)).
- Ore block registration ([TFCBlocks.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/TFCBlocks.java)):
  - Non-graded with block: `ore/<ore>/<rock>`
  - Graded: `ore/<poor|normal|rich>_<ore>/<rock>`
  - Small (graded only): `ore/small_<ore>`, a groundcover "loose ore"
  - `deposit/<ore>/<rock>` for the `OreDeposit` enum: NATIVE_COPPER, NATIVE_SILVER, NATIVE_GOLD, CASSITERITE ([OreDeposit.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/OreDeposit.java))
  - Hardness: ore blocks use the host rock's raw hardness (6.5 + modifier). Deposits use gravel hardness (2.0 + modifier) and gravel sound.
  - Example: `tfc:ore/normal_hematite/granite` exists.

### Inferences
- Block counts, computed from the enums:
  - 21 rocks × (12 graded × 3 + 17 non-graded/gem) = **1,113 per-rock ore blocks**
  - 84 deposit blocks
  - 12 small-ore blocks
  - 3 simple blocks
- The PlateTectonics mod must know the rock at each placement point to pick the right block ID. The simplest compatible approach is to emit TFC-style `replace` → `with` maps keyed by raw rock block (see Q3).
- Tuff is a sedimentary-category rock in TFC, though geologically it is volcaniclastic. A geology-based mod should treat `tfc:rock/raw/tuff` as a pyroclastic deposit for its own logic. TFC veins that list SED will still replace it.

### Gaps
- I did not enumerate every slab/stair/wall ID or the sandstone variant names (`raw_sandstone/<colour>` was seen in rock_settings).

---

## Q3. Vein feature types, JSON format, file locations, and the full default vein list

### Takeaway
TFC 1.21.1 registers four vein feature types: `tfc:cluster_vein`, `tfc:disc_vein`, `tfc:pipe_vein` and `tfc:kaolin_disc_vein`. They share a common `VeinConfig`. The 1.20.1 `biomes` field is gone; biome restriction is now done by placed-feature **tags**. There are 52 configured veins under `data/tfc/worldgen/configured_feature/vein/` (branch `1.21.x`). Each has a matching placed feature under `placed_feature/vein/`, whose `placement` is empty except for kaolin.

### Cited Findings
- **Feature IDs:** `cluster_vein`, `disc_vein`, `kaolin_disc_vein` (uses the `DiscVeinConfig` codec) and `pipe_vein` ([TFCFeatures.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/TFCFeatures.java)).
- **Common fields** (`VeinConfig`, [VeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinConfig.java)):
  - `blocks` (required): list of `{replace: [block ids], with: [{block, weight}]}`
  - `indicator` (optional)
  - `rarity` (positive int, required)
  - `density` (0–1 float, required)
  - `min_y`, `max_y` (int, required)
  - `project` (bool, default false)
  - `project_offset` (bool, default false)
  - `random_name` (string hashed to a seed, or a long; required)
  - `near_lava` (bool, default false)
- **Change vs 1.20.1:** the 1.20.1 codec also had optional `biomes` (biome tag). In 1.20.1 amethyst and opal used `biomes: #tfc:is_river` ([1.20.x VeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinConfig.java), [constants.py 1.20.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/resources/constants.py)).
- **Per-type fields:**
  - Cluster adds `size`; it uses 3D metaballs ([ClusterVeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/ClusterVeinConfig.java), [ClusterVeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/ClusterVeinFeature.java)).
  - Disc adds `size` (horizontal) and `height` (half-thickness); it uses 2D metaballs with density tapering near edges ([DiscVeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/DiscVeinConfig.java), [DiscVeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/DiscVeinFeature.java)).
  - Pipe adds `height`, `radius`, `min_skew`, `max_skew`, `min_slant`, `max_slant` and `sign` (0–1 float). It is a skewed, tapering cylinder with noisy edges ([PipeVeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/PipeVeinConfig.java), [PipeVeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/PipeVeinFeature.java)).
- **Kaolin disc** ignores `blocks` ([KaolinDiscVeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/KaolinDiscVeinFeature.java)):
  - Grass becomes `kaolin_clay_grass`.
  - Blocks in `#tfc:kaolin_clay_replaceable` become red kaolin (y > −2), pink (y > −4) or white kaolin, with y relative to the vein centre.
- **Indicator fields:** `depth`, `rarity` (0 = no surface indicator), `underground_rarity`, `underground_count`, `blocks` (weighted `{block}` list). The kaolin JSON also has a `spread` key that is not in the codec ([Indicator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/Indicator.java), [vein/kaolin_disc.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/kaolin_disc.json)).
- **Sample graded vein JSON** ([vein/montane_native_copper.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/montane_native_copper.json)):
  - `"type":"tfc:cluster_vein"`, `rarity 16`, `density 0.25`, `min_y 100`, `max_y 300`, `size 20`, `random_name "montane_native_copper"`
  - One `blocks` entry per IE rock, e.g. `replace ["tfc:rock/raw/basalt"]` → `with` poor 70 / normal 25 / rich 5 / `tfc:deposit/native_copper/basalt` 10
  - `indicator {rarity 14, depth 35, underground_rarity 1, underground_count 0, blocks [{block: "tfc:ore/small_native_copper"}]}`
  - The placed feature is `{"feature":"tfc:vein/montane_native_copper","placement":[]}` ([placed_feature/vein/montane_native_copper.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/placed_feature/vein/montane_native_copper.json)).
- **Where the data comes from:** all vein JSON is generated by `resources/world_gen.py` from the `ORE_VEINS` dict in `resources/constants.py` ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py), [constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)). The JSON is checked in at [src/main/resources/data/tfc/worldgen/configured_feature/vein/](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein). The Python generator chooses `with` blocks three ways:
  - Graded: weighted poor/normal/rich, plus a weight-10 deposit block if `deposits=True`.
  - Mineral/gem: a single `tfc:ore/<ore>/<rock>`.
  - simple_blocks: `tfc:<ore>` for every rock.
- **Biome assignment is by tags** ([in_biome/veins.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins.json), [veins/montane.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins/montane.json), [veins/river.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins/river.json), [veins/kaolin.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins/kaolin.json), [veins/collisional_mountains.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins/collisional_mountains.json)):
  - `#tfc:in_biome/veins` (used by all 125 biome JSONs) contains gravel, the 3 dikes, every non-river ore vein (montane ones included) and `tfc:geode`.
  - `veins/montane` (44 biomes) contains the 11 `montane_*` veins.
  - `veins/river` (7 biomes: lake, meltwater_lake, oceanic_mountain_lake, rift_lake, river, tower_karst_lake, volcanic_oceanic_mountain_lake) contains amethyst and opal.
  - `veins/kaolin` (29 biomes) contains kaolin_disc.
  - `veins/collisional_mountains` (only the `collisional_mountains` biome) contains ruby_marble_belt.
- In a biome JSON these are separate feature-step entries. Example from `mountains.json`: index 6 `#tfc:in_biome/veins`, index 7 `#tfc:in_biome/veins/montane` ([biome/mountains.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/biome/mountains.json)).
- **Full default vein list in 1.21.1** (from [vein/*.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein) and [constants.py ORE_VEINS](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)). "Ind" = surface indicator rarity / underground indicator count. Rarity is 1-in-N chunks.

| Vein | Type | Rarity | Dens. | Size (disc: size/height; pipe: height/radius) | y range | Host rocks | Output | Ind | Tag |
|---|---|---|---|---|---|---|---|---|---|
| surface_native_copper | cluster | 36 | 0.25 | 20 | 40..100 | IE | POOR + deposit(10) | 14/0 | veins |
| montane_native_copper | cluster | 16 | 0.25 | 20 | 100..300 | IE | POOR + deposit(10) | 14/0 | veins+montane |
| surface_malachite | cluster | 48 | 0.25 | 20 | 40..100 | marble, limestone, chalk, dolomite | POOR | 14/0 | veins |
| montane_malachite | cluster | 11 | 0.25 | 20 | 40..300 | same | POOR | 14/0 | veins+montane |
| normal_malachite | cluster | 45 | 0.5 | 30 | −30..70 | same | NORMAL | 25/0 | veins |
| montane_tetrahedrite | cluster | 3 | 0.25 | 20 | 90..270 | MM | POOR | 8/0 | veins+montane |
| normal_tetrahedrite | cluster | 40 | 0.5 | 30 | −30..70 | MM | NORMAL | 25/0 | veins |
| normal_native_gold | cluster | 90 | 0.25 | 15 | 0..70 | IE+II | NORMAL | 40/0 | veins |
| rich_native_gold | cluster | 50 | 0.5 | 40 | −80..20 | II | RICH | 0/4 | veins |
| fake_native_gold | cluster | 16 | 0.35 | 15 | −50..70 | IE+II | pyrite (no gold blocks in data) | none | veins |
| montane_native_silver | cluster | 7 | 0.2 | 10 | 90..280 | granite, diorite | POOR | 12/0 | veins+montane |
| normal_native_silver | cluster | 25 | 0.6 | 25 | −80..20 | granite, diorite, gneiss, schist | RICH | 0/9 | veins |
| montane_cassiterite | cluster | 2 | 0.4 | 15 | 80..300 | II | NORMAL + deposit(10) | 12/0 | veins+montane |
| surface_bismuthinite | cluster | 48 | 0.3 | 20 | 40..100 | SED | POOR | 14/0 | veins |
| montane_bismuthinite | cluster | 24 | 0.3 | 20 | 100..220 | SED | POOR | 14/0 | veins+montane |
| normal_bismuthinite | cluster | 45 | 0.6 | 40 | −80..20 | II | RICH | 0/4 | veins |
| surface_sphalerite | cluster | 40 | 0.3 | 20 | 40..100 | IE | POOR | 12/0 | veins |
| montane_sphalerite | cluster | 20 | 0.3 | 20 | 100..220 | IE | POOR | 12/0 | veins+montane |
| normal_sphalerite | cluster | 45 | 0.6 | 40 | −80..20 | II | RICH | 0/5 | veins |
| surface_hematite | cluster | 45 | 0.4 | 20 | 10..90 | IE | NORMAL | 24/0 | veins |
| surface_magnetite | cluster | 90 | 0.4 | 20 | 10..90 | SED | NORMAL | 24/0 | veins |
| surface_limonite | cluster | 90 | 0.4 | 20 | 10..90 | SED | NORMAL | 24/0 | veins |
| montane_hematite | cluster | 25 | 0.4 | 20 | 90..250 | IE | NORMAL | 24/0 | veins+montane |
| montane_magnetite | cluster | 45 | 0.4 | 20 | 90..250 | SED | NORMAL | 24/0 | veins+montane |
| montane_limonite | cluster | 45 | 0.4 | 20 | 90..250 | SED | NORMAL | 24/0 | veins+montane |
| normal_garnierite | cluster | 25 | 0.3 | 18 | −80..0 | II | NORMAL | 12/0 | veins |
| gabbro_garnierite | cluster | 20 | 0.6 | 30 | −80..0 | gabbro | RICH | 0/7 | veins |
| graphite | cluster | 20 | 0.4 | 20 | −30..60 | gneiss, marble, quartzite, schist | graphite | none | veins |
| lignite | disc | 160 | 0.85 | 40/2 | −20..−8, project + offset | SED | `tfc:lignite` | none | veins |
| bituminous_coal | disc | 210 | 0.9 | 50/3 | −35..−12, project + offset | SED | `tfc:bituminous_coal` | none | veins |
| halite | disc | 110 | 0.85 | 35/4 | −45..−12, project + offset | SED | `tfc:halite` | none | veins |
| sulfur | disc | 4 | 0.25 | 18/5 | −64..−45, near_lava | II+MM | sulfur | none | veins |
| tuff_sulfur | disc | 2 | 0.45 | 18/4 | 40..200 | tuff | sulfur | none | veins |
| cryolite | cluster | 16 | 0.7 | 18 | −70..−10 | granite, diorite | cryolite | none | veins |
| normal_cinnabar | cluster | 14 | 0.6 | 18 | −70..10 | quartzite, phyllite, gneiss, schist | cinnabar | none | veins |
| montane_cinnabar | cluster | 14 | 0.6 | 14 | 120..280 | same | cinnabar | none | veins+montane |
| saltpeter | disc | 110 | 0.4 | 35/5 | 40..100 | SED | saltpeter | none | veins |
| sylvite | disc | 60 | 0.35 | 35/5 | 40..100 | shale, claystone, chert | sylvite | none | veins |
| borax | disc | 40 | 0.2 | 23/3 | 40..100 | claystone, limestone, shale | borax | none | veins |
| gypsum | disc | 70 | 0.3 | 25/5 | 40..100 | SED | gypsum | none | veins |
| lapis_lazuli | cluster | 30 | 0.12 | 30 | −20..80 | limestone, marble | lapis | none | veins |
| diamond | pipe | 30 | 0.15 | 60/5 (skew 5–13, slant 0–2, sign 0) | −64..100 | gabbro | diamond | none | veins |
| emerald | pipe | 80 | 0.15 | 60/5 (same) | −64..100 | II | emerald | none | veins |
| amethyst | disc | 25 | 0.2 | 8/4 | 40..60 | SED+MM | amethyst | none | river |
| opal | disc | 25 | 0.2 | 8/4 | 40..60 | SED+IE | opal | none | river |
| deep_ruby | cluster | 80 | 0.2 | 22 | −70..−10 | marble | ruby | none | veins |
| ruby_marble_belt | disc | 16 | 1.0 | 50/5 | −40..−4, project + offset | IE+II+MM | 94 raw marble / 6 `ore/ruby/marble` | none | collisional_mountains |
| gravel | disc | 30 | 0.98 | 44/2 | −64..100 | all 21 raw rocks | same-rock gravel | none | veins |
| granite_dike / diorite_dike / gabbro_dike | pipe | 300 | 0.98 | 150/18 (skew 7–20, slant 2–5) | −64..180 | all raw + gravel → raw intrusive; hardened → hardened intrusive | dike rock | none | veins |
| kaolin_disc | kaolin_disc_vein | 50 | 1.0 | 18/6 | −7..1, project | grass/soil (tag) | red/pink/white kaolin | blood lily, rarity 10 | kaolin; placed with `tfc:climate min_temperature 18, min_groundwater 300` |

- **Kaolin placement filter:** ([placed_feature/vein/kaolin_disc.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/placed_feature/vein/kaolin_disc.json)).
- **Vein list changes vs 1.20.1:**
  - 1.20.1 had 41 vein files. It had `surface_tetrahedrite`, `surface_native_silver`, `surface_cassiterite` and a single `cinnabar`, and no montane, tuff, ruby or 1.21-style deposit changes ([1.20.x vein dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.20.x/src/main/resources/data/tfc/worldgen/configured_feature/vein)).
  - The 1.20.1 surface veins reached max_y 130 versus 100 in 1.21.1 ([constants.py 1.20.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/resources/constants.py)).
  - The v4.2.0 notes say: "Allow cinnabar to generate in high-elevation rocks", "Extend the vertical ranges of several other ores to match new mountain heights", "Remove deep ruby ore, and move it to collisional mountains" ([v4.2.0 release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.0)).
  - Discrepancy: `tfc:vein/deep_ruby` is **still listed** in `#tfc:in_biome/veins` at v4.2.12 ([in_biome/veins.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins.json)).
- **Design rationale in TFC's own comments** ([constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)):
  - Native copper is "only in IE, only surface and montane".
  - Coal spawns "roughly based on IRL grade (lignite -> bituminous -> anthracite)".
  - The ruby marble belt is "Inspired by the fact that IRL rubies are mostly from marble belts in the Himalayas" ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py)).

### Inferences
- Simplest TFC-compatible output format for a geology mod: write its own `configured_feature` JSON using the four `tfc:*_vein` types. Rock-specific `replace`/`with` maps are needed because TFC veins only swap exact raw-rock blocks.
- Add the mod's veins to `#tfc:in_biome/veins` (or a new tag referenced by biomes). Remove TFC defaults by overriding `in_biome/veins*.json` with `"replace": true` in a datapack. Disabling only some ores probably needs the same tag override.
- Alternatively, write blocks directly from Java. In that case the mod must reproduce the block-ID conventions from Q2.
- `project=true` makes `min_y`/`max_y` effectively **depth below the local ocean-floor heightmap** (see Q5). Coal, halite and ruby belts are therefore surface-following, which is a convenient shape for stratiform (bedded) deposits.

### Gaps
- I did not verify whether listing montane veins in both `veins` and `veins/montane` makes them place twice (effectively doubled density) in montane biomes.
- I did not verify how the weighted codec treats a missing `weight` (single-entry lists omit it).

---

## Q4. Indicators and prospecting: surface nuggets, loose rocks, the prospector's pick, and the data they rely on

### Takeaway
Surface indicators are the `tfc:ore/small_<ore>` groundcover nuggets, placed by the vein feature itself. They exist for graded metal ores only; kaolin uses blood lilies, clay uses indicator plants, and loose rocks show only the surface rock type. The prospector's pick scans a 25×25×25 cube for blocks in `#tfc:prospectable` (= `#c:ores`).

### Cited Findings
- `SMALL_ORES` are registered only for graded ores as `ore/small_<ore>` via `GroundcoverBlock.looseOre` ([TFCBlocks.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/TFCBlocks.java)).
- Indicator placement in `VeinFeature.place` ([VeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinFeature.java)), for each column where ore was placed:
  - **Surface:** with probability 1/`rarity` (skipped if rarity 0), pick a random x/z within about ±14 blocks. Use the OCEAN_FLOOR_WG height there. Place the indicator only if |surfaceY − highest ore Y| < `depth`, the spot is worldgen-replaceable, and the indicator can survive there.
  - **Underground:** `underground_count` attempts, each with chance 1/`underground_rarity`. The y is biased upward from the vein's y range (+0..31, −0..7), and must be at least 5 blocks below the ground surface.
- In default data all graded veins use `depth 35`. Deep rich veins (rich gold, normal bismuthinite/sphalerite/silver, gabbro garnierite) have surface rarity 0 and 4–9 underground nuggets ([vein/*.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein)).
- Loose rocks: `LooseRockFeature` places `loose`/`mossy_loose` of the **surface** rock (`getRockData().getSurfaceRock(x,z)`). Mossy variants appear when rainfall > 250 ([LooseRockFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/LooseRockFeature.java)).
- Plant indicators:
  - Kaolin's indicator block is `tfc:plant/blood_lily` ([vein/kaolin_disc.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/kaolin_disc.json)).
  - Clay discs use `#tfc:feature/clay_indicators`: athyrium fern, canna, goldenrod, pampas grass, perovskia, water canna, rose patches ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py)).
- Prospector's pick ([PropickItem.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/items/PropickItem.java)):
  - `RADIUS = 12`, `COOLDOWN = 10` ticks.
  - False-negative chance = 0.3 − tier×0.06 (tier clamped 0–5), seeded per clicked position.
  - Clicking a prospectable block gives FOUND.
  - Otherwise it counts prospectable blocks in the cube, merging poor/rich grades into the "normal" representative block. It reports one random found block type with a size class: <10 TRACES, <20 SMALL, <40 MEDIUM, <80 LARGE, else VERY_LARGE.
  - It fires a NeoForge `ProspectedEvent`. Addons can register representatives via `PropickItem.registerRepresentative`.
- `#tfc:prospectable` = `["#c:ores"]` ([tags/block/prospectable.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/tags/block/prospectable.json)).

### Inferences
- Any custom ore block the mod places is prospectable if it is in `#c:ores`. Reusing TFC's own ore blocks makes prospecting work automatically.
- If the mod writes ore blocks directly instead of using TFC vein features, it must also place `small_<ore>` nuggets itself to keep TFC's early-game discovery loop. The VeinFeature algorithm above can be copied: a surface nugget within about 14 blocks horizontally, and only if the vein top is within 35 blocks of the surface.
- Gossans (limonite caps over sulfide bodies, see Q7) could double as extra indicators: limonite ore or small_limonite above deep sulfide veins.

### Gaps
- I did not check whether non-graded minerals and gems have any indicator. The data shows none in vein JSON, and `SMALL_ORES` covers graded ores only.

---

## Q5. How vein generation interacts with ChunkData and rock layers

### Takeaway
Veins never query ChunkData. They read the block already in the world and look it up in the `blocks` map, so host-rock specificity comes entirely from TFC's rock-layer system. That system builds stacked layers 43–63 blocks thick from data in the world preset (`tfc_settings.rock_layer_settings`).

### Cited Findings
- `IVeinConfig.getStateToGenerate(stoneState, random)` looks up `config.states().get(stoneState.getBlock())`. It returns null (no change) for any block not listed, so hardened rock, gravel or other veins' ores are left untouched unless listed ([IVeinConfig.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/IVeinConfig.java)).
- Vein origins ([VeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinFeature.java)):
  - Each chunk has a vein with probability 1/`rarity`, using `XoroshiroRandomSource(worldSeed ^ chunkX*61728364132L, veinSeed ^ chunkZ*16298364123L)`.
  - Origin is a random x/z in that chunk. y is uniform in [min_y + r, max_y − r], where r = size (cluster/disc) or height (pipe); if that range is empty, y is the midpoint.
  - Each chunk's feature call scans neighbouring chunks within `1 + (size>>4)` and places only the part inside the current chunk, so veins cross chunk borders deterministically.
- `project=true` adds the OCEAN_FLOOR_WG height at (x+offX, z+offZ) to each candidate y. `project_offset` uses a deterministic per-vein ±15 lateral offset. `near_lava=true` skips columns with no lava in a 9×9 area at y = −55 ([VeinFeature.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/feature/vein/VeinFeature.java)).
- Rock lookup: `RockData.getRock(x,y,z)` calls `ChunkDataGenerator.generateRock` with the column surface height ([RockData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RockData.java), [RegionChunkDataGenerator.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/RegionChunkDataGenerator.java)):
  - Surface heights above 125 are compressed: 125 + 0.3·(y−125).
  - It walks down layers whose thickness is OpenSimplex noise scaled 43–63.
  - Each layer is laterally skewed by ±1.8 × depth.
  - It samples a region "rock point", then calls `rockLayerSettings.sampleAtLayer(point, layer)`.
  - ChunkData also stores rainfall, rain variance, base groundwater, temperature, aquifer surface height and forest type ([ChunkData.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/chunkdata/ChunkData.java)).
- Default rock layers from the world preset `tfc:overworld` → `tfc_settings.rock_layer_settings` ([world_preset/overworld.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/worldgen/world_preset/overworld.json); codec in [RockLayerSettings.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/settings/RockLayerSettings.java)).
  - `bottom` = gneiss, schist, diorite, granite, gabbro.
  - Named layer chains (each rock maps to the layer below it):

    | Layer | Rocks → layer below |
    |---|---|
    | felsic | granite→bottom |
    | intermediate | diorite→bottom |
    | mafic | gabbro→bottom |
    | igneous_extrusive | rhyolite→felsic, andesite/dacite→intermediate, basalt→mafic |
    | igneous_extrusive_x2 | all IE→igneous_extrusive |
    | igneous_intrusive | granite→felsic, diorite→intermediate, gabbro→mafic |
    | high_grade | gneiss, schist→bottom |
    | low_grade | phyllite, slate→high_grade |
    | marble | marble→bottom |
    | quartzite | quartzite→bottom |
    | sedimentary | chalk, dolomite, limestone→marble; chert→quartzite; claystone, conglomerate, shale→low_grade |
    | uplift | diorite, gabbro, granite→low_grade; gneiss, schist, phyllite, slate→high_grade; marble, quartzite→bottom |

  - Top-level provinces:

    | Province | Layers |
    |---|---|
    | ocean_floor | [igneous_extrusive] |
    | land | [sedimentary ×3, igneous_extrusive] |
    | volcanic | [igneous_extrusive, igneous_extrusive_x2, igneous_intrusive] |
    | uplift | [uplift ×3, sedimentary] |

  - Tuff is in the `rocks` map but in no layer chain.
- 1.20.1 comparison ([1.20.x world_preset/overworld.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/worldgen/world_preset/overworld.json)): `land` = [igneous_extrusive, sedimentary], `volcanic` = [igneous_extrusive, igneous_extrusive_x2], `uplift` = [sedimentary, uplift]. The 1.20.1 uplift chain put intrusives over sedimentary.
- Tuff placement:
  - Tuff-ring surface builders on dormant, extinct, ancient and sunken shield volcanoes and old shield-volcano shores ([TFCBiomes.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/biome/TFCBiomes.java)).
  - The volcanic badlands palette.
  - Stratovolcanoes "made of layers of igneous extrusive rocks and tuff" ([v4.2.0 release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.0)).
- TFC 4.2.0 already "Reworked large-scale terrain generation to mimic plate tectonics" ([v4.2.0 release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.0)):
  - Continents are bordered by subduction zones, rift valleys, collisional mountain ranges and ocean ridges.
  - Stratovolcanoes occur only at subduction zones.
  - Volcanic island arcs form in subduction zones.
  - Trenches appear only in subduction zones.

### Inferences
- A mod that controls rock layout can do it in one of two ways. Either supply a custom `rock_layer_settings` / world preset, or override the rock decision itself (TFC's region generator). Because veins follow rock automatically, porphyry-Cu-above-granite or skarn-at-marble/granite-contact logic can use TFC veins only if the right raw rocks are actually there.
- For deposit types that depend on contacts (skarn, greisen, contact-metamorphic lapis), TFC's per-block lookup has no contact awareness. The mod would need its own feature that checks neighbouring rock types via `RockData.getRock` or by sampling blocks.
- TFC's own tectonic biome assignment (subduction / collisional / rift / ridge) overlaps heavily with the PlateTectonics mod's goals. The biome tags (`veins/montane`, `veins/collisional_mountains`) are the existing hook points.

### Gaps
- I did not trace how "region rock points" choose between the ocean_floor, land, volcanic and uplift provinces in v4.2's tectonic regions, or how tuff layers are written into stratovolcanoes in Java.

---

## Q6. Other ore-like worldgen: geodes, salt, sulfur, kaolin, clay, peat, hot springs, volcanoes

### Takeaway
Besides veins, TFC 1.21.1 places minerals in several other ways:
- Placer "deposit" discs in river and lake biomes
- Gem decorations around hot springs (sapphire, emerald) and volcanic fissures (topaz, diamond)
- Sulfur rivulets and crater-lake sulfur on stratovolcanoes
- Amethyst geodes
- Kaolin discs restricted by climate
- Clay and peat soil discs

### Cited Findings
- **Placer deposits** ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py), [placed_feature/cassiterite_deposit.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/placed_feature/cassiterite_deposit.json), [cassiterite_deep_deposit.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/placed_feature/cassiterite_deep_deposit.json), [in_biome/soil_discs/river.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/soil_discs/river.json)):
  - Covers native copper, cassiterite, native silver and native gold, all via `tfc:soil_disc`.
  - `<ore>_deposit` swaps `tfc:rock/gravel/<rock>` for `tfc:deposit/<ore>/<rock>` (radius 1–3, height 2, 1/12 chance, OCEAN_FLOOR_WG).
  - `<ore>_deep_deposit` swaps raw rock (radius 3–10, height 3, 1/24 chance, y 40–63).
  - Both are in `#tfc:feature/ore_deposits`, used by the soil-disc tags of the river, lake, meltwater_lake, oceanic_mountain_lake, rift_lake, tower_karst_lake and volcanic_oceanic_mountain_lake biomes.
- **Deposit loot** (panning/sluicing) ([assets.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/assets.py), [constants.py DEPOSIT_RARES](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)):
  - 50 % small ore, +10 % with a sluice
  - Loose rock of the host
  - Pumice for rhyolite, dacite and andesite
  - A per-rock rare drop at 5.33 %:

    | Host rock | Rare drop |
    |---|---|
    | granite | topaz |
    | diorite | emerald |
    | gabbro | diamond |
    | shale | borax |
    | claystone, dolomite | amethyst |
    | limestone, marble | lapis |
    | conglomerate | lignite |
    | chert | ruby |
    | chalk | sapphire |
    | quartzite | opal |
    | gneiss | gypsum |
    | tuff, all IE, slate, phyllite, schist | pyrite |

- **Geode** ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py), [placed_feature/geode.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/placed_feature/geode.json)):
  - Vanilla `minecraft:geode` with outer `tfc:rock/hardened/basalt`, middle/inner `tfc:rock/hardened/quartzite`, alternate inner `tfc:ore/amethyst/quartzite` (chance 0.3), air filling.
  - 1/50 chunks, y −48..30, in every biome via `#tfc:in_biome/veins`.
- **Hot springs** ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py), [configured_feature/sapphire_hot_spring.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/sapphire_hot_spring.json), [placed_feature/random_active_hot_spring.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/placed_feature/random_active_hot_spring.json)):
  - `tfc:hot_spring` (radius 14, spring water or empty) can decorate surrounding raw IE/II/MM rock with `ore/sapphire/<rock>` or `ore/emerald/<rock>` (radius 5, count 5 filled / 2 empty, rarity 3).
  - Random active springs (1/50 chunks) weights: sapphire-empty 1, emerald-empty 1, empty 2, sapphire 3, emerald 3, plain 6.
  - Random empty springs: 1/70 chunks.
  - Placed through biome large-feature tags. Examples: `random_active_hot_spring` in canyons and dormant_shield_volcano; `random_empty_hot_spring` in highlands and extinct_shield_volcano. deep_ocean_trench gets only the gem-free `lava_hot_spring` ([in_biome/large_features/](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/large_features)).
- **Volcanic fissures** (`tfc:fissure`; basalt walls, lava or spring water) on cinder cones, tuyas and the stratovolcano variants fuji, tahoma, kelimutu and crater_lake ([world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py)):
  - Lava fissures decorate IE/II/MM raw rock with topaz (weight 3) or diamond (weight 1), or nothing (weight 2–4).
  - Spring-water fissures decorate with sapphire (2), emerald (2) or nothing (2).
- **Sulfur:**
  - `tfc:sulfur_rivulet` (`tfc:rivulet`) replaces raw/hardened andesite, dacite, basalt, rhyolite and tuff with `ore/sulfur/<rock>` ([configured_feature/sulfur_rivulet.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/sulfur_rivulet.json)).
  - `tfc:crater_lake_sulfur` is in `#tfc:feature/stratovolcanoes` ([tags/.../feature/stratovolcanoes.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/feature/stratovolcanoes.json)).
  - The tuff_sulfur and deep near-lava sulfur veins are in Q3.
- **Kaolin** ([constants.py KAOLIN_BIOMES](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py), [v4.0.0-beta release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta)):
  - Red/pink/white kaolin clay blocks plus kaolin clay grass.
  - Only in the 29 `KAOLIN_BIOMES`: rolling hills, highlands, plateaus, old mountains, karst highlands, buttes, mesas, canyons, dormant/extinct/ancient shield volcanoes, rift valley/lake, and others.
  - Needs min temperature 18 and min groundwater 300.
  - v4.0 notes: kaolin now generates in more biomes, with shield volcanoes as a notable source.
- **Other soil and mineral features** ([constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py), [world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py), [TFCBlocks.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/TFCBlocks.java)):
  - Clay discs (with indicator plants)
  - `tfc:peat_disc` (block `tfc:peat`)
  - Salt licks as forest groundcover (`groundcover/salt_lick`, 5–33 °C, rainfall 100–500, needs forest, 1/110)
  - Calcite and mega-calcite cave spikes
  - Guano on islands and atolls
  - Pumice and obsidian-shard groundcover on volcanoes
- **Biome-scale features** ([v4.0.0-beta release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.0.0-beta), [v4.2.0 release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.0)):
  - v4.0: salt flats and mud flats ("Can be mud or salt flats"), shield volcanoes, tuyas, cinder cones, karst biomes on flux-stone surface rock.
  - v4.2: stratovolcanoes in subduction zones, mid-ocean ridges, atolls.
  - `salt_flats` and `mud_flats` biome JSONs exist ([biome dir](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/resources/data/tfc/worldgen/biome)).

### Inferences
- Several gem sources are already geologically motivated hooks: hot-spring/fissure decorations for sapphire, emerald, topaz and diamond, and collisional-mountain marble for ruby. A geology mod could keep them, or replace them with its own deposit logic while emitting the same `ore/<gem>/<rock>` blocks.
- Placer deposits already exist for Cu, Sn, Ag and Au, so a "placer from eroded source" model maps directly onto `tfc:deposit/<ore>/<rock>` blocks in river and lake gravels.
- Halite is generated only as subsurface discs (vein `halite`). Salt flats in v4 are a biome. I did not confirm whether they place halite blocks or only surface palettes.

### Gaps
- I did not inspect the salt_flats surface builder, so I don't know whether salt-flat biomes contain halite.
- I did not check the exact biome list for hot springs and fissures beyond the tag files seen.

---

## Q7. Mapping each TFC ore/mineral/gem to real deposit types, and real deposit types with no TFC equivalent

### Takeaway
Every TFC ore has a clear real-world deposit analogue. TFC's default host-rock choices are gameplay-driven and often differ from reality: for example, garnierite deep in intrusives instead of near-surface laterite, and cinnabar in metamorphic rock instead of near-surface hot-spring settings. Primary copper sulfides (chalcopyrite), lead, PGE, chromite, U, W, Mo, Mn, Al and Li have no base-TFC block.

### Cited Findings (real geology; TFC default placement from Q3 shown for contrast)
- **Native copper:**
  - Real: Keweenaw native copper fills cavities on lava-flow tops ("lacy") and the spaces between clasts in conglomerate/breccia interbedded with the volcanic pile. It formed in the Midcontinent Rift, about 1.096–1.087 Ga ([Wikipedia: Keweenaw Peninsula](https://en.wikipedia.org/wiki/Keweenaw_Peninsula)).
  - TFC: IE only (surface and montane), plus placer deposits.
- **Malachite:**
  - Real: forms by supergene weathering/oxidation of primary copper sulfide ore near the surface. It is typically tied to copper deposits in or around limestone (the carbonate source) and associated with azurite and goethite ([Wikipedia: Malachite](https://en.wikipedia.org/wiki/Malachite)).
  - Porphyry Cu deposits form in subduction-related arcs from multiple porphyritic diorite-to-quartz-monzonite intrusions with stockwork veins. They undergo supergene enrichment and yield Mo, Au and Ag as by-products ([Wikipedia: Porphyry copper deposit](https://en.wikipedia.org/wiki/Porphyry_copper_deposit)).
  - TFC: marble, limestone, chalk, dolomite.
- **Tetrahedrite:**
  - Real: mainly low- to moderate-temperature hydrothermal veins, some contact-metamorphic deposits. It is a minor copper ore and historically a silver ore ([Wikipedia: Tetrahedrite](https://en.wikipedia.org/wiki/Tetrahedrite)).
  - TFC: metamorphic rocks.
- **Cassiterite:**
  - Real: high-temperature hydrothermal quartz veins and pegmatites tied to granite intrusions, with tourmaline, topaz, fluorite, wolframite, molybdenite and arsenopyrite. Most modern production is placer, because cassiterite resists weathering (Cornwall, Bolivia, SE Asia) ([Wikipedia: Cassiterite](https://en.wikipedia.org/wiki/Cassiterite)).
  - TFC: II at high elevation, plus placer deposits.
- **Bismuthinite:**
  - Real: hydrothermal veins, including granite-linked tourmaline-copper veins, some high-temperature gold veins, and volcanic exhalations. Associated with cassiterite, wolframite, stannite and arsenopyrite ([Wikipedia: Bismuthinite](https://en.wikipedia.org/wiki/Bismuthinite)).
  - TFC: SED near the surface, II at depth.
- **Sphalerite:**
  - Real, by deposit type ([Wikipedia: Sphalerite](https://en.wikipedia.org/wiki/Sphalerite)):
    - MVT: replaces carbonate (dolostone/limestone) in platform and foreland settings; usually Phanerozoic.
    - SEDEX: in shales, carbonates and organic-rich siltstones in back-arc basins and failed rifts; about half of mined Zn+Pb.
    - VMS: in submarine volcanic rocks; about 25 % of Zn reserves.
    - Also found in skarns.
  - TFC: IE near the surface, II at depth.
- **Hematite / magnetite (BIF):**
  - Real: banded iron formations are nearly all Precambrian, peaking 2.8–2.5 Ga with a smaller peak around 1.85 Ga ([Wikipedia: Banded iron formation](https://en.wikipedia.org/wiki/Banded_iron_formation)).
  - Deposition peaked as atmospheric O₂ appeared (about 2.41–2.35 Ga) and ended around 1.85 Ga with deep-ocean oxygenation.
  - Algoma type: small basins with volcanics, typically Archean. Lake Superior type: shelf settings with black shale, quartzite and dolomite, typically Paleoproterozoic.
  - Iron oxides are magnetite or hematite; magnetite is more common in older BIFs.
  - Small Neoproterozoic BIFs (about 750 Ma) are tied to Snowball Earth glaciations; they are hematite-rich with about 1 % phosphate.
  - TFC: hematite in IE; magnetite in SED.
- **Limonite:**
  - Real: weathering and oxidation of iron minerals and sulfides. Forms gossans over sulfide ores (used by prospectors), laterites (also Ni/Co sources) and bog iron ore in marshes. It was the most prevalent early iron ore in Africa ([Wikipedia: Limonite](https://en.wikipedia.org/wiki/Limonite)).
  - TFC: SED.
- **Garnierite:**
  - Real: lateritic weathering of serpentinized ultramafic rock (peridotite), filling pockets and veins. Examples: Falcondo (Dominican Republic), Riddle (Oregon, a 15–61 m weathering layer over peridotite), New Caledonia (type area) ([Wikipedia: Garnierite](https://en.wikipedia.org/wiki/Garnierite)).
  - TFC: deep II/gabbro, y −80..0.
- **Native gold:**
  - Real: orogenic gold forms in metamorphosed fore-arc/back-arc and greenstone belts during late orogeny. It is controlled by shear zones and quartz-carbonate veins; epochs are 2.8–2.5 Ga, 2.1–1.8 Ga and 0.5–0.05 Ga, with little from 1.8–0.8 Ga ([Wikipedia: Orogenic gold deposit](https://en.wikipedia.org/wiki/Orogenic_gold_deposit)).
  - Many placers are orogenic in origin; including placers, orogenic deposits make up about 75 % of world gold production. Witwatersrand is cited.
  - TFC: IE+II; pyrite "troll" veins alongside.
- **Cinnabar:**
  - Real: vein-filling mineral tied to volcanic activity and alkaline hot springs. It precipitates from near-surface warm ascending water and is still forming at Sulphur Bank (California) and Steamboat Springs (Nevada). Major deposits: Almadén, Idrija, Monte Amiata, New Almaden ([Wikipedia: Cinnabar](https://en.wikipedia.org/wiki/Cinnabar)).
  - TFC: quartzite, phyllite, gneiss, schist, both deep and montane.
- **Cryolite:**
  - Real: Ivittuut (SW Greenland) was the main commercial source, mined until 1987. Minor occurrences: Pikes Peak, Francon Quarry (Montreal), Miass. Synthetic cryolite made from fluorite now replaces it ([Wikipedia: Cryolite](https://en.wikipedia.org/wiki/Cryolite)).
  - TFC: deep granite/diorite.
- **Saltpeter (niter) / nitratine:**
  - Real: nitratine (Chile saltpeter) is the sodium analogue of saltpeter. It forms "only as an efflorescence in very dry environments". Main deposits: Atacama Desert; also Death Valley, Mexico, Egypt, Peru, South Africa ([Wikipedia: Nitratine](https://en.wikipedia.org/wiki/Nitratine)).
  - TFC: SED discs at y 40–100.
- **Halite, sylvite, gypsum, borax (evaporites):**
  - Real: form where evaporation exceeds inflow, usually in arid areas. Seawater precipitates in order: carbonates → gypsum/anhydrite → halite → K/Mg salts (sylvite, carnallite) ([Wikipedia: Evaporite](https://en.wikipedia.org/wiki/Evaporite)).
  - Marine evaporites are thicker. Borax and other borates form in non-marine arid salt lakes (SW USA).
  - The largest known evaporite deposits are from the Messinian salinity crisis.
  - TFC: all are SED discs; halite at depth below the surface.
- **Sulfur:**
  - Real: native sulfur occurs near hot springs and volcanic regions (Ring of Fire) and in salt domes along the Gulf of Mexico coast. It also occurs in evaporites of eastern Europe and western Asia, and anaerobic bacteria make it from sulfate (gypsum) in salt domes ([Wikipedia: Sulfur](https://en.wikipedia.org/wiki/Sulfur)).
  - TFC: deep near lava, in tuff, and in volcanic rivulets.
- **Graphite:**
  - Real: forms by metamorphism of carbon-rich sediments. Flake graphite comes from carbonaceous metamorphic rocks, amorphous graphite from thermally metamorphosed coal, and vein graphite from high-grade metamorphic regions ([Wikipedia: Graphite](https://en.wikipedia.org/wiki/Graphite)).
  - TFC: gneiss, marble, quartzite, schist.
- **Kaolinite:**
  - Real: chemical weathering of Al-silicates such as feldspar. Most abundant in hot, humid (tropical) climates, favoured by alternating wet/dry seasons; illite/smectite take over in cooler or drier climates ([Wikipedia: Kaolinite](https://en.wikipedia.org/wiki/Kaolinite)).
  - Deposits can be residual or sedimentary, e.g. Georgia "white gold" belt, Mesozoic–Cenozoic mantles in Europe.
  - TFC: temperature ≥ 18 and groundwater ≥ 300.
- **Lapis lazuli:**
  - Real: a metamorphic rock usually in crystalline marble from contact metamorphism (30–40 % lazurite, with calcite and pyrite). Localities: Sar-i Sang (Afghanistan), Ovalle (Chile), Tultui (Lake Baikal) ([Wikipedia: Lapis lazuli](https://en.wikipedia.org/wiki/Lapis_lazuli)).
  - TFC: limestone/marble.
- **Ruby:**
  - Real: red corundum coloured by Cr. Sources: Mogok, Mong Hsu (Myanmar), Jegdalek (Afghanistan), Azad Kashmir, Montepuez (Mozambique, now the most productive) ([Wikipedia: Ruby](https://en.wikipedia.org/wiki/Ruby)).
  - TFC: marble belts in collisional mountains, plus deep marble.
- **Sapphire:**
  - Real: mined from placers or primary rock. Classes are "classic metamorphic" (Kashmir, Burma, Sri Lanka) and magmatic (e.g. Mogok sapphires in pegmatites/syenites). Australia was the largest source of particoloured sapphire; Montana has placer and Yogo Gulch sources ([Wikipedia: Sapphire](https://en.wikipedia.org/wiki/Sapphire)).
  - TFC: hot springs and spring-water fissures.
- **Topaz:**
  - Real: mainly in granitic pegmatites and in vapor cavities of rhyolite flows (Topaz Mountain, Utah). An accessory in felsic igneous and hydrothermally altered rocks; Brazil is a major producer ([Wikipedia: Topaz](https://en.wikipedia.org/wiki/Topaz)).
  - TFC: lava fissures, plus granite-deposit rare drops.
- **Opal:**
  - Real: low-temperature hydrated silica in fissures. Sedimentary opal: Lightning Ridge, Coober Pedy, Wollo. Volcanic opal in weathered rhyolite: Shewa (Ethiopia), Mexican fire opal ([Wikipedia: Opal](https://en.wikipedia.org/wiki/Opal)).
  - TFC: river discs in SED/IE.
- **Emerald:**
  - Real: beryl (Be required) coloured by Cr or V, associated with mica schist, quartz, albite and pegmatite minerals. Producers: Colombia (Muzo, Coscuez, Chivor) and Zambia (Kafubu/Kagem) ([Wikipedia: Emerald](https://en.wikipedia.org/wiki/Emerald)).
  - TFC: pipes in II, plus hot springs.
- **Diamond:**
  - Real: kimberlites are the most important primary source. They are vertical carrot-shaped pipes 75 m–1.5 km across, from 150–450 km depth; about 6,400 are known, about 900 diamondiferous and about 30 mined. They also feed placers ([Wikipedia: Kimberlite](https://en.wikipedia.org/wiki/Kimberlite)).
  - Diamonds "originate in the roots of cratons and are almost always over 2 billion years" old ([Wikipedia: Craton](https://en.wikipedia.org/wiki/Craton)).
  - TFC: pipes in gabbro, plus fissures.
- **Amethyst:**
  - Real: large geodes in volcanic rocks of Rio Grande do Sul / Paraná Basin (Brazil) and Artigas (Uruguay), drusy cavities in granite (Mursinka), and veins (Maissau) ([Wikipedia: Amethyst](https://en.wikipedia.org/wiki/Amethyst)).
  - TFC: river discs, plus basalt/quartzite geodes.

### Mapping table (TFC item → real deposit models → suggested PlateTectonics trigger)
The middle column summarises the cited findings above. The right column contains **design inferences**, not sourced facts.

| TFC ore / mineral | Real deposit models (sourced above) | Suggested tectonic/age trigger (inference) |
|---|---|---|
| native_copper | Rift flood-basalt flow tops and interflow conglomerates (Keweenaw); also supergene | Continental rift / LIP basalts (IE), 1.1 Ga-type rifts |
| malachite | Supergene oxide zone of Cu sulfide deposits near carbonates; porphyry cap | Above porphyry systems in arcs; arid/eroded uplands over carbonate |
| tetrahedrite | Low–moderate-T hydrothermal veins, contact metamorphic; Ag-bearing | Arc-related polymetallic/epithermal veins; metamorphic belts |
| cassiterite | Granite-related high-T veins and pegmatites; placers | Collisional (S-type) granites; downstream placers |
| bismuthinite | Granite-related veins with Sn-W | Same granites as cassiterite (Sn-W-Bi halo) |
| sphalerite | MVT (carbonate platforms), SEDEX (rift/back-arc shale basins), VMS (submarine volcanics), skarn | Passive-margin carbonates; failed rifts; ocean-floor/back-arc volcanics |
| hematite / magnetite | BIF (Algoma: Archean volcanic basins; Superior: Paleoproterozoic shelves), Snowball BIF | Pre-oxygenation oceans (around 2.5–1.85 Ga); Cryogenian glaciations |
| limonite | Gossans, laterites, bog iron | Weathering surfaces over sulfide bodies; humid lowlands/marshes |
| garnierite | Ni laterite over serpentinized ultramafics | Obducted ophiolites/ultramafics under tropical weathering |
| native_gold | Orogenic gold in metamorphic belts (Archean, Paleoproterozoic, Phanerozoic peaks); placers | Accretionary/collisional orogens, greenstone belts; rivers draining them |
| native_silver | Not researched (see Gaps) | (tetrahedrite/epithermal halo, inference) |
| cinnabar | Near-surface hot-spring / volcanic vein deposits (Almadén, Steamboat Springs) | Young volcanic arcs and geothermal fields, shallow levels |
| cryolite | Ivittuut, Greenland (unique); minor Pikes Peak, Miass | Extremely rare; anorogenic alkaline granites (inference) |
| saltpeter | Arid efflorescence (Atacama) | Hyper-arid climates (rain-shadow / cold-current coasts) |
| halite, sylvite, gypsum | Marine evaporite sequence | Restricted arid basins, rifts / closing oceans (Messinian-type) |
| borax | Non-marine arid salt lakes | Arid closed basins in extensional/volcanic terrains |
| sulfur | Volcanic fumaroles/hot springs; salt-dome caprock; evaporites | Active volcanoes; salt-dome provinces |
| graphite | Metamorphosed organic-rich sediments; thermally metamorphosed coal | Shales/coals that went through high-grade metamorphism |
| kaolin | Tropical weathering of feldspathic rocks | Warm, humid, long-exposed granite terrains |
| lapis_lazuli | Contact-metamorphic marble | Marble at intrusive contacts in orogens |
| ruby | Marble / metamorphic (Himalayan belt per TFC comment), Mozambique | Collisional orogens with marble |
| sapphire | Metamorphic and magmatic; placers | Metamorphic belts; alkaline volcanic provinces (inference) |
| topaz | Granitic pegmatites; rhyolite vapor cavities | Evolved F-rich granites/rhyolites |
| opal | Low-T silica in sedimentary basins and weathered rhyolite | Arid deeply weathered basins; felsic volcanic fields |
| emerald | Be source plus Cr/V source (schist/pegmatite; Colombian shale) | Pegmatite intruding mafic/ultramafic schists (inference) |
| diamond | Kimberlite pipes in old cratons | Archean/Paleoproterozoic cratons with thick lithosphere |
| amethyst | Geodes in volcanic rocks; granite cavities | Flood basalts (Paraná-type LIP) |
| bituminous coal / lignite | Not researched here (see the sibling note coal_and_petroleum.md) | Swamp basins; coal rank rises with burial |

- **Real deposit classes with no base-TFC block:** there is no Ore enum entry for chalcopyrite/bornite (primary porphyry Cu sulfide), molybdenite (Mo), wolframite/scheelite (W), galena (**TFC has no lead or galena**), stibnite, chromite, PGE, uraninite, pentlandite (Ni sulfide), manganese oxides, phosphate, bauxite, lithium minerals, REE, fluorite or barite ([Ore.java](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Ore.java)). Porphyry deposits' Mo by-product is cited in [Wikipedia: Porphyry copper deposit](https://en.wikipedia.org/wiki/Porphyry_copper_deposit).

### Inferences
- **Copper in porphyry systems:** with no chalcopyrite block, the mod needs a mapping convention. A reasonable one:
  - Hypogene core → tetrahedrite (veins) and/or poor-grade malachite
  - Supergene enrichment blanket → rich native copper
  - Leached cap/gossan → limonite
  - Oxide zone → malachite
- **Secular variation:** BIF-type magnetite/hematite should be restricted to pre-1.85 Ga (and Cryogenian) sedimentary packages, and diamonds to Archean-cored cratons. Both are supported by the sources above.
- **Cinnabar and garnierite** are the two biggest mismatches between TFC defaults and reality:
  - Cinnabar: TFC puts it deep in metamorphics; reality is shallow volcanic/hot-spring.
  - Garnierite: TFC puts it deep in intrusives; reality is near-surface laterite over ultramafics.
  - A geology mod can move them, but must keep enough supply for redstone (cryolite is an alternative source) and nickel (black steel progression).
- There is no TFC ultramafic rock (peridotite/serpentinite). Gabbro is the closest stand-in for ophiolite or kimberlite hosts.

### Gaps
- I found no fetched source on cryolite's deposit type. Wikipedia's page does not describe the host; "Ivigtut-type pegmatite in alkaline granite" is unverified here.
- I did not research native silver, pyrite or coal deposit models in this note.
- I could not check Clifford's rule: the Wikipedia page returned 404. The craton–diamond link is supported only by the Craton article sentence.
- I did not research real-world chromite, PGE, U, W, Mn, phosphate, bauxite, Li or REE deposit models, because they are outside TFC's ore list.
- The web search budget for this session ran out before I could look for USGS deposit-model sources. Cox & Singer (USGS Bulletin 1693) was opened but its model list was not on the landing page ([USGS Bulletin 1693](https://pubs.usgs.gov/bul/b1693/)).

---

## Q8. Addons that add ores, and whether supporting them is worthwhile

### Takeaway
On 1.21.1 NeoForge, **Firmalife** confirmedly adds a graded **chromite** ore using TFC's own vein system and tag. TFC Metallum's GitHub has only 1.18/1.20.1 branches, though a "TFC Metallum 1.21.1" jar is listed under a successor project. TerraFirmaGreg Modern is a **1.20.x Forge** modpack with its own GregTech ore-vein system, not a 1.21.1 target.

### Cited Findings
- **Firmalife** (`eerussianguy/firmalife`) has a `1.21.x` branch ([firmalife repo](https://github.com/eerussianguy/firmalife)). It adds `firmalife:ore/{poor,normal,rich}_chromite/<rock>`, including tuff, and `firmalife:ore/small_chromite`. The block tag `c:ores/chromite` exists ([firmalife 1.21.x tree](https://github.com/eerussianguy/firmalife/tree/1.21.x/src/generated/resources/data/c/tags/block/ores)). Its veins ([vein/normal_chromite.json](https://github.com/eerussianguy/firmalife/blob/1.21.x/src/main/resources/data/firmalife/worldgen/configured_feature/vein/normal_chromite.json), [vein/deep_chromite.json](https://github.com/eerussianguy/firmalife/blob/1.21.x/src/main/resources/data/firmalife/worldgen/configured_feature/vein/deep_chromite.json)):

  | Vein | Type | Rarity | Density | Size | y range | Hosts | Grade | Indicator |
  |---|---|---|---|---|---|---|---|---|
  | `normal_chromite` | `tfc:cluster_vein` | 50 | 0.25 | 8 | 40..130 | IE + MM | POOR | surface rarity 14 |
  | `deep_chromite` | `tfc:cluster_vein` | 90 | 0.6 | 15 | −80..20 | II + MM | RICH | 4 underground |

  Both are registered by adding `firmalife:vein/normal_chromite` and `firmalife:vein/deep_chromite` to `tfc:in_biome/veins` with `"replace": false` ([firmalife tags/worldgen/placed_feature/in_biome/veins.json](https://github.com/eerussianguy/firmalife/blob/1.21.x/src/main/resources/data/tfc/tags/worldgen/placed_feature/in_biome/veins.json)).
- **TFC Metallum** (`Mrthomas20121-Mods/TFC-Metallum`) has only `1.18` and `1.20.1` branches (default `1.20.1`) ([repo](https://github.com/Mrthomas20121-Mods/TFC-Metallum)).
  - Its 1.20.1 `TFCMetallumModernOre` enum: STIBNITE, BAUXITE, BORACITE, COBALTITE, GALENA, NATIVE_IRIDIUM, NATIVE_OSMIUM, NATIVE_PLATINUM, RUTILE, WOLFRAMITE, URANINITE ([TFCMetallumModernOre.java @1.20.1](https://github.com/Mrthomas20121-Mods/TFC-Metallum/blob/1.20.1/src/main/java/tfc_metallum_modern/util/TFCMetallumModernOre.java)).
  - Vein files use surface_/normal_/deep_ prefixes, e.g. `deep_galena`, `normal_native_platinum`, `surface_bauxite` ([1.20.1 vein dir](https://github.com/Mrthomas20121-Mods/TFC-Metallum/tree/1.20.1/src/generated/resources/data/tfc_metallum_modern/worldgen/configured_feature/vein)).
  - A ModDex listing for "TFC Metallurgy" shows a "TFC Metallum 1.21.1-1.1.0.jar" and describes the project as a refactor of Metallum. The visible ore list began "Bauxite, Certus Quartz" (search-snippet level, not opened) ([ModDex: tfc-metallurgy](https://moddex.gg/mod/tfc-metallurgy)).
- **TerraFirmaGreg Modern:** `pakku.json` describes it as "GregTech and TerraFirmaCraft on 1.20.x". The kubejs folder holds its own `ore_veins` lang files and EMI ore-vein info ([TerraFirmaGreg-Team/Modpack-Modern](https://github.com/TerraFirmaGreg-Team/Modpack-Modern)). Web-search snippets for its CurseForge file pages say release 0.12.0 backported TFC's 1.21 world generation to the 1.20.1 pack. The page was not opened, and the exact file page carrying that changelog is unverified ([CurseForge: terrafirmagreg-modern files](https://www.curseforge.com/minecraft/modpacks/terrafirmagreg-modern/files/6871092)).
- The standalone "TerraFirmaGregTech - Ores" mod is marked deprecated ([CurseForge: terrafirmagregtech-ores](https://www.curseforge.com/minecraft/mc-mods/terrafirmagregtech-ores)).
- A third-party GitHub repo describes itself as a hand-made 1.21.1/NeoForge port of TFG ("Gregnautics-Continued-Test") (GitHub search result, repo `drainstar/Gregnautics-Continued-Test`; not inspected).
- A "TFC Volcanoes" addon for 1.20.1 and 1.21.1 reportedly adds sulphur, ammonium chloride, salt and saltpeter deposits around hot springs and lava pools (search snippet only) ([modpackindex: TFC Volcanoes](https://www.modpackindex.com/mod/54874/tfc-volcanoes)).

### Inferences
- **Supporting Firmalife chromite is cheap and high value.** It uses the same vein codecs, rock naming and `#tfc:in_biome/veins` hook. It also fills one of the biggest real-deposit gaps: chromite is real-world ophiolite/layered-intrusion ore, though that is not sourced here.
- **Metallum-style ores** (galena, bauxite, wolframite, uraninite, PGE, cobaltite, stibnite, rutile) would cover most "no TFC equivalent" deposit classes. Support should be optional and data-driven: detect blocks by registry ID / `c:ores/*` tags at runtime, because its 1.21.1 status is uncertain.
- **TerraFirmaGreg** is not a 1.21.1 target. Its GregTech vein system replaces TFC veins, so supporting it is low priority for a 1.21.1 NeoForge mod.
- **Recommended compatibility layer:** make the mod's ore palette a data map from "deposit model × host-rock category" to block IDs, keyed by mod ID. TFC defaults are required; Firmalife and Metallum entries are optional. Addon ores in `#c:ores` are automatically prospectable (Q4).

### Gaps
- My web search budget ran out, so I could not open the CurseForge/Modrinth pages for Metallum/Metallurgy 1.21.1, Firmalife 1.21.1 release versions, or TFC Volcanoes.
- I did not verify what Firmalife uses chromite for (presumably stainless steel).
- I did not verify TFG's 1.21.1 port status.
