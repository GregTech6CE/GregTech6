package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.*;
/** Original facing/lit shell selection and material tint adapted to native baking. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BurningBoxClientSetup {
 private BurningBoxClientSetup(){}
 @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event){for(var list:List.of(SmelteryRegistries.solidBoxes(),SmelteryRegistries.fuelBoxes()))for(var holder:list)event.register((state,level,pos,index)->index==0?holder.get().spec().tintRgb():0xFFFFFF,holder.get());}
 @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item event){for(var list:List.of(SmelteryRegistries.solidBoxes(),SmelteryRegistries.fuelBoxes()))for(var holder:list)event.register(ItemColorARGB.opaque((stack,index)->index==0?holder.get().spec().tintRgb():0xFFFFFF),holder.get().asItem());}
}
