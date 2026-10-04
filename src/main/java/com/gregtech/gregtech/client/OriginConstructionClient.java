package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.block.misc.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OriginConstructionClient {
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab")),net.minecraft.client.renderer.RenderType.translucent()));
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        var glow=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
        event.register((state,level,pos,index)->index==0?ConcreteBlock.tint(state.getValue(ColoredConstructionBlock.COLOR)):0xFFFFFF,glow);
        event.register((state, level, pos, index) -> index == 0 ? ConcreteBlock.tint(state.getValue(ColoredConstructionBlock.COLOR)) : 0xFFFFFF,
                GTDecorBlocks.ASPHALT.get(), GTDecorBlocks.CFOAM.get(), GTDecorBlocks.CFOAM_FRESH.get(), GTDecorBlocks.CFOAM_SLAB.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        var glow=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
        event.register((stack,index)->index==0?ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)):0xFFFFFF,glow);
        event.register((stack, index) -> index == 0 ? ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)) : 0xFFFFFF, GTDecorBlocks.ASPHALT.get().asItem(), GTDecorBlocks.CFOAM.get().asItem(), GTDecorBlocks.CFOAM_FRESH.get().asItem(), GTDecorBlocks.CFOAM_SLAB.get().asItem());
    }
}
