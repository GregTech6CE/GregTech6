package gregtech.bootstrap.content;

import gregtech.GregTech;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.recipe.GTToolAssemblyRecipe;
import gregtech.content.recipe.GTToolHeadRecipe;
import gregtech.content.recipe.GTToolPatternRecipe;
import gregtech.content.recipe.GTToolRecipeSerializers;
import gregtech.content.registry.GTToolItems;
import gregtech.engine.tool.GTToolType;
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

    public static final RegistryObject<RecipeSerializer<gregtech.content.recipe.SpikeConversionRecipe>>
            SPIKE_CONVERSION = RECIPE_SERIALIZERS.register(
                    "spike_conversion", () -> gregtech.content.recipe.SpikeConversionRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<gregtech.content.recipe.ToolShapelessRecipe>> TOOL_SHAPELESS =
            RECIPE_SERIALIZERS.register("tool_shapeless", () -> gregtech.content.recipe.ToolShapelessRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<gregtech.content.recipe.ToolShapedRecipe>> TOOL_SHAPED =
            RECIPE_SERIALIZERS.register("tool_shaped", () -> gregtech.content.recipe.ToolShapedRecipe.SERIALIZER);

    /** GT6's huge wooden axle consumes a finite 1000 mB creosote container. */
    public static final RegistryObject<RecipeSerializer<gregtech.content.recipe.CreosoteAxleRecipe>> CREOSOTE_AXLE =
            RECIPE_SERIALIZERS.register("creosote_axle", () -> gregtech.content.recipe.CreosoteAxleRecipe.SERIALIZER);

    /** GT6 bottle rows consume a real 1000 mB container and return the empty vessel. */
    public static final RegistryObject<RecipeSerializer<gregtech.content.recipe.FiniteBottleFillingRecipe>>
            FINITE_BOTTLE_FILLING = RECIPE_SERIALIZERS.register(
                    "finite_bottle_filling", () -> gregtech.content.recipe.FiniteBottleFillingRecipe.SERIALIZER);

    @Override
    public void run() {
        gregtech.content.recipe.CreosoteAxleRecipe.registerIngredient();
        gregtech.content.recipe.FiniteBottleFillingRecipe.registerIngredient();
        gregtech.content.recipe.BlockTagCraftingIngredient.registerIngredient();
        gregtech.content.recipe.QuarterFluidCraftingIngredient.registerIngredient();
        GTToolItems.ITEMS.register(bus);
        RECIPE_SERIALIZERS.register("book_binding", () -> gregtech.content.recipe.BookBindingRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("machine", () -> gregtech.content.recipe.MachineDatapackRecipe.ADDITION);
        RECIPE_SERIALIZERS.register("machine_removal", () -> gregtech.content.recipe.MachineDatapackRecipe.REMOVAL);
        gregtech.content.recipe.MachineDatapackRecipe.TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    public static ResourceLocation recipeId(GTToolType type) {
        return ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, "tools/" + type.id());
    }
}
