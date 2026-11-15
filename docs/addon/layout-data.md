# 布局 JSON 与脚本数据生成

本页维护 L3 布局 overlay 的字段、覆盖规则和 KubeJS 数据生成入口；Java 主控声明见[多方块声明](multiblocks.md)。

返回[附属编写指南](README.md)。

## 覆盖范围与重载

数据包路径为 `data/<namespace>/gt6ce_layouts/<machineType>.json`。它只替换已有且可替换的附属／脚本机器的完整布局；内置机器布局不能覆盖。不注册新的主方块；省略的旧单元、约束和端口规则不会自动继承；省略 `constraints` 表示无约束，省略 `ports` 表示无附加端口限制。重载后，运行时匹配缓存、部件归属和客户端预览一起失效并重建。

| 字段 | 含义 |
| --- | --- |
| `cells` | 固定单元数组。每项有整数 `right/up/back`、`match` 和 `role`；主方块为原点，不能再把原点写成部件。 |
| `match` | 已注册的部件名、方块 ID `namespace:block`、方块标签 `#namespace:tag`，或这些选择的数组；也支持 `air` 等现有匹配条件。 |
| `role` | `casing`、`item_input`、`fluid_output`、`item_fluid_io` 等 `StructureRole` 名称，忽略大小写；普通方块只能使用 `casing`。 |
| `design` | 可选部件外观编号，0–255；还会检查目标部件是否支持该编号。省略表示不改外观。 |
| `repeats` | 重复组数组：唯一的 `key`、锚点 `up`、份数 `min/max`、可选 `step`（1 或 -1）和 `cells`。组内 `up` 相对于锚点，`max=-1` 表示使用宿主的安全上限。 |
| `constraints` | 按实际匹配结果执行的数量与同类方块约束，见下。 |
| 单元的 `ports` | 该单元的接入限制；重复组单元也可声明，见下。 |

`constraints` 支持两种对象：`{"type":"count","match":"steel_wall","min":2,"max":4}` 和 `{"type":"same_block","role":"casing"}`。`match`、`role` 都是可选选择器；同时省略表示所有声明单元，同时提供时取交集。数量包含已匹配的重复层，不统计主方块。`max=-1` 表示无上限。`same_block` 要求选中单元使用相同方块注册身份，并不要求它们的朝向状态相同。未定义的字段、非法范围和无法选中任何声明单元的约束都会报错。

`ports` 的键为 `item_input`、`item_output`、`fluid_input`、`fluid_output`、`energy_input`、`energy_output`。每个键下可写：

- `faces`：相对主方块的 `front/back/left/right/top/bottom` 数组，省略为六面，空数组禁止该通道；受限时无方向能力请求不能绕过限制。
- `slots`：物品或流体聚合接口的零起始索引，空数组或省略表示不额外限制；流体输入槽在前、输出槽在后。槽位越界或主方块不能提供精确槽位访问时会报错。
- `filters`：物品/流体注册 ID 或 `#tag` 字符串数组，数组内取并集，和槽位、面、角色限制取交集；省略或空数组表示不过滤。

未知字段、未知资源 ID 和角色不支持的通道会报错。能源通道不能声明物品/流体的槽位和过滤。`ports` 只收紧角色权限，不能让纯壁板凭空拥有能源接口；同样不能让主方块的输入槽变成输出槽。

下面是可嵌入已有布局单元的字段片段，主方块必须确实拥有对应槽位：

```json
"ports": {
  "item_input": {"faces": ["front"], "slots": [0], "filters": ["minecraft:iron_ingot"]},
  "fluid_output": {"faces": ["back"], "slots": [2], "filters": ["#minecraft:water"]}
}
```

`GT6CE.layout(event, machineType, spec)` 接收完整布局对象或兼容的单元列表，使用和数据包相同的解析器，保留 `constraints` 与单元 `ports`，未知字段会报错。KubeJS 也可以通过目标版本的数据生成事件直接提交上述 JSON；Forge 插件接收 `DataPackEventJS`，NeoForge 插件接收 `KubeDataGenerator`。机器注册阶段的 Java builder 与数据生成阶段的 JSON 是不同入口，不能在配方事件中重新注册主方块。脚本运行仍须使用目标平台匹配的 KubeJS 版本；纯 JSON 解析测试不代表已验证全部脚本加载环境。
