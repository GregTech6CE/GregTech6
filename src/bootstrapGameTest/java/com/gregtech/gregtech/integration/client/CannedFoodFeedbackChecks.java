package com.gregtech.gregtech.integration.client;

import com.google.gson.*;
import com.gregtech.gregtech.content.food.*;
import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import com.gregtech.gregtech.item.CannedFoodItem;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Finite native can consumption/recipe checkpoint; excluded from delivered mods. */
final class CannedFoodFeedbackChecks {
 private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Canned food checkpoint: "+reason);}
 private static ItemStack item(String path,int count){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",path)),count);require(!stack.isEmpty(),"missing "+path);return stack;}
 private static boolean feed(net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.LivingEntity target) {
  var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,target);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);return event.isCanceled()&&event.getCancellationResult().consumesAction();
 }
 static void server(MinecraftServer server,JsonObject receipt){
  var level=server.overworld();var actor=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("a4d7fc25-5dfa-45d0-a47c-053b903ba56b"),"CannedFoodCheckpoint"));
  actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(230,210,200);int layers=0,rotting=0;var consumed=new JsonArray();
  for(var row:CannedFoodCatalog.ROWS){
   var stack=item(row.id(),2);require(stack.getItem() instanceof CannedFoodItem,"display-only can "+row.id());var can=(CannedFoodItem)stack.getItem();
   require(can.getUseDuration(stack)==row.useDuration(),"use duration "+row.id());require(can.getCraftingRemainingItem(stack).is(item("empty_food_can",1).getItem()),"crafting container "+row.id());
   actor.getInventory().clearContent();actor.setItemInHand(InteractionHand.MAIN_HAND,stack);actor.getFoodData().setFoodLevel(0);actor.getFoodData().setSaturation(0);actor.removeAllEffects();PlayerFoodStats.clear(actor);PlayerRadiation.change(actor,-999);actor.setAirSupply(200);
   var original=stack.copy();var returned=can.finishUsingItem(stack,level,actor);
   var event=new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(actor,original,0,returned);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
   var food=row.food().food();require(returned.getCount()==1&&returned.is(can),"serving quantity "+row.id());
   require(actor.getInventory().countItem(item("empty_food_can",1).getItem())==1,"duplicate/lost finish-event container "+row.id());
   require(actor.getFoodData().getFoodLevel()==food.level()&&Math.abs(actor.getFoodData().getSaturationLevel()-Math.min(food.level(),2f*food.level()*food.saturation()))<.0001,"nutrition "+row.id());
   var stats=row.food().stats();int[] expected={stats.alcohol(),stats.caffeine(),stats.dehydration(),stats.sugar(),stats.fat()};for(int i=0;i<5;i++)require(PlayerFoodStats.get(actor,i)==expected[i],"duplicate/missing statistic "+row.id()+"/"+i);
   require(actor.getAirSupply()==200+row.rebreathe(),"air replenishment "+row.id());
   if(row.id().contains("chum")){var effect=actor.getEffect(net.minecraft.world.effect.MobEffects.HUNGER);require(effect!=null&&effect.getDuration()==1000&&effect.getAmplifier()==4,"chum callback "+row.id());}
   if(row.layer()>0){require(SandwichIngredients.forItem(original).id()==row.layer(),"source sandwich layer "+row.id());layers++;}else require(SandwichIngredients.forItem(original)==null,"invented sandwich layer "+row.id());
   var named=original.copy();named.setHoverName(net.minecraft.network.chat.Component.literal("preserved can data"));;var rotten=can.getRotten(named);require(rotten.getCount()==2&&rotten.getHoverName().getString().equals("preserved can data"),"rotten quantity/data "+row.id());
   int meta=row.food().meta();if(meta>=20&&meta<32000){var target=((CannedFoodItem)rotten.getItem()).spec();require(target.food().meta()==10+meta%10,"wrong rotten variant "+row.id());rotting++;}else require(rotten==named,"unchanged rotten/air/unknown "+row.id());
   var trace=new JsonObject();trace.addProperty("item",row.id());trace.addProperty("food",food.level());trace.addProperty("saturation",actor.getFoodData().getSaturationLevel());trace.addProperty("air",actor.getAirSupply());trace.addProperty("container",1);consumed.add(trace);
  }
  require(layers==24&&rotting==42,"source can totals");
  // Last can returns into the active hand; creative neither consumes nor emits a can.
  actor.getInventory().clearContent();var last=item("canned_air",1);require(last.getItem().finishUsingItem(last,level,actor).is(item("empty_food_can",1).getItem())&&actor.getInventory().countItem(item("empty_food_can",1).getItem())==0,"last can return");
  actor.setGameMode(net.minecraft.world.level.GameType.CREATIVE);var creative=item("canned_hot_air",2);actor.setAirSupply(0);require(creative.getItem().finishUsingItem(creative,level,actor)==creative&&creative.getCount()==2&&actor.getAirSupply()==400&&actor.getInventory().countItem(item("empty_food_can",1).getItem())==0,"creative callback/consumption");actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  actor.getFoodData().setFoodLevel(20);actor.setItemInHand(InteractionHand.MAIN_HAND,item("canned_air",1));require(actor.getMainHandItem().getItem().use(level,actor,InteractionHand.MAIN_HAND).getResult().consumesAction(),"air cannot be used at full hunger");actor.stopUsingItem();actor.setItemInHand(InteractionHand.MAIN_HAND,item("tiny_food_can_meat",1));require(!actor.getMainHandItem().getItem().use(level,actor,InteractionHand.MAIN_HAND).getResult().consumesAction(),"ordinary food bypasses hunger gate");
  // Meat cans retain the source tamed animal healing/breeding and one empty can per serving.
  actor.getInventory().clearContent();var wolf=new net.minecraft.world.entity.animal.Wolf(net.minecraft.world.entity.EntityType.WOLF,level);wolf.setTame(true);;wolf.setHealth(7);var meat=item("tall_food_can_meat",2);actor.setItemInHand(InteractionHand.MAIN_HAND,meat);require(feed(actor,wolf)&&wolf.getHealth()==13&&meat.getCount()==1&&actor.getInventory().countItem(item("empty_food_can",1).getItem())==1,"dog healing container");wolf.setHealth(20);require(feed(actor,wolf)&&wolf.isInLove(),"dog breeding");
  var cat=new net.minecraft.world.entity.animal.Cat(net.minecraft.world.entity.EntityType.CAT,level);cat.setTame(true);;var fish=item("tiny_food_can_fish",2);actor.setItemInHand(InteractionHand.MAIN_HAND,fish);require(feed(actor,cat)&&cat.isInLove()&&fish.getCount()==1,"cat breeding");
  var craft=server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech","food/empty_food_can")).orElseThrow();var recipe=(CraftingRecipe)craft;var inputs=new ArrayList<ItemStack>();
  for(var ingredient:recipe.getIngredients()){var choices=ingredient.getItems();require(choices.length>0,"unresolved empty-can ingredient");var input=choices[0].copy();if(input.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)com.gregtech.gregtech.api.tool.GTToolHelper.write(input,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce"));inputs.add(input);}
  var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}},2,2);for(int i=0;i<4;i++)grid.setItem(i,inputs.get(i));
  require(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(item("empty_food_can",1).getItem()),"source empty can crafting");var remainders=recipe.getRemainingItems(grid);require(remainders.get(0).getDamageValue()>0&&remainders.get(1).getDamageValue()>0&&remainders.get(2).getDamageValue()>0&&remainders.get(3).isEmpty(),"tool damage/plate consumption");
  int routes=0;for(String id:new String[]{"canned_air","canned_hot_air","canned_space_air"}){
   var target=item(id,1).getItem();var fills=MachineRecipeMaps.Canner.mRecipeList.stream().filter(r->r.mOutputs.length>0&&r.mOutputs[0].is(target)&&r.mFluidInputs.length==1&&r.mFluidInputs[0].getAmount()==16000&&r.mDuration==64&&r.mEUt==16).toList();
   require(!fills.isEmpty(),"missing air filling "+id);var drained=MachineRecipeMaps.Canner.mRecipeList.stream().filter(r->r.mInputs.length==1&&r.mInputs[0].is(target)&&r.mFluidOutputs.length==1&&r.mFluidOutputs[0].getAmount()==16000&&r.mDuration==16&&r.mEUt==16).toList();require(drained.size()==1&&drained.get(0).mOutputs[0].is(item("empty_food_can",1).getItem()),"air uncanning "+id);routes+=2;
  }
  require(MachineRecipeMaps.Canner.mRecipeList.stream().anyMatch(r->r.mInputs.length==2&&r.mInputs[0].is(Items.ROTTEN_FLESH)&&r.mInputs[1].is(item("empty_food_can",1).getItem())&&r.mOutputs[0].is(item("small_food_can_rotten",1).getItem())),"rotten flesh canning");
  require(MachineRecipeMaps.RollBender.mRecipeList.stream().anyMatch(r->r.mOutputs.length>0&&r.mOutputs[0].is(item("empty_food_can",1).getItem())&&r.mDuration==64&&r.mEUt==16),"roll-bent empty can");
  receipt.add("cannedFoodsConsumed",consumed);receipt.addProperty("cannedFoodConsumptionChecks",57);receipt.addProperty("cannedSandwichIngredientsChecked",layers);receipt.addProperty("cannedRottenConversionsChecked",rotting);receipt.addProperty("cannedAirRoundTripRoutesChecked",routes);receipt.addProperty("cannedTameAnimalFeedChecks",3);receipt.addProperty("cannedContainerModeChecks",3);receipt.addProperty("emptyFoodCanCraftingChecked",true);
 }
 static void client(net.minecraft.client.Minecraft minecraft,JsonObject receipt){
  var pages=com.gregtech.gregtech.loaders.b.OriginCreativeContents.pages();var cans=pages.values().stream().flatMap(java.util.Collection::stream).filter(stack->stack.getItem() instanceof CannedFoodItem).toList();require(cans.size()==45,"hidden rotten/chum cans in creative page");
  int models=0;for(var row:CannedFoodCatalog.ROWS){var stack=item(row.id(),1);var model=minecraft.getItemRenderer().getModel(stack,minecraft.level,minecraft.player,0);int quads=0;for(var pass:model.getRenderPasses(stack,false))for(var type:pass.getRenderTypes(stack,false))for(int face=-1;face<6;face++)for(var quad:pass.getQuads(null,face<0?null:Direction.from3DDataValue(face),net.minecraft.util.RandomSource.create(1),net.minecraftforge.client.model.data.ModelData.EMPTY,type)){require(!quad.getSprite().contents().name().getPath().contains("missing"),"missing can texture "+row.id());quads++;}require(quads>0,"transparent can "+row.id());var tips=stack.getTooltipLines(minecraft.player,TooltipFlag.NORMAL);if(row.food().food().rotten())require(tips.stream().anyMatch(c->c.getString().equals(net.minecraft.client.resources.language.I18n.get("tooltip.gregtech.food.rotten"))),"missing rotten tooltip "+row.id());models++;}
  receipt.addProperty("cannedInventoryModelsChecked",models);receipt.addProperty("cannedCreativeVisible",cans.size());
 }
}
