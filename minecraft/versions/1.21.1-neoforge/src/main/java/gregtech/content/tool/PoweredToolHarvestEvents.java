package gregtech.content.tool;
import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.*;import net.neoforged.neoforge.event.level.BlockEvent;
/** Native replacement for removed item start-break callback; respect prior protection cancellation. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class PoweredToolHarvestEvents {
 private PoweredToolHarvestEvents(){}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void breaking(BlockEvent.BreakEvent event){
  if(event.isCanceled())return;var player=event.getPlayer();var stack=player.getMainHandItem();
  if(stack.getItem() instanceof gregtech.content.item.PocketToolItem pocket&&pocket.onBlockStartBreak(stack,event.getPos(),player)){event.setCanceled(true);return;}
  if(stack.getItem() instanceof gregtech.content.item.ElectricToolItem tool&&tool.onBlockStartBreak(stack,event.getPos(),player))event.setCanceled(true);
 }
}
