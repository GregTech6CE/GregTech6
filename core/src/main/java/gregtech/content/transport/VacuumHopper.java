package gregtech.content.transport;

import gregtech.content.material.generated.CompoundMaterials;
import gregtech.engine.machine.HopperSpec;

/** GT6CE-only hopper that vacuums dropped items from a 5x5x5 area instead of only the block above. */
public final class VacuumHopper {
    private VacuumHopper() {}

    public static final String ID = "hopper_vacuum";

    /** GT dye purple, so the shared hopper model reads as a purple hopper. */
    public static final int TINT = CompoundMaterials.Purple.getColor();

    public static final HopperSpec SPEC = HopperSpec.vacuum(ID, CompoundMaterials.Steel, 5, 6.0F, 6.0F, TINT);
}
