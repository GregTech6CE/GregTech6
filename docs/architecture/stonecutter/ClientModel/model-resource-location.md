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

集中到模型门面（`BakedModelMap` 或 `ClientModelEvents`）。

### 为什么这么修复

这类差异不适合在调用点逐个折叠；集中后，调用点不再关心版本。迁移时先核对模型路径与 inventory 变体。

### 其他可能的修复方式

- 调用点各自分支。模型分支形态多，容易漏 inventory 变体。
- 在节点侧各写一套模型 helper。会制造两套入口。

## RegisterAdditional

1.20.1 的 `ModelEvent.RegisterAdditional.register` 接受普通 `ResourceLocation`；1.21.1 需要 `ModelResourceLocation` 的 `STANDALONE_VARIANT`。1.20.1 的 `ModelBakery` 会把非 `inventory` variant 的 `ModelResourceLocation` 当作 blockstate 定义，不能直接复用 1.21.1 的写法。

### 修复前

```java
//? if >=1.21.1 {
/*event.register(new ModelResourceLocation(id, ModelResourceLocation.STANDALONE_VARIANT));
*///?} else {
event.register(new ModelResourceLocation(id, ""));
//?}
```

### 修复后

```java
ClientModelEvents.registerAdditional(event, id);
```

`ClientModelEvents` 内部保留唯一分支：1.20.1 `event.register(id)`，1.21.1 `event.register(new ModelResourceLocation(id, STANDALONE_VARIANT))`。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)、[issue #365](https://github.com/GregTech6CE/GregTech6/issues/365)
