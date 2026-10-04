package com.gregtech.gregtech.client;

import com.gregtech.gregtech.registry.GTFluidSprings;
import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FluidSpringClient {
    private FluidSpringClient() {}
    public static Fluid fluid(ItemStack stack) {
        if(stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof com.gregtech.gregtech.block.FluidSpringBlock block && block.spring()!=null)
            return BuiltInRegistries.FLUID.get(ResourceLocation.parse(block.spring().fluidId()));
        var packed=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var data=packed==null?null:packed.copyTag();
        var id=data==null?null:ResourceLocation.tryParse(data.getString("spring"));
        return id!=null&&BuiltInRegistries.FLUID.containsKey(id)?BuiltInRegistries.FLUID.get(id):Fluids.WATER;
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        for(var block:GTFluidSprings.allBlocks()) {
            var fluid=((com.gregtech.gregtech.block.FluidSpringBlock)block).spring();
            var output=fluid==null?Fluids.WATER:BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluid.fluidId()));
            var id=BlockModelShaper.stateToModelLocation(block.defaultBlockState());
            event.getModels().computeIfPresent(id,(key,model)->new FluidSpringBakedModel(model,output));
            var item=ModelResourceLocation.inventory(BuiltInRegistries.BLOCK.getKey(block));
            event.getModels().computeIfPresent(item,(key,model)->new FluidSpringBakedModel(model,output));
        }
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state,level,pos,layer)-> {
            var definition=((com.gregtech.gregtech.block.FluidSpringBlock)state.getBlock()).spring();
            Fluid output=definition==null?Fluids.WATER:BuiltInRegistries.FLUID.get(ResourceLocation.parse(definition.fluidId()));
            if(definition==null && level!=null && pos!=null && level.getBlockEntity(pos) instanceof FluidSpringBlockEntity be && be.fluid()!=null)output=be.fluid();
            return layer==0?IClientFluidTypeExtensions.of(output).getTintColor():0xFFFFFFFF;
        },GTFluidSprings.allBlocks());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack,layer)->layer==0?IClientFluidTypeExtensions.of(fluid(stack)).getTintColor():0xFFFFFFFF,
                java.util.Arrays.stream(GTFluidSprings.allBlocks()).map(net.minecraft.world.level.block.Block::asItem).toArray(net.minecraft.world.item.Item[]::new));
    }
}
