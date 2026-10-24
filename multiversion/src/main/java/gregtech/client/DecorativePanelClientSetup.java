package gregtech.client;
import gregtech.content.item.PanelItemView;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid=gregtech.internal.mod.GregTechIdentity.MOD_ID,value=Dist.CLIENT)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
@Mod.EventBusSubscriber(modid=gregtech.internal.mod.GregTechIdentity.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
//?}
public final class DecorativePanelClientSetup {
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        for(var item:BuiltInRegistries.ITEM)if(item instanceof PanelItemView)
//? if >=1.21.1 {
            /*event.register(ItemColorARGB.opaque((stack,layer)->layer==0?((PanelItemView)stack.getItem()).panelSpec().tint():0xFFFFFF),item);
*///?} else {
            event.register((stack,layer)->layer==0?((PanelItemView)stack.getItem()).panelSpec().tint():0xFFFFFF,item);
//?}
    }
}
