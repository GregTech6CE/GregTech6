package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.inventory.UsbSwitchBlock;
import com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DataInventoryRepairTests {
    private static ItemStack drive(int tier) { return new ItemStack(GTTechnological.get("usb" + tier + "_hdd")); }
    private static ItemStack stick(int tier) { return new ItemStack(GTTechnological.get("usb" + tier + "_stick")); }
    private static ItemStack dataStick(int tier, String value) {
        ItemStack stack = stick(tier);
        CompoundTag data = new CompoundTag();
        data.putString("test.file", value);
        UsbDataMedia.writeStick(stack, tier, data);
        return stack;
    }
    private static void click(UsbSwitchBlockEntity machine, Player player, ItemStack held) {
        var state = machine.getBlockState();
        Direction face = state.getValue(UsbSwitchBlock.FACING);
        BlockPos pos = machine.getBlockPos();
        Vec3 point = Vec3.atCenterOf(pos).add(face.getStepX() * .5, 0, face.getStepZ() * .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        state.getBlock().use(state, machine.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(point, face, pos, false));
    }

    @GameTest(template = "test_empty")
    public static void clickingWithUsbStickWritesSelectedHddFileWithoutConsumingStick(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        for (Direction face : Direction.Plane.HORIZONTAL) {
            level.removeBlock(pos, false);
            level.setBlock(pos, GTStorage.HDD_SWITCH.get().defaultBlockState().setValue(UsbSwitchBlock.FACING, face), 3);
            var machine = (UsbSwitchBlockEntity) level.getBlockEntity(pos);
            var player = h.makeMockSurvivalPlayer();
            machine.items().setStackInSlot(0, drive(3));
            machine.setMode(11);
            ItemStack source = dataStick(3, "orientation-" + face);
            ItemStack before = source.copy();
            click(machine, player, source);
            h.assertTrue(ItemStack.matches(before, player.getMainHandItem()), "writing retains source in " + face);
            h.assertTrue(("orientation-" + face).equals(machine.readUsbData(face, 3).getString("test.file")),
                    "right click writes selected HDD file in " + face);
            h.assertTrue(machine.mode() == 11 && machine.readUsbData(face, 2) == null,
                    "face does not change the selected slot or tier gate");
            click(machine, player, stick(3));
            h.assertTrue(machine.readUsbData(face, 3) == null, "empty stick clears slot 11");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void writesRejectLowTierHddAndPreserveExistingFiles(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlock(pos, GTStorage.HDD_SWITCH.get().defaultBlockState(), 3);
        var machine = (UsbSwitchBlockEntity) h.getLevel().getBlockEntity(pos);
        ItemStack low = drive(1);
        low.getOrCreateTag().putString("custom", "preserved");
        machine.items().setStackInSlot(0, low);
        ItemStack before = low.copy();
        h.assertTrue(!machine.writeUsbData(Direction.NORTH, 3, dataStick(3, "source").getTag().getCompound("gt.usb.data")),
                "USB 1 HDD rejects a tier-3 file");
        h.assertTrue(ItemStack.matches(before, machine.items().getStackInSlot(0)), "rejected write leaves all HDD tags unchanged");
        machine.items().setStackInSlot(0, drive(3));
        for (int slot = 0; slot < 16; slot++) {
            machine.setMode(slot);
            h.assertTrue(machine.writeUsbData(Direction.UP, 3,
                    dataStick(3, "slot-" + slot).getTag().getCompound("gt.usb.data")), "write selected slot " + slot);
        }
        h.assertTrue(UsbDataMedia.readDrive(machine.items().getStackInSlot(0), 0, 3) != null
                && UsbDataMedia.readDrive(machine.items().getStackInSlot(0), 15, 3) != null,
                "all sixteen slots can be filled independently");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void removedInventoryCapabilitiesInvalidateAndDropOnlyOnce(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        for (boolean switchBlock : new boolean[]{true, false}) {
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
            level.setBlock(pos, (switchBlock ? GTStorage.USB_SWITCH.get() : GTStorage.BOTTLE_CRATE.get()).defaultBlockState(), 3);
            var entity = level.getBlockEntity(pos);
            var cap = entity.getCapability(ForgeCapabilities.ITEM_HANDLER);
            h.assertTrue(cap == entity.getCapability(ForgeCapabilities.ITEM_HANDLER), "queries reuse capability");
            var handler = cap.orElseThrow(AssertionError::new);
            var stack = switchBlock ? dataStick(3, "drop-fixture") : new ItemStack(Items.GLASS_BOTTLE, 7);
            h.assertTrue(handler.insertItem(0, stack, false).isEmpty(), "automation inserts fixture");
            var saved = entity.saveWithoutMetadata();
            entity.load(new CompoundTag());
            h.assertTrue(handler.getSlots() == (switchBlock ? 16 : 9) && handler.getStackInSlot(0).isEmpty(),
                    "empty load clears inventory but retains slot count");
            entity.load(saved);
            h.assertTrue(ItemStack.matches(stack, handler.getStackInSlot(0)), "save/load preserves inventory");
            entity.invalidateCaps();
            h.assertTrue(!cap.isPresent(), "cached interface invalidates");
            entity.reviveCaps();
            var revived = entity.getCapability(ForgeCapabilities.ITEM_HANDLER);
            h.assertTrue(revived.isPresent() && revived != cap, "revive publishes a new interface");
            if (switchBlock) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            else level.destroyBlock(pos, true);
            h.assertTrue(!revived.isPresent(), "world removal invalidates current interface");
            h.assertTrue(handler.getStackInSlot(0).isEmpty(), "old handler contains no dropped inventory");
            int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream().mapToInt(e -> {
                if (switchBlock) return e.getItem().is(stack.getItem()) ? e.getItem().getCount() : 0;
                var data = net.minecraft.world.item.BlockItem.getBlockEntityData(e.getItem());
                if (data == null) return 0;
                var savedItems = new net.minecraftforge.items.ItemStackHandler(9);
                savedItems.deserializeNBT(data.getCompound("gt_bottles"));
                return savedItems.getStackInSlot(0).getCount();
            }).sum();
            h.assertTrue(dropped == stack.getCount(), "nonplayer removal drops contents once");
        }
        h.succeed();
    }
}
