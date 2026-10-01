package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * §111: fluid pipes are cover hosts, the way GT6 has them.
 *
 * <p>In GT6 the pressure valve and the fluid filter are covers of a <b>single-block fluid pipe</b>
 * ({@code CoverPressureValve:44} refuses every host that is not a {@code MultiTileEntityPipeFluid} with
 * exactly one tank, and dereferences {@code mTanks[0]} at {@code :51}). The port only supported machine
 * faces, so a player could not reproduce GT6's valve-on-the-pipe setup at all — §108 recorded that
 * substitution in {@code CoverUtilityBehaviors}' class javadoc. This suite asserts the pipe host
 * itself: attach/remove per face, GT6's placement rule for the valve, per-face fluid interception,
 * the tick-driven covers, and that a pipe with no covers behaves exactly as before.</p>
 *
 * <h2>Why these coordinates</h2>
 *
 * <p>{@link #BASE_X}/{@link #BASE_Z} are {@code 55000 + 4000} — {@code 59000} — which no other suite
 * uses ({@code grep -r "BASE_X" gametest}); the neighbouring reservations are 58000 and 60000. Every
 * site is cleared before use, blocks <em>and</em> entities, so the file is re-runnable.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class PipeCoverTests {
    private static final int BASE_X = 59000;
    private static final int BASE_Y = 100;
    private static final int BASE_Z = 59000;

    /** A single-tank steel pipe: 200 × 24 = 4800 mB, gas-proof (so the vent test may fill it with gas). */
    private static final String PIPE = "pipe_huge_steel";
    /** A four-tank pipe — GT6's valve must refuse it ({@code PipeSpec.PipeSize.QUADRUPLE}). */
    private static final String MULTI_TANK_PIPE = "pipe_quadruple_steel";

    private static BlockPos site(int dx, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y, BASE_Z + dz);
    }

    /** Wipes a site — blocks and entities — so a re-run starts from the same world state. */
    private static void clearSite(ServerLevel level, BlockPos pos, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box(pos, radius))) entity.discard();
    }

    private static AABB box(BlockPos pos, int radius) {
        return new AABB(pos.getX() - radius, pos.getY() - 1, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
    }

    /** Places a pipe of the given id; the caller owns the surrounding clearing. */
    private static FluidPipeBlockEntity setPipe(GameTestHelper helper, BlockPos pos, String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        helper.assertTrue(block != null && block != Blocks.AIR, "gregtech:" + id + " is registered");
        ServerLevel level = helper.getLevel();
        level.setBlock(pos, block.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity,
                "gregtech:" + id + " carries its pipe block entity");
        return (FluidPipeBlockEntity) level.getBlockEntity(pos);
    }

    /** A cleared site with one pipe on it. */
    private static FluidPipeBlockEntity pipe(GameTestHelper helper, BlockPos pos, String id) {
        clearSite(helper.getLevel(), pos, 2);
        return setPipe(helper, pos, id);
    }

    private static ItemStack cover(String id) {
        return new ItemStack(GTTechnological.get(id));
    }

    /** One pipe tick, the way the level calls it. */
    private static void tick(ServerLevel level, BlockPos pos, FluidPipeBlockEntity pipe) {
        FluidPipeBlockEntity.serverTick(level, pos, level.getBlockState(pos), pipe);
    }

    /**
     * Opens one face of a pipe for flow, which is what {@code autoConnectOnPlace} does when a player
     * places a pipe next to another one.
     *
     * <p>{@code distribute} skips every face whose connection property is false
     * ({@code FluidPipeBlockEntity:547}), so a pipe pair placed with {@code setBlock} alone is
     * completely disconnected and nothing can flow — a test that forgets this passes its negative
     * assertions for the wrong reason.</p>
     */
    private static void connect(ServerLevel level, BlockPos pos, Direction side) {
        level.setBlockAndUpdate(pos,
                level.getBlockState(pos).setValue(FluidPipeBlock.propFor(side), true));
    }

    private static IFluidHandler face(FluidPipeBlockEntity pipe, Direction side) {
        return pipe.getCapability(ForgeCapabilities.FLUID_HANDLER, side).resolve().orElse(null);
    }

    // ── the host itself ─────────────────────────────────────────────────────────────

    /**
     * A pipe face takes a cover, dispatches it by the same behaviour table the machines use, keeps the
     * other five faces free, refuses a second cover on an occupied face and gives it back on removal.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aFluidPipeHostsCoversPerFace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FluidPipeBlockEntity pipe = pipe(helper, site(0, 0), PIPE);

        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "a pipe face accepts a cover item");
        helper.assertTrue(CoverUtilityBehaviors.FILTER_FLUID.equals(
                        CoverItems.behavior(pipe.getCover(Direction.EAST))),
                "the face dispatches to the fluid filter behaviour, got "
                        + CoverItems.behavior(pipe.getCover(Direction.EAST)));
        for (Direction side : Direction.values()) {
            if (side == Direction.EAST) continue;
            helper.assertTrue(pipe.getCover(side).isEmpty(),
                    side + " is still free, got " + pipe.getCover(side));
        }
        helper.assertFalse(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "an occupied face refuses a second cover");
        ItemStack removed = pipe.removeCover(Direction.EAST);
        helper.assertTrue(CoverUtilityBehaviors.FILTER_FLUID.equals(CoverItems.behavior(removed))
                        && pipe.getCover(Direction.EAST).isEmpty(),
                "removal hands the cover back and frees the face, got " + removed);
        helper.assertTrue(pipe.panels() != null, "the pipe owns the cover runtime the panels need");
        helper.succeed();
    }

    /**
     * GT6 {@code CoverPressureValve:44}: only a fluid pipe with <b>exactly one tank</b> may host the
     * valve, and not on a face that points at another pipe ({@code valveCanAttachTo}/{@code
     * valveConnectsThrough}). Covers without that rule still attach to such a face.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theValveKeepsGt6sSingleTankPlacementRule(GameTestHelper helper) {
        FluidPipeBlockEntity single = pipe(helper, site(16, 0), PIPE);
        helper.assertTrue(single.spec().tankCount() == 1,
                "a huge pipe holds one tank, got " + single.spec().tankCount());
        helper.assertTrue(single.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "a single-tank pipe takes the valve (CoverPressureValve:44)");

        FluidPipeBlockEntity multi = pipe(helper, site(32, 0), MULTI_TANK_PIPE);
        helper.assertTrue(multi.spec().tankCount() == 4,
                "a quadruple pipe holds four tanks, got " + multi.spec().tankCount());
        helper.assertFalse(multi.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "and the valve refuses anything but a one-tank pipe (:44)");
        helper.assertTrue(multi.getCover(Direction.EAST).isEmpty(), "so that face stays empty");

        // Two touching pipes: the valve may not attach to the face that looks at the other pipe.
        BlockPos first = site(48, 0);
        clearSite(helper.getLevel(), first, 3);
        FluidPipeBlockEntity source = setPipe(helper, first, PIPE);
        setPipe(helper, first.east(), PIPE);
        helper.assertFalse(source.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "a face pointing at another pipe is refused (:44 valveConnectsThrough)");
        helper.assertTrue(source.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "the fluid filter has no such rule and still attaches");
        helper.succeed();
    }

    // ── the per-face fluid interception ─────────────────────────────────────────────

    /**
     * The fluid filter lives on the pipe's face, not on the pipe: the filtered face refuses what the
     * filter refuses while the other five faces keep working — the §108 rule that "no cover" means
     * "permit" applied to a pipe.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theFluidFilterOnAPipeFaceFiltersThatFaceOnly(GameTestHelper helper) {
        FluidPipeBlockEntity pipe = pipe(helper, site(64, 0), PIPE);
        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "the fluid filter attaches to a pipe face");
        helper.assertTrue(pipe.clickFilterCover(Direction.EAST, new ItemStack(Items.LAVA_BUCKET)),
                "the held lava bucket becomes the pipe's fluid filter (CoverFilterFluid:92-114)");

        IFluidHandler east = face(pipe, Direction.EAST);
        IFluidHandler west = face(pipe, Direction.WEST);
        helper.assertTrue(east != null && west != null,
                "both faces still expose a fluid handler, got " + east + " / " + west);
        helper.assertTrue(east.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "water is refused on the filtered face (CoverFilterFluid:117)");
        helper.assertTrue(east.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) > 0,
                "lava passes the lava filter");
        helper.assertTrue(west.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) > 0,
                "and the uncovered face still accepts water — no cover means permit (§108)");
        helper.succeed();
    }

    /**
     * The filter also gates the pipe's own equalization: GT6's fluid filter intercepts the face, so a
     * neighbouring pipe pushing into it must be refused there too — otherwise the cover would only stop
     * machines and buckets while the pipe-to-pipe path walked straight past it.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theFilterAlsoGatesThePipeToPipeFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = site(144, 0);
        clearSite(level, source, 3);
        FluidPipeBlockEntity west = setPipe(helper, source, PIPE);
        FluidPipeBlockEntity east = setPipe(helper, source.east(), PIPE);
        connect(level, source, Direction.EAST);
        connect(level, source.east(), Direction.WEST);
        helper.assertTrue(level.getBlockState(source).getValue(FluidPipeBlock.propFor(Direction.EAST))
                        && level.getBlockState(source.east()).getValue(FluidPipeBlock.propFor(Direction.WEST)),
                "the two pipes are connected to each other, otherwise nothing could flow at all");

        // The receiving pipe's west face only lets lava through.
        helper.assertTrue(east.attachCover(Direction.WEST, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "the receiving pipe takes a fluid filter on the face that looks at its neighbour");
        helper.assertTrue(east.clickFilterCover(Direction.WEST, new ItemStack(Items.LAVA_BUCKET)),
                "the held lava bucket becomes that face's filter");

        helper.assertTrue(west.fill(new FluidStack(Fluids.WATER, 2000),
                        IFluidHandler.FluidAction.EXECUTE) == 2000,
                "the source pipe is filled with water");
        for (int i = 0; i < 3; i++) {
            tick(level, source, west);
            tick(level, source.east(), east);
        }
        helper.assertTrue(east.getFluidInTank(0).isEmpty(),
                "water does not equalise into a lava-filtered face, got " + east.getFluidInTank(0));
        helper.assertTrue(west.getFluidInTank(0).getAmount() == 2000,
                "and the source pipe keeps all of it, got " + west.getFluidInTank(0));

        // Flip the filter to water and the same flow goes through.
        helper.assertTrue(east.configureFilterCover(Direction.WEST, false, true),
                "the soft hammer clears the filter (CoverFilterFluid:67-70)");
        helper.assertTrue(east.clickFilterCover(Direction.WEST, new ItemStack(Items.WATER_BUCKET)),
                "a water bucket becomes the new filter");
        for (int i = 0; i < 3; i++) {
            tick(level, source, west);
            tick(level, source.east(), east);
        }
        helper.assertTrue(east.getFluidInTank(0).getAmount() > 0,
                "water now equalises through the face, got " + east.getFluidInTank(0));
        helper.succeed();
    }

    /**
     * The other half of {@link #theFilterAlsoGatesThePipeToPipeFlow}: a filter gates what passes
     * <b>through</b> its face, so the face the fluid <em>leaves</em> through has to refuse it too —
     * otherwise a player could stop fluid entering a pipe but not leaving it.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theFilterOnTheSendingFaceGatesOutgoingFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = site(160, 0);
        clearSite(level, source, 4);
        FluidPipeBlockEntity west = setPipe(helper, source, PIPE);
        FluidPipeBlockEntity east = setPipe(helper, source.east(), PIPE);
        connect(level, source, Direction.EAST);
        connect(level, source.east(), Direction.WEST);

        // The filter sits on the *sending* pipe's east face and only lets lava through.
        helper.assertTrue(west.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "the sending pipe takes a fluid filter on the face it pushes through");
        helper.assertTrue(west.clickFilterCover(Direction.EAST, new ItemStack(Items.LAVA_BUCKET)),
                "the held lava bucket becomes that face's filter");
        helper.assertTrue(west.fill(new FluidStack(Fluids.WATER, 2000),
                        IFluidHandler.FluidAction.EXECUTE) == 2000,
                "the sending pipe is filled with water");
        for (int i = 0; i < 3; i++) {
            tick(level, source, west);
            tick(level, source.east(), east);
        }
        helper.assertTrue(east.getFluidInTank(0).isEmpty(),
                "water does not leave through a lava-filtered face, got " + east.getFluidInTank(0));
        helper.assertTrue(west.getFluidInTank(0).getAmount() == 2000,
                "and the sender keeps all of it, got " + west.getFluidInTank(0));

        // A second pipe whose filter matches what it carries: the same flow goes through.
        BlockPos other = site(176, 0);
        clearSite(level, other, 4);
        FluidPipeBlockEntity sender = setPipe(helper, other, PIPE);
        FluidPipeBlockEntity receiver = setPipe(helper, other.east(), PIPE);
        connect(level, other, Direction.EAST);
        connect(level, other.east(), Direction.WEST);
        helper.assertTrue(sender.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_FLUID))
                        && sender.clickFilterCover(Direction.EAST, new ItemStack(Items.WATER_BUCKET)),
                "the same face now filters for water");
        helper.assertTrue(sender.fill(new FluidStack(Fluids.WATER, 2000),
                        IFluidHandler.FluidAction.EXECUTE) == 2000, "the sender is filled with water");
        for (int i = 0; i < 3; i++) {
            tick(level, other, sender);
            tick(level, other.east(), receiver);
        }
        helper.assertTrue(receiver.getFluidInTank(0).getAmount() > 0,
                "water that matches the filter still leaves, got " + receiver.getFluidInTank(0));
        helper.succeed();
    }

    /**
     * The regression guard for the whole batch: a pipe with no covers must behave exactly as before —
     * fill, drain and tick — which is the §108.7.3 question asked about the new code path.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aPipeWithoutCoversStillTransfersFluid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(80, 0);
        FluidPipeBlockEntity pipe = pipe(helper, pos, PIPE);

        int filled = pipe.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000,
                "an uncovered pipe still accepts fluid, got " + filled);
        helper.assertTrue(pipe.getFluidInTank(0).getAmount() == 1000,
                "and it is in the pipe's tank, got " + pipe.getFluidInTank(0));
        for (int i = 0; i < 5; i++) tick(level, pos, pipe);
        helper.assertTrue(pipe.getFluidInTank(0).getAmount() == 1000,
                "ticking a coverless pipe leaves its contents alone, got " + pipe.getFluidInTank(0));
        FluidStack drained = pipe.drain(500, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(drained.getAmount() == 500,
                "and it still drains, got " + drained);
        helper.succeed();
    }

    // ── the tick-driven covers ──────────────────────────────────────────────────────

    /**
     * GT6 {@code CoverPressureValve:53-57}: a <b>full</b> tank of <b>gas</b> with open air in front is
     * vented. The pipe supplies its own {@code mTanks[0]} ({@code :51}), which is what makes the pipe
     * host the original one.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theValveOnAPipeVentsAFullTankOfGas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(96, 0);
        FluidPipeBlockEntity pipe = pipe(helper, pos, PIPE);

        var air = GTFluids.still("Air_Nether");
        helper.assertTrue(air != null && air.isPresent(), "gregtech:netherair is registered, got " + air);
        int capacity = (int) pipe.getTankCapacity(0);
        helper.assertTrue(capacity > 0, "the pipe has a capacity, got " + capacity);
        FluidStack gas = new FluidStack(air.get(), capacity);
        helper.assertTrue(pipe.fill(gas, IFluidHandler.FluidAction.EXECUTE) == capacity,
                "the pipe tank starts full of gas");
        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "the single-tank pipe takes the valve (:44)");

        // Nothing stands in front of the east face, so the gas escapes into open air (:57).
        for (int i = 0; i < 6; i++) tick(level, pos, pipe);
        helper.assertTrue(pipe.getFluidInTank(0).isEmpty(),
                "a full tank of gas is vented by the valve (:57), got " + pipe.getFluidInTank(0));
        helper.succeed();
    }

    /**
     * GT6 {@code CoverDrain}: the drain pulls the source block in front of it into the host's tank —
     * on a pipe, that is the pipe's own tank ({@code CoverAttachmentBehaviors.DRAIN_PERIOD} = 20,
     * {@code DRAIN_PHASE} = 5, so the loop below runs past one period).
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void theDrainOnAPipePullsTheSourceBlockInFront(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(112, 0);
        FluidPipeBlockEntity pipe = pipe(helper, pos, PIPE);
        BlockPos source = pos.east();
        level.setBlock(source, Blocks.WATER.defaultBlockState(), 3);
        helper.assertTrue(level.getFluidState(source).isSource(), "the water block starts as a source");

        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverAttachmentBehaviors.DRAIN)),
                "the drain attaches to a pipe face");
        for (int i = 0; i < 30; i++) tick(level, pos, pipe);

        helper.assertTrue(pipe.getFluidInTank(0).getAmount() >= 1000,
                "the drained water landed in the pipe's tank, got " + pipe.getFluidInTank(0));
        helper.assertFalse(level.getFluidState(source).isSource(),
                "and the source block in front is gone, got " + level.getBlockState(source));
        helper.succeed();
    }

    /**
     * Breaking the pipe must give the covers back, the way {@code BasicMachineBlockEntity:1494-1499}
     * drops a machine's covers. Counted one tick later through the whole entity table: a freshly
     * spawned item entity is not visible to a same-tick query (§110).
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aPipeDropsItsCoversWhenBroken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(128, 0);
        FluidPipeBlockEntity pipe = pipe(helper, pos, PIPE);
        helper.assertTrue(pipe.attachCover(Direction.UP, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "the pipe takes an item filter on its top face");
        ItemStack expected = pipe.getCover(Direction.UP).copy();
        helper.assertTrue(!expected.isEmpty(), "the cover was stored, got " + expected);

        level.removeBlock(pos, false);
        AABB area = box(pos, 2);
        helper.runAfterDelay(1, () -> {
            int found = 0;
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity item)) continue;
                if (!area.contains(item.position())) continue;
                if (ItemStack.isSameItemSameTags(item.getItem(), expected)) found += item.getItem().getCount();
            }
            helper.assertTrue(found == 1, "the cover drops when the pipe is broken, found " + found);
            helper.succeed();
        });
    }
}
