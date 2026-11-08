# CoverStackData

## 子原因

覆盖板与过滤器的物品数据有自己的键（`gt.cover.*`、`gt.logistics.value` 等），但读写方式与通用物品数据相同。`CoverStackData` 在 `StackData` 之上保留覆盖板专用逻辑。

## 例子

### 修复前

```java
public static CompoundTag readOrEmpty(ItemStack stack) {
//? if >=1.21.1 {
    var data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? new CompoundTag() : data.copyTag();
//?} else {
    return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
//?}
}
```

### 修复后

```java
public static boolean has(ItemStack stack) {
    return StackData.hasAnyData(stack);
}

public static CompoundTag readOrEmpty(ItemStack stack) {
    return StackData.readOrEmpty(stack);
}

public static void putInt(ItemStack stack, String key, int value) {
    StackData.putInt(stack, key, value);
}
```

`detached(...)` 在复制出的物品上读改写，再写回；覆盖板专用键的清理仍留在 `CoverStackData`，不进入通用门面。

### 为什么这么修复

`CoverStackData` 是覆盖板侧的半成品门面。先把读写全部委托 `StackData`，再保留 `detached` 的键清理逻辑，避免把覆盖板语义塞进通用门面。

### 其他可能的修复方式

- 把覆盖板专用键也放进 `StackData`。会让通用门面承担领域语义。
- 删除 `CoverStackData`，调用点直接用 `StackData`。会丢失 `detached` 的键清理。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
