package gregtech.content.recipe.generated;

import java.util.List;

import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/**
 * Source-linked Ice/Snow input descriptors, not a replacement for native OM/holder tests.
 * GregTech6/gregtech6@3703e40308c8c030763fd6297dea8b210d2a77b1:
 * Loader_Recipes_Chem.java:480-492,495-507,510-522; CS.java:120 defines U9=72072000.
 * The six fractional inputs were OM.dust(material,U9), never 111/1000 U.
 */
public final class ThermalIceSnowGeneratedContracts {
    private ThermalIceSnowGeneratedContracts() {}

    public static void run() {
        List<Row> chemistry = collect(GTChemGen::load);
        for (String map : List.of("Melter", "Smelter", "Drying")) {
            List<Row> actual = chemistry.stream().filter(row -> row.map().equals(map)
                    && row.inputs().stream().anyMatch(ThermalIceSnowGeneratedContracts::iceSnowInput)).toList();
            equal(expected(map), actual, map + " exact U9 inputs and unchanged neighboring source descriptors");
        }
        equal(6L, chemistry.stream().filter(row -> row.inputs().contains("omdu:Ice:72072000")
                || row.inputs().contains("omdu:Snow:72072000")).count(), "six bounded exact-U inputs");
        check(chemistry.stream().flatMap(row -> row.inputs().stream())
                .noneMatch(input -> input.equals("omd:Ice:111") || input.equals("omd:Snow:111")),
                "the six lossy Ice/Snow descriptors must not survive beside their replacements");
        equal(26L, chemistry.stream().filter(ThermalIceSnowGeneratedContracts::thermal).count(),
                "chemistry keeps its 26 thermal declarations");
        equal(18L, collect(GTFoodGen::load).stream().filter(ThermalIceSnowGeneratedContracts::thermal).count(),
                "the other 18 imported thermal declarations remain separate and unchanged in count");
    }

    private static boolean thermal(Row row) {
        return row.map().equals("Melter") || row.map().equals("Smelter");
    }

    private static boolean iceSnowInput(String input) {
        return input.contains(":Ice:") || input.contains(":Snow:")
                || List.of("v:ice:1", "v:packed_ice:1", "v:snowball:1", "v:snow:1").contains(input);
    }

    private static List<Row> expected(String map) {
        long multiplier = map.equals("Drying") ? 4 : 2;
        String fluid = map.equals("Drying") ? "f:DistW:" : "w:";
        return List.of(
                row(map, multiplier, fluid, "omdu:Ice:72072000", 111),
                row(map, multiplier, fluid, "omd:Ice:250", 250),
                row(map, multiplier, fluid, "omd:Ice:1000", 1000),
                row(map, multiplier, fluid, "i:gemChipped:Ice:1", 250),
                row(map, multiplier, fluid, "i:gemFlawed:Ice:1", 500),
                row(map, multiplier, fluid, "i:gem:Ice:1", 1000),
                row(map, multiplier, fluid, "v:ice:1", 1000),
                row(map, multiplier, fluid, "v:packed_ice:1", 2000),
                row(map, multiplier, fluid, "omdu:Snow:72072000", 111),
                row(map, multiplier, fluid, "omd:Snow:250", 250),
                row(map, multiplier, fluid, "omd:Snow:1000", 1000),
                row(map, multiplier, fluid, "v:snowball:1", 250),
                row(map, multiplier, fluid, "v:snow:1", 1000));
    }

    private static Row row(String map, long multiplier, String fluid, String input, long amount) {
        // Keep this legacy importer mode unchanged; this slice corrects only six input tokens.
        return new Row(false, map, 16, amount * multiplier, null, List.of(input), List.of(),
                List.of(fluid + amount), List.of());
    }
}
