# 05 · Lithology: rock columns, metamorphism, rendering to TFC rocks

> **Level 1** · Read when: you add a rock type, change how layers are recorded or rendered, or map rocks to blocks.
> Decisions: A-14, D-15 · Go deeper: report § "TFC exposes 33 ores, 21 rocks" and § "Lossy".

**Summary.** Every master cell has a **rock column**: an ordered stack of layers recorded by the processes that made them (eruption, intrusion, deposition, metamorphism). Erosion removes layers from the top; burial heats the ones below. Each layer carries a **unit id**, so neighbouring columns can be correlated into continuous strata. At chunk time the column is rendered as folded, tilted layers of **TFC's 21 rocks** (or vanilla stones). The internal set of rock types is richer than TFC's and is collapsed only at render time.

## Layer record

| Field | Type | Meaning |
|---|---|---|
| lithology | u8 | Internal rock type (table below) |
| thickness | f16 | Metres (present, after compaction) |
| unitId | u32 | The event that made the layer (eruption, depositional episode…); used to correlate neighbouring columns |
| age | f16 | Myr since formation of the planet |
| environment | u8 | Depositional or emplacement setting (fluvial, deltaic, shelf, reef, deep marine, lacustrine, evaporitic, swamp, arc, ridge, plume…) |
| organic | u8 | TOC class (coal, oil shale, source rock) |
| Tmax | u8 | Peak temperature reached (coal rank, oil maturity, metamorphism) |
| TTI | f16 | Lopatin time–temperature index (petroleum; [06](06-resources.md)) |
| grade | u8 | Metamorphic facies reached |
| deformation | u8 | Fold intensity and dip class |
| flags | u16 | Red bed, evaporite, black shale, carbonate platform, unconformity on top, ore horizon… |

Columns are built **bottom-up during the simulation** and trimmed **top-down by erosion**. An unconformity flag marks a time gap (erosion surface): unconformity-type ore deposits and oil traps use it.

## Storage

- Columns are stored on the **master grid** (≈15.6 km), capped at 32 layers; province tiles (≈1 km) refine them by correlating unitIds and adding local facies changes. When the cap is reached, adjacent thin layers of the same family merge (special horizons such as coal, evaporite, ore horizons and source rocks never merge away).
- Estimate: 3.3 M columns × ~8 layers on average × ~16 bytes ≈ 420 MB in memory if every cell were full; with merging and shared ocean-crust templates the target is ≤ 400 MB in memory and 60–100 MB compressed on disk. If that is exceeded, store columns at 31 km and interpolate. This fits the [budgets](02-simulation-pipeline.md#budgets-reference-machine-8-core-desktop).
- Tier 1.5a interpolates layer boundary depths between neighbouring columns that share a unitId. Where unitIds differ, it places a sharp contact (fault, intrusion margin or facies change).

## Internal lithologies → TFC rocks

| Internal lithology | Typically formed by | TFC rock | Vanilla fallback |
|---|---|---|---|
| Peridotite / serpentinite (ultramafic) | Ophiolite mantle sections, obducted slivers | gabbro (lossy) | deepslate |
| Basalt | Ridges, hotspots, LIPs, rifts | basalt | basalt / smooth basalt |
| Gabbro | Lower ocean crust, layered intrusions | gabbro | deepslate |
| Anorthosite | Proterozoic massifs | gabbro (lossy) | diorite |
| Andesite | Arc volcanoes | andesite | andesite |
| Dacite | Evolved arcs | dacite | andesite |
| Rhyolite | Calderas, continental arcs, rifts | rhyolite | granite |
| Tuff | Explosive eruptions | tuff | tuff |
| Granite | Batholiths, collisional (S-type) and anorogenic (A-type) melts | granite | granite |
| Diorite | Arc intrusions | diorite | diorite |
| Kimberlite | Pipes in old cratons | gabbro (lossy) | blackstone |
| Carbonatite | Rift and plume complexes | marble (lossy) | calcite |
| Sandstone | Rivers, beaches, deserts, deltas | **our sandstone rock** (A-14) | sandstone / red sandstone |
| Conglomerate | Alluvial fans, mountain fronts | conglomerate | sandstone |
| Shale / mudstone | Offshore, deep basins, lakes | shale | stone |
| Claystone | Floodplains, lakes | claystone | terracotta |
| Limestone | Warm shallow seas, reefs | limestone | calcite |
| Dolomite | Evaporitic platforms, diagenesis of old carbonate | dolomite | calcite |
| Chalk | Pelagic, after calcareous plankton | chalk | white terracotta |
| Chert / BIF host | Deep ocean; iron formations | chert | stone |
| Evaporite (halite, gypsum, potash) | Arid restricted basins | host rock (claystone or shale) + mineral blocks | — |
| Coal | Swamps | host rock (shale or claystone) + coal seams | — |
| Quartzite | Metamorphosed sandstone | quartzite | stone |
| Slate → phyllite → schist → gneiss | Mudrocks at rising metamorphic grade | slate / phyllite / schist / gneiss | deepslate variants |
| Marble | Metamorphosed carbonate | marble | calcite |
| Greenschist / amphibolite / blueschist | Metamorphosed basalt | schist (lossy) | deepslate |
| Granulite / migmatite / orthogneiss | Highest grade | gneiss | deepslate |
| Eclogite | Subducted, exhumed crust | gabbro (lossy) | deepslate |

Evaporite, coal and ore horizons are layers whose **mineral blocks** are placed by the deposit renderer ([06](06-resources.md)), embedded in the mapped host rock.

## Metamorphism

- Peak temperature comes from burial depth × geotherm (heat flow) and from intrusion contacts. Peak pressure comes from burial and collision thickening. Grade is the maximum ever reached (retrograde effects are ignored).
- Facies thresholds (approximate): diagenesis < 150 °C · very low grade 150–300 °C · greenschist 300–500 °C · amphibolite 500–700 °C · granulite > 700 °C. Blueschist and eclogite form at high pressure and low temperature (subduction channels), and are exposed only where exhumed.
- **Protolith + grade → rock:** shale → slate → phyllite → schist → gneiss; sandstone → quartzite; limestone or dolomite → marble; basalt → greenschist → amphibolite; granite → orthogneiss.
- **Contact metamorphism** near intrusions: marble skarns and hornfels rims. Used by skarn and lapis deposits ([06](06-resources.md)).

## Deformation and rendering (Tier 1.5/2)

- Collision and thrusting record a fold orientation (from the convergence direction) and an intensity per cell. Rendering displaces layer boundaries with a fold field (sinusoid along the shortening direction plus noise) and tilts strata near uplifts and basin margins.
- Erosion through folded layers produces the outcrop patterns geologists read: old rock in anticline cores and range cores, young rock in basins and synclines.
- **Minimum rendered thickness:** about 3 blocks for ordinary layers. Thinner layers merge into the dominant lithology of their sequence. Special horizons (coal, evaporites, reefs, ore horizons) use their own minimum (≥ 1 block) via the render-scale templates (D-2).
- **TFC requirement:** every solid stone block below the surface must be one of the TFC raw-rock blocks listed in the generator's `rock_layer_settings`, so that TFC veins, collapses, loose rocks and prospecting work ([08](08-minecraft-integration.md)).
