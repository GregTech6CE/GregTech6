package gregtech.engine.multiblock;

import gregtech.api.addon.platform.AddonTool;
import gregtech.api.addon.platform.AddonTools;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.cover.CoverItems;
import gregtech.engine.inventory.MultiblockMenuAccess;
import gregtech.engine.multiblock.tracking.StructureWatchIndex;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;
//?}

/** Opens the controller's existing menu; no synthetic use action or inventory is created on a part. */
public final class MultiblockMenus {
    private MultiblockMenus() {}

    public static InteractionResult use(Player player, InteractionHand hand, BlockPos part) {
        if (player.isSpectator() || player.isSecondaryUseActive() || reservedItem(player.getItemInHand(hand)))
            return InteractionResult.PASS;
        var level = player.level();
        if (!level.hasChunkAt(part)) return InteractionResult.PASS;
        if (level.isClientSide) {
            // Ports already synchronize their owner; ordinary structural blocks are resolved by the server.
            return level.getBlockEntity(part) instanceof MultiblockPortBlockEntity port
                    && port.hasFormedController() && hasMenu(port.sensorTarget())
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel world))
            return InteractionResult.PASS;
        BlockEntity controller = StructureWatchIndex.formedController(world, part);
        if (controller == null || controller.getBlockPos().equals(part) || !hasMenu(controller)
                || !level.mayInteract(player, part) || !level.mayInteract(player, controller.getBlockPos()))
            return InteractionResult.PASS;
        var access = new MultiblockMenuAccess(controller);
        if (!access.fromPart(part) || !access.stillValid(player)) return InteractionResult.PASS;
        var previousMenu = server.containerMenu;
        if (controller instanceof BasicMachineBlockEntity machine
                && controller.getBlockState().getBlock() instanceof BasicMachineBlock block) {
            block.openMenu(server, machine);
        } else if (controller instanceof BehaviorMachineBlockEntity machine) {
//? if >=1.21.1 {
            /*server.openMenu(machine, buf -> buf.writeBlockPos(machine.getBlockPos()));
*///?} else {
            NetworkHooks.openScreen(server, machine, buf -> buf.writeBlockPos(machine.getBlockPos()));
//?}
        }
        if (server.containerMenu == previousMenu) return InteractionResult.PASS;
        if (!(server.containerMenu instanceof MultiblockMenuAccess.Host menu)
                || !menu.multiblockMenuAccess().fromPart(part)) {
            server.closeContainer();
            return InteractionResult.FAIL;
        }
        return InteractionResult.CONSUME;
    }

    private static boolean hasMenu(BlockEntity entity) {
        return entity instanceof BehaviorMachineBlockEntity
                || entity instanceof BasicMachineBlockEntity && entity.getBlockState().getBlock() instanceof BasicMachineBlock;
    }

    private static boolean reservedItem(ItemStack held) {
        if (held.isEmpty()) return false;
        if (held.getItem() instanceof gregtech.content.item.GTToolItem
                || GTToolHelper.isInteractionTool(held) || CoverItems.isCover(held)) return true;
        for (AddonTool tool : AddonTool.values()) if (AddonTools.matches(held, tool)) return true;
        return FluidUtil.getFluidHandler(held).isPresent();
    }
}
