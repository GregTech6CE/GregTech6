# L3：数据包与 KubeJS

本页用于选择可重载的数据入口和脚本入口；机器配方与布局的完整字段分别维护在专题页。

返回[附属编写指南](README.md)。

## 按修改对象选入口

| 目标 | 入口 | 生效时机 |
| --- | --- | --- |
| 追加或移除已有机器配方 | [机器配方数据包](recipe-data.md) | 数据包重载 |
| 让附属自有 RecipeType 进入 overlay | [自定义文档类型](recipe-data.md#让附属自己的数据包配方进入-overlay) | 构造阶段注册类型，重载时读取对象 |
| 替换已有附属或脚本机器布局 | [布局 JSON](layout-data.md) | 数据包重载 |
| 通过脚本声明机器和部件 | 下节 KubeJS | 脚本启动期 |
| 修改机器界面背景 | [GUI 模板](gui-templates.md) | 客户端资源重载 |

纯数据包无需编译 Java 或消费 SDK；只有编写 Java 附属或自定义配方对象时才需要 [L0 工程配置](l0-setup.md)。数据包重载不能补交启动期声明或注册新主控。

## KubeJS

两个节点的 KubeJS 插件都在 `kubejs.plugins.txt` 中声明原生插件。当前公开脚本入口包括：

- `GT6CEStartupEvents.machines`：构造 `RecipeTable`、`RecipeMachine`、`FuelGenerator`、`LayoutPart`、`StructureLayout` 和机器/部件声明。
- `GT6CE.layout(event, machineType, spec)`：生成与 `gt6ce_layouts` 相同的布局 JSON。
- `gt6ce:machine` typed recipe schema：追加配方；`GT6CE.removeMachine(event, spec)` 删除，`GT6CE.clearMap(event, map)` 清空指定表。
- Forge 与 NeoForge 的插件、事件和数据生成适配不同，脚本语法不能无条件跨节点照搬；目标平台必须使用匹配的 KubeJS 版本。

当前 KubeJS 入口只暴露上述声明子集。它没有暴露行为机逻辑、材料声明、世界生成、传感器注册或管道声明的完整 Java SPI，也不替代 L2 的服务端行为工厂。脚本启动和 Java 构造阶段的注册时机不同；脚本数据生成也不证明游戏内配方、机器成型或客户端渲染已经验证。

数据包或 KubeJS 替换参与反推的工作台配方后，重载会根据当前的输入和产出更新对应物品的成分。指定机器的配方增删也在同次重载生效。反推支持固定输入和固定产出的配方。适用范围与回收规则见[物品成分解析](../decisions/material-system/item-composition.md#从当前配方反推成分)。

### 配方 schema

两个平台的 `gt6ce:machine` typed schema 使用与数据包相同的字段名，item 条目支持：

- 字符串：`"minecraft:stone"`、`"#forge:plates/steel"`；
- 对象：`{item, tag, material, form, count}`，其中身份三选一；输出还可带 `chance`；
- 平台原生物品栈或输出对象。

追加走 typed schema；移除用 `GT6CE.removeMachine(event, spec)`，整表清空用 `GT6CE.clearMap(event, map)`。脚本仍须使用目标平台匹配的 KubeJS 版本；两节点的序列化实现不同，不能照搬另一版本的语法。

### 开发运行与示例

开发运行用 `-PkubejsRuntime=true` 加载对应原生运行依赖；插件入口由各节点 `kubejs.plugins.txt` 声明。先看目标节点的插件、事件与声明处理代码，再决定脚本形态，不能照搬另一 MC 版本的语法。

运行入口和开关见[游戏内验收](../development/validation/checks/manual-ingame.md#运行命令)。脚本语法与能力以目标节点实现为准；片段与完整样例的验证边界见[兼容性与能力边界](compatibility.md#示例状态)。
