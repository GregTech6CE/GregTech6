package gregtech.content.machine;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.addon.MachineTuningRules;
import gregtech.engine.machine.BasicMachineParameters;
import gregtech.engine.machine.BasicMachineSpec;
import java.util.List;

/** Native recipe-map boundary for the single shared original machine catalog. */
public final class BasicMachineDefinitions {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private BasicMachineDefinitions() {}

    public static List<BasicMachineSpec> specifications() {

        gregtech.engine.addon.AddonMachineRuntime.install();
        MachineTunings.collect();

        var result = new java.util.ArrayList<BasicMachineSpec>();
        var matched = new java.util.HashSet<MachineTuningRules.Key>();
        for (var parameters : BasicMachineCatalog.specifications()) {
            var applied = MachineTunings.find(
                    parameters.machineName(), parameters.energy().tier());
            if (applied != null)
                matched.add(new MachineTuningRules.Key(
                        parameters.machineName(), parameters.energy().tier()));
            result.add(applied == null ? from(parameters) : from(parameters, applied));
        }

        result.add(from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.distillationTower()));
        result.add(from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.fusionReactor()));
        result.add(from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()));

        result.addAll(gregtech.engine.addon.AddonMachineRuntime.specs());
        MachineTunings.requireMatched(matched);
        return List.copyOf(result);
    }

    public static BasicMachineSpec from(BasicMachineParameters p) {

        var spec = raw(p);

        gregtech.engine.addon.RegisteredMachineIndex.bind(spec);

        return spec;
    }

    /** Applies a merged tuning to one catalog row. Every other field keeps its built-in value. */
    public static BasicMachineSpec from(BasicMachineParameters p, MachineTuningRules.Merged applied) {
        var base = raw(p);
        var tuning = applied.tuning();
        long energyIn = tuning.energyIn() != null ? tuning.energyIn() : base.energyIn();
        long energyInMin = tuning.energyInMin() != null ? tuning.energyInMin() : base.energyInMin();
        long energyInMax = tuning.energyInMax() != null ? tuning.energyInMax() : base.energyInMax();
        int parallel = tuning.parallelLimit() != null ? tuning.parallelLimit() : base.parallelLimit();
        var key = new MachineTuningRules.Key(p.machineName(), p.energy().tier());
        MachineTuningRules.checkEffectiveValues(key, energyIn, parallel);
        if (energyIn < energyInMin || energyIn > energyInMax)
            throw new IllegalStateException("Tuned energy for " + key + " sits outside its effective range: " + energyIn
                    + " not in " + energyInMin + ".." + energyInMax);
        var originalEnergy = base.energy();
        var range = new BasicMachineSpec.InputRange(
                energyInMin,
                energyInMax,
                originalEnergy.range().declared() || tuning.energyInMin() != null || tuning.energyInMax() != null);
        var energy = new BasicMachineSpec.Energy(
                originalEnergy.type(), originalEnergy.tier(), energyIn, originalEnergy.outputPerTick(), range);
        var originalProcessing = base.processing();
        var processing = new BasicMachineSpec.Processing(
                parallel,
                tuning.processingEfficiency() != null ? tuning.processingEfficiency() : originalProcessing.efficiency(),
                tuning.constantEnergy() != null ? tuning.constantEnergy() : originalProcessing.constantEnergy(),
                tuning.cheapOverclocking() != null
                        ? tuning.cheapOverclocking()
                        : originalProcessing.cheapOverclocking(),
                originalProcessing.cheapOverclockingDeclared() || tuning.cheapOverclocking() != null,
                originalProcessing.parallelScalesDuration(),
                originalProcessing.minimumLightAbove(),
                originalProcessing.searchOutputTanks(),
                originalProcessing.exclusiveOutputs());
        var tuned = new BasicMachineSpec(
                base.id(),
                base.namespace(),
                base.machineName(),
                base.casing(),
                base.recipeMap(),
                energy,
                processing,
                base.layout(),
                base.auxiliaryEnergy());
        logApplied(applied, base, tuned);
        gregtech.engine.addon.RegisteredMachineIndex.bind(tuned);
        return tuned;
    }

    private static BasicMachineSpec raw(BasicMachineParameters p) {
        return BasicMachineSpec.builtIn(p, MachineRecipeMaps.byMachineName(p.machineName()));
    }

    private static void logApplied(MachineTuningRules.Merged applied, BasicMachineSpec base, BasicMachineSpec tuned) {
        var t = applied.tuning();
        logField(applied, "parallelLimit", t.parallelLimit(), base.parallelLimit(), tuned.parallelLimit());
        logField(applied, "energyIn", t.energyIn(), base.energyIn(), tuned.energyIn());
        logField(applied, "energyInMin", t.energyInMin(), base.energyInMin(), tuned.energyInMin());
        logField(applied, "energyInMax", t.energyInMax(), base.energyInMax(), tuned.energyInMax());
        logField(
                applied,
                "processingEfficiency",
                t.processingEfficiency(),
                base.processingEfficiency(),
                tuned.processingEfficiency());
        logField(applied, "constantEnergy", t.constantEnergy(), base.constantEnergy(), tuned.constantEnergy());
        logField(
                applied,
                "cheapOverclocking",
                t.cheapOverclocking(),
                base.cheapOverclocking(),
                tuned.cheapOverclocking());
    }

    private static void logField(
            MachineTuningRules.Merged applied, String field, Object requested, Object oldValue, Object newValue) {
        if (requested == null) return;
        LOGGER.info(
                "[gt6ce] tuning: {} {}/T{} {}: {} -> {}",
                applied.owners().get(field),
                applied.tuning().machineName(),
                applied.tuning().tier(),
                field,
                oldValue,
                newValue);
    }
}
