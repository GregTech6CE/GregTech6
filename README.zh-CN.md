# GregTech 6 Community Edition

[English](README.md) · **简体中文** · [问题反馈](https://github.com/GregTech6CE/GregTech6/issues)

**将 GregTech 6 带到现代 Minecraft。**

Minecraft 1.20.1 · Forge | Minecraft 1.21.1 · NeoForge

## ⚠️ 开发版本提醒

**GT6CE 目前处于快速更迭期，尚未进入稳定阶段。更新不只涉及内容与修复，底层架构也可能发生重大调整。**

物品与方块 ID、存档数据格式、材料与配方系统、机器逻辑、世界生成，以及 Addon API 和脚本接口，都可能继续修改或重构。**不同构建之间不保证存档、附属模组和整合包的兼容性，也不保证所有旧数据都能自动迁移。**

更新后，已有产线可能需要调整，附属与脚本可能需要适配；旧存档也可能出现物品或方块丢失、机器数据异常等问题，必要时需要新建世界。

- 建议使用独立实例进行测试，谨慎用于长期存档或正式服务器。
- **更新前请完整备份存档、配置和脚本，并保留对应的旧版模组文件。** 不要依赖降级模组来恢复已被新版修改的存档。
- 制作整合包或附属时，请固定所用构建，并在升级前重新核对兼容性。

## 欢迎来到 GT6CE

**GregTech 6 Community Edition**，简称 **GT6CE**，是 GregTech 6 面向现代 Minecraft 的社区移植项目。我们的目标是将 GT6 的材料加工、手工冶金与工业发展体验带到 **1.20.1 Forge** 和 **1.21.1 NeoForge**。

GregTech 6 把生产中的细节交到你手中。控制坩埚温度、浇铸各种零件，生产新的材料，再想办法驱动新的机器。工厂就是这样一步步建立起来的：

- 从**工具与手工加工**起步，在坩埚中处理金属，用模具浇铸零件。
- 用锅炉、涡轮和引擎，让**蒸汽与机械动力**参与生产。
- 将**加工机器、发电设备和多方块设施**连接成产线，再用管道、电缆与存储为它们供应资源。

项目以原版 GT6 的材料、配方和机器行为为参照，同时适配现代 Minecraft 及其模组生态。

## 你能在这里找到什么

- **坩埚、合金与铸造**：处理熔融金属、调配合金，使用模具和浇铸道制作零件。温度与材料属性都是工艺的一部分。
- **不同材料的工具与零件**：把金属、合金和矿物加工为具有不同属性与用途的工具和组件。
- **多种能源形式**：使用热能、蒸汽、旋转能、动能和电能，通过不同设备完成能源的产生、转换与利用。
- **管道、电缆与覆盖板**：运输物品和流体，连接能源网络，并通过覆盖板控制机器各面的功能。
- **多方块工业设备**：建造焦炉、蒸馏塔、大型储罐与大型加工设施。
- **值得勘探的世界**：寻找矿脉、岩层和地表资源，为工业发展准备原料。
- **配方与机器信息**：通过 JEI / EMI 集成浏览配方和多方块结构，通过 Jade 查看方块与机器信息。
- **扩展与定制**：使用 KubeJS 接口和 Addon API 添加配方、材料与机器。

## 支持版本

| Minecraft | 构建所用加载器 | Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.20+ | 17 |
| 1.21.1 | NeoForge 21.1.243+ | 21 |

将匹配的 GT6CE JAR 放入游戏实例的 `mods` 文件夹。

## 开发状态

GT6CE **正在持续开发中**。移植尚未完成，配方、交互、渲染与兼容功能仍在完善；对原版 GT6 的全面对齐和完整生存流程尚未验证完成。

建议在独立实例中体验开发版，更新前备份存档。

## 参与项目

**发现问题，请通过 [Issue 跟踪器](https://github.com/GregTech6CE/GregTech6/issues)反馈。** 请附上 Minecraft 与加载器版本、GT6CE 构建文件名或提交号、复现步骤，以及相关日志或截图。

欢迎提出建议、提交代码、翻译、文档、贴图与玩法报告。代码贡献请说明改动内容和已检查的平台。

## 开发者

两个版本在同一仓库中使用 **[Stonecutter](https://stonecutter.kikugie.dev/)** 维护。`core` 存放共享逻辑与资源，`multiversion` 存放共用的 Minecraft 相关代码及版本适配。

在仓库根目录选择所需的构建命令：

```powershell
# Forge 1.20.1 发行 JAR
.\gradlew.bat :multiversion:1.20.1-forge:distributionJar

# NeoForge 1.21.1 发行 JAR
.\gradlew.bat :multiversion:1.21.1-neoforge:jar

# 同时构建两个版本
.\gradlew.bat assembleBothPlatforms
```

最终模组 JAR 位于 `multiversion/versions/<version-loader>/build/libs/`。

| 文档 | 内容 |
| --- | --- |
| [Addon API](docs/ADDON_API.zh-CN.md) · [附属示例](examples/addon-sample/README.md) | 材料、配方、机器等扩展入口 |
| [KubeJS 示例](examples/kubejs-sample/README.md) | 脚本配方与机器声明 |

## License

项目代码采用 **LGPL-3.0-or-later**，见 [LICENSE](LICENSE) 与 [GPL 全文](docs/licenses/GPL-3.0.txt)。

资源与第三方组件沿用各自的许可。原 GT6 默认资产为 CC0-1.0（另有声明除外），GregTech 标志及衍生物为 CC BY-NC 4.0；适用声明见 [NOTICE](NOTICE)。
