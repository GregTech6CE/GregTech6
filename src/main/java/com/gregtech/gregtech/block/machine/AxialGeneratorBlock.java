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
public abstract class AxialGeneratorBlock extends DirectionalBlock implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,MachineRotationType.ALL) : null;
    }
    private final AxialGeneratorDefinitions.Grade grade;
    protected AxialGeneratorBlock(AxialGeneratorDefinitions.Grade grade,Properties properties) {
        super(properties);this.grade=grade;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    public AxialGeneratorDefinitions.Grade grade() { return grade; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite()); }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
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
            if(machine instanceof net.minecraftforge.fluids.capability.IFluidHandler fluids) {
                if(level.isClientSide)return InteractionResult.SUCCESS;
                if(net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,fluids))return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var stack=new ItemStack(this);
        if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AxialGeneratorBlockEntity machine)stack.getOrCreateTag().put("BlockEntityTag",com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(machine,machine.saveWithoutMetadata()));
        return List.of(stack);
    }
    @Override public void appendHoverText(ItemStack stack,BlockGetter level,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag) {
        com.gregtech.gregtech.client.GeneratorSourceTooltips.append(com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData.find(grade.id()),lines);
    }
}
