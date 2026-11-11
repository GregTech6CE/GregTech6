package gregtech.gametest;

import gregtech.api.addon.material.AddonMaterialContributor;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialDeclaration;
import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.material.GTValues;
import java.util.List;
import java.util.Set;

/** Test-source SPI contributor exercises host installation without a real optional mod. */
public final class MaterialBaselineAddon implements AddonMaterialContributor {
    @Override
    public String addonId() {
        return "material_baseline_test";
    }

    @Override
    public List<MaterialDeclaration> materials() {
        return List.of(
                MaterialDeclaration.builder("BaselineRuntimeCompound").dust().build(),
                MaterialDeclaration.builder("BaselineThermalAddon")
                        .dust()
                        .liquid()
                        .build());
    }

    @Override
    public void materialInfo(MaterialInfoTool.Registrar registrar) {
        registrar.registerProcess(
                new MaterialKey("BaselineRuntimeCompound"),
                MaterialProcessKeys.DECOMPOSITION,
                MaterialProcessRule.anyForm(new MaterialDecompositionRule(
                        Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), List.of(), false)));
    }

    @Override
    public List<MaterialComposition> compositions() {
        return List.of(new MaterialComposition(
                "BaselineRuntimeCompound",
                3,
                List.of(
                        new MaterialComposition.Part("Iron", GTValues.U * 2),
                        new MaterialComposition.Part("Copper", GTValues.U)),
                false));
    }
}
