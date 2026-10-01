package com.gregtech.gregtech.client;


import com.gregtech.gregtech.content.plant.BedrockFlowers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

/** GT6 BlockFlowersA/B tells the player which bedrock deposit each plant indicates. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
public final class BedrockFlowerTooltips {
    private BedrockFlowerTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id == null || !"gregtech".equals(id.getNamespace())) return;
        BedrockFlowers.Flower flower = BedrockFlowers.byId(id.getPath());
        if (flower == null) return;
        event.getToolTip().add(Component.translatable("tooltip.gregtech.bedrock_flower.indicates",
                Component.translatable("tooltip.gregtech.bedrock_flower.deposit." + flower.indicator()))
                .withStyle(ChatFormatting.GRAY));
        if (flower.occursInRealLife())
            event.getToolTip().add(Component.translatable("tooltip.gregtech.bedrock_flower.real_life")
                    .withStyle(ChatFormatting.DARK_GRAY));
    }
}
