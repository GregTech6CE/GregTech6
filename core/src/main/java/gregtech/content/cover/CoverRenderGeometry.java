/* Adapted from GregTech-6 Team ICover.BOXES_HOLDERS and cover texture culling, LGPL-3.0-or-later. */
package gregtech.content.cover;

/** Render-only source rules. Physical plate/torch bounds remain in CoverPlateGeometry. */
public final class CoverRenderGeometry {
    private CoverRenderGeometry() {}
    /** Both originals use CS.PX_OFFSET in block units, not pixels. */
    public static final double HOLDER_INSET = 0.005;

    /** Original isFullTexture policy, independent of physical support/collision policy. */
    public static boolean fullTexture(String behavior) {
        return behavior != null && switch (behavior) {
            case "cover_redstone_torch", "cover_redstone_repeater", "pressure_value" -> false;
            default -> true;
        };
    }

    /** Inset only the holder's four lateral faces so they cannot coincide with a two-pixel wire. */
    public static CoverPlateGeometry.Box holder(int side) {
        var box = CoverPlateGeometry.holder(side);
        double x = side >= 4 ? 0 : HOLDER_INSET;
        double y = side < 2 ? 0 : HOLDER_INSET;
        double z = side >= 2 && side < 4 ? 0 : HOLDER_INSET;
        return new CoverPlateGeometry.Box(box.minX() + x, box.minY() + y, box.minZ() + z,
                box.maxX() - x, box.maxY() - y, box.maxZ() - z);
    }

    /** Give shared edge/corner volume to the lower side ordinal, without changing physical boxes.
     * Outward art is still full-sized; this only partitions the independently drawn backing cuboids.
     */
    public static CoverPlateGeometry.Box plate(int side, int fullCoverMask) {
        var box = CoverPlateGeometry.plate(side);
        double[] bounds = {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()};
        for (int other = 0; other < side; other++) {
            if ((other & ~1) == (side & ~1) || (fullCoverMask & 1 << other) == 0) continue;
            int axis = other < 2 ? 1 : other < 4 ? 2 : 0;
            if ((other & 1) == 0) bounds[axis] = CoverPlateGeometry.THICKNESS;
            else bounds[axis + 3] = 1 - CoverPlateGeometry.THICKNESS;
        }
        return new CoverPlateGeometry.Box(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
    }

    /** A perpendicular full cover/host face owns that edge; front and back always remain. */
    public static int plateFaces(int coverSide, int fullFaceMask) {
        if (coverSide < 0 || coverSide >= 6) throw new IllegalArgumentException("Invalid cover side: " + coverSide);
        int normalAxis = 3 << (coverSide & ~1);
        return 63 & ~(fullFaceMask & ~normalAxis);
    }
}
