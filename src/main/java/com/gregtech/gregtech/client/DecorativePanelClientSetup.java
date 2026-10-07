package com.gregtech.gregtech.client;
import com.gregtech.gregtech.item.PanelItemView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
@Mod.EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DecorativePanelClientSetup {
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        for(var item:BuiltInRegistries.ITEM)if(item instanceof PanelItemView)
            event.register((stack,layer)->layer==0?((PanelItemView)stack.getItem()).panelSpec().tint():0xFFFFFF,item);
    }
}
