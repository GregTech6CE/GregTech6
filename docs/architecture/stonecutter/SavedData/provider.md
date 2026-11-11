# SavedData provider

## 子原因

1.20.1 的 `DimensionDataStorage#computeIfAbsent` 接收 `Function<CompoundTag, T>`；1.21.1 接收 `SavedData.Factory`，其反序列化器是 `BiFunction<CompoundTag, HolderLookup.Provider, T>`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
storage.computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "gt_data");
//?} else {
storage.computeIfAbsent(Data::load, Data::new, "gt_data");
//?}
```

### 修复后

调用点保留分支；`SavedData` 的工厂签名不同，需要单独门面或调用点分支。

### 为什么这么修复

`SavedData` 的工厂是存储层入口，provider 由存储层传入；没有共享门面时只能在调用点分支。

### 其他可能的修复方式

- 为 `SavedData` 加 `NbtCompat` 工厂重载。调用点不再分支。
- 改用 BlockEntity NBT 保存数据。改变数据归属。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
