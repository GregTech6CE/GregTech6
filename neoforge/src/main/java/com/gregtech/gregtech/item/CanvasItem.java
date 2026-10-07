/* GregTech-6 Team's colored canvas item registration, LGPL-3.0-or-later. */
package com.gregtech.gregtech.item;
import com.gregtech.gregtech.content.cover.*;
import net.minecraft.world.item.*;
import java.util.List;
import net.minecraft.network.chat.Component;
public final class CanvasItem extends TechItem {
    private final CanvasRules.Variant variant;
    public CanvasItem(CanvasRules.Variant variant) { super(variant.name(),new Item.Properties().stacksTo(64));this.variant=variant;com.gregtech.gregtech.api.material.ItemMaterialRegistry.register(this,null,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Paper"),com.gregtech.gregtech.api.material.GTValues.U); }
    public CanvasRules.Variant variant() { return variant; }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CanvasData.tooltip(CoverStackData.readOrEmpty(stack),tooltip);super.appendHoverText(stack,context,tooltip,flag);
    }
}
