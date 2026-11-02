package gregtech.gametest;

import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.transport.ItemPipeNetwork;
import gregtech.content.transport.ItemPipeNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Routing cache lifetime and delivery scheduling against real inventories and pipe entities. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ItemPipeRoutingTests {
    private static final String BATCH = "item_pipe_routing";

    private ItemPipeRoutingTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void saturatedNearExitContinuesTheCachedSearch(GameTestHelper helper) {
        Container near = placeChest(helper, 1);
        ItemPipeBlockEntity source = placePipe(helper, 2, "electrum");
        ItemPipeBlockEntity middle = placePipe(helper, 3, "electrum");
        ItemPipeBlockEntity exit = placePipe(helper, 4, "electrum");
        Container far = placeChest(helper, 5);
        helper.runAfterDelay(2, () -> {
            ItemPipeNetwork network = ItemPipeNetworks.networkOf(source);
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 11), false);
            helper.assertTrue(source.deliverThrough(network) == 1, "nearest inventory accepts the first stack");
            helper.assertTrue(count(near, Items.IRON_INGOT) == 11 && count(far, Items.IRON_INGOT) == 0,
                    "nearest exit wins before the farther branch is needed");
            helper.assertTrue(middle.getTransferredThisSecond() == 0 && exit.getTransferredThisSecond() == 0,
                    "unused branch spends no bandwidth");
            for (int slot = 0; slot < near.getContainerSize(); slot++)
                near.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            source.insertItem(0, new ItemStack(Items.GOLD_INGOT, 13), false);
            helper.assertTrue(ItemPipeNetworks.networkOf(source) == network, "inventory contents retain the snapshot");
            helper.assertTrue(source.deliverThrough(network) == 1, "saturated near exit extends the same search");
            helper.assertTrue(count(far, Items.GOLD_INGOT) == 13, "far exit receives the second stack");
            helper.assertFalse(middle.hasStoredItems() || exit.hasStoredItems(), "transit inventories stay empty");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void bottleneckChargesEachPathMemberOnce(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "electrum");
        ItemPipeBlockEntity middle = placePipe(helper, 3, "brass");
        ItemPipeBlockEntity exit = placePipe(helper, 4, "electrum");
        Container target = placeChest(helper, 5);
        helper.runAfterDelay(2, () -> {
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 7), false);
            source.insertItem(1, new ItemStack(Items.GOLD_INGOT, 9), false);
            ItemPipeNetwork network = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(source.deliverThrough(network) == 1, "one-stack transit capacity limits the path");
            helper.assertTrue(source.deliverThrough(network) == 0, "same-window retry cannot overspend bandwidth");
            helper.assertTrue(count(target, Items.IRON_INGOT) == 7 && count(target, Items.GOLD_INGOT) == 0,
                    "bandwidth counts stacks, retaining the second stack at its source");
            helper.assertTrue(source.getTransferredThisSecond() == 1 && middle.getTransferredThisSecond() == 1
                    && exit.getTransferredThisSecond() == 1, "each path member is charged exactly once");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void newAndReplacedInventoriesReuseSnapshotAndWakeWaitingSource(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "electrum");
        ItemPipeNetwork[] cached = new ItemPipeNetwork[1];
        helper.runAfterDelay(2, () -> {
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 15), false);
            cached[0] = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(source.deliverThrough(cached[0]) == 0, "source waits while the exit is empty");
        });
        helper.runAfterDelay(5, () -> {
            placeChest(helper, 3);
            helper.assertTrue(ItemPipeNetworks.networkOf(source) == cached[0], "placing a chest preserves topology");
            // Repeated notifications must not defer an early wake or duplicate the delivery.
            for (int i = 0; i < 20; i++) source.adjacentInventoryUpdated(Direction.UP, at(helper, 3));
        });
        helper.runAfterDelay(8, () -> {
            Container first = (Container) helper.getLevel().getBlockEntity(at(helper, 3));
            helper.assertTrue(count(first, Items.IRON_INGOT) == 15 && !source.hasStoredItems(),
                    "inventory notification wakes the backed-off source promptly and sends it once");
            first.clearContent();
            helper.getLevel().setBlockAndUpdate(at(helper, 3), Blocks.AIR.defaultBlockState());
            Container replacement = placeChest(helper, 3);
            helper.assertTrue(ItemPipeNetworks.networkOf(source) == cached[0], "replacing a chest preserves topology");
            source.insertItem(0, new ItemStack(Items.GOLD_INGOT, 19), false);
            helper.assertTrue(source.deliverThrough(cached[0]) == 1 && count(replacement, Items.GOLD_INGOT) == 19,
                    "cached exit resolves the replacement inventory live");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void waterloggingAndFiltersPreserveTopologyButApplyLiveRules(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        Container target = placeChest(helper, 3);
        helper.runAfterDelay(2, () -> {
            ItemPipeNetwork network = ItemPipeNetworks.networkOf(source);
            var dry = source.getBlockState();
            helper.getLevel().setBlockAndUpdate(at(helper, 2), dry.setValue(BlockStateProperties.WATERLOGGED, true));
            helper.assertTrue(ItemPipeNetworks.networkOf(source) == network, "waterlogging preserves connection layout");
            helper.getLevel().setBlockAndUpdate(at(helper, 2), dry);
            helper.assertTrue(source.attachCover(Direction.UP, item("item_filter")), "filter fixture attaches");
            helper.assertTrue(ItemPipeNetworks.networkOf(source) == network, "filter attachment preserves topology");
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 17), false);
            helper.assertTrue(source.deliverThrough(network) == 0 && count(target, Items.IRON_INGOT) == 0,
                    "empty whitelist rejects items on a cached exit");
            source.removeCover(Direction.UP);
            helper.assertTrue(ItemPipeNetworks.networkOf(source) == network, "filter removal preserves topology");
            helper.assertTrue(source.deliverThrough(network) == 1 && count(target, Items.IRON_INGOT) == 17,
                    "removing the filter immediately opens the cached exit");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void retrieverAndWrenchChangesInvalidateRoutes(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        ItemPipeBlockEntity exit = placePipe(helper, 3, "brass");
        Container target = placeChest(helper, 4);
        helper.runAfterDelay(2, () -> {
            ItemPipeNetwork initial = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(exit.attachCover(Direction.UP, item("item_retriever_cover")), "retriever fixture attaches");
            helper.assertFalse(initial.valid(), "retriever changes the available output faces");
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 23), false);
            ItemPipeNetwork blocked = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(source.deliverThrough(blocked) == 0, "retriever blocks passive routing");
            exit.removeCover(Direction.UP);
            helper.assertFalse(blocked.valid(), "retriever removal also invalidates topology");
            source.toggleSideIO(Direction.UP);
            ItemPipeNetwork outputDisabled = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(source.deliverThrough(outputDisabled) == 0, "wrench output mask blocks the path");
            source.toggleSideIO(Direction.UP);
            helper.assertFalse(outputDisabled.valid(), "wrench direction change invalidates cached routes");
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 1
                    && count(target, Items.IRON_INGOT) == 23, "reenabled output routes the retained stack");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void removedTransitPipeBreaksPathAndReplacementWakesSource(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        placePipe(helper, 3, "brass");
        placePipe(helper, 4, "brass");
        Container target = placeChest(helper, 5);
        helper.runAfterDelay(2, () -> {
            ItemPipeNetwork before = ItemPipeNetworks.networkOf(source);
            helper.getLevel().setBlockAndUpdate(at(helper, 3), Blocks.AIR.defaultBlockState());
            helper.assertFalse(before.valid(), "removal invalidates the whole predecessor tree");
            helper.assertTrue(source.network() == null, "membership no longer resolves the invalid snapshot");
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 29), false);
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 0,
                    "items cannot cross a removed transit pipe");
            helper.assertTrue(count(target, Items.IRON_INGOT) == 0, "destination stays untouched across the gap");
            placePipe(helper, 3, "brass");
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(count(target, Items.IRON_INGOT) == 29 && !source.hasStoredItems(),
                    "joining replacement wakes and delivers the retained stack without polling the old deadline");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void unloadCallbackInvalidatesCachedTransitPath(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        ItemPipeBlockEntity transit = placePipe(helper, 3, "brass");
        Container target = placeChest(helper, 4);
        helper.runAfterDelay(2, () -> {
            ItemPipeNetwork before = ItemPipeNetworks.networkOf(source);
            // Exercise the callback contract without removing a GameTest server's chunk ticket.
            transit.onChunkUnloaded();
            helper.assertFalse(before.valid(), "unload callback invalidates paths through the unloading entity");
            helper.assertTrue(source.network() == null, "loaded sources release the invalid membership");
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 31), false);
            helper.assertTrue(source.deliverThrough(before) == 0 && count(target, Items.IRON_INGOT) == 0,
                    "old snapshot refuses delivery even before setRemoved runs");
            finish(helper);
        });
    }

    private static ResourceLocation id(String path) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return new ResourceLocation("gt6ce", path);
//?}
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id(path)));
    }

    // Vertical fixtures fit the canonical 1x1 template's horizontal test spacing.
    private static BlockPos at(GameTestHelper helper, int height) {
        return helper.absolutePos(new BlockPos(0, height, 0));
    }

    private static ItemPipeBlockEntity placePipe(GameTestHelper helper, int height, String material) {
        ItemPipeBlock block = (ItemPipeBlock) BuiltInRegistries.BLOCK.get(id("item_pipe_medium_" + material));
        var state = block.defaultBlockState().setValue(ItemPipeBlock.UP, true).setValue(ItemPipeBlock.DOWN, true);
        helper.getLevel().setBlockAndUpdate(at(helper, height), state);
        return (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(at(helper, height));
    }

    private static Container placeChest(GameTestHelper helper, int height) {
        helper.getLevel().setBlockAndUpdate(at(helper, height), Blocks.CHEST.defaultBlockState());
        return (Container) helper.getLevel().getBlockEntity(at(helper, height));
    }

    private static int count(Container inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void finish(GameTestHelper helper) {
        for (int height = 1; height <= 5; height++) {
            var entity = helper.getLevel().getBlockEntity(at(helper, height));
            if (entity instanceof Container container) container.clearContent();
            if (entity instanceof ItemPipeBlockEntity pipe) {
                for (int slot = 0; slot < pipe.getSlots(); slot++) pipe.extractItem(slot, Integer.MAX_VALUE, false);
                for (Direction face : Direction.values()) pipe.removeCover(face);
            }
            helper.getLevel().setBlockAndUpdate(at(helper, height), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }
}
