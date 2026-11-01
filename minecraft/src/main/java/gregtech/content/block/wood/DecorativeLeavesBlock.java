package gregtech.content.block.wood;

import net.minecraft.world.level.block.LeavesBlock;

/** Non-decaying leaf variants use vanilla foliage lighting, culling and waterlogging. */
public final class DecorativeLeavesBlock extends LeavesBlock {
    public DecorativeLeavesBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(defaultBlockState().setValue(PERSISTENT, true));
    }
}
