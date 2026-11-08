# 身份与包装

## 子原因

身份比较、包装类与静态谓词看起来像同义分支，实际承载行为或入口归属。不能只按代码相似度折叠。

## 例子

### 修复前（身份比较）

```java
//? if >=1.21.1 {
return ItemStack.isSameItemSameComponents(first, second);
//?} else {
return ItemStack.isSameItemSameTags(first, second);
//?}
```

### 修复后（身份比较）

```java
return StackCompat.sameItem(first, second);
```

相关提交：`23790c26`、`9174324d`、`aced0f71`。

### 包装类

历史清理中已删除的节点侧包装类包括 `NeoToolBindings`、`FluidPipeToolInteractions`、`WireToolInteractions`、`AxleToolInteractions`；它们只转发共享代码，删除后调用点改用 `GTToolHelper` 等共享门面。

### 静态谓词与列表

`GTToolType.byId("crowbar")` 与 `GTToolType.CROWBAR`、`PanelCoverHost.consumesTool` 与静态列表，属于“两版各有一套判断”。先查原版实际消费路径，再决定合并到共享谓词还是保留节点实现。

### 为什么这么修复

`isSameItemSameTags` 与 `isSameItemSameComponents` 对“相同数据”的定义不同，门面必须保持两侧同义。包装类会制造“两套入口”。静态谓词可能承载真实行为，不能只按“代码看起来一样”折叠。

### 其他可能的修复方式

- 调用点各自分支。身份比较调用点多，分支会扩散。
- 保留包装类，只改方法体。入口仍然有两套。

相关链接：[issue #359](https://github.com/GregTech6CE/GregTech6/issues/359)
