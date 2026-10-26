# GregTech 6 Community Edition

**English** · [简体中文](README.zh-CN.md) · [Issue Tracker](https://github.com/GregTech6CE/GregTech6/issues)

**Bringing GregTech 6 to modern Minecraft.**

Minecraft 1.20.1 · Forge | Minecraft 1.21.1 · NeoForge

## ⚠️ Development Build Notice

**GT6CE is undergoing rapid development and has not reached a stable stage. Updates may include major changes to the underlying architecture, alongside new content and fixes.**

Item and block IDs, save data formats, material and recipe systems, machine behavior, world generation, the addon API, and scripting interfaces may all change or be reworked. **Compatibility between builds is not guaranteed for existing worlds, addons, or modpacks, and automatic migration may not be available for all older data.**

After an update, production lines may need adjustments, and addons or scripts may need changes. Existing worlds may also encounter missing items or blocks, invalid machine data, or other compatibility issues. Some updates may require starting a new world.

- Use a separate instance for testing, and exercise caution with long-term worlds or production servers.
- **Back up your worlds, configuration, and scripts before updating, and keep the matching previous mod files.** Do not rely on downgrading the mod to restore a world that a newer build has already modified.
- When developing a modpack or addon, pin the build you use and check compatibility again before upgrading.

## Welcome to GT6CE

**GregTech 6 Community Edition**, or **GT6CE**, is a community port of GregTech 6 to modern Minecraft. Our goal is to bring GT6's material processing, hands-on metallurgy, and industrial progression to **1.20.1 Forge** and **1.21.1 NeoForge**.

GregTech 6 puts the details of production in your hands. Control the temperature of your crucible, cast a variety of parts, produce new materials, and work out how to power new machines. This is how your factory takes shape, step by step:

- Start with **tools and manual processing**, working metals in crucibles and casting them in molds.
- Put **steam and mechanical power** to work with boilers, turbines, and engines.
- Connect **processing machines, generators, and multiblocks** into production lines, with pipes, cables, and storage keeping everything supplied.

We use original GT6 as the reference for materials, recipes, and machine behavior, while adapting the mod to modern Minecraft and its modding ecosystem.

## What you will find

- **Crucibles, alloys, and casting.** Work with molten metals, alloy compositions, molds, and casting channels. Temperature and material properties are part of the process.
- **Material-based tools and parts.** Turn metals, alloys, and minerals into tools and components with different properties and uses.
- **More than one kind of power.** Use heat, steam, rotational power, kinetic power, and electricity, with machines that generate, convert, and consume them.
- **Pipes, cables, and covers.** Move items and fluids, connect energy networks, and control machine faces with covers.
- **Multiblock machinery.** Build coke ovens, distillation towers, large tanks, and larger processing equipment.
- **A world to prospect.** Find ore veins, stone layers, and surface resources to supply your industry.
- **Recipe and machine information.** Browse recipes and multiblock structures through JEI / EMI integration, and view block and machine information with Jade.
- **Room for customization.** KubeJS integration and an addon API for adding recipes, materials, and machines.

## Supported versions

| Minecraft | Loader build target | Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.20+ | 17 |
| 1.21.1 | NeoForge 21.1.243+ | 21 |

Place the matching GT6CE JAR in your instance's `mods` folder.

## Development status

GT6CE is **under active development**. The port is not yet complete, and recipes, interactions, rendering, and compatibility are still being refined. Full original-GT6 parity and the complete survival progression have not been verified.

Try development builds in a separate instance and back up worlds before updating.

## Get involved

**Please report problems through our [issue tracker](https://github.com/GregTech6CE/GregTech6/issues).** Include the Minecraft and loader versions, the GT6CE build filename or commit, reproduction steps, and relevant logs or screenshots.

Suggestions, code, translations, documentation, textures, and gameplay reports are welcome. For code contributions, explain the change and which platform you checked.

## Developers

Both versions are maintained in one repository with **[Stonecutter](https://stonecutter.kikugie.dev/)**. `core` holds shared logic and resources; `minecraft` holds shared Minecraft-facing code and version-specific adaptations.

Run the command for the artifact you need from the repository root:

```powershell
# Forge 1.20.1 distribution JAR
.\gradlew.bat :minecraft:1.20.1-forge:distributionJar

# NeoForge 1.21.1 distribution JAR
.\gradlew.bat :minecraft:1.21.1-neoforge:jar

# Both distributions
.\gradlew.bat assembleBothPlatforms
```

Final mod JARs are written to `minecraft/versions/<version-loader>/build/libs/`.

| Resource | Purpose |
| --- | --- |
| [Developer documentation](docs/README.md) · [Agent guide](AGENTS.md) | Progressive guides, working rules, and file placement (Chinese) |
| [Getting started](docs/development/getting-started.md) · [Validation](docs/development/validation.md) | Environment, builds, development runs, and checks (Chinese) |
| [Architecture and packages](docs/architecture/README.md) | Core, Minecraft, platform adapters, and package responsibilities (Chinese) |
| [Addon API and KubeJS](docs/ADDON_API.zh-CN.md) | Extension points, SDK boundaries, and sample availability (Chinese) |
| [Release and deployment records](docs/operations/README.md) | Artifact checks, instance updates, and sanitized records (Chinese) |

## License

Project code is licensed under **LGPL-3.0-or-later**. See [LICENSE](LICENSE) and the [GPL text](GPL-3.0.txt).

Assets and third-party components retain their own licenses. Upstream GT6 default assets are CC0-1.0 unless otherwise stated; GregTech logos and derivatives are CC BY-NC 4.0. See [NOTICE](core/src/main/resources/META-INF/gregtech6/NOTICE) for the applicable notices.
