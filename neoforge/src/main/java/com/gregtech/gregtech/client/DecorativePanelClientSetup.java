package com.gregtech.gregtech.client;
import com.gregtech.gregtech.item.PanelItemView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=Dist.CLIENT)
public final class DecorativePanelClientSetup {
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        for(var item:BuiltInRegistries.ITEM)if(item instanceof PanelItemView)
            event.register(ItemColorARGB.opaque((stack,layer)->layer==0?((PanelItemView)stack.getItem()).panelSpec().tint():0xFFFFFF),item);
    }
}
