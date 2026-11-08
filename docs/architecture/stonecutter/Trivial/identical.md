# 同义分支

## 子原因

两侧代码完全相同，只是写法或常量不同。分支没有承载任何版本差异。

## 例子

### 修复前

```java
//? if >=1.21.1 {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?} else {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?}
```

### 修复后

```java
if (GTToolHelper.isMachineWrench(tool)) return true;
```

相关提交：`2e5627aa`、`1b350bb8`、`d90383b2`。

### 为什么这么修复

两侧调用的是同一个共享门面方法，分支没有承载任何版本差异，可以直接删除。

### 其他可能的修复方式

- 保留分支，只做格式统一。`//?` 数量不变，维护面仍在。
- 把两侧合并成一个方法再调用。等同于删除分支，但多一层间接。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
