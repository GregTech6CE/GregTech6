# FluidStack storage

## 子原因

NeoForge 1.21.1 的 `FluidStack` 也已组件化，`getTag()` 不再可用；1.20.1 仍使用 `CompoundTag`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var data = stack.getComponentsPatch();
//?} else {
var data = stack.getTag();
//?}
```

### 修复后

需要自由数据时，统一通过 `StackCompat` 或调用方自己的门面读取；不要把 `FluidStack` 的组件直接暴露给共享代码。

```java
//? if >=1.21.1 {
var patch = stack.getComponentsPatch();
//?} else {
var tag = stack.getTag() == null ? null : stack.getTag().copy();
//?}
```

### 为什么这么修复

`FluidStack` 的组件与 `ItemStack` 的 `CUSTOM_DATA` 不是同一套 API；共享代码只应依赖门面提取后的值。

### 其他可能的修复方式

- 把 `FluidStack` 的 tag 当作字符串比较。会丢失组件语义。
- 在调用点各自判断组件。分支会扩散。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
