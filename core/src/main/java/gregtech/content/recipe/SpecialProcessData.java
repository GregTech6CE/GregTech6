package gregtech.content.recipe;

import java.util.List;

import static gregtech.content.material.MaterialFactKeys.Identity;

/**
 * Finite purified-acid families from GT6 3703e40308c8c030763fd6297dea8b210d2a77b1,
 * Loader_Recipes_Ores 108-159. Canonical identities come from the pinned MT declarations;
 * these descriptors are not per-material recipe emitters or native ownership proof.
 * Phase evidence: MT 1007/1020-1028/1047/1164/1169-1177/1188,
 * OreDictMaterial 313/1285/1294-1303, Loader_Fluids 657-662 and FL 1071-1080/1121-1125.
 */
public final class SpecialProcessData {
    private SpecialProcessData() {}

    public enum State {
        LIQUID,
        GAS
    }
    /** The phase selects a recipe material; binding and unit facts live in the material catalog. */
    public enum Phase {
        AQUAREGIA(id(9827, "AquaRegia"), State.LIQUID),
        BLUEVITRIOL(id(8404, "BlueVitriol"), State.LIQUID),
        CHLOROPLATINICACID(id(8401, "ChloroplatinicAcid"), State.LIQUID),
        CHLORINE(id(170, "Chlorine"), State.GAS),
        CYANVITRIOL(id(8408, "CyanVitriol"), State.LIQUID),
        GRAYVITRIOL(id(8410, "GrayVitriol"), State.LIQUID),
        GREENVITRIOL(id(8405, "GreenVitriol"), State.LIQUID),
        HYDROGEN(id(10, "Hydrogen"), State.GAS),
        SULFURICACID(id(9824, "SulfuricAcid"), State.LIQUID),
        HYDROCHLORICACID(id(9826, "HydrochloricAcid"), State.GAS),
        HYDROGENFLUORIDE(id(9829, "HydrogenFluoride"), State.GAS),
        NITROGENMONOXIDE(id(9837, "NitrogenMonoxide"), State.GAS),
        PINKVITRIOL(id(8407, "PinkVitriol"), State.LIQUID),
        REDVITRIOL(id(8406, "RedVitriol"), State.LIQUID),
        VITRIOLOFCLAY(id(8412, "VitriolOfClay"), State.LIQUID),
        WATER(id(9800, "Water"), State.LIQUID),
        WHITEVITRIOL(id(8409, "WhiteVitriol"), State.LIQUID);
        private final Identity material;
        private final State state;

        Phase(Identity material, State state) {
            this.material = material;
            this.state = state;
        }

        public Identity material() {
            return material;
        }

        public State state() {
            return state;
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
                    124,
                    List.of(
                            id(9104, "Hematite"),
                            id(240, "Chromium"),
                            id(9125, "Pyrite"),
                            id(9102, "Andradite"),
                            id(8630, "Steel"),
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
                    List.of(id(9174, "Graphite"), id(8334, "Coal"), id(8337, "Lignite")),
                    f(Phase.HYDROGENFLUORIDE, 500),
                    List.of(),
                    Tail.GRAPHITE));
    public static List<Family> families() {
        return FAMILIES;
    }

    private static Identity id(int id, String name) {
        return new Identity(id, name);
    }

    private static Fluid f(Phase p, long n) {
        return new Fluid(p, n);
    }
}
