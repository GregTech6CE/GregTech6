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
public final class SpecialMaterials {
    private SpecialMaterials() {}

    public static List<MaterialDefinition> definitions() {
        var result = new ArrayList<MaterialDefinition>();
        part0(result);
        part1(result);
        return List.copyOf(result);
    }

    private static void part0(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(4290, "AntiCopper")
                .displayName("Anti-Copper")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(2000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.ANTIMATTER, MaterialProperty.ELEMENT, MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Copper").build())
                .build());
        result.add(MaterialDefinition.builder(4020, "AntiHelium")
                .displayName("Anti-Helium")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(2000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.ANTIMATTER, MaterialProperty.ELEMENT, MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Helium").build())
                .build());
        result.add(MaterialDefinition.builder(4010, "AntiHydrogen")
                .displayName("Anti-Hydrogen")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(2000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.ANTIMATTER, MaterialProperty.ELEMENT, MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Hydrogen").build())
                .build());
        result.add(MaterialDefinition.builder(4260, "AntiIron")
                .displayName("Anti-Iron")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(1000)
                .boilingPointKelvin(2000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.ANTIMATTER, MaterialProperty.ELEMENT, MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Iron").build())
                .build());
        result.add(MaterialDefinition.builder(4002, "AntiNeutrino")
                .displayName("Anti-Neutrino")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("v")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Neutrino").build())
                .build());
        result.add(MaterialDefinition.builder(4003, "AntiNeutron")
                .displayName("Anti-Neutron")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("n")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Neutron").build())
                .build());
        result.add(MaterialDefinition.builder(4001, "AntiPhoton")
                .displayName("Anti-Photon")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("y")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Photon").build())
                .build());
        result.add(MaterialDefinition.builder(4004, "AntiProton")
                .displayName("Anti-Proton")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("p")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Anti-Proton").build())
                .build());
        result.add(MaterialDefinition.builder(9223, "Blackstone")
                .displayName("Blackstone")
                .color(0x1E1414)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Blackstone")
                        .tools(1, 5.0F, 64L, 1)
                        .forms("DUSTS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(5, "Electron")
                .displayName("Electron")
                .color(0x0000FF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(0)
                .boilingPointKelvin(0)
                .density(0.0F)
                .chemicalFormula("e")
                .atomicProperties(0, 1, 0, 0)
                .alpha(0)
                .properties(MaterialProperty.HIDDEN, MaterialProperty.PARTICLE)
                .facts(MaterialFacts.builder().localName("Electron").build())
                .build());
        result.add(MaterialDefinition.builder(4000, "Magic")
                .displayName("Magic")
                .color(0xFF00FF)
                .texture(MaterialTextureSet.SHINY)
                .meltingPointKelvin(0)
                .boilingPointKelvin(0)
                .density(0.0F)
                .chemicalFormula("Ma")
                .atomicProperties(0, 0, 0, -1)
                .alpha(0)
                .properties(MaterialProperty.ELEMENT, MaterialProperty.HIDDEN, MaterialProperty.MAGICAL)
                .facts(MaterialFacts.builder()
                        .localName("Ma")
                        .source("gt6ce", "GregTech 6 Community Edition")
                        .correctedToolStats()
                        .tools(3, 10.0F, 5120L, 5)
                        .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                        .aliases("Ma")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9901, "MudBrick")
                .displayName("Mud Brick")
                .color(0x93704F)
                .texture(MaterialTextureSet.BRICK)
                .meltingPointKelvin(2000)
                .boilingPointKelvin(4000)
                .density(0.81833994F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder().localName("Mud Brick").build())
                .build());
        result.add(MaterialDefinition.builder(2, "Neutrino")
                .displayName("Neutrino")
                .color(0xB4B4B4)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(0)
                .boilingPointKelvin(0)
                .density(0.0F)
                .chemicalFormula("v")
                .atomicProperties(0, 0, 0, 0)
                .alpha(0)
                .properties(MaterialProperty.HIDDEN, MaterialProperty.PARTICLE)
                .facts(MaterialFacts.builder().localName("Neutrino").build())
                .build());
        result.add(MaterialDefinition.builder(3, "Neutron")
                .displayName("Neutron")
                .color(0x808080)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(0)
                .boilingPointKelvin(0)
                .density(0.0F)
                .chemicalFormula("n")
                .atomicProperties(0, 0, 1, 0)
                .alpha(0)
                .properties(MaterialProperty.HIDDEN, MaterialProperty.PARTICLE)
                .facts(MaterialFacts.builder().localName("Neutron").build())
                .build());
        result.add(MaterialDefinition.builder(1, "Photon")
                .displayName("Photon")
                .color(0xFFFFFF)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(0)
                .boilingPointKelvin(0)
                .density(0.0F)
                .chemicalFormula("y")
                .atomicProperties(0, 0, 0, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN, MaterialProperty.PARTICLE)
                .facts(MaterialFacts.builder().localName("Photon").build())
                .build());
    }

    private static void part1(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(8273, "Porcelain")
                .displayName("Porcelain")
                .color(0xEBEBF5)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(1800)
                .boilingPointKelvin(3600)
                .density(0.80662286F)
                .chemicalFormula("((Al₂O₃)(SiO₂)₄)₂(SiO₂)(K₂(Al₂O₃)(SiO₂)₆O)")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.CENTRIFUGE,
                        MaterialProperty.DECOMPOSABLE,
                        MaterialProperty.DUST,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.UNIT,
                        MaterialProperty.UUM)
                .facts(MaterialFacts.builder()
                        .localName("Porcelain")
                        .composition(
                                4L,
                                new MaterialFacts.Part("Ceramic", 1297296000L),
                                new MaterialFacts.Part("SiliconDioxide", 648648000L),
                                new MaterialFacts.Part("PotassiumFeldspar", 648648000L))
                        .forms("DUSTS", "PLANTS", "PLATES")
                        .processing("Compressing", "Porcelain", 0L)
                        .processing("Bending", "Porcelain", 0L)
                        .processing("Forging", "Porcelain", 0L)
                        .processing("Smashing", "Porcelain", 0L)
                        .build())
                .build());
        result.add(MaterialDefinition.builder(4005, "Positron")
                .displayName("Positron")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .chemicalFormula("e")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("Positron")
                        .aliases("AntiElectron")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(4, "Proton")
                .displayName("Proton")
                .color(0xFF0000)
                .texture(MaterialTextureSet.DULL)
                .meltingPointKelvin(0)
                .boilingPointKelvin(0)
                .density(0.0F)
                .chemicalFormula("p")
                .atomicProperties(1, 0, 0, 0)
                .alpha(0)
                .properties(MaterialProperty.HIDDEN, MaterialProperty.PARTICLE)
                .facts(MaterialFacts.builder().localName("Proton").build())
                .build());
        result.add(MaterialDefinition.builder(9903, "Purpur")
                .displayName("Purpur")
                .color(0xAA7AAA)
                .texture(MaterialTextureSet.BRICK)
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
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder().localName("Purpur").build())
                .build());
        result.add(MaterialDefinition.builder(9902, "RedNetherBrick")
                .displayName("Red Nether Brick")
                .color(0x6A1317)
                .texture(MaterialTextureSet.BRICK)
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
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder().localName("Red Nether Brick").build())
                .build());
        result.add(MaterialDefinition.builder(8472, "Sculk")
                .displayName("Sculk")
                .color(0x111B21)
                .texture(MaterialTextureSet.FINE)
                .meltingPointKelvin(4000)
                .boilingPointKelvin(8000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT)
                .facts(MaterialFacts.builder()
                        .localName("Sculk")
                        .source("minecraft", "Minecraft")
                        .tools(1, 2.0F, 16L, 1)
                        .forms("DUSTS", "PLANTS", "PROJECTILES", "STICKS")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(9900, "Tuff")
                .displayName("Tuff")
                .color(0x696D6E)
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
                        MaterialProperty.STONE,
                        MaterialProperty.UNIT)
                .facts(MaterialFacts.builder().localName("Tuff").build())
                .build());
    }
}
