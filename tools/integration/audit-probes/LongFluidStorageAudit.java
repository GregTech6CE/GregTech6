import com.gregtech.gregtech.api.fluid.LongFluidStorage;

/** Observes the port's current arithmetic, not original-game or native capability execution. */
public final class LongFluidStorageAudit {
    public static void main(String[] args) {
        var resized = new LongFluidStorage(1000);
        resized.setAmount(900);
        resized.setCapacity(500);
        observe("RESIZE_AMOUNT", resized.amount(), 500);
        observe("RESIZE_CAPACITY", resized.capacity(null), 500);

        var restored = new LongFluidStorage(1000);
        restored.restore(1200, 1000L);
        observe("RESTORED_AMOUNT", restored.amount(), 1200);
        observe("RESTORED_CAPACITY", restored.capacity(null), 1200);

        var overflow = new LongFluidStorage(1000);
        overflow.setAmount(900);
        overflow.voidExcess(true);
        observe("PARTIAL_OVERFLOW_ACCEPTED", overflow.add(200, null), 100);
        observe("PARTIAL_OVERFLOW_STORED", overflow.amount(), 1000);
        observe("FULL_OVERFLOW_ACCEPTED", overflow.add(200, null), 200);
        observe("FULL_OVERFLOW_STORED", overflow.amount(), 1000);

        var retained = new LongFluidStorage(1000);
        retained.keepFilterOnEmpty(true);
        retained.setAmount(1000);
        observe("RETAINED_FILTER_DRAIN", retained.remove(1000), 1000);
        observe("RETAINED_FILTER_AMOUNT", retained.amount(), 0);
        if (!retained.keepsFilterOnEmpty()) throw new AssertionError("filter policy lost");
        // This layer carries the policy only. Actual fluid identity and NBT live in FluidTankGT.
        System.out.println("RETAINED_FILTER_POLICY|true");
    }

    private static void observe(String name, long actual, long expectedCurrent) {
        if (actual != expectedCurrent) throw new AssertionError(name + ": " + actual);
        System.out.println(name + "|" + actual);
    }
}
