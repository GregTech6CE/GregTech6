package gregtech.event;

import gregtech.item.behavior.AxeColumnHarvest;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
//?}
public final class AxeColumnEvents {
    private AxeColumnEvents() {}
    @SubscribeEvent
    public static void speed(PlayerEvent.BreakSpeed event) {
        event.getPosition().ifPresent(pos -> event.setNewSpeed(AxeColumnHarvest.speed(event.getEntity(),
                event.getEntity().getMainHandItem(), event.getState(), pos, event.getNewSpeed())));
    }
}
