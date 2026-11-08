# 注册表与 id

## 子原因

两个版本的注册表 API、命名空间常量与数据包目录命名不同。调用点各自分支会让同一段逻辑出现多份。

## 例子

### 修复前（注册表查找）

```java
//? if >=1.21.1 {
var item = BuiltInRegistries.ITEM.get(id);
//?} else {
var item = ForgeRegistries.ITEMS.getValue(id);
//?}
```

### 修复后（注册表查找）

```java
var item = Mc.item(id);
```

相关提交：`73853464`、`14a22508`。

### 修复前（id 与命名空间）

```java
//? if >=1.21.1 {
var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
//?} else {
var id = GregTech.id(path);
//?}
```

### 修复后（id 与命名空间）

收进共享的 `ResourceLocation` 辅助方法或 `GregTechIdentity` 常量。相关提交：`1e942f72`、`8923ebba`、`d3647e54`。

### 标签目录

1.20.1 使用 `tags/items`，1.21.1 使用 `tags/item`；方块与流体同理（`blocks`/`block`、`fluids`/`fluid`）。相关提交：`c751063`。

### 为什么这么修复

两个版本都能查到同一个注册表，差异只在 API 名称；id 与命名空间常量语义相同。门面方法同名同义后，调用点可以共享。

### 其他可能的修复方式

- 调用点各自分支。注册表与 id 调用点数量多，分支会扩散。
- 在节点侧各写一套 helper。会制造两套入口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
