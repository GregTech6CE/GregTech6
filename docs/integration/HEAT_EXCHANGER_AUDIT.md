# 单方块热交换器原版合同审计

第六十三批，2026-10-21。观察c4a95c5c4、来源清单收束于545294e25；219登记运行状态提供者，220登记周围点火准入，221登记玩家排液入口优先序。耐久请求扩展既有126，110／111／132只沿既有合同引用。八种注册与普通换热已有实现，不能再按旧反馈声称完全未移植。

已读工作约定、四份中央文档、ORIGINAL_ALIGNMENT_AUDIT、ENERGY_CONVERTER_AUDIT、CONTROLLER_DETECTOR_AUDIT 和2026-10-20单方块补齐记录。后者此前已补八种注册并运行过限定 Neo 场景，不能沿旧反馈继续报“单方块热交换器根本未移植”。既有126的工具返回值成本可扩充本宿主；110的检测器安装能力门控和111的possible近似不另拆同根条目。17的转换器环境检查原范围不含本类，亦不借热交换器再报告一遍通用锅炉或基础机问题。

原根 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；当前根 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team／Gregorius Techneticies 与LGPL-3.0-or-later头。当前共享路径以下缩写相对于 `multiversion/src/main/java/com/gregtech/gregtech/`；core类另标。

## 主要结果与边界

八种规格、原配方定义入口、双版方块／BE／ticker／流体能力、常规热液换热、无接收器时散热、顶面HU、正面冷液、输出背压、双罐保存和普通拆卸清内容已有对应。

仍需处理或明确取舍的部分为：单块原有运行／活动状态未接入当前通用状态提供者；周围点火用现代火焰存活检查取代原WD.fire的目标／可燃邻居判定；玩家排液器通用1000L fallback抢在原专用整罐入口之前；专用入口固定1点成本可扩展既有126。所有见证均为已追调用链的源码条件／有限算术，不是游戏、堆栈崩溃或世界重启验收。没有把大型热交换器结构、长整型极值或任意扩展配方的全部问题一并包入。

## 目录、注册、配方与真实两端入口

原 `Loader_MultiTileEntities:707–717` 使用 MultiTileEntityGeneratorHotFluid 注册八项，均为Hot Fuels和HU。当前 core `HeatExchangerCatalog.ALL:29–36` 一一对应：

| 原ID | 当前路径后缀 | HU/t | 效率 | 硬度／阻力 |
| --- | --- | ---: | ---: | ---: |
| 9103 | invar | 16 | 10000 | 4 |
| 9107 | tungsten | 128 | 10000 | 10 |
| 9108 | tungsten_steel | 128 | 9000 | 12.5 |
| 9109 | ta4hfc5 | 256 | 10000 | 12.5 |
| 9153 | dense_invar | 64 | 10000 | 4 |
| 9157 | dense_tungsten | 512 | 10000 | 10 |
| 9158 | dense_tungsten_steel | 512 | 9000 | 12.5 |
| 9159 | dense_ta4hfc5 | 1024 | 10000 | 12.5 |

当前前缀为 `gt6ce:heat_exchanger_`。GTHeatExchangers.registerAll遍历这个core目录，用RegisteredBlock.register同时加入GTBlocks.BLOCKS／BLOCK_ITEMS，物品是stack16的HeatExchangerItem；BlockProperties.strength(hardness)对应原同值硬度／阻力。GTMultiblocks.registerAll:37实际调用它。Forge由MachineBlockBootstrap:30进入；Neo由GregTechNeoForge:55→BlockBootstrap.run的Neo投影:53进入，随后提交GTBlocks和GTBlockEntities。不是只存在Catalog、未注册的八条声明。

GTBlockEntities的两版本HEAT_EXCHANGER工厂绑定GTHeatExchangers.all的全部方块。HeatExchangerBlock.getTicker只在服务端及匹配BE类型时调用HeatExchangerBlockEntity.tick。Forge BE本身覆写getCapability提供IFluidHandler；Neo SmelteryRegistries.register将capabilities加入mod总线，:80绑定同BE的fluidCapability方法，后者检查isRemoved。两端原生ticker生命周期沿已核Minecraft1.20.1／Forge47.4.20及1.21.1／NeoForge21.1.243的BlockEntityTicker入口。

实际制作定义在core UtilityCraftingRows:21–29：`PCP/OwO/PMP`，普通／致密分别使用单板／四重铅板、双板／致密铜板、细／大铜管和普通／四重机壳；殷钢的C与O使用ANY.Cu范围，其余C为退火铜。MultiblockRecipePack:131–150将ROWS与OriginalSurvivalRecipes合并，查真实物品、buildRecipe，再按平台原生recipe(s)路径发出required资源包。没有静态JSON不代表没有配方。本轮只读这些入口，没有运行pack生成或枚举最终标签成员。

## 流体、热计算、活动记录与输出：常规合同已有

原HotFluid类:113–173与当前tick的主要顺序均为：先输出／浪费已有热，再在热量低于2r时尝试补燃料，最后正面排冷液。原用mActivity三态保存视觉，当前history为64位历史，history非零渲染活动；两个原活动态在源贴图入口都以state>0选择同一组活动覆盖，不因现代合成二值LIT就断言丢了一种不同纹理。

| 合同 | 当前实际实现／对照 |
| --- | --- |
| 输入罐／输出罐 | hot容量10r；cold默认Long.MAX_VALUE。原mTanks[0]在读入后设10r，原输出默认同为long无上限罐。 |
| 流体输入 | 原getFluidTankFillable2／funnelFill只允许Hot Fuels输入；当前isFluidValid／fill同查FuelRecipeMaps.Hot，不是按普通流体温度猜热量。自动化各面均可接入，原亦未给这两个方法加面限制。 |
| 自动输出与手动取液 | 普通drain只从cold；正面主动输出cold。FluidAttachmentBlock.attachedHandler先识别FluidToolTarget，因此专用tap优先cold，有冷液时不能越过它取hot，cold空才取hot；funnel只填hot。这是龙头／漏斗路径，不等于玩家排液器入口已正确，后者另见下文。 |
| 顶面HU | 原网络emit受SIDES_TOP限制，size=1、amount=min(r,heat)；当前明确向UP发相同size／amount。两者不按接收量退还热，连接空气亦消耗offered。不会把原100%效率误读成绝对不散热。 |
| 充热门槛 | heat<2r且cold<20r才开始查表；循环充至不低于2r，单批可跨过2r，且只在循环外检查20r背压。当前charge也不在每批额外截成20r。 |
| 单批热 | 原floor(abs(EUt×duration)×eff/10000)，当前BigInteger同式；普通表输入1L，产物按原同量冷液。 |
| 不够一批／失效燃料 | 不足时不工作；history为0时清不足残液；已不在可执行燃料候选中的输入会清空。当前检查enabled／非fake，缓存不可用时再找候选。 |
| 朝向 | 热只向UP；冷液从FACING，放置／扳手只允许水平或DOWN，不允许正面UP。原getValidSides为BOTTOM_HORIZONTAL，与原热顶面独立。 |
| 外观与碰撞 | 原撞箱高7/8，当前COLLISION为0.875；活动接触热伤害仍是min(10,r/10)。本轮未实测边缘碰撞、hurt无敌帧或实体免疫。 |

HotFluidFuelCycle是真正被单块及大型设备共同调用的原生燃料边界，HeatExchangerRules.charge为共享数值规则，未新增平台业务副本。原Loader_Fuels:191–213的16条hot定义，包括Blaze无产液、Lava、Volcanic Lava、三热水别名、地热水及九类堆冷却热液，在OriginalFuelRecipeRows:41–56有对应。FuelRecipeLoader.hot读取这些行并注册FuelRecipeMaps.Hot；缺失外部输入流体仍跳过。普通Lava的80HU/L与工业热冷却液20HU/L均未再误按桶乘1000。

有限普通例：钨单块r=128、效率10000，初始heat=0、Lava输入1000L、cold=0，目标256；80HU/L需4批，旧循环与当前charge均消耗4L、产4L冷液、存320HU。钨钢效率9000时每批72HU，同样4批得288HU。此为给定输入算术，不是执行过原生配方或世界。

## ALIGN-219：运行状态提供者与已有110／111的范围

原类实现ITileEntityRunningActively，而该接口继承RunningPassively（并继续提供possible）。源末尾三个getter分别返回mActivity.mActive、mActive||hot.has()、mActive。原CoverDetectorRunningPassively／Actively只要求可tick及对应接口，接受此类即使它没有SwitchableOnOff；onTickPost读取getter并输出15／0。

当前HeatExchangerBlockEntity只实现IFluidHandler、FluidToolTarget，父GTEnergyBlockEntity也未实现MachineControl.Provider；没有这个类的独立machineControl／running／active提供者。MachineControl.find只看Provider，不根据LIT或字段名反射。通用覆盖路径PanelCoverHost.coverControl因此退到CoverProgress；该fallback的running／active恒false、supportsSwitch=false，不能从实际active字段得到状态。

实际安装还有已登记110的同源门控：PanelCoverRuntime.canAttach对所有MachineCoverSpec要求supportsSwitch，所以当前fallback连正常运行／活动检测器也不予安装。即使只修安装门控，fallback也仍不会给本宿主提供真实running／active；反过来，只提供只读running接口却supportsSwitch=false，也不能绕开现有安装条件。报告应区分这两个接点，不以“无同名原接口”直接猜全部红石失效。

possible的旧mActive||hot.has()与现代默认active||progressMax>0近似属于既有111的语义范围，不再独立计数。此类本身原没有开关接口，因此不能把当前缺少开关误报成热交换器应能红石停启；缓冲热继续输出亦是明确原行为。主燃料、热量与LIT仍运行，不应把状态API缺失写成换热本身停摆。

## ALIGN-220：周围起火目标与可燃准入不同

两者都有同一效率抽签和坐标范围：x／z偏移−2..2，y偏移−1..2；有缓冲热输出时才尝试点火。原调用WD.fire(world,pos,true)，不是任意附近空气无条件点燃。

原WD.fire:706–724排除岩浆／已有火，接受地毯或无碰撞目标；目标本身可燃时可直接替换为火，或在六邻有箱子／可燃块时点火。当前tick只接受level.isEmptyBlock(fire)，再取BaseFireBlock.getState并要求flame.canSurvive。这保留周围起火效果，但没有保留原准入语义。

有限反例A：抽中目标为空气，其下是非灵魂系坚固石面，其余邻块均不可燃且不是箱子。原WD.fire(true)找不到可燃邻居，不放火；当前两版FireBlock.canSurvive接受“下方顶面坚固”这一独立条件，所以会尝试setBlockAndUpdate放火。Forge实际class与Neo源码的canSurvive均核过，不是仅凭方法名称推断。

有限反例B：抽中可燃地毯或可燃无碰撞目标，原可替换该目标；当前isEmptyBlock先拒绝非空气。没有把原第三方节点／IItemGT特例、现代SoulFire、火焰随后自然熄灭／蔓延或群系规则全部当作应直接照搬的缺陷。本项需明确火险政策取舍，不建议为“对齐”删除现代原生存活规则而不评估后果；本轮没有实地随机点火。

## ALIGN-221：玩家排液先被通用1000L行为接管

原GT_Tool_Plunger.onStatsAddedToTool:85–87按次序加入Behavior_Tool、物品管fallback、流体fallback；MultiItem.addItemBehavior追加到列表，onItemUseFirst:209–213顺序迭代，第一次true即返回。所以原专用onToolClick2已经把冷罐整罐清除并返回正成本时，不再运行通用1000L流体fallback。

当前真实玩家先进入GTToolItem.onItemUseFirst:83–99。MultiblockTools.use只处理建造杖／放大镜及MultiblockToolTarget，单块热交换器不在其中；随即对PLUNGER先调用BehaviorPlungerFluid。冷罐非空时，它经两端真实handler.drain先模拟再支付固定COSTS=100、执行最多1000L并返回SUCCESS，因而不会再进入HeatExchangerBlock.use→BE.interact的专用整罐分支。

这次继续核到本机原生玩家分派：Forge ServerPlayerGameMode.useItemOn在offset128调用ItemStack.onItemUseFirst，结果非PASS便于143直接返回；Neo对应源码:354–357亦在后续blockstate.useItemOn之前，结果非PASS立即返回。两端ItemStack扩展把该调用转交到物品的onItemUseFirst。所以未被外部RightClickBlock事件拒绝／覆盖的普通交互中，这个SUCCESS会实际截断后续Block回调，不只是方法内部推测。外部模组取消或改写交互的情况不在见证条件中。

有限状态：钨单块r128、cold=2000L、工具可用且足够耐久、非创造、无其它拦截，冷罐阻塞后可自然积累到该值且低于2560背压。旧专用入口会清完2000L；当前玩家fallback只排1000L并余1000L。不能仅阅读BE.interact的setEmpty就宣称玩家仍会整罐清除，也不能把通用fallback本身的1000L规则报成原版不存在——差异是它提前于专用入口执行。其它宿主的优先级不在本稿外推。

## 既有126补充：可达专用排液器入口固定1点

原HotFluid.onToolClick2先排cold，有冷液便不碰hot，否则排hot，返回GarbageGT.trash(tank)；CS.GarbageGT实际用remove返回量传给trash并返回L数。原Plunger工具注册Behavior_Tool(...,100,...)，后者按units(return,10000,100,true)请求耐久，等价ceil(L/100)。当前BE.interact的直接清罐优先级一致，但只调用damageForUse(held,1,player)，不使用已有damageForToolClickReturn。

有限合法状态：钨输入罐容量1280L、cold空、hot为1000L有效燃料，非创造、普通未附魔排液器点击。当前通用fallback的drain仅看cold，因cold空而返回false，随后能到达专用BE.interact；ToolInteractions.describe只为扳手提供Spec，不会抢占排液器。旧请求耐久为10，当前固定请求1。这里只比较传给工具耐久层的请求，不把附魔减损或具体工具电量后的实扣冒称已测。双罐都空时不扣除；放大镜旧返回1对应当前1点已有，不报所有工具消费错误。按根代理要求，这一具体宿主可扩展已有ALIGN-126，不再新增同根编号。原工具反馈声音若需扩围应先沿132共用反馈核查，本轮不另宣称。

## 世界保存／物品态与大型设备归属

当前saveAdditional／load(Additional)两端实际保存hot／cold的long数量及流体身份、gt.energy、active／64位history。hot容量记录亦由FluidTankGT保存读取；未运行独立进程重启。原writeToNBT2保存energy及双罐，但TE_Behavior_Active_Trinary.save实际是从已有tag回读而非写出，不能把当前正确写出history反说成丢原状态，也不能凭该旧方法名宣称原历史保存完全正确。

普通拆卸当前getDrops给普通方块物品，不带热量／流体。原getDrops沿Base04.writeItemNBT至Base05／06／07父链，没有调用本类世界writeToNBT2存热液，所以普通拆卸不保这些内容与原路径对应。覆盖板／涂色的通用物品保存另由既有共用路径处理，本轮未对其全部字段重审；不能因没有portable内容把热交换器误当便携储罐。

HeatExchangerData.settings／restore的实际调用者主要是LargeHeatExchangerControllerBlockEntity与大型主控提示。单块构造从HeatExchangerBlock.spec().settings得到固定材质规格，不调用HeatExchangerData；该helper存在不能作为单块支持任意旧可变NBT配置的证明。旧类可读output／eff／fuelmap／energyId，当前八个固定ID只映射默认注册配置；自定义旧NBT及跨版本迁移属于适配边界，本稿未凭此登记正常默认机器错误。

原大型主控17197继承同一HotFluid基类但另有结构与八个热出口。当前大型BE独立管理结构／配置，复用HotFluidFuelCycle及HeatExchangerRules；本轮只核这个共用函数归属，不再审大型结构、8出口、工具绑定和旧独立多方块保存。HU缓冲stored／capacity当前公开给查询，原单块未覆写Root的零值，属于查询暴露差异，未证明可额外提取复制热量，不列能源守恒错误。

## 来源与未验收范围

[统一回执](verification/nuclear-solar-heat-audit-20261021.json)保留原／当前绝对与相对路径、SHA256、双端维护源投影、原生火焰工件／条目／javap身份、实际注册／配方调用和有限算术。旧报告中的运行通过只代表其指定旧提交与Neo场景，不能转写为本轮已跑过或Forge全面验收。

未运行测试、资源包生成、原生类初始化、编译、Gradle、游戏、自然放火或保存重启；未穷举long溢出、第三方动态RecipeMap handler、不同组件热液、覆盖物能源过滤、自然生存获得、所有提示颜色或现代火焰适配。共享饱和计算与现代原生边界不因和旧Java溢出／旧世界规则不同就自动要求回退。


## 根代理复核与补充

根代理从原add调用的第3项识别真实注册ID（第4项9103是共用类型参数，不能也当作ID命中），重新解析八项额定／效率／stack16及两种太阳能额定／stack16。给定r128／目标256、Lava80HU/L，100%四批320HU、90%四批288HU；cold2000L低于20r2560，通用排出1000后余1000；cold0／hot1000的专用耐久请求10／1。仅为源码标量／算术，不运行换热、玩家工具或随机火焰。

interact中的放大镜仍直接使用Component.literal的Input／Output，归既有003未入语言系统的宿主扩围，不据两个字样推断全部工具提示。原生火焰与onItemUseFirst截断入口、可达分支及未验边界见[统一回执](verification/nuclear-solar-heat-audit-20261021.json)；不擅自改变火险／耐久／交互或覆盖板行为。

## 第六十八批大型设备边界

大型17197的实际专题现见[大型换热器](LARGE_HEAT_EXCHANGER_AUDIT.md)：主链、17件建造和Neo能力已有，新增242–243。它已有只读MachineControl.Provider，不属于本篇219的缺提供者；统一检测器switch安装门控沿110扩宿主。它也没有单块设备的专用整罐plunger，因此不直接扩221。共有HotFluidFuelCycle／HeatExchangerData／HeatExchangerRules本批仅整理排版与局部命名，十份执行流保持，当前批次没有重跑本篇旧Neo场景。
