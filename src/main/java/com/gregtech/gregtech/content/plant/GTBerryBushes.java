package com.gregtech.gregtech.content.plant;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Platform item lookup over the original berry colors and cotton fallback. */
public final class GTBerryBushes extends BerryBushCatalog {
    private GTBerryBushes() {}

    public static ResourceLocation itemId(String id) {
        if (id == null || id.isEmpty()) return null;
        if ("default".equals(id) || "string".equals(id)) return ResourceLocation.withDefaultNamespace("string");
        return ResourceLocation.tryParse(id.contains(":") ? id : "gregtech:" + id);
    }

    public static BerryType of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.is(Items.STRING)) return DEFAULT;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return null;
        var known = id.getNamespace().equals("gregtech") ? BerryBushCatalog.byId(id.getPath()) : null;
        if (known != null) return known;
        var form = com.gregtech.gregtech.api.material.MaterialEquivalence.form(stack);
        if (form == null || form.prefix() != com.gregtech.gregtech.data.MaterialPrefix.plantGtBerry) return null;
        return MaterialBerryBushCatalog.colours(id.toString(), form.material().getColor());
    }

    public static BerryType byId(String id) {
        var known = BerryBushCatalog.byId(id);
        if (known != null) return known;
        var key = itemId(id);
        if (key == null) return null;
        var item = ForgeRegistries.ITEMS.getValue(key);
        return item == null ? null : of(new ItemStack(item));
    }

    public static BerryType ofOrDefault(ItemStack stack) {
        var type = of(stack);
        return type == null ? DEFAULT : type;
    }
}
