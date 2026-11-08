# NBT 序列化与 HolderLookup.Provider

返回[示例索引](README.md)。本文说明物品/流体栈与物品处理器在序列化时的 provider 差异，以及 `gregtech.engine.stack` 下的门面如何吸收它。

## 问题来源

1.20.5 起，物品栈与流体栈的序列化改为 Codec 驱动，编解码需要注册表上下文 `HolderLookup.Provider`。两个节点的原生签名不同：

| 操作 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 物品写 NBT | `ItemStack#save(CompoundTag)` | `ItemStack#save(HolderLookup.Provider)` |
| 物品读 NBT | `ItemStack#of(CompoundTag)` | `ItemStack#parseOptional(HolderLookup.Provider, CompoundTag)` |
| Handler 写 | `ItemStackHandler#serializeNBT()` | `ItemStackHandler#serializeNBT(HolderLookup.Provider)` |
| Handler 读 | `ItemStackHandler#deserializeNBT(CompoundTag)` | `ItemStackHandler#deserializeNBT(HolderLookup.Provider, CompoundTag)` |
| 方块实体写 | `saveAdditional(CompoundTag)` | `saveAdditional(CompoundTag, HolderLookup.Provider)` |
| 方块实体读 | `load(CompoundTag)` | `loadAdditional(CompoundTag, HolderLookup.Provider)` |
| 同步 tag | `getUpdateTag()` | `getUpdateTag(HolderLookup.Provider)` |
| 容器 | `ContainerHelper.saveAllItems(CompoundTag, NonNullList)` | `ContainerHelper.saveAllItems(CompoundTag, NonNullList, HolderLookup.Provider)` |
| 流体写 | `FluidStack#writeToNBT(CompoundTag)` | `FluidStack#save(HolderLookup.Provider)` |
| 流体读 | `FluidStack.loadFluidStackFromNBT(CompoundTag)` | `FluidStack.parseOptional(HolderLookup.Provider, CompoundTag)` |

`SavedData` 同样有差异：1.20.1 的 `DimensionDataStorage#computeIfAbsent` 接收 `Function<CompoundTag, T>`，1.21.1 接收 `SavedData.Factory`，其反序列化器是 `BiFunction<CompoundTag, HolderLookup.Provider, T>`。

已核实的事实：

- `HolderLookup.Provider` 在 Forge 1.20.1 47.4.x 已经存在，`RegistryAccess extends HolderLookup.Provider`，`RegistryOps.create(DynamicOps, HolderLookup.Provider)` 也可用。
- 1.20.1 缺的是 **vanilla 方法重载**，不是类型本身；`ItemStack`、`FluidStack`、`BlockEntity` 的 provider 重载在 1.20.1 不存在。
- provider 不是全局单例：服务端由 `WorldLoader` 构建，客户端在 1.21.1 由 `ClientboundRegistryDataPacket` 同步（1.20.1 随登录包下发），重载后可能失效。不能缓存到静态字段。
- 1.20.1 的 provider 参数是死参数，不能做非空断言；`null` 只允许出现在 1.20.1 分支。
- `ItemStack#save` 的返回类型两边不同（`CompoundTag` 与 `Tag`），门面统一返回 `Tag`。
- `RegistryAccess.EMPTY` 不能作为兜底：`lookup(Registries.ITEM)` 返回空，`ItemStack.CODEC` 仍会失败；兜底应使用 `RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)`。

## 建议方案

按三层组织：

```text
原生带 provider 的事件 / 回调
        ↓
版本无关的 provider 透传代码
        ↓
底层按域划分的 ProviderConsumer
```

1. **顶层**：vanilla 回调与事件。签名由 vanilla 或事件总线决定，必须保留 `//?`，方法体只做委托。
2. **中层**：共享业务代码。函数签名统一携带 `HolderLookup.Provider`，参数位置统一为最后一个，不出现 provider 相关分支。
3. **底层**：`gregtech.engine.stack` 下的门面。按域分类，内部断言非空，是唯一允许出现 provider 相关 `//?` 的位置。

| 门面 | 职责 | 是否带 provider |
| --- | --- | --- |
| `NbtCompat` | `ItemStack` / `FluidStack` ↔ NBT | 是 |
| `HandlerNbt` | `ItemStackHandler` ↔ NBT | 是 |
| `StackCompat` | 身份比较与流体 handler | 否 |
| `StackData` | `CUSTOM_DATA` / `getTag` 内存读写 | 否 |
| `RegistryContext`（待建） | 无 level 路径的 provider 兜底 | 是 |

约定：

- provider 参数放最后；例外只允许在顶层 vanilla 回调。当前仍有少数门面把 provider 放在第一位（`CoverStackData.readItem`、`FluidDisplayBinding` 的若干方法），迁移时应改为最后一位。
- 1.21.1 分支在门面内部 `requireNonNull`；1.20.1 分支允许 `null`。
- 返回类型统一到公共超类。`ItemStack#save` 在 1.20.1 返回 `CompoundTag`、在 1.21.1 返回 `Tag`，门面统一返回 `Tag`。
- 不在 `loadAdditional` 内读 `level.registryAccess()`；provider 由调用方传入。
- 无 level 路径（`getUpdateTag`、客户端 tooltip）应走统一的 `RegistryContext`（待建；当前 `GTToolEnchantments` 等各自维护静态 supplier），不传 `null`。

## 例子与分析

### `NbtCompat` 的 provider 入口

```java
public static Tag saveItem(ItemStack stack, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
    return stack.save(provider(lookup));
//?} else {
    return stack.save(new CompoundTag());
//?}
}

//? if >=1.21.1 {
private static HolderLookup.Provider provider(HolderLookup.Provider lookup) {
    return java.util.Objects.requireNonNull(lookup, "stack codec needs the registry lookup");
}
//?}
```

`provider(...)` 只在 1.21.1 分支定义和调用；1.20.1 分支不会引用它。

### 旧 `tag` 的迁移

`NbtCompat.readItem` 在 1.21.1 上读到的可能是旧格式：物品栈外层仍有 `tag` 复合标签，而不是 `components`。门面在解析后把它搬进 `CUSTOM_DATA`：

```java
public static ItemStack readItem(CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
    ItemStack stack = ItemStack.parseOptional(provider(lookup), tag);
    if (!stack.isEmpty() && tag.contains("tag"))
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.getCompound("tag")));
    return stack;
//?} else {
    return ItemStack.of(tag);
//?}
}
```

DFU 只修复它认识的标准路径（`Items`、`Inventory`、`Equipment` 等）。GT 把物品栈嵌在自定义 key 里时，DFU 不会识别，必须由门面手工迁移。

### `HandlerNbt` 的 parent/key 重载

```java
public static void save(CompoundTag parent, String key, ItemStackHandler handler,
        HolderLookup.Provider lookup) {
    CompoundTag tag = save(handler, lookup);
    tag.putInt("Size", handler.getSlots());
    parent.put(key, tag);
}

public static void load(CompoundTag parent, String key, ItemStackHandler handler,
        HolderLookup.Provider lookup) {
    if (!parent.contains(key, Tag.TAG_COMPOUND)) return;
    CompoundTag tag = parent.getCompound(key).copy();
    tag.putInt("Size", handler.getSlots());
    load(handler, tag, lookup);
}
```

重载统一了 `put` / `getCompound` / `contains` 判断 / `Size` 补丁。`Size` 补丁是必要的：`ItemStackHandler#deserializeNBT` 会按 `Size` 重建数组，旧存档或配置变更可能带来不同的槽位数。

### 完整调用链

`MetalChestBlockEntity`：

```java
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    saveData(tag, lookup);
}

private void saveData(CompoundTag tag, HolderLookup.Provider lookup) {
    tag.put("Inventory", HandlerNbt.save(inventory, lookup));
}
```

`HandlerNbt.save` → `ItemStackHandler#serializeNBT(lookup)` → 每个非空槽位的 `ItemStack#save(lookup)`。任何一环缺少 provider，`RegistryFixedCodec` 都会返回 `Not a registry ops`。

### 残留分支

`BookShelfBlock` 仍在调用点直接分支：

```java
//? if >=1.21.1 {
shelf.inventory().deserializeNBT(level.registryAccess(), new CompoundTag());
//?} else {
shelf.inventory().deserializeNBT(new CompoundTag());
//?}
```

这是 `HandlerNbt.load` 的等价写法，应改为 `HandlerNbt.load(shelf.inventory(), new CompoundTag(), level.registryAccess())`，消掉调用点分支。

### 无 level 路径

`GTToolEnchantments` 需要在客户端与服务端两侧重新解析附魔 `Holder`：

```java
public static HolderLookup.Provider currentLookup() {
    if (EffectiveSide.get().isClient()) return clientLookup.get();
    var server = ServerLifecycleHooks.getCurrentServer();
    return server == null ? null : server.registryAccess();
}
```

同一个附魔 id 在服务端注册表与客户端连接注册表里是不同的 `Holder.Reference` 实例。`ItemEnchantments` 按身份比较，跨快照混用会查不到、加不上。这类路径需要统一的 `RegistryContext`（待建），而不是每个类各维护一个静态 `Supplier`。

### 分析

- **为什么不能传 `null`**：1.21.1 的 `createSerializationContext(null)` 直接 NPE；`ItemStack.CODEC` 的 `id` 字段使用 `RegistryFixedCodec`，对 `RegistryOps` 是无条件要求。
- **为什么不能缓存 provider**：服务端 provider 随世界加载构建，客户端 provider 随连接同步；缓存到静态字段会在重载或切换世界后指向过期快照。
- **为什么 `RegistryAccess.EMPTY` 不行**：它的 `lookup(Registries.ITEM)` 返回空，物品栈仍然无法编码；静态注册表兜底应使用 `fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)`。
- **为什么用 typed provider 而不是 `Object`**：`Object` 会把类型错误推迟到运行时；`HolderLookup.Provider` 在 1.20.1 已存在，共享签名可以直接使用，只有调用体需要分支。

相关契约见 issue [#358](https://github.com/GregTech6CE/GregTech6/issues/358)。
