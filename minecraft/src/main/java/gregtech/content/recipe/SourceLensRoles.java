package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Explicit MT.lens roles, including diamond/emerald/sapphire/garnet/jasper/tigereye/aventurine factories.
 * Source: GregTech6 3703e40308c8c030763fd6297dea8b210d2a77b1, MT.java.
 * Color is a crafting role, never inferred from RGB. IDs and canonical names guard native identities.
 */
public final class SourceLensRoles {
    public enum Color { WHITE, RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW, PINK }
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
            new Role(8279, "ElvenDragonstone", Color.MAGENTA) /* MT:1367 */,
            new Role(8294, "Gravitite", Color.MAGENTA) /* MT:1368 */,
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
            new Role(8469, "GoldenAmber", Color.YELLOW) /* MT:1440 */,
            new Role(8422, "DominicanAmber", Color.BLUE) /* MT:1441 */,
            new Role(8322, "Craponite", Color.MAGENTA) /* MT:1442 */,
            new Role(8321, "Jade", Color.GREEN) /* MT:1443 */,
            new Role(8282, "CrimsonMiddle", Color.RED) /* MT:1487 */,
            new Role(8283, "GreenMiddle", Color.GREEN) /* MT:1488 */,
            new Role(8284, "AquaMiddle", Color.BLUE) /* MT:1489 */,
            new Role(8285, "Valonite", Color.PINK) /* MT:1490 */,
            new Role(8288, "Scabyst", Color.CYAN) /* MT:1491 */,
            new Role(8293, "Ambrosium", Color.YELLOW) /* MT:1494 */,
            new Role(8329, "AmethystEnder", Color.PINK) /* MT:1498 */,
            new Role(8317, "Dilithium", Color.WHITE) /* MT:1508 */,
            new Role(8419, "Zircon", Color.RED) /* MT:1511 */,
            new Role(8333, "Redstone", Color.RED) /* MT:1540 */,
            new Role(8340, "Nikolite", Color.CYAN) /* MT:1541 */,
            new Role(8351, "InfusedVis", Color.MAGENTA) /* MT:1586 */,
            new Role(8352, "InfusedAir", Color.YELLOW) /* MT:1587 */,
            new Role(8353, "InfusedFire", Color.RED) /* MT:1588 */,
            new Role(8354, "InfusedEarth", Color.GREEN) /* MT:1589 */,
            new Role(8355, "InfusedWater", Color.BLUE) /* MT:1590 */,
            new Role(8357, "InfusedOrder", Color.WHITE) /* MT:1592 */,
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
            new Role(8451, "RedAventurine", Color.RED) /* MT:1422 */
    );
    private SourceLensRoles() {}

    public static Color color(GTMaterial material) {
        if (material == null || !material.isValid() || gregtech.engine.addon.AddonMaterials.owns(material)) return null;
        for (Role role : ROLES)
            if (material.getId() == role.id && material.getName().replace(" ", "").equals(role.name)
                    && GTMaterialRegistry.get(role.id) == material && GTMaterialRegistry.get(role.name) == material)
                return role.color;
        return null;
    }

    /** Only actual registered native lens holders; recipe matching retains normal form/tag equivalence. */
    public static List<ItemStack> lenses(Color color) {
        var result = new ArrayList<ItemStack>();
        for (Role role : ROLES) {
            if (role.color != color) continue;
            var material = GTMaterialRegistry.get(role.id);
            if (color(material) != color) continue;
            var lens = GTItems.getStack(MaterialPrefix.lens, material, 1);
            if (!lens.isEmpty()) result.add(lens);
        }
        return List.copyOf(result);
    }
}
