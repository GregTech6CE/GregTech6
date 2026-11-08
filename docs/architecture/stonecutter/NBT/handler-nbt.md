# HandlerNbt

## 子原因

`ItemStackHandler#serializeNBT` / `deserializeNBT` 在 1.21.1 上多一个 provider 参数；同时每个调用点都在重复 `put` / `getCompound` / `contains` 判断 / `Size` 补丁。

## 例子

### 修复前

```java
tag.put("Inventory", inventory.serializeNBT(lookup));

if (tag.contains("Inventory")) {
    CompoundTag data = tag.getCompound("Inventory").copy();
    data.putInt("Size", 54);
    inventory.deserializeNBT(lookup, data);
}
```

### 修复后

```java
HandlerNbt.save(tag, "Inventory", inventory, lookup);
HandlerNbt.load(tag, "Inventory", inventory, lookup);
```

`HandlerNbt` 的 parent/key 重载：

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

### 为什么这么修复

`Size` 补丁是必要的：`deserializeNBT` 会按 `Size` 重建数组，旧存档或配置变更可能带来不同的槽位数。重载把这个补丁、`contains` 判断和 provider 差异收在一处。

### 其他可能的修复方式

- 调用点直接调 `serializeNBT` / `deserializeNBT`，各自分支。分支数量随库存数量增长。
- 在方块实体里手写 `put` / `getCompound`。重复实现 `Size` 与 `contains` 逻辑，容易漏。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
