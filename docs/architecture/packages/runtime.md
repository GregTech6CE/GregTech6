# 游戏运行时：装配、展示与接入

返回[包索引](README.md)。这里说明领域代码之外的横向包，以及 Block/BlockEntity 与规则层的连接。入口位于共享 [minecraft/gregtech](../../../minecraft/src/main/java/gregtech)及两个版本节点；core 另有少量纯契约和工具。

## 生命周期与横向包

| 包/入口 | 应当承担 | 不能成为 |
| --- | --- | --- |
| `gregtech.GregTechConfig` | 配置规格、目录准备与参数访问 | 动态修改注册表的通用入口 |
| Forge 的 `gregtech.GregTech` | Forge 模组入口、绑定与阶段装配 | 具体机器每 tick 的执行器 |
| NeoForge 的 `platform.neoforge.GregTechNeoForge` | NeoForge 模组入口与原生装配 | 从共享代码直接调用的跨平台门面 |
| `bootstrap` | core 的 `InitializationStep` 初始化步骤契约 | 与加载器耦合的 API |
| `bootstrap.ContentRegistration` | 共享内容家族的固定顺序与平台专属 hook | 直接持有加载器注册类型 |
| `bootstrap.content` | 内容初始化装配，如 `DataBootstrap`、`MaterialBootstrap`、`BlockBootstrap` | 新的内容目录副本 |
| `bootstrap.registration` | 创造栏内容分类、原始顺序与材料链接等装配辅助 | 在任意时刻可重复调用的注册工具 |
| `client` | 模型、渲染、颜色、tooltip、预览及客户端同步消费 | 服务端规则与权威库存状态 |
| `client.gui` | Screen、控件，以及既有 Menu、Slot 和转移逻辑 | 直接批准客户端请求的业务控制器 |
| `network` | 数据包定义、注册、编解码、发送和状态同步 | 绕过服务端权限的命令入口 |
| `event` | 原生事件订阅、筛选和转发 | 与事件无关的大段领域算法 |
| `mixin` | 必要的原版/外部行为注入与访问器 | 有原生 API 时仍优先采用的替代方案 |
| `compat.common` | 可选集成共用的展示/接入辅助 | 令所有第三方类在基础启动时加载的集中入口 |
| `compat.jei`、`compat.emi` | 两类配方查看器的分类、展示、可用配方与结构预览 | 决定服务端真实配方是否存在的来源 |
| `compat.jade` | 机器与方块状态的服务端数据采集、客户端展示 | 无权限访问内部状态的通道 |
| 节点 `compat.kubejs` | 原生 KubeJS 插件、事件和脚本类型适配 | core 对 KubeJS 的直接依赖 |
| `util` | core 的格式化、缓存、预览计算；游戏层的掉落、放置和物料等既有工具 | 新领域业务的默认堆放处 |

## 装配顺序

先绑定平台服务与模组存在性，再加载依赖这些能力的材料和内容；注册依赖对象后在正确生命周期装配配方与附属回调。Forge 入口已有按阶段执行的步骤，NeoForge 入口包含原生注册序列。调整任何步骤都应读完对应入口的上下文，不凭方法名排序。

`SetupOnce` 之类控制的是执行次数，不是所有初始化天然可交换的证明；静态字段首次访问也可能触发加载。重构时核对材料、前缀、流体、物品、机器和配方的依赖链。

## 内容注册

`bootstrap.ContentRegistration` 是共享内容家族的唯一顺序：物品 → 方块 → 平台 `blocks` hook → 机器家族 → 平台 `machines` hook → 附属多方块部件 → 创造栏。物品和方块家族只提供一个共享入口（通常是 `register(ContentRegistrar)`），通过 `RegisteredItem` / `RegisteredBlock` 句柄取结果，不直接声明物品或方块 `DeferredRegister`，也不接收 `IEventBus`；创造栏、声音、流体和结构等非物品注册仍由各自的共享注册表持有。

`ContentRegistrar.shared()` 是唯一的适配器实现：内置命名空间落到 `GTItems.ITEMS`、`GTBlocks.BLOCKS` 和 `GTBlocks.BLOCK_ITEMS`，附属命名空间按 namespace 取独立注册表，不并入 `gt6ce`。平台入口只做三件事：绑定平台服务与前置数据、通过 `ContentRegistration.PlatformContent` 的 `blocks` / `machines` 位置追加平台专属家族、在最后调用一次 `submit()` 提交所有延迟注册表。`submit()` 是唯一的提交点；家族方法只排队，不自带 `IEventBus` 重载，也不调用 `DeferredRegister#register(bus)`。附属命名空间的方块实体表、Menu、能力、声音、网络和其他原生注册仍留在版本层，由各平台入口在同一个 `submit()` 中集中提交。

平台内部装载适配器（Forge 的 `MachineRegistry` / `GTMachines`、`GTBlockEntities`、`GTFluids` 等非物品注册表）仍需使用平台注册类型，它们属于版本层的适配代码，不受“内容家族只使用 `ContentRegistrar`”约束；共享内容家族不得绕过适配器直接写 `GTBlocks` / `GTItems`。

新增内容家族时：

1. 在定义层（`core` 或共享 `minecraft/src`）提供数据与工厂；
2. 家族类实现唯一的共享注册入口，只使用 `ContentRegistrar`；
3. 把入口加到 `ContentRegistration` 的对应阶段；平台专属家族加到该平台的 `PlatformContent` hook；
4. 确实需要新的注册对象类型（方块实体、Menu、能力等）时，只扩展平台适配层，不复制共享顺序。

## 世界对象如何接规则

Block 处理放置、形状和交互分发；BlockEntity 保存实例状态、执行 tick、持久化和邻接连接。将可独立表达的材料/耗能/配方规则交给 engine，将设备参数交给 content 定义，再将需要展示的状态同步到客户端。现有具体类可能承担更多历史职责，修改时逐步收敛。

持久数据与网络展示数据分别考虑：变更 NBT/数据组件要检查读旧数据、缺字段、范围与版本差异；包协议的检查项见下方验收。

## 运输网络的快照与调度

`ItemPipeNetwork`/`ItemPipeNetworks`、`ElectricNetwork`/`ElectricNetworks` 与 `FluidPipeTickPhases` 共用一套骨架：

- 组件快照按“双向连接 + `hasChunkAt` + `isRemoved`”建立；成员共享一个 `Membership`，`invalidate()` 为 O(1)，下一次需要时重建。
- 每 level 一个 `State`（`WeakHashMap`）加 `LevelTickEvent`；这些方块实体不注册逐方块 ticker。
- 快照记录 chunk liveness，遍历或投递前按 chunk 检查一次；chunk 消失即整体失效。
- 生命周期挂点固定：`onLoad` → `joined`，`setRemoved`/`onChunkUnloaded` → `left`，连接位变化 → `invalidate`/唤醒。

电网的 `lastWattage`、`transferredAmperes` 与烧毁冷却按 level tick 窗口惰性推导，读取时计算且不持久化；烧毁由 `ElectricNetworks` 的每 tick 队列执行。流体管道空闲调度只决定是否执行 `serverTick`，保留 `FluidPipeTickPhases` 的 `PENDING` 顺序与两相位。新增放置、破坏、扳手、覆盖板或区块加载路径时，必须接到对应的失效或唤醒钩子；漏接会表现为陈旧投递或休眠管道不分配。

## 客户端与可选集成

公用入口不能在专用服务端加载 `net.minecraft.client` 类型。模型、Screen 和渲染初始化要沿已有客户端入口接入；不要把视觉修正顺手改成服务端规则。

当前 `client.gui` 也包含 `BasicMachineContainerMenu`、`MachineMenuTransfers`、`SlotInput` 等双端使用的容器协议/逻辑。不能只因路径带 `client` 就把整个包限为客户端加载；逐类区分 Screen 与 Menu/Slot，后者仍须在服务端校验库存操作。

GUI 模板、碎片图标与 JEI/EMI 的布局接入见[GUI 开发指南](../../development/gui-templates.md)，平台差异见[平台包](platforms.md)。

第三方集成沿配置与现有入口条件加载，`compileOnly` 只说明编译依赖形态，不能保证类加载安全。

## 验收

- 初始化改动核对[装配顺序](#装配顺序)与[内容注册](#内容注册)，包括静态字段首次访问引起的加载。
- 客户端隔离按[上述边界](#客户端与可选集成)检查，区分 Screen 与双端 Menu/Slot。
- 客户端只负责展示与交互入口；真实槽位、库存、权限与配方由服务端 Menu 和服务端状态决定。修改 GUI 模板后先做静态预览，再在客户端与专用服务端分别确认。
- 网络包检查方向、解码上限、执行线程、目标实体有效性与请求者权限；Mixin 检查两个节点的配置、目标签名、注入点与未命中行为。
- 可选集成沿配置与现有入口条件加载；分别验证未安装目标模组与安装匹配版本。
- 运输网络快照只在布局变化时失效，空闲电线/管道不执行 tick；外部 fill/drain、覆盖板、扳手、邻居变化和区块加载都必须接到失效或唤醒钩子。
- 共享 Minecraft 输入遵循 Stonecutter 条件与版本资源路径；渲染、GUI、网络与 Mixin 的结论不能用单平台结果推出另一平台。

## 自动化测试范围

- JUnit
  - 附属声明的收集、校验与冲突；KubeJS 纯声明规则；配置解析与默认值。
  - 网络包编解码 roundtrip；注册 ID 唯一性；GUI 模板与别名 JSON 的结构与引用检查。
  - 语言键完整性（`en_us` 与 `zh_cn` 的新增、缺失、重复键）。
- GameTest
  - 注册存在性与基础方块实体交互；Menu/Slot 的服务端侧交互与库存校验。
  - 渲染、Screen、模型、纹理、JEI/EMI/Jade 显示不纳入自动测试。
- 现有测试集
  - `api/common/src/test` 的附属声明测试与 `api/minecraft/src/test` 的平台契约测试；`BehaviorMachineGuiTest` 覆盖 GUI 几何与 UV 常量。
  - 客户端渲染、GUI 外观、真实客户端-服务端同步与 Mixin 保留人工验收。
