# 多方块储罐主阀、流体接口与拆卸保存核查

第七十五批，2026-10-21，基线 `64a3007c3`。覆盖原17001–17067范围内实际25种主阀的目录字面量、普通流体访问、结构检查和最终物品保存链。新增ALIGN-262–263，扩036／232／233；26份原版、46份当前输入、68个平台视图及本机两个原生工件8个entry／4份只读javap视图见[回执](verification/multiblock-tank-audit-20261021.json)。输入留痕不代表整文件、整个储罐系统或游戏运行验收。

原只读根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，作者／LGPL来源头随回执保存，原目录未动。CompoundMaterials和用户未提交的WoodMaterials按本批起点摘要保持。

## 25种主阀的真实注册与字面量

当前纯数据入口是[TankValveParameters](../../core/src/main/java/com/gregtech/gregtech/content/multiblock/TankValveParameters.java)，共享原生适配是[TankValveSpec](../../multiversion/src/main/java/com/gregtech/gregtech/content/multiblock/TankValveSpec.java)。GTMultiblocks遍历SharedLargeMachineParts的原编号，MultiblockBlockFactory识别主阀spec后创建TankControllerBlock；两版启动都经过BlockBootstrap，Neo也注册了主阀流体能力。

| 结构／主阀族 | 数量 | 原编号 |
| --- | ---: | --- |
| 3³处理木 | 1 | 17001 |
| 3³普通金属 | 6 | 17002–17007 |
| 3³致密金属 | 6 | 17022–17027 |
| 5³普通金属 | 6 | 17042–17047 |
| 5³致密金属 | 6 | 17062–17067 |

逐条比较原Loader_MultiTileEntities实际注册行：编号、边长、壁板编号、容量、四种防护标志、木罐simpleOnly及硬度／抗爆字面量相符，最大容量2,048,000,000 L。回执保留25条原行号、摘要和当前实际注册路径。

这里有明确的材料比较边界：MT.Ad／MT.Ti分别按Adamantium／Titanium名称映射；四条原ANY.W只和当前Tungsten代表名称比较。原ANY.W还注册MT.W与TungstenSintered成员，不能把名称相符当作材料组、合成原料范围或运行时物性全部一致。这部分留在后续配方／物性核查。

GTBlockEntities的MULTIBLOCK_TANK构造支持数组先列17002、17042，后拼全部spec，确有重复的支持引用；它不是两次方块注册，本批不据此报告多注册了两个主阀。

## ALIGN-262：主阀普通灌入角色与形成门控不同

原MultiTileEntityTank的getFluidTankFillable2返回null，getFluidTankDrainable2返回mTank。沿Base06覆盖板包装和Root的普通IFluidHandler.fill／drain追踪，在无覆盖板条件下：主阀普通灌入为0，普通排出可直接访问罐内容，未在这条主阀路径加结构形成检查。原壁板另走带part参数的选择器，原funnelFill／tapDrain也直接操作mTank，不能把普通灌入的禁止套到漏斗／龙头。

当前[MultiblockTankControllerBlockEntity](../../multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/MultiblockTankControllerBlockEntity.java)将同一个tankHandler提供给主阀各面及FLUID_IO部件，fill和两个drain都先调用isStructureOk。有限源码分支见证：

| 条件 | 原版 | 当前 |
| --- | --- | --- |
| 已形成、空罐、无覆盖板；普通接口向主阀提供1000 L水 | 接受0 L | 接受1000 L |
| 未形成、已存1000 L水、无覆盖板；普通接口从主阀请求1000 L | 排出1000 L | 排出0 L |

这分别是灌入角色差异与失效提取保护，不是同一个缺陷。不能为逐字对齐直接移除当前结构／区块保护，也不能把已有funnel／tap例外误报成原主阀完全不能手动加液。没有运行管道、桶或玩家交互测试，本批未改变玩法。

## ALIGN-263：拆下主阀携带整罐内容，原物品链不携带

原Base04.getDrops调用registry.getItem(id,writeItemNBT(newTag))。Base04只写自定义名称和染色，Base05增加保留库存，Base06加入覆盖板并调用默认空的writeItemNBT2；已追继承链及储罐子类没有把mTank写入物品。世界writeToNBT2保存罐内容与此不同，不能混用两条保存路径。

原MultiTileEntityTank.breakBlock还调用GarbageGT.trash(mTank)，其CS实现从源罐remove后计入垃圾流体。即使不依赖原破坏／掉落调用顺序，物品写入路径本身也没有流体内容。

当前[TankControllerBlock](../../multiversion/src/main/java/com/gregtech/gregtech/block/machine/TankControllerBlock.java)的getDrops和clone将BE保存数据写入Forge BlockEntityTag／Neo BLOCK_ENTITY_DATA。saveAdditional含gt.tank，ComponentCoverFallback.forItem原样返回；最终已配置的CoverDropMixin又调用CoverDrops.capture，后者明确将MultiblockTankControllerBlockEntity排除在机器运行数据清除之外。因此不能只看到通用清理代码就断言这里已经清空。

放置时setPlacedBy显式读回这些数据，MultiblockTankData.readContents恢复流体，同时按放置主阀身份固定容量。1000 L水的主阀物品会携带并恢复1000 L，恢复后是否可抽取仍受262中的结构门控限制。本条只针对这些多方块主阀，不扩成普通便携桶罐都应该丢内容。

已读取两版原生实际采掘／放置消费者：ServerPlayerGameMode捕获BE、移除成功且能采掘后交给Block.playerDestroy；其dropResources→getDrops带BLOCK_ENTITY，BlockState分派到当前块的getDrops。BlockItem.place调用setPlacedBy。Forge为已有映射jar的只读javap，Neo为配置21.1.243的源jar，不是模拟调用替代真实入口。生存采掘、创造取样和磁盘重启尚未运行。

## 结构、查询成本和工具入口的既有记录扩围

036：原3³／5³检查四个XZ角是否存在，缺关键区块返回mStructureOkay；扫描中继续检查并逐个认领有效部件。当前StructureGrid.hollowTank逐格检查已加载状态，遇首个失败即返回；BE失败时释放绑定，仅在全部候选有效后整体认领。保留现代已加载保护，不能照搬旧缓存或部分绑定而引入陈旧主控访问。

232：当前每次主阀fill／drain都会重跑结构验证，部件获取目标和tick还可能额外验证。完整3³检查含25个壁板与1个内部空气状态，至少26次方块状态读取；5³含97个壁板与27个空气状态，至少124次。20次完整探测分别至少520／2480次，另有候选与绑定工作。这只是源码调用数，未测TPS；getFluidInTank／getTankCapacity等只读查询没有此形成检查，不能报告所有流体查询都全扫。

233：当前主阀BE只实现MultiblockPortOwner，父类／接口及已配置BlockEntity覆盖板Mixin都未补MultiblockToolTarget。GTToolItem真实调用MultiblockTools，但目标检查失败会PASS。原Base10建造棒把坐标／玩家／库存交给checkStructure2，再检查形成；放大镜进入形成与内容诊断。当前25组ControllerStructureLayouts经MultiblockInfoData／预览面板消费，不会因此获得专用建造入口。

温度计已有BehaviorThermometer对主阀的显式分支，不列为缺失。现有危害、流体物性、自动输出、其它工具、Jade完整显示、客户端资源和配方仍需继续逐链核查，不能由本批结论推为都正确或都缺失。

## 源码整理和证据限制

五份共享维护源只整理空格、压行、循环／链式调用和明确局部绑定：serverTick的be改controller，保存f改storedFluidTag，放置ctx改placement，ticker参数补含义，spec局部all／p改specifications／parameters，结构lambda改position，容量恢复临时量改storedAmount／storedFluid。五处说明改为已核实的职责和检查次序，其它注释／来源头及Stonecutter指令保持。

完整双版执行流逐项比较：BE2865／2759，Block1218／1586，Spec313／313，Hollow206／206，Data177／190。比较仅归一明确的局部绑定，不归一成员访问；例如TankValveParameters.all()必须原样保留。另比较未归一的字符串／数值及其位置，防止局部改名掩盖常量或真实方法名变化。

未Java编译／目标初始化、Gradle、测试、游戏、构建或推送。只读源码和原生工件证据不替代实际世界与客户端验收；全部原版差异核查和可读性目标保持active。
