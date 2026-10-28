package gregtech.content.recipe;

import static gregtech.api.material.GTValues.U;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Finite facts for the existing dust-or-ingot 1U thermal recycling producer, not global material metadata.
 * Original GT6: 3703e40308c8c030763fd6297dea8b210d2a77b1, MT declarations and factory closure
 * (61-93, 164-217, 219-328); OreDictMaterial 749-776/799-806; Loader_Fluids 40-214/364/572-662.
 * The roster is 1,002 exact source input identities plus target-only Water/Lava. Unknown identities,
 * including port-only Superconductor 9516, are deliberately absent. A source NO is only valid inside
 * this reviewed roster and the verified default native context; it says nothing about external bindings.
 *
 * <p>Positive setSmelting adds MELTING. Element FURNACE uses the factory's original melting argument,
 * not later heat/native flags. Non-element metal/alloy does not imply MOLTEN. The all-MELTING fluid
 * loop at Loader_Fluids 216-224 is commented out. Explicit bindings precede the final LIQUID/MOLTEN
 * loop at 657-662; Aerotheum alone uses gas. Source density is not inferred here: the planner consumes
 * valid runtime density, target melting and actual fluid temperature without claiming imported-stat parity.
 */
public final class SourceRecyclingFacts {
    private SourceRecyclingFacts() {}

    public enum Decision {
        YES,
        NO,
        UNKNOWN
    }

    public enum PhaseKind {
        DEFAULT_BINDING,
        NO_DEFAULT_PHASE,
        OPTIONAL_EXTERNAL,
        UNKNOWN
    }

    public enum Accessor {
        LIQUID,
        GAS
    }

    public record Identity(int id, String name) {
        public Identity {
            Objects.requireNonNull(name);
        }

        public boolean matches(int materialId, String canonicalName) {
            return id == materialId && name.equals(canonicalName);
        }
    }
    /** The source path is provenance, not a promise of the same modern native registry spelling. */
    public record Binding(
            String sourceRegistryPath,
            Accessor accessor,
            long millibuckets,
            long referenceMaterialUnits,
            int sourceLine) {
        public Binding {
            Objects.requireNonNull(sourceRegistryPath);
            Objects.requireNonNull(accessor);
            if (sourceRegistryPath.isEmpty() || millibuckets <= 0 || referenceMaterialUnits <= 0 || sourceLine <= 0)
                throw new IllegalArgumentException("Invalid reviewed source phase");
        }
    }

    public record Phase(PhaseKind kind, Binding binding, boolean hasOptionalExternalBinding) {
        public Phase {
            Objects.requireNonNull(kind);
            if ((kind == PhaseKind.DEFAULT_BINDING) != (binding != null))
                throw new IllegalArgumentException("Only a proven default phase has a binding");
        }
    }
    /** Shipped recipe-phase port metadata after built-in material postInit, distinct from source facts.
     * Equal semantic writes are harmless;
     * callers still prove core/native ownership and explicit item composition separately. */
    public record PortDefaults(boolean melting, Identity target, long targetUnitsPerU, boolean generatedMolten) {
        public PortDefaults {
            Objects.requireNonNull(target);
        }

        public boolean matches(boolean observedMelting, int targetId, String targetName, long amount) {
            return melting == observedMelting && target.matches(targetId, targetName) && targetUnitsPerU == amount;
        }
    }

    public record Facts(
            Identity identity,
            Decision melting,
            Decision blacklistedSmelter,
            Decision furnace,
            Identity target,
            long targetUnitsPerU,
            Phase phase,
            PortDefaults portDefaults,
            int sourceLine) {
        public Facts {
            Objects.requireNonNull(identity);
            Objects.requireNonNull(melting);
            Objects.requireNonNull(blacklistedSmelter);
            Objects.requireNonNull(furnace);
            Objects.requireNonNull(target);
            Objects.requireNonNull(phase);
            if (targetUnitsPerU < 0 || sourceLine <= 0) throw new IllegalArgumentException("Invalid source fact");
        }
    }

    private static final int M = 1, B = 2, F = 4; // source MELTING, BLACKLISTED_SMELTER, FURNACE

    private record Seed(Identity identity, int flags, int targetId, long amount, int sourceLine) {}
    // 74 reviewed missing port MELTING flags; do not change global flags.
    private static final Set<Integer> PORT_NO_MELTING = Set.of(
            350, 8001, 8008, 8009, 8010, 8013, 8016, 8017, 8018, 8020, 8026, 8028, 8029, 8030, 8031, 8033, 8034, 8211,
            8213, 8222, 8235, 8236, 8237, 8238, 8239, 8240, 8244, 8246, 8266, 8267, 8280, 8281, 8318, 8319, 8320, 8333,
            8340, 8341, 8368, 8369, 8370, 8371, 8372, 8456, 8500, 8512, 8513, 8515, 8599, 9003, 9004, 9007, 9009, 9011,
            9103, 9104, 9107, 9120, 9122, 9143, 9192, 9243, 9701, 9703, 9710, 9711, 9713, 9778, 9779, 9780, 9783, 9798,
            9799, 9883);
    // 17 port MELTING additions absent in source; Netherrack is NOT one (MT1882).
    // LigniteCoke/PetroleumCoke are added by ImportedMaterialData.applyPostInitFuelStats,
    // not their declarations. Profiles describe recipe-phase defaults after GTMaterialRegistry.postInit.
    private static final Set<Integer> PORT_EXTRA_MELTING =
            Set.of(150, 340, 1090, 1100, 1110, 1130, 1150, 1160, 1170, 1180, 1190, 1200, 1520, 2390, 3720, 8365, 8390);
    // Six missed setAllToTheOutputOf imports: MT1245-1246,1280,1732,1796-1797.
    private static final Set<Integer> PORT_SELF_TARGET = Set.of(8222, 8267, 8652, 8713, 8714, 9243);
    // Default 0faae36d generated-factory identities, not source phase evidence; static finalized fluids excluded.
    private static final Set<Integer> PORT_GENERATED_MOLTEN = Set.of(
            30, 31, 40, 41, 42, 50, 51, 60, 61, 110, 120, 130, 140, 160, 190, 200, 210, 220, 230, 240, 250, 260, 270,
            278, 280, 290, 310, 320, 330, 370, 380, 390, 400, 410, 420, 430, 440, 450, 460, 470, 480, 490, 500, 510,
            520, 550, 560, 570, 580, 590, 600, 610, 620, 630, 640, 650, 660, 670, 680, 690, 700, 710, 720, 730, 740,
            750, 760, 770, 780, 790, 791, 800, 810, 820, 830, 840, 850, 870, 880, 890, 900, 910, 920, 921, 922, 930,
            940, 942, 943, 945, 946, 947, 950, 951, 952, 960, 970, 980, 990, 1000, 1010, 1020, 1030, 1040, 1050, 1060,
            1070, 1080, 1120, 1140, 1148, 2220, 8003, 8005, 8008, 8013, 8016, 8018, 8028, 8031, 8340, 8341, 8368, 8369,
            8370, 8371, 8372, 8456, 8600, 8601, 8602, 8603, 8604, 8605, 8606, 8610, 8611, 8612, 8613, 8614, 8615, 8621,
            8622, 8630, 8631, 8632, 8633, 8634, 8635, 8636, 8637, 8638, 8639, 8640, 8641, 8642, 8643, 8644, 8645, 8646,
            8647, 8648, 8649, 8650, 8651, 8652, 8653, 8654, 8657, 8658, 8659, 8660, 8661, 8662, 8663, 8664, 8665, 8666,
            8667, 8668, 8669, 8670, 8671, 8672, 8673, 8674, 8675, 8676, 8677, 8678, 8679, 8680, 8681, 8682, 8683, 8684,
            8685, 8686, 8687, 8688, 8689, 8690, 8691, 8692, 8693, 8694, 8695, 8696, 8697, 8698, 8700, 8701, 8702, 8703,
            8704, 8705, 8706, 8707, 8708, 8709, 8710, 8711, 8713, 8714, 8715, 8716, 8717, 8718, 8720, 8721, 8722, 8723,
            8725, 8726, 8727, 8728, 8729, 8730, 8731, 8732, 8733, 8734, 8735, 8736, 8737, 8739, 8740, 8741, 8742, 8744,
            8745, 8746, 8747, 8750, 8751, 8752, 8754, 8755, 8756, 8757, 8758, 8759, 8760, 8761, 8762, 8763, 8764, 8765,
            8766, 8767, 8768, 8769, 8770, 8771, 8772, 8773, 8774, 8775, 8776, 8777, 8778, 8779, 8780, 8781, 8782, 8783,
            8784, 8785, 8786, 8790, 8791, 8792, 8793, 8794, 8795, 8796, 8797, 8798, 8799, 8800, 8801, 8802, 8803, 8804,
            8805, 8806, 8807, 8808, 8809, 8810, 8811, 8812, 8813, 9007, 9009, 9011, 9104, 9143, 9175, 9215);
    // Loader_Fluids135-189 optional external receivers. Default-positive bindings still require native proof.
    private static final Set<Integer> OPTIONAL_EXTERNAL = Set.of(
            120, 130, 220, 260, 270, 280, 290, 300, 320, 470, 500, 780, 790, 820, 8318, 8333, 8341, 8600, 8610, 8630,
            8642, 8661, 8678, 8700, 8702, 8703, 8707, 8708, 8709, 8710, 8752);

    private static final Map<Integer, Facts> BY_ID = createFacts();
    private static final List<Facts> ALL = List.copyOf(BY_ID.values());
    // These two output-only identities have no default 1U input, but their own target metadata
    // still participates in the Melter target-to-Calcite gate. Reviewed port defaults, not source flags.
    private static final Map<Integer, PortDefaults> TARGET_ONLY_DEFAULTS = Map.of(
            9800, new PortDefaults(false, new Identity(9800, "Water"), U, false),
            9810, new PortDefaults(false, new Identity(9810, "Lava"), U, false));
    /** Null means unsupported identity, never an implicit source-negative decision. */
    public static Facts find(int id, String canonicalName) {
        Facts value = BY_ID.get(id);
        return value != null && value.identity().matches(id, canonicalName) ? value : null;
    }

    public static PortDefaults defaults(int id, String canonicalName) {
        Facts facts = find(id, canonicalName);
        return facts == null ? null : facts.portDefaults();
    }
    /** Includes the two reviewed target-only profiles; this does not grant input-form eligibility. */
    public static PortDefaults contextDefaults(int id, String canonicalName) {
        Facts facts = find(id, canonicalName);
        return facts == null
                ? null
                : facts.portDefaults() != null ? facts.portDefaults() : TARGET_ONLY_DEFAULTS.get(id);
    }

    public static List<Facts> all() {
        return ALL;
    }

    private static Decision decision(int flags, int bit) {
        return (flags & bit) != 0 ? Decision.YES : Decision.NO;
    }

    private static Map<Integer, Facts> createFacts() {
        Map<Integer, Seed> seeds = new LinkedHashMap<>();
        for (Seed seed : seeds()) {
            if (seeds.put(seed.identity().id(), seed) != null)
                throw new IllegalStateException("Duplicate source identity");
        }
        Map<Integer, Binding> bindings = bindings();
        Map<Integer, Facts> result = new LinkedHashMap<>();
        for (Seed seed : seeds.values()) {
            int id = seed.identity().id();
            Seed targetSeed = seeds.get(seed.targetId());
            if (targetSeed == null) throw new IllegalStateException("Unreviewed thermal target " + seed.targetId());
            Identity target = targetSeed.identity();
            Binding binding = bindings.get(id);
            boolean optional = OPTIONAL_EXTERNAL.contains(id);
            Phase phase = new Phase(
                    binding != null
                            ? PhaseKind.DEFAULT_BINDING
                            : optional ? PhaseKind.OPTIONAL_EXTERNAL : PhaseKind.NO_DEFAULT_PHASE,
                    binding,
                    optional);
            boolean melting = decision(seed.flags(), M) == Decision.YES;
            PortDefaults port = null;
            if (id != 9800 && id != 9810) {
                boolean nativeMelting = PORT_EXTRA_MELTING.contains(id) || (melting && !PORT_NO_MELTING.contains(id));
                boolean self = PORT_SELF_TARGET.contains(id);
                port = new PortDefaults(
                        nativeMelting,
                        self ? seed.identity() : target,
                        self ? U : seed.amount(),
                        PORT_GENERATED_MOLTEN.contains(id));
            }
            result.put(
                    id,
                    new Facts(
                            seed.identity(),
                            decision(seed.flags(), M),
                            decision(seed.flags(), B),
                            decision(seed.flags(), F),
                            target,
                            seed.amount(),
                            phase,
                            port,
                            seed.sourceLine()));
        }
        for (int id : bindings.keySet())
            if (!seeds.containsKey(id)) throw new IllegalStateException("Unreviewed phase " + id);
        return java.util.Collections.unmodifiableMap(result);
    }

    private static void row(List<Seed> rows, int id, String name, int flags, int targetId, long amount, int line) {
        rows.add(new Seed(new Identity(id, name), flags, targetId, amount, line));
    }

    private static List<Seed> seeds() {
        List<Seed> rows = new ArrayList<>(1004);
        seeds0(rows);
        seeds1(rows);
        seeds2(rows);
        seeds3(rows);
        seeds4(rows);
        seeds5(rows);
        seeds6(rows);
        seeds7(rows);
        seeds8(rows);
        seeds9(rows);
        seeds10(rows);
        return rows;
    }

    private static void seeds0(List<Seed> rows) {
        row(rows, 10, "Hydrogen", 0, 10, U, 380);
        row(rows, 11, "Deuterium", 0, 11, U, 381);
        row(rows, 12, "Tritium", 0, 12, U, 382);
        row(rows, 20, "Helium", 0, 20, U, 383);
        row(rows, 21, "Helium3", 0, 21, U, 384);
        row(rows, 30, "Lithium", M | F, 30, U, 385);
        row(rows, 31, "Lithium6", M | F, 31, U, 386);
        row(rows, 40, "Beryllium", M, 40, U, 387);
        row(rows, 41, "Beryllium7", M, 41, U, 388);
        row(rows, 42, "Beryllium8", M, 42, U, 389);
        row(rows, 50, "Boron", M, 50, U, 390);
        row(rows, 51, "Boron11", M, 51, U, 391);
        row(rows, 60, "Carbon", M, 60, U, 392);
        row(rows, 61, "Carbon13", M, 61, U, 393);
        row(rows, 62, "Carbon14", M, 62, U, 394);
        row(rows, 70, "Nitrogen", 0, 70, U, 395);
        row(rows, 80, "Oxygen", 0, 80, U, 396);
        row(rows, 90, "Fluorine", 0, 90, U, 397);
        row(rows, 100, "Neon", 0, 100, U, 398);
        row(rows, 110, "Sodium", M | F, 110, U, 399);
        row(rows, 120, "Magnesium", M | F, 120, U, 400);
        row(rows, 130, "Aluminium", M | F, 130, U, 401);
        row(rows, 140, "Silicon", M, 140, U, 402);
        row(rows, 150, "Phosphor", 0, 150, U, 403);
        row(rows, 160, "Sulfur", M, 160, U, 404);
        row(rows, 170, "Chlorine", 0, 170, U, 405);
        row(rows, 180, "Argon", 0, 180, U, 406);
        row(rows, 190, "Potassium", M | F, 190, U, 407);
        row(rows, 200, "Calcium", M | F, 200, U, 408);
        row(rows, 210, "Scandium", M, 210, U, 409);
        row(rows, 220, "Titanium", M, 220, U, 410);
        row(rows, 230, "Vanadium", M, 230, U, 411);
        row(rows, 240, "Chromium", M, 240, U, 412);
        row(rows, 250, "Manganese", M, 250, U, 413);
        row(rows, 260, "Iron", M, 260, U, 414);
        row(rows, 270, "Cobalt", M, 270, U, 415);
        row(rows, 278, "Cobalt60", M, 278, U, 416);
        row(rows, 280, "Nickel", M, 280, U, 417);
        row(rows, 290, "Copper", M | F, 290, U, 418);
        row(rows, 300, "Zinc", M | F, 300, U, 419);
        row(rows, 310, "Gallium", M | F, 310, U, 420);
        row(rows, 320, "Germanium", M | F, 320, U, 421);
        row(rows, 330, "Arsenic", M | F, 330, U, 422);
        row(rows, 340, "Selenium", 0, 340, U, 423);
        row(rows, 350, "Bromine", M, 350, U, 424);
        row(rows, 360, "Krypton", 0, 360, U, 425);
        row(rows, 370, "Rubidium", M | F, 370, U, 426);
        row(rows, 380, "Strontium", M | F, 380, U, 427);
        row(rows, 390, "Yttrium", M, 390, U, 428);
        row(rows, 400, "Zirconium", M, 400, U, 429);
        row(rows, 410, "Niobium", M, 410, U, 430);
        row(rows, 420, "Molybdenum", M, 420, U, 431);
        row(rows, 430, "Technetium", M, 430, U, 432);
        row(rows, 440, "Ruthenium", M, 440, U, 433);
        row(rows, 450, "Rhodium", M, 450, U, 434);
        row(rows, 460, "Palladium", M, 460, U, 435);
        row(rows, 470, "Silver", M, 470, U, 436);
        row(rows, 480, "Cadmium", M | F, 480, U, 437);
        row(rows, 490, "Indium", M | F, 490, U, 438);
        row(rows, 500, "Tin", M | F, 500, U, 439);
        row(rows, 510, "Antimony", M | F, 510, U, 440);
        row(rows, 520, "Tellurium", M | F, 520, U, 441);
        row(rows, 530, "Iodine", 0, 530, U, 442);
        row(rows, 540, "Xenon", 0, 540, U, 443);
        row(rows, 550, "Caesium", M | F, 550, U, 444);
        row(rows, 560, "Barium", M | F, 560, U, 445);
        row(rows, 570, "Lanthanium", M | F, 570, U, 446);
        row(rows, 580, "Cerium", M | F, 580, U, 447);
        row(rows, 590, "Praseodymium", M, 590, U, 448);
        row(rows, 600, "Neodymium", M, 600, U, 449);
        row(rows, 610, "Promethium", M, 610, U, 450);
        row(rows, 620, "Samarium", M, 620, U, 451);
        row(rows, 630, "Europium", M | F, 630, U, 452);
        row(rows, 640, "Gadolinium", M, 640, U, 453);
        row(rows, 650, "Terbium", M, 650, U, 454);
        row(rows, 660, "Dysprosium", M, 660, U, 455);
        row(rows, 670, "Holmium", M, 670, U, 456);
        row(rows, 680, "Erbium", M, 680, U, 457);
        row(rows, 690, "Thulium", M, 690, U, 458);
        row(rows, 700, "Ytterbium", M | F, 700, U, 459);
        row(rows, 710, "Lutetium", M, 710, U, 460);
        row(rows, 720, "Hafnium", M, 720, U, 461);
        row(rows, 730, "Tantalum", M, 730, U, 462);
        row(rows, 740, "Tungsten", M, 740, U, 463);
        row(rows, 750, "Rhenium", M, 750, U, 464);
        row(rows, 760, "OsmiumElemental", M, 760, U, 465);
        row(rows, 770, "Iridium", M, 770, U, 466);
        row(rows, 780, "Platinum", M, 780, U, 467);
        row(rows, 790, "Gold", M, 790, U, 468);
        row(rows, 791, "Gold198", M, 791, U, 469);
        row(rows, 800, "Mercury", M | F, 800, U, 470);
        row(rows, 810, "Thallium", M | F, 810, U, 471);
        row(rows, 820, "Lead", M | F, 820, U, 472);
        row(rows, 830, "Bismuth", M | F, 830, U, 473);
        row(rows, 840, "Polonium", M | F, 840, U, 474);
        row(rows, 850, "Astatine", M | F, 850, U, 475);
        row(rows, 860, "Radon", 0, 860, U, 476);
        row(rows, 870, "Francium", M | F, 870, U, 477);
        row(rows, 880, "Radium", M | F, 880, U, 478);
        row(rows, 890, "Actinium", M, 890, U, 479);
    }

    private static void seeds1(List<Seed> rows) {
        row(rows, 900, "Thorium", M, 900, U, 480);
        row(rows, 910, "Protactinium", M, 910, U, 481);
        row(rows, 920, "Uranium", M, 920, U, 482);
        row(rows, 921, "Uranium235", M, 921, U, 483);
        row(rows, 922, "Uranium233", M, 922, U, 484);
        row(rows, 930, "Neptunium", M | F, 930, U, 485);
        row(rows, 940, "Plutonium", M | F, 940, U, 486);
        row(rows, 942, "Plutonium240", M | F, 942, U, 487);
        row(rows, 943, "Plutonium241", M | F, 943, U, 488);
        row(rows, 945, "Plutonium243", M | F, 945, U, 489);
        row(rows, 946, "Plutonium238", M | F, 946, U, 490);
        row(rows, 947, "Plutonium239", M | F, 947, U, 491);
        row(rows, 950, "Americium", M, 950, U, 492);
        row(rows, 951, "Americium241", M, 951, U, 493);
        row(rows, 952, "Americium242", M, 952, U, 494);
        row(rows, 960, "Curium", M, 960, U, 495);
        row(rows, 970, "Berkelium", M, 970, U, 496);
        row(rows, 980, "Californium", M | F, 980, U, 497);
        row(rows, 990, "Einsteinium", M | F, 990, U, 498);
        row(rows, 1000, "Fermium", M | F, 1000, U, 499);
        row(rows, 1010, "Mendelevium", M | F, 1010, U, 500);
        row(rows, 1020, "Nobelium", M | F, 1020, U, 501);
        row(rows, 1030, "Lawrencium", M, 1030, U, 502);
        row(rows, 1040, "Rutherfordium", M, 1040, U, 503);
        row(rows, 1050, "Dubnium", M | F, 1050, U, 504);
        row(rows, 1060, "Seaborgium", M | F, 1060, U, 505);
        row(rows, 1070, "Bohrium", M | F, 1070, U, 506);
        row(rows, 1080, "Hassium", M | F, 1080, U, 507);
        row(rows, 1090, "Meitnerium", 0, 1090, U, 508);
        row(rows, 1100, "Darmstadtium", 0, 1100, U, 509);
        row(rows, 1110, "Roentgenium", 0, 1110, U, 510);
        row(rows, 1120, "Copernicium", M | F, 1120, U, 511);
        row(rows, 1130, "Nihonium", 0, 1130, U, 512);
        row(rows, 1140, "Flerovium", M | F, 1140, U, 513);
        row(rows, 1148, "Flerovium298", M | F, 1148, U, 514);
        row(rows, 1150, "Moscovium", 0, 1150, U, 515);
        row(rows, 1160, "Livermorium", 0, 1160, U, 516);
        row(rows, 1170, "Farnsium", 0, 1170, U, 517);
        row(rows, 1180, "Oganesson", 0, 1180, U, 518);
        row(rows, 1190, "Ununennium", 0, 1190, U, 519);
        row(rows, 1200, "Unbinilium", 0, 1200, U, 520);
        row(rows, 1250, "TritaniumElemental", M, 1250, U, 695);
        row(rows, 1260, "Trinium", M, 1260, U, 696);
        row(rows, 1450, "DuraniumElemental", M, 1450, U, 715);
        row(rows, 1520, "Vibranium", 0, 1520, U, 722);
        row(rows, 1740, "Naquadah", M, 1740, U, 744);
        row(rows, 1741, "NaquadahEnriched", M, 1741, U, 745);
        row(rows, 1742, "Naquadria", M, 1742, U, 746);
        row(rows, 1850, "Abyssalnite", M, 1850, U, 757);
        row(rows, 1860, "Coralium", M, 1860, U, 758);
        row(rows, 1870, "Dreadium", M, 1870, U, 759);
        row(rows, 1880, "Ethaxium", M, 1880, U, 760);
        row(rows, 2210, "Atlarus", M, 2210, U, 793);
        row(rows, 2220, "Adamantium", M, 2220, U, 794);
        row(rows, 2390, "MacGuffium", 0, 2390, U, 811);
        row(rows, 3720, "Gravitonium", 0, 3720, U, 944);
        row(rows, 8000, "SiliconDioxide", F, 8000, U, 1056);
        row(rows, 8001, "Glass", M | F, 8001, U, 1057);
        row(rows, 8002, "Flint", M, 8000, U, 1058);
        row(rows, 8003, "Carborundum", M, 8003, U, 1055);
        row(rows, 8004, "LithiumOxide", 0, 8004, U, 1124);
        row(rows, 8005, "Ferrite", M, 8005, U, 1125);
        row(rows, 8006, "Datolite", 0, 8006, U, 1062);
        row(rows, 8007, "HydrogenBorate", 0, 8007, U, 1061);
        row(rows, 8008, "Alumina", M, 8008, U, 1081);
        row(rows, 8009, "Cryolite", M, 8009, U, 1142);
        row(rows, 8010, "AluminiumFluoride", M, 8010, U, 1082);
        row(rows, 8011, "HexafluorosilicicAcid", 0, 8011, U, 1054);
        row(rows, 8012, "SodiumAluminate", 0, 8012, U, 1140);
        row(rows, 8013, "SodiumCarbonate", M, 8013, U, 1139);
        row(rows, 8014, "AluminiumHydroxide", M, 8008, 5 * U / 14, 1083);
        row(rows, 8015, "PotassiumHydroxide", 0, 8015, U, 1149);
        row(rows, 8016, "MagnesiumCarbonate", M, 8016, U, 1101);
        row(rows, 8017, "FerricChloride", M, 8017, U, 1096);
        row(rows, 8018, "MagnesiumChloride", M, 8018, U, 1100);
        row(rows, 8019, "SodiumNitrate", 0, 8019, U, 1130);
        row(rows, 8020, "PotassiumCarbonate", M, 8020, U, 1156);
        row(rows, 8021, "PotassiumSulfite", 0, 8021, U, 1153);
        row(rows, 8022, "PotassiumPersulfate", 0, 8022, U, 1151);
        row(rows, 8023, "PotassiumAluminate", 0, 8023, U, 1157);
        row(rows, 8026, "TungstenTrioxide", M, 8026, U, 1077);
        row(rows, 8027, "TungsticAcid", M, 8026, 4 * U / 7, 1078);
        row(rows, 8028, "CalciumChloride", M, 8028, U, 1104);
        row(rows, 8029, "LithiumChloride", M, 8029, U, 1121);
        row(rows, 8030, "FerrousChloride", M, 8030, U, 1095);
        row(rows, 8031, "ManganeseChloride", M, 8031, U, 1091);
        row(rows, 8032, "LithiumHydroxide", 0, 8032, U, 1126);
        row(rows, 8033, "LithiumChlorate", M, 8033, U, 1122);
        row(rows, 8034, "LithiumPerchlorate", M, 8034, U, 1123);
        row(rows, 8035, "FerricOxyhydroxide", M, 9104, 5 * U / 14, 1097);
        row(rows, 8036, "PotassiumFluoride", 0, 8036, U, 1158);
        row(rows, 8037, "SodiumFluoride", 0, 8037, U, 1141);
        row(rows, 8038, "PotassiumHeptafluorotantalate", 0, 8038, U, 1159);
        row(rows, 8039, "SodiumHydrogencarbonate", 0, 8039, U, 1132);
        row(rows, 8100, "Sand", M | F, 8001, U, 1618);
        row(rows, 8101, "Soulsand", 0, 8101, U, 1622);
        row(rows, 8102, "SluiceSand", 0, 8102, U, 1623);
        row(rows, 8103, "PlatinumGroupSludge", 0, 8103, U, 1624);
        row(rows, 8104, "RedSand", M | F, 8001, U, 1619);
        row(rows, 8105, "WhiteEndSand", M | F, 8001, U, 1620);
    }

    private static void seeds2(List<Seed> rows) {
        row(rows, 8106, "BlackEndSand", M | F, 8001, U, 1621);
        row(rows, 8196, "Teflon", M | F, 8196, 2 * U / 3, 1303);
        row(rows, 8197, "PVC", M | F, 8197, 2 * U / 3, 1304);
        row(rows, 8198, "Bakelite", M | F, 8198, 2 * U / 3, 1305);
        row(rows, 8199, "HardPlastic", M | F, 8199, 2 * U / 3, 1306);
        row(rows, 8200, "Ashes", 0, 8200, U, 1228);
        row(rows, 8201, "DarkAshes", 0, 8201, U, 1229);
        row(rows, 8202, "VolcanicAshes", 0, 8202, U, 1230);
        row(rows, 8203, "Sylvite", 0, 8203, U, 1147);
        row(rows, 8204, "Salt", 0, 8204, U, 1129);
        row(rows, 8205, "PotassiumNitrate", 0, 8205, U, 1148);
        row(rows, 8206, "Niter", 0, 8206, U, 1293);
        row(rows, 8207, "Phosphate", 0, 8207, U, 1074);
        row(rows, 8208, "Phosphorus", 0, 8208, U, 1294);
        row(rows, 8209, "Apatite", 0, 8209, U, 1298);
        row(rows, 8210, "Blizz", 0, 8210, U, 1274);
        row(rows, 8211, "Blaze", M, 8211, U, 1275);
        row(rows, 8213, "Cryotheum", M, 8213, U, 1652);
        row(rows, 8214, "Obsidian", M, 9810, U, 1636);
        row(rows, 8215, "Clay", M, 8225, U, 1281);
        row(rows, 8216, "Paper", 0, 8216, U, 1300);
        row(rows, 8217, "Rubber", M | F, 8217, 2 * U / 3, 1301);
        row(rows, 8218, "Plastic", M | F, 8218, 2 * U / 3, 1302);
        row(rows, 8219, "Bone", 0, 8219, U, 1307);
        row(rows, 8220, "Gunpowder", 0, 8220, U, 1309);
        row(rows, 8221, "Wood", M, 8200, U / 4, 1244);
        row(rows, 8222, "WoodTreated", M, 8200, U / 4, 1245);
        row(rows, 8223, "LiveRoot", 0, 8223, U, 1258);
        row(rows, 8224, "WoodRubber", M, 8200, U / 2, 1247);
        row(rows, 8225, "Ceramic", 0, 8225, U, 1279);
        row(rows, 8226, "Phosphorite", 0, 8226, U, 1299);
        row(rows, 8227, "PeanutWood", 0, 8227, U, 1256);
        row(rows, 8228, "Indigo", 0, 8228, U, 1314);
        row(rows, 8229, "CobaltHexahydrate", 0, 8229, U, 1189);
        row(rows, 8230, "SodiumBisulfate", 0, 8230, U, 1133);
        row(rows, 8231, "SodiumPyrosulfate", 0, 8231, U, 1138);
        row(rows, 8232, "PotassiumBisulfate", 0, 8232, U, 1150);
        row(rows, 8233, "PotassiumPyrosulfate", 0, 8233, U, 1155);
        row(rows, 8234, "VanadiumPentoxide", M, 230, U / 7, 1065);
        row(rows, 8235, "Wax", M | F, 8235, U, 1262);
        row(rows, 8236, "WaxBee", M | F, 8236, U, 1263);
        row(rows, 8237, "WaxRefractory", M | F, 8237, U, 1264);
        row(rows, 8238, "WaxParaffin", M | F, 8238, U, 1265);
        row(rows, 8239, "WaxPlant", M | F, 8239, U, 1266);
        row(rows, 8240, "WaxMagic", M | F, 8240, U, 1267);
        row(rows, 8241, "Leather", M, 8200, U / 9, 1313);
        row(rows, 8242, "IodineSalt", 0, 8242, U, 1146);
        row(rows, 8243, "SilverIodide", 0, 8243, U, 1051);
        row(rows, 8244, "Tallow", M, 8244, U, 1312);
        row(rows, 8246, "Aerotheum", M, 8246, U, 1650);
        row(rows, 8247, "Basalz", 0, 8247, U, 1272);
        row(rows, 8248, "Blitz", 0, 8248, U, 1273);
        row(rows, 8249, "Dynamite", 0, 8249, U, 1310);
        row(rows, 8250, "Black", 0, 8250, U, 989);
        row(rows, 8251, "Red", 0, 8251, U, 990);
        row(rows, 8252, "Green", 0, 8252, U, 991);
        row(rows, 8253, "Brown", 0, 8253, U, 992);
        row(rows, 8254, "Blue", 0, 8254, U, 993);
        row(rows, 8255, "Purple", 0, 8255, U, 994);
        row(rows, 8256, "Cyan", 0, 8256, U, 995);
        row(rows, 8257, "LightGray", 0, 8257, U, 996);
        row(rows, 8258, "Gray", 0, 8258, U, 997);
        row(rows, 8259, "Pink", 0, 8259, U, 998);
        row(rows, 8260, "Lime", 0, 8260, U, 999);
        row(rows, 8261, "Yellow", 0, 8261, U, 1000);
        row(rows, 8262, "LightBlue", 0, 8262, U, 1001);
        row(rows, 8263, "Magenta", 0, 8263, U, 1002);
        row(rows, 8264, "Orange", 0, 8264, U, 1003);
        row(rows, 8265, "White", 0, 8265, U, 1004);
        row(rows, 8266, "Asphalt", M | F, 8266, U, 1311);
        row(rows, 8267, "WoodPolished", M, 8200, U / 4, 1246);
        row(rows, 8268, "SodiumHydroxide", 0, 8268, U, 1131);
        row(rows, 8269, "SodiumSulfite", 0, 8269, U, 1136);
        row(rows, 8270, "SodiumSulfate", 0, 8270, U, 1137);
        row(rows, 8271, "PotassiumSulfate", 0, 8271, U, 1154);
        row(rows, 8272, "PotassiumSulfide", 0, 8272, U, 1152);
        row(rows, 8273, "Porcelain", 0, 8273, U, 1289);
        row(rows, 8274, "CalciumSulfate", 0, 8274, U, 1105);
        row(rows, 8275, "Bark", M, 8200, U / 4, 1243);
        row(rows, 8276, "ClayBrown", M, 8225, U, 1282);
        row(rows, 8277, "PetrifiedWood", 0, 8277, U, 1259);
        row(rows, 8278, "ManaDiamond", M, 60, 2 * U, 1366);
        row(rows, 8279, "ElvenDragonstone", M, 60, 2 * U, 1367);
        row(rows, 8280, "WaxAmnesic", M | F, 8280, U, 1268);
        row(rows, 8281, "WaxSoulful", M | F, 8281, U, 1269);
        row(rows, 8282, "CrimsonMiddle", 0, 8282, U, 1487);
        row(rows, 8283, "GreenMiddle", 0, 8283, U, 1488);
        row(rows, 8284, "AquaMiddle", 0, 8284, U, 1489);
        row(rows, 8285, "Valonite", 0, 8285, U, 1490);
        row(rows, 8286, "Weedwood", M, 8200, U / 2, 1250);
        row(rows, 8287, "SlimyBone", 0, 8287, U, 1308);
        row(rows, 8288, "Scabyst", 0, 8288, U, 1491);
        row(rows, 8289, "Livingwood", M, 8200, U / 2, 1251);
        row(rows, 8290, "Dreamwood", M, 8200, U / 2, 1252);
        row(rows, 8291, "Skyroot", M, 8200, U / 2, 1249);
        row(rows, 8292, "Zanite", 0, 8292, 0, 1428);
        row(rows, 8293, "Ambrosium", 0, 8293, U, 1494);
        row(rows, 8294, "Gravitite", M, 60, 2 * U, 1368);
        row(rows, 8295, "Continuum", 0, 8295, U, 1495);
        row(rows, 8296, "Greatwood", M, 8200, U / 2, 1254);
    }

    private static void seeds3(List<Seed> rows) {
        row(rows, 8297, "Silverwood", 0, 8200, 0, 1255);
        row(rows, 8298, "EnergiumRed", 0, 8298, U, 1581);
        row(rows, 8299, "EnergiumCyan", 0, 8299, U, 1582);
        row(rows, 8300, "Diamond", M, 60, 2 * U, 1358);
        row(rows, 8301, "Emerald", M, 40, U / 36, 1371);
        row(rows, 8302, "Ruby", M, 8008, 3 * U / 4, 1381);
        row(rows, 8303, "BalasRuby", 0, 8303, 0, 1390);
        row(rows, 8304, "Sapphire", M, 8008, 3 * U / 4, 1380);
        row(rows, 8305, "GreenSapphire", M, 8008, 3 * U / 4, 1383);
        row(rows, 8306, "Topaz", 0, 8306, 0, 1425);
        row(rows, 8307, "BlueTopaz", 0, 8307, 0, 1426);
        row(rows, 8308, "Tanzanite", 0, 8308, 0, 1427);
        row(rows, 8309, "RedJasper", 0, 8309, 0, 1401);
        row(rows, 8310, "Amber", 0, 8310, U, 1439);
        row(rows, 8311, "Peridot", 0, 8311, 0, 1435);
        row(rows, 8312, "Opal", 0, 8312, 0, 1431);
        row(rows, 8313, "Amethyst", 0, 8313, 0, 1436);
        row(rows, 8314, "OrangeSapphire", M, 8008, 3 * U / 4, 1386);
        row(rows, 8315, "YellowSapphire", M, 8008, 3 * U / 4, 1385);
        row(rows, 8317, "Dilithium", 0, 8317, 0, 1508);
        row(rows, 8318, "EnderPearl", M, 8318, U, 1499);
        row(rows, 8319, "EnderEye", M, 8319, U, 1500);
        row(rows, 8320, "NetherStar", M, 8320, U, 1501);
        row(rows, 8321, "Jade", 0, 8321, U, 1443);
        row(rows, 8322, "Craponite", 0, 8322, U, 1442);
        row(rows, 8323, "Aquamarine", M, 40, U / 36, 1372);
        row(rows, 8324, "Morganite", M, 40, U / 36, 1373);
        row(rows, 8325, "Dioptase", 0, 8325, 0, 1437);
        row(rows, 8326, "Spinel", 0, 8326, 0, 1389);
        row(rows, 8328, "BlueSapphire", M, 8008, 3 * U / 4, 1382);
        row(rows, 8329, "AmethystEnder", 0, 8329, 0, 1498);
        row(rows, 8330, "Lazurite", 0, 8330, U, 1516);
        row(rows, 8331, "Sodalite", 0, 8331, U, 1517);
        row(rows, 8332, "Lapis", 0, 8332, U, 1518);
        row(rows, 8333, "Redstone", M, 8333, U, 1540);
        row(rows, 8334, "Coal", M, 60, U / 2, 1522);
        row(rows, 8335, "HydratedCoal", F, 8335, U, 1534);
        row(rows, 8336, "Charcoal", M, 60, U / 2, 1521);
        row(rows, 8337, "Lignite", 0, 8337, U, 1527);
        row(rows, 8338, "Monazite", B, 8338, U, 1626);
        row(rows, 8340, "Nikolite", M, 8340, U, 1541);
        row(rows, 8341, "Glowstone", M, 8341, U, 1544);
        row(rows, 8342, "Firestone", 0, 8342, U, 1537);
        row(rows, 8343, "Force", 0, 8343, U, 1627);
        row(rows, 8344, "Forcicium", 0, 8344, U, 1628);
        row(rows, 8345, "Forcillium", 0, 8345, U, 1629);
        row(rows, 8346, "NetherQuartz", M | B, 8000, U, 1554);
        row(rows, 8347, "CertusQuartz", M | B, 8000, U, 1564);
        row(rows, 8348, "ChargedCertusQuartz", M | B, 8000, U, 1565);
        row(rows, 8349, "CoalCoke", M, 60, U, 1523);
        row(rows, 8350, "InfusedDull", 0, 8350, U, 1585);
        row(rows, 8351, "InfusedVis", 0, 8351, U, 1586);
        row(rows, 8352, "InfusedAir", 0, 8352, U, 1587);
        row(rows, 8353, "InfusedFire", 0, 8353, U, 1588);
        row(rows, 8354, "InfusedEarth", 0, 8354, U, 1589);
        row(rows, 8355, "InfusedWater", 0, 8355, U, 1590);
        row(rows, 8356, "InfusedEntropy", 0, 8356, U, 1591);
        row(rows, 8357, "InfusedOrder", 0, 8357, U, 1592);
        row(rows, 8358, "InfusedBalance", 0, 8358, U, 1593);
        row(rows, 8360, "Peat", 0, 8360, U, 1530);
        row(rows, 8361, "PeatBituminous", 0, 8361, U, 1531);
        row(rows, 8362, "Anthracite", M, 60, U / 2, 1524);
        row(rows, 8363, "Prismane", 0, 8363, U, 1525);
        row(rows, 8364, "Lonsdaleite", 0, 8364, U, 1526);
        row(rows, 8365, "LigniteCoke", 0, 8365, U, 1528);
        row(rows, 8366, "ArcaneCompound", M | F, 8367, 2 * U, 1481);
        row(rows, 8367, "ArcaneAshes", 0, 8367, U, 1480);
        row(rows, 8368, "GlowstoneCeres", M, 8368, U, 1545);
        row(rows, 8369, "GlowstoneIo", M, 8369, U, 1546);
        row(rows, 8370, "GlowstoneEnceladus", M, 8370, U, 1547);
        row(rows, 8371, "GlowstoneProteus", M, 8371, U, 1548);
        row(rows, 8372, "GlowstonePluto", M, 8372, U, 1549);
        row(rows, 8373, "Ectoplasm", 0, 8373, U, 1536);
        row(rows, 8374, "QuartzBlack", M | B, 8000, U, 1563);
        row(rows, 8381, "DarkMatter", 0, 8381, U, 1577);
        row(rows, 8382, "RedMatter", 0, 8382, U, 1578);
        row(rows, 8383, "PurpleSapphire", M, 8008, 3 * U / 4, 1384);
        row(rows, 8384, "Heliodor", M, 40, U / 36, 1374);
        row(rows, 8385, "Goshenite", M, 40, U / 36, 1375);
        row(rows, 8386, "Bixbite", M, 40, U / 36, 1376);
        row(rows, 8387, "Maxixe", M, 40, U / 36, 1377);
        row(rows, 8388, "Alexandrite", 0, 8388, 0, 1430);
        row(rows, 8389, "Fluix", M | B, 8000, U, 1566);
        row(rows, 8390, "PetroleumCoke", 0, 8390, U, 1529);
        row(rows, 8391, "Frezarite", 0, 8391, U, 1504);
        row(rows, 8392, "RedMeteor", 0, 8392, U, 1505);
        row(rows, 8393, "SunnyQuartz", M | B, 8000, U, 1556);
        row(rows, 8394, "LavenderQuartz", M | B, 8000, U, 1557);
        row(rows, 8395, "RedQuartz", M | B, 8000, U, 1558);
        row(rows, 8396, "BlazeQuartz", M | B, 8000, U, 1559);
        row(rows, 8397, "SmokeyQuartz", M | B, 8000, U, 1560);
        row(rows, 8398, "ManaQuartz", M | B, 8000, U, 1561);
        row(rows, 8399, "ElvenQuartz", M | B, 8000, U, 1562);
        row(rows, 8400, "ChloroauricAcid", 0, 8400, U, 1163);
        row(rows, 8401, "ChloroplatinicAcid", 0, 8401, U, 1164);
        row(rows, 8402, "StannicChloride", 0, 8402, U, 1165);
        row(rows, 8403, "BlackVitriol", 0, 8403, U, 1168);
        row(rows, 8404, "BlueVitriol", 0, 8404, U, 1169);
        row(rows, 8405, "GreenVitriol", 0, 8405, U, 1170);
        row(rows, 8406, "RedVitriol", 0, 8406, U, 1171);
    }

    private static void seeds4(List<Seed> rows) {
        row(rows, 8407, "PinkVitriol", 0, 8407, U, 1172);
        row(rows, 8408, "CyanVitriol", 0, 8408, U, 1173);
        row(rows, 8409, "WhiteVitriol", 0, 8409, U, 1174);
        row(rows, 8410, "GrayVitriol", 0, 8410, U, 1175);
        row(rows, 8411, "MartianVitriol", 0, 8411, U, 1176);
        row(rows, 8412, "VitriolOfClay", 0, 8412, U, 1177);
        row(rows, 8413, "TitaniumTetrachloride", 0, 8413, U, 1087);
        row(rows, 8414, "Shimmerwood", M, 8200, U / 2, 1253);
        row(rows, 8415, "OnyxRed", 0, 8415, 0, 1432);
        row(rows, 8416, "OnyxBlack", 0, 8416, 0, 1433);
        row(rows, 8417, "Amazonite", 0, 8417, 0, 1429);
        row(rows, 8418, "Bamboo", M, 8200, U / 2, 1248);
        row(rows, 8419, "Zircon", M, 400, U / 9, 1511);
        row(rows, 8420, "Azurite", M | F, 290, U / 9, 1512);
        row(rows, 8421, "Eudialyte", 0, 8421, U, 1513);
        row(rows, 8422, "DominicanAmber", 0, 8422, U, 1441);
        row(rows, 8423, "DiamondIndustrial", M, 60, 2 * U, 1365);
        row(rows, 8424, "PinkDiamond", M, 60, 2 * U, 1364);
        row(rows, 8425, "OceanJasper", 0, 8425, 0, 1402);
        row(rows, 8426, "RainforestJasper", 0, 8426, 0, 1403);
        row(rows, 8427, "BlueJasper", 0, 8427, 0, 1404);
        row(rows, 8428, "GreenJasper", 0, 8428, 0, 1405);
        row(rows, 8429, "YellowJasper", 0, 8429, 0, 1406);
        row(rows, 8430, "TigerEye", 0, 8430, 0, 1409);
        row(rows, 8431, "CatsEye", 0, 8431, 0, 1410);
        row(rows, 8432, "DragonEye", 0, 8432, 0, 1411);
        row(rows, 8433, "HawksEye", 0, 8433, 0, 1412);
        row(rows, 8434, "BlackEye", 0, 8434, 0, 1413);
        row(rows, 8435, "TigerIron", 0, 8435, 0, 1414);
        row(rows, 8436, "RedFluorite", M, 9215, U, 1110);
        row(rows, 8437, "PinkFluorite", M, 9215, U, 1111);
        row(rows, 8438, "BlueFluorite", M, 9215, U, 1112);
        row(rows, 8439, "GreenFluorite", M, 9215, U, 1113);
        row(rows, 8440, "BlackFluorite", M, 9215, U, 1114);
        row(rows, 8441, "WhiteFluorite", M, 9215, U, 1115);
        row(rows, 8442, "YellowFluorite", M, 9215, U, 1116);
        row(rows, 8443, "OrangeFluorite", M, 9215, U, 1117);
        row(rows, 8444, "MagentaFluorite", M, 9215, U, 1118);
        row(rows, 8445, "MilkyQuartz", M | B, 8000, U, 1553);
        row(rows, 8446, "BrownAventurine", 0, 8446, 0, 1418);
        row(rows, 8447, "YellowAventurine", 0, 8447, 0, 1419);
        row(rows, 8448, "GreenAventurine", 0, 8448, 0, 1417);
        row(rows, 8449, "BlackAventurine", 0, 8449, 0, 1420);
        row(rows, 8450, "BlueAventurine", 0, 8450, 0, 1421);
        row(rows, 8451, "RedAventurine", 0, 8451, 0, 1422);
        row(rows, 8454, "Chimerite", 0, 8454, U, 1484);
        row(rows, 8455, "ClayRed", M, 8225, U, 1283);
        row(rows, 8456, "Gloomstone", M, 8456, U, 1550);
        row(rows, 8457, "VoidQuartz", M | B, 8000, U, 1555);
        row(rows, 8458, "BluePhosphorus", 0, 8458, U, 1295);
        row(rows, 8459, "RedPhosphorus", 0, 8459, U, 1296);
        row(rows, 8460, "WhitePhosphorus", 0, 8460, U, 1297);
        row(rows, 8461, "NiobiumPentoxide", 0, 8461, U, 1068);
        row(rows, 8462, "TantalumPentoxide", 0, 8462, U, 1071);
        row(rows, 8463, "Sugilite", 0, 8463, 0, 1434);
        row(rows, 8464, "BlueDiamond", M, 60, 2 * U, 1359);
        row(rows, 8465, "GreenDiamond", M, 60, 2 * U, 1360);
        row(rows, 8466, "PurpleDiamond", M, 60, 2 * U, 1361);
        row(rows, 8467, "RedDiamond", M, 60, 2 * U, 1362);
        row(rows, 8468, "YellowDiamond", M, 60, 2 * U, 1363);
        row(rows, 8469, "GoldenAmber", 0, 8469, U, 1440);
        row(rows, 8470, "Carminite", 0, 8470, U, 1438);
        row(rows, 8471, "Breeze", 0, 8471, U, 1276);
        row(rows, 8500, "Stone", M | F, 8500, U, 1631);
        row(rows, 8501, "Concrete", M | F, 8500, U, 1632);
        row(
                rows,
                8502,
                "Netherrack",
                M | B | F,
                8503,
                U,
                1633); // MT1882 late setter adds MELTING and targets NetherBrick.
        row(rows, 8503, "NetherBrick", F, 8503, U, 1634);
        row(rows, 8504, "Endstone", F, 8504, U, 1635);
        row(rows, 8505, "Basalt", F, 8505, U, 3819);
        row(rows, 8506, "Marble", M | F, 9107, 2 * U / 3, 3820);
        row(rows, 8507, "GraniteRed", F, 8507, U, 3829);
        row(rows, 8508, "GraniteBlack", F, 8508, U, 3830);
        row(rows, 8509, "Redrock", F, 8509, U, 3815);
        row(rows, 8511, "Diorite", F, 8511, U, 3833);
        row(rows, 8512, "SpaceStone", M | F, 8512, U, 3798);
        row(rows, 8513, "MoonStone", M | F, 8513, U, 3799);
        row(rows, 8514, "MoonTurf", F, 8514, U, 3800);
        row(rows, 8515, "MarsStone", M | F, 8515, U, 3801);
        row(rows, 8516, "MarsSand", F, 8516, U, 3802);
        row(rows, 8517, "Umber", F, 8517, U, 3813);
        row(rows, 8518, "Granite", F, 8518, U, 3831);
        row(rows, 8519, "Betweenstone", F, 8519, U, 3807);
        row(rows, 8520, "Pitstone", F, 8520, U, 3808);
        row(rows, 8521, "Livingrock", F, 8521, U, 3805);
        row(rows, 8522, "Holystone", F, 8522, U, 3804);
        row(rows, 8523, "Deadrock", F, 8523, U, 3806);
        row(rows, 8524, "Cragrock", F, 8524, U, 3809);
        row(rows, 8528, "SkyStone", F, 8528, U, 3803);
        row(rows, 8599, "Bedrock", M, 8599, U, 1637);
        row(rows, 8600, "Electrum", M, 8600, U, 1691);
        row(rows, 8601, "SterlingSilver", M, 8601, U, 1692);
        row(rows, 8602, "RoseGold", M, 8602, U, 1693);
        row(rows, 8603, "Angmallen", M, 8603, U, 1694);
        row(rows, 8604, "GoldInductive", M, 8604, U, 1695);
        row(rows, 8605, "CdInAgAlloy", M, 8605, U, 1696);
        row(rows, 8606, "GildedIron", M, 260, U, 1697);
        row(rows, 8610, "Bronze", M | F, 8610, U, 1705);
        row(rows, 8611, "BlackBronze", M, 8611, U, 1706);
        row(rows, 8612, "BismuthBronze", M | F, 8612, U, 1707);
        row(rows, 8613, "Hepatizon", M, 8613, U, 1708);
    }

    private static void seeds5(List<Seed> rows) {
        row(rows, 8614, "ArsenicCopper", M | F, 8614, U, 1709);
        row(rows, 8615, "ArsenicBronze", M | F, 8615, U, 1710);
        row(rows, 8620, "Brass", M | F, 8620, U, 1700);
        row(rows, 8621, "CobaltBrass", M | F, 8621, U, 1701);
        row(rows, 8622, "AluminiumAlloy", M, 8622, U, 1702);
        row(rows, 8630, "Steel", M, 8630, U, 1713);
        row(rows, 8631, "BlackSteel", M, 8631, U, 1714);
        row(rows, 8632, "BlueSteel", M, 8632, U, 1715);
        row(rows, 8633, "RedSteel", M, 8633, U, 1716);
        row(rows, 8634, "DamascusSteel", M, 8634, U, 1717);
        row(rows, 8635, "Tungstensteel", M, 8635, U, 1719);
        row(rows, 8636, "StainlessSteel", M, 8636, U, 1759);
        row(rows, 8637, "HSLASteel", M, 8637, U, 1721);
        row(rows, 8638, "TungstenCarbide", M, 8638, U, 1720);
        row(rows, 8639, "HSLASpringSteel", M, 8639, U, 1722);
        row(rows, 8640, "AnnealedCopper", M | F, 8640, U, 1656);
        row(rows, 8641, "DeepIron", M, 8641, U, 1673);
        row(rows, 8642, "PigIron", M, 8643, U, 1724);
        row(rows, 8643, "WroughtIron", M, 8643, U, 1655);
        row(rows, 8644, "IronCompressed", M, 260, U, 1725);
        row(rows, 8645, "IronMagnetic", M, 260, U, 1727);
        row(rows, 8646, "SteelMagnetic", M, 8630, U, 1728);
        row(rows, 8647, "NeodymiumMagnetic", M, 600, U, 1729);
        row(rows, 8648, "DarkIron", M, 8648, U, 1730);
        row(rows, 8649, "MeteoricIron", M, 8649, U, 1737);
        row(rows, 8650, "MeteoricSteel", M, 8650, U, 1738);
        row(rows, 8651, "SteelGalvanized", M, 8630, U, 1731);
        row(rows, 8652, "TungstenSintered", M, 740, U, 1732);
        row(rows, 8653, "VanadiumSteel", M, 8653, U, 1718);
        row(rows, 8654, "TitaniumGold", M, 8654, U, 1733);
        row(rows, 8657, "PurpleAlloy", M | F, 8657, U, 1746);
        row(rows, 8658, "ElectrotineAlloy", M | F, 8658, U, 1750);
        row(rows, 8659, "BlueAlloy", M | F, 8659, U, 1745);
        row(rows, 8660, "RedAlloy", M | F, 8660, U, 1744);
        row(rows, 8661, "Invar", M, 8661, U, 1754);
        row(rows, 8662, "Constantan", M, 8662, U, 1755);
        row(rows, 8663, "Nichrome", M, 8663, U, 1756);
        row(rows, 8664, "Kanthal", M, 8664, U, 1757);
        row(rows, 8665, "Magnalium", M | F, 8665, U, 1758);
        row(rows, 8666, "Ultimet", M, 8666, U, 1760);
        row(rows, 8667, "TinAlloy", M | F, 8667, U, 1761);
        row(rows, 8668, "BatteryAlloy", M | F, 8668, U, 1762);
        row(rows, 8669, "SolderingAlloy", M | F, 8669, U, 1763);
        row(rows, 8670, "ShadowIron", M, 8670, U, 1674);
        row(rows, 8671, "ShadowSteel", M, 8671, U, 1683);
        row(rows, 8672, "Ironwood", M | F, 8672, U, 1764);
        row(rows, 8673, "Steeleaf", M, 8630, U / 4, 1765);
        row(rows, 8674, "Knightmetal", M, 8674, U, 1766);
        row(rows, 8675, "FierySteel", M, 8675, U, 1767);
        row(rows, 8676, "AstralSilver", M, 8676, U, 1679);
        row(rows, 8677, "Midasium", M, 8677, U, 1680);
        row(rows, 8678, "Mithril", M, 8678, U, 1681);
        row(rows, 8679, "Thaumium", M, 8679, U, 1774);
        row(rows, 8680, "DarkThaumium", M, 8680, U, 1775);
        row(rows, 8681, "VoidMetal", M, 8681, U, 1776);
        row(rows, 8682, "Osmiridium", M, 8682, U, 1777);
        row(rows, 8683, "Sunnarium", M, 8683, U, 1778);
        row(rows, 8684, "Trinaquadalloy", M, 8684, U, 1871);
        row(rows, 8685, "ChromiumDioxide", M, 8685, U, 1779);
        row(rows, 8686, "VanadiumGallium", M, 8686, U, 1780);
        row(rows, 8687, "YttriumBariumCuprate", M, 8687, U, 1781);
        row(rows, 8688, "NiobiumNitride", M, 8688, U, 1782);
        row(rows, 8689, "NiobiumTitanium", M, 8689, U, 1783);
        row(rows, 8690, "MeteoricBlackSteel", M, 8690, U, 1739);
        row(rows, 8691, "MeteoricBlueSteel", M, 8691, U, 1740);
        row(rows, 8692, "MeteoricRedSteel", M, 8692, U, 1741);
        row(rows, 8693, "MeteoflameSteel", M, 8693, U, 1769);
        row(rows, 8694, "MeteoflameBlackSteel", M, 8694, U, 1770);
        row(rows, 8695, "MeteoflameBlueSteel", M, 8695, U, 1771);
        row(rows, 8696, "MeteoflameRedSteel", M, 8696, U, 1772);
        row(rows, 8697, "FlamascusSteel", M, 8697, U, 1773);
        row(rows, 8698, "Fireleaf", M, 8675, U / 4, 1768);
        row(rows, 8700, "AluminiumBrass", M | F, 8700, U, 1784);
        row(rows, 8701, "Aredrite", M, 8701, U, 1664);
        row(rows, 8702, "Alumite", M, 8702, U, 1786);
        row(rows, 8703, "Manyullyn", M, 8703, U, 1787);
        row(rows, 8704, "VibraniumSteel", M, 8704, U, 1788);
        row(rows, 8705, "VibraniumSilver", M, 8705, U, 1789);
        row(rows, 8706, "Vibramantium", M, 8706, U, 1790);
        row(rows, 8707, "Ardite", M, 8707, U, 1785);
        row(rows, 8708, "Signalum", M | F, 8708, U, 1791);
        row(rows, 8709, "Lumium", M | F, 8709, U, 1792);
        row(rows, 8710, "Enderium", M, 8710, U, 1794);
        row(rows, 8711, "ElectrumFlux", M, 8711, U, 1751);
        row(rows, 8713, "GlowstoneRefined", M, 8341, U, 1796);
        row(rows, 8714, "ObsidianRefined", M, 9810, U, 1797);
        row(rows, 8715, "Yellorium", M, 8715, U, 1798);
        row(rows, 8716, "Blutonium", M, 8716, U, 1799);
        row(rows, 8717, "Cyanite", M, 8717, U, 1800);
        row(rows, 8718, "BedrockHSLAAlloy", M, 8718, U, 1803);
        row(rows, 8720, "Manasteel", M, 8720, U, 1820);
        row(rows, 8721, "Terrasteel", M, 8721, U, 1821);
        row(rows, 8722, "ElvenElementium", M, 8722, U, 1822);
        row(rows, 8723, "Ludicrite", M, 8723, U, 1801);
        row(rows, 8724, "Yellorite", M | B, 8715, U / 3, 1802);
        row(rows, 8725, "PulsatingIron", M, 8725, U, 1806);
        row(rows, 8726, "VibrantAlloy", M, 8726, U, 1808);
        row(rows, 8727, "ConductiveIron", M, 8727, U, 1752);
        row(rows, 8728, "EnergeticAlloy", M, 8728, U, 1807);
        row(rows, 8729, "EnderiumBase", M, 8729, U, 1793);
    }

    private static void seeds6(List<Seed> rows) {
        row(rows, 8730, "ElectricalSteel", M, 8730, U, 1809);
        row(rows, 8731, "ObsidianSteel", M, 8731, U, 1805);
        row(rows, 8732, "Soularium", M, 8732, U, 1810);
        row(rows, 8733, "RedstoneAlloy", M, 8733, U, 1748);
        row(rows, 8734, "SpectreIron", M, 8734, U, 1819);
        row(rows, 8735, "GaiaSpirit", M, 8735, U, 1823);
        row(rows, 8736, "Endium", M, 8736, U, 1824);
        row(rows, 8737, "NikolineAlloy", M, 8737, U, 1749);
        row(rows, 8739, "Mauftrium", M, 8739, U, 1825);
        row(rows, 8740, "Elvorium", M, 8740, U, 1826);
        row(rows, 8741, "NiflheimPower", M, 8741, U, 1827);
        row(rows, 8742, "MuspelheimPower", M, 8742, U, 1828);
        row(rows, 8743, "Iffesal", 0, 8743, U, 1829);
        row(rows, 8744, "AncientDebris", M, 8744, U, 1832);
        row(rows, 8745, "Netherite", M, 8745, U, 1833);
        row(rows, 8746, "NetherizedDiamond", M, 8746, U, 1834);
        row(rows, 8747, "Efrine", M, 8747, U, 1835);
        row(rows, 8750, "Desh", M, 8750, U, 1838);
        row(rows, 8751, "Duranium", M, 8751, U, 1840);
        row(rows, 8752, "Tritanium", M, 8752, U, 1841);
        row(rows, 8753, "Dolamide", 0, 8753, U, 1842);
        row(rows, 8754, "Oriharukon", M, 8754, U, 1843);
        row(rows, 8755, "Adamantite", M, 8755, U, 1844);
        row(rows, 8756, "Duralumin", M, 8756, U, 1845);
        row(rows, 8757, "Meteorite", M, 8757, U, 1846);
        row(rows, 8758, "FrozenIron", M, 260, U, 1847);
        row(rows, 8759, "Kreknorite", M, 8759, U, 1848);
        row(rows, 8760, "Alduorite", M, 8760, U, 1659);
        row(rows, 8761, "Infuscolium", M, 8761, U, 1660);
        row(rows, 8762, "Rubracium", M, 8762, U, 1661);
        row(rows, 8763, "Meutoite", M, 8763, U, 1662);
        row(rows, 8764, "Lemurite", M, 8764, U, 1663);
        row(rows, 8765, "Ceruclase", M, 8765, U, 1665);
        row(rows, 8766, "HSLATungstenAlloy", M, 8766, U, 1723);
        row(rows, 8767, "Oureclase", M, 8767, U, 1666);
        row(rows, 8768, "Kalendrite", M, 8768, U, 1667);
        row(rows, 8769, "Orichalcum", M, 8769, U, 1678);
        row(rows, 8770, "Carmot", M, 8770, U, 1668);
        row(rows, 8771, "Sanguinite", M, 8771, U, 1669);
        row(rows, 8772, "Vyroxeres", M, 8772, U, 1670);
        row(rows, 8773, "Eximite", M, 8773, U, 1671);
        row(rows, 8774, "Prometheum", M, 8774, U, 1676);
        row(rows, 8775, "Ignatius", M, 8775, U, 1672);
        row(rows, 8776, "Vulcanite", M, 8776, U, 1677);
        row(rows, 8777, "Inolashite", M, 8777, U, 1684);
        row(rows, 8778, "Haderoth", M, 8778, U, 1685);
        row(rows, 8779, "Celenegil", M, 8779, U, 1682);
        row(rows, 8780, "WorkersAlloy", M, 8780, U, 1839);
        row(rows, 8781, "Desichalkos", M, 8781, U, 1686);
        row(rows, 8782, "Tartarite", M, 8782, U, 1687);
        row(rows, 8783, "Amordrine", M, 8783, U, 1688);
        row(rows, 8784, "Adamantine", M, 8784, U, 1675);
        row(rows, 8785, "Syrmorite", M, 8785, U, 1849);
        row(rows, 8786, "Octine", M, 8786, U, 1850);
        row(rows, 8790, "Trinitanium", M, 8790, U, 1872);
        row(rows, 8791, "Draconium", M, 8791, U, 1861);
        row(rows, 8792, "DraconiumAwakened", M, 8792, U, 1862);
        row(rows, 8793, "TitaniumIridium", M, 8793, U, 1875);
        row(rows, 8794, "TitaniumAluminide", M, 8794, U, 1876);
        row(rows, 8795, "Bedrockium", M, 8795, U, 1858);
        row(rows, 8796, "HSSG", M, 8796, U, 1853);
        row(rows, 8797, "HSSE", M, 8797, U, 1854);
        row(rows, 8798, "HSSS", M, 8798, U, 1855);
        row(rows, 8799, "CrystalMatrix", M, 8799, U, 1865);
        row(rows, 8800, "CosmicNeutronium", M, 8800, U, 1866);
        row(rows, 8801, "Infinity", M, 8801, U, 1867);
        row(rows, 8802, "TantalumHafniumCarbide", M, 8802, U, 1734);
        row(rows, 8803, "CastIron", M, 8803, U, 1726);
        row(rows, 8804, "Mingrade", M | F, 8804, U, 1747);
        row(rows, 8805, "Unstable", M, 8805, U, 1868);
        row(rows, 8806, "ClayCompound", M, 8806, U, 1811);
        row(rows, 8807, "EndSteel", M, 8807, U, 1812);
        row(rows, 8808, "EnergeticSilver", M, 8808, U, 1753);
        row(rows, 8809, "MelodicAlloy", M, 8809, U, 1813);
        row(rows, 8810, "StellarAlloy", M, 8810, U, 1814);
        row(rows, 8811, "VividAlloy", M, 8811, U, 1815);
        row(rows, 8812, "CrystallineAlloy", M, 8812, U, 1816);
        row(rows, 8813, "CrystallinePinkSlime", M, 8813, U, 1817);
        row(rows, 9000, "Pumice", F, 9000, U, 3817);
        row(rows, 9001, "Diatomite", B, 9001, U, 3766);
        row(rows, 9003, "BasalticMineralSand", M, 9003, U, 3705);
        row(rows, 9004, "GraniticMineralSand", M, 9004, U, 3706);
        row(rows, 9005, "GarnetSand", B, 9005, U, 3777);
        row(rows, 9006, "QuartzSand", M | B, 8000, U / 3, 3778);
        row(rows, 9007, "UraniumTetrafluoride", M, 9007, U, 1180);
        row(rows, 9009, "Uranium238Tetrafluoride", M, 9009, U, 1182);
        row(rows, 9011, "Uranium235Tetrafluoride", M, 9011, U, 1184);
        row(rows, 9100, "RareEarth", 0, 9100, U, 1625);
        row(rows, 9101, "Almandine", 0, 9101, 0, 1393);
        row(rows, 9102, "Andradite", 0, 9102, 0, 1397);
        row(rows, 9103, "Asbestos", M | F, 9103, U, 1235);
        row(rows, 9104, "Hematite", M, 9104, U, 1094);
        row(rows, 9105, "Bauxite", B, 9105, U, 3749);
        row(rows, 9106, "BrownLimonite", M, 9104, U / 2, 3740);
        row(rows, 9107, "Calcite", M, 9107, U, 1108);
        row(rows, 9108, "Cassiterite", M | F, 500, 3 * U / 4, 3701);
        row(rows, 9109, "Realgar", M | F, 330, U / 3, 3708);
        row(rows, 9110, "Celestine", M | B, 380, U / 9, 3723);
        row(rows, 9111, "Chalcopyrite", M | F, 290, 2 * U / 9, 3714);
        row(rows, 9112, "Chalk", M | F, 9107, 2 * U / 3, 1233);
    }

    private static void seeds7(List<Seed> rows) {
        row(rows, 9113, "Chromite", M | B, 240, 2 * U / 9, 3750);
        row(rows, 9114, "Cinnabar", M, 800, U / 3, 3709);
        row(rows, 9115, "Cobaltite", M | B, 270, U / 4, 3716);
        row(rows, 9116, "Cooperite", M | B, 780, U / 3, 3718);
        row(rows, 9117, "Galena", M | F, 820, U / 3, 3717);
        row(rows, 9118, "Garnierite", M | B, 280, 3 * U / 4, 3702);
        row(rows, 9119, "Grossular", 0, 9119, 0, 1394);
        row(rows, 9120, "Ilmenite", M, 9120, U, 3748);
        row(rows, 9122, "Magnetite", M, 9122, U, 3704);
        row(rows, 9123, "Molybdenite", M | B, 420, U / 4, 3710);
        row(rows, 9124, "Powellite", M | B, 420, U / 9, 3751);
        row(rows, 9125, "Pyrite", B, 9125, U, 1237);
        row(rows, 9126, "Pyrolusite", M, 250, 3 * U / 4, 1090);
        row(rows, 9127, "Pyrope", 0, 9127, 0, 1395);
        row(rows, 9128, "Scheelite", B, 9128, U, 3725);
        row(rows, 9129, "Spessartine", 0, 9129, 0, 1396);
        row(rows, 9130, "Sphalerite", M | F, 300, U / 3, 3711);
        row(rows, 9131, "Stibnite", M | B, 510, U / 4, 3712);
        row(rows, 9132, "Tetrahedrite", M | F, 290, U / 4, 3719);
        row(rows, 9133, "Tungstate", B, 9133, U, 3729);
        row(rows, 9134, "Uraninite", M | B, 920, U / 3, 3703);
        row(rows, 9135, "Uvarovite", 0, 9135, 0, 1398);
        row(rows, 9136, "Wulfenite", M | B, 420, U / 9, 3752);
        row(rows, 9137, "YellowLimonite", M, 9104, U / 2, 3741);
        row(rows, 9138, "Perlite", B, 9138, U, 3760);
        row(rows, 9139, "Borax", B, 9139, U, 3765);
        row(rows, 9140, "PotassiumFeldspar", 0, 9140, U, 1238);
        row(rows, 9141, "Biotite", 0, 9141, U, 1239);
        row(rows, 9143, "VanadiumMagnetite", M, 9143, U, 3743);
        row(rows, 9144, "Bastnasite", M | B, 580, U / 9, 3753);
        row(rows, 9145, "Pentlandite", M | B, 280, U / 3, 3713);
        row(rows, 9146, "Spodumene", B, 9146, U, 3768);
        row(rows, 9147, "Pollucite", B, 9147, U, 3738);
        row(rows, 9148, "Tantalite", B, 9148, U, 3744);
        row(rows, 9149, "Lepidolite", B, 9149, U, 3769);
        row(rows, 9150, "Glauconite", B, 9150, U, 3770);
        row(rows, 9152, "Vermiculite", B, 9152, U, 3772);
        row(rows, 9153, "Bentonite", M, 8225, U, 1284);
        row(rows, 9154, "Palygorskite", M, 8225, U, 1285);
        row(rows, 9155, "Pitchblende", M | B, 920, U / 5, 3754);
        row(rows, 9156, "Malachite", M | F, 290, U / 6, 3755);
        row(rows, 9157, "Mirabilite", B, 9157, U, 3762);
        row(rows, 9158, "Mica", B, 9158, U, 3773);
        row(rows, 9159, "Trona", B, 9159, U, 3761);
        row(rows, 9160, "Barite", M | B, 560, U / 9, 3722);
        row(rows, 9161, "Gypsum", 0, 9161, U, 1106);
        row(rows, 9162, "Alunite", B, 9162, U, 3775);
        row(rows, 9163, "Dolomite", M | F, 9107, U / 2, 1234);
        row(rows, 9164, "Wollastonite", B, 9164, U, 3735);
        row(rows, 9165, "Zeolite", B, 9165, U, 3737);
        row(rows, 9166, "Kyanite", B, 9166, U, 3774);
        row(rows, 9167, "Kaolinite", M, 8225, U, 1286);
        row(rows, 9169, "Talc", 0, 9169, U, 1236);
        row(rows, 9170, "Gneiss", F, 9170, U, 3826);
        row(rows, 9171, "Greenschist", F, 9171, U, 3822);
        row(rows, 9172, "Greenstone", F, 9172, U, 1642);
        row(rows, 9173, "Greywacke", F, 9173, U, 3835);
        row(rows, 9174, "Graphite", M | B, 60, U / 2, 1292);
        row(rows, 9175, "Graphene", M | B, 60, U / 2, 1535);
        row(rows, 9176, "Gabbro", F, 9176, U, 3818);
        row(rows, 9177, "Komatiite", F, 9177, U, 3816);
        row(rows, 9178, "Siltstone", F, 9178, U, 3836);
        row(rows, 9179, "Rhyolite", F, 9179, U, 3837);
        row(rows, 9180, "Quartzite", M | B | F, 8000, U, 3828);
        row(rows, 9181, "Migmatite", F, 9181, U, 3838);
        row(rows, 9182, "Epidote", F, 9182, U, 1644);
        row(rows, 9183, "Emery", 0, 9183, U, 1240);
        row(rows, 9184, "Blueschist", F, 9184, U, 3823);
        row(rows, 9185, "Bluestone", F, 9185, U, 1643);
        row(rows, 9186, "Chert", F, 9186, U, 3839);
        row(rows, 9187, "Dacite", F, 9187, U, 3840);
        row(rows, 9188, "Andesite", F, 9188, U, 3832);
        row(rows, 9189, "Limestone", M | F, 9107, U / 2, 3821);
        row(rows, 9190, "Shale", F, 9190, U, 3814);
        row(rows, 9191, "Eclogite", F, 9191, U, 3843);
        row(rows, 9192, "Rutile", M, 9192, U, 1086);
        row(rows, 9193, "Stolzite", M | B, 8026, 2 * U / 3, 3731);
        row(rows, 9194, "Ferberite", B, 9194, U, 3727);
        row(rows, 9195, "Huebnerite", B, 9195, U, 3728);
        row(rows, 9196, "Russellite", M | B, 8026, 4 * U / 9, 3732);
        row(rows, 9197, "Pinalite", M | B, 8026, 4 * U / 11, 3733);
        row(rows, 9198, "DiduraniumTrioxide", B, 9198, U, 3780);
        row(rows, 9199, "DuraniumHexafluoride", B, 9199, U, 3781);
        row(rows, 9200, "DuraniumHexachloride", B, 9200, U, 3782);
        row(rows, 9201, "DuraniumHexabromide", B, 9201, U, 3783);
        row(rows, 9202, "DuraniumHexaiodide", B, 9202, U, 3784);
        row(rows, 9203, "DuraniumHexaastatide", B, 9203, U, 3785);
        row(rows, 9204, "TritaniumDioxide", B, 9204, U, 3786);
        row(rows, 9205, "TritaniumHexafluoride", B, 9205, U, 3787);
        row(rows, 9206, "TritaniumHexachloride", B, 9206, U, 3788);
        row(rows, 9207, "TritaniumHexabromide", B, 9207, U, 3789);
        row(rows, 9208, "TritaniumHexaiodide", B, 9208, U, 3790);
        row(rows, 9209, "TritaniumHexaastatide", B, 9209, U, 3791);
        row(rows, 9210, "Bromargyrite", M | B, 470, U / 3, 3756);
        row(rows, 9211, "Smithsonite", M | B, 300, U / 6, 3757);
        row(rows, 9212, "Sperrylite", M | B, 780, U / 4, 3758);
        row(rows, 9213, "Kesterite", M | B, 290, U / 9, 3720);
        row(rows, 9214, "Stannite", M | B, 290, U / 9, 3721);
        row(rows, 9215, "Fluorite", M, 9215, U, 1109);
        row(rows, 9216, "Arsenopyrite", M | B, 330, U / 4, 3715);
    }

    private static void seeds8(List<Seed> rows) {
        row(rows, 9217, "Wolframite", B, 9217, U, 3726);
        row(rows, 9218, "Kimberlite", F, 9218, U, 3827);
        row(rows, 9219, "Prismarine", F, 9219, U, 1639);
        row(rows, 9220, "PrismarineDark", F, 9220, U, 1640);
        row(rows, 9221, "Bischofite", B, 9221, U, 3763);
        row(rows, 9222, "Slate", F, 9222, U, 3841);
        row(rows, 9223, "Blackstone", F, 9223, U, 3834);
        row(rows, 9243, "ClayBrick", M, 8225, U, 1280);
        row(rows, 9244, "Grayschist", F, 9244, U, 3824);
        row(rows, 9245, "Pinkschist", F, 9245, U, 3825);
        row(rows, 9246, "Columbite", B, 9246, U, 3745);
        row(rows, 9247, "Coltan", B, 9247, U, 3746);
        row(rows, 9248, "Deepslate", F, 9248, U, 3842);
        row(rows, 9249, "PhobosRock", F, 9249, U, 3844);
        row(rows, 9250, "DeimosRock", F, 9250, U, 3845);
        row(rows, 9251, "VenusRock", F, 9251, U, 3846);
        row(rows, 9252, "MercuryRock", F, 9252, U, 3847);
        row(rows, 9253, "CeresRock", F, 9253, U, 3848);
        row(rows, 9254, "JupiterRock", F, 9254, U, 3849);
        row(rows, 9255, "IoRock", F, 9255, U, 3850);
        row(rows, 9256, "EuropaRock", F, 9256, U, 3851);
        row(rows, 9257, "GanymedeRock", F, 9257, U, 3852);
        row(rows, 9258, "CallistoRock", F, 9258, U, 3853);
        row(rows, 9259, "SaturnRock", F, 9259, U, 3854);
        row(rows, 9260, "RheaRock", F, 9260, U, 3855);
        row(rows, 9261, "TitanRock", F, 9261, U, 3856);
        row(rows, 9262, "OberonRock", F, 9262, U, 3857);
        row(rows, 9263, "IapetusRock", F, 9263, U, 3858);
        row(rows, 9264, "UranusRock", F, 9264, U, 3859);
        row(rows, 9265, "TitaniaRock", F, 9265, U, 3860);
        row(rows, 9266, "NeptuneRock", F, 9266, U, 3861);
        row(rows, 9267, "TritonRock", F, 9267, U, 3862);
        row(rows, 9268, "PlutoRock", F, 9268, U, 3863);
        row(rows, 9269, "ErisRock", F, 9269, U, 3864);
        row(rows, 9270, "Kepler22bRock", F, 9270, U, 3865);
        row(rows, 9271, "Quicklime", 0, 9271, U, 1107);
        row(rows, 9300, "Oak", M, 8200, U / 4, 3872);
        row(rows, 9301, "Birch", M, 8200, U / 4, 3873);
        row(rows, 9302, "Spruce", M, 8200, U / 4, 3874);
        row(rows, 9303, "Junglewood", M, 8200, U / 4, 3875);
        row(rows, 9304, "Acacia", M, 8200, U / 4, 3876);
        row(rows, 9305, "DarkOak", M, 8200, U / 4, 3877);
        row(rows, 9306, "Crimsonwood", M, 8200, U / 4, 3878);
        row(rows, 9307, "Warpedwood", M, 8200, U / 4, 3879);
        row(rows, 9308, "WoodCompressed", M, 8200, U / 4, 3882);
        row(rows, 9309, "WoodDead", M, 8200, U / 4, 3883);
        row(rows, 9310, "WoodRotten", M, 8200, U / 4, 3884);
        row(rows, 9311, "WoodMossy", M, 8200, U / 4, 3885);
        row(rows, 9312, "WoodFrozen", M, 8200, U / 4, 3886);
        row(rows, 9313, "Maple", M, 8200, U / 4, 3892);
        row(rows, 9314, "Willow", M, 8200, U / 4, 3893);
        row(rows, 9315, "BlueMahoe", M, 8200, U / 4, 3894);
        row(rows, 9316, "Hazel", M, 8200, U / 4, 3895);
        row(rows, 9317, "Cinnamonwood", M, 8200, U / 4, 3896);
        row(rows, 9318, "Coconutwood", M, 8200, U / 4, 3897);
        row(rows, 9319, "Rainbowood", M, 8200, U / 4, 3898);
        row(rows, 9320, "Towerwood", M, 8200, U / 4, 3901);
        row(rows, 9321, "Witchwood", M, 8200, U / 4, 3902);
        row(rows, 9322, "Ogrewood", M, 8200, U / 4, 3903);
        row(rows, 9323, "Wyvernwood", M, 8200, U / 4, 3904);
        row(rows, 9324, "Aspen", M, 8200, U / 4, 3905);
        row(rows, 9325, "DouglasFir", M, 8200, U / 4, 3906);
        row(rows, 9326, "Sycamore", M, 8200, U / 4, 3907);
        row(rows, 9327, "WhiteCedar", M, 8200, U / 4, 3908);
        row(rows, 9328, "WhiteElm", M, 8200, U / 4, 3909);
        row(rows, 9329, "Thorntree", M, 8200, U / 4, 3910);
        row(rows, 9330, "SilverPine", M, 8200, U / 4, 3911);
        row(rows, 9331, "Alder", M, 8200, U / 4, 3912);
        row(rows, 9332, "Hawthorn", M, 8200, U / 4, 3913);
        row(rows, 9333, "Rowan", M, 8200, U / 4, 3914);
        row(rows, 9334, "Autumnwood", M, 8200, U / 4, 3918);
        row(rows, 9335, "Pine", M, 8200, U / 4, 3949);
        row(rows, 9336, "Cypress", M, 8200, U / 4, 3919);
        row(rows, 9337, "Fir", M, 8200, U / 4, 3920);
        row(rows, 9338, "JapaneseMaple", M, 8200, U / 4, 3921);
        row(rows, 9339, "RainbowEucalyptus", M, 8200, U / 4, 3922);
        row(rows, 9340, "Redwood", M, 8200, U / 4, 3923);
        row(rows, 9341, "Sakura", M, 8200, U / 4, 3924);
        row(rows, 9342, "Balsa", M, 8200, U / 4, 3926);
        row(rows, 9343, "Baobab", M, 8200, U / 4, 3927);
        row(rows, 9344, "Cherrywood", M, 8200, U / 4, 3928);
        row(rows, 9345, "Chestnutwood", M, 8200, U / 4, 3929);
        row(rows, 9346, "Citruswood", M, 8200, U / 4, 3930);
        row(rows, 9347, "Cocobolowood", M, 8200, U / 4, 3931);
        row(rows, 9348, "Ebony", M, 8200, U / 4, 3932);
        row(rows, 9349, "Giganteumwood", M, 8200, U / 4, 3933);
        row(rows, 9350, "Greenheart", M, 8200, U / 4, 3934);
        row(rows, 9351, "Ipe", M, 8200, U / 4, 3935);
        row(rows, 9352, "Kapok", M, 8200, U / 4, 3936);
        row(rows, 9353, "Larch", M, 8200, U / 4, 3937);
        row(rows, 9354, "Limewood", M, 8200, U / 4, 3938);
        row(rows, 9355, "Mahoe", M, 8200, U / 4, 3939);
        row(rows, 9356, "Mahogany", M, 8200, U / 4, 3915);
        row(rows, 9357, "Padauk", M, 8200, U / 4, 3940);
        row(rows, 9358, "Palm", M, 8200, U / 4, 3916);
        row(rows, 9359, "Papayawood", M, 8200, U / 4, 3941);
        row(rows, 9360, "Plumwood", M, 8200, U / 4, 3942);
        row(rows, 9361, "Poplar", M, 8200, U / 4, 3943);
        row(rows, 9362, "Sequoia", M, 8200, U / 4, 3944);
        row(rows, 9363, "Teak", M, 8200, U / 4, 3945);
    }

    private static void seeds9(List<Seed> rows) {
        row(rows, 9364, "Walnutwood", M, 8200, U / 4, 3946);
        row(rows, 9365, "Wenge", M, 8200, U / 4, 3947);
        row(rows, 9366, "Zebrawood", M, 8200, U / 4, 3948);
        row(rows, 9367, "Darkwood", M, 8200, U / 4, 3951);
        row(rows, 9368, "Etherealwood", M, 8200, U / 4, 3952);
        row(rows, 9369, "Goldwood", M, 8200, U / 4, 3953);
        row(rows, 9370, "Hellbark", M, 8200, U / 4, 3954);
        row(rows, 9371, "Jacaranda", M, 8200, U / 4, 3955);
        row(rows, 9372, "Mangrove", M, 8200, U / 4, 3956);
        row(rows, 9373, "SacredOak", M, 8200, U / 4, 3957);
        row(rows, 9374, "Magicwood", M, 8200, U / 4, 3958);
        row(rows, 9375, "Applewood", M, 8200, U / 4, 3960);
        row(rows, 9376, "Ashwood", M, 8200, U / 4, 3961);
        row(rows, 9377, "Beech", M, 8200, U / 4, 3962);
        row(rows, 9378, "Boxwood", M, 8200, U / 4, 3963);
        row(rows, 9379, "Brazilwood", M, 8200, U / 4, 3964);
        row(rows, 9380, "Butternutwood", M, 8200, U / 4, 3965);
        row(rows, 9381, "Cedar", M, 8200, U / 4, 3966);
        row(rows, 9382, "Elderwood", M, 8200, U / 4, 3967);
        row(rows, 9383, "Elm", M, 8200, U / 4, 3968);
        row(rows, 9384, "Eucalyptus", M, 8200, U / 4, 3969);
        row(rows, 9385, "Figwood", M, 8200, U / 4, 3970);
        row(rows, 9386, "Gingko", M, 8200, U / 4, 3971);
        row(rows, 9387, "Hemlock", M, 8200, U / 4, 3972);
        row(rows, 9388, "Hickory", M, 8200, U / 4, 3973);
        row(rows, 9389, "Holly", M, 8200, U / 4, 3974);
        row(rows, 9390, "Hornbeam", M, 8200, U / 4, 3975);
        row(rows, 9391, "Iroko", M, 8200, U / 4, 3976);
        row(rows, 9392, "Locust", M, 8200, U / 4, 3977);
        row(rows, 9393, "Logwood", M, 8200, U / 4, 3978);
        row(rows, 9394, "Maclura", M, 8200, U / 4, 3979);
        row(rows, 9395, "Olivewood", M, 8200, U / 4, 3980);
        row(rows, 9396, "Pearwood", M, 8200, U / 4, 3981);
        row(rows, 9397, "PinkIvory", M, 8200, U / 4, 3982);
        row(rows, 9398, "Purpleheart", M, 8200, U / 4, 3983);
        row(rows, 9399, "Rosewood", M, 8200, U / 4, 3984);
        row(rows, 9400, "Sweetgum", M, 8200, U / 4, 3985);
        row(rows, 9401, "Syzgium", M, 8200, U / 4, 3986);
        row(rows, 9402, "Whitebeam", M, 8200, U / 4, 3987);
        row(rows, 9403, "Yew", M, 8200, U / 4, 3988);
        row(rows, 9404, "WoodScorched", M, 8200, U / 4, 3887);
        row(rows, 9405, "WoodVarnished", M, 8200, U / 4, 3888);
        row(rows, 9406, "WoodTainted", M, 8200, U / 4, 3890);
        row(rows, 9407, "WoodBleached", M, 8200, U / 4, 3889);
        row(rows, 9408, "Foxfirewood", M, 8200, U / 4, 3880);
        row(rows, 9409, "BlueSpruce", M, 8200, U / 4, 3899);
        row(rows, 9700, "MeatRaw", M | F, 9701, U, 1318);
        row(rows, 9701, "MeatCooked", M, 9701, U, 1317);
        row(rows, 9702, "Wheat", 0, 9702, U, 1323);
        row(rows, 9703, "Sugar", M | F, 9703, U, 1038);
        row(rows, 9704, "Barley", 0, 9704, U, 1324);
        row(rows, 9705, "Rye", 0, 9705, U, 1325);
        row(rows, 9706, "Rice", 0, 9706, U, 1326);
        row(rows, 9707, "Oat", 0, 9707, U, 1327);
        row(rows, 9708, "Corn", 0, 9708, U, 1329);
        row(rows, 9709, "Potato", 0, 9709, U, 1330);
        row(rows, 9710, "MeatRotten", M, 9710, U, 1319);
        row(rows, 9711, "FishCooked", M, 9711, U, 1320);
        row(rows, 9712, "FishRaw", M | F, 9711, U, 1321);
        row(rows, 9713, "FishRotten", M, 9713, U, 1322);
        row(rows, 9714, "Almond", 0, 9714, U, 1343);
        row(rows, 9715, "Marshmallow", 0, 9715, U, 1257);
        row(rows, 9716, "PEZ", 0, 9716, U, 1344);
        row(rows, 9717, "Licorice", 0, 9717, U, 1345);
        row(rows, 9718, "Nougat", 0, 9718, U, 1346);
        row(rows, 9719, "AbyssalOat", 0, 9719, U, 1328);
        row(rows, 9778, "Tofu", M, 9778, U, 1331);
        row(rows, 9779, "SoylentGreen", M, 9779, U, 1332);
        row(rows, 9780, "Cheese", M | F, 9780, U, 1333);
        row(rows, 9781, "Chili", 0, 9781, U, 1334);
        row(rows, 9782, "Cocoa", 0, 9782, U, 1335);
        row(rows, 9783, "Chocolate", M | F, 9783, U, 1336);
        row(rows, 9784, "Coffee", 0, 9784, U, 1337);
        row(rows, 9785, "Cinnamon", 0, 9785, U, 1338);
        row(rows, 9786, "Nutmeg", 0, 9786, U, 1339);
        row(rows, 9787, "Vanilla", 0, 9787, U, 1039);
        row(rows, 9788, "PepperBlack", 0, 9788, U, 1347);
        row(rows, 9789, "Curry", 0, 9789, U, 1348);
        row(rows, 9790, "Milk", 0, 9790, U, 1349);
        row(rows, 9791, "Honey", 0, 9791, U, 1352);
        row(rows, 9792, "Tea", 0, 9792, U, 1354);
        row(rows, 9793, "Honeydew", 0, 9793, U, 1353);
        row(rows, 9794, "Mint", 0, 9794, U, 1355);
        row(rows, 9795, "Peanut", 0, 9795, U, 1340);
        row(rows, 9796, "Hazelnut", 0, 9796, U, 1341);
        row(rows, 9797, "Pistachio", 0, 9797, U, 1342);
        row(rows, 9798, "Butter", M, 9798, U, 1350);
        row(rows, 9799, "SaltedButter", M, 9799, U, 1351);
        row(rows, 9800, "Water", M, 9800, U, 1007);
        row(rows, 9801, "Snow", M, 9800, U, 1012);
        row(rows, 9802, "Ice", M, 9800, U, 1013);
        row(rows, 9810, "Lava", M, 9810, U, 1194);
        row(rows, 9820, "NitroCarbon", 0, 9820, U, 1191);
        row(rows, 9822, "SodiumPersulfate", 0, 9822, U, 1134);
        row(rows, 9823, "SodiumSulfide", 0, 9823, U, 1135);
        row(rows, 9824, "SulfuricAcid", 0, 9824, U, 1047);
        row(rows, 9825, "NitricAcid", 0, 9825, U, 1031);
        row(rows, 9827, "AquaRegia", 0, 9827, U, 1188);
        row(rows, 9833, "MethaneIce", 0, 9833, U, 1190);
        row(rows, 9844, "DisulfuricAcid", 0, 9844, U, 1048);
    }

    private static void seeds10(List<Seed> rows) {
        row(rows, 9851, "OilSand", 0, 9851, U, 1199);
        row(rows, 9852, "CrudeOil", 0, 9852, U, 1200);
        row(rows, 9853, "OilShale", 0, 9853, U, 1646);
        row(rows, 9883, "ConstructionFoam", M, 9883, U, 1222);
    }

    private static Map<Integer, Binding> bindings() {
        Map<Integer, Binding> result = new LinkedHashMap<>();
        moltenBindings(result);
        explicitBindings(result);
        return result;
    }

    private static void bind(
            Map<Integer, Binding> bindings,
            int id,
            String path,
            Accessor accessor,
            long mb,
            long referenceUnits,
            int sourceLine) {
        if (bindings.put(id, new Binding(path, accessor, mb, referenceUnits, sourceLine)) != null)
            throw new IllegalStateException("Duplicate source phase " + id);
    }
    // Final generic createMolten bindings, Loader_Fluids659; eligibility never follows from this list alone.
    private static void moltenBindings(Map<Integer, Binding> result) {
        bind(result, 30, "molten.lithium", Accessor.LIQUID, 144, U, 659);
        bind(result, 31, "molten.lithium6", Accessor.LIQUID, 144, U, 659);
        bind(result, 40, "molten.beryllium", Accessor.LIQUID, 144, U, 659);
        bind(result, 41, "molten.beryllium7", Accessor.LIQUID, 144, U, 659);
        bind(result, 42, "molten.beryllium8", Accessor.LIQUID, 144, U, 659);
        bind(result, 50, "molten.boron", Accessor.LIQUID, 144, U, 659);
        bind(result, 51, "molten.boron11", Accessor.LIQUID, 144, U, 659);
        bind(result, 60, "molten.carbon", Accessor.LIQUID, 144, U, 659);
        bind(result, 61, "molten.carbon13", Accessor.LIQUID, 144, U, 659);
        bind(result, 62, "molten.carbon14", Accessor.LIQUID, 144, U, 659);
        bind(result, 110, "molten.sodium", Accessor.LIQUID, 144, U, 659);
        bind(result, 120, "molten.magnesium", Accessor.LIQUID, 144, U, 659);
        bind(result, 130, "molten.aluminium", Accessor.LIQUID, 144, U, 659);
        bind(result, 140, "molten.silicon", Accessor.LIQUID, 144, U, 659);
        bind(result, 160, "molten.sulfur", Accessor.LIQUID, 144, U, 659);
        bind(result, 190, "molten.potassium", Accessor.LIQUID, 144, U, 659);
        bind(result, 200, "molten.calcium", Accessor.LIQUID, 144, U, 659);
        bind(result, 220, "molten.titanium", Accessor.LIQUID, 144, U, 659);
        bind(result, 230, "molten.vanadium", Accessor.LIQUID, 144, U, 659);
        bind(result, 240, "molten.chromium", Accessor.LIQUID, 144, U, 659);
        bind(result, 250, "molten.manganese", Accessor.LIQUID, 144, U, 659);
        bind(result, 260, "molten.iron", Accessor.LIQUID, 144, U, 659);
        bind(result, 270, "molten.cobalt", Accessor.LIQUID, 144, U, 659);
        bind(result, 280, "molten.nickel", Accessor.LIQUID, 144, U, 659);
        bind(result, 290, "molten.copper", Accessor.LIQUID, 144, U, 659);
        bind(result, 300, "molten.zinc", Accessor.LIQUID, 144, U, 659);
        bind(result, 320, "molten.germanium", Accessor.LIQUID, 144, U, 659);
        bind(result, 330, "molten.arsenic", Accessor.LIQUID, 144, U, 659);
        bind(result, 370, "molten.rubidium", Accessor.LIQUID, 144, U, 659);
        bind(result, 380, "molten.strontium", Accessor.LIQUID, 144, U, 659);
        bind(result, 420, "molten.molybdenum", Accessor.LIQUID, 144, U, 659);
        bind(result, 470, "molten.silver", Accessor.LIQUID, 144, U, 659);
        bind(result, 500, "molten.tin", Accessor.LIQUID, 144, U, 659);
        bind(result, 510, "molten.antimony", Accessor.LIQUID, 144, U, 659);
        bind(result, 520, "molten.tellurium", Accessor.LIQUID, 144, U, 659);
        bind(result, 550, "molten.caesium", Accessor.LIQUID, 144, U, 659);
        bind(result, 560, "molten.barium", Accessor.LIQUID, 144, U, 659);
        bind(result, 600, "molten.neodymium", Accessor.LIQUID, 144, U, 659);
        bind(result, 740, "molten.tungsten", Accessor.LIQUID, 144, U, 659);
        bind(result, 760, "molten.osmiumelemental", Accessor.LIQUID, 144, U, 659);
        bind(result, 770, "molten.iridium", Accessor.LIQUID, 144, U, 659);
        bind(result, 780, "molten.platinum", Accessor.LIQUID, 144, U, 659);
        bind(result, 790, "molten.gold", Accessor.LIQUID, 144, U, 659);
        bind(result, 791, "molten.gold198", Accessor.LIQUID, 144, U, 659);
        bind(result, 820, "molten.lead", Accessor.LIQUID, 144, U, 659);
        bind(result, 830, "molten.bismuth", Accessor.LIQUID, 144, U, 659);
        bind(result, 850, "molten.astatine", Accessor.LIQUID, 144, U, 659);
        bind(result, 870, "molten.francium", Accessor.LIQUID, 144, U, 659);
        bind(result, 880, "molten.radium", Accessor.LIQUID, 144, U, 659);
        bind(result, 920, "molten.uranium", Accessor.LIQUID, 144, U, 659);
        bind(result, 921, "molten.uranium235", Accessor.LIQUID, 144, U, 659);
        bind(result, 1250, "molten.tritaniumelemental", Accessor.LIQUID, 144, U, 659);
        bind(result, 1260, "molten.trinium", Accessor.LIQUID, 144, U, 659);
        bind(result, 1450, "molten.duraniumelemental", Accessor.LIQUID, 144, U, 659);
        bind(result, 1740, "molten.naquadah", Accessor.LIQUID, 144, U, 659);
        bind(result, 1741, "molten.naquadahenriched", Accessor.LIQUID, 144, U, 659);
        bind(result, 1742, "molten.naquadria", Accessor.LIQUID, 144, U, 659);
        bind(result, 1850, "molten.abyssalnite", Accessor.LIQUID, 144, U, 659);
        bind(result, 1860, "molten.coralium", Accessor.LIQUID, 144, U, 659);
        bind(result, 1870, "molten.dreadium", Accessor.LIQUID, 144, U, 659);
        bind(result, 1880, "molten.ethaxium", Accessor.LIQUID, 144, U, 659);
        bind(result, 2210, "molten.atlarus", Accessor.LIQUID, 144, U, 659);
        bind(result, 2220, "molten.adamantium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8009, "molten.cryolite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8010, "molten.aluminiumfluoride", Accessor.LIQUID, 144, U, 659);
        bind(result, 8013, "molten.sodiumcarbonate", Accessor.LIQUID, 144, U, 659);
        bind(result, 8016, "molten.magnesiumcarbonate", Accessor.LIQUID, 144, U, 659);
        bind(result, 8018, "molten.magnesiumchloride", Accessor.LIQUID, 144, U, 659);
        bind(result, 8020, "molten.potassiumcarbonate", Accessor.LIQUID, 144, U, 659);
        bind(result, 8026, "molten.tungstentrioxide", Accessor.LIQUID, 144, U, 659);
        bind(result, 8028, "molten.calciumchloride", Accessor.LIQUID, 144, U, 659);
        bind(result, 8029, "molten.lithiumchloride", Accessor.LIQUID, 144, U, 659);
        bind(result, 8030, "molten.ferrouschloride", Accessor.LIQUID, 144, U, 659);
        bind(result, 8031, "molten.manganesechloride", Accessor.LIQUID, 144, U, 659);
        bind(result, 8033, "molten.lithiumchlorate", Accessor.LIQUID, 144, U, 659);
        bind(result, 8034, "molten.lithiumperchlorate", Accessor.LIQUID, 144, U, 659);
        bind(result, 8244, "molten.tallow", Accessor.LIQUID, 144, U, 659);
        bind(result, 8266, "molten.asphalt", Accessor.LIQUID, 144, U, 659);
        bind(result, 8340, "molten.nikolite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8341, "molten.glowstone", Accessor.LIQUID, 144, U, 659);
        bind(result, 8368, "molten.glowstoneceres", Accessor.LIQUID, 144, U, 659);
        bind(result, 8369, "molten.glowstoneio", Accessor.LIQUID, 144, U, 659);
        bind(result, 8370, "molten.glowstoneenceladus", Accessor.LIQUID, 144, U, 659);
        bind(result, 8371, "molten.glowstoneproteus", Accessor.LIQUID, 144, U, 659);
        bind(result, 8372, "molten.glowstonepluto", Accessor.LIQUID, 144, U, 659);
        bind(result, 8456, "molten.gloomstone", Accessor.LIQUID, 144, U, 659);
        bind(result, 8500, "molten.stone", Accessor.LIQUID, 144, U, 659);
        bind(result, 8512, "molten.spacestone", Accessor.LIQUID, 144, U, 659);
        bind(result, 8513, "molten.moonstone", Accessor.LIQUID, 144, U, 659);
        bind(result, 8515, "molten.marsstone", Accessor.LIQUID, 144, U, 659);
        bind(result, 8600, "molten.electrum", Accessor.LIQUID, 144, U, 659);
        bind(result, 8601, "molten.sterlingsilver", Accessor.LIQUID, 144, U, 659);
        bind(result, 8602, "molten.rosegold", Accessor.LIQUID, 144, U, 659);
        bind(result, 8603, "molten.angmallen", Accessor.LIQUID, 144, U, 659);
        bind(result, 8604, "molten.goldinductive", Accessor.LIQUID, 144, U, 659);
        bind(result, 8610, "molten.bronze", Accessor.LIQUID, 144, U, 659);
        bind(result, 8611, "molten.blackbronze", Accessor.LIQUID, 144, U, 659);
        bind(result, 8612, "molten.bismuthbronze", Accessor.LIQUID, 144, U, 659);
        bind(result, 8613, "molten.hepatizon", Accessor.LIQUID, 144, U, 659);
        bind(result, 8614, "molten.arseniccopper", Accessor.LIQUID, 144, U, 659);
        bind(result, 8615, "molten.arsenicbronze", Accessor.LIQUID, 144, U, 659);
        bind(result, 8620, "molten.brass", Accessor.LIQUID, 144, U, 659);
        bind(result, 8621, "molten.cobaltbrass", Accessor.LIQUID, 144, U, 659);
        bind(result, 8630, "molten.steel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8631, "molten.blacksteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8632, "molten.bluesteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8633, "molten.redsteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8634, "molten.damascussteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8635, "molten.tungstensteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8636, "molten.stainlesssteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8640, "molten.annealedcopper", Accessor.LIQUID, 144, U, 659);
        bind(result, 8642, "molten.pigiron", Accessor.LIQUID, 144, U, 659);
        bind(result, 8643, "molten.wroughtiron", Accessor.LIQUID, 144, U, 659);
        bind(result, 8649, "molten.meteoriciron", Accessor.LIQUID, 144, U, 659);
        bind(result, 8650, "molten.meteoricsteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8653, "molten.vanadiumsteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8654, "molten.titaniumgold", Accessor.LIQUID, 144, U, 659);
        bind(result, 8657, "molten.purplealloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8658, "molten.electrotinealloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8659, "molten.bluealloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8660, "molten.redalloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8661, "molten.invar", Accessor.LIQUID, 144, U, 659);
        bind(result, 8662, "molten.constantan", Accessor.LIQUID, 144, U, 659);
        bind(result, 8663, "molten.nichrome", Accessor.LIQUID, 144, U, 659);
        bind(result, 8664, "molten.kanthal", Accessor.LIQUID, 144, U, 659);
        bind(result, 8665, "molten.magnalium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8666, "molten.ultimet", Accessor.LIQUID, 144, U, 659);
        bind(result, 8667, "molten.tinalloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8668, "molten.batteryalloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8669, "molten.solderingalloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8672, "molten.ironwood", Accessor.LIQUID, 144, U, 659);
        bind(result, 8674, "molten.knightmetal", Accessor.LIQUID, 144, U, 659);
        bind(result, 8675, "molten.fierysteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8676, "molten.astralsilver", Accessor.LIQUID, 144, U, 659);
        bind(result, 8677, "molten.midasium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8678, "molten.mithril", Accessor.LIQUID, 144, U, 659);
        bind(result, 8679, "molten.thaumium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8680, "molten.darkthaumium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8681, "molten.voidmetal", Accessor.LIQUID, 144, U, 659);
        bind(result, 8682, "molten.osmiridium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8690, "molten.meteoricblacksteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8691, "molten.meteoricbluesteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8692, "molten.meteoricredsteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8693, "molten.meteoflamesteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8694, "molten.meteoflameblacksteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8695, "molten.meteoflamebluesteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8696, "molten.meteoflameredsteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8697, "molten.flamascussteel", Accessor.LIQUID, 144, U, 659);
        bind(result, 8700, "molten.aluminiumbrass", Accessor.LIQUID, 144, U, 659);
        bind(result, 8702, "molten.alumite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8703, "molten.manyullyn", Accessor.LIQUID, 144, U, 659);
        bind(result, 8707, "molten.ardite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8708, "molten.signalum", Accessor.LIQUID, 144, U, 659);
        bind(result, 8709, "molten.lumium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8710, "molten.enderium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8729, "molten.enderiumbase", Accessor.LIQUID, 144, U, 659);
        bind(result, 8733, "molten.redstonealloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8734, "molten.spectreiron", Accessor.LIQUID, 144, U, 659);
        bind(result, 8737, "molten.nikolinealloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8744, "molten.ancientdebris", Accessor.LIQUID, 144, U, 659);
        bind(result, 8745, "molten.netherite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8747, "molten.efrine", Accessor.LIQUID, 144, U, 659);
        bind(result, 8750, "molten.desh", Accessor.LIQUID, 144, U, 659);
        bind(result, 8757, "molten.meteorite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8759, "molten.kreknorite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8769, "molten.orichalcum", Accessor.LIQUID, 144, U, 659);
        bind(result, 8779, "molten.celenegil", Accessor.LIQUID, 144, U, 659);
        bind(result, 8780, "molten.workersalloy", Accessor.LIQUID, 144, U, 659);
        bind(result, 8785, "molten.syrmorite", Accessor.LIQUID, 144, U, 659);
        bind(result, 8786, "molten.octine", Accessor.LIQUID, 144, U, 659);
        bind(result, 8791, "molten.draconium", Accessor.LIQUID, 144, U, 659);
        bind(result, 8792, "molten.draconiumawakened", Accessor.LIQUID, 144, U, 659);
        bind(result, 8796, "molten.hssg", Accessor.LIQUID, 144, U, 659);
        bind(result, 8797, "molten.hsse", Accessor.LIQUID, 144, U, 659);
        bind(result, 8798, "molten.hsss", Accessor.LIQUID, 144, U, 659);
        bind(result, 9007, "molten.uraniumtetrafluoride", Accessor.LIQUID, 144, U, 659);
        bind(result, 9009, "molten.uranium238tetrafluoride", Accessor.LIQUID, 144, U, 659);
        bind(result, 9011, "molten.uranium235tetrafluoride", Accessor.LIQUID, 144, U, 659);
        bind(result, 9104, "molten.hematite", Accessor.LIQUID, 144, U, 659);
        bind(result, 9107, "molten.calcite", Accessor.LIQUID, 144, U, 659);
        bind(result, 9120, "molten.ilmenite", Accessor.LIQUID, 144, U, 659);
        bind(result, 9143, "molten.vanadiummagnetite", Accessor.LIQUID, 144, U, 659);
        bind(result, 9192, "molten.rutile", Accessor.LIQUID, 144, U, 659);
        bind(result, 9215, "molten.fluorite", Accessor.LIQUID, 144, U, 659);
    }
    // Literal bindings or final LIQUID generation. MT1007/1194 are the Water/Lava declaration bindings.
    private static void explicitBindings(Map<Integer, Binding> result) {
        bind(result, 80, "liquidoxygen", Accessor.LIQUID, 1000, U, 68);
        bind(result, 350, "bromine", Accessor.LIQUID, 1000, U, 658);
        bind(result, 800, "mercury", Accessor.LIQUID, 144, U, 618);
        bind(result, 2390, "mcguffium", Accessor.LIQUID, 144, U, 105);
        bind(result, 8001, "glass", Accessor.LIQUID, 144, U, 192);
        bind(result, 8008, "molten.alumina", Accessor.LIQUID, 504, U, 214);
        bind(result, 8011, "hexafluorosilicicacid", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8211, "blaze", Accessor.LIQUID, 1296, U, 195);
        bind(result, 8213, "cryotheum", Accessor.LIQUID, 250, U, 131);
        bind(result, 8217, "molten.rubber", Accessor.LIQUID, 144, U, 204);
        bind(result, 8218, "plastic", Accessor.LIQUID, 1000, U, 191);
        bind(result, 8235, "molten.wax", Accessor.LIQUID, 144, U, 205);
        bind(result, 8236, "molten.waxbee", Accessor.LIQUID, 144, U, 206);
        bind(result, 8237, "molten.waxrefractory", Accessor.LIQUID, 144, U, 209);
        bind(result, 8238, "molten.waxparaffin", Accessor.LIQUID, 144, U, 207);
        bind(result, 8239, "molten.waxplant", Accessor.LIQUID, 144, U, 208);
        bind(result, 8240, "molten.waxmagic", Accessor.LIQUID, 144, U, 210);
        bind(result, 8246, "aerotheum", Accessor.GAS, 250, U, 133);
        bind(result, 8280, "molten.waxamnesic", Accessor.LIQUID, 144, U, 211);
        bind(result, 8281, "molten.waxsoulful", Accessor.LIQUID, 144, U, 212);
        bind(result, 8318, "molten.enderpearl", Accessor.LIQUID, 144, U, 193);
        bind(result, 8333, "molten.redstone", Accessor.LIQUID, 144, U, 194);
        bind(result, 8373, "ectoplasm", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8400, "chloroauricacid", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8401, "chloroplatinicacid", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8402, "stannicchloride", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8403, "blackvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8404, "bluevitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8405, "greenvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8406, "redvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8407, "pinkvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8408, "cyanvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8409, "whitevitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8410, "grayvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8411, "martianvitriol", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8412, "vitriolofclay", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8413, "titaniumtetrachloride", Accessor.LIQUID, 1000, U, 658);
        bind(result, 8501, "concrete", Accessor.LIQUID, 144, U, 196);
        bind(result, 8637, "molten hsla", Accessor.LIQUID, 144, U, 199);
        bind(result, 9703, "molten.sugar", Accessor.LIQUID, 144, U, 203);
        bind(result, 9780, "molten.cheese", Accessor.LIQUID, 144, U, 202);
        bind(result, 9783, "molten.chocolate", Accessor.LIQUID, 144, U, 201);
        bind(result, 9790, "milk", Accessor.LIQUID, 1000, U, 624);
        bind(result, 9791, "for.honey", Accessor.LIQUID, 1000, U, 574);
        bind(result, 9793, "honeydew", Accessor.LIQUID, 1000, U, 575);
        bind(result, 9800, "water", Accessor.LIQUID, 1000, U, 1007);
        bind(result, 9802, "ice", Accessor.LIQUID, 1000, U, 364);
        bind(result, 9810, "lava", Accessor.LIQUID, 1000, 9 * U, 1194);
        bind(result, 9824, "sulfuricacid", Accessor.LIQUID, 1000, U, 658);
        bind(result, 9825, "nitricacid", Accessor.LIQUID, 1000, U, 658);
        bind(result, 9827, "aquaregia", Accessor.LIQUID, 1000, U, 658);
        bind(result, 9844, "disulfuricacid", Accessor.LIQUID, 1000, U, 658);
        bind(result, 9883, "ic2constructionfoam", Accessor.LIQUID, 100, U, 74);
    }
}
