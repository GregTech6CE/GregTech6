# 能力获取

## 子原因

1.20.1 通过 `ForgeCapabilities.FLUID_HANDLER` 与 `LazyOptional` 获取流体能力；1.21.1 通过 `Capabilities.FluidHandler` 与 `Optional`。`FluidUtil` 的返回类型也不同。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
//?} else {
var handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
//?}
```

### 修复后

调用点保留分支，或收进 `StackCompat`：

```java
public static IFluidHandlerItem fluidHandler(ItemStack stack) {
//? if >=1.21.1 {
    return FluidUtil.getFluidHandler(stack).orElse(null);
//?} else {
    return FluidUtil.getFluidHandler(stack).resolve().orElse(null);
//?}
}
```

### 为什么这么修复

`LazyOptional` 与 `Optional` 的解析方式不同；`FluidUtil` 在 1.21.1 直接返回 `Optional`。门面统一返回 `IFluidHandlerItem` 或 `null`，调用点不需要判断。

### 其他可能的修复方式

- 调用点各自处理 `LazyOptional`。容易漏 `resolve()` 或 `orElse(null)`。
- 统一改成 `Optional`。1.20.1 的 `LazyOptional` 无法直接转换。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
