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
        if (slot < 0 || slot >= GTBookList.SLOTS) throw new IllegalArgumentException("book slot " + slot);
        int column = slot % 7;
        boolean front = slot < 14;
        int x = front ? 1 + column * 2 : 13 - column * 2;
        int y = slot % 14 < 7 ? 9 : 1;
        int z = front ? 2 : 9;
        return new AABB(x / 16.0, y / 16.0, z / 16.0, (x + 2) / 16.0, (y + 6) / 16.0, (z + 5) / 16.0);
    }
}
