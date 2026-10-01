package com.gregtech.gregtech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

/** Twigs lying on the forest floor (GT6 {@code WorldgenSticks}) — drop vanilla sticks. */
public class TwigBlock extends Block {
    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0);

    public TwigBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(Items.STICK, 1 + builder.getLevel().random.nextInt(2)));
    }

    /** Right-click picks the twigs up as sticks. */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
                                                     BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand,
                                                     net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack stack = new ItemStack(Items.STICK, 1 + level.random.nextInt(2));
            level.removeBlock(pos, false);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
