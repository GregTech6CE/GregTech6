package gregtech.content.block.stone;

import gregtech.api.material.WeightedMaterial;

import gregtech.content.material.Materials;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.engine.material.MaterialChemistry;
import gregtech.content.data.generated.GT6Materials;

import java.util.ArrayList;
import java.util.List;

/** GT6 stone block material amounts (full block = 9× rock, slab = 4.5×). */
public final class StoneMaterialWeights {
    private StoneMaterialWeights() {}

    public static List<WeightedMaterial> contained(GTMaterial stoneMaterial, StoneVariant variant, boolean slab) {
        long stoneAmount = slab ? GTValues.U * 9 / 2 : GTValues.U * 9;
        List<WeightedMaterial> out = new ArrayList<>(2);
        out.add(new WeightedMaterial(stoneMaterial, stoneAmount));
        if (variant == StoneVariant.BRICKS_REINFORCED) {
            out.add(new WeightedMaterial(gregtech.content.material.generated.ElementMaterials.Iron, GTValues.U / 2));
        } else if (variant == StoneVariant.BRICKS_REDSTONE) {
            out.add(new WeightedMaterial(gregtech.content.material.generated.CompoundMaterials.Redstone, GTValues.U));
        }
        return List.copyOf(out);
    }
}
