# ModelResourceLocation

## 子原因

`ModelResourceLocation` 的构造在两个版本上不同，且分支形态多（独立构造、inventory 变体、事件 API）。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var id = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
//?} else {
var id = new ModelResourceLocation(new ResourceLocation("gt6ce", path), "inventory");
//?}
```

### 修复后

集中到模型门面（如 `BlockItemClientModels`）或共享构造 helper。

### 为什么这么修复

这类差异不适合在调用点逐个折叠；集中后，调用点不再关心版本。迁移时先核对模型路径与 inventory 变体。

### 其他可能的修复方式

- 调用点各自分支。模型分支形态多，容易漏 inventory 变体。
- 在节点侧各写一套模型 helper。会制造两套入口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
