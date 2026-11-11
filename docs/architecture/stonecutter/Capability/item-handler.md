# Item handler

## 子原因

1.20.1 通过 `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` 与 `LazyOptional` 获取物品能力；1.21.1 通过 `Capabilities.ItemHandler.BLOCK` 与 `Optional`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
//?} else {
var handler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
//?}
```

### 修复后

调用点保留分支，或收进 `ContentRegistrar` / 平台门面。

### 为什么这么修复

`LazyOptional` 与 `Optional` 的解析方式不同；能力注册位置也不同。

### 其他可能的修复方式

- 调用点各自处理 `LazyOptional`。容易漏 `resolve()`。
- 统一改成 `Optional`。1.20.1 的 `LazyOptional` 无法直接转换。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
