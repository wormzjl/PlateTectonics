# Decision log

> **Level 1.** Read when you need to know *why* something is the way it is, or what is still open. Each entry has an ID that other docs cite.
> Status: **Accepted** = confirmed by the project owner · **Adopted** = default chosen from research, can be revisited · **Open** = needs the owner's call.

## Accepted (owner)

| ID | Decision | Notes |
|---|---|---|
| A-1 | **Cylinder world.** Terrain repeats east–west with period W; poles lie at the north and south edges | [01](01-world-model.md) |
| A-2 | **Scale factor S is tweakable; default 30** | S = real metres per block (A-9) |
| A-3 | **Vanilla height (−64…320) at the default scale; expand only when needed** | Expansion rule in [01](01-world-model.md#vertical-mapping) |
| A-4 | **Works with TerraFirmaCraft by default.** Report what is missing | Soft dependency; vanilla fallback. Gaps: [08](08-minecraft-integration.md#gaps) and the [report](../reports/Ore%20deposits%20and%20TFC%20compatibility.md) |
| A-5 | **TFC cinnabar = redstone** | TFC's quern turns cinnabar *and* cryolite into 8 redstone |
| A-6 | **Life, oxygen and similar milestones are simulated with randomness** (cheap 0-D box models) | [07](07-life-atmosphere-sun.md) |
| A-7 | **Build the simulation core first** (Phase 1 setup, Phase 2 plates; pure Java, PNG output) | [09](09-roadmap.md) |
| A-8 | **Design docs use progressive disclosure** (L0 README → L1 pages → L2 report and notes) | This folder |
| A-9 | **The scale applies horizontally as well: isotropic 30 m per block, no vertical exaggeration** (replaces the earlier "vertical exaggeration over ~1 km/block" reading) | The planet is ≈1.33 M blocks around; orebodies come out near real size |
| A-10 | **Single Earth scale for now** (was O-5): the planet is Earth-sized (C = 40,000 km); only S changes its size in blocks | No planet-size slider in v1 |
| A-11 | **Our mod adds its own world preset next to TFC's and never changes TFC's worldgen or config** (was O-1; owner note: "add another world provider, do not overwrite TFC's worldgen"). TFC's `tfc:overworld` stays the default; players pick "PlateTectonics" in the world-type list | Phase 4 check: TFC's `WorldPresetsMixin` only changes the *default* selection, so our preset must still be listed and selectable |
| A-12 | **East–west wrap: terrain repeats in v1; teleport-wrap later as an option** (was O-2) | |
| A-13 | **Modern vegetation and animals always spawn**; the simulated life timeline affects geology only, and the preview says so (was O-3) | |
| A-14 | **Add our own sandstone rock** (was O-4) as a full TFC-compatible rock: raw, hardened, cobble, gravel, loose rock, bricks, plus ore variants for ores that real sandstone hosts. Registered with TFC's `tfc:worldgen/rock_settings` registry (TFC ≥ 4.2.8) | Ore variants drop TFC's own ore items. The same mechanism can later add other missing rocks (e.g. peridotite) |

## Adopted (research defaults)

| ID | Decision | Why | Source |
|---|---|---|---|
| D-1 | **Vertical mapping** y = 63 + e / S with soft compression below −3 km and above +7 km ([01](01-world-model.md#vertical-mapping)) | At 30 m/block the vanilla range holds Earth's relief except trenches and the highest peaks | Report § scale arithmetic (it assumed 1 km/block horizontally; superseded by A-9) |
| D-2 | **Orebodies render at about real size** (porphyry 33–66 blocks, pipes 2–17 blocks). Thin horizons (coal seams, reefs) get a 1-block minimum. An optional gameplay size multiplier exists | At 30 m/block most bodies fit naturally; the report's separate render scale is needed only for thin beds | Report D1, revised by A-9 |
| D-3 | **Multi-tier architecture:** master simulation (Tier 1, ≈15.6 km) → province tiles (≈1 km, disk-cached) → local tiles (4 blocks, memory-cached) → chunk | Rivers and erosion need upstream context, and the gap from 15.6 km to 30 m is too large for one step | [01](01-world-model.md#refinement-levels) |
| D-4 | **`geosim-core` is pure Java with no Minecraft dependency**, plus a CLI that writes PNG maps | Fast iteration and unit tests | — |
| D-5 | **Determinism:** every random number = hash(seed, stream, step, cell); parallel loops use fixed partitions and fixed summation order | Same seed + settings → same planet, on any thread count | — |
| D-6 | **t = 0 is planet formation**, and every geologic gate keys off *simulated planetary state* (`WorldHistory.globalState(t)`), never absolute Earth dates | Matches the "proto-terrain, then elapsed time" flow | Report D2 |
| D-7 | **Latitude is linear in z, aligned to TFC:** north pole z = −s/2, equator z = s/2, south pole z = 3s/2; `temperature_scale` s = pole-to-equator distance (≈333,333 blocks at S = 30) | Avoids a client-side `SolarCalculator` mixin | Report D9 |
| D-8 | **TFC integration route (a):** our own `ChunkGenerator implements ChunkGeneratorExtension` plus our own `ChunkDataGenerator`; no mixins into TFC | Public interfaces; KubeJS-TFC precedent | Report § TFC 4.2.12 |
| D-9 | **Ores in two steps:** ship first with TFC's default veins (they follow host rock), then switch to our `DepositFeature` and override TFC's vein tags | Compatibility on day one; real geology after | Report D3 |
| D-10 | **Redstone policy:** geological cinnabar (hot-spring, epithermal, Almadén-type, accessory in Carlin and epizonal gold) + one guaranteed Ivigtut-type cryolite + sparse A-type cryolite | Young shallow Hg deposits erode away on old continents | Report D4 |
| D-11 | **Progression floor:** within a configurable radius of spawn, guarantee copper, tin, zinc + bismuth, gold + silver, nickel, iron, flux carbonate, coal and redstone, placed geologically where possible | Real densities are sparse at 30 m/block | Report D5 |
| D-12 | **Missing commodities:** stand-ins for progression metals (tetrahedrite for Cu sulfides, garnierite for Ni sulfide, magnetite for ilmenite, native silver for argentiferous galena); omit Pb, Mo, W, U, Mn, Al, Li, REE, P, fluorite, barite in v1; optional Firmalife and Metallum palettes | Stays inside TFC's content | Report D6 |
| D-13 | **Petroleum simulated from v1**; render tar seeps and oil shale first; oil and gas fluids later | Biggest TFC content gap | Report D7 |
| D-14 | **Coal blocks:** `tfc:peat`, `tfc:lignite`, `tfc:bituminous_coal` + our anthracite block with a TFC fuel entry | Rank follows peak burial temperature | Report D8 |
| D-15 | **Persistence:** save the master simulation and the deposit index in the world folder with a version stamp; province tiles cached on disk; `generateRock` reads saved columns | TFC recomputes rock on demand after reload | Report D11 |
| D-16 | **Richness:** geology decides *where*; a per-commodity multiplier calibrated to TFC vein frequencies decides *how many* | Real densities are very sparse | Report D12 |
| D-17 | **Biomes:** classify into TFC biome ids; add our own (with `BiomeExtension`) only where TFC has no match | Keeps TFC features, spawns and colours | Report D13 |
| D-18 | **Soil:** our own soil placement from the weathering model, using TFC soil blocks | `SurfaceManager` reuse is unverified | Report D14 |
| D-19 | **Sea level stays at y = 63**; sea-level change exists only inside the simulated history | TFC hard-codes 63 | Report § blocked |
| D-20 | **Rivers and volcanoes come from the simulation**, not from TFC's features | TFC's versions are coupled to its region generator | Report § blocked |

## Open

None at the moment. New questions are added here as `O-n`.
