package gregtech.engine.addon;

import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.EnergyRange;
import gregtech.content.data.FuelRecipeMaps;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.machine.BasicMachineCatalog;
import gregtech.content.multiblock.SharedLargeMachineLayouts;
import gregtech.content.recipe.RecipeMapSpec;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Turns the core machine queue into platform recipe maps and specs. Idempotent. */
public final class AddonMachineRuntime {
    private static final List<BasicMachineSpec> SPECS = new ArrayList<>();
    private static boolean installed;

    private AddonMachineRuntime() {}

    public static void install() {
        if (installed) return;
        BehaviorMachineHosts.collect();
        LayoutPartHosts.collect();
        installed = true;
        var reserved = new HashSet<>(BasicMachineCatalog.machineTypeNames());
        reserved.addAll(SharedLargeMachineLayouts.machines());
        AddonMachines.install(reserved, AddonMachines.builtInRecipeMapNames());
        MachineRecipeMaps.bootstrap();
        for (AddonMachines.AcceptedRecipeMap accepted : AddonMachines.recipeMaps()) create(accepted.spec());
        for (AddonMachines.AcceptedRecipeMap accepted : AddonMachines.fuelMaps())
            FuelRecipeMaps.registerFuelMap(create(accepted.spec()));
        for (AddonMachines.AcceptedMachine accepted : AddonMachines.machines()) {
            BasicMachineSpec spec = toSpec(accepted);
            SPECS.add(spec);
            RegisteredMachineIndex.bind(spec);
        }
    }

    public static List<BasicMachineSpec> specs() {
        return List.copyOf(SPECS);
    }

    public static RecipeMap create(RecipeMapSpec spec) {
        var map = new RecipeMap(
                null,
                spec.mNameInternal,
                spec.mNameLocal,
                spec.mGUIPath,
                spec.mInputItemsCount,
                spec.mOutputItemsCount,
                spec.mMinimalInputItems,
                spec.mInputFluidCount,
                spec.mOutputFluidCount,
                spec.mMinimalInputFluids,
                spec.mMinimalInputs,
                spec.mNeedsOutputs,
                spec.mCombinePower,
                spec.mUseBucketSizeIn,
                spec.mUseBucketSizeOut);
        map.specialValueLabel(spec.mSpecialValuePre, spec.mSpecialValueMultiplier, spec.mSpecialValuePost);
        map.viewer(spec.mViewerAllowed, spec.mShowVoltageAmperage);
        if (spec.mInstantRecipes) map.instantRecipes();
        return map;
    }

    private static BasicMachineSpec toSpec(AddonMachines.AcceptedMachine accepted) {
        var definition = accepted.definition();
        var parameters = definition.parameters();
        RecipeMap recipes = RecipeMap.RECIPE_MAPS.get(definition.recipeMapName());
        if (recipes == null)
            throw new IllegalStateException("Addon '" + accepted.addonId() + "' machine '" + parameters.machineName()
                    + "' uses unknown recipe map '" + definition.recipeMapName() + "'");
        var spec = BasicMachineSpec.builder(parameters.id(), parameters.material())
                .machineType(parameters.machineName())
                .recipes(recipes)
                .energy(
                        AddonPlatformServices.tag(parameters.energy().type().name()),
                        parameters.energy().energyIn())
                .energyOutput(parameters.energy().energyOut())
                .tier(parameters.energy().tier())
                .parallel(parameters.options().parallelLimit())
                .strength(parameters.hardness(), parameters.blastResistance())
                .faces(FaceConfig.from(parameters.faceConfig()))
                .constructionMaterials(parameters.constructionMaterials())
                .namespace(accepted.addonId())
                .controllerKind(definition.kind())
                .processingEfficiency(definition.efficiency())
                .constantEnergy(definition.constantEnergy())
                .structureKeys(definition.structureKeys())
                .minimumLightAbove(parameters.options().minimumLightAbove())
                .searchOutputTanks(parameters.options().searchOutputTanks())
                .exclusiveOutputs(parameters.options().exclusiveOutputs())
                .itemOutputTarget(parameters.io().itemOutputTarget())
                .fluidOutputTarget(parameters.io().fluidOutputTarget())
                .itemInputSource(parameters.io().itemInputSource())
                .fluidInputSource(parameters.io().fluidInputSource());
        if (parameters.energy().range() instanceof EnergyRange.Declared)
            spec.energyRange(
                    parameters.energy().energyInMin(), parameters.energy().energyInMax());
        else if (definition.kind() == gregtech.api.machine.MachineControllerKind.LARGE_RECIPE)
            spec.implicitEnergyRange(
                    parameters.energy().acceptedEnergyInMin(true),
                    parameters.energy().acceptedEnergyInMax(true));
        else if (parameters.energy().energyInMin() > 0)
            spec.implicitEnergyRange(
                    parameters.energy().energyInMin(), parameters.energy().energyInMax());
        if (definition.kind() == gregtech.api.machine.MachineControllerKind.BASIC
                || parameters.options().cheapOverclocking()
                        != BasicMachineParameters.Options.CheapOverclocking.UNSPECIFIED)
            spec.cheapOverclocking(parameters.options().cheapOverclocking()
                    == BasicMachineParameters.Options.CheapOverclocking.ENABLED);
        if (definition.auxiliaryEnergyType() != null) {
            var tag = gregtech.content.data.GregTechTags.Energy.ALL.stream()
                    .filter(candidate -> candidate.getShortName().equalsIgnoreCase(definition.auxiliaryEnergyType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Addon '" + accepted.addonId()
                            + "' machine '" + parameters.machineName() + "' uses unknown auxiliary energy type '"
                            + definition.auxiliaryEnergyType() + "'"));
            spec.auxiliaryEnergy(
                    tag,
                    definition.auxiliaryEnergyIn(),
                    definition.auxiliaryEnergyMin(),
                    definition.auxiliaryEnergyMax(),
                    definition.auxiliaryEnergyCapacity(),
                    definition.auxiliaryFaceMask());
            if (definition.startupEnergy()) spec.startupEnergy(true);
        }
        return spec.build();
    }
}
