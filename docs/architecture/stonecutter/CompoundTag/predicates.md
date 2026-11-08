# 两个谓词

## 子原因

1.20.1 只有一个 tag，`hasTag()` 同时表示“有自定义数据”和“有任何非默认数据”。1.21.1 把附魔、伤害、自定义名称拆成独立组件，两个语义不再等价。

## 例子

### 修复前

```java
//? if >=1.21.1 {
return stack.has(DataComponents.CUSTOM_DATA);
//?} else {
return stack.hasTag();
//?}
```

带伤害或附魔的物品在 1.20.1 返回 `true`，在 1.21.1 返回 `false`。

### 修复后

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

旧 `hasTag()` 调用点改用 `hasAnyData`；需要“有自定义数据”时才用 `hasCustomData`。

### 为什么这么修复

`hasTag()` 覆盖伤害、附魔、自定义名称；`has(CUSTOM_DATA)` 只覆盖自由数据。合并会改变 `RecipeLookupTable`、`MaterialUnification` 等调用点的行为。

### 其他可能的修复方式

- 只保留 `has`，按 1.20.1 语义实现。1.21.1 上会把带伤害的物品误判为“有自定义数据”。
- 只保留 `hasAnyData`。需要判断自由数据时无法区分。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
