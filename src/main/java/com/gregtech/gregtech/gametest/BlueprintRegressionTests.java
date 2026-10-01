package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BlueprintRegressionTests {
    @GameTest(template="test_blueprint_empty")
    public static void automaticHammerRequiresReverseStroke(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);var target=pos.east();
        helper.setBlock(pos,GTMiscBlocks.AUTO_HAMMER_STEEL.get().defaultBlockState().setValue(AutoToolBlock.FACING,Direction.EAST));
        helper.setBlock(target,Blocks.STONE);
        var hammer=(AutoHammerBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(hammer.doEnergyInjection(GregTechTags.Energy.KU,Direction.EAST,8,10,true)==0,"front does not accept KU");
        hammer.doEnergyInjection(GregTechTags.Energy.KU,Direction.WEST,8,10,false);
        helper.assertTrue(hammer.storedEnergy()==0,"simulated stroke does not store energy");
        hammer.doEnergyInjection(GregTechTags.Energy.KU,Direction.WEST,8,10,true);hammer.tick();
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(target)).is(Blocks.STONE),"positive stroke alone does not mine");
        hammer.load(hammer.saveWithoutMetadata());
        hammer.doEnergyInjection(GregTechTags.Energy.KU,Direction.WEST,-8,1,true);hammer.tick();
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(target)).isAir() && hammer.storedEnergy()==0,"reverse packet performs saved work once");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty",timeoutTicks=40)
    public static void automaticIgniterHonorsEnergyAndCooldown(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);var target=pos.east();
        helper.setBlock(pos,GTMiscBlocks.AUTO_IGNITER_ALUMINIUM.get().defaultBlockState().setValue(AutoToolBlock.FACING,Direction.EAST));
        helper.setBlock(target,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));
        var igniter=(AutoIgniterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(igniter.doEnergyInjection(GregTechTags.Energy.KU,Direction.WEST,32,1,true)==0,"igniter rejects KU");
        helper.assertTrue(igniter.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,32,1,true)==0,"igniter rejects front input");
        igniter.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true);
        helper.runAtTickTime(12,()->{
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(target)).getValue(CampfireBlock.LIT),"EU powers front ignition");
            helper.assertTrue(igniter.cooldown()>0 && igniter.storedEnergy()==0,"ignition enters cooldown");
            igniter.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true);
            helper.assertTrue(igniter.storedEnergy()==0,"cooldown packets cannot retrigger ignition");
            igniter.load(igniter.saveWithoutMetadata());helper.assertTrue(igniter.cooldown()>0,"cooldown persists");helper.succeed();
        });
    }
    @GameTest(template="test_blueprint_empty")
    public static void longDistanceItemPipelineDisconnectsImmediately(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);var receiver=new BlockPos(5,2,1);var chest=new BlockPos(6,2,1);
        helper.setBlock(pos,GTMiscBlocks.LONG_DIST_ENDPOINT_ITEM.get().defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST));
        helper.setBlock(receiver,GTMiscBlocks.LONG_DIST_ENDPOINT_ITEM.get().defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST));
        for(int x=2;x<=4;x++) helper.setBlock(new BlockPos(x,2,1),GTMiscBlocks.LONG_DIST_PIPE.get());
        helper.setBlock(chest,Blocks.CHEST);
        var sender=helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var capability=sender.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        var stack=new ItemStack(Items.DIAMOND,23);stack.getOrCreateTag().putBoolean("pipeline_test",true);
        helper.assertTrue(capability.insertItem(0,stack,true).isEmpty(),"pipeline simulates receiver capacity");
        var inventory=(ChestBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(chest));
        helper.assertTrue(inventory.getItem(0).isEmpty(),"simulation changes no receiver items");
        helper.assertTrue(capability.insertItem(0,stack,false).isEmpty() && ItemStack.isSameItemSameTags(stack,inventory.getItem(0)) && inventory.getItem(0).getCount()==23,"pipeline forwards item identity and NBT");
        helper.setBlock(new BlockPos(3,2,1),Blocks.AIR);
        helper.assertTrue(capability.extractItem(0,23,false).isEmpty() && inventory.getItem(0).getCount()==23,"broken pipe invalidates even an already obtained capability");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void extenderResolvesReplacedInventory(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);var chest=pos.south();
        helper.setBlock(pos,GTMiscBlocks.EXTENDER_BASIC.get().defaultBlockState().setValue(ExtenderBlock.FACING,Direction.NORTH));helper.setBlock(chest,Blocks.CHEST);
        var entity=helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var cap=entity.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(cap.insertItem(0,new ItemStack(Items.APPLE,7),false).isEmpty(),"extender forwards to opposite side");
        helper.setBlock(chest,Blocks.AIR);
        helper.assertTrue(cap.getSlots()==0 && cap.extractItem(0,7,false).isEmpty(),"removed receiver cannot be accessed through stale capability");
        helper.setBlock(chest,Blocks.CHEST);
        helper.assertTrue(cap.getStackInSlot(0).isEmpty(),"new receiver does not inherit old inventory");helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void implosionStructureProcessingAndWallPorts(GameTestHelper helper) {
        var pos=new BlockPos(2,2,1);var absolute=helper.absolutePos(pos);
        helper.setBlock(pos,GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get());
        helper.setBlock(pos.below(),Blocks.STONE);
        var layout=com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.LAYOUT;
        for(var cell:layout.cells()) helper.setBlock(cell.at(pos,Direction.NORTH),cell.role()==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR?Blocks.AIR:GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get());
        var machine=(com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity)helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(machine.isStructureOk(),"original hollow implosion structure forms");
        var partPos=pos.east();var part=helper.getLevel().getBlockEntity(helper.absolutePos(partPos));
        var items=part.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).orElseThrow(IllegalStateException::new);
        var recipe=com.gregtech.gregtech.data.MachineRecipeMaps.ImplosionCompressor.mRecipeList.iterator().next();
        for(int slot=0;slot<recipe.mInputs.length;slot++) helper.assertTrue(items.insertItem(slot,recipe.mInputs[slot].copy(),false).isEmpty(),"wall accepts real recipe input");
        for(int tick=0;tick<128;tick++) com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.serverTick(helper.getLevel(),absolute,machine.getBlockState(),machine);
        helper.assertTrue(machine.getProgressPercent()==50,"time-powered recipe advances without EU");
        machine.load(machine.saveWithoutMetadata());
        helper.setBlock(partPos,Blocks.AIR);
        helper.assertTrue(!machine.isStructureOk() && items.getSlots()==0,"breaking a wall disables its previously obtained port");
        for(int tick=0;tick<20;tick++) com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.serverTick(helper.getLevel(),absolute,machine.getBlockState(),machine);
        helper.assertTrue(machine.getProgressPercent()==50,"broken structure pauses saved work");
        helper.setBlock(partPos,GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get());
        for(int tick=0;tick<128;tick++) com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.serverTick(helper.getLevel(),absolute,machine.getBlockState(),machine);
        helper.assertTrue(ItemStack.isSameItemSameTags(machine.inventory().getStackInSlot(machine.inputSlots()),recipe.mOutputs[0]),"repairing structure completes original gem recipe");
        helper.assertTrue(machine.inventory().getStackInSlot(2).getCount()==1,"selector circuit survives processing");
        helper.setBlock(pos.above().south(),Blocks.STONE);
        helper.assertTrue(!machine.isStructureOk(),"center cell must remain air");helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void filterGhostsAndBidirectionalRules(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);var chest=pos.north();
        helper.setBlock(pos,GTMiscBlocks.FILTER_ITEMS.get().defaultBlockState().setValue(FilterBlock.FACING,Direction.NORTH));helper.setBlock(chest,Blocks.CHEST);
        var filter=(FilterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());player.getInventory().clearContent();
        var menu=new com.gregtech.gregtech.client.gui.FilterMenu(1,player.getInventory(),filter);
        menu.setCarried(new ItemStack(Items.APPLE,17));
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        helper.assertTrue(menu.getCarried().getCount()==17 && filter.templates().getItem(0).getCount()==1,"setting ghost template consumes no real items");
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,player);
        helper.assertTrue(player.getInventory().isEmpty(),"ghost shift-click cannot create items");
        var cap=filter.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.SOUTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(cap.insertItem(0,new ItemStack(Items.APPLE,7),false).isEmpty(),"whitelisted item inserts");
        helper.assertTrue(cap.insertItem(1,new ItemStack(Items.DIAMOND,3),false).getCount()==3,"unlisted item is returned intact");
        filter.toggleMode();
        helper.assertTrue(cap.extractItem(0,7,false).isEmpty(),"blacklist also blocks extraction");
        filter.load(filter.saveWithoutMetadata());helper.assertTrue(filter.blacklist()&&!filter.permitsItem(new ItemStack(Items.APPLE)),"templates and mode persist");
        menu.clicked(0,1,net.minecraft.world.inventory.ClickType.PICKUP,player);
        helper.assertTrue(cap.extractItem(0,7,false).getCount()==7,"clearing ghost restores allowed extraction");helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void fluidFilterUsesActualFluidIdentity(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);helper.setBlock(pos,GTMiscBlocks.FILTER_FLUIDS.get());
        var filter=(FilterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
        water.getOrCreateTag().putBoolean("special_water",true);
        filter.setTemplate(0,com.gregtech.gregtech.api.fluid.FluidDisplayBinding.display(water));
        helper.assertTrue(filter.permitsFluid(water),"display fluid template resolves exact fluid identity");
        helper.assertTrue(filter.permitsFluid(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000)),"GT6 fluid filter matches fluid identity regardless of NBT");
        filter.toggleMode();helper.assertTrue(!filter.permitsFluid(water),"fluid blacklist reverses the same predicate");helper.succeed();
    }
    @GameTest(template="test_lightning_empty")
    public static void lightningRodStructureAndEnergy(GameTestHelper helper) {
        var pos=new BlockPos(2,110-helper.absolutePos(BlockPos.ZERO).getY(),2);var absolute=helper.absolutePos(pos);
        helper.setBlock(pos,Blocks.AIR);
        helper.setBlock(pos,GTMultiblocks.LIGHTNING_ROD_MAIN.get());
        for(int y=0;y<5;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            if(x==0&&y==0&&z==0) continue;
            helper.setBlock(pos.offset(x,y,z),y%2==0?GTMultiblocks.LIGHTNING_ROD_WALL.get():GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL.get());
        }
        helper.setBlock(pos.above(5),GTMultiblocks.LIGHTNING_ROD_PILLAR.get());
        // captureStrike() needs the tip to see the sky (LightningRodControllerBlockEntity:36). The test
        // structure's own volume is air, but the world ABOVE that volume is real terrain, and in a fresh
        // world this spot can sit under a mountain top (jagged peaks reach past y=190) - which blocked
        // the sky and made this regression test seed-dependent. Clearing the column keeps the assertion
        // about the structure rule instead of about where the structure happened to land.
        for(int y=6;y<=6+90;y++) {
            var above=pos.above(y);
            if(!helper.getLevel().getBlockState(helper.absolutePos(above)).isAir()) helper.setBlock(above,Blocks.AIR);
        }
        var rod=(com.gregtech.gregtech.blockentity.machine.LightningRodControllerBlockEntity)helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(rod!=null,"the lightning rod controller exists at "+absolute);
        helper.assertTrue(rod.isStructureOk()&&rod.captureStrike(),"original alternating layers and unobstructed high rod capture a strike");
        long capacity=rod.getEnergyCapacity(GregTechTags.Energy.EU,null);
        helper.assertTrue(capacity==589824000L && rod.getEnergyStored(GregTechTags.Energy.EU,null)==capacity,"strike stores original EU capacity");
        helper.assertTrue(rod.doEnergyExtraction(GregTechTags.Energy.EU,Direction.NORTH,32768,1,true)==0,"energy exits only at the bottom");
        helper.assertTrue(rod.doEnergyExtraction(GregTechTags.Energy.EU,Direction.DOWN,32768,2,false)==2 && rod.getEnergyStored(GregTechTags.Energy.EU,null)==capacity,"simulated extraction preserves charge");
        rod.doEnergyExtraction(GregTechTags.Energy.EU,Direction.DOWN,32768,2,true);rod.load(rod.saveWithoutMetadata());
        helper.assertTrue(rod.getEnergyStored(GregTechTags.Energy.EU,null)==capacity-65536,"charge and extraction survive save/load");
        helper.setBlock(pos.offset(1,1,0),Blocks.AIR);
        helper.assertTrue(!rod.isStructureOk() && rod.getEnergyOffered(GregTechTags.Energy.EU,Direction.DOWN,32768)==0,"broken coil layer cannot emit energy");
        for(int y=0;y<=5;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) helper.setBlock(pos.offset(x,y,z),Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template="test_blueprint_large")
    public static void largeMachineLayoutsRequireTheirOriginalParts(GameTestHelper helper) {
        for(var front:java.util.List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST))
        for(String name:com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.machines()) {
            var block=com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines().stream().map(entry->entry.get()).filter(b->b.basicSpec().machineName().equals(name)).findFirst().orElseThrow();
            var pos=new BlockPos(6,2,6);
            helper.setBlock(pos,block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,front));
            var machine=(com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            helper.assertTrue(!machine.isStructureOk(),name+" cannot operate as a single block");
            var layout=com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(name);
            for(var cell:layout) helper.setBlock(cell.at(pos,front),cell.block());
            helper.assertTrue(machine.isStructureOk(),name+" accepts its complete original part arrangement");
            var first=layout.stream().filter(cell->cell.role()!=com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR).findFirst().orElseThrow();
            helper.setBlock(first.at(pos,front),Blocks.AIR);
            helper.assertTrue(!machine.isStructureOk(),name+" immediately rejects missing parts");
            helper.setBlock(pos,Blocks.AIR);
            for(var cell:layout) helper.setBlock(cell.at(pos,front),Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void bedrockDrillConsumesLubricantAndConservesOutput(GameTestHelper helper) {
        var pos=new BlockPos(3,6,3);helper.setBlock(pos,GTMultiblocks.BEDROCK_DRILL_MAIN.get());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-5;y<=0;y++) {
            if(x==0&&y==0&&z==0)continue;
            helper.setBlock(pos.offset(x,y,z),y==-5?Blocks.BEDROCK:y==-4?GTMultiblocks.BEDROCK_DRILL_WALL.get():com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18026));
        }
        var machine=(com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(machine.isStructureOk(),"drill heads and four dense titanium layers form above bedrock");
        var input=(com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos.offset(1,-1,0)));
        var fluids=machine.getCapability(ForgeCapabilities.FLUID_HANDLER,null).orElseThrow(IllegalStateException::new);
        helper.assertTrue(fluids.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==0,"water is not lubricant");
        fluids.fill(new net.minecraftforge.fluids.FluidStack(GTFluids.still("Lubricant").get(),200),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(input.doEnergyInjection(GregTechTags.Energy.HU,Direction.EAST,2048,16,true)==0,"old erroneous HU input is rejected");
        helper.assertTrue(input.doEnergyInjection(GregTechTags.Energy.RU,Direction.EAST,2048,16,false)==16&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"simulation does not power drill");
        input.doEnergyInjection(GregTechTags.Energy.RU,Direction.EAST,2048,16,true);
        machine.load(machine.saveWithoutMetadata());machine.tick();
        var inventory=machine.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        helper.assertTrue(inventory.getStackInSlot(0).getCount()==1&&fluids.getFluidInTank(0).getAmount()==100&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"one cycle consumes exactly 32768 RU and 100 mB");
        input.doEnergyInjection(GregTechTags.Energy.RU,Direction.EAST,2048,16,true);machine.tick();
        helper.assertTrue(fluids.getFluidInTank(0).getAmount()==100&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==32768,"occupied output consumes no more resources");
        helper.setBlock(pos.offset(1,-5,0),Blocks.STONE);inventory.extractItem(0,1,false);machine.tick();
        helper.assertTrue(!machine.isStructureOk()&&inventory.getStackInSlot(0).isEmpty(),"missing bedrock stops mining without destroying the deposit");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void largeCrusherUsesOriginalEfficiencyAndRetainsInterruptedWork(GameTestHelper helper) {
        var block=com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines().stream().map(entry->entry.get()).filter(b->b.basicSpec().machineName().equals("largecrusher")).findFirst().orElseThrow();
        var pos=new BlockPos(2,2,1);helper.setBlock(pos,block);helper.setBlock(pos.below(),Blocks.STONE);
        for(var cell:com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells("largecrusher"))helper.setBlock(cell.at(pos,Direction.NORTH),cell.block());
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var recipes=new com.gregtech.gregtech.api.recipe.RecipeMap(null,"blueprint_crusher_test","Test","test",1,1,1,0,0,0,1,false,false,false,false);
        recipes.addRecipe(new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},null,null,null,null,192,32,0));
        machine.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("blueprint_crusher_test",block.basicSpec().material()).machineType("largecrusher").energy(GregTechTags.Energy.RU,512).recipes(recipes).faces(block.basicSpec().faceConfig()).build());
        machine.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
        for(int i=0;i<12;i++) {machine.doEnergyInjection(GregTechTags.Energy.RU,null,512,1,true);com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(helper.getLevel(),helper.absolutePos(pos),machine.getBlockState(),machine);}
        helper.assertTrue(machine.getProgressPercent()==50,"6144 GU recipe costs 12288 RU at original 50 percent efficiency");
        com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(helper.getLevel(),helper.absolutePos(pos),machine.getBlockState(),machine);
        helper.assertTrue(machine.getProgressPercent()==50,"crusher retains work on a missing power tick");
        for(int i=0;i<12;i++) {machine.doEnergyInjection(GregTechTags.Energy.RU,null,512,1,true);com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(helper.getLevel(),helper.absolutePos(pos),machine.getBlockState(),machine);}
        helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.DIAMOND),"24 powered ticks complete exactly one cheap-overclocked job");helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void fluidFuelBatchChecksExhaustAndNbtBeforeConsuming(GameTestHelper helper) {
        var fuel=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,100);
        var exhaust=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,50);
        var recipe=new com.gregtech.gregtech.api.recipe.Recipe(null,null,null,null,new net.minecraftforge.fluids.FluidStack[]{fuel},new net.minecraftforge.fluids.FluidStack[]{exhaust},1,-10,0);
        var input=fuel.copy();input.setAmount(1000);
        var plan=com.gregtech.gregtech.api.recipe.FluidFuelBatch.plan(recipe,input,net.minecraftforge.fluids.FluidStack.EMPTY,100,100,10000);
        helper.assertTrue(plan!=null&&plan.charges()==2&&plan.energy()==20&&plan.input().getAmount()==800&&plan.output().getAmount()==100&&input.getAmount()==1000,"batch limited by exhaust space; planning never mutates input");
        helper.assertTrue(com.gregtech.gregtech.api.recipe.FluidFuelBatch.plan(recipe,input,plan.output(),100,100,10000)==null,"full exhaust prevents fuel consumption");
        var tagged=exhaust.copy();tagged.getOrCreateTag().putBoolean("different",true);
        helper.assertTrue(com.gregtech.gregtech.api.recipe.FluidFuelBatch.plan(recipe,input,tagged,1000,100,10000)==null,"different exhaust NBT cannot be merged");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void heatExchangerUsesRealFuelAndStopsWithBlockedExhaust(GameTestHelper helper) {
        var pos=new BlockPos(3,2,3);helper.setBlock(pos,GTMultiblocks.HEAT_EXCHANGER_MAIN.get());
        for(int y=0;y<2;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            if(x==0&&y==0&&z==0)continue;
            helper.setBlock(pos.offset(x,y,z),y==0||x==0&&z==0?com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18024):GTMultiblocks.HEAT_TRANSMITTER.get());
        }
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(machine.isStructureOk(),"two-layer dense tungsten / heat-transmitter structure forms");
        var recipe=com.gregtech.gregtech.data.FuelRecipeMaps.Hot.mRecipeList.stream().filter(r->r.mFluidInputs.length==1&&r.mFluidOutputs.length==1&&r.mEUt<0).findFirst().orElseThrow();
        var fuel=recipe.mFluidInputs[0].copy();fuel.setAmount(Math.min(163840,recipe.mFluidInputs[0].getAmount()*1000));
        machine.fill(fuel,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        machine.tick();
        int used=fuel.getAmount()-machine.getFluidInTank(0).getAmount();
        long count=used/recipe.mFluidInputs[0].getAmount();
        helper.assertTrue(count>0&&machine.getEnergyStored(GregTechTags.Energy.HU,null)==count*(-recipe.mEUt)*recipe.mDuration&&machine.getFluidInTank(1).getAmount()==count*recipe.mFluidOutputs[0].getAmount(),"real hot-fluid recipe conserves heat and cooled fluid");
        var data=machine.saveWithoutMetadata();data.putLong("gt.hu",0);
        var full=recipe.mFluidOutputs[0].copy();full.setAmount(327680);data.put("gt.cold",full.writeToNBT(new net.minecraft.nbt.CompoundTag()));machine.load(data);
        int before=machine.getFluidInTank(0).getAmount();machine.tick();
        helper.assertTrue(machine.getFluidInTank(0).getAmount()==before&&machine.getEnergyStored(GregTechTags.Energy.HU,null)==0,"exhaust threshold stops fuel consumption");
        helper.setBlock(pos.offset(1,1,0),Blocks.AIR);helper.assertTrue(!machine.isStructureOk(),"missing heat transmitter invalidates structure");helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void workCostMatchesOriginalOverclockingAndRejectsOverflow(GameTestHelper helper) {
        var normal=com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(32,192,1,true,10000,128,512,false);
        helper.assertTrue(normal.minimumPower()==128&&normal.totalWork()==12288,"4x power gives 2x speed and doubles total energy");
        var cheap=com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(32,192,4,true,5000,512,4096,true);
        helper.assertTrue(cheap.minimumPower()==32&&cheap.totalWork()==49152,"cheap overclock retains work, batch and efficiency scale it");
        var timed=com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(0,256,64,false,10000,1,16,false);
        helper.assertTrue(timed.totalWork()==256&&timed.minimumPower()==1,"TU parallel recipes retain their original duration");
        helper.assertTrue(com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(4096,Long.MAX_VALUE,64,true,2500,512,4096,true)==null,"overflowing recipe is rejected before ingredients can be consumed");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void ordinaryTimeMachineRunsWithoutExternalPower(GameTestHelper helper) {
        var block=com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines().stream().map(entry->entry.get()).filter(b->b.basicSpec().machineName().equals("autoclave")).findFirst().orElseThrow();
        helper.assertTrue(block.basicSpec().energyIn()==1&&block.basicSpec().energyInMin()==1&&block.basicSpec().energyInMax()==16,"TU registration uses original time energy limits");
        var pos=new BlockPos(2,2,2);helper.setBlock(pos,block);
        for(var side:Direction.values())helper.setBlock(pos.relative(side),Blocks.STONE);
        var machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var recipes=new com.gregtech.gregtech.api.recipe.RecipeMap(null,"blueprint_time_test","Test","test",1,1,1,0,0,0,1,false,false,false,false);
        recipes.addRecipe(new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},null,null,null,null,4,0,0));
        machine.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("blueprint_time_test",block.basicSpec().material()).machineType("autoclave").energy(GregTechTags.Energy.TU,1).recipes(recipes).faces(block.basicSpec().faceConfig()).build());
        machine.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));
        for(int tick=0;tick<4;tick++)com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(helper.getLevel(),helper.absolutePos(pos),machine.getBlockState(),machine);
        helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.DIAMOND),"time-only recipe completes without an energy cable");helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void gasTurbineConsumesFuelAndEmitsAtRear(GameTestHelper helper){
        var pos=new BlockPos(3,3,1);helper.setBlock(pos,GTMultiblocks.LARGE_GAS_TURBINE_MAIN.get());
        for(int b=0;b<4;b++)for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)if(b!=0||y!=0||x!=0)helper.setBlock(pos.offset(x,y,b),GTMultiblocks.LARGE_GAS_TURBINE_WALL.get());
        var machine=(com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(machine.isStructureOk(),"35 solid walls form the original turbine volume");
        helper.setBlock(pos.east(),GTMultiblocks.TANK_WALL_DENSE.get());
        helper.assertTrue(machine.isStructureOk(),"weldable dense stainless wall shares the original structural identity");
        var recipe=com.gregtech.gregtech.data.FuelRecipeMaps.Gas.mRecipeList.stream().filter(r->r.mFluidInputs.length==1&&r.mFluidOutputs.length>0&&r.mEUt<0&&r.mFluidInputs[0].getFluid()==GTFluids.still("Methane").get()).findFirst().orElseThrow();
        var fuel=recipe.mFluidInputs[0].copy();fuel.setAmount(49152);machine.fill(fuel,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);machine.tick();
        int count=(49152-machine.getFluidInTank(0).getAmount())/recipe.mFluidInputs[0].getAmount();
        helper.assertTrue(machine.getEnergyStored(GregTechTags.Energy.RU,null)==8192&&count>0,"fuel generates the maximum packet at original 2/3 conversion");
        for(int n=0;n<recipe.mFluidOutputs.length;n++)helper.assertTrue(machine.getFluidInTank(n+1).getAmount()==count*recipe.mFluidOutputs[n].getAmount(),"each exhaust is conserved");
        machine.load(machine.saveWithoutMetadata());int remaining=machine.getFluidInTank(0).getAmount();machine.tick();
        helper.assertTrue(machine.getFluidInTank(0).getAmount()==remaining,"no receiver means buffered output blocks further fuel consumption");
        var block=com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines().iterator().next().get();var target=pos.south(4);helper.setBlock(target,block);
        var receiver=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(target));
        receiver.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("turbine_test_receiver",block.basicSpec().material()).machineType("test").energy(GregTechTags.Energy.RU,8192).recipes(block.basicSpec().recipeMap()).faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
        machine.toggleStopped();machine.tick();helper.assertTrue(receiver.getEnergyTick()==8192&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"stored rotation exits rear structural face");
        helper.setBlock(pos.south(),Blocks.AIR);helper.assertTrue(!machine.isStructureOk(),"turbine center is solid, not hollow");helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void multipleExhaustFuelPlanIsAtomic(GameTestHelper helper){
        var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,10);
        var lava=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,20);
        var recipe=new com.gregtech.gregtech.api.recipe.Recipe(null,null,null,null,new net.minecraftforge.fluids.FluidStack[]{water},new net.minecraftforge.fluids.FluidStack[]{water,lava},1,-100,0);
        var input=water.copy();input.setAmount(100);
        var plan=com.gregtech.gregtech.api.recipe.FluidFuelBatch.plan(recipe,input,java.util.List.of(net.minecraftforge.fluids.FluidStack.EMPTY,lava),new int[]{100,20},1000,10000);
        helper.assertTrue(plan==null&&input.getAmount()==100,"second exhaust full rejects entire fuel transaction");
        plan=com.gregtech.gregtech.api.recipe.FluidFuelBatch.plan(recipe,input,java.util.List.of(net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY),new int[]{100,40},1000,10000);
        helper.assertTrue(plan!=null&&plan.charges()==2&&plan.outputs().get(0).getAmount()==20&&plan.outputs().get(1).getAmount()==40&&plan.input().getAmount()==80,"least available exhaust capacity bounds every output and input");helper.succeed();
    }
}
