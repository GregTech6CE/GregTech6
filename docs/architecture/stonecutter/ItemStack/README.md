# ItemStack

物品栈的差异分六类：provider 签名、自定义数据存储、线格式、旧数据迁移、身份比较、封面过滤器 provider。

| 差异 | 1.20.1 | 1.21.1 | 文档 |
| --- | --- | --- | --- |
| 写 NBT | `save(CompoundTag)` | `save(HolderLookup.Provider)` | [provider.md](provider.md) |
| 读 NBT | `of(CompoundTag)` | `parseOptional(HolderLookup.Provider, CompoundTag)` | [provider.md](provider.md) |
| 自定义数据 | `getTag` / `getOrCreateTag` | `CUSTOM_DATA` / `CustomData` | [storage.md](storage.md) |
| 线格式 | `{id, Count, tag}` | `{id, count, components}` | [wire-format.md](wire-format.md) |
| 旧数据 | `tag` | `components["minecraft:custom_data"]` | [migration.md](migration.md) |
| 身份比较 | `isSameItemSameTags` | `isSameItemSameComponents` | [identity.md](identity.md) |
| 封面过滤器 | `setItemFilter(installed, held)` | `setItemFilter(installed, held, level.registryAccess())` | [registry-access.md](registry-access.md) |

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| provider | `ItemStack#save` / `parseOptional` 的 provider 参数 | [provider.md](provider.md) | save, parseOptional |
| storage | `tag` ↔ `CUSTOM_DATA`、`StackData`、谓词 | [storage.md](storage.md) | CUSTOM_DATA, StackData |
| block-entity-data | `BlockEntityTag` ↔ `BLOCK_ENTITY_DATA`、`BlockEntityData` 门面 | [block-entity-data.md](block-entity-data.md) | BLOCK_ENTITY_DATA, BlockEntityData, CustomData |
| block-state-data | `BlockStateTag` ↔ `BLOCK_STATE`、`BlockStateData` 门面 | [block-state-data.md](block-state-data.md) | BLOCK_STATE, BlockStateData, BlockItemStateProperties |
| wire-format | `{id, Count, tag}` → `{id, count, components}` | [wire-format.md](wire-format.md) | Count, components |
| migration | 旧 `tag` 搬进 `CUSTOM_DATA` | [migration.md](migration.md) | tag, CUSTOM_DATA |
| identity | `isSameItemSameTags` ↔ `isSameItemSameComponents` | [identity.md](identity.md) | StackCompat.sameItem |
| registry-access | 封面过滤器写入时的 `HolderLookup.Provider` 参数 | [registry-access.md](registry-access.md) | registryAccess, setItemFilter, setFluidFilter |
