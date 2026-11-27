package gregtech.content.worldgen;

import java.util.List;

/**
 * GT6 stone layer + contact ore tables from {@code Loader_Worldgen} (the source of
 * the worldgenerationnew.cfg stonelayers/doublelayers sections). Declaration order and weights
 * determine the selection within each {@link StoneLayerField} palette.
 *
 * <p>Imported from GT6; maintained here because the historical generator is not shipped.</p>
 */
public final class GTStoneLayersGen {
    /** Per-block ore roll inside a layer: probability = 1/chanceDiv, within [minY,maxY] (old coords). */
    public record OreDef(String material, boolean indicators, int chanceDiv, int minY, int maxY, String blockId) {
        public boolean providesOre() {
            return !material.isEmpty();
        }

        public OreDef(String material, boolean indicators, int chanceDiv, int minY, int maxY) {
            this(material, indicators, chanceDiv, minY, maxY, "");
        }
    }

    /**
     * stoneType = StoneType enum name, or null. blockId = registry id of a whole-block
     * ore seam (GT6 BlockRockOres) or an explicit vanilla rock, or null. Both null keeps the host.
     * material names the rock used for geological contacts. surfaceMaterial names its surface pebble.
     * noDeep layers use deepslate below the shallow layer threshold.
     */
    public record LayerDef(
            String stoneType,
            String blockId,
            String material,
            String surfaceMaterial,
            boolean noDeep,
            List<OreDef> ores,
            int weight) {
        LayerDef weighted(int amount) {
            return new LayerDef(stoneType, blockId, material, surfaceMaterial, noDeep, ores, amount);
        }

        LayerDef withSurface(String pebble) {
            return new LayerDef(stoneType, blockId, material, pebble, noDeep, ores, weight);
        }

        public boolean isOreSeam() {
            return blockId != null && blockId.startsWith("block_ore_");
        }
    }

    public record ContactDef(boolean bothSides, String materialA, String materialB, List<OreDef> ores) {}

    static final List<LayerDef> DEEP_LAYERS = List.of(
            blockLayer("minecraft:deepslate", "Deepslate", false),
            blockLayer("minecraft:tuff", "Tuff", false, ore("Alunite", true, 256, 12, 28)));

    private GTStoneLayersGen() {}

    private static OreDef ore(String material, boolean indicators, int chanceDiv, int minY, int maxY) {
        return new OreDef(material, indicators, chanceDiv, minY, maxY);
    }

    private static OreDef vanillaOre(String material, int chanceDiv, int minY, int maxY, String block) {
        return new OreDef(material, false, chanceDiv, minY, maxY, "minecraft:" + block);
    }

    private static LayerDef layer(String material, boolean noDeep, OreDef... ores) {
        return new LayerDef(null, null, "Stone", material, noDeep, List.of(ores), 1);
    }

    private static LayerDef stoneLayer(String stoneType, String material, boolean noDeep, OreDef... ores) {
        return new LayerDef(stoneType, null, material, material, noDeep, List.of(ores), 1);
    }

    private static LayerDef blockLayer(String blockId, String material, boolean noDeep, OreDef... ores) {
        return new LayerDef(null, blockId, material, material, noDeep, List.of(ores), 1);
    }

    private static ContactDef contact(boolean bothSides, String a, String b, OreDef... ores) {
        return new ContactDef(bothSides, a, b, List.of(ores));
    }

    public static final List<LayerDef> LAYERS = List.of(
            blockLayer("block_ore_anthracite", "Coal", true),
            blockLayer("block_ore_lignite", "Lignite", true),
            blockLayer("block_ore_salt", "NaCl", true),
            blockLayer("block_ore_rocksalt", "KCl", true),
            blockLayer("block_ore_bauxite", "Bauxite", true),
            blockLayer("block_ore_oil", "Oilshale", true),
            blockLayer("block_ore_gypsum", "Gypsum", true),
            blockLayer("block_ore_milkyquartz", "MilkyQuartz", true),
            layer("Stone", false).weighted(10),
            layer("Chert", false).weighted(3),
            layer("Pumice", false),
            layer(
                    "Stone",
                    false,
                    ore("Peridot", true, 64, 32, 64),
                    vanillaOre("Emerald", 48, 16, 60, "emerald_ore"),
                    vanillaOre("Diamond", 128, 8, 24, "diamond_ore"),
                    vanillaOre("Lapis", 48, 16, 48, "lapis_ore")),
            layer("Dacite", false),
            layer("Dacite", false, ore("Nikolite", true, 32, 0, 20)),
            layer("Stone", false).weighted(2),
            layer(
                    "Stone",
                    false,
                    vanillaOre("Redstone", 32, 8, 24, "redstone_ore"),
                    ore("Cinnabar", false, 128, 8, 24),
                    vanillaOre("Au", 64, 8, 32, "gold_ore"),
                    vanillaOre("Au", 32, 33, 64, "gold_ore"),
                    vanillaOre("Coal", 8, 60, 100, "coal_ore"),
                    vanillaOre("", 48, 0, 255, "infested_stone"),
                    ore("NaCl", false, 32, 50, 80)),
            layer(
                    "Stone",
                    false,
                    ore("Fe2O3", true, 6, 30, 70),
                    ore("Jasper", true, 64, 32, 64),
                    ore("JasperYellow", true, 64, 32, 64),
                    ore("TigerIron", true, 64, 32, 64),
                    vanillaOre("Cu", 16, 20, 50, "copper_ore"),
                    vanillaOre("Fe", 16, 40, 80, "iron_ore")),
            layer("Stone", false),
            stoneLayer("KOMATIITE", "Komatiite", false),
            stoneLayer(
                    "KOMATIITE",
                    "Komatiite",
                    false,
                    ore("MgCO3", true, 16, 20, 50),
                    ore("Cinnabar", true, 12, 0, 32),
                    ore("Redstone", true, 8, 0, 30),
                    ore("Pyrite", true, 12, 0, 30)),
            stoneLayer("KIMBERLITE", "Kimberlite", false),
            stoneLayer(
                    "KIMBERLITE",
                    "Kimberlite",
                    false,
                    ore("Diamond", true, 48, 0, 12),
                    ore("Spinel", true, 48, 24, 48),
                    ore("BalasRuby", true, 48, 24, 48)),
            stoneLayer("BASALT", "Basalt", false).weighted(6),
            stoneLayer(
                    "BASALT",
                    "Basalt",
                    false,
                    ore("Asbestos", true, 16, 16, 48),
                    ore("FluoriteWhite", true, 16, 0, 48),
                    ore("S", true, 8, 0, 32)),
            stoneLayer(
                    "BASALT",
                    "Basalt",
                    false,
                    ore("Bastnasite", true, 24, 24, 32),
                    ore("Monazite", true, 32, 24, 32),
                    ore("MnO2", true, 8, 16, 48)),
            stoneLayer(
                    "BASALT",
                    "Basalt",
                    false,
                    ore("Peridot", true, 32, 0, 32),
                    ore("Uvarovite", true, 32, 8, 40),
                    ore("Grossular", true, 32, 16, 48),
                    ore("Chromite", true, 8, 24, 64)),
            stoneLayer("ANDESITE", "Andesite", false).weighted(2),
            stoneLayer("ANDESITE", "Andesite", false, ore("Bromargyrite", true, 8, 0, 32)),
            stoneLayer("ANDESITE", "Andesite", false, ore("Au", true, 12, 0, 32), ore("Au", true, 8, 33, 64)),
            stoneLayer("MARBLE", "Marble", false, ore("Opal", true, 96, 16, 54)),
            stoneLayer("MARBLE", "Marble", false, ore("Rutile", true, 60, 4, 28), ore("Ilmenite", true, 48, 4, 28)),
            stoneLayer("MARBLE", "Marble", false).weighted(4),
            stoneLayer(
                    "MARBLE", "Marble", false, ore("Cassiterite", true, 16, 20, 80), ore("Dioptase", true, 64, 24, 48)),
            stoneLayer(
                    "MARBLE",
                    "Marble",
                    false,
                    ore("Cassiterite", true, 16, 20, 80),
                    ore("Stannite", true, 16, 38, 82),
                    ore("Kesterite", true, 16, 38, 82),
                    ore("Sphalerite", true, 8, 10, 30),
                    ore("Chalcopyrite", true, 8, 0, 20),
                    ore("Pyrite", true, 12, 0, 30)),
            // These limestone variants can occur below sea level after the height remap.
            stoneLayer("LIMESTONE", "Limestone", false, ore("Alunite", true, 40, 12, 78)),
            stoneLayer(
                    "LIMESTONE",
                    "Limestone",
                    false,
                    ore("Glauconite", true, 32, 30, 74),
                    ore("Diatomite", true, 48, 16, 90)),
            stoneLayer(
                    "LIMESTONE",
                    "Limestone",
                    false,
                    ore("KIO3", true, 56, 35, 73),
                    ore("IodineSalt", true, 48, 40, 78)),
            stoneLayer(
                    "LIMESTONE",
                    "Limestone",
                    false,
                    ore("Trona", true, 32, 24, 80),
                    ore("Mirabilite", true, 40, 24, 80)),
            stoneLayer("LIMESTONE", "Limestone", false, ore("NaCl", true, 8, 40, 80)),
            stoneLayer(
                    "LIMESTONE",
                    "Limestone",
                    false,
                    ore("Stibnite", true, 24, 10, 30),
                    ore("Galena", true, 8, 30, 120),
                    ore("Pb", true, 16, 50, 70)),
            stoneLayer(
                    "LIMESTONE",
                    "Limestone",
                    false,
                    ore("Galena", true, 8, 5, 25),
                    ore("Galena", true, 8, 80, 120),
                    ore("Wulfenite", true, 32, 30, 45),
                    ore("Powellite", true, 32, 35, 50),
                    ore("Molybdenite", true, 128, 30, 50)),
            stoneLayer(
                    "LIMESTONE",
                    "Limestone",
                    false,
                    ore("Scheelite", true, 64, 0, 16),
                    ore("Tungstate", true, 64, 0, 16),
                    ore("YellowLimonite", true, 8, 16, 48),
                    ore("BrownLimonite", true, 8, 32, 64),
                    ore("Malachite", true, 12, 16, 64),
                    ore("Azurite", true, 24, 16, 64)),
            stoneLayer(
                    "GRANITE_BLACK",
                    "GraniteBlack",
                    false,
                    ore("Emerald", true, 64, 24, 48),
                    ore("Aquamarine", true, 64, 8, 32),
                    ore("Morganite", true, 64, 24, 48),
                    ore("Heliodor", true, 64, 24, 48)),
            stoneLayer(
                    "GRANITE_BLACK",
                    "GraniteBlack",
                    false,
                    ore("Goshenite", true, 64, 24, 48),
                    ore("Bixbite", true, 64, 24, 48),
                    ore("Maxixe", true, 64, 24, 48)),
            stoneLayer(
                    "GRANITE_BLACK",
                    "GraniteBlack",
                    false,
                    ore("Cooperite", true, 32, 0, 16),
                    ore("Sperrylite", true, 32, 0, 16),
                    ore("Ir", true, 64, 0, 8)),
            stoneLayer(
                    "GRANITE_RED",
                    "GraniteRed",
                    false,
                    ore("Lepidolite", true, 32, 10, 46),
                    ore("Spodumene", true, 28, 10, 46)),
            stoneLayer(
                    "GRANITE_RED",
                    "GraniteRed",
                    false,
                    ore("Wolframite", true, 64, 0, 16),
                    ore("Ferberite", true, 64, 0, 16),
                    ore("Huebnerite", true, 64, 0, 16)),
            stoneLayer(
                    "GRANITE_RED",
                    "GraniteRed",
                    false,
                    ore("Pitchblende", true, 32, 0, 18),
                    ore("Uraninite", true, 32, 0, 16),
                    ore("Tantalite", true, 64, 30, 40),
                    ore("Columbite", true, 64, 30, 40),
                    ore("Coltan", true, 16, 20, 50)),
            stoneLayer("GRANITE", "Granite", true).withSurface("S"),
            stoneLayer("GRANITE", "Granite", true).withSurface("KNO3"),
            stoneLayer("GRANITE", "Granite", true).withSurface("Lignite"),
            stoneLayer("GRANITE", "Granite", false, ore("BlueTopaz", true, 64, 8, 32), ore("Topaz", true, 64, 24, 48)),
            stoneLayer(
                    "GRANITE",
                    "Granite",
                    false,
                    ore("Apatite", true, 8, 32, 64),
                    ore("PO4", true, 24, 36, 60),
                    ore("Phosphorite", true, 24, 40, 56),
                    ore("PhosphorusRed", true, 24, 44, 52)),
            stoneLayer("DIORITE", "Diorite", false).weighted(3),
            stoneLayer(
                    "DIORITE",
                    "Diorite",
                    false,
                    ore("Sapphire", true, 64, 24, 48),
                    ore("GreenSapphire", true, 64, 24, 48),
                    ore("YellowSapphire", true, 64, 24, 48),
                    ore("OrangeSapphire", true, 64, 24, 48),
                    ore("BlueSapphire", true, 64, 8, 32),
                    ore("PurpleSapphire", true, 64, 24, 48),
                    ore("Ruby", true, 64, 24, 48)),
            stoneLayer(
                    "DIORITE",
                    "Diorite",
                    false,
                    ore("Garnierite", true, 8, 16, 48),
                    ore("Pentlandite", true, 8, 24, 56),
                    ore("Cobaltite", true, 8, 32, 64),
                    ore("Craponite", true, 64, 24, 48),
                    ore("Amethyst", true, 64, 24, 48)),
            stoneLayer(
                    "GREENSCHIST",
                    "Greenschist",
                    false,
                    ore("Talc", true, 32, 14, 52),
                    ore("Chromite", true, 32, 24, 64)),
            stoneLayer("GREENSCHIST", "Greenschist", false, ore("Cobaltite", true, 48, 12, 48)),
            layer("Stone", false, ore("Andradite", true, 32, 8, 40), ore("Almandine", true, 32, 16, 48)),
            stoneLayer(
                    "BLUESCHIST",
                    "Blueschist",
                    false,
                    ore("Tetrahedrite", true, 8, 40, 80),
                    ore("Cu", true, 16, 40, 80)),
            stoneLayer("BLUESCHIST", "Blueschist", false),
            layer("Stone", false, ore("Spessartine", true, 32, 8, 40), ore("Pyrope", true, 32, 16, 48)),
            stoneLayer("SHALE", "Shale", false).weighted(6),
            stoneLayer("SLATE", "Slate", false, ore("Biotite", true, 24, 10, 58), ore("Mica", true, 24, 10, 58)),
            stoneLayer("SLATE", "Slate", false, ore("Kyanite", true, 32, 4, 42), ore("Vermiculite", true, 36, 26, 64)),
            stoneLayer("SLATE", "Slate", false, ore("Pyrite", true, 16, 0, 30), ore("Arsenopyrite", true, 16, 0, 20)),
            stoneLayer("SLATE", "Slate", false).weighted(7),
            stoneLayer("QUARTZITE", "Quartzite", false, ore("Niter", true, 6, 40, 70))
                    .withSurface("RedSand"),
            stoneLayer("QUARTZITE", "Quartzite", false, ore("Niter", true, 6, 40, 70))
                    .withSurface("Sand")
                    .weighted(2),
            stoneLayer(
                    "QUARTZITE",
                    "Quartzite",
                    false,
                    ore("JasperBlue", true, 64, 32, 64),
                    ore("JasperGreen", true, 64, 32, 64),
                    ore("JasperRainforest", true, 64, 32, 64),
                    ore("JasperOcean", true, 64, 32, 64)),
            stoneLayer(
                    "QUARTZITE",
                    "Quartzite",
                    false,
                    ore("TigerEyeYellow", true, 64, 32, 64),
                    ore("TigerEyeGreen", true, 64, 32, 64),
                    ore("TigerEyeRed", true, 64, 32, 64),
                    ore("TigerEyeBlue", true, 64, 32, 64),
                    ore("TigerEyeBlack", true, 64, 32, 64)),
            stoneLayer(
                    "QUARTZITE",
                    "Quartzite",
                    false,
                    ore("CertusQuartz", true, 16, 16, 48),
                    ore("Barite", true, 32, 0, 32)));

    public static final List<ContactDef> CONTACTS = List.of(
            contact(true, "Coal", "Stone", ore("Amber", true, 4, 30, 70)),
            contact(true, "Lignite", "Stone", ore("Amber", true, 4, 30, 70)),
            contact(true, "Oilshale", "Stone", ore("Amber", true, 4, 30, 70)),
            contact(true, "Komatiite", "Basalt", ore("Perlite", true, 4, 0, 16)),
            contact(true, "Limestone", "Basalt", ore("Ilmenite", true, 8, 0, 32), ore("TiO2", true, 12, 0, 32)),
            contact(true, "Granite", "NaCl", ore("Zeolite", true, 8, 16, 48)),
            contact(true, "Granite", "KCl", ore("Pollucite", true, 8, 16, 48)),
            contact(
                    true,
                    "GraniteBlack",
                    "Marble",
                    ore("Lapis", true, 8, 0, 48),
                    ore("Sodalite", true, 16, 0, 48),
                    ore("Lazurite", true, 16, 0, 48),
                    ore("Pyrite", true, 16, 0, 48)),
            contact(
                    false,
                    "GraniteBlack",
                    "Basalt",
                    ore("Diamond", true, 64, 0, 32),
                    ore("DiamondPink", true, 32, 0, 32),
                    ore("Graphite", true, 8, 0, 32)),
            contact(true, "GraniteBlack", "Granite", ore("Zircon", true, 24, 0, 32)),
            contact(true, "GraniteBlack", "GraniteRed", ore("Zircon", true, 24, 0, 32)));
}
