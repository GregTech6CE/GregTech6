# Forge 实现（对齐 NeoForge）

Forge 节点对齐 NeoForge 参考实现，以 NeoForge 行为定义共享门面；保留偏差时须记录并验证。

## 代码位置

- 节点源：`minecraft/versions/1.20.1-forge/src/main/java`
- 平台包：`gregtech.platform.forge.*`
- 节点包：`bootstrap`、`content`、`engine`、`client`、`network`、`event`、`mixin`、`compat.kubejs`。节点侧同名包只放加载器专属入口，共享同名包仍在共享输入。

| 包/入口 | 职责 | 对应 NeoForge 入口 |
| --- | --- | --- |
| `GregTech.java` | Forge 模组入口 | `platform.neoforge.GregTechNeoForge` |
| `platform.forge.ForgeContent` | Forge 内容注册适配 | `platform.neoforge.registry.NeoContent` |
| `platform.forge.transport` | Forge 原生运输能力/接入 | `platform.neoforge.transport` |
| `bootstrap` | Forge 生命周期与注册入口 | `platform.neoforge.GregTechNeoForge` 及节点入口 |
| `content` | 内容物化与节点补充 | NeoForge 节点 `content` |
| `engine` | 执行机制的节点补充 | NeoForge 节点 `engine` |
| `client` | 客户端渲染、模型、事件 | NeoForge 节点 `client` |
| `network` | 网络包与协议 | NeoForge 节点 `network` |
| `event` | Forge 事件接入 | `platform.neoforge` 中的事件注册 |
| `mixin` | Forge Mixin 配置与注入 | NeoForge 节点 `mixin` |
| `compat.kubejs` | KubeJS 兼容 | NeoForge 节点 `compat.kubejs` |

表中只表示职责对应，不表示文件一一对应。Forge 的原生实现还分布在节点其他包，见[平台边界](../platforms.md)。

## 设计准则

- 对齐 NeoForge 的共享门面契约、注册 ID、capability 方向、序列化格式和生命周期时机。
- 使用 Forge API 实现，不复制共享业务规则。
- 偏差必须写明原因、影响范围和验证方式；不能用 Forge 行为反向定义共享契约。
- 共享代码不能无条件使用只在 Java 21 可用的特性；Forge 节点使用 Java 17。

## 验收

- 与 NeoForge 相同场景通过（GameTest 或等价验证）；行为差异有记录、有对应验证。
- 注册 ID、capability 方向、模拟/实际资源转移、序列化、生命周期时机、客户端/专用服务端隔离逐项检查。
- 数据包路径按 1.20.1 检查，不能与 1.21.1 互拷。
- 发行产物、数据生成与版本资源路径按[组装检查](../../../development/validation/checks/assemble.md)与[资源指南](../../resources.md)检查；版本元数据与 Mixin 配置按节点检查。
- 只验证 Forge 时，明确写出 NeoForge 未覆盖。

## 自动化测试范围

- 共享 JUnit/GameTest 见[共享门面](facade.md#自动化测试范围)。
- 现有覆盖：共享 GameTest 与 addon API JUnit；节点专属测试为待补。
- 节点 GameTest：`minecraft/versions/1.20.1-forge/src/test` 当前无节点专属测试文件（目录未纳入版本控制）；仅在加载器 API 差异需要时补充。
- 应覆盖：与 NeoForge 参考场景相同；无法运行同一场景时，写明原因并补等价检查。
- 门槛：对齐场景通过；偏差有记录；新增平台能力补对应 GameTest 或说明无法自动化的原因。
