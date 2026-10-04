package com.gregtech.gregtech.integration.server;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.ElectricToolItem;
import com.gregtech.gregtech.item.behavior.BehaviorRemote;
import com.gregtech.gregtech.platform.neoforge.StackCustomData;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.resources.ResourceLocation;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.api.distmarker.Dist;import net.neoforged.neoforge.event.server.ServerStartedEvent;
/** Opt-in grouped actual recipe/packet/component/fuse checkpoint; excluded from production jars. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=Dist.DEDICATED_SERVER)
public final class NeoPoweredToolCheckpoint {
 private NeoPoweredToolCheckpoint(){}
 private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Powered tool checkpoint: "+message);}
 @SuppressWarnings({"unchecked","rawtypes"})
 @SubscribeEvent public static void started(ServerStartedEvent event){
  if(!Boolean.getBoolean("gregtech.integration.poweredToolSmoke"))return;
  var level=event.getServer().overworld();var id=ResourceLocation.fromNamespaceAndPath("gregtech","electric_tools/electric_wrench/bronze/battery_nickel_cadmium_lv");
  var recipe=(CraftingRecipe)level.getRecipeManager().byKey(id).orElseThrow(()->new IllegalStateException("Missing actual powered recipe "+id)).value();
  var parts=new java.util.ArrayList<ItemStack>();for(var ingredient:recipe.getIngredients()){require(ingredient.getItems().length>0,"resolved assembly ingredient");parts.add(ingredient.getItems()[0].copy());}
  var input=CraftingInput.of(3,3,parts);require(recipe.matches(input,level),"original powered wrench shape");
  var stack=recipe.assemble(input,level.registryAccess());require(stack.getItem()==GTElectricItems.ELECTRIC_WRENCH.get()&&GTToolHelper.getHead(stack)==Materials.Bronze,"actual powered wrench output");
  var tool=(ElectricToolItem)stack.getItem();var eu=GregTechTags.Energy.EU;long capacity=((com.gregtech.gregtech.api.energy.item.IItemEnergy)GTElectricItems.BATTERY_LV.get()).getEnergyCapacity(new ItemStack(GTElectricItems.BATTERY_LV.get()),eu);
  require(tool.getEnergyCapacity(stack,eu)==capacity&&tool.getEnergyStored(stack,eu)==0&&!stack.isDamageableItem(),"inherited battery capacity, uncharged, material wear separate");
  require(tool.doEnergyInjection(eu,stack,32,4,level,BlockPos.ZERO,false)==4&&tool.getEnergyStored(stack,eu)==0,"charge simulation leaves components unchanged");
  require(tool.doEnergyInjection(eu,stack,32,4,level,BlockPos.ZERO,true)==4&&tool.getEnergyStored(stack,eu)==128,"actual packet charge");
  require(GTToolHelper.matchesTool(stack,GTToolType.WRENCH),"powered dispatch");tool.consumeInteractionEnergy(stack,50,null);require(tool.getEnergyStored(stack,eu)==78,"actual click energy");
  long before=ElectricToolWear.damage(stack);ElectricToolWear.apply(tool,stack,123,bound->0);require(ElectricToolWear.damage(stack)==before+123,"independent material wear");
  var saved=ItemStack.parseOptional(level.registryAccess(),(CompoundTag)stack.saveOptional(level.registryAccess()));require(tool.getEnergyStored(saved,eu)==78&&tool.getEnergyCapacity(saved,eu)==capacity&&ElectricToolWear.damage(saved)==before+123,"powered stack components saved and parsed");
  StackCustomData.update(stack,tag->tag.putLong("gt.charge",1));tool.consumeInteractionEnergy(stack,50,null);require(tool.getEnergyStored(stack,eu)==0&&!GTToolHelper.matchesTool(stack,GTToolType.WRENCH),"last partial charge disables next click");
  var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());try{RecipeSerializer serializer=recipe.getSerializer();serializer.streamCodec().encode(buffer,recipe);var received=(CraftingRecipe)serializer.streamCodec().decode(buffer);require(buffer.readableBytes()==0&&received.matches(input,level)&&ItemStack.isSameItemSameComponents(received.assemble(input,level.registryAccess()),recipe.assemble(input,level.registryAccess())),"powered recipe network round trip");}finally{buffer.release();}
  var pos=new BlockPos(128,100,128);level.getChunkAt(pos);level.setBlock(pos,GTToolBlocks.DYNAMITE.get().defaultBlockState(),3);var fuse=(com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity)level.getBlockEntity(pos);require(fuse!=null,"real registered charge entity");
  require(fuse.onIgnite(level,pos,Direction.UP,null,ItemStack.EMPTY,false,0,0,0)==10000&&fuse.remainingTicks()==100,"actual ignition fuse");fuse.serverTick();require(fuse.remainingTicks()==99,"actual fuse tick");
  var fuseTag=fuse.saveWithoutMetadata(level.registryAccess());var parsed=new com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity(pos,level.getBlockState(pos));parsed.loadWithComponents(fuseTag,level.registryAccess());require(parsed.remainingTicks()==99,"native fuse save/load");
  require(fuse.onExtinguish(level,pos,Direction.UP,null,ItemStack.EMPTY,false,0,0,0)==10000&&fuse.remainingTicks()==0,"actual fuse defusing");
  var remote=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","remote_activator")));require(remote.getItem() instanceof com.gregtech.gregtech.item.RemoteActivatorItem,"real remote factory");require(BehaviorRemote.addCoords(remote,level,pos)&&BehaviorRemote.getCoords(StackCustomData.read(remote),level).contains(pos),"actual binding writes item components");
  require(!GTToolBlocks.DYNAMITE.get().remoteActivate(level,pos)&&fuse.remainingTicks()==20,"remote fuse and deliberate unbind response");level.removeBlock(pos,false);
  require(GTToolBlocks.BOOMSTICK.get().fortune()==3&&GTToolBlocks.DYNAMITE_STRONG.get().maxExplosionResistance()==40,"original charge variants");
  com.mojang.logging.LogUtils.getLogger().info("POWERED_TOOL_CHECKPOINT_SUCCESS {}","{\"actualAssembly\":true,\"capacityAndCharge\":true,\"partialCharge\":true,\"materialWear\":true,\"componentRoundTrip\":true,\"networkRoundTrip\":true,\"worldIgnitionTickDefuse\":true,\"fuseNbtRoundTrip\":true,\"remoteBinding\":true}");
 }
}
