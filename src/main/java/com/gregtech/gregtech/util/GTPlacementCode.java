package com.gregtech.gregtech.util;

import net.minecraft.core.Direction;

/** GT6 {@code UT.Code.getSideWrenching}  - 3×3 face subdivision for oriented slab placement. */
public final class GTPlacementCode {
    private GTPlacementCode() {}

    /**
     * Resolves which half of a block a GT slab should occupy from the clicked face and hit position.
     * Hit coordinates must be in block-local space ({@code 0..1}).
     */
    public static Direction resolveSlabFace(Direction clickedFace, float hitX, float hitY, float hitZ) {
        Direction side = subdivideFace(clickedFace, hitX, hitY, hitZ);
        if (side == clickedFace || side == clickedFace.getOpposite()) {
            side = side.getOpposite();
        }
        return side;
    }

    /** GT6 {@code UT.Code.getSideWrenching} — raw 3×3 grid (machines / wrench; no slab invert). */
    public static Direction getSideWrenching(Direction face, float hitX, float hitY, float hitZ) {
        return subdivideFace(face, hitX, hitY, hitZ);
    }

    /** Raw 3×3 grid index without opposite-face adjustment. */
    public static Direction subdivideFace(Direction face, float hitX, float hitY, float hitZ) {
        return switch (face) {
            case DOWN, UP -> {
                if (hitX < 0.25F) {
                    yield hitZ < 0.25F || hitZ > 0.75F ? face.getOpposite() : Direction.WEST;
                }
                if (hitX > 0.75F) {
                    yield hitZ < 0.25F || hitZ > 0.75F ? face.getOpposite() : Direction.EAST;
                }
                if (hitZ < 0.25F) {
                    yield Direction.NORTH;
                }
                if (hitZ > 0.75F) {
                    yield Direction.SOUTH;
                }
                yield face;
            }
            case NORTH, SOUTH -> {
                if (hitX < 0.25F) {
                    yield hitY < 0.25F || hitY > 0.75F ? face.getOpposite() : Direction.WEST;
                }
                if (hitX > 0.75F) {
                    yield hitY < 0.25F || hitY > 0.75F ? face.getOpposite() : Direction.EAST;
                }
                if (hitY < 0.25F) {
                    yield Direction.DOWN;
                }
                if (hitY > 0.75F) {
                    yield Direction.UP;
                }
                yield face;
            }
            case WEST, EAST -> {
                if (hitZ < 0.25F) {
                    yield hitY < 0.25F || hitY > 0.75F ? face.getOpposite() : Direction.NORTH;
                }
                if (hitZ > 0.75F) {
                    yield hitY < 0.25F || hitY > 0.75F ? face.getOpposite() : Direction.SOUTH;
                }
                if (hitY < 0.25F) {
                    yield Direction.DOWN;
                }
                if (hitY > 0.75F) {
                    yield Direction.UP;
                }
                yield face;
            }
        };
    }
}
