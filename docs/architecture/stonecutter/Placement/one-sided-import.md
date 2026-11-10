# 单侧引用与 import

## 子原因

单侧 import 通常只是症状：真正的原因是单侧代码引用了类 A，而另一侧走了自己的实现。处理顺序：

1. 用 `rg` 找到 A 在分支内的使用点。
2. 找到另一侧同位置、同职责的调用链。
3. 如果另一侧是手写等价实现：先把另一侧改为调用 A，再删除行为分支和 import 分支。若 A 只在单节点源码集，先把 A 移到共享源码集，把确实无法共享的 API 差异收进 A 内部；A 只在单节点成立时保留分支，见 [node-only-migration.md](node-only-migration.md)。
4. 如果两侧都已经调用 A（import 被拆到两个分支）：把 import 提到分支外。
5. 如果另一侧有意使用不同 API（平台/parity）：保留单侧 import 和分支，不要 hoist。

反例：只删除 import 侧的 `//?`、把 A 提到两边，却不改另一侧的手写实现。这样另一侧仍然不使用 A，却多背了一个依赖；阅读时也看不出 A 实际被哪一侧使用。

## 例子

### 修复前

```java
//? if >=1.21.1 {
//?} else {
import gregtech.engine.material.MaterialPresentation;
//?}

//? if >=1.21.1 {
        /*return Component.translatable(getDescriptionId(), Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback()));
*///?} else {
        return Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
//?}
```

### 修复后

```java
import gregtech.engine.material.MaterialPresentation;

return Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
```

### 为什么这么修复

`MaterialPresentation` 是共享源码集里的类，两个节点都能用。1.21.1 侧原本自己拼 `Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback())`，与 `MaterialPresentation.name(material)` 职责相同。先把 1.21.1 的调用改为 `MaterialPresentation.name`，行为分支和 import 分支一起消失。

只 hoist import 会让 1.21.1 继续走自己的实现：依赖被带到两边，调用链仍然分叉。判断标准不是“类在两侧都存在”，而是“两侧是否都调用 A”。

统一调用前核对语义：`MaterialPresentation.name` 使用 `translatableWithFallback(key, fallback)`，1.21.1 手写路径使用 `translatable(key, fallback)`。材料翻译键缺失时两者显示不同；现有 GT 材料键都存在。若调用点的键可能缺失，先决定以哪一侧的 fallback 行为为准，再统一。

`OreHarvest` 是这条规则的另一种形态：它最初只写在 1.21.1 源码集，1.21.1 分支调用它，1.20.1 手写附魔等级查询。把类移到共享源码集，并把 `Holder<Enchantment>` ↔ `Enchantment` 的查表差异收进类内后，两个方块的调用点都变成 `OreHarvest.from(params)`。

`gregtech.GregTech` 是另一种同型问题：它是 1.20.1 的 `@Mod` 入口类，共享代码只在 1.20.1 分支使用 `MODID`/`LOGGER`/`id()`。正确做法是先把调用改为 `GregTechIdentity`/`Mc`，再删除 import；不是把节点入口类 hoist 到两边。

### 其他可能的修复方式

- 只 hoist import，不改另一侧调用。也就是反例：依赖面扩大，调用链仍分叉。
- 保留单侧 import 和分支。适用于另一侧确实要使用不同 API（平台/parity）的情况。
- 把整个类移到节点源码。只有该类确实只在单节点运行时才成立。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)