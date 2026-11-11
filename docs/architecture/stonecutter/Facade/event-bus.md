# 事件总线

## 子原因

1.20.1 用 `net.minecraftforge.event.*` / `Mod.EventBusSubscriber`；1.21.1 用 `net.neoforged.*` / `EventBusSubscriber`。同一事件处理器的 import、类级注解和 `hurtAndBreak` 签名因此分支。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

// 方法体内
//? if >=1.21.1 {
/*held.hurtAndBreak(1, player, event.getHand() == InteractionHand.OFF_HAND
        ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
*///?} else {
held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
//?}
```

### 修复后

保留最小的 import/注解分支和 `hurtAndBreak` 分支；`ignite` 的点火与取消事件逻辑共享，扣耐久只保留 `hurtAndBreak` 签名分支：

```java
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class IgnitableBlockHandler {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void ignite(PlayerInteractEvent.RightClickBlock event) { ... }
}
```

### 为什么这么修复

这是加载器事件 API 的运行时差异，不是行为差异；分支只包 import、类级注解和 `hurtAndBreak` 签名，不包点火逻辑。行为本身见[工具交互行为](../../../decisions/parity/tool-interactions.md)。

### 其他可能的修复方式

- 在共享层定义自己的注解/事件包装。成本高于保留 import 分支。
- 把事件处理器整体移到节点。会复制业务逻辑。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
