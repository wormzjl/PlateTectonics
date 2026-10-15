# 03 · Tectonics: proto-terrain, mantle, plates, boundaries, isostasy

> **Level 1** · Read when: you work on plate motion, boundary rules, magmatism, mantle plumes or elevation.
> Related: [02 step order](02-simulation-pipeline.md#order-of-operations-in-one-step) · Go deeper: report § "Fifteen tectonic settings" (which settings must exist for ores).

**Summary.** The mantle is a **parameterized model**: plumes, a slowly evolving upwelling/downwelling pattern, and a slab "graveyard" memory. Plates are rigid bodies on the cylinder. Each step their velocities come from a **force/torque balance** (slab pull, ridge push, basal drag, collision resistance). Each plate keeps its **own raster** in its own frame, so moving it never blurs the data. Overlaps between plates become subduction or collision; gaps become new ocean crust. Elevation is never stored as the primary field: it comes from **isostasy** (crust thickness, density and ocean-floor age).

> TFC 4.2 also has "plate-like" terrain, but its plates have no history (velocities are hashes, boundaries are instantaneous). Our edge is real history: old cratons, sutures, eroded ranges, inherited rock columns.

## Proto-terrain (t = 0)

- Seeded multi-octave noise sets a thin, mostly basaltic **proto-crust** (5–15 km thick) with a few thicker protocontinental nuclei (20–30 km). Their number and size are seed-dependent.
- Initial plates come from a seeded Voronoi partition (8–20 plates), adjusted so plate boundaries follow thin crust.
- **Ocean volume** is a parameter (default: Earth-like volume scaled to the planet's area). Sea level comes from filling basins to that volume (owner's "fill the basins with water").
- Initial mantle potential temperature is high (≈1,600 °C), falling over time ([07](07-life-atmosphere-sun.md)). Hotter mantle means thicker oceanic crust, more komatiites and a faster plate turnover.

## Mantle model (not a convection solver)

| Component | Behaviour |
|---|---|
| Large-scale upwellings | Two or three broad, slowly drifting zones (analogues of Earth's LLSVPs). Plumes preferentially rise from their edges |
| Downwellings | Where subduction has persisted; stored as a decaying "slab graveyard" field. Continents drift toward them, so **supercontinents assemble over downwellings** |
| Supercontinent insulation | Under a large, long-lived continent, mantle temperature rises; plume probability grows; **rifting** follows (the supercontinent cycle, about 300–700 Myr) |
| Plumes | Poisson events in time; positions drawn from the fields above. A plume **head** makes a LIP (flood basalt, ~1 Myr, 10⁵–10⁶ km²) and may trigger rifting; its **tail** makes a hotspot track (volcanic chain) as plates move over it |
| Dynamic topography | Smooth uplift over upwellings, sag over downwellings (±0.5–1 km); added to isostatic elevation |

## Plate kinematics

- **Degrees of freedom:** on a flat cylinder a rigid plate moves by translation (vx, vz) plus rotation ω about a vertical axis. That is 3 numbers per plate, the same count as an Euler pole on a sphere.
- **Force balance** (solved as a small linear system each step):
  - *Slab pull*: along each trench, proportional to trench length × slab negative buoyancy (grows with the age of the subducting ocean floor). The dominant driver.
  - *Ridge push*: proportional to ridge length; pushes plates away from ridges.
  - *Basal drag*: −k·v·area; k larger under thick continental keels.
  - *Collision resistance*: opposes convergence where continents collide, growing with crustal thickness.
  - *Mantle traction* (small): toward downwellings.
- **Rasters:** each plate stores its fields in plate-local coordinates. The world raster is composed each step by mapping every world cell into each plate's frame (inverse transform plus sample). Plate rasters are only *sampled*, never repeatedly resampled, so there is no numerical diffusion. Phase 1 may start with translation only (as in PlaTec) and add rotation once composition works.

## Boundary processes

| Situation (detected from overlaps, gaps and relative velocity) | Result |
|---|---|
| Ocean–ocean convergence | Older, denser plate subducts → trench, **island arc** 100–200 km behind the trench, juvenile crust growth |
| Ocean–continent convergence | Ocean subducts → trench, **continental arc** (andesite, dacite, granite batholiths), accretionary wedge, possible back-arc extension if the slab rolls back |
| Continent–continent convergence | **Collision**: crust thickens toward 60–70 km, high mountains, metamorphism, suture recorded; after sustained collision the plates may **merge** |
| Divergence in ocean | Gap filled with new ocean crust (age 0, ~7 km thick), ridge |
| Divergence in continent | **Rift**: thinning, rift basin, volcanics; if extension persists → passive margins plus a new ocean |
| Transform | Lateral motion; no crust created or destroyed; pull-apart basins at bends |
| Ophiolite obduction | Occasionally, ocean crust is thrust onto a continent instead of subducting (records ophiolite rock) |

**Plate reorganization:**
- Split plates that grow too large, or that are under rift tension or a plume, along the weakest path (preferring old sutures).
- Initiate subduction at old passive margins (ocean floor older than ~100–180 Myr) with a probability that rises with age.
- Remove tiny plates by merging them.

Every boundary episode is written to the **event log** with its parameters (rate, obliquity, slab age, duration); resources ([06](06-resources.md)) depend on these.

## Crust growth

- Continental crust grows mainly at arcs (juvenile additions) and by accreting terranes. It is mostly recycled in deep time and preserved later.
- Target by default: about 40% of the surface continental by T = 4.5 Gyr (tunable). Young worlds end up with small, scattered protocontinents.

## Isostasy and elevation

```
continental: elevation = f(crustThickness, crustDensity)   (Airy; ~35 km crust ≈ near sea level, 70 km ≈ 5 km high)
oceanic:     depth = 2.5 km + 0.35 km × sqrt(oceanAge / Myr), flattening toward ~5.5–6 km after ~80 Myr
rifted margin: thermal subsidence after rifting (same √t law)
plus: dynamic topography, flexure near trenches and mountain loads (forebulge, foreland basin)
```

Erosion and deposition change thickness and load; isostasy then responds (uplift as mountains erode). This is how old ranges become low, worn-down belts.

## References

- Viitanen 2012, *PlaTec* (per-plate rasters on a wrapping world).
- Cortial et al. 2019, *Procedural Tectonic Planets* (subduction, collision and rifting rules).
- Forsyth & Uyeda 1975 (driving forces).
- Parsons & Sclater 1977 (ocean depth vs age).
