/* Gregorius Techneticies (2019), GregTech-6 Team, LGPL-3.0-or-later. */
package gregtech.content.item;

import gregtech.api.material.*;
import gregtech.content.data.VisualDocumentData;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Source Paper_Printed_Pages: paper composition and title/author, ready for colored binding. */
public final class PrintedPagesItem extends TechItem {
    public PrintedPagesItem(boolean many, Item.Properties properties) {
        super(many ? "Many Printed Pages" : "Printed Pages", properties);
        gregtech.engine.material.ItemMaterialRegistry.register(
                this,
                null,
                gregtech.content.material.Materials.Paper,
                gregtech.api.material.GTValues.U * (many ? 6 : 3));
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        super.appendTooltip(stack, context, tooltip, flag);
        VisualDocumentData.pagesTooltip(stack, tooltip);
    }
}
