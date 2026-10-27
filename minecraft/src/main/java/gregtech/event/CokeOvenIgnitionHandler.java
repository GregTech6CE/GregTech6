package gregtech.event;

import gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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

@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class CokeOvenIgnitionHandler {
    private CokeOvenIgnitionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void ignite(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        var held = player.getItemInHand(event.getHand());
        if (level.isClientSide || !held.is(Items.FLINT_AND_STEEL)
                || !player.mayBuild() || !level.mayInteract(player, event.getPos())
                || !(level.getBlockEntity(event.getPos()) instanceof CokeOvenControllerBlockEntity oven)) return;
        var hit = event.getHitVec();
        var pos = event.getPos();
        long cost = oven.onIgnite(level, pos, hit.getDirection(), player, held, player.isShiftKeyDown(),
                (float) (hit.getLocation().x - pos.getX()), (float) (hit.getLocation().y - pos.getY()),
                (float) (hit.getLocation().z - pos.getZ()));
        if (cost <= 0) return;
        if (!player.isCreative()) {
//? if >=1.21.1 {
            /*held.hurtAndBreak(1, player, event.getHand() == InteractionHand.OFF_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
*///?} else {
            held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
//?}
        }
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }
}
