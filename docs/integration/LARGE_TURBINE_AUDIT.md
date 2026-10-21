# 大型蒸汽与燃气涡轮合同核查

第六十五批，2026-10-21。四档蒸汽17211–17214、四档燃气17231–17234的注册、结构、原生ticker、转换、能力与保存／工具链已追。新增ALIGN-229–233五项，036／111／139／218／221扩展宿主；过载与拒收能量的差异包含安全／资源政策选择，不自动按编号恢复原破坏或浪费行为。

原版只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。基线eb7945213，57d7419ca只修正上一批泵的Mixin结论。26份原来源、47份当前输入、36维护源的72视图及两份原生上下文entry在[回执](verification/large-turbine-audit-20261021.json)。只核所列消费者，不代表这些整份文件已验收。共享轴向父类也供大型发电机使用，本批仅保持其执行，没有据此验收发电机玩法。

## 目录与实际入口：不是整族缺失

| 转子／壳材料 | 蒸汽额定SU→RU | 燃气额定HU→RU | 墙ID | 硬度／抗爆 |
| --- | --- | --- | --- | --- |
| Magnalium／StainlessSteel | 12288→4096 | 6144→4096 | 18022 | 6／6 |
| Trinitanium／Titanium | 24576→8192 | 12288→8192 | 18026 | 9／9 |
| Graphene／TungstenSteel | 49152→16384 | 24576→16384 | 18023 | 12.5／12.5 |
| Vibramantium／Adamantium | 393216→131072 | 196608→131072 | 18025 | 100／100 |

原Loader_MultiTileEntities:1254–1257、1264–1267以第三参数给实际ID，第四17101是共用类型。STEAM_PER_EU在所读快照为2；OriginalGeneratorParameters的四条GasGrade、派生STEAM与GasTurbineDefinitions／AxialGeneratorDefinitions共同消费这些额定／墙／材质／物性。MultiblockBlockFactory真实创建对应控制方块，GTMultiblocks和RegisteredBlock负责注册；GTBlockEntities绑定实际四档数组，两端getTicker调对应tick。不是只有目录或预览而没有实际方块。

当前前控制器流体能力有Forge getter、Neo BasicMachineRegistries注册；部件通过MultiblockPortBlockEntity.target→owner.isStructureOk→portFluids取得角色包装。输入只能填，输出只能抽，FLUID_IO双向；后方back3中心是RU端口，真实接收器在控制器后方4格。原getEmittingTileEntity为back3部件，再向外一格发送，两者位置相同。此处不要求额外装入转子物品，名称中的转子材料来自控制器制作与显示。

## 六向几何和已有普通转换

原3×3×4包围盒含主控，共36格，实际墙35；当前SharedTurbineStructure排除原点也得35格。将原min/max／tOut方程与当前StructureGrid.offset、lowest、axialRole按六轴分别有限重建后，每格位置和有效流体／RU角色相同：水平底层排液，朝上时back3是底层，朝下时back0是底层。燃气原ITEM_FLUID角色还有物品bit，但原泵槽并无物品库存；不把省略无实际库存的bit当掉物缺陷。

这些是坐标与角色的源数学，不是六座游戏结构实际搭建。当前结构每次都检查已加载、匹配墙与部件实体；错误／缺区块时清绑定，与原逐格认领且缺角块保留mStructureOkay的区别扩到既有036。没有以“形状相同”推断保存重载、争用、区块和交互全部相同。

蒸汽原min为额定一半，启动条件min×2；当前grade.input相同。两者整罐分成两个整数半量给连续两刻、170 L累计凝结1 L蒸馏水，水罐满时余量丢弃不阻轴。停机不再抽新蒸汽，但已排队的第二半仍可转换；原版就是这个顺序，不列为“停机仍输出”的新缺陷。源／水tank均有世界保存，两端读取也实际存在，不误报Neo没有保存或普通pending必然丢失。

燃气输入容量max×4、三排气罐容量max×16，三罐都低于半容量才烧新燃料，输出按每秒轮换排气罐等普通分支已有。原排气也要求外部抽取，没有自动推液承诺；底层部件的抽取能力已存在，不能因tick没有pushExhaust就报缺失。原FL.steam只识别命名steam，不能拿anysteam的范围套给它来虚构外部蒸汽兼容退化。

## ALIGN-229：燃气的可变即时输出变成最大包整批、延后一刻

原LargeTurbineGas.doConversion先按缺少的最大输入热量计算燃料批次，实际可消耗较少批次；燃烧成功当刻调用父转换器。父类按floor(存储HU×额定RU/额定HU)算包，达到额定输出一半即可尝试，按原max限制。当前则只在fuelEnergy≥inputMaximum时扣完整最大输入、写ruStored=outputMaximum；pushOutput发生在这段之前，新RU直到下一刻才尝试发送，且包大小始终为最大输出。

有效有限例子：两边氢气配方均成功注册、无其他干扰配方，最低档结构完整、空缓冲／空排气罐、接收器接受合法RU。原／现燃料行都是2 L氢气产生32 HU和3 L水；喂256 L得到128批、4096 HU／384 L排气水。原当刻可尝试floor(4096×4096/6144)=2730 RU，≥最小2048；当前4096<12288，不尝试RU输出，继续等待。另即使一口供到最大批次，当前也多一刻队列延迟。不是单纯GUI额定数值不同，也没有在游戏中实际供气。

FluidFuelBatch本身做副产物预检和副本消费，能量以有界BigInteger计算；不应通过删掉这些保护来修输出时序。自定义多配方优先序、额外输入和所有燃料等价仍未验。

## ALIGN-230：燃气拒收后的能量与燃料政策不同

不能仅据原NBT_WASTE_ENERGY=false就推断原燃气在所有路径都保存拒收能量。原LargeTurbineGas有先行特例：若mStorage≥inputMax，保存tEnergy、把临时storage设max、调用父类，然后无条件回写tEnergy-max；即使接收器拒收也扣这部分。相反当前已转换的ruStored在拒收时不清，tick随后因ruStored>0返回，后续停止烧新燃料。

源条件例子：最低档从零开始，喂768 L氢气，生成12288 HU／1152 L排气水，接收器持续拒收。原第一刻燃烧后父转换器尝试8192 RU失败，HU仍12288；第二刻进入上述特例，尝试仍失败但最终HU=0。当前第一刻排队8192 RU、HU=0，第二刻尝试失败仍存8192，后续不继续燃烧。输入和排气量相同，阻塞后的能量命运不同。

当前保留能量可减少燃料浪费，这是一项需明确的对齐政策，不在可读性整理中改成白烧。停机前已存的能量两边都有可能继续尝试输出，不能只看当前pushOutput早于stopped就声称原必定不输出。

## ALIGN-231：大型蒸汽过载变为持久锁停

原蒸汽使用waste=true且没有limitConsumption，TD自动限耗集合也不含STEAM。输出超过max且aTimer>2时，父转换器标记过载，Base11.overload前100次保护累计／清storage，不设置mStopped；之后才可能进入受原配置约束的overcharge。原蒸汽pending另存，过量批次的后一半还可能再次触发反馈。aTimer≤2还有单独的清缓存启动处理，不等同于当前永久故障。

当前AxialGenerator.convertAndEmit一旦output>max就overload：overloaded=true、stopped=true、energy=0；蒸汽tick末还清pending，以后tick在overloaded处直接返回，fill也拒绝。通用setEnabled(true)保留overloaded门槛，不能清锁；现有手动toggle首次复位后仍stopped，再次才启用。这个分支本身没有调用爆炸。

可达源数值：最低档蒸汽输入罐容量98304 L，合法填满后半罐能量49152，换算尝试16384 RU>8192最大值。原先走保护反馈仍保持开关，当前立即锁停；不需要错误NBT。这里记录安全／反馈合同，不据编号恢复原爆炸、旧整数溢出或默认危险配置；实际过量供汽、声音和产线恢复未运行。

## ALIGN-232：流体查询反复完整扫结构并重写部件

原Base10.checkStructure(false)只在mStructureChanged或强制检查时调用checkStructure2；未变更时用缓存。原部件getTarget(true)会验证控制器，但稳定缓存下不重新执行35格结构循环。

当前两类isStructureOk每次都新建候选Map、遍历35格并读取墙／部件，再PartBindings.update的canClaim和claim所有候选。部件relay的getTanks／getFluidInTank／fill／drain等每次target都调用它；控制状态查询也可触发同一路径。对稳定完整结构的20次独立流体查询，单这层至少700格校验和700次bind调用，原稳定缓存下新增完整循环为0。

这是实际调用链和操作数量，不是TPS／耗时或“卡顿已复现”的结论。当前重查也提供较即时的失效检测；改进应设计变更通知、缓存生命周期与卸载策略，不能为降扫描次数直接恢复过期绑定、吞掉部件争用或删掉hasChunkAt。

## ALIGN-233：大型涡轮没有接入专用结构建造与形成诊断

原Base10.onToolClick2／onToolClickMultiBlock会将建造棒点击的坐标、玩家、库存传到这两类checkStructure2，Util.checkAndSetTarget可在原距离／权限／替换条件下放对应墙并认领；放大镜则检查并报告形成状态。两类涡轮真实调用该Util，所以不是只在父类保留空声明。

当前GTToolItem.onItemUseFirst先MultiblockTools.use；它只接MultiblockToolTarget。当前AxialGenerator及LargeGas类只实现结构／端口／控制等接口，核心StructureController也只有isStructureOk；配置的后备覆盖板Mixin不提供MultiblockToolTarget。建造棒于是继续到普通BehaviorBuilderWand的同块平面复制，它没有查询本涡轮的35格定义。部件虽实现MultiblockToolTarget，其转发也要求host属于该接口，不能替这两个主控补齐。

因此现有专用多方块入口不能按涡轮定义建造或给出原形成诊断；不能把手工可搭、JEI预览或普通复制功能当原建造棒功能已完成。没有运行实际放置、消耗与世界保护；普通建造棒、通用放大镜其它用途不在“全坏”的断言中。

## 既有合同扩展与排除的候选

| 记录 | 本批实际宿主与边界 |
| --- | --- |
| 036 | 两类涡轮都用完整候选后PartBindings整体认领，失败清旧绑定；原逐格认领和缺角块保旧状态差异沿已有项，不新增同根编号 |
| 111 | 原Base11.getStateRunningPossible恒true；当前idle控制provider虽存在，但默认coverPossible=active或progressMax>0，switchable的progressMax=0，空闲时false。不是完全没有MachineControl |
| 139 | 原LargeTurbine.allowCovers禁止正面；当前后备host／storage和通用canAttach无该禁面，适合的status控制板可通过supportsSwitch而在正面安装。Mixin宿主确实存在，不重复上一批泵的误判 |
| 218 | 八条原注册stack16，当前GTMultiblocks→RegisteredBlock.register默认plain BlockItem(new Item.Properties())为64。范围扩为已核两太阳能＋四泵＋八涡轮14项，不推断全部机器 |
| 221 | 蒸汽主控有purgeFluid输入先于水罐，但GTToolItem先通用plunger，当前drain只出水：输入2000／水1000时先排水1000并SUCCESS，后续块专用入口被原生分派截断；原先清输入2000。水罐空时专用输入清空仍可达。只比较普通无外部取消的玩家路径 |

此前候选“拆卸物品保存完整能源／内液”排除：AxialGeneratorBlock与LargeGas Block确实先保存完整BE数据，但配置中的CoverDropMixin会在普通静态Block.getDrops返回后调CoverDrops.capture；这两类都继承GTEnergyBlockEntity，且不在Tank／LogisticsTank／MultiblockTank例外中，故先移除BlockEntityTag／BLOCK_ENTITY_DATA，再仅合入覆盖板数据。不能只读局部getDrops就报普通掉落保留内液。直接调用方块实例getDrops或外部绕过这条静态链的路径，以及完整采掘实测仍未验。

燃气原tParallel<tMax时会清掉不足下一批的残液，当前计划保留余液；例如3 L氢气消耗2 L后余1 L，原清空而当前保留。记录为资源保护差异，不自动恢复原丢弃。当前预检排气空间、准确副本消费、long安全／clamp也应保留；它们不能当原每个极值分支已对齐。

## 源码整理与验证范围

展开OriginalGeneratorParameters、AxialGeneratorBlockEntity、LargeTurbineControllerBlockEntity和LargeGasTurbineControllerBlockEntity：四档定义与派生、控制回调、结构循环、能量／流体、两端保存与能力边界分段。grades的list/i/material局部绑定改gradesByMaterial/gradeIndex/gasGrade，不改record字段或公开名称。燃气类Javadoc去掉只适合最低档的Stainless-steel限定，原版权／来源说明未删改。

完整core441项与原生1684／1712、1323／1174、2615／2494项执行词法及字面量位置保持；只对上述局部绑定做范围归一。除列明一条说明外，真实注释和Stonecutter指令保持。没有将“格式更清楚”冒称燃料、过载、存档或所有大型发电机已修复。

原涡轮／Base11头为GregTech-6 Team 2025，燃气／转换行为2023，容量行为Gregorius Techneticies 2019，各自LGPL-3.0-or-later来源只读并留摘要。本轮没有新增复制原方法体、原生源码或第三方资产；只读本机既存BlockItem／CustomData源码来排查掉落候选，非Java目标执行。

未进行目标类初始化、Java编译、Gradle、测试、实际供汽／燃烧／发送、结构建造、渲染、存档重启、构建或推送。继续大型发电机、避雷针、大型换热器及其余全系统队列；极值、可选燃料、完整配方优先序、回调／保护、区块／多人和真实产线仍未验收。
