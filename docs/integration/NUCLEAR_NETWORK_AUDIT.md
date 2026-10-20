# 核反应堆中子网络／空间／tick 分派核查

第五十九批，2026-10-21；观察基线 `67dff604decf8f08df3c09dd12ede04a1158b22f`，登记 ALIGN-209–210。先完成只读源码核查，再归档本报告；没有修改生产行为，未运行测试、编译、Gradle、游戏或构建，未推送。棒目录和单棒公式、冷却／流体／停机／保存／故障及接触运行门控由同批其他专题负责。本稿只使用这些公式的实际消费者来核网络顺序，不重复登记。来源、双端投影、原生工件和视图哈希由[本批统一回执](verification/nuclear-contract-audit-20261021.json)归档。

原根 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；当前根 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team、Gregorius Techneticies、LGPL-3.0-or-later 头。本稿没有复制第三方原生方法体，也不把本地快照冒称实时上游。

## 结论和已对应部分

1×1 与单块内部四棒的 2×2 结构已有双端注册／ticker／全局事件实际入口。四方向、0–3 槽位、2×2 内部邻接及 1×1 每面分两束的合同逐项对应；没有发现“东西／南北槽位整体映射错误”。原有两阶段传播与当期反应的分离已存在，当前内置棒的发射读取上一周期、接收写下一周期缓冲，不能仅凭换成 ReactorNetwork 就判定没有移植。

确认共享辐射消费者少了原 creative／IronGolem 豁免。曾提出的“tick18 多一热 tick”候选在继续追到真实棒消费者后排除：旧反应读取 o，而清零的是 m；不能把内部累加器清零当成反应入口通量清零。原 Universal Extender 的委托邻接在现代直接位置网络中不再参与；这应明确记录兼容／能力取舍，不能把所有普通相邻核一起报错。加载但未 ticking 的邻核、全球时钟／每维度时钟、距离采样中心等边界单列，未在游戏中验证。

## 真实注册、ticker 和事件入口

原 Loader_MultiTileEntities.java:734–738 注册 core1×1=9300、core2×2=9200。ReactorCore 基类 readFromNBT2:78–81 和 onTick2:133–136 将自身加入 GT_API_Proxy.SERVER_TICK_POST／SERVER_TICK_PO2T；不是只有接口定义。

GT_API_Proxy 构造函数 :154–155 注册 Forge 和 FML 总线，onServerTick 在 START 更新 SERVER_TIME（:252–255），在服务器 END **先遍历整个 POST 列表调用 onServerTickPost(true)，再遍历 PO2T 调用 false**（:478–509）。两循环各自丢弃 isDead 核心，普通 ticking 与 post 生命周期见下文。

当前共享前缀为 `multiversion/src/main/java/com/gregtech/gregtech/`：

| 环节 | 实际消费者 |
| --- | --- |
| 方块 | registry/GTEnergyNodes 的两版本分支分别注册 reactor_core／reactor_core_2x2，使用 ReactorCoreBlock／ReactorCore2x2Block |
| BE | GTBlockEntities 的 Forge :291–299、Neo :53–54，将两个注册方块绑定 ReactorCoreBlockEntity／ReactorCore2x2BlockEntity |
| 框架提交 | Forge MachineBlockBootstrap:25,50 及 DeferredRegistrySubmission 提交相应注册；Neo GregTechNeoForge:89,106 提交 BE／能源方块注册 |
| 普通原生 ticker | ReactorCoreBlock.getTicker 检查服务端及匹配 BE 类型；2×2 getTicker 服务端 lambda 检查 instanceof，均调用继承的 serverTick → ReactorNetwork.enqueue |
| 末尾事件 | ReactorNetwork 的 Forge 分支接 LevelTickEvent.END，Neo 分支接 LevelTickEvent.Post；过滤客户端后 drain 当前 Level 的队列，%20==19 exchange，然后每核心 react |

原生版本：Minecraft1.20.1／Forge47.4.20、Minecraft1.21.1／NeoForge21.1.243；Neo 实际 FML loader4.0.43。Forge ForgeEventFactory.onPostLevelTick 向 EVENT_BUS 发 END；实际 MinecraftServer 字节码在 level.tick 后调用它。Neo MinecraftServer.tickChildren:1034–1043 在 serverlevel.tick 前后调用 fireLevelTickPre／Post，EventHooks.Post 将事件发至 GAME bus。实际自动订阅器已核：Forge 注解默认 FORGE，Neo 默认 GAME（现 loader 还按事件是否 IModBusEvent 分类注册）；两者均注册带 @SubscribeEvent 的 ReactorNetwork，故这不是未被调用的理想 helper。原生 class／source／jar entry 哈希和方法锚点在 sources.json。

## 空间布局：内置直连合同已对应

原 ReactorCore.java:153–158 的 S2103／S0312 和注释明确世界 X／Z 槽位；当前 i/2、i%2 采用相同格局：

| 槽 | 世界相对位置 | 2×2 内部收件槽 | 越界方向 |
| --- | --- | --- | --- |
| 0 | 西北 | 1、2 | NORTH、WEST |
| 1 | 西南 | 0、3 | SOUTH、WEST |
| 2 | 东北 | 0、3 | NORTH、EAST |
| 3 | 东南 | 1、2 | SOUTH、EAST |

相邻 2×2 核心在对面接收槽：向 NORTH 为1/3，SOUTH为0/2，WEST为2/3，EAST为0/1；具体 edge 由切向 x 或 z 保留。逐项代入原 S2103／S0312 的实际调用，与当前 edgeSlot(side.getOpposite(), edge, slots) 相同。接收 1×1 时目标槽总为0，原 1×1 的 getReflection 也忽略传入槽并作用自身0槽。

原 1×1 :62–72 将 emission 向上取整除2，每个水平方向调用两次，覆盖相邻 2×2 的两个边槽；当前 :68–71 相同。若相邻也是1×1，会对同一杆接收两次半束，包含奇数发射时的上取整影响，**这不是当前新引入的重复发射**。Moderator 接触次数也应按两次接收看待。

两版都不向 UP／DOWN 传播，壳体不是中子中继。当前 ReactorInteraction.topSlot 与 ReactorRodRenderer 也使用 (i/2,i%2) 的世界 X/Z 分格；2×2 的 FACING／热流体输出方向没有旋转中子槽位，原格局同样不是按主输出面旋转。没有实际渲染验收，只核坐标消费者。

## 两阶段传播与 finishCycle：正常内置棒已有对应

原第一 post pass 只在 %20==19 发射，并通过核心的 getEmission 增加自发中子；邻棒 getReflection 接收或反射，原缓冲 m／o 分开。第二 pass 在19更新每棒 moderation，然后计算反应。

当前 ReactorNetwork.exchange 先对所有本 tick 入队核心构造 incoming=self、emissions=emission(previous neutrons)、moderated=上一周期标记三个快照；再逐边发送，最后拷贝 incoming 到 neutrons，全部 finishCycle，然后 end 才调用 react。内置 Nuclear 发射读取上一期 oNeutronCount，而 receive 只写 mNeutrons／下一期 moderation；Moderator 反射用上一期 oModeration，receive 增 mModeration，原 updateModeration 才交换。当前 RodPhysics 的 *_next／finishCycle 相同用途。因此对这些内置正常棒，先快照发射没有制造“已收到邻棒中子立刻再发射”的额外传播。

此结论不包含任意外部 IItemReactorRod 自定义副作用，也没有证明原 int += 溢出与当前 RodPhysics.bound 饱和的所有极值行为等价。排序 BlockPos.asLong 只提供当前稳定遍历，不等于所有流体、故障和扩展提供者的全局顺序均已验收。

## 排除误报：tick18 清零累加器，旧反应仍读取 o

原 Core1x1 第二 pass 每 tick 先 `oNeutronCounts[0]=mNeutronCounts[0]`（:84），随后 getReactorRodNeutronReaction:197 在 SERVER_TIME%20==18 执行 m-=o；原2×2 :108,224 对四槽同样处理。**但真实棒反应并不以此后的 m 作为通量：**

| 旧棒消费者 | 实际读取 |
| --- | --- |
| Nuclear:204–225 | :205 加热读 o；:215 的最大通量寿命判断和损耗也读 o |
| Breeder:82–92 | :83 加热 o/2；:84 寿命损耗读 o |
| Absorber:45–47 | 加热 2×o |
| Product:54–56 | 加热 o/2 |

因此18号相位只是把下一轮发射累加器 m 清空；o仍为旧周期N，18的实际热／寿命也按N计算。下一次19发射读取上一周期o，接收累加到新m，再把新m快照成o后反应。

当前 exchange 在19用 incoming=self 建立新累加器，emission读取保存的上一周期neutrons；send结束后拷贝incoming为新neutrons，react每tick消费它。18时旧neutrons仍N恰好对应旧o=N，而非应对应已清零的m=0。正常持续运行的内置棒周期中，两者都是20次按当期N消费，不是19次对20次。

有限核对例：给定稳定N=512、棒未转换、冷却足够且无状态切换，除热倍率前旧／当前周期中子热均20N=10240。旧18内部状态为m=0、o=512，但真实Reaction的flux仍512；当前18的neutrons=512。同理不能从m清零推导Breeder在18不扣寿命。

先前草稿的“旧19N=9728／今20N=10240”数学模型误用了旧m而没有追到棒的o消费者，现已撤回；这是排除记录，不登记缺口，也不以额外测试冒充修复。原SERVER_TIME全服务器启动计时与当前每Level持久getGameTime的绝对相位／重启政策仍不同；保存、停机、禁槽等改变条件由另一代理核，本排除只针对正常持续周期。

## ALIGN-209：原委托邻接未进入当前直连位置表

原 getAdjacentTileEntity(side) 默认允许 delegates；Base01Root:219–224 遇 ITileEntityDelegating 会调用目标的 getDelegateTileEntity，并保留其返回的目标面。核心只检查返回对象为 ReactorCore 且目标面水平，因此不只接受物理紧贴的原始 BE。

实际已注册的例子为 Universal Extender30255（Loader_MultiTileEntities:1960），其 ExtenderController 继承 Extender。mModes=EXTENDER_ALL 时，Extender.getDelegateTileEntity:143–149 按配置的主／次面返回另一侧目标（:422），允许核A—万能扩展器—核B，或转向至另一水平面。两端加载且旧目标是活核心时，核心传播会调用被委托核心；该能力不是只存在抽象接口。

当前 ReactorNetwork 仅从本 tick 核心建立 BlockPos→Core 表，positions.get(sourcePos.relative(side)) 不解析扩展器或任何委托目标。无论 SourceExtender 的物品／流体能力是否已工作，扩展器都不是队列里的 ReactorCoreBlockEntity，故这里不会把中子传到后方核心。记录为原委托邻接能力收窄／迁移取舍；没有扩成“所有扩展器失效”或“普通直邻传播错误”，也不自动要求恢复跨维度 MiniPortal 等更广泛委托。

## ALIGN-210：实体辐射缺少原有显式豁免

原第二 pass 每20tick的10号相位，用 sum(neutrons) 向上取整/256 作为范围强度，遍历同世界 loadedEntityList 的 Living，先检查 |dx|／|dz|≤200，再减实体到核心整数坐标的欧氏距离；正强度调用 UT.Entities.applyRadioactivity。当前 react 在 %20==10 且 neutronTotal>0 调用 irradiate(1)，AABB取同世界Living，再调用 ReactorRadiation.apply。玩家实际进入 PlayerRadiation.change 持久dose及其 PlayerTick症状事件；非玩家应用药水，不是只写 Jade 数字。

原 UT.Entities.isWearingFullRadioHazmat:2898–2901 先豁免 creative、原版 Wither／IronGolem，再检查四件防护；applyRadioactivity:3034 实际调用该 helper。当前 ReactorRadiation.apply 只排除死亡、undead／arthropod和四件tag护甲，protectedByArmor 没有 creative／IronGolem特例；PlayerRadiation.change 也没有该前置豁免。默认无防护的活 creative 玩家通过周期辐射入口会增加持久dose，原 helper 会拒绝；这只确认剂量累计，不宣称原生创意免伤失效。普通 IronGolem 的旧 RadioHazmat 特例也没有对应，现路径会尝试 addEffect；未逐项验收其六种药效的原生 canBeAffected、实际生效或伤害，故不把所有效果都写成已施加。Wither 已受 undead 条件保护，不另报缺口。故障和接触入口共享这个消费者，但不重复登记另一代理审的接触运行门控／冷却故障触发条件。

几何采样也非逐点等价：当前距 Vec3.atCenterOf(core)，原距整数方块坐标；现代AABB用实体碰撞盒相交筛选，原先按实体位置的水平距离筛选。有限几何反例：核心(0,64,0)、通量512即exposure2，实体位置(.5,65,.5)，原距离√1.5≈1.224745，截断强度0；当前中心距离.5，截断强度1。该点位于块上方，可避免把接触辐射混作周期辐射。此为距离原点／筛选政策差异，不声称中心采样必应回退，也没有游戏里量测实际剂量。

## 原生 ticking、卸载和未验边界

原 Core 注册两个全局post列表后，普通tile ticker不再是每次post执行的条件；Base01Root.invalidate／onChunkUnload 置mIsDead，GT_API_Proxy两列表在END过滤并onUnregisterPost，使加载后可重注册。默认跨区块邻接先 blockExists 再WD.te，不无条件强加载目标区块。

当前每 tick 必须先由原生 BE ticker enqueue，END只处理本次队列。原生 Level.tickBlockEntities、LevelChunk.BoundTickingBlockEntity 与 LevelChunk.isTicking 的实际入口还要求 block ticking range、FullChunkStatus.BLOCK_TICKING／实体加载状态、有效类型和世界边界等；Forge实际class与Neo源码均有这些门控。**“区块存在／已加载”不等于“本tick该BE运行”**。已加载但不 ticking 的邻核不在positions表，不接收／反射；这是当前明确参与边界，不把现代 simulation distance 自动判为未移植。

END再次检查isRemoved及hasChunkAt；完全卸载／移除后排除，本身不会为了邻核调用 getBlockEntity／getChunk。正常tick drain完成后不保存持久邻居缓存；本次没有实测卸载恰在enqueue与END之间、维度拆除、跨边界恢复或服务端异常中断。WeakHashMap注释不能替代GC验收：值持有Core，而Core持有Level；未证明异常未drain时仅弱key即可释放整个维度。

原全局post针对单个核心捕获Throwable并移出相应列表，当前事件／react异常如何由框架处理不在本轮构造故障验证范围，不据此宣称所有正常核心会崩溃。未复原所有原1.7.10 Forge二进制补丁，也未将当前映射／反编译阅读当作真实多人、世界、存档或游戏验收。

## 并行排版与来源

ReactorCoreBlockEntity 的67d Git原始SHA256为 `06bd7b53a2f9c453f8ecc91f23d07389266d21a0816a96ce6925e8c24bd44e04`，236行；首次工作树观察同236行，raw为 `7e8963ad5bda51505373466307e8d293d0563a8351c25d7e62bb4d8be03949ce`。根代理排版至560行、最终raw `ef616196f083d493deca21f66563494540f49dba01e98fceeb9c72f094b0c44b`；其 work/reactor-core-readability-20261021/proof.json 证明两端3703／3657执行tokens、字面量位置、注释和指令保持。方法锚点优先于旧行号。ReactorNetwork未改动，时序与空间审计不会因该排版变为玩法修复。

[工作来源清单](C:/Dev/GregTech6/work/nuclear-network-audit-20261021/sources.json)保留21个原源码、22个当前源码、34个双端投影视图、7个原生artifact及14个字节码视图的绝对路径、相对路径与SHA256，以及条目哈希、布局表推导和有限反例。投影视图仅在内存读取共享维护源，不是新增业务实现；原生视图只记录方法锚点和哈希。统一回执归档这些证据，不依赖忽略目录持久存在。没有增加测试夹具或运行代码来冒充原生场景验证。
