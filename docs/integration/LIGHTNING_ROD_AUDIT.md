# 避雷针结构、采雷与能量合同核查

第六十六批，2026-10-21，基线9bdea43ae。核实际注册17998及18104杆件、五层支撑、雷雨轮询、顶部条件、EU接口、保存与普通取样；新增ALIGN-234–238五项，218／232／233扩宿主。接口修正与雷击效果中含现代适配政策，登记差异不等于自动恢复原接口错误或有害效果。

原版只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。18份原来源、35份当前输入、29维护源58平台视图、两个本机原生工件16entry及六份只读javap身份在[回执](verification/lightning-rod-audit-20261021.json)；只核实际列明的声明与消费者，不据此验收整份文件。

## 注册与已有的普通主链

原Loader_MultiTileEntities:1282注册17998，容量18000×VREC[6]；CS的VREC[6]=32768，所以总589824000 EU。当前SharedLargeMachineParts→GTMultiblocks／RegisteredBlock→MultiblockBlockFactory实际创建OriginalLightningRodControllerBlock，GTBlockEntities.LIGHTNING_ROD绑定该块，两端ticker实际调用同一个LightningRodControllerBlockEntity.serverTick。AdvancedControllerRules和OriginalUtilityControllerData提供同一容量与包大小，物品提示和预览确有消费者。

支撑为五层实心3×3：钨／铌钛线圈交替，三层钨共27格减主控1格为26墙，线圈18格，支撑44件；顶中自第5格高度起连续18104镀锌钢杆。现代循环到实际getMaxBuildHeight，使用int长度；原byte mSize在超127时可能溢出，不应为“对齐”恢复这个旧限制或固定旧世界高度。44件身份与普通杆柱读取已有，认领语义另列234。

两边均在足量储能时向下尝试32768 EU包、最多16包，拒收也至少损失一包。原常规tick不靠其isEnergyEmittingTo查询：ITileEntityEnergy.Util.emitEnergyToSide直接解析邻居并调用接收端注入。因此下面236中的原接口错误不能被解释成“原避雷针从来不会发电”。当前输出量还按现存整包数限额，避免原代码在尾端剩8包却仍提供16、接收端一次收13而透支；此保护保留。

当缓冲不足一包时才进入自发采雷轮询：长度正、控制器Y＋长度≥100，百万分随机小于min(100,长度)，雷暴允许或普通雨另取1/10，再按同世界X/Z各<256范围内其他有效长度杆的数量竞争。两个版本都有相应公式，不能把256误写成球形半径，也不能用源概率当真实种子分布或等待时间测试。当前高度门槛在captureStrike，天气在普通serverTick调用处；公开测试接缝本身没有天气门槛，不误当实际tick能晴天采雷。

原提示说杆尖到100即可，但实际两边都判控制器Y＋长度，不加四层偏移：Y95＋长1的尖在100，实际门槛参照仍96，均不会成功。这是原提示与代码已存在的差异，不算移植新增。本轮没有修改它。

储能世界保存两端都有gt.energy实际写／读及容量clamp；杆长都需要从世界重建，不能因没有持久化长度就报存档丢杆。LOADED有加载、移除、区块卸载回调，弱集合的value为Boolean，不存在此前核网络强value反持key的同一对象图；未做堆／卸载实测，不能据容器类型宣称绝对无泄漏。

## ALIGN-234：支撑与杆柱只验方块身份，没有认领与争用

原checkStructure2对44件支撑和每段杆调用Util.checkAndSetTargetOffset(..., NOTHING)。NOTHING限制运输，不等于不认领：Util会检查已有目标是否仍覆盖此坐标，拒绝被别的有效主控占用，并setTarget保存主控／设计／模式。原isInsideStructure包括五层和实际杆柱。

当前isStructureOk只按block身份检查支撑、逐段数杆，不读取部件实体，也没有bind／PartBindings调用。它只实现StructureController；通用覆盖板Mixin可补host，却不提供结构认领。MultiblockPortBlockEntity的owner又只接受MultiblockPortOwner，没有另一个全局登记程序替这台主控认领。JEI／ControllerStructureLayouts的地图也不是运行认领入口。

有限布局：两主控同高度、X相差2，其他支撑与杆均正确，五层底座共享15件，且互不把另一主控纳入自己的支撑位。原第一台认领后，第二台在共享件遇到仍覆盖该坐标的原目标而失败；当前两个身份循环都可成功。部件sensorTarget在没有owner时也只回退到部件自己，原有主控转发不能仅靠方块外观恢复。没有在世界搭重叠设备或验证所有传感器读数。

## ALIGN-235：顶端整列空气换成天空亮度门槛

原从Y＋长度＋5直到world.getHeight逐格WD.air，任一非空气就失败。WD.air实际检查Block.isAir并另排除特殊节点；没有“透明就算空气”的分支。当前只在tip.above调用canSeeSky，不逐格检查方块。

本机两版BlockAndTintGetter.canSeeSky的实现都是SKY亮度≥max。Neo原生玻璃注册noOcclusion、透明类向下透天光、LightEngine光遮挡／SkyLightSources也有实际消费者；Forge对应玻璃透光和天空查询已读字节码。以完成光照更新、判定点天空亮度15的透明玻璃顶为条件例子，原在玻璃处air=false，当前亮度门槛为true。控制器Y100、杆长1时尖Y105，两边检查起点Y106一致，差别是谓词，不是加错一格。

这是源准入差异，未构建玻璃顶场景或验收所有光照更新／维度／自定义透明块；不要用canSeeSky名字推断等于原空气柱，也不要未经设计删除现代加载／高度保护。

## ALIGN-236：能量申报与主动抽取是适配差异，原接口本身不一致

原实际tick用32768，但getter min／recommended／max固定1024／2048／4096；isEnergyEmittingTo调用isEnergyType(..., false)，而isEnergyType只在emitting=true时接受，故查询恒false。Root.doEnergyExtraction先查这个门槛，且本类没有doExtract实现，普通抽取为0。原向下推送仍通过前述独立Util工作。

当前接口按PACKET=32768申报，继承min=16384、max=65536；允许有效结构下方（及理论查询），getEnergyOffered与doExtract可给最多16整包。EnergyTransfer.extractEnergyFrom、BasicMachine和BehaviorMachine有实际查询／抽取消费者，不是一个无人调用的新方法。原生／addon适配是否需要保留当前自洽规格属于明确设计选择，不能把真实tick改成2048来追一个原错误getter。

当前主动推送与外部抽取没有共享的每刻包数预算。给定缓冲至少64包，先推16，再有合法API调用抽16，代码可共消耗32包；原常规推送上限16且抽取0。两次都扣能量，不是凭空复制；例子要求足够需求的消费者在同一刻追加合法抽取，未声称某条内置产线已实测输出翻倍。此差异与原尾端透支分开，后者不恢复。

## ALIGN-237：模拟采雷的实体命中回调被visualOnly抑制

原模组创建普通EntityLightningBolt并加入天气效果。[Forge 1.7.10官方补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/entity/effect/EntityLightningBolt.java.patch)确认其命中列表路径：Forge事件未取消时仍调用实体的雷击处理。该补丁只补原生回调依据，不代表已读取全部旧原生起火或所有生物转换实现。

当前captureStrike先给LightningBolt设置visualOnly=true，再addFreshEntity。两版原生LightningBolt的服务端命中列表都由该字段门控，因此不会走原普通雷击的附近实体命中回调。这是影响实体玩法的适配差异，恢复前应明确效果政策，不能在排版中顺手改回伤害。

也不能反过来说“visualOnly保证完全无世界副作用”：当前原生初始化仍有powerLightningRod、铜块处理和gameEvent调用在该门控外。仅实体命中分支抑制已确认；实际命中、自然雷、燃烧、变种、事件拦截和现代其它副作用都未运行验收。本实现与原版都由设备自己的天气随机轮询触发，不是已接入所有自然雷事件后自动吸收它们。

## ALIGN-238：普通取样返回完整运行快照，未经过掉落清理

原MTEBlock.getPickBlock实际委托Base04.getPickBlock→writeItemNBT。其后代链保留名称／颜色／覆盖板等物品数据，但LightningRod／Base10没有把mEnergy写入物品态；世界writeToNBT2含能量不能据此当原取样也含能量。

当前注册块OriginalLightningRodControllerBlock.getCloneItemStack无Ctrl条件，直接saveWithoutMetadata或Neo saveWithId，把gt.energy及继承数据写到取样物品。两端原生Minecraft.pickBlock都先拿此clone，再判断Ctrl是否额外保存方块实体；普通创造取样也把已有clone送到setPickedItem／creative slot更新。普通分支没有调用CoverDropMixin。客户端已有能量快照时，返回的是该快照，不保证等于此刻服务器的最新能量，也不冒称游戏里复制成功。

这与普通挖掘分开：静态Block.getDrops仍经CoverDrops.capture清能源机器完整数据再合入盖数据，该补偿存在。放置端另有setPlacedBy读取所携带数据，但没有运行创造取样／放置／服务器物品包和权限实测。若保留特殊复制功能，应与普通取样的合同明确区分，不能把普通掉落的清理结论推广到所有物品产生路径。

## 既有记录扩围、源码导航与排除项

| 记录／对象 | 本轮范围 |
| --- | --- |
| 218 | 17998原stack16，当前RegisteredBlock默认plain BlockItem64；主表现已核15个能源设备，不推断其余机器 |
| 232 | 避雷针每tick／有效供能查询都会重扫44支撑和整根柱，没有原结构缓存／1200相位延后重查。仅扩“反复扫描”宿主，不谎称本类也有涡轮的35次bind；认领恰好缺失，另属234 |
| 233 | 本类无MultiblockToolTarget，部件也没有可转发owner；原继承Base10的建造／形成诊断不能由预览图补齐。ControllerStructureLayouts只见预览／显示／测试消费者，没有实际工具建造消费 |
| 后备覆盖板 | ComponentCoverHostMixin及事件补偿存在，本类原也没有SwitchableOnOff／RunningActively，不误报“缺少原开关” |
| 重复注册候选 | 旧LightningRodControllerBlock类与实际Original类并存，但实际注册17998只有一个；Builder中同一块重复参数是有效块集合构造，不是两个新物品ID。旧类清理留作源码归属工作，不凭文件数量报重复注册 |

AdvancedControllerRules与LightningRodControllerBlockEntity已展开：共享常量／扣包规则、原生五层／柱、天气竞争、推／拉接口和两端保存逐段可读；lightningDrain的sent参数改sentPackets，结构方法的pos／required／pillar改partPos／requiredBlock／rodBlock。core166、Forge1505／Neo1558项完整执行与字面量位置在仅这四个绑定归一后保持，Stonecutter指令和真实注释保持；一条core说明改成参数职责，不再笼统声称原版所有行为已一致。

原LightningRod头为GregTech-6 Team2025、Gregorius Techneticies、LGPL-3.0-or-later，各来源保留独立头与摘要；官方Forge补丁仅作链接与回调证据，没有复制原方法体、原生源码或资产进生产。外部回执摘要是网页返回补丁行的规范化身份，不冒称下载HTTP原件摘要。

未运行目标初始化／Java编译、Gradle、测试、雷击／RNG／输出／取样／放置／渲染、存档重启、构建或推送。仍需实际失效／跨区块、材质和所有配方、完整旧原生雷击、世界保护／事件、多人／客户端同步及长时间运行验收；大型发电机、大型换热器与全系统队列继续。源程序读到了哪些分支，不等于全部游戏已通过。
