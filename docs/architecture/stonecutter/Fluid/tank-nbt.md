# 罐体 NBT

## 子原因

`FluidTankGT` 与 `FluidStack` 的读写入口在 1.21.1 上多一个 provider 参数；1.20.1 仍使用无参方法。

## 例子

### 修复前

```java
//? if >=1.21.1 {
tank.writeToNBT(tag, lookup);
//?} else {
tank.writeToNBT(tag);
//?}
```

### 修复后

```java
NbtCompat.writeTank(tank, tag, lookup);
NbtCompat.readTank(tank, tag, lookup);
```

`NbtCompat` 内部：

```java
public static void writeTank(FluidTankGT tank, CompoundTag tag, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
    tank.writeToNBT(tag, provider(lookup));
//?} else {
    tank.writeToNBT(tag);
//?}
}
```

### 为什么这么修复

罐体是方块实体持久化的一部分，provider 必须由调用方传入。门面统一签名后，方块实体不需要为每个罐体写分支。

### 其他可能的修复方式

- 在方块实体里逐个罐体分支。分支数量随罐体数量增长。
- 把罐体改成 `ItemStackHandler` 风格。改变存档格式，需要迁移。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
