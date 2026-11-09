# BlockEntity

方块实体的自定义读写与同步回调由 vanilla 固定签名。1.20.5 起这些回调增加了 `HolderLookup.Provider`，同时 `load` 改名 `loadAdditional`。

| 用途 | 1.20.1 | 1.21.1 |
| --- | --- | --- |
| 自定义写 | `protected saveAdditional(CompoundTag)` | `protected saveAdditional(CompoundTag, HolderLookup.Provider)` |
| 自定义读 | `public load(CompoundTag)` | `protected loadAdditional(CompoundTag, HolderLookup.Provider)` |
| 同步 tag | `public getUpdateTag()` | `public getUpdateTag(HolderLookup.Provider)` |
| 静态加载 | `loadStatic(BlockPos, BlockState, CompoundTag)` | `loadStatic(BlockPos, BlockState, CompoundTag, HolderLookup.Provider)` |

`loadAdditional` 执行时方块实体可能还没有 `level`（`loadStatic` 只读档，`level` 由调用方稍后 `setLevel`），因此不能在方法里调用 `level.registryAccess()`。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| provider | 薄覆盖、codecLookup、同步 tag、静态加载 | [provider.md](provider.md) | saveData, loadData, codecLookup |
