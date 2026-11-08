# BlockEntity 的跨版本持久化

返回[示例索引](README.md)。本文说明 `saveAdditional` / `loadAdditional` / `getUpdateTag` 在两个版本上的签名差异，以及如何把方法体收敛到共享代码。

## 问题来源

1.20.5 起，方块实体的自定义读写与同步回调都增加了注册表上下文 `HolderLookup.Provider`，同时 `load` 改名 `loadAdditional`。

| 用途 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 自定义写 | `protected saveAdditional(CompoundTag)` | `protected saveAdditional(CompoundTag, HolderLookup.Provider)` |
| 自定义读 | `public load(CompoundTag)` | `protected loadAdditional(CompoundTag, HolderLookup.Provider)` |
| 同步 tag | `public getUpdateTag()` | `public getUpdateTag(HolderLookup.Provider)` |
| 静态加载 | `loadStatic(BlockPos, BlockState, CompoundTag)` | `loadStatic(BlockPos, BlockState, CompoundTag, HolderLookup.Provider)` |
| 物品化 | `saveToItem(ItemStack)` | `saveToItem(ItemStack, HolderLookup.Provider)` |

这些方法是 vanilla 回调，签名由基类固定，共享代码无法统一 override。差异不能靠“换一个调用”消除，只能把方法体移出分支。

`loadAdditional` 执行时方块实体可能还没有 `level`（`loadStatic` 只读档，`level` 由调用方稍后 `setLevel`），因此不能在方法里调用 `level.registryAccess()`，必须使用传入的 provider。

## 建议方案

保留 override 的签名分支，方法体抽成共享的 `saveData(CompoundTag, HolderLookup.Provider)` / `loadData(...)`：

- 1.21.1 分支把 `lookup` 原样传给共享方法。
- 1.20.1 分支传 `null`；共享方法只在 1.21.1 的底层门面里断言非空。
- 库存走 `HandlerNbt`，物品/流体栈走 `NbtCompat`，覆盖板数据走 `CoverStackData`，或 `ComponentCoverStorage` 的 `save(CompoundTag, Provider)` / `load(CompoundTag, Provider)`。
- 同一个持久化逻辑只写一份，不在 override 与 helper 里各写一遍。

## 例子与分析

### 薄覆盖与共享方法体

`MetalChestBlockEntity` 的写路径：

```java
//? if >=1.21.1 {
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    componentCovers.save(tag, lookup);
    saveData(tag, lookup);
}
//?} else {
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    componentCovers.save(tag);
    saveData(tag, null);
}
//?}

private void saveData(CompoundTag tag, HolderLookup.Provider lookup) {
    tag.put("Inventory", HandlerNbt.save(inventory, lookup));
    tag.putBoolean("GTLootGenerated", lootGenerated);
    // ...
}
```

读路径同理：1.21.1 的 `loadAdditional(tag, lookup)` 与 1.20.1 的 `load(tag)` 除 `super` 与 `componentCovers.load` 外，持久化逻辑都收进 `loadData(tag, lookup)` 或 `loadData(tag, null)`。

修改前后的差别不在 `saveData` 的内容，而在 override 的职责：修改前两个分支各写一份持久化逻辑，修改后分支只负责适配签名。

### `codecLookup` 别名

`BasicMachineBlockEntity` 的持久化逻辑较长，用一个局部别名避免每个调用点分支：

```java
@Override

//? if >=1.21.1 {
public void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    HolderLookup.Provider codecLookup = lookup;
//?} else {
public void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    HolderLookup.Provider codecLookup = null;
//?}

    tag.put(NBT_INVENTORY, HandlerNbt.save(inventory.items(), codecLookup));
    tag.put("gt.potion_input", HandlerNbt.save(inventory.potionInput(), codecLookup));
    NbtCompat.writeTank(inventory.tanksInput()[i], t, codecLookup);
    // ...
}
```

片段省略了 `if (inventory != null)` 与 `if (inventory.potionInput() != null)` 守卫。

`codecLookup` 只在签名分支里赋值，共享方法体只引用它。这样分支数量等于 override 数量，不随持久化字段增长。

### 同步 tag

`getUpdateTag` 与 `handleUpdateTag` 同样按节点分支。1.21.1 的 provider 来自区块包上下文或 `level.registryAccess()`；不要在方法内缓存到静态字段。

### 分析

- **为什么 override 包不住**：签名由 `BlockEntity` 基类决定，共享代码不能声明一个“两边都成立”的 override。
- **为什么 `saveData` 可以共享**：它只依赖 `CompoundTag` 与 `HolderLookup.Provider`，这两个类型在 1.20.1 都存在；1.20.1 只是不使用 provider。
- **为什么不能读 `level`**：`loadAdditional` 可能先于 `setLevel` 执行，`level` 为 `null`。provider 必须由调用方传入。
- **为什么 1.20.1 可以传 `null`**：底层门面在 1.20.1 分支不使用该参数，也不做非空断言。

相关契约见 issue [#358](https://github.com/GregTech6CE/GregTech6/issues/358) 与 [NBT.md](NBT.md)。
