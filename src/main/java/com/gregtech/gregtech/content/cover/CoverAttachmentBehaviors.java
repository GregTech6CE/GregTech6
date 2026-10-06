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

/** Native GT6 drain and air-vent behavior, driven by the original tick phases.
 * Drain operations preserve all-or-nothing fills, gravity and source-block rules, bulk GT waters,
 * XP conversion and entity walk-over products. Vents require a ticking fluid host and select air by
 * vanilla dimension or biome tags, including explicit void/space rejection.
 * Optional OpenBlocks player XP and foreign slime integrations remain separate compatibility work.
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
        if (tickCounter % 100 == 50) acted |= collectExperience(level,machinePos,side,sink);
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
    private static boolean collectExperience(Level level,BlockPos pos,Direction side,IFluidHandler sink){
        var registered=GTFluids.still("XP");boolean mob=false;
        if(registered==null){registered=GTFluids.still("Mob");mob=true;}
        if(registered==null)return false;
        boolean acted=false;var center=pos.relative(side,2);
        for(var orb:level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new net.minecraft.world.phys.AABB(center).inflate(1))){
            long points=(long)orb.getValue()*Math.max(1,((com.gregtech.gregtech.mixin.ExperienceOrbCountAccessor)orb).gregtech$count());
            if(points<=0||points>Integer.MAX_VALUE/200)continue;
            int amount=(int)(mob?points*200/3:points*20);var stack=new FluidStack(registered.get(),amount);
            if(sink.fill(stack,IFluidHandler.FluidAction.SIMULATE)<amount)continue;
            if(sink.fill(stack,IFluidHandler.FluidAction.EXECUTE)==amount){orb.discard();acted=true;}
        }
        return acted;
    }

    public static void walkOverDrain(Level level,BlockPos pos,IFluidHandler sink,net.minecraft.world.entity.Entity entity){
        if(level.isClientSide||sink==null||level.getGameTime()%20!=5)return;
        String field=null;int amount=1;
        if(entity instanceof net.minecraft.world.entity.animal.Squid)field="InkSquid";
        else if(entity instanceof net.minecraft.world.entity.monster.MagmaCube slime){field="Blaze";amount=Math.max(1,slime.getSize());}
        else if(entity instanceof net.minecraft.world.entity.monster.Slime slime){field="Slime_Green";amount=Math.max(1,slime.getSize());}
        else if(entity instanceof net.minecraft.world.entity.animal.Animal animal&&!animal.isBaby()){field="Sewage";amount=Math.max(1,(int)(20*entity.getBbWidth()*entity.getBbWidth()*entity.getBbHeight()));}
        if(field!=null){var fluid=GTFluids.still(field);if(fluid!=null)sink.fill(new FluidStack(fluid.get(),amount),IFluidHandler.FluidAction.EXECUTE);}
    }

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

        var entry=GTFluids.entryForFluid(fluid);
        boolean worldWater=entry!=null&&com.gregtech.gregtech.api.fluid.GTWaterParity.isWorldWater(entry.registryName());
        var biome=level.getBiome(front);int waterLevel=level.getSeaLevel();
        boolean infiniteVanilla=fluid==Fluids.WATER&&front.getY()>=waterLevel-15&&front.getY()<=waterLevel&&biome.is(net.minecraft.tags.BiomeTags.IS_RIVER);
        if(worldWater||infiniteVanilla)return sink.fill(new FluidStack(fluid,16000),IFluidHandler.FluidAction.EXECUTE)>0;
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
        String field=ventAirField(level.dimension());
        if(!Level.OVERWORLD.equals(level.dimension())&&!Level.NETHER.equals(level.dimension())&&!Level.END.equals(level.dimension())){
            var biome=level.getBiome(front);
            if(biome.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","no_collectable_air")))
                    ||biome.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","is_void"))))return false;
            if(biome.is(net.minecraft.tags.BiomeTags.IS_END))field="Air_End";
            else if(biome.is(net.minecraft.tags.BiomeTags.IS_NETHER))field="Air_Nether";
        }
        var air = GTFluids.still(field);
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
