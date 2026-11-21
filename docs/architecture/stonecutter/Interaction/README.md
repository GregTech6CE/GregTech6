# Interaction

本页守卫方块交互签名的跨版本分支：1.20.1 `use` ↔ 1.21.1 `useItemOn` / `useWithoutItem`。

## 来源

- 1.20.1：`BlockBehaviour.use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → InteractionResult`（public）。
- 1.21.1：拆为 `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) → ItemInteractionResult` 与 `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult) → InteractionResult`（均 protected）。
- `ItemInteractionResult`：`SUCCESS` / `CONSUME` / `CONSUME_PARTIAL` / `PASS_TO_DEFAULT_BLOCK_INTERACTION` / `SKIP_DEFAULT_BLOCK_INTERACTION` / `FAIL`。
- 这是真平台签名差异：游戏按版本直接调用这些覆写方法，签名分支无法用门面消除。1.20.1 无 `useItemOn` / `useWithoutItem` / `ItemInteractionResult`。

## 守卫规则

1. 优先继承 `GTBlock` / `GTDirectionalBlock` / `GTHorizontalDirectionalBlock`，只覆写双端共享的 `@Override protected InteractionResult useBlock(...)`；基类已集中处理 `1.20.1 use` / `1.21.1 useItemOn` / `useWithoutItem` 适配，子类未覆写时 `useBlock(...)` 返回 `null` 并直接委托 `super`，保留非交互方块默认的 `PASS_TO_DEFAULT_BLOCK_INTERACTION`。
2. 必须直接继承原版特化方块基类（如 `BaseRailBlock` / `RotatedPillarBlock`）时，交互身体统一到共享 `use(...)`，只返回 1.20 枚举值；1.21.1 适配器 `useItemOn` 经 `ToolInteractions.toItemResult(use(...))` 映射，`useWithoutItem` 调 `use(..., MAIN_HAND, ...)`，`//?` 只承载签名适配与结果映射，不承载业务逻辑。
3. 两侧各写实质交互逻辑的，先统一调用链到共享 `useBlock` / `use`（优先 `ToolInteractions.use` + `ToolInteractionSpec` 声明式框架），再保留最小签名分支。
4. 结果映射统一走 `ToolInteractions.toItemResult`（1.21 facade）：`SUCCESS→SUCCESS`、`CONSUME→CONSUME`、`CONSUME_PARTIAL→CONSUME_PARTIAL`、`FAIL→FAIL`、`PASS→SKIP_DEFAULT_BLOCK_INTERACTION`。子类显式返回 `PASS` 时映射为 `SKIP_DEFAULT` 以跳过 `useWithoutItem` 二次调用；只有基类默认 `useBlock` 返回 `null` 时才退回 `super.useItemOn(...)`（保留原版 `PASS_TO_DEFAULT_BLOCK_INTERACTION`）。
5. 调用点集中、格式化、按注意力排列：直接写签名分支的特例类须将 1.21 适配器与共享 `use` 身体相邻成组，置于类声明后显著位置。含 `//?` 的文件不被共享 spotless 自动格式化，分支段内代码须手动按 spotless 风格格式化（多行签名、缩进、运算符空格），保证可读。

## 规范示例

直接继承特化原版基类时的共享 `use` 身体 + 签名适配器（普通 `GTBlock` 家族只需 `@Override protected InteractionResult useBlock(...)`）：

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