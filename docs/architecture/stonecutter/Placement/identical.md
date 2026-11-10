# 同义分支

## 子原因

两个版本执行同一条语句，但分支写法不同：

- 两侧在同一个分支里各写一份相同语句。
- 同一语句被拆到两个单侧块里：1.21.1 用第一个块，1.20.1 用第二个块。

两种形态都不承载版本差异。第二种形态的每个块单独看都是单侧分支，`scan_branches.py --identical` 不会报告；把两个块合起来看才是同义。

## 例子

### 修复前

两侧同体：

```java
//? if >=1.21.1 {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?} else {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?}
```

分离形态：

```java
//? if >=1.21.1 {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?} else {
//?}

//? if >=1.21.1 {
//?} else {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?}
```

### 修复后

```java
if (GTToolHelper.isMachineWrench(tool)) return true;
```

相关提交：`2e5627aa`、`1b350bb8`、`d90383b2`、`e15ae180`、`038dbba7`。

### 为什么这么修复

两侧调用同一个共享门面方法，语句本身没有版本差异。

分离形态不能只看文本就合并，必须先检查执行流：

- 语句在 1.21.1 和 1.20.1 中的位置不同；合并后只能保留一个位置，必然把其中一个版本的语句前移或后移。
- 如果语句是 `return`、`throw`、`break` 或 `continue`，移动会改变两个块之间的代码是否执行。
- 如果语句或两个块之间的代码有副作用、读写同一个变量，移动会改变结果。
- 只有两个块之间没有代码，或中间的代码无副作用且与该语句无关时，才能按同义分支合并。
- 类成员声明（方法、字段）的顺序不影响执行，可以按同义分支合并；方法体内的可执行语句不能只按文本同义合并。

### 其他可能的修复方式

- 保留分离形态。分支数量不变，后续扫描还要重新判断。
- 把两个单侧块合并成一个双侧块，保留位置差异。只是把两个分支合成一个，差异仍在。
- 确认执行流等价后，先把中间代码移到合适位置，再合并。移动代码本身可能引入新的行为差异。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)