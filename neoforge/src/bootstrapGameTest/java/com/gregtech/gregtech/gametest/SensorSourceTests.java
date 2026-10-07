package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.sensor.SensorCatalog;
import com.gregtech.gregtech.block.sensor.SensorBlock;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_sensor_source")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class SensorSourceTests {
    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_factories_input_and_pressure(GameTestHelper h) {
        for (var entry : SensorCatalog.ALL) {
            var block = (SensorBlock) BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+entry.id()));
            var be = (SensorBlockEntity)block.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            h.assertTrue(be.kind().name().equals(entry.kind()) && be.inputSide() == Direction.SOUTH, "Actual source factory /default input " + entry.id());
        }
        var item = (SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:sensor_itemometer"));
        var pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(pos,item.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.east(),Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(pos.east());
        chest.setItem(0,new ItemStack(Items.IRON_INGOT,13));
        var sensor = (SensorBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(sensor.setInputSide(Direction.EAST) && !sensor.setInputSide(Direction.NORTH),"Independent input /front rejection");
        SensorBlockEntity.measure(h.getLevel(),pos,item.defaultBlockState(),sensor);
        h.assertTrue(sensor.value() == 13,"Actual selected east inventory, not default south");
        var saved = sensor.saveWithoutMetadata(h.getLevel().registryAccess()); sensor.resetInputSide(); sensor.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(sensor.inputSide() == Direction.EAST,"Native save/load retains independent input");
        item.toolStateChanged(h.getLevel(),pos,item.defaultBlockState());
        h.assertTrue(sensor.inputSide() == Direction.SOUTH,"Wrench display click restores opposite input");
        var old = saved.copy(); old.remove("gt.sensor_input"); sensor.loadWithComponents(old,h.getLevel().registryAccess());
        h.assertTrue(sensor.inputSide() == Direction.SOUTH,"Old native save without new key defaults behind display");

        var boilerBlock = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock boiler && boiler.variant().steamCapacity() > 1_234_567L)
                .findFirst().orElseThrow();
        var boilerPos = h.absolutePos(new BlockPos(1,1,4)); h.getLevel().setBlockAndUpdate(boilerPos,boilerBlock.defaultBlockState());
        var boiler = (com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity)h.getLevel().getBlockEntity(boilerPos);
        var steam = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("gregtech:steam"));
        var seeded = new com.gregtech.gregtech.api.fluid.FluidTankGT(boiler.variant().steamCapacity());
        seeded.setFluid(new net.neoforged.neoforge.fluids.FluidStack(steam,1_234_567));
        var tankTag = new net.minecraft.nbt.CompoundTag(); seeded.writeToNBT(tankTag,h.getLevel().registryAccess());
        var boilerSave = boiler.saveWithoutMetadata(h.getLevel().registryAccess()); boilerSave.put("gt.steam",tankTag); boiler.loadWithComponents(boilerSave,h.getLevel().registryAccess());
        h.assertTrue(boiler.gibblValue(0) == 1_234_567,"Actual source compressed steam amount");
        for (var path : java.util.List.of("sensor_gibblometer","sensor_kilogibblometer")) {
            var meterBlock = (SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+path));
            var meterPos = boilerPos.west(); h.getLevel().setBlockAndUpdate(meterPos,meterBlock.defaultBlockState());
            var meter = (SensorBlockEntity)h.getLevel().getBlockEntity(meterPos); meter.setInputSide(Direction.EAST);
            SensorBlockEntity.measure(h.getLevel(),meterPos,meterBlock.defaultBlockState(),meter);
            h.assertTrue(meter.value() == (path.equals("sensor_gibblometer")?1234:1),"Actual source pressure scale " + path);
        }
        var bathBlock = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock basic
                        && basic.basicSpec().machineName().equals("bath") && basic.basicSpec().tier()==1).findFirst().orElseThrow();
        var bathPos = h.absolutePos(new BlockPos(1,1,7)); h.getLevel().setBlockAndUpdate(bathPos,bathBlock.defaultBlockState());
        var bath = (com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)h.getLevel().getBlockEntity(bathPos);
        bath.getTanksInput()[0].setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,5000));
        bath.getTanksOutput()[0].setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,7000));
        h.assertTrue(bath.gibblValue(0)==5000,"Source basic compression counts input only, excludes output");
        h.succeed();
    }
}
