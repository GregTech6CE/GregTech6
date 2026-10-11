package com.gregtech.gregtech.client;

import net.minecraft.world.item.ItemStack;
import com.gregtech.gregtech.GregTech;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
public final class CommonBlockTooltipHandler {
    private CommonBlockTooltipHandler() {}

    // MaterialTooltipHandler runs at NORMAL; finalize common rows after material fire hints.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CommonBlockTooltips.append(stack, event.getToolTip());
    }
}
