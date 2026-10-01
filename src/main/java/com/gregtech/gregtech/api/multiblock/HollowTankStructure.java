package com.gregtech.gregtech.api.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.function.Predicate;

/** Exact hollow cube around a controller in the centre of its front wall. */
public final class HollowTankStructure {
    private HollowTankStructure() {}
    public static boolean validate(BlockPos controller, Direction facing, int size,
            Predicate<BlockPos> loaded, Predicate<BlockPos> wall, Predicate<BlockPos> air) {
        if (size != 3 && size != 5) throw new IllegalArgumentException("Tank size: " + size);
        int radius = size / 2;
        BlockPos center = controller.relative(facing.getOpposite(), radius);
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
            BlockPos pos = center.offset(x, y, z);
            if (!loaded.test(pos)) return false;
            if (pos.equals(controller)) continue;
            boolean shell = Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius;
            if (!(shell ? wall.test(pos) : air.test(pos))) return false;
        }
        return true;
    }
}
