package gregtech.engine.addon;

import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** Maps whose addon machines roll two item outputs as one choice. */
public final class ExclusiveOutputMaps {
    private ExclusiveOutputMaps() {}

    public static boolean enabled(RecipeMap map) {
        if (map == null) return false;
        for (BasicMachineSpec spec : AddonMachineRuntime.specs())
            if (spec.exclusiveOutputs() && spec.recipeMap() == map) return true;
        return false;
    }

    public static int nonEmptyItems(ItemStack[] outputs) {
        if (outputs == null) return 0;
        int count = 0;
        for (ItemStack stack : outputs) if (stack != null && !stack.isEmpty()) count++;
        return count;
    }

    /** Static and display rows on an exclusive map must be a pair. Other maps are left alone. */
    public static void requireRow(RecipeMap map, ItemStack[] outputs) {
        if (!enabled(map)) return;
        int declared = outputs == null ? 0 : outputs.length;
        try {
            ExclusiveOutputRules.requirePair(declared, nonEmptyItems(outputs));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("Recipe map '" + map.mNameInternal + "' " + failure.getMessage(), failure);
        }
    }

    /** After addon recipes are installed. Names the addon and the machine when a row is not a pair. */
    public static void audit() {
        for (BasicMachineSpec spec : AddonMachineRuntime.specs()) {
            if (!spec.exclusiveOutputs() || spec.recipeMap() == null) continue;
            RecipeMap map = spec.recipeMap();
            for (Recipe recipe : map.mRecipeList) {
                int declared = recipe.mOutputs == null ? 0 : recipe.mOutputs.length;
                int present = nonEmptyItems(recipe.mOutputs);
                if (ExclusiveOutputRules.isPair(declared, present)) continue;
                throw new IllegalStateException("Addon '" + spec.namespace() + "' machine '" + spec.id()
                        + "' on map '" + map.mNameInternal
                        + "' exclusive outputs need exactly 2 item outputs: " + present);
            }
        }
    }
}
