package gregtech.internal.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
//?}

/** Adjacent loaded inventory lookup without loading a neighboring chunk. */
public final class NativeItemInventories {
    private NativeItemInventories() {}
    public static IItemHandler at(Level level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
//? if >=1.21.1 {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
*///?} else {
        var owner = level.getBlockEntity(pos);
        return owner == null ? null : owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
//?}
    }
}
