# Land share stability plan (2026-10-16; built, see [Outcome](#outcome))

Plan for steadying the land share the way Earth does it. Owner: "Land % is having too much variations", then: "Write a plan to steady land share the Earth way: measure how much each sea-level driver (ice, ocean-floor age, seafloor sediment) contributes to the swings, then propose how to damp them and raise continental freeboard." No model rule changes until the owner approves; only a measurement mode was added (`--sea-drivers`, [§ Method](#method)).

## L0 — Summary

- **The problem in numbers** (Earth-like preset, 4.5 Gyr; 7 seeds at 78 km, seed 42 at 31.25 km):
  - **Between seeds:** land ends anywhere from 19% to 40% (12 seeds). Continental crust is steady (31–39% of the surface); what differs is how much of it is under water (2–48%).
  - **Over time:** in the last 500 Myr, land has a standard deviation (SD) of 8.9 points and moves up to 25 points within 50 Myr (31.25 km: 11.3 and 30). Sea level has an SD of 104 m and moves up to 262 m within 50 Myr (31.25 km: 160 and 373 m).
  - **Earth:** long-term sea level peaked ~100–170 m above today's in the Cretaceous, over tens of Myr. Land fell from ~29% to perhaps 15–20% (an informal estimate): roughly 10 points over ~90 Myr.
- **Why** (measured, [§ Results](#results)):
  1. **Continents stand low and flat: the main cause.** Land changes by 11 points per 100 m of sea level (3–18 by seed). Land stands 400 m above the sea on average; Earth's stands ~800 m.
     - Many seeds end with a broad platform within ~50 m of the sea. When it is just under water, shallow sea (0–200 m) covers 18–22% of the ocean (Earth ~7.5%) and land is ~20%. When it is just above, land is ~39% and there is almost no shelf.
     - Continental crust holds only 67–89% of the volume per area that the growth curve asks for (Earth's), so continents are 30–32 km thick on average.
  2. **Sea level swings too far, too fast.**
     - **Ice** gives about half of the sea-level variance in the last 500 Myr, and half of the land variance. A third of the cylinder lies poleward of 60° (D-7; 13% of a sphere), so continents drift into and out of ice-prone latitudes far more than on Earth.
     - **Ocean-floor age** gives ~0.4 of the sea-level variance. Depth keeps growing as √age down to 6 km, while Earth's old floor flattens out near 5.6 km.
  3. **Seafloor sediment** (1.5–2.5 km thick; Earth ~1 km) is mostly a fixed offset, absorbed by the water setting. It also partly cancels the age swings, because old floor carries more sediment.
- **Proposal**, in order, each measured before the next:
  - **R1 — Plate-cooling ocean depth (GDH1, Stein & Stein 1992),** Earth's own depth–age curve. First-order effect: sea-level SD −13%, land SD −9%.
  - **R2 — Ice counted at a sphere's polar area.** For the water budget only, each cell's ice counts (π/2)·cos(latitude); the ice on the map is unchanged. First-order: sea-level SD −23%, land SD −28%.
    - R1 and R2 together: sea-level SD −34%, land SD −31%, and the biggest 50-Myr land swing falls from 24 to 16 points.
  - **R3 — Freeboard: make continents stand higher.**
    - (a) Bring continental crust up to its per-area volume (a stronger growth feedback).
    - (b) Measure how fast low, flat land erodes and how much land the sea builds at sea level, then slow whichever planes continents down faster than Earth's.
    - This is the larger lever: halving the sensitivity would halve the land swings left after R1 and R2. It needs experiments, so its size isn't known yet.
  - **Then retune the ocean water** for ~29% land.
- **Targets** (owner to confirm; snowball episodes excluded):
  - Last 500 Myr: land SD ≤ 4 points, at most 10 points within 50 Myr.
  - 12 seeds end within 22–36% land.
  - Mean land elevation ≥ 600 m.
  - Shallow sea (0–200 m) 5–10% of the ocean.
- **Cost:** R1 and R2 are negligible. R3 depends on what it turns out to need.

## L1 — Measurements and rules

### Method

`geosim-cli --sea-drivers` writes `sea_drivers.csv` with one row per step. It is measurement only: with it on, maps, history and events are byte-identical to a run without it. Each step it refills the ocean (`SeaLevel.fillToVolume`) on the same surface:

| Refill | Held fixed | Driver's part |
|---|---|---|
| No ice | The ice's water returned to the ocean | Sea level minus this level |
| One age | Every ocean-floor cell at 80 Myr (its depth on today's law) | Sea level minus this level |
| No sediment | Ocean-floor sediment removed (0.40 m of floor per m of sediment) | Sea level minus this level |
| GDH1 | Ocean floor at GDH1 depths | Candidate R1 |
| Sphere ice | Ice water weighted by (π/2)·cos(latitude) per cell | Candidate R2 |
| Both | R1 and R2 | |

It also records the land share at fixed levels on the datum (−900 to +300 m, every 100 m), and:
- the hypsometric sensitivity: land between 50 m below and 50 m above sea level;
- median continental height above the sea, mean land height;
- mean ocean-floor age and sediment, ice water, continental area.

**Analysis** (`D:\tmp_scripts\sea_analyse.py`, `sea_alt.py`, `sea_final.py`):
- Statistics are weighted by step length, since steps shorten from 10 to 2 Myr near the end.
- A driver's share of the variance is cov(part, sea level) / var(sea level); the shares add up to 1 with "rest" (continental area and shape).
- A driver's share of the land variance uses each step's sensitivity: land moves by −S × part.
- "Land if the sea stood still" reads the land share at the window's mean sea level.

**Candidate rules are first order.** The alternative series is the same surface under a different rule, shifted to the real mean, as if the water were retuned. A real run would also change the history (erosion, deposition, climate), so the rules must be checked in real runs.

### Results

**Sea-level and land variance by driver, last 500 Myr** (7 seeds at 78 km; mean over seeds):

| | Sea level | Ice | Ocean-floor age | Sediment | Rest |
|---|---|---|---|---|---|
| SD | 104 m | 56 m | 99 m | 103 m | 44 m |
| Share of sea-level variance | 1 | 0.50 | 0.44 | −0.08 | 0.14 |
| Biggest change within 50 Myr | 262 m | 180 m | 275 m | 226 m | 94 m |
| Share of land variance | | 0.53 | 0.08 | 0.07 | 0.17 |

- Age and sediment each move sea level by ~100 m but largely cancel, because old floor is deep and sediment-laden.
- Over the last 2,000 Myr the shares are ice 0.28, age 0.34, sediment −0.06 and rest 0.44. The rest is continental growth and area changes; the land variance there splits ice 0.37 and continents' own change 0.42.
- One seed (9) has a snowball episode. Its ice then holds enough water to drop sea level by 1.6 km within 50 Myr.
- Seed 42 at 31.25 km agrees: last 500 Myr ice 0.39, age 0.54 and rest 0.17 of the sea-level variance, and ice 0.46, age 0.17, rest 0.21 of the land variance.

**End states** (4.5 Gyr):

| Seed (78 km) | Land | Land per 100 m of sea level | Median continent above sea | Mean land height | Crust, % of per-area target | Ice water | Shallow sea, % of ocean |
|---|---|---|---|---|---|---|---|
| 42 | 22.0% | 6.9 pts | 135 m | 595 m | 67 | 39 m | 13.8 |
| 1 | 39.0% | 4.1 | 190 m | 403 m | 86 | 95 m | 2.8 |
| 2 | 31.1% | 10.6 | 108 m | 391 m | 84 | 49 m | 9.5 |
| 4 | 23.7% | 15.3 | 59 m | 508 m | 87 | 32 m | 21.5 |
| 5 | 39.5% | 3.0 | 247 m | 478 m | 82 | 145 m | 1.1 |
| 6 | 18.8% | 15.3 | 6 m | 560 m | 78 | 50 m | 21.7 |
| 9 | 26.6% | 17.7 | 8 m | 307 m | 77 | 35 m | 18.0 |

Two kinds of end state, decided by where the sea stands against the continental platform:
- **Platform exposed** (seeds 1 and 5): land ~39% and insensitive to sea level.
- **Platform just awash** (4, 6, 9): huge shallow seas, ~20–27% land and very sensitive.

Over the last 500 Myr the mean sensitivity is 11 points per 100 m (9–15).

**Candidate rules, first order** (mean of 7 seeds at 78 km):

| | Sea-level SD | Sea level, most in 50 Myr | Land SD | Land, most in 50 Myr |
|---|---|---|---|---|
| Today, last 500 Myr | 104 m | 262 m | 7.8 pts | 23.8 pts |
| R1 GDH1 depths | 91 | 230 | 7.1 | 21.7 |
| R2 sphere ice | 80 | 185 | 5.6 | 17.6 |
| R1 + R2 | 69 | 162 | 5.4 | 16.3 |
| Today, last 2,000 Myr | 182 | 539 | 9.1 | 27.1 |
| R1 + R2 | 165 | 469 | 7.1 | 19.9 |

(Land here is read off the recorded levels, every 100 m, so "today" reads slightly below the exact 8.9 points.) R1 and R2 also move mean sea level up by 60–100 m and 27–90 m, so the water must be retuned after them.

### Earth reference

| Measure | Earth | Model | Source |
|---|---|---|---|
| Long-term sea-level peak | ~100 ± 50 m (Cretaceous); ~170 m (85–270 m) from ocean-basin volume; ~200 m of change since the Jurassic | Up to 262 m within 50 Myr | Miller et al. 2005; Müller et al. 2008; Wright et al. (Earth-Science Reviews) |
| Land at that peak | ~15–20% (informal estimate); a seaway over a third of North America | — | PaleoNet discussion; Cambridge text on crinoids |
| Flooding over long times | The average share of continent flooded stays about the same: erosion wears land down to sea level but hardly touches shelves | Varies 2–48% by seed | NRC, *Sea-Level Change* (1990), ch. 8 |
| Mean land elevation | ~800 m (797 m in 2012, 840 m in 1921) | ~400 m | Hypsometry review citing Eakins & Sharman 2012, Kossinna |
| Land within 10 m of the sea | ~2% of land | — | McGranahan et al. 2007 |
| Hypsometry near sea level | Peak of lowland area at ~100 m (and a maximum ~5 m above the sea in modern elevation models) | Platform within ~0–250 m | Britannica; Pedersen et al., EGU24 |
| Shallow sea, 0–200 m | ~7.5% of the ocean | 1–22% | (CLI reference) |
| Ocean sediment | ~1 km on average | 1.5–2.5 km over ocean crust | Oceanography textbook (GlobSed for the grid) |
| Depth of old ocean floor | GDH1: 2,600 + 365√t m to 20 Myr, then 5,651 − 2,473·e^(−0.0278t) m (5.5 km at 100 Myr) | 2,500 + 350√t m, to 6 km at 100 Myr | Stein & Stein 1992 (as given in the GMT docs) |
| Polar area (> 60°) | 13% of a sphere | A third of the map (D-7) | [01 § World model](../design/01-world-model.md) |
| Ocean water from or to the mantle | Over Gyr only; size debated (Korenaga et al. 2017 need a net flow into the mantle to keep freeboard constant; sea-level records say it is small) | Fixed | Korenaga et al. 2017; hydrogen-isotope study |

Not found: a published table of Earth's land area by elevation, to set the sensitivity target. Computing it from ETOPO 2022 needs a download (the owner's OK) — see [Open questions](#open-questions-for-the-owner).

### R1 — Plate-cooling ocean depth

**Rule:** `Isostasy.oceanDepthM(age)` follows GDH1 instead of 2,500 + 350√age capped at 6 km.

**Why:** It is Earth's measured depth–age curve. The model's mean ocean-floor age is 90–150 Myr (Earth ~64 Myr), so most of its floor sits where the two laws differ:
- Between 20 and 100 Myr, today's law deepens 2–4× faster than GDH1: at 64 Myr, 21.9 m per Myr of age against 11.6 m; at 90 Myr, 18.4 against 5.6 m.
- At 100 Myr it stops dead at 6 km.

GDH1 flattens smoothly, so the depth of old ocean swings less with its age.

**Effect:**
- Ocean basins hold less water, so sea level rises 60–100 m and the water needs retuning.
- Ocean-floor maps and the "ocean by depth" report shift slightly: the deepest floor goes from 6.0 to ~5.6 km.
- The mean ocean depth no longer tracks age so closely, which may change the climate's ocean area very slightly.

**Risk:** small. The single function is used wherever ocean depth is computed, so ocean floor across the whole planet becomes a few hundred metres shallower.

### R2 — Ice counted at a sphere's polar area

**Rule:** The ice's water equivalent (`ClimateModel`, which feeds `PlateSimulation.setIceWaterEquivalentM`) weights each cell by (π/2)·cos(latitude). The weight averages 1 over the map. Ice extent and thickness on the map stay the same.

**Why:** The world uses linear latitude for TFC (D-7), so polar bands are 2.5× too large. The climate already treats latitude as a sphere's. The water budget is the one place where the oversized polar area leaks into a global number. On Earth, the whole Antarctic ice sheet holds ~58 m of sea level (from memory, to check).

**Effect:**
- Ice's sea-level swings fall to about half: its SD falls to 0.48 of today's (0.39–0.54 by seed). Sea level stands 27–90 m higher on average, so the water needs retuning.
- Ice holds less water: 14–50 m of sea level on average over the last 500 Myr, instead of 33–117 m. Earth today holds ~65 m (from memory), so the model's ice may then be too small in water terms.
- Snowball episodes lower the sea less.

**Risk:** It is a budget correction, not a process: the ice drawn on the map holds more than it returns. The alternative is to leave ice alone and rely on R3. Owner's call ([Open questions](#open-questions-for-the-owner)).

### R3 — Freeboard

**Three experiments**, each measured with `--sea-drivers` over 7 seeds:

1. **Crust volume.** Continents hold 67–89% of the volume per area that the growth curve targets (14.1 km³ per km², Earth's). The arc multiplier steers with exponent 1 (`crustGrowthFeedback`) and is clamped to 0.25–4. Try exponent 2, and check what the budget loses (subducted arc crust ~53, subduction erosion ~20, collision ~5 billion km³ over 4.5 Gyr).
   - Expected: thicker or larger continents. At today's area the missing 11–33% of the volume would add ~4–15 km of crust, or ~0.6–2 km of height before erosion; part will go into area instead. Measure, then retune the water.
2. **Erosion of low land.** Measure the erosion rate by relief or elevation band.
   - Earth's low-relief cratons wear down at a few m per Myr (to check), against ~50 m per Myr averaged over all land.
   - If the model planes lowlands much faster, slow erosion on low slopes, so platforms keep a few hundred metres of freeboard.
3. **Land built from the sea.** The sea builds delta tops to +2 m and fills enclosed seas toward −1 m; in seed 42's last step it built 6.3 million km² of land. Measure how much of the ±50 m band is made this way. If it dominates, limit marine fill on flooded continent to below the wave base (−10 to −20 m) rather than to the sea's surface.

**Goal:** the platform no longer sits at sea level. The sensitivity falls toward Earth's (the target is to be set from Earth's hypsometry), mean land height reaches ≥ 600 m, and shallow seas cover 5–10% of the ocean.

**Not proposed:** dynamic topography and craton buoyancy (missing Earth processes, but each is a large model of its own), and water regulation (Earth's water exchange with the mantle acts over Gyr).

### Validation

- `--sea-drivers` on 12 seeds at 78 km and 3 at 31.25 km, before and after each rule. Report:
  - land SD and biggest 50-Myr swing over the last 500 and 2,000 Myr;
  - the end spread over seeds;
  - sensitivity, mean land height, and shallow-sea share;
  - each driver's share.
- The existing checks: ocean-floor age, coast orientation, biomes, crust budget, 1 vs 16 threads (identical), unit tests.
- Water retuned last, over 12 seeds, for ~29% mean land.

### Open questions for the owner

1. **R2:** is a sphere-weighted water budget acceptable on the cylinder world, or should ice keep its full map volume?
2. **Targets:** are the L0 targets right (land SD ≤ 4 points over the last 500 Myr, 12 seeds within 22–36%)?
3. **Earth hypsometry:** may I download ETOPO 2022 (60 arc-second grid, a few hundred MB; I'll confirm the exact file and size first) to measure Earth's land per 100 m of sea level, the sensitivity target?
4. **R3:** thicker continents change the look of the world everywhere (higher land, fewer inland seas). Proceed with the experiments?

## L2 — Where things change

| Rule | Files |
|---|---|
| Measurement (done) | `tectonics/SeaLevelDrivers.java` (new), `cli/Main.java` (`--sea-drivers`, `sea_drivers.csv`) |
| R1 | `tectonics/Isostasy.java` (`oceanDepthM`); docs 03 § ocean floor, 04 § Sea level |
| R2 | `climate/ClimateModel.java` (ice water equivalent); docs 04 § Ice, decisions (new D-row) |
| R3 | `tectonics/TectonicsConfig.java` (`crustGrowthFeedback`), `PlateSimulation.arcMultiplier`; `surface/SurfaceProcesses` (erosion of low land, marine fill), after measuring |
| Water | `terrain/SeaLevel.DEFAULT_OCEAN_DEPTH_M`, docs 04 and 08 |

## References

- Stein, C. A. & Stein, S. 1992, *Nature* 359, 123–129 (GDH1); coefficients as given in the [GMT depth–age example](https://docs.generic-mapping-tools.org/6.5/gallery/ex49.html) and [Wikipedia: Seafloor depth versus age](https://en.wikipedia.org/wiki/Seafloor_depth_versus_age).
- [Miller et al. 2005, *Science*: Phanerozoic sea level](https://www.ldeo.columbia.edu/~ncb/Selected_Articles_all_files/19_Science%20310.1293.pdf)
- [Müller et al. 2008, *Science*: long-term sea level and ocean-basin dynamics](https://www.researchgate.net/profile/Carmen-Gaina/publication/5529800_Long-Term_Sea-Level_Fluctuations_Driven_by_Ocean_Basin_Dynamics/links/0912f50b7bcdb4e530000000/Long-Term-Sea-Level-Fluctuations-Driven-by-Ocean-Basin-Dynamics.pdf)
- [Wright et al., *Earth-Science Reviews*: sea level and ocean-basin volume after supercontinent break-up](https://www.earthbyte.org/earth-science-reviews-sea-level-fluctuations-driven-by-changes-in-global-ocean-basin-volume-following-supercontinent-break-up/)
- [NRC 1990, *Sea-Level Change*, ch. 8: long-term eustasy and epeirogeny](https://nap.nationalacademies.org/read/1345/chapter/12)
- [Korenaga, Planavsky & Evans 2017: global water cycle](https://people.earth.yale.edu/sites/default/files/files/Evans/83-Korenaga%2Bwater.pdf); [seawater subduction and escape from hydrogen isotopes](https://arxiv.org/pdf/1806.03792)
- [McGranahan, Balk & Anderson 2007: low elevation coastal zones](https://unfccc.int/files/adaptation/application/pdf/nwpexpert_mcgranahan_balk_anderson_2007.pdf)
- [Britannica: hypsometric curve](https://www.britannica.com/print/article/280163); [Pedersen et al., EGU24: Earth's hypsometry and global sea level](https://meetingorganizer.copernicus.org/EGU24/EGU24-15498.html); [hypsometry review (IJRASET)](https://www.ijraset.com/fileserve.php?FID=32968)
- [Straume et al. 2019, GlobSed](https://epic2-clone.awi.de/id/eprint/49326); [oceanography textbook: sediment thickness](https://pressbooks.ccconline.org/introduction-to-oceanography/chapter/12-1-classifying-sediments/)
- [Heine et al. 2015: global paleoshoreline models](https://www.earthbyte.org/Resources/Pdf/Heine_etal_2015_Global_paleoshoreline_models.pdf); [Kocsis & Scotese 2021: paleocoastlines and continental flooding](https://cris.fau.de/publications/247565191)
- [PaleoNet discussion (informal Late Cretaceous ocean cover)](https://paleonet.org/archive/2003/msg03189.html); [Cambridge text: Western Interior Seaway](https://www.cambridge.org/core/services/aop-cambridge-core/content/view/B00C8BCA3307FC1C40870843BA0E5ACB)
- NOAA NCEI 2022, ETOPO 2022 15 Arc-Second Global Relief Model (60 arc-second version used, via the [PIFSC ERDDAP server](https://oceanwatch.pifsc.noaa.gov/erddap/griddap/ETOPO_2022_v1_60s.html)); Eakins, B. W. & Sharman, G. F. 2012, hypsographic curve of Earth's surface from ETOPO1 (NOAA NGDC).
- Harel, M.-A., Mudd, S. M. & Attal, M. 2016, *Geomorphology* 268, 184–196: global analysis of the stream power law parameters from 10Be denudation rates (n > 1 in most basins).
- Portenga, E. W. & Bierman, P. R. 2011, *GSA Today* 21(8), 4–10: understanding Earth's eroding surface with 10Be.

## Outcome

Owner: "Approve the land share plan as written: build R1 and R2, run the R3 experiments, use the proposed targets, and you may download ETOPO 2022 after telling me the exact file and size." Built as D-43 (R1, R2) and D-44 (slope-squared erosion). Earth-like preset, 4.5 Gyr, measured with `--sea-drivers`.

**Summary:**
- Land is steady: over the last 500 Myr its SD fell from 8.9 to 2.3 points, and it moves at most 8 points within 50 Myr (was 25). It meets both targets at 78 and 31.25 km.
- The fix that mattered was slope-squared river erosion (R3). R1 and R2 helped little on their own, because low flat continents sat at sea level.
- Not met: ~29% land, shallow sea 5–10% of the ocean, and mean land ≥ 600 m. Continents now stand above the sea, so land is about the area of continental crust (~34% at 78 km, ~38% at 31.25 km). Water barely changes that. Earth floods ~30% of its continental crust on thinned margins, which the model does not make.
- Side effect: the Earth-like present is ~2 °C warmer.

### Earth's hypsometry (ETOPO 2022)

Source: NOAA ETOPO 2022 v1, 60 arc-second surface elevation (`ETOPO_2022_v1_60s`), read through the ERDDAP server at oceanwatch.pifsc.noaa.gov as a 10 arc-minute subset (every 10th cell) in six latitude bands. Area-weighted by cos(latitude). Script: `D:\tmp_scripts\etopo_hyps.py`.

| Measure | Whole Earth | Without Antarctica and Greenland |
|---|---|---|
| Land | 29.1% | 28.0% |
| Mean land height | 793 m (Eakins & Sharman 2012: 797 m) | 656 m |
| Median land height | 418 m | 375 m |
| Land per 100 m of sea level (surface within ±50 m) | 4.6 points | 4.8 points |
| Shallow sea, 0–200 m | 7.3% of the ocean | 7.5% |
| Land within 10 m of the sea | 2.3% of land (McGranahan et al. 2007: ~2%) | 2.5% |
| Land 0–50 m, 50–200 m above the sea | — | 8.6%, 21.8% of land |

### R3 experiments

All with R1 and R2, 7 seeds at 78 km, last 500 Myr, 2,640 m of water:

| Experiment | Land SD | Most in 50 Myr | Points per 100 m | Mean land | Land 0–50 m | Shallow sea | End land |
|---|---|---|---|---|---|---|---|
| R1 + R2 alone | 7.2 | 21.3 | 13.3 | 411 m | 23% | 8.6% | 19.8–36.4% (30.5) |
| 1. Crust: `crustGrowthFeedback` 2 | 7.4 | 23.9 | 12.4 | 402 m | 20% | 4.2% | 27.5–42.8% (35.7) |
| 3. No delta top: `deltaTopM` 0 | 7.6 | 22.7 | 13.1 | 417 m | 22% | 12.2% | 19.7–36.6% (27.1) |
| More river transport: `transportK` 9 × 10⁵ | 7.7 | 24.2 | 15.4 | 338 m | 27% | 14.1% | 14.7–34.9% (26.4) |
| **2. Slope-squared erosion: n = 2, K = 250,000** | **2.3** | **7.8** | **4.0** | **561 m** | **6%** | **3.4%** | **31.2–35.7% (34.3)** |

- **Experiment 2, measured first:** with n = 1, lowlands were not eroding too fast; they eroded at 7–13 m/Myr, close to Earth's outcrops (median 5.4 m/Myr; Portenga & Bierman 2011).
  - The flats were an attractor instead. Land near sea level eroded and took sediment at about the same rate, so a platform held at sea level.
  - With n = 2, a 2% slope erodes as before, but gentle land erodes at 1–3 m/Myr and steep land faster. Continents keep their height.
  - Channel profiles show n > 1 in most landscapes (Harel, Mudd & Attal 2016).
- **Experiment 3:** land built from the sea is small (0.2–0.5% of the surface per step), and removing the delta top changed nothing.
- **Experiment 1:** more crust makes more land rather than higher land.

### Targets

| Measure | Target | Before (7 seeds, 78 km) | Final, 12 seeds at 78 km | Final, 4 seeds at 31.25 km | Earth |
|---|---|---|---|---|---|
| Land SD, last 500 Myr | ≤ 4 points | 8.9 (31.25 km: 11.3) | **2.3** | **2.85** | — |
| Most land change within 50 Myr | ≤ 10 points | 25 (31.25 km: 30) | **8.0** | **8.5** | ~10 points over ~90 Myr (Cretaceous) |
| Land at 4.5 Gyr, by seed | 22–36% | 19–40% | 31.2–36.6% (mean 34.3; one seed 0.6 over) | 36.4–39.9% (mean 38.1) | 29% |
| Mean land height | ≥ 600 m | ~400 m | 580 m | 483 m | 656 m (793 m with ice sheets) |
| Shallow sea, 0–200 m | 5–10% of the ocean | 1–22% | 3.3% | 3.3% | 7.5% |
| Land per 100 m of sea level | (Earth) | 11 | 4.0 | 5.4 | 4.8 |
| Sea level SD, last 500 Myr | — | 104 m | 76 m | 72 m | — |
| Most sea-level change within 50 Myr | — | 262 m | 181 m | — | ~100–170 m rise into the Cretaceous |

Each driver's share of the sea-level variance, last 500 Myr (12 seeds at 78 km):
- ocean-floor age 0.83 (SD 100 m);
- ice 0.13 (SD 15 m; ice holds ~49 m of sea level on average);
- sediment 0.05.

Sea level now moves mostly with the volume of the ocean basins, as on Earth (Müller et al. 2008).

**Earlier eras** (4 seeds at 31.25 km). The targets cover the last 500 Myr, and the steadying is strongest there.

| Window | Land SD | Main driver |
|---|---|---|
| 500–2,000 Myr | 4.3–5.5 points | Continental growth (sea level rises as continents add area; ice, age and sediment explain little) |
| 2,000–3,500 Myr | 6.9–7.8 points | Ice, 0.56–0.84 of the sea-level variance (the Earth-like snowball era, excluded from the targets) |
| 3,500–4,500 Myr | 3.0–4.2 points | Ocean-basin volume (age and sediment) |

The timelapse format (seed 42, 2,000 Myr at 31.25 km) therefore still shows sizable swings:
- between 1,500 and 2,000 Myr, land SD 4.2 points and at most 16 points within 50 Myr (before: 7.1 and 21);
- the run ends on a low, 19% land (before: 35%), with half of its young continents flooded.

### Ocean water

Raising the water from 2,640 to 2,730 m moved the 12-seed mean land only from 34.3% to 33.6%.
- Over hundreds of Myr, erosion grades the land down to wherever the sea stands, so land ends up close to the area of continental crust (cont% ≈ land% in every seed).
- In the same 7 seeds, 90 m of water cost ~1.4 points of land in the long run, against the ~4 points that the instant sensitivity predicts.
- Reaching 29% would take ~3,000 m or more, 15% beyond Earth's water.

The water stays at 2,640 m.

Earth's lower land share needs continental crust that lies below the sea: rifted margins thinned to 10–25 km and sagging as they cool, flexed basins, drowned plateaus. That is the "continental accommodation" gap ([Marine spreading review](Marine%20spreading%20review.md) H1). It is a model of its own and was not in this plan.

### Side effects

| Measure (Earth-like present) | R1 + R2 alone (7 seeds) | Final (12 seeds) | Final, same 7 seeds |
|---|---|---|---|
| Global temperature | 15.7 °C (12.8–18.9) | 17.1 °C (15.3–20.6) | 17.8 °C |
| CO2 | 480 ppm | 650 ppm | 770 ppm |
| O2 | 1.55 PAL | 1.37 PAL | 1.20 PAL |
| Land under ice | 16.7% | 11.9% | 11.6% |

Weathering grows with erosion (fresh rock), so lowlands that erode at 1–3 m/Myr weather less. The carbon thermostat then balances at a warmer climate.
- `earthLandWeight` (0.6), which sets "Earth today = 1", was calibrated under n = 1. Recalibrating it would bring the present back to ~15–16 °C.
- One 31.25 km seed (1) ends at 0.31 PAL O2.

### Checks

- `--sea-drivers` leaves maps, history and events byte-identical.
- The new defaults reproduce the `--set slopeExponent=2 --set streamPowerK=250000` runs byte for byte.
- 1, 4 and 16 threads give identical files at 78 km, and 1 and 8 threads at 31.25 km (seed 42, all 16 outputs each).
- 55 tests pass. The isostasy reference test now checks GDH1 depths.
