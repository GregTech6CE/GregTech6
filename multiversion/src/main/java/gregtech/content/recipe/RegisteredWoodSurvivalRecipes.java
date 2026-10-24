package gregtech.content.recipe;
import gregtech.data.*;
import gregtech.registry.GTWoods;
import gregtech.block.wood.WoodSpecies;
import gregtech.api.material.*;
import gregtech.content.plant.WoodSlabCatalog;
import net.minecraft.world.item.ItemStack;
/** GT6 default log/beam sawing applied to the currently registered GT tree families.
 * No invented charcoal conversion for fireproof species or nether stems.
 */
public final class RegisteredWoodSurvivalRecipes extends OriginalRecipeBatch {
 public static final RegisteredWoodSurvivalRecipes INSTANCE=new RegisteredWoodSurvivalRecipes();
 public static void registerMaterials(){
  // PlankEntry binds the actual wood species, not the generic Wood fallback. Keep this
  // available before recipe generation, including shaped wooden-tool material matching.
  for(String wood:WoodSlabCatalog.WOODS){
   var material=material(wood);
   ItemMaterialRegistry.register(item("gt6ce:planks_"+wood,1).getItem(),MaterialPrefix.plate,material,GTValues.U);
   ItemMaterialRegistry.register(item("gt6ce:slab_"+wood,1).getItem(),null,material,GTValues.U/2);
  }
  for(var species:WoodSpecies.values()){
   var material=material(species.id());
   var bark=GTMaterialRegistry.get(species==WoodSpecies.CINNAMON?"Cinnamon":"Bark");
   ItemMaterialRegistry.register(GTWoods.log(species).asItem(),new ItemComposition(null,
           java.util.List.of(MaterialComponent.of(material,8*GTValues.U),MaterialComponent.of(bark,GTValues.U)),
           "GT6 WoodEntry",true));
   ItemMaterialRegistry.register(GTWoods.beam(species).asItem(),null,material,8*GTValues.U);
  }
  for(var wood:NaturalWoodCraftingRows.SPECIES){
   var material=gregtech.api.material.GTMaterialRegistry.get(wood.material());
   var bark=gregtech.api.material.GTMaterialRegistry.get(wood.barkMaterial());
   long unit=gregtech.api.material.GTValues.U;
   var components=java.util.List.of(gregtech.api.material.MaterialComponent.of(material,5*unit),
           gregtech.api.material.MaterialComponent.of(bark,unit));
   // WoodEntry:175-176 upgrades the original generic 4U log to 5U wood + 1U bark,
   // or 6U of the same material for rotten/mossy wood. PlankEntry contributes 1U.
   gregtech.api.material.ItemMaterialRegistry.register(item("gt6ce:log_"+wood.kind(),1).getItem(),
           new gregtech.api.material.ItemComposition(null,components,"GT6 WoodEntry Log1",true));
   gregtech.api.material.ItemMaterialRegistry.register(item("gt6ce:planks_"+wood.kind(),1).getItem(),MaterialPrefix.plate,material,unit);
  }
 }
 public static int register(){
  for(var species:WoodSpecies.values()) INSTANCE.wood(species);
  for(String species:new String[]{"crimson","warped"}) INSTANCE.netherWood(species);
  for(var wood:NaturalWoodCraftingRows.SPECIES) INSTANCE.naturalWood(wood);
  for(String wood:gregtech.content.plant.WoodSlabCatalog.WOODS)
   for(var coolant:SawingCoolants.variants(false))
    INSTANCE.add("saw/slab/"+wood+"/"+coolant.field(),MachineRecipeMaps.Cutter,72L*coolant.multiplier(),16,
     items(item("gt6ce:planks_"+wood,1)),items(item("gt6ce:slab_"+wood,2)),
     fluids(fluid(coolant.field(),3*coolant.multiplier())),null);
  for(String wood:gregtech.content.plant.WoodSlabCatalog.WOODS) INSTANCE.slabUses(wood);
  return INSTANCE.entries().size();
 }
 /**
  * Row count the current species list produces: per species 2 sawing routes x coolants + 4 pressure
  * washer waters + 3 lathes, plus 2 nether woods x 2 block types x 2 routes x coolants.
  * Adding a species must only change this number, so the gametest asserts this formula.
  */
 public static int expectedRows(){
  int coolants=SawingCoolants.variants(false).size();
  return gregtech.content.plant.WoodSlabCatalog.WOODS.size()*(coolants+1)+WoodSpecies.values().length*(2*coolants+7)+2*2*2*coolants+NaturalWoodCraftingRows.SPECIES.size()*(coolants+3);
 }
 /** Loader_Recipes_Woods:296-304: half a unit of wood, or petrify nine slabs. */
 private void slabUses(String wood){
  String name=gregtech.content.plant.WoodSlabCatalog.material(wood);
  var material=gregtech.api.material.GTMaterialRegistry.get(name);
  var slab=item("gt6ce:slab_"+wood,1);
  pulverize(item("gt6ce:planks_"+wood,1),wood,GTValues.U);
  gregtech.api.material.ItemMaterialRegistry.register(slab.getItem(),null,material,
          gregtech.api.material.GTValues.U/2);
  var dust=MortarGrindingRecipes.pulverize(material,gregtech.api.material.GTValues.U/2);
  // pulverizing registers both the crusher and the shredder, as in the original helper.
  if(dust.isEmpty())throw new IllegalStateException("Missing wood slab pulverization material "+name);
  MachineRecipeMaps.pulverizing(slab,dust);
  MachineRecipeMaps.generify(slab,new ItemStack(net.minecraft.world.level.block.Blocks.OAK_SLAB));
  add("petrify/slab/"+wood,MachineRecipeMaps.Injector,64,16,items(item("gt6ce:slab_"+wood,9)),
          items(mat(MaterialPrefix.rockGt,"PetrifiedWood",2)),fluids(fluid("MnWtr",500)),null);
 }
 static GTMaterial material(String wood){return GTMaterialRegistry.get(WoodSlabCatalog.material(wood));}
 /** Loader_Recipes_Woods:168/191/212/278/296 uses the species' mTargetPulver. */
 static ItemStack pulp(String wood,long amount){
  var result=MortarGrindingRecipes.pulverize(material(wood),amount);
  if(result.isEmpty())throw new IllegalStateException("Missing wood pulp "+wood+"/"+amount);
  return result;
 }
 static void pulverize(ItemStack input,String wood,long amount){
  MachineRecipeMaps.pulverizing(input,pulp(wood,amount));
 }
 private void saw(String id,ItemStack input,ItemStack... output){
  for(var coolant:SawingCoolants.variants(false))
   add("saw/"+id+"/"+coolant.field(),MachineRecipeMaps.Cutter,128L*coolant.multiplier(),16,items(input),output,
    fluids(fluid(coolant.field(),4*coolant.multiplier())),null);
 }
 private void wood(WoodSpecies s){
  var log=new ItemStack(GTWoods.log(s));var beam=new ItemStack(GTWoods.beam(s));
  String name=WoodSlabCatalog.material(s.id());
  var bark=mat(MaterialPrefix.dust,s==WoodSpecies.CINNAMON?"Cinnamon":"Bark",1);
  saw("gt_log/"+s.id(),log,new ItemStack(GTWoods.planks(s),6),bark);
  saw("gt_beam/"+s.id(),beam,new ItemStack(GTWoods.planks(s),7),mat(MaterialPrefix.dust,name,1));
  MachineRecipeMaps.pulverizing(log,pulp(s.id(),6*GTValues.U),bark,50);
  pulverize(beam,s.id(),7*GTValues.U);
  for(String water:new String[]{"Water","DistW","SpDew","MnWtr"})
   add("debark/"+s.id()+"/"+water,MachineRecipeMaps.PressureWasher,64,16,items(log),items(beam,bark),fluids(fluid(water,200)),null);
  add("lathe/log/"+s.id(),MachineRecipeMaps.Lathe,80,16,items(log),items(mat(MaterialPrefix.stickLong,name,4),mat(MaterialPrefix.dust,name,1)),null,null);
  add("lathe/beam/"+s.id(),MachineRecipeMaps.Lathe,80,16,items(beam),items(mat(MaterialPrefix.stickLong,name,5),mat(MaterialPrefix.dust,name,1)),null,null);
  add("lathe/planks/"+s.id(),MachineRecipeMaps.Lathe,16,16,items(new ItemStack(GTWoods.planks(s))),items(mat(MaterialPrefix.stick,name,2)),null,null);
 }
 private void naturalWood(NaturalWoodCraftingRows.Spec wood){
  var log=item("gt6ce:log_"+wood.kind(),1);
  var plank=item("gt6ce:planks_"+wood.kind(),1);
  var bark=wood.kind().equals("dry")?item("gt6ce:dry_bark",1):mat(MaterialPrefix.dust,wood.barkMaterial(),1);
  saw("natural/"+wood.kind(),log,item("gt6ce:planks_"+wood.kind(),3),bark);
  MachineRecipeMaps.pulverizing(log,pulp(wood.kind(),3*GTValues.U),bark,50);
  // The source explicitly sets the log's stick count to zero, leaving only the dust output.
  add("lathe/natural/"+wood.kind(),MachineRecipeMaps.Lathe,80,16,items(log),items(mat(MaterialPrefix.dust,wood.material(),1)),null,null);
  add("lathe/natural_plank/"+wood.kind(),MachineRecipeMaps.Lathe,16,16,items(plank),items(mat(MaterialPrefix.stick,wood.material(),2)),null,null);
  add("coke/natural/"+wood.kind(),MachineRecipeMaps.CokeOven,3600,0,items(log),items(new ItemStack(net.minecraft.world.item.Items.CHARCOAL)),null,fluids(fluid("Oil_Creosote",50)));
 }
 private void netherWood(String species){
  String name=WoodSlabCatalog.material(species);
  var bark=mat(MaterialPrefix.dust,"Bark",1);
  // 1.20 adaptation: reuse wood sawing, explicitly do not allow charcoal from non-fuel fungi.
  for(String type:new String[]{"stem","hyphae"}) {
   var raw=item("minecraft:"+species+"_"+type,1);var stripped=item("minecraft:stripped_"+species+"_"+type,1);
   saw(species+"_"+type,raw,item("minecraft:"+species+"_planks",6),bark);
   saw("stripped_"+species+"_"+type,stripped,item("minecraft:"+species+"_planks",7),mat(MaterialPrefix.dust,name,1));
   MachineRecipeMaps.pulverizing(raw,pulp(species,6*GTValues.U),bark,50);
   pulverize(stripped,species,7*GTValues.U);
  }
  pulverize(item("minecraft:"+species+"_planks",1),species,GTValues.U);
  pulverize(item("minecraft:"+species+"_stairs",1),species,3*GTValues.U/4);
  pulverize(item("minecraft:"+species+"_slab",1),species,GTValues.U/2);
 }
}
