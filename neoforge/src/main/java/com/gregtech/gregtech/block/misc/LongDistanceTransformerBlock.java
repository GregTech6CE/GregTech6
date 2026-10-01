package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

/** GT6 Long Distance Transformer — high-voltage long-distance power endpoint. */
public class LongDistanceTransformerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final long voltage;
    @Override public com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public LongDistanceTransformerBlock(long voltage, Properties properties) {
        super(properties);
        this.voltage = voltage;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    public long voltage() { return voltage; }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,hand,hit)==net.minecraft.world.InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    public net.minecraft.world.InteractionResult interact(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        var held=player.getItemInHand(hand);
        if(com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER)) {
            if(!level.isClientSide && level.getBlockEntity(pos) instanceof LongDistanceTransformerBlockEntity endpoint) {
                endpoint.setStopped(!endpoint.isStopped());
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForUse(held,1,player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LongDistanceTransformerBlockEntity(pos, state); }
}
