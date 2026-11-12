package gregtech.content.material;

import gregtech.api.addon.material.*;
import gregtech.api.material.GTValues;
import java.util.List;
import java.util.Set;

/** Loaded only by the isolated SPI test, never by the ordinary material fixture or a release jar. */
public final class MaterialBaselineTestAddon implements AddonMaterialContributor {
    @Override
    public String addonId() {
        return "material_baseline_test";
    }

    @Override
    public List<MaterialDeclaration> materials() {
        return List.of(MaterialDeclaration.builder("BaselineCompound").dust().build());
    }

    @Override
    public void materialInfo(MaterialInfoTool.Registrar registrar) {
        registrar.registerProcess(
                new MaterialKey("BaselineCompound"),
                MaterialProcessKeys.DECOMPOSITION,
                MaterialProcessRule.anyForm(new MaterialDecompositionRule(
                        Set.of(MaterialDecompositionRule.Route.CENTRIFUGE), List.of(), false)));
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
