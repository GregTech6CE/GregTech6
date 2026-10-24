package gregtech.content.machine;

import gregtech.internal.addon.MachineTuningRules;
import gregtech.internal.machine.BasicMachineParameters;
import gregtech.internal.machine.BasicMachineSpec;

import gregtech.internal.energy.FaceConfig;

import gregtech.data.MachineRecipeMaps;

import java.util.List;

/** Native recipe-map boundary for the single shared original machine catalog. */

public final class BasicMachineDefinitions {
 private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

 private BasicMachineDefinitions() {}

 public static List<BasicMachineSpec> specifications() {


  gregtech.internal.addon.AddonMachineRuntime.install();
  MachineTunings.collect();

  var result = new java.util.ArrayList<BasicMachineSpec>();
  var matched = new java.util.HashSet<MachineTuningRules.Key>();
  for (var parameters : BasicMachineCatalog.specifications()) {
   var applied = MachineTunings.find(parameters.machineName(), parameters.tier());
   if (applied != null) matched.add(new MachineTuningRules.Key(parameters.machineName(), parameters.tier()));
   result.add(applied == null ? from(parameters) : from(parameters, applied));
  }

//? if >=1.21.1 {
/*  result.add(from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.distillationTower()));
  result.add(from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.fusionReactor()));
  result.add(from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()));
*///?} else {
//?}

  result.addAll(gregtech.internal.addon.AddonMachineRuntime.specs());
  MachineTunings.requireMatched(matched);
  return List.copyOf(result);


 }

 public static BasicMachineSpec from(BasicMachineParameters p) {

  var spec = raw(p);

  gregtech.internal.addon.RegisteredMachineIndex.bind(spec);

  return spec;

 }

 /** Applies a merged tuning to one catalog row. Every other field keeps its built-in value. */
 public static BasicMachineSpec from(BasicMachineParameters p, MachineTuningRules.Merged applied) {
  var base = raw(p);
  var tuning = applied.tuning();
  long energyIn = tuning.energyIn() != null ? tuning.energyIn() : base.energyIn();
  int parallel = tuning.parallelLimit() != null ? tuning.parallelLimit() : base.parallelLimit();
  MachineTuningRules.checkEffectiveValues(new MachineTuningRules.Key(p.machineName(), p.tier()), energyIn, parallel);
  var tuned = new BasicMachineSpec(
          base.id(), base.material(), base.machineName(), base.energyType(), base.tier(),
          energyIn, base.energyOut(), base.hardness(), base.blastResistance(), base.faceConfig(),
          base.constructionMaterials(), base.recipeMap(), parallel,
          tuning.energyInMin() != null ? tuning.energyInMin() : base.energyInMin(),
          tuning.energyInMax() != null ? tuning.energyInMax() : base.energyInMax(),
          base.namespace(), base.controllerKind(),
          tuning.processingEfficiency() != null ? tuning.processingEfficiency() : base.processingEfficiency(),
          tuning.constantEnergy() != null ? tuning.constantEnergy() : base.constantEnergy(),
          base.structureKeys(),
          tuning.cheapOverclocking() != null ? tuning.cheapOverclocking() : base.cheapOverclocking(),
          base.cheapOverclockingDeclared() || tuning.cheapOverclocking() != null,
          base.minimumLightAbove(), base.searchOutputTanks(), base.exclusiveOutputs(),
          base.itemOutputTarget(), base.fluidOutputTarget(), base.itemInputSource(), base.fluidInputSource(),
          base.energyRangeDeclared() || tuning.energyInMin() != null || tuning.energyInMax() != null,
          base.parallelScalesDuration(), base.energySources(),
          base.suppressSideAutoInput(), base.unlimitedOutputTanks(),
          base.auxiliaryEnergyType(), base.auxiliaryEnergyIn(), base.auxiliaryEnergyMin(),
          base.auxiliaryEnergyMax(), base.auxiliaryEnergyCapacity(), base.auxiliaryFaceMask(),
          base.startupEnergy());
  logApplied(applied, base, tuned);
  gregtech.internal.addon.RegisteredMachineIndex.bind(tuned);
  return tuned;
 }

 private static BasicMachineSpec raw(BasicMachineParameters p) {
  return new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),p.hardness(),p.blastResistance(),FaceConfig.from(p.faceConfig()),p.constructionMaterials(),MachineRecipeMaps.byMachineName(p.machineName()),p.parallelLimit(),p.energyInMin(),p.energyInMax());
 }

 private static void logApplied(MachineTuningRules.Merged applied, BasicMachineSpec base, BasicMachineSpec tuned) {
  var t = applied.tuning();
  logField(applied, "parallelLimit", t.parallelLimit(), base.parallelLimit(), tuned.parallelLimit());
  logField(applied, "energyIn", t.energyIn(), base.energyIn(), tuned.energyIn());
  logField(applied, "energyInMin", t.energyInMin(), base.energyInMin(), tuned.energyInMin());
  logField(applied, "energyInMax", t.energyInMax(), base.energyInMax(), tuned.energyInMax());
  logField(applied, "processingEfficiency", t.processingEfficiency(), base.processingEfficiency(), tuned.processingEfficiency());
  logField(applied, "constantEnergy", t.constantEnergy(), base.constantEnergy(), tuned.constantEnergy());
  logField(applied, "cheapOverclocking", t.cheapOverclocking(), base.cheapOverclocking(), tuned.cheapOverclocking());
 }

 private static void logField(MachineTuningRules.Merged applied, String field, Object requested,
                              Object oldValue, Object newValue) {
  if (requested == null) return;
  LOGGER.info("[gt6ce] tuning: {} {}/T{} {}: {} -> {}", applied.owners().get(field),
          applied.tuning().machineName(), applied.tuning().tier(), field, oldValue, newValue);
 }

}
