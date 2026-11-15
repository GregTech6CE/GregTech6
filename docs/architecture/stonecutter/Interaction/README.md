# Interaction

本页守卫方块交互签名的跨版本分支：1.20.1 `use` ↔ 1.21.1 `useItemOn` / `useWithoutItem`。

## 来源

- 1.20.1：`BlockBehaviour.use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → InteractionResult`（public）。
- 1.21.1：拆为 `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → ItemInteractionResult` 与 `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult) → InteractionResult`（均 protected）。
- `ItemInteractionResult`：`SUCCESS` / `CONSUME` / `CONSUME_PARTIAL` / `PASS_TO_DEFAULT_BLOCK_INTERACTION` / `SKIP_DEFAULT_BLOCK_INTERACTION` / `FAIL`。
- 这是真平台签名差异：游戏按版本直接调用这些覆写方法，签名分支无法用门面消除。1.20.1 无 `useItemOn` / `useWithoutItem` / `ItemInteractionResult`。

## 守卫规则

1. 交互身体统一到共享 `use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → InteractionResult`，只返回 1.20 枚举值（`SUCCESS` / `CONSUME` / `CONSUME_PARTIAL` / `PASS` / `FAIL`）。1.20.1 侧为 `@Override`，1.21.1 侧为普通共享方法（无 `@Override`）；`//?` 只承载签名适配与结果映射，不承载业务逻辑。
2. 1.21.1 适配器 `useItemOn` 经 `ToolInteractions.toItemResult(use(...))` 映射，`useWithoutItem` 调 `use(..., MAIN_HAND, ...)`。身体已共享的纯签名分支属平台 API 例外，保留最小分支。
3. 两侧各写实质交互逻辑的，先统一调用链到共享 `use`（优先 `ToolInteractions.use` + `ToolInteractionSpec` 声明式框架），再保留签名分支。
4. 结果映射统一走 `ToolInteractions.toItemResult`（1.21 facade）：`SUCCESS→SUCCESS`、`CONSUME→CONSUME`、`CONSUME_PARTIAL→CONSUME_PARTIAL`、`FAIL→FAIL`、`PASS→SKIP_DEFAULT_BLOCK_INTERACTION`。PASS 映射为 SKIP_DEFAULT 使游戏跳过 `useWithoutItem`，共享 `use` 只跑一次（避免 PASS_TO_DEFAULT 触发的双调用）；前提是 PASS 无副作用。
5. 调用点集中、格式化、按注意力排列：1.21 适配器与共享 `use` 身体必须相邻成一组（适配器在前作为版本入口，`use` 身体紧随其后），整体置于类声明后的显著位置，不散落类两端。含 `//?` 的文件不被共享 spotless 自动格式化，分支段内代码须手动按 spotless 风格格式化（多行签名、缩进、运算符空格），保证可读。

## 规范示例

共享 `use` 身体 + 签名适配器（`toItemResult` 统一映射）：

```java
//? if >=1.21.1 {
/*@Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
    return ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
}
@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
}
*///?} else {
@Override
//?}
public InteractionResult use(BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
    // 共享交互身体，跨版本一致，只返回 1.20 InteractionResult 值
    if (ToolInteractions.use(state, level, pos, player, hand, hit))
        return InteractionResult.sidedSuccess(level.isClientSide);
    return InteractionResult.PASS;
}
```

`toItemResult` 位于 `gregtech.engine.tool.ToolInteractions` 的 `//? if >=1.21.1` 段（`ItemInteractionResult` 为 1.21-only；switch 须穷举 1.21 的 `InteractionResult`，含 `SUCCESS_NO_ITEM_USED`）。1.21 分派仅在 `useItemOn` 返回 `PASS_TO_DEFAULT_BLOCK_INTERACTION` 时才调 `useWithoutItem`（client `MultiPlayerGameMode` 与 server `ServerPlayerGameMode` 一致）；映射 PASS→SKIP_DEFAULT 即跳过该二次调用。

## 验证

- 双节点 `stonecutterGenerate` + `compileJava`：1.21.1 覆写 `useItemOn` / `useWithoutItem`，1.20.1 覆写 `use`。
- 身体共享后，交互行为差异按 parity 流程记录；签名分支本身无行为影响。