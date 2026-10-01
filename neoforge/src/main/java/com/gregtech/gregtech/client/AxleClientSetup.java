package com.gregtech.gregtech.client;
import com.gregtech.gregtech.registry.GTAxles;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AxleClientSetup {
 private AxleClientSetup(){}
 private static int color(com.gregtech.gregtech.block.energy.AxleBlock block,int layer){return layer==0?block.spec().material().getColor():0xFFFFFF;}
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){for(var entry:GTAxles.all()){var block=entry.get();event.register((state,level,pos,layer)->color(block,layer),block);}}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){for(var entry:GTAxles.all()){var block=entry.get();event.register((stack,layer)->color(block,layer),block.asItem());}}
 @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event){
  var models=event.getModels();ItemTransforms transforms=ItemTransforms.NO_TRANSFORMS;
  for(String path:new String[]{"iron_block","stone","dirt"}){var model=models.get(ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace(path)));if(model!=null&&model.isGui3d()&&model.getTransforms()!=ItemTransforms.NO_TRANSFORMS){transforms=model.getTransforms();break;}}
  var wire=ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/metallic/wire");var rubber=ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/rubber/pipeside");
  for(var entry:GTAxles.all()){
   var block=entry.get();var model=new AxleBakedModel(block.spec().halfThickness(),transforms);
   var key=BuiltInRegistries.BLOCK.getKey(block);for(var state:block.getStateDefinition().getPossibleStates())models.put(BlockModelShaper.stateToModelLocation(key,state),model);models.put(ModelResourceLocation.inventory(key),model);
  }
 }
}
