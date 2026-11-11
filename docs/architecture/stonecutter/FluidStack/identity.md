# FluidStack identity

## 子原因

1.20.1 用 `isFluidEqual` 比较流体与 tag；1.21.1 用 `isSameFluidSameComponents` 比较整个组件栈。

## 例子

### 修复前

```java
//? if >=1.21.1 {
return FluidStack.isSameFluidSameComponents(first, second);
//?} else {
return first.isFluidEqual(second);
//?}
```

### 修复后

```java
public static boolean sameFluid(FluidStack first, FluidStack second) {
//? if >=1.21.1 {
    return FluidStack.isSameFluidSameComponents(first, second);
//?} else {
    return first.isFluidEqual(second);
//?}
}
```

调用点：

```java
if (StackCompat.sameFluid(tank.getFluidInTank(0), stack)) { ... }
```

### 为什么这么修复

这是真实语义差异的收敛，不是简单折叠。门面保持两侧同义，调用点不需要知道组件模型。

### 其他可能的修复方式

- 调用点各自分支。`isFluidEqual` 与 `isSameFluidSameComponents` 容易混用。
- 只比较流体类型，忽略 tag。会改变配方与物流匹配行为。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
