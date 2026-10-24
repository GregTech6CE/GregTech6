package gregtech.client;
import gregtech.platform.neoforge.machine.BasicMachineRegistries;
import gregtech.client.gui.BasicMachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BasicMachineClientSetup {
 private BasicMachineClientSetup(){}
 @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(BasicMachineRegistries.MENU.get(),BasicMachineScreen::new);event.register(BasicMachineRegistries.BEHAVIOR_MENU.get(),(net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<gregtech.client.gui.BehaviorMachineMenu,gregtech.client.gui.BehaviorMachineHostScreen>)(menu,inventory,title)->{var machine=menu.machine();net.minecraft.client.gui.screens.Screen custom=BehaviorMachineClients.screen(menu,machine.addonId(),machine.machineType(),inventory,title);return custom!=null?new gregtech.client.gui.BehaviorMachineCustomScreen(menu,inventory,title,custom):new gregtech.client.gui.BehaviorMachineScreen(menu,inventory,title);});event.register(BasicMachineRegistries.PART_MENU.get(),gregtech.client.gui.PartScreen::new);}
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){for(var holder:BasicMachineRegistries.all()){var block=holder.get();event.register((state,level,pos,index)->index==0?block.basicSpec().material().getColor():0xFFFFFF,block);}}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){for(var holder:BasicMachineRegistries.all()){var block=holder.get();event.register(ItemColorARGB.opaque((stack,index)->index==0?block.basicSpec().material().getColor():0xFFFFFF),block.asItem());}}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){for(var type:BasicMachineRegistries.types())event.registerBlockEntityRenderer(type.get(),MachineCoverRenderer::new);event.registerBlockEntityRenderer(gregtech.registry.GTBlockEntities.COKE_OVEN.get(),MachineCoverRenderer::new);}
}
