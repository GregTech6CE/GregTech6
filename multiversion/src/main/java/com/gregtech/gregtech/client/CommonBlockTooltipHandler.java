package com.gregtech.gregtech.client;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import com.gregtech.gregtech.GregTech;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
//?}
public final class CommonBlockTooltipHandler {
    private CommonBlockTooltipHandler() {}

    // MaterialTooltipHandler runs at NORMAL; finalize common rows after material fire hints.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CommonBlockTooltips.append(stack, event.getToolTip());
    }
}
