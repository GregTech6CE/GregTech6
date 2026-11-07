package gregtech.content.recipe.generated;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.util.OM;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.HashMap;
import java.util.Map;

/**
 * Source-spec utilities shared by the portable, hand-written recipe families.
 *
 * <p>The transpiled loader tables no longer produce rows here: every one of them is transcribed
 * as typed {@link gregtech.engine.recipe.chem.ChemRecipe} rows and published by
 * {@link ChemRecipeResolver}. What remains are the small lookups the hand-written families use
 * for one-off source expressions, such as a loot row's single item spec.</p>
 */
public final class GeneratedRecipeResolver {
    private static final Map<String, MaterialPrefix> PREFIXES = new HashMap<>();

    private GeneratedRecipeResolver() {}

    /**
     * Resolve one source item spec ({@code i:<prefix>:<material>:<count>},
     * {@code tech:<item>:<count>}, {@code v:<vanilla item>:<count>}, ...) to a stack.
     *
     * @return the stack, or {@code null} when the spec cannot be resolved
     */
    public static ItemStack resolveSpec(String spec) {
        return resolveItem(spec);
    }

    /**
     * Resolves a fluid spec ({@code m:<material>:<mb>}, {@code f:<registry name>:<mb>}, {@code w:<mb>})
     * for the hand-ported packs that reuse this loader's grammar.
     *
     * @return the fluid, or null when the referenced content is absent
     */
    public static FluidStack resolveFluidSpec(String spec) {
        return resolveFluid(spec);
    }

    /** Additive exact original-U dust token. Legacy omd thousandths and item counts are unchanged.
     * The existing OM selector retains fallback forms; malformed/negative/overflowing quantities
     * are unsupported rather than rounded to one item. Source CS.U is 648648000.
     */
    private static ItemStack sourceDust(String[] parts) {
        if (parts.length != 3 || parts[1].isEmpty() || !parts[2].matches("[1-9][0-9]*")) return null;
        final long sourceAmount;
        try { sourceAmount = Long.parseLong(parts[2]); }
        catch (NumberFormatException invalidAmount) { return null; }
        var ratio = java.math.BigInteger.valueOf(sourceAmount)
                .multiply(java.math.BigInteger.valueOf(GTValues.U))
                .divideAndRemainder(java.math.BigInteger.valueOf(648648000L));
        // OM.dust can multiply a fractional amount by72. Keep its long arithmetic bounded.
        if (ratio[1].signum() != 0 || ratio[0].signum() <= 0
                || ratio[0].compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE / 72)) > 0) return null;
        GTMaterial material = material(parts[1]);
        return material == null ? null : OM.dust(material, ratio[0].longValueExact());
    }

    /** @return resolved stack, or null/empty when the referenced content is absent. */
    private static ItemStack resolveItem(String spec) {
        String[] parts = spec.split(":");
        switch (parts[0]) {
            case "sci" -> { return sourceCentrifugeItem(spec.split(":", -1)); }
            case "omdu" -> { return sourceDust(spec.split(":", -1)); }
            case "omd" -> {
                GTMaterial material = material(parts[1]);
                if (material == null) return null;
                long amount = Long.parseLong(parts[2]) * GTValues.U / 1000;
                return OM.dust(material, amount);
            }
            case "i" -> {

                GTMaterial material = "Empty".equals(parts[2]) && prefix(parts[1]) != null
                        && prefix(parts[1]).hasEmptyAmmunitionForm()
                        ? gregtech.content.material.Materials.Empty : material(parts[2]);

                if (material == null) return null;
                // Keep a visible stack; the typed resolver marks GT6 count-zero inputs as catalysts.
                int count = Math.max(1, Integer.parseInt(parts[3]));
                MaterialPrefix prefix = prefix(parts[1]);
                if (prefix == null) {
                    // GT6's block forms (OP.blockGem/blockIngot/blockPlate/...) are blocks in the port
                    // (BlockPrefixRegistry), so an item-prefix miss falls through to them instead of
                    // dropping the recipe row.
                    gregtech.engine.prefix.BlockMaterialPrefix block =
                            gregtech.engine.prefix.BlockPrefixRegistry.byName(parts[1]);
                    if (block == null) return null;
                    ItemStack stack = gregtech.content.registry.GTBlocks.getStack(block, material);
                    if (stack.isEmpty()) return null;
                    ItemStack out = stack.copy();
                    out.setCount(Math.min(count, out.getMaxStackSize()));
                    return out;
                }
                return MaterialStackItemHelper.mat(prefix, material, count);
            }
            // GT6 written books (`ST.book("Manual_Printer", ...)`, Loader_Books) — content/book/GTBooks.
            case "book" -> {
                return gregtech.content.book.GTBooks.bookStack(parts[1]);
            }
            case "tag" -> {
                Item tag = GTTechnological.selectorTag(Integer.parseInt(parts[1]));
                return tag == null ? null : new ItemStack(tag);
            }
            case "v" -> {
//? if >=1.21.1 {
                /*Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", parts[1]));
*///?} else {
                Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", parts[1]));
//?}
                return (item == null || item == net.minecraft.world.item.Items.AIR)
                        ? null : new ItemStack(item, Integer.parseInt(parts[2]));
            }
            // GT6 fluid cells: represented by the registered FluidItems (1000 mB unit).
            case "cell" -> {
                var fluidItem = gregtech.content.registry.GTFluidItems.get(parts[1]);
                return fluidItem == null ? null : new ItemStack(fluidItem, Integer.parseInt(parts[2]));
            }
            // GT6 technological items (IL.<Name>), e.g. "tech:extruder_shape_plate:1".
            case "tech" -> {
                String id = gregtech.content.registry.GTTechnological.ALIASES
                        .getOrDefault(parts[1], parts[1]);
                var techItem = gregtech.content.registry.GTTechnological.get(parts[1]);
                if (techItem == null) {
//? if >=1.21.1 {
                    /*var key = id.indexOf(':') >= 0
                            ? net.minecraft.resources.ResourceLocation.tryParse(id)
                            : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    "gt6ce", id);
*///?} else {
                    var key = id.indexOf(':') >= 0
                            ? net.minecraft.resources.ResourceLocation.tryParse(id)
                            : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    gregtech.GregTech.NAMESPACE, id);
//?}
//? if >=1.21.1 {
                    /*techItem = key == null ? null
                            : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
*///?} else {
                    techItem = key == null ? null
                            : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(key);
//?}
                }
                return techItem == null || techItem == net.minecraft.world.item.Items.AIR
                        ? null : new ItemStack(techItem, Math.max(1, Integer.parseInt(parts[2])));
            }
            default -> { return null; }
        }
    }

    /** Reviewed source defaults for the three pinned centrifuge item selectors. */
    private static ItemStack sourceCentrifugeItem(String[] parts) {
        if (parts.length != 4 || !parts[3].matches("[1-9][0-9]*")) return null;
        final int count;
        try { count = Integer.parseInt(parts[3]); }
        catch (NumberFormatException invalid) { return null; }
        var binding = gregtech.content.recipe.SourceCentrifugeItemOutputs.binding(parts[1], parts[2]);
        String fallback = gregtech.content.recipe.SourceCentrifugeItemOutputs.fallback(binding, count);
        if (fallback == null) return null;
        GTMaterial material = material(binding.materialName());
        if (material == null) return null;
        ItemStack target = resolveItem("v:" + binding.vanillaPath() + ":" + count);
        var form = target == null ? null : gregtech.engine.material.MaterialEquivalence.form(target);
        boolean core = material.isValid() && material.resolve() == material
                && GTMaterialRegistry.isCoreMaterial(material)
                && !gregtech.engine.addon.AddonMaterials.owns(material);
        boolean targetMatches = form != null && form.material().resolve() == material
                && form.prefix().getName().equals(binding.prefixName());
        boolean external = externalForm(binding);
        return gregtech.content.recipe.SourceCentrifugeItemOutputs.useDefault(binding,
                material.getId(), material.getName(), core, targetMatches, external)
                ? target : resolveItem(fallback);
    }

    /** A foreign direct material form is public identity even without an explicit composition. */
    private static boolean externalForm(gregtech.content.recipe.SourceCentrifugeItemOutputs.Binding binding) {
        if (gregtech.engine.addon.AddonMaterials.acceptedForms().stream().anyMatch(f ->
                f.materialName().equals(binding.materialName()) && f.prefixName().equals(binding.prefixName())))
            return true;
        GTMaterial material = material(binding.materialName());
        if (material == null) return false;
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (id == null || id.getNamespace().equals("gt6ce") || id.getNamespace().equals("minecraft")) continue;
            if (item instanceof gregtech.engine.material.MaterialFormItem direct
                    && direct.getPrefix() != null && direct.getMaterial() != null
                    && direct.getPrefix().getName().equals(binding.prefixName())
                    && direct.getMaterial().resolve() == material) return true;
        }
        for (var entry : gregtech.engine.material.ItemMaterialRegistry.entries().entrySet()) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id == null || id.getNamespace().equals("gt6ce") || id.getNamespace().equals("minecraft")) continue;
            var declaredForm = gregtech.engine.material.MaterialEquivalence.form(new ItemStack(entry.getKey()));
            if (declaredForm != null && declaredForm.prefix().getName().equals(binding.prefixName())
                    && declaredForm.material().resolve() == material) return true;
            var explicit = entry.getValue();
            if ((explicit.prefix() == null || explicit.prefix().getName().equals(binding.prefixName()))
                    && explicit.components().stream().anyMatch(c -> c.material().resolve() == material))
                return true;
        }
        return false;
    }

    /** @return resolved fluid, or null when the referenced content is absent. */
    private static FluidStack resolveFluid(String spec) {
        String[] parts = spec.split(":");
        return switch (parts[0]) {
            case "w" -> new FluidStack(Fluids.WATER, Integer.parseInt(parts[1]));
            case "f" -> named(parts[1], Integer.parseInt(parts[2]));
            case "m" -> materialFluid(parts[1], Integer.parseInt(parts[2]));
            case "mu" -> sourceMaterialFluid(spec.split(":", -1));
            case "fr" -> byRegistryName(parts[1], Integer.parseInt(parts[2]));
            default -> null;
        };
    }

    /** Additive original-U grammar; legacy m tokens and public materialFluid remain unchanged. */
    private static FluidStack sourceMaterialFluid(String[] parts) {
        if (parts.length != 5 || !parts[3].matches("[0-9]+")
                || !("up".equals(parts[4]) || "down".equals(parts[4]))) return null;
        long sourceAmount;
        try {
            sourceAmount = Long.parseLong(parts[3]);
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
        GTMaterial material = material(parts[1]);
        if (material == null || !material.resolve().isValid()) return null;
        material = material.resolve();
        var binding = gregtech.content.recipe.OriginalMaterialFluidUnits
                .binding(material.getName(), material.getId(), parts[2]);
        if (binding == null) return null;
        int amount = gregtech.content.recipe.OriginalMaterialFluidUnits.millibuckets(
                sourceAmount, binding.referenceSourceUnits(), binding.referenceMillibuckets(),
                "up".equals(parts[4]));
        // None of the migrated source calls is a zero-volume catalyst. Do not invent 1 mB.
        if (amount <= 0) return null;
        var entry = RegisteredFluids.get(binding.fluidField());
        if (entry == null || !binding.registryName().equals(entry.registryName())
                || !"gt6ce".equals(GTFluids.namespace(entry))) return null;
        String boundName = entry.materialKey() != null ? entry.materialKey()
                : RegisteredFluids.boundMaterial(entry.registryName());
        if (boundName == null) return null;
        GTMaterial bound = GTMaterialRegistry.get(boundName).resolve();
        if (!bound.isValid() || bound.getId() != binding.materialId()
                || !bound.getName().equals(binding.materialName())) return null;
        FluidStack stack = named(binding.fluidField(), amount);
        if (stack == null || stack.isEmpty()) return null;
        var nativeId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return nativeId != null && "gt6ce".equals(nativeId.getNamespace())
                && RegisteredFluids.sanitizePath(binding.registryName()).equals(nativeId.getPath()) ? stack : null;
    }

    /** field key by raw 1.7.10 registry name ("nitrofuel", "gas.natural", ...). */
    private static volatile Map<String, String> BY_REGISTRY_NAME;

    public static FluidStack byRegistryName(String registryName, int mb) {
        if ("water".equalsIgnoreCase(registryName) || "minecraft:water".equalsIgnoreCase(registryName))
            return new FluidStack(Fluids.WATER, mb);
        Map<String, String> index = BY_REGISTRY_NAME;
        if (index == null) {
            synchronized (GeneratedRecipeResolver.class) {
                if (BY_REGISTRY_NAME == null) {
                    Map<String, String> map = new HashMap<>();
                    for (Map.Entry<String, RegisteredFluids.FluidEntry> entry : RegisteredFluids.all().entrySet()) {
                        map.putIfAbsent(entry.getValue().registryName().toLowerCase(java.util.Locale.ROOT), entry.getKey());
                    }
                    // GT6's 1.7.10 registry names that the port spells with an underscore. The tokens
                    // are read from GT6's own rows, so the old spelling has to keep working.
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
        // Water is vanilla water; the catalog keeps no separate entry for it.
        if ("Water".equals(key) || "minecraft:water".equals(key)) return new FluidStack(Fluids.WATER, mb);
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get(key);
        if (entry == null) return null;
        // Lava declarations reuse the vanilla fluid; Loader_Fluids registers no still/flowing pair
        // for it, so the vanilla stack has to be returned here (same as GTFluids.stack).
        if (entry.isVanillaLava()) return new FluidStack(Fluids.LAVA, mb);
//? if >=1.21.1 {
        /*DeferredHolder<Fluid, ? extends Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isBound()) return null;
*///?} else {
        RegistryObject<Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isPresent()) return null;
//?}
        return new FluidStack(fluid.get(), mb);
    }

    /** Named registered fluid by field key, including the vanilla water/lava reuse. */
    public static FluidStack namedFluid(String key, int mb) {
        return named(key, mb);
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

    public static GTMaterial material(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        if (material != null && material.resolve().isValid()) return material.resolve();
        // GT6 field names (TiCl4, HNO3, ...) often differ from the registry
        // display names ("Titanium Tetrachloride") — fall back to the MT field
        try {
            Object value = gregtech.content.data.ImportedMaterialData.class.getField(name).get(null);
            if (value instanceof GTMaterial mt && mt.resolve().isValid()) return mt.resolve();
        } catch (ReflectiveOperationException ignored) {
            // no such field — genuinely unknown material
        }
        return null;
    }

    private static MaterialPrefix prefix(String name) {
        if (PREFIXES.isEmpty()) {
            for (MaterialPrefix prefix : gregtech.engine.prefix.PrefixRegistry.all()) {
                PREFIXES.put(prefix.getName(), prefix);
            }
        }
        return PREFIXES.get(name);
    }
}
