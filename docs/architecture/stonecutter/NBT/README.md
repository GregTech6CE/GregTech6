# NBT 通用原因

物品/流体栈与容器的 NBT 序列化入口在两个版本上不同。1.20.5 起编解码改为 Codec 驱动，1.21.1 的方法多一个注册表上下文 `HolderLookup.Provider`；1.20.1 没有这些重载。

| 操作 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 物品写 NBT | `ItemStack#save(CompoundTag)` | `ItemStack#save(HolderLookup.Provider)` |
| 物品读 NBT | `ItemStack#of(CompoundTag)` | `ItemStack#parseOptional(HolderLookup.Provider, CompoundTag)` |
| Handler 写 | `ItemStackHandler#serializeNBT()` | `ItemStackHandler#serializeNBT(HolderLookup.Provider)` |
| Handler 读 | `ItemStackHandler#deserializeNBT(CompoundTag)` | `ItemStackHandler#deserializeNBT(HolderLookup.Provider, CompoundTag)` |
| 容器写 | `ContainerHelper.saveAllItems(CompoundTag, NonNullList)` | `ContainerHelper.saveAllItems(CompoundTag, NonNullList, HolderLookup.Provider)` |
| 容器读 | `ContainerHelper.loadAllItems(CompoundTag, NonNullList)` | `ContainerHelper.loadAllItems(CompoundTag, NonNullList, HolderLookup.Provider)` |
| 流体写 | `FluidStack#writeToNBT(CompoundTag)` | `FluidStack#save(HolderLookup.Provider)` |
| 流体读 | `FluidStack.loadFluidStackFromNBT(CompoundTag)` | `FluidStack.parseOptional(HolderLookup.Provider, CompoundTag)` |
| 存档数据 | `computeIfAbsent(Function<CompoundTag, T>, Supplier<T>, String)` | `computeIfAbsent(SavedData.Factory<T>, String)` |

1.20.1 缺的是 **vanilla 方法重载**，不是 `HolderLookup.Provider` 类型本身。provider 的来源、缓存与无 level 路径见 [Provider](../Provider/README.md)。

线格式也从 `{id, Count, tag}` 变为 `{id, count, components}`，旧 `tag` 需要迁移。

子类型见[分类索引](../categories.md#nbt)。
