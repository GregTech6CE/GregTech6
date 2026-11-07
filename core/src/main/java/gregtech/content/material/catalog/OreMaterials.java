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
public final class OreMaterials {
    private OreMaterials() {}

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
        return List.copyOf(result);
    }

    private static void part0(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8008, "Alumina")
                .displayName("Alumina")
                .color(0x78C3EB)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2345)
                .boilingPointKelvin(3250)
                .density(1.0800574F)
                .chemicalFormula("Al₂O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("NaturalAluminum")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Aluminium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Bauxite", "Alumina", "AluminiumHydroxide")
                        .forms("DUSTS", "INGOTS", "ORES", "PLANTS")
                        .aliases("Al2O3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8014, "AluminiumHydroxide")
                .displayName("Aluminium Hydroxide")
                .color(0xBEBEC8)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(573)
                .boilingPointKelvin(1146)
                .density(0.38607952F)
                .chemicalFormula("AlO₃H₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Gibbsite")
                        .smelting("Alumina", 231660000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L),
                                new MaterialFacts.Part("Hydrogen", 1945944000L))
                        .byproducts("Bauxite", "Ilmenite", "Hematite", "Alumina")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("AlO3H3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9162, "Alunite")
                .displayName("Alunite")
                .color(0xE1B441)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(740)
                .boilingPointKelvin(1114)
                .density(0.67583454F)
                .chemicalFormula("(Al₂O₃)₃(KOH)₂(SO₃)₄(H₂O)₅O₉")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Alunite")
                        .composition(
                                61L,
                                new MaterialFacts.Part("Alumina", 9729720000L),
                                new MaterialFacts.Part("PotassiumHydroxide", 3891888000L),
                                new MaterialFacts.Part("SulfurTrioxide", 10378368000L),
                                new MaterialFacts.Part("Water", 9729720000L),
                                new MaterialFacts.Part("Oxygen", 5837832000L))
                        .byproducts("Quartzite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9216, "Arsenopyrite")
                .displayName("Arsenopyrite")
                .color(0xFAF01E)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(1028)
                .boilingPointKelvin(1647)
                .density(5.239F)
                .chemicalFormula("FeAsS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Arsenopyrite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Arsenic", 162162000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Arsenic", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .byproducts("Gold", "Realgar", "OrangeFluorite", "Cassiterite", "Huebnerite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9103, "Asbestos")
                .displayName("Asbestos")
                .color(0xE6E6E6)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(915)
                .boilingPointKelvin(1439)
                .density(0.88240016F)
                .chemicalFormula("Mg₃(SiO₂)₂(H₂O)₂O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Chrysotile")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .composition(
                                18L,
                                new MaterialFacts.Part("Magnesium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 3891888000L),
                                new MaterialFacts.Part("Water", 3891888000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Mica", "Biotite", "Talc", "Jade")
                        .forms("DUSTS", "INGOTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9160, "Barite")
                .displayName("Barite")
                .color(0xE6EBFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1853)
                .boilingPointKelvin(3706)
                .density(0.94445264F)
                .chemicalFormula("BaSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Barite")
                        .smelting("Barium", 72072000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Barium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("CertusQuartz", "Quartzite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9003, "BasalticMineralSand")
                .displayName("Basaltic Mineral Sand")
                .color(0x283228)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1811)
                .boilingPointKelvin(3622)
                .density(3.375388F)
                .chemicalFormula("Fe₃O₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Basaltic Mineral Sand")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Iron", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Hematite", "Gold", "Basalt")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9144, "Bastnasite")
                .displayName("Bastnasite")
                .color(0xC86E2D)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(847)
                .boilingPointKelvin(1395)
                .density(1.5071639F)
                .chemicalFormula("CeCFO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Bastnasite")
                        .smelting("Cerium", 72072000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Cerium", 648648000L),
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Fluorine", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Monazite", "RareEarth", "Neodymium")
                        .multipliers(3, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9105, "Bauxite")
                .displayName("Bauxite")
                .color(0xC86400)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(2800)
                .boilingPointKelvin(5600)
                .density(2.3340576F)
                .chemicalFormula("(TiO₂)(FeTiO₃)₂(Al₂O₃)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Bauxite")
                        .tools(0, 1.0F, 1L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Rutile", 648648000L),
                                new MaterialFacts.Part("Ilmenite", 1297296000L),
                                new MaterialFacts.Part("Alumina", 1297296000L))
                        .byproducts("Kaolinite", "Ilmenite", "Hematite", "Alumina", "AluminiumHydroxide")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9141, "Biotite")
                .displayName("Biotite")
                .color(0x141E14)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1718)
                .boilingPointKelvin(2606)
                .density(0.9412087F)
                .chemicalFormula("K₂Mg₆(Al₂O₃)₃F₄(SiO₂)₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Biotite")
                        .composition(
                                45L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Magnesium", 3891888000L),
                                new MaterialFacts.Part("Alumina", 9729720000L),
                                new MaterialFacts.Part("Fluorine", 2594592000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L))
                        .byproducts("Mica", "Vermiculite", "Asbestos", "Perlite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9221, "Bischofite")
                .displayName("Bischofite")
                .color(0x636876)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(511)
                .boilingPointKelvin(810)
                .density(2.581476F)
                .chemicalFormula("(MgCl₂)(H₂O)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Bischofite")
                        .composition(
                                3L,
                                new MaterialFacts.Part("MagnesiumChloride", 1945944000L),
                                new MaterialFacts.Part("Water", 3891888000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8219, "Bone")
                .displayName("Bone")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1115)
                .boilingPointKelvin(1757)
                .density(0.1925F)
                .chemicalFormula("Ca")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Fossil")
                        .source("minecraft", "Minecraft")
                        .tools(1, 4.0F, 64L, 1)
                        .composition(8L, new MaterialFacts.Part("Calcium", 648648000L))
                        .multipliers(4, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9139, "Borax")
                .displayName("Borax")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(434)
                .boilingPointKelvin(719)
                .density(0.96074426F)
                .chemicalFormula("Na₂B₄(H₂O)₁₀O₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Borax")
                        .composition(
                                43L,
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("Boron", 2594592000L),
                                new MaterialFacts.Part("Water", 19459440000L),
                                new MaterialFacts.Part("Oxygen", 4540536000L))
                        .byproducts("Boron", "Salt")
                        .multipliers(2, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9210, "Bromargyrite")
                .displayName("Bromargyrite")
                .color(0x5A2D0A)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(749)
                .boilingPointKelvin(1383)
                .density(6.8115F)
                .chemicalFormula("AgBr")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Bromargyrite")
                        .smelting("Silver", 216216000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Silver", 648648000L),
                                new MaterialFacts.Part("Bromine", 648648000L))
                        .byproducts("Pyrolusite", "Silver", "Chromite", "Smithsonite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9106, "BrownLimonite")
                .displayName("Brown Limonite")
                .color(0xC86400)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1523)
                .boilingPointKelvin(3046)
                .density(1.969237F)
                .chemicalFormula("FeHO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Brown Limonite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Hematite", 324324000L)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Malachite", "YellowLimonite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
    }

    private static void part1(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9107, "Calcite")
                .displayName("Calcite")
                .color(0xFAE6DC)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1612)
                .boilingPointKelvin(3000)
                .density(0.7622574F)
                .chemicalFormula("Ca(CO₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Valerite")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("CarbonTrioxide", 2594592000L))
                        .byproducts("Malachite")
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("CaCO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8028, "CalciumChloride")
                .displayName("Calcium Chloride")
                .color(0xEBEBFA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1048)
                .boilingPointKelvin(2208)
                .density(0.515476F)
                .chemicalFormula("CaCl₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Calcium Chloride")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 1297296000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("CaCl2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9108, "Cassiterite")
                .displayName("Cassiterite")
                .color(0xDCDCDC)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(757)
                .boilingPointKelvin(1514)
                .density(7.289858F)
                .chemicalFormula("SnO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("CassiteriteSand")
                        .smelting("Tin", 486486000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Tin", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts(
                                "Molybdenite",
                                "Wolframite",
                                "WhiteFluorite",
                                "Arsenopyrite",
                                "Stannite",
                                "Sperrylite",
                                "Huebnerite",
                                "Apatite")
                        .multipliers(2, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9110, "Celestine")
                .displayName("Celestine")
                .color(0xC8CDF0)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(275)
                .boilingPointKelvin(455)
                .density(0.78545266F)
                .chemicalFormula("SrSO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Celestine")
                        .smelting("Strontium", 72072000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Strontium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Strontium", "Sulfur")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9111, "Chalcopyrite")
                .displayName("Chalcopyrite")
                .color(0xA07828)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(986)
                .boilingPointKelvin(1850)
                .density(5.242F)
                .chemicalFormula("CuFeS₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Chalcopyrite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Copper", 144144000L)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Copper", 648648000L),
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L))
                        .byproducts("Pyrite", "Cobaltite", "Cadmium", "Gold", "Sperrylite", "Stannite", "Indium")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9113, "Chromite")
                .displayName("Chromite")
                .color(0x23140F)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(912)
                .boilingPointKelvin(1340)
                .density(3.168531F)
                .chemicalFormula("FeCr₂O₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Chromite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Chromium", 144144000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Chromium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Pyrolusite", "Hematite", "MagnesiumCarbonate", "Bromargyrite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9114, "Cinnabar")
                .displayName("Cinnabar")
                .color(0x960000)
                .texture(MaterialTextureSet.REDSTONE)
                .meltingPointKelvin(311)
                .boilingPointKelvin(673)
                .density(7.8003F)
                .chemicalFormula("HgS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cinnabar")
                        .source("Thaumcraft", "Thaumcraft")
                        .smelting("Mercury", 216216000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Mercury", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .byproducts("Redstone", "Sulfur", "Glowstone", "Selenium")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8215, "Clay")
                .displayName("Clay")
                .color(0xC8C8DC)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(1.31834F)
                .chemicalFormula("((Al₂O₃)(SiO₂)₄)₂(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Clay")
                        .source("minecraft", "Minecraft")
                        .smelting("Ceramic", 648648000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Ceramic", 1297296000L),
                                new MaterialFacts.Part("Water", 648648000L))
                        .byproducts("Kaolinite", "Palygorskite")
                        .forms("DUSTS", "ORES", "PLANTS", "PLATES")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9115, "Cobaltite")
                .displayName("Cobaltite")
                .color(0x5050FA)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1014)
                .boilingPointKelvin(1669)
                .density(5.5676665F)
                .chemicalFormula("CoAsS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cobaltite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Cobalt", 162162000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Cobalt", 648648000L),
                                new MaterialFacts.Part("Arsenic", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .byproducts("Cobalt", "Realgar", "OrangeFluorite", "Pentlandite", "YellowLimonite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9247, "Coltan")
                .displayName("Coltan")
                .color(0x695342)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(889)
                .boilingPointKelvin(1690)
                .density(4.0842505F)
                .chemicalFormula("((Ta₂O₅)(MnO₂))((Nb₂O₅)(MnO₂))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Coltan")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Tantalite", 648648000L),
                                new MaterialFacts.Part("Columbite", 648648000L))
                        .byproducts("Columbite", "Tantalite", "Pyrolusite", "Ilmenite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9246, "Columbite")
                .displayName("Columbite")
                .color(0x414D0E)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(822)
                .boilingPointKelvin(1601)
                .density(3.0737503F)
                .chemicalFormula("(Nb₂O₅)(MnO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Columbite")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .composition(
                                8L,
                                new MaterialFacts.Part("NiobiumPentoxide", 4540536000L),
                                new MaterialFacts.Part("Pyrolusite", 648648000L))
                        .byproducts("Tantalite", "Coltan", "Pyrolusite", "Ilmenite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9116, "Cooperite")
                .displayName("Cooperite")
                .color(0x82A0E6)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1677)
                .boilingPointKelvin(3238)
                .density(14.563167F)
                .chemicalFormula("Pt₃NiPdS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sheldonite")
                        .smelting("Platinum", 216216000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Platinum", 1945944000L),
                                new MaterialFacts.Part("Nickel", 648648000L),
                                new MaterialFacts.Part("Palladium", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .byproducts("Palladium", "Nickel", "Iridium")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9852, "CrudeOil")
                .displayName("Crude Oil")
                .color(0x0A0A0A)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Crude Oil")
                        .fuel(12000L, "", 0L)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8009, "Cryolite")
                .displayName("Cryolite")
                .color(0xC8BEBE)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1285)
                .boilingPointKelvin(2570)
                .density(0.5621176F)
                .chemicalFormula("Na₃AlF₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.ACID,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Cryolite")
                        .composition(
                                10L,
                                new MaterialFacts.Part("Sodium", 1945944000L),
                                new MaterialFacts.Part("Aluminium", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("Na3AlF6")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8006, "Datolite")
                .displayName("Datolite")
                .color(0xDEFFDE)
                .texture(MaterialTextureSet.ROUGH)
                .meltingPointKelvin(603)
                .boilingPointKelvin(1107)
                .density(0.6907594F)
                .chemicalFormula("H₂Ca₂B₂Si₂O₁₀")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Datolite")
                        .composition(
                                18L,
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Calcium", 1297296000L),
                                new MaterialFacts.Part("Boron", 1297296000L),
                                new MaterialFacts.Part("Silicon", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 6486480000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
    }

    private static void part2(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9001, "Diatomite")
                .displayName("Diatomite")
                .color(0xE1E1E1)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1944)
                .boilingPointKelvin(3142)
                .density(1.0270393F)
                .chemicalFormula("((SiO₂))₈(Fe₂O₃)((Al₂O₃))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Diatomite")
                        .composition(
                                10L,
                                new MaterialFacts.Part("Flint", 5189184000L),
                                new MaterialFacts.Part("Hematite", 648648000L),
                                new MaterialFacts.Part("Sapphire", 648648000L))
                        .byproducts("Mica", "Opal", "Biotite", "Perlite", "Sapphire")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9198, "DiduraniumTrioxide")
                .displayName("Diduranium Trioxide")
                .color(0x2D9191)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(512)
                .boilingPointKelvin(1050)
                .density(8.000857F)
                .chemicalFormula("Dn₂O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Diduranium Trioxide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                5L,
                                new MaterialFacts.Part("DuraniumElemental", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("TritaniumDioxide", "Trinium")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9203, "DuraniumHexaastatide")
                .displayName("Duranium Hexaastatide")
                .color(0x1991AF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(664)
                .boilingPointKelvin(878)
                .density(8.857142F)
                .chemicalFormula("DnAt₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Duranium Hexaastatide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("DuraniumElemental", 648648000L),
                                new MaterialFacts.Part("Astatine", 3891888000L))
                        .byproducts("DiduraniumTrioxide", "Astatine")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9201, "DuraniumHexabromide")
                .displayName("Duranium Hexabromide")
                .color(0x2D7DAF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(398)
                .boilingPointKelvin(640)
                .density(5.533143F)
                .chemicalFormula("DnBr₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Duranium Hexabromide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("DuraniumElemental", 648648000L),
                                new MaterialFacts.Part("Bromine", 3891888000L))
                        .byproducts("DiduraniumTrioxide", "Bromargyrite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9200, "DuraniumHexachloride")
                .displayName("Duranium Hexachloride")
                .color(0x4BAF91)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(318)
                .boilingPointKelvin(560)
                .density(2.8598976F)
                .chemicalFormula("DnCl₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Duranium Hexachloride")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("DuraniumElemental", 648648000L),
                                new MaterialFacts.Part("Chlorine", 3891888000L))
                        .byproducts("DiduraniumTrioxide", "Salt", "Sylvite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9199, "DuraniumHexafluoride")
                .displayName("Duranium Hexafluoride")
                .color(0x19AF7D)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(216)
                .boilingPointKelvin(428)
                .density(2.8585966F)
                .chemicalFormula("DnF₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Duranium Hexafluoride")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("DuraniumElemental", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .byproducts("DiduraniumTrioxide", "PinkFluorite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9202, "DuraniumHexaiodide")
                .displayName("Duranium Hexaiodide")
                .color(0x4B7DAF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(502)
                .boilingPointKelvin(747)
                .density(7.082857F)
                .chemicalFormula("DnI₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Duranium Hexaiodide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("DuraniumElemental", 648648000L),
                                new MaterialFacts.Part("Iodine", 3891888000L))
                        .byproducts("DiduraniumTrioxide", "IodineSalt")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9194, "Ferberite")
                .displayName("Ferberite")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1474)
                .boilingPointKelvin(1850)
                .density(4.5216193F)
                .chemicalFormula("Fe(WO₃)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Ferberite")
                        .tools(0, 1.0F, 1L, 3)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Tungstate", "Hematite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9117, "Galena")
                .displayName("Galena")
                .color(0x643C64)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(784)
                .boilingPointKelvin(1850)
                .density(8.707875F)
                .chemicalFormula("Pb₃Ag₃S₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Galena")
                        .source("factorization", "Factorization")
                        .smelting("Lead", 216216000L)
                        .composition(
                                8L,
                                new MaterialFacts.Part("Lead", 1945944000L),
                                new MaterialFacts.Part("Silver", 1945944000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L))
                        .byproducts("Sphalerite", "Silver", "Lead", "Selenium", "RedFluorite", "Calcite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9005, "GarnetSand")
                .displayName("Garnet Sand")
                .color(0xC86400)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1551)
                .boilingPointKelvin(2412)
                .density(1.3223641F)
                .chemicalFormula(
                        "((Al₂O₃)Fe₃(SiO₂)₃O₃)(Ca₃Fe₂(SiO₂)₃O₆)((Al₂O₃)Ca₃(SiO₂)₃O₃)((Al₂O₃)Mg₃(SiO₂)₃O₃)((Al₂O₃)Mn₃(SiO₂)₃O₃)(Ca₃Cr₂(SiO₂)₃O₆)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Garnet Sand")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Almandine", 648648000L),
                                new MaterialFacts.Part("Andradite", 648648000L),
                                new MaterialFacts.Part("Grossular", 648648000L),
                                new MaterialFacts.Part("Pyrope", 648648000L),
                                new MaterialFacts.Part("Spessartine", 648648000L),
                                new MaterialFacts.Part("Uvarovite", 648648000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9118, "Garnierite")
                .displayName("Garnierite")
                .color(0x32C846)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(891)
                .boilingPointKelvin(1638)
                .density(8.913429F)
                .chemicalFormula("NiO")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Garnierite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Nickel", 486486000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Nickel", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Nickel", "Sperrylite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9150, "Glauconite")
                .displayName("Glauconite")
                .color(0x82B43C)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1166)
                .boilingPointKelvin(1652)
                .density(0.7890686F)
                .chemicalFormula("(Al₂O₃)₂KMg₂(H₂O)O₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("GlauconiteSand")
                        .composition(
                                23L,
                                new MaterialFacts.Part("Alumina", 6486480000L),
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Magnesium", 1297296000L),
                                new MaterialFacts.Part("Water", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 4540536000L))
                        .byproducts("Sodium", "Alumina", "Hematite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9004, "GraniticMineralSand")
                .displayName("Granitic Mineral Sand")
                .color(0x283C3C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1811)
                .boilingPointKelvin(3622)
                .density(3.375388F)
                .chemicalFormula("Fe₃O₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Granitic Mineral Sand")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Iron", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Hematite", "Gold", "GraniteBlack")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9174, "Graphite")
                .displayName("Graphite")
                .color(0x808080)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1700)
                .boilingPointKelvin(4300)
                .density(2.267F)
                .chemicalFormula("C")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Graphite")
                        .tools(1, 5.0F, 32L, 2)
                        .smelting("Carbon", 648648000L)
                        .composition(1L, new MaterialFacts.Part("Carbon", 648648000L))
                        .byproducts("Carbon")
                        .forms("DUSTS", "ORES", "PLANTS", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9161, "Gypsum")
                .displayName("Gypsum")
                .color(0xF0F0F0)
                .texture(MaterialTextureSet.POWDER)
                .meltingPointKelvin(1730)
                .boilingPointKelvin(3000)
                .density(1.6021193F)
                .chemicalFormula("(CaSO₄)(H₂O)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Gypsum")
                        .composition(
                                6L,
                                new MaterialFacts.Part("CalciumSulfate", 3891888000L),
                                new MaterialFacts.Part("Water", 3891888000L))
                        .byproducts("Trona", "Mirabilite", "Asbestos", "Talc", "Sulfur")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
    }

    private static void part3(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9104, "Hematite")
                .displayName("Hematite")
                .color(0x915A5A)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1207)
                .boilingPointKelvin(2414)
                .density(3.1504574F)
                .chemicalFormula("Fe₂O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("BandedIron")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Iron", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Ilmenite", "GraniticMineralSand", "Pyrolusite", "ClayRed")
                        .forms("DUSTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("Fe2O3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9195, "Huebnerite")
                .displayName("Huebnerite")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1426)
                .boilingPointKelvin(1717)
                .density(4.449286F)
                .chemicalFormula("Mn(WO₃)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Gyubnera")
                        .tools(0, 1.0F, 1L, 3)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Manganese", 648648000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts(
                                "Wolframite", "Molybdenite", "GreenFluorite", "Arsenopyrite", "Cassiterite", "Topaz")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9120, "Ilmenite")
                .displayName("Ilmenite")
                .color(0x463732)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(782)
                .boilingPointKelvin(1392)
                .density(2.4836574F)
                .chemicalFormula("FeTiO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Illmenite")
                        .tools(0, 1.0F, 1L, 2)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Titanium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Rutile", "Hematite", "MagnesiumCarbonate", "Pyrolusite")
                        .forms("DUSTS", "MOLTEN", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8242, "IodineSalt")
                .displayName("Iodine Salt")
                .color(0xF0C8F0)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(300)
                .boilingPointKelvin(370)
                .density(1.1592574F)
                .chemicalFormula("KIO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Iodine Salt")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Iodine", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Salt", "Sylvite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("KIO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9213, "Kesterite")
                .displayName("Kesterite")
                .color(0x699B69)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(682)
                .boilingPointKelvin(1574)
                .density(5.076125F)
                .chemicalFormula("Cu₂ZnSnS₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Kesterite")
                        .smelting("Copper", 72072000L)
                        .composition(
                                8L,
                                new MaterialFacts.Part("Copper", 1297296000L),
                                new MaterialFacts.Part("Zinc", 648648000L),
                                new MaterialFacts.Part("Tin", 648648000L),
                                new MaterialFacts.Part("Sulfur", 2594592000L))
                        .byproducts("Germanium", "Pyrite", "Stannite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9166, "Kyanite")
                .displayName("Kyanite")
                .color(0x6E6EFA)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(2210)
                .boilingPointKelvin(3238)
                .density(0.96659315F)
                .chemicalFormula("(Al₂O₃)(SiO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Kyanite")
                        .composition(
                                8L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L))
                        .byproducts("Quartzite", "Lepidolite", "Spodumene")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9149, "Lepidolite")
                .displayName("Lepidolite")
                .color(0xF0328C)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1162)
                .boilingPointKelvin(1769)
                .density(0.6034791F)
                .chemicalFormula("(Al₂O₃)₂KLi₃F₂O₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Lepidolite")
                        .composition(
                                22L,
                                new MaterialFacts.Part("Alumina", 6486480000L),
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Lithium", 1945944000L),
                                new MaterialFacts.Part("Fluorine", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 3891888000L))
                        .byproducts("LithiumChloride", "Caesium", "Rubidium")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8029, "LithiumChloride")
                .displayName("Lithium Chloride")
                .color(0xDEDEFA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(880)
                .boilingPointKelvin(1655)
                .density(0.268607F)
                .chemicalFormula("LiCl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Lithium Chloride")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Lithium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 648648000L))
                        .byproducts("Lithium")
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("LiCl")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8016, "MagnesiumCarbonate")
                .displayName("Magnesium Carbonate")
                .color(0xF0E6E6)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(623)
                .boilingPointKelvin(3000)
                .density(0.8018574F)
                .chemicalFormula("Mg(CO₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Magnesite")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("CarbonTrioxide", 2594592000L))
                        .byproducts("Cobaltite", "Pyrolusite")
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("MgCO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8018, "MagnesiumChloride")
                .displayName("Magnesium Chloride")
                .color(0xEBEBFA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(987)
                .boilingPointKelvin(1685)
                .density(0.581476F)
                .chemicalFormula("MgCl₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Magnesium Chloride")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 1297296000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("MgCl2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9122, "Magnetite")
                .displayName("Magnetite")
                .color(0x1E1E1E)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1811)
                .boilingPointKelvin(3622)
                .density(3.375388F)
                .chemicalFormula("Fe₃O₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Magnetite")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Iron", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Hematite", "Gold", "Stone")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9156, "Malachite")
                .displayName("Malachite")
                .color(0x055F05)
                .texture(MaterialTextureSet.LAPIS)
                .meltingPointKelvin(325)
                .boilingPointKelvin(669)
                .density(2.0194325F)
                .chemicalFormula("Cu₂(CO₃)H₂O₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Malachite")
                        .smelting("Copper", 108108000L)
                        .composition(
                                10L,
                                new MaterialFacts.Part("Copper", 1297296000L),
                                new MaterialFacts.Part("CarbonTrioxide", 2594592000L),
                                new MaterialFacts.Part("Hydrogen", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Copper", "BrownLimonite", "Calcite", "Azurite", "Arsenic")
                        .multipliers(5, 1)
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9158, "Mica")
                .displayName("Mica")
                .color(0xC3C3CD)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1841)
                .boilingPointKelvin(2797)
                .density(0.81862545F)
                .chemicalFormula("(Al₂O₃)₃K₂(SiO₂)₆F₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Mica")
                        .composition(
                                39L,
                                new MaterialFacts.Part("Alumina", 9729720000L),
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Fluorine", 2594592000L))
                        .byproducts("Vermiculite", "Asbestos", "Biotite", "Perlite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9157, "Mirabilite")
                .displayName("Mirabilite")
                .color(0xF0FAD2)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(440)
                .boilingPointKelvin(624)
                .density(4.8592453F)
                .chemicalFormula("(Na₂SO₄)(H₂O)₁₀")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Mirabilite")
                        .composition(
                                7L,
                                new MaterialFacts.Part("SodiumSulfate", 4540536000L),
                                new MaterialFacts.Part("Water", 19459440000L))
                        .byproducts("Trona", "Gypsum")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9123, "Molybdenite")
                .displayName("Molybdenite")
                .color(0x191919)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1224)
                .boilingPointKelvin(2115)
                .density(4.7846665F)
                .chemicalFormula("MoS₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Molybdenite")
                        .smelting("Molybdenum", 162162000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Molybdenum", 648648000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L))
                        .byproducts("Powellite", "Scheelite", "Rhenium", "Wulfenite", "OsmiumElemental")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part4(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8461, "NiobiumPentoxide")
                .displayName("Niobium Pentoxide")
                .color(0x32400A)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(824)
                .boilingPointKelvin(1497)
                .density(2.449592F)
                .chemicalFormula("Nb₂O₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Niobium Pentoxide")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Niobium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 3243240000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("Nb2O5")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8206, "Niter")
                .displayName("Niter")
                .color(0xFFC8C8)
                .texture(MaterialTextureSet.FLINT)
                .meltingPointKelvin(607)
                .boilingPointKelvin(1214)
                .density(0.18440752F)
                .chemicalFormula("(KNO₃)(NaNO₃)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Nitre")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .composition(
                                2L,
                                new MaterialFacts.Part("PotassiumNitrate", 648648000L),
                                new MaterialFacts.Part("SodiumNitrate", 648648000L))
                        .byproducts("PotassiumNitrate", "SodiumNitrate")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9851, "OilSand")
                .displayName("Oil Sand")
                .color(0x0A0A0A)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(100)
                .boilingPointKelvin(400)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Oilsands")
                        .fuel(6000L, "", 0L)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("Oilsands")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9853, "OilShale")
                .displayName("Oil Shale")
                .color(0x32323C)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(500)
                .boilingPointKelvin(1000)
                .density(0.9050852F)
                .chemicalFormula("(Ca(CO₃))₂((SiO₂))(((Al₂O₃)(SiO₂)₄)₂(H₂O))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Oilshale")
                        .fuel(400L, "Stone", 324324000L)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Calcite", 1297296000L),
                                new MaterialFacts.Part("MilkyQuartz", 648648000L),
                                new MaterialFacts.Part("Clay", 648648000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("Oilshale")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9145, "Pentlandite")
                .displayName("Pentlandite")
                .color(0xA59605)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1097)
                .boilingPointKelvin(2024)
                .density(5.6908236F)
                .chemicalFormula("Ni₉S₈")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pentlandite")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Nickel", 216216000L)
                        .composition(
                                17L,
                                new MaterialFacts.Part("Nickel", 5837832000L),
                                new MaterialFacts.Part("Sulfur", 5189184000L))
                        .byproducts("Hematite", "Sulfur", "Cobaltite", "Sperrylite", "Gypsum")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9138, "Perlite")
                .displayName("Perlite")
                .color(0x1E141E)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(786)
                .boilingPointKelvin(2186)
                .density(1.2231661F)
                .chemicalFormula("(MgFe(SiO₂)₂O₄)(H₂O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Perlite")
                        .composition(
                                1L,
                                new MaterialFacts.Part("Obsidian", 648648000L),
                                new MaterialFacts.Part("Water", 648648000L))
                        .byproducts("Peridot", "MagnesiumCarbonate")
                        .multipliers(8, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8207, "Phosphate")
                .displayName("Phosphate")
                .color(0xFFFF00)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(400)
                .boilingPointKelvin(800)
                .density(0.3651432F)
                .chemicalFormula("PO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Phosphate")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Phosphor", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts(
                                "Phosphorus",
                                "Apatite",
                                "YellowFluorite",
                                "Phosphorite",
                                "RedPhosphorus",
                                "WhitePhosphorus")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("PO4")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9197, "Pinalite")
                .displayName("Pinalite")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(839)
                .boilingPointKelvin(1327)
                .density(4.8445067F)
                .chemicalFormula("Pb₃(WO₃)Cl₂O₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pinalite")
                        .tools(0, 1.0F, 1L, 3)
                        .smelting("TungstenTrioxide", 235872000L)
                        .composition(
                                11L,
                                new MaterialFacts.Part("Lead", 1945944000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Chlorine", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Tungstate", "Lead")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9155, "Pitchblende")
                .displayName("Pitchblende")
                .color(0x646E00)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(845)
                .boilingPointKelvin(2333)
                .density(15.984115F)
                .chemicalFormula("(UO₂)₃ThPb")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Pitchblende")
                        .smelting("Uranium", 129729600L)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Uraninite", 1945944000L),
                                new MaterialFacts.Part("Thorium", 648648000L),
                                new MaterialFacts.Part("Lead", 648648000L))
                        .byproducts("Lead", "Radium", "RareEarth", "Thorium")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8103, "PlatinumGroupSludge")
                .displayName("Platinum Group Sludge")
                .color(0x323250)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(21.46F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Platinum Group Sludge")
                        .source("gregtech", "GregTech 5 Unofficial")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9147, "Pollucite")
                .displayName("Pollucite")
                .color(0xF0D2D2)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1455)
                .boilingPointKelvin(2273)
                .density(0.94144416F)
                .chemicalFormula("(Al₂O₃)Cs₂(SiO₂)₄(H₂O)₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Pollucite")
                        .composition(
                                26L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Caesium", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 7783776000L),
                                new MaterialFacts.Part("Water", 3891888000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Zeolite", "Caesium", "Rubidium")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9140, "PotassiumFeldspar")
                .displayName("Potassium Feldspar")
                .color(0x782828)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1853)
                .boilingPointKelvin(2937)
                .density(0.81232554F)
                .chemicalFormula("K₂(Al₂O₃)(SiO₂)₆O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Potassium Feldspar")
                        .composition(
                                26L,
                                new MaterialFacts.Part("Potassium", 1297296000L),
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("SiliconDioxide", 11675664000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8205, "PotassiumNitrate")
                .displayName("Potassium Nitrate")
                .color(0xE6E6E6)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(607)
                .boilingPointKelvin(1214)
                .density(0.17350753F)
                .chemicalFormula("KNO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Saltpeter")
                        .source("Railcraft", "Railcraft")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("SodiumNitrate", "Niter")
                        .multipliers(3, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("KNO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9124, "Powellite")
                .displayName("Powellite")
                .color(0xFFFF00)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(704)
                .boilingPointKelvin(1171)
                .density(1.9609526F)
                .chemicalFormula("CaMoO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Powellite")
                        .smelting("Molybdenum", 72072000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("Molybdenum", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Molybdenite", "Scheelite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9125, "Pyrite")
                .displayName("Pyrite")
                .color(0xFFE650)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(862)
                .boilingPointKelvin(1522)
                .density(4.0026665F)
                .chemicalFormula("FeS₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pyrite")
                        .tools(0, 1.0F, 1L, 0)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Sulfur", 1297296000L))
                        .byproducts("Sulfur", "Phosphorus", "Hematite", "Stannite", "Selenium")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
    }

    private static void part5(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9126, "Pyrolusite")
                .displayName("Pyrolusite")
                .color(0x323246)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(808)
                .boilingPointKelvin(2334)
                .density(7.442858F)
                .chemicalFormula("MnO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Pyrolusite")
                        .smelting("Manganese", 486486000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Manganese", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Bromargyrite", "Hematite", "Chromite")
                        .forms("DUSTS", "INGOTS", "ORES", "PLANTS")
                        .aliases("MnO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9006, "QuartzSand")
                .displayName("Quartz Sand")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("((SiO₂))((SiO₂))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Quartz Sand")
                        .smelting("SiliconDioxide", 216216000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("CertusQuartz", 648648000L),
                                new MaterialFacts.Part("MilkyQuartz", 648648000L))
                        .byproducts("CertusQuartz", "Quartzite", "Barite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9271, "Quicklime")
                .displayName("Quicklime")
                .color(0xF0E6D2)
                .texture(MaterialTextureSet.POWDER)
                .meltingPointKelvin(2886)
                .boilingPointKelvin(3120)
                .density(0.7707145F)
                .chemicalFormula("CaO")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Quicklime")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9100, "RareEarth")
                .displayName("Rare Earth")
                .color(0x808064)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(7.007F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Rare Earth")
                        .source("ThermalExpansion", "Thermal Expansion")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9109, "Realgar")
                .displayName("Realgar")
                .color(0x9D2123)
                .texture(MaterialTextureSet.EMERALD)
                .meltingPointKelvin(637)
                .boilingPointKelvin(903)
                .density(3.9215F)
                .chemicalFormula("AsS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Realgar")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Arsenic", 216216000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Arsenic", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .byproducts("Cobaltite", "Arsenopyrite")
                        .forms("ARMORS", "DUSTS", "GEMS", "LENSES", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9196, "Russellite")
                .displayName("Russellite")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(914)
                .boilingPointKelvin(1313)
                .density(4.319175F)
                .chemicalFormula("Bi₂(WO₃)O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Russellite")
                        .tools(0, 1.0F, 1L, 3)
                        .smelting("TungstenTrioxide", 288288000L)
                        .composition(
                                9L,
                                new MaterialFacts.Part("Bismuth", 1297296000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Tungstate", "Bismuth")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9192, "Rutile")
                .displayName("Rutile")
                .color(0x6E5078)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(2041)
                .boilingPointKelvin(3560)
                .density(4.542858F)
                .chemicalFormula("TiO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Rutile")
                        .source("Mariculture", "Mariculture")
                        .tools(0, 1.0F, 1L, 2)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Titanium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Hematite", "Zircon")
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("TiO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8204, "Salt")
                .displayName("Salt")
                .color(0xFAFAFA)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1074)
                .boilingPointKelvin(1686)
                .density(0.487107F)
                .chemicalFormula("NaCl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Salt")
                        .source("harvestcraft", "HarvestCraft")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 648648000L))
                        .byproducts("Sylvite", "IodineSalt", "Borax")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("NaCl")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9128, "Scheelite")
                .displayName("Scheelite")
                .color(0xC88C14)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1358)
                .boilingPointKelvin(1621)
                .density(3.4659526F)
                .chemicalFormula("Ca(WO₃)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("CalciumTungstate")
                        .tools(0, 1.0F, 1L, 3)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Pyrolusite", "Molybdenite", "Calcite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8000, "SiliconDioxide")
                .displayName("Silicon Dioxide")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.QUARTZ)
                .meltingPointKelvin(1986)
                .boilingPointKelvin(3220)
                .density(0.777486F)
                .chemicalFormula("SiO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Silicon Dioxide")
                        .source("IC2", "IndustrialCraft 2")
                        .composition(
                                3L,
                                new MaterialFacts.Part("Silicon", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("SiO2")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9211, "Smithsonite")
                .displayName("Smithsonite")
                .color(0x6EDFD2)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(930)
                .boilingPointKelvin(1150)
                .density(1.8810574F)
                .chemicalFormula("ZnCO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Smithsonite")
                        .smelting("Zinc", 108108000L)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Zinc", 648648000L),
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Zinc", "Bromargyrite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8039, "SodiumHydrogencarbonate")
                .displayName("Sodium Hydrogencarbonate")
                .color(0xE6EBF0)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(724)
                .boilingPointKelvin(957)
                .density(0.54039615F)
                .chemicalFormula("NaHCO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Soda")
                        .source("SaltMod", "Salty Mod")
                        .composition(
                                6L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Carbon", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("Mirabilite", "Gypsum", "Trona")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("NaHCO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8019, "SodiumNitrate")
                .displayName("Sodium Nitrate")
                .color(0xE6E6E6)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(607)
                .boilingPointKelvin(1214)
                .density(0.19530752F)
                .chemicalFormula("NaNO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sodium Nitrate")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Sodium", 648648000L),
                                new MaterialFacts.Part("Nitrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .byproducts("PotassiumNitrate", "Niter")
                        .multipliers(3, 1)
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("NaNO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8101, "Soulsand")
                .displayName("Soulsand")
                .color(0x646450)
                .texture(MaterialTextureSet.SAND)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Soulsand")
                        .source("minecraft", "Minecraft")
                        .byproducts("Coal", "NetherQuartz", "Niter")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("SoulSand")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9212, "Sperrylite")
                .displayName("Sperrylite")
                .color(0x696969)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(1271)
                .boilingPointKelvin(2092)
                .density(11.004F)
                .chemicalFormula("PtAs₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Sperrylite")
                        .smelting("Platinum", 162162000L)
                        .composition(
                                3L,
                                new MaterialFacts.Part("Platinum", 648648000L),
                                new MaterialFacts.Part("Arsenic", 1297296000L))
                        .byproducts("Antimony", "Copper", "Hematite", "Rhodium", "Cooperite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
    }

    private static void part6(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9130, "Sphalerite")
                .displayName("Sphalerite")
                .color(0xDEDE00)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(540)
                .boilingPointKelvin(948)
                .density(4.6005F)
                .chemicalFormula("ZnS")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Sphalerite")
                        .source("HardcoreEnderExpansion", "Hardcore Ender Expansion")
                        .smelting("Zinc", 216216000L)
                        .composition(
                                2L,
                                new MaterialFacts.Part("Zinc", 648648000L),
                                new MaterialFacts.Part("Sulfur", 648648000L))
                        .byproducts("Cadmium", "Gallium", "Zinc", "Kesterite", "Selenium", "Indium")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9146, "Spodumene")
                .displayName("Spodumene")
                .color(0xBEAAAA)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1825)
                .boilingPointKelvin(2905)
                .density(0.7899774F)
                .chemicalFormula("(Al₂O₃)Li₂(SiO₂)₄O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Spodumene")
                        .composition(
                                20L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Lithium", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 7783776000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Alumina", "LithiumChloride")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9214, "Stannite")
                .displayName("Stannite")
                .color(0x9B9137)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(822)
                .boilingPointKelvin(1818)
                .density(5.168625F)
                .chemicalFormula("Cu₂FeSnS₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Stannite")
                        .smelting("Copper", 72072000L)
                        .composition(
                                8L,
                                new MaterialFacts.Part("Copper", 1297296000L),
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Tin", 648648000L),
                                new MaterialFacts.Part("Sulfur", 2594592000L))
                        .byproducts("Germanium", "Pyrite", "Kesterite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9131, "Stibnite")
                .displayName("Stibnite")
                .color(0x464646)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(823)
                .boilingPointKelvin(1646)
                .density(3.9142F)
                .chemicalFormula("Sb₂S₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Stibnite")
                        .smelting("Antimony", 162162000L)
                        .composition(
                                5L,
                                new MaterialFacts.Part("Antimony", 1297296000L),
                                new MaterialFacts.Part("Sulfur", 1945944000L))
                        .byproducts(
                                "Antimony",
                                "Realgar",
                                "Cinnabar",
                                "Galena",
                                "Arsenopyrite",
                                "Pyrite",
                                "Barite",
                                "Calcite")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9193, "Stolzite")
                .displayName("Stolzite")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1273)
                .boilingPointKelvin(1665)
                .density(5.0996194F)
                .chemicalFormula("Pb(WO₃)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Raspite")
                        .tools(0, 1.0F, 1L, 3)
                        .smelting("TungstenTrioxide", 432432000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Lead", 648648000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Tungstate", "Lead")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8203, "Sylvite")
                .displayName("Sylvite")
                .color(0xF0C8C8)
                .texture(MaterialTextureSet.CUBE)
                .meltingPointKelvin(1040)
                .boilingPointKelvin(1690)
                .density(0.432607F)
                .chemicalFormula("KCl")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("RockSalt")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Potassium", 648648000L),
                                new MaterialFacts.Part("Chlorine", 648648000L))
                        .byproducts("IodineSalt", "Salt")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("KCl")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9169, "Talc")
                .displayName("Talc")
                .color(0x5F915F)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1313)
                .boilingPointKelvin(2100)
                .density(0.8356247F)
                .chemicalFormula("Mg₃(SiO₂)₄(H₂O)O₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Soapstone")
                        .composition(
                                21L,
                                new MaterialFacts.Part("Magnesium", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 7783776000L),
                                new MaterialFacts.Part("Water", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9148, "Tantalite")
                .displayName("Tantalite")
                .color(0x915028)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(956)
                .boilingPointKelvin(1780)
                .density(5.0947504F)
                .chemicalFormula("(Ta₂O₅)(MnO₂)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tantalite")
                        .source("hbm", "HBM's Nuclear Tech Mod")
                        .composition(
                                8L,
                                new MaterialFacts.Part("TantalumPentoxide", 4540536000L),
                                new MaterialFacts.Part("Pyrolusite", 648648000L))
                        .byproducts("Columbite", "Coltan", "Pyrolusite", "Ilmenite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8462, "TantalumPentoxide")
                .displayName("Tantalum Pentoxide")
                .color(0x40320A)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(978)
                .boilingPointKelvin(1701)
                .density(4.7593064F)
                .chemicalFormula("Ta₂O₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tantalum Pentoxide")
                        .composition(
                                7L,
                                new MaterialFacts.Part("Tantalum", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 3243240000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("Ta2O5")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9132, "Tetrahedrite")
                .displayName("Tetrahedrite")
                .color(0xC82000)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(993)
                .boilingPointKelvin(1956)
                .density(5.955F)
                .chemicalFormula("Cu₃SbFeS₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tetrahedrite")
                        .smelting("Copper", 162162000L)
                        .composition(
                                8L,
                                new MaterialFacts.Part("Copper", 1945944000L),
                                new MaterialFacts.Part("Antimony", 648648000L),
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Sulfur", 1945944000L))
                        .byproducts("Copper", "Antimony", "Zinc", "Kesterite", "Arsenic")
                        .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9204, "TritaniumDioxide")
                .displayName("Tritanium Dioxide")
                .color(0x19B97D)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(702)
                .boilingPointKelvin(1106)
                .density(8.334286F)
                .chemicalFormula("TnO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium Dioxide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                3L,
                                new MaterialFacts.Part("TritaniumElemental", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("DiduraniumTrioxide", "Trinium")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9209, "TritaniumHexaastatide")
                .displayName("Tritanium Hexaastatide")
                .color(0x197DB9)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(778)
                .boilingPointKelvin(971)
                .density(9.571428F)
                .chemicalFormula("TnAt₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium Hexaastatide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("TritaniumElemental", 648648000L),
                                new MaterialFacts.Part("Astatine", 3891888000L))
                        .byproducts("TritaniumDioxide", "Astatine")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9207, "TritaniumHexabromide")
                .displayName("Tritanium Hexabromide")
                .color(0x377D9B)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(512)
                .boilingPointKelvin(732)
                .density(6.2474284F)
                .chemicalFormula("TnBr₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium Hexabromide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("TritaniumElemental", 648648000L),
                                new MaterialFacts.Part("Bromine", 3891888000L))
                        .byproducts("TritaniumDioxide", "Bromargyrite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9206, "TritaniumHexachloride")
                .displayName("Tritanium Hexachloride")
                .color(0x37B99B)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(432)
                .boilingPointKelvin(653)
                .density(3.5741835F)
                .chemicalFormula("TnCl₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium Hexachloride")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("TritaniumElemental", 648648000L),
                                new MaterialFacts.Part("Chlorine", 3891888000L))
                        .byproducts("TritaniumDioxide", "Salt", "Sylvite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9205, "TritaniumHexafluoride")
                .displayName("Tritanium Hexafluoride")
                .color(0x557D7D)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(331)
                .boilingPointKelvin(521)
                .density(3.5728822F)
                .chemicalFormula("TnF₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium Hexafluoride")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("TritaniumElemental", 648648000L),
                                new MaterialFacts.Part("Fluorine", 3891888000L))
                        .byproducts("TritaniumDioxide", "MagentaFluorite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
    }

    private static void part7(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(9208, "TritaniumHexaiodide")
                .displayName("Tritanium Hexaiodide")
                .color(0x55B9B9)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(616)
                .boilingPointKelvin(840)
                .density(7.797143F)
                .chemicalFormula("TnI₆")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Tritanium Hexaiodide")
                        .tools(0, 1.0F, 1L, 4)
                        .composition(
                                7L,
                                new MaterialFacts.Part("TritaniumElemental", 648648000L),
                                new MaterialFacts.Part("Iodine", 3891888000L))
                        .byproducts("TritaniumDioxide", "IodineSalt")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9159, "Trona")
                .displayName("Trona")
                .color(0x87875F)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(698)
                .boilingPointKelvin(1310)
                .density(1.7022145F)
                .chemicalFormula("(Na₂(CO₃))(H₂O)₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Trona")
                        .composition(
                                6L,
                                new MaterialFacts.Part("SodiumCarbonate", 3891888000L),
                                new MaterialFacts.Part("Water", 3891888000L))
                        .byproducts("Mirabilite", "Gypsum", "SodiumHydrogencarbonate")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9133, "Tungstate")
                .displayName("Tungstate")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1134)
                .boilingPointKelvin(1584)
                .density(2.903388F)
                .chemicalFormula("Li₂(WO₃)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Tungstate")
                        .tools(0, 1.0F, 1L, 3)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Lithium", 1297296000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Pyrolusite", "Silver", "LithiumChloride")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8026, "TungstenTrioxide")
                .displayName("Tungsten Trioxide")
                .color(0xC7D300)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1746)
                .boilingPointKelvin(1970)
                .density(4.813572F)
                .chemicalFormula("WO₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("TungstenOxide")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Tungsten", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1945944000L))
                        .forms("DUSTS", "INGOTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("WO3")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9134, "Uraninite")
                .displayName("Uraninite")
                .color(0x232323)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(504)
                .boilingPointKelvin(1528)
                .density(18.952858F)
                .chemicalFormula("UO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Uraninite")
                        .smelting("Uranium", 216216000L)
                        .composition(
                                1L,
                                new MaterialFacts.Part("Uranium", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Lead", "Radium", "RareEarth", "Thorium")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9143, "VanadiumMagnetite")
                .displayName("Vanadium Magnetite")
                .color(0x23233C)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1236)
                .boilingPointKelvin(2368)
                .density(2.5610614F)
                .chemicalFormula("(Fe₃O₄)(V₂O₅)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Ferrovanadium")
                        .composition(
                                2L,
                                new MaterialFacts.Part("Magnetite", 648648000L),
                                new MaterialFacts.Part("VanadiumPentoxide", 648648000L))
                        .byproducts("Magnetite", "VanadiumPentoxide")
                        .forms("DUSTS", "MOLTEN", "ORES", "PLANTS")
                        .aliases("Ferrovanadium")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(8234, "VanadiumPentoxide")
                .displayName("Vanadium Pentoxide")
                .color(0x323232)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(662)
                .boilingPointKelvin(1115)
                .density(1.746735F)
                .chemicalFormula("V₂O₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Vanadium Pentoxide")
                        .smelting("Vanadium", 92664000L)
                        .composition(
                                7L,
                                new MaterialFacts.Part("Vanadium", 1297296000L),
                                new MaterialFacts.Part("Oxygen", 3243240000L))
                        .forms("DUSTS", "ORES", "PLANTS")
                        .aliases("V2O5")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9152, "Vermiculite")
                .displayName("Vermiculite")
                .color(0xC8B40F)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1436)
                .boilingPointKelvin(2180)
                .density(1.4295535F)
                .chemicalFormula("(Al₂O₃)₂Fe₃(SiO₂)₄(H₂O)₄H₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Vermiculite")
                        .composition(
                                39L,
                                new MaterialFacts.Part("Alumina", 6486480000L),
                                new MaterialFacts.Part("Iron", 1945944000L),
                                new MaterialFacts.Part("SiliconDioxide", 7783776000L),
                                new MaterialFacts.Part("Water", 7783776000L),
                                new MaterialFacts.Part("Hydrogen", 1297296000L))
                        .byproducts("Mica", "Asbestos", "Biotite", "Diatomite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9217, "Wolframite")
                .displayName("Wolframite")
                .color(0x373223)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1326)
                .boilingPointKelvin(1555)
                .density(3.4989526F)
                .chemicalFormula("Mg(WO₃)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Wolframite")
                        .tools(0, 1.0F, 1L, 3)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Magnesium", 648648000L),
                                new MaterialFacts.Part("TungstenTrioxide", 2594592000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Tungstate", "Hematite", "Stannite", "MagnesiumCarbonate")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9164, "Wollastonite")
                .displayName("Wollastonite")
                .color(0xF0F0F0)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1425)
                .boilingPointKelvin(2301)
                .density(0.7747774F)
                .chemicalFormula("Ca(SiO₂)O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Wollastonite")
                        .composition(
                                5L,
                                new MaterialFacts.Part("Calcium", 648648000L),
                                new MaterialFacts.Part("SiliconDioxide", 1945944000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Hematite", "MagnesiumCarbonate", "Pyrolusite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9136, "Wulfenite")
                .displayName("Wulfenite")
                .color(0xFF8000)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(618)
                .boilingPointKelvin(1215)
                .density(3.5946193F)
                .chemicalFormula("PbMoO₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Wulfenite")
                        .smelting("Molybdenum", 72072000L)
                        .composition(
                                6L,
                                new MaterialFacts.Part("Lead", 648648000L),
                                new MaterialFacts.Part("Molybdenum", 648648000L),
                                new MaterialFacts.Part("Oxygen", 2594592000L))
                        .byproducts("Powellite", "Scheelite", "Molybdenite", "Galena")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9137, "YellowLimonite")
                .displayName("Yellow Limonite")
                .color(0xC8C800)
                .texture(MaterialTextureSet.METALLIC)
                .meltingPointKelvin(1523)
                .boilingPointKelvin(3046)
                .density(1.969237F)
                .chemicalFormula("FeHO₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.MELTING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("BogIron")
                        .tools(0, 1.0F, 1L, 0)
                        .smelting("Hematite", 324324000L)
                        .composition(
                                4L,
                                new MaterialFacts.Part("Iron", 648648000L),
                                new MaterialFacts.Part("Hydrogen", 648648000L),
                                new MaterialFacts.Part("Oxygen", 1297296000L))
                        .byproducts("Nickel", "BrownLimonite", "Cobaltite")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9165, "Zeolite")
                .displayName("Zeolite")
                .color(0xF0E6E6)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1461)
                .boilingPointKelvin(2289)
                .density(0.8720595F)
                .chemicalFormula("(Al₂O₃)Na₂(SiO₂)₄(H₂O)₂O")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.ELECTROLYSER,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.ORE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Zeolite")
                        .composition(
                                26L,
                                new MaterialFacts.Part("Alumina", 3243240000L),
                                new MaterialFacts.Part("Sodium", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 7783776000L),
                                new MaterialFacts.Part("Water", 3891888000L),
                                new MaterialFacts.Part("Oxygen", 648648000L))
                        .byproducts("Pollucite", "Salt")
                        .forms("DUSTS", "ORES", "PLANTS")
                        .build())
                .build());
    }
}
