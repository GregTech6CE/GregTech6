package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Positive-profile/current-form compatibility census; outside84 remains UNKNOWN legacy, not a source-negative oracle. */
final class MagneticBatchAssertions {
 private static final Map<Integer,String> POSITIVE=Map.ofEntries(
        java.util.Map.entry(250, "Manganese"),
        java.util.Map.entry(260, "Iron"),
        java.util.Map.entry(270, "Cobalt"),
        java.util.Map.entry(278, "Cobalt60"),
        java.util.Map.entry(280, "Nickel"),
        java.util.Map.entry(600, "Neodymium"),
        java.util.Map.entry(670, "Holmium"),
        java.util.Map.entry(830, "Bismuth"),
        java.util.Map.entry(2220, "Adamantium"),
        java.util.Map.entry(8005, "Ferrite"),
        java.util.Map.entry(8030, "FerrousChloride"),
        java.util.Map.entry(8234, "VanadiumPentoxide"),
        java.util.Map.entry(8247, "Basalz"),
        java.util.Map.entry(8603, "Angmallen"),
        java.util.Map.entry(8604, "GoldInductive"),
        java.util.Map.entry(8606, "GildedIron"),
        java.util.Map.entry(8630, "Steel"),
        java.util.Map.entry(8634, "DamascusSteel"),
        java.util.Map.entry(8635, "Tungstensteel"),
        java.util.Map.entry(8637, "HSLASteel"),
        java.util.Map.entry(8639, "HSLASpringSteel"),
        java.util.Map.entry(8641, "DeepIron"),
        java.util.Map.entry(8642, "PigIron"),
        java.util.Map.entry(8643, "WroughtIron"),
        java.util.Map.entry(8644, "IronCompressed"),
        java.util.Map.entry(8645, "IronMagnetic"),
        java.util.Map.entry(8646, "SteelMagnetic"),
        java.util.Map.entry(8647, "NeodymiumMagnetic"),
        java.util.Map.entry(8648, "DarkIron"),
        java.util.Map.entry(8649, "MeteoricIron"),
        java.util.Map.entry(8650, "MeteoricSteel"),
        java.util.Map.entry(8653, "VanadiumSteel"),
        java.util.Map.entry(8661, "Invar"),
        java.util.Map.entry(8662, "Constantan"),
        java.util.Map.entry(8670, "ShadowIron"),
        java.util.Map.entry(8671, "ShadowSteel"),
        java.util.Map.entry(8672, "Ironwood"),
        java.util.Map.entry(8673, "Steeleaf"),
        java.util.Map.entry(8674, "Knightmetal"),
        java.util.Map.entry(8675, "FierySteel"),
        java.util.Map.entry(8679, "Thaumium"),
        java.util.Map.entry(8680, "DarkThaumium"),
        java.util.Map.entry(8685, "ChromiumDioxide"),
        java.util.Map.entry(8690, "MeteoricBlackSteel"),
        java.util.Map.entry(8691, "MeteoricBlueSteel"),
        java.util.Map.entry(8692, "MeteoricRedSteel"),
        java.util.Map.entry(8693, "MeteoflameSteel"),
        java.util.Map.entry(8694, "MeteoflameBlackSteel"),
        java.util.Map.entry(8695, "MeteoflameBlueSteel"),
        java.util.Map.entry(8696, "MeteoflameRedSteel"),
        java.util.Map.entry(8697, "FlamascusSteel"),
        java.util.Map.entry(8698, "Fireleaf"),
        java.util.Map.entry(8704, "VibraniumSteel"),
        java.util.Map.entry(8706, "Vibramantium"),
        java.util.Map.entry(8718, "BedrockHSLAAlloy"),
        java.util.Map.entry(8725, "PulsatingIron"),
        java.util.Map.entry(8727, "ConductiveIron"),
        java.util.Map.entry(8730, "ElectricalSteel"),
        java.util.Map.entry(8731, "ObsidianSteel"),
        java.util.Map.entry(8734, "SpectreIron"),
        java.util.Map.entry(8744, "AncientDebris"),
        java.util.Map.entry(8745, "Netherite"),
        java.util.Map.entry(8746, "NetherizedDiamond"),
        java.util.Map.entry(8747, "Efrine"),
        java.util.Map.entry(8757, "Meteorite"),
        java.util.Map.entry(8758, "FrozenIron"),
        java.util.Map.entry(8766, "HSLATungstenAlloy"),
        java.util.Map.entry(8782, "Tartarite"),
        java.util.Map.entry(8784, "Adamantine"),
        java.util.Map.entry(8801, "Infinity"),
        java.util.Map.entry(8803, "CastIron"),
        java.util.Map.entry(9003, "BasalticMineralSand"),
        java.util.Map.entry(9004, "GraniticMineralSand"),
        java.util.Map.entry(9104, "Hematite"),
        java.util.Map.entry(9106, "BrownLimonite"),
        java.util.Map.entry(9115, "Cobaltite"),
        java.util.Map.entry(9118, "Garnierite"),
        java.util.Map.entry(9120, "Ilmenite"),
        java.util.Map.entry(9122, "Magnetite"),
        java.util.Map.entry(9125, "Pyrite"),
        java.util.Map.entry(9126, "Pyrolusite"),
        java.util.Map.entry(9137, "YellowLimonite"),
        java.util.Map.entry(9143, "VanadiumMagnetite"),
        java.util.Map.entry(9145, "Pentlandite"));
 private static boolean expectedClass(GTMaterial m){m=m.resolve();return GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m)&&m.getName().equals(POSITIVE.get(m.getId()))||m.has(MaterialProperty.MAGNETIC);}
 static void audit(GameTestHelper h,List<String> failures){
  for(var p:POSITIVE.entrySet()){var m=GTMaterialRegistry.get(p.getValue()).resolve();h.assertTrue(m.getId()==p.getKey()&&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m),"paired magnetic positive identity "+p);}
  int materials=0,roles=0,deferred=0;var cohort=new java.util.TreeSet<String>();
  for(var m:GTMaterialRegistry.sortedMaterials()){
   if(m.resolve()!=m||!GTMaterialRegistry.isCoreMaterial(m)||AddonMaterials.owns(m))continue;
   var whole=GTItems.getStack(MaterialPrefix.crushedPurified,m,1);var tiny=GTItems.getStack(MaterialPrefix.crushedPurifiedTiny,m,1);
   if(whole.isEmpty()||tiny.isEmpty())continue;materials++;cohort.add(m.getId()+"/"+m.getName());
   h.assertTrue(!m.has(MaterialProperty.ANTIMATTER)&&!m.getName().equals("Bedrock")&&!GTItems.getStack(MaterialPrefix.dust,m,1).isEmpty(),"source magnetic outer-gate fixture "+m.getName());
   var products=m.getByProducts().stream().filter(p->p!=null&&p.isValid()&&expectedClass(p)!=expectedClass(m)).toList();
   if(m.getName().equals("Copper"))h.assertTrue(products.stream().map(GTMaterial::getName).toList().equals(List.of("Cobaltite","Nickel")),"literal Copper opposite source products");
   if(m.getName().equals("Magnetite"))h.assertTrue(products.stream().map(GTMaterial::getName).toList().equals(List.of("Gold","Stone")),"literal Magnetite opposite source products");
   if(m.getName().equals("MeteoricSteel"))h.assertTrue(products.isEmpty(),"literal MeteoricSteel/Hematite same-class exclusion");
   for(boolean small:new boolean[]{false,true}){
    roles++;var in=small?tiny:whole;var map=MachineRecipeMaps.MagneticSeparator;
    var accepted=map.mRecipeList.stream().filter(row->Arrays.stream(row.mInputs).anyMatch(x->!x.isEmpty()&&x.is(in.getItem()))).toList();
    var selected=map.findRecipe(List.of(in),List.of(),false,1,12);var warm=map.findRecipe(List.of(in),List.of(),false,1,12);
    boolean own=in.getItem() instanceof MaterialFormItem form&&form.getMaterial().resolve()==m&&form.getPrefix()==(small?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified);
    var explicit=ItemMaterialRegistry.explicit(in.getItem()).orElse(null);
    boolean ambiguous=!own||explicit!=null&&(!explicit.recoverable()||explicit.components().size()!=1||explicit.material().resolve()!=m||explicit.prefix()!=(small?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified)||explicit.amount()!=(small?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified).getMaterialWeight());
    if(ambiguous)deferred++;
    System.out.println("[ore-batch-magnetic] "+m.getId()+"/"+m.getName()+"/"+(small?"tiny":"whole")+" expected="+products.stream().map(GTMaterial::getName).toList()+" accepted="+accepted.size()+" selected="+(selected!=null)+" deferred="+ambiguous);
    h.assertTrue(warm==selected,"magnetic stable warm selection");
    if(ambiguous)continue; // explicit external provenance never becomes an inferred source prohibition
    if(products.isEmpty()) {if(!accepted.isEmpty()||selected!=null)failures.add(m.getName()+"/"+small+":unexpected");continue;}
    if(accepted.size()!=1||selected!=accepted.get(0)){failures.add(m.getName()+"/"+small+":missing/ambiguous");continue;}
    var row=selected;boolean correct=row.mOutputs.length==6&&row.mDuration==(small?16:144)&&row.mEUt==16&&row.mFluidInputs.length==0&&row.mFluidOutputs.length==0;
    if(correct)for(int i=0;i<6;i++){
     GTMaterial product=i==0?m:i-1<products.size()?products.get(i-1):m;int count=i==0?1:small?2:18;
     MaterialPrefix prefix=i==0?(small?MaterialPrefix.crushedCentrifugedTiny:MaterialPrefix.crushedCentrifuged):MaterialPrefix.crushedCentrifugedTiny;
     ItemStack out=GTItems.getStack(prefix,product,count);
     if(i>0&&(product.getId()==8500&&product.getName().equals("Stone")||out.isEmpty()))out=GTItems.getStack(MaterialPrefix.dustTiny,product,count);
     if(out.isEmpty()||!row.mOutputs[i].is(out.getItem())||row.mOutputs[i].getCount()!=count||row.getOutputChance(i)!=(i==0?10000:600))correct=false;
    }
    if(!correct)failures.add(m.getName()+"/"+small+":literal-vector/work");
   }
  }
  System.out.println("[ore-batch-magnetic-summary] materials="+materials+" roles="+roles+" deferred="+deferred+" positives="+POSITIVE.size());
  try {
   String identityDigest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((String.join("\n",cohort)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   h.assertTrue(identityDigest.equals("6eeb4f929b95684e8bf1b57fec8e6614d4bb9b590a7a85442b1fe4fee1a7659b"),"exact native magnetic539 identity cohort, got "+identityDigest);
   System.out.println("[ore-batch-magnetic-cohort] sha256="+identityDigest);
  }catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
  h.assertTrue(materials==539&&roles==1078&&deferred==0,"controlled magnetic census539/1078 and no ambiguous fixture, got "+materials+"/"+roles+"/"+deferred);
 }
}
