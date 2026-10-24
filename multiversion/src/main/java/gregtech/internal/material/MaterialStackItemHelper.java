package gregtech.internal.material;

import gregtech.data.MaterialPrefix;
import gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;

public final class MaterialStackItemHelper {
    private MaterialStackItemHelper() {}

    public static ItemStack createStack(MaterialPrefix prefix, GTMaterial material, int count) {
        return GTItems.getStack(prefix, material, count);
    }

    public static ItemStack mat(MaterialPrefix prefix, GTMaterial material, int count) {
        return createStack(prefix, material, count);
    }
}
