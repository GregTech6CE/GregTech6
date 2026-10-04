package com.gregtech.gregtech.integration.client;
import com.google.gson.*;
import com.gregtech.gregtech.content.food.CannedFoodCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
/** Actual delivered can item meshes and localized tooltip callback, loaded from production jars. */
final class CannedFoodDeliveryChecks {
 static JsonObject render(net.minecraft.client.Minecraft minecraft,net.minecraft.client.gui.GuiGraphics graphics){
  var result=new JsonObject();int models=0;var displayed=new JsonArray();graphics.pose().pushPose();graphics.pose().translate(0,-90,900);graphics.fill(4,232,416,316,0xff202026);graphics.drawString(minecraft.font,"Delivered source cans: food / air / rotten",8,236,0xffffff,false);
  String[] samples={"tiny_food_can_meat","small_food_can_fish","tall_food_can_vegetables","wide_food_can_fruits","large_food_can_bread","huge_food_can_cookies","canned_air","canned_hot_air","canned_space_air","huge_food_can_rotten","tiny_food_can_chum","empty_food_can"};
  for(var row:CannedFoodCatalog.ROWS){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",row.id())));var model=minecraft.getItemRenderer().getModel(stack,null,null,0);int quads=0;for(var pass:model.getRenderPasses(stack,false))for(var type:pass.getRenderTypes(stack,false))for(int face=-1;face<6;face++)for(var quad:pass.getQuads(null,face<0?null:net.minecraft.core.Direction.from3DDataValue(face),net.minecraft.util.RandomSource.create(1),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,type)){if(quad.getSprite().contents().name().getPath().contains("missing"))throw new IllegalStateException("Delivered can texture missing: "+row.id());quads++;}if(quads==0)throw new IllegalStateException("Delivered can transparent: "+row.id());models++;}
  for(int i=0;i<samples.length;i++){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",samples[i])));graphics.renderItem(stack,12+i%6*67,250+i/6*30);graphics.drawString(minecraft.font,minecraft.font.plainSubstrByWidth(stack.getHoverName().getString(),63),6+i%6*67,269+i/6*30,0xffffff,false);displayed.add(samples[i]);}
  graphics.pose().popPose();result.addProperty("inventoryModelsChecked",models);result.add("renderedCanSamples",displayed);return result;
 }
}
