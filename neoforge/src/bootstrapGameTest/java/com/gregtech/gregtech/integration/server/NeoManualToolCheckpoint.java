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
  if(!Boolean.getBoolean("gregtech.integration.manualToolSmoke"))return;
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
  com.mojang.logging.LogUtils.getLogger().info("MANUAL_TOOL_CHECKPOINT_SUCCESS {}","{\"shapedWrench\":true,\"rejectWrongShape\":true,\"hammerWear\":400,\"headHandleAssembly\":true,\"headCrafting\":true,\"nativeStackComponentRoundTrip\":true,\"threeSerializerNetworkRoundTrips\":true}");
 }
}
