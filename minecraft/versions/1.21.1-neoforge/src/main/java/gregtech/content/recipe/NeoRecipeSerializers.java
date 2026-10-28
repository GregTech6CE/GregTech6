package gregtech.content.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoRecipeSerializers {
    private NeoRecipeSerializers() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "gt6ce");
    public static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENTS =
            DeferredRegister.create(
                    net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES, "gt6ce");
    public static final net.neoforged.neoforge.registries.DeferredHolder<
                    net.neoforged.neoforge.common.crafting.IngredientType<?>,
                    net.neoforged.neoforge.common.crafting.IngredientType<
                            FiniteBottleFillingRecipe.ContainerIngredient>>
            FINITE_CONTAINER = INGREDIENTS.register(
                    "finite_fluid_container_1000",
                    () -> new net.neoforged.neoforge.common.crafting.IngredientType<>(
                            FiniteBottleFillingRecipe.ContainerIngredient.CODEC));
    public static final net.neoforged.neoforge.registries.DeferredHolder<
                    net.neoforged.neoforge.common.crafting.IngredientType<?>,
                    net.neoforged.neoforge.common.crafting.IngredientType<CreosoteAxleRecipe.ContainerIngredient>>
            CREOSOTE_CONTAINER = INGREDIENTS.register(
                    "creosote_container_1000",
                    () -> new net.neoforged.neoforge.common.crafting.IngredientType<>(
                            CreosoteAxleRecipe.ContainerIngredient.CODEC));
    public static final net.neoforged.neoforge.registries.DeferredHolder<
                    net.neoforged.neoforge.common.crafting.IngredientType<?>,
                    net.neoforged.neoforge.common.crafting.IngredientType<BlockTagCraftingIngredient>>
            BLOCK_TAG = INGREDIENTS.register(
                    "block_tag",
                    () -> new net.neoforged.neoforge.common.crafting.IngredientType<>(
                            BlockTagCraftingIngredient.CODEC));
    public static final net.neoforged.neoforge.registries.DeferredHolder<
                    net.neoforged.neoforge.common.crafting.IngredientType<?>,
                    net.neoforged.neoforge.common.crafting.IngredientType<QuarterFluidCraftingIngredient>>
            QUARTER_CONTAINER = INGREDIENTS.register(
                    "finite_fluid_container_250",
                    () -> new net.neoforged.neoforge.common.crafting.IngredientType<>(
                            QuarterFluidCraftingIngredient.CODEC));

    static {
        SERIALIZERS.register("spike_conversion", () -> SpikeConversionRecipe.SERIALIZER);
        SERIALIZERS.register("book_binding", () -> BookBindingRecipe.SERIALIZER);
        SERIALIZERS.register("tool_shapeless", () -> ToolShapelessRecipe.SERIALIZER);
        SERIALIZERS.register("creosote_axle", () -> CreosoteAxleRecipe.SERIALIZER);
        SERIALIZERS.register("finite_bottle_filling", () -> FiniteBottleFillingRecipe.SERIALIZER);
        SERIALIZERS.register("electric_tool_assembly", () -> PoweredToolAssemblyRecipe.SERIALIZER);
        SERIALIZERS.register("tool_shaped", () -> ToolShapedRecipe.SERIALIZER);
        SERIALIZERS.register("tool_assembly", () -> GTToolRecipeSerializers.ASSEMBLY);
        SERIALIZERS.register("tool_crafting", () -> GTToolRecipeSerializers.CRAFTING);
        SERIALIZERS.register("tool_head", () -> GTToolRecipeSerializers.HEAD);
        SERIALIZERS.register("machine", () -> MachineDatapackRecipe.ADDITION);
        SERIALIZERS.register("machine_removal", () -> MachineDatapackRecipe.REMOVAL);
    }

    public static void register(IEventBus bus) {
        MachineDatapackRecipe.TYPES.register(bus);
        INGREDIENTS.register(bus);
        SERIALIZERS.register(bus);
    }
}
