package gregtech.content.block.machine;

import gregtech.content.recipe.ClayMoldCatalog;
import net.minecraft.world.item.ItemStack;

/** Native block-item shape data; normal BlockItem placement loads this into the mold entity. */
public final class MoldItemData {
    private MoldItemData() {}

    public static int shape(ItemStack stack) {
        return gregtech.engine.stack.BlockEntityData.readOrEmpty(stack).getInt(ClayMoldCatalog.SHAPE_KEY);
    }

    public static ItemStack withShape(ItemStack stack, int shape) {
        var tag = gregtech.engine.stack.BlockEntityData.readOrEmpty(stack);
        if (shape == 0) {
            tag.remove(ClayMoldCatalog.SHAPE_KEY);
            if (tag.isEmpty() || (tag.size() == 1 && tag.contains("id"))) {
                gregtech.engine.stack.BlockEntityData.remove(stack);
                return stack;
            }
        }
        tag.putString("id", "gt6ce:mold");
        if (shape != 0) tag.putInt(ClayMoldCatalog.SHAPE_KEY, shape);
        gregtech.engine.stack.BlockEntityData.write(stack, tag);
        return stack;
    }
}
