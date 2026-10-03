package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.GTChemicalBatteries;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ChemicalBatteryClientSetup {
 private ChemicalBatteryClientSetup(){}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(GTChemicalBatteries.CHEMICAL_BATTERY.get(),ChemicalBatteryRenderer::new);}
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){for(var entry:GTChemicalBatteries.allRegistered())event.register(ItemColorARGB.opaque((stack,layer)->layer==0?((ChemicalBatteryItem)stack.getItem()).spec().chemistry().color:0xFFFFFF),entry.get().asItem());}
 @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->GTChemicalBatteries.allRegistered().forEach(entry->net.minecraft.client.renderer.item.ItemProperties.register(entry.get().asItem(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","battery_charge"),(stack,level,entity,seed)->{var item=(ChemicalBatteryItem)stack.getItem();return item.spec().display(item.stored(stack))/(float)item.spec().scale();})));}
}
