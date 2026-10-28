package gregtech.content.recipe;

import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;

import gregtech.api.material.GTValues;
import gregtech.content.data.MaterialPrefix;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * The six explicit Shredder handlers, original GT6 3703e40308c8c030763fd6297dea8b210d2a77b1,
 * Loader_Recipes_Handlers 138-150 and RecipeMapHandlerPrefix 204-226.
 * Actual registered forms, not default-generation flags, decide availability. Native ownership
 * and the separate non-ore continuation policy remain the caller's responsibility.
 */
public final class SourceOreShreddingRules {
    private SourceOreShreddingRules() {}

    public static final int POWER = 16;
    public static final boolean OPTIMIZE = false;

    /** Every input and every ordered output slot has count one. Duplicate slots are intentional.
     * PrefixShredding 45-47 takes only the runtime pulverization target MATERIAL; the target amount
     * is not part of these fixed-output rows, even when it is zero. There are no remains or fluids. */
    public record Shape(MaterialPrefix input, List<MaterialPrefix> outputs) {
        public Shape {
            Objects.requireNonNull(input);
            outputs = List.copyOf(outputs);
            if (outputs.isEmpty()) throw new IllegalArgumentException("Shredding requires output slots");
        }

        public int inputCount() {
            return 1;
        }

        public int outputCount() {
            return 1;
        }
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape(
                    MaterialPrefix.crushed,
                    List.of(MaterialPrefix.dust, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72)),
            new Shape(MaterialPrefix.crushedPurified, List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall)),
            new Shape(
                    MaterialPrefix.crushedCentrifuged,
                    List.of(
                            MaterialPrefix.dust,
                            MaterialPrefix.dustSmall,
                            MaterialPrefix.dustTiny,
                            MaterialPrefix.dustDiv72)),
            new Shape(MaterialPrefix.crushedTiny, List.of(MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72)),
            new Shape(
                    MaterialPrefix.crushedPurifiedTiny,
                    List.of(MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72, MaterialPrefix.dustDiv72)),
            new Shape(
                    MaterialPrefix.crushedCentrifugedTiny,
                    List.of(
                            MaterialPrefix.dustTiny,
                            MaterialPrefix.dustDiv72,
                            MaterialPrefix.dustDiv72,
                            MaterialPrefix.dustDiv72)));

    public static List<Shape> shapes() {
        return SHAPES;
    }

    public static Shape shape(MaterialPrefix input) {
        for (Shape shape : SHAPES) if (shape.input() == input) return shape;
        return null;
    }

    /** All fixed outputs are required; there is no smaller-dust fallback or skipped empty slot.
     * MORTAR NO selects the slower handler rather than denying a recipe. No ORES/Bedrock/hidden
     * or crushed-generation blacklist gate exists in these handlers. */
    public static Availability eligibility(
            Shape shape,
            boolean validInput,
            boolean antimatter,
            boolean validTarget,
            Decision mortar,
            Availability input,
            List<Availability> outputs) {
        Objects.requireNonNull(shape);
        Objects.requireNonNull(mortar);
        Objects.requireNonNull(input);
        Objects.requireNonNull(outputs);
        if (outputs.size() != shape.outputs().size())
            throw new IllegalArgumentException("One availability observation is required per output slot");
        if (!validInput || antimatter || !validTarget) return Availability.ABSENT;
        boolean unknown = mortar == Decision.UNKNOWN || input == Availability.UNKNOWN;
        boolean absent = input == Availability.ABSENT;
        for (Availability output : outputs) {
            Objects.requireNonNull(output);
            absent |= output == Availability.ABSENT;
            unknown |= output == Availability.UNKNOWN;
        }
        return absent ? Availability.ABSENT : unknown ? Availability.UNKNOWN : Availability.PRESENT;
    }

    /** MORTAR and quality both belong to the original input, never the output target. */
    public static long duration(Shape shape, boolean mortar, int inputQuality) {
        Objects.requireNonNull(shape);
        long outputUnits = 0;
        for (MaterialPrefix output : shape.outputs())
            outputUnits = Math.addExact(outputUnits, output.getMaterialWeight());
        return duration(shape.input().getMaterialWeight(), outputUnits, mortar ? 16 : 256, inputQuality);
    }

    /** Prefix 225-226: ceiling at the end, then minimum one tick, not max(1, quality+1).
     * Rock's existing Crusher handler can use the same cost with outputUnits=0 and multiplier=16;
     * unlike these fixed Shredder rows, its remains use PulverizationRules.amount. */
    public static long duration(long inputUnits, long outputUnits, long multiplier, int inputQuality) {
        if (inputUnits < 0 || outputUnits < 0 || multiplier < 0)
            throw new IllegalArgumentException("Negative handler units or multiplier");
        BigInteger numerator = BigInteger.valueOf(Math.max(inputUnits, outputUnits))
                .multiply(BigInteger.valueOf(multiplier))
                .multiply(BigInteger.valueOf((long) inputQuality + 1));
        if (numerator.signum() <= 0) return 1;
        BigInteger unit = BigInteger.valueOf(GTValues.U);
        return numerator
                .add(unit)
                .subtract(BigInteger.ONE)
                .divide(unit)
                .max(BigInteger.ONE)
                .longValueExact();
    }
}
