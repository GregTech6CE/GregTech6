package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.CompoundMaterials;

/** The first real smeltery tiers, shared by both platform registries.
 * Hull definitions use original Loader_MultiTileEntities material statistics and explicit ceramic data.
 */
public final class InitialSmelteryDefinitions {
    private InitialSmelteryDefinitions() {}

    public static MachineSpec brickBurningBox() {
        var material = CompoundMaterials.ClayBrick;
        return new MachineSpec("burning_box_solid_brick", material.getLocalName(), material.getColor(),
                2500, 16, "burning_solid", 6.0F, 6.0F);
    }

    public static CrucibleSpec ceramicCrucible() {
        return OriginalSmelteryDefinitions.get(1005);
    }

    public static CrucibleSpec ceramicMold() {
        return OriginalSmelteryDefinitions.get(1055);
    }
}
