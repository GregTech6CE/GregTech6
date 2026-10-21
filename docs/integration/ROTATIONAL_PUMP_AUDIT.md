# 旋转泵原版合同与源码职责核查

第六十四批，2026-10-21，基线977554e1e。四档旋转泵实际注册、方块ticker、RU接入、世界抽液与两端能力链已追；本批新增ALIGN-222–228七项合同，218扩展至这四档物品。没有修复泵玩法，也没有把已有泵覆盖板ALIGN-098算成这台抽液机器的差异。

原版只读根为`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为`C:/Dev/GregTech6`。[回执](verification/rotational-pump-audit-20261021.json)保存17份原来源、24份当前输入、30份完整平台视图，以及两个本机原生工件的8个entry身份；它们是具体源链证据，不是这些整份文件的全面行为验收。

## 实际所有权与已有路径

| 层 | 实际入口与职责 |
| --- | --- |
| 原注册 | Loader_MultiTileEntities:1123–1128，第三参数16001–16004才是物品身份，第四16002为共用类型；NBT_INPUT为32／128／512／2048 RU |
| 共享参数 | core PumpCatalog.all生成四条PumpSpec；PumpWorkRules存当前扫描与输入数值，不操作世界 |
| 原生注册 | Forge MachineBlockBootstrap和Neo GregTechNeoForge都调GTPumps.registerAll；GTBlockEntities.PUMP绑定这些实际方块 |
| 实际tick | PumpBlock.getTicker在服务端直接返回PumpBlockEntity.serverTick；不存在另一个core抽液算法替换它 |
| 两端流体入口 | Forge PumpBlockEntity.getCapability返回tank；Neo EngineCapabilities注册PUMP并转调fluidHandler，同样返回tank |
| 合成 | OriginalHandCraftingRows.rotationalPumps取这四条实际holder，按tier选择小／中／大／巨型不锈钢管，双层机器外壳／齿轮／不锈钢转子／扳手与GwG、PMP、RPR图案已有对应 |

四档额定数值、8192 RU启动门槛、普通1000 L源消耗2048 RU，以及先输出旧液体再检查输入缓冲已有。当前输出跳过正反两个轴向面，保留四个垂直轴面的实际推送；无RU也会尝试清空旧液体，未接收完则不移除新的世界源。不能概括为“泵整台未实现”或“没有配方”。原ANY.Steel与当前Steel在所有材料域的等价、全配方解析和游戏合成未在本批验收。

## ALIGN-222：相连液体搜索被独立区域扫描替代

原MultiTileEntityPump.onTick2／scanForFluid／addToList先从正面相邻块确定可抽流体集合。水与三种GT水同族，岩浆同族，其他IFluidBlock按其实际块身份；无入口液体时耗1 RU并等待重查，不凭远处液体启动。每轮只沿已连上的液体水平扩展；在液体重力方向相反的一层发现同族液体时清队列并切换层。普通液体mDir=+1，较轻流体为-1。水平可达边界为入口X/Z±64，不能把它读成任意128×128方形内的每个位置都可抽。

当前serverTick不验证入口液体、没有连接队列／同族集合或轻流体方向；每次取pumpX/pumpZ指向的一列，从入口高度-64向上到入口高度，选择第一个source。初始pumpX=0/pumpZ=0时，相对入口是(-64,-64)列，不是入口本身；128个游标值给出[-64,63]。完成一列设scanCooldown=2，之后两个有资格tick返回；无能量或罐非空时游标冷却也不推进。原mNextCheck却每服务端tick递减，并有3000／20刻重查规则。

有限条件：入口为空气，而初始扫描列的合法高度有一个已加载、孤立的原版水源；RU≥8192且输出罐空，setBlock成功。原版不把该源加入相连队列；当前可直接移除它并取得1000 L。反之入口上方相连液柱属于原搜索可达范围，当前固定向下64格的列扫描不能表达同样的分层搜索。本批没有生成此世界或把所有区块边界行为宣称相等。

## ALIGN-223：实际RU输入范围与低速处理不同

原泵只覆写recommended=mInput，继承Root:724–725的min=R/2、max=2R；Root:717是实际doEnergyInjection入口。RU不在TD.Energy.ALL_SIZE_IRRELEVANT中，所以低于min的非零包返回aAmount而不存入泵能量；不能写成“低速必定返回0”。有效包进入泵doInject并按绝对值存储。超过2R时原泵进入overload，先累计保护、清缓冲，足够次数后才overcharge，破坏又受原配置约束。

当前覆写min固定8、max=R，并由PumpWorkRules.accepted再检查packet≥8且≤max。四档实际数值如下：

| 额定R | 原有效存能范围 | 当前有效存能范围 |
| --- | --- | --- |
| 32 | 16–64 | 8–32 |
| 128 | 64–256 | 8–128 |
| 512 | 256–1024 | 8–512 |
| 2048 | 1024–4096 | 8–2048 |

因此青铜48 RU是原有效输入，却被当前拒收；青铜8 RU在原被消耗但不入缓冲，当前会存入。当前超限直接返回0，没有泵原有的保护计数／过载反馈入口。本项描述源链差异，不是要求在可读性修改中恢复爆炸或移除Long.MIN_VALUE等安全检查；源设备是否真的供出这些包还需结合其输出合同另验。

## ALIGN-224：整包向下取整可使合法恒定输入永远低于启动门槛

原doInject以8192-mEnergy为需求，包数向上取整，允许最后一包令缓冲略超门槛；当前accepted和getEnergyDemanded都做room/packet向下取整。它不是只在手工构造8191档案时才有差异：从空缓冲持续输入青铜范围内的17 RU包，当前最多接481包，得8177，余15<17；之后接受0包，serverTick又因<8192不抽液消耗，保持相同输入便无法越过门槛。原可接482包到8194。

这与223的输入范围独立，17在两边均为有效范围。有限算术不运行目标算法／注册表；没有宣称所有泵都必定停机，额定32等整除门槛的供给可达8192。修复需明确整包预算与容量合同，不能把旧溢出极值一并照搬。

已有PumpTransferRepairTests仅验证额定32包累积，并明确把手工载入8191后拒包当预期；该断言恰好固定当前下舍入，不能作为原版舍入一致的证据。测试中的pumpX/Z也被手工设64以直接对准入口，不能据其源抽取用例证明实际初始区域／连接算法。只读了测试，没有运行或增加新测试。

## ALIGN-225：世界抽液的准入与回调被source状态替代

原addToList加入同族流体块，drainFluid也处理流动水／岩浆块；只有metadata0时才取得1000 L，非源仍以最低16 RU移除液块。IFluidBlock路径则查询该块的drain(..., false)实际结果，再按所得量计算成本。当前只接受非空FluidState.isSource，恒定制造该fluid类型的1000 L后直接把整个BlockState替换成空气；没有世界块排液回调，也没有区分“流体块”和“含源流体的实体方块”。

本机两版原生SlabBlock.getFluidState都在waterlogged=true时返回WATER.getSource(false)。在已加载的扫描格放一个合法含水半砖，满足8192 RU／空罐／setBlock成功且无外部覆盖时，当前条件成立，随后请求把**整个半砖**换为空气并储存1000 L。现代SimpleWaterloggedBlock.pickupBlock的正常入口是先把WATERLOGGED改false，不能作为当前已有的补偿，因为泵没有调用它。本例证明实际调用链的对象准入风险，没有游戏目击，也不声称1.7.10原本存在现代含水属性或所有第三方回调必然有损。

当前先simulate整桶容量、块移除成功才execute入罐，并在实际接受不足1000时尝试回滚，这些保护应保留；不能因为原版先填罐再setBlock就恢复复制液体的旧风险。失败后世界回滚、外部事件与非1000 L流体完整适配仍待验。

## ALIGN-226：只读出液罐成为可由外部填充的双向接口

无覆盖板的原泵只覆写getFluidTankDrainable2及getFluidTanks2；Covers父类getFluidTankFillable2返回null，Root.fill实际经此返回0。原输出可以从任意面被抽取，四个垂直轴面是主动推送限制，不能把两者混为“正反面完全没有能力”。

当前两端返回原始FluidTankGT。其fill接受非空流体，空罐时按容量存入，泵没有套只允许drain的包装，所以外部handler可填入水或其他流体。若四侧接收端未排空这份流体，serverTick随后看到tank非空便暂停世界抽液；若先推空，仍可继续抽取，不能把暂停写成无条件。对无盖、无可用排液邻居的空泵填入1000 L水的源推导：原fill=0，当前fill=1000且余液阻止抽新源；不要据此声称已经在游戏中被管道灌入。

原内部默认容量Long.MAX_VALUE，当前16000。普通每次一桶且非空即阻止再抽，因此不能据容量数字直接推断普通世界泵会少装15桶；原可返回特殊量的外部IFluidBlock或现代外部灌液另有适用条件。已有测试通过公开fill准备罐内fixture，只证明所测试的当前输出路径，不证明这个公开填充合同原本存在。

## ALIGN-227：旋转泵缺少开关与运行状态接入

原泵实现SwitchableOnOff／RunningActively，NBT保存stopped／active／activeData；停止时不抽新源，非theoretical能量准入也拒绝。停机仍先输出旧液体。AbstractCoverAttachmentController确实允许SwitchableOnOff宿主并调用setStateOnOff，运行检测板也消费对应接口，故不是一组从未调用的声明。

当前PumpBlockEntity及GTEnergyBlockEntity均无MachineControl.Provider、enabled/stopped字段或运行历史；MachineControl.find返回null。但不能据Java声明没有PanelCoverHost就判覆盖板完全不能安装：两端配置都列出ComponentCoverHostMixin，正常转换后BlockEntity实现FallbackCoverHost→PanelCoverHost，泵满足ownClass／eligible且没有自有getCover，能使用后备存储、安装与事件tick。两端能力装饰亦包含泵。因此撤回上一稿“物品宿主检查直接失败／没有覆盖板运行入口”的过强结论。

该补偿没有提供MachineControl.Provider。无盖泵的coverControl会落到CoverProgress，它依据现有tank提供容量计量，但supportsSwitch=false、running=false、active=false；PanelCoverRuntime对机器控制／运行检测盖的canAttach仍要求supportsSwitch，所以这一类不能因此恢复。现有tick／RU接入不检查泵停机状态，load/save没有相应字段。缺口是泵本身的可表达停启与运行检测，不是后备覆盖板全部失效；与原无开关的219热交换器不同。Mixin变换／实际安装和事件tick尚未在本批运行验证，来源修正另见[回执](verification/rotational-pump-audit-20261021.json)。

原mActiveData还有64位历史与三态模型选择；当前动画／RGB／所有手动工具旋转的真实模型和事件链仍待专题验收。本项不凭方法缺失推断软锤、扳手的所有通用外部回退都不存在。

## ALIGN-228与既有218：注册物性和物品堆叠

原Loader给四档hardness/resistance为7/7、6/6、9/9、12.5/12.5。Paintable.readFromNBT2真实读入，getBlockHardness／getExplosionResistance2返回字段，MultiTileEntityBlock实际委托这些getter；不是只在注册行写着但从未使用。

当前GTPumps四档统一Properties.strength(4,6)，PumpBlock无硬度／抗爆覆写。现代Properties.strength分别设置destroyTime和非负explosionResistance，原生状态／Block getter实际读取这些值。因此四档硬度都变4，抗爆除钢档6外也有差异；228记录此规格消失，不宣称已经量测实际挖掘时间或爆炸破坏半径。

四条原注册stack16通过ClassContainer和ItemInternal的item limit消费；当前GTPumps均plain BlockItem(new Item.Properties())，无stacksTo，采用既有218已追的原生64默认。218范围扩为两太阳能＋四泵，32块的最小槽占用2→1；没有借此推断其它模组物品全是64，也没有重新运行库存操作。

原泵确实定时调用默认环境检查，但它声明RU；原TD易火／易水／雨／雷集合都不含RU。不能只看有doDefaultStructuralChecks就把泵也扩入017“应遇水炸”的宿主，亦不改现有安全政策。

## 可读性、来源与未验边界

展开PumpCatalog和PumpWorkRules的合并导入、方法、常量与分支；all局部out/i/t改为pumps/tierIndex/tier，private record/API名称及四档数据保持。PumpBlockEntity展开输入预算、getter、冷却分支、Neo能力推送和两端NBT调用排版。core 237／124项、Forge1961／Neo1877项完整执行词法与字面量位置保持；只有Catalog三个all局部绑定做有范围／出现次数证明的归一，原生注释及Stonecutter条件保持。

原泵头为Copyright (c) 2021 GregTech-6 Team、Gregorius Techneticies、LGPL-3.0-or-later；来源只读，没有新复制原方法体或原生源码进生产。本批未改任何玩法，也未运行Java初始化／编译、Gradle、测试、游戏、真实抽液／存档重启、构建或推送。只读javap观察Forge三类，Neo读取既存source jar，8个ZIP entry身份已记录；原生代码没有纳入仓库。

后续仍需核实际手动工具／朝向重置、RGB和活动模型、特殊／较轻流体、世界保护与回滚、跨区块／高度边界、全材料域、长时间保存与真实端到端产线。此专题不把有限数值或现有测试当全部机器验收。

第六十五批218范围续补：八大型涡轮已追到实际RegisteredBlock默认物品注册，16→64同属218，主表累计14个明确对象；不是对全部机器的推断，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。227仍按补充后的Mixin后备宿主事实判定，只缺泵停启／运行接口。
