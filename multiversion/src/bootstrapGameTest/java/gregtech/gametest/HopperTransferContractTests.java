package gregtech.gametest;

import gregtech.block.machine.HopperBlock;
import gregtech.block.machine.QueueHopperBlock;
import gregtech.blockentity.machine.HopperBlockEntity;
import gregtech.blockentity.machine.QueueHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class HopperTransferContractTests {
    private static ChestBlockEntity chest(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    private static QueueHopperBlockEntity queue(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof QueueHopperBlock).findFirst().orElseThrow();
        h.setBlock(pos.relative(Direction.EAST), Blocks.STONE);
        h.setBlock(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        return (QueueHopperBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    private static void tick(GameTestHelper h, QueueHopperBlockEntity queue) {
        for (int i = 0; i < 4; i++)
            QueueHopperBlockEntity.serverTick(h.getLevel(), queue.getBlockPos(), queue.getBlockState(), queue);
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void queueHopperPullRespectsSelectedItemStackLimit(GameTestHelper h) {
        Item[] items = {Items.IRON_SWORD, Items.ENDER_PEARL, Items.DIRT};
        for (int test = 0; test < items.length; test++) {
            var pos = new BlockPos(1, 1, 1);
            var queue = queue(h, pos);
            var source = chest(h, pos.above());
            int max = new ItemStack(items[test]).getMaxStackSize();
            source.setItem(0, new ItemStack(items[test], max));
            source.setItem(1, new ItemStack(items[test], max));
            var simulated = queue.insertItem(0, new ItemStack(items[test], max), true);
            h.assertTrue(simulated.isEmpty() && queue.getStackInSlot(0).isEmpty(), "public insertion simulation leaves queue unchanged");
            tick(h, queue);
            int queued = 0;
            for (int slot = 0; slot < queue.getSlots(); slot++) {
                var stack = queue.getStackInSlot(slot);
                h.assertTrue(stack.isEmpty() || stack.getCount() <= stack.getMaxStackSize(), "automatic input never overstacks selected item");
                queued += stack.getCount();
            }
            int remaining = source.getItem(0).getCount() + source.getItem(1).getCount();
            h.assertTrue(queued == max && remaining == max && queued + remaining == 2 * max, "one legal FIFO entry pulled and every source item conserved");
            for (int slot = 0; slot < queue.getSlots(); slot++) queue.setStackInSlot(slot, ItemStack.EMPTY);
            source.clearContent();
            h.setBlock(pos.above(), Blocks.AIR);
            h.setBlock(pos, Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void queueHopperPreservesLegacyOversizedContents(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1);
        var queue = queue(h, pos);
        var source = chest(h, pos.above());
        queue.setStackInSlot(0, new ItemStack(Items.ENDER_PEARL, 32));
        source.setItem(0, new ItemStack(Items.ENDER_PEARL));
        tick(h, queue);
        int queued = 0;
        for (int slot = 0; slot < queue.getSlots(); slot++) queued += queue.getStackInSlot(slot).getCount();
        h.assertTrue(queued == 32 && source.getItem(0).getCount() == 1, "legacy oversize is retained without further pull or truncation");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void hopperMinimumBatchRequiresDestinationCapacity(GameTestHelper h) {
        for (int test = 0; test < 2; test++) {
            var pos = new BlockPos(1, 1, 1);
            var block = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof HopperBlock).findFirst().orElseThrow();
            h.setBlock(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
            h.setBlock(pos.above(), Blocks.STONE);
            var hopper = (HopperBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            var target = chest(h, pos.relative(Direction.EAST));
            for (int slot = 0; slot < target.getContainerSize(); slot++) target.setItem(slot, new ItemStack(Items.STONE, 64));
            target.setItem(0, new ItemStack(Items.DIRT, 63));
            for (int i = 0; i < 16; i++) hopper.cycleMode(false);
            if (test == 1) hopper.toggleExactMode();
            h.assertTrue(hopper.getMode() == 16 && hopper.getExactMode() == (test == 1), "actual mode controls configured");
            hopper.setStackInSlot(0, new ItemStack(Items.DIRT, 16));
            for (int i = 0; i < 4; i++) HopperBlockEntity.serverTick(h.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper);
            int stored = 0;
            for (int slot = 0; slot < hopper.getSlots(); slot++) stored += hopper.getStackInSlot(slot).getCount();
            h.assertTrue(stored == 16 && target.getItem(0).getCount() == 63, "one free target item cannot accept a minimum batch of sixteen");
            target.setItem(0, new ItemStack(Items.DIRT, 48));
            // Mark the real source as changed so the native idle-check gate retries.
            hopper.setStackInSlot(0, new ItemStack(Items.DIRT, 16));
            for (int i = 0; i < 4; i++) HopperBlockEntity.serverTick(h.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper);
            stored = 0;
            for (int slot = 0; slot < hopper.getSlots(); slot++) stored += hopper.getStackInSlot(slot).getCount();
            h.assertTrue(stored == 0 && target.getItem(0).getCount() == 64, "sixteen free target items accept exactly the source batch without loss");
            target.clearContent();
            h.setBlock(pos.relative(Direction.EAST), Blocks.AIR);
            h.setBlock(pos, Blocks.AIR);
        }
        h.succeed();
    }
}
