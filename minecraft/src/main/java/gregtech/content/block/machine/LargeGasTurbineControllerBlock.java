package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity;
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

public class LargeGasTurbineControllerBlock extends DirectionalBlock
        implements gregtech.engine.multiblock.StructurePreviewSource, gregtech.engine.tool.ToolInteractionTarget {
    private final gregtech.content.multiblock.GasTurbineDefinitions.Grade grade;
    public gregtech.content.multiblock.GasTurbineDefinitions.Grade grade() { return grade; }

    /** GT6 {@code TileEntityBase10MultiBlockBase extends TileEntityBase09FacingSingle}. */
    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, net.minecraft.world.item.ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING,
                        MachineRotationType.ALL)
                : null;
    }
    public LargeGasTurbineControllerBlock(Properties properties) {
        this(gregtech.content.multiblock.GasTurbineDefinitions.GRADES.get(0), properties);
    }
    public LargeGasTurbineControllerBlock(gregtech.content.multiblock.GasTurbineDefinitions.Grade grade, Properties properties) {
        super(properties);
        this.grade = grade;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

//? if >=1.21.1 {
    /*@Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held, BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        return gregtech.engine.tool.ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return use(state, level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if(MachineRotationType.handleWrench(state,level,pos,player,hand,hit,FACING,MachineRotationType.ALL))return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if(level.getBlockEntity(pos) instanceof LargeGasTurbineControllerBlockEntity machine){
            if(level.isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
            if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty()){machine.toggleStopped();return net.minecraft.world.InteractionResult.CONSUME;}
//? if >=1.21.1 {
            /*if(net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,machine))return net.minecraft.world.InteractionResult.CONSUME;
*///?} else {
            if(net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,machine))return net.minecraft.world.InteractionResult.CONSUME;
//?}
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LargeGasTurbineControllerBlockEntity(pos, state); }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != gregtech.content.registry.GTBlockEntities.LARGE_GAS_TURBINE.get()) return null;
        return (l, p, s, be) -> LargeGasTurbineControllerBlockEntity.serverTick(l, p, s, (LargeGasTurbineControllerBlockEntity) be);
    }

//? if >=1.21.1 {
    /*@Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
*///?} else {
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.BlockGetter level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
//?}
        gregtech.client.GeneratorSourceTooltips.append(gregtech.content.multiblock.OriginalGeneratorTooltipData.find(grade.id()),lines);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var stack=new net.minecraft.world.item.ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof LargeGasTurbineControllerBlockEntity machine) {
//? if >=1.21.1 {
            /*var data = gregtech.content.cover.ComponentCoverFallback.forItem(machine, machine.saveWithId(machine.getLevel().registryAccess()));
*///?} else {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(machine, machine.saveWithoutMetadata());
//?}
            data.remove("gt.ru");
            data.remove("gt.fuel");
            data.remove("gt.stopped");
            data.remove("gt.rotor_formed");
            data.remove("gt.rotor_active");
            data.remove("gt.tank");
            for (int i = 0; i < 3; i++) data.remove("gt.exhaust" + i);
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return java.util.List.of(stack);
    }
}
