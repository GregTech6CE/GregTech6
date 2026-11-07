# 平台边界：共享门面与节点实现

返回[包索引](README.md)。平台边界按共享门面、NeoForge 实现和 Forge 实现拆分；三层结构、区分与归属、Stonecutter 用法和已知需要单独检查的边界见[跨版本指南](../versions.md)。

| 层 | 页面 | 角色 |
| --- | --- | --- |
| 共享门面 | [共享门面](platforms/facade.md) | 版本中立 API，两个节点实现的契约 |
| NeoForge 实现 | [NeoForge 实现](platforms/neoforge.md) | 参考实现，Forge 对齐此行为 |
| Forge 实现 | [Forge 实现](platforms/forge.md) | 对齐 NeoForge，记录并验证偏差 |

节点实现不只在 `platform.*`；Forge 与 NeoForge 的原生实现还分布在各自的 `bootstrap`、`content`、`engine`、`client`、`network`、`mixin`、`compat.kubejs` 等包，具体入口见两个节点子页。

## 验收

按层见各子页的“验收”：[共享门面](platforms/facade.md#验收)、[NeoForge 实现](platforms/neoforge.md#验收)、[Forge 实现](platforms/forge.md#验收)。

## 自动化测试范围

按层见各子页的“自动化测试范围”：[共享门面](platforms/facade.md#自动化测试范围)、[NeoForge 实现](platforms/neoforge.md#自动化测试范围)、[Forge 实现](platforms/forge.md#自动化测试范围)。
