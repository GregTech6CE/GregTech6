package gregtech.gametest;

import gregtech.api.addon.material.AddonMaterialContributor;
import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialDeclaration;
import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.material.GTValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Test-source SPI contributor exercises host installation without a real optional mod. */
public final class MaterialBaselineAddon implements AddonMaterialContributor {
    record DecompositionCase(String material, String form, String fluid, boolean gas) {}

    static final List<DecompositionCase> DECOMPOSITIONS = List.of(
            new DecompositionCase("BaselineDustOnly", "dust", null, false),
            new DecompositionCase("BaselineBlockOnly", "blockDust", null, false),
            new DecompositionCase("BaselineLiquidOnly", "liquid", "material_baseline_test:baselineliquidonly", false),
            new DecompositionCase("BaselineGasOnly", "gas", "material_baseline_test:baselinegasonly", true),
            new DecompositionCase("BaselineLegacyFluid", "liquid", "baselinelegacyfluid", false),
            new DecompositionCase("BaselineBuiltinFluid", "gas", "gt6ce:oxygen", true),
            new DecompositionCase("BaselineExternalFluid", "liquid", "minecraft:flowing_water", false),
            new DecompositionCase("BaselineUnknownFluid", "liquid", "missing:baselineliquidonly", false),
            new DecompositionCase("BaselineInvalidFluid", "liquid", "Invalid:fluid", false));

    @Override
    public String addonId() {
        return "material_baseline_test";
    }

    @Override
    public List<MaterialDeclaration> materials() {
        var declarations = new ArrayList<>(List.of(
                MaterialDeclaration.builder("BaselineRuntimeCompound").dust().build(),
                MaterialDeclaration.builder("BaselineThermalAddon")
                        .dust()
                        .liquid()
                        .build()));
        for (var test : DECOMPOSITIONS) {
            var material = MaterialDeclaration.builder(test.material()).dust();
            if (test.gas()) material.gas();
            else material.liquid();
            if (test.material().equals("BaselineLiquidOnly")) material.meltingPointKelvin(200);
            declarations.add(material.build());
        }
        declarations.add(
                MaterialDeclaration.builder("BaselineFluidOutput").dust().build());
        return List.copyOf(declarations);
    }

    @Override
    public void materialInfo(MaterialInfoTool.Registrar registrar) {
        registrar.registerProcess(
                new MaterialKey("BaselineRuntimeCompound"),
                MaterialProcessKeys.DECOMPOSITION,
                MaterialProcessRule.anyForm(new MaterialDecompositionRule(
                        Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), List.of(), false)));
        for (var test : DECOMPOSITIONS) {
            var fluids = test.fluid() == null
                    ? List.<MaterialDecompositionRule.FluidBinding>of()
                    : List.of(new MaterialDecompositionRule.FluidBinding(
                            test.gas() ? MaterialDecompositionRule.Phase.GAS : MaterialDecompositionRule.Phase.LIQUID,
                            test.fluid(),
                            GTValues.U,
                            1000));
            registrar.registerProcess(
                    new MaterialKey(test.material()),
                    MaterialProcessKeys.DECOMPOSITION,
                    new MaterialProcessRule<>(
                            Set.of(new FormKey(test.form())),
                            new MaterialDecompositionRule(
                                    Set.of(MaterialDecompositionRule.Route.CENTRIFUGE),
                                    fluids,
                                    !test.form().equals("dust") && !test.form().equals("blockDust"))));
        }
        registrar.registerProcess(
                new MaterialKey("BaselineFluidOutput"),
                MaterialProcessKeys.DECOMPOSITION,
                MaterialProcessRule.anyForm(new MaterialDecompositionRule(
                        Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), List.of(), false)));
    }

    @Override
    public List<MaterialComposition> compositions() {
        var compositions = new ArrayList<>(List.of(new MaterialComposition(
                "BaselineRuntimeCompound",
                3,
                List.of(
                        new MaterialComposition.Part("Iron", GTValues.U * 2),
                        new MaterialComposition.Part("Copper", GTValues.U)),
                false)));
        for (var test : DECOMPOSITIONS)
            compositions.add(new MaterialComposition(
                    test.material(), 9, List.of(new MaterialComposition.Part("Iron", GTValues.U * 9)), false));
        compositions.add(new MaterialComposition(
                "BaselineFluidOutput",
                2,
                List.of(
                        new MaterialComposition.Part("Iron", GTValues.U),
                        new MaterialComposition.Part("BaselineLiquidOnly", GTValues.U)),
                false));
        return List.copyOf(compositions);
    }
}
