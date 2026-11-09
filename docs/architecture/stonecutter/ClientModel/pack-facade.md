# Pack 与 geometry loader

## 子原因

`AbstractPackResources` 的构造、`Pack.readMetaAndCreate` 和 geometry loader 的 key 在两个版本上不同：

- 1.20.1：`AbstractPackResources(String, boolean)`；`Pack.readMetaAndCreate(String, ...)`；`ModelEvent.RegisterGeometryLoaders.register(String, loader)`。
- 1.21.1：`AbstractPackResources(PackLocationInfo)`；`Pack.readMetaAndCreate(PackLocationInfo, ...)`；`register(ResourceLocation, loader)`。

如果分支留在 pack 类和事件处理器里，调用点会同时出现构造、工厂和 key 三种差异。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*public SharedInventoryModelPack(PackLocationInfo info) { super(info); }
*///?} else {
public SharedInventoryModelPack(String id) { super(id, true); }
//?}

@SubscribeEvent
public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
//? if >=1.21.1 {
    /*event.register(id, loader);
*///?} else {
    event.register(id.getPath(), loader);
//?}
}
```

### 修复后

`ClientPackFacade` 收敛工厂与 geometry loader key：

```java
public static Pack createSharedInventoryPack() {
//? if >=1.21.1 {
    /*return Pack.readMetaAndCreate(new PackLocationInfo(...), ...);
*///?} else {
    return Pack.readMetaAndCreate("gt6ce:shared_inventory", ...);
//?}
}

public static void registerGeometryLoader(ModelEvent.RegisterGeometryLoaders event, ResourceLocation id,
                                          IGeometryLoader<SharedInventoryModelPack.Reference> loader) {
//? if >=1.21.1 {
    /*event.register(id, loader);
*///?} else {
    event.register(id.getPath(), loader);
//?}
}
```

pack 类的构造分支仍保留在 `SharedInventoryModelPack`，因为它继承加载器专属的 `AbstractPackResources`；工厂和 key 差异收进门面。

### 为什么这么修复

- 构造差异无法完全消除（基类签名不同），但可以限制在 pack 类内部。
- 工厂和 geometry loader key 是调用点差异，收进 `ClientPackFacade` 后调用点不再分支。
- 事件归属与顺序见 [events.md](events.md)。

### 其他可能的修复方式

- 在节点侧各写一个 pack 工厂。会制造两套入口。
- 在事件处理器里直接分支。key 差异容易漏，且 `AddPackFindersEvent` 与 `RegisterGeometryLoaders` 容易混在一起。

相关链接：[issue #361](https://github.com/GregTech6CE/GregTech6/issues/361)
