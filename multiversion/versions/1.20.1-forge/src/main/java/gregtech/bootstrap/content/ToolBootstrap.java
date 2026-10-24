package gregtech.bootstrap.content;

import gregtech.GregTech;
import gregtech.api.tool.GTToolType;
import gregtech.bootstrap.InitializationStep;
import gregtech.recipe.GTToolAssemblyRecipe;
import gregtech.recipe.GTToolPatternRecipe;
import gregtech.recipe.GTToolHeadRecipe;
import gregtech.recipe.GTToolRecipeSerializers;
import gregtech.registry.GTToolItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers GT6 tools and the tool crafting recipe serializers. */
public record ToolBootstrap(IEventBus bus) implements InitializationStep {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, GregTech.NAMESPACE);

    /** Head + handle assembly (GT6 {@code AdvancedCraftingTool}). */
    public static final RegistryObject<RecipeSerializer<GTToolAssemblyRecipe>> TOOL_ASSEMBLY =
            RECIPE_SERIALIZERS.register("tool_assembly", () -> GTToolRecipeSerializers.ASSEMBLY);

    /** Shaped one-piece tool rows (GT6 {@code OreProcessing_Tool} recipes). */
    public static final RegistryObject<RecipeSerializer<GTToolPatternRecipe>> TOOL_CRAFTING =
            RECIPE_SERIALIZERS.register("tool_crafting", () -> GTToolRecipeSerializers.CRAFTING);

    /** Shaped tool head rows (GT6 {@code mToolHeadRecipes}). */
    public static final RegistryObject<RecipeSerializer<GTToolHeadRecipe>> TOOL_HEAD =
            RECIPE_SERIALIZERS.register("tool_head", () -> GTToolRecipeSerializers.HEAD);

    public static final RegistryObject<RecipeSerializer<gregtech.recipe.SpikeConversionRecipe>> SPIKE_CONVERSION =
            RECIPE_SERIALIZERS.register("spike_conversion", () -> gregtech.recipe.SpikeConversionRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<gregtech.recipe.ToolShapelessRecipe>> TOOL_SHAPELESS =
            RECIPE_SERIALIZERS.register("tool_shapeless", () -> gregtech.recipe.ToolShapelessRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<gregtech.recipe.ToolShapedRecipe>> TOOL_SHAPED =
            RECIPE_SERIALIZERS.register("tool_shaped", () -> gregtech.recipe.ToolShapedRecipe.SERIALIZER);

    /** GT6's huge wooden axle consumes a finite 1000 mB creosote container. */
    public static final RegistryObject<RecipeSerializer<gregtech.recipe.CreosoteAxleRecipe>> CREOSOTE_AXLE =
            RECIPE_SERIALIZERS.register("creosote_axle", () -> gregtech.recipe.CreosoteAxleRecipe.SERIALIZER);

    /** GT6 bottle rows consume a real 1000 mB container and return the empty vessel. */
    public static final RegistryObject<RecipeSerializer<gregtech.recipe.FiniteBottleFillingRecipe>> FINITE_BOTTLE_FILLING =
            RECIPE_SERIALIZERS.register("finite_bottle_filling", () -> gregtech.recipe.FiniteBottleFillingRecipe.SERIALIZER);

    @Override
    public void run() {
        gregtech.recipe.CreosoteAxleRecipe.registerIngredient();
        gregtech.recipe.FiniteBottleFillingRecipe.registerIngredient();
        GTToolItems.ITEMS.register(bus);
        RECIPE_SERIALIZERS.register("book_binding",()->gregtech.recipe.BookBindingRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("machine", () -> gregtech.recipe.MachineDatapackRecipe.ADDITION);
        RECIPE_SERIALIZERS.register("machine_removal", () -> gregtech.recipe.MachineDatapackRecipe.REMOVAL);
        gregtech.recipe.MachineDatapackRecipe.TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    public static ResourceLocation recipeId(GTToolType type) {
        return ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "tools/" + type.id());
    }
}
