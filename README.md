# PlateTectonics

A Minecraft 1.21.1 (NeoForge) world-generation mod that builds worlds from a simulated geologic history.

- **Design (start here):** [docs/design/README.md](docs/design/README.md)
- **Research:** [docs/reports/](docs/reports/) and [docs/research_notes/](docs/research_notes/)

## Layout

| Project | What it is |
|---|---|
| `geosim-core` | The planet simulation. Pure Java 21, no Minecraft classes; unit-tested |
| `geosim-cli` | Command-line runner that writes PNG maps |
| `mod` | The NeoForge mod; compiles `geosim-core` sources into the mod jar |

## Common commands

```bash
./gradlew build                                   # compile everything, run tests, build the mod jar
./gradlew :geosim-cli:run --args="--seed 42"      # write maps/proto_42.png
./gradlew :mod:runClient                          # start Minecraft with the mod
```
