# Plate rotation plan (2026-10-16, for owner review)

Plan for letting plates turn as well as drift. Today they only translate ([D-21](../design/decisions.md); [03 § Plate kinematics](../design/03-tectonics.md#plate-kinematics)). No code is changed until the owner approves.

> **Status (2026-10-16):** the owner approved the plan as written: "per-plate rasters stored as (u, v) on the world raster, spin from torque with spinDragFactor, and build it now as D-42". Built as [D-42](../design/decisions.md) ([03 § Rotation](../design/03-tectonics.md#rotation-d-42)). Building it added a rule for rounding holes and retuned the ocean water; [§ Outcome](#outcome) has the numbers. The rest of this page is the plan as approved. This changes every planet on purpose. It stays deterministic and gives the same planet on any thread count (D-5). It would become decision D-42, replacing D-21.

## L0 — Summary

- **What changes:**
  - Each plate gets its own raster in its own frame, and a pose (position and angle) that places that raster on the world.
  - Plates spin about a vertical axis. The spin comes from the torque of the forces already used for drift (slab pull, ridge push, collision, pole walls), against basal drag, plus slow random drift.
  - Boundaries see each side's local velocity, spin included. A spinning plate can subduct on one flank while it spreads on the other, as Iberia did when the Bay of Biscay opened.
- **What stays:**
  - Moving crust is an exact copy. Nothing is resampled or blurred, and nothing is lost inside a plate.
  - The overlap and gap rules: subduction, collision, accretion, new ocean floor, remembered polarity.
  - The world raster stays the one store of crust. Climate, rivers, erosion, marine spreading, basin closure, crustal flow and roots don't change.
  - D-5 (same planet on any thread count) and D-25 (rules in km and Myr).
- **The key idea:** the plate raster's cells are placed on the world by an exact integer rotation. That is a quarter turn plus three shears, each moving whole rows or columns by whole cells (Paeth 1986). Every step of it can be undone exactly, so the map from a plate's raster to the world never puts two cells in one place or leaves a hole inside the plate, at any angle. With the spin switched off, the new code must rebuild today's planet bit for bit.
- **Expected effect** (to be measured): continents turn by tens of degrees over hundreds of Myr. There will be more oblique and transform boundaries, and back-arc and wedge-shaped ocean basins. Coasts will no longer keep the orientation they were born with.
- **Cost estimate:** plates ~39 → ~45–50 s per 31.25 km world (+3–8% of the whole run), +13 MB of memory.

## L1 — Proposed rules

### Per-plate rasters

| | Today (D-21) | Proposed |
|---|---|---|
| Where a cell lives | Only on the world raster | On the world raster, plus a place (u, v) in its plate's raster: whole-cell coordinates in the plate's own frame |
| Plate state | Velocity and a sub-cell shift accumulator | Velocity, spin ω, and a pose: angle Θ and the position of the frame origin |
| A motion substep | Every cell of a plate moves by the same whole-cell shift | Every cell moves to where the pose puts its (u, v): `world = origin + R_Θ(u, v)`, with R_Θ the exact integer rotation below |
| Inside a plate | One shift, so no two cells collide and no holes open | R_Θ is one-to-one at any angle, so the same holds |

**Exact integer rotation R_Θ.** Write Θ as k quarter turns plus a rest φ in [−45°, 45°]. The quarter turns are exact: (u, v) → (−v, u). The rest uses three shears, each shifting whole rows or columns by a whole number of cells:

1. x₁ = u + round(−tan(φ/2) · v)
2. y = v + round(sin φ · x₁)
3. x = x₁ + round(−tan(φ/2) · y)

Each shear can be undone exactly (subtract the same rounded shift), so R_Θ has an exact inverse. A cell lands at most ~1.5 cells from where an exact rotation would put it. The error is fixed for a given angle, so it never builds up.

The pose is absolute: every substep computes each cell's place afresh from its (u, v), never from its last place. Small turns therefore never round away to nothing. The shifts for each row and column are tabulated once per plate per substep on the calling thread, with `StrictMath` for tan and sin.

**Storage: the plate raster is held as (u, v) on the world raster.** Each world cell stores its (u, v), and the plate's raster is the set of its cells' (u, v). This is still a per-plate raster, only sparse. The other way to store it was rejected:

| | Recommended: (u, v) on the world raster | Rejected: dense raster per plate |
|---|---|---|
| Truth | World raster, as today | The plate rasters; the world is rebuilt from them each substep |
| Other modules (surface, flow, closure, roots, plumes) | Unchanged | Every edit on the world must be written back to the plate cell it came from |
| Exactness | Exact | Write-back is lossy wherever the world view samples a plate cell twice or skips it, so crust volume would drift with the angle |
| Memory | 2 ints per cell, double-buffered (+13 MB at 31.25 km) | One raster per plate's bounding box, ~1–2× the world |

**When a cell joins or changes plate**, its (u, v) comes from the exact inverse of that plate's pose at its world cell. That cell was not occupied by the plate, so the (u, v) is free:

- new ocean floor filling a gap (`fillGaps`);
- the winner's cell in an overlap keeps its own (u, v) (`copyToBack`), as today;
- merges (`absorb`): the absorbed cells are expressed in the receiving plate's frame;
- splits and breakups: the new plate gets a fresh frame with Θ = 0 and its origin at its centroid. Its (u, v) are its cells' world offsets, which is exact and moves nothing;
- the initial Voronoi plates: frames as for a split.

**Turning about the centroid.** The map turns about the frame origin, but a plate should spin about its centre of area, which drifts as the plate grows and shrinks. Each substep:

- Θ += ω · dt;
- the origin moves by (v + ω × (origin − centroid)) · dt.

The plate as a whole therefore turns about its centroid. The origin is kept as whole cells plus a fraction carried between substeps, exactly as `accX`/`accY` are today, so with Θ = 0 every cell lands where it lands now.

**Limits:**

- A plate wider than the world in x, minus twice the largest shear, cannot spin (ω = 0). A band right round the cylinder cannot turn about a vertical axis, and the shears would wrap onto themselves.
- Pole edges work as today: ocean floor pushed past a pole is consumed, and continent piles up against the wall. Turning into a wall meets the wall's resistance as a torque.
- The substep count also counts the spin: the fastest cell of any plate (|v| + |ω| · r_max) moves at most 3 cells per substep.

### Spin from torque

| | Today | Proposed |
|---|---|---|
| Forces | Summed per boundary edge into (Fx, Fy) per plate | Same, and also the torque τ = Σ (r × F), with r the edge's offset from the plate centroid (wrapped across the date line) |
| Drag | Area × (1 + 2 × continental fraction) | Spin drag: Σ over the plate's cells of (1 + 2 × continental) × r². This is the same drag law, applied to each cell moving at ω·r |
| Target | v = F / drag, scaled so the area-weighted RMS speed matches the mantle vigor | ω = τ / spin drag, scaled by the same factor. The RMS speed now counts every cell's speed, spin included, so overall speeds stay Earth-like |
| Random drift | 1.2 km/Myr, 10 Myr memory | Also spin drift, with the same speed at the plate's radius of gyration: σ_ω = drift / r_g. No new constant |
| Relaxation | v relaxes to the target over 10 Myr | ω the same way |
| Cap | \|v\| ≤ max speed | Also \|v\| + \|ω\| · r_max ≤ max speed (ω is cut, not v) |
| Edge classification | Closing speed from the two plates' v | From each side's local velocity v + ω × r. A spinning plate converges on one flank and diverges on the other |
| Merges | v by area | ω by area as well; relaxation handles the rest |
| Splits | The new part gets a push apart | Both parts keep the parent's ω |

New constant: `spinDragFactor` (1.0) multiplies the spin drag and divides the spin drift, as a single lever for tuning spin rates. Setting it to infinity turns spin off, drift included. Spin drift draws from its own random stream, so the other draws don't change. That also gives the exactness check: spin off must reproduce today's planet bit for bit.

### Expected numbers (to check)

These are cited from memory and still to be checked. Earth's plates rotate about Euler poles at roughly 0.1–1°/Myr. The spin about a vertical axis through a plate's centre is the part of that rotation whose pole lies under the plate, so large plates spin slowly. Small plates and microcontinents spin faster: Iberia turned ~35° in the Early Cretaceous, and Corsica–Sardinia ~30° in the Miocene. On this planet, spin from slab pull on one end of a large plate would turn its far edge at a few cm/yr, comparable to drift.

### Validation (printed in the CLI report)

| Measure | Earth (from memory, to check) | Today |
|---|---|---|
| Plate spin, °/Myr (large / small plates) | ≲ 0.5 / up to a few | 0 |
| Turn of the largest continents over the last 500 Myr | tens of degrees | 0 |
| Boundary length converging / diverging / transform | all three common; much is oblique | (to measure) |
| Coast orientations (4 direction bins) | no preferred direction | (to measure: Voronoi and drift may leave a bias) |
| Continental crust, land, ocean floor age, plate count, events | must not get worse | 41% / 14–42% / 61–125 Myr / 8–12 / … |

Checks:

- Unit tests: R_Θ is one-to-one and its inverse is exact, for random angles and shapes; turning by +Θ then −Θ gives the same raster; four quarter turns give back the same raster; motion alone conserves crust volume exactly; a symmetric layout gives zero spin; a lever (slab pull at one end) gives the expected sign.
- With spin off, the 78 km seed 42 maps match hashes recorded before the change.
- Seeds 42 and 1 at 78 km and seed 42 at 31.25 km, before and after, plus a timelapse GIF of the same seed before and after.
- 1 vs 16 threads byte for byte; `./gradlew build`.
- Land fraction re-checked over 12 seeds (H12), because the plate layouts will differ.

### Cost

| Part | Estimate at 31.25 km |
|---|---|
| Targets from (u, v): three table lookups per cell instead of one add, on the workers | +1–2 s |
| Copies: 2 more ints per cell (14 fields instead of 12) | +~1 s |
| More substeps: spin adds speed at plate edges (motion is ~40% of plate time) | +3–6 s |
| Torque, spin drag, inverse maps for new cells | negligible |
| **Total** | plates ~39 → ~45–50 s; whole run ~230 → ~240 s |

### Risks

- The ~1.5-cell rounding of the shears can leave small steps on straight coasts. They are fixed for a given angle and don't build up.
- Spin may make layouts busier: more splits and merges, and more crust piled at the pole walls. The event counts and the pole-loss budget will show it.
- The land fraction will move again (it is chaotic per seed, see D-41), so the ocean water is re-checked.

### Open questions for the owner

1. Approve per-plate rasters stored as (u, v) on the world raster, rather than dense per-plate arrays?
2. Approve spin from torque with the drift's drag law and spin drift (only new constant: `spinDragFactor`)? Or start with random spin drift only?
3. When: now, before chunk generation continues, or in Phase 8 as the roadmap has it? Every planet changes, so maps already looked at will differ.

## L2 — Where things change

- `tectonics/Plate.java`: spin ω, spin drift, angle Θ, frame origin (float, replaces `accX`/`accY`), polar moment and largest radius; per-substep shift tables.
- New `tectonics/PlateFrame.java`: the exact integer rotation (quarter turns + three shears), forward and inverse, and its shift tables.
- `tectonics/Lithosphere.java`: (u, v) arrays and their back buffers, in `copyToBack` and `swap`.
- `tectonics/PlateMotion.java`: targets from (u, v) and the pose (the parallel targets pass); new ocean cells get (u, v) from the inverse map; pole clamp and overlap rules unchanged.
- `tectonics/PlateDynamics.java`: torque, spin drag, spin target and drift, RMS speed with spin, local velocities on edges, the edge-speed cap.
- `tectonics/PlateSimulation.java`: pose integration, substep count with spin, `refreshStats` (polar moment and largest radius, from offsets wrapped about the centroid, in a second pass), initial frames.
- `tectonics/PlateReorganizer.java`: frames and spin in `absorb`, splits, breakups and tiny-plate absorption.
- `tectonics/TectonicsConfig.java`: `spinDragFactor`.
- CLI report: spin rates by plate size, turn of the largest continents, boundary-type shares, coast orientations. Plate maps: a spin marker per plate.
- Tests: `PlateFrame` (one-to-one, inverse, quarter turns, ±Θ), motion volume conservation, spin-off hashes, torque sign, D-5.
- Docs: new D-42 (replaces D-21) in [decisions](../design/decisions.md); [03 § Plate kinematics](../design/03-tectonics.md#plate-kinematics) and its as-built table; [09](../design/09-roadmap.md) (Phase 2 row and Phase 8 item); README.

## References

- Paeth, A. W. (1986). A fast algorithm for general raster rotation. *Graphics Interface '86*. Rotation by three shears, each moving whole rows or columns.
- Viitanen, L. (2012). Physically based terrain generation: procedural heightmap generation using plate tectonics (PlaTec). One raster per plate.
- Rotation examples (Iberia, Corsica–Sardinia) and Euler rates are from memory, to be checked against the literature before the numbers are used for tuning.

## Outcome

Measured with the CLI report on the Earth-like preset (78 km seeds 42 and 1, 31.25 km seed 42; 12 seeds at 78 km for the land fraction).

**Checks:**

- With `spinDragFactor` infinite, maps, history and events are byte-identical to D-41 (78 km seeds 42 and 1, 31.25 km seed 42).
- With spin on, 1 and 16 threads give byte-identical maps at 78 and 31.25 km.
- 54 tests pass, 7 of them new: the rotation is one-to-one with an exact inverse and within 2 cells of the exact rotation at every angle tried; quarter and full turns are exact; a turned disk has no holes; turning a plate and turning it back puts every cell where it was; after every step of an 800 Myr run, every cell sits where its plate's pose puts it and no two cells of a plate share a place; slab pull at one end spins a plate the right way; infinite spin drag gives no spin.

**Changes made while building** (all in D-42 and [03 § Rotation](../design/03-tectonics.md#rotation-d-42)):

| Part | As planned | As built | Why |
|---|---|---|---|
| Holes inside plates | None expected ("never leaves a hole inside the plate") | A gap enclosed on all four sides by one turned plate takes a copy of a neighbour's crust, ocean floor if any neighbour has it | A turned plate has no holes by itself, but its raster is built up at ridges at many angles, and the inverse map skips some places between rows laid down at different angles. ~2% of the ridge-made floor surfaces later as single-cell holes, 62% of them within 25 Myr of the ridge. Filled as new ocean they left streaks of young crust inside old plates. Copying the first neighbour, continent included, made 16,700 continental holes in a run; preferring ocean floor cut that to 5,200 |
| Origin motion | origin += (v + ω × (origin − centroid)) · dt | The origin swings round the centre of area by the exact rotation | The origin is a point of the plate's frame and can end up far from the plate (crust is added at one side and consumed at the other); a first-order step would drift there |
| Reach | Largest radius | Farthest corner of the frame's bounding box from the centre (an upper bound) | One pass with the other sums |
| Too-wide plates | Cannot spin | Cannot spin; fresh frame at angle 0 when they stop, then every eighth step; spin again below 90% of the limit | Reframing every step cost ~3% of the plate time |
| Spin drift | Table row "1.2 km/Myr, 10 Myr memory" | The drift's 1.2 cm/yr (12 km/Myr) and 100 Myr memory; relaxation over 10 Myr | The plan's row mixed up the units and the two times |
| Frame bookkeeping | (not specified) | Centres, reach, fresh frames and merges run on the worker threads, from whole-number sums | Exact in any order; plates at 31.25 km 62.5 → 57.0 s |
| Ocean water (H12) | Re-check | 2,640 m (was 2,660 m) | Land over 12 seeds at 78 km fell from 29.1% to 25.2% at 2,660 m; 2,620 m gives 32.8%, 2,640 m 28.1% (20–39%) |

**Validation, spin off → on** (2,660 m of water):

| Measure | Earth (from memory, to check) | 78 km seed 42 | 78 km seed 1 | 31.25 km seed 42 |
|---|---|---|---|---|
| Spin of plates over 5% of the surface, mean (max) | ≲ 0.5 °/Myr | 0.20 (0.63) °/Myr | 0.24 (0.52) °/Myr | 0.10 (0.26) °/Myr |
| Spin of smaller plates, mean (max) | up to a few °/Myr | 0.26 (0.43) | (one plate, 0) | 0.03 (0.14) |
| RMS speed, spin alone / all | — | 2.1 / 4.7 cm/yr | 2.1 / 4.4 cm/yr | 0.9 / 3.5 cm/yr |
| Turn of the three plates with most continent, last 500 Myr | tens of degrees | −6°, −12°, −11° | +15°, −53°, −41° | +10°, −14°, −9° |
| Boundaries converging / diverging / transform | all common | 20 / 27 / 53 → 27 / 24 / 50% | 26 / 22 / 52 → 20 / 29 / 51% | 18 / 30 / 52 → 25 / 24 / 51% |
| Coasts N–S / NW–SE / E–W / NE–SW | no preference | 29 / 14 / 37 / 20 → 25 / 19 / 36 / 20% | 25 / 20 / 37 / 19 → 27 / 18 / 36 / 19% | 27 / 22 / 31 / 19 → 29 / 17 / 33 / 21% |
| Continental crust / land | ~40 / 29% | 38.7 / 38.2 → 35.5 / 18.3% | 38.4 / 27.1 → 34.1 / 32.4% | 41.1 / 14.2 → 44.1 / 43.4% |
| Ocean floor, mean age | ~64 Myr | 86 → 93 Myr | 66 → 97 Myr | 125 → 71 Myr |
| Collision merges / breakups | — | 182 / 108 → 252 / 164 | 204 / 133 → 227 / 146 | 257 / 173 → 285 / 198 |
| Rounding holes, share of ridge-made floor | — | 2.3% | 2.2% | 1.7% |
| Substeps | — | 2,630 → 3,083 | — → 3,097 | 6,061 → 6,931 |
| Plates / world time, 16 threads | — | 4.4 → ~6 s / 18 → 20 s | 4.8 → ~6 s / 18 → 20 s | 41 → 57 s / 241 → ~245–265 s |

What the numbers say:

- **Plates spin at Earth-like rates** and continents turn by tens of degrees in 500 Myr. Spin is a sizeable share of motion: a fifth to a quarter of the kinetic energy at 78 km, 7% in the 31.25 km run.
- **Layouts are busier:** more collision merges and breakups, as the risks section expected. The extra collisions also raise delaminations by 40–55%.
- **Boundary-type shares hardly move.** Classed by angle between relative motion and the boundary, they are set mostly by boundary geometry, so this measure says little about spin.
- **The E–W excess of coasts is not from translation-only motion:** it is there with and without spin, so another cause (most likely the pole walls) remains.
- **Land and ocean-floor age swing both ways by seed**, as before (chaotic histories). The 12-seed land mean moved by 4 points, so the water was retuned.
- **Cost** came in above the estimate (+6–11 s planned for the plates at 31.25 km, +16 s measured): 14% more substeps, the three shears for every cell in the targets pass (~5 s), and the inverse map for every new cell.

Tried and dropped: giving a ridge gap cell to the plate whose raster has a notch there, to stop holes forming (hole share 2.0–2.4% → 2.0–2.1%, slower).

**Found afterwards:** a 2,000 Myr run at 31.25 km ran out of memory at ~1,700 Myr. Crust piled against a pole edge left more eclogite than crust behind, and delamination took the column below zero. The bug predates D-42; eclogite is now capped at its column's crust ([03 § Plateaus and roots](../design/03-tectonics.md#plateaus-and-roots)). The tables above were measured before the fix. After it, the 12 seeds at 78 km average 31.2% land at 2,640 m (19–40%), within seed noise of 28.1%.
