package com.gregtech.gregtech.integration.client;
import com.google.gson.*;
import com.gregtech.gregtech.content.plant.BerryBushCatalog;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.worldgen.FluidSpringRules;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import java.util.*;

/** Check final installed JAR's actual meshes/colors at the title screen; no extra world/suite. */
final class SurfaceDeliveryChecks {
 private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Delivered surface model: "+reason);}
 static JsonObject capture(Minecraft minecraft,net.minecraft.client.gui.GuiGraphics graphics){
  var stacks=new ArrayList<ItemStack>();var identities=new JsonArray();int sprites=0;
  for(var berry:BerryBushCatalog.worldgenTypes()) {
   var block=GTBushes.byBerry(berry.id());var stack=new ItemStack(block);stacks.add(stack);
   int color=minecraft.getItemColors().getColor(stack,1);
   require((color&0xffffff)==berry.berry(),"berry tint "+berry.id());require((color>>>24)==255,"opaque berry tint");
   for(int stage=0;stage<4;stage++)require((minecraft.getBlockColors().getColor(block.defaultBlockState().setValue(com.gregtech.gregtech.block.plant.BushBlock.STAGE,stage),null,null,1)&0xffffff)==BerryBushCatalog.stageColour(berry,stage),"berry stage "+berry.id());
   checkMesh(minecraft,stack,null);
  }
  for(var spring:FluidSpringRules.SPRINGS) {
   var block=GTFluidSprings.byFluid(spring.fluidId());var stack=new ItemStack(block);stacks.add(stack);
   var fluid=BuiltInRegistries.FLUID.get(ResourceLocation.parse(spring.fluidId()));
   var visual=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
   require(minecraft.getItemColors().getColor(stack,0)==visual.getTintColor(),"spring item tint "+spring.fluidId());
   require(minecraft.getBlockColors().getColor(block.defaultBlockState(),null,null,0)==visual.getTintColor(),"spring block tint without BE");
   checkMesh(minecraft,stack,visual.getStillTexture());sprites++;
   // World mesh must also work with no block-entity model data.
   var world=minecraft.getBlockRenderer().getBlockModel(block.defaultBlockState());boolean matched=false;
   for(int side=-1;side<6;side++)for(var quad:world.getQuads(block.defaultBlockState(),side<0?null:Direction.from3DDataValue(side),RandomSource.create(1),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,net.minecraft.client.renderer.RenderType.cutout()))if(quad.isTinted()&&quad.getSprite().contents().name().equals(visual.getStillTexture()))matched=true;
   require(matched,"spring world fluid sprite without BE");
  }
  graphics.pose().pushPose();graphics.pose().translate(0,0,500);
  graphics.fill(4,4,416,103,0xff14141c);graphics.drawString(minecraft.font,"Delivered bushes + springs (16 registered variants)",8,8,0xffffff,false);
  for(int i=0;i<stacks.size();i++) {
   var stack=stacks.get(i);String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
   require(!stack.getHoverName().getString().startsWith("block.gregtech."),"untranslated name "+id);
   int x=8+i%9*45,y=24+i/9*36;graphics.fill(x,y,x+43,y+33,0xff30303d);graphics.renderItem(stack,x+12,y+2);graphics.drawString(minecraft.font,minecraft.font.plainSubstrByWidth(stack.getHoverName().getString(),40),x+1,y+21,0xffffff,false);identities.add(id);
  }
  var machineIds=new JsonArray();
  String[] machineNames={"lightning_galvanized_steel","lightning_aluminium","lightning_stainless_steel","lightning_chromium","lightning_titanium","melter_stainless_steel"};
  for(int i=0;i<machineNames.length;i++) {
   var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",machineNames[i])));checkMesh(minecraft,stack,null);
   var block=(com.gregtech.gregtech.block.machine.BasicMachineBlock)((BlockItem)stack.getItem()).getBlock();require((minecraft.getItemColors().getColor(stack,0)&0xffffff)==(block.basicSpec().material().getColor()&0xffffff),"machine inventory tint");
   int x=8+i*65,y=110;graphics.fill(x,y,x+62,y+33,0xff30303d);graphics.renderItem(stack,x+22,y+2);graphics.drawString(minecraft.font,minecraft.font.plainSubstrByWidth(stack.getHoverName().getString(),60),x+1,y+21,0xffffff,false);machineIds.add(machineNames[i]);
  }
  graphics.pose().popPose();
  var result=new JsonObject();result.add("surfaceItems",identities);result.add("machineItems",machineIds);result.addProperty("machineModelsChecked",6);result.addProperty("surfaceItemModelsChecked",stacks.size());result.addProperty("surfaceSpringSpritesChecked",sprites);result.addProperty("surfaceBerryStageColorsChecked",36);return result;
 }
 private static void checkMesh(Minecraft minecraft,ItemStack stack,ResourceLocation sprite){
  var model=minecraft.getItemRenderer().getModel(stack,null,null,0);int count=0,tints=0;boolean matched=sprite==null;
  for(var pass:model.getRenderPasses(stack,false))for(var layer:pass.getRenderTypes(stack,false))for(int side=-1;side<6;side++)for(var quad:pass.getQuads(null,side<0?null:Direction.from3DDataValue(side),RandomSource.create(1),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,layer)) {
   require(!quad.getSprite().contents().name().getPath().contains("missing"),"missing item texture "+stack);count++;if(quad.isTinted()){tints++;if(quad.getSprite().contents().name().equals(sprite))matched=true;}
  }
  require(count>0&&tints>0&&matched,"empty/untinted/wrong fluid item model "+stack);
 }
}
