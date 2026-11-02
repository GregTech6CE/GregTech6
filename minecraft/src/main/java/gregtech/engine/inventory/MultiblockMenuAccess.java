package gregtech.engine.inventory;

import gregtech.engine.multiblock.tracking.StructureWatchIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Per-menu access point. Inventory and synchronization always remain on the controller. */
public final class MultiblockMenuAccess {
    public interface Host {
        MultiblockMenuAccess multiblockMenuAccess();
    }

    private final BlockEntity controller;
    private BlockPos position;

    public MultiblockMenuAccess(BlockEntity controller) {
        this.controller = controller;
        this.position = controller == null ? null : controller.getBlockPos();
    }

    /** The server binds an already validated click, never a position supplied by a client menu. */
    public boolean fromPart(BlockPos part) {
        if (controller == null || !(controller.getLevel() instanceof ServerLevel level)
                || StructureWatchIndex.formedController(level, part) != controller) return false;
        position = part.immutable();
        return true;
    }

    public boolean stillValid(Player player) {
        if (controller == null) return true; // Basic machine client menus have no controller entity.
        var level = player.level();
        if (controller.isRemoved() || controller.getLevel() != level
                || !level.hasChunkAt(controller.getBlockPos()) || !level.hasChunkAt(position)
                || level.getBlockEntity(controller.getBlockPos()) != controller) return false;
        if (!position.equals(controller.getBlockPos()) && level instanceof ServerLevel server
                && (StructureWatchIndex.formedController(server, position) != controller
                    || !level.mayInteract(player, position) || !level.mayInteract(player, controller.getBlockPos())))
            return false;
//? if >=1.21.1 {
        /*return player.canInteractWithBlock(position, 4.0);
*///?} else {
        // Same padding as Forge's AbstractContainerMenu, including reach attributes from addons.
        double reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()) + 3.5;
        return player.distanceToSqr(position.getCenter()) <= reach * reach;
//?}
    }
}
