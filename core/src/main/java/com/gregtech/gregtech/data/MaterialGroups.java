package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.data.ImportedMaterialData;

/**
 * Generic "Any X" unification materials from GregTech 6 ANY.java.
 * Auto-transpiled  - do not edit by hand.
 */
public class MaterialGroups {
    protected MaterialGroups() {}

    private static boolean initialized = false;

    private static GTMaterial any(String name) {
        return GTMaterialRegistry.createMaterial(-1, name, name, 0xFFFFFF)
                .put(MaterialProperty.HIDDEN);
    }

    public static final GTMaterial
            Glowstone = any("AnyGlowstone"),
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

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // MT:208 diamond(...) puts every diamond in ANY.Diamond; ANY:97 also adds Diamantine.
        Diamond.addReRegistrationToThis(GT6Materials.Compounds.Diamond, GT6Materials.Compounds.DiamondBlue,
                GT6Materials.Compounds.DiamondGreen, GT6Materials.Compounds.DiamondPurple,
                GT6Materials.Compounds.DiamondRed, GT6Materials.Compounds.DiamondYellow,
                GT6Materials.Compounds.DiamondPink, GT6Materials.Compounds.DiamondIndustrial);
        var diamantine = GTMaterialRegistry.get("Diamantine");
        if (diamantine.isValid()) Diamond.addReRegistrationToThis(diamantine);
        Amber.addReRegistrationToThis(GT6Materials.Compounds.AmberGolden, GT6Materials.Compounds.AmberDominican);
        Amethyst.addReRegistrationToThis(GT6Materials.Compounds.Amethyst, GT6Materials.Compounds.EnderAmethyst);
        Ash.addReRegistrationToThis(GT6Materials.Compounds.Ash, GT6Materials.Compounds.DarkAsh, GT6Materials.Compounds.VolcanicAsh);
        BlackSteel.addReRegistrationToThis(GT6Materials.Compounds.BlackSteel, GT6Materials.Compounds.MeteoricBlackSteel, GT6Materials.Compounds.MeteoflameBlackSteel);
        BlueSteel.addReRegistrationToThis(GT6Materials.Compounds.BlueSteel, GT6Materials.Compounds.MeteoricBlueSteel, GT6Materials.Compounds.MeteoflameBlueSteel);
        C.addReRegistrationToThis(GT6Materials.Elements.C, GT6Materials.Ores.Graphite);
        Calcite.addReRegistrationToThis(GT6Materials.Ores.CaCO3, GT6Materials.Stones.Marble, GT6Materials.Ores.Chalk, GT6Materials.Stones.Limestone, GT6Materials.Ores.Dolomite);
        Clay.addReRegistrationToThis(GT6Materials.Ores.Clay);
        Coal.addReRegistrationToThis(GT6Materials.Elements.C, GT6Materials.Ores.Graphite, GT6Materials.Compounds.CoalCoke, GT6Materials.Compounds.Coal, GT6Materials.Compounds.Charcoal);
        Cu.addReRegistrationToThis(GT6Materials.Elements.Cu, GT6Materials.Compounds.AnnealedCopper);
        Fe.addReRegistrationToThis(GT6Materials.Elements.Fe, GT6Materials.Elements.WroughtIron, GT6Materials.Compounds.IronCast, GT6Materials.Compounds.IronCompressed, GT6Materials.Compounds.PigIron, GT6Materials.Compounds.MeteoricIron, GT6Materials.Compounds.Meteorite);
        Flour.addReRegistrationToThis(GT6Materials.Compounds.Wheat, GT6Materials.Compounds.Rye, GT6Materials.Compounds.Oat, GT6Materials.Compounds.OatAbyssal, GT6Materials.Compounds.Barley, GT6Materials.Compounds.Potato, GT6Materials.Compounds.Corn);
        FlourGrains.addReRegistrationToThis(GT6Materials.Compounds.Potato);
        Iron.addReRegistrationToThis(GT6Materials.Elements.Fe, GT6Materials.Elements.WroughtIron, GT6Materials.Compounds.IronCast, GT6Materials.Compounds.IronCompressed, GT6Materials.Compounds.PigIron, GT6Materials.Compounds.MeteoricIron, GT6Materials.Compounds.Meteorite, GT6Materials.Compounds.Steel, GT6Materials.Compounds.Knightmetal, GT6Materials.Compounds.MeteoricSteel);
        MagicIron.addReRegistrationToThis(GT6Materials.Compounds.Manasteel, GT6Materials.Compounds.Thaumium, GT6Materials.Compounds.DarkThaumium, GT6Materials.Compounds.SpectreIron, GT6Materials.Compounds.FierySteel, GT6Materials.Compounds.MeteoflameSteel);
        Phosphorus.addReRegistrationToThis(GT6Materials.Compounds.Phosphorus, GT6Materials.Compounds.PhosphorusBlue, GT6Materials.Compounds.PhosphorusRed, GT6Materials.Compounds.PhosphorusWhite);
        Plastic.addReRegistrationToThis(GT6Materials.Compounds.Polycarbonate, GT6Materials.Compounds.PVC, GT6Materials.Compounds.Teflon, GT6Materials.Compounds.Bakelite, GT6Materials.Compounds.Plastic);
        PlasticHard.addReRegistrationToThis(GT6Materials.Compounds.Polycarbonate, GT6Materials.Compounds.PVC);
        Prismarine.addReRegistrationToThis(GT6Materials.Stones.PrismarineLight, GT6Materials.Stones.PrismarineDark);
        Quartz.addReRegistrationToThis(GT6Materials.Stones.Quartzite);
        RedSteel.addReRegistrationToThis(GT6Materials.Compounds.RedSteel, GT6Materials.Compounds.MeteoricRedSteel, GT6Materials.Compounds.MeteoflameRedSteel);
        Rubber.addReRegistrationToThis(GT6Materials.Compounds.Rubber);
        Salt.addReRegistrationToThis(GT6Materials.Ores.NaCl, GT6Materials.Ores.KCl, GT6Materials.Ores.LiCl, GT6Materials.Ores.MgCl2, GT6Materials.Ores.CaCl2);
        Sand.addReRegistrationToThis(GT6Materials.Compounds.Sand, GT6Materials.Compounds.RedSand);
        Si.addReRegistrationToThis(GT6Materials.Elements.Si);
        SiO2.addReRegistrationToThis(GT6Materials.Stones.Quartzite, GT6Materials.Ores.SiO2, GT6Materials.Compounds.Sand, GT6Materials.Compounds.RedSand, GT6Materials.Compounds.EndSandWhite, GT6Materials.Compounds.EndSandBlack, GT6Materials.Compounds.Glass, GT6Materials.Compounds.Flint);
        Steel.addReRegistrationToThis(GT6Materials.Compounds.Steel, GT6Materials.Compounds.Knightmetal, GT6Materials.Compounds.MeteoricSteel);
        W.addReRegistrationToThis(GT6Materials.Elements.W, GT6Materials.Compounds.TungstenSintered);
        WoodDefault.addReRegistrationToThis(GT6Materials.Woods.Wood, GT6Materials.Woods.Peanutwood);
        WoodMagical.addReRegistrationToThis(GT6Materials.Woods.Greatwood, GT6Materials.Woods.Silverwood, GT6Materials.Woods.Livingwood, GT6Materials.Woods.Dreamwood, GT6Materials.Woods.Shimmerwood, GT6Materials.Woods.Magic, GT6Materials.Woods.Tainted, GT6Materials.Woods.Witchwood, GT6Materials.Woods.Rainbowood);
        WoodNormal.addReRegistrationToThis(GT6Materials.Woods.WoodRubber, GT6Materials.Woods.Weedwood, GT6Materials.Woods.Skyroot, GT6Materials.Woods.Bamboo, GT6Materials.Woods.Wood, GT6Materials.Woods.Peanutwood);
        WoodPlastic.addReRegistrationToThis(GT6Materials.Compounds.PetrifiedWood);
        WoodTreated.addReRegistrationToThis(GT6Materials.Woods.WoodTreated, GT6Materials.Woods.WoodPolished);
        WoodUntreated.addReRegistrationToThis(GT6Materials.Woods.Greatwood, GT6Materials.Woods.Silverwood, GT6Materials.Woods.Livingwood, GT6Materials.Woods.Dreamwood, GT6Materials.Woods.Shimmerwood, GT6Materials.Woods.Magic, GT6Materials.Woods.Tainted, GT6Materials.Woods.Witchwood, GT6Materials.Woods.Rainbowood, GT6Materials.Woods.WoodRubber, GT6Materials.Woods.Weedwood, GT6Materials.Woods.Skyroot, GT6Materials.Woods.Bamboo, GT6Materials.Woods.Wood, GT6Materials.Woods.Peanutwood);
        // MT.woodnormal registers these families in its factory, before ANY.init's explicit rows.
        for (var material : com.gregtech.gregtech.api.material.MaterialFactories.normalWoods()) {
            Wood.addReRegistrationToThis(material);
            WoodPlastic.addReRegistrationToThis(material);
            WoodNormal.addReRegistrationToThis(material);
            WoodDefault.addReRegistrationToThis(material);
            WoodUntreated.addReRegistrationToThis(material);
        }
    }
}
