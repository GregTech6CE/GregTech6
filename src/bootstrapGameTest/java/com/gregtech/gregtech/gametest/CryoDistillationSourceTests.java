package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.CryoDistillationControllerBlock;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Two finite placed-world fixtures with real imported air recipes and explicit native ticks. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_cryo_tower")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class CryoDistillationSourceTests {
    private static BlockPos origin(GameTestHelper h) {return h.absolutePos(new BlockPos(2,0,2)).atY(260);}
    private static CryoDistillationControllerBlockEntity controller(GameTestHelper h,BlockPos pos) {
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:cryo_distillation_main"));
        h.assertTrue(block instanceof CryoDistillationControllerBlock,"registered cold controller");
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        return (CryoDistillationControllerBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static TankBlockEntity drum(GameTestHelper h,BlockPos pos) {
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:drum_stainless_steel"));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        return (TankBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h,CryoDistillationControllerBlockEntity entity) {
        BasicMachineBlockEntity.serverTick(h.getLevel(),entity.getBlockPos(),entity.getBlockState(),entity);
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void originalColdRecipeAndSpeciesOutlets(GameTestHelper h) {
        var pos=origin(h);var entity=controller(h,pos);
        var cells=LargeMachineLayouts.fromShared(SharedDistillationTowerStructure.CELLS);
        for(var cell:cells)h.getLevel().setBlock(cell.at(pos,Direction.NORTH),cell.block().defaultBlockState(),3);
        h.assertTrue(entity.isStructureOk() && ControllerStructureLayouts.cells(entity.getBlockState().getBlock()).size()==80,
                "original full3x3x8 tower plus nine-transmitter base matches preview");
        h.assertTrue(entity.getEnergySizeInputMin(GregTechTags.Energy.CU,null)==1
                && entity.getEnergySizeInputMax(GregTechTags.Energy.CU,null)==1024,"original cold range");
        h.assertTrue(entity.doEnergyInjection(GregTechTags.Energy.HU,null,64,1,true)==0,"former HU input rejected");
        for(var cell:cells) {
            var part=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(cell.at(pos,Direction.NORTH));
            h.assertTrue(part.isEnergyType(GregTechTags.Energy.CU,Direction.NORTH,false)==(cell.up()<0),"cold only in transmitter base");
            h.assertTrue(!part.isEnergyType(GregTechTags.Energy.HU,Direction.NORTH,false),"base never accepts HU for cold tower");
            var cap=part.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).resolve().orElse(null);
            h.assertTrue(cap!=null && cap.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.SIMULATE)==(cell.up()==0?100:0),
                    "bottom IO, upper output and energy base preserve source roles");
        }
        var receivers=new LinkedHashMap<Integer,TankBlockEntity>();
        for(int height=1;height<=7;height++)receivers.put(height,drum(h,pos.south(3).above(height)));
        h.getLevel().setBlockAndUpdate(pos.south(3),Blocks.CHEST.defaultBlockState());
        var air=GTFluids.stack("Air",200);
        h.assertTrue(!air.isEmpty() && entity.fill(air,IFluidHandler.FluidAction.EXECUTE)==200,"actual registered air input");
        h.assertTrue(entity.recipeMap()==com.gregtech.gregtech.data.MachineRecipeMaps.CryoDistillationTower,"original recipe map wired");
        for(int i=0;i<66;i++) {entity.doEnergyInjection(GregTechTags.Energy.CU,null,64,1,true);tick(h,entity);}
        var expected=Map.of(2,Map.entry("carbondioxide",10),3,Map.entry("argon",1),4,Map.entry("oxygen",50),
                5,Map.entry("nitrogen",143),6,Map.entry("neon",1),7,Map.entry("helium",1));
        for(var entry:expected.entrySet()) {
            var fluid=receivers.get(entry.getKey()).getFluidInTank(0);
            var definition=fluid.isEmpty()?null:GTFluids.entryForFluid(fluid.getFluid());
            h.assertTrue(definition!=null && definition.registryName().equals(entry.getValue().getKey())
                    && fluid.getAmount()==entry.getValue().getValue(),"actual source air product reaches its rear height: "+entry);
        }
        h.assertTrue(receivers.get(1).getFluidInTank(0).isEmpty() && entity.getTanksInput()[0].isEmpty(),
                "no false split air output and exactly200 input consumed");
        entity.inventory().setStackInSlot(2,new ItemStack(Items.STICK,7));
        entity.doEnergyInjection(GregTechTags.Energy.CU,null,64,1,true);tick(h,entity);
        var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(pos.south(3));int sticks=0;
        for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.STICK))sticks+=chest.getItem(i).getCount();
        h.assertTrue(sticks==7 && entity.inventory().getStackInSlot(2).isEmpty(),"bottom rear item outlet three blocks behind main");
        for(var cell:cells)h.getLevel().removeBlock(cell.at(pos,Direction.NORTH),false);
        for(int height=0;height<=7;height++)h.getLevel().removeBlock(pos.south(3).above(height),false);
        h.getLevel().removeBlock(pos,false);h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void legacyFluidsAndUnpoweredStoppedOutput(GameTestHelper h) {
        var pos=origin(h);var entity=controller(h,pos);var old=new CompoundTag();old.putLong("gt.heat",256000);
        var input=new FluidTankGT(64000);input.setFluid(new FluidStack(Fluids.WATER,75));
        old.put("gt.input",savedTank(h,input));
        var names=List.of("He","Ne","Ar");long[] amounts={13,29,31};
        for(int i=0;i<3;i++){var tank=new FluidTankGT(32000);tank.setFluid(com.gregtech.gregtech.loaders.c.GTGeneratedChem.materialFluid(names.get(i),(int)amounts[i]));old.put("gt.output"+i,savedTank(h,tank));}
        entity.load(old);
        h.assertTrue(entity.getEnergyTick()==0 && entity.getTanksInput()[0].getAmount()==75,"legacy fluids retained without converting false HU to CU");
        h.assertTrue(entity.getTanksOutput().length==9,"original nine outputs replace old three slots");
        for(int i=0;i<3;i++)h.assertTrue(entity.getTanksOutput()[i].getAmount()==amounts[i]
                && entity.getTanksOutput()[i].baseCapacity()==Long.MAX_VALUE,"legacy amount retained, source unlimited output restored");
        int[] heights={7,6,3};var receivers=new ArrayList<TankBlockEntity>();
        for(int height:heights)receivers.add(drum(h,pos.south(3).above(height)));
        entity.machineControl(null).setEnabled(false);h.assertTrue(!entity.isStructureOk(),"fixture deliberately unformed");
        tick(h,entity);
        for(int i=0;i<3;i++)h.assertTrue(receivers.get(i).getFluidInTank(0).getAmount()==amounts[i]
                && entity.getTanksOutput()[i].isEmpty(),"source outputs saved fluids before work without power or formed structure");
        var saved=entity.saveWithoutMetadata();h.assertTrue(saved.contains("gt.tanks_input0") && !saved.contains("gt.heat"),"native upgraded save uses recipe machine storage and no false heat");
        for(int height:heights)h.getLevel().removeBlock(pos.south(3).above(height),false);
        h.getLevel().removeBlock(pos,false);h.succeed();
    }
    private static CompoundTag savedTank(GameTestHelper h,FluidTankGT tank) {var tag=new CompoundTag();tank.writeToNBT(tag);return tag;}
}
