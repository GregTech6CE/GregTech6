# 活动源码结构与可读性盘点

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
