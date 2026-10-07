/* Gregorius Techneticies (2019), GregTech-6 Team, LGPL-3.0-or-later. */
package com.gregtech.gregtech.item;
import com.gregtech.gregtech.content.data.VisualDocumentData;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Source Paper_Printed_Pages: paper composition and title/author, ready for colored binding. */
public final class PrintedPagesItem extends TechItem {
    public PrintedPagesItem(boolean many,Item.Properties properties) {
        super(many?"Many Printed Pages":"Printed Pages",properties);
        com.gregtech.gregtech.api.material.ItemMaterialRegistry.register(this,new com.gregtech.gregtech.api.material.ItemComposition(null,List.of(com.gregtech.gregtech.api.material.MaterialComponent.of(com.gregtech.gregtech.content.material.Materials.Paper,com.gregtech.gregtech.api.material.GTValues.U*(many?6:3))),"GT6 Printed Pages",true));
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);VisualDocumentData.pagesTooltip(stack,tooltip);
    }
}
