package gregtech.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
//? if >=1.21.1 {
/*import com.google.gson.JsonParser;
*///?} else {
import com.google.gson.JsonParseException;
//?}
import com.google.gson.JsonPrimitive;
//? if >=1.21.1 {
/**///?} else {
import com.google.gson.JsonSyntaxException;
//?}
import gregtech.internal.recipe.data.MachineRecipeDocuments;
//? if >=1.21.1 {
/*import gregtech.internal.recipe.data.MachineRecipeFields;
*///?} else {
//?}
import gregtech.internal.recipe.data.MachineRecipeRemovalSpec;
import gregtech.internal.recipe.data.MachineRecipeSpec;
//? if >=1.21.1 {
/*import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.HolderLookup;
*///?} else {
//?}
import net.minecraft.core.NonNullList;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
*///?} else {
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
//?}
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/**///?} else {
import net.minecraft.world.inventory.CraftingContainer;
//?}
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
*///?} else {
//?}
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
//? if >=1.21.1 {
/*import java.util.stream.Stream;
*///?} else {
//?}

/** A datapack marker. It is never crafted; the overlay copies it onto a machine recipe map. */
//? if >=1.21.1 {
/*public final class MachineDatapackRecipe implements Recipe<CraftingInput> {
*///?} else {
public final class MachineDatapackRecipe implements Recipe<CraftingContainer> {
//?}
    public static final DeferredRegister<RecipeType<?>> TYPES =
//? if >=1.21.1 {
/*            DeferredRegister.create(Registries.RECIPE_TYPE, "gt6ce");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineDatapackRecipe>> ADDITION_TYPE =
            TYPES.register("machine", () -> named(MachineRecipeFields.TYPE_ADDITION));
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineDatapackRecipe>> REMOVAL_TYPE =
            TYPES.register("machine_removal", () -> named(MachineRecipeFields.TYPE_REMOVAL));
*///?} else {
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "gt6ce");
    public static final RegistryObject<RecipeType<MachineDatapackRecipe>> ADDITION_TYPE =
            TYPES.register("machine", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath("gt6ce", "machine")));
    public static final RegistryObject<RecipeType<MachineDatapackRecipe>> REMOVAL_TYPE =
            TYPES.register("machine_removal", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_removal")));
//?}

//? if >=1.21.1 {
/**///?} else {
    private final ResourceLocation id;
//?}
    private final MachineRecipeSpec addition;
    private final MachineRecipeRemovalSpec removal;

//? if >=1.21.1 {
/*    private MachineDatapackRecipe(MachineRecipeSpec addition, MachineRecipeRemovalSpec removal) {
*///?} else {
    private MachineDatapackRecipe(ResourceLocation id, MachineRecipeSpec addition, MachineRecipeRemovalSpec removal) {
        this.id = id;
//?}
        this.addition = addition;
        this.removal = removal;
    }

//? if >=1.21.1 {
/*    public static MachineDatapackRecipe addition(MachineRecipeSpec spec) {
        return new MachineDatapackRecipe(spec, null);
*///?} else {
    public static MachineDatapackRecipe addition(ResourceLocation id, MachineRecipeSpec spec) {
        return new MachineDatapackRecipe(id, spec, null);
//?}
    }

//? if >=1.21.1 {
/*    public static MachineDatapackRecipe removal(MachineRecipeRemovalSpec spec) {
        return new MachineDatapackRecipe(null, spec);
*///?} else {
    public static MachineDatapackRecipe removal(ResourceLocation id, MachineRecipeRemovalSpec spec) {
        return new MachineDatapackRecipe(id, null, spec);
//?}
    }

    public MachineRecipeSpec addition() { return addition; }
    public MachineRecipeRemovalSpec removal() { return removal; }

//? if >=1.21.1 {
/*    @Override public boolean matches(CraftingInput input, Level level) { return false; }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
*///?} else {
    @Override public boolean matches(CraftingContainer container, Level level) { return false; }
    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess access) { return ItemStack.EMPTY; }
//?}
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
//? if >=1.21.1 {
/*    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
*///?} else {
    @Override public ItemStack getResultItem(RegistryAccess access) { return ItemStack.EMPTY; }
//?}
    @Override public boolean isSpecial() { return true; }
//? if >=1.21.1 {
/*    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.create(); }
*///?} else {
    @Override public ResourceLocation getId() { return id; }
//?}
    @Override public RecipeSerializer<?> getSerializer() { return addition != null ? ADDITION : REMOVAL; }
    @Override public RecipeType<?> getType() { return addition != null ? ADDITION_TYPE.get() : REMOVAL_TYPE.get(); }
//? if >=1.21.1 {
/**///?} else {
    @Override public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() { return NonNullList.create(); }
//?}

//? if >=1.21.1 {
/*    public static final RecipeSerializer<MachineDatapackRecipe> ADDITION = serializer(true);
    public static final RecipeSerializer<MachineDatapackRecipe> REMOVAL = serializer(false);
*///?} else {
    public static final RecipeSerializer<MachineDatapackRecipe> ADDITION = new RecipeSerializer<>() {
        @Override public MachineDatapackRecipe fromJson(ResourceLocation id, JsonObject json) {
            try { return MachineDatapackRecipe.addition(id, MachineRecipeDocuments.addition(values(json))); }
            catch (RuntimeException failure) { throw new JsonSyntaxException(failure.getMessage(), failure); }
        }
        @Override public MachineDatapackRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return MachineDatapackRecipe.addition(id, MachineRecipeDocuments.addition(buffer.readUtf()));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, MachineDatapackRecipe recipe) {
            buffer.writeUtf(MachineRecipeDocuments.canonical(recipe.addition));
        }
    };
//?}

//? if >=1.21.1 {
/*    private static RecipeSerializer<MachineDatapackRecipe> serializer(boolean addition) {
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
*///?} else {
    public static final RecipeSerializer<MachineDatapackRecipe> REMOVAL = new RecipeSerializer<>() {
        @Override public MachineDatapackRecipe fromJson(ResourceLocation id, JsonObject json) {
            try { return MachineDatapackRecipe.removal(id, MachineRecipeDocuments.removal(values(json))); }
            catch (RuntimeException failure) { throw new JsonSyntaxException(failure.getMessage(), failure); }
        }
        @Override public MachineDatapackRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return MachineDatapackRecipe.removal(id, MachineRecipeDocuments.removal(buffer.readUtf()));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, MachineDatapackRecipe recipe) {
            buffer.writeUtf(MachineRecipeDocuments.canonical(recipe.removal));
        }
    };
//?}

//? if >=1.21.1 {
/*            @Override public <T> DataResult<MachineDatapackRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
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
*///?} else {
    static Map<String, Object> values(JsonObject json) {
//?}
        var map = new LinkedHashMap<String, Object>();
//? if >=1.21.1 {
/*        input.entries().forEach(pair -> {
            String key = ops.getStringValue(pair.getFirst()).result().orElseThrow();
            map.put(key, value(ops.convertTo(JsonOps.INSTANCE, pair.getSecond())));
        });
*///?} else {
        for (var entry : json.entrySet()) map.put(entry.getKey(), value(entry.getValue()));
//?}
        return map;
    }

    private static Object value(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
//? if >=1.21.1 {
/*        if (element.isJsonObject()) {
            var map = new LinkedHashMap<String, Object>();
            for (var entry : element.getAsJsonObject().entrySet()) map.put(entry.getKey(), value(entry.getValue()));
            return map;
        }
*///?} else {
        if (element.isJsonObject()) return values(element.getAsJsonObject());
//?}
        if (element.isJsonArray()) {
            var list = new ArrayList<Object>();
            JsonArray array = element.getAsJsonArray();
            for (var child : array) list.add(value(child));
            return list;
        }
//? if >=1.21.1 {
/**///?} else {
        if (!element.isJsonPrimitive()) throw new JsonParseException("Unsupported JSON value");
//?}
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive.getAsString();
    }
}
