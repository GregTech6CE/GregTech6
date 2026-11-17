package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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
        return clicked instanceof MultiblockPortBlockEntity port && isLargeCrucibleInnerSide(port, hit.getDirection());
    }

    /**
     * True for a large-crucible wall face that opens into the two hollow layers, so cavity clicks and
     * covers are refused there. The outer shell, the base ring and the top face are not part of it;
     * {@link #isLargeCrucibleAllowedCoverSide} is the cover admission built on top of this.
     */
    public static boolean isLargeCrucibleInnerSide(@Nullable MultiblockPortBlockEntity port, @Nullable Direction face) {
        if (port == null || face == null || !face.getAxis().isHorizontal()) return false;
        BlockPos controller = port.crucibleControllerPos();
        if (controller == null) return false;
        BlockPos inner = port.getBlockPos().relative(face).subtract(controller);
        // Only the two hollow layers of the 3x3x3 vessel are cavity; the base ring at y=0 is not.
        return inner.getX() == 0 && inner.getZ() == 0 && inner.getY() >= 1 && inner.getY() <= 2;
    }

    /** True for any cover host of a large crucible: its controller or one of its bound wall parts. */
    public static boolean isLargeCrucibleHost(@Nullable BlockEntity owner) {
        return owner instanceof LargeCrucibleControllerBlockEntity
                || owner instanceof MultiblockPortBlockEntity port && port.crucibleControllerPos() != null;
    }

    /**
     * Cover admission for a large crucible. Only faces that leave the 3x3x3 shell cleanly accept a
     * cover: the bottom of a formed controller, the outward horizontal faces of the shell, and the
     * bottom of the base ring. Every top face, the hollow-facing walls and the seams between two
     * vessel blocks stay clear, so a cover can neither bridge the cavity nor sit in an internal seam.
     */
    public static boolean isLargeCrucibleAllowedCoverSide(@Nullable BlockEntity owner, @Nullable Direction side) {
        if (owner == null || side == null) return false;
        if (owner instanceof LargeCrucibleControllerBlockEntity controller)
            return controller.isStructureOk() && side == Direction.DOWN;
        if (!(owner instanceof MultiblockPortBlockEntity part)) return false;
        BlockPos controller = part.crucibleControllerPos();
        if (controller == null || !part.hasFormedController() || side == Direction.UP) return false;
        BlockPos outside = part.getBlockPos().relative(side).subtract(controller);
        // A neighbour that still lies in the shell's [-1..1]x[0..2]x[-1..1] band is another vessel
        // block, i.e. an internal seam, and only the base ring can leave that band through the floor.
        return outside.getX() < -1
                || outside.getX() > 1
                || outside.getY() < 0
                || outside.getZ() < -1
                || outside.getZ() > 1;
    }

    /**
     * True when a formed large crucible's mouth or hollow-facing wall is clicked, i.e. a face that
     * opens into its two hollow layers. Outer shell faces keep ordinary use and block placement.
     */
    public static boolean isFormedLargeCrucibleCavityTarget(
            BlockState state, @Nullable BlockEntity clicked, BlockHitResult hit) {
        if (!state.hasProperty(MultiblockPortBlock.CRUCIBLE_FORMED)
                || !state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED)) return false;
        if (state.getBlock() instanceof LargeCrucibleControllerBlock && hit.getDirection() == Direction.UP) return true;
        return isLargeCrucibleInnerFace(clicked, hit);
    }

    /** True when {@code pos} lies in the hollow column of a formed large crucible. */
    public static boolean isFormedLargeCrucibleCavity(BlockGetter level, BlockPos pos) {
        return isFormedCrucibleController(level, pos.below()) || isFormedCrucibleController(level, pos.below(2));
    }

    /** True when the face a click opens into is a hollow layer, e.g. the underside of a mouth cover. */
    public static boolean isFormedLargeCrucibleAdjacentCavity(BlockGetter level, BlockHitResult hit) {
        return isFormedLargeCrucibleCavity(level, hit.getBlockPos().relative(hit.getDirection()));
    }

    private static boolean isFormedCrucibleController(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof LargeCrucibleControllerBlock
                && state.hasProperty(MultiblockPortBlock.CRUCIBLE_FORMED)
                && state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED);
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
