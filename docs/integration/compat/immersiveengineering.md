# Immersive Engineering

现代 id `immersiveengineering`。Forge 1.20.1 用 `10.2.0-183`，NeoForge 1.21.1 用 `12.4.2-194`。两边 jar 都有这些注册名：`wooden_barrel`、`coke`、`dust_coke`、`hemp_fiber`、`hemp_fabric`、`stick_treated`、`treated_wood_horizontal`、`treated_wood_packaged`、`stairs_treated_wood_horizontal`、`slab_treated_wood_horizontal`、`hammer`。麻纤维标签是 `forge:fiber_hemp` / `c:fiber_hemp`，木棍标签是 `forge:rods/wooden` / `c:rods/wooden`，两个 jar 里各自只列了 `hemp_fiber` 和 `stick_treated`。锤子压板配方 id 是 `immersiveengineering:crafting/plate_{aluminum,constantan,copper,electrum,gold,iron,lead,nickel,silver,steel,uranium}_hammering`。锤子自己的配方 `crafting/hammer` 保留。

来源 `gregtech/compat/Compat_Recipes_ImmersiveEngineering.java`，SHA-256 `02c2e258448c4e99ec4f1a92529cee5dc59402cead301c81ed1cb25b4a045efc`。文件头是 Copyright (c) 2021 GregTech-6 Team，LGPL-3.0-or-later。其余 20 个文件的散列在 [immersiveengineering.json](immersiveengineering.json)。没有改授权。

| 状态 | 处数 | 这次怎么处理 |
| --- | ---: | --- |
| implemented | 21 | 锯木桶、双向 Generifier、两项无序合成、焦炭压缩和粉碎、织麻布、11 种木头的楼梯/半砖油浴、按 id 关掉 11 个锤子压板 |
| covered | 6 | 木板油浴已经产出 `gregtech:planks_treated`。不再加第二条 IE 木板产物，否则 `findRecipe` 不稳定 |
| external_bridge | 9 | `ic2_compressor`、`pulverizing`、IE 电弧炉加渣。外部机器镜像不恢复 |
| deferred | 46 | Railcraft 枕木和木馏油木材；Thermal 搅拌用到的 IE 炉渣；矿石配方里 IE 炉渣只是 Thermal/Factorization 之后的后备；防火木板没有注册 |
| not_applicable | 68 | 统一目标、组成质量、书籍、蓝图扫描/打印、药水 id、能量类名判断、锤子点击、村庄战利品 |

行为和原版的差别：

- 木板油浴在 IE 存在时原版优先产出 IE 处理木板。这边继续产出 GT 处理木板，再用 Generifier 和无序合成跟 `treated_wood_horizontal` 互转。
- 防火处理木板没有这套方块，对应两行不生成。
- 原版删掉所有含 IE 锤子的无序配方。这边只覆盖那 11 个压板 id。
- `GT6_Main` 里的浴槽假配方不另做。真实楼梯和半砖浴已经覆盖同一对物品。
- 锯木桶的输入是 `wooden_barrel`，产物是 6 个橡木板加 2 个小木屑。冷却液倍数沿用现有锯床。楼梯和半砖浴覆盖两边 jar 都有的 11 种原版木头。

`Loader_Recipes_Woods` 里原先写着 “neither mod exists for 1.20.1” 的跳过原因已改。157、158、161、172 仍跳过，因为产物或输入是 Railcraft。174、179、182 不是 IE 配方，原因改成对应的原版语句。

IE 在场时实测 166 条机器行：154 条楼梯/半砖油浴，锯木桶按现有冷却液各出一条，织布机按 `rods/wooden` 展开，再加上焦炭压缩、焦炭粉碎和两条 Generifier。解析失败为 0。合成包 13 个文件。IE 不在场时机器行为和合成包都是 0。

旧存档没有用这份联动测过。
