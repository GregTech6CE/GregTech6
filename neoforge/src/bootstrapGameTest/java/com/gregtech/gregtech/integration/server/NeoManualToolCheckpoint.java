package com.gregtech.gregtech.integration.server;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.recipe.GTToolRecipeSerializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;

/** Opt-in grouped native tool crafting/component/network checkpoint; excluded from production jars. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=Dist.DEDICATED_SERVER)
public final class NeoManualToolCheckpoint {
 private NeoManualToolCheckpoint(){}
 private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Manual tool checkpoint: "+message);}
 private static CraftingRecipe recipe(ServerLevel level,String id){var holder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech",id)).orElseThrow(()->new IllegalStateException("Missing actual datapack recipe "+id));require(holder.value() instanceof CraftingRecipe,"recipe kind "+id);return (CraftingRecipe)holder.value();}
 private static ItemStack form(MaterialPrefix prefix){var stack=GTItems.getStack(prefix,Materials.Bronze,1);require(!stack.isEmpty(),"missing Bronze form "+prefix);return stack;}
 private static ItemStack tool(GTToolType type){var stack=GTToolItem.create(type,Materials.Bronze,WoodMaterials.Wood);require(GTToolHelper.isUsable(stack),"usable assembled "+type);return stack;}
 @SuppressWarnings({"unchecked","rawtypes"})
 private static CraftingRecipe network(CraftingRecipe recipe,ServerLevel level){
  RecipeSerializer serializer=recipe.getSerializer();var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
  try{serializer.streamCodec().encode(buffer,recipe);var result=(CraftingRecipe)serializer.streamCodec().decode(buffer);require(buffer.readableBytes()==0,"packet fully consumed");return result;}finally{buffer.release();}
 }
 @SubscribeEvent public static void started(ServerStartedEvent event){
  if(!Boolean.getBoolean("gregtech.integration.manualToolSmoke") && !Boolean.getBoolean("gregtech.integration.equipmentCraftingSmoke"))return;
  ServerLevel level=event.getServer().overworld();
  // The original metal wrench row: plate/hammer/plate, plate beneath, plate beneath.
  ItemStack hammer=tool(GTToolType.HARD_HAMMER);int before=hammer.getDamageValue();
  var wrenchInput=CraftingInput.of(3,3,List.of(form(MaterialPrefix.plate),hammer,form(MaterialPrefix.plate),ItemStack.EMPTY,form(MaterialPrefix.plate),ItemStack.EMPTY,ItemStack.EMPTY,form(MaterialPrefix.plate),ItemStack.EMPTY));
  var wrench=recipe(level,"tools/wrench");require(wrench.matches(wrenchInput,level),"wrench original shape");
  ItemStack result=wrench.assemble(wrenchInput,level.registryAccess());require(GTToolHelper.getType(result)==GTToolType.WRENCH&&GTToolHelper.getHead(result)==Materials.Bronze,"wrench output material");
  var remains=wrench.getRemainingItems(wrenchInput);require(remains.get(1).getItem()==hammer.getItem()&&remains.get(1).getDamageValue()==before+400,"hammer returned with original wear");require(hammer.getDamageValue()==before,"input tool not mutated by remainder query");
  var wrong=new ArrayList<>(wrenchInput.items());Collections.swap(wrong,1,4);require(!wrench.matches(CraftingInput.of(3,3,wrong),level),"wrong tool position rejected");
  require(network(wrench,level).matches(wrenchInput,level),"crafting serializer network round trip");
  var assemblyInput=CraftingInput.of(2,1,List.of(form(MaterialPrefix.toolHeadPickaxe),GTItems.getStack(MaterialPrefix.stick,WoodMaterials.Wood,1)));
  var assembly=recipe(level,"tools/pickaxe_assembly");require(assembly.matches(assemblyInput,level),"real head plus wood handle");
  ItemStack pick=assembly.assemble(assemblyInput,level.registryAccess());require(GTToolHelper.getType(pick)==GTToolType.PICKAXE&&GTToolHelper.getHandle(pick)==WoodMaterials.Wood&&pick.has(DataComponents.MAX_DAMAGE),"pick type/handle/native durability");
  var roundTrip=ItemStack.parseOptional(level.registryAccess(),(net.minecraft.nbt.CompoundTag)pick.saveOptional(level.registryAccess()));require(GTToolHelper.isUsable(roundTrip)&&GTToolHelper.getHead(roundTrip)==Materials.Bronze&&roundTrip.getMaxDamage()==pick.getMaxDamage(),"actual stack component save parse");
  require(network(assembly,level).matches(assemblyInput,level),"assembly serializer network round trip");
  var headInput=CraftingInput.of(3,2,List.of(form(MaterialPrefix.plate),form(MaterialPrefix.ingot),form(MaterialPrefix.ingot),tool(GTToolType.FILE),ItemStack.EMPTY,tool(GTToolType.HARD_HAMMER)));
  var head=recipe(level,"tool_heads/pickaxe");require(head.matches(headInput,level),"original metal pick head shape");
  require(ItemStack.isSameItemSameComponents(head.assemble(headInput,level.registryAccess()),form(MaterialPrefix.toolHeadPickaxe)),"head output material and form");require(network(head,level).matches(headInput,level),"head serializer network round trip");
  if(Boolean.getBoolean("gregtech.integration.equipmentCraftingSmoke")) equipment(level);
  com.mojang.logging.LogUtils.getLogger().info("MANUAL_TOOL_CHECKPOINT_SUCCESS {}","{\"shapedWrench\":true,\"rejectWrongShape\":true,\"hammerWear\":400,\"headHandleAssembly\":true,\"headCrafting\":true,\"nativeStackComponentRoundTrip\":true,\"threeSerializerNetworkRoundTrips\":true}");
 }
 private static ItemStack equipmentItem(String path) {
  var id=ResourceLocation.fromNamespaceAndPath("gregtech",path);
  require(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id),"registered equipment/input "+id);
  return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
 }
 private static ItemStack equipmentForm(MaterialPrefix prefix,com.gregtech.gregtech.api.material.GTMaterial material) {
  var result=GTItems.getStack(prefix,material,1);require(!result.isEmpty(),"equipment form "+prefix+" / "+material);return result;
 }
 private static void equipment(ServerLevel level) {
  int loaded=0;
  for(String file:com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES) {
   String id=file.substring(0,file.length()-5);
   var holder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech",id))
       .orElseThrow(()->new IllegalStateException("Missing retained equipment recipe "+id));
   require(!holder.value().getResultItem(level.registryAccess()).isEmpty(),"nonempty equipment result "+id);
   for(var ingredient:holder.value().getIngredients())
    if(!ingredient.isEmpty())require(ingredient.getItems().length>0,"resolved equipment ingredient/tag "+id);
   loaded++;
  }
  ItemStack hammer=tool(GTToolType.HARD_HAMMER), wrench=tool(GTToolType.WRENCH);
  ItemStack dense=equipmentForm(MaterialPrefix.plateDense,Materials.Bronze);
  var boilerInput=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,dense.copy(),ItemStack.EMPTY,
      dense.copy(),wrench,dense.copy(),dense.copy(),hammer,dense.copy()));
  var boiler=recipe(level,"smeltery_survival/strong_steam_boiler_bronze");
  require(boiler.matches(boilerInput,level),"actual dense bronze and tools craft strong boiler");
  var boilerResult=boiler.assemble(boilerInput,level.registryAccess());
  require(ItemStack.isSameItemSameComponents(boilerResult,equipmentItem("strong_steam_boiler_bronze")),"strong boiler output");
  var boilerRemains=boiler.getRemainingItems(boilerInput);
  require(boilerRemains.get(4).getDamageValue()==800 && boilerRemains.get(7).getDamageValue()==400,
      "retained wrench 800 / hammer 400 crafting wear");
  require(wrench.getDamageValue()==0 && hammer.getDamageValue()==0,"remaining-items query does not mutate source tools");
  require(network(boiler,level).matches(boilerInput,level),"equipment recipe native packet round trip");
  var burnerInput=CraftingInput.of(3,3,List.of(equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),
      equipmentForm(MaterialPrefix.plateDense,Materials.Copper),equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),
      equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),tool(GTToolType.WRENCH),equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),
      new ItemStack(Items.BRICKS),new ItemStack(Items.BRICKS),new ItemStack(Items.BRICKS)));
  var burner=recipe(level,"smeltery_survival/burning_box_solid_dense_bronze");
  require(burner.matches(burnerInput,level) && ItemStack.isSameItemSameComponents(burner.assemble(burnerInput,level.registryAccess()),
      equipmentItem("burning_box_solid_dense_bronze")),"actual dense burner original pattern");
  var plate=equipmentForm(MaterialPrefix.plateDouble,Materials.Steel);
  var wire=equipmentItem("wire_01_annealed_copper");
  var transformerInput=CraftingInput.of(3,3,List.of(wire.copy(),plate.copy(),wire.copy(),
      equipmentItem("wire_04_annealed_copper"),equipmentItem("casing_machine_tinalloy"),tool(GTToolType.WIRE_CUTTER),
      wire.copy(),plate.copy(),wire.copy()));
  var transformer=recipe(level,"energy_nodes/transformer_ulv_lv");
  require(transformer.matches(transformerInput,level),"ANY copper and steel double-plate families craft transformer");
  require(ItemStack.isSameItemSameComponents(transformer.assemble(transformerInput,level.registryAccess()),
      equipmentItem("transformer_ulv_lv")),"actual transformer output");
  var mirrored=new ArrayList<>(transformerInput.items());Collections.swap(mirrored,3,5);
  require(!transformer.matches(CraftingInput.of(3,3,mirrored),level),"original asymmetric no-mirror flag retained");
  var clayInput=CraftingInput.of(3,3,List.of(new ItemStack(Items.CLAY_BALL),tool(GTToolType.KNIFE),new ItemStack(Items.CLAY_BALL),
      new ItemStack(Items.CLAY_BALL),tool(GTToolType.ROLLING_PIN),new ItemStack(Items.CLAY_BALL),
      new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL)));
  var clay=recipe(level,"smeltery_survival/clay_crucible");
  require(clay.matches(clayInput,level),"clay balls, knife and rolling pin craft unfired crucible");
  ItemStack unfired=clay.assemble(clayInput,level.registryAccess());
  require(ItemStack.isSameItemSameComponents(unfired,equipmentItem("clay_crucible")),"clay crucible result");
  var fired=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech","smeltery_survival/ceramic_crucible_firing")).orElseThrow();
  require(fired.value() instanceof SmeltingRecipe,"actual native firing recipe kind");
  var firing=(SmeltingRecipe)fired.value();
  require(firing.matches(new SingleRecipeInput(unfired),level) && ItemStack.isSameItemSameComponents(
      firing.assemble(new SingleRecipeInput(unfired),level.registryAccess()),equipmentItem("smelting_crucible_ceramic")),
      "unfired crucible enters original furnace recipe and produces ceramic equipment");
  var reclaim=recipe(level,"smeltery_survival/clay_crucible_reclaim");
  var reclaimInput=CraftingInput.of(1,1,List.of(unfired));
  require(reclaim.matches(reclaimInput,level) && reclaim.assemble(reclaimInput,level.registryAccess()).is(Items.CLAY_BALL)
      && reclaim.assemble(reclaimInput,level.registryAccess()).getCount()==7,"original seven-clay reclaim count");
  com.mojang.logging.LogUtils.getLogger().info("EQUIPMENT_CRAFTING_CHECKPOINT_SUCCESS loaded={} resolvedIngredients=true boiler=true burner=true transformer=true mirrorRejected=true clayFiringRecipe=true reclaimCount=7 wrenchWear=800 hammerWear=400",loaded);
 }

}
