# 2026-10-15 / 信号线 addon 接口

分支 `addon/signal-wire-api`，从 `origin/main` 的 `9b244838a` 切开，不叠在材料 id / 动态配方分支上。`GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver` 在 core，不引用 Minecraft 类型。`UNIT` 仍是 `Integer.MAX_VALUE`，两个平台的 `SignalWireBlockEntity.UNIT` 指向它。`side` 按 `Direction.ordinal()`，是从线指向邻居的方向。线先把 `gtSignal` 夹到 `[0, 15 * UNIT]`，再减原来的损耗。`GtSignalReceiver` 和空的 `gregtech:signal_wire_receivers` 优先于信号源；原版发射器、投掷器、漏斗、活塞、TNT、音符盒、活板门、门、动力铁轨和红石灯仍走 `instanceof`。存档只写 `mode`，读档时 `signal` 清零后重算。

探针方块只注册在 gametest 源集。成品标签是空列表。测试在运行时把 `gregtech:signal_probe_tagged` 挂进这个标签；不能再放一份同路径的测试数据包，打包检查会把成品里的空标签当成测试文件。

## 验证

共享检查在 11:31 通过，11:54 复用同一结果：源码策略 2621 个 Java 源、125676 个原键、12624 个别名；core 行为 2806 条断言、33 组；机器规格 8035 条；材料 7947 条（另有 29 条流体显示、1212 条木材夹具）；热学 53 条。Java 17，无游戏依赖。

Forge 1.20.1 命名空间 `gregtech_signal_wire`：

- 11:51:51，6 项新增 addon 测试通过（`BUILD SUCCESSFUL in 4m 28s`）。在此之前的一次同命名空间运行有 3 项失败：石头、接收端探针和标签探针旁边放的是红石块。红石块只提供弱充能，`Level.getSignal` 对中间方块是 0，所以那次失败不能证明求解器把接收端当成了信号源。改为西侧通电拉杆的强充能后，这 3 项和另外 3 项一起通过。
- 12:01:55，12 项通过（`BUILD SUCCESSFUL in 3m 29s`）。其中 6 项是原有 `SignalWireTests`，断言未改。为了不跑整个 `gregtech` 套件，这次临时把该类的 holder 改到 `gregtech_signal_wire`，并临时放入同名空结构；跑完后 holder 和结构都已还原，该类相对 `origin/main` 无差异。

NeoForge 1.21.1 同一命名空间：11:56:32，12 项通过，游戏内 1.660 秒（含 Gradle 的整次 `:core:check :neoforge:runGameTestServer` 为 `BUILD SUCCESSFUL in 2m 4s`）。这 12 项是 6 项 addon 测试，加上按 Forge 原断言移植的 6 项 `SignalWireTests`。NeoForge 的移植测试留在 `gregtech_signal_wire`，不放进默认的 `gregtech_bootstrap` 全套。

去掉同路径测试数据包后重跑：Forge 13:35:35，6 项 addon 测试通过（`BUILD SUCCESSFUL in 3m 11s`）。NeoForge 13:38:30，12 项通过，游戏内 1.931 秒（`BUILD SUCCESSFUL in 1m 44s`）。

两端主源码和 gametest 源码在这些运行里编译通过（后续运行为 UP-TO-DATE）。没有跑 `gregtech` 全套 GameTest、专服或成品包。

Forge 开发客户端实际进过世界。第一次进程里，红色合金线会向周围实心方块自动伸接头，螺丝刀潜行右键没有减档。随后收紧放置连接，并让潜行螺丝刀点击回到信号线。12:57 重新启动客户端后，玩家回到同一世界，灯和线仍然亮着：内置 `mode` 被读回，信号按该档位重算。这次是新的客户端进程，不是只在同一局内重新打开菜单。放置和螺丝刀的修正还没有在这次重进里单独再看一遍。NeoForge 客户端没有开。
