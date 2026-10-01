package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.machine.crucible.CrucibleContentsNbt;
import com.gregtech.gregtech.api.machine.crucible.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.generated.GT6Materials.*;
import com.gregtech.gregtech.registry.GTMachines;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class CrucibleSurvivalTests {
 @GameTest(template="test_empty") public static void reductionConsumesFluxAndHonorsYield(GameTestHelper h) {
  var content=new ArrayList<>(List.of(CrucibleMaterialStack.of(Ores.Fe2O3,5*GTValues.U),CrucibleMaterialStack.of(Elements.C,GTValues.U),CrucibleMaterialStack.of(Ores.CaCO3,GTValues.U)));
  h.assertTrue(!CrucibleReactions.react(content,300),"Cold charge cannot react");
  h.assertTrue(CrucibleReactions.react(content,Elements.Fe.getMeltingPoint()),"Hematite carbon calcite reduction");
  h.assertTrue(content.size()==1&&content.get(0).material==Elements.Fe&&content.get(0).amount==2*GTValues.U,"Only two iron units, flux not added to yield");
  var noFlux=new ArrayList<>(List.of(CrucibleMaterialStack.of(Ores.Fe2O3,5*GTValues.U),CrucibleMaterialStack.of(Elements.C,GTValues.U)));
  h.assertTrue(!CrucibleReactions.react(noFlux,Elements.Fe.getMeltingPoint()),"Hematite requires flux");h.succeed();
 }
 @GameTest(template="test_empty") public static void charcoalActuallyTransformsInsideCrucible(GameTestHelper h) {
  var pos=new BlockPos(1,1,1);var state=GTMachines.SMELTING_CRUCIBLE_CARBON.get().defaultBlockState();h.setBlock(pos,state);
  var be=(SmeltingCrucibleBlockEntity)h.getBlockEntity(pos);
  var tag=new CompoundTag();tag.putLong("gt.temperature",1700);tag.putLong("gt.temperature.old",1699);
  CrucibleContentsNbt.saveList("gt.materials",tag,CrucibleItemInput.parse(new ItemStack(Items.CHARCOAL)));
  be.load(tag);SmeltingCrucibleBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),state,be);
  var saved=be.saveWithoutMetadata();var contents=CrucibleContentsNbt.loadList("gt.materials",saved);
  h.assertTrue(contents.size()==1&&contents.get(0).material==Elements.C&&contents.get(0).amount==GTValues.U/2,"One charcoal becomes half carbon unit in ticking block");
  var ore=CrucibleItemInput.parse(new ItemStack(Items.RAW_IRON));
  h.assertTrue(ore.size()==1&&ore.get(0).material==Ores.Fe2O3&&ore.get(0).amount==3*GTValues.U,"Raw iron uses hematite crushing target and multiplier");h.succeed();
 }
 @GameTest(template="test_empty") public static void allManufacturingIngredientsAreRegistered(GameTestHelper h) throws Exception {
  var manifest=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("../../docs/smeltery-survival-recipes.json"))).getAsJsonObject().getAsJsonObject("recipes");
  var errors=new ArrayList<String>();
  for(var entry:manifest.entrySet()) {
   var id=net.minecraft.resources.ResourceLocation.parse(entry.getKey());var found=h.getLevel().getRecipeManager().byKey(id);
   if(found.isEmpty()){errors.add(id+" missing recipe");continue;}
   var recipe=found.get();
   for(var ingredient:recipe.getIngredients())if(ingredient!=net.minecraft.world.item.crafting.Ingredient.EMPTY&&ingredient.getItems().length==0)errors.add(id+" empty ingredient: "+ingredient.toJson());
   if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped && errors.isEmpty()) {
    var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
     public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
     public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
    };
    var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
    for(int y=0;y<shaped.getHeight();y++)for(int x=0;x<shaped.getWidth();x++) {
     var ingredient=shaped.getIngredients().get(x+y*shaped.getWidth());
     if(ingredient.getItems().length==0)continue;
     var stack=ingredient.getItems()[0].copy();
     if(stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)stack=com.gregtech.gregtech.item.GTToolItem.create(
       com.gregtech.gregtech.api.tool.GTToolHelper.getType(stack),com.gregtech.gregtech.content.material.Materials.Steel,GTMaterialRegistry.get("Wood"));
     grid.setItem(x+y*3,stack);
    }
    h.assertTrue(shaped.matches(grid,h.getLevel()),"Manufacturing pattern executable: "+id);
    h.assertTrue(!shaped.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"Real manufacturing result");
    var remains=shaped.getRemainingItems(grid);
    for(int slot=0;slot<9;slot++)if(grid.getItem(slot).getItem() instanceof com.gregtech.gregtech.item.GTToolItem)
     h.assertTrue(!remains.get(slot).isEmpty()&&remains.get(slot).getDamageValue()>0,"Tools returned with wear: "+id);
   }
   h.assertTrue(!recipe.getResultItem(h.getLevel().registryAccess()).isEmpty(),"Registered crafting result "+id);
  }
  java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/smeltery-survival-test-errors.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(errors));
  h.assertTrue(errors.isEmpty(),"Manufacturing missing ingredients: "+errors);h.succeed();
 }
}
