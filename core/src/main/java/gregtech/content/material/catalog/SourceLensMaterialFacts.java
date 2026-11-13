package gregtech.content.material.catalog;

// source-rows: the per-row MT line numbers are transcription labels.
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.content.material.MaterialFactKeys;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import java.util.LinkedHashMap;
import java.util.List;

/** Explicit MT.lens roles, including diamond/emerald/sapphire/garnet/jasper/tigereye/aventurine factories.
 * Source: GregTech6 3703e40308c8c030763fd6297dea8b210d2a77b1, MT.java.
 * Color is a crafting role, never inferred from RGB. IDs and canonical names guard native identities.
 */
public final class SourceLensMaterialFacts {
    public enum Color {
        WHITE,
        RED,
        GREEN,
        BLUE,
        CYAN,
        MAGENTA,
        YELLOW,
        PINK
    }

    private record Role(int id, String name, Color color) {}

    private static final List<Role> ROLES = List.of(
            new Role(400, "Zirconium", Color.WHITE) /* MT:597 */,
            new Role(8001, "Glass", Color.WHITE) /* MT:1057 */,
            new Role(8300, "Diamond", Color.WHITE) /* MT:1358 */,
            new Role(8464, "BlueDiamond", Color.BLUE) /* MT:1359 */,
            new Role(8465, "GreenDiamond", Color.GREEN) /* MT:1360 */,
            new Role(8467, "RedDiamond", Color.RED) /* MT:1362 */,
            new Role(8468, "YellowDiamond", Color.YELLOW) /* MT:1363 */,
            new Role(8424, "PinkDiamond", Color.PINK) /* MT:1364 */,
            new Role(8423, "DiamondIndustrial", Color.YELLOW) /* MT:1365 */,
            new Role(8278, "ManaDiamond", Color.CYAN) /* MT:1366 */,
            new Role(8301, "Emerald", Color.GREEN) /* MT:1371 */,
            new Role(8323, "Aquamarine", Color.CYAN) /* MT:1372 */,
            new Role(8324, "Morganite", Color.PINK) /* MT:1373 */,
            new Role(8384, "Heliodor", Color.YELLOW) /* MT:1374 */,
            new Role(8385, "Goshenite", Color.WHITE) /* MT:1375 */,
            new Role(8386, "Bixbite", Color.RED) /* MT:1376 */,
            new Role(8387, "Maxixe", Color.BLUE) /* MT:1377 */,
            new Role(8304, "Sapphire", Color.BLUE) /* MT:1380 */,
            new Role(8302, "Ruby", Color.RED) /* MT:1381 */,
            new Role(8328, "BlueSapphire", Color.BLUE) /* MT:1382 */,
            new Role(8305, "GreenSapphire", Color.GREEN) /* MT:1383 */,
            new Role(8315, "YellowSapphire", Color.YELLOW) /* MT:1385 */,
            new Role(8326, "Spinel", Color.GREEN) /* MT:1389 */,
            new Role(8303, "BalasRuby", Color.RED) /* MT:1390 */,
            new Role(9101, "Almandine", Color.RED) /* MT:1393 */,
            new Role(9129, "Spessartine", Color.RED) /* MT:1396 */,
            new Role(9102, "Andradite", Color.YELLOW) /* MT:1397 */,
            new Role(8307, "BlueTopaz", Color.BLUE) /* MT:1426 */,
            new Role(8417, "Amazonite", Color.GREEN) /* MT:1429 */,
            new Role(8312, "Opal", Color.BLUE) /* MT:1431 */,
            new Role(8415, "OnyxRed", Color.RED) /* MT:1432 */,
            new Role(8313, "Amethyst", Color.PINK) /* MT:1436 */,
            new Role(8325, "Dioptase", Color.CYAN) /* MT:1437 */,
            new Role(8322, "Craponite", Color.MAGENTA) /* MT:1442 */,
            new Role(8321, "Jade", Color.GREEN) /* MT:1443 */,
            new Role(8419, "Zircon", Color.RED) /* MT:1511 */,
            new Role(8333, "Redstone", Color.RED) /* MT:1540 */,
            new Role(8340, "Nikolite", Color.CYAN) /* MT:1541 */,
            new Role(9225, "HexoriumRed", Color.RED) /* MT:1597 */,
            new Role(9226, "HexoriumGreen", Color.GREEN) /* MT:1598 */,
            new Role(9228, "HexoriumBlue", Color.BLUE) /* MT:1600 */,
            new Role(9239, "HexoriumWhite", Color.WHITE) /* MT:1611 */,
            new Role(9109, "Realgar", Color.RED) /* MT:3708 */,
            new Role(8309, "RedJasper", Color.RED) /* MT:1401 */,
            new Role(8426, "RainforestJasper", Color.YELLOW) /* MT:1403 */,
            new Role(8427, "BlueJasper", Color.BLUE) /* MT:1404 */,
            new Role(8428, "GreenJasper", Color.GREEN) /* MT:1405 */,
            new Role(8429, "YellowJasper", Color.YELLOW) /* MT:1406 */,
            new Role(8430, "TigerEye", Color.YELLOW) /* MT:1409 */,
            new Role(8431, "CatsEye", Color.GREEN) /* MT:1410 */,
            new Role(8432, "DragonEye", Color.RED) /* MT:1411 */,
            new Role(8433, "HawksEye", Color.BLUE) /* MT:1412 */,
            new Role(8435, "TigerIron", Color.YELLOW) /* MT:1414 */,
            new Role(8448, "GreenAventurine", Color.GREEN) /* MT:1417 */,
            new Role(8447, "YellowAventurine", Color.YELLOW) /* MT:1419 */,
            new Role(8450, "BlueAventurine", Color.BLUE) /* MT:1421 */,
            new Role(8451, "RedAventurine", Color.RED) /* MT:1422 */);

    private SourceLensMaterialFacts() {}

    public static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        for (Role role : ROLES) {
            if (role.id != declaration.id()) continue;
            if (!materialNameMatches(role.name, declaration.name())) continue;
            var info = declaration.info();
            var rules = new LinkedHashMap<>(info.processes().rules());
            rules.put(MaterialFactKeys.SOURCE_LENS_ROLE, List.of(MaterialProcessRule.anyForm(role.color)));
            return new MaterialDefinition.Declaration(
                    declaration.material(),
                    new MaterialInfoDefinition(
                            info.tooling(),
                            info.composition(),
                            info.relations(),
                            new MaterialInfoDefinition.Processes(rules)));
        }
        return declaration;
    }

    private static boolean materialNameMatches(String source, String port) {
        return source.equals(port);
    }
}
