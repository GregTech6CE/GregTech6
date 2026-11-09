# 行为案例

## 子原因

以下案例都落在真实的 `//?` 分支附近。分支本身可能只是 API 差异，但同一段逻辑里承载 CE 与原版的行为差异；折叠分支时不能顺手改掉这些行为。

## 例子

### `craftKind` 与 `isInteractionTool`

`GTToolHelper.isInteractionTool` 的 `//?` 分支只差电动工具类型：

```java
public static boolean isInteractionTool(ItemStack stack) {
//? if >=1.21.1 {
    return isTool(stack) || stack.getItem() instanceof PoweredToolItem electric
//?} else {
    return isTool(stack) || stack.getItem() instanceof ElectricToolItem electric
//?}
            && electric.interactionType(stack) != null;
}
```

分支看起来只是 `PoweredToolItem` ↔ `ElectricToolItem` 的改名，但 `isTool(stack)` 守卫不能省：它排除没有 `GT.ToolStats` 的堆叠。原版按物品基础 `toolType()` 计算合成损耗，不按口袋工具当前模式。

### `plunger`

`GTToolItem.onItemUseFirst` 里有一个 `//?` 分支：

```java
//? if >=1.21.1 {
if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleEntity crucible)) {
//?} else {
if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleBlockEntity crucible)) {
//?}
    return InteractionResult.PASS;
}
```

折叠这个类型分支时，不能改动同一方法里的 plunger 顺序：原版从输出罐、输入罐依次抽 1000 mB；CE 补进 `useMachineTool`，用 `GarbageData` 丢弃。

### `igniter`

`IgnitableBlockHandler` 的 `//?` 分支只差事件总线 import：

```java
//? if >=1.21.1 {
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}
```

折叠 import 分支时，不能改动 `ignite` 的行为：物品级点火行为已存在，但 `useRemainder` 的 GUI 会吞掉；对会消费点火器的宿主返回 `PASS`。

### 封面工具守卫

`UtilityCoverInteraction` 的 `//?` 分支同时包含 provider 参数与 `consumesTool` 守卫：

```java
//? if >=1.21.1 {
if(!host.consumesTool(held)){
    if(!level.isClientSide){if(item)CoverUtilityBehaviors.setItemFilter(installed,held,level.registryAccess());else CoverUtilityBehaviors.setFluidFilter(installed,sample(held),level.registryAccess());host.panels().changed();}
//?} else {
if(!host.consumesTool(held)){
    if(!level.isClientSide){if(item)CoverUtilityBehaviors.setItemFilter(installed,held);else CoverUtilityBehaviors.setFluidFilter(installed,sample(held));host.panels().changed();}
//?}
    return InteractionResult.sidedSuccess(level.isClientSide);
}
```

分支看起来只差 `level.registryAccess()`，但 `!host.consumesTool(held)` 守卫是行为差异：静态列表跨宿主近似，漏 `igniter`/`plunger`、多 `soldering_iron`；改为 `PanelCoverHost.consumesTool`，由宿主按实际工具动作覆盖。

## 为什么保留这些分支

这些分支承载的是行为差异，不是版本差异。折叠前先确认原版消费路径与 CE 的补充逻辑；只按代码相似度折叠会丢失 `isTool` 守卫、plunger 的抽取顺序、igniter 的 `PASS` 行为或封面工具守卫。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
