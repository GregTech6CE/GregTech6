# Provider 通用原因

`HolderLookup.Provider` 是 Codec 解析注册表条目时使用的上下文。1.20.5 起物品/流体栈的序列化改为 Codec 驱动，编解码需要它；1.20.1 的序列化方法不接收它。

| 操作 | 1.20.1 | 1.21.1 |
| --- | --- | --- |
| 物品写 NBT | `ItemStack#save(CompoundTag)` | `ItemStack#save(HolderLookup.Provider)` |
| 物品读 NBT | `ItemStack#of(CompoundTag)` | `ItemStack#parseOptional(HolderLookup.Provider, CompoundTag)` |
| Handler 写 | `ItemStackHandler#serializeNBT()` | `ItemStackHandler#serializeNBT(HolderLookup.Provider)` |
| 方块实体写 | `saveAdditional(CompoundTag)` | `saveAdditional(CompoundTag, HolderLookup.Provider)` |
| 方块实体读 | `load(CompoundTag)` | `loadAdditional(CompoundTag, HolderLookup.Provider)` |
| 同步 tag | `getUpdateTag()` | `getUpdateTag(HolderLookup.Provider)` |
| 流体写 | `FluidStack#writeToNBT(CompoundTag)` | `FluidStack#save(HolderLookup.Provider)` |

`HolderLookup.Provider` 在 1.20.1 已经存在，`RegistryAccess extends HolderLookup.Provider`；缺的是 vanilla 方法重载，不是类型本身。

provider 不是全局单例：服务端由 `WorldLoader` 构建，客户端在 1.21.1 由 `ClientboundRegistryDataPacket` 同步（1.20.1 随登录包下发），重载或切换世界后可能失效。

子类型见[分类索引](../categories.md#provider)。
