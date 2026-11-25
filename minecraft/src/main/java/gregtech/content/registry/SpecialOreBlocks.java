package gregtech.content.registry;

import net.minecraft.world.level.block.Block;

/**
 * Membership lookup over the active platform node's registered special ore block list.
 * Keeps 1.21.1 {@code GTSpecialOreBlocks.all()} and 1.20.1 {@code GTIconSetBlocks.all()} semantics unchanged.
 */
public final class SpecialOreBlocks {
    private SpecialOreBlocks() {}

    public static boolean contains(Block block) {
//? if >=1.21.1 {
/*        return GTSpecialOreBlocks.all().stream()
                .anyMatch(holder -> holder.isBound() && holder.get() == block);
*///?} else {
        return GTIconSetBlocks.all().stream()
                .anyMatch(holder -> holder.isPresent() && holder.get() == block);
//?}
    }
}
