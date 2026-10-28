package gregtech.content.block.machine;

import gregtech.content.multiblock.AxialGeneratorDefinitions;
import gregtech.content.blockentity.machine.AxialGeneratorBlockEntity;
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
public abstract class AxialGeneratorBlock extends DirectionalBlock implements gregtech.engine.multiblock.StructurePreviewSource, gregtech.engine.tool.ToolInteractionTarget {
    @Override public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING,MachineRotationType.ALL) : null;
    }
    private final AxialGeneratorDefinitions.Grade grade;
    protected AxialGeneratorBlock(AxialGeneratorDefinitions.Grade grade,Properties properties) {
        super(properties);this.grade=grade;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    public AxialGeneratorDefinitions.Grade grade() { return grade; }
//? if >=1.21.1 {
    /*@Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite()); }
//? if >=1.21.1 {
    /*private InteractionResult interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
*///?} else {
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
//?}
        if(MachineRotationType.handleWrench(state,level,pos,player,hand,hit,FACING,MachineRotationType.ALL))return InteractionResult.sidedSuccess(level.isClientSide);
        if(level.getBlockEntity(pos) instanceof AxialGeneratorBlockEntity machine) {
            if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty()) {
                if(!level.isClientSide)machine.toggleStopped();return InteractionResult.sidedSuccess(level.isClientSide);
            }
            var held=player.getItemInHand(hand);
            if(gregtech.engine.tool.GTToolHelper.matchesTool(held,gregtech.engine.tool.GTToolType.SOFT_HAMMER)) {
                if(!level.isClientSide) {
                    machine.toggleStopped();
                    gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(held,10000,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(machine instanceof gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity turbine && gregtech.engine.tool.GTToolHelper.matchesTool(held,gregtech.engine.tool.GTToolType.PLUNGER)) {
                if(!level.isClientSide)gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(held,turbine.purgeFluid(),player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
//? if >=1.21.1 {
            /*if(machine instanceof net.neoforged.neoforge.fluids.capability.IFluidHandler fluids) {
*///?} else {
            if(machine instanceof net.minecraftforge.fluids.capability.IFluidHandler fluids) {
//?}
                if(level.isClientSide)return InteractionResult.SUCCESS;
//? if >=1.21.1 {
                /*if(net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,fluids))return InteractionResult.CONSUME;
*///?} else {
                if(net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,fluids))return InteractionResult.CONSUME;
//?}
            }
        }
        return InteractionResult.PASS;
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var stack=new ItemStack(this);
//? if >=1.21.1 {
        /*if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AxialGeneratorBlockEntity machine)stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(gregtech.content.cover.ComponentCoverFallback.forItem(machine,machine.saveWithId(machine.getLevel().registryAccess()))));
*///?} else {
        if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AxialGeneratorBlockEntity machine)stack.getOrCreateTag().put("BlockEntityTag",gregtech.content.cover.ComponentCoverFallback.forItem(machine,machine.saveWithoutMetadata()));
//?}
        return List.of(stack);
    }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag) {
*///?} else {
    @Override public void appendHoverText(ItemStack stack,BlockGetter level,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag) {
//?}
        gregtech.client.GeneratorSourceTooltips.append(gregtech.content.multiblock.OriginalGeneratorTooltipData.find(grade.id()),lines);
    }
}
