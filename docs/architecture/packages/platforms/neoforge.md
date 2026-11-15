# NeoForge 实现（参考）

NeoForge 节点是当前参考实现，Forge 对齐此行为。

## 代码位置

- 节点源：`minecraft/versions/1.21.1-neoforge/src/main/java`
- 平台包：`gregtech.platform.neoforge.*`
- 节点包：`bootstrap`、`content`、`engine`、`client`、`network`、`mixin`、`compat.kubejs`。节点侧同名包只放加载器专属入口，共享同名包仍在共享输入。

| 包/入口 | 职责 |
| --- | --- |
| `platform.neoforge` | `GregTechNeoForge` 生命周期入口、`StackCustomData` 数据适配 |
| `platform.neoforge.energy` | 引擎、轴、激光、电线的能力与工具接入 |
| `platform.neoforge.fluid` | 原生流体注册、能力、流体展示物品 |
| `platform.neoforge.logistics` | 书架、存储、合成台、长距物流能力与原生注册 |
| `platform.neoforge.machine` | 基础/行为机器、附属传感器、压缩气罐注册与原生物品接入 |
| `platform.neoforge.smeltery` | 原生冶炼方块、实体、模具和物品能力 |
| `platform.neoforge.transport` | 流体管环境、工具操作与容器交互 |
| `platform.neoforge.registry` | `NeoContent`、`RetiredStoneAliases` 等注册入口 |
| `platform.neoforge.enchantment` | `GTEnchantmentEffectTypes` 等附魔效果类型 |

共享门面契约见[共享门面](facade.md)；表中是代表入口，完整包列表以源码为准。工具判定与损耗统一走 `GTToolHelper`，节点侧不再保留转发层；共享输入不得直接引用节点类，按[跨版本指南](../../versions.md)的“已知需要单独检查的边界”处理。

## 设计准则

- 直接调用 NeoForge API：注册、capability、事件、数据组件、网络。
- 实现共享门面的契约；不把 NeoForge 类型放进共享门面的公开签名。
- 作为参考实现：行为、注册 ID、capability 方向、序列化格式以 NeoForge 为准。
- 不复制共享业务规则；需要规则时调用 core/共享 Minecraft 层。
- 节点专属行为需记录，便于 Forge 对齐。

## 验收

- 参考场景通过（GameTest 或等价验证）。
- 注册 ID、capability 方向、模拟/实际资源转移、序列化、生命周期时机、客户端/专用服务端隔离逐项检查。
- 共享门面契约以 NeoForge 行为作为基准；长期平台级取舍与差异记录到[跨平台决策与差异](../../../decisions/platform/README.md)。
- 发行产物、数据生成与版本资源路径按[组装检查](../../../development/validation/checks/assemble.md)与[资源指南](../../resources.md)检查；版本元数据与 Mixin 配置按节点检查。
- 只验证 NeoForge 时，明确写出 Forge 未覆盖。

## 自动化测试范围

- 共享 JUnit/GameTest 见[共享门面](facade.md#自动化测试范围)。
- 现有覆盖：共享 GameTest 与 addon API JUnit；节点专属测试与门面契约测试为待补。
- 节点 GameTest：`minecraft/versions/1.21.1-neoforge/src/test` 当前不存在；仅在加载器 API 差异需要时创建。
- 应覆盖：注册存在性、capability 方向、方块实体 tick、传输、序列化、客户端初始化隔离。
- 门槛：参考场景通过；新增平台能力补对应 GameTest 或说明无法自动化的原因。
