package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.content.transport.PanelCatalog;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.item.*;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech_decorative_panels") @PrefixGameTestTemplate(false)
public final class DecorativePanelPortTests {
 private static ItemStack item(String id){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));if(stack.isEmpty())throw new IllegalStateException("Missing panel input "+id);return stack;}
 private static ItemStack tool(GTToolType kind){return GTToolItem.create(kind,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));}
 private static CraftingInput grid(List<ItemStack> stacks){return CraftingInput.of(3,3,stacks);}
 private static List<ItemStack> inputs(PanelCatalog.Spec spec,String color){
  var screw=item("gregtech:screw_iron");var plank=item(spec.input());
  if(!color.isEmpty())plank=com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(((BlockItem)plank.getItem()).getBlock(),DyeColor.byName(color,null));
  return new ArrayList<>(List.of(screw.copy(),tool(GTToolType.SAW),screw.copy(),screw.copy(),plank,screw.copy(),screw.copy(),tool(GTToolType.SCREWDRIVER),screw.copy()));
 }
 private static ToolShapedRecipe row(GameTestHelper h,PanelCatalog.Spec spec){return (ToolShapedRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gregtech:decorative_panels/"+spec.id())).orElseThrow().value();}
 private static ToolShapedRecipe wire(GameTestHelper h,ToolShapedRecipe row){var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{ToolShapedRecipe.SERIALIZER.streamCodec().encode(buffer,row);return ToolShapedRecipe.SERIALIZER.streamCodec().decode(buffer);}finally{buffer.release();}}
 @GameTest(template="test_empty",timeoutTicks=80)
 public static void allOriginalPanelRecipesUseExactInputsAndReturnWornTools(GameTestHelper h){
  h.assertTrue(PanelCatalog.CANONICAL.size()==78,"48 colored and 30 existing native woods");
  var screws=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("gregtech:screws/any_iron_or_steel"));
  for(String metal:List.of("iron","wroughtiron","steel","meteoriciron"))h.assertTrue(item("gregtech:screw_"+metal).is(screws),"native ANY.Iron screw member "+metal);
  h.assertTrue(!item("gregtech:screw_copper").is(screws),"non-iron screw excluded");
  for(var spec:PanelCatalog.CANONICAL){
   var row=row(h,spec);var stacks=inputs(spec,spec.color());var grid=grid(stacks);
   h.assertTrue(row.matches(grid,h.getLevel()),"registered exact recipe "+spec.id());var result=row.assemble(grid,h.getLevel().registryAccess());
   h.assertTrue(result.getCount()==6&&result.is(item("gregtech:"+spec.id()).getItem()),"six original panels "+spec.id());
   h.assertTrue(row.allowMirror()&&row.isAutocraftableByGT(),"CR.MIR and CR.NCC is not NO_AUTOCRAFTING");
   var remains=row.getRemainingItems(grid);
   for(int i=0;i<9;i++)if(i==1||i==7)h.assertTrue(remains.get(i).is(stacks.get(i).getItem())&&remains.get(i).getDamageValue()==((GTToolItem)stacks.get(i).getItem()).toolType().damagePerCraft(),"tools returned with source wear");else h.assertTrue(remains.get(i).isEmpty(),"six screws and source plank consumed");
   var decoded=wire(h,row);h.assertTrue(decoded.constructionColor().equals(spec.color())&&decoded.matches(grid,h.getLevel()),"native recipe network roundtrip retains color");
   if(!spec.color().isEmpty()){
    String other=spec.color().equals("red")?"blue":"red";
    h.assertTrue(!row.matches(grid(inputs(spec,other)),h.getLevel())&&!decoded.matches(grid(inputs(spec,other)),h.getLevel()),"another color cannot craft this panel "+spec.id());
    h.assertTrue(com.gregtech.gregtech.block.misc.ColoredConstructionBlock.itemColor(decoded.getIngredients().get(4).getItems()[0]).getName().equals(spec.color()),"recipe viewer ingredient restores source color");
   }
   stacks.set(1,ItemStack.EMPTY);h.assertTrue(!row.matches(grid(stacks),h.getLevel()),"missing saw refused");
   stacks=inputs(spec,spec.color());stacks.set(0,item("minecraft:iron_ingot"));h.assertTrue(!row.matches(grid(stacks),h.getLevel()),"ingot cannot substitute screw");
  }h.succeed();
 }
 private static PanelCoverHost host(GameTestHelper h){var pos=new BlockPos(2,1,2);h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:battery_box_lv")));return (PanelCoverHost)h.getLevel().getBlockEntity(h.absolutePos(pos));}
 private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,PanelCoverHost host){var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PanelPort"));p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(host.coverOwner().getBlockPos()));return p;}
 private static BlockHitResult hit(BlockPos pos,Direction face){return new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false);}
 @GameTest(template="test_empty",timeoutTicks=80)
 public static void everyPanelAttachesByHeldItemAndCannotPlaceStandalone(GameTestHelper h){
  var host=host(h);var pos=host.coverOwner().getBlockPos();var p=player(h,host);int index=0;
  for(var spec:PanelCatalog.ALL){var stack=item("gregtech:"+spec.id());h.assertTrue(stack.getItem() instanceof PanelItemView&&stack.getMaxStackSize()==16&&CoverItems.isCover(stack),"all registered source panels are 16-stack covers");
   if(!Set.of("panel_wood","panel_concrete","panel_cfoam","panel_asphalt","panel_colored_gray","panel_colored_black").contains(spec.id()))h.assertTrue(!(stack.getItem() instanceof BlockItem)&&!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("gregtech:"+spec.id())),"new variants do not register phantom placeable blocks");
   var face=Direction.from3DDataValue(index++%6);stack.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,stack);var context=new UseOnContext(p,InteractionHand.MAIN_HAND,hit(pos,face));
   h.assertTrue(stack.getItem().useOn(context).consumesAction()&&stack.getCount()==1&&host.getCover(face).is(stack.getItem()),"held survival item installs one on "+face+" "+spec.id());
   h.assertTrue(stack.getItem().useOn(context)==InteractionResult.FAIL&&stack.getCount()==1,"occupied face rejects without consuming");
   var saved=host.coverOwner().saveWithoutMetadata(h.getLevel().registryAccess());host.coverOwner().loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(host.getCover(face).is(stack.getItem()),"native tag roundtrip keeps distinct identity");
   var crowbar=tool(GTToolType.CROWBAR);p.setItemInHand(InteractionHand.MAIN_HAND,crowbar);h.assertTrue(PanelCoverInteraction.use(host,p,InteractionHand.MAIN_HAND,hit(pos,face),true).consumesAction()&&host.getCover(face).isEmpty()&&crowbar.getDamageValue()>0,"held crowbar removes cover with wear");
   h.assertTrue(p.getInventory().contains(stack),"removed panel returned to inventory");p.getInventory().clearContent();
   var local=new BlockPos(4,1,2);h.setBlock(local,Blocks.STONE);var stone=h.absolutePos(local);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
   h.assertTrue(stack.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(stone,Direction.UP)))==InteractionResult.FAIL&&stack.getCount()==1&&h.getLevel().getBlockState(stone.above()).isAir(),"no panel block placed on plain stone");
  }
  p.getAbilities().instabuild=true;var free=item("gregtech:panel_asphalt_red");p.setItemInHand(InteractionHand.MAIN_HAND,free);h.assertTrue(free.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(pos,Direction.UP))).consumesAction()&&free.getCount()==1,"creative attachment retains item");host.removeCover(Direction.UP);h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=60)
 public static void asphaltVariantsWalkSpeedAndCoverCollisionStayOriginal(GameTestHelper h){
  var host=host(h);var p=player(h,host);var pos=host.coverOwner().getBlockPos();
  for(var spec:PanelCatalog.ALL)if(spec.asphalt()){
   var stack=item("gregtech:"+spec.id());h.assertTrue(CoverItems.behavior(stack).equals(CoverUtilityBehaviors.ASPHALT_PANEL)&&host.attachCover(Direction.UP,stack),"all asphalt colors dispatch walking");
   p.setShiftKeyDown(false);p.setDeltaMovement(.2,.4,-.3);CoverWorldInteraction.walk(h.getLevel(),pos,p);var motion=p.getDeltaMovement();h.assertTrue(Math.abs(motion.x-.26)<1e-8&&motion.y==.4&&Math.abs(motion.z+.39)<1e-8,"1.3 horizontal factor preserves vertical motion");
   p.setShiftKeyDown(true);p.setDeltaMovement(.2,.4,-.3);CoverWorldInteraction.walk(h.getLevel(),pos,p);h.assertTrue(p.getDeltaMovement().equals(new Vec3(.2,.4,-.3)),"sneaking disables speed");
   var box=CoverWorldInteraction.collision(h.getLevel(),pos,net.minecraft.world.phys.shapes.Shapes.empty()).bounds();h.assertTrue(box.minY==.875&&box.maxY==1&&box.minX==0&&box.maxZ==1,"source two-pixel collision remains inside machine");host.removeCover(Direction.UP);
  }
  h.assertTrue(CoverUtilityBehaviors.asphaltFactor(.2,.3,true,false)==1,"in-water walking is unmodified");h.assertTrue(Arrays.equals(PanelCatalog.itemBounds(),new double[]{0,0,7,16,16,9}),"original centered two-pixel preview bounds");h.succeed();
 }
}
