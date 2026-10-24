package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.api.addon.AddonMaterials;
import gregtech.api.recipe.*;
import gregtech.data.*;
import gregtech.registry.GTFluids;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import static gregtech.api.material.GTValues.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
/** Ignored proposal, NOT wired. Rules transcribed from original Ores/Handlers.
 * Deliberately requires reviewed source facts; unknown does not mean no fluid/no form.
 * Does not erase existing rows; caller retires ONLY its known generic producer at source.
 */
public final class SourceOreFollowonRows {
 private SourceOreFollowonRows() {}
 public enum Evidence { YES, NO, UNKNOWN }
 public record Environment(Evidence known, FluidStack stack) {}
 public interface Facts {
  /** Proof of baseline input family eligibility including source form registration/overrides. */
  Evidence eligible(GTMaterial m);
  Evidence mortar(GTMaterial m);
  Evidence pulverizingCinnabar(GTMaterial m);
  /** Real source-equivalent registered form; may honor external authoritative forms. */
  ItemStack direct(MaterialPrefix p, GTMaterial m, int n);
  /** Source environment phase and binding at requested U amount, rounded before scaling. */
  Environment environment(GTMaterial m,long amountU);
  ItemStack blockDust(GTMaterial m,int n);
 }
 private static boolean admitted(GTMaterial m,Facts f) {
  return m!=null&&m.isValid()&&!AddonMaterials.owns(m)&&!m.has(MaterialProperty.ANTIMATTER)
    &&f.eligible(m)==Evidence.YES;
 }
 private static GTMaterial by(GTMaterial m,int i) {
  return i<m.getByProducts().size()?m.getByProducts().get(i).resolve():m;
 }
 private static ItemStack ore(Facts f,MaterialPrefix p,GTMaterial m,int n) {
  var s=f.direct(p,m,n);return !s.isEmpty()?s:f.direct(MaterialPrefix.dustTiny,m,n);
 }
 private static Recipe emit(RecipeMap map,ItemStack in,ItemStack[] out,long[] chances,
                           FluidStack[] fin,FluidStack[] fout,long ticks,long eut) {
  if(in.isEmpty())return null;
  for(var s:out)if(s==null||s.isEmpty())return null;
  // No collision replacement: return exact accepted identity or null for caller ownership ledger.
  return map.addRecipe1(false,eut,ticks,chances,in,fin,fout,out);
 }
 /** Source Crushing50-140, bounded RAW input (no host gangue/dense/raw-block extrapolation).
  * Bonus material is always Cinnabar; this is a named source effect, not a generic gem heuristic.
  */
 public static Recipe rawCrusher(GTMaterial m,Facts f) {
  if(!admitted(m,f)||f.pulverizingCinnabar(m)==Evidence.UNKNOWN)return null;
  long multiplier=Math.multiplyExact((long)m.getOreMultiplier(),m.getOreProcessingMultiplier());
  int count=(int)Math.max(1,Math.min(64,Math.multiplyExact(m.getTargetCrushingAmount(),multiplier)/U));
  var target=m.getTargetCrushingMaterial().resolve();var main=f.direct(MaterialPrefix.crushed,target,count);
  if(main.isEmpty())main=f.direct(MaterialPrefix.dust,target,count);
  if(main.isEmpty())main=f.direct(MaterialPrefix.gem,target,count);
  if(main.isEmpty())return null;
  long chance=f.pulverizingCinnabar(m)==Evidence.YES?2500:0;
  for(String name:new String[]{"Mercury","Cinnabar","Redstone"}) {
   var identity=GTMaterialRegistry.get(name);
   if(identity!=null&&m.getByProducts().stream().anyMatch(b->b.resolve()==identity.resolve()))
    chance+=name.equals("Mercury")?1000:500;
  }
  ItemStack[] outputs={main,main.copy()};long[] chances={10000,10000};
  if(chance>0) {
   var cinnabar=GTMaterialRegistry.get("Cinnabar");if(cinnabar==null)return null;
   outputs=new ItemStack[]{main,main.copy(),f.direct(MaterialPrefix.gem,cinnabar,1)};
   chances=new long[]{10000,10000,chance};
  }
  return emit(MachineRecipeMaps.Crusher,f.direct(MaterialPrefix.oreRaw,m,1),outputs,chances,
   new FluidStack[0],new FluidStack[0],128L*count*Math.max(1,m.getToolQuality()+1),16);
 }
 /** Source Handlers145-150/139-144; repeated Div72 outputs must not be coalesced. */
 public static List<Recipe> shredder(GTMaterial m,Facts f) {
  if(!admitted(m,f)||f.mortar(m)==Evidence.UNKNOWN)return List.of();
  MaterialPrefix[] inputs={MaterialPrefix.crushed,MaterialPrefix.crushedPurified,MaterialPrefix.crushedCentrifuged,
   MaterialPrefix.crushedTiny,MaterialPrefix.crushedPurifiedTiny,MaterialPrefix.crushedCentrifugedTiny};
  MaterialPrefix d=MaterialPrefix.dust,t=MaterialPrefix.dustTiny,s=MaterialPrefix.dustSmall,v=MaterialPrefix.dustDiv72;
  MaterialPrefix[][] outputs={{d,t,v},{d,s},{d,s,t,v},{t,v},{t,v,v},{t,v,v,v}};
  var accepted=new ArrayList<Recipe>();long multiplier=f.mortar(m)==Evidence.YES?16:256;
  for(int i=0;i<inputs.length;i++) {
   var out=new ItemStack[outputs[i].length];long units=0;
   for(int j=0;j<out.length;j++) {out[j]=f.direct(outputs[i][j],m.getTargetPulverMaterial().resolve(),1);units+=outputs[i][j].getMaterialWeight();}
   long processed=Math.max(inputs[i].getMaterialWeight(),units);
   long numerator=Math.multiplyExact(processed,Math.multiplyExact(multiplier,m.getToolQuality()+1L));
   long ticks=Math.max(1,Math.floorDiv(Math.addExact(numerator,U-1),U));
   var r=emit(MachineRecipeMaps.Shredder,f.direct(inputs[i],m,1),out,new long[0],new FluidStack[0],new FluidStack[0],ticks,16);
   if(r!=null)accepted.add(r);
  }
  return List.copyOf(accepted);
 }
 /** Source Handlers672-673: fixed durations, flat Petrotheum fluid cost. */
 public static List<Recipe> petrotheum(GTMaterial m,Facts f) {
  if(!admitted(m,f))return List.of();var accepted=new ArrayList<Recipe>();
  var sand=GTMaterialRegistry.get("SluiceSand");
  if(sand==null||!sand.isValid())return List.of();
  for(int scale:new int[]{9,1}) {
   var fluid=OreFluidBindingAdapter.sourceLiquid(8245,"Petrotheum",scale*(U/50),true);if(fluid.isEmpty())continue;
   var in=f.direct(scale==9?MaterialPrefix.crushed:MaterialPrefix.crushedTiny,m,1);
   var out=new ItemStack[]{f.direct(scale==9?MaterialPrefix.crushedPurified:MaterialPrefix.crushedPurifiedTiny,m,2),
    f.direct(MaterialPrefix.crushedPurifiedTiny,m,scale),f.direct(scale==9?MaterialPrefix.dust:MaterialPrefix.dustTiny,sand,1)};
   var r=emit(MachineRecipeMaps.Sluice,in,out,new long[]{10000,5000,10000},new FluidStack[]{fluid},new FluidStack[0],scale*16L,16);
   if(r!=null)accepted.add(r);
  }return List.copyOf(accepted);
 }
 /** Source Ores464-470. Handles solid and fluid byproducts without changing slot probabilities. */
 public static List<Recipe> centrifuge(GTMaterial m,Facts f) {
  if(!admitted(m,f))return List.of();var accepted=new ArrayList<Recipe>();
  Environment[] env=new Environment[3];
  for(int j=0;j<3;j++){env[j]=f.environment(by(m,j),U72);if(env[j]==null||env[j].known()!=Evidence.YES)return List.of();}
  for(int scale:new int[]{9,1}) {
   var out=new ArrayList<ItemStack>();var chances=new ArrayList<Long>();var fluids=new ArrayList<FluidStack>();
   out.add(ore(f,MaterialPrefix.crushedCentrifugedTiny,m,scale));chances.add(10000L);
   for(int j=0;j<3;j++) {
    if(env[j].stack()==null||env[j].stack().isEmpty()) {out.add(ore(f,MaterialPrefix.crushedCentrifugedTiny,by(m,j),scale));chances.add(1000L);}
    else {var fluid=env[j].stack().copy();fluid.setAmount(Math.multiplyExact(fluid.getAmount(),scale));fluids.add(fluid);}
   }
   // Compaction is safe only with aligned chances; unlike dropping just a null stack.
   var r=emit(MachineRecipeMaps.Centrifuge,f.direct(scale==9?MaterialPrefix.crushedPurified:MaterialPrefix.crushedPurifiedTiny,m,1),
    out.toArray(ItemStack[]::new),chances.stream().mapToLong(Long::longValue).toArray(),new FluidStack[0],fluids.toArray(FluidStack[]::new),scale*16L*(m.getToolQuality()+1L),16);
   if(r!=null)accepted.add(r);
  }return List.copyOf(accepted);
 }
 /** Source Ores475-481; caller supplies only source ANY.Wax identities with proven liquid binding. */
 public static List<Recipe> cracking(GTMaterial m,List<GTMaterial> waxes,Facts f) {
  if(!admitted(m,f))return List.of();var accepted=new ArrayList<Recipe>();Environment[] env=new Environment[2];
  for(int j=0;j<2;j++){env[j]=f.environment(by(m,j),U/18);if(env[j]==null||env[j].known()!=Evidence.YES)return List.of();}
  for(var wax:waxes)for(int scale:new int[]{9,1}) {
   var liquid=OreFluidBindingAdapter.liquid(wax,scale==9?U:U9,true);if(liquid.isEmpty())continue;
   var steam=GTFluids.stack("Steam",scale*160);var water=GTFluids.stack("Water",scale);
   if(steam==null||steam.isEmpty()||water==null||water.isEmpty())continue;
   var out=new ArrayList<ItemStack>();var chances=new ArrayList<Long>();var fluids=new ArrayList<FluidStack>();
   out.add(f.direct(MaterialPrefix.dustTiny,m,scale));chances.add(10000L);fluids.add(water);
   for(int j=0;j<2;j++) {
    if(env[j].stack()==null||env[j].stack().isEmpty()) {out.add(f.direct(MaterialPrefix.dustTiny,by(m,j),scale));chances.add(4000L);}
    else {var fluid=env[j].stack().copy();fluid.setAmount(Math.multiplyExact(fluid.getAmount(),scale));fluids.add(fluid);}
   }
   var r=emit(MachineRecipeMaps.SteamCracking,f.direct(scale==9?MaterialPrefix.crushedCentrifuged:MaterialPrefix.crushedCentrifugedTiny,m,1),out.toArray(ItemStack[]::new),
    chances.stream().mapToLong(Long::longValue).toArray(),new FluidStack[]{steam,liquid},fluids.toArray(FluidStack[]::new),scale*16L,16);
   if(r!=null)accepted.add(r);
  }return List.copyOf(accepted);
 }
 /** Source Ores184-187; explicit source list/ANY.Glowstone supplied by reviewed caller, not all gems. */
 public static List<Recipe> cryotheumBath(GTMaterial m,Facts f) {
  if(!admitted(m,f))return List.of();var cryo=GTMaterialRegistry.get("Cryotheum");if(cryo==null)return List.of();
  var accepted=new ArrayList<Recipe>();MaterialPrefix[] inputs={null,MaterialPrefix.crushed,MaterialPrefix.crushedTiny};
  for(int i=0;i<3;i++) {
   var fluid=OreFluidBindingAdapter.liquid(cryo,i==0?U/10:U/20,true);if(fluid.isEmpty())continue;
   var out=f.direct(i==0?MaterialPrefix.gemExquisite:MaterialPrefix.gemFlawless,m,1);
   var r=emit(MachineRecipeMaps.Bath,(i==0?f.blockDust(m,1):f.direct(inputs[i],m,i==2?9:1)),new ItemStack[]{out},new long[0],new FluidStack[]{fluid},new FluidStack[0],256,0);
   if(r!=null)accepted.add(r);
  }return List.copyOf(accepted);
 }
}
