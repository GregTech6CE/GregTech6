# 2026-10-16 / EMI issues #27、#28

用户确认处理的是 [issue #27](https://github.com/SaltNya/GregTech6/issues/27) 和 [issue #28](https://github.com/SaltNya/GregTech6/issues/28)，不是 PR。本批不处理 PR #29–31。#28 按最后澄清限定为材料形态分类：不同材质的锭归锭组，板归板组，杆归杆组；不按铜、铁等物质分组。

## 实现

- 两平台新增原生 `ToolAssemblyEmiRecipe`，复用 JEI 已用的 `ToolAssemblyCatalog`。每个工具类型/头部材料一行，全部合法头部加入 EMI 输入索引，柄的候选沿原材料限制。图案中的加工工具列为催化物；工具装配仅作展示，不启用自动配方树（输出使用代表性柄）。安装 EMI 时不再通过 JEI 桥重复注册这批行，独立 JEI 保留既有注册。
- `core` 共享资源添加 56 个 Reliable EMI 材料形态组，通过现有 `gregtech:<prefix>` 标签收集不同材质。没有使用 `gregtech:material/<material>` 物质标签。工具、机器、玻璃、木板、半砖、模具等不新增分组。防腐木板虽绑定 plate/WoodTreated，仍显式排除；同时排除原版及 GT 木板标签。
- 组名直接复用既有中英文 `itemGroup.gregtech.*` 键，无新增翻译或语言改写。生成器 `tools/integration/generate_remi_material_groups.py --check` 核对全部资源和双语键。Reliable EMI 未安装时，普通 EMI 忽略这些资源，物品列表维持既有行为；不增加硬依赖，不写用户配置。
- 候选槽复用显式列表 `MachineEmiIngredient`，避免为每个候选集合重新扫描所有物品标签。

## 有限验证

- Forge 1.20.1 / NeoForge 1.21.1 主源码增量编译一次通过，41 秒；未新增编译错误，既有弃用警告保留。
- 56 个资源生成一致性及中英文键检查通过；源码核对两版材料总标签入口和 Reliable EMI 两分支的资源加载、优先级、标签/排除项解析。两平台新配方类内容一致。
- `git diff --check` 通过。按减少测试要求，不跑全套测试、客户端或正式打包。EMI 实际用途点击、Reliable EMI 实际折叠视觉与游戏内重载尚未验证，不能以编译代替游戏通过。
- 本地提交，未推送、评论或关闭 GitHub issues。此前成品不含此批。

编译日志 `work/emi-issues-20261016/compile.log`，SHA-256 `55c93af0200b20d5455fad04eba30c0ab70f482c68b56061cd2288f1ad3cb54b`。

## 外部格式来源

参考 [Evan Bowness 的 Reliable EMI](https://github.com/evanbones/Reliable-EMI) 的 `1.20.1`、`1.21.1` 分支中 `StackGroupManager.java` 和 `EmiStackGroup.java`。本批只按格式编写集成资源，没有复制第三方实现。上游许可证为 MIT，Copyright (c) 2026 Evan Bowness；下载的 LICENSE SHA-256 为 `4ead50e4f973868829d7d718fa9702e6e915897a33b75b8683097abc4d55eb2e`。

读取文件保存于忽略目录 `work/pr-review-20261016/remi/`，SHA-256：

| 分支 | 文件 | SHA-256 |
| --- | --- | --- |
| 1.20.1 | StackGroupManager.java | af49687941913342edc8165a62133a172effbf281e544302c266b9c00bfb2738 |
| 1.20.1 | EmiStackGroup.java | f9a18126bbe0287203dcebe95d7e82a1bc2d3caae7e4f8f0c1ac64843b4c25e7 |
| 1.21.1 | StackGroupManager.java | a2c0bbf412dcfe64ee2711da75357ccf0bfacf9bb78751cf16afb35fca1a5627 |
| 1.21.1 | EmiStackGroup.java | d7a8d154333172e081c9cb4b33f10bc554efbb0a96ebf2b799cf3c1800712095 |
