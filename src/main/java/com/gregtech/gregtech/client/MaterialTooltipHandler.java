package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GregTech.MOD_ID, value = Dist.CLIENT)
public final class MaterialTooltipHandler {
    private MaterialTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem() instanceof MaterialItem) {
            return;
        }
        ItemMaterialRegistry.get(stack).ifPresent(data ->
                MaterialTooltips.appendForeign(stack, data, event.getToolTip(), event.getFlags()));
    }
}
