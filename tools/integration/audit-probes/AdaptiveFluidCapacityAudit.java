import com.gregtech.gregtech.api.fluid.LongFluidStorage;
import java.util.Map;

/** Synthetic key/amount observations against current core, not native fluid registration tests. */
public final class AdaptiveFluidCapacityAudit {
    public static void main(String[] args) {
        var capacities = Map.of("minecraft:water", 1000L, "gt6ce:seawater", 32000L);
        var current = new LongFluidStorage(8000);
        current.adjustableCapacity(capacities, 1);
        observe("UNBOUND_CAPACITY", current.capacity(null), 8000);
        observe("FLUID_TYPE_WATER_KEY", current.capacity("minecraft:water"), 8000);
        observe("SEA_FLUID_KEY", current.capacity("gt6ce:seawater"), 32000);
        observe("MISSING_KEY", current.capacity("example:missing"), 8000);

        // Compare the core's supported multiplier with what the machine constructor supplies.
        // GT6 used parallel * 2; this is a parallel=1 arithmetic example only.
        var scaled = new LongFluidStorage(1000);
        scaled.adjustableCapacity(capacities, 2);
        observe("PARALLEL_ONE_TWO_BATCH_CAPACITY", scaled.capacity("gt6ce:seawater"), 64000);
        observe("CURRENT_OUTPUT_BASE", new LongFluidStorage(8000).baseCapacity(), 8000);
        observe("ORIGINAL_OUTPUT_DEFAULT_ARGUMENT", new LongFluidStorage(Long.MAX_VALUE).baseCapacity(), Long.MAX_VALUE);
    }

    private static void observe(String label, long actual, long expected) {
        if (actual != expected) throw new AssertionError(label + ": " + actual);
        System.out.println(label + "|" + actual);
    }
}
