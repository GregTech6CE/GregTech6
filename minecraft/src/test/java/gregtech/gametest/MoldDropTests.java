package gregtech.gametest;

import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.MoldItemData;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;

/** Harvester drops pass through cover stripping, which must not erase the carved cavity mask. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MoldDropTests {
    private static final String BATCH = "mold_drop";

    private MoldDropTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void ordinaryDropsKeepTheCarvedShape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        Block block = BuiltInRegistries.BLOCK.stream().filter(candidate -> candidate instanceof MoldBlock)
                .findFirst().orElseThrow(() -> new IllegalStateException("no mould block is registered"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        if (!(level.getBlockEntity(pos) instanceof MoldBlockEntity mold)) {
            helper.fail("mould block entity missing");
            return;
        }
        int shape = MoldBlockEntity.randomDungeonShape(level.random);
        helper.assertTrue(mold.setDungeonShape(shape), "carved mask is a registered casting shape");

        BlockState state = level.getBlockState(pos);
        List<ItemStack> toolDrops = Block.getDrops(state, level, pos, mold, null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(toolDrops.size() == 1, "the mould drops exactly one host item");
        helper.assertTrue(MoldItemData.shape(toolDrops.get(0)) == shape,
                "breaking with a pickaxe keeps the carved mask");

        List<ItemStack> bareDrops = Block.getDrops(state, level, pos, mold);
        helper.assertTrue(bareDrops.size() == 1 && MoldItemData.shape(bareDrops.get(0)) == shape,
                "tool-free drops keep the carved mask");

        helper.succeed();
    }
}
