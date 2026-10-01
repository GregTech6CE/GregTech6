package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.BarsBlock;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6 bars' connection bits, collision and segment drops. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BarsRepairTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @GameTest(template = "test_empty")
    public static void brassAndSteelHaveGt6SegmentStatesAndDrops(GameTestHelper h) {
        var brass = GTDecorBlocks.BARS_BRASS.get();
        var steel = GTDecorBlocks.BARS_STEEL.get();
        h.assertTrue(BarsBlock.faceBit(Direction.NORTH) == 1
                        && BarsBlock.faceBit(Direction.SOUTH) == 2
                        && BarsBlock.faceBit(Direction.WEST) == 4
                        && BarsBlock.faceBit(Direction.EAST) == 8,
                "connection bits match GT6 metadata");
        h.assertTrue(BarsBlock.placementBit(Direction.UP, 0.25, 0.25) == 1
                        && BarsBlock.placementBit(Direction.UP, 0.25, 0.75) == 2
                        && BarsBlock.placementBit(Direction.WEST, 0.25, 0.75) == 2,
                "top and side placement select the corresponding bar segment");
        var state = brass.defaultBlockState().setValue(BarsBlock.MASK, 3);
        h.setBlock(POS, state);
        var drops = Block.getDrops(state, h.getLevel(), h.absolutePos(POS), null);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(brass.asItem())
                        && drops.get(0).getCount() == 2,
                "two-segment GT6 rail drops two bars");
        var shape = state.getCollisionShape(h.getLevel(), h.absolutePos(POS), CollisionContext.empty());
        h.assertTrue(shape.toAabbs().size() == 2
                        && shape.bounds().minZ == 0 && shape.bounds().maxZ == 1,
                "opposing north/south segments form two thin collision planes");
        h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && steel.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "bars are mineable with a pickaxe");
        for (String material : new String[]{"brass", "steel"}) {
            var recipe = h.getLevel().getRecipeManager().byKey(GregTech.id("hand/bars/" + material));
            h.assertTrue(recipe.isPresent() && recipe.get() instanceof ToolShapedRecipe tool
                            && tool.allowMirror() && tool.getResultItem(h.getLevel().registryAccess()).getCount() == 3,
                    "GT6 " + material + " bars have their six-stick, mirrored-tool survival recipe");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void repeatedUseAddsSegmentsWithoutReplacingTheBlock(GameTestHelper h) {
        var brass = GTDecorBlocks.BARS_BRASS.get();
        var pos = h.absolutePos(POS);
        h.setBlock(POS, brass.defaultBlockState().setValue(BarsBlock.MASK, 1));
        var player = h.makeMockSurvivalPlayer();
        ItemStack held = new ItemStack(brass, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        double[][] hits = {{.25, .75}, {.1, .3}, {.8, .5}};
        int[] masks = {3, 7, 15};
        for (int i = 0; i < hits.length; i++) {
            var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(hits[i][0], 1, hits[i][1]),
                    Direction.UP, pos, false);
            h.assertTrue(held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit))
                    .consumesAction(), "bar segment " + i + " joins the existing block");
            h.assertTrue(h.getLevel().getBlockState(pos).is(brass)
                            && h.getLevel().getBlockState(pos).getValue(BarsBlock.MASK) == masks[i]
                            && held.getCount() == 3 - i,
                    "joining one segment consumes exactly one bar and preserves earlier segments");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fullBarsContinueIntoAdjacentCellOrJoinAdjacentBars(GameTestHelper h) {
        var brass = GTDecorBlocks.BARS_BRASS.get();
        var pos = h.absolutePos(POS);
        var east = pos.east();
        h.setBlock(POS, brass.defaultBlockState().setValue(BarsBlock.MASK, 15));
        var player = h.makeMockSurvivalPlayer();
        ItemStack held = new ItemStack(brass, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);

        var first = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(1, .5, .25),
                Direction.EAST, pos, false);
        h.assertTrue(held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, first))
                        .consumesAction(), "clicking a full bar extends to an empty adjacent cell");
        h.assertTrue(h.getLevel().getBlockState(east).is(brass)
                        && h.getLevel().getBlockState(east).getValue(BarsBlock.MASK) == 1
                        && held.getCount() == 2,
                "first extension places a north segment and consumes one bar");

        var second = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(1, .5, .75),
                Direction.EAST, pos, false);
        h.assertTrue(held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, second))
                        .consumesAction(), "clicking a full bar can add to adjacent partial bars");
        h.assertTrue(h.getLevel().getBlockState(east).getValue(BarsBlock.MASK) == 3
                        && held.getCount() == 1,
                "second extension adds south without replacing the north segment");

        h.getLevel().setBlock(pos, brass.defaultBlockState().setValue(BarsBlock.MASK, 13), 3);
        h.getLevel().setBlock(pos.north(), Blocks.AIR.defaultBlockState(), 3);
        var blocked = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.25, .5, 0),
                Direction.NORTH, pos, false);
        h.assertTrue(!held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, blocked))
                        .consumesAction(), "an occupied segment of a non-full bar does not extend");
        h.assertTrue(h.getLevel().getBlockState(pos.north()).isAir() && held.getCount() == 1,
                "non-full bars leave the neighboring cell and held item untouched");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void heldBarOutlineAndMaterialHarvestMatchGt6(GameTestHelper h) {
        var brass = GTDecorBlocks.BARS_BRASS.get();
        var pos = h.absolutePos(POS);
        var state = brass.defaultBlockState().setValue(BarsBlock.MASK, 1);
        h.setBlock(POS, state);
        var player = h.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(brass));
        var context = CollisionContext.of(player);
        h.assertTrue(state.getShape(h.getLevel(), pos, context).bounds().getZsize() == 1.0,
                "holding the same bars expands their targeting outline to the full cell");
        h.assertTrue(state.getCollisionShape(h.getLevel(), pos, context).bounds().getZsize() == .125,
                "held-item targeting does not enlarge the physical collision plane");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GTDecorBlocks.BARS_STEEL.get()));
        h.assertTrue(state.getShape(h.getLevel(), pos, CollisionContext.of(player))
                        .bounds().getZsize() == .0625,
                "holding a different bar material leaves the selection outline thin");

        BarsBlock[] blocks = {GTDecorBlocks.BARS_BRASS.get(), GTDecorBlocks.BARS_IRON.get(),
                GTDecorBlocks.BARS_BRONZE.get(), GTDecorBlocks.BARS_WROUGHT_IRON.get(),
                GTDecorBlocks.BARS_STEEL.get(), GTDecorBlocks.BARS_STAINLESS.get(),
                GTDecorBlocks.BARS_TUNGSTEN_STEEL.get()};
        int[] qualities = {1, 2, 2, 2, 2, 2, 4};
        for (int index = 0; index < blocks.length; index++) {
            BarsBlock block = blocks[index];
            h.assertTrue(block.harvestLevel() == qualities[index]
                            && BlockHarvestPolicy.level(block) == qualities[index]
                            && com.gregtech.gregtech.api.tool.GTToolHelper.requiredHarvestLevel(
                                    block.defaultBlockState()) == qualities[index],
                    block.materialName() + " bars require the GT6 material tool quality");
        }
        h.assertTrue(GTDecorBlocks.BARS_STEEL.get().getExplosionResistance() == 8f
                        && GTDecorBlocks.BARS_TUNGSTEN_STEEL.get().getExplosionResistance() == 16f,
                "steel and tungstensteel bars retain GT6 blast resistance");
        h.succeed();
    }
}
