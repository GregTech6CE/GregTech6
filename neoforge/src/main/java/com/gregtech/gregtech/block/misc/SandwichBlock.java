package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 MultiTileEntitySandwich: layered food that can also provide redstone. */
public final class SandwichBlock extends Block implements EntityBlock {
    private static final VoxelShape DEFAULT_SHAPE = Block.box(1, 0, 1, 15, 1, 15);

    public SandwichBlock(Properties properties) {
        super(properties.dynamicShape().noOcclusion());
    }

    @Override protected com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(state,level,pos,player,hand,hit)==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SandwichBlockEntity(pos, state);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof SandwichBlockEntity sandwich
                ? Block.box(1, 0, 1, 15, sandwich.sizePixels(), 15) : DEFAULT_SHAPE;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos())
                ? defaultBlockState() : null;
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SandwichBlockEntity sandwich)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return InteractionResult.CONSUME;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack container = held.getCraftingRemainingItem();
        int amount = sandwich.addIngredient(held);
        if (amount > 0) {
            if (!player.getAbilities().instabuild) {
                held.shrink(amount);
                if (!container.isEmpty()) {
                    ItemStack remainder = container.copyWithCount(container.getCount() * amount);
                    if (!player.getInventory().add(remainder)) player.drop(remainder, false);
                }
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 1f);
        }
        return InteractionResult.CONSUME;
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }

    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SandwichBlockEntity sandwich
                ? sandwich.redstoneSignal() : 0;
    }

    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SandwichBlockEntity sandwich
                ? sandwich.comparatorSignal() : 0;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SandwichBlockEntity sandwich) {
            ItemStack drop = sandwich.consumeDrop();
            return drop.isEmpty() ? List.of() : List.of(drop);
        }
        return List.of(new ItemStack(this));
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SandwichBlockEntity sandwich) {
            ItemStack drop = sandwich.consumeDrop();
            if (!drop.isEmpty()) popResource(level, pos, drop);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
