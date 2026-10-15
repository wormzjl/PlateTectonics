# 04 · Surface processes: climate, sea level, rivers, erosion, deposition, glaciers, weathering

> **Level 1** · Read when: you work on climate, rainfall, rivers, erosion, sediment, ice or soils.
> Related: [07](07-life-atmosphere-sun.md) (global temperature drivers), [05](05-lithology.md) (where sediment is recorded) · Go deeper: report § "Coal needs land plants…" (climate requirements for coal).

**Summary.** Climate is recomputed every few steps from latitude, the Sun, greenhouse level, elevation and land–sea layout. Wind belts carry moisture inland, producing rain shadows. Rivers, erosion and deposition use the **FastScape** stream-power solver, which stays stable at million-year time steps and is linear in grid size. Sediment is routed downstream and deposited in basins, deltas and on continental shelves, and every deposit is written into the rock columns. Glaciers erode where ice accumulates; their landforms are carved in detail later (Tier 1.5).

## Sea level

- The ocean volume is fixed by the box model ([07](07-life-atmosphere-sun.md)) minus ice-sheet volume.
- Each step: sort cell elevations (or use a histogram), find the level whose ocean volume matches, and shift so the present-day final level lands at y = 63.
- Mid-ocean ridge volume changes and ice ages move sea level, causing **transgressions and regressions** that leave alternating marine and terrestrial layers.

## Climate (every 1–5 steps; finer in the last 10 Myr)

| Step | Model |
|---|---|
| Energy in | Insolation by latitude × solar luminosity L(t) (~70% of today's at t = 0; [07](07-life-atmosphere-sun.md)) |
| Global temperature | Energy balance with greenhouse forcing from pCO₂ ([07](07-life-atmosphere-sun.md)) and ice–albedo feedback, which allows **snowball** episodes |
| Temperature field | Latitude profile + lapse rate (6.5 °C/km) + continentality (seasonal range grows inland) + simple ocean heat transport |
| Winds | Latitude bands: trade winds (0–30°), westerlies (30–60°), polar easterlies (60–90°); bands shift with global temperature |
| Moisture | Advected along the wind from ocean cells; precipitation from upslope lift (orographic rain) and convergence; **rain shadows** behind ranges; drying with distance inland |
| Outputs | Mean annual temperature, seasonal range, precipitation, aridity index, Köppen-like class (biomes, soils, ore gates) |

Climate statistics are accumulated over time (for example "time spent ever-wet" and "time spent hyper-arid"), because coal, laterites, evaporites and supergene copper depend on how long conditions lasted, not only on the final state.

## Rivers, erosion, transport and deposition

- **Flow routing:** a priority-flood fills depressions; lakes keep their surface and become sinks in arid climates (endorheic basins → evaporites). Single-direction steepest-descent receivers, with the drainage area weighted by precipitation (runoff).
- **Fluvial incision:** stream power `dh/dt = U − K·A^m·S^n` (m ≈ 0.4–0.5, n ≈ 1), solved implicitly with the O(n) Braun & Willett (2013) algorithm.
  - K (erodibility) depends on rock type (hard granite vs soft shale), climate, and **vegetation**: bare land erodes faster, so erosion rates drop once land plants appear ([07](07-life-atmosphere-sun.md)).
- **Sediment:** the Yuan et al. (2019) extension with deposition coefficient G. This yields alluvial fans at mountain fronts, floodplains, and deltas at river mouths.
- **Hillslopes:** linear diffusion (soil creep), with a critical slope for landslides on steep terrain.
- **Marine:** sediment entering the ocean spreads by diffusion over the shelf and slope (deltas, shelves, turbidite fans). In warm, shallow, clear water, **carbonate** production adds limestone; reefs appear once life allows ([07](07-life-atmosphere-sun.md)). Pelagic chalk and chert accumulate in deep water.
- **Heavy minerals:** placer-forming minerals (gold, cassiterite, diamond…) released from eroded deposits travel with sediment and concentrate by density ([06](06-resources.md)).
- **Outputs to Tier 1.5:** the river network (channel cells, discharge, order), lake polygons, and the delta and fan map, so rivers stay continuous across chunks.

## Glaciers

- Ice accumulates where mean temperature and snowfall allow (mass-balance rule), then flows with a simplified shallow-ice approximation (diffusion of ice thickness).
- Erosion is proportional to sliding speed: valleys deepen and widen, and **fjords** form at coasts. Deposition: moraines at ice margins, till sheets, outwash plains, and loess downwind.
- **Visible record:** only the last few glacial cycles matter for the landscape. The last 10 Myr runs short steps with orbital-like cycles (~0.04–0.1 Myr periods, randomized) whenever the global climate is cold enough.
- Recorded per cell: max ice thickness, time since deglaciation, and glacial erosion depth. Tier 1.5 carves U-shaped valleys, cirques, arêtes, eskers and drumlins from these.

## Weathering and soils

- A regolith or saprolite front advances at a climate-dependent rate (≈2–40 m/Myr; fastest when hot and wet) and is stripped by erosion. Slow erosion plus long stability gives **deep laterite profiles**: bauxite, Ni laterite (garnierite over ultramafics), Fe caps, kaolin and supergene copper ([06](06-resources.md)).
- Glaciation and fast uplift strip profiles; burial or a later arid climate preserves them.
- The final regolith depth plus climate give the soil type, which feeds Tier 2 surface blocks (TFC soils; D-18).

## Wind (deserts)

Arid cells with a sand supply (deltas, beaches, eroded sandstone) become dune fields (Tier 2 decoration). Loess forms downwind of glacial outwash.

## References

- Braun & Willett 2013 (FastScape); Yuan et al. 2019 (deposition extension).
- Cordonnier et al. 2016, *Large-scale terrain generation from tectonic uplift and fluvial erosion* (procedural-terrain precedent).
