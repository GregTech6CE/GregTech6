package gregtech.content.material.catalog;

import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import java.util.ArrayList;
import java.util.List;

/** Canonical declarations migrated from the GT6 MT/TD import and the port's explicit corrections.
 * Historical extraction tools are unavailable; these declarations are maintained directly.
 * Intrinsic facts only: recipe sources and world-generation remain owned by their providers. */
public final class StoneMaterials {
    private StoneMaterials() {}

    public static List<MaterialDefinition.Declaration> definitions() {
        var result = new ArrayList<MaterialDefinition.Declaration>();
        part0(result);
        part1(result);
        return List.copyOf(result);
    }

    private static void part0(List<MaterialDefinition.Declaration> result) {
        result.add(MaterialDefinition.builder(9188, "Andesite")
                .displayName("Andesite")
                .color(0xBFBFBF)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Andesite")
                .source("etfuturum", "Et Futurum")
                .tools(1, 2.5F, 16L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8505, "Basalt")
                .displayName("Basalt")
                .color(0x3C3232)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1673)
                .boilingPointKelvin(3346)
                .density(0.94297844F)
                .chemicalFormula("((SiO₂)₂FeMg₂)(Ca(CO₃))₃((SiO₂))₈DarkAsh₄")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Basalt")
                .source("netherlicious", "Netherlicious")
                .tools(1, 3.0F, 32L, 2)
                .composition(
                        16L,
                        new MaterialInfoDefinition.Part("Peridot", 648648000L),
                        new MaterialInfoDefinition.Part("Calcite", 1945944000L),
                        new MaterialInfoDefinition.Part("Flint", 5189184000L),
                        new MaterialInfoDefinition.Part("DarkAshes", 2594592000L))
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9184, "Blueschist")
                .displayName("Blueschist")
                .color(0x6969BE)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Blueschist")
                .tools(1, 2.0F, 24L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9185, "Bluestone")
                .displayName("Bluestone")
                .color(0x3434FC)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Bluestone")
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9186, "Chert")
                .displayName("Chert")
                .color(0x690A0A)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Chert")
                .tools(1, 2.0F, 16L, 0)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8501, "Concrete")
                .displayName("Concrete")
                .color(0x646464)
                .texture(MaterialTextureSet.BRICK)
                .meltingPointKelvin(500)
                .boilingPointKelvin(1000)
                .density(0.777486F)
                .chemicalFormula("Stone")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.STONE)
                .localName("Concrete")
                .tools(1, 2.5F, 32L, 0)
                .smelting("Stone", 648648000L)
                .composition(1L, new MaterialInfoDefinition.Part("Stone", 648648000L))
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9187, "Dacite")
                .displayName("Dacite")
                .color(0x838383)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Dacite")
                .tools(1, 2.0F, 16L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9248, "Deepslate")
                .displayName("Deepslate")
                .color(0x393B3D)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Deepslate")
                .source("etfuturum", "Et Futurum")
                .tools(1, 2.0F, 32L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8511, "Diorite")
                .displayName("Diorite")
                .color(0xF0F0F0)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Diorite")
                .source("etfuturum", "Et Futurum")
                .tools(1, 2.5F, 16L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8504, "Endstone")
                .displayName("Endstone")
                .color(0xD9DE9E)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1200)
                .boilingPointKelvin(2400)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Endstone")
                .source("minecraft", "Minecraft")
                .tools(1, 3.0F, 16L, 1)
                .byproducts("Helium3", "Beryllium")
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9171, "Greenschist")
                .displayName("Greenschist")
                .color(0x69BE69)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Greenschist")
                .tools(1, 2.0F, 24L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9218, "Kimberlite")
                .displayName("Kimberlite")
                .color(0x64460A)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Kimberlite")
                .tools(1, 2.0F, 24L, 2)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9177, "Komatiite")
                .displayName("Komatiite")
                .color(0xBEBE69)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1673)
                .boilingPointKelvin(3346)
                .density(0.9874688F)
                .chemicalFormula("((SiO₂)₂FeMg₂)(Mg(CO₃))₂((SiO₂))₆DarkAsh₃")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Komatiite")
                .tools(1, 3.0F, 32L, 2)
                .composition(
                        12L,
                        new MaterialInfoDefinition.Part("Peridot", 648648000L),
                        new MaterialInfoDefinition.Part("MagnesiumCarbonate", 1297296000L),
                        new MaterialInfoDefinition.Part("Flint", 3891888000L),
                        new MaterialInfoDefinition.Part("DarkAshes", 1945944000L))
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9189, "Limestone")
                .displayName("Limestone")
                .color(0xE6C882)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1612)
                .boilingPointKelvin(3000)
                .density(0.7622574F)
                .chemicalFormula("(Ca(CO₃))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.STONE)
                .localName("Limestone")
                .tools(1, 2.5F, 16L, 1)
                .smelting("Calcite", 324324000L)
                .composition(1L, new MaterialInfoDefinition.Part("Calcite", 648648000L))
                .byproducts("Calcite", "Phosphorite")
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8521, "Livingrock")
                .displayName("Livingrock")
                .color(0xC3CDC3)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1800)
                .boilingPointKelvin(3600)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Livingrock")
                .source("Botania", "Botania")
                .tools(1, 5.0F, 128L, 2)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
    }

    private static void part1(List<MaterialDefinition.Declaration> result) {
        result.add(MaterialDefinition.builder(8506, "Marble")
                .displayName("Marble")
                .color(0xC8C8C8)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1525)
                .boilingPointKelvin(2795)
                .density(0.88422525F)
                .chemicalFormula("Mg(Ca(CO₃))₇")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.STONE)
                .localName("Marble")
                .source("Redpower", "Redpower")
                .tools(1, 2.5F, 16L, 1)
                .smelting("Calcite", 432432000L)
                .composition(
                        8L,
                        new MaterialInfoDefinition.Part("Magnesium", 648648000L),
                        new MaterialInfoDefinition.Part("Calcite", 4540536000L))
                .byproducts("Calcite")
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8516, "MarsSand")
                .displayName("Mars Sand")
                .color(0xCF4242)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Mars Sand")
                .source("GalacticraftCore", "Galacticraft")
                .tools(1, 3.0F, 16L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8503, "NetherBrick")
                .displayName("Nether Brick")
                .color(0x640000)
                .texture(MaterialTextureSet.BRICK)
                .meltingPointKelvin(1800)
                .boilingPointKelvin(3000)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("BrickNether")
                .source("minecraft", "Minecraft")
                .tools(1, 2.0F, 24L, 1)
                .pulverizing("Netherrack", 648648000L)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8502, "Netherrack")
                .displayName("Netherrack")
                .color(0xC80000)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1500)
                .boilingPointKelvin(3000)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.STONE)
                .localName("Netherrack")
                .source("minecraft", "Minecraft")
                .tools(1, 2.0F, 8L, 0)
                .smelting("NetherBrick", 648648000L)
                .byproducts("Sulfur")
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9219, "Prismarine")
                .displayName("Prismarine")
                .color(0x6EB2A5)
                .texture(MaterialTextureSet.PRISMARINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Prismarine")
                .source("etfuturum", "Et Futurum")
                .tools(1, 4.0F, 48L, 1)
                .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("PrismarineLight")
                .build());
        result.add(MaterialDefinition.builder(9220, "PrismarineDark")
                .displayName("PrismarineDark")
                .color(0x587D6C)
                .texture(MaterialTextureSet.PRISMARINE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("PrismarineDark")
                .source("etfuturum", "Et Futurum")
                .tools(1, 4.0F, 48L, 1)
                .forms("ARMORS", "DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9000, "Pumice")
                .displayName("Pumice")
                .color(0xDCD87F)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1673)
                .boilingPointKelvin(3346)
                .density(1.3142402F)
                .chemicalFormula("((SiO₂)₂FeMg₂)₃(Mg(CO₃))₂((SiO₂))₄DarkAsh₂")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Pumice")
                .tools(1, 3.0F, 32L, 2)
                .composition(
                        11L,
                        new MaterialInfoDefinition.Part("Peridot", 1945944000L),
                        new MaterialInfoDefinition.Part("MagnesiumCarbonate", 1297296000L),
                        new MaterialInfoDefinition.Part("Flint", 2594592000L),
                        new MaterialInfoDefinition.Part("DarkAshes", 1297296000L))
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9180, "Quartzite")
                .displayName("Quartzite")
                .color(0xE6CDCD)
                .texture(MaterialTextureSet.QUARTZ)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.STONE)
                .localName("Quartzite")
                .tools(1, 1.7F, 32L, 1)
                .smelting("SiliconDioxide", 648648000L)
                .byproducts("CertusQuartz", "Barite", "Hematite")
                .forms("DUSTS", "GEMS", "ORES", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9190, "Shale")
                .displayName("Shale")
                .color(0x8E8EA8)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1802)
                .boilingPointKelvin(3305)
                .density(0.9050852F)
                .chemicalFormula("(Ca(CO₃))₂((SiO₂))(((Al₂O₃)(SiO₂)₄)₂(H₂O))")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Shale")
                .tools(1, 2.0F, 16L, 0)
                .composition(
                        4L,
                        new MaterialInfoDefinition.Part("Calcite", 1297296000L),
                        new MaterialInfoDefinition.Part("MilkyQuartz", 648648000L),
                        new MaterialInfoDefinition.Part("Clay", 648648000L))
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8528, "SkyStone")
                .displayName("Sky Stone")
                .color(0x515C60)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(2200)
                .boilingPointKelvin(4400)
                .density(2.35098F)
                .chemicalFormula("Peridot₂RareEarthFeObsidian₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Sky Stone")
                .source("appliedenergistics2", "Applied Energistics")
                .tools(1, 5.0F, 64L, 2)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9222, "Slate")
                .displayName("Slate")
                .color(0x94979C)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Slate")
                .tools(1, 2.0F, 16L, 0)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8500, "Stone")
                .displayName("Stone")
                .color(0xCDCDCD)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1100)
                .boilingPointKelvin(2200)
                .density(0.777486F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("Stone")
                .source("minecraft", "Minecraft")
                .tools(1, 2.0F, 16L, 1)
                .forms("DUSTS", "MOLTEN", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9261, "TitanRock")
                .displayName("TitanRock")
                .color(0xBDBDBD)
                .texture(MaterialTextureSet.STONE)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(3000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE)
                .localName("TitanRock")
                .source("ExtraPlanets", "Extra Planets")
                .tools(1, 5.0F, 128L, 1)
                .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
    }
}
