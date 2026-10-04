package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.GTConstructionBlocks;
import com.gregtech.gregtech.block.misc.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OriginConstructionClient {
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        var glow=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
        event.register((state,level,pos,index)->index==0?ConcreteBlock.tint(state.getValue(ColoredConstructionBlock.COLOR)):0xFFFFFF,glow);
        event.register((state, level, pos, index) -> index == 0 ? ConcreteBlock.tint(state.getValue(ColoredConstructionBlock.COLOR)) : 0xFFFFFF,
                GTConstructionBlocks.ASPHALT.get(), GTConstructionBlocks.CFOAM.get(), GTConstructionBlocks.CFOAM_FRESH.get(), GTConstructionBlocks.CFOAM_SLAB.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        var glow=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
        event.register(ItemColorARGB.opaque((stack,index)->index==0?ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)):0xFFFFFF),glow);
        event.register(ItemColorARGB.opaque((stack, index) -> index == 0 ? ConcreteBlock.tint(ColoredConstructionBlock.itemColor(stack)) : 0xFFFFFF), GTConstructionBlocks.ASPHALT.get().asItem(), GTConstructionBlocks.CFOAM.get().asItem(), GTConstructionBlocks.CFOAM_FRESH.get().asItem(), GTConstructionBlocks.CFOAM_SLAB.get().asItem());
    }
}
