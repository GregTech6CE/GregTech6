package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.block.misc.SpikeBlock;
import com.gregtech.gregtech.item.SpikeBlockItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
*///?} else {
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
//?}

/** Original BlockBaseSpike meta6↔7 and meta14↔15, not arbitrary item-ID shapeless crafting.
 * A dedicated serializer avoids changing the established tool_shapeless wire contract. */
public final class SpikeConversionRecipe extends ShapelessRecipe
        implements com.gregtech.gregtech.api.recipe.AutocraftableCraftingRecipe {
//? if >=1.21.1 {
    /*private final ShapelessRecipe base;
*///?}
    public SpikeConversionRecipe(ShapelessRecipe base) {
//? if >=1.21.1 {
        /*super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
        this.base = base;
*///?} else {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
//?}
    }

    @Override public boolean isAutocraftableByGT() { return true; }

    private boolean accepts(ItemStack input) {
        var output = getResultItem(RegistryAccess.EMPTY);
        if (!(input.getItem() instanceof SpikeBlockItem) || !input.is(output.getItem())
                || getIngredients().size() != 1 || output.getCount() != 1) return false;
        var from = SpikeBlock.itemMode(input);
        var to = SpikeBlock.itemMode(output);
        return SpikeBlock.itemSecondary(input) == SpikeBlock.itemSecondary(output)
                && ((from == SpikeBlock.Mode.OMNI && to == SpikeBlock.Mode.FALLING)
                || (from == SpikeBlock.Mode.FALLING && to == SpikeBlock.Mode.OMNI));
    }

//? if >=1.21.1 {
    /*@Override public boolean matches(CraftingInput grid, Level level) {
        if (!base.matches(grid, level)) return false;
        int occupied = 0;
        for (var stack : grid.items()) {
*///?} else {
    @Override public boolean matches(CraftingContainer grid, Level level) {
        if (!super.matches(grid, level)) return false;
        int occupied = 0;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            var stack = grid.getItem(i);
//?}
            if (stack.isEmpty()) continue;
            occupied++;
            if (!accepts(stack)) return false;
        }
        return occupied == 1;
    }

//? if >=1.21.1 {
    /*@Override public ItemStack assemble(CraftingInput grid, HolderLookup.Provider lookup) {
        return base.assemble(grid, lookup);
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) { return base.getResultItem(lookup); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput grid) { return base.getRemainingItems(grid); }
    @Override public boolean canCraftInDimensions(int width, int height) { return base.canCraftInDimensions(width, height); }
    @Override public String getGroup() { return base.getGroup(); }
    @Override public CraftingBookCategory category() { return base.category(); }
    @Override public NonNullList<Ingredient> getIngredients() { return base.getIngredients(); }
    public static final MapCodec<SpikeConversionRecipe> CODEC = new ShapelessRecipe.Serializer().codec()
            .xmap(SpikeConversionRecipe::new, recipe -> recipe.base);
*///?}

    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }

    public static final RecipeSerializer<SpikeConversionRecipe> SERIALIZER = new RecipeSerializer<>() {
//? if >=1.21.1 {
        /*@Override public MapCodec<SpikeConversionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SpikeConversionRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
*///?} else {
        private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();
        @Override public SpikeConversionRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SpikeConversionRecipe(vanilla.fromJson(id, json));
        }
        @Override public SpikeConversionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SpikeConversionRecipe(vanilla.fromNetwork(id, buffer));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, SpikeConversionRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
//?}
    };
}
