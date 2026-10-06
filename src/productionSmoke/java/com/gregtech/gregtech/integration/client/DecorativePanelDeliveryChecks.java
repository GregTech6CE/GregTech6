package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.content.transport.PanelCatalog;
import com.gregtech.gregtech.item.PanelItemView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
/** Installed ordinary JAR meshes, original textures/RGB, creative entries and focused panel atlas. */
final class DecorativePanelDeliveryChecks {
 private static void require(boolean b,String why){if(!b)throw new IllegalStateException("Decorative panel delivery: "+why);}
 static JsonObject capture(Minecraft client,GuiGraphics g){
  int count=0,wood=0,colored=0;var page=com.gregtech.gregtech.loaders.b.OriginCreativeContents.contents("panels");
  var foam=com.gregtech.gregtech.data.generated.GT6Materials.Compounds.ConstructionFoam;
  require(com.gregtech.gregtech.content.recipe.MaterialRecoveryRules.shredderWork(foam)==2L*Math.max(1,foam.getToolQuality()+1),"source BRITTLE foam recovery work");
  for(var spec:PanelCatalog.ALL){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:"+spec.id())));
   require(stack.getItem() instanceof PanelItemView&&stack.getMaxStackSize()==16,"source stack identity "+spec.id());
   var data=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(stack).orElseThrow();
   require(data.components().size()==2&&data.amount()==com.gregtech.gregtech.api.material.GTValues.U/6,"per-panel original construction/plank amount "+spec.id());
   require(data.components().stream().anyMatch(c->c.material()==com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Iron")&&c.amount()==com.gregtech.gregtech.api.material.GTValues.U/9),"original ANY.Iron screw amount "+spec.id());
   require(stack.getTooltipLines(null,net.minecraft.world.item.TooltipFlag.ADVANCED).stream().anyMatch(t->t.getString().contains("0.111")),"material amount reaches native advanced tooltip "+spec.id());
   require(com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes.recipes().stream().anyMatch(r->r.mInputs[0].is(stack.getItem())),"installed panel shredder entry "+spec.id());
   require(!stack.getHoverName().getString().contains(".gregtech."),"localized name "+spec.id());
   if(spec.kind().equals("wood")) {
    var plank=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.input())));
    var tooltip=stack.getTooltipLines(null,net.minecraft.world.item.TooltipFlag.NORMAL);
    require(tooltip.stream().anyMatch(t->t.getString().equals(plank.getHoverName().getString())&&t.getStyle().getColor()!=null&&t.getStyle().getColor().getValue()==net.minecraft.ChatFormatting.AQUA.getColor()),"original actual plank-name tooltip "+spec.id());
   }
   require((client.getItemColors().getColor(stack,0)&0xffffff)==spec.tint(),"native item source RGB "+spec.id());
   var model=client.getItemRenderer().getModel(stack,null,null,0);int quads=0;float minZ=10,maxZ=-10;
   for(var pass:model.getRenderPasses(stack,false))for(var layer:pass.getRenderTypes(stack,false))for(int side=-1;side<6;side++)for(var quad:pass.getQuads(null,side<0?null:Direction.from3DDataValue(side),RandomSource.create(1),net.minecraftforge.client.model.data.ModelData.EMPTY,layer)){
    require(quad.getSprite().contents().name().equals(ResourceLocation.parse(spec.texture())),"source texture "+spec.id());require(quad.getTintIndex()==0,"RGB tint layer");
    var vertices=quad.getVertices();int stride=vertices.length/4;for(int i=0;i<4;i++){float z=Float.intBitsToFloat(vertices[i*stride+2]);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}quads++;
   }
   require(quads==6&&Math.abs(minZ-7/16F)<1e-6&&Math.abs(maxZ-9/16F)<1e-6,"six visible faces and centered 2px mesh "+spec.id());
   require(page.stream().anyMatch(s->s.is(stack.getItem()))==spec.canonical(),"source creative variants without legacy duplicates "+spec.id());count++;
  }
  int transport=0;
  for(var item:com.gregtech.gregtech.content.recipe.TransportMaterialRegistration.recoveryItems()) {
   var stack=new ItemStack(item);var data=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(stack).orElseThrow();
   require(data.amount()>0&&data.recoverable(),"installed transport composition "+stack);
   require(com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes.recipes().stream().anyMatch(r->r.mInputs[0].is(item)),"installed transport shredder entry "+stack);
   transport++;
  }
  long unit=com.gregtech.gregtech.api.material.GTValues.U;long[] weights={unit/2,unit,unit*3,unit*6,unit*12,unit*12,unit*9};
  for(var spec:com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions.pipes()) {
   var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:"+spec.id())));
   require(com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(stack).orElseThrow().amount()==weights[spec.size().ordinal()],"original installed pipe prefix weight "+spec.id());
  }
  for(var row:com.gregtech.gregtech.content.transport.TransportCraftingCatalog.rows())require(com.gregtech.gregtech.content.recipe.TransportCraftingInputs.json(row).isPresent(),"installed source crafting inputs "+row.path());
  int tankGroupVariants=0;
  for(var row:com.gregtech.gregtech.content.transport.TransportCraftingCatalog.rows()) {
   if(!row.path().equals("tank/drum_steel")&&!row.path().equals("tank/drum_tungsten"))continue;
   for(char symbol:new char[]{'P','S'}) {
    var input=row.key().get(symbol);require(input.material().getId()<0,"original ANY.Steel/W tank input");
    var ingredient=com.gregtech.gregtech.content.recipe.TransportCraftingInputs.resolve(input);
    var prefix=com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(input.name());
    for(var member:input.material().getReRegistrations()) {
     var sample=com.gregtech.gregtech.registry.GTItems.getStack(prefix,member);
     if(sample.isEmpty())continue;
     require(ingredient.test(sample),"every existing original barrel group member "+member);tankGroupVariants++;
    }
   }
  }
  g.pose().pushPose();g.pose().translate(0,0,1000);g.fill(2,2,424,238,0xff14141c);g.drawString(client.font,"GT6 panels: concrete / foam / asphalt",8,7,0xffffff,false);
  for(var spec:PanelCatalog.CANONICAL){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:"+spec.id())));
   int x,y;if(spec.kind().equals("wood")){x=9+(wood%15)*27;y=154+(wood/15)*34;wood++;}
   else{x=8+spec.sourceIndex()*25;y=27+(spec.kind().equals("concrete")?0:spec.kind().equals("cfoam")?1:2)*35;colored++;}
   g.renderItem(stack,x,y);
  }
  g.drawString(client.font,"Original dye order: black -> white",8,133,0xffffff,false);
  g.drawString(client.font,"30 available plank identities",8,143,0xffffff,false);
  g.drawString(client.font,"Item previews; source sprite/RGB checked for all 83",8,221,0xaaaaaa,false);g.pose().popPose();
  var out=new JsonObject();out.addProperty("installedItemModels",count);out.addProperty("canonicalColoredModels",colored);out.addProperty("canonicalWoodModels",wood);out.addProperty("centeredTwoPixelMesh",true);out.addProperty("sourceRgbAndTextures",true);out.addProperty("legacyAliasesHiddenFromCreative",true);out.addProperty("originalPerItemMaterialsAndAdvancedTooltip",count);out.addProperty("installedShredderEntries",count);out.addProperty("transportMaterialItemsAndShredderEntries",transport);out.addProperty("originalTankGroupFormVariants",tankGroupVariants);out.addProperty("resolvedTransportCraftingRows",com.gregtech.gregtech.content.transport.TransportCraftingCatalog.rows().size());return out;
 }
}
