package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.HeightCoordinates;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import java.util.List;
import java.util.Optional;

/**
 * Ore generation catalogs based on GT6 {@code gregtech/loaders/b/Loader_Worldgen.java}
 * (the {@code ORE_OVERWORLD} large veins and {@code GEN_OVERWORLD} small ores).
 *
 * <p>Large veins retain the top, bottom, in-between and dispersed ores from the original
 * definitions. {@link OreVeinLayout} plans their shape and placement.
 *
 * <p>Small ores ({@code WorldgenOresSmall}): individual blocks scattered in every chunk,
 * roughly {@link SmallOre#amount} per chunk within the Y range.
 */
public final class GTOreVeins {
    public static final int SURFACE_MARGIN = 2;
    private static final int COASTAL_RESERVE_MIN_Y = 16;
    private static final int COASTAL_RESERVE_DEPTH = 28;

    private GTOreVeins() {}

    /** A large vein with either legacy GT6 or modern Y coordinates. */
    public record OreVein(
            String name,
            int minY,
            int maxY,
            int weight,
            int density,
            int size,
            GTMaterial top,
            GTMaterial bottom,
            GTMaterial between,
            GTMaterial spread,
            HeightCoordinates coordinates,
            boolean subregional) {
        public OreVein(
                String name,
                int minY,
                int maxY,
                int weight,
                int density,
                int size,
                GTMaterial top,
                GTMaterial bottom,
                GTMaterial between,
                GTMaterial spread) {
            this(name, minY, maxY, weight, density, size, top, bottom, between, spread, HeightCoordinates.LEGACY);
        }

        public OreVein(
                String name,
                int minY,
                int maxY,
                int weight,
                int density,
                int size,
                GTMaterial top,
                GTMaterial bottom,
                GTMaterial between,
                GTMaterial spread,
                HeightCoordinates coordinates) {
            this(name, minY, maxY, weight, density, size, top, bottom, between, spread, coordinates, false);
        }

        public HeightRange heights(String dimension) {
            if (coordinates == HeightCoordinates.ABSOLUTE) return new HeightRange(minY, maxY);
            return new HeightRange(OreVeinLayout.height(dimension, minY), OreVeinLayout.height(dimension, maxY));
        }
    }

    public record SmallOre(
            String name, int minY, int maxY, int amount, GTMaterial material, Distribution distribution) {
        public SmallOre(String name, int minY, int maxY, int amount, GTMaterial material) {
            this(name, minY, maxY, amount, material, Distribution.UNIFORM);
        }

        public HeightRange heights(String dimension, int terrainMinY, int terrainMaxY) {
            int min = distribution == Distribution.LEGACY ? OreVeinLayout.height(dimension, minY) : minY;
            int max = distribution == Distribution.LEGACY ? OreVeinLayout.height(dimension, maxY) : maxY;
            return new HeightRange(Math.max(min, terrainMinY), Math.min(max, terrainMaxY));
        }
    }

    public record HeightRange(int min, int max) {
        public boolean isEmpty() {
            return min > max;
        }

        public HeightRange intersect(HeightRange other) {
            return new HeightRange(Math.max(min, other.min), Math.min(max, other.max));
        }
    }

    public enum Distribution {
        UNIFORM,
        TRIANGLE,
        LEGACY;

        public int sample(int minY, int maxY, java.util.function.IntUnaryOperator random) {
            int span = maxY - minY;
            if (this == TRIANGLE) {
                int half = span / 2;
                return minY + random.applyAsInt(half + 1) + random.applyAsInt(span - half + 1);
            }
            return minY + random.applyAsInt(span + 1);
        }
    }

    /**
     * GT6 large veins flagged {@code ORE_OVERWORLD}, followed by additional Overworld sources.
     *
     * <p>Use {@link #activeOverworldVeins()} for generation and recipe-browser information.
     */
    public static final List<OreVein> OVERWORLD_VEINS = List.of(
            //          name             minY maxY weight den size  top                          bottom
            //      between                      spread
            new OreVein(
                    "lignite",
                    50,
                    130,
                    160,
                    8,
                    32,
                    Materials.Lignite,
                    Materials.Lignite,
                    Materials.Lignite,
                    Materials.Coal),
            new OreVein("coal", 50, 80, 80, 6, 32, Materials.Coal, Materials.Coal, Materials.Coal, Materials.Lignite),
            new OreVein(
                    "apatite",
                    40,
                    60,
                    60,
                    3,
                    16,
                    Materials.Apatite,
                    Materials.Apatite,
                    Materials.BluePhosphorus,
                    Materials.Phosphate),
            new OreVein(
                    "lapis",
                    20,
                    50,
                    40,
                    5,
                    16,
                    Materials.Lazurite,
                    Materials.Sodalite,
                    Materials.Lapis,
                    Materials.Azurite),
            new OreVein(
                    "bauxite",
                    50,
                    90,
                    80,
                    4,
                    24,
                    gregtech.content.material.generated.OreMaterials.Bauxite,
                    gregtech.content.material.generated.OreMaterials.Bauxite,
                    gregtech.content.material.generated.OreMaterials.Bauxite,
                    gregtech.content.material.generated.OreMaterials.Ilmenite),
            new OreVein(
                    "iodinesalt",
                    50,
                    60,
                    30,
                    3,
                    24,
                    Materials.IodineSalt,
                    Materials.Salt,
                    gregtech.content.material.generated.OreMaterials.Borax,
                    gregtech.content.material.generated.OreMaterials.Zeolite),
            new OreVein(
                    "rocksalt",
                    50,
                    60,
                    30,
                    3,
                    24,
                    Materials.Sylvite,
                    gregtech.content.material.generated.OreMaterials.Coltan,
                    gregtech.content.material.generated.OreMaterials.Lepidolite,
                    gregtech.content.material.generated.OreMaterials.Spodumene),
            new OreVein(
                    "asbestos",
                    10,
                    40,
                    30,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Chromite,
                    Materials.Talc,
                    Materials.Gypsum,
                    Materials.Asbestos),
            new OreVein(
                    "sapphire",
                    10,
                    40,
                    30,
                    3,
                    16,
                    Materials.BlueSapphire,
                    Materials.OrangeSapphire,
                    Materials.YellowSapphire,
                    Materials.Ruby),
            new OreVein(
                    "sapphire2",
                    10,
                    40,
                    30,
                    3,
                    16,
                    Materials.GreenSapphire,
                    Materials.Ruby,
                    Materials.BlueSapphire,
                    Materials.PurpleSapphire),
            new OreVein(
                    "garnet",
                    10,
                    40,
                    60,
                    3,
                    16,
                    Materials.Almandine,
                    Materials.Pyrope,
                    Materials.Andradite,
                    Materials.Uvarovite),
            new OreVein(
                    "pitchblende",
                    10,
                    40,
                    40,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Pitchblende,
                    gregtech.content.material.generated.OreMaterials.Pitchblende,
                    gregtech.content.material.generated.OreMaterials.Uraninite,
                    gregtech.content.material.generated.OreMaterials.Uraninite),
            new OreVein(
                    "monazite",
                    20,
                    40,
                    30,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Bastnasite,
                    gregtech.content.material.generated.OreMaterials.Bastnasite,
                    Materials.Monazite,
                    Materials.Neodymium),
            new OreVein(
                    "diamond",
                    5,
                    20,
                    40,
                    2,
                    16,
                    Materials.Graphite,
                    Materials.Graphite,
                    Materials.Diamond,
                    Materials.Graphite),
            new OreVein(
                    "galena",
                    30,
                    60,
                    40,
                    5,
                    16,
                    gregtech.content.material.generated.OreMaterials.Galena,
                    gregtech.content.material.generated.OreMaterials.Galena,
                    Materials.Silver,
                    Materials.Lead),
            new OreVein(
                    "quartz",
                    40,
                    80,
                    60,
                    3,
                    16,
                    Materials.MilkyQuartz,
                    gregtech.content.material.generated.OreMaterials.Barite,
                    Materials.CertusQuartz,
                    Materials.CertusQuartz),
            new OreVein(
                    "peridot",
                    10,
                    40,
                    60,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Kyanite,
                    Materials.MagnesiumCarbonate,
                    Materials.Peridot,
                    gregtech.content.material.generated.OreMaterials.Glauconite),
            new OreVein(
                    "gold",
                    20,
                    30,
                    5,
                    3,
                    16,
                    Materials.Pyrite,
                    gregtech.content.material.generated.OreMaterials.Chalcopyrite,
                    gregtech.content.material.generated.OreMaterials.Arsenopyrite,
                    Materials.Gold),
            new OreVein(
                    "platinum",
                    40,
                    50,
                    5,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Cooperite,
                    Materials.Palladium,
                    gregtech.content.material.generated.OreMaterials.Sperrylite,
                    Materials.Iridium),
            new OreVein(
                    "molybdenum",
                    20,
                    50,
                    5,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Wulfenite,
                    gregtech.content.material.generated.OreMaterials.Molybdenite,
                    Materials.Molybdenum,
                    gregtech.content.material.generated.OreMaterials.Powellite),
            new OreVein(
                    "cassiterite",
                    40,
                    90,
                    170,
                    5,
                    24,
                    gregtech.content.material.generated.OreMaterials.Stannite,
                    gregtech.content.material.generated.OreMaterials.Kesterite,
                    gregtech.content.material.generated.OreMaterials.Huebnerite,
                    gregtech.content.material.generated.OreMaterials.Cassiterite),
            new OreVein(
                    "tungstate",
                    20,
                    50,
                    10,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Scheelite,
                    gregtech.content.material.generated.OreMaterials.Russellite,
                    gregtech.content.material.generated.OreMaterials.Tungstate,
                    gregtech.content.material.generated.OreMaterials.Pinalite),
            new OreVein(
                    "manganese",
                    20,
                    30,
                    20,
                    3,
                    16,
                    Materials.Grossular,
                    Materials.Spessartine,
                    Materials.Pyrolusite,
                    gregtech.content.material.generated.OreMaterials.Coltan),
            new OreVein(
                    "beryllium",
                    5,
                    30,
                    15,
                    3,
                    16,
                    Materials.Aquamarine,
                    Materials.Maxixe,
                    Materials.Emerald,
                    Materials.Thorium),
            new OreVein(
                    "beryllium2",
                    5,
                    30,
                    15,
                    3,
                    16,
                    Materials.Bixbite,
                    Materials.Goshenite,
                    Materials.Heliodor,
                    Materials.Morganite),
            new OreVein(
                    "titanium",
                    10,
                    40,
                    40,
                    3,
                    16,
                    Materials.Rutile,
                    Materials.Rutile,
                    Materials.Zircon,
                    gregtech.content.material.generated.OreMaterials.Ilmenite,
                    HeightCoordinates.LEGACY,
                    true),
            new OreVein(
                    "nickel",
                    10,
                    40,
                    40,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Garnierite,
                    Materials.Nickel,
                    gregtech.content.material.generated.OreMaterials.Cobaltite,
                    gregtech.content.material.generated.OreMaterials.Pentlandite),
            new OreVein(
                    "redstone",
                    10,
                    40,
                    60,
                    3,
                    24,
                    Materials.Redstone,
                    Materials.Redstone,
                    Materials.Ruby,
                    gregtech.content.material.generated.OreMaterials.Cinnabar),
            new OreVein(
                    "tetrahedrite",
                    70,
                    120,
                    150,
                    4,
                    24,
                    gregtech.content.material.generated.OreMaterials.Tetrahedrite,
                    gregtech.content.material.generated.OreMaterials.Tetrahedrite,
                    Materials.Copper,
                    gregtech.content.material.generated.OreMaterials.Stibnite),
            new OreVein(
                    "iron",
                    10,
                    40,
                    120,
                    4,
                    24,
                    gregtech.content.material.generated.OreMaterials.BrownLimonite,
                    gregtech.content.material.generated.OreMaterials.YellowLimonite,
                    Materials.Hematite,
                    gregtech.content.material.generated.OreMaterials.Malachite),
            new OreVein(
                    "copper",
                    10,
                    30,
                    80,
                    4,
                    24,
                    gregtech.content.material.generated.OreMaterials.Chalcopyrite,
                    Materials.Hematite,
                    Materials.Pyrite,
                    Materials.Copper),
            new OreVein(
                    "tantalum",
                    8,
                    28,
                    8,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Tantalite,
                    gregtech.content.material.generated.OreMaterials.Columbite,
                    gregtech.content.material.generated.OreMaterials.Coltan,
                    gregtech.content.material.generated.OreMaterials.Tantalite),
            new OreVein(
                    "gt6ce:chromite",
                    -38,
                    18,
                    0,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Chromite,
                    gregtech.content.material.generated.OreMaterials.Chromite,
                    gregtech.content.material.generated.OreMaterials.Chromite,
                    Materials.Uvarovite,
                    HeightCoordinates.ABSOLUTE,
                    true));

    static boolean overworldVeinEnabled(OreVein vein) {
        return !GTWorldgenDefaults.STONE_LAYERS
                || !vein.name().equals("coal") && !vein.name().equals("lignite");
    }

    /** Coal and lignite retain their seam sources when stone layers are active. */
    public static List<OreVein> activeOverworldVeins() {
        return activeVeins(OreVeinDeclaration.OVERWORLD);
    }

    /** Built-in large veins of {@code dimension}, then its addon veins. Other dimensions are addon-only. */
    public static List<OreVein> activeVeins(String dimension) {
        return OreSourceCatalog.current().veins(dimension);
    }

    /** A lower shelf for the built-in shallow bands when a region has no ground in the original band. */
    public static Optional<HeightRange> reserveHeights(String dimension, String name, HeightRange terrain) {
        if (!OreVeinDeclaration.OVERWORLD.equals(dimension)) return Optional.empty();
        var beneathSurface = new HeightRange(terrain.min(), terrain.max() - SURFACE_MARGIN);
        return OVERWORLD_VEINS.stream()
                .filter(vein -> vein.name().equals(name) && overworldVeinEnabled(vein))
                .map(vein -> vein.heights(dimension))
                .filter(heights -> heights.min() >= MineralWorldgenRules.HEIGHT_ANCHOR_Y)
                .map(heights -> new HeightRange(COASTAL_RESERVE_MIN_Y, heights.min() - 1).intersect(beneathSurface))
                .filter(heights -> !heights.isEmpty())
                .findFirst();
    }

    public static HeightRange reservePlacement(HeightRange reserve, int surfaceY) {
        int top = Math.min(reserve.max(), surfaceY - SURFACE_MARGIN);
        return new HeightRange(Math.max(reserve.min(), top - COASTAL_RESERVE_DEPTH), top);
    }

    public static int selectionWeight(String dimension, List<OreVein> veins) {
        if (OreVeinDeclaration.END.equals(dimension)) return endSelectionWeight(veins);
        return veins.stream().mapToInt(OreVein::weight).sum();
    }

    /** Built-in small ores of {@code dimension}, then its addon small ores. Other dimensions are addon-only. */
    public static List<SmallOre> activeSmallOres(String dimension) {
        return OreSourceCatalog.current().smallOres(dimension);
    }

    /**
     * All GT6 small ores flagged {@code GEN_OVERWORLD} (Loader_Worldgen).
     * Mod-compat entries (Thaumcraft/AE2/IHL/HEX/... gated ores when the other mod is absent) are
     * omitted; added the GT6 {@code ore.small.eudialyte} entry, which was skipped before because
     * the port had no Eudialyte material yet ( added it).
     */
    public static final List<SmallOre> OVERWORLD_SMALL_ORES = List.of(
            //           name            minY maxY amt  material
            new SmallOre("copper", 60, 120, 16, Materials.Copper),
            new SmallOre("chalcopyrite", 60, 120, 16, gregtech.content.material.generated.OreMaterials.Chalcopyrite),
            new SmallOre("malachite", 40, 70, 8, gregtech.content.material.generated.OreMaterials.Malachite),
            new SmallOre("tin", 60, 120, 16, Materials.Tin),
            new SmallOre("cassiterite", 60, 120, 16, gregtech.content.material.generated.OreMaterials.Cassiterite),
            new SmallOre("zinc", 40, 70, 4, Materials.Zinc),
            new SmallOre("sphalerite", 30, 60, 12, gregtech.content.material.generated.OreMaterials.Sphalerite),
            new SmallOre("smithsonite", 30, 60, 2, gregtech.content.material.generated.OreMaterials.Smithsonite),
            new SmallOre("stibnite", 20, 40, 2, gregtech.content.material.generated.OreMaterials.Stibnite),
            new SmallOre("bismuth", 80, 120, 8, Materials.Bismuth),
            new SmallOre("lead", 40, 80, 16, Materials.Lead),
            new SmallOre("galena", 40, 80, 16, gregtech.content.material.generated.OreMaterials.Galena),
            new SmallOre("silver", -40, 42, 4, Materials.Silver, Distribution.TRIANGLE),
            new SmallOre("gold", -60, 30, 4, Materials.Gold, Distribution.TRIANGLE),
            new SmallOre("pyrite", -56, 48, 4, Materials.Pyrite, Distribution.TRIANGLE),
            new SmallOre("hematite", 40, 80, 24, Materials.Hematite),
            new SmallOre("pyrolusite", -36, 46, 4, Materials.Pyrolusite, Distribution.TRIANGLE),
            new SmallOre(
                    "garnierite",
                    -38,
                    44,
                    4,
                    gregtech.content.material.generated.OreMaterials.Garnierite,
                    Distribution.TRIANGLE),
            new SmallOre(
                    "pentlandite",
                    -56,
                    36,
                    4,
                    gregtech.content.material.generated.OreMaterials.Pentlandite,
                    Distribution.TRIANGLE),
            new SmallOre(
                    "scheelite",
                    -64,
                    16,
                    1,
                    gregtech.content.material.generated.OreMaterials.Scheelite,
                    Distribution.TRIANGLE),
            new SmallOre(
                    "tungstate",
                    -56,
                    8,
                    1,
                    gregtech.content.material.generated.OreMaterials.Tungstate,
                    Distribution.TRIANGLE),
            new SmallOre(
                    "uraninite",
                    -64,
                    -20,
                    1,
                    gregtech.content.material.generated.OreMaterials.Uraninite,
                    Distribution.TRIANGLE),
            new SmallOre(
                    "tantalite",
                    -48,
                    -10,
                    1,
                    gregtech.content.material.generated.OreMaterials.Tantalite,
                    Distribution.TRIANGLE),
            new SmallOre(
                    "chromite",
                    -64,
                    4,
                    2,
                    gregtech.content.material.generated.OreMaterials.Chromite,
                    Distribution.TRIANGLE),
            new SmallOre("salt", 40, 80, 6, Materials.Salt),
            new SmallOre("rocksalt", 40, 80, 6, Materials.Sylvite),
            new SmallOre("borax", -32, 44, 4, gregtech.content.material.generated.OreMaterials.Borax),
            new SmallOre("asbestos", -30, 46, 8, Materials.Asbestos),
            new SmallOre("diamond", -64, -24, 2, Materials.Diamond, Distribution.TRIANGLE),
            new SmallOre("amber", -20, 70, 1, Materials.Amber),
            new SmallOre("craponite", -44, 250, 2, Materials.Craponite),
            new SmallOre("redstone", -64, 10, 16, Materials.Redstone, Distribution.TRIANGLE),
            new SmallOre("lapis", -38, 42, 8, Materials.Lapis, Distribution.TRIANGLE),
            new SmallOre("eudialyte", -44, 40, 4, gregtech.content.material.generated.CompoundMaterials.Eudialyte),
            new SmallOre("azurite", -18, 42, 4, Materials.Azurite),
            new SmallOre("coal", 40, 100, 36, Materials.Coal),
            new SmallOre("graphite", -64, -20, 2, Materials.Graphite, Distribution.TRIANGLE),
            new SmallOre("pollucite", -55, 250, 1, gregtech.content.material.generated.OreMaterials.Pollucite),
            new SmallOre("zeolite", -52, 250, 1, gregtech.content.material.generated.OreMaterials.Zeolite),
            new SmallOre("sulfur", -60, -8, 8, Materials.Sulfur));

    // ── The End (GT6 ORE_END large veins; platinum/molybdenum/cassiterite are shared with the
    //    overworld, naquadah/trinium are End-exclusive) ──

    /**
     * GT6's {@code ORE_END} veins in registration order, plus the three shared ones
     * (platinum, molybdenum, cassiterite). End cassiterite and trinium weights are reduced
     * to 17/290 and 10/290; the removed weight is reserved for empty grid cells.
     */
    public static final List<OreVein> END_VEINS = List.of(
            new OreVein(
                    "platinum",
                    40,
                    50,
                    5,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Cooperite,
                    Materials.Palladium,
                    gregtech.content.material.generated.OreMaterials.Sperrylite,
                    Materials.Iridium),
            new OreVein(
                    "molybdenum",
                    20,
                    50,
                    5,
                    3,
                    16,
                    gregtech.content.material.generated.OreMaterials.Wulfenite,
                    gregtech.content.material.generated.OreMaterials.Molybdenite,
                    Materials.Molybdenum,
                    gregtech.content.material.generated.OreMaterials.Powellite),
            new OreVein(
                    "cassiterite",
                    40,
                    90,
                    17,
                    5,
                    24,
                    gregtech.content.material.generated.OreMaterials.Stannite,
                    gregtech.content.material.generated.OreMaterials.Kesterite,
                    gregtech.content.material.generated.OreMaterials.Huebnerite,
                    gregtech.content.material.generated.OreMaterials.Cassiterite),
            new OreVein(
                    "naquadah",
                    10,
                    60,
                    10,
                    4,
                    32,
                    Materials.Naquadah,
                    Materials.Naquadah,
                    Materials.Naquadah,
                    Materials.Naquadah),
            new OreVein(
                    "trinium",
                    10,
                    90,
                    10,
                    1,
                    12,
                    Materials.Trinium,
                    Materials.Trinium,
                    Materials.Trinium,
                    Materials.Trinium));
    /** Reduced End vein weights remain empty rolls instead of increasing the other veins. */
    public static final int END_EMPTY_VEIN_WEIGHT = (170 - 17) + (100 - 10);

    public static final int TOTAL_END_VEIN_WEIGHT = endSelectionWeight(END_VEINS);

    /** Shared by world generation and both recipe viewers, including addon weights. */
    public static int endSelectionWeight(List<OreVein> veins) {
        int total = END_EMPTY_VEIN_WEIGHT;
        for (OreVein vein : veins) total = Math.addExact(total, vein.weight());
        return total;
    }

    /** GT6 small ores flagged {@code GEN_NETHER}. */
    public static final List<SmallOre> NETHER_SMALL_ORES = List.of(
            new SmallOre("cassiterite", 60, 120, 16, gregtech.content.material.generated.OreMaterials.Cassiterite),
            new SmallOre("bismuth", 80, 120, 8, Materials.Bismuth),
            new SmallOre("gold", 20, 40, 4, Materials.Gold),
            new SmallOre("scheelite", 5, 50, 1, gregtech.content.material.generated.OreMaterials.Scheelite),
            new SmallOre("salt", 40, 80, 6, Materials.Salt),
            new SmallOre("rocksalt", 40, 80, 6, Materials.Sylvite),
            new SmallOre("borax", 10, 40, 4, gregtech.content.material.generated.OreMaterials.Borax),
            new SmallOre("asbestos", 20, 40, 8, Materials.Asbestos),
            new SmallOre("diamond", 5, 10, 2, Materials.Diamond),
            new SmallOre("craponite", 5, 250, 2, Materials.Craponite),
            new SmallOre("redstone", 5, 20, 16, Materials.Redstone),
            new SmallOre("graphite", 5, 10, 2, Materials.Graphite),
            new SmallOre("pollucite", 1, 250, 1, gregtech.content.material.generated.OreMaterials.Pollucite),
            new SmallOre("zeolite", 1, 250, 1, gregtech.content.material.generated.OreMaterials.Zeolite),
            new SmallOre("coltan", 1, 250, 4, gregtech.content.material.generated.OreMaterials.Coltan),
            new SmallOre("niter", 10, 120, 32, Materials.Niter),
            new SmallOre("efrine", 90, 120, 8, Materials.Efrine),
            new SmallOre("cinnabar", 5, 250, 16, gregtech.content.material.generated.OreMaterials.Cinnabar));

    /** GT6 End small ores, with the shallow copper and tin group adapted to island terrain. */
    public static final List<SmallOre> END_SMALL_ORES = List.of(
            new SmallOre("copper", 48, 72, 8, Materials.Copper),
            new SmallOre("chalcopyrite", 48, 72, 8, gregtech.content.material.generated.OreMaterials.Chalcopyrite),
            new SmallOre("malachite", 40, 70, 8, gregtech.content.material.generated.OreMaterials.Malachite),
            new SmallOre("tin", 48, 72, 8, Materials.Tin),
            new SmallOre("cassiterite", 48, 72, 8, gregtech.content.material.generated.OreMaterials.Cassiterite),
            new SmallOre("zinc", 40, 70, 4, Materials.Zinc),
            new SmallOre("sphalerite", 30, 60, 12, gregtech.content.material.generated.OreMaterials.Sphalerite),
            new SmallOre("smithsonite", 30, 60, 2, gregtech.content.material.generated.OreMaterials.Smithsonite),
            new SmallOre("stibnite", 20, 40, 2, gregtech.content.material.generated.OreMaterials.Stibnite),
            new SmallOre("lead", 40, 80, 16, Materials.Lead),
            new SmallOre("galena", 40, 80, 16, gregtech.content.material.generated.OreMaterials.Galena),
            new SmallOre("silver", 20, 40, 4, Materials.Silver),
            new SmallOre("gold", 20, 40, 4, Materials.Gold),
            new SmallOre("pyrite", 20, 40, 4, Materials.Pyrite),
            new SmallOre("hematite", 40, 80, 24, Materials.Hematite),
            new SmallOre("pyrolusite", 20, 40, 4, Materials.Pyrolusite),
            new SmallOre("garnierite", 20, 40, 4, gregtech.content.material.generated.OreMaterials.Garnierite),
            new SmallOre("pentlandite", 20, 40, 4, gregtech.content.material.generated.OreMaterials.Pentlandite),
            new SmallOre("scheelite", 5, 50, 1, gregtech.content.material.generated.OreMaterials.Scheelite),
            new SmallOre("salt", 40, 80, 6, Materials.Salt),
            new SmallOre("rocksalt", 40, 80, 6, Materials.Sylvite),
            new SmallOre("borax", 10, 40, 4, gregtech.content.material.generated.OreMaterials.Borax),
            new SmallOre("craponite", 5, 250, 2, Materials.Craponite),
            new SmallOre("coltan", 1, 250, 4, gregtech.content.material.generated.OreMaterials.Coltan),
            new SmallOre("platinum", 20, 40, 6, Materials.Platinum),
            new SmallOre("iridium", 20, 40, 6, Materials.Iridium),
            new SmallOre("sperrylite", 20, 40, 4, gregtech.content.material.generated.OreMaterials.Sperrylite),
            new SmallOre("cooperite", 20, 40, 4, gregtech.content.material.generated.OreMaterials.Cooperite),
            new SmallOre("naquadah", 10, 80, 6, Materials.Naquadah),
            new SmallOre("trinium", 10, 80, 12, Materials.Trinium));
}
