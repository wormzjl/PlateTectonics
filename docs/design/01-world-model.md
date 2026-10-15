# 01 · World model: shape, scale, coordinates, heights

> **Level 1** · Read when: you touch coordinates, the scale factor, latitude, the height mapping or grid sizes.
> Decisions: A-1, A-2, A-3, A-9, A-10, A-12, D-1, D-2, D-7, D-19 in [decisions.md](decisions.md).

**Summary.** The planet is **Earth-sized** and shaped as a cylinder that repeats east–west, with poles at the north and south edges. **One block = 30 m in every direction** (scale factor S = 30, isotropic, so no vertical exaggeration). The planet is therefore ≈1.33 million blocks around and ≈667,000 blocks pole to pole. Earth-like relief fits the vanilla −64…320 range after gentle compression of the deepest oceans and highest peaks. The history simulation runs on a coarse ≈15.6 km grid; three refinement levels bring it down to single blocks near the player.

## Parameters

| Parameter | Symbol | Default | Range | Notes |
|---|---|---|---|---|
| Scale factor (real metres per block, both axes) | S | **30** | 10–100 | Owner-tweakable (A-2, A-9) |
| Planet circumference | C | 40,000 km (Earth) | fixed for now | A-10: single Earth scale |
| Circumference in blocks | W = C / S | 1,333,333 | — | Terrain repeats with period W (A-12) |
| Pole-to-pole extent | H = W / 2 | 666,667 | — | Same proportions as Earth's meridian |
| Pole-to-equator distance (TFC `temperature_scale`) | s = H / 2 | 333,333 | — | D-7; TFC accepting this value is a Phase 4 check |
| Sea level | — | y = 63 | fixed | D-19 |
| Master grid (Tier 1) | — | 2,560 × 1,280 cells of 15.625 km | 16 or 31 km | ≈521 blocks per cell at S = 30; Phase 2 benchmark decides |
| Province tiles (Tier 1.5a) | — | ≈1 km cells (≈33 blocks), 256 × 256 cells per tile | — | Local landscape evolution; cached on disk |
| Local tiles (Tier 1.5b) | — | 4-block cells (120 m), 512 × 512 blocks per tile | — | Detail and river channels; cached in memory |
| Elapsed time since formation | T | 4,500 Myr | 100–6,000 Myr | Player choice |

## Coordinates

- **x (east–west):** periodic with period W. In v1 terrain simply repeats; an optional teleport-wrap comes later (A-12). Walking around the equator takes ≈86 hours, so the wrap is mostly a property of the map, not something players reach.
- **z (north–south):** bounded. Minecraft north is −z.
  - North pole at z = −s/2 (≈ −166,667), equator at z = +s/2 (≈ +166,667), south pole at z = +3s/2 (≈ +500,000).
  - **Latitude** φ(z) = 90° × (s/2 − z) / s. This is linear and matches TFC's `SolarCalculator` triangle wave inside that range (D-7). 1° of latitude ≈ 111 km ≈ 3,700 blocks. World origin (0, 0) lies at 45° N.
  - All coordinates stay well inside Minecraft's ±30,000,000 limit.
- **Simulation grids** are defined in kilometres, not blocks. Block coordinates map to planet coordinates by multiplying by S, so changing S changes how many blocks the same planet spans.
- **Time:** the simulation uses t = Myr since formation (0 … T). The UI shows "Ma" = T − t.

> Linear latitude over-represents polar land compared with a sphere: here a third of the map is poleward of 60°, versus about 13% of a sphere's surface. That is accepted for TFC alignment (D-7).

## Refinement levels

| Level | Cell | Covers | Computes | Lifetime |
|---|---|---|---|---|
| Tier 1 master | ≈15.6 km | Whole planet | Full geologic history ([02](02-simulation-pipeline.md)) | Saved with the world |
| Tier 1.5a province | ≈1 km (≈33 blocks) | 256 km tiles | Local landscape evolution for the last few Myr: FastScape with uplift, rock erodibility, climate and inflowing discharge from Tier 1; glacial carving; correlated rock layers | Computed when a player approaches; cached on disk |
| Tier 1.5b local | 4 blocks (120 m) | 512-block tiles | Detail synthesis conditioned on slope, rock and climate; river channels and lake shores from the province network; volcano cones | Cached in memory (LRU) |
| Tier 2 chunk | 1 block | 16 × 16 | Strata, ores, surface, features ([08](08-minecraft-integration.md)) | Normal chunk generation |

Each level only adds detail **inside** the constraints of the level above (drainage, mean elevation, rock, climate), so the levels cannot contradict each other and tiles join without seams.

## Vertical mapping

Present-day elevation e (metres, relative to sea level) maps to block y as follows (S = metres per block):

```
y_lin(e) = 63 + e / S                     (30 m per block by default)

Land:        e ≤ 7,000 m              → y = y_lin(e)          (up to y ≈ 296)
High peaks:  e > 7,000 m              → soft cap: y = 296 + 20·tanh((y_lin − 296) / 20)   (never above 316)
Ocean floor: e ≥ −3,000 m             → y = y_lin(e)          (down to y = −37)
Deep ocean:  −11,000 ≤ e < −3,000 m   → linear compression into y −37 … −60 (≈350 m per block)
Bedrock:     y −64 … −61 reserved
```

Shelves, coasts, lowlands and mountains up to 7 km are true to scale. Only abyssal plains, trenches and the very highest summits are compressed.

**Height expansion (A-3).** If S is lowered or the player wants the full relief, the UI offers a taller dimension type:

```
maxY = roundUp16(63 + E_max / S + 16)      E_max = highest simulated summit (m)
minY = roundDown16(63 − D_max / S − 8)     D_max = deepest ocean floor kept uncompressed (m)
limits: −2032 ≤ minY, maxY ≤ 2031, maxY − minY ≤ 4064 (vanilla dimension-type limits)
example (S = 30, Everest-class peaks, 6 km abyssal floor uncompressed): y −160 … 384
```

Sea level stays at 63. Costs: more sections per chunk, and TFC's climate model stops cooling above y ≈ 173 (lossy; see [08](08-minecraft-integration.md#gaps)).

## What the 30 m scale means in play

- **Slopes are realistic.** A 2–3 km-deep alpine valley is 70–100 blocks deep and 300–700 blocks across; Himalayan-scale ranges are ~10,000 blocks wide and up to ~300 blocks high. Plains really are flat.
- **Rivers are real-sized.** Great rivers are 30–300 blocks wide; streams narrower than a block are drawn at a minimum of 1–2 blocks wherever discharge exceeds a threshold.
- **Climate zones and biomes are huge.** A desert may span tens of thousands of blocks. The in-game map and a latitude/longitude readout matter for navigation (Phase 8).
- **Orebodies are close to real size (D-2).** A porphyry ore shell (1–2 km) is ≈33–66 blocks across and a kimberlite pipe (50–500 m) ≈2–17 blocks. Only thin horizons (coal seams 0.3–3 m, PGE reefs ~1 m) need a minimum thickness of 1 block.
- **Real deposit densities are sparse** (e.g. ~15 porphyries per 100,000 km² of arc ≈ per 10,500 × 10,500 blocks), so richness multipliers (D-16) and the progression floor (D-11) matter.

## Open points

- A-12 (later): teleport-wrap at x = ±W/2.
- Phase 4: confirm TFC accepts `temperature_scale` ≈ 333,333.
