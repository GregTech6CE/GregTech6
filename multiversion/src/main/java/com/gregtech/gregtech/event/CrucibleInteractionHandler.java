package com.gregtech.gregtech.event;

//? if >=1.21.1 {
/*import com.gregtech.gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
*///?} else {
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
//?}
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
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

/**
 * Server-side crucible interactions. {@link net.minecraft.world.level.block.Block#use} runs on the
 * client first and returning {@link InteractionResult#SUCCESS} there can prevent the server packet
 * from applying block-entity changes; all authoritative logic lives here instead.
 * <p>
 * Mold and basin right-click pour is handled by {@link SmelteryInteractionHandler}.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?}
public final class CrucibleInteractionHandler {
    private CrucibleInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHitVec().getDirection() != Direction.UP) {
            return;
        }
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
//? if >=1.21.1 {
        /*SmeltingCrucibleEntity crucible = blockEntity instanceof SmeltingCrucibleEntity direct
*///?} else {
        SmeltingCrucibleBlockEntity crucible = blockEntity instanceof SmeltingCrucibleBlockEntity direct
//?}
                ? direct : blockEntity instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity port
//? if >=1.21.1 {
                        /*&& port.isCrucibleUpperPort() && port.crucibleController() instanceof SmeltingCrucibleEntity owner ? owner : null;
*///?} else {
                        && port.isCrucibleUpperPort() ? port.crucibleController() : null;
//?}
        if (crucible == null) return;
        if (crucible instanceof com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity large
                && !large.isStructureOk()) return;
        InteractionResult result = crucible.tryUse(event.getEntity(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
