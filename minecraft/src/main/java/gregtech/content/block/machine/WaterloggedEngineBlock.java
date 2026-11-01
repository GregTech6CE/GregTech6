package gregtech.content.block.machine;

import gregtech.engine.GTWaterloggable;
import gregtech.engine.machine.EngineType;
import gregtech.engine.machine.MachineSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;

/** Open-frame piston engines retain water around their moving shaft. */
public final class WaterloggedEngineBlock extends EngineBlock implements SimpleWaterloggedBlock {
    public WaterloggedEngineBlock(EngineType type, MachineSpec machineSpec, Object engineSpec, Properties properties) {
        super(type, machineSpec, engineSpec, properties);
        registerDefaultState(defaultBlockState().setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GTWaterloggable.getStateForPlacement(super.getStateForPlacement(context), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }
}
