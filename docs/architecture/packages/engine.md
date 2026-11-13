# engine：执行、状态与算法

先读 core 中的规则，再查 Minecraft 层如何喂入世界与物品栈数据，以核心契约为基准而非从 BlockEntity 细节反推。

## core 中各包的职责

入口为 [core/gregtech/engine](../../../core/src/main/java/gregtech/engine)。这里可以保存状态、注册数据和执行计算，但不能依赖游戏类型。

| 包 | 应当承担 | 代表入口/边界 |
| --- | --- | --- |
| `engine` | 执行层总体说明 | `package-info.java`；不继续使用旧 internal 命名扩张实现 |
| `engine.addon` | 附属声明收集、校验、物化与共享服务绑定 | `AddonCoreServices`、`AddonMaterials`、`AddonMachines`、`AddonPipes` |
| `engine.definition` | 定义目录的组织与查询机制 | `DefinitionCatalog`；内容行放 content |
| `engine.material` | 材料原子安装（`MaterialRegistryState`）、引用与组校验（`MaterialLinker`）、附属事实冻结、物品成分与回收纯裁决、多代发布版本号（`MaterialPublication`）及不可变视图 | `GTMaterialRegistry`、`MaterialRegistryState`、`MaterialLinker`、`MaterialInfoRegistry`、`ItemCompositionResolver`、`ItemRecoveryPlanner`、`MaterialPublication`、`MaterialViews` |
| `engine.prefix` | 材料形态/前缀的注册与解析 | `PrefixRegistry`、`BlockPrefixRegistry` |
| `engine.recipe` | 配方成本、并行与输出空间拟合、燃料批次、精确单位换算与材料分解纯计划、可合成性和界面坐标等共享机制 | `MachineWorkCost`、`ParallelFit`、`FluidFuelChargeMath`、`UnitConversion`、`MaterialDecompositionPlanner`；不能夹带游戏栈 |
| `engine.recipe.data` | 配方文档模型、组件、解析及校验 | `MachineRecipeDocuments`、`ItemComponent`、`FluidComponent` |
| `engine.machine` | 通用机器规格、供能、燃烧与执行规则 | `BasicMachineEnergy`、`BoilerSpec`、`MaterialFuelRules` |
| `engine.machine.crucible` | 坩埚能量、物料、温度与加工过程 | `CrucibleProcess`、`CrucibleMath`、`CrucibleMaterialStack` |
| `engine.multiblock` | 多方块处理、部件绑定、控制器与工具操作规则 | `PartBindings`、`LargeMachineProcessingRules`；世界探测在 Minecraft 层 |
| `engine.energy` | 能源转换、连接、存储状态和门控 | `EnergyBridge`、`EnergyGate`、`BatteryBoxState` |
| `engine.fluid` | 大容量流体状态、物性、通道与安全计算 | `LongFluidStorage`、`FluidPhysicalProperties`、`FluidPipeSafety` |
| `engine.inventory` | 批量存储状态、槽位和容器形态协议 | `BulkStorageState`、`InventorySlotLayout` |
| `engine.logistics` | 路由、功耗、长距离网络与访问集合 | `LongDistanceNetwork`、`LogisticsRoutingRules`、`RelayVisitSet` |
| `engine.block` | 通用面网格、掉落状态与方块规则计算 | `FaceGridSlab`、`StatefulBlockLoot` |
| `engine.tool` | 手工工作、工具损耗、锻造与扫描耗能规则 | `AnvilRecipeCost`、`ManualWorkRules`、`ElectricToolWearRules` |
| `engine.sensor` | 测量、平均、面板和控制算法 | `SensorMeasurements`、`SensorAverage`、`SensorControl` |
| `engine.nuclear` | 核相关纯计算 | `RodPhysicsMath`；世界伤害留在游戏层 |
| `engine.kubejs` | 脚本加载顺序、机器声明与布局文档规则 | `KubeJsLoadOrder`、`KubeJsMachineDeclarations`；不引入 KubeJS 类型 |
| `engine.compat` | 可选模组的适配规格和共享规则 | `CompatSpecs` 等；第三方 API 调用留在 Minecraft 层 |
| `engine.mod` | 模组身份与一次性初始化控制 | `GregTechIdentity`、`SetupOnce`；不成为业务逻辑杂物箱 |

材料 process 的两种附属提交入口都在 `MaterialInfoRegistry` 冻结前合并，统一检查所有权、类型、基数和形态名称；`MaterialViews` 只投影该快照并检查材料实际允许的形态，不再次汇总附属提交。

## Minecraft 中的执行适配

入口为 [minecraft/gregtech/engine](../../../minecraft/src/main/java/gregtech/engine)。这些同名领域包允许游戏类型，但应尽量复用上表的纯规则。

| 包 | 应当承担 |
| --- | --- |
| `engine` | 当前的 `GTWaterloggable` 等游戏侧协议；新增内容优先归入具体子领域 |
| `engine.addon` | 加载器/平台服务、机器宿主、能源适配及输出表审计 |
| `engine.block` | 世界与显示所需的面坐标、方向变换 |
| `engine.data` | 游戏内数据端口，如 `UsbDataPort` |
| `engine.energy`、`engine.energy.item` | 世界能源网络、游戏能源存储、物品能源与面配置 |
| `engine.fluid` | 游戏流体端口、容器、显示绑定、危险与渲染层适配 |
| `engine.inventory` | 游戏物品库存、方块内容、大容量存储面与物料形态适配 |
| `engine.machine`、`engine.machine.crucible` | 机器基类、配方状态机（`RecipeProcessor`、`RecipeLookup`）、相邻能力缓存、控制和坩埚物品输入 |
| `engine.stack` | 两节点物品/流体栈与 NBT 的序列化、同一性助手（`StackCompat`、`NbtCompat`），供组件代码消去版本分支 |
| `engine.material` | ItemStack 的材料归属、候选快照、等价、统一和表现绑定；`ItemMaterialRegistry` 同时提供遍历与单物品查询 |
| `engine.multiblock` | `ControllerDeclaration` / `ControllerBinding` 关联主控、实体类型与结构；端口归属、工具目标和机器连接 |
| `engine.multiblock.tracking` | 结构模式编译、足迹、世界变化跟踪、聚合及客户端成形同步；`StructureConstraints` 把公共约束编译为缓存检查 |
| `engine.recipe` | 游戏配方对象、输入匹配、查询表、产出与 GUI 布局 |
| `engine.sensor` | 将游戏物品质量、机器进度等转为传感器源 |
| `engine.tool` | 游戏工具类型、附魔、交互目标与实际操作适配 |

版本节点另有同包补充，如 NeoForge 的 `engine.transport` 原生运输接入；检查[平台指南](platforms.md)及两个节点的实际类，不把共享包直接搬进 core。

## 多方块运行时

结构数量与同类方块规则从公共 `StructureConstraint` 进入统一匹配缓存，不再由具体主控重复扫描世界。`SameBlock` 还要求监视方块身份变化，即使旧、新方块同属一个标签也不能沿用先前结果。行为机的结构快照和状态回调由宿主维护；区块卸载造成的失效不是物品掉落事件。

成型后的部件外观由声明的 `design` 进入 `StructureTracker` 持有的 `StructureDesigns` 缓存。主控只在工作状态改变时调用 `StructureTracking.refreshDesigns`，不为动画重新认领或扫描整座结构；GT6 的方向和工作状态编号由 `OriginalMultiblockDesigns` 解释。结构失效时恢复部件原有外观，即使该部件仍保留工具路由；区块卸载期间不发送外观更新以避免意外加载区块。部件通过现有 `gt.design` 更新包同步，客户端同时更新模型数据、渲染层和区块网格。

`StructurePortHandlers` 将公开 `StructurePorts` 转为代理壁板的侧面、槽位和资源过滤。`StructureTracker` 只保存成功匹配单元的附加规则，规则不写入 NBT；能力视图每次重新读取当前主控和规则。`MultiblockPortOwner.portFluidTank` 必须对应聚合 handler 的同一物理槽，不能把“只允许槽 2”降级成任意槽填充。无法提供精确槽的主控在加载声明时拒绝这类限制。注册及数据包重载都提前验证规则，而不是留待管道第一次传输才发现错误。

`StructureOutputs` 执行主动输出，复用单元端口限制与实时邻居能力；`StructureOutput` 区分有效部件出口和 GT6 原有缓存排空出口。内置目标与分层分配位于 content 的 `OriginalMultiblockOutputRules`，通用配方主控复用已有的 `IoTarget`，执行器不按机器名分支。转换器仍负责能量扣减和原版耗散。

`StructureDiagnostic` 按同一编译定义进行按需只读扫描，产出公开的 `StructureProblem`。`AddonStructures` 和行为机 `StructureAccess` 通过宿主桥接提供给附属；放大镜复用同一结果和本地化格式。诊断不得触发 claim、成型回调或加载未知区块。

`MultiblockMenus` 将已成型结构部件的普通右键交给主控现有菜单；库存、流体和菜单同步仍属于主控。`StructureWatchIndex.formedController` 读取成功匹配的非空气单元，包含普通方块和实际匹配的重复层，不重新扫描结构；没有唯一归属的共享普通壁板不自动选主控。`MultiblockMenuAccess` 为每次开窗保存点击位置，距离按该部件计算，并持续检查主控身份、结构成型和部件归属。覆盖板、工具、液体容器和潜行交互优先；没有 GUI 的主控不生成新菜单。

## 验收

- 核心规则优先在 core 验证，世界适配还需双平台编译与相关游戏场景。
- 纯算法接收明确数值或值对象，说明单位、上下限、溢出和模拟/实际执行语义；能源、流体、材料量不能当作无意义的“数量”混用。
- 库存或资源转移需验证失败路径和守恒，世界网络需考虑邻接变化与区块卸载；跨 tick 状态明确谁持有、谁保存、何时失效，避免不受控静态集合持有世界或实体。
- 机器、配方、能源、流体、物流的具体场景以 [content 验收](content.md#验收)为准；本包补充执行顺序、状态机、并行、成本与门控。

## 自动化测试范围

- JUnit
  - 配方成本、并行、输出空间与燃料批次；机器供能、燃烧与状态机。
  - 能源转换、门控与存储状态；物流路由与批量存储。
  - 流体状态、物性与管道安全；材料化学与形态解析。
  - `core/src/test` 覆盖配方、机器、能源、物流、流体与材料等规则；其余为待补范围。
- GameTest
  - 机器 tick、配方执行、多方块、能源传输等运行时场景由 content 的 GameTest 覆盖；本包不重复建用例。
