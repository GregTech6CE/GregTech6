# 物流代理、覆盖板生命周期与异常边界核查

第七十三批，2026-10-21，基线`54d8cde85`。接续[核心状态](LOGISTICS_CORE_STATE_AUDIT.md)和[实际搬运](LOGISTICS_TRANSFER_AUDIT.md)，新增ALIGN-258–260，补充036的区块证据边界。24份原版、32份当前输入、46个平台视图，以及本机两个原生工件14个entry／7份只读javap视图见[回执](verification/logistics-network-audit-20261021.json)。这是持续全面审计的一批，不是整套物流的验收或260个未修复问题。

原只读根为`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，原作者与许可证头随来源留存，没有更改原目录。CompoundMaterials 和 WoodMaterials以本轮开始时的字节摘要保护。

## ALIGN-258：撬下物流过滤覆盖板丢失样本

原TileEntityBase06Covers的撬棍入口先取getCoverItem，再拆覆盖板并交给玩家或掉落。CoverData经AbstractCoverAttachmentLogistics→AbstractCoverAttachment→AbstractCoverDefault继承，最终getCoverItem保留mNBTs内的过滤样本。物品／流体过滤板都没有在这条继承链覆盖为清空数据。

当前[LogisticsCovers.remove](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsCovers.java)先移走faces内安装物，清显示信号／刻度，再返回`new ItemStack(result.getItem())`；LogisticsCoverInteraction的撬棍路径把这个返回值交给玩家。因此一个设为铁锭的过滤物品导入总线，原撬下物仍含铁锭样本，当前撬下物没有样本；同一共享方法也用于过滤流体板。

原优先级和目标数量放在CoverData.mValues，不在默认getCoverItem返回的mNBTs，本条不声称它们原来都能随撬下物保留。当前世界保存会序列化完整安装物，普通采掘还经过CoverDropMixin／CoverDrops的最终附着物捕获，不能将手动撬下的样本丢失扩大为所有存档或采掘都丢配置。本轮未改拆除玩法。

## ALIGN-259：物流扫描缺少原万能代理解析

原物流核心遍历tLogistics.getAdjacentTileEntity；Root默认允许代理，若相邻对象实现ITileEntityDelegating，就使用其返回的目标实体和目标侧面。原Universal Extender 30255和Universal Bridge 30755注册mModes=EXTENDER_ALL，实际getDelegateTileEntity会定位另一出口的邻居。原物流线的连接判断也使用同一代理结果，连接器还可通知解析后的对端连接，不是只有扫描中一条不可达分支。

当前[LogisticsNetwork.scan](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsNetwork.java)只读取物理邻坐标，要求该实体是LogisticsHost。两版均已注册万能扩展器，当前ExtenderBlockEntity→EnergyRelayBlockEntity→CapabilityRelayBlockEntity及原生BlockEntity没有实现LogisticsHost；配置中的BlockEntity混入只补FallbackCoverHost／PanelCoverHost，也没有物流节点委托。物品／流体能力转发和Neo能力注册实际存在，但它们不能替代网络发现的接口。

有限源码例：同世界全部区块加载、合法核心中心(0,0,0)、范围至少5，(2,0,0)有效通风部件接物流线A(3,0,0)，万能扩展器在(4,0,0)，出口接物流线B(5,0,0)，无其它路径或种子覆盖B。扩展器正面WEST、次面EAST，原从A的EAST查询以WEST进入代理，解析到B的WEST，连接／范围满足时可扫描B；当前物理邻居是扩展器，非LogisticsHost，扫描停住，原生线更新还会拒绝这个实体作为物流邻端。

这个例子没有实际摆放运行，也不把普通箱子经扩展器的物品／流体访问说成缺失。原代理还有跨坐标／范围和世界的更多合同，本轮不为修此项引入原来的范围绕过或无界代理。未来应明确网络节点解析边界，再衔接现有能力转发。

## ALIGN-260：核心回调异常从局部停用变成原生错误路径

原GT_API_Proxy在ServerTick START逐个调用SERVER_TICK_PRE列表，针对每个回调捕获Throwable：移除此项、设置Server Tick Pre错误并记录，随后继续列表。原物流核心的onServerTickPre运行扫描和搬运。异常移除分支没有调用onUnregisterPre，不能把它说成会立即自动重试或完全无副作用。

当前核心方块ticker直接进入BE.serverTick→routeNetworkOnce→LogisticsRoutingPass→Router，没有这层捕获。给定有效且有电的核心，参与搬运的外部源IItemHandler.getSlots抛RuntimeException，会沿这条链到原生BoundTickingBlockEntity；原版可对照的是对应库存查询引发的核心回调异常，不是虚构1.7.10已有现代IItemHandler。

本机Forge映射jar与Neo源码工件的实际原生ticker都捕获Throwable后创建崩溃报告。`removeErroringBlockEntities`默认false时抛ReportedException；设true时调用setRemoved并从区块移除方块实体。这与原仅停用一个预回调不同，也不等于移除普通方块或保全其物品。本条只是外部提供者抛错这一条件的隔离合同，不宣称正常内置线路普遍崩溃，不建议用开启删除选项代替正确隔离；本轮没有故障注入或更改异常政策。

## 区块、顺序、保存与调度：已有链路和未验收范围

原种子用WD.te(...,T)，但相邻访问不是一律强制加载：Root的mIgnoreUnloadedChunks默认true，Base02在跨区块且未加载时可返回null；缓存、是否跨界和目标类型会影响后续读取。原核心的checkStructure(false)还有已知缓存，而当前每次扫描前重新验证并清绑定，该部分仍归036。不能只看一次WD.te的true就说原来会加载整张网，也不为对齐关闭当前hasChunkAt保护。

当前Snapshot只保存不可变坐标集合，扫描使用LinkedHashSet和FIFO、种子按x/y/z遍历，端点列表依序收集；原扫描集合默认HashSetNoNulls内部是HashMap的keySet迭代，源码没有给出同级坐标顺序。保留当前可解释的物理发现顺序，不杜撰旧版固定先选哪台，也不把恢复旧实体哈希顺序当作修复。

物流线明确覆盖save／load、getUpdateTag和getUpdatePacket。Neo没有显式onDataPacket并不代表收不到：实际IBlockEntityExtension默认onDataPacket和handleUpdateTag调用loadWithComponents，再到loadAdditional。已配置的ComponentCoverHostMixin及ComponentCoverFallback.read/write也有物流覆盖板的独立分支，位于普通组件宿主门槛之前。原生saveWithoutMetadata新建CompoundTag，不能仅因save未删除空面的旧键就报告正常存档残留。上述只核数据调用链，没有启动客户端或重启世界。

原SERVER_TIME在服务启动清0，在ServerTick START用递增前值生成SYNC_SECOND；当前在方块实体ticker读取level.getGameTime()%20。两版现代原生ServerLevel都先tickTime再tickBlockEntities，故不能把当前回调描述为全局服务端前置调用。具体重新启动的初相、同tick供电／搬运可见性、多个核心交错、外部代理与真实客户端仍需单独验收，不据此声称量过延迟、TPS或普通维度时钟不同步。

## 源码整理

展开LogisticsNetwork种子三重循环与Snapshot方法，距离参数a／b改为origin／target；LogisticsCovers的保存／重载／掉落循环索引i改为faceIndex，拆开更新、序列化和拒绝条件；物流线方块实体展开两版保存／同步方法。ExtenderSpec展开八项属性和构造赋值，补全来源30255／30755说明；RelayRoutingRules由3行展开到12行，明确展示桥和扩展器的侧面选择，没有改运算顺序或公开方法。

五源码八份完整执行流在限定局部绑定归一后保持：ExtenderSpec 281、Network 574／574、Covers 1382／1420、Wire BE 381／377、RelayRoutingRules 66。原始字符串／数值位置另核，四处列明注释纠正，其余注释、Stonecutter指令和材料表保持。没有用小函数检查替代整份选中源码证明。

本轮未Java编译／初始化、Gradle、测试、游戏、构建或推送；javap是对本机已有工件的只读反汇编。全面核查仍在进行，来源和完整词法证明不能替代实际外部节点、磁盘重启、客户端或全部功能验收。

第八十六批继续[调度时钟与顺序](LOGISTICS_SCHEDULING_AUDIT.md)：新增284持久化世界时钟相位差；59等级／通道顺序及满载CPU显示投影已有。相位源码分析不替代实际启动、同tick供电／多核心、完整路由／保存重启或客户端验收，全面队列继续。
