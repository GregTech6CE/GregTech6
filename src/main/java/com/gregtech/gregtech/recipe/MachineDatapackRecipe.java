package com.gregtech.gregtech.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeDocuments;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeRemovalSpec;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeSpec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A datapack marker. It is never crafted; the overlay copies it onto a machine recipe map. */
public final class MachineDatapackRecipe implements Recipe<CraftingContainer> {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "gregtech");
    public static final RegistryObject<RecipeType<MachineDatapackRecipe>> ADDITION_TYPE =
            TYPES.register("machine", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath("gregtech", "machine")));
    public static final RegistryObject<RecipeType<MachineDatapackRecipe>> REMOVAL_TYPE =
            TYPES.register("machine_removal", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath("gregtech", "machine_removal")));

    private final ResourceLocation id;
    private final MachineRecipeSpec addition;
    private final MachineRecipeRemovalSpec removal;

    private MachineDatapackRecipe(ResourceLocation id, MachineRecipeSpec addition, MachineRecipeRemovalSpec removal) {
        this.id = id;
        this.addition = addition;
        this.removal = removal;
    }

    public static MachineDatapackRecipe addition(ResourceLocation id, MachineRecipeSpec spec) {
        return new MachineDatapackRecipe(id, spec, null);
    }

    public static MachineDatapackRecipe removal(ResourceLocation id, MachineRecipeRemovalSpec spec) {
        return new MachineDatapackRecipe(id, null, spec);
    }

    public MachineRecipeSpec addition() { return addition; }
    public MachineRecipeRemovalSpec removal() { return removal; }

    @Override public boolean matches(CraftingContainer container, Level level) { return false; }
    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return addition != null ? ADDITION : REMOVAL; }
    @Override public RecipeType<?> getType() { return addition != null ? ADDITION_TYPE.get() : REMOVAL_TYPE.get(); }
    @Override public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() { return NonNullList.create(); }

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

    static Map<String, Object> values(JsonObject json) {
        var map = new LinkedHashMap<String, Object>();
        for (var entry : json.entrySet()) map.put(entry.getKey(), value(entry.getValue()));
        return map;
    }

    private static Object value(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) return values(element.getAsJsonObject());
        if (element.isJsonArray()) {
            var list = new ArrayList<Object>();
            JsonArray array = element.getAsJsonArray();
            for (var child : array) list.add(value(child));
            return list;
        }
        if (!element.isJsonPrimitive()) throw new JsonParseException("Unsupported JSON value");
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive.getAsString();
    }
}
