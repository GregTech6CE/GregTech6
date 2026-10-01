package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTWoods;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import net.minecraft.world.item.ItemStack;
/** GT6 default log/beam sawing applied to the currently registered GT tree families.
 * No invented charcoal conversion for fireproof species or nether stems.
 */
public final class RegisteredWoodSurvivalRecipes extends OriginalRecipeBatch {
 public static final RegisteredWoodSurvivalRecipes INSTANCE=new RegisteredWoodSurvivalRecipes();
 public static int register(){
  for(var species:WoodSpecies.values()) INSTANCE.wood(species);
  for(String species:new String[]{"crimson","warped"}) INSTANCE.netherWood(species);
  return INSTANCE.entries().size();
 }
 /**
  * Row count the current species list produces: per species 2 sawing routes x coolants + 4 pressure
  * washer waters + 1 lathe, plus 2 nether woods x 2 block types x 2 routes x coolants.
  * Adding a species must only change this number, so the gametest asserts this formula.
  */
 public static int expectedRows(){
  int coolants=SawingCoolants.variants(false).size();
  return WoodSpecies.values().length*(2*coolants+5)+2*2*2*coolants;
 }
 private void saw(String id,ItemStack input,ItemStack... output){
  for(var coolant:SawingCoolants.variants(false))
   add("saw/"+id+"/"+coolant.field(),MachineRecipeMaps.Cutter,128L*coolant.multiplier(),16,items(input),output,
    fluids(fluid(coolant.field(),4*coolant.multiplier())),null);
 }
 private void wood(WoodSpecies s){
  var log=new ItemStack(GTWoods.log(s));var beam=new ItemStack(GTWoods.beam(s));
  saw("gt_log/"+s.id(),log,new ItemStack(GTWoods.planks(s),6),mat(MaterialPrefix.dust,"Bark",1));
  saw("gt_beam/"+s.id(),beam,new ItemStack(GTWoods.planks(s),7),mat(MaterialPrefix.dust,"Wood",1));
  for(String water:new String[]{"Water","DistW","SpDew","MnWtr"})
   add("debark/"+s.id()+"/"+water,MachineRecipeMaps.PressureWasher,64,16,items(log),items(beam,mat(MaterialPrefix.dust,"Bark",1)),fluids(fluid(water,200)),null);
  add("lathe/planks/"+s.id(),MachineRecipeMaps.Lathe,16,16,items(new ItemStack(GTWoods.planks(s))),items(new ItemStack(net.minecraft.world.item.Items.STICK,2)),null,null);
 }
 private void netherWood(String species){
  // 1.20 adaptation: reuse wood sawing, explicitly do not allow charcoal from non-fuel fungi.
  for(String type:new String[]{"stem","hyphae"}) {
   saw(species+"_"+type,item("minecraft:"+species+"_"+type,1),item("minecraft:"+species+"_planks",6),mat(MaterialPrefix.dust,"Bark",1));
   saw("stripped_"+species+"_"+type,item("minecraft:stripped_"+species+"_"+type,1),item("minecraft:"+species+"_planks",7),mat(MaterialPrefix.dust,"Wood",1));
  }
 }
}
