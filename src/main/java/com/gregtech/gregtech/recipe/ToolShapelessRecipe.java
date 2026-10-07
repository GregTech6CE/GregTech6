package com.gregtech.gregtech.recipe;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.content.recipe.FormConversionSelector;
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
    private final boolean autocraftable, requireEmptyFluidContainers;
    private final FormConversionSelector formSelector;
    private final String conversionInput, conversionMaterial;
    @Override public boolean isAutocraftableByGT() { return autocraftable; }
    public ToolShapelessRecipe(ShapelessRecipe base) { this(base, true); }
    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable) {
        this(base, autocraftable, FormConversionSelector.NONE);
    }
    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector) {
        this(base, autocraftable, formSelector, "", "");
    }
    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector,
                               String conversionInput, String conversionMaterial) {
        this(base, autocraftable, formSelector, conversionInput, conversionMaterial, false);
    }
    public ToolShapelessRecipe(ShapelessRecipe base, boolean autocraftable, FormConversionSelector formSelector,
                              String conversionInput, String conversionMaterial, boolean requireEmptyFluidContainers) {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
        this.autocraftable = autocraftable;
        this.requireEmptyFluidContainers=requireEmptyFluidContainers;
        this.formSelector = formSelector;
        if (conversionInput.isEmpty() != conversionMaterial.isEmpty()) throw new IllegalArgumentException("Incomplete source conversion form");
        this.conversionInput = conversionInput;
        this.conversionMaterial = conversionMaterial;
    }
    @Override public boolean matches(CraftingContainer grid, Level level) {
        if (!super.matches(grid, level)) return false;
        int first = -1, occupied = 0;
        for (int i=0; i<grid.getContainerSize(); i++) {
            var stack=grid.getItem(i);
            if (!stack.isEmpty()) { if (first < 0) first = i; occupied++; }
            if (!stack.isEmpty() && !conversionInput.isEmpty()
                    && !CraftingMaterialForms.matches(conversionInput, conversionMaterial, stack)) return false;
            if(requireEmptyFluidContainers&&(com.gregtech.gregtech.api.material.ItemMaterialRegistry.hasStoredContents(stack)||net.minecraftforge.fluids.FluidUtil.getFluidContained(stack).filter(f->!f.isEmpty()).isPresent()))return false;
            if (stack.getItem() instanceof GTToolItem && !GTToolHelper.isUsable(stack)) return false;
        }
        return formSelector.matches(grid.getContainerSize(), first, occupied);
    }
    @Override public boolean canCraftInDimensions(int width, int height) {
        return super.canCraftInDimensions(width, height) && width * height >= formSelector.minimumGridSize();
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
        @Override public ToolShapelessRecipe fromJson(ResourceLocation id,JsonObject json) {
            return new ToolShapelessRecipe(vanilla.fromJson(id,json),
                    !json.has("gregtech_autocraftable") || json.get("gregtech_autocraftable").getAsBoolean(),
                    new FormConversionSelector(json.has("gregtech_form_variants") ? json.get("gregtech_form_variants").getAsInt() : 0,
                            json.has("gregtech_form_offset") ? json.get("gregtech_form_offset").getAsInt() : 0),
                    json.has("gregtech_form_input") ? json.get("gregtech_form_input").getAsString() : "",
                    json.has("gregtech_form_material") ? json.get("gregtech_form_material").getAsString() : "",
                    json.has("require_empty_fluid_containers") && json.get("require_empty_fluid_containers").getAsBoolean());
        }
        @Override public ToolShapelessRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            return new ToolShapelessRecipe(vanilla.fromNetwork(id,buffer), buffer.readBoolean(),
                    new FormConversionSelector(buffer.readVarInt(), buffer.readVarInt()), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,ToolShapelessRecipe recipe) {
            vanilla.toNetwork(buffer,recipe); buffer.writeBoolean(recipe.autocraftable);
            buffer.writeVarInt(recipe.formSelector.variants()); buffer.writeVarInt(recipe.formSelector.offset());
            buffer.writeUtf(recipe.conversionInput); buffer.writeUtf(recipe.conversionMaterial); buffer.writeBoolean(recipe.requireEmptyFluidContainers);
        }
    };
}
