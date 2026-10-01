package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.LaserConverterBlock;
import com.gregtech.gregtech.block.energy.LaserFiberBlock;
import com.gregtech.gregtech.blockentity.energy.LaserFiberBlockEntity;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTLasers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 laserometer samples the previous tick's successful LU traffic through its adjacent fiber. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LaserometerRepairTests {
    private LaserometerRepairTests() {}

    private static void tickFiber(GameTestHelper helper,BlockPos relative) {
        var world=helper.getLevel();var pos=helper.absolutePos(relative);
        var state=world.getBlockState(pos);
        var fiber=(LaserFiberBlockEntity)world.getBlockEntity(pos);
        var ticker=((LaserFiberBlock)state.getBlock()).getTicker(world,state,GTBlockEntities.LASER_FIBER.get());
        helper.assertTrue(ticker!=null,"the server actually ticks the laser fiber transfer window");
        ticker.tick(world,pos,state,fiber);
    }

    @GameTest(template="test_blueprint_empty")
    public static void laserometerReadsOnlyPreviousTickTraffic(GameTestHelper helper) {
        BlockPos source=new BlockPos(1,2,2), first=new BlockPos(2,2,2);
        BlockPos second=new BlockPos(3,2,2), receiver=new BlockPos(4,2,2);
        BlockPos nearSensor=new BlockPos(2,2,1), farSensor=new BlockPos(3,2,3);
        helper.setBlock(source,GTLasers.CO2_LASER_LV.get().defaultBlockState()
                .setValue(LaserConverterBlock.FACING,Direction.EAST));
        helper.setBlock(receiver,GTLasers.LASER_ABSORBER_LV.get().defaultBlockState()
                .setValue(LaserConverterBlock.FACING,Direction.EAST));
        helper.setBlock(first,GTLasers.LASER_FIBER_WIRE.get());
        helper.setBlock(second,GTLasers.LASER_FIBER_WIRE.get());
        var meterBlock=ForgeRegistries.BLOCKS.getValue(GregTech.id("sensor_laserometer"));
        helper.assertTrue(meterBlock!=null,"GT6 laserometer is registered");
        helper.setBlock(nearSensor,meterBlock.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.NORTH));
        helper.setBlock(farSensor,meterBlock.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.SOUTH));

        var world=helper.getLevel();
        var fiber1=(LaserFiberBlockEntity)world.getBlockEntity(helper.absolutePos(first));
        var fiber2=(LaserFiberBlockEntity)world.getBlockEntity(helper.absolutePos(second));
        var meter1=(SensorBlockEntity)world.getBlockEntity(helper.absolutePos(nearSensor));
        var meter2=(SensorBlockEntity)world.getBlockEntity(helper.absolutePos(farSensor));
        helper.assertTrue(fiber1!=null&&fiber2!=null&&meter1!=null&&meter2!=null,
                "both fibers and both oppositely facing meters have block entities");

        helper.assertTrue(fiber1.doEnergyInjection(GregTechTags.Energy.LU,Direction.WEST,32,1,false)==1,
                "simulated packet finds the receiver");
        tickFiber(helper,first);tickFiber(helper,second);
        helper.assertTrue(fiber1.transferredLast()==0&&fiber2.transferredLast()==0,
                "simulation does not record LU on either fiber");

        helper.assertTrue(fiber1.doEnergyInjection(GregTechTags.Energy.LU,Direction.WEST,32,1,true)==1,
                "the first real packet reaches the receiver");
        helper.assertTrue(fiber1.doEnergyInjection(GregTechTags.Energy.LU,Direction.WEST,32,1,true)==1,
                "the second real packet also reaches the receiver");
        helper.assertTrue(fiber1.transferredLast()==0&&fiber2.transferredLast()==0,
                "the current tick's traffic is not yet the laserometer reading");
        tickFiber(helper,first);tickFiber(helper,second);
        helper.assertTrue(fiber1.transferredLast()==64&&fiber2.transferredLast()==64,
                "every fiber on the successful route records both 32-LU packets");

        SensorBlockEntity.measure(world,helper.absolutePos(nearSensor),meter1.getBlockState(),meter1);
        SensorBlockEntity.measure(world,helper.absolutePos(farSensor),meter2.getBlockState(),meter2);
        helper.assertTrue(meter1.value()==64&&meter2.value()==64,
                "meters mounted on either side read the adjacent fiber, independent of wire connection");
        var clientCopy=new SensorBlockEntity(helper.absolutePos(nearSensor),meter1.getBlockState());
        clientCopy.handleUpdateTag(meter1.getUpdateTag());
        helper.assertTrue(clientCopy.value()==64,"the sensor reading synchronizes to clients");

        tickFiber(helper,first);tickFiber(helper,second);
        SensorBlockEntity.measure(world,helper.absolutePos(nearSensor),meter1.getBlockState(),meter1);
        helper.assertTrue(fiber1.transferredLast()==0&&fiber2.transferredLast()==0&&meter1.value()==0,
                "an idle tick clears the prior transfer reading");
        helper.succeed();
    }
}
