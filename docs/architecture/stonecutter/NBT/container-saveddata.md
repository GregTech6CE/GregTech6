# 容器与存档数据

## 子原因

`ContainerHelper` 与 `SavedData` 的读写入口在 1.21.1 上也需要 provider。仓库里仍有多处直接调用 `ContainerHelper`，没有走门面。

## 例子

### 修复前

```java
//? if >=1.21.1 {
ContainerHelper.saveAllItems(tag, outputs, lookup);
//?} else {
ContainerHelper.saveAllItems(tag, outputs);
//?}
```

### 修复后

```java
//? if >=1.21.1 {
ContainerHelper.saveAllItems(tag, outputs, lookup);
//?} else {
ContainerHelper.saveAllItems(tag, outputs);
//?}
```

当前 `NbtCompat` 还没有覆盖 `ContainerHelper` 与 `SavedData`。在补门面之前，调用点保留分支；补门面时注意：

- `ContainerHelper.loadAllItems` 在两个节点都返回 `void`，门面不能 `return` 它的结果。
- `SavedData` 在 1.20.1 使用 `Function<CompoundTag, T>`，1.21.1 使用 `SavedData.Factory` 的 `BiFunction<CompoundTag, HolderLookup.Provider, T>`。

### 为什么这么修复

`ContainerHelper` 是原版容器格式，直接替换成 `ItemStackHandler` 会改变线格式；保留分支是过渡方案。`SavedData` 的工厂签名不同，需要单独门面或调用点分支。

### 其他可能的修复方式

- 把容器统一迁到 `ItemStackHandler`，由 `HandlerNbt` 承接。改变存档形状，需要迁移。
- 为 `ContainerHelper` 与 `SavedData` 各加一个 `NbtCompat` 重载。调用点不再分支，但门面面积扩大。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
