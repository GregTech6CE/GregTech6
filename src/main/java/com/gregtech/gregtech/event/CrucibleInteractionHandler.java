package com.gregtech.gregtech.event;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side crucible interactions. {@link net.minecraft.world.level.block.Block#use} runs on the
 * client first and returning {@link InteractionResult#SUCCESS} there can prevent the server packet
 * from applying block-entity changes; all authoritative logic lives here instead.
 * <p>
 * Mold and basin right-click pour is handled by {@link SmelteryInteractionHandler}.
 */
@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
        SmeltingCrucibleBlockEntity crucible = blockEntity instanceof SmeltingCrucibleBlockEntity direct
                ? direct : blockEntity instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity port
                        && port.isCrucibleUpperPort() ? port.crucibleController() : null;
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
