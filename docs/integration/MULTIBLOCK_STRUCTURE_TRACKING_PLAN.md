# 多方块结构跟踪改造计划

目标：结构没变时，结构检查的成本降为 0；结构有变化时，成本只和"被改动的格子数"有关，和结构大小、查询次数都无关。玩家可见的成型条件、区块加载保护和 `/reload` 行为不变；部件争用改为更严格的语义（见"认领表"）。

做法分两条线：先把"什么时候检查"从查询里拆出来（阶段 1），再把 15 个控制器各自重复实现的匹配、认领、释放、设计号和区块检查收敛到一份声明式结构定义上（阶段 2、3）。足迹、增量评估、放大镜诊断和预览都从这份定义推导，不再各写一份。

## 现状

- 15 个控制器类各自实现 `isStructureOk()`，项目里约 68 处调用。这个方法既是查询也是扫描：每次调用都逐格 `hasChunkAt` + `getBlockState`，然后 `PartBindings.update` 对每个候选 `canClaim` 并 `claim`；成功时还会 `StructureDesigns.apply` 把设计号重写到每个部件。
- 调用点不只有 tick。`MultiblockPortBlockEntity` 的能量接收判断、注入、流体 fill/drain、工具流体入口，以及 `MultiblockJadeProvider`、各控制器方块的交互，都会直接调它。一个能量包至少触发两次完整扫描。[ALIGN-232](LARGE_TURBINE_AUDIT.md) 量过：35 格涡轮做 20 次流体查询，需要 700 次格子校验和 700 次 bind。
- 有些调用点把它当"命令"用：`TankControllerBlock.toolStateChanged:59`、`OriginalLargeBoilerControllerBlock.toolStateChanged:68`、`LogisticsCoreControllerBlock.toolStateChanged:91` 丢弃返回值，只为在扳手旋转后强制重扫。
- 有些调用点在客户端执行：`TankControllerBlock.use:71` 和 `OriginalLargeBoilerControllerBlock.use:82` 在客户端扫描客户端世界，靠结果返回 `SUCCESS`，否则拿桶的客户端预测会出错。
- `LargeMachineLayouts.repeatCells` 在 `max < 0` 时从第 0 层一直走到 `LargeMachineLayouts.MAX_REPEATS`（1024）或世界高度，每次检查都重走一遍。`LayoutRepeat` 里没有对应常量，只有 `build()` 中 `max > 1024` 的校验。
- `BehaviorMachineBlockEntity.refreshStructure` 先把 `formed` 设为 false 再扫描，用来挡住扫描过程中端口重入查询。这个问题只是因为扫描发生在查询调用栈里才会出现。
- 部件绑定是瞬态的：`MultiblockPortBlockEntity` 的 `controller`/`role` 不写存档（类注释："Transient binding is rebuilt after chunk load"），`onLoad` 还会把 `CRUCIBLE_FORMED` 复位为 false。设计号相关的 `gt.design`、`gt.design_held`、`gt.design_saved` 写进存档。
- 区块卸载时两个版本的 `LevelChunk.clearAllBlockEntities` 都先调用 `onChunkUnloaded` 再调用 `setRemoved`。现在大部分控制器在 `setRemoved` 里 `bindings.clear(this::release)`，所以控制器区块卸载时会去改其他已加载区块里的部件：`release` 会 `restoreStructureDesign()`（`sendBlockUpdated`），`updateCrucibleVisual(false)` 会 `setBlock(..., 3)`。
- `MultiblockPortBlockEntity.canBind` 在原主人区块未加载时（`owner() == null`）允许被抢占。
- 原版 `Base10.checkStructure(false)` 只在 `mStructureChanged` 或强制时调用 `checkStructure2`，平时读缓存 `mStructureOkay`。

各家族的扫描入口：

| 家族 | 扫描方式 |
|---|---|
| `LargeRecipeMachineBlockEntity` 及子类（蒸馏塔、物质制造机、焦炉） | `LargeMachineLayouts.Layout`：固定格 + 可重复层 + 候选，`StructureDesigns.apply` 写设计号 |
| `FusionReactorControllerBlockEntity` | 覆写扫描：`FusionStructure.CELLS` 逐格，每格要求 `MultiblockPortBlockEntity`，处理器计数 3/12/12，不写设计号 |
| `BehaviorMachineBlockEntity`（附属行为机） | `LargeMachineLayouts.Layout`，按 `declaration.casing().machineType()` 取布局；无布局时直接成型 |
| `ImplosionCompressorControllerBlockEntity` | `MultiblockLayout.matches` |
| `OriginalLargeBoilerControllerBlockEntity`、`LargeGasTurbineControllerBlockEntity` | 遍历共享 `LAYOUT.cells()`，手写匹配 |
| `AxialGeneratorBlockEntity`（大型涡轮、大型发电机）、`FuelTurbineBlockEntity` | 手写逐格 |
| `MultiblockTankControllerBlockEntity` | `HollowTankStructure.validate` |
| `LogisticsCoreControllerBlockEntity` | `LogisticsCoreStructure.counts` + 逐格认领 |
| `LargeHeatExchanger`、`BedrockDrill`、`LargeCrucible`、`VonDaGraagg`、`LightningRod` | 各自手写 |

## 必须保持的行为

1. 结构里任一格所在区块未加载时，机器不运行，端口不提供能力。不能为了少扫描而相信未加载区块里的旧结果。
2. 部件只能属于一个控制器。争用失败的控制器不成型。
3. 结构检查期间和检查结果未出时，端口查询不能看到上一次的成功结果。具体做法和代价见阶段 1 的"失效语义"。
4. `/reload` 之后，已经成型的机器在下次检查时按新布局和新 tag 判断；不在布局里的部件会被解绑。
5. 没有布局的行为机在第一个服务端 tick 就是成型状态（[ADDON_API](../ADDON_API.md) 的现有合同）。
6. 设计号、`gt.*` NBT 键和保存格式不变。缓存和认领不写入存档。
7. 两个版本（`1.20.1-forge`、`1.21.1-neoforge`）行为一致。
8. 客户端交互预测（拿桶点储罐、锅炉）和现在一致。

有意改变的行为只有一条：控制器区块卸载后，它的部件不再能被别的控制器抢走（见"认领表"）。

## 阶段 0：基线计数

在 gametest 环境里加一个只在测试中启用的计数器：匹配器调用次数、`getBlockState` 次数、bind/release 次数。每个家族至少一个场景记录"成型后静置 100 tick"和"成型后 20 次端口查询"的计数，作为后面各阶段的对照。这些数字只用来比较前后，不用来宣称 TPS。

## 阶段 1：查询与扫描分离（收益最大，风险最低）

不改任何匹配语义，只改"什么时候扫"。`Layout` 家族抽出逐格匹配器只是重构，判定结果不变。

### 结构定义接口（先以 Custom 形式接入）

本阶段就引入统一接口，非 `Layout` 家族先以 `Custom` 接入，阶段 2 再逐个改成声明式：

```java
sealed interface StructureDefinition permits Declarative, Custom {
    ClaimPolicy claimPolicy();                  // ATOMIC | PER_CELL
    default void onFormed(Ctx c) {}             // 边沿回调：缓存端口、能量部件等
    default void onBroken(Ctx c) {}
}

non-sealed interface Declarative extends StructureDefinition {
    CompiledPattern compile(Direction front);   // 固定格 + 可重复组 + 每格候选（含逐格匹配器）
    List<Aggregate> aggregates();               // 计数 / 同类约束
}

non-sealed interface Custom extends StructureDefinition {
    Footprint footprint(Direction front);       // 全部可能影响成型的坐标，含 AIR 格和生长前沿
    boolean scan(Ctx c);                        // 沿用现有的整段扫描
    Projection projection(int cell);            // 默认 BLOCK；需要比较属性的格子声明 STATE 或 FLUID
}
```

- `Layout` 家族（`LargeRecipeMachineBlockEntity` 及其子类中走 `Layout` 的那些、`BehaviorMachineBlockEntity`）在阶段 1 就以 `Declarative` 接入：足迹由 `Layout` 推导，逐格匹配器从现有的 `matchingCell` 抽出来。此时 `scanStructure()` 仍是整段扫描，只是投影比较用的是真正的匹配器。
- 其余家族以 `Custom` 接入，扫描体沿用现有方法，足迹手写。

**足迹一致性检查**：测试构建里，`Custom` 扫描期间记录所有 `getBlockState`/`getBlockEntity`/`hasChunkAt` 的坐标，断言全部落在声明的足迹内。手写足迹和手写扫描一旦对不上，测试直接失败。

**投影一致性检查**：足迹检查只能证明"读的坐标都在足迹里"，不能证明投影判断没有漏掉变化。某格用了 `BLOCK` 投影，而扫描体实际读了方块属性，变化就会被静默放过。所以测试构建的审计模式里再加一条：每当 `onChanged` 判定某格"等价、不标脏"时，立即对该 tracker 跑一次整段扫描，断言结果和缓存的 `formed` 一致。正式构建不跑。

### 足迹收紧

可重复组和避雷针杆柱的足迹会随结构生长变化。收紧后的足迹**由扫描结果推导，不由运行时读取记录推导**：

- `Layout` 家族：固定格 + 每个可重复组已匹配的层 + 完整的下一层（生长前沿），即 `LayoutStructures.of(layout, origin, front, matchedLayers, true)`。扫描体把各组匹配到的层数交给 tracker。
- 避雷针：底座 + 已匹配的 `rodLength` 段 + 下一段。
- 固定格家族不收紧。

不用"记录扫描实际读了哪些格子"来收紧，原因有两条：

1. 正确性会挂在 `Level.getBlockState`/`getBlockEntity` 的注入上。别的模组改写方块访问路径、或者扫描体改用别的读取方式，都会让记录少记，结果是静默漏掉失效，而且测试里复现不出来。
2. `hasChunkAt` 返回 false 时扫描不读方块状态，这一格既进不了收紧后的足迹，它所在的区块也进不了覆盖区块集合。那个区块之后加载时找不到 tracker，机器停在 `BROKEN` 直到安全网。

读取记录（`StructureScanProbe` 和三个探针 mixin）只用于测试构建的足迹审计。正式构建里探针入口都要先检查 `activeScans == 0` 再做任何事，`getBlockEntity` 的 HEAD/RETURN 也一样，不能每次都查 `ThreadLocal`。`pause`/`resume` 要能扛住中间抛出的异常，可以改用"进入时记深度、退出时恢复到这个深度"的写法。

`Footprint` 是否使用稠密数组按密度判断（例如 `包围盒体积 ≤ 8 × 格子数`），不按体积上限判断。否则两个相距很远的格子也会分配几 MB 的数组。

### 新增类

位置：`multiversion/src/main/java/com/gregtech/gregtech/api/multiblock/tracking/`。

- `StructureTracker`：每台服务端控制器一个。保存 `state`（`UNKNOWN`、`FORMED`、`BROKEN`、`SUSPENDED`）、`dirty`、布局代数 `epoch`、足迹、包围盒和覆盖区块集合。`formed()` 是 O(1) 字段读取。提供 `invalidate()`（标脏并入队）。
- `StructureWatchIndex`：每个 `ServerLevel` 一份。`Long2ObjectMap<SectionWatch>` 以 `SectionPos.asLong()` 为键，`SectionWatch` 只是一个 tracker 列表（通常一两个）。登记按包围盒覆盖的区段做，不逐坐标登记。命中后由 tracker 用"偏移 → 格号"的稠密数组判断该坐标是否在足迹内。可重复组的生长前沿变化时只更新包围盒。
  - **数据挂在 `ServerLevel` 上**（mixin 注入字段），不放进以 `Level` 为键的全局静态 `WeakHashMap`。全局表有两个问题：一是值通过 tracker、方块实体间接强引用着键，弱引用永远不会生效，清理全靠 `LevelEvent.Unload`；二是单人游戏里客户端线程也可能访问它，而 `WeakHashMap.get` 内部会调用 `expungeStaleEntries()` 改表，和服务端线程并发就会损坏表。
  - **所有入口只在服务端执行**。客户端方块实体也会走 `onChunkUnloaded`、`setRemoved`，`StructureTracking.onChunkUnloaded/unloading/onRemoved` 这类入口第一行就要判断 `level.isClientSide` 并返回。
- `StructureScheduler`：每个世界一个脏队列。在 `LevelTickEvent` 的 END/Post 阶段处理，写法照 `FluidPipeTickPhases` 的双版本事件注册，但优先级设为 `EventPriority.LOWEST`，尽量排在其他 END 处理器之后。排在它后面的写入会顺延到下一 tick 处理；在"失效语义"下这仍然安全，因为相关 tracker 的 `formed` 已经是 false。世界卸载时清空。

### Mixin

新增 `LevelChunkStructureWatchMixin`，在 `gregtech.mixins.json`（1.20.1）和 `gregtech-neoforge.mixins.json`（1.21.1）里都注册。

- 给 `LevelChunk` 注入 `long[] gt$watchedSections` 位掩码，长度为 `ceil(区段数 / 64)`。数据包维度最高 4064 格、254 个区段，`int` 不够。
- 在 `LevelChunk.setBlockState` 返回非空（状态确实变了）处注入。客户端或该区段位为 0 时直接返回；否则调用 `StructureWatchIndex.onChanged(pos, oldState, newState)`。
- **状态投影比较**：`onChanged` 先找到命中的 tracker 和格号，对新旧状态做投影，投影相同就不标脏。这样 `CRUCIBLE_FORMED`、`AddonEnergyPartBlock.SATISFIED`、流体液面高度、火和草之类的变化都不会触发检查，也就不需要"自触发抑制"窗口。
  - `Declarative` 格子：用该格的匹配器分别对新旧状态求值，比较结果（不匹配，或命中第几个候选）。匹配器要按该格实际语义求值，例如 `anyopaque` 用 `isSolidRender`，不能只比较方块身份。
  - `Custom` 格子：默认 `BLOCK` 投影，新旧状态的 `getBlock()` 相同就不标脏。结果依赖方块属性的格子（流体格、`anyopaque` 格、按朝向或属性判定的格子）由定义显式声明为 `STATE` 或 `FLUID`。每个 `Custom` 家族接入时都要逐格审一遍它的扫描体是否读了属性。
  - 需要方块实体的格子：方块不变则实体不变，按上面的规则比较即可。
  - 在"失效语义"下，这一步不只是优化，而是正确性要求。`AddonEnergyPartBlockEntity.drainCondition` 每 tick 在部件自己的 tick 里按条件输入是否付清写 `SATISFIED`，可能频繁翻转；不过滤的话，每次翻转都会让所属控制器立即停机到 tick 末，本 tick 剩余的端口查询全部被拒绝。
- `watch()` 给其他区块设掩码时只用 `getChunkSource().getChunkNow()`，不能触发区块加载；`onLoad` 可能发生在区块加载过程中。区块之后加载时由 `ChunkEvent.Load` 按索引补设掩码。
- 两个版本的方法签名分别核对，写进 mixin 的 `target`。

### 调用点分类

把约 68 个调用点逐个归入三类，分别处理：

| 类别 | 例子 | 改法 |
|---|---|---|
| 查询（服务端） | `serverTick`、端口能量/流体能力、`MultiblockJadeProvider.inspect`、`injectPortEnergy` | 读 `tracker.formed()` |
| 命令 | 三处 `toolStateChanged` | 改为 `tracker.invalidate()` |
| 客户端 | `TankControllerBlock.use:71`、`OriginalLargeBoilerControllerBlock.use:82` | 读同步到客户端的 `formed` |

`CrucibleInteractionHandler:61` 等事件处理器要先确认在哪一端执行再归类。

**客户端同步**：`formed` 只在状态边沿变化时通过方块实体更新包同步（`sendBlockUpdated` + `getUpdateTag`/`onDataPacket`）。`getUpdatePacket` 不要为此变成无条件返回数据包：以前不需要同步的方块实体（例如没有 `clientKeys` 的行为机）应该只对 `TrackedMultiblock` 发包，其余照旧返回 null，否则每次 `sendBlockUpdated` 都会多发一个方块实体包。这个键只放在更新包里，不进 `saveAdditional`，符合合同 6。客户端的 `isStructureOk()` 返回同步值。区块初次发送给客户端时，服务端 tracker 可能还是 `UNKNOWN`，客户端先收到 false，之后靠边沿更新包补正。这个窗口里客户端拿桶点击会按未成型预测，场景 13 要覆盖"刚进入区块就点击"。

### 控制器生命周期

- 把现在 `isStructureOk()` 的方法体原样改名为 `scanStructure()`，只由调度器调用。`isStructureOk()` 在服务端返回 `tracker.formed()`，在客户端返回同步值。
- `onLoad` 时创建 tracker。有结构定义的设为 `UNKNOWN` 并入队；**无布局的行为机直接设为 `FORMED`，不经过调度器**。方块实体 tick 在 `LevelTickEvent` 的 END 之前执行，经过调度器会晚一个 tick，违反合同 5。
- `onChunkUnloaded` 设置 `unloading` 标志并注销监视；随后的 `setRemoved` 看到这个标志时什么也不释放。只有不带这个标志的 `setRemoved`（控制器真正被拆除）才释放认领。`LightningRodControllerBlockEntity` 和 `LogisticsCoreControllerBlockEntity` 已经覆写了 `onChunkUnloaded`，其余控制器照着补。
- 朝向变化时重算足迹并标脏。
- 部件内部影响匹配的数据变化（不经过方块状态的）由部件调用 `StructureWatchIndex.touch`。
- `TrackedMultiblock.structureFormedChanged(boolean)` 只能改瞬态字段，不能改会写进存档的状态，也不能改世界。它会在 `LevelChunk.setBlockState` 的注入点里同步调用，也会在区块卸载、存档之前调用（见"区块加载状态"）。
- 方块实体的 `onLoad` 时机晚于 `ChunkEvent.Load`：1.20.1 Forge 里区块加载时方块实体先进入 `Level.freshBlockEntities`，到下一次 `tickBlockEntities` 开头才调用 `onLoad()`。1.21.1 NeoForge 的顺序需要单独核对。所以任何在区块加载时刻按"有没有 tracker"做的判断都不可靠，必须用 `getBlockEntity(pos) instanceof` 控制器类型，或者推迟到 `onLoad` 之后。

### 失效语义

合同 3 要求检查结果未出时也不能看到上一次的成功结果，所以：

- `onChanged` 判定某格相关（投影不同）时，**同步**把 tracker 的 `formed` 置为 false 并入队，不等到 tick 末。`toolStateChanged` 调用的 `invalidate()` 同理。这堵住了"结构已破坏，但在当 tick 末之前仍能收能量"的窗口。
- 代价一：任何被判定为相关的变化，都会让机器停机到当 tick 末，即使结论仍是成型，例如把外壳换成另一个合法候选。`BasicMachineBlockEntity.tickServer` 在 `structureComplete()` 为 false 时会把 `mActive`、`mRunning` 置为 false 并调用 `updateBlockStates(false,false)`，如果控制器在这个窗口里 tick，外观会闪一下，并产生额外的方块更新。这个代价接受，用场景 22 固定下来。
- 代价二：投影过滤从优化变成了正确性要求（见 Mixin 一节）。不过滤的话，部件在自己的 tick 里写 `SATISFIED`，就会让控制器在本 tick 剩余时间里被判为未成型。

如果之后发现这两个代价不可接受，退路是把合同 3 改回"扫描期间"，接受最多一个 tick 的陈旧窗口。原版就是这个语义。

### 区块加载状态

- 扫描前，调度器先按 tracker 的覆盖区块集合检查一次加载状态。有任何一个未加载就设为 `SUSPENDED`，`formed` 为 false，不调用 `scanStructure()`，认领保留。
- `ChunkEvent.Load` 触发时 `hasChunkAt` 不一定已经返回 true。每个世界维护一个"已收到 Load、但 `hasChunkAt` 仍为 false"的区块小集合，每 tick 检查一次，区块变为可用时再把覆盖它的 tracker 标脏，然后移出集合。成本只和正在加载的区块数有关，`SUSPENDED` 不需要等安全网才恢复。
- `ChunkEvent.Unload` 时把覆盖该区块的 tracker 设为 `SUSPENDED`，同时把该区块移出上面的集合。
- 所有让 `formed` 从 true 变为 false 的路径都要发客户端同步，包括 `ChunkEvent.Unload` 导致的 `SUSPENDED`。否则控制器区块仍加载时，客户端会一直认为机器已成型。
- **待核对的事件顺序**：Forge 1.20.1 的 `ChunkMap.scheduleUnload` 应该是先发 `ChunkEvent.Unload`、再保存区块、最后才 `level.unload`（方块实体的 `onChunkUnloaded`、`setRemoved` 在这一步）。如果确实如此，控制器自己所在的区块卸载时，会先经由 `ChunkEvent.Unload` 被设为 `SUSPENDED` 并调用 `structureFormedChanged(false)`，然后才存档，之后才看到 `unloading` 标志。上面那条"`structureFormedChanged` 不改存档状态"的约束就是为了这个。两个版本的顺序都要对照源码确认，并用真实卸载的测试固定下来（场景 26）。

### 调度与预算

- 阶段 1 的 `scanStructure()` 不可中断，所以预算按"足迹格数估算"计，不按匹配次数计。
- **已成型且脏的 tracker 不受预算限制**，在当 tick 末必须处理完。停机在变化发生时已经同步完成（见"失效语义"），这条通道保证的是"变化后结论仍为成型"的机器最多只停到当 tick 末。
- 预算只用来限制全量扫描：`UNKNOWN`、`epoch` 变化、读档或区块加载带来的批量检查。初始取每个世界每 tick 20 000 格，用测试场景调整。
- **公平性**：预算队列先进先出。每 tick 至少处理队首的一个 tracker，即使它的成本超过整份预算；遇到第一个放不下的就停止，留到下一 tick，不能 `continue` 去处理后面更小的。否则足迹超过预算的 tracker 永远轮不到，中等大小的 tracker 在队列繁忙时也会被小 tracker 一直插队。

### 重扫时的认领步骤

阶段 1 每次重扫后，现有代码仍会对全部部件执行 bind 和 `StructureDesigns.apply`。即使设计号不变，`MultiblockPortBlockEntity.setStructureDesign` 也会调用 `setChanged()`，把部件所在区块标记为需要保存。所以"每次变化一次扫描"的成本里要算上 N 次 bind 和 N 个区块的保存标记。

阶段 1 就加一个短路：扫描得到的候选集合（坐标、角色、设计号）和上次成功时相同，且这些部件仍绑定在本控制器上时，跳过 bind 和 `StructureDesigns.apply`。阶段 3 的边沿认领会彻底取代这一步。

### `/reload`

- 在现有的 `StructureLayoutLifecycle.tags`（`TagsUpdatedEvent` 且 `shouldUpdateStaticData()`）里，`StructureLayoutOverlayRuntime.apply` 之后递增全局 `epoch`。**每次都递增**，不只在 overlay 变化时：附属布局里的 `#tag` 和 `anyfluid#tag` 格（`PlainStructureBlocks.matches:48–59`）会随 tag 变化，而 `LargeMachineLayouts.forget` 只清掉 overlay 涉及的机器。内置布局只按部件号和方块身份匹配，不受 tag 影响。
- tracker 的 `epoch` 落后时按脏处理，在预算内逐步重扫。
- **和合同 3 的关系**：如果只是"`epoch` 落后就入队"，重扫之前 `formed` 仍是 true，端口会按旧布局继续工作几个 tick，这和合同 3 的严格语义冲突。规则改为：
  - 递增 `epoch` 时，判定条件可能改变的 tracker 同步失效（`formed` 置 false 并入队）。范围是：overlay 这次涉及的机器，以及布局里含 `#tag`、`anyfluid#tag` 格的机器。这些机器在 `/reload` 后会停机，直到预算轮到它们。
  - 其他 tracker 只标记 `epoch` 落后，`formed` 保持不变，按预算重扫，作为兜底。内置布局不受 tag 影响，结论不会变。
  - 如果觉得"`/reload` 后部分机器停机几个 tick"不可接受，另一个办法是在 DECISIONS 里把 `/reload` 记为合同 3 的例外。二选一，不能两边都不写。

### 安全网

周期 600 tick，按位置哈希错开相位。`FORMED` 和非 `FORMED` 两种状态都兜：`FORMED` 防止漏掉破坏，`BROKEN`/`SUSPENDED` 防止漏掉补全和区块恢复（原版兜的是后者）。按 200 台机器、每台 100 格估算，平均每 tick 约 33 次格子检查。

tracker 在创建时按相位放进 600 个桶中的一个，每 tick 只取当前相位的桶，不要每 tick 遍历全部 tracker 再比较相位。

### 回滚开关

增加一项配置 `multiblock.structureTracking`，取值 `tracked`（默认）或 `legacy`。`legacy` 时 `isStructureOk()` 直接调用 `scanStructure()`，即现在的行为。阶段 1 到 3 都保留这个开关，阶段 4 再删除。配置项名称在实现时按项目现有配置组织方式对齐。

阶段 1 完成后：静置时的扫描次数为 0；端口查询不再触发扫描；每次真实变化在当 tick 末触发一次现有的完整扫描。

### 阶段 1 进度（2026-10-24 收尾，卡 52）

| 项 | 状态 | 落地 |
|---|---|---|
| 1 服务端索引的线程归属 | 完成 | 数据挂在 `ServerLevel` 的 mixin 字段上，删除静态 `WeakHashMap<Level, ...>`；客户端入口提前返回；测试构建校验服务端线程 |
| 2 调度公平性 | 完成 | `dirty` 改 `ArrayDeque` + 在队标志；每 tick 至少评估队首一个；放不下时按原顺序放回队首；加急通道不变 |
| 3 足迹收紧由扫描结果推导 | 完成 | 扫描体回传各组匹配层数；`LayoutStructures.of(..., int[], true)`；避雷针用 `rodLength + 1`；读取记录只留给审计 |
| 4 探针热路径与异常安全 | 完成 | `activeScans == 0` 提前返回；`pause` 返回深度、`resume(depth)` 恢复，嵌套用配对栈 |
| 5 卸载事件顺序与客户端同步 | 完成 | 外壳区块卸载时补 `ClientFormedSync.send`；新增真实卸载/重载场景与事件顺序记录器 |
| 6 `/reload` 与合同 3 | 完成 | tag 格和本次 overlay 涉及的机器同步 `invalidate()`，其余只标 epoch 落后 |
| 7 投影审计 | 完成 | `auditing()` 下对"等价"判定跑整段扫描；负例覆盖 BLOCK 投影说谎 |
| 顺手修 | 完成/跳过 | 安全网分桶、`Footprint` 密度阈值、删除重复的安全网边沿分支；`onChanged` 的 `List.copyOf` 保留；`getUpdatePacket` 未改 |

## 阶段 2：声明式定义与增量评估

把各家族从 `Custom` 迁移成 `Declarative`，并在 `Declarative` 上做增量评估（阶段 1 已接入的 `Layout` 家族直接开始增量评估）。逐个家族迁移，每迁一个跑一次一致性检查和模糊测试。

### 家族映射

| 家族 | 声明方式 |
|---|---|
| 蒸馏塔、物质制造机、焦炉、附属行为机 | 由 `LargeMachineLayouts.Layout` 编译：固定格 + 可重复组 + 候选 |
| 聚变 | 固定格（`FusionStructure.CELLS`）+ 处理器计数聚合 3/12/12，不写设计号 |
| 大型涡轮、大型发电机、锅炉、燃气涡轮、换热器、内爆压缩机、禁刷怪塔 | 固定格 |
| 储罐 | 每个尺寸一个图样（3³、5³ 由方块的 `size()` 决定） |
| 避雷针 | 底座固定格 + 无上限的杆柱重复组 |
| 物流核心 | 固定框架 + CPU 计数聚合，认领策略 `PER_CELL` |
| 大型坩埚 | 固定格 |
| 大型烤炉 | 线圈同类约束（替代现在的 `ovenCoil[0]`） |
| 基岩钻机 | 支撑固定格；地面 9 格只要求基岩类方块，矿物列表移到运行逻辑（见文末「待定决策」第 1 条） |

无法表达的家族留在 `Custom`，但仍受足迹一致性检查约束。

### `CompiledPattern`

在布局加载和每次 `epoch` 递增时生成（tag 绑定之后）：

- 每个水平朝向存一份 `int[] dx, dy, dz`，运行时不新建 `BlockPos`。
- 每格一个共享的匹配器 id。只看方块状态的格子按 `BlockState` 身份集合判定；需要方块实体的格子额外检查实体类型。候选在同一个匹配器里有序尝试，结果记成"命中第几个候选"。
- `plain` 方块和 tag 按 `PlainStructureBlocks.matches` 的语义编译；tag 解析出的身份集合随 `epoch` 重建。
- 聚合约束单独声明，按计数增量维护。

### Tracker 增量状态与评估规则

每格的 `seenState`、`seenEntity`、`choice`；`failCount`；每个可重复组的 `repeatCopies`；聚合计数。

- 首次检查、旋转、`epoch` 变化时做全量评估。遍历按区段分组，每个区段只取一次 `LevelChunkSection`。
- 某个成员区块重新加载时，只把该区块内的格子置为未知并重评估，不做全量重扫。
- 其余情况只重评估脏格。新状态与 `seenState` 引用相同、并且方块实体也相同时，直接跳过。
- 可重复层保持现在的前缀语义：脏格落在第 k 层时，前 k−1 层保留，从第 k 层往上重走，并更新生长前沿和包围盒。
- 当 `failCount == 0`、所有可重复组满足最小层数、聚合约束成立时，进入认领步骤，然后才算成型。

### 测试手段

- **一致性检查**：测试构建里每次增量评估之后，额外跑一次阶段 1 的整段扫描，两者结果不一致就让测试失败。正式构建不跑。
- **随机模糊测试**：对每个家族，在足迹内随机改格子（放置、破坏、换候选、换方向），每一步比较增量结果和全量结果。重点覆盖可重复层前缀、生长前沿和聚合约束的边界。

## 阶段 3：认领表与边沿副作用

### 认领表

每个 `ServerLevel` 一张，是部件归属的唯一事实来源，完全瞬态：

- 正向：`Long2LongMap`，部件坐标 → 控制器坐标。反向：控制器坐标 → 部件坐标集合，用于释放和孤儿清理。
- **条目只为已加载的部件存在，寿命跟随部件所在区块，不跟随控制器。** 表的大小上限是当前已加载的部件数，不随玩家探索范围增长。
- 部件上的 `controller`/`role` 字段降为认领表的派生缓存，只在认领和释放时写入，不单独参与判断。
- 端口的 `owner()` + `isStructureOk()` 变成：认领表里有本部件的条目，且该控制器已加载、tracker 为 `FORMED`。

条目的生命周期：

| 事件 | 处理 |
|---|---|
| 控制器成型认领 | 写入条目（`ATOMIC` 全部写入，`PER_CELL` 逐格写入） |
| 控制器解体或被拆除 | 释放自己的全部条目，恢复设计号、复位坩埚外观 |
| 控制器所在区块卸载 | 认领表里的条目保留，不触碰部件；控制器的 tracker 随实体销毁；它在预留表里的记录一并删除 |
| 部件所在区块卸载，主人已加载 | 条目从认领表移到预留表（见下），不改部件状态（设计号的 held 状态已随部件存档）；主人因区块缺失进入 `SUSPENDED` |
| 部件所在区块卸载，主人未加载 | 静默删除条目，不改部件状态 |
| 部件所在区块加载 | 在 `ChunkEvent.Load` 处理器里同步把该区块的预留写回认领表 |
| 部件被拆除或替换 | 删除条目，标脏所属控制器 |
| 控制器重新加载 | 新 tracker 全量评估；指向自己的现有条目视为已认领 |

部件也要区分"区块卸载"和"真正被拆除"：和控制器一样，`onChunkUnloaded` 设标志，带标志的 `setRemoved` 只按上表的卸载规则处理。

**预留表**：每个世界一张，按区块索引，记录 `(部件坐标, 控制器坐标)`，只包含"控制器已加载、部件区块未加载"的那部分认领。它的大小上限是已加载控制器足迹里落在未加载区块的格子数。部件区块加载时，预留在 `ChunkEvent.Load` 里同步写回认领表，之后无论扫描顺序、`hasChunkAt` 延迟、重试还是预算推迟，竞争者看到的都是"已被占用"。原主人随后评估时如果发现这一格不再匹配，按正常解体规则释放。

争用规则（方案 B，严格）：

- 部件已有条目、且条目指向别的控制器时，一律不可认领，**无论那个控制器是否已加载**。去掉现在 `canBind` 里 `owner() == null` 的抢占分支。
- 认领失败的控制器保持 `BROKEN`，记录错误"部件被某坐标占用"，并订阅该部件条目的删除事件。条目一删除，它就被加入调度队列，不需要轮询。
- 由此产生的有界语义：控制器未加载、并且部件区块又卸载过一次之后，部件可以被别的控制器认领。这是"条目寿命跟随部件区块"的直接结果，有意接受。

孤儿条目（控制器位置已经没有控制器，例如区块被外部工具删除、方块实体没走 `setRemoved` 就消失）：

- 控制器坐标所在区块加载后，由调度器在**至少晚一个 tick**、且 `hasChunkAt` 为 true 时检查：`getBlockEntity(pos)` 不是对应的控制器类型，才释放反向索引里它的全部条目。不能用"有没有 tracker"判断，因为控制器的 `onLoad` 晚于 `ChunkEvent.Load`（见"控制器生命周期"），正常控制器在那时还没有 tracker。
- 安全网遍历时顺带检查：控制器区块已加载超过一个 tick、且该位置的方块实体不是对应控制器类型的条目，一律释放。

已知的现有限制，本计划不改：部件在区块卸载期间失去主人时，它存档里的 `gt.design_held` 会保持到下一次认领或释放。

### 边沿副作用

- 只在状态边沿认领和释放：进入 `FORMED` 时批量认领并写入设计号，调用 `onFormed`；离开 `FORMED` 时释放并调用 `onBroken`。某格的候选变化时，只重写那一格的设计号。
- 控制器不再每 tick 轮询结构，只响应 `onFormed`/`onBroken`；缓存端口列表、能量部件等在 `onFormed` 里建立。
- 逐格认领（`PER_CELL`）：原版"遇错继续、逐块认领"的家族（焦炉、物流核心等，见 [焦炉审计](COKE_OVEN_AUDIT.md) 和 036 条目）可以改用它。等 `ATOMIC` 认领表稳定后再决定是否开启。

## 阶段 4：收尾

- 删除 `legacy` 开关和已迁移家族的旧整段扫描（`Custom` 家族保留自己的扫描方法）。
- 探针 mixin（`LevelStructureProbeMixin`、`ServerChunkCacheStructureProbeMixin`）只在测试构建里注册，或者在正式构建里保持零开销门控。到时候 `Custom` 家族只剩少数几个，可以评估是否改成只在 gametest 的 mixin 配置里启用。
- 放大镜改为由结构定义推导的诊断扫描：不提前停止，列出全部错误格。它和运行时检查分开。JEI 和结构预览也从同一份定义推导。
- 更新 [ADDON_API](../ADDON_API.md) 的 Structures 一节：结构检查时机、附属部件何时调用 `touch`、认领争用语义的变化。
- 在 [DECISIONS](DECISIONS.md) 记录区块卸载语义、认领表寿命规则和预算数值。

## 测试

已有的回归测试：`MultiblockNativeChunkLifecycleTests`、`MachineChunkBoundaryTests`、`MultiblockAlternativeBindingTests`、`BehaviorStructureSafetyTests`、`LargeRecipeControllerSourceTests`、`MultiblockTankTooltipStateTests`、`BedrockDrillOutputLifecycleTests`、`FusionCoverLifecycleTests`、`ControllerOutputLifecycleTests`。每个阶段都要在两个版本上跑完 `gradlew runGameTestServer` 全量。

需要新增的场景：

1. 成型后静置 100 tick，扫描计数为 0。
2. 成型后 20 次端口能量查询和 20 次流体查询，扫描计数为 0。
3. 拆一块外壳，在当 tick 末解体，补回后成型。
4. 同一 tick 内爆炸或批量放置改动 50 格，每台受影响的机器只评估一次。
5. 蒸馏塔加一层、拆中间一层，层数和端口绑定都正确。
6. 两台控制器争用同一个端口；一台拆除后，另一台在当 tick 末成型。
7. 结构跨越正负区块边界，卸载外壳所在区块后机器停机；重新加载后只对该区块内的格子重新评估并重新认领，库存和能量保持。
8. 成型状态下 `/reload` 改布局，机器按新布局解体或保持；附属布局的 `#tag` 格在 tag 变化后按新 tag 判定。
9. 旋转控制器后按新朝向检查；扳手旋转储罐、锅炉、物流核心后触发重新检查。
10. 活塞推走一块外壳。
11. 无布局的行为机在第一个服务端 tick 的方块实体 tick 中就是成型状态；有布局的按格检查。
12. 读档时 200 台机器同时首次检查，处理分摊到多个 tick，并且全部在预算允许的 tick 数内成型。
13. 客户端拿桶点已成型的储罐和锅炉返回 `SUCCESS`，未成型时返回 `PASS`；刚进入区块、成型状态还没同步到客户端时就点击，结果和服务端最终结论一致，不出现重复的流体操作。
14. 控制器区块卸载时，相邻已加载区块里的部件方块状态和设计号都不变。
15. 控制器区块卸载期间，另一台控制器不能认领它的部件；部件区块卸载再加载之后可以。
16. 控制器已加载、部件区块卸载再加载时，即使原控制器因 `hasChunkAt` 延迟或预算推迟了好几个 tick 才评估，竞争者也认领不到这个部件。
17. `ChunkEvent.Load` 时 `hasChunkAt` 仍为 false 的情况下，`SUSPENDED` 机器在区块可用后的下一个 tick 内恢复，不依赖安全网。
18. 高于第 32 个区段的结构（高维度数据包）能被正常监视。
19. `AddonEnergyPartBlock.SATISFIED`、`CRUCIBLE_FORMED` 和流体液面变化不会把 tracker 标脏；`Layout` 家族和 `Custom` 家族各测一个。
20. 预算被占满时拆一块已成型结构的外壳，机器立即停机；预算被占满时把外壳换成另一个合法候选，机器仍在当 tick 末恢复成型。
21. 孤儿条目：用测试手段让控制器无 `setRemoved` 消失，其部件在控制器区块加载后的检查或安全网之后可被重新认领。
22. 外壳换成另一个合法候选，机器只停机到当 tick 末，下一个 tick 恢复运行。
23. `SATISFIED` 每 tick 翻转，持续 100 tick，机器不停机，扫描计数为 0。
24. 控制器区块和部件区块同时加载、控制器的 `onLoad` 尚未执行时，它的认领不被孤儿清理释放。
25. 成型结构静置时，再次扫描（例如安全网触发）不对部件执行 bind 和设计号写入，部件区块不被标记为需要保存。
26. 真实区块卸载和加载：事件顺序符合「区块加载状态」一节；卸载期间机器停机，重载后恢复；存档里的控制器状态与卸载前一致；客户端在外壳区块卸载后收到 `formed = false`。
27. 一个足迹超过预算的 tracker，在队列里有很多小 tracker 时，也能在有限个 tick 内评估完；中等 tracker 不被小 tracker 持续插队。
28. 可重复层结构收紧足迹后，生长前沿所在的区块仍在覆盖集合里；该区块卸载再加载时，机器在安全网之前恢复评估。
29. `tracking/` 包里没有以 `Level` 为键的静态 `Map`；索引入口在非服务端线程上调用直接抛错。
30. `/reload` 改 overlay 布局或 tag 后，受影响机器在 `epoch` 递增的同一 tick 内 `isStructureOk()` 为 false；不受影响的内置布局机器不停机。
31. 审计模式下跑结构测试零违例；故意构造的 BLOCK 投影负例上审计必须失败。

### 场景覆盖（2026-10-24）

| 场景 | 测试 | 备注 |
|---|---|---|
| 1、2 | `idleAndPortQueriesScanOnlyInLegacy` | 逐家族计 idle 和端口查询 |
| 3 | `brokenShellUnformsAtEndOfTickAndRepairs` | |
| 4 | `fiftyCellEditsEvaluateEachMachineOnce` | |
| 5 | `repeatLayerAddsAndDropsTheMiddle` | |
| 6 | `removingOneControllerLetsTheOtherForm` | |
| 7 | `shellUnloadStopsAndReloadKeepsContents` | 卸载钩子；库存/能量保持 |
| 8 | `epochReloadFollowsTheNewLayoutAndTag` | |
| 9 | `wrenchAndFacingRecheckTankBoilerAndLogistics` | |
| 10 | `pistonPushesAPlainCasingOutOfTheLayout` | |
| 11 | `noLayoutFormsOnTheFirstQueryAndALayoutChecksCells` | |
| 12 | `twoHundredMachinesShareTheCellBudget` | |
| 13 | `bucketClickFollowsTheSyncedFormedBit` | |
| 14 | `controllerUnloadDoesNotChangeLoadedParts` | 卸载钩子 |
| 15、16、21、24 | — | 阶段 3 的认领表场景，本阶段测不了 |
| 17 | `pendingChunkBecomesAvailableWithoutTheSafetyNet` | |
| 18 | `sectionMaskWatchesIndexesPast31` | |
| 19 | `satisfiedCrucibleFormAndFluidLevelDoNotDirty` | Layout 与 Custom 各一 |
| 20 | `exhaustedBudgetStillRepairsAFormedMachineThisTick` | |
| 22 | `exhaustedBudgetStillRepairsAFormedMachineThisTick` | 只覆盖成型位，未覆盖"恢复运行" |
| 23 | `satisfiedCrucibleFormAndFluidLevelDoNotDirty` | SATISFIED 翻转 100 次 |
| 25 | `safetyRescanDoesNotBindOrMarkChunks` | |
| 26 | `realShellChunkUnloadSuspendsAndReloads`、`realControllerChunkUnloadReattaches` | 真实卸载；事件顺序见 [DECISIONS](DECISIONS.md) |
| 27 | `oversizedTrackerIsEvaluatedBeforeSmallOnes` | |
| 28 | `tightenedFootprintKeepsTheFrontierChunk` | |
| 29 | `trackingStateIsOwnedByTheServerWorld` | 源码/线程守卫 |
| 30 | `reloadInvalidatesTagAndOverlayMachinesSynchronously` | |
| 31 | `projectionAuditAcceptsEquivalentChangesAndCatchesALie` | 正例 + 负例 |

## 待定决策

1. **基岩钻机的矿物候选**：对照原版 `checkStructure2` 后选移到运行逻辑。地面 9 格在结构里只要求基岩类方块，矿物列表不是成型条件，改在每个钻井周期读取；成型判定、矿物分支概率和绑定都不变。依据见 [BEDROCK_DRILL_AUDIT](BEDROCK_DRILL_AUDIT.md) 末节和 [DECISIONS](DECISIONS.md)（2026-10-24）。迁移随阶段 2 的家族迁移一起做。
2. **逐格认领**：等阶段 3 的 `ATOMIC` 认领表稳定后，再决定焦炉和物流核心是否开启 `PER_CELL`。
3. **预算数值**：初始每个世界每 tick 20 000 格估算，用场景 12 调整。

已定：区块卸载期间 `formed` 为 false、停机、保留认领；认领表条目寿命跟随部件区块，控制器卸载期间不可抢占，主人已加载时用预留表保住认领；相关变化同步失效（合同 3 的严格语义），接受"合法替换也停机到 tick 末"的代价；安全网 600 tick、按位置错开相位、两种状态都兜。

已定（#101 复查后补充）：收紧足迹由扫描结果推导，读取记录只用于测试审计；预算队列先进先出，每 tick 至少处理队首一个；`/reload` 时判定条件可能改变的机器同步失效（如果改为把 `/reload` 记为合同 3 的例外，需要在 DECISIONS 里写明）。

待核对：

- 1.21.1 NeoForge 里方块实体 `onLoad` 与 `ChunkEvent.Load` 的先后顺序。
- 两个版本里 `ChunkEvent.Unload`、区块保存、方块实体 `onChunkUnloaded`/`setRemoved` 三者的先后顺序。

2026-10-24 部分结论（卡 52 批次 5）：Forge 1.20.1 的 `ChunkEvent.Unload` 只在区块 holder 真正释放时发出。被 `setChunkForced`（`ENTITY_TICKING`，31 级票据）压住的邻近区块可以 `getChunkNow == null`、`hasChunkAt == false`，但不发 `ChunkEvent.Unload`；距离 0 的 `PLAYER` region 票据同样有传播，外壳要放到 16 个区块外才会真正释放。控制器区块无票据卸载时的顺序在 Forge 和 NeoForge 上一致：`ChunkEvent.Unload → onChunkUnloaded → setRemoved`，两条真实卸载测试两个平台都通过。方块实体 `onLoad` 与 `ChunkEvent.Load` 的先后、以及区块保存与这两者的相对顺序仍未单独测量。

## 参考

GTCEu Modern 也走"方块写入钩子 + 坐标到结构状态映射"的路线：`LevelMixin.markAndNotifyBlock` 调用 `MultiblockWorldSavedData` 里按区块索引的结构状态。它挂在 `Level.markAndNotifyBlock` 上，不经过这个方法的写入不会触发；本计划挂在 `LevelChunk.setBlockState` 上，覆盖面更全，但需要自己过滤客户端和无监视区段。
