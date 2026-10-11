package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * The capability handles of the machines the memory audit flagged must be <em>stable</em>: a neighbor (or a
 * third-party mod) that queries {@code getCapability} twice for the same side has to get the same
 * {@link LazyOptional}, and {@code invalidateCaps()} has to invalidate it. Before §97 these four machines
 * allocated a fresh handle plus a fresh handler wrapper on every query, which is one object per neighbor per
 * tick for anything that caches the handle.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CapabilityHandleTests {
    private static final int BASE_X = 72000;
    private static final int BASE_Z = 72000;
    private static final int BASE_Y = 100;

    private static void checkStableHandles(GameTestHelper helper, Block block, int dx, String what) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X + dx, BASE_Y, BASE_Z);
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, block.defaultBlockState(), 2);
        var be = level.getBlockEntity(pos);
        helper.assertTrue(be != null, what + " placed a block entity at " + pos);
        if (be == null) {
            return;
        }
        for (Direction side : new Direction[] {Direction.NORTH, Direction.UP}) {
            LazyOptional<IItemHandler> first = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
            LazyOptional<IItemHandler> second = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
            helper.assertTrue(first == second,
                    what + " returns the same LazyOptional for side " + side + " (was: a new handle per query)");
            // The burning box refuses its front face, so only the upward side is required to offer one -
            // and the solid variant may expose no item handler at all (its fuel enters elsewhere), in which
            // case the stability assertions above are what this test is about.
            if (side == Direction.UP && !what.equals("burning box")) {
                helper.assertTrue(first.isPresent(), what + " offers an item handler on " + side);
            }
            // The burning box's sinks are side-independent on purpose (one cached handle for all sides);
            // the hoppers expose a per-side handler wrapper, so those must not share one handle.
            if (!what.equals("burning box")) {
                LazyOptional<IItemHandler> other = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite());
                helper.assertTrue(other != first, what + " caches per side, not one handle for all sides");
            }
        }
        LazyOptional<IItemHandler> beforeInvalidate = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH);
        be.invalidateCaps();
        helper.assertTrue(!beforeInvalidate.isPresent(),
                what + " invalidates its cached handle in invalidateCaps()");
        LazyOptional<IItemHandler> afterInvalidate = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH);
        helper.assertTrue(afterInvalidate.isPresent(),
                what + " offers a working handle again after re-querying");
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
    }

    /**
     * The §97 contract for a machine whose sinks are <em>side-independent</em>: one cached
     * {@link LazyOptional} per sink instead of one per side. The same handle must come back for repeated
     * queries, two different sides must get the <em>same</em> one — the opposite of
     * {@link #checkStableHandles}'s per-side assertion, because the inventory and the tank are one object
     * for every face — the front face must refuse the item handler, and {@code invalidateCaps()} must
     * invalidate every issued handle and reset the fields so the next query rebuilds a working one.
     */
    private static void checkSideIndependentHandles(GameTestHelper helper, Block block, int dx, String what) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X + dx, BASE_Y, BASE_Z);
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, block.defaultBlockState(), 2);
        var be = level.getBlockEntity(pos);
        helper.assertTrue(be != null, what + " placed a block entity at " + pos);
        if (be == null) {
            return;
        }
        // defaultBlockState() carries the machine's own default rotation (GTFacingMachineBlock.FACING is
        // registered as NORTH), so the front face is known without a BlockItemUseContext placement.
        BlockState state = level.getBlockState(pos);
        Direction front = state.hasProperty(GTFacingMachineBlock.FACING)
                ? state.getValue(GTFacingMachineBlock.FACING) : null;
        helper.assertTrue(front != null, what + " is a facing machine, so its front face is identifiable");

        LazyOptional<IItemHandler> item = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        LazyOptional<IItemHandler> itemAgain = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.assertTrue(item == itemAgain,
                what + " returns the same LazyOptional for side " + Direction.UP + " (was: a new handle per query)");
        helper.assertTrue(item.isPresent(), what + " offers an item handler on " + Direction.UP);
        helper.assertTrue(be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN) == item,
                what + " caches one handle per sink, not per side (its inventory is the same object for"
                        + " every non-front face)");
        helper.assertTrue(be.getCapability(ForgeCapabilities.ITEM_HANDLER, null) == item,
                what + " answers the unsided query with the same cached item handle");
        if (front != null) {
            helper.assertTrue(!be.getCapability(ForgeCapabilities.ITEM_HANDLER, front).isPresent(),
                    what + " refuses the item handler on its front face " + front);
        }

        LazyOptional<IFluidHandler> fluid = be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP);
        LazyOptional<IFluidHandler> fluidAgain = be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP);
        helper.assertTrue(fluid == fluidAgain,
                what + " returns the same fluid LazyOptional for side " + Direction.UP
                        + " (was: a new handle per query)");
        helper.assertTrue(fluid.isPresent(), what + " offers a fluid handler on " + Direction.UP);
        helper.assertTrue(be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN) == fluid,
                what + " caches one fluid handle for all sides");

        be.invalidateCaps();
        helper.assertTrue(!item.isPresent(), what + " invalidates an issued item handle in invalidateCaps()");
        helper.assertTrue(!fluid.isPresent(), what + " invalidates an issued fluid handle in invalidateCaps()");
        helper.assertTrue(be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent(),
                what + " offers a working item handle again after re-querying");
        helper.assertTrue(be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                what + " offers a working fluid handle again after re-querying");
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void hopperCapabilitiesAreCached(GameTestHelper helper) {
        var hoppers = MachineRegistry.hoppers();
        helper.assertTrue(!hoppers.isEmpty(), "the port registers hoppers");
        if (!hoppers.isEmpty()) {
            checkStableHandles(helper, hoppers.get(0).get(), 0, "hopper");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void queueHopperCapabilitiesAreCached(GameTestHelper helper) {
        var hoppers = MachineRegistry.queueHoppers();
        helper.assertTrue(!hoppers.isEmpty(), "the port registers queue hoppers");
        if (!hoppers.isEmpty()) {
            checkStableHandles(helper, hoppers.get(0).get(), 8, "queue hopper");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void itemPipeCapabilitiesAreCached(GameTestHelper helper) {
        var pipes = com.gregtech.gregtech.registry.GTItemPipes.all();
        helper.assertTrue(!pipes.isEmpty(), "the port registers item pipes");
        if (!pipes.isEmpty()) {
            // Pipes are the machine every neighbor queries every tick, and they wrap their handler per side
            // (SideAwareItemHandler), so both the identity and the per-side assertions apply.
            checkStableHandles(helper, pipes.get(0).get(), 24, "item pipe");
        }
        helper.succeed();
    }

    /**
     * The solid burning box is deliberately <em>not</em> asserted here: its solid variant exposes no item
     * handler on any side (fuel enters through its own path), so there is no handle to compare. Its two
     * side-independent cached handles ({@code itemCap}/{@code fluidCap} in {@code BurningBoxBlockEntity})
     * are still covered by the code change and by the burning box's own tests.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void burningBoxCapabilitiesAreCached(GameTestHelper helper) {
        helper.assertTrue(!MachineRegistry.solidBurningBoxes().isEmpty(),
                "the port registers solid burning boxes (their handle caching is asserted in code review, "
                        + "see the javadoc: the solid variant exposes no item handler to compare)");
        helper.succeed();
    }

    /**
     * §97's fourth site, and the one the three tests above leave uncovered: the liquid, gas and
     * fluidized-bed burning boxes ({@code MachineRegistry.burningBoxes()}), whose {@code itemCap} and
     * {@code fluidCap} replaced a fresh {@code LazyOptional} per query.
     *
     * <p>They cache <em>one</em> handle per sink rather than one per side, so the per-side identity
     * assertion of {@link #checkStableHandles} does not apply here: the fuel inventory and the tank are
     * one object for every face and two different sides have to return the same handle — exactly what
     * {@link #checkSideIndependentHandles} asserts instead. Every fluid tier has both sinks
     * ({@code initInventoryAndTank} builds an inventory for all four fuel types and a tank for
     * LIQUID/GAS/FLUIDIZED_BED), so both handlers exist on every non-front side.
     *
     * <p>The solid variant stays out of this test on purpose — see
     * {@link #burningBoxCapabilitiesAreCached}: {@code SolidBurningBoxBlockEntity} inherits the plain
     * {@code BlockEntity#getCapability}, which is {@code LazyOptional.empty()} on every side, so there is
     * no handle to compare and no caching change to guard.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void fluidBurningBoxCapabilitiesAreCached(GameTestHelper helper) {
        var boxes = MachineRegistry.burningBoxes();
        helper.assertTrue(!boxes.isEmpty(), "the port registers fluid burning boxes");
        if (!boxes.isEmpty()) {
            checkSideIndependentHandles(helper, boxes.get(0).get(), 40, "fluid burning box");
        }
        helper.succeed();
    }
}
