# 与原版的特性差异

仅列已核对的当前差异和历史注册清理；每项一行，不代表完整差异清单。按类别拆分如下：

- [世界生成](worldgen.md)：世界生成与矿物生成。
- [物品移除](item-removal.md)：无用物品、重复条目及其替代处理。
- [材料移除](material-removal.md)：停注的材料及其来源模组。
- [流体移除](fluid-removal.md)：闲置兼容流体及其他无用途流体。
- [原版优先](prefer-origin.md)：以原版 MC 物品、流体或方块替代 GT 自有实现。
- [幻觉物品](illusion.md)：重构期凭空产生、原 GT6 不存在的注册项及其清理。
- [其他差异](#其他差异)：交互、配置、新增内容与注册身份调整等无法归类的条目。

## 其他差异

以下条目不属于上述类别，保留在本页。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 覆盖板放置 | 默认潜行右键；关闭 `covers.require_sneaking` 可恢复原版 GT6 的普通右键。 | 避免与机器右键交互冲突。 |
| 流体显示单位 | 默认用 mB/B 等单位；关闭 `display.fluid_in_mB` 改用 L/KL 等，存量不变。 | 统一现代模组的流体显示习惯。 |
| 储罐手动灌取 | 默认需要流体工具；关闭 `general.TanksRequireFluidTools` 才允许桶等容器直接右键灌取。 | 提供可选便捷交互，不影响管道自动化。 |
| 幽匿材料 | 新增独立 `Sculk`，物性和工具属性参照 Blaze，但身份分开。 | 支持现代 MC 幽匿物品。 |
| 现代石材 | 补充 `Tuff`、`MudBrick`、`RedNetherBrick`、`Purpur` 材料。 | 支持现代 MC 石材加工。 |
| 工具合并修复 | 可开启 `recipes.allow_tool_merging`；同工具、同头柄材料合并修复，默认关闭。 | 提供可选的现代 MC 修复方式。 |
| 尖刺身份 | 按材料与形态拆成 30 个注册 ID，不再靠物品附加状态区分。 | 适配现代注册、掉落和配方展示。 |
| 旁观者界面 | 旁观者可只读查看已接入的机器及储存界面，不能取放或操作。 | 适配现代 MC 旁观模式，方便检查。 |
| 染色材料身份 | 原版 GT6 的 `Black`…`White`（8250–8265）改名 `DyeBlack`…`DyeWhite`；移植期另建的 9500–9515 `Dye*` 重复材料停注。 | 同一颜色只保留一套身份，数值 ID 沿用原版染色材料。 |
| 作物与食品物品 | 删除 `barley`、`oats`、`rice`、`peanut`、`butter`、`salted_butter`、`tofu_bar`、`tofu_burger`、`vanilla_ice_cream`、`peanut_butter_ice_cream`、`pistachio_ice_cream`、`neapolitan_ice_cream`、`spumoni_vanilla_ice_cream` 食品条目。 | 对应作物材料停注，见[材料移除](material-removal.md)；本端未移植这些作物与食品加工。 |
| 瓶装流体物品 | 删除 `bottled_indigo_dye`、`peanut_butter`、`whale_oil`、`rice_water`、`rice_vinegar`。 | 对应材料与流体停注，流体登记见[流体移除](fluid-removal.md)。 |
| 反应堆燃料棒 | 删除 `reactor_rod_9229`、`reactor_rod_9239`、`reactor_rod_9249`（Yellorium、Blutonium、Ludicrite）。 | 对应材料停注，见[材料移除](material-removal.md)。 |
