# GregTech 6 Community Edition

**English** · [简体中文](README.zh-CN.md) · [Issue Tracker](https://github.com/GregTech6CE/GregTech6/issues)

**Bringing GregTech 6 to modern Minecraft.**

Minecraft 1.20.1 · Forge | Minecraft 1.21.1 · NeoForge

## ⚠️ Version & Compatibility Notice

**GT6CE has released its `v1.0` baseline and is currently in the `v1.0 ~ v1.1` rapid iteration phase. Updates during this window may introduce breaking architectural and content changes alongside feature additions and bug fixes.**

Item and block IDs, save data formats, material and recipe registries, machine behavior, world generation, the Addon API, and scripting contracts are actively being finalized. **Save and addon compatibility across builds is not guaranteed prior to the conclusion of `v1.1`** (after which a 1–2 version migration window will be maintained for structural changes).

- Use a dedicated instance when testing early builds rather than long-term worlds or production servers.
- **Back up your worlds, configs, and scripts before updating, and keep matching previous mod JARs.** Worlds saved on a newer build cannot be recovered by downgrading the mod.
- Modpack and addon authors should pin a specific build and verify compatibility before upgrading.

## Welcome to GT6CE

**GregTech 6 Community Edition** (**GT6CE**) is a community port and evolution of GregTech 6 for modern Minecraft, bringing hands-on thermodynamics, material processing, and multi-energy industrial engineering to **1.20.1 Forge** and **1.21.1 NeoForge**.

In GregTech 6, production lives in the physical world rather than behind abstract progress bars—regulating crucible temperatures, carving molds, forging parts on anvils, and routing specialized energy forms to each machine:

- Start in the **Stone, Pottery, and Bronze Ages**, prospecting surface rocks, firing clay crucibles, and casting molten alloys into hand-carved `5×5` molds.
- Harness **steam, rotational, and kinetic power** through boilers, turbines, engines, axles, and gearboxes to drive mechanical workshops.
- Expand into **electrification, chemical engineering, optics, and nuclear power**, linking machines, multiblocks, pipes, hoppers, and covers into self-consistent automated factories.

## Features

- **Thermodynamic Crucible Metallurgy**: Melt, alloy, and cast metals driven by real Kelvin melting points, boiling points, and heat capacities—evolving from manual clay crucibles to dual-track small-batch and multiblock industrial automation.
- **Multi-Energy Synergy**: Generate, convert, and coordinate heat (`HU`), cooling (`CU`), steam (`SU`), rotational (`RU`), kinetic (`KU`), electrical (`EU`), magnetic (`MU`), laser (`LU`), and quantum (`QU`) energy rather than stacking a single voltage tier.
- **In-World Crafting & Tooling**: Carve `5×5` bitmask molds, hammer heated ingots on two-slot anvils, read physical sensor dials, and maintain modular tools with replaceable heads.
- **Covers, Hoppers, and Piping**: Route items, fluids, and power while turning any machine face into a programmable I/O controller with conveyors, pumps, regulators, and redstone sensors.
- **Multiblock Industrial Facilities**: Build coke ovens, distillation towers, large crucibles, heat exchangers, nuclear reactors, and logistics cores.
- **Geological Prospecting**: Trace surface rocks, indicator flowers, and bedrock springs to uncover layered rock formations and ore veins.
- **Viewer & Telemetry Integration**: Inspect recipes, ore processing trees, and multiblock layouts via JEI / EMI, with real-time machine and thermal telemetry built into Jade and GUIs.
- **Extensibility**: Customize recipes, materials, energy tiers, worldgen, and reskinned machines through KubeJS, datapacks, and the dual-platform Addon SDK.

## Supported Versions

| Minecraft | Target Loader | Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.20+ | 17 |
| 1.21.1 | NeoForge 21.1.243+ | 21 |

Place the matching GT6CE JAR into your instance's `mods` folder.

## Development Status

GT6CE has reached its **`v1.0` playable baseline** on both target platforms, with single-source material and recipe registries, initial resource slimming, and core survival workflows in place. Development is now advancing through the [`v1.0 ~ v1.4` Roadmap](docs/roadmap/README.md)—stabilizing the narrow-contract Addon API, internalizing telemetry, overhauling world generation and transport networks, and deepening crucible metallurgy and native logistics. Track recent changes in the [v1.0 Changelog](docs/changelog/v1.0.md).

## Get Involved

**Report bugs and submit proposals on our [Issue Tracker](https://github.com/GregTech6CE/GregTech6/issues).** Please include your Minecraft and loader versions, GT6CE build filename or commit hash, reproduction steps, and relevant logs or screenshots.

Pull requests, RFC discussions, translations, documentation, textures, and playtest feedback are all welcome. For code contributions, describe the motivation, scope, and verified platforms.

## Developers

Both target platforms are maintained in a single repository using **[Stonecutter](https://stonecutter.kikugie.dev/)**: `api` defines public extension contracts, `core` holds platform-agnostic rules and shared assets, and `minecraft` houses shared Minecraft runtime code alongside version-specific adapters.

Run the Gradle task for your target build from the repository root (`./gradlew` on Linux/macOS, `.\gradlew.bat` on Windows):

```bash
# Forge 1.20.1 distribution JAR
./gradlew :minecraft:1.20.1-forge:distributionJar

# NeoForge 1.21.1 distribution JAR
./gradlew :minecraft:1.21.1-neoforge:jar

# Build both platforms
./gradlew assembleBothPlatforms
```

Built mod JARs are placed under `minecraft/versions/<version-loader>/build/libs/`.

| Resource | Purpose |
| --- | --- |
| [Developer Documentation](docs/README.md) · [Agent Guide](AGENTS.md) | Documentation index, engineering workflow, and repository layout (Chinese) |
| [Getting Started](docs/development/getting-started.md) · [Validation](docs/development/validation/README.md) | Toolchain setup, build tasks, dev runs, and verification gates (Chinese) |
| [Architecture & Packages](docs/architecture/README.md) | Source layers, platform adapters, and package responsibilities (Chinese) |
| [Addon API & KubeJS](docs/addon/README.md) | Extension contracts, SDK artifacts, and KubeJS integration (Chinese) |
| [Roadmap](docs/roadmap/README.md) · [Policies](docs/decisions/policies.md) · [Progression](docs/decisions/progression.md) | Design principles, seven-era tech tree, and version milestones (Chinese) |
| [Changelog](docs/changelog/v1.0.md) | Versioned user-facing release notes |

## License

Project code is licensed under **LGPL-3.0-or-later**. See [LICENSE](LICENSE) and the [GPL-3.0 text](GPL-3.0.txt).

Assets and third-party components retain their respective licenses. Upstream GT6 default assets are licensed under CC0-1.0 unless otherwise noted, and GregTech logos and derivatives are licensed under CC BY-NC 4.0. See [NOTICE](core/src/main/resources/META-INF/gregtech6/NOTICE) for full details.
