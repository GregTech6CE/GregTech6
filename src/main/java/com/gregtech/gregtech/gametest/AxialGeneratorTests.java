package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class AxialGeneratorTests {
    private static AxialGeneratorBlockEntity assemble(GameTestHelper h,BlockPos pos,Block block,Direction front) {
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,block.defaultBlockState().setValue(DirectionalBlock.FACING,front));
        var grade=AxialGeneratorDefinitions.grade(block);
        grade.cells().forEach((cell,part)->h.setBlock(pos.relative(front.getClockWise(),cell.getX()).above(cell.getY()).relative(front.getOpposite(),cell.getZ()),part));
        var machine=(AxialGeneratorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(machine.isStructureOk(),"solid structure forms: "+grade.id()+" / "+front);
        return machine;
    }
    private static BasicMachineBlockEntity sink(GameTestHelper h,BlockPos pos,GregTechTags.Tag energy,long size) {
        var block=MachineRegistry.basicMachines().iterator().next().get();h.setBlock(pos,Blocks.AIR);h.setBlock(pos,block);
        var receiver=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        receiver.setSpec(BasicMachineSpec.builder("axial_receiver",block.basicSpec().material()).machineType("test")
                .energy(energy,size).recipes(block.basicSpec().recipeMap()).faces(FaceConfig.ALL_SIDES).build());
        return receiver;
    }
    @GameTest(template="test_blueprint_empty")
    public static void steamFourGradesFourFacings(GameTestHelper h) {
        var pos=new BlockPos(6,3,6);
        for(var entry:AxialGeneratorDefinitions.blocks(true))for(var front:Direction.Plane.HORIZONTAL) {
            var machine=(LargeTurbineControllerBlockEntity)assemble(h,pos,entry.get(),front);var grade=machine.grade();
            var output=pos.relative(front.getOpposite(),4);var sink=sink(h,output,GregTechTags.Energy.RU,grade.output());
            int amount=grade.input()*2;
            var steam=new FluidStack(GTFluids.still("Steam").get(),amount);
            h.assertTrue(machine.fill(steam,FluidAction.SIMULATE)==amount&&machine.getFluidInTank(0).isEmpty(),"steam simulation has no side effects");
            h.assertTrue(machine.fill(new FluidStack(GTFluids.still("DistW").get(),amount),FluidAction.EXECUTE)==0,"non-steam rejected");
            h.assertTrue(machine.fill(steam,FluidAction.EXECUTE)==amount,"steam intake");
            machine.tick();
            h.assertTrue(sink.getEnergyTick()==grade.output(),"first half of steam batch produces RU at rear");
            h.assertTrue(machine.getFluidInTank(1).getAmount()==amount/170,"condensation ratio");
            h.assertTrue(machine.saveWithoutMetadata().getInt("gt.steam_counter")==amount%170,"condensation remainder");
            var save=machine.saveWithoutMetadata();machine.load(save);machine.toggleStopped();
            sink=sink(h,output,GregTechTags.Energy.RU,grade.output());machine.tick();
            h.assertTrue(sink.getEnergyTick()==grade.output(),"second pulse survives save/reload and a stop request");
            h.assertTrue(machine.getFluidInTank(0).isEmpty()&&machine.saveWithoutMetadata().getLong("gt.pending")==0,"batch consumed exactly once");
            h.assertTrue(machine.fill(steam,FluidAction.EXECUTE)==0,"stop blocks new steam");
            h.assertTrue(machine.getEnergyOffered(GregTechTags.Energy.RU,null,1)==0,"main cannot duplicate rear shaft output");
            var part=pos.relative(front.getClockWise());h.setBlock(part,Blocks.AIR);
            h.assertTrue(!machine.isStructureOk(),"broken wall invalidates immediately");
            h.setBlock(part,grade.wall());h.assertTrue(machine.isStructureOk(),"wall repair rebinds");
            if(grade.materials().wallId()!=18022) {h.setBlock(part,GTMultiblocks.TANK_WALL_DENSE.get());h.assertTrue(!machine.isStructureOk(),"wrong-grade wall rejected");}
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void dynamoFourGradesAndElectricalBoundaries(GameTestHelper h) {
        var pos=new BlockPos(6,3,6);
        for(var entry:AxialGeneratorDefinitions.blocks(false))for(var front:Direction.Plane.HORIZONTAL) {
            var machine=(LargeDynamoControllerBlockEntity)assemble(h,pos,entry.get(),front);var grade=machine.grade();
            long coils=grade.cells().values().stream().filter(b->b==LargeMachineParts.block(18040)).count();
            h.assertTrue(coils==18&&grade.cells().size()==35,"18 coils and 17 dense walls");
            h.assertTrue(machine.doInject(GregTechTags.Energy.RU,front.getOpposite(),grade.input(),1,true)==0,"rear main face cannot take RU");
            h.assertTrue(machine.doInject(GregTechTags.Energy.EU,front,grade.input(),1,true)==0,"wrong input energy rejected");
            h.assertTrue(machine.doEnergyInjection(GregTechTags.Energy.RU,front,Long.MIN_VALUE,1,true)==0,"signed overflow rejected");
            h.assertTrue(machine.doInject(GregTechTags.Energy.RU,front,grade.input(),1,false)==1&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"injection simulation");
            h.assertTrue(machine.doEnergyInjection(GregTechTags.Energy.RU,front,-grade.input(),1,true)==1,"negative RU direction accepted");
            machine.load(machine.saveWithoutMetadata());
            var sink=sink(h,pos.relative(front.getOpposite(),4),GregTechTags.Energy.EU,grade.output());machine.tick();
            h.assertTrue(sink.getEnergyTick()==grade.output(),"75 percent EU reaches rear");
            h.assertTrue(machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"input consumed once");
            h.assertTrue(machine.doInject(GregTechTags.Energy.RU,front,grade.input(),Long.MAX_VALUE,true)==2,"packet count overflow is bounded by capacity");
            h.setBlock(pos.relative(front.getOpposite(),4),Blocks.AIR);machine.tick();
            h.assertTrue(machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"waste-energy dynamo dissipates disconnected output");
            h.assertTrue(machine.doInject(GregTechTags.Energy.RU,front,grade.inputMaximum()+1L,1,false)==1&&!machine.isOverloaded(),"overload simulation is harmless");
            machine.doInject(GregTechTags.Energy.RU,front,grade.inputMaximum()+1L,1,true);
            h.assertTrue(machine.isOverloaded()&&machine.isStopped(),"overload latches shutdown");
            machine.load(machine.saveWithoutMetadata());h.assertTrue(machine.isOverloaded(),"fault persists");
            machine.toggleStopped();h.assertTrue(!machine.isOverloaded()&&machine.isStopped(),"reset does not immediately restart");
            machine.toggleStopped();h.assertTrue(!machine.isStopped(),"explicit restart");
            var middle=pos.relative(front.getOpposite());h.setBlock(middle,grade.wall());
            h.assertTrue(!machine.isStructureOk(),"solid metal cannot replace a copper coil");
            h.setBlock(middle,GTMultiblocks.LARGE_DYNAMO_WALL.get());
            h.assertTrue(!machine.isStructureOk(),"legacy generic dynamo wall cannot replace GT6 18040 copper coil");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void steamDrivesDynamoAndPreservesFluidsOnBreak(GameTestHelper h) {
        for(int i=0;i<4;i++) {
            var pos=new BlockPos(6,3,2);var dynamoPos=pos.south(4);
            var steam=(LargeTurbineControllerBlockEntity)assemble(h,pos,AxialGeneratorDefinitions.blocks(true).get(i).get(),Direction.NORTH);
            var dynamo=(LargeDynamoControllerBlockEntity)assemble(h,dynamoPos,AxialGeneratorDefinitions.blocks(false).get(i).get(),Direction.NORTH);
            var sink=sink(h,pos.south(8),GregTechTags.Energy.EU,dynamo.grade().output());
            steam.fill(new FluidStack(GTFluids.still("Steam").get(),steam.grade().input()*2),FluidAction.EXECUTE);
            steam.tick();dynamo.tick();h.assertTrue(sink.getEnergyTick()==dynamo.grade().output(),"working steam -> RU -> EU chain at grade "+i);
            var absolute=h.absolutePos(pos);var drops=Block.getDrops(steam.getBlockState(),h.getLevel(),absolute,steam);
            h.assertTrue(drops.size()==1&&drops.get(0).getOrCreateTag().contains("BlockEntityTag"),"controller drops stored fluids and pending pulse");
            var tag=drops.get(0).getTag().getCompound("BlockEntityTag");
            h.assertTrue(tag.getLong("gt.pending")>0&&tag.getCompound("gt.water").getInt("Amount")>0,"pending energy and distilled water included");
            h.setBlock(pos,Blocks.AIR);h.setBlock(dynamoPos,Blocks.AIR);
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void steamPartRoutingPurgeAndOverload(GameTestHelper h) {
        var pos=new BlockPos(6,3,6);
        var machine=(LargeTurbineControllerBlockEntity)assemble(h,pos,GTMultiblocks.LARGE_TURBINE_MAIN.get(),Direction.NORTH);
        var front=h.getLevel().getBlockEntity(h.absolutePos(pos.east())).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        var bottom=h.getLevel().getBlockEntity(h.absolutePos(pos.south().below())).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        int amount=machine.grade().input()*2;
        var steam=new FluidStack(GTFluids.still("Steam").get(),amount);
        h.assertTrue(bottom.fill(steam,FluidAction.EXECUTE)==0,"bottom output cannot fill steam");
        h.assertTrue(front.fill(steam,FluidAction.EXECUTE)==amount,"front structural input relays to controller");
        machine.tick();
        h.assertTrue(front.drain(Integer.MAX_VALUE,FluidAction.EXECUTE).isEmpty(),"front input-only wall cannot drain water");
        var copy=machine.getFluidInTank(1);copy.setAmount(1);
        h.assertTrue(machine.getFluidInTank(1).getAmount()==amount/170,"fluid snapshots cannot mutate storage");
        h.assertTrue(bottom.drain(Integer.MAX_VALUE,FluidAction.SIMULATE).getAmount()==amount/170,"bottom simulation sees condensed water");
        h.assertTrue(bottom.drain(10,FluidAction.EXECUTE).getAmount()==10,"bottom water extraction works");
        machine.tick();
        machine.fill(steam,FluidAction.EXECUTE);
        h.assertTrue(machine.purgeFluid()==amount&&machine.getFluidInTank(1).getAmount()==amount/170-10,"plunger prioritises input");
        h.assertTrue(machine.purgeFluid()==amount/170-10&&machine.getFluidInTank(1).isEmpty(),"second plunge drains output");
        h.setBlock(pos.west(),Blocks.AIR);
        h.assertTrue(front.fill(steam,FluidAction.EXECUTE)==0,"retained capability cannot fill through a broken structure");
        h.setBlock(pos.west(),machine.grade().wall());h.assertTrue(machine.isStructureOk(),"repair forms");
        steam.setAmount(machine.getTankCapacity(0));machine.fill(steam,FluidAction.EXECUTE);machine.tick();
        h.assertTrue(machine.isOverloaded()&&machine.saveWithoutMetadata().getLong("gt.pending")==0,"excessive steam clears queued output and latches overload");
        machine.load(machine.saveWithoutMetadata());h.assertTrue(machine.isOverloaded(),"steam fault survives reload");
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void axialRecipesLoadAndToolsRemain(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for(boolean steam:new boolean[]{true,false})for(var entry:AxialGeneratorDefinitions.blocks(steam)) {
            var recipe=manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech","axial/"+entry.getId().getPath()));
            h.assertTrue(recipe.isPresent(),"manufacturing recipe loads: "+entry.getId());
            var wall=AxialGeneratorDefinitions.grade(entry.get()).wall().asItem();
            h.assertTrue(com.gregtech.gregtech.data.MachineRecipeMaps.Welder.mRecipeList.stream().anyMatch(r->r.mOutputs.length==1&&r.mOutputs[0].is(wall)),"JEI's canonical wall has a welding route");
            h.assertTrue(recipe.get().getIngredients().stream().allMatch(ingredient->ingredient.isEmpty()||ingredient.getItems().length>0),"all ingredients resolve");
        }
        for(var entry:GasTurbineDefinitions.blocks())h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech","axial/"+entry.getId().getPath())).isPresent(),"gas turbine upgrade route");
        var copperCoil = manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech","axial/large_dynamo_wall"));
        h.assertTrue(copperCoil.isPresent()&&copperCoil.get().getResultItem(h.getLevel().registryAccess()).is(LargeMachineParts.block(18040).asItem()),
                "GT6 annealed copper coil recipe crafts original 18040 part");
        for(String material:new String[]{"magnalium","trinitanium","graphene","vibramantium"}) {
            h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech","axial/block_plate_"+material)).isPresent(),"rotor plate packing loads");
            h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech","axial/unpack_"+material+"_plates")).isPresent(),"rotor plate unpacking loads");
        }
        var recipe=(com.gregtech.gregtech.recipe.ToolShapedRecipe)manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech","axial/large_dynamo_main")).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        var inventory=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(int slot=0;slot<9;slot++) {var ingredient=recipe.getIngredients().get(slot);if(!ingredient.isEmpty())inventory.setItem(slot,ingredient.getItems()[0].copy());}
        var wrench=com.gregtech.gregtech.item.GTToolItem.create(GTToolType.WRENCH,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        inventory.setItem(1,wrench);h.assertTrue(recipe.matches(inventory,h.getLevel()),"usable wrench matches original pattern");
        var remaining=recipe.getRemainingItems(inventory);h.assertTrue(remaining.get(1).getDamageValue()==800&&wrench.getDamageValue()==0,"wrench returned with original 800 wear, input untouched");
        wrench.setDamageValue(wrench.getMaxDamage());h.assertTrue(!recipe.matches(inventory,h.getLevel()),"broken wrench cannot craft");
        var info=com.gregtech.gregtech.integration.jade.MultiblockJadeProvider.inspect(assemble(h,new BlockPos(6,3,6),GTMultiblocks.LARGE_DYNAMO_MAIN.get(),Direction.NORTH));
        h.assertTrue(info.getBoolean("formed")&&info.getInt("axialOutput")==3072,"Jade carries actual conversion grade");
        h.succeed();
    }
}
