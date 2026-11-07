package gregtech.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Static helpers for {@link net.minecraft.world.level.block.SimpleWaterloggedBlock} support.
 *  Blocks add {@link #WATERLOGGED} to their state definition and delegate to these methods. */
public final class GTWaterloggable {
    private GTWaterloggable() {}

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Return a placement state with WATERLOGGED set from the fluid at the clicked position. */
    public static BlockState getStateForPlacement(BlockState base, BlockPlaceContext ctx) {
        if (base == null) return null;
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return base.setValue(WATERLOGGED, isWaterForLogging(fluid));
    }

    /**
     * True when {@code fluid} may waterlog a block: vanilla water and any mod fluid in the
     * {@code #minecraft:water} tag.
     */
    public static boolean isWaterForLogging(FluidState fluid) {
        return fluid.is(FluidTags.WATER);
    }

    /** Return the fluid state for a waterloggable block. */
    @SuppressWarnings("deprecation")
    public static FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    /** Schedule a water tick if waterlogged; call from {@code updateShape}. */
    public static void tickWater(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    /** Merge waterlogging tick into an existing updateShape override.
     *  Usage: {@code GTWaterloggable.updateShape(state, level, pos); return super.updateShape(...);} */
    public static void updateShape(BlockState state, LevelAccessor level, BlockPos pos) {
        tickWater(state, level, pos);
    }
}
