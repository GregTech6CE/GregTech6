# Applied Energistics 2

Forge `15.4.11`，NeoForge `19.2.18`。两边 jar 都强制依赖 GuideME，开发期运行时一起带上：Forge `20.1.15`，NeoForge `21.1.19`。原文件 `Compat_Recipes_AppliedEnergistics.java`，Copyright (c) 2026 GregTech-6 Team，LGPL-3.0-or-later。SHA-256 `276755364e8efc53b3f282f28e4080d1aea29f8d10db98ca52b467a06809b87c`。

还在的机器行走冲压、压缩机、切割机、锤子和粉碎机。冲压三输入是这批新加的操作。有序合成也是这批新加的，Forge 和 NeoForge 的结果字段仍然分开。高压釜的第二种流体没有加，因为 15.4 / 19.2 已经没有水晶种子。

冲压：计算压板打赛特斯水晶和赛特斯宝石板，工程压板打八种钻石宝石板，逻辑压板打金板，硅压板打硅板。三种处理器是印刷电路、红石粉和印刷硅。硅没有宝石板，这一行不生成。压板复制（压板加铸块再产出同一块压板）会被现有配方折叠掉，因为输入和输出是同一件物品；这些行不生成。

切割：铁或钢、铜、锡、铅、银、镍、铝、黄铜、青铜、殷钢的锭切成 3 个 `ae2:cable_anchor`。润滑剂 10，水冷剂按 4 倍计为 40。

压缩：4 个赛特斯或福鲁伊克斯宝石，以及 8 个对应水晶，压成 `ae2:quartz_block` 或 `ae2:fluix_block`。下界石英在现代 jar 里就是 `minecraft:quartz`，压缩机里已经有对应配方，这一行不另加。

砸块：12 种石英和福鲁伊克斯方块、楼梯、台阶同时进锤子和粉碎机。楼梯仍按原版出 6 个宝石。天际石的整套石头加工和石粉研钵推后，因为 `blockDust` 不覆盖石头。

有序合成替换两张玻璃。石英玻璃是 `QGQ/GQG/QGQ`，产出 4 个 `ae2:quartz_glass`。Q 用 `forge:dusts/quartz` / `c:dusts/quartz`，这是下界石英粉，不含赛特斯粉。G 用 `forge:glass/colorless` / `c:glass_blocks/colorless`，比 AE 原来的任意玻璃窄。荧光玻璃是一行 `GQG`，荧光石粉加石英玻璃，产出 `ae2:quartz_vibrant_glass`。关掉的配方 id 是 `ae2:decorative/quartz_glass` 和 `ae2:decorative/quartz_vibrant_glass`。

不恢复 `ae_grinder`、DidYouKnow 假配方，也不恢复不消耗镜头的激光雕刻行。不把 AE 物品做成 GT 规范形态。

`MD.AE` / `IL.AE_` 共 191 处：51 处实现，2 处已有路径覆盖，7 处外部机器桥，131 处推后。同一份原文件里还有 132–189 行的矿典 `ae_grinder` 监听，不含 `MD.AE`，同样不恢复。明细在 [ae2.json](ae2.json)。

旧存档没有用这份联动测过。
