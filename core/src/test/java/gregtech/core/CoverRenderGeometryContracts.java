package gregtech.core;

import gregtech.content.cover.CoverPlateGeometry;
import gregtech.content.cover.CoverRenderGeometry;
import java.util.ArrayList;
import java.util.List;

/** Pure geometry, not a client/GPU test: all cover masks, face ownership and physical-bound controls. */
final class CoverRenderGeometryContracts {
    private static int checks;
    private record Face(int axis, double plane, double a0, double a1, double b0, double b1) {}

    static int verify() {
        checks = 0;
        require(!CoverRenderGeometry.fullTexture(null), "empty face");
        for (var id : new String[]{"blank_cover", "material_plate_cover", "canvas_cover", "cover_pump", "logistics_display_cpu_logic"})
            require(CoverRenderGeometry.fullTexture(id), "full-texture cover: " + id);
        for (var id : new String[]{"cover_redstone_torch", "cover_redstone_repeater", "pressure_value"})
            require(!CoverRenderGeometry.fullTexture(id), "partial attachment must not own an edge: " + id);

        var wire = new CoverPlateGeometry.Box(7.0 / 16, 7.0 / 16, 7.0 / 16, 9.0 / 16, 9.0 / 16, 9.0 / 16);
        int rawHolderOverlap = 0, fixedHolderOverlap = 0;
        for (int side = 0; side < 6; side++) {
            int axisMask = 3 << (side & ~1);
            var raw = CoverPlateGeometry.holder(side);
            var render = CoverRenderGeometry.holder(side);
            int axis = axis(side);
            double[] a = bounds(raw), b = bounds(render);
            for (int i = 0; i < 3; i++) {
                require(b[i] == a[i] + (i == axis ? 0 : .005), "source holder minimum inset");
                require(b[i + 3] == a[i + 3] - (i == axis ? 0 : .005), "source holder maximum inset");
            }
            require(raw.equals(CoverPlateGeometry.holder(side)), "render call leaves physical holder unchanged");
            require(CoverRenderGeometry.plate(side, 0).equals(CoverPlateGeometry.plate(side)), "isolated backing unchanged");
            require(CoverRenderGeometry.plateFaces(side, axisMask) == 63, "opposite cover cannot hide lateral faces");
            require(CoverRenderGeometry.plateFaces(side, 63) == axisMask, "normal-axis front/back retained");
            rawHolderOverlap += overlaps(faces(raw, 63 & ~axisMask), faces(wire, 63));
            fixedHolderOverlap += overlaps(faces(render, 63 & ~axisMask), faces(wire, 63));
        }
        require(rawHolderOverlap == 24 && fixedHolderOverlap == 0, "six holder/wire orientations: 24 raw overlaps, zero inset overlaps");
        require(CoverPlateGeometry.plate(0).maxY() == .125 && CoverPlateGeometry.plate(5).minX() == .875,
                "two-pixel physical plate bounds unchanged");
        require(CoverPlateGeometry.holder(2).minX() == .4375 && CoverPlateGeometry.holder(2).maxX() == .5625,
                "physical torch bounds retain their exact width");
        for (int side = 0; side < 6; side++) {
            int axis = axis(side);
            double[] v = bounds(CoverPlateGeometry.valve(side));
            require(v[axis] == (side % 2 == 0 ? 0.0 : .5) && v[axis + 3] == (side % 2 == 0 ? .5 : 1.0),
                    "pressure valve recessed eight pixels");
            for (int i = 0; i < 3; i++) if (i != axis)
                require(v[i] == .375 && v[i + 3] == .625, "pressure valve four-pixel knob");
        }

        int adjacentPairs = 0, rawAdjacentOverlap = 0;
        for (int mask = 0; mask < 64; mask++) {
            var physical = new ArrayList<CoverPlateGeometry.Box>();
            var rendered = new ArrayList<CoverPlateGeometry.Box>();
            var rawFaces = new ArrayList<List<Face>>();
            var visible = new ArrayList<List<Face>>();
            for (int side = 0; side < 6; side++) if ((mask & 1 << side) != 0) {
                var box = CoverRenderGeometry.plate(side, mask);
                var raw = CoverPlateGeometry.plate(side);
                physical.add(raw);
                rendered.add(box);
                rawFaces.add(faces(raw, 63));
                int allowed = CoverRenderGeometry.plateFaces(side, mask);
                visible.add(faces(box, allowed));
                require(Integer.bitCount(allowed) == 6 - Integer.bitCount(mask & ~(3 << (side & ~1))), "exact plate quad count");
                for (int other = 0; other < 6; other++) if ((mask & 1 << other) == 0) {
                    for (var id : new String[]{"cover_redstone_torch", "cover_redstone_repeater", "pressure_value"}) {
                        int mixedMask = mask | (CoverRenderGeometry.fullTexture(id) ? 1 << other : 0);
                        require(CoverRenderGeometry.plate(side, mixedMask).equals(box), "partial attachment does not trim another plate");
                        require((CoverRenderGeometry.plateFaces(side, mixedMask) & 1 << other) != 0, "partial attachment leaves gap-facing quad");
                    }
                }
            }
            for (int i = 0; i < rendered.size(); i++) for (int j = 0; j < i; j++) {
                require(intersection(rendered.get(i), rendered.get(j)) == 0, "backing volume ownership, mask=" + mask);
                require(overlaps(visible.get(i), visible.get(j)) == 0, "no positive-area coplanar plate quads, mask=" + mask);
                if (Integer.bitCount(mask) == 2 && intersection(physical.get(i), physical.get(j)) > 0) {
                    adjacentPairs++;
                    rawAdjacentOverlap += overlaps(rawFaces.get(i), rawFaces.get(j));
                }
            }
            // All backing bounds use only 0,1/8,7/8,1: these 27 cells exhaust the possible union regions.
            for (double x : new double[]{.0625, .5, .9375})
                for (double y : new double[]{.0625, .5, .9375})
                    for (double z : new double[]{.0625, .5, .9375}) {
                        boolean covered = false;
                        int owners = 0;
                        for (var box : physical) covered |= inside(box, x, y, z);
                        for (var box : rendered) if (inside(box, x, y, z)) owners++;
                        require(owners == (covered ? 1 : 0), "same backing union, no holes or double volume, mask=" + mask);
                    }
        }
        require(adjacentPairs == 12 && rawAdjacentOverlap == 48, "all twelve adjacent orientations reproduce old mesh overlaps");
        for (int invalid : new int[]{-1, 6}) {
            boolean rejected = false;
            try { CoverRenderGeometry.plateFaces(invalid, 63); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "invalid side rejected");
        }
        return checks;
    }

    private static int axis(int side) { return side < 2 ? 1 : side < 4 ? 2 : 0; }
    private static double[] bounds(CoverPlateGeometry.Box b) { return new double[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}; }
    private static List<Face> faces(CoverPlateGeometry.Box box, int mask) {
        var out = new ArrayList<Face>();
        var bounds = bounds(box);
        for (int side = 0; side < 6; side++) if ((mask & 1 << side) != 0) {
            int axis = axis(side), a = (axis + 1) % 3, b = (axis + 2) % 3;
            out.add(new Face(axis, bounds[axis + (side % 2 == 0 ? 0 : 3)], bounds[a], bounds[a + 3], bounds[b], bounds[b + 3]));
        }
        return out;
    }
    private static int overlaps(List<Face> first, List<Face> second) {
        int count = 0;
        for (var a : first) for (var b : second)
            if (a.axis == b.axis && a.plane == b.plane && Math.min(a.a1, b.a1) > Math.max(a.a0, b.a0)
                    && Math.min(a.b1, b.b1) > Math.max(a.b0, b.b0)) count++;
        return count;
    }
    private static boolean inside(CoverPlateGeometry.Box b, double x, double y, double z) {
        return x > b.minX() && x < b.maxX() && y > b.minY() && y < b.maxY() && z > b.minZ() && z < b.maxZ();
    }
    private static double intersection(CoverPlateGeometry.Box a, CoverPlateGeometry.Box b) {
        return Math.max(0, Math.min(a.maxX(), b.maxX()) - Math.max(a.minX(), b.minX()))
                * Math.max(0, Math.min(a.maxY(), b.maxY()) - Math.max(a.minY(), b.minY()))
                * Math.max(0, Math.min(a.maxZ(), b.maxZ()) - Math.max(a.minZ(), b.minZ()));
    }
    private static void require(boolean ok, String why) { checks++; if (!ok) throw new IllegalStateException(why); }
    public static void main(String[] args) { System.out.println("Cover render geometry contracts passed: " + verify() + " assertions"); }
}
