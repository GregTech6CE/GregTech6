package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.hazard.BreathingGasEvents;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTanks;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * GT6's acid / gas / plasma fluid marking and the three places it is executed: the fluid pipe
 * ({@code MultiTileEntityPipeFluid:296-317}), the barrel/tank ({@code TileEntityBase08Barrel:170-186}
 * and its fill gate {@code :251-254}) and the breathing hazard
 * ({@code BlockBaseFluid:411-415} via {@code GT_API_Proxy:520-528}).
 *
 * <p>The marking itself lives in {@link FluidHazards}. GT6 answers those three questions from global
 * name sets ({@code FL.java:756} acid, {@code :760} plasma, {@code :770} gas) which are filled from two
 * different facts - gas and plasma are the fluid's creation state ({@code FL.java:1105-1106}) while
 * acid is the <em>material's</em> {@code TD.Properties.ACID} ({@code FL.java:1118},
 * {@code TD.java:386}). The tests below pin both halves, including one fluid that is both at once.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class FluidHazardTests {
    private static final int BASE_X = 23000;
    private static final int BASE_Z = 23000;
    private static final int BASE_Y = 100;

    private static BlockPos at(int dx, int dy, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y + dy, BASE_Z + dz);
    }

    // ---------------------------------------------------------------------------------------------
    // Lookups
    // ---------------------------------------------------------------------------------------------

    /** A registered GT6 fluid by its {@code FL} field name, asserted rather than assumed. */
    private static Fluid still(GameTestHelper helper, String field) {
        RegistryObject<Fluid> registered = GTFluids.still(field);
        helper.assertTrue(registered != null && registered.isPresent(),
                "GT6 fluid " + field + " is registered, got " + registered);
        return registered.get();
    }

    /** The {@code _flowing} variant of a GT6 fluid. */
    private static Fluid flowing(GameTestHelper helper, String field) {
        RegistryObject<Fluid> registered = GTFluids.flowing(field);
        helper.assertTrue(registered != null && registered.isPresent(),
                "GT6 flowing fluid " + field + " is registered, got " + registered);
        return registered.get();
    }

    /**
     * A TINY pipe whose proof flags are exactly the wanted ones, so the test does not depend on which
     * material happens to be first in {@code FluidPipeDefinitions.FLUID_PIPE_MATS}.
     */
    private static FluidPipeBlock pipe(GameTestHelper helper, boolean gasProof, boolean acidProof) {
        for (RegistryObject<FluidPipeBlock> entry : GTFluidPipes.all()) {
            if (!entry.isPresent()) continue;
            PipeSpec spec = entry.get().spec();
            if (spec.size() == PipeSpec.PipeSize.TINY && spec.gasProof() == gasProof
                    && spec.acidProof() == acidProof) {
                return entry.get();
            }
        }
        throw new IllegalStateException("no TINY fluid pipe with gasProof=" + gasProof
                + " acidProof=" + acidProof);
    }

    /** A tank block by its registry id, for the drums that have no static field. */
    private static Block tank(String id) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
    }

    private static TankBlockEntity placeTank(GameTestHelper helper, Block id, BlockPos pos) {
        helper.assertTrue(id != null, "tank block " + id + " is registered");
        ServerLevel level = helper.getLevel();
        // Same reason as placePipe: the world is reused between runs, so the vessel has to be a new one.
        level.removeBlock(pos, false);
        level.setBlock(pos, id.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockEntity(pos) instanceof TankBlockEntity,
                "the tank at " + pos + " has its block entity");
        TankBlockEntity tank = (TankBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(tank.getFluidTank().getAmount() == 0,
                "a vessel placed at " + pos + " starts empty, got " + tank.getFluidTank().getAmount());
        return tank;
    }

    private static FluidPipeBlockEntity placePipe(GameTestHelper helper, FluidPipeBlock block, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        // The GameTest world (build/gametest-run/world) survives between runs, and so does anything an
        // earlier run left at these absolute coordinates. Re-setting the same block state is then a
        // no-op that keeps the old block entity - and a pipe whose tank is already full of the very
        // fluid being filled returns 0 from fill(), which reads as "the pipe refused the gas". Run 2 of
        // §107's gate failed exactly that way on 100 mB of natural gas left in the tiny plastic pipe by
        // run 1. Clearing the position first makes the placement a real placement again.
        level.removeBlock(pos, false);
        level.setBlock(pos, block.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity,
                "the fluid pipe at " + pos + " has its block entity");
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(pipe.getFluidInTank(0).isEmpty(),
                "a pipe placed at " + pos + " starts empty, got " + pipe.getFluidInTank(0));
        return pipe;
    }

    /**
     * Moves the entity so that {@link BreathingGasEvents#headPos} lands on {@code gasPos} exactly:
     * GT6 reads the block at {@code roundDown(posY + getEyeHeight())}
     * ({@code GT_API_Proxy:523-525}), so the eye height of the entity decides where the gas has to be.
     */
    private static void putEyesIn(LivingEntity entity, BlockPos gasPos) {
        entity.moveTo(gasPos.getX() + 0.5D, gasPos.getY() + 0.5D - entity.getEyeHeight(),
                gasPos.getZ() + 0.5D, entity.getYRot(), entity.getXRot());
        entity.invulnerableTime = 0;
    }

    // ---------------------------------------------------------------------------------------------
    // 1) The marking table itself
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void markingTableMatchesGt6sFluidSets(GameTestHelper helper) {
        // GT6 FL.java:1105 - STATE_GASEOUS fluids are added to FluidsGT.GAS, so a gas is a fluid
        // created as one, and FL.java:770 is the test that reads it back.
        Fluid steam = still(helper, "Steam");
        Fluid naturalGas = still(helper, "Gas_Natural");
        helper.assertTrue(FluidHazards.isGas(steam), "steam is a gas, got " + FluidHazards.isGas(steam));
        helper.assertTrue(FluidHazards.isGas(naturalGas),
                "natural gas is a gas, got " + FluidHazards.isGas(naturalGas));
        helper.assertTrue(FluidHazards.kindOf(steam) == FluidHazards.Kind.GAS,
                "steam is Kind.GAS, got " + FluidHazards.kindOf(steam));

        // FL.java:1106 - STATE_PLASMA fluids go to FluidsGT.PLASMA, tested at FL.java:760.
        Fluid plasma = still(helper, "HeliumPlasma");
        helper.assertTrue(FluidHazards.isPlasma(plasma),
                "helium plasma is a plasma, got " + FluidHazards.isPlasma(plasma));
        helper.assertTrue(FluidHazards.kindOf(plasma) == FluidHazards.Kind.PLASMA,
                "helium plasma is Kind.PLASMA, got " + FluidHazards.kindOf(plasma));
        helper.assertTrue(!FluidHazards.isGas(plasma), "a plasma is not also reported as a gas");

        // FL.java:756 + FL.java:1118 + TD.java:386 - acid is the *material* property, so it is a
        // separate question from the state: sulfuric acid is a liquid and an acid at the same time.
        Fluid sulfuricAcid = still(helper, "GenLiquid_SulfuricAcid");
        helper.assertTrue(FluidHazards.isAcid(sulfuricAcid),
                "sulfuric acid is an acid, got " + FluidHazards.isAcid(sulfuricAcid));
        helper.assertTrue(!FluidHazards.isGas(sulfuricAcid),
                "sulfuric acid is a liquid, not a gas, got " + FluidHazards.isGas(sulfuricAcid));
        helper.assertTrue(FluidHazards.kindOf(sulfuricAcid) == FluidHazards.Kind.ACID,
                "sulfuric acid is Kind.ACID, got " + FluidHazards.kindOf(sulfuricAcid));

        // And the case that proves the three predicates are independent rather than one switch:
        // hydrogen chloride is MT.java:1020 `gasaciddcmp`, i.e. in FluidsGT.GAS *and* FluidsGT.ACID.
        Fluid hydrogenChloride = still(helper, "GenGas_HydrochloricAcid");
        helper.assertTrue(FluidHazards.isAcid(hydrogenChloride) && FluidHazards.isGas(hydrogenChloride),
                "hydrogen chloride is both an acid and a gas, got acid "
                        + FluidHazards.isAcid(hydrogenChloride) + " gas " + FluidHazards.isGas(hydrogenChloride));
        // 16 for the acid plus 8 for the gas, the two trashes GT6 applies in turn (one tank, :297+:309).
        helper.assertTrue(FluidHazards.pipeTrashPerTick(hydrogenChloride) == 24,
                "an acid gas costs 24 a tick, got " + FluidHazards.pipeTrashPerTick(hydrogenChloride));

        // Water and lava are neither. Lava in particular is Kind.NONE, not an acid - GT6 has no
        // FluidsGT.ACID entry for it (MT.java:103 is a plain `lqud`, not a `lqudacid*`).
        helper.assertTrue(FluidHazards.kindOf(Fluids.WATER) == FluidHazards.Kind.NONE,
                "water is Kind.NONE, got " + FluidHazards.kindOf(Fluids.WATER));
        helper.assertTrue(FluidHazards.kindOf(Fluids.LAVA) == FluidHazards.Kind.NONE,
                "lava is Kind.NONE, got " + FluidHazards.kindOf(Fluids.LAVA));
        helper.assertTrue(!FluidHazards.isAcid(Fluids.LAVA) && !FluidHazards.isGas(Fluids.LAVA)
                        && !FluidHazards.isPlasma(Fluids.LAVA),
                "lava carries no hazard class at all, got kind " + FluidHazards.kindOf(Fluids.LAVA));

        // The three world waters resolve to a GT6 entry, so they must not fall through to Forge's
        // lighter-than-air flag either.
        helper.assertTrue(FluidHazards.kindOf(still(helper, "Ocean")) == FluidHazards.Kind.NONE,
                "sea water is Kind.NONE, got " + FluidHazards.kindOf(still(helper, "Ocean")));

        // The per-kind trash amounts are GT6's own numbers.
        helper.assertTrue(FluidHazards.Kind.NONE.pipeTrashPerTick() == 0
                        && FluidHazards.Kind.GAS.pipeTrashPerTick() == 8
                        && FluidHazards.Kind.ACID.pipeTrashPerTick() == 16
                        && FluidHazards.Kind.PLASMA.pipeTrashPerTick() == 64,
                "GT6 trashes 0/8/16/64 per tick, got " + FluidHazards.Kind.NONE.pipeTrashPerTick() + "/"
                        + FluidHazards.Kind.GAS.pipeTrashPerTick() + "/"
                        + FluidHazards.Kind.ACID.pipeTrashPerTick() + "/"
                        + FluidHazards.Kind.PLASMA.pipeTrashPerTick());
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 2) The _flowing variants
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void flowingVariantsCarryTheSameMarking(GameTestHelper helper) {
        // A flowing gas/plasma/acid is a fluid of its own in the registry (<path>_flowing), and it is
        // what most positions of a fluid body actually hold, so a marking keyed by the still fluid
        // alone would miss them - the same trap GTWaterParity.isWorldWater documents for the water.
        Fluid gasStill = still(helper, "Gas_Natural");
        Fluid gasFlowing = flowing(helper, "Gas_Natural");
        helper.assertTrue(gasStill != gasFlowing, "the flowing gas is a distinct registered fluid");
        helper.assertTrue(FluidHazards.isGas(gasFlowing),
                "the flowing gas is a gas too, got " + FluidHazards.isGas(gasFlowing));
        helper.assertTrue(FluidHazards.kindOf(gasFlowing) == FluidHazards.Kind.GAS,
                "flowing natural gas is Kind.GAS, got " + FluidHazards.kindOf(gasFlowing));

        Fluid acidFlowing = flowing(helper, "GenLiquid_SulfuricAcid");
        helper.assertTrue(FluidHazards.isAcid(acidFlowing),
                "the flowing acid is an acid too, got " + FluidHazards.isAcid(acidFlowing));
        helper.assertTrue(FluidHazards.isAcid(acidFlowing) == FluidHazards.isAcid(still(helper, "GenLiquid_SulfuricAcid")),
                "still and flowing acid agree");

        Fluid plasmaFlowing = flowing(helper, "HeliumPlasma");
        helper.assertTrue(FluidHazards.isPlasma(plasmaFlowing),
                "the flowing plasma is a plasma too, got " + FluidHazards.isPlasma(plasmaFlowing));

        // Stepping outside the GT6 registry: the three world waters are still not gases through their
        // _flowing forms either.
        helper.assertTrue(FluidHazards.kindOf(flowing(helper, "Swampwater")) == FluidHazards.Kind.NONE,
                "flowing swamp water is Kind.NONE, got " + FluidHazards.kindOf(flowing(helper, "Swampwater")));
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 3) The accept / reject truth table
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void proofFlagTruthTable(GameTestHelper helper) {
        FluidHazards.Kind[] kinds = {FluidHazards.Kind.NONE, FluidHazards.Kind.ACID,
                FluidHazards.Kind.GAS, FluidHazards.Kind.PLASMA};

        // All eight combinations of the three flags, against all four kinds: a vessel refuses a hazard
        // exactly when the one flag that guards it is off (MultiTileEntityPipeFluid:296-317,
        // TileEntityBase08Barrel:251-254), and never refuses Kind.NONE.
        for (FluidHazards.Kind kind : kinds) {
            for (int mask = 0; mask < 8; mask++) {
                boolean gasProof = (mask & 1) != 0;
                boolean acidProof = (mask & 2) != 0;
                boolean plasmaProof = (mask & 4) != 0;
                boolean expected = switch (kind) {
                    case NONE -> false;
                    case GAS -> !gasProof;
                    case ACID -> !acidProof;
                    case PLASMA -> !plasmaProof;
                };
                boolean actual = FluidHazards.proofRejects(kind, gasProof, acidProof, plasmaProof);
                helper.assertTrue(actual == expected, kind + " with gasProof=" + gasProof + " acidProof="
                        + acidProof + " plasmaProof=" + plasmaProof + " must be " + expected
                        + ", got " + actual);
            }
        }

        // The same table against real fluids, where a fluid with two classes needs both flags: GT6
        // asks each question separately, so a gas-proof-only pipe still rejects an acid gas.
        Fluid steam = still(helper, "Steam");
        helper.assertTrue(FluidHazards.proofRejects(steam, false, true, true),
                "a gas in a non-gas-proof vessel is rejected");
        helper.assertTrue(!FluidHazards.proofRejects(steam, true, false, false),
                "a gas-proof vessel only needs its own flag");
        Fluid sulfuricAcid = still(helper, "GenLiquid_SulfuricAcid");
        helper.assertTrue(FluidHazards.proofRejects(sulfuricAcid, true, false, true),
                "an acid in a non-acid-proof vessel is rejected even with the other two flags on");
        helper.assertTrue(!FluidHazards.proofRejects(sulfuricAcid, false, true, false),
                "an acid-proof vessel only needs its own flag");
        Fluid plasma = still(helper, "HeliumPlasma");
        helper.assertTrue(FluidHazards.proofRejects(plasma, true, true, false),
                "a plasma in a non-plasma-proof vessel is rejected");
        helper.assertTrue(!FluidHazards.proofRejects(plasma, false, false, true),
                "a plasma-proof vessel only needs its own flag");
        helper.assertTrue(FluidHazards.proofRejects(still(helper, "GenGas_HydrochloricAcid"), true, false, false),
                "acid gas in a vessel that only covers gas is rejected");
        helper.assertTrue(!FluidHazards.proofRejects(still(helper, "GenGas_HydrochloricAcid"), true, true, false),
                "acid gas in a vessel that covers both is accepted");

        helper.assertTrue(!FluidHazards.proofRejects(Fluids.WATER, false, false, false),
                "water is never rejected, whatever the flags say");
        helper.assertTrue(!FluidHazards.proofRejects(Fluids.LAVA, false, false, false),
                "lava is Kind.NONE and never rejected, whatever the flags say");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 4) Execution: the barrel/tank
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void tankRejectsAndDissolvesWhatItsFlagsDoNotCover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // A wooden barrel is proof against nothing (TankDefinitions:60), a steel drum is gas proof but
        // not acid proof (TankDefinitions:35), and a stainless drum is both (:36).
        TankBlockEntity barrel = placeTank(helper, GTTanks.WOOD_BARREL.get(), at(0, 0, 0));
        TankBlockEntity steelDrum = placeTank(helper, GTTanks.DRUM_STEEL.get(), at(4, 0, 0));
        TankBlockEntity stainlessDrum = placeTank(helper, tank("drum_stainless_steel"), at(8, 0, 0));

        Fluid gas = still(helper, "Gas_Natural");
        Fluid acid = still(helper, "GenLiquid_SulfuricAcid");

        // GT6 TileEntityBase08Barrel:251-254 - the fill gate refuses what the flags do not cover.
        helper.assertTrue(!barrel.isFluidValid(0, new FluidStack(gas, 1000)),
                "a wooden barrel refuses a gas");
        int barrelTook = barrel.fill(new FluidStack(gas, 1000), FluidAction.EXECUTE);
        helper.assertTrue(barrelTook == 0, "and its fill() refuses it as well, got " + barrelTook);
        int steelTook = steelDrum.fill(new FluidStack(gas, 1000), FluidAction.EXECUTE);
        helper.assertTrue(steelTook == 1000, "a steel drum is gas proof and takes it, got " + steelTook);

        helper.assertTrue(!steelDrum.isFluidValid(0, new FluidStack(acid, 1000)),
                "a steel drum refuses an acid");
        int steelAcidTook = steelDrum.fill(new FluidStack(acid, 1000), FluidAction.EXECUTE);
        helper.assertTrue(steelAcidTook == 0, "and its fill() refuses it as well, got " + steelAcidTook);
        int stainlessTook = stainlessDrum.fill(new FluidStack(acid, 1000), FluidAction.EXECUTE);
        helper.assertTrue(stainlessTook == 1000,
                "a stainless drum is acid proof and takes it, got " + stainlessTook);
        helper.assertTrue(!stainlessDrum.isFluidValid(0, new FluidStack(still(helper, "HeliumPlasma"), 1000)),
                "a stainless drum still refuses a plasma");

        // Kind.NONE is never refused: an ordinary liquid goes into the wooden barrel.
        int waterTook = barrel.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        helper.assertTrue(waterTook == 1000, "a wooden barrel still takes water, got " + waterTook);

        // GT6 TileEntityBase08Barrel:170-175 - acid that got in anyway dissolves the barrel: the
        // contents are trashed and the block itself is replaced with air. The content is put in
        // through the tank directly, which is the only way past the fill gate above.
        BlockPos corrodedPos = at(12, 0, 0);
        TankBlockEntity corroded = placeTank(helper, GTTanks.DRUM_STEEL.get(), corrodedPos);
        corroded.getFluidTank().setFluid(new FluidStack(acid, 1000));
        helper.assertTrue(corroded.getFluidTank().getAmount() == 1000, "the acid is in the drum");
        TankBlockEntity.serverTick(level, corrodedPos, corroded.getBlockState(), corroded);
        helper.assertTrue(level.getBlockState(corrodedPos).isAir(),
                "acid dissolves the drum (GT6 setToAir), block is "
                        + level.getBlockState(corrodedPos).getBlock());
        helper.assertTrue(corroded.getFluidTank().getAmount() == 0,
                "and the acid is gone with it, got " + corroded.getFluidTank().getAmount());

        // A gas alone does not dissolve anything: GT6 :180-183 only trashes the contents, because the
        // dissolving branch is the acid one above. Wooden barrels are the only tanks whose gasProof is
        // off (TankDefinitions:60), so the gas has to come in past the fill gate like the acid did.
        BlockPos gassedPos = at(16, 0, 0);
        TankBlockEntity gassed = placeTank(helper, GTTanks.WOOD_BARREL.get(), gassedPos);
        gassed.getFluidTank().setFluid(new FluidStack(gas, 1000));
        TankBlockEntity.serverTick(level, gassedPos, gassed.getBlockState(), gassed);
        helper.assertTrue(gassed.getFluidTank().getAmount() == 0, "the gas is trashed, got "
                + gassed.getFluidTank().getAmount());
        helper.assertTrue(!level.getBlockState(gassedPos).isAir(),
                "but a gas does not dissolve the vessel, block is "
                        + level.getBlockState(gassedPos).getBlock());
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 5) Execution: the pipe
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void pipeEatsWhatItsFlagsDoNotCover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Fluid gas = still(helper, "Gas_Natural");
        Fluid acid = still(helper, "GenLiquid_SulfuricAcid");

        // A wood pipe is proof against nothing (FluidPipeDefinitions:19). GT6's pipe does not refuse
        // the fluid - it accepts it and then leaks it, 8 units a tick (MultiTileEntityPipeFluid:296).
        BlockPos woodPos = at(0, 8, 0);
        FluidPipeBlockEntity wood = placePipe(helper, pipe(helper, false, false), woodPos);
        helper.assertTrue(wood.fill(new FluidStack(gas, 1000), FluidAction.EXECUTE) > 0,
                "a pipe accepts what it cannot hold, that is the GT6 behaviour");
        long before = wood.getFluidInTank(0).getAmount();
        helper.assertTrue(before > 8, "the wood pipe holds the gas, got " + before);
        FluidPipeBlockEntity.serverTick(level, woodPos, wood.getBlockState(), wood);
        helper.assertTrue(wood.getFluidInTank(0).getAmount() == before - 8,
                "GT6 :297 trashes 8 a tick, got " + wood.getFluidInTank(0).getAmount()
                        + " from " + before);

        // A steel pipe is gas proof but not acid proof (FluidPipeDefinitions:28), so the same gas is
        // kept - proof flags are asked per hazard class, not "is this pipe safe at all".
        BlockPos steelPos = at(4, 8, 0);
        FluidPipeBlockEntity steel = placePipe(helper, pipe(helper, true, false), steelPos);
        helper.assertTrue(steel.fill(new FluidStack(gas, 1000), FluidAction.EXECUTE) > 0,
                "the steel pipe takes the gas");
        long kept = steel.getFluidInTank(0).getAmount();
        FluidPipeBlockEntity.serverTick(level, steelPos, steel.getBlockState(), steel);
        helper.assertTrue(steel.getFluidInTank(0).getAmount() == kept,
                "a gas-proof pipe keeps its gas, got " + steel.getFluidInTank(0).getAmount()
                        + " from " + kept);

        // The acid is another matter: 16 units a tick (GT6 :309), and :312 rolls 1-in-100 to destroy
        // the pipe outright, so both outcomes are legal and the assertion accepts either.
        BlockPos acidPos = at(8, 8, 0);
        FluidPipeBlockEntity acidPipe = placePipe(helper, pipe(helper, true, false), acidPos);
        helper.assertTrue(acidPipe.fill(new FluidStack(acid, 1000), FluidAction.EXECUTE) > 0,
                "the steel pipe takes the acid too");
        long acidBefore = acidPipe.getFluidInTank(0).getAmount();
        helper.assertTrue(acidBefore > 16, "the pipe holds the acid, got " + acidBefore);
        FluidPipeBlockEntity.serverTick(level, acidPos, acidPipe.getBlockState(), acidPipe);
        boolean destroyed = level.getBlockState(acidPos).isAir();
        long after = acidPipe.getFluidInTank(0).getAmount();
        helper.assertTrue(destroyed ? after == 0 : after == acidBefore - 16,
                "GT6 :309 trashes 16 of the acid and :312 destroys the pipe on 1-in-100, kept " + after
                        + " of " + acidBefore + ", destroyed " + destroyed);
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // 6) Execution: breathing
    // ---------------------------------------------------------------------------------------------

    /**
     * Places GT6's natural-gas spring block at far-away absolute coordinates - the single gas that has
     * a world block in the port ({@code Loader_Fluids.WORLD_FLUID_PATHS}) and so the only one anything
     * can be standing in. The coordinates are outside the {@code test_empty} template (which is only
     * 1x1x1), so the block comes from {@code helper.getLevel()} rather than from a relative position.
     */
    private static BlockPos placeNaturalGas(GameTestHelper helper) {
        BlockPos pos = at(0, 12, 0);
        Block block = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", BreathingGasEvents.NATURAL_GAS));
        helper.assertTrue(block instanceof LiquidBlock,
                "gregtech:" + BreathingGasEvents.NATURAL_GAS + " is a world fluid block, got " + block);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 2);
        helper.assertTrue(helper.getLevel().getFluidState(pos).getType() == still(helper, "Gas_Natural"),
                "the block holds GT6's natural gas, got " + helper.getLevel().getFluidState(pos).getType());
        return pos;
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void breathingNaturalGasHurtsWhoeverBreathesIt(GameTestHelper helper) {
        BlockPos gasPos = placeNaturalGas(helper);

        // A cow has no protection and is not in GT6's immune list, so it takes the full 2.0.
        Cow cow = helper.spawn(EntityType.COW, BlockPos.ZERO);
        putEyesIn(cow, gasPos);
        helper.assertTrue(BreathingGasEvents.headPos(cow).equals(gasPos),
                "the eyes are in " + gasPos + ", got " + BreathingGasEvents.headPos(cow));
        helper.assertTrue(FluidHazards.isGas(BreathingGasEvents.headFluid(cow)),
                "the fluid at eye height is a gas, got " + BreathingGasEvents.headFluid(cow));
        helper.assertTrue(BreathingGasEvents.breatheIn(cow), "a cow breathing natural gas is hurt");
        helper.assertTrue(Math.abs(10.0F - cow.getHealth() - BreathingGasEvents.DROWN_DAMAGE) < 0.001F,
                "GT6 BlockBaseFluid:414 deals exactly 2.0, health " + cow.getHealth());

        // GT6 Loader_Blocks:153 - the natural-gas block declares poison 300/0 and confusion 120/0, and
        // that non-empty mEffectsBreathing list is what makes the drown rule run at all (:412).
        MobEffectInstance poison = cow.getEffect(MobEffects.POISON);
        MobEffectInstance confusion = cow.getEffect(MobEffects.CONFUSION);
        helper.assertTrue(poison != null && poison.getDuration() == 300 && poison.getAmplifier() == 0,
                "natural gas poisons for 300 ticks at amplifier 0, got "
                        + (poison == null ? "no effect" : poison.getDuration() + "/" + poison.getAmplifier()));
        helper.assertTrue(confusion != null && confusion.getDuration() == 120 && confusion.getAmplifier() == 0,
                "and confuses for 120 ticks at amplifier 0, got "
                        + (confusion == null ? "no effect" : confusion.getDuration() + "/" + confusion.getAmplifier()));

        // A bare survival player is hurt the same way (GT6 checks only the gas suit, not players).
        Player survivor = helper.makeMockSurvivalPlayer();
        helper.assertTrue(!survivor.isCreative(), "makeMockSurvivalPlayer is not creative");
        putEyesIn(survivor, gasPos);
        helper.assertTrue(BreathingGasEvents.breatheIn(survivor), "a bare survival player is hurt");
        helper.assertTrue(Math.abs(20.0F - survivor.getHealth() - BreathingGasEvents.DROWN_DAMAGE) < 0.001F,
                "a player loses exactly 2.0, health " + survivor.getHealth());

        // GT6 UT.java:2971 isWearingFullGasHazmat doubles as the immunity list, and GT6's list keeps
        // an iron golem out; GTHazmat:127-130 is the port's copy of it.
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, BlockPos.ZERO);
        putEyesIn(golem, gasPos);
        helper.assertTrue(GTEntityHelper.isImmuneToBreathingGases(golem),
                "an iron golem is immune to breathing gases");
        helper.assertTrue(!BreathingGasEvents.breatheIn(golem), "so it is not hurt by the gas");
        helper.assertTrue(golem.getHealth() == golem.getMaxHealth(),
                "an iron golem keeps its 100 health, got " + golem.getHealth());

        // Creative mode is the other half of the same check (GTHazmat.isCreative, and makeMockPlayer()
        // is creative no matter what its abilities say, GameTestHelper:214-228).
        Player creative = helper.makeMockPlayer();
        helper.assertTrue(creative.isCreative(), "makeMockPlayer is creative");
        putEyesIn(creative, gasPos);
        helper.assertTrue(GTEntityHelper.isImmuneToBreathingGases(creative),
                "creative mode is immune to breathing gases");
        helper.assertTrue(!BreathingGasEvents.breatheIn(creative), "a creative player is not hurt");
        helper.assertTrue(creative.getHealth() == creative.getMaxHealth(),
                "a creative player keeps full health, got " + creative.getHealth());
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void breathingIgnoresNonGasesAndGasesWithoutEffects(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, BlockPos.ZERO);
        cow.invulnerableTime = 0;

        // GT6 BlockWaterlike:227 keys the breathing hazard on the fluid being a gas, so a liquid at
        // eye height is not this hazard - even an acid one, whose hazard is the bathing one.
        helper.assertTrue(!BreathingGasEvents.breatheIn(cow, Fluids.WATER),
                "water at eye height is not breathed");
        helper.assertTrue(!BreathingGasEvents.breatheIn(cow, still(helper, "GenLiquid_SulfuricAcid")),
                "a liquid acid at eye height is not breathed");
        helper.assertTrue(cow.getHealth() == 10.0F,
                "none of that hurt the cow, health " + cow.getHealth());

        // GT6 BlockBaseFluid:412 requires a non-empty mEffectsBreathing list, and Loader_Blocks only
        // gives one to the four oils and the natural gas - so a gas without effects deals no damage.
        Fluid steam = still(helper, "Steam");
        helper.assertTrue(FluidHazards.isGas(steam), "steam is a gas");
        helper.assertTrue(!BreathingGasEvents.applyBreathingEffects(cow, steam),
                "steam declares no breathing effects");
        helper.assertTrue(!BreathingGasEvents.breatheIn(cow, steam),
                "so breathing steam deals no damage");
        helper.assertTrue(BreathingGasEvents.applyBreathingEffects(cow, still(helper, "Gas_Natural")),
                "natural gas declares them");
        helper.assertTrue(cow.getHealth() == 10.0F, "still unhurt, health " + cow.getHealth());

        // GT6 BlockBaseFluid:414 lands the drown hit only every 20 ticks.
        helper.assertTrue(BreathingGasEvents.isDrownTick(0) && BreathingGasEvents.isDrownTick(40)
                        && BreathingGasEvents.isDrownTick(1200),
                "GT6 hits every 20 ticks");
        helper.assertTrue(!BreathingGasEvents.isDrownTick(1) && !BreathingGasEvents.isDrownTick(19)
                        && !BreathingGasEvents.isDrownTick(41),
                "and not in between");

        // Air is not a gas either: GT6's air fluids are in FluidsGT.AIR, and the port's own Air fluid
        // is a gas, but Fluids.EMPTY - the fluid of a block of air - must not be one.
        helper.assertTrue(!FluidHazards.isGas(Fluids.EMPTY),
                "the empty fluid is not a gas, got " + FluidHazards.isGas(Fluids.EMPTY));
        helper.assertTrue(!BreathingGasEvents.breatheIn(cow, Fluids.EMPTY), "and nothing is breathed");
        helper.succeed();
    }
}
