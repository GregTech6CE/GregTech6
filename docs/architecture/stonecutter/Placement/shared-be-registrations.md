# 共享 BE 注册

## 子原因

两个分支各声明一次同名 BE 类型。方块目录统一后，两侧的方块列表同义，BE 类型本身也是共享类；分支里的两份声明没有承载任何版本差异，只会让 BE ID、方块列表和 holder 类型逐渐漂移。

holder 类型本身不是本篇重点；holder 类型差异见 [holder-supplier.md](holder-supplier.md)，同一注册项被拆成两份的形态见 [split-duplicate.md](split-duplicate.md)。

## 例子

### 修复前

1.21.1 分支用共享目录 `GTMetalChests.all()`：

```java
//? if >=1.21.1 {
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalChestBlockEntity>> METAL_CHEST =
        BLOCK_ENTITIES.register("metal_chest", () -> BlockEntityType.Builder.of(
                MetalChestBlockEntity::new, GTMetalChests.all().stream().map(RegisteredBlock::get)
                        .toArray(Block[]::new)).build(null));
//?} else {
```

1.20.1 分支用节点目录 `GTStorage` 的三段列表：

```java
public static final RegistryObject<BlockEntityType<MetalChestBlockEntity>> METAL_CHEST =
        BLOCK_ENTITIES.register("metal_chest", () -> BlockEntityType.Builder.of(
                MetalChestBlockEntity::new,
                Stream.of(GTStorage.METAL_CHESTS, GTStorage.REINFORCED_WOOD_CHESTS, GTLootChests.LOOT_CHESTS)
                        .flatMap(Collection::stream).map(RegisteredBlock::get)
                        .toArray(Block[]::new)).build(null));
//?}
```

### 修复后

`GTMetalChests` 成为共享目录后，两侧列表同义，BE 类型只保留一份共享声明：

```java
public static final Supplier<BlockEntityType<MetalChestBlockEntity>> METAL_CHEST =
        BLOCK_ENTITIES.register("metal_chest", () -> BlockEntityType.Builder.of(
                MetalChestBlockEntity::new, GTMetalChests.all().stream().map(RegisteredBlock::get)
                        .toArray(Block[]::new)).build(null));
```

### 为什么这么修复

- `GTMetalChests.all()` 已经包含金属箱、强化木箱和战利品箱，与 1.20.1 的三段列表是同一批方块。
- BE ID `metal_chest` 不变，注册顺序不影响 ID。
- `Supplier<BlockEntityType<T>>` 同时兼容 `DeferredHolder` 和 `RegistryObject`，共享段不需要 holder 类型分支。

### 其他可能的修复方式

- 保留两份声明，只把 1.20.1 的列表改成 `GTMetalChests.all()`。分支仍在，BE 类型仍有两个 holder 类型。
- 保留共享段一份声明，用 `//?` 在 `DeferredHolder` 和 `RegistryObject` 之间切换。holder 类型分支仍在，且仍需确认两侧方块列表一致。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
