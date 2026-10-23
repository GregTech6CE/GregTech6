# 通用材料分解合同核查

第五十四批，2026-10-21，新增 ALIGN-189–193。初查起点为 `e3f08722c4934a9478b38e7b3395012eedd803b1`，初查收据采集时为 `f42d88cb2bf2d4444c624e722e8bb722d42d35d8`；其间归档了前批文档。共享代码去重以 `f42d88cb2` 为基线，将 `DecompositionRecipeLoader.java` 的两份完整业务合为一份，336→124 行，仅保留原生导入和 holder 类型／查询两处条件块，并纠正过强的原版归因说明。Forge 850／Neo 858 个完整执行词法项、字面量及位置逐项相同；没有在去重中修复下列行为。原／最终哈希、双端证明及来源由[统一回执](verification/material-decomposition-audit-20261021.json)记录。以下当前加载器行号按去重后的 124 行文件。

本批没有运行／导入生成器，没有 Gradle、编译、新测试、游戏、世界重启或构建，没有推送；没有修改材料表。原模组根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team／Gregorius Techneticies 与 LGPL-3.0-or-later 头；这里只引用源码行为，没有移入原生依赖源码，也没有扩大已有发布许可信任范围。

## 实际加载入口与已保留的数据

原 [GT6_Main.java:267](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/GT6_Main.java:267) 在后加载兼容回调中运行 Replace → Foreign → Decomp → Handlers，其中 Decomp 在 271 行；流体在更早的 Loader_Fluids 创建。原加载器是实际使用路径。

当前 Forge 主类 `GregTech.java:79–80` 在 commonSetup.enqueueWork 中完成 material post-init／物品关联后调用共享 MachineRecipeBootstrap.load；Neo 主类 `GregTechNeoForge.java:155–156` 同样先 postInit 再调用。共享 bootstrap:64 实际运行 DecompositionRecipeLoader，之后运行 ChemicalRecipeLoader，最后执行 GeneratedRecipeResolver.loadAll。两端更早已完成 material link、物品定义和 FluidDefinitions.prepare；SetupOnce 防止共享步骤重复运行，recipes-ready 回调又允许 addon 继续添加配方。

当前不是只留下化学式文字：MaterialDefinitions.link 实际调用 MaterialCompositionData.apply，GTMaterial.setComposition 保存未除分母的组件和分母，0 分母按总分量/U推导，显式分母仍可与总量不同。material.resolve()==entry 的规范身份过滤也保留了。上批酸族 19 项仍沿已有结论：19 条组成／酸性及 18 条真实流体已恢复，黑矾状态遗漏另有 ALIGN-182；本批不重复登记这些问题，也不宣称所有酸类化学配方缺失。

## ALIGN-189：分解准入与地图启发式替代原 processing 合同

原 [Loader_Recipes_Decomp.java:38](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/c/Loader_Recipes_Decomp.java:38) 要求有组成、commonDivider≤64、DECOMPOSABLE，再分别查询 CENTRIFUGE／ELECTROLYSER 加入地图列表。材料可以没有通用分解地图，也可以有两种；“有组成”本身不是准入许可。

原 MT:94–96 的 dcmp/cent/elec 分别授 DECOMPOSABLE、再加指定处理方式。`OreDictMaterial.setMcfg:527` 只建组成，`uumMcfg:602` 另外处理 UUM，不因全元素自动推断地图。另一条实际合同是 `setAloy/uumAloy:677–735` → alloyCentrifuge:424，不能只检查声明开头的工厂名而漏掉末尾调用。

当前 [DecompositionRecipeLoader.java:47](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/content/recipe/DecompositionRecipeLoader.java:47) 只检查有效组成、0<divider≤64、非 ANTIMATTER，随后在 72／93 行用“直接成分是否全为 ELEMENT”选择 Electrolyzer／Centrifuge，没有原准入／地图判断。以下两个反向样本证明它不是原 processing 标志的等价表达：

| 样本 | 原版合同 | 当前通用路径 |
| --- | --- | --- |
| Bronze／8610 | MT:1705 的 clloymachine 末尾 `.uumAloy(0,Cu,3U,Sn,1U)` 实际授予 CENTRIFUGE；组成分母4 | CompoundMaterials:436 保留青铜及组成，但 Cu／Sn 都是 ELEMENT，93 行改选 **Electrolyzer**。 |
| Lazurite／8330 | MT:1516 使用 **elec**；Al2O3×6、SiO2×6、Ca×8、Na×8，分母28；原是 Electrolyzer | CompoundMaterials:333 保留组成，但直接组分 Al2O3／SiO2 不是 ELEMENT，93 行改选 **Centrifuge**。 |

NaCl／8204 也是准入反例：原 oredustdcmp 声明（MT:1129）有 DECOMPOSABLE，但没有指定通用地图；当前 Salt 有 dust，Na／Cl 均为 ELEMENT，会尝试额外的通用电解。原本存在盐粉+水的显式氯碱电解，并不能反推纯盐粉通用反解也是原合同。

当前 `GregTechTags.Compounds.DECOMPOSABLE` 与 `Processing.CENTRIFUGE/ELECTROLYSER` 确有静态定义（342、349–350），所以不能说这些名字完全没有移植；但它们没有连接到 GTMaterial 的属性集合或此加载器的判断，实际搜索只见定义／解释注释。MaterialForms 的形态和 MaterialDataFacts 的 UUM 也不提供这一 processing 选择。没有找到针对两张地图安装通用组成动态查找提供器的补偿；已有 scanner／printer／replicator／bumble 动态机制不是此合同。

本项确认具体地图及准入差异，没有枚举所有组成材料，也没有运行期统计最终地图行数。

## ALIGN-190：仅 dust 输入遗漏原气体／液体／粉块入口

原 Decomp:59–84 按材料真实 liquid／gas 绑定分别尝试输入；输入材料量为 divider×U，经各自绑定的量纲转换并向上取整。普通液体／气体常为1000mB/U，熔融金属为144mB/U，不能统一替代。存在可用流体时会抑制常规粉末回退；没有流体时尝试 dust，divider%9==0 时也尝试 blockDust，Glowstone 组另有固体例外。

当前加载器 56–57 行只请求 divider 个 dust，失败立即 continue，完全没有流体输入及 blockDust 分支。MaterialStackItemHelper.mat 直接转到 GTItems.getStack，没有自动替成 unit／流体的隐藏回退。具体非酸样本如下：

| 样本 | 原输入 | 当前通用入口／已存在反例 |
| --- | --- | --- |
| Nitrogen Monoxide／9837 | MT:1028 gaschemelec；N1/O1，分母2；selector0 + 2000mB NO 输入电解 | 当前 CompoundMaterials:46 使用 gas，材料没有 DUST／ELEMENT、原形态9837也无DUSTS，因此没有对应dust，56–57行跳过。现有 NO 生产／氧化／硝酸反应不等于通用 NO→N/O 已恢复。 |
| Glycerol／9828 | MT:1040 lqudchemelec；C3/H8/O3，分母14；selector0 + 14000mB 甘油电解 | 当前 CompoundMaterials:56 使用 gas，也无dust，通用入口跳过。GTChemGen:116 使用甘油、245–246 蒸馏产生甘油，不能报成甘油没有用途或整个链缺失。 |
| Bronze／8610 | 原 MOLTEN 已创建，分母4对应576mB熔融青铜离心，常规dust回退受抑制 | 当前入口变为4青铜粉，且地图还受 ALIGN-189 影响。正向合金混合／坩埚展示不是反向输入合同的补偿。 |
| SaltWater／9804 | 原 MT:1143 lqudelec，H2O3/NaCl1，分母4，流体电解地图 | 当前无dust而跳过通用入口；**GTChemGen:241 已有独立盐水流体电解，370 有干燥**。它们的产物／工时与通用组分反解不同，但足以排除“盐水完全没有电解”的结论。 |

原 Electrolyzer 路径加入 `ST.tag(0)`，即数量0、不消耗的selector0；固体分解时selector和粉末分别占槽。当前没有selector，但现代 `MachineRecipeMapDefinitions.Electrolyzer:40` 已主动把最小物品改0、最小总输入改1，允许单水电解与单粉分解。故不能宣称当前全部候选因缺selector而被validate拒绝；selector控制、地图最小输入与现代交互须作为后续取舍连同核查。

本项确认此通用入口没有原粉块／气液形式，不等于已排除所有粉块转换、其他显式配方或 addon 补偿；没有恢复旧前缀或物品存储形式。

## ALIGN-191：EU/t 与 duration 公式误置且功率固定

原 RecipeMap 完整 addRecipe0/1/2 的两个 long 参数依次是 **aEUt、aDuration**（Recipe.java:227–278），当前便捷重载也保持这个顺序（RecipeMap.java:302–350）。因此原 Decomp 传入的 `max(16,totalAmount*14/U)` 是功率，`UT.Code.units(totalAmount,U,292,true)` 才是时长，后者按 UT.units:1677 向上取整。

当前加载器 94–95 行却将前一表达式赋给 duration，并把 eut 固定为全元素64／其余16。以下均为 **RecipeMap 优化之前的参数**：

| 样本／未除分母的组成总量 | 原 EU/t／duration tick | 当前候选 EU/t／duration tick | 范围 |
| --- | --- | --- | --- |
| Bronze／4U | 56／1168 | 64／56 | 同时还有输入形态／地图差异。 |
| Lazurite／28U | 392／8176 | 16／392 | 组件粉末可解，地图还受ALIGN-189影响。 |
| MethaneIce／3U、输入分母2 | 42／876 | 16／42 | 组成总量用于功率／时长，分母用于输入，不能混为同一数。 |
| CobaltHexahydrate／7U | 98／2044 | 理论16／98 | 当前在工作参数计算前因水输出解析失败跳过，见ALIGN-192；不把理论值算成已生成配方。 |

原 Recipe 构造与当前 RecipeMap.make 都可能把输入／输出／duration除以公因数，当前还会抵消同物品输入输出并舍弃完全抵消行；后续校验／碰撞也可能拒绝候选。本项确认提交公式差异，不把表中值冒充全部最终显示值或机器实际运行耗能，也不声称由此得到最终配方数量。

## ALIGN-192：vanilla Water 无 still holder 导致六水合钴整条跳过

原 CobaltHexahydrate／8229（MT:1189）是 dustcent，组成 Co1/H2O6、分母7。原 Water（MT:1007）明确 `.liquid(FL.Water.make(1000))`，熔点273K，因此原通用离心可输出钴粉和6000mB水。

当前 Water（CompoundMaterials:29）有 GAS/LIQUID，FluidCatalog:322 也明确以 VANILLA_WATER 复用原生水；Forge FluidBootstrap:69、Neo FluidRegistries:38 遇到 usesVanillaFluid 都跳过创建 still holder。这是正确复用 Minecraft 水，不是水本身注册缺失。

当前 [namedFluid:109](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/content/recipe/DecompositionRecipeLoader.java:109) 对 Water 在112行找到目录项，随后114–118行只尝试 GTFluids.still，没有原生水分支，所以返回 null。Water 又没有 DUST／ELEMENT、源形态9800也无DUSTS；MaterialRoleFlags／MaterialFormCorrections 没有额外补水粉，OM.dust(Water)最终为空。加载器84–90行因此 complete=false、跳过整条，不是仅少一个水输出继续提交。

已有正确补偿 helper 也已核对：GeneratedRecipeResolver.named:962–980 明确为 vanilla water／lava 返回原生FluidStack，GTFluids.stack也有相应分支；但是本加载器没有调用它们。因此不能把别处存在helper当成已补此处，也不能泛化为所有显式含水配方都出错。其他含水组成尚未逐条穷举，本项只使用可直接闭合的六水合钴样本。

## ALIGN-193：按状态 flag 选输出相态替代原温度／绑定规则

原 Decomp:49–54 先检查组件熔点是否不高于 DEF_ENV_TEMP，再使用组件 material.fluid(amount,false)。原默认值是 CS:135 的C+20，即293K；material.fluid（OreDictMaterial:1294–1304）又按实际温度与熔点／沸点／等离子点选择已绑定的液／气等相态。不是只检查某个材料分类名字。

当前加载器 namedFluid:110 仅以 GAS／LIQUID flag 放行，随后依名字查holder并固定1000mB/U；没有温度或真实相态绑定判断。失败后79行直接请求同材料粉末。具体反例为 MethaneIce／9833：

- 原 MT:1190 为dustcent，显式分母2，组件CH4:1U + Ice:2U。Ice熔点273K低于293K；原 Loader_Fluids:364 已真实将 `ice`“近冰点水”绑定到MT.Ice，所以原输出该流体。
- 当前 CompoundMaterials:131 保留分母／组成，但 Ice:35 是dust、无GAS／LIQUID，namedFluid第一道门返回null。FluidCatalog:332其实有名为Ice的专用真实流体，依然未被采用。
- 甲烷有具名 Methane 流体可解算，冰组件则经OM.dust得到2冰粉，流程可以继续。后续MaterialUnification.canonical只做同材料／形态的显式原版物品归一，不把冰粉转换成流体。

这是可核对的产物相态差异；分母2与组成总量3本来就是原显式配置，不能把它当成移植分母计算错误而自动改3。当前其他材料的高温／低温输出、生成流体别名或molten比例仍需逐条核查，本项不宣布所有分解产物相态都错。

## 显式补偿、完整性与未完成范围

GTChemGen和其他显式loader是实际被加载的化学配方路径。除上述盐水外，GTChemGen:49和ChemicalRecipeLoader还有盐粉+水氯碱电解；NO生产／氧化、甘油蒸馏也存在。GeneratedRecipeResolver通过真实材料／流体注册对象解析m:等表达式，不能因为通用入口有差异就把所有化学配方认定为缺失。本批没有逐条执行这些显式行的运行期依赖解算。

原输出还先转换组件的mTargetPulver材料和比例，再选择OM.dust；当前直接OM.dust(part,amount)。postInit虽保存若干pulver规则，但此加载器／OM.dust不查询，RecipeMap中的MaterialUnification.canonical也不替代pulver。此为后续输出目标合同边界，没有在本批另给所有组件输出错误编号。

原ArrayListNoNulls只过滤null，非空输出即可继续，UT.Code.makeArray按地图槽数复制／截断；当前要求每个组件均可解，否则整条跳过。原Error流体sentinel及RecipeMap错误处理也存在，不能把原宽松／可能丢失输出的路径自动当作应恢复行为；完整性策略需单独决定。

当前加载器103行added++不检查map.addRecipe1返回值。RecipeMap.make的归一／优化、validate及collision仍可拒绝候选；后续显式行和addon也会影响实际表。必须区分循环尝试数、实际接受数和最终可查询行数，本批无任何全量最终配方统计，也未以日志added或历史测试文件存在替代实测。

初查的8个非酸样本、78个源码身份、9份双端投影记录及去重后的精确等价证明，由[本批统一回执](verification/material-decomposition-audit-20261021.json)汇总；初查scratch保留在 `work/material-decomposition-audit-20261021/`，其中旧共享文件hash代表观察时版本，不覆盖为新排版hash。

本批确认五项静态合同缺口，并保留显式补偿与策略边界。没有运行期注册表／最终配方表、完整化学生存链、专服、旧物品迁移或独立旧存档重启证据；没有宣布通用材料分解或所有材料处理系统全部核查完成。
