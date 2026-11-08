package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.stone.StoneType;
import gregtech.content.material.generated.CompoundMaterials;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared bounded default classifications for Lathe, Cutter and forming, from GT6 MT/TD/OP at 3703e403.
 * Factory and direct tag closure includes 1,070 matched identities, not unknown native additions.
 * COATED/LAYERED do not propagate through qual/steal or material re-registration.
 * Lens means OP.lens.canGenerateItem (forced OR LENSES), not holder existence; pinned defaults
 * have no forced lens additions. This does not mutate global workability or item generation.
 */
public final class SourceLatheMaterialProfiles {
    private SourceLatheMaterialProfiles() {}

    public record Profile(boolean coated, boolean layered, boolean easy, boolean lens, boolean smithable) {}

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
        for (String line : SOURCE.strip().split("\n")) {
            String[] fields = line.strip().split("\\|");
            int id = Integer.parseInt(fields[0]);
            GTMaterial material = GTMaterialRegistry.get(fields[1]);
            if (material == null
                    || material.getId() != id
                    || !material.getName().equals(fields[1])
                    || material.resolve() != material
                    || AddonMaterials.owns(material)
                    || id > 0 && GTMaterialRegistry.get(id) != material) continue;
            String f = fields[2];
            DEFAULTS.put(
                    material,
                    new Captured(
                            id,
                            fields[1],
                            eligibility(material),
                            cutterEligibility(material),
                            formingEligibility(material),
                            rollingWireEligibility(material),
                            new Profile(
                                    f.charAt(0) == '1',
                                    f.charAt(1) == '1',
                                    f.charAt(2) == '1',
                                    f.charAt(3) == '1',
                                    SOURCE_SMITHABLE.contains(id))));
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

    // Complete SMITHABLE closure of the same 1,070 MT identities at 3703e403:
    // 309 positives; absence means false ONLY inside the captured source table, never for addons.
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

    // id | canonical native name | COATED,LAYERED,FURNACE-or-SOFT,lens-generation
    private static final String SOURCE = """
            0|Empty|0000
            1|Photon|0000
            2|Neutrino|0000
            3|Neutron|0000
            4|Proton|0000
            5|Electron|0000
            10|Hydrogen|0000
            11|Deuterium|0000
            12|Tritium|0000
            20|Helium|0000
            21|Helium3|0000
            30|Lithium|0010
            31|Lithium6|0010
            40|Beryllium|0000
            41|Beryllium7|0000
            42|Beryllium8|0000
            50|Boron|0000
            51|Boron11|0000
            60|Carbon|0000
            61|Carbon13|0000
            62|Carbon14|0000
            70|Nitrogen|0000
            80|Oxygen|0000
            90|Fluorine|0000
            100|Neon|0000
            110|Sodium|0010
            120|Magnesium|0010
            130|Aluminium|0010
            140|Silicon|0000
            150|Phosphor|0000
            160|Sulfur|0000
            170|Chlorine|0000
            180|Argon|0000
            190|Potassium|0010
            200|Calcium|0010
            210|Scandium|0000
            220|Titanium|0000
            230|Vanadium|0000
            240|Chromium|0000
            250|Manganese|0000
            260|Iron|0000
            270|Cobalt|0000
            278|Cobalt60|0000
            280|Nickel|0000
            290|Copper|0010
            300|Zinc|0010
            310|Gallium|0010
            320|Germanium|0010
            330|Arsenic|0010
            340|Selenium|0000
            350|Bromine|0000
            360|Krypton|0000
            370|Rubidium|0010
            380|Strontium|0010
            390|Yttrium|0000
            400|Zirconium|0001
            410|Niobium|0000
            420|Molybdenum|0000
            430|Technetium|0000
            440|Ruthenium|0000
            450|Rhodium|0000
            460|Palladium|0000
            470|Silver|0010
            480|Cadmium|0010
            490|Indium|0010
            500|Tin|0010
            510|Antimony|0010
            520|Tellurium|0010
            530|Iodine|0000
            540|Xenon|0000
            550|Caesium|0010
            560|Barium|0010
            570|Lanthanium|0010
            580|Cerium|0010
            590|Praseodymium|0000
            600|Neodymium|0000
            610|Promethium|0000
            620|Samarium|0000
            630|Europium|0010
            640|Gadolinium|0000
            650|Terbium|0000
            660|Dysprosium|0000
            670|Holmium|0000
            680|Erbium|0000
            690|Thulium|0000
            700|Ytterbium|0010
            710|Lutetium|0000
            720|Hafnium|0000
            730|Tantalum|0000
            740|Tungsten|0000
            750|Rhenium|0000
            760|OsmiumElemental|0000
            770|Iridium|0000
            780|Platinum|0000
            790|Gold|0010
            791|Gold198|0010
            800|Mercury|0010
            810|Thallium|0010
            820|Lead|0010
            830|Bismuth|0010
            840|Polonium|0010
            850|Astatine|0010
            860|Radon|0000
            870|Francium|0010
            880|Radium|0010
            890|Actinium|0000
            900|Thorium|0000
            910|Protactinium|0000
            920|Uranium|0000
            921|Uranium235|0000
            922|Uranium233|0000
            930|Neptunium|0010
            940|Plutonium|0010
            942|Plutonium240|0010
            943|Plutonium241|0010
            945|Plutonium243|0010
            946|Plutonium238|0010
            947|Plutonium239|0010
            950|Americium|0000
            951|Americium241|0000
            952|Americium242|0000
            960|Curium|0000
            970|Berkelium|0000
            980|Californium|0010
            990|Einsteinium|0010
            1000|Fermium|0010
            1010|Mendelevium|0010
            1020|Nobelium|0010
            1030|Lawrencium|0000
            1040|Rutherfordium|0000
            1050|Dubnium|0010
            1060|Seaborgium|0010
            1070|Bohrium|0010
            1080|Hassium|0010
            1090|Meitnerium|0000
            1100|Darmstadtium|0000
            1110|Roentgenium|0000
            1120|Copernicium|0010
            1130|Nihonium|0000
            1140|Flerovium|0010
            1148|Flerovium298|0010
            1150|Moscovium|0000
            1160|Livermorium|0000
            1170|Farnsium|0000
            1180|Oganesson|0000
            1250|TritaniumElemental|0000
            1260|Trinium|0000
            1450|DuraniumElemental|0000
            1520|Vibranium|0001
            1740|Naquadah|0000
            1741|NaquadahEnriched|0000
            1742|Naquadria|0000
            2220|Adamantium|0000
            3720|Gravitonium|0000
            4000|Magic|0000
            8000|SiliconDioxide|0010
            8001|Glass|0011
            8002|Flint|0000
            8003|Carborundum|0000
            8006|Datolite|0000
            8007|HydrogenBorate|0000
            8008|Alumina|0000
            8009|Cryolite|0000
            8010|AluminiumFluoride|0000
            8011|HexafluorosilicicAcid|0000
            8012|SodiumAluminate|0000
            8013|SodiumCarbonate|0000
            8014|AluminiumHydroxide|0000
            8015|PotassiumHydroxide|0000
            8016|MagnesiumCarbonate|0000
            8017|FerricChloride|0000
            8018|MagnesiumChloride|0000
            8019|SodiumNitrate|0000
            8020|PotassiumCarbonate|0000
            8021|PotassiumSulfite|0000
            8022|PotassiumPersulfate|0000
            8023|PotassiumAluminate|0000
            8024|HydrosulfuricAcid|0000
            8025|Ammonia|0000
            8026|TungstenTrioxide|0000
            8027|TungsticAcid|0000
            8028|CalciumChloride|0000
            8029|LithiumChloride|0000
            8030|FerrousChloride|0000
            8031|ManganeseChloride|0000
            8032|LithiumHydroxide|0000
            8033|LithiumChlorate|0000
            8034|LithiumPerchlorate|0000
            8035|FerricOxyhydroxide|0000
            8036|PotassiumFluoride|0000
            8037|SodiumFluoride|0000
            8038|PotassiumHeptafluorotantalate|0000
            8039|SodiumHydrogencarbonate|0000
            8100|Sand|0010
            8101|Soulsand|0000
            8102|SluiceSand|0000
            8103|PlatinumGroupSludge|0000
            8104|RedSand|0010
            8196|Teflon|0010
            8197|PVC|0010
            8200|Ashes|0000
            8201|DarkAshes|0000
            8203|Sylvite|0000
            8204|Salt|0000
            8205|PotassiumNitrate|0000
            8206|Niter|0000
            8207|Phosphate|0000
            8208|Phosphorus|0000
            8209|Apatite|0000
            8210|Blizz|0000
            8211|Blaze|0000
            8214|Obsidian|0000
            8215|Clay|0000
            8216|Paper|0000
            8217|Rubber|0010
            8218|Plastic|0010
            8219|Bone|0000
            8220|Gunpowder|0000
            8221|Wood|0000
            8222|WoodTreated|1000
            8223|LiveRoot|0000
            8224|WoodRubber|0000
            8225|Ceramic|0000
            8226|Phosphorite|0000
            8229|CobaltHexahydrate|0000
            8230|SodiumBisulfate|0000
            8231|SodiumPyrosulfate|0000
            8232|PotassiumBisulfate|0000
            8233|PotassiumPyrosulfate|0000
            8234|VanadiumPentoxide|0000
            8235|Wax|0010
            8236|WaxBee|0010
            8238|WaxParaffin|0010
            8239|WaxPlant|0010
            8241|Leather|0000
            8242|IodineSalt|0000
            8247|Basalz|0000
            8248|Blitz|0000
            8249|Dynamite|0000
            8250|DyeBlack|0000
            8251|DyeRed|0000
            8252|DyeGreen|0000
            8253|DyeBrown|0000
            8254|DyeBlue|0000
            8255|DyePurple|0000
            8256|DyeCyan|0000
            8257|DyeLightGray|0000
            8258|DyeGray|0000
            8259|DyePink|0000
            8260|DyeLime|0000
            8261|DyeYellow|0000
            8262|DyeLightBlue|0000
            8263|DyeMagenta|0000
            8264|DyeOrange|0000
            8265|DyeWhite|0000
            8266|Asphalt|0010
            8267|WoodPolished|1000
            8268|SodiumHydroxide|0000
            8269|SodiumSulfite|0000
            8270|SodiumSulfate|0000
            8271|PotassiumSulfate|0000
            8272|PotassiumSulfide|0000
            8273|Porcelain|0000
            8274|CalciumSulfate|0000
            8275|Bark|0000
            8276|ClayBrown|0000
            8277|PetrifiedWood|0000
            8278|ManaDiamond|0001
            8289|Livingwood|0000
            8290|Dreamwood|0000
            8296|Greatwood|0000
            8297|Silverwood|0000
            8298|EnergiumRed|0001
            8299|EnergiumCyan|0001
            8300|Diamond|0001
            8301|Emerald|0001
            8302|Ruby|0001
            8303|BalasRuby|0001
            8304|Sapphire|0001
            8305|GreenSapphire|0001
            8306|Topaz|0001
            8307|BlueTopaz|0001
            8308|Tanzanite|0001
            8309|RedJasper|0001
            8310|Amber|0001
            8311|Peridot|0001
            8312|Opal|0001
            8313|Amethyst|0001
            8314|OrangeSapphire|0001
            8315|YellowSapphire|0001
            8318|EnderPearl|0001
            8319|EnderEye|0001
            8320|NetherStar|0001
            8321|Jade|0001
            8322|Craponite|0001
            8323|Aquamarine|0001
            8324|Morganite|0001
            8325|Dioptase|0001
            8326|Spinel|0001
            8328|BlueSapphire|0001
            8330|Lazurite|0000
            8331|Sodalite|0000
            8332|Lapis|0000
            8333|Redstone|0001
            8334|Coal|0000
            8335|HydratedCoal|0010
            8336|Charcoal|0000
            8337|Lignite|0000
            8338|Monazite|0000
            8340|Nikolite|0001
            8341|Glowstone|0001
            8342|Firestone|0000
            8346|NetherQuartz|0000
            8347|CertusQuartz|0000
            8348|ChargedCertusQuartz|0000
            8349|CoalCoke|0000
            8360|Peat|0000
            8365|LigniteCoke|0000
            8383|PurpleSapphire|0001
            8384|Heliodor|0001
            8385|Goshenite|0001
            8386|Bixbite|0001
            8387|Maxixe|0001
            8389|Fluix|0000
            8390|PetroleumCoke|0000
            8393|SunnyQuartz|0000
            8395|RedQuartz|0000
            8396|BlazeQuartz|0000
            8397|SmokeyQuartz|0000
            8399|ElvenQuartz|0000
            8400|ChloroauricAcid|0000
            8401|ChloroplatinicAcid|0000
            8402|StannicChloride|0000
            8403|BlackVitriol|0000
            8404|BlueVitriol|0000
            8405|GreenVitriol|0000
            8406|RedVitriol|0000
            8407|PinkVitriol|0000
            8408|CyanVitriol|0000
            8409|WhiteVitriol|0000
            8410|GrayVitriol|0000
            8412|VitriolOfClay|0000
            8413|TitaniumTetrachloride|0000
            8415|OnyxRed|0001
            8416|OnyxBlack|0001
            8417|Amazonite|0001
            8418|Bamboo|0000
            8419|Zircon|0001
            8420|Azurite|0010
            8421|Eudialyte|0000
            8423|DiamondIndustrial|0001
            8424|PinkDiamond|0001
            8425|OceanJasper|0001
            8426|RainforestJasper|0001
            8427|BlueJasper|0001
            8428|GreenJasper|0001
            8429|YellowJasper|0001
            8430|TigerEye|0001
            8431|CatsEye|0001
            8432|DragonEye|0001
            8433|HawksEye|0001
            8434|BlackEye|0001
            8435|TigerIron|0001
            8436|RedFluorite|0000
            8437|PinkFluorite|0000
            8438|BlueFluorite|0000
            8439|GreenFluorite|0000
            8440|BlackFluorite|0000
            8441|WhiteFluorite|0000
            8442|YellowFluorite|0000
            8443|OrangeFluorite|0000
            8444|MagentaFluorite|0000
            8445|MilkyQuartz|0000
            8446|BrownAventurine|0001
            8447|YellowAventurine|0001
            8448|GreenAventurine|0001
            8449|BlackAventurine|0001
            8450|BlueAventurine|0001
            8451|RedAventurine|0001
            8455|ClayRed|0000
            8458|BluePhosphorus|0000
            8459|RedPhosphorus|0000
            8460|WhitePhosphorus|0000
            8461|NiobiumPentoxide|0000
            8462|TantalumPentoxide|0000
            8464|BlueDiamond|0001
            8465|GreenDiamond|0001
            8467|RedDiamond|0001
            8468|YellowDiamond|0001
            8470|Carminite|0001
            8471|Breeze|0000
            8500|Stone|0010
            8501|Concrete|0010
            8502|Netherrack|0010
            8503|NetherBrick|0010
            8504|Endstone|0010
            8505|Basalt|0010
            8506|Marble|0010
            8507|GraniteRed|0010
            8508|GraniteBlack|0010
            8511|Diorite|0010
            8516|MarsSand|0010
            8518|Granite|0010
            8521|Livingrock|0010
            8528|SkyStone|0010
            8599|Bedrock|0000
            8600|Electrum|0010
            8601|SterlingSilver|0010
            8602|RoseGold|0010
            8605|CdInAgAlloy|0000
            8610|Bronze|0010
            8611|BlackBronze|0010
            8612|BismuthBronze|0010
            8614|ArsenicCopper|0010
            8615|ArsenicBronze|0010
            8620|Brass|0010
            8621|CobaltBrass|0010
            8622|AluminiumAlloy|0000
            8630|Steel|0000
            8631|BlackSteel|0000
            8632|BlueSteel|0000
            8633|RedSteel|0000
            8634|DamascusSteel|0000
            8635|Tungstensteel|0000
            8636|StainlessSteel|0000
            8638|TungstenCarbide|0000
            8640|AnnealedCopper|0010
            8643|WroughtIron|0000
            8645|IronMagnetic|0100
            8646|SteelMagnetic|0100
            8647|NeodymiumMagnetic|0100
            8651|SteelGalvanized|1000
            8652|TungstenSintered|0000
            8653|VanadiumSteel|0000
            8658|ElectrotineAlloy|0010
            8659|BlueAlloy|0010
            8660|RedAlloy|0010
            8661|Invar|0000
            8662|Constantan|0000
            8663|Nichrome|0000
            8664|Kanthal|0000
            8665|Magnalium|0010
            8666|Ultimet|0000
            8667|TinAlloy|0010
            8668|BatteryAlloy|0010
            8669|SolderingAlloy|0010
            8672|Ironwood|0010
            8673|Steeleaf|0010
            8674|Knightmetal|0000
            8675|FierySteel|0000
            8679|Thaumium|0000
            8682|Osmiridium|0000
            8684|Trinaquadalloy|0000
            8685|ChromiumDioxide|0000
            8686|VanadiumGallium|0000
            8687|YttriumBariumCuprate|0100
            8689|NiobiumTitanium|0000
            8697|FlamascusSteel|0000
            8698|Fireleaf|0010
            8700|AluminiumBrass|0010
            8702|Alumite|0000
            8704|VibraniumSteel|0000
            8705|VibraniumSilver|0000
            8706|Vibramantium|0000
            8708|Signalum|0010
            8709|Lumium|0010
            8710|Enderium|0000
            8717|Cyanite|0000
            8720|Manasteel|0000
            8721|Terrasteel|0000
            8722|ElvenElementium|0000
            8726|VibrantAlloy|0000
            8727|ConductiveIron|0000
            8729|EnderiumBase|0000
            8730|ElectricalSteel|0000
            8732|Soularium|0000
            8733|RedstoneAlloy|0000
            8735|GaiaSpirit|0000
            8737|NikolineAlloy|0000
            8744|AncientDebris|0000
            8745|Netherite|0000
            8746|NetherizedDiamond|1000
            8747|Efrine|0000
            8751|Duranium|0000
            8752|Tritanium|0000
            8756|Duralumin|0000
            8784|Adamantine|0000
            8790|Trinitanium|0000
            8793|TitaniumIridium|0000
            8796|HSSG|0000
            8797|HSSE|0000
            8798|HSSS|0000
            8799|CrystalMatrix|0000
            8800|CosmicNeutronium|0000
            8802|TantalumHafniumCarbide|0000
            8803|CastIron|0000
            9000|Pumice|0010
            9001|Diatomite|0000
            9003|BasalticMineralSand|0000
            9004|GraniticMineralSand|0000
            9005|GarnetSand|0000
            9006|QuartzSand|0000
            9007|UraniumTetrafluoride|0000
            9008|UraniumHexafluoride|0000
            9009|Uranium238Tetrafluoride|0000
            9010|Uranium238Hexafluoride|0000
            9011|Uranium235Tetrafluoride|0000
            9012|Uranium235Hexafluoride|0000
            9100|RareEarth|0000
            9101|Almandine|0001
            9102|Andradite|0001
            9103|Asbestos|0010
            9104|Hematite|0000
            9105|Bauxite|0000
            9106|BrownLimonite|0000
            9107|Calcite|0000
            9108|Cassiterite|0010
            9109|Realgar|0011
            9110|Celestine|0000
            9111|Chalcopyrite|0010
            9113|Chromite|0000
            9114|Cinnabar|0001
            9115|Cobaltite|0000
            9116|Cooperite|0000
            9117|Galena|0010
            9118|Garnierite|0000
            9119|Grossular|0001
            9120|Ilmenite|0000
            9122|Magnetite|0000
            9123|Molybdenite|0000
            9124|Powellite|0000
            9125|Pyrite|0000
            9126|Pyrolusite|0000
            9127|Pyrope|0001
            9128|Scheelite|0000
            9129|Spessartine|0001
            9130|Sphalerite|0010
            9131|Stibnite|0000
            9132|Tetrahedrite|0010
            9133|Tungstate|0000
            9134|Uraninite|0000
            9135|Uvarovite|0001
            9136|Wulfenite|0000
            9137|YellowLimonite|0000
            9138|Perlite|0000
            9139|Borax|0000
            9140|PotassiumFeldspar|0000
            9141|Biotite|0000
            9143|VanadiumMagnetite|0000
            9144|Bastnasite|0000
            9145|Pentlandite|0000
            9146|Spodumene|0000
            9147|Pollucite|0000
            9148|Tantalite|0000
            9149|Lepidolite|0000
            9150|Glauconite|0000
            9152|Vermiculite|0000
            9153|Bentonite|0000
            9154|Palygorskite|0000
            9155|Pitchblende|0000
            9156|Malachite|0010
            9157|Mirabilite|0000
            9158|Mica|0000
            9159|Trona|0000
            9160|Barite|0000
            9161|Gypsum|0000
            9162|Alunite|0000
            9164|Wollastonite|0000
            9165|Zeolite|0000
            9166|Kyanite|0000
            9167|Kaolinite|0000
            9169|Talc|0000
            9171|Greenschist|0010
            9174|Graphite|0000
            9175|Graphene|0000
            9177|Komatiite|0010
            9180|Quartzite|0010
            9184|Blueschist|0010
            9185|Bluestone|0010
            9186|Chert|0010
            9187|Dacite|0010
            9188|Andesite|0010
            9189|Limestone|0010
            9190|Shale|0010
            9192|Rutile|0000
            9193|Stolzite|0000
            9194|Ferberite|0000
            9195|Huebnerite|0000
            9196|Russellite|0000
            9197|Pinalite|0000
            9198|DiduraniumTrioxide|0000
            9199|DuraniumHexafluoride|0000
            9200|DuraniumHexachloride|0000
            9201|DuraniumHexabromide|0000
            9202|DuraniumHexaiodide|0000
            9203|DuraniumHexaastatide|0000
            9204|TritaniumDioxide|0000
            9205|TritaniumHexafluoride|0000
            9206|TritaniumHexachloride|0000
            9207|TritaniumHexabromide|0000
            9208|TritaniumHexaiodide|0000
            9209|TritaniumHexaastatide|0000
            9210|Bromargyrite|0000
            9211|Smithsonite|0000
            9212|Sperrylite|0000
            9213|Kesterite|0000
            9214|Stannite|0000
            9215|Fluorite|0000
            9216|Arsenopyrite|0000
            9217|Wolframite|0000
            9218|Kimberlite|0010
            9219|Prismarine|0010
            9220|PrismarineDark|0010
            9221|Bischofite|0000
            9222|Slate|0010
            9223|Blackstone|0010
            9243|ClayBrick|0000
            9246|Columbite|0000
            9247|Coltan|0000
            9248|Deepslate|0010
            9261|TitanRock|0010
            9271|Quicklime|0000
            9300|Oak|0000
            9301|Birch|0000
            9302|Spruce|0000
            9303|Junglewood|0000
            9304|Acacia|0000
            9305|DarkOak|0000
            9306|Crimsonwood|0000
            9307|Warpedwood|0000
            9308|WoodCompressed|0000
            9309|WoodDead|0000
            9310|WoodRotten|0000
            9311|WoodMossy|0000
            9312|WoodFrozen|0000
            9313|Maple|0000
            9314|Willow|0000
            9315|BlueMahoe|0000
            9316|Hazel|0000
            9317|Cinnamonwood|0000
            9318|Coconutwood|0000
            9319|Rainbowood|0000
            9344|Cherrywood|0000
            9372|Mangrove|0000
            9409|BlueSpruce|0000
            9700|MeatRaw|0010
            9701|MeatCooked|0000
            9702|Wheat|0000
            9703|Sugar|0010
            9705|Rye|0000
            9709|Potato|0000
            9710|MeatRotten|0000
            9711|FishCooked|0000
            9712|FishRaw|0010
            9713|FishRotten|0000
            9779|SoylentGreen|0000
            9780|Cheese|0010
            9781|Chili|0000
            9782|Cocoa|0000
            9783|Chocolate|0010
            9784|Coffee|0000
            9785|Cinnamon|0000
            9790|Milk|0000
            9791|Honey|0000
            9792|Tea|0000
            9793|Honeydew|0000
            9794|Mint|0000
            9796|Hazelnut|0000
            9800|Water|0000
            9801|Snow|0000
            9802|Ice|0001
            9803|FreshWater|0000
            9804|Saltwater|0000
            9805|HolyWater|0000
            9806|SeaWater|0000
            9808|WaterDistilled|0000
            9809|HydrogenPeroxide|0000
            9810|Lava|0000
            9811|SemiheavyWater|0000
            9812|HeavyWater|0000
            9813|TritiatedWater|0000
            9814|Steam|0000
            9815|SaltedWater|0000
            9820|NitroCarbon|0000
            9821|Glyceryl|0000
            9822|SodiumPersulfate|0000
            9823|SodiumSulfide|0000
            9824|SulfuricAcid|0000
            9825|NitricAcid|0000
            9826|HydrochloricAcid|0000
            9827|AquaRegia|0000
            9828|Glycerol|0000
            9829|HydrogenFluoride|0000
            9830|Air|0000
            9831|NitrogenDioxide|0000
            9832|Methane|0000
            9833|MethaneIce|0000
            9834|SulfurDioxide|0000
            9835|SulfurTrioxide|0000
            9836|CarbonDioxide|0000
            9837|NitrogenMonoxide|0000
            9838|CarbonMonoxide|0000
            9839|HeliumNeon|0000
            9840|Biomass|0000
            9841|BioFuel|0000
            9842|Ethanol|0000
            9843|CarbonTrioxide|0000
            9844|DisulfuricAcid|0000
            9850|Oil|0000
            9851|OilSand|0000
            9852|CrudeOil|0000
            9853|OilShale|0000
            9860|Fuel|0000
            9861|NitroFuel|0000
            9862|Kerosine|0000
            9863|Diesel|0000
            9864|Petrol|0000
            9865|Propane|0000
            9866|Butane|0000
            9867|Propylene|0000
            9868|Ethylene|0000
            9870|Creosote|0000
            9871|FishOil|0000
            9872|SeedOil|0000
            9873|HempOil|0000
            9874|LinOil|0000
            9875|SunflowerOil|0000
            9876|NutOil|0000
            9877|OliveOil|0000
            9880|FryingOilHot|0000
            9881|Glue|0000
            9882|Lubricant|0000
            9883|ConstructionFoam|0000
            9886|Latex|0000
            """;
}
