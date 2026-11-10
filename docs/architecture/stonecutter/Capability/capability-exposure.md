# 能力暴露

## 子原因

共享方块实体要向两个加载器暴露物品与流体能力，但暴露位置不同：NeoForge 由 `RegisterCapabilitiesEvent` 在节点事件里注册，Forge 由方块实体重写 `getCapability` 返回 `LazyOptional`。两边各写一套 BE 会让能力代码随 BE 类重复；把注册塞进共享类又无法同时编译。

## 例子

### 修复前

共享调用点按节点类型分叉，才能拿到对方块实体的能力：

```java
//? if >=1.21.1 {
/*public static InteractionResult interact(SmeltingCrucibleEntity crucible, Player player,
*///?} else {
public static InteractionResult interact(SmeltingCrucibleBlockEntity crucible, Player player,
//?}
        InteractionHand hand, BlockPos hitPos) {
    var handler = new MoltenFluidHandler(crucible);
    ...
}
```

### 修复后

调用点只依赖共享类型，能力暴露集中在共享 BE；NeoForge 用查询方法，Forge 保留 `getCapability`：

```java
//? if >=1.21.1 {
/*public IItemHandler itemInputHandler(@Nullable Direction side) {
    return side == Direction.UP && !isRemoved() ? itemInput : null;
}

public IFluidHandler moltenFluidHandler() {
*///?} else {
@Override
public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
    if (capability == ForgeCapabilities.ITEM_HANDLER && side == Direction.UP)
        return itemInputCapability.cast();
    if (capability == ForgeCapabilities.FLUID_HANDLER && side != Direction.DOWN)
        return moltenFluidCapability.cast();
    return super.getCapability(capability, side);
}

@Override
public void invalidateCaps() {
    super.invalidateCaps();
    moltenFluidCapability.invalidate();
    itemInputCapability.invalidate();
}

public IFluidHandler moltenFluidHandler() {
//?}
    return new MoltenFluidHandler(this);
}
```

NeoForge 节点把查询方法接到事件注册：

```java
event.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK,
        GTBlockEntities.SMELTING_CRUCIBLE.get(),
        (crucible, side) -> crucible.itemInputHandler(side));
```

### 为什么这么修复

- 能力注册位置是平台 API 运行时差异，不能共享；暴露口集中在共享 BE，注册留在节点。
- 两侧 side 规则必须一致：物品输入只在上表面，流体输入除底面外。
- Forge 的 `LazyOptional` 需要缓存并在 `invalidateCaps` / `reviveCaps` 重建；NeoForge 每次从事件取值，不需要 BE 缓存。
- 分支保留在共享类的暴露段，调用点不再分支；验证时两个节点分别检查能力查询的 side 与空值行为。

### 其他可能的修复方式

- 让共享调用点继续按加载器查询。每个消费者都要重复 `LazyOptional` / `Optional` 解析。
- 把能力注册也放进共享类。`RegisterCapabilitiesEvent` 与 `getCapability` 都是加载器 API，共享类无法同时编译。
- 用共享门面包住两侧。门面只能包装查询，不能改变注册位置，分支仍在。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)