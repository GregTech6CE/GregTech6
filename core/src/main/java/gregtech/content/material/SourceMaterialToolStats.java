package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded default tool-stat imports from GT6 MT.java at 3703e403.
 * These 71 rows lost factory/qual/steal inheritance (plus the packed Magic declaration).
 * qual(types, speed, durability, quality) clamps durability to one even when copying zero.
 * Other material identities and source form/processing flags are deliberately not inferred here.
 */
public final class SourceMaterialToolStats {
    private SourceMaterialToolStats() {}

    private record Stats(int types, float speed, long durability, int quality) {
        boolean matches(GTMaterial material) {
            return material.getToolTypes() == types && Float.compare(material.getToolSpeed(), speed) == 0
                    && material.getToolDurability() == durability && material.getToolQuality() == quality;
        }
        void apply(GTMaterial material) {
            material.setToolStats(types, speed, durability, quality);
        }
    }
    private record Row(int id, String name, Stats source) {}
    private static final Stats LOST_DEFAULT = new Stats(0, 1F, 0L, 0);
    private static final List<Row> ROWS = List.of(
            new Row(4000, "Magic", new Stats(3, 10F, 5120L, 5)), // MT539
            new Row(8210, "Blizz", new Stats(1, 2F, 16L, 1)), // MT1274
            new Row(8211, "Blaze", new Stats(1, 2F, 16L, 1)), // MT1275
            new Row(8227, "PeanutWood", new Stats(1, 2F, 16L, 0)), // MT1256
            new Row(8247, "Basalz", new Stats(1, 2F, 16L, 1)), // MT1272
            new Row(8248, "Blitz", new Stats(1, 2F, 16L, 1)), // MT1273
            new Row(8300, "Diamond", new Stats(3, 8F, 1280L, 3)), // MT1358
            new Row(8301, "Emerald", new Stats(3, 9F, 128L, 2)), // MT1371
            new Row(8302, "Ruby", new Stats(3, 7F, 512L, 3)), // MT1381
            new Row(8304, "Sapphire", new Stats(3, 7F, 512L, 3)), // MT1380
            new Row(8305, "GreenSapphire", new Stats(3, 7F, 512L, 3)), // MT1383
            new Row(8309, "RedJasper", new Stats(3, 7F, 256L, 2)), // MT1401
            new Row(8314, "OrangeSapphire", new Stats(3, 7F, 512L, 3)), // MT1386
            new Row(8315, "YellowSapphire", new Stats(3, 7F, 512L, 3)), // MT1385
            new Row(8323, "Aquamarine", new Stats(3, 9F, 128L, 2)), // MT1372
            new Row(8324, "Morganite", new Stats(3, 9F, 128L, 2)), // MT1373
            new Row(8328, "BlueSapphire", new Stats(3, 7F, 512L, 3)), // MT1382
            new Row(8348, "ChargedCertusQuartz", new Stats(1, 5F, 32L, 3)), // MT1565
            new Row(8349, "CoalCoke", new Stats(0, 1F, 1L, 0)), // MT1523
            new Row(8383, "PurpleSapphire", new Stats(3, 7F, 512L, 3)), // MT1384
            new Row(8384, "Heliodor", new Stats(3, 9F, 128L, 2)), // MT1374
            new Row(8385, "Goshenite", new Stats(3, 9F, 128L, 2)), // MT1375
            new Row(8386, "Bixbite", new Stats(3, 9F, 128L, 2)), // MT1376
            new Row(8387, "Maxixe", new Stats(3, 9F, 128L, 2)), // MT1377
            new Row(8390, "PetroleumCoke", new Stats(0, 1F, 1L, 0)), // MT1529
            new Row(8425, "OceanJasper", new Stats(3, 7F, 256L, 2)), // MT1402
            new Row(8426, "RainforestJasper", new Stats(3, 7F, 256L, 2)), // MT1403
            new Row(8427, "BlueJasper", new Stats(3, 7F, 256L, 2)), // MT1404
            new Row(8428, "GreenJasper", new Stats(3, 7F, 256L, 2)), // MT1405
            new Row(8429, "YellowJasper", new Stats(3, 7F, 256L, 2)), // MT1406
            new Row(8430, "TigerEye", new Stats(3, 7F, 256L, 2)), // MT1409
            new Row(8431, "CatsEye", new Stats(3, 7F, 256L, 2)), // MT1410
            new Row(8432, "DragonEye", new Stats(3, 7F, 256L, 2)), // MT1411
            new Row(8433, "HawksEye", new Stats(3, 7F, 256L, 2)), // MT1412
            new Row(8434, "BlackEye", new Stats(3, 7F, 256L, 2)), // MT1413
            new Row(8435, "TigerIron", new Stats(3, 7F, 256L, 2)), // MT1414
            new Row(8446, "BrownAventurine", new Stats(3, 7F, 256L, 2)), // MT1418
            new Row(8447, "YellowAventurine", new Stats(3, 7F, 256L, 2)), // MT1419
            new Row(8448, "GreenAventurine", new Stats(3, 7F, 256L, 2)), // MT1417
            new Row(8449, "BlackAventurine", new Stats(3, 7F, 256L, 2)), // MT1420
            new Row(8450, "BlueAventurine", new Stats(3, 7F, 256L, 2)), // MT1421
            new Row(8451, "RedAventurine", new Stats(3, 7F, 256L, 2)), // MT1422
            new Row(8464, "BlueDiamond", new Stats(3, 8F, 1280L, 3)), // MT1359
            new Row(8465, "GreenDiamond", new Stats(3, 8F, 1280L, 3)), // MT1360
            new Row(8466, "PurpleDiamond", new Stats(3, 8F, 1280L, 3)), // MT1361
            new Row(8467, "RedDiamond", new Stats(3, 8F, 1280L, 3)), // MT1362
            new Row(8468, "YellowDiamond", new Stats(3, 8F, 1280L, 3)), // MT1363
            new Row(8471, "Breeze", new Stats(1, 2F, 16L, 1)), // MT1276
            new Row(8622, "AluminiumAlloy", new Stats(2, 10F, 128L, 2)), // MT1702
            new Row(8637, "HSLASteel", new Stats(3, 6F, 512L, 2)), // MT1721
            new Row(8639, "HSLASpringSteel", new Stats(3, 6F, 512L, 2)), // MT1722
            new Row(8640, "AnnealedCopper", new Stats(2, 4F, 64L, 0)), // MT1656
            new Row(8644, "IronCompressed", new Stats(3, 6F, 256L, 2)), // MT1725
            new Row(8645, "IronMagnetic", new Stats(3, 6F, 256L, 2)), // MT1727
            new Row(8646, "SteelMagnetic", new Stats(3, 6F, 512L, 2)), // MT1728
            new Row(8647, "NeodymiumMagnetic", new Stats(3, 6F, 512L, 3)), // MT1729
            new Row(8725, "PulsatingIron", new Stats(3, 6F, 384L, 2)), // MT1806
            new Row(8726, "VibrantAlloy", new Stats(3, 12.5F, 64L, 2)), // MT1808
            new Row(8727, "ConductiveIron", new Stats(3, 6F, 384L, 2)), // MT1752
            new Row(8728, "EnergeticAlloy", new Stats(3, 12.5F, 64L, 2)), // MT1807
            new Row(8730, "ElectricalSteel", new Stats(3, 6F, 512L, 2)), // MT1809
            new Row(8731, "ObsidianSteel", new Stats(3, 6F, 512L, 2)), // MT1805
            new Row(8758, "FrozenIron", new Stats(3, 6F, 256L, 2)), // MT1847
            new Row(8803, "CastIron", new Stats(3, 6F, 256L, 2)), // MT1726
            new Row(9101, "Almandine", new Stats(3, 7F, 128L, 2)), // MT1393
            new Row(9102, "Andradite", new Stats(3, 7F, 128L, 2)), // MT1397
            new Row(9119, "Grossular", new Stats(3, 7F, 128L, 2)), // MT1394
            new Row(9127, "Pyrope", new Stats(3, 7F, 128L, 2)), // MT1395
            new Row(9129, "Spessartine", new Stats(3, 7F, 128L, 2)), // MT1396
            new Row(9135, "Uvarovite", new Stats(3, 7F, 128L, 2)), // MT1398
            new Row(9243, "ClayBrick", new Stats(0, 1F, 1L, 0)) // MT1280
    );

    private static boolean identity(Row row, GTMaterial material) {
        return material != null && material.isValid() && material.resolve() == material
                && material.getId() == row.id() && material.getName().equals(row.name())
                && GTMaterialRegistry.get(row.id()) == material
                && GTMaterialRegistry.get(row.name()) == material && !AddonMaterials.owns(material);
    }

    /**
     * Exact finite cohort for per-stack compatibility; never resolves caller aliases or accepts
     * a matching name/tuple on a different object. A subsequent addon stat override opts out.
     * Safe during built-in linking, before the registry's core-material snapshot exists.
     */
    public static boolean matchesCorrectedSource(GTMaterial material) {
        if (material == null) return false;
        for (Row row : ROWS) {
            if (row.id() == material.getId()) return identity(row, material) && row.source().matches(material);
        }
        return false;
    }

    /** Built-in linking only: collect eligible objects before writes, retaining unexpected overrides. */
    static void apply() {
        var eligible = new ArrayList<Row>();
        for (Row row : ROWS) {
            GTMaterial material = GTMaterialRegistry.get(row.id());
            if (identity(row, material) && LOST_DEFAULT.matches(material)) eligible.add(row);
        }
        for (Row row : eligible) row.source().apply(GTMaterialRegistry.get(row.id()));
    }
}
