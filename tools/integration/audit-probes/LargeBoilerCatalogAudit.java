import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters;
import java.util.HashMap;

/** Enumerates actual core geometry and ratings; does not execute the native boiler. */
class LargeBoilerCatalogAudit {
    public static void main(String[] args) {
        for (var variant : OriginalLargeBoilerParameters.all()) {
            System.out.println("BOILER|" + variant.originalId() + "|" + variant.denseWallId()
                    + "|" + variant.steamOutput() + "|" + variant.hardness()
                    + "|" + variant.heatCapacity() + "|" + variant.steamCapacity()
                    + "|" + variant.heatInputRecommended());
        }
        for (int[] front : new int[][] {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
            var preview = new HashMap<StructureGrid.Position, StructureGrid.Role>();
            for (var cell : OriginalLargeBoilerParameters.LAYOUT.cells()) {
                var position = StructureGrid.offset(front[0], 0, front[1],
                        cell.right(), cell.up(), cell.back());
                preview.put(position, cell.role());
            }
            var runtime = new HashMap<StructureGrid.Position, StructureGrid.Role>();
            for (var cell : OriginalLargeBoilerParameters.CHECK_ORDER) {
                var position = new StructureGrid.Position(cell.x() - front[0], cell.y(), cell.z() - front[1]);
                if (runtime.put(position, cell.role()) != null)
                    throw new AssertionError("Duplicate runtime cell");
            }
            if (!preview.equals(runtime)) throw new AssertionError("Preview / check geometry differs");
            if (preview.size() != 36) throw new AssertionError("Unexpected boiler size");
            System.out.println("GEOMETRY|" + front[0] + "|" + front[1] + "|" + preview.size());
        }
        var counts = new java.util.EnumMap<StructureGrid.Role, Integer>(StructureGrid.Role.class);
        for (var cell : OriginalLargeBoilerParameters.LAYOUT.cells()) counts.merge(cell.role(), 1, Integer::sum);
        counts.forEach((role, count) -> System.out.println("ROLE|" + role + "|" + count));
        long capacity = OriginalLargeBoilerParameters.byOriginalId(17204).steamCapacity();
        System.out.println("SENSOR_BOUNDARY|" + capacity + "|"
                + com.gregtech.gregtech.api.fluid.LongFluidStorage.bindInt(capacity));
    }
}
