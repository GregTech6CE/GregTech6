# GregTech 6 Community Edition

[English](README.md) | **简体中文**

一个面向现代 Minecraft 的 GregTech 6 社区整合项目，通过共享核心和各自的 Forge、NeoForge 平台实现持续移植与开发。

当前实验分支使用 **Stonecutter 0.7.11** 管理全部原生源集及资源，详见[迁移与生成器说明](docs/integration/STONECUTTER_MIGRATION.md)。

## 项目介绍

GregTech 6 Community Edition 整合了 **saltnya 的 gregtech6reborn**、**brokestar233 的 gregtech6** 和 **lombinaxmasson 的 cruciblecraft** 三个项目的工作。项目以 saltnya 的实现作为初始内容基准，逐项比较并吸收三个移植项目的完善部分。

目标是形成系统互通、便于维护的模组，让材料、配方、机器、能源和物流使用共享底层，并为各 Minecraft 版本保留必要的平台适配。

GregTech 6 以科技、资源加工和工业发展为核心。本项目重点整合材料与合金、工具、热量与坩埚、铸造、蒸汽、机器与发电设备、自动化、多方块设备和世界生成。

## Minecraft 版本

- **Minecraft 1.20.1 — Forge**，目标依赖为 Forge **47.4.20**。
- **Minecraft 1.21.1 — NeoForge**，目标依赖为 NeoForge **21.1.243**。

两个平台在同一仓库开发，共用 `core` 模块，并分别生成包含共享核心的模组 JAR。安装时选择与 Minecraft 版本和加载器对应的产物，无需额外安装 core JAR。

模组名称为 **GregTech 6 Community Edition**，加载 ID 与内容／资源命名空间统一为 **`gt6ce`**；物品、方块、流体和配方 ID 均使用 `gt6ce:`。

原 `gregtech:` 身份不保留兼容映射，引用它们的旧存档和脚本需要另行迁移。

## 项目状态

项目正在持续整合。双版本功能对齐、完整生存发展流程、客户端表现、专用服务器及世界持久化仍在验证中。来源项目的旧世界兼容性和跨 Minecraft 版本存档兼容性尚未确认。

建议在独立游戏实例中评估开发版本，并备份已有世界。当前进度与已知缺口见[整合状态](docs/integration/STATUS.md)和[验证记录](docs/integration/VERIFICATION.md)。

## 从源码构建

### 环境要求

- **JDK 17**：用于 Gradle 启动、Forge 和共享核心。
- **JDK 21**：用于 NeoForge。
- 首次下载依赖时需要联网。

仓库包含 **Gradle wrapper 8.8**，请在仓库根目录执行命令。将 `JAVA17_HOME` 和 `JAVA21_HOME` 设置为各自 JDK 的安装目录，并将 `JAVA_HOME` 设置为 `JAVA17_HOME`。

Windows PowerShell 示例，请替换为自己的安装路径：

```powershell
$env:JAVA17_HOME = 'C:\path\to\jdk-17'
$env:JAVA21_HOME = 'C:\path\to\jdk-21'
$env:JAVA_HOME = $env:JAVA17_HOME
```

### 构建模组

Forge 1.20.1：

```powershell
.\gradlew.bat --console=plain :multiversion:1.20.1-forge:distributionJar
```

NeoForge 1.21.1：

```powershell
.\gradlew.bat --console=plain :multiversion:1.21.1-neoforge:jar
```

两个版本：

```powershell
.\gradlew.bat --console=plain :assembleBothPlatforms
```

Linux 或 macOS 使用相同的环境变量，以 `./gradlew` 替代 `.\gradlew.bat`。必要时先执行 `chmod +x gradlew`。

Forge 最终产物位于 `multiversion/versions/1.20.1-forge/build/libs/`，NeoForge 最终产物位于 `multiversion/versions/1.21.1-neoforge/build/libs/`。Forge 的 `multiversion/versions/1.20.1-forge/build/forge-intermediates/` 目录存放中间产物。模组版本在 `gradle.properties` 中配置。

完整环境配置、更多构建选项及 Git 交接步骤见 [BUILDING.md](docs/work/BUILDING.md)。

## 开发环境

在 IntelliJ IDEA 或其他支持 Gradle 的 IDE 中，以 Gradle 项目打开仓库根目录，并为各模块选择相应的 JDK。

Forge 开发命令：

```powershell
.\gradlew.bat :runClient
.\gradlew.bat :runServer
```

NeoForge 开发命令：

```powershell
.\gradlew.bat :multiversion:1.21.1-neoforge:runClient
.\gradlew.bat :multiversion:1.21.1-neoforge:runServer
```

每条命令分别执行。服务器环境需要接受 Minecraft EULA。已有 Windows 辅助脚本也可运行共享核心检查并构建两个平台：

```powershell
.\scripts\build.ps1 -Target all
```

### 仓库结构

```text
GregTech6/
├── multiversion/         Stonecutter 共同原生源码与版本覆盖
├── core/                 共享逻辑、数据与资源
├── gradle/               Gradle wrapper
├── scripts/              构建与开发辅助脚本
├── .github/workflows/    双平台 CI
└── docs/integration/     计划、来源、取舍与验证记录
```

## 支持与贡献

请通过本仓库的 Issue 跟踪器反馈问题或提出功能建议。问题报告应包含 Minecraft 版本、加载器版本、模组版本、相关日志和清楚的复现步骤。

欢迎贡献代码、文档、翻译、测试和素材。请保留原作者署名与许可证声明，说明改动的来源和目的，并记录检查过的平台。共享玩法规则放在 `core` 中，加载器及 Minecraft API 适配放在各平台模块中。

较大改动开始前，请阅读[协作说明](docs/integration/COLLABORATION.md)、[整合计划](docs/integration/PLAN.md)和[来源记录](docs/integration/SOURCES.md)。

## Addon 开发

两版提供共享 `GregTechAddon` / `GregTechAddons` 配方生命周期、材料领域与能源合同。用 `:core:addonApiJar` 构建仅供编译的 SDK，Addon 不应再次打包这些类。见 [Addon 接口说明](docs/ADDON_API.zh-CN.md)及[电压等级说明](docs/VOLTAGE_TIERS.md)。API版本1目前支持配方就绪回调，新材料自动注册生命周期尚待完善。

## 许可证

项目代码采用 **LGPL-3.0-or-later**。saltnya 项目所有者于2026-10-02确认采用另外两份移植项目的代码许可。全文见 [LICENSE](LICENSE)、[GPL 3.0 全文](docs/licenses/GPL-3.0.txt)，署名与分项范围见 [NOTICE](NOTICE) 和[来源许可记录](docs/integration/LICENSE_REVIEW.md)。

原 GT6 默认资产仍为 CC0-1.0（另有声明除外）；GregTech 标志及衍生物仍为 CC BY-NC 4.0。其他第三方声明和作者署名继续保留，代码许可不覆盖重授权资产。原 Forge MDK 模板原文保存在来源审计中，与当前项目声明分开。

## 致谢

- **Gregorius Techneticies 与 GregTech 6 团队**：原版 GregTech 6。
- **saltnya**：gregtech6reborn 与初始内容基准。
- **brokestar233**：gregtech6-main 及相关移植工作。
- **Lorbineitte Masson**：cruciblecraft 及相关移植工作。
- 保留的来源声明中记载的原贡献者、翻译者、美术作者及第三方作者。

详细来源与可获得的历史记录见 [SOURCES.md](docs/integration/SOURCES.md) 和 `core/provenance/`。
