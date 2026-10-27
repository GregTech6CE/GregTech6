package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static gregtech.content.recipe.SourceOreWashingRules.Availability;

/**
 * Purified Sifter calls from original GT6 Ores 284-309 and 385-387/448-461,
 * pinned at 3703e40308c8c030763fd6297dea8b210d2a77b1. Fixed calls precede the
 * generic loop and resolve their direct output prefixes without generic grade fallbacks.
 */
public final class SourceOreSiftingRules {
    private SourceOreSiftingRules() {}
    public static final int POWER = 16;
    public static final boolean OPTIMIZE = true;
    public static final List<Long> WHOLE_CHANCES = List.of(9L, 90L, 360L, 1350L, 1800L, 3600L, 4500L);
    public static final List<Long> TINY_CHANCES = List.of(1L, 10L, 40L, 150L, 200L, 400L, 500L);

    public enum Size {
        WHOLE(MaterialPrefix.crushedPurified, 144, 9),
        TINY(MaterialPrefix.crushedPurifiedTiny, 16, 1);
        private final MaterialPrefix input;
        private final int ticks, specialTinyCount;
        Size(MaterialPrefix input, int ticks, int specialTinyCount) {
            this.input = input; this.ticks = ticks; this.specialTinyCount = specialTinyCount;
        }
        public MaterialPrefix input() { return input; }
        public int ticks() { return ticks; }
        public int specialTinyCount() { return specialTinyCount; }
        public List<Long> genericChances() { return this == WHOLE ? WHOLE_CHANCES : TINY_CHANCES; }
    }

    /** Source tests actual registrations, not the ordinary gem's generation condition. */
    public static Availability eligibility(boolean valid, boolean antimatter, boolean bedrock,
            Availability dust, Availability wholePurified, Availability tinyPurified, Availability ordinaryGem) {
        if (!valid || antimatter || bedrock) return Availability.ABSENT;
        return SourceOreWashingRules.requireAll(dust, wholePurified, tinyPurified, ordinaryGem);
    }

    /** The fallback is an ordinary-gem count, not multiplied by the requested grade count.
     * Zero means a direct output; these alternatives apply only to the generic route. */
    public record GenericOutput(MaterialPrefix prefix, int count, int fallbackGemCount) {}
    private static final List<GenericOutput> GENERIC_OUTPUTS = List.of(
            new GenericOutput(MaterialPrefix.gemLegendary, 1, 8),
            new GenericOutput(MaterialPrefix.gemExquisite, 1, 4),
            new GenericOutput(MaterialPrefix.gemFlawless, 1, 2),
            new GenericOutput(MaterialPrefix.gem, 1, 0),
            new GenericOutput(MaterialPrefix.gemFlawed, 2, 1),
            new GenericOutput(MaterialPrefix.gemChipped, 4, 1),
            new GenericOutput(MaterialPrefix.dust, 1, 0));
    public static List<GenericOutput> genericOutputs() { return GENERIC_OUTPUTS; }

    public record Material(int id, String name) {
        public Material { Objects.requireNonNull(name); }
        public boolean matches(int materialId, String canonicalName) {
            return id == materialId && name.equals(canonicalName);
        }
    }
    public sealed interface Output permits FormOutput, VanillaOutput, FossilOutput {}
    public record FormOutput(Material material, MaterialPrefix prefix, int count) implements Output {
        public FormOutput { Objects.requireNonNull(material); Objects.requireNonNull(prefix); }
    }
    public enum VanillaItem { SKELETON_SKULL, BONE }
    public record VanillaOutput(VanillaItem item, int count) implements Output {
        public VanillaOutput { Objects.requireNonNull(item); }
    }
    /** Source UB fossilPiece metadata is an identity index, not modern item damage or a guessed ID. */
    public record FossilOutput(int index, FormOutput fallback) implements Output {
        public FossilOutput {
            if (index < 0 || index > 7) throw new IllegalArgumentException("Fossil index must be 0..7");
            Objects.requireNonNull(fallback);
        }
    }
    public enum FossilChoice { PROVIDER, FALLBACK, DEFER }
    /** Each optional piece resolves independently. Only known absence selects its Bone-dust default. */
    public static FossilChoice fossilChoice(Availability provider) {
        if (provider == Availability.PRESENT) return FossilChoice.PROVIDER;
        if (provider == Availability.ABSENT) return FossilChoice.FALLBACK;
        return FossilChoice.DEFER;
    }

    /** One independent input, no generic dust/gem/paired-input gate and no fluids. Known missing
     * outputs retain their indices as empty native slots; do not filter this descriptor's outputs.
     * Tiny quartz calls deliberately have more raw chances than outputs (source Recipe 892-895). */
    public record SpecialRow(Material input, Size size, List<Output> outputs, List<Long> chances, int sourceLine) {
        public SpecialRow {
            Objects.requireNonNull(input); Objects.requireNonNull(size);
            outputs = List.copyOf(outputs); chances = List.copyOf(chances);
        }
        public int inputCount() { return 1; }
        public int ticks() { return size.ticks(); }
        public int power() { return POWER; }
        public boolean optimize() { return OPTIMIZE; }
    }

    private static final Material BONE = new Material(8219, "Bone");
    private static final Material CERTUS = new Material(8347, "CertusQuartz");
    private static final Material CHARGED_CERTUS = new Material(8348, "ChargedCertusQuartz");
    private static final List<SpecialRow> SPECIAL_ROWS = createSpecialRows();
    public static List<SpecialRow> specialRows() { return SPECIAL_ROWS; }

    private static List<SpecialRow> createSpecialRows() {
        var rows = new ArrayList<SpecialRow>(22);
        gradePair(rows, new Material(9103, "Asbestos"), new Material(8321, "Jade"), 284);
        gradePair(rows, new Material(500, "Tin"), new Material(8419, "Zircon"), 286);
        gradePair(rows, new Material(9108, "Cassiterite"), new Material(8419, "Zircon"), 288);
        fossilPair(rows, BONE, 292);
        fossilPair(rows, new Material(9163, "Dolomite"), 294);
        certusPair(rows, CHARGED_CERTUS, true, 298);
        certusPair(rows, CERTUS, false, 300);
        quartzPair(rows, new Material(8346, "NetherQuartz"), 302);
        quartzPair(rows, new Material(8457, "VoidQuartz"), 304);
        quartzPair(rows, new Material(8445, "MilkyQuartz"), 306);
        quartzPair(rows, new Material(8389, "Fluix"), 308);
        return List.copyOf(rows);
    }
    private static FormOutput form(Material material, MaterialPrefix prefix, int count) {
        return new FormOutput(material, prefix, count);
    }
    private static void gradePair(List<SpecialRow> rows, Material input, Material target, int line) {
        for (Size size : Size.values()) {
            List<Output> outputs = List.of(
                    form(input, MaterialPrefix.crushedCentrifugedTiny, size.specialTinyCount()),
                    form(target, MaterialPrefix.gemExquisite, 1), form(target, MaterialPrefix.gemFlawless, 1),
                    form(target, MaterialPrefix.gem, 1), form(target, MaterialPrefix.gemFlawed, 2),
                    form(target, MaterialPrefix.gemChipped, 4), form(target, MaterialPrefix.dust, 1));
            var chances = new ArrayList<>(size.genericChances());
            chances.set(0, 10000L);
            rows.add(new SpecialRow(input, size, outputs, chances, line + size.ordinal()));
        }
    }
    private static void fossilPair(List<SpecialRow> rows, Material input, int line) {
        for (Size size : Size.values()) {
            var outputs = new ArrayList<Output>(12);
            outputs.add(form(input, MaterialPrefix.crushedCentrifugedTiny, size.specialTinyCount()));
            outputs.add(new VanillaOutput(VanillaItem.SKELETON_SKULL, 1));
            outputs.add(new VanillaOutput(VanillaItem.BONE, 1));
            outputs.add(form(BONE, MaterialPrefix.dust, 1));
            for (int index = 0; index < 8; index++)
                outputs.add(new FossilOutput(index, form(BONE, MaterialPrefix.dust, 1)));
            var chances = new ArrayList<Long>(12);
            chances.add(10000L); chances.add(size == Size.WHOLE ? 360L : 40L);
            for (int index = 0; index < 10; index++) chances.add(size == Size.WHOLE ? 3600L : 400L);
            rows.add(new SpecialRow(input, size, outputs, chances, line + size.ordinal()));
        }
    }
    private static void certusPair(List<SpecialRow> rows, Material input, boolean charged, int line) {
        for (Size size : Size.values()) {
            var outputs = new ArrayList<Output>();
            outputs.add(form(CHARGED_CERTUS, MaterialPrefix.gem, 1));
            outputs.add(form(CERTUS, MaterialPrefix.gem, 1));
            for (int index = 0; index < size.specialTinyCount(); index++)
                outputs.add(form(CERTUS, MaterialPrefix.dustTiny, 1));
            long high = size == Size.WHOLE ? 7200 : 800, low = size == Size.WHOLE ? 900 : 100;
            var chances = new ArrayList<Long>();
            chances.add(charged ? high : low); chances.add(charged ? low : high);
            for (int index = 0; index < 9; index++) chances.add(5000L);
            rows.add(new SpecialRow(input, size, outputs, chances, line + size.ordinal()));
        }
    }
    private static void quartzPair(List<SpecialRow> rows, Material input, int line) {
        for (Size size : Size.values()) {
            var outputs = new ArrayList<Output>();
            outputs.add(form(input, MaterialPrefix.gem, 1));
            for (int index = 0; index < size.specialTinyCount(); index++)
                outputs.add(form(input, MaterialPrefix.dustTiny, 1));
            var chances = new ArrayList<Long>();
            chances.add(size == Size.WHOLE ? 7200L : 800L);
            for (int index = 0; index < 9; index++) chances.add(5000L);
            rows.add(new SpecialRow(input, size, outputs, chances, line + size.ordinal()));
        }
    }
}
