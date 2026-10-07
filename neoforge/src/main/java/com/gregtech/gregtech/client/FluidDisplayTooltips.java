package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.fluid.OriginalFluidDisplayRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "gregtech", value = Dist.CLIENT)
public final class FluidDisplayTooltips {
    private FluidDisplayTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() != null && event.getEntity().getAbilities().instabuild
                && event.getItemStack().getItem() instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem) {
            event.getToolTip().add(Component.translatable(OriginalFluidDisplayRules.PREFIX + "creative_fill")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
