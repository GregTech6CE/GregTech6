package gregtech.block;

import gregtech.content.material.Materials;
import gregtech.internal.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.internal.material.MaterialChemistry;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.data.generated.GT6Materials;

import java.util.ArrayList;
import java.util.List;

/** GT6 block material amounts (crates include +1× wood hull). */
public final class BlockMaterialWeights {
    private BlockMaterialWeights() {}

    public static List<MaterialChemistry.WeightedMaterial> contained(GTMaterial material, BlockMaterialPrefix prefix) {
        List<MaterialChemistry.WeightedMaterial> out = new ArrayList<>(
                MaterialChemistry.materialWeights(material, prefix.getMaterialWeight()));
        if (prefix.isCrate()) {
            out.add(new MaterialChemistry.WeightedMaterial(gregtech.content.material.generated.WoodMaterials.Wood, GTValues.U));
        }
        return List.copyOf(out);
    }
}
