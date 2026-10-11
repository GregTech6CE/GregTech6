package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import com.gregtech.gregtech.content.tool.PortableContainerLimits;
import com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("gregtech")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class PortableContainerLimitTests {
    private static ItemStack vessel(PortableFluidContainerSpec spec){return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","fluid_"+spec.id())));}
    @GameTest(template="test_empty") public static void originalMeasuringClickBands(GameTestHelper h){
        for(var spec:PortableFluidContainerSpec.values())if(PortableContainerLimits.adjustable(spec)){
            var stack=vessel(spec);boolean gas=spec.shapeId().equals("barometer_gas_cylinder");
            int[] expected=gas?new int[]{-500,-100,-50,-10,10,50,100,500}:new int[]{-50,-10,10,50};
            for(int band=0;band<expected.length;band++)for(boolean precise:new boolean[]{false,true}){
                stack.getOrCreateTag().putInt("gt.mode",500);
                int actual=PortableContainerLimits.adjust(stack,spec,(band*2+1)/16.0,precise);
                h.assertTrue(actual==Math.max(1,500+expected[band]/(precise?10:1)),"original side hit band "+spec.id()+" / "+band+" / "+precise);
            }
            stack.getOrCreateTag().putLong("gt.mode",Long.MAX_VALUE);h.assertTrue(PortableContainerLimits.capacity(stack,spec)==spec.capacity(),"oversized saved limit clamps");
            stack.getOrCreateTag().putLong("gt.mode",Long.MIN_VALUE);h.assertTrue(PortableContainerLimits.capacity(stack,spec)==1,"negative saved limit clamps");
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void capacityLimitsPreserveFluidAndPlacement(GameTestHelper h){
        var pos=new BlockPos(1,2,1);
        for(var spec:PortableFluidContainerSpec.values())if(PortableContainerLimits.adjustable(spec)){
            var stack=vessel(spec);var handler=stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
            boolean gas=spec.shapeId().equals("barometer_gas_cylinder");
            var fluid=gas?com.gregtech.gregtech.registry.GTFluids.still("Helium").get():net.minecraft.world.level.material.Fluids.WATER;
            stack.getOrCreateTag().putInt("gt.mode",600);
            h.assertTrue(handler.fill(new FluidStack(fluid,900),FluidAction.SIMULATE)==600&&handler.getFluidInTank(0).isEmpty(),"simulation obeys live limit without mutation");
            h.assertTrue(handler.fill(new FluidStack(fluid,900),FluidAction.EXECUTE)==600,"item fills only to chosen limit");
            PortableContainerLimits.adjust(stack,spec,0,false);
            h.assertTrue(handler.getFluidInTank(0).getAmount()==600&&handler.fill(new FluidStack(fluid,1),FluidAction.EXECUTE)==0,"lowering limit never deletes stored fluid");
            var block=((BlockItem)stack.getItem()).getBlock();h.setBlock(pos,block);
            block.setPlacedBy(h.getLevel(),h.absolutePos(pos),block.defaultBlockState(),h.makeMockPlayer(),stack);
            var be=(PortableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));be.load(be.saveWithoutMetadata());
            var cap=be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
            h.assertTrue(cap.getFluidInTank(0).getAmount()==600&&cap.getTankCapacity(0)==PortableContainerLimits.capacity(stack,spec),"placement and save preserve fluid and independent limit");
            cap.drain(600,FluidAction.EXECUTE);
            h.assertTrue(PortableContainerLimits.capacity(be.contents(),spec)==PortableContainerLimits.capacity(stack,spec),"emptying keeps chosen limit");
            var dropped=net.minecraft.world.level.block.Block.getDrops(be.getBlockState(),h.getLevel(),be.getBlockPos(),be).get(0);
            var itemCap=dropped.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
            h.assertTrue(itemCap.getTankCapacity(0)==cap.getTankCapacity(0),"drop item keeps limit");
            be.setRemoved();h.assertTrue(cap.fill(new FluidStack(fluid,100),FluidAction.EXECUTE)==0&&cap.drain(1,FluidAction.EXECUTE).isEmpty(),"stale automation handler cannot modify removed vessel");
        }
        h.succeed();
    }
}
