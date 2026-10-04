package com.gregtech.gregtech.recipe;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Original CR shapeless rows: ordinary GT tool container crafting wears and returns the tool. */
public final class ToolShapelessRecipe extends ShapelessRecipe implements com.gregtech.gregtech.api.recipe.AutocraftableCraftingRecipe {
    private final boolean autocraftable;
    @Override public boolean isAutocraftableByGT() { return autocraftable; }
    public ToolShapelessRecipe(ShapelessRecipe base) { this(base, true); }
    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable) {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
        this.autocraftable = autocraftable;
    }
    @Override public boolean matches(CraftingContainer grid, Level level) {
        if (!super.matches(grid, level)) return false;
        for (int i=0; i<grid.getContainerSize(); i++) {
            var stack=grid.getItem(i);
            if (stack.getItem() instanceof GTToolItem && !GTToolHelper.isUsable(stack)) return false;
        }
        return true;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        var remaining=super.getRemainingItems(grid);
        for (int i=0; i<grid.getContainerSize(); i++) {
            var stack=grid.getItem(i);
            if (!(stack.getItem() instanceof GTToolItem)) continue;
            var tool=stack.copyWithCount(1);
            long damage=(long)tool.getDamageValue()+GTToolHelper.getType(tool).damagePerCraft();
            if (damage>=tool.getMaxDamage()) remaining.set(i,ItemStack.EMPTY);
            else { tool.setDamageValue((int)damage); remaining.set(i,tool); }
        }
        return remaining;
    }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    public static final RecipeSerializer<ToolShapelessRecipe> SERIALIZER=new RecipeSerializer<>() {
        private final ShapelessRecipe.Serializer vanilla=new ShapelessRecipe.Serializer();
        @Override public ToolShapelessRecipe fromJson(ResourceLocation id,JsonObject json) { return new ToolShapelessRecipe(vanilla.fromJson(id,json), !json.has("gregtech_autocraftable") || json.get("gregtech_autocraftable").getAsBoolean()); }
        @Override public ToolShapelessRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) { return new ToolShapelessRecipe(vanilla.fromNetwork(id,buffer), buffer.readBoolean()); }
        @Override public void toNetwork(FriendlyByteBuf buffer,ToolShapelessRecipe recipe) { vanilla.toNetwork(buffer,recipe); buffer.writeBoolean(recipe.autocraftable); }
    };
}
