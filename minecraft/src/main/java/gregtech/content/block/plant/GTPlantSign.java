package gregtech.content.block.plant;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6's "a plant, easily displaced" sign.
 *
 * <p>In 1.7.10 every plant descended from {@code BlockBush} — crops, flowers, saplings, mushrooms,
 * lilies, tall grass, GT's own bushes. The modern {@code BushBlock} only covers part of that, and
 * GT's berry bush descends from {@code Block} directly, so asking {@code instanceof BushBlock}
 * refuses plants the original soil accepted.</p>
 */
public final class GTPlantSign {
    private GTPlantSign() {}

    public static boolean isPlant(Block block) {
        if (block instanceof BushBlock) return true;
//? if >=1.21.1 {
        /*return block instanceof net.minecraft.world.level.block.BushBlock;
*///?} else {
        return block instanceof net.minecraft.world.level.block.BushBlock
                || block instanceof net.minecraftforge.common.IPlantable;
//?}
    }

    public static boolean isPlant(BlockState state) {
        return isPlant(state.getBlock());
    }
}
