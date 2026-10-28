package gregtech.client;

import gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import java.util.*;
import net.minecraft.client.resources.model.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Original facing/lit shell selection and material tint adapted to native baking. */
@EventBusSubscriber(
        modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class BurningBoxClientSetup {
    private BurningBoxClientSetup() {}

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        for (var list : List.of(SmelteryRegistries.solidBoxes(), SmelteryRegistries.fuelBoxes()))
            for (var holder : list)
                event.register(
                        (state, level, pos, index) ->
                                index == 0 ? holder.get().spec().tintRgb() : 0xFFFFFF,
                        holder.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        for (var list : List.of(SmelteryRegistries.solidBoxes(), SmelteryRegistries.fuelBoxes()))
            for (var holder : list)
                event.register(
                        ItemColorARGB.opaque((stack, index) ->
                                index == 0 ? holder.get().spec().tintRgb() : 0xFFFFFF),
                        holder.get().asItem());
    }
}
