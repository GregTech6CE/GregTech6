package gregtech.event;

//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
*///?} else {
import gregtech.GregTech;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
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
 * Predict container handling on the client and apply transfers only on the server.
 * Both sides consume the click so BucketItem cannot also place fluid in the world.
 * <p>
 * Mold and basin right-click pour is handled by {@link SmelteryInteractionHandler}.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?}
public final class CrucibleInteractionHandler {
    private CrucibleInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSpectator()) return;
        if (event.getHitVec().getDirection() != Direction.UP) {
            return;
        }
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        boolean container = gregtech.engine.machine.crucible.CrucibleContainerInteraction.handles(event.getItemStack());
        if (event.getLevel().isClientSide()) {
            var state = event.getLevel().getBlockState(event.getPos());
            var formed = gregtech.content.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
            if (container && (blockEntity instanceof gregtech.engine.fluid.MoltenMaterialStorage
                    || state.hasProperty(formed) && state.getValue(formed))) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
//? if >=1.21.1 {
        /*SmeltingCrucibleEntity crucible = blockEntity instanceof SmeltingCrucibleEntity direct
*///?} else {
        SmeltingCrucibleBlockEntity crucible = blockEntity instanceof SmeltingCrucibleBlockEntity direct
//?}
                ? direct : blockEntity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity port
//? if >=1.21.1 {
                        /*&& (container || port.isCrucibleUpperPort()) && port.crucibleController() instanceof SmeltingCrucibleEntity owner ? owner : null;
*///?} else {
                        && (container || port.isCrucibleUpperPort()) ? port.crucibleController() : null;
//?}
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
}
