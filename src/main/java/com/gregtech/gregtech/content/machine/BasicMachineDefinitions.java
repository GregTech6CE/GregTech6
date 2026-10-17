package com.gregtech.gregtech.content.machine;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import java.util.List;
/** Native recipe-map boundary for the single shared original machine catalog. */
public final class BasicMachineDefinitions {
 private BasicMachineDefinitions() {}
 public static List<BasicMachineSpec> specifications() {
  com.gregtech.gregtech.api.addon.AddonMachineRuntime.install();
  var result = new java.util.ArrayList<BasicMachineSpec>();
  for (var parameters : BasicMachineCatalog.specifications()) result.add(from(parameters));
  result.addAll(com.gregtech.gregtech.api.addon.AddonMachineRuntime.specs());
  return List.copyOf(result);
 }
 public static BasicMachineSpec from(com.gregtech.gregtech.api.machine.BasicMachineParameters p) {
  var spec = new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),p.hardness(),p.blastResistance(),FaceConfig.from(p.faceConfig()),p.constructionMaterials(),MachineRecipeMaps.byMachineName(p.machineName()),p.parallelLimit(),p.energyInMin(),p.energyInMax());
  com.gregtech.gregtech.api.addon.RegisteredMachineIndex.bind(spec);
  return spec;
 }
}
