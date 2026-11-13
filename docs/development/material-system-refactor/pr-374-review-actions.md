# PR #374 非文档审查意见整理

本页汇总 [PR #374](https://github.com/GregTech6CE/GregTech6/pull/374) 中与代码、行为、架构和发布决策有关的审查意见，合并重复问题，供后续分步处理。整理日期为 2026-11-12；审查针对的提交为 `a7c03f27759396b8d918ef7186719f0395373aec`。

**状态：审查意见已由用户确认属实。步骤三至七均已完成代码修复；2026-11-12 复核补正后通过双平台编译与全量 GameTest（Forge 92/92、NeoForge 97/97）。本页记录各步骤的验收边界，不据此宣称客户端交互、第三方模组联动或整个 PR 已验收。**

不收录 docs 目录定位、文档迁移、索引、模板、报告、计数、链接、清理表、parity 记录及文档披露要求。混合意见仅保留其中的实现问题与行为选择，例如保留 API 兼容性变化，排除“文档未披露”的要求。

## 来源与阅读方式

| 代号 | 来源 | 范围 |
| --- | --- | --- |
| C1 | [lombinaxmasson 的复核评论][C1] | 测试导入、分解输入、热回收比例、外部矿石转换 |
| C2 | [takanashisatou 的架构建议][C2] | 成分优先级、注册结构与同级覆盖策略 |
| R | [Tanimodori 的完整审阅][R] | 回收、成分、生命周期、行为差异、兼容性与设计分歧 |

读取范围包含 PR 讨论评论与正式 review；读取时没有行内审查评论。正式审阅末尾的结论及测试说明已归并到对应问题，不重复列项。

以下步骤是建议处理顺序。步骤一可先独立完成；步骤二处理明确提出的行为问题；步骤三至六处理结构与契约；步骤七集中裁决尚未确定的行为。涉及相同决策的项目通过编号引用，避免重复安排。

## 步骤一：恢复测试源码编译

- **1.1 补齐 JUnit 断言导入（C1 标为 P1）。** 审查指出 `GregTechAddonsTest` 使用了未导入的 `assertEquals`、`assertTrue`，`MaterialViewTest` 缺少 `assertEquals` 导入；所引用的 CI 有 24 个 `cannot find symbol`，后续构建与发布任务被跳过。先修复导入，再由实施任务执行对应检查。[C1][R]

## 步骤二：处理配方与数量问题

- **2.1 无流体绑定时保留固体分解入口（C1 标为 P2）。** 审查指出 `MaterialDecompositionPlanner` 在无 liquid／gas 绑定时推断 `1000 mB/U`，`DecompositionRecipeLoader` 随后认为流体入口已发布并跳过固体入口。示例为 `AluminiumAlloy`：45U 被转换为 45000 mB，原有 45 粉末／5 粉块入口消失。建议取消无声明绑定的通用流体兜底，按实际绑定决定流体入口，否则走固体入口。[C1][R]

- **2.2 附属材料热回收按声明比例与权限生成（C1 标为 P2）。** `SharedThermalRecyclingRows.planDeclared()` 被指出只使用绑定字段名，再从 `MoltenFluidHandler.ratio()` 取得 144 mB/U，忽略声明的 1000 mB/U；`BaselineThermalAddon` 可触发该差异，`MaterialBaselineTests` 还把 144 写成期望值。另缺少 `MELTING`／`BLACKLISTED_SMELTER` 检查。建议一并修正单位来源、准入判定和相应测试期望。[C1][R]

- **2.3 恢复外部 ore tag 的手工转换（C1 标为 P2）。** `CraftingMaterialForms.tagged()` 要求成分快照，但 `ExternalOreProcessingRules.COMPOSITION_FORMS` 不含 `ore`，使仅带 `forge:ores/tin`／`c:ores/tin` 等标签的外部矿石无法进入 `nativeAliases()`，矿石到原矿的配方生成与合成校验均受影响。建议补齐外部 ore 形态进入成分与转换链路的路径。[C1]

- **2.4 区分 Shredder 静态行与动态回退工时。** 审阅指出静态前缀行使用了 2/64/256，而所引用的 GT6 静态倍率为 256/16；2/64/256 属于动态回退公式。同时缺少 `STRETCHY`、`BOUNCY`、`MEAT`、`ROTTEN` 条件，Quartz 仍按名称识别。建议按两种入口分别确定规则，并与 3.2 的工时收口一起实施。[R]

- **2.5 统一缺失组件或输出时的处理策略。** 审阅多处指出：多组件回收缺少 pulver target、分解产物绑定失败或缺少输出时，会拒绝整条配方；其引用的 GT6 行为是跳过该组件并保留可生成部分。这里合并为一个策略项，分别覆盖 Shredder 与分解器；需要决定恢复部分输出还是保留整条拒绝，并统一相关分支。[R]

- **2.6 补齐 tag 派生形态的炉配方接替。** 审阅指出 tag 派生的 dust／gem／crushed 没有相应原版炉行，而 `hasMaterialProcessingRule()` 会删除外来炉行，可能出现删除旧配方后没有新配方接替的缺口。建议把删除条件与实际可发布的替代行关联起来。[R]

## 步骤三：收敛回收计算、发布和所有权

- **3.1 材料数量到粉尘栈只保留一套权威。** `MaterialRecoveryRules.dustPlan()` 按栈上限拆分并报告余量，`OM.dust()` 则选择单栈表达，不同消费者可能得到不同结果。建议明确权威计算入口，让另一个入口转发或退出，并明确单栈调用方如何处理多栈与余量。[R]

  **已修复：** `dustPlan()` 是完整输出权威，`dustSlotPlan()` 是同一算法的单栈视图；`OM.dust()` 使用单栈视图，`OM.dustAll()` 保留全部栈。分解、零件余料、流化床和外部矿石等可容纳多输出的入口改用完整列表，Dust Funnel 按单栈计划实际表示量扣减库存。

- **3.2 工时计算统一进入共享计划器。** `MaterialRecoveryRules.shredderWork()` 与 `CapsuleCellRecipes.registerRecycling()` 分别维护公式。建议在完成 2.4 的静态／动态规则裁决后，让各生产端读取同一工时规则，避免继续内联另一份公式。[R]

  **已修复：** 胶囊回收改为调用 `planShredderStatic()`，输出形态和时长均来自共享静态计划，不再内联 `(work + 8) / 9`。

- **3.3 每个配方族只有一个发布者与一份形态集合。** `CapsuleCellRecipes`、`StoneVariantRecipes`、`OreProcessingRecipeLoader`、`AnvilForgingRecipes` 被指出仍直接写入 Shredder／Anvil 行，`RecoveryRecipeConflicts` 仅做仲裁；热回收的 `RECYCLABLE` 与粉碎使用的 `RecyclablePrefixes` 又并行维护。建议把这些路径纳入按族归属的统一发布入口，并收敛形态集合，防止同一输入由不同路径生成不同数量。[R]

  **已修复：** 上述生产端及热回收、外部矿石、原生矿石回收路径统一经 `RecoveryRecipePublisher` 发布，并声明所属 family。Shredder 与热回收的形态准入集中到 `RecoveryFormCatalog`。

  **复核补正：** `SourceDustThermalRecipes`、`SourceThermalNativeAdapter` 和 `OreProcessingRecipeLoader` 的兼容热回收路径此前仍直接写 Melter／Smelter，`VanillaStoneMachineRecipes` 的石质 Smelter／Shredder 也仍直接写 map。现在这些路径分别以 explicit／generic ownership 经统一发布器提交，不再绕过 typed ownership、去重和冲突处理。

- **3.4 类型化回收行所有权，并建立输入索引。** `RecoveryRecipeConflicts.generic()` 依赖 `mSource` 的 `-generic` 后缀；`guardedCollision` 未命中后扫描全部配方，审阅认为两万余行规模下可能形成 O(N²) 发布成本。建议用结构化所有权替代命名约定，通过按输入建立的索引完成所有者与冲突查询。[R]

  **已修复：** 回收行在 `Recipe` 上携带 typed generic/explicit ownership；`RecoveryRecipePublisher` 不再解析来源字符串，并在 `findCollision()` 未覆盖 guarded rows 时只查询 `RecipeMap.mRecipeItemMap` 的候选输入桶。

## 步骤四：明确成分裁决与附属兼容契约

- **4.1 显式提交来源与优先级，并裁决同级冲突。** 两位审查者都要求减少隐式推断，但方案不同：C2 建议在 `ItemMaterialRegistry` 用显式整数优先级及默认值，按 `(key, priority)` 存储，同键同级后写覆盖并告警；R 建议显式受控来源类别，保留同层不同内容失败与覆盖链。共同问题是 `ItemCompositionSource.fromLegacy()` 依赖 `block composition`、`cr.rev`、`common form tags` 等文本反推类别，方块重量与 CR.REV 生产端仍提交旧字符串。先确定冲突契约，再迁移生产端、移除文本反推；两种同级处理方案互斥，不能合并为已达成共识的实现要求。[C2][R]

  **已修复：** 采用受控来源类别：同一优先级只有内容与回收授权完全一致时才合并，不同内容继续失败；显式覆盖仍要求命名现存声明并保留覆盖链。`fromLegacy()` 不再从自由文本猜测类别，旧调用统一标为 `LEGACY`。方块重量、CR.REV、机器构造、运输构件、木材及物品构造器自注册已改为提交结构化来源。

- **4.2 保留落选候选及原因。** 审阅指出 `ItemCompositionResolver.resolveItem()` 在首个有胜出者的层返回，`EFFECTIVE` 只保留胜出者，低层候选与淘汰原因不可查。建议提供每个物品的完整裁决诊断，包含胜出来源、落选来源及原因。[R]

  **已修复：** `ItemCompositionResolver.resolveDetailed()` 返回 `ItemCompositionResolution`，逐项保留 `SELECTED`、`REPLACED`、`LOWER_PRIORITY` 候选及原因；`ItemMaterialRegistry.compositionDiagnosis()` 发布与有效快照同代的诊断。tag generation 更新和撤销会同步替换或移除诊断。

- **4.3 处理旧注册与查询接口的兼容性变化。** `register(Item, ItemComposition)` 被指出从后写覆盖变为同声明 ID 冲突失败，可能使依赖重复注册的附属启动失败；其最终策略依赖 4.1。另有多组分 legacy 行的 `material()`／`amount()` 从首组件及其数量变为 `INVALID`／0，以及 `recoverable()` 被移除。需要分别决定保留旧行为、提供兼容入口或接受契约变化。[R]

  **已修复：** 弃用的 `register(Item, ItemComposition)` 为兼容入口：同一物品的重复旧调用保留最后写入语义，结构化提交仍执行严格冲突检查。legacy 多组件构造恢复首组件主材料及其数量；`recoverable()` 以弃用方法恢复并转发 `bulkRecoveryAllowed()`。

- **4.4 确认 API 版本与发布坐标的变更决策。** 审阅要求确认 API 1.7 → 1.8 的批准来源及发布坐标变化。本项仅保留版本与发布决定，不纳入提交说明或文档同步要求；审阅对授权的质疑不代表本次已核实存在未经授权的变更。[R]

  **已确定：** 保留累计功能对应的 API `1.8.0`；API 1.4—1.8 依次承载材料定义目录、解析处理键、材料信息工具、形态视图和 1.8 的类型化分解规则，因此不能只记为 1.7。发布坐标仍为 Maven `gregtech:gt6ce-api:1.8.0`，只递增版本，不改 groupId、artifactId 或 SDK 组成。

  验证：`:api:check`、`:core:test`、双平台生产与测试源码编译、ratchet Spotless、路由与注释检查通过；Forge 1.20.1 全量 GameTest 90/90，NeoForge 1.21.1 全量 GameTest 95/95。首轮 Forge 运行暴露 Stone 铸模主材料兼容回归，修复后最终全量运行通过。

## 步骤五：收敛分解声明与数据消费

- **5.1 明确 typed rule 与 flag 谁是权威，合并派生映射。** 审阅指出 catalog 写入 `CENTRIFUGE`／`ELECTROLYSER` flag，`MaterialDefinitions.attachDecomposition` 据此生成类型化规则，`MaterialInfoRegistry.syncDecompositionFlags` 又回写 flag；属性到分解路线的映射还在两处重复。建议确定唯一声明来源与单向派生关系，消除往返同步和重复默认语义。[R]

  **已修复：** 路线与相态统一由 `DecompositionMaterials` 声明，规范材料目录不再保存三个分解兼容 flag。`MaterialDefinitions` 一次生成 typed rule；冻结期只把 rule 单向镜像到 `GTMaterial`。旧附属 flag 仅在没有 typed rule 时作为兼容兜底。

- **5.2 检出未被消费的分解数据行。** `DecompositionMaterials.input()`／`thermal()` 对没有匹配活动声明的 `(id, name)` 返回 null，审阅认为缺少反向检查。建议让每条数据行都明确属于已消费或有意停用，避免孤儿行静默失效。[R]

  **已修复：** 规范目录装配后反向检查全部相态行和路线。重复、无所有者、typed rule 未保留或路线不一致均在初始化时失败；显式停用集合为空，停用行必须登记后才能跳过。

- **5.3 确定流体内容范围是否先于绑定定稿。** 审阅指出 [issue #369](https://github.com/GregTech6CE/GregTech6/issues/369) 关于熔融态及 EMI／坩埚／液化炉配方的取舍仍未决，而本次已确定 69 个材料的液体／气体绑定。需要决定该议题是否是绑定与默认规则定稿的前置，避免内容范围收缩后再次重做。[R]

  **已确定：** 不是前置。typed 分解规则保留固定来源的候选流体身份与单位换算；原生流体注册、EMI 展示、坩埚与熔化器配方属于运行时内容投影。issue #369 收缩范围时，对应绑定按既有诊断缺失；扩展展示时也不改分解规则本身。

  验证：完整 `:core:test` 171 个 JUnit、ratchet Spotless、路由与注释检查通过；Forge 1.20.1 全量 GameTest 90/90，NeoForge 1.21.1 全量 GameTest 95/95。

## 步骤六：明确快照生命周期、失败边界与分层

- **6.1 材料、成分与配方使用一致的发布版本。** 审阅指出 `MaterialInfoRegistry.freeze()` 一次性冻结，`MaterialViews.revision()` 恒为 1，成分 revision 与 `MachineRecipeOverlay` generation 分别递增，缺少统一发布点。需要决定在 08-D 前引入共同 stamp／发布服务，还是采用平台侧快照加 core 纯查询的设计，并明确材料定义不可重载是长期契约还是阶段限制。[R]

  **已修复：** 明确材料定义与显式成分是进程内静态契约，标签成分和配方按平台代次重新发布。新增 `MaterialPublication` 稳定 stamp；`MachineRecipeOverlay` 仅在整代成功后提交材料、成分 revision 与 overlay generation，失败代保留上一份成功戳。`MaterialCatalogView.revision()` 返回材料定义版本，配方查询版本同时观察该稳定戳。

  **复核补正：** `ItemMaterialRegistry.beginTagGeneration()` 现在返回代次检查点。overlay 在开始重建前捕获上一代标签候选、有效成分、诊断、readiness、tag generation 与 composition revision；失败恢复时整体回滚该检查点，再恢复 baseline 与配方 owner。失败代不会留下“旧 stamp 指向已改变成分状态”的不一致。

- **6.2 建立 server／level／会话边界。** `ItemMaterialRegistry` 的 `EFFECTIVE`、`COMPOSITION_REVISION`、`explicitClosed`、`tagInputReady` 被指出是全局静态状态，后两个标志只置位不复位。建议明确多服务器、多世界及切换存档时的状态归属、重置与快照获取方式；本项与 6.1 一起确定生命周期。[R]

  **已修复：** `beginTagGeneration()` 在每代 overlay 与服务端停止时递增 tag generation、撤销上一代标签候选并进入未就绪状态，完整发布后原子替换 `EFFECTIVE`。`compositionState()` 暴露 revision、代次、readiness 与显式关闭状态。`explicitClosed` 是有意保持的 JVM 生命周期契约，不在世界或资源重载时复位。

- **6.3 统一异常策略与失败影响范围。** 审阅指出成分同级冲突终止启动、分解失败仅告警跳过、回收冲突则可能触发整代 overlay 回滚，单个附属或数据包冲突会波及全部动态行。建议明确何时启动失败、跳过单行或整代回滚，并与 4.1 的冲突契约一致。[R]

  **已修复：** 策略固定为：显式成分歧义、静态内置配方冲突及初始化结构损坏终止启动；外部 tag／数据包／附属派生的单行回收冲突通过 `publishIsolated(...)` 告警并跳过；分解候选保留逐项拒绝诊断；非行级结构性异常整代回滚到 baseline。动态 Shredder、热回收、外部矿石和源矿石适配路径已接入单行隔离。

- **6.4 消除新增的 content／engine 双向知识依赖。** 审阅列出 `MaterialRecoveryRules` 依赖 `engine.material.ItemComposition`、`MaterialDecompositionPlanner` 依赖 `content.recipe.SourceDecompositionRules`；另有 `ItemMaterialRegistry.authorizesMaterialProcessingRule()` 直接依赖 `VanillaCompositionLoader.inheritsVanillaUnification()`，使 registry 知晓 MoldBlock、ClayMoldCatalog、LootChestBlock、MetalChestBlock。建议将内容事实留在内容层，以规则或声明输入通用引擎。[R]

  **已修复：** 单位换算移入 `engine.recipe.UnitConversion`；回收计算拆为引擎层 `ItemRecoveryPlanner` 与内容侧 `MaterialFacts` 适配。原版炉授权随 `ItemCompositionSource` 提交，模具与箱子的空内容语义经 `ItemMaterialRuntimeFacts` 注入。材料 registry 不再直接引用 `VanillaCompositionLoader`、`MoldBlock`、`ClayMoldCatalog`、`LootChestBlock` 或 `MetalChestBlock`。

- **6.5 清理无消费者 API。** 审阅将 `MaterialInfoRegistry.compositionDefinition` 列为无调用者的死 API，建议在兼容边界明确后处理其去留。[R]

  **已修复：** 删除 `MaterialInfoRegistry.compositionDefinition(...)`；组成读取保留公开 `MaterialInfoTool.composition(...)` 与内部当前快照入口，不保留重复的冻结访问 API。

  验证（2026-11-12）：
  - 执行且通过：`:core:test` 171 个 JUnit；`compileBothPlatforms`；Forge/NeoForge `compileTestJava`；`:minecraft:spotlessCheck`；Forge 1.20.1 全量 GameTest 90/90，NeoForge 1.21.1 全量 GameTest 95/95。GameTest 覆盖两次真实资源重载、标签代次重置、稳定发布戳和单行冲突隔离。
  - 执行但失败：`:core:spotlessCheck` 与节点级 `spotlessCheck` 仍报告本步骤未修改文件的导入顺序问题；未在本步骤顺手改写。
  - 尚未执行：客户端界面手工检查。本步骤没有新增客户端交互或网络格式。

## 步骤七：逐项裁决保留或恢复的行为

本步骤只列行为选择，不包含新增 parity 文档的要求。2.1、2.2、2.4、2.5、2.6 已收录的差异不再重复；下列项目均已按 GT6 行为修复。

| 项目 | 裁决与实现 |
| --- | --- |
| 损坏物品的固定输出回收 | **恢复比例回收。** Shredder 查询从损坏栈的剩余 `ItemComposition` 重新执行共享计划，再生成该次实际输出；静态 prefix 行保留 16/256 规则，动态行保留 2/64/256 规则。`RecipeMap` 为带运行时解析器的行提供执行期配方，guard 固定预期耐久并重新检查回收准入。[R] |
| 朱砂专用热回收与输入证明 | **移除特例。** 删除 9114／`Cinnabar` 的 Melter／Smelter 硬编码及 `captureCinnabarBlock`；`ThermalRecoveryOwnership` 中遗留的 9114／dust／crushed 前缀所有权也已删除，朱砂不再绕过 `MELTING`、`BLACKLISTED_SMELTER` 和常规相态绑定。`emitWholeDustMetal` 的 Ti／Fe 分数组仍按原审核边界保留，并有 GameTest 防止朱砂重新进入特殊所有者集合。[R] |
| 物品级回收授权 | **恢复原行为。** `bulkRecoveryAllowed` 仅作为 legacy 构造参数和兼容访问器输入保留，规范构造时统一归一为 `true`；回收准入改由有效成分、形态、具体工艺与栈状态决定，`canMelt` 继续独立。[R] |
| tag 形态覆盖范围 | **补齐。** common form tag 新增齿轮、小齿轮、环、螺栓、螺丝和箔；`storage_blocks` 与 `cables` 作为没有一对 `MaterialPrefix` 身份的 tag 组直接生成 9U 与 1/2U 成分候选。ore 转换缺口仍由 2.3 的专门路径处理。[R] |
| 胶囊专用回收行 | **保留为平台适配，不另立规则。** `fluid_cell_*` 是端口对 GT6 `capcellcon` 具体材质变体的表示，空壳按 1/9U 输出，时长由 `planShredderStatic()` 的 16/256 静态规则计算；盛装内容物的胶囊仍不回收。[R] |
| 铜块跨平台回收授权 | **统一。** `VanillaMaterialCorrections.copper()` 在两个平台都执行；两平台共同存在的 `minecraft:copper_block` 提交 9U Copper 修正成分，版本独有铜变体按物品存在性跳过。[R] |

验证（2026-11-12）：

- 执行且通过（复核补正）：`compileBothPlatforms`；两节点 `compileTestJava`；`:minecraft:spotlessCheck`；Forge 1.20.1 全量 GameTest 92/92，NeoForge 1.21.1 全量 GameTest 97/97，包含失败代整体回滚与朱砂普通所有权回归。
- 执行但失败：节点级 `spotlessCheck` 仍报告 `HopperClientModels`、`PipeWireClientModels`、`GTIconSetBlocks`、`GTRemainingIconBlocks` 等本轮未修改文件的既有导入顺序问题；共享 `:minecraft:spotlessCheck` 已通过。
- 执行但失败：`:core:spotlessJavaCheck` 仍报告本轮未修改的 `NutritionRemovalTest.java` 导入顺序；本轮 core 文件已按同一 Palantir 2.103.0 格式化，未改写该无关文件。
- 尚未执行：客户端交互与 JEI／EMI 展示手工检查；没有真实第三方模组提供新增 storage block／cable tag 成员，tag 映射由静态表和现有资源加载链路覆盖。

[C1]: https://github.com/GregTech6CE/GregTech6/pull/374#issuecomment-6797033166
[C2]: https://github.com/GregTech6CE/GregTech6/pull/374#issuecomment-6798836766
[R]: https://github.com/GregTech6CE/GregTech6/pull/374#pullrequestreview-5861956940
