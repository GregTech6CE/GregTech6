package gregtech.event;

import gregtech.content.block.machine.MultiblockPortBlock;
import gregtech.content.block.machine.VesselInnerFaces;
import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.engine.fluid.MoltenMaterialStorage;
import gregtech.engine.machine.crucible.CrucibleContainerInteraction;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
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
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID)
public final class CrucibleInteractionHandler {
    private CrucibleInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getEntity().isSpectator()) return;
        var level = event.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        var state = level.getBlockState(event.getPos());
        boolean container = CrucibleContainerInteraction.handles(event.getEntity(), event.getItemStack());
        // Any face that opens into the vessel accepts containers: the mouth itself, the four inner
        // walls, and the large crucible's wall parts. Outer faces stay free for block placement.
        var hit = event.getHitVec();
        boolean crucibleFace = hit.getDirection() == Direction.UP
                || VesselInnerFaces.isInnerFace(state, hit)
                || VesselInnerFaces.isLargeCrucibleInnerFace(blockEntity, hit);
        // The face a click opens into can reach the hollow too, e.g. the underside of a block that
        // rests on the mouth, so such a click must not place or light anything inside the vessel.
        boolean cavityTarget = (crucibleFace && VesselInnerFaces.isFormedLargeCrucibleCavityTarget(state, blockEntity, hit))
                || VesselInnerFaces.isFormedLargeCrucibleAdjacentCavity(level, hit);
        // A formed mouth rejects every item that could light a fire or put a block into the hollow:
        // vanilla flint and steel, fire charges, block items and GT's igniters all use the item path
        // that useItem denies, and a denied useItem also skips onItemUseFirst. GT tools keep item use
        // so the magnifier, builder's wand, scraper and wrench stay available; the container and
        // scrape paths below are handled directly and cancel the event themselves when they act.
        if (cavityTarget && !keepsItemUse(event.getItemStack())) denyItemUse(event);
        if (!crucibleFace) return;
        if (level.isClientSide()) {
            var formed = MultiblockPortBlock.CRUCIBLE_FORMED;
            if (container && (blockEntity instanceof MoltenMaterialStorage
                    || (state.hasProperty(formed) && state.getValue(formed)))) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        SmeltingCrucibleBlockEntity crucible = resolveCrucible(blockEntity, container);
        if (crucible == null) return;
        if (crucible instanceof LargeCrucibleControllerBlockEntity large && !large.isStructureOk()) return;

        InteractionResult result = CrucibleContainerInteraction.interact(
                crucible, event.getEntity(), event.getHand(), event.getPos());
        if (result == InteractionResult.PASS) {
            result = crucible.tryUse(event.getEntity(), event.getHand(), hit);
        }
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static SmeltingCrucibleBlockEntity resolveCrucible(BlockEntity blockEntity, boolean container) {
        if (blockEntity instanceof SmeltingCrucibleBlockEntity crucible) return crucible;
        if (blockEntity instanceof MultiblockPortBlockEntity port
                && (container || port.isCrucibleUpperPort())
                && port.crucibleController() instanceof SmeltingCrucibleBlockEntity crucible) {
            return crucible;
        }
        return null;
    }

    /**
     * GT tools other than the igniter still need {@code onItemUseFirst} on the cavity faces, which a
     * denied useItem would skip; ordinary items have nothing to lose there.
     */
    private static boolean keepsItemUse(ItemStack stack) {
        return GTToolHelper.isTool(stack) && !GTToolHelper.matchesTool(stack, GTToolType.FLINT_AND_TINDER);
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
