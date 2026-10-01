package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * The two GUI-less covers the port owns itself: the drain cover ({@code drain}) and the air vent
 * ({@code air_vent}).
 *
 * <p>All work happens here; {@code BasicMachineBlockEntity.tickCovers} only matches the behaviour id
 * and forwards. The methods are static and take the objects they touch, exactly like GT6's
 * {@code AbstractCoverAttachment} subclasses take a {@code CoverData} instead of the tile entity —
 * so the drain only ever sees the {@link IFluidHandler} GT6's own {@code FL.fillAll} sees
 * ({@code CoverDrain:117}).</p>
 *
 * <h2>Both items really are registered</h2>
 *
 * <p>Checked before implementing, as required: {@code drain} and {@code air_vent} are entries of the
 * port's technological item list ({@code GTTechnological} covers block, lines 348-358, both ids
 * literally on line 355) with display names "Drain" and "Air Vent" ({@code GTTechnological:120} and
 * {@code :108}). {@link CoverItems#behavior} therefore returns {@code "drain"} / {@code "air_vent"}
 * for them through its {@code portCoverId} branch ({@code CoverItems:76}), and the ids below are
 * only named constants for those same registry paths.</p>
 *
 * <h2>Drain — ported from {@code CoverDrain:68-159}</h2>
 *
 * <ul>
 *   <li>the operation throttle {@code SERVER_TIME % 20 == 5} ({@code CoverDrain:109});</li>
 *   <li>the fluid block in front of the cover ({@code CoverDrain:110,124-155}): a source block of
 *       any fluid becomes a {@code 1000 mB} {@link FluidStack} ({@code FL.Water.make(1000)} /
 *       {@code FL.Lava.make(1000)}, {@code CoverDrain:129,134}), is offered to the machine, and is
 *       only removed once the machine accepted <em>all</em> of it — GT6's {@code FL.fillAll}
 *       all-or-nothing rule ({@code CoverDrain:148}), followed by
 *       {@code setBlockToAir} ({@code CoverDrain:152});</li>
 *   <li>the gravity gate ({@code CoverDrain:147}): {@code SIDES_HORIZONTAL[aCoverSide] ||
 *       FL.gas(tFluid) || (FL.lighter(tFluid) ? SIDES_BOTTOM : SIDES_TOP)[aCoverSide]} — a heavy
 *       liquid is skipped when the cover faces down, "if not against Gravity" from the tooltip
 *       ({@code CoverDrain:229});</li>
 *   <li>rain water ({@code CoverDrain:70-84}): every 100 ticks, for the up face and the four
 *       horizontal ones, when it rains, the biome is not dry and not cold
 *       ({@code rainfall > 0 && temperature >= 0.2}, {@code CoverDrain:72}), the face is open to the
 *       sky ({@code getRainOffset}, {@code CoverDrain:77-79} — the port asks
 *       {@link Level#canSeeSky}) and no fluid block sits in the way
 *       ({@code CoverDrain:74}), the machine receives
 *       {@code max(1, rainfall * 10000) * (thundering ? 2 : 1)} mB of water
 *       ({@code CoverDrain:81}, {@link #rainAmount}).</li>
 * </ul>
 *
 * <p><b>Deliberately not ported from the drain:</b></p>
 *
 * <ul>
 *   <li>the {@code BlockBaseFluid} quanta branch ({@code CoverDrain:113-122}): it decrements GT6's
 *       own block metadata one quantum at a time. The port has no quanta — its GT6 fluid blocks are
 *       {@code GTWorldFluidBlock}s, i.e. ordinary {@code LiquidBlock}s where one block is exactly
 *       one bucket — so the flat 1000 mB source rule above replaces it. Flowing levels
 *       ({@code LEVEL != 0}) are therefore left alone, which is the same gate GT6 applies to vanilla
 *       water and lava ({@code CoverDrain:125,134}).</li>
 *   <li>the infinite-water and river/lake/ocean/swamp "16000 mB" shortcuts
 *       ({@code CoverDrain:126-128,136-143}): the port drains 1000 mB per source block instead.
 *       The ocean half of that rule is not even needed here, because the port places GT6's real
 *       {@code gregtech:seawater} / {@code riverwater} / {@code swampwater} blocks in the world
 *       ({@code Loader_Fluids:39-42}), so draining an ocean already yields seawater without a
 *       special case.</li>
 *   <li>XP orbs to liquid XP / Mob Essence ({@code CoverDrain:85-108}): needs {@code FL.XP} or
 *       {@code FL.Mob} and, for the better ratio, OpenBlocks' {@code MD.OB} ({@code CoverDrain:89-98}).
 *       The port has neither fluid, so there is nothing to fill with.</li>
 *   <li>{@code onWalkOver} ({@code CoverDrain:161-224}): player XP draining, squid ink, slime, magma
 *       cube and animal sewage — each one needs a fluid the port does not have (and the squid/slime
 *       branches need GT6's entity-width based amounts).</li>
 *   <li>the tooltips ({@code CoverDrain:226-241}) are GUI text, and the drain is a GUI-less cover.</li>
 * </ul>
 *
 * <h2>Air vent — ported from {@code CoverVent:40-85}</h2>
 *
 * <p>GT6's vent is an air <em>intake</em>: once per side per 360 ticks
 * ({@code SERVER_TIME % 360 == 30 + 60 * aSide}, {@code CoverVent:45}) and only when the block in
 * front of it is collectable air ({@code WD.collectable_air}, {@code CoverVent:46}), it pours
 * {@code 256000 mB} of air into the fluid handler — {@code FL.Air} in the overworld,
 * {@code FL.Air_Nether} in the nether and {@code FL.Air_End} in the end ({@code CoverVent:47-51}),
 * with GT6's biome sets as the fallback for every other dimension ({@code CoverVent:52-62}). All of
 * that is ported here: {@link #ventPhase} is the original's {@code 30 + 60 * aSide},
 * {@link #VENT_AIR_AMOUNT} is its {@code 256000}, and {@link #ventAirField} is its dimension
 * selection.</p>
 *
 * <p><b>Partial fill, not all-or-nothing.</b> The original calls {@code FL.fill_} at all six fill
 * sites ({@code CoverVent:48,49,50,55,59,62}), and that is the lenient variant:
 * {@code FL.java:826-828} hands {@code aFluidHandler.fill(...)} straight back, so it returns the
 * amount that actually fit and every caller discards it. The strict variant is {@code FL.fillAll},
 * which demands {@code fill(..., F) == aFluid.amount} before it does anything
 * ({@code FL.java:833}) — {@code CoverVent} never uses it. A tank smaller than 256000 mB is
 * therefore filled to the brim and the remainder is simply dropped, so {@link #tickVent} reports
 * "something went in" rather than requiring all 256000.</p>
 *
 * <p><b>The 1.20.1 answer to {@code WD.collectable_air}.</b> The original
 * ({@code WD.java:399-400}) is {@code !hasCollide(...) && !liquid(...)}: no colliding block and no
 * liquid in front. It never asks whether the block literally <em>is</em> air, and the port keeps
 * exactly those two halves — no collision shape, and no liquid ({@link FluidState#isEmpty} plus a
 * {@code LiquidBlock} test, which covers vanilla water and lava as well as the port's
 * {@code GTWorldFluidBlock}s). The leading
 * {@code !MD.GC.mLoaded || !(provider instanceof IGalacticraftWorldProvider)} term has no port
 * equivalent, because there is no Galacticraft dimension here for it to reject.</p>
 *
 * <p><b>Not ported:</b> GT6's biome-set fallback for dimensions outside the three vanilla ones
 * ({@code CoverVent:52-61}: {@code BIOMES_SPACE} returns without filling anything, while
 * {@code BIOMES_END} and {@code BIOMES_NETHER} pick their variant). 1.20.1 has exactly three
 * dimensions and {@code CoverVent:47-51} already covers all three, and the port has no
 * {@code BIOMES_SPACE} / {@code BIOMES_END} / {@code BIOMES_NETHER} equivalent to port it into — the
 * port's {@code GTWorldgenBiomes} is overworld worldgen only — so {@link #ventAirField} answers from
 * the dimension alone. A modded dimension therefore receives overworld air; that is the one gap this
 * leaves open.</p>
 *
 * <h2>Which fluids count as gases</h2>
 *
 * <p>Not decided here. {@link #isGas} forwards to
 * {@code com.gregtech.gregtech.api.fluid.FluidHazards#isGas(net.minecraft.world.level.material.Fluid)},
 * which answers GT6's {@code FL.gas} ({@code FL.java:770}) from the port's own fluid metadata and
 * falls back to Forge's lighter-than-air flag for fluids GT6 never declared
 * ({@code FluidHazards:125-128}). Its single caller is the drain's gravity gate
 * ({@code CoverDrain:147}), where it supplies the {@code FL.gas(tFluid)} term: a gas drains from any
 * face, while a heavy liquid still has to sit above the cover to be drained "against Gravity"
 * ({@code CoverDrain:229}). No gas table is written here on purpose — a private copy would be a
 * second source of truth for a question {@code FluidHazards} already owns.</p>
 *
 * <h2>Throttling uses the owner's cover tick counter</h2>
 *
 * <p>GT6 throttles with its global {@code SERVER_TIME}; the port's existing covers use
 * {@code level.getGameTime()} ({@code BasicMachineBlockEntity.tickPumpCover}). Both are unusable in
 * a GameTest, where the test calls {@code serverTick} in a loop inside a single game tick: the value
 * would never change and the cover would never act. These methods therefore take the owning
 * machine's own cover tick counter, which advances exactly once per server tick. In a running game
 * it is the same cadence as {@code getGameTime()}, and it makes the guard observable in the test.</p>
 */
public final class CoverAttachmentBehaviors {

    /** Behaviour id of the drain cover, i.e. the registry path of {@code gregtech:drain}. */
    public static final String DRAIN = "drain";

    /** Behaviour id of the air vent, i.e. the registry path of {@code gregtech:air_vent}. */
    public static final String AIR_VENT = "air_vent";

    /** GT6 {@code CoverDrain:109}: {@code SERVER_TIME % 20 == 5}. */
    public static final int DRAIN_PERIOD = 20, DRAIN_PHASE = 5;

    /** GT6 {@code CoverDrain:70}: {@code SERVER_TIME % 100 == 10}. */
    public static final int RAIN_PERIOD = 100, RAIN_PHASE = 10;

    /** GT6 {@code CoverVent:45}: one 360 tick cycle, one slot of 60 ticks per side, first at 30. */
    public static final int VENT_CYCLE = 360, VENT_SLOT = 60, VENT_OFFSET = 30;

    /** GT6 {@code CoverVent:48,49,50,55,59,62}: {@code FL.Air.make(256000)} per operation. */
    public static final int VENT_AIR_AMOUNT = 256000;

    /** One fluid block is one bucket, GT6 {@code FL.Water.make(1000)} ({@code CoverDrain:129}). */
    public static final int FLUID_BLOCK_AMOUNT = 1000;

    private CoverAttachmentBehaviors() {}

    /**
     * One drain cover operation: pull the neighbouring fluid block and/or the rain into the machine.
     *
     * <p>The two halves run on GT6's own two clocks — the fluid block on {@code % 20 == 5}, the rain
     * on {@code % 100 == 10} ({@code CoverDrain:109,70}) — so a single entry point covers both and
     * the caller only has to hand over its tick counter.</p>
     *
     * @param level       the server level the machine stands in
     * @param machinePos  the machine's own position; the cover's face is {@code machinePos.relative(side)}
     * @param side        the absolute face the cover is attached to
     * @param sink        the machine as GT6's {@code FL.fillAll} sees it (only its input tanks accept)
     * @param tickCounter the owner's cover tick counter, advancing once per server tick
     * @return whether the cover moved any fluid, which is the only thing worth reporting
     */
    public static boolean tickDrain(Level level, BlockPos machinePos, Direction side, IFluidHandler sink,
                                    long tickCounter) {
        if (sink == null) return false;
        boolean acted = false;
        // Both branches are evaluated independently: they run on different periods.
        if (tickCounter % DRAIN_PERIOD == DRAIN_PHASE) acted = drainFluidBlock(level, machinePos, side, sink);
        if (tickCounter % RAIN_PERIOD == RAIN_PHASE) acted |= collectRain(level, machinePos, side, sink);
        return acted;
    }

    /**
     * GT6 {@code CoverDrain:110-155} without the {@code BlockBaseFluid} branch: a source fluid block
     * in front of the cover is offered to the machine as one bucket and only disappears when the
     * machine took all of it.
     *
     * <p>The all-or-nothing rule is the important half. GT6 calls {@code FL.fillAll}
     * ({@code CoverDrain:148}) and removes the block only inside that {@code if}, so a full tank
     * leaves the world untouched instead of destroying fluid — the same shape is kept here with a
     * {@code SIMULATE} fill, which also avoids allocating the stack twice.</p>
     */
    private static boolean drainFluidBlock(Level level, BlockPos machinePos, Direction side, IFluidHandler sink) {
        BlockPos front = machinePos.relative(side);
        if (!level.hasChunkAt(front)) return false;
        BlockState state = level.getBlockState(front);
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty()) return false;
        // CoverDrain:125,134 drain vanilla water and lava only at getMetaDataAtSide(...) == 0, i.e.
        // source blocks; a flowing level is left to flow.
        if (!fluidState.isSource()) return false;
        Fluid fluid = fluidState.getType();
        if (!drainsAt(side, fluid)) return false;

        FluidStack bucket = new FluidStack(fluid, FLUID_BLOCK_AMOUNT);
        if (sink.fill(bucket, IFluidHandler.FluidAction.SIMULATE) < FLUID_BLOCK_AMOUNT) return false;
        sink.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
        // CoverDrain:152 setBlockToAir, with GT6's own update flag WD.set(..., 3).
        level.setBlock(front, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    /**
     * GT6 {@code CoverDrain:147}: {@code SIDES_HORIZONTAL[aCoverSide] || FL.gas(tFluid) ||
     * (FL.lighter(tFluid) ? SIDES_BOTTOM : SIDES_TOP)[aCoverSide]} — a liquid block may only be
     * drained when that does not work against gravity.
     */
    private static boolean drainsAt(Direction side, Fluid fluid) {
        if (side.getAxis().isHorizontal()) return true;
        if (isGas(fluid)) return true;
        return fluid.getFluidType().isLighterThanAir() ? side == Direction.DOWN : side == Direction.UP;
    }

    /**
     * GT6 {@code CoverDrain:70-84}: rain water into the machine, every 100 ticks, from the up face
     * and the four horizontal ones.
     *
     * <p>{@code FL.fill_} ignores whether the machine had room ({@code CoverDrain:81}), so this
     * returns the ordinary "did anything go in" answer rather than pretending to be strict.</p>
     */
    private static boolean collectRain(Level level, BlockPos machinePos, Direction side, IFluidHandler sink) {
        if (side == Direction.DOWN) return false;                       // SIDES_TOP_HORIZONTAL
        if (!level.isRaining()) return false;
        BlockPos front = machinePos.relative(side);
        if (!level.hasChunkAt(front)) return false;
        if (!level.canSeeSky(front)) return false;                      // getRainOffset, CoverDrain:77-79
        if (!level.getBlockState(front).getFluidState().isEmpty()) return false; // CoverDrain:74

        var climate = level.getBiome(front).value().getModifiedClimateSettings();
        long amount = rainAmount(climate.downfall(), climate.temperature(), level.isThundering());
        if (amount <= 0) return false;
        FluidStack rain = new FluidStack(Fluids.WATER, (int) Math.min(Integer.MAX_VALUE, amount));
        return sink.fill(rain, IFluidHandler.FluidAction.EXECUTE) > 0;
    }

    /**
     * GT6 {@code CoverDrain:72,81}: {@code rainfall > 0 && temperature >= 0.2} gates the collection
     * and the amount is {@code (long)Math.max(1, rainfall * 10000) * (thundering ? 2 : 1)}.
     *
     * <p>Split out from {@link #collectRain} because it is the only part of the rain path that can be
     * asserted: the live path needs {@code Level.isRaining()}, which is the interpolated
     * {@code rainLevel} that the server only ramps by 0.01 per tick ({@code ServerLevel:624-628})
     * before comparing it against 0.2 ({@code Level:786-788}) — a one-shot GameTest body cannot wait
     * out the 21 ticks that takes.</p>
     *
     * @return the millibuckets one operation delivers, or {@code 0} when the biome cannot rain
     */
    public static long rainAmount(float downfall, float temperature, boolean thundering) {
        if (downfall <= 0 || temperature < 0.2F) return 0;
        return Math.max(1L, (long) (downfall * 10000)) * (thundering ? 2 : 1);
    }

    /**
     * GT6 {@code CoverVent:45}: {@code SERVER_TIME % 360 == 30 + 60 * aSide} — the tick inside the
     * 360 tick cycle at which this face's vent fires, so the six faces never all fire at once.
     *
     * <p>GT6 numbers sides in its own order ({@code SIDE_X_NEG} first), the port in
     * {@link Direction#ordinal()}; the six slots {@code 30, 90, 150, 210, 270, 330} are the same set,
     * only which physical face owns which slot differs. That is visible, not silent: the tests read
     * the phase from here instead of hard coding it.</p>
     */
    public static int ventPhase(Direction side) {
        return VENT_OFFSET + VENT_SLOT * side.ordinal();
    }

    /**
     * GT6 {@code CoverVent:47-51}: the air a vent pulls in is chosen by dimension —
     * {@code FL.Air_Nether} in the nether, {@code FL.Air_End} in the end, {@code FL.Air} everywhere
     * else.
     *
     * <p>Returned as GT6's own field name for {@code GTFluids.still}, which is how every other
     * caller in the port names a fluid. GT6's biome-set fallback for modded dimensions is not ported;
     * see the class javadoc.</p>
     */
    public static String ventAirField(ResourceKey<Level> dimension) {
        if (Level.NETHER.equals(dimension)) return "Air_Nether";
        if (Level.END.equals(dimension)) return "Air_End";
        return "Air";
    }

    /**
     * One air vent operation: GT6 {@code CoverVent:44-64}, air from in front of the cover into the
     * machine.
     *
     * <p>The order of the original's tests is kept — cycle slot first ({@code :45}), then
     * {@code WD.collectable_air} ({@code :46}), then the dimension switch — because that is what
     * decides how often the (cheaper) world queries are reached at all.</p>
     *
     * @param level       the server level the machine stands in; also the dimension the air comes from
     * @param machinePos  the machine's own position; the vent's face is {@code machinePos.relative(side)}
     * @param side        the absolute face the vent is attached to, which picks its cycle slot
     * @param sink        the machine as GT6's {@code FL.fill_} sees it (only its input tanks accept)
     * @param tickCounter the owner's cover tick counter, advancing once per server tick
     * @return whether any air went in, i.e. whether a partial 256000 mB fill was accepted
     */
    public static boolean tickVent(Level level, BlockPos machinePos, Direction side, IFluidHandler sink,
                                   long tickCounter) {
        if (sink == null) return false;
        if (tickCounter % VENT_CYCLE != ventPhase(side)) return false;
        BlockPos front = machinePos.relative(side);
        if (!level.hasChunkAt(front)) return false;
        if (!collectableAir(level, front)) return false;
        var air = GTFluids.still(ventAirField(level.dimension()));
        if (air == null) return false;
        // FL.fill_ semantics: fill what fits, drop the rest. No second call and no all-or-nothing
        // simulate, exactly like CoverVent:48.
        FluidStack stack = new FluidStack(air.get(), VENT_AIR_AMOUNT);
        return sink.fill(stack, IFluidHandler.FluidAction.EXECUTE) > 0;
    }

    /**
     * GT6 {@code WD.collectable_air} ({@code WD.java:399-400}): the block in front must neither
     * collide nor be a liquid.
     *
     * <p>Both halves are kept and neither is narrowed to "is an air block", because the original
     * does not narrow it either — anything the player can reach through counts, which is why a vent
     * also works behind a torch or a sapling. The {@code LiquidBlock} test is what catches vanilla
     * water and lava and the port's {@code GTWorldFluidBlock}s even where their fluid state alone
     * would be ambiguous.</p>
     */
    private static boolean collectableAir(Level level, BlockPos front) {
        BlockState state = level.getBlockState(front);
        if (!state.getFluidState().isEmpty() || state.getBlock() instanceof LiquidBlock) return false;
        return state.getCollisionShape(level, front).isEmpty();
    }

    /**
     * Whether a fluid is a gas.
     *
     * <p>GT6's own predicate, not a table written here: {@code FL.gas} ({@code FL.java:770}) is
     * {@code STATE_GASEOUS}, which the port keeps as {@code RegisteredFluids.FluidEntry#gas()} and
     * which {@code FluidHazards} resolves for a live fluid, falling back to Forge's
     * lighter-than-air flag only for fluids GT6 never declared
     * ({@code FluidHazards:125-128}). It allocates nothing and caches nothing, so calling it from
     * {@link #drainsAt} on the cover tick is what it is meant for.</p>
     */
    private static boolean isGas(Fluid fluid) {
        return com.gregtech.gregtech.api.fluid.FluidHazards.isGas(fluid);
    }
}
