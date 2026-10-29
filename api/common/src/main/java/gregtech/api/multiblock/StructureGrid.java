package gregtech.api.multiblock;

import java.util.*;

/** Loader-free source coordinates and roles. Minecraft world/registry access is a boundary. */
public final class StructureGrid {
    public record Position(int x, int y, int z) {}
    public record Cell(int right, int up, int back, StructureRole role) {}

    private final List<Cell> cells;

    public StructureGrid(List<Cell> cells) {
        var positions = new HashSet<Position>();
        for (var cell : cells)
            if (!positions.add(new Position(cell.right(), cell.up(), cell.back())))
                throw new IllegalArgumentException("Duplicate structure cell");
        this.cells = List.copyOf(cells);
    }

    public List<Cell> cells() {
        return cells;
    }

    public static Position offset(int frontX, int frontY, int frontZ, int right, int up, int back) {
        return frontY == 0
                ? new Position(-frontZ * right - frontX * back, up, frontX * right - frontZ * back)
                : new Position(right, -frontY * back, -up);
    }

    public static boolean lowest(int frontY, int up, int back) {
        return frontY > 0 ? back == 3 : frontY < 0 ? back == 0 : up == -1;
    }

    public static StructureRole fluidRole(int frontY, int up, int back) {
        return back == 0
                ? (lowest(frontY, up, back) ? StructureRole.FLUID_IO : StructureRole.FLUID_INPUT)
                : (lowest(frontY, up, back) ? StructureRole.FLUID_OUTPUT : StructureRole.CASING);
    }

    public static StructureRole axialRole(int frontY, int right, int up, int back, boolean fluids) {
        return right == 0 && up == 0 && back == 3
                ? StructureRole.ENERGY_OUTPUT
                : fluids ? fluidRole(frontY, up, back) : StructureRole.CASING;
    }

    public static boolean canAutoOutput(int faceY, boolean gas, int density) {
        return faceY == 0 || gas || faceY == (density < 0 ? 1 : -1);
    }

    public static boolean hollowTank(int controllerX, int controllerY, int controllerZ,
                                     int frontX, int frontY, int frontZ, int size,
                                     java.util.function.Predicate<Position> loaded,
                                     java.util.function.Predicate<Position> wall,
                                     java.util.function.Predicate<Position> air) {
        if (size != 3 && size != 5) throw new IllegalArgumentException("Tank size: " + size);
        int radius = size / 2;
        int centerX = controllerX - frontX * radius,
                centerY = controllerY - frontY * radius,
                centerZ = controllerZ - frontZ * radius;
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    var position = new Position(centerX + x, centerY + y, centerZ + z);
                    if (!loaded.test(position)) return false;
                    if (position.x() == controllerX && position.y() == controllerY && position.z() == controllerZ)
                        continue;
                    boolean shell = Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius;
                    if (!(shell ? wall.test(position) : air.test(position))) return false;
                }
        return true;
    }
}
