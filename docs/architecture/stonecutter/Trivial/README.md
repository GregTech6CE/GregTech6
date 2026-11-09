# Trivial 通用原因

共享源码里大量分支并不是真实语义差异，而是同一段代码在两侧各写一遍：两侧完全相同、一侧为空、一侧只有 import、同条件拆成两个单侧块、只差一个常量或命名空间。这些分支不改变行为，只增加维护面。

历史扫描的规模：

| 形态 | 清理前数量 | 说明 |
| --- | --- | --- |
| 两侧完全相同的分支 | 29 | 清理前扫描 |
| 空块 | 588 | 清理前扫描 |
| 单侧 import 分支 | 80+ | 清理前扫描 |
| 同义候选（宽口径） | 889 | 清理前扫描，含误报 |

数量是清理前的快照，用于说明形态分布，不是当前待办数。

判断标准只有一条：**分支是否真的消失**。只把分支内的一行调用换成门面、分支本身原样保留，不算有效改动。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| 同义分支 | 两侧代码完全相同 | [identical.md](identical.md) | identical |
| 空块与单侧 import | 一侧为空或只有 import | [empty-import.md](empty-import.md) | empty, import |
| 拆分重复 | 同条件两个单侧块，或重复注册 | [split-duplicate.md](split-duplicate.md) | split duplicate, registration |
| 注册表与 id | 注册表查找、命名空间、标签目录 | [registry-id.md](registry-id.md) | Mc, ResourceLocation, tags |
| logger 与模型 | 日志门面与模型资源位置 | [logger-model.md](logger-model.md) | Mc.logger, ModelResourceLocation |
| 身份与包装 | 身份比较、包装类、静态谓词 | [identity-wrapper.md](identity-wrapper.md) | StackCompat, NeoToolBindings |
| 语义案例 | 看起来同义、实际承载行为 | [semantic-cases.md](semantic-cases.md) | craftKind, plunger, igniter |
