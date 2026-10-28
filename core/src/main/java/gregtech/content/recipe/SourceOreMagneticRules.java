package gregtech.content.recipe;

import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceOreWashingRules.FormSelection;

import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Magnetic Separator rules from original GT6 Ores 54/59/64, 385-393 and 448-456,
 * pinned at 3703e40308c8c030763fd6297dea8b210d2a77b1. Actual native registrations
 * are supplied by the caller; this class neither generates forms nor changes magnetic facts.
 */
public final class SourceOreMagneticRules {
    private SourceOreMagneticRules() {}

    public static final int POWER = 16;
    public static final boolean OPTIMIZE = true;
    public static final List<Long> CHANCES = List.of(10000L, 600L, 600L, 600L, 600L, 600L);

    public enum Size {
        WHOLE(MaterialPrefix.crushedPurified, MaterialPrefix.crushedCentrifuged, MaterialPrefix.dust, 18, 144),
        TINY(MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.crushedCentrifugedTiny, MaterialPrefix.dustTiny, 2, 16);
        private final MaterialPrefix input, refined, dust;
        private final int byproductCount, ticks;

        Size(MaterialPrefix input, MaterialPrefix refined, MaterialPrefix dust, int byproductCount, int ticks) {
            this.input = input;
            this.refined = refined;
            this.dust = dust;
            this.byproductCount = byproductCount;
            this.ticks = ticks;
        }

        public MaterialPrefix input() {
            return input;
        }

        public MaterialPrefix refined() {
            return refined;
        }

        public MaterialPrefix dust() {
            return dust;
        }

        public int byproductCount() {
            return byproductCount;
        }

        public int ticks() {
            return ticks;
        }
    }

    /** Shared Ores gate; neither centrifuged output form is a prerequisite. */
    public static Availability eligibility(
            boolean valid,
            boolean antimatter,
            boolean bedrock,
            Availability dust,
            Availability wholePurified,
            Availability tinyPurified) {
        if (!valid || antimatter || bedrock) return Availability.ABSENT;
        return SourceOreWashingRules.requireAll(dust, wholePurified, tinyPurified);
    }

    /** OM 347-353: same count in the selected direct prefix, not a material-unit conversion.
     * An absent final dust form is an empty source output slot, not a whole-plan veto. */
    public static FormSelection refinedPrefix(Size size, Function<MaterialPrefix, Availability> forms) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(forms);
        for (MaterialPrefix prefix : List.of(size.refined(), size.dust())) {
            Availability available = forms.apply(prefix);
            if (available == Availability.PRESENT) return new FormSelection(available, prefix);
            if (available == null || available == Availability.UNKNOWN)
                return new FormSelection(Availability.UNKNOWN, null);
        }
        return new FormSelection(Availability.ABSENT, null);
    }

    /** Caller retains the existing opposite-class filtering. Source UT.Code.select pads with
     * self, not the first/last candidate. Duplicate positions remain independent chance slots. */
    public static <M> List<M> byproducts(M self, List<M> orderedOppositeCandidates) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(orderedOppositeCandidates);
        if (orderedOppositeCandidates.isEmpty()) return List.of();
        var selected = new ArrayList<M>(5);
        for (int index = 0; index < 5; index++)
            selected.add(index < orderedOppositeCandidates.size() ? orderedOppositeCandidates.get(index) : self);
        return List.copyOf(selected);
    }

    public record Material(int id, String name) {
        public Material {
            Objects.requireNonNull(name);
        }

        public boolean matches(int materialId, String canonicalName) {
            return id == materialId && name.equals(canonicalName);
        }
    }

    public record Output(Material material, MaterialPrefix prefix, int count) {}
    /** These independent direct rows have no fluid input or output and no generic Ores gate.
     * Input is one physical blockDust, never nine dust items. Outputs are direct prefixes. */
    public record BlockRow(Material input, int ticks, int power, List<Output> outputs, List<Long> chances) {
        public BlockRow {
            outputs = List.copyOf(outputs);
            chances = List.copyOf(chances);
        }

        public int inputCount() {
            return 1;
        }

        public boolean optimize() {
            return OPTIMIZE;
        }
    }

    private static Output output(int id, String name, MaterialPrefix prefix, int count) {
        return new Output(new Material(id, name), prefix, count);
    }

    private static final List<BlockRow> BLOCK_ROWS = List.of(
            new BlockRow(
                    new Material(8102, "SluiceSand"),
                    1296,
                    16,
                    List.of(
                            output(8500, "Stone", MaterialPrefix.dust, 9),
                            output(260, "Iron", MaterialPrefix.dustTiny, 2),
                            output(600, "Neodymium", MaterialPrefix.dustTiny, 1),
                            output(280, "Nickel", MaterialPrefix.dustTiny, 1),
                            output(270, "Cobalt", MaterialPrefix.dustTiny, 1),
                            output(250, "Manganese", MaterialPrefix.dustTiny, 1)),
                    List.of(9640L, 5832L, 5832L, 5832L, 5832L, 5832L)),
            new BlockRow(
                    new Material(8599, "Bedrock"),
                    1296,
                    64,
                    List.of(
                            output(9248, "Deepslate", MaterialPrefix.dust, 9),
                            output(8784, "Adamantine", MaterialPrefix.dustTiny, 9),
                            output(2210, "Atlarus", MaterialPrefix.dustTiny, 9),
                            output(9100, "RareEarth", MaterialPrefix.dustTiny, 18),
                            output(600, "Neodymium", MaterialPrefix.dustTiny, 27),
                            output(8234, "VanadiumPentoxide", MaterialPrefix.dustTiny, 27)),
                    List.of(7000L, 3000L, 3000L, 3000L, 3000L, 3000L)),
            new BlockRow(
                    new Material(8514, "MoonTurf"),
                    1296,
                    16,
                    List.of(
                            output(8505, "Basalt", MaterialPrefix.dust, 9),
                            output(8649, "MeteoricIron", MaterialPrefix.dustTiny, 9),
                            output(600, "Neodymium", MaterialPrefix.dustTiny, 1),
                            output(280, "Nickel", MaterialPrefix.dustTiny, 1),
                            output(270, "Cobalt", MaterialPrefix.dustTiny, 1),
                            output(250, "Manganese", MaterialPrefix.dustTiny, 1)),
                    List.of(3000L, 972L, 972L, 972L, 972L, 972L)));

    public static List<BlockRow> blockRows() {
        return BLOCK_ROWS;
    }
}
