# 04 · Surface processes: climate, sea level, rivers, erosion, deposition, glaciers, weathering

> **Level 1** · Read when: you work on climate, rainfall, rivers, erosion, sediment, ice or soils.
> Decisions: D-30, D-31, D-33, D-37, D-39, D-40, D-41 · Related: [02 step order](02-simulation-pipeline.md#order-of-operations-in-one-step), [07](07-life-atmosphere-sun.md) (global temperature, life), [03](03-tectonics.md) (crust, isostasy, trenches), [05](05-lithology.md) (where sediment will be recorded) · Go deeper: report § "Coal needs land plants…" (climate requirements for coal).

**Summary.** Every step, after the plates move, a **climate field** is computed from latitude, the global temperature of the box models ([07](07-life-atmosphere-sun.md)), elevation and the land–sea layout. Moisture is carried along each latitude row with the prevailing wind and, more weakly, against it, and poleward from the equator (monsoons, storm tracks); plants return part of the rain to the air, and ranges cast **rain shadows**. Rivers are routed over the land with depressions filled into **lakes**; a hollow below sea level counts as a lake too unless it belongs to a sea of at least 100,000 km². Rock erodes by an **implicit stream-power law plus a sub-grid relief term**, and the surface drops only by the isostatic share of the rock removed, because the crust rebounds. Rivers deposit what they cannot carry; the rest builds **shelves, continental slopes, turbidite fans and deltas** at sea, shaped by each river's supply and its margin. Sediment mass is conserved. **Ice sheets** grow where summers stay below 0 °C, and the water they hold lowers sea level. A **regolith** front advances with heat and rain and is stripped by erosion and ice. Every change goes through the crust thickness and sediment fields, so isostasy and sea level follow. Nothing is written to rock columns yet (Phase 6).

## As built (Phase 3) vs planned

| Built (`geosim-core`, packages `climate`, `surface`) | Deferred |
|---|---|
| Zonal temperature with lapse rate and continentality; fixed wind belts; moisture swept both ways along rows and poleward along columns; vegetation recycling and forest convection; rain shadows; aridity; 16 biome classes | Ocean heat transport and currents (cold coasts, hyper-arid deserts); wind belts that shift with global temperature; climate statistics over time |
| Ice sheets from a mass-balance rule with a plastic profile; ice water taken out of the ocean | Ice flow, orbital cycles, moraines, till, outwash, loess, fjords |
| D8 Priority-Flood routing with lakes and endorheic basins | Separate sediment and evaporites in endorheic lakes |
| Implicit stream power + sub-grid relief erosion; isostatic surface drop; transport-limited deposition; lakes trap sediment | Hillslope diffusion and landslides; erodibility by rock type (needs lithology, Phase 6) |
| Shelves, slopes, turbidite fans, deltas (D-41); pelagic clay, carbonate ooze, shallow carbonate | Heavy-mineral placers ([06](06-resources.md)); river network export to Tier 1.5 |
| Regolith depth, last glaciation time, exhumation per cell | Soil types (D-18); dune fields |

## Sea level

- The ocean holds a fixed water volume: by default a mean depth of 2,640 m over the whole planet (CLI `--ocean-depth`, world setting `ocean_depth_m`), minus the water held in ice sheets. Earth's is about 2,620 m. Since D-44 continents stand above the sea, and land is about the area of continental crust: 12 seeds at 78 km end with 31–37% land (mean 34.3%; Earth 29%), and over the last 500 Myr land varies with an SD of 2.3 points. More water barely changes the mean (2,730 m: 33.6%), because over hundreds of Myr erosion grades the land to the new sea level ([Known gaps](#known-gaps)).
- Sea level is the level at which the basins hold that volume (1 m histogram fill). It is recomputed twice per step: after the plates move, and after surface processes and ice ([02](02-simulation-pipeline.md#order-of-operations-in-one-step)).
- Young (shallow) ocean floor and ice sheets therefore move sea level without extra rules. Ocean floor deepens with age on Earth's GDH1 curve ([03](03-tectonics.md#isostasy-and-elevation)); ice counts at a sphere's area ([Glaciers](#glaciers); D-43). In the last 500 Myr (12 seeds at 78 km) sea level has an SD of 76 m and moves at most 181 m within 50 Myr (Earth: ~100–170 m in the Cretaceous): 0.83 of its variance comes from ocean-floor age and 0.13 from ice. `geosim-cli --sea-drivers` writes each part per step. The layers that transgressions and regressions leave are not recorded until rock columns exist ([05](05-lithology.md)).
- In Minecraft the present-day level sits at y = 63 (D-19).

## Climate

Computed every step (`ClimateModel`) from the global state, elevation above sea level and the previous step's erosion map.

| Part | Model |
|---|---|
| Temperature | Zonal profile around the global mean T_g: pole + (equator − pole) × cos(latitude)^1.1, with equator T_g + 12 − 0.3 (T_g − 15) °C and poles T_g − 40 + 0.5 (T_g − 15) °C (27 and −25 °C for a 15 °C Earth; the exponent fits Earth's zonal means). Warm worlds have a smaller pole-to-equator contrast. Sea surface at least −2 °C. Land: −6.5 °C per km of elevation; interiors (full effect 1,500 km from the coast) are up to 6 °C colder at high latitude and up to 2 °C warmer in the tropics |
| Seasonal range | Sea 2–10 °C; land 3 °C plus up to 22 °C toward the poles, larger inland |
| Wind | Trade winds blow west below 30°, westerlies east from 30° to 60°, polar easterlies west beyond 60°. The belts are fixed |
| Moisture | Rain over the sea follows Earth's zonal pattern (2,100 mm/yr at the equator, ~450 at 30°, ~1,050 at 55°, 150 at the poles), × e^(0.03 (T_g − 15)), × 0.1 during a snowball. Each row is swept in the prevailing wind. Over bare land the air rains out with distance (e-folding 4,000 km below 25°, 3,000 km to 60°, 1,500 km beyond); within 5° of the equator it lasts 7× longer, fading out by 15°, because the converging trade winds keep resupplying it. A second sweep runs **against** the wind at 0.6 strength and 0.6 × the decay length (monsoons, cyclones on lee coasts). Then each column is swept **poleward** from the equator, starting from what the row sweeps left there: at full strength in the tropics (summer monsoons) and 0.7 in mid-latitudes (storm tracks). Each cell keeps the wettest value. Finally three box passes each way spread the rain like a Gaussian of σ = 250 km (storms and monsoons beyond the sweep lines; the same at any cell size) |
| Vegetation | Plants return part of the rain to the air, so moisture decays over decay / (1 − share): closed forest 0.6 (Amazon-like transpiration), open vegetation 0.25, bare land 0. Cover is judged from the previous step's aridity and this step's temperature, and only once land plants (and forests) exist ([07](07-life-atmosphere-sun.md#life-milestones-stochastic)). Warm forests (above 18–24 °C) also rain up to 1.4× the zonal rate: transpiration feeds their own thunderstorms, which keeps rainforests wet |
| Rain shadows | Rising air rains more (rain × (1 + 0.02 × climb in m per km)) and dries: it loses 0.25 e-folds of moisture per km it is lifted **above the highest ground it has already crossed**, which it forgets over 1,000 km downwind. Climbing back over lower bumps wrings out little, so finer cells, which resolve more bumps, do not dry the interior more (test: mean land rain within 15% on 100, 50 and 25 km cells). Cold air holds less water (−6.5% per °C below the row's sea temperature); that caps the rain where it falls, not the moisture carried on |
| Aridity | P / PET, with PET = 60 mm/yr per °C above −3 °C (< 0.2 arid, > 0.65 humid) |
| Biomes | 16 Whittaker-style classes from temperature, precipitation and aridity (`Biome`). Phase 4 maps them to TFC biomes (D-17) |
| Land weathering | One number for the carbon thermostat ([07](07-life-atmosphere-sun.md#carbon-thermostat)): the sum over ice-free land of (P / 750 mm) × e^((T − T_g) / 13.7) × freshness, where freshness = 0.5 + 0.5 × min(1, erosion / 50 m/Myr). Normalized so an Earth-like layout gives 1 (mean cell weight 0.6, calibrated at 31.25 km). This rain already includes the vegetation feedback, so the thermostat's life-on-land factor is smaller when the climate field drives it (forests 0.7, plants 0.6, instead of 1.0 and 0.8) |

## Rivers, erosion, transport and deposition

`SurfaceProcesses` runs once per step in this order: flow → erosion (outlets first) → transport and deposition (headwaters first) → sea → sea floor → weathering. Every change goes through `Lithosphere.changeSurface`, so crust thickness, sediment thickness and elevation stay consistent.

**Flow** (`FlowRouting`)

- D8 routing over the land with depressions filled: Priority-Flood + FIFO (Barnes et al. 2014). Starting from the coast, cells are taken lowest first, and each drains to the neighbour that reached it first. Cells inside a depression take its spill level as their water surface and form a **lake** (water at least 0.5 m deep).
- **What counts as sea** (D-39): cells below sea level joined edge to edge into a water body of at least 100,000 km² (the Caspian is 371,000 km²); the largest body stays sea if none is that big. A smaller hollow below sea level with land all round, or touching the sea only at a corner, is a depression like any other: a lake that rivers fill with sediment up to its spill level before flowing on. Before D-39 such one-cell "seas" swallowed whole rivers' loads and grew into pillars.
- Runoff = P² / (P + PET); under ice, all of P. Discharge Q (km³/yr) is summed downstream.
- Each lake collects its inflow and its evaporation (PET − P over the lake area). If evaporation exceeds inflow, the lake does not spill: an **endorheic** basin, marked for future evaporites.

**Erosion** (outlets first; implicit, so any step length is stable; Braun & Willett 2013)

- Rock erosion, m/Myr = K × erodibility × Q^m × S^n, with m = 0.5 and **n = 2** (D-44). Each cell is solved for its new height y above its already-updated receiver: y + c y^n = d. Closed forms for n = 1 and n = 2; Newton steps for other n.
- **Why n = 2:** erosion then falls with the square of the slope, so gentle lowlands near sea level erode slowly while steep land erodes faster. K is set so a 2% slope erodes as before (5,000 × 0.02 = 250,000 × 0.02²). With n = 1, low continents were planed to a flat platform at sea level; small sea-level changes flooded or drained it ([land share plan](../reports/Land%20share%20stability%20plan.md)). Channel steepness in real landscapes points to n > 1 in most places (Harel, Mudd & Attal 2016).
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

**River sediment at a mouth** (mass conserved; D-40, D-41). Sea cells are taken nearest first by straight-line distance from the mouth, reached through the sea edge to edge, in rings one cell wide (a bucket queue). Shelves, fans and deltas therefore grow round; the breadth-first search used before drew diamonds, which showed as 45° coasts hundreds of km long. A neighbour lies at most one ring further out than the cell that queues it, so two buffers (this ring, the next) hold the whole queue.

Each mouth gets its own shape from its sediment this step, as a multiple q of the median mouth's:

- **Shelf radius** R = 100 km × q^0.25, between 50 and 250 km. A mouth within 300 km of a trench or an arc (an active margin) gets a shelf one cell wide.
- **Fan length** L = 100 km × q^0.5, between 50 and 400 km: bigger rivers build longer fans (Sømme et al. 2009).

The mouth's walk goes out to R or 3L, whichever is further:

1. **Shelf:** 65% of the sediment fills the cells within R, nearest first, up to a ramp 10 m deep at the mouth that slopes to the shelf break (120 m) at R.
2. **Fan:** the other 35% (canyons and gravity flows bypass the shelf), plus what the shelf could not hold, is shared over the deeper cells beyond R out to 3L in proportion to e^(−d/L). No cell builds above a continental slope that falls 50 m/km from the shelf break to 2,000 m below it, and then a rise that falls 5 m/km. A full cell passes the rest of its share on to the others (water filling).
3. **Delta top:** what is left builds the mouth cell up to 2 m above sea level. It becomes land and the coast moves out.
4. **Drape:** any rest raises the fan's slope and rise by the same height everywhere, never above the shelf break. The margin grows up and out as a wedge, not as a plateau at the shelf break.
5. **Beyond the fan:** once the fan is full to the shelf break, the shelf builds out at the shelf break, nearest first, as far as it takes. Only a sea that is full to the shelf break all over (an enclosed sea) fills on to 1 m below sea level; after that the surplus spreads over all of it as a delta plain.

The mouths take turns in one of four scan orders (rows down or up, columns east or west), rotating each step, so no direction always reaches a shared sea first.

Before D-41 every mouth built a flat shelf at −120 m out to 300 km and spread the rest evenly out to 900 km, never above −120 m: flat plateaus round every river, then a delta top that filled the sea to −1 m. Before D-40 the surplus went on as few cells as the search found, often the mouth alone: about 20% of all marine sediment piled into single cells. Small seas no longer fill up as whole basins: ocean floor that one plate wraps round closes tectonically (D-36, [03](03-tectonics.md#boundary-processes)), and hollows below sea level smaller than 100,000 km² are lakes (D-39).

**Walks made ahead (same result on any thread count).** The cells a mouth takes, and their order, depend only on which cells are sea; what earlier mouths deposited only decides where the walk stops. So the walks are made ahead on the worker threads while the spreading itself stays in turn order on one thread:

1. Mouths go in batches of up to 8,192, sized so that a batch records about 8 million cells. Each batch is cut into bands of columns, four per worker thread.
2. A worker takes a band and spreads its mouths in order, with the same code as the real spreading but on its own copy of the sea floor, and records the walk: every ring it took, one ring more, and the cells it had queued for the next ring.
3. The calling thread spreads each mouth for real along its recorded walk. Where the walk must go further than recorded, it carries the walk on from the recorded queue. That happens to ~3% of mouths at 78 km: mouths whose fan is full to the shelf break walk on past the recorded window, across the shelf they built before.

A batch records at most 16.7 million cells (~200 MB). Past that, a worker records only the mouth, and the calling thread walks it.

Recorded walks stay inside a window around the mouth: the furthest any mouth reaches (3 × the longest fan length, 1,200 km) plus 3 cells. A cell of ring r lies less than r + 1 cells from the mouth, so a window of half-width R holds rings up to R − 1. The visited marks take one byte per cell, so a worker's marks fit in its L1 cache.

**Settling in place:**

- Pelagic clay and dust: 2 m/Myr in water deeper than 5 m. It stops 5 m below sea level and never builds land.
- Carbonate ooze: +6 m/Myr above the carbonate compensation depth (4,500 m) once calcareous plankton exist ([07](07-life-atmosphere-sun.md#life-milestones-stochastic)).
- Shallow carbonate (reefs, platforms; D-41): 40 m/Myr × e^(−z / 27 m) at water depth z, in water 5–150 m deep and at least 20 °C. It grows fastest in the top few tens of metres, as light does (Bosscher & Schlager 1992). As the floor rises the rate rises with it; the step is solved exactly (u = e^(z/z₀) falls by f_s r dt / z₀), and growth stops 5 m below sea level. There is no growth where river sediment arrived faster than 10 m/Myr this step, because mud smothers the factories. Full rate once animals exist, × 0.8 with microbial life only (stromatolite platforms), × 0.4 before life.

**Sediment on ocean floor stays oceanic** (D-31). It forms a passive-margin wedge on transitional crust, rides with its plate to a trench, and is recycled there: 50% is scraped into the overriding plate's accretionary wedge, which builds up to sea level (D-38), and the rest subducts ([03](03-tectonics.md#crust-growth)). Converting thick piles to continental crust made unsinkable crust from sediment alone and inflated continental area to ~58%.

## Glaciers

Built (v0):

- Land ice where the summer temperature (annual mean + half the seasonal range) stays below 0 °C and precipitation is at least 30 mm/yr. During a snowball all land is iced.
- Thickness follows a perfectly plastic profile, H = √(2 τ d / (ρ_ice g)) with τ = 100 kPa and d the distance from the ice margin, at most 4,000 m (≈3,300 m at 500 km from the margin).
- The water equivalent (× 0.917) is taken out of the ocean before the second sea-level update of the step. Each cell's ice counts at a sphere's area for its latitude, (π/2) × cos(latitude), which averages 1 over the map (D-43). The map's linear latitude (D-7) makes polar bands 2.5× a sphere's area: a third of the map lies poleward of 60°, against 13% of a sphere. Counted at map area, ice held 33–117 m of sea level and gave half of the sea-level swings; counted at sphere area it holds 14–50 m (Earth today ~65 m). The ice on the map is unchanged.
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

`SurfaceConfig.defaults()` and `ClimateConfig.defaults()`. Any of them can be changed for one CLI run with `--set name=value` (for example `--set reliefErosionK=30` or `--set monsoonFlow=0.8`). The lapse rate and the zonal sea rain are fixed in `ClimateModel`.

| Constant | Value | Unit | Meaning |
|---|---|---|---|
| `streamPowerK` | 250,000 | m/Myr at Q = 1 km³/yr, S = 1 | Fluvial erodibility K of hard rock under forests: rock erosion = K Q^m S^n (as 5,000 with n = 1 at a 2% slope) |
| `dischargeExponent` | 0.5 | — | m |
| `slopeExponent` | 2 | — | n (1 and 2 in closed form, others by Newton steps; D-44) |
| `reliefErosionK` | 20 | m/Myr per km² | Sub-grid relief erosion k h² × wetness (h in km above sea level) |
| `transportK` | 3 × 10⁵ | km³/Myr per (km³/yr × slope) | River transport capacity K_t |
| `softSedimentFactor` | 2 | × | Sediment and soft cover erode this much faster than crystalline rock |
| `bareLandFactor` | 1.5 | × | Erodibility before land plants (1.15 with plants but no forests) |
| `glacialFactor` | 2 | × | Extra erodibility under 300 m or more of ice (linear below) |
| `shelfDepthM` | 120 | m | Shelf break: shelves slope down to this depth below sea level (D-41) |
| `shelfRadiusKm` | 100 | km | Shelf radius R on a passive margin for a river of the step's median supply |
| `shelfSupplyExponent` | 0.25 | — | R grows as (supply / median)^this … |
| `shelfRadiusMinKm`, `shelfRadiusMaxKm` | 50, 250 | km | … between these |
| `activeMarginKm` | 300 | km | A trench or arc this close to a mouth gives it a shelf one cell wide |
| `rampTopM` | 10 | m | Shelves fill to this depth at the mouth, sloping to the shelf break at R |
| `bypassFraction` | 0.35 | share | Sediment that goes straight to the fan even while the shelf has room |
| `fanLengthKm` | 100 | km | Fan e-folding length L for a river of median supply; fans reach 3L |
| `fanSupplyExponent` | 0.5 | — | L grows as (supply / median)^this … |
| `fanLengthMinKm`, `fanLengthMaxKm` | 50, 400 | km | … between these |
| `slopeMPerKm`, `slopeFootM` | 50, 2,000 | m/km, m | Fans build no higher than a continental slope this steep, down to this far below the shelf break … |
| `riseMPerKm` | 5 | m/km | … and a continental rise this steep beyond |
| `deltaTopM`, `deltaRadiusKm` | 2, 0 | m, km | The delta top builds the shelf this far above sea level, within this distance of the mouth (0: the mouth cell) |
| `pelagicMPerMyr` | 2 | m/Myr | Clay and dust settling at sea (in water deeper than 5 m) |
| `oozeMPerMyr` | 6 | m/Myr | Carbonate ooze above the CCD once calcareous plankton exist |
| `ccdDepthM` | 4,500 | m | Carbonate compensation depth |
| `carbonateMPerMyr` | 40 | m/Myr | Reef and platform carbonate at the sea surface in warm water once animals exist |
| `carbonateDecayM` | 27 | m | … falling off with depth as e^(−z / this) |
| `carbonateClasticLimitMPerMyr` | 10 | m/Myr | No carbonate where river sediment arrives faster than this |
| `carbonateMicrobialFactor`, `carbonateAbioticFactor` | 0.8, 0.4 | × | Carbonate rate with microbial life only, and before life |
| `carbonateDepthM` | 150 | m | Carbonate factories work above this depth |
| `carbonateMinTempC` | 20 | °C | … and in water at least this warm |
| `soilProductionMPerMyr` | 60 | m/Myr | Weathering-front advance into bare rock, warm and wet, under forests |
| `soilDecayDepthM` | 5 | m | Production falls by e for each this many metres of regolith |

| Climate constant | Value | Unit | Meaning |
|---|---|---|---|
| `equatorOffsetC`, `poleOffsetC` | 12, −40 | °C | Sea-level temperature at the equator and the poles minus the global mean (Earth-like mean) |
| `profileExponent` | 1.1 | — | Temperature between them follows cos(latitude)^exponent |
| `decayTropicsKm`, `decayMidKm`, `decayPolarKm` | 4,000, 3,000, 1,500 | km | Moisture e-folding distance over bare land below 25°, to 60°, beyond |
| `itczDecayFactor` | 6 | × | Equatorial moisture lasts 1 + this times longer (full within 5°, gone by 15°) |
| `forestRecycling`, `openRecycling` | 0.6, 0.25 | share | Rain that forests and open vegetation return to the air |
| `landConvection` | 0.4 | × | Warm forests rain up to 1 + this times the zonal rate |
| `reverseFlow` | 0.6 | × | Flow against the prevailing wind, relative to it |
| `monsoonFlow`, `stormTrackFlow` | 1.0, 0.7 | × | Poleward sweeps in the tropics and mid-latitudes |
| `mixingKm` | 250 | km | Spread of rain beyond the sweep lines (σ) |
| `riseDryingPerKm` | 0.25 | e-folds per km | Drying of air lifted above the highest ground it has crossed |
| `crestRelaxKm` | 1,000 | km | Distance over which the air forgets that crest |
| `earthLandWeight` | 0.6 | — | Mean weathering weight of a land cell for an Earth-like layout (normalizes land weathering) |

## Known gaps

- **Ocean-floor sediment is too thick.** At 4.5 Gyr ~1.2–1.6 × 10⁹ km³ stands on the sea floor, roughly 2–3× an Earth-scaled estimate. The marine flux is Earth-like, but sediment stays ~120 Myr on the sea floor before a trench recycles it, against Earth's ~40 Myr.
- **No ocean currents or subtropical highs**, so there are no hyper-arid desert cores or cold-coast deserts (Atacama, Namib). Land at 20–30° gets 330–390 mm/yr against Earth's ~600, yet deserts cover only 6–11% of land (Earth ~20%): cells there are dry rather than hyper-arid. Ice covers 13–21% of land (Earth ~10%). Forests and grasslands are near Earth's shares (78 km cells, 4 seeds).
- **Land is ~34%, above Earth's 29%, and shallow seas are scarce.** Since D-44 continents stand above the sea (mean land 580 m at 78 km, ~480 m at 31.25 km; Earth 656 m without ice sheets), and land is about the area of continental crust. Earth keeps ~30% of its continental crust under shallow sea, on thinned rifted margins and subsiding basins; the model does not thin continental margins, so water under 200 m covers ~3% of the ocean (Earth 7.5%), and adding water hardly helps ([plan](../reports/Land%20share%20stability%20plan.md#outcome); [review](../reports/Marine%20spreading%20review.md) H1). Before D-44, low flat continents sat at sea level and land swung between 13% and 41% by seed.
- **Weathering follows erosion** (fresh rock weathers faster), so the slower lowland erosion of D-44 weakened weathering: the Earth-like present is ~2 °C warmer than before (12 seeds at 78 km: 17.1 °C, 650 ppm CO2, O2 1.37 PAL; was 15.7 °C, 480 ppm, 1.55). `earthLandWeight` was calibrated with the old erosion law.
- **Marine spreading costs ~2× what it did before D-41** (31.25 km world ~230 s, was ~117 s): every mouth walks out to 3 fan lengths and deposits on every fan cell, on one thread. Spreading mouths whose reaches do not overlap at the same time would keep the result exact.
- No hillslope diffusion or landslides beyond the tectonic slope relaxation of thick crust ([03](03-tectonics.md#plateaus-and-roots)).
- Glaciers are v0: no ice flow, no orbital cycles, no moraines or loess.
- Endorheic lakes do not yet keep their sediment separately or make evaporites.
- Climate statistics over time ("time ever-wet", "time hyper-arid") are not accumulated yet. Coal, laterite, evaporite and supergene-copper rules need them, because those deposits depend on how long conditions lasted.
- No lithology or rock columns (Phase 6): erodibility ignores rock type, and each cell has one sediment thickness.

## References

- Barnes, Lehman & Mulla 2014 (Priority-Flood depression filling).
- Braun & Willett 2013 (implicit O(n) stream-power solver, FastScape); Yuan et al. 2019 (a deposition extension; ours is a simpler transport-limited closed form).
- Ahnert 1970; Montgomery & Brandon 2002 (erosion rate grows with local relief).
- Heimsath et al. 1997 (exponential soil production function).
- Nye 1951 (perfectly plastic ice-sheet profile).
- Cordonnier et al. 2016, *Large-scale terrain generation from tectonic uplift and fluvial erosion* (procedural-terrain precedent).
