package gregtech.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.api.addon.material.DataKey;
import gregtech.compat.common.MaterialProcessDisplay;
import gregtech.content.registry.GTFluids;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.fluid.FluidVisualPolicy;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialProcessGraph.Port;
import gregtech.engine.material.MaterialProcessGraph.Role;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
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
    private MaterialProcessEmiRecipes() {}

    record Captured(MaterialProcessSnapshot snapshot, Map<String, List<MaterialProcessDisplay.Information>> information,
            Map<DataKey, EmiRecipe> links) {}

    static Captured capture(MaterialProcessSnapshot source, List<EmiRecipe> recipes,
            Map<EmiRecipeCategory, List<EmiIngredient>> workstations) {
        var builder = source.viewerBuilder();
        var information = new LinkedHashMap<String, List<MaterialProcessDisplay.Information>>();
        var links = new LinkedHashMap<DataKey, EmiRecipe>();
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<EmiRecipe, Boolean>());
        for (int index = 0; index < recipes.size(); index++) {
            EmiRecipe recipe = recipes.get(index);
            if (recipe instanceof MaterialProcessEmiRecipe || !seen.add(recipe)) continue;
            // EMI's tree contract describes a complete transformation; lookup slots alone do not.
            if (!recipe.supportsRecipeTree() || recipe.getInputs().isEmpty() || recipe.getOutputs().isEmpty()) {
                builder.omitted();
                information(source, recipe, index, information, links);
                continue;
            }
            if (recipe instanceof MachineEmiRecipe machine) {
                builder.machine(machine.recipeMap(), machine.nativeRecipe());
                continue;
            }
            var inputs = ports(builder, recipe.getInputs(), Role.CONSUMED);
            var catalysts = ports(builder, recipe.getCatalysts(), Role.CATALYST);
            var outputs = ports(builder, recipe.getOutputs(), Role.UNKNOWN);
            // Dropping an unsupported slot would turn a partial recipe into an invented route.
            if (inputs == null || catalysts == null || outputs == null) {
                builder.omitted();
                information(source, recipe, index, information, links);
                continue;
            }
            inputs.addAll(catalysts);
            var category = recipe.getCategory();
            var machine = workstations.getOrDefault(category, List.of()).stream()
                    .flatMap(ingredient -> ingredient.getEmiStacks().stream())
                    .map(EmiStack::getItemStack).filter(stack -> !stack.isEmpty())
                    .findFirst().orElse(ItemStack.EMPTY);
            var reference = new DataKey("gt6ce", "process/emi/" + index + "/"
                    + Long.toUnsignedString(source.snapshot().publication().revision(), 36) + "/"
                    + Long.toUnsignedString(source.snapshot().sessionGeneration(), 36));
            builder.recipe(reference, category.getName().getString(), machine, inputs, outputs,
                    List.of("EMI category: " + category.getId(), "EMI recipe: " + recipe.getId(),
                            "matching constraints and remainders: see original recipe"));
        }
        information.replaceAll((material, entries) -> List.copyOf(entries));
        return new Captured(builder.build(), Map.copyOf(information), Map.copyOf(links));
    }

    private static void information(MaterialProcessSnapshot source, EmiRecipe recipe, int index,
            Map<String, List<MaterialProcessDisplay.Information>> information, Map<DataKey, EmiRecipe> links) {
        var inputs = informationIcons(java.util.stream.Stream.concat(recipe.getInputs().stream(), recipe.getCatalysts().stream()).toList());
        var outputs = informationIcons(recipe.getOutputs());
        var materials = new java.util.LinkedHashSet<>(inputs.keySet());
        materials.addAll(outputs.keySet());
        if (materials.isEmpty()) return;
        var reference = new DataKey("gt6ce", "process/emi_information/" + index + "/"
                + Long.toUnsignedString(source.snapshot().publication().revision(), 36) + "/"
                + Long.toUnsignedString(source.snapshot().sessionGeneration(), 36));
        ItemStack icon = recipe.getOutputs().stream().filter(stack -> stack != null).map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty()).findFirst().orElse(new ItemStack(Items.BOOK));
        Component name = recipe.getCategory().getName();
        int materialIndex = 0;
        for (String material : materials) {
            var ports = new ArrayList<MaterialProcessDisplay.Port>();
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
                    ItemMaterialRegistry.get(item).ifPresent(composition -> composition.components().forEach(component ->
                            informationIcon(result, component.material().resolve().getName(), item)));
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
            List<? extends EmiIngredient> ingredients, Role role) {
        var ports = new ArrayList<Port>();
        for (EmiIngredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty() || ingredient.getAmount() <= 0) return null;
            var candidates = new ArrayList<Port>();
            for (EmiStack stack : ingredient.getEmiStacks()) {
                if (stack == null || stack.isEmpty()) return null;
                if (role == Role.UNKNOWN && (!Float.isFinite(stack.getChance()) || stack.getChance() <= 0 || stack.getChance() > 1))
                    return null;
                long amount = ingredient.getAmount();
                var chance = role == Role.UNKNOWN ? chance(stack.getChance()) : OptionalInt.empty();
                if (stack.getKey() instanceof Item) {
                    var item = stack.getItemStack();
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
