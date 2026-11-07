package gregtech.content.material;

import gregtech.api.addon.material.AddonMaterialContributor;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialDeclaration;
import gregtech.api.material.GTValues;
import java.util.List;

/** Loaded only by the isolated SPI test, never by the ordinary material fixture or a release jar. */
public final class MaterialBaselineTestAddon implements AddonMaterialContributor {
    @Override
    public String addonId() {
        return "material_baseline_test";
    }

    @Override
    public List<MaterialDeclaration> materials() {
        return List.of(MaterialDeclaration.builder("BaselineCompound")
                .dust()
                .centrifuge()
                .build());
    }

    @Override
    public List<MaterialComposition> compositions() {
        return List.of(new MaterialComposition(
                "BaselineCompound",
                3,
                List.of(
                        new MaterialComposition.Part("Iron", GTValues.U * 2),
                        new MaterialComposition.Part("Copper", GTValues.U)),
                false));
    }
}
