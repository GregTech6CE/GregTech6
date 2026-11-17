package gregtech.content.recipe;


import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialFormsView;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.SpecialProcessMethod;
import gregtech.api.addon.material.SpecialProcessValue;
import gregtech.engine.material.MaterialViews;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
        var omitted = new LinkedHashMap<String, Integer>();
        for (var methodRules : methods) {
            var method = methodRules.method();
            RecipeMap map = RecipeMap.RECIPE_MAP_LIST.stream()
                    .filter(candidate -> candidate.mNameInternal.equals(method.recipeMap()))
                    .findFirst()
                    .orElse(null);
            if (map == null) {
                com.mojang.logging.LogUtils.getLogger().warn(
                        "[gregtech] Skipping special process {} with unknown recipe map {}",
                        method.process().id(), method.recipeMap());
                continue;
            }
            if (methodRules.reagents().isEmpty()) {
                if (!methodRules.targets().isEmpty())
                    com.mojang.logging.LogUtils.getLogger().warn(
                            "[gregtech] Skipping special process {}: {} targets have no declared reagents",
                            method.process().id(), methodRules.targets().size());
                continue;
            }
            for (var target : methodRules.targets()) {
                for (var inputEntry : target.inputCounts().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(FormKey::name)))
                        .toList()) {
                    for (var reagent : methodRules.reagents()) {
                        var built = build(
                                target.material(), inputEntry.getKey(), inputEntry.getValue(), target.plan(),
                                reagent, resolver);
                        if (built.recipe() == null) {
                            omitted.merge(built.reason(), 1, Integer::sum);
                            com.mojang.logging.LogUtils.getLogger().debug(
                                    "[gregtech] Omitted special process {} for {}/{} with {}: {}",
                                    method.process().id(), target.material().name(), inputEntry.getKey().name(),
                                    reagent.material().name(), built.reason());
                            continue;
                        }
                        Recipe accepted = map.addRecipe(built.recipe());
                        if (accepted != null) {
                            OWNED.add(accepted);
                        } else {
                            omitted.merge("recipe-rejected", 1, Integer::sum);
                        }
                    }
                }
            }
        }
        if (!omitted.isEmpty())
            com.mojang.logging.LogUtils.getLogger()
                    .warn("[gregtech] Omitted special-process rows by reason: {}",
                            omitted);
    }

    record BuildResult(Recipe recipe, String reason) {}

    static BuildResult build(
            MaterialKey target,
            FormKey inputForm,
            int inputCount,
            SpecialProcessValue.Target plan,
            ReagentSelector reagent,
            Resolver resolver) {
        ItemStack input = resolver.item(target, inputForm, inputCount);
        if (input.isEmpty()) return new BuildResult(null, "input-form");
        var itemInputs = new ArrayList<ItemStack>();
        itemInputs.add(input);
        var fluidInputs = new ArrayList<FluidStack>();
        if (!addReagent(reagent, plan.reagentAmount(), itemInputs, fluidInputs, resolver))
            return new BuildResult(null, "reagent");

        var itemOutputs = new ArrayList<ItemStack>();
        var chances = new ArrayList<Long>();
        for (var output : plan.itemOutputs()) {
            ItemStack stack = firstItem(output, resolver);
            if (stack.isEmpty()) return new BuildResult(null, "item-output");
            itemOutputs.add(stack);
            chances.add((long) output.chance());
        }
        var fluidOutputs = new ArrayList<FluidStack>();
        for (var output : plan.fluidOutputs()) {
            FluidStack stack = firstFluid(output, resolver);
            if (stack.isEmpty()) return new BuildResult(null, "fluid-output");
            fluidOutputs.add(stack);
        }
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) return new BuildResult(null, "empty-outputs");
        return new BuildResult(new Recipe(
                itemInputs.toArray(ItemStack[]::new),
                itemOutputs.toArray(ItemStack[]::new),
                null,
                chances.stream().mapToLong(Long::longValue).toArray(),
                fluidInputs.toArray(FluidStack[]::new),
                fluidOutputs.toArray(FluidStack[]::new),
                plan.duration(),
                plan.euPerTick(),
                0), null);
    }

    private static boolean addReagent(
            ReagentSelector reagent,
            long amount,
            List<ItemStack> itemInputs,
            List<FluidStack> fluidInputs,
            Resolver resolver) {
        var forms = reagent.forms().isEmpty()
                ? resolver.bindings.find(reagent.material()).stream()
                        .map(binding -> binding.form()).collect(java.util.stream.Collectors.toSet())
                : reagent.forms();
        if (reagent.forms().isEmpty()) {
            var kinds = resolver.bindings.find(reagent.material()).stream()
                    .map(binding -> binding.kind()).filter(kind -> kind == MaterialFormKind.ITEM || kind == MaterialFormKind.FLUID)
                    .collect(java.util.stream.Collectors.toSet());
            if (kinds.size() > 1) return false;
        }
        for (var form : forms.stream()
                .sorted(Comparator.comparing(FormKey::name))
                .toList()) {
            if (isFluid(form)) {
                FluidStack fluid = resolver.fluid(reagent.material(), form, amount);
                if (!fluid.isEmpty()) {
                    fluidInputs.add(fluid);
                    return true;
                }
            } else {
                if (amount > Integer.MAX_VALUE) continue;
                ItemStack stack = resolver.item(reagent.material(), form, Math.toIntExact(amount));
                if (!stack.isEmpty() && amount <= stack.getMaxStackSize()) {
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

    record ReagentSelector(MaterialKey material, Set<FormKey> forms) {}

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

    static final class Resolver {
        private final MaterialFormsView bindings;

        Resolver() {
            this(MaterialViews.catalog().formBindings());
        }

        Resolver(MaterialFormsView bindings) {
            this.bindings = bindings;
        }

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
            for (var binding : bindings.find(materialKey, form).stream()
                    .filter(value -> value.kind() == MaterialFormKind.ITEM)
                    .sorted(Comparator.comparing(value -> value.id()))
                    .toList()) {
                var id = ResourceLocation.tryParse(binding.id());
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
                var item = BuiltInRegistries.ITEM.get(id);
                if (item != net.minecraft.world.item.Items.AIR) return new ItemStack(item, count);
            }
            return ItemStack.EMPTY;
        }

        private FluidStack resolveFluid(MaterialKey materialKey, FormKey form, long amount) {
            if (!isFluid(form) || amount < 1 || amount > Integer.MAX_VALUE) return FluidStack.EMPTY;
            // Special-process quantities are already mB; material-unit conversion is unrelated.
            for (var binding : bindings.find(materialKey, form).stream()
                    .filter(value -> value.kind() == MaterialFormKind.FLUID)
                    .sorted(Comparator.comparing(value -> value.id()))
                    .toList()) {
                var id = ResourceLocation.tryParse(binding.id());
                if (id == null) continue;
                var fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid != Fluids.EMPTY) return new FluidStack(fluid, (int) amount);
            }
            return FluidStack.EMPTY;
        }
    }
}
