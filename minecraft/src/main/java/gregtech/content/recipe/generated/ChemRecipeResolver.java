package gregtech.content.recipe.generated;

import com.mojang.logging.LogUtils;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MaterialPrefixes;
import gregtech.content.recipe.OriginalMaterialFluidUnits;
import gregtech.content.recipe.SourceCentrifugeBlockDustInputs;
import gregtech.content.recipe.SourceCentrifugeItemOutputs;
import gregtech.content.recipe.SourceCentrifugeOptionalBindings;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluidItems;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.chem.ChemFluid;
import gregtech.engine.recipe.chem.ChemIngredient;
import gregtech.engine.recipe.chem.ChemOutcome;
import gregtech.engine.recipe.chem.ChemRecipe;
import gregtech.engine.recipe.chem.ChemResult;
import gregtech.engine.recipe.chem.MaterialFluidBinding;
import gregtech.util.OM;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

/**
 * Converts the source-generated recipe rows to native recipes and publishes them exactly once.
 *
 * <p>A source row is resolved as a whole: every material, form and fluid must be present before
 * the row is submitted to its map. The ledger keeps the disposition of every candidate, so a
 * missing optional mod fluid, a missing required material and an input collision stay distinct.</p>
 */
public final class ChemRecipeResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, RecipeMap> MAPS = new HashMap<>();
    private static final List<ChemResult> RESULTS = new ArrayList<>();

    private ChemRecipeResolver() {}

    public static void clear() {
        RESULTS.clear();
        MAPS.clear();
    }

    /** Records a source row that the current content set deliberately does not build. */
    public static void exclude(String source, String detail) {
        record(ChemResult.excluded(source, detail));
    }

    /** Records an optional-mod source row whose fluid family is not installed. */
    public static void optionalAbsent(String source, String detail) {
        record(ChemResult.optionalAbsent(source, detail));
    }

    public static List<ChemResult> results() {
        return List.copyOf(RESULTS);
    }

    public static int count(ChemOutcome outcome) {
        int count = 0;
        for (ChemResult result : RESULTS) if (result.outcome() == outcome) count++;
        return count;
    }

    /** Resolves and submits one source row. Never throws for absent content or a map collision. */
    public static ChemResult publish(ChemRecipe row) {
        try {
            RecipeMap map = map(row.map());
            if (map == null) {
                return record(ChemResult.failed(row, ChemOutcome.VALIDATION_FAILED, "unknown recipe map " + row.map()));
            }
            List<String> substitutions = new ArrayList<>();
            Recipe recipe = resolve(row, substitutions);
            if (recipe == null) {
                return record(ChemResult.failed(row, ChemOutcome.MISSING_CONTENT, missing(row)));
            }
            var collision = map.findCollision(recipe);
            if (collision != null) {
                return record(ChemResult.collision(row, describe(collision)));
            }
            if (map.addRecipe(recipe) == null) {
                return record(ChemResult.failed(row, ChemOutcome.VALIDATION_FAILED, "map rejected row"));
            }
            return record(new ChemResult(row, row.source(),
                    substitutions.isEmpty() ? ChemOutcome.PUBLISHED : ChemOutcome.SUBSTITUTED,
                    String.join("; ", substitutions), ""));
        } catch (MissingContent missing) {
            return record(ChemResult.failed(row, missing.optional ? ChemOutcome.OPTIONAL_ABSENT : ChemOutcome.MISSING_CONTENT,
                    missing.getMessage()));
        } catch (RuntimeException invalid) {
            return record(ChemResult.failed(row, ChemOutcome.VALIDATION_FAILED,
                    invalid.getMessage() == null ? invalid.getClass().getSimpleName() : invalid.getMessage()));
        }
    }

    /** Logs one summary line; the detailed rows remain available through {@link #results()}. */
    public static void logSummary(String family) {
        LOGGER.info("[gregtech] {}: {} published, {} substituted, {} excluded, {} missing, {} optional absent, "
                        + "{} collisions, {} invalid",
                family, count(ChemOutcome.PUBLISHED), count(ChemOutcome.SUBSTITUTED), count(ChemOutcome.EXCLUDED),
                count(ChemOutcome.MISSING_CONTENT), count(ChemOutcome.OPTIONAL_ABSENT), count(ChemOutcome.COLLISION),
                count(ChemOutcome.VALIDATION_FAILED));
        for (ChemResult result : RESULTS) {
            if (result.outcome() == ChemOutcome.PUBLISHED || result.outcome() == ChemOutcome.SUBSTITUTED) continue;
            LOGGER.warn("[gregtech] chemistry {}: {} ({})", result.outcome(), result.source(), result.detail());
        }
    }

    private static ChemResult record(ChemResult result) {
        RESULTS.add(result);
        return result;
    }

    private static Recipe resolve(ChemRecipe row, List<String> substitutions) throws MissingContent {
        if (row.inputs().size() > 1
                && row.inputs().stream().anyMatch(i -> i.ingredient() instanceof ChemIngredient.CentrifugeBlockDust))
            throw new MissingContent("block dust is a single-input row", false);
        List<ItemStack> inputs = new ArrayList<>(row.inputs().size());
        List<Integer> kept = new ArrayList<>();
        for (int i = 0; i < row.inputs().size(); i++) {
            var input = row.inputs().get(i);
            ItemStack stack = item(input.ingredient());
            if (stack.isEmpty()) throw new MissingContent("item " + input.ingredient(), false);
            inputs.add(stack);
            if (input.role() == ChemRecipe.Role.KEPT) kept.add(i);
        }

        List<FluidStack> fluidInputs = new ArrayList<>(row.fluidInputs().size());
        List<Integer> keptFluids = new ArrayList<>();
        List<Integer> presenceFluids = new ArrayList<>();
        for (int i = 0; i < row.fluidInputs().size(); i++) {
            var input = row.fluidInputs().get(i);
            FluidStack stack = fluid(input.fluid(), substitutions);
            if (stack.isEmpty()) throw new MissingContent("fluid " + input.fluid(), false);
            fluidInputs.add(stack);
            if (input.role() == ChemRecipe.Role.KEPT) keptFluids.add(i);
            if (input.role() == ChemRecipe.Role.PRESENCE) presenceFluids.add(i);
        }

        List<FluidStack> fluidOutputs = new ArrayList<>(row.fluidOutputs().size());
        for (var output : row.fluidOutputs()) {
            FluidStack stack = fluid(output.fluid(), substitutions);
            if (stack.isEmpty()) throw new MissingContent("fluid output " + output.fluid(), false);
            fluidOutputs.add(stack);
        }

        List<ItemStack> itemOutputs = new ArrayList<>(row.outputs().size());
        List<Long> outputChances = new ArrayList<>(row.outputs().size());
        boolean optimize = row.optimize();
        for (int i = 0; i < row.outputs().size(); i++) {
            var output = row.outputs().get(i);
            if (output.ingredient() instanceof ChemIngredient.OptionalOutput optional) {
                // Both pinned source calls place the optional holder after one known output.
                if (i != row.outputs().size() - 1 || itemOutputs.size() != 1)
                    throw new MissingContent("optional tail position " + optional.plan(), false);
                var plan = SourceCentrifugeOptionalBindings.Plan.fromToken(optional.plan());
                if (plan == null) throw new MissingContent("optional plan " + optional.plan(), false);
                var resolved = SourceCentrifugeOptionalItemOutputs.resolve(plan);
                switch (resolved.action()) {
                    case DEFER_ROW -> throw new MissingContent("optional " + optional.plan(), true);
                    case OMIT_OPTIONAL_TAIL -> optimize = false;
                    case APPEND -> {
                        itemOutputs.add(resolved.stack());
                        outputChances.add((long) output.chance());
                    }
                }
                continue;
            }
            ItemStack stack = item(output.ingredient());
            if (stack.isEmpty()) throw new MissingContent("output " + output.ingredient(), false);
            itemOutputs.add(stack);
            outputChances.add((long) output.chance());
        }
        ItemStack[] outputs = itemOutputs.toArray(RecipeMap.ZL_IS);
        long[] chances = new long[outputChances.size()];
        for (int i = 0; i < chances.length; i++) chances[i] = outputChances.get(i);

        Recipe recipe = optimize
                ? RecipeMap.make(true, inputs.toArray(RecipeMap.ZL_IS), outputs, null, chances,
                fluidInputs.toArray(RecipeMap.ZL_FS), fluidOutputs.toArray(RecipeMap.ZL_FS), row.ticks(), row.eut(), 0)
                : new Recipe(inputs.toArray(RecipeMap.ZL_IS), outputs, null, chances,
                fluidInputs.toArray(RecipeMap.ZL_FS), fluidOutputs.toArray(RecipeMap.ZL_FS), row.ticks(), row.eut(), 0);
        if (recipe == null) throw new IllegalStateException("optimized chemistry row collapsed");
        if (!kept.isEmpty()) recipe.withCatalystInputs(kept.stream().mapToInt(Integer::intValue).toArray());
        if (!keptFluids.isEmpty())
            recipe.withCatalystFluidInputs(keptFluids.stream().mapToInt(Integer::intValue).toArray());
        if (!presenceFluids.isEmpty())
            recipe.withPresenceFluidInputs(presenceFluids.stream().mapToInt(Integer::intValue).toArray());
        return recipe;
    }

    private static ItemStack item(ChemIngredient ingredient) throws MissingContent {
        if (ingredient instanceof ChemIngredient.DustUnits dust) {
            GTMaterial material = material(dust.material());
            return material == null ? ItemStack.EMPTY : OM.dust(material, dust.units().units());
        }
        if (ingredient instanceof ChemIngredient.IngotUnits ingot) {
            GTMaterial material = material(ingot.material());
            return material == null ? ItemStack.EMPTY : OM.ingot(material, ingot.units().units());
        }
        if (ingredient instanceof ChemIngredient.IngotOrDustUnits ingotOrDust) {
            GTMaterial material = material(ingotOrDust.material());
            return material == null ? ItemStack.EMPTY : OM.ingotOrDust(material, ingotOrDust.units().units());
        }
        if (ingredient instanceof ChemIngredient.GemUnits gem) {
            GTMaterial material = material(gem.material());
            return material == null ? ItemStack.EMPTY : OM.gem(material, gem.units().units());
        }
        if (ingredient instanceof ChemIngredient.ItemForm form) {
            var entry = MaterialPrefixes.all().get(form.prefix());
            MaterialPrefix prefix = entry == null ? null : entry.getDelegate();
            // Ammunition casings keep GT6's dedicated Empty material, not a registry material.
            GTMaterial material = "Empty".equals(form.material()) && prefix != null
                    && prefix.hasEmptyAmmunitionForm() ? gregtech.content.material.Materials.Empty
                    : material(form.material());
            if (prefix != null)
                return material == null ? ItemStack.EMPTY : MaterialStackItemHelper.mat(prefix, material, form.count());
            // GT6's block forms (blockDust and the other OP.block*) are blocks in this port.
            BlockMaterialPrefix block = blockPrefix(form.prefix());
            if (block == null || material == null) return ItemStack.EMPTY;
            ItemStack stack = GTBlocks.getStack(block, material);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            stack = stack.copy();
            stack.setCount(Math.min(form.count(), stack.getMaxStackSize()));
            return stack;
        }
        if (ingredient instanceof ChemIngredient.BlockForm form) {
            BlockMaterialPrefix prefix = blockPrefix(form.prefix());
            GTMaterial material = material(form.material());
            if (prefix == null || material == null) return ItemStack.EMPTY;
            ItemStack stack = GTBlocks.getStack(prefix, material);
            if (stack.isEmpty()) {
                LOGGER.warn("[gregtech] chemistry block form missing: prefix={} material={} object={}",
                        prefix.getName(), material.getName(),
                        GTBlocks.getObject(prefix, material));
                return ItemStack.EMPTY;
            }
            stack = stack.copy();
            stack.setCount(Math.min(form.count(), stack.getMaxStackSize()));
            return stack;
        }
        if (ingredient instanceof ChemIngredient.RegistryItem item) {
            ResourceLocation id = ResourceLocation.tryParse(item.id());
            Item value = id == null ? null : BuiltInRegistries.ITEM.get(id);
            return value == null || value == Items.AIR ? ItemStack.EMPTY : new ItemStack(value, item.count());
        }
        if (ingredient instanceof ChemIngredient.FluidItem fluid) {
            Item value = GTFluidItems.get(fluid.fluidKey());
            return value == null ? ItemStack.EMPTY : new ItemStack(value, fluid.count());
        }
        if (ingredient instanceof ChemIngredient.Selector selector) {
            Item value = GTTechnological.selectorTag(selector.index());
            return value == null ? ItemStack.EMPTY : new ItemStack(value);
        }
        if (ingredient instanceof ChemIngredient.TechnologyItem technology) {
            return technology(technology);
        }
        if (ingredient instanceof ChemIngredient.Book book) {
            return gregtech.content.book.GTBooks.bookStack(book.name());
        }
        if (ingredient instanceof ChemIngredient.CentrifugeBlockDust blockDust) {
            return blockDust(blockDust);
        }
        if (ingredient instanceof ChemIngredient.SourceItem source) {
            return sourceItem(source);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack technology(ChemIngredient.TechnologyItem technology) {
        String id = GTTechnological.ALIASES.getOrDefault(technology.name(), technology.name());
        Item value = GTTechnological.get(technology.name());
        if (value == null) {
            ResourceLocation key = id.indexOf(':') >= 0
                    ? ResourceLocation.tryParse(id)
                    : ResourceLocation.fromNamespaceAndPath("gt6ce", id);
            value = key == null ? null : BuiltInRegistries.ITEM.get(key);
        }
        return value == null || value == Items.AIR ? ItemStack.EMPTY : new ItemStack(value, technology.count());
    }

    /**
     * Only the reviewed exact block-dust holders are accepted. An absent or unreviewed block
     * prevents this row; it is never projected onto nine loose dusts.
     */
    private static ItemStack blockDust(ChemIngredient.CentrifugeBlockDust blockDust) {
        if (blockDust.count() != 1) return ItemStack.EMPTY;
        var binding = SourceCentrifugeBlockDustInputs.binding(blockDust.material());
        GTMaterial selected = material(blockDust.material());
        if (binding == null || selected == null || !selected.isValid()) return ItemStack.EMPTY;
        GTMaterial canonical = selected.resolve();
        ItemStack stack = GTBlocks.getStack(BlockMaterialPrefix.blockDust, canonical);
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        boolean typed = stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof gregtech.content.block.MaterialBlockLike form
                && form.prefix() == BlockMaterialPrefix.blockDust
                && form.material() != null && form.material().resolve() == canonical;
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean registry = typed && itemId != null && itemId.getNamespace().equals("gt6ce")
                && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock().asItem() == stack.getItem()
                && itemId.equals(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()));
        boolean plain = plainSingle(stack);
        var facts = new SourceCentrifugeBlockDustInputs.Facts(true, typed, registry, plain,
                GTMaterialRegistry.isCoreMaterial(canonical)
                        && !gregtech.engine.addon.AddonMaterials.owns(canonical),
                canonical.getName(), canonical.getId());
        return SourceCentrifugeBlockDustInputs.decide(binding, facts) == SourceCentrifugeBlockDustInputs.Decision.PRESENT
                ? stack.copy() : ItemStack.EMPTY;
    }

    private static boolean plainSingle(ItemStack stack) {
        boolean plain = stack.getCount() == 1 && !stack.isDamaged();
//? if >=1.21.1 {
        /*plain &= stack.getComponentsPatch().isEmpty();*/
//?} else {
        plain &= !stack.hasTag();
//?}
        return plain;
    }

    /**
     * The reviewed vanilla default when the exact item is the core identity; otherwise the
     * material form named by the source call.
     */
    private static ItemStack sourceItem(ChemIngredient.SourceItem source) {
        var binding = SourceCentrifugeItemOutputs.binding(source.material(), source.prefix());
        if (binding == null) return ItemStack.EMPTY;
        GTMaterial material = material(binding.materialName());
        if (material == null) return ItemStack.EMPTY;
        Item targetItem = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", binding.vanillaPath()));
        ItemStack target = targetItem == null || targetItem == Items.AIR
                ? ItemStack.EMPTY : new ItemStack(targetItem, source.count());
        var form = target.isEmpty() ? null : MaterialEquivalence.form(target);
        boolean core = material.isValid() && material.resolve() == material
                && GTMaterialRegistry.isCoreMaterial(material)
                && !gregtech.engine.addon.AddonMaterials.owns(material);
        boolean targetMatches = form != null && form.material().resolve() == material
                && form.prefix().getName().equals(binding.prefixName());
        boolean external = externalForm(binding);
        if (SourceCentrifugeItemOutputs.useDefault(binding, material.getId(), material.getName(),
                core, targetMatches, external)) return target;
        if (binding.dustSourceUnits() == 0)
            return MaterialStackItemHelper.mat(
                    MaterialPrefixes.all().get(binding.prefixName()).getDelegate(), material, source.count());
        try {
            return OM.dust(material, Math.multiplyExact(binding.dustSourceUnits(), source.count()));
        } catch (ArithmeticException overflow) {
            return ItemStack.EMPTY;
        }
    }

    /** A foreign direct material form is public identity even without an explicit composition. */
    private static boolean externalForm(SourceCentrifugeItemOutputs.Binding binding) {
        if (gregtech.engine.addon.AddonMaterials.acceptedForms().stream().anyMatch(f ->
                f.materialName().equals(binding.materialName()) && f.prefixName().equals(binding.prefixName())))
            return true;
        GTMaterial material = material(binding.materialName());
        if (material == null) return false;
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || id.getNamespace().equals("gt6ce") || id.getNamespace().equals("minecraft")) continue;
            if (item instanceof MaterialFormItem direct
                    && direct.getPrefix() != null && direct.getMaterial() != null
                    && direct.getPrefix().getName().equals(binding.prefixName())
                    && direct.getMaterial().resolve() == material) return true;
        }
        for (var entry : gregtech.engine.material.ItemMaterialRegistry.entries().entrySet()) {
            var id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id == null || id.getNamespace().equals("gt6ce") || id.getNamespace().equals("minecraft")) continue;
            var declaredForm = MaterialEquivalence.form(new ItemStack(entry.getKey()));
            if (declaredForm != null && declaredForm.prefix().getName().equals(binding.prefixName())
                    && declaredForm.material().resolve() == material) return true;
            var explicit = entry.getValue();
            if ((explicit.prefix() == null || explicit.prefix().getName().equals(binding.prefixName()))
                    && explicit.components().stream().anyMatch(c -> c.material().resolve() == material))
                return true;
        }
        return false;
    }

    private static FluidStack fluid(ChemFluid fluid, List<String> substitutions) throws MissingContent {
        if (fluid instanceof ChemFluid.MaterialFluid material) {
            GTMaterial resolved = material(material.material());
            String materialName = resolved == null ? material.material() : resolved.getName();
            var resolution = MaterialFluidBinding.resolve(materialName, material.phase())
                    .orElseThrow(() -> new MissingContent("binding " + materialName + "/" + material.phase(), false));
            MaterialFluidBinding binding = resolution.binding();
            if (resolution.phaseSubstituted())
                substitutions.add(materialName + ": source " + material.phase() + " -> registered " + binding.phase());
            int mb = binding.millibuckets(material.units(), material.rounding());
            FluidStack stack = GTFluids.stack(binding.field(), mb);
            if (stack == null || stack.isEmpty())
                throw new MissingContent("fluid " + binding.field() + " (" + binding.registryName() + ")", false);
            return stack;
        }
        if (fluid instanceof ChemFluid.NativeFluid nativeFluid) {
            FluidStack stack = GTFluids.stack(nativeFluid.key(), nativeFluid.millibuckets());
            if (stack == null || stack.isEmpty()) throw new MissingContent("fluid " + nativeFluid.key(), false);
            return stack;
        }
        if (fluid instanceof ChemFluid.Water water) {
            int mb = OriginalMaterialFluidUnits.millibuckets(water.units().units(), GTValues.U, 1000,
                    water.rounding() == gregtech.engine.recipe.chem.Rounding.CEIL);
            if (mb <= 0) throw new MissingContent("water amount " + water.units(), false);
            return new FluidStack(Fluids.WATER, mb);
        }
        if (fluid instanceof ChemFluid.RegistryFluid registry) {
            FluidStack stack = GeneratedRecipeResolver.byRegistryName(
                    registry.registryName(), registry.millibuckets());
            if (stack == null || stack.isEmpty())
                throw new MissingContent("fluid " + registry.registryName(), false);
            return stack;
        }
        return FluidStack.EMPTY;
    }

    private static BlockMaterialPrefix blockPrefix(String name) {
        if ("blockDust".equals(name) && MaterialPrefixes.blockDust != null)
            return MaterialPrefixes.blockDust.getBlockDelegate();
        var entry = MaterialPrefixes.all().get(name);
        if (entry != null && entry.getBlockDelegate() != null) return entry.getBlockDelegate();
        BlockMaterialPrefix prefix = BlockPrefixRegistry.byName(name);
        if (prefix != null) return prefix;
        return null;
    }

    private static GTMaterial material(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        if (material != null && material.resolve().isValid()) return material.resolve();
        String canonical = gregtech.content.recipe.generated.GTMaterialFields.materialOf(name);
        if (canonical == null) return null;
        material = GTMaterialRegistry.get(GTMaterial.sanitize(canonical));
        return material == null || !material.resolve().isValid() ? null : material.resolve();
    }

    private static RecipeMap map(String fieldName) {
        if (MAPS.isEmpty()) {
            for (Field field : MachineRecipeMaps.class.getFields()) {
                if (!RecipeMap.class.isAssignableFrom(field.getType())) continue;
                try {
                    MAPS.put(field.getName(), (RecipeMap) field.get(null));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return MAPS.get(fieldName);
    }

    private static String missing(ChemRecipe row) {
        return "unresolved ingredient";
    }

    private static String describe(Recipe recipe) {
        StringBuilder text = new StringBuilder();
        for (ItemStack stack : recipe.mInputs) if (!stack.isEmpty()) {
            if (text.length() > 0) text.append(", ");
            text.append(stack.getCount()).append("x ").append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        for (FluidStack stack : recipe.mFluidInputs) if (!stack.isEmpty()) {
            if (text.length() > 0) text.append(", ");
            text.append(stack.getAmount()).append("mB ").append(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        }
        return text.toString();
    }

    private static final class MissingContent extends RuntimeException {
        private final boolean optional;

        private MissingContent(String message, boolean optional) {
            super(message);
            this.optional = optional;
        }
    }
}
