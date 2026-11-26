package gregtech.content.recipe;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else {
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
//?}

/** Version-compatibility helpers for custom crafting recipe serializers and ingredients. */
public final class CraftingRecipeCompat {
    private CraftingRecipeCompat() {}

//? if >=1.21.1 {
    /*public static <T extends ShapelessRecipe> RecipeSerializer<T> wrapShapelessSerializer(
            Function<ShapelessRecipe, T> wrap, Function<T, ShapelessRecipe> unwrap) {
        MapCodec<T> codec = new ShapelessRecipe.Serializer().codec().xmap(wrap, unwrap);
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return ByteBufCodecs.fromCodecWithRegistries(codec.codec());
            }
        };
    }

    public static <T extends ShapedRecipe> RecipeSerializer<T> wrapShapedSerializer(
            Function<ShapedRecipe, T> wrap, Function<T, ShapedRecipe> unwrap) {
        MapCodec<T> codec = new ShapedRecipe.Serializer().codec().xmap(wrap, unwrap);
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return ByteBufCodecs.fromCodecWithRegistries(codec.codec());
            }
        };
    }

    public static <T extends CustomRecipe> RecipeSerializer<T> simpleCustomSerializer(
            BiFunction<@Nullable ResourceLocation, CraftingBookCategory, T> factory) {
        return new SimpleCraftingRecipeSerializer<>(category -> factory.apply(null, category));
    }

    public static @Nullable Object customIngredientOf(Ingredient ingredient) {
        return ingredient.getCustomIngredient();
    }

    public static JsonElement ingredientToJson(Ingredient ingredient) {
        return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow();
    }

    public static RecipeManager newScratchRecipeManager(RegistryAccess access) {
        return new RecipeManager(access);
    }

    public static int dedupRecipes(RecipeManager manager, RegistryAccess access) {
        var deduped = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        int duplicates = FormConversionCraftingRecipeInjector.dedupGenerated(
                deduped, access, entry -> ((RecipeHolder<?>) entry).id(), entry -> ((RecipeHolder<?>) entry).value());
        if (duplicates > 0) manager.replaceRecipes(deduped);
        return duplicates;
    }
*///?} else {
    public static <T extends ShapelessRecipe> RecipeSerializer<T> wrapShapelessSerializer(
            Function<ShapelessRecipe, T> wrap, Function<T, ShapelessRecipe> unwrap) {
        return new RecipeSerializer<>() {
            private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();

            @Override
            public T fromJson(ResourceLocation id, JsonObject json) {
                return wrap.apply(vanilla.fromJson(id, json));
            }

            @Override
            public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
                return wrap.apply(vanilla.fromNetwork(id, buffer));
            }

            @Override
            public void toNetwork(FriendlyByteBuf buffer, T recipe) {
                vanilla.toNetwork(buffer, unwrap.apply(recipe));
            }
        };
    }

    public static <T extends ShapedRecipe> RecipeSerializer<T> wrapShapedSerializer(
            Function<ShapedRecipe, T> wrap, Function<T, ShapedRecipe> unwrap) {
        return new RecipeSerializer<>() {
            private final ShapedRecipe.Serializer vanilla = new ShapedRecipe.Serializer();

            @Override
            public T fromJson(ResourceLocation id, JsonObject json) {
                return wrap.apply(vanilla.fromJson(id, json));
            }

            @Override
            public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
                return wrap.apply(vanilla.fromNetwork(id, buffer));
            }

            @Override
            public void toNetwork(FriendlyByteBuf buffer, T recipe) {
                vanilla.toNetwork(buffer, unwrap.apply(recipe));
            }
        };
    }

    public static <T extends CustomRecipe> RecipeSerializer<T> simpleCustomSerializer(
            BiFunction<@Nullable ResourceLocation, CraftingBookCategory, T> factory) {
        return new SimpleCraftingRecipeSerializer<>(factory::apply);
    }

    public static @Nullable Object customIngredientOf(Ingredient ingredient) {
        return ingredient;
    }

    public static JsonElement ingredientToJson(Ingredient ingredient) {
        return ingredient.toJson();
    }

    public static RecipeManager newScratchRecipeManager(RegistryAccess access) {
        return new RecipeManager();
    }

    public static int dedupRecipes(RecipeManager manager, RegistryAccess access) {
        var deduped = new ArrayList<Recipe<?>>(manager.getRecipes());
        int duplicates = FormConversionCraftingRecipeInjector.dedupGenerated(
                deduped, access, entry -> ((Recipe<?>) entry).getId(), entry -> (Recipe<?>) entry);
        if (duplicates > 0) manager.replaceRecipes(deduped);
        return duplicates;
    }
//?}
}
