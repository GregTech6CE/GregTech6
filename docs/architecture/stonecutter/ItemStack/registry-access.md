# registryAccess 参数

## 子原因

1.21.1 的物品/流体栈 codec 需要 `HolderLookup.Provider`，1.20.1 不需要。同一方法因此长出三类分支：方法签名分支（带/不带 lookup）、1.20 便利委托重载（`itemFilter(cover)` 转 `itemFilter(cover, null)`）、调用点的 `level.registryAccess()` 参数分支。它们都是同一落点偏差的表现，不是行为差异。

## 例子

### 修复前

`CoverUtilityBehaviors.itemFilter` 同时有签名分支、内部 codec 分支与 1.20 委托适配器：

```java
//? if >=1.21.1 {
    /*public static ItemStack itemFilter(ItemStack cover, HolderLookup.Provider lookup) {
*///?} else {
    public static ItemStack itemFilter(ItemStack cover) {
//?}
        CompoundTag tag = cover == null ? null : StackData.read(cover);
        if (tag == null || !tag.contains(FILTER_ITEM_KEY)) return ItemStack.EMPTY;
//? if >=1.21.1 {
        /*return CoverStackData.readItem(lookup, tag.getCompound(FILTER_ITEM_KEY));
*///?} else {
        return ItemStack.of(tag.getCompound(FILTER_ITEM_KEY));
//?}
    }
//? if >=1.21.1 {
//?} else {
    public static ItemStack itemFilter(ItemStack cover, HolderLookup.Provider lookup) {
        return itemFilter(cover);
    }
//?}
```

### 修复后

单一带 lookup 签名，内部 codec 走 `NbtCompat`，委托适配器删除：

```java
    public static ItemStack itemFilter(ItemStack cover, HolderLookup.Provider lookup) {
        CompoundTag tag = cover == null ? null : StackData.read(cover);
        if (tag == null || !tag.contains(FILTER_ITEM_KEY)) return ItemStack.EMPTY;
        return NbtCompat.readItem(tag.getCompound(FILTER_ITEM_KEY), lookup);
    }
```

调用点两节点统一 `itemFilter(cover, level.registryAccess())`，参数分支随之消失。

### 为什么这么修复

`NbtCompat` 在 1.20.1 忽略 lookup（`readItem` 走 `ItemStack.of`、`readFluid` 走 `loadFluidStackFromNBT`），故带 lookup 的单一签名两节点通用：签名分支、委托适配器、内部 codec 分支、调用点参数分支一并消除。`NbtCompat.readFluid` 已含 legacy `FluidName`/`Amount` 信封，流体过滤解码不丢旧存档兼容。

委托重载按调用者分两类处置：纯 1.20 委托（调用者全在 1.20 分支，如 `LogisticsCovers.save(tag)`）直接删、调用点传 `null`；被共享代码调用的方法统一为带 lookup 签名（不再保留适配器）。判定前用 `rg` 全量核实调用者。`CoverDisplayInfo` 等 tooltip 上下文保留 `level != null` 守卫（1.21 codec 要求非空 lookup）。

### 其他可能的修复方式

- 保留 1.20 委托适配器、仅消调用点分支：适配器本身仍是分支，治标不治本，签名统一后不需要。
- 保留调用点最小 provider 分支：签名可统一时冗余。
- 共享层包 `RegistryContext` 门面：需调用点迁移，成本更高。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)