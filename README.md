# PlateTectonics

A Minecraft 1.21.1 (NeoForge) world-generation mod that builds worlds from a simulated geologic history. Not yet usable ingame.


## Layout

| Project | What it is |
|---|---|
| `geosim-core` | The planet simulation. Pure Java 21, no Minecraft classes; unit-tested |
| `geosim-cli` | Command-line runner that writes PNG maps |
| `mod` | The NeoForge mod; compiles `geosim-core` sources into the mod jar |

## Common commands

```bash
./gradlew build                                                # compile everything, run tests, build the mod jar
./gradlew :geosim-cli:run --args="--seed 42 --cell-km 31.25"   # simulate 4.5 Gyr; maps in maps/seed_42/
./gradlew :mod:runClient                                       # start Minecraft with the mod
```

CLI flags (`--help` lists all):

- `--age MYR`, `--cell-km K` (or `--width W`), `--frame-every MYR` (0 = no timeline GIF), `--out DIR`.
- `--gif-width PX`, `--gif-delay MS` (time per frame, default 80 ms), `--gif-panels`: each frame shows every map in a grid (`--gif-width` is then the width of one map), a header of global numbers and a chart of the global history so far.
- `--sea-drivers`: writes `sea_drivers.csv`, which per step gives the part of sea level due to ice, ocean-floor age and seafloor sediment, the land share at fixed levels, land, erosion and sediment by height band, and sea level under the rules before D-43 ([land share stability plan](docs/reports/Land%20share%20stability%20plan.md)). Measurement only; the planet is unchanged.
- `--history random|earth`: a random life and climate history (default) or the Earth-like preset.
- `--plates-only`: the Phase 2 plate engine alone, with no climate or erosion.
- `--set CONSTANT=VALUE`: override one `TectonicsConfig` or `SurfaceConfig` constant for this run (repeatable).

Output in `maps/seed_<seed>/`: elevation, plates, crust age and crust type maps; in full runs also temperature, precipitation, biome, rivers, ice, erosion, sediment and regolith maps; `timeline.gif`, `events.csv` and `history.csv` (the global state after every step).

## License

[MIT](LICENSE)
