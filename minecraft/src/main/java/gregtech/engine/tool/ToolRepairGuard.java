package gregtech.engine.tool;

import gregtech.content.item.GTToolItem;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
*///?} else {
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * GT6 meta-tools have no vanilla repair path; the anvil and grindstone combine branches copy the
 * left stack but still treat the tools as ordinary damageable items.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class ToolRepairGuard {
    private ToolRepairGuard() {}

    @SubscribeEvent
    public static void anvil(AnvilUpdateEvent event) {
        if (!isGtTool(event.getLeft()) || !isGtTool(event.getRight())) return;
        // Forge 1.20.1 leaves the previous result in the slot when the event is canceled.
        if (event.getPlayer() != null && event.getPlayer().containerMenu instanceof AnvilMenu menu) {
            menu.getSlot(2).set(ItemStack.EMPTY);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void grindstone(GrindstoneEvent.OnPlaceItem event) {
        if (isGtTool(event.getTopItem()) && isGtTool(event.getBottomItem())) event.setCanceled(true);
    }

    private static boolean isGtTool(ItemStack stack) {
        return stack.getItem() instanceof GTToolItem;
    }
}
