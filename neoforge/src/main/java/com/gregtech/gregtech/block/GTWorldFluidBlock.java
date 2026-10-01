package com.gregtech.gregtech.block;

import com.gregtech.gregtech.content.hazard.WorldFluidEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The {@link LiquidBlock} the port uses for the fluids it actually puts into the world (GT6's sea,
 * river and swamp water, the oil/gas springs and geothermal water).
 *
 * <p>Forge's {@code LiquidBlock(Supplier<? extends FlowingFluid>, Properties)} constructor - the one
 * a mod has to use, because the fluid needs the block to build its {@code createLegacyBlock} - leaves
 * the deprecated {@code private final FlowingFluid fluid} field at {@code null}
 * ({@code aconst_null; putfield fluid} in Forge 1.20.1-47.4.20, confirmed by reflection) while
 * {@code onPlace}, {@code neighborChanged}, {@code updateShape}, {@code isPathfindable},
 * {@code skipRendering}, {@code pickupBlock} and {@code getPickupSound} still read it directly. Any
 * of those paths therefore throws a {@code NullPointerException} as soon as such a block is placed
 * with {@code Level.setBlock} or gets a block update next to it, which is exactly what "the world
 * block behaves like minecraft:water" must not do.
 *
 * <p>This subclass repeats those seven methods verbatim and reads the fluid through
 * {@link LiquidBlock#fluid} - the supplier Forge does keep - so the block behaves exactly like
 * vanilla {@code Blocks.WATER} again. Nothing else about {@code LiquidBlock} changes: same state
 * shape (level 0..15, source and falling), same collision, render shape, drops and flow handling.
 */
public class GTWorldFluidBlock extends LiquidBlock {
    @Override public com.mojang.serialization.MapCodec<LiquidBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public GTWorldFluidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    /** {@code LiquidBlock.isPathfindable} with the fluid read through the supplier. */
    @Override
    public boolean isPathfindable(BlockState state,PathComputationType type) {
        return !fluid.is(FluidTags.LAVA);
    }

    /** {@code LiquidBlock.skipRendering} with the fluid read through the supplier. */
    @Override
    public boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.getFluidState().getType().isSame(fluid);
    }

    /** {@code LiquidBlock.onPlace} with the fluid read through the supplier. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!FluidInteractionRegistry.canInteract(level, pos)) {
            level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
        }
    }

    /** {@code LiquidBlock.updateShape} with the fluid read through the supplier. */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (state.getFluidState().isSource() || neighbor.getFluidState().isSource()) {
            level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** {@code LiquidBlock.neighborChanged} with the fluid read through the supplier. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (!FluidInteractionRegistry.canInteract(level, pos)) {
            level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
        }
    }

    /**
     * {@code LiquidBlock.pickupBlock} with the fluid read through the supplier. GT6 fluids have no
     * vanilla bucket of their own (GT6 uses its own cells, the port uses the creative-only
     * {@code gregtech:fluid_item_*} items), so this still yields {@code Items.AIR} - the same result
     * vanilla water gives for a fluid without a bucket - instead of crashing.
     */
    @Override
    public ItemStack pickupBlock(net.minecraft.world.entity.player.Player player,LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            return new ItemStack(fluid.getBucket());
        }
        return ItemStack.EMPTY;
    }

    /** {@code LiquidBlock.getPickupSound} with the fluid read through the supplier. */
    @Override
    public Optional<SoundEvent> getPickupSound() {
        return fluid.getPickupSound();
    }

    /** {@code WebBlock.entityInside}: {@code makeStuckInBlock(state, new Vec3(0.25, 0.05, 0.25))}. */
    private static final Vec3 WEB_MOVEMENT_MULTIPLIER = new Vec3(0.25D, 0.05D, 0.25D);

    /**
     * GT6 {@code BlockBaseFluid:405} - {@code if (mActLikeWeb) aEntity.setInWeb();} - for the two heavy
     * oils, the only fluids whose registration line calls {@code .setWeb()}
     * ({@code Loader_Blocks.java:149,150}).
     *
     * <p>1.7.10's {@code Entity#setInWeb()} <em>is</em> vanilla cobweb: {@code makeStuckInBlock} with
     * the 0.25 / 0.05 / 0.25 movement multiplier. GT6's {@code getBlocksMovement}
     * ({@code BlockBaseFluid:374}) is the same fact seen from the path finder's side; it has no 1.20.1
     * equivalent for a fluid block (whether a block blocks movement comes from
     * {@code BlockBehaviour.Properties}, which a {@code LiquidBlock} cannot change without replacing
     * vanilla fluid physics), so only the web half is ported and the other is recorded in §108.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && WorldFluidEffects.actsLikeWeb(fluid)) {
            entity.makeStuckInBlock(state, WEB_MOVEMENT_MULTIPLIER);
        }
        super.entityInside(state, level, pos, entity);
    }
}
