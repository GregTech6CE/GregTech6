package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * GT6's flint and tinder rows ({@code Loader_Tools:225-247}): {@code "T "/" F"} — a striking material
 * above a piece of flint, with {@code T} being the first of the material's gem, rock or nugget form
 * (GT6 registers a separate row per case, in that order).
 */
public final class GTFlintAndTinderRecipe extends GTToolPatternRecipe {

    public GTFlintAndTinderRecipe(ResourceLocation id) {
        super(id, GTToolType.FLINT_AND_TINDER, GTToolRecipes.shaped(GTToolType.FLINT_AND_TINDER).get(0));
    }

    @Override
    public int patternIndex() {
        return 0;
    }

    /**
     * GT6 accepts any of the material's striking forms for the {@code T} key; the base class knows one
     * prefix per letter, so the alternatives are listed here.
     */
    @Override
    protected GTMaterial classify(ItemStack stack, char letter, @Nullable GTMaterial material) {
        if (letter != 'T') return super.classify(stack, letter, material);
        var form = com.gregtech.gregtech.api.material.MaterialEquivalence.form(stack);
        if (form == null) return null;
        MaterialPrefix prefix = form.prefix();
        if (prefix != MaterialPrefix.gem && prefix != MaterialPrefix.gemChipped
                && prefix != MaterialPrefix.rockGt && prefix != MaterialPrefix.nugget) {
            return null;
        }
        GTMaterial found = form.material().resolve();
        if (found == null) return null;
        return material == null || material == found ? found : null;
    }

    /**
     * GT6 registers these rows per material list (gem, rock, nugget, stone) without requiring the
     * material to be a tool material, so the tool-type gate does not apply here.
     */
    @Override
    protected boolean acceptsMaterial(GTMaterial material) {
        return material != null && material.isValid();
    }

    @Override
    protected ItemStack assembleFrom(Match match) {
        GTMaterial flint = GTMaterialRegistry.get("Flint");
        return GTToolItem.create(type, match.material(), flint == null ? match.material() : flint);
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return GTToolRecipeSerializers.CRAFTING;
    }
}
