package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Search aliases for informational unit stacks; never used for recipe matching. */
public final class MaterialDisplayBinding {
    private static Map<GTMaterial,List<ItemStack>> aliases;
    private MaterialDisplayBinding() {}
    public static synchronized List<ItemStack> alternatives(ItemStack display) {
        if (!(display.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem item) || item.getPrefix()!=MaterialPrefix.unit) return List.of();
        if (aliases==null) {
            Map<GTMaterial,List<ItemStack>> index=new IdentityHashMap<>();
            for(Item registered:BuiltInRegistries.ITEM) {
                var stack=new ItemStack(registered);var form=MaterialEquivalence.form(stack);
                if(form==null||form.prefix()==MaterialPrefix.unit)continue;
                index.computeIfAbsent(form.material().resolve(),k->new ArrayList<>()).add(stack);
            }
            index.replaceAll((m,stacks)->List.copyOf(stacks));aliases=index;
        }
        return aliases.getOrDefault(item.getMaterial().resolve(),List.of()).stream().map(ItemStack::copy).toList();
    }
}
