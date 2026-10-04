package com.gregtech.gregtech.recipe;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Normal shaped crafting, with usable GT tools returned with the specified GT6 durability cost. */
public class ToolShapedRecipe extends ShapedRecipe implements com.gregtech.gregtech.api.recipe.AutocraftableCraftingRecipe {
    private final boolean autocraftable;
    @Override public boolean isAutocraftableByGT() { return autocraftable; }
    private final boolean allowMirror;
    private final boolean requireEmptyFluidContainers;
    public ToolShapedRecipe(ShapedRecipe base) { this(base,true); }
    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror) {
        this(base,allowMirror,false);
    }
    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror, boolean requireEmptyFluidContainers) {
        this(base, allowMirror, requireEmptyFluidContainers, true);
    }
    public ToolShapedRecipe(ShapedRecipe base, boolean allowMirror, boolean requireEmptyFluidContainers, boolean autocraftable) {
        super(base.getId(),base.getGroup(),base.category(),base.getWidth(),base.getHeight(),base.getIngredients(),base.getResultItem(RegistryAccess.EMPTY));
        getIngredients().replaceAll(CraftingTools::expand);
        this.allowMirror=allowMirror;
        this.requireEmptyFluidContainers=requireEmptyFluidContainers;
        this.autocraftable=autocraftable;
    }
    @Override public boolean matches(CraftingContainer inventory,Level level) {
        if(!(allowMirror?super.matches(inventory,level):matchesUnmirrored(inventory)))return false;
        return toolsUsable(inventory);
    }
    /** Every GT tool in the grid has to be a usable tool of its own type. */
    protected boolean toolsUsable(CraftingContainer inventory) {
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var stack=inventory.getItem(slot);
            if(requireEmptyFluidContainers&&net.minecraftforge.fluids.FluidUtil.getFluidContained(stack).filter(f->!f.isEmpty()).isPresent())return false;
            if(stack.getItem() instanceof GTToolItem&&!GTToolHelper.matchesTool(stack,GTToolHelper.getType(stack)))return false;
        }
        return true;
    }
    private boolean matchesUnmirrored(CraftingContainer inventory) {
        for(int left=0;left<=inventory.getWidth()-getWidth();left++)
            for(int top=0;top<=inventory.getHeight()-getHeight();top++) {
                boolean matches=true;
                for(int y=0;y<inventory.getHeight()&&matches;y++)for(int x=0;x<inventory.getWidth();x++) {
                    int dx=x-left,dy=y-top;
                    var ingredient=dx>=0&&dy>=0&&dx<getWidth()&&dy<getHeight()?getIngredients().get(dx+dy*getWidth()):Ingredient.EMPTY;
                    if(!ingredient.test(inventory.getItem(x+y*inventory.getWidth()))) {matches=false;break;}
                }
                if(matches)return true;
            }
        return false;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        var result=super.getRemainingItems(inventory);
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var original=inventory.getItem(slot);
            if(!(original.getItem() instanceof GTToolItem))continue;
            var copy=original.copy();copy.setCount(1);
            long damage=(long)copy.getDamageValue()+GTToolHelper.getType(copy).damagePerCraft();
            if(damage>=copy.getMaxDamage())result.set(slot,ItemStack.EMPTY);
            else {copy.setDamageValue((int)damage);result.set(slot,copy);}
        }
        drainFiniteIngredients(inventory, result);
        return result;
    }
    /** Return vessels only at the matched fluid slots, respecting offsets and mirror policy. */
    private void drainFiniteIngredients(CraftingContainer inventory, NonNullList<ItemStack> remains) {
        if (getIngredients().stream().noneMatch(i -> i instanceof FiniteBottleFillingRecipe.ContainerIngredient)) return;
        for (int left=0;left<=inventory.getWidth()-getWidth();left++)
            for (int top=0;top<=inventory.getHeight()-getHeight();top++)
                for (int mirror=0;mirror<=(allowMirror?1:0);mirror++) {
                    Ingredient[] mapped = new Ingredient[inventory.getContainerSize()];
                    boolean matched=true;
                    for (int y=0;y<inventory.getHeight() && matched;y++) for (int x=0;x<inventory.getWidth();x++) {
                        int dx=x-left, dy=y-top;
                        var ingredient=dx>=0 && dy>=0 && dx<getWidth() && dy<getHeight()
                                ? getIngredients().get((mirror==1?getWidth()-1-dx:dx)+dy*getWidth()) : Ingredient.EMPTY;
                        int slot=x+y*inventory.getWidth();
                        mapped[slot]=ingredient;
                        if (!ingredient.test(inventory.getItem(slot))) { matched=false; break; }
                    }
                    if (!matched) continue;
                    for (int slot=0;slot<mapped.length;slot++)
                        if (mapped[slot] instanceof FiniteBottleFillingRecipe.ContainerIngredient)
                            remains.set(slot,FiniteBottleFillingRecipe.ContainerIngredient.drainOne(inventory.getItem(slot)));
                    return;
                }
    }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    /** GT6's {@code CR.MIR} for this row: false for every row the original did not opt in to. */
    public boolean allowMirror() { return allowMirror; }
    public static final RecipeSerializer<ToolShapedRecipe> SERIALIZER=new RecipeSerializer<>() {
        private final ShapedRecipe.Serializer vanilla=new ShapedRecipe.Serializer();
        @Override public ToolShapedRecipe fromJson(ResourceLocation id,JsonObject json) { return new ToolShapedRecipe(vanilla.fromJson(id,json),!json.has("allow_mirror")||json.get("allow_mirror").getAsBoolean(),json.has("require_empty_fluid_containers")&&json.get("require_empty_fluid_containers").getAsBoolean(),!json.has("gregtech_autocraftable")||json.get("gregtech_autocraftable").getAsBoolean()); }
        @Override public ToolShapedRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) { return new ToolShapedRecipe(vanilla.fromNetwork(id,buffer),buffer.readBoolean(),buffer.readBoolean(),buffer.readBoolean()); }
        @Override public void toNetwork(FriendlyByteBuf buffer,ToolShapedRecipe recipe) { vanilla.toNetwork(buffer,recipe); buffer.writeBoolean(recipe.allowMirror); buffer.writeBoolean(recipe.requireEmptyFluidContainers); buffer.writeBoolean(recipe.autocraftable); }
    };
}
