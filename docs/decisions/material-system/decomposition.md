# 统一材料分解规则与计划

本文说明离心机（Centrifuge）与电解槽（Electrolyser）的材料化学分解链路。系统采用“类型化规则权限 → 纯数学计划 → 原生注册解析与重载适配”三层解耦结构，使配方权限、单位换算与原生流体注册变更互不干扰。

## `MaterialDecompositionRule` 与权限单一权威

- **唯一类型化声明**：分解权限由宿主机过程 `MaterialProcessKeys.DECOMPOSITION` 承载 `MaterialDecompositionRule`，一次声明涵盖三类事实：`routes`（允许的 `Route.CENTRIFUGE` / `Route.ELECTROLYSER` 集合）、`fluids`（每个相态 `LIQUID` / `GAS` 至多一条 `FluidBinding(phase, fluid, sourceUnits, millibuckets)`）及 `keepSolidInput`（原生流体绑定可用时是否仍保留固体粉末输入兜底）。
- **内置目录覆盖与完整性检查**：内置材料的分解规则集中定义在 `DecompositionMaterials`，由 `MaterialDefinitions` 在目录收集阶段附加到规范声明；`DecompositionMaterials.validateConsumed(...)` 按 `(id, name)` 反向核查全部来源行，孤儿行、重复键或与规范所有者路线不一致会在初始化阶段直接报错。因成分依赖停注材料而无法成立的条目明确保持无分解能力，不伪造替代组成。
- **废除双向补全**：旧 `CENTRIFUGE`、`ELECTROLYSER` 与 `DECOMPOSABLE` 在规范材料目录中不再作为权限源头，仅在 `MaterialInfoRegistry` 冻结时由类型化分解规则**单向回写**为兼容标记；第三方附属仅在缺失类型化 `MaterialDecompositionRule` 时才按旧快捷标记生成默认兼容规则，绝不与显式规则求并集。

## 纯计划计算与精确换算

`MaterialDecompositionPlanner` 脱离游戏注册表独立计算每条允许路线的可执行计划：

- **高精度整数换算（`UnitConversion`）**：基于 `BigInteger` 按声明自带的 `sourceUnits -> millibuckets` 换算比例执行换算，严格区分**输入向上取整**与**输出向下取整**，在收窄为 `int` 前逐项检验非负与溢出边界，不凭材料名称猜测 `144` 或 `1000` mB。
- **批量约简与电解催化剂保护**：依据规范化学组成的显式 `divider` 与各组分份量约简最小正整数批量；电解模式的模式选择器作为不消耗催化剂（`electrolyserCatalyst`）独立参与规划，不因数量公约数约简而被消去。
- **一跳粉化目标与工艺输出上限**：固体产出只按规范目标做一跳粉化转换（不递归追踪到目标的目标）；液体与气体作为相互独立的替代输入候选；完整保留产出排序以及各机器固有的输出槽数量上限，超出槽位上限截断的部分在 `Plan` 中记为 `droppedItemOutputs` / `droppedFluidOutputs` 以供诊断，而非隐式修改机器槽位容量。
- **失败状态分类**：`MaterialDecompositionPlanner.Status` 区分无权限（`NO_PERMISSION`）、无化学组成（`NO_COMPOSITION`）、非法 `divider`/数量、反物质排除（`ANTIMATTER`）、组分缺失（`MISSING_COMPONENT`）与关键输出绑定缺失（`MISSING_OUTPUT`）；关键产出不可表达时直接失败跳过，不发布缺失产物的残缺表面配方。

## 原生适配器与可重载生命周期

- **与流体内容范围解耦**：`DecompositionRecipeLoader` 读取冻结后的分解规则并运行纯计划，按当前游戏注册表实际解析固体 `dust`（及满足 `divider` 整除时的方块粉尘 `blockDust`）与流体；若某相态绑定未启用或未注册，只对该流体候选产生清晰绑定诊断，按 `keepSolidInput` 决定固体兜底存在性，不迫使改动核心组成或分解规则契约。
- **Overlay 所有权与重载稳定**：`DecompositionRecipeLoader` 集中登记 `OWNED` 配方集合并接入 `MachineRecipeOverlay`，在基线排除与每代重载时安全撤除旧行并重建，失败时恢复上一成功代次，确保连续资源重载不会产生残留或重复配方。
