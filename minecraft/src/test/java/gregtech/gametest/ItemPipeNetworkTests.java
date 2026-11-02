package gregtech.gametest;

import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.transport.ItemPipeNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** World lifecycle, sided inventory and persistence contracts beyond the pure route tests. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ItemPipeNetworkTests {
    private static final String BATCH = "item_pipe_network";

    private ItemPipeNetworkTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void scheduledDeliverySkipsIntermediateInventories(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        ItemPipeBlockEntity middle = placePipe(helper, 3, "brass");
        ItemPipeBlockEntity exit = placePipe(helper, 4, "brass");
        Container target = placeChest(helper, 5);
        helper.assertTrue(source.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(), "source accepts a stack");

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(count(target, Items.IRON_INGOT) == 64, "the level scheduler delivers without a block entity ticker");
            helper.assertFalse(source.hasStoredItems(), "source drained");
            helper.assertFalse(middle.hasStoredItems() || exit.hasStoredItems(), "items never rest in transit pipes");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void bottleneckChargesEveryPipeOncePerStack(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "electrum");
        ItemPipeBlockEntity middle = placePipe(helper, 3, "brass");
        ItemPipeBlockEntity exit = placePipe(helper, 4, "electrum");
        Container target = placeChest(helper, 5);
        helper.runAfterDelay(2, () -> {
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 7), false);
            source.insertItem(1, new ItemStack(Items.GOLD_INGOT, 9), false);
            var network = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(source.deliverThrough(network) == 1, "one-stack transit pipe limits the whole path");
            helper.assertTrue(source.deliverThrough(network) == 0, "repeated attempts cannot spend the same window twice");
            helper.assertTrue(count(target, Items.IRON_INGOT) == 7, "a stack, rather than one item, was delivered");
            helper.assertTrue(count(target, Items.GOLD_INGOT) == 0, "second stack waits behind the bottleneck");
            helper.assertTrue(source.getTransferredThisSecond() == 1 && middle.getTransferredThisSecond() == 1
                    && exit.getTransferredThisSecond() == 1, "every path member is charged once");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void originsBelongToIndividualSlots(GameTestHelper helper) {
        Container below = placeChest(helper, 1);
        ItemPipeBlockEntity source = placePipe(helper, 2, "electrum");
        Container above = placeChest(helper, 3);
        helper.runAfterDelay(2, () -> {
            source.componentItems(Direction.DOWN).insertItem(0, new ItemStack(Items.IRON_INGOT, 7), false);
            source.componentItems(Direction.UP).insertItem(1, new ItemStack(Items.GOLD_INGOT, 9), false);
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 2, "both origins can route in one pass");
            helper.assertTrue(count(below, Items.IRON_INGOT) == 0 && count(below, Items.GOLD_INGOT) == 9, "lower origin receives only the other stack");
            helper.assertTrue(count(above, Items.GOLD_INGOT) == 0 && count(above, Items.IRON_INGOT) == 7, "upper origin receives only the other stack");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void otherHalfOfSourceChestIsNotAnExit(GameTestHelper helper) {
        // Two columns fit within the shared template's five-block horizontal test spacing.
        BlockPos leftChest = at(helper, 2);
        BlockPos rightChest = leftChest.east();
        var left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.LEFT);
        var right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT);
        helper.getLevel().setBlockAndUpdate(leftChest, left);
        helper.getLevel().setBlockAndUpdate(rightChest, right);
        var sourceState = block("brass").defaultBlockState().setValue(ItemPipeBlock.UP, true)
                .setValue(ItemPipeBlock.EAST, true);
        var exitState = block("brass").defaultBlockState().setValue(ItemPipeBlock.UP, true)
                .setValue(ItemPipeBlock.WEST, true);
        helper.getLevel().setBlockAndUpdate(leftChest.below(), sourceState);
        helper.getLevel().setBlockAndUpdate(rightChest.below(), exitState);
        ItemPipeBlockEntity source = (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(leftChest.below());
        helper.runAfterDelay(2, () -> {
            var actualLeft = helper.getLevel().getBlockState(leftChest);
            helper.assertTrue(actualLeft.getValue(ChestBlock.TYPE) == ChestType.LEFT
                    && helper.getLevel().getBlockState(rightChest).getValue(ChestBlock.TYPE) == ChestType.RIGHT,
                    "fixture forms a real left/right double chest");
            Container shared = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, actualLeft,
                    helper.getLevel(), leftChest, false);
            helper.assertTrue(shared != null && shared.getContainerSize() == 54, "both chest halves share one inventory");
            shared.setItem(0, new ItemStack(Items.IRON_INGOT, 31));
            source.componentItems(Direction.UP).insertItem(0, shared.removeItem(0, 13), false);
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 0,
                    "neither the input chest half nor its partner receives the extracted stack");
            helper.assertTrue(count(shared, Items.IRON_INGOT) == 18 && source.getStackInSlot(0).getCount() == 13,
                    "blocked backflow preserves the items in the source pipe");

            // Removing the origin must make the same exit usable, proving the fixture is not blocked.
            ItemStack unsided = source.extractItem(0, 64, false);
            source.insertItem(0, unsided, false);
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 1
                    && count(shared, Items.IRON_INGOT) == 31, "the double chest accepts a stack without that origin");
            shared.clearContent();
            helper.getLevel().setBlockAndUpdate(rightChest.below(), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(rightChest, Blocks.AIR.defaultBlockState());
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void changingDirectionRebuildsCachedRoutes(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        placePipe(helper, 3, "brass");
        Container target = placeChest(helper, 4);
        helper.runAfterDelay(2, () -> {
            source.toggleSideIO(Direction.UP);
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 12), false);
            var blocked = ItemPipeNetworks.networkOf(source);
            helper.assertTrue(source.deliverThrough(blocked) == 0, "disabled output blocks a physical pipe link");
            source.toggleSideIO(Direction.UP);
            helper.assertFalse(blocked.valid(), "changing direction invalidates the previous snapshot");
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 1, "enabled output routes despite disabled input");
            helper.assertTrue(count(target, Items.IRON_INGOT) == 12, "target receives exactly the original stack");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void newInventoryAppearsBesideCachedNetwork(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        placePipe(helper, 3, "brass");
        helper.runAfterDelay(2, () -> {
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 15), false);
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 0, "initial network has no inventories");
        });
        helper.runAfterDelay(5, () -> placeChest(helper, 4));
        helper.runAfterDelay(55, () -> {
            Container target = (Container) helper.getLevel().getBlockEntity(at(helper, 4));
            helper.assertTrue(count(target, Items.IRON_INGOT) == 15, "a newly placed endpoint becomes reachable without editing a pipe");
            helper.assertFalse(source.hasStoredItems(), "waiting source resumes after endpoint placement");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void removedTransitPipeCannotRemainInCachedPath(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        placePipe(helper, 3, "brass");
        placePipe(helper, 4, "brass");
        Container target = placeChest(helper, 5);
        helper.runAfterDelay(2, () -> {
            var beforeRemoval = ItemPipeNetworks.networkOf(source);
            helper.getLevel().setBlockAndUpdate(at(helper, 3), Blocks.AIR.defaultBlockState());
            helper.assertFalse(beforeRemoval.valid(), "removing transit pipe invalidates a cached path");
            source.insertItem(0, new ItemStack(Items.IRON_INGOT, 17), false);
            helper.assertTrue(source.deliverThrough(ItemPipeNetworks.networkOf(source)) == 0, "items cannot cross the gap");
            helper.assertTrue(count(target, Items.IRON_INGOT) == 0, "destination remains untouched");
            placePipe(helper, 3, "brass");
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(count(target, Items.IRON_INGOT) == 17, "replacement joins and wakes the waiting source");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH)
    public static void wideInventoryPersistsDistinctSlotIndices(GameTestHelper helper) {
        ItemPipeBlockEntity original = detached("vibranium_silver");
        helper.assertTrue(original.getSlots() == 512, "fixture exposes more than a byte of slots");
        original.componentItems(Direction.DOWN).insertItem(0, new ItemStack(Items.IRON_INGOT, 11), false);
        original.componentItems(Direction.UP).insertItem(256, new ItemStack(Items.GOLD_INGOT, 23), false);
        original.componentItems(Direction.NORTH).insertItem(511, new ItemStack(Items.COPPER_INGOT, 37), false);
        CompoundTag saved = save(helper, original);
        ItemPipeBlockEntity restored = detached("vibranium_silver");
        load(helper, restored, saved);
        assertStack(helper, restored, 0, Items.IRON_INGOT, 11);
        assertStack(helper, restored, 256, Items.GOLD_INGOT, 23);
        assertStack(helper, restored, 511, Items.COPPER_INGOT, 37);
        byte[] origins = save(helper, restored).getByteArray("gt.slot_origins");
        helper.assertTrue(origins[0] == Direction.DOWN.ordinal() && origins[256] == Direction.UP.ordinal()
                && origins[511] == Direction.NORTH.ordinal(), "slot origins also survive a save/load round trip");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void runtimeNbtLoadWakesAnIdlePipe(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        Container target = placeChest(helper, 3);
        helper.runAfterDelay(3, () -> {
            ItemPipeBlockEntity seed = detached("brass");
            seed.insertItem(0, new ItemStack(Items.IRON_INGOT, 21), false);
            // /data reloads an existing block entity without placement or an item-handler insertion.
            load(helper, source, save(helper, seed));
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(count(target, Items.IRON_INGOT) == 21, "runtime NBT load re-registers the idle source");
            helper.assertFalse(source.hasStoredItems(), "loaded items have been delivered");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH)
    public static void legacyLargeStackMigratesWithoutLosingItemsOrOrigin(GameTestHelper helper) {
        ItemPipeBlockEntity seed = detached("vibranium_silver");
        seed.insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        CompoundTag old = save(helper, seed);
        old.remove("gt.slot_origins");
        old.putByte("gt.last_received", (byte) Direction.DOWN.ordinal());
        ListTag items = old.getCompound("gt.items").getList("Items", Tag.TAG_COMPOUND);
        items.getCompound(0).putByte("Slot", (byte) 0);
        items.getCompound(0).putInt("gt.pipe_count", 512);
        ItemPipeBlockEntity restored = detached("vibranium_silver");
        load(helper, restored, old);
        int total = 0;
        byte[] origins = save(helper, restored).getByteArray("gt.slot_origins");
        for (int slot = 0; slot < restored.getSlots(); slot++) {
            ItemStack stack = restored.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            helper.assertTrue(stack.is(Items.IRON_INGOT) && stack.getCount() <= 64, "legacy buffer is split into ordinary stacks");
            helper.assertTrue(origins[slot] == Direction.DOWN.ordinal(), "every split stack retains the legacy origin");
            total += stack.getCount();
        }
        helper.assertTrue(total == 512, "all legacy items survive migration");
        helper.succeed();
    }

    private static ItemPipeBlock block(String material) {
//? if >=1.21.1 {
        /*ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gt6ce", "item_pipe_medium_" + material);
*///?} else {
        ResourceLocation id = new ResourceLocation("gt6ce", "item_pipe_medium_" + material);
//?}
        return (ItemPipeBlock) BuiltInRegistries.BLOCK.get(id);
    }

    private static ItemPipeBlockEntity detached(String material) {
        return new ItemPipeBlockEntity(BlockPos.ZERO, block(material).defaultBlockState());
    }

    // Each test uses a vertical column so the shared 1x1 template's horizontal spacing is sufficient.
    private static BlockPos at(GameTestHelper helper, int height) {
        return helper.absolutePos(new BlockPos(0, height, 0));
    }

    private static ItemPipeBlockEntity placePipe(GameTestHelper helper, int height, String material) {
        var state = block(material).defaultBlockState().setValue(ItemPipeBlock.UP, true).setValue(ItemPipeBlock.DOWN, true);
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
            }
            helper.getLevel().setBlockAndUpdate(at(helper, height), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    private static CompoundTag save(GameTestHelper helper, ItemPipeBlockEntity pipe) {
//? if >=1.21.1 {
        /*return pipe.saveWithoutMetadata(helper.getLevel().registryAccess());
*///?} else {
        return pipe.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper helper, ItemPipeBlockEntity pipe, CompoundTag tag) {
//? if >=1.21.1 {
        /*pipe.loadWithComponents(tag, helper.getLevel().registryAccess());
*///?} else {
        pipe.load(tag);
//?}
    }

    private static void assertStack(GameTestHelper helper, ItemPipeBlockEntity pipe, int slot, Item item, int count) {
        ItemStack actual = pipe.getStackInSlot(slot);
        helper.assertTrue(actual.is(item) && actual.getCount() == count, "slot " + slot + " retains its item and count");
    }
}
