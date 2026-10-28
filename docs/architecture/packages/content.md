# content：定义、目录与游戏内容

返回[包索引](README.md)。core 的 content 描述“有什么”，Minecraft 的 content 将其变为游戏里的具体对象。前者可以包含声明性的规则计算；后者确实包含 BlockEntity 与游戏逻辑，不能仅因包名就把它们当不可变数据。

## core：定义与内容表

入口为 [core/gregtech/content](../../../core/src/main/java/gregtech/content)。下表覆盖当前主要子包；运行状态和通用执行机制优先放 engine。

| 包 | 应当承担 | 代表入口/说明 |
| --- | --- | --- |
| `content` | 内容层总体职责说明 | `package-info.java` |
| `content.material` | 材料集合、补充定义、形态修正 | `Materials`、`MaterialDefinitions` |
| `content.material.generated` | 元素、化合物、矿石、石材、木材目录 | `ElementMaterials`、`CompoundMaterials`；查来源再更新 |
| `content.data` | 原版参数、跨领域内容事实与标识表 | `FluidCatalog`、`MaterialGroups`；仅跨领域事实放这里 |
| `content.data.generated` | 从原数据提取的材料事实等兼容目录 | `GT6Materials`、`MaterialCompositionData`；不是临时文件 |
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
| `content.food` | 食物/饮料营养、瓶装、罐头等内容行 | `FoodNutritionRows`、`BottleFillingRows` |
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
| `content.multiblock` | 具体控制器结构、世界结构布局与设备规格，如 `ControllerStructureLayouts` |
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

涉及旧 GT6 数据时保存可追溯来源、单位转换与差异，提炼到 [integration](../../integration/README.md)。不要因 `Original*` 名字而声称完整还原。生成表来源缺失时先记录局限，参见[生成物说明](../resources.md)。验证包括受影响平台编译、注册/资源检查以及相关游戏场景。
