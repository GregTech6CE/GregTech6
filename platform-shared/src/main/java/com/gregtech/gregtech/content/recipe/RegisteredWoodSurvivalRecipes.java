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
 public static void registerMaterials(){
  for(var wood:NaturalWoodCraftingRows.SPECIES){
   var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(wood.material());
   var bark=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(wood.barkMaterial());
   long unit=com.gregtech.gregtech.api.material.GTValues.U;
   var components=java.util.List.of(com.gregtech.gregtech.api.material.MaterialComponent.of(material,5*unit),
           com.gregtech.gregtech.api.material.MaterialComponent.of(bark,unit));
   // WoodEntry:175-176 upgrades the original generic 4U log to 5U wood + 1U bark,
   // or 6U of the same material for rotten/mossy wood. PlankEntry contributes 1U.
   com.gregtech.gregtech.api.material.ItemMaterialRegistry.register(item("gregtech:log_"+wood.kind(),1).getItem(),
           new com.gregtech.gregtech.api.material.ItemComposition(null,components,"GT6 WoodEntry Log1",true));
   com.gregtech.gregtech.api.material.ItemMaterialRegistry.register(item("gregtech:planks_"+wood.kind(),1).getItem(),null,material,unit);
  }
 }
 public static int register(){
  for(var species:WoodSpecies.values()) INSTANCE.wood(species);
  for(String species:new String[]{"crimson","warped"}) INSTANCE.netherWood(species);
  for(var wood:NaturalWoodCraftingRows.SPECIES) INSTANCE.naturalWood(wood);
  return INSTANCE.entries().size();
 }
 /**
  * Row count the current species list produces: per species 2 sawing routes x coolants + 4 pressure
  * washer waters + 1 lathe, plus 2 nether woods x 2 block types x 2 routes x coolants.
  * Adding a species must only change this number, so the gametest asserts this formula.
  */
 public static int expectedRows(){
  int coolants=SawingCoolants.variants(false).size();
  return WoodSpecies.values().length*(2*coolants+5)+2*2*2*coolants+NaturalWoodCraftingRows.SPECIES.size()*(coolants+3);
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
 private void naturalWood(NaturalWoodCraftingRows.Spec wood){
  var log=item("gregtech:log_"+wood.kind(),1);
  var plank=item("gregtech:planks_"+wood.kind(),1);
  var bark=wood.kind().equals("dry")?item("gregtech:dry_bark",1):mat(MaterialPrefix.dust,wood.barkMaterial(),1);
  saw("natural/"+wood.kind(),log,item("gregtech:planks_"+wood.kind(),3),bark);
  // The source explicitly sets the log's stick count to zero, leaving only the dust output.
  add("lathe/natural/"+wood.kind(),MachineRecipeMaps.Lathe,80,16,items(log),items(mat(MaterialPrefix.dust,wood.material(),1)),null,null);
  add("lathe/natural_plank/"+wood.kind(),MachineRecipeMaps.Lathe,16,16,items(plank),items(mat(MaterialPrefix.stick,wood.material(),2)),null,null);
  add("coke/natural/"+wood.kind(),MachineRecipeMaps.CokeOven,3600,0,items(log),items(new ItemStack(net.minecraft.world.item.Items.CHARCOAL)),null,fluids(fluid("Oil_Creosote",50)));
 }
 private void netherWood(String species){
  // 1.20 adaptation: reuse wood sawing, explicitly do not allow charcoal from non-fuel fungi.
  for(String type:new String[]{"stem","hyphae"}) {
   saw(species+"_"+type,item("minecraft:"+species+"_"+type,1),item("minecraft:"+species+"_planks",6),mat(MaterialPrefix.dust,"Bark",1));
   saw("stripped_"+species+"_"+type,item("minecraft:stripped_"+species+"_"+type,1),item("minecraft:"+species+"_planks",7),mat(MaterialPrefix.dust,"Wood",1));
  }
 }
}
