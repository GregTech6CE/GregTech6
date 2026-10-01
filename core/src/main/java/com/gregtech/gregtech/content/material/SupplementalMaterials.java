package com.gregtech.gregtech.content.material;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.material.generated.CompoundMaterials;
import com.gregtech.gregtech.content.material.generated.OreMaterials;

/** Original materials missed by the first import; kept outside generated catalogs. */
public final class SupplementalMaterials {
    private static GTMaterial porcelain;
    private SupplementalMaterials() {}
    public static void declare() {
        MaterialDefinition.builder(9223, "Blackstone").color(0x1E1414)
                .texture(MaterialTextureSet.FINE).dust().build().register();
        var ceramic = CompoundMaterials.Ceramic;
        var silica = OreMaterials.SiliconDioxide;
        var feldspar = OreMaterials.PotassiumFeldspar;
        porcelain = MaterialDefinition.builder(8273, "Porcelain").color(0xEBEBF5)
                .texture(MaterialTextureSet.FINE).meltingPointKelvin(1800).boilingPointKelvin(3600)
                .density((ceramic.getDensity() * 2 + silica.getDensity() + feldspar.getDensity()) / 4)
                .dust().properties(MaterialProperty.GENERATE_PLATE).build().register();
        porcelain.setComposition(4, MaterialComponent.of(ceramic, 2 * GTValues.U),
                MaterialComponent.of(silica, GTValues.U), MaterialComponent.of(feldspar, GTValues.U));
    }
    public static GTMaterial porcelain() { GTMaterialRegistry.init(); return porcelain; }
}
