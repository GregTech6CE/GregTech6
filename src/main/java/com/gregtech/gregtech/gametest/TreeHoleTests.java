package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.blockentity.TreeHoleBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTToolItems;
import com.gregtech.gregtech.registry.GTTreeHoles;
import com.gregtech.gregtech.registry.GTWoods;
import com.gregtech.gregtech.worldgen.GTTreeShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * Guards GT6's tree holes: the rubber resin hole, the tapped maple and the tapped rainbowood
 * ({@code MultiTileEntityResinHoleRubber} / {@code SapHoleMaple} / {@code SapHoleRainbowood},
 * multi-tile ids 32762/32761/32760).
 *
 * <p>GT6 punches a resin hole into about half of its rubber trunks while the tree grows
 * ({@code BlockTreeSaplingAB} case 0) and lets a player drill the two sap holes into maple and
 * rainbowood logs. A hole only refills while its tree still has enough leaves — GT6 counts the
 * leaves at the canopy positions its own sapling shape placed, which the port now has too (see
 * {@code TreeGrowthTests}). The tests run far outside the test structures, like the other worldgen
 * tests, and clear their sites first because the GameTest world persists between runs.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class TreeHoleTests {
    private static final int BASE_X = 26000;
    private static final int BASE_Z = 26000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 48, BASE_Y, BASE_Z);
    }

    private static void clear(ServerLevel level, BlockPos center, int radius, int height) {
        level.getBlockState(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= height; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), side, pos, false);
    }

    /** Grows a rubber tree (GT6's own shape) and returns the position of its trunk base. */
    private static BlockPos growRubber(ServerLevel level, BlockPos pos, long seed) {
        clear(level, pos, 8, 22);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        GTTreeShapes.grow(level, pos, WoodSpecies.RUBBER, RandomSource.create(seed));
        return pos;
    }

    /** The three holes exist, carry GT6's state and ship their models. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void holesAreRegistered(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        String[] ids = {"resin_hole_rubber", "tapped_maple", "tapped_rainbowood"};
        WoodSpecies[] species = {WoodSpecies.RUBBER, WoodSpecies.MAPLE, WoodSpecies.RAINBOWOOD};
        for (int i = 0; i < ids.length; i++) {
            TreeHoleBlock hole = GTTreeHoles.hole(species[i]);
            if (hole == null) {
                problems.add(ids[i] + ": not registered");
                continue;
            }
            if (ForgeRegistries.BLOCKS.getKey(hole) == null
                    || !ForgeRegistries.BLOCKS.getKey(hole).getPath().equals(ids[i])) {
                problems.add(ids[i] + ": wrong registry id " + ForgeRegistries.BLOCKS.getKey(hole));
            }
            BlockState state = hole.defaultBlockState();
            if (!state.hasProperty(TreeHoleBlock.FACING) || !state.hasProperty(TreeHoleBlock.RESIN)) {
                problems.add(ids[i] + ": needs GT6's facing and resin state");
            }
            if (!GTBlockEntities.TREE_HOLE.get().isValid(state)) {
                problems.add(ids[i] + ": not covered by the tree_hole block entity type");
            }
            for (String asset : new String[]{
                    "assets/gregtech/blockstates/" + ids[i] + ".json",
                    "assets/gregtech/models/block/wood/" + ids[i] + "_hole.json",
                    "assets/gregtech/models/block/wood/" + ids[i] + "_resin.json"}) {
                if (TreeHoleTests.class.getClassLoader().getResource(asset) == null) {
                    problems.add(ids[i] + ": missing " + asset);
                }
            }
        }
        // GT6's yields: the resin item and the two sap fluids.
        helper.assertTrue(GTTreeHoles.hole(WoodSpecies.RUBBER).yieldsItem(), "rubber hands out an item");
        helper.assertTrue(!GTTreeHoles.hole(WoodSpecies.MAPLE).resinFluid().isEmpty(), "maple hands out sap");
        helper.assertTrue(GTTreeHoles.hole(WoodSpecies.MAPLE).resinFluid().getAmount() == 250,
                "GT6 fills 250 mB per tap");
        helper.assertTrue(ForgeRegistries.ITEMS.getKey(GTTreeHoles.hole(WoodSpecies.RUBBER).resinItem().getItem())
                        .getPath().equals("rubber_resin"),
                "GT6's resin item is IL.Resin");
        helper.assertTrue(problems.isEmpty(), "tree holes (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** A grown rubber tree carries exactly one resin hole in its trunk (GT6 case 0). */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void rubberTreesGrowAResinHole(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TreeHoleBlock hole = GTTreeHoles.hole(WoodSpecies.RUBBER);
        List<String> problems = new ArrayList<>();
        int treesWithHole = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos pos = growRubber(level, base(i), 31L + i);
            Block trunk = GTWoods.log(WoodSpecies.RUBBER);
            int holes = 0, logs = 0;
            int holeY = -1;
            for (int dy = 0; dy < 12; dy++) {
                BlockState state = level.getBlockState(pos.above(dy));
                if (state.is(hole)) {
                    holes++;
                    holeY = dy;
                    if (!state.getValue(TreeHoleBlock.FACING).getAxis().isHorizontal()) {
                        problems.add("tree " + i + ": the hole must face a horizontal side");
                    }
                    if (state.getValue(TreeHoleBlock.RESIN)) problems.add("tree " + i + ": a fresh hole starts empty");
                } else if (state.is(trunk)) {
                    logs++;
                }
            }
            if (holes > 1) problems.add("tree " + i + ": " + holes + " resin holes (GT6 places one per tree)");
            if (holes == 1) {
                treesWithHole++;
                // GT6 only punches the hole where at least six trunk blocks remain above it.
                if (holeY < 1) problems.add("tree " + i + ": the hole sits at the very bottom of the trunk");
            }
            if (logs == 0) problems.add("tree " + i + ": the trunk is gone");
        }
        helper.assertTrue(treesWithHole == 4, "every GT6 rubber tree carries a resin hole, got " + treesWithHole + "/4");
        helper.assertTrue(problems.isEmpty(), "rubber resin holes (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** Holes refill only from a healthy tree: GT6's leaf counting decides. */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void holesRefillOnlyFromHealthyTrees(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = growRubber(level, base(10), 77L);
        // My §34 shape puts the hole into the trunk; find it and use its block entity.
        TreeHoleBlock hole = GTTreeHoles.hole(WoodSpecies.RUBBER);
        int holeY = -1;
        for (int dy = 0; dy < 12 && holeY < 0; dy++) {
            if (level.getBlockState(pos.above(dy)).is(hole)) holeY = dy;
        }
        helper.assertTrue(holeY >= 0, "the grown tree has a resin hole to refill");
        BlockPos holePos = pos.above(holeY);
        int healthy = TreeHoleBlockEntity.countTreeLeaves(level, holePos, WoodSpecies.RUBBER);
        helper.assertTrue(healthy > TreeHoleBlockEntity.threshold(WoodSpecies.RUBBER),
                "GT6 counts at least " + (TreeHoleBlockEntity.threshold(WoodSpecies.RUBBER) + 1)
                        + " leaves on a grown rubber tree, counted " + healthy);

        var be = level.getBlockEntity(holePos);
        helper.assertTrue(be instanceof TreeHoleBlockEntity, "the hole has its block entity");
        TreeHoleBlockEntity treeHole = (TreeHoleBlockEntity) be;
        boolean filled = false;
        for (int i = 0; i < 400 && !filled; i++) {
            filled = treeHole.refresh(WoodSpecies.RUBBER);
        }
        helper.assertTrue(filled, "a healthy tree refills the hole (GT6: 10% per 30 s)");
        helper.assertTrue(level.getBlockState(holePos).getValue(TreeHoleBlock.RESIN),
                "the filled state is visible in the block state (GT6 syncs it to the client)");

        // Strip the canopy: GT6's leaf count collapses and the hole stops refilling.
        BlockPos stripped = growRubber(level, base(11), 78L);
        int strippedHoleY = -1;
        for (int dy = 0; dy < 12 && strippedHoleY < 0; dy++) {
            if (level.getBlockState(stripped.above(dy)).is(hole)) strippedHoleY = dy;
        }
        helper.assertTrue(strippedHoleY >= 0, "the second tree has a hole too");
        BlockPos strippedHole = stripped.above(strippedHoleY);
        for (int dy = 4; dy <= 16; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos leaf = stripped.offset(dx, dy, dz);
                    if (level.getBlockState(leaf).is(GTWoods.leaves(WoodSpecies.RUBBER))) {
                        level.setBlock(leaf, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        int strippedLeaves = TreeHoleBlockEntity.countTreeLeaves(level, strippedHole, WoodSpecies.RUBBER);
        helper.assertTrue(strippedLeaves <= TreeHoleBlockEntity.threshold(WoodSpecies.RUBBER),
                "a stripped tree counts " + strippedLeaves + " leaves, GT6's gate is "
                        + TreeHoleBlockEntity.threshold(WoodSpecies.RUBBER));
        TreeHoleBlockEntity strippedBe = (TreeHoleBlockEntity) level.getBlockEntity(strippedHole);
        boolean refilled = false;
        for (int i = 0; i < 400 && !refilled; i++) refilled = strippedBe.refresh(WoodSpecies.RUBBER);
        helper.assertFalse(refilled, "a stripped tree never refills the hole");
        helper.succeed();
    }

    /** Tapping by hand: the resin item, and the sap fluid into the held container. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void harvestingYieldsResinAndSap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Rubber: right-click hands out GT6's resin item.
        BlockPos resinPos = base(20);
        clear(level, resinPos, 2, 3);
        TreeHoleBlock rubber = GTTreeHoles.hole(WoodSpecies.RUBBER);
        level.setBlock(resinPos, rubber.defaultBlockState()
                .setValue(TreeHoleBlock.FACING, Direction.NORTH).setValue(TreeHoleBlock.RESIN, true), 3);
        Player player = helper.makeMockPlayer();
        InteractionResult result = rubber.use(level.getBlockState(resinPos), level, resinPos, player,
                InteractionHand.MAIN_HAND, hit(resinPos, Direction.NORTH));
        helper.assertTrue(result.consumesAction(), "tapping a full hole is an interaction");
        helper.assertTrue(player.getInventory().countItem(ForgeRegistries.ITEMS.getValue(
                        ResourceLocation.fromNamespaceAndPath("gregtech", "rubber_resin"))) > 0,
                "the player receives GT6's rubber resin");
        helper.assertFalse(level.getBlockState(resinPos).getValue(TreeHoleBlock.RESIN),
                "GT6 extractResin() empties the hole");
        helper.assertTrue(rubber.use(level.getBlockState(resinPos), level, resinPos, player,
                InteractionHand.MAIN_HAND, hit(resinPos, Direction.NORTH)) == InteractionResult.PASS,
                "an empty hole does nothing");

        // Maple: the sap goes into whatever container the player holds.
        BlockPos sapPos = base(21);
        clear(level, sapPos, 2, 3);
        TreeHoleBlock maple = GTTreeHoles.hole(WoodSpecies.MAPLE);
        level.setBlock(sapPos, maple.defaultBlockState()
                .setValue(TreeHoleBlock.FACING, Direction.NORTH).setValue(TreeHoleBlock.RESIN, true), 3);
        ItemStack container = containerAccepting(maple.resinFluid());
        helper.assertTrue(!container.isEmpty(), "the port has a container that takes GT6's maple sap");
        Player tapper = helper.makeMockPlayer();
        tapper.setItemInHand(InteractionHand.MAIN_HAND, container);
        maple.use(level.getBlockState(sapPos), level, sapPos, tapper,
                InteractionHand.MAIN_HAND, hit(sapPos, Direction.NORTH));
        ItemStack filled = tapper.getItemInHand(InteractionHand.MAIN_HAND);
        FluidStack sap = filled.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve()
                .map(handler -> handler.drain(1000, IFluidHandlerItem.FluidAction.SIMULATE))
                .orElse(FluidStack.EMPTY);
        helper.assertTrue(!sap.isEmpty() && sap.getAmount() == 250,
                "GT6 fills 250 mB of sap into " + ForgeRegistries.ITEMS.getKey(container.getItem())
                        + ", got " + sap.getAmount() + " mB");
        helper.assertTrue(sap.getFluid() == GTTreeHoles.hole(WoodSpecies.MAPLE).resinFluid().getFluid(),
                "the sap is maple sap");
        helper.assertFalse(level.getBlockState(sapPos).getValue(TreeHoleBlock.RESIN), "the maple hole is emptied");
        helper.succeed();
    }

    /** Drilling a maple or rainbowood log with a hand drill taps it (GT6 BlockTreeLogA/B). */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void drillingTapsMapleAndRainbowood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        for (WoodSpecies species : List.of(WoodSpecies.MAPLE, WoodSpecies.RAINBOWOOD)) {
            BlockPos pos = base(species == WoodSpecies.MAPLE ? 30 : 31);
            clear(level, pos, 2, 3);
            Block log = GTWoods.log(species);
            TreeHoleBlock hole = GTTreeHoles.hole(species);
            level.setBlock(pos, log.defaultBlockState(), 3);

            // A wrong tool must not tap the tree.
            Player bare = helper.makeMockPlayer();
            bare.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
            log.use(level.getBlockState(pos), level, pos, bare, InteractionHand.MAIN_HAND, hit(pos, Direction.NORTH));
            if (!level.getBlockState(pos).is(log)) problems.add(species + ": a stick must not tap the log");

            // A hand drill does.
            Player driller = helper.makeMockPlayer();
            driller.setItemInHand(InteractionHand.MAIN_HAND, GTToolItems.empty(GTToolType.HAND_DRILL));
            log.use(level.getBlockState(pos), level, pos, driller, InteractionHand.MAIN_HAND, hit(pos, Direction.NORTH));
            BlockState tapped = level.getBlockState(pos);
            if (!tapped.is(hole)) {
                problems.add(species + ": drilling did not tap the log (" + tapped + ")");
            } else {
                if (tapped.getValue(TreeHoleBlock.FACING) != Direction.NORTH) {
                    problems.add(species + ": the hole must face the drilled side");
                }
                if (tapped.getValue(TreeHoleBlock.RESIN)) problems.add(species + ": a fresh hole starts empty");
            }
        }
        // GT6 only taps horizontal sides.
        BlockPos top = base(32);
        clear(level, top, 2, 3);
        level.setBlock(top, GTWoods.log(WoodSpecies.MAPLE).defaultBlockState(), 3);
        Player driller = helper.makeMockPlayer();
        driller.setItemInHand(InteractionHand.MAIN_HAND, GTToolItems.empty(GTToolType.HAND_DRILL));
        GTWoods.log(WoodSpecies.MAPLE).use(level.getBlockState(top), level, top, driller,
                InteractionHand.MAIN_HAND, hit(top, Direction.UP));
        helper.assertTrue(level.getBlockState(top).is(GTWoods.log(WoodSpecies.MAPLE)),
                "drilling the top of a log does nothing (GT6: SIDES_HORIZONTAL)");
        helper.assertTrue(problems.isEmpty(), "drilling (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** The first registered portable fluid container that accepts this GT6 sap (the {@code fluid_*} items). */
    private static ItemStack containerAccepting(FluidStack sap) {
        for (ItemStack stack : containerCandidates()) {
            IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
            if (handler == null) continue;
            if (handler.fill(sap, IFluidHandlerItem.FluidAction.SIMULATE) >= sap.getAmount()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> containerCandidates() {
        List<ItemStack> out = new ArrayList<>();
        for (var entry : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(entry);
            if (id == null || !id.getNamespace().equals("gregtech")) continue;
            if (!id.getPath().startsWith("fluid_")) continue;
            out.add(new ItemStack(entry));
        }
        return out;
    }
}
