# FluidStack provider

## 子原因

1.20.1 的 `FluidStack#writeToNBT(CompoundTag)` / `loadFluidStackFromNBT(CompoundTag)` 不接收 provider；1.21.1 改为 `save(HolderLookup.Provider)` / `parseOptional(Provider, CompoundTag)`。`FluidTankGT` 同样多一个 provider 参数。

## 例子

### 修复前

```java
//? if >=1.21.1 {
tag.put("Fluid", fluid.save(lookup));
//?} else {
tag.put("Fluid", fluid.writeToNBT(new CompoundTag()));
//?}
```

### 修复后

```java
tag.put("Fluid", NbtCompat.saveFluid(fluid, lookup));
```

罐体：

```java
NbtCompat.writeTank(tank, tag, lookup);
NbtCompat.readTank(tank, tag, lookup);
```

### 为什么这么修复

调用点不需要判断版本；`NbtCompat` 内部在 1.21.1 分支断言 provider 非空，1.20.1 分支忽略它。

### 其他可能的修复方式

- 每个调用点各自分支。罐体数量多时分支会扩散。
- 把 provider 存成字段。provider 随世界/连接变化，字段会在重载后过期。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
