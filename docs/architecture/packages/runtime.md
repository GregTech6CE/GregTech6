# 游戏运行时：装配、展示与接入

返回[包索引](README.md)。这里说明领域代码之外的横向包，以及 Block/BlockEntity 与规则层的连接。入口位于共享 [minecraft/gregtech](../../../minecraft/src/main/java/gregtech)及两个版本节点；core 另有少量纯契约和工具。

## 生命周期与横向包

| 包/入口 | 应当承担 | 不能成为 |
| --- | --- | --- |
| `gregtech.GregTechConfig` | 配置规格、目录准备与参数访问 | 动态修改注册表的通用入口 |
| Forge 的 `gregtech.GregTech` | Forge 模组入口、绑定与阶段装配 | 具体机器每 tick 的执行器 |
| NeoForge 的 `platform.neoforge.GregTechNeoForge` | NeoForge 模组入口与原生装配 | 从共享代码直接调用的跨平台门面 |
| `bootstrap` | core 的 `InitializationStep` 初始化步骤契约 | 与加载器耦合的 API |
| `bootstrap.content` | 内容初始化装配，如 `DataBootstrap`、`MaterialBootstrap`、`BlockBootstrap` | 新的内容目录副本 |
| `bootstrap.registration` | 材料链接、延迟注册提交、机器及创造栏装配 | 在任意时刻可重复调用的注册工具 |
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

`client.gui.GuiTemplates` 加载客户端 GUI 模板与碎片图标，`GuiTemplate` 组合背景、槽位和进度条；`RecipeGuiLayout` 为 JEI/EMI 计算共用紧凑布局。资源重载只影响展示，真实槽位和同步仍由 Menu 决定。格式与扩展入口见[GUI 开发指南](../../development/gui-templates.md)。

先绑定平台服务与模组存在性，再加载依赖这些能力的材料和内容；注册依赖对象后在正确生命周期装配配方与附属回调。Forge 入口已有按阶段执行的步骤，NeoForge 入口包含原生注册序列。调整任何步骤都应读完对应入口的上下文，不凭方法名排序。

`SetupOnce` 之类控制的是执行次数，不是所有初始化天然可交换的证明；静态字段首次访问也可能触发加载。重构时核对材料、前缀、流体、物品、机器和配方的依赖链。

## 世界对象如何接规则

Block 处理放置、形状和交互分发；BlockEntity 保存实例状态、执行 tick、持久化和邻接连接。将可独立表达的材料/耗能/配方规则交给 engine，将设备参数交给 content 定义，再将需要展示的状态同步到客户端。现有具体类可能承担更多历史职责，修改时逐步收敛。

持久数据与网络展示数据分别考虑：变更 NBT/数据组件要检查读旧数据、缺字段、范围与版本差异；变更包协议要检查方向、解码上限、执行线程、目标实体是否有效及请求者是否有权限。

## 客户端与可选集成

公用入口不能在专用服务端加载 `net.minecraft.client` 类型。模型、Screen 和渲染初始化要沿已有客户端入口接入；不要把视觉修正顺手改成服务端规则。

当前 `client.gui` 也包含 `BasicMachineContainerMenu`、`MachineMenuTransfers`、`SlotInput` 等双端使用的容器协议/逻辑。不能只因路径带 `client` 就把整个包限为客户端加载；逐类区分 Screen 与 Menu/Slot，后者仍须在服务端校验库存操作。

第三方集成沿配置与现有入口条件加载，`compileOnly` 只能说明编译依赖形态，不能自动保证类加载安全。分别测试“没有该模组”和“安装匹配版本”。Mixin 还需核对两个节点配置、目标签名、注入点与未命中时的行为。

验证路线见[验证指南](../../development/validation.md)，平台差异见[平台包](platforms.md)。客户端画面正常不代表专用服务端可以启动，编译成功也不证明 Mixin 已正确应用。
