# 当前源码导航与命名约定

此文描述 `main` 的 Stonecutter 结构。原版行为差异见 [原版对齐审计](ORIGINAL_ALIGNMENT_AUDIT.md)；目录整理不代表全部功能已经对齐。

禁刷怪塔17996的纯数值／唯一布局在core的`VonDaGraaggRules`；共享原生BE负责验证与供能，`VonDaGraaggSpawnInhibitor`负责刷怪事件，`ControllerStructureLayouts`的本机预览委托core而不再重复63格。实际消费者及可读性边界见[专题](VON_DA_GRAAGG_AUDIT.md)。

基岩钻机17999真实方块是`OriginalBedrockDrillControllerBlock`，运行在共享`BedrockDrillControllerBlockEntity`，core的`AdvancedControllerRules`与`DirectMachinePowerRules`分别给出角色／常数和软容量注入计划。Neo能力监听在`BasicMachineRegistries`，原生罐恢复在版本专有`NativeMachineTankData`。同目录另一HorizontalDirectional类不能代替实际注册判断，见[钻机专题](BEDROCK_DRILL_AUDIT.md)。

物流核心17997：core的`LogisticsCoreLayout`给结构／计数经济，`LogisticsCorePowerRules`委托共享软容量计划，`LogisticsRoutingRules`给路由顺序；共享BE负责世界验证、调度／显示／保存，`LogisticsRoutingPass`连接原生物品／流体端点。Forge实际注册在版本初始化的GTLasers，Neo在GTMultiblocks，入口差异不代表两套业务。见[结构与状态专题](LOGISTICS_CORE_STATE_AUDIT.md)；完整搬运／生命周期待续。

## 先按层找代码

| 目录 | 职责 |
| --- | --- |
| `core/src/main/java/com/gregtech/gregtech` | 不依赖 Minecraft 的材料、数量、配方合同、能源计算、结构目录等领域代码 |
| `core/src/api/java` | 对附属模组公开的纯 Java API |
| `multiversion/src/main/java/com/gregtech/gregtech` | 两版本共用的 Minecraft 交互、注册适配、渲染、机器和界面；必要差异用 Stonecutter 条件表示 |
| `multiversion/src/api/java` | 需要 Minecraft 类型的公开 API |
| `multiversion/versions/1.20.1-forge/src` | Forge 生命周期与仍需分离的版本适配 |
| `multiversion/versions/1.21.1-neoforge/src` | NeoForge 生命周期与仍需分离的版本适配 |
| `*/build/generated/stonecutter`、`build/native-tools` | 自动生成／投影视图，不是人工维护入口 |
| `tools/integration` | 来源导入、结构／语言检查和构建衔接；旧原生写入工具通过 `tools/native_tool.py` 执行 |

`core` 中的数据不能为了方便调用而依赖原生加载器。主源码、API、GameTest 和客户端探针分别维护，不能把测试逻辑塞回生产启动流程。

## 批量材料数据从哪里看

材料的实际数值定义位于 `core/src/main/java/com/gregtech/gregtech/content/material/generated/`。不要只看提供字段引用的 `Materials.java`、`GT6Materials.java` 或旧短名门面。

| 要找的内容 | 实际文件（相对于 core 的 Java 根目录） |
| --- | --- |
| 元素、金属元素的颜色、熔点、沸点、密度、工具参数 | `content/material/generated/ElementMaterials.java`，例如 Iron |
| 合金、化合物和其它复合材料 | `content/material/generated/CompoundMaterials.java`，例如 Bronze、Steel |
| 矿物、石材、木材 | 同目录的 `OreMaterials.java`、`StoneMaterials.java`、`WoodMaterials.java` |
| 金属／粉末／宝石等材料工厂的默认属性 | `api/material/MaterialFactories.java` |
| 材料组成、矿物处理关系 | `data/generated/MaterialCompositionData.java`、`MaterialOreProcessing.java` |
| 定义与后续关联的执行顺序 | `content/material/MaterialDefinitions.java` 的 `declare()`、`link()` |
| 单个材料保存的属性字段与访问方法 | `api/material/GTMaterial.java` |
| 哪些材料生成锭、板、杆等形态 | `data/generated/MaterialForms.java`；批量物品定义在 `api/material/MaterialItemDefinitions.java` |
| 按名字、数字 ID 保存材料的运行时注册表 | `api/material/GTMaterialRegistry.java` 的 `BY_NAME`、`BY_ID`、`ALL_MATERIALS`；初始化在 `init()` |

`Materials.java` 是方便手写业务使用的全名字段入口；`data/generated/GT6Materials.java` 主要保留原版短名引用并触发五类材料初始化，`data/ImportedMaterialData.java` 保留原版符号和少量特殊定义。它们不是另一套独立材料实例全集。新增补充材料另见 `SupplementalMaterials.java`、`ParticleMaterials.java`、`VanillaMatterMaterials.java`。

这些材料数据属于共享 core，两版本共同读取。生成文件的持久修改需要同步对应生成器（`tools/transpile_gt6_materials.py` 与 `tools/modular_materials.py`），否则重新生成会覆盖。工厂继承／加工属性的已知缺口仍见审计主表，不能根据一个定义行推断所有后续加工行为。

CompoundMaterials 现保留用户统一颜色列；生成端和独立排版入口使用同一计算规则。`dust(...).setTextureSet(...)` 与 `elec(..., texture)` 来自不同工厂签名，不能互换工厂统一外观写法，见[说明与七表重现](MATERIAL_TABLE_FORMATTING_20261020.md)。

## 初始化和配方从哪里看

### 定义表的空格与列对齐

材料字段名／等号、同类工厂调用的参数、连续构造器或注册表行使用空格对齐。逗号紧跟参数，补齐空格放在逗号后；不重排条目，不改数值、字面量或注释，不为对齐修改方法签名。两版共享的 core 表只维护一次。

`python tools/format_definition_tables.py` 只列出需要调整的定义文件；加 `--write` 才写入，也可传入指定 Java 文件。工具跳过无法完整解析的多行表达式和泛型参数，保留原有换行格式，写入前核对非空白内容／字符串／注释，并检查重复执行不再改变结果。它不是通用 Java 格式化器。

默认检查清单另包含特殊／导入材料、多方块参数和部件、仓储金属、基岩矿与矿脉、蜜蜂种类／槽位、装瓶及食物属性表。对应的源码读取脚本接受对齐空格，生产条目及提取顺序保持不变；见 [补充排版回执](verification/definition-table-formatting-supplement-20261019.json)。

材料拆分、机器参数、机器含材、储存含材、黏土模具及共用导入输出已经接入相同对齐函数。旧生成器仍应遵循各自的来源／工具视图要求；格式化本身不能证明重新导入的数据正确。排版时发现的旧材料快照漂移已补回生成链：从原版生成的七份文件与当前源码逐字相同，生产材料数据未变，见 [材料生成链核查](MATERIAL_GENERATION_AUDIT.md)。

物质制造的 28 条特殊转换纯数据现在位于 core `content/recipe/MatterConversionCatalog.java`；共享原生 `MatterRecipeLoader` 遍历它并解析物品／流体，另展开元素分离。流体身份定义是 core `data/FluidCatalog.java`，须同时区分条目 materialKey 与 boundMaterial 旁表；原生边界 `RegisteredFluids` 继承同一目录。相关消费和数量缺口见 [物质流体核查](MATTER_FLUID_AUDIT.md)。

- Forge 入口：版本目录的 `com/gregtech/gregtech/GregTech.java`。按 `content`、`machine registration`、`material linking` 职责调用初始化步骤。
- NeoForge 入口：版本目录的 `platform/neoforge/GregTechNeoForge.java`。保留原生注册事件及材料／发酵准备顺序。
- 共用机器配方入口：共享目录的 `content/recipe/MachineRecipeBootstrap.java`。两平台共用注册清单；显式配方在前，通用形态处理随后，导入的大表最后补充。
- 发酵配方在 NeoForge 材料后处理前绑定，在 Forge 清单中注册；燃料表在 Forge 基础数据阶段创建，在 NeoForge 配方阶段创建。这两个条件点被明确保留，不能为“统一”随意调换顺序。
- `bootstrap/InitializationStep` 是一个有明确职责的初始化动作，也兼容 `Runnable`；`SetupOnce` 防止重复注册并保留首次异常。
- Forge 的工作台配方随 RecipeManager 重载注入，见 `recipe/RuntimeRecipeLifecycle.java`。`*RecipeInjector` 表示这些注入器，不与初始化机器配方混用。
- 导入配方表位于 core 的 `content/recipe/generated`；原生 `GeneratedRecipeResolver` 负责物品／流体解析，`GeneratedRecipeSink` 负责向当前解析上下文输出纯数据。`GTTechnologyRecipes` 是手写的原版嵌入配方目录，不是机器控制器。

DecompositionRecipeLoader、VanillaProcessingRecipes、AlloyRecipeLoader、ChemicalRecipeLoader、GeneratedRecipeResolver目前各共用一份业务。前四者仅原生导入／holder查询留条件；解析器另保留combs／technology真实顺序、注册表查找与命名空间表达式差异，见[结构盘点](NATIVE_BODY_SHARING_AUDIT.md)及[后两项共享](RECIPE_RESOLVER_SHARING_AUDIT.md)。这不代表其它741份条件输入都需要删除分支，也不表示其余方法职责已经核完。

配方表本身的名称、物品槽／储罐数和构造标志在 core 的 `data/MachineRecipeMapDefinitions.java`，数据结构是 `api/recipe/RecipeMapSpec.java`；原生 `data/MachineRecipeMaps.java` 将它们转成 RecipeMap。`dynamicRecipes`／`contextualRecipes` 安装动态查询入口，fake 展示行不参与机器加工。当前表标志和动态入口仍有确认缺口，见 [动态配方核查](DYNAMIC_RECIPE_AUDIT.md)。只读工具 `tools/integration/inventory_recipe_map_flags.py` 使用 `--source <原版RM.java> --current <当前定义文件> --output <报告>` 比对四个尾部标志，报告未解析行；它不会替代配方执行验收。

核反应堆的棒目录／冷却参数／纯算术位于core `content/nuclear`；实际棒ItemStack数据、流体身份、网络tick与实体状态位于共享原生同名内容包及`blockentity/energy`。核心压行已展开，三组原版行为专题继续，见[核反应堆职责导航](NUCLEAR_SOURCE_LAYOUT.md)。

## 机器参数与运行行为从哪里看

流体管道和普通储罐目录在 core `content/transport/fluid/FluidTransportDefinitions.java`；管道规格 `PipeSpec`、通道均衡 `FluidPipeChannels`、危险数值 `FluidPipeSafety` 均属 core。Forge 由 `content/fluid/FluidPipeDefinitions` 消费，Neo 由 `content/transport/fluid/FluidTransportRegistries` 消费；共享 `FluidPipeBlockEntity` 负责通道状态、邻居交互和危险处理。数值表中的防护不等于接触伤害开关或仪表接口已接入，见 [流体管道核查](FLUID_PIPE_AUDIT.md)。

物品管道目录为 core `content/transport/ItemPipeCatalog.java`，规格展开是 `api/machine/ItemPipeSpec.java`；shared `content/transport/ItemPipeDefinitions.java` 调用 `registry/GTItemPipes.java` 注册两版方块。core `WeightedItemPipeRoutes` 只选择出口的第一跳，`ItemPipeTransfer` 只处理目的库存的模拟／实际交付；原生库存、每 tick 转运和侧面能力仍在 `blockentity/machine/ItemPipeBlockEntity.java`。目录参数正确不代表这些消费路径正确，详见 [物品管道核查](ITEM_PIPE_AUDIT.md)。

普通导线／电缆的 30 组额定数据在 core `content/energy/WireCatalog.java`；有符号损耗和计数算术在 `WirePacketRules.java`，实际递归输电／过载 tick 在 shared `blockentity/energy/ElectricWireBlockEntity.java`。`api/energy/EnergyNet.java` 是辅助连接索引，当前输电并不通过它调度；不要把它和原版引入的 IC2 EnergyNet 混为一谈。核查范围和缺口见 [导线报告](ELECTRIC_WIRE_AUDIT.md)。

| 要找的内容 | 入口（相对于对应 Java 根目录） |
| --- | --- |
| 原版各档机器的额定输入、并行数和硬度 | core 的 `data/BasicMachineOriginalParams.java`；这是参数表 |
| 单方块机器的超频、效率、点火、恒定供能豁免 | core 的 `content/machine/OriginalBasicMachineRules.java` |
| 工作成本与能量包的纯计算 | core API 的 `api/recipe/MachineWorkCost.java`、`api/machine/BasicMachineEnergy.java` |
| 实际启动、推进、中断、输出和保存 | shared 的 `blockentity/machine/BasicMachineBlockEntity.java`；多方块子类还会覆写结构和供能规则 |
| 待产物抽取、分堆、交付和保存 | shared 的 `api/recipe/MachineWorkOutputs.java` |
| 玩家持工具操作机器 | shared 的 `block/machine/BasicMachineBlock.java`，覆盖板交互再转给相应处理类 |

“参数表中有标志”不代表运行代码已消费它。机器审计已经发现完成余量、固定输出位置、中断和 KU 完成条件等差异，具体见 [加工执行](BASIC_MACHINE_EXECUTION_AUDIT.md) 与 [中断／供能](BASIC_MACHINE_INTERRUPTION_AUDIT.md)。

## 普通储液桶与发酵

共享 `blockentity/machine/TankBlockEntity.java` 处理普通桶的保存、能力、自动排液和发酵；`BarrelFermentationRules` 只做比例算术。Neo 的 `FermentationAccess` 已在 bootstrap 绑定配方查询，`FluidTransportEnvironment` 是当前环境／熔岩伤害动作，不等同原版桶按内容物选择火焰或熔岩的逻辑。物品态与世界态填充接口必须分别沿继承链核查，详见 [普通桶核查](BARREL_AUDIT.md)。

## 电变压器的参数与控制

额定目录在 core `content/energy/EnergyNodeDefinitions.java`；开关、模式、双方向输出状态和 64 tick 显示状态在 `content/energy/TransformerControlState.java`。共享原生 `content/energy/ElectricTransformerControl.java` 仅适配 MachineControl 和 NBT；实际输入、输出和切换方向仍在 `blockentity/energy/EnergyNodeBlockEntity.java`。包数取整帮助函数是 core `api/energy/EnergyPackets.java`，公共最小包门限是 `EnergyGate.java`，两者不能混同。现存边界差异见 [电变压器核查](ELECTRIC_TRANSFORMER_AUDIT.md)。

## 电池盒与电池物品

core `content/energy/BatteryBoxDefinitions.java` 定义二十种盒，`BatteryBoxState.java` 处理工作缓存、充放电分档与输出模式。共享原生 `content/energy/BatteryBoxEnergy.java` 按固定槽位适配物品能源接口及世界回调，并读写 NBT；原生库存和 tick 仍在 EnergyNodeBlockEntity。化学电池目录是 core `ChemicalBatterySpec`，单次包数规则是 `ItemBatteryRules`，原生物品是 `item/ChemicalBatteryItem.java`。Energium 材料宝石尚未拥有同样的储能能力，详见 [储能核查](BATTERY_STORAGE_AUDIT.md)。

## ZPM 与尚缺的特殊储能

core `content/energy/ZpmWorkRules.java` 管容量、补缓存量和光照，`refillAmount` 表示从神器提取到放能器，不是给神器充电。共享原生 `ZpmEnergy` 处理两版物品数据；`ZpmModuleBlockEntity` 与 `ZpmDischargerBlockEntity` 分别负责摆放的神器和放能器。Forge 注册在 GTLasers，Neo 在版本目录的 GTZpmArtifacts；ELITE 是 BASIC 引用别名。crystal_chargers／portable_power_cells 目前仅有分类，不能当作控制器入口，详见 [特殊储能核查](SPECIAL_STORAGE_AUDIT.md)。

## 命名与修改约定

物质制造机结构维护在 core `content/multiblock/SharedMatterFabricatorStructure.java`，主控 `MatterFabricatorBlockEntity` 只补充 QU 参数和处理器互换验证，实际加工／保存沿大型配方机父类。共用 `content/recipe/MatterRecipeLoader.java` 生成特殊转换与元素分离行，不能只搜索同名动态 RecipeMap。`tools/tests/test_fusion_resources.py` 已直接读取维护源码和三层资源位置，不再借助 native 工具视图；它是源引用检查，不能替代游戏渲染验证。

聚变的实际运行入口是 shared `blockentity/machine/FusionReactorControllerBlockEntity.java` 的专用 ticker，不能只看 BasicMachine。部件结构数据在 core `content/multiblock/SharedFusionStructure.java`，原生 `FusionStructure.java` 只绑定共享数据。生成器 `tools/import_fusion_structure.py --source <原版控制器.java> --check` 可只读核对，`--output` 可指定临时输出；无需原生投影包装。内爆中空结构为 `SharedImplosionStructure`，三个特殊主控的参数入口为 `OriginalAdvancedControllerData`。运行行为差异见 [聚变／内爆核查](ADVANCED_CONTROLLER_AUDIT.md)。

- 包名表达职责，不再新增 `a/b/c` 阶段目录或并列的 `loader/loaders`。
- 类名使用正常 Java 命名：如 `MaterialBootstrap`、`DeferredRegistrySubmission`、`FusionRecipeLoader`、`WoodCraftingRecipeInjector`、`OvenRecipeBridge`。
- 公共类型名与文件名一致。生成目录名与数据来源名保留含义，不把所有 `GT` 前缀机械删除。
- `Loader_Recipes_*` 等原版类名仍可以出现在来源注释、原版路径和历史回执里；这些是溯源信息，不是未改名的活动类。
- 修改生成数据时同步生成器，输出必须落在实际维护目录；不得重新生成另一份平台数据副本。
- 共用初始化清单的顺序影响配方冲突优先级。重构需比较两平台改动前后的实际调用顺序，并检查平台准备动作与附属 `recipesReady` 回调的位置。
- 一般生产代码保持按方法／职责分行。剩余压行实现、过大职责类和原版属性映射仍在审计，不将本轮改名算作全部清理完成。

本轮 30 个类名的完整映射、两版 54 步初始化序列和验证范围见 [标准化回执](verification/loader-standardization-20261019.json)。

材料生成表需要区分“表已存在”与“属性已正确继承／被玩法消费”。目前的工作属性表和处理目标表仍有确认缺口，见 [材料处理审计](MATERIAL_PROCESSING_AUDIT.md)。`tools/integration/inventory_processing_target_consumers.py` 是只读导航工具，输出引用位置，不会替代语义核查或写回原版源码。

纯 core 生成器不需要经由原生投影视图：`tools/extract_gt6_workability.py` 和 `tools/extract_gt6_processing_targets.py` 使用 `--source <MT.java>`，默认输出维护中的 core 文件；先用 `--check` 比较。临时生成可用 `--output` 指向 `work/`，工作属性报告仅在显式传 `--report` 时生成。`tools/generator_output.py` 共用输出／检查逻辑。它们的算法局限仍在审计表中，不应把可重复生成当作内容已经对齐。

## 旋转变速器与可调齿轮箱入口

`content/energy/GearboxCatalog` 保存十三种材料及两类设备的额定目录；`GearboxRotationRules` 是可调齿轮箱的齿轮啮合／方向纯函数。两者职责不同。共享 `registry/GTGearboxes` 消费目录，旋转变速器运行在 `blockentity/energy/EnergyNodeBlockEntity` 的专用分支，没有接入同目录的 OriginalRotaryConverter.State。控制差异见 [核查报告](ROTATION_TRANSFORMER_AUDIT.md)。

## 长距离线路入口

`content/logistics/LongDistanceCatalog` 是设备与线种目录；`LongDistanceNetwork` 负责纯扫描，`LongDistanceLink` 负责单接收端归属及目标坐标，`LongDistanceRules.loss` 负责每包损耗。实际重载／线路失效位于共享 `block/misc/LongDistanceTransformerBlockEntity`、`LongDistEndpointBlockEntity` 与 `LongDistanceTopology`，不能仅通过 core 测试断言其正确。旧 MAX_SCAN／unique／rescan helper 不是当前原生扫描／缓存策略，见 [核查](LONG_DISTANCE_ENERGY_AUDIT.md)。

## 能力转发器的共用业务

共享 `blockentity/CapabilityRelayBlockEntity` 的 ItemRelay／FluidRelay 维护物品与流体操作；forwardItems／forwardFluids 只选择平台能力常量，不放业务规则。`LongDistEndpointBlockEntity` 选择路由、物品或流体类型和流体权限；`RelayVisitSet` 只保存线程访问集合，清理责任在调用者。源码去重核对入口为 `tools/integration/check_relay_sharing.py --baseline c07466737`，不依赖先生成完整原生视图。

## 轴的转速与功率目录

`content/energy/AxleCatalog` 以 material／maxSpeed／powerLimits 保存十三材料四尺寸，family／sizeTier 是目录循环变量；`api/energy/AxleSpec` 是公开规格。`AxleConnectionRules` 只核直线连接。实际输送和过载在共享 `blockentity/energy/AxleBlockEntity`，可调箱缓存／输出调度在独立 GearboxBlockEntity，不能互相替代审计，见 [报告](AXLE_GEARBOX_AUDIT.md)。

## 物流桶与共享储液实现

- `core/.../content/transport/fluid/LogisticsTankSpec.java`：32072 物流桶容量、材料、防护和温限。
- `multiversion/src/main/java/.../blockentity/machine/LogisticsTankBlockEntity.java`：直接物流存储接口、保留过滤及覆盖板保存；两版共用。
- `multiversion/versions/1.21.1-neoforge/src/main/java/.../registry/GTLogisticsTank.java`：Neo 注册入口；Forge 对应 GTTanks.registerLogisticsTank。
- `core/.../api/fluid/LongFluidStorage.java`：长整数容量、液量和策略；原生 `multiversion/src/main/java/.../api/fluid/FluidTankGT.java` 才持有流体身份及 NBT。

原版同名标志并非均有同语义，具体边界见 [物流桶核查](LOGISTICS_TANK_AUDIT.md)。

## 流体身份、可调容量和物品能力

- `multiversion/src/main/java/.../api/recipe/RecipeMap.java`：按流体注册 ID 统计配方所需罐大小。
- `multiversion/src/main/java/.../api/fluid/FluidTankGT.java`：原生流体身份、容量查询、填充／抽取及保存；纯数量计算在 LongFluidStorage。
- `multiversion/src/main/java/.../registry/GTWorldWaterFluid.java`：GT 世界水的原版水类型兼容边界。
- `multiversion/versions/1.21.1-neoforge/src/main/java/.../content/transport/fluid/FluidTransportBlockItem.java`：桶／管道物品提示和堆叠上限；并非物品流体 handler。
- `multiversion/versions/1.21.1-neoforge/src/main/java/.../platform/neoforge/fluid/FluidCapabilities.java`：流体展示物品、便携小容器及坩埚的能力注册。

注册表的类型 ID 与流体 ID、物品数据与容器能力之间的差异见 [专题报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。

## 组件覆盖板与排水口

- `core/.../content/cover/ComponentCoverRules.java`：泵、传送带、机械臂的档位、周期、方向、槽号；不操作世界。
- `core/.../content/cover/DrainCoverRules.java`：三种世界水的产物映射与雨量公式。
- `multiversion/src/main/java/.../content/cover/ComponentCoverRuntime.java`：覆盖板通过宿主／邻接能力搬运物品和流体。
- 同目录 `CoverAttachmentBehaviors.java`：排水口世界取液、雨水、经验球、动物与通风口。
- `BasicMachineBlockEntity`、`FluidPipeBlockEntity` 和 `PanelCoverRuntime`：分别提供本地或世界时钟，不能只读规则类判断实际调度。

已有实现、未对齐边界和验证限制见 [覆盖板核查](COMPONENT_DRAIN_COVER_AUDIT.md)。

## 物品／流体过滤与检索器

- `core/.../content/logistics/FilterPolicy.java`：54 个物流模板槽和黑白名单／标签策略，不代表所有覆盖板的统一匹配合同。
- `multiversion/src/main/java/.../content/logistics/FilterRules.java`：两版原生物品数据比较与前缀识别。
- 同一共享源码根下 `content/cover/CoverUtilityBehaviors.java`：普通过滤器设置／匹配，以及当前检索器搬运。
- `blockentity/machine/BasicMachineBlockEntity.java` 与 `ItemPipeBlockEntity.java`：提供目标库存、本地时钟与 pending 生命周期。
- `content/cover/PanelCoverRuntime.java`、`CoverConnections.java`：覆盖板安装和管道连接限制。

三套筛选合同、当前流向和待核边界见 [过滤器与检索器核查](FILTER_RETRIEVER_COVER_AUDIT.md)。

## 覆盖板的原生与后备能力边界

- `multiversion/src/main/java/.../content/cover/ComponentCoverAccess.java`：物品／流体能力包装，动态读取覆盖板，不含检索器专用封锁。
- 同目录 `ComponentCoverFallback.java`：根据 getCover 声明类选择后备宿主；原生重写者自行接入。
- `mixin/ComponentCoverCapabilitiesMixin.java`：Forge 指定实体返回值与 Neo BlockCapability 返回值包装，均受 uses 门控。
- `blockentity/energy/EnergyNodeBlockEntity.java`：电池盒原生库存与蒸汽口，两者接入方式不同。

原版父类入口和当前接入矩阵见 [能力核查](COVER_CAPABILITY_AUDIT.md)。

## 压力阀连接与容器模板采样

- `multiversion/src/main/java/.../content/cover/CoverUtilityBehaviors.java`：压力阀释放判断、执行、温度伤害；valveBlocksPipeConnection 是未接入的拦截谓词。
- 同目录 `CoverConnections.java`：连接状态和扳手共用门控。
- 同目录 `UtilityCoverInteraction.java` 的 sample：能力／瓶子／桶／物品 ID 回退；ComponentCoverEvents 先于方块交互调用。
- `blockentity/machine/FluidPipeBlockEntity.java`：阀门最终安装限制与覆盖板计数调度。

当前缺口与已有路径见 [压力阀核查](PRESSURE_VALVE_AUDIT.md)。

## 模式选择器与按钮生命周期

- `multiversion/src/main/java/.../content/cover/PanelCover.java`：手动、红石、按钮、输出与显示等面板角色定义。
- 同目录 `PanelCoverRuntime.java` 的 beforeTick：控制器、固定标签、模式恢复、按钮计数；click／configure 处理玩家操作。
- `ComponentCoverStorage.java`／`CoverDrops.java`：面板物品和全局停止状态的持久化入口；不能替代模式恢复流程验证。
- `blockentity/energy/EnergyNodeBlockEntity.java` 的 tickOriginalConverter：面板与机器工作的先后顺序。

停止合同、恢复和末轮复位差异见 [选择器核查](SELECTOR_PANEL_AUDIT.md)。

## 自动控制、运行状态与检测输出

- `multiversion/src/main/java/.../content/cover/MachineCoverSpec.java`：控制条件和检测输出谓词；参数的实际来源由调用者负责。
- `api/machine/MachineControl.java`：控制边界，目前未提供独立 possible／success。
- `blockentity/machine/BasicMachineBlockEntity.java`：coverState、switchesAllowRunning、updateCoverSignals 专用路径。
- `block/misc/ExtenderBlockEntity.java`：模式、开关、运行／进度转发；通用面板经 PanelCoverRuntime 和 CoverSignalMixin 输出。

真假状态缺口与时钟来源见 [控制器核查](CONTROLLER_DETECTOR_AUDIT.md)。

## 显示图层和计量回退

- `multiversion/src/main/java/.../content/cover/CoverProgress.java`：无原生 MachineControl 时按内容物／容量生成进度视图，不能替代吞吐或发酵接口。
- 同目录 `PanelCoverRuntime.java` 的 scale／energy／afterTick：红石分档、能源类型、状态位和显示更新。
- `client/PanelCoverRenderer.java` 的 layers：原版贴图选择，尚无图层高亮属性。
- `client/CoverSurfaceRenderer.java`：实际写入顶点的光照与贴图坐标。

新增亮度缺口与 ALIGN-077 范围修订见 [显示核查](DISPLAY_METER_AUDIT.md)。

## 独立传感器执行链

- `core/.../api/sensor/SensorControl.java`：八种模式与阈值／红石计算。
- 同目录 `SensorAverage.java`：采样窗口、int 样本、long 累计和保存索引。
- 同目录 `SensorPanelRules.java`：输入面、布局、单位／颜色、TPS 和传输上限算术。
- `multiversion/src/main/java/.../blockentity/sensor/SensorBlockEntity.java`：采样调度、原生目标读取、最大值及同步保存。
- `tools/integration/audit-probes/SensorModeAudit.java`：五项纯规则原版差异观察，不依赖 Minecraft。

实际缺口、已有入口和验证范围见 [独立传感器核查](SENSOR_EXECUTION_AUDIT.md)。

## 特殊仪表与重量数据

- `multiversion/src/main/java/.../blockentity/sensor/SensorBlockEntity.java` 的 TACHOMETER／GEIGER／WEIGHT 分支：实际源值与量程。
- `core/.../api/sensor/SensorMeasurements.java`：重量和体积单位转换；不决定被测材料来源。
- `multiversion/src/main/java/.../api/machine/crucible/CrucibleItemInput.java` 与 core 的 `CrucibleInputRules.java`：投料回收／矿石产量合同，当前测重复用了它。
- `blockentity/inventory/MassStorageBlockEntity.java`：stored() 与自动化单堆视图的区别。
- `content/nuclear/RodPhysics.java`／core `ReactorRodCatalog.java`：燃料棒最大中子值与冷却剂调整；仪表尚未读取该上限。

量程、重量与库存视图的核对见 [特殊仪表报告](SENSOR_SOURCE_AUDIT.md)。

## 手持扫描仪与行为归属

- `core/.../content/tool/ItemBehaviorCatalog.java`：46 条物品 ID 与行为归属；每条独立一行。
- `multiversion/src/main/java/.../item/behavior/ItemBehaviors.java`：行为类选择和作物／方块扫描分派。
- `item/ScannerItem.java`：物品能源、交互、付费与聊天输出门控。
- `item/behavior/BehaviorScanner.java`：实际诊断内容与按段计价，不是完整 WD.scan 等价实现。
- `core/.../content/tool/ScannerEnergyRules.java`：扫描档位、容量、packet 与使用扣费纯规则。

活动注册、休眠旧类和实际缺项见 [扫描仪核查](SCANNER_BEHAVIOR_AUDIT.md)。

## 作物分析与通用工具事件

- `core/src/api/java/.../api/crop/CropScanSource.java`：可选附属作物接口。
- `core/.../content/tool/OriginalCropScan.java`：公开接口／旧可选反射、分析等级与输出；局部 cropAccess／cardAccess 说明所持对象。
- `multiversion/src/main/java/.../content/cover/ComponentCoverEvents.java`：覆盖板最高优先级分派与事件取消。
- 同目录 `PanelCoverInteraction.java`：通用放大镜／配置路径；ComponentCoverInteraction 则处理机械臂等组件。
- `api/tool/GTToolHelper.java` 与 core `ManualToolRules.clickDamage`：直接损耗和原版返回成本的转换边界。

已核作物条件和诊断／损耗缺口见 [本轮报告](CROP_MAGNIFIER_AUDIT.md)。

## 保险箱、钥匙与衣柜

- core `content/storage/SafeLockRules.java`：十种钥匙材料、所有者门控与绑定／复制／开关纯规则。
- 共享 `block/inventory/SafeBlock.java`、`blockentity/inventory/SafeBlockEntity.java`：真实交互入口、15 槽、锁状态和世界保存；钥匙物品是 `item/GTDungeonKeyItem.java`。
- `registry/GTDungeonKeys.java` 只解析规范物品引用，实际注册由 GTMultiItems 持有，不能再加一套别名物品。
- 共享 `block/inventory/LockerBlock.java`、`blockentity/inventory/LockerBlockEntity.java`：普通衣柜四装备槽与更衣；充电家族仍缺失。
- core `registry/GTStorageMetals.java`：60 材料规格；`data/UtilityCraftingRows.java` 与共享 `data/OriginalHandCraftingRows.java` 都要核查，后者的旧配方不会因前者存在而自动失效。
- core `content/storage/OriginalStorageTooltipData.java`：存储提示的行选择；不是玩法实现清单。

各宿主锁权限、正面限制、自动化和拆除不同，见[核查](SAFE_LOCKER_AUDIT.md)。全量路径／命名和剩余压行候选见[布局盘点](SOURCE_READABILITY_INVENTORY.md)。

## 瓶框、书架与原生配方桥接

- `block/inventory/BottleCrateBlock.java`：世界 X/Z 格子与整组取放；`blockentity/inventory/BottleCrateBlockEntity.java`：handler、同步、packedStack 和拆除快照。
- core `content/storage/ContainerStorageRules.java` 与共享 `content/tool/BottleCrateGeometry.java`：瓶身／液体／瓶颈坐标；不随框朝向旋转格子。
- `block/BookShelfBlock.java`、`blockentity/inventory/BookShelfBlockEntity.java`：书面交互、附魔、控制、loot 和保存；core `content/book/BookShelfRules.java` 与共享 `content/book/GTBookList.java`：当前允许集和书点，完整原版陈列登记仍缺部分。
- `registry/GTEngines.java`：Forge 走现有 MachineRegistry，Neo 走各类型实体构造分支，维护两版本体均已展开。
- `recipe/FiniteBottleFillingRecipe.java`：装瓶配方的消耗／余料版本边界；core `content/recipe/generated/GeneratedRecipeSink.java`：唯一配方表的线程内 consumer 与 finally 恢复，属于运行桥接，不是普通数据行。

缺项与十一份投影等价边界见[本批报告](BOTTLE_BOOKSHELF_AUDIT.md)。

## 金属箱、战利品与专用模型

- `block/inventory/MetalChestBlock.java`、`blockentity/inventory/MetalChestBlockEntity.java`：GUI 箱、物品掉落、能力、盖子与 pending／消费状态；`LootChestBlock` 保存专用目录的表身份。
- `client/MetalChestVisuals.java`：世界／物品共用的专用 RGB 参数与模型；普通 block tint Mixin 不会自动覆盖它。
- `block/LootCrateBlock.java`：一次性撬棍板条箱；与 GUI 箱经验生命周期不同。
- core `content/loot/LootRollRules.java`、共享 `content/loot/GTLootTables.java`：权重、计数与表填充；当前 inclusive count 不等于原版两种计数合同均已对齐。
- 书架 `BookShelfRules` 集合／坐标公式与 `GTBookList` 说明已展开；仍是当前子集，不是完整 BOOK_REGISTER。

源码范围和十四份等价投影见[箱子报告](CHEST_LOOT_AUDIT.md)。

## 养蜂规则与原生库存爆炸

- core `content/bumble/BumbleGenomeRules.java`：鲜基因范围、天气阈值及逐基因遗传；`BumbleSpeciesRules.java`：二十条组合、突变和产物概率。两份已展开排版，不意味着已修复所有边界。
- 共享 `content/bumble/BumbleBeeGenes.java`：Forge NBT／Neo components 边界和默认基因回退；`BumbleBreeding.java`：后代构造和配对成本。
- 共享 `block/tool/BumbliaryBlock.java`、`blockentity/tool/BumbliaryBlockEntity.java`：普通／高级养蜂箱服务端周期、库存、工作与寿终晋升。自然蜂巢另在 `block/misc/BumbleHiveBlock.java`／`FixedBumbleHiveBlock.java`，新生成使用扁平 ID。
- 共享 `content/bumble/BumbleSting.java` 先经过 `damage/GTHazmat.java`；不能孤立看攻击分支判断铁傀儡保护。
- 库存外部爆炸应从实际原生 `state.getDrops`／`onBlockExploded`／`onRemove` 追踪；`CoverDropMixin` 的静态助手不是该入口。方块物品与散落库存的掉落合同分开，见[核查](INVENTORY_EXPLOSION_AUDIT.md)。

养蜂来源与未核完边界见[周期报告](BUMBLE_LIFECYCLE_AUDIT.md)，战利品首空槽／原随机覆盖见[填槽报告](LOOT_SLOT_AUDIT.md)。没有将原生反编译源码纳入项目生产目录。

## 保存时机、库存通知与共用菜单

- `BumbliaryBlockEntity` 的寿命／配对计时写读与 `setChanged` 时机分开；实际区块保存有 isUnsaved 门控，见[持久化核查](BUMBLE_PERSISTENCE_AUDIT.md)。
- `MetalChestBlockEntity` 的普通 handler 回调、`HopperContainerMenu.quickMoveStack` 的原位变化与原生 SlotItemHandler 适配分别核对；菜单使用相同基类不保证每个宿主补偿方式相同。
- `api/machine/ITileEntityAdjacentInventoryUpdatable` 是专用通知合同；`setChanged`／`sendBlockUpdated`／普通邻居事件不会按名称自动转换为它。接收方与轮询范围见[库存报告](INVENTORY_NOTIFICATION_AUDIT.md)。
- BumbleBeeGenes 和 GTBumbleBlocks 已展开两版边界；BumbleHiveBlock 的旧 COLOR 只是历史兼容，新变种身份仍是 FixedBumbleHiveBlock。火焰及末影龙保护的实际原生来源见[自然巢报告](BUMBLE_HIVE_PHYSICAL_AUDIT.md)。

## 灌木环境与手动库存菜单

- core `content/plant/BushGrowthRules.java`：四阶段／byte计数的纯计算；共享 `blockentity/BushBlockEntity.java` 负责支撑、现代雨／光查询和保存，不能把 helper 差异藏在核心规则相同的结论里。
- 共享 `block/plant/BushBlock.java` 是实际放置、采收和枝条几何入口；`registry/GTBushes.java` 与两版注册目录决定扁平物种，旧berry键仅作兼容。已实现与差异见[灌木报告](BERRY_BUSH_AUDIT.md)。
- core `api/inventory/InventorySlotLayout.java` 保存当前菜单坐标；共享 `client/gui/HopperContainerMenu.java` 负责普通槽和shift。两个许可扩展方法当前未被槽工厂消费，不等于底层handler没有验证。
- Queue的自动化handler、手动GUI许可和原生doClick入口要一起核对；九类真实菜单宿主的六类同源回调、三类补偿／约束见[共用菜单报告](SHARED_MENU_AUDIT.md)。

## 液态材料工厂与补偿

- core `api/material/MaterialFactories.java`：当前工厂默认属性和纹理入口，长参数／方法已展开；不是所有默认值都与原版同名helper等价。
- `tools/transpile_gt6_materials.py`先识别源工厂族，`tools/modular_materials.py`再拆分声明；用户整理的CompoundMaterials保持独立颜色列。不要直接运行旧脚本覆盖采用后的数据。
- core `content/material/MaterialDefinitions.java`的link加载组成／事实／形态补偿；`content/fluid/FluidDefinitions.prepare`在实际流体注册前补状态。`data/generated/MaterialForms.java`只是前缀准入侧表，不能替代GTMaterial.GAS/LIQUID。
- `api/fluid/FluidHazards.isMagic`读取生成流体关联材料，`FluidPipeBlockEntity.checkSafety`真正消费危险标志。三项差异与已有补偿见[液态酸核查](LIQUID_ACID_FACTORY_AUDIT.md)。

## 灌木物理与世界生成入口

- 共享`registry/GTBushes.java`的注册工厂设置实际BlockBehaviour属性；`strength(.2f)`后还有`instabreak()`，最终值必须沿原生实现读取。`BushBlock`的选框、碰撞、光遮挡与墙内谓词各有消费者，见[物理核查](BERRY_BUSH_PHYSICAL_AUDIT.md)。
- 共享`worldgen/GTBushesFeature.java`选择候选列、群系、噪声索引和枝条；core`worldgen/GTCellNoise.java`只供噪声算法／偏移表。调用者半坐标并非原整数除法，不再在类说明中保证同坐标同莓种。
- 共享`worldgen/GTSurfaceFloraFeature.surface`负责实际向下扫描；两版`GTFeatures`、configured/placed feature及biome_modifier共同决定可达性。九种自然成员与未核完的中心／高度／替换规则见[生成核查](BERRY_BUSH_WORLDGEN_AUDIT.md)。

## 通用材料分解与流体组件解析

- 共享`content/recipe/DecompositionRecipeLoader.java`现为一份业务流程，只有原生FluidStack／holder类型及查询分支；`MachineRecipeBootstrap`在实际setup链中调用，随后显式化学／生成行仍可追加。
- 组成来自core `data/generated/MaterialCompositionData.java`，原处理地图标志并不等于组成；目前加载器按ELEMENT启发式选图，不能从注释或标签名字存在推断已接线。
- 原生水、具名流体、材料绑定和温度相态是不同入口；`namedFluid`没有复用`GTFluids.stack`／`GeneratedRecipeResolver.named`的原生水处理。样本、公式和显式补偿见[分解核查](MATERIAL_DECOMPOSITION_AUDIT.md)。

第六十批核辅助类已展开：core `content/nuclear/RodPhysicsMath.java` 的数量／向上取整与热／寿命运算，以及 `ReactorCoolantRules.java` 的11项冷却表／棒参数修饰改用明确参数名。原生Fluid解析仍看 `ReactorCoolants`，不能把表键当成已验证的实际注册流体；本次只做可读性保持，见[核导航](NUCLEAR_SOURCE_LAYOUT.md)。

第六十一批补充[配置入口与真实消费](CONFIGURATION_CONTRACT_AUDIT.md)：共享`GregTechConfig.java`定义三个键，两版入口注册COMMON spec，默认文件由加载器命名为`gt6ce-common.toml`。两个过载getter由`MachineOverload`等消费；日志键当前未被读取。原版材料／掉落／世界生成配置只核了代表矩阵，Nexus全开与配置收尾仍按用户安排。核状态和异常边界分别见[状态](NUCLEAR_TRANSITION_AUDIT.md)、[网络生命周期](NUCLEAR_NETWORK_LIFECYCLE_AUDIT.md)。

第六十二批补充[工具参与工作台配方导航](TOOL_CRAFTING_SOURCE_LAYOUT.md)：ToolShapedRecipe／ToolShapelessRecipe处理工具返还、镜像／形态与空容器限制；工具本身的头柄／整体制作另看GTToolRecipes及相应配方类。两版包装serializer注册分别在Forge ToolBootstrap和NeoRecipeSerializers，不能与GTToolRecipeSerializers的tool-kind／pattern编码混为一层。

第六十三批能源导航补充：[太阳能板](SOLAR_PANEL_AUDIT.md)从core SolarPanelEnergy／EnergyNodeDefinitions追到GTEnergyNodes与EnergyNodeBlockEntity.tickSolar；[单块换热器](HEAT_EXCHANGER_AUDIT.md)从HeatExchangerCatalog／GTHeatExchangers追到HeatExchangerBlockEntity、HotFluidFuelCycle和共用HeatExchangerRules。大型主控另有结构与出口，HeatExchangerData不能作为单块保存补偿。核棒参数扩展／支持SDK／耗尽出口区别见[核扩展](NUCLEAR_EXTENSION_AUDIT.md)。

第六十四批[旋转泵导航](ROTATIONAL_PUMP_AUDIT.md)：core PumpCatalog／PumpSpec负责四档定义，PumpWorkRules只给当前阈值与包预算；真实世界搜索／删块、保存和推液均在原生PumpBlockEntity，PumpBlock给ticker、GTPumps与两端入口完成注册。ComponentCoverHostMixin可补上后备覆盖板宿主，机器停启／运行状态仍需独立Provider；EngineCapabilities只是Neo流体能力桥。三处all局部变量与压行展开，不改抽液算法。

第六十五批[大型涡轮源码导航](LARGE_TURBINE_AUDIT.md)：core OriginalGeneratorParameters给四档与派生额定，SharedTurbineStructure／StructureGrid给无加载器几何；原生Definitions／Factory注册具体墙与主控。AxialGeneratorBlockEntity管共同结构／轴输出／故障／控制，LargeTurbine管蒸汽两半和凝水，LargeGas独立管燃料／三排气／RU队列；capability的role包装在FluidPort／MultiblockPortBlockEntity。最终掉落必须再追CoverDropMixin→CoverDrops；父类共享不代表大型发电机已全面核完。

第六十六批[避雷针导航](LIGHTNING_ROD_AUDIT.md)：实际17998由MultiblockBlockFactory创建OriginalLightningRodControllerBlock，执行在LightningRodControllerBlockEntity；AdvancedControllerRules／OriginalUtilityControllerData给包与容量，旧通用LightningRodControllerBlock并非第二个注册主控。认领、天气轮询与普通pick要分别追，最终drop还经CoverDropMixin。ControllerStructureLayouts用于有限预览，不能据其地图推定运行已绑定或有建造工具；两个源码展开和四绑定改名没有改玩法。

第六十七批[大型发电机导航](LARGE_DYNAMO_AUDIT.md)：SharedLargeMachineParts原ID17221–17224→MultiblockBlockFactory→LargeDynamoControllerBlock及GTBlockEntities.LARGE_DYNAMO；BE只负责RU准入／旧键兼容，结构与75%转换在AxialGeneratorBlockEntity，档位由OriginalGeneratorParameters→AxialGeneratorDefinitions映射。SharedTurbineStructure／StructureGrid为纯坐标，AxialStructureTransform是原生坐标边界；后端部件查询与真正后4格输出须分别追。五源码展开与八局部命名仅提高可读性。

第六十八批[大型换热器导航](LARGE_HEAT_EXCHANGER_AUDIT.md)：GTMultiblocks→LargeHeatExchangerControllerBlock／GTBlockEntities，BE负责世界17格认领、八出口与冷热能力／持久化；SharedHeatExchangerStructure给纯布局，HeatExchangerRules给纯数值计划，HotFluidFuelCycle处理原生流体／配方，HeatExchangerData处理版本设置／NBT。Neo流体入口在BasicMachineRegistries，而非单块用的SmelteryRegistries；制作由MultiblockCraftingRecipes→MultiblockRecipePack→MachineRecipeIngredients。六源展开只改善阅读，不增平台业务副本。
