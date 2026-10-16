# Marine spreading review (2026-10-16)

Three independent reviews of the walk-ahead marine spreading (D-40) and the marine sediment model, consolidated and checked against the code. The exact items were carried out or measured the same day; see [Status](#status-after-the-exact-speedups). The science items are planned in [Marine science plan](Marine%20science%20plan.md).

## L0 — Summary

- **Implementation:** no correctness defect found. The exactness argument (recorded walks are true prefixes, `walkedTo` is a valid bound, resume rebuilds the exact walker state), the concurrency and the float overlay all hold. Open items: a worst-case memory risk, a missing window guard, and test gaps.
- **Science:** the river and erosion side is sound. The marine scheme is mass-conserving but fills to fixed levels on a planet with **no accommodation on continental crust**: no rifted margins, no thermal subsidence, no flexure, no sags. Every grain that leaves land therefore lands on oceanic crust and recycles. The fill rules give 900 km wide −120 m plateaus and deltas 1 m deep that never become land. This blocks most sedimentary resources planned for Phases 6–7.
- **Performance:** exact (byte-identical) changes could take a 31.25 km world from ~150 s to ~95–110 s. Allowing documented, deterministic output changes could reach ~70–80 s, close to the old 1.5 min with round fans kept. All of these are estimates from reading the code, not measurements.

## L1 — Findings

### Implementation (code review)

| # | Severity | Finding | Status | Fix |
|---|---|---|---|---|
| C1 | Medium | Per-batch records are unbounded. Each mouth with no deep water within 3 apron radii records ~23k cells (~280 KB); 8,192 such mouths (a filled epicontinental sea) would hold ~2.3 GB at once. Buffers never shrink. | Plausible; measured averages are ~44 MB per batch | Cell budget per batch (stop recording above ~4M cells; the calling thread carries on), or an adaptive batch size |
| C2 | Low | `SeaWalk` never checks its window. Walking past `radius − 1` would alias keys silently instead of throwing; only `WalkSlot.take` keeps the walk in range. | Confirmed | One check per `takeRing`; also correct the doc ("radius − 2" is one ring of slack) |
| C3 | Low | The whole-grid `seaWalk` is built unconditionally, so the 32,768-cell packed limit now applies on one thread too | Confirmed; not reachable in practice | Document it |
| C4 | Low | Workers write the per-mouth `ahead*` arrays at interleaved indices, causing false sharing | Confirmed; small cost | Per-band records |
| C5 | Low | No test asserts that the resume path runs. The mid-ring `reach` break, exhausted walks and whole-grid slots are also unasserted. | Confirmed | A resume counter asserted in a 1-vs-N test; synthetic two-band test; grid too narrow for the window; odd widths, h = 1, `resume` with offsets, `dist2` in the resume test |

Checked and correct: prefix property, `walkedTo` semantics against the old `nextRing()` side effects, the resume invariant (taken ∪ queued = all marks), date-line wrap for even and odd widths, `dist2` overflow, the spare queue slot for branchless pushes, stamp wrap, happens-before through ForkJoin, overlay float expressions, and edge cases (no mouths, partial batches, bands > width, `setParallel` twice or never).

### Science (marine and basin sedimentology)

| # | Size | Hole | Effect | Method |
|---|---|---|---|---|
| H1 | Large | No continental accommodation. (a) A split fills the gap with 7 km ocean crust at once: no rift stage, no transitional crust, no thermal subsidence. (b) Airy isostasy is local per cell, so there are no foreland basins or forebulges. (c) No intracratonic sags. | Sediment sits on oceanic crust and recycles. The standing sea-floor pile (1.1–1.6 × 10⁹ km³) is about equal to Earth's *whole* preserved sedimentary shell, ~90% of which sits on continental crust. Flooded continent is 6–12% vs Earth ~29%. No foreland coal basins, no rift lakes or evaporites, no passive-margin petroleum. | **H1b (first):** regional compensation as a Gaussian filter of the Airy load, σ ≈ flexural parameter (Te 20–80 km → 50–140 km), reusing the separable box passes; ~20–50 ms per pass. **H1a:** rift stage (β 1.3–1.5 thinning, lakes) for 10–30 Myr, then 100–200 km of tapering transitional crust before ocean crust; thermal sag through `keel` with τ ≈ 60 Myr (McKenzie 1978). **H1c:** rare cratonic sag events. |
| H2 | Cheap | The fill rules give the wrong hypsometry. The shelf is 300 km everywhere and flat at −120 m, and it fills abyssal cells too. Aprons are equal shares capped at −120 m out to 900 km, making plateaus. The delta top fills to −1 m and progrades ~70 km/Myr (Earth 2–6 km/Myr); it stays ocean, so deltas never become land. No bypass to fans. | Shelves 3–4× too wide (30× on active margins); no slope or rise; sediment 27–54 km thick vs Earth ≤ ~20 km; large areas 1 m deep that flood or drain with small sea-level changes | Shelf radius by margin type and supply (passive ~100 km, active 1 cell). Ramp profile −(10 + 110·d/R) m. Apron shares ∝ exp(−d/L) with L from supply (fan scaling, Sømme et al. 2009) under a sloping cap. 30–40% bypass to the apron. Delta top only over the platform and to sea + 2 m, so it becomes land; distal drape instead of deep −1 m fills. Report the 0–200 / 200–3,000 / >3,000 m ocean area split (Earth ≈ 7.5 / 15 / 77%). |
| H3 | Cheap | Shallow carbonate grows 40 m/Myr regardless of depth (5–150 m), even under river mud; Precambrian factors 0.1/0.3 | Every warm cell above 150 m (the whole −120 m shelf belt) fills to −5 m in 1–2 steps; Proterozoic platforms ~10× too thin | Rate r₀·e^(−z/z₀), z₀ ≈ 25–30 m (Bosscher & Schlager 1992); skip where clastic deposition > ~10 m/Myr; factors 0.4 / 0.8 / 1.0 |
| H4 | Cheap | `accretedSedimentFraction` = 0.5 and wedges build up to sea level | Accretion ~10× Earth's rate; tallest residual spikes are trench wedges | Fraction 0.25–0.3; cap wedges 0.5–1 km below sea level; print an accretion budget |
| H5 | Medium | No sill depth: silled basins are indistinguishable from open ocean | No basin evaporites, euxinic black shales, SEDEX or Kupferschiefer hosts | Bottleneck Priority-Flood for `sillDepthM`; evaporites from net evaporation in silled basins at 10–35° latitude; euxinic flag; salts in endorheic lakes |
| H6 | Cheap | All eroded rock is routed as clastic sediment; dissolved load (~15–20% on Earth) is not separated, while ooze and carbonate come "from outside" | Clastic pile too large | Remove ~17% at the mouth as dissolved load, feeding the carbonate/ooze budget |
| H7 | Cheap | No upwelling index | No phosphorite, diatomite or organic-rich shelves | Eastern-boundary coasts at 10–35° and the equatorial band, from the fixed wind belts |
| H8 | Cheap | Mouths run in ascending cell order every step | Shared sea always goes to the northern/western river; deltas skew south-east | Alternate the order or order by hash(seed, step, mouth); deterministic, and walk-ahead does not care |
| H9 | Cheap | `depositM` is one net number per cell | Phase 6/7 cannot tell environments apart | Record an `env` byte (fan, floodplain, lakes, inner/outer shelf, slope, basin floor, pelagic, ooze, carbonate, delta plain, wedge, glaciomarine), water depth and distance to shore at deposition |
| H10 | Cheap | No compaction | Matters only when writing rock columns | Athy porosity when writing columns |
| H12 | Diagnostic | Sediment on oceanic crust displaces water: O(1 km) of sea-level rise | Land fraction is the residual of two large numbers | Retune `--ocean-depth` after H1/H2/H6; print the displaced water |

Sound and worth keeping: floating sea level with conserved water (D-26), seas ≥ 100,000 km² (D-39), implicit stream power with sub-grid relief (D-30), the lowest-donor rule, isostasy constants, D-38 invariants, round fans with exact walk-ahead (D-40), D-31 for deep-sea sediment, basin closure (D-36), shelf-break depth 120 m, CCD 4.5 km, pelagic and ooze rates, subduction erosion 35 km³/km/Myr (USGS long-term ~30).

Items the reviewer cited from memory and wants checked: the GlobSed ocean total (~3.4 × 10⁸ km³), the subducted share (Clift & Vannucchi 2004), the dissolved flux (Gaillardet et al. 1999), Proterozoic carbonate rates (Grotzinger 1990).

### Performance

Exact (byte-identical), in suggested order:

| # | Change | Mechanism | Est. gain at 31.25 km | Effort |
|---|---|---|---|---|
| P1 | `erode`: `Math.pow(lengthM, n)` with n = 1.0 → `lengthM` | The Java spec guarantees pow(x, 1.0) == x; saves a libm call per land cell (224M calls) | −3 to −6 s | Trivial |
| P2 | `refreshStats`: nested j/i loops, `Plate[]` and primitive accumulators, same order; skip the call when nothing changed | Removes `List.get`, `%` and `/` per cell | −2 to −3 s | Trivial |
| P3 | `CrustalFlow.ceilings` in parallel; skip the first `updateSeaLevel` of a step (it repeats the last one of the previous step); share one elevation pass per sea-level update | Independent per-cell work; duplicated passes | −4 to −7 s | Low |
| P4 | Marine E1: per-block counts of cells below shelf level; when none lie within 3 apron radii, skip the 3× search (the delta-top and extension fill calls come out in the same order) | Removes ~22k-cell walks | −5 to −12 s | Medium |
| P5 | Marine E2: per-sea-body counts of cells below sea − 1; when the body is full, skip the walk across the whole body (the delta plain gets one deposit per cell, so order doesn't matter) | Removes serial walks across whole seas | −4 to −10 s | Medium |
| P6 | Marine E3: record rings until their room covers ~25% of the volume (max +6 rings); record to 3× when a mouth has fewer than ~8 apron cells | Moves continuations onto the workers | −4 to −6 s (+1–2 s workers) | Low |
| P7 | Marine E4: byte visited stamps (window 30 KB in L1, whole grid 0.8 MB in L2), sea mask merged in, windowed resume | Fewer cache misses per neighbour test | −5 to −8 s | Low |
| P8 | Marine E5: bitsets for "below shelf level" and "below sea − 1", kept current in `depositKm` | Replay stops reading `elevation` for cells it only checks | −3 to −5 s | Low–medium |
| P9 | Marine E6: 4-byte records (cell index + shelf flag + ring-start flag) | 75 → 25 GB of record traffic per run | −2 to −4 s | Medium |
| P10 | `BasinClosure`: run-length connected-component labelling with union-find instead of flooding every oceanic region each step | Big open-ocean regions dominate | −6 to −9 s | Medium |
| P11 | Large pages (`-XX:+UseLargePages`, needs SeLockMemoryPrivilege), ParallelGC with pre-touch; check inlining with `-XX:+PrintInlining` | TLB misses in random-access phases | −3 to −6 s (CLI only) | Low |

Not worth it exactly: fewer apron passes (changes rounding), exact parallel replay by footprint (shelf fans along a coast form one long dependency chain), row-span flood fill in the same order, caching walks across steps.

Allowed to change outputs (documented, deterministic):
- **I1, tile-coloured direct spreading:** checkerboard tiles 2R wide in 4 colours, all mouths of one colour in parallel, walks capped at R. Order becomes colour-major; no 2,700 km search; per-step progradation ≤ R. Marine ~8–12 s (−35 s).
- **I2, canonical in-ring order** (distance, then angle): a scanline flood plus a counting sort, ~3–4× cheaper per cell. Cape cells join their distance ring.
- **Routing:** an explicit tie-break enables a radix heap (13 → ~5 s); `Math.sqrt` for pow(q, 0.5); fixed-partition parallel reductions for sea-level bins and stats sums.

Suggested order: P1–P3 and the marine counters first (low risk), then P4–P7, then P8–P11. Exact total: ~150 → ~95–110 s. With I1 and radix routing: ~70–80 s.

### Status after the exact speedups

Profiling a 31.25 km run (JFR) and counting the marine work first changed the plan. Every change below gives byte-identical output on 1, 2 and 16 threads ([09 § World-model refinement](../design/09-roadmap.md#world-model-refinement) has the timings).

| # | Status | What was found or done |
|---|---|---|
| C1 | Done | One cell budget per batch, shared by the workers (16.7M cells, ~200 MB); batch size adapts to ~8M recorded cells; buffers over 2M cells are let go. A first per-worker budget was too tight (61,000 mouths fell back to the calling thread) |
| C2 | Done | `SeaWalk` throws past ring `radius − 1` (the true limit; the doc said `radius − 2`) |
| C3 | Done | Documented in `SeaWalk` |
| C4 | Not done | Small |
| C5 | Done | 1-vs-5-thread test with a resume counter, a tiny budget and a window wider than the grid; `SeaWalk` tests on odd widths, one and two rows, resume with offsets and `dist2` |
| P1 | Done | Saved ~2.5 s at 31.25 km |
| P2 | Done | The profile showed 5.5 s in `ArrayList.get` alone |
| P3 | Partly | Ceilings run on the workers. The sea-level skip (~1 s) is not done: it needs a proof that no cell hits the elevation cap |
| P4 | Dropped | Counted: only 1.9% of mouths walk the search with nothing to fill (18% of searched cells) |
| P5 | Dropped | Counted: 0.8% of mouths reach the whole-sea delta plain (2% of cells) |
| P6 | Dropped | Spare rings cut carried-on walks by 20–27% but not the cells walked (the long walks remain); no faster |
| P7 | Partly | Byte visited marks. Sea mask not merged; resume still uses the whole-grid walker |
| P8 | Replaced | Workers flag cells already at shelf (87% of searched cells) or delta-top level in the record. Saves only ~1.7 s: the replay is bound by its serial pass over the record, not by elevation reads |
| P9, P11 | Not done | |
| P10 | Done | Runs along rows joined by union-find, on the workers; only enclosed regions are flooded |
| — | Done | Found by profiling: water bodies (D-39) labelled the same way (~4 s); last subduction pair remembered (~3 s of `HashMap` lookups); eclogite, keel and rift-side loops on the workers |

What limits marine spreading now (~44 s at 31.25 km): the replay's serial pass over ~4.4 × 10⁹ recorded cells, and ~0.8 × 10⁹ cells of walks carried on by one thread. The next exact step would record a second stream holding only cells with room and replay that (estimated −10 s). It is best done after the [marine science changes](Marine%20science%20plan.md), which change the per-cell levels the flags use.

## L2 — Where things are

- Code: `geosim-core/src/main/java/com/platetectonics/geosim/surface/SeaWalk.java`, `SurfaceProcesses.java` (`spreadAllMarine`, `spreadAhead`, `spreadMarine`, `more`, `WalkSlot`, `fillApron`, `fillTo`, `depositKm`, `seaFloor`, `erode`), `tectonics/PlateMotion.java` (`newOcean`), `tectonics/PlateSimulation.java` (`refreshStats`), `tectonics/BasinClosure.java`, `tectonics/CrustalFlow.java`, `tectonics/TectonicsConfig.java` (`accretedSedimentFraction` 0.5), `surface/SurfaceConfig.java` (shelf 300 km / 120 m, apron 900 km, carbonate 40 m/Myr).
- Design: [04 § Sea floor](../design/04-surface-processes.md#sea-floor), [decisions D-26–D-40](../design/decisions.md), [09 § World-model refinement](../design/09-roadmap.md#world-model-refinement).
- Sources cited by the science review: Ronov sediment-mass compilation (earthref ERDA 98; NAP *Sedimentary Cover*), Peters & Husson 2017, McKenzie 1978, Watts 2001, DeCelles & Giles 1996, Sømme et al. 2009, Bosscher & Schlager 1992, von Huene & Scholl 1991, Stern 2011, USGS subduction-erosion rate, Bengal Fan and continental-shelf statistics (Wikipedia), Messinian salinity crisis, GlobSed (Straume et al. 2019).
