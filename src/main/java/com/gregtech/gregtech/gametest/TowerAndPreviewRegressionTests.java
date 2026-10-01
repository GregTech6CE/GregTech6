package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.blockentity.machine.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class TowerAndPreviewRegressionTests {
    @GameTest(template="test_blueprint_empty")
    public static void distillationBaseFeedsHeatAndReleasesBrokenStructure(GameTestHelper h) {
        var origin=new BlockPos(5,3,5);
        h.setBlock(origin,GTMultiblocks.DISTILLATION_TOWER_MAIN.get());
        var layout=com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.cells(GTMultiblocks.DISTILLATION_TOWER_MAIN.get());
        layout.forEach((p,b)->h.setBlock(origin.offset(p),b));
        var tower=(DistillationTowerControllerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(origin));
        h.assertTrue(tower.isStructureOk(),"tower forms and binds ports");
        var base=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(origin.below()));
        h.assertTrue(base.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,32,1,false)==1 && tower.getEnergyTick()==0,"HU simulation reaches controller without mutation");
        h.assertTrue(base.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,32,1,true)==1 && tower.getEnergyTick()==32,"bottom transmitter actually feeds HU to controller");
        h.assertTrue(base.doEnergyInjection(GregTechTags.Energy.EU,Direction.DOWN,32,1,true)==0,"heat base rejects EU");
        var top=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(origin.above()));
        h.assertTrue(top.doEnergyInjection(GregTechTags.Energy.HU,Direction.UP,32,1,true)==0,"upper wall is not a heat input");
        h.setBlock(origin.offset(1,1,1),Blocks.AIR);
        h.assertTrue(base.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,32,1,true)==0,"cached port immediately rejects energy after a structure break");
        h.setBlock(origin.offset(1,1,1),GTMultiblocks.DISTILLATION_TOWER_PART.get());
        h.assertTrue(tower.isStructureOk() && base.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,32,1,true)==1,"repaired tower rebinds");
        var recipes=new com.gregtech.gregtech.api.recipe.RecipeMap(null,"tower_heat_test","Test","test",1,1,1,0,0,0,1,false,false,false,false);
        recipes.addRecipe(new com.gregtech.gregtech.api.recipe.Recipe(
            new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE)},
            new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)},null,null,null,null,4,32,0));
        tower.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("tower_heat_test",tower.spec().material())
            .machineType("distillationtower").energy(GregTechTags.Energy.HU,512).recipes(recipes)
            .faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
        tower.inventory().setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE));
        for(int i=0;i<6;i++){
            base.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,32,1,true);
            BasicMachineBlockEntity.serverTick(h.getLevel(),tower.getBlockPos(),tower.getBlockState(),tower);
        }
        h.assertTrue(tower.inventory().getStackInSlot(tower.inputSlots()).is(net.minecraft.world.item.Items.DIAMOND),"heat from base runs the actual recipe executor");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void previewCameraIsBoundedAndResettable(GameTestHelper h) {
        var camera=new com.gregtech.gregtech.jei.PreviewCamera();
        h.assertTrue(com.gregtech.gregtech.jei.PreviewLayers.levels(java.util.List.of(new BlockPos(0,-2,0),new BlockPos(0,1,0))).equals(java.util.List.of(-2,-1,0,1)), "floors start at bottom and retain empty physical levels");
        h.assertTrue(com.gregtech.gregtech.jei.PreviewLayers.levels(java.util.List.of()).isEmpty(), "empty preview has no selectable floors");
        camera.rotate(17,12);camera.pan(20,-10);camera.zoom(2);
        h.assertTrue(camera.yaw()==242&&camera.pitch()==42&&camera.x()==20&&camera.scale()>1,"drag and wheel change camera continuously");
        camera.zoom(100000);camera.rotate(-100000,100000);camera.pan(100000,-100000);
        h.assertTrue(camera.scale()==4&&camera.pitch()==89&&camera.x()==160&&camera.y()==-100,"camera cannot escape its bounds");
        camera.reset();h.assertTrue(camera.yaw()==225&&camera.pitch()==30&&camera.scale()==1&&camera.x()==0&&camera.y()==0,"reset restores useful initial view");
        h.succeed();
    }
    @GameTest(template="test_empty", batch="weather", timeoutTicks=1000)
    public static void manualRainCollection(GameTestHelper h) {
        var world=h.getLevel();
        var data=(net.minecraft.world.level.storage.ServerLevelData)world.getLevelData();
        boolean rain=data.isRaining(),thunder=data.isThundering();
        float rainLevel=world.getRainLevel(1),thunderLevel=world.getThunderLevel(1);
        var column=h.absolutePos(new BlockPos(1,0,1));
        // Adjacent GameTest structures clear up to their own height + 20. Keep the weather
        // fixture above that region while remaining inside the build height and loaded chunk.
        var pos=new BlockPos(column.getX(),world.getMaxBuildHeight()-16,column.getZ());
        // 1.20.1 GameTestHelper.relativePos applies an extra 180-degree rotation. Use world positions throughout.
        var min=pos.offset(-8,-8,-8);var max=pos.offset(8,8,8);
        world.getServer().getCommands().performPrefixedCommand(world.getServer().createCommandSourceStack().withLevel(world).withSuppressedOutput(),
            "fillbiome "+min.getX()+" "+min.getY()+" "+min.getZ()+" "+max.getX()+" "+max.getY()+" "+max.getZ()+" minecraft:plains");
        Runnable cleanup=()->{
            world.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());world.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            data.setRaining(rain);data.setThundering(thunder);world.setRainLevel(rainLevel);world.setThunderLevel(thunderLevel);
        };
        // GameTestSequence can continue executing later steps after a thenExecute assertion.
        // Preserve the first failure instead of replacing it with a missing entity after cleanup.
        boolean[] failed={false};
        java.util.function.Consumer<Runnable> checked=action->{
            if(failed[0])return;
            try{action.run();}catch(RuntimeException error){failed[0]=true;cleanup.run();throw error;}
        };
        var sequence=h.startSequence();
        for(String id:java.util.List.of("mixing_bowl","bathing_pot","juicer")){
            sequence.thenExecute(()->checked.accept(()->{
                world.setRainLevel(1);world.setThunderLevel(0);data.setRaining(true);data.setThundering(false);
                world.setBlockAndUpdate(pos,net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",id)).defaultBlockState());
            }));
            // isRainingAt reads SKY light, which the threaded light engine updates asynchronously.
            // Heightmap + 4 is not sufficient after placing/removing the preceding test roof.
            sequence.thenWaitUntil(()->h.assertTrue(failed[0]||world.isRainingAt(pos.above()),"wait for exposed rainy fixture skylight"));
            sequence.thenExecute(()->checked.accept(()->{
                var tool=(com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity)world.getBlockEntity(pos);
                h.assertTrue(tool!=null,"weather fixture remains at "+pos+", block="+world.getBlockState(pos));
                var climate=world.getBiome(pos).value().getModifiedClimateSettings();
                int expected=com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity.rainfallAmount(climate.downfall(),climate.temperature(),false);
                int before=tool.displayFluid().getAmount();h.assertTrue(expected>0,"plains fixture has rain");tool.collectRain();
                h.assertTrue(tool.displayFluid().getAmount()==before+(id.equals("juicer")?0:expected),"only open bowl and bath collect rain");
                world.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());before=tool.displayFluid().getAmount();tool.collectRain();
                h.assertTrue(tool.displayFluid().getAmount()==before,"roof prevents collection");world.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            }));
            sequence.thenWaitUntil(()->h.assertTrue(failed[0]||world.isRainingAt(pos.above()),"wait for removed roof skylight"));
            sequence.thenExecute(()->checked.accept(()->{
                var tool=(com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity)world.getBlockEntity(pos);
                h.assertTrue(tool!=null,"weather fixture remains at "+pos+", block="+world.getBlockState(pos));
                if(!id.equals("juicer")){
                    var climate=world.getBiome(pos).value().getModifiedClimateSettings();
                    int expected=com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity.rainfallAmount(climate.downfall(),climate.temperature(),false);
                    int before=tool.displayFluid().getAmount();data.setThundering(true);world.setThunderLevel(1);tool.collectRain();
                    h.assertTrue(tool.displayFluid().getAmount()==before+expected*2,"thunder doubles rainfall");data.setThundering(false);world.setThunderLevel(0);
                    for(int i=0;i<100;i++)tool.collectRain();h.assertTrue(tool.displayFluid().getAmount()==8000,"rain never overflows or opens duplicate water tanks");
                    var copy=new com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity(pos,tool.getBlockState());copy.load(tool.saveWithoutMetadata());
                    h.assertTrue(copy.displayFluid().getAmount()==8000,"collected water persists");
                }
            }));
        }
        sequence.thenExecute(()->checked.accept(()->{
            h.assertTrue(com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity.rainfallAmount(0,1,false)==0
                &&com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity.rainfallAmount(1,0.1F,false)==0,"dry and cold biomes collect nothing");cleanup.run();
        })).thenSucceed();
    }
}
