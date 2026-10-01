package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.fluid.VanillaWaterlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * The {@code FlowingFluid}s behind GT6's three world waters (sea, river and swamp water,
 * {@code Loader_Worldgen:576-578}). They stay separate fluids - separate registry entries, separate
 * world blocks, separate {@code #minecraft:water} members and separate GT6 items - but they present
 * themselves as vanilla water everywhere the engine identifies a fluid by its instance rather than
 * by {@code FluidTags.WATER}, which is what "the GT waters must be indistinguishable from vanilla
 * water in play" needs.
 *
 * <p>Four vanilla/Forge mechanisms are matched here:
 *
 * <ol>
 *   <li><b>The {@code FluidType} identity.</b> Forge's entity code does not test the water tag for
 *       the entity state that produces the splash sound, the swimming animation, the underwater
 *       overlay, the FOV change, the bubble HUD and drowning; it tests the {@code FluidType}
 *       instance. {@code Entity.updateFluidHeightAndDoFluidPushing(TagKey, double)} ignores its tag
 *       argument for water and returns {@code isInFluidType(ForgeMod.WATER_TYPE.get())}
 *       ({@code Entity.java:3035-3040}), the per-type height map it fills is keyed by
 *       {@code fluidstate.getFluidType()} ({@code Entity.java:3078,3127}), {@code isInFluidType}
 *       compares the type instance ({@code IForgeEntity.java:267-270}), and
 *       {@code Entity.isEyeInFluid(FluidTags.WATER)} is {@code isEyeInFluidType(ForgeMod.WATER_TYPE)}
 *       with {@code isEyeInFluidType} being {@code type == this.getEyeInFluidType()}
 *       ({@code Entity.java:1320-1325}, {@code IForgeEntity.java:316-319}). Because
 *       {@code Entity.updateInWaterStateAndDoWaterCurrentPushing} drives {@code wasTouchingWater}
 *       and {@code doWaterSplashEffect} from that answer ({@code Entity.java:1208-1233}), a GT6
 *       water that reported its own {@code FluidType} produced no splash sound and no swimming at
 *       all. {@code FluidState.getFluidType()} delegates to the fluid
 *       ({@code IForgeFluidState.java:49-52}), so reporting vanilla water's type from
 *       {@code ForgeFlowingFluid.getFluidType} ({@code ForgeFlowingFluid.java:60-64}) makes every
 *       one of those checks answer exactly as it does for {@code minecraft:water}.
 *       <p>Consequences that are intended: lava flowing next to a GT6 water turns it into obsidian
 *       or cobblestone, because Forge's own interaction is registered on
 *       {@code ForgeMod.LAVA_TYPE} and tests {@code fluidstate.getFluidType() == ForgeMod.WATER_TYPE}
 *       ({@code FluidInteractionRegistry.java:79-92,133-136}), and the GT6 world water block keeps
 *       scheduling its fluid tick because {@code FluidInteractionRegistry.canInteract} then finds no
 *       interaction for a water source ({@code FluidInteractionRegistry.java:59-77}).</li>
 *   <li><b>Source conversion.</b> {@code WaterFluid.canConvertToSource(Level)} honours
 *       {@code RULE_WATER_SOURCE_CONVERSION} ({@code WaterFluid.java:57-59}); plain
 *       {@code ForgeFlowingFluid.canConvertToSource} answers from the {@code FluidType} instead
 *       ({@code ForgeFlowingFluid.java:84-88}), so it is repeated here.</li>
 *   <li><b>Replacement.</b> {@code WaterFluid.canBeReplacedWith} only lets a fluid in downwards and
 *       never lets anything in {@code #minecraft:water} in ({@code WaterFluid.java:86-88}); vanilla
 *       {@code ForgeFlowingFluid.canBeReplacedWith} only refuses the same fluid
 *       ({@code ForgeFlowingFluid.java:115-120}).</li>
 *   <li><b>Waterlogged blocks.</b> Vanilla's waterlogged-block contract identity-checks
 *       {@code Fluids.WATER}; see {@link VanillaWaterlogging} for the mechanism, for why the hook has
 *       to be {@code getNewLiquid} rather than {@code canSpreadTo}/{@code spreadTo}, and for what the
 *       {@code getNewLiquid} override at the bottom of each class does.</li>
 * </ol>
 *
 * <p>The GT6 {@code FluidType} the port registers for these fluids stays registered (it is what
 * {@code GTFluids.FLUID_TYPES} holds for {@code gregtech:seawater} and friends, and it is what
 * {@code GTFluids.entryForTypeId} and the port's own item/tank/GUI metadata lookups resolve), so the
 * GT6 entry of a world water - its name, tooltip, flags and phase - is unchanged; it is simply not
 * what the engine's water identity checks consult any more. The port's own visual copy of vanilla
 * water's client extension lives on that type ({@code WaterFluidClientExtensions}, same sprites,
 * tint and camera overlay as {@code ForgeMod.WATER_TYPE}'s), and the world renderer now asks
 * vanilla's own extension because the fluid reports vanilla's type.
 */
public final class GTWorldWaterFluid {
    private GTWorldWaterFluid() {}

    /** {@code ForgeMod.WATER_TYPE}: the {@code FluidType} vanilla water reports. */
    public static FluidType waterType() {
        return ForgeMod.WATER_TYPE.get();
    }

    /** {@code WaterFluid.canConvertToSource(Level)}: the vanilla water-source game rule. */
    public static boolean waterSourceConversion(Level level) {
        return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
    }

    /** {@code WaterFluid.canBeReplacedWith}: only downwards, and never by another water-tagged fluid. */
    public static boolean waterReplacement(Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isWaterFamily(fluid);
    }

    /**
     * The water family test, ported from TFC's own river water
     * ({@code RiverWaterFluid.isSame}: {@code super.isSame(fluid) || fluid == TFCFluids.RIVER_WATER.get()}).
     *
     * <p>Vanilla drives surface merging and the slope/level maths of a flowing body through
     * {@code Fluid#isSame} - most visibly {@code FlowingFluid.getHeight}, which returns a full block height
     * only when the fluid above is <em>the same</em> fluid, and {@code FlowingFluid.getSlopeDistance},
     * which only looks at same-fluid neighbours. The default implementation is an identity test, so before
     * this override every GT6 world water reported "a different fluid" both against the neighbouring world
     * waters and against vanilla water, and the port rendered/flowed with a seam at every such border.
     *
     * <p>All six world-water ids are in {@code #minecraft:water} (see {@code GTWaterParity} and
     * {@code data/minecraft/tags/fluids/water.json}). Our own six fluids and vanilla water are also
     * recognised directly, so a chunk rebuilt before the client receives a fluid-tag sync still
     * merges them. The tag keeps compatibility with other mods' tagged waters.
     */
    public static boolean isWaterFamily(Fluid fluid) {
        // Rendering invokes this through the vanilla WaterFluid Mixin while chunks are built.
        // The six fluids we own must still merge with vanilla water if a client has not received
        // (or a datapack has replaced) the water tag yet; keep the tag for other mods' waters.
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER
                || fluid instanceof Source || fluid instanceof Flowing) return true;
        return fluid.is(FluidTags.WATER);
    }

    /**
     * {@code WaterFluid.Source} for GT6's world waters. It has to override {@code getNewLiquid}
     * ({@code FlowingFluid.java:153-187}) rather than the {@code canSpreadTo}/{@code spreadTo} pair,
     * because {@code getSpread} decides with {@code FlowingFluid}'s private helpers which fluid the
     * target is asked with; see {@link VanillaWaterlogging}.
     */
    public static class Source extends ForgeFlowingFluid.Source {
        public Source(ForgeFlowingFluid.Properties properties) {
            super(properties);
        }

        /** See the class javadoc: vanilla water's {@code FluidType}, not the GT6 one. */
        @Override
        public FluidType getFluidType() {
            return waterType();
        }

        /** TFC's family test: this water, the other world waters and vanilla water are all "water". */
        @Override
        public boolean isSame(Fluid fluid) {
            return isWaterFamily(fluid);
        }

        @Override
        public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
            return waterSourceConversion(level);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                            Fluid fluid, Direction direction) {
            return waterReplacement(fluid, direction);
        }

        /** See {@link VanillaWaterlogging}: the one hook that reaches every spread decision. */
        @Override
        protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState state) {
            return VanillaWaterlogging.asVanillaWaterState(state, super.getNewLiquid(level, pos, state));
        }
    }

    /** {@code WaterFluid.Flowing}: the same four water overrides as {@link Source}. */
    public static class Flowing extends ForgeFlowingFluid.Flowing {
        public Flowing(ForgeFlowingFluid.Properties properties) {
            super(properties);
        }

        /** See the class javadoc: vanilla water's {@code FluidType}, not the GT6 one. */
        @Override
        public FluidType getFluidType() {
            return waterType();
        }

        /** TFC's family test: this water, the other world waters and vanilla water are all "water". */
        @Override
        public boolean isSame(Fluid fluid) {
            return isWaterFamily(fluid);
        }

        @Override
        public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
            return waterSourceConversion(level);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                            Fluid fluid, Direction direction) {
            return waterReplacement(fluid, direction);
        }

        /** See {@link VanillaWaterlogging}: the one hook that reaches every spread decision. */
        @Override
        protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState state) {
            return VanillaWaterlogging.asVanillaWaterState(state, super.getNewLiquid(level, pos, state));
        }
    }
}
