package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.CompoundMaterials;

/** The first real smeltery tiers, shared by both platform registries.
 * Values retain saltnya GTMachines/MachineRegistry's exact hull parameters.
 */
public final class InitialSmelteryDefinitions {
    private InitialSmelteryDefinitions() {}

    public static MachineSpec brickBurningBox() {
        var material = CompoundMaterials.ClayBrick;
        return new MachineSpec("burning_box_solid_brick", material.getLocalName(), material.getColor(),
                2500, 16, "burning_solid", 6.0F, 6.0F);
    }

    public static CrucibleSpec ceramicCrucible() {
        return CrucibleSpec.of("smelting_crucible_ceramic", Materials.Ceramic, 1005,
                5.0F, 5.0F, false, 2000, 4000, 0.8181818181818182D);
    }

    public static CrucibleSpec ceramicMold() {
        CrucibleSpec base = ceramicCrucible();
        return new CrucibleSpec("mold_ceramic", base.material(), base.gt6MetaId() + 50,
                base.meltingPointK(), base.boilingPointK(), base.hullDensity(), base.hardness(),
                base.blastResistance(), base.acidProof(), CrucibleSpec.MOLD_HULL_UNITS);
    }
}
