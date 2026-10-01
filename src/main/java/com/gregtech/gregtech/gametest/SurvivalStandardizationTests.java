package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class SurvivalStandardizationTests {
 @GameTest(template="test_empty") public static void vanillaOutputTargetsRetainMaterialAndNbt(GameTestHelper h){
  for(var pair:List.of(Map.entry("Iron",Items.IRON_INGOT),Map.entry("Gold",Items.GOLD_INGOT),Map.entry("Copper",Items.COPPER_INGOT))){
   var raw=GTItems.getStack(MaterialPrefix.ingot,GTMaterialRegistry.get(pair.getKey()),3);
   var result=MaterialUnification.canonical(raw);
   h.assertTrue(result.is(pair.getValue())&&result.getCount()==3,"Canonical vanilla output: "+pair.getKey());
   h.assertTrue(MaterialEquivalence.matches(raw,result),"Unification preserves exact form and material");
   raw.getOrCreateTag().putString("owner","keep");
   h.assertTrue(MaterialUnification.canonical(raw)==raw,"NBT-bearing output never replaced");
  }
  var plate=GTItems.getStack(MaterialPrefix.plate,GTMaterialRegistry.get("Iron"));
  h.assertTrue(MaterialUnification.canonical(plate).is(plate.getItem()),"Plate is not ingot");
  for(var recipe:MachineRecipeMaps.Compressor.mRecipeList)for(var out:recipe.mOutputs)
   h.assertTrue(MaterialUnification.canonical(out).getItem()==out.getItem(),"Published machine outputs already canonical");
  h.succeed();
 }
 @GameTest(template="test_empty") public static void treeSurvivalRoutesAreExecutable(GameTestHelper h){
  var entries=RegisteredWoodSurvivalRecipes.INSTANCE.entries();
  int expected=RegisteredWoodSurvivalRecipes.expectedRows();
  h.assertTrue(entries.size()==expected,expected+" tree processing/coolant rows, got "+entries.size());
  for(var entry:entries){var recipe=entry.recipe();
   h.assertTrue(entry.map().findRecipe(Arrays.asList(recipe.mInputs),Arrays.asList(recipe.mFluidInputs),false,entry.map().mInputItemsCount,entry.map().mOutputItemsCount)==recipe,"Recipe reachable: "+entry.id());
   var consumed=RecipeInputs.consume(recipe,Arrays.asList(recipe.mInputs),Arrays.asList(recipe.mFluidInputs),1);
   h.assertTrue(consumed!=null&&consumed.items().stream().allMatch(ItemStack::isEmpty),"Exact survival input consumed");
   h.assertTrue(!recipe.mOutputs[0].isEmpty(),"Real registered output");
  }h.succeed();
 }
 @GameTest(template="test_empty") public static void conveyorNamesAreCanonicalWithLegacyLookup(GameTestHelper h){
  for(String tier:List.of("ulv","lv","mv","hv","ev","iv","luv","zpm","uv","puv1","xv")){
   var current=GTTechnological.get("compact_electric_conveyor_"+tier);
   h.assertTrue(current!=null&&current==GTTechnological.get("compact__electric_conveyor_"+tier),"Legacy lookup resolves same item");
   h.assertTrue(!net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(current).getPath().contains("__"),"Canonical registry name");
  }h.succeed();
 }
 @GameTest(template="test_empty") public static void craftingTagsContainOriginalIngredients(GameTestHelper h) throws Exception {
  var mapping=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of("../../docs/standardized-ingredients.json")),new com.google.gson.reflect.TypeToken<Map<String,String>>(){}.getType());
  @SuppressWarnings("unchecked") var pairs=(Map<String,String>)mapping;
  var failures=new java.util.TreeMap<String,String>();
  for(var entry:pairs.entrySet()) {
   var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(entry.getKey()));
   h.assertTrue(item!=null&&item!=Items.AIR,"Existing source ingredient: "+entry.getKey());
   if(!new ItemStack(item).is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse(entry.getValue())))) {
    var form=MaterialEquivalence.form(new ItemStack(item));
    failures.put(entry.getKey(),form==null?"no material form":MaterialEquivalence.tagPath(form));
   }
  }
  java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/ingredient-tag-mismatches.json"),new com.google.gson.Gson().toJson(failures));
  h.assertTrue(failures.isEmpty(),"Crafting tag mismatches: "+failures);
  h.succeed();
 }
}
