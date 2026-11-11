# ResourceLocation

## 子原因

1.20.1 用 `GregTech.id(path)`；1.21.1 用 `ResourceLocation.fromNamespaceAndPath("gt6ce", path)`。`GregTech.MODID` / `NAMESPACE` 与 `GregTechIdentity.MOD_ID` / `REGISTRY_NAMESPACE` 也是同一组常量。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
//?} else {
var id = GregTech.id(path);
//?}
```

### 修复后

收进共享的 `ResourceLocation` 辅助方法或 `GregTechIdentity` 常量。

### 为什么这么修复

两者语义相同；共享 helper 可能统一 `toLowerCase()`，迁移前核对调用点是否依赖原始大小写。

### 其他可能的修复方式

- 调用点各自分支。id 构造点数量多，分支会扩散。
- 在节点侧各写一套 helper。会制造两套入口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
