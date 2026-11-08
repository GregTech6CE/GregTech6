# CompoundTag 通用原因

1.20.5 起，物品的自定义数据从 `ItemStack` 上的一个可变 `CompoundTag` 改为 `DataComponents.CUSTOM_DATA` 组件。

| 操作 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 判存在 | `ItemStack#hasTag()` | `ItemStack#has(DataComponents.CUSTOM_DATA)` |
| 读整块 | `ItemStack#getTag()` | `ItemStack#get(DataComponents.CUSTOM_DATA)` 后 `CustomData#copyTag()` |
| 写整块 | `ItemStack#setTag(CompoundTag)` | `ItemStack#set(DataComponents.CUSTOM_DATA, CustomData.of(tag))` |
| 惰性创建 | `ItemStack#getOrCreateTag()` | 无对应，需 copy → 修改 → set |
| 子 tag | `getTagElement` / `getOrCreateTagElement` | 无直接对应 |
| 删除整块 | `setTag(null)` | `remove(DataComponents.CUSTOM_DATA)` |
| 删除单键 | `removeTagKey(String)` | copy → remove → set 或 remove |
| 身份比较 | `ItemStack.isSameItemSameTags` | `ItemStack.isSameItemSameComponents` |
| 是否有非默认数据 | `hasTag()` | `!getComponentsPatch().isEmpty()` |
| 线格式 | `{id, Count, tag}` | `{id, count, components}` |

关键语义差异：

1. 1.20.1 的 `getOrCreateTag()` 返回栈内活引用，原地修改即生效；1.21.1 没有活引用，必须 copy → 修改 → set。
2. `CustomData.of(tag)` 在 NeoForge 21.1 会复制传入的 tag，但读取时 `copyTag()` 仍会产生一次副本；不要依赖引用共享。
3. 空 tag 的表示不同：1.20.1 用 `setTag(null)`；1.21.1 用 `remove(CUSTOM_DATA)`。`set(CUSTOM_DATA, CustomData.of(new CompoundTag()))` 会留下一个存在但为空的组件，`has()` 返回 `true`。
4. 1.21.1 的 `DataComponentType<T>` 是强类型槽位；`CUSTOM_DATA` 是承载自由形式物品数据的组件（`BLOCK_ENTITY_DATA` 等也使用 `CustomData`，但语义是方块实体数据），其余组件（`DAMAGE`、`ENCHANTMENTS`、`CUSTOM_NAME` 等）都有各自的类型。

两个谓词在 1.20.1 上等价，在 1.21.1 上不等价：附魔、伤害、自定义名称都是独立组件，不在 `CUSTOM_DATA` 里。

子类型见[分类索引](../categories.md#compoundtag)。
