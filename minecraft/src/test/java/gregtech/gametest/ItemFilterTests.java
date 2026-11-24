package gregtech.gametest;

import gregtech.client.gui.FilterMenu;
import gregtech.content.block.misc.FilterBlockEntity;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.content.logistics.LogisticsItemRouter;
import gregtech.content.logistics.LogisticsNetwork;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.stack.StackCompat;
import gregtech.engine.stack.StackData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ItemFilterTests {
    private ItemFilterTests() {}

    @GameTest(template = "test_empty")
    public static void slotModesControlFilteringAndDumpReservations(GameTestHelper helper) {
        var filter = placeFilter(helper, "filter_items_fluids");
        var level = helper.getLevel();
//? if >=1.21.1 {
        /*var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        var swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
*///?} else {
        var player = helper.makeMockPlayer();
        var healing = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
        var swiftness = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);
//?}
        player.setPos(filter.getBlockPos().getCenter());
        var menu = new FilterMenu(1, player.getInventory(), filter);
        var pick = new ItemStack(Items.IRON_PICKAXE);
        var worn = pick.copy();
        worn.setDamageValue(1);

        menu.setCarried(pick);
        menu.clicked(53, 0, ClickType.PICKUP, player);
        menu.setCarried(healing);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(filter.permitsItem(pick) && !filter.permitsItem(worn), "new tool template is exact");
        menu.clicked(53, 1, ClickType.QUICK_MOVE, player);
        helper.assertTrue(filter.permitsItem(worn) && !filter.permitsItem(new ItemStack(Items.STONE_PICKAXE)),
                "type-only slot accepts wear but still checks the item");
        helper.assertTrue(filter.permitsItem(healing) && !filter.permitsItem(swiftness),
                "a second slot still requires the sampled potion effect");
        var saved = NbtCompat.saveWithoutMetadata(filter, level.registryAccess());
        filter.clearFilter();
        NbtCompat.loadBlockEntity(filter, saved, level.registryAccess());
        var reservation = filter.logisticsItemFilter();
        helper.assertTrue(reservation.matches(worn) && reservation.matches(healing) && !reservation.matches(swiftness),
                "Dump reserves only the items accepted by the saved slot modes");
        menu.clicked(53, 1, ClickType.QUICK_MOVE, player);
        helper.assertFalse(filter.permitsItem(worn), "second mode click returns to exact");
        menu.clicked(53, 1, ClickType.QUICK_MOVE, player);
        menu.clicked(53, 1, ClickType.PICKUP, player);
        helper.assertTrue(!filter.permitsItem(pick) && filter.templates().getItem(53).isEmpty(),
                "ordinary right click clears the ghost slot");
        menu.setCarried(pick);
        menu.clicked(53, 0, ClickType.PICKUP, player);
        helper.assertFalse(filter.permitsItem(worn), "a newly populated slot starts exact");
        filter.toggleMode();
        helper.assertTrue(!filter.permitsItem(pick) && filter.permitsItem(worn) && filter.logisticsItemFilter() == null,
                "blacklist inverts the same result and reserves nothing");
        helper.succeed();
    }

    private static FilterBlockEntity placeFilter(GameTestHelper helper, String name) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", name));
        var pos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return (FilterBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "test_empty")
    public static void prefixFilterKeepsOneRecognizedFamily(GameTestHelper helper) {
        var filter = placeFilter(helper, "filter_oredict");
//? if >=1.21.1 {
        /*var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
        player.setPos(filter.getBlockPos().getCenter());
        var menu = new FilterMenu(1, player.getInventory(), filter);
        menu.setCarried(new ItemStack(Items.IRON_INGOT));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.setCarried(new ItemStack(Items.IRON_PICKAXE));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.prefixMode() && !menu.supportsItemMatchModes() && "ingot".equals(filter.selectedPrefix()),
                "an unrecognized sample preserves the selected prefix");
        helper.assertTrue(filter.permitsItem(new ItemStack(Items.GOLD_INGOT)) && !filter.permitsItem(new ItemStack(Items.IRON_NUGGET)),
                "a prefix spans materials and still distinguishes different forms");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dumpRoutesOnlyUnreservedStacks(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var wire = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "logistics_wire"));
        level.setBlockAndUpdate(pos, wire.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(pos.west(), Blocks.BARREL.defaultBlockState());
        var filterBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "filter_items"));
        level.setBlockAndUpdate(pos.south(), filterBlock.defaultBlockState());
        var covers = ((LogisticsCoverHost) level.getBlockEntity(pos)).logisticsCovers();
        helper.assertTrue(covers.attach(Direction.EAST, registeredItem("generic_logistics_storage_bus"))
                        && covers.attach(Direction.WEST, registeredItem("logistics_dump_bus_item"))
                        && covers.attach(Direction.SOUTH, registeredItem("generic_logistics_export_bus")),
                "wire has storage, dump and whitelist endpoints");
        var storage = (Container) level.getBlockEntity(pos.east());
        var dump = (Container) level.getBlockEntity(pos.west());
        var filter = (FilterBlockEntity) level.getBlockEntity(pos.south());
        var selected = new ItemStack(Items.PAPER);
        StackData.putString(selected, "batch", "selected");
        var other = new ItemStack(Items.PAPER);
        StackData.putString(other, "batch", "other");
        storage.setItem(0, selected.copy());
        storage.setItem(1, other.copy());
        filter.setTemplate(0, selected);
        var network = new LogisticsNetwork.Snapshot(java.util.Set.of(pos), 0);
        filter.toggleItemMatchMode(0);
        helper.assertTrue(LogisticsItemRouter.collect(level, network).tryDump(1) == 0 && dump.isEmpty(),
                "item-only whitelist reserves both variants from Dump");
        filter.toggleItemMatchMode(0);
        helper.assertTrue(LogisticsItemRouter.collect(level, network).tryDump(1) == 1
                        && StackCompat.sameItem(storage.getItem(0), selected) && storage.getItem(1).isEmpty()
                        && StackCompat.sameItem(dump.getItem(0), other),
                "exact whitelist keeps its sample and sends the other variant to Dump");

        var fluidBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "filter_fluids"));
        level.setBlockAndUpdate(pos.south(), fluidBlock.defaultBlockState());
        var fluidFilter = (FilterBlockEntity) level.getBlockEntity(pos.south());
        fluidFilter.setTemplate(0, selected);
        helper.assertTrue(fluidFilter.logisticsItemFilter() == null
                        && LogisticsItemRouter.collect(level, network).tryDump(1) == 1 && storage.isEmpty()
                        && StackCompat.sameItem(dump.getItem(1), selected),
                "a fluid-only filter leaves both item variants available to Dump");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void coversFilterAndRetrieveByItemType(GameTestHelper helper) {
        var level = helper.getLevel();
        var lookup = level.registryAccess();
        var pos = helper.absolutePos(new BlockPos(0, 1, 0));
        var pipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "item_pipe_medium_brass"));
        level.setBlockAndUpdate(pos, pipeBlock.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.CHEST.defaultBlockState());
        var pipe = (ItemPipeBlockEntity) level.getBlockEntity(pos);
        var chest = (Container) level.getBlockEntity(pos.above());
        helper.assertTrue(pipe.attachCover(Direction.DOWN, registeredItem("item_filter"))
                        && pipe.attachCover(Direction.UP, registeredItem("item_retriever_cover")),
                "item pipe accepts both covers");
        var sample = new ItemStack(Items.IRON_PICKAXE);
        sample.setDamageValue(8);
        StackData.putInt(sample, "sample", 1);
        var candidate = new ItemStack(Items.IRON_PICKAXE);
        candidate.setDamageValue(27);
        StackData.putInt(candidate, "sample", 2);
        helper.assertTrue(pipe.clickFilterCover(Direction.DOWN, sample) && pipe.clickFilterCover(Direction.UP, sample),
                "both covers accept the sample");
        var installed = pipe.getCover(Direction.DOWN);
        var stored = CoverUtilityBehaviors.itemFilter(installed, lookup);
        helper.assertTrue(StackCompat.sameItem(stored, new ItemStack(Items.IRON_PICKAXE)),
                "the saved sample holds only the item type");
        helper.assertFalse(pipe.clickFilterCover(Direction.DOWN, candidate), "repeating a sample does not toggle a mode");
        helper.assertTrue(pipe.coverFilterPermits(Direction.DOWN, candidate)
                        && !pipe.coverFilterPermits(Direction.DOWN, new ItemStack(Items.STONE_PICKAXE)),
                "whitelist matches item identity across wear and data");
        pipe.configureFilterCover(Direction.DOWN, CoverUtilityBehaviors.FilterAction.TOGGLE_MODE);
        helper.assertFalse(pipe.coverFilterPermits(Direction.DOWN, candidate), "blacklist rejects the same item type");
        pipe.configureFilterCover(Direction.DOWN, CoverUtilityBehaviors.FilterAction.CLEAR);
        helper.assertTrue(pipe.coverFilterPermits(Direction.DOWN, candidate), "clearing the target retains blacklist mode");
        chest.setItem(0, new ItemStack(Items.STONE_PICKAXE));
        chest.setItem(1, candidate.copy());
        for (int tick = 0; tick < CoverUtilityBehaviors.RETRIEVER_PERIOD; tick++) pipe.tickCovers();
        helper.assertTrue(chest.getItem(0).is(Items.STONE_PICKAXE) && chest.getItem(1).isEmpty()
                        && StackCompat.sameItem(pipe.getStackInSlot(0), candidate),
                "retriever pulls only the selected item type and preserves the actual stack");
        helper.succeed();
    }

    private static ItemStack registeredItem(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", name)));
    }
}
