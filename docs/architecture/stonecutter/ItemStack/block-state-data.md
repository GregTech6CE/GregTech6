# 方块状态属性

## 子原因

物品栈携带的方块状态属性在两个版本上存储位置不同：1.20.1 是主 tag 下的 `BlockStateTag` 子 tag（string→string），1.21.1 是 `DataComponents.BLOCK_STATE` 组件（`BlockItemStateProperties`）。与 [block-entity-data.md](block-entity-data.md) 不同，`BLOCK_STATE` 的 backing 是结构化 string map 而非自由 CompoundTag，因此共享面是 `Map<String, String>` 而非 `CompoundTag`，单独成门面。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*var data = stack.get(DataComponents.BLOCK_STATE);
return data == null ? fallback : DyeColor.byName(data.properties().get("color"), fallback);
*///?} else {
var data = stack.getTagElement("BlockStateTag");
return data == null ? fallback : DyeColor.byName(data.getString("color"), fallback);
//?}
```

### 修复后

```java
return DyeColor.byName(BlockStateData.getProperty(stack, "color"), fallback);
```

`BlockStateData` 的公共面：

```java
public static Map<String, String> readProperties(ItemStack stack) {
//? if >=1.21.1 {
    return stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).properties();
//?} else {
    var data = StackData.read(stack);
    Map<String, String> properties = new HashMap<>();
    if (data != null) {
        CompoundTag stateTag = data.getCompound("BlockStateTag");
        for (String key : stateTag.getAllKeys()) properties.put(key, stateTag.getString(key));
    }
    return properties;
//?}
}
```

另有 `getProperty(stack, key)`、`writeProperties(stack, map)`（空 map 移除）、`with(stack, key, value)`（保留其他属性）、`remove`。

### 为什么这么修复

- map API 吸收存储差异，调用点读写同一份属性 map；`with` 读改写保留其他属性。
- 类型化 `Property` 重载（`BlockItemStateProperties.with(STONE, stone)`）等价于 `with(stack, STONE.getName(), stone.getSerializedName())`：`EnumProperty` 存储 `getSerializedName()`，对 `OreHostStone` 即 `id()`，与 1.20.1 `putString(name, id())` 一致。
- 节点特定 legacy 共写保留为分支：`OreBlock.withStone` 的 1.20.1 侧对完整矿石另写 `BlockEntityTag`（旧存档迁移），1.21.1 不写虚构 BE。门面化后 1.20.1 的 `BlockStateTag` 由整段替换改为保留其他属性，与 1.21.1 `.with` 语义对齐。
- 整 stack 组件拷贝（`LegacyConstructionBlockItem` 的 `applyComponents` ↔ `setTag`）与多组件 legacy 回退（`SourceOreCrushingAdapter`）不属于本子样例，保留分支另行复核。

### 其他可能的修复方式

- 各调用点保持分支。string map 与组件 API 的差异随文件重复。
- 塞进 `StackData` 的 CompoundTag API。丢失结构化属性语义，且 `BLOCK_STATE` 不是自由 tag。

相关链接：[storage.md](storage.md)、[block-entity-data.md](block-entity-data.md)、[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)