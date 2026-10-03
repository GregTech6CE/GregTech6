package com.gregtech.gregtech.client;

import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FluidSpringClient {
    private FluidSpringClient() {}
    public static Fluid fluid(ItemStack stack) {
        var data=stack.getTagElement("BlockEntityTag");
        var id=data==null?null:ResourceLocation.tryParse(data.getString("spring"));
        return id!=null&&BuiltInRegistries.FLUID.containsKey(id)?BuiltInRegistries.FLUID.get(id):Fluids.WATER;
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        var block=GTDecorBlocks.FLUID_SPRING.get();
        var id=BlockModelShaper.stateToModelLocation(block.defaultBlockState());
        event.getModels().computeIfPresent(id,(key,model)->new FluidSpringBakedModel(model));
        var item=new ModelResourceLocation(BuiltInRegistries.BLOCK.getKey(block),"inventory");
        event.getModels().computeIfPresent(item,(key,model)->new FluidSpringBakedModel(model));
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state,level,pos,layer)->layer==0&&level!=null&&pos!=null
                &&level.getBlockEntity(pos) instanceof FluidSpringBlockEntity spring&&spring.fluid()!=null
                ?IClientFluidTypeExtensions.of(spring.fluid()).getTintColor():0xFFFFFFFF,GTDecorBlocks.FLUID_SPRING.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack,layer)->layer==0?IClientFluidTypeExtensions.of(fluid(stack)).getTintColor():0xFFFFFFFF,
                GTDecorBlocks.FLUID_SPRING.get().asItem());
    }
}
