# 物流核心调度时钟、执行顺序和CPU计数核查

2026-10-21，第八十六批，基线 `4b5dcb44c`。接续[核心状态](LOGISTICS_CORE_STATE_AUDIT.md)、[搬运](LOGISTICS_TRANSFER_AUDIT.md)与[网络生命周期](LOGISTICS_NETWORK_AUDIT.md)，新增ALIGN-284的调度合同差异。来源、原作者头、完整平台投影和保持证据见[回执](verification/logistics-scheduling-audit-20261021.json)。正式284条包含已修、部分修复及政策，并非未修问题数或完成百分比；全面队列继续。

## ALIGN-284：全局服务端秒节拍改为持久化世界时间相位

原GT_API_Proxy.onProxyBeforeServerStarted把SERVER_TIME清零。在ServerTick START中，先按递增前的SERVER_TIME%20设置SYNC_SECOND，再递增并分派SERVER_TICK_PRE。原物流核心在自己的服务端onTick2注册该回调，onServerTickPre仅在SYNC_SECOND时扫描和路由，每个回调仍扣固定耗能。

当前[核心BE.serverTick](../../multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/LogisticsCoreControllerBlockEntity.java)由实际方块实体ticker调用，以level.getGameTime()%20==0作为普通生产路径的秒门槛。原生ServerLevel在正常推进时间的路径先递增世界gameTime，再tickBlockEntities。PrimaryLevelData从Time字段读入gameTime，并将其写回Time；Level.getGameTime读取levelData的同一值。因此这不是原SERVER_TIME的等价替代，世界保存的余数会影响路由相位。

限定核心已注册、结构形成、能量足够且正常加载，世界每个服务器步骤推进一次时间。以服务器启动后步骤j计，原全局门槛看j−1，当前世界门槛看保存余数r+j。只比较步骤21–60，排除原核心首个普通方块tick才注册回调的问题：原门槛在21、41；r=0时当前门槛在40、60，r=19时在21、41。20种余数中19种相位不同，但每种情况下两者相邻尝试仍相隔20步。

这是20项限定源码时钟分析，不是实际启动／重启测量，也没有确认某条实线会因这次差别丢物品或降低稳定吞吐。现代DerivedLevelData.getGameTime还会委托所包裹的世界数据，不能据“使用世界时间”就声称原版维度之间必然不同步。暂停、冻结、跳过世界tick、可选维度、同tick供电或多个核心交错需要继续核；本批不更换调度时点。

## 已对应的优先级和停止条件

原tExports1／tImports1、tExports2／tImports2四组数组与嵌套循环给出以下顺序，当前[LogisticsRoutingRules](../../core/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsRoutingRules.java)一致：

1. 导入依次寻找过滤、半过滤、普通导出，再寻找相同降序的存储；每个目的等级内，源按普通、半过滤、过滤顺序尝试。
2. 存储按普通、半过滤、过滤源等级寻找过滤、半过滤、普通导出。
3. 仅从普通存储整理到过滤存储，再到半过滤存储。
4. 最后才把未预留物品从普通存储送到Dump。

每个等级配对先试流体，再试物品；有成功操作后本逻辑CPU轮次停止寻找其它配对，下一轮重新从高优先级开始；全失败则停止余下轮次。将没有成功时的路线展开，共59个等级／通道尝试位置，原与当前顺序相同；另列59个“首个成功在位置n”的条件，核其停止前缀。位置数不是网络端点数，未把原生库存、能力模拟或完整调度当作已执行。

共享[LogisticsRoutingPass](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsRoutingPass.java)实际把同一套规则接到物品／流体Session。物品Session的转换循环及目标数量限制继续保留，失败物品返还仍检查余量并尝试掉落；ALIGN-255的严格数量政策和ALIGN-257的流体返还缺口没有被这次顺序核查覆盖或撤销。

## 能量公式和满载显示：保持的合同与旧计数问题

原路由开始要求128+logic×64×conversion，当前Counts.routingThreshold沿相同公式；两者都是先路由、再扣固定能量并夹到零。普通物品按实际件数扣能，流体按向上取整的升数／250扣能。原UT.Code.divup实际采用整除加余数判断，当前用(amount+249)/250；在四种转换CPU数量及列明的正流体量边界条件上相等。没有以此宣称所有外部超大堆叠、任意能力返回或整条产线都不会透支。

原while(++oCPU_Logic<=mCPU_Logic)在所有轮次成功后还会执行一次失败的条件递增，留下logic+1；当前usedLogic统计实际成功操作数，满载为logic。若某轮全失败，原随后减一，计数与实际成功数相同。这是原始值差异，不能称两份源码的原始计数完全相等。

但原显示在used>=total时直接输出红石15、纹理10，当前[LogisticsDisplayRules](../../core/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsDisplayRules.java)也有同样的饱和判断。对总数1–108和成功数0–total的5994项限定算术投影，信号／纹理结果相等。原始保存数值、对外API和客户端实际结果仍没有被这份算术验证代替；不为追求字面一致恢复旧多加1的问题。

## 可读性整理与实际门槛入口

展开LogisticsRoutingRules的短条件，logic／conversion／cycle／moved等改为logicProcessors／conversionProcessors／passIndex／passResult，并将角色、等级、转移量分开命名；LogisticsDisplayRules展开判断，参数明确为usedProcessors／totalProcessors。

展开[LogisticsItemRouter](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsItemRouter.java)的记录类型、停止／空能力分支和长表达式；收集阶段hostPos／hostEntity等改为hostPosition／hostBlockEntity／targetPosition／targetBlockEntity，转换循环和槽位循环使用conversionIndex／sourceSlot／destinationSlot。成员、字段、方法签名类型、注册、顺序、数值及字符串不变。

核心BE只纠正一条说明：public routeNetworkOnce辅助方法自身没有一秒时间检查，普通生产路径由serverTick提供门槛。在core、共享及两端专有生产源搜索没有发现另一个普通调用入口；这不是反射／外部调用的全面证明，也不把辅助方法误报为当前生产中持续额外路由。

四份源码六份完整执行流在限定局部名称归一后保持：716、234、2240／2241、3094／3135；字符串／数值／字符字面量及其位置、其它注释、Stonecutter指令另核。core路由77→87行、最长122→118；物品路由249／247→283／284行、最长136／138→117。BE仅改说明。8份原版／17份当前输入及26份共享平台投影留痕；两份材料表保持。

原生证据复用第73批所需六个工件条目与三份只读javap，重新核摘要；新增PrimaryLevelData／DerivedLevelData的两份源码、两份类条目和两份只读javap。未Java编译／初始化目标类、Gradle、测试、游戏、实际路由／时钟／重启／UI、构建或推送；整体移植仍未完成，全面功能和源码标准化尚未完成。
