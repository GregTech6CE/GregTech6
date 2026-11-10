# 材料系统重构 01–03 实施摘要

## 核心目的与要求

本次重构要把分散在不同注册入口、生成表、兼容门面和机器消费者中的材料事实，收敛为**每类事实只有一个权威定义**，再通过统一校验、链接和只读查询交给各类消费者使用。目标不是单纯压缩代码，而是消除注册顺序、查询路径和双写造成的身份漂移与规则不一致。

核心要求：

- 材料身份、注册名、数值 ID、配方身份和持久化单位保持兼容；附属材料仍有明确所有权和宿主分配 ID。
- 定义安装必须经过统一校验；失败不能留下半完成索引，READY 后不可再修改目录，FAILED 后不能继续伪装为可用。
- 组成、工具、燃料、加工目标、分类 tag、替代组和形态规则分别有明确归属，不能由消费者自行重新解释。
- 旧 API 和生成符号只能转发到新权威，不能继续保存第二份可变事实；没有消费者的旧入口逐步删除。
- 扩展性优先于强制深度冻结：process value 由调用方持有，但注册后必须遵守不可变契约。
- 身份和主流程迁移不得顺带改变没有依据的游戏行为；GT6 对照、parity 决策和已知差异必须单独记录。

本文概括 01–03 已完成的工作、当前权威入口和兼容边界。详细验收见各阶段计划及[基线报告](../../integration/material-baseline.md)。

## 01：基线与正确性对照

- 建立三处独立材料快照：两次无附属初始化，以及一次通过真实 ServiceLoader 加载测试附属的初始化。
- 用稳定名称、ID、别名、属性、组成、燃料、加工目标、形态和引用生成排序投影，保留有语义的顺序。
- 新增材料引用审计，检查组成、分解、形态、UUM、加工目标等引用，并把有效、别名、历史停注和真正悬空分开。
- 建立固定的 GT6 来源依据和已知差异清单，包括 72U 截量、铜材双平台差异、铸模状态、原生工具成分等；这些现状没有被当成正确性通过。
- 新增共享 `MaterialBaselineTests`，覆盖存储块、72U 物品、复合工具头、损坏工具、容器、铸模、分解和附属材料，并提供真实资源重载批次。
- 清理形态表中误抽取的 `rmaterial` 局部变量键。没有改变材料注册顺序或生产行为。

## 02：统一注册与规范目录

- 内置材料改为由 `content.material.catalog` 的七类声明统一维护：元素、化合物、矿物、石材、木材、特殊材料和组。
- `MaterialDefinitions` 只负责收集、排序和提供稳定键索引；读取 `Materials.*` 等符号不会隐式启动注册。
- `GTMaterialRegistry.init()` 统一执行定义安装、引用校验、链接、附属回调、最终角色、辅助数据冻结和只读视图发布。
- 安装阶段明确分为 `DEFINITIONS`、`LINKS`、`FINALIZING`、`READY`、`FAILED`。身份、别名或附属声明失败后，公开材料入口持续拒绝访问。
- `MaterialRegistryState` 在修改名称、ID、别名和集合前完成冲突检查；sentinel、内置、组和附属材料使用明确安装模式。
- 附属同名一致声明继续合并；所有权、命名空间、tag 包含环、组环、未知材料和跨所有者冲突在 READY 前校验。
- 主材料定义收敛为身份、展示、物性、基础属性和来源；工具、组成、关系和处理规则进入 `MaterialInfoDefinition` 与 `MaterialInfoRegistry`。
- 新增只读 `MaterialCatalogView`、`MaterialView`、类型化 metadata、材料分类 tag、替代组、process 和定义期 form 查询。
- `postInit()` 只检查 READY，不再补写静态材料事实。材料翻译继续由现有语言文件维护，并增加英中键覆盖检查。
- API 1.4–1.6 依次加入定义视图、类型化 process、独立辅助索引和附属注册拆分。

## 03：属性、燃料与形态

- `GTMaterial` 不再保存组成、工具、燃料、矿石副产物、加工目标或倍率字段；这些事实只由每材料的辅助状态维护，并在链接和最终角色完成后发布。
- 燃料使用 `FuelRule`，加工目标使用 `MaterialTarget`，矿石规则使用 `OreProcessingRule` 等类型化值。
- 多组件燃料从同一组件集合计算热值和灰烬；同种灰烬合并，不同灰烬或超过单槽容量时拒绝燃烧，不再任取首组件。
- 按固定 GT6 来源修正多处木材燃料；补齐 Bentonite 组成；删除 Phosphor 无来源的 `MELTING`。
- 布尔 flag 使用与声明顺序无关的闭包；气体包含液体，离心／电解权限包含 `DECOMPOSABLE`，并拒绝 `FLAMMABLE` 与 `UNBURNABLE` 冲突。
- 定义期形态统一为 `MaterialFormRule`，区分 item、block、fluid、默认材料量和显式请求来源；实际注册结果由 `MaterialFormBinding` 发布。
- `MaterialForms` 改为兼容查询门面，从规范辅助快照读取；`MaterialFormContent` 在平台注册完成后发布真实 item、block、fluid 和显式统一物品绑定。
- 常见 form 的资源 tag 映射集中在 `MaterialEquivalence`，`CraftingMaterialForms` 不再维护第二份映射。
- `Any*` 消费者继续使用兼容符号，但成员和包含关系统一读取 `MaterialTagsView` 中的替代组。
- 补测覆盖 Paper 多组板、NetherQuartz 宝石形态、Iron 物品／方块重量和 Tantalum 强制宝石规则。
- 当前 API 1.7 增加详细 form 规则、实际绑定视图和分组包含查询。

## 已移除或停止作为权威

| 内容 | 当前状态 |
| --- | --- |
| `MaterialFactories` | 已删除；生产代码不再通过旧工厂隐式安装材料。 |
| `MaterialFacts` | 已删除；原扁平事实迁入类型化的 `MaterialInfoDefinition`。 |
| `MaterialCompositionData`、`MaterialDecompositionData`、`MaterialModTags`、`MaterialOreProcessing`、`MaterialRegistryExtras` | 已删除运行时写入；事实迁入规范目录。 |
| `MaterialFormCorrections` | 已删除；修正进入规范声明和最终派生规则。 |
| 各生成材料表和 `GT6Materials` | 保留兼容符号，只指向已安装的规范材料，不再定义第二份事实。 |
| `MaterialForms`、`MaterialProcessingTargets`、`MaterialGroups` | 保留查询门面；分别转发到辅助快照、process 快照和组视图。 |

## 已弃用入口

- `GTMaterial` 整体标记为内部兼容模型；工具、燃料、矿石、粉碎、熔炼、压粉目标和倍率等旧 getter／setter 保留签名，但转发到新的辅助状态。
- `GregTechMaterials.find()`、`find(int)`、`all()`、`invalid()` 和旧 `Material` 句柄弃用；新读取使用 `catalog()`、`MaterialView` 及各领域视图。
- `AddonMaterialContributor` 的 `forms()`、`compositions()`、`metadata()`、`materialTags()`、`materialGroups()`、`processes()` 弃用；新附属使用 `materialInfo(MaterialInfoTool.Registrar)`。
- `MaterialInfoTool.forAddon()` 以及接收旧 `GregTechMaterials.Material` 的注册重载弃用。
- `MaterialToolHelper`、`FuelMaterialHelper`、`CrushingMaterialHelper`、`SmeltingMaterialHelper`、`PulverizationMaterialHelper`、`OreProcessingMaterialHelper` 中接收 `GTMaterial` 的重载弃用；优先使用 `MaterialKey` 查询。
- `GTMaterialRegistry.createMaterial()` 弃用；内置材料走规范目录，附属材料走公开声明 SPI。

## 明确边界

- Process value 保留调用方扩展能力，不进行深度冻结；注册后必须由调用方保证不再修改。
- 资源 tag 桥接、完整来源／用途索引和处理图仍属于后续阶段。
- 客户端 tooltip、配方展示和实际交互仍需游戏内手工验收。
