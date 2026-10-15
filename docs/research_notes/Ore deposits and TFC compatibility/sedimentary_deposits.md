# Sediment-hosted, chemical-sedimentary, evaporite and placer ore/mineral deposits: geology catalog and simulation rules (for a TFC-compatible plate-tectonic worldgen mod)

Notes compiled Oct 2026. Method: web search plus fetches of USGS deposit models, abstracts and reviews. Several key PDFs could not be fetched: Bekker et al. 2010 (socket error), the USGS SIR 2010-5070 chapters, and Cao et al. 2018 (paywalled). For those papers, the abstract-level and search-summary facts are cited, and the gaps are flagged.

Conventions used in "Inferences / simulation rule" blocks. These field names are proposals that map onto the simulated history described in the brief:
- `t` (Ma); `lat` (paleolatitude); `env` (depositional environment class); `depth` (water depth); `aridity` (evaporation minus precipitation); `upwelling` (coastal or equatorial upwelling index); `oceanState` (global: `ANOXIC_FERRUGINOUS` before about 2.4 Ga; `STRATIFIED` about 2.4–0.8 Ga, meaning an oxic surface, a ferruginous deep ocean and euxinic margins; `OXIC` after about 0.6 Ga, with `OAE` pulses); `atmO2` (`PRE_GOE`, `POST_GOE_LOW`, `POST_NOE`); `subsidence`; `basinType` (rift, sag, passive margin, foreland, back-arc, pull-apart); `restricted` (hydrographic isolation, 0–1); `sedFlux` (clastic input); `srcComp[]` (upstream source-terrain fractions, e.g. granite_Sn, Au_lode, kimberlite, highgrade_meta, ultramafic, BIF, felsic_volcanic); `streamPower`, `knickpoint`, `slopeBreak`; `seaLevel` and `dSeaLevel`; `ashFlux`; `lipFlux` (LIP/plume hydrothermal input); `glacial`; `landPlants` (after about 0.42 Ga); `burialTmax`; `exhumed` (preservation and erosion flag); column flags such as `hasRedBeds`, `hasEvaporite`, `hasBlackShale`, `hasCarbonatePlatform`, `unconformity`.

---

## Q0. Which global "gates" (oxygen, ocean redox, seawater chemistry, climate belts, biology) control sedimentary deposit types through time?

### Takeaway
Most sedimentary deposit types are switched on or off by a small number of global state variables. These are the atmosphere and ocean redox state (GOE at about 2.4–2.3 Ga, a second oxygenation in the late Neoproterozoic), climate belts (evaporites sit in a bimodal subtropical arid belt under the Hadley-cell descending limbs) and secular seawater chemistry (Mg/Ca and sulfate, which control calcite versus aragonite seas and which potash salts form). Biology adds further gates: sulfate-reducing bacteria, land plants as a source of organic reductant, and diatoms. The simulator therefore needs a global timeline object that every deposit rule queries.

### Cited Findings
- The conventional model of oxygenation is a rise in "two large steps": the Great Oxidation Event (GOE) at about 2.3–2.4 Ga and another near the end of the Neoproterozoic. Lyons et al. argue that newer data complicate this two-step paradigm. — [Lyons et al. EGU2014 abstract](https://opac.geologie.ac.at/ais312/dokumente/EGU/EGU2014-9595.pdf); [Lyons, Reinhard & Planavsky 2014, Nature 506:307–315](https://ideas.repec.org/a/nat/nature/v506y2014i7488d10.1038_nature13068.html)
- Planavsky and colleagues (2014, Science) argue for low mid-Proterozoic atmospheric O2 levels and a delayed rise of animals. — [Lyons 2014 search context / NAI](https://astrobiology.nasa.gov/nai/directory/lyons-timothy/index.html)
- Iron formations generally disappeared at about 1.85 Ga. They reappeared in the late Neoproterozoic, linked to magmatism and Snowball Earth glaciations. In the Phanerozoic, marine iron deposition was limited to local closed or semi-closed basins with strong volcanic and hydrothermal input. — [Bekker et al. 2010, Econ. Geol. 105:467–508 (Curtin record)](https://espace.curtin.edu.au/handle/20.500.11937/18243); [PDF](https://people.earth.yale.edu/sites/default/files/files/Planavsky/8_%20Bekker%20et%20al%20Econ%20Geo%202010.pdf)
- CD (SEDEX) Zn-Pb deposits were the main sediment-hosted Pb-Zn repository between the GOE and the late-Neoproterozoic second oxidation. MVT deposits became abundant only after that second oxidation. The earliest deposits of both types are about 2.02 Ga old. — [Leach et al. 2010 (USGS pub page)](https://pubs.usgs.gov/publication/70230192)
- Leach et al. attribute the 1.85–1.58 Ga CD peak to several factors: sulfide oxidation supplying sulfate and metals, strong ocean redox gradients, the first significant sulfate evaporites, red beds, the evolution of sulfate-reducing bacteria, and large long-lived cratonic basins. — [USGS](https://pubs.usgs.gov/publication/70230192)
- Evaporite basins form in subtropical arid zones beneath the descending limbs of the Hadley cells. Proterozoic evaporite basins lie about 6° closer to the equator than the mean for the last 250 Ma. Between 550 and 370 Ma the mean evaporite paleolatitude was about 9° lower than in the Mesozoic–Cenozoic. The data argue against a high-obliquity (>54°) Proterozoic Earth (Evans 2006, Nature 444:51–55). — [Snowball Earth news summary of Evans 2006](https://snowballearth.org/news.html)
- Over the last ~400 Ma the evaporite paleolatitude distribution is "clearly bimodal" (one belt in each hemisphere). The exceptions are the Early Devonian, Early Carboniferous, earliest Permian and Middle–Late Jurassic. Coals shifted strongly poleward at the start of the Permian. — [Cao et al. 2018/2019, Geol. Mag. 156(2)](https://www.cambridge.org/core/services/aop-cambridge-core/content/view/3DB4F8A0E6B424CC9DB3B6166B4813DD/S0016756818000110a.pdf/div-class-title-palaeolatitudinal-distribution-of-lithologic-indicators-of-climate-in-a-palaeogeographic-framework-div.pdf); [Univ. Sydney record](https://ses.library.usyd.edu.au/handle/2123/19947)
- Hardie (1996) proposed that coupled shifts in marine limestone mineralogy (calcite versus aragonite seas) and in potash evaporite mineralogy (MgSO4-rich versus MgSO4-poor) over the last 600 Ma share one cause: changes in seawater chemistry driven by mid-ocean-ridge hydrothermal flux. — [Hardie 1996 cited in Baztech article](https://yadda.icm.edu.pl/baztech/element/bwmeta1.element.baztech-article-BUS6-0035-0099); [Hardie 2003 Precambrian extension](https://repository.geologyscience.ru/handle/123456789/34119?show=full)
- The Hardie model is contested. Holland et al. (1996) favored dolomitization during evaporation as the explanation and noted that seawater K+ has been nearly constant. A USGS fluid-inclusion study of Silurian seawater supports oscillations driven by ridge and river fluxes. — [Holland et al. 1996](https://acnpsearch.tweb-dev.unibo.it/singlejournalindex/163995); [USGS Silurian seawater](https://www.usgs.gov/publications/major-ion-composition-silurian-seawater)
- Warren argues that greenhouse periods, with their greater marine freeboard, favor marine platform mega-sulfate deposits. Modern marine-fed evaporites are limited by icehouse eustasy and by a lack of hydrographically isolated subsea-level depressions. — [Warren 2012 AAPG abstract](https://www.searchanddiscovery.com/abstracts/html/2012/90141geo/abstracts/warren.htm)
- Sandstone-type U deposits depend on organic reductant. Moore et al. (2024) find that sedimentary U4+ minerals appear later in the Phanerozoic than U6+ minerals because enough sedimentary organic matter had to accumulate first. — [Carnegie summary of Moore et al. 2024, G-cubed](https://carnegiescience.edu/uranium-redox-and-deposition-transitions-embedded-deep-time-geochemical-models-and-mineral); [USGS primer on organic matter and sandstone U](https://www.usgs.gov/publications/organic-matter-and-sandstone-type-uranium-deposits-a-primer)
- Detrital uraninite and pyrite are unstable under oxic conditions. Pre-2.2 Ga detrital placers have therefore been used as "evidence" for a reduced early atmosphere. Ohmoto disputes this; he argues the atmosphere was already oxic by 3.0 Ga. — [Nedachi, Yamanouchi & Ohmoto, Mineral. Mag. 62A](https://rruff.info/doclib/MinMag/Volume_62A/62a-2-1060.pdf)

### Inferences
- Proposed global timeline object (`WorldHistory.globalState(t)`), randomized per world but with defaults pinned to Earth:
  - `atmO2`: `PRE_GOE` for t > ~2.40 Ga (randomize 2.45–2.25). `POST_GOE_LOW` until ~0.80–0.55 Ga. `POST_NOE` after that.
  - `oceanState`: before the GOE, `ANOXIC_FERRUGINOUS` (BIF-permissive everywhere deep). Between the GOE and ~1.85 Ga, `FERRUGINOUS_DEEP` with BIF/GIF pulses tied to LIPs. From ~1.85 to ~0.8 Ga, `STRATIFIED_EUXINIC_MARGINS` (CD Zn-Pb favoured, BIF absent). During Cryogenian glaciations, `FERRUGINOUS_REVIVAL` (Rapitan-type BIF in rifts). After ~0.6 Ga, `OXIC` with short `OAE` episodes (ironstones, black shales, Mn pulses).
  - `seaChem`: a toggle between `ARAGONITE_MgSO4` and `CALCITE_KCl` driven by the simulated global ridge-production rate. A high spreading rate gives calcite seas and KCl-type potash (Hardie). Flag this as contested and give it a low weight, or randomize it.
  - `bio`: `sulfateReducers` (after ~2.4 Ga, needed for MVT/CD/sed-Cu sulfide), `landPlants` (after ~0.42 Ga; terrestrial organic reductant for roll-front U, coal, plant-debris-hosted red-bed Cu), `diatoms` (needed for diatomite; see Q17 gap), `calcareousPlankton` (chalk).
  - `climateBelts(lat, t)`: the evaporite belt is a bimodal subtropical band. Shift it about 6° equatorward in the Proterozoic and about 9° equatorward for 550–370 Ma, following Evans 2006.
- Preservation is a first-order filter, and Leach et al. stress that the observed record is biased by recycling. The simulator should accumulate deposits through time and then destroy them where the stratigraphic column is eroded, subducted or metamorphosed past mineral stability limits. The resulting age histogram of surviving deposits will then look like Earth's without being forced to.

### Gaps
- No quantitative O2 curve (pO2 versus time) was retrieved. Lyons 2014's figures would supply one. Background values, not verified this session: about 10^-5 PAL before the GOE, a possible overshoot during the Lomagundi excursion (about 2.22–2.06 Ga), mid-Proterozoic values of about 0.1–1% PAL, and near-modern values after about 0.54–0.42 Ga.
- No numeric latitude band (degrees) for evaporites was obtained from Cao et al. 2018 (paywalled). The commonly quoted figure of about 10–35° (and the user's 15–35°) was not confirmed this session.
- Timing of aragonite and calcite seas: Hardie's Phanerozoic intervals were not retrieved. Background, not verified: aragonite seas in the late Neoproterozoic to Early Cambrian, the Late Carboniferous to Triassic, and the Neogene; calcite seas in the Middle Cambrian to Early Carboniferous and the Jurassic to Paleogene.

---

## Q1. Banded iron formations (Algoma, Superior, Rapitan/Neoproterozoic): settings, timing and simulation

### Takeaway
Iron formation (IF) needs a ferruginous (Fe2+-rich, anoxic) deep ocean, a hydrothermal or mantle-plume Fe source, and a low clastic input. Algoma-type IF is small and volcanic-associated (greenstone belts and arcs). Superior-type IF is giant, sits on passive margins and platforms, starts at about 2.6 Ga, peaks between 2.6 and 2.4 Ga, has a last pulse at about 1.88 Ga, and is absent after about 1.85 Ga. Rapitan-type IF returns in actively rifting basins during Cryogenian Snowball glaciations (Sturtian, about 717–660 Ma). Phanerozoic marine iron formation is restricted to local volcanic and hydrothermal settings.

### Cited Findings
- **Algoma-type** IF is interlayered with or stratigraphically linked to submarine volcanic rocks in greenstone belts, and sometimes occurs with VMS deposits. **Superior-type** IF is developed in passive-margin sedimentary successions and generally lacks direct volcanic links. A full gradation between the two types exists, and the distinction remains a valid first approximation. — [Bekker et al. 2010](https://espace.curtin.edu.au/handle/20.500.11937/18243); [Egyptian Algoma BIF paper citing Bekker, Frontiers 2024](https://www.frontiersin.org/journals/earth-science/articles/10.3389/feart.2024.1359617/pdf)
- Texture changes through time. Banded IF (BIF) dominates the Archean to earliest Paleoproterozoic, and granular IF (GIF, a shallow-water, reworked facies) is much more common in Paleoproterozoic successions. GIF dominated after the GOE (about 2.4 Ga). — [Bekker et al. 2010](https://espace.curtin.edu.au/handle/20.500.11937/18243)
- Giant IF deposition was episodic and coeval with, and genetically linked to, large igneous province (LIP) emplacement. Superior-type IF first appeared at about 2.6 Ga, which the authors link to continent construction changing core–mantle heat flux. Between about 2.6 and 2.4 Ga, global mafic magmatism culminated in giant Superior-type BIF in South Africa, Australia, Brazil, Russia and Ukraine. — [Bekker et al. 2010](https://espace.curtin.edu.au/handle/20.500.11937/18243)
- Bekker et al. argue that no single parameter controlled IF deposition; it reflects interplay among mantle, tectonic, oceanic and biospheric processes. — [Bekker et al. 2010 Curtin record](https://espace.curtin.edu.au/handle/20.500.11937/9434)
- Hamersley example (commercial summaries, need verification): the Brockman Iron Formation is about 620 m thick within the Hamersley Group (about 2.63–2.45 Ga, about 2.5 km of sedimentary and volcanic rock over about 80,000 km² of the Pilbara). It was deposited on a large, stable, clastic-starved platform with episodic fine tuff input. — [Mining Data Online: Greater Tom Price](https://miningdataonline.com/property/854/Greater-Tom-Price-Mine.aspx). The Brockman is "among the largest known BIFs", dated at about 2.46 Ga. — [Cambridge Elements excerpt](https://assets.cambridge.org/97811089/95290/excerpt/9781108995290_excerpt.pdf)
- Controversy: Courtney-Davies et al. (2024, PNAS), as reported secondhand, suggest that some Hamersley iron *ore* may be as young as about 1.4 Ga. This refers to ore upgrading, not BIF deposition. — [Substack summary (secondary)](https://richardigibson.substack.com/p/tiger-iron)
- **Rapitan (Cryogenian):** the onset of the Sturtian glaciation in NW Laurentia is constrained by a 717.4 ± 0.1 Ma CA-ID-TIMS zircon date on the underlying Mount Harper volcanics. A postglacial Re-Os age of 662.4 ± 3.9 Ma implies a glacial epoch of about 55 Myr. — [Rooney et al. 2014, PNAS (PMC)](https://pmc.ncbi.nlm.nih.gov/articles/PMC3890860)
- Rapitan IF is younger than about 716 Ma. Low Re and Re/Mo through most of the IF indicate ferruginous to oxic basin waters. High values in the uppermost jasper indicate that IF deposition was terminated by a shift to a sulfidic (euxinic) water column. — [eScholarship Re/Mo study](https://escholarship.org/content/qt37w1m0bm/qt37w1m0bm.pdf)
- "Differential subsidence in actively rifting basins was essential for deposition of Rapitan-type iron formation", whereas glaciation, though coeval, "was not a strong control". An alternative model (Baldwin et al.) has a partly restricted basin with Fe supplied by glacial meltwater and cycled through a stratified, ice-covered water column. — [Laurentian Univ. publication: tectonic controls on Rapitan IF](https://hes.laurentian.ca/publications/tectonic-controls-distribution-and-stratigraphy-the-cryogenian-rapitan-iron-formation-0)
- The Rapitan Group (about 711 Ma) and the Mount Harper Group (about 717 Ma) differ in glacial onset by about 6 Myr, which argues for a diachronous Sturtian. Note that the exact record behind this 2016 Precambrian Research item was not verified. — [Laurentian MERC record](https://merc.laurentian.ca/node/1211)

### Inferences
**Commodities and minerals:** Fe as hematite and magnetite (with siderite, Fe-silicates and chert); ore is supergene-enriched hematite or goethite. **Abundance:** IF-derived ore is the dominant global iron source today. The USGS ironstone model notes that Precambrian BIF displaced Phanerozoic ironstones after 1945 ([USGS ironstone model](https://pubs.usgs.gov/bul/b2004/html/bull2004oolitic_ironstones.htm)).

**Proposed simulation rule:**
- Gate: `oceanState ∈ {ANOXIC_FERRUGINOUS, FERRUGINOUS_DEEP, FERRUGINOUS_REVIVAL}` and the cell is marine with `sedFlux` low (clastic-starved).
- Algoma: `env` = deep marine near submarine volcanics (arc, back-arc, oceanic plateau or rift volcanic cells; `lipFlux` or `ridgeProximity` high), for t > 1.85 Ga (most common 3.8–2.6 Ga). Small lenses, tens of metres thick and kilometre-scale extent, interbedded with volcanics. Base probability per eligible cell is moderate, but bodies are small. Option: co-locate with VMS rolls from the volcanic-deposit module.
- Superior: passive-margin or intracratonic platform shelf to slope below storm wave base. Requires `lipFlux_global(t)` above threshold (a global LIP pulse) within ±20 Myr, t between 2.65 and 1.85 Ga, a low clastic flux and a moderate subsidence rate. The probability peaks at 2.6–2.4 Ga with a secondary peak at about 1.88 Ga. Large sheets (hundreds of metres thick, basin-wide, 10^4–10^5 km² for giants). Facies rule: deeper water gives laminated BIF (hematite/magnetite-chert); shallow water after the GOE gives GIF (granular, more hematite).
- Rapitan: t in a Cryogenian glaciation window (simulated global glaciation flag; on Earth about 717–660 Ma and about 650–635 Ma), `basinType = rift` with high differential subsidence, glaciomarine diamictite in the column, and restricted connection. Smaller (tens of metres) jasper/hematite IF interbedded with diamictite. End the deposit when the basin becomes euxinic.
- Post-deposition: if `burialTmax` passes the greenschist window, convert hematite to magnetite ("taconite"). If the IF is later exhumed under warm, humid weathering (and/or along faults), spawn high-grade supergene hematite or goethite orebodies in the upper 100–300 m (see the weathering researcher's notes).
- **Block mapping:** TFC `hematite` (unmetamorphosed or supergene IF, and the jasper of Rapitan IF), TFC `magnetite` (metamorphosed IF, taconite), TFC `limonite` (goethitic weathering cap); vanilla `iron_ore`. Because IF is laterally continuous, giant Superior-type IF is an ideal fit for TFC's "very large flat" vein shape, with hematite/magnetite grades by facies. Optional decorative jaspilite band: none in TFC, but red chert or jasper could be texture-coloured chert.

### Gaps
- Exact thickness and lateral statistics for Algoma versus Superior IF were not obtained (Bekker PDF fetch failed). Background, not verified: Algoma IF is typically less than 50–100 m thick and less than 10 km in extent; Superior IF is 100–1000 m thick and extends 10^2–10^3 km; IF contains about 20–40% Fe; enriched hematite ore contains about 55–68% Fe.
- The age of the earliest IF (Isua, about 3.8 Ga) and the peak ages (about 2.7, 2.5–2.45 and 1.88 Ga) were not confirmed against Bekker 2010 this session.
- Marinoan-age (about 635 Ma) IF examples (e.g. Urucum, Brazil; Chuos/Jacadigo) were not researched. The Vilela et al. PDF on Brazilian Neoproterozoic IF was not fetched.

---

## Q2. Phanerozoic oolitic ironstones (Minette, Clinton)

### Takeaway
Oolitic ironstones are Fe-silicate and oxide ooid beds, about 2–5 m thick and 2–10 km across. They form on shallow, tide-influenced shelves at the top of shoaling-upward cycles that begin with black shale, during times of high sea level and continental dispersal (Ordovician–Devonian and Jurassic–Paleogene). They require deep weathering of the hinterland as an iron source and are linked to poorly ventilated epicontinental seas.

### Cited Findings
- USGS descriptive model (Maynard & Van Houten): synonyms are Clinton-type and Minette-type. The setting is a shallow shelf near the nonmarine–marine transition, with bipolar cross-stratification (intertidal) in the uppermost beds. Ages are mainly Ordovician–Devonian and Jurassic–Paleogene, with a few Proterozoic examples. Tectonic settings: craton margins about 40%, craton interiors 25%, foreland basins 20%, exotic terranes 15%. — [USGS Bull. 2004 oolitic ironstone model](https://pubs.usgs.gov/bul/b2004/html/bull2004oolitic_ironstones.htm)
- The ironstone sits at the top of coarsening- and shoaling-upward cycles from a few metres to about 300 m thick (black shale, then grey shale and siltstone, then tempestite sandstone, then ironstone). About 75% of well-developed Phanerozoic ironstones have an extensive black shale at the base of the cycle. About three-quarters are strongly controlled by position at the top of a cycle, and many large deposits show tidal influence. Bodies are tabular, about 2–5 m thick and 2–10 km across. — [USGS model](https://pubs.usgs.gov/bul/b2004/html/bull2004oolitic_ironstones.htm)
- Mineralogy: younger rocks contain goethite and berthierine (7 Å); older rocks contain hematite and chamosite (14 Å). Siderite and pyrite are replacements, with minor apatite. Goethite converts to hematite above about 80 °C, and berthierine converts to chamosite at about 130–160 °C. Ironstones were a major iron source from 1850 to 1945. — [USGS model](https://pubs.usgs.gov/bul/b2004/html/bull2004oolitic_ironstones.htm)
- Minettes contain goethite and Clintons contain hematite. The Clinton type (Appalachians, Newfoundland to Alabama) is several hundred Myr older, and "the major difference... is probably age". — [Alex Strekeisen ironstone page](https://alexstrekeisen.it/english/sedi/ironstone.php)
- Most ironstones accumulated during two principal episodes (150–170 Myr each) of craton dispersal and relatively high sea level, and are especially numerous in the Ordovician and Jurassic. Under persistently favourable conditions (tectonic stability, deep weathering), ironstone accumulation recurred every few to several Myr in 10 major basins. — [Van Houten 1986 AAPG abstract](https://www.searchanddiscovery.com/abstracts/html/1986/annual/abstracts/0659.htm)
- Van Houten & Arthur (1989): ironstone episodes coincide with extensive epicontinental seas and "ineffective oxygenation of deep-water masses", and follow transgression–regression sequence patterns. — [Penn State record](https://pure.psu.edu/en/publications/temporal-patterns-among-phanerozoic-oolitic-ironstones-and-oceani/)
- The genesis of ironstones remains poorly understood and controversial. — [Alex Strekeisen](https://alexstrekeisen.it/english/sedi/ironstone.php)

### Inferences
**Proposed simulation rule:**
- Gate: `atmO2 = POST_NOE` (Phanerozoic) and global `seaLevel` high (dispersal phase of the supercontinent cycle), with enhanced probability during `OAE`/poorly ventilated windows.
- Cell: `env ∈ {inner shelf, tidal shelf, deltaic front}` in an epicontinental sea or on a craton margin, at the top of a simulated shoaling-upward parasequence (deepening, then shallowing). This is detected as a transition from `hasBlackShale` toward a shallow facies within one sea-level cycle.
- Source: upstream catchment with deep chemical weathering (humid tropical `lat`, low relief and long exposure, i.e. a laterite-forming hinterland) and low clastic flux.
- Output: one to several 2–5 m beds of 2–10 km lateral extent, recurring every few Myr while conditions persist.
- Diagenesis: if `burialTmax` > 80 °C, goethite becomes hematite (Clinton-like); otherwise goethite plus berthierine (Minette-like).
- **Block mapping:** TFC `limonite` (goethitic Minette-type, unburied) or TFC `hematite` (buried >80 °C, Clinton-type); vanilla `iron_ore`. Siderite replacement could appear as `limonite` as well.

### Gaps
- Typical grade was not retrieved. Background, not verified: about 25–40% Fe, high P (about 0.5–1.8%); Lorraine Minette is about 30–35% Fe.
- No quantitative link to greenhouse climate beyond the sea-level and anoxia association was found.

---

## Q3. Sedimentary manganese (Kalahari, Groote Eylandt and others)

### Takeaway
Giant sedimentary Mn deposits precipitate at redox boundaries where Mn2+-bearing anoxic or suboxic deep water meets oxygenated shallow water. They therefore cluster at oxygenation transitions: the Kalahari Mn field at about 2.4–2.2 Ga within the Hotazel BIF (GOE), and Phanerozoic pulses at transgressions and anoxic events, such as Cretaceous (Cenomanian) Groote Eylandt. The Kalahari field alone holds about 70–80% of the world's land-based Mn resources.

### Cited Findings
- The Kalahari Manganese Field (KMF) is hosted in the Hotazel Iron Formation (Postmasburg Group) and is interbedded with BIF. A diabase sill in the lower Hotazel is dated at 2,422 (+16/−7) Ma, so the Hotazel is probably about 2.4 Ga. Other reviews give about 2.2 Ga, so the age is disputed. — [Wits thesis](https://wiredspace.wits.ac.za/bitstreams/eb0f7923-49b8-4523-8bc8-e4cd3bd93542/download); [Manganese Deposits of Africa review](https://www.literature.wolkersdorfer.info/literature/Manganese%20Deposits%20of%20Africa.pdf)
- A Rhodes University abstract ties the KMF to the Great Oxidation Event (about 2.4 Ga). — [Rhodes repository](https://researchrepository.ru.ac.za/handle/123456789/3915)
- KMF resources: about 4,200 Mt of Mn metal, about 77% of the world's land-based resource ([Africa Mn review](https://www.literature.wolkersdorfer.info/literature/Manganese%20Deposits%20of%20Africa.pdf)). Another estimate is about 5 Gt of ore, about 70% of the global resource ([Wits](https://wiredspace.wits.ac.za/bitstreams/eb0f7923-49b8-4523-8bc8-e4cd3bd93542/download)). The Rhodes estimate is about 8 Gt ore, about half of the world's resources ([Rhodes](https://researchrepository.ru.ac.za/handle/123456789/3915)).
- Ore types (KMF): primary low-grade Mamatwan-type braunite–kutnahorite ore at 30–38 wt% Mn makes up about 97% of the resource. Hydrothermally upgraded Wessels-type ore carries 42–60% Mn, and supergene ore 40–42% Mn. — [search synthesis of KMF sources; see also Ntsimbintle KMF page](https://ntsimbintle.co.za/our-business/the-kalahari-manganese-field)
- Groote Eylandt (Australia): Cenomanian shallow-marine pisolitic Mn oxides in claystones and sandstones pass into deeper-water Ca-Mn carbonates in glauconitic sandy clay or marl. — [Ostwald 1987 via Schweizerbart](https://www.schweizerbart.de//papers/njma/detail/157/109380/Manganese_carbonates_in_the_Groote_Eylandt_sedimentary_manganese_deposit)
- Groote Eylandt geometry: an almost continuous horizon of about 50 km² up to 11 m thick, with massive, pisolitic and oolitic oxides formed as a chemical precipitate on a wave-affected shallow sea floor ([Mining Data Online GEMCO](https://www.miningdataonline.com/property/337/GEMCO-Mine.aspx)). Alternatively about 150 km², in the upper 15 m of the host beds ([NSW manganese review](https://www.resources.nsw.gov.au/sites/default/files/2022-11/manganese.pdf)). A Proterozoic paleo-coastline provided an embayment. The Cretaceous paleoshoreline is the exploration target. — [NT Manganese factsheet](https://resourcingtheterritory.nt.gov.au/media/docs/commodity-factsheets/NT_Manganese_factsheet.pdf)
- GEMCO resources: about 115 Mt at 43.6% Mn (ROM) plus about 11 Mt at 19.8% Mn (sands), in the 2026 NT factsheet. — [NT factsheet](https://resourcingtheterritory.nt.gov.au/media/docs/commodity-factsheets/NT_Manganese_factsheet.pdf)
- Maynard (2010, Econ. Geol. 105:535–552) argues that Mn ore chemistry through time signals increasing diversity of Earth-surface environments. Sedimentary Mn ore is a chemical precipitate under suboxic marine redox conditions. — [Mindat ref](https://zh.mindat.org/reference.php?id=225574)

### Inferences
**Proposed simulation rule:**
- Gate A (Precambrian): `oceanState` transitioning (GOE window, about 2.45–2.2 Ga) and a Superior-type IF basin already active. Place a Mn-rich interval *within* the IF stack at a shallowing (regressive) step. These deposits are rare but giant (one or two per world), and the layers are metres to tens of metres thick over 10^2–10^3 km².
- Gate B (Phanerozoic): an `OAE` pulse or rapid transgression, with an oxygen-minimum zone impinging on a shallow shelf. The cell is a shallow shelf or embayment margin around a basin that has an anoxic deep. Place a thin (1–15 m) oxide–pisolite bed at the paleoshoreline facing the anoxic basin, grading downdip into Mn-carbonate.
- Output pattern: a shoreline-parallel belt tens of km long that follows a transgressive surface.
- **Block mapping:** no Mn ore exists in TFC or vanilla, so "no mapping" (or a custom `pyrolusite`/`braunite` block if the mod adds metals). In-game fallback: treat as `limonite`-like Fe-Mn ore, or leave out.

### Gaps
- Other important Mn deposits were not researched: Nikopol and Chiatura (Oligocene, Paratethys), Molango (Jurassic), Urucum (Neoproterozoic, with IF), Moanda (Paleoproterozoic). A Phanerozoic Mn-epoch timeline (Maynard & Kuleshov 2016 book) was not retrieved.
- The Groote Eylandt–OAE2 link (about 94 Ma) is plausible but not confirmed by any retrieved source.

---

## Q4. Clastic-dominated (CD, formerly SEDEX) Zn-Pb-Ag

### Takeaway
CD Zn-Pb deposits form from oxidized, saline basinal brines (evaporite-derived) that vent into, or replace, reduced (pyritic, organic-rich) fine-grained sediments in active rift sub-basins or on passive margins. They first appear at about 2.02 Ga, peak at 1.85–1.58 Ga (Australia's Carpentaria basins), and continue into the Phanerozoic, notably the Paleozoic passive margins. They are the most important source of sediment-hosted Zn-Pb, and individual giants exceed 100 Mt at about 10% Zn+Pb.

### Cited Findings
- CD deposits are hosted by shale, sandstone, siltstone or mixed clastic rocks, and the class includes what were traditionally called SEDEX. — [USGS summary of Leach 2010](https://www.usgs.gov/publications/sediment-hosted-lead-zinc-deposits-earth-history)
- Timing: earliest at about 2.02 Ga; dominant between the GOE and the late-Neoproterozoic oxidation; abundant at 1.85–1.58 Ga. Factors: sulfate supply, redox gradients, the first sulfate evaporites, red beds, sulfate-reducing bacteria, and large long-lived cratonic basins. CD mineralization also formed along passive margins in Pangea's evaporite belts. — [Leach et al. 2010 USGS page](https://pubs.usgs.gov/publication/70230192); [USGS summary](https://www.usgs.gov/publications/sediment-hosted-lead-zinc-deposits-earth-history)
- The 2005 USGS global compilation found that SEDEX ages fall into a Proterozoic group and a Phanerozoic group. — [USGS 2005](https://www.usgs.gov/publications/sediment-hosted-lead-zinc-deposits-a-global-perspective)
- The most important source of sediment-hosted Zn and Pb is the CD class. A 2026 USGS data release updates the Singer et al. (2009) and Taylor et al. (2009) grade–tonnage compilations. — [USGS data release](https://www.usgs.gov/data/grade-and-tonnage-data-clastic-dominated-and-mississippi-valley-type-zinc-lead-silver-deposits)
- HYC (McArthur River, Australia): about 1.5 km × 1.0 km × 55 m thick, 227 Mt at 9.2% Zn, 4.1% Pb and 41 g/t Ag. Host: pyritic bituminous shale with sedimentary breccia and tuff (H.Y.C. member, Barney Creek Fm) at the base of the member in a sub-basin. — [AusIMM 1984 McArthur River Project abstract](https://www.ausimm.com/publications/conference-proceedings/annual-conference-darwin/the-mcarthur-river-project/)
- A broad Zn-Pb-Tl halo extends along the pyritic black-shale facies of the Barney Creek Fm. — [Large et al. lithogeochemical halos](https://eclass.upatras.gr/modules/document/file.php/GEO389/Large%20et%20al%20ithogeochemical_halos.pdf)
- Controversy: whether ore is syngenetic (exhalative, water column) or diagenetic replacement is still contentious, and early pyrite has a wide δ34S range. One thesis interprets the HYC host as *saline lacustrine* rather than a euxinic marine basin. — [Springer chapter on CD genesis](https://link.springer.com/chapter/10.1007/978-3-031-27897-6_11)
- Fluid inclusion Cl/Br and Na/Br ratios indicate that SEDEX and MVT ore fluids are seawater evaporated past halite saturation (bittern brines). — [Emsbo GSA 2009](https://gsa.confex.com/gsa/2009AM/webprogram/Paper165492.html)
- Paleolatitude reconstructions place many sediment-hosted Zn-Pb deposits at low to mid latitudes, with shifts through time possibly related to glaciations (preliminary). — [Armistead & Williams EGU26](https://meetingorganizer.copernicus.org/EGU26/EGU26-16017.html)

### Inferences
**Proposed simulation rule:**
- Gate: `atmO2 ≥ POST_GOE_LOW` (t < 2.05 Ga), `sulfateReducers = true`, with peak weighting at 1.85–1.58 Ga and secondary weighting in the Paleozoic (Cambrian–Devonian passive margins).
- Basin: `basinType ∈ {intracontinental rift, sag over rift, passive margin}` with active synsedimentary growth faults (high differential `subsidence`, creating third-order sub-basins). The column contains `hasRedBeds` and/or `hasEvaporite` (brine and metal source) at depth, plus a reduced fine-grained unit (`hasBlackShale`, pyritic) at the level of the vent or replacement.
- Climate: the evaporite generation stage (the basin passed through the arid belt; `lat` within the subtropical band at some point during fill) supplies the brine.
- Location: within a sub-basin (depocentre) adjacent to a basin-bounding fault (fluid conduit). Deposits are stratiform lenses 10–60 m thick, 1–3 km across, stacked or clustered within a district of about 10–50 km along the fault. A wider Zn-Pb-Tl/Mn halo (kilometres) surrounds each lens in the same horizon.
- Size: lognormal, median tens of Mt, about 5–10% Zn and 2–5% Pb (calibrate to the USGS data release), with rare giants of 100–200 Mt.
- **Block mapping:** TFC `sphalerite` (Zn). Galena has no TFC or vanilla block, so "no mapping" (TFC has no lead). Ag-bearing sulfosalts could be TFC `tetrahedrite` (a real Ag-Cu-Sb sulfosalt) in feeder zones. TFC `pyrite` (gem) is very abundant in host shales. The barite cap has no mapping.

### Gaps
- Red Dog (Alaska, Carboniferous, the world's largest Zn mine) and the Selwyn/Howards Pass, Rammelsberg/Meggen, Broken Hill and Rampura-Agucha grades were not retrieved. A median grade–tonnage from the 2026 USGS release was not extracted.
- No reliable statistic for latitude within ±30° was found (Taylor et al. 2009 OFR appendix would have it).

---

## Q5. Mississippi Valley-type (MVT) Zn-Pb

### Takeaway
MVT deposits are epigenetic Zn-Pb sulfide replacements and open-space fills in platform carbonates (usually dolostones on passive-margin or foreland platforms). They precipitate from dense basinal brines (about 75–200 °C, about 10–30 wt% NaCl eq., derived from evaporated seawater) driven by topography-driven or orogen-driven flow during basin inversion and orogeny. They are abundant only after the late-Neoproterozoic oxygenation, peak in the Devonian–Carboniferous during Pangea assembly, and pulse again after Pangea's breakup.

### Cited Findings
- MVT deposits occur in platform carbonate sequences, usually in passive-margin settings. — [Geoscience Australia record of Leach 2010](https://ecat.ga.gov.au/geonetwork/srv/api/records/a05f7892-ef22-7506-e044-00144fdd4fa6)
- MVT deposits are mainly a Phanerozoic phenomenon. They peaked in the Devonian–Carboniferous during Pangea assembly, and a new MVT episode began after Pangea broke up as the next supercontinent started assembling. — [USGS summary](https://www.usgs.gov/publications/sediment-hosted-lead-zinc-deposits-earth-history); [USGS 2005](https://www.usgs.gov/publications/sediment-hosted-lead-zinc-deposits-a-global-perspective)
- MVT Pb-Zn ores precipitate from dense basinal brines at about 75–200 °C, typically in platform carbonate sequences. — [USGS OFR 92-704](https://pubs.usgs.gov/of/1992/0704/report.pdf)
- District examples of fluid temperature and salinity: Irish Midlands 4–28 wt% NaCl eq. (mostly 8–19), Th 70–280 °C ([Wilkinson, Imperial College](https://spiral.imperial.ac.uk/bitstream/10044/1/52218/2/Wilkinson_RoedderVolume_revised.pdf)). Sabinas (Mexico) 7–22 wt%, 75–150 °C ([Redalyc](https://www.redalyc.org/pdf/943/94316033003.pdf)). Verviers (Belgium) about 20 wt% salts, about 110 °C ([KU Leuven](https://lirias.kuleuven.be/1130755)). SE Missouri/Reelfoot rift 19 to >23 wt%, 45–140 °C ([USGS PP1577](https://pubs.usgs.gov/pp/p1577/p1577.pdf)). The mapping of these district figures to individual URLs comes from search snippets and is approximate.
- Trèves (France): deposition at about 80–150 °C from a brine of evaporated seawater past halite saturation. — [USGS Trèves](https://pubs.usgs.gov/publication/70031192)
- Wiesloch (Upper Rhine Graben), formed in an extensional setting: mixing of hot basement-derived brines with sedimentary formation waters caused precipitation, and the same mechanism is proposed for other European MVTs. — [USGS Wiesloch](https://www.usgs.gov/publications/formation-wiesloch-mississippi-valley-type-zn-pb-ag-deposit-extensional-setting-upper)
- The USGS MVT deposit model (Leach et al. 2010, SIR 2010-5070-A) is based on a global dataset of all known MVT deposits. Carbonate hosts buffer supergene weathering, giving near-neutral drainage. — [USGS SIR 2010-5070-A](https://pubs.usgs.gov/publication/sir20105070A)
- A Newcastle climate-modelling study found that sediment-hosted deposit types track climate belts: sandstone-Pb favours low-latitude arid regions, while other types favour equatorial and temperate rainfall belts. — [Newcastle Univ. thesis](https://dspace.ncl.ac.uk/jspui/handle/10443/1014?mode=full)

### Inferences
**Proposed simulation rule:**
- Gate: `atmO2 = POST_NOE` (most common at t < 0.6 Ga; rare at 2.0–0.6 Ga), `sulfateReducers`, and a column containing a carbonate platform (`hasCarbonatePlatform`, preferably dolomitized) above or adjacent to a basin with evaporite or saline brine (`hasEvaporite` anywhere in the connected basin).
- Trigger event: an orogeny or collision on the platform's far side producing a regional topographic head across a foreland basin. The simulated hydraulic gradient (topographic elevation difference / basin width) must exceed a threshold for at least several Myr. Alternative trigger: rift-related basement brine upflow (Wiesloch type).
- Location: on the cratonward (distal) flank of the foreland basin, 100–1000 km from the orogenic front, where the brine aquifer pinches out against basement highs, arches, reef margins, solution-collapse breccias or unconformities. Deposits are districts (10^2–10^4 km²) containing many small orebodies (each 0.1–10 Mt at about 3–10% Zn+Pb). The MVT event age equals the orogeny age, which can be much younger than the host carbonate.
- Temperature check: the brine source depth must satisfy `burialTmax` of 75–200 °C, but the host must not be metamorphosed later.
- **Block mapping:** TFC `sphalerite`. Galena has no mapping. Fluorite (Illinois-Kentucky subtype) has no mapping. Barite has no mapping. TFC `pyrite` (marcasite) is minor. Vanilla has no zinc ore.

### Gaps
- Median MVT grade–tonnage was not retrieved (the USGS SIR 2010-5070-A PDF was not fetched). Background, not verified: about 7 Mt median at about 6–7% combined Zn+Pb.
- Paleolatitude statistics (the often-cited ±30° and an evaporite association) were not confirmed.

---

## Q6. Sediment-hosted stratiform copper (Kupferschiefer, Central African Copperbelt) and red-bed copper

### Takeaway
Basin-scale oxidized saline brines leach Cu from red beds and/or mafic volcanics and precipitate Cu sulfides where they meet a reduced horizon (pyritic black shale, algal mat, plant debris, hydrocarbons) at or near a red-bed/reduced-bed contact. This happens in intracontinental rifts with evaporites, during diagenesis to early inversion. Supergiants occur in three basins: Paleoproterozoic Kodaro-Udokan, Neoproterozoic Katanga (Copperbelt) and Permian Central Europe (Kupferschiefer). The type supplies about 23% of world Cu and is an important source of Co and Ag.

### Cited Findings
- Sediment-hosted stratiform Cu is about 23% of world Cu production and reserves and a significant source of Co and Ag. Required components: metal and S sources, saline fluids, pathways, a thermal or hydraulic pump, and a precipitation trap. Red beds supply metals (Fe-oxyhydroxides weakly bind metals). Sulfur comes from marine or lacustrine evaporites, reduced seawater, or H2S-bearing petroleum. Fluids are low to moderate temperature and saline. They are focused by red-bed thinning at basin margins, faults, permeability contrasts, paleotopography and salt-diapir margins. Supergiants: Kodaro-Udokan (Paleoproterozoic), Katanga (Neoproterozoic), Central Europe (Permian). — [Hitzman et al. 2005 abstract (mirror)](https://isgb.otago.ac.nz/infosci/nigel.stanger/Digital_Repository/raw/306ce8fa1f6a963a67d8f587e4c2fecc30412ae0/Misc/Mass%20downloads/UTas/705.html); [UTas record](https://eprints.utas.edu.au/705/)
- Subtypes by host and reductant: reduced-facies, sandstone (Revett) and red-bed types. — [Hitzman framework summary; Iran Hamzelou paper](https://aag.scu.ac.ir/mobile/article_15292_d91be65349e562f44b5fb380efb67410.pdf?lang=en)
- USGS model 30b ("sediment-hosted Cu"; synonyms sandstone Cu, Cu-shale): stratabound disseminated Cu sulfides in reduced beds of red-bed sequences with thin carbonate and evaporite beds. Age range: Middle Proterozoic, Permian and early Mesozoic, possibly other Phanerozoic. Setting: epicontinental shallow-marine basins near the paleo-equator, including sabkhas, with high evaporation and permeable sediments. Tectonics: intracontinental rifts, aulacogens, passive margins, growth faults. Associated with halite, sylvite, gypsum and anhydrite, sandstone U, and basalt Cu. Minerals: chalcocite, bornite, chalcopyrite, pyrite, native silver, carrollite (Co). Zoning: Cu2S/bornite core, chalcopyrite rim, galena-sphalerite fringe. Ore controls: fossil wood, algal mats, biogenic S, petroleum, permeable footwall, oxidized/reduced boundaries. — [USGS Bull. 1693 model 30b](https://pubs.usgs.gov/bul/b1693/html/bull27xx.htm)
- Kupferschiefer: a black shale at the base of the marine Upper Permian Zechstein, generally less than 1 m thick (up to about 70 cm, locally absent). It overlies Rotliegend red beds and conglomerate and is overlain by Zechstein carbonate, anhydrite and salt. — [Kupferschiefer lithology/stratigraphy (ZDGG)](https://www.schweizerbart.de/papers/zdgg/detail/157/55456/The_Kupferschiefer_Lithology_stratigraphy_facies_a); [MDPI Minerals 9(10):592](https://www.mdpi.com/2075-163X/9/10/592)
- Lubin–Głogów: the ore zone ranges from 0.4 to 26 m thick and covers nearly 600 km². Reported figures include about 1.5 Gt ore at 1–3% Cu and about 50 Mt Cu (this source uses a non-mainstream classification, so treat with caution). — [Chinese Geol. Bull. paper](https://www.cgsjournals.com/article/id/dztb_20150618)
- Re-Os ages of Kupferschiefer mineralization span the Lower Triassic to Lower Cretaceous, indicating multiphase epigenetic flux after sedimentation. One model has reducing, slightly acid hot brines rising along faults and basement relief to mix with seawater-derived formation waters. — [MDPI Minerals 9(10):592](https://www.mdpi.com/2075-163X/9/10/592); [Kupferschiefer brine composition paper](https://www.literature.wolkersdorfer.info/literature/The%20composition%20of%20brines%20in%20the%20early%20diagenetic%20mineralization%20of%20the%20Permian%20Kupferschiefer%20in%20Germany.pdf)
- Central African Copperbelt: the basal Roan Group has rift-stage continental red beds, then evaporitic carbonate–siliciclastic rocks, then the regionally extensive, organic-rich marginal-marine "Ore Shale" that hosts most ore. Upper Roan evaporites (now dissolved breccias) generated the metal-bearing brines. — [Selley et al. "A New Look at the Geology of the Zambian Copperbelt" (UTas)](https://eprints.utas.edu.au/755/); [EGU24 drillcore study](https://meetingorganizer.copernicus.org/EGU24/EGU24-19482.html)
- Copperbelt ages: about 815 Ma Re-Os for diagenetic sulfide at Konkola; 576 ± 41 Ma for late-diagenetic to early-orogenic Cu-Co; a minor Cu-U-Mo-Au event at about 500 Ma. The origin is multiphase, from early diagenetic to syn-orogenic, plus a supergene phase. — [Geologica Belgica](https://popups.uliege.be/1374-8505/index.php?id=5264)

### Inferences
**Proposed simulation rule:**
- Gate: `atmO2 ≥ POST_GOE_LOW` (red beds and sulfate must exist), t < about 2.1 Ga, `sulfateReducers` present. Plant-debris-hosted red-bed Cu needs `landPlants` (after about 0.42 Ga); algal-mat or black-shale hosts do not.
- Basin: `basinType ∈ {intracontinental rift, sag over rift}`. Column order: (1) continental red beds or bimodal volcanics in the synrift fill (metal source, `hasRedBeds`); (2) a first marine transgression depositing a reduced, organic or pyritic thin unit (black shale or algal-mat carbonate) directly on the red beds; (3) overlying evaporites (`hasEvaporite`) from an arid-belt climate (`lat` in the subtropical belt during the evaporite stage).
- Mineralization stage: during burial diagenesis to basin inversion (10–100 Myr after deposition), when `burialTmax` of the red-bed aquifer is about 50–150 °C. Ore sits in the reduced bed just above the red-bed contact, best over basement highs and basin-margin thinning of red beds, and near faults and salt-diapir margins.
- Geometry: sheet-like and extremely laterally extensive (10^2–10^4 km²) but thin (0.5–20 m). Zoning outward from the fluid entry point: barren red bed, then a hematite "Rote Fäule" front, then a chalcocite/bornite core, then chalcopyrite, then a Pb-Zn fringe.
- Red-bed Cu (small type): continental red-bed sequence with local reduced lenses (channel plant trash, gray sands) in arid continental basins. Many small occurrences, rarely economic.
- **Block mapping:** hypogene chalcocite/bornite/chalcopyrite has no direct TFC block. Use TFC `tetrahedrite` (TFC's Cu sulfide ore; it is mapped to metamorphic rocks in default TFC, so the mod must override host rocks) or TFC `malachite` in near-surface oxidized parts (TFC places malachite in carbonates, which suits dolomitic Copperbelt and Zechstein carbonate hosts). Native copper appears in some red-bed and White Pine–type ores and could be TFC `native_copper` in minor amounts. Vanilla `copper_ore`. Co (carrollite) has no mapping. Ag maps to TFC `native_silver` as a rare by-product.

### Gaps
- Grade–tonnage medians were not retrieved (USGS SIR 2010-5070-M, Hayes et al. 2015, not fetched). Background, not verified: reduced-facies median about 22 Mt at about 2% Cu; Copperbelt giants up to Gt scale at 2–5% Cu.
- Kodaro-Udokan (about 1.9–1.8 Ga) and the White Pine (Keweenawan, about 1.08 Ga) details were not retrieved.

---

## Q7. Uranium deposits in sediments: unconformity-related, sandstone (roll-front, tabular), quartz-pebble paleoplacer and calcrete

### Takeaway
Uranium deposits follow redox chemistry. U(IV) is insoluble and U(VI) is mobile in oxidized water. Detrital uraninite placers therefore exist only before the GOE (older than about 2.4–2.2 Ga). After the GOE, U is transported in oxidized groundwater or brines and trapped at reductants. That trapping produces three types: unconformity deposits (Proterozoic red-bed sandstone basins over graphitic basement, very high grade), roll-fronts (permeable fluvial sandstones with organic or pyrite reductant, mostly Mesozoic–Cenozoic) and calcrete (arid paleodrainages, evaporative, Cenozoic).

### Cited Findings
**Unconformity-related**
- Pods, veins and semi-massive replacements of uraninite sit close to the unconformity between late Paleo- to Mesoproterozoic conglomeratic sandstone and metamorphosed basement. Basin strata are up to 1.5 km thick, flat-lying and unmetamorphosed but pervasively altered. Average Athabasca grades are about 2% U, compared with 0.03% U at Olympic Dam. McArthur River holds 192,085 t U at 22.28% U and Cigar Lake 131,386 t U at 15.02% U. — [Jefferson et al. empirical models](https://www.911metallurgist.com/blog/wp-content/uploads/2015/10/Empirical-Models-for-Canadian-Unconformity-Associated-Uranium-Deposits.pdf)
- The Athabasca system was dominated by basinal brines of evaporated-seawater origin. They percolated through reactivated basement structures, leached metals and caused Mg-B alteration. Many aspects of the genesis are still debated. — [HAL review](https://hal-insu.archives-ouvertes.fr/insu-01060139)
- U-rich brines (0.6–26.8 ppm U) were widespread across the Athabasca Basin, including barren areas. — [Chi et al. 2019, Sci. Rep. (PMC)](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6445087/)

**Sandstone roll-front**
- U(VI) dissolved in oxygenated groundwater is reduced to insoluble U(IV) (uraninite, coffinite) where it meets solid reductants (organic carbon and/or pyrite), producing the classic C-shaped (crescent) roll with the highest grade at the nose and weak "tails" in oxidized ground. — [USGS Texas roll-type models](https://pubs.usgs.gov/publication/70179186); [Nature Comms on roll-front U reduction](https://link.springer.com/article/10.1038/ncomms15538)
- In the Texas Coastal Plain, reducing conditions come from oil and gas or H2S migrating up faults, and less commonly from organic-rich sediments. The district has produced about 80 Mlb U3O8 with about 60 Mlb identified in place. — [USGS Texas](https://pubs.usgs.gov/publication/70179186)
- Sandstone subtypes (roll-front, tabular, basal channel, tectono-lithologic) have significantly different grade–tonnage distributions. — [Global grade–tonnage modelling of U deposits](https://dspace.bits-pilani.ac.in/items/a3b0bc37-b942-4319-8b1c-9b4069f01395)
- Example: Saint Anthony (New Mexico) roll-front deposit, 2,500–5,000 t U at 0.05–0.10% U. — [IAEA UDEPO entry](https://www-pub.iaea.org/MTCD/publications/PDF/TE_1629_CD/Deposits/785.html)
- Sandstone-hosted deposits are mostly in the USA, Australia, Kazakhstan, China and Africa. Paleozoic examples are less common, and most known deposits are Mesozoic–Cenozoic. — [search synthesis; Moore et al. 2024 context](https://carnegiescience.edu/uranium-redox-and-deposition-transitions-embedded-deep-time-geochemical-models-and-mineral)

**Quartz-pebble conglomerate paleoplacer**
- Elliot Lake–Blind River: U is hosted in pyritic quartz-pebble conglomerate of the Matinenda Fm at the base of the Huronian Supergroup (fluvial arkose and quartzite). The reefs resemble Witwatersrand bankets, and the gold and U are inferred to be detrital placers that predate an oxidizing atmosphere. — [Rocky Mtn SEPM depositional model](https://archives.datapages.com/data/rocky_sepm/data/003/003001/39_rocky_mount30039.htm); [Waterloo Matinenda description](https://uwaterloo.ca/earth-sciences-museum/peter-russell-rock-garden/6-matinenda-formation-conglomerate)
- Paleoplacer U occurrences are restricted to rocks older than about 2.5 Ga (general overview). Other sources give >2.1 Ga or link the limit to the GOE. — [Northern Miner Geology 101](https://northernminer.com/news/geology-101-paleoplacer-gold-deposits-pt-1/1000147862)
- Pele Mountain: detrital thorite at 2489 ± 24 Ma, and brannerite at 2403 ± 120 Ma (likely hydrothermal). This is a "modified placer". — [Am. Mineral. 2012 abstract](https://msaweb.org/MSA/AmMin/TOC/2012/Abstracts/AM097P1274.pdf)

**Calcrete**
- Calcrete-hosted U occurs in main drainage channels or calcrete deltas or platforms (valley type, e.g. Yeelirrie), in playa sediments (Lake Maitland) or in terraces. All deposits are at or near the surface, with a capillary or evaporative genesis. The only U mineral is carnotite (K-U-vanadate), and grade is often highest at the surface. — [Company technical document](https://wcsecure.weblink.com.au/pdf/ENT/01029655.pdf)
- Yeelirrie: calcrete fills an Early Tertiary drainage. The orebody is a thin, flat sheet about 9 km long, 0.5–1.5 km wide and up to about 7 m thick. Resource: 52,500 t U3O8 at 1.5 kg/t (0.15%). — [SME 1991 Yeelirrie discovery](https://onemine.org/documents/52-discovery-of-the-yeelirrie-uranium-deposit); [company doc](https://wcsecure.weblink.com.au/pdf/ENT/01029655.pdf)
- Most Australian calcrete U is in Cenozoic drainages of the Yilgarn Craton, Gascoyne and Ngalia Basin, and makes up only about 1% of Australia's U resources. — [Geoscience Australia AusGeo News 2011](https://www.ga.gov.au/ausgeonews/ausgeonews201109/exploring.jsp)
- Mechanism: a redox and evaporative front in saline groundwater oxidizes V4+ to V5+, and uranyl-carbonate complexes destabilize, so carnotite precipitates. — [Saltwork Consultants](https://www.saltworkconsultants.com/uranium-ores-from-saline-arid-zone-groundwaters/)

### Inferences
**Proposed simulation rules:**
- **Paleoplacer U(-Au):** `atmO2 = PRE_GOE` at deposition (t > 2.4 Ga, hard gate). Fluvial braided-plain or fan-delta conglomerate (`env = fluvial_braided`, high `streamPower`) sourced from an Archean granite-greenstone terrane (`srcComp.granite + greenstone`). Basal conglomerates directly above an unconformity. See Q8.
- **Unconformity U:** t of basin deposition about 1.9–1.0 Ga is optimal (any post-GOE age is possible). The column needs a thick (≥1 km) oxidized, quartz-rich continental red-bed sandstone basin (`hasRedBeds`) resting unconformably on metamorphic basement with graphitic pelites (`unconformity` and basement `graphite` flag). Basinal brines (evaporite-derived) circulate at `burialTmax` about 150–250 °C. Place ore at the unconformity where basement fault zones cut it. Orebodies are small (tens to hundreds of metres) but ultra-high grade, clustered along basin-margin fault corridors. Preservation requires the sandstone cover not to be fully eroded or metamorphosed.
- **Roll-front:** `atmO2 = POST_NOE`. The best hosts are permeable fluvial or marginal-marine sandstones with organic debris (`landPlants` true for detrital plant matter; before plants, only H2S or hydrocarbon reductant) or pyrite, confined between mudstones. Requirements: an upstream U source (felsic volcanic ash or granite, `srcComp.felsic`) and an uplift or tilt that sets up a recharge zone at the basin margin with groundwater flowing basinward. The roll front sits at the down-dip edge of the oxidized tongue: 1–30 km from the outcrop recharge, parallel to the outcrop belt, sinuous and kilometres to tens of kilometres long. Late Mesozoic–Cenozoic foreland and intermontane basins dominate (e.g. Wyoming, Texas Coastal Plain, Kazakhstan Chu-Sarysu).
- **Calcrete U:** `aridity` high (arid to semi-arid; deep weathering of a granite craton earlier), very low relief, a long-lived paleodrainage (Cenozoic) and a granite source with U plus a V source. Place in valley-fill calcrete along trunk drainages at constrictions or above playa margins, as a sheet ≤7 m thick and up to about 10 km long. Only at or near the present surface (young, easily eroded).
- **Block mapping:** no uranium in TFC or vanilla, so "no mapping" for all U types. Optional custom `uraninite` / `carnotite` block. Associated real minerals that can be mapped: pyrite in paleoplacers (TFC `pyrite` gem), calcrete as TFC limestone/chalk-like caliche, and graphite in basement (TFC `graphite`, metamorphic).

### Gaps
- Athabasca mineralization ages were not retrieved. Background, not verified: basin about 1.75–1.5 Ga; primary mineralization about 1.6–1.5 Ga. The Alligator Rivers (Australia) type was not covered.
- No source quantified a post-Devonian (land plant) step in sandstone-U ages. The link is inferred.
- Typical roll-front grades and tonnages (median) were not obtained.

---

## Q8. Witwatersrand-type Au(-U) paleoplacers (pre-GOE)

### Takeaway
The Witwatersrand Basin is by far the largest gold concentration in Earth's history, at about 40% of all gold ever mined (about 49,000 t to 2002 at about 8 g/t). The gold lies in Mesoarchean fluvial to fluvio-deltaic pyritic quartz-pebble conglomerate "reefs" with detrital uraninite and rounded pyrite. Most workers favour a (modified) paleoplacer origin. Detrital pyrite and uraninite imply a pre-GOE reducing atmosphere, but a hydrothermal-remobilization view persists. Frimmel and others argue that the extreme gold endowment needs a special Archean process (e.g. microbial gold fixation), but this was not verified this session.

### Cited Findings
- The Witwatersrand goldfields, in low-grade metamorphosed siliciclastic rocks of the Kaapvaal craton, have yielded close to 40% of all gold ever mined. Ore bodies are stratiform in fluvial to fluvio-deltaic quartz-pebble conglomerates (reefs) that are predominantly pyrite-rich and locally uraninite-rich. At the microscale, gold is typically associated with hydrothermal phases and microfractures, hence the hydrothermal and modified-paleoplacer debate. — [Frimmel et al. "Controversies on the origin of world-class gold deposits Pt II: Witwatersrand"](https://www.literature.wolkersdorfer.info/literature/Controversies%20on%20the%20Origin%20of%20World-Class%20Gold%20Deposits%20Part%20II%20Witwatersrand%20Gold%20Deposits.pdf)
- Total production from the start of mining to 2002 was 49,332 t Au at an average recovered grade of about 8 g/t, and individual reefs reach up to 1200 g/t. — [Witwatersrand Metallogenesis](https://911metallurgist.com/wp-content/uploads/2015/10/Witwatersrand-Metallogenesis.pdf)
- The classic detrital argument: gold and hydraulically equivalent uraninite and pyrite survived subaerial and shallow-water exposure because of low pO2. — [1977 AAPG abstract](https://www.searchanddiscovery.com/abstracts/html/1977/annual/abstracts/0815.htm)
- Vaal Reef uraninite has high UO2/ThO2 and Pb contents showing it is older than deposition, so it is detrital. The modified placer model is supported. — [Sebola thesis, Wits](https://wiredspace.wits.ac.za/items/f04c7e55-0820-40f3-b955-0f7c25b79d57)

### Inferences
**Proposed simulation rule:**
- Hard gate: `atmO2 = PRE_GOE` at deposition (t > about 2.4 Ga); best at 3.0–2.7 Ga (Mesoarchean). After the GOE, permit only gold-only paleoplacers (e.g. Tarkwa-type, about 2.1 Ga, rounded hematite instead of pyrite) and suppress U and pyrite.
- Basin: an intracratonic or retroarc foreland basin on a stabilized craton (first large stable continents), with high-energy braided-river fans and fan-deltas fed from a granite-greenstone hinterland that has gold-bearing greenstone (`srcComp.Au_lode` or `greenstone`). Strong control by unconformities and degradation surfaces: reefs sit on erosion surfaces (sequence boundaries) at the basin margin where fans prograde over a lag.
- Concentration: on erosional surfaces (lag gravels), along braided channels and at fan apices near the entry points. Reefs are centimetres to a few metres thick and laterally extensive (tens of km along strike, fan-shaped "payshoots" radiating from entry points).
- Post-depositional: lower-greenschist burial metamorphism remobilizes gold locally (keep the placer geometry).
- Preservation: Kaapvaal-type survival needs early burial under thick lavas (e.g. Ventersdorp-type flood basalts). Add a preservation bonus if a LIP or flood-basalt cover follows within 50 Myr.
- **Block mapping:** TFC `native_gold` (in conglomerate host; TFC "conglomerate" is a sedimentary rock type), TFC `pyrite` (gem) with it; uraninite has no mapping. Vanilla `gold_ore`. This should be the largest gold concentration in any world that has an Archean craton.

### Gaps
- Precise depositional ages (Central Rand Group, background value about 2.9–2.7 Ga) and reef thickness statistics were not retrieved.
- The microbial or "atmospheric gold" hypothesis (Frimmel, Heinrich) was not retrieved.

---

## Q9. Modern (Cenozoic) placers: gold and cassiterite (tin)

### Takeaway
Placers form by hydraulic sorting of dense, chemically resistant grains: settling equivalence followed by entrainment-equivalence winnowing. Trap sites are bedrock riffles, potholes, the base of gravels, inside bends, below knickpoints or slope breaks, and lag gravels. Economic placers lie close to source: tin within about 8 km and gold typically within kilometres to tens of km. Sea-level rise drowns river placers to make offshore ones (Sundaland tin). About 80% of world identified tin resources are placers, concentrated in the SE Asian tin belt.

### Cited Findings
- Placer concentration occurs through sequential hydraulic "laws" acting on one grain population: settling equivalence (sorting by settling velocity) then entrainment equivalence (erosional removal of lighter grains). Placers are rarely preserved in sedimentary sequences. — [USGS OFR 91-306](https://pubs.usgs.gov/of/1991/0306/report.pdf)
- Controls on placer formation: the settling-velocity distributions of heavy and light populations, long-term flow hydraulics, bed roughness and the volume of sediment processed. Two cases: roughness set by grains in transport, or a pre-existing coarse substrate trapping a moving population (bedrock or lag traps). — [Slingerland 1984](https://ftp.ems.psu.edu/pub/geosc/sling/PUBLICATIONS_SLINGERLAND/1981-1990/Slingerland1984.pdf)
- Modelling: gold accumulates in a residual gravel lag. Optimal concentration happens when channels frequently approach the threshold between detachment-limited and transport-limited conditions. Too much transport capacity strips the bedload, and too little dilutes the gold. Placers need 10^3–10^6× concentration of heavy minerals. — [Otago "Gold in the hills" modelling paper](https://ourarchive.otago.ac.nz/esploro/outputs/journalArticle/Gold-in-the-hills-patterns-of/9926515118201891)
- Review of placer formation in gravel-bed rivers (Ore Geol. Rev. 2006, 28:377–401). — [Geologyscience repository](https://repository.geologyscience.ru/handle/123456789/48706?show=full)
- USGS Placer Au-PGE (model 39a): native gold and PGE alloys in alluvial, beach, eolian and glacial deposits. Traps include riffles, fractured bedrock and clay layers. The grade–tonnage model covers 65 deposits, mostly Quaternary alluvial. Grade correlates negatively with tonnage (r = −0.35), and Ag with Au (r = 0.66). Large mountain-front regional placers (dredge-scale, lower grade) and drift-mined placers are excluded. — [USGS model 39a](https://pubs.usgs.gov/bul/b1693/html/bull6945.htm); [USGS G-T model](https://pubs.usgs.gov/bul/b1693/html/bull42lh.htm)
- USGS Alluvial Placer Sn (model 39e): cassiterite in silt- to cobble-size grains in modern and fossil streambeds, commonly late Tertiary to Holocene. Source: evolved granitoids (accreted terranes or stable cratonic foldbelts). Economic placers lie within about 8 km of the primary source. Grains coarsen and become more euhedral toward the source. The richest placers lie directly over the source. Streams flowing parallel to a tin-granite margin are favourable. Traps: the base of gravels, natural riffles, potholes, bedrock structures across flow. Stream placers can extend offshore into drowned valleys and strandlines. — [USGS model 39e](https://pubs.usgs.gov/bul/b1693/html/bull3pke.htm)
- About 80% of the world's identified tin resources are unconsolidated placers, with the largest onshore and offshore concentration in the SE Asian belt (S China, Thailand, Myanmar, Malaysia, Indonesia). — [USGS tin (search summary)](https://www.usgs.gov/publications/tin); [USGS PP1802-S](https://pubs.usgs.gov/publication/pp1802S)
- SE Asian tin placers derive from Triassic tin granites. Types: eluvial, colluvial, alluvial, beach and reworked tailings. Bangka: residual eluvial on slopes, placers directly on mineralized bedrock, alluvial valley fills. Drowning of old river systems by Late Cenozoic sea-level rise is "by far" the most important factor for giant offshore placers. — [Indonesian Bangka study](https://ejournal.mgi.esdm.go.id/index.php/bomg/article/download/285/275); [Geol. Soc. Malaysia Bull. 11](https://gsm.org.my/?p=4553)

### Inferences
**Proposed simulation rule (generic placer engine, reused by Q10/Q11):**
1. For each river cell, compute the heavy-mineral supply `HM_supply[m] = Σ_upstream erosionRate × srcComp[m]` (gold from Au lodes, greenstone and orogenic gold belts; cassiterite from Sn-granite and pegmatite cells; plus other heavies).
2. Apply a survival or transport decay: `supply × exp(−d / L_m)`, with `L_Sn` about 3–8 km (brittle, so it comminutes; economic within about 8 km), `L_Au` about 10–50 km (flattens and fines; tunable), and `L_diamond` up to 10^3 km (Q11).
3. Concentration factor `C = f(streamPower)`. It peaks where the reach is near the threshold between detachment- and transport-limited conditions (Otago), and is boosted at trap sites: (a) a slope break, knickpoint base, or the canyon mouth onto a fan or valley floor, (b) the inside of meander bends (point bars), (c) the bedrock–gravel contact (base of the alluvium in thin-alluvium bedrock reaches), (d) confluences, (e) below gorges. Apply when `C × HM_supply` exceeds a threshold.
4. Vertical placement: a basal pay layer 0.3–2 m thick on bedrock or a false bedrock (clay), under 2–30 m of barren gravel. Paleo-channels (raised terraces, "deep leads" buried by lava) are preserved when incision or a lava cover occurs (Victoria deep leads analogy).
5. Sea-level coupling: when `dSeaLevel` > 0 (transgression) drowns a river valley holding placers, keep them as offshore placers in submerged channels, and rework them into beach placers at transgressive shorelines.
6. Climate coupling: deep tropical weathering of the source releases more cassiterite and gold (eluvial and colluvial placers on granite slopes). Glacial erosion followed by fluvial reworking makes glacio-fluvial gold placers (Yukon, NZ).
- **Block mapping:** TFC already has river "deposit" blocks for panning: native gold, native silver, native copper and cassiterite. The TFC field guide lists river deposits for native copper, native gold, native silver and cassiterite ([TFC Field Guide: Ores and Minerals](https://terrafirmacraft.github.io/Field-Guide/en_us/the_world/ores_and_minerals.html)). Map placer gold to TFC gold deposit blocks (gravel-hosted), placer tin to TFC cassiterite deposits, and vanilla to `gold_ore` in gravel or vanilla `raw_gold` drops. Black sand (magnetite) maps to TFC `magnetite` small ore.

### Gaps
- The median grade–tonnage values for placer gold (USGS figures 196–197) were not legible. Background, not verified: median about 1.1 Mt at about 0.2–0.3 g/t.
- No source gave a quantitative gold transport distance. The "travels little" statement came from non-authoritative pages. Gold e-folding transport lengths are a tuning parameter.

---

## Q10. Heavy-mineral sands (ilmenite, rutile, leucoxene, zircon, monazite)

### Takeaway
Coastal heavy-mineral sand (HMS) deposits are the main source of Ti feedstock and zircon. They form on wave-dominated, stable (passive) coastlines fed by deeply weathered high-grade metamorphic and granitic terranes. Heavies concentrate in beach swash and foredune zones during regressions or stillstands, as "shoestring" strandlines parallel to the coast. These are later stranded inland, preserved by progradation, and upgraded by post-depositional weathering (ilmenite to leucoxene). Grades are low (cut-off about 1–2% THM) but tonnages are large (tens to hundreds of Mt).

### Cited Findings
- HMS are the main source of Ti feedstock for TiO2 pigment (ilmenite, rutile, leucoxene) and the principal source of zircon. Monazite (REE, Th) is a coproduct, along with sillimanite/kyanite, staurolite and garnet. — [USGS SIR 2010-5070-L](https://pubs.usgs.gov/publication/sir20105070L)
- Regression and transgression strongly influence deposition and preservation, and most accumulation is tied to seaward shoreline movement during regressions. Local faulting shapes the coastal geomorphology. Weathering after deposition (groundwater, humic acids) removes Fe from ilmenite to form leucoxene and raises TiO2. — [USGS SIR 2010-5070-L summary](https://pubs.usgs.gov/sir/2010/5070/l)
- USGS Shoreline Placer Ti (model 39c): well-sorted medium–fine sand in dune, beach and inlet deposits over shallow-marine sediments. Commonly Miocene–Holocene (any age possible). Setting: a stable coastal region receiving sediment from deeply weathered metamorphic terranes of sillimanite grade or higher, on a craton margin with crustal stability. Ore controls: a high-grade metamorphic source, a stable coastline with efficient sorting and winnowing, and weathering. Elongate "shoestring" bodies parallel to dunes and beaches. Examples: Trail Ridge and Green Cove Springs (Florida), Lakehurst (NJ), Eneabba (W. Australia). — [USGS model 39c](https://pubs.usgs.gov/bul/b1693/html/bull273d.htm)
- Provenance: HMS derived from high-grade metamorphic rocks are richer in rutile and monazite. — [USGS Atlantic Coastal Plain critical minerals](https://www.usgs.gov/index.php/centers/gggsc/science/critical-mineral-resources-heavy-mineral-sands-us-atlantic-coastal-plain)
- Grade and size: the lowest cut-off in most deposits is about 1% THM, and valuable minerals rarely exceed about 30% of the THM. Strandline deposits are typically tens to hundreds of Mt. Coburn (WA) is 230 Mt at 1.1% HM over 13 km. — [Wikipedia HMS article (aggregator)](https://en.wikipedia.org/wiki/Heavy_mineral_sands_ore_deposits). Tormin (South Africa) beaches: 1.1 Mt at 8.98% THM and 2.4 Mt at 21.61% THM. — [Mining Weekly](https://www.miningweekly.com/article/tormin-mineral-sands-operation-south-africa-2023-05-05)
- The Murray Basin (about 300,000 km²) hosts coarse rutile, zircon and ilmenite strandlines in the Loxton–Parilla Sands. The NSW part alone holds more than 145 Mt of HMS. — [NSW heavy mineral sands](https://www.resources.nsw.gov.au/sites/default/files/2022-11/heavy-mineral-sands.pdf)
- Two HMS operations are active in the SE United States. — [USGS SIR 2018-5045](https://pubs.usgs.gov/sir/2018/5045/sir20185045.pdf)

### Inferences
**Proposed simulation rule:**
- Supply: coastal sediment flux with HM fraction = Σ upstream (`srcComp.highgrade_meta` × k1 + `srcComp.granite` × k2 + `srcComp.mafic` × k3 for ilmenite), weighted by catchment chemical weathering (humid, warm, long exposure destroys labile heavies such as amphibole and pyroxene and leaves the resistant ones).
- Coast: `env ∈ {beach/foreshore, foredune}` on a wave-dominated, microtidal, passive or stable margin (`basinType = passive margin`, low tectonic uplift), with high wave energy and longshore drift.
- Timing: concentrate at regressive stillstands. For each shoreline position held for ≥ about 10 kyr during a regression (or at a transgressive maximum that is then abandoned), emit a strandline. Repeated sea-level oscillations produce a stair-step series of parallel strandlines inland of the coast (Murray Basin style).
- Geometry: 1–10 km long, 100–1000 m wide and 2–20 m thick shoestrings with 1–10% THM, parallel to the paleo-coast. Bigger dune-hosted low-grade bodies sit landward of them.
- Post-depositional: humid weathering of preserved strandlines converts ilmenite to leucoxene (Ti upgrade).
- **Block mapping:** no ilmenite, rutile, zircon or monazite in TFC or vanilla, so "no mapping" (or custom). The magnetite and ilmenite "black sand" layer could appear as TFC `magnetite` small ores in sand or a dark sand variant. Garnet sand has no mapping. These deposits could become visible as dark-sand laminae in TFC beach sand for flavour.

### Gaps
- No primary source gave statistical distributions of strandline dimensions (width and thickness). The values above are inferred.
- Richards Bay (South Africa) and Indian (Chavara) examples were not retrieved.

---

## Q11. Diamond placers (alluvial and marine) and gem gravels (sapphire, ruby)

### Takeaway
Diamond placers are fed from kimberlite or lamproite pipes on old cratons and are the most transport-robust placers. Orange River diamonds travelled hundreds of km to the Namibian coast over tens of Myr. Long transport winnows flawed stones, so far-travelled placers are dominated by gems. Coastal longshore drift redistributes them (sand travels up to 1,750 km north of the Orange River mouth). Corundum (sapphire and ruby) gem gravels come from two sources: alkali basalt (volume; e.g. E. Australia, Thailand) and high-grade metamorphic terranes (quality; Sri Lanka, Madagascar Ilakaka).

### Cited Findings
- Orange River sand moves north on persistent longshore currents as far as northern Namibia and southern Angola, about 1,750 km from the mouth, "the longest cell of littoral sand transport documented". About 80% of Skeleton Coast dune sand is Orange-derived. The gem diamonds in Namibian coastal placers all come from the Orange River, and the Kunene is ruled out. — [Garzanti et al. 2014 (CNR record)](https://iris.cnr.it/handle/20.500.14243/291089); [Univ. Milano-Bicocca record](https://boa.unimib.it/handle/10281/52904)
- The palaeo-Orange mouth is an extreme wave-dominated delta with coarse Plio-Pleistocene littoral gravel placers preserved for more than 150 km north. Proximal spits and barrier beaches give way after about 5 km to narrow linear beaches, and after about 70 km to pocket beaches. — [2006 palaeo-Orange study (EIS library)](https://the-eis.com/elibrary/search/2491)
- Namibian coastal diamonds come from kimberlites on South Africa's interior plateau, more than 300 miles (about 480 km) east. The diamonds took about 80 Myr to reach the coast, and long transport winnowed poorer crystals. — [GIA Namibia reading list](https://www.gia.edu/articles/alluvial-diamonds-from-namibia-reading-list)
- Lower Vaal River: alluvial diamond grade depends on tectonic setting, paleoclimate, level of erosion, drainage stability and local trap sites in the river profile (bed roughness, channel width, sorting with distance from source). — [Rhodes University MSc](https://researchrepository.ru.ac.za/handle/123456789/5189)
- In the 1930s, alluvial mines produced 91% of world diamond output by weight and 77% by value. This figure is historical and not current. — [Minerals Yearbook text (UW digital)](https://search.library.wisc.edu/digital/ADCTPMSF6KGZOY85/text/A2FZPZ3AHICZOK9A)
- Argyle (Kimberley craton, WA) was found by drainage sampling in Smoke Creek in 1979, which led to the lamproite pipe. Its alluvials are mainly industrial-quality. — [AusIMM Argyle](https://onemine.org/documents/the-discovery-and-geology-of-the-argyle-diamond-deposits-kimberley-western-australia)
- Corundum deposits are primary (magmatic, metamorphic) or secondary (placer). Most sapphires come from placers related to alkali basalts (E. Australia, S. Vietnam). Metamorphic-terrane placers (Sri Lanka Ratnapura and Elahera, Madagascar Ilakaka) yield the highest-quality sapphires. — [GIA geology of corundum and emerald deposits](https://origin.prod.gia.edu/dam/jcr:f12cfeb2-b481-40f1-b554-3a803c18d18e/WN19-geology-of-corundum-and-emerald-gem-deposits.pdf)
- Madagascar gem gravels are worked from pits and are geologically similar to the alluvial deposits of Sri Lanka and Tanzania. Secondary deposits include eluvial and alluvial placers and beach ridges. — [Gem-A Journal of Gemmology 2001](https://gem-a.com/images/Documents/JoG/Archive/JoG2001_27_7.pdf); [IRD document](https://horizon.documentation.ird.fr/exl-doc/pleins_textes/divers19-10/010037123.pdf)

### Inferences
**Proposed simulation rule:**
- Source cells: kimberlite or lamproite pipes from the magmatic module (on cratons older than about 1.5–2.5 Ga, i.e. lithospheric keels). Erosion of a pipe adds diamonds to the river load in proportion to the eroded volume.
- Transport: very long survival (`L_diamond` about 500–2,000 km). Apply a quality filter: the gem fraction increases with transport distance (e.g. gemFraction = 1 − (1 − g0)·exp(−d/300 km)).
- Fluvial traps: bedrock potholes and gullies, the base of terrace gravels (incised "Older Gravels" terraces on the Vaal), knickpoints.
- Marine: where a diamondiferous river reaches a high-wave-energy coast, emit placers in raised beaches and gravel spits. The richest are within about 5 km of the mouth. Linear beaches continue to about 70 km, and pocket beaches and longshore-drift transport extend 10^2–10^3 km down-drift (direction from simulated prevailing wind and coast orientation). Drowned paleo-shorelines on the shelf (−20 to −120 m, from glacial lowstands) host offshore placers.
- Corundum gravels: source either alkali-basalt fields (intraplate or rift volcanic cells) or high-grade metamorphic terranes (granulite and marble belts, `srcComp.highgrade_meta`). Use short to moderate transport (L about 10–50 km) and humid-tropical deep weathering (Sri Lanka-type "illam" gravel under soil). Ruby favours marble (metamorphic) sources and sapphire favours basalt and granulite sources.
- **Block mapping:** diamond in TFC gem `diamond` (TFC places primary diamonds in kimberlite pipes in gabbro; placers would be the mod's addition, e.g. diamond-bearing gravel deposits) and vanilla `diamond_ore` (but as gravel-hosted). Sapphire and ruby in TFC gems `sapphire` and `ruby` (in gravel deposits). Topaz in TFC `topaz` (topaz also occurs in tin-granite placers per the USGS Sn model mineral list). Garnet and spinel have no mapping. Opal (weathering and silica in sedimentary basins) in TFC `opal` is outside this scope, but TFC places opal "beneath rivers" in sedimentary and extrusive rocks.

### Gaps
- No current figure was found for the alluvial share of diamond production. Bluck, Ward & De Wit (2005) on diamond megaplacers was not retrieved.
- Quantitative gem-gravel grades were not found.

---

## Q12. Marine evaporites: gypsum/anhydrite, halite, potash (sylvite, carnallite, polyhalite)

### Takeaway
Basin-wide marine evaporites need a hydrographically isolated, subsea-level basin in the subtropical arid belt, fed by seepage or overflow of seawater: rifts, collision sutures and intracratonic sags. Minerals precipitate in order as seawater concentrates: carbonate, gypsum at about 4–5× seawater concentration, halite at about 10–12×, and K-Mg bitterns at more than about 60–90×. Halite therefore dominates volumes and potash is rare and episodic. The largest potash deposits are in intracratonic basin-wide evaporites with no modern analogue. Potash mineralogy (sulfate-rich or KCl-only) may track seawater chemistry.

### Cited Findings
- Quaternary evaporites are poor analogues for most Phanerozoic deposits. Most Neogene economic deposits occur in suprasea-level, endorheic continental settings. — [Warren 2012 AAPG](https://www.searchanddiscovery.com/abstracts/html/2012/90141geo/abstracts/warren.htm)
- In an arid belt, large parts of a collision suture or an incipient rift can be subsea-level and hydrographically isolated yet fed by seawater seepage and overflow. Basin-wide deposits are classified as convergent (collision), divergent (pre-, syn- and post-rift) and intracratonic. — [Warren 2010 GEO Bahrain abstract](https://www.searchanddiscovery.com/abstracts/html/2010/geo_bahrain/abstracts/Warren2.html)
- Ancient potash formed in basin-wide settings with no same-scale modern counterpart, and intracratonic rather than plate-edge basin-wide evaporites host the larger potash deposits. — [Warren 2012 ACE abstract](https://www.searchanddiscovery.com/abstracts/html/2012/90142ace/abstracts/warr.htm); [EarthDoc Warren](https://www.earthdoc.org/content/papers/10.3997/2214-4609-pdb.248.297)
- The Tethys region was markedly arid from the Late Mesozoic to the mid–late Cenozoic, and potash-forming epochs get progressively younger from west to east along the Tethyan salt chain. Khorat Plateau (Thailand/Laos) Mid–Late Cretaceous potash formed in a high-temperature, extremely arid climate. — [China Geology 2020](https://doaj.org/article/4287091855fa4e538bbf85749d5abc77)
- Delaware Basin potash is in Upper Permian (Ochoan, about 250 Ma) marine evaporites. Few places on Earth today are arid enough to produce significant potash. — [NM Bureau of Geology](https://geoinfo.nmt.edu/banner/home.cfml?id=214)
- Precipitation thresholds: gypsum begins at about 4–5× seawater concentration, halite at 10–12× (340–360‰, density about 1.22 g/cc), and bitterns at more than 70–90×. Carnallite and epsomite are the dominant modern marine bitterns. Predicting the bittern sequence is the hardest part. — [Saltwork Consultants, marine brine](https://www.saltworkconsultants.com/marine-brine-modern/)
- Alternative thresholds: gypsum starts when 15% of the brine remains, halite at 10%, K-Mg salts at 5%. Evaporating 1,000 m of seawater gives about 0.61 m of gypsum and 13.3 m of halite. — [LibreTexts Sedimentary Geology 7.01](https://geo.libretexts.org/Courses/SUNY_Potsdam/Sedimentary_Geology%3A_Rocks_Environments_and_Stratigraphy/07%3A_Chemical_Biochemical_and_Other_Sedimentary_Rocks/7.01%3A_Evaporites). Sources disagree on the bittern threshold.
- Evaporating modern seawater should deposit MgSO4 salts (e.g. polyhalite, kieserite) before or with sylvite and carnallite, yet many ancient potash deposits lack them. Explanations include secular seawater change (Hardie) or non-marine (spring) inflow. Qarhan playa (China) potash lacks MgSO4 salts because of spring inflow and resembles the Devonian Prairie Formation. — [Geochemical Society document](https://geochemsoc.org/index.php/download_file/view/1033/377)
- Evaporite basins are subtropical (Hadley descending limbs), with secular latitude shifts as in Q0. — [Evans 2006 summary](https://snowballearth.org/news.html)

### Inferences
**Proposed simulation rule:**
- Eligibility per basin (connected low region): (1) `lat` in the evaporite belt (bimodal subtropical band; aridity from the climate model > threshold); (2) a basin floor below sea level, or a marine-connected depression, with a restricted connection (`restricted` ≥ 0.8: a sill or narrow seaway set by tectonic topography, e.g. an early rift, a foreland or suture trough, or an intracratonic sag barred by uplift); (3) inflow less than evaporation.
- Mass-balance model: track brine concentration factor `CF` from inflow and evaporation. Deposit carbonate (CF 1–4), then gypsum or anhydrite (CF 4–11), halite (CF 11–60) and potash (CF > 60–90). Potash forms only if the basin reaches near-desiccation (draw-down) at basin centres. It is thinner (1–30 m beds) and lies in the central, last-to-dry depocentre above thick halite.
- Facies pattern (bullseye): carbonate and sulfate at basin margins and platforms (sabkha or saltern), halite in the basin centre (hundreds of metres to kilometres thick over 10^4–10^6 km²), potash at the centre-top of halite cycles. Repeated cycles follow sea-level oscillations.
- Burial: gypsum converts to anhydrite at about 1 km burial or about 40–60 °C, and anhydrite rehydrates to gypsum when exhumed near the surface (background, not verified). Halite more than about 1 km thick under clastic overburden becomes mobile: generate salt diapirs and walls (needed for caprock sulfur in Q14 and for traps and fluid focusing in Q5/Q6). Halite and potash are lost by dissolution where exhumed under humid climate, leaving collapse breccias and gypsum caprock.
- Secular modifiers: greenhouse (high freeboard) gives platform mega-sulfates. Potash mineralogy: with `seaChem = CALCITE_KCl`, sylvite and carnallite only; with `ARAGONITE_MgSO4`, add polyhalite and kieserite.
- Typical Earth deposits to calibrate (background, not verified): Devonian Elk Point/Prairie Evaporite (Saskatchewan, the largest potash), Permian Zechstein and Delaware, Cretaceous Khorat and Congo/Gabon (South Atlantic rift), Miocene Messinian (Mediterranean salinity crisis, about 5.97–5.33 Ma, up to about 1.5–3 km of salt).
- **Block mapping:** TFC `halite` (rock salt), TFC `gypsum` (gypsum or anhydrite), TFC `sylvite` (sylvite and carnallite; TFC places sylvite in shale, claystone and chert as very large flat deposits). Vanilla has no mapping. Anhydrite could reuse gypsum. TFC treats all three as "very large, flat deposits near the surface" in sedimentary rock ([TFC Field Guide](https://terrafirmacraft.github.io/Field-Guide/en_us/the_world/ores_and_minerals.html)), which fits the bullseye stratiform model well.

### Gaps
- Evaporite deposition rates and the paleolatitude degree band could not be confirmed (Warren 2016 and Cao et al. 2018 full text were not retrieved).
- The Phanerozoic potash epochs list (Warren's "Potash Resources: Occurrences and Controls" chapter) was not retrieved.

---

## Q13. Continental (non-marine) evaporites and brines: borates, nitrates (Atacama), lithium brines (salars)

### Takeaway
These are young (mostly Neogene–Quaternary), easily destroyed deposits of closed (endorheic) basins in arid climates, and they need a volcanic or geothermal element source. Borates come from Neogene post-collisional extensional basins with calc-alkaline volcanism and thermal springs (Turkey, Death Valley, Argentina). Nitrates come from hyperarid (<2 mm/yr) surfaces accumulating atmospheric nitrate over about 10^7 yr (Atacama). Li brines need six conditions: an arid climate, a closed basin with a salar or lake, igneous or geothermal activity, tectonic subsidence, a Li source, and time.

### Cited Findings
**Borates**
- Turkish borates formed in lakes during Neogene calc-alkaline volcanism and are interbedded with volcanic ash, limestone, marl and clay. Major deposits are associated with volcanics and thermal springs (B sources). — [Helvacı 2015, MTA Bull.](https://dergi.mta.gov.tr/article/show/167); [Palmer & Helvacı 1997 B isotopes](https://literature.wolkersdorfer.info/literature/The%20boron%20isotope%20geochemistry%20of%20the%20Neogene%20borate%20deposits%20of%20western%20Turkey.pdf)
- Conditions to form and preserve large borates are most common in post-collisional settings such as W Anatolia. All major economic borates are in non-marine evaporites in extensional basins within collisional terrains. — [Helvacı (Southampton eprint)](https://eprints.soton.ac.uk/413247/1/Helvaci_revised_April_24.pdf)
- Kırka formed by evaporation of playa lakes fed by geothermal springs, and is linked to the Miocene Kırka–Phrigian caldera. Favourable factors: playa lakes, andesitic–rhyolitic volcanic B source, ash fall or hydrothermal input along graben faults, arid to semi-arid climate, lake pH about 8.5–11. — [Kırka B-isotope study](https://www.literature.wolkersdorfer.info/literature/The%20boron%20isotope%20geochemistry%20of%20the%20Kirka%20borate%20deposit%20western%20Turkey.pdf); [JMO paper](https://www.jmo.org.tr/resimler/ekler/e392f0007159463_ek.pdf)
- W Anatolia, Death Valley (California) and Sijes (Argentina) are Neogene borate districts. Turkey and Argentina borates are in Miocene playa-lake deposits resting unconformably on Paleozoic–Mesozoic basement. Turkey has the largest borax, ulexite and colemanite reserves. — [Turkey–Argentina comparison (CONICET)](https://ri.conicet.gov.ar/handle/11336/128985)

**Nitrates (Atacama)**
- Nitrate's primary origin is predominantly atmospheric. Its accumulation in the Atacama reflects a unique convergence of climatic, tectonic and hydrologic conditions in the Central Andes, with negligible precipitation (<2 mm/yr) in the hyperarid core. The onset of hyperaridity is debated (Late Paleogene to Pleistocene). — [Reich & Bao 2018, Elements (Univ. Chile)](https://repositorio.uchile.cl/handle/2250/169399); [LSU record](https://repository.lsu.edu/geo_pubs/198)
- N and O isotopes support high-grade caliche nitrate as long-term accumulation of atmospheric deposition over about 10^7 yr. — [Böhlke, Ericksen & Revesz 1997 (USGS)](https://pubs.usgs.gov/publication/70019976)
- Alternative: more than 10^4 km³ of Altiplano–Puna ignimbrite eruptions may have fixed about 2800 Mt of atmospheric N as NO3, with hyperaridity needed for preservation. — [Oyarzun & Oyarzun Muñoz 2007 (UCM)](https://docta.ucm.es/entities/publication/c0a9920c-a2be-4a12-bed7-5d71150de83c)

**Lithium brines**
- The USGS preliminary deposit model (Bradley et al. 2013, OFR 2013-1006) uses Clayton Valley (Nevada) and Salar de Atacama (Chile) as type examples. Li brines accounted for about three-quarters of world Li production at the time. — [USGS OFR 2013-1006](https://pubs.usgs.gov/of/2013/1006); [USGS pub page](https://pubs.usgs.gov/publication/ofr20131006)
- The six shared characteristics are: arid climate; a closed basin with a salar and/or salt lake; associated igneous and/or geothermal activity; tectonically driven subsidence; a suitable Li source; and sufficient time to concentrate brine. — [Munk et al. 2016, SEG Reviews 18 ch. 14](https://pubs.usgs.gov/publication/70104988); [NMT PDF](https://geoinfo.nmt.edu/staff/mclemore/teaching/documents/14_Munketal.pdf)

### Inferences
**Proposed simulation rules:**
- **Closed-basin detection** (shared): from the drainage network, find endorheic basins (sinks with no outlet). Compute the basin water balance from rainfall and evaporation over the catchment. If P−E < 0 over the basin floor, set `env = playa/salar`.
- **Borates:** endorheic basin + extension (graben or caldera, `basinType ∈ {post-collisional extensional, intra-arc caldera, back-arc}`) + active felsic–intermediate volcanism within about 50 km + hot springs + semi-arid to arid climate. Age: active only in the last tens of Myr of the simulation (older borates mostly lost; allow rare preserved Neogene-like examples). Geometry: lens-like lacustrine beds 1–100 m thick over a few km² to tens of km², interbedded with tuff and claystone.
- **Nitrates:** hyperarid (P < about 2–10 mm/yr sustained for ≥ 5–10 Myr), low-relief surfaces (forearc pediment, Central Depression type) between a coastal range and the arc, upwind of an ocean with a cold current (simulated eastern-boundary upwelling and a subtropical high). Geometry: caliche crust 0.2–5 m thick blanketing a belt hundreds of km long, at the surface or very shallow. Any rain event (aridity drop) destroys it, so it should be rare and young.
- **Li brines:** endorheic basin in an arid belt or high plateau (orogenic plateau, Altiplano-like; or Basin and Range extension) with felsic volcanics or geothermal activity in the catchment, subsidence (thick basin fill), and persistence of closed drainage ≥ about 1 Myr. Li concentrates in the subsurface brine of the salar nucleus (halite) and in clays (lacustrine Li-clay variant, e.g. McDermitt caldera).
- **Block mapping:** borax in TFC `borax` (TFC: claystone, limestone, shale; very large flat deposits; very good fit). Nitrate in TFC `saltpeter` (TFC: sedimentary, very large flat near-surface; a good fit for caliche). Lithium has no mapping (brine; no block), so represent salar crusts as TFC `halite` and gypsum, plus a custom brine fluid if desired. Vanilla has nothing.

### Gaps
- Atacama nitrate grades (% NaNO3, iodine) and belt dimensions could not be retrieved (Univ. Chile PDF behind bot protection). Background, not verified: belt about 700 km long; caliche typically 7–10% NaNO3 historically, with iodate co-products.
- The modern lithium production share has shifted toward hard-rock (spodumene) since 2016. The "three-quarters" figure is dated.

---

## Q14. Native sulfur in salt-dome caprock (and stratabound biogenic sulfur)

### Takeaway
Native sulfur forms where hydrocarbons and sulfate-reducing bacteria consume caprock anhydrite above salt diapirs. The process makes H2S, which oxidizes to native S plus secondary calcite. It occurs only in a few domes (Gulf Coast) and in bedded evaporites (stratabound type, e.g. Poland and Sicily), and it was historically recovered by the Frasch process.

### Cited Findings
- Rising salt dissolves near the surface and leaves an anhydrite residual caprock. Gulf Coast caprocks are commonly secondary limestone over residual anhydrite. Sulfur forms by bacterial sulfate reduction accompanying petroleum destruction to H2S, which is then oxidized to elemental S. Bacteria use anhydrite as an oxidant to metabolize oil or gas, forming calcite and native S. — [USGS OFR 92-403 descriptive model (Long)](https://pubs.usgs.gov/of/1992/0403/report.pdf); [AAPG 2005 abstract](https://www.searchanddiscovery.com/abstracts/html/2005/annual/abstracts/peters01.htm)
- Caprock calcite δ13C ranges from −1 to −52‰, indicating a substantial methane component. — [AAPG 2005 abstract](https://www.searchanddiscovery.com/abstracts/html/2005/annual/abstracts/peters01.htm)
- A Zechstein salt dome in Germany has native S with isotopes indicating bacterial sulfate reduction, by an anaerobic mechanism. — [Springer, Mineralium Deposita](https://link.springer.com/article/10.1007/BF01128710)
- Biogenic sulfur is divided into salt-dome and stratabound models. Salt-dome S is limited to the caprock, and commercial quantities occur in only a few domes. — [USGS OFR 92-403](https://pubs.usgs.gov/of/1992/0403/report.pdf)
- The Frasch process injects water at about 150 °C to melt sulfur. — [Wikipedia: Salt dome (aggregator)](https://en.wikipedia.org/wiki/Salt_dome)

### Inferences
**Proposed simulation rule:**
- Requires: a halokinetic salt diapir from Q12 (thick halite plus overburden) whose crest reached less than about 1 km depth (dissolution and caprock formation), together with hydrocarbon presence (organic-rich source rock in the basin with `burialTmax` in the oil/gas window, 60–150 °C) and a meteoric water connection.
- Probability: low per dome (only about 10% of domes hold commercial sulfur; this is an inference from "a few domes"). Place S as a 10–100 m thick porous zone in the calcite part of the caprock above the anhydrite, on the dome crest, about 0.5–2 km in diameter.
- Stratabound variant: bedded gypsum or anhydrite in a basin with hydrocarbon seepage along faults (e.g. foreland basins). The anhydrite is replaced in tabular zones.
- **Block mapping:** TFC `sulfur`. Default TFC places sulfur in metamorphic and igneous intrusive rock near lava level, so the mod should add a sedimentary caprock occurrence. Vanilla has no mapping. The caprock itself would be limestone or gypsum blocks.

### Gaps
- No quantitative data on the proportion of domes that are sulfur-bearing or on deposit sizes (USGS OFR 92-403 PDF not fully read).

---

## Q15. Phosphorites and glauconite

### Takeaway
Phosphorites (P2O5 typically about 10–35%) form on shelves and epeiric seas where high productivity (coastal upwelling on western or equatorward-facing margins in subtropical to temperate latitudes, or nutrient-rich epeiric seas) combines with slow, condensed sedimentation and suboxic pore waters. Repeated winnowing and reworking concentrates the phosphate. There are four global phosphogenic episodes: 2.2–1.8 Ga (minor), 740–410 Ma (first giants), 305–250 Ma, and 200–10 Ma (largest, including the South Tethyan province). Glauconite (glaucony) marks the same condensed transgressive intervals on shelves at less than about 500 m depth.

### Cited Findings
- Four phosphogenic episodes: Paleoproterozoic (2.2–1.8 Ga) small sub-economic peritidal deposits; about 740–410 Ma upwelling-related epeiric-sea deposits and the first giant phosphorites; Carboniferous–Permian (305–250 Ma) continental-margin and epeiric phosphorites; Jurassic–Miocene (200–10 Ma) with the largest accumulations. The South Tethyan province holds at least half of the global phosphate reserve base. — [IMS 2016 abstract "Sedimentary phosphate deposits"](https://programme.exordo.com/ims2016/delegates/presentation/355)
- Gafsa-Metlaoui (Tunisia) shows the classic coastal-upwelling trinity of phosphorite, organic-rich marl and diatom porcelanite. — [Kingston Univ. eprint (Jarvis)](https://eprints.kingston.ac.uk/id/eprint/38766/1/Jarvis-I-38766-AAM.pdf)
- Controversy: some major phosphorites cannot be explained by upwelling models, and modern phosphorites also form outside upwelling centres. The Permian Phosphoria (about 20°N paleolatitude in western Pangea) has no satisfactory modern upwelling analogue, and its shallow oxic setting makes the upwelling model "difficult at best". Southern North Sea Tertiary phosphorite concretions formed in shelf seas remote from upwelling. — ["Are Phosphorites Reliable Indicators of Upwelling?"](https://kirjandus.geoloogia.info/reference/27677); [GSA 2010 Phosphoria abstract](https://gsa.confex.com/gsa/2010RM/webprogram/Paper171833.html); [North Sea concretions](https://natuurtijdschriften.nl/pub/521274/MWTKG1987024001008.pdf)
- Mediterranean and Scandinavian (Cambrian–Ordovician) phosphorites formed as West Gondwana and Baltica crossed subtropical and temperate southern paleolatitudes, with upwelling and high productivity along their western margins. — [Ahlberg publication (Lund)](https://www.geologi.lu.se/ahlberg/publication/2049d37d-2aea-4439-9eb7-b19aefba003a)
- Equatorial Pacific seamount phosphorites record Cenozoic episodes at 39–34 Ma and 27–21 Ma, tied to climate transitions. — [USGS](https://pubs.usgs.gov/publication/70018023)
- Average sedimentary rock has about 0.09 wt% P, and phosphorites typically about 10–20 wt% P2O5. — [Lumiste thesis, Univ. Tartu](https://dspace.ut.ee/bitstream/10062/74475/1/lumiste_kaarel.pdf). Marketed rock is beneficiated to ≥28%, often >30% P2O5 — [Wikipedia (aggregator)](https://www.Wikipedia.com/wiki/Phosphorite). Egyptian Duwi ore averages about 25% P2O5 — [Ain Shams Univ.](https://scholar.asu.edu.eg/handle/123456789/221595)
- Glaucony: commonly low-latitude shallow marine at depths below 500 m (often 100–300 m), temperatures below 15 °C, suboxic, with low sedimentation rates and condensed intervals. Requirements include a permeable granular substrate, Eh about 0 mV, pH 7–8, and residence near the sediment–water interface. In-situ glaucony marks the transgressive systems tract and maximum flooding condensed sections. — [Weddell Sea glaucony (PMC)](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6753099/); [MDPI Minerals 13(2):177](https://www.mdpi.com/2075-163x/13/2/177); [Geological Digressions](https://geological-digressions.com/glauconite-in-thin-section)
- Deep-sea glaucony forms at about 2,100 m and 3–6 °C, about 5× slower than on shelves. Abundant reactive Fe and weakly reducing conditions are the two key requirements. — [Clays & Clay Minerals, Ivory Coast margin](https://resolve.cambridge.org/core/journals/clays-and-clay-minerals/article/rate-and-mechanism-of-deepsea-glauconite-formation-at-the-ivory-coastghana-marginal-ridge/A230ABB6B4ED9A7EE04AC103B38A78C8); [C. R. Géoscience](https://comptes-rendus.academie-sciences.fr/geoscience/item/10.5802/crgeos.274.pdf)

### Inferences
**Proposed simulation rule:**
- Phosphorite: gate `t < 0.75 Ga` for giants (with minor peritidal occurrences at 2.2–1.8 Ga). The cell is an outer or mid shelf or epeiric sea at 30–300 m depth, with high `upwelling` (wind-driven offshore Ekman transport on a coast facing the trade winds or westerlies, typically western continental margins at 10–35° paleolatitude, or equatorial divergence) or high epeiric productivity. Requires low clastic `sedFlux`. Probability peaks during transgressions (condensed sections at maximum flooding) and during global phosphogenic-episode weights. Output: 0.5–30 m phosphatic beds (pelletal, nodular) over 10^3–10^5 km², commonly with chert or porcelanite (diatoms) and organic-rich shale. Reworked hardgrounds give higher-grade lag beds.
- Glauconite: any marine shelf at 50–500 m with low `sedFlux` during transgressive and maximum flooding intervals and a reactive-Fe supply. Output: greensand beds 0.5–10 m. It is a stratigraphic marker that the column can store.
- **Block mapping:** no phosphate in TFC or vanilla, so "no mapping". As a possible gameplay hook, TFC fertilizer phosphorus normally comes from bone meal, but a custom "phosphorite" block could supply it (TFC fertilizer mechanics not verified this session). Glauconite has no mapping; it could be a green sandstone decorative block or a potash fertilizer analogue.

### Gaps
- Föllmi's (1996) "condensation and winnowing" model text and quantitative upwelling thresholds were not retrieved.
- Phosphate deposit sizes (e.g. Moroccan Ouled Abdoun, Phosphoria) were not retrieved.

---

## Q16. Seafloor Fe-Mn nodules and crusts

### Takeaway
Deep-ocean Fe-Mn oxides grow extremely slowly. Hydrogenetic crusts on seamounts grow at about 1–5 mm/Myr and are Co-rich; abyssal nodules (e.g. the Clarion-Clipperton Zone) are mixed hydrogenetic and diagenetic and rich in Ni and Cu. They need oxic bottom water, very low sedimentation rates and long exposure (Myr). They are almost never preserved on land because oceanic crust is subducted. They are a current resource (deep-sea mining), not a geological-record ore.

### Cited Findings
- Pacific Co-rich crusts grow at 2.5–5 mm/Myr and average 2–4 cm thick, with Co up to 2.5 wt%, Pt up to 1.3 ppm and Ni up to 1.0%. — [Hein et al. chapter (ResearchGate PDF)](https://researchgate.net/profile/James_Hein/publication/264383178_Cobalt-rich_ferromanganese_crusts_from_the_Exclusive_Economic_Zone_of_the_United_States_and_nodules_from_the_oceanic_Pacific/links/53daa39a0cf2a19eee8ab9ed.pdf). NW Pacific crusts grow at 2.1–5.0 mm/Ma — [JpGU 2015](https://www2.jpgu.org/meeting/2015/PDF2015/M-IS46_O_e.pdf). Crust growth is typically about 1–5 mm/Myr — [Frontiers Mar. Sci. 2024](https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2024.1489184/pdf)
- Eastern CCZ nodules average about 1.16% Cu, 1.15% Ni, 0.15% Co and 0.14% Zn, higher than in other basins ([MDPI Minerals 11(8):868](https://www.mdpi.com/2075-163X/11/8/868)). UK licence-area nodules: Mn 29%, Ni 1.3%, Cu 1.0%, Co 0.17% ([Plymouth thesis](https://researchportal.plymouth.ac.uk/en/studentTheses/rare-metal-resources-in-polymetallic-nodules-from-the-eastern-equ/)). Abundance reaches up to 30 kg/m² (Rühlemann et al. 2010) — [Polish Geol. Quarterly](https://gq.pgi.gov.pl/article/view/33049)
- Hydrogenetic layers have Mn/Fe < 3, Ni+Cu < 1.5% and Co about 0.4%. Diagenetic layers have Mn/Fe > 10, Ni+Cu about 4% and Co about 0.1%. Bulk CCZ Mn/Fe is about 5.3, a mixed origin. Eastern CCZ composition varies with distance from the equatorial high-productivity zone. — [Polish Geol. Quarterly](https://gq.pgi.gov.pl/article/view/33049); [Frontiers 2024](https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2024.1489184/pdf)
- USGS overview: ferromanganese crusts and nodules are "rocks that grow". — [USGS](https://www.usgs.gov/publications/ferromanganese-crusts-and-nodules-rocks-grow)

### Inferences
**Proposed simulation rule:**
- Gate: `oceanState = OXIC` (Phanerozoic, most robust after about 0.5 Ga). Crusts form on seamount and guyot flanks (from the plume or hotspot module) at 800–2,500 m depth in the oxygen-minimum zone (background, not verified), with no sediment cover, over tens of Myr. Thickness is about 1–5 mm/Myr × exposure age (max about 25 cm). Nodules form on abyssal plains with sedimentation below about 1 cm/kyr (red clay or siliceous ooze), below the CCD, under moderate surface productivity (between the equatorial high-productivity belt and the gyres). Abundance is 0–30 kg/m².
- Preservation: nodules and crusts are almost always subducted. Allow survival only in ophiolites or accreted seamounts (very rare). In gameplay terms they live on the modern ocean floor of the final world.
- **Block mapping:** no mapping in TFC or vanilla. Possible decorative "manganese nodule" on the ocean floor if the mod renders deep-ocean floors.

### Gaps
- CCZ nodule growth rates (background, not verified: about 2–20 mm/Myr) and crust depth windows were not confirmed this session.

---

## Q17. Industrial sediments: bentonite, diatomite, kaolin, limestone, chalk, dolomite, glass sand

### Takeaway
These are rock-type outcomes of the stratigraphic simulator rather than "ores". Bentonite needs volcanic ash falls into marine or alkaline-lake water, with later diagenetic alteration of glass to smectite. Diatomite needs diatom blooms (most commercial deposits are Miocene) in marine upwelling basins or volcanic-terrain lakes. Carbonates (limestone, chalk, dolomite) and quartz-rich "glass sands" are facies of shelves and mature sand sheets. Of these, TFC already models limestone, chalk, dolomite and kaolin as blocks or rock types.

### Cited Findings
- Wyoming bentonite is montmorillonite clay formed from volcanic ash altered in seawater, deposited in the Upper Cretaceous seaway about 94–98 Ma (Mowry Shale). Wyoming and "western" Cretaceous bentonites are the world's principal source of high-swelling Na-bentonite. — [WSGS 2014 bentonite summary](https://www.wsgs.wyo.gov/products/wsgs-2014-bentonite-summary.pdf); [NSW bentonite review](https://www.resources.nsw.gov.au/sites/default/files/2022-11/bentonite.pdf)
- Most bentonites form under marine conditions, less commonly in alkaline lakes. Up to 40–50 wt% of amorphous silica is leached during alteration, and Wyoming bentonites altered after burial by diagenetic seawater pore fluids. — [NSW bentonite](https://www.resources.nsw.gov.au/sites/default/files/2022-11/bentonite.pdf); [Smectite (Wikipedia, aggregator)](https://en.wikipedia.org/wiki/Smectite)
- Most commercial diatomites, both marine and lacustrine, were deposited in the Miocene. Marine deposits are of higher quality and larger, while most known diatomites are lacustrine. Commercial marine diatomites accumulated on continental margins with submerged coastal basins under nutrient-rich upwelling. — [USGS Bull. 2209-D](https://pubs.usgs.gov/publication/b2209D)
- Monterey Fm diatomites (California) are marine. Sea-level rise plus tectonic deepening of the transform margin started deposition abruptly at 18–16 Ma. — [USGS Monterey](https://www.usgs.gov/publications/a-refined-assessment-paleoceanographic-and-tectonic-influences-deposition-monterey)
- TFC: kaolinite (kaolin clay) spawns in plateaus, old mountains, rolling hills and highlands, requiring at least 18 °C and 300 mm rainfall. TFC sedimentary rock types (older wiki list) include chalk, chert, claystone, conglomerate, dolomite, limestone and shale (plus rock salt in old versions). — [TFC Field Guide](https://terrafirmacraft.github.io/Field-Guide/en_us/the_world/ores_and_minerals.html); [TFC wiki (old revision)](https://tfcraft.fandom.com/wiki/Limonite?oldid=6570)

### Inferences
**Proposed simulation rules:**
- Bentonite: when `ashFlux` (from explosive arc or caldera volcanism upwind; layer thickness decaying with distance over 10^2–10^3 km) lands on a marine shelf, a basin or an alkaline lake, record an ash bed (centimetres to metres). After burial plus time (or immediately in alkaline lakes), convert it to bentonite. Thick, pure beds need a quiet, low-sedFlux basin (foreland seaway) downwind of an arc (Cretaceous Western Interior analogue).
- Diatomite: gate `diatoms` (Cretaceous onward; peak Miocene). Marine type: upwelling-margin silled basins (as for phosphorite, often co-occurring). Lacustrine type: lakes in volcanic terrains (Si supply from ash) with low clastic input.
- Carbonates: limestone forms on warm, low-clastic shelves within about 30° of the equator (reefs after the Cambrian; microbial before). Chalk requires calcareous nannoplankton (Jurassic onward; peak Late Cretaceous) in deep, clastic-starved epeiric seas during high sea level. Dolomite is favoured in arid-belt sabkhas and evaporitic platforms (Precambrian dolomite abundant) and in MVT host-rock alteration.
- Glass sand: quartz arenite of more than about 95% quartz, from multi-cycle reworking of cratonic sands (low-relief stable cratons, long transport, beach or eolian reworking, e.g. Ordovician-type sheet sands). Use a sediment "maturity" index that increases with recycling count and transport distance.
- **Block mapping:** limestone, chalk, dolomite, shale, claystone and chert are native TFC rock types. Kaolin maps to TFC `kaolinite` / kaolin clay. Bentonite and diatomite have no mapping; they could map to TFC clay variants or custom blocks. Glass sand maps to TFC sand (white sand color). Note that TFC glassmaking uses sand plus a flux (lime, potash or soda), not verified this session.

### Gaps
- Not researched: sedimentary kaolin (e.g. Georgia kaolin belt), chalk deposits (English Chalk), quartz-sand deposits (St. Peter Sandstone) and sources for carbonate latitude belts. Diatom first appearance and radiation dates were not retrieved (background, not verified: Jurassic or Early Cretaceous origin, major Cenozoic radiation).

---

## Q18. How does each deposit map to TFC and vanilla blocks, and what does TFC (1.21.1, v4.x) do by default?

### Takeaway
TFC already provides blocks for most sedimentary commodities: hematite, magnetite, limonite, malachite, sphalerite, native gold and cassiterite river deposits, halite, gypsum, sylvite, borax, saltpeter, sulfur, kaolinite and pyrite. Its default host-rock rules are simplified, and some contradict real geology (e.g. hematite only in extrusive igneous; sulfur only near lava; magnetite in "any sedimentary"). TFC has no Pb, Mn, U, Ti or Zr, phosphate or lithium, so those deposit types are either "no mapping" or need custom blocks. A TFC version for Minecraft 1.21.1 (4.x, NeoForge) exists, and the latest seen is 4.2.7 (Aug 2026).

### Cited Findings
- TerraFirmaCraft 1.21.1-4.2.7 was released on CurseForge on 3 Aug 2026. Its changelog includes removing tool-tier requirements for mining TFC ores. 4.2.5 is dated 3 Jul 2026. — [CurseForge TFC files](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft/files/8566530)
- TFC Field Guide default ore placement:
  - Native copper: igneous extrusive, above y=40, plus river deposits.
  - Native gold: igneous extrusive and intrusive, below y=70, plus river deposits.
  - Native silver: granite and diorite (uplift) or granite, diorite, schist and gneiss (deep), plus river deposits.
  - Tetrahedrite: metamorphic, any elevation.
  - Malachite: marble, limestone, chalk, dolomite.
  - Cassiterite: igneous intrusive dikes above y=80, plus river deposits.
  - Bismuthinite: sedimentary near the surface, igneous intrusive deep.
  - Garnierite: gabbro.
  - Hematite: any igneous extrusive, large veins near the surface.
  - Magnetite: any sedimentary, large veins near the surface.
  - Limonite: any sedimentary, large veins near the surface.
  - Sphalerite: igneous extrusive (small) and intrusive (large).
  - Lignite and bituminous coal: sedimentary, very large flat deposits near the surface.
  - Saltpeter: sedimentary, very large flat deposits.
  - Sylvite: shale, claystone, chert, very large flat deposits.
  - Borax: claystone, limestone, shale, very large flat deposits.
  - Gypsum and halite: sedimentary, very large flat deposits.
  - Sulfur: metamorphic and igneous intrusive near lava level.
  - Kaolinite: highlands with ≥18 °C and ≥300 mm rainfall.
  - Graphite: gneiss, marble, quartzite, schist.
  - Lapis: limestone and marble, y −20 to 80.
  - Amethyst: sedimentary and metamorphic beneath rivers, plus geodes.
  - Opal: sedimentary and extrusive beneath rivers.
  - Pyrite: igneous.
  - Diamond: kimberlite pipes in gabbro.
  - Sapphire: hot-spring fissures.
  - Ruby: marble.
  - Topaz: volcano fissures.
  - Emerald: igneous intrusive and hot springs.
  — [TFC Field Guide: Ores and Minerals](https://terrafirmacraft.github.io/Field-Guide/en_us/the_world/ores_and_minerals.html)
- TFC small surface ores (12 types) indicate veins below. Hematite, limonite and magnetite smelt to cast iron, and malachite and tetrahedrite to copper. — [TerraFirmaGreg field guide (TFC-derived)](https://wiki.terrafirmagreg.team/modern/field-guide/de_de/getting_started/finding_ores.html)

### Inferences
Mapping table proposal (sedimentary deposit to TFC block, with the vanilla fallback):

| Deposit type | Real ore minerals | TFC block(s) | Vanilla | Notes |
|---|---|---|---|---|
| Superior/Algoma BIF | hematite, magnetite | `hematite` (unmetamorphosed, supergene), `magnetite` (metamorphosed) | iron_ore | Override TFC's "hematite = extrusive" rule; giant flat bodies |
| Rapitan IF | hematite-jasper | `hematite` | iron_ore | Rift basin with diamictite |
| Oolitic ironstone | goethite, berthierine, hematite, chamosite | `limonite` (Minette), `hematite` (Clinton, >80 °C) | iron_ore | 2–5 m beds |
| Bog iron / goethite caps | goethite | `limonite` | iron_ore | (weathering researcher) |
| Sedimentary Mn | braunite, pyrolusite, rhodochrosite | none (custom) | none | KMF, Groote Eylandt |
| CD (SEDEX) Zn-Pb | sphalerite, galena, pyrite | `sphalerite`, `pyrite` gem; Ag via `tetrahedrite` | none | Galena has no mapping (no Pb in TFC) |
| MVT Zn-Pb | sphalerite, galena, fluorite, barite | `sphalerite` | none | Carbonate host (TFC limestone, dolomite) |
| Sed-hosted Cu (Kupferschiefer, Copperbelt) | chalcocite, bornite, chalcopyrite, carrollite | `tetrahedrite` (sulfide zone), `malachite` (oxidized), minor `native_copper` | copper_ore | Override host rules (shale, dolomite) |
| Red-bed Cu | chalcocite, malachite, native Cu | `malachite`, `native_copper` | copper_ore | Small, many |
| Unconformity, roll-front, calcrete U | uraninite, coffinite, carnotite | none (custom) | none | |
| Witwatersrand Au-U | gold, pyrite, uraninite | `native_gold` (in conglomerate), `pyrite` | gold_ore | Pre-GOE only |
| Elliot Lake U paleoplacer | uraninite, brannerite, pyrite | `pyrite` (U has no mapping) | none | Pre-GOE only |
| Placer gold | gold | TFC native gold river deposit (gravel) | gold_ore / raw_gold drops | Trap sites |
| Placer tin | cassiterite | TFC cassiterite river deposit | none | Within about 8 km of granite |
| Heavy-mineral sands | ilmenite, rutile, zircon, monazite | none (`magnetite` small ores for black sand) | none | |
| Diamond placers | diamond | `diamond` gem in gravel deposits | diamond_ore | Far-travelled means gem-rich |
| Gem gravels | sapphire, ruby, topaz, garnet | `sapphire`, `ruby`, `topaz` gems | (emerald n/a) | Basalt or metamorphic sources |
| Marine evaporites | halite, gypsum/anhydrite, sylvite/carnallite | `halite`, `gypsum`, `sylvite` | none | TFC "very large flat" fits |
| Borates | borax, ulexite, colemanite | `borax` | none | Closed basin with volcanism |
| Nitrates | nitratine (NaNO3) | `saltpeter` | none | Hyperarid only |
| Li brines / salars | Li in brine, halite crust | `halite` (crust); Li none | none | |
| Caprock sulfur | native S | `sulfur` | none | Add sedimentary occurrence |
| Phosphorite | carbonate-fluorapatite | none | none | |
| Glauconite | glauconite | none | none | |
| Fe-Mn nodules and crusts | Mn-Fe oxides | none | none | Modern sea floor |
| Bentonite, diatomite | smectite, opal-A | none (clay variants) | (clay) | |
| Kaolin (sedimentary) | kaolinite | `kaolinite` / kaolin clay | none | |
| Limestone, chalk, dolomite | calcite, dolomite | TFC rock types | (stone) | Facies output |
| Black shale (with all of the above) | pyrite | `pyrite` gem | none | Very common |
| Emerald, lapis, amethyst, cryolite, cinnabar, graphite, garnierite, bismuthinite, coal | — | — | — | Not sedimentary-deposit types in scope (other researchers). TFC's "bismuthinite in sedimentary near surface" has no clear sedimentary analogue |

Relative global importance (inferred ranking from the cited shares):
1. BIF-derived iron (dominant Fe source).
2. Sediment-hosted Zn-Pb, with CD as the most important Zn-Pb source and MVT second.
3. Sediment-hosted Cu (about 23% of Cu, plus most of the world's Co).
4. Witwatersrand (about 40% of all gold ever mined).
5. Kalahari Mn (about 70–77% of land Mn resources).
6. Marine evaporites (all halite and potash).
7. Phosphorites (essentially all phosphate; South Tethyan ≥50%).
8. HMS (main Ti and Zr source).
9. Tin placers (about 80% of identified tin resources).
10. Unconformity U (highest grade).
11. Li brines (major Li source).
12. Borates (Turkey dominant).
13. Minor types: diamonds, gems, nitrates, calcrete U, caprock S, glauconite, nodules.

### Gaps
- The TFC Field Guide page fetched may describe the 1.20.1 (3.x) behaviour. Ore and vein configs for 4.x (1.21.1) were not inspected. The mod should read TFC's actual 1.21.1 data (worldgen `configured_feature` vein JSONs and the `tfc:deposit/*` blocks) before finalising.
- The TFC "deposit" block list (panning gravel) for 1.21.1 was not verified in source code. Gem gravel deposits (diamond, sapphire) are likely not in default TFC.
- TFC fertilizer chemistry (phosphorus source) was not verified.
