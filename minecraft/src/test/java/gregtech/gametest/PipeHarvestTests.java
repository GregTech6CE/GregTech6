package gregtech.gametest;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.BlockHarvestPolicy;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * A pipe's material tool quality is its harvest level, so a manual wrench below that quality digs
 * and harvests nothing (GT6 {@code MultiItemTool.getDigSpeed}).
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class PipeHarvestTests {
    private static final String BATCH = "pipe_harvest";

    private PipeHarvestTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void wrenchBelowPipeMaterialQualityCannotHarvestPipes(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack steel = wrench(GT6Materials.Compounds.Steel);
        ItemStack copper = wrench(GT6Materials.Elements.Cu);
        helper.assertTrue(GTToolHelper.getHarvestLevel(steel) == 2,
                "the steel wrench fixture no longer has quality 2");
        helper.assertTrue(GTToolHelper.getHarvestLevel(copper) < 2,
                "the copper wrench fixture no longer undercuts steel");

        BlockState gaia = place(helper, pos, "pipe_medium_gaia_spirit");
        helper.assertTrue(BlockHarvestPolicy.level(gaia.getBlock()) == 4,
                "the gaia spirit pipe does not report its material quality");
        assertDenied(helper, pos, steel, "a steel wrench on a gaia spirit pipe");

        BlockState vibranium = place(helper, pos, "item_pipe_medium_vibranium_silver");
        assertDenied(helper, pos, steel, "a steel wrench on a vibranium silver item pipe");

        place(helper, pos, "pipe_medium_steel");
        assertDenied(helper, pos, copper, "a copper wrench on a steel pipe");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void wrenchAtPipeMaterialQualityStillHarvestsPipes(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));

        place(helper, pos, "pipe_medium_gaia_spirit");
        assertHarvests(helper, pos, wrench(GT6Materials.Compounds.GaiaSpirit),
                "a gaia spirit wrench on a gaia spirit pipe");

        place(helper, pos, "pipe_medium_steel");
        assertHarvests(helper, pos, wrench(GT6Materials.Compounds.Steel),
                "a steel wrench on a steel pipe");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void woodenAndRubberPipePathsKeepTheirTools(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));

        BlockState wood = place(helper, pos, "pipe_medium_wood");
        helper.assertTrue(BlockHarvestPolicy.level(wood.getBlock()) == 0,
                "a wooden pipe is not a wrench-quality target");
        assertHarvests(helper, pos, new ItemStack(Items.IRON_AXE), "an iron axe on a wooden pipe");

        BlockState rubber = place(helper, pos, "pipe_medium_rubber");
        helper.assertTrue(BlockHarvestPolicy.level(rubber.getBlock()) == 0,
                "a rubber pipe is not a wrench-quality target");
        assertHarvests(helper, pos, new ItemStack(Items.SHEARS), "shears on a rubber pipe");
        helper.succeed();
    }

    private static BlockState place(GameTestHelper helper, BlockPos pos, String path) {
        Block block = BuiltInRegistries.BLOCK.get(id(path));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return helper.getLevel().getBlockState(pos);
    }

    private static void assertDenied(GameTestHelper helper, BlockPos pos, ItemStack tool, String what) {
        Player player = playerWith(helper, tool);
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertFalse(state.canHarvestBlock(helper.getLevel(), pos, player),
                what + " still harvests the pipe");
        helper.assertTrue(state.getDestroyProgress(player, helper.getLevel(), pos) == 0.0F,
                what + " still makes breaking progress on the pipe");
    }

    private static void assertHarvests(GameTestHelper helper, BlockPos pos, ItemStack tool, String what) {
        Player player = playerWith(helper, tool);
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.canHarvestBlock(helper.getLevel(), pos, player),
                what + " no longer harvests the pipe");
        helper.assertTrue(state.getDestroyProgress(player, helper.getLevel(), pos) > 0.0F,
                what + " makes no breaking progress on the pipe");
    }

    private static Player playerWith(GameTestHelper helper, ItemStack tool) {
//? if >=1.21.1 {
        /*Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        Player player = helper.makeMockPlayer();
//?}
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private static ItemStack wrench(GTMaterial head) {
        return GTToolItem.create(GTToolType.WRENCH, head, GT6Materials.Woods.Wood);
    }

    private static ResourceLocation id(String path) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return new ResourceLocation("gt6ce", path);
//?}
    }
}
