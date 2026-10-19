# 普通物品管道核查

日期：2026-10-19，基线 `c1e513f77`。原版根目录：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。本轮沿注册、库存父类、网络扫描和两版能力入口对照；不包含完整检索覆盖板、流体管道或实际世界网络验收。

主要原版依据为 `gregtech/loaders/b/Loader_MultiTileEntities.java:1823–1843`、`gregapi/tileentity/connectors/MultiTileEntityPipeItem.java`、同目录 `ITileEntityItemPipe.java`、`gregapi/tileentity/base/TileEntityBase05Inventories.java` 和 `TileEntityBase06Covers.java`。原始 GregTech-6 Team／Gregorius Techneticies、LGPL 声明保持不变；本轮只读原版。现有路由和交付算法分别有 brokestar／masson 的适配来源说明，本轮未将其冒称原版算法。

## 对应的目录与现有能力

21 个材料家族乘六种规格，共 126 项：三种尺寸和相应限制型。材料通过旧符号门面对应，stepSize、invSize、直径、restrictive、blocking 与原版注册参数相同。普通三种步长为 base／base/2／base/4，限制型为 base*100／base*50／base*25；现有参数均能整除，当前先除后乘在这些参数上等价。这里的步长是路由代价，不是传输时间，已纠正“慢 25–100 倍”的注释。

共享 ItemPipeDefinitions 展开目录；Forge MachineBlockBootstrap 和 Neo GTItemPipes.register 都调用它。两版保留各自 Item 构造器，没有为了格式统一偷偷改变物品类型。原版物品堆叠上限 64／32／16，当前两个 GTItemPipes 分支相同；这是物品形式堆叠，不是管道内部库存容量。

当前已有按权重寻找可接受出口、邻区块加载检查、过滤／快门和检索覆盖板入口、刷新能力后交付、仅扣除已确认实际交付量等路径。14 条现有 core 合同覆盖较低代价、等价路径次序、扫描限制、零步长环路、溢出饱和、部分接受与异常返回。它们证明当前纯逻辑回归行为，不能证明原版网络运行等价。

## ALIGN-079：库存槽数被解释为单槽数量

原版 addItemPipes 写入 NBT_INV_SIZE；父类 TileEntityBase05Inventories.getDefaultInventory 以它创建 ItemStack[]，getInventoryStackLimit 返回 64。普通黄铜中型管道为一个槽，大型两个、巨型四个；高档管道有更多槽。

当前 ItemPipeBlockEntity 构造器固定 `NonNullList.withSize(1, ItemStack.EMPTY)`，getSlotLimit 却返回 spec.invSize()。因此中型黄铜只有一个物品容量，大型黄铜仍只有一个槽、容量二；无法同时缓存两种物品。最高目录参数 2048 被解释成单堆数量，还衍生了 Neo NBT 的 gt.pipe_count 特殊处理。

此外，insertItem 空槽时没有用待插入物品的 getMaxStackSize 限制数量，合并已有堆时才检查；不能把当前大堆存档成功当作原版库存正确。修复涉及库存结构和现有保存迁移，故本轮记录而不直接替换。

## ALIGN-080：吞吐计数单位和限制缺失

原版 mTransferredItems 在每个 SERVER_TIME%20==0 清零；每次成功 ST.move 后 incrementTransferCounter(1)，即成功移动一堆（可能是部分堆）计一次。pipeCapacityCheck 用库存槽数作为每秒次数上限，扫描及出口发送均执行容量检查。原版循环增加的是按代价枚举到当前出口为止的 tPipeList，不应简化为现代意义的“只计实际最短路径”。

当前 transferredThisSecond 按 accepted 物品数量累加，按实体自己的二十 tick 倒计时重置。检索全部读写后，计数只进入保存、读取和调试显示，没有参与 tickServer、findBestExit、pushItemsToPipe 或 tryPushToAdjacent 的准入。当前容量大小限制缓存，不等于原版吞吐限制。

在持续补货且出口接收的条件下，中型黄铜当前可每四 tick 输出一个物品；原版对应一次允许移动至多一正常堆、每秒容量一次。此为源码条件推导，未实际运行吞吐场景，不将两者粗略换算成普遍固定倍数。

## ALIGN-081：逐段转交替代原版源库存直达出口

原版 onServerTickPre 的第二阶段每十 tick、入口方向稳定后扫描网络，按代价排序各管道；候选出口 sendItemStack(this) 将源实体作为 aSender，ST.move 从源库存直接向出口邻居移动。中间管道用于可达、代价和容量，不要求物品先进入每段管道的缓存；成功后还能继续循环。

当前每四世界 tick 先试邻接库存，否则 findBestExit 只返回 firstHop，pushItemsToPipe 把物品放进下一段管道库存。下一段依赖自己的 tick 再输送；延时、堵塞位置与输入锁定受中间库存及实体调度顺序影响。不能简单说一定“每段四 tick”，因为同一 tick 的实体执行顺序可能继续转发。

当前 WeightedItemPipeRoutes 是非负步长的有界最短代价搜索，最多访问 32768 个节点，等价路径保留第一跳顺序。原版递归 scanPipes 没有这个显式上限，按 HashMap 结果排序。保护性扫描上限、稳定顺序和递归改迭代不在本轮撤销；它们与端到端语义需要分别评估。

## ALIGN-082：外部侧面能力遗漏连接与入口锁定检查

原版 canInsertItem2 要求该面 connected、未禁入；库存为空才设 mLastReceivedFrom，随后只允许同面且空槽插入。canExtractItem2 对有效面要求 connected；disabledOutputs 限制主动出口，不是这条外部抽取许可。

当前 capabilityHandler/getCapability 为任意有效方向返回 SideAwareItemHandler。该包装插入只查 disabledInputs，再调用无方向 insertItem，成功后直接覆盖 lastReceivedFrom；没有检查方块连接和此前的入口方向。抽取只查 disabledOutputs，也没有连接检查。已有物品允许合并到同一槽。内部路由虽调用 canAcceptFrom 并检查连接，不能保护外部模组直接调用侧面能力的路径。

FaceItemHandler 增加覆盖板筛选后仍委托上述包装，不会补齐基础连接和入口合同。此处没有声称所有漏斗一定触发某个症状；实际各模组查询能力的方向仍需运行场景确认。

## 其他确认与后续边界

- 旧类说明宣称允许在管道相邻面装物品过滤器；实际 attachCover 已拒绝并断开该面，CoverConnections 也参与自动连接。删除这段与实现相反的历史说明，不将它误报为未修复问题。
- 物品管道同样没有 ProgressSensorSource／MachineControl.Provider；原版 getProgressValue/getProgressMax 为传输堆数和容量各乘 64。这是 ALIGN-077 所述接口缺口的另一处消费链，独立功率计不相关。
- 原版禁止在两个物品管道之间使用活动扳手修改输入／输出限制；当前 toggleSide 本身没这个判断。需继续完整检查交互调用方后决定登记范围。
- 原版出口对朝向管道的漏斗／发射器有特别拒绝条件；当前出口按能力访问。自动连接、特殊库存、输出通知、拆除掉落、泡沫／外观及跨区块重载仍待完整核查。

## 代码整理与验证范围

将 ItemPipeBlockEntity 的大段过时设计说明替换为当前职责和明确的实现边界。展开 GTItemPipes 的 Neo 注册实现，step／inv 改为 stepSize／inventorySize，误导性 routing 形参在两版改为 blocking；它实际传给 ItemPipeSpec.blocking，签名类型、注册 ID、次序和数值不变。ItemPipeSpec 只修正步长注释。

源码等价检查遇到了此前工具未支持的 Stonecutter 嵌套注释：`/^* ... ^/` 中的英文撇号被当成 Java 字符字面量，导致条件块无法闭合。check_relay_sharing.project 现在在展开 Neo 包装时恢复嵌套注释；增加两个定向回归，保留共享普通注释。三份生产源码分别投影两版，归一化声明的形参改名后执行代码和字符串一致。

Java 17 编译当前五个相关 core 生产类、CoreBehaviorContracts 和 ItemPipeAudit，仅执行原有 itemPipeRoutingAndDelivery 的 14 条合同，输出全部 126 个目录条目。其他依赖使用已有 core/API/test 编译输出。没有原生编译、世界模拟、整套测试、Gradle 构建或推送。原生差异均为源代码证据；见 [回执](verification/item-pipe-audit-20261019.json)。
