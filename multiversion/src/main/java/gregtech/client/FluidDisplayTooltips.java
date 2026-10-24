package gregtech.client;

import gregtech.content.fluid.OriginalFluidDisplayRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
//?}
public final class FluidDisplayTooltips {
    private FluidDisplayTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() != null && event.getEntity().getAbilities().instabuild
//? if >=1.21.1 {
                /*&& event.getItemStack().getItem() instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem) {
*///?} else {
                && event.getItemStack().getItem() instanceof gregtech.item.FluidItem) {
//?}
            event.getToolTip().add(Component.translatable(OriginalFluidDisplayRules.PREFIX + "creative_fill")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
