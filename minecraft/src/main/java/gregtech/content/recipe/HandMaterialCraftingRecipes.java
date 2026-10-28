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

/** GT6 BlockStones:262, Loader_Recipes_Vanilla:155-157 and Loader_OreProcessing:150-181. */
public final class HandMaterialCraftingRecipes {
    private HandMaterialCraftingRecipes() {}

    private static final JsonElement SAW = tag("forge:tools/saw");
    private static final JsonElement FILE = tag("forge:tools/file");
    private static final JsonElement WRENCH = tag("forge:tools/wrench");
    private static final JsonElement WIRE_CUTTER = tag("forge:tools/wire_cutter");
    private static final JsonElement HAMMER = tag("forge:tools/hammer");

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
            handForms(rows, material);
            smallGear(rows, material);
            var bolt = mat("bolt", material);
            var screw = mat("screw", material);
            if (bolt.isEmpty() || screw.isEmpty()) continue;
            // Loader_OreProcessing:165 bolt -> screw.
            put(rows, "craft/screw/" + slug(material), List.of("fX", "X "),
                    Map.of("f", FILE, "X", form("bolt", material, bolt)), copy(screw, 1));
        }
        return Map.copyOf(rows);
    }

    /**
     * Loader_OreProcessing:150-181 {@code OreProcessing_CraftFrom} hand rows. The transpiler keeps
     * only the machine maps, and this port had ported just the screw row, so every other prefix
     * conversion was missing from the workbench.
     */
    private static void handForms(Map<ResourceLocation, JsonObject> rows, GTMaterial material) {
        String m = slug(material);
        var plate = mat("plate", material);
        var stick = mat("stick", material);
        var stickLong = mat("stickLong", material);
        var gem = mat("gem", material);

        // :150 four plates and four sticks held by a wrench; smithable materials only.
        var gear = mat("gearGt", material);
        if (material.has(MaterialProperty.SMITHABLE) && !gear.isEmpty() && !plate.isEmpty() && !stick.isEmpty())
            put(rows, "craft/gear/" + m, List.of("XYX", "YwY", "XYX"),
                    Map.of("X", form("stick", material, stick),
                            "Y", form("plate", material, plate),
                            "w", WRENCH), copy(gear, 1));

        // :164 one stick saws into two bolts; wood and coated materials keep their own rows.
        var bolt = mat("bolt", material);
        if (!material.has(MaterialProperty.WOOD) && !bolt.isEmpty() && !stick.isEmpty())
            put(rows, "craft/bolt/" + m, List.of("s ", " S"),
                    Map.of("s", SAW, "S", form("stick", material, stick)), copy(bolt, 2));

        // :166-169 gem -> ring, chunk -> round, foil -> fine wire, plate -> thin wire.
        var ring = mat("ring", material);
        if (!ring.isEmpty() && !gem.isEmpty())
            put(rows, "craft/ring/" + m, List.of("f ", " X"),
                    Map.of("f", FILE, "X", form("gem", material, gem)), copy(ring, 1));
        var chunk = mat("chunkGt", material);
        var round = mat("round", material);
        if (!round.isEmpty() && !chunk.isEmpty())
            put(rows, "craft/round/" + m, List.of("f ", " X"),
                    Map.of("f", FILE, "X", form("chunkGt", material, chunk)), copy(round, 1));
        var foil = mat("foil", material);
        var wireFine = mat("wireFine", material);
        if (!wireFine.isEmpty() && !foil.isEmpty())
            put(rows, "craft/wire_fine/" + m, List.of("Xx"),
                    Map.of("X", form("foil", material, foil), "x", WIRE_CUTTER), copy(wireFine, 1));
        var wire = mat("wireGt01", material);
        if (!wire.isEmpty() && !plate.isEmpty())
            put(rows, "craft/wire/" + m, List.of("Px"),
                    Map.of("P", form("plate", material, plate), "x", WIRE_CUTTER), copy(wire, 1));

        // :170 eight tiny plates from one plate; paper and wood use their own forms.
        var plateTiny = mat("plateTiny", material);
        if (!plateTiny.isEmpty() && !plate.isEmpty()
                && !material.getName().equals("Paper") && !material.has(MaterialProperty.WOOD))
            put(rows, "craft/plate_tiny/" + m, List.of("s ", " P"),
                    Map.of("s", SAW, "P", form("plate", material, plate)), copy(plateTiny, 8));

        // :171,175-178 gem plates and tiny gem plates.
        var plateGem = mat("plateGem", material);
        var plateGemTiny = mat("plateGemTiny", material);
        if (!plateGem.isEmpty()) {
            if (!plateGemTiny.isEmpty())
                put(rows, "craft/plate_gem_tiny/" + m, List.of("s ", " C"),
                        Map.of("s", SAW, "C", form("plateGem", material, plateGem)), copy(plateGemTiny, 8));
            gemPlate(rows, material, "gemChipped", 2, plateGemTiny);
            gemPlate(rows, material, "gemFlawed", 4, plateGemTiny);
            gemPlate(rows, material, "gem", 8, plateGemTiny);
            gemPlate(rows, material, "gemFlawless", 1, plateGem);
            gemPlate(rows, material, "gemExquisite", 3, plateGem);
            gemPlate(rows, material, "gemLegendary", 7, plateGem);
            gemPlate(rows, material, "bouleGt", 3, plateGem);
        }

        // :157-159 long sticks from flawless gems; :160 a long stick saws into two sticks.
        gemStick(rows, material, "stick_long", "gemFlawless", 1, stickLong, new String[] {"sf", " X"});
        gemStick(rows, material, "stick_long", "gemExquisite", 2, stickLong, new String[] {"sf", " X"});
        gemStick(rows, material, "stick_long", "gemLegendary", 4, stickLong, new String[] {"sf", " X"});
        if (!stick.isEmpty() && !stickLong.isEmpty())
            put(rows, "craft/stick_long_from_long/" + m, List.of("s ", " X"),
                    Map.of("s", SAW, "X", form("stickLong", material, stickLong)), copy(stick, 2));

        // :161-164 gems cut and filed into sticks.
        gemStick(rows, material, "stick", "gem", 1, stick, new String[] {"s ", "fX"});
        gemStick(rows, material, "stick", "gemFlawless", 2, stick, new String[] {"s ", "fX"});
        gemStick(rows, material, "stick", "gemExquisite", 4, stick, new String[] {"s ", "fX"});
        gemStick(rows, material, "stick", "gemLegendary", 8, stick, new String[] {"s ", "fX"});

        // :179 cart wheels: two rings around a stick, hammered and wrenched.
        var wheels = mat("minecartWheels", material);
        if (!wheels.isEmpty() && !ring.isEmpty() && !stick.isEmpty())
            put(rows, "craft/minecart_wheels/" + m, List.of(" h ", "XSX", " w "),
                    Map.of("X", form("ring", material, ring),
                            "S", form("stick", material, stick),
                            "h", HAMMER,
                            "w", WRENCH), copy(wheels, 1));
    }

    /** Loader_Recipes_Other:417: one plate and a file (stone) or hard hammer forms a small gear. */
    private static void smallGear(Map<ResourceLocation, JsonObject> rows, GTMaterial material) {
        if (material.has(MaterialProperty.WOOD)) return;
        var plate = mat("plate", material);
        var gear = mat("gearGtSmall", material);
        if (plate.isEmpty() || gear.isEmpty()) return;
        boolean stone = material.has(MaterialProperty.STONE);
        // Tool tags also accept the single-use forms and every tool with that GT6 identity.
        put(rows, "craft/gear_small/" + slug(material), List.of("P ", stone ? " f" : " h"),
                Map.of("P", form("plate", material, plate),
                        stone ? "f" : "h", stone ? FILE : HAMMER), copy(gear, 1));
    }

    private static void gemPlate(Map<ResourceLocation, JsonObject> rows, GTMaterial material,
                                 String gemPrefix, int count, ItemStack output) {
        var input = mat(gemPrefix, material);
        if (output.isEmpty() || input.isEmpty()) return;
        put(rows, "craft/plate_gem/" + gemPrefix.toLowerCase(Locale.ROOT) + "/" + slug(material),
                List.of("s ", " X"),
                Map.of("s", SAW, "X", form(gemPrefix, material, input)), copy(output, count));
    }

    private static void gemStick(Map<ResourceLocation, JsonObject> rows, GTMaterial material, String idPrefix,
                                 String gemPrefix, int count, ItemStack output, String[] pattern) {
        var input = mat(gemPrefix, material);
        if (output.isEmpty() || input.isEmpty()) return;
        put(rows, "craft/" + idPrefix + "/" + gemPrefix.toLowerCase(Locale.ROOT) + "/" + slug(material),
                List.of(pattern),
                Map.of("s", SAW, "f", FILE, "X", form(gemPrefix, material, input)), copy(output, count));
    }

    private static void rocks(Map<ResourceLocation, JsonObject> rows, GTMaterial material, ItemStack output) {
        if (material == null || !material.isValid() || output.isEmpty()) return;
        var rock = mat("rockGt", material);
        if (rock.isEmpty()) return;
        put(rows, "rocks/" + slug(material), List.of("XX", "XX"),
                Map.of("X", form("rock_gt", material, rock)), output);
    }

    private static ItemStack mat(String prefix, GTMaterial material) {
        var registered = PrefixRegistry.byName(prefix);
        return registered == null ? ItemStack.EMPTY : GTItems.getStack(registered, material, 1);
    }

    private static ItemStack copy(ItemStack stack, int count) {
        var result = stack.copy();
        result.setCount(count);
        return result;
    }

    private static String slug(GTMaterial material) {
        return material.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static JsonElement form(String prefix, GTMaterial material, ItemStack stack) {
        var choices = new JsonArray();
        choices.add(item(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        var tag = new JsonObject();
        tag.addProperty("tag", "gt6ce:" + MaterialPrefix.camelToSnake(prefix) + "/"
                + MaterialEquivalence.materialName(material));
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
