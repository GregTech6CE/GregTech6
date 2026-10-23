# 库存变更通知合同：箱子、书架与瓶框

2026-10-21 第八十九批扩172：USB换槽原直接markDirty相邻且线缆匹配的库存；当前普通neighborChanged的基础机消费者仅置能源来源标志。它与下文箱子专用接口广播是不同发送机制，库存标志问题共同续核；保留自行查配方，不报固定12tick延迟或永久停顿，见[USB专题](USB_SWITCH_AUDIT.md)。

第五十批，2026-10-21，基线 `2d14d4009`；新增 ALIGN-172–173。范围为金属箱／强化木箱／可配置战利品箱共用实体、书架、瓶框及实际接收方。衣柜 ALIGN-145 不重复核报；本批没有修改通知或菜单逻辑，没有 Gradle、编译、新测试、游戏、重启、构建或推送。

本轮确认两项独立合同差异：**原箱子的相邻 GT 库存专用通知链没有完整接入当前发送方／接收方；箱子 GUI 部分 shift 转出还存在没有触发自身库存回调的具体入口。** 前者影响主动通知／再次检查的时机，不表示物品永远不传输；后者确认本次操作缺少对应标脏／通知，不表示已经运行重启证明丢物。

## 先区分通知的接收边界

| 调用 | 已证实接收者／作用 | 不能替代什么 |
| --- | --- | --- |
| ItemStackHandler.onContentsChanged | 所属 GT 实体覆写的回调；当前正常非模拟插入／提取、直接 setter 会触发 | 不能捕捉外部直接原位改 getStackInSlot 返回堆而不调用 handler 的所有情况 |
| BlockEntity.setChanged | 标记所在已加载区块未保存；非空气还进入原生输出信号邻接路径 | 不直接发送 GT adjacentInventoryUpdated，也不自动发送客户端实体包 |
| ServerLevel.sendBlockUpdated | 通知区块变化／客户端同步；碰撞几何变化时更新导航 | 直接调用即使传 3，也不等价于 Level.setBlock 的 flag 3 邻居分派 |
| updateNeighborsAt／普通 neighborChanged | 原生邻居更新器；GT 漏斗方块在普通 neighborChanged 中唤醒其实体 | 不等价于 GT 专用接口；当前 setChanged 使用的是另一个 onNeighborChange 扩展钩子 |
| GT adjacentInventoryUpdated | 直接给实现该接口的实体传相对面与来源位置 | 不等价于保存、客户端同步或普通方块邻居事件 |

## 原 GT6 的变更标记和真实消费

[Base05Inventories.java:102](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/tileentity/base/TileEntityBase05Inventories.java:102) 的 updateInventory／updateTanks 设 mInventoryChanged。108–114 行的 markDirty、decrStackSize、setInventorySlotContents 进入该标记；133–139 行 GUI 入口亦如此。不能把 slot／slotTake 这类原位辅助方法都当作会自行标记，它们由使用者另行 updateInventory。

[Base03TicksAndSync.java:111](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/tileentity/base/TileEntityBase03TicksAndSync.java:111) 在同一 tick 先 onTick，随后 onTickResetChecks；Base05:86–88 在后者清 mInventoryChanged。因此箱子能先消费这一轮的累计变更，再清标记。原 Base01:157 的 markDirty 本体有意为空，原 GUI 的 markDirty 通过 Base05 覆写仍会设库存标志；本报告不把旧 markDirty 直接套用成现代保存语义。

### 箱子实际发送，其他两类不能机械类推

- [MultiTileEntityChest.java:134](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/block/multitileentity/example/MultiTileEntityChest.java:134)：服务端发现 mInventoryChanged 时遍历六面，向相邻 ITileEntityAdjacentInventoryUpdatable 直接派发，传递邻居看到的反向面与自身 IInventory。这条不是仅声明接口或空的 helper。
- [MultiTileEntityBookShelf.java:154](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/tileentity/inventories/MultiTileEntityBookShelf.java:154)：库存标志用于计算陈列类型，仅显示类型改变时 updateClientData；红石按钮／拉杆另走 causeBlockUpdate。
- [MultiTileEntityBottleCrate.java:80](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/tileentity/inventories/MultiTileEntityBottleCrate.java:80)：库存标志用于重算九瓶陈列并 updateClientData。
- 书架／瓶框父 [Base06Covers.java:198](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/tileentity/base/TileEntityBase06Covers.java:198) 调用盖板 tickPre → onTick2 → tickPost，并传入库存标志；它不在这个主链替子类自动广播邻库。Base01:544 有 updateAdjacentInventories 工具方法，但不能仅因方法存在便认定两类都调用过。

因此**本轮相邻 GT 库存广播缺口只以原箱子的明确发送为依据，不把书架、瓶框都列为丢失相同广播**。它们当前正常显示同步是否存在另见下文。

### 原接口有实际接收者

| 原接收者 | 实际消费 |
| --- | --- |
| MultiTileEntityHopper.java:252 | 只接受上方或输出朝向面的通知；闲置 mCheck<0 时改为 0，促进下一次处理。164–170 行本来另有 SYNC_SECOND 兜底 |
| MultiTileEntityQueueHopper.java:234 | 同样从上方／输出面唤醒闲置检查；151–157 行也有 SYNC_SECOND |
| MultiTileEntityDustFunnel.java:261 | 垂直面通知转为 updateInventory；96–98 行在标志或每 100 tick 尝试传输 |
| MultiTileEntityMassStorage.java:551 | 下方通知转为 updateInventory，供自动下送链再次检查 |
| MultiTileEntityBasicMachine.java:1033 | 仅配置的物品输入／输出面通知置库存标志；800、873、895 行的配方／输出检查会消费 |
| MultiTileEntityExtender.java:130／MultiTileEntityBridge.java:114 | 把通知转交给实际委托目标；接口还有明确转发消费者，不只是名称保留 |

原物品管道 `MultiTileEntityPipeItem` 的搜索结果只有**发送**该通知，不能把 import 当作它实现了接收接口。该点与当前管道增加一个接收接口相互独立。

## ALIGN-172：箱子专用邻库广播及接收链未完整接入

维护源码的 Neo 分支被 Stonecutter 块注释遮蔽。本轮用既有 `check_relay_sharing.project` 先暴露维护体再选平台，在内存中计算 46 个投影视图与签名摘要，没有新增投影文件或编译。

### 两版可实际进入的能力和手动路径

| 宿主 | Forge 1.20.1 实际能力入口 | NeoForge 1.21.1 实际能力入口 |
| --- | --- | --- |
| 金属／强化木／战利品箱共用实体 | MetalChestBlockEntity.java:260–265 的 getCapability；无面返回库存，有面经 ComponentCoverAccess.items 包装 | MOD bus 上 MetalChestCapabilities.java:3–4 注册 ItemHandler.BLOCK，调用 componentCapability:316，返回同一库存或面包装 |
| 书架 | BookShelfBlockEntity.java:235–238 返回其库存 LazyOptional | MOD bus BookShelfCapabilities.java:3–4 注册并返回 be.inventory |
| 瓶框 | BottleCrateBlockEntity.java:165–169 返回 items 的 LazyOptional | MOD bus StorageContainerCapabilities.java:3–6 注册 BOTTLE_CRATE，未移除时返回 be.items |

这些 handler 的正常非模拟 insert／extract 和 setter 会回到宿主覆写的 onContentsChanged。Forge／Neo 依赖本体均已核：模拟探测不触发此回调，成功变更和直接 setStackInSlot 会触发，空／拒绝插入不能算实际库存改变。

- [MetalChestBlockEntity.java:125](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/inventory/MetalChestBlockEntity.java:125) 的 54 槽 handler 回调只调用 setChanged。正常自动化已经标脏并进入原生输出信号邻接，不是完全无通知。它没有在该回调向 GT 专用接口广播；MetalChestBlock:129–137 的服务端 ticker 只 tickComponentCovers，没有恢复原箱 onTick 的六面派发。
- [BookShelfBlockEntity.java:136](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/inventory/BookShelfBlockEntity.java:136) 先 setChanged，再发送客户端 block update，并更新原生输出信号。BookShelfBlock:205／212 的手动取放经过 handler setter／insert，同样回调。按钮／拉杆变化另经 changedRedstone:70–75 → updateNeighborsAt，不能把按钮的邻居事件算作所有书籍取放都会发生。
- [BottleCrateBlockEntity.java:26](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/inventory/BottleCrateBlockEntity.java:26) 回调先 setChanged，再 sendBlockUpdated；BottleCrateBlock:115／125 的手动取放用 setter，实体 insert／extractLast:72–81 用 handler。陈列同步已有明确调用，不以没有原 mDisplay 同名字段判缺失。

当前生产源码全文检索的 GT 专用接口实际实现者只有 [ItemPipeBlockEntity.java:53](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/ItemPipeBlockEntity.java:53)。Hopper／QueueHopper 虽 import 接口且能向周边发送，却未 implements／覆写接收；DustFunnel、MassStorage、BasicMachine 与 CapabilityRelay 也没有接这个接口。不能通过补箱子单侧广播就宣称恢复了原版整条消费链。

### setChanged 的原生边界已追到实际方法体

已读本地 Forge 47.4.20 mapped 官方名 jar 的 javap，以及 NeoForge 21.1.243 的源与 class 见证，版本分别对应 Minecraft 1.20.1／1.21.1。所有工件、class／source 条目哈希与十六组方法见证摘要放在[本批回执](verification/inventory-persistence-audit-20261021.json)，不以网上新版本文档替代本地版本。

两版 BlockEntity.setChanged 调 Level.blockEntityChanged，将已加载区块标记为未保存；非空气继续调 updateNeighbourForOutputSignal。Neo 源行 BlockEntity:191–201、Level:941–944／1063–1077，与两版实际字节码一致。

Loader 在 updateNeighbourForOutputSignal 中先对相邻 BlockState 调 **onNeighborChange** 扩展钩子；若相邻导体后的方块接受 getWeakChanges，才对后者走 ordinary neighborChanged。Forge IForgeBlock.java:464、Neo IBlockExtension.java:503 的默认 onNeighborChange 为空，两个 getWeakChanges 默认为 false，BlockState 扩展只是转发。

当前 GT HopperBlock.java:150–157／QueueHopperBlock.java:149–156 只覆写了 **neighborChanged**，然后调用实体 onNeighborChanged（分别 647／609 行）设 blockUpdated 并唤醒闲置 mCheck。它们没有覆写 Loader 的 onNeighborChange。因此箱子正常 setChanged 的直接邻接钩子**不能被当成当前 GT 漏斗的该主动唤醒入口**。也不能说所有外部模组都收不到，第三方可以实现自己的 onNeighborChange。

两版 ServerLevel.sendBlockUpdated 的本体调区块源 blockChanged，并在碰撞形状变化时重新寻路；Neo 源 1065–1095 行如此，Forge 方法体已读字节码。直接传参数 3 不会自动再次执行 Level.setBlock 中的邻居更新步骤。当前 BlockEntity 上两个通用 mixin 只有盖板／染色保存与能力等职责，未在 setChanged 或 onNeighborChange 中补 GT 专用派发；没有凭缺一个同名覆写就结束搜索。

## 接收方依然有轮询，不能推成永久卡死

| 当前对象 | 活 ticker／接收入口与重试边界 |
| --- | --- |
| GT 漏斗 | HopperBlock:125–133 注册 serverTick；实体 113–119 满足闲置／自库存标记／普通邻居标记／每 20 tick 之一时尝试，仍受正冷却和红石门控。onNeighborChanged:647–650 可提前唤醒 |
| GT 队列漏斗 | QueueHopperBlock:124–132 → serverTick；实体 111–117 同样每 20 tick 兜底、正冷却／红石门控；普通邻居回调 609 起可唤醒 |
| GT 物品管道 | ItemPipeBlock:158–160 → serverTick；实体 176–192 每 4 tick 对非空库存路由，tryPushToAdjacent:541–593 每轮重新查看实际目标能力并模拟／执行。adjacentInventoryUpdated:1226–1228 **只 setChanged**，没有重连或清缓存字段；注释的 wake up 不代表额外路由加速 |
| 筛粉漏斗 | DustFunnelBlock:59–61 → serverTick；实体 105–117 在自身 inventoryChanged 或每 100 tick 时查看上方 NativeItemInventories，再取符合输入的堆。没有原接口接收，所以单靠上方箱子改变不会设置其专用标志；100 tick 兜底仍在 |
| 大容量存储自动下送 | MassStorageBlockEntity:231–244 在自身标志或每 100 tick 重查下方实际能力；此处仅作为原接口接收者当前行为的边界，不重复审计其模式／旧问题 |
| 原生 MC 漏斗 | 两版实际 HopperBlockEntity.pushItemsTick 按 cooldown 每次尝试；suckInItems 先进入 Loader VanillaInventoryCodeHooks.extractHook，能力扫描有真实入口。Neo 源 95–125／215–219 与 Forge 相关 javap 已读。不是等待 GT 专用通知才工作 |

GT 漏斗／队列的 findInputSource、findOutputTarget 分别使用当前平台能力 API；Neo 走 level.getCapability，Forge 走实体 getCapability。筛粉漏斗的 NativeItemInventories.at 也按平台分支重新读取已加载邻格。当前管道是路由已有物品，不因其实现接收接口就等价于会主动抽取箱内物品。

因此本轮的可靠结论是：原先箱子变更促进相邻自动化尽快重检的**专用通知合同未恢复**；存在定时重试，最大等待还受冷却、红石、内容、方向和能力规则影响。没有复现实测时延，也没有断言此前料斗故障仍然存在，或通知缺失必然永久卡死。

## ALIGN-173：部分 shift 转出没有触发箱子自身库存回调

2026-10-21第五十一批续核：该项的同源范围现已扩展到箱子、保险箱、抽屉、普通／队列漏斗及末影垃圾桶六类；工作台储物页、电池盒、垃圾场有不同补偿／限制，不一并列错。Queue必须先通过原生mayPickup，只有合法末格且允许多件堆叠的部分转出进入173；非末格手动取出被拒另记179。以下保留本批最初箱子证据，完整范围与许可前置链见[共用菜单核查](SHARED_MENU_AUDIT.md)。

这是独立于上述“箱子已经回调之后没有广播”的路径：部分操作连箱子自己的 handler 回调也没有到达。

原 [ContainerCommon.java:498](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/gui/ContainerCommon.java:498) transferStackInSlot 在 502 行先 markDirtyGUI，519 行还对保留非空的源槽调用 onSlotChanged。ContainerCommonChest:39 起用 Slot_Normal，Slot_Base:97 把 onSlotChanged 送给库存 markDirtyGUI，Base05:139／108 由此置 mInventoryChanged。原箱子会在随后的 onTick 消费通知标志。

当前 [MetalChestBlockEntity.java:227](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/inventory/MetalChestBlockEntity.java:227) 的 ChestMenu 继承 HopperContainerMenu，231 行把真实库存传入。[HopperContainerMenu.java:55](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/client/gui/HopperContainerMenu.java:55) 为每格使用原生 SlotItemHandler；73–99 的 quickMoveStack 取源堆本体并交给 moveItemStackTo，若转移后仍非空，只调用源 slot.setChanged。

两版 SlotItemHandler 构造器把继承的 Slot.container 指向独立的空 SimpleContainer(0)，没有覆写 setChanged。继承 Slot.setChanged 仅调用这个空容器的 setChanged；空容器不会替 owner BlockEntity 标脏。原生 AbstractContainerMenu.moveItemStackTo 会直接改变源／已有目标 ItemStack 数量，目标是玩家背包时只会通知玩家槽。

**已确认两版共同的可达条件：从箱内 shift 转出，玩家背包只能接纳一部分，箱内源堆仍非空。** 这条路径不调用该箱 handler 的 insert／extract／setStackInSlot；没有 owner setChanged，故本次操作没有通过正常回调标记箱子区块或通知其输出信号。GUI 网络同步可能仍会发现堆变化，其他事件也可能随后标脏；本报告不等同“画面必定不变”或“保存一定丢失”。未实际重启存档。

**不能泛化成两版所有 shift 都不回调：**

- 源堆被全部搬空会走 slot.set(ItemStack.EMPTY)，回到 handler setter，正常触发回调。
- Neo 的 SlotItemHandler.getMaxStackSize(stack) 只取物品／槽上限；向箱内**已有堆**合并时的原位数量变化和 slot.setChanged 同样没有 handler 回调。
- Forge 47.4.20 的同方法会对可修改 handler 临时清槽、模拟并恢复原堆，调用两次 setter；它确实可能先触发 owner 回调，不能说这条合并在 Forge 也完全无通知。这个依赖实现差异已分别读源和 class，未因 API 同名便归一。
- 书架、瓶框没有同一 ChestMenu GUI，正常手动交互走已列出的 handler 方法，本轮不把这个 GUI 漏口套给两者。其他使用 HopperContainerMenu 的库存可另核，但不自动扩大为所有机器缺失。

## 已有对应、未确认边界与交付

已有：三类正常 handler 变更标脏、书架／瓶框显示同步；普通邻居事件可唤醒 GT 漏斗；GT／原生漏斗与管道均有定时工作入口。原书架／瓶框没有同样的邻库广播，不能误报。

未运行：实际一秒／五秒等待、比较器延迟、多客户端显示、受限背包 shift 的游戏复现、保存／卸载／重启、第三方能力消费者、附属桥接多跳链的全端到端行为、全部机器自动化。原 cover tick 的变更标志不在本轮扩成盖板行为新缺失；Locker 145 不重报。

[本批回执](verification/inventory-persistence-audit-20261021.json)合并本项二十二份原版、二十九份当前来源及其它两项核查的去重摘要，保存四十六份实际平台投影视图摘要与原生依赖条目／方法见证。原生方法体和反编译源留在忽略的 work/，未复制进生产或提交。原作者／snapshot 沿用 GregTech-6 Team 原树及既有项目来源。本轮没有恢复原专用通知或修复 GUI，只将确定差异及已有轮询分开归档，全面核查继续。
