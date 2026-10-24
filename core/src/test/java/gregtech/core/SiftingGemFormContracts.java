package gregtech.core;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.internal.mod.ModData;
import gregtech.content.recipe.*;
import gregtech.content.data.MaterialPrefix;
public final class SiftingGemFormContracts {
 private static int checks;
 private static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 public static void main(String[] args){
  ModData.bindPresence(id->id.equals("minecraft")||id.equals("gt6ce"));
  GTMaterialRegistry.init();GTMaterialRegistry.postInit();
  var ice=GTMaterialRegistry.get("Ice").resolve();
  check(ice.isValid()&&ice.getId()==9802,"canonical reviewed Ice identity");
  MaterialPrefix[] grades={MaterialPrefix.gemLegendary,MaterialPrefix.gemExquisite,MaterialPrefix.gemFlawless,
   MaterialPrefix.gem,MaterialPrefix.gemFlawed,MaterialPrefix.gemChipped,MaterialPrefix.dust};
  int no=0;
  for(var material:GTMaterialRegistry.sortedMaterials()){
   if(material.resolve()!=material)continue;
   for(int i=0;i<grades.length;i++){
    boolean expected=material==ice&&i<3;
    var actual=ReviewedSiftingGemForms.decision(material,grades[i]);
    check(actual==(expected?ReviewedOreSourceProfile.Decision.NO:ReviewedOreSourceProfile.Decision.UNKNOWN),
      "bounded identity/form decision "+material.getName()+"/"+grades[i].getName());
    if(actual==ReviewedOreSourceProfile.Decision.NO)no++;
   }
  }
  check(no==3,"exactly three source-proven absent upper forms, not whole-material ban");
  check(ReviewedSiftingGemForms.decision(null,MaterialPrefix.gem)==ReviewedOreSourceProfile.Decision.UNKNOWN,"null material unknown");
  check(ReviewedSiftingGemForms.decision(ice,null)==ReviewedOreSourceProfile.Decision.UNKNOWN,"null form unknown");
  for(var p:grades)check(!gregtech.internal.material.MaterialItemDefinitions.all().stream()
    .filter(d->d.material()==ice&&d.prefix()==p).toList().isEmpty(),"legacy Ice form definition retained "+p.getName());
  check(ReviewedOreSourceProfile.decision(GTMaterialRegistry.get("Cinnabar"),ReviewedOreSourceProfile.Rule.GRADED_GEM)==ReviewedOreSourceProfile.Decision.YES,"existing Cinnabar profile unchanged");
  check(ReviewedOreSourceProfile.decision(GTMaterialRegistry.get("Lapis"),ReviewedOreSourceProfile.Rule.GRADED_GEM)==ReviewedOreSourceProfile.Decision.NO,"existing Lapis profile unchanged");
  System.out.println("SiftingGemFormContracts: "+checks+" checks passed; runtime external override proof requires native gates");
 }
}
