package com.gregtech.gregtech.event;

import com.gregtech.gregtech.item.behavior.AxeColumnHarvest;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class AxeColumnEvents {
    private AxeColumnEvents() {}
    @SubscribeEvent
    public static void speed(PlayerEvent.BreakSpeed event) {
        event.getPosition().ifPresent(pos -> event.setNewSpeed(AxeColumnHarvest.speed(event.getEntity(),
                event.getEntity().getMainHandItem(), event.getState(), pos, event.getNewSpeed())));
    }
}
