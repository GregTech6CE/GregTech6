package gregtech.content.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.material.SourceLatheMaterialProfiles;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.prefix.PrefixRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** GT6 BlockStones:262, Loader_Recipes_Vanilla:155-157 and Loader_OreProcessing:165. */
public final class HandMaterialCraftingRecipes {
    private HandMaterialCraftingRecipes() {}
    public static Map<ResourceLocation, JsonObject> recipes() {
        var rows = new LinkedHashMap<ResourceLocation, JsonObject>();
        rocks(rows, GTMaterialRegistry.get("Stone"), new ItemStack(Items.COBBLESTONE));
        rocks(rows, GTMaterialRegistry.get("Netherrack"), new ItemStack(Items.NETHERRACK));
        rocks(rows, GTMaterialRegistry.get("Endstone"), new ItemStack(Items.END_STONE));
        for (var type : StoneType.values()) {
            var block = GTBlocks.getStone(type, StoneVariant.COBBLE);
            if (block != null) rocks(rows, type.material(), new ItemStack(block));
        }
        for (var material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || material.has(MaterialProperty.ANTIMATTER)) continue;
            var profile = SourceLatheMaterialProfiles.find(material);
            if (profile != null && profile.coated()) continue;
            gemPlates(rows, material);
            smallGear(rows, material);
            largeGear(rows, material);
            var bolt = GTItems.getStack(PrefixRegistry.byName("bolt"), material, 1);
            var screw = GTItems.getStack(PrefixRegistry.byName("screw"), material, 1);
            if (bolt.isEmpty() || screw.isEmpty()) continue;
            put(rows, "screw/" + material.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"), List.of("fX", "X "),
                    Map.of("f", item("gt6ce:tool_file"), "X", form("bolt", material, bolt)), screw);
        }
        return Map.copyOf(rows);
    }

    /**
     * Loader_Recipes_Other:417: one plate and a file (stone materials) or a hard hammer forms
     * a small gear. Wood keeps its own plank/button rows, and coated materials are excluded.
     */
    private static void smallGear(Map<ResourceLocation, JsonObject> rows, GTMaterial material) {
        if (material.has(MaterialProperty.WOOD)) return;
        var plate = GTItems.getStack(MaterialPrefix.plate, material, 1);
        var gear = GTItems.getStack(MaterialPrefix.gearGtSmall, material, 1);
        if (plate.isEmpty() || gear.isEmpty()) return;
        boolean stone = material.has(MaterialProperty.STONE);
        // Tool tags also accept the single-use forms and every tool with that GT6 identity.
        put(rows, "gear_small/" + slug(material), List.of("P ", stone ? " f" : " h"),
                Map.of("P", form("plate", material, plate),
                        stone ? "f" : "h", tag(stone ? "forge:tools/file" : "forge:tools/hammer")),
                gear);
    }

    /**
     * Port addition for issue #173: GT6 only hand-crafts stone and wooden large gears and leaves
     * metal gears to the Extruder and the casting molds. Assembling a gear from four plates and
     * four bolts with a saw gives every gear-bearing material a crafting-table route as well.
     */
    private static void largeGear(Map<ResourceLocation, JsonObject> rows, GTMaterial material) {
        if (material.has(MaterialProperty.WOOD) || material.has(MaterialProperty.STONE)) return;
        var plate = GTItems.getStack(MaterialPrefix.plate, material, 1);
        var bolt = GTItems.getStack(PrefixRegistry.byName("bolt"), material, 1);
        var gear = GTItems.getStack(MaterialPrefix.gearGt, material, 1);
        if (plate.isEmpty() || bolt.isEmpty() || gear.isEmpty()) return;
        put(rows, "gear/" + slug(material), List.of("BPB", "PsP", "BPB"),
                Map.of("P", form("plate", material, plate),
                        "B", form("bolt", material, bolt),
                        "s", tag("forge:tools/saw")),
                gear);
    }

    private static String slug(GTMaterial material) {
        return material.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static void gemPlates(Map<ResourceLocation, JsonObject> rows, GTMaterial material) {
        for (var entry : Map.of("gemFlawless", 1, "gemExquisite", 3, "gemLegendary", 7).entrySet()) {
            var input = GTItems.getStack(PrefixRegistry.byName(entry.getKey()), material, 1);
            var output = GTItems.getStack(PrefixRegistry.byName("plateGem"), material, entry.getValue());
            if (input.isEmpty() || output.isEmpty()) continue;
            String id = entry.getKey().toLowerCase(Locale.ROOT) + "/"
                    + material.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            put(rows, "gem_plate/" + id, List.of("s ", " X"),
                    Map.of("s", item("gt6ce:tool_saw"), "X", item(BuiltInRegistries.ITEM.getKey(input.getItem()).toString())), output);
        }
    }
    private static void rocks(Map<ResourceLocation, JsonObject> rows, GTMaterial material, ItemStack output) {
        if (material == null || !material.isValid() || output.isEmpty()) return;
        var rock = GTItems.getStack(PrefixRegistry.byName("rockGt"), material, 1);
        if (rock.isEmpty()) return;
        put(rows, "rocks/" + material.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"), List.of("XX", "XX"),
                Map.of("X", form("rock_gt", material, rock)), output);
    }
    private static JsonElement form(String prefix, GTMaterial material, ItemStack stack) {
        var choices = new JsonArray();
        choices.add(item(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        var tag = new JsonObject();
        tag.addProperty("tag", "gt6ce:" + prefix + "/" + MaterialEquivalence.materialName(material));
        choices.add(tag);
        return choices;
    }
    private static JsonObject item(String id) {
        var json = new JsonObject(); json.addProperty("item", id); return json;
    }
    private static JsonObject tag(String id) {
        var json = new JsonObject(); json.addProperty("tag", id); return json;
    }
    private static void put(Map<ResourceLocation, JsonObject> rows, String id, List<String> pattern,
                            Map<String, JsonElement> keys, ItemStack output) {
        var json = new JsonObject();
        json.addProperty("type", "gt6ce:tool_shaped");
        json.addProperty("allow_mirror", false);
        var shape = new JsonArray(); pattern.forEach(shape::add); json.add("pattern", shape);
        var key = new JsonObject(); keys.forEach(key::add); json.add("key", key);
        var result = new JsonObject();
//? if >=1.21.1 {
        /*result.addProperty("id", BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
*///?} else {
        result.addProperty("item", BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
//?}
        result.addProperty("count", output.getCount()); json.add("result", result);
        rows.put(ResourceLocation.fromNamespaceAndPath("gt6ce", "hand_material/" + id), json);
    }
}
