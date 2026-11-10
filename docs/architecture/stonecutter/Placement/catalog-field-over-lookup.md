# 目录字段优先

## 子原因

1.20.1 用 `ForgeRegistries.BLOCKS.getValue(...)` 按 ID 查找方块，1.21.1 用共享目录字段。两侧都有目录字段时，按 ID 查找是旧路径的残留，还会引入 `ResourceLocation` 和该条目的注册表查找依赖。

本篇处理单条 BE 注册的按 ID 查找；整个引用目录门面的形态见 [reference-catalogs.md](reference-catalogs.md)。

## 例子

### 修复前

1.21.1 分支直接用 `GTManualStations.ADVANCED_BUTTON`，1.20.1 分支按 ID 查找注册表：

```java
//? if >=1.21.1 {
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedButtonBlockEntity>> ADVANCED_BUTTON =
        BLOCK_ENTITIES.register("advanced_button", () -> BlockEntityType.Builder.of(
                AdvancedButtonBlockEntity::new, GTManualStations.ADVANCED_BUTTON.get()).build(null));
//?} else {
public static final RegistryObject<BlockEntityType<AdvancedButtonBlockEntity>> ADVANCED_BUTTON =
        BLOCK_ENTITIES.register("advanced_button", () -> BlockEntityType.Builder.of(
                AdvancedButtonBlockEntity::new,
                ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(
                        REGISTRY_NAMESPACE, "advanced_button"))).build(null));
//?}
```

### 修复后

两侧都用目录字段，BE 类型只保留一份共享声明：

```java
public static final Supplier<BlockEntityType<AdvancedButtonBlockEntity>> ADVANCED_BUTTON =
        BLOCK_ENTITIES.register("advanced_button", () -> BlockEntityType.Builder.of(
                AdvancedButtonBlockEntity::new, GTManualStations.ADVANCED_BUTTON.get()).build(null));
```

### 为什么这么修复

- `GTManualStations` 在两个节点都注册 `advanced_button`，并暴露 `ADVANCED_BUTTON` 字段；按 ID 查找只是绕开目录，拿到的仍是同一个方块。
- 用字段后 BE 声明可以共享，该条目的 `ForgeRegistries.BLOCKS.getValue` 查找依赖一并删除。
- 目录字段带类型，编译期能发现方块类变更；按 ID 查找返回 `Block`，错误会推迟到运行时。

### 其他可能的修复方式

- 保留按 ID 查找，只把 holder 类型改成 `Supplier`。查找仍在，节点注册表依赖仍在。
- 在共享层加一个按 ID 查方块的 helper。多一层间接，且仍然绕开目录。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
