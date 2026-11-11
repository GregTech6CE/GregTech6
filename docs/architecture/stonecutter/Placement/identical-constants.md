# 同义常量分支

## 子原因

两侧逻辑相同，只有命名空间字面量与常量的写法不同：一侧写 `"gt6ce"`，另一侧写 `GregTechIdentity.REGISTRY_NAMESPACE`。`scan_branches.py --identical` 按文本比较，不会把这类分支计为同义，但它们不承载任何版本差异。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".machine.none")
*///?} else {
tooltip.add(Component.translatable("tooltip." + GregTechIdentity.REGISTRY_NAMESPACE + ".machine.none")
//?}
        .withStyle(ChatFormatting.WHITE))
        .withStyle(ChatFormatting.RED);
```

### 修复后

```java
tooltip.add(Component.translatable("tooltip." + GregTechIdentity.REGISTRY_NAMESPACE + ".machine.none")
        .withStyle(ChatFormatting.WHITE))
        .withStyle(ChatFormatting.RED);
```

### 为什么这么修复

`GregTechIdentity.REGISTRY_NAMESPACE` 在共享 `core` 源码中定义为 `MOD_ID = "gt6ce"`，两侧运行时生成的字符串相同，不属于平台 API 差异。保留常量写法即可删除分支，并让命名空间只有一个来源。

### 其他可能的修复方式

- 保留分支。没有行为差异，但 `--identical` 不报告它，后续扫描会一直漏掉。
- 两侧都写字面量。同样能删除分支，但把命名空间散落在字符串拼接里，新增命名空间时容易漏改。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)