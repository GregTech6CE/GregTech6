package gregtech.engine.addon;

import gregtech.api.machine.EnergyRange;
import gregtech.content.data.FuelRecipeMaps;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.machine.BasicMachineCatalog;
import gregtech.content.multiblock.SharedLargeMachineLayouts;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.machine.BasicMachineParameters.Options.CheapOverclocking;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.machine.MachineControllerKind;
import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jetbrains.annotations.Nullable;

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
        for (AddonMachines.AcceptedRecipeMap accepted : AddonMachines.recipeMaps()) new RecipeMap(accepted.spec());
        for (AddonMachines.AcceptedRecipeMap accepted : AddonMachines.fuelMaps())
            FuelRecipeMaps.registerFuelMap(new RecipeMap(accepted.spec()));
        for (AddonMachines.AcceptedMachine accepted : AddonMachines.machines()) {
            BasicMachineSpec spec = toSpec(accepted);
            SPECS.add(spec);
            RegisteredMachineIndex.bind(spec);
        }
    }

    public static List<BasicMachineSpec> specs() {
        return List.copyOf(SPECS);
    }

    private static BasicMachineSpec toSpec(AddonMachines.AcceptedMachine accepted) {
        var definition = accepted.definition();
        var parameters = definition.parameters();
        RecipeMap recipes = RecipeMap.RECIPE_MAPS.get(definition.recipeMapName());
        if (recipes == null)
            throw new IllegalStateException("Addon '" + accepted.addonId() + "' machine '" + parameters.machineName()
                    + "' uses unknown recipe map '" + definition.recipeMapName() + "'");
        var profile = parameters.energy();
        String energyType = AddonPlatformServices.tag(profile.type().name()).getShortName();
        boolean large = definition.kind() == MachineControllerKind.LARGE_RECIPE;
        var range = new BasicMachineSpec.InputRange(
                profile.acceptedEnergyInMin(large),
                profile.acceptedEnergyInMax(large),
                profile.range() instanceof EnergyRange.Declared);
        var energy =
                new BasicMachineSpec.Energy(energyType, profile.tier(), profile.energyIn(), profile.energyOut(), range);

        var options = parameters.options();
        var cheap = options.cheapOverclocking();
        boolean timed = "TU".equals(energyType);
        boolean cheapOverclocking =
                cheap == CheapOverclocking.ENABLED || (cheap == CheapOverclocking.UNSPECIFIED && large && !timed);
        var processing = new BasicMachineSpec.Processing(
                options.parallelLimit(),
                definition.efficiency(),
                definition.constantEnergy(),
                cheapOverclocking,
                !large || cheap != CheapOverclocking.UNSPECIFIED,
                !large || !timed,
                options.minimumLightAbove(),
                options.searchOutputTanks(),
                options.exclusiveOutputs());
        var layout = new BasicMachineSpec.Layout(
                definition.kind(),
                FaceConfig.from(parameters.faceConfig()),
                definition.structureKeys(),
                parameters.io(),
                List.of(),
                false,
                false);
        return new BasicMachineSpec(
                parameters.id(),
                accepted.addonId(),
                parameters.machineName(),
                BasicMachineSpec.Casing.from(parameters),
                recipes,
                energy,
                processing,
                layout,
                auxiliaryEnergy(accepted));
    }

    private static BasicMachineSpec.@Nullable AuxiliaryEnergy auxiliaryEnergy(AddonMachines.AcceptedMachine accepted) {
        var definition = accepted.definition();
        String type = definition.auxiliaryEnergyType();
        if (type == null) return null;
        var tag = GregTechTags.Energy.ALL.stream()
                .filter(candidate -> candidate.getShortName().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Addon '" + accepted.addonId()
                        + "' machine '" + definition.parameters().machineName()
                        + "' uses unknown auxiliary energy type '"
                        + type + "'"));
        return new BasicMachineSpec.AuxiliaryEnergy(
                tag,
                definition.auxiliaryEnergyIn(),
                definition.auxiliaryEnergyMin(),
                definition.auxiliaryEnergyMax(),
                definition.auxiliaryEnergyCapacity(),
                definition.auxiliaryFaceMask(),
                definition.startupEnergy());
    }
}
