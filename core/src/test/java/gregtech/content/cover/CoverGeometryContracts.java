package gregtech.content.cover;

/** Source bounds in all six orientations, independent of a game renderer. */
public final class CoverGeometryContracts {
    public static void main(String[] args) {
        for (int face = 0; face < 6; face++) {
            var valve = CoverPlateGeometry.attachment("pressure_value", face);
            double[] min = {valve.minX(), valve.minY(), valve.minZ()};
            double[] max = {valve.maxX(), valve.maxY(), valve.maxZ()};
            int axis = face < 2 ? 1 : face < 4 ? 2 : 0;
            for (int i = 0; i < 3; i++) {
                check(max[i] - min[i] == (i == axis ? .5 : .25), "4x4x8 valve, face " + face);
                check(min[i] >= 0 && max[i] <= 1, "valve stays inside host");
                if (i != axis) check(min[i] == .375 && max[i] == .625, "centered stem");
            }
            check((face & 1) == 0 ? min[axis] == 0 : max[axis] == 1, "outward face at boundary");
            check(!valve.equals(CoverPlateGeometry.attachment("warning_cover", face)), "valve is not a full support plate");
            var panel = CoverPlateGeometry.attachment("warning_cover", face);
            check((panel.maxX()-panel.minX())*(panel.maxY()-panel.minY())*(panel.maxZ()-panel.minZ()) == .125,
                    "ordinary covers retain two-pixel thickness");
        }
        System.out.println("Cover geometry contracts passed.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
