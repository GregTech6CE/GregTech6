# 架构：先选源码层，再选职责包

本项目有两个独立维度：**源码层决定能依赖哪些平台类型，Java 包决定承担什么职责**。`gregtech.content` 或 `gregtech.engine` 可能同时存在于 core、共享 Minecraft 和版本节点，包名相同不代表实现层相同。

## 第一层：源码与依赖

| 源码层 | 职责 | 允许的方向 |
| --- | --- | --- |
| `api/common`（内联自 GregTech6API） | Java 17 公共协议、模型、声明与宿主服务接口 | 仅依赖 JDK；主库接为 `:api` |
| `core` | 纯 Java 内容定义、规则、状态与共享资源 | 依赖 api，不依赖 Minecraft 层 |
| `api/minecraft`（内联自 GregTech6API） | 含 Minecraft/加载器类型的公开协议 | `:api-platform:<node>` 依赖公共 API 及平台类型，不依赖 core 或模组 main |
| `minecraft/src` | 游戏对象、共享运行时与跨版本输入 | 可调用 core；版本差异由 Stonecutter/适配承接 |
| `minecraft/versions/*/src` | 原生加载器入口与版本差异 | 组合共享输入及 core，不引用另一节点专属实现 |

两个节点不是把两套源码同时塞进一个模组。Stonecutter 生成各节点的共享输入，再与节点源码编译，见[跨版本指南](versions.md)。

## 第二层：职责

| 包族 | 关键问题 | 不应承担 |
| --- | --- | --- |
| `api` | 外部可以声明或调用什么？ | 内部算法、具体注册表和客户端展示 |
| `content` | 游戏里有哪些材料、设备、配方和具体对象？ | 无关领域的通用执行框架 |
| `engine` | 规则如何执行、状态如何演进？ | 内容清单、加载器入口和 UI 装饰 |
| `bootstrap` | 在什么阶段、按什么顺序装配？ | 每 tick 的业务逻辑 |
| `platform` | 如何接到当前游戏/加载器 API？ | 第二份共享规则或内容目录 |
| `client` / `network` / `event` / `mixin` / `compat` | 如何展示、同步、接入事件与外部生态？ | 绕过服务端权威规则 |
| `util` | 哪些小工具确实跨领域通用？ | 暂时不知道放哪的业务代码 |

API 独立编译，不依赖实现；core 不依赖 Minecraft。content 定义与 engine 执行通过明确类型协作，二者仍有历史交叉引用，不能把整个仓库理解成严格无环的包依赖图。API 独立版本与迁移方式见[附属 API](../ADDON_API.zh-CN.md)。

## 第三层：按领域追踪

例如新增机器：公开扩展需求先考虑 API；参数和布局属于 content；耗能/进度/产出规则属于 engine；Block/BlockEntity 承载世界状态；bootstrap 与平台注册接入生命周期；network 同步；client 展示。并非每次改动都要新建所有层，先复用现有路径。

继续读[包职责索引](packages/README.md)，只展开当前任务对应专题。资源与生成输入另见[资源指南](resources.md)。
