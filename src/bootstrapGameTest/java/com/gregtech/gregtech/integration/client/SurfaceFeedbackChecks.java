package com.gregtech.gregtech.integration.client;
import com.google.gson.*;
import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.blockentity.*;
import com.gregtech.gregtech.content.plant.BerryBushCatalog;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.worldgen.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Native identity/drop/placement/growth/legacy boundary checks in the existing finite world launch. */
public final class SurfaceFeedbackChecks {
 private SurfaceFeedbackChecks(){}
 private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Surface identity: "+reason);}
 private static boolean plain(ItemStack stack){return stack.getTagElement("BlockEntityTag")==null;}
 public static void server(MinecraftServer server,JsonObject receipt){
  var level=server.overworld();var pos=new BlockPos(200,210,200);var target=pos.offset(0,0,4);
  var actor=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("44f94c84-852b-4aa0-8aa5-1d04e4e17269"),"SurfaceCheckpoint"));
  actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(200.5,210,202.5);actor.getInventory().clearContent();
  var bushes=new JsonArray();var springs=new JsonArray();int attachments=0;
  require(GTBushes.allBlocks().length==10+com.gregtech.gregtech.content.plant.MaterialBerryBushCatalog.variants().size()&&GTFluidSprings.allBlocks().length==8,"native variant registry counts");
  var certificate=ResourceLocation.fromNamespaceAndPath("gregtech","supporter_certificate");
  require(!BuiltInRegistries.BLOCK.containsKey(certificate)&&!BuiltInRegistries.ITEM.containsKey(certificate),"certificate remains registered");
  require(!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(certificate),"certificate BE remains registered");
  for(var berry:BerryBushCatalog.worldgenTypes()) {
   var block=GTBushes.byBerry(berry.id());var item=new ItemStack(block);
   require(plain(item),"new bush item carries identity data");
   require(GTBlockEntities.BUSH.get().isValid(block.defaultBlockState()),"bush BE variant coverage");
   level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);
   level.setBlock(pos,block.defaultBlockState().setValue(BushBlock.STAGE,1),3);
   var be=(BushBlockEntity)level.getBlockEntity(pos);var tag=new CompoundTag();tag.putInt("growth",255);tag.putString("berry","wrong_legacy_value");be.load(tag);
   require(be.berryId().equals(berry.id()),"legacy key overwrote typed berry");
   require(!be.saveWithoutMetadata().contains("berry"),"typed bush serializes identity");
   int increment=be.grow();require(increment>0&&be.stage()==2&&be.growth()==increment-1,"original byte growth counter");
   var drops=Block.getDrops(level.getBlockState(pos),level,pos,be);
   require(drops.size()==1&&drops.get(0).is(block.asItem())&&plain(drops.get(0)),"ordinary bush variant drop");
   var picked=block.getCloneItemStack(level,pos,level.getBlockState(pos));require(picked.is(block.asItem())&&plain(picked),"bush pick-block variant");
   level.setBlock(target,Blocks.AIR.defaultBlockState(),3);level.setBlock(target.below(),Blocks.DIRT.defaultBlockState(),3);
   actor.setItemInHand(InteractionHand.MAIN_HAND,drops.get(0));
   var hit=new BlockHitResult(Vec3.atCenterOf(target.below()).add(0,.5,0),Direction.UP,target.below(),false);
   var placed=((BlockItem)drops.get(0).getItem()).place(new BlockPlaceContext(actor,InteractionHand.MAIN_HAND,drops.get(0),hit));
   require(placed.consumesAction()&&level.getBlockState(target).is(block),"native bush item placement");
   var planted=(BushBlockEntity)level.getBlockEntity(target);require(planted.berryId().equals(berry.id())&&planted.growth()==0&&planted.stage()==0,"replaced bush species/progress reset");
   var expected=planted.berryStack(1);require(!expected.isEmpty(),"bush output absent");actor.getInventory().clearContent();
   level.setBlock(target,level.getBlockState(target).setValue(BushBlock.STAGE,3),3);
   var harvest=block.use(level.getBlockState(target),level,target,actor,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(target),Direction.UP,target,false));
   int count=actor.getInventory().countItem(expected.getItem());require(harvest.consumesAction()&&count>=1&&count<=2&&level.getBlockState(target).is(block)&&level.getBlockState(target).getValue(BushBlock.STAGE)==0,"native harvest/reset");
   actor.getInventory().clearContent();
   GTBushesFeature.placeBranches(level,pos,berry.id());
   for(var face:Direction.values())if(face!=Direction.DOWN){var branch=level.getBlockState(pos.relative(face));require(branch.is(block)&&branch.getValue(BushBlock.STAGE)==3&&branch.getValue(BushBlock.SUPPORT)==face.getOpposite().get3DDataValue(),"typed generated branch");attachments++;level.removeBlock(pos.relative(face),false);}
   bushes.add(BuiltInRegistries.BLOCK.getKey(block).toString());
  }
  // Empty-bush planting accepts the source specimen without consuming it; identity changes immediately.
  level.setBlock(pos,GTBushes.BUSH.get().defaultBlockState(),3);actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STRING,4));
  GTBushes.BUSH.get().use(level.getBlockState(pos),level,pos,actor,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
  require(level.getBlockState(pos).is(GTBushes.byBerry("minecraft:string"))&&actor.getMainHandItem().getCount()==4,"cotton specimen planting");
  // Old saved species becomes a registered variant, preserving growth and block-state stage/support.
  level.setBlock(pos,GTBushes.BUSH.get().defaultBlockState().setValue(BushBlock.STAGE,2),3);
  var oldBush=(BushBlockEntity)level.getBlockEntity(pos);var oldTag=new CompoundTag();oldTag.putString("berry","gregtech:blueberry");oldTag.putInt("growth",37);oldBush.load(oldTag);oldBush.tick();
  require(level.getBlockState(pos).is(GTBushes.byBerry("blueberry"))&&((BushBlockEntity)level.getBlockEntity(pos)).growth()==37&&level.getBlockState(pos).getValue(BushBlock.STAGE)==2,"legacy bush migration");
  for(var definition:GTFluidSpringsFeature.SPRINGS) {
   var block=GTFluidSprings.byFluid(definition.fluidId());require(GTBlockEntities.FLUID_SPRING.get().isValid(block.defaultBlockState()),"spring BE variant coverage");
   require(GTFluidSpringsFeature.placeSpring(level,pos,definition),"typed spring generation");
   var be=(FluidSpringBlockEntity)level.getBlockEntity(pos);require(be.fluidId().equals(definition.fluidId())&&be.amount()==definition.amount()&&be.fluid()!=null,"source spring identity/rate");
   var encoded=be.saveWithoutMetadata();require(!encoded.contains("spring")&&!encoded.contains("amount"),"typed spring serializes identity/rate");
   var drops=Block.getDrops(level.getBlockState(pos),level,pos,be);require(drops.size()==1&&drops.get(0).is(block.asItem())&&plain(drops.get(0)),"ordinary spring drop");
   var picked=block.getCloneItemStack(level,pos,level.getBlockState(pos));require(picked.is(block.asItem())&&plain(picked),"spring pick-block");
   level.setBlock(target,Blocks.AIR.defaultBlockState(),3);actor.setItemInHand(InteractionHand.MAIN_HAND,drops.get(0));
   var hit=new BlockHitResult(Vec3.atCenterOf(target.below()).add(0,.5,0),Direction.UP,target.below(),false);
   require(((BlockItem)drops.get(0).getItem()).place(new BlockPlaceContext(actor,InteractionHand.MAIN_HAND,drops.get(0),hit)).consumesAction()&&level.getBlockState(target).is(block),"native spring item placement");
   var placed=(FluidSpringBlockEntity)level.getBlockEntity(target);require(placed.fluidId().equals(definition.fluidId())&&placed.amount()==definition.amount(),"placed spring rate");
   level.setBlock(target.above(),Blocks.AIR.defaultBlockState(),3);require(placed.emit()&&level.getBlockState(target.above()).getFluidState().getType()==placed.fluid(),"native spring fluid emission");level.setBlock(target.above(),Blocks.STONE.defaultBlockState(),3);
   springs.add(BuiltInRegistries.BLOCK.getKey(block).toString());
  }
  level.setBlock(pos,GTFluidSprings.FLUID_SPRING.get().defaultBlockState(),3);var oldSpring=(FluidSpringBlockEntity)level.getBlockEntity(pos);
  var oldFluidTag=new CompoundTag();oldFluidTag.putString("spring","gregtech:liquid_light_oil");oldFluidTag.putInt("amount",6000);oldFluidTag.putBoolean("active",true);oldSpring.load(oldFluidTag);oldSpring.tick();
  var migrated=(FluidSpringBlockEntity)level.getBlockEntity(pos);require(level.getBlockState(pos).is(GTFluidSprings.byFluid("gregtech:liquid_light_oil"))&&migrated.active()&&migrated.amount()==6000,"legacy spring migration");
  receipt.add("flatBushVariantsChecked",bushes);receipt.add("flatSpringVariantsChecked",springs);receipt.addProperty("flatGeneratedBranchesChecked",attachments);receipt.addProperty("flatLegacyWorldConversionsChecked",2);receipt.addProperty("supporterCertificateRemoved",true);
  // Leave a palette checkpoint in the isolated save; this is a saved sample, not a restart assertion.
  for(int i=0;i<BerryBushCatalog.worldgenSize();i++){var checkpoint=pos.offset(i*2,0,12);level.setBlock(checkpoint.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(checkpoint,GTBushes.byBerry(BerryBushCatalog.worldgenByIndex(i).id()).defaultBlockState().setValue(BushBlock.STAGE,3),3);}
  for(int i=0;i<FluidSpringRules.SPRINGS.size();i++){var checkpoint=pos.offset(i*2,0,16);level.setBlock(checkpoint,GTFluidSprings.byFluid(FluidSpringRules.SPRINGS.get(i).fluidId()).defaultBlockState(),3);level.setBlock(checkpoint.above(),Blocks.STONE.defaultBlockState(),3);}
  actor.getInventory().clearContent();
 }
 public static void client(net.minecraft.client.Minecraft minecraft,JsonObject receipt){
  int models=0;var colors=new JsonObject();
  for(var berry:BerryBushCatalog.worldgenTypes()) {
   var block=GTBushes.byBerry(berry.id());var stack=new ItemStack(block);OriginFeedbackChecks.checkModel(minecraft,stack,true);
   int tint=minecraft.getItemColors().getColor(stack,1);require((tint&0xffffff)==BerryBushCatalog.inventoryColour(berry),"native bush inventory color");
   for(int stage=0;stage<=3;stage++) {var state=block.defaultBlockState().setValue(BushBlock.STAGE,stage);int world=minecraft.getBlockColors().getColor(state,minecraft.level,null,1);require((world&0xffffff)==BerryBushCatalog.stageColour(berry,stage),"native stage color without BE");}
   require(!stack.getHoverName().getString().startsWith("block.gregtech."),"untranslated bush variant");models++;colors.addProperty(BuiltInRegistries.BLOCK.getKey(block).getPath(),Integer.toHexString(tint));
  }
  for(var definition:GTFluidSpringsFeature.SPRINGS) {
   var block=GTFluidSprings.byFluid(definition.fluidId());var stack=new ItemStack(block);OriginFeedbackChecks.checkModel(minecraft,stack,true);
   var expected=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse(definition.fluidId())));
   require(com.gregtech.gregtech.client.FluidSpringClient.fluid(stack)==BuiltInRegistries.FLUID.get(ResourceLocation.parse(definition.fluidId())),"plain spring item texture fluid");
   require(minecraft.getItemColors().getColor(stack,0)==expected.getTintColor(),"plain spring item tint");
   var model=minecraft.getItemRenderer().getModel(stack,minecraft.level,minecraft.player,0);boolean fluidSprite=false;
   for(var pass:model.getRenderPasses(stack,false))for(var layer:pass.getRenderTypes(stack,false))for(int face=-1;face<6;face++)for(var quad:pass.getQuads(null,face<0?null:Direction.from3DDataValue(face),net.minecraft.util.RandomSource.create(1),net.minecraftforge.client.model.data.ModelData.EMPTY,layer))if(quad.isTinted()&&quad.getSprite().contents().name().equals(expected.getStillTexture()))fluidSprite=true;
   require(fluidSprite,"spring inventory has wrong fluid sprite");require(!stack.getHoverName().getString().startsWith("block.gregtech."),"untranslated spring variant");models++;
  }
  var pages=com.gregtech.gregtech.loaders.b.OriginCreativeContents.pages();require(pages.values().stream().flatMap(Collection::stream).filter(s->s.is(GTFluidSprings.FLUID_SPRING.get().asItem())).count()==0,"empty legacy spring in creative page");
  require(pages.get("untyped").stream().filter(s->s.getItem() instanceof BlockItem b && b.getBlock() instanceof com.gregtech.gregtech.block.FluidSpringBlock).count()==7,"typed creative spring count");
  receipt.addProperty("flatSurfaceInventoryModelsChecked",models);receipt.add("flatBushInventoryColors",colors);
 }
 public static void render(net.minecraft.client.gui.GuiGraphics graphics,net.minecraft.client.Minecraft minecraft,JsonObject receipt){
  var stacks=new ArrayList<ItemStack>();for(var berry:BerryBushCatalog.worldgenTypes())stacks.add(new ItemStack(GTBushes.byBerry(berry.id())));for(var spring:FluidSpringRules.SPRINGS)stacks.add(new ItemStack(GTFluidSprings.byFluid(spring.fluidId())));
  graphics.pose().pushPose();graphics.pose().translate(0,0,500);
  int y=8;graphics.fill(4,y,410,y+86,0xff121216);graphics.drawString(minecraft.font,"Registered bushes and springs (no species NBT)",8,y+4,0xffffff,false);
  var names=new JsonArray();for(int i=0;i<stacks.size();i++){var stack=stacks.get(i);int x=8+i%8*50,row=y+20+i/8*30;graphics.renderItem(stack,x+14,row);graphics.drawString(minecraft.font,minecraft.font.plainSubstrByWidth(stack.getHoverName().getString(),47),x,row+17,0xffffff,false);names.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());}receipt.add("renderedFlatSurfaceItems",names);graphics.pose().popPose();
 }
}
