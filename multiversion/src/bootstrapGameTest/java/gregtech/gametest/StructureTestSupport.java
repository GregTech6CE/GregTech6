package gregtech.gametest;

import gregtech.api.multiblock.tracking.StructureScanProbe;
import gregtech.api.multiblock.tracking.StructureScheduler;
import gregtech.api.multiblock.tracking.StructureTracking;
import gregtech.api.multiblock.tracking.TrackedMultiblock;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Same-tick structure assertions scan once, then read the tracker. A scan already on the stack stays closed. */
public final class StructureTestSupport {
    private StructureTestSupport() {}

    public static boolean ok(BlockEntity entity) {
        if (!(entity instanceof TrackedMultiblock machine)) return false;
        if (StructureScanProbe.inScan()) return StructureTracking.query(machine);
        StructureTracking.attach(machine);
        if (entity.getLevel() != null) StructureScheduler.flush(entity.getLevel());
        return StructureTracking.query(machine);
    }
}
