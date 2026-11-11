# 标签目录

## 子原因

1.20.1 使用 `tags/items`、`tags/blocks`、`tags/fluids`；1.21.1 使用 `tags/item`、`tags/block`、`tags/fluid`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
return "tags/item";
//?} else {
return "tags/items";
//?}
```

### 修复后

以节点实际读取的目录为准，不能把一边的 JSON 路径直接拷给另一边。

### 为什么这么修复

这是资源路径差异，不是 Java 分支。折叠时以节点实际读取的目录为准。

### 其他可能的修复方式

- 统一成一种目录名。会改变资源加载路径。
- 在构建期重写目录。需要额外的资源转换步骤。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
