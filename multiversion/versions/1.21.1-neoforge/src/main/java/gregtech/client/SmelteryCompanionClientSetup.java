package gregtech.client;
import gregtech.platform.neoforge.smeltery.SmelteryRegistries;import gregtech.registry.GTBlockEntities;
import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.api.distmarker.Dist;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SmelteryCompanionClientSetup{
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(SmelteryRegistries.MOLD.get(),context->new SmelteryHullRenderer(context));event.registerBlockEntityRenderer(GTBlockEntities.MOLD_BASIN.get(),context->new SmelteryHullRenderer(context));}
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){for(var list:java.util.List.of(SmelteryRegistries.molds(),SmelteryRegistries.basins(),SmelteryRegistries.faucets(),SmelteryRegistries.crossings()))for(var holder:list)event.register((state,level,pos,index)->index==0?spec(state.getBlock()).tintRgb():0xFFFFFF,holder.get());}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){for(var list:java.util.List.of(SmelteryRegistries.molds(),SmelteryRegistries.basins(),SmelteryRegistries.faucets(),SmelteryRegistries.crossings()))for(var holder:list)event.register(ItemColorARGB.opaque((stack,index)->index==0?spec(holder.get()).tintRgb():0xFFFFFF),holder.get().asItem());}
 private static gregtech.internal.machine.CrucibleSpec spec(net.minecraft.world.level.block.Block b){if(b instanceof gregtech.block.machine.MoldBlock x)return x.spec();if(b instanceof gregtech.block.machine.MoldBasinBlock x)return x.spec();if(b instanceof gregtech.block.machine.CrucibleFaucetBlock x)return x.spec();return ((gregtech.block.machine.CrucibleCrossingBlock)b).spec();}
}
