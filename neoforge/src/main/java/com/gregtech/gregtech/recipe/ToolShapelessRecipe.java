package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.item.GTToolItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Original CR shapeless rows: ordinary GT tool container crafting wears and returns the tool. */
public final class ToolShapelessRecipe extends ShapelessRecipe {
    private final ShapelessRecipe base;
    public ToolShapelessRecipe(ShapelessRecipe base) {
        super(base.getGroup(),base.category(),base.getResultItem(net.minecraft.core.RegistryAccess.EMPTY),base.getIngredients());
        this.base=base;
    }
    @Override public boolean matches(CraftingInput grid,Level level) {
        if (!base.matches(grid,level)) return false;
        for (var stack:grid.items())
            if (stack.getItem() instanceof GTToolItem && !GTToolHelper.isUsable(stack)) return false;
        return true;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput grid) {
        var remaining=base.getRemainingItems(grid);
        for (int i=0;i<grid.size();i++) {
            var stack=grid.getItem(i);
            if (!(stack.getItem() instanceof GTToolItem)) continue;
            var tool=stack.copyWithCount(1);
            long damage=(long)tool.getDamageValue()+GTToolHelper.getType(tool).damagePerCraft();
            if (damage>=tool.getMaxDamage()) remaining.set(i,ItemStack.EMPTY);
            else {tool.setDamageValue((int)damage);remaining.set(i,tool);}
        }
        return remaining;
    }
    @Override public ItemStack assemble(CraftingInput grid,HolderLookup.Provider lookup){return base.assemble(grid,lookup);}
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup){return base.getResultItem(lookup);}
    @Override public boolean canCraftInDimensions(int width,int height){return base.canCraftInDimensions(width,height);}
    @Override public String getGroup(){return base.getGroup();}
    @Override public CraftingBookCategory category(){return base.category();}
    @Override public NonNullList<Ingredient> getIngredients(){return base.getIngredients();}
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
    public static final MapCodec<ToolShapelessRecipe> CODEC=new ShapelessRecipe.Serializer().codec()
            .xmap(ToolShapelessRecipe::new,recipe->recipe.base);
    public static final RecipeSerializer<ToolShapelessRecipe> SERIALIZER=new RecipeSerializer<>() {
        @Override public MapCodec<ToolShapelessRecipe> codec(){return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,ToolShapelessRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
    };
}
