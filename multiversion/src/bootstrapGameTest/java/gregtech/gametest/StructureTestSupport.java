package gregtech.gametest;

import gregtech.internal.multiblock.tracking.StructureScanProbe;
import gregtech.internal.multiblock.tracking.StructureScheduler;
import gregtech.internal.multiblock.tracking.StructureTracking;
import gregtech.internal.multiblock.tracking.TrackedMultiblock;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Same-tick structure assertions scan once, then read the tracker. A scan already on the stack stays closed. */
public final class StructureTestSupport {
    static {
        // Test builds compare every incremental verdict with the phase-1 full scan.
        StructureTracking.consistencyCheck(true);
    }

    private StructureTestSupport() {}

    public static boolean ok(BlockEntity entity) {
        if (!(entity instanceof TrackedMultiblock machine)) return false;
        if (StructureScanProbe.inScan()) return StructureTracking.query(machine);
        StructureTracking.attach(machine);
        if (entity.getLevel() != null) StructureScheduler.flush(entity.getLevel());
        return StructureTracking.query(machine);
    }
}
