# 物品自定义数据：CompoundTag 与 DataComponent

返回[示例索引](README.md)。本文说明 1.20.1 的物品 tag 与 1.21.1 的 `CUSTOM_DATA` 组件之间的差异，以及 `StackData` 如何把差异收进一个公共面。

## 问题来源

1.20.5 起，物品的自定义数据从 `ItemStack` 上的一个可变 `CompoundTag` 改为 `DataComponents.CUSTOM_DATA` 组件。

| 操作 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 判存在 | `ItemStack#hasTag()` | `ItemStack#has(DataComponents.CUSTOM_DATA)` |
| 读整块 | `ItemStack#getTag()` | `ItemStack#get(DataComponents.CUSTOM_DATA)` 后 `CustomData#copyTag()` |
| 写整块 | `ItemStack#setTag(CompoundTag)` | `ItemStack#set(DataComponents.CUSTOM_DATA, CustomData.of(tag))` |
| 惰性创建 | `ItemStack#getOrCreateTag()` | 无对应，需 copy → 修改 → set |
| 子 tag | `getTagElement` / `getOrCreateTagElement` | 无直接对应 |
| 删除整块 | `setTag(null)` | `remove(DataComponents.CUSTOM_DATA)` |
| 删除单键 | `removeTagKey(String)` | copy → remove → set 或 remove |
| 身份比较 | `ItemStack.isSameItemSameTags` | `ItemStack.isSameItemSameComponents` |
| 是否有非默认数据 | `hasTag()` | `!getComponentsPatch().isEmpty()` |
| 线格式 | `{id, Count, tag}` | `{id, count, components}` |

关键语义差异：

1. 1.20.1 的 `getOrCreateTag()` 返回栈内活引用，原地修改即生效；1.21.1 没有活引用，必须 copy → 修改 → set。
2. `CustomData.of(tag)` 在 NeoForge 21.1 会复制传入的 tag，但读取时 `copyTag()` 仍会产生一次副本；不要依赖引用共享。
3. 空 tag 的表示不同：1.20.1 用 `setTag(null)`；1.21.1 用 `remove(CUSTOM_DATA)`。`set(CUSTOM_DATA, CustomData.of(new CompoundTag()))` 会留下一个存在但为空的组件，`has()` 返回 `true`。
4. 1.21.1 的 `DataComponentType<T>` 是强类型槽位；`CUSTOM_DATA` 是承载自由形式物品数据的组件（`BLOCK_ENTITY_DATA` 等也使用 `CustomData`，但语义是方块实体数据），其余组件（`DAMAGE`、`ENCHANTMENTS`、`CUSTOM_NAME` 等）都有各自的类型。

### 两个不同的谓词

| 语义 | 1.20.1 | 1.21.1 |
| --- | --- | --- |
| 有自定义数据 | `hasTag()` | `has(CUSTOM_DATA)` |
| 有任何非默认数据 | `hasTag()` | `!getComponentsPatch().isEmpty()` |

1.20.1 上两者等价，因为只有 tag；1.21.1 上不等价：附魔、伤害、自定义名称都是独立组件，不在 `CUSTOM_DATA` 里。

`StackData.has` 在 1.20.1 是 `hasTag()`，在 1.21.1 是 `has(CUSTOM_DATA)`，两者语义并不等价：带伤害或附魔的物品在 1.20.1 返回 `true`，在 1.21.1 返回 `false`。旧 `hasTag()` 调用点应改用 `hasAnyData`。

## 建议方案

```text
调用点（共享代码）
         ↓
StackData（CompoundTag 公共面）
         ↓
1.20.1: ItemStack tag        1.21.1: DataComponents.CUSTOM_DATA
```

要求：

- 公共面统一为 `CompoundTag`，调用点不出现 `DataComponentType` 与 `CustomData`。
- 类型化组件（如 `DataComponents.ENCHANTMENTS` 或自定义 `DataComponentType<T>`）只允许出现在 1.21.1 分支内部，不进入公共签名。
- `StackData` 不接收 `HolderLookup.Provider`：它只做内存读写，不碰 codec。
- 现有键名保持不变（`gt.cover.*`、`GT.ToolStats`、`gt.mold.*` 等），保证旧存档可读。
- `read` 统一返回快照（1.20.1 使用 `copy()`），避免“1.20 上能跑、1.21 上不生效”。
- `write` 统一把空 tag 归一化为移除。
- 两个谓词分别提供：`hasCustomData`（有自定义数据）与 `hasAnyData`（有任何非默认数据）。

## 例子与分析

### `StackData` 的公共面

```java
public static CompoundTag read(ItemStack stack) {
//? if >=1.21.1 {
    var data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.copyTag();
//?} else {
    return stack.hasTag() ? stack.getTag().copy() : null;
//?}
}

public static void write(ItemStack stack, CompoundTag tag) {
//? if >=1.21.1 {
    if (tag == null || tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
//?} else {
    stack.setTag(tag == null || tag.isEmpty() ? null : tag);
//?}
}

public static void update(ItemStack stack, Consumer<CompoundTag> action) {
    var tag = readOrEmpty(stack);
    action.accept(tag);
    write(stack, tag);
}
```

`read` 返回副本，调用方修改副本不会直接影响物品；`update` 把读改写收成一个入口，避免 1.20.1 的活引用语义泄漏到共享代码。

### 两个谓词

```java
public static boolean hasCustomData(ItemStack stack) {
//? if >=1.21.1 {
    return stack.has(DataComponents.CUSTOM_DATA);
//?} else {
    return stack.hasTag();
//?}
}

public static boolean hasAnyData(ItemStack stack) {
//? if >=1.21.1 {
    return !stack.getComponentsPatch().isEmpty();
//?} else {
    return stack.hasTag();
//?}
}
```

迁移时逐个判断调用点语义，不能把 `hasTag()` 一律替换成 `has`。

### `CoverStackData` 委托

`CoverStackData` 是覆盖板侧的半成品门面，现在全部委托 `StackData`：

```java
public static boolean has(ItemStack stack) {
    return StackData.hasAnyData(stack);
}

public static CompoundTag readOrEmpty(ItemStack stack) {
    return StackData.readOrEmpty(stack);
}

public static void putInt(ItemStack stack, String key, int value) {
    StackData.putInt(stack, key, value);
}
```

`detached(...)` 在复制出的物品上读改写，再写回；覆盖板专用键的清理仍留在 `CoverStackData`，不进入通用门面。

### 读改写

```java
StackData.update(stack, tag -> {
    tag.remove(AutocraftingRules.LEGACY_KEY);
    tag.remove(AutocraftingRules.BLUEPRINT_KEY);
    tag.merge(data);
});
```

等价于 1.20.1 的 `getOrCreateTag()` 加原地修改，但不会把活引用暴露给调用方。

### 身份比较

`isSameItemSameTags` → `isSameItemSameComponents` 不是存储 API，但经常和 tag 访问出现在同一个分支里。它属于 `StackCompat`，不属于 `StackData`：

```java
public static boolean sameItem(ItemStack first, ItemStack second) {
//? if >=1.21.1 {
    return ItemStack.isSameItemSameComponents(first, second);
//?} else {
    return ItemStack.isSameItemSameTags(first, second);
//?}
}
```

### 分析

- **为什么 `read` 要返回副本**：1.20.1 的 `getTag()` 是活引用，1.21.1 的 `copyTag()` 是副本；如果门面直接返回活引用，同一段代码在两个节点上的写回行为会不同。
- **为什么 `write` 要归一化空 tag**：1.21.1 上留下空组件会让 `has(CUSTOM_DATA)` 返回 `true`，与 1.20.1 的 `setTag(null)` 不一致。
- **为什么两个谓词不能合并**：`hasTag()` 覆盖伤害、附魔、自定义名称；`has(CUSTOM_DATA)` 只覆盖自由数据。合并会改变 `RecipeLookupTable`、`MaterialUnification` 等调用点的行为。
- **为什么类型化组件不能进公共签名**：1.20.1 没有 `DataComponentType`，公共签名一旦出现它，1.20.1 节点无法编译。`BLOCK_ENTITY_DATA`、`BLOCK_STATE`、`MAP_ID`、`WRITTEN_BOOK_CONTENT`、`CUSTOM_NAME` 等保留在 1.21.1 分支。
- **高频路径的成本**：`PanelCoverRuntime`、`ElectricToolWear`、`RodPhysics` 每次 `update` 都是一次深拷贝；长期应迁移到类型化组件。

相关契约见 issue [#359](https://github.com/GregTech6CE/GregTech6/issues/359)。
