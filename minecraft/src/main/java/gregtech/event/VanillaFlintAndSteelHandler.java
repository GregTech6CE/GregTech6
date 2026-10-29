package gregtech.event;

import gregtech.GregTechConfig;
import gregtech.content.item.behavior.BehaviorFlintAndTinder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
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

/** Roll once before vanilla or machine-specific block ignition, not on entity use. */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class VanillaFlintAndSteelHandler {
    private VanillaFlintAndSteelHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void ignite(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        var held = player.getItemInHand(event.getHand());
        if (level.isClientSide || !held.is(Items.FLINT_AND_STEEL) || player.getAbilities().instabuild
                || !player.mayBuild() || !level.mayInteract(player, event.getPos())
                || !GregTechConfig.Rule.RANDOMIZE_VANILLA_FLINT_AND_STEEL.enabled()) return;
        if (level.random.nextInt(100) < BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
//? if >=1.21.1 {
        /*held.hurtAndBreak(1, player, event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
*///?} else {
        held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
//?}
    }
}
