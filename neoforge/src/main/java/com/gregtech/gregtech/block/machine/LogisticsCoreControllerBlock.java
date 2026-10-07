package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 17997: the outward-facing main block in a 5x5x5 Logistics Core. */
public final class LogisticsCoreControllerBlock extends DirectionalBlock implements EntityBlock, ToolInteractionTarget {
    @Override public com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public LogisticsCoreControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof LogisticsCoreControllerBlockEntity core) {
            var coverResult = com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction.use(
                    core, level, player, hand, hit.getDirection());
            if (coverResult != InteractionResult.PASS) return coverResult;
            if(player.getItemInHand(hand).isEmpty()){
                if(!level.isClientSide){
                    var cpu=core.processorCounts();
                    if(cpu==null)player.displayClientMessage(Component.literal("Structure Incomplete!"),false);
                    else player.displayClientMessage(Component.literal("Logic: "+cpu.logic()+", Control: "+cpu.control()+", Storage: "+cpu.storage()+", Conversion: "+cpu.conversion()+"; "+cpu.fixedEnergyPerTick()+" EU/t"),false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ToolInteractions.use(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof LogisticsCoreControllerBlockEntity core) core.isStructureOk();
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsCoreControllerBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.LOGISTICS_CORE.get()) return null;
        return (l, p, s, be) -> LogisticsCoreControllerBlockEntity.serverTick(
                l, p, s, (LogisticsCoreControllerBlockEntity) be);
    }

    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                          List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 10; i++)
            tooltip.add(Component.translatable("gt.tooltip.multiblock.logisticscore." + i));
        tooltip.add(Component.translatable("gt.tooltip.multiblock.logisticscore.energy"));
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        return List.of(new ItemStack(this));
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return com.gregtech.gregtech.content.logistics.LogisticsCoverSignals.at(level, pos, side.getOpposite());
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof LogisticsCoreControllerBlockEntity core)
            core.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
}
