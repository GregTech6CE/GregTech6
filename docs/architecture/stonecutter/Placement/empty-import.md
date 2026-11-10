# 空块与单侧 import

## 子原因

空块通常来自“先加分支、后删内容”的中间状态；单侧 import 则是把整段 import 放进了分支。两者都不是行为差异。

## 例子

### 修复前（空块）

```java
//? if >=1.21.1 {
//?} else {
charge = spec().capacity();
//?}
```

### 修复后（空块）

```java
charge = spec().capacity();
```

### 修复前（单侧 import）

```java
//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
```

### 修复后（单侧 import）

如果两个版本都需要该类型，import 放在分支外；如果只有一个版本需要，保留分支。

相关提交：`e15ae180`、`038dbba7`。

### 为什么这么修复

空块没有任何内容；单侧 import 不是行为差异。删除后分支数量减少，编译结果不变。

### 其他可能的修复方式

- 保留空块作为占位。容易在后续编辑中误判为有意差异。
- 把 import 移到节点源。只有确实只在单节点使用时才这样做。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
