package com.gregtech.gregtech.integration.client;
import com.google.gson.*;
import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import com.gregtech.gregtech.content.food.*;
import com.gregtech.gregtech.client.SandwichItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Uses the final installed native item renderer and actual serialized layers. */
final class SandwichDeliveryChecks {
 private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Delivered sandwich: "+why);}
 private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",id)));}
 static JsonObject capture(Minecraft minecraft,net.minecraft.client.gui.GuiGraphics graphics){
  var block=((BlockItem)item("sandwich_block").getItem()).getBlock();var custom=new SandwichBlockEntity(BlockPos.ZERO,block.defaultBlockState());
  custom.seedBase(item("toasted_toast").copyWithCount(16));require(custom.addIngredient(item("tofu_bar").copyWithCount(16))==16,"tofu ingredient");require(custom.addIngredient(item("tomato_ketchup").copyWithCount(4))==4,"ketchup ingredient");require(custom.addIngredient(item("toasted_toast").copyWithCount(16))==16,"top bread");
  var slices=new SandwichBlockEntity(BlockPos.ZERO,block.defaultBlockState());slices.seedBase(item("toast"));for(String id:new String[]{"banana_slice","cucumber_slice","onion_slice","olive_oil","toast"})require(slices.addIngredient(item(id))==1,"slice ingredient "+id);
  var previews=List.of(item("sandwich_block"),custom.dropItem(),slices.dropItem());var counts=new JsonArray();var textures=new HashSet<ResourceLocation>();
  graphics.pose().pushPose();graphics.pose().translate(0,0,600);graphics.fill(4,150,416,228,0xff191922);graphics.drawString(minecraft.font,"Delivered: actual sandwich layers + colors",8,154,0xffffff,false);
  String[] labels={"Sample (10 layers)","Custom batch (4)","Four-slice layers (6)"};
  for(int i=0;i<previews.size();i++){
   var stack=previews.get(i);require(minecraft.getItemRenderer().getModel(stack,null,null,0).isCustomRenderer(),"custom model missing");require(net.minecraftforge.client.extensions.common.IClientItemExtensions.of(stack).getCustomRenderer() instanceof SandwichItemRenderer,"custom item extension missing");
   var ingredients=SandwichBlockEntity.readItemIngredients(stack);int count=0;for(var ingredient:ingredients)if(!ingredient.isEmpty()){var layer=SandwichIngredients.forItem(ingredient);require(layer!=null,"unknown serialized layer");require(ingredient.getCount()==1,"saved ingredient count exceeds one");textures.add(layer.texture(true));textures.add(layer.texture(false));count++;}counts.add(count);
   graphics.pose().pushPose();graphics.pose().translate(24+i*134,171,0);graphics.pose().scale(2.5f,2.5f,2.5f);graphics.renderItem(stack,0,0);graphics.pose().popPose();graphics.drawString(minecraft.font,labels[i],8+i*134,214,0xffffff,false);
  }
  graphics.pose().popPose();for(var texture:textures)require(minecraft.getResourceManager().getResource(texture).isPresent(),"missing layer texture "+texture);
  require(counts.get(0).getAsInt()==10&&counts.get(1).getAsInt()==4&&counts.get(2).getAsInt()==6,"actual composition");
  require(SandwichIngredients.forItem(item("olive_oil")).tint()==0x80ff80&&SandwichIngredients.forItem(item("tomato_ketchup")).tint()==0xff0000,"source sauce tint");
  var result=new JsonObject();result.addProperty("customInventoryModelsChecked",3);result.add("ingredientLayerCounts",counts);result.addProperty("sourceLayerTexturesChecked",textures.size());result.addProperty("sourceSauceColorsChecked",2);return result;
 }
}
