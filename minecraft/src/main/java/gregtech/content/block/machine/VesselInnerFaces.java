package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Inner-wall clicks shared by the open-top crucible and mold basin.
 * <p>
 * Both single blocks expose their cavity as the interaction shape, like a vanilla cauldron, so a
 * ray entering through the mouth reports {@link Direction#UP} even when it lands on an inner wall.
 * This predicate covers the remaining horizontal wall hits and the large crucible's wall parts.
 */
public final class VesselInnerFaces {
    /** Wall faces sit on exact 1/16 planes; only floating-point noise needs absorbing. */
    private static final double PLANE_EPSILON = 1.0E-4;

    private VesselInnerFaces() {}

    /** True when the hit landed on a side wall facing into the single-block vessel cavity. */
    public static boolean isInnerFace(BlockState state, BlockHitResult hit) {
        if (state.getBlock() instanceof SmeltingCrucibleBlock crucible) {
            return onInnerWall(crucible.cavityShape(), hit);
        }
        if (state.getBlock() instanceof MoldBasinBlock basin) {
            return onInnerWall(basin.cavityShape(), hit);
        }
        return false;
    }

    /** A large-crucible wall part opens into the hollow column above its controller. */
    public static boolean isLargeCrucibleInnerFace(BlockEntity clicked, BlockHitResult hit) {
        if (!(clicked instanceof MultiblockPortBlockEntity port)) return false;
        BlockPos controller = port.crucibleControllerPos();
        if (controller == null) return false;
        Direction face = hit.getDirection();
        if (!face.getAxis().isHorizontal()) return false;
        BlockPos inner = hit.getBlockPos().relative(face).subtract(controller);
        return inner.getX() == 0 && inner.getZ() == 0 && inner.getY() > 0;
    }

    private static boolean onInnerWall(VoxelShape cavity, BlockHitResult hit) {
        Direction face = hit.getDirection();
        if (!face.getAxis().isHorizontal()) return false;
        AABB box = cavity.bounds();
        BlockPos pos = hit.getBlockPos();
        double x = hit.getLocation().x - pos.getX();
        double y = hit.getLocation().y - pos.getY();
        double z = hit.getLocation().z - pos.getZ();
        if (y < box.minY - PLANE_EPSILON || y > box.maxY + PLANE_EPSILON) return false;
        return switch (face) {
            case EAST -> near(x, box.minX) && between(z, box.minZ, box.maxZ);
            case WEST -> near(x, box.maxX) && between(z, box.minZ, box.maxZ);
            case SOUTH -> near(z, box.minZ) && between(x, box.minX, box.maxX);
            case NORTH -> near(z, box.maxZ) && between(x, box.minX, box.maxX);
            default -> false;
        };
    }

    private static boolean near(double value, double plane) {
        return Math.abs(value - plane) <= PLANE_EPSILON;
    }

    private static boolean between(double value, double min, double max) {
        return value >= min - PLANE_EPSILON && value <= max + PLANE_EPSILON;
    }
}
