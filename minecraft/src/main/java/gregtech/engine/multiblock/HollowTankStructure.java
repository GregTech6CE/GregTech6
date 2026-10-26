package gregtech.engine.multiblock;

import gregtech.api.multiblock.StructureGrid;
import net.minecraft.core.*;
import java.util.function.Predicate;

/** Adapter for hollow-cube traversal; loading, wall and air policies belong to the caller. */
public final class HollowTankStructure {

    private HollowTankStructure() {
    }

    public static boolean validate(BlockPos controller, Direction facing, int size, Predicate<BlockPos> loaded, Predicate<BlockPos> wall,
            Predicate<BlockPos> air) {
        return StructureGrid.hollowTank(controller.getX(), controller.getY(), controller.getZ(), facing.getStepX(), facing.getStepY(),
                facing.getStepZ(), size,
                position -> loaded.test(new BlockPos(position.x(), position.y(), position.z())),
                position -> wall.test(new BlockPos(position.x(), position.y(), position.z())),
                position -> air.test(new BlockPos(position.x(), position.y(), position.z())));
    }
}
