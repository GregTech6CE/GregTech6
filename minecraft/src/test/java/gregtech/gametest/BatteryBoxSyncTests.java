package gregtech.gametest;

import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.item.PowerCellItem;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.engine.stack.NbtCompat;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class BatteryBoxSyncTests {
    private BatteryBoxSyncTests() {}

    @GameTest(template = "test_empty", batch = "battery_box_node")
    public static void batteryBoxNotifiesOnlyForChangedState(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = BuiltInRegistries.BLOCK.get(Mc.gregtechId("battery_box_lv")).defaultBlockState();
        var box = new CountingBox(pos, state);
        var control = box.machineControl(null);
        int before = box.changes;
        EnergyNodeBlockEntity.serverTick(level, pos, state, box);
        control.setEnabled(true);
        control.setMode(16);
        helper.assertTrue(box.changes == before, "an idle tick and unchanged controls leave the box clean");
        control.setMode(3);
        control.setEnabled(false);
        helper.assertTrue(box.changes == before + 2, "changing mode and stopping both mark the box");

        var cell = GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.LEAD_ACID, 1);
        var charged = new ItemStack(cell);
        long voltage = box.spec().inputRate();
        cell.setCharge(charged, 4 * voltage);
        box.installBattery(charged);
        before = box.changes;
        box.batteryEnergy().tick(1, level, pos);
        helper.assertTrue(box.changes > before && box.stored() > 0
                        && cell.stored(box.batteryInventory().getStackInSlot(0)) < 4 * voltage,
                "an in-place cell discharge marks the working buffer and inventory");
//? if >=1.21.1 {
        /*var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
        var menu = box.batteryMenu().createMenu(1, player.getInventory(), player);
        var slot = menu.getSlot(0);
        before = box.changes;
        cell.setCharge(slot.getItem(), 2 * voltage);
        slot.setChanged();
        helper.assertTrue(box.changes > before && cell.stored(box.batteryInventory().getStackInSlot(0)) == 2 * voltage,
                "marking a live menu slot after changing the same battery notifies its box");
        var update = NbtCompat.getUpdateTag(box, level.registryAccess());
        helper.assertTrue(update.getLong("gt.buffer") == box.stored() && update.getInt("gt.battery_mode") == 3
                        && update.contains("gt.batteries"), "the update retains the full battery data");

        var depleted = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof PowerCellItem power && power.spec().filled())
                .map(item -> (PowerCellItem) item).findFirst().orElseThrow();
        var oldCell = new ItemStack(depleted);
        depleted.setCharge(oldCell, 0);
        box.batteryInventory().setStackInSlot(0, oldCell);
        before = box.changes;
        long oldBuffer = box.stored();
        box.batteryEnergy().tick(21, level, pos);
        var replacement = BuiltInRegistries.ITEM.get(Mc.gregtechId(depleted.spec().emptyId()));
        helper.assertTrue(box.changes > before && box.stored() == oldBuffer
                        && box.batteryInventory().getStackInSlot(0).is(replacement),
                "replacing a depleted cell marks the inventory even when no energy moved");
        helper.succeed();
    }

    private static final class CountingBox extends EnergyNodeBlockEntity {
        private int changes;

        CountingBox(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            changes++;
        }
    }
}
