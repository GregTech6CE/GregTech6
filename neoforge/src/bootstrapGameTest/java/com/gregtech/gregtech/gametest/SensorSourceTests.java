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
    public static void heat_exchanger_long_state_and_real_fuel(GameTestHelper h) {
        var world=h.getLevel();var pos=h.absolutePos(new BlockPos(7,2,7));
        var block=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:heat_exchanger_main"));
        world.setBlockAndUpdate(pos,block.defaultBlockState());
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity)world.getBlockEntity(pos);
        for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)
            world.setBlockAndUpdate(pos.offset(cell.right(),cell.up(),cell.back()),com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(cell.part()).defaultBlockState());
        h.assertTrue(machine.isStructureOk(),"Original two-layer shell binds native parts");
        var recipe=com.gregtech.gregtech.data.FuelRecipeMaps.Hot.mRecipeList.stream().filter(x->x.mFluidInputs.length==1&&x.mFluidOutputs.length==1&&x.mEUt<0).findFirst().orElseThrow();
        var fuel=recipe.mFluidInputs[0].copy();fuel.setAmount(Math.min(163840,recipe.mFluidInputs[0].getAmount()*1000));
        int accepted=machine.fill(fuel,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        machine.tick();long used=accepted-machine.tankAmount(0),batches=used/recipe.mFluidInputs[0].getAmount();
        h.assertTrue(batches>0&&machine.tankAmount(1)==batches*recipe.mFluidOutputs[0].getAmount()
                &&machine.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.HU,null)==batches*(-recipe.mEUt)*recipe.mDuration,
                "Actual hot recipe consumes matching fuel and conserves cold output and heat");
        var data=machine.saveWithoutMetadata(world.registryAccess());data.putLong("gt.output",500000000L);data.putShort("gt.eff",(short)6250);
        data.putString("gt.energy.emitted","ENERGY.ELECTRICITY");data.putLong("gt.energy",8_000_000_000L);
        var cold=new net.minecraft.nbt.CompoundTag();cold.put("Fluid",recipe.mFluidOutputs[0].save(world.registryAccess()));cold.putLong("Amount",5_000_000_000L);
        data.put("gt.cold",cold);machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.tankCapacity(0)==5_000_000_000L&&machine.tankCapacity(1)==Long.MAX_VALUE
                &&machine.tankAmount(1)==5_000_000_000L&&machine.settings().efficiency()==6250,
                "Native large configured capacity and long cold save do not truncate to int");
        var roundtrip=machine.saveWithoutMetadata(world.registryAccess());data=roundtrip;machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.tankAmount(1)==5_000_000_000L,"Native save/load preserves long output exactly");
        h.assertTrue(machine.isEnergyEmittingTo(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.UP,false)
                &&!machine.isEnergyEmittingTo(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.DOWN,false)
                &&machine.getEnergySizeOutputMin(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.UP)==500000000L
                &&machine.getEnergySizeOutputMax(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.UP)==500000000L,
                "Source energy type, top emission and equal min/recommended/max");
        world.setBlockAndUpdate(pos.above().east(),Blocks.AIR.defaultBlockState());
        machine.tick();
        h.assertTrue(!machine.isStructureOk()&&machine.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.EU,null)==7_500_000_000L,
                "Original buffered energy dissipates through all eight outlets even with broken shell /no receivers");
        var legacy=new net.minecraft.nbt.CompoundTag();legacy.putLong("gt.hu",12345L);
        legacy.put("gt.cold",recipe.mFluidOutputs[0].save(world.registryAccess()));data=legacy;machine.loadWithComponents(data,world.registryAccess());
        h.assertTrue(machine.settings().rate()==16384&&machine.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.HU,null)==12345L
                &&machine.tankAmount(1)==recipe.mFluidOutputs[0].getAmount(),"Old native int tank and gt.hu remain readable");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_raw_progress_and_spawner(GameTestHelper h) {
        var world=h.getLevel();
        var targetPos=h.absolutePos(new BlockPos(5,1,5));
        var controller=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:von_da_graagg_generator"));
        world.setBlockAndUpdate(targetPos,controller.defaultBlockState());
        var machine=(com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity)world.getBlockEntity(targetPos);
        var saved=machine.saveWithoutMetadata(world.registryAccess());
        saved.putInt("gt.range",255); machine.loadWithComponents(saved,world.registryAccess());
        h.assertTrue(machine.progressValue(0)==255 && machine.progressMaximum(0)==256
                && machine.gibblValue(0)==255000 && machine.gibblMaximum(0)==256000,"Source range / compression domains");
        var sensorPos=targetPos.west();
        for(var name:java.util.List.of("sensor_progressmeter","sensor_gibblometer")) {
            var block=(SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+name));
            world.setBlockAndUpdate(sensorPos,block.defaultBlockState());
            var meter=(SensorBlockEntity)world.getBlockEntity(sensorPos);meter.setInputSide(Direction.EAST);
            SensorBlockEntity.measure(world,sensorPos,block.defaultBlockState(),meter);
            h.assertTrue(meter.value()==255,"Actual sensor exposes raw255 not percent99 " + name);
        }
        world.setBlockAndUpdate(targetPos,Blocks.SPAWNER.defaultBlockState());
        var spawner=(net.minecraft.world.level.block.entity.SpawnerBlockEntity)world.getBlockEntity(targetPos);
        var data=new net.minecraft.nbt.CompoundTag();data.putShort("Delay",(short)237);
        spawner.getSpawner().load(world,targetPos,data);
        var block=(SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:sensor_progressmeter"));
        world.setBlockAndUpdate(sensorPos,block.defaultBlockState());
        var meter=(SensorBlockEntity)world.getBlockEntity(sensorPos);meter.setInputSide(Direction.EAST);
        SensorBlockEntity.measure(world,sensorPos,block.defaultBlockState(),meter);
        h.assertTrue(meter.value()==237,"Actual vanilla countdown accessed through installed mixin");
        h.succeed();
    }

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

    @GameTest(template="test_empty",timeoutTicks=160)
    public static void original_parts_delegate_sensor_measurements(GameTestHelper h) {
        for (var source : com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData.ALL) {
            var block = (com.gregtech.gregtech.block.machine.MultiblockPortBlock) BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.parse("gregtech:" + source.path()));
            var part = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity) block.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            h.assertTrue(part.getType().isValid(block.defaultBlockState()) && part.sensorTarget() == part,
                    "All45 native factory bindings, unbound measures itself " + source.path());
        }
        var block = (com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock) BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse("gregtech:stainless_steel_boiler_main_barometer"));
        var origin = h.absolutePos(new BlockPos(4,2,4));
        h.getLevel().setBlockAndUpdate(origin,block.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,Direction.NORTH));
        var boiler = (com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity)h.getLevel().getBlockEntity(origin);
        for (var cell : com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs.LAYOUT.cells()) {
            var position = cell.at(origin,Direction.NORTH);
            if (position.equals(origin)) continue;
            var required = cell.role() == com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR ? Blocks.AIR
                    : cell.role() == com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT
                    ? com.gregtech.gregtech.registry.GTMultiblocks.HEAT_TRANSMITTER.get() : boiler.variant().wall();
            h.getLevel().setBlockAndUpdate(position,required.defaultBlockState());
        }
        var steam = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("gregtech:steam"));
        var seeded = new com.gregtech.gregtech.api.fluid.FluidTankGT(boiler.variant().steamCapacity());
        seeded.setFluid(new net.neoforged.neoforge.fluids.FluidStack(steam,1_234_567));
        var tank = new net.minecraft.nbt.CompoundTag(); seeded.writeToNBT(tank,h.getLevel().registryAccess());
        var saved = boiler.saveWithoutMetadata(h.getLevel().registryAccess()); saved.put("gt.steam",tank); boiler.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(boiler.isStructureOk(),"Real boiler binds its complete shell");
        var heatPos = origin.east().below().south();
        var heat = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity) h.getLevel().getBlockEntity(heatPos);
        h.assertTrue(heat.sensorTarget() == boiler,"Source sensor unwraps an energy-only port to controller");
        var fluids = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,heatPos,Direction.EAST);
        h.assertTrue(fluids == null || fluids.getTanks() == 0,"Sensor delegation does not expose transport through heat port");
        var meterPos = heatPos.east();
        for (var name : java.util.List.of("sensor_gibblometer","sensor_kilogibblometer","sensor_fluidometer")) {
            var meterBlock = (SensorBlock)BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("gregtech:"+name));
            h.getLevel().setBlockAndUpdate(meterPos,meterBlock.defaultBlockState());
            var meter = (SensorBlockEntity)h.getLevel().getBlockEntity(meterPos); meter.setInputSide(Direction.WEST);
            SensorBlockEntity.measure(h.getLevel(),meterPos,meterBlock.defaultBlockState(),meter);
            long expected = name.equals("sensor_gibblometer")?1234:name.equals("sensor_kilogibblometer")?1:1_234_567;
            h.assertTrue(meter.value() == expected,"Pressure /fluid reads controller, not restricted part " + name);
        }
        var meter = (SensorBlockEntity)h.getLevel().getBlockEntity(meterPos);
        var broken = origin.west(); h.getLevel().setBlockAndUpdate(broken,Blocks.AIR.defaultBlockState());
        h.assertTrue(!boiler.isStructureOk() && heat.sensorTarget() == heat,"Invalid shell releases measurement binding");
        SensorBlockEntity.measure(h.getLevel(),meterPos,meter.getBlockState(),meter);
        h.assertTrue(meter.value() == 0,"Released part cannot retain controller measurement");
        h.getLevel().setBlockAndUpdate(broken,boiler.variant().wall().defaultBlockState());
        h.assertTrue(boiler.isStructureOk(),"Repaired shell rebinds");
        SensorBlockEntity.measure(h.getLevel(),meterPos,meter.getBlockState(),meter);
        h.assertTrue(meter.value() == 1_234_567,"Reformed shell restores real measurement");
        h.getLevel().setBlockAndUpdate(origin,Blocks.AIR.defaultBlockState());
        SensorBlockEntity.measure(h.getLevel(),meterPos,meter.getBlockState(),meter);
        h.assertTrue(meter.value() == 0,"Removed controller leaves no stale measurement");
        h.succeed();
    }
}
