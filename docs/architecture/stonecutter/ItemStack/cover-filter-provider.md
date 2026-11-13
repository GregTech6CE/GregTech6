# 封面过滤器 provider 统一

## 子原因

`CoverUtilityBehaviors` 的六个过滤器方法——`itemFilter` / `fluidFilter`、`itemFilterPermits` / `fluidFilterPermits`、`setItemFilter` / `setFluidFilter`——读写封面过滤器模板。1.21.1 的栈 codec 需要 `HolderLookup.Provider`，1.20.1 不需要，于是每个方法都长出四类分支：方法签名分支（带/不带 lookup）、1.20 委托适配器（带 lookup 转调无 lookup）、内部 codec 分支（`CoverStackData.readItem`/`FluidStackNbt.read`/`save(lookup)` ↔ `ItemStack.of`/`loadFluidStackFromNBT`/`save(new CompoundTag)`）、以及调用点的 `level.registryAccess()` 参数分支。它们是同一落点偏差的连锁表现，不是行为差异。

## 例子

### 修复前

`itemFilter` 同时有签名分支、内部 codec 分支与 1.20 委托适配器：

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

六个方法同样收敛；调用点两节点统一传 `level.registryAccess()`，参数分支消失。

### 为什么这么修复

`NbtCompat` 在 1.20.1 忽略 lookup（`readItem`→`ItemStack.of`、`readFluid`→`loadFluidStackFromNBT`、`saveItem`/`saveFluid`→无 provider 写法），故带 lookup 的单一签名两节点通用，签名分支、委托适配器、内部 codec 分支、调用点参数分支一并消除。`NbtCompat.readFluid` 含 legacy `FluidName`/`Amount` 信封，流体过滤与储罐解码不丢旧存档兼容（`FluidStackNbt` 节点类因此删除）。

调用点迁移面：`ComponentCoverAccess`（物品/流体能力包装）、`LogisticsItemRouter`/`LogisticsFluidRouter`、`BasicMachineBlockEntity`、`ItemPipeBlockEntity`、`CoverDisplayInfo`。两处边界：

- `CoverDisplayInfo` 保留 `level != null` 守卫——1.21 codec 要求非空 lookup，tooltip 上下文可能无 level；1.20 无 level 时不再显示过滤器目标（次要回退）。
- `FluidTankGT.readFromNBT` 的 1.20 签名本身不带 lookup，故流体读取保留 `lookup`/`null` 最小分支（仍走 `NbtCompat.readFluid`）。

委托重载按调用者分类：纯 1.20 委托（调用者全在 1.20 分支）直接删、调用点传 `null`；被共享代码调用的方法统一签名，不保留适配器。判定前用 `rg` 全量核实调用者。

### 其他可能的修复方式

- 保留 1.20 委托适配器、仅消调用点分支：适配器本身仍是分支，签名统一后不需要。
- 保留调用点最小 provider 分支：签名可统一时冗余（`FluidTankGT` 因签名所限除外）。
- 共享层包 `RegistryContext` 门面：需调用点迁移，成本更高。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)、[registry-access.md](registry-access.md)