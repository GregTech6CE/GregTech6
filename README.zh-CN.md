# GregTech 6 Community Edition

[English](README.md) · **简体中文** · [问题反馈](https://github.com/GregTech6CE/GregTech6/issues)

**面向现代 Minecraft 的 GregTech 6 社区移植版。**

Minecraft 1.20.1 · Forge | Minecraft 1.21.1 · NeoForge

## ⚠️ 开发版本提醒

**GT6CE 目前处于快速开发期，尚未达到稳定阶段。后续更新不仅包含内容补充与问题修复，底层架构也可能进行较大调整。**

物品与方块 ID、存档数据格式、材料与配方系统、机器逻辑、世界生成，以及 Addon API 和脚本接口，均可能继续演进或重构。**不同构建之间不保证对现有存档、附属模组和整合包的兼容性，也不保证所有旧数据都能自动迁移。**

更新版本后，已有产线、附属或脚本可能需要相应适配，旧存档也可能出现注册项缺失或机器数据异常等情况；部分重大变更可能需要新建世界。

- 建议使用独立游戏实例进行测试，谨慎用于长期存档或正式服务器。
- **更新前请完整备份存档、配置与脚本，并保留对应的旧版模组文件。** 已经被新版修改过的存档无法通过降级模组恢复。
- 制作整合包或附属时，请锁定所用构建版本，并在升级前重新确认兼容性。

## 欢迎来到 GT6CE

**GregTech 6 Community Edition**（简称 **GT6CE**）是 GregTech 6 面向现代 Minecraft 的社区移植项目，致力于将 GT6 的材料加工、手工冶金与工业发展体系引入 **1.20.1 Forge** 与 **1.21.1 NeoForge**。

在 GregTech 6 中，生产过程中的每个环节都由你亲手掌控——调控坩埚温度、浇铸精密零件、提炼新材料，并规划各机器的能源供应。你的工厂将一步步成形：

- 从**工具与手工加工**起步，在坩埚中熔炼金属并用模具浇铸零件。
- 利用锅炉、涡轮与引擎，将**蒸汽与机械动力**投入生产。
- 通过管道、电缆与存储设备，把**加工机器、发电设备与多方块设施**串联为自动化产线。

项目以原版 GT6 的材料、配方和机器行为为基准，同时针对现代 Minecraft 及其模组生态进行适配。

## 核心特性

- **坩埚、合金与铸造**：掌控熔融金属、合金配比、模具与浇铸道，让温度与材料特性真正参与加工流程。
- **材料驱动的工具与零件**：将金属、合金与矿物加工为属性各异的工具和组件。
- **多样化能源体系**：涵盖热能、蒸汽、旋转能、动能与电能，通过各类设备完成产生、转换与消耗。
- **管道、电缆与覆盖板**：运输物品与流体、搭建能源网络，并使用覆盖板精细控制机器各面行为。
- **多方块工业设施**：建造焦炉、蒸馏塔、大型储罐与重型工业设备。
- **地质勘探**：探寻矿脉、岩层与地表矿物资源，为工业发展提供原料。
- **配方与信息集成**：通过 JEI / EMI 浏览配方与多方块结构预览，通过 Jade 查看方块与机器状态。
- **可扩展接口**：提供 KubeJS 脚本支持与 Addon API，用于扩展自定义配方、材料与机器。

## 支持版本

| Minecraft | 目标加载器版本 | Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.20+ | 17 |
| 1.21.1 | NeoForge 21.1.243+ | 21 |

将对应版本的 GT6CE JAR 放入游戏实例的 `mods` 目录即可运行。

## 开发状态

GT6CE **仍在持续开发中**。移植工作尚未全部完成，配方、交互、渲染与模组兼容仍在不断完善，原版 GT6 的全面对齐及完整生存流程尚待最终验证。

## 参与贡献

**遇到问题请前往 [GitHub Issues](https://github.com/GregTech6CE/GregTech6/issues) 提交反馈。** 反馈时请附上 Minecraft 与加载器版本、GT6CE 构建文件名或提交号、复现步骤，以及相关日志或截图。

欢迎提出建议，或参与代码开发、翻译、文档编写、贴图绘制与游玩测试。提交代码贡献时，请说明改动内容及已验证的平台。

## 开发者指南

项目使用 **[Stonecutter](https://stonecutter.kikugie.dev/)** 在同一仓库中维护双版本：`api` 定义公开扩展契约，`core` 存放平台无关的核心逻辑与共享资源，`minecraft` 存放跨版本 Minecraft 代码与平台适配层。

在仓库根目录运行 Gradle 任务构建对应产物（Linux/macOS 使用 `./gradlew`，Windows 使用 `.\gradlew.bat`）：

```bash
# Forge 1.20.1 发行 JAR
./gradlew :minecraft:1.20.1-forge:distributionJar

# NeoForge 1.21.1 发行 JAR
./gradlew :minecraft:1.21.1-neoforge:jar

# 同时构建双平台版本
./gradlew assembleBothPlatforms
```

构建生成的模组 JAR 位于 `minecraft/versions/<version-loader>/build/libs/`。

| 文档 | 内容 |
| --- | --- |
| [开发文档总入口](docs/README.md) · [Agent 指南](AGENTS.md) | 文档导航、工作规范与目录归属 |
| [开发上手](docs/development/getting-started.md) · [验证与验收](docs/development/validation/README.md) | 环境搭建、构建运行与验证流程 |
| [架构与包职责](docs/architecture/README.md) | 源码分层、平台适配与包职责说明 |
| [Addon API 与 KubeJS](docs/ADDON_API.zh-CN.md) | 扩展契约、SDK 构建与脚本集成 |
| [路线图](docs/roadmap/v1.0.md) | 开发方针、架构规划、版本节点与协作约定 |

## 许可证

本项目代码采用 **LGPL-3.0-or-later** 许可协议，详情见 [LICENSE](LICENSE) 与 [GPL-3.0 全文](GPL-3.0.txt)。

美术资产与第三方组件保留其各自许可。原版 GT6 默认资产除另有说明外均采用 CC0-1.0，GregTech 标志及其衍生物采用 CC BY-NC 4.0；完整声明见 [NOTICE](core/src/main/resources/META-INF/gregtech6/NOTICE)。
