package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.plant.BedrockFlowers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 BlockFlowersA/B tells the player which bedrock deposit each plant indicates. */
@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, value = Dist.CLIENT)
public final class BedrockFlowerTooltips {
    private BedrockFlowerTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !GregTech.MODID.equals(id.getNamespace())) return;
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
