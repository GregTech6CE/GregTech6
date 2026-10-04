package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.tool.CrankBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CrankClientColors {
    private CrankClientColors() {}
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","crank"));
        event.register((state,level,pos,index)->index==0?CrankBlock.tintRgb():0xFFFFFF,block);
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","crank"));
        event.register(ItemColorARGB.opaque((stack,index)->index==0?CrankBlock.tintRgb():0xFFFFFF),item);
    }
}
