package gregtech.api.material;

import gregtech.data.MaterialPrefix;
import gregtech.item.MaterialItem;
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
    private MaterialDisplayBinding() {}
    /** Tag-bound compositions may change on reload or when joining another server. */
    public static synchronized void invalidate() { aliases = null; }
    public static synchronized List<ItemStack> alternatives(ItemStack display) {
        if (!(display.getItem() instanceof gregtech.api.material.MaterialFormItem item) || item.getPrefix()!=MaterialPrefix.unit) return List.of();
        if (aliases==null) {
            Map<GTMaterial,List<ItemStack>> index=new IdentityHashMap<>();
//? if >=1.21.1 {
            /*for(Item registered:BuiltInRegistries.ITEM) {
*///?} else {
            for(Item registered:ForgeRegistries.ITEMS.getValues()) {
//?}
                var stack=new ItemStack(registered);var form=MaterialEquivalence.form(stack);
                if(form==null||form.prefix()==MaterialPrefix.unit)continue;
                index.computeIfAbsent(form.material().resolve(),k->new ArrayList<>()).add(stack);
            }
            index.replaceAll((m,stacks)->List.copyOf(stacks));aliases=index;
        }
        return aliases.getOrDefault(item.getMaterial().resolve(),List.of()).stream().map(ItemStack::copy).toList();
    }
}
