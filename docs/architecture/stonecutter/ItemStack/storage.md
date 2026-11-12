# ItemStack storage

## 子原因

1.20.5 起，物品的自定义数据从 `ItemStack` 上的一个可变 `CompoundTag` 改为 `DataComponents.CUSTOM_DATA` 组件。

| 操作 | 1.20.1 | 1.21.1 |
| --- | --- | --- |
| 判存在 | `hasTag()` | `has(CUSTOM_DATA)` |
| 读整块 | `getTag()` | `get(CUSTOM_DATA).copyTag()` |
| 写整块 | `setTag(tag)` | `set(CUSTOM_DATA, CustomData.of(tag))` |
| 惰性创建 | `getOrCreateTag()` | 无对应，需 copy → 修改 → set |
| 子 tag | `getTagElement` / `getOrCreateTagElement` | 无直接对应 |
| 删除整块 | `setTag(null)` | `remove(CUSTOM_DATA)` |
| 删除单键 | `removeTagKey(String)` | copy → remove → set 或 remove |

关键语义差异：

1. 1.20.1 的 `getOrCreateTag()` 返回栈内活引用；1.21.1 没有活引用，必须 copy → 修改 → set。
2. `CustomData.of(tag)` 在 NeoForge 21.1 会复制传入的 tag；读取时 `copyTag()` 仍会产生一次副本。
3. 空 tag 的表示不同：1.20.1 用 `setTag(null)`，1.21.1 用 `remove(CUSTOM_DATA)`。留下空组件会让 `has()` 返回 `true`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
tag.putInt("gt.mold", value);
stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
//?} else {
stack.getOrCreateTag().putInt("gt.mold", value);
//?}
```

### 修复后

```java
StackData.putInt(stack, "gt.mold", value);
```

`StackData` 的公共面：

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

1.20.1 上两者等价；1.21.1 上附魔、伤害、自定义名称是独立组件，不在 `CUSTOM_DATA` 里。旧 `hasTag()` 调用点改用 `hasAnyData`。

### 类型化组件

类型化组件是独立的组件键，不能换成 `CUSTOM_DATA`（vanilla 按键识别）。但"不能换成 `CUSTOM_DATA`"不等于"不能门面化"，按 backing 类型区分：

- `BLOCK_ENTITY_DATA` 与 `CUSTOM_DATA` 同为 `DataComponentType<CustomData>`（CompoundTag backing），可用与 `StackData` 相同的读改写契约门面化，见 [block-entity-data.md](block-entity-data.md)。
- `BLOCK_STATE` 是 `BlockItemStateProperties`（结构化 string map），用 map API 门面化，见 [block-state-data.md](block-state-data.md)。
- `ENCHANTMENTS`、`CUSTOM_NAME` 等不透明强类型组件暂无门面，访问保持分支：

```java
//? if >=1.21.1 {
var data = stack.get(DataComponents.ENCHANTMENTS);
//?} else {
var data = stack.getTagElement("Enchantments");
//?}
```

### 为什么这么修复

`read` 返回副本，`update` 把读改写收成一个入口，避免 1.20.1 的活引用语义泄漏到共享代码；`write` 把空 tag 归一化为移除，保证两边的 `has` 语义一致。

### 其他可能的修复方式

- 每个调用点各自 copy → 修改 → set。重复且容易漏写回。
- 直接返回活引用。1.20.1 能跑，1.21.1 上不生效。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
