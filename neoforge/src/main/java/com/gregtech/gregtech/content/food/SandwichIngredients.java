package com.gregtech.gregtech.content.food;
import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.Items;
/** Platform lookup and texture boundary over the complete shared GT6 layer table. */
public final class SandwichIngredients {private SandwichIngredients(){}public record Layer(int id,String texture,int footprint,int thickness,int tint){public ResourceLocation texture(boolean top){return ResourceLocation.fromNamespaceAndPath("gregtech","textures/block/sandwiches/"+texture+(top?"_top":"_sides")+".png");}}
private static Layer adapt(SandwichLayerCatalog.Layer row){return row==null?null:new Layer(row.id(),row.texture(),row.footprint(),row.thickness(),row.tint());}
public static void register(ResourceLocation item,int id){SandwichLayerCatalog.register(item==null?null:item.toString(),id);}
public static Layer of(int id){return adapt(SandwichLayerCatalog.of(id));}
public static Layer forItem(ItemStack stack){return stack==null||stack.isEmpty()?null:adapt(SandwichLayerCatalog.forId(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));}
public static boolean isRedstone(ItemStack stack){return stack!=null&&stack.is(Items.REDSTONE);}
}
