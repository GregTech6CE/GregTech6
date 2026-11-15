package gregtech.content.material.catalog;

import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.content.material.MaterialFactKeys;
import gregtech.engine.material.MaterialDefinition;
import gregtech.engine.material.MaterialInfoDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reviewed processing facts whose payload is bound to exact built-in material identities. */
public final class ReviewedOreMaterialFacts {
    private ReviewedOreMaterialFacts() {}

    /** Bounded baseline ORES-property proof; omission is UNKNOWN, not a negative. */
    private static final Map<Integer, String> ORE_INPUT_NO = Map.ofEntries(
            Map.entry(8418, "Bamboo"),
            Map.entry(8275, "Bark"),
            Map.entry(8620, "Brass"),
            Map.entry(8610, "Bronze"),
            Map.entry(8501, "Concrete"),
            Map.entry(8290, "Dreamwood"),
            Map.entry(8751, "Duranium"),
            Map.entry(8504, "Endstone"),
            Map.entry(8035, "FerricOxyhydroxide"),
            Map.entry(8698, "Fireleaf"),
            Map.entry(9712, "FishRaw"),
            Map.entry(8002, "Flint"),
            Map.entry(90, "Fluorine"),
            Map.entry(9175, "Graphene"),
            Map.entry(8296, "Greatwood"),
            Map.entry(9802, "Ice"),
            Map.entry(8645, "IronMagnetic"),
            Map.entry(8241, "Leather"),
            Map.entry(9189, "Limestone"),
            Map.entry(8289, "Livingwood"),
            Map.entry(8506, "Marble"),
            Map.entry(9700, "MeatRaw"),
            Map.entry(8647, "NeodymiumMagnetic"),
            Map.entry(8502, "Netherrack"),
            Map.entry(8214, "Obsidian"),
            Map.entry(8218, "Plastic"),
            Map.entry(8197, "PVC"),
            Map.entry(8104, "RedSand"),
            Map.entry(8217, "Rubber"),
            Map.entry(8297, "Silverwood"),
            Map.entry(9801, "Snow"),
            Map.entry(8673, "Steeleaf"),
            Map.entry(8651, "SteelGalvanized"),
            Map.entry(8646, "SteelMagnetic"),
            Map.entry(8196, "Teflon"),
            Map.entry(8752, "Tritanium"),
            Map.entry(8027, "TungsticAcid"),
            Map.entry(8221, "Wood"),
            Map.entry(8224, "WoodRubber"));

    private static final Set<String> CRUSHED_EXCLUSIONS = Set.of(
            "crushed",
            "crushedTiny",
            "crushedPurified",
            "crushedPurifiedTiny",
            "crushedCentrifuged",
            "crushedCentrifugedTiny");

    /** Handlers145-150 source ORES/MORTAR/self-pulver profiles. */
    private record ShredderFact(String name, MaterialFactKeys.OreShredderProfile profile) {}

    private static final Map<Integer, ShredderFact> SHREDDER = Map.ofEntries(
            Map.entry(290, new ShredderFact("Copper", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(9117, new ShredderFact("Galena", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(8346, new ShredderFact("NetherQuartz", new MaterialFactKeys.OreShredderProfile(3, true))),
            Map.entry(9111, new ShredderFact("Chalcopyrite", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(9145, new ShredderFact("Pentlandite", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(9130, new ShredderFact("Sphalerite", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(9132, new ShredderFact("Tetrahedrite", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(9125, new ShredderFact("Pyrite", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(280, new ShredderFact("Nickel", new MaterialFactKeys.OreShredderProfile(2, true))),
            Map.entry(9116, new ShredderFact("Cooperite", new MaterialFactKeys.OreShredderProfile(0, true))),
            Map.entry(9137, new ShredderFact("YellowLimonite", new MaterialFactKeys.OreShredderProfile(0, true))));

    private static final Map<Integer, String> CRUSHED_SOURCE_MATERIALS = Map.ofEntries(
            Map.entry(2220, "Adamantium"),
            Map.entry(260, "Iron"),
            Map.entry(140, "Silicon"),
            Map.entry(130, "Aluminium"),
            Map.entry(220, "Titanium"),
            Map.entry(740, "Tungsten"),
            Map.entry(90, "Fluorine"),
            Map.entry(730, "Tantalum"),
            Map.entry(410, "Niobium"));

    /** Shared MT3709/w3803 Cinnabar follow-on identity set. */
    private static final MaterialFactKeys.CinnabarFollowonRule CINNABAR_FOLLOWON =
            new MaterialFactKeys.CinnabarFollowonRule(
                    new MaterialFactKeys.Identity(9114, "Cinnabar"),
                    List.of(
                            new MaterialFactKeys.Identity(8333, "Redstone"),
                            new MaterialFactKeys.Identity(160, "Sulfur"),
                            new MaterialFactKeys.Identity(8341, "Glowstone"),
                            new MaterialFactKeys.Identity(340, "Selenium")),
                    new MaterialFactKeys.Identity(8102, "SluiceSand"));

    public static MaterialDefinition.Declaration apply(MaterialDefinition.Declaration declaration) {
        var info = declaration.info();
        var rules = new LinkedHashMap<>(info.processes().rules());
        String name = declaration.name();
        if (name.equals(ORE_INPUT_NO.get(declaration.id()))) {
            rules.put(
                    MaterialFactKeys.SOURCE_ORE_INPUT,
                    List.of(MaterialProcessRule.anyForm(MaterialFactKeys.Decision.NO)));
        }
        var shredder = SHREDDER.get(declaration.id());
        if (shredder != null && shredder.name().equals(name)) {
            rules.put(MaterialFactKeys.SOURCE_ORE_SHREDDER, List.of(MaterialProcessRule.anyForm(shredder.profile())));
        }
        if (name.equals(CRUSHED_SOURCE_MATERIALS.get(declaration.id()))) {
            rules.put(
                    MaterialFactKeys.SOURCE_CRUSHED_EXCLUSIONS,
                    List.of(MaterialProcessRule.anyForm(new MaterialFactKeys.PrefixExclusions(CRUSHED_EXCLUSIONS))));
        }
        if (declaration.id() == 9114 && name.equals("Cinnabar")) {
            rules.put(
                    MaterialFactKeys.SOURCE_CINNABAR_FOLLOWON, List.of(MaterialProcessRule.anyForm(CINNABAR_FOLLOWON)));
        }
        var generatedFluid = generatedFluid(declaration.id(), name);
        if (generatedFluid != null) {
            rules.put(
                    MaterialFactKeys.SOURCE_GENERATED_FLUID,
                    List.of(MaterialProcessRule.anyForm(
                            new MaterialFactKeys.GeneratedFluidRules(Set.of(generatedFluid)))));
        }
        if (declaration.id() == 8745 && name.equals("Netherite")
                || declaration.id() == 1740 && name.equals("Naquadah")) {
            rules.put(
                    MaterialFactKeys.SOURCE_ADDITIONAL_WIRE_FAMILY, List.of(MaterialProcessRule.anyForm(Boolean.TRUE)));
        }
        if (declaration.id() == 9802 && name.equals("Ice")) {
            rules.put(
                    MaterialFactKeys.COMPRESSOR_DUST_TO_GEM_PLATE, List.of(MaterialProcessRule.anyForm(Boolean.FALSE)));
        }
        if (rules.equals(info.processes().rules())) return declaration;
        return new MaterialDefinition.Declaration(
                declaration.material(),
                new MaterialInfoDefinition(
                        info.tooling(),
                        info.composition(),
                        info.relations(),
                        new MaterialInfoDefinition.Processes(rules)));
    }

    private static MaterialFactKeys.GeneratedFluidRule generatedFluid(int id, String name) {
        return switch (id) {
            case 800 ->
                name.equals("Mercury")
                        ? new MaterialFactKeys.GeneratedFluidRule("GenLiquid_Mercury", "mercury", 300, 0F, false)
                        : null;
            case 220 ->
                name.equals("Titanium")
                        ? new MaterialFactKeys.GeneratedFluidRule(
                                "GenMolten_Titanium", "molten.titanium", 1941, 4.54F, true)
                        : null;
            case 260 ->
                name.equals("Iron")
                        ? new MaterialFactKeys.GeneratedFluidRule("GenMolten_Iron", "molten.iron", 1811, 7.874F, true)
                        : null;
            default -> null;
        };
    }
}
