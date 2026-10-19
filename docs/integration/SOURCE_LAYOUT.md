# 当前源码导航与命名约定

此文描述 `main` 的 Stonecutter 结构。原版行为差异见 [原版对齐审计](ORIGINAL_ALIGNMENT_AUDIT.md)；目录整理不代表全部功能已经对齐。

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
| 按名字、数字 ID 保存材料的运行时注册表 | `api/material/GTMaterialRegistry.java` 的 `BY_NAME`、`BY_ID`、`ALL_MATERIALS`；初始化在 `init()` |

`Materials.java` 是方便手写业务使用的全名字段入口；`data/generated/GT6Materials.java` 主要保留原版短名引用并触发五类材料初始化，`data/ImportedMaterialData.java` 保留原版符号和少量特殊定义。它们不是另一套独立材料实例全集。新增补充材料另见 `SupplementalMaterials.java`、`ParticleMaterials.java`、`VanillaMatterMaterials.java`。

这些材料数据属于共享 core，两版本共同读取。生成文件的持久修改需要同步对应生成器（`tools/transpile_gt6_materials.py` 与 `tools/modular_materials.py`），否则重新生成会覆盖。工厂继承／加工属性的已知缺口仍见审计主表，不能根据一个定义行推断所有后续加工行为。

## 初始化和配方从哪里看

物质制造的 28 条特殊转换纯数据现在位于 core `content/recipe/MatterConversionCatalog.java`；共享原生 `MatterRecipeLoader` 遍历它并解析物品／流体，另展开元素分离。流体身份定义是 core `data/FluidCatalog.java`，须同时区分条目 materialKey 与 boundMaterial 旁表；原生边界 `RegisteredFluids` 继承同一目录。相关消费和数量缺口见 [物质流体核查](MATTER_FLUID_AUDIT.md)。

- Forge 入口：版本目录的 `com/gregtech/gregtech/GregTech.java`。按 `content`、`machine registration`、`material linking` 职责调用初始化步骤。
- NeoForge 入口：版本目录的 `platform/neoforge/GregTechNeoForge.java`。保留原生注册事件及材料／发酵准备顺序。
- 共用机器配方入口：共享目录的 `content/recipe/MachineRecipeBootstrap.java`。两平台共用注册清单；显式配方在前，通用形态处理随后，导入的大表最后补充。
- 发酵配方在 NeoForge 材料后处理前绑定，在 Forge 清单中注册；燃料表在 Forge 基础数据阶段创建，在 NeoForge 配方阶段创建。这两个条件点被明确保留，不能为“统一”随意调换顺序。
- `bootstrap/InitializationStep` 是一个有明确职责的初始化动作，也兼容 `Runnable`；`SetupOnce` 防止重复注册并保留首次异常。
- Forge 的工作台配方随 RecipeManager 重载注入，见 `recipe/RuntimeRecipeLifecycle.java`。`*RecipeInjector` 表示这些注入器，不与初始化机器配方混用。
- 导入配方表位于 core 的 `content/recipe/generated`；原生 `GeneratedRecipeResolver` 负责物品／流体解析，`GeneratedRecipeSink` 负责向当前解析上下文输出纯数据。`GTTechnologyRecipes` 是手写的原版嵌入配方目录，不是机器控制器。

配方表本身的名称、物品槽／储罐数和构造标志在 core 的 `data/MachineRecipeMapDefinitions.java`，数据结构是 `api/recipe/RecipeMapSpec.java`；原生 `data/MachineRecipeMaps.java` 将它们转成 RecipeMap。`dynamicRecipes`／`contextualRecipes` 安装动态查询入口，fake 展示行不参与机器加工。当前表标志和动态入口仍有确认缺口，见 [动态配方核查](DYNAMIC_RECIPE_AUDIT.md)。只读工具 `tools/integration/inventory_recipe_map_flags.py` 使用 `--source <原版RM.java> --current <当前定义文件> --output <报告>` 比对四个尾部标志，报告未解析行；它不会替代配方执行验收。

## 机器参数与运行行为从哪里看

| 要找的内容 | 入口（相对于对应 Java 根目录） |
| --- | --- |
| 原版各档机器的额定输入、并行数和硬度 | core 的 `data/BasicMachineOriginalParams.java`；这是参数表 |
| 单方块机器的超频、效率、点火、恒定供能豁免 | core 的 `content/machine/OriginalBasicMachineRules.java` |
| 工作成本与能量包的纯计算 | core API 的 `api/recipe/MachineWorkCost.java`、`api/machine/BasicMachineEnergy.java` |
| 实际启动、推进、中断、输出和保存 | shared 的 `blockentity/machine/BasicMachineBlockEntity.java`；多方块子类还会覆写结构和供能规则 |
| 待产物抽取、分堆、交付和保存 | shared 的 `api/recipe/MachineWorkOutputs.java` |
| 玩家持工具操作机器 | shared 的 `block/machine/BasicMachineBlock.java`，覆盖板交互再转给相应处理类 |

“参数表中有标志”不代表运行代码已消费它。机器审计已经发现完成余量、固定输出位置、中断和 KU 完成条件等差异，具体见 [加工执行](BASIC_MACHINE_EXECUTION_AUDIT.md) 与 [中断／供能](BASIC_MACHINE_INTERRUPTION_AUDIT.md)。

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
