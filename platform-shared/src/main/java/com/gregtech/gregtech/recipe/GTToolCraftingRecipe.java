package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * GT6's shaped one-piece-tool row ({@code Loader_Tools:305-320}), e.g.
 * {@code WRENCH = {"PhP"," P "," P "}}.
 *
 * <p>The port used to register these tools as one shapeless assembly, which both lost the shape and
 * kept them out of the crafting-table recipe list; this row carries GT6's pattern and is a real
 * {@code ShapedRecipe}, so the recipe viewer shows it under crafting.</p>
 */
public final class GTToolCraftingRecipe extends GTToolPatternRecipe {

    public GTToolCraftingRecipe(ResourceLocation id, GTToolType type, int patternIndex) {
        super(id, type, GTToolRecipes.shaped(type).get(patternIndex));
    }

    /** Index of this row in {@link GTToolRecipes#shaped(GTToolType)}. */
    @Override
    public int patternIndex() {
        return GTToolRecipes.shaped(type).indexOf(pattern);
    }

    @Override
    protected ItemStack assembleFrom(Match match) {
        return GTToolItem.create(type, match.material(),type==GTToolType.POCKET_MULTITOOL?com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Blue"):match.handle());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GTToolRecipeSerializers.CRAFTING;
    }
}
