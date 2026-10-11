package com.gregtech.gregtech.client;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
public final class CommonBlockTooltipHandler {
    private CommonBlockTooltipHandler() {}

    // MaterialTooltipHandler runs at NORMAL; finalize common rows after material fire hints.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CommonBlockTooltips.append(stack, event.getToolTip());
    }
}
