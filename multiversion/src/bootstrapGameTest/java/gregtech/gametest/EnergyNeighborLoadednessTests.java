package gregtech.gametest;

import gregtech.block.energy.LaserConverterBlock;
import gregtech.block.machine.EngineBlock;
import gregtech.blockentity.energy.GearboxBlockEntity;
import gregtech.blockentity.energy.LaserConverterBlockEntity;
import gregtech.blockentity.machine.KineticRotationEngineBlockEntity;
import gregtech.data.GregTechTags;
import gregtech.util.GTEnergySides;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Remote FULL-chunk manual native-ticker contract, not naturally missing BLOCK_TICKING neighbors. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class EnergyNeighborLoadednessTests {
    private static Block block(String id){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce",id));}
    private static CompoundTag save(GameTestHelper h,BlockEntity entity){
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h,BlockEntity entity,CompoundTag data){
//? if >=1.21.1 {
/*        entity.loadWithComponents(data,h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }
    @SuppressWarnings("unchecked")
    private static void tick(GameTestHelper h,BlockEntity entity){
        var state=entity.getBlockState();
        var ticker=((EntityBlock)state.getBlock()).getTicker(h.getLevel(),state,
                (BlockEntityType<BlockEntity>)(BlockEntityType<?>)entity.getType());
        h.assertTrue(ticker!=null,"registered block has native server ticker");
        ticker.tick(h.getLevel(),entity.getBlockPos(),state,entity);
    }
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state,CompoundTag data){}
        final GameTestHelper h;
        final java.util.Map<BlockPos,Saved> saved=new java.util.LinkedHashMap<>();
        Fixture(GameTestHelper h){this.h=h;}
        void set(BlockPos p,BlockState state){
            // Do not snapshot an unavailable target before its negative assertion: that read would load it.
            if(!saved.containsKey(p)){var be=h.getLevel().getBlockEntity(p);saved.put(p.immutable(),new Saved(h.getLevel().getBlockState(p),be==null?null:save(h,be)));}
            h.getLevel().removeBlockEntity(p);
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
        @Override public void close(){
            for(var p:saved.keySet())h.getLevel().removeBlockEntity(p);
            for(var e:saved.entrySet()){
                h.getLevel().setBlock(e.getKey(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                h.getLevel().setBlock(e.getKey(),e.getValue().state(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                var be=h.getLevel().getBlockEntity(e.getKey());if(be!=null&&e.getValue().data()!=null)load(h,be,e.getValue().data());
            }
        }
        LaserConverterBlockEntity laser(BlockPos pos,String id,Direction front,boolean active){
            var nativeBlock=block(id);h.assertTrue(nativeBlock instanceof LaserConverterBlock,"registered laser exists: "+id);
            set(pos,nativeBlock.defaultBlockState().setValue(LaserConverterBlock.FACING,front).setValue(LaserConverterBlock.ACTIVE,active));
            return (LaserConverterBlockEntity)h.getLevel().getBlockEntity(pos);
        }
    }
    private static void chargeLaser(GameTestHelper h,LaserConverterBlockEntity laser,Direction input){
        h.assertTrue(laser.doEnergyInjection(GregTechTags.Energy.EU,laser.front(),32,1,true)==0,"electric laser rejects its output face as input");
        h.assertTrue(laser.doEnergyInjection(GregTechTags.Energy.EU,input,32,1,false)==1
                &&laser.getEnergyStored(GregTechTags.Energy.EU,input)==0,"native EU simulation is nonmutating");
        h.assertTrue(laser.doEnergyInjection(GregTechTags.Energy.EU,input,32,1,true)==1
                &&laser.getEnergyStored(GregTechTags.Energy.EU,input)==32,"legal native EU packet is credited");
    }
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void laserNeighborLookupSkipsAbsentBordersWithoutRetainingWaste(GameTestHelper h){
        try(var f=new Fixture(h)){
            // Positive same-chunk vertical control runs before the baseline counterexample.
            var vertical=new BlockPos(28_000_008,200,28_000_104);
            var laser=f.laser(vertical,"co2_laser_lv",Direction.UP,true);
            var absorber=f.laser(vertical.above(),"laser_absorber_lv",Direction.UP,false);
            h.assertTrue(GTEnergySides.getNeighbor(h.getLevel(),vertical,Direction.UP)==absorber,"same-chunk vertical neighbor identity is preserved");
            chargeLaser(h,laser,Direction.DOWN);tick(h,laser);
            h.assertTrue(absorber.getEnergyStored(GregTechTags.Energy.LU,Direction.DOWN)==16
                    &&laser.getEnergyStored(GregTechTags.Energy.EU,Direction.DOWN)==0,"loaded vertical transfer preserves native32EU to16LU conversion");
            for(int sign:new int[]{1,-1}){
                var pos=new BlockPos(28_000_008,200,sign*28_000_000);var target=pos.north();
                h.assertTrue(!h.getLevel().hasChunkAt(target),"fresh remote target starts unavailable sign="+sign);
                // ACTIVE is pre-set solely to isolate lookup from vanilla shape updates on activity transitions.
                laser=f.laser(pos,"co2_laser_lv",Direction.NORTH,true);
                h.assertTrue(!h.getLevel().hasChunkAt(target),"known-shape source placement leaves neighbor absent sign="+sign);
                chargeLaser(h,laser,Direction.SOUTH);
                h.assertTrue(!h.getLevel().hasChunkAt(target),"legal input injection leaves output neighbor absent sign="+sign);
                tick(h,laser);
                h.assertTrue(laser.getBlockState().getValue(LaserConverterBlock.ACTIVE),"active visual state did not transition");
                h.assertTrue(laser.getEnergyStored(GregTechTags.Energy.EU,Direction.SOUTH)==0,"missing receiver still wastes original per-tick supply");
                h.assertTrue(!laser.machineControl(null).active(),"unavailable output does not report successful emission");
                h.assertTrue(!h.getLevel().hasChunkAt(target),"native laser neighbor lookup must not load absent chunk sign="+sign);
                // Deliberate target load only AFTER negative proof. This is not a chunk-unload test.
                absorber=f.laser(target,"laser_absorber_lv",Direction.NORTH,false);
                chargeLaser(h,laser,Direction.SOUTH);tick(h,laser);
                h.assertTrue(absorber.getEnergyStored(GregTechTags.Energy.LU,Direction.SOUTH)==16
                        &&laser.getEnergyStored(GregTechTags.Energy.EU,Direction.SOUTH)==0
                        &&laser.machineControl(null).active(),"explicitly loaded crossed-border receiver gets exactly16LU and reports emission");
            }
        }h.succeed();
    }
    private static void rotationPushControl(GameTestHelper h,Fixture f,BlockPos pos){
        f.set(pos,block("engine_rotation_bronze").defaultBlockState().setValue(EngineBlock.FACING,Direction.EAST).setValue(EngineBlock.LIT,false));
        var engine=(KineticRotationEngineBlockEntity)h.getLevel().getBlockEntity(pos);var target=pos.north();
        var gearboxBlock=block("gearbox_bronze");
        h.assertTrue(gearboxBlock instanceof gregtech.block.energy.GearboxBlock,"registered bronze gearbox exists");
        f.set(target,gearboxBlock.defaultBlockState());var gearbox=(GearboxBlockEntity)h.getLevel().getBlockEntity(target);
        gearbox.mountGear(Direction.EAST);gearbox.mountGear(Direction.SOUTH);
        h.assertTrue(gearbox.gearsWork(),"real two-gear corner configuration works");
        h.assertTrue(gearbox.doEnergyInjection(GregTechTags.Energy.RU,Direction.EAST,32,1,true)==1,"registered gearbox accepts legal32RU packet");
        tick(h,gearbox);
        h.assertTrue(engine.storedRu()==32&&gearbox.transferredLast()==32,"loaded gearbox pushes one real packet to radial engine input");
        h.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU,Direction.NORTH,32,1,false)==1&&engine.storedRu()==32,"loaded radial simulation preserves stored energy");
        h.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU,Direction.EAST,32,1,true)==0,"axial face remains output-only");
        tick(h,engine);
        h.assertTrue(engine.storedRu()==0&&engine.isActive(),"native engine consumes its input even when KU receivers are absent");
        // No builtin pull supplier is invented: gearbox input is native push, not pull extraction.
    }
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void rotationRadialPollingSkipsAbsentBordersAndKeepsNativePushInput(GameTestHelper h){
        try(var f=new Fixture(h)){
            var engineBlock=block("engine_rotation_bronze");
            h.assertTrue(engineBlock instanceof EngineBlock,"registered bronze rotation engine exists");
            rotationPushControl(h,f,new BlockPos(28_000_136,200,28_000_104));
            for(int sign:new int[]{1,-1}){
                var pos=new BlockPos(28_000_136,200,sign*28_000_000);var target=pos.north();
                h.assertTrue(!h.getLevel().hasChunkAt(target),"fresh radial target starts unavailable sign="+sign);
                f.set(pos,engineBlock.defaultBlockState().setValue(EngineBlock.FACING,Direction.EAST).setValue(EngineBlock.LIT,false));
                var engine=(KineticRotationEngineBlockEntity)h.getLevel().getBlockEntity(pos);
                h.assertTrue(!h.getLevel().hasChunkAt(target),"known-shape engine placement leaves radial neighbor absent");
                tick(h,engine); // No RU means no KU emission or LIT transition: only radial polling can cross north.
                h.assertTrue(!engine.isActive()&&engine.storedRu()==0&&!engine.getBlockState().getValue(EngineBlock.LIT),"idle polling fabricated no work or visual transition");
                h.assertTrue(!h.getLevel().hasChunkAt(target),"native rotation polling must not load missing radial neighbor sign="+sign);
                rotationPushControl(h,f,pos);
            }
        }h.succeed();
    }
}
