package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class GasTurbineGradeTests {
    @GameTest(template="test_blueprint_empty")
    public static void structuralPartsDropThemselves(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        for(var entry:GTMultiblocks.parts()) {
            if(!(entry.get() instanceof com.gregtech.gregtech.block.machine.MultiblockPortBlock))continue;
            h.setBlock(pos,entry.get());
            var absolute=h.absolutePos(pos);var state=h.getLevel().getBlockState(absolute);
            var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),absolute,h.getLevel().getBlockEntity(absolute));
            h.assertTrue(drops.size()==1&&drops.get(0).is(entry.get().asItem())&&drops.get(0).getCount()==1,"structural part drops once: "+entry.getId());
            h.assertTrue(!drops.get(0).hasTag(),"controller binding must not be carried to new structures");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void fourGradesConsumeConserveAndBind(GameTestHelper h) {
        var pos=new BlockPos(6,3,6);
        var recipe=FuelRecipeMaps.Gas.mRecipeList.stream().filter(r->r.mFluidInputs.length==1&&r.mFluidOutputs.length>0&&r.mEUt<0&&r.mFluidInputs[0].getFluid()==GTFluids.still("Methane").get()).findFirst().orElseThrow();
        for(var entry:GasTurbineDefinitions.blocks()) for(var front:Direction.Plane.HORIZONTAL) {
            var grade=GasTurbineDefinitions.grade(entry.get());h.setBlock(pos,Blocks.AIR);h.setBlock(pos,entry.get().defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING,front));
            for(var cell:TurbineStructure.LAYOUT.cells())h.setBlock(cell.at(pos,front),grade.wall());
            var machine=(LargeGasTurbineControllerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
            h.assertTrue(machine.isStructureOk(),"grade forms from its own 35 walls: "+grade.id());
            h.assertTrue(machine.getTankCapacity(0)==grade.inputMaximum()*4,"grade-specific fuel capacity");
            var fuel=recipe.mFluidInputs[0].copy();fuel.setAmount(machine.getTankCapacity(0));
            int filled=machine.fill(fuel,FluidAction.EXECUTE);machine.tick();
            int count=(filled-machine.getFluidInTank(0).getAmount())/recipe.mFluidInputs[0].getAmount();
            h.assertTrue(count>0&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==grade.outputMaximum(),"actual fuel produces grade output");
            for(int i=0;i<recipe.mFluidOutputs.length;i++)h.assertTrue(machine.getFluidInTank(i+1).getAmount()==count*recipe.mFluidOutputs[i].getAmount(),"all exhaust conserved");
            machine.load(machine.saveWithoutMetadata());int remaining=machine.getFluidInTank(0).getAmount();machine.tick();
            h.assertTrue(machine.getFluidInTank(0).getAmount()==remaining,"blocked output survives reload without consuming more fuel");
            var target=pos.relative(front.getOpposite(),4);
            var block=com.gregtech.gregtech.api.machine.MachineRegistry.basicMachines().iterator().next().get();h.setBlock(target,block);
            var receiver=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(target));
            receiver.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("graded_turbine_receiver",block.basicSpec().material())
                    .machineType("test").energy(GregTechTags.Energy.RU,grade.outputMaximum()).recipes(block.basicSpec().recipeMap())
                    .faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
            machine.toggleStopped();machine.tick();
            h.assertTrue(receiver.getEnergyTick()==grade.outputMaximum()&&machine.getEnergyStored(GregTechTags.Energy.RU,null)==0,"grade output reaches rear receiver in each facing");
            h.setBlock(target,Blocks.AIR);
            h.setBlock(pos.relative(front.getClockWise()),Blocks.AIR);h.assertTrue(!machine.isStructureOk(),"broken structure invalidates immediately");
            h.setBlock(pos.relative(front.getClockWise()),grade.wall());h.assertTrue(machine.isStructureOk(),"repair rebinds structure");
            if(grade.wallId()!=18022) {
                h.setBlock(pos.relative(front.getClockWise()),GTMultiblocks.LARGE_GAS_TURBINE_WALL.get());h.assertTrue(!machine.isStructureOk(),"lower-grade casing is rejected");
            }
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void newWallsHaveOriginalWeldingRecipes(GameTestHelper h) {
        var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Adamantium");
        for(var prefix:new com.gregtech.gregtech.data.MaterialPrefix[]{MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.plateDense})
            h.assertTrue(!GTItems.getStack(prefix,material).isEmpty(),"original Adamantium form exists: "+prefix.getName());
        var adamantiumDust=GTItems.getStack(MaterialPrefix.dust,material);
        h.assertTrue(MachineRecipeMaps.Furnace.mRecipeList.stream().noneMatch(r->r.mInputs.length>0&&r.mInputs[0].is(adamantiumDust.getItem())),
                "Adamantium remains crucible-only rather than gaining ordinary furnace processing");
        var dense=GTItems.getStack(MaterialPrefix.plateDense,material);
        h.assertTrue(MachineRecipeMaps.Compressor.mRecipeList.stream().anyMatch(r->r.mOutputs.length==1&&r.mOutputs[0].is(dense.getItem())
                &&r.mInputs.length==1&&r.mInputs[0].getCount()==9&&r.mEUt==16&&r.mDuration==9L*256*(material.getToolQuality()+1)),
                "Adamantium plate-to-dense-plate manufacturing route preserves original hard-workable cost");
        for(int id:new int[]{18023,18025}) {
            var wall=LargeMachineParts.block(id).asItem();
            h.assertTrue(MachineRecipeMaps.Welder.mRecipeList.stream().anyMatch(r->r.mOutputs.length==1&&r.mOutputs[0].is(wall)&&r.mInputs[0].getCount()==4&&r.mDuration==512&&r.mEUt==64),"four dense plates weld into wall "+id+"; existing recipes: "+MachineRecipeMaps.Welder.mRecipeList.stream()
                    .filter(r->r.mOutputs.length>0&&(r.mOutputs[0].is(wall)||java.util.Arrays.stream(r.mInputs).anyMatch(i->i.getItem() instanceof com.gregtech.gregtech.item.MaterialItem m && m.getMaterial().getName().equals(id==18023?"TungstenSteel":"Adamantium"))))
                    .map(r->java.util.Arrays.toString(r.mInputs)+" -> "+java.util.Arrays.toString(r.mOutputs)+" / "+r.mDuration+" / "+r.mEUt).toList());
        }
        h.succeed();
    }
}
