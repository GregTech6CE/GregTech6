package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.content.logistics.ExtenderSpec;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class PanelCoverTests {
    private static final BlockPos POS=new BlockPos(4,3,4);
    private static ItemStack stack(PanelCover panel){return new ItemStack(GTTechnological.get(panel.id));}
    private static ExtenderBlockEntity relay(GameTestHelper h,ExtenderSpec spec){
        h.setBlock(POS,Blocks.AIR);h.setBlock(POS,GTMiscBlocks.SOURCE_EXTENDERS.get(spec).get().defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.EAST).setValue(SourceExtenderBlock.SECONDARY,Direction.EAST));
        return (ExtenderBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static ExtenderBlockEntity modeRelay(GameTestHelper h){
        var relay=relay(h,ExtenderSpec.UNIVERSAL);h.setBlock(POS.east(),MachineRegistry.electricEngines().get(0).get());return relay;
    }
    private static BasicMachineBlockEntity machine(GameTestHelper h){
        var block=MachineRegistry.basicMachines().get(0).get();h.setBlock(POS,block);
        var be=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        var recipes=new RecipeMap(null,"panel_"+Long.toUnsignedString(be.getBlockPos().asLong()),"Panel test","panel",1,1,1,1,1,0,1,false,false,false,false);
        recipes.addRecipe(new Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},null,null,null,null,100,32,0));
        be.setSpec(BasicMachineSpec.builder("panel_test",block.basicSpec().material()).machineType("test").energy(GregTechTags.Energy.EU,32).recipes(recipes).faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());return be;
    }
    @GameTest(template="test_blueprint_empty") public static void manualSelectorAllFacesUsesRealEngineModes(GameTestHelper h){
        var relay=modeRelay(h);
        for(var side:Direction.values()){
            relay.machineControl(side).setMode(0);h.assertTrue(relay.attachCover(side,stack(PanelCover.MANUAL)),"mode-capable host accepts selector");
            for(int mode=1;mode<=16;mode++){h.assertTrue(relay.panels().click(side,.85,.125),"plus button handles click");h.assertTrue(relay.machineControl(side).mode()==(mode&15),"real engine follows all sixteen modes");}
            relay.panels().click(side,.125,.125);h.assertTrue(relay.machineControl(side).mode()==15,"minus wraps zero to fifteen");
            for(double u:new double[]{.2,.4,.6,.8})relay.panels().click(side,u,.65);
            h.assertTrue(relay.machineControl(side).mode()==0,"four bit buttons toggle 8,4,2,1");
            h.assertTrue(!relay.panels().click(side,.5,.4),"display area is not a hidden button");relay.removeCover(side);
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void redstoneSelectorReadsOnlyInstalledFace(GameTestHelper h){
        var relay=modeRelay(h);relay.attachCover(Direction.NORTH,stack(PanelCover.REDSTONE));
        h.setBlock(POS.south(),Blocks.REDSTONE_BLOCK);relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==0,"wrong face ignored");
        h.setBlock(POS.north(),Blocks.REDSTONE_BLOCK);relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==15,"full signal selects fifteen");
        h.setBlock(POS.north(),Blocks.AIR);relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==0,"falling signal selects zero");
        h.setBlock(POS.east(),Blocks.AIR);relay.tickSignals();h.assertTrue(!relay.machineControl(null).available(),"missing target is safe");
        h.setBlock(POS.east(),MachineRegistry.fluxEngines().get(0).get());h.setBlock(POS.north(),Blocks.REDSTONE_BLOCK);relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==15,"replacement target receives current signal");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void buttonPanelSixteenCellsResetAndStyles(GameTestHelper h){
        var relay=modeRelay(h);var side=Direction.NORTH;relay.attachCover(side,stack(PanelCover.BUTTONS));
        for(int n=0;n<16;n++){relay.panels().click(side,(n%4+.5)/4,(n/4+.5)/4);h.assertTrue(relay.machineControl(null).mode()==n,"4x4 button chooses actual mode "+n);}
        relay.panels().configure(side,false,false);relay.panels().click(side,.875,.875);
        for(int i=0;i<8;i++)relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==15,"momentary button stays on through eight post ticks");
        relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==0,"source countdown 10 to 1 resets button");
        for(int i=1;i<=8;i++){relay.panels().configure(side,false,true);h.assertTrue(PanelCoverRuntime.style(relay.getCover(side))==i%8,"eight chisel layouts");}
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void emitterStrongWeakAllFacesAndPersistence(GameTestHelper h){
        var m=machine(h);
        for(var side:Direction.values()){
            m.attachCover(side,stack(PanelCover.EMITTER));m.panels().click(side,.125,.125);
            h.assertTrue(h.getLevel().getSignal(m.getBlockPos(),side.getOpposite())==15&&h.getLevel().getDirectSignal(m.getBlockPos(),side.getOpposite())==0,"weak output uses installed face");
            m.panels().configure(side,true,false);h.assertTrue(h.getLevel().getDirectSignal(m.getBlockPos(),side.getOpposite())==15,"cutter enables strong signal");
            var tag=m.saveWithoutMetadata();m.load(tag);m.panels().afterTick();h.assertTrue(m.getStrongRedstoneSignal(side)==15,"level and strong flag survive reload");m.removeCover(side);
            h.assertTrue(h.getLevel().getSignal(m.getBlockPos(),side.getOpposite())==0,"removed emitter stops immediately");
        }h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void progressScaleMeasuresRealWorkAndInverts(GameTestHelper h){
        var m=machine(h);m.attachCover(Direction.NORTH,stack(PanelCover.PROGRESS));m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
        m.panels().afterTick();h.assertTrue(m.getRedstoneSignal(Direction.NORTH)==0,"idle is zero");
        for(int i=0;i<50;i++){m.doEnergyInjection(GregTechTags.Energy.EU,null,32,1,true);BasicMachineBlockEntity.serverTick(m.getLevel(),m.getBlockPos(),m.getBlockState(),m);}
        h.assertTrue(m.machineControl(null).progress()>0&&m.getRedstoneSignal(Direction.NORTH)==7,"half-complete recipe yields source scale seven");
        m.panels().configure(Direction.NORTH,false,false);m.panels().configure(Direction.NORTH,true,false);h.assertTrue(m.getStrongRedstoneSignal(Direction.NORTH)==8,"inverted half scale is eight");
        h.assertTrue(PanelCoverRuntime.scale(1,Long.MAX_VALUE,15)==1&&PanelCoverRuntime.scale(Long.MAX_VALUE-1,Long.MAX_VALUE,15)==14&&PanelCoverRuntime.scale(Long.MAX_VALUE,Long.MAX_VALUE,15)==15,"long boundaries cannot overflow");h.succeed();
    }
    private static ExtenderBlockEntity energyRelay(GameTestHelper h){
        var relay=relay(h,ExtenderSpec.UNIVERSAL);h.setBlock(POS.east(),GTLasers.ZPM_DISCHARGER_ADVANCED.get());return relay;
    }
    @GameTest(template="test_blueprint_empty") public static void energyScaleUsesQuantumStorageWithoutConversion(GameTestHelper h){
        var relay=energyRelay(h);h.assertTrue(relay.getEnergyCapacitorTypes(Direction.NORTH).contains(GregTechTags.Energy.QU),"QU storage is discoverable despite EU emission");
        h.assertTrue(relay.attachCover(Direction.NORTH,stack(PanelCover.ENERGY)),"real capacitor accepts energy scale");relay.tickSignals();h.assertTrue(relay.panels().signal(Direction.NORTH)==0,"empty module is zero");
        var zpm=(com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.east()));
        zpm.inventory().insertItem(0,com.gregtech.gregtech.content.energy.ZpmEnergy.charged(),false);long before=zpm.totalEnergy();relay.tickSignals();
        h.assertTrue(relay.panels().signal(Direction.NORTH)==14&&zpm.totalEnergy()==before,"nearly-full QU is fourteen without extracting energy");
        relay.panels().configure(Direction.NORTH,false,false);relay.panels().configure(Direction.NORTH,true,false);h.assertTrue(relay.coverStrongSignal(Direction.SOUTH)==1,"reverse and strong configuration");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void energyDisplayTracksChargeAndClientSnapshot(GameTestHelper h){
        var relay=energyRelay(h);relay.attachCover(Direction.NORTH,stack(PanelCover.ENERGY_DISPLAY));relay.tickSignals();h.assertTrue(PanelCoverRuntime.value(relay.getCover(Direction.NORTH))==0,"empty display");
        var zpm=(com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.east()));zpm.inventory().insertItem(0,com.gregtech.gregtech.content.energy.ZpmEnergy.charged(),false);relay.tickSignals();
        var client=new ExtenderBlockEntity(relay.getBlockPos(),relay.getBlockState());client.handleUpdateTag(relay.getUpdateTag());h.assertTrue(PanelCoverRuntime.value(client.getCover(Direction.NORTH))==9,"client receives current energy display stage");
        zpm.inventory().extractItem(0,1,false);relay.tickSignals();h.assertTrue(PanelCoverRuntime.value(relay.getCover(Direction.NORTH))==0,"removed module clears display");
        for(int n=0;n<=10;n++)h.assertTrue(PanelCoverRuntime.scale(n,10,10)==n,"eleven source display stages");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void statusDisplayControlsMachineAndBothLayouts(GameTestHelper h){
        var m=machine(h);m.attachCover(Direction.NORTH,stack(PanelCover.STATUS));m.panels().afterTick();
        h.assertTrue((PanelCoverRuntime.value(m.getCover(Direction.NORTH))&8)!=0,"enabled light visible");
        h.assertTrue(m.panels().click(Direction.NORTH,.8,.9)&&!m.machineControl(null).enabled(),"bottom switch stops real machine");
        m.panels().configure(Direction.NORTH,false,true);h.assertTrue(!m.panels().click(Direction.NORTH,.8,.9),"old switch area is inactive after style change");
        h.assertTrue(m.panels().click(Direction.NORTH,.8,.1)&&m.machineControl(null).enabled(),"top switch starts machine");
        m.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));m.doEnergyInjection(GregTechTags.Energy.EU,null,32,1,true);BasicMachineBlockEntity.serverTick(m.getLevel(),m.getBlockPos(),m.getBlockState(),m);
        h.assertTrue((PanelCoverRuntime.value(m.getCover(Direction.NORTH))&15)==15,"possible/running/active/enabled are separate live lights");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void conductorsUseOnlyReceiverFaces(GameTestHelper h){
        var m=machine(h);m.attachCover(Direction.NORTH,stack(PanelCover.CONDUCTOR_IN));m.attachCover(Direction.SOUTH,stack(PanelCover.CONDUCTOR_OUT));
        h.setBlock(POS.east(),Blocks.REDSTONE_BLOCK);m.panels().afterTick();h.assertTrue(m.getRedstoneSignal(Direction.SOUTH)==0,"unmarked input ignored");
        h.setBlock(POS.north(),Blocks.REDSTONE_BLOCK);m.panels().afterTick();h.assertTrue(m.getRedstoneSignal(Direction.SOUTH)==15&&m.getRedstoneSignal(Direction.NORTH)==0,"receiver is input only and output forwards maximum");
        m.removeCover(Direction.NORTH);h.assertTrue(m.getRedstoneSignal(Direction.SOUTH)==0,"removing last receiver clears output");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void controllerShutterBlocksCachedItemsAndFluids(GameTestHelper h){
        var m=machine(h);var side=Direction.NORTH;
        var items=m.getCapability(ForgeCapabilities.ITEM_HANDLER,side).orElseThrow(IllegalStateException::new);
        var fluids=m.getCapability(ForgeCapabilities.FLUID_HANDLER,side).orElseThrow(IllegalStateException::new);
        m.attachCover(side,stack(PanelCover.SHUTTER));h.assertTrue(!m.isFaceShuttered(side),"normal shutter open without stopped flag");
        m.attachCover(Direction.SOUTH,stack(PanelCover.CONTROLLER));m.panels().beforeTick();h.assertTrue(m.isFaceShuttered(side),"unpowered normal controller closes shutter");
        var initiallyClosed=m.getCapability(ForgeCapabilities.ITEM_HANDLER,side).orElseThrow(IllegalStateException::new);
        h.assertTrue(initiallyClosed.insertItem(0,new ItemStack(Items.APPLE),false).getCount()==1,"closed-state discovery returns a live gated handle");
        h.assertTrue(items.insertItem(0,new ItemStack(Items.APPLE),false).getCount()==1&&fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0,"previously cached handlers cannot bypass gate");
        h.setBlock(POS.south(),Blocks.REDSTONE_BLOCK);m.panels().beforeTick();h.assertTrue(items.insertItem(0,new ItemStack(Items.APPLE),false).isEmpty()&&fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==100,"same handlers resume after signal rises");
        h.assertTrue(initiallyClosed.insertItem(0,new ItemStack(Items.APPLE),true).isEmpty(),"handle discovered while closed resumes without rediscovery");
        m.panels().configure(side,false,false);h.assertTrue(m.isFaceShuttered(side),"inverted shutter closes with running covers");
        m.panels().configure(Direction.SOUTH,false,false);h.assertTrue(!m.isFaceShuttered(side),"inverted controller and shutter compose");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void controllerPausesButtonsAndExtenderShutter(GameTestHelper h){
        var relay=modeRelay(h);relay.attachCover(Direction.NORTH,stack(PanelCover.BUTTONS));relay.attachCover(Direction.SOUTH,stack(PanelCover.CONTROLLER));relay.tickSignals();
        h.assertTrue(!relay.panels().click(Direction.NORTH,.875,.875),"stopped selectors ignore clicks");
        relay.removeCover(Direction.SOUTH);h.assertTrue(!relay.panels().click(Direction.NORTH,.875,.875),"stopped flag stays latched while other covers remain");
        relay.removeCover(Direction.NORTH);h.setBlock(POS.east(),Blocks.BARREL);relay.attachCover(Direction.NORTH,stack(PanelCover.SHUTTER));
        var items=relay.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).orElseThrow(IllegalStateException::new);
        h.assertTrue(items.insertItem(0,new ItemStack(Items.APPLE),false).isEmpty(),"open extender gate forwards actual item");relay.panels().configure(Direction.NORTH,false,false);
        h.assertTrue(items.extractItem(0,1,false).isEmpty(),"cached extender handler respects inverted closed gate");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void faceCoordinatesRoundTripAndCoverSave(GameTestHelper h){
        var relay=modeRelay(h);relay.attachCover(Direction.NORTH,stack(PanelCover.BUTTONS));relay.panels().click(Direction.NORTH,.875,.875);relay.panels().configure(Direction.NORTH,false,true);
        var data=relay.saveWithoutMetadata();relay.machineControl(null).setMode(0);relay.load(data);relay.tickSignals();h.assertTrue(relay.machineControl(null).mode()==15&&PanelCoverRuntime.style(relay.getCover(Direction.NORTH))==1,"reload restores mode and style");
        for(var side:Direction.values())for(int n=0;n<16;n++){
            double u=(n%4+.5)/4,v=(n/4+.5)/4;var p=CoverFaceCoordinates.to(side,u,v,.063);var uv=CoverFaceCoordinates.from(side,p.x,p.y,p.z);
            h.assertTrue(Math.abs(u-uv.u())<1e-8&&Math.abs(v-uv.v())<1e-8,"render and click coordinates agree: "+side);
        }
        var removed=relay.removeCover(Direction.NORTH);h.assertTrue(PanelCoverRuntime.value(removed)==0&&PanelCoverRuntime.style(removed)==0,"crowbar returns default stackable cover");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void unsupportedAttachmentsAndSurvivalRecipes(GameTestHelper h){
        var m=machine(h);h.assertTrue(!m.attachCover(Direction.NORTH,stack(PanelCover.MANUAL))&&!m.attachCover(Direction.NORTH,stack(PanelCover.ENERGY)),"no fake modes or capacitor on processing machines");
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int s){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        for(var panel:PanelCover.values()){
            var recipe=(com.gregtech.gregtech.recipe.ToolShapedRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","panel_covers/"+panel.id)).orElseThrow();
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
            for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++){
                var ingredient=recipe.getIngredients().get(x+y*recipe.getWidth());if(ingredient.isEmpty())continue;
                var item=ingredient.getItems()[0].copy();
                if(item.getItem() instanceof com.gregtech.gregtech.item.GTToolItem){
                    var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem()).getPath();
                    item=com.gregtech.gregtech.item.GTToolItem.create(key.contains("screwdriver")?com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER:com.gregtech.gregtech.api.tool.GTToolType.WRENCH,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
                }
                grid.setItem(x+y*3,item);
            }
            h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.assemble(grid,h.getLevel().registryAccess()).is(GTTechnological.get(panel.id)),"real crafting resolves every ingredient: "+panel.id);
            var remaining=recipe.getRemainingItems(grid);for(int i=0;i<9;i++)if(grid.getItem(i).getItem() instanceof com.gregtech.gregtech.item.GTToolItem)h.assertTrue(!remaining.get(i).isEmpty()&&remaining.get(i).getDamageValue()>0,"shutter tools retained with wear");
        }h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void playerClicksMatchFaceTexturesAndUseOneCover(GameTestHelper h){
        var relay=modeRelay(h);var player=h.makeMockPlayer();player.getAbilities().instabuild=false;
        var block=(SourceExtenderBlock)relay.getBlockState().getBlock();
        for(var side:Direction.values()){
            var held=stack(PanelCover.MANUAL);held.setCount(2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
            var point=CoverFaceCoordinates.to(side,.85,.125,0).add(relay.getBlockPos().getX(),relay.getBlockPos().getY(),relay.getBlockPos().getZ());
            var hit=new net.minecraft.world.phys.BlockHitResult(point,side,relay.getBlockPos(),false);
            block.use(relay.getBlockState(),h.getLevel(),relay.getBlockPos(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(held.getCount()==1&&!relay.getCover(side).isEmpty(),"real right click consumes exactly one attachment");
            int before=relay.machineControl(side).mode();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            block.use(relay.getBlockState(),h.getLevel(),relay.getBlockPos(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(relay.machineControl(side).mode()==((before+1)&15),"rendered plus location operates actual block face "+side);relay.removeCover(side);
        }
        relay.attachCover(Direction.NORTH,stack(PanelCover.BUTTONS));
        var chisel=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.CHISEL,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,chisel);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(relay.getBlockPos()),Direction.NORTH,relay.getBlockPos(),false);
        block.use(relay.getBlockState(),h.getLevel(),relay.getBlockPos(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(PanelCoverRuntime.style(relay.getCover(Direction.NORTH))==1&&chisel.getDamageValue()==1,"chisel changes style and wears exactly once");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void brokenExtenderDropsConfiguredPanelOnce(GameTestHelper h){
        var relay=relay(h,ExtenderSpec.INVENTORY);relay.attachCover(Direction.NORTH,stack(PanelCover.EMITTER));relay.panels().click(Direction.NORTH,.125,.125);
        h.getLevel().destroyBlock(relay.getBlockPos(),true);
        var entities=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(relay.getBlockPos()).inflate(1));
        int count=0;for(var entity:entities)if(entity.getItem().is(GTTechnological.get(PanelCover.EMITTER.id))){count+=entity.getItem().getCount();h.assertTrue(PanelCoverRuntime.value(entity.getItem())==15,"broken host preserves panel setting");}
        h.assertTrue(count==1,"breaking extender drops exactly one attached panel");h.succeed();
    }
}
