package gregtech.engine.material;

import gregtech.api.material.*;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.MaterialItem;
import net.minecraft.world.item.*;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.util.*;

/** Search aliases for informational unit stacks; never used for recipe matching. */
public final class MaterialDisplayBinding {
    private static Map<GTMaterial,List<ItemStack>> aliases;
    private static long revision;
    public static synchronized long revision() { return revision; }
    private MaterialDisplayBinding() {}
    /** Tag-bound compositions may change on reload or when joining another server. */
    public static synchronized void invalidate() { aliases = null; revision++; }
    public static synchronized List<ItemStack> alternatives(ItemStack display) {
        if (!(display.getItem() instanceof gregtech.engine.material.MaterialFormItem item) || item.getPrefix()!=MaterialPrefix.unit) return List.of();
        if (aliases==null) {
            Map<GTMaterial,List<ItemStack>> index=new IdentityHashMap<>();
//? if >=1.21.1 {
            /*for(Item registered:BuiltInRegistries.ITEM) {
*///?} else {
            for(Item registered:ForgeRegistries.ITEMS.getValues()) {
//?}
                var stack = new ItemStack(registered);
                if (registered instanceof MaterialFormItem form && form.getPrefix() == MaterialPrefix.unit) continue;
                var form = MaterialEquivalence.form(stack);
                GTMaterial material = form == null ? null : form.material().resolve();
                if (material == null) {
                    var data = ItemMaterialRegistry.get(stack).orElse(null);
                    if (data != null && data.components().size() == 1 && data.amount() > 0) material = data.material().resolve();
                }
                if (material != null) index.computeIfAbsent(material, k -> new ArrayList<>()).add(stack);
            }
            index.replaceAll((m,stacks)->List.copyOf(stacks));aliases=index;
        }
        return aliases.getOrDefault(item.getMaterial().resolve(),List.of()).stream().map(ItemStack::copy).toList();
    }
}
