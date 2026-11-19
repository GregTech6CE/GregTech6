package gregtech.content.recipe;

import gregtech.content.item.behavior.BehaviorDuctTape;
import gregtech.content.item.behavior.ItemBehaviors.Consumable;
import gregtech.engine.stack.StackData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
*///?} else {
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
//?}

/** Combines used rolls without increasing the total remaining tape. */
public final class TapeRepackRecipe extends ShapelessRecipe {
    public TapeRepackRecipe(ShapelessRecipe base) {
//? if >=1.21.1 {
        /*super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
*///?} else {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
//?}
    }

//? if >=1.21.1 {
    /*@Override
    public boolean matches(CraftingInput grid, Level level) {
        return super.matches(grid, level) && !assemble(grid, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput grid, net.minecraft.core.HolderLookup.Provider access) {
        return repack(grid.items());
    }
*///?} else {
    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return super.matches(grid, level) && !assemble(grid, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return repack(grid.getItems());
    }
//?}

    private ItemStack repack(Iterable<ItemStack> grid) {
        var output = getResultItem(RegistryAccess.EMPTY);
        Consumable roll = BehaviorDuctTape.consumableOf(output);
        if (roll == null || !roll.isFull(output) || output.getCount() != 1) return ItemStack.EMPTY;
        long remaining = 0;
        for (var input : grid) {
            if (input.isEmpty()) continue;
            if (!roll.isUsed(input)) return ItemStack.EMPTY;
            remaining += Math.max(0, Math.min(roll.uses(), roll.remaining(input)));
        }
        if (remaining == 0) return ItemStack.EMPTY;
        if (remaining >= roll.uses()) return output.copy();
        var result = roll.used().copy();
        var tag = StackData.readOrEmpty(result);
        tag.putLong(roll.key(), remaining);
        StackData.write(result, tag);
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

//? if >=1.21.1 {
    /*private static final MapCodec<TapeRepackRecipe> CODEC = new ShapelessRecipe.Serializer().codec()
            .xmap(TapeRepackRecipe::new, recipe -> recipe);
*///?}

    public static final RecipeSerializer<TapeRepackRecipe> SERIALIZER = new RecipeSerializer<>() {
//? if >=1.21.1 {
        /*@Override
        public MapCodec<TapeRepackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TapeRepackRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
*///?} else {
        private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();

        @Override
        public TapeRepackRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new TapeRepackRecipe(vanilla.fromJson(id, json));
        }

        @Override
        public TapeRepackRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new TapeRepackRecipe(vanilla.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TapeRepackRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
//?}
    };
}
