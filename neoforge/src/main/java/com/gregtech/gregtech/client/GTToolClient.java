package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTToolItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GTToolClient {
    private GTToolClient() {}

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (!GTToolHelper.isTool(stack)) {
                return 0xFFFFFFFF;
            }
            int headColor = 0xFF000000 | (GTToolHelper.getHead(stack).getColor() & 0xFFFFFF);
            if (tintIndex == ToolIconSets.OVERLAY_TINT) {
                return 0xFFFFFFFF;
            }
            GTToolType type = GTToolHelper.getType(stack);
            if (type == GTToolType.GEM_PICK) {
                if (tintIndex == 0) {
                    return 0xFF000000 | (GTToolHelper.getHandle(stack).getColor() & 0xFFFFFF);
                }
                if (tintIndex == 1) {
                    return headColor;
                }
                return 0xFFFFFFFF;
            }
            if (type.isHeadless()) {
                return tintIndex == 0 ? headColor : 0xFFFFFFFF;
            }
            if (tintIndex == 0) {
                return 0xFF000000 | (GTToolHelper.getHandle(stack).getColor() & 0xFFFFFF);
            }
            if (tintIndex == 1) {
                return headColor;
            }
            return 0xFFFFFFFF;
        }, GTToolItems.all().values().stream().map(holder -> holder.get()).toArray(net.minecraft.world.item.Item[]::new));
    }
}
