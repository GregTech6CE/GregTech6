/* Adapted from GregTech-6 Team ICover.BOXES_COVERS / BOXES_HOLDERS, LGPL-3.0-or-later. */
package gregtech.content.cover;

/** Original attachment bounds, inside the host block. Side order: down, up, north, south, west, east. */
public final class CoverPlateGeometry {
    private CoverPlateGeometry() {}
    public static final double THICKNESS = 2.0 / 16;
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}
    public static Box plate(int side) { return bounds(side, 0, 1, THICKNESS); }
    public static Box holder(int side) { return bounds(side, 7.0 / 16, 9.0 / 16, .5); }
    /** Original GT6 {@code CoverPressureValve.BOXES_VALVES}: a 4x4 px knob recessed 8 px into the face. */
    public static Box valve(int side) { return bounds(side, 6.0 / 16, 10.0 / 16, .5); }
    /** Physical bounds shared by the collision and selection paths. */
    public static Box attachment(String behavior, int side) {
        if ("pressure_value".equals(behavior)) return valve(side);
        if ("cover_redstone_torch".equals(behavior) || "cover_redstone_repeater".equals(behavior))
            return holder(side);
        return plate(side);
    }
    private static Box bounds(int side, double min, double max, double depth) {
        return switch (side) {
            case 0 -> new Box(min, 0, min, max, depth, max);
            case 1 -> new Box(min, 1-depth, min, max, 1, max);
            case 2 -> new Box(min, min, 0, max, max, depth);
            case 3 -> new Box(min, min, 1-depth, max, max, 1);
            case 4 -> new Box(0, min, min, depth, max, max);
            case 5 -> new Box(1-depth, min, min, 1, max, max);
            default -> throw new IllegalArgumentException("Invalid cover side: " + side);
        };
    }
}
