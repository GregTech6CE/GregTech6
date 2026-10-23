# 核反应堆网络生命周期与异常边界审计

第六十一批，2026-10-21。观察基线b11081f4f，随后可读性提交f252bcf5d未改网络行为。ALIGN-214登记未到END排空的条件性持有边界，ALIGN-215登记post反应的异常隔离边界；没有故障注入、崩溃复现或堆／GC测量，不据此宣称正常核心会崩服或普通换世界必泄漏。

已先读工作约定、四份中央文档、第五十九批 NUCLEAR_NETWORK_AUDIT.md 和 nuclear-contract-audit-20261021.json。本稿不重复209／210，也不恢复 phase18 误报：原棒 reaction 消费 o，正常内置棒为20N；这里仅核队列持有、原生事件次序、卸载和异常传播。

原根 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；当前根 `C:/Dev/GregTech6`。原源码保留 GregTech-6 Team、Gregorius Techneticies、LGPL-3.0-or-later 头；原生证据来自已有本地依赖，不复制第三方方法体。Minecraft／loader 为1.20.1／Forge47.4.20 与1.21.1／NeoForge21.1.243。实际总线依赖为 Forge eventbus6.2.33、Neo bus8.0.5，由 userdev config／Neo module 元数据确认，非仅取任意缓存 jar。JDK21 源只用于核 WeakHashMap 持有语义，不冒称当前运行的 Java 版本。

## 核心结论

正常完整 Level tick 的队列能及时排空；**当前 END 处理器先 remove，再 exchange／react，所以本处理器自己的异常不会把该 Level 的 PENDING 条目留下。** 不能从 WeakHashMap 的持有链直接宣称每次换世界、普通卸载或每个反应堆都已经泄漏。

有两个可以限定条件证明的控制流边界：

- 若核心已经入队，而该 Level 在到达 ReactorNetwork.end 之前中止，并且以后不再对同一个 Level 对象派发 END，当前没有 unload／stop 清理或有效弱引用兜底。Set 强持有 Core，Core 的原生 level 字段强持有 key；弱 key 本身不能回收这条链。实际原生 tick 失败可以跳过 Post，实际 stopServer 会发 Unload，但当前没有对应消费者。这里证明条件成立时的滞留路径，未做堆快照、GC、反复换世界或异常注入，不声称已测出内存增长。
- 当前 exchange／react 在 Level END 事件内执行，失去原全局 post 队列逐核心的 catch／移出／错误标记边界。条件为其中一个调用抛出 E：当前跳过后续调用并把 E 交给会重抛的原生总线；原 post 在自己的回调 catch 内可记录并继续其他成员。真实外部流体能力回调位于 react 的 pushTank 中，说明并非只有纯数学运算；没有断言已有某一提供者一定会抛出，也没有构造无效棒或坏档来冒充实际崩溃。

## 全部真实 PENDING 使用者与正常排空

当前维护源缩写相对于 `multiversion/src/main/java/com/gregtech/gregtech/`。

| 环节 | 实际入口／持有或移除动作 |
| --- | --- |
| 原生核心 ticker | block/energy/ReactorCoreBlock.getTicker 与 ReactorCore2x2Block.getTicker 的服务端实际注册路径，已在59批核实；进入 ReactorCoreBlockEntity.serverTick:148–150 |
| 入队 | ReactorNetwork.enqueue:38 仅检查 core.getLevel 非空且非客户端，以 core.getLevel 为 key，computeIfAbsent 创建 HashSet 并加入同一 Core |
| Forge 排空 | ReactorNetwork.end:46–52 仅 END／服务端，:48 先 PENDING.remove(event.level)；随后过滤、排序，19号相位 exchange，逐 core.react |
| Neo 排空 | 同文件 :40–44 的真实投影分支处理 LevelTickEvent.Post；:42 同样先 remove，再进行其他工作 |
| 成员生命周期 | Core 没有覆写 setRemoved／onChunkUnloaded 来移除该队列成员；end 本身会过滤 isRemoved、hasChunkAt |
| 其它清理 | 生产维护源中无 ReactorNetwork 的 unload、server stopping/stopped、clear 或第二个 PENDING 消费者；全引用检索仅 serverTick 的 enqueue 与本类两个平台 end |

HashSet 在同一 tick 内按 Core 身份去重，普通单线程 BE ticker／同 Level END 的调用链没有新增跨线程调度。当前未证明外部任意异步调用公开 enqueue API 的安全性，也不把这种未接入的调用当作已有竞态。

正常顺序为 enqueue(L,C) → END remove(L) → 本地 pending／cores → 返回。Map 移除后本地集合活到方法退出，随后没有 PENDING 长期邻居缓存。即使过滤、排序、exchange 或 react 在 remove 后抛出，留在栈上的本地 pending 也不等于 map 里仍有 L；不能把“处理失败”和“当前 key 尚未移除”混为一件事。此后若新 tick 再 enqueue，则是一个新条目。

## 原生 Post 并非 finally，真实卸载事件没有清此队列

Forge 实际 MinecraftServer.tickChildren 字节码：serverlevel.tick 的范围为 offset130–136，异常目标139包装 ReportedException，post 调用位于172。异常表只覆盖 tick，不存在保证 Post 的 finally。ForgeEventFactory.onPostLevelTick:953–955 把 END 事件发到 MinecraftForge.EVENT_BUS。

Neo 实际 MinecraftServer.java:1034–1043 同样为 Pre → try level.tick → catch 后 throw → Post；Post 在 try／catch 之后，不是 finally。EventHooks.fireLevelTickPost 把事件发到 GAME bus。两端 LevelChunk.BoundTickingBlockEntity 只包围实际 ticker 调用；默认 removeErroringBlockEntities=false 时，会包装并重新抛出 ticker 异常，因此一个**已先入队核心之后**的后续 ticker 异常能够中止该 Level，跳过 Post。若配置为 true 并成功移除了出错 BE、世界 tick 继续，则此场景仍能到达 Post，不能不分配置概括。

两端 stopServer 的真实路径都在关闭 ServerLevel 前发 LevelEvent.Unload：Forge 字节码 offset266–284；Neo MinecraftServer.java:624–628。LevelEvent 文档亦明确事件由 stopServer／客户端世界切换等入口派发。ReactorNetwork 没有订阅此事件，亦没有 ServerStopped 清理。

FluidPipeTickPhases:32／37 确实订阅两端 Unload 并删除它自己的 PENDING；两类的 private static map 是不同对象，不能因此认为反应堆获得了同样清理。本稿仅将流体管道作为实际接线的对照，不重新审计它的全部生命周期。

## ALIGN-214：弱key反向强持有与未排空生命周期

当前链为静态 ReactorNetwork.PENDING → WeakHashMap entry 的强 value → HashSet → Core → 原生 BlockEntity.level → 同一 Level key。两端实际 BlockEntity 的 level 均为普通受保护字段；getLevel／setLevel 直接读写。Forge setRemoved 置 remove 标志并失效能力，onChunkUnloaded 失效能力；Neo setRemoved 同样置标志及失效能力，IBlockEntityExtension.onChunkUnloaded 默认为空。这些默认回调没有把 level 置空。

两端 LevelChunk.clearAllBlockEntities 会调用 onChunkUnloaded／setRemoved、清区块中的实体表与 ticker；这能断开区块拥有的引用，但没有反向遍历 ReactorNetwork.PENDING，也没有清 Core.level。被移除 Core 仍在尚未 drain 的 Set 时，isRemoved=true 只有在未来 end 的过滤阶段才会被看见。

JDK21 WeakHashMap 源 :90–102 明确 value 使用强引用，Entry:712–715 的 value 亦为普通 V 字段。因而这个 key 仍从静态 value 路径强可达时，清理过期弱 key 的操作也不能代替显式生命周期清理。此结论针对该对象图，不以“用了 WeakHashMap”或注释所说的“本 tick 通常很短”视为已经修复异常生命周期。

有限控制流见证，不是执行过的场景：

1. L 的合法原生 ticker 已令正常核心 C 入队，PENDING[L]={C}。
2. 在 ReactorNetwork.end 运行之前，后续原生 world tick／更早的 END 监听器抛出 E，实际框架重抛路径使该次 end 没有执行。这里 E 为条件输入，没有假造现存提供者错误。
3. 服务器 finally 进入 stopServer，派发 Unload 并关闭世界；当前反应堆没有对应清理。默认移除标志不清 level。
4. 若该 L 不再获得下一次 END，map 条目仍存在，value→C→L 链阻止弱 key 自发回收。另一个新世界 L2 的 END 只 remove(L2)，不会扫描 L。

反例边界：E 发生在本处理器 remove 之后，不满足步骤2；同一个 L 后续正常 END 会移除残留；普通完整 tick 后 Unload 时 map 已无 L。没有核到默认原版正常卸载一定会打断在 enqueue 与 Post 之间，也没有证明独立专服崩溃后 JVM 还继续存在，故不把条件滞留写成必然的长期泄漏。

## ALIGN-215：post反应异常隔离边界迁到原生事件外层

原 GT_API_Proxy.onServerTick END:479–509 对 POST／PO2T 分别遍历。dead 成员被移出并调用 onUnregisterPost；活成员的 onServerTickPost(true／false) 单独在 try 内。回调抛 Throwable 时，只从该相位列表移出该项，调用 setError、打印，再继续循环。Base01Root.setError:1010 存错误字符串并发送错误包；不是悄悄把异常全吞掉。该保证只覆盖 try 内的回调，isDead／onUnregisterPost／catch 自己的 setError 再抛等不在这个保证中。

原 Core.onUnregisterPost:127 把 mHasToAddTimer 设 true；但异常 catch 分支没有调用它。故“异常后自动从两列表彻底隔离并下 tick 恢复”也不能从原源码推出：两个列表独立移除，正常 dead／坐标变化与抛错分支不同。坐标变更 :146–150 会移两表并设重注册标志。

当前 end 的 exchange 和 for core.react 都没有逐核心 catch，亦没有错误成员移除／错误记录。exchange 的跨核心传播并不是简单的可随意逐核心事务；本稿不提出未经设计的 catch 后继续算法。只记录给定 E 的执行合同：exchange 先抛则本次后续 react 尚未进行；第k个 react 抛则之前调用已做的状态修改不会回滚，其后核心本次未反应；此时 map 已移除，不是前节的未排空条目。

实际 react→pushFluids→pushTank 会调用邻居流体能力的获取及 fill(SIMULATE／EXECUTE)：Forge 是 target.getCapability(...).ifPresent，Neo 是 level.getCapability(...)。该外部回调说明异常边界可以触及非本模组代码；这仅是可达调用点，不断言 fill 通常抛异常、输入恶意，也不审计第三方能力事务保证。

原对应流体排出位于普通onTick2，不能把当前可达的fill抛错例等同为“同一fill异常原本受post逐核心catch保护”。本项比较的是post内反应／调度的捕获合同，不据此宣称所有第三方流体故障在原版都可恢复，或当前正常产线更易必然崩溃。

原生 Forge EventBus.post 在监听器分派循环 catch Throwable 后调用 exceptionHandler，继而把同一个 Throwable 重抛；Neo EventBus 的私有 post 同样先记录再重抛。两个实际 GAME／FORGE bus 的 builder 没有自定义吞异常策略。**事件总线有错误日志，不等于会继续余下核心或监听器。** MinecraftServer.runServer 的最外层会记录报告并进入停止流程，因此这里只能说异常能够逃出本模组的该 tick 处理，不应说未经任何原生处理直接永久挂死。

两端 removeErroringBlockEntities 只存在于 BoundTickingBlockEntity 的 ticker try 范围。原生核心 ticker 目前只 enqueue；实际 exchange／react 在后面的 Level END 才运行，故该设置不能给这些后移工作提供逐 BE 捕获／移除补偿。未注入 E，不宣称普通有效棒必会触发此路。

## 原队列的生命周期补偿与未证边界

原 POST／PO2T 是静态 ArrayListNoNulls，仍强持有成员；不是弱引用容器。Base01Root.onChunkUnload／invalidate 置 dead，之后某次服务器 END 的全局遍历能删除所有 dead 成员，无需按特定旧 Level 事件取 key。当前按单 Level remove 则不会在 L2 的 END 顺带扫除 L 的残项。

全原树引用检索没有发现这两个原 post 列表的统一 clear；原 onProxyAfterServerStopping:220–222 调的是 checkSaveLocation 与 MultiTileEntityRegistry.onServerStop，不是直接清 post；onWorldUnload 的候选处理器原本就是注释。不能因此声称原版在停止之后立即完全释放所有旧世界。旧1.7.10停止原生阶段何时给每个核心置 dead、极端退出时会否还有下一次全局 END，未复原其全部二进制调用。本稿保留“下一次全局 END 可清 dead”这个已证条件，不假造完整旧退出验收。

也未核任意异步 enqueue、热切换维度提供者、第三方对静态队列的反射改动、OOM／StackOverflow 恢复、GC 压力、集成服崩溃后客户端是否持续运行或独立多世界重启。没有恢复机器自爆／过载，也没有改中子、冷却、棒寿命或辐射算法。[统一回执](verification/nuclear-transitions-config-audit-20261021.json)保存源码、投影、native entry／javap身份、版本绑定和以上有限控制流，不能把来源阅读称为游戏验收。

## 本批源码注释纠错

以上原坐标／源身份对应观察时的f252bcf5d网络文本；归档后只更正PENDING的Javadoc，撤去弱key足以保证世界回收的表述，明确值反持key、正常先remove及缺少卸载清理。没有实现新的清理或恢复算法。Forge1228／Neo1229项完整执行流与字面量位置均保持，初始工作树／Git及最终字节另记于[回执](verification/nuclear-transitions-config-audit-20261021.json)，不能把注释纠正计为214／215玩法修复。
