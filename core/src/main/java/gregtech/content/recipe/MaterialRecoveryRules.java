package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.ItemComposition;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Shared material-side recovery planning for item compositions.
 *
 * <p>Platform producers bind the returned material stacks to items. This class never reads an
 * {@code ItemStack}, so damaged scaling, container state and bulk authorization stay at the runtime
 * boundary.</p>
 */
public final class MaterialRecoveryRules {
    private MaterialRecoveryRules() {}

    public record DustPile(MaterialPrefix prefix, int count) {
        public DustPile {
            if (prefix == null || count <= 0 || count > 64)
                throw new IllegalArgumentException("Dust pile must use a positive legal stack count");
        }

        public long units() {
            return Math.multiplyExact(prefix.getMaterialWeight(), count);
        }
    }

    /** Exact stack grouping for one material amount; {@code remainder} is below GT6's finest form. */
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

    public static long pulverizedAmount(GTMaterial source, long amount) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(source.getTargetPulverAmount()))
                .divide(BigInteger.valueOf(GTValues.U))
                .longValueExact();
    }

    public static long shredderWork(GTMaterial material) {
        return (material.getName().contains("Quartz")
                        ? 64L
                        : material.hasAny(MaterialProperty.WOOD, MaterialProperty.STONE, MaterialProperty.GEM)
                                        || gregtech.content.data.generated.MaterialWorkability.isBrittle(material)
                                        || gregtech.content.data.generated.MaterialWorkability.isFood(material)
                                ? 2L
                                : 256L)
                * Math.max(1, material.getToolQuality() + 1);
    }

    /**
     * Groups an amount into legal dust stacks without the historical 64-item cutoff.
     *
     * <p>The stock stack is 64; an amount of 72U therefore becomes 64 dust plus eight dust,
     * not 64 dust total. Amounts below one 1/72 dust are reported as an explicit remainder.</p>
     */
    public static DustPlan dustPlan(long amount) {
        return dustPlan(amount, Integer.MAX_VALUE);
    }

    private static DustPlan dustPlan(long amount, int maximumStacks) {
        if (amount < 0 || maximumStacks <= 0)
            throw new IllegalArgumentException("Dust amount and stack limit must be positive");
        var piles = new ArrayList<DustPile>(Math.min(maximumStacks, 4));
        long remaining = amount;
        long represented = 0;
        for (MaterialPrefix prefix : List.of(
                MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72)) {
            long weight = prefix.getMaterialWeight();
            if (weight <= 0 || remaining < weight) continue;
            while (remaining >= weight) {
                if (piles.size() >= maximumStacks) return new DustPlan(piles, represented, remaining);
                int count = (int) Math.min(64, remaining / weight);
                piles.add(new DustPile(prefix, count));
                long units = Math.multiplyExact(weight, count);
                represented = Math.addExact(represented, units);
                remaining = Math.subtractExact(remaining, units);
            }
        }
        return new DustPlan(piles, represented, remaining);
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

    /**
     * Pure Shredder plan over the complete effective composition. Every positive component is
     * converted through its own pulverization target; missing targets and output overflow are
     * reported instead of silently dropping that component.
     */
    public record ShredderPlan(List<ShredderOutput> outputs, long duration, ShredderRejection rejection) {
        public ShredderPlan {
            outputs = List.copyOf(outputs);
            if (rejection == null) throw new IllegalArgumentException("Missing shredder rejection");
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

    public static ShredderPlan planShredder(ItemComposition composition, int outputItemSlots) {
        if (composition == null || composition.components().isEmpty()) return rejected(ShredderRejection.NO_COMPONENTS);
        if (outputItemSlots <= 0) return rejected(ShredderRejection.OUTPUT_LIMIT);
        try {
            var targetUnits = new LinkedHashMap<GTMaterial, Long>();
            for (MaterialComponent component : composition.components()) {
                GTMaterial material = component.material().resolve();
                if (!material.isValid() || component.amount() <= 0)
                    return rejected(ShredderRejection.INVALID_COMPONENT);
                GTMaterial target = material.getTargetPulverMaterial();
                if (target == null) return rejected(ShredderRejection.MISSING_PULVERIZATION_TARGET);
                target = target.resolve();
                if (!target.isValid()) return rejected(ShredderRejection.INVALID_PULVERIZATION_TARGET);
                long amount = pulverizedAmount(material, component.amount());
                if (amount > 0) targetUnits.merge(target, amount, Math::addExact);
            }
            if (targetUnits.isEmpty()) return rejected(ShredderRejection.NO_COMPONENTS);

            var outputs = new ArrayList<ShredderOutput>();
            int remainingSlots = outputItemSlots;
            long duration = 0;
            for (var entry : targetUnits.entrySet()) {
                if (remainingSlots <= 0) return rejected(ShredderRejection.OUTPUT_LIMIT);
                var dust = dustPlan(entry.getValue(), remainingSlots);
                if (!dust.complete()) {
                    return rejected(
                            dust.stackCount() >= remainingSlots
                                    ? ShredderRejection.OUTPUT_LIMIT
                                    : ShredderRejection.UNREPRESENTABLE_AMOUNT);
                }
                if (dust.piles().isEmpty()) continue;
                outputs.add(new ShredderOutput(entry.getKey(), dust));
                remainingSlots -= dust.stackCount();
                long work = shredderWork(entry.getKey());
                duration = Math.addExact(
                        duration,
                        BigInteger.valueOf(entry.getValue())
                                .multiply(BigInteger.valueOf(work))
                                .add(BigInteger.valueOf(GTValues.U - 1))
                                .divide(BigInteger.valueOf(GTValues.U))
                                .longValueExact());
            }
            if (outputs.isEmpty()) return rejected(ShredderRejection.NO_COMPONENTS);
            return new ShredderPlan(outputs, Math.max(1, duration), ShredderRejection.NONE);
        } catch (ArithmeticException overflow) {
            return rejected(ShredderRejection.OVERFLOW);
        }
    }

    private static ShredderPlan rejected(ShredderRejection rejection) {
        return new ShredderPlan(List.of(), 0, rejection);
    }
}
