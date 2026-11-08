# StackData

## 子原因

1.20.1 的 tag 是活引用，1.21.1 的组件是值；如果调用点直接读写，同一段代码在两个节点上的写回行为会不同。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
tag.putInt("gt.mold", value);
stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
//?} else {
stack.getOrCreateTag().putInt("gt.mold", value);
//?}
```

### 修复后

```java
StackData.putInt(stack, "gt.mold", value);
```

`StackData` 的公共面：

```java
public static CompoundTag read(ItemStack stack) {
//? if >=1.21.1 {
    var data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.copyTag();
//?} else {
    return stack.hasTag() ? stack.getTag().copy() : null;
//?}
}

public static void write(ItemStack stack, CompoundTag tag) {
//? if >=1.21.1 {
    if (tag == null || tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
//?} else {
    stack.setTag(tag == null || tag.isEmpty() ? null : tag);
//?}
}

public static void update(ItemStack stack, Consumer<CompoundTag> action) {
    var tag = readOrEmpty(stack);
    action.accept(tag);
    write(stack, tag);
}
```

### 为什么这么修复

`read` 返回副本，调用方修改副本不会直接影响物品；`update` 把读改写收成一个入口，避免 1.20.1 的活引用语义泄漏到共享代码。`write` 把空 tag 归一化为移除，保证两边的 `has` 语义一致。

### 其他可能的修复方式

- 每个调用点各自 copy → 修改 → set。重复且容易漏写回。
- 直接返回活引用。1.20.1 能跑，1.21.1 上不生效。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
