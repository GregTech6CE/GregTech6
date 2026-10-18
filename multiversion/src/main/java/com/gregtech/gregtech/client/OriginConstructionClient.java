package com.gregtech.gregtech.client;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.registry.GTConstructionBlocks;
*///?} else {
import com.gregtech.gregtech.registry.GTDecorBlocks;
//?}
import com.gregtech.gregtech.block.misc.*;
//? if >=1.21.1 {
/*import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class OriginConstructionClient {
//? if >=1.21.1 {
    /*@SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
*///?} else {
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
//?}
        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab")),net.minecraft.client.renderer.RenderType.translucent()));
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        var glow=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
        event.register((state,level,pos,index)->index==0?ConcreteBlock.tint(state.getValue(ColoredConstructionBlock.COLOR)):0xFFFFFF,glow);
        event.register((state, level, pos, index) -> index == 0 ? ConcreteBlock.tint(state.getValue(ColoredConstructionBlock.COLOR)) : 0xFFFFFF,
//? if >=1.21.1 {
                /*GTConstructionBlocks.ASPHALT.get(), GTConstructionBlocks.CFOAM.get(), GTConstructionBlocks.CFOAM_FRESH.get(), GTConstructionBlocks.CFOAM_SLAB.get());
*///?} else {
                GTDecorBlocks.ASPHALT.get(), GTDecorBlocks.CFOAM.get(), GTDecorBlocks.CFOAM_FRESH.get(), GTDecorBlocks.CFOAM_SLAB.get());
//?}
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        var glow=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
//? if >=1.21.1 {
        /*event.register(ItemColorARGB.opaque((stack,index)->index==0?ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)):0xFFFFFF),glow);
        event.register(ItemColorARGB.opaque((stack, index) -> index == 0 ? ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)) : 0xFFFFFF), GTConstructionBlocks.ASPHALT.get().asItem(), GTConstructionBlocks.CFOAM.get().asItem(), GTConstructionBlocks.CFOAM_FRESH.get().asItem(), GTConstructionBlocks.CFOAM_SLAB.get().asItem());
*///?} else {
        event.register((stack,index)->index==0?ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)):0xFFFFFF,glow);
        event.register((stack, index) -> index == 0 ? ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)) : 0xFFFFFF, GTDecorBlocks.ASPHALT.get().asItem(), GTDecorBlocks.CFOAM.get().asItem(), GTDecorBlocks.CFOAM_FRESH.get().asItem(), GTDecorBlocks.CFOAM_SLAB.get().asItem());
//?}
    }
}
