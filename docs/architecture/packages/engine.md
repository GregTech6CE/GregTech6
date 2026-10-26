# engine：执行、状态与算法

返回[包索引](README.md)。先读 core 中的规则，再查 Minecraft 层如何喂入世界/栈数据；不要从 BlockEntity 的全部细节反推公共契约。

## core 中各包的职责

入口为 [core/gregtech/engine](../../../core/src/main/java/gregtech/engine)。这里可以保存状态、注册数据和执行计算，但不能依赖游戏类型。

| 包 | 应当承担 | 代表入口/边界 |
| --- | --- | --- |
| `engine` | 执行层总体说明 | `package-info.java`；不继续使用旧 internal 命名扩张实现 |
| `engine.addon` | 附属声明收集、校验、物化与共享服务绑定 | `AddonCoreServices`、`AddonMaterials`、`AddonMachines`、`AddonPipes` |
| `engine.definition` | 定义目录的组织与查询机制 | `DefinitionCatalog`；内容行放 content |
| `engine.material` | 材料注册、构造、化学关系、物质成分和角色规则 | `GTMaterialRegistry`、`MaterialFactories`、`MaterialChemistry` |
| `engine.prefix` | 材料形态/前缀的注册与解析 | `PrefixRegistry`、`BlockPrefixRegistry` |
| `engine.recipe` | 配方成本、燃料批次、可合成性和界面坐标等共享机制 | `MachineWorkCost`、`FluidFuelChargeMath`；不能夹带游戏栈 |
| `engine.recipe.data` | 配方文档模型、组件、解析及校验 | `MachineRecipeDocuments`、`ItemComponent`、`FluidComponent` |
| `engine.machine` | 通用机器规格、供能、燃烧与执行规则 | `BasicMachineEnergy`、`BoilerSpec`、`BurningBoxFuelRules` |
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
| `engine.machine`、`engine.machine.crucible` | 机器基类、相邻能力缓存、控制和坩埚物品输入 |
| `engine.material` | ItemStack 的材料归属、等价、统一和表现绑定 |
| `engine.multiblock` | 世界结构、端口归属、工具目标和机器连接 |
| `engine.multiblock.tracking` | 结构模式编译、足迹、世界变化跟踪、聚合及客户端成形同步 |
| `engine.recipe` | 游戏配方对象、输入匹配、查询表、产出与 GUI 布局 |
| `engine.sensor` | 将游戏物品质量、机器进度等转为传感器源 |
| `engine.tool` | 游戏工具类型、附魔、交互目标与实际操作适配 |

版本节点另有同包补充，如 NeoForge 的 `engine.transport` 原生运输接入；检查[平台指南](platforms.md)及两个节点的实际类，不把共享包直接搬进 core。

## 修改时的约束

纯算法接收明确数值/值对象，说明单位、上下限、溢出和模拟/实际执行语义；不要把能源包、流体量、材料量都用无说明的“数量”混用。库存或资源转移需验证失败路径和守恒，世界网络需考虑邻接变化与区块卸载。

新增跨 tick 状态时明确谁持有、谁保存、何时失效；避免不受控静态集合持有世界或实体。共享规则的回归优先在 core 验证；世界适配还需双平台编译与相关游戏场景，参见[验证指南](../../development/validation.md)。
