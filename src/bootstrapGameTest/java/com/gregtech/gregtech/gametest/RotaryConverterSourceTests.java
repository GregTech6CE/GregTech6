package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.block.energy.RotaryConverterBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** One finite native-world chain; source core boundaries are checked separately. Ordinary jars exclude this fixture. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_rotary_converters") @net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class RotaryConverterSourceTests {
    private static EnergyNodeBlockEntity place(GameTestHelper h, BlockPos pos, String id) {
        var block = (EnergyNodeBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:" + id));
        h.setBlock(pos, block.defaultBlockState().setValue(EnergyNodeBlock.FACING, Direction.EAST));
        return (EnergyNodeBlockEntity) h.getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h, EnergyNodeBlockEntity node) {
        EnergyNodeBlockEntity.serverTick(h.getLevel(), node.getBlockPos(), node.getBlockState(), node);
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void motorDynamoBatterySourceChainAndSavedControl(GameTestHelper h) {
        var motor = place(h, new BlockPos(2,3,2), "electric_motor_lv");
        var dynamo = place(h, new BlockPos(3,3,2), "electric_dynamo_lv");
        var sink = place(h, new BlockPos(4,3,2), "battery_box_lv");
        var battery = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:battery_lead_acid_lv")));
        h.assertTrue(sink.installBattery(battery), "real complete rechargeable battery installs");
        sink.batteryEnergy().tick(2, null, null);
        h.assertTrue(motor.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 64, 1, true) == 1, "source motor max EU packet accepted");
        tick(h, motor);
        h.assertTrue(motor.stored() == 0 && dynamo.stored() == 32, "native network converts 64EU to one 32RU packet");
        h.assertTrue(motor.machineControl(null).active() && motor.getBlockState().getValue(RotaryConverterBlock.ACTIVITY) == 2, "actual receiving neighbor gives source emitted and transient visual state");
        tick(h, dynamo);
        h.assertTrue(dynamo.stored() == 0 && sink.batteryEnergy().buffer() == 22, "native dynamo converts 32RU to 22EU without a rotor");

        motor.reverseMotor();
        motor.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 64, 1, true);
        tick(h, motor); tick(h, dynamo);
        h.assertTrue(sink.batteryEnergy().buffer() == 44, "negative RU and EU survive native energy gates without loss");

        motor.machineControl(null).setMode(1);
        motor.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 64, 1, true);
        tick(h, motor);
        h.assertTrue(motor.stored() == 4 && dynamo.stored() == 30, "source motor selector mode clamps RU and wastes 60EU");
        tick(h, dynamo);
        h.assertTrue(sink.batteryEnergy().buffer() == 64, "source dynamo floors 30RU to 20EU");
        motor.machineControl(null).setEnabled(false);
        var saved = motor.saveWithoutMetadata();
        var restored = new EnergyNodeBlockEntity(motor.getBlockPos(), motor.getBlockState());
        restored.load(saved);
        h.assertTrue(restored.stored() == 4 && restored.motorCounterClockwise()
                && restored.machineControl(null).mode() == 1 && !restored.machineControl(null).enabled(), "native world NBT retains exact buffer, mode, reverse and stopped state; this is not an independent restart");

        var idle = place(h, new BlockPos(2,3,5), "electric_motor_lv");
        idle.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 16, 1, true);
        tick(h, idle);
        h.assertTrue(idle.stored() == 0 && idle.machineControl(null).running() && !idle.machineControl(null).active(), "source no receiver still wastes energy and distinguishes possible from emitted");
        idle.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 16, 1, true);
        idle.machineControl(null).setEnabled(false);
        tick(h, idle);
        h.assertTrue(idle.stored() == 0 && idle.getBlockState().getValue(RotaryConverterBlock.ACTIVITY) == 0
                && idle.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, 32, 1, true) == 0, "source stopped converter drains old buffer, hides activity and rejects new energy");
        h.succeed();
    }
}
