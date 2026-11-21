package gregtech.content.block.machine;

import gregtech.content.block.GTDirectionalBlock;
import gregtech.content.blockentity.machine.LargeHeatExchangerControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

public class LargeHeatExchangerControllerBlock extends GTDirectionalBlock implements gregtech.engine.multiblock.StructurePreviewSource, gregtech.engine.tool.ToolInteractionTarget {

    public LargeHeatExchangerControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected net.minecraft.world.InteractionResult useBlock(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if (level.getBlockEntity(pos) instanceof LargeHeatExchangerControllerBlockEntity machine) {
            if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
//? if >=1.21.1 {
            /*if (net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand,
                    machine)) return net.minecraft.world.InteractionResult.CONSUME;
*///?} else {
            if (net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand,
                    machine)) return net.minecraft.world.InteractionResult.CONSUME;
//?}
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placementContext) {
        return defaultBlockState();
    }

    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, net.minecraft.world.item.ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING, MachineRotationType.BOTTOM) : null;
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, Direction.DOWN);
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeHeatExchangerControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != gregtech.content.registry.GTBlockEntities.LARGE_HEAT_EXCHANGER.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> LargeHeatExchangerControllerBlockEntity.serverTick(tickLevel, tickPos, tickState, (LargeHeatExchangerControllerBlockEntity) blockEntity);
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack = new net.minecraft.world.item.ItemStack(this);
        if (builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof LargeHeatExchangerControllerBlockEntity machine) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(machine,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(machine, builder.getLevel().registryAccess()));
            data.remove("gt.energy");
            data.remove("gt.active");
            data.remove("gt.active.data");
            data.remove("gt.state.str");
            data.remove("gt.hot");
            data.remove("gt.cold");
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return java.util.List.of(stack);
    }

    @Override
    protected void appendTooltip(net.minecraft.world.item.ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        gregtech.client.UtilityControllerTooltips.heat(stack, tooltip);
    }
}
