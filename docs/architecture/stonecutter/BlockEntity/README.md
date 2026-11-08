# BlockEntity 通用原因

方块实体的自定义读写与同步回调由 vanilla 固定签名，共享代码无法统一 override。1.20.5 起这些回调增加了注册表上下文 `HolderLookup.Provider`，同时 `load` 改名 `loadAdditional`。

| 用途 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 自定义写 | `protected saveAdditional(CompoundTag)` | `protected saveAdditional(CompoundTag, HolderLookup.Provider)` |
| 自定义读 | `public load(CompoundTag)` | `protected loadAdditional(CompoundTag, HolderLookup.Provider)` |
| 同步 tag | `public getUpdateTag()` | `public getUpdateTag(HolderLookup.Provider)` |
| 静态加载 | `loadStatic(BlockPos, BlockState, CompoundTag)` | `loadStatic(BlockPos, BlockState, CompoundTag, HolderLookup.Provider)` |
| 物品化 | `saveToItem(ItemStack)` | `saveToItem(ItemStack, HolderLookup.Provider)` |

`loadAdditional` 执行时方块实体可能还没有 `level`（`loadStatic` 只读档，`level` 由调用方稍后 `setLevel`），因此不能在方法里调用 `level.registryAccess()`，必须使用传入的 provider。

子类型见[分类索引](../categories.md#blockentity)。
