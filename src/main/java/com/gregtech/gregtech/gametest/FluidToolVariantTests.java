package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.fluid.*;
import com.gregtech.gregtech.block.tool.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class FluidToolVariantTests {
    @GameTest(template="test_empty") public static void finalizedFluidDeclarations(GameTestHelper h){
        var declared=com.gregtech.gregtech.data.RegisteredFluids.Steam;
        h.assertTrue(com.gregtech.gregtech.data.RegisteredFluids.get("Steam").equals(declared),"registry must retain chained immutable declaration properties");
        var steam=GTFluids.still("Steam").get();
        h.assertTrue(GTFluids.isGas(new FluidStack(steam,1))&&steam.getFluidType().getTemperature()==373&&steam.getFluidType().getDensity()==-1000,"actual Forge steam retains gas phase, temperature and density");
        for(var field:com.gregtech.gregtech.data.RegisteredFluids.class.getFields()){
            if(field.getType()!=com.gregtech.gregtech.data.RegisteredFluids.FluidEntry.class)continue;
            var registered=com.gregtech.gregtech.data.RegisteredFluids.get(field.getName());
            if(registered!=null)try{h.assertTrue(registered.equals(field.get(null)),"final declaration retained: "+field.getName());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void containerMaterialLimits(GameTestHelper h){
        int pots=0,cylinders=0;
        for(var spec:PortableFluidContainerSpec.values()){
            if(spec.shapeId().equals("measuring_pot"))pots++;
            if(spec.shapeId().equals("barometer_gas_cylinder"))cylinders++;
            boolean gas=spec.shapeId().equals("barometer_gas_cylinder");
            h.assertTrue(spec.material().isValid()&&spec.maxTemperature()>0,"valid material and temperature "+spec.id());
            h.assertTrue(spec.accepts(spec.maxTemperature(),gas,false,false,false)&&(spec.maxTemperature()==Integer.MAX_VALUE||!spec.accepts(spec.maxTemperature()+1,gas,false,false,false)),"original inclusive temperature boundary "+spec.id());
            var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_"+spec.id()));
            h.assertTrue(block instanceof PortableContainerBlock,"registered placeable vessel "+spec.id());
        }
        h.assertTrue(pots==4&&cylinders==4,"four source variants for each family");
        var acid=new FluidStack(GTFluids.still("GenLiquid_SulfuricAcid").get(),100);
        var ceramic=(com.gregtech.gregtech.item.PortableFluidContainerItem)net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_measuring_pot"));
        var stainless=(com.gregtech.gregtech.item.PortableFluidContainerItem)net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_measuring_pot_stainless_steel"));
        h.assertTrue(!ceramic.accepts(acid)&&stainless.accepts(acid),"actual acid fluid obeys material resistance in item handler");

        h.assertTrue(!PortableFluidContainerSpec.BAROMETER_GAS_CYLINDER.accepts(300,false,false,false,false),"gas cylinder rejects liquids");
        h.assertTrue(PortableFluidContainerSpec.MEASURING_POT_STAINLESS.accepts(300,false,true,false,false)&&!PortableFluidContainerSpec.MEASURING_POT.accepts(300,false,true,false,false),"material-specific acid resistance");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void attachmentPhaseAndFlowRules(GameTestHelper h){
        var tank=new FluidTankGT(8000);var water=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
        for(var spec:com.gregtech.gregtech.content.tool.FluidAttachmentSpec.all()){
            var raw=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech",spec.id()));
            h.assertTrue(raw instanceof FluidAttachmentBlock,"registered material attachment "+spec.id());var b=(FluidAttachmentBlock)raw;var port=b.access(tank);
            tank.setFluid(water);boolean nozzle=spec.shape().equals("cap_nozzle"),funnel=spec.shape().equals("fluid_funnel");
            h.assertTrue(port.fill(water,FluidAction.SIMULATE)==(funnel?1000:0)&&tank.getAmount()==1000,"fill direction and simulation "+spec.id());
            h.assertTrue(port.drain(100,FluidAction.SIMULATE).getAmount()==(!nozzle&&!funnel?100:0),"liquid drain rule "+spec.id());
            tank.setFluid(new FluidStack(GTFluids.still("Steam").get(),1000));
            h.assertTrue(port.drain(100,FluidAction.EXECUTE).getAmount()==(nozzle?100:0),"gas drain rule "+spec.id());
        }
        var preferred=ResourceLocation.fromNamespaceAndPath("gregtech","block/fluids/arbitrary");
        h.assertTrue(FluidTexturePolicy.select(preferred,FluidTexturePolicy.Kind.GAS,x->true).equals(preferred),"existing dedicated gas texture wins; only missing textures use the solid standard");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void fluidToolRecipesAndChestSlots(GameTestHelper h){
        long count=h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals("gregtech")&&r.getId().getPath().startsWith("fluid_tools/")).count();
        h.assertTrue(count==38,"23 material tool recipes plus five forming/firing/reclaiming ceramic chains decode and load");
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var menu=new com.gregtech.gregtech.client.gui.HopperContainerMenu(1,player.getInventory(),54);
        h.assertTrue(menu.getSlot(0).x==8&&menu.getSlot(0).y==18&&menu.getSlot(53).y==108,"chest six rows align to original PNG");
        h.assertTrue(menu.getSlot(54).y==140&&menu.getSlot(81).y==198,"player and hotbar align to original PNG");h.succeed();
    }
}
