package com.gregtech.gregtech.block.stone;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialChemistry;
import com.gregtech.gregtech.data.generated.GT6Materials;

import java.util.ArrayList;
import java.util.List;

/** GT6 stone block material amounts (full block = 9× rock, slab = 4.5×). */
public final class StoneMaterialWeights {
    private StoneMaterialWeights() {}

    public static List<MaterialChemistry.WeightedMaterial> contained(GTMaterial stoneMaterial, StoneVariant variant, boolean slab) {
        long stoneAmount = slab ? GTValues.U * 9 / 2 : GTValues.U * 9;
        List<MaterialChemistry.WeightedMaterial> out = new ArrayList<>(2);
        out.add(new MaterialChemistry.WeightedMaterial(stoneMaterial, stoneAmount));
        if (variant == StoneVariant.BRICKS_REINFORCED) {
            out.add(new MaterialChemistry.WeightedMaterial(com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron, GTValues.U / 2));
        } else if (variant == StoneVariant.BRICKS_REDSTONE) {
            out.add(new MaterialChemistry.WeightedMaterial(com.gregtech.gregtech.content.material.generated.CompoundMaterials.Redstone, GTValues.U));
        }
        return List.copyOf(out);
    }
}
