/* GregTech-6 Team's colored canvas item registration, LGPL-3.0-or-later. */
package gregtech.content.item;

import gregtech.content.cover.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class CanvasItem extends TechItem {
    private final CanvasRules.Variant variant;

    public CanvasItem(CanvasRules.Variant variant) {
        super(variant.name(), new Item.Properties().stacksTo(64));
        this.variant = variant;
        gregtech.engine.material.ItemMaterialRegistry.register(
                this, null, gregtech.engine.material.GTMaterialRegistry.get("Paper"), gregtech.api.material.GTValues.U);
    }

    public CanvasRules.Variant variant() {
        return variant;
    }

    @Override
    protected void appendTooltip(
            ItemStack stack,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
        CanvasData.tooltip(CoverStackData.readOrEmpty(stack), tooltip);
        super.appendTooltip(stack, level, tooltip, flag);
    }
}
