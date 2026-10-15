# 06 · Resources: ores, gems, industrial minerals, coal, oil and gas

> **Level 1** · Read when: you work on anything that places an ore, mineral, gem, coal seam or hydrocarbon.
> Decisions: D-2, D-9 … D-14, D-16 · **Go deeper (L2):** [research report](../reports/Ore%20deposits%20and%20TFC%20compatibility.md), especially § "Deposit catalog: about 70 types" (per-type rules and TFC blocks), § "One event log, one scorer and eight generator families", and § "Lacking". Raw cited notes are in [research_notes](../research_notes/Ore%20deposits%20and%20TFC%20compatibility/).

**Summary.** About 70 real deposit types are produced by **8 generator families** that share one pipeline:

1. The plate and basin history writes an **event log**.
2. A **scorer** checks source, trigger, pathway and trap against the rock column.
3. **Global time gates** filter candidates (oxygen, mantle temperature, land plants…).
4. Deposits are **instantiated** in clusters with log-normal sizes.
5. Each deposit gets a **depth-zoned template**.
6. A **preservation test** compares formation depth with later exhumation; destroyed deposits feed placers.
7. Survivors go into a spatial index and are **stamped into chunks** at gameplay size, as TFC blocks.

## The 8 generator families

| Family | Main deposit types | Fires on |
|---|---|---|
| G1 Arc magmatic–hydrothermal | Porphyry Cu-Mo-Au, skarn, carbonate-replacement Zn-Pb-Ag, epithermal Au-Ag, hot-spring Hg (**cinnabar**), Carlin-like gold | Subduction segments (fast orthogonal convergence, moderate obliquity, suitable slab age), contraction; post-collisional variant |
| G2 Seafloor exhalative / syngenetic | VMS (Cyprus, Besshi, Kuroko), SEDEX Pb-Zn, Algoma and Superior BIF, Mn | Ridges, back-arcs, rifted arcs; rift and sag basins; anoxic shelves |
| G3 Basin brine | Mississippi Valley-type Zn-Pb, sediment-hosted Cu, unconformity U, red-bed Cu | Foreland loading or orogenic fluid expulsion; rifts with red beds plus evaporites; unconformities |
| G4 Metamorphic-fluid orogenic | Orogenic gold (± Sb, W), epizonal Sb-Hg | Late collision or accretion, crustal-scale shear zones |
| G5 Mantle magmatic | Komatiite Ni, Ni-Cu-PGE, layered-intrusion PGE/Cr/V-Ti, podiform chromite, **kimberlite diamonds**, carbonatite, anorthosite Ti | Plume heads and LIPs, rifts, ophiolites, thick old cratons |
| G6 Crustal-melt granitic | **Sn-W (cassiterite)** greisen and veins, LCT pegmatites, Climax Mo | Crustal thickening, then post-collisional extension; anorogenic A-type granites |
| G7 Surface, residual and placer | Ni laterite (**garnierite**), bauxite, supergene Cu (**malachite**), gossans (**limonite**), kaolin, alluvial and beach placers (Au, Sn, diamond, gems), Witwatersrand paleoplacers | Exposure of a parent body plus climate; sediment routing |
| G8 Bulk sedimentary and chemical | Evaporites (halite, gypsum, sylvite, borax, saltpeter), phosphorite, ironstone, **coal**, oil shale, **petroleum** | Basin + climate + biosphere state |

The full per-type catalog is in the report (L2). That includes metamorphic and gem deposits such as marble ruby, schist emerald, contact lapis, graphite, jade, amethyst geodes and opal.

## Pipeline

| Stage | When | What |
|---|---|---|
| 1 Event log | Every step (from [03](03-tectonics.md)) | Subduction segments, collisions, rifts, ridges, plumes and LIPs, basin subsidence, uplift, erosion |
| 2 Column context | Every step (from [05](05-lithology.md)) | Lithology, redox flags (red bed, black shale, evaporite, carbonate), Tmax, TTI, unconformities |
| 3 Scorer | Cells and steps where a family's trigger fired | prospectivity = fuzzy AND(source, trigger, pathway, trap) × gates from `WorldHistory.globalState(t)` |
| 4 Instantiate | Same | Expected count from density models (e.g. porphyry median 15 per 100,000 km² *exposed*, so about 2.6× more must form). Clustered: province → belt (100s of km) → cluster (5–30 km) → deposit. Size and grade log-normal (top 10% of deposits hold 70–86% of the metal) |
| 5 Template | Same | Vertical zoning anchored to the paleosurface (e.g. porphyry: sinter 0–0.1 km, lithocap 0–1, ore 1.5–4, pluton 5–15 km) |
| 6 Preservation | Every step afterwards | Exhumation E since formation: **buried** if E < z_form; **exposed** if z_form ≤ E < z_form + h; **destroyed** if E ≥ z_form + h. Destroyed bodies release resistant minerals to placers. Supergene, laterite and gossan overprints apply while the body is near the surface |
| 7 Stamp | Chunk generation (lazy) | Query the deposit index; resolve host rock from the column; map (mineral, rock, grade) → block via the palette; place surface indicators |

**Key formation depths** (preservation inputs): epithermal ≈ 0.5 km · hot-spring Hg < 0.3 km · porphyry ≈ 2 km (1–6) · skarn and Carlin < 3–4 km · plutons and pegmatites 5–15 km · orogenic gold 2–20 km (mean ≈ 10) · kimberlite pipes 0 to > 2 km · laterites and supergene 0–0.5 km. That is why **young worlds** show epithermal gold and cinnabar, and **old worlds** show orogenic gold, pegmatites and deep plutons.

## Gameplay layer

- **Near real size (D-2):** at 30 m per block bodies render close to their real dimensions (porphyry ≈33–66 blocks across); reefs and seams get a 1-block minimum; an optional size multiplier exists for gameplay.
- **Richness (D-16):** a per-commodity multiplier, calibrated to TFC vein frequencies, scales counts while keeping ratios and clustering.
- **Progression floor (D-11):** guaranteed copper, tin, zinc + bismuth, gold + silver, nickel, iron, flux carbonate, coal and redstone within a radius of spawn, placed geologically if possible.
- **Redstone (D-10):** cinnabar in hot-spring, epithermal and Almadén-type quartzite settings, plus accessory cinnabar in Carlin and epizonal gold; one guaranteed Ivigtut-type cryolite; sparse A-type cryolite.
- **Indicators:** TFC small nuggets (`tfc:ore/small_<ore>`, graded ores only) within about 14 blocks when a body's top is within 35 blocks of the surface. Plus our own cues: gossans (limonite-stained caps), alteration colours, salt crusts and tar seeps.

## TFC mapping (summary)

- Block ids: `tfc:ore/{poor|normal|rich}_<ore>/<rock>` for graded ores, `tfc:ore/<ore>/<rock>` for others, `tfc:deposit/<ore>/<rock>` for placer gravels (Cu, Sn, Ag, Au), `tfc:halite`, `tfc:lignite`, `tfc:bituminous_coal`.
- Grade: deposit grade percentile → POOR / NORMAL / RICH weights (TFC uses 70/25/5, 35/40/25, 15/25/60).
- **Stand-ins (D-12):** tetrahedrite for Cu sulfides · garnierite for Ni sulfide · magnetite for ilmenite · native silver (+ sphalerite) for argentiferous galena.
- **Not in v1 (no TFC block):** Pb, Mo, W, Sb, As, Cr (unless Firmalife is installed), PGE, U, Mn, Al, Ti sands, Li, REE, P, fluorite, barite. Their deposits are still *simulated* (for maps and future addon palettes) but emit nothing.
- **Rollout (D-9):** step 1 keeps TFC's own veins (they follow our rock layers automatically); step 2 overrides `#tfc:in_biome/veins*` tags and switches to our `DepositFeature`.

## Coal

- **Gates:** land plants exist ([07](07-life-atmosphere-sun.md)); ever-wet climate (statistic from [04](04-surface-processes.md)); subsidence roughly matches peat growth (0.05–4.5 mm/yr); low clastic input. Coal is absent for about 10 Myr after a major terrestrial extinction (coal gap).
- **Seams:** each step emits a stochastic seam package: (dt ÷ cycle length 0.1–0.4 Myr) × fraction of cycles with peat; seams 0.3–3 m, rare 10–30 m in fast-subsiding basins. Compaction peat:coal ≈ 2–4:1 (lignite) to 10–20:1 (bituminous).
- **Rank from Tmax:** < 35 °C peat (`tfc:peat`) · 35–80 °C lignite (`tfc:lignite`) · 80–180 °C bituminous (`tfc:bituminous_coal`) · 180–300 °C anthracite (our block, D-14) · metamorphism → graphite (`tfc:ore/graphite/<rock>`).

## Petroleum

```
per step, per organic layer:  T = surfaceT + geotherm·depth;  Tmax = max(Tmax, T)
                              TTI += dt · 2^((T − 105)/10)          (Lopatin; oil 15 → peak 75 → end 160)
source layer (TOC ≥ 1–2 %):   transformation ratio = sigmoid(TTI); generated = TOC·HI·volume·ΔTR
                              expelled 50–80 % → carrier bed; TTI > 160 cracks oil to gas
every 5–10 Myr, per reservoir horizon:
                              priority-flood the inverted top-reservoir surface → traps, spill points
                              route charge updip (fill–spill); gas takes the crest (differential entrapment)
                              seal capacity: shale 50–300 m, evaporite 300–1,000 m of column
                              a trap accepts only charge arriving after it formed
after filling:                < 80 °C and never pasteurized → biodegraded heavy oil; breached → tar seeps, oil sands
                              immature rich source rock → oil shale; slow leakage over 100–300 Myr
```

Render (D-13): tar seeps and oil shale first. Later: an oil fluid and a gas block filling reservoir layers below flat gas–oil and oil–water contacts. A Ghawar-sized 400 m column is about 12 blocks thick.

## Validation targets (for tuning, not hard rules)

Copper: porphyry about 60% (systems about 75%), sediment-hosted about 20%. Gold: orogenic about 30%, Witwatersrand about 30%, porphyry 16%, epithermal 12%. About 62% of Phanerozoic porphyries are eroded. Exposed-age modes: epithermal 2–3 Myr, porphyry 11 Myr, orogenic gold 199 Myr old. Coal by period: Permian 29%, Paleogene–Neogene ("Tertiary") 21%, Carboniferous 20%. About 70% of petroleum was generated in the last ~89 Myr. Giant oil fields: passive margins 36%, rifts 30%, collisions 19%. Sources are in the report.
