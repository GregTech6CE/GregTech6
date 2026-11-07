# 共享门面

返回[平台边界](../platforms.md)。共享门面是共享代码与节点实现之间的版本中立契约。

## 职责

共享门面是共享代码使用的版本中立 API，也是两个节点实现共同遵守的契约。共享代码通过门面使用加载器/版本能力，不直接 import 节点类；差异处理方式见[跨版本指南](../../versions.md)的“区分与归属”。

## 代码位置

共享门面位于共享输入，不单独成层目录：

| 门面 | 所在包 | 职责 | 吸收的差异 |
| --- | --- | --- | --- |
| `GTToolHelper` | `engine.tool` | 工具谓词、耐久、采集规则 | NBT 与数据组件、电动工具类型 |
| `CoverStackData` | `content.cover` | 覆盖板栈数据读写 | NBT 与数据组件 |
| `ContentRegistrar` | `content.registry` | 加载器中立的物品/方块注册视图 | 注册 API |

表中是代表入口；完整判断标准见[跨版本指南](../../versions.md)的“区分与归属”。

`Mc` 是平台 shim；共享代码当前使用其 `logger()`，注册表辅助方法暂无调用点。

`GTBlocks`/`GTItems` 是共享输入中的原生注册表，公开字段带加载器类型；共享代码可直接读取，新增代码优先通过 `ContentRegistrar`。

## 设计准则

- 新增门面的公开签名只出现版本中立类型，不出现加载器类型；`GTBlocks`/`GTItems` 是已知例外。
- 同名方法在两个版本必须同义；语义不同就改名，或在调用点显式选择。
- 不写纯转发：门面方法必须做适配或有明确契约。
- 新增门面必须有调用点；没有调用点的门面不构成新代码的边界。
- 不维护第二套业务规则；需要核心规则时调用 core/共享 Minecraft 层。
- 需要外部可调用桥接时，使用 `api.addon.platform` 契约及 `engine.addon` 绑定，不扩张门面职责。

## 验收

- 契约测试覆盖版本中立行为。
- 双平台编译通过；新增门面的公开签名无加载器类型（`GTBlocks`/`GTItems` 例外）。
- 同名方法的输入/输出语义一致；调用点覆盖。
- 新增门面应补契约测试；没有测试覆盖时需说明原因。

## 自动化测试范围

- API 契约 JUnit：`api/minecraft/src/test`（`:api-platform`）。
- 共享规则 JUnit：`core/src/test`、`api/common/src/test`。
- 共享 GameTest：`minecraft/src/test`，通过 `runGameTestServer` 运行。
- 现有覆盖：`minecraft/src/test` 的 GameTest 与 `api/minecraft/src/test` 的 addon API JUnit；门面契约测试为待补。
- 应覆盖：版本中立行为、同名方法语义、输入/输出契约、序列化往返。
- 门槛：新增门面必须有契约测试；涉及注册/能力/数据组件的门面补对应节点 GameTest。
