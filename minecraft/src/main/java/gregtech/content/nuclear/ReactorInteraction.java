package gregtech.content.nuclear;
import gregtech.engine.tool.*;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
public final class ReactorInteraction {

    private ReactorInteraction() {
    }

    public static InteractionResult use(ReactorCoreBlockEntity core, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (ToolInteractions.use(core.getBlockState(), core.getLevel(), core.getBlockPos(), player, hand,
                hit)) return InteractionResult.sidedSuccess(core.getLevel().isClientSide);
        if (core.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var held = player.getItemInHand(hand);
        if (GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER)) {
            core.setStopped(!core.stopped);
            GTToolHelper.damageForUse(held, 1, player);
//? if >=1.21.1 {
        /*} else if (net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, core.handFluids)) return InteractionResult.SUCCESS;
*///?} else {
        } else if (net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, core.handFluids)) return InteractionResult.SUCCESS;
//?}
        else if (GTToolHelper.matchesTool(held, GTToolType.PINCERS) && hit.getDirection() == net.minecraft.core.Direction.UP) {
            var removedRod = core.overflowCount() > 0 ? core.removeRod() : core.removeRod(topSlot(core, hit));
            if (!removedRod.isEmpty()) {
                // Inventory.add silently deletes rejected stacks in creative mode, including rod progress.
                player.getInventory().placeItemBackInInventory(removedRod);
                if (!held.isEmpty()) GTToolHelper.damageForUse(held, 1, player);
            }
        } else if (ReactorCoreBlockEntity.isFuelRod(held) && hit.getDirection() == net.minecraft.core.Direction.UP) {
            if (core.insertHandRod(topSlot(core, hit), held)) {
                if (!player.isCreative()) held.shrink(1);
            }
        }

        player.displayClientMessage(Component.translatable("message.gt6ce.reactor.detail", core.rodCount(), core.rods.length, core.neutronTotal(),
                core.lastHeat,
                Component.translatable(core.stopped ? "message.gt6ce.reactor.stopped" : "message.gt6ce.reactor.running"), core.overflowCount()),
                        true);
        return InteractionResult.SUCCESS;
    }

    public static int topSlot(ReactorCoreBlockEntity core, BlockHitResult hit) {
        if (core.rods.length == 1) return 0;
        var localHit = hit.getLocation().subtract(core.getBlockPos().getX(), core.getBlockPos().getY(), core.getBlockPos().getZ());
        return (localHit.x >= 0.5 ? 2 : 0) + (localHit.z >= 0.5 ? 1 : 0);
    }
}
