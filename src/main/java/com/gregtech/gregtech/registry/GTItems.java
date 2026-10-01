package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GTItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    public record CreativeEntry(GTMaterial material, RegistryObject<Item> item) {}
    private static final Map<String, java.util.NavigableMap<String,CreativeEntry>> BY_PREFIX = new HashMap<>();

    private GTItems() {}

    public static void bind(MaterialPrefix prefix, GTMaterial material, RegistryObject<Item> item) {
        BY_PREFIX.computeIfAbsent(prefix.getName(), ignored -> new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder())))
                .put(material.getName(),new CreativeEntry(material,item));
    }

    public static String key(MaterialPrefix prefix, GTMaterial material) {
        return prefix.getName() + "/" + material.getName();
    }

    public static RegistryObject<Item> getObject(MaterialPrefix prefix, GTMaterial material) {
        var entries=BY_PREFIX.get(prefix.getName());
        var entry=entries==null?null:entries.get(material.getName());
        return entry==null?null:entry.item();
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

    public static ItemStack getStack(MaterialPrefix prefix, GTMaterial material, int count) {
        RegistryObject<Item> item = getObject(prefix, material);
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get(), count);
    }

    /** Creative tabs require exactly one item per entry. */
    public static ItemStack getCreativeStack(MaterialPrefix prefix, GTMaterial material) {
        return getStack(prefix, material, 1);
    }

    public static Collection<RegistryObject<Item>> allEntries() {
        return BY_PREFIX.values().stream().flatMap(entries -> entries.values().stream()).map(CreativeEntry::item).toList();
    }

    public static boolean hasBoundItems(MaterialPrefix prefix) {
        var entries=BY_PREFIX.get(prefix.getName());
        return entries!=null && !entries.isEmpty();
    }

    /** Sorted registry bindings, not a second cache of tens of thousands of ItemStacks. */
    public static Collection<CreativeEntry> creativeEntries(MaterialPrefix prefix) {
        var entries=BY_PREFIX.get(prefix.getName());
        return entries==null?java.util.List.of():Collections.unmodifiableCollection(entries.values());
    }
}
