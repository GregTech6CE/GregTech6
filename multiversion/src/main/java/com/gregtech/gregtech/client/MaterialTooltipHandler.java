package com.gregtech.gregtech.client;


//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.block.MaterialBlockItem;
import net.minecraft.world.item.ItemStack;
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
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, value = Dist.CLIENT)
//?}
public final class MaterialTooltipHandler {
    private MaterialTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        var form = com.gregtech.gregtech.api.material.MaterialEquivalence.form(stack);
        if (form != null && com.gregtech.gregtech.content.recipe.MaterialWashingRules.row(form.prefix()) != null)
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("gt.behaviour.washing"));
        if (stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem
                || stack.getItem() instanceof MaterialBlockItem blockItem && blockItem.material() != null) {
            return;
        }
        ItemMaterialRegistry.get(stack).ifPresent(data ->
                MaterialTooltips.appendForeign(stack, data, event.getToolTip(), event.getFlags()));
    }
}
