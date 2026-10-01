package com.gregtech.gregtech.block;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialChemistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.generated.GT6Materials;

import java.util.ArrayList;
import java.util.List;

/** GT6 block material amounts (crates include +1× wood hull). */
public final class BlockMaterialWeights {
    private BlockMaterialWeights() {}

    public static List<MaterialChemistry.WeightedMaterial> contained(GTMaterial material, BlockMaterialPrefix prefix) {
        List<MaterialChemistry.WeightedMaterial> out = new ArrayList<>(
                MaterialChemistry.materialWeights(material, prefix.getMaterialWeight()));
        if (prefix.isCrate()) {
            out.add(new MaterialChemistry.WeightedMaterial(com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood, GTValues.U));
        }
        return List.copyOf(out);
    }
}
