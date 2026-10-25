package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.sensor.*;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import com.gregtech.gregtech.api.machine.MachineControl;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.energy.ZpmEnergy;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTFluids;
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
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_sensor_progress")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
@net.minecraftforge.gametest.GameTestHolder("gregtech_sensor_progress")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
/** Explicit native host operations; not a claim of server tick-order or cold-JVM persistence parity. */
public final class SensorProgressRegressionTests {
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
                System.out.println("SENSOR_PROGRESS_CLEANUP " + name + " cells=" + saved.size());
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
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
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
        System.out.println("SENSOR_PROGRESS_OBSERVATION "+label+" value="+value+" maximum="+maximum);
    }
    private static BlockEntity place(GameTestHelper h, BlockPos pos, Block block) {
        h.assertTrue(block!=Blocks.AIR,"actual registered fixture block");
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var entity=h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity!=null&&entity.getType().isValid(entity.getBlockState()),"valid actual registered block entity");
        return entity;
    }
    private static void progress(GameTestHelper h, BlockEntity entity, long value, long maximum, String label) {
        var source=ProgressSources.find(entity,Direction.UP);
        h.assertTrue(source!=null&&source.value()==value&&source.maximum()==maximum,label+" live progress source");
        check(h,meter(h,entity.getBlockPos(),Direction.UP,"sensor_progressmeter"),value,maximum,label);
    }
    private static MassStorageBlockEntity storage(GameTestHelper h, BlockPos pos) {
        var barrel=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.inventory.ItemBarrelBlock)
                .sorted(java.util.Comparator.comparing(b->BuiltInRegistries.BLOCK.getKey(b).toString())).findFirst().orElseThrow();
        return (MassStorageBlockEntity)place(h,pos,barrel);
    }
    private static void insert(GameTestHelper h,MassStorageBlockEntity storage,ItemStack item,int count) {
        h.assertTrue(!item.isEmpty(),"registered bulk input");
        for(int remaining=count;remaining>0;){int amount=Math.min(item.getMaxStackSize(),remaining);
            h.assertTrue(storage.insert(item.copyWithCount(amount))==amount,"actual bulk insert "+amount);remaining-=amount;}
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void bulkStorageProgressUsesFullNominalCapacity(GameTestHelper h) {
        fixture(h,"bulk_progress",pos->{
            var storage=storage(h,pos);
            long maximum=storage.baseCapacity();
            h.assertTrue(maximum==5000||maximum==10000,"original wood barrel nominal capacity");
            insert(h,storage,new ItemStack(Items.IRON_INGOT),1024);
            progress(h,storage,1024,maximum,"bulk_1024");
            check(h,meter(h,pos,Direction.NORTH,"sensor_itemometer"),1024,maximum,"bulk_existing_item");
            storage.toggleOverflow();
            insert(h,storage,new ItemStack(Items.IRON_INGOT),(int)maximum-1024+128);
            progress(h,storage,maximum+128,maximum,"bulk_overflow_nominal");
            h.assertTrue(storage.maximumItems()==maximum+256,"overflow capacity stays distinct");
            h.assertTrue(storage.extractAmount(1).getCount()==1,"native extraction works before packing");
            long count=maximum+127;
            h.assertTrue(storage.onTape(h.getLevel(),pos,Direction.NORTH,null,ItemStack.EMPTY,count,0,false,0,0,0)==count
                    &&storage.isPacked(),"native tape operation packs without consuming inventory");
            progress(h,storage,count,maximum,"bulk_packed_progress");
            check(h,meter(h,pos,Direction.NORTH,"sensor_itemometer"),count,maximum,"bulk_packed_item");
            h.assertTrue(storage.extractAmount(64).isEmpty()&&storage.stored()==count,"packing still blocks automation");
            h.assertTrue(storage.unseal()&&storage.extractAmount(Integer.MAX_VALUE).getCount()==64,"unpacking keeps extraction stack cap");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void sealedBarrelProgressUsesFermentationNotFill(GameTestHelper h) {
        fixture(h,"barrel_progress",pos->{
            var tank=(TankBlockEntity)place(h,pos,block("drum_steel"));
            var juice=GTFluids.stack("Juice_Apple",50);
            h.assertTrue(!juice.isEmpty()&&tank.getFluidTank().fill(juice,IFluidHandler.FluidAction.EXECUTE)==50,"native juice fill50");
            progress(h,tank,0,0,"barrel_unsealed");
            tank.toggleSoftHammerState();
            for(int i=0;i<128;i++)TankBlockEntity.serverTick(h.getLevel(),pos,tank.getBlockState(),tank);
            h.assertTrue(tank.getSoftHammerState()&&tank.getFluidTank().getFluid().getAmount()==50,"sealed juice remains unchanged before completion");
            progress(h,tank,128,1024,"barrel_fermentation128");
            // This round trip checks the stored observation only. Resuming after load currently
            // loses work when the recipe cache resolves; tracked as a separate lifecycle regression.
            var saved=save(h,tank);load(h,tank,saved);
            progress(h,tank,128,1024,"barrel_saved_observation");
            tank.toggleSoftHammerState();
            progress(h,tank,0,0,"barrel_unseal_reset");
            tank.getFluidTank().setEmpty();
            h.assertTrue(tank.getFluidTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==1000,"native inert water fill");
            tank.toggleSoftHammerState();
            TankBlockEntity.serverTick(h.getLevel(),pos,tank.getBlockState(),tank);
            progress(h,tank,0,0,"barrel_water_not_progress");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void zpmProgressUsesWorkingBufferAndPreservesControls(GameTestHelper h) {
        fixture(h,"zpm_progress",pos->{
            for(String path:new String[]{"zpm_discharger_basic","zpm_discharger_advanced"}){
                h.getLevel().setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
                var state=block(path).defaultBlockState().setValue(com.gregtech.gregtech.block.energy.ZpmDischargerBlock.FACING,Direction.EAST);
                h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(pos,state);
                var zpm=(ZpmDischargerBlockEntity)h.getLevel().getBlockEntity(pos);
                var module=BuiltInRegistries.ITEM.stream().filter(i->ZpmEnergy.isModule(new ItemStack(i))).findFirst().orElseThrow();
                var charged=new ItemStack(module);ZpmEnergy.set(charged,2_000_000_000_000L);
                h.assertTrue(zpm.inventory().insertItem(0,charged,false).isEmpty(),"native one-module insertion "+path);
                zpm.tick();
                progress(h,zpm,5_242_880,41_943_040,path+"_refill40");
                h.assertTrue(zpm.totalEnergy()==2_000_000_000_000L,"refill conserves artifact plus buffer");
                var control=MachineControl.find(zpm,Direction.NORTH);
                h.assertTrue(control!=null&&control.enabled()&&control.running()&&!control.active()&&control.supportsSwitch()&&!control.supportsMode(),"existing switch and running roles preserved");
                var sink=progressSink(h,pos.east(),zpm.outputType(),Direction.WEST,131072);
                zpm.tick();
                h.assertTrue(sink.received==131072&&sink.packets==1&&sink.lastSize==131072&&sink.lastSide==Direction.WEST,
                        "native EnergyTransfer emission delivers actual exact front packet "+path);
                progress(h,zpm,5_111_808,41_943_040,path+"_emitted_one");
                h.assertTrue(control.active()&&zpm.totalEnergy()==1_999_999_868_928L,"activity and full energy conserve after emission");
                h.assertTrue(!control.setEnabled(false)&&zpm.stopped(),"existing control disables emitter");
                zpm.tick();progress(h,zpm,5_111_808,41_943_040,path+"_stopped");
                h.assertTrue(sink.packets==1&&!control.running()&&!control.active(),"stopped does not emit");
                var saved=save(h,zpm);load(h,zpm,saved);progress(h,zpm,5_111_808,41_943_040,path+"_saved_buffer");
                h.assertTrue(control.setEnabled(true),"existing control enables emitter");zpm.tick();
                h.assertTrue(sink.packets==2&&control.active(),"resumed native emission");
                progress(h,zpm,4_980_736,41_943_040,path+"_resumed");
                saved=save(h,zpm);saved.putLong("gt.zpm.buffer",Long.MAX_VALUE);load(h,zpm,saved);
                progress(h,zpm,41_943_040,41_943_040,path+"_saved_buffer_clamp");
                h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            }
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void bulkWeightReadsAllStoredItemsEvenWhenPacked(GameTestHelper h) {
        fixture(h,"bulk_weight",pos->{
            var storage=storage(h,pos);
            var ironBlock=com.gregtech.gregtech.registry.GTBlocks.getStack(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockSolid,Materials.Iron);
            insert(h,storage,ironBlock,256);
            String[] names={"sensor_weightometer_light","sensor_weightometer_medium","sensor_weightometric","sensor_weightometer_super_heavy"};
            long[] expected={65535,65535,2015,2};
            h.assertTrue(((Object)storage) instanceof WeightSensorSource,"bulk host exposes physical content weight, not automation inventory cap");
            System.out.println("SENSOR_PROGRESS_WEIGHT_DIAGNOSTIC stored="+storage.stored()+" template="+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(storage.template().getItem())+" composition="+com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(storage.template())+" singleKg="+com.gregtech.gregtech.api.sensor.PhysicalItemMass.kilograms(storage.template().copyWithCount(1))+" totalKg="+((WeightSensorSource)(Object)storage).weightKilograms(0));
            h.assertTrue(storage.stored()==256&&storage.template().getItem()==ironBlock.getItem(),"all 256 solid iron blocks retained without form conversion");
            h.assertTrue(Materials.Iron.getDensity()==(double)7.874F&&com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockSolid.getMaterialWeight()==9L*com.gregtech.gregtech.api.material.GTValues.U,"original iron density 7.874F and solid block composition 9U");
            // Original MT.java Iron density 7.874 and OP.blockSolid 9U; native float storage
            // gives 7.874F * 111.111111D * 9 * 256 = 2,015,744.016538943 kg.
            double actualWeight=((WeightSensorSource)(Object)storage).weightKilograms(0);
            h.assertTrue(Math.abs(actualWeight-2_015_744.016539)<0.000001,"physical 256 iron blocks at original density, not automation64; actual="+actualWeight);
            for(int i=0;i<names.length;i++)check(h,meter(h,pos,Direction.NORTH,names[i]),expected[i],65535,"bulk_256_"+names[i]);
            h.assertTrue(storage.onTape(h.getLevel(),pos,Direction.NORTH,null,ItemStack.EMPTY,256,0,false,0,0,0)==256,"native tape pack");
            h.assertTrue(storage.extractAmount(64).isEmpty(),"automation empty while packed");
            for(int i=0;i<names.length;i++)check(h,meter(h,pos,Direction.NORTH,names[i]),expected[i],65535,"packed_256_"+names[i]);
            storage.unseal();
            h.assertTrue(storage.extractAmount(Integer.MAX_VALUE).getCount()==64&&storage.stored()==192,"native extraction remains legal stack");
            check(h,meter(h,pos,Direction.NORTH,"sensor_weightometric"),1511,65535,"bulk_after_actual_extract");
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());storage=storage(h,pos);
            insert(h,storage,GTItems.getStack(MaterialPrefix.ingot,Materials.Iron,1),1);
            h.assertTrue(storage.insert(GTItems.getStack(MaterialPrefix.nugget,Materials.Iron,1))==1&&storage.stored()==1&&storage.partialUnits()>0,"native partial nugget retained outside stored stack");
            check(h,meter(h,pos,Direction.NORTH,"sensor_weightometer_medium"),874,65535,"bulk_partial_excluded");
            h.assertTrue(storage.insert(GTItems.getStack(MaterialPrefix.nugget,Materials.Iron,8))==8&&storage.stored()==2&&storage.partialUnits()==0,"native fractions complete second ingot");
            check(h,meter(h,pos,Direction.NORTH,"sensor_weightometer_medium"),1749,65535,"bulk_fraction_completed");
        });
    }
    /** Native EnergyTransfer endpoint fixture; real chest BE type, isolated in the owned volume. */
    private static final class ProgressEnergySink extends net.minecraft.world.level.block.entity.ChestBlockEntity
            implements com.gregtech.gregtech.api.energy.IEnergyBlock {
        final com.gregtech.gregtech.data.GregTechTags.Tag type;
        final Direction input;
        final long maxSize;
        long received, packets, calls, lastSize;
        Direction lastSide;
        ProgressEnergySink(BlockPos pos, BlockState state,
                com.gregtech.gregtech.data.GregTechTags.Tag type, Direction input, long maxSize) {
            super(pos,state); this.type=type; this.input=input; this.maxSize=maxSize;
        }
        @Override public boolean isEnergyType(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s, boolean output) { return !output&&t==type; }
        @Override public java.util.Collection<com.gregtech.gregtech.data.GregTechTags.Tag> getEnergyTypes(Direction s) { return java.util.List.of(type); }
        @Override public boolean isEnergyAcceptingFrom(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s, boolean theoretical) { return !isRemoved()&&t==type&&s==input; }
        @Override public boolean isEnergyEmittingTo(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s, boolean theoretical) { return false; }
        @Override public long doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s, long size, long amount, boolean execute) {
            if(!isEnergyAcceptingFrom(t,s,false)||size<=0||size>maxSize||amount<=0)return 0;
            long accepted=Math.min(amount,(16_777_216L-received)/size);
            if(execute&&accepted>0){received+=accepted*size;packets+=accepted;calls++;lastSize=size;lastSide=s;}
            return accepted;
        }
        @Override public long getEnergyDemanded(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s, long size) { return isEnergyAcceptingFrom(t,s,false)&&size>0&&size<=maxSize?(16_777_216L-received)/size:0; }
        @Override public long doEnergyExtraction(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s,long size,long amount,boolean execute){return 0;}
        @Override public long getEnergyOffered(com.gregtech.gregtech.data.GregTechTags.Tag t, Direction s,long size){return 0;}
        @Override public long getEnergySizeInputMin(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return t==type?1:0;}
        @Override public long getEnergySizeInputRecommended(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return t==type?maxSize:0;}
        @Override public long getEnergySizeInputMax(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return t==type?maxSize:0;}
        @Override public long getEnergySizeOutputMin(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return 0;}
        @Override public long getEnergySizeOutputRecommended(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return 0;}
        @Override public long getEnergySizeOutputMax(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return 0;}
        @Override public long getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Tag t,Direction s){return t==type?received:0;}
    }
    private static ProgressEnergySink progressSink(GameTestHelper h, BlockPos pos,
            com.gregtech.gregtech.data.GregTechTags.Tag type, Direction side, long maxSize) {
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
        var sink=new ProgressEnergySink(pos,Blocks.CHEST.defaultBlockState(),type,side,maxSize);
        h.getLevel().setBlockEntity(sink);
        h.assertTrue(h.getLevel().getBlockEntity(pos)==sink&&sink.getType().isValid(sink.getBlockState()),"registered chest-typed energy fixture installed");
        return sink;
    }
    // Requires ordinary net.minecraft imports used by parent. Conditional FluidStack and
    // IFluidHandler.FluidAction imports must select the loader (same as existing channel tests).
    private static com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity transportPipe(
            GameTestHelper h, BlockPos pos, String name, boolean east) {
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var b=block(name);
        h.assertTrue(b instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock,"registered pipe "+name);
        var state=b.defaultBlockState();
        for(var side:Direction.values())state=state.setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(side),side==Direction.EAST&&east);
        h.getLevel().setBlockAndUpdate(pos,state);
        // Placement callbacks can connect a neighbor; reset to this exact declared route.
        h.getLevel().setBlockAndUpdate(pos,state);
        return (com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void transportPipeTick(GameTestHelper h,com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity pipe) {
        com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity.serverTick(h.getLevel(),pipe.getBlockPos(),pipe.getBlockState(),pipe);
    }
    private static long transportPipeMaximum(com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity pipe) {
        return pipe.spec().capacity()*pipe.getTanks();
    }
    private static void fluidPipeProgress(GameTestHelper h,BlockPos pos) {
        var drumBlock=block("drum_stainless_steel");
        var sink=(com.gregtech.gregtech.blockentity.machine.TankBlockEntity)place(h,pos.east(),drumBlock);
        var pipe=transportPipe(h,pos,"pipe_quadruple_steel",true);
        h.assertTrue(pipe.getTanks()==4,"four actual registered pipe channels");
        h.assertTrue(pipe.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,500),FluidAction.EXECUTE)==500,"source admits exact500mB");
        transportPipeTick(h,pipe);
        h.assertTrue(sink.getFluidInTank(0).getAmount()==250&&pipe.getFluidInTank(0).getAmount()==250,
                "actual pipe to drum native capability transaction conserves500mB");
        progress(h,pipe,250,transportPipeMaximum(pipe),"pipe_actual_outgoing250");
        h.getLevel().setBlockAndUpdate(pos,pipe.getBlockState().setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.EAST,false));
        h.assertTrue(pipe.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,250),FluidAction.EXECUTE)==250,"refill blocked pipe");
        transportPipeTick(h,pipe);
        h.assertTrue(pipe.getFluidInTank(0).getAmount()==500&&sink.getFluidInTank(0).getAmount()==250,
                "closed face preserves stock and receiver");
        progress(h,pipe,0,transportPipeMaximum(pipe),"pipe_occupied_but_no_transfer");
        pipe.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);sink.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
        h.getLevel().setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.east(),Blocks.CAULDRON.defaultBlockState());
        pipe=transportPipe(h,pos,"pipe_huge_steel",true);
        h.assertTrue(pipe.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE)==1000,"cauldron source bucket");
        transportPipeTick(h,pipe);
        h.assertTrue(pipe.getFluidInTank(0).isEmpty()&&h.getLevel().getBlockState(pos.east()).is(Blocks.WATER_CAULDRON)
                &&h.getLevel().getBlockState(pos.east()).getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL)==3,
                "native priority cauldron transaction consumes exact1000mB");
        progress(h,pipe,0,transportPipeMaximum(pipe),"pipe_cauldron_not_counted_by_source");
        h.getLevel().setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
        pipe=transportPipe(h,pos,"pipe_medium_wood",false);
        var nitrogen=BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","nitrogen"));
        h.assertTrue(nitrogen!=net.minecraft.world.level.material.Fluids.EMPTY
                &&com.gregtech.gregtech.api.fluid.FluidHazards.isGas(nitrogen)
                &&!com.gregtech.gregtech.api.fluid.FluidHazards.isAcid(nitrogen)
                &&!com.gregtech.gregtech.api.fluid.FluidHazards.isPlasma(nitrogen)
                &&!com.gregtech.gregtech.api.fluid.FluidHazards.isMagic(nitrogen),"registered benign gas fixture, no random corrosion/magic destruction");
        h.assertTrue(pipe.fill(new FluidStack(nitrogen,100),FluidAction.EXECUTE)==100,"nonproof pipe accepts gas before safety tick");
        transportPipeTick(h,pipe);
        h.assertTrue(h.getLevel().getBlockEntity(pos)==pipe&&pipe.getFluidInTank(0).getAmount()==92,"native gas safety leaks exactly8mB");
        progress(h,pipe,8,transportPipeMaximum(pipe),"pipe_native_gas_loss8");
        pipe.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);transportPipeTick(h,pipe);
        progress(h,pipe,0,transportPipeMaximum(pipe),"pipe_empty_interval_reset");
    }
    private static void electricWireProgress(GameTestHelper h,BlockPos pos) {
        var selected=BuiltInRegistries.BLOCK.stream()
                .filter(b->b instanceof com.gregtech.gregtech.block.energy.ElectricWireBlock w
                        &&w.spec().amperage()>=2&&w.spec().amperage()<=16&&w.spec().voltage()>=64&&w.spec().lossPerBlock()>=0&&w.spec().lossPerBlock()<=16)
                .map(b->(com.gregtech.gregtech.block.energy.ElectricWireBlock)b).findFirst().orElseThrow();
        var wire=(com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity)place(h,pos,selected);
        var laserBlock=block("co2_laser_lv");
        h.assertTrue(laserBlock instanceof com.gregtech.gregtech.block.energy.LaserConverterBlock,"registered actual EU receiver");
        h.getLevel().setBlockAndUpdate(pos.east(),laserBlock.defaultBlockState()
                .setValue(com.gregtech.gregtech.block.energy.LaserConverterBlock.FACING,Direction.EAST));
        var laser=(com.gregtech.gregtech.blockentity.energy.LaserConverterBlockEntity)h.getLevel().getBlockEntity(pos.east());
        var state=wire.getBlockState();
        for(var side:Direction.values())state=state.setValue(com.gregtech.gregtech.block.energy.ElectricWireBlock.propFor(side),side==Direction.EAST);
        h.getLevel().setBlockAndUpdate(pos,state);
        wire.serverTick();
        h.assertTrue(wire.transferElectricity(Direction.WEST,16+wire.lossPerBlock(),2,new java.util.HashSet<>())==2,
                "wire sends two real16EU packets through loss into laser");
        h.assertTrue(laser.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.WEST)==32,"receiver credited32EU, not counter-only injection");
        progress(h,wire,2,wire.amperage(),"wire_two_amperes_low_voltage");
        wire.serverTick();
        h.assertTrue(wire.lastWattage()==32&&wire.transferredAmperes()==0,"wire next tick records wattage and clears current amps");
        progress(h,wire,0,wire.amperage(),"wire_interval_reset");
        laser.tick();
        h.assertTrue(laser.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.WEST)==0,"native laser consumes previous interval");
        h.assertTrue(wire.transferElectricity(Direction.WEST,32+wire.lossPerBlock(),2,new java.util.HashSet<>())==2
                &&laser.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.EU,Direction.WEST)==64,"same two amps deliver64EU at doubled voltage");
        progress(h,wire,2,wire.amperage(),"wire_two_amperes_high_voltage");
        wire.serverTick();
        h.assertTrue(wire.lastWattage()==64&&wire.burnCounter()==0&&h.getLevel().getBlockEntity(pos)==wire,"actual safe transfer changes energy not amp progress");
    }
    private static void axleProgress(GameTestHelper h,BlockPos pos) {
        var selected=BuiltInRegistries.BLOCK.stream()
                .filter(b->b instanceof com.gregtech.gregtech.block.energy.AxleBlock a&&a.spec().maxPower()>=2&&a.spec().maxPower()<=65535&&a.spec().maxSpeed()>=32)
                .map(b->(com.gregtech.gregtech.block.energy.AxleBlock)b).findFirst().orElseThrow();
        var axle=(com.gregtech.gregtech.blockentity.energy.AxleBlockEntity)place(h,pos,selected);
        var engineBlock=block("engine_rotation_bronze");
        h.getLevel().setBlockAndUpdate(pos.east(),engineBlock.defaultBlockState()
                .setValue(com.gregtech.gregtech.block.machine.EngineBlock.FACING,Direction.NORTH));
        var engine=(com.gregtech.gregtech.blockentity.machine.KineticRotationEngineBlockEntity)h.getLevel().getBlockEntity(pos.east());
        var state=axle.getBlockState();
        for(var side:Direction.values())state=state.setValue(com.gregtech.gregtech.block.energy.AxleBlock.propFor(side),side.getAxis()==Direction.Axis.X);
        h.getLevel().setBlockAndUpdate(pos,state);
        axle.serverTick();
        h.assertTrue(axle.transferRotation(Direction.WEST,16,2,new java.util.HashSet<>())==2&&engine.storedRu()==0,
                "original axle first interval spins up without emitting to real receiver");
        progress(h,axle,2,axle.maxPower(),"axle_spinup_consumed_power");
        axle.serverTick();
        h.assertTrue(axle.transferredLast()==32,"spinup energy32 retained independently");
        progress(h,axle,0,axle.maxPower(),"axle_interval_reset");
        h.assertTrue(axle.transferRotation(Direction.WEST,16,2,new java.util.HashSet<>())==2&&engine.storedRu()==32,
                "spinning axle sends two16RU packets into actual radial rotation engine");
        progress(h,axle,2,axle.maxPower(),"axle_two_power_at_speed16");
        axle.serverTick();
        com.gregtech.gregtech.blockentity.machine.KineticRotationEngineBlockEntity.serverTick(h.getLevel(),engine.getBlockPos(),engine.getBlockState(),engine);
        h.assertTrue(engine.storedRu()==0,"native engine consumes low-speed supply");
        h.assertTrue(axle.transferRotation(Direction.WEST,32,2,new java.util.HashSet<>())==2&&engine.storedRu()==64,
                "same power at doubled speed delivers64RU");
        progress(h,axle,2,axle.maxPower(),"axle_two_power_at_speed32");
        axle.serverTick();
        h.assertTrue(axle.transferredLast()==64&&h.getLevel().getBlockEntity(pos)==axle,"higher energy does not change two-unit power progress or overload safe axle");
    }
    private static net.minecraft.world.item.ItemStack progressCoverItem(String path) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", path));
        if (item == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("missing cover " + path);
        return new net.minecraft.world.item.ItemStack(item);
    }

    private static void seedSealedProgress(GameTestHelper h,
            com.gregtech.gregtech.blockentity.machine.TankBlockEntity tank, long value, long maximum) {
        // Only the input state is seeded here. The assertion under test is actual cover/relay observation;
        // the separate barrel case exercises the native fermentation transaction itself.
        var tag = save(h, tank);
        tag.putBoolean("gt.soft_hammer", true);
        tag.putLong("gt.sealed_time", value);
        tag.putLong("gt.max_sealed_time", maximum);
        load(h, tank, tag);
        h.assertTrue(tank.getSealedTime() == value && tank.getMaxSealedTime() == maximum,
                "native barrel accepts the explicit observation fixture");
    }

    private static void progressCoverAndScannerBody(GameTestHelper h, BlockPos pos) {
        var tank = (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) place(h, pos, block("drum_steel"));
//? if >=1.21.1 {
        /*h.assertTrue(tank.getFluidTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                net.minecraft.world.level.material.Fluids.WATER, 1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000,
                "native tank has a deliberately different fill ratio");
*///?} else {
        h.assertTrue(tank.getFluidTank().fill(new net.minecraftforge.fluids.FluidStack(
                net.minecraft.world.level.material.Fluids.WATER, 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000,
                "native tank has a deliberately different fill ratio");
//?}
        seedSealedProgress(h, tank, 120, 240);
        h.assertTrue(com.gregtech.gregtech.api.machine.MachineControl.find(tank, Direction.NORTH) == null,
                "passive source must not advertise global machine controls");
        var scanBefore = new java.util.ArrayList<String>();
        long costBefore = com.gregtech.gregtech.item.behavior.BehaviorScanner.scan(h.getLevel(), pos,
                Direction.NORTH, com.gregtech.gregtech.item.behavior.BehaviorScanner.PORTABLE_LEVEL, null, scanBefore);
        h.assertTrue(costBefore == 512 && scanBefore.stream().anyMatch(s -> s.startsWith("Tank 0: 1000 / ")),
                "existing native fluid section and exact scanner cost are retained");
        h.assertTrue(scanBefore.stream().noneMatch(s -> s.startsWith("State:") || s.startsWith("Progress:")
                        || s.contains(" --- Running:")), "no invented switch/running/progress scanner sections");
        for (var path : java.util.List.of("machine_status_display_cover", "manual_selector", "redstone_machine_switch"))
            h.assertTrue(!tank.panels().canAttach(Direction.SOUTH, progressCoverItem(path)),
                    "progress-only source must not admit unrelated control cover " + path);
        h.assertTrue(tank.attachCover(Direction.NORTH, progressCoverItem("progress_sensor")),
                "actual registered progress cover attaches to passive native barrel");
        tank.panels().afterTick();
        h.assertTrue(tank.panels().signal(Direction.NORTH) == 7
                        && com.gregtech.gregtech.content.cover.PanelCoverRuntime.value(tank.getCover(Direction.NORTH)) == 7,
                "native half-progress scale is seven, not fluid fill ratio or standalone meter rounding");
        h.assertTrue(tank.panels().configure(Direction.NORTH, false, false), "native screwdriver configuration inverts");
        h.assertTrue(com.gregtech.gregtech.content.cover.MachineCoverSpec.inverted(tank.getCover(Direction.NORTH))
                        && tank.panels().signal(Direction.NORTH) == 8,
                "inverted half-progress outputs eight");
        h.assertTrue(tank.panels().configure(Direction.NORTH, false, false), "native configuration uninverts");
        seedSealedProgress(h, tank, 0, 240); tank.panels().afterTick();
        h.assertTrue(tank.panels().signal(Direction.NORTH) == 0, "zero progress is zero despite nonempty tank");
        seedSealedProgress(h, tank, 240, 240); tank.panels().afterTick();
        h.assertTrue(tank.panels().signal(Direction.NORTH) == 15, "complete progress outputs fifteen");
        var scanAfter = new java.util.ArrayList<String>();
        long costAfter = com.gregtech.gregtech.item.behavior.BehaviorScanner.scan(h.getLevel(), pos,
                Direction.NORTH, com.gregtech.gregtech.item.behavior.BehaviorScanner.PORTABLE_LEVEL, null, scanAfter);
        h.assertTrue(costAfter == costBefore && scanAfter.equals(scanBefore),
                "native scanner text and returned charge stay exactly unchanged after progress cover and state changes");
        var removed = tank.removeCover(Direction.NORTH);
        h.assertTrue(!removed.isEmpty() && tank.getCover(Direction.NORTH).isEmpty()
                        && tank.panels().signal(Direction.NORTH) == 0,
                "actual cover removal clears the attached face signal");
        h.assertTrue(tank.attachCover(Direction.NORTH, removed), "removed cover can be attached again");
        tank.panels().afterTick();
        h.assertTrue(tank.panels().signal(Direction.NORTH) == 15
                        && tank.getFluidTank().getAmount() == 1000,
                "reattachment observes current progress without consuming fluid");
        System.out.println("SENSOR_PROGRESS_OBSERVATION native_cover half=7 inverted=8 empty=0 full=15 scanner_cost=" + costAfter);
    }

    private static com.gregtech.gregtech.block.misc.ExtenderBlockEntity progressRelay(
            GameTestHelper h, BlockPos pos, String id, Direction front, Direction secondary) {
        var state = block(id).defaultBlockState()
                .setValue(com.gregtech.gregtech.block.misc.SourceExtenderBlock.FACING, front)
                .setValue(com.gregtech.gregtech.block.misc.SourceExtenderBlock.SECONDARY, secondary);
        h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, state);
        var relay = (com.gregtech.gregtech.block.misc.ExtenderBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(relay != null && relay.getType().isValid(state), "actual registered relay " + id);
        return relay;
    }

    /** Test endpoint only: valid native barrel type, explicit side-sensitive progress and call accounting. */
    private static final class SideProgressTank extends com.gregtech.gregtech.blockentity.machine.TankBlockEntity
            implements com.gregtech.gregtech.api.sensor.ProgressSensorSource {
        final int base;
        int valueCalls, maximumCalls, valueSide = -2, maximumSide = -2;
        SideProgressTank(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int base) {
            super(pos, state); this.base = base;
        }
        @Override public long progressValue(int side) { valueCalls++; valueSide = side; return base + side; }
        @Override public long progressMaximum(int side) { maximumCalls++; maximumSide = side; return 1000 + base + side; }
        void resetCalls() { valueCalls = maximumCalls = 0; valueSide = maximumSide = -2; }
    }
    private static SideProgressTank sideProgressTank(GameTestHelper h, BlockPos pos, int base) {
        var state = block("drum_steel").defaultBlockState();
        h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, state);
        h.getLevel().removeBlockEntity(pos);
        var endpoint = new SideProgressTank(pos, state, base);
        h.getLevel().setBlockEntity(endpoint);
        h.assertTrue(h.getLevel().getBlockEntity(pos) == endpoint && endpoint.getType().isValid(state),
                "side-sensitive endpoint is a valid barrel BE inside the owned fixture");
        return endpoint;
    }
    private static void relayProgress(GameTestHelper h,
            com.gregtech.gregtech.block.misc.ExtenderBlockEntity relay, Direction side,
            long value, long maximum, String label) {
        var access = com.gregtech.gregtech.api.sensor.ProgressSources.find(relay, side);
        h.assertTrue(access != null && access.value() == value && access.maximum() == maximum, label);
        System.out.println("SENSOR_PROGRESS_OBSERVATION " + label + " value=" + value + " maximum=" + maximum);
    }
    private static final class CountingProgressRelay extends com.gregtech.gregtech.block.misc.ExtenderBlockEntity {
        int sourceLookups,controlLookups;
        CountingProgressRelay(BlockPos pos,BlockState state){super(pos,state);}
        @Override public ProgressSources.Access progressSource(Direction side){sourceLookups++;return super.progressSource(side);}
        @Override public MachineControl machineControl(Direction side){controlLookups++;return super.machineControl(side);}
    }
    private static void noRelayControls(GameTestHelper h,
            com.gregtech.gregtech.block.misc.ExtenderBlockEntity relay, Direction side, String label) {
        var c=MachineControl.find(relay,side);
        h.assertTrue(c!=null&&!c.available()&&!c.supportsProgress()&&!c.supportsSwitch()&&!c.supportsMode(),label);
    }
    private static void extenderAndBridgeProgressBody(GameTestHelper h, BlockPos pos) {
        var drum = (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) place(h, pos.east(), block("drum_steel"));
        seedSealedProgress(h, drum, 123, 456);
        var relay = progressRelay(h, pos, "universal_extender", Direction.WEST, Direction.EAST);
        relayProgress(h, relay, Direction.WEST, 123, 456, "extender_real_barrel");
        check(h, meter(h, pos, Direction.WEST, "sensor_progressmeter"), 123, 456, "extender_actual_meter");
        noRelayControls(h,relay,Direction.WEST,"passive progress relay does not invent live controls");
        for(var path:java.util.List.of("machine_status_display_cover","manual_selector","redstone_machine_switch"))
            h.assertTrue(!relay.panels().canAttach(Direction.WEST,progressCoverItem(path)),"relay rejects unrelated control cover "+path);
        var retained=ProgressSources.find(relay,Direction.WEST);
        seedSealedProgress(h,drum,120,240);
        h.assertTrue(relay.attachCover(Direction.WEST,progressCoverItem("progress_sensor")),"passive relay accepts actual progress cover");
        relay.panels().afterTick();
        h.assertTrue(relay.panels().signal(Direction.WEST)==7,"relay cover uses half fermentation not tank fill");
        h.assertTrue(relay.panels().configure(Direction.WEST,false,false)&&relay.panels().signal(Direction.WEST)==8,"relay cover inverted half progress");
        h.getLevel().setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
        h.assertTrue(retained.value()==0&&retained.maximum()==0,"retained source does not cache removed endpoint");
        noRelayControls(h,relay,Direction.WEST,"absent target retains no control capabilities");
        relay.panels().afterTick();
        h.assertTrue(relay.panels().signal(Direction.WEST)==15,"original absent progress is zero, inverted signal fifteen");
        h.assertTrue(relay.panels().configure(Direction.WEST,false,false)&&relay.panels().signal(Direction.WEST)==0,"uninverted absent signal zero");
        var replacement=sideProgressTank(h,pos.east(),300);
        replacement.resetCalls();
        h.assertTrue(retained.value()==304&&retained.maximum()==1304&&replacement.valueCalls==1&&replacement.maximumCalls==1,
                "retained source resolves replacement side once per observation");
        replacement.setRemoved();
        h.assertTrue(retained.value()==0&&retained.maximum()==0,"removed endpoint is not observed");
        replacement.clearRemoved();
        // Chunk lookup evicts a removed BE; clearing this detached reference cannot restore its world binding.
        h.getLevel().setBlockEntity(replacement);
        h.assertTrue(h.getLevel().getBlockEntity(pos.east())==replacement&&!replacement.isRemoved(),
                "recovered endpoint explicitly rebound through native world API");
        h.assertTrue(retained.value()==304&&retained.maximum()==1304,"removed endpoint guard recovers");
        relay.removeCover(Direction.WEST);
        // Restore the west meter cell before constructing the longer chain.
        h.getLevel().setBlockAndUpdate(pos.west(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var bridge = progressRelay(h, pos, "universal_bridge", Direction.NORTH, Direction.SOUTH);
        var east = sideProgressTank(h, pos.east(), 100);
        var south = sideProgressTank(h, pos.south(), 200);
        east.resetCalls(); south.resetCalls();
        relayProgress(h, bridge, Direction.WEST, 104, 1104, "bridge_west_routes_east_face_west");
        h.assertTrue(east.valueSide == Direction.WEST.ordinal() && east.maximumSide == Direction.WEST.ordinal(),
                "bridge passes the destination face, not its queried owner face blindly");
        relayProgress(h, bridge, Direction.NORTH, 202, 1202, "bridge_north_routes_south_face_north");
        h.assertTrue(south.valueSide == Direction.NORTH.ordinal() && south.maximumSide == Direction.NORTH.ordinal(),
                "independent bridge input resolves its own destination");
        h.assertTrue(com.gregtech.gregtech.api.machine.MachineControl.find(bridge, null) == null,
                "bridge null-side control remains unsupported");
        h.assertTrue(ProgressSources.find(bridge,null)==null,"bridge null-side progress remains unsupported");
        // Four actual relay blocks fit x=2..5, and their side-sensitive endpoint at x=6.
        var endpoint = sideProgressTank(h, pos.east(2), 100);
        com.gregtech.gregtech.block.misc.ExtenderBlockEntity first = null;
        var countedRelays=new java.util.ArrayList<CountingProgressRelay>();
        for (int offset = -2; offset <= 1; offset++) {
            var current = progressRelay(h, pos.offset(offset, 0, 0), "universal_extender", Direction.WEST, Direction.EAST);
            var counted=new CountingProgressRelay(current.getBlockPos(),current.getBlockState());
            h.getLevel().removeBlockEntity(current.getBlockPos());
            h.getLevel().setBlockEntity(counted);
            h.assertTrue(h.getLevel().getBlockEntity(counted.getBlockPos())==counted&&counted.getType().isValid(counted.getBlockState()),
                    "registered relay-typed counting fixture installed");
            countedRelays.add(counted);
            if (offset == -2) first = counted;
        }
        endpoint.resetCalls();
        for(var counted:countedRelays){counted.sourceLookups=0;counted.controlLookups=0;}
        var chain = com.gregtech.gregtech.api.sensor.ProgressSources.find(first, Direction.WEST);
        h.assertTrue(chain != null && endpoint.valueCalls == 0 && endpoint.maximumCalls == 0,
                "resolving a chain does not eagerly read endpoint measurements");
        h.assertTrue(chain.value() == 104 && endpoint.valueCalls == 1 && endpoint.maximumCalls == 0,
                "four-hop value traversal is lazy and linear, not eager value/max recursion");
        h.assertTrue(chain.maximum() == 1104 && endpoint.valueCalls == 1 && endpoint.maximumCalls == 1,
                "four-hop maximum traversal calls the endpoint once");
        h.assertTrue(countedRelays.stream().mapToInt(r->r.sourceLookups).sum()==7
                        &&countedRelays.stream().mapToInt(r->r.controlLookups).sum()==0,
                "four-hop read-only routing performs seven lazy source lookups and no recursive feature queries");
        // Both facing properties are independently writable in native SourceExtenderBlock.
        var loopA = progressRelay(h, pos, "universal_extender", Direction.EAST, Direction.EAST);
        progressRelay(h, pos.east(), "universal_extender", Direction.WEST, Direction.WEST);
        relayProgress(h, loopA, Direction.WEST, 0, 0, "extender_cycle_returns_zero");
        var cycleAccess=ProgressSources.find(loopA,Direction.WEST);
        noRelayControls(h,loopA,Direction.WEST,"cycle advertises no live control features");
        // Leaving a cycle must also release the thread-local visited set for the next normal read.
        drum = (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) place(h, pos.east(), block("drum_steel"));
        seedSealedProgress(h, drum, 12, 24);
        relayProgress(h, loopA, Direction.WEST, 12, 24, "extender_cycle_guard_recovers");
        h.assertTrue(cycleAccess.value()==12&&cycleAccess.maximum()==24,"retained cyclic source recovers without cached endpoint");
        for (var id : java.util.List.of("inventory_extender", "tank_extender", "inventory_tank_bridge")) {
            var passive = progressRelay(h, pos, id, Direction.WEST, Direction.EAST);
            h.assertTrue(com.gregtech.gregtech.api.machine.MachineControl.find(passive, Direction.WEST) == null
                            && com.gregtech.gregtech.api.sensor.ProgressSources.find(passive, Direction.WEST) == null,
                    "non-control relay stays unsupported " + id);
        }
        var zpmBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof com.gregtech.gregtech.block.energy.ZpmDischargerBlock).findFirst().orElseThrow();
        var zpm = (com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity) place(h, pos.east(), zpmBlock);
        var switchedRelay = progressRelay(h, pos, "universal_extender", Direction.WEST, Direction.EAST);
        var switchedControl = com.gregtech.gregtech.api.machine.MachineControl.find(switchedRelay, Direction.WEST);
        h.assertTrue(switchedControl != null && switchedControl.enabled() && !zpm.stopped(),
                "existing switchable endpoint is still readable through the relay");
        h.assertTrue(!switchedControl.setEnabled(false) && zpm.stopped(), "relay still delegates actual machine stop");
        h.assertTrue(switchedControl.setEnabled(true) && !zpm.stopped(), "relay still delegates actual machine resume");
        h.assertTrue(switchedControl.setMode(7) == 0 && switchedControl.mode() == 0,
                "endpoint with no mode does not gain a writable mode through progress routing");
        // Guard-only control: a detached native relay on the real ServerLevel performs no world writes.
        // This is NOT a placed cross-chunk lifecycle/reload assertion.
        var remotePos = new BlockPos(28_900_008, 200, 28_900_000);
        var remoteTarget = remotePos.north();
        h.assertTrue(!h.getLevel().hasChunkAt(remotePos) && !h.getLevel().hasChunkAt(remoteTarget),
                "unloaded guard coordinates are absent before the read");
        var remoteState = block("universal_extender").defaultBlockState()
                .setValue(com.gregtech.gregtech.block.misc.SourceExtenderBlock.FACING, Direction.NORTH)
                .setValue(com.gregtech.gregtech.block.misc.SourceExtenderBlock.SECONDARY, Direction.SOUTH);
        var detached = new com.gregtech.gregtech.block.misc.ExtenderBlockEntity(remotePos, remoteState);
        detached.setLevel(h.getLevel());
        try {
            relayProgress(h, detached, Direction.SOUTH, 0, 0, "detached_native_unloaded_guard");
            noRelayControls(h,detached,Direction.SOUTH,"unloaded target has no live control features");
            h.assertTrue(!h.getLevel().hasChunkAt(remotePos) && !h.getLevel().hasChunkAt(remoteTarget),
                    "progress lookup does not force-load the absent endpoint or owner");
        } finally { detached.setRemoved(); }
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void fluidPipeProgressTracksActualOutgoingVolume(GameTestHelper h) {
        fixture(h,"pipe_progress",pos->fluidPipeProgress(h,pos));
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void electricWireProgressReportsAmperesNotEnergy(GameTestHelper h) {
        fixture(h,"wire_progress",pos->electricWireProgress(h,pos));
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void axleProgressReportsPowerNotEnergy(GameTestHelper h) {
        fixture(h,"axle_progress",pos->axleProgress(h,pos));
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void progressCoverUsesSourceWithoutFakeMachineControls(GameTestHelper h) {
        fixture(h,"cover_progress",pos->progressCoverAndScannerBody(h,pos));
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void extenderAndBridgeRelayProgressWithOriginalGuards(GameTestHelper h) {
        fixture(h,"relay_progress",pos->extenderAndBridgeProgressBody(h,pos));
    }
}
