# 跨版本要求与入口

先读[架构](README.md)。节点由 [settings.gradle](../../settings.gradle) 定义：`1.20.1-forge` 与 `1.21.1-neoforge`。两个节点的行为与内容必须一致；版本差异由 Stonecutter 与共享门面承接，不维护两套业务规则。差异的裁决顺序、统一目标和平台 API 例外见[跨版本统一方针](stonecutter/unification-policy.md)。

当前 VCS 节点与活动节点均为 `1.20.1-forge`。两者的区别见[用法](stonecutter/usage.md#vcsversion-与-active)。

## 要求

| 要求 | 说明 |
| --- | --- |
| 行为一致 | 以 parity 声明与原版 GT6 golden 为准；同一输入在两个节点上产生相同结果。确需不同行为时写明原因并分别验证。 |
| 内容一致 | 材料、配方、机器规则与注册内容在两个节点上对齐。 |
| 共享输入 | 业务规则、内容定义与共享运行时放 `minecraft/src`；两个节点都编译。 |
| 节点输入 | 加载器专属入口放 `minecraft/versions/<node>/src`；不引用另一节点专属实现。 |
| 条件编译 | `//?` 只写在共享输入；只承载平台 API 运行时差异（类/方法不存在、签名、注册表、能力、网络、事件生命周期）。注册归属、列表分叉等差异统一到共享路径。 |
| 公开签名 | 共享代码的新增公开签名不出现加载器类型；`GTBlocks`/`GTItems` 是已知例外。 |
| Java 版本 | Forge 使用 Java 17，NeoForge 使用 Java 21；共享代码不能无条件使用只在 21 可用的特性。 |
| 事实来源 | 版本支持、依赖版本、任务入口与 `vcsVersion` 以仓库配置为准，不以文档示例为准。 |

## 文档指引

| 要做什么 | 从这里开始 |
| --- | --- |
| 裁决版本差异、判断保留还是统一 | [跨版本统一方针](stonecutter/unification-policy.md) |
| 理解三层结构、内容分类与归属 | [Stonecutter 设计](stonecutter/design.md) |
| 查 `//?` 写法、输入目录、生成结果与验证命令 | [Stonecutter 用法](stonecutter/usage.md) |
| 处理具体调用点（BlockEntity、ItemStack、FluidStack、Container、SavedData、Registry、Capability、ClientModel、Facade、Trivial、Parity） | [分类索引](stonecutter/categories.md) |
| 查平台门面、Forge/NeoForge 节点入口与验收 | [平台边界](packages/platforms.md) |
| 执行双平台编译、格式与 GameTest | [验证与验收](../development/validation/README.md) |

## 修改步骤

1. 按[设计](stonecutter/design.md#判断准则)分类：共享代码、共享门面或节点实现。
2. 检查两个节点的现有实现，不能只查 `platform`。
3. 先确定共享门面契约，再按参考实现与对齐实现编写。
4. 按[用法](stonecutter/usage.md#验证)与[验证与验收](../development/validation/README.md)执行双平台编译，检查失败节点实际生成源码，随后修改源输入。
5. 按[平台边界](packages/platforms.md)的验收与自动化测试范围执行。

不为普通修改随意切换活动版本。确需切换时先检查工作区、节点任务与生成改写范围，完成后审查是否带入大规模无关格式/分支变动。

## 区分与归属

跨版本代码按共享代码、共享门面、节点实现三层组织；内容分类与六条判断准则见[Stonecutter 设计](stonecutter/design.md#区分与归属)。

平台能力的调用方向是共享代码 → 共享门面 → 节点实现。共享代码不 import 节点类；节点实现不把加载器类型泄漏回共享层。

## 已知需要单独检查的边界

- Forge 使用 Java 17、NeoForge 使用 Java 21；共享代码不能无条件使用只在 21 可用的特性。
- 物品 NBT 与数据组件、资源标识构造、注册与能力 API 存在差异，沿已有适配处理；具体做法见 [ItemStack](stonecutter/ItemStack/README.md)、[Registry](stonecutter/Registry/README.md) 与 [Capability](stonecutter/Capability/README.md)。
- 两版本数据包目录命名不同，不能把一边 JSON 路径直接拷给另一边。
- 共享构建对完整 JSON 文档中的 composite loader 名称进行转换；这不是所有资源格式都会自动迁移的保证。
- 两平台的发行任务和产物区别见[组装检查](../development/validation/checks/assemble.md#产物位置)；游戏侧 API 在平台编译/重映射阶段接入。

版本支持、依赖版本和任务入口以仓库配置为事实来源。升级工具或加载器属于单独任务，不能因文档示例而顺手修改。
