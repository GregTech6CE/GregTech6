# registryAccess 参数

## 子原因

1.21.1 的栈 codec 需要 `HolderLookup.Provider`，1.20.1 不需要。共享方法因此常带一个 1.20 便利委托重载（无 lookup 转调带 lookup，或反之），调用点也多出 `level.registryAccess()` 参数分支。这是落点偏差，不是行为差异。codec 本身的签名/返回类型差异见 [provider.md](provider.md)；封面过滤器的完整签名统一见 [cover-filter-provider.md](cover-filter-provider.md)。

## 例子

### 修复前

`LogisticsCovers.save` 共享真实现带 lookup，1.20 另挂单参委托，调用点按版本分支：

```java
    public void save(CompoundTag tag, HolderLookup.Provider lookup) {
        // 共享真实现：NbtCompat.saveItem(face, lookup)
    }
//? if >=1.21.1 {
//?} else {
    public void save(CompoundTag tag) { save(tag, null); }
//?}
```

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
    public void save(CompoundTag tag, HolderLookup.Provider lookup) {
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

### 为什么这么修复

委托重载按调用者分两类：纯 1.20 委托（调用者全在 1.20 分支，如此例）直接删、调用点传 `null`；被共享代码调用的方法应统一为带 lookup 的单一签名（不保留适配器），见 [cover-filter-provider.md](cover-filter-provider.md)。判定前用 `rg` 全量核实调用者（含节点源码），遗漏会导致编译失败。调用点保留的最小 provider 参数分支属平台签名例外。

### 其他可能的修复方式

- 保留单参委托：冗余落点，违反「每个家族一条共享路径」方针。
- 共享层包 `RegistryContext` 门面：需调用点迁移，成本更高。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)