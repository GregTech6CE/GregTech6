# 核反应棒目录与单棒合同核查

第五十九批子域，2026-10-21，新增 ALIGN-202–205。行为观察基线为 `67dff604decf8f08df3c09dd12ede04a1158b22f`；随后 `e4867cf1ee33a02cf40ad2061706d06a9d22e007` 仅展开核心源码并补职责导航，执行与原注释保持，没有修复下述合同。本稿涉及 ReactorCoreBlockEntity 的旧行号按67d基线，现文件以同名方法重定位。来源、两端投影、有限参数和核心排版证明由[统一回执](verification/nuclear-contract-audit-20261021.json)汇总。

原模组根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为 `C:/Dev/GregTech6`。本子域留存21份原版／52份当前路径身份，18份共享维护源的36份显式平台视图；没有修改生产或用户材料表，没有导入／执行生成器、Java初始化、Gradle、编译、新测试、游戏、构建或推送。这里只审目录、单棒公式、注册／消费和棒配方／提示；核心冷却、控制、运行状态、保存及中子网络空间／周期另行归档。

原棒类保留GregTech-6 Team的2021／2022版权与LGPL-3.0-or-later头，作者Gregorius Techneticies；Moderator还署名Erik3003。当前NOTICE及已有native-nuclear来源记录继续保留；本批未引入新源码、资产、依赖或扩大发布许可信任范围，不推定原快照发行版本或制造上游commit。

## 46份原版目录及真实消费已经存在

原 [Loader_MultiTileEntities.java:741](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java:741) 至790实际注册46棒。前面的Enable1x1ReactorCore只控制1x1核心合成，不包住这些棒。当前 [ReactorRodCatalog.java:8](C:/Dev/GregTech6/core/src/main/java/com/gregtech/gregtech/content/nuclear/ReactorRodCatalog.java:8) 同样有46条：

| 类别 | 数量／原ID | 已核对内容 |
| --- | --- | --- |
| 核燃料 | 17，9210–9261中的实际注册项 | life、self、emission、divisor、maximum、乏棒ID全部与原显式值相同。 |
| 乏燃料 | 17，9310–9361中的对应项 | 对应材料及目录项存在，静态离心路线已有。 |
| 增殖棒 | 4，9410／9420／9430／9440 | 预算64000000／256000000／640000／4096000000，逐面loss为1000／2500／250／10000，产物9411／9421／9431／9441。 |
| 增殖产物 | 4，9411／9421／9431／9441 | 反向来源ID、接收及半热规则已有。 |
| 空／吸收／反射／慢化 | 各1，9201–9204 | 实际注册类型及单棒规则已有。 |

46条原显式数值逐项比较没有差异，完整目录与材料字段对应保存在回执；不能把原17种核燃料误报为只移植了六个旧常量。

Forge ItemBootstrap:60、Neo GregTechNeoForge:49均调用 [GTFuelRods.registerAll](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/registry/GTFuelRods.java:89)。其中六个旧常量由FuelRodItem构造器41–44绑定原定义，另40条由目录循环注册，六旧ID不再次重复注册。两版初始化随后均到MachineRecipeBootstrap.load，后者62行调用ReactorRodRecipes.register。

真实核心入口 `isFuelRod/insertRod` 要求RodPhysics.definition非null；Network.exchange实际调用self／emission／receive／finishCycle，核心 `react()` 每tick调用RodPhysics.react、替换产物stack并累加热。普通无数据物品回落目录的long寿命；旧health／maxHealth数据才走比例兼容。旧FuelType的baseHealth、baseHuPerTick和consumeHealth没有参与这条实际反应链，不能拿旧枚举数值替代目录参数。

## 常规单棒公式已有对应，周期另行核查

原 [Nuclear:175](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/tileentity/energy/reactors/MultiTileEntityReactorRodNuclear.java:175) 至264与当前 [RodPhysics:35](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/content/nuclear/RodPhysics.java:35)／RodPhysicsMath均有：基础self加在本棒；每轮发射 `E + ceil(max(previous-S,0)/div)`；核燃料每tick热量n；n≤max损耗100，否则ceil(400n/max)，慢化再乘4；耗尽换对应乏棒。

Breeder按每次接收先扣loss，拒收慢化来源；预算扣最终flux、热floor(n/2)、耗尽换产物。Absorber全吸收、热2n；Product全吸收、热floor(n/2)；Reflector原量反射且无热；Moderator按上一轮contacts乘incoming反射，incoming>0才累加下一轮contacts，并令来源核燃料进入下一轮慢化。Base／Depleted不发不收不产热。这些是单次规则结论，不证明世界周期相位正确。

| 有限参数样本 | 原／当前结果 | 范围 |
| --- | --- | --- |
| U235，previous／flux=1000 | S32、发射274、热1000、寿命损耗100 | 一致。 |
| U235，previous／flux=3000 | 发射774、热3000、损耗586；慢化2344 | 一致。 |
| U238增殖棒，两面分别输入3000／4000，loss2500 | 分别吸收500／1500，总2000；热1000、预算扣2000；慢化来源吸收0 | 一致，未把两面先合并再只扣一次loss。 |
| flux1001的吸收棒／产物棒 | 热2002／500 | 一致。 |
| Moderator旧contacts2、incoming30；普通Reflector同输入 | Moderator反射60、下一轮contacts加1；Reflector反射30 | 一致。 |
| 锆空棒挤压，melt2128K、density6.506 | 原216 tick、当前64 tick | 差异，见ALIGN-203。 |

11种冷却剂的单棒变换已有对应：工业冷却液S/E×4、div×2；CO2的S×3；He／LiCl／Thorium Salt的E折半；LiCl的S×5、max增加ceil(max/4)；Thorium Salt的S0、max×4、div减1；Tin／Sodium的div减1；Heavy／Tritiated的max分别ceil(/8)／ceil(/16)；四类水系慢化。当前E/2与原E−ceil(E/2)在实际目录的非负偶数E上相同；div的最小值保护在实际目录不触发。流体真实解析及槽准入属于冷却子域，不由此表推定通过。

保留数值边界：原Nuclear的`400 * intFlux`、Absorber的`intFlux * 2`、Moderator的int乘法可能溢出，当前部分使用long／饱和。五个常规样本低于溢出域；不声称无限临界flux逐位相同，也不建议为追随原溢出恢复危险行为。

## ALIGN-202：空棒直接挤压入料从合格前缀收窄为锭／粉

原 [Loader_Recipes_Extruder.java:201](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/c/Loader_Recipes_Extruder.java:201) 至204遍历EXTRUDER_FODDER／INGOT_BASED／GEM_BASED／DUST_BASED，要求U64≤amount≤U且能整除U；287–292对实际有效Zr栈生成空棒。原OP.dustSmall(U/4)、dustTiny(U/9)、nugget(U/9)都满足，故4小粉或9小搓粉是可闭合的直接输入样本。

当前 [ReactorRodRecipes.java:49](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/content/nuclear/ReactorRodRecipes.java:49) 至53仅遍历MaterialPrefix.ingot／dust。生成GTExtruderGen及其余内置配方表未见另一条空棒输出补偿；RecipeInputs.matches→MaterialEquivalence.matches要求同prefix，9小搓粉不会直接匹配1整粉。先用其他路线合并成整粉仍可继续生产，但不等于原直接挤压入口。

结论限内置源码的输入合同；没有初始化最终配方表或排除addon／外部脚本新增路线，不把静态前缀候选数当实际缺失配方数。

## ALIGN-203：空棒挤压工时把每单位重量换成密度

原Extruder:287–292使用 `max(64,trunc((melt-DEF_ENV_TEMP)*(1+getWeight(U))/6144))`。原CS:132／135给DEF_ENV_TEMP=293K，OreDictMaterial:1391–1401定义getWeight(U)=density×111.111111公斤；它与g/cm³密度不是同一量。

当前ReactorRodRecipes:51用getDensity代替getWeight(U)。原MT的Zr与当前ElementMaterials:63均声明熔点2128K、密度6.506：原1U重量722.888888166，原始工时216.2005…截断为216；当前float原始值约2.24178，被最低值提升为64 tick。功率96EU/t、1U材料／空棒1产量仍对应，不能把整条配方说成没有。

这是声明参数的有限数学差异，不是机器实际运行计时、最终地图完整性或全挤压系统验收。

## ALIGN-204：棒专用动态状态、解释与单位显示减少

原Nuclear:76–170显示分钟、慢化状态，解释self／emission／maximum／factor，并轮播10组冷却剂的实际参数与critical提示；Breeder:62–73显示逐面loss、慢化输入限制、产物目标和所需中子预算。Moderator／Absorber／Reflector／Product／Depleted／Base也各有独立说明。

当前 [FuelRodItem.java:182](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/item/FuelRodItem.java:182) 至187对有定义栈只加Kind枚举、无单位的原始Remaining long，核燃料再显示一行基础四参数后return；没有读取当前慢化标记、显示冷却剂改参或增殖目标／loss。CommonBlockTooltipHandler／CommonBlockTooltips提供采掘、抗爆等通用行，其他客户端棒引用没有补回这些专属信息；手册已有解释也不替代物品动态提示。

本项是缺少内容／动态状态／单位的合同，与ALIGN-003的硬编码语言问题分开；不因显示减少而声称上述物理规则缺失，也未验收客户端实际排版。

## ALIGN-205：两种旧额外棒仍可见，但没有当前反应定义

GTFuelRods仍实际register `fuel_rod_mox`／`fuel_rod_am_243`。FuelRodItem:41–44对这两个FuelType回落original0，所以definition=null；核心的定义门控拒绝插入，RodPhysics也不反应。它们不在原46棒目录内，不应编造原GT的MOX／Am243棒参数；原Compat_Recipes_IndustrialCraft:365–366产物是IC2自己的reactorMOXSimple，不是GT核心棒。

必须按实际创页所有权判断：旧CreativeTabBootstrap能量页297–299确有definition过滤，但当前双端原创页入口10／86优先调用OriginCreativeContents。后者98–160扫描注册gt6ce物品，不过滤这两个FuelRodItem，48行归reactors，末尾通用分支加入页面。因此这是两种仍注册、在当前原创页可见而没有当前核反应定义的旧移植残留，不是已删除或统一隐藏。没有断言外部脚本不能为其提供其他用途。

## 已有补偿与未验边界

ReactorRodRecipes已有相关棒罐装、17条乏燃料及3条固态增殖产物离心、9431输出500mB Tritium并回空棒的路径。MaterialFormCorrections:38–44已为有关材料补STICKS，Graphite也补；不能只按最初生成工厂断言原料形态全部缺失。没有查询运行期注册表，不以候选条数代替最终可用／可查配方数。

空棒挤压的模具保留已沿真实消费确认，排除“数量1模具必被吃掉”：GTTechnological:48将含_shape_物品注册成TechItem(catalyst=true)，Recipe.isCatalystInput:84–88消费RecipeCatalystLike，RecipeInputs.consume:21–50预留后返还催化剂；BasicMachineBlockEntity:700实际使用该流程。原`.get(0)`和现代new ItemStack(shape)虽形式不同，已有保留补偿。

本批没有真实注册表／JEI／EMI、临界反应堆、自动插拔、世界tick／跨区块、故障、辐射、双端客户端渲染、旧物品／旧NBT重启、专服或生存链实测。46目录和常规单棒规则已有对应，不意味着整个核反应堆完成对齐；周期、冷却、控制、运行状态与保存以同批其他子域报告为准。完整来源和静态推导见[统一回执](verification/nuclear-contract-audit-20261021.json)。
