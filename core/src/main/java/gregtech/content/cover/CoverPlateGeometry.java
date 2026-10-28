/* Adapted from GregTech-6 Team ICover.BOXES_COVERS / BOXES_HOLDERS, LGPL-3.0-or-later. */
package gregtech.content.cover;

/** Original attachment bounds, inside the host block. Side order: down, up, north, south, west, east. */
public final class CoverPlateGeometry {
    private CoverPlateGeometry() {}

    public static final double THICKNESS = 2.0 / 16;

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}

    public static Box plate(int side) {
        return bounds(side, 0, 1, THICKNESS);
    }

    public static Box holder(int side) {
        return bounds(side, 7.0 / 16, 9.0 / 16, .5);
    }
    /** Original GT6 {@code CoverPressureValve.BOXES_VALVES}: a 4x4 px knob recessed 8 px into the face. */
    public static Box valve(int side) {
        return bounds(side, 6.0 / 16, 10.0 / 16, .5);
    }
    /** Side-sprite coordinates run from the exposed cap toward the host interior. */
    public static UV valveSideUv(int side, int face, double x, double y, double z) {
        int axis = axis(side), faceAxis = axis(face);
        if (axis == faceAxis) throw new IllegalArgumentException("Valve side face must be lateral");
        int across = 3 - axis - faceAxis;
        Box box = valve(side);
        double coordinate = coordinate(axis, x, y, z);
        double minimum = minimum(axis, box), maximum = maximum(axis, box);
        double length = (side == 1 || side == 3 || side == 5)
                ? (coordinate - minimum) / (maximum - minimum)
                : (maximum - coordinate) / (maximum - minimum);
        double width =
                (coordinate(across, x, y, z) - minimum(across, box)) / (maximum(across, box) - minimum(across, box));
        return new UV((6 + 4 * width) / 16, (1 - length) / 2);
    }

    public record UV(double u, double v) {}

    private static int axis(int side) {
        return switch (side) {
            case 0, 1 -> 1; // Y
            case 2, 3 -> 2; // Z
            case 4, 5 -> 0; // X
            default -> throw new IllegalArgumentException("Invalid cover side: " + side);
        };
    }

    private static double coordinate(int axis, double x, double y, double z) {
        return axis == 0 ? x : axis == 1 ? y : z;
    }

    private static double minimum(int axis, Box b) {
        return axis == 0 ? b.minX() : axis == 1 ? b.minY() : b.minZ();
    }

    private static double maximum(int axis, Box b) {
        return axis == 0 ? b.maxX() : axis == 1 ? b.maxY() : b.maxZ();
    }
    /** Physical bounds shared by the collision and selection paths. */
    public static Box attachment(String behavior, int side) {
        if ("pressure_value".equals(behavior)) return valve(side);
        if ("cover_redstone_torch".equals(behavior) || "cover_redstone_repeater".equals(behavior)) return holder(side);
        return plate(side);
    }

    private static Box bounds(int side, double min, double max, double depth) {
        return switch (side) {
            case 0 -> new Box(min, 0, min, max, depth, max);
            case 1 -> new Box(min, 1 - depth, min, max, 1, max);
            case 2 -> new Box(min, min, 0, max, max, depth);
            case 3 -> new Box(min, min, 1 - depth, max, max, 1);
            case 4 -> new Box(0, min, min, depth, max, max);
            case 5 -> new Box(1 - depth, min, min, 1, max, max);
            default -> throw new IllegalArgumentException("Invalid cover side: " + side);
        };
    }
}
