package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.stone.StoneType;
import gregtech.content.material.generated.CompoundMaterials;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared bounded default classifications for Lathe, Cutter and forming, from GT6 MT/TD/OP at 3703e403.
 * The table covers 734 retained material identities from the factory and direct tag closure.
 * COATED/LAYERED do not propagate through qual/steal or material re-registration.
 * Lens means OP.lens.canGenerateItem (forced OR LENSES), not holder existence; pinned defaults
 * have no forced lens additions. This does not mutate global workability or item generation.
 */
public final class SourceLatheMaterialProfiles {
    private SourceLatheMaterialProfiles() {}

    public record Profile(boolean coated, boolean layered, boolean easy, boolean lens, boolean smithable) {}

    private enum SourceTrait {
        COATED,
        LAYERED,
        FURNACE_OR_SOFT,
        LENS_GENERATION
    }

    private record SourceRow(int id, String name, Set<SourceTrait> traits) {
        private SourceRow(int id, String name, SourceTrait... traits) {
            this(id, name, Set.of(traits));
        }

        private Profile profile() {
            return new Profile(
                    traits.contains(SourceTrait.COATED),
                    traits.contains(SourceTrait.LAYERED),
                    traits.contains(SourceTrait.FURNACE_OR_SOFT),
                    traits.contains(SourceTrait.LENS_GENERATION),
                    SOURCE_SMITHABLE.contains(id));
        }
    }

    private record Captured(
            int id,
            String name,
            Set<MaterialProperty> eligibility,
            Set<MaterialProperty> cutterEligibility,
            Set<MaterialProperty> formingEligibility,
            Set<MaterialProperty> rollingWireEligibility,
            Profile profile) {}

    private static final Map<GTMaterial, Captured> DEFAULTS = new IdentityHashMap<>();

    private record StoneTransition(Set<MaterialProperty> cutterEligibility) {}

    private static final Map<GTMaterial, StoneTransition> OFFICIAL_STONE_TRANSITIONS = new IdentityHashMap<>();
    private static boolean captured;
    // Native flags consumed by the eleven prefixes and admission. Quality is intentionally live.
    private static final Set<MaterialProperty> ELIGIBILITY = EnumSet.of(
            MaterialProperty.METAL,
            MaterialProperty.INGOT,
            MaterialProperty.GEM,
            MaterialProperty.ALLOY,
            MaterialProperty.WOOD,
            MaterialProperty.STONE,
            MaterialProperty.BOULE,
            MaterialProperty.GENERATE_INGOT,
            MaterialProperty.GENERATE_NUGGET,
            MaterialProperty.GENERATE_PLATE,
            MaterialProperty.GENERATE_STICKS,
            MaterialProperty.GENERATE_PARTS,
            MaterialProperty.GENERATE_LENS,
            MaterialProperty.ANTIMATTER,
            MaterialProperty.HIDDEN);

    // Separate Cutter snapshot: adding multi-form flags must not alter Lathe opt-out semantics.
    private static final Set<MaterialProperty> CUTTER_ELIGIBILITY = EnumSet.of(
            MaterialProperty.METAL,
            MaterialProperty.INGOT,
            MaterialProperty.GEM,
            MaterialProperty.ALLOY,
            MaterialProperty.WOOD,
            MaterialProperty.STONE,
            MaterialProperty.BOULE,
            MaterialProperty.GENERATE_INGOT,
            MaterialProperty.GENERATE_PLATE,
            MaterialProperty.GENERATE_STICKS,
            MaterialProperty.GENERATE_MULTIINGOT,
            MaterialProperty.GENERATE_MULTIPLATE,
            MaterialProperty.ANTIMATTER,
            MaterialProperty.HIDDEN);

    // Independent six-rule forming context; changing it must not change Lathe/Cutter opt-outs.
    private static final Set<MaterialProperty> FORMING_ELIGIBILITY = EnumSet.of(
            MaterialProperty.INGOT,
            MaterialProperty.METAL,
            MaterialProperty.GENERATE_INGOT,
            MaterialProperty.ALLOY,
            MaterialProperty.GEM,
            MaterialProperty.WOOD,
            MaterialProperty.STONE,
            MaterialProperty.GENERATE_STICKS,
            MaterialProperty.GENERATE_PARTS,
            MaterialProperty.GENERATE_FOIL,
            MaterialProperty.GENERATE_WIRE,
            MaterialProperty.GENERATE_RAIL,
            MaterialProperty.SMITHABLE,
            MaterialProperty.ANTIMATTER,
            MaterialProperty.HIDDEN);

    // Rolling/fine/physical wire have an independent admission and holder context.
    private static final Set<MaterialProperty> ROLLING_WIRE_ELIGIBILITY = EnumSet.of(
            MaterialProperty.INGOT,
            MaterialProperty.METAL,
            MaterialProperty.GENERATE_INGOT,
            MaterialProperty.ALLOY,
            MaterialProperty.GEM,
            MaterialProperty.WOOD,
            MaterialProperty.STONE,
            MaterialProperty.GENERATE_NUGGET,
            MaterialProperty.GENERATE_STICKS,
            MaterialProperty.GENERATE_WIRE,
            MaterialProperty.GENERATE_MULTIINGOT,
            MaterialProperty.GENERATE_MULTIPLATE,
            MaterialProperty.GENERATE_DENSEPLATE,
            MaterialProperty.SMITHABLE,
            MaterialProperty.ANTIMATTER,
            MaterialProperty.HIDDEN);

    private static Set<MaterialProperty> rollingWireEligibility(GTMaterial material) {
        var flags = EnumSet.noneOf(MaterialProperty.class);
        for (var flag : ROLLING_WIRE_ELIGIBILITY) if (material.has(flag)) flags.add(flag);
        return Set.copyOf(flags);
    }

    private static Set<MaterialProperty> formingEligibility(GTMaterial material) {
        var flags = EnumSet.noneOf(MaterialProperty.class);
        for (var flag : FORMING_ELIGIBILITY) if (material.has(flag)) flags.add(flag);
        return Set.copyOf(flags);
    }

    private static Set<MaterialProperty> cutterEligibility(GTMaterial material) {
        var flags = EnumSet.noneOf(MaterialProperty.class);
        for (var flag : CUTTER_ELIGIBILITY) if (material.has(flag)) flags.add(flag);
        return Set.copyOf(flags);
    }

    private static Set<MaterialProperty> eligibility(GTMaterial material) {
        var flags = EnumSet.noneOf(MaterialProperty.class);
        for (var flag : ELIGIBILITY) if (material.has(flag)) flags.add(flag);
        return Set.copyOf(flags);
    }

    /** Built-in link hook, after native corrections/groups and before addon link callbacks. */
    static void captureDefaults() {
        if (captured) throw new IllegalStateException("Lathe defaults already captured");
        for (SourceRow row : SOURCE_ROWS) {
            int id = row.id();
            GTMaterial material = GTMaterialRegistry.get(row.name());
            if (material == null
                    || material.getId() != id
                    || !material.getName().equals(row.name())
                    || material.resolve() != material
                    || AddonMaterials.owns(material)
                    || id > 0 && GTMaterialRegistry.get(id) != material) continue;
            DEFAULTS.put(
                    material,
                    new Captured(
                            id,
                            row.name(),
                            eligibility(material),
                            cutterEligibility(material),
                            formingEligibility(material),
                            rollingWireEligibility(material),
                            row.profile()));
        }
        captured = true;
    }

    /** Null means unknown or an observable addon override: retain the legacy recipe behavior. */
    public static Profile find(GTMaterial material) {
        Captured value = DEFAULTS.get(material);
        if (value == null
                || material.resolve() != material
                || AddonMaterials.owns(material)
                || material.getId() != value.id()
                || !material.getName().equals(value.name())
                || GTMaterialRegistry.get(value.name()) != material
                || value.id() > 0 && GTMaterialRegistry.get(value.id()) != material
                || !value.eligibility().equals(eligibility(material))) return null;
        return value.profile();
    }

    /** Same bounded source facts, with independently observed Cutter prefix eligibility. */
    public static Profile findForCutter(GTMaterial material) {
        Captured value = DEFAULTS.get(material);
        if (value == null
                || material.resolve() != material
                || AddonMaterials.owns(material)
                || material.getId() != value.id()
                || !material.getName().equals(value.name())
                || GTMaterialRegistry.get(value.name()) != material
                || value.id() > 0 && GTMaterialRegistry.get(value.id()) != material
                || !value.cutterEligibility().equals(cutterEligibility(material))) return null;
        return value.profile();
    }

    /** Source forming facts for unchanged defaults; observable native flag overrides opt out. */
    public static Profile findForForming(GTMaterial material) {
        Captured value = DEFAULTS.get(material);
        if (value == null
                || material.resolve() != material
                || AddonMaterials.owns(material)
                || material.getId() != value.id()
                || !material.getName().equals(value.name())
                || GTMaterialRegistry.get(value.name()) != material
                || value.id() > 0 && GTMaterialRegistry.get(value.id()) != material
                || !value.formingEligibility().equals(formingEligibility(material))) return null;
        return value.profile();
    }

    /** Independent rolling/wire context; does not adopt later official granite role transitions. */
    public static Profile findForRollingWire(GTMaterial material) {
        Captured value = DEFAULTS.get(material);
        if (value == null
                || material.resolve() != material
                || AddonMaterials.owns(material)
                || material.getId() != value.id()
                || !material.getName().equals(value.name())
                || GTMaterialRegistry.get(value.name()) != material
                || value.id() > 0 && GTMaterialRegistry.get(value.id()) != material
                || !value.rollingWireEligibility().equals(rollingWireEligibility(material))) return null;
        return value.profile();
    }

    /** Apply the existing official role and retain provenance only for the three audited granites.
     * No original snapshot is overwritten, and an already-added STONE flag is never adopted.
     */
    public static void applyOfficialStoneRole(StoneType stone) {
        if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY
                && GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.FINALIZING)
            throw new IllegalStateException("Official stone roles require a linked catalog");
        GTMaterial material = stone.material();
        boolean granite = material == generatedGranite(stone);
        Profile before =
                granite && !material.has(MaterialProperty.STONE) && !OFFICIAL_STONE_TRANSITIONS.containsKey(material)
                        ? findForCutter(material)
                        : null;
        material.put(MaterialProperty.STONE);
        if (before == null) return;
        var expected = EnumSet.noneOf(MaterialProperty.class);
        expected.addAll(DEFAULTS.get(material).cutterEligibility());
        expected.add(MaterialProperty.STONE);
        if (expected.equals(cutterEligibility(material)))
            OFFICIAL_STONE_TRANSITIONS.put(material, new StoneTransition(Set.copyOf(expected)));
    }

    private static GTMaterial generatedGranite(StoneType stone) {
        return switch (stone) {
            case GRANITE_RED -> CompoundMaterials.GraniteRed;
            case GRANITE_BLACK -> CompoundMaterials.GraniteBlack;
            case GRANITE -> CompoundMaterials.Granite;
            default -> null;
        };
    }

    /** Only the Cutter's independently source-proven pairs may consume this separate transition. */
    public static Profile findForOfficialStoneCutter(GTMaterial material) {
        StoneTransition transition = OFFICIAL_STONE_TRANSITIONS.get(material);
        Captured value = DEFAULTS.get(material);
        if (transition == null
                || value == null
                || material.resolve() != material
                || AddonMaterials.owns(material)
                || material.getId() != value.id()
                || !material.getName().equals(value.name())
                || GTMaterialRegistry.get(value.name()) != material
                || GTMaterialRegistry.get(value.id()) != material
                || !transition.cutterEligibility().equals(cutterEligibility(material))) return null;
        return value.profile();
    }

    // GT6 MT SMITHABLE closure at 3703e403. Only IDs present in SOURCE_ROWS contribute to the snapshot.
    // Direct/factory puts count; qual, steal and re-registration do not inherit tags.
    private static final Set<Integer> SOURCE_SMITHABLE = Set.of(
            30, 31, 40, 41, 42, 50, 51, 110, 120, 130, 140, 190, 200, 210, 220, 230, 240, 250, 260, 270, 278, 280, 290,
            300, 310, 320, 330, 370, 380, 390, 400, 410, 420, 430, 440, 450, 460, 470, 480, 490, 500, 510, 520, 550,
            560, 570, 580, 590, 600, 610, 620, 630, 640, 650, 660, 670, 680, 690, 700, 710, 720, 730, 740, 750, 760,
            770, 780, 790, 791, 800, 810, 820, 830, 840, 850, 870, 880, 890, 900, 910, 920, 921, 922, 930, 940, 942,
            943, 945, 946, 947, 950, 951, 952, 960, 970, 980, 990, 1000, 1010, 1020, 1030, 1040, 1050, 1060, 1070, 1080,
            1120, 1140, 1148, 1250, 1260, 1450, 1740, 1741, 1742, 1850, 1860, 1870, 1880, 2210, 2220, 8003, 8005, 8643,
            8640, 8760, 8761, 8762, 8763, 8764, 8701, 8765, 8767, 8768, 8770, 8771, 8772, 8773, 8775, 8641, 8670, 8784,
            8774, 8776, 8769, 8676, 8677, 8678, 8779, 8671, 8777, 8778, 8781, 8782, 8783, 8600, 8601, 8602, 8603, 8604,
            8605, 8606, 8620, 8621, 8622, 8610, 8611, 8612, 8613, 8614, 8615, 8630, 8631, 8632, 8633, 8634, 8653, 8635,
            8638, 8637, 8639, 8766, 8642, 8644, 8803, 8645, 8646, 8647, 8648, 8651, 8652, 8654, 8802, 8649, 8650, 8690,
            8691, 8692, 8660, 8659, 8657, 8804, 8733, 8737, 8658, 8711, 8727, 8808, 8661, 8662, 8663, 8664, 8665, 8636,
            8666, 8667, 8668, 8669, 8672, 8673, 8674, 8675, 8698, 8693, 8694, 8695, 8696, 8697, 8679, 8680, 8681, 8682,
            8683, 8685, 8686, 8687, 8688, 8689, 8700, 8707, 8702, 8703, 8704, 8705, 8706, 8708, 8709, 8729, 8710, 8713,
            8714, 8715, 8716, 8717, 8723, 8718, 8731, 8725, 8728, 8726, 8730, 8732, 8806, 8807, 8809, 8810, 8811, 8812,
            8813, 8734, 8720, 8721, 8722, 8735, 8736, 8739, 8740, 8741, 8742, 8744, 8745, 8746, 8747, 8750, 8780, 8751,
            8752, 8754, 8755, 8756, 8757, 8758, 8759, 8785, 8786, 8796, 8797, 8798, 8795, 8799, 8800, 8805, 8684, 8790,
            8793, 8794);

    private static final List<SourceRow> SOURCE_ROWS = List.of(
            new SourceRow(0, "Empty"),
            new SourceRow(1, "Photon"),
            new SourceRow(2, "Neutrino"),
            new SourceRow(3, "Neutron"),
            new SourceRow(4, "Proton"),
            new SourceRow(5, "Electron"),
            new SourceRow(10, "Hydrogen"),
            new SourceRow(11, "Deuterium"),
            new SourceRow(12, "Tritium"),
            new SourceRow(20, "Helium"),
            new SourceRow(21, "Helium3"),
            new SourceRow(30, "Lithium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(31, "Lithium6", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(40, "Beryllium"),
            new SourceRow(41, "Beryllium7"),
            new SourceRow(42, "Beryllium8"),
            new SourceRow(50, "Boron"),
            new SourceRow(51, "Boron11"),
            new SourceRow(60, "Carbon"),
            new SourceRow(61, "Carbon13"),
            new SourceRow(62, "Carbon14"),
            new SourceRow(70, "Nitrogen"),
            new SourceRow(80, "Oxygen"),
            new SourceRow(90, "Fluorine"),
            new SourceRow(100, "Neon"),
            new SourceRow(110, "Sodium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(120, "Magnesium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(130, "Aluminium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(140, "Silicon"),
            new SourceRow(150, "Phosphor"),
            new SourceRow(160, "Sulfur"),
            new SourceRow(170, "Chlorine"),
            new SourceRow(180, "Argon"),
            new SourceRow(190, "Potassium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(200, "Calcium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(210, "Scandium"),
            new SourceRow(220, "Titanium"),
            new SourceRow(230, "Vanadium"),
            new SourceRow(240, "Chromium"),
            new SourceRow(250, "Manganese"),
            new SourceRow(260, "Iron"),
            new SourceRow(270, "Cobalt"),
            new SourceRow(278, "Cobalt60"),
            new SourceRow(280, "Nickel"),
            new SourceRow(290, "Copper", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(300, "Zinc", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(310, "Gallium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(320, "Germanium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(330, "Arsenic", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(340, "Selenium"),
            new SourceRow(350, "Bromine"),
            new SourceRow(360, "Krypton"),
            new SourceRow(370, "Rubidium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(380, "Strontium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(390, "Yttrium"),
            new SourceRow(400, "Zirconium", SourceTrait.LENS_GENERATION),
            new SourceRow(410, "Niobium"),
            new SourceRow(420, "Molybdenum"),
            new SourceRow(430, "Technetium"),
            new SourceRow(440, "Ruthenium"),
            new SourceRow(450, "Rhodium"),
            new SourceRow(460, "Palladium"),
            new SourceRow(470, "Silver", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(480, "Cadmium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(490, "Indium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(500, "Tin", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(510, "Antimony", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(520, "Tellurium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(530, "Iodine"),
            new SourceRow(540, "Xenon"),
            new SourceRow(550, "Caesium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(560, "Barium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(570, "Lanthanium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(580, "Cerium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(590, "Praseodymium"),
            new SourceRow(600, "Neodymium"),
            new SourceRow(610, "Promethium"),
            new SourceRow(620, "Samarium"),
            new SourceRow(630, "Europium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(640, "Gadolinium"),
            new SourceRow(650, "Terbium"),
            new SourceRow(660, "Dysprosium"),
            new SourceRow(670, "Holmium"),
            new SourceRow(680, "Erbium"),
            new SourceRow(690, "Thulium"),
            new SourceRow(700, "Ytterbium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(710, "Lutetium"),
            new SourceRow(720, "Hafnium"),
            new SourceRow(730, "Tantalum"),
            new SourceRow(740, "Tungsten"),
            new SourceRow(750, "Rhenium"),
            new SourceRow(760, "OsmiumElemental"),
            new SourceRow(770, "Iridium"),
            new SourceRow(780, "Platinum"),
            new SourceRow(790, "Gold", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(791, "Gold198", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(800, "Mercury", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(810, "Thallium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(820, "Lead", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(830, "Bismuth", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(840, "Polonium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(850, "Astatine", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(860, "Radon"),
            new SourceRow(870, "Francium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(880, "Radium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(890, "Actinium"),
            new SourceRow(900, "Thorium"),
            new SourceRow(910, "Protactinium"),
            new SourceRow(920, "Uranium"),
            new SourceRow(921, "Uranium235"),
            new SourceRow(922, "Uranium233"),
            new SourceRow(930, "Neptunium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(940, "Plutonium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(942, "Plutonium240", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(943, "Plutonium241", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(945, "Plutonium243", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(946, "Plutonium238", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(947, "Plutonium239", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(950, "Americium"),
            new SourceRow(951, "Americium241"),
            new SourceRow(952, "Americium242"),
            new SourceRow(960, "Curium"),
            new SourceRow(970, "Berkelium"),
            new SourceRow(980, "Californium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(990, "Einsteinium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1000, "Fermium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1010, "Mendelevium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1020, "Nobelium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1030, "Lawrencium"),
            new SourceRow(1040, "Rutherfordium"),
            new SourceRow(1050, "Dubnium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1060, "Seaborgium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1070, "Bohrium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1080, "Hassium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1090, "Meitnerium"),
            new SourceRow(1100, "Darmstadtium"),
            new SourceRow(1110, "Roentgenium"),
            new SourceRow(1120, "Copernicium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1130, "Nihonium"),
            new SourceRow(1140, "Flerovium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1148, "Flerovium298", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(1150, "Moscovium"),
            new SourceRow(1160, "Livermorium"),
            new SourceRow(1170, "Farnsium"),
            new SourceRow(1180, "Oganesson"),
            new SourceRow(1250, "TritaniumElemental"),
            new SourceRow(1260, "Trinium"),
            new SourceRow(1450, "DuraniumElemental"),
            new SourceRow(1520, "Vibranium", SourceTrait.LENS_GENERATION),
            new SourceRow(1740, "Naquadah"),
            new SourceRow(1741, "NaquadahEnriched"),
            new SourceRow(1742, "Naquadria"),
            new SourceRow(2220, "Adamantium"),
            new SourceRow(3720, "Gravitonium"),
            new SourceRow(4000, "Magic"),
            new SourceRow(8000, "SiliconDioxide", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8001, "Glass", SourceTrait.FURNACE_OR_SOFT, SourceTrait.LENS_GENERATION),
            new SourceRow(8002, "Flint"),
            new SourceRow(8003, "Carborundum"),
            new SourceRow(8006, "Datolite"),
            new SourceRow(8007, "HydrogenBorate"),
            new SourceRow(8008, "Alumina"),
            new SourceRow(8009, "Cryolite"),
            new SourceRow(8010, "AluminiumFluoride"),
            new SourceRow(8011, "HexafluorosilicicAcid"),
            new SourceRow(8012, "SodiumAluminate"),
            new SourceRow(8013, "SodiumCarbonate"),
            new SourceRow(8014, "AluminiumHydroxide"),
            new SourceRow(8015, "PotassiumHydroxide"),
            new SourceRow(8016, "MagnesiumCarbonate"),
            new SourceRow(8017, "FerricChloride"),
            new SourceRow(8018, "MagnesiumChloride"),
            new SourceRow(8019, "SodiumNitrate"),
            new SourceRow(8020, "PotassiumCarbonate"),
            new SourceRow(8021, "PotassiumSulfite"),
            new SourceRow(8022, "PotassiumPersulfate"),
            new SourceRow(8023, "PotassiumAluminate"),
            new SourceRow(8024, "HydrosulfuricAcid"),
            new SourceRow(8025, "Ammonia"),
            new SourceRow(8026, "TungstenTrioxide"),
            new SourceRow(8027, "TungsticAcid"),
            new SourceRow(8028, "CalciumChloride"),
            new SourceRow(8029, "LithiumChloride"),
            new SourceRow(8030, "FerrousChloride"),
            new SourceRow(8031, "ManganeseChloride"),
            new SourceRow(8032, "LithiumHydroxide"),
            new SourceRow(8033, "LithiumChlorate"),
            new SourceRow(8034, "LithiumPerchlorate"),
            new SourceRow(8035, "FerricOxyhydroxide"),
            new SourceRow(8036, "PotassiumFluoride"),
            new SourceRow(8037, "SodiumFluoride"),
            new SourceRow(8038, "PotassiumHeptafluorotantalate"),
            new SourceRow(8039, "SodiumHydrogencarbonate"),
            new SourceRow(8100, "Sand", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8101, "Soulsand"),
            new SourceRow(8102, "SluiceSand"),
            new SourceRow(8103, "PlatinumGroupSludge"),
            new SourceRow(8104, "RedSand", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8196, "Teflon", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8197, "PVC", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8200, "Ashes"),
            new SourceRow(8201, "DarkAshes"),
            new SourceRow(8203, "Sylvite"),
            new SourceRow(8204, "Salt"),
            new SourceRow(8205, "PotassiumNitrate"),
            new SourceRow(8206, "Niter"),
            new SourceRow(8207, "Phosphate"),
            new SourceRow(8208, "Phosphorus"),
            new SourceRow(8209, "Apatite"),
            new SourceRow(8210, "Blizz"),
            new SourceRow(8211, "Blaze"),
            new SourceRow(8214, "Obsidian"),
            new SourceRow(8215, "Clay"),
            new SourceRow(8216, "Paper"),
            new SourceRow(8217, "Rubber", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8218, "Plastic", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8219, "Bone"),
            new SourceRow(8220, "Gunpowder"),
            new SourceRow(8221, "Wood"),
            new SourceRow(8222, "WoodTreated", SourceTrait.COATED),
            new SourceRow(8223, "LiveRoot"),
            new SourceRow(8224, "WoodRubber"),
            new SourceRow(8225, "Ceramic"),
            new SourceRow(8226, "Phosphorite"),
            new SourceRow(8229, "CobaltHexahydrate"),
            new SourceRow(8230, "SodiumBisulfate"),
            new SourceRow(8231, "SodiumPyrosulfate"),
            new SourceRow(8232, "PotassiumBisulfate"),
            new SourceRow(8233, "PotassiumPyrosulfate"),
            new SourceRow(8234, "VanadiumPentoxide"),
            new SourceRow(8235, "Wax", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8236, "WaxBee", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8238, "WaxParaffin", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8239, "WaxPlant", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8241, "Leather"),
            new SourceRow(8242, "IodineSalt"),
            new SourceRow(8247, "Basalz"),
            new SourceRow(8248, "Blitz"),
            new SourceRow(8249, "Dynamite"),
            new SourceRow(8250, "DyeBlack"),
            new SourceRow(8251, "DyeRed"),
            new SourceRow(8252, "DyeGreen"),
            new SourceRow(8253, "DyeBrown"),
            new SourceRow(8254, "DyeBlue"),
            new SourceRow(8255, "DyePurple"),
            new SourceRow(8256, "DyeCyan"),
            new SourceRow(8257, "DyeLightGray"),
            new SourceRow(8258, "DyeGray"),
            new SourceRow(8259, "DyePink"),
            new SourceRow(8260, "DyeLime"),
            new SourceRow(8261, "DyeYellow"),
            new SourceRow(8262, "DyeLightBlue"),
            new SourceRow(8263, "DyeMagenta"),
            new SourceRow(8264, "DyeOrange"),
            new SourceRow(8265, "DyeWhite"),
            new SourceRow(8266, "Asphalt", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8267, "WoodPolished", SourceTrait.COATED),
            new SourceRow(8268, "SodiumHydroxide"),
            new SourceRow(8269, "SodiumSulfite"),
            new SourceRow(8270, "SodiumSulfate"),
            new SourceRow(8271, "PotassiumSulfate"),
            new SourceRow(8272, "PotassiumSulfide"),
            new SourceRow(8273, "Porcelain"),
            new SourceRow(8274, "CalciumSulfate"),
            new SourceRow(8275, "Bark"),
            new SourceRow(8276, "ClayBrown"),
            new SourceRow(8277, "PetrifiedWood"),
            new SourceRow(8278, "ManaDiamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8289, "Livingwood"),
            new SourceRow(8290, "Dreamwood"),
            new SourceRow(8296, "Greatwood"),
            new SourceRow(8297, "Silverwood"),
            new SourceRow(8298, "EnergiumRed", SourceTrait.LENS_GENERATION),
            new SourceRow(8299, "EnergiumCyan", SourceTrait.LENS_GENERATION),
            new SourceRow(8300, "Diamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8301, "Emerald", SourceTrait.LENS_GENERATION),
            new SourceRow(8302, "Ruby", SourceTrait.LENS_GENERATION),
            new SourceRow(8303, "BalasRuby", SourceTrait.LENS_GENERATION),
            new SourceRow(8304, "Sapphire", SourceTrait.LENS_GENERATION),
            new SourceRow(8305, "GreenSapphire", SourceTrait.LENS_GENERATION),
            new SourceRow(8306, "Topaz", SourceTrait.LENS_GENERATION),
            new SourceRow(8307, "BlueTopaz", SourceTrait.LENS_GENERATION),
            new SourceRow(8308, "Tanzanite", SourceTrait.LENS_GENERATION),
            new SourceRow(8309, "RedJasper", SourceTrait.LENS_GENERATION),
            new SourceRow(8310, "Amber", SourceTrait.LENS_GENERATION),
            new SourceRow(8311, "Peridot", SourceTrait.LENS_GENERATION),
            new SourceRow(8312, "Opal", SourceTrait.LENS_GENERATION),
            new SourceRow(8313, "Amethyst", SourceTrait.LENS_GENERATION),
            new SourceRow(8314, "OrangeSapphire", SourceTrait.LENS_GENERATION),
            new SourceRow(8315, "YellowSapphire", SourceTrait.LENS_GENERATION),
            new SourceRow(8318, "EnderPearl", SourceTrait.LENS_GENERATION),
            new SourceRow(8319, "EnderEye", SourceTrait.LENS_GENERATION),
            new SourceRow(8320, "NetherStar", SourceTrait.LENS_GENERATION),
            new SourceRow(8321, "Jade", SourceTrait.LENS_GENERATION),
            new SourceRow(8322, "Craponite", SourceTrait.LENS_GENERATION),
            new SourceRow(8323, "Aquamarine", SourceTrait.LENS_GENERATION),
            new SourceRow(8324, "Morganite", SourceTrait.LENS_GENERATION),
            new SourceRow(8325, "Dioptase", SourceTrait.LENS_GENERATION),
            new SourceRow(8326, "Spinel", SourceTrait.LENS_GENERATION),
            new SourceRow(8328, "BlueSapphire", SourceTrait.LENS_GENERATION),
            new SourceRow(8330, "Lazurite"),
            new SourceRow(8331, "Sodalite"),
            new SourceRow(8332, "Lapis"),
            new SourceRow(8333, "Redstone", SourceTrait.LENS_GENERATION),
            new SourceRow(8334, "Coal"),
            new SourceRow(8335, "HydratedCoal", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8336, "Charcoal"),
            new SourceRow(8337, "Lignite"),
            new SourceRow(8338, "Monazite"),
            new SourceRow(8340, "Nikolite", SourceTrait.LENS_GENERATION),
            new SourceRow(8341, "Glowstone", SourceTrait.LENS_GENERATION),
            new SourceRow(8342, "Firestone"),
            new SourceRow(8346, "NetherQuartz"),
            new SourceRow(8347, "CertusQuartz"),
            new SourceRow(8348, "ChargedCertusQuartz"),
            new SourceRow(8349, "CoalCoke"),
            new SourceRow(8360, "Peat"),
            new SourceRow(8365, "LigniteCoke"),
            new SourceRow(8383, "PurpleSapphire", SourceTrait.LENS_GENERATION),
            new SourceRow(8384, "Heliodor", SourceTrait.LENS_GENERATION),
            new SourceRow(8385, "Goshenite", SourceTrait.LENS_GENERATION),
            new SourceRow(8386, "Bixbite", SourceTrait.LENS_GENERATION),
            new SourceRow(8387, "Maxixe", SourceTrait.LENS_GENERATION),
            new SourceRow(8389, "Fluix"),
            new SourceRow(8390, "PetroleumCoke"),
            new SourceRow(8393, "SunnyQuartz"),
            new SourceRow(8395, "RedQuartz"),
            new SourceRow(8396, "BlazeQuartz"),
            new SourceRow(8397, "SmokeyQuartz"),
            new SourceRow(8399, "ElvenQuartz"),
            new SourceRow(8400, "ChloroauricAcid"),
            new SourceRow(8401, "ChloroplatinicAcid"),
            new SourceRow(8402, "StannicChloride"),
            new SourceRow(8403, "BlackVitriol"),
            new SourceRow(8404, "BlueVitriol"),
            new SourceRow(8405, "GreenVitriol"),
            new SourceRow(8406, "RedVitriol"),
            new SourceRow(8407, "PinkVitriol"),
            new SourceRow(8408, "CyanVitriol"),
            new SourceRow(8409, "WhiteVitriol"),
            new SourceRow(8410, "GrayVitriol"),
            new SourceRow(8412, "VitriolOfClay"),
            new SourceRow(8413, "TitaniumTetrachloride"),
            new SourceRow(8415, "OnyxRed", SourceTrait.LENS_GENERATION),
            new SourceRow(8416, "OnyxBlack", SourceTrait.LENS_GENERATION),
            new SourceRow(8417, "Amazonite", SourceTrait.LENS_GENERATION),
            new SourceRow(8418, "Bamboo"),
            new SourceRow(8419, "Zircon", SourceTrait.LENS_GENERATION),
            new SourceRow(8420, "Azurite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8421, "Eudialyte"),
            new SourceRow(8423, "DiamondIndustrial", SourceTrait.LENS_GENERATION),
            new SourceRow(8424, "PinkDiamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8425, "OceanJasper", SourceTrait.LENS_GENERATION),
            new SourceRow(8426, "RainforestJasper", SourceTrait.LENS_GENERATION),
            new SourceRow(8427, "BlueJasper", SourceTrait.LENS_GENERATION),
            new SourceRow(8428, "GreenJasper", SourceTrait.LENS_GENERATION),
            new SourceRow(8429, "YellowJasper", SourceTrait.LENS_GENERATION),
            new SourceRow(8430, "TigerEye", SourceTrait.LENS_GENERATION),
            new SourceRow(8431, "CatsEye", SourceTrait.LENS_GENERATION),
            new SourceRow(8432, "DragonEye", SourceTrait.LENS_GENERATION),
            new SourceRow(8433, "HawksEye", SourceTrait.LENS_GENERATION),
            new SourceRow(8434, "BlackEye", SourceTrait.LENS_GENERATION),
            new SourceRow(8435, "TigerIron", SourceTrait.LENS_GENERATION),
            new SourceRow(8436, "RedFluorite"),
            new SourceRow(8437, "PinkFluorite"),
            new SourceRow(8438, "BlueFluorite"),
            new SourceRow(8439, "GreenFluorite"),
            new SourceRow(8440, "BlackFluorite"),
            new SourceRow(8441, "WhiteFluorite"),
            new SourceRow(8442, "YellowFluorite"),
            new SourceRow(8443, "OrangeFluorite"),
            new SourceRow(8444, "MagentaFluorite"),
            new SourceRow(8445, "MilkyQuartz"),
            new SourceRow(8446, "BrownAventurine", SourceTrait.LENS_GENERATION),
            new SourceRow(8447, "YellowAventurine", SourceTrait.LENS_GENERATION),
            new SourceRow(8448, "GreenAventurine", SourceTrait.LENS_GENERATION),
            new SourceRow(8449, "BlackAventurine", SourceTrait.LENS_GENERATION),
            new SourceRow(8450, "BlueAventurine", SourceTrait.LENS_GENERATION),
            new SourceRow(8451, "RedAventurine", SourceTrait.LENS_GENERATION),
            new SourceRow(8455, "ClayRed"),
            new SourceRow(8458, "BluePhosphorus"),
            new SourceRow(8459, "RedPhosphorus"),
            new SourceRow(8460, "WhitePhosphorus"),
            new SourceRow(8461, "NiobiumPentoxide"),
            new SourceRow(8462, "TantalumPentoxide"),
            new SourceRow(8464, "BlueDiamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8465, "GreenDiamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8467, "RedDiamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8468, "YellowDiamond", SourceTrait.LENS_GENERATION),
            new SourceRow(8470, "Carminite", SourceTrait.LENS_GENERATION),
            new SourceRow(8471, "Breeze"),
            new SourceRow(8500, "Stone", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8501, "Concrete", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8502, "Netherrack", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8503, "NetherBrick", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8504, "Endstone", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8505, "Basalt", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8506, "Marble", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8507, "GraniteRed", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8508, "GraniteBlack", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8511, "Diorite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8516, "MarsSand", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8518, "Granite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8521, "Livingrock", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8528, "SkyStone", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8599, "Bedrock"),
            new SourceRow(8600, "Electrum", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8601, "SterlingSilver", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8602, "RoseGold", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8605, "CdInAgAlloy"),
            new SourceRow(8610, "Bronze", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8611, "BlackBronze", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8612, "BismuthBronze", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8614, "ArsenicCopper", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8615, "ArsenicBronze", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8620, "Brass", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8621, "CobaltBrass", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8622, "AluminiumAlloy"),
            new SourceRow(8630, "Steel"),
            new SourceRow(8631, "BlackSteel"),
            new SourceRow(8632, "BlueSteel"),
            new SourceRow(8633, "RedSteel"),
            new SourceRow(8634, "DamascusSteel"),
            new SourceRow(8635, "Tungstensteel"),
            new SourceRow(8636, "StainlessSteel"),
            new SourceRow(8638, "TungstenCarbide"),
            new SourceRow(8640, "AnnealedCopper", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8643, "WroughtIron"),
            new SourceRow(8645, "IronMagnetic", SourceTrait.LAYERED),
            new SourceRow(8646, "SteelMagnetic", SourceTrait.LAYERED),
            new SourceRow(8647, "NeodymiumMagnetic", SourceTrait.LAYERED),
            new SourceRow(8651, "SteelGalvanized", SourceTrait.COATED),
            new SourceRow(8652, "TungstenSintered"),
            new SourceRow(8653, "VanadiumSteel"),
            new SourceRow(8658, "ElectrotineAlloy", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8659, "BlueAlloy", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8660, "RedAlloy", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8661, "Invar"),
            new SourceRow(8662, "Constantan"),
            new SourceRow(8663, "Nichrome"),
            new SourceRow(8664, "Kanthal"),
            new SourceRow(8665, "Magnalium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8666, "Ultimet"),
            new SourceRow(8667, "TinAlloy", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8668, "BatteryAlloy", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8669, "SolderingAlloy", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8672, "Ironwood", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8673, "Steeleaf", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8674, "Knightmetal"),
            new SourceRow(8675, "FierySteel"),
            new SourceRow(8679, "Thaumium"),
            new SourceRow(8682, "Osmiridium"),
            new SourceRow(8684, "Trinaquadalloy"),
            new SourceRow(8685, "ChromiumDioxide"),
            new SourceRow(8686, "VanadiumGallium"),
            new SourceRow(8687, "YttriumBariumCuprate", SourceTrait.LAYERED),
            new SourceRow(8689, "NiobiumTitanium"),
            new SourceRow(8697, "FlamascusSteel"),
            new SourceRow(8698, "Fireleaf", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8700, "AluminiumBrass", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8702, "Alumite"),
            new SourceRow(8704, "VibraniumSteel"),
            new SourceRow(8705, "VibraniumSilver"),
            new SourceRow(8706, "Vibramantium"),
            new SourceRow(8708, "Signalum", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8709, "Lumium", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(8710, "Enderium"),
            new SourceRow(8717, "Cyanite"),
            new SourceRow(8720, "Manasteel"),
            new SourceRow(8721, "Terrasteel"),
            new SourceRow(8722, "ElvenElementium"),
            new SourceRow(8726, "VibrantAlloy"),
            new SourceRow(8727, "ConductiveIron"),
            new SourceRow(8729, "EnderiumBase"),
            new SourceRow(8730, "ElectricalSteel"),
            new SourceRow(8732, "Soularium"),
            new SourceRow(8733, "RedstoneAlloy"),
            new SourceRow(8735, "GaiaSpirit"),
            new SourceRow(8737, "NikolineAlloy"),
            new SourceRow(8744, "AncientDebris"),
            new SourceRow(8745, "Netherite"),
            new SourceRow(8746, "NetherizedDiamond", SourceTrait.COATED),
            new SourceRow(8747, "Efrine"),
            new SourceRow(8751, "Duranium"),
            new SourceRow(8752, "Tritanium"),
            new SourceRow(8756, "Duralumin"),
            new SourceRow(8784, "Adamantine"),
            new SourceRow(8790, "Trinitanium"),
            new SourceRow(8793, "TitaniumIridium"),
            new SourceRow(8796, "HSSG"),
            new SourceRow(8797, "HSSE"),
            new SourceRow(8798, "HSSS"),
            new SourceRow(8799, "CrystalMatrix"),
            new SourceRow(8800, "CosmicNeutronium"),
            new SourceRow(8802, "TantalumHafniumCarbide"),
            new SourceRow(8803, "CastIron"),
            new SourceRow(9000, "Pumice", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9001, "Diatomite"),
            new SourceRow(9003, "BasalticMineralSand"),
            new SourceRow(9004, "GraniticMineralSand"),
            new SourceRow(9005, "GarnetSand"),
            new SourceRow(9006, "QuartzSand"),
            new SourceRow(9007, "UraniumTetrafluoride"),
            new SourceRow(9008, "UraniumHexafluoride"),
            new SourceRow(9009, "Uranium238Tetrafluoride"),
            new SourceRow(9010, "Uranium238Hexafluoride"),
            new SourceRow(9011, "Uranium235Tetrafluoride"),
            new SourceRow(9012, "Uranium235Hexafluoride"),
            new SourceRow(9100, "RareEarth"),
            new SourceRow(9101, "Almandine", SourceTrait.LENS_GENERATION),
            new SourceRow(9102, "Andradite", SourceTrait.LENS_GENERATION),
            new SourceRow(9103, "Asbestos", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9104, "Hematite"),
            new SourceRow(9105, "Bauxite"),
            new SourceRow(9106, "BrownLimonite"),
            new SourceRow(9107, "Calcite"),
            new SourceRow(9108, "Cassiterite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9109, "Realgar", SourceTrait.FURNACE_OR_SOFT, SourceTrait.LENS_GENERATION),
            new SourceRow(9110, "Celestine"),
            new SourceRow(9111, "Chalcopyrite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9113, "Chromite"),
            new SourceRow(9114, "Cinnabar", SourceTrait.LENS_GENERATION),
            new SourceRow(9115, "Cobaltite"),
            new SourceRow(9116, "Cooperite"),
            new SourceRow(9117, "Galena", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9118, "Garnierite"),
            new SourceRow(9119, "Grossular", SourceTrait.LENS_GENERATION),
            new SourceRow(9120, "Ilmenite"),
            new SourceRow(9122, "Magnetite"),
            new SourceRow(9123, "Molybdenite"),
            new SourceRow(9124, "Powellite"),
            new SourceRow(9125, "Pyrite"),
            new SourceRow(9126, "Pyrolusite"),
            new SourceRow(9127, "Pyrope", SourceTrait.LENS_GENERATION),
            new SourceRow(9128, "Scheelite"),
            new SourceRow(9129, "Spessartine", SourceTrait.LENS_GENERATION),
            new SourceRow(9130, "Sphalerite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9131, "Stibnite"),
            new SourceRow(9132, "Tetrahedrite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9133, "Tungstate"),
            new SourceRow(9134, "Uraninite"),
            new SourceRow(9135, "Uvarovite", SourceTrait.LENS_GENERATION),
            new SourceRow(9136, "Wulfenite"),
            new SourceRow(9137, "YellowLimonite"),
            new SourceRow(9138, "Perlite"),
            new SourceRow(9139, "Borax"),
            new SourceRow(9140, "PotassiumFeldspar"),
            new SourceRow(9141, "Biotite"),
            new SourceRow(9143, "VanadiumMagnetite"),
            new SourceRow(9144, "Bastnasite"),
            new SourceRow(9145, "Pentlandite"),
            new SourceRow(9146, "Spodumene"),
            new SourceRow(9147, "Pollucite"),
            new SourceRow(9148, "Tantalite"),
            new SourceRow(9149, "Lepidolite"),
            new SourceRow(9150, "Glauconite"),
            new SourceRow(9152, "Vermiculite"),
            new SourceRow(9153, "Bentonite"),
            new SourceRow(9154, "Palygorskite"),
            new SourceRow(9155, "Pitchblende"),
            new SourceRow(9156, "Malachite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9157, "Mirabilite"),
            new SourceRow(9158, "Mica"),
            new SourceRow(9159, "Trona"),
            new SourceRow(9160, "Barite"),
            new SourceRow(9161, "Gypsum"),
            new SourceRow(9162, "Alunite"),
            new SourceRow(9164, "Wollastonite"),
            new SourceRow(9165, "Zeolite"),
            new SourceRow(9166, "Kyanite"),
            new SourceRow(9167, "Kaolinite"),
            new SourceRow(9169, "Talc"),
            new SourceRow(9171, "Greenschist", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9174, "Graphite"),
            new SourceRow(9175, "Graphene"),
            new SourceRow(9177, "Komatiite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9180, "Quartzite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9184, "Blueschist", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9185, "Bluestone", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9186, "Chert", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9187, "Dacite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9188, "Andesite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9189, "Limestone", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9190, "Shale", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9192, "Rutile"),
            new SourceRow(9193, "Stolzite"),
            new SourceRow(9194, "Ferberite"),
            new SourceRow(9195, "Huebnerite"),
            new SourceRow(9196, "Russellite"),
            new SourceRow(9197, "Pinalite"),
            new SourceRow(9198, "DiduraniumTrioxide"),
            new SourceRow(9199, "DuraniumHexafluoride"),
            new SourceRow(9200, "DuraniumHexachloride"),
            new SourceRow(9201, "DuraniumHexabromide"),
            new SourceRow(9202, "DuraniumHexaiodide"),
            new SourceRow(9203, "DuraniumHexaastatide"),
            new SourceRow(9204, "TritaniumDioxide"),
            new SourceRow(9205, "TritaniumHexafluoride"),
            new SourceRow(9206, "TritaniumHexachloride"),
            new SourceRow(9207, "TritaniumHexabromide"),
            new SourceRow(9208, "TritaniumHexaiodide"),
            new SourceRow(9209, "TritaniumHexaastatide"),
            new SourceRow(9210, "Bromargyrite"),
            new SourceRow(9211, "Smithsonite"),
            new SourceRow(9212, "Sperrylite"),
            new SourceRow(9213, "Kesterite"),
            new SourceRow(9214, "Stannite"),
            new SourceRow(9215, "Fluorite"),
            new SourceRow(9216, "Arsenopyrite"),
            new SourceRow(9217, "Wolframite"),
            new SourceRow(9218, "Kimberlite", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9219, "Prismarine", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9220, "PrismarineDark", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9221, "Bischofite"),
            new SourceRow(9222, "Slate", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9223, "Blackstone", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9243, "ClayBrick"),
            new SourceRow(9246, "Columbite"),
            new SourceRow(9247, "Coltan"),
            new SourceRow(9248, "Deepslate", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9261, "TitanRock", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9271, "Quicklime"),
            new SourceRow(9300, "Oak"),
            new SourceRow(9301, "Birch"),
            new SourceRow(9302, "Spruce"),
            new SourceRow(9303, "Junglewood"),
            new SourceRow(9304, "Acacia"),
            new SourceRow(9305, "DarkOak"),
            new SourceRow(9306, "Crimsonwood"),
            new SourceRow(9307, "Warpedwood"),
            new SourceRow(9308, "WoodCompressed"),
            new SourceRow(9309, "WoodDead"),
            new SourceRow(9310, "WoodRotten"),
            new SourceRow(9311, "WoodMossy"),
            new SourceRow(9312, "WoodFrozen"),
            new SourceRow(9313, "Maple"),
            new SourceRow(9314, "Willow"),
            new SourceRow(9315, "BlueMahoe"),
            new SourceRow(9316, "Hazel"),
            new SourceRow(9317, "Cinnamonwood"),
            new SourceRow(9318, "Coconutwood"),
            new SourceRow(9319, "Rainbowood"),
            new SourceRow(9344, "Cherrywood"),
            new SourceRow(9372, "Mangrove"),
            new SourceRow(9409, "BlueSpruce"),
            new SourceRow(9700, "MeatRaw", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9701, "MeatCooked"),
            new SourceRow(9702, "Wheat"),
            new SourceRow(9703, "Sugar", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9705, "Rye"),
            new SourceRow(9709, "Potato"),
            new SourceRow(9710, "MeatRotten"),
            new SourceRow(9711, "FishCooked"),
            new SourceRow(9712, "FishRaw", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9713, "FishRotten"),
            new SourceRow(9779, "SoylentGreen"),
            new SourceRow(9780, "Cheese", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9781, "Chili"),
            new SourceRow(9782, "Cocoa"),
            new SourceRow(9783, "Chocolate", SourceTrait.FURNACE_OR_SOFT),
            new SourceRow(9784, "Coffee"),
            new SourceRow(9785, "Cinnamon"),
            new SourceRow(9790, "Milk"),
            new SourceRow(9791, "Honey"),
            new SourceRow(9792, "Tea"),
            new SourceRow(9793, "Honeydew"),
            new SourceRow(9794, "Mint"),
            new SourceRow(9796, "Hazelnut"),
            new SourceRow(9800, "Water"),
            new SourceRow(9801, "Snow"),
            new SourceRow(9802, "Ice", SourceTrait.LENS_GENERATION),
            new SourceRow(9803, "FreshWater"),
            new SourceRow(9804, "Saltwater"),
            new SourceRow(9805, "HolyWater"),
            new SourceRow(9806, "SeaWater"),
            new SourceRow(9808, "WaterDistilled"),
            new SourceRow(9809, "HydrogenPeroxide"),
            new SourceRow(9810, "Lava"),
            new SourceRow(9811, "SemiheavyWater"),
            new SourceRow(9812, "HeavyWater"),
            new SourceRow(9813, "TritiatedWater"),
            new SourceRow(9814, "Steam"),
            new SourceRow(9815, "SaltedWater"),
            new SourceRow(9820, "NitroCarbon"),
            new SourceRow(9821, "Glyceryl"),
            new SourceRow(9822, "SodiumPersulfate"),
            new SourceRow(9823, "SodiumSulfide"),
            new SourceRow(9824, "SulfuricAcid"),
            new SourceRow(9825, "NitricAcid"),
            new SourceRow(9826, "HydrochloricAcid"),
            new SourceRow(9827, "AquaRegia"),
            new SourceRow(9828, "Glycerol"),
            new SourceRow(9829, "HydrogenFluoride"),
            new SourceRow(9830, "Air"),
            new SourceRow(9831, "NitrogenDioxide"),
            new SourceRow(9832, "Methane"),
            new SourceRow(9833, "MethaneIce"),
            new SourceRow(9834, "SulfurDioxide"),
            new SourceRow(9835, "SulfurTrioxide"),
            new SourceRow(9836, "CarbonDioxide"),
            new SourceRow(9837, "NitrogenMonoxide"),
            new SourceRow(9838, "CarbonMonoxide"),
            new SourceRow(9839, "HeliumNeon"),
            new SourceRow(9840, "Biomass"),
            new SourceRow(9841, "BioFuel"),
            new SourceRow(9842, "Ethanol"),
            new SourceRow(9843, "CarbonTrioxide"),
            new SourceRow(9844, "DisulfuricAcid"),
            new SourceRow(9850, "Oil"),
            new SourceRow(9851, "OilSand"),
            new SourceRow(9852, "CrudeOil"),
            new SourceRow(9853, "OilShale"),
            new SourceRow(9860, "Fuel"),
            new SourceRow(9861, "NitroFuel"),
            new SourceRow(9862, "Kerosine"),
            new SourceRow(9863, "Diesel"),
            new SourceRow(9864, "Petrol"),
            new SourceRow(9865, "Propane"),
            new SourceRow(9866, "Butane"),
            new SourceRow(9867, "Propylene"),
            new SourceRow(9868, "Ethylene"),
            new SourceRow(9870, "Creosote"),
            new SourceRow(9871, "FishOil"),
            new SourceRow(9872, "SeedOil"),
            new SourceRow(9873, "HempOil"),
            new SourceRow(9874, "LinOil"),
            new SourceRow(9875, "SunflowerOil"),
            new SourceRow(9876, "NutOil"),
            new SourceRow(9877, "OliveOil"),
            new SourceRow(9880, "FryingOilHot"),
            new SourceRow(9881, "Glue"),
            new SourceRow(9882, "Lubricant"),
            new SourceRow(9883, "ConstructionFoam"),
            new SourceRow(9886, "Latex"));
}
