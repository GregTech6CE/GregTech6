package com.gregtech.gregtech.block.stone;

import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Shared GT6 slab placement rules for server placement and client preview. */
public final class SlabPlacementHelper {
    private SlabPlacementHelper() {}

    /** True when placing would merge with an existing matching slab into a full block. */
    public static boolean wouldMergeToFullBlock(BlockPlaceContext context, GTStoneSlabBlockItem item) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState existing = level.getBlockState(clickedPos);
        if (!(existing.getBlock() instanceof GTStoneSlabBlock existingSlab)) {
            return false;
        }
        if (!existingSlab.equals(item.getBlock())) {
            return false;
        }
        if (context.getClickedFace() != existing.getValue(GTStoneSlabBlock.FACING).getOpposite()) {
            return false;
        }
        Block full = GTBlocks.getStone(existingSlab.stoneType(), existingSlab.variant());
        return full != null;
    }
}
