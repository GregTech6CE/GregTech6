package gregtech.gametest;

import gregtech.api.addon.material.AddonMaterialContributor;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialDeclaration;
import gregtech.api.material.GTValues;
import java.util.List;

/** Test-source SPI contributor exercises host installation without a real optional mod. */
public final class MaterialBaselineAddon implements AddonMaterialContributor {
    @Override
    public String addonId() {
        return "material_baseline_test";
    }

    @Override
    public List<MaterialDeclaration> materials() {
        return List.of(
                MaterialDeclaration.builder("BaselineRuntimeCompound")
                        .dust()
                        .centrifuge()
                        .build(),
                MaterialDeclaration.builder("BaselineThermalAddon")
                        .dust()
                        .liquid()
                        .build());
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
