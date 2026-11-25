package gregtech.gametest;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.engine.inventory.NativeItemInventories;
import gregtech.engine.stack.NbtCompat;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class BatteryBoxNodeTests {
    private BatteryBoxNodeTests() {}

    @GameTest(template = "test_empty", batch = "battery_box_node")
    public static void batteryBoxSlotsAndEnergyRoundTrip(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = BuiltInRegistries.BLOCK.get(Mc.gregtechId("battery_box_lv"));
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, state);
        var box = (EnergyNodeBlockEntity) level.getBlockEntity(pos);
        var automation = NativeItemInventories.at(level, pos, Direction.SOUTH);
        helper.assertTrue(automation != null && automation.getSlots() == 4, "LV box exposes four automation slots");
//? if >=1.21.1 {
        /*var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
        var menu = box.batteryMenu().createMenu(1, player.getInventory(), player);
        var battery = GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.LEAD_ACID, 1);
        var pair = new ItemStack(battery, 2);
        helper.assertTrue(automation.insertItem(2, pair, true).getCount() == 1 && !menu.getSlot(2).hasItem(),
                "simulated insertion reports room for one battery and leaves the menu slot empty");
        helper.assertTrue(automation.insertItem(2, pair, false).getCount() == 1 && menu.getSlot(2).getItem().is(battery),
                "committed automation insertion populates the same menu slot");

        EnergyNodeBlockEntity.serverTick(level, pos, state, box);
        var eu = GregTechTags.Energy.EU;
        long voltage = box.spec().inputRate();
        helper.assertTrue(box.doEnergyInjection(eu, Direction.NORTH, voltage, 1, true) == 0,
                "the output face rejects input");
        helper.assertTrue(box.doEnergyInjection(eu, Direction.SOUTH, voltage, 1, false) == 1 && box.stored() == 0,
                "simulated injection leaves the buffer empty");
        helper.assertTrue(box.doEnergyInjection(eu, Direction.SOUTH, voltage, 1, true) == 1,
                "the input face accepts a working-buffer packet");
        var charged = new ItemStack(battery);
        battery.setCharge(charged, voltage * 5);
        menu.getSlot(2).set(charged);
        helper.assertTrue(battery.stored(automation.getStackInSlot(2)) == voltage * 5,
                "automation sees the cell replaced through the menu");
        box.machineControl(null).setMode(3);
        box.machineControl(null).setEnabled(false);
        var saved = NbtCompat.saveWithoutMetadata(box, level.registryAccess());
        helper.assertTrue(saved.getLong("gt.buffer") == voltage && saved.getInt("gt.battery_mode") == 3,
                "the existing buffer and mode keys preserve the node state");

        var restoredPos = pos.east();
        level.setBlockAndUpdate(restoredPos, state);
        var restored = (EnergyNodeBlockEntity) level.getBlockEntity(restoredPos);
        NbtCompat.loadBlockEntity(restored, saved, level.registryAccess());
        var slots = restored.batteryInventory();
        helper.assertTrue(slots.getSlots() == 4 && slots.getStackInSlot(0).isEmpty()
                        && slots.getStackInSlot(1).isEmpty() && slots.getStackInSlot(3).isEmpty(),
                "loading preserves all vacant slot positions");
        helper.assertTrue(slots.getStackInSlot(2).is(battery) && slots.getStackInSlot(2).getCount() == 1
                        && restored.getEnergyStored(eu, null) == voltage * 5,
                "the charged cell returns to its original slot and supplies the capacitor readout");
        var control = restored.machineControl(null);
        helper.assertTrue(control.progress() == voltage && control.mode() == 3 && !control.enabled(),
                "loading restores the separate working buffer, output mode and stopped state");
        helper.succeed();
    }
}
