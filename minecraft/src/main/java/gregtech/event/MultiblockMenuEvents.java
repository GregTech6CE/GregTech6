package gregtech.event;

import gregtech.engine.multiblock.MultiblockMenus;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.world.InteractionResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
//?}
public final class MultiblockMenuEvents {
    private MultiblockMenuEvents() {}

    /** Covers and direct vessel interactions run first; canceled/protected clicks never open a menu. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
//? if >=1.21.1 {
        /*if (event.getUseBlock() == net.neoforged.neoforge.common.util.TriState.FALSE) return;
*///?} else {
        if (event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
//?}
        InteractionResult result = MultiblockMenus.use(event.getEntity(), event.getHand(), event.getPos());
        if (result == InteractionResult.PASS) return;
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
