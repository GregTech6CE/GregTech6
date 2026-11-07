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
public final class WoodMaterials {
    private WoodMaterials() {}

    public static List<MaterialDefinition.Declaration> definitions() {
        var result = new ArrayList<MaterialDefinition.Declaration>();
        part0(result);
        part1(result);
        part2(result);
        return List.copyOf(result);
    }

    private static void part0(List<MaterialDefinition.Declaration> result) {
        result.add(MaterialDefinition.builder(9304, "Acacia")
                .displayName("Acacia")
                .color(0xBA683B)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Acacia")
                .tools(1, 2.5F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8418, "Bamboo")
                .displayName("Bamboo")
                .color(0xC6AF4C)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(350)
                .boilingPointKelvin(450)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Bamboo")
                .source("minecraft", "Minecraft")
                .tools(1, 2.0F, 32L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 324324000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9301, "Birch")
                .displayName("Birch")
                .color(0xD7CC8E)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Birch")
                .tools(1, 2.5F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9315, "BlueMahoe")
                .displayName("Blue Mahoe")
                .color(0x0F67FE)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Blue Mahoe")
                .tools(1, 3.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9409, "BlueSpruce")
                .displayName("Blue Spruce")
                .color(0xD5D5D9)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Blue Spruce")
                .tools(1, 3.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9344, "Cherrywood")
                .displayName("Cherrywood")
                .color(0xE4C8C3)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Cherrywood")
                .source("minecraft", "Minecraft")
                .tools(1, 2.5F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Cherry")
                .build());
        result.add(MaterialDefinition.builder(9317, "Cinnamonwood")
                .displayName("Cinnawood")
                .color(0x41C0C0)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Cinnawood")
                .source("harvestcraft", "HarvestCraft")
                .tools(1, 1.5F, 16L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9318, "Coconutwood")
                .displayName("Coconutwood")
                .color(0xFFAA00)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Coconut")
                .tools(1, 3.0F, 16L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Coconut")
                .build());
        result.add(MaterialDefinition.builder(9306, "Crimsonwood")
                .displayName("Crimsonwood")
                .color(0xB45A6A)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Crimsonwood")
                .source("minecraft", "Minecraft")
                .tools(1, 2.5F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Crimson")
                .build());
        result.add(MaterialDefinition.builder(9305, "DarkOak")
                .displayName("Dark Oak")
                .color(0x462D15)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Dark Oak")
                .tools(1, 3.5F, 32L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8290, "Dreamwood")
                .displayName("Dreamwood")
                .color(0xC8F0F0)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(350)
                .boilingPointKelvin(550)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Dreamwood")
                .source("Botania", "Botania")
                .tools(1, 4.0F, 128L, 1)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 324324000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8296, "Greatwood")
                .displayName("Greatwood")
                .color(0x3C1E19)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(600)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Greatwood")
                .source("Thaumcraft", "Thaumcraft")
                .tools(1, 6.0F, 64L, 1)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 324324000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9316, "Hazel")
                .displayName("Hazel")
                .color(0xE4AFAF)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Hazel")
                .tools(1, 2.5F, 16L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9303, "Junglewood")
                .displayName("Junglewood")
                .color(0xB1805C)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Junglewood")
                .tools(1, 2.0F, 16L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Jungle")
                .build());
        result.add(MaterialDefinition.builder(8289, "Livingwood")
                .displayName("Livingwood")
                .color(0x3C1E00)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(350)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Livingwood")
                .source("Botania", "Botania")
                .tools(1, 4.0F, 64L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 324324000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
    }

    private static void part1(List<MaterialDefinition.Declaration> result) {
        result.add(MaterialDefinition.builder(9372, "Mangrove")
                .displayName("Mangrove")
                .color(0x6E292C)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Mangrove")
                .source("minecraft", "Minecraft")
                .tools(1, 2.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9313, "Maple")
                .displayName("Maple")
                .color(0x971A1A)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Maple")
                .tools(1, 3.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9300, "Oak")
                .displayName("Oak")
                .color(0xB4905A)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Oak")
                .tools(1, 3.0F, 32L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9319, "Rainbowood")
                .displayName("Rainbowood")
                .color(0xC840F5)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MAGICAL,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Rainbowood")
                .tools(1, 4.0F, 64L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8297, "Silverwood")
                .displayName("Silverwood")
                .color(0xEADED2)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(450)
                .boilingPointKelvin(650)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Silverwood")
                .source("Thaumcraft", "Thaumcraft")
                .tools(1, 8.0F, 128L, 1)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 0L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9302, "Spruce")
                .displayName("Spruce")
                .color(0x664F2F)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Spruce")
                .tools(1, 3.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9307, "Warpedwood")
                .displayName("Warped Wood")
                .color(0x2A8D85)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Warped Wood")
                .source("minecraft", "Minecraft")
                .tools(1, 3.5F, 32L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Warped")
                .build());
        result.add(MaterialDefinition.builder(9314, "Willow")
                .displayName("Willow")
                .color(0x259600)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Willow")
                .tools(1, 2.0F, 16L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8221, "Wood")
                .displayName("Wood")
                .color(0x643200)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Wood")
                .source("minecraft", "Minecraft")
                .tools(1, 2.0F, 16L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9308, "WoodCompressed")
                .displayName("Compressed Wood")
                .color(0x5E3C19)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Compressed Wood")
                .tools(1, 1.5F, 8L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .pulverizing("Wood", 648648000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Compressed")
                .build());
        result.add(MaterialDefinition.builder(9309, "WoodDead")
                .displayName("Dead Wood")
                .color(0x746C3F)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Dead Wood")
                .tools(1, 1.5F, 8L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Dead")
                .build());
        result.add(MaterialDefinition.builder(9312, "WoodFrozen")
                .displayName("Frozen Wood")
                .color(0x547D7D)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Frozen Wood")
                .tools(1, 1.0F, 8L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Frozen")
                .build());
        result.add(MaterialDefinition.builder(9311, "WoodMossy")
                .displayName("Mossy Wood")
                .color(0x1D7F00)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Mossy Wood")
                .tools(1, 1.5F, 8L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Mossy")
                .build());
        result.add(MaterialDefinition.builder(8267, "WoodPolished")
                .displayName("WoodPolished")
                .color(0x3C1E00)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(500)
                .boilingPointKelvin(600)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("WoodPolished")
                .tools(1, 3.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(9310, "WoodRotten")
                .displayName("Rotten Wood")
                .color(0x162C0F)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(400)
                .boilingPointKelvin(500)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("Rotten Wood")
                .tools(1, 1.0F, 8L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 162162000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .aliases("Rotten")
                .build());
    }

    private static void part2(List<MaterialDefinition.Declaration> result) {
        result.add(MaterialDefinition.builder(8224, "WoodRubber")
                .displayName("WoodRubber")
                .color(0xB49600)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(350)
                .boilingPointKelvin(450)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_ORE_PROCESSING,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.MELTING,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("WoodRubber")
                .source("IC2", "IndustrialCraft 2")
                .tools(1, 1.5F, 12L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .smelting("Ashes", 324324000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
        result.add(MaterialDefinition.builder(8222, "WoodTreated")
                .displayName("WoodTreated")
                .color(0x502800)
                .texture(MaterialTextureSet.WOOD)
                .meltingPointKelvin(500)
                .boilingPointKelvin(600)
                .density(1.362F)
                .chemicalFormula("C₆(H₂O)₅")
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(
                        MaterialProperty.DUST,
                        MaterialProperty.FLAMMABLE,
                        MaterialProperty.GENERATE_DUST,
                        MaterialProperty.GENERATE_FOIL,
                        MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_PROJECTILE,
                        MaterialProperty.GENERATE_STICKS,
                        MaterialProperty.UUM,
                        MaterialProperty.WOOD)
                .classifications("wood_family")
                .localName("WoodSealed")
                .source("ImmersiveEngineering", "Immersive Engineering")
                .tools(1, 3.0F, 24L, 0)
                .fuel(100L, "Ashes", 72072000L)
                .composition(
                        21L,
                        new MaterialInfoDefinition.Part("Carbon", 3891888000L),
                        new MaterialInfoDefinition.Part("Water", 9729720000L))
                .forms("DUSTS", "FOILS", "PLANTS", "PLATES", "PROJECTILES", "STICKS")
                .build());
    }
}
