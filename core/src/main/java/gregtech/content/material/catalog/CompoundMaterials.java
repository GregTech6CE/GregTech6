package gregtech.content.material.catalog;

import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialFacts;
import java.util.ArrayList;
import java.util.List;

/** Canonical declarations migrated from the GT6 MT/TD import and the port's explicit corrections.
 * Historical extraction tools are unavailable; these declarations are maintained directly.
 * Intrinsic facts only: recipe sources and world-generation remain owned by their providers. */
public final class CompoundMaterials {
    private CompoundMaterials() {}

    public static List<MaterialDefinition> definitions() {
        var result = new ArrayList<MaterialDefinition>();
        part0(result);
        part1(result);
        part2(result);
        part3(result);
        part4(result);
        part5(result);
        part6(result);
        part7(result);
        part8(result);
        part9(result);
        part10(result);
        part11(result);
        part12(result);
        part13(result);
        part14(result);
        part15(result);
        part16(result);
        part17(result);
        part18(result);
        part19(result);
        part20(result);
        part21(result);
        part22(result);
        part23(result);
        part24(result);
        part25(result);
        part26(result);
        return List.copyOf(result);
    }

    private static void part0(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8784, "Adamantine")
                .displayName("Adamantine")
                .color(0xFF0040)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(5225)
                .boilingPointKelvin(14528)
                .density(5.7249227F)
                .chemicalFormula("Ad₃O₄")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Adamantine")
                        .source("Metallurgy", "Metallurgy")
                        .tools(3, 10.0F, 4500L, 5)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Adamantium", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("GraniticMineralSand")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9830, "Air")
                .displayName("Air")
                .color(0xA9D0F5)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.0012F)
                .chemicalFormula("N₄₀O₁₁Ar")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID, MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Air")
                        .composition(
                                52L,
                                new MaterialFacts.Part("Nitrogen", 25945920000L),
                                new MaterialFacts.Part("Oxygen", 7135128000L),
                                new MaterialFacts.Part("Argon", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9101, "Almandine")
                .displayName("Almandine")
                .color(0xFF0000)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1759)
                .boilingPointKelvin(2745)
                .density(1.8011974F)
                .chemicalFormula("(Al₂O₃)Fe₃(SiO₂)₃O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("GarnetRed")
                        .correctedToolStats()
                        .tools(3, 7.0F, 128L, 2)
                        .smelting("Almandine", 0L)
                        .composition(
                                20L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Iron", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 5837832000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Grossular", "Pyrope", "Spessartine", "Andradite", "Uvarovite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8622, "AluminiumAlloy")
                .displayName("Aluminium Alloy")
                .color(0xC8C8B4)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(949)
                .boilingPointKelvin(2808)
                .density(2.749769F)
                .chemicalFormula("Al₄₅Si")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Aluminium Alloy")
                        .source("RotaryCraft", "RotaryCraft")
                        .correctedToolStats()
                        .tools(2, 10.0F, 128L, 2)
                        .composition(
                                45L,
                                new MaterialFacts.Part("Aluminium", 29189160000L),
                                new MaterialFacts.Part("Silicon", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8700, "AluminiumBrass")
                .displayName("Aluminium Brass")
                .color(0xDCDC82)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1039)
                .boilingPointKelvin(2802)
                .density(4.2635F)
                .chemicalFormula("Al₃Cu")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("AluminumBrass")
                        .source("TConstruct", "Tinkers Construct")
                        .tools(2, 6.0F, 64L, 2)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Aluminium", 1945944000L),
                                new MaterialFacts.Part("Copper", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8010, "AluminiumFluoride")
                .displayName("Aluminium Fluoride")
                .color(0xC8BEBE)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1560)
                .boilingPointKelvin(3120)
                .density(0.675772F)
                .chemicalFormula("AlF₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Aluminium Fluoride")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 1945944000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("AlF3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8702, "Alumite")
                .displayName("Alumite")
                .color(0xE664E6)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1565)
                .boilingPointKelvin(3780)
                .density(2.7961419F)
                .chemicalFormula("(Al₂O₃)(Fe)₂(MgFe(SiO₂)₂O₄)₁₈")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Alumite")
                        .source("TConstruct", "Tinkers Construct")
                        .tools(2, 1.5F, 64L, 3)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("WroughtIron", 1297296000L),
                                new MaterialFacts.Part("Obsidian", 11675664000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8417, "Amazonite")
                .displayName("Amazonite")
                .color(0x47F1AA)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1853)
                .boilingPointKelvin(2937)
                .density(0.81232554F)
                .chemicalFormula("(Al₂O₃)(SiO₂)₆K₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Amazonite")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("Amazonite", 0L)
                        .composition(
                                26L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8310, "Amber")
                .displayName("Amber")
                .color(0xFFB400)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(473)
                .boilingPointKelvin(946)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Amber")
                        .source("Thaumcraft", "Thaumcraft")
                        .tools(3, 4.0F, 256L, 3)
                        .multipliers(2, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8313, "Amethyst")
                .displayName("Amethyst")
                .color(0xA678F1)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1951)
                .boilingPointKelvin(3202)
                .density(2.1967888F)
                .chemicalFormula("(SiO₂)₄Fe")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Amethyst")
                        .source("BiomesOPlenty", "Biomes O' Plenty")
                        .tools(3, 6.0F, 128L, 3)
                        .smelting("Amethyst", 0L)
                        .composition(
                                5L,
                                new MaterialFacts.Part("SiliconDioxide", 2594592000L),
                                new MaterialFacts.Part("Iron", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8025, "Ammonia")
                .displayName("Ammonia")
                .color(0x72DFE8)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(195)
                .boilingPointKelvin(239)
                .density(3.8006E-4F)
                .chemicalFormula("NH₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ammonia")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 1945944000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("NH3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9102, "Andradite")
                .displayName("Andradite")
                .color(0x967800)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1258)
                .boilingPointKelvin(2052)
                .density(1.3686974F)
                .chemicalFormula("Ca₃Fe₂(SiO₂)₃O₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("GarnetYellow")
                        .correctedToolStats()
                        .tools(3, 7.0F, 128L, 2)
                        .smelting("Andradite", 0L)
                        .composition(
                                20L,
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("Iron", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 5837832000L),
                                new MaterialFacts.Part("Oxygen", 3891888000L))
                        .byproducts("Almandine", "Grossular", "Pyrope", "Spessartine", "Uvarovite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8640, "AnnealedCopper")
                .displayName("Annealed Copper")
                .color(0xFF6400)
                .texture(MaterialTextureSet.COPPER)
                .meltingPointKelvin(2800)
                .boilingPointKelvin(2835)
                .density(8.96F)
                .chemicalFormula("Cu")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Annealed Copper")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .correctedToolStats()
                        .tools(2, 4.0F, 64L, 0)
                        .pulverizing("Copper", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Copper", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8209, "Apatite")
                .displayName("Apatite")
                .color(0x78B4FA)
                .texture(MaterialTextureSet.DIAMOND)
                .meltingPointKelvin(771)
                .boilingPointKelvin(1269)
                .density(0.97762704F)
                .chemicalFormula("Ca₅(PO₄)₃Cl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Apatite")
                        .source("Forestry", "Forestry")
                        .composition(
                                9L,
                                new MaterialFacts.Part("Calcium", 3243240000L),
                                new MaterialFacts.Part("Phosphate", 1945944000L),
                                new MaterialFacts.Part("Chlorine", 648648000L))
                        .byproducts("Phosphorite", "BluePhosphorus", "BlueFluorite", "Phosphate")
                        .multipliers(4, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8323, "Aquamarine")
                .displayName("Aquamarine")
                .color(0xC8DCFF)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Aquamarine")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Emerald",
                                "Morganite",
                                "Goshenite",
                                "Bixbite",
                                "Heliodor",
                                "Maxixe")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part1(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9827, "AquaRegia")
                .displayName("Aqua Regia")
                .color(0x40FF40)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(0.5779396F)
                .chemicalFormula("(HNO₃)(HCl)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Aqua Regia")
                        .source("factorization", "Factorization")
                        .composition(
                                13L,
                                new MaterialFacts.Part("NitricAcid", 3243240000L),
                                new MaterialFacts.Part("HydrochloricAcid", 5189184000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8615, "ArsenicBronze")
                .displayName("Arsenic Bronze")
                .color(0xC8C8DE)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1357)
                .boilingPointKelvin(2835)
                .density(7.9886F)
                .chemicalFormula("As(Cu₃Sn)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Arsenic Bronze")
                        .tools(3, 6.0F, 480L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Arsenic", 648648000L),
                                new MaterialFacts.Part("Bronze", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8614, "ArsenicCopper")
                .displayName("Arsenic Copper")
                .color(0xD2A03C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1070)
                .boilingPointKelvin(2835)
                .density(8.164F)
                .chemicalFormula("Cu₃As")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Arsenic Copper")
                        .tools(3, 5.5F, 448L, 2)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("Arsenic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8200, "Ashes")
                .displayName("Ashes")
                .color(0x787878)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Ash")
                        .source("Forestry", "Forestry")
                        .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("Ash")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8266, "Asphalt")
                .displayName("Asphalt")
                .color(0x585863)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Asphalt")
                        .forms("DUSTS", "MOLTEN", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8420, "Azurite")
                .displayName("Azurite")
                .color(0x6DA4F7)
                .texture(MaterialTextureSet.QUARTZ)
                .meltingPointKelvin(382)
                .boilingPointKelvin(754)
                .density(2.2949336F)
                .chemicalFormula("Cu₃(CO₃)₂O(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Azurite")
                        .source("tropicraft", "Tropicraft")
                        .smelting("Copper", 72072000L)
                        .composition(
                                15L,
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("CarbonTrioxide", 5189184000L),
                                new MaterialFacts.Part("Oxygen", 648648000L),
                                new MaterialFacts.Part("Water", 1945944000L))
                        .byproducts("Zircon", "Malachite", "Hafnium")
                        .multipliers(5, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8303, "BalasRuby")
                .displayName("Balas Ruby")
                .color(0xFF6464)
                .texture(MaterialTextureSet.GEM_VERTICAL)
                .meltingPointKelvin(785)
                .boilingPointKelvin(1087)
                .density(2.2919595F)
                .chemicalFormula("Cr₂MgO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("FoolsRuby")
                        .source("Redpower", "Redpower")
                        .tools(3, 7.0F, 384L, 2)
                        .smelting("BalasRuby", 0L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Chromium", 1297296000L),
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Chromite", "Spinel")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8275, "Bark")
                .displayName("Bark")
                .color(0x502800)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Bark")
                        .source("BinnieCore", "Binnie's Mods")
                        .smelting("Ashes", 162162000L)
                        .composition(
                                21L,
                                new MaterialFacts.Part("Carbon", 3891888000L),
                                new MaterialFacts.Part("Water", 9729720000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8247, "Basalz")
                .displayName("Basalz")
                .color(0x645151)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Basalz")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .correctedToolStats()
                        .tools(1, 2.0F, 16L, 1)
                        .forms("DUSTS", "PLANTS", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8668, "BatteryAlloy")
                .displayName("Battery Alloy")
                .color(0x9C7CA0)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(660)
                .boilingPointKelvin(1989)
                .density(10.4106F)
                .chemicalFormula("Pb₄Sb")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Battery Alloy")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Lead", 2594592000L),
                                new MaterialFacts.Part("Antimony", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8599, "Bedrock")
                .displayName("Bedrock")
                .color(0x404040)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(4000)
                .boilingPointKelvin(8000)
                .density(13.356248F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Bedrock")
                        .source("minecraft", "Minecraft")
                        .tools(1, 8.0F, 2048L, 5)
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9153, "Bentonite")
                .displayName("Bentonite")
                .color(0xFFC004)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(1.3363496F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Bentonite")
                        .source("PFAAGeologica", "Per Fabrica Ad Astra")
                        .smelting("Ceramic", 648648000L)
                        .byproducts("ClayRed", "Palygorskite")
                        .forms("DUSTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9841, "BioFuel")
                .displayName("Bio Fuel")
                .color(0xC88000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Bio Fuel")
                        .source("Forestry", "Forestry")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8612, "BismuthBronze")
                .displayName("Bismuth Bronze")
                .color(0x647D7D)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1036)
                .boilingPointKelvin(2635)
                .density(8.7642F)
                .chemicalFormula("Bi(Cu₃Zn)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Bismuth Bronze")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 8.0F, 512L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Bismuth", 648648000L),
                                new MaterialFacts.Part("Brass", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8386, "Bixbite")
                .displayName("Bixbite")
                .color(0xFF5050)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("ScarletEmerald")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Emerald",
                                "Aquamarine",
                                "Morganite",
                                "Goshenite",
                                "Heliodor",
                                "Maxixe")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part2(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8449, "BlackAventurine")
                .displayName("Black Aventurine")
                .color(0x323232)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Black Aventurine")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("BlackAventurine", 0L)
                        .byproducts(
                                "GreenAventurine",
                                "BrownAventurine",
                                "YellowAventurine",
                                "BlueAventurine",
                                "RedAventurine")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("AventurineBlack")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8611, "BlackBronze")
                .displayName("Black Bronze")
                .color(0x64327D)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1328)
                .boilingPointKelvin(2813)
                .density(11.3326F)
                .chemicalFormula("Cu₃(AgAu)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Black Bronze")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 12.0F, 512L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("Electrum", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8434, "BlackEye")
                .displayName("Black Eye")
                .color(0x424442)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("BlackTigerEye")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("BlackEye", 0L)
                        .byproducts("Hematite", "TigerEye", "CatsEye", "DragonEye", "HawksEye", "TigerIron")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("TigerEyeBlack")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8440, "BlackFluorite")
                .displayName("Black Fluorite")
                .color(0x303030)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Black Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("WhiteFluorite", "Fluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteBlack")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8631, "BlackSteel")
                .displayName("Black Steel")
                .color(0x5A5A5A)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1838)
                .boilingPointKelvin(3080)
                .density(8.77332F)
                .chemicalFormula("Ni(Cu₃(AgAu))((Fe))₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Black Steel")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 6.5F, 768L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Nickel", 648648000L),
                                new MaterialFacts.Part("BlackBronze", 648648000L),
                                new MaterialFacts.Part("Steel", 1945944000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8403, "BlackVitriol")
                .displayName("Black Vitriol")
                .color(0x424242)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(4.9705F)
                .chemicalFormula("FeS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Black Vitriol")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8211, "Blaze")
                .displayName("Blaze")
                .color(0xFFC800)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(4000)
                .boilingPointKelvin(8000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Blaze")
                        .source("minecraft", "Minecraft")
                        .correctedToolStats()
                        .tools(1, 2.0F, 16L, 1)
                        .fuel(4800L, "", 0L)
                        .forms("DUSTS", "PLANTS", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8396, "BlazeQuartz")
                .displayName("Blaze Quartz")
                .color(0xFFE6C8)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blaze Quartz")
                        .source("Botania", "Botania")
                        .tools(1, 2.5F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8248, "Blitz")
                .displayName("Blitz")
                .color(0xFADB00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Blitz")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .correctedToolStats()
                        .tools(1, 2.0F, 16L, 1)
                        .forms("DUSTS", "PLANTS", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8210, "Blizz")
                .displayName("Blizz")
                .color(0x21C8EA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Blizz")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .correctedToolStats()
                        .tools(1, 2.0F, 16L, 1)
                        .forms("DUSTS", "PLANTS", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8659, "BlueAlloy")
                .displayName("Blue Alloy")
                .color(0x64B4FF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1400)
                .boilingPointKelvin(2435)
                .density(10.501F)
                .chemicalFormula("Ag(((Al₂O₃)₃(SiO₂)Na₄Cl)₅Cu₃(SiO₂)Ar)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Alloy")
                        .source("Redpower", "Redpower")
                        .composition(
                                1L,
                                new MaterialFacts.Part("Silver", 648648000L),
                                new MaterialFacts.Part("Nikolite", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8450, "BlueAventurine")
                .displayName("Blue Aventurine")
                .color(0x43678A)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Aventurine")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("BlueAventurine", 0L)
                        .byproducts(
                                "GreenAventurine",
                                "BrownAventurine",
                                "YellowAventurine",
                                "BlackAventurine",
                                "RedAventurine")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("AventurineBlue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8464, "BlueDiamond")
                .displayName("Blue Diamond")
                .color(0x1E3CF0)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Diamond")
                        .source("ExtraPlanets", "Extra Planets")
                        .correctedToolStats()
                        .tools(3, 8.0F, 1280L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .byproducts("Graphite", "Diamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("DiamondBlue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8438, "BlueFluorite")
                .displayName("Blue Fluorite")
                .color(0x58ACB4)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("MagentaFluorite", "BlackFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteBlue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8427, "BlueJasper")
                .displayName("Blue Jasper")
                .color(0x3C7C97)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1927)
                .boilingPointKelvin(3191)
                .density(3.1429906F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Jasper")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("BlueJasper", 0L)
                        .byproducts("RedJasper", "OceanJasper", "RainforestJasper", "GreenJasper", "YellowJasper")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("JasperBlue")
                        .build())
                .build());
    }

    private static void part3(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8458, "BluePhosphorus")
                .displayName("Blue Phosphorus")
                .color(0x9BE3E4)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(829)
                .boilingPointKelvin(1374)
                .density(1.0700573F)
                .chemicalFormula("Ca₃(PO₄)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Phosphorus")
                        .source("Forestry", "Forestry")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("Phosphate", 1297296000L))
                        .byproducts(
                                "Phosphorite",
                                "Apatite",
                                "BlueFluorite",
                                "Phosphate",
                                "RedPhosphorus",
                                "WhitePhosphorus")
                        .multipliers(3, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("PhosphorusBlue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8328, "BlueSapphire")
                .displayName("Blue Sapphire")
                .color(0x6464C8)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2256)
                .boilingPointKelvin(3230)
                .density(2.2123811F)
                .chemicalFormula("(Al₂O₃)Fe")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Sapphire")
                        .source("Redpower", "Redpower")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Iron", 648648000L))
                        .byproducts("Alumina", "Ruby", "GreenSapphire", "PurpleSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8632, "BlueSteel")
                .displayName("Blue Steel")
                .color(0x64648C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1717)
                .boilingPointKelvin(2967)
                .density(8.724785F)
                .chemicalFormula("(CuAg₄)(Bi(Cu₃Zn))((Fe))₂(Ni(Cu₃(AgAu))((Fe))₃)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Blue Steel")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 7.0F, 896L, 2)
                        .composition(
                                8L,
                                new MaterialFacts.Part("SterlingSilver", 648648000L),
                                new MaterialFacts.Part("BismuthBronze", 648648000L),
                                new MaterialFacts.Part("Steel", 1297296000L),
                                new MaterialFacts.Part("BlackSteel", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8307, "BlueTopaz")
                .displayName("Blue Topaz")
                .color(0x7B96DC)
                .texture(MaterialTextureSet.GEM_HORIZONTAL)
                .meltingPointKelvin(1431)
                .boilingPointKelvin(2092)
                .density(0.8258567F)
                .chemicalFormula("(Al₂O₃)(SiO₂)F₂(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Blue Topaz")
                        .source("arsmagica2", "Ars Magica")
                        .tools(3, 7.0F, 256L, 3)
                        .smelting("BlueTopaz", 0L)
                        .composition(
                                13L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L),
                                new MaterialFacts.Part("Fluorine", 1297296000L),
                                new MaterialFacts.Part("Water", 1945944000L))
                        .byproducts("Topaz")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8404, "BlueVitriol")
                .displayName("Blue Vitriol")
                .color(0x4242DE)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.838786F)
                .chemicalFormula("CuSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("RomanVitriol")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8620, "Brass")
                .displayName("Brass")
                .color(0xFFB400)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1160)
                .boilingPointKelvin(2835)
                .density(8.5035F)
                .chemicalFormula("Cu₃Zn")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Brass")
                        .source("Redpower", "Redpower")
                        .tools(2, 7.0F, 96L, 1)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("Zinc", 648648000L))
                        .byproducts("Copper", "Zinc", "Arsenic")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8471, "Breeze")
                .displayName("Breeze")
                .color(0xAA9BCB)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Breeze")
                        .source("minecraft", "Minecraft")
                        .correctedToolStats()
                        .tools(1, 2.0F, 16L, 1)
                        .forms("DUSTS", "PLANTS", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8610, "Bronze")
                .displayName("Bronze")
                .color(0xD2823C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1357)
                .boilingPointKelvin(2835)
                .density(8.54175F)
                .chemicalFormula("Cu₃Sn")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Bronze")
                        .source("IC2", "IndustrialCraft 2")
                        .tools(3, 5.5F, 448L, 2)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("Tin", 648648000L))
                        .byproducts("Copper", "Tin", "Arsenic")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8446, "BrownAventurine")
                .displayName("Brown Aventurine")
                .color(0xB96E23)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Brown Aventurine")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("BrownAventurine", 0L)
                        .byproducts(
                                "GreenAventurine",
                                "YellowAventurine",
                                "BlackAventurine",
                                "BlueAventurine",
                                "RedAventurine")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("AventurineBrown")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9866, "Butane")
                .displayName("Butane")
                .color(0xFF2828)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Butane")
                        .forms("CONTAINERS", "CONTAINERS_GAS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8274, "CalciumSulfate")
                .displayName("Calcium Sulfate")
                .color(0xF0DCD2)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1730)
                .boilingPointKelvin(3000)
                .density(0.6021193F)
                .chemicalFormula("CaSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("CalciumSulphate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("CaSO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9836, "CarbonDioxide")
                .displayName("Carbon Dioxide")
                .color(0x282828)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.75661933F)
                .chemicalFormula("CO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Carbon Dioxide")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("CO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9838, "CarbonMonoxide")
                .displayName("Carbon Monoxide")
                .color(0x0A0A0A)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(1.1342145F)
                .chemicalFormula("CO")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Carbon Monoxide")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("CO")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9843, "CarbonTrioxide")
                .displayName("Carbon Trioxide")
                .color(0x2D2D2D)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.56782174F)
                .chemicalFormula("CO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Carbon Trioxide")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("CO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8003, "Carborundum")
                .displayName("Carborundum")
                .color(0x4D4D4D)
                .texture(MaterialTextureSet.QUARTZ)
                .meltingPointKelvin(3000)
                .boilingPointKelvin(3100)
                .density(2.2983F)
                .chemicalFormula("SiC")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Carborundum")
                        .source("ihl", "IHL")
                        .tools(3, 8.0F, 256L, 3)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Silicon", 648648000L),
                                new MaterialFacts.Part("Carbon", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("SiC")
                        .build())
                .build());
    }

    private static void part4(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8470, "Carminite")
                .displayName("Carminite")
                .color(0x960000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Carminite")
                        .tools(3, 7.0F, 384L, 3)
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8803, "CastIron")
                .displayName("Cast Iron")
                .color(0x404040)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1811)
                .boilingPointKelvin(3134)
                .density(7.874F)
                .chemicalFormula("Fe")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cast Iron")
                        .source("steamcraft2", "Steamcraft 2")
                        .correctedToolStats()
                        .tools(3, 6.0F, 256L, 2)
                        .pulverizing("Iron", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Iron", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("IronCast")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8431, "CatsEye")
                .displayName("Cat's Eye")
                .color(0x4D7451)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("GreenTigerEye")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("CatsEye", 0L)
                        .byproducts("Hematite", "TigerEye", "DragonEye", "HawksEye", "BlackEye", "TigerIron")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("TigerEyeGreen")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8605, "CdInAgAlloy")
                .displayName("Cd-In-Ag-Alloy")
                .color(0x646480)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(752)
                .boilingPointKelvin(1940)
                .density(8.833667F)
                .chemicalFormula("CdInAg")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cd-In-Ag-Alloy")
                        .source("RotaryCraft", "RotaryCraft")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Cadmium", 648648000L),
                                new MaterialFacts.Part("Indium", 648648000L),
                                new MaterialFacts.Part("Silver", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("Cd_In_Ag_Alloy")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8225, "Ceramic")
                .displayName("Ceramic")
                .color(0xDC8246)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(0.81833994F)
                .chemicalFormula("(Al₂O₃)(SiO₂)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ceramic")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                18L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("SiliconDioxide", 7783776000L))
                        .forms("DUSTS", "PLANTS", "PLATES")
                        .processing("Compressing", "Ceramic", 0L)
                        .processing("Bending", "Ceramic", 0L)
                        .processing("Forging", "Ceramic", 0L)
                        .processing("Smashing", "Ceramic", 0L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8347, "CertusQuartz")
                .displayName("Certus Quartz")
                .color(0xD2D2E6)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Certus Quartz")
                        .source("appliedenergistics2", "Applied Energistics")
                        .tools(1, 5.0F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .byproducts("MilkyQuartz", "Barite")
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8336, "Charcoal")
                .displayName("Charcoal")
                .color(0x644646)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(1700)
                .boilingPointKelvin(4300)
                .density(0.929F)
                .chemicalFormula("C")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Charcoal")
                        .source("minecraft", "Minecraft")
                        .fuel(1600L, "Ashes", 162162000L)
                        .smelting("Carbon", 324324000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "INGOTS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8348, "ChargedCertusQuartz")
                .displayName("Charged Certus Quartz")
                .color(0xD2D2E6)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Charged Certus Quartz")
                        .source("appliedenergistics2", "Applied Energistics")
                        .correctedToolStats()
                        .tools(1, 5.0F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .pulverizing("CertusQuartz", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .byproducts("MilkyQuartz", "Barite")
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9780, "Cheese")
                .displayName("Cheese")
                .color(0xFFEA00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(320)
                .boilingPointKelvin(500)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Cheese")
                        .source("GalacticraftCore", "Galacticraft")
                        .forms("DUSTS", "INGOTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9781, "Chili")
                .displayName("Chili")
                .color(0xC80000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Chili")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8400, "ChloroauricAcid")
                .displayName("Chloroauric Acid")
                .color(0xFFC846)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(3.2158244F)
                .chemicalFormula("AuCl₄H")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Chloroauric Acid")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Gold", 648648000L),
                                new MaterialFacts.Part("Chlorine", 2594592000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8401, "ChloroplatinicAcid")
                .displayName("Chloroplatinic Acid")
                .color(0xFF4646)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(2.3866072F)
                .chemicalFormula("PtCl₆H₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Chloroplatinic Acid")
                        .composition(
                                9L,
                                new MaterialFacts.Part("Platinum", 648648000L),
                                new MaterialFacts.Part("Chlorine", 3891888000L),
                                new MaterialFacts.Part("Hydrogen", 1297296000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9783, "Chocolate")
                .displayName("Chocolate")
                .color(0x643200)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(313)
                .boilingPointKelvin(400)
                .density(0.8024633F)
                .chemicalFormula("Cocoa(C₁₂H₂₂O₁₁)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Chocolate")
                        .source("harvestcraft", "HarvestCraft")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Cocoa", 648648000L),
                                new MaterialFacts.Part("Sugar", 648648000L))
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8685, "ChromiumDioxide")
                .displayName("Chromium Dioxide")
                .color(0x0A140A)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(650)
                .boilingPointKelvin(1300)
                .density(7.152858F)
                .chemicalFormula("CrO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Chromium Dioxide")
                        .tools(3, 11.0F, 256L, 3)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Chromium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("CrO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9785, "Cinnamon")
                .displayName("Cinnamon")
                .color(0x7A5335)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Cinnamon")
                        .source("harvestcraft", "HarvestCraft")
                        .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part5(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9243, "ClayBrick")
                .displayName("Clay Brick")
                .color(0xB75A40)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(0.81833994F)
                .chemicalFormula("((Al₂O₃)(SiO₂)₄)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Brick")
                        .correctedToolStats()
                        .tools(0, 1.0F, 1L, 0)
                        .composition(1L, new MaterialFacts.Part("Ceramic", 648648000L))
                        .aliases("Brick")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8276, "ClayBrown")
                .displayName("ClayBrown")
                .color(0xE68C4B)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(1.328257F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("ClayBrown")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .smelting("Ceramic", 648648000L)
                        .byproducts("Clay", "ClayRed")
                        .forms("DUSTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8455, "ClayRed")
                .displayName("ClayRed")
                .color(0xE62819)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(1.334331F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("ClayRed")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .smelting("Ceramic", 648648000L)
                        .byproducts("Bentonite", "ClayBrown")
                        .forms("DUSTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8334, "Coal")
                .displayName("Coal")
                .color(0x464646)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(1700)
                .boilingPointKelvin(4300)
                .density(0.929F)
                .chemicalFormula("C")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Coal")
                        .source("minecraft", "Minecraft")
                        .fuel(1600L, "DarkAshes", 162162000L)
                        .smelting("Carbon", 324324000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 648648000L))
                        .byproducts("Lignite", "Sulfur")
                        .forms("ARMORS", "DUSTS", "GEMS", "INGOTS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8349, "CoalCoke")
                .displayName("Coal Coke")
                .color(0x8C8CAA)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(1700)
                .boilingPointKelvin(4300)
                .density(0.929F)
                .chemicalFormula("C")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Coke")
                        .source("Railcraft", "Railcraft")
                        .correctedToolStats()
                        .tools(0, 1.0F, 1L, 0)
                        .fuel(3200L, "DarkAshes", 72072000L)
                        .smelting("Carbon", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "INGOTS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8621, "CobaltBrass")
                .displayName("Cobalt Brass")
                .color(0xB4B4A0)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1202)
                .boilingPointKelvin(2870)
                .density(7.8980556F)
                .chemicalFormula("(Cu₃Zn)₇AlCo")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cobalt Brass")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 8.0F, 256L, 2)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Brass", 4540536000L),
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Cobalt", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8229, "CobaltHexahydrate")
                .displayName("Cobalt Hexahydrate")
                .color(0x5050FA)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(486)
                .boilingPointKelvin(776)
                .density(2.122857F)
                .chemicalFormula("Co(H₂O)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cobalt Hexahydrate")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Cobalt", 648648000L),
                                new MaterialFacts.Part("Water", 3891888000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9782, "Cocoa")
                .displayName("Cocoa")
                .color(0xBE5F00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Cocoa")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9784, "Coffee")
                .displayName("Coffee")
                .color(0x964B00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("CoffeeDust")
                        .source("IC2", "IndustrialCraft 2")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8727, "ConductiveIron")
                .displayName("Conductive Iron")
                .color(0xAA8C8C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1255)
                .boilingPointKelvin(2317)
                .density(7.874F)
                .chemicalFormula("(Fe)((FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Conductive Iron")
                        .source("EnderIO", "Ender IO")
                        .correctedToolStats()
                        .tools(3, 6.0F, 384L, 2)
                        .composition(
                                1L,
                                new MaterialFacts.Part("WroughtIron", 648648000L),
                                new MaterialFacts.Part("Redstone", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8662, "Constantan")
                .displayName("Constantan")
                .color(0x966455)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1542)
                .boilingPointKelvin(3010)
                .density(8.936F)
                .chemicalFormula("CuNi")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cupronickel")
                        .source("ImmersiveEngineering", "Immersive Engineering")
                        .tools(3, 6.0F, 64L, 1)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Nickel", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9883, "ConstructionFoam")
                .displayName("Construction Foam")
                .color(0x808080)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(500)
                .boilingPointKelvin(2000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Construction Foam")
                        .source("IC2", "IndustrialCraft 2")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8800, "CosmicNeutronium")
                .displayName("Cosmic Neutronium")
                .color(0x1E0A28)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(100000)
                .boilingPointKelvin(200000)
                .density(1.0F)
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Cosmic Neutronium")
                        .source("Avaritia", "Avaritia")
                        .tools(3, 50.0F, 100000L, 10)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8322, "Craponite")
                .displayName("Craponite")
                .color(0xFFAAB9)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Craponite")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 7.0F, 256L, 2)
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8799, "CrystalMatrix")
                .displayName("Crystal Matrix")
                .color(0x53E7EA)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(3896)
                .boilingPointKelvin(5127)
                .density(72.6F)
                .chemicalFormula("(C₄)₂₀Nether Star₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Crystal Matrix")
                        .source("Avaritia", "Avaritia")
                        .tools(3, 20.0F, 25600L, 4)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Diamond", 12972960000L),
                                new MaterialFacts.Part("NetherStar", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part6(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8717, "Cyanite")
                .displayName("Cyanite")
                .color(0x326E96)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Cyanite")
                        .source("BigReactors", "Big Reactors")
                        .byproducts("Lead", "Radium", "RareEarth")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8408, "CyanVitriol")
                .displayName("Cyan Vitriol")
                .color(0x6FDEDE)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.830786F)
                .chemicalFormula("NiSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SolutionNickelSulfate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Nickel", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8634, "DamascusSteel")
                .displayName("Damascus Steel")
                .color(0x6E6E6E)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2080)
                .boilingPointKelvin(3196)
                .density(8.3812F)
                .chemicalFormula("((Fe))₅₀VW")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Damascus Steel")
                        .source("Metallurgy", "Metallurgy")
                        .tools(3, 8.0F, 1280L, 2)
                        .composition(
                                50L,
                                new MaterialFacts.Part("Steel", 32432400000L),
                                new MaterialFacts.Part("Vanadium", 648648000L),
                                new MaterialFacts.Part("Tungsten", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8201, "DarkAshes")
                .displayName("Dark Ashes")
                .color(0x323232)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DarkAsh")
                        .forms("DUSTS", "PLANTS")
                        .aliases("DarkAsh")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8300, "Diamond")
                .displayName("Diamond")
                .color(0xC8FFFF)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Diamond")
                        .source("minecraft", "Minecraft")
                        .correctedToolStats()
                        .tools(3, 8.0F, 1280L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .byproducts("Graphite", "PinkDiamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8423, "DiamondIndustrial")
                .displayName("DiamondIndustrial")
                .color(0xFFFFD2)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("DiamondIndustrial")
                        .tools(3, 9.0F, 1440L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9863, "Diesel")
                .displayName("Diesel")
                .color(0xFFFF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Diesel")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8325, "Dioptase")
                .displayName("Dioptase")
                .color(0x00B4B4)
                .texture(MaterialTextureSet.EMERALD)
                .meltingPointKelvin(1023)
                .boilingPointKelvin(1713)
                .density(1.7867359F)
                .chemicalFormula("(SiO₂)CuO(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Dioptase")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("Dioptase", 0L)
                        .composition(
                                8L,
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L),
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L),
                                new MaterialFacts.Part("Water", 1945944000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9844, "DisulfuricAcid")
                .displayName("Disulfuric Acid")
                .color(0xFF9600)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.5F)
                .chemicalFormula("H₂S₂O₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Disulfuric Acid")
                        .composition(
                                11L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 4540536000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("H2S2O7")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8432, "DragonEye")
                .displayName("Dragon Eye")
                .color(0xA45F53)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("RedTigerEye")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("DragonEye", 0L)
                        .byproducts("Hematite", "TigerEye", "CatsEye", "HawksEye", "BlackEye", "TigerIron")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("TigerEyeRed")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8756, "Duralumin")
                .displayName("Duralumin")
                .color(0xFFFFDC)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1145)
                .boilingPointKelvin(2813)
                .density(5.829F)
                .chemicalFormula("AlCu")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Duralumin")
                        .source("GalaxySpace", "Galaxy Space")
                        .tools(3, 10.0F, 512L, 2)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Copper", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8751, "Duranium")
                .displayName("Duranium")
                .color(0x4BAFAF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1165)
                .boilingPointKelvin(2350)
                .density(17.71725F)
                .chemicalFormula("Dn₇Mg")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Duranium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 8.0F, 1280L, 4)
                        .composition(
                                8L,
                                new MaterialFacts.Part("DuraniumElemental", 4540536000L),
                                new MaterialFacts.Part("Magnesium", 648648000L))
                        .byproducts("TritaniumDioxide", "Trinium")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("DuraniumAlloy")
                        .build())
                .build());
    }

    private static void part7(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8250, "DyeBlack")
                .displayName("DyeBlack")
                .color(0x202020)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeBlack")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Black")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8254, "DyeBlue")
                .displayName("DyeBlue")
                .color(0x0000FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeBlue")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Blue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8253, "DyeBrown")
                .displayName("DyeBrown")
                .color(0x604000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeBrown")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Brown")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8256, "DyeCyan")
                .displayName("DyeCyan")
                .color(0x00FFFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeCyan")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Cyan")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8258, "DyeGray")
                .displayName("DyeGray")
                .color(0x808080)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeGray")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Gray")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8252, "DyeGreen")
                .displayName("DyeGreen")
                .color(0x00FF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeGreen")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Green")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8262, "DyeLightBlue")
                .displayName("Light Blue")
                .color(0x8080FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeLightBlue")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("LightBlue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8257, "DyeLightGray")
                .displayName("Light Gray")
                .color(0xC0C0C0)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeLightGray")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("LightGray")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8260, "DyeLime")
                .displayName("DyeLime")
                .color(0x80FF80)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeLime")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("Lime")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8263, "DyeMagenta")
                .displayName("DyeMagenta")
                .color(0xFF00FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeMagenta")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Magenta")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8264, "DyeOrange")
                .displayName("DyeOrange")
                .color(0xFF8000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeOrange")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Orange")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8259, "DyePink")
                .displayName("DyePink")
                .color(0xFFC0C0)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyePink")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Pink")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8255, "DyePurple")
                .displayName("DyePurple")
                .color(0x800080)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyePurple")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Purple")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8251, "DyeRed")
                .displayName("DyeRed")
                .color(0xFF0000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeRed")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Red")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8265, "DyeWhite")
                .displayName("DyeWhite")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeWhite")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("White")
                        .build())
                .build());
    }

    private static void part8(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8261, "DyeYellow")
                .displayName("DyeYellow")
                .color(0xFFFF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("DyeYellow")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Yellow")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8249, "Dynamite")
                .displayName("Dynamite")
                .color(0x6F836F)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(343)
                .boilingPointKelvin(411)
                .density(1.431F)
                .chemicalFormula("(C₃H₅N₃O₉)(C₆(H₂O)₅)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.EXPLOSIVE,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Dynamite")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Glyceryl", 648648000L),
                                new MaterialFacts.Part("Wood", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8747, "Efrine")
                .displayName("Efrine")
                .color(0x506B48)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2246)
                .boilingPointKelvin(3334)
                .density(1.0F)
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Efrine")
                        .source("netherlicious", "Netherlicious")
                        .tools(3, 9.0F, 500L, 3)
                        .byproducts("Soulsand", "Beryllium", "Pentlandite", "Zircon", "GreenFluorite")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8730, "ElectricalSteel")
                .displayName("Electrical Steel")
                .color(0x8C8C8C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1866)
                .boilingPointKelvin(3336)
                .density(10.2036F)
                .chemicalFormula("((Fe))Si")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Electrical Steel")
                        .source("EnderIO", "Ender IO")
                        .correctedToolStats()
                        .tools(3, 6.0F, 512L, 2)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Steel", 648648000L),
                                new MaterialFacts.Part("Silicon", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8658, "ElectrotineAlloy")
                .displayName("Electrotine Alloy")
                .color(0x64B4FF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1400)
                .boilingPointKelvin(3134)
                .density(7.874F)
                .chemicalFormula("(Fe)(((Al₂O₃)₃(SiO₂)Na₄Cl)₅Cu₃(SiO₂)Ar)₈")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Electrotine Alloy")
                        .source("ProjRed|Core", "Project Red")
                        .composition(
                                1L,
                                new MaterialFacts.Part("WroughtIron", 648648000L),
                                new MaterialFacts.Part("Nikolite", 5189184000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8600, "Electrum")
                .displayName("Electrum")
                .color(0xFFFF64)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1285)
                .boilingPointKelvin(2782)
                .density(14.8915F)
                .chemicalFormula("AgAu")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Electrum")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .tools(3, 12.0F, 64L, 2)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Silver", 648648000L),
                                new MaterialFacts.Part("Gold", 648648000L))
                        .byproducts("Gold", "Silver")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8722, "ElvenElementium")
                .displayName("Elven Elementium")
                .color(0xFA78FA)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2811)
                .boilingPointKelvin(5134)
                .density(7.874F)
                .chemicalFormula("Fe")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Elementium")
                        .source("Botania", "Botania")
                        .tools(3, 14.0F, 512L, 3)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8399, "ElvenQuartz")
                .displayName("Elven Quartz")
                .color(0xD2FFD2)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Elven Quartz")
                        .source("Botania", "Botania")
                        .tools(1, 2.5F, 64L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8301, "Emerald")
                .displayName("Emerald")
                .color(0x50FF50)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Emerald")
                        .source("minecraft", "Minecraft")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Aquamarine",
                                "Morganite",
                                "Goshenite",
                                "Bixbite",
                                "Heliodor",
                                "Maxixe")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8319, "EnderEye")
                .displayName("EnderEye")
                .color(0xA0FAE6)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(3447)
                .boilingPointKelvin(4978)
                .density(0.6415364F)
                .chemicalFormula("(BeK₄N₅Ma₆)₉Blaze")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("EnderEye")
                        .source("minecraft", "Minecraft")
                        .tools(3, 1.0F, 16L, 1)
                        .composition(
                                9L,
                                new MaterialFacts.Part("EnderPearl", 5837832000L),
                                new MaterialFacts.Part("Blaze", 648648000L))
                        .forms("DUSTS", "GEMS", "LENSES", "PLANTS", "PLATES", "PROJECTILES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8710, "Enderium")
                .displayName("Enderium")
                .color(0x3C7D73)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1897)
                .boilingPointKelvin(3427)
                .density(12.164175F)
                .chemicalFormula("(Sn₂AgPt)(BeK₄N₅Ma₆)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Enderium")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .tools(3, 8.0F, 256L, 3)
                        .composition(
                                1L,
                                new MaterialFacts.Part("EnderiumBase", 648648000L),
                                new MaterialFacts.Part("EnderPearl", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8729, "EnderiumBase")
                .displayName("Enderium Base")
                .color(0x35556C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1071)
                .boilingPointKelvin(3070)
                .density(11.63375F)
                .chemicalFormula("Sn₂AgPt")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Enderium Base")
                        .source("EnderIO", "Ender IO")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Tin", 1297296000L),
                                new MaterialFacts.Part("Silver", 648648000L),
                                new MaterialFacts.Part("Platinum", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8318, "EnderPearl")
                .displayName("EnderPearl")
                .color(0x6CDCC8)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(2723)
                .boilingPointKelvin(3785)
                .density(0.5304253F)
                .chemicalFormula("BeK₄N₅Ma₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Ender")
                        .source("minecraft", "Minecraft")
                        .tools(3, 1.0F, 16L, 1)
                        .composition(
                                10L,
                                new MaterialFacts.Part("Beryllium", 648648000L),
                                new MaterialFacts.Part("Potassium", 2594592000L),
                                new MaterialFacts.Part("Nitrogen", 3243240000L),
                                new MaterialFacts.Part("Magic", 3891888000L))
                        .forms("DUSTS", "GEMS", "LENSES", "PLANTS", "PLATES", "PROJECTILES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8299, "EnergiumCyan")
                .displayName("EnergiumCyan")
                .color(0x00FFFF)
                .texture(MaterialTextureSet.DIAMOND)
                .meltingPointKelvin(1875)
                .boilingPointKelvin(3111)
                .density(2.1755323F)
                .chemicalFormula("((Al₂O₃))₄(((Al₂O₃)₃(SiO₂)Na₄Cl)₅Cu₃(SiO₂)Ar)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("EnergiumCyan")
                        .composition(
                                9L,
                                new MaterialFacts.Part("Sapphire", 2594592000L),
                                new MaterialFacts.Part("Nikolite", 3243240000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8298, "EnergiumRed")
                .displayName("EnergiumRed")
                .color(0xFF0000)
                .texture(MaterialTextureSet.DIAMOND)
                .meltingPointKelvin(1320)
                .boilingPointKelvin(2277)
                .density(3.9268732F)
                .chemicalFormula("((Al₂O₃))₄((FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr))₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("EnergiumRed")
                        .source("IC2", "IndustrialCraft 2")
                        .composition(
                                9L,
                                new MaterialFacts.Part("Sapphire", 2594592000L),
                                new MaterialFacts.Part("Redstone", 3243240000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part9(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9842, "Ethanol")
                .displayName("Ethanol")
                .color(0xFF8000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Ethanol")
                        .source("Forestry", "Forestry")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9868, "Ethylene")
                .displayName("Ethylene")
                .color(0x402864)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Ethylene")
                        .forms("CONTAINERS", "CONTAINERS_GAS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8421, "Eudialyte")
                .displayName("Eudialyte")
                .color(0x9B6072)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("(Zr(SiO₂)O₂)₃(MnO₂)₃Na₁₅Ca₆Cl(SiO₂)₂₅O₁₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.ORE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Eudialyte")
                        .composition(
                                130L,
                                new MaterialFacts.Part("Zircon", 11675664000L),
                                new MaterialFacts.Part("Pyrolusite", 1945944000L),
                                new MaterialFacts.Part("Sodium", 9729720000L),
                                new MaterialFacts.Part("Calcium", 3891888000L),
                                new MaterialFacts.Part("Chlorine", 648648000L),
                                new MaterialFacts.Part("SiliconDioxide", 48648600000L),
                                new MaterialFacts.Part("Oxygen", 7783776000L))
                        .byproducts("Zircon", "RareEarth", "Hafnium", "Lead")
                        .multipliers(5, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8017, "FerricChloride")
                .displayName("Ferric Chloride")
                .color(0xB4B478)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(580)
                .boilingPointKelvin(589)
                .density(1.9709105F)
                .chemicalFormula("FeCl₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ferric Chloride")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Chlorine", 1945944000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("FeCl3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8035, "FerricOxyhydroxide")
                .displayName("Ferric Oxyhydroxide")
                .color(0x893E28)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1207)
                .boilingPointKelvin(2414)
                .density(1.1255081F)
                .chemicalFormula("FeO₃H₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ferric Oxyhydroxide")
                        .smelting("Hematite", 231660000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L),
                                new MaterialFacts.Part("Hydrogen", 1945944000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("FeO3H3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8030, "FerrousChloride")
                .displayName("Ferrous Chloride")
                .color(0xC7E9C7)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(950)
                .boilingPointKelvin(1296)
                .density(2.6268094F)
                .chemicalFormula("FeCl₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ferrous Chloride")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Chlorine", 1297296000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("FeCl2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8675, "FierySteel")
                .displayName("Fiery Steel")
                .color(0x400000)
                .texture(MaterialTextureSet.FIERY)
                .meltingPointKelvin(2934)
                .boilingPointKelvin(3634)
                .density(7.874F)
                .chemicalFormula("((Fe))Ma")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Fiery")
                        .source("TwilightForest", "Twilight Forest")
                        .tools(3, 9.0F, 1024L, 4)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Steel", 648648000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8698, "Fireleaf")
                .displayName("Fireleaf")
                .color(0x7F3232)
                .texture(MaterialTextureSet.LEAF)
                .meltingPointKelvin(2934)
                .boilingPointKelvin(3634)
                .density(7.874F)
                .chemicalFormula("((Fe))Ma₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Fireleaf")
                        .source("TwilightForest", "Twilight Forest")
                        .tools(2, 12.0F, 288L, 4)
                        .smelting("FierySteel", 162162000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Steel", 648648000L),
                                new MaterialFacts.Part("Magic", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8342, "Firestone")
                .displayName("Firestone")
                .color(0xC81400)
                .texture(MaterialTextureSet.QUARTZ)
                .meltingPointKelvin(3000)
                .boilingPointKelvin(3700)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Firestone")
                        .source("Railcraft", "Railcraft")
                        .tools(3, 6.0F, 1280L, 3)
                        .byproducts("NetherQuartz", "RedPhosphorus", "RedFluorite")
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9711, "FishCooked")
                .displayName("FishCooked")
                .color(0x967814)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(477)
                .boilingPointKelvin(550)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("FishCooked")
                        .source("Mariculture", "Mariculture")
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9871, "FishOil")
                .displayName("Fish Oil")
                .color(0xFFC400)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Fish Oil")
                        .source("Mariculture", "Mariculture")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9712, "FishRaw")
                .displayName("FishRaw")
                .color(0xFF9664)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(477)
                .boilingPointKelvin(550)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("FishRaw")
                        .source("Mariculture", "Mariculture")
                        .smelting("FishCooked", 648648000L)
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .processing("Forging", "FishCooked", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9713, "FishRotten")
                .displayName("FishRotten")
                .color(0xDCC864)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(477)
                .boilingPointKelvin(550)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("FishRotten")
                        .source("Mariculture", "Mariculture")
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8697, "FlamascusSteel")
                .displayName("Flamascus Steel")
                .color(0x6E6E6E)
                .texture(MaterialTextureSet.FIERY)
                .meltingPointKelvin(2996)
                .boilingPointKelvin(3696)
                .density(8.3812F)
                .chemicalFormula("(((Fe))₅₀VW)Ma")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Flamascus Steel")
                        .source("TwilightForest", "Twilight Forest")
                        .tools(3, 12.0F, 1280L, 4)
                        .composition(
                                1L,
                                new MaterialFacts.Part("DamascusSteel", 648648000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8002, "Flint")
                .displayName("Flint")
                .color(0x002040)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Flint")
                        .source("minecraft", "Minecraft")
                        .tools(1, 2.5F, 48L, 1)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .byproducts("Obsidian")
                        .forms("ARMORS", "DUSTS", "GEMS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part10(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8389, "Fluix")
                .displayName("Fluix")
                .color(0x78468C)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1490)
                .boilingPointKelvin(2646)
                .density(3.9516528F)
                .chemicalFormula("(SiO₂)₂((FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Fluix")
                        .source("appliedenergistics2", "Applied Energistics")
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("SiliconDioxide", 1297296000L),
                                new MaterialFacts.Part("Redstone", 648648000L))
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9215, "Fluorite")
                .displayName("Fluorite")
                .color(0xE1B98C)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Fluorite")
                        .source("RotaryCraft", "RotaryCraft")
                        .byproducts("GreenFluorite", "OrangeFluorite")
                        .forms(
                                "ARMORS",
                                "DUSTS",
                                "GEMS",
                                "INGOTS",
                                "MOLTEN",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("CaF2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9803, "FreshWater")
                .displayName("Fresh Water")
                .color(0x6E6EFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID, MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Fresh Water")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9880, "FryingOilHot")
                .displayName("FryingOilHot")
                .color(0xC8C400)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("FryingOilHot")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8735, "GaiaSpirit")
                .displayName("Gaia Spirit")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(3945)
                .boilingPointKelvin(6328)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Gaia Spirit")
                        .source("Botania", "Botania")
                        .tools(3, 20.0F, 2048L, 4)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8001, "Glass")
                .displayName("Glass")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.GLASS)
                .meltingPointKelvin(1200)
                .boilingPointKelvin(2400)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Glass")
                        .source("minecraft", "Minecraft")
                        .tools(1, 1.0F, 1L, 0)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8341, "Glowstone")
                .displayName("Glowstone")
                .color(0xFFFF00)
                .texture(MaterialTextureSet.REDSTONE)
                .meltingPointKelvin(500)
                .boilingPointKelvin(600)
                .density(6.3510957F)
                .chemicalFormula("(Ca₅(PO₄)₃F)₅Au₃(SiO₂)He")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Glowstone")
                        .source("minecraft", "Minecraft")
                        .composition(
                                10L,
                                new MaterialFacts.Part("Phosphorite", 3243240000L),
                                new MaterialFacts.Part("Gold", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 648648000L),
                                new MaterialFacts.Part("Helium", 648648000L))
                        .byproducts("Redstone", "Gold", "YellowFluorite")
                        .multipliers(4, 1)
                        .forms(
                                "ARMORS",
                                "DUSTS",
                                "GEMS",
                                "LENSES",
                                "MOLTEN",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9881, "Glue")
                .displayName("Glue")
                .color(0xC8C400)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(150)
                .boilingPointKelvin(500)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Glue")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9828, "Glycerol")
                .displayName("Glycerol")
                .color(0x00B4B4)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(291)
                .boilingPointKelvin(563)
                .density(1.5F)
                .chemicalFormula("C₃H₈O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Glycerol")
                        .composition(
                                14L,
                                new MaterialFacts.Part("Carbon", 1945944000L),
                                new MaterialFacts.Part("Hydrogen", 5189184000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9821, "Glyceryl")
                .displayName("Glyceryl")
                .color(0x009696)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(287)
                .boilingPointKelvin(323)
                .density(1.5F)
                .chemicalFormula("C₃H₅N₃O₉")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Glyceryl")
                        .composition(
                                20L,
                                new MaterialFacts.Part("Carbon", 1945944000L),
                                new MaterialFacts.Part("Hydrogen", 3243240000L),
                                new MaterialFacts.Part("Nitrogen", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 5837832000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8385, "Goshenite")
                .displayName("Goshenite")
                .color(0xF0F0F0)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Goshenite")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Emerald",
                                "Aquamarine",
                                "Morganite",
                                "Bixbite",
                                "Heliodor",
                                "Maxixe")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8518, "Granite")
                .displayName("Granite")
                .color(0xA07882)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1500)
                .boilingPointKelvin(3000)
                .density(0.8436734F)
                .chemicalFormula("(K₂Mg₆(Al₂O₃)₃F₄(SiO₂)₆)(K₂(Al₂O₃)(SiO₂)₆O)((SiO₂))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Granite")
                        .tools(1, 3.0F, 64L, 1)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Biotite", 648648000L),
                                new MaterialFacts.Part("PotassiumFeldspar", 648648000L),
                                new MaterialFacts.Part("Flint", 648648000L))
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8508, "GraniteBlack")
                .displayName("GraniteBlack")
                .color(0x141414)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1500)
                .boilingPointKelvin(3000)
                .density(0.8436734F)
                .chemicalFormula("(K₂Mg₆(Al₂O₃)₃F₄(SiO₂)₆)(K₂(Al₂O₃)(SiO₂)₆O)((SiO₂))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("GraniteBlack")
                        .tools(1, 3.0F, 64L, 3)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Biotite", 648648000L),
                                new MaterialFacts.Part("PotassiumFeldspar", 648648000L),
                                new MaterialFacts.Part("Flint", 648648000L))
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8507, "GraniteRed")
                .displayName("GraniteRed")
                .color(0xA03C46)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1500)
                .boilingPointKelvin(3000)
                .density(0.8436734F)
                .chemicalFormula("(K₂Mg₆(Al₂O₃)₃F₄(SiO₂)₆)(K₂(Al₂O₃)(SiO₂)₆O)((SiO₂))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("GraniteRed")
                        .tools(1, 3.0F, 64L, 3)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Biotite", 648648000L),
                                new MaterialFacts.Part("PotassiumFeldspar", 648648000L),
                                new MaterialFacts.Part("Flint", 648648000L))
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9175, "Graphene")
                .displayName("Graphene")
                .color(0x3C3C3C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(4300)
                .boilingPointKelvin(4400)
                .density(2.267F)
                .chemicalFormula("C")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Graphene")
                        .smelting("Carbon", 324324000L)
                        .pulverizing("Carbon", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 648648000L))
                        .forms("ARMORS", "FOILS", "PARTS", "PLATES", "PROJECTILES", "STICKS", "WIRES")
                        .build())
                .build());
    }

    private static void part11(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8410, "GrayVitriol")
                .displayName("Gray Vitriol")
                .color(0x6F6F6F)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.5854527F)
                .chemicalFormula("MnSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Gray Vitriol")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Manganese", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8448, "GreenAventurine")
                .displayName("Green Aventurine")
                .color(0x42BD85)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Aventurine")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("GreenAventurine", 0L)
                        .byproducts(
                                "BrownAventurine",
                                "YellowAventurine",
                                "BlackAventurine",
                                "BlueAventurine",
                                "RedAventurine")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("AventurineGreen")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8465, "GreenDiamond")
                .displayName("Green Diamond")
                .color(0x00F000)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Green Diamond")
                        .source("ExtraPlanets", "Extra Planets")
                        .correctedToolStats()
                        .tools(3, 8.0F, 1280L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .byproducts("Graphite", "Diamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("DiamondGreen")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8439, "GreenFluorite")
                .displayName("Green Fluorite")
                .color(0x25A823)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Green Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("YellowFluorite", "BlueFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteGreen")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8428, "GreenJasper")
                .displayName("Green Jasper")
                .color(0x5B836C)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1927)
                .boilingPointKelvin(3191)
                .density(3.1429906F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Green Jasper")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("GreenJasper", 0L)
                        .byproducts("RedJasper", "OceanJasper", "RainforestJasper", "BlueJasper", "YellowJasper")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("JasperGreen")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8305, "GreenSapphire")
                .displayName("Green Sapphire")
                .color(0x64C882)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2108)
                .boilingPointKelvin(2935)
                .density(1.1897146F)
                .chemicalFormula("(Al₂O₃)Mg")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Green Sapphire")
                        .source("Redpower", "Redpower")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Magnesium", 648648000L))
                        .byproducts("Alumina", "Ruby", "YellowSapphire", "BlueSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8405, "GreenVitriol")
                .displayName("Green Vitriol")
                .color(0x42DE42)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.657786F)
                .chemicalFormula("FeSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Green Vitriol")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9119, "Grossular")
                .displayName("Grossular")
                .color(0xC86400)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1655)
                .boilingPointKelvin(2538)
                .density(0.8510974F)
                .chemicalFormula("(Al₂O₃)Ca₃(SiO₂)₃O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("GarnetOrange")
                        .correctedToolStats()
                        .tools(3, 7.0F, 128L, 2)
                        .smelting("Grossular", 0L)
                        .composition(
                                20L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 5837832000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Almandine", "Pyrope", "Spessartine", "Andradite", "Uvarovite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8220, "Gunpowder")
                .displayName("Gunpowder")
                .color(0x808080)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(2148)
                .boilingPointKelvin(2632)
                .density(1.6990769F)
                .chemicalFormula("C₂S(NaNO₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.EXPLOSIVE,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Gunpowder")
                        .source("minecraft", "Minecraft")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Carbon", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("SodiumNitrate", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8433, "HawksEye")
                .displayName("Hawk's Eye")
                .color(0x4C5E6D)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("BlueTigerEye")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("HawksEye", 0L)
                        .byproducts("Hematite", "TigerEye", "CatsEye", "DragonEye", "BlackEye", "TigerIron")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("TigerEyeBlue")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9796, "Hazelnut")
                .displayName("Hazelnut")
                .color(0xF0C88C)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Hazelnut")
                        .source("BinnieCore", "Binnie's Mods")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9812, "HeavyWater")
                .displayName("Heavy Water")
                .color(0xFFFF64)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(277)
                .boilingPointKelvin(375)
                .density(1.1056F)
                .chemicalFormula("D₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Heavy Water")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Deuterium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("D2O")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8384, "Heliodor")
                .displayName("Heliodor")
                .color(0xFFFF96)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Heliodor")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Emerald",
                                "Aquamarine",
                                "Morganite",
                                "Goshenite",
                                "Bixbite",
                                "Maxixe")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9839, "HeliumNeon")
                .displayName("Helium-Neon")
                .color(0xFF0080)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(12)
                .boilingPointKelvin(15)
                .density(5.392E-4F)
                .chemicalFormula("HeNe")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Helium-Neon")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Helium", 648648000L),
                                new MaterialFacts.Part("Neon", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("HeNe")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9873, "HempOil")
                .displayName("Hemp Oil")
                .color(0xC4FF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Hemp Oil")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
    }

    private static void part12(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8011, "HexafluorosilicicAcid")
                .displayName("Hexafluorosilicic Acid")
                .color(0xBEC8BE)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(250)
                .boilingPointKelvin(381)
                .density(1.5F)
                .chemicalFormula("H₂SiF₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Hexafluorosilicic Acid")
                        .composition(
                                9L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Silicon", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("H2SiF6")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9805, "HolyWater")
                .displayName("Holy Water")
                .color(0x7878FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Holy Water")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9791, "Honey")
                .displayName("Honey")
                .color(0xFAC800)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Honey")
                        .source("Forestry", "Forestry")
                        .forms("CONTAINERS", "DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9793, "Honeydew")
                .displayName("Honeydew")
                .color(0xD26400)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Honeydew")
                        .source("Forestry", "Forestry")
                        .forms("CONTAINERS", "DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8797, "HSSE")
                .displayName("HSS-E")
                .color(0x336600)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2367)
                .boilingPointKelvin(3886)
                .density(9.589215F)
                .chemicalFormula("((((Fe))W)₅CrMo₂V)₆CoMnSi")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("HSS-E")
                        .source("gregtech", "GregTech 5 Unofficial")
                        .tools(3, 10.0F, 5120L, 4)
                        .composition(
                                9L,
                                new MaterialFacts.Part("HSSG", 3891888000L),
                                new MaterialFacts.Part("Cobalt", 648648000L),
                                new MaterialFacts.Part("Manganese", 648648000L),
                                new MaterialFacts.Part("Silicon", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8796, "HSSG")
                .displayName("HSS-G")
                .color(0x999900)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2722)
                .boilingPointKelvin(4317)
                .density(11.278889F)
                .chemicalFormula("(((Fe))W)₅CrMo₂V")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("HSS-G")
                        .source("gregtech", "GregTech 5 Unofficial")
                        .tools(3, 10.0F, 4000L, 3)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Tungstensteel", 3243240000L),
                                new MaterialFacts.Part("Chromium", 648648000L),
                                new MaterialFacts.Part("Molybdenum", 1297296000L),
                                new MaterialFacts.Part("Vanadium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8798, "HSSS")
                .displayName("HSS-S")
                .color(0x660033)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2786)
                .boilingPointKelvin(4509)
                .density(15.044815F)
                .chemicalFormula("((((Fe))W)₅CrMo₂V)₆(OsIr)Ir")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("HSS-S")
                        .source("gregtech", "GregTech 5 Unofficial")
                        .tools(3, 14.0F, 3000L, 4)
                        .composition(
                                9L,
                                new MaterialFacts.Part("HSSG", 3891888000L),
                                new MaterialFacts.Part("Osmiridium", 1297296000L),
                                new MaterialFacts.Part("Iridium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8335, "HydratedCoal")
                .displayName("Hydrated Coal")
                .color(0x464664)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("(C)₈(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Hydrated Coal")
                        .composition(
                                8L,
                                new MaterialFacts.Part("Coal", 5189184000L),
                                new MaterialFacts.Part("Water", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9826, "HydrochloricAcid")
                .displayName("Hydrochloric Acid")
                .color(0x00FF80)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.00165194F)
                .chemicalFormula("HCl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Hydrochloric Acid")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Chlorine", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("HCl")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8007, "HydrogenBorate")
                .displayName("Hydrogen Borate")
                .color(0xEAEAFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(364)
                .boilingPointKelvin(647)
                .density(0.33493665F)
                .chemicalFormula("H₃BO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("BoricAcid")
                        .source("ihl", "IHL")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Hydrogen", 1945944000L),
                                new MaterialFacts.Part("Boron", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("H3BO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9829, "HydrogenFluoride")
                .displayName("Hydrogen Fluoride")
                .color(0x00F0F0)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(189)
                .boilingPointKelvin(292)
                .density(8.9294E-4F)
                .chemicalFormula("HF")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Hydrogen Fluoride")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Fluorine", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("HF")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9809, "HydrogenPeroxide")
                .displayName("Hydrogen Peroxide")
                .color(0x1414FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(423)
                .density(1.0F)
                .chemicalFormula("H₂O₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Hydrogen Peroxide")
                        .source("ihl", "IHL")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("H2O2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8024, "HydrosulfuricAcid")
                .displayName("Hydrosulfuric Acid")
                .color(0xF1BC85)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(191)
                .boilingPointKelvin(213)
                .density(0.6890599F)
                .chemicalFormula("H₂S")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Hydrosulfuric Acid")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("H2S")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9802, "Ice")
                .displayName("Ice")
                .color(0xC8C8FF)
                .texture(MaterialTextureSet.CUBE_SHINY)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ice")
                        .source("minecraft", "Minecraft")
                        .tools(1, 2.0F, 4L, 0)
                        .smelting("Water", 648648000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms(
                                "ARMORS",
                                "CONTAINERS",
                                "DUSTS",
                                "GEMS",
                                "LENSES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part13(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8661, "Invar")
                .displayName("Invar")
                .color(0xDCDC96)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1916)
                .boilingPointKelvin(3151)
                .density(8.22F)
                .chemicalFormula("(Fe)₂Ni")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Invar")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .tools(3, 6.0F, 256L, 2)
                        .composition(
                                3L,
                                new MaterialFacts.Part("WroughtIron", 1297296000L),
                                new MaterialFacts.Part("Nickel", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8645, "IronMagnetic")
                .displayName("IronMagnetic")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.MAGNETIC)
                .meltingPointKelvin(1811)
                .boilingPointKelvin(3134)
                .density(7.874F)
                .chemicalFormula("Fe")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("IronMagnetic")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .correctedToolStats()
                        .tools(3, 6.0F, 256L, 2)
                        .smelting("Iron", 648648000L)
                        .pulverizing("Iron", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Iron", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .processing("Bending", "Iron", 648648000L)
                        .processing("Compressing", "Iron", 648648000L)
                        .processing("Smashing", "Iron", 648648000L)
                        .processing("Working", "Iron", 648648000L)
                        .processing("Forging", "Iron", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8672, "Ironwood")
                .displayName("Ironwood")
                .color(0x968C6E)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(1580)
                .boilingPointKelvin(2816)
                .density(5.6892223F)
                .chemicalFormula("Fe₈LiveRoot₉Angmallen₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("IronWood")
                        .source("TwilightForest", "Twilight Forest")
                        .tools(2, 6.5F, 512L, 2)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("IronWood")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8321, "Jade")
                .displayName("Jade")
                .color(0x64FF7D)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Jade")
                        .source("erebus", "Erebus")
                        .tools(3, 8.0F, 512L, 2)
                        .byproducts("Mica", "Biotite", "Talc", "Asbestos")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8664, "Kanthal")
                .displayName("Kanthal")
                .color(0xC2D2DF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1708)
                .boilingPointKelvin(2956)
                .density(5.9073334F)
                .chemicalFormula("(Fe)AlCr")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Kanthal")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 6.0F, 64L, 2)
                        .composition(
                                3L,
                                new MaterialFacts.Part("WroughtIron", 648648000L),
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Chromium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9167, "Kaolinite")
                .displayName("Kaolinite")
                .color(0xF5EBEB)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(1.4038955F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Kaolinite")
                        .source("PFAAGeologica", "Per Fabrica Ad Astra")
                        .smelting("Ceramic", 648648000L)
                        .byproducts("ClayBrown", "Clay")
                        .forms("DUSTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9862, "Kerosine")
                .displayName("Kerosine")
                .color(0x0000FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Kerosine")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8674, "Knightmetal")
                .displayName("Knightmetal")
                .color(0xD2F0C8)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2146)
                .boilingPointKelvin(3234)
                .density(7.874F)
                .chemicalFormula("((Fe))₂Ma")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("KnightMetal")
                        .source("TwilightForest", "Twilight Forest")
                        .tools(3, 8.0F, 512L, 3)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Steel", 1297296000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8332, "Lapis")
                .displayName("Lapis")
                .color(0x4646DC)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1335)
                .boilingPointKelvin(2221)
                .density(1.2419112F)
                .chemicalFormula("((Al₂O₃)₆(SiO₂)₂Ca₈Na₈)₁₂((Al₂O₃)₃(SiO₂)Na₄Cl)₂(FeS₂)(Ca(CO₃))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lapis")
                        .source("minecraft", "Minecraft")
                        .composition(
                                16L,
                                new MaterialFacts.Part("Lazurite", 7783776000L),
                                new MaterialFacts.Part("Sodalite", 1297296000L),
                                new MaterialFacts.Part("Pyrite", 648648000L),
                                new MaterialFacts.Part("Calcite", 648648000L))
                        .byproducts("Lazurite", "Sodalite", "Pyrite")
                        .multipliers(5, 1)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "GEMS",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9886, "Latex")
                .displayName("Latex")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(150)
                .boilingPointKelvin(500)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Latex")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9810, "Lava")
                .displayName("Lava")
                .color(0xFF4000)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1300)
                .boilingPointKelvin(4000)
                .density(0.22316612F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Lava")
                        .source("minecraft", "Minecraft")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .processing("Solidifying", "Obsidian", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8330, "Lazurite")
                .displayName("Lazurite")
                .color(0x6478FF)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1352)
                .boilingPointKelvin(2218)
                .density(1.1154736F)
                .chemicalFormula("(Al₂O₃)₆(SiO₂)₂Ca₈Na₈")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lazurite")
                        .composition(
                                28L,
                                new MaterialFacts.Part("Alumina", 3891888000L),
                                new MaterialFacts.Part("SiliconDioxide", 3891888000L),
                                new MaterialFacts.Part("Calcium", 5189184000L),
                                new MaterialFacts.Part("Sodium", 5189184000L))
                        .byproducts("Sodalite", "Lapis")
                        .multipliers(5, 1)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "GEMS",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8241, "Leather")
                .displayName("Leather")
                .color(0x8D4125)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Leather")
                        .source("minecraft", "Minecraft")
                        .smelting("Ashes", 72072000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8337, "Lignite")
                .displayName("Lignite")
                .color(0x644646)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(1384)
                .boilingPointKelvin(1870)
                .density(0.865F)
                .chemicalFormula("C₂(H₂O)₄DarkAsh")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Lignite")
                        .source("UndergroundBiomes", "Underground Biomes")
                        .fuel(800L, "DarkAshes", 162162000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Carbon", 1297296000L),
                                new MaterialFacts.Part("Water", 2594592000L),
                                new MaterialFacts.Part("DarkAshes", 648648000L))
                        .byproducts("Coal", "Sulfur", "Germanium")
                        .forms("ARMORS", "DUSTS", "GEMS", "INGOTS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8365, "LigniteCoke")
                .displayName("Lignite Coke")
                .color(0x8C6464)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(2866)
                .boilingPointKelvin(3866)
                .density(0.865F)
                .chemicalFormula("C₂DarkAsh")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Lignite Coke")
                        .fuel(1600L, "DarkAshes", 72072000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Carbon", 1297296000L),
                                new MaterialFacts.Part("DarkAshes", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "INGOTS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part14(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9874, "LinOil")
                .displayName("Lin Oil")
                .color(0xC4FF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Lin Oil")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8033, "LithiumChlorate")
                .displayName("Lithium Chlorate")
                .color(0xDEE9FA)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(400)
                .boilingPointKelvin(800)
                .density(0.1083002F)
                .chemicalFormula("LiClO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lithium Chlorate")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Lithium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("LiClO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8032, "LithiumHydroxide")
                .displayName("Lithium Hydroxide")
                .color(0xDECAFA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(735)
                .boilingPointKelvin(1197)
                .density(0.1785063F)
                .chemicalFormula("LiOH")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lithium Hydroxide")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Lithium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("LiOH")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8034, "LithiumPerchlorate")
                .displayName("Lithium Perchlorate")
                .color(0xDEF4FA)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(509)
                .boilingPointKelvin(703)
                .density(0.09048834F)
                .chemicalFormula("LiClO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lithium Perchlorate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Lithium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("LiClO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8223, "LiveRoot")
                .displayName("LiveRoot")
                .color(0xDCC800)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(1178)
                .boilingPointKelvin(2465)
                .density(1.362F)
                .chemicalFormula("(C₆(H₂O)₅)₃Ma")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("LiveRoot")
                        .source("TwilightForest", "Twilight Forest")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Wood", 1945944000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9882, "Lubricant")
                .displayName("Lubricant")
                .color(0xFFC400)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(150)
                .boilingPointKelvin(500)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Lubricant")
                        .source("RotaryCraft", "RotaryCraft")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8709, "Lumium")
                .displayName("Lumium")
                .color(0xFFFF50)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(593)
                .boilingPointKelvin(1682)
                .density(14.441596F)
                .chemicalFormula("Sn₃Ag((Ca₅(PO₄)₃F)₅Au₃(SiO₂)He)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lumium")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Tin", 1945944000L),
                                new MaterialFacts.Part("Silver", 648648000L),
                                new MaterialFacts.Part("Glowstone", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8444, "MagentaFluorite")
                .displayName("Magenta Fluorite")
                .color(0xCC58FF)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Magenta Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("PinkFluorite", "BlueFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteMagenta")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8665, "Magnalium")
                .displayName("Magnalium")
                .color(0xC8BEFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(929)
                .boilingPointKelvin(2315)
                .density(2.378F)
                .chemicalFormula("MgAl₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Magnalium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 6.0F, 256L, 2)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("Aluminium", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8278, "ManaDiamond")
                .displayName("Mana Diamond")
                .color(0x80FFFF)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄Ma")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Mana Diamond")
                        .source("Botania", "Botania")
                        .tools(3, 10.0F, 1280L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Carbon", 2594592000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .byproducts("Graphite", "Diamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8720, "Manasteel")
                .displayName("Manasteel")
                .color(0x6EC8FA)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2311)
                .boilingPointKelvin(4134)
                .density(7.874F)
                .chemicalFormula("FeMa")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Manasteel")
                        .source("Botania", "Botania")
                        .tools(3, 12.0F, 256L, 3)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8031, "ManganeseChloride")
                .displayName("Manganese Chloride")
                .color(0xFFD5D5)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(927)
                .boilingPointKelvin(1498)
                .density(2.4821427F)
                .chemicalFormula("MnCl₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Manganese Chloride")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Manganese", 648648000L),
                                new MaterialFacts.Part("Chlorine", 1297296000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("MnCl2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8387, "Maxixe")
                .displayName("Maxixe")
                .color(0x5050FF)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Maxixe")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Emerald",
                                "Aquamarine",
                                "Morganite",
                                "Goshenite",
                                "Bixbite",
                                "Heliodor")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9701, "MeatCooked")
                .displayName("MeatCooked")
                .color(0x963C14)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(477)
                .boilingPointKelvin(550)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Meat")
                        .source("MineFactoryReloaded", "MineFactory Reloaded")
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .build())
                .build());
    }

    private static void part15(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9700, "MeatRaw")
                .displayName("MeatRaw")
                .color(0xFF6464)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(477)
                .boilingPointKelvin(550)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("MeatRaw")
                        .source("MineFactoryReloaded", "MineFactory Reloaded")
                        .smelting("MeatCooked", 648648000L)
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .processing("Forging", "MeatCooked", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9710, "MeatRotten")
                .displayName("MeatRotten")
                .color(0xFF9664)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(477)
                .boilingPointKelvin(550)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("MeatRotten")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9832, "Methane")
                .displayName("Methane")
                .color(0xFAC8FA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.4534719F)
                .chemicalFormula("CH₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Methane")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("CH4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9833, "MethaneIce")
                .displayName("Methane Ice")
                .color(0xE1C8FA)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(215)
                .boilingPointKelvin(315)
                .density(1.226736F)
                .chemicalFormula("(CH₄)(H₂O)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Methane Ice")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Methane", 648648000L),
                                new MaterialFacts.Part("Ice", 1297296000L))
                        .forms("CONTAINERS", "DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9790, "Milk")
                .displayName("Milk")
                .color(0xFEFEFE)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Milk")
                        .source("minecraft", "Minecraft")
                        .forms("CONTAINERS", "DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8445, "MilkyQuartz")
                .displayName("Milky Quartz")
                .color(0xD2D2D2)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Milky Quartz")
                        .tools(1, 2.5F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .byproducts("CertusQuartz", "Barite")
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9794, "Mint")
                .displayName("Mint")
                .color(0x96FA96)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Mint")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8338, "Monazite")
                .displayName("Monazite")
                .color(0x324632)
                .texture(MaterialTextureSet.REDSTONE)
                .meltingPointKelvin(700)
                .boilingPointKelvin(1900)
                .density(3.6860716F)
                .chemicalFormula("Rare Earth(PO₄)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Monazite")
                        .composition(
                                2L,
                                new MaterialFacts.Part("RareEarth", 648648000L),
                                new MaterialFacts.Part("Phosphate", 648648000L))
                        .byproducts("Thorium", "Neodymium", "RareEarth")
                        .multipliers(2, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8324, "Morganite")
                .displayName("Morganite")
                .color(0xFFC8C8)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1803)
                .boilingPointKelvin(2851)
                .density(0.86032146F)
                .chemicalFormula("(Al₂O₃)Be₃(SiO₂)₆O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Morganite")
                        .correctedToolStats()
                        .tools(3, 9.0F, 128L, 2)
                        .smelting("Beryllium", 18018000L)
                        .composition(
                                29L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Beryllium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts(
                                "Alumina",
                                "Beryllium",
                                "Emerald",
                                "Aquamarine",
                                "Goshenite",
                                "Bixbite",
                                "Heliodor",
                                "Maxixe")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8647, "NeodymiumMagnetic")
                .displayName("NeodymiumMagnetic")
                .color(0x646464)
                .texture(MaterialTextureSet.MAGNETIC)
                .meltingPointKelvin(1297)
                .boilingPointKelvin(3347)
                .density(7.007F)
                .chemicalFormula("Nd")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("NeodymiumMagnetic")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .correctedToolStats()
                        .tools(3, 6.0F, 512L, 3)
                        .smelting("Neodymium", 648648000L)
                        .pulverizing("Neodymium", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Neodymium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .processing("Bending", "Iron", 648648000L)
                        .processing("Compressing", "Neodymium", 648648000L)
                        .processing("Smashing", "Neodymium", 648648000L)
                        .processing("Working", "Neodymium", 648648000L)
                        .processing("Forging", "Neodymium", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8745, "Netherite")
                .displayName("Netherite")
                .color(0x504650)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2246)
                .boilingPointKelvin(3334)
                .density(81.128F)
                .chemicalFormula("Au₄Ancient₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Netherite")
                        .source("netheriteplus", "Netherite Plus")
                        .tools(2, 10.0F, 500L, 4)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Gold", 2594592000L),
                                new MaterialFacts.Part("AncientDebris", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8746, "NetherizedDiamond")
                .displayName("Netherized Diamond")
                .color(0x5A505A)
                .texture(MaterialTextureSet.DIAMOND)
                .meltingPointKelvin(2246)
                .boilingPointKelvin(3334)
                .density(23.812F)
                .chemicalFormula("(Au₄Ancient₄)(C₄)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Netherized Diamond")
                        .source("netheriteplus", "Netherite Plus")
                        .tools(3, 12.0F, 2560L, 4)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Netherite", 648648000L),
                                new MaterialFacts.Part("Diamond", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "GEMS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8346, "NetherQuartz")
                .displayName("Nether Quartz")
                .color(0xE6D2D2)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nether Quartz")
                        .source("minecraft", "Minecraft")
                        .tools(1, 2.5F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .byproducts("Barite", "Efrine", "WhiteFluorite")
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8320, "NetherStar")
                .displayName("Nether Star")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(3896)
                .boilingPointKelvin(5127)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Nether Star")
                        .source("minecraft", "Minecraft")
                        .tools(3, 8.0F, 5120L, 4)
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8663, "Nichrome")
                .displayName("Nichrome")
                .color(0xCDCEF6)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1818)
                .boilingPointKelvin(3137)
                .density(8.5596F)
                .chemicalFormula("Ni₄Cr")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nichrome")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 6.0F, 64L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Nickel", 2594592000L),
                                new MaterialFacts.Part("Chromium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part16(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8737, "NikolineAlloy")
                .displayName("Nikoline Alloy")
                .color(0x325A8C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1593)
                .boilingPointKelvin(3269)
                .density(2.3296F)
                .chemicalFormula("Si(((Al₂O₃)₃(SiO₂)Na₄Cl)₅Cu₃(SiO₂)Ar)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("TeslatineAlloy")
                        .source("Redpower", "Redpower")
                        .composition(
                                1L,
                                new MaterialFacts.Part("Silicon", 648648000L),
                                new MaterialFacts.Part("Nikolite", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8340, "Nikolite")
                .displayName("Nikolite")
                .color(0x3CB4C8)
                .texture(MaterialTextureSet.REDSTONE)
                .meltingPointKelvin(1500)
                .boilingPointKelvin(3000)
                .density(3.19592F)
                .chemicalFormula("((Al₂O₃)₃(SiO₂)Na₄Cl)₅Cu₃(SiO₂)Ar")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Electrotine")
                        .source("Redpower", "Redpower")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                10L,
                                new MaterialFacts.Part("Sodalite", 3243240000L),
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 648648000L),
                                new MaterialFacts.Part("Argon", 648648000L))
                        .byproducts("Copper", "RareEarth", "Azurite", "Arsenic")
                        .multipliers(4, 1)
                        .forms(
                                "ARMORS",
                                "DUSTS",
                                "GEMS",
                                "LENSES",
                                "MOLTEN",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8689, "NiobiumTitanium")
                .displayName("Niobium Titanium")
                .color(0x1D1D29)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(2345)
                .boilingPointKelvin(4288)
                .density(6.555F)
                .chemicalFormula("NbTi")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Niobium Titanium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Niobium", 648648000L),
                                new MaterialFacts.Part("Titanium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9825, "NitricAcid")
                .displayName("Nitric Acid")
                .color(0x80FF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(231)
                .boilingPointKelvin(356)
                .density(1.5F)
                .chemicalFormula("HNO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nitric Acid")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("HNO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9820, "NitroCarbon")
                .displayName("Nitro Carbon")
                .color(0x004B64)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1931)
                .boilingPointKelvin(2188)
                .density(1.1341254F)
                .chemicalFormula("NC")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nitro Carbon")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Carbon", 648648000L))
                        .forms("CONTAINERS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9861, "NitroFuel")
                .displayName("Nitro-Fuel")
                .color(0xC8FF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.1F)
                .chemicalFormula("(C₃H₅N₃O₉)FuelOil₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Nitro-Fuel")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Glyceryl", 648648000L),
                                new MaterialFacts.Part("Fuel", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9831, "NitrogenDioxide")
                .displayName("Nitrogen Dioxide")
                .color(0x78BEFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.0013695334F)
                .chemicalFormula("NO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nitrogen Dioxide")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("NO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9837, "NitrogenMonoxide")
                .displayName("Nitrogen Monoxide")
                .color(0x64AFFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(0.0013398F)
                .chemicalFormula("NO")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nitrogen Monoxide")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("NO")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9876, "NutOil")
                .displayName("Nut Oil")
                .color(0xEBAD46)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Nut Oil")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8214, "Obsidian")
                .displayName("Obsidian")
                .color(0x503264)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1300)
                .boilingPointKelvin(4000)
                .density(0.22316612F)
                .chemicalFormula("MgFe(SiO₂)₂O₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Obsidian")
                        .source("minecraft", "Minecraft")
                        .tools(1, 3.0F, 32L, 3)
                        .smelting("Lava", 648648000L)
                        .composition(
                                64L,
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("SiliconDioxide", 3891888000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Peridot", "MagnesiumCarbonate")
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8425, "OceanJasper")
                .displayName("Ocean Jasper")
                .color(0x8B7356)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1927)
                .boilingPointKelvin(3191)
                .density(3.1429906F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ocean Jasper")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("OceanJasper", 0L)
                        .byproducts("RedJasper", "RainforestJasper", "BlueJasper", "GreenJasper", "YellowJasper")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("JasperOcean")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9850, "Oil")
                .displayName("Oil")
                .color(0x0A0A0A)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Oil")
                        .source("BuildCraft|Core", "BuildCraft")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9877, "OliveOil")
                .displayName("Olive Oil")
                .color(0x3F9200)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Olive Oil")
                        .source("harvestcraft", "HarvestCraft")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8416, "OnyxBlack")
                .displayName("OnyxBlack")
                .color(0x323232)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Onyx")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("OnyxBlack", 0L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8415, "OnyxRed")
                .displayName("OnyxRed")
                .color(0xFF584D)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("OnyxRed")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("OnyxRed", 0L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part17(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8312, "Opal")
                .displayName("Opal")
                .color(0x0000FF)
                .texture(MaterialTextureSet.OPAL)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Opal")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("Opal", 0L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .byproducts("Tanzanite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8443, "OrangeFluorite")
                .displayName("Orange Fluorite")
                .color(0xFFBD58)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Orange Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("YellowFluorite", "RedFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteOrange")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8314, "OrangeSapphire")
                .displayName("Orange Sapphire")
                .color(0xDC9632)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2180)
                .boilingPointKelvin(3180)
                .density(2.393381F)
                .chemicalFormula("(Al₂O₃)Cu")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Orange Sapphire")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Copper", 648648000L))
                        .byproducts("Alumina", "Ruby", "GreenSapphire", "BlueSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8682, "Osmiridium")
                .displayName("Osmiridium")
                .color(0x6464FF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(3012)
                .boilingPointKelvin(4993)
                .density(22.585F)
                .chemicalFormula("OsIr")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Osmiridium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 11.0F, 3840L, 4)
                        .composition(
                                2L,
                                new MaterialFacts.Part("OsmiumElemental", 648648000L),
                                new MaterialFacts.Part("Iridium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9154, "Palygorskite")
                .displayName("Palygorskite")
                .color(0x729DB3)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(1.4148955F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("FullersEarth")
                        .source("PFAAGeologica", "Per Fabrica Ad Astra")
                        .smelting("Ceramic", 648648000L)
                        .byproducts("Kaolinite", "Bentonite")
                        .forms("DUSTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8216, "Paper")
                .displayName("Paper")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.PAPER)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Paper")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "MULTIPLATES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8360, "Peat")
                .displayName("Peat")
                .color(0x40280E)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Peat")
                        .source("Forestry", "Forestry")
                        .fuel(2000L, "Ashes", 324324000L)
                        .forms("DUSTS", "INGOTS", "MULTIINGOTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8311, "Peridot")
                .displayName("Peridot")
                .color(0x96FF96)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1525)
                .boilingPointKelvin(2460)
                .density(2.5809944F)
                .chemicalFormula("(SiO₂)₂FeMg₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Olivine")
                        .source("BiomesOPlenty", "Biomes O' Plenty")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("Peridot", 0L)
                        .composition(
                                5L,
                                new MaterialFacts.Part("SiliconDioxide", 1297296000L),
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Magnesium", 1297296000L))
                        .byproducts("Obsidian", "MagnesiumCarbonate")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8277, "PetrifiedWood")
                .displayName("Petrified Wood")
                .color(0x6E3223)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(350)
                .boilingPointKelvin(450)
                .density(1.362F)
                .chemicalFormula("(C₆(H₂O)₅)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT,
                        MaterialProperty.WOOD)
                .facts(MaterialFacts.builder()
                        .localName("Petrified Wood")
                        .source("erebus", "Erebus")
                        .tools(1, 2.0F, 24L, 1)
                        .fuel(50L, "", 0L)
                        .composition(1L, new MaterialFacts.Part("Wood", 648648000L))
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8390, "PetroleumCoke")
                .displayName("Petroleum Coke")
                .color(0x9696B4)
                .texture(MaterialTextureSet.LIGNITE)
                .meltingPointKelvin(1700)
                .boilingPointKelvin(4300)
                .density(0.929F)
                .chemicalFormula("C₂S")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PetCoke")
                        .correctedToolStats()
                        .tools(0, 1.0F, 1L, 0)
                        .fuel(6400L, "DarkAshes", 72072000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Carbon", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .forms("ARMORS", "DUSTS", "GEMS", "INGOTS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("PetCoke")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8226, "Phosphorite")
                .displayName("Phosphorite")
                .color(0x323241)
                .texture(MaterialTextureSet.DIAMOND)
                .meltingPointKelvin(758)
                .boilingPointKelvin(1252)
                .density(0.9774584F)
                .chemicalFormula("Ca₅(PO₄)₃F")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Phosphorite")
                        .composition(
                                9L,
                                new MaterialFacts.Part("Calcium", 3243240000L),
                                new MaterialFacts.Part("Phosphate", 1945944000L),
                                new MaterialFacts.Part("Fluorine", 648648000L))
                        .byproducts(
                                "Phosphorus",
                                "Apatite",
                                "YellowFluorite",
                                "Phosphate",
                                "RedPhosphorus",
                                "WhitePhosphorus")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8208, "Phosphorus")
                .displayName("Phosphorus")
                .color(0xFFFF00)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(829)
                .boilingPointKelvin(1374)
                .density(1.0700573F)
                .chemicalFormula("Ca₃(PO₄)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Phosphorous")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("Phosphate", 1297296000L))
                        .byproducts(
                                "Phosphorite",
                                "Apatite",
                                "YellowFluorite",
                                "Phosphate",
                                "RedPhosphorus",
                                "WhitePhosphorus")
                        .multipliers(3, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8424, "PinkDiamond")
                .displayName("Pink Diamond")
                .color(0xF0A0A0)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pink Diamond")
                        .tools(3, 12.0F, 1600L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .byproducts("Graphite", "Diamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("DiamondPink")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8437, "PinkFluorite")
                .displayName("Pink Fluorite")
                .color(0xE27594)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pink Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("WhiteFluorite", "RedFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoritePink")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8407, "PinkVitriol")
                .displayName("Pink Vitriol")
                .color(0xDE6F6F)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(0.6351193F)
                .chemicalFormula("MgSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pink Vitriol")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
    }

    private static void part18(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8218, "Plastic")
                .displayName("Plastic")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(423)
                .boilingPointKelvin(846)
                .density(0.7557266F)
                .chemicalFormula("CH₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Plastic")
                        .source("MineFactoryReloaded", "MineFactory Reloaded")
                        .tools(1, 3.0F, 256L, 1)
                        .smelting("Plastic", 432432000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8023, "PotassiumAluminate")
                .displayName("Potassium Aluminate")
                .color(0xFAE6E6)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1920)
                .boilingPointKelvin(3840)
                .density(0.8907145F)
                .chemicalFormula("KAlO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Potassium Aluminate")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("KAlO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8232, "PotassiumBisulfate")
                .displayName("Potassium Bisulfate")
                .color(0xFFF0F0)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(136)
                .boilingPointKelvin(304)
                .density(0.41925797F)
                .chemicalFormula("KHSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PotassiumBisulphate")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("KHSO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8020, "PotassiumCarbonate")
                .displayName("Potassium Carbonate")
                .color(0xE6E1E1)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1164)
                .boilingPointKelvin(2328)
                .density(0.66588116F)
                .chemicalFormula("K₂(CO₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Potassium Carbonate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("CarbonTrioxide", 2594592000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("K2CO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8036, "PotassiumFluoride")
                .displayName("Potassium Fluoride")
                .color(0xC840E1)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1131)
                .boilingPointKelvin(2262)
                .density(0.431848F)
                .chemicalFormula("KF")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Potassium Fluoride")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("KF")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8038, "PotassiumHeptafluorotantalate")
                .displayName("Potassium Heptafluorotantalate")
                .color(0xA4C8E1)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(433)
                .boilingPointKelvin(839)
                .density(1.8389872F)
                .chemicalFormula("K₂TaF₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Potassium Heptafluorotantalate")
                        .composition(
                                10L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Tantalum", 648648000L),
                                new MaterialFacts.Part("Fluorine", 4540536000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("K2TaF7")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8015, "PotassiumHydroxide")
                .displayName("Potassium Hydroxide")
                .color(0xFADCDC)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(633)
                .boilingPointKelvin(1600)
                .density(0.28783962F)
                .chemicalFormula("KOH")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Potassium Hydroxide")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("KOH")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8022, "PotassiumPersulfate")
                .displayName("Potassium Persulfate")
                .color(0xFAB482)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(156)
                .boilingPointKelvin(351)
                .density(0.48911932F)
                .chemicalFormula("KSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PotassiumPersulphate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("KSO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8233, "PotassiumPyrosulfate")
                .displayName("Potassium Pyrosulfate")
                .color(0xFFF0F0)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(598)
                .boilingPointKelvin(1196)
                .density(0.53345484F)
                .chemicalFormula("K₂S₂O₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PotassiumPyrosulphate")
                        .composition(
                                11L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 4540536000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("K2S2O7")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8271, "PotassiumSulfate")
                .displayName("Potassium Sulfate")
                .color(0x8CBEBE)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1342)
                .boilingPointKelvin(1962)
                .density(0.542388F)
                .chemicalFormula("K₂SO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PotassiumSulphate")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("K2SO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8272, "PotassiumSulfide")
                .displayName("Potassium Sulfide")
                .color(0x64DCDC)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(353)
                .boilingPointKelvin(927)
                .density(1.2636666F)
                .chemicalFormula("K₂S")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PotassiumSulphide")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("K2S")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8021, "PotassiumSulfite")
                .displayName("Potassium Sulfite")
                .color(0x8CBEBE)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(306)
                .boilingPointKelvin(612)
                .density(0.63254786F)
                .chemicalFormula("K₂SO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PotassiumSulphite")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("K2SO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9709, "Potato")
                .displayName("Potato")
                .color(0xF0F0A4)
                .texture(MaterialTextureSet.POWDER)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Potato")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9865, "Propane")
                .displayName("Propane")
                .color(0xFF1414)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Propane")
                        .forms("CONTAINERS", "CONTAINERS_GAS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9867, "Propylene")
                .displayName("Propylene")
                .color(0x5A3C8C)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(200)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Propylene")
                        .forms("CONTAINERS", "CONTAINERS_GAS", "PLANTS")
                        .build())
                .build());
    }

    private static void part19(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8383, "PurpleSapphire")
                .displayName("Purple Sapphire")
                .color(0xDC32FF)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2318)
                .boilingPointKelvin(3321)
                .density(1.9183812F)
                .chemicalFormula("(Al₂O₃)V")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Purple Sapphire")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Vanadium", 648648000L))
                        .byproducts("Alumina", "Ruby", "GreenSapphire", "BlueSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8197, "PVC")
                .displayName("PVC")
                .color(0xFAFA32)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(423)
                .boilingPointKelvin(846)
                .density(0.7557266F)
                .chemicalFormula("CH₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("PVC")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .tools(1, 3.0F, 256L, 1)
                        .smelting("PVC", 432432000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9127, "Pyrope")
                .displayName("Pyrope")
                .color(0x783264)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1626)
                .boilingPointKelvin(2479)
                .density(0.8807974F)
                .chemicalFormula("(Al₂O₃)Mg₃(SiO₂)₃O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("GarnetPurple")
                        .correctedToolStats()
                        .tools(3, 7.0F, 128L, 2)
                        .smelting("Pyrope", 0L)
                        .composition(
                                20L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Magnesium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 5837832000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Almandine", "Grossular", "Spessartine", "Andradite", "Uvarovite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8426, "RainforestJasper")
                .displayName("Rainforest Jasper")
                .color(0x817B37)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1927)
                .boilingPointKelvin(3191)
                .density(3.1429906F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Rainforest Jasper")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("RainforestJasper", 0L)
                        .byproducts("RedJasper", "OceanJasper", "BlueJasper", "GreenJasper", "YellowJasper")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("JasperRainforest")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8660, "RedAlloy")
                .displayName("Red Alloy")
                .color(0xC80000)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1400)
                .boilingPointKelvin(2835)
                .density(8.96F)
                .chemicalFormula("Cu((FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr))₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Alloy")
                        .source("Redpower", "Redpower")
                        .composition(
                                1L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Redstone", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8451, "RedAventurine")
                .displayName("Red Aventurine")
                .color(0x742E21)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Aventurine")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("RedAventurine", 0L)
                        .byproducts(
                                "GreenAventurine",
                                "BrownAventurine",
                                "YellowAventurine",
                                "BlackAventurine",
                                "BlueAventurine")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("AventurineRed")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8467, "RedDiamond")
                .displayName("Red Diamond")
                .color(0xF00000)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Diamond")
                        .source("ExtraPlanets", "Extra Planets")
                        .correctedToolStats()
                        .tools(3, 8.0F, 1280L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .byproducts("Graphite", "Diamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("DiamondRed")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8436, "RedFluorite")
                .displayName("Red Fluorite")
                .color(0xE23841)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("PinkFluorite", "MagentaFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteRed")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8309, "RedJasper")
                .displayName("Red Jasper")
                .color(0xC85050)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1927)
                .boilingPointKelvin(3191)
                .density(3.1429906F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Jasper")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("RedJasper", 0L)
                        .byproducts("OceanJasper", "RainforestJasper", "BlueJasper", "GreenJasper", "YellowJasper")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("Jasper")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8459, "RedPhosphorus")
                .displayName("Red Phosphorus")
                .color(0x77040E)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(829)
                .boilingPointKelvin(1374)
                .density(1.0700573F)
                .chemicalFormula("Ca₃(PO₄)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Phosphorus")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("Phosphate", 1297296000L))
                        .byproducts(
                                "Phosphorite",
                                "Apatite",
                                "RedFluorite",
                                "Phosphate",
                                "BluePhosphorus",
                                "WhitePhosphorus")
                        .multipliers(3, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("PhosphorusRed")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8395, "RedQuartz")
                .displayName("Red Quartz")
                .color(0xFFD2D2)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Quartz")
                        .source("Botania", "Botania")
                        .tools(1, 2.5F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8104, "RedSand")
                .displayName("Red Sand")
                .color(0xAA5623)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Red Sand")
                        .source("minecraft", "Minecraft")
                        .smelting("Glass", 648648000L)
                        .forms("DUSTS", "PLANTS")
                        .processing("Forging", "Glass", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8633, "RedSteel")
                .displayName("Red Steel")
                .color(0x8C6464)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1743)
                .boilingPointKelvin(3061)
                .density(9.570297F)
                .chemicalFormula("(CuAu₄)(Cu₃Zn)((Fe))₂(Ni(Cu₃(AgAu))((Fe))₃)₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Steel")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 7.5F, 1024L, 2)
                        .composition(
                                8L,
                                new MaterialFacts.Part("RoseGold", 648648000L),
                                new MaterialFacts.Part("Brass", 648648000L),
                                new MaterialFacts.Part("Steel", 1297296000L),
                                new MaterialFacts.Part("BlackSteel", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8333, "Redstone")
                .displayName("Redstone")
                .color(0xC80000)
                .texture(MaterialTextureSet.REDSTONE)
                .meltingPointKelvin(500)
                .boilingPointKelvin(1500)
                .density(6.3483334F)
                .chemicalFormula("(FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Redstone")
                        .source("minecraft", "Minecraft")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                10L,
                                new MaterialFacts.Part("Pyrite", 3243240000L),
                                new MaterialFacts.Part("Mercury", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 648648000L),
                                new MaterialFacts.Part("Ruby", 648648000L))
                        .byproducts("Cinnabar", "RareEarth", "Glowstone")
                        .multipliers(4, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8733, "RedstoneAlloy")
                .displayName("Redstone Alloy")
                .color(0x8C3232)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1093)
                .boilingPointKelvin(2519)
                .density(2.3296F)
                .chemicalFormula("Si((FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Redstone Alloy")
                        .source("EnderIO", "Ender IO")
                        .composition(
                                1L,
                                new MaterialFacts.Part("Silicon", 648648000L),
                                new MaterialFacts.Part("Redstone", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part20(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8406, "RedVitriol")
                .displayName("Red Vitriol")
                .color(0xDE4242)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.8221194F)
                .chemicalFormula("CoSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Red Vitriol")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Cobalt", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8602, "RoseGold")
                .displayName("Rose Gold")
                .color(0xFFE61E)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1341)
                .boilingPointKelvin(3070)
                .density(17.2176F)
                .chemicalFormula("CuAu₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tumbaga")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 14.0F, 128L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Gold", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8217, "Rubber")
                .displayName("Rubber")
                .color(0x141414)
                .texture(MaterialTextureSet.RUBBER)
                .meltingPointKelvin(410)
                .boilingPointKelvin(820)
                .density(0.8719784F)
                .chemicalFormula("C₅H₈")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Rubber")
                        .source("IC2", "IndustrialCraft 2")
                        .tools(1, 3.0F, 256L, 0)
                        .smelting("Rubber", 432432000L)
                        .composition(
                                13L,
                                new MaterialFacts.Part("Carbon", 3243240000L),
                                new MaterialFacts.Part("Hydrogen", 5189184000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8302, "Ruby")
                .displayName("Ruby")
                .color(0xFF6464)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2317)
                .boilingPointKelvin(3199)
                .density(2.0917144F)
                .chemicalFormula("(Al₂O₃)Cr")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ruby")
                        .source("Redpower", "Redpower")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Chromium", 648648000L))
                        .byproducts("Alumina", "OrangeSapphire", "GreenSapphire", "BlueSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9705, "Rye")
                .displayName("Rye")
                .color(0xFFE6B4)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Rye")
                        .source("harvestcraft", "HarvestCraft")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8100, "Sand")
                .displayName("Sand")
                .color(0xFAFAC8)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Sand")
                        .source("minecraft", "Minecraft")
                        .smelting("Glass", 648648000L)
                        .forms("DUSTS", "PLANTS")
                        .processing("Forging", "Glass", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8304, "Sapphire")
                .displayName("Sapphire")
                .color(0x7878A0)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2345)
                .boilingPointKelvin(3250)
                .density(0.90004784F)
                .chemicalFormula("(Al₂O₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Saphire")
                        .source("Redpower", "Redpower")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(6L, new MaterialFacts.Part("Alumina", 3243240000L))
                        .byproducts("Alumina", "Ruby", "GreenSapphire", "BlueSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9806, "SeaWater")
                .displayName("Sea Water")
                .color(0x5A5AFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Sea Water")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9872, "SeedOil")
                .displayName("Seed Oil")
                .color(0xC4FF00)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Seed Oil")
                        .source("Forestry", "Forestry")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9811, "SemiheavyWater")
                .displayName("Semiheavy Water")
                .color(0xC8C89B)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(275)
                .boilingPointKelvin(374)
                .density(1.054F)
                .chemicalFormula("HDO")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Semiheavy Water")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Deuterium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("HDO")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8708, "Signalum")
                .displayName("Signalum")
                .color(0xFF4000)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1353)
                .boilingPointKelvin(2735)
                .density(9.34525F)
                .chemicalFormula("CuAg₂(Cu((FeS₂)₅Hg₃(SiO₂)((Al₂O₃)Cr))₄)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Signalum")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .composition(
                                8L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Silver", 1297296000L),
                                new MaterialFacts.Part("RedAlloy", 3243240000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8102, "SluiceSand")
                .displayName("Sluice Sand")
                .color(0xA5A578)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Sluice Sand")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8397, "SmokeyQuartz")
                .displayName("Smokey Quartz")
                .color(0x141414)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("QuartzSmoky")
                        .source("Botania", "Botania")
                        .tools(1, 2.5F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9801, "Snow")
                .displayName("Snow")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Snow")
                        .source("minecraft", "Minecraft")
                        .smelting("Water", 648648000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8331, "Sodalite")
                .displayName("Sodalite")
                .color(0x1414FF)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(1331)
                .boilingPointKelvin(2206)
                .density(0.8599858F)
                .chemicalFormula("(Al₂O₃)₃(SiO₂)Na₄Cl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sodalite")
                        .composition(
                                11L,
                                new MaterialFacts.Part("Alumina", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L),
                                new MaterialFacts.Part("Sodium", 2594592000L),
                                new MaterialFacts.Part("Chlorine", 648648000L))
                        .byproducts("Lazurite", "Lapis")
                        .multipliers(5, 1)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "GEMS",
                                "ORES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part21(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8012, "SodiumAluminate")
                .displayName("Sodium Aluminate")
                .color(0xE6E6FA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1920)
                .boilingPointKelvin(3840)
                .density(0.9179645F)
                .chemicalFormula("NaAlO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sodium Aluminate")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("NaAlO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8230, "SodiumBisulfate")
                .displayName("Sodium Bisulfate")
                .color(0xF0F0FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(141)
                .boilingPointKelvin(321)
                .density(0.4348294F)
                .chemicalFormula("NaHSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SodiumBisulphate")
                        .source("ihl", "IHL")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("NaHSO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8013, "SodiumCarbonate")
                .displayName("Sodium Carbonate")
                .color(0xE6E6E6)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1124)
                .boilingPointKelvin(2248)
                .density(0.7022145F)
                .chemicalFormula("Na₂(CO₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sodium Carbonate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("CarbonTrioxide", 2594592000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "PLANTS")
                        .aliases("Na2CO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8037, "SodiumFluoride")
                .displayName("Sodium Fluoride")
                .color(0x40C8E1)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1266)
                .boilingPointKelvin(2532)
                .density(0.486348F)
                .chemicalFormula("NaF")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sodium Fluoride")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("NaF")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8268, "SodiumHydroxide")
                .displayName("Sodium Hydroxide")
                .color(0xDCFADC)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(596)
                .boilingPointKelvin(1661)
                .density(0.32417297F)
                .chemicalFormula("NaOH")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sodium Hydroxide")
                        .source("ihl", "IHL")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("NaOH")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9822, "SodiumPersulfate")
                .displayName("Sodium Persulfate")
                .color(0x82B4FA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(162)
                .boilingPointKelvin(372)
                .density(0.507286F)
                .chemicalFormula("NaSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SodiumPersulphate")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("NaSO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8231, "SodiumPyrosulfate")
                .displayName("Sodium Pyrosulfate")
                .color(0xF0F0FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(674)
                .boilingPointKelvin(1348)
                .density(0.553273F)
                .chemicalFormula("Na₂S₂O₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SodiumPyrosulphate")
                        .composition(
                                11L,
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 4540536000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("Na2S2O7")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8270, "SodiumSulfate")
                .displayName("Sodium Sulfate")
                .color(0xBEBE8C)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1157)
                .boilingPointKelvin(1702)
                .density(0.57353085F)
                .chemicalFormula("Na₂SO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SodiumSulphate")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("Na2SO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9823, "SodiumSulfide")
                .displayName("Sodium Sulfide")
                .color(0xDCDC64)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(376)
                .boilingPointKelvin(1009)
                .density(1.3363333F)
                .chemicalFormula("Na₂S")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SodiumSulphide")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("Na2S")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8269, "SodiumSulfite")
                .displayName("Sodium Sulfite")
                .color(0xBEBE8C)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(306)
                .boilingPointKelvin(612)
                .density(0.6688812F)
                .chemicalFormula("Na₂SO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SodiumSulphite")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("Na2SO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8669, "SolderingAlloy")
                .displayName("Soldering Alloy")
                .color(0xDCDCE6)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(544)
                .boilingPointKelvin(2773)
                .density(7.2268F)
                .chemicalFormula("Sn₉Sb")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Soldering Alloy")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                10L,
                                new MaterialFacts.Part("Tin", 5837832000L),
                                new MaterialFacts.Part("Antimony", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8732, "Soularium")
                .displayName("Soularium")
                .color(0x5A4632)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1033)
                .boilingPointKelvin(3012)
                .density(26.279373F)
                .chemicalFormula("Soulsand₉Au")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Soularium")
                        .source("EnderIO", "Ender IO")
                        .tools(3, 15.0F, 128L, 2)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Soulsand", 5837832000L),
                                new MaterialFacts.Part("Gold", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9779, "SoylentGreen")
                .displayName("Soylent Green")
                .color(0x00DE00)
                .texture(MaterialTextureSet.FOOD)
                .meltingPointKelvin(422)
                .boilingPointKelvin(500)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Soylent Green")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("DUSTS", "INGOTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9129, "Spessartine")
                .displayName("Spessartine")
                .color(0xFF6464)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1715)
                .boilingPointKelvin(2625)
                .density(1.7360975F)
                .chemicalFormula("(Al₂O₃)Mn₃(SiO₂)₃O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Garnet")
                        .correctedToolStats()
                        .tools(3, 7.0F, 128L, 2)
                        .smelting("Spessartine", 0L)
                        .composition(
                                20L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Manganese", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 5837832000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Almandine", "Grossular", "Pyrope", "Andradite", "Uvarovite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8326, "Spinel")
                .displayName("Spinel")
                .color(0x006400)
                .texture(MaterialTextureSet.GEM_VERTICAL)
                .meltingPointKelvin(1814)
                .boilingPointKelvin(2529)
                .density(1.0199594F)
                .chemicalFormula("(Al₂O₃)MgO")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Spinel")
                        .tools(3, 7.0F, 384L, 2)
                        .smelting("Spinel", 0L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Alumina", "BalasRuby")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part22(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8636, "StainlessSteel")
                .displayName("Stainless Steel")
                .color(0xC8C8DC)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1943)
                .boilingPointKelvin(3029)
                .density(7.8606668F)
                .chemicalFormula("(Fe)₄((Fe)₂Ni)CrMn")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Stainless Steel")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 7.0F, 480L, 2)
                        .composition(
                                9L,
                                new MaterialFacts.Part("WroughtIron", 2594592000L),
                                new MaterialFacts.Part("Invar", 1945944000L),
                                new MaterialFacts.Part("Chromium", 648648000L),
                                new MaterialFacts.Part("Manganese", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8402, "StannicChloride")
                .displayName("Stannic Chloride")
                .color(0xD2FAFA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.4599712F)
                .chemicalFormula("SnCl₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Stannic Chloride")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Tin", 648648000L),
                                new MaterialFacts.Part("Chlorine", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9814, "Steam")
                .displayName("Steam")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(0.001F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Steam")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8630, "Steel")
                .displayName("Steel")
                .color(0x828282)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2046)
                .boilingPointKelvin(3134)
                .density(7.874F)
                .chemicalFormula("(Fe)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.NEVER_FURNACE,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Steel")
                        .source("Railcraft", "Railcraft")
                        .tools(3, 6.0F, 512L, 2)
                        .composition(1L, new MaterialFacts.Part("WroughtIron", 648648000L))
                        .byproducts("Hematite")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8673, "Steeleaf")
                .displayName("Steeleaf")
                .color(0x327F32)
                .texture(MaterialTextureSet.LEAF)
                .meltingPointKelvin(1023)
                .boilingPointKelvin(1567)
                .density(7.874F)
                .chemicalFormula("((Fe))Ma")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Steeleaf")
                        .source("TwilightForest", "Twilight Forest")
                        .tools(2, 8.0F, 144L, 3)
                        .smelting("Steel", 162162000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Steel", 648648000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8651, "SteelGalvanized")
                .displayName("SteelGalvanized")
                .color(0xFAF0F0)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1910)
                .boilingPointKelvin(2938)
                .density(8.666667F)
                .chemicalFormula("((Fe))₉Zn")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("SteelGalvanized")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 7.0F, 768L, 2)
                        .smelting("Steel", 648648000L)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Steel", 5837832000L),
                                new MaterialFacts.Part("Zinc", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .processing("Forging", "Steel", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8646, "SteelMagnetic")
                .displayName("SteelMagnetic")
                .color(0x808080)
                .texture(MaterialTextureSet.MAGNETIC)
                .meltingPointKelvin(2046)
                .boilingPointKelvin(3134)
                .density(7.874F)
                .chemicalFormula("((Fe))")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ELEMENT,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SteelMagnetic")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .correctedToolStats()
                        .tools(3, 6.0F, 512L, 2)
                        .smelting("Steel", 648648000L)
                        .pulverizing("Steel", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Steel", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .processing("Bending", "Steel", 648648000L)
                        .processing("Compressing", "Steel", 648648000L)
                        .processing("Smashing", "Steel", 648648000L)
                        .processing("Working", "Steel", 648648000L)
                        .processing("Forging", "Steel", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8601, "SterlingSilver")
                .displayName("Sterling Silver")
                .color(0xFADCE1)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1258)
                .boilingPointKelvin(2515)
                .density(10.1928F)
                .chemicalFormula("CuAg₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sterling Silver")
                        .source("terrafirmacraft", "TerraFirmaCraft")
                        .tools(3, 13.0F, 128L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Silver", 2594592000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9703, "Sugar")
                .displayName("Sugar")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(459)
                .boilingPointKelvin(918)
                .density(0.6049266F)
                .chemicalFormula("C₁₂H₂₂O₁₁")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sugar")
                        .source("minecraft", "Minecraft")
                        .composition(
                                45L,
                                new MaterialFacts.Part("Carbon", 7783776000L),
                                new MaterialFacts.Part("Hydrogen", 14270256000L),
                                new MaterialFacts.Part("Oxygen", 7135128000L))
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9824, "SulfuricAcid")
                .displayName("Sulfuric Acid")
                .color(0xFF8000)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.5F)
                .chemicalFormula("H₂SO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("SulphuricAcid")
                        .source("factorization", "Factorization")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("H2SO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9875, "SunflowerOil")
                .displayName("Sunflower Oil")
                .color(0xD8BD11)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Sunflower Oil")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8393, "SunnyQuartz")
                .displayName("Sunny Quartz")
                .color(0xFFFFC8)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sunny Quartz")
                        .source("Botania", "Botania")
                        .tools(1, 2.5F, 32L, 3)
                        .smelting("SiliconDioxide", 648648000L)
                        .composition(1L, new MaterialFacts.Part("SiliconDioxide", 648648000L))
                        .multipliers(2, 1)
                        .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9516, "Superconductor")
                .displayName("Superconductor")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder().localName("Superconductor").build())
                .build());
        result.add(MaterialDefinition.builder(8802, "TantalumHafniumCarbide")
                .displayName("Tantalum Hafnium Carbide")
                .color(0x208020)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(4263)
                .boilingPointKelvin(8526)
                .density(9.1261F)
                .chemicalFormula("Ta₄HfC₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tantalum Hafnium Carbide")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(0, 1.0F, 1L, 2)
                        .composition(
                                10L,
                                new MaterialFacts.Part("Tantalum", 2594592000L),
                                new MaterialFacts.Part("Hafnium", 648648000L),
                                new MaterialFacts.Part("Carbon", 3243240000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("Ta4HfC5")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8308, "Tanzanite")
                .displayName("Tanzanite")
                .color(0x4000C8)
                .texture(MaterialTextureSet.GEM_HORIZONTAL)
                .meltingPointKelvin(1736)
                .boilingPointKelvin(2618)
                .density(0.8945756F)
                .chemicalFormula("(Al₂O₃)₃(SiO₂)₆Ca₄(H₂O)O₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tanzanite")
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("Tanzanite", 0L)
                        .composition(
                                44L,
                                new MaterialFacts.Part("Alumina", 9729720000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Calcium", 2594592000L),
                                new MaterialFacts.Part("Water", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Opal")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part23(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9792, "Tea")
                .displayName("Tea")
                .color(0x64FA64)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tea")
                        .forms("DUSTS", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8196, "Teflon")
                .displayName("Teflon")
                .color(0x505050)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(423)
                .boilingPointKelvin(846)
                .density(0.7557266F)
                .chemicalFormula("CH₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Polymer")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .tools(1, 3.0F, 256L, 1)
                        .smelting("Teflon", 432432000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 1297296000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS",
                                "WIRES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8721, "Terrasteel")
                .displayName("Terrasteel")
                .color(0x6EC832)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2561)
                .boilingPointKelvin(4634)
                .density(7.874F)
                .chemicalFormula("Fe")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Terrasteel")
                        .source("Botania", "Botania")
                        .tools(3, 16.0F, 2048L, 4)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8679, "Thaumium")
                .displayName("Thaumium")
                .color(0x9664C8)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2311)
                .boilingPointKelvin(4134)
                .density(7.874F)
                .chemicalFormula("FeMa")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("Thaumium")
                        .source("Thaumcraft", "Thaumcraft")
                        .tools(3, 12.0F, 256L, 3)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Magic", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "ORES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8430, "TigerEye")
                .displayName("Tiger Eye")
                .color(0x8E743D)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("YellowTigerEye")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("TigerEye", 0L)
                        .byproducts("Hematite", "CatsEye", "DragonEye", "HawksEye", "BlackEye", "TigerIron")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("TigerEyeYellow")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8435, "TigerIron")
                .displayName("Tiger Iron")
                .color(0x6A5642)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tiger Iron")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("TigerIron", 0L)
                        .byproducts("Hematite", "TigerEye", "CatsEye", "DragonEye", "HawksEye", "BlackEye")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8667, "TinAlloy")
                .displayName("Tin Alloy")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1258)
                .boilingPointKelvin(3004)
                .density(7.5805F)
                .chemicalFormula("Sn(Fe)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tin Alloy")
                        .source("Railcraft", "Railcraft")
                        .tools(3, 6.5F, 96L, 2)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Tin", 648648000L),
                                new MaterialFacts.Part("WroughtIron", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8793, "TitaniumIridium")
                .displayName("Titanium Iridium")
                .color(0xDCDCFF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2330)
                .boilingPointKelvin(4130)
                .density(13.55F)
                .chemicalFormula("IrTi")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Iritanium")
                        .source("advancedRocketry", "Advanced Rocketry")
                        .tools(3, 8.0F, 7680L, 4)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Iridium", 648648000L),
                                new MaterialFacts.Part("Titanium", 648648000L))
                        .aliases("Iritanium")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8413, "TitaniumTetrachloride")
                .displayName("Titanium Tetrachloride")
                .color(0xE9F4DE)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(249)
                .boilingPointKelvin(409)
                .density(0.9105712F)
                .chemicalFormula("TiCl₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Titanium Tetrachloride")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Titanium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("TiCl4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8306, "Topaz")
                .displayName("Topaz")
                .color(0xFF8000)
                .texture(MaterialTextureSet.GEM_HORIZONTAL)
                .meltingPointKelvin(1431)
                .boilingPointKelvin(2092)
                .density(0.8258567F)
                .chemicalFormula("(Al₂O₃)(SiO₂)F₂(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Topaz")
                        .source("BiomesOPlenty", "Biomes O' Plenty")
                        .tools(3, 7.0F, 256L, 3)
                        .smelting("Topaz", 0L)
                        .composition(
                                13L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L),
                                new MaterialFacts.Part("Fluorine", 1297296000L),
                                new MaterialFacts.Part("Water", 1945944000L))
                        .byproducts("BlueTopaz")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8684, "Trinaquadalloy")
                .displayName("Trinaquadalloy")
                .color(0x92BA92)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2518)
                .boilingPointKelvin(4159)
                .density(5.6310487F)
                .chemicalFormula("Ke₆Nq₂C")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("NaquadahAlloy")
                        .tools(3, 14.0F, 20480L, 5)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Trinium", 3891888000L),
                                new MaterialFacts.Part("Naquadah", 1297296000L),
                                new MaterialFacts.Part("Carbon", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8790, "Trinitanium")
                .displayName("Trinitanium")
                .color(0xEBAFFF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2410)
                .boilingPointKelvin(4202)
                .density(2.2258267F)
                .chemicalFormula("Ke₂Ti")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Trinitanium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 16.0F, 5120L, 4)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Trinium", 1297296000L),
                                new MaterialFacts.Part("Titanium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8752, "Tritanium")
                .displayName("Tritanium")
                .color(0x379B9B)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1800)
                .boilingPointKelvin(2976)
                .density(23.75F)
                .chemicalFormula("Tn₃Dn")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium")
                        .source("mo", "Matter Overdrive")
                        .tools(3, 12.0F, 2560L, 5)
                        .composition(
                                4L,
                                new MaterialFacts.Part("TritaniumElemental", 1945944000L),
                                new MaterialFacts.Part("DuraniumElemental", 648648000L))
                        .byproducts("DiduraniumTrioxide", "Trinium")
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .aliases("TritaniumAlloy")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9813, "TritiatedWater")
                .displayName("Tritiated Water")
                .color(0xFF6464)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(280)
                .boilingPointKelvin(377)
                .density(1.2112F)
                .chemicalFormula("T₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Tritiated Water")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Tritium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .aliases("T2O")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8638, "TungstenCarbide")
                .displayName("Tungsten Carbide")
                .color(0x7B7B7B)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(3070)
                .boilingPointKelvin(6270)
                .density(15.6F)
                .chemicalFormula("WC")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Carbide")
                        .source("ReactorCraft", "ReactorCraft")
                        .tools(3, 10.0F, 5120L, 4)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Tungsten", 648648000L),
                                new MaterialFacts.Part("Carbon", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part24(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8652, "TungstenSintered")
                .displayName("TungstenSintered")
                .color(0x464646)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(3695)
                .boilingPointKelvin(5828)
                .density(19.25F)
                .chemicalFormula("W")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("TungstenSintered")
                        .source("RotaryCraft", "RotaryCraft")
                        .tools(3, 8.0F, 5120L, 3)
                        .composition(1L, new MaterialFacts.Part("Tungsten", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .processing("Forging", "TungstenSintered", 648648000L)
                        .processing("Cutting", "TungstenSintered", 648648000L)
                        .processing("Working", "TungstenSintered", 648648000L)
                        .processing("Smashing", "TungstenSintered", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8635, "Tungstensteel")
                .displayName("Tungstensteel")
                .color(0x6464A0)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2870)
                .boilingPointKelvin(4481)
                .density(13.562F)
                .chemicalFormula("((Fe))W")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("TungstenSteel")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 10.0F, 5120L, 4)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Steel", 648648000L),
                                new MaterialFacts.Part("Tungsten", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "RAILS",
                                "STICKS")
                        .aliases("TungstenSteel")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8027, "TungsticAcid")
                .displayName("Tungstic Acid")
                .color(0xBCC800)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(373)
                .boilingPointKelvin(1746)
                .density(2.7508423F)
                .chemicalFormula("H₂WO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tungstic Acid")
                        .smelting("TungstenTrioxide", 370656000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Tungsten", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("DUSTS", "PLANTS")
                        .aliases("H2WO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8666, "Ultimet")
                .displayName("Ultimet")
                .color(0xB4B4E6)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1980)
                .boilingPointKelvin(3331)
                .density(8.6368885F)
                .chemicalFormula("Co₅NiCr₂Mo")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ultimet")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 8.0F, 1024L, 3)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Cobalt", 3243240000L),
                                new MaterialFacts.Part("Nickel", 648648000L),
                                new MaterialFacts.Part("Chromium", 1297296000L),
                                new MaterialFacts.Part("Molybdenum", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9012, "Uranium235Hexafluoride")
                .displayName("Uranium-235 Hexafluoride")
                .color(0x426255)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(329)
                .density(2.7085965F)
                .chemicalFormula("U-235F₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Uranium-235 Hexafluoride")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Uranium235", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("U235F6")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9011, "Uranium235Tetrafluoride")
                .displayName("Uranium-235 Tetrafluoride")
                .color(0x567669)
                .texture(MaterialTextureSet.SHARDS)
                .meltingPointKelvin(1309)
                .boilingPointKelvin(1690)
                .density(3.7913568F)
                .chemicalFormula("U-235F₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Uranium-235 Tetrafluoride")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Uranium235", 648648000L),
                                new MaterialFacts.Part("Fluorine", 2594592000L))
                        .forms("DUSTS", "MOLTEN", "PLANTS")
                        .aliases("U235F4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9010, "Uranium238Hexafluoride")
                .displayName("Uranium-238 Hexafluoride")
                .color(0x426255)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(329)
                .density(2.7085965F)
                .chemicalFormula("UF₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Uranium-238 Hexafluoride")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Uranium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("U238F6")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9009, "Uranium238Tetrafluoride")
                .displayName("Uranium-238 Tetrafluoride")
                .color(0x567669)
                .texture(MaterialTextureSet.SHARDS)
                .meltingPointKelvin(1309)
                .boilingPointKelvin(1690)
                .density(3.7913568F)
                .chemicalFormula("UF₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Uranium-238 Tetrafluoride")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Uranium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 2594592000L))
                        .forms("DUSTS", "MOLTEN", "PLANTS")
                        .aliases("U238F4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9008, "UraniumHexafluoride")
                .displayName("Uranium Hexafluoride")
                .color(0x426255)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(100)
                .boilingPointKelvin(329)
                .density(2.7085965F)
                .chemicalFormula("UF₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DECOMPOSABLE, MaterialProperty.GAS, MaterialProperty.LIQUID)
                .facts(MaterialFacts.builder()
                        .localName("Uranium Hexafluoride")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Uranium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .forms("CONTAINERS", "CONTAINERS_GAS", "GASES", "PLANTS")
                        .aliases("UF6")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9007, "UraniumTetrafluoride")
                .displayName("Uranium Tetrafluoride")
                .color(0x567669)
                .texture(MaterialTextureSet.SHARDS)
                .meltingPointKelvin(1309)
                .boilingPointKelvin(1690)
                .density(3.7913568F)
                .chemicalFormula("UF₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Uranium Tetrafluoride")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Uranium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 2594592000L))
                        .forms("DUSTS", "MOLTEN", "PLANTS")
                        .aliases("UF4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9135, "Uvarovite")
                .displayName("Uvarovite")
                .color(0xB4FFB4)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1295)
                .boilingPointKelvin(2033)
                .density(1.2962974F)
                .chemicalFormula("Ca₃Cr₂(SiO₂)₃O₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("GarnetGreen")
                        .correctedToolStats()
                        .tools(3, 7.0F, 128L, 2)
                        .smelting("Uvarovite", 0L)
                        .composition(
                                20L,
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("Chromium", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 5837832000L),
                                new MaterialFacts.Part("Oxygen", 3891888000L))
                        .byproducts("Almandine", "Grossular", "Pyrope", "Spessartine", "Andradite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8686, "VanadiumGallium")
                .displayName("Vanadium-Gallium")
                .color(0x80808C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1712)
                .boilingPointKelvin(3379)
                .density(6.05925F)
                .chemicalFormula("V₃Ga")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Vanadium-Gallium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Vanadium", 1945944000L),
                                new MaterialFacts.Part("Gallium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8653, "VanadiumSteel")
                .displayName("VanadiumSteel")
                .color(0x646464)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2073)
                .boilingPointKelvin(3243)
                .density(7.5212F)
                .chemicalFormula("((Fe))₄V")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("VanadiumSteel")
                        .tools(3, 7.0F, 512L, 3)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Steel", 2594592000L),
                                new MaterialFacts.Part("Vanadium", 648648000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MOLTEN",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8706, "Vibramantium")
                .displayName("Vibramantium")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(5131)
                .boilingPointKelvin(13249)
                .density(10.827131F)
                .chemicalFormula("VbAd₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Vibramantium")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 1000.0F, 5120L, 15)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Vibranium", 648648000L),
                                new MaterialFacts.Part("Adamantium", 1945944000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8705, "VibraniumSilver")
                .displayName("Vibranium Silver")
                .color(0xF0F0FF)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2138)
                .boilingPointKelvin(4180)
                .density(8.685696F)
                .chemicalFormula("VbAg₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Vibranium Silver")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 100.0F, 512L, 9)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Vibranium", 648648000L),
                                new MaterialFacts.Part("Silver", 1945944000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
    }

    private static void part25(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8704, "VibraniumSteel")
                .displayName("Vibranium Steel")
                .color(0x281832)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2747)
                .boilingPointKelvin(4704)
                .density(6.715446F)
                .chemicalFormula("Vb((Fe))₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Vibranium Steel")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .tools(3, 50.0F, 2048L, 10)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Vibranium", 648648000L),
                                new MaterialFacts.Part("Steel", 1945944000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PARTS",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8726, "VibrantAlloy")
                .displayName("Vibrant Alloy")
                .color(0xA0AA46)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1722)
                .boilingPointKelvin(2763)
                .density(32.511856F)
                .chemicalFormula("EnergeticAlloyEnderPearl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD)
                .facts(MaterialFacts.builder()
                        .localName("PhasedGold")
                        .source("EnderIO", "Ender IO")
                        .correctedToolStats()
                        .tools(3, 12.5F, 64L, 2)
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8412, "VitriolOfClay")
                .displayName("Vitriol Of Clay")
                .color(0x42DEDE)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(0.68318516F)
                .chemicalFormula("(Al₂O₃)S₃O₉")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Vitriol Of Clay")
                        .composition(
                                17L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Sulfur", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 5837832000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9800, "Water")
                .displayName("Water")
                .color(0x6464FF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Water")
                        .source("minecraft", "Minecraft")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .aliases("H2O")
                        .processing("Solidifying", "Ice", 648648000L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9808, "WaterDistilled")
                .displayName("WaterDistilled")
                .color(0x6E6EFF)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(273)
                .boilingPointKelvin(373)
                .density(1.0F)
                .chemicalFormula("H₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.GAS,
                        MaterialProperty.LIQUID,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("WaterDistilled")
                        .source("IC2", "IndustrialCraft 2")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "PLANTS")
                        .aliases("DistWater")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8235, "Wax")
                .displayName("Wax")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(350)
                .boilingPointKelvin(700)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.INGOT,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Wax")
                        .source("Forestry", "Forestry")
                        .forms("DUSTS", "FOILS", "INGOTS", "PARTS", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8236, "WaxBee")
                .displayName("WaxBee")
                .color(0xFADC6E)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(350)
                .boilingPointKelvin(700)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.INGOT,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("BeesWax")
                        .source("Forestry", "Forestry")
                        .forms("DUSTS", "FOILS", "INGOTS", "PARTS", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8238, "WaxParaffin")
                .displayName("WaxParaffin")
                .color(0xD2D2FA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(400)
                .boilingPointKelvin(800)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.INGOT,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("ParaffinWax")
                        .forms("DUSTS", "FOILS", "INGOTS", "PARTS", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8239, "WaxPlant")
                .displayName("WaxPlant")
                .color(0xD2FAD2)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(350)
                .boilingPointKelvin(700)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_NUGGET,
                        MaterialProperty.INGOT,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("WaxPlant")
                        .source("harvestcraft", "HarvestCraft")
                        .forms("DUSTS", "FOILS", "INGOTS", "PARTS", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9702, "Wheat")
                .displayName("Wheat")
                .color(0xFFFFC4)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Flour")
                        .source("minecraft", "Minecraft")
                        .forms("DUSTS", "PLANTS")
                        .aliases("Flour")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8441, "WhiteFluorite")
                .displayName("White Fluorite")
                .color(0xB4B4B4)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("White Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("BlackFluorite", "Fluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteWhite")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8460, "WhitePhosphorus")
                .displayName("White Phosphorus")
                .color(0xECEADD)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(829)
                .boilingPointKelvin(1374)
                .density(1.0700573F)
                .chemicalFormula("Ca₃(PO₄)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("White Phosphorus")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Calcium", 1945944000L),
                                new MaterialFacts.Part("Phosphate", 1297296000L))
                        .byproducts(
                                "Phosphorite",
                                "Apatite",
                                "WhiteFluorite",
                                "Phosphate",
                                "RedPhosphorus",
                                "BluePhosphorus")
                        .multipliers(3, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("PhosphorusWhite")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8409, "WhiteVitriol")
                .displayName("White Vitriol")
                .color(0xDEDEDE)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(200)
                .boilingPointKelvin(400)
                .density(1.5344527F)
                .chemicalFormula("ZnSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("White Vitriol")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Zinc", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .forms("CONTAINERS", "CONTAINERS_FLUID", "LIQUID", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8447, "YellowAventurine")
                .displayName("Yellow Aventurine")
                .color(0xC8B379)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Yellow Aventurine")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("YellowAventurine", 0L)
                        .byproducts(
                                "GreenAventurine",
                                "BrownAventurine",
                                "BlackAventurine",
                                "BlueAventurine",
                                "RedAventurine")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("AventurineYellow")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8468, "YellowDiamond")
                .displayName("Yellow Diamond")
                .color(0xF0F000)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(4200)
                .boilingPointKelvin(4300)
                .density(3.53F)
                .chemicalFormula("C₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Yellow Diamond")
                        .source("ExtraPlanets", "Extra Planets")
                        .correctedToolStats()
                        .tools(3, 8.0F, 1280L, 3)
                        .smelting("Carbon", 1297296000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 2594592000L))
                        .byproducts("Graphite", "Diamond")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("DiamondYellow")
                        .build())
                .build());
    }

    private static void part26(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8442, "YellowFluorite")
                .displayName("Yellow Fluorite")
                .color(0xCEB650)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1633)
                .boilingPointKelvin(3266)
                .density(0.514464F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Yellow Fluorite")
                        .smelting("Fluorite", 648648000L)
                        .byproducts("GreenFluorite", "OrangeFluorite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("FluoriteYellow")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8429, "YellowJasper")
                .displayName("Yellow Jasper")
                .color(0x9C8027)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(1927)
                .boilingPointKelvin(3191)
                .density(3.1429906F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Yellow Jasper")
                        .correctedToolStats()
                        .tools(3, 7.0F, 256L, 2)
                        .smelting("YellowJasper", 0L)
                        .byproducts("RedJasper", "OceanJasper", "RainforestJasper", "BlueJasper", "GreenJasper")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("JasperYellow")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8315, "YellowSapphire")
                .displayName("Yellow Sapphire")
                .color(0xDCDC32)
                .texture(MaterialTextureSet.RUBY)
                .meltingPointKelvin(2294)
                .boilingPointKelvin(3301)
                .density(1.6571908F)
                .chemicalFormula("(Al₂O₃)(TiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.BOULE,
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Yellow Sapphire")
                        .correctedToolStats()
                        .tools(3, 7.0F, 512L, 3)
                        .smelting("Alumina", 486486000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Rutile", 648648000L))
                        .byproducts("Alumina", "Ruby", "GreenSapphire", "BlueSapphire")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8687, "YttriumBariumCuprate")
                .displayName("Yttrium-Barium-Cuprate")
                .color(0x504046)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1200)
                .boilingPointKelvin(2400)
                .density(6.4245005F)
                .chemicalFormula("YBa₂Cu₃O₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ALLOY,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DENSEPLATE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_INGOT,
                        MaterialProperty.GENERATE_MULTIINGOT,
                        MaterialProperty.GENERATE_MULTIPLATE,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_RAIL,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.GENERATE_WIRE,
                        MaterialProperty.INGOT,
                        MaterialProperty.MELTING,
                        MaterialProperty.METAL,
                        MaterialProperty.SMITHABLE,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Yttrium-Barium-Cuprate")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Yttrium", 648648000L),
                                new MaterialFacts.Part("Barium", 1297296000L),
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 4540536000L))
                        .forms(
                                "ARMORS",
                                "DENSEPLATES",
                                "DUSTS",
                                "FOILS",
                                "INGOTS",
                                "INGOTS_HOT",
                                "MULTIINGOTS",
                                "MULTIPLATES",
                                "PLANTS",
                                "PLATES",
                                "PROJECTILES",
                                "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8419, "Zircon")
                .displayName("Zircon")
                .color(0x63181D)
                .texture(MaterialTextureSet.EMERALD)
                .meltingPointKelvin(1365)
                .boilingPointKelvin(2420)
                .density(1.4735527F)
                .chemicalFormula("Zr(SiO₂)O₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GEM,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_LENS,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PARTS,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.TOOL_HEAD,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Zircon")
                        .source("tropicraft", "Tropicraft")
                        .tools(3, 8.0F, 384L, 2)
                        .smelting("Zirconium", 72072000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Zirconium", 648648000L),
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Rutile", "Hafnium", "Uraninite")
                        .multipliers(2, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }
}
