package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.sensor.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.multiblock.SharedLargeCrucibleStructure;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

//? if >=1.21.1 {
/*import com.gregtech.gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_sensor_interfaces")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gregtech_sensor_interfaces")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
/** Native measurement boundaries. These synchronous calls do not claim sampling-cadence parity. */
public final class SensorInterfaceRegressionTests {
    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
    }
    private static void fixture(GameTestHelper h, String name, java.util.function.Consumer<BlockPos> body) {
        h.runAfterDelay(2, () -> {
            var saved = new LinkedHashMap<BlockPos, BlockState>();
            for (var cursor : BlockPos.betweenClosed(h.absolutePos(new BlockPos(1,1,1)), h.absolutePos(new BlockPos(7,7,7)))) {
                var pos = cursor.immutable();
                h.assertTrue(h.getLevel().getBlockEntity(pos) == null, "owned fixture must not overwrite a block entity " + pos);
                saved.put(pos, h.getLevel().getBlockState(pos));
            }
            var bounds=new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(1,1,1))),net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(8,8,8))));
            var existingItems=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).stream()
                    .map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            try {
                saved.keySet().forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
                body.accept(h.absolutePos(new BlockPos(4,3,4)));
            } finally {
                // Remove tested block entities before restoring the original states.
                saved.keySet().forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
                saved.forEach((p,s) -> h.getLevel().setBlockAndUpdate(p,s));
                h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).stream()
                        .filter(entity -> !existingItems.contains(entity.getUUID())).forEach(net.minecraft.world.entity.Entity::discard);
                for (var entry : saved.entrySet()) h.assertTrue(h.getLevel().getBlockState(entry.getKey()).equals(entry.getValue())
                        && h.getLevel().getBlockEntity(entry.getKey()) == null, "fixture restored " + name);
                System.out.println("SENSOR_INTERFACE_CLEANUP " + name + " cells=" + saved.size());
            }
            h.succeed();
        });
    }
    private static CompoundTag save(GameTestHelper h, BlockEntity be) {
//? if >=1.21.1 {
        /*return be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return be.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity be, CompoundTag data) {
//? if >=1.21.1 {
        /*be.loadWithComponents(data,h.getLevel().registryAccess());
*///?} else {
        be.load(data);
//?}
    }
    private static SensorBlockEntity meter(GameTestHelper h, BlockPos target, Direction fromTarget, String name) {
        var pos = target.relative(fromTarget);
        var state = block(name).defaultBlockState().setValue(DirectionalBlock.FACING, fromTarget);
        h.getLevel().setBlockAndUpdate(pos,state);
        var meter = (SensorBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(meter != null, "registered meter " + name);
        return meter;
    }
    private static void check(GameTestHelper h, SensorBlockEntity meter, long value, long maximum, String label) {
        SensorBlockEntity.measure(h.getLevel(),meter.getBlockPos(),meter.getBlockState(),meter);
        var data = save(h,meter);
        h.assertTrue(meter.value()==value, label+" value expected="+value+" actual="+meter.value());
        h.assertTrue(data.getLong("gt.measured_max")==maximum,label+" maximum expected="+maximum+" actual="+data.getLong("gt.measured_max"));
        System.out.println("SENSOR_INTERFACE_OBSERVATION "+label+" value="+value+" maximum="+maximum);
    }
//? if >=1.21.1 {
    /*private static SmeltingCrucibleEntity small(GameTestHelper h,BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos,block("smelting_crucible_steel").defaultBlockState());
        return (SmeltingCrucibleEntity)h.getLevel().getBlockEntity(pos);
*///?} else {
    private static SmeltingCrucibleBlockEntity small(GameTestHelper h,BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos,block("smelting_crucible_steel").defaultBlockState());
        return (SmeltingCrucibleBlockEntity)h.getLevel().getBlockEntity(pos);
//?}
    }
    private static LargeCrucibleControllerBlockEntity large(GameTestHelper h,BlockPos pos) {
        var block = (com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock)block("large_steel_crucible");
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        for(var cell:SharedLargeCrucibleStructure.CELLS) if(!cell.air())
            h.getLevel().setBlockAndUpdate(pos.offset(cell.x(),cell.y(),cell.z()),block.variant().wall().defaultBlockState());
        var entity=(LargeCrucibleControllerBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(StructureTestSupport.ok(entity),"real large crucible forms");
        return entity;
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void smallCrucibleContentWeightAndScales(GameTestHelper h) {
        fixture(h,"small_weight",pos -> {
            var crucible=small(h,pos);
            h.assertTrue(crucible.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper,GTValues.U)),293),"admit copper content");
            var cache=GTItems.getStack(MaterialPrefix.ingot,Materials.Iron,4);
            var state=save(h,crucible);
//? if >=1.21.1 {
            /*var cacheHandler=new net.neoforged.neoforge.items.ItemStackHandler(1);
            cacheHandler.setStackInSlot(0,cache);state.put("gt.cache",cacheHandler.serializeNBT(h.getLevel().registryAccess()));
*///?} else {
            var cacheHandler=new net.minecraftforge.items.ItemStackHandler(1);
            cacheHandler.setStackInSlot(0,cache);state.put("gt.cache",cacheHandler.serializeNBT());
//?}
            load(h,crucible,state);
            h.assertTrue(crucible.getCacheStack().getCount()==4,"serialized actual cache contains four iron ingots");
            String[] meters={"sensor_weightometer_light","sensor_weightometer_medium","sensor_weightometric","sensor_weightometer_super_heavy"};
            long[] expected={65535,995,0,0};
            for(int i=0;i<meters.length;i++) {
                var meter=meter(h,pos,Direction.NORTH,meters[i]);
                check(h,meter,expected[i],65535,"small_"+meters[i]);
            }
            h.assertTrue(crucible.getContentView().size()==1&&crucible.getCacheStack().getCount()==4,"measurement mutates neither content nor cache");
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            for(String name:meters) {
                var meter=meter(h,pos,Direction.NORTH,name);
                check(h,meter,0,65535,"absent_"+name);
                var data=save(h,meter);data.putInt("gt.control_mode",SensorControl.Mode.FULL.ordinal());load(h,meter,data);
                SensorBlockEntity.measure(h.getLevel(),meter.getBlockPos(),meter.getBlockState(),meter);
                h.assertTrue(meter.signal()==0,"absent target is not FULL "+name);
            }
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void largeCrucibleWeightThroughBoundPart(GameTestHelper h) {
        fixture(h,"large_weight",pos -> {
            var crucible=large(h,pos);
            h.assertTrue(crucible.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper,GTValues.U),
                    CrucibleMaterialStack.of(Materials.Iron,GTValues.U)),293),"admit mixed large content");
            // The underside of the controller permits direct sensing without replacing a wall.
            var direct=meter(h,pos,Direction.DOWN,"sensor_weightometer_medium");
            check(h,direct,1870,65535,"large_direct_mixed");
            h.getLevel().setBlockAndUpdate(direct.getBlockPos(),Blocks.AIR.defaultBlockState());
            h.assertTrue(StructureTestSupport.ok(crucible),"large shell remains complete after direct sensor removal");
            var part=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(pos.east());
            h.assertTrue(part.sensorTarget()==crucible,"real bound structural part delegates");
            check(h,meter(h,pos.east(),Direction.EAST,"sensor_weightometer_medium"),1870,65535,"large_part_mixed");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void inventoryWeightUsesPhysicalComposition(GameTestHelper h) {
        fixture(h,"inventory_weight",pos -> {
            h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
            var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(pos);
            var meter=meter(h,pos,Direction.NORTH,"sensor_weightometer_medium");
            var raw=GTItems.getStack(MaterialPrefix.oreRaw,Materials.Redstone,3);
            h.assertTrue(!raw.isEmpty()&&raw.getCount()==3,"actual raw redstone fixture");
            chest.setItem(0,raw);check(h,meter,4232,65535,"raw_redstone_three");
            chest.setItem(0,new ItemStack(Items.IRON_INGOT,9));
            h.assertTrue(ItemMaterialRegistry.get(chest.getItem(0)).isPresent(),"vanilla iron explicit composition");
            check(h,meter,7874,65535,"explicit_iron_nine");
            var unknown=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("addon_sample","nonore_raw_probe"));
            h.assertTrue(unknown!=Items.AIR&&unknown.getClass()==Item.class&&ItemMaterialRegistry.base(unknown).isEmpty(),"registered genuinely unbound probe, not technical empty composition");
            chest.setItem(0,new ItemStack(unknown,3));check(h,meter,0,65535,"unknown_probe");
            var materialBlock=com.gregtech.gregtech.registry.GTBlocks.getStack(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockSolid,Materials.Iron);
            h.assertTrue(!materialBlock.isEmpty(),"registered iron block");
            chest.setItem(0,materialBlock);check(h,meter,7874,65535,"material_iron_block");
            var stone=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.stone.GTStoneBlock s
                    &&s.stoneType()==com.gregtech.gregtech.block.stone.StoneType.GRANITE_BLACK&&s.variant()==com.gregtech.gregtech.block.stone.StoneVariant.STONE).findFirst().orElseThrow();
            var slab=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.stone.GTStoneSlabBlock s
                    &&s.stoneMaterial().resolve()==((com.gregtech.gregtech.block.stone.GTStoneBlock)stone).stoneMaterial().resolve()
                    &&s.variant()==com.gregtech.gregtech.block.stone.StoneVariant.STONE).findFirst().orElseThrow();
            chest.setItem(0,new ItemStack(stone));check(h,meter,843,65535,"physical_stone");
            chest.setItem(0,new ItemStack(slab));check(h,meter,421,65535,"physical_stone_slab");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void crucibleCompressionReportsContentUnits(GameTestHelper h) {
        fixture(h,"compression",pos -> {
            var small=small(h,pos);
            h.assertTrue(small.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper,GTValues.U9)),293),"admit ninth ingot");
            h.assertTrue(((Object)small) instanceof CompressionSensorSource,"small crucible provides compression interface");
            var compressed=(CompressionSensorSource)(Object)small;
            h.assertTrue(compressed.gibblValue(0)==1000&&compressed.gibblMaximum(0)==144000,"small raw milligibbl from U9, capacity16U");
            check(h,meter(h,pos,Direction.NORTH,"sensor_gibblometer"),1,144,"small_ninth");
            h.assertTrue(small.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper,1)),293),"fractional increment admitted");
            h.assertTrue(compressed.gibblValue(0)==1001,"raw pressure rounds positive fraction upward before sensor scales it");
            h.assertTrue(small.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper,GTValues.U-GTValues.U9-1)),293),"complete one ingot");
            check(h,meter(h,pos,Direction.NORTH,"sensor_gibblometer"),9,144,"small_one_ingot");
            h.getLevel().setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            var large=large(h,pos);
            h.assertTrue(large.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper,GTValues.U)),293),"large one ingot");
            h.assertTrue(((Object)large) instanceof CompressionSensorSource,"large crucible inherits compression interface");
            h.assertTrue(((CompressionSensorSource)(Object)large).gibblMaximum(0)==3888000,"large432U raw capacity");
            check(h,meter(h,pos.east(),Direction.EAST,"sensor_gibblometer"),9,3888,"large_gibbl");
            check(h,meter(h,pos.east(),Direction.EAST,"sensor_kilogibblometer"),0,3,"large_kilogibbl");
        });
    }
    private static void temperature(GameTestHelper h,BlockEntity entity,long temperature) {
        var data=save(h,entity);data.putLong("gt.temperature",temperature);load(h,entity,data);
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void moldExposesNativeTemperature(GameTestHelper h) {
        fixture(h,"mold_temperature",pos -> {
            var block=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.machine.MoldBlock).findFirst().orElseThrow();
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
            var mold=(MoldBlockEntity)h.getLevel().getBlockEntity(pos);temperature(h,mold,777);
            check(h,meter(h,pos,Direction.NORTH,"sensor_thermometer"),777,mold.getMoldMaxTemperature(),"mold_native");
            temperature(h,mold,333);check(h,meter(h,pos,Direction.NORTH,"sensor_thermometer"),333,mold.getMoldMaxTemperature(),"mold_changed");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void pipeExposesNativeTemperature(GameTestHelper h) {
        fixture(h,"pipe_temperature",pos -> {
            var block=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock).findFirst().orElseThrow();
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
            var pipe=(FluidPipeBlockEntity)h.getLevel().getBlockEntity(pos);temperature(h,pipe,777);
            check(h,meter(h,pos,Direction.NORTH,"sensor_thermometer"),777,pipe.spec().maxTemperature(),"pipe_native");
            temperature(h,pipe,333);check(h,meter(h,pos,Direction.NORTH,"sensor_thermometer"),333,pipe.spec().maxTemperature(),"pipe_changed");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void stackometerCountsOccupiedSlots(GameTestHelper h) {
        fixture(h,"occupied_slots",pos -> {
            h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
            var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(chest.getContainerSize()==27,"actual chest has27slots on both loaders");
            var meter=meter(h,pos,Direction.NORTH,"sensor_stackometer");
            check(h,meter,0,27,"empty_chest");
            chest.setItem(0,new ItemStack(Items.COBBLESTONE,1));check(h,meter,1,27,"one_item_one_slot");
            chest.setItem(0,new ItemStack(Items.COBBLESTONE,64));check(h,meter,1,27,"full_stack_one_slot");
            chest.setItem(1,new ItemStack(Items.DIAMOND,1));check(h,meter,2,27,"two_occupied_slots");
            chest.clearContent();check(h,meter,0,27,"emptied_chest");
        });
    }
}
