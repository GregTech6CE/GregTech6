package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.item.behavior.BehaviorFlintAndTinder;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Finite provided-device native-world check; no manual machine ticks or injected clock energy. */
final class CokeOvenRuntimeSmoke {
    private static final BlockPos[] positions=new BlockPos[4];
    private static final Direction[] fronts={Direction.NORTH,Direction.WEST,Direction.SOUTH,Direction.EAST};
    private static long started;
    private static int phase,screenFrames;
    private static boolean complete,guiDone;
    private static volatile Throwable failure;
    private static java.util.concurrent.CompletableFuture<Boolean> observation;
    private static JsonObject receipt;
    private static void require(boolean valid,String message) {if(!valid)throw new IllegalStateException(message);}
    static boolean frame(Minecraft mc,JsonObject result) {
        if(failure!=null)throw new IllegalStateException("Coke world check failed",failure);
        receipt=result;
        if(phase==0) {
            phase=1;observation=mc.getSingleplayerServer().submit(()->{setup(mc.getSingleplayerServer());return false;});return false;
        }
        if(observation!=null&&!observation.isDone())return false;
        if(observation!=null){complete=observation.join();observation=null;}
        if(!complete) {observation=mc.getSingleplayerServer().submit(()->observe(mc.getSingleplayerServer()));return false;}
        if(!guiDone) {
            if(phase!=5) {phase=5;mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.connection.teleport(positions[0].getX()+.5,positions[0].getY()+1.01,positions[0].getZ()+.5,180,0);
                var state=player.serverLevel().getBlockState(positions[0]);
                state.useWithoutItem(player.serverLevel(),player,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(positions[0]),fronts[0],positions[0],false));
            });}
            return false;
        }
        return ColoredBooksRuntimeSmoke.frame(mc,result);
    }
    private static void setup(net.minecraft.server.MinecraftServer server) {
        var player=server.getPlayerList().getPlayers().get(0);var level=player.serverLevel();
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var origin=player.blockPosition().offset(0,12,0);
        for(int i=0;i<4;i++) {
            var pos=origin.offset((i%2)*7,0,(i/2)*7);positions[i]=pos;
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)level.setBlock(pos.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos,GTMultiblocks.COKE_OVEN_MAIN.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,fronts[i]),3);
            for(var cell:LargeMachineLayouts.fromShared(OriginalCokeOvenRules.cells()))level.setBlock(cell.at(pos,fronts[i]),cell.block().defaultBlockState(),3);
            require(oven(level,i).isStructureOk(),"Exact source structure failed facing "+fronts[i]);
        }
        var spec=oven(level,0).spec();
        require(spec.energyTag()==GregTechTags.Energy.TU&&spec.parallelLimit()==16&&spec.energyInMin()==1&&spec.energyInMax()==16,"Original TU parameters missing");
        require(oven(level,0).doEnergyInjection(GregTechTags.Energy.HU,Direction.UP,512,1,true)==0,"Coke still accepts HU");
        var port=positions[0].relative(fronts[0].getClockWise());
        var input=items(level,port,Direction.UP);
        require(input!=null&&input.insertItem(0,new ItemStack(Items.OAK_LOG,17),false).isEmpty(),"Fire brick does not forward input");
        require(input.extractItem(0,1,false).isEmpty(),"Fire brick extracts recipe inputs");
        for(int i=1;i<4;i++)oven(level,i).inventory().setStackInSlot(0,new ItemStack(i==3?Items.COAL:Items.OAK_LOG));
        level.setBlock(positions[1].relative(fronts[1].getOpposite()),Blocks.STONE.defaultBlockState(),3);
        require(!oven(level,1).isStructureOk(),"Filled centre accepted");
        require(items(level,positions[1].relative(fronts[1].getClockWise()),Direction.UP).getSlots()==0,"Broken oven keeps ports");
        oven(level,2).getTanksOutput()[0].setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
        for(int i=1;i<4;i++)ignite(level,player,i);
        var tankBlock=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.machine.TankBlock tank&&tank.spec().capacity()>=8000).findFirst().orElseThrow();
        level.setBlock(positions[0].relative(fronts[0].getOpposite()).below(2),tankBlock.defaultBlockState(),3);
        legacy(level);
        ColoredBooksRuntimeSmoke.server(server,receipt);
        started=level.getGameTime();receipt.addProperty("cokeSourceStructureFacings",4);receipt.addProperty("cokeSourceFireBricks",25);
        receipt.addProperty("cokeRecipeRows",com.gregtech.gregtech.data.MachineRecipeMaps.CokeOven.mRecipeList.size());
        phase=2;
    }
    private static boolean observe(net.minecraft.server.MinecraftServer server) {
        var player=server.getPlayerList().getPlayers().get(0);var level=player.serverLevel();long elapsed=level.getGameTime()-started;
        if(phase==2&&elapsed>=100) {
            require(oven(level,0).inventory().getStackInSlot(0).getCount()==17&&oven(level,0).machineControl(null).progress()==0,"Unignited coke consumes input");
            require(oven(level,1).inventory().getStackInSlot(0).getCount()==1,"Invalid oven consumes input");
            require(oven(level,2).inventory().getStackInSlot(0).getCount()==1,"Blocked output consumes input");
            level.setBlock(positions[1].relative(fronts[1].getOpposite()),Blocks.AIR.defaultBlockState(),3);
            oven(level,2).getTanksOutput()[0].setEmpty();
            ignite(level,player,0);phase=3;
        }
        if(phase==3&&elapsed>=120&&oven(level,0).inventory().getStackInSlot(0).getCount()!=1)throw new IllegalStateException(diagnostic(level,0));
        if(phase==3&&elapsed>=200) {
            require(oven(level,1).isStructureOk()&&oven(level,1).inventory().getStackInSlot(0).getCount()==1,"Expired ignition starts repaired oven");
            require(oven(level,2).inventory().getStackInSlot(0).getCount()==1,"Expired ignition starts unblocked oven");
            require(oven(level,0).inventory().getStackInSlot(0).getCount()==1,"16 parallel input limit differs: "+diagnostic(level,0));
            ignite(level,player,1);ignite(level,player,2);phase=4;
            receipt.addProperty("cokeIgnitionAndBackpressureChecks",6);
        }
        if(phase<4||elapsed<3810)return false;
        for(int i=0;i<3;i++)require(oven(level,i).inventory().getStackInSlot(1).is(Items.CHARCOAL)&&oven(level,i).inventory().getStackInSlot(1).getCount()==(i==0?16:1),"Source wood outputs differ "+i);
        var coal=GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.gem,com.gregtech.gregtech.content.material.Materials.CoalCoke,1);
        require(oven(level,3).inventory().getStackInSlot(1).is(coal.getItem())&&oven(level,3).getTanksOutput()[0].getAmount()==500,"Coal source recipe not used");
        var floor=fluids(level,positions[0].relative(fronts[0].getOpposite()).below(2),Direction.UP);
        require(floor!=null&&floor.getFluidInTank(0).getAmount()==4000&&oven(level,0).getTanksOutput()[0].getAmount()==0,"Source floor output or conservation failed");
        require(oven(level,0).inventory().getStackInSlot(0).isEmpty()&&oven(level,0).machineControl(null).progress()>0,"Successful batch did not preserve ignition for next job");
        receipt.addProperty("cokeNativeWorldTicks",elapsed);receipt.addProperty("cokeActualParallelOutputs",16);receipt.addProperty("cokeUnderFloorCreosote",4000);
        receipt.addProperty("cokeNativeCompletedJobs",4);receipt.addProperty("cokeContinuousNextJob",true);
        return true;
    }
    private static String diagnostic(net.minecraft.server.level.ServerLevel level,int i) {
        var machine=oven(level,i);var recipe=machine.recipeMap().findRecipe(java.util.List.of(machine.inventory().getStackInSlot(0)),java.util.List.of(),false,1,9);
        int parallel;
        try {var method=BasicMachineBlockEntity.class.getDeclaredMethod("computeParallel",com.gregtech.gregtech.api.recipe.Recipe.class);method.setAccessible(true);parallel=(int)method.invoke(machine,recipe);}
        catch(Exception e){throw new IllegalStateException(e);}
        return "parallel="+parallel+", ignition="+machine.saveWithoutMetadata(level.registryAccess()).getInt("gt.coke_ignition")+", running="+machine.isRunning()+", cost="+(recipe==null?null:com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(recipe.mEUt,recipe.mDuration,parallel,false,10000,1,16,false,true))+", input="+machine.inventory().getStackInSlot(0)+", progress="+machine.machineControl(null).progress()+"/"+machine.machineControl(null).progressMax()+", structure="+machine.isStructureOk()+", energy="+machine.getEnergyTick()+", recipe="+(recipe==null?"null":"power="+recipe.mEUt+", ticks="+recipe.mDuration+", outputs="+java.util.Arrays.toString(recipe.mOutputs)+", fluids="+java.util.Arrays.toString(recipe.mFluidOutputs));
    }
    private static void ignite(net.minecraft.server.level.ServerLevel level,net.minecraft.server.level.ServerPlayer player,int i) {
        require(BehaviorFlintAndTinder.ignite(level,positions[i],fronts[i],player,new ItemStack(Items.FLINT_AND_STEEL),.5f,.5f,.5f)==10000,"Native ignition route failed");
        require(oven(level,i).saveWithoutMetadata(level.registryAccess()).getInt("gt.coke_ignition")==40,"Ignition state not set "+i);
    }
    private static CokeOvenControllerBlockEntity oven(net.minecraft.server.level.ServerLevel level,int i) {return (CokeOvenControllerBlockEntity)level.getBlockEntity(positions[i]);}
    private static IItemHandler items(net.minecraft.server.level.ServerLevel level,BlockPos pos,Direction side) {return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,pos,side);}
    private static IFluidHandler fluids(net.minecraft.server.level.ServerLevel level,BlockPos pos,Direction side) {return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,side);}
    private static void legacy(net.minecraft.server.level.ServerLevel level) {
        var old=new ItemStackHandler(2);old.setStackInSlot(0,new ItemStack(Items.COAL,3));old.setStackInSlot(1,new ItemStack(Items.CHARCOAL,4));
        var tag=new CompoundTag();tag.put("gt.items",old.serializeNBT(level.registryAccess()));
        var creosote=GTFluids.still("Oil_Creosote").get();
        var tank=new com.gregtech.gregtech.api.fluid.FluidTankGT(16000);tank.fill(new FluidStack(creosote,12000),IFluidHandler.FluidAction.EXECUTE);
        var saved=new CompoundTag();tank.writeToNBT(saved,level.registryAccess());tag.put("gt.output",saved);tag.putLong("gt.heat",512);
        var entity=new CokeOvenControllerBlockEntity(BlockPos.ZERO,GTMultiblocks.COKE_OVEN_MAIN.get().defaultBlockState());entity.loadAdditional(tag,level.registryAccess());
        require(entity.inventory().getSlots()==10&&entity.inventory().getStackInSlot(0).getCount()==3&&entity.inventory().getStackInSlot(1).getCount()==4&&entity.getTanksOutput()[0].getAmount()==12000,"Legacy coke inventory/fluid truncated");
        receipt.addProperty("cokeLegacyDataMigrationChecks",1);
    }
    static void screen(Minecraft mc,net.minecraft.client.gui.screens.Screen screen) {
        if(phase!=5||guiDone)return;
        try {
            require(screen instanceof com.gregtech.gregtech.client.gui.BasicMachineScreen,"Coke opened wrong screen "+screen.getClass());
            if(++screenFrames<12)return;
            String name="coke-oven-gui-"+java.util.UUID.randomUUID()+".png";
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),ignored->{
                try {var path=mc.gameDirectory.toPath().resolve("screenshots").resolve(name).toAbsolutePath();require(java.nio.file.Files.isRegularFile(path),"Coke GUI screenshot missing");receipt.addProperty("cokeGuiScreenshot",path.toString());guiDone=true;mc.execute(()->mc.setScreen(null));}
                catch(Throwable e){failure=e;}
            });phase=6;
        }catch(Throwable e){failure=e;}
    }
}
