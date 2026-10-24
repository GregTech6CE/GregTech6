/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package gregtech.content.food;

import gregtech.item.CannedFoodItem;
import net.minecraft.world.entity.LivingEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}

/** Original MultiItem behavior runs before vanilla handles owner right-click as sitting. */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID)
//?}
public final class CannedFoodInteractionEvents {
    private CannedFoodInteractionEvents() {}
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        var stack=event.getItemStack();
        if (!(stack.getItem() instanceof CannedFoodItem can) || !(event.getTarget() instanceof LivingEntity target)) return;
        var outcome=can.interactLivingEntity(stack,event.getEntity(),target,event.getHand());
        if(outcome.consumesAction()) { event.setCanceled(true); event.setCancellationResult(outcome); }
    }
}
