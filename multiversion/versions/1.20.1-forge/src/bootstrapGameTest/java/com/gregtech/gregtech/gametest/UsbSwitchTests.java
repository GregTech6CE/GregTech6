package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.inventory.UsbSwitchBlock;
import com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity;
import com.gregtech.gregtech.client.gui.DataSwitchMenu;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6 19000 and 19001 data-switch regression tests. */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class UsbSwitchTests {
    private UsbSwitchTests() {}
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static UsbSwitchBlockEntity place(GameTestHelper h, UsbSwitchBlock.Kind kind) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        level.removeBlock(pos, false);
        level.setBlock(pos, (kind == UsbSwitchBlock.Kind.USB
                ? GTStorage.USB_SWITCH.get() : GTStorage.HDD_SWITCH.get()).defaultBlockState(), 3);
        return (UsbSwitchBlockEntity) level.getBlockEntity(pos);
    }
    private static ItemStack stick(int tier) { return new ItemStack(GTTechnological.get("usb" + tier + "_stick")); }
    private static ItemStack drive(int tier) { return new ItemStack(GTTechnological.get("usb" + tier + "_hdd")); }
    private static CompoundTag file(String value) {
        CompoundTag out = new CompoundTag();
        out.putString("test.file", value);
        return out;
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void usbSwitchSelectsSixteenIndependentSticks(GameTestHelper h) {
        UsbSwitchBlockEntity machine = place(h, UsbSwitchBlock.Kind.USB);
        h.assertTrue(machine.items().getSlots() == 16, "USB switch exposes sixteen slots");
        h.assertTrue(machine.mode() == 0, "without selector cover GT6 starts at slot zero");
        for (int slot = 0; slot < 16; slot++) {
            ItemStack medium = stick(3);
            UsbDataMedia.writeStick(medium, 3, file("file-" + slot));
            machine.items().setStackInSlot(slot, medium);
            machine.setMode(slot);
            h.assertTrue("file-".concat(Integer.toString(slot)).equals(machine.readUsbData(Direction.NORTH, 3).getString("test.file")),
                    "mode " + slot + " reads its own stick");
        }
        machine.setMode(15);
        h.assertTrue(machine.readUsbData(Direction.SOUTH, 2) == null, "USB 2 machine cannot read a USB 3 file");
        CompoundTag returned = machine.readUsbData(Direction.SOUTH, 3);
        returned.putString("test.file", "modified outside");
        h.assertTrue("file-15".equals(machine.readUsbData(Direction.SOUTH, 3).getString("test.file")),
                "port read returns a copy, not mutable inventory NBT");
        h.assertTrue(!machine.items().isItemValid(1, drive(3)), "USB switch rejects an HDD");
        DataSwitchMenu menu = new DataSwitchMenu(1, h.makeMockSurvivalPlayer().getInventory(), machine);
        h.assertTrue(menu.slots.size() == 16 + 36 && menu.slots.get(15).x == 107 && menu.slots.get(15).y == 62,
                "USB GUI exposes the original 4x4 slot grid");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void hddSwitchWritesSelectedSlotAndPreservesOtherFiles(GameTestHelper h) {
        UsbSwitchBlockEntity machine = place(h, UsbSwitchBlock.Kind.HDD);
        h.assertTrue(machine.items().getSlots() == 1 && machine.items().isItemValid(0, drive(4)),
                "HDD switch accepts one drive");
        h.assertTrue(!machine.items().isItemValid(0, stick(4)), "HDD switch rejects USB sticks in its drive slot");
        machine.items().setStackInSlot(0, drive(4));
        for (int slot = 0; slot < 16; slot++) {
            machine.setMode(slot);
            h.assertTrue(machine.writeUsbData(Direction.UP, 3, file("file-" + slot)), "writes slot " + slot);
        }
        machine.setMode(5);
        h.assertTrue("file-5".equals(machine.readUsbData(Direction.DOWN, 3).getString("test.file")),
                "selected HDD file is readable from any side");
        h.assertTrue(machine.readUsbData(Direction.DOWN, 2) == null, "tier-2 reader cannot read tier-3 file");
        h.assertTrue(machine.writeUsbData(Direction.DOWN, 0, null), "empty USB stick clears selected file");
        h.assertTrue(machine.readUsbData(Direction.DOWN, 3) == null, "selected file was cleared");
        machine.setMode(6);
        h.assertTrue("file-6".equals(machine.readUsbData(Direction.DOWN, 3).getString("test.file")),
                "adjacent HDD file remains untouched");
        CompoundTag saved = machine.saveWithoutMetadata();
        machine.items().setStackInSlot(0, ItemStack.EMPTY);
        machine.load(saved);
        h.assertTrue(machine.mode() == 6 && "file-6".equals(machine.readUsbData(Direction.NORTH, 3).getString("test.file")),
                "selector mode, HDD, and its files survive save/load");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void selectorCoverControlsPortMode(GameTestHelper h) {
        UsbSwitchBlockEntity machine = place(h, UsbSwitchBlock.Kind.USB);
        ItemStack selector = new ItemStack(GTTechnological.get("button_panel_selector"));
        h.assertTrue(machine.attachCover(Direction.UP, selector), "selector cover attaches to USB switch");
        h.assertTrue(machine.panels().click(Direction.UP, .35, .60), "selector accepts a button click");
        h.assertTrue(machine.mode() == 9, "4x4 selector picks mode 9");
        CompoundTag saved = machine.saveWithoutMetadata();
        machine.removeCover(Direction.UP);
        machine.load(saved);
        h.assertTrue(machine.mode() == 9 && !machine.getCover(Direction.UP).isEmpty(),
                "mode and cover persist together");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void removingSelectorReturnsBothSwitchesToSlotZero(GameTestHelper h) {
        for (UsbSwitchBlock.Kind kind : UsbSwitchBlock.Kind.values()) {
            UsbSwitchBlockEntity machine = place(h, kind);
            if (kind == UsbSwitchBlock.Kind.USB) {
                ItemStack zero = stick(3);
                ItemStack nine = stick(3);
                UsbDataMedia.writeStick(zero, 3, file("zero"));
                UsbDataMedia.writeStick(nine, 3, file("nine"));
                machine.items().setStackInSlot(0, zero);
                machine.items().setStackInSlot(9, nine);
            } else {
                ItemStack medium = drive(3);
                UsbDataMedia.writeDrive(medium, 0, 3, file("zero"));
                UsbDataMedia.writeDrive(medium, 9, 3, file("nine"));
                machine.items().setStackInSlot(0, medium);
            }
            ItemStack selector = new ItemStack(GTTechnological.get("button_panel_selector"));
            h.assertTrue(machine.attachCover(Direction.UP, selector), kind + " accepts a selector");
            h.assertTrue(machine.panels().click(Direction.UP, .35, .60), kind + " selector clicks slot 9");
            h.assertTrue("nine".equals(machine.readUsbData(Direction.NORTH, 3).getString("test.file")),
                    kind + " reads selected slot 9");
            h.assertTrue(!machine.removeCover(Direction.UP).isEmpty(), kind + " removes selector");
            h.assertTrue(machine.mode() == 0 && "zero".equals(
                    machine.readUsbData(Direction.NORTH, 3).getString("test.file")),
                    kind + " returns to slot 0 after selector removal");
        }
        h.succeed();
    }
}
