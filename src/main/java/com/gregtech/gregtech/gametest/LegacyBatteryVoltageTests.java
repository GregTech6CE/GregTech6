package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.energy.BatteryBoxEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.BatteryItem;
import com.gregtech.gregtech.registry.GTElectricItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.List;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LegacyBatteryVoltageTests {
    @GameTest(template = "test_empty")
    public static void everyLegacyTierHonorsVoltagePacketLimitsAndSimulation(GameTestHelper h) {
        var batteries = List.of(GTElectricItems.BATTERY_LV.get(), GTElectricItems.BATTERY_MV.get(),
                GTElectricItems.BATTERY_HV.get(), GTElectricItems.BATTERY_EV.get(), GTElectricItems.BATTERY_IV.get());
        var eu = GregTechTags.Energy.EU;
        for (BatteryItem battery : batteries) {
            var stack = new ItemStack(battery);
            long v = 8L << (battery.tier() * 2), capacity = battery.getEnergyCapacity(stack, eu);
            long packets = Math.min(v, capacity / v);
            h.assertTrue(battery.canEnergyInjection(stack, eu, v / 2) && battery.canEnergyExtraction(stack, eu, v * 2),
                    "theoretical range is inclusive and independent of charge");
            for (long invalid : new long[]{0, v / 2 - 1, v * 2 + 1, Long.MIN_VALUE, Long.MAX_VALUE}) {
                h.assertTrue(battery.doEnergyInjection(eu, stack, invalid, Long.MAX_VALUE, h.getLevel(), BlockPos.ZERO, true) == 0,
                        "invalid voltage rejected: " + invalid);
            }
            h.assertTrue(!stack.hasTag(), "rejection never creates charge NBT");
            h.assertTrue(battery.doEnergyInjection(eu, stack, v, Long.MAX_VALUE, h.getLevel(), BlockPos.ZERO, false) == packets
                    && !stack.hasTag(), "simulation observes packet cap without mutation");
            h.assertTrue(battery.doEnergyInjection(eu, stack, -v, Long.MAX_VALUE, h.getLevel(), BlockPos.ZERO, true) == packets
                    && battery.getEnergyStored(stack, eu) == packets * v, "negative magnitude and per-call packet cap");
            h.assertTrue(battery.doEnergyExtraction(eu, stack, v * 4, 1, h.getLevel(), BlockPos.ZERO, true) == 0,
                    "stored charge cannot be extracted at higher tier");
            h.assertTrue(battery.doEnergyExtraction(eu, stack, -v, Long.MAX_VALUE, h.getLevel(), BlockPos.ZERO, false) == packets
                    && battery.getEnergyStored(stack, eu) == packets * v, "extraction simulation preserves charge");
            h.assertTrue(battery.doEnergyExtraction(eu, stack, -v, Long.MAX_VALUE, h.getLevel(), BlockPos.ZERO, true) == packets
                    && battery.getEnergyStored(stack, eu) == 0, "extraction consumes complete packets");
            stack.getOrCreateTag().putLong("gt.charge", capacity - 1);
            h.assertTrue(battery.doEnergyInjection(eu, stack, v, 1, h.getLevel(), BlockPos.ZERO, true) == 1
                    && battery.getEnergyStored(stack, eu) == capacity, "last packet clamps stored charge to original capacity");
            stack.setCount(2);
            var before = stack.getTag().copy();
            h.assertTrue(!battery.canEnergyExtraction(stack, eu, v)
                    && battery.doEnergyExtraction(eu, stack, v, 1, h.getLevel(), BlockPos.ZERO, true) == 0
                    && battery.doEnergyInjection(eu, stack, v, 1, h.getLevel(), BlockPos.ZERO, true) == 0
                    && before.equals(stack.getTag()), "malformed stacked battery cannot duplicate energy");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void batteryBoxCountsOnlyCompatibleLegacyVoltage(GameTestHelper h) {
        var lv = new ItemStack(GTElectricItems.BATTERY_LV.get());
        var mv = new ItemStack(GTElectricItems.BATTERY_MV.get());
        var cells = List.of(lv, mv);
        h.assertTrue(new BatteryBoxEnergy(cells, 32).providers() == 1, "LV box counts only LV battery");
        h.assertTrue(new BatteryBoxEnergy(cells, 128).providers() == 1, "MV box counts only MV battery");
        h.assertTrue(new BatteryBoxEnergy(cells, 512).providers() == 0, "HV box cannot upscale legacy batteries");
        lv.setCount(2);
        h.assertTrue(new BatteryBoxEnergy(cells, 32).providers() == 0, "invalid stacked battery is not an output provider");
        h.succeed();
    }
}
