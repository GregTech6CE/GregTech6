# 节点类拆分

## 子原因

同一逻辑在两个版本使用不同的类型：1.21.1 用节点接口 `PoweredToolItem`，1.20.1 用共享类 `ElectricToolItem`。调用点因此多出类型分支。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*        return isTool(stack) || stack.getItem() instanceof gregtech.engine.tool.PoweredToolItem electric
*///?} else {
        return isTool(stack) || stack.getItem() instanceof gregtech.content.item.ElectricToolItem electric
//?}
                && electric.interactionType(stack) != null;
```

### 修复后

先把 `PoweredToolItem` 移到共享源码集，让共享 `ElectricToolItem` 在两个节点都实现该接口，再删除分支：

```java
return isTool(stack) || stack.getItem() instanceof gregtech.engine.tool.PoweredToolItem electric
        && electric.interactionType(stack) != null;
```

### 为什么这么修复

`PoweredToolItem` 是 1.21.1 节点接口，`ElectricToolItem` 是共享类；分支承载的是类落点，不是行为。

### 其他可能的修复方式

- 保留节点接口，分支留在调用点。类落点未统一，维护面仍在。
- 让共享代码只依赖共享类 `ElectricToolItem`，删除 1.21.1 接口。接口已有其他节点实现时不可行。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)