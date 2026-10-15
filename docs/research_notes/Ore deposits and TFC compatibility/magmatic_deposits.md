# Orthomagmatic and Igneous-Related (Non-Hydrothermal-Dominated) Ore and Gem Deposits

Scope: magmatic deposit types, catalogued so that simulation rules can be derived from them. The mod simulates plate tectonics, plumes and LIPs, cratons, erosion, stratigraphy and the atmosphere/life timeline, and should be TFC-compatible.

Method note: research ran on 2026-10-15. Primary sources used: USGS Bulletin 1693 descriptive models (pages fetched directly), USGS SIR/OFR deposit models, the BC Geological Survey carbonatite review, the Kjarsgaard GAC kimberlite synthesis, and TFC 1.21.x source files fetched from GitHub. Some details come only from search-engine excerpts or Wikipedia, and these are marked. The shared web-search budget ran out partway through, so several facts are left as Gaps or labelled "background knowledge (unverified)" in the Inferences sections. Simulation rules and block mappings are design inferences, not literature facts.

---

## Q0. Which TFC (1.21.x) and vanilla blocks can magmatic deposits map to? (baseline for all sections)

### Takeaway
TFC 1.21.x has no ultramafic, kimberlite, anorthosite, carbonatite, syenite or pegmatite rock types. Its only intrusive rocks are granite, diorite and gabbro, and TFC already uses gabbro as the stand-in host for both diamond pipes and Ni (garnierite). Chromite, PGE, Ti, V, Nb, REE, Li, Ta, W and fluorite have no ore blocks. Gems that do exist and fit magmatic settings: diamond, emerald, topaz, sapphire, opal, amethyst and pyrite.

### Cited Findings
- The TFC 1.21.x `Ore` enum contains these entries. Graded ores: NATIVE_COPPER, NATIVE_GOLD, HEMATITE, NATIVE_SILVER, CASSITERITE, BISMUTHINITE, GARNIERITE, MALACHITE, MAGNETITE, LIMONITE, SPHALERITE, TETRAHEDRITE. Normal: GYPSUM, CINNABAR, CRYOLITE, BORAX. With powder: GRAPHITE, SALTPETER, SULFUR, SYLVITE. Gems: AMETHYST, DIAMOND, EMERALD, LAPIS_LAZULI, OPAL, PYRITE, RUBY, SAPPHIRE, TOPAZ. Item-only: BITUMINOUS_COAL, LIGNITE, HALITE. — [TFC Ore.java, 1.21.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/common/blocks/rock/Ore.java)
- The TFC 1.21.x vein folder contains: amethyst, bituminous_coal, borax, cryolite, deep_ruby, diamond, diorite_dike, emerald, fake_native_gold, gabbro_dike, gabbro_garnierite, granite_dike, graphite, gravel, gypsum, halite, kaolin_disc, lapis_lazuli, lignite, montane_* (bismuthinite, cassiterite, cinnabar, hematite, limonite, magnetite, malachite, native_copper, native_silver, sphalerite, tetrahedrite), normal_* (bismuthinite, cinnabar, garnierite, malachite, native_gold, native_silver, sphalerite, tetrahedrite), opal, rich_native_gold, ruby_marble_belt, saltpeter, sulfur, surface_* (bismuthinite, hematite, limonite, magnetite, malachite, native_copper, sphalerite), sylvite, tuff_sulfur. There is no vein file named for sapphire, topaz or pyrite, although those gem ore blocks exist. — [TFC vein configured_feature folder, 1.21.x](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein)
- TFC rock types (taken from the replace list of `gabbro_dike`): granite, diorite, gabbro, shale, claystone, limestone, conglomerate, dolomite, chert, chalk, tuff, rhyolite, basalt, andesite, dacite, quartzite, slate, phyllite, schist, gneiss, marble. — [gabbro_dike.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/gabbro_dike.json)
- TFC diamond is a `tfc:pipe_vein` (rarity 30, density 0.15, y −64 to 100, height 60, radius 5) that replaces **only gabbro**. — [diamond.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/diamond.json)
- TFC emerald is a pipe_vein (rarity 80, same geometry) in granite, diorite and gabbro. — [emerald.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/emerald.json)
- TFC `gabbro_garnierite` is a cluster vein (rarity 20, density 0.6, y −80 to 0, size 30). It is gabbro-only and weighted 60% rich ore. `normal_garnierite` uses granite, diorite and gabbro. — [gabbro_garnierite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/gabbro_garnierite.json); [normal_garnierite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/normal_garnierite.json)
- TFC amethyst is a disc_vein (y 40–60) in sedimentary and metamorphic rocks plus tuff. **Basalt is not a host.** TFC opal is a disc_vein (y 40–60) in sedimentary rocks plus tuff, rhyolite, basalt, andesite and dacite. — [amethyst.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/amethyst.json); [opal.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/opal.json)
- TFC has two sulfur veins. `tuff_sulfur` is a disc vein in tuff (y 40–200, rarity 2). `sulfur` is a deep disc vein (y −64 to −45) in intrusive and metamorphic rocks. Cryolite is a cluster vein in granite and diorite (y −70 to −10). `montane_cassiterite` is in granite, diorite and gabbro (y 80–300). — [tuff_sulfur.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/tuff_sulfur.json); [cryolite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/cryolite.json); [montane_cassiterite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/montane_cassiterite.json)
- TFC vein configs can carry an `indicator` block (surface and underground "small ore" markers). Garnierite uses `tfc:ore/small_garnierite`. — [gabbro_garnierite.json](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/gabbro_garnierite.json)

### Inferences
- **Host-rock proxy table** (design proposal): ultramafic rocks (dunite, harzburgite, peridotite, komatiite, kimberlite, pyroxenite) → `gabbro`, or a new "peridotite/serpentinite/kimberlite" block if the mod may add rocks. Anorthosite → gabbro. Carbonatite → marble or limestone stand-in, or a new block. Syenite → diorite. Pegmatite → granite. Obsidian → vanilla `minecraft:obsidian`. Komatiite flows → basalt.
- **Commodity proxy table** (design proposal): Ni sulfide → TFC garnierite (TFC's only Ni ore; really a lateritic Ni-silicate, see the supergene researcher). Cu by-product → malachite in gossan caps, or a new chalcopyrite block. Fe-Ti-V magnetite → TFC magnetite or vanilla iron. Chromite, PGE, ilmenite, pyrochlore, bastnäsite, spodumene, tantalite, wolframite, fluorite → **new blocks needed**. Gems map directly (diamond, emerald, topaz, sapphire, opal, amethyst, pyrite). Vanilla amethyst geodes are a near-perfect analogue for basalt-hosted amethyst geodes.
- The TFC `pipe_vein` feature (vertical cylinder, radius 5, height 60, with skew and slant) is a ready-made geometry for kimberlite pipes and for pipe-like Ni-Cu or ilmenite bodies.

### Gaps
- How TFC 1.21.x places sapphire, topaz and pyrite ore blocks is unknown. No vein config was found in the folder, so they may come from other features or datapacks. Not verified.

---

## Q1. Podiform (ophiolite, "Alpine-type") chromite

### Takeaway
These are small, irregular chromitite pods in dunite envelopes inside the mantle (harzburgite) section of ophiolites, close to the mantle–crust transition. They are numerous but small (median ~11 kt for "major" deposits). They only survive at the surface where ocean lithosphere has been obducted onto a continent along a suture, and they hold only a small share of world chromite reserves.

### Cited Findings
- Description: "Podlike masses of chromitite in ultramafic parts of ophiolite complexes." Hosts are highly deformed dunite and harzburgite, commonly serpentinized. Ore is "restricted to dunite bodies in tectonized harzburgite or lower portions of ultramafic cumulate." Age range "Phanerozoic". The ores are "magmatic cumulates in elongate magma pockets along spreading plate boundaries" that are later "exposed in accreted terranes as part of ophiolite assemblage." Mineralogy: chromite ± ferrichromite ± magnetite ± Ru-Os-Ir alloys ± laurite. "Highly resistant to weathering and oxidation." No geochemical signature recognized. — [USGS B1693 Model 8a/8b](https://pubs.usgs.gov/bul/b1693/html/bull3lr9.htm)
- USGS 2012 database: 1,124 deposits, 619 used in models. Major podiform model: median tonnage 11,000 t, mean grade 45% Cr2O3. Minor: median 100 t, 43% Cr2O3. Banded: median 650 t, 42% Cr2O3. The models apply "for any ophiolite complex regardless of its tectonic setting or ophiolite zone." The banded model applies in both suprasubduction-zone (SSZ) and mid-ocean-ridge settings. Most deposits lie "in dunite or peridotite near the contact of the cumulate and tectonite zones." — [USGS SIR 2012-5157](https://pubs.usgs.gov/publication/sir20125157)
- PGE in podiform chromite: Pt grade correlates with chromite grade (r = 0.76) and with Ir (r = 0.71). The "major" deposit list is dominated by Turkey, Cuba, New Caledonia, the Philippines and Iran. — [USGS B1693 major podiform G-T](https://pubs.usgs.gov/bul/b1693/html/bull20o5.htm)
- Minor podiform deposits (California and Oregon) are reported mostly at shipping grades. Grades below 35% typically represent in-place ore. — [USGS B1693 minor podiform G-T](https://pubs.usgs.gov/bul/b1693/html/bull3zhh.htm)
- Stratiform deposits supply "98% of the worldwide chromite reserves." Podiform resources are mainly in Kazakhstan, Turkey and Albania. Podiform nodular ore texture has 5–20 mm nodules. — [Wikipedia: Chromite](https://en.wikipedia.org/wiki/Chromite) (secondary source)

### Inferences
- **Commodities:** Cr (chromite; metallurgical and refractory grades), with by-product Os-Ir-Ru (PGE). Associated deposit types: Limassol-Forest Co-Ni-As, serpentine-hosted asbestos and magnesite, and lateritic Ni (garnierite) on the weathered peridotite (supergene; another researcher covers this).
- **Setting and timing (partly background knowledge, unverified):** current consensus is that most economic podiform chromitites formed in SSZ (fore-arc or back-arc) mantle. Boninitic or high-Mg melts react with harzburgite and produce dunite channels in which chromite precipitates. Ophiolites are then obducted during arc–continent collision, typically within about 0–20 Myr of formation. The 1986-era USGS text favoured mid-ocean ridges. This is a **flagged controversy**: the 2012 USGS model deliberately stays setting-agnostic.
- **Secular distribution:** USGS gives "Phanerozoic". Background knowledge: ophiolites become common after ~1 Ga, especially in the Neoproterozoic and Phanerozoic, and older ones are rarely preserved.
- **Proposed simulation rule:**
  - Inputs: `ophiolite_obduction` events (oceanic lithosphere thrust onto a continental margin during collision or arc accretion); `sz_spreading` flag (oceanic crust created in a fore-arc or back-arc above a subducting slab gets a weight of ~3× relative to MOR crust); `erosion_depth` (the mantle section must be exhumed, i.e. eroded below the ophiolite crust, roughly 5–7 km nominal by background knowledge); and age of the ophiolite.
  - Placement: along suture belts, inside "peridotite" layers of the obducted nappe. Put pods within the upper ~1–2 km of the mantle section (unverified) as lenses about 1–100 m long (unverified). Give each ophiolite cell a Poisson number of pods, e.g. λ ≈ 0.5–3 per 10 km² of exposed mantle section. Tonnage follows a lognormal with median 100 t (minor) to 11 kt (major), with a rare tail.
  - Spatial pattern: linear chains along collisional sutures (Tethyan, Uralian or circum-Pacific style), never in cratonic interiors.
  - Preservation: chromite resists weathering, so it should also feed **chromite placers** and residual soil blocks.
- **Block mapping:** no TFC or vanilla chromite → **new block "chromite ore"** hosted in a peridotite or serpentinite stand-in (gabbro in TFC). The weathered cap over the same ophiolite could carry TFC garnierite (lateritic Ni).

### Gaps
- Quantitative pod dimensions, depth below the petrologic Moho, and SSZ vs MOR proportions were not fetched; the full SIR 2012-5157 PDF was not read.
- Age-frequency of ophiolites through time was not fetched.

---

## Q2. Stratiform chromite and PGE reefs in layered mafic–ultramafic intrusions (Bushveld, Stillwater, Great Dyke)

### Takeaway
A handful of giant Precambrian layered intrusions on stable cratons hold nearly all the world's chromite and PGE. The ore occurs as laterally continuous (tens to >100 km), thin (cm to ~1 m) layers at specific stratigraphic levels: chromitite in the Lower and Critical zones, PGE reefs near new-magma influx horizons, and V-magnetite near the top. These are extremely rare (a few per planet) but enormous.

### Cited Findings
- Bushveld Cr model: "Layered chromitite in lower intermediate zone of large repetitively layered mafic-ultramafic intrusions." Age "Generally Precambrian, but may be as young as Tertiary." Tectonic setting "Cratonal, mostly in Precambrian shield areas," intruded into granitic gneiss or volcanic-sedimentary terrane. "Thickness of chromite increases in basinal depressions in layering." Weathering leaves "Abundant blocks of chromitite in soil and alluvium." Examples: Bushveld, Stillwater, Great Dyke. — [USGS B1693 Model 2a](https://pubs.usgs.gov/bul/b1693/html/bull8c6q.htm)
- Merensky Reef PGE model: disseminated PGE-rich sulfides "in layers near first reappearance of olivine as a cumulate phase after thick accumulation of plagioclase pyroxene rocks. May be related to introduction of new magma." "Difficult to see ore zone on weathered surface." Associated with PGE placers. — [USGS B1693 Model 2b](https://pubs.usgs.gov/bul/b1693/html/bull75et.htm)
- Stillwater Ni-Cu model: Ni-Cu sulfides at the base of large layered intrusions, in "basins in basal contact of intrusion with rapidly varying lithologies". "Ingress of sulfur through fractures in footwall may be important ore control". Forms gossans. — [USGS B1693 Stillwater Ni-Cu](https://pubs.usgs.gov/bul/b1693/html/bull46k5.htm)
- Bushveld Complex: covers >66,000 km², up to 9 km thick, with the Rustenburg Layered Suite c. 8 km thick. Zones are Marginal, Lower, Critical, Main and Upper. Critical Zone chromitites are LG1–LG7, MG1–MG4 and UG1–UG2 (13 seams; up to 25 layers recognized). Merensky Reef is 30–90 cm. The Upper Zone has 24 major massive magnetite layers up to ~6 m thick. The complex holds ~75% of world Pt and ~50% of Pd resources, and Merensky + UG2 hold ~90% of known PGM reserves. Age given as "approximately two billion years". — [Wikipedia: Bushveld Igneous Complex](https://en.wikipedia.org/wiki/Bushveld_Igneous_Complex) (secondary)
- The UG2 lies 15–400 m below the Merensky. The reefs are "~1 m thick, 5–8 ppm PGE, >100 km strike". UG2 averages ~1 m (0.4–2.5 m). Merensky thickness has been reported from 4 cm to 4 m, or up to 20 m, averaging ~1 m, a value defined by grade distribution rather than lithology. The Bushveld hosts ~75% of global PGE resources (Mudd 2012). — search excerpts from [PMC review](https://pmc.ncbi.nlm.nih.gov/articles/PMC7831339/), [Mineralium Deposita 2018](https://link.springer.com/article/10.1007/s00126-018-0800-x), [CRIRSCO Merensky case study](https://crirsco.com/wp-content/uploads/2024/04/9_Case_Study_Merensky_Reef_K_Lomberg.pdf). Exact attribution of each number to each source was not verified, and a popular blog gives thinner values (30–90 cm Merensky, <50 cm UG2).
- Great Dyke: 2.575 Ga, ~550 km long, 3–12 km wide, lopolithic and Y-shaped in section, with four sub-chambers fed by a feeder dyke. Chromitite seams form the bases of cyclic units in the Ultramafic Sequence. The Main Sulphide Zone has a PGE subzone. It lies in the Zimbabwe Craton and has been undeformed since intrusion. — [Wikipedia: Great Dyke](https://en.wikipedia.org/wiki/Great_Dyke) (secondary)
- Stratiform chromite layers are 1 cm to 1 m thick, laterally up to 70 km. — [Wikipedia: Chromite](https://en.wikipedia.org/wiki/Chromite)
- Carbonate (dolomite) contamination in the Platreef (northern Bushveld) is generally regarded as essential or beneficial to Ni-Cu-PGE mineralization, either by adding S or by raising fO2. — [McFall et al., UCL repository](https://discovery.ucl.ac.uk/id/eprint/10212496/7/McFall_The%20effects%20of%20carbonate%20contamination%20on%20Ni-Cu-PGE%20deposit%20genesis%20in%20the%20Platreef%2C%20northern%20Bushveld%20Complex_VoR.pdf)

### Inferences
- **Commodities:** Cr (chromitite), PGE (Pt, Pd, Rh, Ru, Ir, Os) with by-product Ni, Cu, Co and Au (reefs and Platreef), and V-Ti-Fe (Upper Zone magnetite; see Q7).
- **Tectonic setting and timing:** intracratonic, emplaced into thick, cold, already-stable cratons. Background knowledge (unverified here): Bushveld ~2.055 Ga, Stillwater ~2.7 Ga, Great Dyke 2.575 Ga (cited). Most are plume/LIP-related and emplaced in ≤1–2 Myr. There is a strong secular bias toward the Neoarchean–Paleoproterozoic (2.7–2.0 Ga). Smaller Phanerozoic analogues exist (USGS says ages "may be as young as Tertiary").
- **Depth and temperature:** mid- to upper-crustal sill/lopolith emplacement (background knowledge: a few km deep, mafic–ultramafic magma ~1200–1300 °C). Not verified here.
- **Proposed simulation rule:**
  - Inputs: `plume_head_arrival` or LIP event under a craton cell with `craton_age > ~0.5 Gyr` and `lithosphere_thickness > ~150 km`; `intrusion_volume` from the LIP module (need ≥ 10⁵ km³ magma); craton must be unextended (rigid). Probability is strongly time-weighted: multiply by ~5× if 2.8–2.0 Ga, 1× for 2.0–0.5 Ga, 0.3× in the Phanerozoic.
  - Geometry: a funnel- or sheet-shaped intrusion (radius 50–200 km for a Bushveld analogue; dyke-like 3–12 km × ≥500 km for a Great Dyke analogue), internally layered. Assign stratigraphic "zones" by height fraction: Lower (0–20%) gets chromitite layers; Critical (20–40%) gets chromitite seams plus a PGE reef at the top; Main (40–75%); Upper (75–100%) gets magnetite layers. Each ore layer is 0.3–2 m thick (scale ×N for Minecraft visibility) and continuous for tens of km. Thicken chromitite in synclinal depressions.
  - Preservation and exposure: layers appear as concentric outcrop rings in a basin; erosion depth decides which zone outcrops. Over-erosion (>~8 km) removes the intrusion entirely.
  - Expected pattern: 0–3 per world, each a large ring- or trough-shaped province in an old craton.
- **Block mapping:** chromitite → new chromite block. PGE reef → new "PGE/sperrylite" block, or in TFC a pyroxenite-hosted reef containing TFC garnierite plus pyrite as the visible Ni-sulfide proxy. Magnetite layers → TFC magnetite or vanilla iron (see Q7). Basal Ni-Cu (Stillwater/Platreef-type) → TFC garnierite in gabbro, which TFC already does. Chromitite and PGE placers downstream need new blocks.

### Gaps
- Exact Bushveld age, emplacement duration and emplacement depth were not retrieved from a primary source.
- Stillwater J-M Reef grade and thickness were not retrieved.
- No global count of layered intrusions by age was found.

---

## Q3. Magmatic Ni-Cu-(PGE) sulfides: general genetic controls (applies to Q4–Q6)

### Takeaway
Sulfide ore needs four things. (1) A large volume of mantle-derived mafic or ultramafic magma. (2) Sulfide saturation, usually triggered by assimilation of crustal S (black shale, evaporite, sulfidic sediment) or of silica or carbonate. (3) Interaction of the sulfide liquid with a lot of magma (high R-factor) so it picks up metal. (4) Physical traps such as conduit bends, footwall embayments, or the base of flows or intrusions. Conduits and small intrusions in feeder systems dominate the very large deposits.

### Cited Findings
- Naldrett (1999): sulfide saturation is "necessary but not sufficient". The sulfide liquid must mix with enough magma to become enriched in chalcophile metals and then be concentrated. Most non-MORB basaltic magmas are not S-saturated when they leave the mantle, so a trigger is needed in specific magma batches. — [Naldrett 1999, Mineralium Deposita (search excerpt)](https://pages.mtu.edu/~raman/papers2/NaldrettNiCuPGEMD99.pdf)
- Usual saturation mechanisms are crustal contamination, fractional crystallization and magma mixing. Assimilation can push S content past capacity, and added silica lowers S solubility (Li & Naldrett 1993). At Aguablanca (Spain), assimilation of pyrite-rich black slate was the key factor. — [Tejadillas/Aguablanca study, UCM (search excerpt)](https://docta.ucm.es/bitstreams/112f1fcc-e3d0-41a7-bfb6-66abb3b1d93b/download)
- Song et al. (2011) name three key controls: large volumes of mantle-derived magma, fractional crystallization plus crustal contamination (especially crustal S), and timing of sulfide concentration. They note that most super-large deposits other than Sudbury occur in **small** mafic–ultramafic intrusions, and several large Chinese deposits sit in magma conduits. — [Song et al. 2011, Geoscience Frontiers (catalog record)](https://katalog.hcu-hamburg.de/vufind/Search2Record/DOAJ037909029)
- For Jinchuan, researchers disagree whether saturation came from crustal sulfur or from oxidation linked to carbonate assimilation. — [Xue et al., Economic Geology (abstract)](https://www.citedrive.com/en/discovery/the-roles-of-various-types-of-crustal-contamination-in-the-genesis-of-the-jinchuan-magmatic-ni-cu-pge-deposit-new-mineralogical-and-c-s-sr-nd-isotope-constraints)
- Duluth-type: associated with "pyritic shale, anhydrite, or recognizable source of sulfur to contaminate magma". Ore controls are syn-intrusion faulting forming basins at the base of the intrusion, an external S source, and silicic contaminant. "Sulfur isotopes show non-magmatic sulfur". Ni/Cu ≈ 1/3. Rift setting. — [USGS B1693 Model 5a](https://pubs.usgs.gov/bul/b1693/html/bull6811.htm)

### Inferences
- **Generic simulation kernel** (reuse for Q4–Q6):
  `P(ore) ∝ V_magma × MgO_factor × S_crust_availability × trap_factor × R_factor_proxy`
  - `V_magma`: mafic or ultramafic melt flux through the cell, from plume/LIP/rift events.
  - `S_crust_availability`: from the stratigraphic column, the presence and thickness of **sulfidic black shale**, **evaporite (anhydrite/gypsum)**, coal or carbonate within ~0–10 km depth at intrusion time. Tie this to the atmosphere timeline. Before the Great Oxidation Event (~2.4 Ga) marine sulfate evaporites are rare, so the contaminant is sulfidic shale or sediments. After the GOE, and especially in the Neoproterozoic–Phanerozoic, evaporite anhydrite becomes a major S source (Noril'sk). The GOE dependence is an inference, not a cited claim.
  - `trap_factor`: high for conduits and feeder sills at the transition from dyke to sill, for footwall embayments, and for the basal contact of komatiite channels.
  - `R_factor_proxy`: flow-through magma volume ÷ sulfide volume. High in long-lived conduits.
- Ore bodies should sit at the **base** of the intrusion or flow and in **footwall** fractures. Gossans form above them when uplifted and weathered.

### Gaps
- Quantitative R-factor values and S solubility (SCSS) numbers were not fetched.
- No global Ni-Cu deposit age histogram was fetched. The Begg et al. 2010 and Maier & Groves 2011 type syntheses were not retrieved.

---

## Q4. Komatiite-hosted (and dunitic) Ni-Cu-(PGE) sulfides (Kambalda, Mt Keith, Perseverance, Raglan, Thompson)

### Takeaway
Ni-rich sulfide ore sits at the base of thick, channelized komatiite flows and sills in greenstone belts. It is dominantly late Archean (~2.7 Ga), with a secondary Paleoproterozoic (~1.9 Ga) group and only rare Phanerozoic komatiites. Each deposit is small (Mt-scale), but districts cluster.

### Cited Findings
- USGS Model 6a: "Lenticular, irregular elongate to tabular, pipelike Ni-Cu sulfides associated with komatiitic volcanic extrusive rocks". Hosts contain ">15 percent and may approach 40 percent MgO". Age "Archean or Proterozoic generally, but some may be Cretaceous or Tertiary". Setting: greenstone belts. Ore controls: "lowermost flows more than 10 m thick", zones of increased spinifex development, near feeder areas, irregularities at the bottom of flows. The unit has ">1,000 ppm sulfur or is associated with sulfide-bearing chert and argillite". Shale or iron-carbonate sequences occur below flows. Weathering produces gossans and laterites. Gossans contain 15–30 ppb Pd and 5–10 ppb Ir over known deposits. — [USGS B1693 Model 6a](https://pubs.usgs.gov/bul/b1693/html/bull6sol.htm)
- Komatiitic Ni grade correlates negatively with tonnage (r = −0.47). The deposit list is dominated by Western Australia, Zimbabwe, Canada and Finland. — [USGS B1693 komatiitic G-T](https://pubs.usgs.gov/bul/b1693/html/bull2e5x.htm)
- Dunitic Ni (Model 6b; Mt Keith, Perseverance): disseminated sulfide in dunite lenses "500–1,000 m long, 50–100 m thick". High-grade ore is 1–9% Ni and low-grade 0.4–1% Ni. Massive ore is 6–9% Ni and disseminated ore up to 3% Ni. Ni/Cu = 19–70+. Precambrian, greenstone belts, close to major strike-slip faults. — [USGS B1693 Model 6b](https://pubs.usgs.gov/bul/b1693/html/bull27n9.htm)
- Kambalda-type ores: sulfides pool in lava-channel floors and in footwall embayments. Type I (basal) ores are massive (>90% sulfide), semi-massive (75–90%), net-textured (30–50%) and disseminated (5–30%). Type II ores extend 40–60 m into the footwall. Some host peridotite bodies are 300–1200 m thick. Metamorphic remobilization moves sulfide tens to hundreds of metres (>600 m at Emily Ann). A Kambalda Ratio (Ni:Cr/Cu:Zn) ≥1 indicates a fertile flow. Bekker et al. (2009) S isotopes suggest S came "upstream" rather than from local country rock. Yilgarn gossans are goethite/hematite boxworks. — [Wikipedia: Kambalda type komatiitic nickel ore deposits](https://en.wikipedia.org/wiki/Kambalda_type_komatiitic_nickel_ore_deposits) (secondary)
- The Kambalda Dome is part of the 2.7 Ga Kalgoorlie Terrane, and fertile komatiite sequences are generally late Archean (~2.70 Ga). Whether the embayments formed by thermal erosion (Staude, Barnes, Le Vaillant) or by primary volcanic topography and later folding/thrusting (Lesher 1989; Stone et al.) is **debated**. — search excerpts incl. [Monash record on komatiite erosion](https://research.monash.edu/en/publications/field-characteristics-and-erosional-processess-associated-with-ko/) and [Kambalda Wikipedia](https://en.wikipedia.org/wiki/Kambalda_type_komatiitic_nickel_ore_deposits)
- Tappe et al. (2018): ambient mantle potential temperature fell below 1400 °C after ~2 Ga, so volatile-fluxed (kimberlite) melting took over from komatiites as the typical intracratonic magma. — [Tappe et al. 2018 EPSL (UiO CEED summary)](https://mn.uio.no/ceed/english/research/publications/articles/2017/geodynamics-of-kimberlites-on-a-cooling-earth:-clu.html); [HAL record](https://hal.archives-ouvertes.fr/hal-03552804)

### Inferences
- **Commodities:** Ni (primary), with by-product Cu, Co, PGE (Pd, Pt, Ir) and Au.
- **Setting and timing:** plume-related komatiite volcanism in Archean greenstone belts and on rifted craton margins (Thompson, Raglan ~1.9 Ga, background knowledge). Komatiite eruption temperatures of ~1500–1650 °C are background knowledge and not verified here.
- **Proposed simulation rule:**
  - Inputs: `mantle_potential_temperature(t)` from the planet timeline. Komatiite production needs a hot plume, so the probability is high when t > 2.5 Ga, moderate for 2.5–1.8 Ga, near zero after ~1.8 Ga (allow rare Phanerozoic exceptions, e.g. ~0.01×). Also `plume_arrival` under thin or rifting lithosphere and `sulfidic_sediment_in_footwall` (chert, argillite, BIF, shale in the stratigraphy at eruption time).
  - Placement: within greenstone-belt volcanic sequences (basalt-dominated strata in the column). Ore lenses sit at the base of the lowest thick flow, as ribbons along channel axes. Each shoot is roughly 1–5 m thick × 20–200 m wide × 0.5–2 km long (unverified estimates consistent with the cited 40–60 m footwall extent). Dunitic Mt Keith-type disseminated bodies are 500–1000 m × 50–100 m (cited).
  - Post-processing: metamorphism (from the column's thermal history) can remobilize ore up to hundreds of metres along faults. Greenstone deformation folds the flows steeply.
  - Expected pattern: clusters of 5–30 small shoots per greenstone belt, in Archean cratons only. Gossans (limonite/goethite) appear at surface when exhumed.
- **Block mapping:** TFC garnierite (rich ore at the massive basal layer, normal and poor upward), with basalt (komatiite proxy) host or gabbro for intrusive dunite. Gossan cap → TFC limonite with some malachite. Vanilla: no Ni; map to copper/iron or skip.

### Gaps
- Primary tonnage/grade statistics (median Mt, %Ni) from the USGS G-T model figures could not be read (figures only).
- No authoritative count of komatiite occurrences by age was fetched.

---

## Q5. Conduit/intrusion-hosted and flood-basalt-related Ni-Cu-PGE (Noril'sk-Talnakh, Voisey's Bay, Duluth, Jinchuan, synorogenic types)

### Takeaway
The richest deposits sit in small, shallow, sill-like or chonolithic intrusions and feeder conduits beneath or within LIPs and rifts, where magma assimilated S-rich crust: evaporite and coal at Noril'sk, sulfide-bearing paragneiss at Voisey's Bay, pyritic shale at Duluth. Timing coincides with LIP or rift magmatism, within the ~1 Myr magmatic pulse.

### Cited Findings
- Noril'sk Model 5b: "Massive to disseminated sulfides in small shallow mafic to ultramafic intrusives with an external source of sulfur". Hosts are flood basalts, picritic intrusions, picritic gabbro, dolerite and breccias, "Associated with evaporites or some external source of sulfur". "Magma has intruded through evaporites or pyritic shale, and formed sills in flood basalts during active faulting." Rift setting. Ores form "persistent basal layers to intrusion and dike-like bodies into country rock" and occur in fault-bounded depressions. Ni/Cu = 1.5–0.5. — [USGS B1693 Model 5b](https://pubs.usgs.gov/bul/b1693/html/bull2g11.htm)
- Duluth-type (rift-related, base of large layered intrusions, external S, Ni/Cu ≈ 1/3). Age Precambrian to Tertiary(?). — [USGS B1693 Model 5a](https://pubs.usgs.gov/bul/b1693/html/bull6811.htm)
- Synorogenic-synvolcanic Ni-Cu (Model 7a): massive lenses and disseminated sulfide in small to medium gabbroic intrusions in greenstone, metamorphic and mobile belts. Age "Archean to Tertiary, predominantly Archean and Proterozoic". Sulfides sit in the ultramafic parts near the basal contacts. — [USGS B1693 Model 7a](https://pubs.usgs.gov/bul/b1693/html/bull5igl.htm)
- Voisey's Bay: discovered in September 1993 by Archean Inc. for Diamond Fields Resources. Estimated in 2007 at 141 Mt @ 1.6% Ni. — [Wikipedia: Voisey's Bay Mine](https://en.wikipedia.org/wiki/Voisey%27s_Bay_Mine) (secondary)
- The USGS anorthosite model notes Voisey's Bay Ni-Cu sulfides within a Proterozoic anorthosite (Nain) suite. Kerr & Ryan (2000) view anorthosite-hosted massive Ni-Cu sulfide as part of a spectrum of magma-conduit processes. — [USGS SIR 2010-5070-K (Fe-Ti anorthosite model)](https://pubs.usgs.gov/sir/2013/5091/SIR13-5091.pdf)
- Large Chinese deposits (e.g. Jinchuan) sit in small conduit intrusions. — [Song et al. 2011](https://katalog.hcu-hamburg.de/vufind/Search2Record/DOAJ037909029)

### Inferences
- **Commodities:** Ni, Cu, Pd, Pt, Co, Au, Ag, Rh, Te. Noril'sk is the world's top Pd source (background knowledge, unverified share).
- **Timing (background knowledge, unverified here):** Noril'sk ~251 Ma (Siberian Traps, end-Permian). Voisey's Bay ~1.33 Ga (Nain Plutonic Suite). Duluth ~1.1 Ga (Midcontinent Rift). Jinchuan ~0.83 Ga (Rodinia rifting).
- **Proposed simulation rule:**
  - Inputs: LIP or rift event with `V_magma > 10⁵ km³` (Noril'sk analogue) or a long-lived feeder (Voisey's analogue). Sedimentary basin under the LIP with **evaporite or anhydrite** layers (only after evaporite-forming climates exist; post-GOE sulfate) or **coal** (requires land plants, i.e. Devonian+) or sulfidic shale. Fault zones (rift-border faults) as conduit loci.
  - Placement: within ~0–5 km below the LIP base, where conduits cross S-rich strata. Bodies are elongated sills or "chonoliths" (≈0.1–0.3 km thick × 1–2 km wide × up to ~15 km long; background-knowledge dimensions for Talnakh, unverified). Massive sulfide at the base, disseminated above, "dike-like bodies into country rock".
  - Probability for a LIP: P ≈ 0.05–0.2 that at least one significant deposit forms (designer choice; real LIPs are mostly barren). P is multiplied ×3 if evaporite is present in the column and ×2 if coal is present.
  - Pattern: deposits occur along the margins and feeder faults of flood-basalt provinces, not in the lava pile interior.
- **Block mapping:** TFC garnierite in gabbro (TFC already has `gabbro_garnierite`), plus TFC pyrite as a visual sulfide marker, plus TFC malachite/limonite in the oxidized cap. A new chalcopyrite or pentlandite block would be more faithful. Vanilla: copper ore plus iron ore.

### Gaps
- Noril'sk and Jinchuan tonnages and grades, and the Noril'sk share of world Ni/Pd, could not be fetched (Wikipedia page not found; search budget exhausted).
- Voisey's Bay host age and conduit geometry were not retrieved from a source.

---

## Q6. Impact-melt-hosted Ni-Cu-PGE (Sudbury) — unique

### Takeaway
Sudbury is a one-of-a-kind deposit: a 1.849 Ga impact-melt sheet (≈130 km original crater) that segregated sulfide into a basal "sublayer", offset dikes and the footwall. Treat it as a rare-event special case.

### Cited Findings
- Sudbury formed by an impact "1.849 billion years ago". The impactor was ~10–15 km and the original crater ~130 km. The present basin is 62 km × 30 km. The impact melted a large amount of crust. "The sub layer is the main zone of mineralization". Ore types are contact deposits (SIC/footwall-breccia margin), footwall deposits in country rock, offset deposits, and Sudbury breccia-hosted deposits. Most deposits lie on the outer rim. Metals: Ni, Cu, Au, Ag, Pt, Pd, Rh, Ir, Ru. — [Wikipedia: Sudbury Basin](https://en.wikipedia.org/wiki/Sudbury_Basin) (secondary)
- Song et al. single out Sudbury as the exception: most other super-large deposits are in small intrusions. — [Song et al. 2011](https://katalog.hcu-hamburg.de/vufind/Search2Record/DOAJ037909029)

### Inferences
- **Proposed simulation rule:** only if the planet history includes large impacts (a crater ≥ ~100 km into continental crust containing S-bearing rocks). Place a ring-shaped melt sheet (TFC: gabbro/diorite norite proxy) with ore lenses at the basal contact (0–200 m above the floor), in radial and concentric "offset dikes" (10–100 m wide, km long), and in footwall breccia veins, all concentrated around the rim. Expected 0–1 per world. Ore is mapped to TFC garnierite, plus native copper or malachite for the Cu-rich footwall.

### Gaps
- Sudbury tonnage, grade and SIC thickness were not retrieved.

---

## Q7. Fe-Ti-V oxide deposits (layered intrusions, massif anorthosites, LIP-related intrusions)

### Takeaway
There are three families. (a) V-rich titanomagnetite layers near the top of large layered intrusions (Bushveld Upper Zone) and in LIP-related gabbro intrusions (Panzhihua/Emeishan). (b) Ilmenite (Ti) ores in Proterozoic massif-type anorthosite (AMCG) suites, almost all 2.12–0.93 Ga and mostly 1.8–1.06 Ga. (c) Ti-V magnetite in Alaskan-type complexes (Q8). Each body is up to a few hundred Mt and they are important globally (two mines supply ~32% of world TiO2).

### Cited Findings
- Bushveld Fe-Ti-V (Model 3): "Layers of Ti-V-rich magnetite in upper parts of large repetitively layered mafic-ultramafic intrusions". "Layers near top of intrusion". "Layers may be cut by pipes and veins rich in ilmenite." "Blocks of magnetite in soil and alluvium." — [USGS B1693 Model 3](https://pubs.usgs.gov/bul/b1693/html/bull5vsm.htm)
- Massif-anorthosite Fe-Ti deposits: "rarely contains more than 300 million tons of ore, with between 10- to 45-percent TiO2, 32- to 45-percent FeO, and less than 0.2-percent V". Lac Tio and Tellnes together produce ~32% of world TiO2 and the remaining 68% comes from shoreline placers. Massif anorthosites range up to 15,000–20,000 km² and are >90% plagioclase. The span of massif-type anorthosite formation is ~930 Ma (Rogaland) to ~2120 Ma (Labrador), with most emplaced ~1800–1060 Ma (Ashwal 2010). Grenville consensus favours an accretionary, Andean-type/back-arc model "rather than a plume model". Deposits sit along deep-seated fault zones at margins of major tectonic provinces. Tellnes: 380 Mt @ 18.4% TiO2, in a norite dyke-shaped intrusion >2.5 km long and 400 m thick. Lac Tio: 125 Mt @ 34.2% TiO2, 38.8% Fe. Orebodies include dikes, pods, veins, lenses and layers ranging from centimetres to hundreds of metres thick. Emplacement depth ranges from 20–30 km (granulite) to <10 km (Adirondacks evidence). Strong magnetic anomalies. — [USGS SIR 2010-5070-K, Woodruff et al. 2013](https://pubs.usgs.gov/sir/2013/5091/SIR13-5091.pdf); [USGS publication record](https://pubs.usgs.gov/publication/sir20105070K)
- USGS Model 7b (anorthosite Ti): "Most, and perhaps all, between 900 and 1,500 m.y. in age". Depositional environment: "Lower crust, intrusion under hot, dry conditions" in granulite terranes. Includes immiscible Ti-P liquid in ferrodiorite (nelsonite). — [USGS B1693 Model 7b](https://pubs.usgs.gov/bul/b1693/html/bull473d.htm)
- Charlier et al. (2015) classify ores as gabbro-noritic ilmenite ore, Ti-magnetite-dominated ore, nelsonite (Fe-Ti oxide + apatite) and rutile-ilmenite ore. fO2 and crystallization pressure control ilmenite vs magnetite. Fractional crystallization of TiO2-rich magma plus plagioclase buoyancy are the main mechanisms. — [Charlier et al. 2015, Earth-Science Reviews](https://www.sciencedirect.com/science/article/abs/pii/S0012825214002037)
- Giant Fe-Ti-V oxide deposits (Panzhihua, Hongge, Baima) are hosted in layered mafic–ultramafic intrusions of the Emeishan LIP. — [Flood basalt-related Fe–Ti oxide deposits, Emeishan LIP (Lithos)](https://www.sciencedirect.com/science/article/abs/pii/S0024493710001659)

### Inferences
- **Commodities:** Fe, Ti (ilmenite, hemo-ilmenite, rutile), V (in magnetite), with by-product P (apatite in nelsonite) and Sc.
- **Secular distribution:** anorthosite-hosted Ti is a **Proterozoic-only** type (a strong time gate, 2.1–0.93 Ga). Layered-intrusion and LIP Fe-Ti-V occur at any age. Panzhihua is ~260 Ma (background knowledge, unverified).
- **Proposed simulation rules:**
  - (a) Layered-intrusion and LIP V-magnetite: within any layered intrusion from Q2 (or any LIP gabbro sill >~1 km thick), add 1–20 magnetite layers in the upper 25% of the body. Each is 0.5–6 m thick (cited up to ~6 m) and laterally continuous for km.
  - (b) Massif anorthosite Ti: inputs are a long-lived accretionary or back-arc orogen (several thousand km scale, per USGS), the time window 2.1–0.93 Ga (weight ≈1 in 1.8–1.06 Ga, ≈0.3 elsewhere in the window, 0 outside), and mid- to lower-crust emplacement. Generate anorthosite plutons (100–20,000 km²). Within each, with P ≈ 0.1–0.3, add ilmenite dykes or lenses (0.2–2.5 km long, 10–400 m thick) along margins or faults.
  - Erosion: ore is exposed after deep exhumation (granulite levels), so expect these in old shield cores. Weathering produces **heavy-mineral placers** (ilmenite plus magnetite sands in fluvial and coastal sediments), which dominate real Ti supply.
- **Block mapping:** TFC magnetite (rich grade for massive layers) / vanilla iron ore. Ilmenite and rutile need a new block (Ti not in TFC). V has no mapping.

### Gaps
- Bushveld Main Magnetite Layer V2O5 grade and Panzhihua tonnage were not retrieved.

---

## Q8. Alaskan-Ural-type zoned ultramafic complexes (Pt-Cr, Ti-V magnetite) — added type

### Takeaway
These are small, concentrically zoned dunite–clinopyroxenite–gabbro intrusions in arc and accreted terranes, mostly Paleozoic–Mesozoic. They matter mainly as the source of **Pt placers** (Urals, Colombia), with chromite-Pt in dunite cores and Ti-V magnetite in pyroxenite rims.

### Cited Findings
- Model 9: "Crosscutting ultramafic to felsic intrusive rocks with approximately concentric zoning" containing chromite, platinum and Ti-V magnetite. Age "Precambrian to late Mesozoic, most Paleozoic and Mesozoic". Hosts intrude granodiorite, island arc or ophiolite terranes. "Evidence indicates shallow levels of emplacement." Setting: "Unstable tectonic areas". Assemblage 1 is chromite + Pt-Fe alloys + Os-Ir alloys in dunite. Assemblage 2 is Ti-V magnetite in wehrlite, pyroxenite and gabbro. "Mechanical weathering produces placers". Examples: Urals, Duke Island (Alaska). — [USGS B1693 Model 9](https://pubs.usgs.gov/bul/b1693/html/bull4cdh.htm)

### Inferences
- **Simulation rule:** in arc and accreted-terrane cells (subduction-zone magmatism, post-accretion), spawn rare small (1–10 km diameter) zoned intrusions: dunite core → pyroxenite → gabbro rim. The dunite core carries chromite schlieren plus native Pt (new block). The pyroxenite carries magnetite layers (TFC magnetite). Downstream rivers get Pt nuggets in placers (new item) and magnetite sand.

### Gaps
- No grade or tonnage data were retrieved for this type.

---

## Q9. Kiruna-type (volcanic-hosted) magnetite-apatite — added type, origin contested

### Takeaway
These are massive magnetite-apatite bodies in intermediate to alkalic volcanic and subvolcanic rocks of continental-margin arcs. They are economically important for Fe, but whether they are orthomagmatic (iron-oxide melt) or hydrothermal is a **live controversy**. Include them as a bridge type.

### Cited Findings
- Model 25i: "Massive concordant and discordant magnetite ore bodies in intermediate to alkalic volcanic rocks with actinolite or diopside alteration". Setting: "Continental margin, subduction-related volcanic terrane. Especially with high-K volcanic rocks, possibly related to waning stages of volcanism." Age "Mesozoic to Holocene in circum-Pacific area. In Sweden and Missouri, 1,300–1,500 m.y." Mineralogy magnetite + apatite. Strong magnetic anomalies. Examples: Kirunavaara, El Romeral (Chile), Middle-Lower Yangtze. — [USGS B1693 Model 25i](https://pubs.usgs.gov/bul/b1693/html/bull7mn9.htm)

### Inferences
- The USGS 1,300–1,500 Ma age for Kiruna appears outdated. Kiruna is generally dated ~1.88 Ga (background knowledge; verify).
- **Simulation rule:** in mature continental arcs (subduction under continental lithosphere, high-K late-stage volcanism), with low probability, place massive magnetite lenses (100s of m) in andesite or diorite. Map to TFC magnetite (rich) in andesite or diorite, or vanilla iron. The hydrothermal researcher may already cover IOCG and Kiruna; coordinate to avoid double-counting.

### Gaps
- Tonnage and grade figures were not read (figures only).

---

## Q10. Carbonatite and alkaline-complex deposits (REE, Nb, P, Fe, Cu, vermiculite, fluorite, Zr)

### Takeaway
Carbonatites are small (~3 km² average) plugs and ring complexes in intracontinental rifts and fracture zones on cratons. Of >500 known, ~9% host a mine, and they are the main sources of LREE and Nb. Mineralization concentrates in late carbonatite phases and, critically, in **lateritic regolith** over them (Araxá Nb, Mt Weld REE, Brazilian phosphate). They occur at all ages, and the number preserved decreases with age.

### Cited Findings
- "Most carbonatites are emplaced in continental extensional settings along large-scale, intra-plate fracture zones or rifts. Some are in orogenic belts". Some were emplaced during post-orogenic extensional collapse or before the transition from rift to compression. "In rare cases" they occur in oceanic settings. Forms: components of alkaline complexes or isolated pipes, sills, dikes, plugs, lava flows and pyroclastic blankets. Circular, ring or crescent-shaped aeromagnetic and radiometric anomalies. — [Simandl 2015, BCGS Paper 2015-3](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P2015-03-05_Simandl.pdf)
- Average carbonatite surface area is ~3 km². With the associated fenite (~5 km²) and silicate rocks (~2 km²) the target is ~10 km². The 27 complexes measured range from 0.01 to 78.5 km². Associated silicate rock groups are melilitolites, ijolites, alkali gabbros, feldspathoid syenites, syenites, kimberlites and lamprophyres. — [Simandl 2015](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P2015-03-05_Simandl.pdf)
- Temporal pattern: the number of carbonatites with isotopic ages **decreases with age**, and mineralized ones follow the same trend except 150–0 Ma. Young (near-surface) carbonatites are commonly barren unless supergene-enriched, because mineralization relates to intrusive phases. "Carbonatites range from Archean to Recent", so age alone is not a prospectivity indicator. — [Simandl 2015](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P2015-03-05_Simandl.pdf)
- About 6% of known carbonatites host active mines, 3% historic mines and 11% an established resource, so a new carbonatite has a ~9 in 100 chance of hosting a mine. REE ore occurs in unweathered carbonatite (Mountain Pass, St-Honoré) and/or regolith (Mt Weld). REE concentrate in late ferrocarbonatite or calciocarbonatite phases in central breccias, ring dikes and cone sheets, or distally (Bayan Obo). Nb is in carbonatite (St-Honoré/Niobec), alkaline rocks, fenite or regolith (Catalão, Araxá). Vermiculite and phlogopite are in mafic/ultramafic units (Palabora pyroxenite). Most producing apatite deposits (Tapira, Ipanema, Catalão I) were enriched by weathering. Other products: Cu, U, Th and baddeleyite (Palabora); baddeleyite (Kovdor); Fe (Kovdor, Bayan Obo, Palabora); fluorite (Mato Preto, Okorusu, Amba Dongar); lime and cement; sodalite. — [Simandl 2015](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P2015-03-05_Simandl.pdf)
- Fenitization (alkali metasomatism) halo around carbonatites. — [Simandl 2015](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P2015-03-05_Simandl.pdf); [USGS B1693 Model 10](https://pubs.usgs.gov/bul/b1693/html/bull5rsj.htm)
- USGS Model 10: zoned complexes with a "central plug of carbonatite or syenite breccia surrounded by ring dikes and cone sheets". "Almost all known carbonatite complexes are intrusive into Precambrian shields, however, the carbonatites themselves may be much younger." Spatially related to fault lineaments such as the East African rift. Weathering "May result in goethite-rich soil enriched in P, Nb, and RE". Signature: radiometric, magnetic and high gravity anomalies. — [USGS B1693 Model 10](https://pubs.usgs.gov/bul/b1693/html/bull5rsj.htm)
- Carbonatite complexes may carry economic U, Th, Ti, Fe, Cu, vermiculite, Zr or P, frequently in different zones from the Nb-rich parts. G-T deposit list includes Araxá, Catalão, Mountain Pass, Oka, St Honoré, Tapira and Lueshe. — [USGS B1693 carbonatite G-T](https://pubs.usgs.gov/bul/b1693/html/bull1qlv.htm)
- More than 500 carbonatites are known, but only four were being mined for REE: Bayan Obo, Maoniuping, Dalucao and Mountain Pass. — [USGS, REE ore geology of carbonatites](https://pubs.usgs.gov/publication/70138176)

### Inferences
- **Depth:** intrusive carbonatites form at shallow-crustal subvolcanic levels (a few km); background knowledge, not verified. Erosion level controls what is exposed: too shallow gives barren volcanic carbonatite, intermediate gives the ore-rich intrusive phase.
- **Proposed simulation rule:**
  - Inputs: continental rift initiation or failed-rift arms on thick lithosphere (cratons or craton margins), plume-related alkaline provinces, and post-orogenic extension. Background (not cited): carbonatites cluster in time with rifting and with LIP/plume episodes.
  - Placement: rare point features along rift-border faults and lineaments. Each complex is a circular body of radius 0.1–5 km (area 0.01–78 km², median ~3 km²) with concentric zoning: carbonatite core → ijolite/syenite/pyroxenite ring → fenite halo (≈ doubling the radius).
  - Ore: P(ore) ≈ 0.09 per complex (cited ~9%). If ore is present, choose type by weight: Nb-pyrochlore ~35%, REE ~25%, phosphate ~25%, Fe/Cu/vermiculite/fluorite ~15% (designer weights, not literature). If the climate history gives a long humid-tropical weathering period after exhumation, add a laterite cap 10–100 m thick with ×3–10 enrichment of P, Nb and REE.
  - Frequency: ~20–60 complexes per Earth-sized continent area (designer estimate scaled from >500 known globally). Older complexes are only exposed where deeply eroded.
- **Block mapping:** no TFC or vanilla REE, Nb or P ore → new blocks (pyrochlore, bastnäsite/monazite, apatite). Partial mappings: magnetite (Kovdor/Palabora) → TFC magnetite. Cu (Palabora) → TFC malachite or native copper. Fluorite → new block. TFC **cryolite** belongs to an alkaline/A-type granite context (Ivittuut; see Q12), not carbonatite. Carbonatite rock itself → TFC marble as a visual proxy.

### Gaps
- No numeric age histogram (Woolley & Kjarsgaard 2008 data) could be read; only the trend statement.
- Grades and tonnages for Araxá, Bayan Obo and Mountain Pass were not retrieved.

---

## Q11. Kimberlite and lamproite diamonds

### Takeaway
Economic diamonds need a thick (>~150 km), cold, old (typically Archean, 2.5–3.6 Ga) lithospheric keel. Diamond-bearing kimberlite must pierce that keel; the pipe age can be much younger than the diamonds. Kimberlites are small (most <~20 ha) carrot-shaped pipes in clusters and fields. About 98% are younger than 1.2 Ga, with emplacement epochs at ~1.15–1.05 Ga, ~600–480 Ma, ~400–320 Ma and ~170–50 Ma (Jurassic–Cretaceous–Paleogene). Productive pipes are rare. Plume vs plate-tectonic triggering is **debated**.

### Cited Findings
- Clifford (1966) originally defined favourable cratons as stable ≥1.5 Gyr. Later revisions moved this to the end of the Archean (2.5 Ga). Aryasova & Khazan (2013) note productive deposits outside Archean cratons, but say the rule holds in most cases. They propose tying diamonds to thick depleted lithosphere, attributing the rule's validity to secular mantle cooling. — [Aryasova & Khazan, "Clifford's Rule" and kimberlite geodynamics](https://journals.uran.ua/geofizicheskiy/article/view/116453)
- Diamond requires P > 4.0 GPa and T < 1350 °C, met in thick, old lithospheric roots with low geotherms. Economic kimberlite mines occur "only in ancient continental shield regions (Clifford, 1966) older than 2.5 Ga (Janse, 1984)". "The most favourable tectonic environment for economic kimberlites is a thick, old (2.5–3.6 Ga) craton with low heat flow." Kimberlite magmatism initiates at >150 km depth. Diamonds form at >~150 km. Diamond ages range ~3.57 Ga to 88 Ma. Kimberlites ascend at 10–30 km/h. Diamond contents range <0.001 to >10 ct/t. — [Kjarsgaard 2007, GAC Kimberlite Diamond Deposits synthesis](https://www.stardiamondcorp.com/_resources/pdfs/Kjarsgaard-Kimberlite-Diamonds-Synthesis.pdf)
- Plume heads, magmatic underplating and large collisions can destroy diamond-bearing roots (Helmstaedt 2003). In Minas Gerais, ~75 km of diamond-bearing root was removed in <10 Myr during kamafugite magmatism (Read et al. 2004). Premier (Cullinan) lies inside the Bushveld Complex, a counterexample. — [Kjarsgaard 2007](https://www.stardiamondcorp.com/_resources/pdfs/Kjarsgaard-Kimberlite-Diamonds-Synthesis.pdf)
- Economic grades are ~0.02 to ~10 ct/t. Rough value is US$15–800/ct (mines). Letseng is 16 ha (upper quartile of mines), but only its 4 ha K6 phase was economic. Lac de Gras pipes are small (50–150 m diameter, <5 ha) and steep-sided, with exceptional grades of 2.7–4.8 ct/t. Hypabyssal dykes and sills are 0.1–3 m (rarely 5–10 m) thick. Blows and plugs reach up to 100 m diameter. Dykes extend >1 km. Fort à la Corne tephra cones are up to ~900 m and coalesce to 2.5 km. Pipe infill consists of multiple phases (hypabyssal, massive volcaniclastic, pyroclastic, resedimented) with very different grades. — [Kjarsgaard 2007](https://www.stardiamondcorp.com/_resources/pdfs/Kjarsgaard-Kimberlite-Diamonds-Synthesis.pdf)
- Indicator minerals are Cr-pyrope garnet (G10 subcalcic harzburgitic), Cr-spinel, Mg-ilmenite and Cr-diopside, used in till, stream and soil sampling with glacial dispersion trains. — [Kjarsgaard 2007](https://www.stardiamondcorp.com/_resources/pdfs/Kjarsgaard-Kimberlite-Diamonds-Synthesis.pdf)
- USGS Model 12: "Most productive pipes are 80–100, 250, and 1,000–1,100 m.y. in age". Pipes intrude cratonal areas stable since the Early Proterozoic, are "not correlated with orogenic events", and lie on epeirogenic warps or domes and basement fracture zones. Mined kimberlites yield 0.1–0.6 ppm diamond. "Productive pipes are rare." Pipes "weather rapidly to form topographic depressions". Heavy minerals pyrope, phlogopite and Mg-ilmenite indicate nearby pipes, and "Lamproite pipes lack ilmenite". Associated diamond placers. — [USGS B1693 Model 12](https://pubs.usgs.gov/bul/b1693/html/bull9fhv.htm)
- Heaman et al. (screened database): a discrete Mesoproterozoic epoch at 1,155–1,075 Ma (n = 26). Most epochs last 30–80 Myr. Earlier summaries give peaks at 1,200–1,050, 600–480, 400–320 and 170–50 Ma. "98% younger than 1.2 Ga", and the ~286–257 Ma interval is a global low. — search excerpts via [Heaman et al. 2019, "Dating Kimberlites: Methods and Emplacement Patterns Through Time"](https://www.researchgate.net/publication/338244870_Dating_Kimberlites_Methods_and_Emplacement_Patterns_Through_Time) (attribution of each number to this paper vs. related Kjarsgaard compilations not fully verified)
- Canada: kimberlites span Cambrian, Ordovician, Permian, Jurassic and Cretaceous–Paleogene. Economic Lac de Gras (Ekati/Diavik) is Middle Paleogene. Victor and Jericho are Middle Jurassic. Snap Lake and Gahcho Kué are Early Cambrian. Fort à la Corne is ~103 Ma. Fields contain tens to ~95 kimberlites, and fields are active over ~10–30 Myr. — [Kjarsgaard 2007](https://www.stardiamondcorp.com/_resources/pdfs/Kjarsgaard-Kimberlite-Diamonds-Synthesis.pdf)
- A mid-Cretaceous (103–94 Ma) kimberlite corridor runs through Nunavut, Saskatchewan and the central USA. — search excerpts via [Kjarsgaard et al. 2017 (mantleplumes.org)](https://www.mantleplumes.org/WebDocuments/Kjarsgaard_et_al-2017.pdf) and [IKC abstract](https://ikcabstracts.com/index.php/ikc/article/download/3048/3048/3034)
- Tappe et al. (2018): kimberlites dominate after ~2 Ga once Tp < 1400 °C. "Compelling cases of cause-and-effect relationships between thermal anomalies in the deep mantle and kimberlite eruptions … are rare if not absent". They propose melt drainage through thick lithosphere driven by plate-motion stresses. — [Tappe et al. 2018 (CEED)](https://mn.uio.no/ceed/english/research/publications/articles/2017/geodynamics-of-kimberlites-on-a-cooling-earth:-clu.html)
- A related study argues kimberlite ages are skewed young and that eruptions are a consequence of modern-style plate tectonics, especially deep subduction of hydrated crust. — [MERC Laurentian record](https://merc.laurentian.ca/node/2205)
- A 2023 Nature paper analyzes Mesozoic kimberlite fields of Africa and South America that formed during Gondwana breakup. — [Nature s41586-023-06193-3](https://link.springer.com/10.1038/s41586-023-06193-3) (snippet only)

### Inferences
- **Controversy flag:** the trigger is debated. One view links kimberlites to plumes or LLSVP margins (Torsvik-type arguments; background knowledge). Tappe et al. favour plate-tectonic stress and volatile-fluxed melting. The 2023 Nature work (Gernon et al., background knowledge) links them to continental breakup, with eruptions migrating inland ~20–30 Myr after rifting. That lag figure is unverified here; check before use.
- **Lamproites** (Argyle, Australia; background knowledge, ~1.2 Ga in a Proterozoic mobile belt) are the main exception to strict Archean-craton siting. They lack ilmenite (cited).
- **Proposed simulation rule:**
  - Inputs per cell and time: `lithosphere_thickness ≥ 150 km` (diamond window) and `craton_stabilization_age` (≥2.5 Ga gives weight 1.0; 1.5–2.5 Ga gives 0.2; <1.5 Ga gives 0.02, lamproite-only). `root_destroyed` flag: set if the cell experienced a plume head, voluminous underplating or major collision after root formation. Clear diamond potential (or reduce it ×0.1) for kimberlites erupting after that event. Kimberlite trigger events are any of: continental breakup or rift onset nearby (lag 10–30 Myr, migrating inland), passage over a plume, or plate-speed change. Time weighting uses the global epochs above, with near-zero probability before ~2 Ga (cited Tappe cooling argument; a few Archean exceptions only).
  - Geometry: clusters of 5–100 pipes per field, field radius ~20–100 km, active 10–30 Myr. Each pipe is a downward-tapering cone (steep walls ~80–85°, background knowledge). Surface diameter 50–500 m (rare 1–2.5 km tephra-cone complexes). The diatreme extends to ~1–2 km depth (background knowledge) and passes into a root zone and feeder dykes 0.1–3 m wide.
  - Erosion level: the crater facies is preserved only if erosion since eruption is <~0.3 km. Erosion of ~0.5–2 km exposes the diatreme (optimal). Deeper erosion leaves only root and dykes (low tonnage). This is a direct use of the stratigraphic column's denudation since eruption.
  - Grade: per-pipe lognormal. ~80–90% of pipes barren or uneconomic, a few % rich (designer weights consistent with "productive pipes are rare").
  - Surface expression: depressions (pipes weather quickly, so lakes over pipes, as at Lac de Gras); indicator minerals in tills and streams downstream; diamond placers in rivers and coastal terraces downstream of eroded pipes (Namibia-type).
- **Block mapping:** TFC diamond pipe_vein (radius 5, height 60) is the right shape. The host should be a new "kimberlite" block, or gabbro as in TFC default. Vanilla `diamond_ore` / `deepslate_diamond_ore`. Garnet and Cr-diopside indicators → new "indicator" small-ore, or reuse the TFC indicator mechanism with a gem-gravel block.

### Gaps
- No primary count of known kimberlites vs. diamondiferous vs. economic was retrieved. The commonly quoted "~1% economic" is background knowledge, unverified.
- The full Heaman/Kjarsgaard 2019/2022 age histograms and the RiMG 88 diamond-mines review ([GSW](https://pubs.geoscienceworld.org/msa/rimg/article/88/1/1/614951/A-Review-of-the-Geology-of-Global-Diamond-Mines)) could not be read.
- Diatreme depth and wall angles were not verified.

---

## Q12. Granitic pegmatites: LCT (Li-Cs-Ta-Be-Sn), NYF (Nb-Y-F-REE) and gem pegmatites

### Takeaway
LCT pegmatites are the late, most fractionated products of peraluminous S-type granites in collisional orogenic hinterlands. They form at ~350–550 °C and ~3 kbar, in amphibolite to upper-greenschist metasediments, in swarms up to 10 km from the parent granite with regional zoning outward. Only 1–2% of a pegmatite population is highly fractionated. Their ages cluster at supercontinent-assembly collisions. NYF pegmatites go with anorogenic A-type granites. Gem pegmatites (tourmaline, beryl/aquamarine, topaz, kunzite) are pocket-bearing, fractionated LCT (or NYF for topaz and amazonite) bodies.

### Cited Findings
- Rare-element pegmatites form two families. LCT are enriched in Li, Cs, Ta (plus Sn, Rb, Be) and tied to orogenic magmatism. NYF are enriched in Nb, Y and F and "associated with anorogenic magmatism". — [USGS OFR 2013-1008 LCT model](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf); [Wikipedia: Pegmatite](https://en.wikipedia.org/wiki/Pegmatite) (NYF ↔ A-type granites)
- "All LCT pegmatites were emplaced into orogenic hinterlands". The majority tie to collision. Mechanisms include crustal thickening, slab breakoff, delamination and late extensional collapse. Most intrude metasedimentary rocks at "low-pressure amphibolite to upper greenschist facies". Parent granites are typically peraluminous S-type (some Archean ones are metaluminous I-type). Fluid inclusions indicate 350–550 °C and ~3 kb. Fluxes (H2O, F, P, B) lower solidus and viscosity. — [USGS OFR 2013-1008](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf)
- Geometry: tabular dikes, sills, lenses and irregular masses, always much smaller than plutons. Greenbushes is "only 3-km long and a few hundred meters across". Groups contain "tens to hundreds of individuals and cover areas up to a few tens of square kilometers". Pegmatites form up to 10 km from the parent granite, with distal ones most fractionated. The regional zonation sequence outward is barren → beryl → columbite → tantalite + spodumene → pollucite. "The most highly fractionated rare-element-enriched pegmatites only constitute 1 or 2 percent of regional pegmatite populations". Bodies are internally zoned into border, wall, intermediate and core (quartz) zones. Shallow pegmatites follow fractures and bedding, and deeper ones are concordant to foliation. — [USGS OFR 2013-1008](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf)
- Giant deposits: Tanco 2.1 Mt @ 0.215% Ta2O5, Greenbushes 70.4 Mt @ 2.6% Li2O, Bikita 12 Mt @ 1.4% Li2O. Gemstones include beryl varieties (emerald, heliodor, aquamarine), spodumene varieties (kunzite, hiddenite) and watermelon tourmaline. Cassiterite and beryl are ores. — [USGS OFR 2013-1008](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf)
- Exploration haloes: at Greenbushes, As-Be-Sb-Sn form a 12 × 20 km halo in lateritic soil and Nb-Ta-B a 1 × 5 km halo. Cassiterite, tantalite, elbaite and spessartine are dense and durable stream-sediment indicators. Li and Rb alteration haloes overlie buried Tanco. Holmquistite haloes reach up to 20 m in mafic wallrock. Fertile-granite tourmaline is black, and fractionated pegmatite tourmaline is pink-green elbaite. — [USGS OFR 2013-1008](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf)
- Age distribution: LCT maxima at ca. 2650, 1800, 525, 350 and 100 Ma correspond to collisional orogeny and (except 100 Ma) supercontinent assembly. Minima at 2450–2225, 1625–1000, 875–725 and 250–200 Ma overlap supercontinent tenures. — [Bradley et al., GSA 2012 abstract](https://gsa.confex.com/gsa/2012AM/webprogram/Paper208315.html); [USGS OFR 2013-1008](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf)
- Refined maxima: 2638, 1800, 962, 529, 485, 371, 309 and 274 Ma. Li and Ta resources are concentrated in Archean and Phanerozoic pegmatites (bimodal, especially Ta), similar to orogenic gold and interpreted as a preservation effect. — [McCauley & Bradley 2014, Can. Mineral. (USGS record)](https://pubs.usgs.gov/publication/70100916)
- Some of the largest LCT deposits formed in short intervals, 2.7–2.5 Ga and 1.9–1.8 Ga, matching crust-growth pulses. — [Magna et al. 2016 (Exeter record)](https://ore.exeter.ac.uk/repository/handle/10871/23550)
- Pegmatites host aquamarine, tourmaline, topaz, fluorite, apatite and corundum. They are the main source of Li and Cs (pollucite) and most non-gem Be. Crystal sizes: spodumene >12 m (Black Hills), a ~380 t beryl (Madagascar). Fluid separation needs ~4 wt% H2O at 0.5 GPa vs 1.5 wt% at 0.1 GPa. — [Wikipedia: Pegmatite](https://en.wikipedia.org/wiki/Pegmatite) (secondary)
- The LCT model's references include "Cretaceous gem bearing pegmatite dikes of San Diego County, California". — [USGS OFR 2013-1008](https://pubs.usgs.gov/of/2013/1008/OF13-1008.pdf)

### Inferences
- **NYF and gem specifics (background knowledge, unverified):** NYF pegmatites (Pikes Peak-type amazonite–topaz–smoky quartz; Ivittuut cryolite in a Gardar rift A-type granite, ~1.27 Ga) form in post-orogenic or anorogenic (rift, A-type) settings. Miarolitic gem pockets need shallow emplacement (roughly ≤ 2–3 kbar, upper crust) so that a fluid phase can exsolve and open cavities. This matches the USGS observation that shallow pegmatites are fracture-controlled.
- **Schist-type emerald** (background knowledge): emerald forms where Be-bearing pegmatites or fluids meet Cr/V-bearing mafic–ultramafic rocks (greenstone, ophiolite). Colombian emerald is a different, black-shale-hosted hydrothermal type ([USGS B1693 Model 31c](https://pubs.usgs.gov/bul/b1693/html/bull51tf.htm): emerald in plagioclase–dolomite veins in black shale, Cretaceous–Tertiary, major faults; evaporites possibly supplied saline fluids).
- **Proposed simulation rule (LCT):**
  - Inputs: continental collision event (thickened crust >~50 km), syn- to post-collisional S-type granite generation (requires metasedimentary, pelitic crust in the column), host at greenschist–amphibolite grade, timing ~0–30 Myr after collision peak.
  - Placement: around each S-type pluton, generate a swarm of 10–300 dikes within 0–10 km. Assign the fractionation index f by distance and structural height (f increases outward and upward). Mineral zones follow f thresholds: f > 0.5 beryl (TFC emerald/aquamarine proxy), f > 0.7 columbite-tantalite + cassiterite, f > 0.85 spodumene/lepidolite (+ gem pockets if emplacement depth < ~8–10 km), f > 0.95 pollucite. Only ~1–2% of dikes get f > 0.85.
  - Dike size: 0.5–20 m thick, 10–500 m long. Rare giants (1 per many fields) up to 3 km × 300 m.
  - Time weighting: boost during supercontinent-assembly collisions. Preservation is strongly erosion-dependent: shallow gem-pocket levels erode away in old orogens, so Precambrian fields expose deeper, pocket-poor levels.
  - NYF: same procedure around A-type granites in rifts or post-orogenic extension. Commodities are REE, Nb, Y, F, topaz and amazonite, plus cryolite (very rare).
- **Block mapping:** cassiterite → TFC cassiterite. Beryl/emerald → TFC emerald (vanilla emerald). Topaz → TFC topaz. Tourmaline, spodumene, lepidolite, tantalite, pollucite, muscovite → new blocks. Cryolite → TFC cryolite (fits NYF/A-type granite better than TFC's generic granite/diorite placement). Amethyst and smoky quartz in pockets → TFC amethyst (minor). Corundum in desilicated pegmatite (plumasite) → TFC sapphire/ruby (rare). Host → TFC granite.

### Gaps
- Primary P-T data for miarolitic gem pegmatites and NYF-specific age distributions were not fetched.
- The Ivittuut cryolite age and setting were not verified.

---

## Q13. Granite-related Sn-W greisen, and rhyolite-hosted Sn / topaz rhyolite (bridge to hydrothermal; brief)

### Takeaway
Sn (±W, Mo, Bi, Be) greisens sit in the cupolas of highly fractionated S-type leucogranites (>73% SiO2) emplaced at mesozonal levels in thickened foldbelts, typically after the main folding. They produce rich placers. Their volcanic equivalent is rhyolite-hosted ("Mexican-type") wood-tin veinlets in topaz rhyolite domes (Tertiary), a source of topaz (and red beryl). The hydrothermal researcher covers veins and skarns in detail.

### Cited Findings
- Sn greisen (Model 15c): disseminated cassiterite, veinlets, stockworks, lenses, pipes and breccia in greisenized granite. Hosts are specialized biotite and/or muscovite leucogranite (S-type) with accessory topaz, fluorite, tourmaline and beryl. "Mesozonal plutonic to deep volcanic environment". Setting: foldbelts of thick sediments on cratonic shields and accreted margins, with granitoids generally postdating major folding. Lodes sit "in or near cupolas and ridges developed on the roof or along margins of granitoids". Zoning runs from cassiterite + molybdenite through wolframite + beryl + arsenopyrite + bismuthinite to Cu-Pb-Zn sulfides. Specialized granites have SiO2 > 73%, K2O > 4%, and are enriched in Sn, F, Rb, Li, Be, W, Mo, B, Nb, Cs, U, Th, Ta. "Rich placer deposits form by weathering". Cassiterite, topaz and tourmaline appear in streams. Examples: Lost River, Erzgebirge, Anchor (Tasmania). — [USGS B1693 Model 15c](https://pubs.usgs.gov/bul/b1693/html/bull217y.htm); deposit list incl. Altenberg, Cinovec, East Kemptville — [G-T model](https://pubs.usgs.gov/bul/b1693/html/bull22pa.htm)
- Rhyolite-hosted Sn (Model 25h): cassiterite and wood tin in discontinuous veinlets in rhyolite flow-dome complexes. Hosts are alkali-feldspar rhyolite with >75% SiO2, including topaz rhyolites, with accessory topaz, fluorite, bixbyite, pseudobrookite and beryl. Tertiary, chiefly Oligocene–Miocene. Silicic volcanic fields on thick continental crust. Veins are 0.1–10 cm wide and their other dimensions "seldom exceed 75 m". Ore sits in fractured outer parts of domes. Cassiterite in stream sediments usually stays within 2–3 km of deposits. — [USGS B1693 Model 25h](https://pubs.usgs.gov/bul/b1693/html/bull2nn2.htm)

### Inferences
- **Simulation rule:** for each S-type granite (Q12 inputs), with P ≈ 0.1–0.2 (designer), add a greisen and stockwork zone in the pluton roof or cupola: the top 0–500 m of the granite, ~0.1–1 km wide. Contents: cassiterite (TFC cassiterite), bismuthinite (TFC bismuthinite), wolframite (new block) and topaz (TFC topaz). After exhumation and erosion, add cassiterite placers downstream. For topaz-rhyolite domes in continental silicic volcanic fields (crust >~40 km), place sparse cassiterite plus topaz veinlets in rhyolite (TFC rhyolite host).
- **Block mapping:** TFC cassiterite and bismuthinite (existing montane_cassiterite in granite already fits). TFC topaz. W needs a new block.

### Gaps
- Greisen grade and tonnage medians are shown only in figures, not retrieved.

---

## Q14. Volcanic-hosted gems and minerals: amethyst and agate geodes, opal, obsidian, fumarolic sulfur, basalt-hosted sapphire

### Takeaway
These are near-surface, young-biased deposits with poor preservation. Amethyst and agate geodes fill gas cavities in thick continental flood-basalt flows (Paraná-Etendeka) via later low-temperature (<~150 °C) meteoric fluids. Precious opal is mostly sedimentary/weathering-related (Australia), with a volcanic subtype in rhyolite and ignimbrite (Ethiopia, Mexico). Obsidian is a rhyolitic glass rarely older than Miocene. Native sulfur forms at fumaroles on active volcanoes. Sapphire in alkali basalts is a mantle or crustal **xenocryst** recovered mainly from placers.

### Cited Findings
- **Amethyst geodes:** low-temperature alteration of Ametista do Sul basalts (<130 °C, constrained by clinoptilolite and stable isotopes) produced the geodes, with smectite-rich altered rock tied to a "ballooning" cavity process. — [Rosenstengel & Hartmann, EGU 2011](https://meetingorganizer.copernicus.org/EGU2011/EGU2011-3236.pdf)
- Low crystallization temperatures indicate amethyst formed **after** emplacement of the complete basalt pile, from meteoric water of very low salinity infiltrating episodically into pre-existing cavities. — [Mineralium Deposita 2024](https://link.springer.com/article/10.1007/s00126-024-01310-2)
- Hartmann: the copper and amethyst deposits formed at most ~150 °C from water and vapour interacting with the basalts, not at lava temperatures. — [Revista Pesquisa FAPESP](https://revistapesquisa.fapesp.br/de-carona-no-vapor/)
- **Opal:** deposited at relatively low temperature and may occur in fissures of almost any rock. Water content 3–21 wt% (usually 6–10%). Australian sources are Lightning Ridge (Griman Creek Formation) and boulder opal in ironstone concretions in Queensland. Australia is often cited at 95–97% of precious opal supply, but this "may be overstated" given Ethiopian production. Ethiopian Shewa opal is volcanic nodules "within weathered layers of rhyolite", and Wollo (Wegeltena) opal resembles sedimentary opal. Mexican (Querétaro) opal is cut in rhyolitic host. Virgin Valley (Nevada) opal is in a bentonite horizon, much of it wood replacement. — [Wikipedia: Opal](https://en.wikipedia.org/wiki/Opal) (secondary)
- **Obsidian:** "usually 70% by weight or more" SiO2, "typically less than 1% water". It forms at margins of rhyolitic flows and domes and at dike margins. It is metastable, devitrifies and hydrates to perlite. "Obsidian older than the Miocene is rare", and the oldest known are a Cretaceous welded tuff and a partly devitrified Ordovician perlite. — [Wikipedia: Obsidian](https://en.wikipedia.org/wiki/Obsidian) (secondary)
- **Fumarolic sulfur (Ijen):** gases emerge at up to 600 °C. Miners pipe the gases through ceramic pipes where sulfur condenses and flows out molten (red), solidifying yellow. About 14 t/day is extracted, ~20% of the continuous daily deposit. The crater lake pH is <0.3. — [Wikipedia: Ijen](https://en.wikipedia.org/wiki/Ijen) (secondary)
- **Basalt-hosted sapphire:** sapphires in basalts are xenocrysts. Spinel coronas at contact with host basalt show disequilibrium. Primitive alkali basalts are not corundum-normative. Consensus is a mantle or crustal reservoir disrupted and transported by ascending basaltic melt, and "the origin of the basalt-associated sapphires remains a puzzle". — [Siebengebirge study, Contrib. Mineral. Petrol. 2017](https://link.springer.com/article/10.1007/s00410-017-1362-0)
- Eastern Australia: Weldborough (Tasmania) sapphires are magmatic (Ga ~200 ppm, mantle δ18O). Companion zircons indicate multiple sources (290–47 Ma) disrupted by 47 ± 0.6 Ma basalts. "Corundum xenocrysts from alkaline basalt fields differ in their characteristics and so have different lithospheric origins". — [Zaw, Sutherland et al. 2006, Mineralogical Magazine](https://cambridge.org/core/journals/mineralogical-magazine/article/contrasts-in-gem-corundum-characteristics-eastern-australian-basaltic-fields-trace-elements-fluidmelt-inclusions-and-oxygen-isotopes/B45E57685510D621D64DA14D334D1C64)
- Sapphire occurrence classes are (1) magmatic, mainly xenocrysts and xenoliths in alkali basalts, syenites, monzonites and lamprophyres; (2) metamorphic; (3) secondary alluvial. — [Uher et al. 2012, Geologica Carpathica](https://www.sav.sk/journals/uploads/01091228GeolCarp_Vol63_No1_71_82.pdf)
- Sapphire is recovered from alluvial deposits or primary workings. Blue colour needs only ~0.01% Ti + Fe, and ruby red needs ≥~1% Cr. At Mogok, rubies form in marble while sapphires form in pegmatites or corundum syenites. Yogo Gulch (Montana) is a primary source. — [Wikipedia: Sapphire](https://en.wikipedia.org/wiki/Sapphire) (secondary)

### Inferences
- **Amethyst and agate rule:** in continental flood-basalt (LIP) piles with thick (>~20–50 m) individual flows, flag the vesicular or "protogeode" upper zones of flows. After the pile is complete (≥ the end of the LIP pulse) and while it sits in a groundwater-active (humid or semi-arid, not frozen) climate, convert a fraction of cavities to geodes. Agate/chalcedony is the default fill and amethyst occurs in a subset of flows. Geodes are 0.1–3 m (background knowledge; Brazilian amethyst geodes reach several metres). Map to vanilla amethyst geode structure in basalt (best analogue) or TFC amethyst, which needs basalt added as host because TFC's default amethyst vein excludes basalt. Agate has no mapping (new block). Preservation is good while the basalt pile survives (Cretaceous Paraná, Deccan; background knowledge).
  - Unverified upper-temperature outlier: a 2025 GCA study (Goboboseb, Namibia) reportedly gives 281–327 °C. See Gaps.
- **Opal rule:** volcanic opal goes in weathered rhyolite, ignimbrite or tuff layers of Cenozoic silicic volcanic fields, formed by later low-temperature silica-rich groundwater. Sedimentary (Australian) opal goes in deeply weathered Cretaceous-type sediments under a long semi-arid weathering history; the weathering/sedimentary researcher covers this. Map to TFC opal (existing hosts include rhyolite, basalt, andesite, dacite and tuff).
- **Obsidian rule:** only on rhyolitic flows and domes younger than ~20–30 Myr (strong age cut-off; older ones become perlite or devitrified), at flow margins. Map to vanilla obsidian.
- **Sulfur rule:** only at currently or recently (<~1 Myr) active volcanic vents (arc stratovolcanoes, hotspots): summit craters and fumarole fields. Thin crusts, plus bedded lake or solfatara sulfur in crater-fill tuff. TFC `tuff_sulfur` already places sulfur in tuff at y 40–200. Restrict it to cells with active volcanism.
- **Sapphire rule:** intraplate alkali-basalt fields (rifts, hotspots on continental lithosphere, mostly Cenozoic). Give sparse xenocrysts in basalt (very low density) and concentrate ×100–1000 in alluvial gravels draining the field. Map to TFC sapphire. Add rare ruby in basalt-derived placers (e.g. Thai/Cambodian fields; background knowledge, unverified).

### Gaps
- Agate formation conditions, opal formation temperatures and ages, and fumarole-sulfur deposit sizes (Andean summit deposits such as Aucanquilcha) were not retrieved; the search budget ran out.
- The 2025 GCA high-temperature amethyst result and the often-cited ~50 °C amethyst estimate (Juchem; Gilg et al. 2003 fluid inclusions) appeared only in search summaries without verifiable links.

---

## Q15. Cross-cutting: secular distribution, preservation and simulation inputs (summary)

### Takeaway
Magmatic deposit types are strongly time-gated by mantle cooling: komatiite Ni is mostly Archean, kimberlite diamonds come mostly after 1.2 Ga, and anorthosite Ti is Proterozoic-only. Each type is also tied to a specific tectonic trigger (plume/LIP, rift, collision, ophiolite obduction, arc), and erosion depth decides what is exposed. Deep types (anorthosite, LCT roots, layered intrusions) need deep exhumation. Shallow types (obsidian, sulfur, crater-facies kimberlite, geodes, gem pockets, carbonatite laterite) need little erosion and are young-biased.

### Cited Findings
- Komatiite-to-kimberlite transition after ~2 Ga with Tp < 1400 °C. — [Tappe et al. 2018](https://mn.uio.no/ceed/english/research/publications/articles/2017/geodynamics-of-kimberlites-on-a-cooling-earth:-clu.html)
- Massif-anorthosite Ti window ~2120–930 Ma, mostly 1800–1060 Ma. — [USGS SIR 2010-5070-K](https://pubs.usgs.gov/sir/2013/5091/SIR13-5091.pdf)
- LCT pegmatite peaks at supercontinent-assembly collisions, with minima during supercontinent tenure. — [GSA 2012](https://gsa.confex.com/gsa/2012AM/webprogram/Paper208315.html); [McCauley & Bradley 2014](https://pubs.usgs.gov/publication/70100916)
- Carbonatite numbers decrease with age (preservation and erosion). — [Simandl 2015](https://cmscontent.nrs.gov.bc.ca/geoscience/publicationcatalogue/Paper/BCGS_P2015-03-05_Simandl.pdf)
- Productive diamond pipes are concentrated at 80–100, 250 and 1000–1100 Ma. — [USGS Model 12](https://pubs.usgs.gov/bul/b1693/html/bull9fhv.htm)
- Podiform chromite is Phanerozoic. Layered-intrusion Cr/PGE/Fe-Ti-V is "generally Precambrian". — [USGS Model 8a](https://pubs.usgs.gov/bul/b1693/html/bull3lr9.htm); [USGS Model 2a](https://pubs.usgs.gov/bul/b1693/html/bull8c6q.htm)
- Obsidian rarely older than Miocene. — [Wikipedia: Obsidian](https://en.wikipedia.org/wiki/Obsidian)

### Inferences
Summary table for the simulation designer. Columns: trigger event, gating fields, time weighting, depth of formation, erosion window, abundance per Earth-like world, TFC/vanilla block. Values marked * are designer estimates or background knowledge, not cited facts.

| Type | Trigger / setting | Key gating fields | Time weighting | Formation depth | Exposure window (erosion) | Abundance* | Block |
|---|---|---|---|---|---|---|---|
| Podiform chromite | SSZ/MOR ocean lithosphere → obduction at collision | ophiolite present, mantle section exhumed | Phanerozoic–Neoproterozoic* | upper mantle, near Moho* | after crust section eroded* | many tiny pods along sutures | new chromite |
| Stratiform Cr / PGE reef | plume/LIP into stable craton | craton age, lithosphere rigid, huge intrusion volume | peak 2.7–2.0 Ga* | upper-mid crust* | 0–8 km erosion* | 0–3 giant complexes | new chromite, new PGE, TFC garnierite (basal) |
| Komatiite Ni | Archean plume volcanism in greenstone belts | Tp high, sulfidic sediments in footwall | >2.5 Ga strong, 1.9 Ga minor, ~0 after | surface flows, shallow sills | any (old belts are deformed) | clusters in Archean cratons | TFC garnierite |
| Conduit/LIP Ni-Cu-PGE | LIP or rift feeder systems | S-rich strata (evaporite, coal, black shale) in crust | any age; evaporite and coal later in Earth history* | 0–5 km below LIP* | moderate | rare (P~0.1 per LIP*) | TFC garnierite + pyrite + malachite cap |
| Sudbury-type | large impact | crater ≥100 km* | any | crater floor | moderate | 0–1 | as above |
| Layered-intrusion Fe-Ti-V | same as stratiform, top zone; LIP sills | intrusion thickness | any | upper crust | moderate | with each layered intrusion | TFC magnetite |
| Anorthosite Ti | long-lived accretionary orogen | Proterozoic window | 2.12–0.93 Ga only | 10–30 km | deep (shield cores) | several provinces | new ilmenite + TFC magnetite |
| Alaskan Pt | arcs and accreted terranes | — | mostly Paleozoic–Mesozoic | shallow | moderate | few, small | new Pt placer, TFC magnetite |
| Kiruna Fe-P | continental arc, late high-K | — | Proterozoic & Mesozoic–Cenozoic | subvolcanic | shallow–moderate | rare | TFC magnetite |
| Carbonatite | continental rift, fracture zones | thick lithosphere, rift | all ages, young-biased | subvolcanic* | intermediate; laterite needs humid climate | tens per continent, ~9% mineralized | new REE/Nb/apatite; TFC magnetite |
| Kimberlite diamond | breakup, plate stress, plume (debated) | lithosphere ≥150 km, craton ≥2.5 Ga, root not destroyed | ~0 before 2 Ga; epochs 1.1, 0.55, 0.36, 0.17–0.05 Ga | >150 km source | 0.3–2 km erosion best* | fields of 5–100 pipes, few productive | TFC diamond pipe |
| LCT pegmatite | continental collision, S-type granite | pelitic crust, amphibolite–greenschist | supercontinent-assembly peaks | ~3 kbar (~10 km)* | moderate (gem pockets shallow) | swarms per orogen; 1–2% fertile | TFC cassiterite, emerald, topaz; new Li/Ta |
| NYF pegmatite | anorogenic A-type granite (rift) | — | any | shallow–mid crust* | moderate | rare | TFC topaz, cryolite; new REE |
| Sn-W greisen | fractionated S-type granite cupola | granite roof preserved | any, post-folding | mesozonal | shallow erosion into roof | common in Sn belts | TFC cassiterite, bismuthinite, topaz |
| Geodes (amethyst/agate) | continental flood basalts | thick flows, groundwater | any LIP; young preserved | <~1 km* | flows preserved | abundant in LIPs | vanilla amethyst geode / TFC amethyst |
| Opal (volcanic) | silicic volcanic fields, weathering | rhyolite/tuff, groundwater | Cenozoic-biased* | near surface | very shallow | scattered | TFC opal |
| Obsidian | rhyolite flows and domes | felsic volcanism | <~20 Ma mostly | surface | none | common in rhyolite fields | vanilla obsidian |
| Sulfur | active volcanic vents | active volcanism | Recent | surface | none | per active volcano | TFC sulfur (tuff) |
| Basalt sapphire | intraplate alkali basalt fields | continental lithosphere, hotspot or rift | mostly Cenozoic* | xenocryst from depth | basalt plus placers | few fields | TFC sapphire |

- **Life and atmosphere coupling (inference):** magmatic deposits are mostly independent of biology. The exceptions are the S sources for Ni-Cu saturation (sulfidic shale, evaporite after oxygenation, coal after land plants) and lateritic or supergene upgrades that need weathering climates (carbonatite Nb/REE/P, ultramafic garnierite).
- **Generic preservation function (inference):** `exposed(t) = (erosion_since_formation ≥ d_min) AND (erosion_since_formation ≤ d_max)`. Use type-specific `[d_min, d_max]` from the table. For example, kimberlite diatreme ~[0.3, 2.5] km, LCT gem pockets ~[3, 8] km below the paleo-surface, anorthosite Ti ~[10, 35] km. These ranges are designer estimates.

### Gaps
- A global, quantitative secular-distribution dataset for all magmatic types was not retrieved (Groves et al. 2005, Goldfarb et al. 2010, Kerrich et al. 2005, and Begg et al. 2010 were not accessed). The time-weights above are partly inferred.
- Relative global production shares (USGS Mineral Commodity Summaries 2025/2026 for PGM, Cr, Nb, REE, Ti, V, diamonds, Li, Ta) were not fetched because the search budget was exhausted.
