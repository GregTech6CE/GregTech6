package com.gregtech.gregtech.internal.addon;

import com.gregtech.gregtech.api.addon.platform.AddonFluids;
import com.gregtech.gregtech.api.addon.platform.AddonLoader;
import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the Forge fluid and recipe entry. */
public final class AddonLoaderServices {
    private AddonLoaderServices() {}

    public static void bind() {
        AddonLoader.bind(new Services());
    }

    private static final class Services implements AddonLoader.Services {
        @Override public FluidStack fluid(String name, AddonFluids.Kind kind, int millibuckets) {
            if (name == null || kind == null || millibuckets <= 0) return null;
            if (kind == AddonFluids.Kind.STILL) {
                FluidStack stack = GTFluids.stack(name, millibuckets);
                return stack == null || stack.isEmpty() ? null : stack;
            }
            return flowing(name, millibuckets);
        }

        @Override public AddonRecipes.Map map(String name) {
            if (name == null) return null;
            RecipeMap map = RecipeMap.RECIPE_MAPS.get(name);
            return map == null ? null : new MapView(map);
        }
    }

    private static FluidStack flowing(String name, int millibuckets) {
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get(name);
        if (entry == null) return null;
        if (entry.isVanillaWater()) return new FluidStack(Fluids.FLOWING_WATER, millibuckets);
        if (entry.isVanillaLava()) return new FluidStack(Fluids.FLOWING_LAVA, millibuckets);
        RegistryObject<net.minecraft.world.level.material.Fluid> fluid = GTFluids.flowing(name);
        if (fluid == null || !fluid.isPresent()) return null;
        return new FluidStack(fluid.get(), millibuckets);
    }

    private static final class MapView implements AddonRecipes.Map {
        private final RecipeMap map;

        private MapView(RecipeMap map) { this.map = map; }

        @Override public String name() { return map.mNameInternal; }

        @Override public AddonRecipes.Line add(AddonRecipes.Line line) {
            Recipe recipe = map.addRecipe(recipe(line));
            return recipe == null ? null : line(recipe);
        }

        @Override public AddonRecipes.Line addDisplay(AddonRecipes.Line line) {
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
                AddonRecipes.Line line = provider.find(items, fluids);
                return line == null ? null : recipe(line);
            });
        }

        @Override public AddonRecipes.Line find(List<ItemStack> items, List<FluidStack> fluids,
                                                 boolean needsOutputs, int inputSlots, int outputSlots) {
            Recipe recipe = map.findRecipe(items == null ? List.of() : items, fluids == null ? List.of() : fluids,
                    needsOutputs, inputSlots, outputSlots);
            return recipe == null ? null : line(recipe);
        }
    }

    private static Recipe recipe(AddonRecipes.Line line) {
        return new Recipe(line.itemInputs(), line.itemOutputs(), null, null,
                line.fluidInputs(), line.fluidOutputs(), line.duration(), line.euPerTick(), line.special());
    }

    private static AddonRecipes.Line line(Recipe recipe) {
        return new AddonRecipes.Line(recipe.mInputs, recipe.mOutputs, recipe.mFluidInputs, recipe.mFluidOutputs,
                recipe.mDuration, recipe.mEUt, recipe.mSpecialValue);
    }
}
