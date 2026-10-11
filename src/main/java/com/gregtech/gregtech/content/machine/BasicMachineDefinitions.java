package com.gregtech.gregtech.content.machine;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import java.util.List;
/** Native recipe-map boundary for the single shared original machine catalog. */
public final class BasicMachineDefinitions {
 private BasicMachineDefinitions() {}
 public static List<BasicMachineSpec> specifications() {
  return BasicMachineCatalog.specifications().stream().map(BasicMachineDefinitions::from).toList();
 }
 public static BasicMachineSpec from(com.gregtech.gregtech.api.machine.BasicMachineParameters p) {
  return new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),p.hardness(),p.blastResistance(),FaceConfig.from(p.faceConfig()),p.constructionMaterials(),MachineRecipeMaps.byMachineName(p.machineName()),p.parallelLimit(),p.energyInMin(),p.energyInMax());
 }
}
