# holder 类型

## 子原因

`GTBlockEntities` 的两个分支用 `DeferredHolder` / `RegistryObject` 各写一遍同一注册项，消费者再用 `//?` 切换类型。holder 类型只是加载器包装，不是行为差异。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HopperBlockEntity>> HOPPER = ...
*///?} else {
public static final RegistryObject<BlockEntityType<HopperBlockEntity>> HOPPER = ...
//?}
```

消费者也要跟着分支：

```java
//? if >=1.21.1 {
/*var expected = GTBlockEntities.HOPPER;
*///?} else {
RegistryObject<BlockEntityType<HopperBlockEntity>> expected = GTBlockEntities.HOPPER;
//?}
```

### 修复后

字段用两个 holder 的共同父接口 `Supplier`：

```java
public static final Supplier<BlockEntityType<HopperBlockEntity>> HOPPER =
        BLOCK_ENTITIES.register("hopper", () -> BlockEntityType.Builder.of(
                HopperBlockEntity::new,
                GTHoppers.hoppers().stream().map(RegisteredBlock::get).toArray(Block[]::new)).build(null));
```

消费者不再分支：

```java
var expected = GTBlockEntities.HOPPER;
```

### 为什么这么修复

`DeferredHolder` 和 `RegistryObject` 都实现 `Supplier`；字段只暴露 `get()` 时，holder 类型不需要出现在共享签名里。注册项只写一次，`GTBlockEntities` 的共享 BE 类型从两份列表折叠成一份。

### 其他可能的修复方式

- 定义 `RegisteredBlockEntity` record 同时保存 id 与 supplier。需要注册键时可用；只读 `.get()` 的调用点没有必要。
- 保留两套 holder 字段，只在消费者折叠。分支仍在，维护成本不降。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
