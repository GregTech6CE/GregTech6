package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class FusionRegressionTests {
    private static final BlockPos ORIGIN=new BlockPos(12,4,10);
    private static FusionReactorControllerBlockEntity build(GameTestHelper h,Direction front) {
        h.setBlock(ORIGIN,GTMultiblocks.FUSION_REACTOR_MAIN.get().defaultBlockState().setValue(BasicMachineBlock.FACING,front));
        var absolute=h.absolutePos(ORIGIN);
        for(var cell:FusionStructure.CELLS)h.getLevel().setBlock(cell.at(absolute,front),cell.block().defaultBlockState(),3);
        var reactor=(FusionReactorControllerBlockEntity)h.getLevel().getBlockEntity(absolute);
        h.assertTrue(reactor.isStructureOk(),"original fusion ring forms facing "+front);
        return reactor;
    }
    private static void tick(FusionReactorControllerBlockEntity r) {FusionReactorControllerBlockEntity.serverTick(r.getLevel(),r.getBlockPos(),r.getBlockState(),r);}
    private static void fixture(FusionReactorControllerBlockEntity r,String id) {
        var map=new RecipeMap(null,id,"Test","test",1,1,1,0,0,0,1,false,false,false,false);
        map.addRecipe(new Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND)},null,null,null,null,3,-8192,16385));
        r.setSpec(BasicMachineSpec.builder(id,r.spec().material()).machineType("fusionreactor").energy(GregTechTags.Energy.TU,1).recipes(map).build());
    }
    private static MultiblockPortBlockEntity port(FusionReactorControllerBlockEntity r,Role role) {
        var cell=FusionStructure.CELLS.stream().filter(c->c.role()==role).findFirst().orElseThrow();
        return (MultiblockPortBlockEntity)r.getLevel().getBlockEntity(cell.at(r.getBlockPos(),r.getBlockState().getValue(BasicMachineBlock.FACING)));
    }
    @GameTest(template="test_fusion_empty",timeoutTicks=200)
    public static void startupSimulationOutputAndContinuousReaction(GameTestHelper h) {
        var r=build(h,Direction.NORTH);fixture(r,"fusion_energy_test");
        var lu=port(r,Role.ENERGY_INPUT);
        h.assertTrue(lu.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,1,true)==0,"cannot precharge an idle fusion core");
        r.inventory().setStackInSlot(0,new ItemStack(Items.APPLE,2));tick(r);
        h.assertTrue(r.chargeRemaining()==16385&&r.inventory().getStackInSlot(0).getCount()==1,"recipe inputs reserved once before startup");
        h.assertTrue(lu.doEnergyInjection(GregTechTags.Energy.EU,Direction.UP,8192,1,true)==0,"startup rejects EU");
        h.assertTrue(lu.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,16385,1,true)==0,"startup rejects oversized packets");
        h.assertTrue(lu.getEnergyDemanded(GregTechTags.Energy.LU,Direction.UP,8192)==3,"network demand includes the final partial startup packet");
        h.assertTrue(lu.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,3,false)==3&&r.chargeRemaining()==16385,"simulation including partial final packet is inert");
        h.assertTrue(lu.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,Long.MAX_VALUE,true)==3&&r.chargeRemaining()==0,"last packet saturates charge without overflow");
        var target=r.getBlockPos().south(12);
        var block=MachineRegistry.basicMachines().get(0).get();h.getLevel().setBlock(target,block.defaultBlockState(),3);
        var receiver=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(target);
        receiver.setSpec(BasicMachineSpec.builder("fusion_receiver",block.basicSpec().material()).machineType(block.basicSpec().machineName()).energy(GregTechTags.Energy.EU,8192).recipes(block.basicSpec().recipeMap()).build());
        tick(r);h.assertTrue(receiver.getEnergyTick()==8192,"cardinal outlet emits exactly one EU packet");
        tick(r);tick(r);
        h.assertTrue(r.inventory().getStackInSlot(1).getCount()==1&&r.chargeRemaining()==0&&r.inventory().getStackInSlot(0).isEmpty(),"identical next batch retains startup charge");
        for(int i=0;i<6;i++)tick(r);
        h.assertTrue(r.inventory().getStackInSlot(1).getCount()==2,"only two batches complete without duplicate outputs");h.succeed();
    }
    @GameTest(template="test_fusion_empty",timeoutTicks=200)
    public static void processorPermutationBrokenPortsAndRotation(GameTestHelper h) {
        var r=build(h,Direction.EAST);var lu=port(r,Role.ENERGY_INPUT);
        var cpus=FusionStructure.CELLS.stream().filter(c->c.part()==18200||c.part()==18201).toList();
        var a=cpus.get(0).at(r.getBlockPos(),Direction.EAST);var b=cpus.get(3).at(r.getBlockPos(),Direction.EAST);
        var first=h.getLevel().getBlockState(a);var second=h.getLevel().getBlockState(b);
        h.getLevel().setBlock(a,second,3);h.getLevel().setBlock(b,first,3);
        h.assertTrue(r.isStructureOk(),"processor counts matter, not prescribed positions");
        fixture(r,"fusion_break_test");r.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));tick(r);
        h.getLevel().setBlock(a,Blocks.AIR.defaultBlockState(),3);
        h.assertTrue(lu.doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,1,true)==0&&!r.isStructureOk(),"cached ports close immediately on broken structure");
        h.getLevel().setBlock(a,second,3);h.assertTrue(r.isStructureOk(),"repair rebinds ring");
        var eu=port(r,Role.ENERGY_OUTPUT);h.assertTrue(eu.isEnergyEmittingTo(GregTechTags.Energy.EU,Direction.EAST,false)&&!eu.isEnergyAcceptingFrom(GregTechTags.Energy.LU,Direction.EAST,false),"EU outlets cannot accept startup energy");
        h.succeed();
    }
    @GameTest(template="test_fusion_empty",timeoutTicks=200)
    public static void reservedProductsSurviveSaveAndBlockedOutputs(GameTestHelper h) {
        var r=build(h,Direction.NORTH);fixture(r,"fusion_save_test");
        r.inventory().setStackInSlot(0,new ItemStack(Items.APPLE));tick(r);
        port(r,Role.ENERGY_INPUT).doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,1,true);
        var saved=r.saveWithoutMetadata();r.load(saved);
        h.assertTrue(r.chargeRemaining()==8193&&r.inventory().getStackInSlot(0).isEmpty(),"reload retains reserved inputs and remaining LU");
        port(r,Role.ENERGY_INPUT).doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,2,true);
        r.inventory().setStackInSlot(1,new ItemStack(Items.STONE,64));for(int i=0;i<6;i++)tick(r);
        h.assertTrue(r.getProgressPercent()==100&&!r.isRunning(),"blocked finished job stops generating and retains product");
        saved=r.saveWithoutMetadata();r.load(saved);r.inventory().setStackInSlot(1,ItemStack.EMPTY);
        tick(r);tick(r);
        h.assertTrue(r.inventory().getStackInSlot(1).is(Items.DIAMOND)&&r.inventory().getStackInSlot(1).getCount()==1,"saved blocked output resumes exactly once");h.succeed();
    }
    @GameTest(template="test_fusion_empty",timeoutTicks=200)
    public static void originalDeuteriumReactionUsesRealFluidPorts(GameTestHelper h) {
        var r=build(h,Direction.NORTH);
        var recipe=MachineRecipeMaps.Fusion.mRecipeList.stream().filter(work->work.mDuration==730).findFirst().orElseThrow();
        var fluid=port(r,Role.ITEM_FLUID_IO).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError("fluid port"));
        var input=recipe.mFluidInputs[0].copy();
        h.assertTrue(fluid.fill(input,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)==input.getAmount()&&r.getTanksInput()[0].isEmpty(),"real gas fill simulation is inert");
        h.assertTrue(fluid.fill(input,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==2000,"ring accepts original deuterium gas");
        r.inventory().setStackInSlot(0,recipe.mInputs[0].copy());tick(r);
        h.assertTrue(r.chargeRemaining()==95682560L&&r.inventory().getStackInSlot(0).getCount()==1,"original startup and reusable selector retained");
        var saved=r.saveWithoutMetadata();
        var restored=new FusionReactorControllerBlockEntity(r.getBlockPos(),r.getBlockState());restored.load(saved);
        h.assertTrue(restored.chargeRemaining()==r.chargeRemaining()&&restored.getTanksInput()[0].isEmpty(),"new block entity loads reserved fluid job without consuming twice");
        port(r,Role.ENERGY_INPUT).doEnergyInjection(GregTechTags.Energy.LU,Direction.UP,8192,11680,true);
        for(int i=0;i<730;i++)tick(r);
        for(var expected:recipe.mFluidOutputs) {
            var drained=fluid.drain(expected,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(drained.isFluidEqual(expected)&&drained.getAmount()==expected.getAmount(),"original isotope output available from ring");
        }
        h.assertTrue(fluid.drain(1000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty(),"no additional product after draining");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalFusionRecipesAndCraftingResolve(GameTestHelper h) {
        h.assertTrue(MachineRecipeMaps.Fusion.mRecipeList.size()==18,"all 18 original fusion reactions resolve real materials and phases");
        var dt=MachineRecipeMaps.Fusion.mRecipeList.stream().filter(r->r.mDuration==1760).findFirst().orElseThrow();
        h.assertTrue(dt.mSpecialValue==230686720&&dt.mEUt==-8192&&dt.mFluidInputs.length==2&&dt.mFluidOutputs[0].getAmount()==1000,"original D/T fusion balance and startup retained");
        for(String id:java.util.List.of("fusion_reactor_main","versatile_processor_unit","logic_processor_unit","control_processor_unit","large_iridium_coil","fusion_ventilation_unit"))
            h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","fusion/"+id)).isPresent(),"fusion crafting resolves: "+id);
        h.succeed();
    }
}
