# NbtCompat

## 子原因

物品栈与流体栈的 save/parse 在 1.20.1 与 1.21.1 上签名不同，返回类型也不同：`ItemStack#save` 在 1.20.1 返回 `CompoundTag`，在 1.21.1 返回 `Tag`。调用点不应该逐个判断版本。

## 例子

### 修复前

```java
//? if >=1.21.1 {
tag.put("item", stack.save(lookup));
//?} else {
tag.put("item", stack.save(new CompoundTag()));
//?}
```

### 修复后

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

调用点：

```java
tag.put("item", NbtCompat.saveItem(stack, lookup));
```

`NbtCompat` 同时提供 `saveFluid` / `readFluid` / `writeTank` / `readTank` / `saveWithoutMetadata`，覆盖物品、流体、罐体与方块实体快照。

### 为什么这么修复

门面统一返回 `Tag`，调用点不需要知道 1.20.1 返回的是 `CompoundTag`。`provider(...)` 只在 1.21.1 分支定义和调用，1.20.1 不会引用它。

### 其他可能的修复方式

- 每个调用点各自分支。分支数量随调用点增长，且返回类型不一致。
- 用 `Object` 传 provider。把类型错误推迟到运行时，且 1.20.1 仍要分支。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
