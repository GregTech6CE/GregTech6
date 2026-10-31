package gregtech.compat.emi;

import gregtech.engine.material.MaterialDisplayBinding;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** One cache per EMI reload; mutable amounts/chances are copied, alias lists are shared. */
final class MachineEmiIngredients {
    private final EmiItemCandidates candidates;

    MachineEmiIngredients(EmiItemCandidates candidates) {
        this.candidates = candidates;
    }
    private final Map<Item, EmiIngredient> plain = new IdentityHashMap<>();
    private final Map<Item, java.util.List<EmiStack>> unitAliases = new IdentityHashMap<>();
    private record FluidKey(net.minecraft.world.level.material.Fluid fluid, Object data, int amount) {}
    private record FluidTagKey(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag, int amount) {}
    // Container aliases are one bucket even when the native fluid amount is larger; keep that ratio.
    private final Map<FluidKey, java.util.List<EmiStack>> fluidAliases = new java.util.HashMap<>();
    private final Map<FluidTagKey, java.util.List<EmiStack>> fluidTagAliases = new java.util.HashMap<>();
    private final Map<net.minecraft.tags.TagKey<Item>, EmiIngredient> unitTags = new java.util.HashMap<>();

    EmiIngredient fluidIngredient(gregtech.engine.recipe.Recipe recipe, int index) {
        var fluid = recipe.mFluidInputs[index];
        java.util.List<EmiStack> aliases;
        var tag = recipe.fluidTag(index);
        if (tag != null) {
            aliases = fluidTagAliases.computeIfAbsent(new FluidTagKey(tag, fluid.getAmount()), ignored -> {
                var choices = new java.util.LinkedHashSet<EmiStack>();
                for (var holder : net.minecraft.core.registries.BuiltInRegistries.FLUID.getTagOrEmpty(tag))
                    if (holder.value() != net.minecraft.world.level.material.Fluids.EMPTY)
                        choices.addAll(fluidAliases(new FluidStack(holder.value(), fluid.getAmount())));
                return java.util.List.copyOf(choices);
            });
        } else aliases = fluidAliases(fluid);
        return aliases.isEmpty() ? EmiStack.EMPTY : new MachineEmiIngredient(aliases, 1);
    }
//? if >=1.21.1 {
    /*private java.util.List<EmiStack> fluidAliases(net.neoforged.neoforge.fluids.FluidStack fluid) {
        var key = new FluidKey(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount());
*///?} else {
    private java.util.List<EmiStack> fluidAliases(net.minecraftforge.fluids.FluidStack fluid) {
        var key = new FluidKey(fluid.getFluid(), fluid.getTag() == null ? null : fluid.getTag().copy(), fluid.getAmount());
//?}
        return fluidAliases.computeIfAbsent(key, ignored -> {
            var choices = new java.util.LinkedHashSet<EmiStack>();
//? if >=1.21.1 {
            /*choices.add(EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()));
*///?} else {
            choices.add(EmiStack.of(fluid.getFluid(), fluid.getTag(), fluid.getAmount()));
//?}
            for (var container : gregtech.engine.fluid.FluidContainerDisplayBinding.containers(fluid)) choices.add(EmiStack.of(container));
            return java.util.List.copyOf(choices);
        });
    }
    EmiIngredient ingredient(gregtech.engine.recipe.Recipe recipe, int index) {
        var unit = recipe.materialInputDisplay(index);
        if (unit != null) {
            var stack = recipe.mInputs[index];
            var aliases = unitAliases.computeIfAbsent(stack.getItem(), ignored -> {
                var all = new ArrayList<EmiStack>();
                all.add(EmiStack.of(stack.copyWithCount(1)));
                for (var alias : MaterialDisplayBinding.alternatives(stack)) all.add(EmiStack.of(alias.copyWithCount(1)));
                return java.util.List.copyOf(all);
            });
            return new MaterialUnitEmiIngredient(EmiStack.of(stack.copyWithCount(1)), aliases, unit,
                    unit.tag() == null ? null : unitTags.computeIfAbsent(unit.tag(), EmiIngredient::of));
        }
        if (recipe.itemTag(index) != null)
            return EmiIngredient.of(recipe.itemTag(index), recipe.mInputs[index].getCount());
        var choices = recipe.viewerInputAlternatives(index);
        if (choices.isEmpty() && (recipe.mExactItemInputs || recipe.hasItemInputGuard(index))) return EmiStack.of(recipe.mInputs[index]);
        if (choices.isEmpty()) return ingredient(recipe.mInputs[index]);
        var expanded = new ArrayList<ItemStack>();
        for (var stack : choices) {
            if (!recipe.mExactItemInputs && !recipe.hasItemInputGuard(index)) {
                var preferred = gregtech.engine.material.MaterialUnification.canonical(stack);
                if (preferred.getItem() != stack.getItem()) expanded.add(preferred.copyWithCount(1));
            }
            expanded.add(stack.copyWithCount(1));
        }
        return new MachineEmiIngredient(candidates.of(expanded), recipe.mInputs[index].getCount());
    }
    EmiIngredient ingredient(ItemStack stack) {
//? if >=1.21.1 {
        /*if (!stack.getComponentsPatch().isEmpty()) return create(stack);
*///?} else {
        if (stack.hasTag()) return create(stack);
//?}
        return plain.computeIfAbsent(stack.getItem(), item -> create(new ItemStack(item)))
                .copy().setAmount(stack.getCount());
    }
    private static EmiIngredient create(ItemStack stack) {
        var aliases = MaterialDisplayBinding.alternatives(stack);
        var preferred = gregtech.engine.material.MaterialUnification.canonical(stack);
        boolean unified = preferred.getItem() != stack.getItem();
        if (aliases.isEmpty() && !unified) return EmiStack.of(stack);
        var alternatives = new ArrayList<EmiStack>(aliases.size() + 1);
        if (unified) alternatives.add(EmiStack.of(preferred.copyWithCount(1)));
        alternatives.add(EmiStack.of(stack.copyWithCount(1)));
        for (var alias : aliases) alternatives.add(EmiStack.of(alias.copyWithCount(1)));
        return new MachineEmiIngredient(alternatives, stack.getCount());
    }
}
