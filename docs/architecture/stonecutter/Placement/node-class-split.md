# 节点类拆分

## 子原因

同一逻辑在两个版本使用不同的节点类：1.21.1 有 `SmeltingCrucibleEntity`，1.20.1 有 `SmeltingCrucibleBlockEntity`；两者都不在共享源码集。调用点因此多出类型分支。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleEntity crucible)) {
*///?} else {
if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleBlockEntity crucible)) {
//?}
```

### 修复后

先把两个节点 BE 类合并到共享源码集（或让共享代码只依赖共享接口），再删除分支：

```java
if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleBlockEntity crucible)) {
```

统一前不要用这个片段替换 1.21.1 节点；分支保留，但归类为类落点差异，不当作行为 parity。

### 为什么这么修复

`SmeltingCrucibleEntity` 是 1.21.1 节点类，`SmeltingCrucibleBlockEntity` 是 1.20.1 节点类；分支承载的是类落点，不是行为。`PoweredToolItem` ↔ `ElectricToolItem` 是同类问题（1.21.1 节点接口与共享类）。

### 其他可能的修复方式

- 保留两个节点类，分支留在调用点。类落点未统一，维护面仍在。
- 让共享代码只依赖共享接口，两个节点类各自实现。需要先抽出接口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
