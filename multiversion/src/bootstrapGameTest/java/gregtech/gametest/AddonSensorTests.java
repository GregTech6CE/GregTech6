package gregtech.gametest;

import gregtech.block.sensor.SensorBlock;
import gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DirectionalBlock;

//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_sensor_source")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gregtech_sensor_source")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class AddonSensorTests {
    private AddonSensorTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void addon_sensor_reads_signal(GameTestHelper h) {
        var world = h.getLevel();
        var sensorPos = h.absolutePos(new BlockPos(4, 2, 4));
        var block = (SensorBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:sensor_signal"));
        h.assertTrue(block != null && block.addon() != null, "addon sensor block and item are registered");
        var state = block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH);
        world.setBlockAndUpdate(sensorPos, state);
        var targetPos = sensorPos.south();
        world.setBlockAndUpdate(targetPos, BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("addon_sample:sample_switch")).defaultBlockState());
        var machine = (BehaviorMachineBlockEntity) world.getBlockEntity(targetPos);
        machine.state().setBoolean("armed", true);
        var sensor = (SensorBlockEntity) world.getBlockEntity(sensorPos);
        SensorBlockEntity.measure(world, sensorPos, state, sensor);
        h.assertTrue(sensor.value() == 15, "addon sensor reads the coarse signal");
        sensor.control(0, 0, true, false);
        h.assertTrue(sensor.value() == 100 && sensor.signal() == 15,
                "percentage mode follows the addon reading");
        h.assertTrue(state.getSignal(world, sensorPos, Direction.NORTH) == 15,
                "comparator output follows the addon reading");
        h.succeed();
    }
}
