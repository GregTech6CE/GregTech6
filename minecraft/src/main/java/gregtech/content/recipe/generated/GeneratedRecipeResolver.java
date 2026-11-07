package gregtech.content.recipe.generated;

import gregtech.api.material.GTMaterial;
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
     * Resolves a fluid spec ({@code f:<registry name>:<mb>}, {@code fr:<registry name>:<mb>},
     * {@code w:<mb>}) for the hand-ported packs that reuse this loader's grammar.
     *
     * @return the fluid, or null when the referenced content is absent
     */
    public static FluidStack resolveFluidSpec(String spec) {
        return resolveFluid(spec);
    }

    /** @return resolved stack, or null/empty when the referenced content is absent. */
    private static ItemStack resolveItem(String spec) {
        String[] parts = spec.split(":");
        switch (parts[0]) {
            case "sci" -> { return sourceCentrifugeItem(spec.split(":", -1)); }
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
        if (binding == null) return null;
        GTMaterial material = material(binding.materialName());
        if (material == null) return null;
        ItemStack target = resolveItem("v:" + binding.vanillaPath() + ":" + count);
        if (target == null) return null;
        var form = gregtech.engine.material.MaterialEquivalence.form(target);
        boolean core = material.isValid() && material.resolve() == material
                && GTMaterialRegistry.isCoreMaterial(material)
                && !gregtech.engine.addon.AddonMaterials.owns(material);
        boolean targetMatches = form != null && form.material().resolve() == material
                && form.prefix().getName().equals(binding.prefixName());
        boolean external = externalForm(binding);
        if (gregtech.content.recipe.SourceCentrifugeItemOutputs.useDefault(binding,
                material.getId(), material.getName(), core, targetMatches, external)) return target;
        var output = gregtech.content.recipe.SourceCentrifugeItemOutputs.output(binding, count);
        if (output != null) return OM.dust(material, output.units().units());
        MaterialPrefix prefix = prefix(binding.prefixName());
        return prefix == null ? null : MaterialStackItemHelper.mat(prefix, material, count);
    }

    /** The optional-tail material target, resolved from its typed name and material amount. */
    static ItemStack sourceCentrifugeMaterialTarget(
            gregtech.content.recipe.SourceCentrifugeOptionalBindings.Target target) {
        if (target.kind() != gregtech.content.recipe.SourceCentrifugeOptionalBindings.TargetKind.MATERIAL_SPEC
                || target.amount() == null) return ItemStack.EMPTY;
        GTMaterial material = material(target.value());
        return material == null ? ItemStack.EMPTY : OM.dust(material, target.amount().units());
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
            case "fr" -> byRegistryName(parts[1], Integer.parseInt(parts[2]));
            default -> null;
        };
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

    /**
     * The registered fluid of a material that has exactly one phase binding; {@code mb} is the
     * native amount. A material with both a gas and a liquid phase is ambiguous and returns null;
     * callers that know the phase resolve it through {@link gregtech.engine.recipe.chem.MaterialFluidBinding}.
     */
    public static FluidStack materialFluid(String materialName, int mb) {
        GTMaterial material = material(materialName);
        if (material == null) return null;
        var bindings = gregtech.engine.recipe.chem.MaterialFluidBinding.findAll(material.getName());
        var fields = new java.util.LinkedHashSet<String>();
        for (var binding : bindings) fields.add(binding.field());
        if (fields.size() != 1) return null;
        FluidStack stack = GTFluids.stack(fields.iterator().next(), mb);
        return stack == null || stack.isEmpty() ? null : stack;
    }

    public static GTMaterial material(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        if (material != null && material.resolve().isValid()) return material.resolve();
        String canonical = gregtech.content.recipe.generated.GTMaterialFields.materialOf(name);
        if (canonical == null) return null;
        material = GTMaterialRegistry.get(GTMaterial.sanitize(canonical));
        return material == null || !material.resolve().isValid() ? null : material.resolve();
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
