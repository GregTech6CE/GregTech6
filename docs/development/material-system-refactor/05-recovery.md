# 05：统一回收计算与生成

本计划把粉碎、砧、热回收和坩埚所需的共同判定与数量计算收敛，保留各机器的真实工艺限制。状态：已实施本页生产侧范围；前置 [02](02-material-registration.md)、[03](03-properties-and-forms.md)、[04](04-item-composition.md) 已完成。验收与剩余边界见[05 验收记录](../../integration/material-refactor-05-recovery.md)。

生成与运行期能力确定后，向 [08-D](08-material-data-api.md#d来源与用途索引快照发布) 发布同源关系投影，包括输入状态限制、数量、主／副产物角色及覆盖程度；来源索引不从成分再次计算回收。unit 与动态规则优先投影符号族，不为绘图重新物化所有物理配方。

## 当前入口与目标

实际生产端包括 `VanillaRecoveryRecipes`、`ShredderRecyclingRecipes`、`AnvilShreddingRecipes`、`SharedThermalRecyclingRows`、`SharedThermalInputRows`、`SourceThermalNativeAdapter`、`OreProcessingRecipeLoader`、`ReviewedCinnabarThermalRecipes`、`ExternalOreProcessing`、`StoneVariantRecipes`、`CapsuleCellRecipes` 等。清理和重载还涉及 `MachineRecipeLifecycle`、`MachineRecipeOverlay`、`ExternalOreRecipeLifecycle`、`MoltenMetalUnitRecipes`。

目标数据流是“有效成分＋栈状态＋指定工艺 → 回收计划 → 原生配方或运行期处理”。计划包含输入基准量、完整输出量、耗时／能量、输入限制以及不能执行的原因；计算规则不持有 `ItemStack`，游戏层负责绑定物品、流体与机器槽。

## 实施步骤

### 1. 建立输入判定与结果模型

1. 使用 04 的统一成分和状态判断，分别查询是否有成分、具体回收工艺与形态是否接受、该热工艺是否可处理；不把物品级兼容字段或 `canMelt()` 当作所有回收机器的通行证。
2. 输入状态包括耐久、带内容物容器、填充桶及其他已核对的阻止条件；静态配方的匹配约束必须与其满额产物假设一致。
3. 输出分成可执行计划、明确无该工艺、因必要绑定缺失而无法解析。后者必须可定位，不再用静默 `null` 混同“设计上不能回收”。
4. 附属材料按声明能力和实际绑定参与，不以“非 core”直接否定；缺少相态或产物形态时说明原因，不能给未知材料套上原版默认。

交付：纯规则与少量原生绑定适配；没有机器配方注册副作用的计算可单独测试。

### 2. 解决数量表达与守恒

1. 移除 `MaterialRecoveryRules.pile()` 对 64 的无条件截断，先按材料量计算，再选择可表示的输出形态与批量。
2. 对 72U 等大数量，优先用已有大形态或等比例批处理；只有机器支持时才拆成多个合法栈。不能生成超过输出槽、最大栈或整型范围的非法配方。
3. 无法精确表示时明确记录量化余量、工艺允许损耗或拒绝原因。GT6 已规定的取整和损耗保留，不要求任意数量都无损，也不能把 64 截断伪装成既定损耗。
4. 所有组件都进入计算；加工目标转换、相态转换和灰烬／副产物有各自换算依据。使用有界且可检查的整数计算，验证溢出与上下取整。
5. `raw_*` 过滤改成明确的矿石／原矿类别规则，分别裁决 raw metal item 与 raw metal block；不能仅删字符串条件就把已有独立矿石工艺重复注册。

### 3. 逐工艺替换生产端

1. 先迁普通单材料物品的 Shredder，验证查找和输出；再迁复合物品与 Anvil，处理 `rockGt` 等重复归属。
2. 再迁 Smelter／Melter 的热回收，输入成分、相态和工艺限制共用计算。`phases.size() != 1` 等限制先核对固定 GT6 与槽位能力；按规定拒绝或选择产物，不自动放开全部多组件输入。
3. 坩埚共用成分解析与数量转换；保留其运行时温度、反应、燃烧和容器规则。它未必适合物化静态配方，不强行套入批量 recipe 表。
4. 特殊矿物、石材、单元／胶囊等生产端逐族声明归属；工艺确有差异的 handler 保留，成分与授权判定移除重复实现。
5. 每迁一族即关闭该族旧写入；测试可以对照旧计划，生产不能同时发布新旧两套等待 collision 丢弃。

### 4. 统一发布、冲突与重载

1. 为通用回收与显式专用配方建立归属表。相同语义的重复候选去重；不同输出的冲突报错；特定配方覆盖通用回收必须有显式规则与理由。
2. 冲突比较覆盖真实匹配语义：标签重叠、材料等价、催化剂、状态限制和 unit 输入，不能只比较配方 ID。
3. 与现有 `RecipeMap` 注册、overlay、baseline 恢复、unit 折叠接线；共享冲突策略不要求重写整个配方引擎。
4. 每个快照 revision 对某工艺只生成一批；重载移除该提供者拥有的旧行后发布新行，保留无关手写配方及数据包覆盖。
5. 避免中途暴露半套结果：在现有服务端重载生命周期允许的边界校验并替换；失败时沿用当前框架定义的失败处理，不额外引入吞异常降级。
6. 诊断区分候选数、明确不支持数、不可解析数、重复合并数、冲突数、物理行数、unit 吸收数。最终是否可用通过实际查询验证，不能用候选数减去几个计数代替查询覆盖。

## 独立验证

检查集：D、F、J、C、G；资源改变加 B；公开契约改变加 A。

| 样例 | 核心断言 |
| --- | --- |
| 普通铁物品 | 成分量、加工目标、最终回收量、时长／能量符合依据 |
| 附魔金苹果、末影箱 | 在其获准工艺中检查 72U 是否完整表达或明确拒绝，不静默变 64U |
| 原铜／金／铁块 | 与原矿物品分别查找，工艺归属唯一，不被 `raw_` 名称误排除 |
| 复合工具头 | 每个组件参与；不把整件物品按首材料回收 |
| 损坏物品、容器 | 损坏 Shredder 输入按剩余成分重建实际输出；装内容物的容器不被当空壳消耗 |
| 已凿铸模、压模机模具 | 形状元数据与库存分开；按已裁决工艺进入回收，材料量正确，不因纯形状状态误拒绝 |
| 多组件热输入 | 一种可热处理与一种被工艺限制拒绝的样例，原因和输出明确 |
| 附属材料 | 具备声明和绑定可参与；缺绑定返回可定位结果，不悄悄继承 core 数据 |
| 重复与重载 | `rockGt` 只有一个有效拥有者；重载两次无累积，移除标签成员会撤掉旧回收行为 |

JUnit 验证计算边界；GameTest 要通过最终 `RecipeMap`／运行期入口查找，并至少选一条批量机器配方执行消耗与产出。坩埚样例验证真实输入接纳和材料入池；带内容物拒绝时输入保持原样。

## 完成与拆分

建议按“输入／计划模型 → 数量表达 → Shredder／Anvil → 热回收／坩埚 → 特殊族 → 重载与统计”交付。每个小变更只迁一组明确归属，余下生产端列在迁移清单中。

完成条件：所有已迁移工艺只消费权威成分；运行期匹配与生成时假设一致；未知冲突为零；缺配方均能区分设计拒绝和数据缺失；全链路代表例在两个节点通过。06 的离心／电解分解仍保留独立的化学配比与工艺职责。

## 实施结果（2026-11-10）

- `MaterialRecoveryRules` 现在从有效 `ItemComposition` 生成 Shredder 计划；每个组件的粉碎目标和数量都进入计划，缺失目标、无法表示的数量、输出槽不足和整数溢出分别报告。72U 变为按 64 上限分组的合法栈，不再保留 64U 截断。
- 运行期回收准入拆成 `ItemMaterialRegistry.RecoveryRejection`：无成分、空成分、内容物容器和不支持容器可分别诊断。GT6 没有物品级授权门槛；损坏 Shredder 输入按剩余成分缩放，其他固定输出热行继续由各自 `RecoveryInputProof` 保护其满额假设。
- `ShredderRecyclingRecipes` 的物品形态直接遍历有效成分快照，复合工具头的每个有效组分进入计划；平台方块形态仍通过实际 block delegate 解析物品身份，但成分、授权和输出重新读取同一有效快照，不另算一部组分量。`VanillaRecoveryRecipes`、Anvil、胶囊和石材生产端同样以有效成分为输入，`rockGt` 的普通轮次由 `StoneAndToolSurvivalRecipes` 唯一拥有。
- 原矿类别由 `VanillaUnificationDefinitions.OreInputKind` 明确区分 ore、raw metal item 和 raw metal block；通用 Vanilla 回收不再依赖 path 的 `raw_` 字符串截断。
- 热回收候选直接读取有效物品快照，移除了方块与石材重算的重复成分来源；输入行在发布前先构建完整候选集，失败时清理 owned rows，已有重载 generation 继续负责整批替换。
- 本次没有新增公开 API。现有 `MaterialUnitRecipe`、`ItemComposition`、`RecipeMap` overlay／owned-row 生命周期和热回收纯规则继续承担发布与重载，只新增内部计划与拒绝类型。
- 08-D 的配方来源／用途投影仍需在 08 阶段按最终 `RecipeMap` 快照发布；本页只提供可投影的输入限制、数量、输出角色和诊断结果，不另建来源表。

## 审查闭合（2026-11-10）

- Shredder 的显式启动行留在静态 baseline；最终来源为 `FORM_TAG` 的行改由 `rebuildTagRows()`／`clearTagRows()` 管理，并接入 `MachineRecipeOverlay`。tag-only 成分加入或移除时会创建／撤回对应配方及动态索引。
- Shredder duration 直接使用 `MaterialRecoveryRules.planShredder()` 的结果，不再按主材料重算。实际方块形态继续由 block-prefix 解析，但过滤掉 `GTBlocks` 返回的非方块回退项。
- 通用 Shredder 行写入 `recovery:shredder-generic` 来源标记；同条件不同输出的通用行会报冲突，具体配方仍可显式拥有同一输入。
- 新增 GameTest 验证 tag-only Shredder 行加入、撤销及重载 owned row 稳定。Forge 1.20.1 GameTest 75/75，NeoForge 1.21.1 GameTest 80/80；双平台生产与测试源码编译通过。

## 第二轮审查闭合（2026-11-11）

- 新增 `ThermalRecoveryOwnership`，审核输入行、特殊审核行和通用热回收行共用一个所有权判断；附属材料由通用声明路径处理，不再因“非 core”被拒绝。
- 附属材料使用已发布的 `SMELTING` 目标、唯一流体绑定和标准 `SOURCE_UNIT` 比例生成 Melter／Smelter 行；1U 流体按绑定比例输出，缺失或歧义相态进入明确诊断。
- 删除分开的 thermal 输入证明，统一使用 `RecoveryInputProof`，覆盖损坏、内容物、催化剂、容器和空流体限制。
- 新增 `RecoveryRecipeConflicts`：通用行之间相同输出去重、不同输出失败；专用行可显式替换通用行；通用行遇到专用所有者时让位。Shredder、Vanilla、Anvil、热输入、热回收、胶囊和石材生产端共用该规则。
- Forge 1.20.1 GameTest 77/77、NeoForge 1.21.1 GameTest 82/82。新增测试覆盖附属热回收 144 mB/U、generic/specific 冲突和显式注册关闭。

## 第三轮审查闭合（2026-11-11）

- Shredder 的静态与动态材料规则使用运行时解析器；损坏栈在查询时从剩余成分重算输出和时长，guard 固定预期耐久并重新检查准入，完整物品生成时的输出假设不会扩用到缩量状态。
- Shredder 在发布前按注册 ID 和规范化输入检查静态拥有者，并在发布后复核经 `RecipeMap` 规范化得到的真实输入；同输出 owner 仅保留一个，同时移除对应动态索引。
- 审计探针在 Forge 和 NeoForge 上均只看到 `recovery:vanilla-generic` 一条铁锭通用回收行；tag-only 动态行、撤销和绑定刷新仍通过。
- Forge 1.20.1 GameTest 78/78、NeoForge 1.21.1 GameTest 83/83。

## PR #374 步骤三闭合（2026-11-12）

- `MaterialRecoveryRules.dustPlan()` 是完整粉尘输出与余量的权威计算；`dustSlotPlan()` 保留源 `OM.dust()` 的单栈选择规则，并单独报告该栈未表示的余量。`OM.dust()` 返回该兼容单栈，`OM.dustAll()` 返回完整栈列表。能够容纳多输出的分解、零件余料、流化床和外部矿石入口改用完整列表；Dust Funnel 按计划实际表示的 U 数扣减库存。
- `CapsuleCellRecipes.registerRecycling()` 改用 `planShredderStatic()`，输出形态和时长与静态 Shredder 生产端共用同一计划，不再内联胶囊专用时长公式。
- 朱砂 9114 的专用 Melter／Smelter 行和块级输入证明已删除，不再绕过常规 `MELTING` 与相态绑定；`bulkRecoveryAllowed` 只作为 legacy 兼容输入归一保留。
- common form tag 补齐齿轮、小齿轮、环、螺栓、螺丝和箔；`storage_blocks` 与 `cables` 在没有一对 `MaterialPrefix` 身份时直接生成 9U／1/2U 成分候选。
- `RecoveryRecipePublisher` 取代仅供冲突仲裁的 `RecoveryRecipeConflicts`，作为 Shredder、Anvil 和热回收生产端的统一发布入口。配方族、显式专用行和通用回退行按 typed ownership 区分，不再从 `mSource` 后缀推断。
- guarded row 的冲突查询复用 `RecipeMap.mRecipeItemMap` 输入索引，只检查候选物品可达的既有行，不再每次发布扫描整张配方表。
- `RecoveryFormCatalog` 集中维护 Shredder 生成子集、热回收完整 `RECYCLABLE` 集、dirty ore 排除和容器形态，Shredder 与热回收不再各自维护并行的准入表。
- Forge 1.20.1 GameTest 90/90、NeoForge 1.21.1 GameTest 95/95；新增用例验证 72U 的完整/单栈投影、胶囊行使用共享静态计划和 typed ownership。

## PR #374 复核补正（2026-11-12）

- `ReviewedCinnabarThermalRecipes`、`SourceThermalNativeAdapter`、`OreProcessingRecipeLoader` 与 `VanillaStoneMachineRecipes` 遗留的 Melter／Smelter／石质 Shredder 直写全部改经 `RecoveryRecipePublisher`。审核、native 和石材分支使用 explicit ownership，兼容回退使用 generic ownership，不再绕过 typed family 冲突处理。
- `ItemMaterialRegistry.beginTagGeneration()` 返回上一代检查点；`MachineRecipeOverlay` 失败恢复时回滚标签候选、有效成分、诊断、readiness、tag generation 与 composition revision。上一份 `MaterialPublication` stamp 保持不变。
- `ThermalRecoveryOwnership` 删除 9114／`Cinnabar` 及其 dust／crushed 前缀硬编码，只保留已审核的 Ti／Fe 粉尘分数组所有者。GameTest 同时覆盖失败代回滚与朱砂普通所有权。
- 验证通过：双平台生产与测试源码编译、`:minecraft:spotlessCheck`、Forge 1.20.1 GameTest 92/92、NeoForge 1.21.1 GameTest 97/97。
