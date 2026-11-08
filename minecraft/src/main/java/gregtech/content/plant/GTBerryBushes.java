package gregtech.content.plant;
import gregtech.platform.Mc;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** Platform item lookup over the original berry colors and cotton fallback. */
public final class GTBerryBushes extends BerryBushCatalog {
    private GTBerryBushes() {}

    public static ResourceLocation itemId(String id) {
        if (id == null || id.isEmpty()) return null;
        if ("default".equals(id) || "string".equals(id)) return ResourceLocation.withDefaultNamespace("string");
        return ResourceLocation.tryParse(id.contains(":") ? id : "gt6ce:" + id);
    }

    public static BerryType of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.is(Items.STRING)) return DEFAULT;
        ResourceLocation id = Mc.itemId(stack.getItem());
        if (id == null) return null;
        return id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE) ? BerryBushCatalog.byId(id.getPath()) : null;
    }

    public static BerryType byId(String id) {
        var known = BerryBushCatalog.byId(id);
        if (known != null) return known;
        var key = itemId(id);
        if (key == null) return null;
        var item = Mc.item(key);
        return item == null ? null : of(new ItemStack(item));
    }

    public static BerryType ofOrDefault(ItemStack stack) {
        var type = of(stack);
        return type == null ? DEFAULT : type;
    }
}
