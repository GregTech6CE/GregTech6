package gregtech.content.material;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.addon.AddonMaterials;
/** Bounded source qual(long) cohort. Quality does not create tool types.
 * Outside this reviewed cohort keep legacy item eligibility, including AncientDebris.
 * This is not a complete tool-prefix parity profile. Explicit TOOL_HEAD remains a separate arm.
 */
public final class ReviewedSingleArgumentToolTypes {
 private ReviewedSingleArgumentToolTypes() {}
 public static boolean allowsLegacyQualityFallback(GTMaterial material) {
  if (material == null) return false;
  // Preserve the old receiver contract for noncanonical aliases; prefix hidden checks remain separate.
  if (material.resolve()!=material) return material.getToolQuality()>=1;
  GTMaterial m=material;
  if (!GTMaterialRegistry.isCoreMaterial(m) || AddonMaterials.owns(m)) return m.getToolQuality()>=1;
  boolean reviewed=switch(m.getId()) {
   case 8333 -> m.getName().equals("Redstone");
   case 8340 -> m.getName().equals("Nikolite");
   case 8802 -> m.getName().equals("TantalumHafniumCarbide");
   case 9003 -> m.getName().equals("BasalticMineralSand");
   case 9004 -> m.getName().equals("GraniticMineralSand");
   case 9104 -> m.getName().equals("Hematite");
   case 9105 -> m.getName().equals("Bauxite");
   case 9106 -> m.getName().equals("BrownLimonite");
   case 9109 -> m.getName().equals("Realgar");
   case 9111 -> m.getName().equals("Chalcopyrite");
   case 9113 -> m.getName().equals("Chromite");
   case 9115 -> m.getName().equals("Cobaltite");
   case 9118 -> m.getName().equals("Garnierite");
   case 9120 -> m.getName().equals("Ilmenite");
   case 9122 -> m.getName().equals("Magnetite");
   case 9125 -> m.getName().equals("Pyrite");
   case 9128 -> m.getName().equals("Scheelite");
   case 9133 -> m.getName().equals("Tungstate");
   case 9137 -> m.getName().equals("YellowLimonite");
   case 9145 -> m.getName().equals("Pentlandite");
   case 9192 -> m.getName().equals("Rutile");
   case 9193 -> m.getName().equals("Stolzite");
   case 9194 -> m.getName().equals("Ferberite");
   case 9195 -> m.getName().equals("Huebnerite");
   case 9196 -> m.getName().equals("Russellite");
   case 9197 -> m.getName().equals("Pinalite");
   case 9198 -> m.getName().equals("DiduraniumTrioxide");
   case 9199 -> m.getName().equals("DuraniumHexafluoride");
   case 9200 -> m.getName().equals("DuraniumHexachloride");
   case 9201 -> m.getName().equals("DuraniumHexabromide");
   case 9202 -> m.getName().equals("DuraniumHexaiodide");
   case 9203 -> m.getName().equals("DuraniumHexaastatide");
   case 9204 -> m.getName().equals("TritaniumDioxide");
   case 9205 -> m.getName().equals("TritaniumHexafluoride");
   case 9206 -> m.getName().equals("TritaniumHexachloride");
   case 9207 -> m.getName().equals("TritaniumHexabromide");
   case 9208 -> m.getName().equals("TritaniumHexaiodide");
   case 9209 -> m.getName().equals("TritaniumHexaastatide");
   case 9216 -> m.getName().equals("Arsenopyrite");
   case 9217 -> m.getName().equals("Wolframite");
   default -> false;
  };
  return reviewed ? m.getToolTypes()>=1 : m.getToolQuality()>=1;
 }
}
