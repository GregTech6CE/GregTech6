package gregtech.content.data;

import gregtech.content.data.BasicMachineCraftingRecipes.*;
import gregtech.content.material.Materials;
import gregtech.content.recipe.ElectricalCraftingRules;
import gregtech.engine.machine.PipeSpec;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.prefix.BlockMaterialPrefix;
//? if >=1.21.1 {
/*import gregtech.engine.energy.WireMaterialLike;
*///?} else {
import gregtech.content.block.energy.ElectricWireBlock;
//?}
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.platform.Mc;
import gregtech.content.registry.GTFluidPipes;
import gregtech.content.registry.GTWires;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Resolves typed basic-machine ingredients and legacy crafting-key expressions to recipe JSON.
 * <p>
 * Legacy spec grammar:
 * <ul>
 *   <li>{@code item:<id>} — a concrete item id.</li>
 *   <li>{@code tag:<id>} — a tag id (used for vanilla/ore-dictionary references).</li>
 *   <li>{@code <spec>;<spec>} — alternatives, e.g. a vanilla item plus the fluid-container form.</li>
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
        if (spec.indexOf(';') >= 0) {
            var alternatives = new ArrayList<Object>();
            for (var part : spec.split(";"))
                if (!part.isBlank()) alternatives.add(resolve(part, material, tier));
            return alternatives.size() == 1 ? alternatives.get(0) : alternatives;
        }
        int colon = spec.indexOf(':');
        if (colon < 0) return itemStackFallback(material);
        String kind = spec.substring(0, colon);
        String argument = spec.substring(colon + 1);
        switch (kind) {
            case "item": {
                return itemIngredient(argument.trim(), material);
            }
            case "tag":
                return Map.of("tag", argument.trim());
            case "container1000":
                return Map.of("type", "gt6ce:finite_fluid_container_1000", "fluid", argument.trim());
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
            case "cable": {
                String[] parts = splitMaterial(argument);
                GTMaterial target = parts[1] == null ? material : fixedMaterial(parts[1], material, "cable " + parts[0]);
                return wireIngredient(Integer.parseInt(parts[0]), target, material, true);
            }
            case "wire": {
                String[] parts = splitMaterial(argument);
                GTMaterial target = parts[1] == null ? material : fixedMaterial(parts[1], material, "wire " + parts[0]);
                return wireIngredient(Integer.parseInt(parts[0]), target, material, false);
            }
            case "cabletier":
                return tierWireIngredient(Integer.parseInt(argument.trim()), true, material);
            case "wiretier":
                return tierWireIngredient(Integer.parseInt(argument.trim()), false, material);
            case "il":
                return componentIngredient(componentType(argument.trim()), argument.trim(), tier, material);
            case "part":
                return partIngredient(Integer.parseInt(argument.trim()), material);
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

    /** Resolves a typed GT6 key with the machine's source casing. */
    public static Object resolve(Ingredient key, GTMaterial casing) {
        if (key instanceof Form form) {
            GTMaterial material = form.material() == null ? casing : form.material();
            if (form.group() != null) return groupIngredient(form.prefix(), form.group(), material, casing);
            return materialIngredient(form.prefix(), material, casing);
        }
        if (key instanceof BlockForm block) return blockIngredient(block.prefix(), block.material());
        if (key instanceof Casing block) return blockIngredient(block.prefix(), casing);
        if (key instanceof Pipe pipe) {
            GTMaterial material = pipe.material() == null ? casing : pipe.material();
            return pipeIngredient(pipe.size().name().toLowerCase(Locale.ROOT), material, casing);
        }
        if (key instanceof Wire wire) return wireIngredient(wire.size(), wire.material(), casing, false);
        if (key instanceof TierWire wire) return tierWireIngredient(wire.tier(), wire.cable(), casing);
        if (key instanceof Component component)
            return componentIngredient(component.type(), component.type().name(), component.tier(), casing);
        if (key instanceof Circuit circuit) return circuitIngredient(circuit.tier());
        if (key instanceof ItemRef item) return itemIngredient(item.id(), casing);
        if (key instanceof RegistryItem item) return itemIngredient(item.id(), casing);
        if (key instanceof Tool tool) return itemIngredient("gt6ce:tool_" + tool.type().id(), casing);
        if (key instanceof Crucible crucible) return itemIngredient("gt6ce:" + crucible.spec().id(), casing);
        if (key instanceof TagRef tag) return Map.of("tag", tag.id());
        if (key instanceof RegistryTag tag) return Map.of("tag", tag.id());
        if (key instanceof FluidContainer fluid)
            return Map.of("type", "gt6ce:finite_fluid_container_1000", "fluid", fluid.fluidField());
        if (key instanceof Alternatives alternatives)
            return alternatives.options().stream().map(option -> resolve(option, casing)).toList();
        throw new IllegalArgumentException("Unsupported basic machine ingredient: " + key);
    }

    // ── Kinds ─────────────────────────────────────────────────────────────────

    private static Object blockIngredient(BlockMaterialPrefix prefix, GTMaterial material) {
        var stack = gregtech.content.registry.GTBlocks.getStack(prefix, material);
        if (!stack.isEmpty()) {
            return Map.of("item", Mc.itemId(stack.getItem()).toString());
        }
        note("block " + (prefix == null ? "?" : prefix.getName()) + " of " + material.getName(),
                "machine casing block");
        var casing = gregtech.content.registry.GTBlocks.getStack(BlockMaterialPrefix.casingMachine, material);
        if (!casing.isEmpty()) {
            return Map.of("item", Mc.itemId(casing.getItem()).toString());
        }
        return Map.of("item", "minecraft:iron_block");
    }

    private static Object materialIngredient(String prefixName, GTMaterial target, GTMaterial machineMaterial) {
        MaterialPrefix itemPrefix = prefix(prefixName);
        if (itemPrefix == null) {
            note("material prefix " + prefixName, "machine casing block");
            return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
        }
        return materialIngredient(itemPrefix, target, machineMaterial);
    }

    private static Object materialIngredient(MaterialPrefix prefix, GTMaterial target, GTMaterial machineMaterial) {
        var ingredient = findMaterialIngredient(prefix, target);
        if (ingredient != null) return ingredient;
        note(prefix.getName() + " of " + target.getName(), "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    private static Object groupIngredient(MaterialPrefix prefix, GTMaterial group, GTMaterial representative,
                                          GTMaterial machineMaterial) {
        var members = new LinkedHashSet<GTMaterial>();
        members.add(representative);
        members.addAll(MaterialGroups.members(group));

        var choices = new LinkedHashSet<Object>();
        for (GTMaterial material : members) {
            var choice = findMaterialIngredient(prefix, material);
            if (choice != null) choices.add(choice);
        }
        if (choices.isEmpty()) return materialIngredient(prefix, representative, machineMaterial);
        return choices.size() == 1 ? choices.iterator().next() : List.copyOf(choices);
    }

    private static Object findMaterialIngredient(MaterialPrefix prefix, GTMaterial target) {
        // Original ore-dictionary aliases for vanilla forms are not generated GT material items.
        if (prefix == MaterialPrefix.plate && target.getName().equals("Glass"))
            return Map.of("item", "minecraft:glass_pane");
        if (prefix == MaterialPrefix.ingot && target.getName().equals("ClayBrick"))
            return Map.of("item", "minecraft:brick");
        var ingredient = registeredFormIngredient(prefix, target);
        if (ingredient != null) return ingredient;

        String prefixName = prefix.getName();
        for (String fallback : FALLBACKS.getOrDefault(prefixName, List.of())) {
            var candidate = prefix(fallback);
            if (candidate == null) continue;
            ingredient = registeredFormIngredient(candidate, target);
            if (ingredient != null) {
                note(prefixName + " of " + target.getName(), fallback + " of " + target.getName());
                return ingredient;
            }
        }
        return null;
    }

    private static Object registeredFormIngredient(MaterialPrefix prefix, GTMaterial material) {
        String path = MaterialEquivalence.tagPath(new MaterialEquivalence.Form(prefix, material));
        // Vanilla diamond has no standalone GT item when it is preferred by unification.
        if (!(prefix == MaterialPrefix.gem && material.resolve() == Materials.Diamond.resolve())
                && (!prefix.isValidFor(material) || !hasMaterialItem(prefix, material))) return null;
        if (path != null) return Map.of("tag", "forge:" + path);
        return Map.of("tag", "gt6ce:" + prefix.getRegistryName() + "/" + MaterialEquivalence.materialName(material));
    }

    private static Object pipeIngredient(String sizeName, GTMaterial target, GTMaterial machineMaterial) {
        if (sizeName.startsWith("any_") && target.resolve() == gregtech.content.material.Materials.Copper.resolve()) {
            var choices = new java.util.ArrayList<Object>();
            for (var material : gregtech.content.data.MaterialGroups.members(
                    gregtech.content.data.MaterialGroups.Cu)) {
                Item pipe = pipeItem(sizeName.substring(4), material);
                if (pipe != null) choices.add(Map.of("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pipe).toString()));
            }
            return choices.isEmpty() ? null : choices;
        }
        Item item = pipeItem(sizeName, target);
        if (item != null) {
            ResourceLocation id = Mc.itemId(item);
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
    private static Object wireIngredient(int size, GTMaterial target, GTMaterial machineMaterial, boolean insulated) {
        Item item = wireItem(size, target, insulated);
        if (item != null) {
            ResourceLocation id = Mc.itemId(item);
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

    private static Object tierWireIngredient(int tier, boolean cable, GTMaterial machineMaterial) {
        GTMaterial target = material(ElectricalCraftingRules.wireMaterial(tier));
        if (target == null) {
            note((cable ? "cable tier " : "wire tier ") + tier, "machine casing block");
            return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
        }
        if (tier == 2) return Map.of("tag", cable ? "gt6ce:cable_01/any_copper" : "gt6ce:wire_04/any_copper");
        return wireIngredient(cable ? 1 : 4, target, machineMaterial,
                cable && ElectricalCraftingRules.insulatedCable(tier));
    }

    private static ComponentType componentType(String arrayName) {
        for (ComponentType type : ComponentType.values()) {
            if (type.name().equals(arrayName)) return type;
        }
        return null;
    }

    private static Object componentIngredient(ComponentType type, String arrayName, int tier,
                                               GTMaterial machineMaterial) {
        if (type != null) {
            String itemPrefix = type.itemPrefix();
            // GT6 IL.* arrays are ULV-first; recipe tier 1 asks for index 1 (LV).
            int index = Math.min(Math.max(tier, 1), IL_TIER_SUFFIX.length - 1);
            for (int i = index; i >= 1; i--) {
                var ingredient = byItemId("gt6ce:" + itemPrefix + IL_TIER_SUFFIX[i]);
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
                return Map.of("tag","gt6ce:rechargeable_batteries/"+tiers[tier]);
            }
        }
        note("ore dictionary " + key, "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    /** GT6 multiblock part referenced by its original numeric registration id. */
//? if >=1.21.1 {
    /*private static Object partIngredient(int id,GTMaterial material){
        note("GT6 multiblock part "+id+" (Neo factory pending)","machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine,material);
*///?} else {
    private static Object partIngredient(int id, GTMaterial machineMaterial) {
        var block = gregtech.content.multiblock.LargeMachineParts.find(id);
        if (block != null && block.isPresent()) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(block.get().asItem());
            if (key != null) return Map.of("item", key.toString());
        }
        note("GT6 multiblock part " + id, "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
//?}
    }

    private static Object circuitIngredient(int tier) {
        return Map.of("tag", ElectricalCraftingRules.circuitTag(tier));
    }

    private static Object itemIngredient(String id, GTMaterial machineMaterial) {
        var ingredient = byItemId(id);
        if (ingredient != null) return ingredient;
        note("item " + id, "machine casing block");
        return blockIngredient(BlockMaterialPrefix.casingMachine, machineMaterial);
    }

    private static Object byItemId(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id.trim());
        if (key == null) return null;
        if (key.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) key = ResourceLocation.fromNamespaceAndPath("gt6ce",
                key.getPath());
        Item item = Mc.item(key);
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
        for (Item item : Mc.items()) {
            if (item instanceof gregtech.engine.material.MaterialFormItem materialItem) {
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
            if (!entry.isBound()) continue;
            FluidPipeBlock block = entry.get();
            PipeSpec spec = block.spec();
            index.computeIfAbsent(spec.size().name().toLowerCase(java.util.Locale.ROOT), k -> new HashMap<>())
                    .put(spec.material(), block.asItem());
        }
        // OP.pipeMedium/pipeLarge/pipeHuge also names the original item-pipe forms.
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof gregtech.content.block.machine.ItemPipeBlock pipe
                    && !pipe.spec().size().restrictive()) {
                index.computeIfAbsent(pipe.spec().size().name().toLowerCase(java.util.Locale.ROOT), k -> new HashMap<>())
                        .putIfAbsent(pipe.spec().material(), item);
            }
        }
        pipesBySize = index;
    }

    private static Item wireItem(int size, GTMaterial material, boolean insulated) {
        if (wiresBySize == null) indexWires();
        // GT6 sizes are 1/2/4/8/16; registry ids are zero padded (wire_02_constantan).
        String key = (insulated ? "cable_" : "wire_") + String.format(Locale.ROOT, "%02d", size);
        return wiresBySize.getOrDefault(key, Map.of()).get(material);
    }

    /** Every registered wire/cable of one size, in a stable order. */
    private static List<Object> wiresOfSize(int size, boolean insulated) {
        if (wiresBySize == null) indexWires();
        String key = (insulated ? "cable_" : "wire_") + String.format(Locale.ROOT, "%02d", size);
        List<Object> result = new java.util.ArrayList<>();
        // GTMaterial is not Comparable, so order by the resolved item id instead.
        for (var entry : wiresBySize.getOrDefault(key, Map.of()).entrySet()) {
            ResourceLocation id = Mc.itemId(entry.getValue());
            if (id != null) result.add(Map.of("item", id.toString()));
        }
        result.sort(java.util.Comparator.comparing(Object::toString));
        return result;
    }

//? if >=1.21.1 {
    /*private static synchronized void indexWires(){
        if (wiresBySize!=null)return;Map<String,Map<GTMaterial,Item>> index=new HashMap<>();
        for (var block:BuiltInRegistries.BLOCK)if (block instanceof WireMaterialLike wire){var spec=wire.spec();String key=(spec.insulated()?"cable_":"wire_")+String.format("%02d",spec.size());index.computeIfAbsent(key,k->new HashMap<>()).put(spec.material(),block.asItem());}
        wiresBySize=index;
*///?} else {
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
//?}
    }

    private static MaterialPrefix prefix(String name) {
        for (MaterialPrefix candidate : gregtech.engine.prefix.PrefixRegistry.all()) {
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
            Mc.logger().info("Machine crafting recipes: all original GT6 ingredients resolved exactly");
            return;
        }
        Mc.logger().warn("Machine crafting recipes: {} ingredient substitutions (port lacks the exact form):",
                SUBSTITUTIONS.size());
        SUBSTITUTIONS.forEach((requested, used) ->
                Mc.logger().warn("   {} -> {}", requested, used));
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
                return stack.is(Mc.item(ResourceLocation.parse(item.toString())));
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
