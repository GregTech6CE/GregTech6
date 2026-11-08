# 薄覆盖

## 子原因

两个版本的 `saveAdditional` / `load` 签名不同，但持久化字段与顺序相同。如果两个分支各写一份持久化逻辑，任何字段改动都要改两处。

## 例子

### 修复前

```java
//? if >=1.21.1 {
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.put("Inventory", inventory.serializeNBT(lookup));
    tag.putBoolean("GTLootGenerated", lootGenerated);
}
//?} else {
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.put("Inventory", inventory.serializeNBT());
    tag.putBoolean("GTLootGenerated", lootGenerated);
}
//?}
```

### 修复后

```java
//? if >=1.21.1 {
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    saveData(tag, lookup);
}
//?} else {
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    saveData(tag, null);
}
//?}

private void saveData(CompoundTag tag, HolderLookup.Provider lookup) {
    tag.put("Inventory", HandlerNbt.save(inventory, lookup));
    tag.putBoolean("GTLootGenerated", lootGenerated);
}
```

### 为什么这么修复

override 的签名由基类固定，无法共享；方法体只依赖 `CompoundTag` 与 `HolderLookup.Provider`，这两个类型在 1.20.1 都存在，因此可以抽成共享方法。1.20.1 不使用 provider，传 `null`。

### 其他可能的修复方式

- 两个节点各写一份完整持久化逻辑（来源：Townstead `ServingPlateBlockEntity`）。只在差异确实无法下沉时使用。
- 在节点侧保留薄覆盖，方法体委托共享方法（来源：GT Modern `ManagedSyncBlockEntity`）。与本方案相同。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
