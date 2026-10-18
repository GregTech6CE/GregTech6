package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
import com.gregtech.gregtech.content.machine.BasicMachineCatalog;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.data.MachineRecipeMaps;

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
        installed = true;
        var reserved = new HashSet<>(BasicMachineCatalog.machineTypeNames());
        reserved.addAll(SharedLargeMachineLayouts.machines());
        AddonMachines.install(reserved, AddonMachines.builtInRecipeMapNames());
        MachineRecipeMaps.bootstrap();
        for (AddonMachines.AcceptedRecipeMap accepted : AddonMachines.recipeMaps())
            create(accepted.spec());
        for (AddonMachines.AcceptedRecipeMap accepted : AddonMachines.fuelMaps())
            FuelRecipeMaps.registerFuelMap(create(accepted.spec()));
        for (AddonMachines.AcceptedMachine accepted : AddonMachines.machines()) {
            BasicMachineSpec spec = toSpec(accepted);
            SPECS.add(spec);
            RegisteredMachineIndex.bind(spec);
        }
    }

    public static List<BasicMachineSpec> specs() { return List.copyOf(SPECS); }

    public static RecipeMap create(RecipeMapSpec spec) {
        var map = new RecipeMap(null, spec.mNameInternal, spec.mNameLocal, spec.mGUIPath,
                spec.mInputItemsCount, spec.mOutputItemsCount, spec.mMinimalInputItems,
                spec.mInputFluidCount, spec.mOutputFluidCount, spec.mMinimalInputFluids,
                spec.mMinimalInputs, spec.mNeedsOutputs, spec.mCombinePower,
                spec.mUseBucketSizeIn, spec.mUseBucketSizeOut);
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
            throw new IllegalStateException("Addon '" + accepted.addonId() + "' machine '"
                    + parameters.machineName() + "' uses unknown recipe map '" + definition.recipeMapName() + "'");
        return BasicMachineSpec.builder(parameters.id(), parameters.material())
                .machineType(parameters.machineName())
                .recipes(recipes)
                .energy(parameters.energyTag(), parameters.energyIn())
                .energyOutput(parameters.energyOut())
                .energyRange(parameters.energyInMin(), parameters.energyInMax())
                .tier(parameters.tier())
                .parallel(parameters.parallelLimit())
                .strength(parameters.hardness(), parameters.blastResistance())
                .faces(FaceConfig.from(parameters.faceConfig()))
                .constructionMaterials(parameters.constructionMaterials())
                .namespace(accepted.addonId())
                .controllerKind(definition.kind())
                .processingEfficiency(definition.efficiency())
                .constantEnergy(definition.constantEnergy())
                .structureKeys(definition.structureKeys())
                .build();
    }
}
