# BakedModelMap

## 子原因

`ModelEvent.ModifyBakingResult.getModels()` 的 key 类型不同：

- 1.20.1：`Map<ResourceLocation, BakedModel>`；`RegisterAdditional` 也把 plain `ResourceLocation` 作为 key。
- 1.21.1：`Map<ModelResourceLocation, BakedModel>`。

如果每个调用点各自声明 map 类型并手写 variant 回退，分支会散落在所有模型类里。missing 哨兵的常量名不同（1.20.1 `MISSING_MODEL_LOCATION`，1.21.1 `MISSING_MODEL_VARIANT`），但两版 map 中的 key 值相同；只做 `!= null` 检查会把哨兵当成有效模型。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*Map<ModelResourceLocation, BakedModel> models = event.getModels();
BakedModel model = models.get(ModelResourceLocation.standalone(id));
*///?} else {
Map<ResourceLocation, BakedModel> models = event.getModels();
BakedModel model = models.get(new ModelResourceLocation(id, "inventory"));
//?}
if (model != null) {
    ...
}
```

### 修复后

```java
BakedModelMap models = BakedModelMap.of(event.getModels());
BakedModel model = models.find(id); // 拒绝 missing 哨兵
```

`BakedModelMap` 内部用 raw `Map<Object, BakedModel>` 持有事件 map；`get`/`getPlain`/`find` 全部拒绝 missing 哨兵，`missing()` 是唯一原始访问器。`replaceAll` 的 id 还原保留一个 `//?` 分支：

```java
//? if >=1.21.1 {
/*private static ResourceLocation modelId(Object key) {
    return ((ModelResourceLocation) key).id();
}
*///?} else {
private static ResourceLocation modelId(Object key) {
    return (ResourceLocation) key;
}
//?}
```

### 为什么这么修复

- 调用点不再声明 map key 类型，也不再手写 `standalone`/`inventory`/`""` 回退。
- `getPlain` 保留给 1.20.1 的 plain key；`find` 先 plain 再 variant。
- 所有 lookup 默认拒绝 missing 哨兵；`missing()` 只给 `ConfiguredAlphaModels` 的身份比较使用。

### 其他可能的修复方式

- 每个调用点各自分支。模型类多，容易漏 `inventory` 或 plain key。
- 只在 1.21.1 用 `ModelResourceLocation`，1.20.1 用 `ResourceLocation`。调用点仍要两套代码。
- 把 missing 哨兵判断留给调用点。旁路多，资源包给错模型引用时会被包装成正常模型。

相关链接：[issue #361](https://github.com/GregTech6CE/GregTech6/issues/361)
