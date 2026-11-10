# 单侧 import

## 子原因

整段 import 被放进一个版本分支，另一侧为空。是否保留不取决于“哪个版本用到”，而取决于被导入的类在哪里存在，以及共享代码是否应该依赖它：

- 类在两侧都存在（共享源码、两侧都有的原版类或公共库）：import 放到分支外，删除分支。未被另一节点使用的 import 不影响编译，以两个节点都编译通过为准。
- 类只在单节点存在：
  - 共享关注点（命名空间、logger、`ResourceLocation` 构造）已有共享常量或门面：改调用点，删除节点 import。
  - 真正的平台 API（Forge/NeoForge 注解、注册表、网络类）：保留分支，按平台 API 例外处理。
  - import 未被使用：直接删除。

## 例子

### 修复前

```java
//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
...
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}
```

### 修复后

```java
import gregtech.engine.mod.GregTechIdentity;
...
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
//?}
```

### 为什么这么修复

`gregtech.GregTech` 是 1.20.1 Forge 的 `@Mod` 入口类，只存在于 `minecraft/versions/1.20.1-forge/src/main/java/gregtech/GregTech.java`；1.21.1 的入口是 `gregtech.platform.neoforge.GregTechNeoForge`。共享代码依赖它，会把 1.20.1 的落点固化到共享源码里。`MODID` 在共享 `core` 的 `GregTechIdentity.MOD_ID` 中有等值常量，改调用点后 import 分支消失。剩余的 `@Mod.EventBusSubscriber` ↔ `@EventBusSubscriber` 是平台 API 差异，按例外保留。

### 其他可能的修复方式

- 保留 import 分支。共享代码仍依赖 1.20.1 入口类，后续调用点会继续复制这个依赖。
- 把整个类移到节点源码。只有该类确实只在单节点运行时才成立；共享的 loot 注入逻辑移到节点会变成两套实现。
- 用 `Mc.logger()` / `Mc.gregtechId()` 替代 `GregTech.LOGGER` / `GregTech.id()`。同一原则：共享门面吸收节点实现，调用点不再 import 节点入口类。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)