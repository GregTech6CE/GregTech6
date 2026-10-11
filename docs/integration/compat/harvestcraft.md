# HarvestCraft 2 Food Core

Forge `1.0.5`，NeoForge `1.0.2`。两边 mod id 都是 `pamhc2foodcore`。原文件 `Compat_Recipes_HarvestCraft.java`，Copyright (c) 2025 GregTech-6 Team，LGPL-3.0-or-later。SHA-256 `35b9d325e0f65afeec0a5bd76be69ea28467fc4de90d1e1cf472245e8d03157f`，与 2026-10-05 目录里 `GregTech6/gregtech6` `3703e40308c8c030763fd6297dea8b210d2a77b1` 的原始字节一致。

Food Core 的物品名还是小写加 `item` 后缀。Forge `1.0.5` 的 jar 文件名写成 `1.20.4`，Modrinth 把它标成同时支持 1.20.1。NeoForge 用 `1.0.2`。作物、树木和 Food Extended 在 Modrinth 上没有同时覆盖 Forge 1.20.1 和 NeoForge 1.21.1 的发行包，这次不加。

还在的行：

- 粉碎机把 1 个 `minecraft:sunflower` 打成 1 个 `pamhc2foodcore:sunflowerseedsitem`。16 EU/t、16 tick。`pamhc2foodcore:sunflowerseedsitem` 的合成留下，用来证明模组配方还在。
- 搅拌机把牛肉和盐粉做成 `pamhc2foodcore:beefjerkyitem`。小堆粉 1 份、细粉 3 份各出 1 个，16 tick；整份粉配 4 块牛肉出 4 个，64 tick。盐是 GT 的 `Salt`。
- 原味甜甜圈 `plaindonutitem` 加糖粉做成 `powdereddonutitem`，比例和牛肉干一样。原版监听的是任意 `foodDonut`，Food Core 没有这个标签，所以只留下原味甜甜圈。
- 浴槽把原味甜甜圈浸入 36 mB `GenMolten_Chocolate`，得到 `chocolatedonutitem`。0 EU/t、16 tick。36 mB 是 `U/4` 对 144 mB 熔流体的换算。
- 搅拌机把 1 个 `flouritem` 和 1000 mB 水、矿泉水、蒸馏水或灵露做成 1 个 `gregtech:dough`。

关掉的配方 id：`pamhc2foodcore:beefjerkyitem`、`powdereddonutitem`、`chocolatedonutitem`、`doughitem_x2`。这些 id 两边 jar 都有。没替换的合成不删。

没有生成的包括蜡烛、硬化皮革、原味酸奶、僵尸肉干、饵料、辣椒巧克力、奶昔和双流体配方。通配矿物词典监听、合成表改写、营养值和熔炼也不在这批。

`MD.HaC` / `IL.HaC` 共 440 处。这批实现的是 Food Core 里两边都还在的机器行。明细在 [harvestcraft.json](harvestcraft.json)。

旧存档没有用这份联动测过。
