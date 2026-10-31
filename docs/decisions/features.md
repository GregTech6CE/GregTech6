# 与原版的特性差异

仅列已核对的当前差异和历史注册清理；每项一行，不代表完整差异清单。

| 内容 | 与原版不同的处理 | 简要原因 |
| --- | --- | --- |
| 植物及玻璃管形态 | 停注 `plantGtBerry`、`plantGtBlossom`、`plantGtFiber`、`plantGtTwig`、`plantGtWart`、`glasstube` 物品及对应配方。 | 本端无可达获取路径，减少无用注册。 |
| 材料灌木 | 删除 `bush_plant_gt_berry_*`；普通蓝莓、棉花等灌木保留，旧材料灌木 ID 会缺失。 | 随材料浆果形态一并取消。 |
| 木梁 | `beam_*` 改用去皮原木，配方随之替换；NeoForge 为已列木种保留旧 ID 别名。 | 适配现代 MC 木材体系。 |
| 覆盖板放置 | 默认潜行右键；关闭 `covers.require_sneaking` 可恢复原版 GT6 的普通右键。 | 避免与机器右键交互冲突。 |
| 世界水装桶 | 河水、海水、沼泽水用铁桶收集后统一得到原版水桶。 | 接入原版装桶交互。 |
| 世界水兼容 | GT 世界水加入 `minecraft:water`，并按原版水参与相关判定。 | 兼容水生生物及其他模组。 |
| 流体显示单位 | 默认用 mB/B 等单位；关闭 `display.fluid_in_mB` 改用 L/KL 等，存量不变。 | 统一现代模组的流体显示习惯。 |
| 储罐手动灌取 | 默认需要流体工具；关闭 `general.TanksRequireFluidTools` 才允许桶等容器直接右键灌取。 | 提供可选便捷交互，不影响管道自动化。 |
| 原版 MC 等价物 | 部分 GT 锭、宝石、储存块等优先用 MC 物品；重复项隐藏但保留注册。 | 减少重复展示，保留旧物品可用性。 |
| 羊肉物品 | 删除 `gt6ce:mutton`、`gt6ce:grilled_mutton`，改用 MC 生熟羊肉并接回加工链。 | 现代 MC 已有羊肉，不再重复注册。 |
| 幽匿材料 | 新增独立 `Sculk`，物性和工具属性参照 Blaze，但身份分开。 | 支持现代 MC 幽匿物品。 |
| 现代石材 | 补充 `Tuff`、`MudBrick`、`RedNetherBrick`、`Purpur` 材料。 | 支持现代 MC 石材加工。 |
| 工具合并修复 | 可开启 `recipes.allow_tool_merging`；同工具、同头柄材料合并修复，默认关闭。 | 提供可选的现代 MC 修复方式。 |
| 下界矿物生成 | 下界金矿、远古残骸独立开关，默认开启，不随禁用普通 MC 矿石一起关闭。 | 保留现代 MC 下界资源。 |
| 装饰石生成 | 默认禁用 MC 花岗岩、闪长岩、安山岩矿团，可用 `DisableVanillaDecorativeStone` 调整。 | 避免与 GT 地层生成重复。 |
| 尖刺身份 | 按材料与形态拆成 30 个注册 ID，不再靠物品附加状态区分。 | 适配现代注册、掉落和配方展示。 |
| 旁观者界面 | 旁观者可只读查看已接入的机器及储存界面，不能取放或操作。 | 适配现代 MC 旁观模式，方便检查。 |
| 赞助证书 | 删除 `supporter_certificate` 及关联实现。 | 原版赞助体系未沿用。 |

以下记录本端历史注册清理；移除重复 ID 不等于删除对应功能。

| 内容 | 清理后的处理 | 简要原因 |
| --- | --- | --- |
| 占位材料 | 撤销 `Wool`、`PlantMatter`、`Soil`、`Feather`、`AnimalShell`、`DyeMatter`、`MagicMatter`、`Dripstone` 占位注册。 | 缺少原版依据，不再凭物品名称编造材质。 |
| 同名材料 | 旧占位声明删除后，`Sculk/Tuff` 已单独注册，`Calcite/Prismarine/Deepslate` 仍有原材料。 | 不把占位清理误记成材料删除。 |
| 闲置兼容酒类 | 停注未使用的 Binnie 啤酒、利口酒、白兰地等；在用威士忌、葡萄酒保留。 | 无配方用途的兼容占位不再注册。 |
| 其他闲置流体 | 停注 `ic2steam`、`ic2superheatedsteam`、`myst.ink.black`、`ooze`、`purelava`、`endergoo`。 | 无实际用途的兼容占位。 |
| 重复钥匙 | 删除 `key_*` 旧项及贴图，保留 `brass_key` 等 `*_key`。 | 同一把钥匙只保留一套身份。 |
| 占位电池 | 移除 `battery_lv/mv/hv/ev/iv`、`battery_eu_*` 旧入口，使用按化学体系和电压分型的电池。 | 恢复原版电池分类，去掉占位型号。 |
| 虚构扩展器等级 | 去掉 `extender_basic/advanced/elite/wireless`，使用 `inventory_tank_extender` 等实际类型。 | 原版按功能分类，不按这四档分级。 |
| 多余 ZPM 档位 | 去掉 `zpm_discharger_elite`，保留原版 QU/EU 两种放电机。 | 原版没有第三档。 |
| 重复主控 | 旧 `large_crucible_main/large_boiler_main/bedrock_drill_main/lightning_rod_main` 入口移除，使用对应原版型号。 | 去掉移植期重复主控。 |
