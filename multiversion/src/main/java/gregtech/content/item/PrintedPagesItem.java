/* Gregorius Techneticies (2019), GregTech-6 Team, LGPL-3.0-or-later. */
package gregtech.content.item;
import gregtech.content.data.VisualDocumentData;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Source Paper_Printed_Pages: paper composition and title/author, ready for colored binding. */
public final class PrintedPagesItem extends TechItem {
    public PrintedPagesItem(boolean many,Item.Properties properties) {
        super(many?"Many Printed Pages":"Printed Pages",properties);
        gregtech.internal.material.ItemMaterialRegistry.register(this,new gregtech.internal.material.ItemComposition(null,List.of(gregtech.internal.material.MaterialComponent.of(gregtech.content.material.Materials.Paper,gregtech.api.material.GTValues.U*(many?6:3))),"GT6 Printed Pages",true));
    }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
*///?} else {
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level context,List<Component> tooltip,TooltipFlag flag) {
//?}
        super.appendHoverText(stack,context,tooltip,flag);VisualDocumentData.pagesTooltip(stack,tooltip);
    }
}
