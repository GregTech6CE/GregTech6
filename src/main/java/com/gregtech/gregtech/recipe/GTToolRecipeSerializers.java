package com.gregtech.gregtech.recipe;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serializers for the tool crafting rows, so they can be synced to the client like any other recipe.
 *
 * <p>The rows themselves are built from {@link GTToolRecipes} at server start
 * ({@code Loader_ToolCraftingRecipes}); the JSON form exists for completeness and for pack authors who
 * want to re-add a single row ({@code {"type":"gregtech:tool_crafting","tool":"wrench","pattern":0}}).</p>
 */
public final class GTToolRecipeSerializers {

    private GTToolRecipeSerializers() {}

    /** Head + handle assembly (GT6 {@code AdvancedCraftingTool}), shapeless. */
    public static final RecipeSerializer<GTToolAssemblyRecipe> ASSEMBLY = new AssemblySerializer();

    /** Shaped one-piece tool row (GT6 {@code OreProcessing_Tool} recipes). */
    public static final RecipeSerializer<GTToolPatternRecipe> CRAFTING = new CraftingSerializer();

    /** Shaped tool head row (GT6 {@code mToolHeadRecipes}). */
    public static final RecipeSerializer<GTToolHeadRecipe> HEAD = new HeadSerializer();

    /** {@code gregtech:tool_assembly} — the id the port's data pack used before §29. */
    public static final ResourceLocation ASSEMBLY_ID = GregTech.id("tool_assembly");
    /** {@code gregtech:tool_crafting} */
    public static final ResourceLocation CRAFTING_ID = GregTech.id("tool_crafting");
    /** {@code gregtech:tool_head} */
    public static final ResourceLocation HEAD_ID = GregTech.id("tool_head");

    private static int intOrDefault(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }

    /** Shared implementation: one tool type plus the pattern index. */
    private abstract static class Base<T extends Recipe<?>> implements RecipeSerializer<T> {
        abstract T create(ResourceLocation id, GTToolType type, int pattern);

        @Override
        public T fromJson(ResourceLocation id, JsonObject json) {
            return create(id, GTToolType.byId(json.get("tool").getAsString()), intOrDefault(json, "pattern"));
        }

        @Override
        public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return create(id, GTToolType.byId(buffer.readUtf()), buffer.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            GTToolType type = ((GTToolPatternRecipe) recipe).toolType();
            buffer.writeUtf(type.id());
            buffer.writeVarInt(((GTToolPatternRecipe) recipe).patternIndex());
        }
    }

    private static final class AssemblySerializer extends Base<GTToolAssemblyRecipe> {
        @Override
        GTToolAssemblyRecipe create(ResourceLocation id, GTToolType type, int pattern) {
            return new GTToolAssemblyRecipe(id, type);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GTToolAssemblyRecipe recipe) {
            buffer.writeUtf(recipe.toolType().id());
            buffer.writeVarInt(0);
        }
    }

    private static final class CraftingSerializer extends Base<GTToolPatternRecipe> {
        @Override
        GTToolPatternRecipe create(ResourceLocation id, GTToolType type, int pattern) {
            if (type == GTToolType.FLINT_AND_TINDER) return new GTFlintAndTinderRecipe(id);
            return new GTToolCraftingRecipe(id, type, pattern);
        }
    }

    private static final class HeadSerializer extends Base<GTToolHeadRecipe> {
        @Override
        GTToolHeadRecipe create(ResourceLocation id, GTToolType type, int pattern) {
            return new GTToolHeadRecipe(id, type, pattern);
        }
    }
}
