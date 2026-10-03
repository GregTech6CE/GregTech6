package com.gregtech.gregtech.recipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
public final class NeoRecipeSerializers {
 private NeoRecipeSerializers(){}
 public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"gregtech");
 public static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENTS=DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES,"gregtech");
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.common.crafting.IngredientType<?>,net.neoforged.neoforge.common.crafting.IngredientType<FiniteBottleFillingRecipe.ContainerIngredient>> FINITE_CONTAINER=INGREDIENTS.register("finite_fluid_container_1000",()->new net.neoforged.neoforge.common.crafting.IngredientType<>(FiniteBottleFillingRecipe.ContainerIngredient.CODEC));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.common.crafting.IngredientType<?>,net.neoforged.neoforge.common.crafting.IngredientType<CreosoteAxleRecipe.ContainerIngredient>> CREOSOTE_CONTAINER=INGREDIENTS.register("creosote_container_1000",()->new net.neoforged.neoforge.common.crafting.IngredientType<>(CreosoteAxleRecipe.ContainerIngredient.CODEC));
 static{SERIALIZERS.register("tool_shapeless",()->ToolShapelessRecipe.SERIALIZER);SERIALIZERS.register("creosote_axle",()->CreosoteAxleRecipe.SERIALIZER);SERIALIZERS.register("finite_bottle_filling",()->FiniteBottleFillingRecipe.SERIALIZER);SERIALIZERS.register("electric_tool_assembly",()->PoweredToolAssemblyRecipe.SERIALIZER);SERIALIZERS.register("tool_shaped",()->ToolShapedRecipe.SERIALIZER);SERIALIZERS.register("tool_assembly",()->GTToolRecipeSerializers.ASSEMBLY);SERIALIZERS.register("tool_crafting",()->GTToolRecipeSerializers.CRAFTING);SERIALIZERS.register("tool_head",()->GTToolRecipeSerializers.HEAD);}
 public static void register(IEventBus bus){INGREDIENTS.register(bus);SERIALIZERS.register(bus);}
}
