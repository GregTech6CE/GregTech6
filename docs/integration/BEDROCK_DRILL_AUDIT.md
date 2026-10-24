# 基岩钻机的结构、耗材、产物与工具核查

第七十批，2026-10-21，基线`2840bb4c7`。核原17999的实际注册、44支撑、RU与润滑油、加权矿物／出石、输出、工具、检测和保存。新增ALIGN-248–251，扩036／110／218／232／233／247；三份源码整理保持五份完整执行视图，本轮不修改玩法或危险政策。

原只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。30份原来源、62份当前输入、43份共享原生源的86个平台视图，以及2个本机原生工件、11个entry和9份只读javap视图见[回执](verification/bedrock-drill-audit-20261021.json)。各输入只对本报告列明合同留证，不等于整份文件已经验收。

## 实际注册、布局和已有功能

原Loader_MultiTileEntities注册17999，材料Ti、stack16、硬度／抗爆9／9、接受RU，带RM.BedrockOreList参数。当前SharedLargeMachineParts→GTMultiblocks→MultiblockBlockFactory实际创建OriginalBedrockDrillControllerBlock；两端GTBlockEntities绑定同一专用BE，ticker调用tick。不能按文件名把另一个未见当前仓库构造引用的HorizontalDirectional旧类当真实注册，也不能据Builder支持块参数出现同一对象两次就报双重注册：两端实际Builder.of使用ImmutableSet.copyOf。

原基岩地面在主控下5格的3×3；上方9个18103钻头，再上35个18026致密钛壁板，主控在顶中心。四个主控下1格的正侧壁为RU输入，其余31墙为流体输入，钻头不运输。当前实际17999分支与AdvancedControllerRules.drillRole对应。原Util对主控自身直接成功，所以支撑44件另加地面9格，不把主控重复算进墙数。

当前主控有单格只出库存和16000L润滑罐，形成部件委托同一主控，没有第二份储液。主控罐可填可排，流体输入墙只填的FluidPort符合原ONLY_FLUID_IN对真实排液的阻断；不能只看原getFluidTankDrainable返回mTank就报墙体应能排液。Forge通过getCapability暴露库存／罐；Neo的GregTechNeoForge→BasicMachineRegistries.register→capabilities实际注册两种能力，不是只有声明没监听。

完整9格都为普通基岩时也能工作，只是不进入矿物分支；不要求必须有至少一块矿。大基岩矿各放材料两次，小基岩矿一次，随机选择128个位置。9大矿对应18／128进入矿物分支，9小矿9／128；非矿物分支才有1／1000基岩粉，9大矿时整体为11／12800。这些只是选择分支概率，不能代替最终物品注册或游戏产率验收。

普通成功周期需要32768RU、空输出、形成结构和100L润滑油，产一件；先尝试把旧产物向上送出。OreBlock.brokenStack真实生成可放置的破碎矿石，已不是一概拿oreRaw代替；材料或对应矿块缺失时仍有当前raw／原矿材补偿分支。原Hexorium重分色、HBM覆盖与旧第三方维度分支要结合现代对应注册另核，不因旧模组名不存在就报默认世界不能采矿。

## 原过压规则与保存中已有的修正

1024／2048／4096的最小／推荐／最大RU包可对应。原mEnergy>40000先拒收，等于40000仍可接合法整批；DirectMachinePowerRules已保留这个软上限。给定形成结构、能量0、4096×10，双方接受10包并暂存40960；下一包8192因先满足缓冲超限而直接拒收，不请求爆炸。若原有能量恰40000、执行注入8192，双方走原明确的强度6爆炸特例；模拟注入不爆炸。

这些是源算术和分支，不是在世界中引爆。MachineOverload.explode先检查实体仍在、先移除，再请求爆炸，先前防重复修正保留；不把本机明确的直接explode改成通用默认关闭的overcharge。Long.MIN_VALUE和有界加法防护也保留，不恢复原溢出。

原与当前均保存运行电量、石种、输出和润滑油；当前能量读回不截到40000，不能重复报告旧软上限修复尚未做。Neo的NativeMachineTankData用于恢复普通固定容量罐，仍有其既存容量夹限；本轮只展开代码，不据标准16000L路径承诺所有超量／旧格式液体都能保持。真实磁盘重启仍未验。

Block.getDrops局部虽先构造快照，最终配置CoverDropMixin→CoverDrops.capture会剥除本机运行数据，物品由onRemove／BlockContents另掉。不能把局部快照当成普通挖掉仍携带电量／润滑油。clone／pick另一路径未在本批接受；不与普通掉落混同。

## ALIGN-248：未形成时新增主控接电与主动输出门控

原主控isEnergyAcceptingFrom只查能源身份，doInject没有结构条件；原tick中的slotHas→ST.move也在checkStructure之前。因此无完整结构的在位主控，仍能接合法RU包，并把已有输出送给上方库存。

当前doInject先要求isStructureOk，tick也在任何输出前检查结构并返回。给定未形成、能量0、2048RU×1，原主控存2048，当前拒收；给定一件已有产物、上方无覆盖板阻挡且容器接收，原尝试一次主动输出，当前零次。手动或外部提取能力仍存在，不报告物品永久锁死。此处记录新增保护门控的合同差异，不直接撤销当前保护，也不把它说成多耗32768或多爆炸。

## ALIGN-249：润滑油从可扩展集合变为单一流体身份

原CS.FluidsGT.LUBRICANT是公开可变名称集合，FL构造器将lubricant、rc lubricant等加入，本机填充以集合contains为准。当前罐validator只接受GTFluids.still("Lubricant").get的同一Fluid对象；字段别名最终仍只解析一个holder。两版实际FluidTank.fill都会调用validator，不能因候选未生成配方就忽略填罐接口差异。

给定额外流体已注册且其名称加入原LUBRICANT集合，原空罐可接受，当前空罐拒绝。此为扩展协议边界，没有安装RotaryCraft或注册测试流体；不是要求把含空格的旧流体名称直接注册到现代ResourceLocation。默认本模组Lubricant仍有正常路径，不报普通润滑油不可用。

## ALIGN-250：普通出石池扩大，连带改变矿石背景分布

原BlocksGT.stones数组及Loader_Rocks实注册为17种，mType重选的额外1／2项分别用于原版圆石及可选Et Futurum深板岩。当前直接用StoneType.values的27种，再加两项现代圆石／深板岩，共29；BlockBootstrap确实把这27种石材及变种登记给GTBlocks.getStone，不能只当无效枚举占位。

额外10种为Chalk、Dolomite、Gabbro、Gneiss、Gypsum、Oilshale、Rhyolite、Salt、Sylvite、Talc。普通世界中给定周期重选得到共同索引17，原分支落到原版圆石，当前得到白垩圆石；矿物分支也会采用该StoneType对应OreHostStone。原17种在重选后的候选占比从17／18或17／19变为17／29。原1／1000换石种条件仍在，差异是选择池，不是每次产物都重新均匀换石。

现代原生深板岩可作为合理适配保留；额外十种是否应默认进入钻机池需作明确产品选择，本轮不删除这些材料。原初次构造的随机值会被Registry.initFromNBT→readFromNBT2再次读值，不能只看原字段随机初始化就断言初放两边同分布；本条限定运行中重选分支，不声称初放或旧存档索引迁移已经对齐。

## ALIGN-251：设备专用整罐排液缺失，且两版回退不同

原onToolClick2在Base10结构工具后处理plunger，GarbageGT.trash(mTank)默认Long.MAX_VALUE，清整罐并返回实际清掉量。当前主控不是MultiblockToolTarget，也没有等价的专用plunger分派。普通FluidUtil容器交互仍可排液，不等于专用工具存在。

Forge通用BehaviorPlungerFluid首先要求BE自身instanceof IFluidHandler；本类只提供getCapability，不实现该接口。已追GTEnergyBlockEntity、实际原生BlockEntity／CapabilityProvider、IForgeBlockEntity及其接口祖先，没有这个继承关系；当前配置Mixin补的是后备覆盖板宿主，不是IFluidHandler。Neo通用行为则通过level.getCapability找到罐，每次最多排1000L。

所以给定无遮盖主控、可用排液器与2000L正常润滑油，原清2000，当前Forge这条工具路线清0，Neo清1000。这是当前类型／能力消费与缺专用分派的源结论，没有实际玩家点击，不泛化成Forge所有机器排液器失效，也不修改全局排液量。

## 既有记录扩围及源码导航

| 记录 | 本轮具体范围 |
| --- | --- |
| 036 | 当前首错清绑定、全候选才认领44件；原逐格有效认领。本机原checkStructure2没有禁刷怪塔的四角未加载缓存分支，不套该子结论 |
| 110 | 原RunningPossible为形成且润滑油≥100，不要求已有能量／空输出；当前无Provider，后备supportsSwitch=false导致可能运行检测器被拒。原本没有OnOff，不额外补报开关缺失 |
| 218 | 17999原stack16、当前普通BlockItem64，已核能源设备21→22项 |
| 232 | 当前每次完整检查44支撑＋9地面并重建矿材列表／绑定；20次为880支撑、180地面，源次数不是TPS测量 |
| 233 | 原建造棒／形成诊断／润滑油放大镜详情缺专用目标接口，部件转发也到不了该接口；存在预览不代替工具 |
| 247 | 原gt.energy.accepted可把已知EU设为接受类型，当前固定RU；扩为部分主控的同一设置读取缺口，默认RU工作另列 |

当前预览旧手写钻机行在末尾canonical按controller物品被新布局替换，expectedPart也优先读ControllerStructureLayouts；不能只据旧行的九钛壁板报告最终预览仍缺九钻头。归并后的端口角色标签、客户端仪表与渲染仍需独立核查。旧HorizontalDirectional类的公开扩展可达性亦待核，不为整理重复代码贸然把公开类型删除。

[钻机BE](../../multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/BedrockDrillControllerBlockEntity.java)和[真实方块入口](../../multiversion/src/main/java/com/gregtech/gregtech/block/machine/OriginalBedrockDrillControllerBlock.java)展开结构、物品输出、随机候选、能力和两端保存，局部名称改为candidateParts、depositPos、partPos、expectedBlock、partPosition／partRole、brokenOre／rawOre／hostStone和明确ticker参数；只在各自方法区域归一比较，未全局改包名或字段。

五份完整有序执行词法及字面量位置保持：BE Forge2771／Neo2702，Block1000／1322；[Neo罐恢复辅助](../../multiversion/versions/1.21.1-neoforge/src/main/java/com/gregtech/gregtech/api/fluid/NativeMachineTankData.java)3→16行、174项保持。BE一条注释改为当前周期，去掉“必须有矿”的过强暗示；其余注释、许可证和Stonecutter指令保留。原BedrockDrill头为GregTech-6 Team2025、Gregorius Techneticies、LGPL-3.0-or-later，其余来源独立留头／摘要；没有复制外部原生方法进生产。

CompoundMaterials／WoodMaterials摘要保持，未Java编译／目标初始化、Gradle、测试、游戏、pack生成、供能／排液／爆炸、磁盘重启、构建或推送。正式251条包括已修复／部分修复、扩展合同和保留政策，不能当作251个未修故障；全面核查继续。

## 2026-10-24 补充：结构检查与矿物候选对照（结构跟踪卡 56）

结构跟踪改造要定基岩钻机的矿物候选留在结构匹配里，还是移到运行逻辑。这一节只对照成型条件，不改代码。原只读根`D:/Projects/java/gt6_source_code/gregtech6/src/main/java`，当前基线`main@4dc02e6ca`。没有新增ALIGN编号：成型条件没有发现新的差异，只补充036和232的源码依据。

### 原版怎么成型

原`MultiTileEntityBedrockDrill.checkStructure2`（82–120行）：

- `yCoord < 5`直接不成型；先清空矿物列表`mList`。
- 主控下5格的3×3地面逐格看方块。大基岩矿把材料加进`mList`两次，小基岩矿一次。HBM的Coltan、Oil两种基岩矿加各自的材料并置`tOverride`。其余方块若`WD.bedrock`不为真（原版基岩或Betweenlands基岩，`WD.java:672`），置`tBedrock`为假。
- 每个(i, j)再检查y=-4到0五个零件位，用`checkAndSetTargetOffset`认领。某格失败只把`tSuccess`置假，循环不中断。
- 返回`tSuccess && (tBedrock || tOverride)`。

`checkAndSetTarget`（`ITileEntityMultiBlockController.java:47-77`）对匹配的零件直接`setTarget`；只有零件当前的目标是别的控制器、且那个控制器的`isInsideStructure`覆盖它时才拒绝。钻机的`isInsideStructure`是x±1、y-5到y、z±1（`MultiTileEntityBedrockDrill.java:144-146`），含地面层。

`checkStructure2`只在`mStructureChanged`或强制检查时运行（`TileEntityBase10MultiBlockBase.java:177-185`）。强制检查来自首个tick、坐标变化、建造棒和放大镜，以及未成型时每600tick一次（同文件105–170行）；成型后没有周期性重检。`onTick2`的生产条件调的是`checkStructure(F)`（`MultiTileEntityBedrockDrill.java:155`），所以`mList`是上一次结构检查留下的缓存。

### 当前怎么成型

`BedrockDrillControllerBlockEntity.scanStructure()`（85–127行）：

- 地面格：区块没加载就不成型并清认领；宿主岩为基岩的`OreBlock`把材料放进`deposit`（`oreSmall`一次，其余两次）；否则不是原版基岩就不成型并清认领。
- 5层零件：方块和方块实体类型都对才进候选；第一处不对就清认领并返回。
- 最后`StructureClaims.update`→`PartBindings.update`：先验证全部候选都能认领，任何一个不行就清掉所有并失败（`core/.../PartBindings.java:11`）。
- 追踪足迹（`CustomStructures.drillFootprint`，139–152行）：9个地面格用`Projection.bedrockFloor()`，比较矿的材料、前缀、宿主岩，或原版基岩，或其他方块；44个支撑用`Projection.BLOCK`。

### 五个问题

1. **原版地面格的接受集合。** 大基岩矿、小基岩矿、HBM的两种基岩矿，以及`WD.bedrock`为真的方块（原版基岩、Betweenlands基岩）。集合之外的方块使`tBedrock`为假，成型失败；例外只有HBM基岩矿出现时的`tOverride`，那时非基岩格也被容忍（`MultiTileEntityBedrockDrill.java:89-108、119`）。
2. **矿物列表的位置。** 在`checkStructure2`里顺手收集，但不是成型条件：9格都是普通基岩也成型，只是`tSelector < mList.size()`不成立，不进矿物分支（160–161行）。成型状态不随列表变化。
3. **当前实现和原版的差异。** 地面格和矿物候选都在`scanStructure()`里（90–107行）；接受集合是宿主岩为基岩的矿块和原版基岩，成型同样不看有没有矿。差异只有三处：没有HBM覆盖和Betweenlands基岩两个第三方分支（现代端口没有这两个模组，见上文待另核）；第一处不匹配就清掉全部认领，原版继续认领其余零件（036）；地面区块没加载时直接不成型，原版没有对应的显式检查。
4. **移到运行逻辑的可见差异。** 前提：地面格在结构里只要求“基岩类方块”，矿物列表在每个钻井周期开始时读这9格。
   - 成型判定不变，矿物分支概率不变（仍由选择值是否小于列表长度决定），绑定不变（地面格不是零件）。
   - 列表新鲜度：原版是上次结构检查的缓存，当前是每次扫描重建，移后总是当前状态。唯一能看出差别的改动是“地面格换成另一种基岩矿”：原版不会因此重检，当前因为`bedrockFloor()`比较矿的材料而让机器停到当tick末，移后当前也不停。生存里基本碰不到：基岩矿挖不动、炸不掉（`OreBlock.java:317、335`）。
   - 成本：现在一次完整检查读44支撑加9地面并重建列表和认领（232）；移后结构评估不碰矿物，每个周期读9格。原版提示输入最多32768RU/t（`MultiTileEntityBedrockDrill.java:139`），一个周期耗32768RU，最坏每tick一个周期、9次`getBlockState`，仍少于一次完整检查，需要时可按地面格变化缓存。
5. **逐格认领和`PER_CELL`。** 原版逐格认领，当前原子认领。可见差异集中在没建完或有争用的塔：原版未建完时已有的零件先被这台钻机认领，当前整塔都不认领。这台钻机的能量入口在当前要求已成型（`injectPortEnergy`和`doInject`先问`isStructureOk()`，248），所以未建完时已认领的零件不会因此多出可用的能量入口；流体端口的门控没有逐条核对。结论：列入`PER_CELL`对比，但不建议为它开启，默认保持原子认领。

### 结论

对照结果支持把矿物候选移出结构匹配，见DECISIONS 2026-10-24。HBM覆盖和Betweenlands基岩不在这个结论里。只读对照，没有改代码，没有跑Gradle，没有进游戏。
