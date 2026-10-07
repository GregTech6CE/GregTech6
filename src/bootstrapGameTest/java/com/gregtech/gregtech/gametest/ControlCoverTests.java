package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.cover.MachineCoverSpec;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class ControlCoverTests {
    private static final BlockPos POS=new BlockPos(4,3,4);
    private static BasicMachineBlockEntity machine(GameTestHelper h){
        var block=MachineRegistry.basicMachines().get(0).get();h.setBlock(POS,Blocks.AIR);h.setBlock(POS,block);
        var m=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        var map=new RecipeMap(null,"control_cover_test_"+Long.toUnsignedString(m.getBlockPos().asLong()),"Test","test",1,1,1,0,0,0,1,false,false,false,false);
        map.addRecipe(new Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},null,null,null,null,4,32,0));
        m.setSpec(BasicMachineSpec.builder("control_cover_test",block.basicSpec().material()).machineType("test").energy(GregTechTags.Energy.EU,32).recipes(map).faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());return m;
    }
    private static void tick(BasicMachineBlockEntity m){m.doEnergyInjection(GregTechTags.Energy.EU,null,32,1,true);BasicMachineBlockEntity.serverTick(m.getLevel(),m.getBlockPos(),m.getBlockState(),m);}
    private static void attach(BasicMachineBlockEntity m,Direction side,MachineCoverSpec spec){m.attachCover(side,new ItemStack(GTTechnological.get(spec.id)));}
    @GameTest(template="test_blueprint_empty") public static void switchReadsOnlyItsFaceAndCrowbarReturnsDefaults(GameTestHelper h){
        var m=machine(h);attach(m,Direction.NORTH,MachineCoverSpec.REDSTONE);m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
        h.setBlock(POS.east(),Blocks.REDSTONE_BLOCK);tick(m);
        h.assertTrue(m.inventory().getStackInSlot(0).getCount()==1&&m.machineControl(null).progress()==0,"wrong-face power cannot start a machine");
        h.setBlock(POS.north(),Blocks.REDSTONE_BLOCK);tick(m);long progress=m.machineControl(null).progress();
        h.assertTrue(progress>0&&m.inventory().getStackInSlot(0).isEmpty(),"cover-face power starts actual recipe");
        h.setBlock(POS.north(),Blocks.AIR);tick(m);h.assertTrue(m.machineControl(null).progress()==progress,"switch pauses existing work");
        h.assertTrue(m.configureControlCover(Direction.NORTH,false),"screwdriver inversion supported");
        var saved=m.saveWithoutMetadata();m.load(saved);
        h.assertTrue(MachineCoverSpec.inverted(m.getCover(Direction.NORTH)),"inversion persists in attached cover NBT");
        tick(m);h.assertTrue(m.machineControl(null).progress()>progress,"inverted cover runs without its face powered");
        var removed=m.removeCover(Direction.NORTH);h.assertTrue(!MachineCoverSpec.inverted(removed),"crowbar returns default stackable cover");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void autoRedstoneFinishesOneJobAfterSignalStops(GameTestHelper h){
        var m=machine(h);attach(m,Direction.NORTH,MachineCoverSpec.AUTO_REDSTONE);m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE,2));
        h.setBlock(POS.north(),Blocks.REDSTONE_BLOCK);tick(m);h.setBlock(POS.north(),Blocks.AIR);
        for(int i=0;i<6;i++)tick(m);
        h.assertTrue(m.inventory().getStackInSlot(0).getCount()==1,"auto redstone cannot consume next input after completing first job");
        h.assertTrue(m.inventory().getStackInSlot(1).is(Items.DIAMOND)&&m.inventory().getStackInSlot(1).getCount()==1,"current job completes once after power falls");
        h.assertTrue(!m.machineControl(null).active(),"completed machine waits for next signal");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void automaticCoverWakesOnInputWithoutConsumingDuringProbe(GameTestHelper h){
        var m=machine(h);attach(m,Direction.NORTH,MachineCoverSpec.AUTOMATIC);tick(m);
        h.assertTrue(!m.isRunning(),"empty automatic machine stays idle despite energy");
        m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
        for(int i=0;i<6;i++)tick(m);
        h.assertTrue(m.inventory().getStackInSlot(1).is(Items.DIAMOND)&&m.inventory().getStackInSlot(1).getCount()==1,"readiness probe does not consume or duplicate input");
        h.assertTrue(!m.isRunning(),"empty machine returns to idle after completion");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void rebootWindowsAndMachineTimerPersist(GameTestHelper h){
        var idle=new MachineCoverSpec.State(false,false,false,false);var processing=new MachineCoverSpec.State(false,true,true,false);
        for(var spec:MachineCoverSpec.values())if(spec.interval>0){
            h.assertTrue(!spec.allows(idle,0,false,spec.interval-11)&&spec.allows(idle,0,false,spec.interval-10)&&spec.allows(idle,0,false,spec.interval-1)&&!spec.allows(idle,0,false,spec.interval),"exact ten-tick reboot window: "+spec);
            h.assertTrue(spec.allows(processing,0,false,0),"running recipe survives closed timer window");
        }
        var m=machine(h);attach(m,Direction.NORTH,MachineCoverSpec.TIMER_1);m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
        var saved=m.saveWithoutMetadata();saved.putLong("gt.cover_ticks",1188);m.load(saved);tick(m);
        h.assertTrue(m.inventory().getStackInSlot(0).getCount()==1,"does not start before timer window");
        saved=m.saveWithoutMetadata();m.load(saved);m.machineControl(null).setEnabled(false);tick(m);
        h.assertTrue(m.inventory().getStackInSlot(0).isEmpty()&&m.machineControl(null).progress()>0,"saved timer resumes into startup window");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void detectorOutputsAreSidedAndSeparateSuccessFromProcessing(GameTestHelper h){
        var m=machine(h);
        tick(m);for(var side:Direction.values())h.assertTrue(h.getLevel().getSignal(m.getBlockPos(),side)==0,"machine without emitting cover never powers surroundings");
        attach(m,Direction.NORTH,MachineCoverSpec.POSSIBLE);attach(m,Direction.EAST,MachineCoverSpec.RUNNING);attach(m,Direction.SOUTH,MachineCoverSpec.PROCESSING);attach(m,Direction.WEST,MachineCoverSpec.SUCCESS);
        tick(m);h.assertTrue(m.getRedstoneSignal(Direction.EAST)==15&&m.getRedstoneSignal(Direction.SOUTH)==0&&m.getRedstoneSignal(Direction.WEST)==0,"standby energy differs from processing and success");
        m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));tick(m);
        h.assertTrue(m.getRedstoneSignal(Direction.NORTH)==15&&m.getRedstoneSignal(Direction.SOUTH)==15&&m.getRedstoneSignal(Direction.WEST)==0,"processing is not successful output yet");
        for(int i=0;i<3;i++)tick(m);
        h.assertTrue(m.getRedstoneSignal(Direction.WEST)==15,"completed output produces success pulse");
        h.assertTrue(h.getLevel().getSignal(m.getBlockPos(),Direction.EAST)==15&&h.getLevel().getSignal(m.getBlockPos(),Direction.UP)==0,"vanilla opposite-side query emits only on installed face");
        tick(m);h.assertTrue(m.getRedstoneSignal(Direction.WEST)==0&&m.getRedstoneSignal(Direction.NORTH)==0,"success pulse resets and input readiness refreshes");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void invertedStrongDetectorSurvivesReloadAndRemoval(GameTestHelper h){
        var m=machine(h);attach(m,Direction.NORTH,MachineCoverSpec.PROCESSING);
        m.configureControlCover(Direction.NORTH,false);m.configureControlCover(Direction.NORTH,true);tick(m);
        h.assertTrue(h.getLevel().getDirectSignal(m.getBlockPos(),Direction.SOUTH)==15,"cutter enables strong power with inverted idle condition");
        var saved=m.saveWithoutMetadata();m.load(saved);tick(m);
        h.assertTrue(m.getStrongRedstoneSignal(Direction.NORTH)==15,"both detector flags survive reload");
        m.removeCover(Direction.NORTH);h.assertTrue(h.getLevel().getSignal(m.getBlockPos(),Direction.SOUTH)==0&&h.getLevel().getDirectSignal(m.getBlockPos(),Direction.SOUTH)==0,"removing cover clears both outputs immediately");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void allControlCoverSurvivalRecipesActuallyCraft(GameTestHelper h){
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        for(var spec:MachineCoverSpec.values()){
            var recipe=(com.gregtech.gregtech.recipe.ToolShapedRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","control_covers/"+spec.id)).orElseThrow();
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
            for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++){
                var ingredient=recipe.getIngredients().get(x+y*recipe.getWidth());if(ingredient.isEmpty())continue;
                var item=ingredient.getItems()[0].copy();
                if(item.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)item=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
                grid.setItem(x+y*3,item);
            }
            h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.assemble(grid,h.getLevel().registryAccess()).is(GTTechnological.get(spec.id)),"source pattern crafts implemented cover: "+spec.id);
            var remainder=recipe.getRemainingItems(grid);
            for(int slot=0;slot<9;slot++)if(grid.getItem(slot).getItem() instanceof com.gregtech.gregtech.item.GTToolItem)h.assertTrue(!remainder.get(slot).isEmpty()&&remainder.get(slot).getDamageValue()>0,"reboot-cover screwdriver remains with wear");
        }
        h.succeed();
    }
}
