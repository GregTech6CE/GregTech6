# Project Red

Forge `4.20.0`，NeoForge `4.23.0`。原文件 `Compat_Recipes_ProjectRed.java`，Copyright (c) 2023 GregTech-6 Team，LGPL-3.0-or-later。SHA-256 `0fff207a990823fe35a346e9832c59e040edccc101e88cfac998f7a95870e024`。

Forge 不能用 `4.21.0`。`maven.modrinth:project-red-core:4.21.0` 解析出来的是 NeoForge 1.20.4 的 jar。1.20.1 对应的是 `4.20.0`，而且这份 jar 是 SRG，开发期必须 `fg.deobf`。NeoForge 用 `4.23.0`，不走 `fg.deobf`。两边都带上强制依赖：Forge 是 CodeChickenLib `4.4.0.528` 和 CB Multipart `3.3.0.159`；NeoForge 是 CodeChickenLib `4.6.1.529` 和 CB Multipart `3.5.0.155`。

原版 meta 对上现代注册名：11 是 `projectred_core:boule`，12 是 `silicon`，13 是 `infused_silicon`，40 是 `red_iron_comp`。锯是 `gregtech:tool_saw`。

还在的行：

- 关掉 `projectred_core:red_iron_comp`。原配方是 8 个红石粉围着铁锭。替换是加号形 `" D ","DID"," D "`，红石粉标签加任意铜锭，产出 1 个 `projectred_core:red_iron_comp`。
- 宝石板硅加锯，产出 4 个 `projectred_core:silicon`。
- 宝石板红石合金加锯，产出 4 个 `projectred_core:infused_silicon`。
- 切割机把 1 个 `projectred_core:boule` 切成 16 个硅片。64 EU/t、64 tick、不是食物、润滑剂 1000。水冷剂按 4 倍计为 4000，所以这条锯切展开成 6 条冷却剂行。

硅片和注红石硅的原配方 id 不删。`silicon` 仍是晶坯加 `cb_microblock:diamond_saw` 出 8 个，`infused_silicon` 和 `boule` 仍是熔炼。GT 加的是另一条合成。

内存数据包在 TOP，但 Project Red 的模组数据包注册得更晚，`listResources` 会盖掉 `forge:false` 那份文件。两边因此在配方管理器应用完之后，用 `AddReloadListenerEvent` 再按删除名单摘掉仍在的配方 id。这次只摘到 `projectred_core:red_iron_comp` 这一条。

大理石、玄武岩的 `stonetypes` / `stoneshapes` 推后。Exploration 两边都有发行包，这批不把它加进运行依赖。红石合金锭、蓝石合金锭、蓝石粉的规范形态，线圈和发光粉的材料数据，矿石归属、百合种子、数据卡和蓝图书，都不改。

`MD.PR`、`MD.PR_EXPLORATION`、`MD.PR_EXPANSION` 共 48 处：5 处实现，1 处由模块自注册覆盖，42 处推后。明细在 [projectred.json](projectred.json)。

旧存档没有用这份联动测过。
