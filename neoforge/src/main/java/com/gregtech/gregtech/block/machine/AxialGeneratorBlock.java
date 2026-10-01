package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.blockentity.machine.AxialGeneratorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

/** Shared placement, wrench, stop switch and stored-content drops for axial generators. */
public abstract class AxialGeneratorBlock extends DirectionalBlock implements EntityBlock {
    private final AxialGeneratorDefinitions.Grade grade;
    protected AxialGeneratorBlock(AxialGeneratorDefinitions.Grade grade,Properties properties) {
        super(properties);this.grade=grade;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    public AxialGeneratorDefinitions.Grade grade() { return grade; }
    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite()); }
    private InteractionResult interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(MachineRotationType.handleWrench(state,level,pos,player,hand,hit,FACING,MachineRotationType.ALL))return InteractionResult.sidedSuccess(level.isClientSide);
        if(level.getBlockEntity(pos) instanceof AxialGeneratorBlockEntity machine) {
            if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty()) {
                if(!level.isClientSide)machine.toggleStopped();return InteractionResult.sidedSuccess(level.isClientSide);
            }
            var held=player.getItemInHand(hand);
            if(com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER)) {
                if(!level.isClientSide) {
                    machine.toggleStopped();
                    com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(held,10000,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(machine instanceof com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity turbine && com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,com.gregtech.gregtech.api.tool.GTToolType.PLUNGER)) {
                if(!level.isClientSide)com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(held,turbine.purgeFluid(),player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(machine instanceof net.neoforged.neoforge.fluids.capability.IFluidHandler fluids) {
                if(level.isClientSide)return InteractionResult.SUCCESS;
                if(net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,fluids))return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var stack=new ItemStack(this);
        if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AxialGeneratorBlockEntity machine)stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(machine.saveWithId(machine.getLevel().registryAccess())));
        return List.of(stack);
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable(grade.steam()?"gt.tooltip.axial.steam":"gt.tooltip.axial.dynamo",grade.input(),grade.output()));
        lines.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.axial.control"));
    }
}
