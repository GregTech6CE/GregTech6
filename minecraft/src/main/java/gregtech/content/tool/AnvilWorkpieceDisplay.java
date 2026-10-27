package gregtech.content.tool;

import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialFormItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Only verified ingot forms opt foreign items into the GT anvil's solid workpiece model. */
public final class AnvilWorkpieceDisplay {
    private AnvilWorkpieceDisplay() {}

    @Nullable
    public static MaterialEquivalence.Form materialForm(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof MaterialFormItem item)
            return new MaterialEquivalence.Form(item.getPrefix(), item.getMaterial());
        // Keep actual foreign block models and unrelated item models, not a generic material cube.
        if (stack.getItem() instanceof BlockItem) return null;
        var form = MaterialEquivalence.form(stack);
        return form != null && form.prefix().getName().startsWith("ingot") ? form : null;
    }
}
