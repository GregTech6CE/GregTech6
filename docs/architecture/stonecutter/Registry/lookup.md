# Registry lookup

## 子原因

1.20.1 用 `ForgeRegistries.ITEMS.getValue(id)`；1.21.1 用 `BuiltInRegistries.ITEM.get(id)`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
var item = BuiltInRegistries.ITEM.get(id);
//?} else {
var item = ForgeRegistries.ITEMS.getValue(id);
//?}
```

### 修复后

```java
var item = Mc.item(id);
```

### 为什么这么修复

两个版本都能查到同一个注册表，差异只在 API 名称。门面方法同名同义后，调用点可以共享。

### 其他可能的修复方式

- 调用点各自分支。注册表调用点数量多，分支会扩散。
- 在节点侧各写一套 helper。会制造两套入口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
