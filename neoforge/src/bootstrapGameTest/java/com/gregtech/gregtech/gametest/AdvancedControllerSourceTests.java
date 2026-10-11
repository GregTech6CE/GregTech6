package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_advanced_source")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class AdvancedControllerSourceTests {
    @GameTest(template="test_empty",timeoutTicks=160)
    public static void registered_parameters_and_stopped_output(GameTestHelper h) {
        int count=0;
        BasicMachineBlock matter=null;
        for(var block:BuiltInRegistries.BLOCK) {
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(!id.getNamespace().equals("gregtech"))continue;
            boolean eligible=block instanceof BasicMachineBlock b&&OriginalAdvancedControllerData.handles(b.basicSpec().machineName())||id.getPath().equals("implosion_compressor_main");
            if(!eligible)continue;
            var nativeFactory=(net.minecraft.world.level.block.EntityBlock)block;
            var be=(BasicMachineBlockEntity)nativeFactory.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());var spec=be.spec();
            var expected=OriginalAdvancedControllerData.parameters(spec.machineName(),spec.id());
            h.assertTrue(spec.energyIn()==expected.energyIn()&&spec.energyInMax()==expected.energyInMax()&&spec.parallelLimit()==expected.parallelLimit()&&spec.hardness()==expected.hardness()&&spec.faceConfig().fluidOutputs()==63,"actual native source factory parameters "+id);
            for(var tank:be.getTanksOutput())h.assertTrue(tank.capacity()==Long.MAX_VALUE,"source unlimited output storage "+id);
            if(spec.machineName().equals("largemassfab"))matter=(BasicMachineBlock)block;count++;
        }
        h.assertTrue(count==3&&matter!=null,"actual three source factories");
        var main=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(main,matter.defaultBlockState());
        var machine=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(main);
        var bath=java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false).filter(b->b instanceof BasicMachineBlock m&&m.basicSpec().machineName().equals("bath")&&m.basicSpec().tier()==1).map(b->(BasicMachineBlock)b).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(main.below(),bath.defaultBlockState());var receiver=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(main.below());
        machine.getTanksOutput()[0].setFluid(new FluidStack(Fluids.WATER,500));machine.getTanksOutput()[0].setCapacity(8000);
        var saved=machine.saveWithoutMetadata(h.getLevel().registryAccess());machine.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(machine.getTanksOutput()[0].capacity()==Long.MAX_VALUE,"source maximum output capacity restored after native load");
        machine.machineControl(null).setEnabled(false);BasicMachineBlockEntity.serverTick(h.getLevel(),main,machine.getBlockState(),machine);
        long accepted=java.util.Arrays.stream(receiver.getTanksInput()).mapToLong(t->t.getAmount()).sum();
        h.assertTrue(accepted==500&&machine.getTanksOutput()[0].getAmount()==0,"existing matter fluid outputs to actual block below while stopped/unformed/no energy");
        h.succeed();
    }
}
