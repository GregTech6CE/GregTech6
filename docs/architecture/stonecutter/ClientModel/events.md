# 模型事件注册

## 子原因

模型事件在两个加载器上使用不同的注解、事件类和优先级类型：

- 类级订阅：1.21.1 `@EventBusSubscriber`，1.20.1 `@Mod.EventBusSubscriber`；两者都必须显式 `bus = MOD`、`value = Dist.CLIENT`。
- 事件类：`net.neoforged.neoforge.client.event.ModelEvent` 与 `net.minecraftforge.client.event.ModelEvent`。
- 注解：`net.neoforged.bus.api.SubscribeEvent` 与 `net.minecraftforge.eventbus.api.SubscribeEvent`。
- 优先级：`net.neoforged.bus.api.EventPriority` 与 `net.minecraftforge.eventbus.api.EventPriority`。

如果每个模型类各自订阅，这段注解分支会散落在 20 多个类里，执行顺序只能靠 `EventPriority` 和事件总线注册顺序隐式保证。

## 例子

### 修复前

每个模型类各自带类级订阅注解和模型方法注解：

```java
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class FooClientModels {
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ...
    }
}
```

`ConfiguredAlphaModels` 只能靠 `LOWEST` 保证最后执行；新增 handler 的顺序取决于类扫描顺序。

### 修复后

模型事件集中到一个客户端 bootstrap；模型类只保留 `public static` handler：

```java
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class ClientModelBootstrap {
    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        FooClientModels.registerSharedModels(event);
        ...
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BlockItemModelHelper.captureFromVanillaBlockItem(BakedModelMap.of(event.getModels()));
        FooClientModels.onModifyBakingResult(event);
        ...
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void modifyBakingResultLow(ModelEvent.ModifyBakingResult event) {
        PortableFluidItemModels.models(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyBakingResultLowest(ModelEvent.ModifyBakingResult event) {
        ConfiguredAlphaModels.models(event);
    }
}
```

节点专属 handler 用同包类名的 `//?` 分支列出，不在共享源码顶层 import 节点类：

```java
//? if >=1.21.1 {
/*private static void modifyPlatformBakingResult(ModelEvent.ModifyBakingResult event) {
    AxleClientSetup.models(event);
    ...
}
*///?} else {
private static void modifyPlatformBakingResult(ModelEvent.ModifyBakingResult event) {
    GearboxClientModels.onModifyBakingResult(event);
    ...
}
//?}
```

### 为什么这么修复

- 模型事件的注解分支只写一次，顺序显式：block-item transforms 最前，NORMAL handler 全部在 `LOW`/`LOWEST` 之前，alpha 最后。
- 纯模型类可以删掉类级订阅注解和 `Dist`/`Mod`/`EventBusSubscriber`/`SubscribeEvent` 的 import。
- 混合类（颜色、渲染器、`FMLClientSetupEvent`）保留类级注解和非模型方法，只删模型方法的 `@SubscribeEvent`；不要整类去注解，也不按节点拆分类。
- `AddPackFindersEvent` 不是 `ModelEvent`，留在 `SharedInventoryModelPack.packs`；`RegisterGeometryLoaders` 是模型事件，由 bootstrap 调用 `SharedInventoryModelPack.loaders`。
- 类级注解的 `//?` 分支位置见[用法](../usage.md)的“类级注解紧贴类声明”。

### 其他可能的修复方式

- 保留每个类自己的订阅，只折叠注解分支。顺序仍然隐式，新增 handler 容易插错位置。
- 在节点侧各写一个 bootstrap。会制造两套入口，平台 handler 仍要分支。
- 用 `DistExecutor` 在共享类里直接引用节点类。模型事件是客户端专用，类级 `value = Dist.CLIENT` 已经足够。

相关链接：[issue #361](https://github.com/GregTech6CE/GregTech6/issues/361)
