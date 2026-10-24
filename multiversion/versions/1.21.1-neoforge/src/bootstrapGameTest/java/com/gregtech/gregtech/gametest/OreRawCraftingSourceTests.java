package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.AutocraftableCraftingRecipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.recipe.ToolShapelessRecipe;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
/** Source Handlers552-553/w557-558; AdvancedCrafting1ToY: one occupied cell, offsets0/1.
 * Reads actual registered recipes; never injects expected rows. Baseline two source managers. */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreRawCraftingSourceTests {
 private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("gt6ce","form_conversion/oreraw_to_gem/cinnabar_1");
 @GameTest(template="test_empty",batch="ore_source_differential")
 public static void cinnabarRawGemTwoByTwoPositions(GameTestHelper h){positions(h,2);h.succeed();}
 @GameTest(template="test_empty",batch="ore_source_differential")
 public static void cinnabarRawGemThreeByThreePositions(GameTestHelper h){positions(h,3);h.succeed();}
 @GameTest(template="test_empty",batch="ore_source_differential")
 public static void cinnabarRawGemOccupiedCellsNotStackCount(GameTestHelper h){
  var cells=empty(9);cells.set(0,raw(64));check(h,3,cells,true);
  cells.set(2,raw(1));check(h,3,cells,false);
  cells.set(2,new ItemStack(net.minecraft.world.item.Items.STICK));check(h,3,cells,false);
  check(h,3,empty(9),false);h.succeed();
 }
 private static void positions(GameTestHelper h,int side){
  for(int slot=0;slot<side*side;slot++){
   var cells=empty(side*side);cells.set(slot,raw(1));check(h,side,cells,slot%2==0);
  }
 }
 private static java.util.List<ItemStack> empty(int size){
  return new java.util.ArrayList<>(java.util.Collections.nCopies(size,ItemStack.EMPTY));
 }
 private static ItemStack raw(int count){return GTItems.getStack(MaterialPrefix.oreRaw,GTMaterialRegistry.get("Cinnabar"),count);}
 private static boolean gem(ItemStack stack){
  var form=MaterialEquivalence.form(stack);
  return form!=null&&form.prefix()==MaterialPrefix.gem&&form.material()==GTMaterialRegistry.get("Cinnabar").resolve();
 }
 private static void check(GameTestHelper h,int side,java.util.List<ItemStack> cells,boolean expected){
  var material=GTMaterialRegistry.get("Cinnabar").resolve();
  h.assertTrue(material.getId()==9114,"source Cinnabar9114 identity");
  h.assertTrue(!raw(1).isEmpty(),"actual builtin Cinnabar raw input exists");
  var rawForm=MaterialEquivalence.form(raw(1));
  h.assertTrue(rawForm!=null&&rawForm.prefix()==MaterialPrefix.oreRaw&&rawForm.material()==material,"fixture oreRaw/Cinnabar identity");
  var manager=h.getLevel().getRecipeManager();
  var registered=manager.byKey(ID).orElseThrow(()->new AssertionError("missing existing "+ID));
  var recipe=(CraftingRecipe)registered.value();
  h.assertTrue(recipe instanceof ToolShapelessRecipe,"existing source conversion implementation, not injected recipe");
  h.assertTrue(recipe instanceof AutocraftableCraftingRecipe a&&!a.isAutocraftableByGT(),"manual conversion is not GT autocraftable");
  var grid=CraftingInput.ofPositioned(side,side,cells).input();
  if(!grid.isEmpty()){
   var position=(com.gregtech.gregtech.recipe.CraftingFormPosition)grid;
   int first=0;while(cells.get(first).isEmpty())first++;
   h.assertTrue(position.gregtech$formGridSize()==side*side&&position.gregtech$formFirstSlot()==first,
       "Neo native position capture must preserve full grid before cropping");
  }
  h.assertTrue(recipe.matches(grid,h.getLevel())==expected,"source position/cell predicate side="+side+" cells="+cells);
  var selected=manager.getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel());
  if(expected){
   h.assertTrue(selected.isPresent(),"actual RecipeManager finds manual conversion");
   var selectedRecipe=selected.orElseThrow().value();var selectedId=selected.orElseThrow().id();
   h.assertTrue(selectedId.equals(ID),"actual recipe winner must be reviewed raw-to-gem conversion: "+selectedId);
   var result=selectedRecipe.assemble(grid,h.getLevel().registryAccess());
   h.assertTrue(gem(result)&&result.getCount()==1,"source exact gem/Cinnabar1 output");
   var direct=recipe.assemble(grid,h.getLevel().registryAccess());
   h.assertTrue(gem(direct)&&direct.getCount()==1,"registered recipe direct output matches selected output");
  } else if(selected.isPresent()){
   var selectedRecipe=selected.orElseThrow().value();var selectedId=selected.orElseThrow().id();
   var result=selectedRecipe.assemble(grid,h.getLevel().registryAccess());
   h.assertTrue(!selectedId.equals(ID)&&!gem(result),"invalid slot/cells must not leak a competing raw-to-Cinnabar-gem row");
   // A legitimate externally registered rawOreChunk may occupy odd slots; no blanket absence assertion.
  }
 }
}
