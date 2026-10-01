package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

public class LargeGasTurbineControllerBlock extends DirectionalBlock implements EntityBlock {
    private final com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.Grade grade;
    public com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.Grade grade() { return grade; }
    public LargeGasTurbineControllerBlock(Properties properties) {
        this(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.GRADES.get(0), properties);
    }
    public LargeGasTurbineControllerBlock(com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.Grade grade, Properties properties) {
        super(properties);
        this.grade = grade;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LargeGasTurbineControllerBlockEntity(pos, state); }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.LARGE_GAS_TURBINE.get()) return null;
        return (l, p, s, be) -> LargeGasTurbineControllerBlockEntity.serverTick(l, p, s, (LargeGasTurbineControllerBlockEntity) be);
    }

    private net.minecraft.world.InteractionResult interact(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(MachineRotationType.handleWrench(state,level,pos,player,hand,hit,FACING,MachineRotationType.ALL))return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if(level.getBlockEntity(pos) instanceof LargeGasTurbineControllerBlockEntity machine){
            if(level.isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
            if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty()){machine.toggleStopped();return net.minecraft.world.InteractionResult.CONSUME;}
            if(net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,machine))return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var stack=new net.minecraft.world.item.ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof LargeGasTurbineControllerBlockEntity machine)stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(machine.saveWithId(machine.getLevel().registryAccess())));
        return java.util.List.of(stack);
    }
}
