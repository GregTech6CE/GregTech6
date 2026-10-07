package com.gregtech.gregtech.registry;


import com.gregtech.gregtech.item.TechItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.*;

/** Technological items; cover hosts and machines provide the behavior of installed components. */
public final class GTTechnological {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, "gregtech");

    private static final Map<String, DeferredHolder<Item,Item>> BY_ID = new LinkedHashMap<>();

    private GTTechnological() {}

    /** All registered tech items in insertion order. */
    public static Collection<DeferredHolder<Item,Item>> all() { return BY_ID.values(); }

    public static void registerAll() {
        for (String id : IDS) {
            String name = englishName(id);
            boolean catalyst = id.contains("_shape_");
            DeferredHolder<Item,Item> obj = ITEMS.register(id, () -> new TechItem(name, catalyst, new Item.Properties().stacksTo(64)));
            BY_ID.put(id, obj);
        }
        for (var variant : com.gregtech.gregtech.content.cover.CanvasRules.VARIANTS)
            BY_ID.put(variant.path(), ITEMS.register(variant.path(), () -> new com.gregtech.gregtech.item.CanvasItem(variant)));
        // GT6 ST.tag equivalents: integrated circuits select recipe variants, never consumed.
        for (int i = 0; i < 25; i++) {
            String id = "integrated_circuit_" + i;
            final int configuration = i;
            DeferredHolder<Item,Item> obj = ITEMS.register(id, () -> new com.gregtech.gregtech.item.SelectorTagItem(configuration, new Item.Properties().stacksTo(64)));
            BY_ID.put(id, obj);
        }
    }

    /** Integrated circuit N (GT6 {@code ST.tag(N)}), or null before registration. */
    public static Item selectorTag(int n) {
        return get("integrated_circuit_" + n);
    }

    /** Tech item by registry id (e.g. {@code "extruder_shape_plate"}), or null before registration. */
    public static Item get(String id) {
        DeferredHolder<Item,Item> obj = BY_ID.get(ALIASES.getOrDefault(id, id.replace("compact__electric_conveyor", "compact_electric_conveyor")));
        return obj != null && obj.isBound() ? obj.get() : null;
    }

    /**
     * GT6 item names the transpiled loaders spell differently from the port's registry ids.
     *
     * <p>The recipe transpiler turns {@code IL.<Field>.get(n)} into {@code tech:<field lower case>}
     * (tools/transpile_gt6_chem.py:160-173), so a GT6 item the port registers under another spelling
     * would silently count as missing content. Each entry here is the same item under two names —
     * verified against the item's own registration, not guessed:
     * {@code IL.Shape_Press_BulletCasing*} are the three bullet-casing press molds the Press uses as
     * catalysts (GT6 {@code Loader_Recipes_Handlers:254-255} vs
     * {@code PressAmmunitionRecipes}), and {@code IL.Shape_Slicer_Eigths} is the original's typo of
     * "eighths". Read by {@code tools/audit_tech_item_tokens.py}.</p>
     */
    public static final Map<String,String> ALIASES=com.gregtech.gregtech.content.recipe.TechnologicalItemDefinitions.ALIASES;
    static final String[] IDS=com.gregtech.gregtech.content.recipe.TechnologicalItemDefinitions.IDS;
    private static String englishName(String id) { return com.gregtech.gregtech.content.recipe.TechnologicalItemDefinitions.englishName(id); }
}
