package com.gregtech.gregtech.loaders.a;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.recipe.GTToolAssemblyRecipe;
import com.gregtech.gregtech.recipe.GTToolPatternRecipe;
import com.gregtech.gregtech.recipe.GTToolHeadRecipe;
import com.gregtech.gregtech.recipe.GTToolRecipeSerializers;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers GT6 tools and the tool crafting recipe serializers. */
public record Loader_Tools(IEventBus bus) implements IGTLoader {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, GregTech.MODID);

    /** Head + handle assembly (GT6 {@code AdvancedCraftingTool}). */
    public static final RegistryObject<RecipeSerializer<GTToolAssemblyRecipe>> TOOL_ASSEMBLY =
            RECIPE_SERIALIZERS.register("tool_assembly", () -> GTToolRecipeSerializers.ASSEMBLY);

    /** Shaped one-piece tool rows (GT6 {@code OreProcessing_Tool} recipes). */
    public static final RegistryObject<RecipeSerializer<GTToolPatternRecipe>> TOOL_CRAFTING =
            RECIPE_SERIALIZERS.register("tool_crafting", () -> GTToolRecipeSerializers.CRAFTING);

    /** Shaped tool head rows (GT6 {@code mToolHeadRecipes}). */
    public static final RegistryObject<RecipeSerializer<GTToolHeadRecipe>> TOOL_HEAD =
            RECIPE_SERIALIZERS.register("tool_head", () -> GTToolRecipeSerializers.HEAD);

    public static final RegistryObject<RecipeSerializer<com.gregtech.gregtech.recipe.ToolShapedRecipe>> TOOL_SHAPED =
            RECIPE_SERIALIZERS.register("tool_shaped", () -> com.gregtech.gregtech.recipe.ToolShapedRecipe.SERIALIZER);

    /** GT6's huge wooden axle consumes a finite 1000 mB creosote container. */
    public static final RegistryObject<RecipeSerializer<com.gregtech.gregtech.recipe.CreosoteAxleRecipe>> CREOSOTE_AXLE =
            RECIPE_SERIALIZERS.register("creosote_axle", () -> com.gregtech.gregtech.recipe.CreosoteAxleRecipe.SERIALIZER);

    /** GT6 bottle rows consume a real 1000 mB container and return the empty vessel. */
    public static final RegistryObject<RecipeSerializer<com.gregtech.gregtech.recipe.FiniteBottleFillingRecipe>> FINITE_BOTTLE_FILLING =
            RECIPE_SERIALIZERS.register("finite_bottle_filling", () -> com.gregtech.gregtech.recipe.FiniteBottleFillingRecipe.SERIALIZER);

    @Override
    public void run() {
        com.gregtech.gregtech.recipe.CreosoteAxleRecipe.registerIngredient();
        com.gregtech.gregtech.recipe.FiniteBottleFillingRecipe.registerIngredient();
        GTToolItems.ITEMS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    public static ResourceLocation recipeId(GTToolType type) {
        return ResourceLocation.fromNamespaceAndPath(GregTech.MODID, "tools/" + type.id());
    }
}
