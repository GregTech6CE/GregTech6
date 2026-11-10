# 引用目录

## 子原因

1.20.1 的 `GTDecorBlocks`、`GTMiscBlocks` 用 `RegisteredBlock.reference(id)` 或私有 `reference(id)` 辅助方法（返回 `RegistryObject`）指向共享目录已经注册的方块。这些字段是查询，不是注册；删除字段不会注销方块，消费端应直接依赖共享目录。

本篇处理整个引用目录门面；单条 BE 注册用按 ID 查找代替目录字段的形态见 [catalog-field-over-lookup.md](catalog-field-over-lookup.md)。

## 例子

### 修复前

`GTDecorBlocks` 为 bookshelf 家族保留了引用字段，注册方是 `GTBookShelves`：

```java
// GTDecorBlocks (1.20.1)
public static RegistryObject<BookShelfBlock> BOOKSHELF;
...
BOOKSHELF = reference("bookshelf");
for (var entry : GTBookShelves.all())
    BOOKSHELVES.put(entry.getId().getPath(), reference(entry.getId().getPath()));

// GTBlockEntities 1.20.1 分支
public static final RegistryObject<BlockEntityType<BookShelfBlockEntity>> BOOKSHELF =
        BLOCK_ENTITIES.register("bookshelf", () ->
                BlockEntityType.Builder.of(BookShelfBlockEntity::new,
                        GTDecorBlocks.allBookshelves().stream().map(RegistryObject::get)
                                .toArray(Block[]::new)).build(null));
```

### 修复后

BE 类型和地牢代码直接使用共享目录：

```java
public static final Supplier<BlockEntityType<BookShelfBlockEntity>> BOOKSHELF =
        BLOCK_ENTITIES.register("bookshelf", () ->
                BlockEntityType.Builder.of(BookShelfBlockEntity::new,
                        GTBookShelves.all().stream().map(RegisteredBlock::get)
                                .toArray(Block[]::new)).build(null));

// 地牢代码
GTBookShelves.get("bookshelf").defaultBlockState()
```

### 为什么这么修复

- `reference` 只是按 ID 查 `ForgeRegistries.BLOCKS`，没有注册行为；`GTBookShelves.initialize` 才是注册方。
- 引用目录让 1.20.1 看起来有第二套路径，实际指向同一批方块；删除后 bookshelf 家族只剩 `GTBookShelves` 一条路径。
- 引用字段和 `allBookshelves()` 之类的查询方法没有其他版本差异，保留只会让后续调用点继续依赖节点门面。

### 其他可能的修复方式

- 保留引用字段，只把 BE 条目改成共享目录。引用目录仍在，后续仍可能被误认为注册路径。
- 把引用目录改成共享目录的别名。多一层间接，维护成本不降。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
