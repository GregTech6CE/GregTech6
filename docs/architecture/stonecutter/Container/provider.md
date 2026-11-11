# Container provider

## 子原因

`ContainerHelper.saveAllItems` / `loadAllItems` 在 1.21.1 上多一个 `HolderLookup.Provider`；1.20.1 没有。

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

当前 `NbtCompat` 还没有覆盖 `ContainerHelper`，调用点保留分支。补门面时注意 `loadAllItems` 在两个节点都返回 `void`。

```java
//? if >=1.21.1 {
ContainerHelper.loadAllItems(tag, outputs, lookup);
//?} else {
ContainerHelper.loadAllItems(tag, outputs);
//?}
```

### 为什么这么修复

`ContainerHelper` 是原版容器格式，直接替换成 `ItemStackHandler` 会改变线格式；保留分支是过渡方案。

### 其他可能的修复方式

- 把容器统一迁到 `ItemStackHandler`，由 `HandlerNbt` 承接。改变存档形状，需要迁移。
- 为 `ContainerHelper` 加 `NbtCompat` 重载。调用点不再分支。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
