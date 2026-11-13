# 07：清理与双平台整体验收

本计划在数据与执行路径已收敛后移除旧实现，审计无消费内容，完成用户可见行为与双平台验收。状态：内部清理已实施；[02](02-material-registration.md)—[06](06-decomposition.md) 已合入 master。07-A—D 的首轮清理已完成并通过 core 测试与双平台编译；07-E 已完成文档点名的高优先级形态定向审计，但完整注册项清单和 07-F 最终验收仍未完成；[08](08-material-data-api.md)-D—E 也尚未实施，因此不能把全部 07 标为完成。

本页原清单按 master `f096a79ee`（2026-11-12）核对源码。2026-11-13 已完成首轮内部迁移：材料事实归入 `content.material.catalog` 的类型化 process 规则，运行时消费者改经材料查询，recipe 包只保留配方模板、特殊工艺和原生适配。下文保留旧类名用于追溯删除对象；实际新入口见“本轮实施结果”。

## 本轮实施结果

| 原入口 | 新权威入口 | 已迁移消费者 | 结果 |
| --- | --- | --- | --- |
| `ReviewedOreSourceProfile`、`ReviewedMagneticMaterials`、`ReviewedSiftingGemForms` | `ReviewedSourceMaterialFacts` | `OreSourcePolicy`、`FurnaceSmeltingRules`、`SourceFurnaceNativeAdapter`、`SourceGemGradeResolver`、`OreProcessingRecipeLoader` | 旧类删除；磁性正例写入材料 `MAGNETIC` 属性，其余事实写入 `SOURCE_*` process |
| `ReviewedOreInputProfile`、`ReviewedOreShredderFacts`、`ReviewedCrushedFormGeneration` | `ReviewedOreMaterialFacts`、`SourceOreFollowonAdapter`、`SourceCrushedFormAdapter` | 非矿延续、研钵、粉碎、洗矿与输出证明 | 重复资格和逐材料常量删除，原生所有权检查保留 |
| `ReviewedCinnabarFacts`、`ReviewedOreFluidBindings`、`ReviewedOreGeneratedFluids`、`ReviewedCinnabarThermalRecipes` | `ReviewedOreMaterialFacts`、`MaterialFluidUnitFacts`、`SourceCinnabarFollowonAdapter`、`SourceGeneratedFluidAdapter`、`SourceDustThermalRecipes` | 朱砂后续路线、蒸汽裂解、洗矿、离心和 Ti／Fe 热回收 | 朱砂局部蜡集合移入蒸汽裂解 recipe 自身；流体身份与 recipe 发布分开 |
| `SourceRecyclingFacts` | `SourceRecyclingMaterialFacts` 的 `SOURCE_RECYCLING` process | 炉、粉碎、洗矿、离心、酸洗和热回收适配 | 数据归入材料目录；运行时只经 `MaterialFactKeys` 读取，Water／Lava 的仅目标上下文也随 process 发布 |
| `GenerifyingRules.TARGET_IDS` | `GenerifyingMaterialFacts` | `GenerifierRecipeFamily` | 目标由材料 process 提供；prefix 条件和工时仍留在 recipe 同名类 |
| `SourceExtrusionRules.DATA` | `SourceExtrusionMaterialFacts` | `SourceExtruderRecipes`、`MaterialPrefix`、`MaterialPrefixes` | 挤压能力、锻造目标和可锻性归入材料事实；数量计算与配方向 recipe 门面保留 |
| `SourceOreMacerationFacts`、`SourceOreWashingData`、`SourceOreCrushingFacts` | `SourceMacerationMaterialFacts`、`SourceWashingMaterialFacts`、`SourceOreCrushingMaterialFacts`、`OreHostFacts` | 研钵、洗矿、Crusher／Hammer、OilSand | 材料能力与宿主岩石事实分开；Hexorium 产物策略仍留 recipe |
| `SourceLensRoles.ROLES` | `SourceLensMaterialFacts` | `ElectronicsRecipes`、`LaserStoneRecipes` | 透镜颜色角色进入材料 process；原生物品解析留在 `SourceLensRecipeItems` |
| `OriginalMaterialFluidUnits`、`SourcePurifiedAcidData.Phase` 的绑定字段、`PurifiedCentrifugeMaterialFacts` 的重复流体字段 | `MaterialFluidUnitFacts` | 分解、酸洗、离心、热回收、洗矿、化学解析 | 流体身份、相态和单位只由 `MATERIAL_FLUID_UNIT` 提供；配方枚举保留材料选择与工艺配比，不再保存第二份原生字段／单位 |
| `PanelMaterialRegistration`、`TransportMaterialRegistration` | `PanelMaterialCompositions`、`TransportMaterialCompositions` | `MaterialItemCompositions`、`VanillaRecoveryRecipes` | 固定成分安装不再由 recipe 生产类承担 |
| `RegisteredWoodSurvivalRecipes.registerMaterials()`、`VanillaWoodProcessingRecipes.registerMaterials()`、`DiggableRecipes.registerMaterials()`、单物品与空胶囊写入 | `WoodMaterialCompositions`、`MaterialItemCompositions` | 木材、黏土、食物条、化合物和空胶囊回收 | 固定成分在配方发布前显式登记；配方类只发布配方 |
| `MaterialTooltips.appendForeign()` 的失效回收分支 | 04 有效成分快照 | 普通／高级 tooltip | 删除 `composition_nonmaterial`、`composition_descriptive` 及其语言键 |

### 复核补正

- `MaterialFactKeys` 现在是事实值类型的唯一定义点：`Decision`、`Identity` 以及所有内建事实键只定义一次。配方适配器不再在 `ReviewedSourceMaterialFacts`、`SourceRecyclingMaterialFacts`、`SourceWashingMaterialFacts` 之间转换相同枚举。
- 新增 Java 类已逐个反查：11 个 catalog fact provider 都由 `MaterialDefinitions` 的装配顺序引用，`MaterialFactKeys` 的 21 个内建键都有生产读取方，6 个 recipe 适配器、3 个固定成分安装器和 `SourceLensRoles` 门面均有真实消费者；没有只改包名或只转发一次的残留类。
- 无消费者的弃用别名 `MaterialDataRules.REPLICATOR_POWER_PER_NUCLEON` 与 `WoodSpecies.zhName()` 已删除。`ItemComposition` 的 legacy 构造／访问器和 `ImportedMaterialData` 仍有明确源码兼容用途，按前置审查结论保留，不把它们当作新的材料事实权威。
- Reviewed 炉／分级宝石事实表只在声明装配期按 ID+规范化名称投影为 process；生产运行时查询统一走 `MaterialFactKeys`，不再直接读取 `SourceRecyclingMaterialFacts.find()` 或静态流体绑定表。声明期辅助查找保持包内可见。
- Reviewed 炉授权的源名含空格或连字符时按 `GTMaterial.sanitize` 规范化后与当前规范名校验；否则 `Lithium-6`、`Red Sand` 等 22 条源行会因显示名格式差异永久不命中。
- `MaterialFluidUnitFacts` 同时覆盖通用流体、酸洗相态、蜂蜜／冰和离心相态；`MaterialFluidBinding`、酸洗和离心适配都从同一材料视图取得字段与比例。
- `FluidCatalog` 的生成熔融物例外改由 `SOURCE_MOLTEN_FLUID` process 提供；Ti／Fe 的整粉热回收所有权改由 `SOURCE_GENERATED_FLUID` process 判定，不再在 recipe 中复制材料身份。
- 蒸汽裂解的四个蜡反应物保留 recipe 局部集合，但恢复 ID+名称校验；朱砂副产物声明恢复带 ID 的身份并只挂在 Cinnabar 自身，适配器从这一个规范身份查询，避免同名、别名或相关副产物出现多份同级规则。
- Ti／Fe 生成流体的材料密度与流体条目密度是两个不同事实：前者与材料密度比较，后者仍要求为 0。

## 目标与边界

删除的对象必须有“已被替代”或“无实际消费且无保留依据”的证据。材料名、prefix 名、没有 delegate、当前没有调用方都不足以单独证明可删除；还要检查资源引用、附属契约、序列化与存档身份。

本阶段不预先承诺删除 chain、minecartWheels 等具体内容。行为减少和纯内部清理分开验收；确需删除注册项时先准备兼容处理和 parity 记录。

### material 与 recipe 的分界

沿用 [content 包职责](../../architecture/packages/content.md)与[总计划的分层约束](README.md#共同实施约束)。清理的单位是事实和职责；一个混合类可以拆开，不能仅根据 `Reviewed*`、`Source*` 或材料名称整类搬迁。

| 内容 | 权威位置与消费者 |
| --- | --- |
| 材料身份、物性、化学组成、粉碎／熔炼目标及比例、通用矿石副产物、形态规则、加工能力 | core `content.material.catalog` 的材料声明，必要时采用同目录的类型化辅助声明；通过既有材料查询提供给配方生成器 |
| 通用加工模板的机器、输入形态、工时、能耗、概率、选择器及输出槽约束 | core `content.recipe`；生成器读取材料事实，通用计算放 `engine.material`／`engine.recipe` |
| 某材料的独立化学反应或特殊机器路线，包括额外反应物、流体与独立产物配比 | `content.recipe` 的显式配方声明；原生配方装配和发布留 Minecraft `content.recipe` |
| 运行期物品／方块／流体存在性、外部覆盖、栈状态和输入证明 | Minecraft 层按材料、流体、配方的实际职责适配；不把 `ItemStack`、原生注册表或 loader 类型搬进 core |
| 原始上游事实、当前默认值指纹、有限审核范围 | 保留适用范围的依据与校验；经裁决的领域事实再归入材料或配方声明，不能把审核白名单直接当全局禁用规则 |

**朱砂与铝土矿的具体处理：**

- 朱砂的通用粉碎目标、倍率、副产物顺序等属于材料事实；例如 `OreMaterials` 中已有朱砂组成与 `Redstone → Sulfur → Glowstone → Selenium` 副产物声明。生成粉碎、洗矿等通用路线时读取这些事实，recipe 不再维护一份朱砂属性副本。机器的具体产率、工时和输入形态仍由配方模板决定，化学组成也不能直接替代矿石副产物。
- 铝土矿的高压釜路线属于特殊配方。当前由共享 Minecraft `content.recipe.generated.GTChemRecipes.autoclaveAndAluminiumElectrolysisRows()` 显式声明 12 条配方，`autoclave(...)` 绑定铝土矿、NaOH／KOH 和蒸汽，输出对应铝酸盐、蒸馏水与概率副产物；`MachineRecipeBootstrap` 调用该配方族。**保留在 recipe，不迁入 `OreMaterials` 或通用分解声明。** 如需拆出纯配方表，声明放 core `content.recipe`，原生装配留游戏层，且单独核验现有声明的来源约束。
- `AutoclaveRecipeRows`／`AutoclaveRecipes` 是另一条按材料可结晶能力生成的通用宝石生长路线，不是上述铝土矿化学配方的所有者。材料的“可结晶”能力与高压釜模板分工维护，不因共用机器而合并两族配方。
- 同一材料可以同时具有通用处理与特殊配方；特殊路线是否替换某条通用路线必须有明确工艺依据和所有权，不能见到特殊配方就关闭该材料全部通用处理。08-D—E 从最终有效配方派生铝土矿的来源／用途与处理图，材料目录不再保存一份高压釜路径。

### 本轮已清理的 Reviewed／Source 清单

以下保留旧文件与旧职责用于追溯，已经完成消费者迁移和回归验证，旧表或旧类不再存在。当前权威实现见上方结果表和代码中的 `MaterialFactKeys`；“材料声明”包括有明确适用范围的加工／形态声明，不等于向 `GTMaterial` 增加字段。

| 模块与当前类 | 当前职责及主要消费者 | 07 的具体处理与退出条件 |
| --- | --- | --- |
| core `ReviewedOreSourceProfile` | `FURNACE`、`GRADED_GEM` 的有限 YES／NO／UNKNOWN 表；`OreSourcePolicy`、`SourceGemGradeResolver`、炉配方适配消费 | 将已确认的炉授权和分级宝石能力归入材料加工／形态声明；统一与 `SourceRecyclingFacts` 的重叠查询。所有调用改读同一权威后删除该表；未覆盖身份不能变成 NO |
| core `ReviewedMagneticMaterials` | 磁性正例名单；`OreSourcePolicy.magneticCandidates()` 间接消费 | 磁性分类归回材料声明；核对现有 `MAGNETIC` 与正例名单差异及全部属性消费者。替换 `名单 OR flag` 后删名单；磁选机器的候选筛选和概率仍留 recipe |
| core `ReviewedSiftingGemForms` | Ice 的三个高级宝石形态负例；`SourceGemGradeResolver` 消费 | 将形态事实归入材料形态规则，保留“配方产出不可选”与“旧物品身份仍注册”的区别。外部显式表示仍按现有优先级选择，不能顺手注销 Ice 的旧形态 |
| Minecraft `ReviewedCrushedFormGeneration` | 九种材料的 crushed 系列排除及原生绑定证明；`SourceOreInputForms`、`SourceOreOutputRows`、洗矿适配消费 | 纯材料／形态排除并入规范规则；原生物品所有权、成分和外部覆盖检查留薄适配。统一输入／输出查询后删重复名单；当前是 recipe 局部排除，扩大到注册规则须另列行为影响 |
| Minecraft `ReviewedOreInputProfile` | 非矿输入的有限 ORES 负例及附属形态退让；非矿粉碎与后续路线消费 | 材料的矿物处理资格归入材料声明，外部形态与兼容退让留游戏侧。消除按 ID 的第二张资格表；不能把“没有 ORES”扩张为禁止一切配方或外部形态 |
| Minecraft `ReviewedOreShredderFacts` | 十一种材料的质量、MORTAR／self-pulver 审核范围和物品绑定；`SourceOreFollowonRows`／矿处理 loader 消费 | 材料质量、研钵能力和粉碎目标改读材料规则；形态、成分、外部所有权证明留适配。通用 shredder／研钵模板继续在 recipe；移除逐材料重复常量后再合并该适配器 |
| Minecraft `ReviewedCinnabarFacts` | 朱砂及副产物身份／属性指纹、原生形态和常温状态证明；粉碎、后续处理、蒸汽裂解消费 | 材料事实读 `OreMaterials` 及相关材料声明；本地物品解析留适配，共用配方规则保留。删除仅为断言目录未漂移而重复写出的整组常量，改用目录校验和工艺准入；先处理外部覆盖与 UNKNOWN 退让，不能直接放开所有材料 |
| core `SourceWaxMaterials`／Minecraft `ReviewedCinnabarFacts.sourceWaxes()` | 四种蜡的名单经朱砂类转发给 `SharedMineralSteamCrackingRows` | 去掉蒸汽裂解对朱砂类的依赖。若该集合表示材料分类，用材料 tag／group；若只是此工艺支持的四个反应物，保留 recipe 的局部输入集合。裁决后只有一处成员定义，不为了搬包扩大其他蜡的工艺资格 |
| Minecraft `ReviewedOreFluidBindings`、core `ReviewedOreGeneratedFluids` | 汞／蜡的单位绑定与汞、Ti／Fe 的生成流体指纹；洗矿、离心、热配方和 `OreFluidBindingAdapter` 消费 | 材料相态／单位声明、流体内容定义、原生所有权检查分开：分别复用材料 process、流体目录和薄适配。合并与 05／06 的重复换算及绑定定义；同一流体的声明一致不代表已原生注册，也不能统一假定 144 或 1000 mB/U |
| Minecraft `ReviewedCinnabarThermalRecipes` → `SourceDustThermalRecipes` | `emitIfSupported()` 只走 Ti／Fe 整粉与分数粉 Smelter 路线；`OreProcessingRecipeLoader` 消费 | 已按真实职责更名；氧化物热回收特权未恢复。熔点、密度、熔炼目标及流体单位从材料事实读取，工时／分数粉模板和发布仍在 recipe |
| core `SourceRecyclingFacts` 与 `OreSourcePolicy` | 源版热处理事实、当前端口默认值和炉授权的有界适配 | 逐字段区分源版事实、现行材料事实和工艺策略，再与前述表去重。源版与现行定义冲突时先裁决并评估所有消费者；不能直接把 source flags 全局回写材料，不能仅为了删表去掉外部／变更元数据退让 |

首轮之外的具体对象见 [recipe 包补充审计](07-recipe-package-audit.md)：`GenerifyingRules` 与 `SourceExtrusionRules` 已拆成“材料事实 + recipe 模板”，其余材料表和固定物品成分写入已迁出。机器独有的酸洗反应物、概率、选择器及输出预算继续保留在 recipe。两份清单仍不代表全部配方已经完成 07-E 的注册项去留裁决。

当前可跟踪文件中仍未找到已删除审核类所称的 `reviewed-source-policy.json`、`nonore-input-source-profile.json`，也未找到 `RecyclablePrefixes`、`MaterialDataFacts` 注释所称的抽取脚本。已迁移数据按现状保留并进入目录校验；缺失资料仍列为依据缺口，不能假称现有构建可以再生成。

## 实施步骤

### 1. 建立字段级退出清单，先处理重复事实

1. 对上表按字段登记：当前声明、规范声明、读／写调用方、适用工艺、原生绑定、附属覆盖、替代查询、迁移后保留项、删除条件和验收样例。已经与目录重合的常量优先去重；目录尚不能表达的能力先设计类型化声明，不新增无作用域的 boolean 白名单。
2. 明确 YES／NO／UNKNOWN 的原有语义。未审核、非内置所有者、外部形态和修改后的默认值可能属于 UNKNOWN，不能用普通 boolean 合并后当作 NO；合法不支持与缺绑定分别诊断。
3. 先以朱砂通用粉碎作为迁移样例，贯通材料声明、通用模板、原生输入证明与最终发布；再迁磁性、炉授权和形态限制等多材料名单。每次只切换一个配方族的写入者，比较最终可查询行为，不能长期双写。
4. 对铝土矿高压釜作保留对照：确认显式配方未被材料化，也未因通用矿物能力迁移而消失或重复生成。额外碱输入、蒸汽消耗、蒸馏水和概率产出属于配方本身的验收内容。

交付：字段级清单、材料事实迁移 diff、保留的特殊配方与运行期适配说明，以及旧／新路径的预期差异。纯搬迁不得悄悄改变工艺资格、产率和原生注册范围。

### 2. 清理兼容门面、旧写入与重复发布

1. 对 `Materials`、`GT6Materials`、`ImportedMaterialData`、旧属性字段和生成表逐项标明消费者、是否只读转发、被哪个规范声明替代。`Materials` 是当前可读符号入口，应保留；`GT6Materials`、`ImportedMaterialData` 与 `content.material.generated` 已有大量引用，不在本阶段按“旧类”整类删除。
2. 对已迁移字段和配方族搜索全部写入与调用，确认生产中只有一个决定位置；测试与兼容访问器不得继续暗中初始化旧表。
3. 删除零消费者的内部表、死方法和重复初始化；保留仍有公共消费者的兼容访问器，并写明废弃版本及迁移方式。
4. 验证最终生产路径不再出现无理由的 `continue`、`return null` 或 collision 丢弃来隐藏非法引用；合法“不支持此工艺”继续允许，但有明确分类。

| 当前入口 | 本阶段必须核对的内容 | 保留／删除条件 |
| --- | --- | --- |
| `Materials`、`GT6Materials`、`ImportedMaterialData`、`content.material.generated` | 静态字段是否只引用规范目录对象、类加载顺序、哨兵与别名、生成区的来源约束 | 保留有效符号与公开兼容；仅迁移确有必要的局部调用，不全仓改名。不得恢复各自安装或后置修改材料事实 |
| `MaterialForms`、`MaterialProcessingTargets`、`MaterialWorkability` | 区分规范目录查询门面与独立常量；检查老工艺是否仍读另一份默认规则 | 当前只读门面可保留；删的是重复事实或已经迁移且无消费者的方法。公开签名按 API 兼容边界处理 |
| `MaterialGroups`、旧 re-registration 入口 | 组成员是否来自规范组视图，匹配是否误改真实材料身份 | 有兼容消费者则保留转发；生产侧不再维护第二份成员或通过重注册合并材料 |
| `RecyclablePrefixes`、`MaterialDataFacts`、`RecoveryFormCatalog` | 回收形态、可扫描形态和实际注册规则之间的重叠与差异 | 同一语义只留一份声明；可扫描不等于可回收，不按集合相似就合并。生成来源不明时先记录缺口 |
| `GTMaterial` 的兼容属性／target、`MaterialInfoRegistry` 发布快照 | 每个已迁移字段的写入时点与单向派生；明确允许的附属回调 | 兼容读不再反写规范声明；不要删除仍承载运行时查询的字段，也不要重新引入 flag／typed rule 往返同步 |
| `bulkRecoveryAllowed` 兼容入口与消费者 | 当前构造已归一为 true，tooltip 和 producer 是否还把它当独立加工权威 | 移除内部失效分支和误导展示；旧参数／访问器如需保留只作兼容，不重新引入物品级回收否决 |
| `OreProcessingRecipeLoader`、Shredder／热回收／分解生产端 | 对照 `RecoveryRecipePublisher`、`ThermalRecoveryOwnership`、`DecompositionRecipeLoader` 的族所有权与重载撤销 | 通用、源版有界、显式特殊、动态／unit 各行只有一个有效发布者；迁出特殊分支时同步退出旧 owner，防止漏行与双发 |

保留 04—06 已收敛的行为：使用 `ItemRecoveryPlanner` 和类型化分解规则；保留 `MaterialPublication` 的成功代发布、失败代成分与配方整体恢复、标签撤销和单行冲突隔离。它们承担实际生命周期职责，不属于本阶段应删除的“重构脚手架”。

交付：逐项清理表和实际删除 diff，能够追溯到前面各阶段的替代能力。

### 3. 审计材料与形态的来源和终端

1. 从实际注册形态与配方查询结果建立“来源 → 中间转换 → 消费终端”关系，不仅靠 Java 字符串搜索。
2. 消费端包含机器部件、工具、配方输入、存储／建筑、附属请求和有意展示用途；纯“转成自己再回收”的闭环不算充分消费理由。
3. chain、minecartWheels 优先核对；billet、gearGt／gearGtSmall、itemCasing 已有消费线索，必须验证实际可达性，不能按名称批量删。
4. 对无来源或无终端项列出选择：保留并说明用途、停止生成相应配方、停止新注册并保留旧身份读取、删除。注册项删除需额外考虑旧世界中的物品／方块与流体。
5. 仅在证据和兼容方案完整后实施；材料、物品、流体的有意移除分别更新现有 parity 台账。未完成裁决的候选可以明确保留，但不能宣称审计已全部完成。

每项至少记录：材料与形态身份、实际注册对象、获得途径、有效配方输入／输出、终端用途、公开附属契约、存档影响、去留决定和证据。没有 08-D 索引时可先对候选做定向注册／查询审计，不能把文本命中数当最终可达性，也不能声称已经覆盖全部处理图。

### 07-E 定向审计记录

2026-11-13 使用最终双平台 GameTest 生成的 `reload-items.tsv` 与源码配方入口做了首轮定向审计。注册数是实际发布物品数，不是 Java 字符串命中数；两端完全相同，因此没有观察到版本专属缺口。

| 形态 | 实际注册物品数（Forge／NeoForge） | 已确认获得途径 | 已确认消费终端 | 本轮决定 |
| --- | --- | --- | --- | --- |
| `chain` | 407／407 | `AnvilRecipeDefinitions`：2 ring → 1 chain | `SourceThermalInputRules` 热回收输入、`SourceExtruderRecipes` 特殊输入矩阵、`PolarizerMaterialRecipes`、材料形态组界面 | 保留；有来源和终端，不删除身份 |
| `minecartWheels` | 207／207 | `HandMaterialCraftingRecipes`：2 ring + stick + hammer + wrench → 1 set | 热回收输入、特殊挤压输入、偏振器输入、材料形态组界面 | 保留；不从名称推断无消费者 |
| `billet` | 258／258 | 坩埚／模具、材料形态转换及显式配方 | 工具、研钵、挤压、偏振器和多项机器配方输入 | 保留；继续作为具体形态使用 |
| `gearGt`／`gearGtSmall` | 407／407 | 挤压、木材配方、坩埚模具及材料形态转换 | 旋转动力设备、齿轮箱、工作站建造、纳米制造和配方输入 | 保留；不是可批量注销的兼容形态 |
| `itemCasing` | 407／407 | 挤压与材料形态转换 | Welder、石墨烯路线、电源单元和机器建造输入 | 保留；源版 `casingSmall` 别名仍有实际消费者 |

本次没有找到足以删除上述注册项的无来源、无终端证据，因此没有改变物品 ID、NBT、配方身份或存档兼容性。完整来源／用途边仍由 08-D—E 的最终处理图验收覆盖。

### 4. 整理 tooltip 与语言

1. 以 04 的有效成分与加工判定为数据源，明确展示组成、具体工艺准入与当前状态；删除“完全损坏工具属于 technical/entity item”这样的错误分类。物品含哪些材料、能否执行当前工艺、损坏后剩余多少必须分别判断。
2. 从共享 `MaterialTooltips.appendForeign()`、两节点 tooltip 接线和对应语言资源逐项核对。`composition_nonmaterial` 不能只凭主材料 ID 判定所有拒绝原因；`composition_descriptive` 当前依赖已归一的 `bulkRecoveryAllowed()`，核验可达性后清理失效分支与语言键。复用现有状态展示入口，不另造第三套授权。
3. `zh_cn` 提供实际中文，普通／高级 tooltip 的门槛两平台一致；不把数据源、内部生产端、诊断术语塞进玩家常规流程。
4. 材料键与 lang 覆盖检查只处理本域，不批量改写或重新排序整份大型语言资源。

### 5. 验收初始化、查询、运行与重载

按下表执行，使用 01 的归一化输出对照；每个差异都有对应变更、测试和规则依据。

| 层次 | 无附属环境 | 测试附属／标签派生环境 | 通过条件 |
| --- | --- | --- | --- |
| 材料 | 身份、别名、属性、形态 | 合并／冲突／所有权、合法链接 | 无重复身份或未裁决漂移 |
| 成分 | 显式物品、JSON、方块与植物 | 标签兜底、CR.REV 与显式覆盖 | 查询与遍历一致，变更顺序不改变结果 |
| 回收 | 粉碎、砧、Smelter／Melter、坩埚 | 附属材料与外部输入 | 授权与数量正确，非法栈不被满额消耗 |
| 分解 | 固／液／气体输入与 map 权限 | 附属绑定与形态 | 配比、单位、选择器、槽位规则正确 |
| 材料 API 与图 | 分类／组、形态、有效来源／用途 | 外部 tag、附属工艺与世界生成 | 图与最终内容同版本，无条件丢失、伪来源或无限展开 |
| 生命周期 | 首次启动、连续两次重载 | 标签增删、候选权限变化 | 无旧行残留、无重复、缓存同步失效 |
| 客户端 | tooltip、配方展示、机器实际交互 | 基础游戏未装可选模组 | 无错字／错误状态、服务端权威不被展示绕过 |

两个版本共有的材料信息与规则应相同；版本独有物品和原生绑定差异单独说明。配方展示抽样不能代替最终查询与机器执行；服务端 GameTest 也不能代替客户端视觉检查。

清理专项至少增加或复用以下断言，按真实测试源码集接入；现有用例不是所有新迁移项已经有覆盖的证明。

| 迁移风险 | 必须观察的结果 | 优先复用入口 |
| --- | --- | --- |
| 删表后材料事实漂移 | 材料身份／别名不变；修改一个规范声明即可改变相应通用生成结果；无需同步修改审核名单 | core `MaterialCatalogTest`、`MaterialFormCatalogTest`、`MaterialReferenceAudit` |
| 有限名单被误当全局否决 | 原有正例、负例、未覆盖材料、附属材料、变更元数据和外部显式表示分别验证 | core 工艺规则测试与共享 `MaterialBaselineTests`，补足本次实际迁移范围 |
| 朱砂／Ti／Fe 旧 owner 回归 | 朱砂无专用热回收绕过；Ti／Fe 分数粉不漏行或双发，输入证明与损坏／容器状态仍生效 | `RecoveryRecipePublisherTests`、材料领域 GameTest 与最终 `RecipeMap` 查询 |
| 特殊配方被材料化或挤掉 | 铝土矿高压釜的 NaOH／KOH 两支、等量粉末形态、额外物料与概率输出保持；通用结晶路线仍独立 | 化学配方解析结果、最终高压釜查询；迁移触及该族时补对应执行用例 |
| 清理影响成分和回收 | 铜块 9U、72U 输出、复合工具头、损坏工具、空／满容器及已凿模具的量和拒绝行为不回归 | `MaterialBaselineTests` 与 core `ItemCompositionResolverTest` |
| 重载只撤销了一半状态 | 连续两次真实重载无旧行；移除 tag 后表示与派生行退出；失败代保留上一成功成分和配方 | `MaterialBaselineTests` 的 tag generation、失败恢复与真实资源重载用例 |

### 6. 核对发行包与维护入口

1. 使用 Forge `distributionJar` 和 NeoForge `jar` 的实际产物验收，不把 SDK 或 Forge 中间 JAR 当最终模组。
2. 检查两种发行包中无 GameTest 类、模板、案例 TSV、原始 dump、导入脚本或任务日志；运行资源与 API 类按既有打包规则各出现正确份数。
3. 同步 API、content、engine、runtime、platforms 中确有变化的职责与验收条件；新增检查方式或命令时按规则同步 validation 文档和路由。
4. 将长期数据维护步骤写入开发指南，跨平台验证结论按 integration 模板保存；原始记录继续留 `work/`。本组计划标明哪些阶段已实施、哪些仍待完成，不把计划全文改成未经核验的成功记录。

## 独立验证与最终通过条件

检查集：D、F、J、A（涉及公开 API 时）、C、G、B、M。检查命令及先后要求见[总计划](README.md#检查的使用方式)和相应检查文档。

### 本轮验证记录（2026-11-13）

执行且通过：

- D：`python3 tools/validation/route.py check`，路由闭合、检查文档和相对链接通过。
- J：`./gradlew :core:test`，core 全套 174 tests 通过；新增事实投影计数覆盖全部迁移类别。
- C：`./gradlew compileBothPlatforms`，Forge 与 NeoForge 节点均编译通过。
- F（改动范围）：`-PspotlessRatchet=HEAD` 的 core 与 Minecraft 格式检查通过；`git diff --check` 与注释引用检查通过。
- G：最终代码在全新目录 `work/material-system-refactor/07-review2-forge-20261113` 与 `07-review2-neo-20261113` 分别运行 Forge／NeoForge `runGameTestServer`，结果为 94/94 与 99/99；两节点均执行 `material_baseline`、`material_baseline_reload` 和配方发布批次。
- B（首轮）：两个平台实际发行 JAR 构建成功；最新 JAR 中未发现已删除旧类、GameTest 类、案例 TSV 或测试模板。

执行但失败或阻塞：

- 不带 ratchet 的 `:core:spotlessJavaCheck` 仍会报告 HEAD 既有的 `OriginalMachineMaterialData`、`RecoveryFormCatalog` 等格式差异；本次没有为通过检查而批量改写这些无关文件。按本次改动路径的 ratchet 检查已通过。

尚未执行：

- 07-E 的完整材料／形态清单裁决；`chain`、`minecartWheels`、`billet`、两种齿轮和 `itemCasing` 的定向审计已完成。
- 07-F / 08-D—E 的有效来源／用途索引、有界处理图和远程客户端验收。
- 客户端 tooltip、JEI/EMI 展示及机器实际交互的人工验收。

- 新增／修改普通材料只改一处领域声明；组件、燃料、形态和分解不再跨表重复维护同一字段。
- 同一物品只有一份有效成分解释，冲突可定位，加载顺序不再决定结果。
- 回收／分解同一配方族只有一个发布者；不支持、缺绑定、量化损耗与真正冲突可区分。
- 生成量与最终可查询行为都有证据；unit 折叠和显式覆盖可解释，没有用“配方数变少”代替正确性验收。
- 代表样例覆盖铜块授权、72U 输出、复合工具头、损坏工具、容器、已凿铸模与压模机模具、多组件热输入、附属材料、化合物分解及重载。
- 双平台检查与客户端确认分别报告，材料信息无未解释的版本漂移；没有把 compile 成功写成游戏行为成功。
- 所有内容删除都有消费证据、兼容说明和必要的 parity 记录。

## 拆分与兼容处理

建议按下列小变更实施，每项独立保留可编译、可验证的最终生产路径：

| 切片 | 内容 | 前置与完成条件 |
| --- | --- | --- |
| 07-A | 字段级清单、朱砂通用事实样例与蜡集合解耦 | 02—06；材料事实只维护一处，铝土矿特殊配方保留，外部／未知范围不被误禁用 |
| 07-B | 磁性、炉授权、形态与流体绑定名单收敛；热类名称／所有权整理 | 按 07-A 的边界逐族迁移；旧表退出、原生证明保留、最终可查询结果可解释 |
| 07-C | 兼容门面、死分支、旧写入和重复发布清理 | 对应消费者已迁移；公开兼容和成功／失败代语义不回归 |
| 07-D | tooltip 与语言清理 | 使用现有有效成分与状态；两个客户端确认普通／高级展示 |
| 07-E | 材料／形态去留与必要迁移 | 定向审计可先开始；删注册项必须单独提供可达性、存档和 parity 依据 |
| 07-F | 全链路、处理图、远程客户端与发行包总验收 | 08-D—E 完成；按本页矩阵补齐，分别记录两个平台与未覆盖场景 |

07-A—D 无需等待 08-D—E；07-E 的定向审计也可以先开展。08 的有效来源与共享图仍是 07-F 的交付前置，不能用内部清理完成替代。每个切片的检查按实际改动路径与总计划检查集执行，不把纯文档清单的检查结果沿用为实施代码的验收结果。

仅删除内部代码时，可回退对应小变更并恢复旧适配器；一旦删除了世界中可持久化的身份，恢复源码不等于恢复已保存的数据，必须在实施删除前选择并验证迁移／保留读取策略。测试世界使用隔离副本，不对真实服务器执行本计划。
