# 平台包：原生 API 的边界

返回[包索引](README.md)。平台代码应把加载器/版本差异变成明确的适配，不维护第二套材料、机器算法或配方事实。下面列出当前实际包，包多寡不代表两个平台的功能等价程度。

## 当前入口

| 包/源码层 | 职责与入口 |
| --- | --- |
| 共享 `platform` | `Mc`：资源标识、注册查询、栈数据等小范围版本门面，内部保留 Stonecutter 条件 |
| Forge 节点 `platform.forge.transport` | Forge 原生运输相关能力/接入 |
| NeoForge 节点 `platform.neoforge` | `GregTechNeoForge` 生命周期入口，`StackCustomData` 数据适配和 `NeoToolBindings` 工具绑定 |
| `platform.neoforge.energy` | 引擎、轴、激光、电线的能力与工具接入 |
| `platform.neoforge.fluid` | 原生流体注册、能力、流体展示物品 |
| `platform.neoforge.logistics` | 书架、存储、合成台、长距物流等能力与原生注册 |
| `platform.neoforge.machine` | 基础/行为机器、附属传感器、压缩气罐的注册与原生物品接入 |
| `platform.neoforge.smeltery` | 原生冶炼方块、实体、模具和物品能力等适配 |
| `platform.neoforge.transport` | 流体管环境、工具操作与容器交互适配 |

Forge 的大量原生实现当前仍位于节点的 `bootstrap`、`content`、`engine`、`client`、`network` 和 `compat.kubejs`，而非全部集中在 `platform.forge`。NeoForge 也有同类节点包。定位差异须搜索两个节点，不能只搜索 platform。

## 放哪里与如何演进

小范围方法/签名差异优先沿共享输入已有 Stonecutter 分支处理；若整类需要不同的原生生命周期、能力或注册 API，再放版本节点。新适配保持输入/输出语义一致，差异本身写清楚，不从 NeoForge 实现反向调用 Forge 类型。

需要核心规则时调用 core/共享 Minecraft 层。需要外部可调用桥接时，考虑现有 `api.addon.platform` 契约及 `engine.addon` 绑定，不为一个局部差异扩张巨大万能门面。

## 验证

检查两个节点相同功能的初始化时机、注册 ID、能力方向、模拟/实际资源转移、序列化与客户端隔离。共享契约改动进行双平台编译；平台运行期行为分别验证并记录证据。代码结构相似不等于行为一致，结论归档到 [integration](../../integration/README.md)。
