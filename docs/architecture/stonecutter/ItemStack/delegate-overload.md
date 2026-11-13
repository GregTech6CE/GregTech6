# delegate-overload

## 子原因

带 `HolderLookup.Provider` 的方法在 1.20.1 不需要 lookup。为省去 1.20 调用点传 `null`，常另写一个**单参委托重载**（`save(tag)` 转 `save(tag, null)`），用 `//?` 只在 1.20 侧编译。这类重载是落点偏差（便利委托），不是平台 API 运行时差异：真实现两侧共享，委托只是转发。

## 例子

### 修复前

`LogisticsCovers` 的共享真实现带 lookup，1.20 另挂单参委托：

```java
    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        // 共享真实现：NbtCompat.saveItem(face, lookup)
    }
//? if >=1.21.1 {
//?} else {
    public void save(CompoundTag tag) { save(tag, null); }
//?}
```

1.20 调用点（`LogisticsWireBlockEntity` 等）用单参版：

```java
//? if >=1.21.1 {
    /*covers.save(tag, lookup);
*///?} else {
    covers.save(tag);
//?}
```

### 修复后

删除单参委托，1.20 调用点直接传 `null`（`NbtCompat` 在 1.20 忽略 lookup）：

```java
    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        // 共享真实现
    }
```

```java
//? if >=1.21.1 {
    /*covers.save(tag, lookup);
*///?} else {
    covers.save(tag, null);
//?}
```

委托的 `//?` 块消失；调用点仍保留最小 provider 参数分支（见 [registry-access.md](registry-access.md)）。

### 为什么这么修复

先按调用者把委托分两类，再决定删留：

- **纯 1.20 委托**：调用者全在 1.20 分支（如 `LogisticsCovers.save(tag)` 只被 1.20 的 BE `load`/`saveAdditional` 调用）。删除委托、调用点改传 `null`，行为等价（1.20 忽略 lookup）。
- **共享调用者适配器**：带 lookup 的重载被**共享代码**调用（如 `CoverUtilityBehaviors.fluidFilterPermits(..., lookup)` 被 `FluidPipeBlockEntity` 无条件调用）。这类必须保留——删掉会让共享调用点重新长出 provider 分支，得不偿失。

判定前用 `rg` 对方法名全量核实调用者（含节点源码）；调用者清单遗漏会导致编译失败。保留的适配器按 [unification-policy.md](../unification-policy.md) 平台签名例外写明原因。

### 其他可能的修复方式

- 连无 lookup 真实现一起删，统一为带 lookup 单签名（`NbtCompat` 模式推广到封面过滤器）：需迁移全部调用点、改写若干真实现，成本高，留作后续完整签名统一。
- 保留单参委托：冗余落点，违反「每个家族一条共享路径」方针。