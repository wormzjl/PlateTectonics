# Weathering/Supergene/Residual, Metamorphic, Gem and Industrial-Mineral Deposits: Catalog for Simulation Rules (with TFC mapping)

Scope notes for the report writer:
- Each "##" section below is one deposit family. It answers the assignment's key questions for that family: commodities and minerals, setting, host rock and P–T, time to form and preservation, geometry and grade, global abundance, a proposed simulation rule, and the Minecraft/TFC block mapping.
- Proposed simulation rules, rarity tiers and block mappings are design inferences. They appear under "Inferences". Items tagged **[background, unverified this session]** are standard textbook knowledge that I did not re-verify with a source in this session. Treat them as lower confidence.
- Simulation field names used in the rules: `climate(t)` (mean annual temperature MAT, mean annual precipitation MAP, seasonality), `slope`/`relief`, `erosionRate(t)`, `depositionRate(t)`, `surfaceExposure(t)` (is the layer at the surface), `waterTableDepth`, `column[]` (layer lithology, age, peak P/T, metamorphic grade, burial history), `exhumation(t)`, `glaciated(t)`, and tectonic event tags (`subduction`, `HP-LT mélange`, `collision`, `ophiolite obduction`, `rift`, `plume/LIP`, `arc magmatism`, `granite intrusion (type)`, `evaporite basin`, `euxinic basin`, `coal swamp`), plus `lifeTimeline` (land plants, diatoms) and `atmosphereO2(t)`.
- TFC facts come from the TerraFirmaCraft GitHub `1.21.x` branch (last commit 2026-10-14). This is the development head. It may differ from the released TFC jar for 1.21.1, so check it against the version you ship with.

---

## Cross-cutting: How fast do lateritic/weathering profiles form, how old are they, and what destroys them?

### Takeaway
Saprolite/laterite profiles thicken at roughly 2–40 m per Myr. A 10–60 m profile therefore needs about 1–10+ Myr of tectonic and climatic stability. Dated profiles record several weathering episodes over tens of Myr. Supergene profiles survive only where erosion is slower than weathering, or where they were buried or dried out (desiccation).

### Cited Findings
- A laterite-formation review says a saprolite horizon is "assumed to differentiate at rates ranging from 4 to 40 m Myr−1". It also says estimated weathering rates can vary by a factor of ten — [Comptes Rendus Géoscience review](https://comptes-rendus.academie-sciences.fr/geoscience/item/10.1016/j.crte.2003.10.008.pdf)
- At Nsimi (South Cameroon, humid tropical lateritic terrain), the contemporary saprolite production rate is about 2 mm/kyr. The authors estimate the long-term rate at 2–10 m/Myr — [Nsimi weathering-flux study (HAL)](https://hal-univ-tlse3.archives-ouvertes.fr/hal-00806642)
- In humid crystalline terrain in Sri Lanka, silicate weathering rates are 16–36 t km⁻² yr⁻¹, which is a weathering-front advance of 6–14 mm/kyr (≈6–14 m/Myr). This was measured with cosmogenic nuclides. The site is not a laterite — [GFZ Potsdam](https://gfzpublic.gfz-potsdam.de/pubman/item/item_247122_2/component/file_247121/20632.pdf)
- Goethite (U-Th)/He ages from lateritic duricrusts on the Brownsberg plateau (Suriname) range from <0.8 Ma to ~19 Ma. Fe-oxides in downslope boulders give ages up to 36 Ma, which the authors link to an erosion episode at ~30–20 Ma — [Frontiers in Earth Science 2022](https://www.frontiersin.org/journals/earth-science/articles/10.3389/feart.2022.888993/full)
- Guiana Shield goethite ages range from 0.5 to 20 Ma, with a formation peak around 3–5 Ma — [Ansart et al., EGU2020](https://presentations.copernicus.org/EGU2020/EGU2020-9521_presentation.pdf)
- Most Ni-laterite deposits form in several phases as climate or topography changes. The highest grades (>3 wt% Ni) form where uplift lets Ni leach downward from the oxide zone into the saprolite — [Butt & Cluzel 2013, Elements 9:123–128](https://elementsmagazine.org/author/dominique-cluzel)
- Supergene copper profiles: mature profiles need a few Myr of sustained activity (minima 3–9 Myr). High denudation rates leave profiles immature. Fossil profiles survive where erosion was slow, where they were buried under volcanosedimentary cover, or where the climate dried out (northern Chile) — [Sillitoe 2005 GSA abstract](https://gsa.confex.com/gsa/2005AM/webprogram/Paper91736.html)

### Inferences
- Simulation core ("weathering integrator", used by all weathering deposits below): for each cell and each time step where the top layer is exposed, add `dR = k(MAT, MAP) * dt` (regolith thickness) and subtract `erosionRate * dt`. Set `k` to 2–40 m/Myr, scaled up for hot and wet climates. Track `R`, the parent rock under the regolith, and a "maturity" integral (time spent under tropical-humid conditions with low slope). This one state variable drives bauxite, Ni laterite, Fe/Mn caps, REE clays, kaolin, residual Au and supergene Cu.
- Preservation rules: (a) any `glaciated(t)` step strips the regolith (set R≈0). **[background, unverified this session]** Laterites and bauxites are essentially absent from recently glaciated shields such as Canada and Scandinavia. (b) If `erosionRate > k` the profile cannot accumulate. (c) Burial by deposition freezes the profile as a "fossil laterite" or karst bauxite inside the column. (d) A later shift to arid conditions preserves the profile (desiccation) instead of destroying it.
- Time gating: a profile ≥10 m needs `maturity` ≥ ~1 Myr at the top rate, and ~5 Myr or more at typical rates. Tens-of-metres bauxite or Ni-laterite profiles should require ≥3–10 Myr of cumulative favorable time. That cumulative time can be split across several episodes, matching the multi-episode goethite ages.
- **[background, unverified this session]** Full lateritic Fe retention (ferricretes, Fe caps) needs an oxygenated atmosphere. Gate Fe-cap formation on `atmosphereO2(t)` past the Great Oxidation Event (~2.4 Ga). Pre-GOE paleosols lose Fe.

### Gaps
- I did not retrieve Freyssinet et al. (2005, Econ. Geol. 100th Anniv. Vol.) directly, so its quantitative climate thresholds (MAP, MAT) for lateritization are not cited here. A frequently used rule of thumb is MAP >~1,200 mm and MAT >~20–22 °C, which is **[background, unverified this session]**.
- I found no study that gives one weathering-front advance rate for a dated laterite. The rates above come from different methods.

---

## Ni–Co laterites (garnierite) on ultramafic/ophiolite rocks

### Takeaway
Ni–Co laterites form by long, intense tropical weathering of serpentinized peridotite (usually obducted ophiolite) on uplifted, well-drained land. They are near-surface blankets a few to tens of metres thick, grading about 1–2.5% Ni. They are now the dominant source of new nickel. In TFC, garnierite currently generates deep in igneous-intrusive rock, which is geologically backwards.

### Cited Findings
- Weathered serpentinites supply most new nickel. Ni grades exceed 1.0 wt% and the Ni sits in secondary oxides, hydrous Mg silicates and smectites. Rock type, tectonics, climate and landscape control the deposits. The highest-grade ores (>3 wt% Ni) form when Ni leaches downward from uplifted oxide-rich soil and concentrates in new silicates in the saprolite — [Butt & Cluzel 2013](https://elementsmagazine.org/author/dominique-cluzel); [abstract mirror](https://synapsesocial.com/papers/W2077193584)
- Profile from top to bottom: limonite zone (high Fe; Co concentrated in Mn-oxyhydroxides), saprolite zone (Ni enriched), then bedrock — [Sorowako resource-modelling study](https://www.academia.edu/77451999/Nickel_Laterite_Resources_Modeling_Using_Geological_Domain_Algorithm)
- Three ore types by Ni host: oxide (Ni in goethite and Mn-oxides such as lithiophorite–asbolane; mean grades 1.0–1.6 wt% Ni), clay silicate (Ni-smectites), and hydrous Mg silicate (Mg–Ni phyllosilicates including "garnierite"; highest grades, 1.8–2.5 wt% Ni). The hydrous Mg silicate type has a thick serpentine-dominated saprolite under a thin limonite horizon. The major deposits lie within about ±26° latitude (New Caledonia, Indonesia, Philippines, Cuba, Dominican Republic, Venezuela, Colombia, Brazil). At Loma de Hierro the Co-bearing Mn-oxyhydroxides carry 2–13 wt% CoO. Sc is highest in the limonite (40–68 ppm). A thick saprolite is tied to continuous uplift and a low water table — [Domènech et al., Bol. Soc. Geol. Mex.](https://boletinsgm.igeolcu.unam.mx/bsgm/index.php/volumenes-volumes/cuarta-epoca/375-sitio/articulos/cuarta-epoca/7203/2280-7203-15-a050620_Domenech)
- At Sorowako, saprolite grade rises with saprolite thickness, and slope controls limonite thickness — [Sorowako study](https://www.academia.edu/77451999/Nickel_Laterite_Resources_Modeling_Using_Geological_Domain_Algorithm)
- Example drill intercepts (PNG region, company report, not peer reviewed): limonite 8.10 m at 1.08% Ni and 0.11% Co; saprolite 5.25 m at 1.36% Ni and 0.04% Co — [Corcel drilling report](https://www.investegate.co.uk/announcement/rns/corcel--crcl/drilling-report/1952212)
- USGS grade-tonnage model: deposits range from 2.5 to ~400 Mt. Ni grade 0.66–2.4% (median 1.3%). Co grade 0.01–0.15% (median 0.08%) — [Marsh, Anderson & Gray 2013, USGS SIR 2010-5070-H](https://pubs.usgs.gov/sir/2010/5070/h/)
- Global abundance: world mine Ni production in 2024 was ~3.7 Mt, of which Indonesia produced ~2.2 Mt and the Philippines ~330 kt (both mostly laterite producers). World reserves are >130 Mt Ni — [USGS Mineral Commodity Summaries 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- TFC today: `normal_garnierite` has rarity 25, size 18, y −80 to 0, density 0.3, rocks `igneous_intrusive`. `gabbro_garnierite` has rarity 20, size 30, y −80 to 0, density 0.6, rocks `gabbro`, grade RICH. Both are deep-only — [TFC constants.py (1.21.x)](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Simulation rule (Ni laterite):
  - Inputs: parent layer = ultramafic (peridotite, dunite, or serpentinite, usually from an `ophiolite obduction` event or an exhumed mantle slice); weathering-integrator maturity under tropical humid climate; moderate relief with active gentle uplift (lets the water table fall, which favors the hydrous Mg silicate/garnierite type); no later glaciation; erosion < weathering rate.
  - Output: a blanket at 0 to ~R m depth. The top ~1/3 is limonite (Fe-oxide; Co, Mn, Sc). The lower part is saprolite (garnierite, Ni 1.5–2.5%). Choose oxide-type versus hydrous-silicate-type from the uplift rate and water-table depth. Low uplift and a high water table give clay/oxide types. Higher uplift and a deep water table give garnierite.
  - Spatial pattern: patchy lenses along the ophiolite belt, thickest on gentle spurs and plateaus, thin on steep slopes.
  - Rarity tier: regional/common wherever obducted ophiolites sat in the tropics. Absent elsewhere.
- Block mapping: saprolite ore → TFC `garnierite`, as poor/normal/rich grades by Ni% (TFC grade weights: POOR 70/25/5, NORMAL 35/40/25, RICH 15/25/60). Limonite cap → TFC `limonite` (goethite). Vanilla has no Ni ore; use iron ore for the cap if running vanilla.
- TFC has no peridotite/serpentinite rock. The closest host stone for the ore block is `gabbro` (the ore textures are per-rock), or `basalt` for altered ophiolite. Recommend placing garnierite near the surface (projected to the top of the column), not at y −80..0.

### Gaps
- I did not find a sourced global age distribution of Ni laterites (most active ones are Cenozoic, **[background, unverified this session]**) or typical total profile thickness from Butt & Cluzel's full text. The full text is behind a membership wall.

---

## Bauxite: lateritic (on aluminosilicate rocks) and karst (on carbonates)

### Takeaway
Lateritic bauxite is the gibbsite-rich top of mature tropical laterite profiles on stable plateaus, typically 3–10 m thick at 44–55% Al₂O₃. Karst bauxite fills depressions on emergent carbonate platforms, and its age is bracketed by unconformities. Bauxite is a large-tonnage commodity (450 Mt/yr), so it is regionally common but strongly climate- and stability-gated.

### Cited Findings
- World bauxite mine production in 2024 was ~450 Mt (438 Mt in 2023). Guinea produced 130 Mt, Australia 100 Mt, China 93 Mt, Brazil 33 Mt, Indonesia 32 Mt and India 25 Mt. World reserves are ~29 Gt, including Guinea 7.4 Gt and Australia 3.5 Gt — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- USGS karst bauxite model (38c): weathered residual and transported material on carbonate rocks. It needs intense weathering, mainly in wet tropical areas. Deposits concentrate in depressions on karst surfaces. Quaternary tropical deposits are mainly gibbsite, and boehmite becomes more common in older Mesozoic and Paleozoic deposits — [USGS Bulletin 1693, Model 38c](https://pubs.usgs.gov/bul/b1693/html/bull20dh.htm)
- Guinea (Boké): the average bauxite zone is ~8–10 m thick (another article from the same source says 6–8 m), with thin soil and an Fe-rich cap ~1–2 m thick. Ore is low silica (2.5%) and medium alumina (44–46%) gibbsitic bauxite — [AlCircle industry blog](https://www.alcircle.com/blog/booming-bauxite-mining-industry-guinea-future-prospects)
- Weipa (Australia): at Andoom a ~30 m profile overlies Cretaceous Rolling Downs Group sediments. The key transformation happens in a transition zone ~1 m thick, with >75% gibbsite at the top and ~80% kaolinite at the base — [Eggleton et al. 2008 (ANU)](https://openresearch-repository.anu.edu.au/items/4c0f4a61-e98a-40c9-985c-8772d4eab172/full). Weipa grades are quoted as ~49–55% Al₂O3 with ~9% Fe₂O₃ (non-peer-reviewed letter) — [Stabroek News letter](https://www.stabroeknews.com/2019/02/15/opinion/letters/why-the-bonasika-bauxite-is-the-purest/)
- Gove (NT, Australia): the bauxite zone is historically up to 10 m thick but more typically 3–4 m — [NTGS AGES 2020](https://geoscience.nt.gov.au/gemis/ntgsjspui/bitstream/1/90016/3/GRogersAGES2020_paper.pdf). Darling Range (WA) ore runs ~27–30% Al₂O₃, described as the world's lowest-grade commercially mined bauxite — [geologyforinvestors.com](https://geologyforinvestors.com/?p=479)
- Many circum-Mediterranean bauxites (including Italian ones) are Cretaceous karst bauxites formed on exhumed carbonates — [Periodico di Mineralogia](https://rosa.uniroma1.it/rosa04/periodico_di_mineralogia/article/download/15766/15139/30100). In the External Dinarides, bauxites occur at several regional unconformities from Triassic to Neogene. The stable-platform ones are Middle–Late Triassic, Late Jurassic and Early Cretaceous. Late Cretaceous–Neogene ones reflect stronger tectonics — [Croatian bibliography record](https://www.bib.irb.hr:8443/10710)
- Bauxite usually lacks direct age markers. Its age is bracketed by the oldest overlying strata — [Cambridge core paper](https://www.cambridge.org/core/product/05DA4A3E94C2B2E984AADEA13F03DDFC/core-reader). Platform emergence under warm, wet conditions (e.g., mid-Cretaceous Sardinia) allowed bauxitization — [repository.geologyscience.ru](https://repository.geologyscience.ru/bitstreams/b8b2de21-a8f4-442f-b11d-2998c5737b5e/download)
- U.S. domestic bauxite came from Arkansas, where syenite was lateritized. This shows that parent rocks other than granite and basalt also work — [USGS search summary of 1984 review via OneMine](https://onemine.org/documents/bauxite-deposits-of-the-united-states)

### Inferences
- Simulation rule (lateritic bauxite):
  - Parent: any Al-rich, quartz-poor-to-moderate rock (basalt, syenite, granite/gneiss, shale or schist, arkose). It forms most readily on basalt or syenite, which have little quartz to dilute the Al **[background, unverified this session]**.
  - Requirements: higher maturity than Ni laterites (gibbsite needs desilication of the kaolinite), low relief on a plateau or high bench with good drainage (deep water table), and no glaciation. Output a ferricrete/Fe cap 1–2 m over a bauxite layer of 3–10 m (thicker with maturity), over kaolinitic mottled zone and saprolite.
  - Gibbsite versus boehmite flavour: set from deposit age plus burial history (younger and unburied → gibbsite).
- Simulation rule (karst bauxite): when a carbonate platform layer has a subaerial exposure interval (unconformity) under humid tropical climate lasting ≥~1 Myr, place bauxite pods in karst depressions on the unconformity surface. These preserve well when buried by the next transgression. Spatial pattern: discontinuous lenses and pockets along a stratigraphic horizon. This is the only bauxite type that can sit deep in the column.
- Rarity: regionally common in tropical belts (both active and fossil). Absent in present-day high latitudes unless fossil.
- Block mapping: no TFC or vanilla block (TFC has no aluminium). Options: no mapping; a custom block; or (cosmetic only) represent the lateritic cap with TFC `oxisol` soil plus TFC `limonite` (poor) in the Fe cap. The TFC 1.21.x branch defines an `oxisol` soil variant, which matches tropical laterite soils — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Gaps
- No sourced global age histogram of bauxites (Bárdossy & Aleva 1990 and Bárdossy 1982 were not retrieved). No sourced MAP/MAT thresholds.

---

## Lateritic Fe caps (ferricrete) and supergene Mn caps

### Takeaway
Fe-oxide caps (ferricrete or duricrust; goethite/hematite "limonite") top most mature laterite profiles. Supergene Mn ores form by tropical weathering that upgrades Mn-bearing sedimentary rocks. Moanda (Gabon) has a 9–17 m profile with a 3–9 m ore zone at 100–500 Mt scale.

### Cited Findings
- In Guinea bauxite, the Fe-rich cap is ~1–2 m thick — [AlCircle](https://www.alcircle.com/blog/booming-bauxite-mining-industry-guinea-future-prospects). At East Weipa the bauxite sits above a ferricrete at ~4 m depth — [Eggleton et al. 2008](https://openresearch-repository.anu.edu.au/items/4c0f4a61-e98a-40c9-985c-8772d4eab172/full)
- The limonite zone at the top of Ni-laterite profiles is Fe-rich (goethite-dominated) — [Sorowako study](https://www.academia.edu/77451999/Nickel_Laterite_Resources_Modeling_Using_Geological_Domain_Algorithm)
- Regolith Mn deposits form above the water table under tropical weathering by intense leaching of silica and other components from low-grade Mn rocks. Mn is enriched by replacement — [NSW Mn summary](https://www.resources.nsw.gov.au/sites/default/files/2022-11/manganese.pdf)
- Moanda (Gabon): supergene "manganocrete" on 2.3–2.0 Ga Francevillian Mn-bearing black shale, on plateau remnants of a Cenozoic lateritized surface. The profile is ~9–17 m, with a 3–9 m economic "plaquette" zone. Deposits are large (~100–500 Mt) and shallow — [Manganese Deposits of Africa review](https://www.literature.wolkersdorfer.info/literature/Manganese%20Deposits%20of%20Africa.pdf)
- Kalahari Manganese Field: Ar–Ar ages of supergene ore peak at ~27.8, 10.1 and 5.2 Ma, with older ages higher in the profile. Supergene high-grade ore is ≤0.1% of the total resource, because the field is mostly primary sedimentary Mn — [Univ. Johannesburg record](https://pure.uj.ac.za/en/publications/timing-of-supergene-enrichment-of-low-grade-sedimentary-manganese/)
- Groote Eylandt: Mn in the upper 15 m of Early–Middle Cretaceous beds under lateritic Tertiary conglomerate, covering ~150 km² — [NSW Mn summary](https://www.resources.nsw.gov.au/sites/default/files/2022-11/manganese.pdf)
- World Mn ore production in 2024 was ~20 Mt (gross weight, as reported) — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Simulation rule (Fe cap): any lateritic profile with maturity above a threshold gets a 1–5 m Fe cap. It is thicker on Fe-rich parents (basalt, BIF, ultramafic) and gated on oxygenated atmosphere. Duricrust caps resist erosion and preserve mesas/plateaus, so they can feed back into geomorphology.
- Simulation rule (supergene Mn): parent layer = Mn-rich sediment (Mn-carbonate black shale or Mn-formation; these need a primary sedimentary Mn event, covered by other research notes) + weathering integrator → oxide Mn cap 3–10 m. Rare (few districts worldwide).
- Block mapping: Fe cap → TFC `limonite` (goethite = limonite in TFC's naming; poor/normal grade). Vanilla → iron ore. Mn → no TFC/vanilla block (no mapping).

### Gaps
- Supergene upgrading of banded iron formation to high-grade hematite/goethite ore (Hamersley, Carajás) was not researched here. It likely belongs with the BIF notes from another researcher. Ferricrete thickness statistics were not found.

---

## Supergene enrichment of porphyry copper, oxide Cu zones (malachite/azurite/chrysocolla/atacamite) and gossans

### Takeaway
Exhuming a pyrite-bearing porphyry Cu system into a semi-arid to seasonally wet climate with a falling water table produces a leached cap (limonite, gossan), an oxide zone (malachite, azurite, chrysocolla, atacamite) and a chalcocite enrichment blanket. Blankets reach up to hundreds of metres (Escondida up to 500 m). Mature profiles need ≥3–9 Myr. Preservation needs later aridity or burial. Supergene ores supply >50% of world mined copper.

### Cited Findings
- Supergene ores in the central Andes and the southwestern US account for >50% of world mined Cu. Favourable deposits are vertically extensive, with steep fault/fracture arrays, high pyrite/Cu-sulfide ratios, and non-reactive advanced argillic and sericitic alteration. Mature profiles need sustained activity (minima 3–9 Myr), uplift that lowers the water table, and hot semi-arid to pluvial climates, as long as erosion does not outpace supergene processes. Enrichment can extend several hundred metres deep. Planar erosion surfaces are not required. Preservation happens through slow erosion, burial under volcanosedimentary cover, or climatic desiccation — [Sillitoe 2005 GSA abstract](https://gsa.confex.com/gsa/2005AM/webprogram/Paper91736.html)
- Escondida: the enriched blanket covers ~4.5 × 1 km and is a few metres to 500 m thick, under a leached capping a few to 350 m thick. Most enrichment happened at 18.0–14.7 Ma, on a 38 Ma stock. Regional enrichment ran from ~44 Ma, peaked at 21–14 Ma, and ended near 9 Ma with hyperaridity — [Porter GeoConsultancy abstract](https://portergeo.com.au/publishing/superporphyry/Abstract078sp.asp); [Alpers & Brimhall, Escondida paleohydrology](https://literature.wolkersdorfer.info/literature/Paleohydrologic%20evolution%20and%20geochemical%20dynamics%20of%20cumulative%20supergene%20metal%20enrichment%20at%20La%20Escondida%20Atacama%20Desert%20northern%20Chile%20Available.pdf)
- Copper grade in the blanket rose with depth as the water table descended. This is evidence for cumulative downward enrichment — [Alpers & Brimhall 1989](https://literature.wolkersdorfer.info/literature/Paleohydrologic%20evolution%20and%20geochemical%20dynamics%20of%20cumulative%20supergene%20metal%20enrichment%20at%20La%20Escondida%20Atacama%20Desert%20northern%20Chile%20Available.pdf)
- Climate: enrichment needed recharge of ~8 cm/yr, i.e., rainfall 1–2 orders of magnitude above the present (<10 mm/yr). Clark et al. (1990) suggest >10 cm/yr — [Clark et al. 1990](https://pyrite.test.utah.edu/fieldtrips/SEGFperu-2012/Readings/Clark%201990.pdf)
- Supergene alunite ages show oxidation and enrichment in Chile was active from the early Oligocene to the middle Miocene (≥20 Myr). Supergene activity started no less than ~11 Myr after each porphyry formed, because the system first had to be unroofed — [USGS, Age of supergene oxidation and enrichment in the Chilean porphyry copper province](https://pubs.usgs.gov/publication/70018070)
- Reich et al. (2009) used U-series disequilibrium ages on atacamite to challenge the view that enrichment occurred only from the middle Eocene to the late Miocene. Some Cu-chloride oxide minerals formed much later. This is a flagged controversy over how much oxide-zone mineralogy is young — [Reich et al. 2009](https://repositorio.uchile.cl/bitstream/handle/2250/126164/Reich_Martin_Supergene.pdf)
- Rapid exhumation during the supergene stage leaves too little time to build mature blankets (attribution to this abstract is from a search summary and is uncertain) — [Porter GeoConsultancy abstract](https://portergeo.com.au/publishing/porphyry98/Abstract2p.asp)

### Inferences
- Simulation rule (supergene Cu):
  - Inputs: a porphyry Cu body (from arc magmatism, placed by the hypogene notes), with `exhumation` bringing its top to within ~0–500 m of the surface; ≥~10 Myr between intrusion and the start of supergene activity (unroofing time); `climate` semi-arid to seasonally wet (MAP roughly 100–1,000 mm, **[background, unverified this session]** for the upper bound); a falling water table (uplift or incision); erosion slower than water-table descent; ≥3 Myr cumulative favourable time.
  - Output profile from top down: leached cap/gossan (Fe-oxides, thickness 0–350 m) → oxide zone (malachite/azurite/chrysocolla/atacamite, typically tens of metres, **[background, unverified this session]**) → chalcocite enrichment blanket (up to hundreds of metres, Cu grade 1.5–3× hypogene, **[background, unverified this session]**) → hypogene.
  - Preservation: hyperaridity after formation or burial (e.g., under ignimbrites) freezes the profile. Continued humid erosion strips it.
- Gossans (any sulfide body): when an exhumed massive-sulfide, skarn, MVT or porphyry body reaches the weathering zone under oxygenated atmosphere, convert its top 5–50 m to a gossan (goethite/hematite boxwork, Pb/Ba/Au residues) **[background, unverified this session]**. Gossans are exploration indicators. They should generate surface "indicator" blocks or loose rocks above buried ore.
- Block mapping: oxide zone → TFC `malachite` (graded; rich in the oxide zone). Chalcocite blanket → no exact TFC block. Use TFC `malachite` rich or TFC `tetrahedrite` (gray Cu sulfide) rich as a stand-in, or vanilla copper ore. Gossan/leached cap → TFC `limonite` (poor) plus surface indicators. Native copper in the oxide-zone base → TFC `native_copper`.
- TFC today puts `malachite` in marble, limestone, chalk and dolomite (surface y 40–100, montane, and normal y −30..70). Real malachite is mostly a supergene product of Cu sulfides in any host, and is only locally carbonate-hosted (carbonate wall rock helps fix it). Recommend allowing malachite above any Cu sulfide body — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Gaps
- I found no sourced quantitative gossan model (thickness vs. sulfide content). Blanket thickness statistics across many deposits were not found beyond Escondida.

---

## Ion-adsorption (regolith-hosted) REE clays

### Takeaway
These are weathering crusts 30–60 m thick on REE-enriched granites and felsic volcanics in subtropical humid, low-hill terrain (<500 m). The REE sit 60–90% as clay-adsorbed ions in a 5–10 m enriched zone grading ~0.05–0.3 wt% REO. They are geographically concentrated in South China and SE Asia and are a key heavy-REE source.

### Cited Findings
- Parent rocks are felsic igneous rocks (granite, rhyolite, rhyolitic tuff), often REE-enriched granitoids. Climate is subtropical, warm and humid, with slightly acidic soils. The whole weathering crust is 30–60 m thick. The REE-enriched zone is 5–10 m thick at the B/C boundary. Grade is 0.05–0.3 wt% extractable REE. Deposits occur on hills <500 m elevation and are thicker on ridges, thinning downslope. Exchangeable REE make up 60–90% of the total. Districts: Jiangxi, Hunan, Guangdong, Fujian, Guangxi (Zudong, Datian = HREE; Heling, Dingnan = LREE). Regolith-hosted deposits give ~35% of China's REE output — [Wikipedia: Regolith-hosted REE deposits](https://en.wikipedia.org/wiki/Regolith-hosted_rare_earth_element_deposits)
- In Laos, ore bodies are several metres to >20 m thick, with ion-exchangeable REO of 100–1,760 ppm (average 400–700 ppm) — [Goldschmidt 2025 abstract](https://goldschmidtabstracts.info/2025/29351.pdf)
- Weathering crusts of granites, felsic volcanics, low-grade metamorphic rocks and even basic magmatic rocks can host such deposits — [search summary of HKU/China Geology literature](https://hub.hku.hk/handle/10722/262321)
- World REE mine production in 2024 was ~390 kt REO. China produced ~270 kt, Burma (Myanmar) ~31 kt, the US 45 kt, Australia, Nigeria and Thailand ~13 kt each — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Simulation rule: parent layer = granite (prefer late, fractionated or A-type granites, if the granite generator tags REE fertility) or felsic volcanic; climate subtropical humid (MAT ~18–25 °C, MAP >1,000 mm, **[background, unverified this session]**); low relief (hills <500 m above base level); weathering integrator R ≥ 20 m. Place a 5–10 m enriched horizon 1.5–10 m below the surface, thickest under ridge crests.
- Rarity: rare to regional. Concentrate in a few granite provinces in humid subtropics.
- Block mapping: no TFC or vanilla REE block (no mapping). Optionally render as TFC `kaolin` clay variants (white/pink), because the hosts are kaolinite/halloysite-rich clays.

### Gaps
- Typical age of the weathering (mostly Cenozoic) and deposit tonnages were not sourced. Myanmar's REE output is widely reported to be ion-adsorption clay, but this session's sources do not state that explicitly.

---

## Kaolin: residual, hydrothermal and sedimentary

### Takeaway
Kaolin forms (1) residually by weathering of feldspathic rocks, (2) hydrothermally at ~50–100 °C in granite cupolas (St Austell-type funnels narrowing downward), or (3) as reworked sedimentary lenses (Georgia: up to 20 m thick, km-scale). It is common (≈44 Mt/yr kaolin).

### Cited Findings
- Cornwall china clay forms in situ in granite by decomposition of feldspar — [Clay Minerals Bulletin, St Austell granite](https://resolve.cambridge.org/core/journals/clay-minerals-bulletin/article/geological-aspects-of-the-st-austell-granite/711C317E1DC2B91B9DAB7338111E0A61)
- Fluid inclusions in quartz linked to kaolin indicate a low-temperature hydrothermal event at ~50–100 °C, coincident with the oil-generation window — [Manning et al. (Newcastle eprints)](https://eprints.ncl.ac.uk/66722)
- Kaolinization follows fracture and vein systems, with funnel- or trough-shaped zones narrowing downward. Hydrothermal solutions are generally considered the principal agents, with possible tropical weathering during the Tertiary — [Bath Geological Society](https://bathgeolsoc.org.uk/journal/articles/1983/1983_China_Clay_SW_England.html)
- Kaolin deposits are classed as hydrothermal (granites, pegmatites), residual (weathered granite, gneiss, arkose) and sedimentary — [Spektrum Lexikon der Geowissenschaften](https://www.spektrum.de/lexikon/geowissenschaften/kaolinlagerstaetten/8055). Georgia/South Carolina sedimentary kaolin occurs as lenses and tabular layers up to 20 m thick, covering several km, in Upper Paleocene–Middle Eocene and uppermost Cretaceous sediments. These produce ~90% of US output — [SMEDG slide](https://smedg.org.au/skardonkaolin_files/v3_slide0005.htm)
- World kaolin production ~44 Mt (2024), bentonite ~21 Mt and fuller's earth ~14 Mt — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- TFC: `kaolin_disc` vein (rarity 50, y −7..1, size 18, height 6, projected to the surface). It is gated by climate `min_groundwater=300, min_temperature=18` and restricted to specific biomes (plateaus, highlands, old mountains, karst, shield volcanoes, rift valleys and others). The indicator plant is the blood lily — [TFC world_gen.py (1.21.x)](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py)

### Inferences
- Simulation rules:
  - Residual kaolin: any feldspathic parent (granite, gneiss, arkose) + weathering integrator under warm humid climate, at moderate maturity (lower than bauxite).
  - Hydrothermal kaolin: the roof zone of peraluminous (S-type/tin) granites, in downward-narrowing funnels along fractures.
  - Sedimentary kaolin: deposition layers sourced from kaolinized hinterland (fluvial/deltaic), as lenses.
- Rarity: common.
- Block mapping: TFC kaolin clay (red/pink/white variants) placed via a TFC-like disc. Vanilla `clay`. TFC's climate gate (MAT ≥18 and groundwater ≥300) is a reasonable proxy, but the simulation should allow fossil/relict kaolin outside the present climate.

### Gaps
- Temperatures of residual kaolinization were not sourced (ambient by definition).

---

## Residual/lateritic gold (and the placer link)

### Takeaway
Lateritic gold is gold redistributed by weathering above primary gold mineralization. It forms a "mushroom" with a depleted upper saprolite, a supergene saprolitic layer at ~10–30 m depth, and a lateritic pisolith halo. Deposits are small (<10 Mt) and low grade (1–5 g/t).

### Cited Findings
- The supergene zone is broadly mushroom-shaped with saprolitic and lateritic facies. Gold in pisoliths has undergone several stages of mobilization and reprecipitation, rather than simple residual concentration (a flagged controversy) — [CRC LEME Open File Report 218](https://crcleme.org.au/Pubs/OPEN%20FILE%20REPORTS/OFR%20218/OFR%20218.pdf)
- Deposits are mostly small (<10 Mt) and low grade (1–5 g/t) — [GSA 2012 abstract](https://gsa.confex.com/gsa/2012AM/webprogram/Paper209246.html)
- At Mount Joel (Yandal belt), the upper saprolite is strongly leached of Au and Si (kaolinite-rich depletion zone). Supergene ore sits at ~10–30 m, and the lateritic residuum is anomalous — [CRC LEME report summary](https://crcleme.org.au/Pubs/PubsAbstracts/report035.html)
- Secondary Au distribution is governed by mechanical eluviation and hydromorphic transport with organic, thiosulfate and chloride complexes. Later aridification in Western Australia redistributed secondary gold — [Rhodes University thesis](https://researchrepository.ru.ac.za/handle/123456789/5232)

### Inferences
- Simulation rule: where a primary Au deposit (orogenic, greenstone or epithermal, from the hypogene notes) intersects a mature weathering profile, add (a) a supergene Au blanket at 10–30 m depth, (b) a depleted zone above it, and (c) a broad low-grade pisolitic halo at the surface (indicator). Erosion of the profile feeds placers downslope and downstream.
- Block mapping: TFC `native_gold` (poor grade) in the blanket. TFC `deposit/native_gold/<rock>` (panable gravel) for placers. Vanilla gold ore.

### Gaps
- No global tonnage share for lateritic versus hypogene gold was found.

---

## Graphite (flake in high-grade metasediments; amorphous from metamorphosed coal; vein)

### Takeaway
Flake graphite forms by graphitization of organic carbon in metasedimentary belts (pelites, marbles, quartzites) at upper-amphibolite to granulite facies, typically in collisional orogens. Median grades are ~9% C for flake and ~40% C for amorphous (coal-derived). Production is dominated by China (~79%).

### Cited Findings
- Disseminated flake graphite is commonly hosted by marbles, paragneisses and quartzites. The most favourable hosts are alumina-rich paragneisses and marbles in upper-amphibolite or granulite terrains intruded by igneous rocks. The highest grades are at marble–paragneiss contacts, and the thickest zones are in fold crests — [Simandl & Kenan, BCGS Paper 1998-1 (via search summary)](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P1998-01-40_Simandl.pdf)
- Orogenic graphite deposits are commonly associated with upper amphibolite- to granulite-facies metasedimentary rocks in continent–continent or continent–arc collisional orogens. They form mainly by graphitization of organic carbon during regional metamorphism. Strain localization and partial melting of pelites help raise grade and flake size — [Mineralium Deposita 2025](https://link.springer.com/article/10.1007/s00126-025-01412-5)
- Bissett Creek (Grenville) has graphite at 2–10 vol%, a biogenic carbon source, and metamorphic fluid/melt remobilization producing extra-large flakes. Lac Knife is the metamorphosed equivalent of the Menihek black shales — [Mineralium Deposita 2022](https://link.springer.com/article/10.1007/s00126-022-01145-9)
- Deposit types: disseminated flake (median ~9% C, median 240,000 t), amorphous/microcrystalline (median ~40% C, median 130,000 t), and vein — [Sutphin & Bliss 1990, USGS](https://pubs.usgs.gov/publication/70016144)
- World natural graphite production in 2024 was ~1.6 Mt. China produced 1.27 Mt, Madagascar 89 kt, Mozambique 75 kt and Brazil 68 kt. Reserves are ~290 Mt — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- TFC: `graphite` vein has rarity 20, size 20, y −30..60, density 0.4, rocks gneiss, marble, quartzite, schist — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Simulation rule:
  - Flake graphite: a layer with protolith C_org-rich (black shale, organic carbonate, coal; this needs the life timeline to have produced organic burial, and Precambrian black shales qualify) + peak T ≥ ~650 °C (upper amphibolite–granulite) + later exhumation. Place stratabound lenses following the folded layer, preferring marble–gneiss contacts and fold hinges.
  - Amorphous graphite: a coal seam (needs land plants, Devonian+) metamorphosed at lower grade or by contact aureoles.
  - Vein graphite (Sri Lanka type): granulite terrains with CO₂-rich fluids **[background, unverified this session]**.
- Rarity: regional (common in old high-grade metasedimentary belts).
- Block mapping: TFC `graphite` ore in gneiss, schist, marble or quartzite (this matches TFC well). Vanilla has no graphite block; vanilla coal ore is a loose stand-in.

### Gaps
- The exact T threshold for "flake-size" graphite is not sourced. The ~650 °C above is an inference from "upper amphibolite".

---

## Aluminosilicates (kyanite/sillimanite/andalusite), industrial garnet, wollastonite

### Takeaway
Al₂SiO₅ polymorphs record P–T: the triple point is ~4.5 kbar/550 °C (Pattison) or ~3.8 kbar/500 °C (Holdaway). Andalusite marks low-P contact aureoles, kyanite Barrovian moderate-P, sillimanite high-T. Industrial garnet comes from metagabbro (Gore Mountain) or placers shed from garnet schist. Wollastonite forms in contact aureoles of siliceous limestone at ≥~600 °C.

### Cited Findings
- Pattison (1992): andalusite–sillimanite equilibrium at Ballachulish is 645 °C and 3.0 ± 0.4 kbar. The extrapolated triple point is 4.5 ± 0.5 kbar, 550 ± 35 °C — [Pattison 1992, J. Geol.](https://www.ucalgary.ca/sites/default/files/teams/373/92AndSilJGeol.pdf)
- Most workers place the triple point between 3.8 kbar/500 °C (Holdaway 1971; Holdaway & Mukhopadhyay 1993) and 4.5 kbar/550 °C (Pattison 1992). In most rocks the polymorphs grew at different times (sequential growth, not simultaneous equilibrium) — [Pattison 2001, Am. Mineral.](https://www.ucalgary.ca/sites/default/files/teams/373/01tripleptammin.pdf); [Smith College Al₂SiO₅ diagram](https://www.science.smith.edu/~jbrady/petrology/metrocks-diagrams/Al2SiO5.php)
- Kyanite-group production in 2024: South Africa (andalusite) 150 kt, China (andalusite crude ore) 100 kt, US (kyanite) 80 kt, France (andalusite) 60 kt, Peru (andalusite) 40 kt, India (kyanite + sillimanite) 3.6 kt. No world total is published — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- Gore Mountain (Barton mine, NY) garnet sits in hornblende-rich garnet amphibolite at the edge of an olivine metagabbro. Garnet size jumps from <1 mm to 50–350 mm across a narrow zone — [NYSGA 2002](https://www.nysga-online.org/wp-content/uploads/2019/06/NYSGA-2002-B3-Geology-and-Mining-History-of-the-Barton-Garnet-Mine-Gore-Mt.-and-the-NL-Ilmenite-Mine-Tahawus-NY-with-a-temporal-excursion-to-the-MacIntyre-Iron-Plantation-of-1857.pdf). The megacrysts are ~1049 Ma and grew with fluid from a boundary fault — [Union College abstract](https://digitalworks.union.edu/ajes/vol21/iss1/9)
- Commercially attractive industrial garnet is rare. Idaho's Fernwood alluvial deposits (the largest US industrial garnet mine) are shed from mica–garnet schist — [USGS garnet](https://apps.usgs.gov/minerals-information-archives/gemstones/sp14-95/garnet.html); [Abrasive materials 1992](https://search.library.wisc.edu/digital/AFYIUSH2IKTZTI8Q/text/ARS3DD2E6TG2D78Y). World industrial garnet production was ~980 kt in 2024 — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- Wollastonite forms mainly by contact metamorphism of siliceous limestone (CaCO₃ + SiO₂ → CaSiO₃ + CO₂). It needs ~600 °C at low pressure and up to 950 °C at depth — [NSW wollastonite summary](https://www.resources.nsw.gov.au/sites/default/files/2022-11/wollastonite.pdf). In the Ritter Range it formed by infiltration of water-rich fluid at ~600 °C and 1,500 bar — [Contrib. Mineral. Petrol.](https://link.springer.com/doi/10.1007/s004100050371). At Pilares (Mexico, skarn), commercial grade needs >60% wollastonite over tens of metres — [NM Bureau of Geology abstract](https://geoinfo.nmt.edu/museum/nmms/abstracts/view.cfm?aid=179). St. Lawrence (Ontario) averages 41.3% wollastonite — [Ontario MDI](https://www.geologyontario.mndm.gov.on.ca/mndmfiles/mdi/data/records/MDI31C08NW00058.html). World production was ~1.1 Mt (China ~800 kt) — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Simulation rule: for each pelitic layer, compute its peak (P, T). If T > the andalusite/kyanite → sillimanite boundary, tag sillimanite. If P > ~4–5 kbar and T < sillimanite, tag kyanite. If P < ~4 kbar and T > ~450 °C (contact aureole), tag andalusite. Deposits (quartzite- or schist-hosted kyanite, aureole andalusite) are regional to rare.
- Garnet: amphibolite-facies pelitic schist → disseminated almandine, which feeds placers (heavy-mineral sands). Rare metagabbro megacryst deposits form on margins of metagabbro bodies with fluid access.
- Wollastonite: siliceous limestone/dolomite within ~1 km of a hot intrusion (aureole T ≥ 600 °C at low P) → skarn lenses. Rare/regional.
- Block mapping: none of these has a TFC/vanilla ore block (no mapping). The grade information can still drive TFC rock choice: pelite → `slate` (low grade) → `phyllite` → `schist` → `gneiss`; carbonate → `marble`; sandstone → `quartzite`.

### Gaps
- No sourced global abundance for garnet gem varieties. No sourced pressure dependence of the wollastonite reaction beyond the NSW summary.

---

## Talc, serpentinite and chrysotile asbestos

### Takeaway
Talc forms either by silica metasomatism of dolomite (stratabound in carbonate) or by low-temperature steatization of serpentinized ultramafics (ophiolites), at ~70–100 °C in one isotope study. Chrysotile asbestos forms cross-fiber veins in partly serpentinized peridotite of ophiolites (Thetford, Québec). Both are tied to obducted mantle slices.

### Cited Findings
- Many of the most productive talc deposits form by metasomatic addition of silica to carbonate rocks, mostly from adjacent siliceous sediments. Intrusions locally supply heat and fluids. Ultramafic-hosted talc comes from serpentinized ultramafics (ophiolite fragments or intrusions) later steatized by low-T hydrothermal fluids into talc-carbonate, pure talc or chlorite blackwall — [USGS OFR 95-586](https://pubs.usgs.gov/of/1995/0586/report.pdf)
- Sivas Basin (Turkey) oxygen-isotope temperatures: sedimentary-hosted talc ~68 °C, ultramafic-hosted talc 80–98 °C, serpentine ~100 °C — [Clays and Clay Minerals](https://resolve.cambridge.org/core/journals/clays-and-clay-minerals/article/mineralogy-and-geochemistry-of-paleocene-ultramafic-and-sedimentaryhosted-talc-deposits-in-the-southern-part-of-the-sivas-basin-turkey/ABC5B43F142BFD8196295AA990211B47)
- Cross-fiber chrysotile veins in serpentinized peridotite are the most important asbestos occurrence, as at Thetford — [USGS Bulletin 470-K](https://pubs.usgs.gov/bul/0470k/report.pdf). The Baie Verte, Thetford Mines, Black Lake, Asbestos (Québec) and Belvidere Mountain deposits all sit in partly serpentinized ultramafics of Lower Paleozoic ophiolites of the northern Appalachians. Larger, less serpentinized (stronger) peridotite bodies develop cross-fiber veins — [OneMine, chrysotile deposits of northern Appalachia](https://onemine.org/documents/geology-and-genesis-of-the-chrysotlle-asbestos-deposits-of-northern-appalachia). At Thetford Mines–Black Lake the peridotite was serpentinized during both oceanic and continental episodes — [Canadian Mineralogist 22:173](https://rruff.info/doclib/cm/vol22/CM22_173.pdf)
- World talc and pyrophyllite production was ~6.9 Mt in 2024. Asbestos was ~1.2 Mt — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Simulation rule: (a) any ultramafic slice emplaced by `ophiolite obduction`, or exhumed in a suture, becomes partly serpentinite (at ocean-floor hydration plus obduction). It gets asbestos-vein zones in large bodies cut by shear zones, and talc/steatite rinds where later low-T fluids act, especially along contacts with siliceous rocks. (b) Dolomite layers adjacent to cherts or quartzites, with a fluid event (burial or intrusion), get stratabound talc lenses.
- Rarity: talc is common/regional; asbestos is regional (suture zones).
- Block mapping: no TFC/vanilla block (no mapping).

### Gaps
- No sourced P–T for chrysotile vein formation.

---

## Jade: jadeitite (HP-LT subduction mélange) and nephrite

### Takeaway
Jadeitite forms from Na-Al-Si fluids in serpentinite-matrix subduction-channel mélanges that always contain HP-LT rocks. Most is fluid precipitate (P-type), with some replacement (R-type). The oldest jadeitites are Early Paleozoic, which suggests cold-enough subduction geotherms only from then on. Nephrite forms by Ca-metasomatism of serpentinite (common) or silicification of dolomite (rare, more valuable) at ~100–550 °C and low–moderate pressure.

### Cited Findings
- Jadeitite-bearing serpentinite-matrix mélange occurs in Caribbean (Guatemala, Cuba, Dominican Republic), circum-Pacific (Japan, Western USA, Papua New Guinea), Alpine–Himalayan (Italy, Iran, Greece, Myanmar) and Caledonian (Russia, Kazakhstan) belts, always with HP-LT metamorphic rocks. The oldest jadeitites are Early Paleozoic. Jadeitite formation P–T extends beyond blueschist facies into epidote-amphibolite and eclogite facies. Most are P-type fluid precipitates in the mantle wedge, and fewer are R-type replacements of plagiogranite, metagabbro or eclogite. Mélange can stay in the mantle wedge for a long time with repeated fluid infiltration. Formation ages and HP-LT metamorphism ages do not match at some localities — [Tsujimori & Harlow 2012, Eur. J. Mineral. 24:371–390](https://www.schweizerbart.de/papers/ejm/detail/24/76960/Petrogenetic_relationships_between_jadeitite_and_a)
- Most gem jadeite comes from northern Myanmar (Hpakan). Exhumation by serpentinite diapirism has been proposed — [GSHK article (secondary)](https://www.geolsoc.org.hk/Members_article/Trudy%20Kwong/Jade_GSHK_TrudyKwong.pdf)
- Nephrite forms by (1) dolomite replacement by silicic fluids tied to granitic plutonism or (2) serpentinite replacement by Ca-metasomatism at contacts with more silicic rocks. Serpentinite-type is found worldwide. Dolomite-type is rare and more valuable — [Yukon government report](https://yma.gov.yk.ca/097152/097152.pdf)
- Nephrite formation temperatures range from <550 °C (dolomite type), to ~400 °C (Fengtian, Taiwan), to ~100 °C (ophiolites), all at moderate to low pressure — [Harlow et al. abstract (AMNH)](https://research.amnh.org/users/gharlow/IGC2000LowRes.pdf). Val Malenco jade is a dolomite-related tremolite nephrite with up to ~30 wt% calcite — [Univ. Milan](https://air.unimi.it/handle/2434/223585)

### Inferences
- Simulation rule (jadeitite): a cell that experienced `subduction` with a cold geotherm (HP-LT: blueschist/eclogite at T ≲ 500 °C; **[background, unverified this session]**: jadeite + quartz stability needs roughly ≥1 GPa at 300–400 °C), with serpentinized mantle-wedge material in a subduction-channel mélange, later exhumed (by accretion, collision or serpentinite diapirism) to the surface. Gate on deposit age ≤ ~500 Ma, matching the Early Paleozoic oldest jadeitites. Place small (metre-scale) blocks and pods scattered in a serpentinite mélange belt along sutures. Very rare.
- Simulation rule (nephrite): (a) ophiolite serpentinite contacting granitoid, chert or graywacke + a fluid event → small lenses along contacts (rare); (b) dolomitic marble intruded by granite → very rare dolomite-type lenses.
- Block mapping: no TFC/vanilla jade. Vanilla `emerald` is a visual stand-in at most (no mapping recommended).

### Gaps
- Quantitative jadeitite P–T windows from the review's tables were not extracted. The Myanmar P–T path was not found.

---

## Marble, slate, schist and other metamorphic rocks as industrial/dimension materials

### Takeaway
These are rock types rather than ore deposits. The value for the mod is that simulated metamorphic grade should choose the TFC rock block directly.

### Cited Findings
- TFC rock types: igneous intrusive (granite, diorite, gabbro); sedimentary (shale, claystone, limestone, conglomerate, dolomite, chert, chalk, tuff); igneous extrusive (rhyolite, basalt, andesite, dacite); metamorphic (quartzite, slate, phyllite, schist, gneiss, marble) — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)
- TFC rock categories decide where a rock can spawn and which ores may spawn in it — [TerraFirmaGreg field guide, Geology](https://wiki.terrafirmagreg.team/modern/field-guide/sv_se/the_world/geology.html)
- World dimension-stone production has no published world total — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Rule: pelite/shale protolith → slate (sub-greenschist to lower greenschist), phyllite (greenschist), schist (upper greenschist–amphibolite), gneiss (upper amphibolite–granulite). Limestone/dolomite → marble at ≥ greenschist. Sandstone → quartzite. Because the TFC ore tables key on rock type, this mapping also passes geology into ore placement automatically.
- TFC has no ultramafic, serpentinite, eclogite, blueschist or migmatite blocks. Map ultramafic/serpentinite → `gabbro` (closest dark mafic), eclogite/blueschist → `schist` or `gneiss`, migmatite → `gneiss`.

### Gaps
- No metamorphic-grade boundaries were sourced in this session beyond the Al₂SiO₅ triple point.

---

## Ruby and sapphire (marble-hosted, amphibolite-hosted, basalt-xenocryst, placer)

### Takeaway
Corundum gems need Al-rich, Si-poor rocks with Cr (ruby) or Fe–Ti (blue sapphire). There are three main primary types. (1) Marble-hosted ruby in Himalayan collision belts formed at ~620–670 °C and ~3 kbar from evaporite- and organic-bearing platform carbonates. (2) Amphibolite-hosted ruby (Montepuez) formed at 550–750 °C and 0.4–1.1 GPa by metasomatism. (3) Sapphire xenocrysts carried by Cenozoic intraplate alkali basalts. Most production comes from secondary placers.

### Cited Findings
- Marble-hosted rubies are the most important coloured-gem source in Central and SE Asia. They sit in the Himalayan belt formed by Tertiary India–Eurasia collision, spatially related to granitoids, in platform carbonate series that underwent high-grade metamorphism. P ~3 kbar and 620 < T < 670 °C. The protolith was carbonates with detrital Fe–Cr–V clays, organic-rich shales and evaporite layers. Ruby formed on the retrograde path, mainly by destabilization of muscovite or spinel. The fluid was CO₂-rich with F, Cl and B from molten salts, in a closed system — [Garnier et al. 2008, Ore Geol. Rev.](https://www.isterre.fr/IMG/pdf/garnier-oregeology-2008-p1.pdf); [GIA 2024 summary](https://www.gia.edu/articles/fall-2024-gemnews-arzakite-inclusion-in-ruby)
- Mogok is not fully explained by this model. Some Mogok rubies show skarn-type formation (a flagged controversy) — [Lotus Gemology Mogok primer](https://lotusgemology.com/resources/articles/159-rock-talk-a-mogok-geology-primer)
- Luc Yen (Vietnam): ruby, sapphire and spinel in Upper Proterozoic–Lower Cambrian marble east of the Red River shear zone. Secondary placers sit in karst pockets and alluvial fans — [GIA Gems & Gemology, Vietnam](https://www.gia.edu/gems-gemology/wn13-long-gemstone-mining-vietnam)
- Montepuez (Mozambique): the whole complex underwent amphibolite-grade metamorphism at 0.4–1.1 GPa and 550–750 °C. Ruby formed mainly by metasomatism, from magma-derived fluid interacting with hosts in a low-silica environment. No rubies are found in marble on the concession. Deposits are mostly secondary. In the one primary pit the ruby amphibolite extends ~30 m below the surface over basement gneiss — [GIA Gems & Gemology Spring 2015](https://www.gia.edu/articles/spring-2015-mozambique-ruby-discovery-21st-century)
- Eastern Australia: many gem corundum deposits in Mesozoic–Cenozoic alkali basalt fields. Corundum is concentrated in alluvium as xenocrysts released from pyroclastic, volcaniclastic and lava hosts — [Australian Museum journal](https://journals.australian.museum/media/Uploads/Journals/17941/1358_complete.pdf); [NSW sapphire summary](https://www.resources.nsw.gov.au/sites/default/files/2022-11/sapphire.pdf). Corundum is split into "magmatic" and "metamorphic" suites — [Goldschmidt 2015](https://goldschmidtabstracts.info/2015/3441.pdf)
- Chanthaburi (Thailand) sapphires occur as xenocrysts in alkali basalt and resemble those of Cambodia, Nigeria, Cameroon, Australia, Laos and northern Madagascar — [GIA Chanthaburi](https://www.gia.edu/articles/sapphire-chanthaburi-thailand-gemological-characteristics). Spinel reaction coronas against the host basalt show disequilibrium, which supports a xenocrystic origin — [Contrib. Mineral. Petrol. 2017](https://link.springer.com/article/10.1007%2Fs00410-017-1362-0)
- TFC today:
  - `deep_ruby` vein: rarity 80, size 22, y −70..−10, density 0.2, in marble.
  - `ruby_marble_belt`: a disc in `collisional` mountain biomes only, rarity 16, y −40..−4, size 50, height 5. It replaces igneous and metamorphic rocks with marble (94%) plus ruby-in-marble (6%), with the comment "Inspired by the fact that IRL rubies are mostly from marble belts in the Himalayas".
  - Sapphire has no vein. It appears via spring-water fissures in stratovolcanoes (`sapphire_volcano_spring_water_fissure`), sapphire hot springs, and as the 5.33% rare drop from chalk placer deposits.
  - Sources: [TFC world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py); [TFC assets.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/assets.py)

### Inferences
- Simulation rules:
  - Marble ruby (Mogok/Himalaya type): carbonate-platform layer containing evaporite/organic interbeds (from the sedimentary record) + `collision` metamorphism reaching ~600–700 °C at moderate P (amphibolite facies) + granitoids nearby + exhumation. Place disseminated ruby (and spinel) in marble bands within the collision belt. Rare.
  - Amphibolite ruby: Al-rich mafic amphibolite in deep orogenic crust (550–750 °C, 0.4–1.1 GPa) + metasomatic/desilicated pegmatite fluid. Very rare.
  - Basalt sapphire: intraplate alkali basalt field (`plume`/`rift`, small-volume Cenozoic-style fields) + weathering/erosion → alluvial placers downstream of the basalt field. Rare.
  - Placer concentration: any exhumed corundum source + erosion + fluvial transport → gravels.
- Block mapping: ruby → TFC `ore/ruby/marble` (and vanilla: no ruby; use emerald or redstone as a stand-in, or no mapping). Sapphire → TFC `ore/sapphire/<rock>` in basalt or placer gravel (TFC currently has no sapphire vein; the mod should add a placer-gravel or basalt-hosted path).

### Gaps
- Global production shares by ruby/sapphire type were not found. Kashmir, Sri Lanka and Madagascar sapphire types (skarn, metasomatic, pegmatite-related) were not covered in detail.

---

## Emerald (schist-type at granite–ultramafic contact; Colombian black-shale type)

### Takeaway
Emerald needs Be (granite or pegmatite, or basinal brines) to meet Cr/V (ultramafic or mafic rocks, or black shale). Type IA "schist-type" deposits form where Be-bearing pegmatites or granites react with Cr-bearing ultramafics, giving phlogopite blackwall schists at ~330–650 °C. They supply a large share of world output. Colombian deposits formed at ~290–360 °C from evaporite-derived brines in Lower Cretaceous black shales, with no magmatism, via thermochemical sulfate reduction.

### Cited Findings
- Giuliani et al. (2019) classify primary emerald deposits as Type I (tectonic-magmatic-related) and Type II (tectonic-metamorphic-related). Type IA (hosted in metamorphosed ultramafic rocks) supplies a large share of world output, and IB/IC (sedimentary or granitic hosts) are minor. Type IIB is Colombian: Lower Cretaceous black shales of the Eastern Cordillera, with hydrothermal basinal fluids that dissolved evaporites along faults — [Giuliani et al. 2019, Minerals 9:105](https://www.mdpi.com/2075-163X/9/2/105)
- Type I deposits are the most numerous and can be subdivided by Cr/V. Type IIB emerald REE patterns match the host black shales — [Eby, Goldschmidt 2020](https://goldschmidtabstracts.info/2020/639.pdf)
- Colombian model: hydrothermal brines carried evaporitic sulfate to structural traps. There it was thermochemically reduced, and the reaction with shale organic matter released Cr, V and Be — [Ottaway et al. 1994, Nature 369:552](https://www.nature.com/articles/369552a0). Fluid-inclusion P–T is ~1 kbar and 290–360 °C. Burial alone (~135 °C at 4.5 km) would need 165–225 °C extra heat input — [IRD colloquium paper](https://horizon.documentation.ird.fr/exl-doc/pleins_textes/pleins_textes_6/colloques2/38474.pdf). Fluids are Na–Ca brines with some KCl — [IRD/Giuliani et al. 1995](https://horizon.documentation.ird.fr/exl-doc/pleins_textes/pleins_textes_7/b_fdi_51-52/010019064.pdf)
- Schist-type temperatures: Paraná deposit (Brazil) 390–530 °C deformation temperatures, with emerald in phlogopite schists — [Anais Acad. Bras. Ciênc. 2021](https://doaj.org/article/57872f2223a04cf0921f6bca0d3b9d3f). Um Solimate (Egypt) fluid inclusions 330–370 °C at ~200 MPa — [METU/Egypt schist study](https://avesis.metu.edu.tr/yayin/e13b940d-aa66-4981-a98e-45ee18a0ff8e/genetic-model-for-beryl-emerald-related-schist-in-egypt-clues-of-metasomatism). Sandawana (Zimbabwe) 560–650 °C — [Naturalis Scripta Geologica 131](https://repository.naturalis.nl/pub/209632/). Most mineralization sits in tabular or lenticular mafic schists/"blackwall zones" — [Yukon schist-hosted emerald model](https://emrlibrary.gov.yk.ca/ygs/open_files/2005/2005-5/q07_schist_hosted_emeralds.pdf)
- TFC today: `emerald` is a pipe vein (rarity 80, size 60, radius 5, y −64..100, density 0.15) in any igneous intrusive rock. It is also a decoration in volcano spring-water fissures, and the rare placer drop from diorite — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Simulation rules:
  - Type IA: a granite/pegmatite intrusion event (fractionated, Be-bearing: peraluminous or late-orogenic) whose aureole intersects an ultramafic or mafic layer (ophiolite slice, greenstone komatiite). Place emerald in phlogopite-schist rinds 0.5–5 m wide along the contacts (width **[background, unverified this session]**). Rare.
  - Type IIB (Colombia): black-shale + evaporite basin later inverted (`collision` or `thrust` reactivation) with deep faults and a thermal pulse giving ~300 °C. Place vein/breccia emerald along thrusts. Very rare (one district on Earth).
- Block mapping: TFC `ore/emerald/<rock>` (schist for IA; shale for IIB). Vanilla `emerald_ore`. Recommend replacing TFC's pipe-in-any-intrusive placement with contact-zone placement.

### Gaps
- Production shares by country and type were not retrieved (the MDPI full text was blocked, HTTP 403).

---

## Lapis lazuli (contact/regional metasomatism of dolomitic marble: Sar-e-Sang)

### Takeaway
Lapis (lazurite + calcite + diopside + pyrite) forms as metasomatic lenses (up to ~6 m thick) in calcite/dolomite marbles at upper amphibolite facies. It forms where quartzofeldspathic rocks or granitoids exchange with marble in sodic fluids, with sulfur from pyrite. An alternative metamorphosed-evaporite origin is debated. Major deposits are very few (Sar-e-Sang, Lake Baikal, Chile).

### Cited Findings
- Sar-e-Sang host rocks are sillimanite–kyanite–garnet–cordierite gneiss, amphibolite, calc-silicates and dolomitic marble at upper amphibolite facies. Lapis is interpreted as high-T metasomatic exchange between quartzofeldspathic rocks and marble in the presence of sodic solutions from anatexis. Sulfur came from pyrite in the marble and calc-silicates. An alternative view (Kulke 1976; Schreyer & Abraham 1976) treats lapis and associated whiteschist as a metamorphosed evaporite. Age is generally Precambrian, but is uncertain — [American Mineralogist 73:345 (1988)](https://msaweb.org/AmMin/AM73/AM73_345.pdf)
- Lapis forms lens-like bodies and layers up to 6 m thick in calcite and dolomite marbles, with diopside skarn and forsterite calciphyre. It formed by metasomatic reaction between granitoids and marble — [IUGS Geoheritage, Sar-e-Sang](https://iugs-geoheritage.org/?p=10779); [GIA Wyart et al. 1981](https://www.gia.edu/gems-gemology/winter-1981-lapis-afghanistan-wyart)
- TFC today: `lapis_lazuli` vein (rarity 30, size 30, y −20..80, density 0.12) in limestone and marble. It is also the rare placer drop from limestone and marble — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Simulation rule: a dolomitic marble layer (evaporite-associated carbonate platforms are favoured, consistent with both genetic models) metamorphosed to upper amphibolite facies (~600–700 °C) + a granitoid/pegmatite or anatectic melt in contact + a sulfide-bearing protolith. Place lenses 1–6 m thick at marble–gneiss/granite contacts. Very rare (handful per world).
- Block mapping: TFC `ore/lapis_lazuli/marble`. Vanilla `lapis_ore`. TFC's placement in unmetamorphosed limestone is geologically wrong. Restrict it to high-grade marble near intrusions.

### Gaps
- Global production figures for lapis were not found.

---

## Spinel, tanzanite, peridot

### Takeaway
Gem spinel is mainly marble-hosted, together with ruby (Mogok, Luc Yen, Mahenge). Tanzanite is essentially a single deposit, Merelani (~7 km², Neoproterozoic graphitic gneiss, Pan-African). Peridot comes from mantle xenoliths in young alkali basalts (San Carlos) or from hydrothermal veins in obducted mantle peridotite (Zabargad).

### Cited Findings
- Luc Yen spinel occurs in marble with phlogopite, dravite, pyrite and graphite. Spinel and ruby co-occur in marble-hosted deposits — [GIA Vietnam](https://www.gia.edu/gems-gemology/wn13-long-gemstone-mining-vietnam); [VJS Earth Sciences (Long, Giuliani et al.)](https://vjs.ac.vn/jse/article/view/12241). Mogok, Mahenge and Luc Yen spinel is marble-hosted and low-Fe (dealer source, secondary) — [Skyjems encyclopedia](https://skyjems.ca/pages/encyclopedia-marble-hosted-deposit)
- Tanzanite: an ~7 km² deposit in the Merelani area is the only known source. Late Proterozoic metasediments along the Lelatema fault zone host gem material in boudinaged pegmatitic veins and hydrothermal fracture fillings in brecciated graphite-bearing gneiss — [Stellenbosch thesis](https://scholar.sun.ac.za/handle/10019.1/1093). One study proposes formation at ~500 Ma (Pan-African) as a contact-metasomatic skarn (dealers quote ~585 Ma; the age is disputed) — [World Geology 2024](https://search.napstic.cn/literature/periodical/010sjdz202403004)
- San Carlos peridot: alkali olivine basalt carrying mantle-peridotite (dunite) xenoliths. Weathering frees the olivine. The basalt age is quoted variously at ~0.5–3 Ma, which conflicts between sources — [AMNH mantle xenolith](https://amnh.org/exhibitions/permanent/planet-earth/why-are-there-ocean-basins-continents-and-mountains/plate-tectonics/circulating-heat/how-to-read-the-mantle/mantle-xenolith); [EarthDate](https://www.earthdate.org/files/000/002/414/EarthDate_224_C.pdf)
- Zabargad (Red Sea): harzburgite/dunite emplaced during Red Sea rifting, resembling oceanic mantle. Gem olivine grows in late hydrothermal veins in serpentinite — [EarthRef Zabargad](https://earthref.org/ERR/36517)

### Inferences
- Rules:
  - Spinel: same rule as marble ruby. Use spinel when Cr is low, Mg is high, or Al is in excess, as a co-product. Rare.
  - Tanzanite: unique. Graphitic (V-bearing) gneiss + kyanite schist + Pan-African-style collision metamorphism + late pegmatite/hydrothermal veining. Treat as a "legendary" deposit (≤1 per world) or omit.
  - Peridot: (a) intraplate alkali basalt fields with mantle xenoliths, young (<~5 Ma) so the xenoliths are fresh; weathering frees olivine into soils and placers. (b) Rift-exhumed mantle peridotite with serpentinization veins. Rare.
- Block mapping: none have TFC or vanilla blocks (no mapping).

### Gaps
- Global production shares for spinel and peridot were not found. The "80–95% of world peridot from San Carlos" claim is from a dealer source and is unverified.

---

## Topaz and tourmaline

### Takeaway
Topaz needs F-rich, Al-rich felsic systems. It occurs in greisens and pegmatites of evolved granites and as vapor-phase crystals in topaz rhyolites (Thomas Range). Imperial topaz is essentially one district (Ouro Preto). Gem tourmaline (elbaite) comes from Li-rich LCT miarolitic pegmatites. Cu-bearing "Paraíba" tourmaline comes from pegmatites (530–480 Ma) in NE Brazil, Nigeria and Mozambique.

### Cited Findings
- Topaz Mountain (Thomas Range, Utah): HF-rich gas from cooling rhyolite carried Al, Si and trace metals and deposited topaz in gas cavities and vent channels. Vugs concentrate along flow bands — [Am. Mineral. 2023 abstract](https://msaweb.org/MSA/AmMin/TOC/2023/Abstracts/AM108P2153.pdf); [Christiansen et al. 1984, Am. Mineral. 69:223](https://msaweb.org/AmMin/AM69/AM69_223.pdf)
- Topaz hosts are granites, pegmatites, greisens and hydrothermal veins. The Ouro Preto district has been the only known source of imperial topaz for over 200 years — [GIA imperial topaz reading list](https://www.gia.edu/reading-list-imperial-topaz); [Keller, GIA: Capão deposit](https://cms.gia.edu/dam/jcr:27ea72a5-0b0f-4630-b1f4-e3fd0ad21daa/The-Capao-Topaz-Deposit-Ouro-Preto-Minas-Gerais-Brazil.pdf). Greisen forms at roughly 300–500 °C (dealer source, secondary) — [Skyjems greisen](https://skyjems.ca/pages/encyclopedia-greisen)
- Cruzeiro (Minas Gerais), economically the most important coloured-tourmaline mine, is a Li-, Be-, B-, Nb-, Ta- and Zn-enriched miarolitic pegmatite in Proterozoic schist–quartzite — [MDPI Crystals 2025](https://www.mdpi.com/2073-4352/15/5/415)
- Paraíba tourmaline: primary deposits in pegmatites intruding quartzites and metaconglomerates at 530–480 Ma. Cu-tourmaline crystallized from a B–Li-rich hydrous melt early in the quartz core. The Cu source is unresolved. Nigerian and Mozambican material is mostly alluvial — [GIA Paraíba origin, 2019](https://www.gia.edu/gems-gemology/winter-2019-paraiba-tourmaline-geographic-origin-determination)
- TFC today: topaz has no vein. It appears in lava fissures of volcanoes and stratovolcanoes (`topaz_volcano_fissure`) and as the rare placer drop from granite — [TFC world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py); [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rules:
  - Topaz: (a) evolved granite intrusion (S-type/A-type, late-orogenic or anorogenic) → cupola greisens and pegmatites (with cassiterite/wolframite; links to TFC `cassiterite`); (b) F-rich rhyolite eruptions (rift/anorogenic) → vapor-phase topaz in vesicular rhyolite. Regional to rare. Imperial-type: unique.
  - Tourmaline: LCT pegmatite fields at the margins of peraluminous granites in collision belts → miarolitic pockets. Rare.
- Block mapping: topaz → TFC `ore/topaz/<rock>` (granite, rhyolite). Tourmaline → no block (no mapping).

### Gaps
- No global production figures for topaz or tourmaline were found.

---

## Opal (Australian sedimentary/weathering opal; volcanic opal)

### Takeaway
Over 90% of precious opal comes from deeply weathered Cretaceous sediments of Australia's Great Artesian Basin. It probably formed by acidic oxidative weathering of pyrite- and ash-rich sediments, with the profile preserved under Cenozoic cover. The silica source and timing are debated. Volcanic opal (e.g., Wegel Tena, Ethiopia) forms in rhyolitic ignimbrite by pedogenesis at ambient temperature, in stacked horizons.

### Cited Findings
- Australia produces >90% of the world's precious opal, from highly weathered Cretaceous sedimentary rocks of the Great Artesian Basin (Eromanga and Surat basins). The silica source is unresolved. Ba content in eastern opals suggests a volcanic contribution — [Dutkiewicz et al. 2015, Gondwana Research](https://www.earthbyte.org/Resources/Pdf/Dutkiewicz_et_al_opal_GondwanaResearch_2015.pdf); [Merdith et al. 2013 prospectivity map](https://earthbyte.org/Resources/Pdf/Merdith_etal_opal_prospectivity_map_AJES2013.pdf)
- One proposed model: Australia was at high latitude from 97 to 60 Ma with protracted uplift and erosion. Most precious opal may have formed then by acidic oxidative weathering, and the opalised redox front was preserved by a veneer of Cenozoic sediments. Lower Cretaceous lithologies rich in pyrite, Fe²⁺, feldspar and volcanic ash have large acidification potential. This is a suggestion, from a secondary site summarizing the literature — [austhrutime.com (secondary)](https://austhrutime.com/great_australan_basin_opalisation.htm)
- Opal-A consists of submicrometre silica spheres 140–320 nm in diameter, which control play-of-colour — [Liesegang & Milke 2014, Am. Mineral.](https://www.degruyterbrill.com/document/doi/10.2138/am.2014.4791/html)
- Wegel Tena (Ethiopia): opal in Oligocene rhyolitic ignimbrites, in lenticular horizons in an unwelded ignimbrite. Several superimposed opal levels formed by successive pedogenesis — [Canadian Mineralogist 2017 study (UTS repository)](https://opus.lib.uts.edu.au/handle/10453/125998). Silica came from glass and feldspar, and opal formed at ambient temperature from soil water in a warmer, wetter climate — [HAL: pedogenic origin of Wegel Tena opal](https://hal.archives-ouvertes.fr/hal-02010828)
- TFC today: `opal` is a river-only disc (rarity 25, size 8, y 40..60, height 4) in sedimentary and igneous-extrusive rocks. It is also the rare placer drop from quartzite — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rules:
  - Sedimentary opal: a cratonic basin with Cretaceous-like fine sediments containing volcanic ash and pyrite + long subaerial weathering (deep, acidic, oxidizing) + later burial or cover + an arid present. Place thin seams and nodules along a paleo-redox front 10–30 m below the surface (depth **[background, unverified this session]**) over large areas with low density. Rare but areally extensive.
  - Volcanic opal: rhyolitic ignimbrite sheets + humid-warm pedogenesis episodes between eruptions → stacked thin horizons. Rare.
- Block mapping: TFC `ore/opal/<rock>` (claystone/shale/sandstone stand-ins; rhyolite/tuff for the volcanic type). No vanilla equivalent.

### Gaps
- Robust ages for Australian opal formation are controversial and not resolved in the sources found.

---

## Amethyst and agate geodes (flood-basalt hosted)

### Takeaway
The large amethyst geodes of the Paraná–Etendeka LIP (Brazil/Uruguay) formed in basalt lava cavities. Older models invoked high-T magmatic degassing. Recent fluid-inclusion and triple-oxygen-isotope work indicates low-temperature (15–60 °C) groundwater crystallization after the basalts were altered to smectite and zeolites.

### Cited Findings
- Los Catalanes (Uruguay): crystallization temperatures are estimated at 15–60 °C. Two hypotheses compete: hot residual fluids from the cooling lava, or groundwater from underlying aquifers (Gilg et al. 2003, 2014; Morteani et al. 2010 favour groundwater/low T) — [Mineralium Deposita 2024](https://link.springer.com/article/10.1007/s00126-024-01310-2)
- Earlier work linked protogeodes to high-T (1,150 °C) degassing. Other work indicates the basalts were brittle (~130 °C) and intensely altered to smectite and zeolites before protogeode formation — [Redalyc/Paraná study](https://www.redalyc.org/pdf/327/32730090012.pdf)
- TFC today: `amethyst` is a river-only disc (rarity 25, size 8, y 40..60) in sedimentary and metamorphic rocks. Separately, a vanilla-style `geode` (chance 1/50, y −48..30) has a basalt outer shell, a quartzite middle and inner layer, and an amethyst-in-quartzite alternate inner layer — [TFC world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py)

### Inferences
- Rule: `plume/LIP` flood-basalt layers (thick flows with vesicular tops) + later low-T groundwater circulation (burial < ~1–2 km, an aquifer beneath) → geode-bearing horizons in specific flows. Regional within LIPs. Agate is the same rule with chalcedony (more widespread, including in andesites).
- Block mapping: vanilla amethyst geode / TFC `ore/amethyst` in basalt cavities. Restrict TFC-style geodes to basalt/andesite flows in the simulated column.

### Gaps
- No global production data. Rio Grande do Sul/Artigas tonnages were not found.

---

## Cryolite (Ivigtut: essentially unique)

### Takeaway
The only commercially significant cryolite body on Earth was at Ivigtut (Greenland). It was a pegmatitic body of nearly pure cryolite in the cupola of a small (~300 m) Mesoproterozoic (~1.25 Ga) peralkaline/A-type granite stock in the Gardar rift province, mined from 1854 until exhaustion in 1987. Production figures are inconsistent between sources.

### Cited Findings
- Ivigtut: a Mid-Proterozoic granite pipe with cross-cutting cryolite. Violent brecciation was followed by fluid-driven events, and zones include siderite–cryolite, pure cryolite and fluorite–topaz. The granite is ~1,250 Ma (Rb–Sr) — [Pauly & Bailey 1999, Meddelelser om Grønland](https://tidsskrift.dk/meddrgroenland_geosci/article/view/140693)
- It was a massive pegmatitic body of nearly pure cryolite in a peralkaline granite stock (~300 m diameter) intruding Precambrian gneisses, in the Proterozoic Gardar alkaline province. More than 3 Mt was mined from 1854 until depletion in 1987 — [Richard I. Gibson (substack)](https://richardigibson.substack.com/p/ivigtut). A figure of 12.3 Mt (original body, Pauly & Bailey) also circulates, and the two figures may not describe the same quantity (conflict flagged) — [tidsskrift.dk record](https://tidsskrift.dk/meddrgroenland_geosci/article/view/140693)
- The granite held the world's largest cryolite body, now mined out — [Glasgow eprints](https://eprints.gla.ac.uk/874). Fluorite worldwide-type lists include "cryolite" deposits as a distinct small category — [SFU thesis](https://summit.sfu.ca/item/17481)
- TFC today: `cryolite` vein has rarity 16, size 18, y −70..−10, density 0.7, in granite and diorite. TFC uses cryolite (with cinnabar) as a redstone source — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rule: a `rift`/anorogenic event producing A-type/peralkaline granite (fluorine-rich) at shallow level (cupola). With very low probability per stock (e.g., ≤1 per world), place a massive cryolite pod (tens to ~100 m) in the cupola with siderite, fluorite and topaz zones. Small disseminated cryolite in other peralkaline granites can be allowed at very low grade (**[background, unverified this session]**: minor cryolite occurs in e.g. Pitinga, Brazil and Pikes Peak, USA).
- Game-balance conflict: TFC makes cryolite a common deep-granite redstone ore. If the mod makes cryolite geologically unique, redstone supply falls back to `cinnabar`. Recommend: one guaranteed "Ivigtut" jackpot per world, plus sparse small cryolite in A-type granite cupolas, plus geologically placed cinnabar (epithermal/hot-spring Hg in arcs) as the main redstone path.
- Block mapping: TFC `ore/cryolite/granite`. Vanilla `redstone_ore` (a functional stand-in).

### Gaps
- Pauly & Bailey's monograph text was not accessed. The exact tonnages remain conflicting.

---

## Fluorite and barite

### Takeaway
Fluorite (fluorspar, 9.5 Mt/yr, China ~62%) forms mainly in igneous-hosted hydrothermal/epithermal veins, MVT/carbonate-hosted replacements (e.g., Illinois–Kentucky over a carbonatite), and carbonatite/alkaline systems, often in rift settings. Barite (~8.2 Mt/yr) shares these settings plus SEDEX/bedded types. Neither has a TFC or vanilla block.

### Cited Findings
- From ~630 analyses of ~183 deposits, economic fluorite settings ranked by representation are: hydrothermal/epithermal veins in igneous hosts; MVT; carbonate-hosted veins and replacements; carbonatite-related; then metamorphic-hosted veins, SEDEX, skarn, greisen, intrusion-related Mo, cryolite, peralkaline igneous rocks, sedimentary-hosted veins, rare-metal pegmatites, granite-related U and IOCG — [SFU thesis (Makin)](https://summit.sfu.ca/item/17481)
- The Illinois–Kentucky Fluorspar District is a fluoritic MVT subclass. Hicks Dome is a cryptoexplosive breccia over a carbonatite, which may have supplied F, REE and heat — [Univ. Illinois record](https://experts.illinois.edu/en/publications/mineralogical-characterization-of-hicks-dome-intrusive-core-sampl/)
- Fluorite REE patterns (Y–Yb) separate carbonatite-related from MVT fluorite. Fluorite is a proximal indicator for carbonatite REE deposits — [BCGS Paper 2014-1 (Makin et al.)](https://cmscontent.nrs.gov.bc.ca/geoscience/PublicationCatalogue/Paper/BCGS_P2014-01-13_Makin.pdf)
- Fluorspar 2024: world ~9.5 Mt. China 5.9 Mt, Mexico 1.2 Mt, Mongolia 1.2 Mt. Reserves ~320 Mt. Barite 2024: world ~8.2 Mt (largest producers India ~2.6 Mt and China ~2.1 Mt, as read from the MCS table; column alignment in the PDF extraction was imperfect) — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Rules:
  - Fluorite: (a) veins around F-rich granites and alkaline/carbonatite intrusions in rifts; (b) MVT-style replacements in platform carbonates near basin margins (basinal brine flow driven by orogenic loading; links to the MVT notes); (c) greisen accessory. Regional/common.
  - Barite: SEDEX/bedded barite in rift-basin black shales, MVT/vein barite, and residual barite in weathering profiles over carbonate-hosted veins. Common.
- Block mapping: no TFC/vanilla block (no mapping). If an addon adds fluorite, map it here.

### Gaps
- No dedicated barite deposit-model source was retrieved this session. Barite settings are partly **[background, unverified this session]**.

---

## Sulfur (salt-dome cap rock/Frasch; volcanic)

### Takeaway
Native sulfur deposits form (1) in salt-dome cap rocks and bedded evaporites, where sulfate-reducing bacteria use anhydrite and hydrocarbons to make H₂S that is oxidized to S, within ~900 m of the surface, and (2) at volcanic fumaroles/solfataras. Today >~85 Mt/yr of sulfur is almost all a by-product of oil/gas processing.

### Cited Findings
- USGS model: bacterial sulfate reduction in cap rocks occurs within 900 m of the surface. It needs ~0.3–0.6 m³ of oil per tonne of sulfur produced — [USGS OFR 92-403](https://pubs.usgs.gov/of/1992/0403/report.pdf)
- Rising salt dissolves near the surface and leaves an anhydrite cap rock. Sulfate reducers consume hydrocarbons using anhydrite sulfate, and the resulting H₂S is oxidized to native sulfur — [EBSCO Research Starters: Salt domes](https://www.ebsco.com/research-starters/geology/salt-domes/); [ACS PRF report](https://acswebcontent.acs.org/prfar/2017/Paper14810.html)
- Frasch mining injects ~150 °C water to melt sulfur. The Gulf Coast dominated world sulfur in the early–mid 20th century until by-product sulfur from oil and gas undercut it — [Frasch process (Wikipedia mirror)](https://wikipedia.jakami.de/content/wikipedia_en_chemistry_nopic/Frasch_process)
- Newer work favours anaerobic generation of elemental sulfur by sulfate reducers (Zechstein) and sulfate-dependent anaerobic oxidation of methane for cap-rock carbonate (Gulf Coast), not older aerobic models — [PMC 6379371](https://pmc.ncbi.nlm.nih.gov/articles/PMC6379371)
- World sulfur (all forms) ~85 Mt (2024). Most production is a by-product of fossil-fuel processing — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- TFC today: `sulfur` disc (rarity 4, size 18, y −64..−45, height 5) near lava in igneous-intrusive and metamorphic rocks. `tuff_sulfur` disc (rarity 2, size 18, y 40..200) in tuff — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rules:
  - Salt-dome sulfur: `evaporite basin` salt layer + later diapirism (salt tectonics; may need a simple proxy, e.g., thick salt + sediment loading) + hydrocarbon maturation in adjacent strata (burial-T history) → sulfur-bearing cap rock above dome crests at 100–900 m depth. Rare.
  - Volcanic sulfur: active or young arc volcanoes and hot springs → surface encrustations and shallow lenses in altered volcanics (tuff, andesite). TFC's `tuff_sulfur` is consistent with this.
  - Bedded biogenic sulfur in gypsum (e.g., Carpathian foredeep, **[background, unverified this session]**).
- Block mapping: TFC `ore/sulfur/<rock>`. No vanilla block.

### Gaps
- No quantitative volcanic sulfur deposit sizes were retrieved.

---

## Saltpeter/niter: Atacama caliche nitrate and cave saltpeter

### Takeaway
Atacama nitrate (with iodine) is caliche built up from atmospheric deposition over Myr of hyperaridity, possibly ~10⁷ yr. The onset of hyperaridity is debated (25, 14 or 3 Ma). Cave saltpeter in limestone caves comes mainly from soil-derived ammonium/nitrate carried in by seepage and nitrified in cave sediments. Bat guano is only a minor contributor, and the deposit regenerates.

### Cited Findings
- Isotope evidence (N, O, Δ¹⁷O) shows Atacama nitrate is a long-term accumulation of atmospheric deposition, on the order of 10⁷ yr for the Atacama (10⁴ yr for Death Valley) — [Böhlke, Ericksen & Revesz 1997 (USGS)](https://pubs.usgs.gov/publication/70019976); [USGS summary](https://www.usgs.gov/publications/long-term-atmospheric-deposition-source-nitrate-and-other-salts-atacama-desert-chile)
- Flux calculations allow the major deposits to have accumulated in 0.2–2.0 Myr of hyperaridity. Estimates of hyperaridity onset range from 25 Ma (Dunai et al. 2005) through 14 Ma (Alpers & Brimhall 1988) to 3 Ma (Hartley & Chong 2003) — [ASA 2006 abstract](https://a-c-s.confex.com/crops/wc2006/techprogram/P18247.HTM)
- The massive accumulation needs a rare combination of climate, tectonics and hydrology — [Reich & Bao 2018, Elements](https://repositorio.uchile.cl/handle/2250/169399)
- Hill (1981): compost, animal remains and bat guano cannot account for the areal and vertical extent and regeneration of saltpeter earth. Surface-soil nitrogen (NH₄⁺) carried by seeping water is oxidized to nitrate in cave sediments by Nitrosomonas/Nitrobacter, and evaporation at the cave air–rock boundary concentrates it. Minerals are niter (KNO₃), soda-niter and others — [Hill 1981, NSS Bulletin 43](https://legacy.caves.org/pub/journal/PDF/V43/v43n4-Hill-Origin.htm)
- World iodine ~33 kt (2024). The Atacama caliche is the main source (Chile is the leading producer; stated in the MCS iodine chapter) — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- TFC today: `saltpeter` disc (rarity 110, size 35, y 40..100, height 5) in all sedimentary rocks — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rules:
  - Caliche nitrate: hyperarid climate (MAP < ~20 mm, **[background, unverified this session]**) sustained for ≥~1–10 Myr, in endorheic or low-relief terrain with no flushing (a coastal fog desert is ideal), with no later humid phase. Place a surface/near-surface crust (0.5–5 m) over large areas. Rare (one Atacama-scale region per world at most). Co-place iodine/borates/perchlorate flavour.
  - Cave saltpeter: karstic limestone with dry cave passages + soil cover above (temperate–subtropical) → saltpeter earth in cave floors. Common but small. Place as cave-floor patches in carbonate layers near the surface.
- Block mapping: TFC `ore/saltpeter/<rock>`. Vanilla has no saltpeter (gunpowder comes from mobs). TFC's "any sedimentary rock at surface depths" placement should be restricted to arid playa crusts and limestone caves.

### Gaps
- No quantitative Atacama caliche thickness or grade was found in this session.

---

## Borates (borax/tincal, colemanite, ulexite)

### Takeaway
Major borate deposits are evaporitic/lacustrine chemical sediments in closed continental basins under arid–semiarid climates. All are associated with contemporaneous volcanism and thermal springs that supply boron, typically Neogene, in extensional or arc settings. Western Turkey holds roughly 85–90% of listed reserves. Only four major borax (tincal) deposits exist worldwide.

### Cited Findings
- Western Anatolia hosts the world's largest borate deposits, over an area of ~300 × 150 km. They are evaporites or chemical precipitates in closed continental basins under arid to semi-arid conditions, all associated with volcanic rocks and thermal springs. Kırka formed in a caldera (Kırka–Phrigian). Districts: Bigadiç (colemanite, ulexite), Sultançayır (priceite), Kestelek and Emet (colemanite), Kırka (borax). The four main borax deposits are Kırka, Boron (California), Tincalayu and Loma Blanca (Andes) — [Helvacı et al. (Southampton eprints)](https://eprints.soton.ac.uk/413247/1/Helvaci_revised_April_24.pdf); [Helvacı paper](https://eprints.soton.ac.uk/443224/1/Helvaci_paper.pdf)
- Borate δ¹¹B (−1.6 to −25.3‰) is controlled by mineralogy and brine pH. Boron sources differ by district (Emet: aluminosilicates; Kırka: Eocene carbonates) — [Palmer & Helvacı 1997](https://literature.wolkersdorfer.info/literature/The%20boron%20isotope%20geochemistry%20of%20the%20Neogene%20borate%20deposits%20of%20western%20Turkey.pdf)
- USGS: borates are associated with volcanic activity and arid climates. The largest economic deposits are in the Mojave Desert, the Alpide belt and the Andean belt. Turkey's reserves are listed at 950 Mt (B₂O₃ basis), against 48 Mt (US), 40 Mt (Russia), 35 Mt (Chile) and 9.1 Mt (China). ~70% of Turkish deposits are colemanite — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- TFC today: `borax` disc (rarity 40, size 23, y 40..100, height 3) in claystone, limestone and shale. It is also the rare placer drop from shale — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rule: an endorheic basin (rift or caldera, from the tectonic sim) + arid/semi-arid climate during basin fill + active felsic/intermediate volcanism within ~tens of km (arc or post-collisional extension) → lacustrine borate beds (metres to tens of metres) interbedded with tuff, claystone and marl. Na-borate (borax) in alkaline Na-rich brines and Ca-borate (colemanite) in Ca-rich ones. Rare (a few districts per world). They preserve poorly over long times (soluble), so bias toward young (Neogene–Quaternary) basins.
- Block mapping: TFC `ore/borax/<rock>` (claystone, tuff stand-ins). No vanilla block.

### Gaps
- No sourced bed thickness or grade figures.

---

## Gypsum, diatomite, bentonite and zeolites

### Takeaway
Gypsum (160 Mt/yr) comes from marine and lacustrine evaporites and is common. Diatomite (3 Mt/yr) is biogenic silica, mostly Miocene, from upwelling continental margins (marine, best quality) or lakes in volcanic terrains. Bentonite (21 Mt/yr) and zeolites (1 Mt/yr) are altered volcanic ash. Saline-alkaline lake water gives zeolites, and fresher or Mg-rich water gives smectite, with concentric zoning in playa basins.

### Cited Findings
- World gypsum ~160 Mt (2024); diatomite ~3.0 Mt (US 29%, Denmark 18%, China 12%); bentonite ~21 Mt; natural zeolites ~1.0 Mt — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)
- Diatomite: marine commercial deposits formed along continental margins with submerged basins and shelves where upwelling fed diatom blooms. Lacustrine deposits form in volcanic terrains (e.g., Basin and Range) with little clastic input. Marine deposits are generally higher quality and larger. Most commercial diatomites, marine and lacustrine, are Miocene — [USGS Bulletin 2209-D](https://pubs.usgs.gov/publication/b2209D)
- Volcanic ash alters rapidly to zeolites in saline, alkaline lakes, and slowly to smectite in moderately saline, non-alkaline lakes. Playa tuffs zone from smectite/fresh glass (margin) → zeolites → analcime/K-feldspar (centre). Open-system groundwater can also make bentonite or zeolite beds — [Hay 1986, Pure Appl. Chem.](https://joomla.iupac.org/publications/pac/1986/pdf/5810x1339.pdf)
- Most bentonites form under marine conditions (e.g., US Western Interior, ash buried beneath hundreds of metres of marine sediment), less commonly in alkaline lakes — [NSW bentonite summary](https://www.resources.nsw.gov.au/sites/default/files/2022-11/bentonite.pdf). Tuzgölü Basin (Turkey): Miocene zeolitic tuffs (clinoptilolite, chabazite, erionite, analcime) formed in a saline-alkaline basin — [Clays and Clay Minerals](https://resolve.cambridge.org/core/journals/clays-and-clay-minerals/article/abs/geology-and-conditions-of-formation-of-the-zeolitebearing-deposits-southeast-of-ankara-central-turkey/F91ED1CE7851CF297F988AB9A01EFC03)
- TFC today: `gypsum` disc (rarity 70, size 25, y 40..100) in all sedimentary rocks. It is also the rare placer drop from gneiss — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rules:
  - Gypsum: any evaporite layer (from the sedimentation module) and cap rocks → gypsum near the surface (anhydrite hydrates to gypsum on exhumation, **[background, unverified this session]**). Common.
  - Diatomite: needs diatoms in `lifeTimeline` (gate to post-Cretaceous, and heavily favour ≥ Miocene). Marine upwelling margin (west-coast eastern-boundary currents in the climate sim) or volcanic-terrain lakes. Regional.
  - Bentonite: each explosive volcanic ash-fall event deposits thin layers. Those in marine basins become bentonite on burial. Common as thin beds.
  - Zeolites: tuff in saline-alkaline closed lakes (arid + volcanic) or open-system groundwater → zoned deposits. Regional.
- Block mapping: gypsum → TFC `ore/gypsum/<rock>`. Diatomite, bentonite and zeolite have no TFC/vanilla block; bentonite could be represented as vanilla/TFC clay.

### Gaps
- Evaporite-specific details (halite, sylvite, anhydrite sequencing) are presumably covered by the sedimentary/evaporite notes and were not researched here.

---

## Mica, feldspar and silica (industrial)

### Takeaway
These are large-tonnage, low-value commodities: industrial sand ~440 Mt/yr, feldspar ~33 Mt/yr, mica ~380 kt/yr. They come from pegmatites (mica, feldspar), mature quartz sands/sandstones (silica) and alaskite/granite. Geologically they are common everywhere, so they matter little for rarity balance.

### Cited Findings
- World production 2024: industrial sand and gravel ~440 Mt; feldspar and nepheline syenite ~33 Mt; mica (scrap and flake) ~380 kt (China ~80 kt, Madagascar ~85 kt) — [USGS MCS 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf)

### Inferences
- Rules **[background, unverified this session]**: mica and feldspar come from granitic pegmatites in collision belts and at granite margins (same generator as tourmaline). Silica comes from quartz-rich beach, eolian and fluvial sands and quartzites (maturity = recycling count in the sediment module).
- Block mapping: no ore blocks. Silica sand → TFC white sand. Pegmatite could be an aesthetic dike using TFC `granite`.

### Gaps
- No deposit-model sources were retrieved for mica, feldspar or high-purity quartz.

---

## Pyrite (shales, coal, hydrothermal)

### Takeaway
Pyrite is the most widespread sulfide. It forms diagenetically in organic-rich marine muds via bacterial sulfate reduction (framboids, smallest in euxinic water columns), in coal mostly where marine sulfate reached the peat, and in essentially every hydrothermal system. TFC treats pyrite as a gem/"fake gold" in igneous rocks.

### Cited Findings
- Framboid diameters average 5.0 ± 1.7 µm in modern euxinic settings versus 7.7 ± 4.1 µm in oxic and dysoxic settings. Water-column framboids have narrower size ranges — [GSA 2002 abstract](https://gsa.confex.com/gsa/2002NC/webprogram/Paper33092.html). Hydrodynamics can weaken this proxy — [EGU25-18](https://meetingorganizer.copernicus.org/EGU25/EGU25-18.html)
- In Everglades peat, pyrite formation depends on organic oxysulfur compounds used by sulfur-reducing bacteria. This explains sulfur distribution in low-sulfur coals — [USGS, Sulfur diagenesis in Everglades peat](https://pubs.usgs.gov/publication/70011298)
- In coal, the main pyrite deposition is linked to influx of marine-water sulfate from overlying marine bands, which penetrates down into the peat and seat-earth — [Alton marine band study (Cambridge)](https://www.cambridge.org/core/product/8318EAA753226ED26ED1BA0131E4B830); [GSA 2003 abstract](https://gsa.confex.com/gsa/2003AM/webprogram/Paper65920.html)
- TFC today: `fake_native_gold` = pyrite clusters (rarity 16, size 15, y −50..70, density 0.35) in igneous extrusive and intrusive rocks, in the same areas as native gold. Pyrite is also the rare placer drop from tuff, rhyolite, basalt, andesite, dacite, slate, phyllite and schist — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
- Rules:
  - Sedimentary pyrite: in each marine shale layer, pyrite abundance ∝ organic carbon × seawater sulfate availability (gate on `atmosphereO2` / oxygenated ocean; **[background, unverified this session]**, sulfate-poor Archean oceans gave less BSR pyrite). Euxinic-basin layers are richest (black shales). Place disseminated or nodular pyrite in shale, slate and phyllite.
  - Coal pyrite: coal seams overlain by marine transgressive shales get high pyrite; freshwater-overlain seams get low pyrite.
  - Hydrothermal: halo pyrite around every hydrothermal deposit (porphyry pyrite shells, epithermal, VMS, orogenic gold). Keep TFC's "fool's gold" decoy near gold.
- Rarity: ubiquitous.
- Block mapping: TFC `ore/pyrite/<rock>`. Vanilla has no pyrite.

### Gaps
- No quantitative coal-sulfur percentages were sourced.

---

## Relative global abundance (USGS MCS 2025, 2024 production): quick reference for game balance

### Takeaway
Annual production spans about seven orders of magnitude, from industrial sand (~440 Mt), bauxite (~450 Mt) and gypsum (~160 Mt) down to a single defunct cryolite mine and one tanzanite district. For game balance, use the deposit-count rarity and the tonnage per deposit, not only production.

### Cited Findings
All values are from [USGS Mineral Commodity Summaries 2025](https://pubs.usgs.gov/periodicals/mcs2025/mcs2025.pdf) (2024 estimates; values carrying superscript footnote digits in the PDF were read without them):

| Commodity | World 2024 | Notable share |
|---|---|---|
| Bauxite | ~450 Mt | Guinea 130, Australia 100, China 93 Mt; reserves 29 Gt |
| Industrial sand & gravel | ~440 Mt | — |
| Salt | ~280 Mt | — |
| Gypsum | ~160 Mt | — |
| Sulfur (all forms) | ~85 Mt | mostly by-product of oil/gas |
| Kaolin / bentonite / fuller's earth | ~44 / ~21 / ~14 Mt | — |
| Feldspar + nepheline syenite | ~33 Mt | — |
| Manganese ore | ~20 Mt | — |
| Fluorspar | ~9.5 Mt | China ~5.9 Mt |
| Barite | ~8.2 Mt | India, China lead |
| Talc + pyrophyllite | ~6.9 Mt | — |
| Nickel (contained) | ~3.7 Mt | Indonesia ~2.2 Mt (laterite) |
| Diatomite | ~3.0 Mt | US 29%, Denmark 18% |
| Graphite (natural) | ~1.6 Mt | China 1.27 Mt |
| Asbestos | ~1.2 Mt | — |
| Wollastonite | ~1.1 Mt | China ~0.8 Mt |
| Natural zeolites | ~1.0 Mt | — |
| Industrial garnet | ~0.98 Mt | — |
| REE (REO) | ~390 kt | China 270 kt; Burma 31 kt |
| Mica (scrap/flake) | ~380 kt | — |
| Cobalt | ~290 kt | Congo (Kinshasa) 76%, Indonesia 10% |
| Iodine | ~33 kt | Chile leading (caliche) |
| Gem diamonds | ~72 million carats | Russia, Botswana, Canada |
| Kyanite group | no world total | S. Africa andalusite 150 kt |
| Boron | no world total | Turkey refined borates ~3.0 Mt; Turkey reserves 950 Mt |

### Inferences
- Suggested rarity tiers (deposits per typical world, design proposal):
  - Ubiquitous: pyrite, clay/kaolin, silica sand.
  - Common: gypsum, bauxite (in tropics), Ni laterite (on tropical ophiolites), graphite (in high-grade belts), fluorite/barite, talc.
  - Regional: supergene Cu (over porphyries), REE clays, wollastonite, garnet placers, borates, zeolites/bentonite, diatomite, basalt sapphire fields.
  - Rare: marble ruby belts, schist emerald, opal fields, nephrite, amethyst-geode LIP flows, salt-dome sulfur, caliche nitrate.
  - Very rare/unique: jadeitite (gem), Colombian-type emerald, lapis (Sar-e-Sang-type), imperial topaz, tanzanite, cryolite (Ivigtut-type).

### Gaps
- No production data for coloured gemstones by species (USGS reports gem value aggregates and gem diamond carats only).

---

## TFC compatibility summary: current TFC placement vs. geology, and recommended mapping

### Takeaway
TFC 1.21.x places most of these minerals by rock category and y-band, with a few climate and biome hooks: kaolin climate gate, collisional-mountain ruby belts, volcano fissures for diamond/topaz/sapphire/emerald, river-only amethyst/opal, and placer "deposit" gravels with rare gem drops. Several placements contradict real geology: deep garnierite, emerald pipes in any intrusive, lapis in limestone, diamond pipes in gabbro, and cryolite as common deep granite ore. A physically simulated history can replace them while reusing the same TFC ore blocks.

### Cited Findings
- TFC ore and mineral list (1.21.x): native copper, native gold, hematite, native silver, cassiterite, bismuthinite, garnierite, malachite, magnetite, limonite, sphalerite, tetrahedrite (graded poor/normal/rich), plus bituminous coal, lignite, gypsum, graphite, sulfur, cinnabar, cryolite, saltpeter, sylvite, borax, halite, amethyst, diamond, emerald, lapis lazuli, opal, pyrite, ruby, sapphire, topaz. Cinnabar and cryolite both yield "redstone" — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)
- Vein parameters (rarity, size, min_y, max_y, density, rocks):
  - graphite: 20, 20, −30, 60, 0.4, gneiss/marble/quartzite/schist
  - garnierite: 25/20, 18/30, −80, 0, igneous intrusive/gabbro
  - cryolite: 16, 18, −70, −10, 0.7, granite/diorite
  - sulfur: 4, 18, −64, −45, near lava (also tuff 40..200)
  - saltpeter: 110, 35, 40, 100, sedimentary
  - sylvite: 60, 35, 40, 100, shale/claystone/chert
  - borax: 40, 23, 40, 100, claystone/limestone/shale
  - gypsum: 70, 25, 40, 100, sedimentary
  - halite: 110, 35, −45, −12, sedimentary
  - lapis: 30, 30, −20, 80, limestone/marble
  - diamond: pipe, 30, 60, −64, 100, gabbro
  - emerald: pipe, 80, 60, −64, 100, igneous intrusive
  - amethyst and opal: river-only discs, y 40..60
  - deep_ruby: 80, 22, −70, −10, marble
  - Source: [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)
- Special features:
  - `ruby_marble_belt` in collisional-mountain biomes.
  - `kaolin_disc` gated by climate (groundwater ≥300, T ≥18) and biome list.
  - Volcano/stratovolcano/tuya lava fissures decorated with diamond or topaz; spring-water fissures decorated with sapphire or emerald; sapphire hot springs.
  - A basalt/quartzite geode with amethyst.
  - Source: [TFC world_gen.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/world_gen.py)
- Placer "deposit" blocks exist for native copper, cassiterite, native silver and native gold in each rock's gravel. Their loot includes a 5.33% chance of a rock-specific rare gem:
  - granite → topaz; diorite → emerald; gabbro → diamond
  - shale → borax; claystone → amethyst; limestone → lapis; conglomerate → lignite; dolomite → amethyst
  - chert → ruby; chalk → sapphire; volcanics and slate/phyllite/schist → pyrite
  - quartzite → opal; gneiss → gypsum; marble → lapis
  - Source: [TFC assets.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/assets.py)
- TFC defines soil variants including `oxisol` (tropical laterite soil) and `aridisol` — [TFC constants.py](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/resources/constants.py)

### Inferences
Recommended mapping table (design proposal):

| Real deposit | Simulation trigger (summary) | TFC block | Vanilla block |
|---|---|---|---|
| Ni laterite (saprolite) | ophiolite/ultramafic + tropical weathering integrator + uplift | `garnierite` (graded), host `gabbro` | none |
| Laterite Fe cap / gossan | mature profile, O₂ atmosphere / exposed sulfide | `limonite` (poor) | iron_ore |
| Bauxite (lateritic/karst) | Al-rich parent or karst unconformity + high maturity | none (custom or none); `oxisol` soil | none |
| Supergene Cu oxide / chalcocite blanket | exhumed porphyry + semi-arid + falling water table | `malachite` rich / `tetrahedrite` rich (stand-in), `native_copper` | copper_ore |
| REE ion-adsorption clay | granite + subtropical humid + low hills | none (kaolin clay visual) | none |
| Kaolin | feldspathic parent weathering / granite cupola | kaolin clay | clay |
| Residual/lateritic Au; placers | Au deposit + weathering; erosion → rivers | `native_gold` poor; `deposit/native_gold` | gold_ore |
| Flake/amorphous graphite | C-rich protolith + ≥upper amphibolite / coal + low grade | `graphite` in gneiss/schist/marble/quartzite | coal_ore (loose) |
| Kyanite/sillimanite/andalusite, garnet, wollastonite, talc, asbestos, jade | P–T path, contact aureoles, ophiolite serpentinite, HP-LT mélange | none | none |
| Marble ruby (+spinel) | collision belt, evaporitic platform carbonate, ~620–670 °C | `ruby` in marble | none |
| Basalt sapphire (placer) | intraplate alkali basalt field + erosion | `sapphire` (basalt / placer gravel) | none |
| Schist emerald / Colombian emerald | Be-granite × ultramafic contact / black shale + evaporite thrusts | `emerald` in schist/shale | emerald_ore |
| Lapis lazuli | dolomitic marble + granitoid + upper amphibolite | `lapis_lazuli` in marble | lapis_ore |
| Topaz | F-rich granite greisen/pegmatite; topaz rhyolite | `topaz` in granite/rhyolite | none |
| Opal | weathered ash/pyrite-rich sediments under cover; rhyolitic ignimbrite pedogenesis | `opal` | none |
| Amethyst/agate geodes | flood-basalt flows + low-T groundwater | `amethyst` geode in basalt | amethyst geode |
| Cryolite | A-type/peralkaline granite cupola in rift (≤1 major per world) | `cryolite` in granite | redstone_ore |
| Sulfur | salt-dome cap rock + hydrocarbons; volcanic fumaroles | `sulfur` (tuff, cap-rock host) | none |
| Saltpeter | hyperarid caliche ≥Myr; limestone caves | `saltpeter` | none |
| Borax | closed arid basin + volcanism | `borax` | none |
| Gypsum | evaporites, cap rocks | `gypsum` | none |
| Pyrite | black shales, marine-roofed coal, all hydrothermal halos | `pyrite` | none |
| Fluorite, barite, mica, feldspar, silica, diatomite, bentonite, zeolite, Mn, tourmaline, peridot, spinel, tanzanite | as above | none | none (bentonite → clay; silica → sand) |

- Because TFC ore blocks are per-rock (`tfc:ore/<ore>/<rock>`), every simulated deposit must resolve to one of the 21 TFC rocks. Ultramafic → gabbro is the main forced approximation.
- TFC uses y-bands as a proxy for geology (e.g., "deep = intrusive"). In a simulated column, depth below the local surface and the actual layer lithology should replace the absolute y-bands. Keep TFC's "indicator" mechanic (surface loose rocks or small ores above veins) for gossans and laterite caps.
- Keep TFC's gameplay floors even where geology is stingy: an early-game copper source near spawn, and the redstone path (cryolite/cinnabar). A fallback "minimum guarantee" pass is advisable.

### Gaps
- I checked the 1.21.x development branch, not the released 1.21.1 jar. Parameter values and features such as `ruby_marble_belt`, soil orders and stratovolcano fissures may not be in the released version.
- Addon blocks (e.g., aluminium/bauxite in DecoFirmaCraft or TFC IE Minerals) were not inventoried. DecoFirmaCraft reportedly makes aluminium from alumina with cryolite in a blast furnace — [mado-hosting DecoFirmaCraft page](https://www.mado-hosting.com/minecraft-mods/decofirmacraft)
