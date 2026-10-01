package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GTToolClient {
    private GTToolClient() {}

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (!GTToolHelper.isTool(stack)) {
                return 0xFFFFFF;
            }
            int headColor = 0xFF000000 | (GTToolHelper.getHead(stack).getColor() & 0xFFFFFF);
            if (tintIndex == ToolIconSets.OVERLAY_TINT) {
                return 0xFFFFFF;
            }
            GTToolType type = GTToolHelper.getType(stack);
            if (type == GTToolType.GEM_PICK) {
                if (tintIndex == 0) {
                    return 0xFF000000 | (GTToolHelper.getHandle(stack).getColor() & 0xFFFFFF);
                }
                if (tintIndex == 1) {
                    return headColor;
                }
                return 0xFFFFFF;
            }
            if (type.isHeadless()) {
                return tintIndex == 0 ? headColor : 0xFFFFFF;
            }
            if (tintIndex == 0) {
                return 0xFF000000 | (GTToolHelper.getHandle(stack).getColor() & 0xFFFFFF);
            }
            if (tintIndex == 1) {
                return headColor;
            }
            return 0xFFFFFF;
        }, GTToolItems.all().values().stream().map(holder -> holder.get()).toArray(net.minecraft.world.item.Item[]::new));
    }
}
