package com.gregtech.gregtech.recipe;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.NonNullList;
/** Original KEEPNBT dye/printed-pages bindings; cloning follows vanilla generation and retains the source. */
public final class BookBindingRecipe extends ShapelessRecipe {
    private final ShapelessRecipe base;
    public BookBindingRecipe(ShapelessRecipe base){super(base.getId(),base.getGroup(),base.category(),base.getResultItem(null),base.getIngredients());this.base=base;}
    private boolean copies(){return getGroup().equals("gregtech.books.copy");}
    private static boolean payload(ItemStack stack){
        if(stack.getItem() instanceof com.gregtech.gregtech.item.ColoredBookItem||stack.is(Items.WRITTEN_BOOK))return true;
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());return id.getNamespace().equals("gregtech")&&(id.getPath().equals("printed_pages")||id.getPath().equals("many_printed_pages"));
    }
    private static int generation(ItemStack stack){return stack.hasTag()?WrittenBookItem.getGeneration(stack):3;}
    private ItemStack source(net.minecraft.world.inventory.CraftingContainer input){for(int i=0;i<input.getContainerSize();i++){var stack=input.getItem(i);if(payload(stack))return stack;}return ItemStack.EMPTY;}
    @Override public boolean matches(net.minecraft.world.inventory.CraftingContainer input,Level level){return super.matches(input,level)&&(!copies()||generation(source(input))<2);}
    @Override public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer input,net.minecraft.core.RegistryAccess lookup){
        var output=super.assemble(input,lookup);var payload=source(input);if(payload.isEmpty())return output;
        if(payload.hasTag())output.setTag(payload.getTag().copy());
        if(copies()){if(generation(payload)>=2)return ItemStack.EMPTY;output.getOrCreateTag().putInt("generation",generation(payload)+1);}
        return output;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(net.minecraft.world.inventory.CraftingContainer input){
        var remaining=super.getRemainingItems(input);if(copies())for(int i=0;i<input.getContainerSize();i++)if(payload(input.getItem(i))){remaining.set(i,input.getItem(i).copyWithCount(1));break;}return remaining;
    }
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
    public static final RecipeSerializer<BookBindingRecipe> SERIALIZER=new RecipeSerializer<>() {
        private final ShapelessRecipe.Serializer vanilla=new ShapelessRecipe.Serializer();
        public BookBindingRecipe fromJson(net.minecraft.resources.ResourceLocation id,com.google.gson.JsonObject json){return new BookBindingRecipe(vanilla.fromJson(id,json));}
        public BookBindingRecipe fromNetwork(net.minecraft.resources.ResourceLocation id,net.minecraft.network.FriendlyByteBuf buffer){return new BookBindingRecipe(vanilla.fromNetwork(id,buffer));}
        public void toNetwork(net.minecraft.network.FriendlyByteBuf buffer,BookBindingRecipe recipe){vanilla.toNetwork(buffer,recipe.base);}
    };
}
