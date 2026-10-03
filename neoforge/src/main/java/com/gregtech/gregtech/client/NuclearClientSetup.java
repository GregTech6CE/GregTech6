package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class NuclearClientSetup{
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(GTBlockEntities.REACTOR_CORE.get(),ReactorRodRenderer::new);event.registerBlockEntityRenderer(GTBlockEntities.REACTOR_CORE_2X2.get(),ReactorRodRenderer::new);}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){
  for(var holder:GTFuelRods.ALL)event.register(ItemColorARGB.opaque((stack,index)->index==0?((com.gregtech.gregtech.item.FuelRodItem)stack.getItem()).tintRgb():0xFFFFFF),holder.get());
  for(var block:reactorBlocks())event.register(ItemColorARGB.opaque((stack,index)->reactorColor(index)),block.asItem());
 }
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){
  for(var holder:GTFuelRods.BLOCKS.values())event.register((state,level,pos,index)->index==0?((com.gregtech.gregtech.item.FuelRodItem)state.getBlock().asItem()).tintRgb():0xFFFFFF,holder.get());
  for(var block:reactorBlocks())event.register((state,level,pos,index)->reactorColor(index),block);
 }
 private static int reactorColor(int index){return index==0?com.gregtech.gregtech.content.material.Materials.Lead.getColor():0xFFFFFF;}
 private static net.minecraft.world.level.block.Block[] reactorBlocks(){return new net.minecraft.world.level.block.Block[]{GTEnergyNodes.REACTOR_CORE_BLOCK.get(),GTEnergyNodes.REACTOR_CORE_2X2.get(),GTEnergyNodes.REACTOR_CASING.get()};}
}
