package gregtech.compat.emi;

import com.mojang.logging.LogUtils;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiPatternCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.api.addon.material.DataKey;
import gregtech.compat.common.MaterialProcessDisplay;
import gregtech.content.registry.GTFluids;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.fluid.FluidVisualPolicy;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialProcessGraph.Port;
import gregtech.engine.material.MaterialProcessGraph.AmountUnit;
import gregtech.engine.material.MaterialProcessGraph.Resource;
import gregtech.engine.material.MaterialProcessGraph.Role;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Processing rows and related information share a source, but never share graph semantics. */
final class MaterialProcessEmiRecipes {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private MaterialProcessEmiRecipes() {}

    record Captured(MaterialProcessSnapshot snapshot, Map<String, List<MaterialProcessDisplay.Information>> information,
            Map<DataKey, EmiRecipe> links) {}

    private record SlotKey(Resource resource, Role role, AmountUnit unit, OptionalInt chance, long stochasticBatch) {}

    private record ProcessKey(ResourceLocation category, Map<SlotKey, BigInteger> inputs, Map<SlotKey, BigInteger> outputs) {}

    static Captured capture(MaterialProcessSnapshot source, List<EmiRecipe> recipes,
            Map<EmiRecipeCategory, List<EmiIngredient>> workstations) {
        var builder = source.viewerBuilder();
        var information = new LinkedHashMap<String, List<MaterialProcessDisplay.Information>>();
        var links = new LinkedHashMap<DataKey, EmiRecipe>();
        var informationIds = new HashSet<ResourceLocation>();
        var processIds = new HashSet<ResourceLocation>();
        var processes = new HashSet<ProcessKey>();
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<EmiRecipe, Boolean>());
        for (int index = 0; index < recipes.size(); index++) {
            EmiRecipe recipe = recipes.get(index);
            try {
                if (recipe instanceof MaterialProcessEmiRecipe || !seen.add(recipe)) continue;
                // Ingredient disambiguation lists tag members, not material-specific guidance.
                if (recipe instanceof EmiIngredientRecipe && !(recipe instanceof MaterialUnitTagEmiRecipe)) continue;
                // JEMI wraps JEI tag browsers as plain recipes, retaining the registry's tag_recipes category.
                if (recipe.getCategory().getId().getPath().startsWith("tag_recipes/")) continue;
                // Unit templates opt out of automatic crafting, but carry GT's complete processing semantics.
                if (recipe instanceof MachineEmiRecipe machine
                        && builder.machine(machine.recipeMap(), machine.nativeRecipe())) continue;
                if (informationOnly(recipe) || recipe.getInputs().isEmpty() || recipe.getOutputs().isEmpty()) {
                    builder.omitted();
                    information(source, recipe, index, information, links, informationIds);
                    continue;
                }
                boolean crafting = recipe instanceof EmiCraftingRecipe
                        || recipe.getCategory().getId().equals(VanillaEmiRecipeCategories.CRAFTING.getId());
                var inputs = ports(builder, recipe.getInputs(), Role.CONSUMED, crafting);
                var catalysts = ports(builder, recipe.getCatalysts(), Role.CATALYST, false);
                var outputs = ports(builder, recipe.getOutputs(), Role.UNKNOWN, false);
                // Dropping an unsupported slot would turn a partial recipe into an invented route.
                if (inputs == null || catalysts == null || outputs == null || inputs.isEmpty() || outputs.isEmpty()) {
                    builder.omitted();
                    information(source, recipe, index, information, links, informationIds);
                    continue;
                }
                inputs.addAll(catalysts);
                var category = recipe.getCategory();
                // IDs catch repeated registrations; slot multisets also catch equivalent synthetic wrappers.
                var id = recipe.getId();
                if (id != null && processIds.contains(id)) continue;
                boolean remainders = java.util.stream.Stream.concat(recipe.getInputs().stream(), recipe.getCatalysts().stream())
                        .flatMap(ingredient -> ingredient.getEmiStacks().stream()).anyMatch(stack -> !stack.getRemainder().isEmpty());
                var process = remainders ? null : new ProcessKey(category.getId(), slots(inputs), slots(outputs));
                if (process != null && processes.contains(process)) continue;
                var machine = workstations.getOrDefault(category, List.of()).stream()
                        .flatMap(ingredient -> ingredient.getEmiStacks().stream())
                        .map(EmiStack::getItemStack).filter(stack -> !stack.isEmpty())
                        .findFirst().orElseGet(() -> category.getId().equals(VanillaEmiRecipeCategories.CRAFTING.getId())
                                ? new ItemStack(Items.CRAFTING_TABLE) : ItemStack.EMPTY);
                var reference = new DataKey("gt6ce", "process/emi/" + index + "/"
                        + Long.toUnsignedString(source.snapshot().publication().revision(), 36) + "/"
                        + Long.toUnsignedString(source.snapshot().sessionGeneration(), 36));
                builder.recipe(reference, category.getId().toString(), category.getName().getString(), machine, inputs, outputs,
                        List.of("EMI category: " + category.getId(), "EMI recipe: " + recipe.getId(),
                                "matching constraints and remainders: see original recipe"), recipe.supportsRecipeTree());
                if (id != null) processIds.add(id);
                if (process != null) processes.add(process);
            } catch (RuntimeException failure) {
                builder.omitted();
                LOGGER.warn("Discarded invalid EMI material process entry at index {}", index, failure);
            }
        }
        information.replaceAll((material, entries) -> List.copyOf(entries));
        return new Captured(builder.build(), Map.copyOf(information), Map.copyOf(links));
    }

    private static boolean informationOnly(EmiRecipe recipe) {
        return recipe instanceof MachineEmiRecipe
                || recipe instanceof EmiInfoRecipe
                || recipe instanceof EmiPatternCraftingRecipe
                || recipe instanceof StructureEmiRecipe
                || recipe instanceof WorldgenEmiRecipe
                || recipe instanceof LootEmiRecipe
                || recipe instanceof MaterialUnitTagEmiRecipe
                || recipe instanceof MoldChiselEmiRecipe
                || recipe instanceof WoodenBucketLookupRecipe
                || recipe.getCategory().getId().equals(VanillaEmiRecipeCategories.INFO.getId());
    }

    private static Map<SlotKey, BigInteger> slots(List<Port> ports) {
        var result = new HashMap<SlotKey, BigInteger>();
        for (Port port : ports) {
            // Two independent random output slots are not one double-sized random batch.
            long batch = port.output() && port.chance().orElse(0) != 10_000 ? port.amount().value() : 0;
            result.merge(new SlotKey(port.resource(), port.role(), port.amount().unit(), port.chance(), batch),
                    BigInteger.valueOf(port.amount().value()), BigInteger::add);
        }
        return Map.copyOf(result);
    }

    private static void information(MaterialProcessSnapshot source, EmiRecipe recipe, int index,
            Map<String, List<MaterialProcessDisplay.Information>> information, Map<DataKey, EmiRecipe> links,
            Set<ResourceLocation> informationIds) {
        var inputs = informationIcons(java.util.stream.Stream.concat(recipe.getInputs().stream(), recipe.getCatalysts().stream()).toList());
        var outputs = informationIcons(recipe.getOutputs());
        var materials = new java.util.LinkedHashSet<>(inputs.keySet());
        materials.addAll(outputs.keySet());
        if (materials.isEmpty()) return;
        // EMI IDs identify pages, even when plugins register a fresh object for the same page.
        if (recipe.getId() != null && !informationIds.add(recipe.getId())) return;
        var reference = new DataKey("gt6ce", "process/emi_information/" + index + "/"
                + Long.toUnsignedString(source.snapshot().publication().revision(), 36) + "/"
                + Long.toUnsignedString(source.snapshot().sessionGeneration(), 36));
        Component name = recipe.getCategory().getName();
        int materialIndex = 0;
        for (String material : materials) {
            var ports = new ArrayList<MaterialProcessDisplay.Port>();
            var materialIcons = outputs.getOrDefault(material, inputs.getOrDefault(material, Map.of()));
            ItemStack icon = materialIcons.values().iterator().next();
            for (boolean output : List.of(false, true)) {
                var icons = (output ? outputs : inputs).getOrDefault(material, Map.of()).values().stream().toList();
                if (icons.isEmpty()) continue;
                var key = new DataKey(reference.namespace(), reference.path() + "/index/" + materialIndex + "/" + output);
                ports.add(new MaterialProcessDisplay.Port(key, material, icons.get(0).getHoverName(), icons,
                        output ? MaterialProcessDisplay.Role.UNKNOWN : MaterialProcessDisplay.Role.PRESENCE,
                        false, Component.empty(), Component.empty(), OptionalInt.empty(), List.of()));
            }
            var entry = new MaterialProcessDisplay.Information(reference, name, icon,
                    List.of(icon.getHoverName(), Component.translatable("gt6ce.jei.material_process.information_hint")), ports);
            information.computeIfAbsent(material, ignored -> new ArrayList<>()).add(entry);
            materialIndex++;
        }
        links.put(reference, recipe);
    }

    private static Map<String, Map<String, ItemStack>> informationIcons(List<? extends EmiIngredient> ingredients) {
        var result = new LinkedHashMap<String, Map<String, ItemStack>>();
        for (EmiIngredient ingredient : ingredients) {
            if (ingredient == null) continue;
            for (EmiStack stack : ingredient.getEmiStacks()) {
                if (stack == null || stack.isEmpty()) continue;
                if (stack.getKey() instanceof Item) {
                    var item = stack.getItemStack();
                    ItemMaterialRegistry.get(item).ifPresent(composition -> {
                        var material = composition.primaryMaterial().resolve();
                        if (material.isValid()) informationIcon(result, material.getName(), item);
                    });
                } else if (stack.getKey() instanceof Fluid fluid) {
                    var entry = GTFluids.entryForFluid(fluid);
                    if (entry == null) continue;
                    var material = FluidVisualPolicy.material(entry).resolve();
                    if (!material.isValid()) continue;
                    var nativeFluid = fluid(stack, fluid);
//? if >=1.21.1 {
                    /*var lookup = gregtech.engine.fluid.FluidDisplayBinding.currentLookup();
                    if (lookup != null) informationIcon(result, material.getName(), gregtech.engine.fluid.FluidDisplayBinding.display(lookup, nativeFluid));
*///?} else {
                    informationIcon(result, material.getName(), gregtech.engine.fluid.FluidDisplayBinding.display(nativeFluid));
//?}
                }
            }
        }
        return result;
    }

    private static void informationIcon(Map<String, Map<String, ItemStack>> icons, String material, ItemStack stack) {
        if (stack.isEmpty()) return;
        var values = icons.computeIfAbsent(material, ignored -> new LinkedHashMap<>());
        if (values.size() < MaterialProcessDisplay.MAX_ICONS)
            values.putIfAbsent(MaterialProcessSnapshot.itemDisplayState(stack), stack.copyWithCount(1));
    }

    private static List<Port> ports(MaterialProcessSnapshot.ViewerBuilder builder,
            List<? extends EmiIngredient> ingredients, Role role, boolean padding) {
        var ports = new ArrayList<Port>();
        for (EmiIngredient ingredient : ingredients) {
            // Shaped crafting pads its grid with EMPTY. An unresolved nonempty ingredient is not padding.
            if (padding && ingredient == EmiStack.EMPTY) continue;
            if (role == Role.CATALYST && ingredient != null && ingredient.getAmount() == 0)
                ingredient = ingredient.copy().setAmount(1);
            if (ingredient == null || ingredient.isEmpty() || ingredient.getAmount() < 0
                    || ingredient.getAmount() == 0 && role != Role.CATALYST) return null;
            var candidates = new ArrayList<Port>();
            for (EmiStack stack : ingredient.getEmiStacks()) {
                if (stack == null || stack.isEmpty()) return null;
                if (role == Role.UNKNOWN && (!Float.isFinite(stack.getChance()) || stack.getChance() <= 0 || stack.getChance() > 1))
                    return null;
                long amount = Math.max(1, ingredient.getAmount());
                var chance = role == Role.UNKNOWN ? chance(stack.getChance()) : OptionalInt.empty();
                if (stack.getKey() instanceof Item) {
                    var item = stack.copy().setAmount(1).getItemStack();
                    if (item.isEmpty()) return null;
                    candidates.add(builder.item(item, role, amount, chance));
                } else if (stack.getKey() instanceof Fluid fluid) {
                    candidates.add(builder.fluid(fluid(stack, fluid), role, amount, chance));
                } else return null;
            }
            if (candidates.isEmpty()) return null;
            ports.add(builder.alternatives(candidates));
        }
        return ports;
    }

    private static FluidStack fluid(EmiStack stack, Fluid fluid) {
//? if >=1.21.1 {
        /*return new FluidStack(fluid.builtInRegistryHolder(), 1, stack.getComponentChanges());
*///?} else {
        return new FluidStack(fluid, 1, stack.getNbt());
//?}
    }

    private static OptionalInt chance(float chance) {
        if (!Float.isFinite(chance) || chance <= 0 || chance > 1) return OptionalInt.empty();
        return OptionalInt.of(Math.max(1, Math.round(chance * 10_000)));
    }
}
