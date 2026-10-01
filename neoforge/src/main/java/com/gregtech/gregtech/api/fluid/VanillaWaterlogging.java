package com.gregtech.gregtech.api.fluid;

import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Makes GT6's three world waters fill vanilla's waterlogged blocks exactly where vanilla water
 * would, and nowhere else.
 *
 * <p><b>The vanilla mechanism.</b> Vanilla's waterlogged-block contract is an <em>instance</em>
 * comparison, not a tag test: {@code SimpleWaterloggedBlock.canPlaceLiquid} is
 * {@code fluid == Fluids.WATER} ({@code SimpleWaterloggedBlock.java:17-19}) and {@code placeLiquid}
 * needs {@code fluidState.getType() == Fluids.WATER} ({@code SimpleWaterloggedBlock.java:21-22}) -
 * the <em>flowing</em> fluid is rejected just like any other fluid.
 *
 * <p>{@code FlowingFluid} computes the fluid it is about to place in exactly one method,
 * {@code getNewLiquid} ({@code FlowingFluid.java:153-187}): it returns {@code this.getSource(false)}
 * when the target position has two source neighbours and a solid block below it
 * ({@code FlowingFluid.java:170-176}), and otherwise the flowing fluid
 * ({@code getFlowing(8, true)} for a column falling in from above, {@code FlowingFluid.java:178-186},
 * or {@code getFlowing(amount - dropOff, false)} at line 185). That state is then what every later
 * decision compares:
 * <ul>
 *   <li>{@code getSpread} computes it per direction ({@code FlowingFluid.java:349}) and drops the
 *       direction unless {@code canPassThrough(..., fluidstate1.getType(), ...)}
 *       ({@code FlowingFluid.java:350}) succeeds - and that runs {@code FlowingFluid}'s own
 *       <b>private</b> {@code canPassThrough} (310-312) and <b>private</b> {@code canHoldFluid}
 *       (377-390), i.e. {@code LiquidBlockContainer.canPlaceLiquid(level, pos, state, thatFluid)}.</li>
 *   <li>{@code spreadToSides} then asks {@code canSpreadTo} with {@code fluidstate.getType()} of the
 *       surviving entries ({@code FlowingFluid.java:140-147}, 392-394), which hits the same
 *       {@code canHoldFluid}.</li>
 *   <li>{@code spreadTo} finally calls {@code placeLiquid} with the same state
 *       ({@code FlowingFluid.java:245-256}).</li>
 * </ul>
 *
 * <p><b>Why substituting only at canSpreadTo/spreadTo was not enough.</b> The first of those two
 * gates lives inside {@code getSpread} and uses {@code FlowingFluid}'s private helpers, which a
 * subclass cannot reach. An override that substituted the fluid only at {@code canSpreadTo} never
 * ran for a waterlogged neighbour: {@code getSpread} had already dropped that direction, because the
 * private {@code canHoldFluid} asked the block with the GT6 fluid and got {@code false}. The
 * GameTest showed exactly that - vanilla water in a two-source lane left the stairs
 * {@code waterlogged=true} while GT6 sea water in the same lane left it {@code waterlogged=false}
 * (build/gametest.log).
 *
 * <p><b>The fix.</b> Substitute at {@code getNewLiquid}, the one point where the fluid about to be
 * placed is computed, and only for a {@code SimpleWaterloggedBlock} target whose computed liquid is
 * this fluid's source - i.e. in precisely the case where vanilla's own pipeline would be handing
 * {@code Fluids.WATER} to {@code canPlaceLiquid}. Everything else is handed back untouched, so:
 * <ul>
 *   <li>a GT6 water waterlogs a block exactly when vanilla water does (two sources flanking it over
 *       solid ground), because that is the only case that produces a source state;</li>
 *   <li>the flowing case is still rejected by {@code canPlaceLiquid}'s {@code == Fluids.WATER} test,
 *       exactly as vanilla's {@code Fluids.FLOWING_WATER} is;</li>
 *   <li>the substitution can only reach a {@code LiquidBlockContainer} target (every
 *       {@code SimpleWaterloggedBlock} is one), so {@code FlowingFluid.spreadTo} takes its
 *       {@code placeLiquid} branch and the substituted state can never be turned into a world block
 *       by {@code setBlock(createLegacyBlock())}; and</li>
 *   <li>every other block - air, stone, GT6's own water, seagrass, kelp or a mod's tank - sees the
 *       GT6 fluid, unchanged from before.</li>
 * </ul>
 * A waterlogged block filled this way carries vanilla water internally, which is what the block
 * stores in vanilla anyway and what several of them store unconditionally
 * ({@code SeagrassBlock.getFluidState} returns {@code Fluids.WATER.getSource(false)} with no
 * waterlogged property at all, {@code SeagrassBlock.java:62-64}; {@code KelpBlock.java:56-58} the
 * same). The block's fluid state is water-tagged, so swimming, drowning, the underwater overlay and
 * the renderer treat it exactly like vanilla water, and the GT6 fluid around it is not touched.
 */
public final class VanillaWaterlogging {
    private VanillaWaterlogging() {}

    /**
     * The fluid state the rest of vanilla's spread pipeline must see for a target block.
     *
     * <p>Returns vanilla water's source state for a {@code SimpleWaterloggedBlock} that the GT6
     * computation resolved to this fluid's source, and {@code computed} unchanged otherwise - which
     * includes every flowing-shaped result, every block that is not a waterlogged block and every
     * non-source liquid, so the port's own flow behaviour is untouched outside that one case.
     *
     * @param target   the block state at the position the fluid would be placed in, i.e. the state
     *                 {@code FlowingFluid.getNewLiquid} was called with
     * @param computed what {@code FlowingFluid.getNewLiquid} returned for that position
     */
    public static FluidState asVanillaWaterState(BlockState target, FluidState computed) {
        if (!(target.getBlock() instanceof SimpleWaterloggedBlock)) return computed;
        return computed.isSource() ? Fluids.WATER.getSource(false) : computed;
    }
}
