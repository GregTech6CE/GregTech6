package com.gregtech.gregtech.client;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class EmiWidgetInputEvents {
    private static boolean loaded() { return net.minecraftforge.fml.ModList.get().isLoaded("emi"); }
    @SubscribeEvent public static void render(ScreenEvent.Render.Pre e) { if(loaded()) com.gregtech.gregtech.emi.EmiWidgetInput.beginFrame(e.getScreen()); }
    @SubscribeEvent public static void pressed(ScreenEvent.MouseButtonPressed.Pre e) { if(loaded()&&com.gregtech.gregtech.emi.EmiWidgetInput.pressed(e.getScreen(),e.getMouseX(),e.getMouseY(),e.getButton()))e.setCanceled(true); }
    @SubscribeEvent public static void released(ScreenEvent.MouseButtonReleased.Pre e) { if(loaded()&&com.gregtech.gregtech.emi.EmiWidgetInput.released(e.getScreen(),e.getButton()))e.setCanceled(true); }
    @SubscribeEvent public static void dragged(ScreenEvent.MouseDragged.Pre e) { if(loaded()&&com.gregtech.gregtech.emi.EmiWidgetInput.dragged(e.getScreen(),e.getMouseButton(),e.getDragX(),e.getDragY()))e.setCanceled(true); }
    @SubscribeEvent public static void scrolled(ScreenEvent.MouseScrolled.Pre e) { if(loaded()&&com.gregtech.gregtech.emi.EmiWidgetInput.scrolled(e.getScreen(),e.getMouseX(),e.getMouseY(),e.getScrollDelta()))e.setCanceled(true); }
}
