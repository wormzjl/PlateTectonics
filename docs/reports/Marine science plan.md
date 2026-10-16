# Marine science plan (2026-10-16, for owner review)

Plan for the cheap marine-model fixes H2, H3, H8 (and optionally H9 and H12) from [Marine spreading review](Marine%20spreading%20review.md). No code is changed until the owner approves. These fixes change the planet on purpose. They stay deterministic and give the same planet on any thread count (D-5). They would become decision D-41.

## L0 — Summary

- **What changes:**
  - Shelves get a width that depends on the margin and the river, and slope down from the coast instead of sitting flat at −120 m.
  - Aprons thin away from the mouth and drop down a slope instead of forming −120 m plateaus.
  - Deltas build land.
  - Carbonate grows fastest in the top few tens of metres and not under river mud.
  - River mouths no longer always run north-west first.
- **What stays:**
  - Round fans walked in rings (D-40).
  - Mass conservation.
  - The exact walk-ahead (its worker copy is changed line for line with the real spreading).
  - D-31, D-38 and D-39.
- **Expected effect (to be measured):**
  - Ocean-depth split moves toward Earth's ≈ 7.5 / 15 / 77% for 0–200 / 200–3,000 / >3,000 m.
  - Shelves narrow from ~300 km everywhere to ~50–250 km on passive margins and ~1 cell on active ones.
  - Maximum sediment pile falls from 27–54 km toward ≤ ~20 km.
  - Delta plains appear as land.
  - Land fraction will drift, so `--ocean-depth` is retuned (H12).
- **Cost:** about the same as today. The walk is unchanged; only the weights and levels per cell change.

## L1 — Proposed rules

### H2: shelf, slope, apron, delta

| Part | Today | Proposed |
|---|---|---|
| Shelf radius R | 300 km for every mouth | Passive margin: R = 100 km × (Q / Q_ref)^0.25, clamped to 50–250 km (Q = the mouth's sediment this step). Active margin (a trench or arc mark within 300 km of the mouth): R = 1 cell |
| Shelf profile | Fills to −120 m everywhere within R | Ramp: fills to −(10 + 110 · d / R) m at distance d, so −10 m at the coast and −120 m at the shelf break |
| Bypass | None: the shelf fills first | 35% of the mouth's sediment goes straight to the apron through canyons and gravity flows, even when the shelf has room |
| Apron shares | Equal shares out to 900 km (2,700 km with no deep water), capped at −120 m, making plateaus | Shares ∝ e^(−d/L), with L = 150 km × (Q / Q_ref)^0.5 clamped to 75–600 km (fan length grows with supply; Sømme et al. 2009). Search radius 3L. Sloping cap: no cell above −120 m − 25 m/km × (d − R), which builds a continental slope and rise instead of a plateau |
| Delta top | Fills the whole sea to −1 m, prograding ~70 km/Myr; stays under water | Fills only cells already on the shelf ramp (the platform), to sea level + 2 m, so it becomes land and the coast moves out |
| Leftover | Whole-sea delta plain at −1 m | Open ocean: a distal drape ∝ e^(−d/L) over the apron cells, with no cap but no higher than −120 m. Seas enclosed by land (water bodies other than the largest): the whole-sea delta plain stays |

Q_ref is the median mouth supply of the step, so the scaling adapts to the planet's erosion rate. The constants are first guesses, to be tuned against the validation numbers below.

### H3: carbonate

- **Growth with depth:** rate = r₀ · e^(−z / z₀), with r₀ = 40 m/Myr and z₀ = 27 m (light-limited growth; Bosscher & Schlager 1992), from 5 m down to the 150 m floor. Today the rate is the same at any depth.
- **Clastic suppression:** no growth in cells where marine spreading deposited more than 10 m/Myr this step (river mud smothers carbonate factories).
- **Factors before animals:** microbial life × 0.8 (today 0.3), before life × 0.4 (today 0.1). Proterozoic stromatolite platforms were thick (Grotzinger 1990, cited from memory: to be checked).

### H8: mouth order

Mouths run in cell order every step, so a shared sea always goes to the north-western river first. Proposal: rotate through four scan orders, one per step (rows down or up, crossed with columns east or west). This keeps neighbouring mouths together, which a random order would not: the replay stays cache-friendly and the walk-ahead bands still work. It is deterministic and the same on any thread count.

### H9 (optional): depositional environment

A byte per cell giving the last step's main deposit: fan, floodplain, lake, inner shelf, outer shelf, slope, basin floor, pelagic, ooze, carbonate, delta plain, accretionary wedge, glaciomarine. Phases 6–7 need it for rock columns and ores. Water depth and distance to shore can be derived later. Cost: n bytes, written where deposits already happen.

### H12: sea level

Sediment on oceanic crust displaces water, so the land fraction is the difference of two large numbers. After H2 and H3, retune `--ocean-depth` so seeds 42 and 1 land near today's land fraction (or near Earth's 29% if the owner prefers), and print the displaced water.

### Validation (printed in the CLI report)

| Measure | Earth | Today (to measure) |
|---|---|---|
| Ocean area 0–200 / 200–3,000 / >3,000 m | ≈ 7.5 / 15 / 77% | — |
| Shelf width (coast to the −200 m line), histogram | mean ~80 km; passive 100–300, active < 30 | ~300 km |
| Maximum sediment thickness | ≤ ~20 km | 27–54 km |
| Delta-plain land area | small, nonzero | 0 |
| Sea level, land fraction | 29% land | 33–43% |

Runs:
- Seeds 42 and 1 at 78 km, seed 42 at 31.25 km, before and after.
- 1 vs 16 threads byte for byte.
- `./gradlew build`.

## L2 — Where things change

- `surface/SurfaceProcesses.java`: `spreadMarine` and its mirror `WalkSlot.spread`, `fillApron`, `fillTo`, `seaFloor` (carbonate), `spreadAllMarine` (mouth order). The walk-ahead flags (`AT_SHELF`, `AT_TOP`) become flags against each cell's own level.
- `surface/SurfaceConfig.java`: new constants (bypass 0.35, ramp top 10 m, fan length 150 km, slope 25 m/km, carbonate z₀ 27 m, clastic limit 10 m/Myr, factors 0.4 / 0.8).
- `geosim-cli` report: the validation table.
- Docs: new D-41 in [decisions](../design/decisions.md); [04 § Sea floor](../design/04-surface-processes.md#sea-floor).

## Questions for the owner

1. Approve H2, H3 and H8 as above? Add H9 now, or later with Phase 6?
2. Land-fraction target for the H12 retune: keep today's ~35–40%, or aim for Earth's 29%?
3. The exact speedups (same planet, faster) are finished in the same working tree. Commit them first, so this change starts from a clean, hash-checked baseline?
