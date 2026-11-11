# 注册家族目录

## 子原因

同一内容家族在两个版本各有一套注册路径时，会产生重复列表、不同 holder 类型和重复 BE 类型。典型形态：

- 1.20.1 的节点注册类（如 `MachineRegistry`、`GTMachines`）直接使用 `GTBlocks` / `GTBlocks.BLOCK_ITEMS` 和 `RegistryObject`；
- 1.21.1 的共享目录（如 `GTEngines`、`GTSmeltery`）使用 `ContentRegistrar` 和 `RegisteredBlock`；节点适配器（如 `BasicMachineRegistries`）只保留 Menu、capability 和提交入口；
- 调用点通过 `//?` 在 `List<RegistryObject<T>>` 与 `List<RegisteredBlock<T>>` 之间切换。

这类差异不是平台 API 差异，而是注册归属和 holder 类型的历史分叉。

## 例子

### 修复前

```java
//? if >=1.21.1 {
        /*for (var entry : BasicMachineRegistries.all()) {
*///?} else {
        for (var entry : MachineRegistry.basicMachines()) {
//?}
```

### 修复后

```java
for (var entry : GTBasicMachines.all()) {
```

共享家族类只保留一个注册入口和一组列表：

```java
public final class GTBasicMachines {
    private static final List<RegisteredBlock<BasicMachineBlock>> ALL = new ArrayList<>();

    public static List<RegisteredBlock<BasicMachineBlock>> all() {
        return List.copyOf(ALL);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (BasicMachineSpec spec : BasicMachineDefinitions.specifications()) {
            var block = MultiblockRegistration.register(..., (path, factory) ->
                    registrar.block(spec.namespace(), path, factory,
                            bare -> new BasicMachineItem(bare, new Item.Properties().stacksTo(16))));
            ALL.add(block);
            registrar.blockEntity(spec.namespace(), "be_" + spec.id(), () -> ...);
        }
    }
}
```

### 边界

- 方块/物品注册放共享家族类，使用 `ContentRegistrar`。
- 共享 BE 类用 `ContentRegistrar.blockEntity` + `RegisteredBlockEntity`。
- 节点专属 BE 类、Menu、capability、网络仍留在节点类；共享家族类只暴露列表/句柄。
- 本次统一的机器物品类（`BasicMachineItem`、`EngineItem`、`BurningBoxBlockItem`）只保留 `appendHoverText` 签名的 `//?`（`TooltipContext` ↔ `Level`）；其他共享物品类不受此限。
- `GTBlockEntities` 的共享 BE 类型已折叠为 `Supplier<BlockEntityType<T>>` 字段；剩余节点专属条目仍留在分支中。
- 删除旧节点注册类前先 `rg` 外部引用；外部引用面通常远小于类本身。

## 为什么这么修复

- 调用点不再分支，列表内容、顺序和 holder 类型由共享目录决定。
- 1.20.1 不再维护第二套注册路径，减少重复注册和死代码。
- 行为基准按[跨版本统一方针](../unification-policy.md)：parity > golden > 1.21 > 1.20。

## 其他可能的修复方式

- 共享门面包两套路径。分支仍在，维护成本不降。
- 保留节点注册类，只在调用点折叠。列表内容仍可能漂移。
- 把节点 BE 类也移到共享。只有 BE 类本身版本中立时才可行；否则保留节点 BE 类型。
