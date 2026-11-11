# Interaction

本页守卫方块交互签名的跨版本分支：1.20.1 `use` ↔ 1.21.1 `useItemOn` / `useWithoutItem`。

## 来源

- 1.20.1：`BlockBehaviour.use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → InteractionResult`（public）。
- 1.21.1：拆为 `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → ItemInteractionResult` 与 `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult) → InteractionResult`（均 protected）。
- `ItemInteractionResult`：`SUCCESS` / `CONSUME` / `CONSUME_PARTIAL` / `PASS_TO_DEFAULT_BLOCK_INTERACTION` / `SKIP_DEFAULT_BLOCK_INTERACTION` / `FAIL`。
- 这是真平台签名差异：游戏按版本直接调用这些覆写方法，签名分支无法用门面消除。1.20.1 无 `useItemOn` / `useWithoutItem` / `ItemInteractionResult`。

## 守卫规则

1. 交互身体统一到共享 helper（`interact` / `originalUse` / `useOriginal`），helper 签名跨版本恒为 `(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)`；`//?` 只承载签名适配与结果映射，不承载业务逻辑。
2. 身体已共享的纯签名分支属平台 API 例外，保留最小分支并写明原因；不为减少分支数新增转发层。
3. 两侧各写实质交互逻辑的，先统一调用链到共享 helper（优先 `ToolInteractions.use` + `ToolInteractionSpec` 声明式框架），再保留签名分支。
4. 结果映射（`InteractionResult` ↔ `ItemInteractionResult`）当前存在多套写法；统一映射属一致性清理，不减少分支，需行为验证，非本守卫强制。

## 规范示例

共享 helper + 签名适配器（身体共享，`//?` 只改名/可见性/结果映射）：

```java
private InteractionResult interact(BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
    // 共享交互身体，跨版本一致
    ...
}

//? if >=1.21.1 {
/*@Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
    InteractionResult result = interact(state, level, pos, player, hand, hit);
    return result == InteractionResult.PASS
            ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            : ItemInteractionResult.sidedSuccess(level.isClientSide);
}
@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
}
*///?} else {
@Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
    return interact(state, level, pos, player, hand, hit);
}
//?}
```

## 验证

- 双节点 `stonecutterGenerate` + `compileJava`：1.21.1 覆写 `useItemOn` / `useWithoutItem`，1.20.1 覆写 `use`。
- 身体共享后，交互行为差异按 parity 流程记录；签名分支本身无行为影响。