# 04 · Surface processes: climate, sea level, rivers, erosion, deposition, glaciers, weathering

> **Level 1** · Read when: you work on climate, rainfall, rivers, erosion, sediment, ice or soils.
> Decisions: D-30, D-31, D-33 · Related: [02 step order](02-simulation-pipeline.md#order-of-operations-in-one-step), [07](07-life-atmosphere-sun.md) (global temperature, life), [03](03-tectonics.md) (crust, isostasy, trenches), [05](05-lithology.md) (where sediment will be recorded) · Go deeper: report § "Coal needs land plants…" (climate requirements for coal).

**Summary.** Every step, after the plates move, a **climate field** is computed from latitude, the global temperature of the box models ([07](07-life-atmosphere-sun.md)), elevation and the land–sea layout. Moisture is carried along each latitude row with the prevailing wind and, more weakly, against it, so ranges cast **rain shadows**. Rivers are routed over the land with depressions filled into **lakes**. Rock erodes by an **implicit stream-power law plus a sub-grid relief term**, and the surface drops only by the isostatic share of the rock removed, because the crust rebounds. Rivers deposit what they cannot carry; the rest builds **shelves, turbidite aprons and deltas** at sea. Sediment mass is conserved. **Ice sheets** grow where summers stay below 0 °C, and the water they hold lowers sea level. A **regolith** front advances with heat and rain and is stripped by erosion and ice. Every change goes through the crust thickness and sediment fields, so isostasy and sea level follow. Nothing is written to rock columns yet (Phase 6).

## As built (Phase 3) vs planned

| Built (`geosim-core`, packages `climate`, `surface`) | Deferred |
|---|---|
| Zonal temperature with lapse rate and continentality; fixed wind belts; two-way moisture sweep with rain shadows; aridity; 16 biome classes | Ocean heat transport; wind belts that shift with global temperature; climate statistics over time |
| Ice sheets from a mass-balance rule with a plastic profile; ice water taken out of the ocean | Ice flow, orbital cycles, moraines, till, outwash, loess, fjords |
| D8 Priority-Flood routing with lakes and endorheic basins | Separate sediment and evaporites in endorheic lakes |
| Implicit stream power + sub-grid relief erosion; isostatic surface drop; transport-limited deposition; lakes trap sediment | Hillslope diffusion and landslides; erodibility by rock type (needs lithology, Phase 6) |
| Shelves, turbidite aprons, deltas; pelagic clay, carbonate ooze, shallow carbonate | Heavy-mineral placers ([06](06-resources.md)); river network export to Tier 1.5 |
| Regolith depth, last glaciation time, exhumation per cell | Soil types (D-18); dune fields |

## Sea level

- The ocean holds a fixed water volume: a mean depth of 2,620 m over the whole planet (CLI `--ocean-depth`), minus the water held in ice sheets.
- Sea level is the level at which the basins hold that volume (1 m histogram fill). It is recomputed twice per step: after the plates move, and after surface processes and ice ([02](02-simulation-pipeline.md#order-of-operations-in-one-step)).
- Young (shallow) ocean floor and ice sheets therefore move sea level without extra rules. The layers that transgressions and regressions leave are not recorded until rock columns exist ([05](05-lithology.md)).
- In Minecraft the present-day level sits at y = 63 (D-19).

## Climate

Computed every step (`ClimateModel`) from the global state, elevation above sea level and the previous step's erosion map.

| Part | Model |
|---|---|
| Temperature | Zonal profile around the global mean T_g, blended by sin² of latitude: equator T_g + 13 − 0.3 (T_g − 15) °C, poles T_g − 32 + 0.5 (T_g − 15) °C. Warm worlds have a smaller pole-to-equator contrast. Sea surface at least −2 °C. Land: −6.5 °C per km of elevation; interiors (full effect 1,500 km from the coast) are up to 6 °C colder at high latitude and up to 2 °C warmer in the tropics |
| Seasonal range | Sea 2–10 °C; land 3 °C plus up to 22 °C toward the poles, larger inland |
| Wind | Trade winds blow west below 30°, westerlies east from 30° to 60°, polar easterlies west beyond 60°. The belts are fixed |
| Moisture | Rain over the sea follows Earth's zonal pattern (2,100 mm/yr at the equator, ~450 at 30°, ~1,050 at 55°, 150 at the poles), × e^(0.03 (T_g − 15)), × 0.1 during a snowball. Each row is swept in the prevailing wind. Over land the air rains out with distance (e-folding 2,500 km in the tropics, where forests recycle rain; 1,500 km in mid-latitudes; 1,000 km near the poles). A second sweep runs **against** the wind at 0.6 strength and 0.6 × the decay length (monsoons, cyclones on lee coasts); each cell keeps the wetter value. A light north–south smoothing follows |
| Rain shadows | Rising air rains more (rain × (1 + 0.02 × climb in m per km)) and dries: it loses 0.45 e-folds of moisture per km climbed. Cold air holds less water (−6.5% per °C below the row's sea temperature) |
| Aridity | P / PET, with PET = 60 mm/yr per °C above −3 °C (< 0.2 arid, > 0.65 humid) |
| Biomes | 16 Whittaker-style classes from temperature, precipitation and aridity (`Biome`). Phase 4 maps them to TFC biomes (D-17) |
| Land weathering | One number for the carbon thermostat ([07](07-life-atmosphere-sun.md#carbon-thermostat)): the sum over ice-free land of (P / 750 mm) × e^((T − T_g) / 13.7) × freshness, where freshness = 0.5 + 0.5 × min(1, erosion / 50 m/Myr). Normalized so an Earth-like layout gives 1 |

## Rivers, erosion, transport and deposition

`SurfaceProcesses` runs once per step in this order: flow → erosion (outlets first) → transport and deposition (headwaters first) → sea → sea floor → weathering. Every change goes through `Lithosphere.changeSurface`, so crust thickness, sediment thickness and elevation stay consistent.

**Flow** (`FlowRouting`)

- D8 routing over the land with depressions filled: Priority-Flood + FIFO (Barnes et al. 2014). Starting from the coast, cells are taken lowest first, and each drains to the neighbour that reached it first. Cells inside a depression take its spill level as their water surface and form a **lake** (water at least 0.5 m deep).
- Runoff = P² / (P + PET); under ice, all of P. Discharge Q (km³/yr) is summed downstream.
- Each lake collects its inflow and its evaporation (PET − P over the lake area). If evaporation exceeds inflow, the lake does not spill: an **endorheic** basin, marked for future evaporites.

**Erosion** (outlets first; implicit, so any step length is stable; Braun & Willett 2013)

- Rock erosion, m/Myr = K × erodibility × Q^m × S^n. Each cell is solved for its new height y above its already-updated receiver: y + c y^n = d. Closed forms for n = 1 and n = 2; Newton steps for other n.
- **Sub-grid relief term**: rock erosion += k × h² × wetness × erodibility, with h = km above sea level and wetness = runoff / 0.5 m (clamped 0.2–2). A 31 km cell cannot resolve the steep slopes that do most of the erosion in mountains; local relief, and with it erosion, grows with elevation (Ahnert 1970; Montgomery & Brandon 2002).
- Erodibility: soft sediment (more than 10 m of cover) × 2; before land plants × 1.5, with plants but no forests × 1.15, with forests × 1; under ice × (1 + 2 × min(1, ice / 300 m)).
- **Isostatic drop.** Removed rock lets the crust rebound, so the surface drops only by a share of the rock removed: 15% for crystalline crust, 27% for sediment on continental crust (40% for sediment on oceanic crust). Old ranges wear down over hundreds of Myr while young ones stay sharp. A cell never drops below its receiver, and lakes do not erode.
- Sediment is removed first. Only basement removed below the sediment counts as **exhumation** (how deep today's basement surface once lay; it drives ore preservation in [06](06-resources.md)).

**Transport and deposition** (headwaters first)

- Capacity per step: κ × S, with κ = K_t × Q × dt. Where the incoming load exceeds it, the river deposits just enough that the steepened channel carries the rest. That has a closed form: deposit = (load − κ S₀) / (1 + κ × f_s / (cell area × length)), with S₀ the slope before deposition and f_s the isostatic share for sediment. This builds alluvial fans at mountain fronts and floodplains.
- A deposit never raises a cell above the lowest cell that feeds it.
- Lakes trap sediment until they are filled to their water surface.
- What reaches the coast goes to the river mouth ([below](#sea-floor)).

## Sea floor

**River sediment at a mouth** (mass conserved):

1. **Shelf:** ocean cells within 300 km of the mouth fill to 120 m below sea level.
2. **Turbidite aprons:** the rest spreads evenly over deeper cells out to 900 km. If no deep water lies within 900 km, the search continues to 2,700 km.
3. **Delta top:** in an enclosed sea with no deep water in reach, the cells fill to 1 m below sea level.
4. **Delta plain:** any surplus is deposited at the mouth and builds land.

Small ocean basins enclosed by continental blocks (rift and back-arc slivers left by the plate engine) fill up this way and become land on oceanic crust: 3–8% of the planet's surface in tuning runs. That is like the Pannonian Basin, and it makes continents more coherent.

**Settling in place:**

- Pelagic clay and dust: 2 m/Myr in water deeper than 5 m. It stops 5 m below sea level and never builds land.
- Carbonate ooze: +6 m/Myr above the carbonate compensation depth (4,500 m) once calcareous plankton exist ([07](07-life-atmosphere-sun.md#life-milestones-stochastic)).
- Shallow carbonate (reefs, platforms): 40 m/Myr in water 5–150 m deep and at least 20 °C. Full rate once animals exist, × 0.3 with microbial life only, × 0.1 before life. Growth stops 5 m below sea level.

**Sediment on ocean floor stays oceanic** (D-31). It forms a passive-margin wedge on transitional crust, rides with its plate to a trench, and is recycled there: 50% is scraped into the overriding plate's accretionary wedge, the rest subducts ([03](03-tectonics.md#crust-growth)). Converting thick piles to continental crust made unsinkable crust from sediment alone and inflated continental area to ~58%.

## Glaciers

Built (v0):

- Land ice where the summer temperature (annual mean + half the seasonal range) stays below 0 °C and precipitation is at least 30 mm/yr. During a snowball all land is iced.
- Thickness follows a perfectly plastic profile, H = √(2 τ d / (ρ_ice g)) with τ = 100 kPa and d the distance from the ice margin, at most 4,000 m (≈3,300 m at 500 km from the margin).
- The water equivalent (× 0.917) is taken out of the ocean before the second sea-level update of the step.
- Under ice: erodibility up to × 3 (at 300 m or more of ice), regolith stripped where ice is over 100 m thick (e-folding 0.5 Myr), and `lastGlaciated` recorded per cell.

Planned:

- Ice flow (shallow-ice approximation), erosion proportional to sliding speed, **fjords** at coasts; moraines, till sheets, outwash plains and loess downwind.
- Orbital-like cycles (~0.04–0.1 Myr periods) in the last 10 Myr, which needs the finer era steps of [02](02-simulation-pipeline.md#time-stepping). Only the last few glacial cycles shape the visible landscape; Tier 1.5 carves U-shaped valleys, cirques, arêtes, eskers and drumlins from the recorded fields.

## Weathering and soils

- A regolith front advances at P = 60 m/Myr × life × wetness × e^((T − 15) / 13.7), with life = 0.25 bare rock, 0.35 microbial mats, 0.8 land plants, 1.0 forests, and wetness = min(3, runoff / 0.5 m).
- Production slows under its own cover and erosion strips the top: dH/dt = P e^(−H / h₀) − E, with h₀ = 5 m and E the rock erosion rate (Heimsath et al. 1997). The step is solved exactly (substituting u = e^(H / h₀)), so 10 Myr steps are safe.
- The equilibrium depth is h₀ ln(P / E). Fast erosion keeps profiles thin; deep **laterite** profiles (bauxite, Ni laterite, Fe caps, kaolin, supergene copper; [06](06-resources.md)) grow only on old, flat, tropical land.
- Ice over 100 m thick strips the profile. Burial and a later arid climate will preserve profiles once rock columns exist.
- Planned: the final regolith depth plus climate give the soil type for Tier 2 surface blocks (TFC soils; D-18).

## Wind (deserts) — planned

Arid cells with a sand supply (deltas, beaches, eroded sandstone) become dune fields (Tier 2 decoration). Loess forms downwind of glacial outwash.

## Constants

`SurfaceConfig.defaults()`. Any of them can be changed for one CLI run with `--set name=value` (for example `--set reliefErosionK=30`). Climate constants (lapse rate, reverse-flow strength 0.6, rise drying 0.45 per km) are fixed in `ClimateModel`.

| Constant | Value | Unit | Meaning |
|---|---|---|---|
| `streamPowerK` | 5,000 | m/Myr at Q = 1 km³/yr, S = 1 | Fluvial erodibility K of hard rock under forests: rock erosion = K Q^m S^n |
| `dischargeExponent` | 0.5 | — | m |
| `slopeExponent` | 1 | — | n (1 and 2 in closed form, others by Newton steps) |
| `reliefErosionK` | 20 | m/Myr per km² | Sub-grid relief erosion k h² × wetness (h in km above sea level) |
| `transportK` | 3 × 10⁵ | km³/Myr per (km³/yr × slope) | River transport capacity K_t |
| `softSedimentFactor` | 2 | × | Sediment and soft cover erode this much faster than crystalline rock |
| `bareLandFactor` | 1.5 | × | Erodibility before land plants (1.15 with plants but no forests) |
| `glacialFactor` | 2 | × | Extra erodibility under 300 m or more of ice (linear below) |
| `shelfDepthM` | 120 | m | River sediment builds shelves up to this depth below sea level |
| `shelfRadiusKm` | 300 | km | Shelf building reaches this far from a river mouth |
| `apronRadiusKm` | 900 | km | The rest spreads as turbidite aprons this far out |
| `pelagicMPerMyr` | 2 | m/Myr | Clay and dust settling at sea (in water deeper than 5 m) |
| `oozeMPerMyr` | 6 | m/Myr | Carbonate ooze above the CCD once calcareous plankton exist |
| `ccdDepthM` | 4,500 | m | Carbonate compensation depth |
| `carbonateMPerMyr` | 40 | m/Myr | Reef and platform carbonate in warm shallow seas once animals exist (less before) |
| `carbonateDepthM` | 150 | m | Carbonate factories work above this depth |
| `carbonateMinTempC` | 20 | °C | … and in water at least this warm |
| `soilProductionMPerMyr` | 60 | m/Myr | Weathering-front advance into bare rock, warm and wet, under forests |
| `soilDecayDepthM` | 5 | m | Production falls by e for each this many metres of regolith |

## Known gaps

- **Ocean-floor sediment is too thick.** At 4.5 Gyr ~1.2–1.6 × 10⁹ km³ stands on the sea floor, roughly 2–3× an Earth-scaled estimate. The marine flux is Earth-like, but sediment stays ~120 Myr on the sea floor before a trench recycles it, against Earth's ~40 Myr.
- Moisture is swept along rows, so precipitation maps show east–west streaks.
- No hillslope diffusion or landslides beyond the tectonic slope relaxation of thick crust ([03](03-tectonics.md#plateaus-and-roots)).
- Glaciers are v0: no ice flow, no orbital cycles, no moraines or loess.
- Endorheic lakes do not yet keep their sediment separately or make evaporites.
- Climate statistics over time ("time ever-wet", "time hyper-arid") are not accumulated yet. Coal, laterite, evaporite and supergene-copper rules need them, because those deposits depend on how long conditions lasted.
- No lithology or rock columns (Phase 6): erodibility ignores rock type, and each cell has one sediment thickness.
- The cell-size dependence of the flooded-continent share is still being checked (O-6).

## References

- Barnes, Lehman & Mulla 2014 (Priority-Flood depression filling).
- Braun & Willett 2013 (implicit O(n) stream-power solver, FastScape); Yuan et al. 2019 (a deposition extension; ours is a simpler transport-limited closed form).
- Ahnert 1970; Montgomery & Brandon 2002 (erosion rate grows with local relief).
- Heimsath et al. 1997 (exponential soil production function).
- Nye 1951 (perfectly plastic ice-sheet profile).
- Cordonnier et al. 2016, *Large-scale terrain generation from tectonic uplift and fluvial erosion* (procedural-terrain precedent).
