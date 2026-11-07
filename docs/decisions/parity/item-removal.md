# 物品移除

本端移除的物品、方块及其替代处理；移除重复 ID 不等于删除对应功能。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 植物及玻璃管形态 | 停注 `plantGtBerry`、`plantGtBlossom`、`plantGtFiber`、`plantGtTwig`、`plantGtWart`、`glasstube` 物品及对应配方。 | 本端无可达获取路径，减少无用注册。 |
| 材料灌木 | 删除 `bush_plant_gt_berry_*`；普通蓝莓、棉花等灌木保留，旧材料灌木 ID 会缺失。 | 随材料浆果形态一并取消。 |
| 赞助证书 | 删除 `supporter_certificate` 及关联实现。 | 原版赞助体系未沿用。 |
| 重复钥匙 | 删除 `key_*` 旧项及贴图，保留 `brass_key` 等 `*_key`。 | 同一把钥匙只保留一套身份。 |
| 蜂蜜瓶 | 删除 `gt6ce:honey` 及其模型、贴图和语言键，所有产出改用 `minecraft:honey_bottle`，不保留旧注册。 | 使用现代 MC 蜂蜜瓶，详见[原版优先](prefer-origin.md)。 |
| 重复主控 | 旧 `large_crucible_main/large_boiler_main/bedrock_drill_main/lightning_rod_main` 入口移除，使用对应原版型号。 | 去掉移植期重复主控。 |
| 光谱露物品 | 删除 `spectral_dew` 瓶、`wooden_bucket_*_spectral_dew` 木桶（9 种）及对应模型、贴图、堆组和语言键。 | 对应流体 `spectral_dew` 停注，见[流体移除](fluid-removal.md)。 |
| 毒瓶与毒性木桶 | 删除 `bottle_of_poison` 与 9 个 `wooden_bucket_<band>_poison`（`copper`、`tin`、`zinc`、`lead`、`bismuth`、`brass`、`bronze`、`bismuth_bronze`、`gold`）及对应模型、贴图、堆组和语言键；三明治表中的毒瓶条目一并移除。 | 两者都绑定已停注的 `potion.poison.strong` 流体；详见[药水系统移除](potion-removal.md)。 |
| 防火板条箱 | 删除 `crate_fireproof` 方块、模型与药水沐浴配方。 | 唯一仍在用药水流体作沐浴介质的木制品；本端未注册 GT6 防火木料家族，改随 lootbox 移植。 |
| 材料板条箱 | 删除 12 个 `crateGt*` 前缀，共 4,018 个方块与 4,018 个物品，连同装箱/拆箱配方、模型、贴图、堆组与语言键；地牢储物间改用单一 `material_crate`。 | 材料板条箱不再按材料注册，见[板条箱重构](crate-rework.md)。 |
| Draconic Evolution、Avaritia、Fossils and Archeology、Botania 材料物品 | 删除 `Infinity`、`Draconium`、`DraconiumAwakened`、`DominicanAmber`、`ManaQuartz` 的存储块、书架金属、漏斗/队列漏斗、流体单元/管道/储罐、砧、长距离管道及全部材料形态物品。 | 对应材料停注，见[材料移除](material-removal.md)。 |
| 热锭与旧矿粉形态 | 删除 `ingotHot`、`dustImpure`、`dustPure`、`dustRefined` 前缀及其注册、配方入口、专用模型/贴图、堆组和语言键；不保留旧物品 ID。普通粉末与粉碎、纯净粉碎、离心粉碎矿石保留。 | 按 #314 清理本端无独立生产用途的形态；纯净、精炼粉末虽已不生成 GT 物品，残留的前缀和外部配方入口也一并移除。 |
