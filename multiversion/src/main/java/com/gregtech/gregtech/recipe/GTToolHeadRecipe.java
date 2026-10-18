package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * GT6's shaped tool-<em>head</em> row ({@code Loader_Tools:293-313}, the {@code mToolHeadRecipes} of
 * every head-based tool), e.g. {@code PICKAXE = {"PII","f h"}} with the gem variant
 * {@code {"CGG","f  "}}.
 *
 * <p>The row consumes the plates/ingots of one material and yields that material's head, so a player
 * can reach a tool head in the crafting grid as well as through the extruder.</p>
 */
public final class GTToolHeadRecipe extends GTToolPatternRecipe {

    public GTToolHeadRecipe(ResourceLocation id, GTToolType type, int patternIndex) {
        super(id, type, GTToolRecipes.heads(type).get(patternIndex), true);
    }

    @Override
    public int patternIndex() {
        return GTToolRecipes.heads(type).indexOf(pattern);
    }

    public MaterialPrefix headPrefix() {
        return GTToolRecipes.headPrefix(type);
    }

    @Override
    protected ItemStack assembleFrom(Match match) {
        return GTItems.getStack(GTToolRecipes.headPrefix(type), match.material(), 1);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GTToolRecipeSerializers.HEAD;
    }
}
