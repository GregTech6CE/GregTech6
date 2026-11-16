package gregtech.content.recipe;

import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;

import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.SpecialProcessMethod;
import gregtech.api.addon.material.SpecialProcessValue;
import gregtech.api.material.GTValues;
import gregtech.content.data.FluidCatalog;
import gregtech.content.material.catalog.MaterialFluidUnitFacts;
import gregtech.content.registry.GTFluids;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialViews;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Generic producer for declarative special-process targets and reagents. */
public final class SharedSpecialProcessRows {
    private static final Set<Recipe> OWNED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean initialized;

    private SharedSpecialProcessRows() {}

    public static void initialize() {
        initialized = true;
    }

    public static void clear() {
        OWNED.forEach(recipe -> RecipeMap.RECIPE_MAP_LIST.forEach(map -> {
            map.unlink(recipe);
        }));
        OWNED.clear();
        RecipeMap.RECIPE_MAP_LIST.forEach(map -> map.mRecipeList.forEach(recipe -> recipe.withSpecialProcess(null)));
    }

    public static Set<Recipe> owned() {
        var result = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        result.addAll(OWNED);
        return Collections.unmodifiableSet(result);
    }

    public static void rebuild() {
        if (!initialized) return;
        clear();
        var catalog = MaterialViews.catalog();
        var methods = methodRules(catalog);
        var resolver = new Resolver();
        int omitted = 0;
        for (var methodRules : methods) {
            var method = methodRules.method();
            RecipeMap map = RecipeMap.RECIPE_MAP_LIST.stream()
                    .filter(candidate -> candidate.mNameInternal.equals(method.recipeMap()))
                    .findFirst()
                    .orElse(null);
            if (map == null)
                throw new IllegalArgumentException(
                        "Special process " + method.process().id() + " names unknown recipe map " + method.recipeMap());
            if (methodRules.reagents().isEmpty()) continue;
            for (var target : methodRules.targets()) {
                for (var inputEntry : target.inputCounts().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(FormKey::name)))
                        .toList()) {
                    for (var reagent : methodRules.reagents()) {
                        var built = build(
                                target.material(), inputEntry.getKey(), inputEntry.getValue(), target.plan(),
                                reagent, resolver);
                        if (built.recipe() == null) {
                            omitted++;
                            continue;
                        }
                        Recipe accepted = map.addRecipe(built.recipe());
                        if (accepted != null) {
                            accepted.withSpecialProcess(method.process().id());
                            OWNED.add(accepted);
                        }
                    }
                }
            }
        }
        if (omitted > 0)
            com.mojang.logging.LogUtils.getLogger()
                    .warn("[gregtech] Omitted {} special-process rows whose material forms or reagent did not resolve",
                            omitted);
        annotateExistingRows(methods);
    }

    /** Marks rows installed after static/addon producers, including datapack recipe documents. */
    public static void annotateCurrent() {
        if (!initialized) return;
        annotateExistingRows(methodRules(MaterialViews.catalog()));
    }

    private static void annotateExistingRows(List<MethodRules> methods) {
        var byMap = methods.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        rules -> rules.method().recipeMap(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
        for (var map : RecipeMap.RECIPE_MAP_LIST) {
            var candidates = byMap.getOrDefault(map.mNameInternal, List.of());
            if (candidates.isEmpty()) continue;
            for (var recipe : map.mRecipeList) {
                var matches = candidates.stream().filter(rules -> matches(recipe, rules)).toList();
                if (matches.size() > 1)
                    throw new IllegalArgumentException("Recipe belongs to multiple special process methods in "
                            + map.mNameInternal);
                if (matches.size() == 1)
                    recipe.withSpecialProcess(matches.get(0).method().process().id());
            }
        }
    }

    private static boolean matches(Recipe recipe, MethodRules rules) {
        boolean target = false;
        boolean reagent = false;
        for (var selector : rules.targets()) {
            for (var input : selector.inputCounts().entrySet()) {
                if (containsItem(recipe, selector.material(), input.getKey(), input.getValue())) {
                    target = true;
                    break;
                }
            }
        }
        for (var selector : rules.reagents()) {
            for (var form : selector.forms())
                if (containsInput(recipe, selector.material(), form)) {
                    reagent = true;
                    break;
                }
        }
        return target && reagent;
    }

    private static boolean containsInput(Recipe recipe, MaterialKey material, FormKey form) {
        return isFluid(form)
                ? java.util.Arrays.stream(recipe.mFluidInputs)
                        .anyMatch(stack -> sameFluidMaterial(stack, material, form))
                : java.util.Arrays.stream(recipe.mInputs).anyMatch(stack -> sameItemForm(stack, material, form));
    }

    private static boolean containsItem(Recipe recipe, MaterialKey material, FormKey form, int count) {
        return java.util.Arrays.stream(recipe.mInputs)
                .anyMatch(stack -> stack != null
                        && !stack.isEmpty()
                        && stack.getCount() == count
                        && sameItemForm(stack, material, form));
    }

    private static boolean sameItemForm(ItemStack stack, MaterialKey material, FormKey form) {
        if (stack == null || stack.isEmpty()) return false;
        var equivalent = MaterialEquivalence.form(stack);
        return equivalent != null
                && equivalent.material().resolve().getName().equals(material.name())
                && equivalent.prefix().getName().equals(form.name());
    }

    private static boolean sameFluidMaterial(FluidStack stack, MaterialKey material, FormKey form) {
        if (stack == null || stack.isEmpty() || !isFluid(form)) return false;
        var entry = GTFluids.entryForFluid(stack.getFluid());
        if (entry == null) return false;
        var value = gregtech.engine.fluid.FluidVisualPolicy.material(entry).resolve();
        return value.isValid() && value.getName().equals(material.name()) && entry.gas() == form.name().equals("gas");
    }

    private record BuildResult(Recipe recipe) {}

    private static BuildResult build(
            MaterialKey target,
            FormKey inputForm,
            int inputCount,
            SpecialProcessValue.Target plan,
            ReagentSelector reagent,
            Resolver resolver) {
        ItemStack input = resolver.item(target, inputForm, inputCount);
        if (input.isEmpty()) return new BuildResult(null);
        var itemInputs = new ArrayList<ItemStack>();
        itemInputs.add(input);
        var fluidInputs = new ArrayList<FluidStack>();
        if (!addReagent(reagent, plan.reagentAmount(), itemInputs, fluidInputs, resolver))
            return new BuildResult(null);

        var itemOutputs = new ArrayList<ItemStack>();
        var chances = new ArrayList<Long>();
        for (var output : plan.itemOutputs()) {
            ItemStack stack = firstItem(output, resolver);
            if (stack.isEmpty()) continue;
            itemOutputs.add(stack);
            chances.add((long) output.chance());
        }
        var fluidOutputs = new ArrayList<FluidStack>();
        for (var output : plan.fluidOutputs()) {
            FluidStack stack = firstFluid(output, resolver);
            if (!stack.isEmpty()) fluidOutputs.add(stack);
        }
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) return new BuildResult(null);
        return new BuildResult(new Recipe(
                itemInputs.toArray(ItemStack[]::new),
                itemOutputs.toArray(ItemStack[]::new),
                null,
                chances.stream().mapToLong(Long::longValue).toArray(),
                fluidInputs.toArray(FluidStack[]::new),
                fluidOutputs.toArray(FluidStack[]::new),
                plan.duration(),
                plan.euPerTick(),
                0));
    }

    private static boolean addReagent(
            ReagentSelector reagent,
            long amount,
            List<ItemStack> itemInputs,
            List<FluidStack> fluidInputs,
            Resolver resolver) {
        for (var form : reagent.forms().stream()
                .sorted(Comparator.comparing(FormKey::name))
                .toList()) {
            if (isFluid(form)) {
                FluidStack fluid = resolver.fluid(reagent.material(), form, amount);
                if (!fluid.isEmpty()) {
                    fluidInputs.add(fluid);
                    return true;
                }
            } else {
                if (amount > Integer.MAX_VALUE) return false;
                ItemStack stack = resolver.item(reagent.material(), form, Math.toIntExact(amount));
                if (!stack.isEmpty()) {
                    itemInputs.add(stack);
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack firstItem(SpecialProcessValue.ItemOutput output, Resolver resolver) {
        for (var candidate : output.forms()) {
            ItemStack stack = resolver.item(output.material(), candidate.form(), candidate.count());
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static FluidStack firstFluid(SpecialProcessValue.FluidOutput output, Resolver resolver) {
        for (var candidate : output.forms()) {
            if (!isFluid(candidate.form())) continue;
            FluidStack stack = resolver.fluid(output.material(), candidate.form(), output.millibuckets());
            if (!stack.isEmpty()) return stack;
        }
        return FluidStack.EMPTY;
    }

    private static boolean isFluid(FormKey form) {
        return form.name().equals("liquid") || form.name().equals("gas") || form.name().equals("molten");
    }

    private record TargetSelector(MaterialKey material, Map<FormKey, Integer> inputCounts, SpecialProcessValue.Target plan) {}

    private record ReagentSelector(MaterialKey material, Set<FormKey> forms) {}

    private record MethodRules(
            SpecialProcessMethod method, List<TargetSelector> targets, List<ReagentSelector> reagents) {}

    private static List<MethodRules> methodRules(gregtech.api.addon.material.MaterialCatalogView catalog) {
        var targets = new LinkedHashMap<MaterialProcessKey<?>, List<TargetSelector>>();
        var reagents = new LinkedHashMap<MaterialProcessKey<?>, List<ReagentSelector>>();
        for (var material : catalog.materials()) {
            for (var key : catalog.processes().keys(material.key())) {
                for (var rule : catalog.processes().find(material.key(), key)) {
                    if (rule.value() instanceof SpecialProcessValue.Target plan) {
                        targets.computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(new TargetSelector(material.key(), plan.inputCounts(), plan));
                    } else if (rule.value() instanceof SpecialProcessValue.Reagent) {
                        reagents.computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(new ReagentSelector(material.key(), rule.forms()));
                    }
                }
            }
        }
        return catalog.specialProcesses().methods().stream()
                .map(method -> new MethodRules(
                        method,
                        List.copyOf(targets.getOrDefault(method.process(), List.of())),
                        List.copyOf(reagents.getOrDefault(method.process(), List.of()))))
                .toList();
    }

    private record ItemKey(MaterialKey material, FormKey form, int count) {}

    private record FluidKey(MaterialKey material, FormKey form, long amount) {}

    private static final class Resolver {
        private final SharedOreWashingRows.Snapshot forms = new SharedOreWashingRows.Snapshot();
        private final Map<ItemKey, ItemStack> items = new HashMap<>();
        private final Map<FluidKey, FluidStack> fluids = new HashMap<>();

        private ItemStack item(MaterialKey materialKey, FormKey form, int count) {
            var key = new ItemKey(materialKey, form, count);
            return items.computeIfAbsent(key, ignored -> resolveItem(materialKey, form, count)).copy();
        }

        private FluidStack fluid(MaterialKey materialKey, FormKey form, long amount) {
            var key = new FluidKey(materialKey, form, amount);
            FluidStack value = fluids.computeIfAbsent(key, ignored -> resolveFluid(materialKey, form, amount));
            return value.copy();
        }

        private ItemStack resolveItem(MaterialKey materialKey, FormKey form, int count) {
            if (isFluid(form) || count < 1) return ItemStack.EMPTY;
            var material = GTMaterialRegistry.get(materialKey.name()).resolve();
            var prefix = PrefixRegistry.byName(form.name());
            if (material == null || !material.isValid() || prefix == null) return ItemStack.EMPTY;
            NativeForm nativeForm = forms.direct(prefix, material, count);
            return nativeForm.availability() == Availability.PRESENT ? nativeForm.stack().copy() : ItemStack.EMPTY;
        }

        private FluidStack resolveFluid(MaterialKey materialKey, FormKey form, long amount) {
            if (!isFluid(form) || amount < 1 || amount > Integer.MAX_VALUE) return FluidStack.EMPTY;
            if (materialKey.name().equals("Water")) return new FluidStack(Fluids.WATER, (int) amount);
            var material = GTMaterialRegistry.get(materialKey.name()).resolve();
            if (material == null || !material.isValid()) return FluidStack.EMPTY;
            var binding = MaterialFluidUnitFacts.binding(material, form.name());
            if (binding == null
                    || binding.referenceSourceUnits() != GTValues.U
                    || binding.referenceMillibuckets() != 1000) return FluidStack.EMPTY;
            var entry = FluidCatalog.get(binding.fluidField());
            if (entry == null || entry.gas() != form.name().equals("gas")) return FluidStack.EMPTY;
            FluidStack stack = GTFluids.stack(binding.fluidField(), (int) amount);
            if (stack == null || stack.isEmpty() || stack.getAmount() != amount) return FluidStack.EMPTY;
            return stack;
        }
    }
}
