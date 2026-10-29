package gregtech.content.block;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.engine.material.MaterialChemistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.ArrayList;
import java.util.List;

/** GT6 block material amounts (crates include +1× wood hull). */
public final class BlockMaterialWeights {
    private BlockMaterialWeights() {}

    public static List<MaterialComponent> contained(GTMaterial material, BlockMaterialPrefix prefix) {
        List<MaterialComponent> out =
                new ArrayList<>(MaterialChemistry.materialWeights(material, prefix.getMaterialWeight()));
        if (prefix.isCrate()) {
            out.add(new MaterialComponent(gregtech.content.material.generated.WoodMaterials.Wood, GTValues.U));
        }
        return List.copyOf(out);
    }
}
