# 静态加载

## 子原因

`loadStatic` 在方块实体还没有 `level` 时执行：它只负责读档，`level` 由调用方稍后 `setLevel`。因此读档路径不能从 `level` 取 provider，只能使用调用方传入的那一份。

## 例子

### 修复前

```java
//? if >=1.21.1 {
BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
//?} else {
BlockEntity.loadStatic(pos, state, tag);
//?}
```

### 修复后

调用点保留分支；如果同一模式重复出现，把它收进 `NbtCompat` 或调用方自己的门面。仓库当前的读档入口由 `loadAdditional` 承接，`NbtCompat.saveWithoutMetadata` 已经覆盖了写出方向：

```java
public static CompoundTag saveWithoutMetadata(BlockEntity blockEntity, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
    return blockEntity.saveWithoutMetadata(provider(lookup));
//?} else {
    return blockEntity.saveWithoutMetadata();
//?}
}
```

### 为什么这么修复

`loadStatic` 的 provider 必须来自调用方。把差异收进门面后，调用点不需要判断当前方块实体是否已经有 level。

### 其他可能的修复方式

- 在 `loadAdditional` 内调用 `level.registryAccess()`。`level` 可能为 `null`，会 NPE。
- 在方块实体里缓存 provider。读档先于 `setLevel`，缓存到的是空值或过期快照。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
