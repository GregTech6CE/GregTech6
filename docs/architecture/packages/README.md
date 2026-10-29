# 包职责索引

本组文档按职责渐进展开，包含当前源码中的包与代表入口，不逐类列出实现。表内包名通常省略 `gregtech.`；同一包在多个源码层出现时须同时检查。

| 专题 | 包范围 | 下一步 |
| --- | --- | --- |
| [公开 API](api.md) | `api`、`api.addon.*`、Minecraft 的 `api.addon.platform` | 扩展契约、输入校验与 SDK 边界 |
| [执行机制](engine.md) | `engine.*` | 状态、计算、查询、解析与运行时适配 |
| [具体内容](content.md) | `content.*` | 材料、配方、设备、世界与注册目录 |
| [游戏运行时](runtime.md) | `bootstrap`、`client`、`compat`、`event`、`network`、`mixin`、`util`、顶层配置 | 生命周期、游戏对象与横向接入 |
| [平台边界](platforms.md) | `platform`、`platform.forge.*`、`platform.neoforge.*` | 原生能力、加载器入口及版本差异 |

## 修改前的三个判断

1. 是否需要 `Level`、`ItemStack`、加载器事件或第三方 API？需要则不能进公共 api 模块或 core；游戏侧公开协议放内联 API 的 `api/minecraft/src/main`。
2. 是契约、定义、执行，还是展示/接入？据此选择包族；名称有 `Rules` 不一定就是纯 engine，须看实际输入与用途。
3. 两节点已有哪种实现？搜索同名类、引用和初始化入口，防止新增重复类型或只修到一个版本。

## 新包怎么扩展文档

先把新包挂到本索引对应专题，写清所在源码层、应该放什么、不该放什么、代表入口、上下游和验证方法。只有需要更深入解释时才拆子专题，并从父页链接；不要为每个叶子包建立空 README。

这些是新代码的归属标准。历史内容存在迁移遗留时，在改动说明或[决策记录](../../decisions/README.md)解释，不要仅为追求目录整齐进行无关重构。
