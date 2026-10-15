# Why the Tibetan Plateau Stays ~5 km High, and How to Model It

Scope: physical mechanisms that hold up (or could sink) a collisional plateau, turned into cheap rules for the PlateTectonics grid model. The model uses 15–40 km cells and 1–10 Myr steps. Each cell stores crust thickness, crust type, ocean-floor age, crust formation time and owning plate. Elevation comes from Airy isostasy, `h = 0.1515 × (H_c − 33)` km.

Method note: research ran on 2026-10-15. Open-access papers were read in full where possible: Shi et al. 2018, Klemperer et al. 2022, Chen et al. 2017, Xie et al. 2024, Spicer et al. 2021, Chen 2021, Garzione et al. 2017 and Lee & Anderson 2015. Many other papers were checked only through abstracts or search-engine excerpts, and these are marked "(abstract)" or "(excerpt)". Hacker et al. 2000, Kay & Kay 1993, Clark & Royden 2000, Molnar & Lyon-Caen 1988 and Kapp & DeCelles 2019 could not be opened, so only their framing is used. "Inferences" subsections are my own reasoning and calculations, not literature facts. The comparison cases add some extra sources beyond the 10–25 target.

---

## Bottom line

1. **Tibet floats because its crust is thick and mostly buoyant.** It has 60–80 km of mostly felsic-to-intermediate crust (density ~2.8 g/cm³) on mantle of ~3.3 g/cm³. Airy isostasy alone predicts 4–7 km of elevation for that range. Crustal thickness is the first-order support, and nobody disputes it.
2. **The two things that could make it sink are partial, or have already been removed.**
   - (a) A cold, thickened mantle-lithosphere keel. Beneath central and northern Tibet this has largely been thinned or removed: foundering in the Eocene (central), the Oligocene (south) and the Miocene to present (north). It was replaced by hot asthenosphere, which adds buoyancy.
   - (b) Eclogite in the lower crust, which can be denser than mantle. The underthrust Indian lower crust is only about 30% eclogitized, because it is dry: metastable granulite reacts slowly without free water.
   - Removing dense roots raised parts of Tibet by ~1–2 km. It did not lower it.
3. **The plateau is supported differently in the south and the north.**
   - South: a "double crust" (Indian crust underthrust beneath Asian crust, Moho at ~70–80 km) on a cold, strong Indian plate. The eclogite and the cold Indian mantle make this region stand *lower* than its crust thickness alone predicts.
   - North: thinner crust (~60 km) on hot, thin lithosphere, which makes it stand *higher* than its crust alone predicts.
   - Both regions end up at ~5 km.
4. **Height is capped by gravity, not by a lack of crust.** Above ~5 km mean elevation the plateau spreads instead of rising. It has extended east–west on north–south rifts since ~19–8 Ma, and today that extension (~20 mm/yr) roughly matches north–south shortening (10–14 mm/yr). Hot, weak mid/lower crust also flows outward, mainly to the east and southeast. Gravity flattens the plateau and widens it.
5. **The model can produce all four outcomes with a few extra fields:** stay high, rise after root removal, stand low because of a dense root, or collapse after convergence stops. The fields are a mantle-lithosphere keel, an eclogite fraction with water-dependent kinetics, a stochastic delamination event, slope-limited lateral crustal flow, an elevation ceiling with extension, and underthrusting that spreads incoming crust over 150–300 km behind the suture. A three-term isostasy formula reproduces both southern and northern Tibet (see Q1 Inferences and the Model rules section).

---

## Q1. Why does the plateau stand ~5 km high and stay there?

### Cited findings

**Crustal thickness and Airy isostasy**
- The Qinghai–Tibet Plateau covers ~2.5 million km² with a mean elevation above 4.5 km and is ~1,000 km wide north–south. Cited estimates give ~70 km of crust and a ~4.8 km mean elevation after ~40% shortening since 50 Ma — [Spicer et al. 2021, *Natl Sci Rev*](https://pmc.ncbi.nlm.nih.gov/articles/PMC8288424).
- Textbook Airy calculation: with crust 2.8 and mantle 3.3 g/cm³, a 5 km plateau needs a 28 km root, which is ~33 km of extra crust (~68 km total for a 35 km reference crust) — [MIT 12.002 lecture 26](https://ocw.mit.edu/courses/12-002-physics-and-chemistry-of-the-terrestrial-planets-fall-2008/74fb28dc068682451773d2616e914ff2_MIT12_002f08_lec26.pdf).
- Gravity, geoid and topography modelling gives ~75 km crust and ~240 km lithosphere beneath the Himalaya and southern Tibet, and ~60 km crust and ~140 km lithosphere beneath northern Tibet — [Ravikumar et al. 2020, *Tectonics* (excerpt)](https://repository.iigm.res.in/items/eab4c228-5a8c-483a-8569-7c0f90c0f359). Receiver functions show the crust thinning by up to ~20 km from south to north, with signs of a hot, partly molten northern crust — [Owens & Zandt 1997, *Nature* (abstract)](https://ideas.repec.org/a/nat/nature/v387y1997i6628d10.1038_387037a0.html).
- Thick crust alone cannot support the whole plateau. The northern plateau needs a thin, hot lithosphere, and the south is compensated by thick crust — [Jiménez-Munt group, EGU 2006 (abstract)](https://meetings.copernicus.org/www.cosis.net/abstracts/EGU06/07623/EGU06-J-07623.pdf).

**Underthrust Indian crust ("double crust") beneath southern Tibet**
- Hi-CLIMB used an 800 km receiver-function profile at ~85°E. It traced the Main Himalayan Thrust into the mid-crust under southern Tibet, and Indian lower crust underplated beneath the Lhasa block to ~31°N, about 200 km north of the suture. The Indian upper mantle peels away from the crust and sinks diffusely — [Nábělek et al. 2009, *Science* (summary)](https://sciencedaily.com/releases/2009/09/090911164435.htm); [Hetényi 2007 PhD abstract](https://wp.unil.ch/orog3ny/files/2016/04/Hetenyi2007PhDabs.pdf). The thesis also concludes that Indian lower lithosphere reaches roughly the centre of the plateau. A strong upper mantle is needed to support Tibetan topography and regional isostasy in the Himalaya.
- West of the eastern syntaxis, Indian lithosphere underthrusts at progressively shallower angles and reaches progressively farther north toward the Tarim Basin — [Zhao et al. 2010, *PNAS* (abstract)](https://doi.org/10.1073/pnas.1001921107).
- Helium isotopes from hot springs place the "mantle suture" (the northern edge of Indian mantle) 0–170 km north of the Yarlung-Zangbo suture between 79° and 91°E. East of 85°E it lies 200–300 km south of the Indian *crustal* front, which suggests the Indian mantle has delaminated from Indian crust there. An asthenospheric wedge underlies about three-quarters of the plateau. The Indian Moho is ~650–700 °C beneath the Bangong-Nujiang suture. The Tibetan mantle wedge is >850 °C, and the Moho is ≥1,150 °C beneath the northern volcanic fields — [Klemperer et al. 2022, *PNAS*](https://pmc.ncbi.nlm.nih.gov/articles/PMC8944758).
- Tomography shows a sub-horizontal Indian lithosphere 100–150 km thick dipping ~10° north. Underthrusting averaged ~30 mm/yr over 25 Myr and is ~20 mm/yr today. The pre-collision lithosphere is assumed ~120 km thick — [Chen et al. 2017, *Nat. Commun.*](https://pmc.ncbi.nlm.nih.gov/articles/PMC5467168/).

**Removal of mantle lithosphere beneath central and northern Tibet**
- Thickened lithosphere is a cold thermal boundary layer that can become unstable, detach and be replaced by hotter asthenosphere — [Houseman, McKenzie & Molnar 1981, *JGR* (abstract)](https://earthref.org/ERR/34368).
- In numerical experiments, convective removal raises the surface by up to 2 km and raises gravitational potential energy (GPE) by 5–10 × 10¹² N/m. That is enough to make east–west extension dominant — [England & Houseman 1989, *JGR* (abstract)](https://earth.web.ox.ac.uk/node/2175826).
- Removal can raise mean elevation by ≥1,000 m in a few Myr. Basalts in northern Tibet that began ~13 Ma are melts of lithosphere, which points to removal of deeper lithosphere — [Molnar, England & Martinod 1993, *Rev. Geophys.* (abstract)](https://earthref.org/ERR/62502).
- India–Eurasia convergence slowed by >40% between ~20 and 10 Ma. A simple calculation shows that removing mantle lithosphere could raise Tibet by ~1 km and change the forces on both plates — [Molnar & Stock 2009, *Tectonics* (abstract)](https://resolver.caltech.edu/CaltechAUTHORS:20090821-081957305).
- A T-shaped fast anomaly between ~250 km depth and the transition zone, about 750 km wide, is read as continental lithosphere that foundered in the Oligocene. It is tied to pulsed uplift of southern Tibet at ~30–25 Ma and to ultrapotassic and adakitic magmatism (~30–9 Ma). Under northern Tibet, a ~200 km wide low-velocity zone shallower than ~125 km matches potassic volcanism (~15–0 Ma). There the lithosphere is 200–300 K warmer than the Indian lithosphere — [Chen et al. 2017](https://pmc.ncbi.nlm.nih.gov/articles/PMC5467168/).
- A 2D model of overriding-plate (Asian) delamination gives ~1–3 km of uplift from shortening plus ~2 km from replacing the lithospheric mantle with hot asthenosphere. Central Tibet rises to ~3 km within 8 Myr (53–45 Ma) and the plateau grows beyond 1,000 km wide at ~4 km — [Xie et al. 2024, *Nat. Geosci.*](https://pmc.ncbi.nlm.nih.gov/articles/PMC11245390/).
- Counter-case: Eocene adakitic rocks in Qiangtang record foundering of eclogitized central-Tibetan lower crust shortly after shortening. Uplift from this removal was probably **negligible**, because the crust was weak — [Zeng et al. 2021, *Geology* (abstract)](https://experts.arizona.edu/en/publications/negligible-surface-uplift-following-foundering-of-thickened-centr/).

**Eclogite in the lower crust: present, but partial and kinetically hindered**
- Bouguer gravity combined with the Hi-CLIMB geometry needs denser material (eclogite) beneath southern Tibet for isostasy. Eclogitization is complete only where the Indian lower crust reaches its maximum depth. Neither fully dry nor fully wet models fit: the Indian lower crust is partly hydrated, and eclogitization lags phase equilibrium until dehydration reactions release water at higher P–T — [Hetényi et al. 2007, *EPSL* (abstract)](https://hal.archives-ouvertes.fr/hal-00311551); [PhD abstract](https://wp.unil.ch/orog3ny/files/2016/04/Hetenyi2007PhDabs.pdf).
- Southern Tibet details from [Shi et al. 2018, *Nat. Commun.*](https://pmc.ncbi.nlm.nih.gov/articles/PMC6113232/):
  - The Moho is at ~80 km.
  - Below ~60 km the Indian lower crust is ~30% eclogitized, estimated from P-wave velocities.
  - Thermokinematic models put the start of eclogitization of the metastable Indian lower crust at ~873 ± 100 K (~600 °C).
  - Sample water is below 280 ppm, and xenoliths show the Indian lower crust lacks hydrous minerals.
  - The plagioclase-breakdown reactions cut volume by ~10–20%.
  - Long-term effective elastic thickness in the region is 70–120 km.
- Fluid, not only P–T, controls whether crustal roots eclogitize and delaminate. Partly eclogitized granulites (Norway, Polar Urals, Dabie-Sulu) show that complete conversion needs fluid — [Leech 2001, *EPSL* (abstract)](https://repository.geologyscience.ru/items/ffa3d036-60e5-4f19-9164-bc68638cf0c6/full).
- Dense lower-crustal lithologies can exceed mantle density by ~50–250 kg/m³. With a hot Moho (800–1000 °C), gabbroic and granulitic compositions stay *less* dense than mantle — [Jull & Kelemen 2001, *JGR* (abstract)](https://earthref.org/ERR/43457).
- In models, eclogitized lower crust about 90% converted is ~130 kg/m³ denser than mantle. A density rise of ≥7% over granulite localizes shortening above the root. Delamination happens if the eclogitized layer is weak, even when it is slightly *less* dense than mantle, because the attached mantle lithosphere supplies the negative buoyancy — [Krystopowicz & Currie 2013, *EPSL* (summary)](https://www.mantleplumes.org/Eclogitization.html).
- Not verified: Hacker et al. 2000 (*Science* 287:2463, "hot and dry deep crustal xenoliths") is the classic evidence for a hot, dry lower crust in central Tibet. I could not read its abstract. A related Qiangtang study reports a felsic, hot, dry lower crust and suggests any mafic material below it may have eclogitized and been lost — [Galvé et al. 2006 (excerpt)](https://hal-insu.archives-ouvertes.fr/insu-00150676).

### Inferences

**I1. An isostatic budget with three terms reproduces both halves of Tibet.** Each term is in km of surface elevation:
- Airy crust term: `0.1515 × (H_c − 33)`.
- Eclogite term: about `−0.13` per km of mafic lower crust fully converted. This assumes granulite ~3.0 → eclogite ~3.45 g/cm³ with mass conserved, so the surface drops by the volume lost, `d × (1 − 3.0/3.45)`.
- Thermal-keel term: about `−0.013` per km of normal-temperature mantle lithosphere beyond a reference ~85 km. This assumes α ≈ 3.2×10⁻⁵ K⁻¹ and an average excess ΔT ≈ 390 K, giving ≈ 40 kg/m³ excess density.

| Region | Crust term | Eclogite term | Keel term | Sum | Observed |
|---|---|---|---|---|---|
| South Tibet: H_c ≈ 75 km, mafic zone ≈ 20 km at 30% eclogite, mantle lithosphere ≈ 165 km, cold | +6.36 | −0.78 | −1.04 | **≈ 4.5 km** | ~4.5–5 km. The gap is plausibly flexural support from the strong Indian plate. |
| North Tibet: H_c ≈ 60–65 km, no eclogite, mantle lithosphere ≈ 70 km, hot (thermal weight ~0.4) | +4.1 to +4.85 | 0 | +0.74 | **≈ 4.8–5.6 km** | ~4.8–5 km |

So the "missing" support in the north is heat, and the "excess" crust in the south is cancelled by dense roots. The current single-term model overshoots the south (75 km gives 6.4 km) and undershoots the north (60 km gives 4.1 km).

**I2. Removing the root raises the surface; removing eclogite alone barely does.** Replacing eclogite (~3.45) with asthenosphere (~3.25) gives little uplift. Most delamination uplift comes from removing the cold mantle keel and from heating. This matches Zeng et al. 2021 (eclogite foundering, negligible uplift) and England & Houseman / Molnar (keel removal, 1–2 km).

**I3. Why Tibet did not sink:**
- Its crust is mostly felsic. Felsic rocks never reach mantle density.
- Its mafic lower crust is dry, so it converts slowly.
- Dense roots that did form were shed, not kept.
- Hot asthenosphere now sits close beneath much of the plateau.
- The southern edge is held up flexurally by the strong Indian plate.

Sinking needs a *wet, mafic, attached* root under crust too strong to let it go. In the Andes, the Eastern Cordillera "stayed low" until its root dropped off (Q4).

---

## Q2. What limits the height, and what makes the plateau flat and wide?

### Cited findings

**Gravitational potential energy and extension**
- Raising a range takes ever more work against gravity, so past some height a range spreads sideways instead of rising. Simultaneous extension at high elevation and shortening on the flanks can be used to estimate average lithospheric strength — [Molnar & Lyon-Caen 1988, GSA Spec. Pap. 218 (review blog)](https://blogs.egu.eu/divisions/ts/?p=8832). I could not access the paper's numerical bound.
- 85% of normal-faulting moment release in Tibet over 43 years occurred where surface height exceeds 5 km. This fits normal faulting driven by GPE contrasts. Seismic moment accounts for only 3–4 mm/yr, or 15–20% of the GPS extension rate — [Elliott et al. 2010, *GJI* (abstract)](https://doi.org/10.1111/j.1365-246X.2010.04754.x).
- In the Andes, recent normal faulting is concentrated where mean elevation exceeds ~3.5–4 km — Dalmayrac & Molnar 1981, as cited in [a 2020 South Peru study (excerpt)](https://cris.pucp.edu.pe/en/publications/observations-and-dynamical-implications-of-active-normal-faulting/).
- GPS shows ENE–WSW stretching of 21.6 ± 2.5 mm/yr between 79° and 93°E, and NNE–SSW shortening of 10–14 mm/yr across the plateau — [Zhang et al. 2004, *Geology*](https://ncedc.org/ftp/outgoing/burgmann/REPRINTS/Zhang_GEOLOGY2004.pdf) (extension value via a secondary citation).
- Rift onset estimates vary by method and place:
  - ≥13.5 Ma minimum for a central-Tibet graben, with earlier estimates of 14–8 Ma in the south and ~4 Ma in the centre — [Blisniuk et al. 2001, *Nature* (abstract)](https://www.nature.com/articles/35088045).
  - 8 ± 1 Ma at the Nyainqentanglha detachment (Harrison et al. 1995), and 19.0 ± 1.1 Ma at Tangra Yumco (2023 thermochronology).
  - Overall, 19–8 Ma depending on region (excerpts summarized in the same search). Wikipedia's synthesis says ~14 Ma — [Orogenic collapse](https://en.wikipedia.org/wiki/Orogenic_collapse).
- Molnar suggests in an interview that the plateau may have been ~5.5 km high in the mid-Miocene and has subsided ~1 km over the past ~15 Myr by east–west extension — [Geology Bites podcast notes](https://www.geologybites.com/peter-molnar).

**Lithospheric strength and support**
- A strong upper mantle is needed to support the plateau's topography and regional isostasy in the Himalaya — [Hetényi 2007 PhD abstract](https://wp.unil.ch/orog3ny/files/2016/04/Hetenyi2007PhDabs.pdf). Long-term effective elastic thickness is 70–120 km — [Shi et al. 2018](https://pmc.ncbi.nlm.nih.gov/articles/PMC6113232/).

**Lateral (channel) flow of the lower crust**
- Under isostasy, weak lower crust undergoes Poiseuille flow that removes crust from under mountains and levels topography.
  - Lateral extrusion is insignificant under oceans, marginal under shields, important under elevated plains, and dominant beneath high plateaus and hot, delaminated regions.
  - With cubic power-law creep, flux scales with (topographic gradient)³ and roughly with T¹⁰ of the lower crust.
  - In the Basin and Range, Moho relief >1 km at 100 km wavelength cannot survive more than 10–20 Myr.
  - Source: [Bird 1991, *JGR* (abstract)](https://earthref.org/ERR/38766).
- Low-gradient plateau margins (SE and NE Tibet) need mid/lower-crustal viscosity of ~10¹⁸ Pa·s in a 15 km channel. The steep Sichuan margin needs ≥10²¹ Pa·s. Crustal thickness gradients from the 60–70 km plateau to the 35 km foreland drive the flow — [Clark & Royden 2000, *Geology* (excerpt)](https://doi.org/10.1130/0091-7613%282000%2928%3C703:TOBTEM%3E2.0.CO;2).
- A best-fit model uses 2×10¹⁸ Pa·s in a 15 km channel with an average flow of 80 mm/yr. That rate is a prescribed model input, not a measurement. The model makes up to 1.5 km of dynamic topography near rigid blocks — Clark, Bush & Royden 2005, *GJI* ([excerpt](https://thales.mit.edu/bush/?p=3937); [ASPECT cookbook using U₀ = 80 mm/yr](https://aspect-documentation.readthedocs.io/en/latest/_sources/user/cookbooks/cookbooks/lower_crustal_flow/doc/lower_crustal_flow.md)).
- When rapid extrusion toward the Pacific and Indonesian trenches slowed ~15–20 Ma, eastern Tibet thickened and rose fast by eastward flow of deep crust in seismically imaged channels — [Royden, Burchfiel & van der Hilst 2008, *Science* (abstract)](https://www.osti.gov/biblio/1479291).
- Magnetotellurics image two conductive channels at 20–40 km depth running >800 km from the plateau into SW China, consistent with weak, fluid-rich crust — [Bai et al. 2010, *Nat. Geosci.* (abstract)](https://www.doi.org/10.1038/NGEO830). A later 3D magnetotelluric study disputes the SE-directed channels, so channel flow remains debated.
- In Himalayan models, channel flow needs viscosity to drop to ~10¹⁹ Pa·s by partial melt at 700–750 °C. Flow is then ~1 cm/yr — Beaumont et al. 2001, *Nature* ([record](https://ideas.repec.org/a/nat/nature/v414y2001i6865d10.1038_414738a.html); numbers from the group's related abstracts).

**Flatness**
- Fossils and isotopes suggest central Tibet was a deep east–west valley through much of the Paleogene. It was filled ("bathtub" sedimentation) and raised to ~5 km only in the Neogene — [Spicer et al. 2021](https://pmc.ncbi.nlm.nih.gov/articles/PMC8288424).

### Inferences

**I4. Maximum sustainable mean elevation is about 5.0 ± 0.5 km for a broad plateau under continent–continent collision, and ~4 km under Andean-type subduction forcing.**
- Evidence: Tibet's normal faulting is concentrated above 5 km, the Andes' above 3.5–4 km, and Molnar's suggested ~5.5 km mid-Miocene peak.
- The ceiling depends on the boundary force holding the plateau up, which is larger for India's push than for Nazca coupling.
- Narrow ranges on a strong plate (the central Himalaya averages ~6 km; Spicer et al.) can exceed this because they are flexurally supported. The ceiling applies to features wider than roughly 200–300 km (my guess).

**I5. Tibet's thickness is near steady state today.**
- East–west stretching of ~21.6 mm/yr across ~1,320 km gives ~1.6%/Myr. North–south shortening of 10–14 mm/yr across ~1,000 km gives ~1.0–1.4%/Myr.
- Net vertical thinning is therefore ~0.2–0.6%/Myr. This is the same order as Molnar's ~1 km of subsidence in 15 Myr, which needs ~0.6%/Myr of thinning for 65 km of crust.
- In the model, the plateau should stop thickening once it reaches the ceiling, with excess input exported sideways.

**I6. Channel-flow diffusivity.** For Poiseuille flow in a channel of thickness h_ch driven by surface slope, crustal thickness diffuses with κ = h_ch³ · ρ_c · g · 0.1515 / (12 μ).

| Viscosity μ, 15 km channel | κ (km²/Myr) | Spread in 10 Myr, √(κt) |
|---|---|---|
| 10¹⁸ Pa·s | ≈ 37,000 | ≈ 600 km |
| 10¹⁹ Pa·s | ≈ 3,700 | ≈ 190 km |
| 10²¹ Pa·s (Sichuan-type wall) | ≈ 37 | ≈ 20 km (effectively a wall) |

- Margin advance of several hundred km in 10–20 Myr (channels >800 km long, active since ~15–20 Ma) implies an effective ~40–60 km/Myr. That fits μ ≈ 10¹⁸–10¹⁹.
- Because flux scales with h_ch³ and very steeply with temperature (Bird), flow behaves almost like a threshold. It is negligible in normal crust and fast once thick crust is hot.

---

## Q3. Timeline and main debates

### Cited findings

| Event | Age | Source | Notes / debate |
|---|---|---|---|
| India–Asia initial collision | 59 ± 1 Ma (central–eastern Himalaya, provenance switch); 65–60 Ma (Ding et al.); ~54 Ma closure in the western Himalaya | [Hu et al. 2016 (abstract)](https://boa.unimib.it/handle/10281/292577); [Ding et al. 2022 (abstract)](https://experts.arizona.edu/en/publications/timing-and-mechanisms-of-tibetan-plateau-uplift/) | Proposals range from ~70 to ~34 Ma (Aitchison et al. 2007). Two-stage collision models are debated. |
| Gangdese (southern Tibet) already high | ~4.5 ± 0.4 km by ~56 Ma (Linzhou Basin isotopes); E–W high belt in southern Tibet by ~55 Ma | [Spicer et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC8288424); [Ding et al. 2022](https://experts.arizona.edu/en/publications/timing-and-mechanisms-of-tibetan-plateau-uplift/) | An Andean-type margin was high *before* collision. |
| Central Tibet range high | by ~45 Ma; basins ≤2 km between belts until ~38–29 Ma | Ding et al. 2022 | |
| Central Tibet lower-crust foundering | Eocene, right after shortening; negligible uplift | [Zeng et al. 2021](https://experts.arizona.edu/en/publications/negligible-surface-uplift-following-foundering-of-thickened-centr/) | Xie et al. 2024 instead model ~3 km uplift of central Tibet at 53–45 Ma from Asian-plate delamination. |
| Central valley elevation | <2.3 km at 25.5 ± 0.5 Ma (palms, Lunpola) vs 4.5–5 km at ~26 Ma (isotopes, Nima) | [Su et al. 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6402856); Spicer et al. | **Main debate: early (Eocene–Oligocene) vs late (Neogene) high plateau.** Spicer et al. argue the isotopes record the surrounding ridges. |
| Southern Tibet lithosphere removal | ~30–25 Ma (tomography); ~26 Ma (magmatism) | [Chen et al. 2017](https://pmc.ncbi.nlm.nih.gov/articles/PMC5467168/); [Chung et al. 2005 (abstract)](https://ir.sinica.edu.tw/handle/201000000A/50932) | |
| Lhasa ultrapotassic + adakitic magmatism | ~26–10 Ma (Chung); ~30–9 Ma (Chen) | as above | Adakites are melts of garnet-bearing (eclogitic or amphibolitic) thick lower crust. Ultrapotassic rocks are melts of metasomatized mantle lithosphere. |
| Suture zone ≥4 km; Himalaya ≥5 km | ~20 Ma; ≥5 km by ~15 Ma | Ding et al. 2022; Spicer et al. | Ding et al. call the lag after collision a key open question. |
| India convergence slows >40% | ~20–10 Ma | [Molnar & Stock 2009](https://resolver.caltech.edu/CaltechAUTHORS:20090821-081957305) | Read as a plateau rise of ~1 km, increasing GPE. |
| E–W extension (N–S rifts) | onset ~19–8 Ma; ≥13.5 Ma in central Tibet | [Blisniuk et al. 2001](https://www.nature.com/articles/35088045) and others | Diachronous; the single-date "8 Ma" view is outdated. |
| Northern Tibet basalts / potassic volcanism | from ~13 Ma (Molnar et al.); ~15–0 Ma (Chen et al.) | [Molnar et al. 1993](https://earthref.org/ERR/62502); Chen et al. 2017 | Lithosphere source points to removal or thermal erosion of the north. |
| Eastern Tibet uplift by crustal flow | since ~15–20 Ma | [Royden et al. 2008](https://www.osti.gov/biblio/1479291) | Channel flow vs upper-crustal shortening is debated. |
| Plateau interior at ~5 km | Neogene; Namling ~4.7–5.5 km at 15 Ma, where isotopes and fossils agree | Spicer et al. | |

Mechanism synthesis: delamination and break-off of subducted Indian and Asian lithosphere were the dominant uplift drivers, with spatial variation controlled by inherited heterogeneity — [Ding et al. 2022](https://experts.arizona.edu/en/publications/timing-and-mechanisms-of-tibetan-plateau-uplift/). Spicer et al. contrast this with "oblique stepwise" northward growth (Tapponnier et al. 2001) and outward growth from a proto-plateau.

**Main open debates as of 2026:**
- Early vs late uplift, and isotope vs fossil paleoaltimetry.
- How far Indian lithosphere underthrusts. Estimates range from ~31°N or the helium suture to the Jinsha suture, and it varies with longitude.
- Whether removal events produced uplift (Chen 2017 and Xie 2024 say yes; Zeng 2021 says negligible).
- Channel flow vs crustal shortening at the eastern margin.
- Whether 2025 receiver-function work showing tearing of the Indian plate (ESS Open Archive preprint, not peer-reviewed in what I found) holds up.

### Inferences
- Tibet did not rise as one block. Each sector followed its own sequence: thickening, then root removal or heating, then uplift, then extension or spreading. The sequence stepped northward over ~40 Myr (south ~55 → 25 Ma, centre ~45 → 15 Ma, north ~20 → 0 Ma).
- In the model, root removal should be a *local* stochastic event that repeats across the plateau, not one global switch.
- Delays of ~15–30 Myr between thickening and removal or magmatism appear repeatedly: southern Tibet ~25–30 Myr after collision, model delamination at 15–40 Myr, and arc cycles of 10–30 Myr.

---

## Q4. Comparable cases: what decides whether a thick orogen stays high, collapses or sinks?

### Cited findings

| Case | What happened | Key numbers | Source |
|---|---|---|---|
| Central Andes: Altiplano and Eastern Cordillera | Pulsed uplift from punctuated removal of dense lower lithosphere, plus lower-crustal flow | Moho 65 km (central Altiplano) to 75 km (north); Eastern Cordillera 55–70 km. Rapid changes of ≥1.5 km in 1–4 Myr after tens of Myr of stability. Central Altiplano rose from <2 to ~4 km at 10–6 Ma; northern Altiplano from 1–2 km at 9 Ma to 4 km by 5.4 ± 1 Ma, and its basin switched from shortening to normal faulting. Eastern Cordillera removal events at 24–17 Ma and again at 10–6 Ma *at the same place*; Moho shallowed by 10 km. 25–22 Ma basaltic and shoshonitic lavas from <90 km. Ignimbrite flare-ups at 9–5 Ma. | [Garzione et al. 2008 (summary)](https://www.sciencedaily.com/releases/2008/06/080605150912.htm); [Garzione et al. 2017, *AREPS*](https://pubs.usgs.gov/publication/70198471) |
| Andes: dense root still attached | A remnant high-velocity root to ~120 km under the Titicaca region | That region sits ~500 m *lower* than its surroundings, possibly a root in the process of foundering | Garzione et al. 2017 |
| Andes: crust thick but surface low | The Eastern Cordillera reached its crustal thickness early but stayed low, then rose quickly after shortening stopped | Taken as evidence that an eclogitic root held the surface down until it was removed | Garzione et al. 2017 |
| Andes: shortening vs keel | Southern Altiplano crust went from 44 to 57 km (16–9 Ma) | That gives ~2.4 km isostatic uplift *if* the mantle keel does not thicken too; minimal uplift if the keel thickens at the same rate | Garzione et al. 2017 (crust 2.7, mantle 3.3) |
| Puna | Delamination of eclogitic lower crust plus mantle (Kay & Kay 1993) | Imaged high-Vp body below the Moho at ~90 km beside the Cerro Galán caldera | [Chen et al. 2019 Puna tomography (abstract)](https://arxiv.org/abs/1912.00854) |
| Sierra Nevada (arc root) | A 70 km eclogite-facies root under 30–35 km granitic crust survived to 8–12 Ma, then foundered as a Rayleigh–Taylor drip (onset 10–3 Ma) | The remaining ~35 km crust thickened ~7 km in a narrow welt; present-day subsidence above the drip | [Zandt et al. 2004, *Nature* (abstract)](https://experts.arizona.edu/en/publications/active-foundering-of-a-continental-arc-root-beneath-the-southern-/) |
| Arc delamination cycle | Dense garnet-pyroxenite ("arclogite") cumulates grow, then founder; a magmatic pulse follows | Δρ ≈ 500 kg/m³ vs asthenosphere; root 10–35 km thick at foundering; cycle every 10–30 Myr. Cold (≤800 °C) roots are too stiff (~10²² Pa·s) to drip and need a weak lower-crustal layer (~10¹⁹ Pa·s, 1–5 km) to detach. | [Lee & Anderson 2015, *Sci. Bull.*](https://www.mantleplumes.org/WebDocuments/LeeandAnderson2015.pdf) |
| Delamination model timing | Weak lower crust (diorite or mafic granulite) gives delamination; strong lower crust gives underthrusting instead | Breakoff at ~15 Myr (reference), 22 Myr (mafic granulite, 2.5 cm/yr), 41 Myr (1 cm/yr). Delamination finishes in ~2 Myr vs ~25 Myr for dripping. The centre subsides ~4 km before breakoff, then rebounds to ~2 km. Uplift is focused in ~200 km. | [Chen 2021, *Front. Earth Sci.*](https://www.frontiersin.org/journals/earth-science/articles/10.3389/feart.2021.755519/full); [Göğüş & Pysklywec 2008 (abstract)](https://avesis.itu.edu.tr/yayin/d9988bb0-8f06-465c-9f9d-85b85327b30e/near-surface-diagnostics-of-dripping-or-delaminating-lithosphere) |
| Delamination vs drip surface signature | Delamination: broad uplift over the gap, plus a moving zone of subsidence at the peeling hinge. Drip: symmetric, fixed. | Delamination heats the crust faster and longer | Göğüş & Pysklywec 2008 |
| Basin and Range / Nevadaplano | Late Cretaceous–Paleogene plateau that later collapsed | Plateau crust 50–60 km at 2.5–3.5 km elevation (or ~56.5 km and ~3.95 km at 36 Ma); now 30–34 km. Some data show ≤500 m mean elevation change and mass added by magmas, so the collapse picture is debated. | [SERC 2018 abstract](https://serc.carleton.edu/NAGTWorkshops/structure/2018_Forum/abstracts/192888.html); [SCEC reconstruction](https://central.scec.org/node/37160); [Wikipedia](https://en.wikipedia.org/wiki/Orogenic_collapse) |
| Variscan (Europe) | Plateau ~345–310 Ma, then gravitational collapse on detachments, exhuming migmatite domes at ~305–295 Ma | Delamination widespread but its role debated; gravity alone may not explain thinning | [Massif Central synthesis, BSGF 2020](https://doaj.org/article/4ea7eb2cdbed497f943160aa8a691945); [Wikipedia](https://en.wikipedia.org/wiki/Orogenic_collapse) |
| Grenville | Large, hot, long-lived orogen (~1090–980 Ma) with an orogenic plateau that collapsed by extension of a high-strain channel | — | [Rivers 2015](https://www.geosciencecanada.ca/rivers_42-1.pdf); [Swanson-Hysell et al. 2022](https://geophysics.earth.northwestern.edu/seismology/suzan/publications/101_SwansonHysell_et_al_GSAMemoir_2022.pdf) |
| Southern Urals, southern Trans-Hudson | "Arrested development": no eclogitization, delamination or collapse; the crustal base stayed fluid-absent | — | [Leech 2001](https://repository.geologyscience.ru/items/ffa3d036-60e5-4f19-9164-bc68638cf0c6/full) |
| Deep "eclogite" basins (minority hypothesis) | Gabbro→eclogite conversion in the lower crust proposed to drive deep basins without stretching (North/East Barents, Caspian) | ~15 km eclogite layer → ~14 km sediment-loaded subsidence. Strongly disputed on petrology, kinetics and volumes. | [Artyushkov et al. 2013 (abstract)](https://www.searchanddiscovery.com/abstracts/html/2013/90177arctic/abstracts/artyu.htm); critique: [Dobretsov & Polyansky 2010](https://doi.org/10.1016/j.rgg.2010.11.006) |

General mechanism summary: delamination needs (1) lower lithosphere denser than asthenosphere and (2) a pathway for asthenosphere to reach the crust. The mafic granulite → eclogite change is the main source of density inversion. Removal causes rapid uplift and volcanism — [Wikipedia: Delamination](https://en.wikipedia.org/wiki/Delamination_%28geology%29).

### Inferences: the decision tree for a thickened orogen

1. **Is convergence still pushing?** Yes: the orogen stays near its ceiling (Tibet, Altiplano). No: it collapses if hot (Basin and Range, Variscan, Grenville), or just erodes if cold.
2. **Is the lower crust hot and weak** (thick crust for >10–20 Myr, radiogenic heating, delamination, or melt)? Yes: channel flow and extension flatten and widen the plateau, and they also allow roots to detach. No (cold, dry, strong): arrested development (Urals, Trans-Hudson). The root stays, relief decays only by erosion, and any thick keel holds the surface lower than Airy predicts.
3. **Is there water plus mafic lower crust?** Yes: eclogite forms quickly and the root becomes dense. While the root stays attached, the surface stands low or subsides (Eastern Cordillera before removal, Titicaca, Sierra Nevada drip). Once it detaches, the surface rebounds 1–2+ km with a magmatic pulse. No (dry or felsic): eclogite is slow, so the crust stays buoyant and stays high (Tibet).
4. **Is the keel cratonic?** Yes: it is compositionally buoyant and does not delaminate easily. It acts as a rigid wall (Sichuan Basin, Tarim) that blocks crustal flow and creates steep margins.
5. A true "sinking plateau" is not observed. Dense attached roots cause *transient* subsidence or stunted uplift (hundreds of m to ~1–2 km, or ~4 km in an extreme 2D model) that lasts until they detach. Permanent eclogite-driven basins are a contested minority view, best kept as an optional rare mode.

---

## Q5. Model rules

Design goals: few new fields, all updates local or neighbour-only, stable at 1–10 Myr steps, and the existing Airy formula kept as the first term. Each rule below lists its parameter values and a confidence rating:

- **High (H):** directly measured or textbook physics.
- **Medium (M):** literature range, applied loosely.
- **Low (L):** calibration guess. Tune it in-game.

### Proposed new per-cell fields

| Field | Units | Meaning | Initial value | Confidence |
|---|---|---|---|---|
| `lithMantleThk` (L_m) | km | Mantle-lithosphere thickness below the Moho | 85 for normal continent (120 km lithosphere − 35 km crust; Chen et al. 2017 use ~120 km pre-collision); 150–220 for cratons | M |
| `lithThermalFactor` (θ) | 0–1 | Mean thermal density excess relative to normal lithosphere (1 = normal or cold; 0.3–0.5 = freshly heated after removal) | 1.0 | L |
| `cratonic` | bool | Keel is compositionally buoyant: no keel elevation term, no delamination, acts as a rigid wall for flow | from craton generation | M |
| `maficLowerThk` (d_maf) | km | Thickness of eclogitizable mafic lower crust, near the base of the crust | 0.3 × H_c for normal crust (~10–12 km); 0.4–0.5 for arc roots or underplated LIP crust; 0.1–0.2 for felsic-dominated crust | L |
| `eclogiteFrac` (f_e) | 0–1 | Converted fraction of the mafic lower crust lying below the eclogite depth | 0 | M (target ~0.3 in a Tibet analogue) |
| `lowerCrustWater` (w) | 0–1 | Fluid availability that controls eclogite kinetics | 0.05 for old cratonic or granulite crust; 0.2–0.4 for underthrust passive-margin crust (Hetényi: partly hydrated); 0.7–1.0 for arc roots and crust above a recent oceanic slab | L |
| `thickeningStart` | Ma | When the crust last rose above ~45 km; gives incubation time and a proxy for heating | unset | — |
| `lastRootRemoval` | Ma | Time of the last delamination; drives the magmatism and heat pulse and the lithosphere regrowth | unset | — |
| `underthrustTag` (optional) | plate id | Lower crust or keel inherited from the downgoing plate, useful for rock types and ores | unset | — |
| Event outputs (optional) | — | `magmaPulse` type and end time (adakitic / (ultra)potassic / basaltic-shoshonitic / ignimbrite); `heatPulse` | — | — |

H_c in this design is the "unconverted-equivalent" crust thickness, proportional to crustal mass. Eclogite shrinkage is applied through the elevation term, not by editing H_c.

### Rules, parameters and confidence

| # | Rule | Formula / logic | Parameters | Confidence |
|---|---|---|---|---|
| R1 | **Elevation (continental)** | `h = 0.1515·(H_c − 33) − c_e·f_e·d_zone − c_k·(L_eff − L_ref)`, with `d_zone = max(0, min(d_maf, H_c − z_ecl))` and `L_eff = θ·L_m` (cratons: `L_eff = L_ref`) | c_e = 0.13 (range 0.10–0.16, granulite 2.95–3.05 → eclogite 3.40–3.50); c_k = 0.013 (0.010–0.015; α 3–3.5×10⁻⁵ K⁻¹, ΔT_mean ≈ 350–420 K); L_ref = 85 km; z_ecl = 50 km | Structure H; c_e and c_k M |
| R2 | **Eclogitization** | If d_zone > 0 and Moho is hot (proxy: H_c ≥ 55 km, or > 5 Myr since `thickeningStart`): `f_e += (1 − f_e)·(1 − exp(−k·dt))`, where `k = k_dry + w·(k_wet − k_dry)` | z_ecl 45–55 km (P ≈ 1.2–1.5 GPa); start at ~600 ± 100 °C (Shi et al.); k_dry = 0.01–0.02 /Myr (gives ~30% in 20–30 Myr, matching the Indian lower crust); k_wet ≥ 0.5 /Myr (near-complete in 1–2 steps); optional ×3 on k when H_c > 70 km (dehydration releases internal water, per Hetényi) | Thresholds M; k_dry L (calibrated to one case); k_wet L |
| R3 | **Keel thickening** | Distributed (pure-shear) thickening by factor F = H_new/H_old also sets `L_m *= F` (cap 250 km), and `θ` stays the same. Underthrust zone: see R8. | — | M (Garzione: keel thickening cancels uplift) |
| R4 | **Root load (diagnostic)** | `R = c_k·max(0, L_eff − L_ref) + c_e·f_e·d_zone` (km of surface held down by roots) | — | derived |
| R5a | **Delamination trigger** | Non-cratonic cell with H_c ≥ 55 km, `now − thickeningStart ≥ 10 Myr`, and R ≥ R_crit. Fire with probability `1 − exp(−λ·dt)`. | R_crit = 0.8–1.5 km; λ = 0.05–0.1 /Myr (mean wait 10–20 Myr). This gives onset ~15–40 Myr after thickening (models 15–41 Myr; S Tibet ~25–30 Myr after collision; arc cycles 10–30 Myr). Optional ×0.2 hazard where the keel belongs to a still-converging downgoing plate. | L |
| R5b | **Delamination event** | Patch = connected cells meeting R5a within radius r_p of the trigger cell. In the patch: remove the converted layer (`H_c −= f_e·d_zone`, `d_maf −= f_e·d_zone`, `f_e = 0`); set `L_m = L_resid`, `θ = θ_hot`; set `lastRootRemoval = now`. Uplift then follows from R1, giving ~1–2.5 km. | r_p = 100–300 km (uplift focused ~200 km wide; S Tibet foundered body ~750 km wide); L_resid = 0–20 km; θ_hot = 0.3–0.5; duration ≤ 1 step (delamination ~2 Myr) | Uplift size M; geometry L |
| R5c | **Drip alternative (optional, continuous)** | If not cratonic, H_c ≥ 50 km and L_eff > 1.3·L_ref: `L_m −= (L_m − L_ref)·(1 − exp(−dt/τ_drip))` | τ_drip = 15–30 Myr (Göğüş & Pysklywec ~25 Myr) | L |
| R5d | **After removal** | Regrowth: `L_m → L_ref` with τ_regrow ≈ 50 Myr (40–100; conductive cooling of ~85 km takes ~40 Myr). `θ → 1` with τ_θ ≈ 30–50 Myr. This gives ~1 km of slow thermal subsidence over ~100 Myr. | — | M |
| R5e | **Magmatism and heat pulse** | Set for `pulse = 5–15 Myr` after removal. H_c ≥ 50 km: adakitic (garnet-bearing lower-crust melts) plus (ultra)potassic. Otherwise basaltic/shoshonitic. Ignimbrite flare-up if H_c ≥ 60 km and w ≥ 0.3. Moho temperature to ~900–1,150 °C. Raise flow mobility (R6) ×3–10 while the pulse lasts. | Durations: Lhasa ~26–10 Ma; Andes pulses a few Myr; N Tibet ~15–0 Ma. Moho temperatures from Klemperer et al. Surface heat-flow rise ~+20–40 mW/m², lagging 10–30 Myr. | Types M; numbers L |
| R6 | **Lateral crustal flow** (volume-conserving) | For each neighbour pair (i, j) with i non-cratonic and `H_c,i ≥ H_flow`: move crust `ΔV = κ_ij·dt/dx²·(h_i − h_j)/0.1515·A` from higher to lower elevation, with `κ_ij = κ_ref·(h_ch/15 km)³` and `h_ch = clamp(H_c,i − 45, 0, 30)` km. Limit each transfer to ≤ (H_i − H_j)/(2(n+1)), using sub-steps or an implicit solve. Use κ ≈ 0 into or out of cratonic cells (walls). Transfer `d_maf` proportionally, and thin `f_e` by mixing. | H_flow = 50–55 km; κ_ref = 3,700 km²/Myr (μ = 10¹⁹ Pa·s, 15 km channel); range 370–37,000 (μ 10²⁰–10¹⁸). Expected margin advance 40–60 km/Myr. Explicit stability needs κ·dt/dx² ≤ 0.25, so with dx = 30 km and dt = 1 Myr use ≥ 16 sub-steps (active cells only) or an implicit solve. | Physics M; numbers L (channel flow is debated) |
| R7 | **Elevation ceiling and extension** | If `h > h_ceil`: `H_c −= (h − h_ceil)/0.1515·(1 − exp(−dt/τ_ext))`, and export the removed volume to lower neighbours through the R6 routine; do not delete it. Applied only to features wider than ~3–10 cells (200–300 km); narrow ranges at plateau edges are allowed up to ~6 km. | h_ceil = 5.0–5.5 km with an active continental collision within ~1,000 km; 4.0 km for subduction-driven plateaus; τ_ext = 5–10 Myr | h_ceil H/M; τ L |
| R7b | **Post-convergence collapse** | If there has been no active convergence within ~1,000 km for > 5 Myr: if the lower crust is hot (H_c > 45 and (θ < 0.8 or < 60 Myr since thickening)), lower h_ceil toward 1–2 km with τ_collapse = 10–20 Myr, so 50–60 km crust becomes ~30–35 km in ~20–30 Myr (Basin and Range). Otherwise keep only the existing ~200 Myr erosional relaxation (Urals/Trans-Hudson "arrested"). | — | M (direction); L (rates) |
| R8 | **Underthrusting at continent–continent collision** (replaces "add 85% of over-run crust to the collision cell, cap 75 km") | Each step, the incoming downgoing crust `H_down·v·dt` splits into: f_acc → frontal wedge (first 1–3 cells: a Himalaya-like range with high erosion); f_ut → spread under overriding cells within distance D_ut of the suture with a linearly decreasing weight, added to H_c and to d_maf (lower part) and tagged with the downgoing plate's water w; f_lost → mantle. The downgoing mantle lithosphere replaces the overriding keel under the underthrust band: `L_m = max(L_m, L_m,down)`, `θ = 1` (cold). Cells beyond D_ut thicken by distributed pure shear (R3) using any convergence not absorbed. | f_acc ≈ 0.3–0.4 (upper crust), f_ut ≈ 0.45–0.6, f_lost ≈ 0.1–0.15 (keeps today's 85% retention); D_ut = 150–300 km for crust (Hi-CLIMB ~200 km), 200–600 km for mantle (debated; longitude-dependent); underthrust rate 20–30 km/Myr (Chen et al.) | Geometry M; split fractions L |
| R9 | **Safety caps** | H_c ≤ 85–90 km (S Tibet 75–85 km; the Pamir is locally thicker); cell-mean h ≤ 6.5 km | — | H |

### Order of operations per time step (suggested)

```
1. Plate motion and collision
   - R8: underthrust distribution
   - R3: thicken interior crust and keel
   - set thickeningStart when H_c first rises above 45 km
2. R2: eclogite kinetics
3. R5a/R5b: delamination events, or R5c drip; then R5e pulses
4. R5d: lithosphere regrowth and theta recovery
5. R1: compute elevation
6. R7/R7b: ceiling and collapse; collect the exported crust
7. R6: lateral flow (sub-stepped or implicit; includes exported crust)
8. Existing erosion relaxation (optionally faster at plateau margins, slower inside)
9. R1: recompute elevation for output
```

### Calibration targets (what a "Tibet-like" run should produce)

| Target | Value | Source / confidence |
|---|---|---|
| Plateau mean elevation and flatness | 4.5–5.0 km, interior variation within roughly ±0.5 km over ~1,000 km N–S | Spicer et al.; H |
| Crust thickness | ~75 (60–85) km south; ~60 km north | Ravikumar et al., Shi et al.; H |
| South vs north support | S: thick cold keel plus ~30% eclogite in the deepest lower crust. N: thin, hot lithosphere. Both ~5 km. | I1; M |
| First root removal | ~15–30 Myr after collision; local uplift 1–2.5 km in ≤ 1–4 Myr; adakitic/potassic magmatism for ~5–15 Myr | Chen 2017, Garzione 2008, England & Houseman; M |
| Extension onset | Once broad regions reach h_ceil (Tibet: ~10–40 Myr after collision; observed 19–8 Ma) | Blisniuk et al. and others; M |
| Lateral growth | Weak margins advance several hundred km in 10–20 Myr; steep walls against cratonic blocks | Clark & Royden, Royden et al., Bai et al.; L/M |
| Andes-like pulses | Repeated removal at the same place ~10–15 Myr apart; surface "stuck low" while the root is attached | Garzione et al. 2017; M |
| Collapse after convergence stops | Hot: 50–60 → 30–35 km crust in ~20–30 Myr. Cold and dry: root kept, slow erosion only. | Basin and Range, Leech 2001; M |
| "Sinking" mode | Wet mafic thick crust with an attached root stands ~0.5–2 km below Airy until detachment; no permanent sub-sea-level plateau | Garzione et al. 2017, Chen 2021; M |

### Which numbers are solid and which are guesses

- **Well constrained (H):**
  - Present crust thickness and elevation.
  - Airy coefficients.
  - Normal faulting concentrated above ~5 km (Tibet) and ~3.5–4 km (Andes).
  - GPS rates (~20 mm/yr E–W stretching; 10–14 mm/yr N–S shortening).
  - Collision onset (~59–50 Ma, debate remains).
  - Conductive regrowth timescale (~40–100 Myr).
- **Medium (M):**
  - Uplift from keel removal (1–2.5 km).
  - Density contrasts (eclogite +50–250 kg/m³ over mantle; ~7–15% over granulite).
  - ~30% eclogitization of Indian lower crust.
  - Underthrust distance (~150–300 km for crust).
  - Channel viscosities of 10¹⁸–10¹⁹ Pa·s for weak margins.
  - Delamination delay of 10–40 Myr.
- **Guesses (L):**
  - Kinetic constants k_dry and k_wet.
  - Water index values.
  - Delamination hazard λ and R_crit.
  - Patch radius.
  - Heat-flow pulse size.
  - Channel-thickness proxy (H_c − 45 km).
  - Underthrust split fractions.
  - Ceiling relaxation times.

### Gaps and caveats
- Abstracts only (not read in full): Hacker et al. 2000, Molnar & Lyon-Caen 1988 (numerical strength bound), Kay & Kay 1993 and Kapp & DeCelles 2019. The eclogite kinetic constants are calibrated to one data point (Shi et al. 2018: ~30% after ~20–30 Myr of underthrusting) and should be treated as tunable.
- The model has no temperature field. Moho temperature is proxied by thickness, time since thickening, and time since root removal. A cheap `mohoTemp` field would improve R2, R5 and R6 if one is added later.
- Slab break-off of the oceanic slab after collision (Ding et al. 2022 list it as an uplift driver) was not researched here. It could be a one-time ~10–20 Myr post-collision event similar to R5b.
- Ore link, from background knowledge and not verified in this pass: Miocene post-collisional adakitic magmas in southern Tibet host a major porphyry Cu–Mo belt. R5e's "adakitic" output could feed the ore-deposit rules in `../Ore deposits and TFC compatibility/`.
