package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Pure item-recovery planning over an effective composition.
 *
 * <p>Content supplies stack-form weights and material processing facts through
 * {@link MaterialFacts}; this class does not read recipe tables, prefixes, or game stacks.</p>
 */
public final class ItemRecoveryPlanner {
    private ItemRecoveryPlanner() {}

    /** A legal output stack form and its material weight in GT6 units. */
    public record DustForm(String id, long weight) {
        public DustForm {
            if (id == null || id.isBlank() || weight <= 0)
                throw new IllegalArgumentException("Dust form needs an id and positive weight");
        }
    }

    public record DustPile(DustForm form, int count) {
        public DustPile {
            Objects.requireNonNull(form, "form");
            if (count <= 0 || count > 64)
                throw new IllegalArgumentException("Dust pile must use a positive legal stack count");
        }

        public long units() {
            return Math.multiplyExact(form.weight(), count);
        }
    }

    /** Exact stack grouping for one material amount; {@code remainder} is below the finest form. */
    public record DustPlan(List<DustPile> piles, long representedUnits, long remainderUnits) {
        public DustPlan {
            piles = List.copyOf(piles);
        }

        public boolean complete() {
            return remainderUnits == 0;
        }

        public int stackCount() {
            return piles.size();
        }
    }

    /** Content-owned processing facts consumed by the platform-neutral planner. */
    public interface MaterialFacts {
        long pulverizedAmount(GTMaterial source, long amount);

        long dynamicShredderWork(GTMaterial material);

        long staticShredderWork(GTMaterial material);

        List<DustForm> dustForms();
    }

    public enum ShredderRejection {
        NONE,
        NO_COMPONENTS,
        INVALID_COMPONENT,
        MISSING_PULVERIZATION_TARGET,
        INVALID_PULVERIZATION_TARGET,
        UNREPRESENTABLE_AMOUNT,
        OUTPUT_LIMIT,
        OVERFLOW
    }

    public record ShredderOutput(GTMaterial material, DustPlan dust) {
        public ShredderOutput {
            if (material == null || !material.isValid() || dust == null)
                throw new IllegalArgumentException("Invalid shredder output");
        }
    }

    public record ShredderPlan(List<ShredderOutput> outputs, long duration, ShredderRejection rejection) {
        public ShredderPlan {
            outputs = List.copyOf(outputs);
            Objects.requireNonNull(rejection, "rejection");
        }

        public boolean accepted() {
            return rejection == ShredderRejection.NONE && !outputs.isEmpty();
        }

        public int stackCount() {
            return outputs.stream()
                    .mapToInt(output -> output.dust().stackCount())
                    .sum();
        }
    }

    public static ShredderPlan planShredder(
            ItemComposition composition, int outputItemSlots, MaterialFacts facts, boolean staticPrefix) {
        return planShredder(composition, outputItemSlots, facts, staticPrefix, false);
    }

    /** Damaged source stacks use GT6's floor to the finest dust form instead of rejecting the tail. */
    public static ShredderPlan planShredder(
            ItemComposition composition,
            int outputItemSlots,
            MaterialFacts facts,
            boolean staticPrefix,
            boolean dropDustRemainder) {
        Objects.requireNonNull(facts, "facts");
        if (composition == null || composition.components().isEmpty()) return rejected(ShredderRejection.NO_COMPONENTS);
        if (outputItemSlots <= 0) return rejected(ShredderRejection.OUTPUT_LIMIT);
        try {
            var targetUnits = new LinkedHashMap<GTMaterial, Long>();
            long inputUnits = 0;
            for (MaterialComponent component : composition.components()) {
                GTMaterial material = component.material().resolve();
                if (!material.isValid() || component.amount() <= 0) continue;
                inputUnits = Math.addExact(inputUnits, component.amount());
                GTMaterial target = material.getTargetPulverMaterial();
                if (target == null) continue;
                target = target.resolve();
                if (!target.isValid()) continue;
                long amount = facts.pulverizedAmount(material, component.amount());
                if (amount > 0) targetUnits.merge(target, amount, Math::addExact);
            }
            if (targetUnits.isEmpty()) return rejected(ShredderRejection.NO_COMPONENTS);

            var outputs = new ArrayList<ShredderOutput>();
            int remainingSlots = outputItemSlots;
            long duration = 0;
            if (staticPrefix && inputUnits > 0) {
                GTMaterial source = composition.material();
                if (source == null || !source.isValid()) {
                    for (MaterialComponent component : composition.components()) {
                        GTMaterial candidate = component.material().resolve();
                        if (candidate.isValid()) {
                            source = candidate;
                            break;
                        }
                    }
                }
                duration = source == null
                        ? 0
                        : duration(inputUnits, facts.staticShredderWork(source));
            }
            for (var entry : targetUnits.entrySet()) {
                if (remainingSlots <= 0) return rejected(ShredderRejection.OUTPUT_LIMIT);
                var dust = dustPlan(entry.getValue(), remainingSlots, facts.dustForms());
                if (!dust.complete()) {
                    if (dropDustRemainder && !dust.piles().isEmpty()) {
                        outputs.add(new ShredderOutput(entry.getKey(), dust));
                        remainingSlots -= dust.stackCount();
                        if (!staticPrefix)
                            duration = Math.addExact(
                                    duration,
                                    duration(entry.getValue(), facts.dynamicShredderWork(entry.getKey())));
                        continue;
                    }
                    return rejected(
                            dust.stackCount() >= remainingSlots
                                    ? ShredderRejection.OUTPUT_LIMIT
                                    : ShredderRejection.UNREPRESENTABLE_AMOUNT);
                }
                if (dust.piles().isEmpty()) continue;
                outputs.add(new ShredderOutput(entry.getKey(), dust));
                remainingSlots -= dust.stackCount();
                if (!staticPrefix)
                    duration = Math.addExact(
                            duration,
                            duration(entry.getValue(), facts.dynamicShredderWork(entry.getKey())));
            }
            if (outputs.isEmpty()) return rejected(ShredderRejection.NO_COMPONENTS);
            return new ShredderPlan(outputs, Math.max(1, duration), ShredderRejection.NONE);
        } catch (ArithmeticException overflow) {
            return rejected(ShredderRejection.OVERFLOW);
        }
    }

    private static DustPlan dustPlan(long amount, int maximumStacks, List<DustForm> forms) {
        if (amount < 0 || maximumStacks <= 0)
            throw new IllegalArgumentException("Dust amount and stack limit must be positive");
        var piles = new ArrayList<DustPile>(Math.min(maximumStacks, 4));
        long remaining = amount;
        long represented = 0;
        for (DustForm form : forms) {
            long weight = form.weight();
            if (weight <= 0 || remaining < weight) continue;
            while (remaining >= weight) {
                if (piles.size() >= maximumStacks) return new DustPlan(piles, represented, remaining);
                int count = (int) Math.min(64, remaining / weight);
                piles.add(new DustPile(form, count));
                long units = Math.multiplyExact(weight, count);
                represented = Math.addExact(represented, units);
                remaining = Math.subtractExact(remaining, units);
            }
        }
        return new DustPlan(piles, represented, remaining);
    }

    private static long duration(long amount, long work) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(work))
                .add(BigInteger.valueOf(GTValues.U - 1))
                .divide(BigInteger.valueOf(GTValues.U))
                .longValueExact();
    }

    private static ShredderPlan rejected(ShredderRejection rejection) {
        return new ShredderPlan(List.of(), 0, rejection);
    }
}
