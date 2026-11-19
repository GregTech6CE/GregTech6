package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.compat.kubejs.CraftingCompositionSupport;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialGroups;
import gregtech.content.item.ElectricToolItem;
import gregtech.content.machine.BasicMachineCatalog;
import gregtech.content.recipe.DiggableRecipes;
import gregtech.content.recipe.ElectronicsRecipes;
import gregtech.content.recipe.SpikeConversionRecipe;
import gregtech.content.recipe.TapeRepackRecipe;
import gregtech.content.recipe.ToolShapedRecipe;
import gregtech.content.recipe.ToolShapelessRecipe;
import gregtech.engine.fluid.MoltenFluidHandler;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionScaling;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.ItemMaterialRuntimeFacts;
import gregtech.engine.material.ReversibleCraftingData;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.UnitConversion;
import gregtech.engine.tool.GTToolHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/** Derives the recoverable minimum per item from the current production recipes. */
public final class CraftingItemCompositions {
    private record Row(
            String id,
            ItemStack output,
            List<List<ItemStack>> inputs,
            List<MaterialComponent> incorporated,
            boolean craftingTools,
            boolean firesClay) {}

    private final Map<GTMaterial, GTMaterial> commonMaterials = commonMaterials();
    private final Map<Item, Item> aliases = new HashMap<>();
    private final Set<Item> tracked = new LinkedHashSet<>();
    private final Set<Item> resolving = new HashSet<>();

    private final Map<Item, List<Row>> rows = new HashMap<>();
    private final Map<Item, Optional<ItemComposition>> resolved = new HashMap<>();

    private CraftingItemCompositions() {}

    static boolean tracks(BasicMachineBlock machine) {
        var spec = machine.basicSpec();
        return BasicMachineCatalog.crafting(spec.machineName(), spec.tier()) != null;
    }

    public static boolean tracks(Item item) {
        return RecipeCompositionItems.tracks(item);
    }

    public static void rebuild(RecipeManager manager, RegistryAccess access) {
        var calculation = new CraftingItemCompositions();
        for (var item : BuiltInRegistries.ITEM) {
            if (!tracks(item)) continue;
            calculation.tracked.add(item);
            var source = RecipeCompositionItems.compositionSource(item);
            if (source != item) calculation.aliases.put(item, source);
        }
        for (var entry : manager.getRecipes()) {
//? if >=1.21.1 {
            /*var recipe = entry.value();
            var id = entry.id();
*///?} else {
            var recipe = entry;
            var id = recipe.getId();
//?}
            if (!(recipe instanceof CraftingRecipe || recipe instanceof SmeltingRecipe || recipe instanceof BlastingRecipe))
                continue;
            var output = recipe.getResultItem(access);
            if (!calculation.accepts(output)) continue;
            var path = id.getPath();
            // Spike conversions only change placement mode. Tape repacking supplies its own remaining-use calculation.
            if (recipe instanceof SpikeConversionRecipe || recipe instanceof TapeRepackRecipe
                    || id.getNamespace().equals("gt6ce") && (path.startsWith("logistics/logistics_cycle_")
                            || path.equals("hand_components/integrated_circuit_0_from_any"))) continue;
            if (recipe instanceof CraftingRecipe crafting) {
                boolean tools = crafting.getClass() == ToolShapedRecipe.class || crafting instanceof ToolShapelessRecipe;
                boolean fixed = tools || crafting.getClass() == ShapedRecipe.class
                        || crafting.getClass() == ShapelessRecipe.class || fixedKubeRecipe(crafting);
                if (!fixed) {
                    calculation.add(new Row(id.toString(), output, null, List.of(), false, false));
                    continue;
                }
                var inputs = new ArrayList<List<ItemStack>>();
                for (var ingredient : crafting.getIngredients()) {
                    if (ingredient == Ingredient.EMPTY) continue;
                    var choices = choices(ingredient);
                    if (choices.isEmpty()) {
                        inputs.add(choices);
                        break;
                    }
                    // The imprint's template supplies geometry. Only the blank supplies the clay.
                    if (id.getNamespace().equals("gt6ce") && path.startsWith("clay_molds/imprint_")
                            && choices.stream().noneMatch(stack -> stack.is(RecipeCompositionItems.clayBlank())))
                        continue;
                    inputs.add(choices);
                }
                if (inputs.stream().noneMatch(List::isEmpty))
                    calculation.add(new Row(id.toString(), output, inputs, List.of(), tools, false));
            } else if (recipe instanceof SmeltingRecipe || recipe instanceof BlastingRecipe) {
                var input = choices(((AbstractCookingRecipe) recipe).getIngredients().get(0));
                if (!input.isEmpty())
                    calculation.add(new Row(id.toString(), output, List.of(input), List.of(), false,
                            input.stream().allMatch(stack -> RecipeCompositionItems.isRawClay(stack.getItem()))));
            }
        }

        for (var map : machineMaps()) {
            for (var recipe : map.recipeRows) calculation.addMachineRow(map, recipe, false);
        }
        // These firing rows are mirrored to the vanilla manager after the overlay.
        for (var recipe : MachineRecipeMaps.Furnace.recipeRows) {
            if (recipe.itemInputs.length == 1 && RecipeCompositionItems.isRawClay(recipe.itemInputs[0].getItem()))
                calculation.addMachineRow(MachineRecipeMaps.Furnace, recipe, true);
        }
        var candidates = new HashMap<Item, ItemComposition>();
        for (var item : calculation.tracked) {
            if (ItemMaterialRegistry.baseBeforeRecipeGeneration(item).isPresent()) continue;
            calculation.resolve(item)
                    .filter(data -> !data.components().isEmpty())
                    .ifPresent(data -> candidates.put(item, data));
        }
        ItemMaterialRegistry.publishRecipeCandidates(candidates);
    }

    private static List<ItemStack> choices(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty() && (!stack.is(Items.BARRIER) || ingredient.test(stack)))
                .map(stack -> stack.getCount() == 1 ? stack : stack.copyWithCount(1))
                .toList();
    }

    public static List<RecipeMap> machineMaps() {
        return List.of(
                MachineRecipeMaps.Laminator,
                MachineRecipeMaps.Loom,
                MachineRecipeMaps.Canner,
                MachineRecipeMaps.Press,
                MachineRecipeMaps.LaserEngraver,
                MachineRecipeMaps.Bath,
                MachineRecipeMaps.Assembler);
    }

    private boolean accepts(ItemStack output) {
        return !output.isEmpty() && tracked.contains(output.getItem()) && !aliases.containsKey(output.getItem());
    }

    private void add(Row row) {
        rows.computeIfAbsent(row.output().getItem(), ignored -> new ArrayList<>()).add(row);
    }

    private void addMachineRow(RecipeMap map, Recipe recipe, boolean firesClay) {
        if (!recipe.enabled || recipe.fakeRecipe || recipe.materialRecovery || recipe.hasRuntimeResolver()
                || recipe.itemOutputs.length != 1 || recipe.getOutputChance(0) != 10000
                || recipe.fluidOutputs.length != 0 || !accepts(recipe.itemOutputs[0])) return;
        var output = recipe.itemOutputs[0];
        // Canning also drains fuel cells. Their housing originates in the crafting recipe.
        if (map == MachineRecipeMaps.Canner && RecipeCompositionItems.isEmptyPowerCell(output)) return;
        var inputs = new ArrayList<List<ItemStack>>();
        for (int i = 0; i < recipe.itemInputs.length; i++) {
            var input = recipe.itemInputs[i];
            if (input.isEmpty() || recipe.isCatalystInput(i)) continue;
            if (recipe.materialInputDisplay(i) != null) return;
            var tag = recipe.itemTag(i);
            if (tag == null) {
                inputs.add(List.of(input));
            } else {
                var choices = new ArrayList<ItemStack>();
                for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag))
                    choices.add(new ItemStack(holder.value(), input.getCount()));
                inputs.add(choices);
            }
        }
        if (inputs.isEmpty()) return;
        var incorporated = new ArrayList<MaterialComponent>();
        if (map == MachineRecipeMaps.Bath || map == MachineRecipeMaps.Assembler) {
            for (int i = 0; i < recipe.fluidInputs.length; i++) {
                if (recipe.isCatalystFluidInput(i) || recipe.isPresenceFluidInput(i) || recipe.fluidTag(i) != null) continue;
                var fluid = recipe.fluidInputs[i];
                var material = MoltenFluidHandler.material(fluid);
                if (material != null && !material.has(MaterialProperty.LIQUID))
                    incorporated.add(MaterialComponent.of(material, MoltenFluidHandler.ratio(material, fluid).toUnits(fluid.getAmount())));
            }
        }
        String source = recipe.source.isBlank()
                ? map.internalName + "/" + BuiltInRegistries.ITEM.getKey(output.getItem())
                : recipe.source;
        add(new Row(source, output, inputs, incorporated, false, firesClay));
    }

    private Optional<ItemComposition> resolve(Item item) {
        var previous = resolved.get(item);
        if (previous != null) return previous;
        if (!resolving.add(item)) return Optional.empty();
        try {
            var result = resolveRoutes(item);
            // A dependency blocked by a cycle may be available once its independent route resolves.
            if (result.isPresent() || !rows.containsKey(item) && !aliases.containsKey(item)) resolved.put(item, result);
            return result;
        } finally {
            resolving.remove(item);
        }
    }

    private Optional<ItemComposition> resolveRoutes(Item item) {
        var alias = aliases.get(item);
        if (alias != null) {
            return baseOrResolved(alias).map(data ->
                    ReversibleCraftingData.perItem(data.components(), 1, data.source()));
        }
        var routes = rows.get(item);
        if (routes == null) return originalComposition(item);
        routes.sort(Comparator.comparing(Row::id));
        var alternatives = new ArrayList<Map<GTMaterial, Long>>();
        for (var row : routes) {
            var amount = calculate(row);
            if (amount == null) return Optional.empty();
            alternatives.add(amounts(amount));
        }
        var source = String.join(", ", routes.stream().map(Row::id).distinct().toList());
        try {
            return Optional.of(ReversibleCraftingData.perItem(components(common(alternatives)), 1, source));
        } catch (ArithmeticException overflow) {
            return Optional.empty();
        }
    }

    private ItemComposition calculate(Row row) {
        if (row.inputs() == null || row.output().isDamaged() || ItemMaterialRegistry.hasStoredContents(row.output()))
            return null;
        var inputs = new ArrayList<>(row.incorporated());
        try {
            for (var choices : row.inputs()) {
                if (row.craftingTools() && choices.stream().anyMatch(CraftingItemCompositions::craftingTool)) continue;
                var alternatives = new ArrayList<Map<GTMaterial, Long>>();
                for (var stack : choices) {
                    var amount = inputAmounts(stack);
                    if (amount == null && choices.size() == 1) return null;
                    alternatives.add(amount == null ? Map.of() : amount);
                }
                var shared = common(alternatives);
                if (row.firesClay()) {
                    for (var entry : shared.entrySet()) {
                        var material = entry.getKey();
                        var target = material.getTargetSmeltingMaterial();
                        if (target != null && target.isValid())
                            inputs.add(MaterialComponent.of(target, UnitConversion.units(
                                    entry.getValue(), GTValues.U, material.getTargetSmeltingAmount(), false)));
                    }
                } else {
                    inputs.addAll(components(shared));
                }
            }
            return ReversibleCraftingData.perItem(inputs, row.output().getCount(), row.id());
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    private static boolean fixedKubeRecipe(CraftingRecipe recipe) {
        var serializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        return serializer != null
                && serializer.getNamespace().equals("kubejs")
                && CraftingCompositionSupport.hasFixedInputsAndOutput(recipe);
    }

    private static boolean craftingTool(ItemStack stack) {
        return stack.getItem() instanceof ElectricToolItem || GTToolHelper.craftDamage(stack) > 0;
    }

    private Map<GTMaterial, Long> inputAmounts(ItemStack stack) {
        if (stack.isEmpty() || stack.hasCraftingRemainingItem()
                || ItemMaterialRegistry.hasStoredContents(stack)) return Map.of();
        var item = stack.getItem();
        if (!stack.isDamageableItem() && ItemMaterialRuntimeFacts.compositionDurability(stack) != null)
            return Map.of();
        var data = baseOrResolved(item);
        if (data.isEmpty()) return rows.containsKey(item) ? null : Map.of();
        // A recipe can accept worn armor. Its last usable point is the guaranteed contribution.
        var composition = stack.isDamageableItem()
                ? ItemCompositionScaling.scale(data.get(), 1, stack.getMaxDamage())
                : data.get();
        var amount = amounts(composition);
        try {
            amount.replaceAll((material, value) -> Math.multiplyExact(value, stack.getCount()));
            return amount;
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    private Optional<ItemComposition> baseOrResolved(Item item) {
        var base = ItemMaterialRegistry.baseBeforeRecipeGeneration(item);
        return base.isPresent() || !tracked.contains(item) ? base : resolve(item);
    }

    private Optional<ItemComposition> originalComposition(Item item) {
        var parts = RecipeCompositionItems.originalInputs(item);
        if (parts.isEmpty()) return Optional.empty();
        var inputs = new ArrayList<MaterialComponent>();
        for (var stack : parts) {
            var data = inputAmounts(stack);
            if (data == null) return Optional.empty();
            inputs.addAll(components(data));
        }
        var source = "GT6 construction inputs for " + BuiltInRegistries.ITEM.getKey(item);
        return Optional.of(ReversibleCraftingData.perItem(inputs, 1, source));
    }

    private Map<GTMaterial, Long> common(List<Map<GTMaterial, Long>> alternatives) {
        Map<GTMaterial, Long> exact = null;
        for (var option : alternatives) exact = minimum(exact, option);
        if (exact == null) return Map.of();
        if (alternatives.size() == 1 || alternatives.stream().anyMatch(Map::isEmpty)) return exact;
        Map<GTMaterial, Long> equivalent = null;
        for (var option : alternatives) {
            var remainder = new HashMap<GTMaterial, Long>();
            for (var entry : option.entrySet()) {
                long amount = entry.getValue() - exact.getOrDefault(entry.getKey(), 0L);
                var canonical = commonMaterials.get(entry.getKey());
                if (canonical != null && amount > 0) remainder.merge(canonical, amount, Math::addExact);
            }
            equivalent = minimum(equivalent, remainder);
            if (equivalent.isEmpty()) break;
        }
        for (var entry : equivalent.entrySet()) exact.merge(entry.getKey(), entry.getValue(), Math::addExact);
        return exact;
    }

    private static Map<GTMaterial, GTMaterial> commonMaterials() {
        // GT6 reversing groups supply a common recovery target when the original input type varies.
        var materials = new HashMap<GTMaterial, GTMaterial>();
        bindGroup(materials, MaterialGroups.Rubber, "Rubber");
        bindGroup(materials, MaterialGroups.Plastic, "Plastic");
        bindGroup(materials, MaterialGroups.PlasticHard, "Plastic");
        bindGroup(materials, MaterialGroups.Cu, "Copper");
        bindGroup(materials, MaterialGroups.W, "Tungsten");
        bindGroup(materials, MaterialGroups.Iron, "Iron");
        materials.putAll(ElectronicsRecipes.circuitCrystalRecoveryMaterials());
        var silica = GTMaterialRegistry.get("SiliconDioxide");
        for (String name : List.of("NetherQuartz", "MilkyQuartz", "CertusQuartz", "SiliconDioxide"))
            materials.put(GTMaterialRegistry.get(name), silica);
        var clay = GTMaterialRegistry.get("Clay");
        materials.put(clay, clay);
        for (var row : DiggableRecipes.packable())
            if (row.block().startsWith("clay_")) materials.put(row.material(), clay);
        return materials;
    }

    private static void bindGroup(Map<GTMaterial, GTMaterial> materials, GTMaterial group, String target) {
        var material = GTMaterialRegistry.get(target);
        for (var member : MaterialGroups.members(group)) materials.put(member, material);
    }

    private static Map<GTMaterial, Long> amounts(ItemComposition data) {
        var result = new HashMap<GTMaterial, Long>();
        for (var component : data.components()) result.put(component.material(), component.amount());
        return result;
    }

    private static Map<GTMaterial, Long> minimum(Map<GTMaterial, Long> common, Map<GTMaterial, Long> other) {
        if (common == null) return new HashMap<>(other);
        common.keySet().retainAll(other.keySet());
        common.replaceAll((material, amount) -> Math.min(amount, other.get(material)));
        return common;
    }

    private static List<MaterialComponent> components(Map<GTMaterial, Long> amounts) {
        return amounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(GTMaterial::getName)))
                .map(entry -> MaterialComponent.of(entry.getKey(), entry.getValue()))
                .toList();
    }
}
