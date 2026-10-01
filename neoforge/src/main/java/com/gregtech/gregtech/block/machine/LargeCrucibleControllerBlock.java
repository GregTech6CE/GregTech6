package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Large Crucible multiblock controller. 3x3x3 hollow of crucible wall blocks, HU-powered.
 *  27x capacity of single-block crucible. */
public class LargeCrucibleControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    @Override public com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private final com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.Variant variant;

    public LargeCrucibleControllerBlock() {
        this(com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.LEGACY,
                Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    }

    public LargeCrucibleControllerBlock(Properties properties) {
        this(com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.LEGACY, properties);
    }

    public LargeCrucibleControllerBlock(
            com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.Variant variant,
            Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MultiblockPortBlock.CRUCIBLE_FORMED, false));
    }

    public com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.Variant variant() { return variant; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MultiblockPortBlock.CRUCIBLE_FORMED);
    }

    @Override public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED)
                ? net.minecraft.world.level.block.RenderShape.INVISIBLE : net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LargeCrucibleControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof LargeCrucibleControllerBlockEntity ce)
                LargeCrucibleControllerBlockEntity.serverTick(lvl, pos, st, ce);
        };
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.large_crucible.structure"));
        tooltip.add(Component.translatable("tooltip.gregtech.large_crucible.ports"));
        tooltip.add(Component.translatable("tooltip.gregtech.large_crucible.capacity"));
        tooltip.add(Component.translatable("tooltip.gregtech.large_crucible.meltdown",
                Math.round(variant.material().getMeltingPoint() * 1.10D)));
    }
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof LargeCrucibleControllerBlockEntity contents) {
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    private net.minecraft.world.InteractionResult interact(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(level.getBlockEntity(pos) instanceof LargeCrucibleControllerBlockEntity be)return level.isClientSide?(be.canHandleUse(player,hand)?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.PASS):be.tryUse(player,hand,hit);return net.minecraft.world.InteractionResult.PASS;}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    @Override public List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder context){return List.of(new ItemStack(this));}
}
