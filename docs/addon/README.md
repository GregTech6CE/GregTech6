# 附属编写指南

本目录面向 GT6CE Java 附属、数据包和 KubeJS 作者，按扩展入口与执行时机分层。先选入口，再进入对应层和专题；字段、格式与边界随各专题维护。源码分层和宿主实现职责见 [API 包职责](../architecture/packages/api.md)。

## 阅读路径

| 目标 | 阅读顺序 |
| --- | --- |
| 第一次编写 Java 附属 | [L0 工程与发现](l0-setup.md) → [L1 声明](l1-declarations.md) → [开发流程与验证](validation.md) |
| 新增材料与加工规则 | L0 → [材料专题](materials.md) → [L5 材料目录](l5-query-and-diagnostics.md#材料目录) |
| 新增机器行为 | L0 → L1 → [多方块与端口](multiblocks.md)（按需）→ [L2 行为](l2-behavior.md) → [GUI](gui-templates.md) |
| 只追加或移除配方 | [L3 数据与脚本](l3-data-and-scripts.md) → [配方字段](recipe-data.md) → 验证 |
| 修改已有附属机器布局 | L3 → [布局 JSON](layout-data.md) → [预览与交互](multiblocks.md#预览与部件交互) |
| 让已有方块接入 GT 网络或传感器 | L0 → [L4 互操作](l4-interop.md) → [L5 诊断](l5-query-and-diagnostics.md) |
| 升级或排错 | [兼容性与能力边界](compatibility.md) → 对应专题 → [验证清单](validation.md#验证清单) |

## 分层模型

分层表示扩展入口的形态和执行时机，不是 Java 继承层级，也不是必须全部使用的功能栈。一个附属可以只使用 L1，也可以同时使用 L1、L2 和数据包。

| 层 | 解决的问题 | 主要形态 | 典型入口 | 执行侧与时机 |
| --- | --- | --- | --- | --- |
| [L0：工程、发现与生命周期](l0-setup.md) | 工程依赖、附属身份、生命周期和发现 | 模组入口、`GregTechAddon`、`ServiceLoader` | `GregTechAddons`、`GregTechAddon`、各 SPI | 模组构造和启动期；注册关闭后不能补交 |
| [L1：声明式内容](l1-declarations.md) | 把材料、机器、配方表、结构、部件和世界生成交给宿主注册 | 纯数据声明 | `AddonMaterialContributor`、`AddonMachineContributor`、`RecipeTable`、`MultiblockMachine` | 材料和机器初始化阶段；返回声明，不直接注册方块 |
| [L2：运行时行为](l2-behavior.md) | 实现新机器的加工、状态、交互和服务端/客户端表现 | 平台行为回调 | `MachineBehavior`、`PartBehavior`、`BehaviorMachineClient`、`AddonRecipes` | 行为实例按方块实体创建；配方操作在宿主就绪后；上下文只在回调期间有效 |
| [L3：数据包与 KubeJS](l3-data-and-scripts.md) | 在不重新编译 Java 的情况下追加或替换已有数据 | 数据包、KubeJS、配方文档 | `gt6ce:machine`、`gt6ce:machine_removal`、`gt6ce_layouts`、KubeJS 事件 | 数据包重载或脚本启动阶段；遵循各自的覆盖和优先级规则 |
| [L4：互操作协议](l4-interop.md) | 让附属自己的方块、方块实体或配方对象与 GT6CE 的能源、传感、管道、控制或配方 overlay 互通 | Opt-in 接口 | `EnergyBlock`、`GtSignalSource`、`MachineControl.Provider`、`ItemPipeConnectable` | 由 GT 现有系统在运行时按方块实体或配方对象识别 |
| [L5：查询与诊断](l5-query-and-diagnostics.md) | 查询宿主已经发布的内容并诊断失败 | 只读目录、查询和诊断方法 | `GregTechMaterials.catalog()`、`AddonRecipes.find`、`AddonStructures.diagnose` | 宿主就绪后；诊断是显式扫描或快照读取，不是每 tick 的隐式操作 |

## 按目标选择入口

按目标优先选择数据包或宿主已有的声明入口；只有宿主声明的处理器无法表达的逻辑才进入 L2。

| 需求 | 首选入口 | 补充说明 |
| --- | --- | --- |
| 新增材料、前缀、材料 metadata 或材料加工规则 | `AddonMaterialContributor` + `MaterialDeclaration` / `MaterialInfoTool.Registrar` | 新代码使用 `materialInfo(Registrar)`；不要继续扩展旧的 `forms()`、`compositions()` 等兼容方法 |
| 新增机器配方表或燃料表 | `RecipeTable.machine()` / `RecipeTable.fuel()` | 通过 `AddonMachineContributor.recipeTables()` 提交 |
| 新增由现有配方表驱动的单方块或大型加工机 | `RecipeMachine` | 机器处理循环由宿主负责；需要宿主没有的状态机时改用行为机 |
| 新增使用固定燃料表的发电机 | `FuelGenerator` + `FuelOutput` | `RecipeTable.fuel()` 与 `RecipeMachine` 是不同入口 |
| 新机器需要自定义 tick、状态、交互或结构后处理 | `BehaviorMachine` + `BehaviorMachineContributor` | 行为实例按已放置方块实体创建；需要声明与平台工厂一一对应 |
| 多方块主控 | `controller.withStructure(layout)` | 配方机必须使用 `largeRecipe()`，燃料机必须使用 `multi()` |
| 功能部件或接入端口 | `LayoutPart` + `LayoutPartContributor` | 纯外壳或只带能源能力的部件不需要部件行为工厂；有槽位、tick、点击或字段的部件需要 |
| 新物品管道或流体管道材料 | `ItemPipeDeclaration` / `FluidPipeDeclaration` | 宿主按内置尺寸展开；`idSuffix` 不能与已有管道重名 |
| 大体矿脉或小矿 | `OreVeinDeclaration` / `SmallOreDeclaration` | 通过材料贡献入口提交；自定义维度还要求宿主已有对应生成特性 |
| 只追加或删除已有机器的配方 | 数据包或 `AddonRecipes` | 数据包可重载；Java 入口适合启动期和附属自有配方对象 |
| 让已有方块接受 GT 能源、管道、传感器或控制 | L4 的 opt-in 接口 | 实现自己的方块或方块实体，不要伪造 GT 内部类型 |
| 让附属自有配方类型进入 `gt6ce:machine` overlay | `MachineRecipeDocumentType` + `MachineRecipeDocumentProvider` | 还需要目标加载器的配方类型和序列化器 |
| KubeJS 声明机器或布局 | `GT6CEStartupEvents.machines`、`GT6CE.layout` | 当前只覆盖脚本暴露的声明子集，不覆盖行为逻辑 |

## 专题索引

| 专题 | 维护内容 |
| --- | --- |
| [材料与加工规则](materials.md) | 材料声明、所有权、metadata、tag、分解与特殊处理 |
| [多方块声明与端口](multiblocks.md) | 显式绑定、约束、端口、结构回调、主动输出与预览 |
| [机器配方数据包](recipe-data.md) | JSON 字段、移除、优先级、canonical JSON 与自定义 RecipeType |
| [布局 JSON](layout-data.md) | 可替换范围、重复组、约束、端口和脚本数据生成 |
| [GUI 模板](gui-templates.md) | 客户端模板、碎片贴图、Screen 和 JEI/EMI |
| [开发流程与验证](validation.md) | 实施顺序、检查入口与未覆盖场景 |
| [兼容性与能力边界](compatibility.md) | 内部接口、旧入口、现有限制和示例状态 |

## 相关指南

[开发上手](../development/getting-started.md) · [跨版本要求](../architecture/versions.md) · [资源与生成内容](../architecture/resources.md) · [仓库验证与验收](../development/validation/README.md)。独立 API 的版本、坐标和构建命令见 [L0](l0-setup.md#独立库与-sdk-构建)。
