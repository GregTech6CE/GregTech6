# 显示绑定

## 子原因

`FluidDisplayBinding` 用物品承载流体显示数据。1.21.1 的 `display` / `resolve` / `subtype` 需要 provider，且当前把 provider 放在第一个参数；1.20.1 没有 provider。

## 例子

### 修复前

```java
//? if >=1.21.1 {
public static ItemStack display(HolderLookup.Provider lookup, FluidStack fluid) { ... }
//?} else {
public static ItemStack display(FluidStack fluid) { ... }
//?}
```

### 修复后

签名保留分支；如果要把 provider 统一到最后一位，调用点也要同步调整：

```java
//? if >=1.21.1 {
public static ItemStack display(FluidStack fluid, HolderLookup.Provider lookup) { ... }
//?} else {
public static ItemStack display(FluidStack fluid) { ... }
//?}
```

### 为什么这么修复

provider 是序列化上下文，不是业务参数。放在最后一位可以让共享调用点只多一个实参；当前放在第一位是历史遗留，迁移时应统一。

### 其他可能的修复方式

- 保留 provider 在第一位。调用点仍然要按版本分支。
- 用 `currentLookup()` 在方法内取 provider。客户端/服务端快照不同，且无 level 时不可靠。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
