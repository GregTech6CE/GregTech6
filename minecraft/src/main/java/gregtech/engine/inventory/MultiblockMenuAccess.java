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
        if (controller == null
                || !(controller.getLevel() instanceof ServerLevel level)
                || StructureWatchIndex.formedController(level, part) != controller) return false;
        position = part.immutable();
        return true;
    }

    public boolean stillValid(Player player) {
        var level = player.level();
        // Only the server knows the validated part anchor; its close packet is authoritative.
        // Basic machine client menus also have no controller entity.
        if (level.isClientSide) return true;
        if (controller == null) return false;
        if (controller.isRemoved()
                || controller.getLevel() != level
                || !level.hasChunkAt(controller.getBlockPos())
                || !level.hasChunkAt(position)
                || level.getBlockEntity(controller.getBlockPos()) != controller) return false;
        if (!position.equals(controller.getBlockPos())
                && level instanceof ServerLevel server
                && (StructureWatchIndex.formedController(server, position) != controller
                        || !level.mayInteract(player, position)
                        || !level.mayInteract(player, controller.getBlockPos()))) return false;
        // Preserve the existing eight-block menu range on both platforms.
        return player.distanceToSqr(position.getCenter()) <= 64.0;
    }
}
