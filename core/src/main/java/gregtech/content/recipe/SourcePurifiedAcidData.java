package gregtech.content.recipe;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

import java.util.List;

/**
 * Finite purified-acid families from GT6 3703e40308c8c030763fd6297dea8b210d2a77b1,
 * Loader_Recipes_Ores 108-159. Canonical identities come from the pinned MT declarations;
 * these descriptors are not per-material recipe emitters or native ownership proof.
 * Phase evidence: MT 1007/1020-1028/1047/1164/1169-1177/1188,
 * OreDictMaterial 313/1285/1294-1303, Loader_Fluids 657-662 and FL 1071-1080/1121-1125.
 */
public final class SourcePurifiedAcidData {
    private SourcePurifiedAcidData() {}

    public enum State {
        LIQUID,
        GAS
    }
    /** All 18 bindings are 1000 mB/U. Source ambient fluid() calls resolve liquid at 293 K;
     * HF/HCl/H/NO/Cl calls explicitly request gas. The native field/path is a binding assertion,
     * not permission to resolve an arbitrary same-name external registry entry. */
    public enum Phase {
        AQUAREGIA(id(9827, "AquaRegia"), State.LIQUID, "GenLiquid_AquaRegia", "gt6ce:aquaregia"),
        BLUEVITRIOL(id(8404, "BlueVitriol"), State.LIQUID, "GenLiquid_BlueVitriol", "gt6ce:bluevitriol"),
        CHLOROPLATINICACID(
                id(8401, "ChloroplatinicAcid"),
                State.LIQUID,
                "GenLiquid_ChloroplatinicAcid",
                "gt6ce:chloroplatinicacid"),
        CHLORINE(id(170, "Chlorine"), State.GAS, "GenGas_Chlorine", "gt6ce:chlorine"),
        CYANVITRIOL(id(8408, "CyanVitriol"), State.LIQUID, "GenLiquid_CyanVitriol", "gt6ce:cyanvitriol"),
        GRAYVITRIOL(id(8410, "GrayVitriol"), State.LIQUID, "GenLiquid_GrayVitriol", "gt6ce:grayvitriol"),
        GREENVITRIOL(id(8405, "GreenVitriol"), State.LIQUID, "GenLiquid_GreenVitriol", "gt6ce:greenvitriol"),
        HYDROGEN(id(10, "Hydrogen"), State.GAS, "Hydrogen", "gt6ce:hydrogen"),
        SULFURICACID(id(9824, "SulfuricAcid"), State.LIQUID, "GenLiquid_SulfuricAcid", "gt6ce:sulfuricacid"),
        HYDROCHLORICACID(id(9826, "HydrochloricAcid"), State.GAS, "GenGas_HydrochloricAcid", "gt6ce:hydrochloricacid"),
        HYDROGENFLUORIDE(id(9829, "HydrogenFluoride"), State.GAS, "GenGas_HydrogenFluoride", "gt6ce:hydrogenfluoride"),
        MARTIANVITRIOL(id(8411, "MartianVitriol"), State.LIQUID, "GenLiquid_MartianVitriol", "gt6ce:martianvitriol"),
        NITROGENMONOXIDE(id(9837, "NitrogenMonoxide"), State.GAS, "GenGas_NitrogenMonoxide", "gt6ce:nitrogenmonoxide"),
        PINKVITRIOL(id(8407, "PinkVitriol"), State.LIQUID, "GenLiquid_PinkVitriol", "gt6ce:pinkvitriol"),
        REDVITRIOL(id(8406, "RedVitriol"), State.LIQUID, "GenLiquid_RedVitriol", "gt6ce:redvitriol"),
        VITRIOLOFCLAY(id(8412, "VitriolOfClay"), State.LIQUID, "GenLiquid_VitriolOfClay", "gt6ce:vitriolofclay"),
        WATER(id(9800, "Water"), State.LIQUID, "Water", "minecraft:water"),
        WHITEVITRIOL(id(8409, "WhiteVitriol"), State.LIQUID, "GenLiquid_WhiteVitriol", "gt6ce:whitevitriol");
        private final Identity material;
        private final State state;
        private final String nativeField, registryId;

        Phase(Identity material, State state, String nativeField, String registryId) {
            this.material = material;
            this.state = state;
            this.nativeField = nativeField;
            this.registryId = registryId;
        }

        public Identity material() {
            return material;
        }

        public State state() {
            return state;
        }

        public String nativeField() {
            return nativeField;
        }

        public String registryId() {
            return registryId;
        }

        public long millibucketsPerUnit() {
            return 1000;
        }

        public long materialUnit() {
            return U;
        }
    }

    public record Fluid(Phase phase, long millibuckets) {}

    public enum Tail {
        SELF,
        PLATINUM_GROUP_SLUDGE,
        GRAPHITE
    }

    public record Family(
            int sourceLine, List<Identity> members, Fluid fluidInput, List<Fluid> fluidOutputs, Tail tail) {
        public Family {
            members = List.copyOf(members);
            fluidOutputs = List.copyOf(fluidOutputs);
        }
    }
    /** Audited default regression observations ONLY, not a live selector permission whitelist. */
    public record ManganeseDefault(Identity material, List<Identity> byproducts, int sourceLine) {
        public ManganeseDefault {
            byproducts = List.copyOf(byproducts);
        }
    }

    public static final Identity MNO2 = id(9126, "Pyrolusite"),
            MNCL2 = id(8031, "ManganeseChloride"),
            PGS = id(8103, "PlatinumGroupSludge"),
            GRAPHITE = id(9174, "Graphite");
    private static final List<Family> FAMILIES = List.of(
            new Family(
                    108,
                    List.of(
                            id(120, "Magnesium"),
                            id(9127, "Pyrope"),
                            id(8311, "Peridot"),
                            id(8016, "MagnesiumCarbonate")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.PINKVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    112,
                    List.of(
                            id(280, "Nickel"),
                            id(780, "Platinum"),
                            id(9116, "Cooperite"),
                            id(9118, "Garnierite"),
                            id(9145, "Pentlandite")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.CYANVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    116,
                    List.of(
                            id(250, "Manganese"),
                            id(9129, "Spessartine"),
                            id(9126, "Pyrolusite"),
                            id(9195, "Huebnerite"),
                            id(9133, "Tungstate"),
                            id(9128, "Scheelite")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.GRAYVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    120,
                    List.of(id(8750, "Desh"), id(8755, "Adamantite"), id(8784, "Adamantine")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.MARTIANVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    124,
                    List.of(
                            id(9104, "Hematite"),
                            id(240, "Chromium"),
                            id(9125, "Pyrite"),
                            id(9102, "Andradite"),
                            id(8642, "PigIron"),
                            id(8630, "Steel"),
                            id(8648, "DarkIron"),
                            id(8641, "DeepIron"),
                            id(8670, "ShadowIron"),
                            id(8649, "MeteoricIron"),
                            id(9120, "Ilmenite"),
                            id(9217, "Wolframite"),
                            id(9113, "Chromite"),
                            id(9122, "Magnetite"),
                            id(9003, "BasalticMineralSand"),
                            id(9004, "GraniticMineralSand")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.GREENVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    128,
                    List.of(
                            id(290, "Copper"),
                            id(790, "Gold"),
                            id(8340, "Nikolite"),
                            id(8420, "Azurite"),
                            id(9156, "Malachite"),
                            id(9111, "Chalcopyrite"),
                            id(9132, "Tetrahedrite"),
                            id(9213, "Kesterite"),
                            id(9214, "Stannite")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.BLUEVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    132,
                    List.of(id(270, "Cobalt"), id(278, "Cobalt60"), id(9115, "Cobaltite")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.REDVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    136,
                    List.of(
                            id(300, "Zinc"),
                            id(500, "Tin"),
                            id(510, "Antimony"),
                            id(9130, "Sphalerite"),
                            id(9211, "Smithsonite")),
                    f(Phase.SULFURICACID, 3500),
                    List.of(f(Phase.WHITEVITRIOL, 3000), f(Phase.HYDROGEN, 1000)),
                    Tail.SELF),
            new Family(
                    140,
                    List.of(id(9101, "Almandine"), id(9105, "Bauxite")),
                    f(Phase.SULFURICACID, 10500),
                    List.of(f(Phase.VITRIOLOFCLAY, 8500), f(Phase.HYDROGEN, 3000)),
                    Tail.SELF),
            new Family(
                    144,
                    List.of(
                            id(440, "Ruthenium"),
                            id(450, "Rhodium"),
                            id(460, "Palladium"),
                            id(760, "OsmiumElemental"),
                            id(770, "Iridium"),
                            id(780, "Platinum"),
                            id(280, "Nickel"),
                            id(8678, "Mithril"),
                            id(8649, "MeteoricIron"),
                            id(9116, "Cooperite"),
                            id(9212, "Sperrylite")),
                    f(Phase.AQUAREGIA, 9750),
                    List.of(f(Phase.CHLOROPLATINICACID, 4500), f(Phase.NITROGENMONOXIDE, 1500), f(Phase.WATER, 4125)),
                    Tail.PLATINUM_GROUP_SLUDGE),
            new Family(
                    148,
                    List.of(
                            id(230, "Vanadium"),
                            id(8234, "VanadiumPentoxide"),
                            id(8461, "NiobiumPentoxide"),
                            id(8462, "TantalumPentoxide"),
                            id(9143, "VanadiumMagnetite"),
                            id(9246, "Columbite"),
                            id(9148, "Tantalite"),
                            id(9247, "Coltan")),
                    f(Phase.HYDROGENFLUORIDE, 500),
                    List.of(),
                    Tail.SELF),
            new Family(
                    152,
                    List.of(
                            id(9174, "Graphite"),
                            id(8334, "Coal"),
                            id(8362, "Anthracite"),
                            id(8337, "Lignite"),
                            id(8363, "Prismane"),
                            id(8364, "Lonsdaleite")),
                    f(Phase.HYDROGENFLUORIDE, 500),
                    List.of(),
                    Tail.GRAPHITE));
    // MT .ores declarations supply these complete ordered lists. The original dynamic loop
    // traverses MATERIAL_ARRAY in ID order (OreDictMaterial 326), after all fixed families.
    private static final List<ManganeseDefault> MANGANESE_DEFAULTS = List.of(
            new ManganeseDefault(
                    id(250, "Manganese"),
                    List.of(id(9126, "Pyrolusite"), id(9104, "Hematite"), id(9113, "Chromite")),
                    3062),
            new ManganeseDefault(
                    id(8016, "MagnesiumCarbonate"), List.of(id(9115, "Cobaltite"), id(9126, "Pyrolusite")), 3251),
            new ManganeseDefault(
                    id(9104, "Hematite"),
                    List.of(
                            id(9120, "Ilmenite"),
                            id(9004, "GraniticMineralSand"),
                            id(9126, "Pyrolusite"),
                            id(8455, "ClayRed")),
                    3033),
            new ManganeseDefault(
                    id(9113, "Chromite"),
                    List.of(
                            id(9126, "Pyrolusite"),
                            id(9104, "Hematite"),
                            id(8016, "MagnesiumCarbonate"),
                            id(9210, "Bromargyrite")),
                    3060),
            new ManganeseDefault(
                    id(9120, "Ilmenite"),
                    List.of(
                            id(9192, "Rutile"),
                            id(9104, "Hematite"),
                            id(8016, "MagnesiumCarbonate"),
                            id(9126, "Pyrolusite")),
                    3031),
            new ManganeseDefault(
                    id(9128, "Scheelite"),
                    List.of(id(9126, "Pyrolusite"), id(9123, "Molybdenite"), id(9107, "Calcite")),
                    3181),
            new ManganeseDefault(
                    id(9133, "Tungstate"),
                    List.of(id(9126, "Pyrolusite"), id(470, "Silver"), id(8029, "LithiumChloride")),
                    3180),
            new ManganeseDefault(
                    id(9148, "Tantalite"),
                    List.of(id(9246, "Columbite"), id(9247, "Coltan"), id(9126, "Pyrolusite"), id(9120, "Ilmenite")),
                    3065),
            new ManganeseDefault(
                    id(9164, "Wollastonite"),
                    List.of(id(9104, "Hematite"), id(8016, "MagnesiumCarbonate"), id(9126, "Pyrolusite")),
                    3153),
            new ManganeseDefault(
                    id(9210, "Bromargyrite"),
                    List.of(id(9126, "Pyrolusite"), id(470, "Silver"), id(9113, "Chromite"), id(9211, "Smithsonite")),
                    3061),
            new ManganeseDefault(
                    id(9246, "Columbite"),
                    List.of(id(9148, "Tantalite"), id(9247, "Coltan"), id(9126, "Pyrolusite"), id(9120, "Ilmenite")),
                    3064),
            new ManganeseDefault(
                    id(9247, "Coltan"),
                    List.of(id(9246, "Columbite"), id(9148, "Tantalite"), id(9126, "Pyrolusite"), id(9120, "Ilmenite")),
                    3066));

    public static List<Family> families() {
        return FAMILIES;
    }

    public static List<ManganeseDefault> manganeseDefaults() {
        return MANGANESE_DEFAULTS;
    }

    public static Family family(int sourceLine) {
        return FAMILIES.stream()
                .filter(f -> f.sourceLine() == sourceLine)
                .findFirst()
                .orElse(null);
    }

    public static ManganeseDefault manganeseDefault(int id, String name) {
        return MANGANESE_DEFAULTS.stream()
                .filter(f -> f.material().matches(id, name))
                .findFirst()
                .orElse(null);
    }
    /** Diagnostic/default-regression comparison only. False must not block a proven live core
     * direct MnO2 membership; the source selector reads live byproducts rather than this table. */
    public static boolean matchesManganeseDefault(int id, String name, List<Identity> liveByproducts) {
        var d = manganeseDefault(id, name);
        return d != null && d.byproducts().equals(liveByproducts);
    }

    public static boolean reviewedIdentity(Identity material) {
        if (material == null) return false;
        return FAMILIES.stream().anyMatch(f -> f.members().contains(material))
                || MANGANESE_DEFAULTS.stream().anyMatch(d -> d.material().equals(material));
    }

    private static Identity id(int id, String name) {
        return new Identity(id, name);
    }

    private static Fluid f(Phase p, long n) {
        return new Fluid(p, n);
    }
}
