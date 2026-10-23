# Von da Graagg 禁刷怪塔的供电、范围与事件核查

第六十九批，2026-10-21，基线`6ef1cacca`。核原17996主控的实际注册、塔体、供电入口、禁刷怪事件、仪表／工具和世界保存；新增ALIGN-244–247，扩036／077／218／232／233。四份源码展开并明确局部名称，预览引用已有core布局；没有修改供电、刷怪或保存政策。

原版只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。27份原来源、49份当前输入、34份共享原生源的68份平台视图，以及4个本机原生工件、14个entry和11份只读javap视图见[回执](verification/von-da-graagg-audit-20261021.json)。源算术、接口分支和事件顺序推导不代表游戏验收。

## 已有的实际主链

原Loader_MultiTileEntities:1280注册17996，材料SteelGalvanized、硬度／抗爆6／6、stack16、接受EU，制作布局CSC／PMP／CEC。当前SharedLargeMachineParts→GTMultiblocks→RegisteredBlock→MultiblockBlockFactory确实创建VonDaGraaggControllerBlock；GTBlockEntities两端绑定专用BE，服务端ticker调用serverTick。不是只有一个被动部件或预览模型。

当前制作表已有相同布局、镀锌钢机器外壳、红宝石晶体处理器、六级电路及下界之星／末影之眼位置；原宝石矿辞和当前具体物品的完整候选范围未在本批验收，不据定义行声称全部外部配方兼容。

塔体63件：41个18028镀锌钢基座壁板、5个18040铜线圈、17个18029致密钢壁板。基座是去四角5×5的两层，底中心为主控；中柱五线圈，顶中心另加一钢壁板，再包16件冠部。原Util遇到主控自身直接成功，所以原循环中的主控调用不算支撑件。原调用、core布局和旧原生预览按完整有序坐标／材料重建一致；当前预览改为读取VonDaGraaggRules.layout，保留每件BlockPos与LargeMachineParts.block的顺序和最终Map.copyOf。

形成检查仍是当前完整候选后一次认领，首次错误清掉旧绑定；原逐格保留有效认领、四角区块不可用时保留旧形成值，扩036。主控和部件实际能源／传感器消费者均已追到，不能以预览存在代替认领合同。

## 范围、耗能和苔石豁免可以对应

原先计算范围，再扣4096EU；当前顺序相同。形成时范围为`min(255, floor(min(energy,4096)/16))`，未形成范围0，但仍扣电至不低于0。UT.Code.bind8实际夹到255，原传感器上限却为256；当前保留这个差别，不能擅自把实时最大改成256。

给定形成结构和非负电量，15EU→范围0、16→1、2048→128、4096→255；最后一种在同刻扣完后剩0EU。8192EU时范围仍255、刻末剩4096。以上是有限源算术，未在世界中供能。

禁刷怪判断是同世界的水平正方形，X／Z距离各不超过范围，不是球形，也没有额外垂直半径限制。刷怪点同X／Z、Y±5含原版苔石，或GT石材的苔石变种时豁免。当前StoneVariant.COBBLE_MOSSY对应原BlockStones.MCOBL=2；现代世界边界和负Y按现代规则处理，不恢复原代理把Y夹到0以上的旧高度限制。

当前ACTIVE使用弱ServerLevel键和纯坐标集合，值不反向持有世界。serverTick在正范围注册，否则注销；setRemoved亦注销。刷怪处理跳过未加载主控并确认当前位置仍是本类BE。两端实际LevelChunk.clearAllBlockEntities均调用onChunkUnloaded及setRemoved，不能因为本类没另写onChunkUnloaded就报卸载残留。本轮只追该真实清理路径，不声称所有卸载、异常和长期内存场景已验收。

## ALIGN-244：主控进电面和大电包合同收窄

原类重写isEnergyAcceptingFrom，仅检查能源身份，不检查side或可贴面。Root.doEnergyInjection调用这个虚方法，所以原主控六面均能进电，尽管原tooltip写底面。当前isEnergyType／hasEnergySurface与能源类型枚举都限制底面或null，另外五面拒收，电容发现也随同隐藏。基座输入墙仍可从六面输入；不能把主控限制扩大成整个基座只有底面可用。

原getEnergySizeInputMax报告4096，但原doInject没有按这个上限拒收或触发过载，只累加abs(amount×size)。给定底面、空缓冲、一个8192EU包，原公共入口反馈1并存8192，下一刻范围255／剩4096；当前doInject明确packet>4096返回0。这是实际接收合同差异，不是原会爆炸的证据。

两边有效底面的公共入口对低于256的EU包都返回已消耗数量而不存能：当前继承EnergyGate，不能套用Dynamo的239误报成本机也绕过了门控。4096也不是当前缓冲硬上限，多次合法包同刻仍可累积超过它。当前Long.MIN_VALUE／溢出保护保留；getEnergyDemanded从原能量差额改为非负包数，也不能脱离现代接口单位直接回退。

## ALIGN-245：刷怪事件优先级改变了互操作顺序

原GT_API_Proxy.onCheckSpawnEvent以LOWEST运行，并在已经DENY时返回。当前两端onSpawnPositionCheck的SubscribeEvent都未指定priority。本机实际Forge／Neo总线注解的AnnotationDefault均为NORMAL；监听列表按枚举优先级拼接，总线继续调用后续监听者。原FML的[EventPriority](https://raw.githubusercontent.com/MinecraftForge/FML/1.7.10/src/main/java/cpw/mods/fml/common/eventhandler/EventPriority.java)和[ListenerList](https://raw.githubusercontent.com/MinecraftForge/FML/1.7.10/src/main/java/cpw/mods/fml/common/eventhandler/ListenerList.java)也明确LOW在LOWEST之前。

两版现代checkSpawnPosition及Spawner变体真实消费者均先post，再读取最终结果：Forge ALLOW、Neo SUCCEED可直接允许。给定有效范围／无苔石豁免，并有LOW优先级附属监听者明确允许，当前NORMAL先拒绝、LOW后允许，最后可以放行；原LOW先允许、GT LOWEST后拒绝，最后DENY。此为条件性事件兼容差异，没有加载addon或实际派发刷怪事件，不报告为原版自然刷怪普遍失效。原代理的其它全局出生保护不在本机事件范围内。

## ALIGN-246：世界保存保留剩余电量，属于当前保留政策

原mEnergy初始0，readFromNBT2仅读取能源身份；没有本类writeToNBT2。已沿Base04空钩子、库存／覆盖板／朝向父链核查，没有保存这个本类字段的分支。原mCurrentRange初始256也不代表正常运行可超过255：首次服务端tick会先重新计算，当前不恢复这个临时初始值。

当前save／load显式保存gt.energy和gt.range，非负夹限，不把合法累积电量截到名义4096。给定同刻三个合法4096包、经过一刻耗能后保存8192EU，原新实例能量0、形成后首刻范围0；当前恢复8192、形成后首刻范围255且剩4096。记录的是保存合同差异，**保留当前避免丢电的改善，不要求恢复原重载清空**。这不是磁盘重启验收；普通掉落由Block返回无运行数据的ItemStack，与世界保存分开。

## ALIGN-247：原可设置的接受能源身份未接入

原读取NBT_ENERGY_ACCEPTED，CS实际键为gt.energy.accepted，经TagData.createTagData替换mEnergyTypeAccepted，主控、基座和电容接口都使用它。当前本类在相应接口中固定ELECTRICITY，load只读energy／range，没有读取接受类型。

即使选原版与当前都认识的ENERGY.KINETIC_ROTATION（RU），原合法设置会改变接受身份，当前仍EU。它不只是242的未知新身份注册问题；这里已有已知身份也未读取的具体字段缺口。默认注册原本就是EU，不能据此报默认塔完全无法供电；旧格式迁移和自定义能源的实际运行仍待验。

## 仪表、工具和既有记录扩围

当前本类实现ProgressSensorSource／CompressionSensorSource，分别返回range／256和1000×range／256000。SensorBlockEntity的PROGRESS及GIBBLOMETER分支实际消费它们，形成部件的sensorTarget亦可转发主控，专用范围传感器不是全部缺失。

普通进度覆盖板走另一条链：后备PanelCoverHost→MachineControl.find没有本机Provider，继而CoverProgress读取能源电容剩余量。底面有EU电容所以能提供进度，却把范围替换成电量比例。形成塔一个4096包后的时刻，原CoverScaleProgress得到255／256、显示刻度14；当前底面得到0／4096、刻度0。PanelCoverRuntime中PROGRESS是独立安装资格分支，不能误套运行检测器统一要求开关的拒装规则。这里扩077，未实际贴板或读取红石。

| 既有记录 | 本轮增加的具体范围 |
| --- | --- |
| 036 | 完整候选认领与原有效子集、关键区块未加载时旧形成缓存不同 |
| 077 | 普通进度覆盖板读剩余电量；专用进度／压强传感器已有范围接口，二者分开 |
| 218 | 17996原stack16，当前RegisteredBlock的普通BlockItem默认64；已核能源设备20→21项 |
| 232 | 每tick及有效部件能力查询重建63格列表／候选并绑定，完整20次查询1260格；原未变更查询用缓存，不是TPS实测 |
| 233 | 主控没有MultiblockToolTarget，原Base10建造棒、形成诊断及本机Square Radius详情没有专用入口；预览存在不能代替工具 |

原类没有OnOff／Running接口，Base10的默认环境检查也是直接true，不凭现代命名另补“开关缺失”或雨火爆炸。朝向／外观尚未完成独立原生渲染验收，本批不根据getDefaultSide与getValidSides的表面矛盾推断新故障。

## 源码整理和证据边界

[core范围／布局](../../core/src/main/java/com/gregtech/gregtech/content/multiblock/VonDaGraaggRules.java)、[主控BE](../../multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/VonDaGraaggControllerBlockEntity.java)、[方块入口](../../multiversion/src/main/java/com/gregtech/gregtech/block/machine/VonDaGraaggControllerBlock.java)和[刷怪事件](../../multiversion/src/main/java/com/gregtech/gregtech/content/multiblock/VonDaGraaggSpawnInhibitor.java)展开为可逐方法阅读的代码，out／parts／part／pos／be及ticker参数按各自绑定改为cells／candidateParts／collectPart／partPos／machine等明确名称。公开API、字段、常量、运算和条件顺序不改。

完整有序执行词法及字面量位置在上述限定区域绑定归一后保持：core484，BE Forge1701／Neo1729，Block409／451，事件576／558。不是全文件替换machine之类的词，以免误改包名。两条说明分别限定为GT6来源与“当前移植版底面进电”，其余注释／许可证／Stonecutter指令保留。

[ControllerStructureLayouts](../../multiversion/src/main/java/com/gregtech/gregtech/content/multiblock/ControllerStructureLayouts.java)仅vonDaGraagg方法改为core委托，并纠正类注释对实际验证消费者的过强描述。方法外Forge1439／Neo1446项保持，方法内通过旧原版／core／旧预览的63个有序单元重建及新委托语句核对，**不把这次共享改动宣称为整个方法词法不变**。现有核心生成列表为确定性纯数据，引用不引入世界依赖；异常分配、类初始化和实际预览仍未运行验收。

原MultiTileEntityVonDaGraagg头载GregTech-6 Team2025、Gregorius Techneticies、LGPL-3.0-or-later；其余原来源各自保留头及摘要。没有把外部总线或原生源码拷进生产；CompoundMaterials／WoodMaterials摘要保持。未Java编译／目标初始化、Gradle、测试、游戏、pack生成、供电／刷怪／安装仪表、磁盘重启、构建或推送。全面核查仍继续，正式247条包括已修复／部分修复、扩展合同和保留政策，不是247个待修故障。
