package gregtech.event;

import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
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
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/**
 * Predict container handling on the client and apply transfers only on the server.
 * Both sides consume the click so BucketItem cannot also place fluid in the world.
 * <p>
 * Mold and basin right-click pour is handled by {@link SmelteryInteractionHandler}.
 */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class CrucibleInteractionHandler {
    private CrucibleInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getEntity().isSpectator()) return;
        var level = event.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        var state = level.getBlockState(event.getPos());
        boolean container = gregtech.engine.machine.crucible.CrucibleContainerInteraction.handles(
                event.getEntity(), event.getItemStack());
        // Any face that opens into the vessel accepts containers: the mouth itself, the four inner
        // walls, and the large crucible's wall parts. Outer faces stay free for block placement.
        var hit = event.getHitVec();
        boolean crucibleFace = hit.getDirection() == Direction.UP
                || gregtech.content.block.machine.VesselInnerFaces.isInnerFace(state, hit)
                || gregtech.content.block.machine.VesselInnerFaces.isLargeCrucibleInnerFace(blockEntity, hit);
        // The face a click opens into can reach the hollow too, e.g. the underside of a block that
        // rests on the mouth, so such a click must not place or light anything inside the vessel.
        boolean cavityTarget = crucibleFace
                        && gregtech.content.block.machine.VesselInnerFaces
                                .isFormedLargeCrucibleCavityTarget(state, blockEntity, hit)
                || gregtech.content.block.machine.VesselInnerFaces.isFormedLargeCrucibleAdjacentCavity(level, hit);
        // A formed mouth rejects every item that could light a fire or put a block into the hollow:
        // vanilla flint and steel, fire charges, block items and GT's igniters all use the item path
        // that useItem denies, and a denied useItem also skips onItemUseFirst. GT tools keep item use
        // so the magnifier, builder's wand, scraper and wrench stay available; the container and
        // scrape paths below are handled directly and cancel the event themselves when they act.
        if (cavityTarget && !keepsItemUse(event.getItemStack())) denyItemUse(event);
        if (!crucibleFace) return;
        if (level.isClientSide()) {
            var formed = gregtech.content.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
            if (container && (blockEntity instanceof gregtech.engine.fluid.MoltenMaterialStorage
                    || state.hasProperty(formed) && state.getValue(formed))) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
SmeltingCrucibleBlockEntity crucible = blockEntity instanceof SmeltingCrucibleBlockEntity direct
                ? direct : blockEntity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity port
                        && (container || port.isCrucibleUpperPort())
                        && port.crucibleController() instanceof SmeltingCrucibleBlockEntity owner ? owner : null;
        if (crucible == null) return;
        if (crucible instanceof gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity large
                && !large.isStructureOk()) return;
        var transfer = gregtech.engine.machine.crucible.CrucibleContainerInteraction.interact(
                crucible, event.getEntity(), event.getHand(), event.getPos());
        if (transfer != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(transfer);
            return;
        }
        InteractionResult result = crucible.tryUse(event.getEntity(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    /**
     * GT tools other than the igniter still need {@code onItemUseFirst} on the cavity faces, which a
     * denied useItem would skip; ordinary items have nothing to lose there.
     */
    private static boolean keepsItemUse(net.minecraft.world.item.ItemStack stack) {
        return gregtech.engine.tool.GTToolHelper.isTool(stack)
                && !gregtech.engine.tool.GTToolHelper.matchesTool(
                        stack, gregtech.engine.tool.GTToolType.FLINT_AND_TINDER);
    }

//? if >=1.21.1 {
    /*private static void denyItemUse(PlayerInteractEvent.RightClickBlock event) {
        event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
    }
*///?} else {
    private static void denyItemUse(PlayerInteractEvent.RightClickBlock event) {
        event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }
//?}
}
