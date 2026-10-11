# Mekanism

Forge `10.4.16.80`，NeoForge `10.7.19.85`。原文件 `Compat_Recipes_Mekanism.java`，Copyright (c) 2021 GregTech-6 Team，LGPL-3.0-or-later。SHA-256 `1961dcbdc1f1d54fbb11fb48a9f19c53692ab152a122454d2eff6817695f4be6`。

原版做两件事：删掉 2×2 盐合成盐块，以及用 16 色染料给气球、塑料栅栏、发光板和五种塑料方块染色。后一件在 Mekanism 10 里没有目标物品，两边 jar 都没有 Balloon、PlasticFence、GlowPanel、RoadPlasticBlock、PlasticBlock、SlickPlasticBlock、GlowPlasticBlock、ReinforcedPlasticBlock。这些行不生成。

还在的配方是 `mekanism:storage_blocks/salt`：2×2 `forge:dusts/salt` / `c:dusts/salt` 合成 `mekanism:block_salt`。联动按这个 id 关掉它。`mekanism:hdpe_rod` 留下来，用来证明模组的合成表确实加载了。没装 Mekanism 时不写这个文件。

没有机器行。`MekanismAPI.addBoxBlacklist`、把 Mekanism 粉锭矿当成 GT 规范形态、锇的提示文字替换、矿物替换石头，都不在这批。

`MD.Mek` 共 42 处：1 处实现，2 处由这条删除和模块自注册覆盖，2 处是 API 桥，37 处推后。明细在 [mekanism.json](mekanism.json)。

旧存档没有用这份联动测过。
