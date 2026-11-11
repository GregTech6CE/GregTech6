# 混合目录拆分

## 子原因

节点类同时注册方块和 BE 类型。方块注册依赖 `ContentRegistrar`，BE 类型依赖 `DeferredRegister` 和 `IEventBus`；两者生命周期不同，放在一个节点类里会让共享方块目录和 BE 注册互相牵制。

## 例子

### 修复前

1.21.1 的 `StorageRegistries` 同时持有质量存储方块和 BE 类型：

```java
public final class StorageRegistries {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = ...;
    public static final Supplier<BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE =
            TYPES.register("mass_storage", ...);
    public static final Supplier<BlockEntityType<LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE =
            TYPES.register("logistics_mass_storage", ...);

    public static void register(ContentRegistrar registrar) {
        for (var spec : GTStorageMetals.ALL)
            bind(registrar, "mass_storage_" + spec.suffix(), ...);
        for (var spec : GTStorageMetals.ALL)
            bindLogistics(registrar, "logistics_mass_storage_" + spec.suffix(), ...);
    }

    public static void submit(IEventBus bus) {
        TYPES.register(bus);
    }
}
```

### 修复后

方块注册移到共享目录，BE 类型移到 `GTBlockEntities` 共享段：

```java
// 共享方块目录
public final class GTMassStorages {
    public static final List<RegisteredBlock<MassStorageBlock>> MASS_STORAGES = ...;
    public static final List<RegisteredBlock<LogisticsMassStorageBlock>> LOGISTICS_MASS_STORAGES = ...;

    public static void initialize(ContentRegistrar registrar) { ... }
}

// GTBlockEntities 共享段
public static final Supplier<BlockEntityType<MassStorageBlockEntity>> MASS_STORAGE =
        BLOCK_ENTITIES.register("mass_storage", ...);
public static final Supplier<BlockEntityType<LogisticsMassStorageBlockEntity>> LOGISTICS_MASS_STORAGE =
        BLOCK_ENTITIES.register("logistics_mass_storage", ...);
```

节点入口只保留方块目录调用：

```java
GTMassStorages.initialize(registrar);
```

### 为什么这么修复

- 方块目录只依赖共享类，可以整体移到共享源码集；BE 类型统一进 `GTBlockEntities` 的共享段，两个节点共用同一个 `DeferredRegister`。
- 节点类不再需要 `TYPES` 和 `submit`，`NeoContent` 少一个提交点，1.20.1 也不需要另写一份 BE 类型。
- 拆分按生命周期而不是按内容家族：方块走 `ContentRegistrar`，BE 类型走 `BLOCK_ENTITIES`，与 [catalog-split.md](catalog-split.md) 的家族拆分互补。

### 其他可能的修复方式

- 保留 `StorageRegistries`，只把方块注册改成共享目录。BE 类型仍在节点，1.20.1 仍要另写一份。
- 把 BE 类型也放进 `GTMassStorages`。方块目录会依赖 `DeferredRegister` 和 `IEventBus`，不再是纯共享目录。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
