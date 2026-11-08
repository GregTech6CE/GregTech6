# Stonecutter 示例索引

本目录按问题类型索引 `//?` 分支的处理方法，每篇给出问题来源、建议方案与真实调用例子；它不是仓库根 `examples/`（规划中的附属示例目录）的可运行示例。

先读[设计](../design.md)与[用法](../usage.md)。下面每节的关键词用于检索，详情页给出完整说明。

## BlockEntity

* 关键词：BlockEntity, saveAdditional, loadAdditional, getUpdateTag, loadStatic
* 详情：[BlockEntity.md](BlockEntity.md)
* 建议方案：透传。保留 override 签名分支，方法体抽成共享的 `saveData` / `loadData`。
* 其他方案
  * 包装：在节点侧保留薄覆盖，方法体只委托共享方法（参考实现来源：GT Modern `ManagedSyncBlockEntity`）。
  * 全量分支：两个节点各写一份 `saveAdditional` / `loadAdditional`（参考实现来源：Townstead `ServingPlateBlockEntity`），只在差异确实无法下沉时使用。

## NBT

* 关键词：HolderLookup.Provider, NbtCompat, HandlerNbt, serializeNBT, deserializeNBT, RegistryContext（待建）
* 详情：[NBT.md](NBT.md)
* 建议方案：三层透传。vanilla 回调携带 provider，共享代码逐层传递，底层门面按域封装。
* 其他方案
  * 工具类收敛：把 provider 差异集中到一个编解码工具类，调用方统一传 provider（参考实现来源：Minopp `NbtIOShim`）。
  * 节点侧补重载：1.21.1 已有 provider 方法、1.20.1 只有无参方法时，在 1.20.1 分支补同签名重载委托无参方法。

## CompoundTag

* 关键词：CompoundTag, CUSTOM_DATA, CustomData, getTag, hasTag, StackData, DataComponent
* 详情：[CompoundTag.md](CompoundTag.md)
* 建议方案：门面收敛。公共面统一为 `CompoundTag`，`tag` 与 `CUSTOM_DATA` 的分支留在 `StackData` 内部。
* 其他方案
  * 类型化组件：只在一个版本可用的强类型组件保留在该版本分支，不进入公共签名。
  * 逐点分支：调用点各自写 `//?`，只适合一次性、不会再扩散的差异。

## Trivial

* 关键词：identical, empty block, import, split duplicate, registry lookup, namespace, logger, ModelResourceLocation
* 详情：[trivial.md](trivial.md)
* 建议方案：先折叠同义分支，再把真实差异收进现有门面。
* 其他方案
  * 批量脚本：用扫描脚本定位候选，再逐个确认（参考实现来源：本仓库历史提交）。
  * 保留分支：两侧语义确实不同，或折叠会改变行为时保留，并记录原因。

## 分类总览

| 问题类 | 典型表现 | 文档 |
| --- | --- | --- |
| 方块实体持久化 | `saveAdditional` / `loadAdditional` / `getUpdateTag` 签名不同 | [BlockEntity.md](BlockEntity.md) |
| 序列化上下文 | `ItemStack#save`、`ItemStackHandler#serializeNBT` 多一个 provider | [NBT.md](NBT.md) |
| 物品自定义数据 | `getTag` / `getOrCreateTag` 与 `CUSTOM_DATA` | [CompoundTag.md](CompoundTag.md) |
| 同义分支 | 两侧代码相同，只是写法或常量不同 | [trivial.md](trivial.md) |
| 空块与单侧 import | 一侧为空，另一侧只有 import | [trivial.md](trivial.md) |
| 拆分重复 | 同条件两个单侧块，非空体相同 | [trivial.md](trivial.md) |
| 注册表与命名空间 | `BuiltInRegistries` / `ForgeRegistries`、`GregTech.id` / `ResourceLocation` | [trivial.md](trivial.md) |
| 身份比较 | `isSameItemSameTags` / `isSameItemSameComponents`、`isFluidEqual` / `isSameFluidSameComponents` | [trivial.md](trivial.md) |
