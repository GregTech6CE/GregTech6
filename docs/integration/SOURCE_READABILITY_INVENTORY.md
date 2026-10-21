# 活动源码结构与可读性盘点

2026-10-21 第九十批：MaterialDataRules／GTMaterialDataRecipes／MaterialDataViewerRecipes／Neo MachineContextRecipes四源677→768行；保留已清楚的数组排版，展开紧凑参数、循环与注册。六完整代码308、3762／3859、738／738、315及字面量／其余注释／版本指令保持，修正覆盖计数、复制回退说明与不存在的mUSBTier引用。实际选择顺序差异另记290，未改玩法或用户材料，见[专题](MATERIAL_DATA_CHAIN_AUDIT.md)和[回执](verification/material-data-chain-audit-20261021.json)。

2026-10-20，基线 `36833296c`，读取当时含本轮排版的工作区。共 2253 个追踪且存在的 Java 文件，其中 1790 个活动生产／API 输入；其他 463 个属于测试、示例等，不能把数量差直接当重复或缺失。

| 输入层 | 文件数 |
| --- | ---: |
| core 公共 API | 39 |
| core 领域代码／数据 | 496 |
| 共享原生 API | 13 |
| 共享原生主代码 | 1041 |
| Forge 专有主代码 | 72 |
| NeoForge 专有主代码 | 129 |

实际 source set 将 Stonecutter 输出与节点专有代码共同编译，不会把共享维护输入再编译一次。旧根 `src`／`neoforge/src` 的追踪和磁盘 Java 均为零。

## 已完成的命名迁移

活动生产路径没有 `loader`、`loaders`、`a`、`b`、`c` 目录片段，两版投影也没有旧 `Loader_*`／`IGTLoader*` 类型声明。原版只读来源仍保留作者的旧命名，没有进入当前 source set；不得为了项目风格改写参考原版。

当前短文件名主要是版本边界 `Mc` 和保留 OredictManager 查找名的 `OM`，不是无意义占位。未发现公开顶级类型与主文件名不匹配的候选。命名改善不等于全部架构完成。

## 仍需整理的控制流程

| 维护文件 | 盘点时的具体问题 | 下一步 |
| --- | --- | --- |
| Neo `client/StorageContainerClientSetup.java` | 4 行，最长代码行 3929 字符 | 分开屏幕、渲染器、各类染色事件，保持顺序 |
| Neo `client/EngineClientSetup.java` | 9 行，最长 815 | 展开事件和颜色条件 |
| Neo `registry/GTRelaysFilters.java` | 6 行，最长 1413 | 展开四类注册和列表 |
| 共享 `registry/GTEngines.java` | Neo 投影 25 行、最长 496，Forge 最长仅 152 | 同时核对两版，不能只看活动 Forge 体 |
| 共享 `recipe/FiniteBottleFillingRecipe.java` | Neo 投影 33 行、最长 1002 | 展开 Neo 条件分支 |
| 共享 `blockentity/energy/ReactorCoreBlockEntity.java` | 多行包含 4–8 条语句 | 随反应堆审计逐方法整理 |
| core `content/recipe/generated/GeneratedRecipeSink.java` | ThreadLocal 的 try/finally 压行 | 保留异常恢复顺序，不能因 generated 路径忽略运行逻辑 |

盘点后已完成表中前三份 Neo 入口：StorageContainerClientSetup 4→81 行、EngineClientSetup 9→84 行、GTRelaysFilters 6→54 行；词法、字面量、注释和条件指令均保持。其原长行指标作为整理前的证据保留，不再列作当前待办。Neo GTSafes／SafeClientSetup 也已在本批展开。

2026-10-21 续：表中 GTEngines、FiniteBottleFillingRecipe 的隐藏 Neo 体及 core GeneratedRecipeSink 也已展开，五份执行投影保持；瓶框三份整理另有六份投影。表中对应行保留原盘点数值，当前不再列作待整理，见[第四十七批](BOTTLE_BOOKSHELF_AUDIT.md)。大型业务类与其它压行仍继续。

2026-10-21 再续：core BumbleGenomeRules 与 BumbleSpeciesRules 已从 21／42 行展开为 58／75 行，667／420 个执行词法项及字面量位置保持，随机顺序和数值没有改变。原生养蜂实体／基因边界仍有压行和职责整理待续，见[第四十九批](BUMBLE_LIFECYCLE_AUDIT.md)。用户 CompoundMaterials 的 570 条定义仍是等号第 66 列／RGB 第 141 列，内容／注释未变，格式化入口不再改写它。

第五十批继续展开 BumbleBeeGenes（232→308 行）与 GTBumbleBlocks（28→56 行），明确 NBT／组件、默认基因和注册工厂边界；自然巢说明纠正旧颜色身份及火焰参数归因。三份源码六份执行投影保持，见[自然巢](BUMBLE_HIVE_PHYSICAL_AUDIT.md)与[回执](verification/inventory-persistence-audit-20261021.json)。更大的养蜂实体及共用菜单仍待按职责核查／整理，没有将说明纠正算成玩法修复。

第五十一批已展开共用 InventorySlotLayout（247→254行）、HopperContainerMenu（115→123行）、BushGrowthRules（6→18行）及BushBlockEntity的压行／版本边界（215→229行）。四份源码六份执行投影保持，坐标值、计数和随机调用不变；说明不再声称所有原版菜单／环境已完全一致。共用菜单九类实际宿主及手动权限归属已追踪，见[菜单](SHARED_MENU_AUDIT.md)、[灌木](BERRY_BUSH_AUDIT.md)与[回执](verification/bush-menu-audit-20261021.json)。用户WoodMaterials编辑保持未纳入本批，CompoundMaterials统一RGB列继续保留。

第五十二批展开MaterialFactories长参数和方法（132→147行），以当前实现说明取代三处过强原版归因；1304项执行词法与字面量位置相同，不改变public接口、工厂属性和调用顺序。原elec／cent与当前gem／MAGICAL的实质差异进入[材料核查](LIQUID_ACID_FACTORY_AUDIT.md)，没有借排版偷偷修正；生成材料表与用户空格均保持。

第五十三批展开GTBushes注册／目录／物品工厂（43→58行），同时展开真实Neo注释体；GTBushesFeature整理换行并纠正三处过强原版归因（136→135行），明确算法表保留与调用半坐标差异。两份共享源码四份执行投影相同，注册顺序、地面／群系谓词、随机次数与字面量不变，见[物理](BERRY_BUSH_PHYSICAL_AUDIT.md)、[生成](BERRY_BUSH_WORLDGEN_AUDIT.md)及[回执](verification/bush-physical-worldgen-audit-20261021.json)。

第五十四批去掉DecompositionRecipeLoader中整份平台业务重复：336→124行、同一run/namedFluid流程，只在原生导入、holder类型和可用性查询处保留条件。选中Forge/Neo后850/858项执行词法与字面量位置逐项相同，class说明纠正原处理方式与当前启发式混同；不是只压缩空行，也没有借去重改变配方，见[报告](MATERIAL_DECOMPOSITION_AUDIT.md)与[回执](verification/material-decomposition-audit-20261021.json)。

第五十五批对全部1041份共享原生维护输入显式投影2082次，确认四项完整类体重复候选，并排除MetalChestBlockItem和GTEngines的声明／原生职责边界。VanillaProcessingRecipes与AlloyRecipeLoader已合并954→334行，四份完整执行词法／字面量位置保持；Chemical及有真实加载顺序差异的GeneratedRecipeResolver独立继续。171项方法级密度结果未经逐项核查，不是缺陷数，见[结构报告](NATIVE_BODY_SHARING_AUDIT.md)和[全量身份回执](verification/native-body-sharing-audit-20261021.json)。

第五十七批继续完成后两项Chemical与GeneratedRecipeResolver整类共享，1775→641行；四个候选总2729→975行。解析器两端加载顺序、注册表查找／命名空间表达式和所有公开接口保持，四份完整执行／字面量位置相同；旧数量、永久缺失、入口和统计注释单独纠正。未审方法级／大职责和其它原生注册边界继续，见[共享报告](RECIPE_RESOLVER_SHARING_AUDIT.md)与[证明](verification/chemical-resolver-sharing-20261021.json)。用户WoodMaterials和CompoundMaterials排版未改。

第五十八批完成原盘点中的ReactorCoreBlockEntity压行展开：236→560行、最长305→152；包含被Neo注释包裹的能力和保存同步。两份执行投影、各31字面量位置／5注释和30条件保持，未拆改状态或行为。棒目录／纯算术／冷却参数与原生网络／实体／流体身份导航见[核反应堆源码](NUCLEAR_SOURCE_LAYOUT.md)及[证明](verification/reactor-core-readability-20261021.json)；仍有其它压行／大职责待续，不把本次格式整理算成核反应堆完成。

346 个生产文件命中分号／花括号密度启发式，包括正常循环和布局数据，不能称为 346 个缺陷。坐标、形状、配方数据行可以保留紧凑表示；大类应按稳定职责拆分，不能只为降低行长移动代码。

## 生成数据归属

37 个文件位于 generated 目录，不代表只有 37 个生成文件；例如创造目录也有生成维护，而 GeneratedRecipeSink 属运行桥接。材料表由 `transpile_gt6_materials → modular_materials` 维护，工作属性／目标／事实有独立提取器，化学行与蜂巢行也各有来源工具。整理生成结果必须同步模板，并向隔离目录重现，不能直接执行会覆盖其它 adopted 内容的旧导入脚本。

本轮已将用户 CompoundMaterials 的统一 RGB 列同步到生成端，七表可重现，见[材料排版记录](MATERIAL_TABLE_FORMATTING_20261020.md)。仍发现旧 `extract_compositions.py` 固定其它磁盘路径，未执行它，也不据此断言当前生产组成错误。

本批 OriginalStorageTooltipData 仅展开列表、分支和方法，544 个词法项、54 个字面量、3 个注释保持。其它存储整理见[源链报告](SAFE_LOCKER_AUDIT.md)。[盘点摘要](verification/source-readability-inventory-20261020.json)保存范围、候选与摘要哈希；完整逐文件 scratch 清单未作为玩法证明。本轮没有用目录盘点声称全部代码已标准化，后续压行与职责拆分继续。

第六十批完成两份core核辅助类展开及七个参数作用域的有意义命名：RodPhysicsMath 3→29行、最长646→101；ReactorCoolantRules 22→68行、最长244→86。完整206／522项执行流仅归一列明的参数绑定后保持，两条过强职责说明另行更正，未改玩法或公开API名称。见[核导航](NUCLEAR_SOURCE_LAYOUT.md)和[证明](verification/nuclear-helper-readability-20261021.json)；其余职责与压行整理继续。

第六十一批依据实际对象图纠正ReactorNetwork的弱key生命周期保证注释；代码没有新增卸载清理，不能将文档修正当架构缺口已修。完整双端1228／1229项执行词法保持，原观察和最终源身份分别归档，见[生命周期](NUCLEAR_NETWORK_LIFECYCLE_AUDIT.md)及[证明](verification/nuclear-transitions-config-audit-20261021.json)。

第六十二批按当前真实源码选中ToolShapedRecipe和ToolShapelessRecipe，维护行254→456、168→247；旧FiniteBottleFillingRecipe／GeneratedRecipeSink已整理，不重复改写。匹配／工具返还／流体格位／网络编码流程展开，一处Neo载体闭合位置整理，四份完整词法、字面量、真实注释与指令保持。见[工具导航](TOOL_CRAFTING_SOURCE_LAYOUT.md)及[证明](verification/tool-recipe-readability-20261021.json)；密度筛选仍非重复／缺陷计数。

第六十四批整理PumpCatalog 12→27、PumpWorkRules 4→18维护行，all局部out/i/t改pumps/tierIndex/tier；原生PumpBlockEntity的输入预算、冷却、getter、Neo推液和NBT边界展开。完整core237／124与Forge1961／Neo1877项执行流在列明局部绑定归一后保持，其余标识符、数据、分支和指令不动，见[泵导航](ROTATIONAL_PUMP_AUDIT.md)与[证明](verification/rotational-pump-audit-20261021.json)。不是泵玩法修复或所有源码标准化完成。

第六十五批展开四份大型发电参数／涡轮执行源，grades局部list/i/material改gradesByMaterial/gradeIndex/gasGrade；保留私有record、公共名称、四档数据和平台边界。core441及六端1684／1712、1323／1174、2615／2494项在仅局部绑定归一后相同，字面量位置／指令保持；一条气轮Javadoc移除只适用最低档的材质限定，见[导航](LARGE_TURBINE_AUDIT.md)与[证明](verification/large-turbine-audit-20261021.json)。其余职责／压行整理继续，没有把排版计作玩法修复。

第六十六批展开AdvancedControllerRules 7→24行，以及LightningRodControllerBlockEntity的五层／柱、天气竞争、能量接口、生命周期与两端存档；原生视图85／86→188／192行。lightningDrain.sent改sentPackets，结构局部pos／required／pillar改partPos／requiredBlock／rodBlock，完整166／1505／1558项在限定绑定归一后保持，见[导航](LIGHTNING_ROD_AUDIT.md)和[证明](verification/lightning-rod-audit-20261021.json)。core说明不再泛称原版规则全部一致，其余名称／数值／声明／指令不动；旧未注册类仅盘点，未删公开实现类或新增兼容ID。

第六十七批整理LargeDynamoControllerBlockEntity、LargeDynamoControllerBlock、AxialGeneratorDefinitions、AxialStructureTransform和SharedTurbineStructure：展开压行方法／三重循环／平台读取，八个局部与lambda短名改为cellPos、parameters、firstOriginalId、offset及ticker参数。九份完整执行流与字面量位置保持，offset方法名不随变量归一，注释／指令不变，见[导航](LARGE_DYNAMO_AUDIT.md)及[回执](verification/large-dynamo-audit-20261021.json)。没有重命名公开API或修改材料表；其余压行、大职责边界及全面核查继续。

第六十八批整理LargeHeatExchangerControllerBlockEntity、LargeHeatExchangerControllerBlock、HotFluidFuelCycle、HeatExchangerData、HeatExchangerRules、SharedHeatExchangerStructure。结构局部／lambda、燃料候选、defaults与batchCount改为明确名字，展开压行、平台能力／保存及三重循环；十份完整执行流和字面量位置保持。未跟随局部名改包名context、原builder或Charge.batches组件；仅一条过强“原循环等价”说明限缩，许可证头／其它注释／指令保留，见[导航](LARGE_HEAT_EXCHANGER_AUDIT.md)及[回执](verification/large-heat-exchanger-audit-20261021.json)。未改用户材料或公开二进制方法签名，全面可读性队列继续。

2026-10-21 第六十九批：禁刷怪塔四份压行源码已展开，局部／ticker参数明确命名，七份完整执行视图保持；ControllerStructureLayouts的重复63格改读core布局，方法外两视图保持，内部以原／core／旧预览有序单元证明。不是只改文件名，也不把共享改动冒称完整词法不变，见[禁刷怪塔](VON_DA_GRAAGG_AUDIT.md)。其它压行与职责整理继续。

2026-10-21 第七十批：基岩钻机BE／实际OriginalBlock展开结构、产物与两版保存，局部／lambda明确命名；Neo NativeMachineTankData也从3→16行。五份完整执行视图保持，去掉“必须有矿”的过强说明，见[钻机](BEDROCK_DRILL_AUDIT.md)。遗留HorizontalDirectional类的公开扩展可达性另核，不因无仓库构造引用就宣称已完成死代码清理；其余可读性与全面队列继续。

2026-10-21 第七十一批：物流核心布局／输入、BE／Block及路由薄边界五份展开，tick块缩进与局部名称明确，重复LogisticsCoverHost import精确去一条。四处说明限定模拟软上限／无电复位／扫描绑定／需求值；八完整执行在该import去除与限定绑定后保持。见[物流状态](LOGISTICS_CORE_STATE_AUDIT.md)，路由、生命周期与其余源码整理继续。

2026-10-21 第七十二批：LogisticsCoverDefinition／Type展开共享定义与原生映射方法，Interaction展开Neo数据／拒绝条件和反馈，两段old／next改为previousValue／nextTargetSize；FluidRouter拆拥挤条件与选择表达式。四源码七完整执行流540、370／370、1720／1729、1518／1521在限定绑定归一后保持，三处说明纠正。交互最长行174降到119／122，用户材料表不动，见[物流搬运](LOGISTICS_TRANSFER_AUDIT.md)及[回执](verification/logistics-transfer-audit-20261021.json)；其余可读性与全面核查继续。

2026-10-21 第七十三批：LogisticsNetwork展开种子三循环／快照方法，距离a／b改为origin／target；LogisticsCovers展开数据与同步／掉落，循环i改为faceIndex；物流线BE展开两版保存。ExtenderSpec展开八定义，RelayRoutingRules由3→12行，保留66项完整执行；五源总八份完整执行和原始字面量位置保持，四处说明限定，用户材料不动，见[网络专题](LOGISTICS_NETWORK_AUDIT.md)和[回执](verification/logistics-network-audit-20261021.json)。全面可读性队列继续。

2026-10-21 第七十四批：LogisticsWireBlock展开Neo交互／连接／形状，局部neighborPos、updatedState、connectedHere／There、entityContext明确；renderer的be／vc／out改为blockEntity／vertices／surfaceOffset。Neo注册和客户端订阅展开，queued IDs lambda h改holder，两处职责说明纠正。四源六完整执行1462／1909、904／890、449、196及原始字面量位置保持，最长线275降161、注册307降123；用户材料表不动，见[客户端](LOGISTICS_CLIENT_AUDIT.md)及[回执](verification/logistics-client-audit-20261021.json)。其余可读性和全面核查继续。

2026-10-21 第七十五批：储罐BE、主阀Block、TankValveSpec、HollowTankStructure、MultiblockTankData共五源展开；限定be／f／ctx／ticker参数／all／p／容量临时量改具名，真实TankValveParameters.all()成员调用保持。十份完整执行及原始字面量位置、来源注释／指令另核，五处职责说明纠正，用户材料排版不动，见[储罐专题](MULTIBLOCK_TANK_AUDIT.md)及[回执](verification/multiblock-tank-audit-20261021.json)。完整可读性与原版核查继续。

2026-10-21 第七十六批：MultiblockPortBlock展开Neo交互／火回调／渲染／基础掉落，两处id改registryId；TankFlowRules展开委托。FluidHazards缩短总说明，明确注册依赖与外部气体近似。三源六完整执行保持，三处说明纠正／未归一字面量位置／其余注释指令另核，用户材料不动，见[危害专题](TANK_HAZARDS_AUDIT.md)。全面可读性目标继续。

2026-10-21 第七十七批：展开FluidAttachmentBlock的构造旋转／访问包装／Neo交互和注入分支、MultiblockTankTooltips的内容／防护拼接、MultiblockJadeProvider的状态／流体序列化；core OriginalTankTooltipData循环i改digitIndex，构造两个b绑定改bounds。四源七完整执行及原始字面量位置保持，一处接口职责说明纠正，用户材料表不动，见[工具与制作专题](TANK_TOOLS_RECIPES_AUDIT.md)和[回执](verification/tank-tools-audit-20261021.json)。全面可读性与原版核查继续。

2026-10-21 第七十八批：PaintSprayItem展开喷漆lambda／实体判断，两端17→98行，c／p仅在onItemUseFirst内改context／targetPos，保留已有Neo提示context。ComponentCoverFallbackRenderer展开事件和逐面绘制，Neo MultiblockClientSetup拆开渲染／颜色注册，core OriginalMultiblockTintPolicy展开switch；三处职责说明纠正，四源六完整执行76、940／942、526／532、547及未归一字面量位置保持。用户570条CompoundMaterials对齐和WoodMaterials修改保留，见[客户端专题](TANK_CLIENT_AUDIT.md)及[回执](verification/tank-client-audit-20261021.json)；全面可读性目标继续。

2026-10-21 第七十九批：PaintState9→42行、PaintingRules11→73行、Paintable3→10行，16色逐行列出，mix的a／b改firstRgb／secondRgb，restore的flag改paintedFlag；三共享颜色Mixin展开保存／更新／回调，两个cir改callback。六源九完整执行170、544、45、832／865、210／210、202／230及原始字面量位置保持，两处职责说明纠正／用户材料不动，见[喷漆专题](PAINT_STATE_AUDIT.md)及[回执](verification/paint-state-audit-20261021.json)。其余宿主与全面源码核查继续。

2026-10-21 第八十批：ExtenderBlockEntity展开覆盖板／保存更新／路由／控制／信号，SourceExtenderBlock展开两版工具交互和ticker，c／be分别限定方法内改control／blockEntity。清漆陈旧大说明改为实际目标和委托边界；PaintTargets／FixedConstructionColor缩进、WolfCollarAccessor和六项GrassSoilCatalog也已整理。七源十三完整执行557／557、770／770、412／412、81／81、2515／2587、1305／1575、133与未归一字面量位置保持，六说明纠正／用户材料不动，见[专题](PAINT_TARGETS_AUDIT.md)及[回执](verification/paint-targets-audit-20261021.json)。其它源码与全面原版核查继续。

2026-10-21 第八十一批：CapabilityRelayBlockEntity展开缓存／失效／内层包装，active与h限定区域改visited／targetHandler；EnergyRelayBlockEntity展开FE及19项GT转发，e／s改recipient／recipientSide，保留target方法及所有成员名。Neo StorageContainerCapabilities拆注册，e／be改event／blockEntity，注册表达式顺序不变。三源五完整执行1552／1453、1610／1511、902与字面量位置保持，唯一说明限定子类目标职责，材料未动，见[专题](EXTENDER_CAPABILITIES_AUDIT.md)和[回执](verification/extender-capabilities-audit-20261021.json)；全面队列继续。

2026-10-21 第八十二批：CoverSignalMixin展开宿主选择及三注入，cir改callback并纠正覆盖范围说明；ExtenderJadeProvider展开服务器数据／tooltip，block／data改sourceBlock／controlData；ExtenderCoverRenderer展开render，be／partial改blockEntity／partialTick。三源六完整执行515／515、535／535、139／139与字面量位置保持，成员／注册／调用／光照不变，材料未动，见[信号](EXTENDER_SIGNALS_AUDIT.md)和[回执](verification/extender-signals-audit-20261021.json)。全面可读性与原版核查继续。

2026-10-21 第八十三批：FilterBlockEntity展开保存／加载／模板循环，i／data／stack按限定区域改templateSlot／entryData／templateStack；FilterBlock的b／ctx改builder／context并展开两版交互／掉落／包装。FilterMenu拆构造／槽位，col改column；FilterScreen展开两版render／布局，partial／button改partialTick／clickedButton。四源八完整执行1355／1399、1242／1536、820／820、454／449与字面量位置保持，一说明限定主面选择旁路，用户材料不动，见[专题](FILTER_BLOCKS_AUDIT.md)及[回执](verification/filter-blocks-audit-20261021.json)。全面源码标准化与对齐继续。

2026-10-21 第八十四批：FluidDisplayBinding展开两版序列化／查找，id／item／out／tag改fluidId／carrierItem／displayStack／payloadData；两版FluidItem／FluidDisplayItem展开handler等方法，f改resolvedFluid；ManualToolRules只展开方法。四源五执行421／697、1297、1510、129及字面量位置保持；五说明准确限定饮用载体和无玩家检查的source handler，成员／数值／序列化／玩法不变，用户材料不动，见[专题](FILTER_FLUID_TOOLS_AUDIT.md)及[回执](verification/filter-fluid-tools-audit-20261021.json)。全面标准化／对齐继续。

2026-10-21 第八十五批：core ContainerShapeLike／PipeFormLike展开接口；共享FilterRules展开短条件及映射，限定方法中参数／模式变量／循环局部名改为具体用途。LogisticsItemFilter展开条件并纠正两处说明。四源六执行21、21、514／505、235／235和字面量位置／其它注释／指令保持，材料表未动，见[专题](FILTER_PREFIX_MEMBERSHIP_AUDIT.md)及[回执](verification/filter-prefix-membership-audit-20261021.json)。全面标准化／原版核查继续。

2026-10-21 第八十六批：core LogisticsRoutingRules展开短条件、明确处理器／轮次／结果／等级／数量局部名，DisplayRules展开判断并明确参数；共享LogisticsItemRouter展开记录／条件，宿主／目标及转换／槽位循环使用描述性名称。核心BE仅纠正公开单轮方法与普通ticker门槛说明。四源六完整执行716、234、2240／2241、3094／3135及字面量位置／其它注释／指令保持，材料未动，见[专题](LOGISTICS_SCHEDULING_AUDIT.md)和[回执](verification/logistics-scheduling-audit-20261021.json)。全面标准化／原版核查继续。

2026-10-21 同步前可读性提交：展开物流大容量存储实体及两类方块入口、Neo StorageRegistries注册／构造流程，明确eventBus／materialSpec／registryId等局部名，两个显式import缩短相同类型；普通存储Neo说明修正已完成的137，注册说明不再称物流变种未接。五源九执行344／378、291／291、170／170、2053／2128、855在列明局部名和两个类型import归一后保持；原始字面量值、恢复类型拼写后的字面量位置、其它注释／Stonecutter指令不变，见[回执](verification/logistics-storage-readability-20261021.json)。材料不动，未构建／游戏，直接存储核查及全面队列继续。

2026-10-21 第八十七批：MassStorageBlockEntity展开模板／模式getter、提取后更新、匿名物品能力方法与失效清理，维护源374→475行；纯空白调整，无名称／import归一。Forge2577／Neo2354全部有序执行词法、各60个字面量原始位置、注释及Stonecutter条件不变。同步前五源九视图另复核，未计作本批新修改；材料摘要保持，见[直接存储专题](LOGISTICS_DIRECT_STORAGE_AUDIT.md)和[回执](verification/logistics-direct-storage-audit-20261021.json)。未修玩法／编译／游戏／构建，全面目标继续。

2026-10-21 第八十八批：展开LongDistEndpointBlockEntity、LongDistanceTransformerBlockEntity、两类端点方块与LongDistPipeBlock，共520→980维护行；两实体changed中e／current／previous改observedEntry／currentState／previousState，补两职责注释。十完整执行1658／1686、2476／2502、1280／1439、532／532、1426／1585及原始字面量位置／其它注释／条件／版权头保持，未改import、调用或玩法，见[长距离专题](LONG_DISTANCE_LIFECYCLE_AUDIT.md)及[回执](verification/long-distance-lifecycle-audit-20261021.json)。用户材料保持，全面标准化与原版核查继续。

2026-10-21 第八十九批：UsbDataRules／UsbDataMedia／UsbSwitchBlockEntity三源395→579行，展开级别／组件／控制／保存；tier方法id／n改registryPath／tierDigit。另只改GTMaterialDataRecipes空闲查找职责说明，实体注释准确标明过宽重置。四源七执行277、886／1038、2034／1888、3762／3859在限定名称归一后保持，字面量位置／其它注释／条件另核，未改玩法或用户材料，见[USB专题](USB_SWITCH_AUDIT.md)与[回执](verification/usb-switch-audit-20261021.json)。全面目标继续。
