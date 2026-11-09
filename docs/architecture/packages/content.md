# content：定义、目录与游戏内容

返回[包索引](README.md)。core 的 content 描述“有什么”，Minecraft 的 content 将其变为游戏里的具体对象。前者可以包含声明性的规则计算；后者确实包含 BlockEntity 与游戏逻辑，不能仅因包名就把它们当不可变数据。

## core：定义与内容表

入口为 [core/gregtech/content](../../../core/src/main/java/gregtech/content)。下表覆盖当前主要子包；运行状态和通用执行机制优先放 engine。

| 包 | 应当承担 | 代表入口/说明 |
| --- | --- | --- |
| `content` | 内容层总体职责说明 | `package-info.java` |
| `content.material` | 材料目录入口、物性规则和稳定符号门面 | `MaterialDefinitions`、`Materials`；读取符号不触发注册 |
| `content.material.catalog` | 元素、化合物、矿石、石材、木材、特殊材料和组的规范声明 | `ElementMaterials`、`CompoundMaterials`、`GroupMaterials`；每项材料事实只在这里维护 |
| `content.material.generated` | 既有 GT6 字段符号门面 | `ElementMaterials`、`CompoundMaterials`；引用规范目录中的已安装对象 |
| `content.data` | 原版参数、跨领域内容事实与标识表 | `FluidCatalog`、`MaterialGroups`；仅跨领域事实放这里 |
| `content.data.generated` | 尚留存的兼容查询门面和历史字段映射 | `MaterialForms`、`MaterialProcessingTargets`；权威材料事实位于 `content.material.catalog` |
| `content.recipe` | 具体配方行、合成目录与内容规则 | `AutoclaveRecipeRows`、`ClayMoldCatalog` |
| `content.recipe.generated` | 提取/转换后的原 GT6 配方数据 | `GTAlloyTable`、`GeneratedRecipeSink` |
| `content.machine` | 基础机器种类、材料、面与原版参数 | `BasicMachineCatalog`、`OriginalBasicMachineRules` |
| `content.multiblock` | 原版多方块控制器参数与布局相关定义 | `OriginalAdvancedControllerData` 等；不访问世界 |
| `content.energy` | 锅炉、轴、引擎、电池等具体设备参数 | `BoilerCatalog`、`AxleCatalog`、`ChemicalBatterySpec` |
| `content.fluid` | 流体目录与原版显示定义 | `FluidDefinitions`、`OriginalFluidDisplayRules` |
| `content.transport` | 物品管、漏斗、面板等种类和传输内容规则 | `ItemPipeCatalog`、`HopperCatalog` |
| `content.transport.fluid` | 流体管、物流储罐和木桶规格 | `FluidTransportDefinitions`、`LogisticsTankSpec` |
| `content.logistics` | 物流核心布局、覆盖板、过滤策略和长距定义 | `LogisticsCoreLayout`、`FilterPolicy` |
| `content.storage` | 容器种类、保险箱锁、垃圾与合成存储规则 | `ContainerStorageRules`、`SafeLockRules` |
| `content.block` | 普通方块的材料、构造、重量与行为定义 | `ConstructionRules`、`BlockMaterialWeights` |
| `content.block.stone` | 石材种类、变体、标志与重量 | `StoneType`、`StoneVariant` |
| `content.block.wood` | 木种定义 | `WoodSpecies` |
| `content.cover` | 覆盖板类型规则与几何定义 | `ComponentCoverRules`、`CoverPlateGeometry` |
| `content.tool` | 砧、工具、锻造、冲压的目录和规则 | `AnvilForgingCatalog`、`AutomaticToolRules` |
| `content.nuclear` | 燃料棒、冷却剂与核合成目录 | `ReactorRodCatalog`、`ReactorCoolantRules` |
| `content.hazard` | 材料危害与防护服内容参数 | `MaterialBlockHazards`、`HazmatSuitCatalog` |
| `content.worldgen` | 矿脉、岩层、地表等生成定义与纯规则 | `GTOreVeins`、`GTStoneLayersGen` |
| `content.worldgen.center` | 原点世界、街道与生物群系定义 | `OriginWorld`、`SourceStreets` |
| `content.worldgen.dungeon` | 地牢布局规则 | `DungeonLayoutRules` |
| `content.plant` | 植物种类、生长、浆果与花卉目录 | `BerryBushCatalog`、`BushGrowthRules` |
| `content.bumble` | 蜜蜂种类、遗传、合成与槽位定义 | `GTBumbleSpecies`、`BumbleGenomeRules` |
| `content.food` | 食物/饮料数值、瓶装、罐头与辐射行 | `FoodNutritionRows`、`FoodStatisticRows`、`BottleFillingRows` |
| `content.book` | 书籍、书架、材料字典与附魔书目录 | `GTBookContent`、`BookCraftingCatalog` |
| `content.loot` | 战利品、掉落与抽取规则 | `LootChestCatalog`、`MobDropRules` |
| `content.creative` | 创造栏和配方查看器的内容排序目录 | `CreativeTabCatalog`、`EmiCatalogOrder` |
| `content.registry` | 既有内容目录/兼容门面 | `GTMultiItemsGen`、`GTStorageMetals`；原生注册在游戏层 |

## Minecraft：具体内容与注册

入口为 [minecraft/gregtech/content](../../../minecraft/src/main/java/gregtech/content)，两个版本节点还包含同包实现与补充。

| 包 | 应当承担 |
| --- | --- |
| `content.block` | 具体 Block、放置/形状/交互；子包 `energy`、`inventory`、`machine`、`misc`、`plant`、`sensor`、`stone`、`tool`、`wood` 按内容类型分组 |
| `content.blockentity` | 世界中有状态的实例、tick、持久化及游戏资源连接；子包 `energy`、`inventory`、`machine`、`misc`、`sensor`、`tool`，Forge 节点另有 `behavior` |
| `content.item`、`content.item.behavior` | 具体物品与组合行为，处理游戏栈和实际交互 |
| `content.entity` | 实际世界实体，如材料箭矢；实体注册沿既有注册入口，渲染留在客户端 |
| `content.registry` | 各内容族的原生注册表与注册方法，如 `GTItems`、`GTBlocks`、`GTBlockEntities`；遵循生命周期，避免过早取值 |
| `content.recipe`、`content.recipe.generated` | 将定义行转成游戏配方、注入/重载与生成规格解析；如 `MachineRecipeBootstrap`、`GeneratedRecipeResolver` |
| `content.data`、`content.data.generated` | 内容数据的原生适配和既有兼容数据；新代码优先放明确领域 |
| `content.machine` | 基础机器定义在游戏侧的物化和调参，如 `BasicMachineDefinitions` |
| `content.multiblock` | `ControllerDeclarations` 绑定具体主控，`ControllerStructureLayouts` 从相同声明产生预览；布局与设备规格 |
| `content.material` | 原版物品成分与材料统一的加载 |
| `content.energy` | 能源设备的具体游戏接入；节点的 `content.fluid` 承担原生流体对象 |
| `content.transport`、`content.transport.fluid` | 管道定义物化、物品转移适配及流体 tick 阶段 |
| `content.cover`、`content.logistics` | 覆盖板宿主/交互/信号、`CoverStore` 共享存储与生命周期，及物流核心结构、路由接入 |
| `content.tool` | 工具和砧的游戏配方、工作输入、几何及交互适配 |
| `content.nuclear`、`content.hazard`、`content.damage` | 核网络、辐射、冷却、危险效果与游戏伤害类型 |
| `content.worldgen` | Feature、放置与原生生成注册；`center` 为原点设施与生成事件，`dungeon` 为具体地牢房间与内容 |
| `content.world` | 世界级持久数据，如 `GarbageData`；不是世界生成目录 |
| `content.plant`、`content.bumble` | 植物及蜜蜂的原生内容与世界交互 |
| `content.food`、`content.book`、`content.loot` | 游戏食物、书籍、战利品对象与注入/显示数据 |

`misc` 是既有内容分组，不是任何新业务的默认落点。新增内容先找明确领域；同类逻辑若可共享，应抽取/复用规则，而非在多个 BlockEntity 中复制。

## 从定义到可用内容

先检查 core 目录是否已有定义，再定位游戏注册及 bootstrap。材料可能涉及前缀、形态、流体、配方和渲染；机器可能涉及 Block、BlockEntity、注册、结构、GUI、同步和资源。只补一张表通常不足以完成内容接入。

涉及旧 GT6 数据时保存可追溯来源、单位转换与差异，提炼到 [integration](../../integration/README.md)。不要因 `Original*` 名字而声称完整还原。生成表来源缺失时先记录局限，参见[生成物说明](../resources.md)。验收与自动化测试范围见下。

## 验收

- 新内容按[源码分层](../README.md#第一层源码与依赖)检查依赖，跨版本输入按[跨版本指南](../versions.md)处理。
- 材料、配方、机器、能源、物流、流体、世界生成、工具与食物的单位、概率、注册 ID、序列化键、版本路径与兼容影响必须说明；配方 JSON、生成表与语言键是生产输入，不能统一忽略。
- 机器、多方块、能源、物流、流体与覆盖板的转移要验证失败路径、守恒、方向、区块卸载与重载；机器 tick 与交互还要在客户端与专用服务端分别确认。
- 资源改动按[资源指南](../resources.md)与[组装检查](../../development/validation/checks/assemble.md)核对版本路径、引用和发行包内容。
- 修改配方匹配或产出后，运行 JUnit 静态配方案例与对应节点的 GameTest 配方案例；修改机器、能源或流体行为后补对应 GameTest 或游戏内验证。

## 自动化测试范围

- JUnit
  - 配方 JSON 与 core 表的静态案例；配方行、成本、并行、输出空间与序列化 roundtrip。
  - 材料模型、成分、形态与前缀、统一化与重复 ID；材料相关配方匹配。
  - 能源类型转换、门控与存储状态；物流路由、批量存储与槽位布局。
  - 矿脉、岩层、地牢布局与原点世界的纯规则；矿脉模式持久化与旧数据迁移。
  - 砧配方成本、工具磨损、坩埚热量与物料比例、食物数值与瓶装返还、战利品概率。
  - 长流体存储、物性与管道安全；核燃料棒、冷却与辐射的纯计算。
- GameTest
  - 配方加载、合成匹配、tag 成员、`RecipeMap` 注册与机器配方执行。
  - 方块实体 tick、输入消耗、输出阻塞、能量不足与错误流体；多方块成形、失效与恢复。
  - 线缆、管道、轴、电池、覆盖板与储罐的实际传输、断开与方向变化。
  - 材料物品/方块注册与统一化；矿脉或结构的注册存在性。
- 现有测试集
  - `MaterialBaselineTest` 与共享 `MaterialBaselineTests` 提供材料源表引用审计、独立 JVM 快照、测试材料 SPI、成分与回收／分解及真实重载入口；观察与正确性依据分开，见[材料基线](../../integration/material-baseline.md)。
  - core 静态 TSV 与 GameTest TSV 由对应 JUnit/GameTest 维护；用例覆盖物品管道路由与调度回归。
  - `core/src/test` 的材料目录、标签名翻译、熔融浇筑与配方引擎测试；`api/common` 的材料、机器、能源模型测试。
  - 机器 tick、多方块、能源网络、物流、世界生成完整流程等仍是待补范围。
