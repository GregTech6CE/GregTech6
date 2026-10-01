package com.gregtech.gregtech.content.book;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** GT6 MultiTileEntityBookShelf: two faces, each with two rows of seven books. */
public final class BookShelfGeometry {
    private BookShelfGeometry() {}

    public static int slotAt(Direction facing, Direction side, double x, double y, double z) {
        if (side != facing && side != facing.getOpposite()) return -1;
        double horizontal = switch (side) {
            case NORTH -> 1 - x;
            case SOUTH -> x;
            case WEST -> z;
            case EAST -> 1 - z;
            default -> -1;
        };
        double vertical = 1 - y; // GT6 facing coordinates have their origin at the top left.
        if (horizontal < 1.0 / 16 || horizontal > 15.0 / 16
                || vertical < 1.0 / 16 || vertical > 15.0 / 16) return -1;
        return GTBookList.slotFor(side == facing, horizontal, vertical);
    }

    /** Model-space book bounds for a north-facing shelf, directly from the GT6 render passes. */
    public static AABB bookBounds(int slot) {
        var b=BookShelfRules.bookBounds(slot);
        return new AABB(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1());
    }
}
