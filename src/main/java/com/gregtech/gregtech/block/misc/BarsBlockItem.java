package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A second GT6 bar can be added to a block without replacing the first one. */
public final class BarsBlockItem extends BlockItem {
    public BarsBlockItem(BarsBlock block, Properties properties) { super(block, properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (state.is(getBlock()) && player != null && !player.isShiftKeyDown()) {
            int mask = state.getValue(BarsBlock.MASK);
            Direction face = context.getClickedFace();
            double x = Mth.frac(context.getClickLocation().x);
            double z = Mth.frac(context.getClickLocation().z);
            int bit = chosenBit(mask, face, x, z);
            if ((mask & bit) == 0 && bit != 0) return join(context, pos, state, bit);
            // GT6 checks the neighboring cell only after all four segments are present.
            if (mask != 15) return InteractionResult.PASS;
            BlockPos neighbor = pos.relative(face);
            BlockState adjacent = level.getBlockState(neighbor);
            if (adjacent.is(getBlock())) {
                int adjacentMask = adjacent.getValue(BarsBlock.MASK);
                int adjacentBit = chosenBit(adjacentMask, face, x, z);
                if ((adjacentMask & adjacentBit) == 0 && adjacentBit != 0)
                    return join(context, neighbor, adjacent, adjacentBit);
                if (adjacentMask != 15) return InteractionResult.PASS;
            }
            // The neighbor is replaceable (or occupied by another block); ordinary placement
            // performs GT6's final replaceability and permission checks.
        }
        return super.useOn(context);
    }

    private static int chosenBit(int mask, Direction face, double x, double z) {
        int bit = Integer.bitCount(mask) == 3 ? 15 ^ mask : BarsBlock.placementBit(Direction.UP, x, z);
        if ((mask & bit) != 0 || face.getAxis().isHorizontal()) bit = BarsBlock.placementBit(face, x, z);
        if ((mask & bit) != 0 && face.getAxis().isHorizontal()) bit = BarsBlock.faceBit(face);
        return bit;
    }

    private static InteractionResult join(UseOnContext context, BlockPos pos, BlockState state, int bit) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BarsBlock.MASK, state.getValue(BarsBlock.MASK) | bit), 3);
            level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
