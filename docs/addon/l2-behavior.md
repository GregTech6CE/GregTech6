# L2：运行时行为

本页用于实现行为机和功能部件的服务端逻辑、持久状态与客户端表现；先完成 [L1 声明](l1-declarations.md)及 [L0 provider 配置](l0-setup.md#八个-serviceloader-扩展点)。

返回[附属编写指南](README.md)。

## 行为机

行为机由 `BehaviorMachine` 声明、`BehaviorMachineContributor` 提供工厂、`MachineBehavior` 实现逻辑。宿主负责方块、方块实体、菜单、同步和默认界面；附属不继承内部方块实体，也不直接创建 Menu 或网络注册。

声明应明确：

- 主能源输入、辅助能源输入和能源输出；行为机可以选择无主能源，但必须至少声明输入或输出之一。
- 物品槽、流体罐、开关、进度箭头、能源表和 GUI 几何；默认 GUI 的固定尺寸在 `BehaviorMachineGui` 中公开。
- `recipeMaps`、`maxRange`、压缩气阀面、信号输出开关和需要同步到客户端的 state key。
- 机器类型与平台工厂一一对应。每个已放置方块实体只创建一个 `MachineBehavior`，实例不应假设跨存档或跨方块实体复用。

`MachineBehavior` 的主要回调：

| 回调 | 用途与边界 |
| --- | --- |
| `tick` | 服务端加工、输入输出和状态推进；每个 tick 都可能调用 |
| `onStructureFormed` / `onStructureBroken` | 结构状态变化通知；区块卸载或临时失效不等于破坏，不应在这里退款或掉落物品 |
| `canInsert(..., face)` / `canFill(..., face)` | 管道、菜单和容器的准入策略；有面规则时同时实现无面版本 |
| `acceptsEnergy` | 电缆注入前的包级准入；拒绝不会存入，也不会 overload |
| `acceptFluid` | 管道和玩家容器填充路径；需要改变流体身份时重写 |
| `crucibleContent` / `fillMoldAtSide` | 坩埚类行为的只读内容暴露和向模具浇铸 |
| `gtSignal` | 向相邻信号线输出 GT 分数量；声明必须开启 `signalOutput` |
| `onToggle` / `onTool` / `onUse` / `beforeBreak` | 开关、GT 工具、玩家使用和掉落前回调；返回值和顺序按接口约定处理 |

## 行为机上下文

`MachineBehaviorContext` 只在同一个行为回调期间有效，不能缓存到字段或异步任务。它提供：

- `level()`、`pos()`、`facing()`、`tier()`：当前方块位置和朝向。
- `inventory()`：主机器库存；快照和显式写入绕过自动输入输出槽位角色。
- `fluids()`：主机器流体罐；所有数量为 mB，模拟不得改变存储。
- `energy()`：主能源、辅助能源、发包、端口输出和部件供能。
- `structure()`：成型绑定、能源部件、功能部件、重复份数和完整单元快照。
- `panel()`：开关、进度和工作面状态。
- `world()`：范围内破块、收集物品和压缩气体推送。
- `state()`：持久化 int/long/boolean/string/item 字段；int 与 long 是不同类型表，不能混读。

库存和流体的“处理访问”不是自动化侧能力：它不经过 `canInsert` / `canFill` 和面过滤。需要让管道遵守行为策略时，应通过行为回调提供规则，而不是让外部调用内部视图。

`StructureAccess.cells()` 返回最近一次成功匹配的只读快照，可能包含普通外壳、普通方块、空气和重复层；未成型时为空。它不会因为读取而重扫世界，也不会授予占用其他机器部件的权限。`parts()` 只提供附属功能部件视图。

## 功能部件行为

`LayoutPartContributor` 创建 `PartBehavior`，`PartContext` 在部件回调或绑定主控回调期间有效。主要操作包括 `tick`、`onUse`、`canInsert`、`canExtract`、`canFill`、`canDrain`、`onSlotsChanged`、`describe` 和 `onWrench`；绑定主控时可用 `bound()`、`controllerPos()`、`controller()` 和 `crucible()` 读取控制器的只读状态。

部件字段必须在 `LayoutPart.Builder.field(...)` 中声明；未声明的键读取会抛错。机器行为可以接受合法的通用字段键，但部件只接受自己的声明。持久物品通过 `BehaviorState.setItem` 保存，拆除方块时由宿主处理掉落。

部件可声明宿主通用菜单纹理，但不提供自定义 Screen SPI；`LayoutPartClient` 只负责附加渲染器。需要复杂服务端逻辑时仍通过 `PartBehavior`，不要假设客户端视图可以修改服务端状态。

## 配方、结构与世界运行时

`AddonRecipes` 是访问已存在机器配方表的平台 facade：

| 方法 | 作用 |
| --- | --- |
| `map(name)` | 取得命名配方表；未知名称抛出 `IllegalArgumentException` |
| `add(line)` | 添加静态行；配方表可以拒绝并返回空 |
| `addMaterial(line, inputIndex, ingredient)` | 把一个输入替换为材料单元规则，不改变其他输入和催化剂角色 |
| `addDisplay(line)` | 只用于 JEI/EMI 展示，机器不会加工 |
| `collision(line)` | 查找输入会覆盖的已有静态行 |
| `hasItemInput(stack)` | 查询某个物品是否已经成为静态输入 |
| `addDynamic(provider)` | 添加按输入计算的动态行；动态行必须返回具体批次，不能返回材料规则 |
| `find(items, fluids, needsOutputs, inputSlots, outputSlots)` | 查询首个匹配行，不消耗输入 |

`addDynamic` 的附属优先级固定为中立值 `0`；动态 provider 接收不可变、分离的物品/流体快照，不能借此修改机器或另一 provider 的输入。需要材料级规则时使用 `addMaterial`，不要从动态行返回带 `materialInputs` 的 `Line`。

`AddonStructures.notifyChanged(level, pos)` 只在逻辑服务端使用，用于匹配相关数据改变但方块状态没有改变的情况；它会做失效通知和下一 tick 重新评估，不加载区块，也不立即扫描。`AddonStructures.diagnose` 和 `StructureAccess.diagnose` 是显式、较重的诊断，适合工具或玩家操作，不应每 tick 调用。宿主较旧而没有诊断服务时会抛出 `UnsupportedOperationException`，不得把异常当成“没有结构问题”。

## 客户端表现与持久状态

行为机客户端入口是 `BehaviorMachineClient`：

- `screen(...)` 返回空时保留宿主默认界面；返回自定义 Screen 时，宿主将它嵌入可工作的菜单外层，槽位仍然有效。
- `renderer(...)` 为具体机器类型注册附加方块实体渲染器；没有渲染器时保留默认绘制。
- 渲染器读取 `BehaviorMachineClientView`，只能看到 `BehaviorMachine.Builder.clientState(...)` 声明的键；未声明的键读取失败。
- `BehaviorMachineMenuView` 是只读视图，提供储能、进度、开关、罐量和 `containerId()`。用 `storedEnergy()` 读取完整的 `long` 储能值，`energy()` 自 API 1.10.0 起已弃用。默认屏幕使用菜单按钮路径切换开关，该路径仅支持菜单定义的按钮操作。

功能部件客户端入口是 `LayoutPartClient`，它只返回附加渲染器；部件本身仍由宿主同步 `PartClientView`。客户端 provider 不负责服务端行为，也不要在客户端 provider 中扫描世界作出权威判断。

## 能源包与外部交互

行为机的 `EnergyAccess` 使用 `packetSize × packetCount` 模型。`consumeMain` / `consumeAuxiliary` 是整笔操作，`generateMain` 返回实际存入量，`emitFromFaces` / `emitFromPorts` / `emitTo` / `transferToPart` / `supplyPart` 返回宿主的精确传输计账。请求超过声明上限、负数或溢出的请求会在改变状态前失败。

`WorldAccess.breakBlock` 和 `collectItems` 受 `maxRange` 约束，`pushCompressedGas` 受声明气阀面的接收规则约束。不要在行为中绕过这些范围或面限制；一次性破块、收集物品和压缩气体推送也不能当作无限制的世界编辑 API。

GUI 模板和自定义 Screen 的资源接入见 [GUI 模板](gui-templates.md)；端口输出约束见[多方块主动输出](multiblocks.md#主动输出)，详细诊断见 [L5](l5-query-and-diagnostics.md#结构诊断)。
