package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;

import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.*;
import java.util.*;
import net.neoforged.neoforge.fluids.FluidStack;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_large_recipe")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class LargeRecipeControllerSourceTests {
    private static List<BasicMachineBlock> machines() {return java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(),false)
            .filter(b->b instanceof BasicMachineBlock).map(b->(BasicMachineBlock)b).filter(b->OriginalLargeRecipeMachineData.handles(b.basicSpec().machineName())).toList();}
    private static BlockPos point(BlockPos main,Direction front,int right,int up,int back) {return main.relative(front.getClockWise(),right).above(up).relative(front.getOpposite(),back);}
    private static Direction face(Direction front,OriginalLargeRecipeMachineData.Face f) {return switch(f) {case UP->Direction.UP;case DOWN->Direction.DOWN;case RIGHT->front.getClockWise();case LEFT->front.getCounterClockWise();case FRONT->front;};}
    private static void tick(GameTestHelper h,BasicMachineBlockEntity m) {BasicMachineBlockEntity.serverTick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);}
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void allOriginalLayoutsFacingsPortsSourcesAndItems(GameTestHelper h) {
        var main=h.absolutePos(new BlockPos(2,0,2)).atY(250);var blocks=machines();h.assertTrue(blocks.size()==12,"twelve registered original families");
        for(var block:blocks)for(var front:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
            h.getLevel().setBlockAndUpdate(main,block.defaultBlockState().setValue(BasicMachineBlock.FACING,front));
            var machine=(LargeRecipeMachineBlockEntity)h.getLevel().getBlockEntity(main);var name=machine.spec().machineName();
            var cells=LargeMachineLayouts.cells(name);
            for(var cell:cells)h.getLevel().setBlockAndUpdate(cell.at(main,front),cell.role()==Role.AIR?Blocks.AIR.defaultBlockState():cell.block().defaultBlockState());
            h.assertTrue(machine.isStructureOk(),"original formed layout "+name+"/"+front);
            var energy=machine.spec().energyTag();
            for(var side:Direction.values())h.assertTrue(machine.isEnergyAcceptingFrom(energy,side,true),"original unrestricted main energy face "+name);
            for(var cell:cells)if(cell.role()!=Role.AIR) {
                var part=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(cell.at(main,front));
                boolean accepts=Set.of(Role.ENERGY_INPUT,Role.ITEM_FLUID_ENERGY_INPUT,Role.ITEM_FLUID_ENERGY).contains(cell.role())&&energy!=GregTechTags.Energy.TU;
                h.assertTrue(part.isEnergyType(energy,front,false)==accepts,"source part energy role "+name+"/"+cell);
            }
            var sources=new ArrayList<EnergyNodeBlockEntity>();
            for(var s:OriginalLargeRecipeMachineData.sources(name)) {
                var pos=point(main,front,s.right(),s.up(),s.back());var ef=face(front,s.outputFace());
                var source=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:"+(energy==GregTechTags.Energy.RU?"electric_motor_lv":"electric_dynamo_lv")));
                h.getLevel().setBlockAndUpdate(pos,source.defaultBlockState().setValue(EnergyNodeBlock.FACING,ef));
                sources.add((EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos));
            }
            tick(h,machine);machine.machineControl(null).setEnabled(false);
            for(var s:sources)h.assertTrue(!s.machineControl(null).enabled(),"original positioned provider stops "+name);
            machine.machineControl(null).setEnabled(true);
            for(var s:sources)h.assertTrue(s.machineControl(null).enabled(),"theoretical emit face restarts provider "+name);
            var out=OriginalLargeRecipeMachineData.output(name,true);var outputPos=point(main,front,out.right(),out.up(),out.back());
            h.getLevel().setBlockAndUpdate(outputPos,Blocks.CHEST.defaultBlockState());
            machine.inventory().setStackInSlot(machine.inputSlots(),new ItemStack(Items.STICK,7));
            if(energy!=GregTechTags.Energy.TU)h.assertTrue(machine.doEnergyInjection(energy,null,512,1,true)==1,"real recommended packet "+name);
            tick(h,machine);
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(outputPos);
            int actual=0;for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).is(Items.STICK))actual+=chest.getItem(slot).getCount();
            h.assertTrue(actual==7&&machine.inventory().getStackInSlot(machine.inputSlots()).isEmpty(),"actual source inventory output target "+name+"/"+front);
            machine.machineControl(null).setEnabled(false);h.getLevel().removeBlock(main,false);
            for(var s:sources)h.assertTrue(s.machineControl(null).enabled(),"actual main break releases provider "+name);
            for(var cell:cells)h.getLevel().removeBlock(cell.at(main,front),false);
            for(var s:sources)h.getLevel().removeBlock(s.getBlockPos(),false);
            h.getLevel().removeBlock(outputPos,false);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void stoppedUnformedSavedFluidsUseOriginalTarget(GameTestHelper h) {
        var main=h.absolutePos(new BlockPos(2,0,2)).atY(250);
        for(var block:machines())for(var front:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
            h.getLevel().setBlockAndUpdate(main,block.defaultBlockState().setValue(BasicMachineBlock.FACING,front));
            var machine=(LargeRecipeMachineBlockEntity)h.getLevel().getBlockEntity(main);var name=machine.spec().machineName();
            if(machine.getTanksOutput().length==0) {h.getLevel().removeBlock(main,false);continue;}
            var tank=machine.getTanksOutput()[0];tank.setFluid(new FluidStack(Fluids.WATER,500));
            var saved=machine.saveWithoutMetadata(h.getLevel().registryAccess());var oldCapacity=saved.getCompound("gt.tanks_output0");oldCapacity.putLong("Capacity",8000);
            machine.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(machine.getTanksOutput()[0].baseCapacity()==Long.MAX_VALUE,"source output limit restored from old port tank "+name);
            var out=OriginalLargeRecipeMachineData.output(name,false);var targetPos=point(main,front,out.right(),out.up(),out.back());
            var drum=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:drum_stainless_steel"));
            h.getLevel().setBlockAndUpdate(targetPos,drum.defaultBlockState());
            machine.machineControl(null).setEnabled(false);h.assertTrue(!machine.isStructureOk(),"fixture deliberately unformed "+name);
            tick(h,machine);
            h.assertTrue(((TankBlockEntity)h.getLevel().getBlockEntity(targetPos)).getFluidInTank(0).getAmount()==500&&machine.getTanksOutput()[0].isEmpty(),"actual saved fluid output before work despite stop/no energy/no structure "+name+"/"+front);
            h.getLevel().removeBlock(main,false);h.getLevel().removeBlock(targetPos,false);
        }
        h.succeed();
    }
}
