// CORRECTED 17:31 UTC: original Al IS FURNACE from metal factory933<=1200. Old384 oracle RETRACTED.
// TESTS-ONLY baseline: uses only existing production APIs; no positive-overlay helper dependency.
package gregtech.gametest;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.internal.recipe.Recipe;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.*;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class AluminiumWeldingAuditTests {
 @GameTest(template="test_empty",timeoutTicks=20)
 public static void aluminium_original_easy_welding_registered_costs(GameTestHelper h) {
  var al=GTMaterialRegistry.get("Aluminium");
  h.assertTrue(al.getToolQuality()==2,"Al quality 2");
  h.assertTrue(gregtech.content.data.generated.MaterialWorkability.isFurnace(al),"Al inherits FURNACE via metal factory at933K");
  check(h,List.of(GTItems.getStack(MaterialPrefix.ingot,al,2),new ItemStack(GTTechnological.selectorTag(2))),
    GTItems.getStack(MaterialPrefix.ingotDouble,al,1),32);
  var ingotOutputs=new MaterialPrefix[]{MaterialPrefix.ingotTriple,MaterialPrefix.ingotQuadruple,MaterialPrefix.ingotQuintuple};
  for(int n=3;n<=5;n++)check(h,List.of(GTItems.getStack(MaterialPrefix.ingot,al,n),new ItemStack(GTTechnological.selectorTag(n))),GTItems.getStack(ingotOutputs[n-3],al,1),16L*n);
  check(h,List.of(GTItems.getStack(MaterialPrefix.ingot,al,9),new ItemStack(GTTechnological.selectorTag(9))),GTBlocks.getStack(BlockMaterialPrefix.blockSolid,al),144);
  check(h,List.of(GTItems.getStack(MaterialPrefix.bolt,al,4),new ItemStack(GTTechnological.selectorTag(4))),GTItems.getStack(MaterialPrefix.stick,al,1),8);
  check(h,List.of(GTItems.getStack(MaterialPrefix.bolt,al,8),new ItemStack(GTTechnological.selectorTag(8))),GTItems.getStack(MaterialPrefix.stickLong,al,1),16);
  check(h,List.of(GTItems.getStack(MaterialPrefix.stick,al,2),new ItemStack(GTTechnological.selectorTag(2))),GTItems.getStack(MaterialPrefix.stickLong,al,1),16);
  check(h,List.of(GTItems.getStack(MaterialPrefix.plateCurved,al,4),GTItems.getStack(MaterialPrefix.ring,al,1)),
    GTItems.getStack(MaterialPrefix.rotor,al,1),64);
  var plateForms=new MaterialPrefix[]{MaterialPrefix.plate,MaterialPrefix.plateDouble,MaterialPrefix.plateQuadruple,MaterialPrefix.plateDense};
  var casings=new BlockMaterialPrefix[]{BlockMaterialPrefix.casingMachine,BlockMaterialPrefix.casingMachineDouble,BlockMaterialPrefix.casingMachineQuadruple,BlockMaterialPrefix.casingMachineDense};
  long[] times={128,224,416,896};
  for(int i=0;i<times.length;i++)for(boolean longRod:new boolean[]{false,true})
   check(h,List.of(GTItems.getStack(plateForms[i],al,6),GTItems.getStack(longRod?MaterialPrefix.stickLong:MaterialPrefix.stick,al,longRod?2:4)),GTBlocks.getStack(casings[i],al),times[i]);
  // Independent original rows; do not take expected inputs/counts from PipeRecipes.entries().
  String[] pipeSizes={"tiny","small","medium","large","huge"};
  int[] curved={1,1,3,6,12},selectors={1,2,3,4,5},outputs={2,1,1,1,1};
  long[] pipeTicks={16,16,48,96,192};
  for(int i=0;i<pipeSizes.length;i++) {
   var pipe=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","pipe_"+pipeSizes[i]+"_aluminium")),outputs[i]);
   check(h,List.of(GTItems.getStack(MaterialPrefix.plateCurved,al,curved[i]),new ItemStack(GTTechnological.selectorTag(selectors[i]))),pipe,pipeTicks[i]);
  }
  h.succeed();
 }
 // Intentionally RED on current source if Copper native forms exist: original explicit easy duration64, port68.
 @GameTest(template="test_empty",timeoutTicks=20)
 public static void furnace_rotor_original_fixed_duration(GameTestHelper h) {
  var copper=GTMaterialRegistry.get("Copper");
  h.assertTrue(gregtech.content.data.generated.MaterialWorkability.isFurnace(copper),"Copper is FURNACE");
  check(h,List.of(GTItems.getStack(MaterialPrefix.plateCurved,copper,4),GTItems.getStack(MaterialPrefix.ring,copper,1)),GTItems.getStack(MaterialPrefix.rotor,copper,1),64);
  h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=20)
 public static void welding_base_display_and_work_contract(GameTestHelper h) {
  var al=GTMaterialRegistry.get("Aluminium");
  var r=MachineRecipeMaps.Welder.findRecipe(List.of(GTItems.getStack(MaterialPrefix.ingot,al,2),new ItemStack(GTTechnological.selectorTag(2))),List.of(),false,9,1,h.getLevel(),null,ItemStack.EMPTY);
  h.assertTrue(r!=null,"Registered Al double ingot exists");
  var display=gregtech.internal.recipe.RecipePowerStats.of(r,MachineRecipeMaps.Welder.mPower);
  h.assertTrue(display.time()==32 && display.timeUnit().equals("ticks"),"EMI base duration is ticks, not seconds");
  var t1=gregtech.internal.recipe.MachineWorkCost.calculate(r.mEUt,r.mDuration,1,false,10000,16,64,false);
  var t2=gregtech.internal.recipe.MachineWorkCost.calculate(r.mEUt,r.mDuration,1,false,10000,64,256,false);
  h.assertTrue(t1!=null && t1.minimumPower()==16 && t1.totalWork()==512,"T1 original work");
  h.assertTrue(t2!=null && t2.minimumPower()==64 && t2.totalWork()==1024,"T2 original overclock work");
  // This is core arithmetic, not a claim to exercise native energy delivery or wall-clock scheduling.
  long progress=0;for(int i=0;i<16;i++)progress=gregtech.internal.recipe.MachineWorkCost.advance(progress,t1.totalWork(),32);
  h.assertTrue(progress==t1.totalWork(),"Nominal T1 energy takes16 active ticks, distinct from32 base ticks");
  h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=20)
 public static void inherited_flags_aliases_and_hard_control(GameTestHelper h) {
  var al=GTMaterialRegistry.get("Aluminium");
  h.assertTrue(GTMaterialRegistry.get("Al").resolve()==al.resolve(),"Al alias resolves to canonical Aluminium");
  h.assertTrue(gregtech.content.data.generated.MaterialWorkability.isFurnace(al),"Al933K inherits FURNACE");
  h.assertTrue(gregtech.content.data.generated.MaterialWorkability.isMortarGrindable(al),"Al inherits MORTAR");
  for(String name:new String[]{"Tin","Zinc","Lead","SiliconDioxide","HardPlastic","WhiteEndSand","BlackEndSand","WaxRefractory"})
   h.assertTrue(gregtech.content.data.generated.MaterialWorkability.isFurnace(GTMaterialRegistry.get(name)),"Canonical source FURNACE: "+name);
  h.assertTrue(gregtech.content.data.generated.MaterialWorkability.isSoft(GTMaterialRegistry.get("GoldInductive")),"InductiveAlloy source field maps to GoldInductive id8604");
  var wax=GTMaterialRegistry.get("WaxRefractory");
  h.assertTrue(wax.getMeltingPoint()>1200 && gregtech.content.data.generated.MaterialWorkability.isFurnace(wax),"Postfactory heat does not erase wax FURNACE");
  var ti=GTMaterialRegistry.get("Titanium");
  h.assertTrue(!gregtech.content.data.generated.MaterialWorkability.isFurnace(ti),"Source-proven high-temperature Ti hard control; not a generic unknown-negative assertion");
  check(h,List.of(GTItems.getStack(MaterialPrefix.ingot,ti,2),new ItemStack(GTTechnological.selectorTag(2))),GTItems.getStack(MaterialPrefix.ingotDouble,ti,1),512);
  h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=20)
 public static void source_simple_capsules_use_registered_forms_and_retained_molds(GameTestHelper h) {
  // Literal original capsule-material subset, independent of production profile membership.
  var expected=java.util.Set.of("cell_wax","cell_wax_bee","cell_wax_plant","cell_wax_paraffin",
   "cell_wax_refractory","cell_wax_magic","cell_wax_amnesic","cell_wax_soulful",
   "cell_plastic","cell_tin","cell_tin_alloy","cell_aluminium");
  var seen=new java.util.HashSet<String>();int specs=0,checked=0;
  for(var spec:gregtech.internal.fluid.PortableFluidContainerSpec.values()) {
   if(!spec.shapeId().equals("cell"))continue;
   if(!expected.contains(spec.id()))continue;
   seen.add(spec.id());
   specs++;int forms=0;
   for(var prefix:new MaterialPrefix[]{MaterialPrefix.ingot,MaterialPrefix.dust,MaterialPrefix.nugget,MaterialPrefix.dustTiny}) {
    var input=GTItems.getStack(prefix,spec.material(),1);
    // Missing native forms stay missing; no stand-in item is generated or inserted by this test.
    if(input.isEmpty())continue;
    forms++;boolean tiny=prefix==MaterialPrefix.nugget||prefix==MaterialPrefix.dustTiny;
    for(String id:new String[]{"extruder_shape_capsulecellcontainer","low_heat_extruder_shape_capsulecellcontainer"}) {
     h.assertTrue(GTTechnological.get(id)!=null,"Registered capsule mold: "+id);
     var mold=new ItemStack(GTTechnological.get(id));
     var inputs=List.of(input,mold);
     var recipe=MachineRecipeMaps.Extruder.findRecipe(inputs,List.of(),false,6,6,h.getLevel(),null,ItemStack.EMPTY);
     h.assertTrue(recipe!=null,"Accepted capsule recipe: "+spec.id()+"/"+prefix.getName()+"/"+id);
     h.assertTrue(recipe.mEUt==16 && recipe.mDuration==(tiny?8:64),"Original simple capsule work");
     h.assertTrue(recipe.mOutputs.length==1 && recipe.mOutputs[0].getCount()==(tiny?1:9)
      && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.mOutputs[0].getItem()).equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","fluid_"+spec.id())),"Registered native capsule output identity");
     var after=gregtech.internal.recipe.RecipeInputs.consume(recipe,inputs,List.of(),1);
     h.assertTrue(after!=null && after.items().get(0).isEmpty() && after.items().get(1).is(mold.getItem())
      && after.items().get(1).getCount()==1,"Capsule consumes only material, retains mold");
     checked++;
    }
   }
   h.assertTrue(forms>0,"At least one real registered form for source-simple capsule "+spec.id());
  }
  h.assertTrue(seen.equals(expected) && specs==12 && checked>=24,"All12 existing source-simple capsule variants; no invented form requirement");
  h.succeed();
 }
 private static void check(GameTestHelper h,List<ItemStack> inputs,ItemStack output,long ticks){
  h.assertTrue(!output.isEmpty() && inputs.stream().noneMatch(ItemStack::isEmpty),"Native forms exist");
  Recipe recipe=MachineRecipeMaps.Welder.findRecipe(inputs,List.of(),false,9,1,h.getLevel(),null,ItemStack.EMPTY);
  h.assertTrue(recipe!=null,"Actual winning recipe exists: "+inputs);
  h.assertTrue(recipe.mEUt==16 && recipe.mDuration==ticks,"Winning cost expected16/"+ticks+", got"+recipe.mEUt+"/"+recipe.mDuration);
  h.assertTrue(recipe.mOutputs.length==1 && recipe.mOutputs[0].is(output.getItem()) && recipe.mOutputs[0].getCount()==output.getCount(),"Winning output unchanged");
  var after=gregtech.internal.recipe.RecipeInputs.consume(recipe,inputs,List.of(),1);
  h.assertTrue(after!=null,"Actual recipe consumption plan exists");
  for(int slot=0;slot<inputs.size();slot++) {
   var before=inputs.get(slot);var remaining=after.items().get(slot);
   boolean retained=before.getItem() instanceof gregtech.internal.recipe.RecipeCatalystLike catalyst && catalyst.isCatalyst();
   h.assertTrue(retained ? remaining.is(before.getItem()) && remaining.getCount()==before.getCount() : remaining.isEmpty(),
    "Original selector retained/material consumed at slot"+slot);
  }
 }
}
