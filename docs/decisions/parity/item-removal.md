# 物品移除

本端移除的物品、方块及其替代处理；移除重复 ID 不等于删除对应功能。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 植物及玻璃管形态 | 停注 `plantGtBerry`、`plantGtBlossom`、`plantGtFiber`、`plantGtTwig`、`plantGtWart`、`glasstube` 物品及对应配方。 | 本端无可达获取路径，减少无用注册。 |
| 材料灌木 | 删除 `bush_plant_gt_berry_*`；普通蓝莓、棉花等灌木保留，旧材料灌木 ID 会缺失。 | 随材料浆果形态一并取消。 |
| 赞助证书 | 删除 `supporter_certificate` 及关联实现。 | 原版赞助体系未沿用。 |
| 重复钥匙 | 删除 `key_*` 旧项及贴图，保留 `brass_key` 等 `*_key`。 | 同一把钥匙只保留一套身份。 |
| 重复主控 | 旧 `large_crucible_main/large_boiler_main/bedrock_drill_main/lightning_rod_main` 入口移除，使用对应原版型号。 | 去掉移植期重复主控。 |
