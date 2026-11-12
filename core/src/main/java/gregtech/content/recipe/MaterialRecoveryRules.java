package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialProcessingProperties;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemRecoveryPlanner;
import java.math.BigInteger;
import java.util.ArrayList;
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

    private static final List<MaterialPrefix> DUST_FORMS =
            List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72);

    private static final ItemRecoveryPlanner.MaterialFacts FACTS = new ItemRecoveryPlanner.MaterialFacts() {
        private final List<ItemRecoveryPlanner.DustForm> dustForms = DUST_FORMS.stream()
                .map(prefix -> new ItemRecoveryPlanner.DustForm(prefix.getName(), prefix.getMaterialWeight()))
                .toList();

        @Override
        public long pulverizedAmount(GTMaterial source, long amount) {
            return MaterialRecoveryRules.pulverizedAmount(source, amount);
        }

        @Override
        public long dynamicShredderWork(GTMaterial material) {
            return shredderWork(material);
        }

        @Override
        public long staticShredderWork(GTMaterial material) {
            return MaterialRecoveryRules.staticShredderWork(material);
        }

        @Override
        public List<ItemRecoveryPlanner.DustForm> dustForms() {
            return dustForms;
        }
    };

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
        return (MaterialProcessingProperties.isQuartz(material)
                        ? 64L
                        : material.hasAny(MaterialProperty.WOOD, MaterialProperty.STONE)
                                        || MaterialProcessingProperties.isBrittle(material)
                                        || MaterialProcessingProperties.isBouncy(material)
                                        || MaterialProcessingProperties.isStretchy(material)
                                        || MaterialProcessingProperties.isMeat(material)
                                        || MaterialProcessingProperties.isRotten(material)
                                        || gregtech.content.data.generated.MaterialWorkability.isFood(material)
                                ? 2L
                                : 256L)
                * Math.max(1, material.getToolQuality() + 1);
    }

    /** Static Shredder prefix rows use 256, or 16 for mortar-grindable materials. */
    public static long staticShredderWork(GTMaterial material) {
        long multiplier = MaterialProcessingProperties.isMortarGrindable(material) ? 16L : 256L;
        return multiplier * Math.max(1, material.getToolQuality() + 1);
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

    /**
     * One-slot view of the same dust allocation used by full Shredder plans.
     *
     * <p>Consumers that return one stack consume {@link DustPlan#representedUnits()} and may leave
     * {@link DustPlan#remainderUnits()} for a later retrieval. Recipe producers that own an output
     * array must use {@link #dustPlan(long)} and retain every pile.</p>
     */
    public static DustPlan dustSlotPlan(long amount) {
        return dustPlan(amount, 1);
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
     * converted through its own pulverization target; a component without a usable target is
     * skipped, while output overflow remains an explicit plan rejection.
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
        return convert(ItemRecoveryPlanner.planShredder(composition, outputItemSlots, FACTS, false));
    }

    /** Plans a generated static prefix row instead of the dynamic composition fallback. */
    public static ShredderPlan planShredderStatic(ItemComposition composition, int outputItemSlots) {
        return convert(ItemRecoveryPlanner.planShredder(composition, outputItemSlots, FACTS, true));
    }

    /** Source {@code OM.dust} floors a damaged amount to the finest representable dust form. */
    public static ShredderPlan planShredderDamaged(
            ItemComposition composition, int outputItemSlots, boolean staticPrefix) {
        return convert(ItemRecoveryPlanner.planShredder(composition, outputItemSlots, FACTS, staticPrefix, true));
    }

    private static ShredderPlan convert(ItemRecoveryPlanner.ShredderPlan plan) {
        var outputs = plan.outputs().stream()
                .map(output -> new ShredderOutput(output.material(), convert(output.dust())))
                .toList();
        return new ShredderPlan(
                outputs,
                plan.duration(),
                ShredderRejection.valueOf(plan.rejection().name()));
    }

    private static DustPlan convert(ItemRecoveryPlanner.DustPlan plan) {
        var piles = plan.piles().stream()
                .map(pile -> new DustPile(prefix(pile.form()), pile.count()))
                .toList();
        return new DustPlan(piles, plan.representedUnits(), plan.remainderUnits());
    }

    private static MaterialPrefix prefix(ItemRecoveryPlanner.DustForm form) {
        return DUST_FORMS.stream()
                .filter(prefix -> prefix.getName().equals(form.id()) && prefix.getMaterialWeight() == form.weight())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown dust form from recovery plan: " + form.id()));
    }
}
