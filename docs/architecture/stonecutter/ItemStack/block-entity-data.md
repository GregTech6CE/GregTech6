# 方块实体数据

## 子原因

物品栈携带的方块实体载荷在两个版本上存储位置不同：1.20.1 是主 tag 下的 `BlockEntityTag` 子 tag，1.21.1 是 `DataComponents.BLOCK_ENTITY_DATA` 组件。关键在于 `BLOCK_ENTITY_DATA` 的类型是 `DataComponentType<CustomData>`，与 `CUSTOM_DATA` 完全同型（CompoundTag backing），因此读写契约与 [storage.md](storage.md) 的 `StackData` 一致，只是组件键不同。早期"类型化组件不能进入共享签名"的判断过宽：不能换成 `CUSTOM_DATA`（vanilla 按键识别）是对的，但同型 backing 完全可以门面化。该访问习语曾在约 37 个文件重复。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*var component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
var data = component == null ? null : component.copyTag();
*///?} else {
var data = stack.getTagElement("BlockEntityTag");
//?}
```

### 修复后

```java
var data = BlockEntityData.read(stack);
```

`BlockEntityData` 的公共面（镜像 `StackData`）：

```java
public static CompoundTag read(ItemStack stack) {
//? if >=1.21.1 {
    var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
    return data == null ? null : data.copyTag();
//?} else {
    var tag = stack.getTagElement("BlockEntityTag");
    return tag == null ? null : tag.copy();
//?}
}

public static void write(ItemStack stack, CompoundTag tag) {
//? if >=1.21.1 {
    if (tag == null || tag.isEmpty()) stack.remove(DataComponents.BLOCK_ENTITY_DATA);
    else stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
//?} else {
    if (tag == null || tag.isEmpty()) stack.removeTagKey("BlockEntityTag");
    else stack.addTagElement("BlockEntityTag", tag);
//?}
}
```

另有 `readOrEmpty`、`has`、`remove`、`update(stack, consumer)`。

### 为什么这么修复

- `read` 两侧都返回副本，杜绝 1.20.1 `getTagElement` 活引用语义泄漏到共享代码；`write` 把空 tag 归一化为移除，保证 `has` 一致。
- 读改写收进 `update`，调用点不再各自 copy→修改→set。
- 门面只吸收存储差异。节点特定的细节仍保留为收缩后的分支：1.21.1 写入 `id` 字段（如 ZpmEnergy/BushBlock/FluidSpringBlock）、与 BE save 签名纠缠的 `saveWithId(lookup)` ↔ `saveWithoutMetadata()`（如各控制器 getDrops）。这些残留分支满足平台 API 例外：API 运行时差异、无法用更上层门面吸收、调用点的 BED 访问已不再分支、分支处只余真平台差异。
- 结构性分叉（MassStorageBlock 的 `shell()` ↔ `packed()/emptyShell()` 不同方法集）不属于本子样例，按行为重复另行统一。

### 其他可能的修复方式

- 各调用点保持分支。访问习语随文件重复，copy/写回语义易漏。
- 把 `BLOCK_ENTITY_DATA` 换成 `CUSTOM_DATA`。vanilla 按组件键识别方块实体载荷，换键后放置不再加载 BE 数据。

相关链接：[storage.md](storage.md)、[block-state-data.md](block-state-data.md)、[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)