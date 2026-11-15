# 跨平台决策与差异

本目录记录 Forge（1.20.1）与 NeoForge（1.21.1）双平台之间，因加载器接口、Minecraft 版本机制或原生资产差异而长期保留的设计取舍与行为边界。多版本源码拆分方式和 Stonecutter 约定见[多版本架构](../../architecture/versions.md)，各平台包职责与同步边界见 [NeoForge 平台包](../../architecture/packages/platforms/neoforge.md) 与 [Forge 平台包](../../architecture/packages/platforms/forge.md)。

双节点共享同一份 `api` 与 `core` 规则及统一的 Minecraft 材料/配方语义。仅当版本或加载器差异确实需要长期并存而非修复为一致时，才在此按主题登记；未决缺陷、回归观察或一次性测试结果不写入本台账。

## 目录

目前尚无已核定长期保留的跨平台独立差异条目；后续确有需要长期维护的平台差异决策时，按主题新建文档并在本列表挂载。

## 跨平台基线约定

- **共享契约基准**：除加载器及 MC 版本原生机制所限外，双端行为以共享逻辑和 [NeoForge 平台约定](../../architecture/packages/platforms/neoforge.md)为同步基准。
- **平台特有内容**：各版本独占的原版内容或外部生态对接由对应平台入口负责装配，不强制把单端原版差异伪装为共有特性，亦不将未核实的测试计数差异直接视为平台决策。
