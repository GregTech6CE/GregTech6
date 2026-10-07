package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The drain cover ({@code drain}) and the air vent ({@code air_vent}): the two covers whose behaviour
 * lives in {@code CoverAttachmentBehaviors} instead of in a GUI panel.
 *
 * <p>GT6 sources: {@code CoverDrain:68-159} for the drain, {@code CoverVent:40-85} for the vent — see
 * {@link CoverAttachmentBehaviors} for what each one ports and what it deliberately leaves out.</p>
 *
 * <h2>Why the machines stand at {@link #BASE_X}, {@link #BASE_Z}</h2>
 *
 * <p>These tests build a machine, a cover and a fluid block next to each other and then decide
 * themselves when a tick happens, so the template is the 1×1×1 {@code test_empty} and every block is
 * placed through {@link GameTestHelper#getLevel()} at a coordinate pair no other suite uses
 * (checked: 25000 is not a {@code BASE_X} or {@code BASE_Z} anywhere else in this package). Each test
 * has its own site inside that area, and each site is wiped before use, which makes the file
 * re-runnable in a world that already ran it.</p>
 *
 * <h2>What "the face returns drain / air_vent" means here</h2>
 *
 * <p>{@code BasicMachineBlockEntity.getCoverId(Direction)} is the dispatch expression, and it is
 * private; this batch may only add to that class, so the test asserts the identical expression
 * through the public getter — {@code CoverItems.behavior(machine.getCover(side))}. The switch in
 * {@code tickCovers} matching that id is what makes the cover act at all, so the effect assertions
 * below (fluid arriving, tanks emptying) are the independent proof that the same id reached the
 * dispatcher.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CoverAttachmentTests {
    private static final int BASE_X = 25000;
    private static final int BASE_Y = 100;
    private static final int BASE_Z = 25000;

    /** One of this file's sites, {@code dx}/{@code dz} apart inside the reserved area. */
    private static BlockPos site(int dx, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y, BASE_Z + dz);
    }

    /** Wipes a site, so a re-run starts from the same world state. */
    private static void clearSite(ServerLevel level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 2; y++) for (int z = -1; z <= 1; z++) {
            level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * A machine on a stone floor with one input and one output fluid tank, on its own site.
     *
     * <p>The recipe map is empty on purpose: the covers have to work on an idle machine, and an idle
     * machine is also what keeps the tank contents untouched by anything but the cover under test.</p>
     */
    private static BasicMachineBlockEntity machine(ServerLevel level, BlockPos pos) {
        clearSite(level, pos);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            level.setBlock(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
        }
        var block = MachineRegistry.basicMachines().get(0).get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        var recipes = new RecipeMap(null, "cover_attachment_" + Long.toUnsignedString(pos.asLong()),
                "Cover attachment", "cover_attachment", 1, 1, 1, 1, 1, 0, 1, false, false, false, false);
        machine.setSpec(BasicMachineSpec.builder("cover_attachment_test", block.basicSpec().material())
                .machineType("test").energy(GregTechTags.Energy.EU, 32).recipes(recipes)
                .faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
        return machine;
    }

    /**
     * Ticks the machine itself. The covers throttle on the machine's own cover tick counter, which
     * this advances once per call, so the number of ticks the test asks for is the number it gets.
     */
    private static void tick(BasicMachineBlockEntity machine, int times) {
        for (int i = 0; i < times; i++) {
            BasicMachineBlockEntity.serverTick(machine.getLevel(), machine.getBlockPos(),
                    machine.getBlockState(), machine);
        }
    }

    private static ItemStack cover(String id) {
        return new ItemStack(GTTechnological.get(id));
    }

    /** {@code test_empty} is a 1×1×1 blueprint; this test needs no world at all. */
    @GameTest(template = "test_empty")
    public static void coverItemIdsAndRainAmountMatchTheOriginal(GameTestHelper helper) {
        String drain = CoverItems.behavior(cover(CoverAttachmentBehaviors.DRAIN));
        String vent = CoverItems.behavior(cover(CoverAttachmentBehaviors.AIR_VENT));
        helper.assertTrue(CoverAttachmentBehaviors.DRAIN.equals(drain),
                "gregtech:drain is the drain cover behaviour, got " + drain);
        helper.assertTrue(CoverAttachmentBehaviors.AIR_VENT.equals(vent),
                "gregtech:air_vent is the air vent behaviour, got " + vent);
        helper.assertTrue(!"drain".equals(vent) && !"air_vent".equals(drain),
                "the two covers do not share a behaviour id");

        // CoverDrain:72,81 - rainfall > 0 && temperature >= 0.2, then max(1, rainfall*10000) doubled
        // by a thunderstorm.
        helper.assertTrue(CoverAttachmentBehaviors.rainAmount(0.5F, 0.5F, false) == 5000,
                "half rainfall gives 5000 mB, got " + CoverAttachmentBehaviors.rainAmount(0.5F, 0.5F, false));
        helper.assertTrue(CoverAttachmentBehaviors.rainAmount(0.5F, 0.5F, true) == 10000,
                "a thunderstorm doubles it, got " + CoverAttachmentBehaviors.rainAmount(0.5F, 0.5F, true));
        helper.assertTrue(CoverAttachmentBehaviors.rainAmount(0.0F, 0.5F, false) == 0,
                "a dry biome collects nothing, got " + CoverAttachmentBehaviors.rainAmount(0.0F, 0.5F, false));
        helper.assertTrue(CoverAttachmentBehaviors.rainAmount(0.5F, 0.19F, false) == 0
                        && CoverAttachmentBehaviors.rainAmount(0.5F, 0.2F, false) == 5000,
                "temperature >= 0.2 is the exact cold-region gate (CoverDrain:72)");
        helper.succeed();
    }

    /** GT6 {@code CoverDrain:124-131,148-152}: one water source block becomes one bucket in the tank. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void drainCoverPullsAnAdjacentWaterSourceIntoTheTank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(0, 0);
        var machine = machine(level, pos);
        BlockPos water = pos.above();
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);

        var drain = cover(CoverAttachmentBehaviors.DRAIN);
        helper.assertTrue(machine.attachCover(Direction.UP, drain), "the machine face accepts the drain cover");
        helper.assertTrue(CoverItems.behavior(machine.getCover(Direction.UP)).equals(CoverAttachmentBehaviors.DRAIN),
                "the attached cover keeps the drain behaviour, got "
                        + CoverItems.behavior(machine.getCover(Direction.UP)));

        var tank = machine.getTanksInput()[0];
        helper.assertTrue(tank.isEmpty(), "the input tank starts empty, got " + tank.getAmount() + " mB");
        helper.assertTrue(level.getBlockState(water).getFluidState().isSource(),
                "the test placed a water source block, got " + level.getBlockState(water));

        tick(machine, 25);

        helper.assertTrue(tank.getAmount() == CoverAttachmentBehaviors.FLUID_BLOCK_AMOUNT,
                "one water source block fills exactly 1000 mB, got " + tank.getAmount() + " mB");
        helper.assertTrue(tank.getFluid().getFluid() == Fluids.WATER,
                "the tank holds the fluid of the drained block, got " + tank.getFluid().getFluid());
        helper.assertTrue(level.getBlockState(water).getFluidState().isEmpty(),
                "the drained block is gone, got " + level.getBlockState(water));
        helper.succeed();
    }

    /**
     * The same for a GT6 world fluid block: {@code gregtech:seawater} is what GT6's ocean is, so the
     * tank has to end up with seawater and not with water ({@code CoverDrain:139-140} fills
     * {@code FL.Ocean} for the ocean case).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void drainCoverTakesTheFluidOfAGt6WorldBlock(GameTestHelper helper) {
        var seaBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("gregtech:seawater"));
        helper.assertTrue(seaBlock != null && seaBlock != Blocks.AIR && seaBlock instanceof LiquidBlock,
                "the gregtech:seawater world fluid block is registered, got " + seaBlock);
        var seaFluid = seaBlock.defaultBlockState().getFluidState().getType();
        var seaId = ForgeRegistries.FLUIDS.getKey(seaFluid);
        helper.assertTrue(seaId != null && seaId.toString().equals("gregtech:seawater"),
                "the block carries its own fluid, got " + seaId);

        ServerLevel level = helper.getLevel();
        BlockPos pos = site(8, 0);
        var machine = machine(level, pos);
        BlockPos sea = pos.above();
        level.setBlock(sea, seaBlock.defaultBlockState(), 3);
        helper.assertTrue(machine.attachCover(Direction.UP, cover(CoverAttachmentBehaviors.DRAIN)),
                "the machine face accepts the drain cover");

        tick(machine, 25);

        var tank = machine.getTanksInput()[0];
        helper.assertTrue(tank.getFluid().getFluid() == seaFluid,
                "the tank holds seawater, not water, got " + tank.getFluid().getFluid());
        helper.assertTrue(tank.getAmount() == CoverAttachmentBehaviors.FLUID_BLOCK_AMOUNT,
                "the GT6 water block fills 1000 mB, got " + tank.getAmount() + " mB");
        helper.assertTrue(level.getBlockState(sea).getFluidState().isEmpty(),
                "the drained GT6 water block is gone, got " + level.getBlockState(sea));
        helper.succeed();
    }

    /**
     * The vent pours air into the tank, once per 360 tick cycle, in this face's own slot.
     *
     * <p>Three things are asserted and they fail separately: the cadence (nothing before the face's
     * slot, air after it), the fluid ({@code GTFluids.still("Air")}, the overworld variant of
     * {@code CoverVent:48}) and the partial fill — {@code CoverVent} calls {@code FL.fill_}, the
     * lenient variant that returns what actually fit ({@code FL.java:826}), not the all-or-nothing
     * {@code FL.fillAll} ({@code FL.java:833}) — so 256000 mB offered to a smaller tank fills the
     * tank instead of being rejected.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void airVentPullsAirIntoTheTankInItsOwnCycleSlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(16, 0);
        var machine = machine(level, pos);

        var vent = cover(CoverAttachmentBehaviors.AIR_VENT);
        helper.assertTrue(machine.attachCover(Direction.UP, vent), "the machine face accepts the air vent");
        helper.assertTrue(CoverItems.behavior(machine.getCover(Direction.UP)).equals(CoverAttachmentBehaviors.AIR_VENT),
                "the attached cover keeps the vent behaviour, got "
                        + CoverItems.behavior(machine.getCover(Direction.UP)));

        int phase = CoverAttachmentBehaviors.ventPhase(Direction.UP);
        helper.assertTrue(level.getBlockState(pos.above()).isAir(),
                "the test left collectable air in front of the vent, got " + level.getBlockState(pos.above()));
        var tank = machine.getTanksInput()[0];

        tick(machine, phase - 1);
        helper.assertTrue(tank.isEmpty(), "the vent waits for its slot at tick " + phase + " of the "
                + CoverAttachmentBehaviors.VENT_CYCLE + " tick cycle, got " + tank.getAmount() + " mB");

        tick(machine, 1);
        helper.assertTrue(tank.getFluid().getFluid() == GTFluids.still("Air").get(),
                "the vent pulls overworld air, got " + tank.getFluid().getFluid());
        helper.assertTrue(tank.getAmount() == Math.min(CoverAttachmentBehaviors.VENT_AIR_AMOUNT,
                        tank.getCapacity()),
                "256000 mB are offered and the tank takes what fits, got " + tank.getAmount()
                        + " mB of " + tank.getCapacity() + " mB capacity");
        helper.succeed();
    }

    /** GT6 {@code CoverVent:46} + {@code WD.java:399-400}: a colliding block in front stops the vent. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void airVentNeedsCollectableAirInFrontOfIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(32, 0);
        var machine = machine(level, pos);
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(machine.attachCover(Direction.UP, cover(CoverAttachmentBehaviors.AIR_VENT)),
                "the machine face accepts the air vent");

        tick(machine, CoverAttachmentBehaviors.ventPhase(Direction.UP));

        var tank = machine.getTanksInput()[0];
        helper.assertTrue(tank.isEmpty(), "a colliding block in front blocks the intake (WD.java:400 "
                + "hasCollide), got " + tank.getAmount() + " mB");
        helper.succeed();
    }

    /**
     * The dimension table of {@code CoverVent:47-51} and the cycle of {@code CoverVent:45}, asserted
     * directly because a GameTest only ever runs in one dimension — the nether and end rows of the
     * original's switch cannot be reached from a live test body, so they are pinned here instead.
     */
    @GameTest(template = "test_empty")
    public static void ventCycleAndDimensionTableMatchTheOriginal(GameTestHelper helper) {
        helper.assertTrue(
                CoverAttachmentBehaviors.ventAirField(net.minecraft.world.level.Level.OVERWORLD).equals("Air")
                        && CoverAttachmentBehaviors.ventAirField(net.minecraft.world.level.Level.NETHER)
                                .equals("Air_Nether")
                        && CoverAttachmentBehaviors.ventAirField(net.minecraft.world.level.Level.END)
                                .equals("Air_End"),
                "CoverVent:47-51 selects the air by dimension");
        helper.assertTrue(CoverAttachmentBehaviors.VENT_AIR_AMOUNT == 256000,
                "CoverVent:48 offers 256000 mB, got " + CoverAttachmentBehaviors.VENT_AIR_AMOUNT);

        java.util.Set<Integer> slots = new java.util.TreeSet<>();
        for (var side : Direction.values()) slots.add(CoverAttachmentBehaviors.ventPhase(side));
        helper.assertTrue(slots.equals(new java.util.TreeSet<>(java.util.List.of(30, 90, 150, 210, 270, 330))),
                "the six faces own GT6's 30 + 60 * aSide slots of the 360 tick cycle, got " + slots);
        helper.succeed();
    }

    /**
     * GT6 {@code CoverDrain:147} with the tooltip it comes from ({@code CoverDrain:229}, "Collects
     * Fluid Blocks (if not against Gravity)"): a heavy liquid below a cover that faces down is not
     * lifted into the machine.
     *
     * <p>This is the negative half of the water test above, and on its own it would also pass if the
     * drain did nothing at all — which is why the positive case is asserted next to it.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void drainCoverRefusesToLiftAHeavyLiquidAgainstGravity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(24, 0);
        var machine = machine(level, pos);
        BlockPos water = pos.below();
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        helper.assertTrue(machine.attachCover(Direction.DOWN, cover(CoverAttachmentBehaviors.DRAIN)),
                "the machine face accepts the drain cover");

        tick(machine, 25);

        var tank = machine.getTanksInput()[0];
        helper.assertTrue(tank.isEmpty(),
                "a heavy liquid is not drained against gravity, got " + tank.getAmount() + " mB");
        helper.assertTrue(level.getBlockState(water).getFluidState().isSource(),
                "the water under the machine is untouched, got " + level.getBlockState(water));
        helper.succeed();
    }
}
