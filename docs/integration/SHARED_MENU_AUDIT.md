# 共用库存菜单回调与 GUI 权限核查

第五十一批，2026-10-21。使用者盘点基线为 `2d14d400914e0e2567d20dd0c546efdf43cb65c1`，权限补查基线为 `396a9ee55168c9297b2db76a5eda0e73a73b0d43`。其间及本批对 `HopperContainerMenu`、`InventorySlotLayout` 的整理仅改排版／说明，执行代码与字面量投影保持。本报告扩展既有 ALIGN-173，并新增独立的 ALIGN-179；没有按材质、菜单大小或每个宿主重复编号。

原版根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。本地原生证据对应 Minecraft 1.20.1／Forge 47.4.20 和 Minecraft 1.21.1／NeoForge 21.1.243，来源、工件／条目哈希、投影及方法见证摘要归入[统一回执](C:/Dev/GregTech6/docs/integration/verification/bush-menu-audit-20261021.json)。原生方法体、反编译源只留在忽略的 work 目录，没有复制到本报告或生产代码。

## ALIGN-173：共用菜单的原位修改绕过宿主回调

前一批已确认箱子部分 shift 转出不触发自身库存通知，见[库存通知核查](C:/Dev/GregTech6/docs/integration/INVENTORY_NOTIFICATION_AUDIT.md)。本批沿同一菜单盘点全部生产使用者，将同源范围扩展到六类实际宿主，而不是再为每类增加一项。

### 原合同与当前漏口

原 `ContainerCommon.transferStackInSlot:498–519` 在处理前调用库存 `markDirtyGUI`，源堆未空时还调用槽的 `onSlotChanged`；`Slot_Base:97` 将其送到库存，`Base05Inventories:108,139` 设置 `mInventoryChanged`。这里证明的是原库存变化合同，不将旧 `markDirty` 直接解释为现代强制磁盘保存。

当前 [HopperContainerMenu.java:61](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/client/gui/HopperContainerMenu.java:61) 直接创建原生 `SlotItemHandler`；`quickMoveStack:81` 取得真实堆并交给原生 `moveItemStackTo`。成功转出后源堆仍非空时，只调用源槽 `setChanged`。

两版 `SlotItemHandler` 继承的 `container` 都是独立的空 `SimpleContainer(0)`，没有覆写 `setChanged`。继承槽的通知只到这个空容器，不会替真实 handler 或宿主实体设置变化标志。原位改动堆数量又没有调用实际 handler 的插入、提取或 setter，因此绕过宿主正常回调。

**两版共同的确认条件是：玩家操作已通过原生槽许可，目标背包只能接纳源堆的一部分，转出后源堆仍非空。** 全部搬空会走槽 setter，正常回到 handler；不能说所有 shift 操作都漏回调。GUI 网络同步也可能发现堆变化，其他动作可能随后标脏；本项不等于已经实测画面不更新或重启丢物。

Neo 还有向宿主已有堆 shift 合并的同源入口：目标计数原位变化、槽 `setChanged` 同样未通知实际库存。Forge 47.4.20 的 `getMaxStackSize(ItemStack)` 会对可修改 handler 临时清槽并恢复，触发 setter 副作用；不能把 Neo 的“完全没有宿主回调”描述照搬给 Forge 的这一路合并。

### 六类实际宿主及有限补偿

| 宿主族 | 活菜单绑定及变化来源 | 确认范围／补偿边界 |
| --- | --- | --- |
| 金属箱、强化木箱、可配置战利品箱 | `MetalChestBlock:206` 打开实体；`MetalChestBlockEntity.createMenu:222` → 内部 `ChestMenu:227`，传真实54槽 handler。`onContentsChanged:127` 负责宿主 `setChanged` | 菜单没有补 shift／broadcast 通知；源堆部分转出会绕过该回调。盖板 ticker 不是每次库存计数变化的保证补偿 |
| 机械／钥匙保险箱 | `SafeBlock:126–127` → `SafeBlockEntity.createMenu:101–119`，用 `InvWrapper` 包装15槽 `SimpleContainer`；正常容器 listener:28 才通知宿主 | wrapper 的读取返回实际堆，原位修改不调用其 setter／容器通知。匿名菜单只补有效性检查；打开时的一次 `setChanged:107` 不能当作后续每次操作均已通知 |
| 四格抽屉 | `DrawerQuadBlock:106–107` 选页 → `DrawerQuadBlockEntity.createMenu:78–87`；每页36槽的 view:65–77 映射实际 handler | 回调:40–45 负责旧溢出回填、标脏和输出信号。原位变化绕过它们；匿名菜单只补 `stillValid`，没有固定库存 tick 补偿 |
| 普通 GT 漏斗 | `HopperBlock:225` → `HopperBlockEntity.createMenu:734–735`，把自身可修改 handler 传入 | 正常 insert／extract／setter:434–503 会标脏并设自身库存变化标志；部分 shift 不经过它们。每20 tick 重试运输不等于每20 tick 无条件标脏，实际移动可能另行补通知 |
| 队列 GT 漏斗 | `QueueHopperBlock:172,221` → `QueueHopperBlockEntity.createMenu:695–696`，同样传自身 handler | **只限已通过原生 `mayPickup` 的合法末格源堆**，且当前模式允许多件堆叠、玩家只接纳部分、源堆仍非空。非末格正常玩家操作会先被 ALIGN-179 的门控拒绝，不能算这一路已发生修改。运输补偿仍有条件 |
| 末影垃圾桶 | `EnderGarbageBlock:101` → `EnderGarbageBlockEntity.createMenu:78–79`，传原始九槽 handler；回调:44 标脏 | 每100 tick 且无红石时倾倒，逐槽 setter 清空剩余堆，可在后来补标脏；红石暂停时不发生。这是操作回调漏口，不是垃圾桶永远不保存 |

这里的 Queue 限定也修正了旧工作稿可能引起的歧义：不能把“直接调用父 `quickMoveStack` 的方法体没有许可判断”读成玩家能从任意 Queue 源格 shift 取出。只有先通过真实点击入口许可的操作，才继续讨论修改后的回调。

### 三类不应一并列错的使用者

| 宿主族 | 已有实现／补偿 |
| --- | --- |
| 高级／充能工作台储物页 | `AdvancedCraftingTableBlock:59–60` → `storageMenu:232–238`，充能版继承同一页。匿名菜单在 `clicked`、`quickMoveStack` 后调用 `inventoryChanged`，`broadcastChanges` 也标宿主脏；36槽储物页已有明确补偿。用于配方计算的另一个临时 `AbstractContainerMenu` 不属于此页 |
| 电池盒 | `EnergyNodeBlock:266` → `EnergyNodeBlockEntity.batteryMenu:382–386`。正常每槽上限1，insert／setter 限制为单件，典型“搬走一部分仍有余堆”条件不成立；电池盒 `serverTick:405–413` 还每 tick 标脏。旧非法超量堆不作为正常槽合同的反例 |
| 末影垃圾场 | `EnderGarbageDumpBlock:89` → 实体 `createMenu:120–121` → `GarbageDumpContainerMenu`。后者覆写 shift:36–47，拒绝玩家侧输入，输出实际调用 handler 提取。视图显示的是副本，真正移除由 `GarbageData.extractItem:121–130` 扣减并标记全局 SavedData 脏；没有用父类原位 shift 代替实际提取 |

垃圾桶和垃圾场是两种宿主，不能把前者的原始九槽 GUI、后者的只出不进全局视图混为同一权限／保存合同。书架与瓶框没有使用这份 GUI 菜单，不扩入 ALIGN-173。

## ALIGN-179：队列漏斗手动 GUI 误用自动化提取规则

### 原版允许手动整理合法 GUI 槽

原 `MultiTileEntityQueueHopper:227–229` 对侧面自动化只开放首／末格：首格插入、末格提取并服从输出面锁。手动 GUI 是另一条路径：

- Queue:102 允许玩家打开，281–282 绑定 `ContainerCommonDefault`；后者的默认槽目录为全部 GUI 槽，没有为 Queue 单独限制首／末格。
- `Slot_Base:91` 询问 `isItemValidForSlotGUI`；`Base05:143` 继续调用 `isItemValidForSlot`，而 **Base05:116 明确返回 true**。Queue 及 Base06→Base09 继承链没有覆写这组许可。
- `Slot_Base:92` 询问 `canTakeOutOfSlotGUI`，Base05:144 明确返回 true；其余非创造玩家不可取 debug 物品等槽基础保护仍在。本项的“任意合法槽可取”以玩家有使用权、普通合法物品为前提。
- 实际移除走 `decrStackSizeGUI`，不是侧面的 `canExtractItem2`；原普通点击和 shift 均消费 GUI 许可。

因此原手动整理不能按自动化 FIFO 首入末出规则重写，也不能把原本允许的非末格手工取物描述成绕过 FIFO 的漏洞。

### 当前普通点击和 QUICK_MOVE 都受末格限制

当前 [QueueHopperBlockEntity.java:695](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/QueueHopperBlockEntity.java:695) 直接把自身 handler 传给通用菜单，没有手动 GUI 专用提取视图。其 `extractItem:457–461` 对非末格返回空；两版原生 `SlotItemHandler.mayPickup` 都通过模拟提取判断能否取出，于是非末格已有普通物品也被拒绝。

**真实原生总入口会先检查许可：** `AbstractContainerMenu.doClick` 的 QUICK_MOVE 分支先调用源槽 `mayPickup`，失败就返回，通过后才调用 `quickMoveStack`。Neo 1.21.1 源379–389以及两版实际 class 的调用顺序均已核；普通 PICKUP 对已有物品同样先检查 `mayPickup` 再实际移除。

所以当前普通点击、正常 shift 均受末格限制，不能仅根据父菜单子方法缺少判断，推断正常 shift 能从任意格取出。程序直接调用该子方法、第三方菜单或注入数据包不属于本轮已验证的玩家入口。

插入又有另一层区别：Queue `insertItem` 自动化只给首格，但 GUI slot 的 `mayPlace` 询问的是 `isItemValid`，当前后者对各格为 true，手动空槽 setter 也允许任意有效索引。因此手动放入非首格不能直接列为自动化 FIFO 漏洞；确认差异是**原本允许的非末格手动取出，被自动化提取规则过度限制**。

该门控发生在操作之前；ALIGN-173 发生在许可已通过且堆已被原位修改之后。两者是不同阶段，既不互相替代，也不按每个 Queue 材料重复登记。

## 未接线的扩展方法不等于全部 GUI 权限缺失

生产类型检索中，父菜单 `canInsertIntoSlot`／`canExtractFromSlot` 只有两个声明，无调用和覆写。槽工厂没有消费这两个钩子，它们当前都返回 true；本批只纠正说明，没有把它们接成新的玩法规则。

真正仍在工作的门控包括：原生 `SlotItemHandler.mayPlace` 询问 handler 物品验证，`mayPickup` 模拟提取，实际 remove 仍调 handler；保险箱仍有开锁／所有者／距离及 `stillValid`；抽屉仍受指定页面索引视图约束；电池盒仍验证能源物品与单件上限。不能把休眠钩子单独扩大成所有菜单许可失效。

末影垃圾桶 GUI 传原始库存，而自动化能力另用 insertOnly 视图；原垃圾桶也允许 GUI 手取但禁止自动化抽出，两份合同分离已有对应。垃圾场则由只出不进视图和专用 shift 实现约束，不依赖这两个父钩子。没有核完原垃圾场全部 GUI 对应关系，不据其侧面规则直接宣布所有 GUI 行为已对齐。

## 真实注册、来源与验证边界

两版实际菜单／实体绑定已沿活入口核对。Forge 的机器 bootstrap 加载 GTMachines、GTStorage、GTMiscBlocks 和能源节点；Queue 经 `MachineRegistry.registerQueueHopper` 创建真实方块。Neo 经 `HopperRegistries`、GTSafes、GTStorageContainers、GTCraftingTables 和 GTEnergyNodes 注册对应宿主；共用 GTBlockEntities 的分支绑定相应实体列表。GTMenuTypes 两版登记支持大小与垃圾场专用类型，Forge／Neo 客户端也登记对应 HopperScreen。客户端 dummy 构造器不是额外的持久化宿主。

使用者盘点扫描1,738个生产 Java，12个文件直接引用通用菜单，再反查间接垃圾场宿主，共9类服务端宿主；不因客户端注册或同一菜单多种大小增加计数。权限扫描只找到两个休眠方法声明。工作稿的来源数量分别为4份原版／53份当前／58个投影，以及14份原版／29份当前／38个投影；统一回执按实际文件去重，并保留各自基线与本批纯排版边界。原 snapshot／作者证据沿用原树。

没有运行实际点击、shift、拖拽、快捷栏换位、比较器延迟、多客户端、自动保存、卸载重载、旧档重启或数据包测试。宿主正常 handler setter、后来运输或定时倾倒会产生通知；源码确认的单次漏回调不扩大成所有场景都会丢物。这里完成的是确定源链与范围归档，没有修复玩法，也不以静态证据宣布全部库存／GUI 已验收。
