# 菜单目录

## 子原因

同一菜单在两个版本各有一套注册路径：1.21.1 把 `basic_machine` / `behavior_machine` / `part` 放在节点类 `BasicMachineRegistries`，1.20.1 放在共享类 `GTMenuTypes`；字段类型在 `DeferredHolder` 与 `RegistryObject` 之间切换；菜单工厂在 `IMenuTypeExtension.create` 与 `IForgeMenuType.create` 之间切换。菜单 ID 没有版本差异；构造器多数相同，`BasicMachineContainerMenu` 的客户端构造器在 1.21.1 接收 `RegistryFriendlyByteBuf`，是例外。

## 例子

### 修复前

1.21.1 的 `GTMenuTypes` 只做转发：

```java
public static final DeferredHolder<MenuType<?>, MenuType<BasicMachineContainerMenu>> BASIC_MACHINE =
        gregtech.platform.neoforge.machine.BasicMachineRegistries.MENU;
```

1.20.1 的 `GTMenuTypes` 自己注册，字段类型不同：

```java
public static final RegistryObject<MenuType<BasicMachineContainerMenu>> BASIC_MACHINE =
        MENU_TYPES.register("basic_machine",
                () -> IForgeMenuType.create((w, i, b) -> new BasicMachineContainerMenu(w, i, b)));
```

### 修复后

`GTMenuTypes` 成为两个节点唯一的菜单目录，字段统一为 `Supplier<MenuType<T>>`：

```java
public static final Supplier<MenuType<BasicMachineContainerMenu>> BASIC_MACHINE =
        MENU_TYPES.register("basic_machine", GTMenuTypes::createBasicMachine);

public static final Supplier<MenuType<BehaviorMachineMenu>> BEHAVIOR_MACHINE = MENU_TYPES.register(
        "behavior_machine",
        () -> create((id, inventory, buffer) -> new BehaviorMachineMenu(id, inventory, buffer)));

public static final Supplier<MenuType<PartMenu>> PART = MENU_TYPES.register(
        "part", () -> create((id, inventory, buffer) -> new PartMenu(id, inventory, buffer)));
```

平台工厂只留在一个小助手：

```java
private static <T extends AbstractContainerMenu> MenuType<T> create(MenuFactory<T> factory) {
//? if >=1.21.1 {
    return IMenuTypeExtension.create(factory::create);
//?} else {
    return IForgeMenuType.create(factory::create);
//?}
}

@FunctionalInterface
private interface MenuFactory<T extends AbstractContainerMenu> {
    T create(int id, Inventory inventory, FriendlyByteBuf buffer);
}
```

`MENU_TYPES` 声明和 `IEventBus` 类型仍是平台差异：

```java
//? if >=1.21.1 {
/*public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, REGISTRY_NAMESPACE);
*///?} else {
public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, REGISTRY_NAMESPACE);
//?}

public static void submit(IEventBus bus) {
    MENU_TYPES.register(bus);
}
```

`BasicMachineContainerMenu` 在 1.21.1 的客户端构造器接收 `RegistryFriendlyByteBuf`，不是 `FriendlyByteBuf`：

```java
//? if >=1.21.1 {
/*public BasicMachineContainerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
*///?} else {
public BasicMachineContainerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
//?}
```

通用 `MenuFactory` 的参数是 `FriendlyByteBuf`，不能直接传给这个构造器，因此 `BASIC_MACHINE` 单独走一个助手：

```java
//? if >=1.21.1 {
/*private static MenuType<BasicMachineContainerMenu> createBasicMachine() {
    return IMenuTypeExtension.create(BasicMachineContainerMenu::new);
}
*///?} else {
private static MenuType<BasicMachineContainerMenu> createBasicMachine() {
    return IForgeMenuType.create(
            (id, inventory, buffer) -> new BasicMachineContainerMenu(id, inventory, buffer));
}
//?}
```

`BasicMachineRegistries` 只保留 capability 与 payload 接线。

### 为什么这么修复

菜单 ID 与构造器只写一次，两个节点的菜单列表不会漂移；节点类不再持有第二套菜单注册表，`submit` 只需提交共享的 `MENU_TYPES`。平台差异集中在 `create` 与 `createBasicMachine` 两个工厂助手里，调用点不再分支。

### 其他可能的修复方式

- 定义 `RegisteredMenu` record（`ResourceKey` + `Supplier`），彻底隐藏 holder 类型；需要 `getKey()` 的调用点改读 record。
- 保留节点菜单注册类，只在调用点折叠。菜单列表仍可能漂移。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
