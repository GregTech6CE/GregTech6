package gregtech.content.block;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.engine.material.MaterialChemistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.List;

/** GT6 block material amounts. */
public final class BlockMaterialWeights {
    private BlockMaterialWeights() {}

    public static List<MaterialComponent> contained(GTMaterial material, BlockMaterialPrefix prefix) {
        return List.copyOf(MaterialChemistry.materialWeights(material, prefix.getMaterialWeight()));
    }
}
