# 液态酸工厂与材料消费核查

第五十二批，2026-10-21。行为观察与排版基线为 `21f68f1b1874e0d99a95e11de5d3bdfe68c9e1fb`，新增 ALIGN-180–182。`MaterialFactories.java` 仅展开排版并纠正三处过强的原版归因说明，没有改变工厂签名、属性、顺序或执行内容；132→147 行、1304 个执行词法项及字面量位置等价证明、原始／最终哈希由[本批统一回执](verification/liquid-acid-factory-audit-20261021.json)记录。以下当前工厂行号使用排版后的文件。

本批没有修改材料生成声明或用户整理的材料表，没有运行／导入材料生成器，没有运行 Gradle、编译、新测试、游戏、世界重启或构建，没有推送。原模组根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team／Gregorius Techneticies 与 LGPL-3.0-or-later 头；本报告只引用行为证据，没有移入原生依赖源码，也没有据此另行裁定发布授权。

## 原工厂含义及实际生成链

原 [MT.java:127](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/data/MT.java:127) 的无纹理参数 `lqudacid` 继承 `SET_FLUID`，加入 G_CONTAINERS、CONTAINERS_FLUID、ACID；`lqudaciddcmp` 增加 DECOMPOSABLE，`lqudacidcent/elec` 再增加 CENTRIFUGE／ELECTROLYSER。显式纹理重载在 153–156 行延续该层级。实际 19 条声明都使用无纹理参数重载，并在参数中加入 LIQUID。

原 [TD.java:589](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/data/TD.java:589) 的 G_CONTAINERS 是 CONTAINERS + PLANTS，不包含 DUSTS、GEMS、STICKS、PARTS。通用 `dcmp/cent/elec`（MT.java:94–96）也是 create + DECOMPOSABLE，再加对应处理标志；**原版 elec/cent 表达处理方式，并非宝石家族，也不自动赋予 MAGICAL**。个别材料可以在声明中另外传入宝石或魔法属性，不能因此把所有现代 elec 调用一并判错；本批只穷举液态酸族。

当前 [transpile_gt6_materials.py:68](C:/Dev/GregTech6/tools/transpile_gt6_materials.py:68) 的 LIQUID_FACTORIES 包含 lqud/lqudchem 族，遗漏 lqudacid 族。`factory_kind`（301–335）先检查液态集合，再按 endswith(elec/cent) 路由，最后回退 dust；`infer_texture`（338–360）按错误 kind 给出 FINE／SHINY／RUBY。`emit_material`（646 起）使用该结果，`modular_materials.write_catalog` 只拆分声明到 CompoundMaterials 并生成旧名 facade，没有修正工厂语义。保留的 `tools/generated/materials-source.java.txt` 也可按相同 ID 找到这些表达式。

`Materials`、`ImportedMaterialData`、`GT6Materials` facade 引用同一生成实例，不是另外注册的 19 份材料。以下原行号属于 MT.java，生成行号属于当前 `core/src/main/java/com/gregtech/gregtech/content/material/generated/CompoundMaterials.java`；全部原纹理均为 SET_FLUID。D／E／C 三组当前属性见表后说明。

| ID | 原字段／原行 | 原工厂 | 当前字段／生成行 | 当前工厂／纹理 | 属性组 | LIQUID 补偿 |
| --- | --- | --- | --- | --- | --- | --- |
| 9825 | HNO3／1031 | lqudaciddcmp | NitricAcid／49 | dust／FINE | D | 有 |
| 9824 | H2SO4／1047 | lqudaciddcmp | SulfuricAcid／59 | dust／FINE | D | 有 |
| 9844 | H2S2O7／1048 | lqudaciddcmp | DisulfuricAcid／60 | dust／FINE | D | 有 |
| 8011 | H2SiF6／1054 | lqudacidelec | HexafluorosilicicAcid／61 | elec／SHINY | E | 有 |
| 8413 | TiCl4／1087 | lqudaciddcmp | TitaniumTetrachloride／68 | dust／FINE | D | 有 |
| 8400 | ChloroauricAcid／1163 | lqudaciddcmp | ChloroauricAcid／110 | dust／FINE | D | 有 |
| 8401 | ChloroplatinicAcid／1164 | lqudaciddcmp | ChloroplatinicAcid／111 | dust／FINE | D | 有 |
| 8402 | StannicChloride／1165 | lqudaciddcmp | StannicChloride／112 | dust／FINE | D | 有 |
| 8403 | BlackVitriol／1168 | lqudacidelec | BlackVitriol／113 | elec／SHINY | E | **未找到** |
| 8404 | BlueVitriol／1169 | lqudaciddcmp | BlueVitriol／114 | dust／FINE | D | 有 |
| 8405 | GreenVitriol／1170 | lqudaciddcmp | GreenVitriol／115 | dust／FINE | D | 有 |
| 8406 | RedVitriol／1171 | lqudaciddcmp | RedVitriol／116 | dust／FINE | D | 有 |
| 8407 | PinkVitriol／1172 | lqudaciddcmp | PinkVitriol／117 | dust／FINE | D | 有 |
| 8408 | CyanVitriol／1173 | lqudaciddcmp | CyanVitriol／118 | dust／FINE | D | 有 |
| 8409 | WhiteVitriol／1174 | lqudaciddcmp | WhiteVitriol／119 | dust／FINE | D | 有 |
| 8410 | GrayVitriol／1175 | lqudaciddcmp | GrayVitriol／120 | dust／FINE | D | 有 |
| 8411 | MartianVitriol／1176 | lqudaciddcmp | MartianVitriol／121 | dust／FINE | D | 有 |
| 8412 | VitriolOfClay／1177 | lqudaciddcmp | VitriolOfClay／122 | dust／FINE | D | 有 |
| 9827 | AquaRegia／1188 | lqudacidcent | AquaRegia／129 | cent／RUBY | C | 有 |

当前 [MaterialFactories.java:41](C:/Dev/GregTech6/core/src/main/java/com/gregtech/gregtech/api/material/MaterialFactories.java:41) 的实际属性为：D 组具有 DUST、GENERATE_DUST、UNIT；C 组经 cent（137–139）→ gem（47–54），具有 DUST、GENERATE_DUST、GEM、GENERATE_PLATE、GENERATE_STICKS、GENERATE_LENS、GENERATE_PARTS、TOOL_HEAD；E 组经 elec（127–129）再加 MAGICAL。

## 已存在的补偿与两端入口

不能从生成声明误分类直接推导“酸性、全部酸液或全部配方缺失”。当前存在以下实际调用的补偿：

| 环节 | 已确认内容及范围 |
| --- | --- |
| 注册与链接 | `GTMaterialRegistry.init:240–273` 执行 declare → link；`MaterialDefinitions.link:26–33` 调用 MaterialCompositionData.apply、MaterialDataFacts.apply、MaterialFormCorrections.apply。 |
| **19 条酸性已补齐** | `MaterialFormCorrections.java:47–55` 已在 link 阶段加入全部 19 条 ACID，早于物品／流体注册；postInit:278–280 → ImportedMaterialData.applyAcidFlags:36–49 又补一次。Forge MaterialLinkBootstrap 与 Neo 主类:155 均接入 postInit。 |
| **19 条组成已补齐** | `MaterialCompositionData` 保存全部 19 条组成并在 link 执行；GTMaterial.setComposition 推导常见分母，MartianVitriol 保留显式 18。MaterialDataFacts.apply 也恢复 UUM，不能报成组成仅剩 tooltip 字符串。 |
| **18 条真实流体已补齐** | `FluidDefinitions.prepare:34–58` 两组恢复 5 + 13 条 LIQUID；FluidCatalog.registerGeneratedGasLiquidFluids:965–990 按实际材料状态生成以规范名小写命名的流体并建立 GenGas_/GenLiquid_别名和材料关联。唯一未补为 BlackVitriol，见 ALIGN-182。 |
| Forge 平台消费 | 构造阶段 material → item → fluid；ItemBootstrap:27 消费 MaterialItemDefinitions.all；FluidBootstrap.run:60 起先 prepare，再遍历 RegisteredFluids.all 排入 FluidType／Fluid／fluid_item 注册。 |
| NeoForge 平台消费 | 主类在 init／roles 后调用 FluidRegistries.register:36 起，先 prepare，再遍历同一流体目录；随后 GTItems.register:97 消费相同材料物品目录。 |

酸性已在 link 阶段补齐，不依赖两端物品／流体注册先后。`MaterialForms` 保存原 CONTAINERS、CONTAINERS_FLUID、LIQUID、PLANTS，但其查表不会修改 GTMaterial 状态；这是原形态元数据，不是另一条流体注册入口。

## ALIGN-180：液态酸族误获固体／宝石形态并进入实际物品目录

原工厂不授予 DUSTS、GEMS、STICKS、PARTS；当前 16 个 dust、2 个 elec、1 个 cent 却都增加 DUST／GENERATE_DUST，其中 E／C 三条又继承整套 gem 属性。

[MaterialPrefix.java:179](C:/Dev/GregTech6/core/src/main/java/com/gregtech/gregtech/data/MaterialPrefix.java:179) 的 HAS_DUST 是当前属性 OR 原形态 DUSTS，HAS_GEM、STICKS、PARTS 同样保留当前属性；239–246 明确记录只补充原形态、不删除移植版已有形态。因此原形态表没有 DUSTS／GEMS 并不会抵消误加属性。GTMaterial.put 只追加，getProperties 返回不可变视图；没有发现后续清除这 19 条 DUST／GEM 的补偿。

全部 19 条会通过 dust、dustSmall、dustTiny、dustDiv72 条件；氟硅酸、黑矾、王水三条还通过宝石、宝石板、杆／螺栓／螺丝、齿轮／环／弹簧／转子／壳体、透镜等条件。原来就来自 G_CONTAINERS 的五种 plantGt* 不算误加；对所有材料开放的 unit 也不是本项特有问题。

`MaterialItemDefinitions.all:148` 与 generates:255 实际遍历 candidatePrefixes 并调用 isValidFor；上述 Forge ItemBootstrap／Neo GTItems 真实消费同一目录排入平台注册。这里已越过“仅有属性字符串”的证据层，但没有在游戏中检查最终显示、排序或旧物品保存。后续移除这些误生成物品仍需单独明确旧存档策略，本批没有删除或迁移它们。

## ALIGN-181：氟硅酸误获 MAGICAL，并被流体管道危险逻辑消费

原 H2SiF6（MT.java:1054）使用 lqudacidelec，没有传入 MAGICAL；当前 HexafluorosilicicAcid（CompoundMaterials.java:61）经 elec → gem + MAGICAL。黑矾也有同类错误属性，但目前没有本模组原生流体，本项不把它算成已经进入管道流体路径的实例。

氟硅酸的 LIQUID 已由 FluidDefinitions 恢复，生成流体携带对应材料关联。[FluidHazards.isMagic:62](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/api/fluid/FluidHazards.java:62) 除独立流体 MAGIC 标志，还查询 FluidVisualPolicy.material(entry).has(MAGICAL)。`FluidPipeBlockEntity.checkSafety:452–501` 实际将此判定送入 FluidPipeSafety.magicLoss；对非耐魔管道，路径包含魔法漏失、附近实体中毒和随机移除管道，且独立于酸性腐蚀检查。

因此这是额外物理危险，不只是图标或名称差异。触发仍取决于流体实际进入对应管道、耐魔参数和 tick／随机分支，本批没有搭建产线实测。PortableFluidContainerItem.accepts 也会在 entry.materialKey 非空时消费 MAGICAL；氟硅酸生成流体具有该关联，不能因此把任意其他独立流体或全部容器一并宣称受影响。

## ALIGN-182：BlackVitriol 的 LIQUID 补偿遗漏

原 BlackVitriol（8403，MT.java:1168）明确传入 LIQUID，原 Loader_Fluids.java:657–661 按该标记创建实际液体。当前生成声明经 elec 只有 gem／dust／MAGICAL 等属性；FluidDefinitions.prepare 的第一组恢复 TiCl4、HNO3、H2SO4、H2S2O7、H2SiF6，第二组恢复另外 13 条酸液，均不含 BlackVitriol。

当前 `FluidCatalog.registerGeneratedGasLiquidFluids:971–974` 只检查 GTMaterial.GAS／LIQUID，不查询 MaterialForms 中 `8403|CONTAINERS,CONTAINERS_FLUID,LIQUID,PLANTS`。因此源形态表里的 LIQUID 不构成补偿。两端平台注册又只遍历准备后的同一 RegisteredFluids.all，不能在边界自动补出这条流体。

全生产 Java／资源搜索只见黑矾材料声明、别名、组成、源形态、显示名及多余固态块模型，没有独立同名 FluidEntry、物理状态补偿、动态酸族枚举补偿或显式化学配方行；语言别名里的 blackvitriol 不会注册流体。范围限定为项目自带目录和已核初始化链，外部 addon 自行注册的流体不在本结论内。黑矾的酸性和组成已经补齐，不能把本项写成整份材料定义缺失。

## 纹理与分解配方的后续合同边界

原 19 条均为 SET_FLUID，当前为 FINE／SHINY／RUBY；当前枚举已有 FLUID，而且生成器还独立保留 `SET_FLUID → FINE` 映射。因此只补 LIQUID_FACTORIES 集合不足以恢复原纹理。MaterialIconDefinitions.resolveTextureSet:9–20 优先显式 textureSet，后续模型路径会使用它；没有发现重新设置本族纹理的补偿。但原生流体渲染的 FluidVisualPolicy 先选专用流体 sprite，只有 molten／plasma 路径用材料图标集替换模板，故不能宣布所有世界酸液贴图都错误。本批不为这条尚需区分图标／流体路径的边界新增编号。

原 Loader_Recipes_Decomp.java:38 起按 DECOMPOSABLE 与显式 CENTRIFUGE／ELECTROLYSER 选择地图，并先尝试液体／气体输入。原工厂明确授予处理地图的三条为 H2SiF6、BlackVitriol → Electrolyzer，AquaRegia → Centrifuge；另 16 条 dcmp 声明本身只授予可分解，不能据此要求它们全加相同电解配方。

当前共享 DecompositionRecipeLoader 的 Forge／Neo 显式展开都先构造 dust 输入，检查组成与 divider≤64，再解算输出；输出完整时按“直接成分是否全为 ELEMENT”选择 Electrolyzer／Centrifuge。MachineRecipeBootstrap:64 实际运行；MaterialStackItemHelper.mat 只转到 GTItems.getStack，没有无粉末时替换 unit／流体的隐藏回退。它不会通过此通用入口注册上述原流体输入形式，但这不是对最终全部配方的穷举。

全部 19 条组成均已存在并在分母范围内。按当前路由静态看可形成 17 个 Electrolyzer、2 个 Centrifuge 候选；**这不是最终注册配方数量**，输出完整性、地图接受条件、显式行和冲突处理尚须分别核实。本批不据入口差异新增缺失配方编号。

明确反例是 `GTChemGen`：54–65 行已有多种矾及氯金酸／氯铂酸／四氯化锡的独立酸液电解行，71–72 有氟硅酸混合／热混合，176–194 有 TiCl4 生产／还原，197 有王水混合。GeneratedRecipeResolver.loadAll 实际加载该表，m: 经 materialFluid 解析真实 GenGas_/GenLiquid_等对象。因此不能宣称这些酸完全没有流体配方，不能把工厂语义修正等同于已经对齐全部分解配方。

## 本批验收范围

原版声明、生成器、生成材料、MaterialForms／组成／初始化补偿、两端实际注册入口与管道危险消费者已连接核查；没有扩审其余 elec／cent 材料全集。原作者、文件身份、54 个取证源码哈希、19 项逐行数据、共享代码两端投影与排版证明由[统一回执](verification/liquid-acid-factory-audit-20261021.json)汇总，初查材料保存在 `work/liquid-acid-factory-audit-20261021/`。

本批仅确认三项静态合同缺口。没有运行期注册表、最终配方表、旧物品迁移、完整化学生存链、管道实际破坏、专服或独立旧存档重启证据；没有宣布酸族或材料处理系统全部核查完成。
