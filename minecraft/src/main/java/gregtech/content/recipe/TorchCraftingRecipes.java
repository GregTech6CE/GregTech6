package gregtech.content.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gregtech.GregTechConfig;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.prefix.PrefixRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Normal wooden torches from GT6 Loader_Recipes_Vanilla's explicit fuel roster, not furnace fuels. */
public final class TorchCraftingRecipes {
    private static final List<String> BASE = List.of("oreRaw", "gem", "dust", "crushed", "crushedPurified", "crushedCentrifuged");
    private static final List<String> FULL = List.of("rockGt", "oreRaw", "ingot", "gem", "dust", "crushed", "crushedPurified", "crushedCentrifuged");
    private TorchCraftingRecipes() {}

    public static Map<ResourceLocation, JsonObject> recipes() {
        var rows = new LinkedHashMap<ResourceLocation, JsonObject>();
        forms(rows, "Sulfur", 2, BASE);
        for (String phosphorus : List.of("Phosphor", "Phosphorus", "BluePhosphorus", "RedPhosphorus", "WhitePhosphorus"))
            forms(rows, phosphorus, 6, BASE);
        forms(rows, "Blaze", 2, List.of("dustTiny"));
        forms(rows, "Peat", 4, FULL);
        forms(rows, "PeatBituminous", 8, FULL);
        direct(rows, "resin", "gt6ce:rubber_resin", 4);
        direct(rows, "dry_grass", "gt6ce:dry_grass", 1);
        // External solid tar, torchberries and Netherlicious shroomlight belong to their compatibility loaders.
        // Never substitute a fluid bottle or the unrelated vanilla shroomlight block for those source items.
        fluid(rows, "Resin_Rubber", 6, 6);
        fluid(rows, "Resin_Spruce", 6, 6);
        fluid(rows, "Sap_Maple", 6, 2);
        fluid(rows, "Sap_Rainbow", 8, 4);
        fluid(rows, "Creosote", 6, 2);
        // Original disabledrecipes.torchesFromCoal defaults false and guards this entire family.
        if (!GregTechConfig.Rule.DISABLE_COAL_TORCHES.enabled()) {
            forms(rows, "Charcoal", 4, List.of("oreRaw", "ingot", "gem", "dust"));
            forms(rows, "Coal", 4, FULL);
            forms(rows, "CoalCoke", 8, FULL);
            forms(rows, "Anthracite", 8, FULL);
            forms(rows, "Lignite", 2, FULL);
            forms(rows, "LigniteCoke", 4, FULL);
            forms(rows, "OilShale", 4, FULL);
        }
        rows.put(ResourceLocation.fromNamespaceAndPath("gt6ce", "vanilla/soul_torches"), soulTorches());
        return Map.copyOf(rows);
    }

    /** Modern soul conversion uses the live block tag, not a similarly named item tag. */
    private static JsonObject soulTorches() {
        var json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");
        var ingredients = new JsonArray();
        for (int i = 0; i < 4; i++) ingredients.add(item("minecraft:torch"));
        var base = new JsonObject();
        base.addProperty("type", "gt6ce:block_tag");
        base.addProperty("tag", "minecraft:soul_fire_base_blocks");
        ingredients.add(base);
        json.add("ingredients", ingredients);
        var result = new JsonObject();
//? if >=1.21.1 {
        /*result.addProperty("id", "minecraft:soul_torch");
*///?} else {
        result.addProperty("item", "minecraft:soul_torch");
//?}
        result.addProperty("count", 4);
        json.add("result", result);
        return json;
    }

    private static void forms(Map<ResourceLocation, JsonObject> rows, String name, int count, List<String> prefixes) {
        var material = GTMaterialRegistry.get(name);
        if (material == null || !material.isValid()) throw new IllegalStateException("Missing source torch material " + name);
        for (String prefix : prefixes) {
            var ids = new LinkedHashSet<String>();
            var canonical = GTItems.getStack(PrefixRegistry.byName(prefix), material, 1);
            if (!canonical.isEmpty()) ids.add(BuiltInRegistries.ITEM.getKey(canonical.getItem()).toString());
            // Include extant GT form identities even where preferred output unification selects vanilla.
            for (var item : BuiltInRegistries.ITEM)
                if (item instanceof MaterialFormItem form && form.getMaterial().resolve() == material.resolve()
                        && PrefixRegistry.sourceName(form.getPrefix().getName()).equals(prefix))
                    ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
            if (ids.isEmpty()) continue; // The source ore-dictionary recipe is likewise unavailable without this form.
            var alternatives = new JsonArray();
            ids.forEach(id -> alternatives.add(item(id)));
            put(rows, name.toLowerCase(Locale.ROOT) + "/" + prefix.toLowerCase(Locale.ROOT), count,
                    List.of("X", "S"), Map.of("X", alternatives, "S", tag("gt6ce:ammunition/sticks_wood")));
        }
    }

    private static void direct(Map<ResourceLocation, JsonObject> rows, String name, String id, int count) {
        if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)) == Items.AIR) return;
        put(rows, name, count, List.of("X", "S"), Map.of("X", item(id), "S", tag("gt6ce:ammunition/sticks_wood")));
    }

    private static void fluid(Map<ResourceLocation, JsonObject> rows, String fluid, int large, int small) {
        for (int amount : new int[]{1000, 250}) {
            var container = new JsonObject();
            container.addProperty("type", "gt6ce:finite_fluid_container_" + amount);
            container.addProperty("fluid", fluid);
            for (boolean string : new boolean[]{false, true})
                put(rows, fluid.toLowerCase(Locale.ROOT) + "/" + amount + (string ? "_string" : "_wool"),
                        amount == 1000 ? large : small, string ? List.of("WCW", "WSW") : List.of("WC", "S "),
                        Map.of("W", string ? item("minecraft:string") : tag("minecraft:wool"),
                                "C", container, "S", tag("gt6ce:ammunition/sticks_wood")));
        }
    }

    private static JsonObject item(String id) { var json = new JsonObject(); json.addProperty("item", id); return json; }
    private static JsonObject tag(String id) { var json = new JsonObject(); json.addProperty("tag", id); return json; }

    private static void put(Map<ResourceLocation, JsonObject> rows, String id, int count,
                            List<String> pattern, Map<String, ? extends JsonElement> ingredients) {
        var json = new JsonObject();
        json.addProperty("type", "gt6ce:tool_shaped");
        json.addProperty("group", "gt.torches");
        json.addProperty("allow_mirror", false);
        var shape = new JsonArray(); pattern.forEach(shape::add); json.add("pattern", shape);
        var key = new JsonObject(); ingredients.forEach(key::add); json.add("key", key);
        var result = new JsonObject();
//? if >=1.21.1 {
        /*result.addProperty("id", "minecraft:torch");
*///?} else {
        result.addProperty("item", "minecraft:torch");
//?}
        result.addProperty("count", count); json.add("result", result);
        rows.put(ResourceLocation.fromNamespaceAndPath("gt6ce", "vanilla/torch/" + id), json);
    }
}
