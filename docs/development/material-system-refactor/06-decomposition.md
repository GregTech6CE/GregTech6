# 06：统一材料分解

本计划让离心／电解分解的权限、化学组成、输入相态和数量换算来自同一个材料定义上下文。状态：已实施；前置为 [02](02-material-registration.md)、[03](03-properties-and-forms.md)、[04](04-item-composition.md)，并复用 [05](05-recovery.md) 的发布、冲突和诊断约定。实施与验收记录见[材料重构 06：统一分解验收](../../integration/material-refactor-06-decomposition.md)。

与 [08-D](08-material-data-api.md#d来源与用途索引快照发布) 共用有效分解结果：保留全部输入、产物、选择器角色、相态与单位。材料有化学组成不等于已存在可执行分解来源，图查询不能绕过本计划的工艺权限。

## 当前入口与目标

- `content.material.catalog.*` 中的 `MaterialDefinition.Composition`：规范化学组成与 divider。
- `content.material.catalog.DecompositionMaterials`：分解路线、各材料的液体／气体绑定及 `sourceUnits → mB` 换算的唯一内容声明；`MaterialDefinitions` 将其作为现有 `MaterialProcessKeys.DECOMPOSITION` 宿主 process 的 `MaterialDecompositionRule` 关联到规范声明。
- typed rule 是已知分解路线的唯一运行时权威；`DECOMPOSABLE`、`CENTRIFUGE`、`ELECTROLYSER` 只在安装完成后作为 `GTMaterial` 的兼容镜像，不再出现在规范材料的固有属性中。旧附属 feature／flag 仅在附属没有 typed rule 时生成一次兼容规则，不能与显式规则求并集。
- core `engine.recipe.UnitConversion`、`engine.recipe.MaterialDecompositionPlanner`：共享单位换算与纯计划计算，包括权限、组成、相态候选、批量、EUt、时长、产物顺序和槽位截尾。
- Minecraft `content.recipe.DecompositionNativeAdapter`、`DecompositionRecipeLoader`：解析已注册物品／流体身份，将计划投影并发布为离心／电解配方。

目标是一个材料分解声明配合共享计算器：声明决定允许的 map、组成与相态绑定；计算器产生有单位的计划；游戏适配器只解析已注册形态和流体并发布配方。物品回收不等于材料化学分解，两者共用底层事实，不共用一个不区分工艺的授权开关。

## 实施步骤

### 1. 合并权限与声明归属

1. 把每个活动材料的分解权限、输入相态和固体兜底条件挂到 02 的规范声明；组成与 divider 直接引用同一材料定义。
2. `DECOMPOSABLE` 与具体 map 权限不能互相猜测；成分里有金属、气体或某元素都不自动推导“可离心／电解”。
3. 每个绑定写清单位基准、流体身份与参考材料量；不能只存一个模糊的 mB 值，再由各加载器猜 144 或 1000。
4. 已停注的数据不进入运行分解集。液体／气体绑定是可独立尝试的替代输入，未注册项应逐项诊断后跳过；缺少可表示输入或任一必要输出绑定时不发布截断配方。保留明确无分解能力的材料，不把“无配方”都视为缺陷。
5. 与已有 parity 对齐：钼钙矿、钼铅矿的分解 map 已有项目决策，不能为追求 GT6 表面一致恢复成离心；矿石加工的离心步骤仍独立。

交付：同一材料的分解事实可一次读取；旧表暂存的部分有单一字段所有者，不重复决定权限。

### 2. 抽出纯计划计算

1. 复用并收敛 `UnitConversion` 中整数换算；其他兼容类只允许引用同一常量并转发算法，不再保存独立数字副本。
2. 以显式 divider 与组成量计算输入批量，保留必要的公因数约简；区分来源基准单位与当前 `GTValues.U`。
3. 明确每个绑定的输入向上取整、输出向下取整、不可表示和溢出处理；在最终缩窄到 int 前校验范围。
4. 固体产物的 pulver 目标、液体／气体／熔融态选择、温度条件和有语义的输出顺序逐项来自已核对规则；不能因统一模型把“一跳目标”递归化。
5. 槽位上限、输出顺序和截尾先按 GT6 与 parity 确认，再作为 map 能力参数传入。现有分解器按 map 数组长度截尾可能是原版工艺，不得与 05 的 64 栈截断混为同一种缺陷。
6. 计算器返回明确的可执行／无工艺／绑定缺失／范围不合法结果。表示不了的产物不能转成空输出后继续生成一条表面有效配方。

### 3. 迁移原生绑定与输入选择

1. 游戏层按声明解析流体和物品，保留实际注册身份，明确 dedicated liquid、gas、molten 与原版 Water 的绑定关系。
2. 保留液体与气体输入独立尝试的规则；接受流体后是否保留固体 fallback 由声明决定，Glowstone 作为明确案例。
3. 固体粉末与 blockDust 按实际形态存在性生成；只有 divider 能表示对应块量时才生成块输入，不强制为所有材料创建 dust。
4. 附属材料使用其声明与实际注册绑定；保留所有权和冲突校验。缺证据时明确拒绝该候选，不把附属存在变成全局禁用原版数据路径的理由。
5. 电解机选择器按现有规则作为存在性催化输入，放在约简正确的位置；不能被数量约简消掉或运行时消耗。

### 4. 切换生成与删除旧权威

1. `DecompositionRecipeLoader` 改成遍历规范定义、调用计划器、解析与发布；不再分别猜属性、相态和成分是否同步。
2. 候选冲突由 05 的发布约定处理，专用分解规则有显式归属，不依赖注册先后。
3. 逐批删除 `MaterialDecompositionData` 与 `SourceDecompositionData` 中已迁移的重复字段；可复用数学工具保留。旧名字不作为必须删除的理由，删除依据是事实或功能已经被替代。
4. 接入机器配方重载：分解行作为 overlay-owned rows 每代撤下并重建，连续重载不得累积；保持无关化学配方与矿石流程不变。

## 独立验证

检查集：D、F、J、C、G；公开声明改变加 A，资源改变加 B。

| 案例 | 必须验证的内容 |
| --- | --- |
| 纯固体化合物 | divider、输入批量、固体输出和 map 权限 |
| 液体／气体输入 | 144 与 1000 等声明转换率独立生效，不按材料名猜量 |
| Glowstone | 液／气输入及保留固体 fallback 符合权威规则 |
| 钼钙矿／钼铅矿 | 电解模式 0、产物量、存在性选择器；矿石离心链不被删除 |
| 一跳 pulver 目标 | 产物只做声明的一次目标转换，不追到目标的目标 |
| 边界数量 | 1/72U、非整除、大于 int 的材料量、long 溢出均有明确定义 |
| 输出槽上限 | 顺序与经核对的截尾／拒绝规则正确，不能为“守恒”擅自添加槽 |
| 禁止与缺失 | 无权限材料无配方；未知组件、错误绑定逐项定位；缺必要绑定不生成截断配方 |
| 附属／重载 | 授权附属的声明可用；连续重载无重复、移除权限后旧行不可查 |

JUnit 验证数学与权限；GameTest 从最终 map 查询并执行至少一个离心和一个电解样例，检查输入消耗、催化剂不消耗、流体量、时长与 EUt。把当前生成器输出直接另存作 expected 不算独立正确性证据。

## 完成与拆分

建议按“权限声明与负例 → 纯计算器 → 固体绑定 → 流体／气体与附属 → 切换、重载和旧表退出”交付。

完成条件：每个分解候选能说明权限、组成、相态和单位依据；活动定义没有静默悬空引用；两个节点代表例通过；现有明确 parity 差异保留。机器的化学处理能力和槽位未获依据前不在重构中扩大。

## 实施结果（2026-11-11）

- API 1.8 新增唯一类型化 `MaterialProcessKeys.DECOMPOSITION` 和 `MaterialDecompositionRule`。规则同时声明离心／电解路线、液体／气体注册身份、`sourceUnits → mB` 换算以及接受流体后是否保留固体输入；没有新增平行材料注册表。附属只能为自己拥有的材料提交该宿主机 process。旧 `MaterialDeclaration` 离心／电解 shortcut 已弃用，只在没有 typed rule 时转换默认规则。
- 规范目录中的 263 个材料现在发布分解上下文：250 个带至少一个可执行路线，13 个仅提供组件相态；69 个材料带显式流体相态。路线权限仍镜像为兼容的 `CENTRIFUGE`、`ELECTROLYSER`、`DECOMPOSABLE` 属性。
- 旧 `SourceDecompositionData`、`SourceDecompositionNativeFacts` 已由 `DecompositionMaterials` 声明和 `DecompositionNativeAdapter` 取代。`MaterialDecompositionPlanner` 统一计算约简后的输入数量、EUt、时长、相态选择、按声明的一跳粉化目标、输出顺序、槽位截尾及失败状态；截尾和绑定失败均产生明确诊断。
- 补齐 22 个已有路线但缺少组成的活动材料；`Ironwood`、`SkyStone`、`VibrantAlloy` 因组成依赖已停注材料，保留为明确无分解能力，不伪造替代组成。
- Forge 1.20.1 以真实 `RecipeProcessor` 执行附属离心配方并核对消耗、时长、EUt 与两类输出，78/78 通过；NeoForge 1.21.1 为 83/83。详见验收记录。

### 审查闭合（2026-11-11）

- typed decomposition rule 现在是路线唯一权威；旧 route flag／feature 只在其缺失时合成默认规则，显式规则的 route 集合会完整覆盖并回写兼容 flag，不再求并集。
- `DecompositionRecipeLoader` 记录并暴露 overlay-owned rows，接入 `MachineRecipeOverlay` 的 baseline 排除、逐代重建和失败恢复；真实资源重载增加 owned 数量与电解配方可查询断言。
- 材料单位以 `GTValues.U` 为唯一常量，换算统一到引擎层 `UnitConversion`；分解表与旧来源表共享的液体／气体 binding 改为读取同一数据。
- 删除没有消费者的等离子体 API 与表字段；分解表的热学温度字段重新用于拒绝与规范材料物性不一致的数据。
- 验证：API 108 个 JUnit、core 159 个 JUnit、API 契约、双平台生产及测试源码编译通过；Forge 78/78、NeoForge 83/83 GameTest 通过。

## PR #374 步骤五闭合（2026-11-12）

- 路线、相态和换算声明统一由 `DecompositionMaterials` 持有；规范材料目录不再自行声明 `CENTRIFUGE`、`ELECTROLYSER` 或 `DECOMPOSABLE`。`MaterialDefinitions` 一次生成 typed rule，`MaterialInfoRegistry` 冻结时只把 rule 单向投影为兼容 flag，不再从 flag 反向补全已有 typed rule。
- `DecompositionMaterials.validateConsumed(...)` 在规范目录装配后按 `(id, name)` 反查每条 `INPUTS`／`AMBIENT` 行和每条路线。重复行、没有规范材料所有者、存在所有者但 typed rule 未保留该行或路线不一致都会终止初始化；确需停用的行必须列入显式停用集合，不能静默留下孤儿数据。
- 流体内容范围不是 typed 分解声明的定稿前置。`MaterialDecompositionRule` 保存的是固定来源中的候选原生流体身份与单位换算；issue #369 最终若收缩熔融流体，只会让对应运行时绑定缺失并产生既有诊断，不要求重写路线、组成或分解规则。反过来，若决定补配熔化器／EMI，也只改变原生流体与配方投影。两类决定通过 `DecompositionNativeAdapter` 的解析边界解耦。
- 复验通过：完整 `:core:test` 171 个 JUnit、ratchet Spotless、路由闭合与注释检查；Forge 1.20.1 GameTest 90/90，NeoForge 1.21.1 GameTest 95/95，两个节点均完成真实启动、分解发布和材料重载。

## PR #374 步骤六闭合（2026-11-12）

- 材料定义与显式物品成分是进程内一次性定义，不随世界切换或资源重载再次注册；`MaterialCatalogView.revision()` 返回该材料定义的发布版本。标签成分、有效成分快照和机器配方是平台侧可重新发布的内容；`MaterialPublication` 仅在整代配方发布成功后提交同时包含材料版本、成分 revision 与 overlay generation 的稳定 stamp，失败代不会覆盖上一份成功 stamp。
- 标签成分状态按发布代次归属：每次 overlay 开始时先递增代次、撤销上一代的标签候选并进入 `TAG_INPUT_NOT_READY`，完整替换发布后才重新变为 ready。服务端停止通过同一入口清理；`explicitClosed` 不复位，因为显式定义在 JVM 生命周期内不可重载。
- 异常策略按影响范围固定：显式成分同级歧义、静态内置配方所有权冲突和初始化结构损坏终止启动；外部 tag、数据包或附属派生的单条回收候选冲突由 `RecoveryRecipePublisher.publishIsolated(...)` 记录并跳过该行；分解候选缺绑定或不可表示时保留既有逐项诊断；overlay 生产器出现非行级结构性异常时整代回滚并恢复 baseline。
- 纯计划不再反向读取内容配方类：`MaterialDecompositionPlanner` 使用引擎层 `UnitConversion`；回收计划由 `ItemRecoveryPlanner` 接收 `MaterialFacts`。原版炉处理授权随 `ItemCompositionSource` 提交，方块特定“空内容”判定经 `ItemMaterialRuntimeFacts` 由内容层注入，材料 registry 不再直接识别模具、箱子或原版成分 JSON 类。
- 删除无消费者的 `MaterialInfoRegistry.compositionDefinition(...)`。需要组成时继续使用公开 `MaterialInfoTool.composition(...)` 或内部当前快照查询，不保留第二份冻结读取入口。
