# 03 · Tectonics: proto-terrain, mantle, plates, boundaries, plateaus, isostasy

> **Level 1** · Read when: you work on plate motion, boundary rules, plateaus, magmatism, mantle plumes or elevation.
> Decisions: D-21–D-26, D-32, D-36, D-38 · Related: [02 step order](02-simulation-pipeline.md#order-of-operations-in-one-step) · Go deeper: report § "Fifteen tectonic settings" (which settings must exist for ores) · [Tibetan plateau support notes](../research_notes/Tibetan%20plateau%20support/tibetan_plateau_support.md) (why thick crust stays high, rules R1–R9).

**Summary.** The mantle is a **parameterized model** (plumes, supercontinent insulation), not a convection solver. Plates are rigid bodies on the cylinder whose velocities come each step from a **force balance** (slab pull, ridge push, basal drag, collision resistance). They move over one world raster by **whole-cell shifts**, which copy data exactly and never blur it. Overlaps become subduction (of normal ocean floor) or collision (of anything buoyant); gaps become new ocean crust. Collisions build **plateaus** whose height is set by crust buoyancy, dense roots that come and go, and lateral flow of the lower crust. Elevation is never stored: it comes from **isostasy** (crust thickness, roots, ocean-floor age).

> TFC 4.2 also has "plate-like" terrain, but its plates have no history (velocities are hashes, boundaries are instantaneous). Our edge is real history: old cratons, sutures, eroded ranges, inherited rock columns.

## As built (Phase 2) vs planned

| Built (`geosim-core`, package `tectonics`) | Deferred |
|---|---|
| Proto-crust, Voronoi plates, translation by whole cells with sub-cell accumulators | Plate **rotation** (needs per-plate rasters and resampling; Phase 8 polish unless maps need it sooner) |
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

- **Degrees of freedom:** translation (vx, vy) per plate. Rotation about a vertical axis (the cylinder's analogue of an Euler pole) is deferred.
- **Forces**, summed over every cell edge between two plates:
  - *Slab pull*: the sinking plate is pulled toward the trench, ×(0.3 + age/80 Myr), capped ×1.5. The dominant driver.
  - *Ridge push*: plates on either side of young (< 20 Myr) floor are pushed apart.
  - *Collision resistance*: opposes convergence where buoyant crust meets buoyant crust, and where continents push into a **pole edge** (the cylinder's poles are walls; ocean floor pushed into them is consumed).
  - *Basal drag*: area × (1 + 2 × continental fraction).
- Target velocity = force / drag, rescaled so the area-weighted RMS speed matches the **mantle vigor** (4.5 cm/yr today), plus slow random drift (Ornstein–Uhlenbeck, 1.2 cm/yr). Velocities relax toward the target over 10 Myr; there is no net translation of the whole system.
- **Motion:** each step is split into substeps of at most 3 cells per plate. Moving a rigid plate by whole cells is an exact copy, so crust ages, thickness and marks never blur.
- **Poles:** plates pulling away from a pole leave a strip of new ocean floor along the edge (a cylinder-world artifact, accepted).

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
oceanic:     e = −(2.5 km + 0.35 km × √(age / Myr)), at most −6 km; + 0.1515 × (H − S − 7 km) for plateaus and arcs;
             + 0.396 × S (sediment fills water)
sea level:   the level at which the oceans hold the configured water volume (fast 1 m histogram fill)
```

H is the whole crust and S its sediment (2.4 g/cm³ against 2.8 for crystalline crust, so sediment floats higher). **S never exceeds H** (D-38): if a process thinned the crust under its sediment, the formula would read negative crystalline crust and raise a lone pillar to the 6.5 km cap. Before D-38 that happened (crustal flow moved crust but left the sediment, and the cap hid piles of up to 1,200 km of sediment from erosion), and it was the source of the single-cell spikes on the maps.

Planned: rifted-margin thermal subsidence, dynamic topography, flexure near trenches and mountain loads (forebulge, foreland basin).

In `--plates-only` runs an **erosion stand-in** relaxes crust thicker than 35 km toward 35 km over 200 Myr. 75% of the removed crust settles on the plate's shelves (ocean floor within 150 km of its continent) and fills its submerged continental basins; the other 25% is lost. Full runs use real erosion and deposition instead ([04](04-surface-processes.md)).

## References

- Viitanen 2012, *PlaTec* (rigid plates on a wrapping raster).
- Cortial et al. 2019, *Procedural Tectonic Planets* (subduction, collision and rifting rules).
- Forsyth & Uyeda 1975 (driving forces).
- Parsons & Sclater 1977 (ocean depth vs age).
- Dhuime et al. 2012 (continental growth curve).
- von Huene & Scholl 1991; Scholl & von Huene 2007 (subduction erosion and crustal recycling rates).
- Plateau support: England & Houseman 1989; Molnar et al. 1993; Clark & Royden 2000; Hetényi 2007; Garzione et al. 2017. Full list in the [research notes](../research_notes/Tibetan%20plateau%20support/tibetan_plateau_support.md).
