package gregtech.gametest;

import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.content.registry.GTWoods;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.List;

/** A GT6 grafter (branch cutter) converts leaves into the species sapling instead of the 1/50 roll. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class WoodLeafDropTests {
    private static final String BATCH = "wood_leaf_drop";
    private static final int BRANCH_CUTTER_ROLLS = 8;

    private WoodLeafDropTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void branchCutterYieldsRubberSapling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState state = GTWoods.leaves(WoodSpecies.RUBBER).defaultBlockState();
        level.setBlockAndUpdate(pos, state);

        ItemStack branchCutter = GTToolItem.create(
                GTToolType.BRANCH_CUTTER, GT6Materials.Compounds.Steel, GT6Materials.Woods.Spruce);
        helper.assertTrue(!branchCutter.isEmpty(), "the branch cutter item is registered");
        ItemStack sapling = new ItemStack(GTWoods.sapling(WoodSpecies.RUBBER));

        for (int i = 0; i < BRANCH_CUTTER_ROLLS; i++) {
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, branchCutter.copy());
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(sapling.getItem()),
                    "a branch cutter always drops the species sapling, never the leaf or a stick");
        }
        helper.succeed();
    }
}
