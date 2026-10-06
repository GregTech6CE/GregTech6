package com.gregtech.gregtech.data;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.recipe.ElectricalCraftingRules;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTWires;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Translates the original GT6 crafting-key expressions (see
 * {@link BasicMachineCraftingRecipes}) into 1.20.1 ingredients.
 * <p>
 * Spec grammar:
 * <ul>
 *   <li>{@code item:<id>} — a concrete item id.</li>
 *   <li>{@code tag:<id>} — a tag id (used for vanilla/ore-dictionary references).</li>
 *   <li>{@code block:<prefix>[@<Material>]} — material casing/storage block of the machine's material.</li>
 *   <li>{@code mat:<prefix>[@<Material>]} — material item form (plate, gear, screw, ...).</li>
 *   <li>{@code pipe:<size>[@<Material>]} — GT6 fluid pipe block.</li>
 *   <li>{@code wire:<size>@<Material>} — GT6 bare wire block item.</li>
 *   <li>{@code cabletier:<n>} — GT6 {@code MT.DATA.CABLES_01[n]} (tin/copper/gold/aluminium/platinum).</li>
 *   <li>{@code wiretier:<n>} — GT6 {@code MT.DATA.WIRES_04[n]}, the 4x wire of that cable tier.</li>
 *   <li>{@code il:<ARRAY>} — GT6 {@code IL.*} component of the machine's tier
 *       (MOTORS, PUMPS, PISTONS, EMITTERS, CONVEYERS, SENSORS, ROBOT_ARMS, FIELD_GENERATORS).</li>
 *   <li>{@code circuit:<n>} — GT6 {@code OD_CIRCUITS[n]}: tier-n circuit plus every higher tier,
 *       matching GT6's cumulative ore-dictionary re-registration chain.</li>
 * </ul>
 * Every resolved form is checked against the live registry. When the exact form does not
 * exist in this port the legacy form fallback is logged once. Electrical tier requirements
 * retain their exact source grades; missing external circuits are never downgraded.
 */
public final class MachineRecipeIngredients {
    /** Substitute chains, tried in order when the exact material form is not registered. */
    private static final Map<String, List<String>> FALLBACKS = Map.ofEntries(
            Map.entry("plateQuintuple", List.of("plateQuadruple", "plateTriple", "plateDouble", "plate")),
            Map.entry("plateQuadruple", List.of("plateTriple", "plateDouble", "plate")),
            Map.entry("plateTriple", List.of("plateDouble", "plate")),
            Map.entry("plateDouble", List.of("plate")),
            Map.entry("plateDense", List.of("plateDouble", "plate")),
            Map.entry("gearGtSmall", List.of("gearGt")),
            Map.entry("gearGt", List.of("gearGtSmall")),
            Map.entry("spring", List.of("stickLong", "stick")),
            Map.entry("rotor", List.of("plateDouble", "plate")),
            Map.entry("screw", List.of("bolt", "stick")),
            Map.entry("bolt", List.of("screw")),
            Map.entry("stickLong", List.of("stick")),
            Map.entry("itemCasing", List.of("plate")),
            Map.entry("toolHeadBuzzSaw", List.of("toolHeadSaw", "plate")),
            Map.entry("plateGem", List.of("gem", "plate")),
            Map.entry("plateGemTiny", List.of("plateGem", "gem", "plate")),
            Map.entry("wireFine", List.of("wireGt01", "foil", "plate")),
            Map.entry("ring", List.of("plate")),
            Map.entry("foil", List.of("plate")));

    /** GT6 {@code IL.*[n]} arrays are ULV-first; recipes use index 1..5 for LV..IV. */
    private static final String[] IL_TIER_SUFFIX = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "xv"};

    /** GT6 {@code gt:re-batteryN} rechargeable battery keys. */
    private static final java.util.regex.Pattern BATTERY_OREDICT =
            java.util.regex.Pattern.compile("gt:re-battery(\\d+)");

    private static Map<MaterialPrefix, Map<GTMaterial, Item>> materialItems;
    private static Map<String, Map<GTMaterial, Item>> pipesBySize;
    private static Map<String, Map<GTMaterial, Item>> wiresBySize;
    private static final Map<String, String> SUBSTITUTIONS = new TreeMap<>();
    private static boolean validated;

    private MachineRecipeIngredients() {}

    // ── Resolution ────────────────────────────────────────────────────────────

    /**
     * Resolve one spec into an ingredient JSON value: either
     * {@code {"item": id}}, {@code {"tag": id}} or an array of those (OR).
     *
     * @param spec     spec string from the generated table
     * @param material casing material of the machine (GT6's {@code aMat})
     * @param tier     port tier of the machine (1-based)
     */
    public static Object resolve(String spec, GTMaterial material, int tier) {
        int colon = spec.indexOf(':');
        if (colon < 0) return itemStackFallback(material);
        String kind = spec.substring(0, colon);
        String argument = spec.substring(colon + 1);
        switch (kind) {
            case "item": {
                var ingredient = byItemId(argument.trim());
                if (ingredient == null) {
                    note("item " + argument, "machine casing block");
                    return blockIngredient(BlockMaterialPrefix.casingMachine, material);
                }
                return ingredient;
            }
            case "tag":
                return Map.of("tag", argument.trim());
            case "block": {
                String[] parts = splitMaterial(argument);
                return blockIngredient(blockPrefix(parts[0]),
                        parts[1] == null ? material : fixedMaterial(parts[1], material, "block " + parts[0]));
            }
            case "mat": {
                String[] parts = splitMaterial(argument);
                GTMaterial target = parts[1] == null ? material : fixedMaterial(parts[1], material, "mat " + parts[0]);
                return materialIngredient(parts[0], target, material);
            }
            case "pipe": {
                String[] parts = splitMaterial(argument);
                GTMaterial target = parts[1] == null ? material : fixedMaterial(parts[1], material, "pipe " + parts[0]);
                return pipeIngredient(parts[0], target, material);
            }
            case "wire": {
                String[] parts = splitMaterial(argument);
                GTMaterial target = parts[1] == null ? material : fixedMaterial(parts[1], material, "wire " + parts[0]);
                return wireIngredient(parts[0], target, material, false);
            }
            case "cabletier": {
                GTMaterial target = cableTierMaterial(argument);
                if (target == null) {
                    note("cable tier " + argument, "machine casing block");
                    return blockIngredient(BlockMaterialPrefix.casingMachine, material);
                }
                int sourceTier = Integer.parseInt(argument.trim());
                if (sourceTier == 2) return Map.of("tag", "gregtech:cable_01/any_copper");
                return wireIngredient("01", target, material, ElectricalCraftingRules.insulatedCable(sourceTier));
            }
            case "wiretier": {
                GTMaterial target = cableTierMaterial(argument);
                if (target == null) {
                    note("wire tier " + argument, "machine casing block");
                    return blockIngredient(BlockMaterialPrefix.casingMachine, material);
                }
                if (Integer.parseInt(argument.trim()) == 2) return Map.of("tag", "gregtech:wire_04/any_copper");
                return wireIngredient("04", target, material, false);
            }
            case "il":
                return componentIngredient(argument.trim(), tier, material);
            case "part":
                return partIngredient(argument.trim(), material);
            case "oredict":
                return oreDictIngredient(argument.trim(), material);
            case "circuit":
                return circuitIngredient(Integer.parseInt(argument.trim()));
            default:
                note(spec, "machine casing block");
                return blockIngredient(BlockMaterialPrefix.casingMachine, material);
        }
    }

    /** All resolved ingredients for one recipe, keyed by pattern symbol, in symbol order. */
    public static Map<Character, Object> resolveAll(Map<Character, String> keys, GTMaterial material, int tier) {
        Map<Character, Object> result = new LinkedHashMap<>();
        for (var entry : new java.util.TreeMap<>(keys).entrySet()) {
            result.put(entry.getKey(), resolve(entry.getValue(), material, tier));
        }
        return result;
    }

    // ── Kinds ─────────────────────────────────────────────────────────────────

    private static Object blockIngredient(BlockMaterialPrefix prefix, GTMaterial material) {
        var stack = com.gregtech.gregtech.registry.GTBlocks.getStack(prefix, material);
        if (!stack.isEmpty()) {
            return Map.of("item", ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        }
        note("block " + (prefix == null ? "?" : prefix.getName()) + " of " + material.getName(),
                "machine casing block");
        var casing = com.gregtech.gregtech.registry.GTBlocks.getStack(BlockMaterialPrefix.casingMachine, material);
        if (!casing.isEmpty()) {
            return Map.of("item", ForgeRegistries.ITEMS.getKey(casing.getItem()).toString());
        }
        return Map.of("item", "minecraft:iron_block");
    }

    private static Object materialIngredient(String prefixName, GTMaterial target, GTMaterial machineMaterial) {
        MaterialPrefix prefix = prefix(prefixName);
        if (prefix == null) {
            note("material prefix " + prefixName, "machine casing block");
            return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
        }
        List<String> chain = new ArrayList<>();
        chain.add(prefixName);
        chain.addAll(FALLBACKS.getOrDefault(prefixName, List.of()));

        for (int i = 0; i < chain.size(); i++) {
            MaterialPrefix candidate = prefix(chain.get(i));
            if (candidate == null || !candidate.isValidFor(target)) continue;
            if (!hasMaterialItem(candidate, target)) continue;
            if (i > 0) note(prefixName + " of " + target.getName(), chain.get(i) + " of " + target.getName());
            String path = MaterialEquivalence.tagPath(new MaterialEquivalence.Form(candidate, target));
            if (path != null) {
                return Map.of("tag", "forge:" + path);
            }
            return Map.of("tag", "gregtech:" + candidate.getRegistryName() + "/"
                    + MaterialEquivalence.materialName(target));
        }
        note(prefixName + " of " + target.getName(), "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    private static Object pipeIngredient(String sizeName, GTMaterial target, GTMaterial machineMaterial) {
        Item item = pipeItem(sizeName, target);
        if (item != null) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) return Map.of("item", id.toString());
        }
        note("pipe " + sizeName + " of " + target.getName(), "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    /**
     * Wire form of a specific material and size.
     * <p>
     * GT6 writes these keys as {@code OP.wireGt01.dat(ANY.Iron)} — the {@code ANY} family means
     * "any material of that group", so when the port has no wire of the requested material the
     * ingredient accepts every wire of that size instead of a made-up substitute.
     * </p>
     */
    private static Object wireIngredient(String size, GTMaterial target, GTMaterial machineMaterial, boolean insulated) {
        Item item = wireItem(size, target, insulated);
        if (item != null) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) return Map.of("item", id.toString());
        }
        List<Object> anyOfSize = wiresOfSize(size, insulated);
        if (!anyOfSize.isEmpty()) {
            note((insulated ? "cable " : "wire ") + size + " of " + target.getName(),
                    "any " + size + "x " + (insulated ? "cable" : "wire") + " (GT6 ANY family)");
            return anyOfSize.size() == 1 ? anyOfSize.get(0) : anyOfSize;
        }
        note((insulated ? "cable " : "wire ") + size + " of " + target.getName(), "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    private static Object componentIngredient(String arrayName, int tier, GTMaterial machineMaterial) {
        String itemPrefix = switch (arrayName) {
            case "MOTORS" -> "compact_electric_motor_";
            case "PUMPS" -> "compact_electric_pump_";
            case "PISTONS" -> "compact_electric_piston_";
            case "EMITTERS" -> "compact_signal_emitter_";
            case "CONVEYERS" -> "compact_electric_conveyor_";
            case "SENSORS" -> "compact_sensor_";
            case "ROBOT_ARMS" -> "compact_robot_arm_";
            case "FIELD_GENERATORS" -> "compact_force_field_emitter_";
            default -> null;
        };
        if (itemPrefix != null) {
            // GT6 IL.* component arrays are ULV-first: index 0 = ULV, 1 = LV, 2 = MV … Machine
            // recipes reference indices 1..5 (LV..IV), so a machine tier of t reads IL.*[t] — the
            // tier must NOT be shifted down, or tier 1 would ask for a ULV component that GT6
            // never uses in these recipes. See gregtech6-master .../Loaders/Loader_MachineRecipes
            // and MT/IL definitions (IL.MOTORS[1] = SteelGalvanized motor = LV).
            int index = Math.min(Math.max(tier, 1), IL_TIER_SUFFIX.length - 1);
            for (int i = index; i >= 1; i--) {
                var ingredient = byItemId("gregtech:" + itemPrefix + IL_TIER_SUFFIX[i]);
                if (ingredient != null) {
                    if (i != index) note("IL." + arrayName + "[" + tier + "]", itemPrefix + IL_TIER_SUFFIX[i]);
                    return ingredient;
                }
            }
        }
        note("IL." + arrayName + "[" + tier + "]", "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    /**
     * GT6 ore-dictionary keys that name a concrete port item.
     * <p>
     * {@code gt:re-batteryN} includes every rechargeable chemistry at that voltage,
     * not just the port's older generic battery item.
     * </p>
     */
    private static Object oreDictIngredient(String key, GTMaterial machineMaterial) {
        java.util.regex.Matcher battery = BATTERY_OREDICT.matcher(key);
        if (battery.matches()) {
            int tier = Integer.parseInt(battery.group(1));
            String[] tiers = {"ulv", "lv", "mv", "hv", "ev", "iv"};
            if (tier >= 0 && tier < tiers.length) {
                return Map.of("tag","gregtech:rechargeable_batteries/"+tiers[tier]);
            }
        }
        note("ore dictionary " + key, "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    /** GT6 multiblock part referenced by its original numeric registration id. */
    private static Object partIngredient(String idText, GTMaterial machineMaterial) {
        int id = Integer.parseInt(idText);
        var block = com.gregtech.gregtech.content.multiblock.LargeMachineParts.find(id);
        if (block != null && block.isPresent()) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(block.get().asItem());
            if (key != null) return Map.of("item", key.toString());
        }
        note("GT6 multiblock part " + id, "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    private static Object circuitIngredient(int tier) {
        return Map.of("tag", ElectricalCraftingRules.circuitTag(tier));
    }

    private static Object byItemId(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id.trim());
        if (key == null) return null;
        Item item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null || item == Items.AIR) return null;
        return Map.of("item", key.toString());
    }

    private static Object itemStackFallback(GTMaterial material) {
        return blockIngredient(BlockMaterialPrefix.casingMachine, material);
    }

    // ── Registry indexes ──────────────────────────────────────────────────────

    private static boolean hasMaterialItem(MaterialPrefix prefix, GTMaterial material) {
        if (materialItems == null) indexMaterialItems();
        return materialItems.getOrDefault(prefix, Map.of()).containsKey(material);
    }

    private static synchronized void indexMaterialItems() {
        if (materialItems != null) return;
        Map<MaterialPrefix, Map<GTMaterial, Item>> index = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof com.gregtech.gregtech.api.material.MaterialFormItem materialItem) {
                GTMaterial material = materialItem.getMaterial();
                if (material != null && material.isValid()) {
                    index.computeIfAbsent(materialItem.getPrefix(), k -> new HashMap<>()).put(material, item);
                }
            }
        }
        materialItems = index;
    }

    private static Item pipeItem(String sizeName, GTMaterial material) {
        if (pipesBySize == null) indexPipes();
        return pipesBySize.getOrDefault(sizeName.toLowerCase(java.util.Locale.ROOT), Map.of()).get(material);
    }

    private static synchronized void indexPipes() {
        if (pipesBySize != null) return;
        Map<String, Map<GTMaterial, Item>> index = new HashMap<>();
        for (var entry : GTFluidPipes.all()) {
            if (!entry.isPresent()) continue;
            FluidPipeBlock block = entry.get();
            PipeSpec spec = block.spec();
            index.computeIfAbsent(spec.size().name().toLowerCase(java.util.Locale.ROOT), k -> new HashMap<>())
                    .put(spec.material(), block.asItem());
        }
        // OP.pipeMedium/pipeLarge/pipeHuge also names the original item-pipe forms.
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof com.gregtech.gregtech.block.machine.ItemPipeBlock pipe
                    && !pipe.spec().size().restrictive()) {
                index.computeIfAbsent(pipe.spec().size().name().toLowerCase(java.util.Locale.ROOT), k -> new HashMap<>())
                        .putIfAbsent(pipe.spec().material(), item);
            }
        }
        pipesBySize = index;
    }

    private static Item wireItem(String size, GTMaterial material, boolean insulated) {
        if (wiresBySize == null) indexWires();
        // GT6 sizes are 1/2/4/8/16; registry ids are zero padded (wire_02_constantan).
        String key = (insulated ? "cable_" : "wire_") + String.format("%02d", Integer.parseInt(size));
        return wiresBySize.getOrDefault(key, Map.of()).get(material);
    }

    /** Every registered wire/cable of one size, in a stable order. */
    private static List<Object> wiresOfSize(String size, boolean insulated) {
        if (wiresBySize == null) indexWires();
        String key = (insulated ? "cable_" : "wire_") + String.format("%02d", Integer.parseInt(size));
        List<Object> result = new java.util.ArrayList<>();
        // GTMaterial is not Comparable, so order by the resolved item id instead.
        for (var entry : wiresBySize.getOrDefault(key, Map.of()).entrySet()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(entry.getValue());
            if (id != null) result.add(Map.of("item", id.toString()));
        }
        result.sort(java.util.Comparator.comparing(Object::toString));
        return result;
    }

    private static synchronized void indexWires() {
        if (wiresBySize != null) return;
        Map<String, Map<GTMaterial, Item>> index = new HashMap<>();
        for (var entry : GTWires.all()) {
            if (!entry.isPresent()) continue;
            ElectricWireBlock block = entry.get();
            var spec = block.spec();
            String key = (spec.insulated() ? "cable_" : "wire_") + String.format("%02d", spec.size());
            index.computeIfAbsent(key, k -> new HashMap<>()).put(spec.material(), block.asItem());
        }
        wiresBySize = index;
    }

    private static MaterialPrefix prefix(String name) {
        for (MaterialPrefix candidate : com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) {
            if (candidate.getName().equals(name)) return candidate;
        }
        return null;
    }

    private static BlockMaterialPrefix blockPrefix(String name) {
        MaterialPrefixes.Entry entry = MaterialPrefixes.all().get(name);
        return entry == null ? null : entry.getBlockDelegate();
    }

    private static GTMaterial material(String name) {
        GTMaterial resolved = GTMaterialRegistry.get(name);
        return resolved != null && resolved.isValid() ? resolved : null;
    }

    /** Fixed material referenced by the original recipe; falls back to the machine's own material. */
    private static GTMaterial fixedMaterial(String name, GTMaterial machineMaterial, String requested) {
        GTMaterial resolved = material(name);
        if (resolved == null) {
            note(requested + " (" + name + ")", machineMaterial.getName());
            return machineMaterial;
        }
        return resolved;
    }

    private static GTMaterial cableTierMaterial(String tierText) {
        return material(ElectricalCraftingRules.wireMaterial(Integer.parseInt(tierText.trim())));
    }

    private static String[] splitMaterial(String argument) {
        int at = argument.indexOf('@');
        if (at < 0) return new String[]{argument.trim(), null};
        return new String[]{argument.substring(0, at).trim(), argument.substring(at + 1).trim()};
    }

    // ── Diagnostics ───────────────────────────────────────────────────────────

    private static void note(String requested, String used) {
        SUBSTITUTIONS.putIfAbsent(requested, used);
    }

    /** Every ingredient substitution applied while building the recipe pack. */
    public static Map<String, String> substitutions() {
        return Map.copyOf(SUBSTITUTIONS);
    }

    /** Log the substitutions once, after the pack has generated its data. */
    public static synchronized void logSubstitutions() {
        if (validated) return;
        validated = true;
        if (SUBSTITUTIONS.isEmpty()) {
            GregTech.LOGGER.info("Machine crafting recipes: all original GT6 ingredients resolved exactly");
            return;
        }
        GregTech.LOGGER.warn("Machine crafting recipes: {} ingredient substitutions (port lacks the exact form):",
                SUBSTITUTIONS.size());
        SUBSTITUTIONS.forEach((requested, used) ->
                GregTech.LOGGER.warn("   {} -> {}", requested, used));
    }

    /** Test/DI hook: forget cached registry indexes. */
    public static synchronized void resetCaches() {
        materialItems = null;
        pipesBySize = null;
        wiresBySize = null;
        SUBSTITUTIONS.clear();
        validated = false;
    }

    /** Tag key helper for tests. */
    public static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    /** Convenience for tests: whether an ingredient object accepts the given stack. */
    public static boolean accepts(Object ingredient, ItemStack stack) {
        if (ingredient instanceof Map<?, ?> map) {
            Object item = map.get("item");
            if (item != null) {
                return stack.is(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(item.toString())));
            }
            Object tag = map.get("tag");
            if (tag != null) {
                return stack.is(itemTag(tag.toString()));
            }
            return false;
        }
        if (ingredient instanceof List<?> list) {
            for (Object option : list) {
                if (accepts(option, stack)) return true;
            }
        }
        return false;
    }
}
