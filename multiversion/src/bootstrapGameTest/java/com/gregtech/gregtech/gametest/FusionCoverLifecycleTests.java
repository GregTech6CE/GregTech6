package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import com.gregtech.gregtech.content.cover.ComponentCoverRules;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.multiblock.FusionStructure;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Actual registered ring, native admission/charging/covers; fresh-BE load is not a JVM restart. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FusionCoverLifecycleTests {
    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag tag) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(tag, h.getLevel().registryAccess());
*///?} else {
        entity.load(tag);
//?}
    }
    private static ItemStack cover(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", name)));
    }
    private static void tick(FusionReactorControllerBlockEntity r) {
        FusionReactorControllerBlockEntity.serverTick(r.getLevel(), r.getBlockPos(), r.getBlockState(), r);
    }
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final RecipeMap map;
        final java.util.Map<BlockPos, Saved> saved = new java.util.LinkedHashMap<>();
        FusionReactorControllerBlockEntity r;
        Fixture(GameTestHelper h, int index) {
            this.h = h; pos = new BlockPos(25_000_000 + index * 128, 200, 25_000_000);
            map = new RecipeMap(null, "gt.recipe.test.fusion_covers_" + java.util.UUID.randomUUID(),
                    "Fusion cover fixture", "test", 1, 1, 1, 0, 0, 0, 1, false, false, false, false);
        }
        void build(int duration) {
            var positions = new java.util.ArrayList<BlockPos>(); positions.add(pos);
            for (var cell : FusionStructure.CELLS) positions.add(cell.at(pos, Direction.NORTH));
            int minX=positions.stream().mapToInt(BlockPos::getX).min().orElseThrow()-2;
            int maxX=positions.stream().mapToInt(BlockPos::getX).max().orElseThrow()+2;
            int minY=positions.stream().mapToInt(BlockPos::getY).min().orElseThrow()-2;
            int maxY=positions.stream().mapToInt(BlockPos::getY).max().orElseThrow()+2;
            int minZ=positions.stream().mapToInt(BlockPos::getZ).min().orElseThrow()-2;
            int maxZ=positions.stream().mapToInt(BlockPos::getZ).max().orElseThrow()+2;
            for (var p : BlockPos.betweenClosed(new BlockPos(minX,minY,minZ),new BlockPos(maxX,maxY,maxZ))) set(p,Blocks.AIR.defaultBlockState());
            // Neo registers this controller through BasicMachineDefinitions, not the Forge-only static handle.
            var controller=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce","fusion_reactor_main"));
            h.assertTrue(controller instanceof BasicMachineBlock basic&&basic.basicSpec().machineName().equals("fusionreactor"),
                    "real registered fusion controller exists on this loader");
            set(pos,controller.defaultBlockState().setValue(BasicMachineBlock.FACING,Direction.NORTH));
            for (var cell : FusionStructure.CELLS) set(cell.at(pos,Direction.NORTH),cell.block().defaultBlockState());
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof FusionReactorControllerBlockEntity,
                    "native registered controller creates dedicated fusion executor");
            r=(FusionReactorControllerBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(r.isStructureOk(),"actual registered fusion ring forms");
            map.mRecipeList.add(new Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},
                    null,null,null,null,duration,-8192,32768));
            r.setSpec(BasicMachineSpec.builder("fusion_cover_fixture",r.spec().material()).machineType("fusionreactor")
                    .energy(GregTechTags.Energy.TU,1).recipes(map).faces(FaceConfig.ALL_SIDES).build());
        }
        void set(BlockPos p, BlockState state) {
            if (!saved.containsKey(p)) {
                var entity=h.getLevel().getBlockEntity(p);
                saved.put(p.immutable(),new Saved(h.getLevel().getBlockState(p),entity==null?null:save(h,entity)));
            }
            h.getLevel().removeBlockEntity(p);
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
        void charge() {
            var cell=FusionStructure.CELLS.stream().filter(c->c.role()==Role.ENERGY_INPUT).findFirst().orElseThrow();
            var port=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(cell.at(pos,Direction.NORTH));
            h.assertTrue(port.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,16384,2,false)==2
                    && r.chargeRemaining()==32768,"native startup simulation preserves charge");
            h.assertTrue(port.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,16384,2,true)==2
                    && r.chargeRemaining()==0,"native LU port pays exactly one startup");
        }
        void reload() {
            var tag=save(h,r); var spec=r.spec(); var state=r.getBlockState();
            set(pos,state); r=(FusionReactorControllerBlockEntity)h.getLevel().getBlockEntity(pos);
            r.setSpec(spec); load(h,r,tag);
        }
        BasicMachineBlockEntity receiver() {
            var block=(BasicMachineBlock)BuiltInRegistries.BLOCK.stream()
                    .filter(b->b instanceof BasicMachineBlock basic&&basic.basicSpec().machineName().equals("crusher"))
                    .findFirst().orElseThrow();
            var target=pos.south(12); set(target,block.defaultBlockState());
            var receiver=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(target);
            receiver.setSpec(BasicMachineSpec.builder("fusion_cover_receiver",block.basicSpec().material()).machineType("test")
                    .energy(GregTechTags.Energy.EU,8192).energyRange(1,1_000_000).recipes(block.basicSpec().recipeMap())
                    .faces(FaceConfig.ALL_SIDES).build());
            return receiver;
        }
        @Override public void close() {
            try {
                for (var p:saved.keySet()) h.getLevel().removeBlockEntity(p);
                for(var entry:saved.entrySet()) {
                    var p=entry.getKey();var original=entry.getValue();
                    h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                    h.getLevel().setBlock(p,original.state(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                    var entity=h.getLevel().getBlockEntity(p);
                    if(entity!=null&&original.data()!=null)load(h,entity,original.data());
                }
            } finally { RecipeMap.unregister(map); }
        }
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void fusionTimerChargesAcrossWindowsAndRetainsProcessingAfterLoad(GameTestHelper h) {
        try(var f=new Fixture(h,0)) {
            f.build(40);f.r.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
            h.assertTrue(f.r.attachCover(Direction.NORTH,cover("auto_reboot_switch_1m")),"real timer attaches");
            for(int i=0;i<1189;i++)tick(f.r);
            h.assertTrue(f.r.inventory().getStackInSlot(0).getCount()==1,"timer closed before1190 does not reserve input");
            tick(f.r);
            h.assertTrue(f.r.chargeRemaining()==32768&&f.r.inventory().getStackInSlot(0).isEmpty()
                    &&f.r.machineControl(null).running()&&!f.r.machineControl(null).active(),"timer admits charge wait, passive not processing");
            for(int i=0;i<20;i++)tick(f.r);
            h.assertTrue(!f.r.machineControl(null).enabled()&&f.r.chargeRemaining()==32768,"long charge wait survives closed window");
            f.charge(); // Existing policy: native LU charging remains possible while control is stopped.
            for(int i=0;i<1179;i++)tick(f.r);
            h.assertTrue(f.r.machineControl(null).progress()==0,"no production before next admission window");
            var receiver=f.receiver();tick(f.r);
            for(int i=0;i<11;i++)tick(f.r);
            h.assertTrue(f.r.machineControl(null).active()&&f.r.machineControl(null).progress()==12
                    &&receiver.getEnergyTick()==12L*8192,"processing continues past2400 without timer cutting it off");
            f.reload();tick(f.r);
            h.assertTrue(f.r.machineControl(null).active()&&f.r.machineControl(null).progress()==13
                    &&f.r.chargeRemaining()==0&&receiver.getEnergyTick()==13L*8192
                    &&f.r.inventory().getStackInSlot(0).isEmpty(),"explicit processing latch survives fresh native load without repeat charge/input/EU");
        }
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void fusionControlSensorsStopAndBlockedOutputsConserveJob(GameTestHelper h) {
        try(var f=new Fixture(h,1)) {
            f.build(3);f.r.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));tick(f.r);
            var c=f.r.machineControl(null);
            h.assertTrue(c.running()&&!c.active()&&c.progress()==0&&c.progressMax()==3,"charging exposes real job but no processing");
            f.charge();var receiver=f.receiver();tick(f.r);
            h.assertTrue(c.running()&&c.active()&&c.progress()==1&&c.progressPerTick()==1,"productive control follows actual reaction");
            var sensorPos=f.pos.north();var sensorBlock=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce","sensor_progressmeter"));
            f.set(sensorPos,sensorBlock.defaultBlockState());var meter=(SensorBlockEntity)h.getLevel().getBlockEntity(sensorPos);
            meter.setInputSide(Direction.SOUTH);SensorBlockEntity.measure(h.getLevel(),sensorPos,meter.getBlockState(),meter);
            h.assertTrue(meter.value()==1,"native progress sensor reads dedicated fusion work");
            c.setEnabled(false);
            h.assertTrue(!c.running()&&!c.active()&&!f.r.isRunning()&&c.progress()==1&&f.r.chargeRemaining()==0,"stop is immediate and preserves job/charge");
            c.setEnabled(true);h.assertTrue(!c.running()&&!c.active(),"reenable cannot resurrect stale transient state beforetick");
            f.r.inventory().setStackInSlot(1,new ItemStack(Items.COBBLESTONE,64));tick(f.r);tick(f.r);
            h.assertTrue(c.active()&&c.progress()==3&&c.progressMax()==3&&c.progressPerTick()==0
                    &&receiver.getEnergyTick()==3L*8192,"blocked completed output remains processing but emits no further work");
            h.assertTrue(f.r.attachCover(Direction.UP,cover("activity_detector_processing")),"processing detector attaches");
            tick(f.r);h.assertTrue(f.r.getRedstoneSignal(Direction.UP)==15&&!f.r.isRunning(),"blocked output detector is active without a legacy productive pulse");
            f.reload();tick(f.r);
            h.assertTrue(f.r.machineControl(null).active()&&receiver.getEnergyTick()==3L*8192
                    &&f.r.inventory().getStackInSlot(1).getCount()==64,"blocked native reload retains pending output with no extra EU");
            f.r.inventory().extractItem(1,64,false);tick(f.r);
            h.assertTrue(f.r.inventory().getStackInSlot(1).is(Items.DIAMOND)
                    &&f.r.inventory().getStackInSlot(1).getCount()==1&&f.r.inventory().getStackInSlot(0).isEmpty(),"exactly one reserved product flushes");
            tick(f.r);h.assertTrue(!f.r.machineControl(null).active()&&f.r.machineControl(null).progressMax()==0
                    &&f.r.getRedstoneSignal(Direction.UP)==0&&receiver.getEnergyTick()==3L*8192,"completion pulse clears and idle does not duplicate output/EU");
            // A different immediately admitted recipe needs its own startup, even on a success pulse.
            f.map.mRecipeList.add(new Recipe(new ItemStack[]{new ItemStack(Items.CARROT)},new ItemStack[]{new ItemStack(Items.EMERALD)},
                    null,null,null,null,3,-8192,32768));
            f.r.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));tick(f.r);f.charge();tick(f.r);
            f.r.inventory().setStackInSlot(0,new ItemStack(Items.CARROT));tick(f.r);tick(f.r);
            h.assertTrue(f.r.chargeRemaining()==32768&&f.r.inventory().getStackInSlot(0).isEmpty()
                    &&f.r.machineControl(null).progressPerTick()==0&&receiver.getEnergyTick()==6L*8192
                    &&f.r.inventory().getStackInSlot(1).getCount()==2,"different recipe rollover reserves once but reports no work while its startup is unpaid");
            tick(f.r);f.reload();
            h.assertTrue(f.r.machineControl(null).running()&&!f.r.machineControl(null).active()
                    &&f.r.chargeRemaining()==32768&&f.r.machineControl(null).progress()==0,
                    "fresh native load preserves explicit passive charge wait without inventing processing");
            tick(f.r);h.assertTrue(receiver.getEnergyTick()==6L*8192&&f.r.inventory().getStackInSlot(1).getCount()==2,
                    "unpaid reloaded rollover cannot produce energy or duplicate prior products");
        }
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void fusionCoverClockAndTransfersRespectInvalidAndStoppedStates(GameTestHelper h) {
        try(var f=new Fixture(h,2)) {
            f.build(3);
            var conveyor=BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                    .filter(s->CoverItems.CONVEYOR.equals(CoverItems.behavior(s)))
                    .max(java.util.Comparator.comparingInt(CoverItems::tierIndex)).orElseThrow();
            h.assertTrue(ComponentCoverRules.itemInterval(CoverItems.tierIndex(conveyor))==1,"registered conveyor fixture is due everytick");
            h.assertTrue(f.r.attachCover(Direction.NORTH,conveyor),"native conveyor attaches");
            var chestPos=f.pos.north();f.set(chestPos,Blocks.CHEST.defaultBlockState());
            var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(chestPos);
            f.r.inventory().setStackInSlot(1,new ItemStack(Items.DIAMOND));
            var cell=FusionStructure.CELLS.get(0);var wall=cell.at(f.pos,Direction.NORTH);var wallState=h.getLevel().getBlockState(wall);
            f.set(wall,Blocks.AIR.defaultBlockState());
            long before=save(h,f.r).getLong("gt.cover_ticks");tick(f.r);
            h.assertTrue(save(h,f.r).getLong("gt.cover_ticks")==before+1&&chest.isEmpty()
                    &&f.r.inventory().getStackInSlot(1).getCount()==1,"invalid structure advances coverclock without transferring stock");
            f.set(wall,wallState);h.assertTrue(f.r.isStructureOk(),"real wall repair rebinds ports");
            f.r.machineControl(null).setEnabled(false);tick(f.r);
            h.assertTrue(save(h,f.r).getLong("gt.cover_ticks")==before+2&&!f.r.machineControl(null).running()
                    &&f.r.inventory().getStackInSlot(1).isEmpty()&&chest.getItem(0).is(Items.DIAMOND)
                    &&chest.getItem(0).getCount()==1,"valid stopped controller still runs component cover exactly once, matching Basic policy");
            tick(f.r);h.assertTrue(chest.getItem(0).getCount()==1,"later stopped tick cannot duplicate transferred output");
        }
        h.succeed();
    }
}
