# 长距离端点的危险分支、活动指示与源码整理

2026-10-21，第八十八批，基线 `5ad5fdc48`。继续 [长距离输电](LONG_DISTANCE_ENERGY_AUDIT.md) 与 [管道转发](PIPELINE_RELAY_AUDIT.md)，新增 ALIGN-287、288。本批没有修改玩法，只展开五份原生源码并明确状态观察局部名。全面记录共 288 条，包含已修、部分修复及保留政策，不是未修复问题数。

原只读来源为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，作者 GregTech-6 Team／Gregorius Techneticies，版权与 LGPL 头保留。10 份原版、23 份当前来源、32 份共享平台视图、25 份 JSON 的身份，以及有限源码投影见[回执](verification/long-distance-lifecycle-audit-20261021.json)。这些数量不代表整套传输系统的运行验收。

## ALIGN-287：超出导线耐压时缺少原版邻格点火尝试

原 `MultiTileEntityLongDistanceTransformer.doInject` 在有效线路上遇到 `aSize > mThroughput`，且为实际注入时调用 `scanWires(true)`。扫描到每段同身份导线后，先执行 `WD.burn(world, coords, true, false)`，再显式将这段导线设为火。

`WD.burn` 的两个参数不能按名称猜测。这里 true 选择 `CS.ALL_SIDES_MIDDLE_UP={6,1,2,3,4,5,0}`：中心及六个邻格都交给 `WD.fire`。false 则进入不要求附近可燃物的分支，但 `fire` 仍排除已有火／熔岩，并检查无碰撞或地毯、GT 特殊物品及可选节点保护。因此这是七个位置的尝试，另加导线中心的显式设火，不是保证七格都出现或持续燃烧。

当前 `LongDistanceTransformerBlockEntity.doEnergyInjection` 在同类分支先 `rescan()`，随后只对 `wires` 中的位置执行 `setBlockAndUpdate(...FIRE...)`，没有邻格尝试消费者。该差异独立于发送／接收端过压的 `MachineOverload` 配置；本轮没有恢复或扩大火灾影响。

有限例子使用目录中的 8192 EU 额定端点和耐压 4096 EU 的锡长线：5000 EU 单包处于端点 4096–16384 输入范围，却超过导线耐压。假定已有有效加载线路、实际注入一包，它进入的是烧线分支。源码坐标投影只证明原版另尝试六个邻格；没有执行世界点火、传播、区域保护或破坏范围测试。

两边搜索都能在找到匹配接收端时提前终止。完整分叉线路的燃烧集合、原 HashSet 与现代队列的并列顺序、先扫描后设火与边扫描边设火的更新影响仍待核查，不能据 `WD.burn` 推断原版必定烧完整张网络。

## ALIGN-288：活动指示的“目标不可用”门槛被简化

原 `onTickCheck` 在停机时显示 0。进入状态 3 还要求当前有 `mTarget` 对象、该对象的发送者资格满足原条件、已记住目标坐标，并且目标失效或区块不可用；只有保存坐标而没有目标对象时，第一项就为假，继续普通活动历史分支。

当前 `tickActivity` 则以 `link.known() && link.targetOwner()==null && targetPosition!=self` 判断 unavailable。新实体加载后，`link.restore` 恢复坐标并清除对象引用。在真正注入／查询调用 `receiver()` 之前，即使目标区块已加载，这个式子也会成立，显示状态 3 并保持历史不移位。

具体源条件是：两端未停机、保存的目标不是自身、历史值为 0、当 tick 没有活动，发送端刚恢复坐标而尚未绑定对象。原版为普通空闲状态 0；当前为 3。这里只确认显示／历史更新资格不同，不表示线路已经失效，也没有运行实际存档重启场景。

八种明确目标状态、四个 64 位历史样本、active／stopped 两布尔量共 128 个有限条件中，24 个状态／历史投影不同。已绑定且可用、停止门控、自身失败哨兵及普通历史移位仍有对应。原版本来就保存 `mActiveData`，当前保存 history 不能再被误报为新增的重启行为。

五个变压器的 blockstate 均声明六方向×四活动值，共 120 个引用；对应 20 份模型文档存在，状态 3 使用 `overlay_unloaded` 资源。此项证明状态值有模型消费者，不代表纹理烘焙、Neo composite 适配或画面已经验收。

## 已有实现与旧报告当前状态

共享 `GTLongDistance` 工厂由 Forge `GTMiscBlocks`、Neo `GregTechNeoForge` 的实际入口调用。两个管道端点使用独立 `LONG_DIST_ENDPOINT` 类型，Neo 物品／流体能力回调有注册；管道按 ITEM_PIPE／FLUID_PIPE 分开，线路身份保留原 meta，不把同耐压的不同材料自动连成一种。

端点操作中已有权限检查、六向扳手、软锤重扫和放大镜详情入口；实际调用的工具返回值仍为软锤 10000、放大镜 1。接收端被有效发送者占用时，软锤重扫仍被 `link.receiving()` 阻止。物品／流体最终目标位于接收端背后，能力查询面朝向接收端；流体两种 drain 仍明确拒绝，没有本地流体缓存或额外吞吐计数。完整外部容器、工具事件和循环转发仍需运行验收。

| 既有记录 | 当前源码状态 |
| --- | --- |
| ALIGN-070 | 记住目标后未恢复 watched 的路径缺口仍在，两类管道端点同样受影响；不重复造编号。 |
| ALIGN-071 | 公开附属 `EnergyBlock → AddonEnergyAdapter` 已接入；仅提供 FE 能力的实体仍不由 `EnergyBlockViews.view` 接收，保持部分修复。 |
| ALIGN-072 | 两端过压均已调用共用 `MachineOverload`，无条件删块差异保持已修，原定向运行证据仍只适用于其日期／场景。 |
| ALIGN-073 | 共用物品转发器仍允许原版侧面合同禁止的抽取；流体拒绝抽取是另一条已有合同。 |

普通路线损耗、单一接收端认领、无任意 4096 格上限等既有结论保留各自证据边界。缓存与前向目标查询的加载保护，不代表完整搜索绝不加载区块。原流体端点温度保存键不一致也不恢复；现代同键保存属于已有改善，路径有效性需另补。

## 源码标准化与验证边界

整理 `LongDistEndpointBlockEntity`、`LongDistanceTransformerBlockEntity`、两类端点方块及 `LongDistPipeBlock` 共五份源码。维护输入合计 520→980 行，拆开构造、缓存观察、重扫、原生回调、工具、保存与恢复方法。两份实体的 `changed` 方法将 e／current／previous 明确为 observedEntry／currentState／previousState，并补充准确的类职责说明，完整原版权头不动。

十份完整执行投影分别为端点实体 1658／1686、变压器实体 2476／2502、管道端点方块 1280／1439、管线方块 532／532、变压器方块 1426／1585 项。在仅恢复所列方法局部名后，所有有序执行词法保持；未归一的字面量原始位置、其他注释和 Stonecutter 条件分别核对。没有更改 import、字段、公开方法、注册 ID、配方、NBT、危险或同步策略。

两份材料表摘要保持。未 Java 编译、目标初始化、Gradle、测试、游戏、实际重启／客户端、构建或推送；有限条件由源码推导，不冒充运行目标类。其余长距离边界、存储中继与全部系统队列继续。
