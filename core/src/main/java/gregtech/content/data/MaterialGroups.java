package gregtech.content.data;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;

/**
 * Generic "Any X" unification materials from GregTech 6 ANY.java.
 * Compatibility symbols; membership is declared in the canonical group catalog.
 */
public class MaterialGroups {
    protected MaterialGroups() {}

    private static GTMaterial any(String name) {
        return GTMaterialRegistry.requireInstalled(name);
    }

    public static final GTMaterial Glowstone = any("AnyGlowstone"),
            Diamond = any("AnyDiamond"),
            Sapphire = any("AnySapphire"),
            Emerald = any("AnyEmerald"),
            Amethyst = any("AnyAmethyst"),
            Garnet = any("AnyGarnet"),
            Jasper = any("AnyJasper"),
            TigerEye = any("AnyTigerEye"),
            Aventurine = any("AnyAventurine"),
            Amber = any("AnyAmber"),
            Fluorite = any("AnyFluorite"),
            Phosphorus = any("AnyPhosphorus"),
            Blaze = any("AnyBlaze"),
            Prismarine = any("AnyPrismarine"),
            Grains = any("AnyGrains"),
            Flour = any("AnyFlour"),
            FlourGrains = any("AnyFlourOrGrains"),
            Wax = any("AnyWax"),
            Stone = any("AnyStone"),
            Calcite = any("AnyCalcite"),
            Clay = any("AnyClay"),
            Salt = any("AnySalt"),
            Fe = any("AnyIron"),
            Iron = any("AnyIronOrSteel"),
            Steel = any("AnyIron-Steel"),
            BlackSteel = any("AnyBlackSteel"),
            BlueSteel = any("AnyBlueSteel"),
            RedSteel = any("AnyRedSteel"),
            MagicIron = any("AnyMagicIron"),
            Cu = any("AnyCopper"),
            Ash = any("AnyAshes"),
            C = any("AnyCarbon"),
            Coal = any("AnyCoal/Carbon"),
            Si = any("AnySilicon"),
            SiO2 = any("AnySiliconDioxide"),
            Quartz = any("Quartz"),
            Sand = any("AnySand"),
            W = any("AnyTungsten"),
            ThaumCrystal = any("AnyThaumicCrystal"),
            Hexorium = any("Hexorium"),
            Wood = any("AnyWood"),
            WoodDefault = any("AnyDefaultWood"),
            WoodNormal = any("AnyNormalWood"),
            WoodMagical = any("AnyMagicalWood"),
            WoodTreated = any("AnyTreatedWood"),
            WoodUntreated = any("AnyUntreatedWood"),
            WoodPlastic = any("AnyWoodOrPlastic"),
            Rubber = any("AnyRubber"),
            Plastic = any("AnyPlastic"),
            PlasticHard = any("AnyHardPlastic"),
            _Steel = any("AnySteel"),
            _Bronze = any("AnyBronze"),
            _Metal = any("AnyMetal"),
            CaF2 = Fluorite;
}
