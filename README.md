# GregTech 6 Community Edition

**English** · [简体中文](README.zh-CN.md) · [Issue Tracker](https://github.com/GregTech6CE/GregTech6/issues)

**Bringing GregTech 6 to modern Minecraft.**

Minecraft 1.20.1 · Forge | Minecraft 1.21.1 · NeoForge

## ⚠️ Development Build Notice

**GT6CE is under rapid development and has not yet reached a stable release. Updates may introduce major architectural changes alongside new content and bug fixes.**

Item and block IDs, save data formats, material and recipe systems, machine behavior, world generation, the addon API, and scripting interfaces are all subject to change. **Cross-build compatibility is not guaranteed for existing worlds, addons, or modpacks, and automatic migration may not cover all older data.**

After updating, existing production lines, addons, and scripts may need adjustments, and older worlds may encounter missing registries or incompatible machine data. In some cases, a new world may be required.

- Test development builds in a separate instance rather than on long-term worlds or production servers.
- **Back up your worlds, configs, and scripts before updating, and keep matching previous mod files.** Do not rely on downgrading the mod to recover a world already saved on a newer build.
- When building a modpack or addon, pin a specific build and verify compatibility before upgrading.

## Welcome to GT6CE

**GregTech 6 Community Edition** (**GT6CE**) is a community port of GregTech 6 to modern Minecraft. It brings GT6's material processing, hands-on metallurgy, and industrial progression to **1.20.1 Forge** and **1.21.1 NeoForge**.

GregTech 6 puts production details directly in your hands—regulating crucible temperatures, casting parts, refining new materials, and supplying power to each machine. Your factory grows step by step:

- Start with **tools and manual processing**, melting metals in crucibles and casting them into molds.
- Harness **steam and mechanical power** through boilers, turbines, and engines.
- Connect **processing machines, generators, and multiblocks** with pipes, cables, and storage into automated lines.

Original GT6 serves as the primary reference for materials, recipes, and machine behavior, adapted for modern Minecraft and its modding ecosystem.

## Features

- **Crucibles, alloys, and casting**: Work with molten metals, alloy ratios, molds, and casting channels where temperature and material properties drive the process.
- **Material-driven tools and parts**: Craft metals, alloys, and minerals into tools and components with distinct stats and roles.
- **Diverse power systems**: Generate, convert, and consume heat, steam, rotational power, kinetic power, and electricity.
- **Pipes, cables, and covers**: Transport items and fluids, route power networks, and customize machine faces with covers.
- **Multiblock machinery**: Construct coke ovens, distillation towers, large tanks, and heavy industrial structures.
- **World prospecting**: Locate ore veins, stone layers, and surface deposits to feed your industry.
- **Recipe and machine integration**: Browse recipes and multiblock previews via JEI / EMI, and inspect blocks and machines with Jade.
- **Extensibility**: Add custom recipes, materials, and machines through KubeJS and the Addon API.

## Supported Versions

| Minecraft | Target Loader | Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.20+ | 17 |
| 1.21.1 | NeoForge 21.1.243+ | 21 |

Place the matching GT6CE JAR into your instance's `mods` folder.

## Development Status

GT6CE is **in active development**. The port is still in progress, with recipes, interactions, rendering, and mod compatibility actively being refined. Full parity with original GT6 and a complete survival playthrough have not yet been verified.

## Get Involved

**Report issues on our [Issue Tracker](https://github.com/GregTech6CE/GregTech6/issues).** Please include your Minecraft and loader versions, GT6CE build filename or commit hash, reproduction steps, and relevant logs or screenshots.

Suggestions, pull requests, translations, documentation, textures, and playtest feedback are all welcome. For code contributions, describe the changes and specify which platforms were verified.

## Developers

Both target platforms are maintained in a single repository using **[Stonecutter](https://stonecutter.kikugie.dev/)**: `api` defines public extension contracts, `core` holds platform-agnostic logic and shared resources, and `minecraft` holds shared Minecraft code alongside version-specific adapters.

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
| [Developer Documentation](docs/README.md) · [Agent Guide](AGENTS.md) | Documentation entry, engineering rules, and repository layout (Chinese) |
| [Getting Started](docs/development/getting-started.md) · [Validation](docs/development/validation/README.md) | Toolchain setup, build tasks, dev runs, and verification checks (Chinese) |
| [Architecture & Packages](docs/architecture/README.md) | Source layers, platform adapters, and package responsibilities (Chinese) |
| [Addon API & KubeJS](docs/ADDON_API.zh-CN.md) | Extension contracts, SDK artifacts, and KubeJS integration (Chinese) |
| [Roadmap](docs/roadmap/v1.0.md) | Guiding principles, architecture, milestones, and contribution workflow (Chinese) |

## License

Project code is licensed under **LGPL-3.0-or-later**. See [LICENSE](LICENSE) and the [GPL-3.0 text](GPL-3.0.txt).

Assets and third-party components retain their respective licenses. Upstream GT6 default assets are licensed under CC0-1.0 unless otherwise noted, and GregTech logos and derivatives are licensed under CC BY-NC 4.0. See [NOTICE](core/src/main/resources/META-INF/gregtech6/NOTICE) for full details.
