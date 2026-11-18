# GregTech 6 Community Edition

[English](README.md) · **简体中文** · [问题反馈](https://github.com/GregTech6CE/GregTech6/issues)

**面向现代 Minecraft 的 GregTech 6 社区移植与演进项目。**

Minecraft 1.20.1 · Forge | Minecraft 1.21.1 · NeoForge

## ⚠️ 版本与兼容提醒

**GT6CE 已发布 `v1.0` 基线版本，当前处于 `v1.0 ~ v1.1` 快速迭代期。在此期间，更新除补充内容与修复缺陷外，仍可能包含破坏性的架构与注册项调整。**

物品与方块 ID、存档数据格式、材料与配方系统、机器逻辑、世界生成，以及 Addon API 和脚本契约均在持续收敛定型。**在 `v1.1` 结束并正式进入存档兼容周期前，不同构建之间不保证现有存档、附属模组和整合包的跨版本兼容**（`v1.1` 结束后若有结构性调整，将尽力提供 1～2 个版本窗口内的迁移支持）。

- 建议使用独立游戏实例体验测试，谨慎用于长期生存存档或正式服务器。
- **更新前请完整备份存档、配置与脚本，并保留对应的旧版模组文件。** 已被新版修改保存过的存档无法通过降级模组恢复。
- 制作整合包或开发附属时，请锁定具体构建版本，并在升级前重新验证兼容性。

## 欢迎来到 GT6CE

**GregTech 6 Community Edition**（简称 **GT6CE**）致力于将 GregTech 6 独树一帜的热力学冶金、多能量工业协同与世界内实体工坊引入 **1.20.1 Forge** 与 **1.21.1 NeoForge**。

在 GT6CE 中，工业生产不是单调的“菜单进度条与电压叠叠乐”，而是触手可及的车间工程——调控坩埚温区、雕刻浇铸模具、在砧上锻打零件，并为每台设备调配契合其物理特性的能量源：

- 从**火与陶冶时代**起步，根据地表散落岩石勘探矿脉，烧制陶坩埚并在 `5×5` 模具中浇铸青铜与生铁。
- 迈入**蒸汽与钢铁机械时代**，借助锅炉、涡轮、引擎、传动轴与齿轮箱，将热能、旋转能与动能直驱投入机械工坊。
- 迈向**电气化、重化工、激光光刻与核能时代**，用管道、线缆、料斗、覆盖板与多方块设施搭建环环相扣的自动化工业帝国。

## 核心特性

- **热力学坩埚与双轨冶金**：基于真实开尔文熔点、沸点与比热容的开放式冶金体系，支持合金配比、模具与坩埚浇铸道/浇铸口流控，并从前期手作演进为小坩埚特种精密制造与大坩埚大宗量产的双轨自动化。
- **十大物理能量协同**：涵盖热能（`HU`）、低温能（`CU`）、蒸汽（`SU`）、旋转能（`RU`）、动能（`KU`）、电能（`EU`）、磁力能（`MU`）、光能（`LU`）、量子能（`QU`）与时间（`TU`），因地制宜直驱产线并通过多能量协同突破工艺瓶颈。
- **世界内实体交互**：“空间里的工坊，而非菜单里的进度条”。包含 `5×5` 像素掩码模具雕刻、双槽石头砧/青铜砧/钢砧物理锤锻、物理指针传感器表盘，以及支持换头修复的模块化工具体系。
- **管道、料斗与表面覆盖板**：传输物品、流体与能量，在机器六面直接安装输送带、泵、调节器与红石检测覆盖板，配合扳手与螺丝刀实现精细的 I/O 分流与定量控制。
- **多方块重工设施**：建造焦炉、蒸馏塔核心、大型坩埚、大型热交换器、核反应堆、聚变反应堆与物流核心。
- **立体地质勘探**：观察地表岩石、矿脉指示花与基岩流体泉，探寻连续岩层、大型矿脉与深层矿藏。
- **配方树与原生遥测**：通过 JEI / EMI 查阅配方、矿物处理全景图与多方块结构预览，并在 Jade 与 GUI 中直接读取分单位耗能、坩埚温控预警与锅炉工况。
- **高可定制与扩展契约**：提供细粒度配置、数据包、KubeJS 脚本支持与窄语义双平台 Addon SDK。

## 支持版本

| Minecraft | 目标加载器版本 | Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.20+ | 17 |
| 1.21.1 | NeoForge 21.1.243+ | 21 |

将对应版本的 GT6CE JAR 放入游戏实例的 `mods` 目录即可运行。

## 开发状态

GT6CE 已在双平台发布 **`v1.0` 基线版本**，完成了材料与回收单一事实来源收敛、首轮资源与模型瘦身以及基础生存链路打通。当前正按 [`v1.0 ~ v1.4` 整体路线图](docs/roadmap/README.md) 推进后续里程碑——涵盖窄语义 Addon API 稳定、原生运行遥测、边缘系统附属化、世界生成与管网重构，以及坩埚双轨冶金与原生物流重塑。近期版本改动详见 [v1.0 更新日志](docs/changelog/v1.0.zh-CN.md)。

## 参与贡献

**遇到问题或提交建议请前往 [GitHub Issues](https://github.com/GregTech6CE/GregTech6/issues)。** 反馈缺陷时请附上 Minecraft 与加载器版本、GT6CE 构建文件名或提交号、复现步骤，以及相关日志或截图。

欢迎参与代码开发、RFC 方案讨论、本地化翻译、文档编写、贴图绘制与生存实测。提交代码 PR 时请说明改动立意、影响范围及已验证的平台。

## 开发者指南

项目使用 **[Stonecutter](https://stonecutter.kikugie.dev/)** 在同一仓库内维护双平台版本：`api` 定义公开扩展契约，`core` 承载平台无关的核心规则与共享资源，`minecraft` 存放跨版本共享运行时与加载器适配层。

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
| [开发上手](docs/development/getting-started.md) · [验证与验收](docs/development/validation/README.md) | 环境搭建、构建运行与验证门禁 |
| [架构与包职责](docs/architecture/README.md) | 源码分层、平台适配与包职责说明 |
| [Addon API 与 KubeJS](docs/addon/README.md) | 扩展契约、SDK 构建与脚本集成 |
| [整体路线图](docs/roadmap/README.md) · [设计方针](docs/decisions/policies.md) · [科技演进路线](docs/decisions/progression.md) | 九大设计方针、七大工业时代科技树与分版本里程碑规划 |
| [更新日志](docs/changelog/v1.0.zh-CN.md) | 分版本面向用户的更新记录 |

## 许可证

本项目代码采用 **LGPL-3.0-or-later** 许可协议，详情见 [LICENSE](LICENSE) 与 [GPL-3.0 全文](GPL-3.0.txt)。

美术资产与第三方组件保留其各自许可。原版 GT6 默认资产除另有说明外均采用 CC0-1.0，GregTech 标志及其衍生物采用 CC BY-NC 4.0；完整声明见 [NOTICE](core/src/main/resources/META-INF/gregtech6/NOTICE)。
