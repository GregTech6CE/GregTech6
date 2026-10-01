package com.gregtech.gregtech.block;
import java.util.List;
import java.util.Map;
/** Original GT6 dense/vanilla ore descriptors; platform blocks only construct and read these data. */
public final class SpecialOreDefinitions {
    public record RockSpec(String material, int harvestLevel, float hardnessMultiplier, boolean flammable) {}
    public record VanillaSpec(String icon, String material, int harvestLevel, float hardnessMultiplier,
                              int burnLevel, int minXp, int maxXp) {}
    private static final Map<String, RockSpec> ROCKS = Map.ofEntries(
            Map.entry("ore_anthracite", new RockSpec("Coal", 0, 0.5F, true)),
            Map.entry("ore_lignite", new RockSpec("Lignite", 0, 0.5F, true)),
            Map.entry("ore_salt", new RockSpec("NaCl", 1, 1.0F, false)),
            Map.entry("ore_rocksalt", new RockSpec("KCl", 1, 1.0F, false)),
            Map.entry("ore_bauxite", new RockSpec("Bauxite", 2, 2.0F, false)),
            Map.entry("ore_oil", new RockSpec("Oilshale", 1, 0.5F, true)),
            Map.entry("ore_gypsum", new RockSpec("Gypsum", 0, 0.5F, false)),
            Map.entry("ore_milkyquartz", new RockSpec("MilkyQuartz", 1, 1.0F, false)),
            Map.entry("ore_netherquartz", new RockSpec("NetherQuartz", 1, 1.0F, false)));
    private static final List<VanillaSpec> VANILLA = List.of(
            new VanillaSpec("ore_sulfur", "Sulfur", 0, 0.5F, 30, 0, 2),
            new VanillaSpec("ore_apatite", "Apatite", 0, 0.5F, 30, 0, 2),
            new VanillaSpec("ore_ruby", "Ruby", 2, 1.5F, 0, 3, 7),
            new VanillaSpec("ore_amber", "Amber", 1, 1.0F, 0, 3, 7),
            new VanillaSpec("ore_amethyst", "Amethyst", 2, 1.0F, 0, 3, 7),
            new VanillaSpec("ore_galena", "Galena", 1, 1.0F, 0, 2, 5),
            new VanillaSpec("ore_tetrahedrite", "Tetrahedrite", 1, 1.0F, 0, 2, 5),
            new VanillaSpec("ore_cassiterite", "Cassiterite", 1, 1.0F, 0, 2, 5),
            new VanillaSpec("ore_sheldonite", "Cooperite", 2, 1.5F, 0, 2, 5),
            new VanillaSpec("ore_pentlandite", "Pentlandite", 1, 1.0F, 0, 2, 5),
            new VanillaSpec("ore_scheelite", "Scheelite", 2, 1.5F, 0, 2, 5),
            new VanillaSpec("ore_rutile", "Rutile", 2, 1.5F, 0, 2, 5),
            new VanillaSpec("ore_bastnasite", "Bastnasite", 2, 1.5F, 0, 2, 5),
            new VanillaSpec("ore_graphite", "Graphite", 0, 0.5F, 30, 2, 5),
            new VanillaSpec("ore_pitchblende", "Pitchblende", 3, 2.0F, 0, 2, 5),
            new VanillaSpec("ore_borax", "Borax", 0, 0.5F, 0, 2, 5));
    public static final List<String> ORDERED_ICONS = List.of("crystal_ore_arsenopyrite", "crystal_ore_chalcopyrite", "crystal_ore_cinnabar", "crystal_ore_cobaltite", "crystal_ore_galena", "crystal_ore_kesterite", "crystal_ore_molybdenite", "crystal_ore_pyrite", "crystal_ore_sphalerite", "crystal_ore_stannite", "crystal_ore_stibnite", "crystal_ore_tetrahedrite", "ore_amber", "ore_amethyst", "ore_anthracite", "ore_apatite", "ore_bastnasite", "ore_bauxite", "ore_borax", "ore_cassiterite", "ore_galena", "ore_graphite", "ore_gypsum", "ore_lignite", "ore_milkyquartz", "ore_netherquartz", "ore_oil", "ore_pentlandite", "ore_pitchblende", "ore_rocksalt", "ore_ruby", "ore_rutile", "ore_salt", "ore_scheelite", "ore_sheldonite", "ore_sulfur", "ore_tetrahedrite");
    private SpecialOreDefinitions() {}
    public static Map<String, RockSpec> rocks() { return ROCKS; }
    public static List<VanillaSpec> vanilla() { return VANILLA; }
}
