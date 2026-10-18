package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.SapBagBlock;
import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.blockentity.SapBagBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTToolBlocks;
import com.gregtech.gregtech.registry.GTTreeHoles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards GT6's sap bag ({@code MultiTileEntitySapBag}): a bag hung on the side of a tree hole that
 * empties the hole into its 8000 mB tank (or its one item slot, for the rubber hole which yields an
 * item) and hands the contents to a player on right-click.
 *
 * <p>§36 left this as a documented gap ("the sap bag is not ported yet, harvest by hand"); these
 * tests cover the automated path. The bag collects on its block entity tick, so the assertions run a
 * few ticks after placement.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class SapBagTests {
    private static final int BASE_X = 28000;
    private static final int BASE_Z = 28000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 16, BASE_Y, BASE_Z);
    }

    private static void clear(ServerLevel level, BlockPos center) {
        level.getBlockState(center);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), side, pos, false);
    }

    /**
     * Places a full hole at {@code holePos} and a sap bag hanging on its {@code holeFacing} side, so
     * the bag faces back at the hole (GT6 {@code getAdjacentTileEntity(mFacing)}).
     *
     * @return the position of the bag
     */
    private static BlockPos placePair(ServerLevel level, BlockPos holePos, WoodSpecies species, Direction holeFacing) {
        clear(level, holePos.east());
        // The bag works through its block entity tick, so the chunk has to be a ticking one; the
        // GameTest area itself ticks, but these positions are far outside it.
        level.setChunkForced(holePos.getX() >> 4, holePos.getZ() >> 4, true);
        TreeHoleBlock hole = GTTreeHoles.hole(species);
        level.setBlock(holePos, hole.defaultBlockState()
                .setValue(TreeHoleBlock.FACING, holeFacing)
                .setValue(TreeHoleBlock.RESIN, true), 3);
        BlockPos bagPos = holePos.relative(holeFacing);
        SapBagBlock bag = (SapBagBlock) GTToolBlocks.SAP_BAG.get();
        level.setBlock(bagPos, bag.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, holeFacing.getOpposite()), 3);
        return bagPos;
    }

    /**
     * GT6's bag collects on its tick; the test also drives the tick method directly so the
     * assertions stay deterministic when a far-away chunk is not ticking (the second call is a
     * no-op once the hole is empty).
     */
    private static void tickBag(ServerLevel level, BlockPos bagPos) {
        if (level.getBlockEntity(bagPos) instanceof SapBagBlockEntity bag) bag.collect();
    }

    /** The bag block is the real thing now: a block entity, an 8000 mB tank and a ticker. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bagIsRegisteredAsABlockEntity(GameTestHelper helper) {
        Block block = GTToolBlocks.SAP_BAG.get();
        helper.assertTrue(block instanceof SapBagBlock, "sap_bag is a SapBagBlock, got " + block.getClass());
        BlockPos pos = base(0);
        clear(helper.getLevel(), pos);
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof SapBagBlockEntity,
                "placing it creates the SapBagBlockEntity");
        helper.assertTrue(GTBlockEntities.SAP_BAG.get().isValid(block.defaultBlockState()),
                "the sap_bag block entity type covers the block");
        helper.assertTrue(((SapBagBlock) block).getTicker(helper.getLevel(), block.defaultBlockState(),
                        GTBlockEntities.SAP_BAG.get()) != null,
                "the bag ticks on the server (GT6 checks the hole every tick)");
        helper.assertTrue(SapBagBlockEntity.CAPACITY == 8000, "GT6's bag holds 8000 mB");
        var handler = helper.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER).resolve();
        helper.assertTrue(handler.isPresent(), "the bag exposes IFluidHandler so pipes can drain it");
        helper.assertTrue(handler.get().getTankCapacity(0) == 8000, "capacity through the capability");
        helper.succeed();
    }

    /** The bag drains the hole it faces: fluid for the sap holes, the item for the rubber hole. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bagCollectsFromTheHoleItFaces(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mapleHole = base(10);
        BlockPos mapleBag = placePair(level, mapleHole, WoodSpecies.MAPLE, Direction.EAST);
        BlockPos rubberHole = base(11);
        BlockPos rubberBag = placePair(level, rubberHole, WoodSpecies.RUBBER, Direction.EAST);
        // A hole on another side must be ignored: the bag faces west, so the hole behind it stays full.
        BlockPos behind = base(12);
        BlockPos behindBag = placePair(level, behind, WoodSpecies.MAPLE, Direction.EAST);
        BlockPos otherHole = behindBag.east();
        level.setBlock(otherHole, GTTreeHoles.hole(WoodSpecies.MAPLE).defaultBlockState()
                .setValue(TreeHoleBlock.FACING, Direction.WEST).setValue(TreeHoleBlock.RESIN, true), 3);

        helper.runAfterDelay(4, () -> {
            tickBag(level, mapleBag);
            tickBag(level, rubberBag);
            tickBag(level, behindBag);
            List<String> problems = new ArrayList<>();
            var maple = (SapBagBlockEntity) level.getBlockEntity(mapleBag);
            if (maple == null) {
                problems.add("no bag block entity at " + mapleBag);
            } else {
                if (level.getBlockState(mapleHole).getValue(TreeHoleBlock.RESIN)) {
                    problems.add("the bag did not empty the hole it faces");
                }
                FluidStack sap = maple.tank().getFluid();
                if (sap.getAmount() != 250) problems.add("collected " + sap.getAmount() + " mB, GT6 taps 250");
                if (sap.getFluid() != GTTreeHoles.hole(WoodSpecies.MAPLE).resinFluid().getFluid()) {
                    problems.add("collected the wrong fluid: " + sap.getFluid());
                }
                if (!maple.stored().isEmpty()) problems.add("a sap hole must not fill the item slot");
            }
            var rubber = (SapBagBlockEntity) level.getBlockEntity(rubberBag);
            if (rubber == null) {
                problems.add("no bag block entity for the rubber hole");
            } else {
                if (rubber.stored().isEmpty()) problems.add("the rubber hole's resin item did not land in the bag");
                if (rubber.tank().getAmount() != 0) problems.add("the rubber hole must not fill the tank");
                if (level.getBlockState(rubberHole).getValue(TreeHoleBlock.RESIN)) {
                    problems.add("the rubber hole stayed full");
                }
            }
            if (level.getBlockState(otherHole).getValue(TreeHoleBlock.RESIN) == false) {
                problems.add("the bag drained a hole it does not face");
            }
            helper.assertTrue(problems.isEmpty(), "sap bag collection (" + problems.size() + "): "
                    + problems.subList(0, Math.min(5, problems.size())));
            helper.succeed();
        });
    }

    /** Right-clicking the bag hands out the item, then fills the held container from the tank. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bagHandsOutItsContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SapBagBlock bag = (SapBagBlock) GTToolBlocks.SAP_BAG.get();
        BlockPos rubberBag = placePair(level, base(20), WoodSpecies.RUBBER, Direction.EAST);
        BlockPos mapleBag = placePair(level, base(21), WoodSpecies.MAPLE, Direction.EAST);

        helper.runAfterDelay(4, () -> {
            tickBag(level, rubberBag);
            tickBag(level, mapleBag);
            // Rubber resin in the slot.
            var be = (SapBagBlockEntity) level.getBlockEntity(rubberBag);
            helper.assertTrue(be != null && !be.stored().isEmpty(), "the bag collected the resin item");
            Player player = helper.makeMockPlayer();
            bag.use(level.getBlockState(rubberBag), level, rubberBag, player, InteractionHand.MAIN_HAND,
                    hit(rubberBag, Direction.EAST));
            helper.assertTrue(be.stored().isEmpty(), "the item was handed out");
            helper.assertTrue(player.getInventory().countItem(ForgeRegistries.ITEMS.getValue(
                            ResourceLocation.fromNamespaceAndPath("gregtech", "rubber_resin"))) > 0,
                    "the player received the resin item");

            // Sap in the tank goes into the container the player holds.
            var maple = (SapBagBlockEntity) level.getBlockEntity(mapleBag);
            helper.assertTrue(maple != null && maple.tank().getAmount() == 250, "the bag collected 250 mB of sap");
            ItemStack container = containerAccepting(maple.tank().getFluid());
            helper.assertTrue(!container.isEmpty(), "a container that takes GT6's sap exists");
            Player tapper = helper.makeMockPlayer();
            tapper.setItemInHand(InteractionHand.MAIN_HAND, container);
            InteractionResult result = bag.use(level.getBlockState(mapleBag), level, mapleBag, tapper,
                    InteractionHand.MAIN_HAND, hit(mapleBag, Direction.EAST));
            helper.assertTrue(result.consumesAction(), "tapping a bag with a container is an interaction");
            helper.assertTrue(maple.tank().getAmount() == 0, "the bag was drained into the container");
            FluidStack filled = tapper.getItemInHand(InteractionHand.MAIN_HAND)
                    .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve()
                    .map(handler -> handler.drain(1000, IFluidHandlerItem.FluidAction.SIMULATE))
                    .orElse(FluidStack.EMPTY);
            helper.assertTrue(filled.getAmount() == 250, "the container holds 250 mB, got " + filled.getAmount());
            helper.succeed();
        });
    }

    /** Breaking the bag keeps GT6's behaviour: the tank is trashed, the resin item drops. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void breakingTheBagTrashesTheTankAndDropsTheItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bagPos = placePair(level, base(30), WoodSpecies.RUBBER, Direction.EAST);
        helper.runAfterDelay(4, () -> {
            tickBag(level, bagPos);
            var be = (SapBagBlockEntity) level.getBlockEntity(bagPos);
            helper.assertTrue(be != null && !be.stored().isEmpty(), "the bag has the resin item");
            Player player = helper.makeMockPlayer();
            BlockState state = level.getBlockState(bagPos);
            state.getBlock().playerWillDestroy(level, bagPos, state, player);
            helper.assertTrue(be.stored().isEmpty(), "the stored item left the bag");
            level.removeBlock(bagPos, false);
            helper.assertTrue(level.getBlockState(bagPos).isAir(), "the bag is gone");
            helper.succeed();
        });
    }

    /** The first portable fluid container that accepts this GT6 sap. */
    private static ItemStack containerAccepting(FluidStack sap) {
        for (var item : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getNamespace().equals("gregtech") || !id.getPath().startsWith("fluid_")) continue;
            ItemStack stack = new ItemStack(item);
            IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
            if (handler == null) continue;
            if (handler.fill(sap, IFluidHandlerItem.FluidAction.SIMULATE) >= sap.getAmount()) return stack;
        }
        return ItemStack.EMPTY;
    }
}
