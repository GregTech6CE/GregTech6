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
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        com.gregtech.gregtech.client.OriginalControllerTooltips.standalone(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.BEDROCK_DRILL, tooltip);
    }

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

    @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity machine) {
            if(level.isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
            var tank=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,hit.getDirection()).resolve().orElse(null);
            if(tank!=null&&net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,tank))return net.minecraft.world.InteractionResult.CONSUME;
            if(player.getItemInHand(hand).isEmpty()) {
                var inventory=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.UP).resolve().orElse(null);
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
            var data=com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(machine,machine.saveWithoutMetadata());data.remove("gt.output");stack.getOrCreateTag().put("BlockEntityTag",data);
        }
        return java.util.List.of(stack);
    }
}
