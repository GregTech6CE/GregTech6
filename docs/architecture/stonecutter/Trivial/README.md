# Trivial

两侧相同、一侧为空、只差 import 或常量的分支，优先折叠。

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
| identical | 两侧代码完全相同 | [identical.md](identical.md) | identical |
| empty-import | 一侧为空或只有 import | [empty-import.md](empty-import.md) | empty, import |
| split-duplicate | 同条件两个单侧块，或重复注册 | [split-duplicate.md](split-duplicate.md) | split duplicate |
