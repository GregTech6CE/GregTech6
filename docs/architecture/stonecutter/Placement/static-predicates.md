# 静态谓词

## 子原因

`GTToolType.byId("crowbar")` 与 `GTToolType.CROWBAR`、`PanelCoverHost.consumesTool` 与静态列表，属于“两版各有一套判断”。

## 例子

### 修复前

```java
//? if >=1.21.1 {
return GTToolType.byId("crowbar");
//?} else {
return GTToolType.CROWBAR;
//?}
```

### 修复后

先查原版实际消费路径，再决定合并到共享谓词还是保留节点实现。

### 为什么这么修复

这类差异可能承载真实行为，不能只按“代码看起来一样”折叠。

### 其他可能的修复方式

- 直接折叠成枚举。可能改变 `byId` 的容错行为。
- 保留两套列表。维护面大，且容易漂移。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
