package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity;
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

/** F6.11: Implosion Compressor multiblock controller. */
public class ImplosionCompressorControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public ImplosionCompressorControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.BlockGetter context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        com.gregtech.gregtech.client.AdvancedControllerTooltips.append(com.gregtech.gregtech.content.machine.BasicMachineDefinitions.from(com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()),lines);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ImplosionCompressorControllerBlockEntity(pos, state); }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.IMPLOSION_COMPRESSOR.get()) return null;
        return (l, p, s, be) -> ImplosionCompressorControllerBlockEntity.serverTick(l, p, s, (ImplosionCompressorControllerBlockEntity) be);
    }

    @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(MachineRotationType.handleWrench(state,level,pos,player,hand,hit,FACING,MachineRotationType.HORIZONTAL)) return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if(!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof ImplosionCompressorControllerBlockEntity machine)
            net.minecraftforge.network.NetworkHooks.openScreen(server,machine,buffer->buffer.writeUtf("implosioncompressor"));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ImplosionCompressorControllerBlockEntity machine) machine.dropContents();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new net.minecraft.world.item.ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ImplosionCompressorControllerBlockEntity machine) {
            var data=com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(machine,machine.saveWithoutMetadata());data.remove("gt.inventory");for(int i=0;i<6;i++) data.remove("gt_cover_"+i);
            stack.getOrCreateTag().put("BlockEntityTag",data);
        }
        return java.util.List.of(stack);
    }
}
