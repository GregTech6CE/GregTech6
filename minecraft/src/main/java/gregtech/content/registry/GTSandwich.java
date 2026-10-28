package gregtech.content.registry;

import gregtech.content.block.misc.SandwichBlock;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTSandwich {
    private GTSandwich() {}

    public static RegisteredBlock<SandwichBlock> SANDWICH_BLOCK;

    public static void initialize() {
        SANDWICH_BLOCK = RegisteredBlock.registerBlock(
                "sandwich_block",
                () -> new SandwichBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_YELLOW)
                        .strength(.25f, 0f)
                        .sound(SoundType.WOOL)
                        .noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register(
                "sandwich_block",
                () -> new gregtech.content.item.SandwichBlockItem(
                        SANDWICH_BLOCK.get(), new net.minecraft.world.item.Item.Properties()));
    }
}
