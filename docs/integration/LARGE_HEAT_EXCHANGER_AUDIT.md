# 大型换热器输出、流体与扩展合同核查

第六十八批，2026-10-21，基线fa352343e。核原17197大型换热器的实际注册、17部件、八路输出、热液充热、冷液排出、工具／覆盖板、设置与制作入口。新增ALIGN-242–243两个扩展／输入范围差异，003／036／110／218／232扩宿主；六份源码整理保持执行，不修改玩法。

原版只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。38份原来源、61份当前输入、41份共享原生源的82个平台视图和十份完整执行比较在[回执](verification/large-heat-exchanger-audit-20261021.json)。原Forge矿辞匹配另引用官方源；本批没有运行或重新编译原生类，下面的有限例子均为源推导。

## 实际入口与已有主链

原Loader_MultiTileEntities:1245注册17197：额定16384HU/t、效率字段默认10000、硬度／抗爆6／6、stack16、Hot Fuels，材料ANY.W。当前GTMultiblocks.registerAll创建heat_exchanger_main，GTBlockEntities.LARGE_HEAT_EXCHANGER的两端工厂绑定该块；LargeHeatExchangerControllerBlock的服务端ticker实际执行BE.tick。原ID亦由SourceBlockProperties等映射，不能因它未列在SharedLargeMachineParts数组就报未注册。

额定16384来自OriginalGeneratorParameters，HeatExchangerRules.DEFAULTS提供10000效率／Hot Fuels／HU。热液名义容量10R=163840L，补热目标2R=32768HU，冷液背压门槛20R=327680L；冷罐默认Long.MAX_VALUE。6／6物性由GTMultiblocks.properties实际传给Block，当前物品默认64的堆叠差异扩218。

Forge的BE.getCapability提供共用IFluidHandler；Neo由BasicMachineRegistries.register挂capabilities监听，再显式注册LARGE_HEAT_EXCHANGER::fluidCapability。它位于machine注册边界，单块设备则由SmelteryRegistries注册。只查后者会误报大型设备没有能力，本轮已排除。

结构是两层实心3×3，底层中心主控不列为支撑，剩17件：9个18024致密钨壁板、8个18101热传导器。底层其余8件允许输入，上层NOTHING不提供运输；当前FLUID_INPUT／CASING省略的物品／能源bit没有本机库存或注入实现，不据此报丢物。原17次checkAndSetTarget的坐标、材料和顺序，与当前SharedHeatExchangerStructure按y／z／x构造顺序相同。有限重建只是源坐标，不是实际搭建。

主控IFluidHandler仅填hot、普通drain仅取cold；底层部件用只入FluidPort，形成验证后委托同一主控，无第二份储液。漏斗工具仅填有效热液；龙头专用tap优先cold，cold空才选hot。每tick末尾向下给冷液，最大单次流量受原生int FluidStack表示限制，两边都没有另加1000L主动输出上限。当前没有为大型设备新设原本不存在的开关。

## 八路热量、燃料循环与背压

原onTick2先调用父类，再无条件处理已有热和燃料；没有把结构形成结果当总工作门槛。当前tick先检查／绑定结构，同样不以返回值截断。不能为对齐另外加“未形成就不出热／不烧热液”的要求。

两边先算每出口`min(R/8, heat/8)`，扣八倍，然后向主控上2格的3×3外圈八处接收器底面发送size1、相同amount。顶层传导器的NOTHING能力不是这个主动发送入口，不能据其不暴露HU就报八路失效。接收端拒收或空气不会退还已经扣掉的热，原版亦如此。源坐标集合相同，遍历顺序不同：原先四正向再四角，当前按X／Z顺序；未建立内置接收器因顺序不同而改变产线结果的见证，不虚构普通分配故障。

之后heat<2R且cold<20R才补燃料，HotFluidFuelCycle为大小换热器共用的实际边界，HeatExchangerRules只计划数值批次。普通行热量为floor(abs(EUt×duration)×效率/10000)，充到不低于2R；20R仅在循环外检查，不能改成每批硬截断。有效行不足一批时，64位活动历史为0才清剩余hot；失效燃料找不到候选时清空。冷液种类不兼容则保留输入并不补热。最后不足8HU余热清零，活动历史推进，再向下排cold。

已存在16条Hot燃料定义和FuelRecipeLoader.hot→FuelRecipeMaps.Hot消费；缺外部流体时仍跳过，不能把所有条件行都当内置注册保证。普通Lava行16×5=80HU/L：默认设备从heat0、hot1000L、兼容空cold开始，410批消耗410L／产410L冷液、存32800HU；下一次八路各可尝试2048HU，共扣16384。初始cold327679L时，同一次补热可增至328089L，跨过20R，原循环与当前计划都允许。效率0、同种有效1L行与1000L输入时，两边也会耗完1000L而不增热。这些是给定条件的源算术，未生成或执行配方。

当前BigInteger与饱和乘法避免原long乘法溢出，并不等于所有扩展配方、极值和动态RecipeMap handler已对齐。HeatExchangerRules原注释泛称“等价原循环”过强，现改为明确的有界批次规划；没有改变计算。

## ALIGN-242：指定目录外能源时无法恢复原扩展输出

原readFromNBT2对gt.energy.emitted调用TagData.createTagData。该公共工厂将名称大写、复用已有标识，找不到便创建并登记新对象；原接收器接口接受TagData，自定义模组可注册同名类型参与。原八次插入使用这个对象，不要求它列在内置能源常量中。

当前HeatExchangerData仍保留该字符串，Settings.energyType却只调用GregTechTags.Energy.byId；后者仅遍历内置20种身份的ALL列表，未知名称返回null。Tag构造器不公开，注册表及工厂为private，也没有对应的公开新增身份入口。只保留字符串不能证明能源扩展协议已移植。

给定有效设置ENERGY.AUDIT_EXTENSION、heat16384、无后续热液，原扩展已注册对应TagData及接收器时可尝试八次2048包；当前先扣16384，再因emitted=null跳过全部插入，getEnergyTypes也为空。此例限定扩展类型与有效设置已加载，未执行addon、插入算法或存档。默认ENERGY.HEAT／HU在当前目录中，普通换热不因此失效。将来应明确公共注册与未知设置处理合同，本轮不临时补一种专用能源来绕过。

## ALIGN-243：制作定义未保留ANY.Cu铜管范围

原17197布局DDD／PMP／DDD，D为退火铜致密板，M为18024壁板，P为OP.pipeHuge.dat(ANY.Cu)。ANY.Cu用addReRegistrationToThis关联Cu和AnnealedCopper，OreDictManager把合格前缀材料物品重注册到对应组名。CR把OreDictItemData转成矿辞名交AdvancedCraftingShaped，其父类接受该组的候选列表；这不是拿组里第一件物品后固定死。[Forge 1.7.10官方匹配实现](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/src/main/java/net/minecraftforge/oredict/ShapedOreRecipe.java)确认字符串输入取OreDictionary列表，匹配时遍历候选。

当前MultiblockCraftingRecipes.ENTRIES中的大型换热器已有同布局、致密板和壁板，但P写为pipe:huge@Copper。MultiblockRecipePack→MachineRecipeIngredients.resolveAll→pipeIngredient因此走按尺寸／GTMaterial精确查一件物品的分支。解析器实际已有any_铜族列表分支，单块换热器也会使用，但本行没有选择它。结果定义只发一个item，不是原开放的任意合格铜管范围。

ToolShapedRecipe把输入交原生有序配方；CraftingTools.expand只扩展gt6ce:tool_输入，不会替普通管道补铜族候选。当前已核内置流体管只有Copper，没有AnnealedCopper管，故不能捏造一个内置退火铜管失败案例，也不能声称默认配方无法制作。确认的是矿辞／材料组输入范围未保留，额外合格管道的兼容需后续真实注册验收；未生成pack、加载标签或游戏合成。

## 工具、覆盖板与保存：已有分支及明确差异

本类已实现MultiblockToolTarget：GTToolItem先调用MultiblockTools.use，建造棒遍历17件，部件有owner时亦可转发；放大镜有formed／just formed／incomplete及流体内容反馈。不能把233中Dynamo／涡轮的缺专用入口推广过来。Input／Output／Empty和流体描述的直接字符串仍扩003，材料／流体原生名称可翻译不代表整个句子已进入语言系统。

当前还已有只读MachineControl.Provider，running／active读取active，supportsSwitch=false，符合原没有OnOff的接口边界；不扩219的“缺提供者”。但PanelCoverRuntime.canAttach对所有MachineCoverSpec统一要求supportsSwitch，仍会拒绝原仅要求Running接口的运行检测器，沿110的安装资格范围补具体宿主。普通后备覆盖板宿主由配置Mixin提供，不能报所有板都不能贴；原运行值和安装资格必须分别追。

两端世界保存含设置、gt.energy、活动／历史、形成状态和冷热罐，读取实际调用HeatExchangerData.restore。restore先清／设名义容量，再setFluid恢复long数量；LongFluidStorage.setAmount不截断，所以例子中名义163840、保存200000L仍能恢复200000，而不是只看通用setCapacity就报丢液。原Activity.save自身读取已有键而不写出活动／历史；当前显式保存这两个字段是保留的改善，不为逐字一致恢复原缺陷。真实磁盘重启、客户端瞬时状态仍未验。

普通挖掘虽由Block.getDrops先构造运行快照，最终配置CoverDropMixin→CoverDrops会清本类完整能源／液体内容并合回覆盖板，不能把局部快照当最终掉落。原大型换热器没有单块设备的专用排液器整罐入口；本类通用排液器的1000L路径不直接扩221。原Base10默认环境检查返回true，不自动补新火／雨损坏或额外烫伤。

## 既有记录扩围与六份源码导航

| 记录 | 本轮范围 |
| --- | --- |
| 003 | 专用放大镜的直接Input／Output／Empty及流体句子仍需语言入口，原文本来源与现代汉化验收分开 |
| 036 | 本机当前已保留有效子集认领，不套“完整候选才整体认领”；缺区块时原旧形成缓存与当前逐格检查／释放策略仍不同，仅扩这个子情况 |
| 110 | 只读运行提供者已有，但检测器统一要求开关能力导致拒装；与原成功状态缺失同属已登记的状态／资格边界 |
| 218 | 17197原stack16、当前默认64，已核能源设备从19扩20 |
| 232 | 当前每tick及部件有效能力查询重扫17格并重绑有效件；原缓存查询不重跑整个循环。20次完整部件查询可造成340次格检查／绑定，只为条件源次数，不是TPS测量 |

LargeHeatExchangerControllerBlockEntity展开结构／工具、八路、冷热能力和两端保存；Block展开版本交互与ticker；HotFluidFuelCycle展开候选选择，HeatExchangerData展开设置／长罐边界，HeatExchangerRules展开有界计划，SharedHeatExchangerStructure展开三重循环。结构局部、lambda、候选燃料、默认值及批次数改用明确名称；公开类型、字段、注册ID、方法类型签名、常量、分支和语句顺序保持。

完整词法与字面量位置在仅列明绑定归一后相等：BE Forge3168／Neo3103，Block924／1185，燃料496／502，设置392／452，core数值569，core结构253。包名context、原getDrops参数builder和Charge.batches组件均未跟随新局部名称改动，许可证头、其它注释及Stonecutter指令保持；一条数值说明限定了原先过强的等价范围。CompoundMaterials／WoodMaterials摘要保持。

原LargeHeatExchanger头为GregTech-6 Team2025、Gregorius Techneticies、LGPL-3.0-or-later，其余来源独立留头与摘要；官方Forge源仅引用，没有复制旧方法体／原生源码／资产进生产。本轮未运行目标初始化／Java编译、Gradle、测试、资源包生成、实际供能／排液／造结构／覆盖板／合成、存档重启、构建或推送。其余大型能源、全部配方／材料、控制器、世界、模型和外部扩展仍在全面队列中。
