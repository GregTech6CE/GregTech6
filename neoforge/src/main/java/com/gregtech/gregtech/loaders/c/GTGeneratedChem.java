package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTechnological;
import com.gregtech.gregtech.util.OM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime resolver for the transpiled GT6 chemistry data ({@link GTChemGen}).
 *
 * <p>Recipes whose materials/fluids are absent in the port are skipped and
 * counted — GT6 behaved the same way for missing cross-mod content. The
 * residual skips reference content the port deliberately does not register
 * (cross-mod compounds such as Desh, or chemistry-only intermediates such as
 * CrO2); per the Wave 35 decision these are accepted as permanently missing
 * rather than back-filled (B8). {@link #loadAll()} logs the exact unresolved
 * tokens each run so the set stays auditable instead of being a silent number.</p>
 */
public final class GTGeneratedChem {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, RecipeMap> MAPS = new HashMap<>();
    private static final Map<String, MaterialPrefix> PREFIXES = new HashMap<>();
    /** Distinct content tokens (material/fluid, amount stripped) that failed to resolve. */
    private static final java.util.Set<String> MISSING = new java.util.TreeSet<>();
    private static int added, skipped;

    private GTGeneratedChem() {}

    /**
     * Entry point — called from Loader_Recipes_Chem after the core chains.
     * <p>
     * Every transpiled GT6 loader has its own generated class; they all feed the same resolver, so
     * a recipe that names content this port does not register is skipped and recorded instead of
     * being guessed at.
     * </p>
     */
    public static void loadAll() {
        added = 0;
        skipped = 0;
        MISSING.clear();
        SET_STATS.clear();
        load("chem", GTChemGen::load);
        load("other", GTOtherGen::load);
        load("potions", GTPotionGen::load);
        load("food", GTFoodGen::load);
        load("extruder", GTExtruderGen::load);
        load("vanilla", GTVanillaGen::load);
        load("temporary", GTTemporaryGen::load);
        load("ores", GTOresGen::load);
        load("combs", GTCombGen::load);
        LOGGER.info("[gregtech] Transpiled GT6 recipes: {} added, {} skipped (missing content)", added, skipped);
        if (!MISSING.isEmpty()) {
            LOGGER.info("[gregtech] accepted-missing content ({}): {}",
                    MISSING.size(), String.join(", ", MISSING));
        }
    }

    /** One transpiled GT6 loader's outcome, for the coverage tests and the round reports. */
    public record SetStats(String set, int added, int skipped) {}

    private static final java.util.List<SetStats> SET_STATS = new java.util.ArrayList<>();

    /** Per-loader transpile results of {@link #loadAll()}. */
    public static java.util.List<SetStats> setStats() {
        return java.util.List.copyOf(SET_STATS);
    }

    private static void load(String set, Runnable loader) {
        int beforeAdded = added;
        int beforeSkipped = skipped;
        GeneratedRecipeSink.emit(GTGeneratedChem::register,loader);
        SET_STATS.add(new SetStats(set, added - beforeAdded, skipped - beforeSkipped));
    }

    /** Record the failing spec (trailing amount stripped) for the audit summary. */
    private static void recordMissing(String spec) {
        int i = spec.lastIndexOf(':');
        MISSING.add(i > 0 ? spec.substring(0, i) : spec);
    }

    /** Transpiled GT6 chemistry recipes that were registered. */
    public static int addedRecipes() { return added; }

    /** Transpiled recipes skipped because their content does not exist in this port. */
    public static int skippedRecipes() { return skipped; }

    /** Distinct content tokens (material/fluid/prefix, amount stripped) that failed to resolve. */
    public static java.util.Set<String> missingContent() {
        return java.util.Collections.unmodifiableSet(MISSING);
    }

    static void register(String mapName, long eut, long dur, long[] chances,
                         String[] itemIn, String[] fluidIn, String[] fluidOut, String[] itemOut) {
        RecipeMap map = map(mapName);
        if (map == null) { skipped++; LOGGER.debug("[gregtech] chem skip (no map): {}", mapName); return; }

        List<ItemStack> inputs = resolveItems(itemIn);
        List<FluidStack> fIn = resolveFluids(fluidIn);
        List<FluidStack> fOut = resolveFluids(fluidOut);
        List<ItemStack> outputs = resolveItems(itemOut);
        if (inputs == null || fIn == null || fOut == null || outputs == null) {
            skipped++;
            LOGGER.debug("[gregtech] chem skip ({}): in={} fIn={} fOut={} out={}", mapName,
                    inputs == null ? String.join("|", itemIn) : "ok",
                    fIn == null ? String.join("|", fluidIn) : "ok",
                    fOut == null ? String.join("|", fluidOut) : "ok",
                    outputs == null ? String.join("|", itemOut) : "ok");
            return;
        }

        var candidate = new com.gregtech.gregtech.api.recipe.Recipe(
                inputs.toArray(RecipeMap.ZL_IS), outputs.toArray(RecipeMap.ZL_IS),
                null, chances, fIn.toArray(RecipeMap.ZL_FS), fOut.toArray(RecipeMap.ZL_FS), dur, eut, 0);
        for (int index = 0; index < itemIn.length; index++) {
            String[] token = itemIn[index].split(":");
            // GT6 marks catalyst inputs with a count of 0 (ST.amount(0, x), IL.X.get(0)):
            // they gate the recipe without being consumed.
            boolean catalyst = token.length == 4 && token[0].equals("i") && token[3].equals("0")
                    || token.length == 3 && token[0].equals("tech") && token[2].equals("0")
                    || token.length == 3 && token[0].equals("v") && token[2].equals("0")
                    || token.length == 3 && token[0].equals("cell") && token[2].equals("0");
            if (catalyst) candidate.withCatalystInputs(index);
        }
        var recipe = map.addRecipe(candidate);
        if (recipe != null) added++;
        else skipped++;
    }

    /**
     * Resolve one transpiled item spec ({@code i:<prefix>:<material>:<count>},
     * {@code tech:<item>:<count>}, {@code v:<vanilla item>:<count>}, …) to a stack.
     *
     * <p>Public so other generated data can reuse the same grammar — the GT loot rows
     * ({@code GTLootGen}) and their tests do.</p>
     *
     * @return the stack, or {@code null} when the spec cannot be resolved
     */
    public static ItemStack resolveSpec(String spec) {
        return resolveItem(spec);
    }

    /**
     * Resolves a fluid spec ({@code m:<material>:<mb>}, {@code f:<registry name>:<mb>}, {@code w:<mb>})
     * for the hand-ported packs that reuse this loader's grammar (e.g. {@code GTMainRecipes}).
     *
     * @return the fluid, or null when the referenced content is absent
     */
    public static FluidStack resolveFluidSpec(String spec) {
        return resolveFluid(spec);
    }

    private static List<ItemStack> resolveItems(String[] specs) {
        List<ItemStack> out = new ArrayList<>();
        for (String spec : specs) {
            ItemStack stack = resolveItem(spec);
            if (stack == null || stack.isEmpty()) { recordMissing(spec); return null; }
            out.add(stack);
        }
        return out;
    }

    /** @return resolved stack, or null/empty when the referenced content is absent. */
    private static ItemStack resolveItem(String spec) {
        String[] parts = spec.split(":");
        switch (parts[0]) {
            case "omd" -> {
                GTMaterial material = material(parts[1]);
                if (material == null) return null;
                long amount = Long.parseLong(parts[2]) * GTValues.U / 1000;
                return OM.dust(material, amount);
            }
            case "i" -> {
                GTMaterial material = "Empty".equals(parts[2]) && prefix(parts[1]) != null
                        && prefix(parts[1]).hasEmptyAmmunitionForm()
                        ? com.gregtech.gregtech.content.material.Materials.Empty : material(parts[2]);
                if (material == null) return null;
                // Keep a visible stack; register() separately marks GT6 count-zero inputs as catalysts.
                int count = Math.max(1, Integer.parseInt(parts[3]));
                if ("blockDust".equals(parts[1])) {
                    // the port has no dust-block prefix -> 9 dusts worth
                    return OM.dust(material, 9L * count * GTValues.U);
                }
                MaterialPrefix prefix = prefix(parts[1]);
                if (prefix == null) {
                    // GT6's block forms (OP.blockGem/blockIngot/blockPlate/…, crateGt*) are blocks in the
                    // port (BlockPrefixRegistry), so an item-prefix miss falls through to them instead of
                    // dropping the recipe row.
                    com.gregtech.gregtech.api.prefix.BlockMaterialPrefix block =
                            com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.byName(parts[1]);
                    if (block == null) return null;
                    ItemStack stack = com.gregtech.gregtech.registry.GTBlocks.getStack(block, material);
                    if (stack.isEmpty()) return null;
                    ItemStack out = stack.copy();
                    out.setCount(Math.min(count, out.getMaxStackSize()));
                    return out;
                }
                return MaterialStackItemHelper.mat(prefix, material, count);
            }
            // GT6 written books (`ST.book("Manual_Printer", …)`, Loader_Books) — content/book/GTBooks.
            case "book" -> {
                return com.gregtech.gregtech.content.book.GTBooks.bookStack(parts[1]);
            }
            case "tag" -> {
                Item tag = GTTechnological.selectorTag(Integer.parseInt(parts[1]));
                return tag == null ? null : new ItemStack(tag);
            }
            case "v" -> {
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", parts[1]));
                return (item == null || item == net.minecraft.world.item.Items.AIR)
                        ? null : new ItemStack(item, Integer.parseInt(parts[2]));
            }
            // GT6 fluid cells: represented by the registered FluidItems
            // (1000 mB unit), e.g. "cell:Water:2"
            case "cell" -> {
                var fluidItem = com.gregtech.gregtech.registry.GTFluidItems.get(parts[1]);
                return fluidItem == null ? null : new ItemStack(fluidItem, Integer.parseInt(parts[2]));
            }
            // GT6 technological items (IL.<Name>), e.g. "tech:extruder_shape_plate:1".
            // PLenty of IL.* names are not technological items: the generator modules and the food /
            // clay items are multi-items registered under their display-name id, and a few map onto
            // vanilla items, so the alias table is consulted and then the item registry (which also
            // accepts the "minecraft:" namespace).
            case "tech" -> {
                String id = com.gregtech.gregtech.registry.GTTechnological.ALIASES
                        .getOrDefault(parts[1], parts[1]);
                var techItem = com.gregtech.gregtech.registry.GTTechnological.get(parts[1]);
                if (techItem == null) {
                    var key = id.indexOf(':') >= 0
                            ? net.minecraft.resources.ResourceLocation.tryParse(id)
                            : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    "gregtech", id);
                    techItem = key == null ? null
                            : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
                }
                return techItem == null || techItem == net.minecraft.world.item.Items.AIR
                        ? null : new ItemStack(techItem, Math.max(1, Integer.parseInt(parts[2])));
            }
            default -> { return null; }
        }
    }

    private static List<FluidStack> resolveFluids(String[] specs) {
        List<FluidStack> out = new ArrayList<>();
        for (String spec : specs) {
            FluidStack stack = resolveFluid(spec);
            if (stack == null) { recordMissing(spec); return null; }
            out.add(stack);
        }
        return out;
    }

    /** @return resolved fluid, or null when the referenced content is absent. */
    private static FluidStack resolveFluid(String spec) {
        String[] parts = spec.split(":");
        return switch (parts[0]) {
            case "w" -> new FluidStack(Fluids.WATER, Integer.parseInt(parts[1]));
            case "f" -> named(parts[1], Integer.parseInt(parts[2]));
            case "m" -> materialFluid(parts[1], Integer.parseInt(parts[2]));
            case "fr" -> byRegistryName(parts[1], Integer.parseInt(parts[2]));
            default -> null;
        };
    }

    /** field key by raw 1.7.10 registry name ("nitrofuel", "gas.natural", ...). */
    private static volatile Map<String, String> BY_REGISTRY_NAME;

    static FluidStack byRegistryName(String registryName, int mb) {
        Map<String, String> index = BY_REGISTRY_NAME;
        if (index == null) {
            synchronized (GTGeneratedChem.class) {
                if (BY_REGISTRY_NAME == null) {
                    Map<String, String> map = new HashMap<>();
                    for (Map.Entry<String, RegisteredFluids.FluidEntry> entry : RegisteredFluids.all().entrySet()) {
                        map.putIfAbsent(entry.getValue().registryName().toLowerCase(java.util.Locale.ROOT), entry.getKey());
                    }
                    // GT6's 1.7.10 registry names that the port spells with an underscore. The tokens
                    // are read from GT6's own rows, so the old spelling has to keep working (§32).
                    for (String[] alias : new String[][]{
                            {"chocolatemilk", "ChocolateMilk"},
                            {"darkchocolatemilk", "ChocolateMilk_Dark"},
                    }) {
                        map.putIfAbsent(alias[0], alias[1]);
                    }
                    BY_REGISTRY_NAME = map;
                }
                index = BY_REGISTRY_NAME;
            }
        }
        String key = index.get(registryName.toLowerCase(java.util.Locale.ROOT));
        return key == null ? null : named(key, mb);
    }

    private static FluidStack named(String key, int mb) {
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get(key);
        if (entry == null) return null;
        // Water/Lava declarations reuse the vanilla fluids; Loader_Fluids registers no still/flowing
        // pair for them, so the vanilla stacks have to be returned here (same as GTFluids.stack).
        if (entry.textureMode() == RegisteredFluids.FluidTextureMode.VANILLA_WATER)
            return new FluidStack(Fluids.WATER, mb);
        if (entry.textureMode() == RegisteredFluids.FluidTextureMode.VANILLA_LAVA)
            return new FluidStack(Fluids.LAVA, mb);
        DeferredHolder<Fluid,? extends Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isBound()) return null;
        return new FluidStack(fluid.get(), mb);
    }

    /** Material fluid: generated gas/liquid (1U=1000mB) or molten (1U=144mB) or named. */
    public static FluidStack materialFluid(String materialName, int mb) {
        GTMaterial material = material(materialName);
        String name = material != null ? material.getName() : materialName;
        FluidStack stack = named("GenGas_" + name, mb);
        if (stack != null) return stack;
        stack = named("GenLiquid_" + name, mb);
        if (stack != null) return stack;
        stack = named(name, mb);
        if (stack != null) return stack;
        // dedicated fluids registered under their registry names skip the
        // GenGas_/GenLiquid_ keys — fall back to the registry-name forms
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        stack = byRegistryName("liquid." + lower, mb);
        if (stack != null) return stack;
        stack = byRegistryName("gas." + lower, mb);
        if (stack != null) return stack;
        stack = byRegistryName(lower, mb);
        if (stack != null) return stack;
        // molten scale: U amounts in mB-of-gas units -> 144/1000 for molten metals
        return named("GenMolten_" + name, Math.max(1, mb * 144 / 1000));
    }

    static GTMaterial material(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        if (material != null && material.resolve().isValid()) return material.resolve();
        // GT6 field names (TiCl4, HNO3, ...) often differ from the registry
        // display names ("Titanium Tetrachloride") — fall back to the MT field
        try {
            Object value = com.gregtech.gregtech.data.ImportedMaterialData.class.getField(name).get(null);
            if (value instanceof GTMaterial mt && mt.resolve().isValid()) return mt.resolve();
        } catch (ReflectiveOperationException ignored) {
            // no such field — genuinely unknown material
        }
        return null;
    }

    private static MaterialPrefix prefix(String name) {
        if (PREFIXES.isEmpty()) {
            for (MaterialPrefix prefix : com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) {
                PREFIXES.put(prefix.getName(), prefix);
            }
        }
        return PREFIXES.get(name);
    }

    private static RecipeMap map(String fieldName) {
        if (MAPS.isEmpty()) {
            for (Field field : MachineRecipeMaps.class.getFields()) {
                if (RecipeMap.class.isAssignableFrom(field.getType())) {
                    try {
                        MAPS.put(field.getName(), (RecipeMap) field.get(null));
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        }
        return MAPS.get(fieldName);
    }
}
