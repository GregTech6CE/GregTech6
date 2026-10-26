package gregtech.compat.emi;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid=gregtech.engine.mod.GregTechIdentity.MOD_ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
*///?} else {
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=gregtech.engine.mod.GregTechIdentity.MOD_ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
//?}
public final class EmiWidgetInputEvents {
//? if >=1.21.1 {
    /*private static boolean loaded() { return net.neoforged.fml.ModList.get().isLoaded("emi"); }
*///?} else {
    private static boolean loaded() { return net.minecraftforge.fml.ModList.get().isLoaded("emi"); }
//?}
    @SubscribeEvent public static void render(ScreenEvent.Render.Pre e) { if(loaded()) gregtech.compat.emi.EmiWidgetInput.beginFrame(e.getScreen()); }
    @SubscribeEvent public static void pressed(ScreenEvent.MouseButtonPressed.Pre e) { if(loaded()&&gregtech.compat.emi.EmiWidgetInput.pressed(e.getScreen(),e.getMouseX(),e.getMouseY(),e.getButton()))e.setCanceled(true); }
    @SubscribeEvent public static void released(ScreenEvent.MouseButtonReleased.Pre e) { if(loaded()&&gregtech.compat.emi.EmiWidgetInput.released(e.getScreen(),e.getButton()))e.setCanceled(true); }
    @SubscribeEvent public static void dragged(ScreenEvent.MouseDragged.Pre e) { if(loaded()&&gregtech.compat.emi.EmiWidgetInput.dragged(e.getScreen(),e.getMouseButton(),e.getDragX(),e.getDragY()))e.setCanceled(true); }
//? if >=1.21.1 {
    /*@SubscribeEvent public static void scrolled(ScreenEvent.MouseScrolled.Pre e) { if(loaded()&&gregtech.compat.emi.EmiWidgetInput.scrolled(e.getScreen(),e.getMouseX(),e.getMouseY(),e.getScrollDeltaY()))e.setCanceled(true); }
*///?} else {
    @SubscribeEvent public static void scrolled(ScreenEvent.MouseScrolled.Pre e) { if(loaded()&&gregtech.compat.emi.EmiWidgetInput.scrolled(e.getScreen(),e.getMouseX(),e.getMouseY(),e.getScrollDelta()))e.setCanceled(true); }
//?}
}
