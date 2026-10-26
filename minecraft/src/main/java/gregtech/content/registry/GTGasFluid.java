package gregtech.content.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.BaseFlowingFluid;
*///?} else {
import net.minecraftforge.fluids.ForgeFlowingFluid;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code FlowingFluid}s behind the GT6 fluids whose density is below air's, natural gas being
 * the one with a world block ({@code Loader_Blocks:150}).
 *
 * <p>Original GT6 places these fluids as {@code BlockFluidFinite} with a negative density, and
 * Forge's 1.7.10 fluid base turns that into {@code densityDir = -1}: the block spreads toward
 * {@code +Y}, rises as far as it can and is trashed at the vertical world limit
 * ({@code BlockBaseFluid:135-345}). Modern {@code FlowingFluid} has no such direction switch - it
 * always looks at the block below - so a GT6 gas registered with the ordinary
 * {@code ForgeFlowingFluid}/{@code BaseFlowingFluid} runs downhill like water, which is what the
 * port did for natural gas.
 *
 * <p>This class replaces vanilla's water flow with the original finite-quanta behaviour:
 *
 * <ul>
 *   <li>The fluid state's amount is the quantum count (1-8). {@code amount == 8} is stored as a
 *       source state because vanilla has no block state for a full non-falling flowing fluid; all
 *       other amounts are ordinary flowing states. Nothing ever sets {@code FALLING}, so gas has
 *       no vanilla falling-water column.</li>
 *   <li>A block rises as a whole. A full block leaves one quantum behind and re-schedules the
 *       moved block for the next tick, the original's compressed jet
 *       ({@code BlockBaseFluid:269-303}); smaller amounts rise on the ordinary five-tick cadence,
 *       matching {@code BlockBaseFluid}'s vertical move.</li>
 *   <li>When the cell above is blocked, the quanta equalise across the open horizontal neighbours
 *       instead of following vanilla's fixed one-level-per-block drop-off, so gas forms a thin
 *       spreading cloud. A single quantum drifts sideways into one random open cell.</li>
 *   <li>A block at the vertical world limit is removed, matching the original's trash behaviour.</li>
 * </ul>
 *
 * <p>Only fluids whose GT6 entry density is strictly negative are registered with this class (see
 * {@code FluidBootstrap} and {@code FluidRegistries}). The three air fluids are density 0 and keep
 * the ordinary flow, as does a heavier gas such as Dragon Breath.
 */
public final class GTGasFluid {
    private GTGasFluid() {}

    /** Quanta in one full fluid block; {@code FluidState} amounts use this same 1..8 range. */
    public static final int MAX_QUANTA = 8;

    /**
     * Quanta in an uncompressed block. Original quanta run 1-16 and {@code BlockBaseFluid} treats
     * more than 8 as compressed; the port's 1-8 range maps that threshold to 4, so gas rushes up
     * for four jumps and then settles into the ordinary five-tick rise.
     */
    private static final int NORMAL_QUANTA = MAX_QUANTA / 2;

    /**
     * The fluid state for a quantum count. Full blocks use the source state because vanilla's
     * {@code LiquidBlock} state cache has no non-falling flowing state for amount 8.
     */
    public static FluidState quanta(FlowingFluid self, int amount) {
        if (amount >= MAX_QUANTA) return self.getSource(false);
        return self.getFlowing(Math.max(1, amount), false);
    }

    /** Writes a quantum count; zero removes the gas block. */
    public static void setQuanta(Level level, BlockPos pos, FlowingFluid self, int amount) {
        if (amount <= 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, quanta(self, amount).createLegacyBlock(), 3);
        }
    }

//? if >=1.21.1 {
    /*public static class Source extends BaseFlowingFluid.Source implements CanSpread {
        public Source(BaseFlowingFluid.Properties properties) {
*///?} else {
    public static class Source extends ForgeFlowingFluid.Source implements CanSpread {
        public Source(ForgeFlowingFluid.Properties properties) {
//?}
            super(properties);
        }

        /** Gas has its own quanta simulation; vanilla's level recalculation would decay it. */
        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            spread(level, pos, state);
        }

        @Override
        protected void spread(Level level, BlockPos pos, FluidState state) {
            if (state.isEmpty()) return;
            BlockPos abovePos = pos.above();
            if (level.isOutsideBuildHeight(abovePos)) {
                // BlockBaseFluid:150-154 trashes gas at the vertical world limit.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            BlockState here = level.getBlockState(pos);
            BlockState aboveState = level.getBlockState(abovePos);
            FluidState above = level.getFluidState(abovePos);
            int amount = state.getAmount();

            if (amount > NORMAL_QUANTA) {
                // BlockBaseFluid:269-303: a compressed block leaves one quantum and jets the rest
                // on the next tick, so gas rises as a plume instead of a falling-water column.
                if (canSpreadTo(level, pos, here, Direction.UP, abovePos, aboveState, above, getFlowing())) {
                    setQuanta(level, abovePos, this, amount - 1);
                    setQuanta(level, pos, this, 1);
                    level.scheduleTick(abovePos, level.getFluidState(abovePos).getType(), 1);
                } else {
                    spreadSides(level, pos, here, amount);
                }
                return;
            }

            if (above.getType().isSame(this)) {
                int total = amount + above.getAmount();
                if (total > NORMAL_QUANTA) {
                    setQuanta(level, abovePos, this, NORMAL_QUANTA);
                    setQuanta(level, pos, this, total - NORMAL_QUANTA);
                    spreadSides(level, pos, level.getBlockState(pos), total - NORMAL_QUANTA);
                } else {
                    setQuanta(level, abovePos, this, total);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                return;
            }

            if (canSpreadTo(level, pos, here, Direction.UP, abovePos, aboveState, above, getFlowing())) {
                setQuanta(level, abovePos, this, amount);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }

            spreadSides(level, pos, here, amount);
        }

        private void spreadSides(Level level, BlockPos pos, BlockState here, int amount) {
            spreadQuanta(level, pos, here, amount, this);
        }

        @Override
        public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return Vec3.ZERO;
        }

        @Override
        public boolean gasCanSpreadTo(BlockGetter level, BlockPos pos, BlockState state, Direction direction,
                                      BlockPos targetPos, BlockState targetState, FluidState targetFluid,
                                      Fluid fluid) {
            return canSpreadTo(level, pos, state, direction, targetPos, targetState, targetFluid, fluid);
        }
    }

//? if >=1.21.1 {
    /*public static class Flowing extends BaseFlowingFluid.Flowing implements CanSpread {
        public Flowing(BaseFlowingFluid.Properties properties) {
*///?} else {
    public static class Flowing extends ForgeFlowingFluid.Flowing implements CanSpread {
        public Flowing(ForgeFlowingFluid.Properties properties) {
//?}
            super(properties);
        }

        /** Gas has its own quanta simulation; vanilla's level recalculation would decay it. */
        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            spread(level, pos, state);
        }

        @Override
        protected void spread(Level level, BlockPos pos, FluidState state) {
            if (state.isEmpty()) return;
            BlockPos abovePos = pos.above();
            if (level.isOutsideBuildHeight(abovePos)) {
                // BlockBaseFluid:150-154 trashes gas at the vertical world limit.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            BlockState here = level.getBlockState(pos);
            BlockState aboveState = level.getBlockState(abovePos);
            FluidState above = level.getFluidState(abovePos);
            int amount = state.getAmount();

            if (above.getType().isSame(this)) {
                int total = amount + above.getAmount();
                if (total > MAX_QUANTA) {
                    setQuanta(level, abovePos, this, MAX_QUANTA);
                    setQuanta(level, pos, this, total - MAX_QUANTA);
                    spreadSides(level, pos, level.getBlockState(pos), total - MAX_QUANTA);
                } else {
                    setQuanta(level, abovePos, this, total);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                return;
            }

            if (canSpreadTo(level, pos, here, Direction.UP, abovePos, aboveState, above, getFlowing())) {
                if (amount >= MAX_QUANTA) {
                    setQuanta(level, abovePos, this, MAX_QUANTA);
                    setQuanta(level, pos, this, 1);
                    level.scheduleTick(abovePos, level.getFluidState(abovePos).getType(), 1);
                } else {
                    setQuanta(level, abovePos, this, amount);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                return;
            }

            spreadSides(level, pos, here, amount);
        }

        private void spreadSides(Level level, BlockPos pos, BlockState here, int amount) {
            spreadQuanta(level, pos, here, amount, this);
        }

        @Override
        public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return Vec3.ZERO;
        }

        @Override
        public boolean gasCanSpreadTo(BlockGetter level, BlockPos pos, BlockState state, Direction direction,
                                      BlockPos targetPos, BlockState targetState, FluidState targetFluid,
                                      Fluid fluid) {
            return canSpreadTo(level, pos, state, direction, targetPos, targetState, targetFluid, fluid);
        }
    }

    /**
     * Horizontal half of {@code BlockBaseFluid.updateTick}: a lone quantum drifts into one random
     * open neighbour, larger amounts equalise with the lower same-gas neighbours and the open
     * neighbours instead of dropping one level per block.
     */
    private static void spreadQuanta(Level level, BlockPos pos, BlockState here, int amount, FlowingFluid self) {
        if (amount <= 0) return;
        Direction[] directions = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);

        if (amount == 1) {
            // BlockBaseFluid:172-183 only moves a lone quantum sideways when the cell above it is
            // not liquid and the cell above the target is free, i.e. when the side move actually
            // opens a way upwards. Under a flat ceiling the quantum therefore stays put instead
            // of hopping between neighbours every tick.
            if (!level.getFluidState(pos.above()).isEmpty()) return;
            for (int i = directions.length - 1; i > 0; i--) {
                int j = level.random.nextInt(i + 1);
                Direction swap = directions[i];
                directions[i] = directions[j];
                directions[j] = swap;
            }
            for (Direction direction : directions) {
                BlockPos target = pos.relative(direction);
                BlockState targetState = level.getBlockState(target);
                FluidState targetFluid = level.getFluidState(target);
                if (targetFluid.isEmpty()
                        && !level.getBlockState(target.above()).blocksMotion()
                        && canSpread(level, pos, here, direction, target, targetState, targetFluid, self)) {
                    setQuanta(level, target, self, 1);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    return;
                }
            }
            return;
        }

        int total = amount;
        int count = 1;
        List<Direction> targets = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos target = pos.relative(direction);
            BlockState targetState = level.getBlockState(target);
            FluidState targetFluid = level.getFluidState(target);
            if (targetFluid.getType().isSame(self)) {
                int neighbourAmount = targetFluid.getAmount();
                if (neighbourAmount < amount) {
                    total += neighbourAmount;
                    count++;
                    targets.add(direction);
                }
            } else if (canSpread(level, pos, here, direction, target, targetState, targetFluid, self)) {
                count++;
                targets.add(direction);
            }
        }

        if (count == 1) return;
        int base = total / count;
        int remainder = total % count;
        int selfAmount = base + (remainder-- > 0 ? 1 : 0);
        if (selfAmount != amount) setQuanta(level, pos, self, selfAmount);
        for (Direction direction : targets) {
            BlockPos target = pos.relative(direction);
            int targetAmount = base + (remainder-- > 0 ? 1 : 0);
            FluidState existing = level.getFluidState(target);
            int existingAmount = existing.getType().isSame(self) ? existing.getAmount() : 0;
            if (targetAmount == existingAmount) continue;
            if (targetAmount > 0) {
                setQuanta(level, target, self, targetAmount);
            } else if (!existing.isEmpty()) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * {@code FlowingFluid.canSpreadTo} is protected, so the shared quanta code needs this small
     * public bridge. The target state is asked with the flowing fluid, exactly like vanilla.
     */
    private static boolean canSpread(Level level, BlockPos pos, BlockState here, Direction direction,
                                     BlockPos target, BlockState targetState, FluidState targetFluid,
                                     FlowingFluid self) {
        if (!(self instanceof CanSpread bridge)) return false;
        return bridge.gasCanSpreadTo(level, pos, here, direction, target, targetState, targetFluid, self.getFlowing());
    }

    /** Bridge implemented by both nested classes; widened from the protected parent method. */
    public interface CanSpread {
        boolean gasCanSpreadTo(BlockGetter level, BlockPos pos, BlockState state, Direction direction,
                               BlockPos targetPos, BlockState targetState, FluidState targetFluid,
                               Fluid fluid);
    }
}
