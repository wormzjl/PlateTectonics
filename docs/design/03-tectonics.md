# 03 · Tectonics: proto-terrain, mantle, plates, boundaries, plateaus, isostasy

> **Level 1** · Read when: you work on plate motion, boundary rules, plateaus, magmatism, mantle plumes or elevation.
> Decisions: D-22–D-26, D-32, D-36, D-38, D-42 (D-21 superseded) · Related: [02 step order](02-simulation-pipeline.md#order-of-operations-in-one-step) · Go deeper: report § "Fifteen tectonic settings" (which settings must exist for ores) · [Tibetan plateau support notes](../research_notes/Tibetan%20plateau%20support/tibetan_plateau_support.md) (why thick crust stays high, rules R1–R9).

**Summary.** The mantle is a **parameterized model** (plumes, supercontinent insulation), not a convection solver. Plates are rigid bodies on the cylinder whose velocities and spins come each step from a **force balance** (slab pull, ridge push, basal drag, collision resistance). They move and turn over one world raster: each plate keeps its own raster, stored as places on the world cells, and an **exact integer rotation** puts it on the world, so moving and turning copy data exactly and never blur it. Overlaps become subduction (of normal ocean floor) or collision (of anything buoyant); gaps become new ocean crust. Collisions build **plateaus** whose height is set by crust buoyancy, dense roots that come and go, and lateral flow of the lower crust. Elevation is never stored: it comes from **isostasy** (crust thickness, roots, ocean-floor age).

> TFC 4.2 also has "plate-like" terrain, but its plates have no history (velocities are hashes, boundaries are instantaneous). Our edge is real history: old cratons, sutures, eroded ranges, inherited rock columns.

## As built (Phase 2) vs planned

| Built (`geosim-core`, package `tectonics`) | Deferred |
|---|---|
| Proto-crust, Voronoi plates; plates drift and **spin** (D-42): per-plate rasters held as places on the world raster, an exact integer rotation, spin from torque | Spin of plates too wide to turn on the cylinder (bands round the world) |
| Force balance, mantle-vigor speed normalization, random drift, pole walls | Upwelling/downwelling fields, slab graveyard, dynamic topography |
| Subduction with remembered polarity; buoyant crust jams trenches; arcs; accretion | Back-arc extension, ophiolite obduction, transform-specific features |
| Collision with underthrusting, plateaus, dense roots, delamination, collapse (below) | Cratons as rigid walls, explicit drips, magma-type outputs per event |
| Plumes (LIPs), rifts, subduction initiation (margins and old ocean), merges, breakups | Rifts preferring old sutures |
| Crust growth curve; subduction erosion (Phase 3); erosion stand-in, now only for `--plates-only` runs (full runs use [04](04-surface-processes.md)) | — |

## Proto-terrain (t = 0)

- Seeded noise sets a thin basaltic **proto-crust** (7–11 km) with 5–9 thicker protocontinental nuclei (Gaussian bumps of +24 km, radius 600–1,500 km). Crust ≥ 20 km is continental (~2–4% of the surface at t = 0).
- Initial plates: 8–14 Voronoi cells warped by 700 km noise, random speeds 3–7 cm/yr.
- **Ocean volume** is a parameter (default 2,620 m mean depth over the planet, Earth-like). Sea level comes from filling basins to that volume, recomputed every step.
- Planned: initial mantle potential temperature is high (≈1,600 °C), falling over time ([07](07-life-atmosphere-sun.md)). Today a speed boost stands in for it (plates ×2 faster at t = 0, decaying over 1.5 Gyr).

## Plate kinematics

- **Degrees of freedom:** translation (vx, vy) and spin ω about a vertical axis through the plate's centre of area (the cylinder's analogue of an Euler pole), D-42. See [Rotation](#rotation-d-42).
- **Forces**, summed over every cell edge between two plates:
  - *Slab pull*: the sinking plate is pulled toward the trench, ×(0.3 + age/80 Myr), capped ×1.5. The dominant driver.
  - *Ridge push*: plates on either side of young (< 20 Myr) floor are pushed apart.
  - *Collision resistance*: opposes convergence where buoyant crust meets buoyant crust, and where continents push into a **pole edge** (the cylinder's poles are walls; ocean floor pushed into them is consumed).
  - *Basal drag*: area × (1 + 2 × continental fraction).
- Target velocity = force / drag, rescaled so the area-weighted RMS speed of all cells (spin included) matches the **mantle vigor** (4.5 cm/yr today), plus slow random drift (Ornstein–Uhlenbeck, 1.2 cm/yr, 100 Myr memory). Velocities relax toward the target over 10 Myr; there is no net translation of the whole system.
- **Motion:** each step is split into substeps in which no cell moves more than 3 cells (drift plus spin at the plate's farthest reach). Every cell lands where its plate's pose puts its place in the plate's raster, an exact copy, so crust ages, thickness and marks never blur.
- **Poles:** plates pulling away from a pole leave a strip of new ocean floor along the edge (a cylinder-world artifact, accepted).

## Rotation (D-42)

Owner request: "Approve the plate-rotation plan as written: per-plate rasters stored as (u, v) on the world raster, spin from torque with spinDragFactor, and build it now as D-42" ([plan](../reports/Plate%20rotation%20plan.md)). It replaces D-21 (translation only).

**Per-plate rasters.** Every world cell stores its place (u, v) in its plate's raster, so the plate raster is the set of its cells' places; the world raster stays the only store of crust, and climate, rivers, erosion, marine spreading, basin closure, crustal flow and roots are unchanged. A plate's pose is an angle Θ and a frame origin (whole cells plus a fraction carried between substeps, as the old shift accumulators were). A cell's world position is `origin + R_Θ(u, v)`, recomputed from scratch every substep, so small turns add up instead of rounding away.

**Exact integer rotation R_Θ** (`PlateFrame`): Θ is k quarter turns, which are exact, then a rest φ in [−45°, 45°] as three shears that each move whole rows or columns by whole cells (Paeth 1986): x₁ = u + round(−tan(φ/2)·v), y = v + round(sin φ·x₁), x = x₁ + round(−tan(φ/2)·y). Each shear is undone exactly, so the map is one-to-one at any angle with an exact inverse. A cell lands within ~1.5 cells of the exact rotation; the error depends only on the angle and the cell. Shifts use `StrictMath`.

**Turning about the centre.** Each substep Θ += ω·dt, and the origin swings round the plate's centre of area by the exact rotation (the centre is fixed in the plate's frame for the step; drift moves it). The origin can lie far from the plate (it is a point of the plate's frame, and crust is added at one side and consumed at the other); that does not change the error.

**Cells that join a plate** take the place that lands on their cell:

| Event | Place |
|---|---|
| New ocean floor (`fillGaps`) | From the neighbour it was filled from: that neighbour's unwrapped position plus one step, through the inverse map (this fixes the column across the date line) |
| Overlap winner | Keeps its own place (`copyToBack`) |
| Continent held at a pole edge | The place that lands on the edge cell; a cell of the same plate that held it landed there too and was stacked into it |
| Merge (`absorb`) | The absorbed cells go through their own pose, then the inverse of the receiver's; the two origins are taken the short way round the cylinder between the plates' centres |
| Split, breakup, initial plates | A fresh frame at angle 0, origin at the plate's centre, measured from the far side of its widest run of empty columns. Nothing moves |

**Holes from rounding.** A turned plate never has holes by itself (tested), but its raster is built up at ridges at many angles, and the inverse map skips some places between rows laid down at different angles. When a later angle lands such a place inside the plate, the world cell is a gap enclosed on all four sides by the same turned plate. It is not an opening: it takes a copy of a neighbour's crust, ocean floor if any neighbour has it (so rifted margins do not grow continent), and the place is filled for good. About 2% of the ocean floor made at ridges is filled this way, mostly within 25 Myr of the ridge; the crust budget counts what the copies hold beyond new ocean crust (`rotation rounding`). Filling them as new ocean instead left streaks of young crust inside old plates.

**Limits.**
- A plate whose reach (from its centre to the farthest corner of its frame's bounding box) reaches (W − 6)/2 cannot spin: a band round the cylinder cannot turn about a vertical axis, and the image would wrap onto itself. It gets a fresh frame at angle 0 and ω = 0, and may spin again below 90% of that limit; its frame is refreshed every eighth step in case its places went round the cylinder.
- Pole edges work as before: ocean floor turned past a pole is consumed, continent piles up against the wall.

**Spin from torque** (`PlateDynamics`):

| | Rule |
|---|---|
| Torque | τ = Σ r × F over the boundary edges and pole walls, r from the plate's centre of area to the edge's midpoint (wrapped across the date line) |
| Spin drag | Σ over the plate's cells of (1 + continentalDrag × continental) × r², times `spinDragFactor` (1.0): the drift's drag law for each cell moving at ω·r |
| Target | ω = τ / spin drag, scaled by the same factor that scales the drift; the RMS speed counts every cell, spin included |
| Random spin drift | Ornstein–Uhlenbeck with the drift's speed at the plate's radius of gyration (σ_ω = 1.2 cm/yr / r_g / `spinDragFactor`), the drift's 100 Myr memory, its own random streams (`SPIN_DRIFT`, 22 and 23) |
| Relaxation | Toward the target over 10 Myr, as drift |
| Cap | \|v\| + \|ω\| × reach ≤ 15 cm/yr; spin gives way, not drift |
| Edges | Classified by each side's local velocity v + ω × r: a spinning plate can converge on one flank and diverge on the other |
| Merges / splits | Spin combines by area / both parts keep the parent's spin and turn history |

`spinDragFactor` is the one lever for spin rates; infinity switches spin off, and then the planet is bit-for-bit the D-41 planet (checked on all 15 maps and tables for seeds 42 and 1 at 78 km, before the eclogite cap of [§ Plateaus and roots](#plateaus-and-roots), which changes planets with and without spin).

The measurements below were taken before that cap; each seed's history has changed since, but not the picture.

**Measured** (Earth-like preset, 4.5 Gyr, 2,660 m of water; "before → after" is spin off → on):

| | 78 km seed 42 | 78 km seed 1 | 31.25 km seed 42 |
|---|---|---|---|
| Spin, large plates (> 5% of the surface), mean (max) | 0.20 (0.63) °/Myr | 0.24 (0.52) °/Myr | 0.10 (0.26) °/Myr |
| Spin, smaller plates, mean (max) | 0.26 (0.43) °/Myr | — (one plate, 0) | 0.03 (0.14) °/Myr |
| RMS speed, spin alone | 2.1 of 4.7 cm/yr | 2.1 of 4.4 cm/yr | 0.9 of 3.5 cm/yr |
| Turn of the three plates with most continent, last 500 Myr | −6°, −12°, −11° | +15°, −53°, −41° | +10°, −14°, −9° |
| Continental crust / land | 38.7 / 38.2% → 35.5 / 18.3% | 38.4 / 27.1% → 34.1 / 32.4% | 41.1 / 14.2% → 44.1 / 43.4% |
| Ocean floor mean age | 86 → 93 Myr | 66 → 97 Myr | 125 → 71 Myr |
| Collision merges / breakups | 182 / 108 → 252 / 164 | 204 / 133 → 227 / 146 | 257 / 173 → 285 / 198 |
| Boundaries converging / diverging / transform (by local motion) | 20 / 27 / 53% → 27 / 24 / 50% | 26 / 22 / 52% → 20 / 29 / 51% | 18 / 30 / 52% → 25 / 24 / 51% |
| Coasts N–S / NW–SE / E–W / NE–SW | 29 / 14 / 37 / 20% → 25 / 19 / 36 / 20% | 25 / 20 / 37 / 19% → 27 / 18 / 36 / 19% | 27 / 22 / 31 / 19% → 29 / 17 / 33 / 21% |

Earth's plates turn about Euler poles at roughly 0.1–1°/Myr, with small plates faster (Iberia ~35° in the Early Cretaceous, Corsica–Sardinia ~30° in the Miocene; from memory, to check). The boundary-type shares hardly move: classed by angle, they are set mostly by boundary geometry. The E–W excess of coasts was there without spin too, so spin did not cause it and does not fix it (pole walls are a likely cause).

Land over 12 seeds at 78 km fell from 29.1% to 25.2% at 2,660 m of water, so the default water is now 2,640 m (28.1%; [04 § Sea level](04-surface-processes.md#sea-level)). Single seeds still range from ~16% to ~40%. With the eclogite cap the same 12 seeds give 31.2% (19–40%); the seed-to-seed spread is ~7 points, so a 12-seed mean is good to ~2 points and the water was left at 2,640 m.

**Cost.** Places add 2 ints per cell, double-buffered (+13 MB at 31.25 km). Spin adds 14% more substeps (6,061 → 6,931 at 31.25 km), every cell goes through the three shears in the targets pass, and each new cell through the inverse. The frame bookkeeping (centres, reach, fresh frames, merges) uses whole-number sums, exact in any order, so it runs on the worker threads. Seed 42 at 31.25 km, 16 threads, one session: plates 41 s (D-41) → 57 s; the whole world ~241 → ~245–265 s (the surface step alone varies by ±8 s between runs). At 78 km: plates 4.4 → ~6 s, the world ~18 → ~20 s.

## Boundary processes

| Situation (from overlaps and gaps) | Result |
|---|---|
| Normal ocean floor meets ocean floor | One side subducts → trench + **island arc**. The older floor sinks *when the boundary forms*; afterwards the **polarity is remembered** for as long as the plates keep converging. Without that memory the trench flips as soon as younger floor arrives and every young arc is dragged down its own trench |
| Ocean floor meets a continent, arc or plateau | Ocean subducts → trench, **arc** 80–250 km behind it (0.25 km/Myr × growth-curve factor). Crust ≥ 20 km becomes continental |
| Buoyant meets buoyant (continent, or crust ≥ 15 km: arcs, oceanic plateaus) | **Collision.** The plate with more continent overrides (Asia over India); the other side underthrusts ([plateaus](#plateaus-and-roots)). Arcs and plateaus arriving at a continent are **accreted** this way, which is how continents grow |
| Divergence | Gap filled breadth-first with new 7 km ocean crust, so the ridge sits near the middle of the opening |
| Plume head | Large igneous province: +8 km on ocean floor (an oceanic plateau), +2 km on continents, within 500 km |

**Plate reorganization** (after motion each step):

- **Merge** plates after ~300 km of convergence across their contact (overlap area > 300 km × √(smaller plate area), decaying memory τ = 60 Myr). The measure is in km, so it behaves the same at any cell size.
- **Plumes** (Poisson, 1 per 30 Myr) rise preferentially under long-lived supercontinents. They may **rift** the plate along a noisy line (900 km wiggle): 70% chance under an insulating supercontinent, 5–15% otherwise.
- **Subduction initiation**: old ocean at a passive margin (> 160 Myr) breaks away from its continent; ocean-only plates with floor > 170 Myr break along a fracture through the oldest floor and converge (Izu–Bonin style).
- Absorb plates below 0.2% of the area; break up the largest plate while fewer than 10 remain (at most 24).
- **Basin closure** (D-36): ocean floor that one plate has wrapped round, touching its continent but no other plate (a rift or back-arc sliver the plate engine left behind), has no trench to close it. It closes from its rim inward at 5 km/Myr, as small basins are squeezed shut between continental blocks. Closed cells become continental crust as thick as their continental neighbours (at most 35 km) and keep their sediment. Rift oceans touch their own ridge boundary with another plate, so they are never closed. Enclosed ocean fell from 3–8% of the surface to 0–1.4%. The regions are found as runs of one plate's ocean cells along rows (read on the worker threads), joined across rows and the date line by union-find; only enclosed ones are then flooded, in the order of their first cell. Closing one region never changes whether another is enclosed: cells of two regions touch only where their plates differ, which already makes both open.

Every boundary episode is written to the **event log** (type, time, cell, plates, value); resources ([06](06-resources.md)) read it.

## Plateaus and roots

Why Tibet stands ~5 km high and does not sink, and how the model reproduces it (evidence and rules R1–R9: [research notes](../research_notes/Tibetan%20plateau%20support/tibetan_plateau_support.md#q5-model-rules)):

- **Buoyant crust does most of the work.** 60–80 km of felsic crust floats 4–7 km high by Airy isostasy alone.
- **Dense roots can hold it down, but only for a while.** Two roots are tracked per cell:
  - **Keel:** the mantle lithosphere thickens with the crust (2.4 km per km of stacked crust, at most +100 km). It is slightly denser than the asthenosphere and loses that excess as it warms (120 Myr).
  - **Eclogite:** below 50 km, up to 35% of the lower crust turns to eclogite, denser than the mantle. The reaction needs water: in a dry terrane (85%, like India's crust under Tibet) it takes ~70 Myr, in a wet one ~2 Myr. Wetness is drawn per crustal terrane.
- **Delamination.** Where crust is ≥ 55 km and the roots hold the surface down by > 750 m, roots peel off at random (about once per 20 Myr per 200 km patch). Then:
  - the eclogite returns to the mantle (a crust sink);
  - hot asthenosphere replaces the keel, and the surface **rebounds** 1–2 km;
  - post-collisional magmas erupt (`DELAMINATION` event plus plume mark), which feeds porphyry and adakite rules later;
  - the hot lithosphere cools back over 50 Myr.
- **Eclogite never exceeds the crust it is part of.** Flow, thinning and pile-ups at a pole edge move or cut crust but leave the eclogite where it is, so a column could end up with more eclogite than crust: crust stacked against a pole wall stacks its eclogite too, and the ceiling then cuts the crust back but not the eclogite (up to 230 km of eclogite under 72 km of crust). Delamination then took the crust below zero, and slope flow spread the negative crust until a 2,000 Myr run at 31.25 km ran out of memory at ~1,700 Myr. The roots step now caps eclogite at its column's crust, and delamination removes at most the crust there is. In a 4.5 Gyr run at 78 km the cap acts ~220 times, two-thirds of them on the pole rows; histories change from the first time it acts (a chaotic system), averages hardly.
- **So a plateau *can* sink.** While a dense root stays attached, the surface stands 0.5–2 km below what its crust alone would hold (like the Andean Eastern Cordillera or the Titicaca basin). The model never makes a permanently sinking plateau, which is not observed on Earth.
- **Underthrusting** replaces "stack everything in one cell". Of the over-run crust:
  - 35% stacks at the suture (frontal range, a Himalaya);
  - 55% spreads 150–300 km behind it, under the overriding plate (Tibet);
  - 10% sinks with the slab.
- **Ceiling and lateral flow.** Gravity limits a collisional plateau to ~5.3 km above sea level. Crust above that flows to the nearest cells of the same plate, so plateaus grow **outward, flat-topped**. A cell with an eclogite root holds more crust (southern Tibet, ~75 km); one over hot mantle holds less (northern Tibet, ~60 km). Crust thicker than 50 km cannot be steeper than 0.15 km of thickness per km, so plateau margins taper over a few hundred km.
- **Flowing crust carries its sediment** (D-38). A cell that gives crust thins as a whole column, and what moves keeps the giver's share of sediment. The ceiling allows for that share: sediment is lighter, so a sediment-rich column reaches the ceiling elevation with less crust, and a cell receiving sediment-rich crust takes less of it.
- **Collapse.** Once a plate is no longer in a collision, its hot crust (over a removed keel) spreads down toward ~1 km elevation (Basin and Range). Cold crust keeps its height and only erodes.
- Safety caps: crust ≤ 88 km, cell-mean elevation ≤ 6.5 km above sea level.

## Crust growth

- Continents grow at arcs and by accreting arcs and plateaus. They lose crust when arc crust is subducted before it becomes buoyant, in collisions (10%), to delamination, and to **subduction erosion**: the sinking slab drags 35 km³ of the overriding plate's crust down per km of trench per Myr, taken at its trench cell (Earth: ~1.4 km³/yr over ~40,000 km of trench; von Huene & Scholl 1991; D-32). The rate is per km of trench, so the volume does not depend on cell size.
- Erosion itself conserves mass ([04](04-surface-processes.md)). Sediment that settles on ocean floor stays oceanic and rides to a trench, where 50% is scraped into the overriding plate's accretionary wedge and the rest subducts (D-31). The wedge builds up to sea level, a forearc high; what it cannot hold goes down with the slab (D-38).
- **Growth curve.** Arc output is steered toward an Earth-like crustal growth curve: ~70% of today's volume by 1.5 Gyr (zircon-based models, Dhuime et al. 2012), and 14.1 km³ per km² of planet by 4.5 Gyr. The planet's history is chaotic, so without this seeds would differ several-fold in land area. With it they differ in **where** continents are, not **how much** continent there is. The strength is a setting (0 = off).
- Result at 4.5 Gyr (Phase 2, plates only; seeds 42 and 7, 31 km cells): 37–40% of the surface continental, 34–35% land, 11–12 × 10⁹ km³ of continental crust, RMS plate speed ~4 cm/yr, peaks at 6.5 km.

## Isostasy and elevation

```
continental: e = 0.1515 × (H − S − 33 km) + 0.2727 × S − 0.013 × keel − 0.13 × eclogite   (km; keel < 0 = hot, pushes up)
oceanic:     e = −(2.6 km + 0.365 km × √t) for t < 20 Myr, else −(5.651 km − 2.473 km × e^(−0.0278 t))   (GDH1, D-43);
             + 0.1515 × (H − S − 7 km) for plateaus and arcs;
             + 0.396 × S (sediment fills water)
sea level:   the level at which the oceans hold the configured water volume (fast 1 m histogram fill)
```

Ocean floor follows GDH1 (Stein & Stein 1992), Earth's measured plate-cooling curve: it deepens as √age while young, then flattens toward 5.65 km (5.5 km at 100 Myr). Until D-43 the model used 2.5 + 0.35 √t km down to a 6 km floor, which kept deepening old floor 2–4× faster than Earth's and made sea level swing with the mean age of the ocean floor ([land share plan](../reports/Land%20share%20stability%20plan.md)).

H is the whole crust and S its sediment (2.4 g/cm³ against 2.8 for crystalline crust, so sediment floats higher). **S never exceeds H** (D-38): if a process thinned the crust under its sediment, the formula would read negative crystalline crust and raise a lone pillar to the 6.5 km cap. Before D-38 that happened (crustal flow moved crust but left the sediment, and the cap hid piles of up to 1,200 km of sediment from erosion), and it was the source of the single-cell spikes on the maps.

Planned: rifted-margin thermal subsidence, dynamic topography, flexure near trenches and mountain loads (forebulge, foreland basin).

In `--plates-only` runs an **erosion stand-in** relaxes crust thicker than 35 km toward 35 km over 200 Myr. 75% of the removed crust settles on the plate's shelves (ocean floor within 150 km of its continent) and fills its submerged continental basins; the other 25% is lost. Full runs use real erosion and deposition instead ([04](04-surface-processes.md)).

## References

- Viitanen 2012, *PlaTec* (rigid plates on a wrapping raster).
- Paeth 1986, *A fast algorithm for general raster rotation* (rotation by three shears).
- Cortial et al. 2019, *Procedural Tectonic Planets* (subduction, collision and rifting rules).
- Forsyth & Uyeda 1975 (driving forces).
- Parsons & Sclater 1977 (ocean depth vs age); Stein & Stein 1992 (GDH1, the depth–age curve used).
- Dhuime et al. 2012 (continental growth curve).
- von Huene & Scholl 1991; Scholl & von Huene 2007 (subduction erosion and crustal recycling rates).
- Plateau support: England & Houseman 1989; Molnar et al. 1993; Clark & Royden 2000; Hetényi 2007; Garzione et al. 2017. Full list in the [research notes](../research_notes/Tibetan%20plateau%20support/tibetan_plateau_support.md).
