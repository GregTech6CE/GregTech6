package gregtech.content.material.catalog;

import gregtech.api.material.MaterialProperty;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialFacts;
import java.util.ArrayList;
import java.util.List;

/** Canonical declarations migrated from the GT6 MT/TD import and the port's explicit corrections.
 * Historical extraction tools are unavailable; these declarations are maintained directly.
 * Intrinsic facts only: recipe sources and world-generation remain owned by their providers. */
public final class GroupMaterials {
    private GroupMaterials() {}

    public static List<MaterialDefinition> definitions() {
        var result = new ArrayList<MaterialDefinition>();
        part0(result);
        part1(result);
        part2(result);
        part3(result);
        return List.copyOf(result);
    }

    private static void part0(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(-1, "AnyAmber")
                .displayName("AnyAmber")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyAmber")
                        .group()
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyAmethyst")
                .displayName("AnyAmethyst")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyAmethyst")
                        .group()
                        .members("Amethyst")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyAshes")
                .displayName("AnyAshes")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyAshes")
                        .group()
                        .members("Ashes", "DarkAshes")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyAventurine")
                .displayName("AnyAventurine")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyAventurine")
                        .group()
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyBlackSteel")
                .displayName("AnyBlackSteel")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyBlackSteel")
                        .group()
                        .members("BlackSteel")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyBlaze")
                .displayName("AnyBlaze")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyBlaze").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyBlueSteel")
                .displayName("AnyBlueSteel")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyBlueSteel")
                        .group()
                        .members("BlueSteel")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyBronze")
                .displayName("AnyBronze")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyBronze").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyCalcite")
                .displayName("AnyCalcite")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyCalcite")
                        .group()
                        .members("Calcite", "Limestone", "Marble")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyCarbon")
                .displayName("AnyCarbon")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyCarbon")
                        .group()
                        .members("Carbon", "Graphite")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyClay")
                .displayName("AnyClay")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyClay")
                        .group()
                        .members("Clay")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyCoal/Carbon")
                .displayName("AnyCoal/Carbon")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyCoal/Carbon")
                        .group()
                        .members("Carbon", "Charcoal", "Coal", "CoalCoke", "Graphite")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyCopper")
                .displayName("AnyCopper")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyCopper")
                        .group()
                        .members("AnnealedCopper", "Copper")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyDefaultWood")
                .displayName("AnyDefaultWood")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyDefaultWood")
                        .group()
                        .members(
                                "Acacia",
                                "Birch",
                                "BlueMahoe",
                                "BlueSpruce",
                                "Cherrywood",
                                "Cinnamonwood",
                                "Coconutwood",
                                "Crimsonwood",
                                "DarkOak",
                                "Hazel",
                                "Junglewood",
                                "Mangrove",
                                "Maple",
                                "Oak",
                                "Rainbowood",
                                "Spruce",
                                "Warpedwood",
                                "Willow",
                                "Wood",
                                "WoodCompressed",
                                "WoodDead",
                                "WoodFrozen",
                                "WoodMossy",
                                "WoodRotten")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyDiamond")
                .displayName("AnyDiamond")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyDiamond")
                        .group()
                        .members(
                                "BlueDiamond",
                                "Diamond",
                                "DiamondIndustrial",
                                "GreenDiamond",
                                "PinkDiamond",
                                "RedDiamond",
                                "YellowDiamond")
                        .build())
                .build());
    }

    private static void part1(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(-1, "AnyEmerald")
                .displayName("AnyEmerald")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyEmerald").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyFlour")
                .displayName("AnyFlour")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyFlour")
                        .group()
                        .members("Potato", "Rye", "Wheat")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyFlourOrGrains")
                .displayName("AnyFlourOrGrains")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyFlourOrGrains")
                        .group()
                        .members("Potato")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyFluorite")
                .displayName("AnyFluorite")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyFluorite")
                        .group()
                        .members(
                                "Fluorite",
                                "RedFluorite",
                                "PinkFluorite",
                                "BlueFluorite",
                                "GreenFluorite",
                                "BlackFluorite",
                                "WhiteFluorite",
                                "YellowFluorite",
                                "OrangeFluorite",
                                "MagentaFluorite")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyGarnet")
                .displayName("AnyGarnet")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyGarnet").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyGlowstone")
                .displayName("AnyGlowstone")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyGlowstone").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyGrains")
                .displayName("AnyGrains")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyGrains").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyHardPlastic")
                .displayName("AnyHardPlastic")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyHardPlastic")
                        .group()
                        .members("PVC")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyIron")
                .displayName("AnyIron")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyIron")
                        .group()
                        .members("CastIron", "Iron", "WroughtIron")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyIronOrSteel")
                .displayName("AnyIronOrSteel")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyIronOrSteel")
                        .group()
                        .members("CastIron", "Iron", "Knightmetal", "Steel", "WroughtIron")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyIronSteel")
                .displayName("AnyIron-Steel")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyIron-Steel")
                        .group()
                        .members("Knightmetal", "Steel")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyJasper")
                .displayName("AnyJasper")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyJasper").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyMagicalWood")
                .displayName("AnyMagicalWood")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyMagicalWood")
                        .group()
                        .members("Dreamwood", "Greatwood", "Livingwood", "Rainbowood", "Silverwood")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyMagicIron")
                .displayName("AnyMagicIron")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyMagicIron")
                        .group()
                        .members("FierySteel", "Manasteel", "Thaumium")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyMetal")
                .displayName("AnyMetal")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyMetal").group().build())
                .build());
    }

    private static void part2(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(-1, "AnyNormalWood")
                .displayName("AnyNormalWood")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyNormalWood")
                        .group()
                        .members(
                                "Acacia",
                                "Bamboo",
                                "Birch",
                                "BlueMahoe",
                                "BlueSpruce",
                                "Cherrywood",
                                "Cinnamonwood",
                                "Coconutwood",
                                "Crimsonwood",
                                "DarkOak",
                                "Hazel",
                                "Junglewood",
                                "Mangrove",
                                "Maple",
                                "Oak",
                                "Rainbowood",
                                "Spruce",
                                "Warpedwood",
                                "Willow",
                                "Wood",
                                "WoodCompressed",
                                "WoodDead",
                                "WoodFrozen",
                                "WoodMossy",
                                "WoodRotten",
                                "WoodRubber")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyPhosphorus")
                .displayName("AnyPhosphorus")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyPhosphorus")
                        .group()
                        .members("BluePhosphorus", "Phosphorus", "RedPhosphorus", "WhitePhosphorus")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyPlastic")
                .displayName("AnyPlastic")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyPlastic")
                        .group()
                        .members("PVC", "Plastic", "Teflon")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyPrismarine")
                .displayName("AnyPrismarine")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyPrismarine")
                        .group()
                        .members("Prismarine", "PrismarineDark")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyRedSteel")
                .displayName("AnyRedSteel")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyRedSteel")
                        .group()
                        .members("RedSteel")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyRubber")
                .displayName("AnyRubber")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyRubber")
                        .group()
                        .members("Rubber")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnySalt")
                .displayName("AnySalt")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnySalt")
                        .group()
                        .members("CalciumChloride", "LithiumChloride", "MagnesiumChloride", "Salt", "Sylvite")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnySand")
                .displayName("AnySand")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnySand")
                        .group()
                        .members("RedSand", "Sand")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnySapphire")
                .displayName("AnySapphire")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnySapphire").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnySilicon")
                .displayName("AnySilicon")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnySilicon")
                        .group()
                        .members("Silicon")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnySiliconDioxide")
                .displayName("AnySiliconDioxide")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnySiliconDioxide")
                        .group()
                        .members("Flint", "Glass", "Quartzite", "RedSand", "Sand", "SiliconDioxide")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnySteel")
                .displayName("AnySteel")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnySteel").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyStone")
                .displayName("AnyStone")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyStone").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyThaumicCrystal")
                .displayName("AnyThaumicCrystal")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyThaumicCrystal")
                        .group()
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyTigerEye")
                .displayName("AnyTigerEye")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyTigerEye").group().build())
                .build());
    }

    private static void part3(List<MaterialDefinition> result) {
        result.add(MaterialDefinition.builder(-1, "AnyTreatedWood")
                .displayName("AnyTreatedWood")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyTreatedWood")
                        .group()
                        .members("WoodPolished", "WoodTreated")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyTungsten")
                .displayName("AnyTungsten")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyTungsten")
                        .group()
                        .members("Tungsten", "TungstenSintered")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyUntreatedWood")
                .displayName("AnyUntreatedWood")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyUntreatedWood")
                        .group()
                        .members(
                                "Acacia",
                                "Bamboo",
                                "Birch",
                                "BlueMahoe",
                                "BlueSpruce",
                                "Cherrywood",
                                "Cinnamonwood",
                                "Coconutwood",
                                "Crimsonwood",
                                "DarkOak",
                                "Dreamwood",
                                "Greatwood",
                                "Hazel",
                                "Junglewood",
                                "Livingwood",
                                "Mangrove",
                                "Maple",
                                "Oak",
                                "Rainbowood",
                                "Silverwood",
                                "Spruce",
                                "Warpedwood",
                                "Willow",
                                "Wood",
                                "WoodCompressed",
                                "WoodDead",
                                "WoodFrozen",
                                "WoodMossy",
                                "WoodRotten",
                                "WoodRubber")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyWax")
                .displayName("AnyWax")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("AnyWax").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyWood")
                .displayName("AnyWood")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyWood")
                        .group()
                        .members(
                                "Acacia",
                                "Birch",
                                "BlueMahoe",
                                "BlueSpruce",
                                "Cherrywood",
                                "Cinnamonwood",
                                "Coconutwood",
                                "Crimsonwood",
                                "DarkOak",
                                "Hazel",
                                "Junglewood",
                                "Mangrove",
                                "Maple",
                                "Oak",
                                "Rainbowood",
                                "Spruce",
                                "Warpedwood",
                                "Willow",
                                "Wood",
                                "WoodCompressed",
                                "WoodDead",
                                "WoodFrozen",
                                "WoodMossy",
                                "WoodRotten")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "AnyWoodOrPlastic")
                .displayName("AnyWoodOrPlastic")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("AnyWoodOrPlastic")
                        .group()
                        .members(
                                "Acacia",
                                "Birch",
                                "BlueMahoe",
                                "BlueSpruce",
                                "Cherrywood",
                                "Cinnamonwood",
                                "Coconutwood",
                                "Crimsonwood",
                                "DarkOak",
                                "Hazel",
                                "Junglewood",
                                "Mangrove",
                                "Maple",
                                "Oak",
                                "PetrifiedWood",
                                "Rainbowood",
                                "Spruce",
                                "Warpedwood",
                                "Willow",
                                "WoodCompressed",
                                "WoodDead",
                                "WoodFrozen",
                                "WoodMossy",
                                "WoodRotten")
                        .build())
                .build());
        result.add(MaterialDefinition.builder(-1, "Hexorium")
                .displayName("Hexorium")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder().localName("Hexorium").group().build())
                .build());
        result.add(MaterialDefinition.builder(-1, "Quartz")
                .displayName("Quartz")
                .color(0xFFFFFF)
                .texture(null)
                .meltingPointKelvin(300)
                .boilingPointKelvin(1000)
                .density(1.0F)
                .atomicProperties(43, 43, 55, 0)
                .alpha(255)
                .properties(MaterialProperty.HIDDEN)
                .facts(MaterialFacts.builder()
                        .localName("Quartz")
                        .group()
                        .members("Quartzite")
                        .build())
                .build());
    }
}
