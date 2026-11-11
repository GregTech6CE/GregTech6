# ItemStack migration

## 子原因

1.20.1 的物品栈把自定义数据放在外层 `tag` 复合标签里；1.21.1 改成 `components` 下的 `minecraft:custom_data`。DFU 只修复它认识的标准路径（`Items`、`Inventory`、`Equipment` 等），GT 把物品栈嵌在自定义 key 里时不会被识别。

## 例子

### 修复前

```java
//? if >=1.21.1 {
return ItemStack.parseOptional(lookup, tag);
//?} else {
return ItemStack.of(tag);
//?}
```

旧存档里的 `tag` 在 1.21.1 上读不到自定义数据。

### 修复后

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

### 为什么这么修复

`ItemStack.parseOptional` 只认新形状；旧 `tag` 需要手工搬进 `CUSTOM_DATA`。迁移只发生在读取侧，写出侧始终写新形状。

### 其他可能的修复方式

- 依赖 DFU 自动迁移。只对标准物品栈路径有效，GT 的自定义嵌套结构不在覆盖范围内。
- 在存档加载后统一扫描并迁移。需要遍历所有自定义结构，成本高且容易漏。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
