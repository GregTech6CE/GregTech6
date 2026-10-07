package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.MultiblockControllerBlock;
import com.gregtech.gregtech.blockentity.machine.DistillationTowerControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.SharedDistillationTowerStructure;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** One finite formed-tower port check pooled with the saved multiblock-tank case. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_process_controllers")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ProcessControllerPortTests {
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void originalTowerAnyFaceDefaultsPreserveStructurePortRoles(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,0,2)).atY(260);
        var block=(MultiblockControllerBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:distillation_tower_main"));
        var cells=LargeMachineLayouts.fromShared(SharedDistillationTowerStructure.CELLS);
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        for(var cell:cells) h.getLevel().setBlock(cell.at(pos,Direction.NORTH),cell.block().defaultBlockState(),3);
        var entity=(DistillationTowerControllerBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity.isStructureOk(),"actual source tower forms from 80 original structural parts");
        for(var side:Direction.values()) {
            var cap=entity.getCapability(ForgeCapabilities.FLUID_HANDLER,side).resolve().orElse(null);
            h.assertTrue(cap!=null && cap.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.SIMULATE)==100,
                    "source any-face main input is available without fabricated left-only input");
            h.assertTrue(entity.getEnergySizeInputMin(GregTechTags.Energy.HU,side)==1
                    && entity.getEnergySizeInputMax(GregTechTags.Energy.HU,side)==1024,"actual explicit1..1024 heat range");
        }
        for(var cell:cells) {
            var part=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(cell.at(pos,Direction.NORTH));
            var fluid=part.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).resolve().orElse(null);
            h.assertTrue(fluid!=null,"live bound part fluid relay");
            h.assertTrue(fluid.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.SIMULATE)==(cell.up()==0?100:0),
                    "original bottom IO / upper output / heat-only base roles remain distinct");
            h.assertTrue(part.isEnergyType(GregTechTags.Energy.HU,Direction.NORTH,false)==(cell.up()<0),
                    "HU belongs only to original heat-transmitter base parts");
            h.assertTrue(!part.isEnergyType(GregTechTags.Energy.EU,Direction.NORTH,false),"tower rejects EU on structural parts");
        }
        h.assertTrue(entity.getFluidInTank(0).isEmpty(),"simulation did not fill any input tank");
        for(var cell:cells) h.getLevel().removeBlock(cell.at(pos,Direction.NORTH),false);
        h.getLevel().removeBlock(pos,false);
        h.succeed();
    }
}
