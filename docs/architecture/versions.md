# 跨版本要求与入口

节点由 [settings.gradle](../../settings.gradle) 定义：`1.20.1-forge` 与 `1.21.1-neoforge`。两个节点的行为与内容保持一致，版本差异由 Stonecutter 与共享门面承接，核心业务规则统一维护。

当前 VCS 节点与活动节点均为 `1.20.1-forge`（详见[Stonecutter 语法与生成](stonecutter/usage.md#vcsversion-与-active)）。

## 核心要求

| 维度 | 要求 |
| --- | --- |
| 行为与内容 | 以 parity 声明与原版 GT6 golden 为准，同一输入在两平台产出相同结果；材料、配方与机器规则双端对齐。 |
| 源码放置 | 业务规则、内容定义与共享运行时放在 `minecraft/src`；加载器专属入口放在 `minecraft/versions/<node>/src`，节点之间相互隔离。 |
| 条件编译 | `//?` 仅写在共享输入内，用于承载无法由门面吸收的平台 API 运行时差异；公开签名保持加载器类型中立（`GTBlocks`/`GTItems` 为已知例外）。 |
| Java 版本 | Forge 使用 Java 17，NeoForge 使用 Java 21；共享代码遵循 Java 17 语言特性。 |
| 事实来源 | 版本支持、依赖版本、任务入口与 `vcsVersion` 以仓库 Gradle 配置为准。 |

## 文档指引

| 主题 | 入口 |
| --- | --- |
| 跨版本架构、三层结构与 `//?` 规范 | [Stonecutter 跨版本维护](stonecutter/README.md) |
| 差异裁决顺序与统一目标 | [跨版本统一方针](stonecutter/unification-policy.md) |
| 共享门面与两节点实现职责 | [平台边界](packages/platforms.md) |
| 双平台编译、格式与 GameTest | [验证与验收](../development/validation/README.md) |

## 修改流程

1. 按[三层结构与归属](stonecutter/design.md#判断准则)确定改动属于共享代码、共享门面还是节点实现，并排查两个节点的现有代码。
2. 优先确定共享门面契约，再按参考实现与对齐实现编写。
3. 保持活动版本稳定；确需切换 `stonecutter.active` 时，先检查工作区并审查生成改写范围。
4. 按[验证与验收](../development/validation/README.md)与[平台边界](packages/platforms.md)执行双平台编译与检查，核对生成源码后回到源输入修改。

## 版本间典型差异边界

- **Java 工具链**：Forge 为 Java 17，NeoForge 为 Java 21，共享代码保持 Java 17 兼容。
- **核心游戏 API**：物品 NBT 与数据组件、资源标识构造、注册与能力 API 差异沿已有适配处理，见 [ItemStack](stonecutter/ItemStack/README.md)、[Registry](stonecutter/Registry/README.md) 与 [Capability](stonecutter/Capability/README.md)。
- **资源路径与格式**：1.20.1（`recipes/`、`tags/items/`、`loot_tables/`）与 1.21.1（`recipe/`、`tags/item/`、`loot_table/`）各自维护对应路径；共享构建会自动转换完整 JSON 文档中的 `forge:composite` ↔ `neoforge:composite`。
- **发行产物**：两平台发行任务与产物位置见[组装检查](../development/validation/checks/assemble.md#产物位置)。
