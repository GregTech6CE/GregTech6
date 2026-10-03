package com.gregtech.gregtech.client;
import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.api.distmarker.Dist;import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;import com.gregtech.gregtech.registry.GTBushes;import com.gregtech.gregtech.content.plant.GTBerryBushes;import com.gregtech.gregtech.blockentity.BushBlockEntity;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SurfaceLifeClientSetup {
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){e.register((state,level,pos,tint)->level!=null&&pos!=null&&level.getBlockEntity(pos) instanceof BushBlockEntity bush?bush.tintColour(tint):tint==0?GTBerryBushes.NO_BERRY_COLOUR:GTBerryBushes.stageColour(null,3),GTBushes.BUSH.get());}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item e){e.register(ItemColorARGB.opaque((stack,tint)->{var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);var type=data==null?null:GTBerryBushes.byId(data.copyTag().getString("berry"));return tint==0?type==null?GTBerryBushes.NO_BERRY_COLOUR:type.bush():GTBerryBushes.stageColour(type,3);}),GTBushes.BUSH.get().asItem());}
}
