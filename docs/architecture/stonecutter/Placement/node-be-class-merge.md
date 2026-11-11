# 节点 BE 类合并

## 子原因

同一个方块实体在两个节点各有一份完整实现：1.21.1 有 `SmeltingCrucibleEntity`，1.20.1 有 `SmeltingCrucibleBlockEntity`。两者实现相同的熔炼逻辑，只在平台 API 上不同；共享代码里的 `instanceof`、导入和注册都因此多出一条类型分支。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*        if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleEntity crucible)) {
*///?} else {
        if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleBlockEntity crucible)) {
//?}
            return InteractionResult.PASS;
        }
```

### 修复后

以 1.21.1 实现为基准合并到共享源码集，平台差异收进共享类内部的 `//?`，两个节点共用 `SmeltingCrucibleBlockEntity`：

```java
if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleBlockEntity crucible)) {
    return InteractionResult.PASS;
}
```

### 为什么这么修复

- 1.21.1 是当前实现参考；1.20.1 的差异集中在平台 API，不需要第二套熔炼逻辑。
- 合并后 `SmeltingCrucibleBlock`、`GTToolItem`、`DustFunnelBlockEntity`、`SensorBlockEntity`、Jade 和地牢代码的类型分支一起消失，共享代码只依赖一个类。
- 共享类内部只保留平台 API 分支：`load` / `saveAdditional` / `getUpdateTag` / `handleUpdateTag` / `onDataPacket` 签名、能力暴露（见 [Capability/capability-exposure.md](../Capability/capability-exposure.md)）、`ItemStack.isSameItemSameComponents` ↔ `isSameItemSameTags`、`ItemStackHandler` / `ModelData` 的加载器类型。
- 只转发共享类方法的节点包装类没有收益；删除后 `MoldBlock`、`SolidBurningBoxBlock` 直接构造共享 BE，注册也指向共享类。

### 其他可能的修复方式

- 保留两套实现，调用点继续分支。逻辑同步和平台修复都要做两遍。
- 抽一个共享接口，两个节点各自实现。调用点只依赖接口，但仍有两套完整实现；只在实现确实无法合并时使用。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)