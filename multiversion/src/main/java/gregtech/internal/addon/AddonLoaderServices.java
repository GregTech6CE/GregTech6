package gregtech.internal.addon;

import gregtech.api.addon.RecipeKeptInputs;
import gregtech.api.addon.RecipeOutputChances;
import gregtech.api.addon.platform.AddonFluids;
import gregtech.api.addon.platform.AddonLoader;
import gregtech.api.addon.platform.AddonRecipes;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.level.material.Fluid;
*///?} else {
//?}
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the Forge fluid and recipe entry. */

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the NeoForge fluid and recipe entry. */
public final class AddonLoaderServices {
    private AddonLoaderServices() {}

    public static void bind() {
        AddonLoader.bind(new Services());
    }

    private static final class Services implements AddonLoader.Services {
        @Override public FluidStack fluid(String name, AddonFluids.Kind kind, int millibuckets) {
            if (name == null || kind == null || millibuckets <= 0) return FluidStack.EMPTY;
            if (kind == AddonFluids.Kind.STILL) {
                FluidStack stack = GTFluids.stack(name, millibuckets);
                return stack == null || stack.isEmpty() ? FluidStack.EMPTY : stack;
            }
            return flowing(name, millibuckets);
        }

        @Override public AddonRecipes.Map map(String name) {
            if (name == null) return null;
            RecipeMap map = RecipeMap.RECIPE_MAPS.get(name);
            return map == null ? null : new MapView(map);
        }

        @Override public boolean gas(net.minecraft.world.level.material.Fluid fluid) {
            return fluid != null && gregtech.internal.fluid.FluidHazards.isGas(fluid);
        }

        @Override public boolean acid(net.minecraft.world.level.material.Fluid fluid) {
            return fluid != null && gregtech.internal.fluid.FluidHazards.isAcid(fluid);
        }

        @Override public boolean plasma(net.minecraft.world.level.material.Fluid fluid) {
            return fluid != null && gregtech.internal.fluid.FluidHazards.isPlasma(fluid);
        }

        @Override public boolean magic(net.minecraft.world.level.material.Fluid fluid) {
            return fluid != null && gregtech.internal.fluid.FluidHazards.isMagic(fluid);
        }
    }

    private static FluidStack flowing(String name, int millibuckets) {
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get(name);
        if (entry == null) return FluidStack.EMPTY;
        if (entry.isVanillaWater()) return new FluidStack(Fluids.FLOWING_WATER, millibuckets);
        if (entry.isVanillaLava()) return new FluidStack(Fluids.FLOWING_LAVA, millibuckets);
//? if >=1.21.1 {
/*        DeferredHolder<Fluid, ? extends Fluid> fluid = GTFluids.flowing(name);
        if (fluid == null || !fluid.isBound()) return FluidStack.EMPTY;
*///?} else {
        RegistryObject<net.minecraft.world.level.material.Fluid> fluid = GTFluids.flowing(name);
        if (fluid == null || !fluid.isPresent()) return FluidStack.EMPTY;
//?}
        return new FluidStack(fluid.get(), millibuckets);
    }

    /** Live view over the registry entry found by the call that created it; not cached. */
    private static final class MapView implements AddonRecipes.Map {
        private final RecipeMap map;

        private MapView(RecipeMap map) { this.map = map; }

        @Override public String name() { return map.mNameInternal; }

        @Override public AddonRecipes.Line add(AddonRecipes.Line line) {
            gregtech.internal.addon.ExclusiveOutputMaps.requireRow(map, line.itemOutputs());
            Recipe recipe = map.addRecipe(recipe(line));
            return recipe == null ? null : line(recipe);
        }

        @Override public AddonRecipes.Line addDisplay(AddonRecipes.Line line) {
            gregtech.internal.addon.ExclusiveOutputMaps.requireRow(map, line.itemOutputs());
            Recipe recipe = map.addFakeRecipe(false, recipe(line));
            return recipe == null ? null : line(recipe);
        }

        @Override public AddonRecipes.Line collision(AddonRecipes.Line line) {
            Recipe recipe = map.findCollision(recipe(line));
            return recipe == null ? null : line(recipe);
        }

        @Override public boolean hasItemInput(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            Collection<Recipe> recipes = map.mRecipeItemMap.get(stack.getItem());
            return recipes != null && recipes.stream().anyMatch(recipe -> Arrays.stream(recipe.mInputs)
                    .anyMatch(candidate -> candidate != null && candidate.getItem() == stack.getItem()));
        }

        @Override public void addDynamic(AddonRecipes.Dynamic provider) {
            if (provider == null) throw new IllegalArgumentException("dynamic recipe provider");
            map.addDynamicRecipes((items, fluids) -> {
                AddonRecipes.Line line = provider.find(itemSnapshot(items), fluidSnapshot(fluids));
                return line == null ? null : recipe(line);
            });
        }

        @Override public AddonRecipes.Line find(List<ItemStack> items, List<FluidStack> fluids,
                                                 boolean needsOutputs, int inputSlots, int outputSlots) {
            Recipe recipe = map.findRecipe(itemSnapshot(items), fluidSnapshot(fluids),
                    needsOutputs, inputSlots, outputSlots);
            return recipe == null ? null : line(recipe);
        }
    }

    private static List<ItemStack> itemSnapshot(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return List.of();
        List<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) copy.add(stack == null ? null : stack.copy());
        return Collections.unmodifiableList(copy);
    }

    private static List<FluidStack> fluidSnapshot(List<FluidStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return List.of();
        List<FluidStack> copy = new ArrayList<>(stacks.size());
        for (FluidStack stack : stacks) copy.add(stack == null ? null : stack.copy());
        return Collections.unmodifiableList(copy);
    }

    private static Recipe recipe(AddonRecipes.Line line) {
        ItemStack[] inputs = line.itemInputs();
        for (int i = 0; i < inputs.length; i++) {
            ItemStack stack = inputs[i];
            RecipeKeptInputs.requirePositiveCount(stack == null || stack.isEmpty() ? 0 : stack.getCount(), i);
        }
        FluidStack[] fluidInputs = line.fluidInputs();
        for (int i = 0; i < fluidInputs.length; i++) {
            FluidStack stack = fluidInputs[i];
            if (stack == null || stack.getFluid() == Fluids.EMPTY)
                throw new IllegalArgumentException("Fluid input " + i + " is empty");
            RecipeKeptInputs.requirePositiveFluid(stack.getAmount(), i);
        }
        int[] outputChances = line.outputChances();
        long[] chances = new long[outputChances.length];
        for (int i = 0; i < chances.length; i++) chances[i] = outputChances[i];
        return new Recipe(inputs, line.itemOutputs(), null, chances,
                fluidInputs, line.fluidOutputs(), line.duration(), line.euPerTick(), line.special())
                .withCatalystInputs(line.keptInputs())
                .withCatalystFluidInputs(line.keptFluidInputs())
                .withPresenceFluidInputs(line.presenceFluidInputs());
    }

    private static AddonRecipes.Line line(Recipe recipe) {
        int outputs = recipe.mOutputs == null ? 0 : recipe.mOutputs.length;
        int[] chances = new int[outputs];
        for (int i = 0; i < outputs; i++)
            chances[i] = recipe.mChances != null && i < recipe.mChances.length
                    ? (int) recipe.mChances[i] : RecipeOutputChances.CERTAIN;
        return AddonRecipes.Line.read(recipe.mInputs, recipe.mOutputs, recipe.mFluidInputs, recipe.mFluidOutputs,
                recipe.mDuration, recipe.mEUt, recipe.mSpecialValue, chances, recipe.explicitCatalystInputs(),
                recipe.explicitCatalystFluidInputs(), recipe.explicitPresenceFluidInputs());
    }
}
