package gregtech.content.block.stone;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import java.util.ArrayList;
import java.util.List;

/** GT6 stone block material amounts (full block = 9× rock, slab = 4.5×). */
public final class StoneMaterialWeights {
    private StoneMaterialWeights() {}

    public static List<MaterialComponent> contained(GTMaterial stoneMaterial, StoneVariant variant, boolean slab) {
        long stoneAmount = slab ? GTValues.U * 9 / 2 : GTValues.U * 9;
        List<MaterialComponent> out = new ArrayList<>(2);
        out.add(new MaterialComponent(stoneMaterial, stoneAmount));
        // The reinforcement is part of the block it belongs to, so a slab carries half of it.
        if (variant == StoneVariant.BRICKS_REINFORCED) {
            out.add(new MaterialComponent(
                    gregtech.content.material.generated.ElementMaterials.Iron, slab ? GTValues.U / 4 : GTValues.U / 2));
        } else if (variant == StoneVariant.BRICKS_REDSTONE) {
            out.add(new MaterialComponent(
                    gregtech.content.material.generated.CompoundMaterials.Redstone,
                    slab ? GTValues.U / 2 : GTValues.U));
        }
        return List.copyOf(out);
    }
}
