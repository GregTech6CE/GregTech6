# 07 补充：recipe 包中的材料事实审计

本页给出 [07 清理计划](07-cleanup-and-acceptance.md)首轮 Reviewed 清单之外的具体对象。源码核对起点为 master `f096a79ee`（2026-11-12），2026-11-13 已完成下表中的材料事实与固定成分迁移；本页保留原检查和边界，用于追溯新入口和仍需 07-E／07-F 验证的范围。

本轮对 core 与共享 Minecraft 的 `content.recipe` 共 258 个 Java 文件、两版本节点的同包共 14 个 Java 文件做文件清点和模式搜索，对疑似项阅读声明、方法和调用方；另外扫描两层 `engine.recipe` 共 44 个 Java 文件的同类特征。检索覆盖材料 ID／名称名单、属性与 target 表、相态单位、成分写入和默认值指纹。该范围不等于对所有配方逐条核验，也不包含 JSON、脚本及第三方模组的完整运行期审计。

迁移后逐表复核确认：挤压源表 734 行的 payload 完全一致；回收 seed、研钵正负集合、洗矿行和透镜角色没有在复制时改值；新增 process 投影后，炉授权 198 条与分级宝石 2 条全部命中，其中 22 条源名需经 `GTMaterial.sanitize` 才能对应 `Lithium6`、`RedSand` 等规范名。4 个 Hexorium 透镜角色因当前没有该可选材料而保持未投影，不会被当成普通材料身份。

职责判断沿用 [material 与 recipe 的分界](07-cleanup-and-acceptance.md#material-与-recipe-的分界)。以下建议不增加已支持的公共 API；目录缺少表达能力时，在实施切片中设计声明并验证消费者。

## 一、需要归入材料声明的新增对象

| 当前对象与证据 | 材料事实与当前消费者 | 具体清理方式 |
| --- | --- | --- |
| [GenerifyingMaterialFacts](../../../core/src/main/java/gregtech/content/material/catalog/GenerifyingMaterialFacts.java) 的 `TARGET_IDS` | 143 对材料 source／target ID；Minecraft `GenerifierRecipeFamily.target()` 据此决定转换目标 | 已迁入 `GENERIFICATION_TARGET` process；配方的适用 prefix、工时、动态查询与显式配方优先级仍在 [GenerifyingRules](../../../core/src/main/java/gregtech/content/recipe/GenerifyingRules.java) |
| [SourceExtrusionMaterialFacts](../../../core/src/main/java/gregtech/content/material/catalog/SourceExtrusionMaterialFacts.java) 的 `DATA`／`Profile` | 734 行材料挤压能力、简单挤压、可锻性和 forging target／amount；`SourceExtruderRecipes`、`MaterialPrefix`、`MaterialPrefixes` 消费 | 已迁入 `SOURCE_EXTRUSION` process；[SourceExtrusionRules](../../../core/src/main/java/gregtech/content/recipe/SourceExtrusionRules.java) 只保留数量的取整、工时和原生查询门面 |
| [SourceMacerationMaterialFacts](../../../core/src/main/java/gregtech/content/material/catalog/SourceMacerationMaterialFacts.java) 的 `MORTAR_YES`／`MORTAR_NO` | 两套研钵能力 ID 集合；`SourceOreMacerationAdapter` 使用 | 已迁入 `SOURCE_MACERATION`；适配器仍保留 UNKNOWN、外部形态和原生输入证明 |
| [SourceWashingMaterialFacts](../../../core/src/main/java/gregtech/content/material/catalog/SourceWashingMaterialFacts.java) 的 `Binding.tags` | 按材料声明 MERCURY／PERSULFATE／FIRESTONE 三种通用洗矿能力；`SharedOreWashingRows` 查询 | 已迁入 `SOURCE_WASHING`；具体反应物、用量、产出与概率留 recipe |
| [SourceOreCrushingMaterialFacts](../../../core/src/main/java/gregtech/content/material/catalog/SourceOreCrushingMaterialFacts.java) 与 [OreHostFacts](../../../core/src/main/java/gregtech/content/block/stone/OreHostFacts.java) | 特殊粉碎／Hexorium 分类与宿主岩石、脉石、破碎配对 | 材料能力进入 `SOURCE_ORE_CRUSHING`，宿主关系进入石材内容声明；适配器保留原生所有权检查 |
| [SourceLensMaterialFacts](../../../core/src/main/java/gregtech/content/material/catalog/SourceLensMaterialFacts.java) 的 `ROLES` | 57 条材料透镜颜色角色；`ElectronicsRecipes`、`LaserStoneRecipes` 消费 | 已迁入 `SOURCE_LENS_ROLE`；`SourceLensRecipeItems` 只解析已注册原生物品 |
| Minecraft [BathMaterialRecipes](../../../minecraft/src/main/java/gregtech/content/recipe/BathMaterialRecipes.java) 的 `NOT_ANY_WOOD_DUST` 与 [TreatedWoodRecipes](../../../minecraft/src/main/java/gregtech/content/recipe/TreatedWoodRecipes.java) 的 `NOT_UNTREATED_WOOD` | 两处分别用 WOOD 属性加 `PetrifiedWood` 排除来近似 `AnyWood`／`AnyUntreatedWood`；已有 `MaterialGroups.Wood`／`WoodUntreated` 与规范组目录 | 核对全部现有成员与附属扩展后改读对应组；纸浆配方额外接受 Bark／Paper、处理木材的转换输出仍留 recipe。组替换必须比较行为差异，不能只把条件机械改写后认定等价 |

其中两个大型目标表现已拆为材料事实和 recipe 模板。首轮 Reviewed 类清单及迁移结果仍见 [07 主计划](07-cleanup-and-acceptance.md#本轮已清理的-reviewedsource-清单)。

## 二、混合类中需要拆出的事实和绑定

这些类承担有效的配方职责，应按字段／方法拆分，不能整类迁到 material。

| 当前对象 | 应收敛的部分 | 仍应保留的职责与删除条件 |
| --- | --- | --- |
| Minecraft [SharedPurifiedCentrifugeRows](../../../minecraft/src/main/java/gregtech/content/recipe/SharedPurifiedCentrifugeRows.java) | 六条相态记录与七条显式无默认相态记录 | 已迁入 `PurifiedCentrifugeMaterialFacts` 的两个材料 process；原生注册字段／所有权、离心槽位、概率、重载发布和兼容分支仍留 recipe |
| [MaterialFluidUnitFacts](../../../core/src/main/java/gregtech/content/material/catalog/MaterialFluidUnitFacts.java) 与 core `engine.recipe.chem.MaterialFluidBinding` | 材料相态／单位／流体身份绑定，后者从材料视图和流体目录建立索引 | 绑定事实进入 `MATERIAL_FLUID_UNIT` process；`UnitConversion` 只保留换算机制 |
| Minecraft [SourceThermalNativeAdapter](../../../minecraft/src/main/java/gregtech/content/recipe/SourceThermalNativeAdapter.java) 的 `nativeBinding()` | Glass、Blaze、Plastic、Ender、Redstone、Lava、CFoam、蜡及熔盐等原生字段／ID | 精确字段和单位从 `MaterialFluidUnitFacts` 查询；特殊原生持有检查与配方适配仍在游戏侧 |
| core [SpecialProcessData](../../../core/src/main/java/gregtech/content/recipe/SpecialProcessData.java) 与 `SpecialProcessMaterialFacts` | 酸洗 `Phase` 的材料选择、固定 target/reagent 规则和动态锰规则 | `Phase` 的原生字段、相态和 1000 mB/U 归入 `MaterialFluidUnitFacts`；酸洗材料列表、共同反应物、配比和尾产物通过 API 1.9 的 `SpecialProcessValue.Target／Reagent` 挂到 typed process。旧 `SourcePurifiedAcidData`、`SourcePurifiedAcidRules` 与 `SharedPurifiedAcidRows` 已删除 |
| core [SourceRollingWireRules](../../../core/src/main/java/gregtech/content/recipe/SourceRollingWireRules.java) 的 `physicalWire()` | Netherite／Naquadah 的额外线材族准入 | 已迁入 `SOURCE_ADDITIONAL_WIRE_FAMILY`；轧制／拉丝模板、工时与实际线材族注册仍留 recipe |
| Minecraft [MaterialFormConversionRecipes](../../../minecraft/src/main/java/gregtech/content/recipe/MaterialFormConversionRecipes.java) 的 `compressor()` | Ice 的 `dust → plateGem` 排除 | 已迁入 `COMPRESSOR_DUST_TO_GEM_PLATE`，与筛矿高级宝石限制保持独立；装箱、拆箱、压缩模板仍留 recipe |
| core `OreHostFacts` 的 `HOSTS`／`BROKEN_SOURCE_PAIRS` | 宿主岩石、矿石 prefix、4U 脉石和破碎矿块表示关系；`SourceOilSandRules` 也消费 | 已位于石材内容声明；原生 host ID 解析和机器对脉石的产出策略仍留 recipe；不写入矿物化学组成 |

`SourceLatheRules`、`SourceCutterRules`、`SourceFormingRules` 和 `SourceRollingWireRules` 已共同读取包外 `content.material.SourceLatheMaterialProfiles`。这部分结构可复用：recipe 保留工艺模板。后续还要审计该 material helper 与规范 catalog、`MaterialWorkability` 的事实重叠，不能仅因为已经移出 recipe 就宣称材料事实已只维护一处。

## 三、应归入游戏侧物品成分接入的对象

这是“物品包含什么材料”的事实，目标是 Minecraft `content.material`／现有物品领域声明；不是把这些物品配比写进 core 材料化学组成。

| 当前对象 | 证据与迁移范围 | 必须保留的边界 |
| --- | --- | --- |
| [PanelMaterialCompositions](../../../minecraft/src/main/java/gregtech/content/material/PanelMaterialCompositions.java) | 提交基础／彩色／增强混凝土和面板成分，维护回收物品集合 | 已归 `content.material`；`RECIPE_REVERSE` 来源、声明 ID、优先级和回收消费者保持 |
| [TransportMaterialCompositions](../../../minecraft/src/main/java/gregtech/content/material/TransportMaterialCompositions.java) | 提交管道、储罐、物流部件成分及 prefix 绑定 | 已归 `content.material`；规格仍由 transport 领域维护 |
| `DiggableRecipes.registerMaterials()`、`RegisteredWoodSurvivalRecipes.registerMaterials()`、`VanillaWoodProcessingRecipes.registerMaterials()` | 黏土块／球／泥炭、树种、树皮、原木、木板、楼梯和台阶成分 | 已迁到 `MaterialItemCompositions` 与 `WoodMaterialCompositions`；实际加工／合成配方仍留原类，材料量、树皮复合成分和 JSON 显式覆盖来源不变 |
| [SingleItemSourceRecipes](../../../minecraft/src/main/java/gregtech/content/recipe/SingleItemSourceRecipes.java) 中 `register()`／`compounds()`／`meat()` 等成分写入 | dry bark、compound、肉条等固定成分 | 已提前交给 `MaterialItemCompositions`；原版炉、坩埚展示和食物加工路线保留 |
| [CapsuleCellRecipes.registerRecycling()](../../../minecraft/src/main/java/gregtech/content/recipe/CapsuleCellRecipes.java) | 空胶囊 1/9U 成分 | 已在 `MaterialItemCompositions` 安装；回收端只读取有效成分并发布配方，空／满容器状态和共享静态工时保持 |

迁移后，`MaterialItemCompositions` 不应再为安装固定成分而调用 recipe 生产类；配方发布也不应偷偷创建固定材料事实。接线要在显式成分关闭前完成，并保留当前来源优先级、覆盖关系和注册后才访问原生物品的时点。

## 四、经区分应继续留在 recipe 的对象

| 对象 | 保留原因与边界 |
| --- | --- |
| `GTChemRecipes` 的铝土矿高压釜、`SpecialProcessData` 的 fixed families | 明确反应物、流体、比例和产物的特殊化学工艺；共用材料不等于材料属性。target／reagent 规则进入 API 1.9，重复身份／单位绑定仍由材料目录提供 |
| `SourceAlloyRecipes`、`SourceOilSandRules`、`SourceOreSiftingRules.specialRows()`、`SourceOreMagneticRules.blockRows()` | 显式合金、油砂事件、特殊筛矿和固定磁选路线；输入／输出／概率就是配方内容，不因含材料名单而整体材料化 |
| `TechnologyIngredients.PISTON_METALS`、`craftingRedstone()` | 工艺局部的原料替代集合和形态选择。可以声明为配方专用组，不能由此推导全局“可做所有机械零件”能力，也不能合并不同语义的 `Any*` 组 |
| `PolarizerMaterialRecipes` 的材料对、`BathMaterialRecipes` 的材料转化与流体、`UtilityProcessingRecipes` 的木材提取产率 | 当前表达具体工艺的输入／输出或用量；应保留为配方关系。若以后确认为跨消费者的通用 target，可另行收敛声明，不能仅凭材料名称硬编码认定应全局化 |
| `SourceCentrifugeOptionalBindings` 及可选物品输出 | 版本、提供模组和实际注册项决定的外部集成适配；不是材料身份或加工能力。保留缺失／未知区别及未装模组时的类加载隔离 |
| `MaterialRecoveryRules`、`SourceThermalInputRules`、`SourceThermalRecyclingRules`、各 `*RecipeRows` 的通用机器模板 | 描述配方族、工时、形态数量和槽位策略。只把重复材料事实移出；纯换算机制是否归 engine 按既有分层另行整理，不将整套生成器搬进 material |

## 五、实施顺序与验收补充

1. 通用目标、挤压、透镜、研钵、洗矿、相态单位和固定成分写入已完成迁移。
2. `MaterialWorkability`、`SourceLatheMaterialProfiles` 等混合对象仍需在 07-E 定向审计中逐字段裁决去留；特殊处理系列已迁移到 API 1.9，不再保留酸洗专用声明 API。
3. 对局部工艺例外继续按“材料加工声明”或“显式配方声明”裁决；不能用全局 flag 扩大效果。

源码中的数量注释也需要核实：`SourceOreMacerationFacts` 注释写 589 个正例／997 个身份，实际集合为 588 个正例加 408 个负例；`SourceOreWashingData` 注释中的 59／159 等口径与实际 142 个身份行、54 个含正例 tag 的身份不一致。酸洗相态迁移时确认实际枚举为 17 个，旧注释的 18 已修正。

实施时在 [07 的清理专项断言](07-cleanup-and-acceptance.md#5-验收初始化查询运行与重载)之外补以下范围：

- 简化目标：动态查询与枚举得到同一目标，显式配方仍优先，未知／附属身份不误套 ID 表。
- 挤压：锻造目标和比例、简单／热挤压工时、依赖 smithable 的形态注册同时验证。
- 透镜：颜色角色与材料显示颜色独立，激光石材和电子配方使用同一角色声明。
- 木材组：比较两组完整成员、PetrifiedWood、处理／抛光木材以及附属成员；纸浆特许 Bark／Paper 不丢失。
- 物品成分：不生成任何加工配方时固定成分仍可查；重新发布配方不追加固定声明；树皮、楼梯、面板、运输容器和空／满胶囊的数量与状态不回归。
- 相态与单位：核对汞 144 mB/U、显式熔盐、气体／普通液体和 source NONE；连续重载、外部覆盖与缺绑定保持可区分。

本页没有裁决任何注册项删除或 GT6 行为变化。实际发生行为变更时，仍按主计划提供来源、兼容影响、必要的 parity 记录和双平台验证结果。
