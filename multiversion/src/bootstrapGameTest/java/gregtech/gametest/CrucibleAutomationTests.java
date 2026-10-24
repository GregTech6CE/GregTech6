package gregtech.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce_crucible_input")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
import net.minecraftforge.items.IItemHandler;
@net.minecraftforge.gametest.GameTestHolder("gt6ce_crucible_input")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class CrucibleAutomationTests {
    private static IItemHandler handler(GameTestHelper h, BlockPos pos, Direction side) {
//? if >=1.21.1 {
        /*return h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
*///?} else {
        return h.getLevel().getBlockEntity(pos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
//?}
    }

    private static void place(GameTestHelper h, BlockPos pos, String id) {
        h.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id)).defaultBlockState(), 3);
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void topOnlyEmptyCacheAndNoExtraction(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        place(h, pos, "smelting_crucible_ceramic");
        var input = handler(h, pos, Direction.UP);
        h.assertTrue(input != null, "top capability must exist");
        for (var side : Direction.values()) if (side != Direction.UP)
            h.assertTrue(handler(h, pos, side) == null, "no side/bottom item access");
        h.assertTrue(handler(h, pos, null) == null, "no unsided automation bypass");
        var iron = new ItemStack(Items.IRON_INGOT, 8);
        h.assertTrue(input.insertItem(0, iron, true).isEmpty() && input.getStackInSlot(0).isEmpty(), "simulation is read only");
        h.assertTrue(input.insertItem(0, iron, false).isEmpty() && iron.getCount() == 8, "accept batch without mutating source");
        h.assertTrue(input.insertItem(0, iron, false).getCount() == 8, "occupied cache rejects even same material");
        input.getStackInSlot(0).shrink(8);
        h.assertTrue(input.getStackInSlot(0).getCount() == 8, "inspection cannot mutate cache");
        h.assertTrue(input.extractItem(0, 8, true).isEmpty() && input.extractItem(0, 8, false).isEmpty(), "no automated extraction");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void vanillaAndGtHoppersDeliverToCrucible(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        place(h, pos, "smelting_crucible_steel");
        var above = pos.above();
        var vanillaState = Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN);
        h.getLevel().setBlock(above, vanillaState, 3);
        var vanilla = (net.minecraft.world.level.block.entity.HopperBlockEntity) h.getLevel().getBlockEntity(above);
        vanilla.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(h.getLevel(), above, vanillaState, vanilla);
        h.assertTrue(handler(h, pos, Direction.UP).getStackInSlot(0).getCount() == 1 && vanilla.getItem(0).getCount() == 7,
                "vanilla hopper transfers exactly one item through registered capability");
        var gtPos = pos.offset(3, 0, 0);
        place(h, gtPos, "smelting_crucible_steel");
        var gtAbove = gtPos.above();
        var gtState = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "hopper_steel"))
                .defaultBlockState().setValue(gregtech.block.machine.HopperBlock.FACING, Direction.DOWN);
        h.getLevel().setBlock(gtAbove, gtState, 3);
        var gt = (gregtech.blockentity.machine.HopperBlockEntity) h.getLevel().getBlockEntity(gtAbove);
        gt.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        for (int i = 0; i < 10; i++)
            gregtech.blockentity.machine.HopperBlockEntity.serverTick(h.getLevel(), gtAbove, gtState, gt);
        h.assertTrue(handler(h, gtPos, Direction.UP).getStackInSlot(0).getCount() == 8 && gt.getStackInSlot(0).isEmpty(),
                "GT hopper transfers its batch without loss or duplication");
        h.succeed();
    }
}
