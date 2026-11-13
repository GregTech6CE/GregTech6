# 与原版的特性差异

仅列已核对的当前差异和历史注册清理；每项一行，不代表完整差异清单。按类别拆分如下：

- [世界生成](worldgen.md)：世界生成与矿物生成。
- [物品移除](item-removal.md)：无用物品、重复条目及其替代处理。
- [材料移除](material-removal.md)：停注的材料及其来源模组。
- [流体移除](fluid-removal.md)：重复注册、闲置兼容流体、原版水替代项、无产出流体及随材料停注的生成流体。
- [原版优先](prefer-origin.md)：以原版 MC 物品、流体或方块替代 GT 自有实现。
- [幻觉物品](illusion.md)：重构期凭空产生、原 GT6 不存在的注册项及其清理。
- [流体管道分布模式](fluid-distribution.md)：新增的 `BALANCED`/`SHARED` 配置与空闲调度开关。
- [流体物理参数](fluid-physics.md)：GT 流体运动缩放与坠落伤害的移植决策，含高温液体的已知简化。
- [工具交互行为](tool-interactions.md)：交互工具判定、plunger 抽取顺序、igniter 的 `PASS`、封面工具守卫。
- [药水系统移除](potion-removal.md)：移除整套药水系统的决策，含流体、配方表、饮用条目与下游联动；已执行，清理项并入[流体移除](fluid-removal.md)与[物品移除](item-removal.md)。
- [营养系统移除](nutrition-removal.md)：移除五项食物统计、其伤害类型与配置，保留辐射；停注 13 条闭环饮料流体与 `Tea_Ice`，保留瓶装物品。
- [硬币系统移除](coin-removal.md)：移除硬币前缀、物品、硬币堆与铸币模具、渲染、配置与地牢产出；地牢硬币堆位置改由锭堆承接，奖励有意加强。
- [板条箱重构](crate-rework.md)：装饰板条箱、战利品板条箱与材料板条箱的三种身份划分，含走廊生成与 4,018 个材料板条箱的删除。
- [其他差异](#其他差异)：交互、配置、新增内容与注册身份调整等无法归类的条目。

## 其他差异

以下条目不属于上述类别，保留在本页。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 多方块部件开窗 | 已成型结构中，归属唯一的有效部件右键可打开主方块已有 GUI，范围为点击部件中心 8 格；拆除或失效后关闭。原版不提供这一统一入口。 | 便于操作大型结构；主方块库存、权限、工具/覆盖板/流体交互保持原路径。 |
| 覆盖板放置 | 默认潜行右键；关闭 `covers.require_sneaking` 可恢复原版 GT6 的普通右键。 | 避免与机器右键交互冲突。 |
| 流体显示单位 | 默认用 mB/B 等单位；关闭 `display.fluid_in_mB` 改用 L/KL 等，存量不变。 | 统一现代模组的流体显示习惯。 |
| 量子存储器数字补零 | 客户端配置 `display.mass_storage_leading_zeroes` 默认关闭，与原版一样只显示实际位数；开启后数量补足六位，满仓仍显示 `100%`。 | 为 [#315](https://github.com/GregTech6CE/GregTech6/issues/315) 保留可选显示样式，不改变存量。 |
| 钼钙矿、钼铅矿分解 | 原版粉末离心分解改为电解机配方，使用模式 0 的选择器；分解成分和产量保持不变。矿石加工中的离心步骤不变。 | 按 [#318](https://github.com/GregTech6CE/GregTech6/issues/318) 将化合物分解归入电解。 |
| 储罐手动灌取 | 默认需要流体工具；关闭 `general.TanksRequireFluidTools` 才允许桶等容器直接右键灌取。 | 提供可选便捷交互，不影响管道自动化。 |
| 电锯自动收集 | 锯类工具（`SAW.canCollect=true`）破坏可采掘方块（如木壳机器）时掉落直接进背包，满则溢出；原版电锯 `canCollect=false` 不自动收集，仅扳手类收集。 | QoL：减少电锯伐木/拆木壳机器的拾取操作；有意保留的偏差。 |
| 电池盒直接插电池 | 持电池右键电池盒直接插入充电槽，空手右键开 GUI；原版右键只开 GUI，电池需在 GUI 槽放置。 | QoL：简化电池装填；有意保留的偏差。 |
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
| 石墨碳产率 | 石墨的熔炼/坩埚转换与热回收按 1 单位石墨产出 1 单位碳计算；原版为 0.5 U（石墨:碳 = 2:1）。 | 石墨作为碳源效率过低，按 [#290](https://github.com/GregTech6CE/GregTech6/issues/290) 提高碳产率。 |
| 瓶装饮料饮用时长 | 瓶装饮料按 `max(32, 食物值 × 8)` tick 结算，最低 32 tick（1.6 秒）；原版 GT6 的 `MultiItemRandom.getMaxItemUseDuration` 为 `max(食物值 × 8, 16)`，最低 16 tick。汞瓶食物值为 0，因此本端 32 tick、原版 16 tick。 | 采用原版 MC 药水的 1.6 秒作为下限，避免误触消耗；见 [#334](https://github.com/GregTech6CE/GregTech6/issues/334)。 |
| 72U 及以上大份量物品粉尘回收 | 原版 GT6 `OM.dust` 按单栈 64 上限截断超出部分；本端保留 `OM.dust`（另附未表示余量报告）服务单输出槽兼容路径，对支持多输出的分解、零件余料与外部矿石路径改用 `OM.dustAll` / `MaterialRecoveryRules.dustPlan()`，按 64 上限拆分为多组合法堆叠（如 72U 附魔金苹果拆为 64 + 8 堆叠）。 | 修复大份量材料在回收与分解时的无声截断，保证材料数量守恒并兼顾既有单输出槽约束。 |
