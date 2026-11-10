# 材料重构 05：统一回收计算与生成验收

日期：2026-11-10。适用分支：`master`，Stonecutter 节点 `1.20.1-forge` 与 `1.21.1-neoforge`。

## 结论

05 的生产侧范围已实施。普通物品粉碎、配方驱动的 Shredder、Anvil、胶囊、石材和热回收不再各自重建物品材料量；最终配方从有效 `ItemComposition` 生成，输入状态、数量、输出槽和不可表示余量分别有明确结果。本轮没有新增公开 API，也没有建立第二套回收框架。

08-D 的静态配方来源／用途索引仍属于 08 阶段。本阶段提供的是可被该索引投影的最终输入限制、数量、目标材料、输出角色和拒绝原因。

## 权威入口

| 职责 | 入口 |
| --- | --- |
| 单个材料的精确粉尘分组 | `MaterialRecoveryRules.dustPlan` |
| 完整成分的 Shredder 计划 | `MaterialRecoveryRules.planShredder` |
| 运行期输入拒绝原因 | `ItemMaterialRegistry.recoveryRejection` |
| 普通物品回收生成 | `VanillaRecoveryRecipes` |
| prefix 驱动 Shredder 生成 | `ShredderRecyclingRecipes` |
| Anvil 生成 | `AnvilShreddingRecipes`、`StoneAndToolSurvivalRecipes` |
| 热回收输入与 native 绑定 | `SharedThermalInputRows`、`SharedThermalRecyclingRows`、`SourceThermalNativeAdapter` |
| 胶囊权威成分与回收 | `CapsuleCellRecipes` |
| 动态行发布与重载 | `MachineRecipeOverlay` 及 owned-row 集合 |

## 行为裁决

- 72U 不再变成 64U。计划按 64 的单栈上限分组为 64U 和 8U 两个合法 dust 栈；更小的余量用 small、tiny、1/72 dust 继续表示。低于 1/72 dust 的不可表示部分不会静默丢弃。
- Shredder 计划逐组件转换粉碎目标并合并同目标材料；任一组件缺少有效目标时，整个候选带原因拒绝，不再保留“至少一个组件成功就发布”的半套输出。
- 配方输出超过机器槽位、计划数量溢出或最终 item 绑定缺失时，生产端拒绝该候选并计入诊断；不会生成超栈、负数量或静默截断配方。
- `RecoveryRejection` 分开报告 `NO_COMPOSITION`、`NO_COMPONENTS`、`NOT_BULK_RECOVERABLE`、`DAMAGED`、`STORED_CONTENTS` 和 `UNSUPPORTED_CONTAINER`。静态满额配方因此不会接受损坏物品，铸模空腔仍不属于盛装内容。
- Vanilla ore、raw metal item 和 raw metal block 使用 `VanillaUnificationDefinitions.OreInputKind` 明确分类；原矿物品与块分别进入自己的工艺，不靠 `raw_` 前缀字符串决定。
- 热回收候选直接读取有效物品成分，方块与石材不再从类对象重算第二份组分量。输入快照和最终查询在同一发布 generation 中构建。
- `rockGt` 的普通 Anvil 行只由 `StoneAndToolSurvivalRecipes` 声明；`AnvilShreddingRecipes` 不再重复提供同一候选。
- 胶囊的 `capcellcon` 物品不在通用 MaterialPrefix registry 中，因此由其现有容器规格声明 1/9U 的结构化有效成分；没有伪造新的 prefix 或公开 API。

## 检查结果

执行且通过：

- `:core:test --configure-on-demand --offline`：148 个测试通过，包含 72U 精确分组、输出槽拒绝、不可表示余量、原矿类别区分和已有材料基线。
- `compileBothPlatforms --configure-on-demand --offline`：Forge 1.20.1 与 NeoForge 1.21.1 生产源码编译通过。
- 两节点生产源码和共享 GameTest 源码编译通过。
- Forge 1.20.1 实际启动后注册 2775 条成分驱动 Vanilla 回收、超过 2 万条 prefix Shredder、1300 条 Anvil、28 条胶囊回收和 4200 条热回收输入行；NeoForge 1.21.1 对应为 2854、超过 2 万、1300、28 和 4232 条；没有新增 Shredder 重复行。最终候选遍历还显式排除 antimatter forms。
- 两个节点均真实资源重载两次：热回收 owned row 计数保持节点内稳定值，未累积旧行；有效物品成分 generation 同步更新。
- 72U 最终 Shredder 查询返回两个合法 Gold dust 栈；原料类别和状态拒绝原因可在运行时分别观察。
- Forge 1.20.1 实际执行 74 个 GameTest，全部通过；NeoForge 1.21.1 实际执行 79 个 GameTest，全部通过。

执行但失败或阻塞：

- `:core:spotlessCheck`／`:minecraft:spotlessCheck` 无法作为整仓通过项：检查显示 14 个与本轮无关的既有格式违规。本轮文件已用同一 Palantir 2.103.0 版本单独格式化，没有批量修改这些既有文件。
- 首次完整 Forge GameTest 因多目标耗尽槽位后的空计划调用失败；已增加无剩余槽位立即拒绝。后续运行还暴露胶囊没有权威成分、复合计划与旧 prefix 环不一致；均已修复。

尚未执行：

- 客户端 tooltip、JEI／EMI 展示和机器手动交互尚未执行。
- 08-D 的来源／用途快照与 08-E 的有界处理图尚未实现。

## 剩余边界

- 损坏物品目前由固定满额回收行明确拒绝，并保留原始物品；其按耐久缩放后的有效成分仍可用于查询和非批量消费者。要把“部分损坏物品按比例批量回收”变成可执行行，需要在现有 `RecipeMap` 查询解析中增加按实际 stack 缩放输出的动态规则，并先取得 GT6 行为依据；本轮没有用固定配方伪造该能力。
- 21 个源 prefix 在当前端口没有可生成的 item form，相关候选保持缺失诊断；不能用同名猜测物重建它们。

## 审查闭合（2026-11-10）

- Shredder 显式启动行进入静态 baseline；tag-owned 行作为独立 generation，在 `MachineRecipeOverlay` 中重建和清理。`MaterialUnitRecipe` 的动态索引与物理行通过同一个 rule 一起撤销，不会留下旧 tag 查询。
- Shredder duration 统一取自 `MaterialRecoveryRules.planShredder()`。方块形态仍通过实际的 block delegate 解析，但不再接受 `GTBlocks` 返回的非方块回退项。
- 通用 Shredder 行带来源标记；相同输入和条件下的不同输出冲突会终止发布，具体配方拥有输入时通用行明确让位。
- 新增运行时断言：tag-only 成分生成 Shredder 行，清空 tag generation 后该行不可查询；真实资源重载两次时 Shredder owned row 计数保持稳定。
- Forge 1.20.1 实际执行 75 个 GameTest，NeoForge 1.21.1 实际执行 80 个 GameTest，全部通过。

## 第二轮审查闭合（2026-11-11）

- 热回收所有权改由 `ThermalRecoveryOwnership` 单点判断，附属材料不再因非 core 被排除。
- 附属材料从已发布 smelting target 与唯一流体绑定生成热回收行；标准绑定 1U 输出 144 mB，小型形态按同一比例换算。
- `RecoveryInputProof` 统一运行时输入限制；`RecoveryRecipeConflicts` 统一 generic/specific 归属、去重和不同输出冲突。
- 运行时注册行数：Forge 4142 条热回收输入、NeoForge 4174 条；附加热回收行在两个节点均可查询。
- Forge 1.20.1 GameTest 77/77，NeoForge 1.21.1 GameTest 82/82；双平台生产与测试源码编译、core JUnit、ratchet Spotless 通过。
