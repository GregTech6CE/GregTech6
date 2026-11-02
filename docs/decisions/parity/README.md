# 与原版的特性差异

仅列已核对的当前差异和历史注册清理；每项一行，不代表完整差异清单。按类别拆分如下：

- [世界生成](worldgen.md)：世界生成与矿物生成。
- [物品移除](item-removal.md)：无用物品、重复条目及其替代处理。
- [材料移除](material-removal.md)：停注的材料及其来源模组。
- [流体移除](fluid-removal.md)：重复注册、闲置兼容流体及随材料停注的生成流体。
- [原版优先](prefer-origin.md)：以原版 MC 物品、流体或方块替代 GT 自有实现。
- [幻觉物品](illusion.md)：重构期凭空产生、原 GT6 不存在的注册项及其清理。
- [流体管道分布模式](fluid-distribution.md)：新增的 `BALANCED`/`SHARED` 配置与空闲调度开关。
- [药水系统移除](potion-removal.md)：移除整套药水系统的决策，含流体、配方表、饮用条目与下游联动；方案已定、**尚未执行**。
- [其他差异](#其他差异)：交互、配置、新增内容与注册身份调整等无法归类的条目。

## 其他差异

以下条目不属于上述类别，保留在本页。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 多方块部件开窗 | 已成型结构中，归属唯一的有效部件右键可打开主方块已有 GUI，范围为点击部件中心 8 格；拆除或失效后关闭。原版不提供这一统一入口。 | 便于操作大型结构；主方块库存、权限、工具/覆盖板/流体交互保持原路径。 |
| 覆盖板放置 | 默认潜行右键；关闭 `covers.require_sneaking` 可恢复原版 GT6 的普通右键。 | 避免与机器右键交互冲突。 |
| 流体显示单位 | 默认用 mB/B 等单位；关闭 `display.fluid_in_mB` 改用 L/KL 等，存量不变。 | 统一现代模组的流体显示习惯。 |
| 储罐手动灌取 | 默认需要流体工具；关闭 `general.TanksRequireFluidTools` 才允许桶等容器直接右键灌取。 | 提供可选便捷交互，不影响管道自动化。 |
| 幽匿材料 | 新增独立 `Sculk`，物性和工具属性参照 Blaze，但身份分开。 | 支持现代 MC 幽匿物品。 |
| 现代石材 | 补充 `Tuff`、`MudBrick`、`RedNetherBrick`、`Purpur` 材料。 | 支持现代 MC 石材加工。 |
| 工具合并修复 | 可开启 `recipes.allow_tool_merging`；同工具、同头柄材料合并修复，默认关闭。 | 提供可选的现代 MC 修复方式。 |
| 合成工具身份 | GT 的 `gt6ce:tool_shaped` 与工具配方只接受 GT 自制工具并按 GT 磨损规则结算；`forge:tools/*` 中的其他模组工具不再匹配 GT 合成配方，机器交互仍按原语义判断。 | 现代原版合成会直接消耗没有合成剩余物的外部工具，外部扳手等会被吞掉或变成免磨损工具。 |
| 尖刺身份 | 按材料与形态拆成 30 个注册 ID，不再靠物品附加状态区分。 | 适配现代注册、掉落和配方展示。 |
| 旁观者界面 | 旁观者可只读查看已接入的机器及储存界面，不能取放或操作。 | 适配现代 MC 旁观模式，方便检查。 |
| 染色材料身份 | 原版 GT6 的 `Black`…`White`（8250–8265）改名 `DyeBlack`…`DyeWhite`；移植期另建的 9500–9515 `Dye*` 重复材料停注。 | 同一颜色只保留一套身份，数值 ID 沿用原版染色材料。 |
| 作物与食品物品 | 删除 `barley`、`oats`、`rice`、`peanut`、`butter`、`salted_butter`、`tofu_bar`、`tofu_burger`、`vanilla_ice_cream`、`peanut_butter_ice_cream`、`pistachio_ice_cream`、`neapolitan_ice_cream`、`spumoni_vanilla_ice_cream` 食品条目。 | 对应作物材料停注，见[材料移除](material-removal.md)；本端未移植这些作物与食品加工。 |
| 瓶装流体物品 | 删除 `bottled_indigo_dye`、`peanut_butter`、`whale_oil`、`rice_water`、`rice_vinegar`、`spectral_dew`。 | 对应材料与流体停注，流体登记见[流体移除](fluid-removal.md)。 |
| 反应堆燃料棒 | 删除 `reactor_rod_9229`、`reactor_rod_9239`、`reactor_rod_9249` 与乏燃料 `reactor_rod_9329`、`reactor_rod_9339`、`reactor_rod_9349`（Yellorium、Blutonium、Ludicrite）。 | 对应材料停注，见[材料移除](material-removal.md)。 |
| 储物桶封存读取 | 贴胶布后统一不向 `IItemHandler`/AE 存储总线暴露内容，拆封后恢复；原版存在“先读后贴仍可见但不可提取、先贴后读不可见”的时序差异。 | 统一为贴胶布时读不到，避免“可见但不可提取”的终态；内容仍保存在方块内。 |
| 储物桶自动输出到空气 | 下方无容器且无碰撞时掉落一组物品；岩浆、火不特殊处理，抗火物品可在岩浆中存活。原版 `ST.put` 对岩浆、火和世界底部无效方块直接销毁。 | 保留现代物品实体的燃烧与拾取行为；铁轨因有碰撞形状实际不会掉落。 |
