package gregtech.gametest;

import gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity;
import gregtech.content.blockentity.inventory.UsbSwitchBlockEntity;
import gregtech.client.gui.GarbageDumpContainerMenu;
import gregtech.content.data.UsbDataMedia;
import gregtech.content.world.GarbageData;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class StorageCoverTransactionTests {
    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path)));
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void garbageSwapCommitsExactlyOnce(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "GarbageSwap"));
        var dumpState = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "ender_garbage_dump")).defaultBlockState();
        // No ticking output neighbor: exercise the real BE view and native menu synchronously.
        var dump = new EnderGarbageDumpBlockEntity(h.absolutePos(new BlockPos(1, 1, 1)), dumpState);
        dump.setLevel(h.getLevel());
        var storage = h.getLevel().getServer().overworld().getDataStorage();
        var previous = GarbageData.get(h.getLevel());
        try {
            for (var gameType : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                player.gameMode.changeGameModeForPlayer(gameType);
                for (int button : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 40}) {
                    for (int count : new int[]{64, 100}) {
                        var pool = new GarbageData();
                        storage.set("gregtech_garbage", pool);
                        pool.trash(new ItemStack(Items.DIRT, count));
                        pool.trash(new ItemStack(Items.STONE, 5));
                        player.getInventory().clearContent();
                        var menu = new GarbageDumpContainerMenu(1, player.getInventory(), dump.view());
                        menu.clicked(0, button, ClickType.SWAP, player);
                        h.assertTrue(player.getInventory().getItem(button).is(Items.DIRT)
                                && player.getInventory().getItem(button).getCount() == 64, "empty swap receives one display stack");
                        int remainder = count == 64 ? 0 : pool.viewItem(0).getCount();
                        h.assertTrue(64 + remainder == count, "pool and player conserve dirt in " + gameType + "/" + button);
                        h.assertTrue(pool.viewItem(count == 64 ? 0 : 1).is(Items.STONE)
                                && pool.viewItem(count == 64 ? 0 : 1).getCount() == 5, "following compacted entry is untouched");
                        menu.clicked(0, button, ClickType.SWAP, player);
                        h.assertTrue(player.getInventory().getItem(button).getCount() == 64
                                && pool.viewItem(0).getCount() == (count == 64 ? 5 : 36), "occupied destination refuses insertion and extraction");
                        for (int invalid : new int[]{-1, 9, 39, 41}) {
                            menu.clicked(0, invalid, ClickType.SWAP, player);
                            h.assertTrue(pool.viewItem(0).getCount() == (count == 64 ? 5 : 36), "invalid swap button has no effect");
                        }
                    }
                }
            }
        } finally {
            storage.set("gregtech_garbage", previous);
            player.getInventory().clearContent();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void garbageQuickMovePreservesEveryRemainder(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "GarbageShift"));
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "ender_garbage_dump")).defaultBlockState();
        var dump = new EnderGarbageDumpBlockEntity(h.absolutePos(new BlockPos(1, 1, 1)), state);
        dump.setLevel(h.getLevel());
        var storage = h.getLevel().getServer().overworld().getDataStorage();
        var previous = GarbageData.get(h.getLevel());
        try {
            for (var gameType : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                player.gameMode.changeGameModeForPlayer(gameType);
                for (var source : new net.minecraft.world.item.Item[]{Items.DIRT, Items.ENDER_PEARL, Items.DIAMOND_SWORD}) {
                    int maximum = new ItemStack(source).getMaxStackSize();
                    for (int space : new int[]{0, 1, maximum}) {
                        var pool = new GarbageData();
                        storage.set("gregtech_garbage", pool);
                        pool.trash(new ItemStack(source, maximum));
                        pool.trash(new ItemStack(Items.GOLD_INGOT, 3));
                        player.getInventory().clearContent();
                        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
                        player.getInventory().setItem(0, space == maximum ? ItemStack.EMPTY : new ItemStack(source, maximum - space));
                        var menu = new GarbageDumpContainerMenu(1, player.getInventory(), dump.view());
                        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                        int remaining = 0;
                        int gold = 0;
                        for (int slot = 0; slot < pool.entryCount(); slot++) {
                            var stack = pool.viewItem(slot);
                            if (stack.is(source)) remaining += stack.getCount();
                            if (stack.is(Items.GOLD_INGOT)) gold += stack.getCount();
                        }
                        int received = player.getInventory().getItem(0).getCount() - (maximum - space);
                        h.assertTrue(received == space && remaining == maximum - space,
                                "quick move conserves full/partial/no-space " + source + " in " + gameType);
                        h.assertTrue(received + remaining == maximum && gold == 3, "no remainder or following entry lost");
                    }
                }
            }
        } finally {
            storage.set("gregtech_garbage", previous);
            player.getInventory().clearContent();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void usbNonSelectorRemovalPreservesSelectedFile(GameTestHelper h) {
        for (String kind : new String[]{"usb_switch", "hdd_switch"}) {
            var pos = new BlockPos(2, 1, 2);
            h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", kind)));
            var port = (UsbSwitchBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            for (String selectorId : new String[]{"manual_selector", "button_panel_selector", "redstone_selector"}) {
                var selector = item(selectorId);
                h.assertTrue(!selector.isEmpty() && gregtech.content.cover.CoverItems.isCover(selector),
                        "registered selector is admitted as a cover: " + selectorId);
                h.assertTrue(port.attachCover(Direction.NORTH, selector), "mode-capable " + kind + " accepts " + selectorId);
                port.setMode(7);
                h.assertTrue(!port.removeCover(Direction.NORTH).isEmpty() && port.mode() == 0,
                        "every selector removal resets mode: " + selectorId);
            }
            for (int mode = 0; mode < 16; mode++) {
                var file = new CompoundTag();
                file.putInt("selected_file", mode);
                var medium = item(kind.equals("usb_switch") ? "usb3_stick" : "usb3_hdd");
                h.assertTrue(kind.equals("usb_switch") ? UsbDataMedia.writeStick(medium, 3, file)
                        : UsbDataMedia.writeDrive(medium, mode, 3, file), "native medium accepts selected file");
                port.items().setStackInSlot(kind.equals("usb_switch") ? mode : 0, medium);
                h.assertTrue(port.attachCover(Direction.NORTH, item("manual_selector")), "manual selector attaches");
                h.assertTrue(port.attachCover(Direction.UP, item("redstone_emitter")), "non-selector emitter attaches");
                port.setMode(mode);
                h.assertTrue(!port.removeCover(Direction.UP).isEmpty() && port.mode() == mode,
                        "non-selector removal preserves " + kind + " mode " + mode);
                var read = port.readUsbData(Direction.SOUTH, 3);
                h.assertTrue(read != null && read.getInt("selected_file") == mode, "actual USB consumer retains selected file");
                h.assertTrue(!port.removeCover(Direction.NORTH).isEmpty() && port.mode() == 0,
                        "selector removal still resets " + kind);
            }
        }
        h.succeed();
    }
}
