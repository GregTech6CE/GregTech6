# 节点专属旧存档迁移

## 子原因

有些方块实体只为某个节点的旧存档格式存在。1.20.1 世界在方块状态迁移前，每个矿石方块都带一个 `gt6ce:ore` 方块实体；加载旧世界时，它把存档里的 host stone 写回 `OreBlock.STONE`，然后自删。1.21.1 世界不可能包含这种实体，因此该 BE 类型只注册在 1.20.1 节点。

## 例子

### 修复前

`GTBlockEntities` 的 1.20.1 分支保留旧存档需要的 BE 类型，1.21.1 分支没有对应条目：

```java
//? if >=1.21.1 {
/*// 1.21.1 世界没有旧格式矿石实体。
*///?} else {
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.OreBlockEntity>> ORE =
        BLOCK_ENTITIES.register("ore", () -> {
            ...
            return BlockEntityType.Builder.of(
                    gregtech.content.blockentity.OreBlockEntity::new, blocks).build(null);
        });
//?}
```

### 修复后

分支保留，并在注册处写明迁移范围；`OreBlockEntity` 留在 1.20.1 源码集：

```java
//? if >=1.21.1 {
/*// 1.21.1 不注册 gt6ce:ore。
*///?} else {
// 1.20.1 旧存档迁移：加载时把 host stone 写回 OreBlock.STONE 后自删。
public static final RegistryObject<BlockEntityType<gregtech.content.blockentity.OreBlockEntity>> ORE =
        BLOCK_ENTITIES.register("ore", () -> {
            ...
        });
//?}
```

### 为什么这么修复

- 这是存档格式兼容，不是平台 API；但它的生命周期只存在于 1.20.1 的旧世界，1.21.1 不可能产生或加载这种实体。
- 移入共享段会给 1.21.1 注册一个永不使用的 BE 类型，并让共享 `OreBlock` 依赖只有 1.20.1 才需要的迁移类。
- 影响面：1.20.1 旧存档首次加载后矿石实体被移除，方块状态带上 host stone；之后保存不再写 `gt6ce:ore`。1.21.1 不受影响。
- 验证方式：用迁移前的 1.20.1 存档加载一次，确认矿石位置仍是正确的 host stone、区块里不再有矿石实体，再次保存后 NBT 不再带 `gt6ce:ore`。

### 其他可能的修复方式

- 移入共享源码集并在两个节点注册。1.21.1 多一个死类型。
- 删除迁移实体。旧 1.20.1 世界会丢失 host stone 信息。
- 把迁移逻辑放进 `OreBlock` 的静态加载路径。方块状态加载发生在实体创建之前，拿不到旧存档的实体字段。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)