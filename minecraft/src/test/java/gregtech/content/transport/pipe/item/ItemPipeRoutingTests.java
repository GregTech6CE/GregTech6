package gregtech.content.transport.pipe.item;

import gregtech.content.blockentity.inventory.MetalChestBlockEntity;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.cover.CoverAdmission;
import gregtech.content.cover.CoverPlacement;
import gregtech.content.cover.CoverUtilityBehaviors.FilterAction;
import gregtech.content.cover.PanelCoverHost;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.tool.GTToolType;
import gregtech.gametest.MultiblockBehaviorApiTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 60)
    public static void reloadRestoresBandwidthOnlyWithinItsWindow(GameTestHelper helper) {
        ItemPipeBlockEntity source = placePipe(helper, 2, "brass");
        ItemPipeBlockEntity restored = placePipe(helper, 4, "brass");
        var level = helper.getLevel();
        var lookup = level.registryAccess();
        source.chargeTransfer();
        helper.assertFalse(source.pipeCapacityCheck(), "fixture consumes the only transfer this window");
        var saved = NbtCompat.saveWithoutMetadata(source, lookup);

        NbtCompat.loadBlockEntity(restored, saved, lookup);
        helper.assertFalse(restored.pipeCapacityCheck(), "reloading in the same window preserves the spent allowance");
        helper.assertTrue(restored.getTransferredThisSecond() == 1, "the restored counter retains the charge");

        var legacy = saved.copy();
        legacy.remove("gt.transfer_window");
        NbtCompat.loadBlockEntity(restored, legacy, lookup);
        helper.assertTrue(restored.pipeCapacityCheck(), "legacy counters without a window do not charge the current one");

        long untilNextWindow = source.nextBandwidthWindow() - level.getGameTime();
        helper.runAfterDelay(untilNextWindow + 1, () -> {
            NbtCompat.loadBlockEntity(restored, saved, lookup);
            helper.assertTrue(restored.pipeCapacityCheck() && restored.getTransferredThisSecond() == 0,
                    "a charge from an earlier window does not consume the new allowance");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void retainedSideHandlerFollowsFilterAttachmentAndRemoval(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = placePipe(helper, 2, "brass");
        placeChest(helper, 3);
        Direction face = Direction.UP;
//? if >=1.21.1 {
        /*var retained = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pipe.getBlockPos(), face);
*///?} else {
        var retained = pipe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, face)
                .orElseThrow(() -> new IllegalStateException("Missing sided item pipe handler"));
//?}
        var iron = new ItemStack(Items.IRON_INGOT, 5);
        var gold = new ItemStack(Items.GOLD_INGOT, 5);
        helper.assertTrue(retained != null && retained.insertItem(0, iron, false).isEmpty(),
                "the pre-attachment view accepts an unfiltered stack");
        helper.assertTrue(pipe.attachCover(face, item("item_filter")) && pipe.clickFilterCover(face, gold),
                "the fixture attaches a gold whitelist");
        helper.assertTrue(retained.extractItem(0, 5, false).isEmpty() && pipe.getStackInSlot(0).getCount() == 5,
                "the retained view blocks extraction of iron");
        pipe.extractItem(0, 5, false);
        helper.assertTrue(retained.insertItem(0, iron, false).getCount() == 5 && !pipe.hasStoredItems(),
                "the retained view also rejects insertion of iron");
        helper.assertTrue(retained.insertItem(0, gold, false).isEmpty() && retained.extractItem(0, 5, false).is(gold.getItem()),
                "the retained view accepts matching stacks in both directions");
        pipe.removeCover(face);
        helper.assertTrue(retained.insertItem(0, iron, false).isEmpty(), "removal reopens the same view");
        finish(helper);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void itemCapabilitiesReturnAfterRevival(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = placePipe(helper, 2, "brass");
        Direction face = Direction.UP;
//? if >=1.21.1 {
        /*var level = helper.getLevel();
        var capability = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK;
        helper.assertTrue(level.getCapability(capability, pipe.getBlockPos(), face) != null
                        && level.getCapability(capability, pipe.getBlockPos(), null) != null,
                "the pipe initially exposes both capability views");
        pipe.setRemoved();
        pipe.clearRemoved();
        var sided = level.getCapability(capability, pipe.getBlockPos(), face);
        var unsided = level.getCapability(capability, pipe.getBlockPos(), null);
*///?} else {
        var capability = net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER;
        var oldSided = pipe.getCapability(capability, face);
        var oldUnsided = pipe.getCapability(capability, null);
        helper.assertTrue(oldSided.isPresent() && oldUnsided.isPresent(),
                "the pipe initially exposes both capability views");
        pipe.setRemoved();
        try {
            helper.assertFalse(oldSided.isPresent() || oldUnsided.isPresent(),
                    "removing the pipe invalidates both published capabilities");
            helper.assertFalse(pipe.getCapability(capability, face).isPresent(),
                    "a request while removed does not recreate the sided capability");
        } finally {
            pipe.clearRemoved();
            pipe.reviveCaps();
        }
        helper.assertFalse(oldSided.isPresent() || oldUnsided.isPresent(),
                "the revived pipe does not reactivate the old handles");
        var sided = pipe.getCapability(capability, face).orElse(null);
        var unsided = pipe.getCapability(capability, null).orElse(null);
//?}
        helper.assertTrue(sided != null && unsided != null, "both capability views return after revival");
        var iron = new ItemStack(Items.IRON_INGOT, 5);
        helper.assertTrue(sided.insertItem(0, iron, false).isEmpty()
                        && unsided.extractItem(0, 5, false).getCount() == 5,
                "the restored views access the same live storage");
        finish(helper);
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
            ItemStack gold = new ItemStack(Items.GOLD_INGOT);
            helper.assertTrue(source.clickFilterCover(Direction.UP, gold), "set the cover filter");
            helper.assertTrue(source.coverFilterPermits(Direction.UP, gold), "whitelist admits gold");
            helper.assertTrue(source.configureFilterCover(Direction.UP, FilterAction.TOGGLE_MODE)
                    && !source.coverFilterPermits(Direction.UP, gold), "the mode toggle blacklists gold");
            helper.assertTrue(source.configureFilterCover(Direction.UP, FilterAction.CLEAR)
                    && source.coverFilterPermits(Direction.UP, gold), "clearing the template retains blacklist mode");
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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void retrieverRequiresTickingItemPipeAndFilterPreviewPreservesConnections(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(at(helper, 1), BuiltInRegistries.BLOCK.get(id("mixer_bronze")).defaultBlockState());
        ItemPipeBlockEntity lowerPipe = placePipe(helper, 2, "brass");
        placePipe(helper, 3, "brass");
        level.setBlockAndUpdate(at(helper, 4), BuiltInRegistries.BLOCK.get(id("pipe_medium_steel")).defaultBlockState());
        level.setBlockAndUpdate(at(helper, 5), BuiltInRegistries.BLOCK.get(id("chest_steel")).defaultBlockState());
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(at(helper, 1));
        var fluidPipe = (FluidPipeBlockEntity) level.getBlockEntity(at(helper, 4));
        var metalChest = (MetalChestBlockEntity) level.getBlockEntity(at(helper, 5));
        ItemStack retriever = item("item_retriever_cover");
        ItemStack itemFilter = item("item_filter");

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(CoverAdmission.allowCover(lowerPipe, Direction.EAST, retriever)
                            && lowerPipe.panels().canAttach(Direction.EAST, retriever)
                            && CoverPlacement.canPlace(level, at(helper, 2), Direction.EAST, retriever),
                    "retriever preview and admission accept a ticking item pipe");
            for (var forbidden : new PanelCoverHost[] {machine, fluidPipe, metalChest}) {
                BlockPos pos = forbidden.coverOwner().getBlockPos();
                helper.assertFalse(CoverAdmission.allowCover(forbidden.coverOwner(), Direction.UP, retriever),
                        "CoverAdmission rejects retriever on " + forbidden.getClass().getSimpleName());
                helper.assertFalse(forbidden.panels().canAttach(Direction.UP, retriever),
                        "PanelCoverRuntime rejects retriever on " + forbidden.getClass().getSimpleName());
                helper.assertFalse(CoverPlacement.canPlace(level, pos, Direction.UP, retriever),
                        "CoverPlacement rejects retriever on " + forbidden.getClass().getSimpleName());
                helper.assertFalse(forbidden.attachCover(Direction.UP, retriever),
                        "attachCover rejects retriever on " + forbidden.getClass().getSimpleName());
            }

            helper.assertTrue(lowerPipe.getBlockState().getValue(ItemPipeBlock.UP),
                    "adjacent item pipes start connected");
            helper.assertFalse(CoverPlacement.canPlace(level, at(helper, 2), Direction.UP, itemFilter),
                    "item filter preview rejects pipe-to-pipe face");
            helper.assertTrue(lowerPipe.getBlockState().getValue(ItemPipeBlock.UP),
                    "item filter preview does not disconnect pipes");
            helper.assertFalse(lowerPipe.attachCover(Direction.UP, itemFilter),
                    "item filter attach rejects pipe-to-pipe face");
            helper.assertTrue(lowerPipe.getBlockState().getValue(ItemPipeBlock.UP),
                    "rejected item filter attach preserves existing pipe-to-pipe connection");
            helper.assertTrue(lowerPipe.attachCover(Direction.EAST, retriever),
                    "retriever attaches to the item pipe's open face");
            level.setBlockAndUpdate(at(helper, 3), Blocks.AIR.defaultBlockState());
            helper.assertTrue(lowerPipe.attachCover(Direction.UP, itemFilter),
                    "item filter attaches on open pipe face");
            placePipe(helper, 3, "brass");
            helper.assertFalse(gregtech.content.cover.CoverConnections.canConnect(level, at(helper, 2), Direction.UP),
                    "pre-attached item filter still blocks new pipe connection");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void wrenchDismantleCollectsSeparatedCoversIntoInventoryUnlessFull(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack filter = item("item_filter");
        ItemStack retriever = item("item_retriever_cover");
        ItemPipeBlockEntity pipe = placePipe(helper, 2, "brass");
        helper.assertTrue(pipe.attachCover(Direction.EAST, filter), "attach filter before wrench dismantle");
        helper.assertTrue(pipe.attachCover(Direction.WEST, retriever), "attach retriever before wrench dismantle");

        var wrenchPlayer = MultiblockBehaviorApiTests.menuPlayer(helper);
        wrenchPlayer.setGameMode(GameType.SURVIVAL);
        try {
            wrenchPlayer.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    GTToolItem.create(GTToolType.WRENCH, GT6Materials.Compounds.VanadiumSteel, GT6Materials.Woods.Spruce));
            helper.assertTrue(wrenchPlayer.gameMode.destroyBlock(at(helper, 2)), "wrench destroys brass pipe");
            helper.assertTrue(
                    count(wrenchPlayer.getInventory(), filter.getItem()) == 1
                            && count(wrenchPlayer.getInventory(), retriever.getItem()) == 1,
                    "wrench dismantle moves separated covers directly into player inventory");
            helper.assertTrue(
                    level.getEntitiesOfClass(ItemEntity.class, new AABB(at(helper, 2)).inflate(1.5)).isEmpty(),
                    "no cover items spill when player inventory has room");
        } finally {
            wrenchPlayer.discard();
        }

        ItemPipeBlockEntity fullPipe = placePipe(helper, 2, "brass");
        helper.assertTrue(fullPipe.attachCover(Direction.EAST, filter), "attach filter for full-inventory spill check");
        var fullPlayer = MultiblockBehaviorApiTests.menuPlayer(helper);
        fullPlayer.setGameMode(GameType.SURVIVAL);
        try {
            fullPlayer.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    GTToolItem.create(
                            GTToolType.MONKEY_WRENCH, GT6Materials.Compounds.VanadiumSteel, GT6Materials.Woods.Spruce));
            for (int slot = 0; slot < fullPlayer.getInventory().items.size(); slot++) {
                if (fullPlayer.getInventory().getItem(slot).isEmpty()) {
                    fullPlayer.getInventory().setItem(slot, new ItemStack(Items.STICK));
                }
            }
            helper.assertTrue(fullPlayer.gameMode.destroyBlock(at(helper, 2)), "monkey wrench destroys brass pipe");
            var spilled = level.getEntitiesOfClass(ItemEntity.class, new AABB(at(helper, 2)).inflate(1.5));
            helper.assertTrue(
                    spilled.stream().anyMatch(entity -> entity.getItem().is(filter.getItem())),
                    "monkey wrench spills separated cover to world when player inventory is full");
            for (ItemEntity entity : spilled) entity.discard();
        } finally {
            fullPlayer.discard();
        }
        finish(helper);
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
