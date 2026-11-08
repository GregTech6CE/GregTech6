# 类型化组件

## 子原因

1.21.1 的部分数据不是自由 `CompoundTag`，而是强类型组件（`BLOCK_ENTITY_DATA`、`ENCHANTMENTS`、`CUSTOM_NAME` 等）。这些组件不能换成 `CUSTOM_DATA`，也不能进入共享签名。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
//?} else {
var data = stack.getTagElement("BlockEntityTag");
//?}
```

### 修复后

```java
//? if >=1.21.1 {
var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
//?} else {
var data = stack.getTagElement("BlockEntityTag");
//?}
```

分支保留在 1.21.1 侧；共享代码只接收提取后的 `CompoundTag` 或领域值。`PortableTankHandler` 的 `BLOCK_ENTITY_DATA`、`GTToolEnchantments` 的 `ENCHANTMENTS` 都按这个方式处理。

### 为什么这么修复

`BLOCK_ENTITY_DATA` 带方块实体 id，`ENCHANTMENTS` 带 `Holder<Enchantment>`，它们的语义与自由数据不同。换成 `CUSTOM_DATA` 会丢失放置语义或附魔解析。

### 其他可能的修复方式

- 把类型化组件序列化成 `CompoundTag` 再塞进 `CUSTOM_DATA`。会丢失类型与 codec 校验。
- 在共享层引入 `DataComponentType`。1.20.1 没有这个类型，无法编译。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
