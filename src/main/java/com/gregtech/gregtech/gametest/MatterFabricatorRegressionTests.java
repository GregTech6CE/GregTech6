package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MatterFabricatorRegressionTests {
    private static MatterFabricatorBlockEntity build(GameTestHelper h,Direction front) {
        var block=MachineRegistry.basicMachines().stream().map(e->e.get()).filter(b->b.basicSpec().machineName().equals("largemassfab")).findFirst().orElseThrow();
        var pos=new BlockPos(5,2,5);h.setBlock(pos,block.defaultBlockState().setValue(BasicMachineBlock.FACING,front));
        for(var cell:MatterFabricatorStructure.CELLS)h.setBlock(cell.at(pos,front),cell.block());
        var machine=(MatterFabricatorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(machine.isStructureOk(),"original matter structure forms");return machine;
    }
    private static void tick(MatterFabricatorBlockEntity m){BasicMachineBlockEntity.serverTick(m.getLevel(),m.getBlockPos(),m.getBlockState(),m);}
    @GameTest(template="test_blueprint_empty")
    public static void elementalIronRunsOnQuantumAndPersistsWithoutPower(GameTestHelper h) {
        var machine=build(h,Direction.NORTH);
        var port=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(machine.getBlockPos().east());
        h.assertTrue(port.doEnergyInjection(GregTechTags.Energy.EU,Direction.UP,131072,1,true)==0,"QU wall rejects EU");
        h.assertTrue(port.doEnergyInjection(GregTechTags.Energy.QU,Direction.UP,131072,1,false)==1&&machine.getEnergyTick()==0,"QU simulation is inert");
        var iron=GTItems.getStack(MaterialPrefix.ingot,GTMaterialRegistry.get("Iron"),1);
        var items=port.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new AssertionError("item port"));
        h.assertTrue(items.insertItem(0,iron,false).isEmpty(),"wall inserts real iron ingot");
        port.doEnergyInjection(GregTechTags.Energy.QU,Direction.UP,1048576,1,true);tick(machine);
        int progress=machine.getProgressPercent();h.assertTrue(progress>0&&machine.inventory().getStackInSlot(0).isEmpty(),"one input reserved on first quantum tick");
        tick(machine);h.assertTrue(machine.getProgressPercent()==progress,"lack of QU pauses instead of resetting work");
        var saved=machine.saveWithoutMetadata();machine.load(saved);
        h.assertTrue(machine.getProgressPercent()==progress,"pending matter job survives NBT reload");
        for(int i=0;i<6;i++){port.doEnergyInjection(GregTechTags.Energy.QU,Direction.UP,1048576,1,true);tick(machine);}
        var charged=machine.drain(new FluidStack(GTFluids.still("MatterCharged").get(),1000),FluidAction.EXECUTE);
        var neutral=machine.drain(new FluidStack(GTFluids.still("MatterNeutral").get(),1000),FluidAction.EXECUTE);
        h.assertTrue(charged.getAmount()==26&&neutral.getAmount()==30,"iron yields exactly 26 charged and 30 neutral matter after 7340032 QU");
        tick(machine);h.assertTrue(machine.drain(1000,FluidAction.EXECUTE).isEmpty(),"no duplicated nucleons");h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void processorCountsAirAndBrokenPorts(GameTestHelper h) {
        var machine=build(h,Direction.EAST);var front=Direction.EAST;
        var candidates=MatterFabricatorStructure.CELLS.stream().filter(c->c.part()==18202||c.part()==18204).toList();
        var a=candidates.get(0).at(machine.getBlockPos(),front);var b=candidates.get(4).at(machine.getBlockPos(),front);
        var first=h.getLevel().getBlockState(a);var second=h.getLevel().getBlockState(b);
        h.getLevel().setBlock(a,second,3);h.getLevel().setBlock(b,first,3);
        h.assertTrue(machine.isStructureOk(),"top processors can swap positions while preserving 4+4 counts");
        var cell=MatterFabricatorStructure.CELLS.stream().filter(c->c.part()==18031).findFirst().orElseThrow();
        var port=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(cell.at(machine.getBlockPos(),front));
        var center=MatterFabricatorStructure.CELLS.stream().filter(c->c.role()==Role.AIR).findFirst().orElseThrow().at(machine.getBlockPos(),front);
        h.getLevel().setBlock(center,Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(!machine.isStructureOk()&&port.doEnergyInjection(GregTechTags.Energy.QU,Direction.UP,32,1,true)==0,"center obstruction invalidates cached ports immediately");
        h.getLevel().setBlock(center,Blocks.AIR.defaultBlockState(),3);h.assertTrue(machine.isStructureOk(),"clearing center rebinds");
        h.getLevel().setBlock(a,first,3);h.assertTrue(!machine.isStructureOk(),"incorrect processor ratio is rejected");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void compactGradesAndOriginalEfficiency(GameTestHelper h) {
        var variants=MachineRegistry.basicMachines().stream().map(e->e.get()).filter(b->b.basicSpec().machineName().equals("massfab")).toList();
        h.assertTrue(variants.size()==5,"all five original QU tiers registered");
        for(var block:variants) {
            var spec=block.basicSpec();h.assertTrue(spec.energyIn()==(32L << (2*(spec.tier()-1)))&&spec.material()==GTMaterialRegistry.get("Osmiridium"),"original energy and casing for tier "+spec.tier());
        }
        var block=variants.stream().filter(b->b.basicSpec().tier()==1).findFirst().orElseThrow();
        var pos=new BlockPos(1,1,1);h.setBlock(pos,block);
        var machine=(CompactMatterFabricatorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var map=new com.gregtech.gregtech.api.recipe.RecipeMap(null,"matter_efficiency_test","Test","test",1,1,1,0,0,0,1,false,false,false,false);
        map.addRecipe(new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{new ItemStack(net.minecraft.world.item.Items.APPLE)},new ItemStack[]{new ItemStack(net.minecraft.world.item.Items.DIAMOND)},null,null,null,null,32,1,0));
        machine.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("matter_efficiency_test",block.basicSpec().material()).machineType("massfab").energy(GregTechTags.Energy.QU,32).tier(1).recipes(map).build());
        machine.inventory().setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.APPLE));
        machine.doEnergyInjection(GregTechTags.Energy.QU,null,32,1,true);
        BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);
        h.assertTrue(machine.getProgressPercent()==50,"T1 consumes 64 QU for 32 QU of useful work at 50 percent efficiency");
        BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);
        h.assertTrue(machine.getProgressPercent()==50,"compact machine retains work without power");
        machine.doEnergyInjection(GregTechTags.Energy.QU,null,32,1,true);BasicMachineBlockEntity.serverTick(h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);
        h.assertTrue(machine.inventory().getStackInSlot(1).is(net.minecraft.world.item.Items.DIAMOND),"second packet completes exactly one product");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalMatterRulesAndManufacturingExist(GameTestHelper h) {
        var iron=GTItems.getStack(MaterialPrefix.ingot,GTMaterialRegistry.get("Iron"),1);
        var recipe=MachineRecipeMaps.Massfab.findRecipe(java.util.List.of(iron),java.util.List.of(),false,2,1);
        h.assertTrue(recipe!=null&&recipe.mEUt==1&&recipe.mDuration==7340032,"iron original nucleon cost retained");
        var bronze=GTItems.getStack(MaterialPrefix.ingot,GTMaterialRegistry.get("Bronze"),1);
        h.assertTrue(MachineRecipeMaps.Massfab.findRecipe(java.util.List.of(bronze),java.util.List.of(),false,2,1)==null,"alloys are not fabricated as fictional elements");
        var dilithium=GTItems.getStack(MaterialPrefix.dust,GTMaterialRegistry.get("Dilithium"),1);
        var special=MachineRecipeMaps.Massfab.findRecipe(java.util.List.of(new ItemStack(GTTechnological.selectorTag(0)),dilithium),java.util.List.of(),false,2,1);
        h.assertTrue(special!=null&&special.mDuration==10368&&special.mEUt==16&&special.mFluidOutputs[0].getAmount()==103680,"original dilithium ender-matter conversion is retained");
        for(String id:java.util.List.of("largemassfab_lead","large_osmium_coil","conversion_processor_unit"))
            h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","matter/"+id)).isPresent(),"matter manufacturing recipe loaded: "+id);
        for(int tier=1;tier<=5;tier++) {
            String id=tier==1?"massfab_osmium":"massfab_osmiridium_t"+tier;
            h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","matter/"+id)).isPresent(),"compact machine crafting loaded: "+id);
        }
        h.succeed();
    }
}
