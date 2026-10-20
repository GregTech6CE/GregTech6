# 大型发电机输入、转换与端口合同核查

第六十七批，2026-10-21，基线8950a1686。核实际注册17221–17224四档大型发电机，沿公共注入入口、轴向主控、后部部件、覆盖板宿主和客户端ticker追踪。新增ALIGN-239–241，036／111／218／231／232／233扩宿主；不把原有保护性适配自动改回危险行为。

原版只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。24份原来源、44份当前输入、33份共享原生源的66个平台视图、两个既存工件的两个LevelChunk entry，以及五份源码的完整执行比较，见[回执](verification/large-dynamo-audit-20261021.json)。这些身份只覆盖下述声明和消费者，不代表整份大文件或整个系统已验收。

## 实际注册与已有普通转换

原Loader_MultiTileEntities:1259–1262注册四档。当前SharedLargeMachineParts→GTMultiblocks／RegisteredBlock→MultiblockBlockFactory实际创建LargeDynamoControllerBlock，GTBlockEntities.LARGE_DYNAMO收集这四块；Block.getTicker在服务端调用LargeDynamoControllerBlockEntity.tick，执行交给共享AxialGeneratorBlockEntity。不是仅有目录或预览而没有机器。

| 原ID／材质 | 额定输入RU | 额定输出EU | 容量RU | 墙ID | 硬度／抗爆 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 17221 不锈钢 | 4096 | 3072 | 8192 | 18022 | 6／6 |
| 17222 钛 | 8192 | 6144 | 16384 | 18026 | 9／9 |
| 17223 钨钢 | 16384 | 12288 | 32768 | 18023 | 12.5／12.5 |
| 17224 精金 | 131072 | 98304 | 262144 | 18025 | 100／100 |

当前OriginalGeneratorParameters.DYNAMO从同档参数推导，Factory把相应物性传入Block.Properties，两端共用这些值。源表逐行解析与当前推导均符合上表，额定效率75%，输入范围R/2–2R，最大输出2O。没有据默认强度或其它设备的规格猜测本机物性。

每台结构为实心3×3×4，主控居前端中央；除主控外35件，其中17墙、18个18040大型铜线圈。原轴向min/max与当前StructureGrid.offset按六方向分别重建，位置、铜线圈所在两内层以及后端中心角色相同。后部端口位于主控后3格，实际接收器在后4格。主控正面接受RU，后端主动推EU，不能因主控isEnergyEmittingTo返回false就说整台机器不发电。

普通输入按`energy × output / input`向下取整，每刻最多一个可变大小包，达到半额输出门槛才尝试发送；无论接收端是否接受，正常转换分支仍消耗缓存。最低档3000RU可尝试2250EU，4096RU尝试3072EU，8192RU尝试6144EU，与原waste=true转换一致。当前没有大型燃气机的“攒满最大批次再排队”问题，不能把229／230泛化到本机。

停机阻止后续RU注入，但已存能量仍可能完成一次转换；原Base11的doConversion也没有以mStopped阻断这段余能，不列新缺陷。当前两端保存／读取gt.axial.energy、gt.stopped、gt.overloaded，并在缺新能量键时读取早期移植版gt.ru；这不是已经证明能直接迁移1.7.10整份世界，未做重启验收。

## ALIGN-239：低于最小输入包的上游反馈不同

原真实入口是Root.doEnergyInjection:717，而不是只看Energy_Stats.doInject。前者先查类型／方向和最小包：低于min且不是size-irrelevant能量时，直接返回aAmount，不调用doInject，也不存能量。原Util.insertEnergyInto把这个返回值交回发送者，表示包已消耗。不能因Stats本身没有min判断就推断原版能把多个低速RU包累加发电。

当前共享EnergyGate其实也保留这种语义，但LargeDynamoControllerBlockEntity重写公共doEnergyInjection，直接调用本类doInject；后者在size<R/2时返回0，绕开了共享门控。对已形成／开启、正面RU输入的最低档，一次提供4个1024RU包：原返回4且缓存0，当前返回0且缓存0；两边都不产电，上游余能或其它接收器选择却可能不同。模拟调用也有相应返回值差异，未运行发送网络。

这是明确的反馈政策差异，不是效率公式或储能缺失。本轮只记录；保留能量与恢复原丢弃行为应在功能修改时明确取舍。

## ALIGN-240：原运行声音没有对应客户端触发链

原Base11.onTick2客户端分支在mActivity.mState非零、输入或输出为RU且WD.random(...,20,CLIENT_TIME)满足时播放SFX.MC_MINECART；CS把它定义为minecart.base。Trinary活动检查会将非零历史映射为状态1或2，真实服务端转换与原同步状态为这段声音提供条件。这不是仅有一个声音常量却无人使用。

当前Dynamo块在level.isClientSide时直接返回null ticker，BE及轴向父类没有客户端运行声音；共用GTEnergy同步、后备覆盖板服务和两端客户端注册也没有替代这个分支。两版原生LevelChunk消费者均先取BlockState.getTicker，null时移除ticker：Neo读取既存源，Forge用既存工件只读javap核对，未初始化游戏类。蒸汽／燃气的独立转子renderer不能据此当成Dynamo声音入口。

记录仅指已追到的原运行反馈缺口，不宣称模组全部机器无声，也不声称已在客户端听验或验证第三方音效修改。

## ALIGN-241：后部能源端口的方向查询放宽为六面

原后端中心的ONLY_ENERGY_OUT部件先检查模式，再将isEnergyEmittingTo经IMultiBlockEnergy回传主控；Root的部件适配继续调用Base11查询，而Dynamo.isOutput只接受mFacing的反向。因此在有效六方向中只有真正后方输出面返回true。

当前MultiblockPortBlockEntity.isEnergyEmittingTo只检查owner、输出角色／类型和结构，不使用传入side；轴向emitsPortEnergy也仅检查ENERGY_OUTPUT角色和类型。给定已形成、无覆盖板的EU后端端口，六面查询都可为true。此返回值可影响外部连接或能力判断，不能仅凭实际tick位置正确就认定接口也一致。

当前convertAndEmit仍直接查主控后4格，只向真正后方接收器注入；部件getEnergyOffered固定0，未提供第二份可抽缓存。因此本项是接口元数据差异，不能描述为已确认侧向输电、双份缓存或能量复制。没有运行外部连接器验证。

## 既有记录扩围与保留政策

| 记录／边界 | 本轮证据与限制 |
| --- | --- |
| 036 | 当前轴向父类遇错／未加载即清已认领部件，仅完整候选整体绑定；原逐件认领且缺关键区块保留旧状态，同大型涡轮的既有差异扩到四Dynamo |
| 111 | 原Base11.getStateRunningPossible恒true；当前Provider虽能开关／读取运行，coverPossible用active或progressMax>0近似，空闲时可false。不报整个开关接口缺失 |
| 218 | 四档原stack16，当前默认BlockItem64，已核能源设备由15扩到19；没有据此验收全部物品 |
| 231 | 本机8193RU超限包也直接调用轴向overload，首次置overloaded／stopped并清能量，且两端保存；原首次保护只计数／清缓存，不锁停。手动复位、远程不能解除的既有差异扩宿主，不恢复原破坏／爆炸 |
| 232 | 本机共享同一35格重扫／绑定父类；每次有效能力查询可能重建Map并重复绑定，仍是源调用次数，不是TPS实测 |
| 233 | 当前Dynamo及轴向父类无MultiblockToolTarget，部件即使有owner也无法转发专用建造／形成诊断；普通预览与普通平面建造并非替代 |

保留当前整包容量上限：最低档空缓存一次接2个5000RU包，原Stats向上取整会收2包到10000RU，超过8192容量，转换7500EU又超6144最大输出；当前只收1包，转换3750EU。这个源见证说明直接恢复原ceil可能引入原有过载，不把有界接收当成应立即撤销的缺陷。

未把139的涡轮正面禁贴扩到Dynamo：它继承Base11／Base10，未经过LargeTurbine.allowCovers，原Covers默认允许各面。当前BlockEntity覆盖板Mixin实际补host，不能因声明没写PanelCoverHost就报所有覆盖板失效。普通挖掘的完整运行数据仍经配置CoverDropMixin清理，不以AxialGeneratorBlock.getDrops局部快照误报为保留RU；普通取样或其他特殊物品路径需另核。原Base10默认环境检查返回true，也不将017的雨／火风险直接推广到此机。

## 五份源码可读性

LargeDynamoControllerBlockEntity和Block展开注入／需求／ticker及两端旧键读取；AxialGeneratorDefinitions展开布局构造、档位映射和注册列表；AxialStructureTransform展开坐标边界，SharedTurbineStructure展开三重循环。八个局部／lambda绑定由短名改为cellPos、parameters、firstOriginalId、offset和ticker参数名称；公开类型／签名／字段／注册ID、常量、字面量、运算、条件与语句顺序不改。

完整执行流比较为BE Forge732／Neo745、Block236／236、档位570／570、坐标255／255、共享结构199项；只归一列明的绑定，坐标offset变量与origin.offset方法名明确区分。注释和Stonecutter指令保持，用户CompoundMaterials／WoodMaterials摘要不变。源证明不是编译或游戏测试。

原Dynamo为GregTech-6 Team2025、Gregorius Techneticies、LGPL-3.0-or-later；各引用来源独立留头与摘要。本轮只整理既有移植源码，没有把原生或原版方法体／资产新复制进生产。未运行Java编译／目标初始化、Gradle、测试、游戏供能／声音、存档重启、构建或推送。大型换热器、其余能量、配方／模型／材料、控制器和全面队列仍继续，不能由三个新记录推算完成比例。
