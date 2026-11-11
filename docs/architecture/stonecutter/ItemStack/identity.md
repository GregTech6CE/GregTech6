# ItemStack identity

## 子原因

1.20.1 用 `isSameItemSameTags` 比较物品与 tag；1.21.1 用 `isSameItemSameComponents` 比较整个组件栈。两个方法对“相同数据”的定义不同。

## 例子

### 修复前

```java
//? if >=1.21.1 {
return ItemStack.isSameItemSameComponents(first, second);
//?} else {
return ItemStack.isSameItemSameTags(first, second);
//?}
```

### 修复后

```java
public static boolean sameItem(ItemStack first, ItemStack second) {
//? if >=1.21.1 {
    return ItemStack.isSameItemSameComponents(first, second);
//?} else {
    return ItemStack.isSameItemSameTags(first, second);
//?}
}
```

调用点：

```java
if (StackCompat.sameItem(first, second)) { ... }
```

门面实现见 `minecraft/src/main/java/gregtech/engine/stack/StackCompat.java`。

### 为什么这么修复

这是真实语义差异的收敛，不是简单折叠。门面保持两侧同义，调用点不需要知道组件模型。

### 其他可能的修复方式

- 调用点各自分支。`isSameItemSameTags` 与 `isSameItemSameComponents` 容易混用。
- 只比较物品类型，忽略数据。会改变配方与合并行为。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
