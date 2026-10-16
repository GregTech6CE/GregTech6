package com.gregtech.gregtech.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeDocuments;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeFields;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeRemovalSpec;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeSpec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** A datapack marker. It is never crafted; the overlay copies it onto a machine recipe map. */
public final class MachineDatapackRecipe implements Recipe<CraftingInput> {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, "gregtech");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineDatapackRecipe>> ADDITION_TYPE =
            TYPES.register("machine", () -> named(MachineRecipeFields.TYPE_ADDITION));
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineDatapackRecipe>> REMOVAL_TYPE =
            TYPES.register("machine_removal", () -> named(MachineRecipeFields.TYPE_REMOVAL));

    private final MachineRecipeSpec addition;
    private final MachineRecipeRemovalSpec removal;

    private MachineDatapackRecipe(MachineRecipeSpec addition, MachineRecipeRemovalSpec removal) {
        this.addition = addition;
        this.removal = removal;
    }

    public static MachineDatapackRecipe addition(MachineRecipeSpec spec) {
        return new MachineDatapackRecipe(spec, null);
    }

    public static MachineDatapackRecipe removal(MachineRecipeRemovalSpec spec) {
        return new MachineDatapackRecipe(null, spec);
    }

    public MachineRecipeSpec addition() { return addition; }
    public MachineRecipeRemovalSpec removal() { return removal; }

    @Override public boolean matches(CraftingInput input, Level level) { return false; }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.create(); }
    @Override public RecipeSerializer<?> getSerializer() { return addition != null ? ADDITION : REMOVAL; }
    @Override public RecipeType<?> getType() { return addition != null ? ADDITION_TYPE.get() : REMOVAL_TYPE.get(); }

    public static final RecipeSerializer<MachineDatapackRecipe> ADDITION = serializer(true);
    public static final RecipeSerializer<MachineDatapackRecipe> REMOVAL = serializer(false);

    private static RecipeSerializer<MachineDatapackRecipe> serializer(boolean addition) {
        MapCodec<MachineDatapackRecipe> codec = new MapCodec<>() {
            @Override public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(
                        MachineRecipeFields.MAP,
                        MachineRecipeFields.ITEM_INPUTS,
                        MachineRecipeFields.FLUID_INPUTS,
                        MachineRecipeFields.ITEM_OUTPUTS,
                        MachineRecipeFields.FLUID_OUTPUTS,
                        addition ? MachineRecipeFields.EU_PER_TICK : MachineRecipeFields.CLEAR,
                        addition ? MachineRecipeFields.DURATION : MachineRecipeFields.CLEAR,
                        addition ? MachineRecipeFields.SPECIAL : MachineRecipeFields.CLEAR
                ).distinct().map(ops::createString);
            }

            @Override public <T> DataResult<MachineDatapackRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
                try {
                    var values = values(ops, input);
                    return DataResult.success(addition
                            ? MachineDatapackRecipe.addition(MachineRecipeDocuments.addition(values))
                            : MachineDatapackRecipe.removal(MachineRecipeDocuments.removal(values)));
                } catch (RuntimeException failure) {
                    return DataResult.error(() -> failure.getMessage() == null ? failure.toString() : failure.getMessage());
                }
            }

            @Override public <T> RecordBuilder<T> encode(MachineDatapackRecipe recipe, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                String json = addition
                        ? MachineRecipeDocuments.canonical(recipe.addition)
                        : MachineRecipeDocuments.canonical(recipe.removal);
                JsonObject object = JsonParser.parseString(json).getAsJsonObject();
                for (var entry : object.entrySet())
                    prefix.add(ops.createString(entry.getKey()), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
                return prefix;
            }
        };
        StreamCodec<RegistryFriendlyByteBuf, MachineDatapackRecipe> stream = StreamCodec.of(
                (buffer, recipe) -> buffer.writeUtf(addition
                        ? MachineRecipeDocuments.canonical(recipe.addition)
                        : MachineRecipeDocuments.canonical(recipe.removal)),
                buffer -> addition
                        ? MachineDatapackRecipe.addition(MachineRecipeDocuments.addition(buffer.readUtf()))
                        : MachineDatapackRecipe.removal(MachineRecipeDocuments.removal(buffer.readUtf())));
        return new RecipeSerializer<>() {
            @Override public MapCodec<MachineDatapackRecipe> codec() { return codec; }
            @Override public StreamCodec<RegistryFriendlyByteBuf, MachineDatapackRecipe> streamCodec() { return stream; }
        };
    }

    private static RecipeType<MachineDatapackRecipe> named(String name) {
        return new RecipeType<>() {
            @Override public String toString() { return name; }
        };
    }

    private static <T> Map<String, Object> values(DynamicOps<T> ops, MapLike<T> input) {
        var map = new LinkedHashMap<String, Object>();
        input.entries().forEach(pair -> {
            String key = ops.getStringValue(pair.getFirst()).result().orElseThrow();
            map.put(key, value(ops.convertTo(JsonOps.INSTANCE, pair.getSecond())));
        });
        return map;
    }

    private static Object value(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) {
            var map = new LinkedHashMap<String, Object>();
            for (var entry : element.getAsJsonObject().entrySet()) map.put(entry.getKey(), value(entry.getValue()));
            return map;
        }
        if (element.isJsonArray()) {
            var list = new ArrayList<Object>();
            JsonArray array = element.getAsJsonArray();
            for (var child : array) list.add(value(child));
            return list;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive.getAsString();
    }
}
