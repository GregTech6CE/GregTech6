package com.gregtech.gregtech.integration.client;

import com.google.gson.*;
import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import com.gregtech.gregtech.content.food.*;
import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import com.gregtech.gregtech.item.SandwichBlockItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Finite native interaction checkpoint; never packaged in the distribution. */
public final class SandwichFeedbackChecks {
 private static final List<ItemStack> PREVIEWS = new ArrayList<>();
 private static void require(boolean ok, String reason) { if(!ok) throw new IllegalStateException("Sandwich checkpoint: "+reason); }
 private static ItemStack item(String id, int count) { var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",id)),count);require(!stack.isEmpty(),"missing "+id);return stack; }
 private static SandwichBlockEntity entity(net.minecraft.world.level.Level level, BlockPos pos) {return (SandwichBlockEntity)level.getBlockEntity(pos);}
 public static void server(MinecraftServer server, JsonObject receipt) {
  var level=server.overworld();var pos=new BlockPos(220,210,200);var target=pos.offset(4,0,0);
  var actor=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("7523aa72-6171-41f3-97ba-ec074e43aef5"),"SandwichCheckpoint"));
  actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(220.5,210,202.5);actor.getInventory().clearContent();actor.setShiftKeyDown(true);
  level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
  var starter=item("toasted_toast",23);actor.setItemInHand(InteractionHand.MAIN_HAND,starter);
  var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
  var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(actor,InteractionHand.MAIN_HAND,pos.below(),hit);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
  require(event.isCanceled()&&event.getCancellationResult().consumesAction()&&starter.getCount()==7,"subscribed sneak-toast placement/batch limit");
  var be=entity(level,pos);require(be.baseQuantity()==16&&be.contents().size()==1&&be.layerId(0)==253&&be.sizePixels()==2,"batch seeded creative sample");
  actor.setShiftKeyDown(false);
  java.util.function.Consumer<ItemStack> use=stack->{actor.setItemInHand(InteractionHand.MAIN_HAND,stack);actor.gameMode.useItemOn(actor,level,stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));};
  use.accept(item("tofu_bar",15));require(be.contents().size()==1&&actor.getMainHandItem().getCount()==15,"insufficient batch ingredients consumed");
  use.accept(item("tofu_bar",16));require(be.ingredient(2).getCount()==16&&actor.getMainHandItem().isEmpty(),"native food layer insertion");
  use.accept(item("tofu_bar",16));require(be.contents().size()==2&&actor.getMainHandItem().getCount()==16,"duplicate top layer accepted");
  use.accept(item("tomato_ketchup",3));require(be.contents().size()==2&&actor.getMainHandItem().getCount()==3,"insufficient bottle accepted");
  use.accept(item("tomato_ketchup",4));require(be.ingredient(4).getCount()==4&&be.layerId(4)==1&&actor.getInventory().countItem(item("tomato_ketchup",1).getItem())==0,"four bottles per sixteen sandwiches");
  require(actor.getInventory().countItem(com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem())==4,"empty bottles returned exactly once");
  use.accept(new ItemStack(Items.REDSTONE,16));require(be.redstoneEnabled()&&be.redstoneSignal()==4&&be.comparatorSignal()==4&&be.sizePixels()==5,"redstone count/height");
  use.accept(item("toasted_toast",16));require(be.sizePixels()==7&&be.comparatorSignal()==6&&be.redstoneSignal()==6,"height signal update");
  var drop=be.dropItem();require(drop.is(item("sandwich_block",1).getItem())&&drop.getCount()==16,"preserved batch outer count");
  for(var layer:SandwichBlockEntity.readItemIngredients(drop))if(!layer.isEmpty())require(layer.getCount()==1,"batch ingredient count duplicated inside dropped item");
  var serialized=be.saveWithoutMetadata();require(serialized.getList("Ingredients",10).getCompound(0).getCompound("Stack").getInt("Count")==16,"world serialization lost batch count");
  var block=level.getBlockState(pos).getBlock();var picked=block.getCloneItemStack(level,pos,level.getBlockState(pos));require(picked.getCount()==1&&SandwichBlockEntity.itemFood(picked)==be.totalFood(),"pick block lost contents");
  PREVIEWS.clear();PREVIEWS.add(item("sandwich_block",1));PREVIEWS.add(drop.copy());
  level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(target,Blocks.AIR.defaultBlockState(),3);
  actor.setShiftKeyDown(true);actor.setItemInHand(InteractionHand.MAIN_HAND,drop);
  var targetHit=new BlockHitResult(Vec3.atCenterOf(target.below()).add(0,.5,0),Direction.UP,target.below(),false);
  var placed=((BlockItem)drop.getItem()).place(new BlockPlaceContext(actor,InteractionHand.MAIN_HAND,drop,targetHit));
  require(placed.consumesAction()&&drop.getCount()==15&&entity(level,target).baseQuantity()==1&&entity(level,target).dropItem().getCount()==1,"native replacement duplicated batch");
  PREVIEWS.add(entity(level,target).dropItem());
  require(entity(level,target).sizePixels()==7&&entity(level,target).redstoneSignal()==6,"replaced ingredient/signal preservation");
  var noSneak=pos.offset(8,0,0);level.setBlock(noSneak.below(),Blocks.STONE.defaultBlockState(),3);actor.setShiftKeyDown(false);
  var noSneakHit=new BlockHitResult(Vec3.atCenterOf(noSneak.below()).add(0,.5,0),Direction.UP,noSneak.below(),false);
  actor.setItemInHand(InteractionHand.MAIN_HAND,item("sandwich_block",2));require(!actor.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(actor,InteractionHand.MAIN_HAND,noSneakHit)).consumesAction()&&level.isEmptyBlock(noSneak),"sandwich item placed without sneaking");
  require(!SandwichPlacementHandler.tryPlace(level,actor,InteractionHand.MAIN_HAND,noSneakHit).consumesAction(),"toast handler accepted non-toast");
  var eatPos=pos.offset(0,0,8);level.setBlock(eatPos.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(eatPos,block.defaultBlockState(),3);var meal=entity(level,eatPos);meal.seedBase(item("toast",16));require(meal.addIngredient(item("chum",16))==16,"chum layer");
  var edible=meal.dropItem();require(SandwichBlockEntity.itemFood(edible)==6&&Math.abs(SandwichBlockEntity.itemSaturation(edible)-2.1f)<.0001,"source per-serving food/saturation");
  var foodItem=(SandwichBlockItem)edible.getItem();require(foodItem.getUseDuration(edible)==48,"source eating duration");
  PlayerFoodStats.clear(actor);PlayerRadiation.change(actor,-999);actor.removeAllEffects();actor.getFoodData().setFoodLevel(2);actor.getFoodData().setSaturation(0);actor.getInventory().clearContent();
  var consumed=foodItem.finishUsingItem(edible,level,actor);require(consumed.getCount()==15&&actor.getFoodData().getFoodLevel()==8&&actor.getFoodData().getSaturationLevel()==8,"native sandwich consumption");
  require(PlayerFoodStats.get(actor,GTFoodStats.DEHYDRATION)==20&&PlayerFoodStats.get(actor,GTFoodStats.SUGAR)==0&&PlayerRadiation.dose(actor)==0,"layer statistics multiplied by batch");
  var hunger=actor.getEffect(MobEffects.HUNGER);require(hunger!=null&&hunger.getDuration()==1000&&hunger.getAmplifier()==4&&hunger.isVisible(),"source chum guaranteed effect");
  require(actor.getInventory().isEmpty(),"second ingredient containers on eating");
  SandwichNutrition.apply(item("antidote",1),actor);actor.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));SandwichNutrition.apply(item("antidote",1),actor);require(actor.hasEffect(MobEffects.POISON),"original negative-duration callback gate changed");
  actor.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));SandwichNutrition.apply(item("royal_jelly",1),actor);require(!actor.hasEffect(MobEffects.POISON)&&actor.getEffect(MobEffects.REGENERATION).getDuration()==150&&actor.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"royal jelly milk and regeneration callbacks");
  actor.setRemainingFireTicks(200);SandwichNutrition.apply(item("cure_all",1),actor);require(!actor.isOnFire()&&PlayerFoodStats.get(actor,GTFoodStats.DEHYDRATION)==0&&actor.getEffect(MobEffects.REGENERATION).getAmplifier()==100&&actor.getEffect(MobEffects.SATURATION).getDuration()==100,"cure-all callback");
  actor.removeAllEffects();PlayerFoodStats.clear(actor);
  int ingredients=0;for(var row:SandwichSourceIngredients.ROWS){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(row.item())));require(!stack.isEmpty()&&SandwichIngredients.forItem(stack).id()==row.layer(),"unresolved source ingredient "+row.item());ingredients++;}
  receipt.addProperty("sandwichSourceIngredientsChecked",ingredients);receipt.addProperty("sandwichNativeInteractionChecks",14);receipt.addProperty("sandwichNativeConsumptionChecks",7);receipt.addProperty("sandwichDroppedIngredientCountsNormalized",true);receipt.addProperty("sandwichSourceBottleFoods",SandwichBottleFoods.ROWS.size());receipt.addProperty("sandwichWorldBatchCount",16);
 }
 public static void client(net.minecraft.client.Minecraft minecraft,JsonObject receipt){
  require(PREVIEWS.size()==3,"preview fixture missing");
  for(var stack:PREVIEWS){require(minecraft.getItemRenderer().getModel(stack,minecraft.level,minecraft.player,0).isCustomRenderer(),"item model bypasses composition renderer");require(net.minecraftforge.client.extensions.common.IClientItemExtensions.of(stack).getCustomRenderer() instanceof com.gregtech.gregtech.client.SandwichItemRenderer,"native item extension missing");}
  var stack=PREVIEWS.get(1);var tips=stack.getTooltipLines(minecraft.player,TooltipFlag.NORMAL);int previous=-1;String[] expected={"toasted_toast","tomato_ketchup","tofu_bar","toasted_toast"};
  for(var id:expected){String name=item(id,1).getHoverName().getString();int found=-1;for(int i=previous+1;i<tips.size();i++)if(tips.get(i).getString().equals(name)){found=i;break;}require(found>previous,"layer tooltip order "+id);previous=found;}
  var slice=SandwichGeometry.boxes(4,SandwichLayerCatalog.of(23),1);require(slice.size()==4&&slice.get(0).x0()==1/16.0&&slice.get(3).z1()==15/16.0,"source four-slice geometry");
  var toast=SandwichGeometry.boxes(0,SandwichLayerCatalog.of(253),1).get(0);require(toast.x0()==.5/16.0&&toast.x1()==15.5/16.0&&toast.y1()==2/16.0,"source toast geometry");
  receipt.addProperty("sandwichInventoryModelsChecked",3);receipt.addProperty("sandwichLayerTooltipOrderChecked",4);
 }
 public static void render(net.minecraft.client.gui.GuiGraphics graphics,net.minecraft.client.Minecraft minecraft,JsonObject receipt){
  graphics.pose().pushPose();graphics.pose().translate(0,0,600);graphics.fill(4,96,410,168,0xff202024);graphics.drawString(minecraft.font,"Actual sandwich ingredients: sample / batch / replaced one",8,100,0xffffff,false);
  String[] names={"Creative sample","16 layered sandwiches","Replaced single serving"};for(int i=0;i<PREVIEWS.size();i++){graphics.pose().pushPose();graphics.pose().translate(20+i*130,116,0);graphics.pose().scale(2,2,2);graphics.renderItem(PREVIEWS.get(i),0,0);graphics.pose().popPose();graphics.drawString(minecraft.font,names[i],8+i*130,151,0xffffff,false);}graphics.pose().popPose();receipt.addProperty("sandwichRenderedPreviews",PREVIEWS.size());
 }
}
