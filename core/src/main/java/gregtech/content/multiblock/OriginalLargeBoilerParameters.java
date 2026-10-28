/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. Source boiler tool and structure rules. */
package gregtech.content.multiblock;

import gregtech.api.multiblock.StructureGrid;
import java.util.*;

/** Original five boiler variants, rated capacities and geometry. */
public final class OriginalLargeBoilerParameters {
    private OriginalLargeBoilerParameters() {}

    public static final int WATER_CAPACITY = 128_000, HU_PER_WATER = 80, STEAM_PER_WATER = 160;

    /** Source checkStructure2 uses world X/Z around the body centre, irrespective of front. */
    public record CheckCell(int x, int y, int z, StructureGrid.Role role) {}

    public static final List<CheckCell> CHECK_ORDER = checkOrder();

    private static List<CheckCell> checkOrder() {
        return SharedBoilerStructure.LAYOUT.cells().stream()
                .map(c -> new CheckCell(c.right(), c.up(), c.back() - 1, c.role()))
                .sorted(java.util.Comparator.comparingInt((CheckCell c) -> c.role() == StructureGrid.Role.AIR
                                ? 0
                                : c.y() < 1 ? 1 : c.x() == 0 && c.z() == 0 ? 2 : 3)
                        .thenComparingInt(CheckCell::y)
                        .thenComparingInt(CheckCell::z)
                        .thenComparingInt(CheckCell::x))
                .toList();
    }

    public static boolean contains(int dx, int dy, int dz) {
        return dx >= -1 && dx <= 1 && dy >= -1 && dy <= 2 && dz >= -1 && dz <= 1;
    }

    /** MultiTileEntityLargeBoiler.onMagnifyingGlass2 and LH.percent, including two decimal places. */
    public static String calcificationMessage(int efficiency) {
        if (efficiency >= 10000) return "No Calcification in this Boiler";
        int scale = 10000 - Math.max(0, efficiency);
        return "Calcification: " + scale / 100 + "." + (scale % 100 < 10 ? "0" : "") + scale % 100 + "%";
    }

    public record Variant(int originalId, String path, int denseWallId, long steamOutput, float hardness) {
        public long heatCapacity() {
            return steamOutput * 10_000L;
        }

        public long steamCapacity() {
            return steamOutput * 10_000L;
        }

        public long heatInputRecommended() {
            return steamOutput / 2;
        }
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant(17201, "stainless_steel_boiler_main_barometer", 18022, 8_192, 6),
            new Variant(17202, "titanium_boiler_main_barometer", 18026, 16_384, 9),
            new Variant(17203, "tungstensteel_boiler_main_barometer", 18023, 32_768, 12.5f),
            new Variant(17204, "adamantium_boiler_main_barometer", 18025, 262_144, 100),
            new Variant(17205, "invar_boiler_main_barometer", 18027, 8_192, 6));

    public static final StructureGrid LAYOUT = layout();

    public static List<Variant> all() {
        return VARIANTS;
    }

    public static Variant byOriginalId(int id) {
        return VARIANTS.stream()
                .filter(v -> v.originalId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GT6 large boiler " + id));
    }

    private static StructureGrid layout() {
        return SharedBoilerStructure.LAYOUT;
    }
}
