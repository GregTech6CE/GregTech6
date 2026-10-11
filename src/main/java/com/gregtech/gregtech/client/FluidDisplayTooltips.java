package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.fluid.OriginalFluidDisplayRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "gregtech", value = Dist.CLIENT)
public final class FluidDisplayTooltips {
    private FluidDisplayTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() != null && event.getEntity().getAbilities().instabuild
                && event.getItemStack().getItem() instanceof com.gregtech.gregtech.item.FluidItem) {
            event.getToolTip().add(Component.translatable(OriginalFluidDisplayRules.PREFIX + "creative_fill")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
