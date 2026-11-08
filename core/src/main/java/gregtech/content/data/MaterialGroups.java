package gregtech.content.data;

import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.material.MaterialGroupKey;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialViews;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generic "Any X" unification materials from GregTech 6 ANY.java.
 * Compatibility symbols; membership is declared in the canonical group catalog.
 */
public class MaterialGroups {
    protected MaterialGroups() {}

    private static GTMaterial any(String name) {
        return GTMaterialRegistry.requireInstalled(name);
    }

    /** Stable key for the group represented by a compatibility {@code Any*} symbol. */
    public static MaterialGroupKey key(GTMaterial group) {
        return new MaterialGroupKey(new DataKey("gt6ce", group.getName().toLowerCase(java.util.Locale.ROOT)));
    }

    /** Canonical members from the published definition view, never a second group table. */
    public static Set<GTMaterial> members(GTMaterial group) {
        var view = MaterialViews.catalog().tags().group(key(group)).orElse(null);
        if (view == null) return Set.of();
        var result = new LinkedHashSet<GTMaterial>();
        for (MaterialKey member : view.members()) {
            GTMaterial material = GTMaterialRegistry.get(member.name());
            if (material.isValid()) result.add(material.resolve());
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(result));
    }

    public static boolean contains(GTMaterial group, GTMaterial material) {
        return material != null && members(group).contains(material.resolve());
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
