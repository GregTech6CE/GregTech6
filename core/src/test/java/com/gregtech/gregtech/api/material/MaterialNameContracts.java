package com.gregtech.gregtech.api.material;

import java.time.LocalDate;
import java.util.function.Function;

/** Fixed source examples and condition boundaries, independent of the code generator. */
public final class MaterialNameContracts {
    private static int assertions;
    private static final Function<GTMaterial, String> LOCAL = GTMaterial::getDisplayNameFallback;
    private MaterialNameContracts() {}

    public static int verify() {
        assertions = 0;
        String[][] examples = {
            {"dust", "Wheat", "Flour"}, {"dustTiny", "Wheat", "Tiny Pile of Flour"},
            {"dust", "Oat", "Oatmeal"}, {"dust", "Corn", "Cornmeal"},
            {"rockGt", "Stone", "Rock"}, {"rockGt", "Netherrack", "Nether Rock"},
            {"rockGt", "MeteoricIron", "Meteorite"}, {"oreRaw", "MeteoricIron", "Big Meteorite"},
            {"gem", "Diamond", "Diamond"}, {"plateGem", "Diamond", "Crystalline Diamond Plate"},
            {"plateGem", "Glass", "Cast Glass Pane"}, {"gemChipped", "Glass", "Chipped Glass Crystal"},
            {"crateGtGem", "Glass", "Partial Crate of Glass Crystal"},
            {"blockDust", "Wheat", "Block of Flour"},
            {"plateTiny", "Paper", "Tiny piece of Paper"}, {"plate", "Paper", "Sheet of Paper"},
            {"plateDouble", "Paper", "Paperboard"}, {"plateTriple", "Paper", "Carton"},
            {"plateQuadruple", "Paper", "Cardboard"}, {"plateQuintuple", "Paper", "Thick Cardboard"},
            {"plateDense", "Paper", "Strong Cardboard"}, {"dust", "Paper", "Chad"},
            {"chemtube", "Empty", "Empty Glass Tube"}, {"arrowGtWood", "Empty", "Headless Wood Arrow"},
            {"arrowGtPlastic", "Empty", "Headless Plastic Arrow"},
            {"bulletGtSmall", "Empty", "Small Bullet Casing"},
            {"bulletGtMedium", "Empty", "Medium Bullet Casing"}, {"bulletGtLarge", "Empty", "Large Bullet Casing"},
            {"oreRaw", "Gold", "Raw Native Gold Ore"}, {"oreRaw", "Coal", "Raw Coal Ore"},
            {"gemChipped", "Ice", "Ice Cubes"}, {"dust", "Bone", "Bonemeal"},
            {"dust", "Blaze", "Blaze Powder"}, {"dust", "Blizz", "Blizz Powder"},
            {"dust", "Clay", "Clay Powder"}, {"foil", "Plastic", "Thin Plastic Sheet"},
            {"plate", "Rubber", "Rubber Sheet"}, {"stick", "WoodTreated", "Treated Stick"},
            {"dust", "AncientDebris", "Netherite Scrap Powder"}, {"gem", "InfusedAir", "Air Shard"},
            {"ore", "Pyrite", "Gold Ore"}, {"dust", "Sylvite", "Rock Salt"},
            {"itemCasing", "Steel", "Steel Item Casing"}
        };
        for (var row : examples) {
            GTMaterial material = GTMaterialRegistry.get(row[1]);
            check(material.getName().equals(row[1]), "existing canonical material " + row[1]);
            equal(row[2], OriginalMaterialNameRules.name(row[0], material, LOCAL, false), row[0] + row[1]);
        }
        equal("Medium Bolt Shaft", OriginalMaterialNameRules.name("bulletGtMedium", GTMaterialRegistry.get("Empty"), LOCAL, true), "April empty ammo");
        equal("Medium Steel Bolt", OriginalMaterialNameRules.name("bulletGtMedium", GTMaterialRegistry.get("Steel"), LOCAL, true), "April ammo");
        equal("Raw Native Localized Ore", OriginalMaterialNameRules.name("oreRaw", GTMaterialRegistry.get("Gold"), m -> "Localized", false), "caller display name");
        equal(null, OriginalMaterialNameRules.name("unit", GTMaterialRegistry.get("Steel"), LOCAL, false), "port-only form");
        equal(null, OriginalMaterialNameRules.name("glassTube", GTMaterialRegistry.get("Glass"), LOCAL, false), "no invented chemical-tube alias");
        for (int day : new int[]{1, 2, 3}) check(OriginalMaterialNameRules.aprilFools(LocalDate.of(2026, 4, day)) == (day <= 2), "April interval " + day);
        check(!OriginalMaterialNameRules.aprilFools(LocalDate.of(2026, 3, 31)), "March boundary");

        var impostor = new GTMaterial(0, "Wheat", "Example", 0);
        equal("Example Dust", OriginalMaterialNameRules.name("dust", impostor, LOCAL, false), "name alone cannot select special material");
        equal("Unidentified Ore", OriginalMaterialNameRules.name("ore", impostor, LOCAL, false), "unidentified ID interval");
        equal("Schrödingers Ore", OriginalMaterialNameRules.name("ore", impostor, LOCAL, true), "April unidentified ore");
        var metal = new GTMaterial(830, "Example", "Example", 0).put(MaterialProperty.SMITHABLE).setStats(274, 1000, 1);
        equal("Native Example Ore", OriginalMaterialNameRules.name("ore", metal, LOCAL, false), "upper elemental ID included");
        metal.setStats(273, 1000, 1);
        equal("Example Ore", OriginalMaterialNameRules.name("ore", metal, LOCAL, false), "melting boundary excluded");
        metal.setStats(274, 1000, 1).setCrushing(GTMaterialRegistry.get("Steel"), GTValues.U);
        equal("Example Ore", OriginalMaterialNameRules.name("ore", metal, LOCAL, false), "different crushed material excluded");
        for (int id : new int[]{831, 840}) {
            var other = new GTMaterial(id, "Other", "Other", 0).put(MaterialProperty.SMITHABLE);
            equal("Other Ore", OriginalMaterialNameRules.name("ore", other, LOCAL, false), "outside elemental ID " + id);
        }
        var wood = new GTMaterial(-1, "ExampleWood", "Example", 0).put(MaterialProperty.WOOD);
        equal("Example Pulp", OriginalMaterialNameRules.name("dust", wood, LOCAL, false), "generic wood flag");
        var stone = new GTMaterial(-1, "ExampleStone", "Bedrock", 0).put(MaterialProperty.STONE);
        equal("Bed Rock", OriginalMaterialNameRules.name("rockGt", stone, LOCAL, false), "rock suffix spacing");
        return assertions;
    }

    private static void equal(String expected, String actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": " + actual + " != " + expected);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
