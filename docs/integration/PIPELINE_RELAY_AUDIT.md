# 长距离管道能力与转发器去重核查

日期：2026-10-19；基线 `c07466737`。接续长距离线路缓存核查，本批检查物品／流体的实际能力转发，并把重复的平台业务方法归并。没有修改传输权限或配方。

## ALIGN-073：长距离物品管道允许自动化抽取，与原版侧面合同不同

原版 `gregtech/tileentity/inventories/MultiTileEntityLongDistancePipelineItem` 将库存查看、槽位数、直接 decrStackSize、直接写槽等委托到远端背面的相邻库存；getAccessibleSlotsFromSide2／canInsertItem2 又将插入权限委托到该库存朝向接收端的一面。**canExtractItem2 无条件返回 false**。

父类 `TileEntityBase06Covers.canExtractItem` 即使遇到覆盖板 override，也仍与 canExtractItem2 做 AND。因此符合侧面库存合同的自动化不能通过发送端抽取远端物品。直接 decrStackSize 能执行，不等于 canExtractItem 允许自动化调用。

当前 `LongDistEndpointBlockEntity` 继承 CapabilityRelayBlockEntity，物品端点启用 supportsItems，未覆盖 permitsItem 的默认 true。ItemRelay.extractItem 仅检查槽位范围与 permitsItem，随后直接调用远端 IItemHandler.extractItem。Forge LazyOptional 与 NeoForge 注册回调暴露的都是这一转发器，因此两版都有该差异。

结果示例：远端箱子中有三个钻石，当前发送端 capability 可以抽走两个；原版经侧面库存检查时会被拒绝。此为源链确认，本轮没有执行原生 Minecraft 测试。以后修复应考虑专门的物品抽取权限，不能直接关闭所有 CapabilityRelayBlockEntity 子类的抽取功能，因为过滤器等也复用它。

## 自检归因错误已经修正，玩法差异仍保留

`LongDistanceRuntimeSmoke` 原来断言抽取两个钻石成功，并将失败消息写成 “differs from original inventory delegate”。该断言忽略原版 canExtractItem2，只验证了移植版当前行为。

本轮保留现有断言与执行过程，仅把注释／失败消息明确标记为当前行为回归检查，引用 ALIGN-073。没有为了让测试变绿而宣布原版已对齐，也没有只修改测试期望、让未修的生产逻辑与测试矛盾。运行时测试本轮未执行。

## 已核对的流体和侧面路径

原版流体端点 fill／canFill 在 checkTarget 后比较 `FL.temperature <= mTemperature`，目标为接收端背面相邻罐，使用该罐朝向接收端的一面；drain 两个重载返回 null，canDrain=false。当前 target 返回同样的相邻位置与面，supportsFluidExtraction=false；因此共用 FluidRelay 的两个 drain 分支都不会调用远端抽取。

当前 permitsFluid 先拒空栈，GT 流体使用注册条目的温度，外部流体使用 FluidType.getTemperature(stack)，包含等于管道上限的边界。原版 FL 对 steam 有 373 K 特判，并读取实际 FluidStack 的温度；完整外部动态温度行为及温度目录仍不能仅凭这条比较符号判定完全一致。

当前四类流体线阈值为不锈钢 1943、钨 3695、Adamantium 5225、Draconium 4500 K；物品线为 -1。原版 BlockLongDistPipe 的五个 metadata 分别为 -1 和上述四种材料的 mMeltingPoint；本批观察了当前数值和原版材料来源，没有重新独立计算所有原版合金熔点，故不把该观察写成完整数值等价证明。

输入端 target(side) 不按调用面限制路由；所有非 null 面取得同一远端面接口。原版物品／流体端点的相关转发也没有把调用端限定为正面，不应擅自增加正面输入要求。现代 null-side capability 默认不暴露；它与旧 ForgeDirection.UNKNOWN／直接库存 API 的差异还需按现代调用者评估。

转发器每次操作重新取得目标和能力，复制只读栈／流体视图，传递 simulate／FluidAction，保留槽位边界与失败返回值。它没有本地库存或流体缓存，也没有额外限速计数。实际容量、插入和抽取数量仍由目标处理器决定。

## 重载与循环的边界

ALIGN-070 的 watched 缺失仍存在，本轮没有借去重顺带修复。原版流体端点还有一个来源细节：writeToNBT2 将 mTemperature 写到 NBT_THROUGHPUT，readFromNBT2 却读取 NBT_TEMPERATURE，可能使新实体恢复时温度为零并触发重新扫描；当前正确使用同一个 gt.temperature 键。不能为了“对齐”重新引入原版键不一致；应补齐当前路径失效机制。

CapabilityRelayBlockEntity 的 ACTIVE 集合跨物品／流体转发共享，进入同一个实体两次时拒绝嵌套操作，finally 移除当前实体，最外层退出清理 ThreadLocal。局部观察证明集合拒绝重复访问、保留外层访问、线程隔离与 remove 后重建；没有冒充复杂世界中的循环库存测试。

目标相邻库存未加载时 forward 返回不可用。但调用 target(side) 的子类可能先扫描线路并读取其他区块，因此已修正基类原先笼统的“without chunk loading”注释；不能据此宣称整条操作绝不加载区块。

## 代码去重及验证

原先 ItemRelay 六个方法和 FluidRelay 七个方法各有 Forge／NeoForge 两份，逻辑相同、只有能力常量不同。现为 13 个共享方法；两个类型明确的 forwardItems／forwardFluids 包装方法只选择平台能力常量，仍进入原有 forward。能力注册、缓存类型、失效处理和原生能力查询继续保留在平台边界。

RelayVisitSet 和 NeoForge LongDistanceCapabilities 的压行代码也已展开。没有更改 ID、NBT、流体温度、物品抽取许可、模拟参数或异常清理顺序。

新增只读检查 `tools/integration/check_relay_sharing.py --baseline c07466737`：先展开维护源码中 Stonecutter 注释包装的 NeoForge 分支，再分别投影两版；核对包装函数确实只是转发，并内联后比较完整代码和字面量。两版各六项物品／七项流体方法一致。直接把维护源码交给只清理已生成分支的工具会漏看注释里的 NeoForge 代码，本检查显式处理这一点。

Java 17 只编译核心目录、访问集合和观察入口；10 行观察已保存。未运行 Gradle、原生编译、完整客户端、自检场景、第三方自动化或世界重启。源等价验证不等于原生编译通过。详见 [回执](verification/pipeline-relay-audit-20261019.json)。

继续核查管道失效／燃线、轴与可调齿轮箱、其它物流和全面队列；本批不是整个项目审计完成。
