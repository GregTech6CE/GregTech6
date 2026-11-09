# ItemStack provider

## 子原因

1.20.1 的 `ItemStack#save(CompoundTag)` 返回 `CompoundTag`；1.21.1 的 `save(HolderLookup.Provider)` 返回 `Tag`，读取改为 `parseOptional(Provider, CompoundTag)`。

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
tag.put("item", NbtCompat.saveItem(stack, lookup));
```

`NbtCompat.saveItem` 统一返回 `Tag`，内部按版本分支。

### 为什么这么修复

调用点不需要知道 1.20.1 返回 `CompoundTag`、1.21.1 返回 `Tag`；provider 只在 1.21.1 分支断言非空。

### 其他可能的修复方式

- 每个调用点各自分支。返回类型不一致，容易漏写回。
- 用 `Object` 传 provider。类型错误推迟到运行时。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
