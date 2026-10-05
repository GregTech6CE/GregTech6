package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

/** F6.9: Bedrock Drilling Rig controller. */
public class BedrockDrillControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    @Override public com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public BedrockDrillControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BedrockDrillControllerBlockEntity(pos, state); }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.BEDROCK_DRILL.get()) return null;
        return (l, p, s, be) -> BedrockDrillControllerBlockEntity.serverTick(l, p, s, (BedrockDrillControllerBlockEntity) be);
    }

    private net.minecraft.world.InteractionResult interact(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity machine) {
            if(level.isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
            var tank=machine.fluidCapability(hit.getDirection());
            if(tank!=null&&net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,tank))return net.minecraft.world.InteractionResult.CONSUME;
            if(player.getItemInHand(hand).isEmpty()) {
                var inventory=machine.itemCapability(Direction.UP);
                if(inventory!=null) {var stack=inventory.extractItem(0,64,false);if(!player.getInventory().add(stack))player.drop(stack,false);}
                return net.minecraft.world.InteractionResult.CONSUME;
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity machine)machine.dropContents();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new net.minecraft.world.item.ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof BedrockDrillControllerBlockEntity machine) {
            var data=com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(machine,machine.saveWithId(machine.getLevel().registryAccess()));data.remove("gt.output");stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        }
        return java.util.List.of(stack);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
}
