package gregtech.gametest;

import gregtech.api.energy.FaceConfig;
import gregtech.api.machine.BasicMachineSpec;
import gregtech.api.multiblock.MultiblockLayout;
import gregtech.api.recipe.Recipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.blockentity.machine.*;
import gregtech.content.cover.*;
import gregtech.content.multiblock.*;
import gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
//?}

/** Registered controller tickers, real shells, native cover policies; no modeled scheduler. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class ControllerOutputLifecycleTests {
    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag tag) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(tag,h.getLevel().registryAccess());
*///?} else {
        entity.load(tag);
//?}
    }
    private static Block block(String id) {return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce",id));}
    private static ItemStack cover(String id) {return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",id)));}
    @SuppressWarnings("unchecked")
    private static void tick(GameTestHelper h, BasicMachineBlockEntity machine) {
        var state=machine.getBlockState();
        var ticker=((EntityBlock)state.getBlock()).getTicker(h.getLevel(),state,
                (BlockEntityType<BlockEntity>)(BlockEntityType<?>)machine.getType());
        h.assertTrue(ticker!=null,"actual registered controller has a server ticker");
        ticker.tick(h.getLevel(),machine.getBlockPos(),state,machine);
    }
    private static IItemHandler items(BasicMachineBlockEntity machine, Direction side) {
//? if >=1.21.1 {
/*        return machine.itemCapability(side);
*///?} else {
        return machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,side).orElse(null);
//?}
    }
    private static void filter(GameTestHelper h, ItemStack stack, boolean inverted) {
        var tag=CoverStackData.readOrEmpty(stack);
//? if >=1.21.1 {
/*        tag.put("gt.filter.item",new ItemStack(Items.STICK).save(h.getLevel().registryAccess()));
*///?} else {
        tag.put("gt.filter.item",new ItemStack(Items.STICK).save(new CompoundTag()));
//?}
        tag.putBoolean("gt.cover.inverted",inverted);CoverStackData.write(stack,tag);
    }
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state,CompoundTag data) {}
        final GameTestHelper h;final BlockPos pos;
        final java.util.Map<BlockPos,Saved> saved=new java.util.LinkedHashMap<>();
        RecipeMap map;
        Fixture(GameTestHelper h,int index){this.h=h;pos=new BlockPos(26_000_000+index*128,200,26_000_000);}
        void clear(){for(var p:BlockPos.betweenClosed(pos.offset(-5,-2,-5),pos.offset(5,9,5)))set(p,Blocks.AIR.defaultBlockState());}
        void set(BlockPos p,BlockState state){
            if(!saved.containsKey(p)){var be=h.getLevel().getBlockEntity(p);saved.put(p.immutable(),new Saved(h.getLevel().getBlockState(p),be==null?null:save(h,be)));}
            h.getLevel().removeBlockEntity(p);
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
        @Override public void close(){try{
            for(var p:saved.keySet())h.getLevel().removeBlockEntity(p);
            for(var e:saved.entrySet()){
                h.getLevel().setBlock(e.getKey(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                h.getLevel().setBlock(e.getKey(),e.getValue().state(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                var be=h.getLevel().getBlockEntity(e.getKey());if(be!=null&&e.getValue().data()!=null)load(h,be,e.getValue().data());
            }
        }finally{if(map!=null) RecipeMap.unregister(map);}}
    }
    private static void tower(GameTestHelper h,boolean cold){
        try(var f=new Fixture(h,cold?1:0)){
            f.clear();var front=cold?Direction.EAST:Direction.NORTH;var back=front.getOpposite();
            var controller=block(cold?"cryo_distillation_main":"distillation_tower_main");
            h.assertTrue(controller instanceof EntityBlock,"registered tower controller exists");
            f.set(f.pos,controller.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,front));
            var machine=(DistillationTowerControllerBlockEntity)h.getLevel().getBlockEntity(f.pos);
            h.assertTrue(machine.attachCover(back,cover(CoverUtilityBehaviors.FILTER_ITEM)),"attach own BACK filter before enclosing shell");
            for(var cell:LargeMachineLayouts.fromShared(SharedDistillationTowerStructure.CELLS))f.set(cell.at(f.pos,front),cell.block().defaultBlockState());
            h.assertTrue(StructureTestSupport.ok(machine),"real registered tower shell forms");
            var target=f.pos.relative(back,3);f.set(target,Blocks.CHEST.defaultBlockState());
            var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(target);int output=machine.inputSlots();
            machine.inventory().setStackInSlot(output,new ItemStack(Items.STICK,4)); // Explicit transfer-boundary output fixture.
            filter(h,machine.getCover(back),true);
            var view=items(machine,back);h.assertTrue(view!=null&&view.extractItem(output,4,true).isEmpty(),"native own-face simulation respects matching blacklist");
            powerTick(h,machine,front,cold);
            h.assertTrue(chest.isEmpty()&&machine.inventory().getStackInSlot(output).getCount()==4,"blacklist blocks automatic remote output without loss");
            filter(h,machine.getCover(back),false);
            h.assertTrue(view.extractItem(output,4,true).getCount()==4&&machine.inventory().getStackInSlot(output).getCount()==4,"permitted simulation is nonmutating");
            powerTick(h,machine,front,cold);powerTick(h,machine,front,cold);
            h.assertTrue(chest.getItem(0).getCount()==4&&machine.inventory().getStackInSlot(output).isEmpty(),"allowed remote transfer commits exactly once");
            h.assertTrue(h.getLevel().getBlockEntity(f.pos.relative(back)) instanceof MultiblockPortBlockEntity,"adjacent BACK remains a bound wall, not the selected receiver");
            machine.removeCover(back);h.assertTrue(machine.attachCover(back,cover(PanelCover.SHUTTER.id)),"native shutter replaces filter");
            h.assertTrue(machine.panels().configure(back,false,false)&&machine.panels().shuttered(back),"shutter is closed");
            machine.inventory().setStackInSlot(output,new ItemStack(Items.STICK,3));powerTick(h,machine,front,cold);
            h.assertTrue(chest.getItem(0).getCount()==4&&machine.inventory().getStackInSlot(output).getCount()==3,"closed own shutter blocks remote automatic output");
            h.assertTrue(machine.panels().configure(back,false,false)&&!machine.panels().shuttered(back),"native configuration opens shutter");
            powerTick(h,machine,front,cold);
            h.assertTrue(chest.getItem(0).getCount()==7&&machine.inventory().getStackInSlot(output).isEmpty(),"open shutter conserves all seven items");
            f.set(target,Blocks.AIR.defaultBlockState());machine.inventory().setStackInSlot(output,new ItemStack(Items.STICK,2));
            var area=new AABB(target).inflate(1);int entities=h.getLevel().getEntitiesOfClass(ItemEntity.class,area).size();
            powerTick(h,machine,front,cold);
            h.assertTrue(machine.inventory().getStackInSlot(output).getCount()==2&&h.getLevel().getEntitiesOfClass(ItemEntity.class,area).size()==entities,
                    "remote air target retains products and does not acquire ordinary adjacent air-ejection behavior");
        }h.succeed();
    }
    private static void powerTick(GameTestHelper h,DistillationTowerControllerBlockEntity machine,Direction front,boolean cold){
        var energy=cold?GregTechTags.Energy.CU:GregTechTags.Energy.HU;
        long packet=machine.getEnergySizeInputRecommended(energy,front);
        h.assertTrue(packet==512&&packet>=machine.getEnergySizeInputMin(energy,front)
                &&packet<=machine.getEnergySizeInputMax(energy,front)&&machine.isEnergyAcceptingFrom(energy,front,false),
                "native recommended tower packet is legal on the actual front face");
        h.assertTrue(machine.doEnergyInjection(energy,front,packet,1,true)==1&&machine.getEnergyTick()==packet,
                "actual native tower energy is credited, not consumed as an overload");tick(h,machine);
    }
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void distillationRemoteItemsRespectOwnFilterAndShutter(GameTestHelper h){tower(h,false);}
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void cryoRemoteItemsRespectOwnFilterAndShutter(GameTestHelper h){tower(h,true);}

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void implosionNativeTickerClearsInvalidActivityAndRetainsJob(GameTestHelper h){
        try(var f=new Fixture(h,2)){
            f.clear();var front=Direction.NORTH;var controller=block("implosion_compressor_main");
            h.assertTrue(controller instanceof EntityBlock,"registered implosion controller exists");
            f.set(f.pos,controller.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,front));
            var machine=(ImplosionCompressorControllerBlockEntity)h.getLevel().getBlockEntity(f.pos);
            var wall=block("implosion_compressor_wall");
            for(var cell:ImplosionCompressorControllerBlockEntity.LAYOUT.cells())if(cell.role()!=MultiblockLayout.Role.AIR)f.set(cell.at(f.pos,front),wall.defaultBlockState());
            h.assertTrue(StructureTestSupport.ok(machine),"real implosion shell forms");
            f.map=new RecipeMap(null,"gt.recipe.test.implosion_lifecycle_"+java.util.UUID.randomUUID(),"Implosion lifecycle","test",1,1,1,0,0,0,1,false,false,false,false);
            f.map.mRecipeList.add(new Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},null,null,null,null,6,1,0));
            machine.setSpec(BasicMachineSpec.builder("implosion_lifecycle_fixture",machine.spec().material()).machineType("implosioncompressor")
                    .energy(GregTechTags.Energy.TU,1).recipes(f.map).faces(FaceConfig.ALL_SIDES).build());
            h.assertTrue(machine.attachCover(Direction.NORTH,cover("activity_detector_processing")),"native processing detector attaches");
            machine.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));tick(h,machine);
            long progress=machine.machineControl(null).progress(),maximum=machine.machineControl(null).progressMax();
            h.assertTrue(machine.machineControl(null).active()&&progress>0&&maximum>progress&&machine.inventory().getStackInSlot(0).isEmpty()
                    &&machine.getRedstoneSignal(Direction.NORTH)==15,"actual registered ticker starts one reserved productive job");
            var cell=ImplosionCompressorControllerBlockEntity.LAYOUT.cells().stream().filter(c->c.role()!=MultiblockLayout.Role.AIR).findFirst().orElseThrow();
            var missing=cell.at(f.pos,front);f.set(missing,Blocks.AIR.defaultBlockState());long clock=save(h,machine).getLong("gt.cover_ticks");
            for(int i=0;i<3;i++)tick(h,machine);
            h.assertTrue(!machine.machineControl(null).active()&&!machine.machineControl(null).running()&&machine.getRedstoneSignal(Direction.NORTH)==0
                    &&save(h,machine).getLong("gt.cover_ticks")==clock+3,"broken shell clears activity and detector while advancing lifecycle clock");
            h.assertTrue(machine.machineControl(null).progress()==progress&&machine.machineControl(null).progressMax()==maximum
                    &&machine.inventory().getStackInSlot(0).isEmpty()&&machine.inventory().getStackInSlot(1).isEmpty(),"invalid ticks retain reserved job without consuming or producing again");
            f.set(missing,wall.defaultBlockState());h.assertTrue(StructureTestSupport.ok(machine),"real shell repair reforms");
            for(int i=0;i<10;i++)tick(h,machine);
            h.assertTrue(machine.inventory().getStackInSlot(1).is(Items.DIAMOND)&&machine.inventory().getStackInSlot(1).getCount()==1
                    &&machine.inventory().getStackInSlot(0).isEmpty(),"repair resumes retained job to exactly one product without replacement input");
        }h.succeed();
    }
}
