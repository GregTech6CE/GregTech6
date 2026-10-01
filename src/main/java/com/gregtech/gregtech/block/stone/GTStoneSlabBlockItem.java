package com.gregtech.gregtech.block.stone;

import com.gregtech.gregtech.block.MaterialBlockItem;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.util.GTPlacementCode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Places {@link GTStoneSlabBlock} with GT6 3×3 face orientation. */
public final class GTStoneSlabBlockItem extends MaterialBlockItem {
    public GTStoneSlabBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @Nullable
    public BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !(state.getBlock() instanceof GTStoneSlabBlock)) {
            return state;
        }
        BlockPos placePos = context.getClickedPos();
        Vec3 click = context.getClickLocation();
        float hitX = (float) (click.x - placePos.getX());
        float hitY = (float) (click.y - placePos.getY());
        float hitZ = (float) (click.z - placePos.getZ());
        Direction facing = GTPlacementCode.resolveSlabFace(context.getClickedFace(), hitX, hitY, hitZ);
        return state.setValue(GTStoneSlabBlock.FACING, facing);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (SlabPlacementHelper.wouldMergeToFullBlock(context, this)) {
            Level level = context.getLevel();
            BlockPos clickedPos = context.getClickedPos();
            BlockState existing = level.getBlockState(clickedPos);
            GTStoneSlabBlock existingSlab = (GTStoneSlabBlock) existing.getBlock();
            Block full = GTBlocks.getStone(existingSlab.stoneType(), existingSlab.variant());
            Player player = context.getPlayer();
            if (full != null && (player == null || player.mayUseItemAt(clickedPos, context.getClickedFace(), context.getItemInHand()))) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(clickedPos, full.defaultBlockState());
                    level.playSound(null, clickedPos,
                            full.defaultBlockState().getSoundType(level, clickedPos, player).getPlaceSound(),
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (player != null && !player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.place(context);
    }
}
